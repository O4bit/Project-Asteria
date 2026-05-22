//! HTTP handlers for `/v2/nasa/...`.
//!
//! Each handler:
//!   1. Extracts and *validates* query parameters (rejects unknown params,
//!      enforces date format / range bounds).
//!   2. Applies the IP-based rate limit (100 req / hour) — this happens
//!      *before* any upstream NASA call, as required.
//!   3. Calls `NasaClient` and returns the JSON, or a structured `ApiError`.
//!
//! Responses are always JSON. Successful payloads mirror NASA's native
//! shape (see `models` module) so existing Android models keep working.
//! Error payloads always follow `ApiError`.

use crate::models::{ApiError, AstronomyPicture, NeoFeedResponse};
use crate::nasa::{NasaClient, DEFAULT_CACHE_TTL_SECS};
use crate::ratelimit;
use chrono::NaiveDate;
use serde_json::json;
use worker::{Request, Response, Result as WorkerResult, RouteContext};

const APOD_PATH: &str = "/planetary/apod";
const NEOWS_FEED_PATH: &str = "/neo/rest/v1/feed";

// --- Entry points -------------------------------------------------------

pub async fn apod_today(req: Request, ctx: RouteContext<()>) -> WorkerResult<Response> {
    run(req, ctx, |env, _req| async move {
        // Reject any query params — `today` takes none.
        // (We get `req` borrowed via the closure capture below.)
        let client = NasaClient::from_env(&env)?;
        client
            .get_json::<AstronomyPicture>(APOD_PATH, &[], DEFAULT_CACHE_TTL_SECS)
            .await
    })
    .await
}

pub async fn apod_by_date(req: Request, ctx: RouteContext<()>) -> WorkerResult<Response> {
    run(req, ctx, |env, req| async move {
        let date = require_query(&req, "date")?;
        validate_date(&date)?;
        deny_extra_params(&req, &["date"])?;

        let client = NasaClient::from_env(&env)?;
        client
            .get_json::<AstronomyPicture>(
                APOD_PATH,
                &[("date", date)],
                DEFAULT_CACHE_TTL_SECS,
            )
            .await
    })
    .await
}

pub async fn apod_range(req: Request, ctx: RouteContext<()>) -> WorkerResult<Response> {
    run(req, ctx, |env, req| async move {
        let start = require_query(&req, "start_date")?;
        let end = require_query(&req, "end_date")?;
        let start_d = validate_date(&start)?;
        let end_d = validate_date(&end)?;
        deny_extra_params(&req, &["start_date", "end_date"])?;

        if end_d < start_d {
            return Err(ApiError::new("invalid_request", "end_date must be >= start_date."));
        }
        // Cap to 30 days to keep upstream costs predictable.
        let days = (end_d - start_d).num_days();
        if days > 30 {
            return Err(ApiError::new(
                "invalid_request",
                "Date range too large (max 30 days).",
            ));
        }

        let client = NasaClient::from_env(&env)?;
        // APOD with start/end_date returns an array.
        client
            .get_json::<Vec<AstronomyPicture>>(
                APOD_PATH,
                &[("start_date", start), ("end_date", end)],
                DEFAULT_CACHE_TTL_SECS,
            )
            .await
    })
    .await
}

pub async fn neows_feed(req: Request, ctx: RouteContext<()>) -> WorkerResult<Response> {
    run(req, ctx, |env, req| async move {
        let start = require_query(&req, "start_date")?;
        let end = require_query(&req, "end_date")?;
        let start_d = validate_date(&start)?;
        let end_d = validate_date(&end)?;
        deny_extra_params(&req, &["start_date", "end_date"])?;

        if end_d < start_d {
            return Err(ApiError::new("invalid_request", "end_date must be >= start_date."));
        }
        // NASA NeoWs hard-limits this to 7 days; enforce it ourselves so we
        // return a clean 400 instead of forwarding the upstream error.
        if (end_d - start_d).num_days() > 7 {
            return Err(ApiError::new(
                "invalid_request",
                "NeoWs date range cannot exceed 7 days.",
            ));
        }

        let client = NasaClient::from_env(&env)?;
        client
            .get_json::<NeoFeedResponse>(
                NEOWS_FEED_PATH,
                &[("start_date", start), ("end_date", end)],
                DEFAULT_CACHE_TTL_SECS,
            )
            .await
    })
    .await
}

pub async fn health(_req: Request, _ctx: RouteContext<()>) -> WorkerResult<Response> {
    Response::from_json(&json!({ "status": "ok", "service": "nasa-mirror-api", "version": "v2" }))
}

// --- Shared pipeline ----------------------------------------------------

