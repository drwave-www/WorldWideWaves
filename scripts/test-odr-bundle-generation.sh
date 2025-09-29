#!/bin/bash

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


# Test script for ODR bundle generation
# Validates that 40-generate-modules.sh correctly handles iOS ODR setup

set -e

echo "🧪 Testing iOS ODR Bundle Generation Script"
echo "==========================================="

# Test directory setup
TEST_DIR="/tmp/worldwidewaves_odr_test"
mkdir -p "$TEST_DIR"
cd "$TEST_DIR"

# Create minimal test structure
mkdir -p "scripts/maps/data"
mkdir -p "iosApp/worldwidewaves"
mkdir -p "maps/android"

# Create test map data
echo '{"type":"Point","coordinates":[2.3522,48.8566]}' > "scripts/maps/data/test_paris.geojson"
echo "MOCK_MBTILES_DATA" > "scripts/maps/data/test_paris.mbtiles"

# Create minimal Info.plist
cat > "iosApp/worldwidewaves/Info.plist" << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>CFBundleURLTypes</key>
    <array>
        <dict>
            <key>CFBundleURLName</key>
            <string>com.worldwidewaves.app</string>
        </dict>
    </array>
</dict>
</plist>
EOF

# Create minimal settings.gradle.kts
echo 'include(":composeApp")' > "settings.gradle.kts"

# Create minimal lib.inc.sh
mkdir -p "scripts/maps/libs"
cat > "scripts/maps/libs/lib.inc.sh" << 'EOF'
exists() {
    local event="$1"
    [ -f "data/$event.geojson" ] && [ -f "data/$event.mbtiles" ]
}
EOF

# Copy the actual script for testing
cp "/Users/ldiasdasilva/StudioProjects/WorldWideWaves/scripts/maps/40-generate-modules.sh" "scripts/maps/"

# Test 1: Run script with test event
echo "Test 1: Running script with test_paris event..."
cd "scripts/maps"
bash 40-generate-modules.sh test_paris

# Verify iOS resources were created
echo "✅ Checking iOS bundle structure..."
if [ -f "../../iosApp/worldwidewaves/Resources/Maps/test_paris/test_paris.geojson" ]; then
    echo "✅ iOS geojson file created successfully"
else
    echo "❌ iOS geojson file missing"
    exit 1
fi

if [ -f "../../iosApp/worldwidewaves/Resources/Maps/test_paris/test_paris.mbtiles" ]; then
    echo "✅ iOS mbtiles file created successfully"
else
    echo "❌ iOS mbtiles file missing"
    exit 1
fi

# Verify Info.plist was updated
echo "✅ Checking Info.plist ODR configuration..."
if grep -q "NSBundleResourceRequestTags" "../../iosApp/worldwidewaves/Info.plist"; then
    echo "✅ NSBundleResourceRequestTags added to Info.plist"
else
    echo "❌ NSBundleResourceRequestTags missing from Info.plist"
    exit 1
fi

if grep -q "test_paris" "../../iosApp/worldwidewaves/Info.plist"; then
    echo "✅ test_paris ODR tag added to Info.plist"
else
    echo "❌ test_paris ODR tag missing from Info.plist"
    exit 1
fi

# Test 2: Idempotent execution
echo "Test 2: Testing idempotent execution..."
bash 40-generate-modules.sh test_paris

# Should not error and should not duplicate entries
if grep -c "test_paris" "../../iosApp/worldwidewaves/Info.plist" | grep -q "^2$"; then
    echo "✅ Idempotent execution works correctly"
else
    echo "❌ Script is not idempotent - created duplicates"
    exit 1
fi

# Test 3: Multiple events
echo "Test 3: Testing multiple events..."
echo '{"type":"Point","coordinates":[-74.0060,40.7128]}' > "data/test_newyork.geojson"
echo "MOCK_MBTILES_DATA_NYC" > "data/test_newyork.mbtiles"

bash 40-generate-modules.sh test_paris,test_newyork

# Verify both events in Info.plist
if grep -q "test_newyork" "../../iosApp/worldwidewaves/Info.plist"; then
    echo "✅ Multiple events handled correctly"
else
    echo "❌ Multiple events not handled properly"
    exit 1
fi

# Cleanup
cd /
rm -rf "$TEST_DIR"

echo "🎉 All ODR bundle generation tests passed!"
echo "✅ iOS bundle structure creation works"
echo "✅ Info.plist ODR configuration works"
echo "✅ Idempotent execution verified"
echo "✅ Multiple events support confirmed"
echo "✅ Both geojson and mbtiles handling verified"