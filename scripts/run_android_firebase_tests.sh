#!/usr/bin/env bash

# Copyright 2025 DrWave
#
# WorldWideWaves Firebase Test Lab - Android E2E Tests Runner
#
# This script builds and runs Android E2E tests on Firebase Test Lab.
# It supports multiple device configurations and captures screenshots.

set -e

# Configuration
PROJECT_ID="${FIREBASE_PROJECT_ID:-worldwidewaves-test}"
RESULTS_BUCKET="${FIREBASE_RESULTS_BUCKET:-worldwidewaves-test-results}"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
RESULTS_DIR="android/${TIMESTAMP}"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}🚀 Firebase Test Lab - Android E2E Tests${NC}"
echo "========================================"
echo ""

# Check if gcloud is installed
if ! command -v gcloud &> /dev/null; then
    echo -e "${RED}❌ Error: gcloud CLI is not installed${NC}"
    echo "Install it: brew install google-cloud-sdk"
    exit 1
fi

# Check if authenticated
if ! gcloud auth list --filter=status:ACTIVE --format="value(account)" &> /dev/null; then
    echo -e "${YELLOW}⚠️  Not authenticated with gcloud${NC}"
    echo "Authenticating..."
    gcloud auth login
fi

# Set project
echo -e "${GREEN}📝 Setting Firebase project: ${PROJECT_ID}${NC}"
gcloud config set project "${PROJECT_ID}"

# Build APKs
echo ""
echo -e "${GREEN}🔨 Building Android APKs...${NC}"
./gradlew :composeApp:assembleDebug
./gradlew :composeApp:assembleDebugAndroidTest

# Verify APKs exist
APP_APK="composeApp/build/outputs/apk/debug/composeApp-debug.apk"
TEST_APK="composeApp/build/outputs/apk/androidTest/debug/composeApp-debug-androidTest.apk"

if [[ ! -f "$APP_APK" ]]; then
    echo -e "${RED}❌ Error: App APK not found at ${APP_APK}${NC}"
    exit 1
fi

if [[ ! -f "$TEST_APK" ]]; then
    echo -e "${RED}❌ Error: Test APK not found at ${TEST_APK}${NC}"
    exit 1
fi

echo -e "${GREEN}✅ APKs built successfully${NC}"
echo "  App APK: ${APP_APK}"
echo "  Test APK: ${TEST_APK}"

# Device matrix
echo ""
echo -e "${GREEN}📱 Device Matrix:${NC}"
echo "  - Pixel 6 (API 33)"
echo "  - Pixel 5 (API 31)"
echo "  - Samsung Galaxy S22 (API 33)"
echo ""

# Run tests on Firebase Test Lab
echo -e "${GREEN}🧪 Running tests on Firebase Test Lab...${NC}"
if gcloud firebase test android run \
  --type instrumentation \
  --app "${APP_APK}" \
  --test "${TEST_APK}" \
  --device model=redfin,version=33,locale=en_US,orientation=portrait \
  --device model=redfin,version=31,locale=en_US,orientation=portrait \
  --device model=b0q,version=33,locale=en_US,orientation=portrait \
  --timeout 20m \
  --results-bucket="${RESULTS_BUCKET}" \
  --results-dir="${RESULTS_DIR}" \
  --environment-variables coverage=true,clearPackageData=true \
  --directories-to-pull /sdcard/Android/data/com.worldwidewaves/files/e2e_screenshots \
  --project="${PROJECT_ID}"; then
    echo ""
    echo -e "${GREEN}✅ Android tests completed successfully!${NC}"
else
    echo ""
    echo -e "${RED}❌ Android tests failed${NC}"
    exit 1
fi

# Output results location
echo ""
echo -e "${GREEN}📊 Test Results:${NC}"
echo "  Bucket: gs://${RESULTS_BUCKET}/${RESULTS_DIR}"
echo "  Console: https://console.firebase.google.com/project/${PROJECT_ID}/testlab/histories"
echo ""
echo -e "${GREEN}📸 Screenshots:${NC}"
echo "  Download command:"
echo "    gsutil -m cp -r gs://${RESULTS_BUCKET}/${RESULTS_DIR}/*/artifacts/sdcard/ ./test_results/android/"
echo ""
echo -e "${GREEN}✨ Done!${NC}"
