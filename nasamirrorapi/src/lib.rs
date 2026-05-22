//! Cloudflare Workers entry point for the NASA proxy API (v2).
//!
//! Route map (all GET, JSON in/out):
//!   GET /v2/healthz                              → service health
//!   GET /v2/nasa/apod/today                      → AstronomyPicture
//!   GET /v2/nasa/apod?date=YYYY-MM-DD            → AstronomyPicture
//!   GET /v2/nasa/apod/range?start_date&end_date  → [AstronomyPicture]
//!   GET /v2/nasa/neows/feed?start_date&end_date  → NeoFeedResponse
//!
//! The previous `/v1/...` (mirror-scraper) routes are intentionally removed
//! per the issue's "replace legacy NASA logic" directive — clients must
//! migrate to `/v2`. The error contract (`ApiError`) is consistent across
//! all routes; see `models::ApiError`.
//!
//! Secrets: `NASA_API_KEY` must be set via `wrangler secret put NASA_API_KEY`.
//! It is read server-side only and never echoed in responses or logs.

mod handlers;
mod models;
mod nasa;
mod ratelimit;

use worker::*;

#[event(fetch)]
pub async fn main(req: Request, env: Env, _ctx: Context) -> Result<Response> {
    console_error_panic_hook::set_once();

    Router::new()
        .get_async("/v2/healthz", handlers::health)
        .get_async("/v2/nasa/apod/today", handlers::apod_today)
        .get_async("/v2/nasa/apod", handlers::apod_by_date)
        .get_async("/v2/nasa/apod/range", handlers::apod_range)
        .get_async("/v2/nasa/neows/feed", handlers::neows_feed)
        .run(req, env)
        .await
}
