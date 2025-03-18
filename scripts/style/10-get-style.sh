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
# You may not use this file except in compliance with the License.
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

DEST_DIR=../../shared/src/commonMain/composeResources/files/style/
IN_STYLE_FILE=./osm-liberty/style.json
OUT_STYLE_FILE=./data/mapstyle.json

# Navigate to the directory of the script
cd "$(dirname "$0")"

# Download jq for JSON processing if not already present
mkdir -p ./bin
if [ ! -f ./bin/jq ]; then
  wget -q https://github.com/stedolan/jq/releases/latest/download/jq-linux64 -O ./bin/jq
  chmod +x ./bin/jq
fi

# ---------- Download dependencies --------------------------------------------

# Clone osm-liberty if not already present
if [ ! -d osm-liberty ]; then
  git clone --depth=1 git@github.com:maputnik/osm-liberty.git osm-liberty
  rm -rf osm-liberty/.git
fi

# Clone font-glyphs if not already present
if [ ! -d font-glyphs ]; then
  git clone --depth=1 git@github.com:orangemug/font-glyphs.git font-glyphs
  (cd font-glyphs && git submodule update --init)
  rm -rf font-glyphs/.git
fi

# ---------- Adapt style for MapLibre -----------------------------------------

# Create the data directory if it doesn't exist
mkdir -p ./data

# Modify the style.json for MapLibre native/WWW use
./bin/jq '
  (.sources.openmaptiles.url) |= "__MBTILES_URI__"
  | del(.sources.natural_earth_shaded_relief)
  | del(.layers[] | select(.id == "natural_earth"))
  | (.sprite) |= "__SPRITE_URI__"
  | (.glyphs) |= sub("^(.*)/\\{fontstack\\}(.*)$"; "__GLYPHS_URI__/{fontstack}/{range}.pbf")
  | .sources.geojson = { "data": "__GEOJSON_URI__", "type": "geojson" }
  | .layers += [
      {
        "id": "waveboundaryarea",
        "source": "geojson",
        "type": "fill",
        "paint": {
          "fill-color": "#0000ff",
          "fill-opacity": 0.1
        }
      },
      {
        "id": "waveboundaryline",
        "source": "geojson",
        "type": "line",
        "paint": {
          "line-color": "#0000ff",
          "line-width": 2
        }
      }
    ]
' "$IN_STYLE_FILE" > "$OUT_STYLE_FILE"

# Copy sprites to the data directory
cp -r osm-liberty/sprites/ ./data/

# ---------- Generate glyphs and copy them ------------------------------------

# Run the Docker Compose to generate glyphs
docker compose up -d

# Generate glyphs for Roboto fonts if not already present
if [ ! -d ./data/glyphs ]; then
  docker compose run --rm build sh -c '
    cd /font-glyphs
    npm audit fix
    npm install .
    ./generate.sh
  '
  mkdir -p ./data/glyphs
  cp -r \
    font-glyphs/glyphs/Roboto\ Condensed\ Italic/ \
    font-glyphs/glyphs/Roboto\ Medium/ \
    font-glyphs/glyphs/Roboto\ Regular/ \
    ./data/glyphs/
fi

# ---------- Update App Resources ---------------------------------------------

# Clean and copy updated data to the app's resource directory
rm -rf "$DEST_DIR"
mkdir -p "$DEST_DIR"
cp -r ./data/* "$DEST_DIR"

# Generate listing of glyph and sprite files
(cd ./data && find ./glyphs ./sprites -type f | sed 's/^\.\///') > ./data/listing

