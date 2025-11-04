#!/usr/bin/env bash

# Copyright (c) 2025 WorldWideWaves
# Manual dSYM upload script for Firebase Crashlytics
#
# Usage:
#   ./upload_ios_dsyms.sh                    # Upload from latest DerivedData
#   ./upload_ios_dsyms.sh /path/to/dsym      # Upload specific dSYM
#   ./upload_ios_dsyms.sh /path/to/dir/*.dSYM # Upload all dSYMs from directory

set -e

# Color output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

# Firebase configuration
GOOGLE_SERVICE_PLIST="$PROJECT_ROOT/iosApp/worldwidewaves/GoogleService-Info.plist"
UPLOAD_SYMBOLS="$PROJECT_ROOT/iosApp/build_ios/SourcePackages/checkouts/firebase-ios-sdk/Crashlytics/upload-symbols"

# Check if GoogleService-Info.plist exists
if [ ! -f "$GOOGLE_SERVICE_PLIST" ]; then
    echo -e "${RED}❌ Error: GoogleService-Info.plist not found at:${NC}"
    echo "   $GOOGLE_SERVICE_PLIST"
    echo ""
    echo "   Generate it first:"
    echo "   ./scripts/dev/build/generate_ios_firebase_config.sh"
    exit 1
fi

# Check if upload-symbols exists
if [ ! -f "$UPLOAD_SYMBOLS" ]; then
    echo -e "${RED}❌ Error: upload-symbols binary not found at:${NC}"
    echo "   $UPLOAD_SYMBOLS"
    echo ""
    echo "   Make sure Firebase iOS SDK is installed via SPM in Xcode."
    exit 1
fi

# Function to upload a single dSYM
upload_dsym() {
    local dsym_path="$1"

    if [ ! -d "$dsym_path" ]; then
        echo -e "${RED}❌ dSYM not found: $dsym_path${NC}"
        return 1
    fi

    echo -e "${BLUE}📤 Uploading: $(basename "$dsym_path")${NC}"

    if "$UPLOAD_SYMBOLS" -gsp "$GOOGLE_SERVICE_PLIST" -p ios "$dsym_path"; then
        echo -e "${GREEN}✅ Successfully uploaded: $(basename "$dsym_path")${NC}"
        return 0
    else
        echo -e "${RED}❌ Failed to upload: $(basename "$dsym_path")${NC}"
        return 1
    fi
}

# Main logic
main() {
    echo -e "${BLUE}╔════════════════════════════════════════════════════════╗${NC}"
    echo -e "${BLUE}║    Firebase Crashlytics dSYM Upload Tool (iOS)        ║${NC}"
    echo -e "${BLUE}╚════════════════════════════════════════════════════════╝${NC}"
    echo ""

    if [ $# -eq 0 ]; then
        # No arguments: upload from latest DerivedData
        echo -e "${YELLOW}🔍 Searching for dSYMs in DerivedData...${NC}"

        # Find latest DerivedData for worldwidewaves
        DERIVED_DATA=$(find ~/Library/Developer/Xcode/DerivedData/worldwidewaves-* -maxdepth 0 -type d 2>/dev/null | head -1)

        if [ -z "$DERIVED_DATA" ]; then
            echo -e "${RED}❌ No DerivedData found for worldwidewaves${NC}"
            echo ""
            echo "   Build the app first in Xcode (Release configuration, Archive build)"
            exit 1
        fi

        echo -e "${GREEN}📂 Found DerivedData: $(basename "$DERIVED_DATA")${NC}"
        echo ""

        # Find Release build dSYMs
        DSYM_DIR="$DERIVED_DATA/Build/Intermediates.noindex/ArchiveIntermediates/worldwidewaves/BuildProductsPath/Release-iphoneos"

        if [ ! -d "$DSYM_DIR" ]; then
            echo -e "${RED}❌ No Release build found in DerivedData${NC}"
            echo ""
            echo "   Create an Archive build first:"
            echo "   Xcode → Product → Archive"
            exit 1
        fi

        # Upload app dSYM
        APP_DSYM="$DSYM_DIR/worldwidewaves.app.dSYM"
        if [ -d "$APP_DSYM" ]; then
            upload_dsym "$APP_DSYM"
        else
            echo -e "${YELLOW}⚠️  App dSYM not found: $APP_DSYM${NC}"
        fi

        echo ""

        # Upload Shared framework dSYM (KMM)
        SHARED_DSYM="$DSYM_DIR/Shared.framework.dSYM"
        if [ -d "$SHARED_DSYM" ]; then
            upload_dsym "$SHARED_DSYM"
        else
            echo -e "${YELLOW}⚠️  Shared.framework dSYM not found: $SHARED_DSYM${NC}"
        fi

    else
        # Arguments provided: upload specific dSYMs
        success_count=0
        fail_count=0

        for dsym_path in "$@"; do
            # Handle wildcards
            for dsym in $dsym_path; do
                if upload_dsym "$dsym"; then
                    ((success_count++))
                else
                    ((fail_count++))
                fi
                echo ""
            done
        done

        echo -e "${BLUE}════════════════════════════════════════════════════════${NC}"
        echo -e "${GREEN}✅ Successful uploads: $success_count${NC}"
        if [ $fail_count -gt 0 ]; then
            echo -e "${RED}❌ Failed uploads: $fail_count${NC}"
        fi
    fi

    echo ""
    echo -e "${BLUE}════════════════════════════════════════════════════════${NC}"
    echo -e "${GREEN}🎉 Upload complete!${NC}"
    echo ""
    echo -e "${YELLOW}📝 Next steps:${NC}"
    echo "   1. Wait 5-10 minutes for Firebase to process"
    echo "   2. Check Firebase Console:"
    echo "      https://console.firebase.google.com/project/world-wide-waves/crashlytics/app/ios:com.worldwidewaves/settings/symbols"
    echo "   3. Look for the uploaded UUIDs in the dSYMs list"
    echo ""
}

main "$@"
