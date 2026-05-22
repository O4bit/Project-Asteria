# NASA Mirror API

A secure, production-ready Cloudflare Workers API that scrapes and exposes content from the APOD (Astronomy Picture of the Day) mirror at NCKU as JSON, modeled after NASA's official API.

[![CI](https://github.com/yourusername/nasa-mirror-api/workflows/CI/badge.svg)](https://github.com/yourusername/nasa-mirror-api/actions)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

## 🌟 Features

- **🦀 Rust + WASM**: High-performance implementation using workers-rs
- **⚡ Edge Computing**: Deployed on Cloudflare's global network
- **💾 Smart Caching**: KV-based caching with content-hash change detection
- **🔒 Security First**: Rate limiting, SSRF protection, security headers, HTML sanitization
- **📅 Daily Updates**: Automated cron job for fresh content
- **🎯 Standards Compliant**: HTTP caching (ETag, If-Modified-Since), proper status codes
- **📊 Well-Documented**: OpenAPI spec, comprehensive examples

## 🏗️ Architecture

```
┌─────────────┐
│   Client    │
└──────┬──────┘
       │ HTTPS
       ▼
┌─────────────────────────────────────┐
│   Cloudflare Edge (Global CDN)      │
│  ┌──────────────────────────────┐   │
│  │  NASA Mirror API Worker       │   │
│  │  (Rust/WASM)                  │   │
│  │                               │   │
│  │  ┌─────────────────────────┐  │   │
│  │  │  Security Middleware    │  │   │
│  │  │  - Rate Limiting        │  │   │
│  │  │  - Input Validation     │  │   │
│  │  │  - SSRF Protection      │  │   │
│  │  └─────────────────────────┘  │   │
│  │              │                │   │
│  │  ┌───────────▼──────────────┐ │   │
│  │  │  API Handlers            │ │   │
│  │  │  - GET /apod/latest      │ │   │
│  │  │  - GET /apod/{date}      │ │   │
│  │  └───────────┬──────────────┘ │   │
│  │              │                │   │
│  │  ┌───────────▼──────────────┐ │   │
│  │  │  Cache Manager (KV)      │ │   │
│  │  │  - ETag Support          │ │   │
│  │  │  - TTL Management        │ │   │
│  │  └───────────┬──────────────┘ │   │
│  │              │ Cache miss     │   │
│  │  ┌───────────▼──────────────┐ │   │
│  │  │  HTML Parser & Scraper   │ │   │
│  │  └───────────┬──────────────┘ │   │
│  └──────────────┼────────────────┘   │
│                 │                     │
│  ┌──────────────▼────────────────┐   │
│  │  Scheduled Cron Trigger       │   │
│  │  (Daily at 12:00 UTC)         │   │
│  └───────────────────────────────┘   │
└─────────────────┬───────────────────┘
                  │ Fetch HTML
                  ▼
┌─────────────────────────────────────┐
│  Source: NCKU APOD Mirror           │
│  sprite.phys.ncku.edu.tw            │
└─────────────────────────────────────┘
```

## 📋 API Endpoints

### `GET /apod/latest`
Returns the most recent APOD entry.

**Response**: `200 OK`
```json
{
  "date": "2024-10-05",
  "title": "NGC 1365: A Barred Spiral Galaxy",
  "explanation": "<p>NGC 1365 is a giant barred spiral galaxy...</p>",
  "explanation_text": "NGC 1365 is a giant barred spiral galaxy...",
  "media_type": "image",
  "url": "http://sprite.phys.ncku.edu.tw/astrolab/mirrors/apod_e/image/2410/ngc1365.jpg",
  "hdurl": "http://sprite.phys.ncku.edu.tw/astrolab/mirrors/apod_e/image/2410/ngc1365_hd.jpg",
  "source_url": "http://sprite.phys.ncku.edu.tw/astrolab/mirrors/apod_e/apod.html",
  "extracted_at": "2024-10-05T12:00:00Z",
  "copyright": "Copyright: John Doe"
}
```

### `GET /apod/{YYYY-MM-DD}`
Returns APOD entry for a specific date.

**Parameters**:
- `date`: ISO 8601 date format (YYYY-MM-DD)

**Response**: `200 OK` (same schema as above)

**Error Responses**:
- `400 Bad Request`: Invalid date format
- `404 Not Found`: No entry found for that date
- `429 Too Many Requests`: Rate limit exceeded
- `500 Internal Server Error`: Server error

### Caching Headers
- `ETag`: Content hash for change detection
- `Last-Modified`: Timestamp of last update
- `Cache-Control`: Caching directives
- `If-None-Match`: Client sends ETag to check for changes (returns `304 Not Modified` if unchanged)

## 🚀 Quick Start

### Prerequisites
- Rust 1.70+ (`rustup install stable`)
- Node.js 18+ and npm
- Wrangler CLI (`npm install -g wrangler`)
- Cloudflare account

### Installation

1. **Clone the repository**
```bash
git clone https://github.com/yourusername/nasa-mirror-api.git
cd nasa-mirror-api
```

2. **Install dependencies**
```bash
npm install
cargo build
```

3. **Install worker-build**
```bash
cargo install worker-build
```

4. **Create KV namespaces**
```bash
# Create production KV namespaces
wrangler kv:namespace create "APOD_CACHE"
wrangler kv:namespace create "RATE_LIMIT"

# Create preview KV namespaces
wrangler kv:namespace create "APOD_CACHE" --preview
wrangler kv:namespace create "RATE_LIMIT" --preview
```

5. **Update wrangler.toml**
Replace the placeholder KV namespace IDs with the IDs from step 4:
```toml
[[kv_namespaces]]
binding = "APOD_CACHE"
id = "your-actual-namespace-id"
preview_id = "your-actual-preview-id"

[[kv_namespaces]]
binding = "RATE_LIMIT"
id = "your-actual-ratelimit-id"
preview_id = "your-actual-ratelimit-preview-id"
```

6. **Run locally**
```bash
npm run dev
```

Visit http://localhost:8787/apod/latest

### Deployment

1. **Authenticate with Cloudflare**
```bash
wrangler login
```

2. **Deploy to Cloudflare Workers**
```bash
npm run deploy
```

3. **Configure custom domain (optional)**
Add to `wrangler.toml`:
```toml
[env.production]
name = "nasa-mirror-api-production"
route = "api.yourdomain.com/apod/*"
```

## 🧪 Testing

### Run unit tests
```bash
cargo test
```

### Manual API testing
```bash
# Test latest endpoint
curl https://nasa-mirror-api.yourusername.workers.dev/apod/latest

# Test specific date
curl https://nasa-mirror-api.yourusername.workers.dev/apod/2024-10-01

# Test with ETag caching
curl -H "If-None-Match: abc123hash" \
     https://nasa-mirror-api.yourusername.workers.dev/apod/latest
```

### JavaScript example
```javascript
const response = await fetch('https://nasa-mirror-api.yourusername.workers.dev/apod/latest');
const apod = await response.json();

console.log(`Title: ${apod.title}`);
console.log(`Date: ${apod.date}`);
console.log(`Image URL: ${apod.url}`);
```

## 🔒 Security Features

### Implemented Protections

1. **Rate Limiting**
   - Configurable per-IP rate limits (default: 100 requests/minute)
   - Stored in KV with automatic expiration

2. **SSRF Prevention**
   - Whitelist-based URL validation
   - Only allows fetching from approved domains

3. **Input Validation**
   - Strict date format validation
   - Request size limits (100KB default)

4. **HTML Sanitization**
   - Removes dangerous tags (script, iframe, object)
   - Strips javascript: and data: protocols

5. **Security Headers**
   - Content-Security-Policy
   - Strict-Transport-Security (HSTS)
   - X-Content-Type-Options: nosniff
   - X-Frame-Options: DENY
   - Referrer-Policy

6. **Privacy**
   - PII redaction in logs (IP addresses anonymized)
   - No sensitive data in error messages

7. **Request Timeouts**
   - Configurable timeout for external fetches (10s default)

### OWASP Top 10 Compliance

| Risk | Mitigation |
|------|------------|
| Injection | Input validation, HTML sanitization |
| Broken Auth | N/A (public API, no authentication) |
| Sensitive Data | No sensitive data stored, PII redacted |
| XXE | Not applicable (no XML processing) |
| Broken Access | Rate limiting, proper error handling |
| Security Misconfig | Strict CSP, security headers |
| XSS | HTML sanitization, CSP headers |
| Insecure Deserialization | Type-safe Rust with serde |
| Components with Vulnerabilities | Regular dependency updates |
| Insufficient Logging | Structured logging with PII redaction |

### Threat Model

**Assets**: Cached APOD data, API availability
**Threats**: DDoS, SSRF, data tampering, cache poisoning
**Mitigations**: Rate limiting, URL validation, content hashing, KV isolation

## ⚙️ Configuration

Environment variables in `wrangler.toml`:

| Variable | Default | Description |
|----------|---------|-------------|
| `APOD_SOURCE_URL` | `http://sprite...` | Source APOD mirror URL |
| `MAX_REQUEST_SIZE_KB` | `100` | Maximum request size |
| `RATE_LIMIT_REQUESTS` | `100` | Requests per window |
| `RATE_LIMIT_WINDOW_SEC` | `60` | Rate limit window (seconds) |
| `REQUEST_TIMEOUT_SEC` | `10` | Fetch timeout |

Cron schedule:
```toml
[triggers]
crons = ["0 12 * * *"]  # Daily at 12:00 UTC
```

## 📖 OpenAPI Specification

See [openapi.yaml](./docs/openapi.yaml) for the complete OpenAPI 3.0 specification.

Quick preview:
```yaml
openapi: 3.0.0
info:
  title: NASA Mirror API
  version: 0.1.0
  description: Scrapes APOD mirror and exposes as JSON API
paths:
  /apod/latest:
    get:
      summary: Get latest APOD entry
      responses:
        '200':
          description: Success
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ApodEntry'
```

## 📜 Legal & Ethical Considerations

### Robots.txt Compliance
- The NCKU mirror does not explicitly prohibit scraping in robots.txt
- Our cron job runs once daily to minimize server load
- Implements conditional requests (If-Modified-Since) to reduce bandwidth

### Copyright
- All APOD content is credited to original authors via the `copyright` field
- `source_url` field always links back to the original page
- This API serves as a proxy/mirror service, not claiming ownership

### Fair Use
- Educational and research purposes
- No commercial use of underlying data
- Minimal impact on source server (1 request/day + on-demand with caching)

### Responsible Scraping
- User-Agent identifies our service
- Respects HTTP caching headers
- Implements exponential backoff on errors (TODO: add retry logic)
- Monitors for changes to avoid unnecessary updates

**Disclaimer**: This is an unofficial API. Users should comply with NASA's and NCKU's terms of service.

## 🛠️ Development

### Project Structure
```
nasa-mirror-api/
├── src/
│   ├── lib.rs           # Main worker entry point, router
│   ├── models.rs        # Data structures (ApodEntry, etc.)
│   ├── parser.rs        # HTML parsing logic
│   ├── scraper.rs       # Fetching and cache management
│   ├── handlers.rs      # Request handlers
│   └── security.rs      # Security middleware
├── docs/
│   ├── openapi.yaml     # OpenAPI specification
│   └── architecture.md  # Detailed architecture docs
├── .github/
│   └── workflows/
│       └── ci.yml       # CI/CD pipeline
├── Cargo.toml           # Rust dependencies
├── wrangler.toml        # Cloudflare Workers config
└── README.md            # This file
```

### Building
```bash
# Debug build
cargo build

# Release build (optimized for size)
cargo build --release

# Build for Workers
worker-build --release
```

### Code Quality
```bash
# Format code
cargo fmt

# Lint
cargo clippy

# Run all tests
cargo test --all
```

## 🤝 Contributing

Contributions welcome! Please:
1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'Add amazing feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Commit Convention
- `feat:` New features
- `fix:` Bug fixes
- `docs:` Documentation changes
- `test:` Test additions/changes
- `refactor:` Code refactoring
- `chore:` Maintenance tasks

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- NASA for the original APOD content
- NCKU Physics Department for maintaining the mirror
- Cloudflare for the Workers platform
- The Rust and workers-rs communities

## 📧 Contact

- GitHub Issues: https://github.com/yourusername/nasa-mirror-api/issues
- Email: your.email@example.com

---

**Status**: Production Ready ✅
**Last Updated**: October 2024
**Rust Version**: 1.70+
**Workers Runtime**: Cloudflare Workers (2024-10-01)
