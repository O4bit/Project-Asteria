#!/usr/bin/env pwsh
# Quick Build & Test Script for NASA Mirror API (PowerShell)

Write-Host "🔨 NASA Mirror API - Build & Test" -ForegroundColor Cyan
Write-Host "==================================`n" -ForegroundColor Cyan

# Check if we're in the right directory
if (!(Test-Path "Cargo.toml")) {
    Write-Host "❌ Error: Cargo.toml not found. Run this from the project root." -ForegroundColor Red
    exit 1
}

# Clean previous builds
Write-Host "🧹 Cleaning previous builds..." -ForegroundColor Yellow
cargo clean 2>$null

# Format code
Write-Host "`n📐 Formatting code..." -ForegroundColor Yellow
cargo fmt --all
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Formatting failed" -ForegroundColor Red
    exit 1
}
Write-Host "✅ Code formatted" -ForegroundColor Green

# Lint with clippy
Write-Host "`n🔍 Linting with clippy..." -ForegroundColor Yellow
cargo clippy --all-targets --all-features -- -D warnings
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Clippy found issues" -ForegroundColor Red
    exit 1
}
Write-Host "✅ Clippy checks passed" -ForegroundColor Green

# Run tests
Write-Host "`n🧪 Running tests..." -ForegroundColor Yellow
cargo test --verbose
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Tests failed" -ForegroundColor Red
    exit 1
}
Write-Host "✅ All tests passed" -ForegroundColor Green

# Security audit
Write-Host "`n🔒 Running security audit..." -ForegroundColor Yellow
cargo audit 2>$null
if ($LASTEXITCODE -ne 0) {
    Write-Host "⚠️  Security audit found vulnerabilities (or cargo-audit not installed)" -ForegroundColor Yellow
    Write-Host "   Install: cargo install cargo-audit" -ForegroundColor Gray
} else {
    Write-Host "✅ No known vulnerabilities" -ForegroundColor Green
}

# Build for WASM
Write-Host "`n🏗️  Building Worker (WASM)..." -ForegroundColor Yellow
$workerBuild = Get-Command worker-build -ErrorAction SilentlyContinue
if (!$workerBuild) {
    Write-Host "⚠️  worker-build not found. Installing..." -ForegroundColor Yellow
    cargo install worker-build --locked
}

worker-build --release
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Worker build failed" -ForegroundColor Red
    exit 1
}
Write-Host "✅ Worker built successfully" -ForegroundColor Green

# Show build size
if (Test-Path "build/worker") {
    $buildSize = (Get-ChildItem -Recurse build/worker | Measure-Object -Property Length -Sum).Sum / 1KB
    Write-Host "`n📦 Build size: $([math]::Round($buildSize, 2)) KB" -ForegroundColor Cyan
}

Write-Host "`n✅ Build & Test Complete!" -ForegroundColor Green
Write-Host "`n🚀 Next steps:" -ForegroundColor Cyan
Write-Host "   • Test locally: npm run dev" -ForegroundColor White
Write-Host "   • Deploy: npm run deploy" -ForegroundColor White
Write-Host "`n🎉 Ready to deploy!" -ForegroundColor Green
