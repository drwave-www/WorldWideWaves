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

# Output the bounding box coordinates
echo "Bounding Box: $minlon,$minlat,$maxlon,$maxlat"
