#!/bin/bash
#
# Copyright 2024 DrWave
#
# WorldWideWaves is an ephemeral mobile app designed to orchestrate human waves through cities and countries,
# culminating in a global wave. The project aims to transcend physical and cultural boundaries, fostering unity,
# community, and shared human experience by leveraging real-time coordination and location-based services.
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

#set -x

# Download jq for JSON decoding with bash
mkdir -p ./bin
[ ! -f ./bin/jq ] && wget -np -q https://github.com/stedolan/jq/releases/latest/download/jq-linux64 -O ./bin/jq && chmod +x ./bin/jq

# ---------- Download dependencies --------------------------------------------

# git clone osm-liberty style 
[ ! -d osm-liberty ] && git clone git@github.com:maputnik/osm-liberty.git && rm -rf osm-liberty/.git

# git clone font-glyphs
[ ! -d font-glyphs ] && git clone git@github.com:orangemug/font-glyphs.git && (cd font-glyphs && git submodule sync && git submodule update --init) && rm -rf font-glyphs/.git

# ----------

mkdir -p ./data

IN_STYLE_FILE=./osm-liberty/style.json
OUT_STYLE_FILE=./data/mapstyle.json

# Adapt the style for MapLibre native / WWW
./bin/jq '
 (.sources.openmaptiles.url) |= "__MBTILES_URI__"
 | del(.sources.natural_earth_shaded_relief)
 | del(.layers[] | select(.id == "natural_earth"))
 | (.sprite) |= "__SPRITE_URI__"
 | (.glyphs) |= sub("^(.*)/\\{fontstack\\}(.*)$"; "__GLYPHS_URI__/{fontstack}/{range}.pbf")
' $IN_STYLE_FILE > $OUT_STYLE_FILE

# Copy the sprites
cp -r osm-liberty/sprites/ ./data/

# Generate and copy the glyphs for Roboto used in the style
docker compose up
[ ! -d ./data/glyphs ] && (
   docker compose run --rm build sh -c 'cd /font-glyphs ; npm audit fix ; npm install . ; ./generate.sh'
   mkdir -p data/glyphs
   cp -r \
     font-glyphs/glyphs/Roboto\ Condensed\ Italic/ \
     font-glyphs/glyphs/Roboto\ Medium \
     font-glyphs/glyphs/Roboto\ Regular/ \
     ./data/glyphs/
)

DEST_DIR=../../shared/src/commonMain/composeResources/files/style/
rm -rf $DEST_DIR
cp -r data $DEST_DIR

