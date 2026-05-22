# Architecture Documentation

## System Overview

The NASA Mirror API is a serverless application built on Cloudflare Workers that scrapes Astronomy Picture of the Day (APOD) content from the NCKU mirror and exposes it through a RESTful JSON API.

## Component Architecture

### 1. Request Flow

```
Client Request
    ↓
Cloudflare Edge (Global CDN)
    ↓
Security Middleware
    ├─ Rate Limiting Check
    ├─ Request Size Validation
    ├─ Input Sanitization
    └─ SSRF Protection
    ↓
Router
    ├─ /apod/latest → Latest Handler
    ├─ /apod/{date} → Date Handler
    └─ /* → 404 Handler
    ↓
Cache Manager (KV Check)
    ├─ Cache Hit → Return Cached Entry
    └─ Cache Miss → Scraper
        ↓
    HTML Scraper
        ├─ Fetch from Source
        ├─ Parse HTML
        ├─ Extract Data
        └─ Save to KV
    ↓
Response with Security Headers
```

### 2. Scheduled Cron Flow

```
Cron Trigger (Daily 12:00 UTC)
    ↓
Fetch Latest APOD
    ↓
Parse HTML Content
    ↓
Compute Content Hash
    ↓
Compare with Cached Hash
    ├─ Changed → Update KV
    └─ Unchanged → Skip Update
    ↓
Log Result
```

## Module Breakdown

### `lib.rs` - Main Entry Point
- **Responsibility**: Request routing, event handling
- **Key Functions**:
  - `fetch()`: Handles HTTP requests
  - `scheduled()`: Handles cron triggers
- **Dependencies**: All other modules

### `models.rs` - Data Structures
- **Responsibility**: Define data schemas
- **Key Types**:
  - `ApodEntry`: Main APOD data structure
  - `CachedEntry`: Entry with metadata for KV
  - `ErrorResponse`: Standardized error format
  - `MediaType`: Enum for image/video
- **Features**:
  - Serialization/deserialization (serde)
  - Content hashing for change detection

### `parser.rs` - HTML Parsing
- **Responsibility**: Extract APOD data from HTML
- **Key Functions**:
  - `parse()`: Main parsing entry point
  - `extract_title()`: Title extraction
  - `extract_date()`: Date parsing
  - `extract_explanation()`: Explanation extraction
  - `extract_media()`: Media URL detection
- **Dependencies**: scraper, regex
- **Test Coverage**: Unit tests for parsing logic

### `scraper.rs` - Content Fetching
- **Responsibility**: HTTP fetching and caching
- **Key Components**:
  - `ApodScraper`: Fetches and parses content
  - `CacheManager`: KV operations
- **Key Functions**:
  - `fetch_and_parse()`: Fetch + parse combined
  - `fetch_latest()`: Get latest entry
  - `fetch_by_date()`: Get specific date
  - `get_latest()`: Read from KV
  - `save_latest()`: Write to KV
  - `has_content_changed()`: Hash comparison

### `handlers.rs` - Request Handlers
- **Responsibility**: Handle specific routes
- **Key Functions**:
  - `handle_latest()`: GET /apod/latest
  - `handle_by_date()`: GET /apod/{date}
  - `handle_scheduled()`: Cron trigger
  - `handle_not_found()`: 404 responses
  - `handle_method_not_allowed()`: 405 responses
- **Features**:
  - ETag support
  - Conditional requests (304)
  - Error handling

### `security.rs` - Security Layer
- **Responsibility**: Security enforcement
- **Key Components**:
  - `SecurityMiddleware`: Request validation, headers
  - `RateLimiter`: IP-based rate limiting
  - `RequestLogger`: Privacy-preserving logging
- **Key Functions**:
  - `check_request_size()`: Size validation
  - `add_security_headers()`: HTTP security headers
  - `validate_url()`: SSRF prevention
  - `sanitize_html()`: XSS prevention
  - `check_rate_limit()`: Rate limiting
  - `log_request()`: PII-redacted logging

## Data Flow Diagrams

### Cache Hit Scenario
```
GET /apod/latest
    ↓
Security Check ✓
    ↓
Rate Limit Check ✓
    ↓
KV Lookup → Cache Hit
    ↓
Check If-None-Match Header
    ├─ Match → 304 Not Modified
    └─ No Match → 200 OK + Entry
```

### Cache Miss Scenario
```
GET /apod/2024-10-01
    ↓
Security Check ✓
    ↓
KV Lookup → Cache Miss
    ↓
Fetch HTML from Source
    ↓
Parse HTML
    ↓
Extract Data
    ↓
Save to KV (async)
    ↓
Return 200 OK + Entry
```

### Cron Update Scenario
```
Cron Trigger (12:00 UTC)
    ↓
Fetch Latest HTML
    ↓
Parse Content
    ↓
Compute Hash: abc123
    ↓
Get Cached Hash: abc123
    ↓
Compare → No Change
    ↓
Skip KV Update
    ↓
Log: "Content unchanged"
```

## Security Architecture

### Defense in Depth Layers

1. **Edge Layer (Cloudflare)**
   - DDoS protection
   - TLS termination
   - Geographic distribution

2. **Application Layer (Worker)**
   - Rate limiting
   - Input validation
   - SSRF prevention
   - HTML sanitization

