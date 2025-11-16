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


# Map Validation Script
# Validates that all map modules have required files (mbtiles, geojson, e_map, e_location)
# Ensures file integrity before push to prevent missing map data

set -e

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Counters
TOTAL_MAPS=0
FAILED_MAPS=0
ERRORS=()

echo -e "${BLUE}🗺️  Validating map modules...${NC}"
echo ""

# Get project root directory
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../" && pwd)"
MAPS_DIR="$PROJECT_ROOT/maps"
DRAWABLES_DIR="$PROJECT_ROOT/shared/src/commonMain/composeResources/drawable"

# Get all map module directories (exclude android and build directories)
MAP_MODULES=$(find "$MAPS_DIR" -mindepth 1 -maxdepth 1 -type d ! -name "android" ! -name "build" ! -name ".claude" | sort)

if [ -z "$MAP_MODULES" ]; then
    echo -e "${RED}❌ No map modules found in $MAPS_DIR${NC}"
    exit 1
fi

# Iterate through each map module
for MAP_MODULE in $MAP_MODULES; do
    MAP_NAME=$(basename "$MAP_MODULE")
    TOTAL_MAPS=$((TOTAL_MAPS + 1))

    echo -e "${BLUE}Checking: $MAP_NAME${NC}"

    ASSETS_DIR="$MAP_MODULE/src/main/assets"
    MAP_ERRORS=()

    # Check 1: Assets directory exists
    if [ ! -d "$ASSETS_DIR" ]; then
        MAP_ERRORS+=("  ${RED}✗${NC} Assets directory missing: $ASSETS_DIR")
    else
        # Check 2: MBTiles file exists and is not empty
        MBTILES_FILE="$ASSETS_DIR/${MAP_NAME}.mbtiles"
        if [ ! -f "$MBTILES_FILE" ]; then
            MAP_ERRORS+=("  ${RED}✗${NC} MBTiles file missing: ${MAP_NAME}.mbtiles")
        elif [ ! -s "$MBTILES_FILE" ]; then
            MAP_ERRORS+=("  ${RED}✗${NC} MBTiles file is empty: ${MAP_NAME}.mbtiles")
        else
            SIZE=$(du -h "$MBTILES_FILE" | cut -f1)
            echo -e "  ${GREEN}✓${NC} MBTiles file exists and is not empty ($SIZE)"
        fi

        # Check 3: GeoJSON file exists and is not empty
        GEOJSON_FILE="$ASSETS_DIR/${MAP_NAME}.geojson"
        if [ ! -f "$GEOJSON_FILE" ]; then
            MAP_ERRORS+=("  ${RED}✗${NC} GeoJSON file missing: ${MAP_NAME}.geojson")
        elif [ ! -s "$GEOJSON_FILE" ]; then
            MAP_ERRORS+=("  ${RED}✗${NC} GeoJSON file is empty: ${MAP_NAME}.geojson")
        else
            SIZE=$(du -h "$GEOJSON_FILE" | cut -f1)
            echo -e "  ${GREEN}✓${NC} GeoJSON file exists and is not empty ($SIZE)"
        fi
    fi

    # Check 4: e_map image exists
    E_MAP_FILE="$DRAWABLES_DIR/e_map_${MAP_NAME}.webp"
    if [ ! -f "$E_MAP_FILE" ]; then
        MAP_ERRORS+=("  ${RED}✗${NC} Event map image missing: e_map_${MAP_NAME}.webp")
    else
        echo -e "  ${GREEN}✓${NC} Event map image exists"
    fi

    # Check 5: e_location image exists
    E_LOCATION_FILE="$DRAWABLES_DIR/e_location_${MAP_NAME}.webp"
    if [ ! -f "$E_LOCATION_FILE" ]; then
        MAP_ERRORS+=("  ${RED}✗${NC} Event location image missing: e_location_${MAP_NAME}.webp")
    else
        echo -e "  ${GREEN}✓${NC} Event location image exists"
    fi

    # Report errors for this map
    if [ ${#MAP_ERRORS[@]} -gt 0 ]; then
        FAILED_MAPS=$((FAILED_MAPS + 1))
        echo -e "${RED}❌ $MAP_NAME has ${#MAP_ERRORS[@]} error(s):${NC}"
        for ERROR in "${MAP_ERRORS[@]}"; do
            echo -e "$ERROR"
            ERRORS+=("[$MAP_NAME] $ERROR")
        done
    else
        echo -e "${GREEN}✅ $MAP_NAME is valid${NC}"
    fi

    echo ""
done

# Summary
echo "═══════════════════════════════════════════════════════════"
echo -e "${BLUE}Map Validation Summary:${NC}"
echo "  Total maps checked: $TOTAL_MAPS"
echo -e "  ${GREEN}Valid maps: $((TOTAL_MAPS - FAILED_MAPS))${NC}"

if [ $FAILED_MAPS -gt 0 ]; then
    echo -e "  ${RED}Failed maps: $FAILED_MAPS${NC}"
    echo ""
    echo -e "${RED}❌ Map validation failed!${NC}"
    echo ""
    echo "The following issues were found:"
    for ERROR in "${ERRORS[@]}"; do
        echo -e "  $ERROR"
    done
    echo ""
    echo "Please fix the missing or empty files before pushing."
    echo ""
    exit 1
else
    echo ""
    echo -e "${GREEN}✅ All maps are valid!${NC}"
    echo ""
    exit 0
fi
