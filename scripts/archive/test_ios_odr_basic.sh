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


# Basic test of iOS ODR setup
# Verifies that bundle structure and configuration are correct

echo "🧪 Testing iOS ODR Basic Setup"
echo "==============================="

# Test 1: Verify bundle structure
echo "Test 1: Checking iOS bundle structure..."
TEST_CITIES=("paris_france" "new_york_usa" "london_england" "berlin_germany" "tokyo_japan")
BUNDLE_DIR="iosApp/worldwidewaves/Resources/Maps"

for city in "${TEST_CITIES[@]}"; do
    if [ -d "$BUNDLE_DIR/$city" ] && [ -f "$BUNDLE_DIR/$city/$city.geojson" ] && [ -f "$BUNDLE_DIR/$city/$city.mbtiles" ]; then
        echo "✅ $city: Bundle structure correct"
    else
        echo "❌ $city: Bundle structure incomplete"
        exit 1
    fi
done

# Test 2: Verify Info.plist ODR configuration
echo "Test 2: Checking Info.plist ODR configuration..."
INFO_PLIST="iosApp/worldwidewaves/Info.plist"

if grep -q "NSBundleResourceRequestTags" "$INFO_PLIST"; then
    echo "✅ NSBundleResourceRequestTags found in Info.plist"
else
    echo "❌ NSBundleResourceRequestTags missing from Info.plist"
    exit 1
fi

for city in "${TEST_CITIES[@]}"; do
    if grep -q "$city" "$INFO_PLIST"; then
        echo "✅ $city: ODR tag found in Info.plist"
    else
        echo "❌ $city: ODR tag missing from Info.plist"
        exit 1
    fi
done

# Test 3: Check file sizes are reasonable
echo "Test 3: Validating file sizes..."
for city in "${TEST_CITIES[@]}"; do
    geojson_size=$(stat -f%z "$BUNDLE_DIR/$city/$city.geojson" 2>/dev/null || echo "0")
    mbtiles_size=$(stat -f%z "$BUNDLE_DIR/$city/$city.mbtiles" 2>/dev/null || echo "0")

    if [ "$geojson_size" -gt 1000 ] && [ "$mbtiles_size" -gt 1000000 ]; then
        echo "✅ $city: File sizes reasonable (geojson: ${geojson_size} bytes, mbtiles: ${mbtiles_size} bytes)"
    else
        echo "⚠️ $city: Unusual file sizes (geojson: ${geojson_size} bytes, mbtiles: ${mbtiles_size} bytes)"
    fi
done

# Test 4: Verify resource paths are correct
echo "Test 4: Checking resource path format..."
if grep -q "paris_france.geojson" "$INFO_PLIST" && grep -q "paris_france.mbtiles" "$INFO_PLIST"; then
    echo "✅ Resource paths correctly formatted in Info.plist"
else
    echo "❌ Resource paths incorrectly formatted in Info.plist"
    exit 1
fi

echo "🎉 All iOS ODR basic setup tests passed!"
echo "Ready for Xcode ODR tag configuration and integration testing."