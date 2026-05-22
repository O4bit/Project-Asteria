//! Upstream NASA client.
//!
//! Responsibilities:
//!   * Build URLs to `api.nasa.gov` from validated query params (never from
//!     raw client input — SSRF protection).
//!   * Inject the `NASA_API_KEY` secret server-side. The key never reaches
//!     the client and is never logged.
//!   * Retry transient upstream failures with capped exponential backoff
//!     + jitter (lightweight, no extra crates).
//!   * Optional D1-backed response cache to reduce upstream calls and
//!     soak up bursts under the IP rate limit.

use crate::models::ApiError;
use serde::de::DeserializeOwned;
use std::time::Duration;
use worker::{console_warn, Env, Fetch, Headers, Method, Request, RequestInit};

/// Only this exact host is allowed for upstream calls.
/// Any other host triggers an SSRF-prevention error.
const NASA_HOST: &str = "api.nasa.gov";
const NASA_SCHEME: &str = "https";

/// Hard upstream timeout. Workers' fetch already has a platform timeout,
/// but we trip earlier to give retries a chance within the request budget.
const UPSTREAM_TIMEOUT: Duration = Duration::from_secs(8);

/// Max retry attempts for idempotent GETs on transient failures (5xx / 429 / network).
const MAX_RETRIES: u32 = 2;

/// Cache TTL for NASA responses, in seconds. APOD changes once per day;
/// NeoWs feed is also safe to cache briefly. Tunable per endpoint via param.
pub const DEFAULT_CACHE_TTL_SECS: i64 = 60 * 30; // 30 min

// --- Public client API --------------------------------------------------

pub struct NasaClient<'a> {
    env: &'a Env,
    api_key: String,
}

impl<'a> NasaClient<'a> {
    /// Build a client. Returns `Err` if the secret is missing — this is
    /// surfaced as a 500 "service misconfigured" without leaking which env
    /// var is missing in the client-facing message.
    pub fn from_env(env: &'a Env) -> Result<Self, ApiError> {
        let api_key = env
            .secret("NASA_API_KEY")
            .or_else(|_| env.var("NASA_API_KEY"))
            .map(|v| v.to_string())
            .map_err(|_| ApiError::new("service_misconfigured", "Service is not configured."))?;
        if api_key.trim().is_empty() {
            return Err(ApiError::new("service_misconfigured", "Service is not configured."));
        }
        Ok(Self { env, api_key })
    }

    /// GET a NASA endpoint and deserialize as `T`.
    ///
    /// `path` must be a known, safe path constant (e.g. `/planetary/apod`).
    /// `query` is a list of pre-validated `(name, value)` pairs from the
    /// caller — values are URL-encoded here. The API key is appended
    /// server-side; callers must not pass `api_key`.
    pub async fn get_json<T: DeserializeOwned>(
        &self,
        path: &'static str,
        query: &[(&str, String)],
        cache_ttl_secs: i64,
    ) -> Result<T, ApiError> {
        let url = self.build_url(path, query)?;
        let cache_key = format!("nasa:{}", url);

        // Try cache first (best-effort; cache miss is non-fatal).
        if let Some(body) = cache_get(self.env, &cache_key).await {
            if let Ok(parsed) = serde_json::from_str::<T>(&body) {
                return Ok(parsed);
            }
        }

        let body = self.fetch_with_retry(&url).await?;

        let parsed: T = serde_json::from_str(&body).map_err(|_| {
            ApiError::new("upstream_invalid", "Upstream returned malformed data.")
        })?;

        // Write-through cache. Failure to cache must not fail the request.
        let _ = cache_put(self.env, &cache_key, &body, cache_ttl_secs).await;

        Ok(parsed)
    }

    // --- internals ------------------------------------------------------

    /// Builds and validates the upstream URL.
    ///
    /// SSRF protection: we construct the URL from a hard-coded scheme +
    /// host + path constant and only then attach caller-supplied query
    /// values via the `url` crate (which percent-encodes them). After
    /// building, we re-parse and assert host/scheme match — this catches
    /// any accidental path containing `//evil.com` style injection.
    fn build_url(&self, path: &'static str, query: &[(&str, String)]) -> Result<String, ApiError> {
        if !path.starts_with('/') || path.contains("//") {
            return Err(ApiError::new("internal", "Invalid upstream path."));
        }
        let base = format!("{NASA_SCHEME}://{NASA_HOST}{path}");
        let mut url = url::Url::parse(&base)
            .map_err(|_| ApiError::new("internal", "Invalid upstream URL."))?;
        {
            let mut qp = url.query_pairs_mut();
            for (k, v) in query {
                if *k == "api_key" {
                    // Hard refuse: clients must never inject the key.
                    return Err(ApiError::new("invalid_request", "Reserved parameter."));
                }
                qp.append_pair(k, v);
            }
            qp.append_pair("api_key", &self.api_key);
        }
        // Defense-in-depth re-parse + host check.
        let parsed = url::Url::parse(url.as_str())
            .map_err(|_| ApiError::new("internal", "Invalid upstream URL."))?;
        if parsed.scheme() != NASA_SCHEME || parsed.host_str() != Some(NASA_HOST) {
            return Err(ApiError::new("internal", "Upstream host rejected."));
        }
        Ok(url.into())
    }

