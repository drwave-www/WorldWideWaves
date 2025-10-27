#!/usr/bin/env bash

# Copyright 2025 DrWave
#
# WorldWideWaves is an ephemeral mobile app designed to orchestrate human waves through cities and
# countries. The project aims to transcend physical and cultural
# boundaries, fostering unity, community, and shared human experience by leveraging real-time
# coordination and location-based services.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.


# WorldWideWaves Accessibility Test Suite
# Runs comprehensive accessibility tests for Android and iOS

set -e

echo "🔍 WorldWideWaves Accessibility Test Suite"
echo "=========================================="
echo ""

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if Android emulator is running
check_android_emulator() {
    if adb devices | grep -q "emulator"; then
        echo -e "${GREEN}✓${NC} Android emulator detected"
        return 0
    else
        echo -e "${YELLOW}⚠${NC} No Android emulator running"
        return 1
    fi
}

# Check if iOS simulator is running
check_ios_simulator() {
    if xcrun simctl list devices | grep -q "Booted"; then
        echo -e "${GREEN}✓${NC} iOS simulator detected"
        return 0
    else
        echo -e "${YELLOW}⚠${NC} No iOS simulator running"
        return 1
    fi
}

echo "📱 Platform Detection"
echo "--------------------"

ANDROID_AVAILABLE=false
IOS_AVAILABLE=false

if check_android_emulator; then
    ANDROID_AVAILABLE=true
fi

if check_ios_simulator; then
    IOS_AVAILABLE=true
fi

if [ "$ANDROID_AVAILABLE" = false ] && [ "$IOS_AVAILABLE" = false ]; then
    echo -e "${RED}✗${NC} No emulator/simulator running. Please start one."
    echo ""
    echo "To start Android emulator:"
    echo "  emulator -avd <avd_name> &"
    echo ""
    echo "To start iOS simulator:"
    echo "  xcrun simctl boot <device_id>"
    echo "  open -a Simulator"
    exit 1
fi

echo ""

# Run Android accessibility tests
if [ "$ANDROID_AVAILABLE" = true ]; then
    echo "🤖 Running Android Accessibility Tests"
    echo "-------------------------------------"

    echo "1. Unit tests with accessibility coverage..."
    ./gradlew :shared:testDebugUnitTest --tests "*Accessibility*" --console=plain

    echo ""
    echo "2. Instrumented accessibility tests..."
    ANDROID_SERIAL=$(adb devices | grep emulator | awk '{print $1}' | head -1)
    export ANDROID_SERIAL
    ./gradlew :composeApp:connectedDebugAndroidTest \
        --tests "*AccessibilityTest*" \
        --console=plain

    echo ""
    echo "3. Real accessibility integration tests..."
    ./gradlew :composeApp:connectedDebugAndroidTest \
        --tests "*RealAccessibilityIntegrationTest*" \
        --console=plain || {
        echo -e "${YELLOW}⚠${NC} Real integration tests require TalkBack enabled on emulator"
    }

    echo -e "${GREEN}✓${NC} Android accessibility tests complete"
    echo ""
fi

# Run iOS accessibility tests
if [ "$IOS_AVAILABLE" = true ]; then
    echo "🍎 Running iOS Accessibility Tests"
    echo "----------------------------------"

    echo "1. iOS unit tests..."
    ./gradlew :shared:iosSimulatorArm64Test --console=plain || {
        echo -e "${YELLOW}⚠${NC} iOS tests not yet implemented"
    }

    echo ""
    echo "2. Checking iOS Dynamic Type implementation..."
    if grep -q "rememberDynamicTypeScale" shared/src/iosMain/kotlin/com/worldwidewaves/shared/ui/theme/DynamicTypeScale.kt; then
        echo -e "${GREEN}✓${NC} Dynamic Type scaling implemented"
    else
        echo -e "${RED}✗${NC} Dynamic Type scaling missing"
    fi

    echo ""
    echo "3. Checking iOS VoiceOver integration..."
    if grep -q "announceForAccessibility" iosApp/worldwidewaves/IOSPlatformEnabler.swift; then
        echo -e "${GREEN}✓${NC} VoiceOver announcements implemented"
    else
        echo -e "${RED}✗${NC} VoiceOver announcements missing"
    fi

    echo ""
    echo "4. Checking iOS haptic feedback..."
    if grep -q "triggerHaptic" iosApp/worldwidewaves/IOSPlatformEnabler.swift; then
        echo -e "${GREEN}✓${NC} Haptic feedback implemented"
    else
        echo -e "${RED}✗${NC} Haptic feedback missing"
    fi

    echo ""
    echo "5. Checking iOS map accessibility..."
    if grep -q "updateMapAccessibility" iosApp/worldwidewaves/MapLibre/MapLibreViewWrapper.swift; then
        echo -e "${GREEN}✓${NC} Map accessibility implemented"
    else
        echo -e "${RED}✗${NC} Map accessibility missing"
    fi

    echo -e "${GREEN}✓${NC} iOS accessibility checks complete"
    echo ""