/// Runs the standard request pipeline:
///   * extract client IP
///   * enforce IP rate limit *before* upstream
///   * invoke the handler-specific logic
///   * serialize success or `ApiError` into a JSON `Response`
async fn run<F, Fut, T>(
    req: Request,
    ctx: RouteContext<()>,
    handler: F,
) -> WorkerResult<Response>
where
    F: FnOnce(worker::Env, Request) -> Fut,
    Fut: std::future::Future<Output = Result<T, ApiError>>,
    T: serde::Serialize,
{
    let ip = client_ip(&req);

    // Rate limit FIRST — must run before any upstream NASA call.
    match ratelimit::check(&ctx.env, &ip).await {
        ratelimit::Outcome::Allowed => {}
        ratelimit::Outcome::Limited { retry_after_seconds } => {
            return error_response(
                ApiError::new("rate_limit_exceeded", "Rate limit exceeded (100/hour).")
                    .with_retry(retry_after_seconds),
                429,
            );
        }
        ratelimit::Outcome::Unavailable => {
            // Fail closed on rate-limiter outage to avoid abuse; this is
            // a rare path (D1 outage) and the client can retry.
            return error_response(
                ApiError::new("service_unavailable", "Service temporarily unavailable."),
                503,
            );
        }
    }

    match handler(ctx.env, req).await {
        Ok(v) => {
            let mut resp = Response::from_json(&v)?;
            let headers = resp.headers_mut();
            let _ = headers.set("Cache-Control", "public, max-age=300");
            let _ = headers.set("X-Content-Type-Options", "nosniff");
            Ok(resp)
        }
        Err(e) => {
            let status = status_for(e.code);
            error_response(e, status)
        }
    }
}

fn error_response(err: ApiError, status: u16) -> WorkerResult<Response> {
    let mut resp = Response::from_json(&err)?;
    let resp = resp.with_status(status);
    Ok(resp)
}

fn status_for(code: &str) -> u16 {
    match code {
        "invalid_request" => 400,
        "rate_limit_exceeded" => 429,
        "upstream_rate_limited" => 502,
        "upstream_invalid" => 502,
        "upstream_unavailable" => 503,
        "service_unavailable" => 503,
        "service_misconfigured" => 500,
        _ => 500,
    }
}

// --- Param validation ---------------------------------------------------

fn require_query(req: &Request, name: &str) -> Result<String, ApiError> {
    let url = req.url().map_err(|_| ApiError::new("invalid_request", "Invalid URL."))?;
    url.query_pairs()
        .find(|(k, _)| k == name)
        .map(|(_, v)| v.into_owned())
        .ok_or_else(|| ApiError::new("invalid_request", format!("Missing query parameter: {name}")))
}

fn deny_extra_params(req: &Request, allowed: &[&str]) -> Result<(), ApiError> {
    let url = req.url().map_err(|_| ApiError::new("invalid_request", "Invalid URL."))?;
    for (k, _) in url.query_pairs() {
        if !allowed.iter().any(|a| *a == k) {
            return Err(ApiError::new(
                "invalid_request",
                format!("Unknown query parameter: {k}"),
            ));
        }
    }
    Ok(())
}

/// Parse and validate a `YYYY-MM-DD` date. Bounds: 1995-06-16 (first APOD)
/// up to one day in the future (NASA's tz can be ahead of UTC).
fn validate_date(s: &str) -> Result<NaiveDate, ApiError> {
    if s.len() != 10 {
        return Err(ApiError::new("invalid_request", "Date must be YYYY-MM-DD."));
    }
    let d = NaiveDate::parse_from_str(s, "%Y-%m-%d")
        .map_err(|_| ApiError::new("invalid_request", "Date must be YYYY-MM-DD."))?;
    let min = NaiveDate::from_ymd_opt(1995, 6, 16).unwrap();
    let max = chrono::Utc::now().date_naive() + chrono::Duration::days(1);
    if d < min || d > max {
        return Err(ApiError::new("invalid_request", "Date out of supported range."));
    }
    Ok(d)
}

// --- Client IP ----------------------------------------------------------

/// Cloudflare sets `CF-Connecting-IP`. Fall back to `X-Forwarded-For` head
/// element. If neither is present, use a fixed sentinel — this only
/// happens off-platform (local dev) and groups all such requests under
/// one bucket, which is the safe choice.
fn client_ip(req: &Request) -> String {
    if let Ok(Some(v)) = req.headers().get("cf-connecting-ip") {
        if !v.is_empty() {
            return v;
        }
    }
    if let Ok(Some(v)) = req.headers().get("x-forwarded-for") {
        if let Some(first) = v.split(',').next() {
            let t = first.trim();
            if !t.is_empty() {
                return t.to_string();
            }
        }
    }
    "unknown".to_string()
}
