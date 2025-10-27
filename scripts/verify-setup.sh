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


# WorldWideWaves - Development Environment Verification Script
# Verifies all required tools and configurations for developing the app
# Compatible with macOS, Linux, and Windows (Git Bash)

# Don't exit on error - we want to continue checking even if some checks fail
set +e

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Symbols
CHECK="✅"
CROSS="❌"
WARN="⚠️ "
INFO="ℹ️ "

# Counters
PASSED=0
FAILED=0
WARNINGS=0

# Platform detection
detect_platform() {
    case "$(uname -s)" in
        Darwin*)
            echo "macos"
            ;;
        Linux*)
            echo "linux"
            ;;
        CYGWIN*|MINGW*|MSYS*)
            echo "windows"
            ;;
        *)
            echo "unknown"
            ;;
    esac
}

PLATFORM=$(detect_platform)

# Print functions
print_header() {
    echo -e "\n${BLUE}========================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}========================================${NC}\n"
}

print_check() {
    if [ "$1" = "pass" ]; then
        echo -e "${GREEN}${CHECK} $2${NC}"
        ((PASSED++))
    elif [ "$1" = "fail" ]; then
        echo -e "${RED}${CROSS} $2${NC}"
        ((FAILED++))
    else
        echo -e "${YELLOW}${WARN}$2${NC}"
        ((WARNINGS++))
    fi
}

print_info() {
    echo -e "${BLUE}${INFO}$1${NC}"
}

# Version comparison helper (reserved for future use)
# version_ge() {
#     printf '%s\n%s\n' "$2" "$1" | sort -V -C
# }

# Check command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Main verification starts here
print_header "WorldWideWaves Setup Verification"
echo "Platform: $PLATFORM"
echo "Working Directory: $(pwd)"
echo ""

# 1. Platform Detection
print_header "1. Platform Detection"
case "$PLATFORM" in
    macos)
        print_check "pass" "macOS detected"
        MACOS_VERSION=$(sw_vers -productVersion)
        print_info "macOS version: $MACOS_VERSION"
        ;;
    linux)
        print_check "pass" "Linux detected"
        if [ -f /etc/os-release ]; then
            DISTRO=$(. /etc/os-release && echo "$NAME $VERSION")
            print_info "Distribution: $DISTRO"
        fi
        ;;
    windows)
        print_check "pass" "Windows (Git Bash) detected"
        ;;
    *)
        print_check "fail" "Unknown platform - this script may not work correctly"
        ;;
esac

# 2. Essential Development Tools
print_header "2. Essential Development Tools"

# Git
if command_exists git; then
    GIT_VERSION=$(git --version | awk '{print $3}')
    print_check "pass" "Git installed (version $GIT_VERSION)"
else
    print_check "fail" "Git not found - install from https://git-scm.com/"
fi

# Java
if command_exists java; then
    JAVA_VERSION=$(java -version 2>&1 | head -n 1 | awk -F '"' '{print $2}')
    JAVA_MAJOR=$(echo "$JAVA_VERSION" | awk -F'.' '{print $1}')

    if [ "$JAVA_MAJOR" -ge 17 ]; then
        print_check "pass" "Java installed (version $JAVA_VERSION)"
    else
        print_check "fail" "Java $JAVA_VERSION found, but JDK 17+ required"
    fi
else
    print_check "fail" "Java not found - install JDK 17+ from https://adoptium.net/"
fi

# Gradle
if command_exists gradle; then
    GRADLE_VERSION=$(gradle --version | grep "Gradle" | awk '{print $2}')
    print_check "pass" "Gradle installed (version $GRADLE_VERSION)"
else
    print_check "warn" "Gradle not in PATH (gradlew wrapper will be used)"
fi

# Node.js
if command_exists node; then
    NODE_VERSION=$(node --version | sed 's/v//')
    NODE_MAJOR=$(echo "$NODE_VERSION" | awk -F'.' '{print $1}')

    if [ "$NODE_MAJOR" -ge 16 ]; then
        print_check "pass" "Node.js installed (version $NODE_VERSION)"
    else
        print_check "fail" "Node.js $NODE_VERSION found, but 16+ required"
    fi
else
    print_check "fail" "Node.js not found - install from https://nodejs.org/"
fi

# npm
if command_exists npm; then
    NPM_VERSION=$(npm --version)
    print_check "pass" "npm installed (version $NPM_VERSION)"
else
    print_check "fail" "npm not found - install Node.js from https://nodejs.org/"
fi

# ripgrep (rg)
if command_exists rg; then
    RG_VERSION=$(rg --version | head -n 1 | awk '{print $2}')
    print_check "pass" "ripgrep installed (version $RG_VERSION)"
else
    print_check "fail" "ripgrep (rg) not found - install from https://github.com/BurntSushi/ripgrep"
fi

# 3. Platform-Specific Tools
print_header "3. Platform-Specific Tools"

