#!/bin/bash
# Quick Build & Test Script for NASA Mirror API (Bash)

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
WHITE='\033[1;37m'
NC='\033[0m' # No Color

echo -e "${CYAN}🔨 NASA Mirror API - Build & Test${NC}"
echo -e "${CYAN}==================================${NC}\n"

# Check if we're in the right directory
if [ ! -f "Cargo.toml" ]; then
    echo -e "${RED}❌ Error: Cargo.toml not found. Run this from the project root.${NC}"
    exit 1
fi

# Clean previous builds
echo -e "${YELLOW}🧹 Cleaning previous builds...${NC}"
cargo clean 2>/dev/null || true

# Format code
echo -e "\n${YELLOW}📐 Formatting code...${NC}"
if cargo fmt --all; then
    echo -e "${GREEN}✅ Code formatted${NC}"
else
    echo -e "${RED}❌ Formatting failed${NC}"
    exit 1
fi

# Lint with clippy
echo -e "\n${YELLOW}🔍 Linting with clippy...${NC}"
if cargo clippy --all-targets --all-features -- -D warnings; then
    echo -e "${GREEN}✅ Clippy checks passed${NC}"
else
    echo -e "${RED}❌ Clippy found issues${NC}"
    exit 1
fi

# Run tests
echo -e "\n${YELLOW}🧪 Running tests...${NC}"
if cargo test --verbose; then
    echo -e "${GREEN}✅ All tests passed${NC}"
else
    echo -e "${RED}❌ Tests failed${NC}"
    exit 1
fi

# Security audit
echo -e "\n${YELLOW}🔒 Running security audit...${NC}"
if command -v cargo-audit &> /dev/null; then
    if cargo audit; then
        echo -e "${GREEN}✅ No known vulnerabilities${NC}"
    else
        echo -e "${YELLOW}⚠️  Security audit found vulnerabilities${NC}"
    fi
else
    echo -e "${YELLOW}⚠️  cargo-audit not installed. Install: cargo install cargo-audit${NC}"
fi

# Build for WASM
echo -e "\n${YELLOW}🏗️  Building Worker (WASM)...${NC}"
if ! command -v worker-build &> /dev/null; then
    echo -e "${YELLOW}⚠️  worker-build not found. Installing...${NC}"
    cargo install worker-build --locked
fi

if worker-build --release; then
    echo -e "${GREEN}✅ Worker built successfully${NC}"
else
    echo -e "${RED}❌ Worker build failed${NC}"
    exit 1
fi

# Show build size
if [ -d "build/worker" ]; then
    BUILD_SIZE=$(du -sh build/worker | cut -f1)
    echo -e "\n${CYAN}📦 Build size: $BUILD_SIZE${NC}"
fi

echo -e "\n${GREEN}✅ Build & Test Complete!${NC}"
echo -e "\n${CYAN}🚀 Next steps:${NC}"
echo -e "   ${WHITE}• Test locally: npm run dev${NC}"
echo -e "   ${WHITE}• Deploy: npm run deploy${NC}"
echo -e "\n${GREEN}🎉 Ready to deploy!${NC}"
