#!/bin/bash

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
echo "bbox: $minlon,$minlat,$maxlon,$maxlat"
echo "center: $center_lon,$center_lat"
