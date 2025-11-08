#!/bin/bash
#
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
#
#
#

if [ -z "$DISPLAY" ] && [ -z "$XVFB_RUNNING" ]; then
    echo "No DISPLAY environment variable found. Checking for xvfb-run..."

    if ! command -v xvfb-run &> /dev/null; then
        echo "Error: xvfb-run is not available. Please install it or run in an X environment."
        echo "On Ubuntu/Debian: sudo apt-get install xvfb"
        echo "On CentOS/RHEL: sudo yum install xorg-x11-server-Xvfb"
        exit 1
    fi

    echo "Running script through xvfb-run..."
    export XVFB_RUNNING=1
    exec xvfb-run -a --server-args="-screen 0 1024x768x24" "$0" "$@"

    # This line should never be reached
    echo "Error: Failed to execute xvfb-run"
    exit 1
fi

# ---------- Vars and support functions ---------------------------------------
cd "$(dirname "$0")" || exit # always work from executable folder
. ./libs/lib.inc.sh
# shellcheck disable=SC2164
cd "$(dirname "$0")" # always work from executable folder

# Create necessary directories
mkdir -p ./data
mkdir -p ./bin

# Constants
IMAGE_WIDTH=1024
# DIM_EVENT_MAP_RATIO is 16/9 from WWWGlobals.kt
IMAGE_HEIGHT=$(echo "$IMAGE_WIDTH / (16/9)" | bc -l | xargs printf "%.0f")
OUTPUT_DIR="../../shared/src/commonMain/composeResources/drawable"
STYLE_DIR="../../shared/src/commonMain/composeResources/files/style"
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

# Setup Node.js version (use v18 for better compatibility with native modules)
if [ -s "$HOME/.nvm/nvm.sh" ]; then
    . "$HOME/.nvm/nvm.sh"
    # Try to use Node 18 if available, otherwise use whatever is current
    nvm use 18 &>/dev/null || true
fi

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

# Install required packages if they're not already installed
echo -e "${BLUE}Checking for required Node.js packages...${NC}"
if [ ! -d "node_modules" ]; then
    echo -e "${YELLOW}Installing required Node.js packages...${NC}"
    npm install
    if [ $? -ne 0 ]; then
        echo -e "${RED}Failed to install required Node.js packages.${NC}"
        exit 1
    fi
fi

# Check if @maplibre/maplibre-gl-native can load with current environment
# Try to use custom libpng if it exists (for systems with libpng version mismatch)
if [ -f "$HOME/local/lib/libpng16.so.16.37.0" ]; then
    export LD_LIBRARY_PATH="$HOME/local/lib:$LD_LIBRARY_PATH"
    echo -e "${GREEN}✓ Using custom libpng from $HOME/local/lib${NC}"
fi

if ! node -e "require('@maplibre/maplibre-gl-native')" 2>/dev/null; then
    echo "=========================================================================="
    echo -e "${RED}ERROR: Cannot load @maplibre/maplibre-gl-native module${NC}"
    echo "=========================================================================="
    echo ""
    echo "This is usually caused by a native dependency version mismatch."
    echo ""
    echo "Common solutions:"
    echo "  1. Build libpng 1.6.37 locally:"
    echo "     mkdir -p ~/local/src && cd ~/local/src"
    echo "     curl -L https://sourceforge.net/projects/libpng/files/libpng16/1.6.37/libpng-1.6.37.tar.xz/download -o libpng-1.6.37.tar.xz"
    echo "     tar -xf libpng-1.6.37.tar.xz && cd libpng-1.6.37"
    echo "     ./configure --prefix=\$HOME/local && make -j4"
    echo "     mkdir -p \$HOME/local/lib"
    echo "     cp .libs/libpng16.so.16.37.0 \$HOME/local/lib/"
    echo "     cd \$HOME/local/lib && ln -sf libpng16.so.16.37.0 libpng16.so.16"
    echo ""
    echo "  2. Then run this script with:"
    echo "     LD_LIBRARY_PATH=\$HOME/local/lib:\$LD_LIBRARY_PATH ./35-generate-default-map-images.sh"
    echo ""
    echo "  3. Or use Docker (if available):"
    echo "     See docs/maplibre-rendering.md for Docker instructions"
    echo ""
    echo "=========================================================================="
    exit 1
fi

echo -e "${GREEN}✓ MapLibre native module loaded successfully${NC}"
echo ""

# ---------- Process maps ---------------------------------------------------

if [ $# -gt 0 ]; then
  ALL_PARAMS="$*"

  IFS=', ' read -ra EVENT_ARRAY <<< "$ALL_PARAMS"
  VALID_EVENTS=()

  for event in "${EVENT_ARRAY[@]}"; do
    if [ -z "$event" ]; then
      continue
    fi

    if exists "$event"; then
      VALID_EVENTS+=("$event")
    else
      echo "Unexistent event: $event"
    fi
  done

  if [ ${#VALID_EVENTS[@]} -eq 0 ]; then
    echo "No valid events provided"
    exit 1
  fi

  EVENTS="${VALID_EVENTS[*]}"
else
  if [ -z "$EVENTS" ]; then
    echo "No events available"
    exit 1
  fi
fi

# -----------------------------------------------------------------------------

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

    # Check if MBTiles file exists
    MBTILES_FILE="$MBTILES_DIR/${event}.mbtiles"
    if [ ! -f "$MBTILES_FILE" ]; then
        echo -e "${YELLOW}MBTILES file not found for $event. Skipping.${NC}"
        continue
    fi
    
    # Output file path
    OUTPUT_FILE="$OUTPUT_DIR/e_map_${event}.png"
    
    # Detect if we have an explicit bbox in events.json.
    BBOX_RAW=$(get_event_bbox "$event")
    NODE_EXTRA_ARGS=""

    if [ -n "$BBOX_RAW" ] && [ "$BBOX_RAW" != "null" ]; then
        IFS=',' read -r MIN_LNG MIN_LAT MAX_LNG MAX_LAT <<< "$BBOX_RAW"
        NODE_EXTRA_ARGS="$MIN_LNG $MIN_LAT $MAX_LNG $MAX_LAT"
        echo "Rendering map for $event (using explicit area.bbox)"
    else
        echo "Rendering map for $event (bbox will be derived from GeoJSON)"
    fi
    
    # Render the map using the Node.js script.
    # shellcheck disable=SC2086 # intentional word-splitting for NODE_EXTRA_ARGS
    node "$NODE_SCRIPT" \
        "$GEOJSON_FILE" \
        "$MBTILES_FILE" \
        "$STYLE_DIR/mapstyle.json" \
        "$OUTPUT_FILE" \
        "$IMAGE_WIDTH" \
        "$IMAGE_HEIGHT" \
        $NODE_EXTRA_ARGS
    
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}Successfully generated map image for $event: $OUTPUT_FILE${NC}"
    else
        echo -e "${RED}Failed to generate map image for $event${NC}"
    fi
    
    echo
done

echo -e "${GREEN}Map image generation complete!${NC}"
