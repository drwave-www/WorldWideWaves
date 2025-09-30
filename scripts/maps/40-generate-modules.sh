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

DEST_DIR=../../maps/
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

  INCLUDE_GRADLE='include(":maps:'$event'")'
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

# ========================================================================
# iOS ODR Symbolic Links Setup
# ========================================================================

echo "Setting up iOS ODR files to maps/* files..."

# Function to copy files for iOS ODR (all files go to ODR assetpacks)
create_ios_odr_files() {
    local ios_maps_dir="../../iosApp/worldwidewaves/Maps"

    # Create Maps directory if it doesn't exist
    mkdir -p "$ios_maps_dir"

    # Get all cities that have Android files (existing + new)
    local all_android_cities=()

    # Find all cities that have files in Android maps structure
    if [ -d "../../maps" ]; then
        while IFS= read -r -d '' android_city_dir; do
            local city
            city=$(basename "$android_city_dir")
            local geojson_file="$android_city_dir/src/main/assets/$city.geojson"
            local mbtiles_file="$android_city_dir/src/main/assets/$city.mbtiles"
            if [ -f "$geojson_file" ] && [ -f "$mbtiles_file" ]; then
                all_android_cities+=("$city")
            fi
        done < <(find "../../maps" -mindepth 1 -maxdepth 1 -type d -print0)
    fi

    # Add any new cities from current VALID_EVENTS that have files
    for event in "${VALID_EVENTS[@]}"; do
        local android_geojson="../../maps/$event/src/main/assets/$event.geojson"
        local android_mbtiles="../../maps/$event/src/main/assets/$event.mbtiles"
        if [ -f "$android_geojson" ] && [ -f "$android_mbtiles" ]; then
            local found=false
            for existing_city in "${all_android_cities[@]}"; do
                if [[ "$existing_city" == "$event" ]]; then
                    found=true
                    break
                fi
            done
            if [[ "$found" == "false" ]]; then
                all_android_cities+=("$event")
            fi
        fi
    done

    echo "Copying ODR files for ${#all_android_cities[@]} cities with complete Android files: ${all_android_cities[*]}"

    for event in "${all_android_cities[@]}"; do
        local event_dir="$ios_maps_dir/$event"
        local source_dir="../../maps/$event/src/main/assets"

        # Create event directory if it doesn't exist
        mkdir -p "$event_dir"

        # Copy files for geojson and mbtiles from Android location
        local geojson_dest="$event_dir/$event.geojson"
        local mbtiles_dest="$event_dir/$event.mbtiles"
        local geojson_source="$source_dir/$event.geojson"
        local mbtiles_source="$source_dir/$event.mbtiles"

        # Copy files only if destination is outdated (both files are guaranteed to exist)
        if [ ! -f "$geojson_dest" ] || [ "$geojson_source" -nt "$geojson_dest" ]; then
            cp "$geojson_source" "$geojson_dest"
            echo "Copied ODR file: $event.geojson"
        else
            echo "ODR file up to date: $event.geojson"
        fi

        if [ ! -f "$mbtiles_dest" ] || [ "$mbtiles_source" -nt "$mbtiles_dest" ]; then
            cp "$mbtiles_source" "$mbtiles_dest"
            echo "Copied ODR file: $event.mbtiles"
        else
            echo "ODR file up to date: $event.mbtiles"
        fi
    done
}

