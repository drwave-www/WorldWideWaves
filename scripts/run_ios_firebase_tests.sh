#!/usr/bin/env bash

# Copyright 2025 DrWave
#
# WorldWideWaves Firebase Test Lab - iOS E2E Tests Runner
#
# This script builds and runs iOS E2E tests on Firebase Test Lab.
# It supports multiple device configurations and captures screenshots.

set -e

# Configuration
PROJECT_ID="${FIREBASE_PROJECT_ID:-worldwidewaves-test}"
RESULTS_BUCKET="${FIREBASE_RESULTS_BUCKET:-worldwidewaves-test-results}"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
RESULTS_DIR="ios/${TIMESTAMP}"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}🚀 Firebase Test Lab - iOS E2E Tests${NC}"
echo "========================================"
echo ""

# Check if gcloud is installed
if ! command -v gcloud &> /dev/null; then
    echo -e "${RED}❌ Error: gcloud CLI is not installed${NC}"
    echo "Install it: brew install google-cloud-sdk"
    exit 1
fi

# Check if xcodebuild is available
if ! command -v xcodebuild &> /dev/null; then
    echo -e "${RED}❌ Error: xcodebuild not found${NC}"
    echo "Ensure Xcode is installed and command line tools are configured"
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

# Build iOS app and test bundle
echo ""
echo -e "${GREEN}🔨 Building iOS app for testing...${NC}"
cd iosApp

# Clean previous builds
rm -rf ./build

# Build for testing
if ! xcodebuild build-for-testing \
  -project worldwidewaves.xcodeproj \
  -scheme worldwidewaves \
  -sdk iphoneos \
  -derivedDataPath ./build \
  -destination generic/platform=iOS; then
    echo -e "${RED}❌ Error: iOS build failed${NC}"
    cd ..
    exit 1
fi

echo -e "${GREEN}✅ iOS build successful${NC}"

# Create test bundle zip
echo ""
echo -e "${GREEN}📦 Creating test bundle...${NC}"
cd build/Build/Products

# Find the .app and test runner
APP_PATH=$(find . -name "worldwidewaves.app" -type d | head -1)
TEST_RUNNER_PATH=$(find . -name "worldwidewavesUITests-Runner.app" -type d | head -1)

if [[ -z "$APP_PATH" ]] || [[ -z "$TEST_RUNNER_PATH" ]]; then
    echo -e "${RED}❌ Error: Could not find app or test runner${NC}"
    cd ../../../..
    exit 1
fi

echo "  App: ${APP_PATH}"
echo "  Test Runner: ${TEST_RUNNER_PATH}"

# Create zip
ZIP_NAME="worldwidewaves_tests_${TIMESTAMP}.zip"
zip -r "${ZIP_NAME}" "${APP_PATH}" "${TEST_RUNNER_PATH}"

echo -e "${GREEN}✅ Test bundle created: ${ZIP_NAME}${NC}"

# Move back to project root
cd ../../../..

# Device matrix - 5 devices with different form factors and iOS versions
echo ""
echo -e "${GREEN}📱 Device Matrix (5 devices):${NC}"
echo "  1. iPhone 15 Pro (iOS 18.0) - Latest flagship"
echo "  2. iPhone 14 Pro (iOS 16.6) - Previous flagship"
echo "  3. iPhone 13 Pro (iOS 16.6) - Older flagship"
echo "  4. iPhone 8 (iOS 16.6) - Legacy small screen"
echo "  5. iPad (10th gen) (iOS 16.6) - Tablet"
echo ""

# Run tests on Firebase Test Lab
echo -e "${GREEN}🧪 Running tests on Firebase Test Lab...${NC}"
if gcloud firebase test ios run \
  --test "iosApp/build/Build/Products/${ZIP_NAME}" \
  --device model=iphone15pro,version=18.0,locale=en_US,orientation=portrait \
  --device model=iphone14pro,version=16.6,locale=en_US,orientation=portrait \
  --device model=iphone13pro,version=16.6,locale=en_US,orientation=portrait \
  --device model=iphone8,version=16.6,locale=en_US,orientation=portrait \
  --device model=ipad10,version=16.6,locale=en_US,orientation=portrait \
  --timeout 20m \
  --results-bucket="${RESULTS_BUCKET}" \
  --results-dir="${RESULTS_DIR}" \
  --project="${PROJECT_ID}"; then
    echo ""
    echo -e "${GREEN}✅ iOS tests completed successfully!${NC}"
else
    echo ""
    echo -e "${RED}❌ iOS tests failed${NC}"
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
echo "    gsutil -m cp -r gs://${RESULTS_BUCKET}/${RESULTS_DIR}/*/artifacts/ ./test_results/ios/"
echo ""
echo -e "${GREEN}✨ Done!${NC}"
