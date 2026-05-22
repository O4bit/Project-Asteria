# Development Guide

## Prerequisites

### Required Tools
- **Rust**: 1.70 or later
  ```bash
  curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh
  rustup install stable
  rustup default stable
  ```

- **wasm32 target**:
  ```bash
  rustup target add wasm32-unknown-unknown
  ```

- **Node.js**: 18 or later
  ```bash
  # Using nvm (recommended)
  nvm install 18
  nvm use 18
  ```

- **Wrangler CLI**:
  ```bash
  npm install -g wrangler
  ```

- **worker-build**:
  ```bash
  cargo install worker-build
  ```

### Optional Tools
- **cargo-watch**: Auto-rebuild on changes
  ```bash
  cargo install cargo-watch
  ```

- **cargo-audit**: Security auditing
  ```bash
  cargo install cargo-audit
  ```

## Project Setup

### 1. Clone and Install
```bash
git clone https://github.com/yourusername/nasa-mirror-api.git
cd nasa-mirror-api

# Install Node dependencies
npm install

# Build project
cargo build
```

### 2. Create KV Namespaces

```bash
# Production namespaces
wrangler kv:namespace create "APOD_CACHE"
wrangler kv:namespace create "RATE_LIMIT"

# Preview namespaces (for wrangler dev)
wrangler kv:namespace create "APOD_CACHE" --preview
wrangler kv:namespace create "RATE_LIMIT" --preview
```

Copy the IDs output by these commands and update `wrangler.toml`.

### 3. Configure wrangler.toml

Update the KV namespace IDs:
```toml
[[kv_namespaces]]
binding = "APOD_CACHE"
id = "<your-production-id>"
preview_id = "<your-preview-id>"

[[kv_namespaces]]
binding = "RATE_LIMIT"
id = "<your-production-id>"
preview_id = "<your-preview-id>"
```

### 4. Test Locally

```bash
# Start local dev server
npm run dev

# Or with wrangler directly
wrangler dev
```

Visit http://localhost:8787/apod/latest

## Development Workflow

### Day-to-Day Development

1. **Make Changes**: Edit files in `src/`

2. **Run Tests**:
   ```bash
   cargo test
   ```

3. **Check Code Quality**:
   ```bash
   # Format code
   cargo fmt
   
   # Lint
   cargo clippy
   ```

4. **Build Worker**:
   ```bash
   worker-build --release
   ```

5. **Test Locally**:
   ```bash
   npm run dev
   ```

### Auto-Rebuild on Changes

```bash
# Watch for changes and rebuild
cargo watch -x build

# Or use wrangler dev (it watches automatically)
wrangler dev
```

## Testing

### Unit Tests

Run all unit tests:
```bash
cargo test
```

Run specific test:
```bash
cargo test test_parse_month
```

Run tests with output:
```bash
cargo test -- --nocapture
```

### Integration Testing

Test against local server:
```bash
# Terminal 1: Start dev server
npm run dev

# Terminal 2: Run curl tests
curl http://localhost:8787/apod/latest | jq .
```

### Debugging

Add debug prints:
```rust
use worker::console_log;

console_log!("Debug: value = {}", some_value);
```

View logs:
```bash
# In wrangler dev, logs appear in console
wrangler dev

# For deployed worker
wrangler tail
```

## Code Style

### Formatting

Format all code:
```bash
cargo fmt
```

Check formatting without modifying:
```bash
cargo fmt -- --check
```

### Linting

Run clippy:
```bash
cargo clippy
```

Fix auto-fixable lints:
```bash
cargo clippy --fix
```

Strict mode (used in CI):
```bash
cargo clippy -- -D warnings
```

### Naming Conventions

- **Files**: snake_case (e.g., `scraper.rs`)
- **Modules**: snake_case (e.g., `mod security`)
- **Structs/Enums**: PascalCase (e.g., `ApodEntry`)
- **Functions/Variables**: snake_case (e.g., `fetch_latest`)
- **Constants**: SCREAMING_SNAKE_CASE (e.g., `MAX_SIZE_KB`)

## Project Structure

```
nasa-mirror-api/
├── src/
│   ├── lib.rs           # Main entry, router
│   ├── models.rs        # Data structures
│   ├── parser.rs        # HTML parsing
│   ├── scraper.rs       # Fetching/caching
│   ├── handlers.rs      # Request handlers
│   └── security.rs      # Security layer
├── docs/
│   ├── openapi.yaml     # API spec
│   ├── architecture.md  # Architecture docs
│   └── examples.md      # Usage examples
├── .github/
│   └── workflows/
│       └── ci.yml       # CI/CD pipeline
├── Cargo.toml           # Rust deps
├── wrangler.toml        # CF Workers config
└── README.md
```

## Common Tasks

### Add a New Dependency

1. Add to `Cargo.toml`:
   ```toml
   [dependencies]
   new-crate = "1.0"
   ```

2. Rebuild:
   ```bash
   cargo build
   ```

### Add a New Route

