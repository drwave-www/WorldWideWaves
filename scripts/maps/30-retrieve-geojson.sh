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

DEST_DIR=data/  #../../shared/src/commonMain/composeResources/files/maps

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

# Function to merge multiple GeoJSON files into one
# Usage: merge_geojsons <output_file> <input_file1> [<input_file2> ...]
merge_geojsons() {
  local output_file="$1"
  shift
  local input_files=("$@")

  # If there's only one input file, just copy it to the output
  if [ ${#input_files[@]} -eq 1 ]; then
    cp "${input_files[0]}" "$output_file"
    return
  fi

  # Build a single FeatureCollection containing features from every input file.
  #
  #  Each downloaded file from polygons.openstreetmap.fr is **not** a
  #  FeatureCollection but a raw `MultiPolygon` geometry JSON such as:
  #     { "type":"MultiPolygon","coordinates":[ ... ] }
  #
  #  We therefore need to gather all the `coordinates` arrays and wrap them
  #  explicitly in a FeatureCollection → Feature → MultiPolygon hierarchy.
  #
  #  jq explanation:
  #   -s                      : slurp all input files into a JSON array
  #   map(.coordinates)       : keep only the coordinates array of each object
  #   | add                   : concatenate the coordinate arrays
  #   {type:...,features:[...]}: build the final valid GeoJSON
  ./bin/jq -s '{
      type: "FeatureCollection",
      features: [
        {
          type: "Feature",
          geometry: {
            type: "MultiPolygon",
            coordinates: (map(.coordinates) | add)
          }
        }
      ]
    }' "${input_files[@]}" > "$output_file"
}

for event in $EVENTS; do # Retrieve Geojson files from OSM
  echo "==> EVENT $event"
  echo

  TYPE=$(conf "$event" type)

  if [ "$TYPE" = "world" ]; then
    echo "Skip the world"
    continue
  fi

  # Get the OSM admin IDs for this event
  ADMINIDS=$(get_osmAdminids "$event")

  if [ -z "$ADMINIDS" ]; then
    echo "Error: No area.osmAdminids found for event $event"
    continue
  fi

  echo "Retrieved OSM Admin IDs for event $event: $ADMINIDS"

  # Array to store the paths of downloaded GeoJSON files
  geojson_files=()

  # Download GeoJSON for each admin ID
  for admin_id in $(echo "$ADMINIDS" | tr ',' ' '); do
    echo "Downloading GeoJSON for OSM Admin ID: $admin_id"
    # Create a unique temporary filename for each admin ID
    temp_geojson="data/${event}_${admin_id}.geojson"
    wget "http://polygons.openstreetmap.fr/get_geojson.py?id=${admin_id}&params=0" -O "$temp_geojson"

    # Add this file to our array if the download was successful
    if [ -s "$temp_geojson" ]; then
      geojson_files+=("$temp_geojson")
    else
      echo "Warning: Failed to download GeoJSON for admin ID $admin_id"
    fi
  done

  # Merge all downloaded GeoJSON files into a single one
  #DEST_GEOJSON=$DEST_DIR/$event.geojson
  MERGED_GEOJSON="data/${event}.geojson"

  echo "Merging ${#geojson_files[@]} GeoJSON files for event $event"
  merge_geojsons "$MERGED_GEOJSON" "${geojson_files[@]}"

  # Copy the final merged GeoJSON to the destination
  #cp "$MERGED_GEOJSON" "$DEST_GEOJSON"
  echo "Created GeoJSON for event $event at $MERGED_GEOJSON"

  # Clean up temporary files if needed
  for temp_file in "${geojson_files[@]}"; do
    if [ "$temp_file" != "$MERGED_GEOJSON" ]; then
      rm "$temp_file"
    fi
  done

  echo
done
