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

cd "$(dirname "$0")" # always work from executable folder

#set -x

# ---------- Download dependencies --------------------------------------------

# Download OpenMapTiles to generate events maps -------------------------------
[ ! -d openmaptiles ] && git clone git@github.com:openmaptiles/openmaptiles.git && rm -rf openmaptiles/.git

# Adapt openmaptiles docker-compose file
# Increase PostgreSQL shared memory size
./bin/yq '.services.postgres |= select(.shm_size == null ) |= .shm_size = "512m" | .' openmaptiles/docker-compose.yml -i

# ---------- Vars and support functions ---------------------------------------
. ./libs/lib.inc.sh

# -----------------------------------------------------------------------------

if [ ! -z "$1" ]; then
  IFS=', ' read -ra EVENT_ARRAY <<< "$1"
  VALID_EVENTS=()

  for event in "${EVENT_ARRAY[@]}"; do
    if [ -z "$event" ]; then
      continue
    fi

    if $(exists "$event"); then
      VALID_EVENTS+=("$event")
      rm -f "./data/$event.mbtiles"
    else
      echo "Unexistent event(s): ${INVALID_EVENTS[*]}"
    fi
  done

  if [ ${#VALID_EVENTS[@]} -eq 0 ]; then
    echo "No valid events provided"
    exit 1
  fi

  EVENTS="${VALID_EVENTS[*]}"
fi

for event in $EVENTS; do # Generate MBTILES files from PBF area files 
                         # EVENTS is defined in lib.inc.sh

  echo "==> EVENT $event"

  TYPE=$(conf $event type)

  if [ "$TYPE" = "world" ]; then
    echo "Skip the world"
    continue
  fi

  echo
  [ ! -f ./data/$event.mbtiles ] && ./libs/generate_map.dep.sh $event

done
