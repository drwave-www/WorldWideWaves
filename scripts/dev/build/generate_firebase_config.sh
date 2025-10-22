#!/usr/bin/env bash
# Copyright 2025 DrWave
#
# Generates Firebase configuration files for both Android and iOS
# This provides a unified interface for Firebase config generation across platforms

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Usage information
usage() {
    echo "Usage: $0 [android|ios|all]"
    echo ""
    echo "Generate Firebase configuration files from environment variables or local.properties"
    echo ""
    echo "Options:"
    echo "  android    Generate google-services.json for Android"
    echo "  ios        Generate GoogleService-Info.plist for iOS"
    echo "  all        Generate configs for both platforms (default)"
    echo ""
    echo "Examples:"
    echo "  $0              # Generate both"
    echo "  $0 android      # Generate Android only"
    echo "  $0 ios          # Generate iOS only"
    exit 1
}

# Parse arguments
PLATFORM="${1:-all}"

case "$PLATFORM" in
    android|ios|all)
        ;;
    -h|--help)
        usage
        ;;
    *)
        echo -e "${RED}❌ Invalid platform: $PLATFORM${NC}"
        usage
        ;;
esac

echo "🔥 Generating Firebase configuration..."

# Generate Android config
if [ "$PLATFORM" = "android" ] || [ "$PLATFORM" = "all" ]; then
    echo ""
    echo -e "${YELLOW}📱 Android Configuration${NC}"
    echo "----------------------------------------"

    cd "$PROJECT_ROOT"
    if ./gradlew :composeApp:generateFirebaseConfig --quiet; then
        if [ -f "composeApp/google-services.json" ]; then
            echo -e "${GREEN}✅ google-services.json generated${NC}"
            echo "📁 Location: composeApp/google-services.json"
        else
            echo -e "${RED}❌ google-services.json not found${NC}"
            exit 1
        fi
    else
        echo -e "${RED}❌ Android config generation failed${NC}"
        exit 1
    fi
fi

# Generate iOS config
if [ "$PLATFORM" = "ios" ] || [ "$PLATFORM" = "all" ]; then
    echo ""
    echo -e "${YELLOW}🍎 iOS Configuration${NC}"
    echo "----------------------------------------"

    if "$SCRIPT_DIR/generate_ios_firebase_config.sh"; then
        if [ -f "iosApp/worldwidewaves/GoogleService-Info.plist" ]; then
            echo -e "${GREEN}✅ GoogleService-Info.plist generated${NC}"
        else
            echo -e "${RED}❌ GoogleService-Info.plist not found${NC}"
            exit 1
        fi
    else
        echo -e "${RED}❌ iOS config generation failed${NC}"
        exit 1
    fi
fi

echo ""
echo -e "${GREEN}🎉 Firebase configuration complete!${NC}"
echo ""
echo "Next steps:"
if [ "$PLATFORM" = "android" ] || [ "$PLATFORM" = "all" ]; then
    echo "  Android: Build with ./gradlew assembleDebug"
fi
if [ "$PLATFORM" = "ios" ] || [ "$PLATFORM" = "all" ]; then
    echo "  iOS: Open iosApp/worldwidewaves.xcodeproj and build"
fi
