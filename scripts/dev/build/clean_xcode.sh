#!/usr/bin/env bash

# Copyright 2025 DrWave
# Licensed under the Apache License, Version 2.0

set -e

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
cd "$PROJECT_ROOT"

echo "🧹 Cleaning Xcode state to prevent GUID conflicts..."

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to safely remove directory/file
safe_remove() {
    local path="$1"
    if [ -e "$path" ]; then
        echo -e "${YELLOW}Removing:${NC} $path"
        rm -rf "$path"
        echo -e "${GREEN}✓ Removed${NC}"
    else
        echo -e "  Skipping (not found): $path"
    fi
}

echo ""
echo "=== Step 1: Clean Xcode DerivedData ==="
safe_remove "$HOME/Library/Developer/Xcode/DerivedData/worldwidewaves-"*
safe_remove "$PROJECT_ROOT/iosApp/build"

echo ""
echo "=== Step 2: Clean Xcode Caches ==="
safe_remove "$HOME/Library/Caches/org.swift.swiftpm"
safe_remove "$HOME/Library/Caches/com.apple.dt.Xcode"

echo ""
echo "=== Step 3: Clean Swift Package Manager ==="
safe_remove "$PROJECT_ROOT/iosApp/worldwidewaves.xcodeproj/project.xcworkspace/xcshareddata/swiftpm"
safe_remove "$PROJECT_ROOT/iosApp/worldwidewaves.xcodeproj/project.xcworkspace/xcuserdata"
safe_remove "$PROJECT_ROOT/iosApp/.swiftpm"

echo ""
echo "=== Step 4: Clean Xcode User State ==="
safe_remove "$PROJECT_ROOT/iosApp/worldwidewaves.xcodeproj/xcuserdata"
safe_remove "$PROJECT_ROOT/iosApp/worldwidewaves.xcodeproj/project.xcworkspace/xcuserdata"

echo ""
echo "=== Step 5: Clean Temporary Files ==="
find "$PROJECT_ROOT" -name ".DS_Store" -delete 2>/dev/null || true
find "$PROJECT_ROOT" -name "*.swp" -delete 2>/dev/null || true
find "$PROJECT_ROOT" -name "*.swo" -delete 2>/dev/null || true

echo ""
echo -e "${GREEN}✅ Xcode cleanup complete!${NC}"
echo ""
echo "Next steps:"
echo "  1. Open Xcode: open iosApp/worldwidewaves.xcodeproj"
echo "  2. Let Xcode re-resolve Swift packages (File → Packages → Resolve Package Versions)"
echo "  3. Build the project: Cmd+B"
echo ""
echo "If issues persist:"
echo "  - Restart Xcode"
echo "  - Run: rm -rf ~/Library/Developer/Xcode/DerivedData/*"
echo "  - Restart your Mac (nuclear option)"
echo ""
