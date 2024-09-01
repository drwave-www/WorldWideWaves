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

#!/bin/bash

# Events configuration file
EVENTS_FILE=../../shared/src/commonMain/composeResources/files/events.json
echo "--> Using events file $EVENTS_FILE"

# Download jq for JSON processing if not already present
mkdir -p ./bin
if [ ! -f ./bin/jq ]; then
  wget -q https://github.com/stedolan/jq/releases/latest/download/jq-linux64 -O ./bin/jq
  chmod +x ./bin/jq
fi

# Download yq for YAML processing if not already present
if [ ! -f ./bin/yq ]; then
  wget -q https://github.com/mikefarah/yq/releases/download/v4.44.3/yq_linux_amd64 -O ./bin/yq
  chmod +x ./bin/yq
fi

# List of all configured events
EVENTS=$(./bin/jq -r '.[] | .id' "$EVENTS_FILE")

exists() {
  echo $EVENTS | grep "$1" > /dev/null
}

# Function to read an event's configuration property
# Usage: conf <event_id> <property_name>
conf() {
  ./bin/jq -r --arg event "$1" \
    ".[] | select(.id == \$event) | .$2" "$EVENTS_FILE"
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

  # Retrieve the event's mapOsmadminid and bbox information
  local mapOsmadminid
  mapOsmadminid=$(conf "$event" "area.osmAdminid")

  local bbox_output
  bbox_output=$(./libs/get_bbox.dep.sh "$mapOsmadminid")

  local bbox center
  bbox=$(echo "$bbox_output" | head -n 1 | cut -d ':' -f 2)
  center=$(echo "$bbox_output" | tail -n 1 | cut -d ':' -f 2)

  # Replace placeholders with event properties and bbox/center data
  ./bin/jq -r 'paths | map(tostring) | join(".")' "$EVENTS_FILE" | sed -e 's/^[0-9\.]\.*//' | sort | uniq | while read -r prop; do
  if [ -n "$prop" ] && [ "$(conf $event $prop | wc -l)" = "1" ]; then
      sed -i \
        -e "s/#${prop}#/$(conf "$event" "$prop" | sed 's/\//\\\//g')/g" \
        -e "s/#map.center#/$center/g" \
        -e "s/#map.bbox#/$bbox/g" \
        "$tpl_file"
    fi
  done

  # Move the processed template to the output file
  mv "$tpl_file" "$output_file"
}


