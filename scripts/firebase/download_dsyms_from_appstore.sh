#!/usr/bin/env bash

# Copyright (c) 2025 WorldWideWaves
# Download dSYMs from App Store Connect for a specific build
#
# Usage:
#   ./download_dsyms_from_appstore.sh <apple_id> <build_number> [version]

set -e

# Color output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
APP_IDENTIFIER="com.worldwidewaves"
DEFAULT_VERSION="1.0"

# Parse arguments
if [ $# -lt 2 ]; then
    echo -e "${RED}Usage: $0 <apple_id> <build_number> [version]${NC}"
    echo ""
    echo "Example:"
    echo "  $0 your.email@example.com 27"
    echo "  $0 your.email@example.com 27 1.0"
    echo ""
    exit 1
fi

APPLE_ID="$1"
BUILD_NUMBER="$2"
VERSION="${3:-$DEFAULT_VERSION}"

# Script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
OUTPUT_DIR="$PROJECT_ROOT/iosApp/dsyms_build${BUILD_NUMBER}"

echo -e "${BLUE}╔════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║    Download dSYMs from App Store Connect              ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "${YELLOW}Configuration:${NC}"
echo "  Apple ID:       $APPLE_ID"
echo "  App Identifier: $APP_IDENTIFIER"
echo "  Version:        $VERSION"
echo "  Build:          $BUILD_NUMBER"
echo "  Output Dir:     $OUTPUT_DIR"
echo ""

# Create output directory
mkdir -p "$OUTPUT_DIR"

echo -e "${BLUE}📥 Downloading dSYMs from App Store Connect...${NC}"
echo -e "${YELLOW}Note: You may be prompted for your Apple ID password or 2FA code${NC}"
echo ""

# Download dSYMs using Fastlane
cd "$PROJECT_ROOT/iosApp"

if fastlane run download_dsyms \
    username:"$APPLE_ID" \
    app_identifier:"$APP_IDENTIFIER" \
    version:"$VERSION" \
    build_number:"$BUILD_NUMBER" \
    output_directory:"$OUTPUT_DIR"; then

    echo ""
    echo -e "${GREEN}✅ dSYMs downloaded successfully!${NC}"
    echo ""

    # List downloaded files
    echo -e "${BLUE}📂 Downloaded files:${NC}"
    ls -lh "$OUTPUT_DIR"
    echo ""

    # Show UUIDs
    echo -e "${BLUE}🔍 dSYM UUIDs:${NC}"
    for dsym in "$OUTPUT_DIR"/*.dSYM; do
        if [ -d "$dsym" ]; then
            echo -e "${YELLOW}$(basename "$dsym"):${NC}"
            find "$dsym" -name "*.dwarf" -o -name "worldwidewaves" -o -name "Shared" 2>/dev/null | while read -r dwarf; do
                if [ -f "$dwarf" ]; then
                    dwarfdump --uuid "$dwarf" 2>/dev/null | grep UUID || true
                fi
            done
        fi
    done
    echo ""

    # Offer to upload to Crashlytics
    echo -e "${YELLOW}🚀 Upload these dSYMs to Firebase Crashlytics?${NC}"
    echo "Run:"
    echo -e "${GREEN}  $SCRIPT_DIR/upload_ios_dsyms.sh $OUTPUT_DIR/*.dSYM${NC}"
    echo ""

else
    echo -e "${RED}❌ Failed to download dSYMs${NC}"
    echo ""
    echo "Common issues:"
    echo "  1. Wrong Apple ID or password"
    echo "  2. 2FA not completed"
    echo "  3. Build not uploaded to App Store Connect yet"
    echo "  4. Insufficient permissions in App Store Connect"
    echo ""
    exit 1
fi
