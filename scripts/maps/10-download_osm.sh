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

cd "$(dirname "$0")" || exit # always work from executable folder

#set -x

# Parse command line arguments
FORCE_GENERATION=false
EVENT_PARAM=""

while [[ "$#" -gt 0 ]]; do
  case $1 in
    -f|--force) FORCE_GENERATION=true ;;
    *)
      if [ -z "$EVENT_PARAM" ]; then
        EVENT_PARAM="$1"
      else
        echo "Unknown parameter: $1"
        exit 1
      fi
      ;;
  esac
  shift
done

mkdir -p ./bin
mkdir -p ./data

# ---------- Download dependencies --------------------------------------------

# git clone openmpatiles-tools to download OSM areas
# [ ! -d openmaptiles-tools ] && git clone git@github.com:openmaptiles/openmaptiles-tools.git && rm -rf openmaptiles-tools/.git
pip install openmaptiles-tools

# Download osmconvert to extract BBOX from OSM areas
#[ ! -f ./bin/osmconvert ] && wget http://m.m.i24.cc/osmconvert64 -O ./bin/osmconvert && chmod +x ./bin/osmconvert && (
#  cd openmaptiles-tools && make
#)

# ---------- Vars and support functions ---------------------------------------
. ./libs/lib.inc.sh

# -----------------------------------------------------------------------------

if [ ! -z "$EVENT_PARAM" ]; then
  if $(exists "$EVENT_PARAM"); then
    EVENTS=$EVENT_PARAM
    rm -f data/.env-"$EVENT_PARAM"
    rm -f data/"$EVENT_PARAM".yaml
  else
    echo "Unexistent event $EVENT_PARAM"
    exit 1
  fi
fi

for event in $EVENTS; do # Download OSM area as PBF file
                         # and generates a dedicated PBF file for corresponding BBOX
                         # EVENTS is defined in lib.inc.sh
  echo "==> EVENT $event"
  TYPE=$(conf "$event" type)

  if [ "$TYPE" = "world" ]; then
    echo "Skip the world"
    continue
  fi

  # Get the BBOX for this event using the helper function
  BBOX=$(get_event_bbox "$event")

  if [ $? -ne 0 ]; then
    echo "Failed to get BBOX for event $event"
    continue
  fi

  echo "Retrieved BBOX for event $event : $BBOX"

  AREAZONE=$(conf "$event" map.zone)
  SPBF=data/osm-$(echo "$AREAZONE" | sed -e 's,/,_,g').osm.pbf
  DPBF=data/www-${event}.osm.pbf

  echo "-- Download area $AREAZONE from OSM.."
  [ ! -f "$SPBF" ] && download-osm "$AREAZONE" -o "$SPBF"

  echo "-- Extract bbox $BBOX from area $AREAZONE.."
  if [ "$FORCE_GENERATION" = true ] || [ ! -f "$DPBF" ]; then
    ./bin/osmconvert "$SPBF" -b="$BBOX" -o="$DPBF" && ./bin/osmconvert "$DPBF" --out-statistics
  else
    echo "   DPBF file already exists. Use -f to force regeneration."
  fi

  echo "-- Generates OpenMapTiles environment for event $event"
  tpl "$event" templates/.env-template-"${TYPE}" data/.env-"${event}"

  echo "-- Generates OpenMapTiles tileset definition for event $event"
  tpl "$event" templates/template-omt-"${TYPE}".yaml data/"${event}".yaml

done
