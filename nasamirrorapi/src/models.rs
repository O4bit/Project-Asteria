//! Data models for the NASA proxy API.
//!
//! The wire shapes for `AstronomyPicture` and `NeoFeedResponse` deliberately
//! mirror NASA's official JSON payloads (`api.nasa.gov/planetary/apod`,
//! `api.nasa.gov/neo/rest/v1/feed`) so existing clients (the Android app)
//! can keep their data classes unchanged. We re-emit NASA fields verbatim
//! after sanitizing strings — no extra wrapper layer.
//!
//! All structured error responses follow `ApiError` which is the single
//! JSON error contract for every endpoint.

use serde::{Deserialize, Serialize};
use serde_json::Value;
use std::collections::BTreeMap;

// --- APOD ---------------------------------------------------------------

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct AstronomyPicture {
    pub date: String,
    pub explanation: String,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub hdurl: Option<String>,
    pub media_type: String,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub service_version: Option<String>,
    pub title: String,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub url: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub copyright: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub thumbnail_url: Option<String>,
}

// --- NeoWs --------------------------------------------------------------
//
// NeoWs payloads are deeply nested and include navigation links + many
// optional fields. Rather than mirror every leaf in Rust types (which
// risks deserialization failure when NASA adds fields), we keep the
// `near_earth_objects` map as `Value` and only validate the top-level
// envelope. The Android model parses the same fields it always has.

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct NeoFeedResponse {
    pub element_count: u32,
    /// Map of `YYYY-MM-DD` -> array of asteroid objects (NASA's native shape).
    pub near_earth_objects: BTreeMap<String, Value>,
}

// --- Errors -------------------------------------------------------------

/// Single JSON error contract for every endpoint.
///
/// `code` is a stable machine-readable string; `message` is human-readable.
/// We deliberately omit upstream error bodies to avoid leaking the API key
/// or other NASA-internal details.
#[derive(Debug, Clone, Serialize)]
pub struct ApiError {
    pub code: &'static str,
    pub message: String,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub retry_after_seconds: Option<u64>,
}

impl ApiError {
    pub fn new(code: &'static str, message: impl Into<String>) -> Self {
        Self { code, message: message.into(), retry_after_seconds: None }
    }
    pub fn with_retry(mut self, secs: u64) -> Self {
        self.retry_after_seconds = Some(secs);
        self
    }
}
