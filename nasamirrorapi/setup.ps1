# NASA Mirror API - Quick Start Script

Write-Host "🚀 NASA Mirror API - Quick Start Setup" -ForegroundColor Cyan
Write-Host "======================================`n" -ForegroundColor Cyan

# Check prerequisites
Write-Host "📋 Checking prerequisites..." -ForegroundColor Yellow

# Check Rust
$rustVersion = & rustc --version 2>$null
if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ Rust: $rustVersion" -ForegroundColor Green
} else {
    Write-Host "❌ Rust not found. Install from: https://rustup.rs/" -ForegroundColor Red
    exit 1
}

# Check wasm32 target
$wasmTarget = & rustup target list --installed 2>$null | Select-String "wasm32-unknown-unknown"
if ($wasmTarget) {
    Write-Host "✅ wasm32 target installed" -ForegroundColor Green
} else {
    Write-Host "⚠️  Installing wasm32 target..." -ForegroundColor Yellow
    rustup target add wasm32-unknown-unknown
}

# Check Node.js
$nodeVersion = & node --version 2>$null
if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ Node.js: $nodeVersion" -ForegroundColor Green
} else {
    Write-Host "❌ Node.js not found. Install from: https://nodejs.org/" -ForegroundColor Red
    exit 1
}

# Check wrangler
$wranglerVersion = & wrangler --version 2>$null
if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ Wrangler: $wranglerVersion" -ForegroundColor Green
} else {
    Write-Host "⚠️  Installing Wrangler..." -ForegroundColor Yellow
    npm install -g wrangler
}

# Check worker-build
$workerBuild = & cargo install --list 2>$null | Select-String "worker-build"
if ($workerBuild) {
    Write-Host "✅ worker-build installed" -ForegroundColor Green
} else {
    Write-Host "⚠️  Installing worker-build (this may take a few minutes)..." -ForegroundColor Yellow
    cargo install worker-build --locked
}

Write-Host "`n📦 Installing dependencies..." -ForegroundColor Yellow
npm install

Write-Host "`n🔨 Building project..." -ForegroundColor Yellow
cargo build

Write-Host "`n✅ Setup complete!" -ForegroundColor Green
Write-Host "`n📝 Next steps:" -ForegroundColor Cyan
Write-Host "1. Create KV namespaces:" -ForegroundColor White
Write-Host "   wrangler kv:namespace create 'APOD_CACHE'" -ForegroundColor Gray
Write-Host "   wrangler kv:namespace create 'RATE_LIMIT'" -ForegroundColor Gray
Write-Host "   wrangler kv:namespace create 'APOD_CACHE' --preview" -ForegroundColor Gray
Write-Host "   wrangler kv:namespace create 'RATE_LIMIT' --preview" -ForegroundColor Gray
Write-Host "`n2. Update wrangler.toml with the namespace IDs" -ForegroundColor White
Write-Host "`n3. Start development server:" -ForegroundColor White
Write-Host "   npm run dev" -ForegroundColor Gray
Write-Host "`n4. Test the API:" -ForegroundColor White
Write-Host "   curl http://localhost:8787/apod/latest" -ForegroundColor Gray
Write-Host "`n📚 Documentation:" -ForegroundColor Cyan
Write-Host "   - README.md - Overview and quick start" -ForegroundColor Gray
Write-Host "   - docs/DEVELOPMENT.md - Development guide" -ForegroundColor Gray
Write-Host "   - docs/examples.md - API examples" -ForegroundColor Gray
Write-Host "   - docs/architecture.md - Architecture details" -ForegroundColor Gray
Write-Host "`n🎉 Happy coding!" -ForegroundColor Green