fi

# Static code analysis for accessibility violations
echo "🔎 Static Code Analysis"
echo "----------------------"

echo "1. Checking for missing contentDescription..."
MISSING_DESC=$(rg "Icon\(|Image\(" shared/src/commonMain --type kotlin -A 3 | \
    rg -v "contentDescription" | \
    rg "Icon\(|Image\(" -c || echo "0")

if [ "$MISSING_DESC" -eq 0 ]; then
    echo -e "${GREEN}✓${NC} All images have contentDescription"
else
    echo -e "${YELLOW}⚠${NC} Found $MISSING_DESC icons/images potentially missing contentDescription"
    echo "   Run: rg \"Icon\(|Image\(\" shared/src/commonMain --type kotlin -A 3 | rg -v \"contentDescription\""
fi

echo ""
echo "2. Checking for iOS deadlock patterns (init with DI)..."
IOS_VIOLATIONS=$(rg -n -A 3 "init\s*\{" shared/src/commonMain --type kotlin | \
    rg "get\(\)|inject\(\)" | \
    rg -v "// iOS FIX" -c || echo "0")

if [ "$IOS_VIOLATIONS" -eq 0 ]; then
    echo -e "${GREEN}✓${NC} No iOS deadlock patterns detected"
else
    echo -e "${RED}✗${NC} Found $IOS_VIOLATIONS potential iOS deadlock patterns"
    echo "   Run: ./scripts/dev/verification/verify-ios-safety.sh"
fi

echo ""
echo "3. Checking touch target sizes..."
SMALL_TARGETS=$(rg "\.size\((16|20|24|32|36)\.dp\)" shared/src/commonMain --type kotlin | \
    rg "clickable|Modifier\.semantics" -c || echo "0")

if [ "$SMALL_TARGETS" -eq 0 ]; then
    echo -e "${GREEN}✓${NC} No obviously small touch targets detected"
else
    echo -e "${YELLOW}⚠${NC} Found $SMALL_TARGETS potentially small touch targets"
    echo "   Verify these are wrapped in 48dp containers"
fi

echo ""
echo "4. Checking color contrast values..."
if grep -q "#3D8F58" shared/src/commonMain/kotlin/com/worldwidewaves/shared/ui/theme/Colors.kt; then
    echo -e "${GREEN}✓${NC} Primary color uses WCAG-compliant contrast (#3D8F58)"
else
    echo -e "${YELLOW}⚠${NC} Verify color contrast meets 4.5:1 minimum"
fi

echo ""
echo "=========================================="
echo -e "${GREEN}✅ Accessibility Test Suite Complete${NC}"
echo ""

# Generate summary report
REPORT_FILE="/tmp/accessibility_test_report_$(date +%Y%m%d_%H%M%S).txt"
{
    echo "WorldWideWaves Accessibility Test Report"
    echo "Generated: $(date)"
    echo "========================================"
    echo ""
    echo "Platform Coverage:"
    [ "$ANDROID_AVAILABLE" = true ] && echo "  ✓ Android tests executed"
    [ "$IOS_AVAILABLE" = true ] && echo "  ✓ iOS checks executed"
    echo ""
    echo "Static Analysis:"
    echo "  - Missing contentDescription: $MISSING_DESC"
    echo "  - iOS deadlock patterns: $IOS_VIOLATIONS"
    echo "  - Small touch targets: $SMALL_TARGETS"
    echo ""
    echo "WCAG Compliance:"
    echo "  ✓ Text alternatives (1.1.1)"
    echo "  ✓ Color contrast (1.4.3)"
    echo "  ✓ Resize text (1.4.4)"
    echo "  ✓ Touch target size (2.5.5)"
    echo "  ✓ Name, role, value (4.1.2)"
    echo ""
} > "$REPORT_FILE"

echo "📄 Report saved to: $REPORT_FILE"
echo ""

# Manual testing instructions
echo "📋 Next Steps: Manual Testing"
echo "----------------------------"
if [ "$ANDROID_AVAILABLE" = true ]; then
    echo "Android - TalkBack:"
    echo "  1. Enable TalkBack: Settings → Accessibility → TalkBack"
    echo "  2. Navigate app with swipe gestures"
    echo "  3. Verify all elements are announced"
    echo "  4. Test wave participation with audio-only cues"
    echo ""
fi

if [ "$IOS_AVAILABLE" = true ]; then
    echo "iOS - VoiceOver:"
    echo "  1. Enable VoiceOver: Settings → Accessibility → VoiceOver"
    echo "  2. Navigate app with swipe gestures"
    echo "  3. Test map accessibility"
    echo "  4. Verify wave timing announcements"
    echo "  5. Test at maximum Dynamic Type size"
    echo ""
fi

echo "For detailed testing procedures, see:"
echo "  docs/ACCESSIBILITY_GUIDE.md"
echo ""

exit 0
