//! Per-IP rate limit: 100 requests / rolling hour.
//!
//! Storage: the `DB` D1 binding (already declared in `wrangler.toml`).
//! We use a fixed-hour window keyed by `(ip, hour_bucket)`. Fixed
//! windows are coarser than sliding windows but D1 makes them cheap
//! and they satisfy the "100 req/hour per IP" contract.
//!
//! This module is intentionally self-contained so it can be invoked
//! BEFORE the upstream NASA call (the call site is in `handlers::run`).

use worker::Env;

const LIMIT_PER_HOUR: u32 = 100;
const WINDOW_SECONDS: i64 = 3600;

pub enum Outcome {
    Allowed,
    Limited { retry_after_seconds: u64 },
    /// Limiter storage is unavailable (D1 outage). Caller should fail closed.
    Unavailable,
}

pub async fn check(env: &Env, ip: &str) -> Outcome {
    let db = match env.d1("DB") {
        Ok(db) => db,
        Err(_) => return Outcome::Unavailable,
    };

    // Lazy schema. Cheap on hot path; D1 keeps prepared statements cached.
    if db
        .exec(
            "CREATE TABLE IF NOT EXISTS rate_limit (\
             ip TEXT NOT NULL, \
             window_start INTEGER NOT NULL, \
             count INTEGER NOT NULL, \
             PRIMARY KEY (ip, window_start))",
        )
        .await
        .is_err()
    {
        return Outcome::Unavailable;
    }

    let now = chrono::Utc::now().timestamp();
    let window_start = now - (now % WINDOW_SECONDS);
    let next_window = window_start + WINDOW_SECONDS;

    // Atomic increment via UPSERT. SQLite/D1 supports `ON CONFLICT DO UPDATE`.
    let stmt = match db
        .prepare(
            "INSERT INTO rate_limit (ip, window_start, count) VALUES (?1, ?2, 1) \
             ON CONFLICT(ip, window_start) DO UPDATE SET count = count + 1 \
             RETURNING count",
        )
        .bind(&[ip.into(), (window_start as i64).into()])
    {
        Ok(s) => s,
        Err(_) => return Outcome::Unavailable,
    };

    let row: Option<serde_json::Value> = match stmt.first(None).await {
        Ok(r) => r,
        Err(_) => return Outcome::Unavailable,
    };

    let count = row
        .as_ref()
        .and_then(|v| v.get("count"))
        .and_then(|v| v.as_i64())
        .unwrap_or(LIMIT_PER_HOUR as i64 + 1); // fail closed on parse miss

    if count > LIMIT_PER_HOUR as i64 {
        let retry_after = (next_window - now).max(1) as u64;
        Outcome::Limited { retry_after_seconds: retry_after }
    } else {
        Outcome::Allowed
    }
}