3. **Data Layer (KV)**
   - Isolated namespaces
   - TTL-based expiration
   - Content hashing

### Security Controls Matrix

| Threat | Control | Implementation |
|--------|---------|----------------|
| DDoS | Rate Limiting | KV-based per-IP tracking |
| SSRF | URL Whitelist | Domain validation before fetch |
| XSS | HTML Sanitization | Strip dangerous tags/protocols |
| Injection | Input Validation | Strict date format checking |
| Info Leak | PII Redaction | IP anonymization in logs |
| MITM | HSTS | Strict-Transport-Security header |
| Clickjacking | X-Frame-Options | DENY policy |
| Cache Poisoning | Content Hashing | SHA-256 verification |

## Performance Characteristics

### Latency Targets
- Cache Hit: < 50ms (p95)
- Cache Miss: < 2s (p95)
- Cron Execution: < 5s (p95)

### Resource Limits
- Worker CPU: 50ms per request
- Worker Memory: 128MB
- KV Read: < 1ms
- KV Write: < 1s (background)

### Caching Strategy
- Latest Entry: 1 hour TTL
- Date-specific: 7 days TTL
- Rate Limit: 60 seconds TTL

## Deployment Architecture

### Production Environment
```
GitHub Repository
    ↓
GitHub Actions (CI)
    ├─ cargo test
    ├─ cargo clippy
    ├─ cargo audit
    └─ worker-build
    ↓
Wrangler Deploy
    ↓
Cloudflare Edge (Global)
    ├─ Americas
    ├─ Europe
    ├─ Asia-Pacific
    └─ (200+ locations)
```

### KV Namespace Layout
```
APOD_CACHE:
  - apod:latest → CachedEntry (JSON)
  - apod:date:2024-10-01 → CachedEntry (JSON)
  - apod:date:2024-10-02 → CachedEntry (JSON)
  ...

RATE_LIMIT:
  - ratelimit:192.168.xxx.xxx → Counter (string)
  - ratelimit:10.0.xxx.xxx → Counter (string)
  ...
```

## Error Handling Strategy

### Error Categories
1. **Client Errors (4xx)**
   - 400: Invalid date format
   - 404: Entry not found
   - 405: Method not allowed
   - 429: Rate limit exceeded

2. **Server Errors (5xx)**
   - 500: Parse failure, fetch failure
   - 503: Service unavailable (rare)

### Retry Logic
- Client errors: No retry (client must fix)
- Server errors: Exponential backoff (TODO)
- Fetch timeouts: 10s hard limit

## Monitoring & Observability

### Logs
- Request logs (PII-redacted)
- Error logs (with context)
- Cron execution logs

### Metrics (via Cloudflare Dashboard)
- Request count
- Error rate
- Response time (p50, p95, p99)
- KV operations
- Worker CPU time

### Alerts (Recommended Setup)
- Error rate > 5%
- Cron failures
- KV quota warnings

## Scalability

### Horizontal Scaling
- Automatic via Cloudflare's global network
- No configuration needed

### Vertical Scaling
- Not applicable (serverless)

### Bottlenecks
- Source server rate limiting (mitigated by caching)
- KV write throughput (mitigated by hash-based updates)

## Future Improvements

1. **Enhanced Caching**
   - Implement stale-while-revalidate
   - Add R2 support for large media

2. **Advanced Security**
   - Implement token-based auth (optional)
   - Add request signing

3. **Monitoring**
   - Prometheus metrics export
   - Custom dashboards

4. **Features**
   - Date range queries
   - Search functionality
   - Webhook notifications

5. **Reliability**
   - Circuit breaker pattern
   - Automatic retry with exponential backoff
   - Health check endpoint

## Design Decisions

### Why Rust?
- **Performance**: Near-native speed, small WASM size
- **Safety**: Memory safety without garbage collection
- **Type System**: Catch errors at compile time

### Why Cloudflare Workers?
- **Global Edge**: Low latency worldwide
- **Serverless**: No infrastructure management
- **Cost-Effective**: Free tier sufficient for most use cases

### Why KV (not R2 or D1)?
- **Simplicity**: Key-value model fits our use case
- **Performance**: Low-latency reads from edge
- **Cost**: Free tier includes 100k reads/day

### Why Daily Cron (not real-time)?
- **Respectful Scraping**: Minimal load on source
- **Consistency**: Predictable update schedule
- **Efficiency**: Batch processing reduces overhead

## Compliance & Standards

### HTTP Standards
- RFC 7231: HTTP/1.1 Semantics
- RFC 7232: Conditional Requests (ETag, If-None-Match)
- RFC 6265: Cookies (not used)

### Security Standards
- OWASP Top 10 compliance
- CSP Level 3
- HSTS preload eligible

### API Standards
- OpenAPI 3.0 specification
- RESTful design principles
- Semantic versioning

## Testing Strategy

### Unit Tests
- Parser logic (models, extraction)
- Security functions (sanitization, validation)
- Date parsing

### Integration Tests
- End-to-end API calls (local dev)
- KV operations (mocked)

### CI/CD Tests
- Formatting (cargo fmt)
- Linting (cargo clippy)
- Security audit (cargo audit)
- Build verification

### Manual Testing
- Postman/curl tests
- Browser testing
- Load testing (optional)
