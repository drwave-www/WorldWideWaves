#!/usr/bin/env bash

# Copyright 2025 DrWave
#
# WorldWideWaves - Test Setup Validation Script
#
# Validates that all components for E2E testing are properly configured

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}  WorldWideWaves - E2E Test Setup Validation${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

# Track validation status
ALL_PASSED=true

# Function to check and report status
check_item() {
    local description="$1"
    local command="$2"

    echo -n "  ${description}... "
    if eval "$command" > /dev/null 2>&1; then
        echo -e "${GREEN}✓${NC}"
        return 0
    else
        echo -e "${RED}✗${NC}"
        ALL_PASSED=false
        return 1
    fi
}

# 1. Check testTags in UI components
echo -e "${YELLOW}📝 Checking testTag Implementation${NC}"
check_item "FilterButton_All testTag" "grep -r 'testTag.*FilterButton_All' shared/src/commonMain/kotlin/com/worldwidewaves/shared/ui/components/"
check_item "FilterButton_Favorites testTag" "grep -r 'testTag.*FilterButton_Favorites' shared/src/commonMain/kotlin/com/worldwidewaves/shared/ui/components/"
check_item "EventsList testTag" "grep -r 'testTag.*EventsList' shared/src/commonMain/kotlin/com/worldwidewaves/shared/ui/components/"
check_item "JoinWaveButton testTag" "grep -r 'testTag.*JoinWaveButton' shared/src/commonMain/kotlin/com/worldwidewaves/shared/ui/components/"
check_item "AboutTab testTags" "grep -r 'testTag.*AboutTab' shared/src/commonMain/kotlin/com/worldwidewaves/shared/ui/screens/"
echo ""

# 2. Check debug build configuration
echo -e "${YELLOW}🔧 Checking Build Configuration${NC}"
check_item "Debug buildType exists" "grep -A 10 'debug {' composeApp/build.gradle.kts | grep -q 'ENABLE_SIMULATION_MODE'"
check_item "ENABLE_SIMULATION_MODE flag" "grep -q 'ENABLE_SIMULATION_MODE.*true' composeApp/build.gradle.kts"
echo ""

# 3. Check Android test files
echo -e "${YELLOW}📱 Checking Android Test Files${NC}"
check_item "BaseE2ETest.kt exists" "test -f composeApp/src/androidInstrumentedTest/kotlin/com/worldwidewaves/testing/BaseE2ETest.kt"
check_item "E2ETestHelpers.kt exists" "test -f composeApp/src/androidInstrumentedTest/kotlin/com/worldwidewaves/testing/E2ETestHelpers.kt"
check_item "CompleteWaveParticipationE2ETest.kt exists" "test -f composeApp/src/androidInstrumentedTest/kotlin/com/worldwidewaves/e2e/CompleteWaveParticipationE2ETest.kt"
check_item "Android test compiles" "./gradlew :composeApp:compileDebugAndroidTestKotlin --quiet"
echo ""

# 4. Check iOS test files
echo -e "${YELLOW}🍎 Checking iOS Test Files${NC}"
check_item "CompleteWaveParticipationUITest.swift exists" "test -f iosApp/worldwidewavesUITests/CompleteWaveParticipationUITest.swift"
check_item "XCUITestExtensions.swift exists" "test -f iosApp/worldwidewavesUITests/XCUITestExtensions.swift"
check_item "ScreenshotHelper.swift exists" "test -f iosApp/worldwidewavesUITests/ScreenshotHelper.swift"
check_item "iOS UI test README exists" "test -f iosApp/worldwidewavesUITests/README.md"
echo ""

# 5. Check Firebase Test Lab scripts
echo -e "${YELLOW}🔥 Checking Firebase Test Lab Scripts${NC}"
check_item "run_android_firebase_tests.sh exists" "test -x scripts/firebase/run_android_firebase_tests.sh"
check_item "run_ios_firebase_tests.sh exists" "test -x scripts/firebase/run_ios_firebase_tests.sh"
check_item "run_all_firebase_tests.sh exists" "test -x scripts/firebase/run_all_firebase_tests.sh"
check_item "collect_firebase_screenshots.sh exists" "test -x scripts/firebase/collect_firebase_screenshots.sh"
check_item "generate_test_report.py exists" "test -x scripts/generate_test_report.py"
echo ""

# 6. Check documentation
echo -e "${YELLOW}📚 Checking Documentation${NC}"
check_item "TODO_FIREBASE_UI.md exists" "test -f TODO_FIREBASE_UI.md"
check_item "FIREBASE_TEST_LAB_GUIDE.md exists" "test -f FIREBASE_TEST_LAB_GUIDE.md"
echo ""

# 7. Check iOS simulators
echo -e "${YELLOW}📲 Checking iOS Simulators${NC}"
if xcrun simctl list devices available 2>/dev/null | grep -q "iPhone"; then
    echo -e "  iOS simulators available... ${GREEN}✓${NC}"
    echo -e "    Available simulators:"
    xcrun simctl list devices available 2>/dev/null | grep -E "iPhone|iPad" | head -3 | sed 's/^/      /'
else
    echo -e "  iOS simulators available... ${RED}✗${NC}"
    ALL_PASSED=false
fi
echo ""

# 8. Check for build readiness
echo -e "${YELLOW}🏗️  Checking Build Readiness${NC}"
check_item "Android debug APK can be built" "./gradlew :composeApp:assembleDebug --quiet"
echo ""

# Summary
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
if $ALL_PASSED; then
    echo -e "${GREEN}✅ All checks passed! Test setup is ready.${NC}"
    echo ""
    echo -e "${YELLOW}Next steps:${NC}"
    echo "  1. For iOS: Add UI test target in Xcode (see iosApp/worldwidewavesUITests/README.md)"
    echo "  2. Run Android test locally:"
    echo "     ./gradlew :composeApp:connectedDebugAndroidTest"
    echo "  3. Or run on Firebase Test Lab:"
    echo "     ./scripts/firebase/run_all_firebase_tests.sh"
    exit 0
else
    echo -e "${RED}❌ Some checks failed. Please review the issues above.${NC}"
    exit 1
fi
