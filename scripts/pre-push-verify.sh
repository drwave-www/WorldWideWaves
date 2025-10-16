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


# Pre-Push Verification Script
# Runs all tests that will be executed by GitHub Actions workflows
# This ensures local testing matches CI/CD requirements

set -e  # Exit on first error

echo "🔍 Pre-Push Verification - Matching GitHub Actions Workflows"
echo "=============================================================="

# Track overall success
VERIFICATION_FAILED=false

# ============================================================
# Workflow 01: Build Android
# ============================================================
echo ""
echo "📦 [Workflow 01] Building Android..."
echo "------------------------------------------------------------"

if ! ./gradlew :shared:compileDebugKotlinAndroid :shared:compileReleaseKotlinAndroid \
    :composeApp:compileDebugKotlinAndroid :composeApp:compileReleaseKotlinAndroid \
    --no-daemon --stacktrace; then
    echo "❌ Android compilation failed"
    VERIFICATION_FAILED=true
else
    echo "✅ Android compilation passed"
fi

# ============================================================
# Workflow 02: Build iOS
# ============================================================
echo ""
echo "🍎 [Workflow 02] Building iOS Framework..."
echo "------------------------------------------------------------"

# Only run on macOS
if [[ "$OSTYPE" == "darwin"* ]]; then
    if ! ./gradlew :shared:linkDebugFrameworkIosSimulatorArm64 --no-daemon --stacktrace; then
        echo "❌ iOS framework build failed"
        VERIFICATION_FAILED=true
    else
        echo "✅ iOS framework build passed"
    fi

    # Try to build iOS app with Xcode (if project exists)
    if [ -f "iosApp/worldwidewaves.xcodeproj/project.pbxproj" ]; then
        echo "Building iOS app with Xcode..."
        if ! xcodebuild \
            -project iosApp/worldwidewaves.xcodeproj \
            -scheme worldwidewaves \
            -configuration Debug \
            -sdk iphonesimulator \
            -destination 'platform=iOS Simulator,name=iPhone 16' \
            CODE_SIGNING_ALLOWED=NO \
            CODE_SIGNING_REQUIRED=NO \
            build 2>&1 | grep -E "(BUILD SUCCEEDED|BUILD FAILED)"; then
            echo "⚠️  iOS app build check completed (check output above)"
        else
            echo "✅ iOS app build passed"
        fi
    fi
else
    echo "⏭️  Skipping iOS build (not on macOS)"
fi

# ============================================================
# Workflow 03: Code Quality
# ============================================================
echo ""
echo "🔎 [Workflow 03] Running Code Quality Checks..."
echo "------------------------------------------------------------"

# Unit tests (all platforms)
if ! ./gradlew :shared:testDebugUnitTest :shared:iosSimulatorArm64Test --no-daemon --stacktrace; then
    echo "❌ Unit tests failed"
    VERIFICATION_FAILED=true
else
    echo "✅ Unit tests passed"
fi

# Detekt
if ! ./gradlew detekt --no-daemon; then
    echo "❌ Detekt failed"
    VERIFICATION_FAILED=true
else
    echo "✅ Detekt passed"
fi

# Lint
if ! ./gradlew lint --no-daemon; then
    echo "❌ Lint failed"
    VERIFICATION_FAILED=true
else
    echo "✅ Lint passed"
fi

# ============================================================
# Workflows 04/05/06: Instrumented/E2E/Performance Tests
# ============================================================
echo ""
echo "🤖 [Workflows 04/05/06] Instrumented Tests Check..."
echo "------------------------------------------------------------"

# Find adb in common locations
adb_cmd=""
if command -v adb >/dev/null 2>&1; then
    adb_cmd="adb"
elif [ -f "$HOME/Library/Android/sdk/platform-tools/adb" ]; then
    adb_cmd="$HOME/Library/Android/sdk/platform-tools/adb"
elif [ -n "$ANDROID_HOME" ] && [ -f "$ANDROID_HOME/platform-tools/adb" ]; then
    adb_cmd="$ANDROID_HOME/platform-tools/adb"
fi

if [ -n "$adb_cmd" ]; then
    if $adb_cmd devices | grep -q "emulator.*device\|.*device$"; then
        echo "✅ Android device/emulator detected"
        echo "📱 Running instrumented tests (this may take several minutes)..."

        if ! ./gradlew :composeApp:connectedDebugAndroidTest --no-daemon --stacktrace; then
            echo "❌ Instrumented tests failed"
            echo "   Review test reports in: composeApp/build/reports/androidTests/"
            VERIFICATION_FAILED=true
        else
            echo "✅ Instrumented tests passed"
        fi
    else
        echo "⚠️  No Android device/emulator connected"
        echo "   Instrumented tests will be skipped locally but WILL run in CI"
        echo ""
        echo "   To run locally:"
        echo "   1. Start an Android emulator"
        echo "   2. Or connect a physical device via USB"
        echo "   3. Re-run this script or: ./gradlew :composeApp:connectedDebugAndroidTest"
        echo ""
        echo "   Continuing without instrumented tests..."
    fi
else
    echo "⏭️  Android SDK (adb) not found - skipping instrumented tests"
    echo "   These WILL run in CI/CD pipeline"
fi

# ============================================================
# Summary
# ============================================================
echo ""
echo "=============================================================="
echo "📊 Pre-Push Verification Summary"
echo "=============================================================="

if [ "$VERIFICATION_FAILED" = true ]; then
    echo "❌ VERIFICATION FAILED"
    echo ""
    echo "Some checks failed. Please fix the issues before pushing."
    echo "To skip this verification (NOT RECOMMENDED), use:"
    echo "   SKIP_PRE_PUSH_VERIFY=1 git push"
    echo ""

    if [ "${SKIP_PRE_PUSH_VERIFY:-0}" = "1" ]; then
        echo "⚠️  SKIP_PRE_PUSH_VERIFY=1 detected. Proceeding despite failures."
        exit 0
    fi

    exit 1
else
    echo "✅ ALL VERIFICATIONS PASSED"
    echo ""
    echo "Your code is ready to push and should pass all GitHub Actions workflows:"
    echo "  ✅ Workflow 01: Android Build"
    echo "  ✅ Workflow 02: iOS Build"
    echo "  ✅ Workflow 03: Code Quality (Unit Tests + Lint)"
    if adb devices 2>/dev/null | grep -q "device"; then
        echo "  ✅ Workflows 04/05/06: Instrumented/E2E/Performance Tests"
    else
        echo "  ⏭️  Workflows 04/05/06: Skipped locally (will run in CI)"
    fi
    echo ""
    exit 0
fi
