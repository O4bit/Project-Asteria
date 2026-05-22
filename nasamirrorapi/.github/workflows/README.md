# GitHub Actions CI/CD

This directory contains the CI/CD pipeline configuration for the NASA Mirror API.

## Workflows

### `ci.yml` - Continuous Integration and Deployment

Runs on:
- Push to `main` or `develop` branches
- Pull requests to `main` or `develop`
- Manual workflow dispatch

#### Jobs

1. **test**
   - Checks code formatting (`cargo fmt`)
   - Runs linter (`cargo clippy`)
   - Executes unit tests (`cargo test`)
   - Runs in both debug and release modes

2. **build**
   - Builds the Worker for WASM target
   - Installs `worker-build` tool
   - Creates optimized release build
   - Uploads build artifacts
   - Shows build size information

3. **security-audit**
   - Runs `cargo audit` to check for known vulnerabilities
   - Scans Rust dependencies
   - Fails if vulnerabilities found

4. **deploy** (conditional)
   - Deploys to Cloudflare Workers
   - Only runs on:
     - Push to `main` branch (automatic)
     - Manual workflow dispatch with deploy input set to 'true'
   - Requires secrets: `CLOUDFLARE_API_TOKEN`, `CLOUDFLARE_ACCOUNT_ID`
   - Comments deployment URL on pull requests

5. **notify-failure** (conditional)
   - Runs only if previous jobs fail
   - Placeholder for notification logic (Slack, Discord, email, etc.)

## Setup

### Required Secrets

Configure these in your GitHub repository settings (Settings → Secrets and variables → Actions):

1. **CLOUDFLARE_API_TOKEN**
   - Create at: https://dash.cloudflare.com/profile/api-tokens
   - Required permissions:
     - Account: Workers Scripts (Edit)
     - Account: Workers KV Storage (Edit)
   - Recommended: Create a token specifically for CI/CD

2. **CLOUDFLARE_ACCOUNT_ID**
   - Find at: https://dash.cloudflare.com/
   - Click on "Workers & Pages"
   - Account ID is shown in the right sidebar

### How to Get Cloudflare Credentials

#### API Token
```bash
# Or create via Cloudflare dashboard:
# 1. Go to: https://dash.cloudflare.com/profile/api-tokens
# 2. Click "Create Token"
# 3. Use template "Edit Cloudflare Workers" or create custom:
#    - Account Settings: Workers Scripts (Edit)
#    - Account Settings: Workers KV Storage (Edit)
# 4. Copy the token immediately (shown only once)
# 5. Add to GitHub Secrets as CLOUDFLARE_API_TOKEN
```

#### Account ID
```bash
# Find in dashboard URL or:
wrangler whoami

# Or via API:
curl -X GET "https://api.cloudflare.com/client/v4/accounts" \
  -H "Authorization: Bearer YOUR_API_TOKEN" \
  | jq -r '.result[0].id'
```

## Caching Strategy

The workflow caches Cargo dependencies to speed up builds:

- **Cache key**: Based on `Cargo.lock` hash
- **Cached directories**:
  - `~/.cargo/bin/` - Cargo binaries
  - `~/.cargo/registry/` - Crate registry
  - `~/.cargo/git/` - Git dependencies
  - `target/` - Build artifacts

Cache is automatically invalidated when dependencies change.

## Manual Deployment

To manually trigger deployment:

1. Go to the Actions tab in your repository
2. Select "CI" workflow
3. Click "Run workflow"
4. Set "Deploy to Cloudflare Workers" to `true`
5. Click "Run workflow"

Or via GitHub CLI:
```bash
gh workflow run ci.yml -f deploy=true
```

## Customization

### Change Rust Version

Edit the `RUST_VERSION` environment variable in `ci.yml`:
```yaml
env:
  RUST_VERSION: '1.75'  # Change to desired version
```

### Change Node.js Version

Edit the Node.js setup step:
```yaml
- name: Setup Node.js
  uses: actions/setup-node@v4
  with:
    node-version: '20'  # Change to desired version
```

### Add Notification

Edit the `notify-failure` job:
```yaml
- name: Send notification
  run: |
    # Example: Slack notification
    curl -X POST ${{ secrets.SLACK_WEBHOOK_URL }} \
      -H 'Content-Type: application/json' \
      -d '{"text":"CI pipeline failed!"}'
```

### Skip Deployment on Push

Comment out or modify the deploy job condition:
```yaml
if: |
  (github.event_name == 'workflow_dispatch' && github.event.inputs.deploy == 'true')
  # Removed: (github.event_name == 'push' && github.ref == 'refs/heads/main')
```

## Workflow Status Badges

Add to README.md:
```markdown
[![CI](https://github.com/yourusername/nasa-mirror-api/workflows/CI/badge.svg)](https://github.com/yourusername/nasa-mirror-api/actions)
```

## Troubleshooting

### Build Fails with "worker-build not found"

The workflow installs `worker-build`, but if it fails:
1. Check Rust toolchain version
2. Verify Cargo cache is working
3. Try clearing cache (in Actions settings)

### Deploy Fails with "Authentication error"

1. Verify `CLOUDFLARE_API_TOKEN` is correct
2. Check token permissions
3. Ensure token hasn't expired
4. Verify `CLOUDFLARE_ACCOUNT_ID` is correct

### Tests Fail

Check the test logs:
1. Click on the failed workflow run
2. Click on the "test" job
3. Expand the "Run tests" step
4. Review error messages

### Deploy Succeeds but Worker Not Updated

1. Check deployment logs for errors
2. Verify namespace IDs in `wrangler.toml`
3. Check Cloudflare dashboard for deployment status
4. Try manual deployment: `wrangler deploy`

## Performance

Typical run times:
- **test**: 2-3 minutes
- **build**: 3-5 minutes (first run), 1-2 minutes (cached)
- **security-audit**: 1 minute
- **deploy**: 1-2 minutes

Total: ~5-10 minutes for full pipeline

## Best Practices

1. **Always run tests locally first**: `cargo test`
2. **Format code before committing**: `cargo fmt`
3. **Fix clippy warnings**: `cargo clippy`
4. **Review security audits**: `cargo audit`
5. **Test deployment in preview first** before production
6. **Monitor deployment logs** for issues
7. **Keep secrets secure** - never commit them
8. **Update dependencies regularly** to get security fixes

## Additional Workflows

You can add more workflows for:
- Scheduled security audits (weekly)
- Dependency updates (Dependabot)
- Performance benchmarks
- Scheduled smoke tests
- Release automation

## Resources

- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [Cloudflare Workers Actions](https://github.com/cloudflare/wrangler-action)
- [Rust Setup Action](https://github.com/dtolnay/rust-toolchain)
- [Cargo Cache Action](https://github.com/actions/cache)
