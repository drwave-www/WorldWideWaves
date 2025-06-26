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
NODE_SCRIPT="$TEMP_DIR/render-map.js"
GEOJSON_DIR="./data"

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
    "@maplibre/maplibre-gl-native": "^5.1.0",
    "fs-extra": "^11.1.1",
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

# Create the Node.js renderer script
cat > "$NODE_SCRIPT" << EOF
const fs = require('fs-extra');
const path = require('path');
const { PNG } = require('pngjs');
const maplibre = require('@maplibre/maplibre-gl-native');

// Function to render a map
async function renderMap(options) {
    const {
        geojsonPath,
        stylePath,
        outputPath,
        width,
        height,
        center,
        zoom
    } = options;

    try {
        // Read the GeoJSON file
        const geojsonData = await fs.readJson(geojsonPath);
        
        // Read the style file
        const styleData = await fs.readJson(stylePath);
        
        // Create a map
        const map = new maplibre.Map({
            width,
            height,
            ratio: 1,
            center,
            zoom,
            style: styleData,
            request: (req, callback) => {
                try {
                    // Handle file requests (sprites, fonts, etc.)
                    const url = req.url;
                    if (url.startsWith('file://')) {
                        const filePath = url.replace('file://', '');
                        if (fs.existsSync(filePath)) {
                            const data = fs.readFileSync(filePath);
                            callback(null, { data });
                        } else {
                            callback(new Error(\`File not found: \${filePath}\`));
                        }
                    } else {
                        callback(new Error(\`Unsupported URL: \${url}\`));
                    }
                } catch (error) {
                    callback(error);
                }
            }
        });
        
        // Add GeoJSON source and layer for the event area
        map.addSource('event-area', {
            type: 'geojson',
            data: geojsonData
        });
        
        map.addLayer({
            id: 'event-area-fill',
            type: 'fill',
            source: 'event-area',
            paint: {
                'fill-color': '#D33682',
                'fill-opacity': 0.5
            }
        });
        
        map.addLayer({
            id: 'event-area-line',
            type: 'line',
            source: 'event-area',
            paint: {
                'line-color': '#D33682',
                'line-width': 2
            }
        });
        
        // Render the map
        const pixels = await new Promise((resolve, reject) => {
            map.render((err, buffer) => {
                if (err) reject(err);
                else resolve(buffer);
            });
        });
        
        // Create a PNG from the pixels
        const png = new PNG({
            width,
            height,
            inputHasAlpha: true
        });
        
        // Copy the pixels to the PNG
        for (let i = 0; i < pixels.length; i++) {
            png.data[i] = pixels[i];
        }
        
        // Write the PNG to a file
        await new Promise((resolve, reject) => {
            png.pack()
                .pipe(fs.createWriteStream(outputPath))
                .on('finish', resolve)
                .on('error', reject);
        });
        
        console.log(\`Map rendered successfully: \${outputPath}\`);
        return true;
    } catch (error) {
        console.error(\`Error rendering map: \${error.message}\`);
        return false;
    }
}

// Parse command line arguments
const args = process.argv.slice(2);
if (args.length < 7) {
    console.error('Usage: node render-map.js <geojsonPath> <stylePath> <outputPath> <width> <height> <centerLat> <centerLng> [<zoom>]');
    process.exit(1);
}

const geojsonPath = args[0];
const stylePath = args[1];
const outputPath = args[2];
const width = parseInt(args[3], 10);
const height = parseInt(args[4], 10);
const centerLat = parseFloat(args[5]);
const centerLng = parseFloat(args[6]);
const zoom = args[7] ? parseFloat(args[7]) : 10;

renderMap({
    geojsonPath,
    stylePath,
    outputPath,
    width,
    height,
    center: [centerLng, centerLat],
    zoom
}).then(success => {
    process.exit(success ? 0 : 1);
});
EOF

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
        echo -e "${YELLOW}GeoJSON file not found for $event. Generating it...${NC}"
        ./30-retrieve-geojson.sh "$event"
        if [ ! -f "$GEOJSON_FILE" ]; then
            echo -e "${RED}Failed to generate GeoJSON for $event. Skipping.${NC}"
            continue
        fi
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
