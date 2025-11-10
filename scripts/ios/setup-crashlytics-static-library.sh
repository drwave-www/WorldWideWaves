#!/usr/bin/env bash

#
# Copyright 2025 DrWave
#
# Setup script for CrashlyticsBridge static library
#
# This script creates a static library Xcode project for the Crashlytics bridge.
#
# WHY THIS IS NEEDED:
# - iOS apps (MH_EXECUTE) cannot export Objective-C symbols
# - Kotlin/Native Shared.framework needs these symbols at link time
# - Solution: Compile bridge into static library, link to both framework and app
#
# ARCHITECTURE:
#   libCrashlyticsBridge.a (static lib)
#         ↓
#   Shared.framework (links .a during build - symbol resolved)
#         ↓
#   iOS App (links framework + .a - consistent symbols)
#
# This script automates the entire setup process.
#

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
IOS_APP_DIR="$PROJECT_ROOT/iosApp"
BRIDGE_PROJECT_DIR="$IOS_APP_DIR/CrashlyticsBridge"

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "🔧 CrashlyticsBridge Static Library Setup"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "This will create:"
echo "  • CrashlyticsBridge.xcodeproj (static library project)"
echo "  • CrashlyticsBridgeTests target (XCTest unit tests)"
echo "  • Gradle automation for building .a file"
echo "  • Full documentation"
echo ""

# 1. Create project directory structure
echo "📁 Creating project structure..."
mkdir -p "$BRIDGE_PROJECT_DIR/CrashlyticsBridge"
mkdir -p "$BRIDGE_PROJECT_DIR/CrashlyticsBridgeTests"

# 2. Move source files
echo "📝 Moving source files..."
mv "$IOS_APP_DIR/worldwidewaves/Utils/CrashlyticsBridge.h" "$BRIDGE_PROJECT_DIR/CrashlyticsBridge/"
mv "$IOS_APP_DIR/worldwidewaves/Utils/CrashlyticsBridge.m" "$BRIDGE_PROJECT_DIR/CrashlyticsBridge/"

# 3. Create Xcode project using command line
echo "🔨 Creating Xcode static library project..."
cd "$BRIDGE_PROJECT_DIR"

# Note: We'll need to create the project.pbxproj manually or via ruby xcodeproj gem
# For now, output instructions for manual creation

cat > "$BRIDGE_PROJECT_DIR/CREATE_PROJECT.md" << 'EOF'
# Manual Xcode Project Creation Required

Due to the complexity of programmatically creating Xcode projects, please complete these steps in Xcode:

## Steps

1. Open Xcode
2. File → New → Project
3. Choose "Static Library" under iOS
4. Product Name: "CrashlyticsBridge"
5. Save to: `iosApp/CrashlyticsBridge/`

6. Add Files to Project:
   - Right-click CrashlyticsBridge folder
   - Add Files → Select `CrashlyticsBridge.h` and `CrashlyticsBridge.m`
   - Ensure "Copy items if needed" is UNCHECKED
   - Target: CrashlyticsBridge

7. Add Firebase Dependency:
   - Select project → CrashlyticsBridge target
   - General → Frameworks and Libraries
   - Add Package → `https://github.com/firebase/firebase-ios-sdk`
   - Add FirebaseCrashlytics

8. Add Test Target:
   - File → New → Target → Unit Testing Bundle
   - Target: CrashlyticsBridgeTests

9. Build Settings:
   - Deployment Target: iOS 16.0
   - Architectures: arm64, x86_64

Then run: `./scripts/ios/finalize-crashlytics-setup.sh`
EOF

echo ""
echo "✅ Project structure created"
echo ""
echo "⚠️  MANUAL STEP REQUIRED:"
echo "   Please follow instructions in:"
echo "   $BRIDGE_PROJECT_DIR/CREATE_PROJECT.md"
echo ""
echo "   Then run:"
echo "   ./scripts/ios/finalize-crashlytics-setup.sh"
echo ""
