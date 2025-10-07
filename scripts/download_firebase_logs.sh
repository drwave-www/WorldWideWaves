#!/usr/bin/env bash

# Copyright 2025 DrWave
#
# WorldWideWaves - Download Firebase Test Lab Logs
#
# Downloads test logs and logcat files from Firebase Test Lab results

set -e

# Configuration
RESULTS_BUCKET="${FIREBASE_RESULTS_BUCKET:-world-wide-waves-results}"
OUTPUT_DIR="test_results/firebase_logs"

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${GREEN}📥 Downloading Firebase Test Lab Logs${NC}"
echo "========================================="
echo ""

# Check if gsutil is available
if ! command -v gsutil &> /dev/null; then
    echo -e "${YELLOW}❌ Error: gsutil not found${NC}"
    echo "Install Google Cloud SDK: brew install google-cloud-sdk"
    exit 1
fi

# Create output directory
mkdir -p "${OUTPUT_DIR}/android"
mkdir -p "${OUTPUT_DIR}/ios"

# Get latest test results
echo -e "${GREEN}📂 Finding latest test results...${NC}"

LATEST_ANDROID=$(gsutil ls "gs://${RESULTS_BUCKET}/android/" 2>/dev/null | tail -1)
LATEST_IOS=$(gsutil ls "gs://${RESULTS_BUCKET}/ios/" 2>/dev/null | tail -1)

if [[ -z "$LATEST_ANDROID" ]] && [[ -z "$LATEST_IOS" ]]; then
    echo -e "${YELLOW}⚠️  No test results found in gs://${RESULTS_BUCKET}${NC}"
    echo "Have you run the tests yet?"
    echo "  ./scripts/run_all_firebase_tests.sh"
    exit 1
fi

# Download Android logs
if [[ -n "$LATEST_ANDROID" ]]; then
    echo ""
    echo -e "${GREEN}📱 Downloading Android logs...${NC}"
    echo "  From: ${LATEST_ANDROID}"

    # Download all artifacts
    gsutil -m cp -r "${LATEST_ANDROID}**" "${OUTPUT_DIR}/android/" 2>/dev/null || {
        echo -e "${YELLOW}⚠️  Android logs download failed${NC}"
    }

    # Find logcat files
    LOGCAT_COUNT=$(find "${OUTPUT_DIR}/android" -name "*logcat*" 2>/dev/null | wc -l | tr -d ' ')
    echo -e "${GREEN}  Found ${LOGCAT_COUNT} logcat files${NC}"
fi

# Download iOS logs
if [[ -n "$LATEST_IOS" ]]; then
    echo ""
    echo -e "${GREEN}🍎 Downloading iOS logs...${NC}"
    echo "  From: ${LATEST_IOS}"

    # Download all artifacts
    gsutil -m cp -r "${LATEST_IOS}**" "${OUTPUT_DIR}/ios/" 2>/dev/null || {
        echo -e "${YELLOW}⚠️  iOS logs download failed${NC}"
    }

    # Find log files
    LOG_COUNT=$(find "${OUTPUT_DIR}/ios" -name "*.log" 2>/dev/null | wc -l | tr -d ' ')
    echo -e "${GREEN}  Found ${LOG_COUNT} log files${NC}"
fi

echo ""
echo -e "${GREEN}✅ Log download complete!${NC}"
echo ""
echo -e "${GREEN}📁 Logs location:${NC}"
echo "  Android: ${OUTPUT_DIR}/android/"
echo "  iOS: ${OUTPUT_DIR}/ios/"
echo ""
echo -e "${GREEN}🔍 View logcat files:${NC}"
find "${OUTPUT_DIR}" -name "*logcat*" 2>/dev/null | head -5 | while read -r file; do
    echo "  ${file}"
done
echo ""
