#!/usr/bin/env bash

# Copyright 2025 DrWave
#
# WorldWideWaves Firebase Test Lab - Complete E2E Test Suite Runner
#
# This script runs both Android and iOS E2E tests on Firebase Test Lab.

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}  WorldWideWaves - Firebase Test Lab E2E Test Suite${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

# Configuration
PROJECT_ID="${FIREBASE_PROJECT_ID:-world-wide-waves}"

# Authenticate with Firebase (if needed)
if ! gcloud auth list --filter=status:ACTIVE --format="value(account)" &> /dev/null; then
    echo -e "${YELLOW}⚠️  Not authenticated with gcloud${NC}"
    echo "Authenticating..."
    gcloud auth login
fi

echo -e "${GREEN}📝 Firebase Project: ${PROJECT_ID}${NC}"
gcloud config set project "${PROJECT_ID}"
echo ""

# Track results
ANDROID_SUCCESS=false
IOS_SUCCESS=false

# Run Android tests
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}  📱 ANDROID TESTS${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

if ./scripts/run_android_firebase_tests.sh; then
    ANDROID_SUCCESS=true
    echo -e "${GREEN}✅ Android tests completed${NC}"
else
    echo -e "${RED}❌ Android tests failed${NC}"
fi

echo ""
echo ""

# Run iOS tests
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}  🍎 iOS TESTS${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

if ./scripts/run_ios_firebase_tests.sh; then
    IOS_SUCCESS=true
    echo -e "${GREEN}✅ iOS tests completed${NC}"
else
    echo -e "${RED}❌ iOS tests failed${NC}"
fi

echo ""
echo ""

# Summary
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}  📊 TEST SUMMARY${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

if $ANDROID_SUCCESS; then
    echo -e "${GREEN}✅ Android: PASSED${NC}"
else
    echo -e "${RED}❌ Android: FAILED${NC}"
fi

if $IOS_SUCCESS; then
    echo -e "${GREEN}✅ iOS: PASSED${NC}"
else
    echo -e "${RED}❌ iOS: FAILED${NC}"
fi

echo ""
echo -e "${GREEN}🌐 Firebase Test Lab Console:${NC}"
echo "  https://console.firebase.google.com/project/${PROJECT_ID}/testlab/histories"
echo ""

# Next steps
echo -e "${YELLOW}📥 Download Screenshots:${NC}"
echo "  ./scripts/collect_firebase_screenshots.sh"
echo ""
echo -e "${YELLOW}📈 Generate Report:${NC}"
echo "  python3 scripts/generate_test_report.py"
echo ""

# Exit with appropriate code
if $ANDROID_SUCCESS && $IOS_SUCCESS; then
    echo -e "${GREEN}✨ All tests passed!${NC}"
    exit 0
else
    echo -e "${RED}⚠️  Some tests failed${NC}"
    exit 1
fi
