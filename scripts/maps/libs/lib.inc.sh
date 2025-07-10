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

# Events configuration file
EVENTS_FILE=../../shared/src/commonMain/composeResources/files/events.json
echo "--> Using events file $EVENTS_FILE"

# ---------------------------------------------------------------------------
# Detect platform (Linux vs macOS) so we fetch the right jq/yq binaries and
# use the proper in-place syntax for `sed -i`.
# ---------------------------------------------------------------------------
OS_NAME="$(uname -s)"

JQ_URL="https://github.com/stedolan/jq/releases/latest/download/jq-linux64"
YQ_URL="https://github.com/mikefarah/yq/releases/download/v4.44.3/yq_linux_amd64"
SED_INPLACE_FLAG=""

if [ "$OS_NAME" = "Darwin" ]; then
  # macOS binaries
  JQ_URL="https://github.com/stedolan/jq/releases/latest/download/jq-osx-amd64"
  YQ_URL="https://github.com/mikefarah/yq/releases/download/v4.44.3/yq_darwin_amd64"
  SED_INPLACE_FLAG="''"
fi

# Download jq for JSON processing if not already present
mkdir -p ./bin
if [ ! -f ./bin/jq ]; then
  wget -q "$JQ_URL" -O ./bin/jq
  chmod +x ./bin/jq
fi

# Download yq for YAML processing if not already present
if [ ! -f ./bin/yq ]; then
  wget -q "$YQ_URL" -O ./bin/yq
  chmod +x ./bin/yq
fi

# List of all configured events
EVENTS=$(./bin/jq -r '.[] | .id' "$EVENTS_FILE")

exists() {
  echo "$EVENTS" | grep "$1" > /dev/null
}

# Function to read an event's configuration property
# Usage: conf <event_id> <property_name>
conf() {
  ./bin/jq -r --arg event "$1" \
    ".[] | select(.id == \$event) | .$2" "$EVENTS_FILE"
}

# Function to get the osmAdminids for an event
# It handles backward compatibility with the old osmAdminid field
# Usage: get_osmAdminids <event_id>
get_osmAdminids() {
  local event="$1"
  local ids
  
  # First try to get the osmAdminids as an array
  ids=$(./bin/jq -r --arg event "$event" \
    '.[] | select(.id == $event) | .area.osmAdminids | if type=="array" then map(tostring) | join(",") else . end' "$EVENTS_FILE")

  # If osmAdminids doesn't exist or is null, try the legacy osmAdminid field for backward compatibility
  if [ "$ids" = "null" ] || [ -z "$ids" ]; then
    ids=$(./bin/jq -r --arg event "$event" \
      '.[] | select(.id == $event) | .area.osmAdminid' "$EVENTS_FILE")
    
    # If neither field exists, return an error
    if [ "$ids" = "null" ] || [ -z "$ids" ]; then
      echo ""
      return 1
    fi
  fi
  
  echo "$ids"
}

# Function to get the bbox for an event
# It first checks if area.bbox is specified; if not, it gets it from area.osmAdminids
# Usage: get_event_bbox <event_id>
get_event_bbox() {
  local event="$1"
  local direct_bbox
  direct_bbox=$(conf "$event" "area.bbox")
  
  # If a direct bbox is specified, use that
  if [ "$direct_bbox" != "null" ] && [ -n "$direct_bbox" ]; then
    adjust_bbox_to_16_9 "$direct_bbox"
    return
  fi
  
  # Get the OSM admin IDs
  local osmAdminids
  osmAdminids=$(get_osmAdminids "$event")
  
  if [ -z "$osmAdminids" ]; then
    echo "Error: No area.bbox or area.osmAdminids found for event $event" >&2
    return 1
  fi
  
  # Use the admin IDs to get the bbox
  local calc_bbox
  calc_bbox=$(./libs/get_bbox.dep.sh "$osmAdminids" bbox)
  adjust_bbox_to_16_9 "$calc_bbox"
}