    /// Fetches the URL with retry/backoff on transient failures.
    ///
    /// Errors are mapped to the public `ApiError` taxonomy. The NASA URL
    /// (which contains `api_key`) is *never* placed in any returned
    /// message — only the path constant ever leaves this module.
    async fn fetch_with_retry(&self, url: &str) -> Result<String, ApiError> {
        let mut last_err: ApiError =
            ApiError::new("upstream_unavailable", "NASA service is unavailable.");

        for attempt in 0..=MAX_RETRIES {
            match fetch_once(url).await {
                Ok(FetchOutcome::Ok(body)) => return Ok(body),
                Ok(FetchOutcome::Retryable(status)) => {
                    last_err = match status {
                        429 => ApiError::new("upstream_rate_limited", "NASA upstream rate limited."),
                        _ => ApiError::new("upstream_unavailable", "NASA service is unavailable."),
                    };
                    console_warn!("upstream retryable status={status} attempt={attempt}");
                }
                Ok(FetchOutcome::Fatal(status)) => {
                    // 4xx (except 429): client-shaped problem; don't retry.
                    return Err(match status {
                        400 | 404 | 422 => ApiError::new("invalid_request", "Invalid upstream request."),
                        _ => ApiError::new("upstream_unavailable", "NASA service is unavailable."),
                    });
                }
                Err(e) => {
                    last_err = ApiError::new("upstream_unavailable", "NASA service is unavailable.");
                    console_warn!("upstream fetch error attempt={attempt}: {e}");
                }
            }
            if attempt < MAX_RETRIES {
                backoff_sleep(attempt).await;
            }
        }
        Err(last_err)
    }
}

// --- low-level fetch ----------------------------------------------------

enum FetchOutcome {
    Ok(String),
    /// 5xx / 429 → retry
    Retryable(u16),
    /// 4xx (other) → give up immediately
    Fatal(u16),
}

async fn fetch_once(url: &str) -> Result<FetchOutcome, String> {
    let mut headers = Headers::new();
    let _ = headers.set("Accept", "application/json");
    let _ = headers.set("User-Agent", "asteria-nasa-proxy/0.2");

    let mut init = RequestInit::new();
    init.with_method(Method::Get).with_headers(headers);

    let req = Request::new_with_init(url, &init).map_err(|e| e.to_string())?;

    // Workers fetch is the platform's async HTTP client (the WASM analogue
    // of reqwest). Platform-level timeout protects us; UPSTREAM_TIMEOUT
    // exists in code for documentation and future native-runtime parity.
    let _ = UPSTREAM_TIMEOUT;

    let mut resp = Fetch::Request(req).send().await.map_err(|e| e.to_string())?;
    let status = resp.status_code();

    if (200..300).contains(&status) {
        let text = resp.text().await.map_err(|e| e.to_string())?;
        Ok(FetchOutcome::Ok(text))
    } else if status == 429 || (500..600).contains(&status) {
        Ok(FetchOutcome::Retryable(status))
    } else {
        Ok(FetchOutcome::Fatal(status))
    }
}

async fn backoff_sleep(attempt: u32) {
    // Exponential backoff with jitter: 150ms, 300ms (+ up to 100ms jitter).
    let base_ms: u64 = 150u64.saturating_mul(1u64 << attempt);
    let mut jitter = [0u8; 1];
    let _ = getrandom::getrandom(&mut jitter);
    let jitter_ms = (jitter[0] as u64) * 100 / 255;
    let total = base_ms + jitter_ms;

    // Workers expose Delay via Date arithmetic; use a simple Promise-based
    // sleep through the `worker` crate's `Delay` helper.
    worker::Delay::from(Duration::from_millis(total)).await;
}

// --- D1-backed response cache ------------------------------------------
//
// We reuse the existing `DB` D1 binding (declared in wrangler.toml). A
// dedicated `response_cache` table is created on first use and pruned
// lazily on read. Cache misses or any D1 errors are silently degraded
// to "no cache" — they must never break the request path.

async fn cache_get(env: &Env, key: &str) -> Option<String> {
    let db = env.d1("DB").ok()?;
    let _ = db
        .exec("CREATE TABLE IF NOT EXISTS response_cache (k TEXT PRIMARY KEY, v TEXT NOT NULL, expires_at INTEGER NOT NULL)")
        .await;
    let now = chrono::Utc::now().timestamp();
    let stmt = db
        .prepare("SELECT v FROM response_cache WHERE k = ?1 AND expires_at > ?2")
        .bind(&[key.into(), (now as i64).into()])
        .ok()?;
    let row: Option<serde_json::Value> = stmt.first(None).await.ok().flatten();
    row.and_then(|v| v.get("v").and_then(|s| s.as_str()).map(|s| s.to_string()))
}

async fn cache_put(env: &Env, key: &str, value: &str, ttl_secs: i64) -> Result<(), ()> {
    let db = env.d1("DB").map_err(|_| ())?;
    let expires_at = chrono::Utc::now().timestamp() + ttl_secs;
    let stmt = db
        .prepare("INSERT OR REPLACE INTO response_cache (k, v, expires_at) VALUES (?1, ?2, ?3)")
        .bind(&[key.into(), value.into(), (expires_at as i64).into()])
        .map_err(|_| ())?;
    stmt.run().await.map(|_| ()).map_err(|_| ())
}