1. Add handler in `handlers.rs`:
   ```rust
   pub async fn handle_new_route(req: Request, env: Env, ctx: Context) -> Result<Response> {
       // Implementation
       Response::from_json(&json!({"message": "Hello"}))
   }
   ```

2. Register in `lib.rs`:
   ```rust
   router
       .get_async("/new-route", |req, ctx| async move {
           handle_new_route(req, ctx.env, ctx).await
       })
   ```

3. Test:
   ```bash
   curl http://localhost:8787/new-route
   ```

### Update Parser Logic

1. Edit `parser.rs`
2. Add/update unit tests
3. Run tests:
   ```bash
   cargo test parser
   ```

### Modify Security Settings

Edit `security.rs` and update headers/validation logic.

### Change Cron Schedule

Edit `wrangler.toml`:
```toml
[triggers]
crons = ["0 8 * * *"]  # 8:00 AM UTC
```

Deploy:
```bash
wrangler deploy
```

## Deployment

### Development Deployment

```bash
# Deploy to workers.dev subdomain
wrangler deploy
```

### Production Deployment

1. Configure custom domain in `wrangler.toml`:
   ```toml
   [env.production]
   name = "nasa-mirror-api-production"
   route = "api.yourdomain.com/apod/*"
   ```

2. Deploy:
   ```bash
   wrangler deploy --env production
   ```

### Rollback

View deployments:
```bash
wrangler deployments list
```

Rollback:
```bash
wrangler rollback --message "Rollback to previous version"
```

## Monitoring

### View Logs

Real-time logs:
```bash
wrangler tail
```

Filter logs:
```bash
wrangler tail --status error
```

### Check KV Data

List keys:
```bash
wrangler kv:key list --namespace-id=<your-namespace-id>
```

Get value:
```bash
wrangler kv:key get "apod:latest" --namespace-id=<your-namespace-id>
```

Delete key:
```bash
wrangler kv:key delete "apod:latest" --namespace-id=<your-namespace-id>
```

### Trigger Cron Manually

```bash
wrangler publish
curl -X POST https://nasa-mirror-api.yourusername.workers.dev/__scheduled \
  -H "X-Cron-Trigger: manual"
```

(Note: May need to enable in Cloudflare dashboard)

## Troubleshooting

### Build Errors

**Error**: `error: failed to run custom build command for 'worker'`
- **Solution**: Ensure `worker-build` is installed: `cargo install worker-build`

**Error**: `target 'wasm32-unknown-unknown' not found`
- **Solution**: Add target: `rustup target add wasm32-unknown-unknown`

### Runtime Errors

**Error**: `KvStore binding not found: APOD_CACHE`
- **Solution**: Create KV namespaces and update `wrangler.toml`

**Error**: `Rate limit exceeded`
- **Solution**: Clear rate limit KV or wait for TTL expiration

### Local Development Issues

**Issue**: Changes not reflected
- **Solution**: Restart `wrangler dev`

**Issue**: Port 8787 already in use
- **Solution**: Kill process or use `wrangler dev --port 8788`

### Deployment Issues

**Error**: `Authentication error`
- **Solution**: Run `wrangler login` to authenticate

**Error**: `Namespace not found`
- **Solution**: Verify KV namespace IDs in `wrangler.toml`

## Security Checklist

Before deploying:
- [ ] All secrets in Cloudflare secrets (not `wrangler.toml`)
- [ ] Rate limiting configured
- [ ] Security headers enabled
- [ ] HTML sanitization active
- [ ] SSRF protection in place
- [ ] Request size limits set
- [ ] Logs don't contain PII
- [ ] Dependencies audited: `cargo audit`

## Performance Optimization

### Build Size

Check WASM size:
```bash
worker-build --release
ls -lh build/worker/
```

Optimize for size in `Cargo.toml`:
```toml
[profile.release]
opt-level = "z"
lto = true
strip = true
```

### KV Optimization

- Use longer TTLs for dated entries (7 days)
- Use shorter TTLs for latest entry (1 hour)
- Batch writes when possible

### Caching Strategy

- Implement stale-while-revalidate
- Use ETag for client-side caching
- Cache parsed HTML aggressively

## Contributing

See [CONTRIBUTING.md](../CONTRIBUTING.md) for guidelines.

### Quick Checklist

Before submitting PR:
- [ ] Tests pass: `cargo test`
- [ ] Code formatted: `cargo fmt`
- [ ] Lints pass: `cargo clippy`
- [ ] Security audit clean: `cargo audit`
- [ ] Documentation updated
- [ ] CHANGELOG.md updated

## Resources

- [Cloudflare Workers Docs](https://developers.cloudflare.com/workers/)
- [workers-rs Documentation](https://docs.rs/worker/)
- [Rust Book](https://doc.rust-lang.org/book/)
- [Wrangler CLI Reference](https://developers.cloudflare.com/workers/wrangler/)

## Support

- GitHub Issues: https://github.com/yourusername/nasa-mirror-api/issues
- Cloudflare Community: https://community.cloudflare.com/
- Rust Forum: https://users.rust-lang.org/