# -----------------------------------------------------------------------------
# Ensure returned bbox keeps a 16:9 aspect-ratio.
#   • If current ratio < 16/9 → expand width (keep height)
#   • Else                  → expand height (keep width)
# In every expansion the original box is centred in the new one.
# -----------------------------------------------------------------------------
# Usage: adjust_bbox_to_16_9 "minLng,minLat,maxLng,maxLat"
# Returns: adjusted bbox string with 6-decimal precision
adjust_bbox_to_16_9() {
  local bbox="$1"
  if [ -z "$bbox" ]; then
    echo ""
    return
  fi

  IFS=',' read -r minLng minLat maxLng maxLat <<< "$bbox"

  # Validate numeric
  for v in "$minLng" "$minLat" "$maxLng" "$maxLat"; do
    if ! [[ $v =~ ^-?[0-9.]+$ ]]; then
      echo "$bbox"
      return
    fi
  done

  # Constants
  local targetRatio="1.777777777" # 16/9

  # Width / Height
  local width height ratio
  width=$(echo "$maxLng - $minLng" | bc -l)
  height=$(echo "$maxLat - $minLat" | bc -l)

  # Guard against zero height/width
  if [ "$(echo "$height == 0" | bc)" -eq 1 ] || \
     [ "$(echo "$width == 0"  | bc)" -eq 1 ]; then
    echo "$bbox"
    return
  fi

  ratio=$(echo "$width / $height" | bc -l)

  # If already ~16/9 (within 1%), keep original
  if [ "$(echo "($ratio / $targetRatio) > 0.99 && ($ratio / $targetRatio) < 1.01" | bc -l)" -eq 1 ]; then
    printf "%.6f,%.6f,%.6f,%.6f" "$minLng" "$minLat" "$maxLng" "$maxLat"
    return
  fi

  # Expand logic
  if [ "$(echo "$ratio < $targetRatio" | bc -l)" -eq 1 ]; then
    # Too narrow → enlarge width
    local newWidth delta
    newWidth=$(echo "$height * $targetRatio" | bc -l)
    delta=$(echo "$newWidth - $width" | bc -l)
    minLng=$(echo "$minLng - $delta/2" | bc -l)
    maxLng=$(echo "$maxLng + $delta/2" | bc -l)
  else
    # Too wide → enlarge height
    local newHeight deltaH
    newHeight=$(echo "$width / $targetRatio" | bc -l)
    deltaH=$(echo "$newHeight - $height" | bc -l)
    minLat=$(echo "$minLat - $deltaH/2" | bc -l)
    maxLat=$(echo "$maxLat + $deltaH/2" | bc -l)
  fi

  printf "%.6f,%.6f,%.6f,%.6f" "$minLng" "$minLat" "$maxLng" "$maxLat"
}

# Function to get the center for an event
# It follows the same logic as get_event_bbox
# Usage: get_event_center <event_id>
get_event_center() {
  local event="$1"
  local direct_center
  direct_center=$(conf "$event" "area.center")
  
  # If a direct center is specified, use that
  if [ "$direct_center" != "null" ] && [ -n "$direct_center" ]; then
    echo "$direct_center"
    return
  fi
  
  # Get the OSM admin IDs
  local osmAdminids
  osmAdminids=$(get_osmAdminids "$event")
  
  if [ -z "$osmAdminids" ]; then
    echo "Error: No area.center or area.osmAdminids found for event $event" >&2
    return 1
  fi
  
  # Use the admin IDs to get the center
  ./libs/get_bbox.dep.sh "$osmAdminids" center
}

safe_replace() {
  local pattern=$1
  local value=$2
  local escaped_value=$(echo "$value" | sed 's/[\/&~]/\\&/g')
  echo "s~$pattern~$escaped_value~g"
}

# Function to replace placeholders in the template file with event configuration values
# Usage: tpl <event_id> <template_file> <output_file>
tpl() {
  local event="$1"
  local template_file="$2"
  local output_file="$3"

  # Copy template to a temporary file
  local tpl_file
  tpl_file=$(mktemp)
  cp "$template_file" "$tpl_file"

  # Get bbox and center information
  local bbox center
  bbox=$(get_event_bbox "$event")
  center=$(get_event_center "$event")

  # Replace placeholders with event properties and bbox/center data
  # First, handle array values specifically
  # Replace osmAdminids array elements with their values using proper array syntax

  # Then handle all other properties using the standard approach
  ./bin/jq -r 'paths | map(tostring) | join(".")' "$EVENTS_FILE" | grep -v '\[[0-9]\]' | grep -v '[0-9]$' | sed -e 's/^[0-9\.]*\.*//' | sort | uniq | while read -r prop; do
    if [ -n "$prop" ] && [ "$(conf "$event" "$prop" | wc -l)" = "1" ]; then
      # Use portable in-place editing for both GNU and BSD sed
      eval sed -i $SED_INPLACE_FLAG \
        -e "\"$(safe_replace "#${prop}#" "$(conf "$event" "$prop")")\"" \
        -e "\"$(safe_replace "#map.center#" "$center")\"" \
        -e "\"$(safe_replace "#map.bbox#" "$bbox")\"" \
        "$tpl_file"
    fi
  done

  # Move the processed template to the output file
  mv "$tpl_file" "$output_file"
}
