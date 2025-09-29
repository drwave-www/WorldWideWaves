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

DEST_DIR=../../maps/android/
GRADLE_SETTINGS=../../settings.gradle.kts
IOS_INFO_PLIST=../../iosApp/worldwidewaves/Info.plist

cd "$(dirname "$0")" || exit # always work from executable folder

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
else
  if [ -z "$EVENTS" ]; then
    echo "No events available"
    exit 1
  fi
fi

# -----------------------------------------------------------------------------

for event in $EVENTS; do # Retrieve Geojson files from OSM
  echo "==> EVENT $event"
  echo

  [ ! -f "data/$event.mbtiles" ] && echo "MBTILES file has not been yet generated" && continue
  [ ! -f "data/$event.geojson" ] && echo "GEOJSON file has not been yet generated" && continue

  DEST_DIR_MODULE=$DEST_DIR/$event
  DEST_DIR_MODULE_MAIN=$DEST_DIR_MODULE/src/main
  DEST_DIR_MODULE_FILES=$DEST_DIR_MODULE/src/main/assets
  DEST_DIR_MODULE_VALUES=$DEST_DIR_MODULE/src/main/res/values

  mkdir -p "$DEST_DIR_MODULE"
  mkdir -p "$DEST_DIR_MODULE_FILES"
  mkdir -p "$DEST_DIR_MODULE_VALUES"

  tpl "$event" templates/template-android-build-gradle.kts "$DEST_DIR_MODULE/build.gradle.kts"
  tpl "$event" templates/template-AndroidManifest.xml "$DEST_DIR_MODULE_MAIN/AndroidManifest.xml"
  tpl "$event" templates/template-android-strings.xml "$DEST_DIR_MODULE_VALUES/strings.xml"

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
  if ! grep -q "$INCLUDE_GRADLE" "$GRADLE_SETTINGS"; then
    echo "$INCLUDE_GRADLE" >> "$GRADLE_SETTINGS"
  fi

done

# ========================================================================
# iOS Info.plist ODR Configuration (Idempotent)
# ========================================================================

echo "Configuring iOS ODR entries in Info.plist..."

# Check if Info.plist exists and handle Xcode "Generate Info.plist = Yes" scenario
if [ ! -f "$IOS_INFO_PLIST" ]; then
    echo "Warning: Info.plist not found at $IOS_INFO_PLIST"
    echo "If using Xcode 'Generate Info.plist = Yes', this is expected."
    echo "ODR tags will be added to project.pbxproj instead during Xcode build."
    exit 0
fi

# Function to add ODR entries to Info.plist idempotently
add_odr_entries_to_plist() {
    local plist_file="$1"
    local temp_plist="/tmp/worldwidewaves_info_temp.plist"

    # Copy original plist
    cp "$plist_file" "$temp_plist"

    # Check if NSBundleResourceRequestTags already exists
    if ! plutil -extract NSBundleResourceRequestTags xml1 -o - "$temp_plist" >/dev/null 2>&1; then
        # Add NSBundleResourceRequestTags dictionary
        plutil -insert NSBundleResourceRequestTags -dictionary "$temp_plist"
        echo "Added NSBundleResourceRequestTags to Info.plist"
    fi

    # Add entries for each valid event (idempotent)
    for event in "${VALID_EVENTS[@]}"; do
        if ! plutil -extract NSBundleResourceRequestTags."$event" xml1 -o - "$temp_plist" >/dev/null 2>&1; then
            # Add array for this event
            plutil -insert NSBundleResourceRequestTags."$event" -array "$temp_plist"
            # Add geojson and mbtiles file references
            plutil -insert NSBundleResourceRequestTags."$event" -string "$event.geojson" -append "$temp_plist"
            plutil -insert NSBundleResourceRequestTags."$event" -string "$event.mbtiles" -append "$temp_plist"
            echo "Added ODR tag for $event (geojson + mbtiles)"
        else
            echo "ODR tag for $event already exists, skipping"
        fi
    done

    # Replace original with updated version
    mv "$temp_plist" "$plist_file"
    echo "Info.plist ODR configuration completed"
}

# Only configure if we have valid events and plist exists
if [ ${#VALID_EVENTS[@]} -gt 0 ] && [ -f "$IOS_INFO_PLIST" ]; then
    add_odr_entries_to_plist "$IOS_INFO_PLIST"
else
    echo "Skipping Info.plist configuration: ${#VALID_EVENTS[@]} events, plist exists: $([ -f "$IOS_INFO_PLIST" ] && echo "yes" || echo "no")"
fi
