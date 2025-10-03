#!/usr/bin/env bash

# Copyright 2025 DrWave
#
# WorldWideWaves Firebase Test Lab - Screenshot Collection Script
#
# Downloads screenshots from Firebase Test Lab results

set -e

# Configuration
RESULTS_BUCKET="${FIREBASE_RESULTS_BUCKET:-worldwidewaves-test-results}"
OUTPUT_DIR="test_results/firebase"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}📸 Collecting Firebase Test Lab Screenshots${NC}"
echo "==========================================="
echo ""

# Check if gsutil is available
if ! command -v gsutil &> /dev/null; then
    echo -e "${RED}❌ Error: gsutil not found${NC}"
    echo "Install Google Cloud SDK: brew install google-cloud-sdk"
    exit 1
fi

# Create output directories
mkdir -p "${OUTPUT_DIR}/android"
mkdir -p "${OUTPUT_DIR}/ios"

# Get latest test results
echo -e "${GREEN}📂 Finding latest test results...${NC}"

LATEST_ANDROID=$(gsutil ls "gs://${RESULTS_BUCKET}/android/" | tail -1)
LATEST_IOS=$(gsutil ls "gs://${RESULTS_BUCKET}/ios/" | tail -1)

if [[ -z "$LATEST_ANDROID" ]]; then
    echo -e "${YELLOW}⚠️  No Android results found${NC}"
else
    echo "  Android: ${LATEST_ANDROID}"
fi

if [[ -z "$LATEST_IOS" ]]; then
    echo -e "${YELLOW}⚠️  No iOS results found${NC}"
else
    echo "  iOS: ${LATEST_IOS}"
fi

echo ""

# Download Android screenshots
if [[ -n "$LATEST_ANDROID" ]]; then
    echo -e "${GREEN}📱 Downloading Android screenshots...${NC}"
    gsutil -m cp -r "${LATEST_ANDROID}*/artifacts/sdcard/" "${OUTPUT_DIR}/android/" 2>/dev/null || {
        echo -e "${YELLOW}⚠️  No Android screenshots found or download failed${NC}"
    }

    # Count files
    ANDROID_COUNT=$(find "${OUTPUT_DIR}/android" -type f -name "*.png" 2>/dev/null | wc -l | tr -d ' ')
    echo -e "${GREEN}  Downloaded ${ANDROID_COUNT} Android screenshots${NC}"
fi

echo ""

# Download iOS screenshots
if [[ -n "$LATEST_IOS" ]]; then
    echo -e "${GREEN}🍎 Downloading iOS screenshots...${NC}"
    gsutil -m cp -r "${LATEST_IOS}*/artifacts/" "${OUTPUT_DIR}/ios/" 2>/dev/null || {
        echo -e "${YELLOW}⚠️  No iOS screenshots found or download failed${NC}"
    }

    # Count files
    IOS_COUNT=$(find "${OUTPUT_DIR}/ios" -type f -name "*.png" 2>/dev/null | wc -l | tr -d ' ')
    echo -e "${GREEN}  Downloaded ${IOS_COUNT} iOS screenshots${NC}"
fi

echo ""
echo -e "${GREEN}✅ Screenshot collection complete!${NC}"
echo ""
echo -e "${GREEN}📁 Screenshots location:${NC}"
echo "  Android: ${OUTPUT_DIR}/android/"
echo "  iOS: ${OUTPUT_DIR}/ios/"
echo ""
echo -e "${YELLOW}📊 Next step:${NC}"
echo "  Generate HTML report: python3 scripts/generate_test_report.py"
echo ""
