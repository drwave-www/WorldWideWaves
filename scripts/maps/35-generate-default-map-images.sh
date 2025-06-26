#!/bin/bash
#
# Copyright 2024 DrWave
#
# WorldWideWaves is an ephemeral mobile app designed to orchestrate human waves through cities and
# countries, culminating in a global wave. The project aims to transcend physical and cultural
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
#

cd "$(dirname "$0")" # always work from executable folder

# ---------- Vars and support functions ---------------------------------------
. ./libs/lib.inc.sh

# Create necessary directories
mkdir -p ./data
mkdir -p ./bin
mkdir -p ./tmp

# Constants
IMAGE_WIDTH=1024
# DIM_EVENT_MAP_RATIO is 16/9 from WWWGlobals.kt
IMAGE_HEIGHT=$(echo "$IMAGE_WIDTH / (16/9)" | bc -l | xargs printf "%.0f")
OUTPUT_DIR="../../shared/src/commonMain/composeResources/drawable"
STYLE_DIR="../../shared/src/commonMain/composeResources/files/style"
TEMP_DIR="./tmp"
# Node renderer is now a standalone script committed in this folder
NODE_SCRIPT="./render-map.js"
GEOJSON_DIR="./data"
MBTILES_DIR="./data"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# ---------- Check dependencies ----------------------------------------------

# Check if Node.js is installed
if ! command -v node &> /dev/null; then
    echo -e "${RED}Node.js is not installed. Please install Node.js to continue.${NC}"
    exit 1
fi

# Check if npm is installed
if ! command -v npm &> /dev/null; then
    echo -e "${RED}npm is not installed. Please install npm to continue.${NC}"
    exit 1
fi

# Create a temporary package.json if it doesn't exist
if [ ! -f "$TEMP_DIR/package.json" ]; then
    mkdir -p "$TEMP_DIR"
    cat > "$TEMP_DIR/package.json" << EOF
{
  "name": "maplibre-renderer",
  "version": "1.0.0",
  "description": "Temporary package for rendering maps",
  "main": "render-map.js",
  "dependencies": {
    "@maplibre/maplibre-gl-native": "^6.1.0",
    "fs-extra": "^11.3.0",
    "pngjs": "^7.0.0"
  }
}
EOF
fi

# Install required packages if they're not already installed
echo -e "${BLUE}Checking for required Node.js packages...${NC}"
cd "$TEMP_DIR"
if [ ! -d "node_modules" ]; then
    echo -e "${YELLOW}Installing required Node.js packages...${NC}"
    npm install
    if [ $? -ne 0 ]; then
        echo -e "${RED}Failed to install required Node.js packages.${NC}"
        cd ..
        exit 1
    fi
fi
cd ..

# ---------- Process maps ---------------------------------------------------

if [ ! -z "$1" ]; then
    if $(exists "$1"); then
        EVENTS=$1
    else
        echo -e "${RED}Unexistent event $1${NC}"
        exit 1
    fi
fi

echo -e "${BLUE}Generating default map images with dimensions: ${IMAGE_WIDTH}x${IMAGE_HEIGHT}${NC}"
echo

# Ensure output directory exists
mkdir -p "$OUTPUT_DIR"

# Process each event
for event in $EVENTS; do
    echo -e "${GREEN}==> Processing event $event${NC}"
    
    TYPE=$(conf "$event" type)
    if [ "$TYPE" = "world" ]; then
        echo "Skip the world"
        continue
    fi
    
    # Check if GeoJSON file exists
    GEOJSON_FILE="$GEOJSON_DIR/${event}.geojson"
    if [ ! -f "$GEOJSON_FILE" ]; then
        echo -e "${YELLOW}GeoJSON file not found for $event. Skipping.${NC}"
        continue
    fi

    # Check if GeoJSON file exists
    MBTILES_FILE="$MBTILES_DIR/${event}.mbtiles"
    if [ ! -f "$MBTILES_FILE" ]; then
        echo -e "${YELLOW}MBTILES file not found for $event. Skipping.${NC}"
        continue
    fi
    
    # Get the center coordinates for the event
    CENTER=$(get_event_center "$event")
    if [ -z "$CENTER" ]; then
        echo -e "${RED}Failed to get center coordinates for $event. Skipping.${NC}"
        continue
    fi
    CENTER_LAT=$(echo $CENTER | cut -d',' -f1)
    CENTER_LNG=$(echo $CENTER | cut -d',' -f2)
    
    # Get appropriate zoom level based on event area size
    BBOX=$(get_event_bbox "$event")
    if [ -z "$BBOX" ]; then
        echo -e "${YELLOW}Failed to get bounding box for $event. Using default zoom.${NC}"
        ZOOM=10
    else {
        # Calculate appropriate zoom level based on bounding box size
        SW_LAT=$(echo $BBOX | cut -d',' -f1)
        SW_LNG=$(echo $BBOX | cut -d',' -f2)
        NE_LAT=$(echo $BBOX | cut -d',' -f3)
        NE_LNG=$(echo $BBOX | cut -d',' -f4)
        
        # Calculate the size of the bounding box
        LAT_DIFF=$(echo "$NE_LAT - $SW_LAT" | bc -l)
        LNG_DIFF=$(echo "$NE_LNG - $SW_LNG" | bc -l)
        
        # Estimate zoom level based on the size
        if (( $(echo "$LAT_DIFF > 10" | bc -l) )) || (( $(echo "$LNG_DIFF > 10" | bc -l) )); then
            ZOOM=4
        elif (( $(echo "$LAT_DIFF > 5" | bc -l) )) || (( $(echo "$LNG_DIFF > 5" | bc -l) )); then
            ZOOM=6
        elif (( $(echo "$LAT_DIFF > 1" | bc -l) )) || (( $(echo "$LNG_DIFF > 1" | bc -l) )); then
            ZOOM=8
        elif (( $(echo "$LAT_DIFF > 0.5" | bc -l) )) || (( $(echo "$LNG_DIFF > 0.5" | bc -l) )); then
            ZOOM=10
        else
            ZOOM=12
        fi
    }
    fi
    
    # Output file path
    OUTPUT_FILE="$OUTPUT_DIR/e_map_${event}.png"
    
    echo "Rendering map for $event (center: $CENTER_LAT,$CENTER_LNG, zoom: $ZOOM)"
    
    # Render the map using the Node.js script
    node "$NODE_SCRIPT" \
        "$GEOJSON_FILE" \
        "$MBTILES_FILE" \
        "$STYLE_DIR/mapstyle.json" \
        "$OUTPUT_FILE" \
        "$IMAGE_WIDTH" \
        "$IMAGE_HEIGHT" \
        "$CENTER_LAT" \
        "$CENTER_LNG" \
        "$ZOOM"
    
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}Successfully generated map image for $event: $OUTPUT_FILE${NC}"
    else
        echo -e "${RED}Failed to generate map image for $event${NC}"
    fi
    
    echo
done

echo -e "${GREEN}Map image generation complete!${NC}"