if [ "$PLATFORM" = "macos" ]; then
    # Xcode
    if command_exists xcodebuild; then
        XCODE_VERSION=$(xcodebuild -version | head -n 1 | awk '{print $2}')
        print_check "pass" "Xcode installed (version $XCODE_VERSION)"

        # Check Command Line Tools
        if xcode-select -p >/dev/null 2>&1; then
            print_check "pass" "Xcode Command Line Tools configured"
        else
            print_check "fail" "Xcode Command Line Tools not configured - run: xcode-select --install"
        fi
    else
        print_check "fail" "Xcode not found - install from App Store"
    fi

    # xcrun
    if command_exists xcrun; then
        print_check "pass" "xcrun available"
    else
        print_check "fail" "xcrun not found - install Xcode Command Line Tools"
    fi

    # CocoaPods (optional)
    if command_exists pod; then
        POD_VERSION=$(pod --version)
        print_check "pass" "CocoaPods installed (version $POD_VERSION) [optional]"
    else
        print_check "warn" "CocoaPods not found [optional] - install with: sudo gem install cocoapods"
    fi

elif [ "$PLATFORM" = "linux" ]; then
    # KVM support
    if [ -r /dev/kvm ]; then
        print_check "pass" "KVM available for Android emulator"
    else
        print_check "warn" "KVM not available - Android emulator will be slow"
        print_info "Enable KVM: sudo adduser \$USER kvm && sudo chmod 666 /dev/kvm"
    fi

    # libstdc++
    if ldconfig -p 2>/dev/null | grep -q libstdc++; then
        print_check "pass" "libstdc++ available"
    else
        print_check "warn" "libstdc++ may be missing - install build-essential"
    fi
fi

# 4. Android SDK
print_header "4. Android SDK"

# Check ANDROID_SDK_ROOT or ANDROID_HOME
if [ -n "$ANDROID_SDK_ROOT" ]; then
    print_check "pass" "ANDROID_SDK_ROOT set: $ANDROID_SDK_ROOT"
    SDK_PATH="$ANDROID_SDK_ROOT"
elif [ -n "$ANDROID_HOME" ]; then
    print_check "pass" "ANDROID_HOME set: $ANDROID_HOME"
    SDK_PATH="$ANDROID_HOME"
else
    print_check "fail" "ANDROID_SDK_ROOT or ANDROID_HOME not set"
    print_info "Install Android SDK and set environment variable"
    SDK_PATH=""
fi

# Check SDK components
if [ -n "$SDK_PATH" ] && [ -d "$SDK_PATH" ]; then
    # Check platform-tools
    if [ -d "$SDK_PATH/platform-tools" ]; then
        print_check "pass" "Android platform-tools installed"
    else
        print_check "fail" "Android platform-tools not found in SDK"
    fi

    # Check build-tools
    if [ -d "$SDK_PATH/build-tools" ] && [ -n "$(find "$SDK_PATH/build-tools" -mindepth 1 -maxdepth 1 -type d 2>/dev/null)" ]; then
        LATEST_BUILD_TOOLS=$(find "$SDK_PATH/build-tools" -mindepth 1 -maxdepth 1 -type d 2>/dev/null | sed 's|.*/||' | sort -V | tail -n 1)
        print_check "pass" "Android build-tools installed (latest: $LATEST_BUILD_TOOLS)"
    else
        print_check "fail" "Android build-tools not found in SDK"
    fi

    # Check platforms
    if [ -d "$SDK_PATH/platforms" ] && [ -n "$(find "$SDK_PATH/platforms" -mindepth 1 -maxdepth 1 -type d 2>/dev/null)" ]; then
        PLATFORMS=$(find "$SDK_PATH/platforms" -mindepth 1 -maxdepth 1 -type d 2>/dev/null | wc -l | xargs)
        print_check "pass" "Android platforms installed ($PLATFORMS platforms)"
    else
        print_check "fail" "Android platforms not found in SDK"
    fi

    # Check cmdline-tools or tools
    if [ -d "$SDK_PATH/cmdline-tools" ] || [ -d "$SDK_PATH/tools" ]; then
        print_check "pass" "Android SDK tools installed"
    else
        print_check "warn" "Android SDK command-line tools not found"
    fi
fi

# 5. Project Configuration
print_header "5. Project Configuration"

# Check if in project root
if [ -f "settings.gradle.kts" ]; then
    print_check "pass" "Running in project root directory"
else
    print_check "fail" "Not in project root - settings.gradle.kts not found"
fi

# Check local.properties
if [ -f "local.properties" ]; then
    print_check "pass" "local.properties exists"

    # Verify SDK path in local.properties
    if grep -q "sdk.dir" local.properties; then
        SDK_DIR=$(grep "sdk.dir" local.properties | cut -d'=' -f2)
        if [ -d "$SDK_DIR" ]; then
            print_check "pass" "SDK path in local.properties is valid"
        else
            print_check "fail" "SDK path in local.properties does not exist: $SDK_DIR"
        fi
    else
        print_check "warn" "sdk.dir not set in local.properties"
    fi
else
    print_check "fail" "local.properties not found"
    print_info "Create local.properties with: sdk.dir=/path/to/android/sdk"
fi

# Check Firebase config (optional)
if [ -f "composeApp/google-services.json" ]; then
    print_check "pass" "Firebase config (google-services.json) present [optional]"
