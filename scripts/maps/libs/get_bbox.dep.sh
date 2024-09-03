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

#set -x

# Check if an admin ID is provided as an argument
if [ $# -eq 0 ]; then
    echo "Error: Please provide an admin ID as an argument."
    exit 1
fi

admin_id=$1 # Get the admin ID from the first argument

# Construct theOverpass API query URL
url="https://overpass-api.de/api/interpreter?data=\[out:json\];relation(id:$admin_id);out%20bb;"

# Fetch the JSON response using curl
response=$(curl -s "$url")

# Extract the bounding box coordinates using jq
minlat=$(echo "$response" | ./bin/jq -r '.elements[0].bounds.minlat')
minlon=$(echo "$response" | ./bin/jq -r '.elements[0].bounds.minlon')
maxlat=$(echo "$response" | ./bin/jq -r '.elements[0].bounds.maxlat')
maxlon=$(echo "$response" | ./bin/jq -r '.elements[0].bounds.maxlon')

# Calculate the center of the bounding box
center_lat=$(echo "($minlat + $maxlat) / 2" | bc -l | awk '{printf "%.7f\n", $0}' )
center_lon=$(echo "($minlon + $maxlon) / 2" | bc -l | awk '{printf "%.7f\n", $0}')

# Output the bounding box coordinates
bbox=$minlon,$minlat,$maxlon,$maxlat
center=$center_lon,$center_lat
if [ "$2" = "bbox" ]; then
  echo $bbox
elif [ "$2" = "center" ]; then 
  echo $center
else
  echo "bbox: $bbox"
  echo "center: $center"
fi
