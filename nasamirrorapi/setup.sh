#!/bin/bash

# NASA Mirror API - Quick Start Script (Bash version for Unix/Linux/macOS)

set -e

echo "🚀 NASA Mirror API - Quick Start Setup"
echo "======================================"
echo ""

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Check prerequisites
echo -e "${YELLOW}📋 Checking prerequisites...${NC}"

# Check Rust
if command -v rustc &> /dev/null; then
    RUST_VERSION=$(rustc --version)
    echo -e "${GREEN}✅ Rust: $RUST_VERSION${NC}"
else
    echo -e "${RED}❌ Rust not found. Install from: https://rustup.rs/${NC}"
    exit 1
fi

# Check wasm32 target
if rustup target list --installed | grep -q "wasm32-unknown-unknown"; then
    echo -e "${GREEN}✅ wasm32 target installed${NC}"
else
    echo -e "${YELLOW}⚠️  Installing wasm32 target...${NC}"
    rustup target add wasm32-unknown-unknown
fi

# Check Node.js
if command -v node &> /dev/null; then
    NODE_VERSION=$(node --version)
    echo -e "${GREEN}✅ Node.js: $NODE_VERSION${NC}"
else
    echo -e "${RED}❌ Node.js not found. Install from: https://nodejs.org/${NC}"
    exit 1
fi

# Check wrangler
if command -v wrangler &> /dev/null; then
    WRANGLER_VERSION=$(wrangler --version)
    echo -e "${GREEN}✅ Wrangler: $WRANGLER_VERSION${NC}"
else
    echo -e "${YELLOW}⚠️  Installing Wrangler...${NC}"
    npm install -g wrangler
fi

# Check worker-build
if cargo install --list | grep -q "worker-build"; then
    echo -e "${GREEN}✅ worker-build installed${NC}"
else
    echo -e "${YELLOW}⚠️  Installing worker-build (this may take a few minutes)...${NC}"
    cargo install worker-build --locked
fi

echo ""
echo -e "${YELLOW}📦 Installing dependencies...${NC}"
npm install

echo ""
echo -e "${YELLOW}🔨 Building project...${NC}"
cargo build

echo ""
echo -e "${GREEN}✅ Setup complete!${NC}"
echo ""
echo -e "${CYAN}📝 Next steps:${NC}"
echo "1. Create KV namespaces:"
echo "   wrangler kv:namespace create 'APOD_CACHE'"
echo "   wrangler kv:namespace create 'RATE_LIMIT'"
echo "   wrangler kv:namespace create 'APOD_CACHE' --preview"
echo "   wrangler kv:namespace create 'RATE_LIMIT' --preview"
echo ""
echo "2. Update wrangler.toml with the namespace IDs"
echo ""
echo "3. Start development server:"
echo "   npm run dev"
echo ""
echo "4. Test the API:"
echo "   curl http://localhost:8787/apod/latest"
echo ""
echo -e "${CYAN}📚 Documentation:${NC}"
echo "   - README.md - Overview and quick start"
echo "   - docs/DEVELOPMENT.md - Development guide"
echo "   - docs/examples.md - API examples"
echo "   - docs/architecture.md - Architecture details"
echo ""
echo -e "${GREEN}🎉 Happy coding!${NC}"
