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

DEST_DIR=../../maps/android/
GRADLE_SETTINGS=../../settings.gradle.kts

cd "$(dirname "$0")" # always work from executable folder

#set -x

# Create necessary directories
mkdir -p ./data
mkdir -p $DEST_DIR

# ---------- Vars and support functions ---------------------------------------
. ./libs/lib.inc.sh

# -----------------------------------------------------------------------------

if [ ! -z "$1" ]; then
  if $(exists "$1"); then
    EVENTS=$1
  else
    echo "Unexistent event $1"
    exit 1
  fi
fi

for event in $EVENTS; do # Retrieve Geojson files from OSM
  echo "==> EVENT $event"
  echo

  [ ! -f "data/$event.mbtiles" ] && echo "MBTILES file has not been yet generated" && continue
  [ ! -f "data/$event.geojson" ] && echo "GEOJSON file has not been yet generated" && continue

  DEST_DIR_MODULE=$DEST_DIR/$event
  DEST_DIR_MODULE_MAIN=$DEST_DIR_MODULE/src/main
  DEST_DIR_MODULE_FILES=$DEST_DIR_MODULE/src/main/assets

  mkdir -p $DEST_DIR_MODULE
  mkdir -p $DEST_DIR_MODULE_FILES

  tpl $event templates/template-android-build-gradle.kts $DEST_DIR_MODULE/build.gradle.kts
  tpl $event templates/template-AndroidManifest.xml $DEST_DIR_MODULE_MAIN/AndroidManifest.xml
  cp -f data/$event.mbtiles $DEST_DIR_MODULE_FILES/
  cp -f data/$event.geojson $DEST_DIR_MODULE_FILES/
  [ ! -f "$DEST_DIR_MODULE/.gitignore" ] && echo "/build" > "$DEST_DIR_MODULE/.gitignore"

  INCLUDE_GRADLE='include(":maps:android:'$event'")'
  grep "$INCLUDE_GRADLE" "$GRADLE_SETTINGS" 2>&1 >/dev/null
  [ "$?" != 0 ] && echo "$INCLUDE_GRADLE" >> "$GRADLE_SETTINGS"

done
