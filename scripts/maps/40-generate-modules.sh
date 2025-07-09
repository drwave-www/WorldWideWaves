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

  # 1) Check the MBTiles file
  if [ ! -f "$DEST_DIR_MODULE_FILES/$event.mbtiles" ] || \
     [ "$(md5sum "data/$event.mbtiles" | awk '{print $1}')" != "$(md5sum "$DEST_DIR_MODULE_FILES/$event.mbtiles" 2>/dev/null | awk '{print $1}')" ]; then
      cp -f "data/$event.mbtiles" "$DEST_DIR_MODULE_FILES/"
      echo "Copied $event.mbtiles because it did not exist or differed by MD5."
  fi

  # 2) Check the GeoJSON file
  if [ ! -f "$DEST_DIR_MODULE_FILES/$event.geojson" ] || \
     [ "$(md5sum "data/$event.geojson" | awk '{print $1}')" != "$(md5sum "$DEST_DIR_MODULE_FILES/$event.geojson" 2>/dev/null | awk '{print $1}')" ]; then
      cp -f "data/$event.geojson" "$DEST_DIR_MODULE_FILES/"
      echo "Copied $event.geojson because it did not exist or differed by MD5."
  fi

  [ ! -f "$DEST_DIR_MODULE/.gitignore" ] && echo "/build" > "$DEST_DIR_MODULE/.gitignore"

  INCLUDE_GRADLE='include(":maps:android:'$event'")'
  grep "$INCLUDE_GRADLE" "$GRADLE_SETTINGS" 2>&1 >/dev/null
  [ "$?" != 0 ] && echo "$INCLUDE_GRADLE" >> "$GRADLE_SETTINGS"

done