# Copy ODR files
if [ ${#VALID_EVENTS[@]} -gt 0 ]; then
    create_ios_odr_files
else
    echo "Skipping iOS ODR files: no valid events"
fi

# ========================================================================
# iOS Xcode Project ODR Configuration
# ========================================================================

echo "Configuring iOS ODR in Xcode project..."

# Path to Xcode project
IOS_XCODE_PROJECT="../../iosApp/worldwidewaves.xcodeproj/project.pbxproj"

# Function to add ODR asset tags to Xcode project with direct file references
add_odr_tags_to_xcode() {
    local project_file="$1"
    local temp_project="/tmp/worldwidewaves_project_temp.pbxproj"

    if [ ! -f "$project_file" ]; then
        echo "Warning: Xcode project not found at $project_file"
        return 1
    fi

    # Copy original project
    cp "$project_file" "$temp_project"

    # Get all cities that have iOS ODR files (existing + newly copied)
    local ios_maps_dir="../../iosApp/worldwidewaves/Maps"
    local all_ios_cities=()

    # Add existing cities from iOS Maps directory
    if [ -d "$ios_maps_dir" ]; then
        while IFS= read -r -d '' city_dir; do
            local city
            city=$(basename "$city_dir")
            if [ -f "$city_dir/$city.geojson" ] && [ -f "$city_dir/$city.mbtiles" ]; then
                all_ios_cities+=("$city")
            fi
        done < <(find "$ios_maps_dir" -mindepth 1 -maxdepth 1 -type d -print0)
    fi

    echo "Adding ODR asset tags for ${#all_ios_cities[@]} total cities with iOS files: ${all_ios_cities[*]}"

    # Generate asset tags by relative path entries - reference files in iOS project structure
    local asset_tags_entries=""
    for event in "${all_ios_cities[@]}"; do
        # Reference files in iOS project structure
        asset_tags_entries="${asset_tags_entries}				Maps/${event}/${event}.geojson = (${event}, );\n"
        asset_tags_entries="${asset_tags_entries}				Maps/${event}/${event}.mbtiles = (${event}, );\n"
    done

    # Generate known asset tags entries
    local known_tags_entries=""
    for event in "${all_ios_cities[@]}"; do
        known_tags_entries="${known_tags_entries}					${event},\n"
    done

    # Check if KnownAssetTags exists (current project uses this approach)
    if grep -q "KnownAssetTags" "$temp_project"; then
        echo "Found KnownAssetTags in Xcode project, updating with new file references..."

        # Replace existing KnownAssetTags section
        awk -v new_entries="$known_tags_entries" '
        /KnownAssetTags = \(/ {
            print
            print new_entries
            # Skip existing entries until closing parenthesis
            while (getline > 0 && !/^\t\t\t\t\);$/) {}
            print "\t\t\t\t);"
            next
        }
        { print }' "$temp_project" > "${temp_project}.tmp" && mv "${temp_project}.tmp" "$temp_project"

        # Add assetTagsByRelativePath section if it doesn't exist
        if ! grep -q "assetTagsByRelativePath" "$temp_project"; then
            echo "Adding assetTagsByRelativePath section for direct file references..."

            # Find the line with C17B46632E899ED40097A3A5 (the exception set) and add our section before it
            awk -v asset_entries="$asset_tags_entries" '
            /isa = PBXFileSystemSynchronizedBuildFileExceptionSet;/ {
                # Add our assetTagsByRelativePath section before the existing exception set
                print "\t\t\t\tassetTagsByRelativePath = {"
                print asset_entries
                print "\t\t\t};"
                print $0
                next
            }
            { print }' "$temp_project" > "${temp_project}.tmp" && mv "${temp_project}.tmp" "$temp_project"
        else
            # Update existing assetTagsByRelativePath section
            awk -v new_entries="$asset_tags_entries" '
            /assetTagsByRelativePath = {/ {
                print
                print new_entries
                # Skip existing entries until closing brace
                while (getline > 0 && !/^\t\t\t};$/) {}
                print "\t\t\t};"
                next
            }
            { print }' "$temp_project" > "${temp_project}.tmp" && mv "${temp_project}.tmp" "$temp_project"
        fi

    else
        echo "No KnownAssetTags found in Xcode project"
        echo "Please add ODR configuration manually or ensure the project structure is correct"
        return 1
    fi

    # Replace original with updated version
    mv "$temp_project" "$project_file"
    echo "Xcode project ODR configuration completed for ${#all_ios_cities[@]} total cities"
}

# Only configure if we have valid events and project exists
if [ ${#VALID_EVENTS[@]} -gt 0 ] && [ -f "$IOS_XCODE_PROJECT" ]; then
    add_odr_tags_to_xcode "$IOS_XCODE_PROJECT"
else
    echo "Skipping Xcode project configuration: ${#VALID_EVENTS[@]} events, project exists: $([ -f "$IOS_XCODE_PROJECT" ] && echo "yes" || echo "no")"
fi
