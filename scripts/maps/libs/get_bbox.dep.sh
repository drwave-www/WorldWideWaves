#!/bin/bash
#
# Copyright 2025 DrWave
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

#set -x

# Check if admin IDs are provided as arguments
if [ $# -eq 0 ]; then
    echo "Error: Please provide at least one admin ID as an argument."
    exit 1
fi

# Initialize variables for the final bounding box
final_minlat=90
final_minlon=180
final_maxlat=-90
final_maxlon=-180

# Process each admin ID
admin_ids=$(echo "$1" | tr ',' ' ')
for admin_id in $admin_ids; do
    # Construct the Overpass API query URL
    url="https://overpass-api.de/api/interpreter?data=\[out:json\];relation(id:$admin_id);out%20bb;"

    # Fetch the JSON response using curl
    response=$(curl -s "$url")

    # Extract the bounding box coordinates using jq
    minlat=$(echo "$response" | ./bin/jq -r '.elements[0].bounds.minlat')
    minlon=$(echo "$response" | ./bin/jq -r '.elements[0].bounds.minlon')
    maxlat=$(echo "$response" | ./bin/jq -r '.elements[0].bounds.maxlat')
    maxlon=$(echo "$response" | ./bin/jq -r '.elements[0].bounds.maxlon')

    # Update the final bounding box to encompass all areas
    if (( $(echo "$minlat < $final_minlat" | bc -l) )); then final_minlat=$minlat; fi
    if (( $(echo "$minlon < $final_minlon" | bc -l) )); then final_minlon=$minlon; fi
    if (( $(echo "$maxlat > $final_maxlat" | bc -l) )); then final_maxlat=$maxlat; fi
    if (( $(echo "$maxlon > $final_maxlon" | bc -l) )); then final_maxlon=$maxlon; fi
done

# Calculate the center of the final bounding box
center_lat=$(echo "($final_minlat + $final_maxlat) / 2" | bc -l | awk '{printf "%.7f\n", $0}' )
center_lon=$(echo "($final_minlon + $final_maxlon) / 2" | bc -l | awk '{printf "%.7f\n", $0}')

# Output the bounding box coordinates
bbox=$final_minlon,$final_minlat,$final_maxlon,$final_maxlat
center=$center_lon,$center_lat
if [ "$2" = "bbox" ]; then
  echo "$bbox"
elif [ "$2" = "center" ]; then 
  echo "$center"
else
  echo "bbox: $bbox"
  echo "center: $center"
fi