else
    print_check "warn" "Firebase config not found [optional] - some features may not work"
fi

if [ "$PLATFORM" = "macos" ] && [ -f "iosApp/worldwidewaves/GoogleService-Info.plist" ]; then
    print_check "pass" "iOS Firebase config (GoogleService-Info.plist) present [optional]"
elif [ "$PLATFORM" = "macos" ]; then
    print_check "warn" "iOS Firebase config not found [optional]"
fi

# Check gradlew wrapper
if [ -f "gradlew" ]; then
    print_check "pass" "Gradle wrapper (gradlew) exists"

    if [ -x "gradlew" ]; then
        print_check "pass" "Gradle wrapper is executable"
    else
        print_check "warn" "Gradle wrapper not executable - run: chmod +x gradlew"
    fi
else
    print_check "fail" "Gradle wrapper (gradlew) not found"
fi

# 6. Optional Tools
print_header "6. Optional Tools"

# Docker
if command_exists docker; then
    DOCKER_VERSION=$(docker --version | awk '{print $3}' | sed 's/,//')
    print_check "pass" "Docker installed (version $DOCKER_VERSION) [optional]"
else
    print_check "warn" "Docker not found [optional] - useful for CI/CD testing"
fi

# gcloud
if command_exists gcloud; then
    GCLOUD_VERSION=$(gcloud version --format="value(core)" 2>/dev/null || echo "unknown")
    print_check "pass" "Google Cloud SDK installed (version $GCLOUD_VERSION) [optional]"
else
    print_check "warn" "gcloud not found [optional] - required for Firebase deployment"
fi

# swiftlint (macOS only)
if [ "$PLATFORM" = "macos" ]; then
    if command_exists swiftlint; then
        SWIFTLINT_VERSION=$(swiftlint version)
        print_check "pass" "SwiftLint installed (version $SWIFTLINT_VERSION) [optional]"
    else
        print_check "warn" "SwiftLint not found [optional] - install with: brew install swiftlint"
    fi
fi

# detekt (check in project)
if [ -f "gradlew" ]; then
    if ./gradlew tasks --all 2>/dev/null | grep -q "detekt"; then
        print_check "pass" "Detekt configured in project"
    else
        print_check "warn" "Detekt not found in Gradle tasks"
    fi
fi

# 7. Build System Test
print_header "7. Build System Test"

if [ -f "gradlew" ]; then
    print_info "Running './gradlew tasks' to verify build system..."

    if ./gradlew tasks >/dev/null 2>&1; then
        print_check "pass" "Gradle build system working"
    else
        print_check "fail" "Gradle build system test failed - check ./gradlew tasks output"
    fi
else
    print_check "fail" "Cannot test build system - gradlew not found"
fi

# Summary
print_header "Summary"

TOTAL=$((PASSED + FAILED + WARNINGS))
echo -e "${GREEN}Passed: $PASSED${NC}"
echo -e "${RED}Failed: $FAILED${NC}"
echo -e "${YELLOW}Warnings: $WARNINGS${NC}"
echo -e "Total checks: $TOTAL"
echo ""

# Exit code and next steps
if [ "$FAILED" -eq 0 ]; then
    echo -e "${GREEN}${CHECK} Setup verification complete!${NC}"
    echo ""
    echo "Next steps:"
    echo "  1. Run tests: ./gradlew :shared:testDebugUnitTest"
    echo "  2. Build app: ./gradlew assembleDebug"

    if [ "$PLATFORM" = "macos" ]; then
        echo "  3. Open iOS project: cd iosApp && open worldwidewaves.xcodeproj"
    fi

    echo ""
    exit 0
else
    echo -e "${RED}${CROSS} Setup verification failed - $FAILED critical issue(s) found${NC}"
    echo ""
    echo "Required actions:"

    if ! command_exists git; then
        echo "  - Install Git: https://git-scm.com/"
    fi

    if ! command_exists java; then
        echo "  - Install JDK 17+: https://adoptium.net/"
    elif [ "${JAVA_MAJOR:-0}" -lt 17 ]; then
        echo "  - Upgrade Java to version 17 or higher"
    fi

    if ! command_exists node; then
        echo "  - Install Node.js 16+: https://nodejs.org/"
    elif [ "${NODE_MAJOR:-0}" -lt 16 ]; then
        echo "  - Upgrade Node.js to version 16 or higher"
    fi

    if ! command_exists rg; then
        echo "  - Install ripgrep: https://github.com/BurntSushi/ripgrep"
    fi

    if [ -z "$ANDROID_SDK_ROOT" ] && [ -z "$ANDROID_HOME" ]; then
        echo "  - Install Android SDK and set ANDROID_SDK_ROOT environment variable"
    fi

    if [ ! -f "local.properties" ]; then
        echo "  - Create local.properties with SDK path"
    fi

    if [ "$PLATFORM" = "macos" ] && ! command_exists xcodebuild; then
        echo "  - Install Xcode from App Store"
    fi

    echo ""
    echo "See docs/setup/ for detailed installation instructions"
    echo ""
    exit 1
fi
