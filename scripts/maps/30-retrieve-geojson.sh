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

cd "$(dirname "$0")" # always work from executable folder

set -x

# ---------- Vars and support functions ---------------------------------------
. ./libs/lib.inc.sh

# ----------

for event in $EVENTS; do # Generate MBTILES files from PBF area files 
                         # EVENTS is defined in lib.inc.sh

  echo "==> EVENT $event"
  echo

  OSMADMINID=$(conf $event mapOsmadminid)
  echo $OSMADMINID


  DEST_GEOJSON=../../shared/src/commonMain/composeResources/files/maps/tiles/$event.geojson
  wget http://polygons.openstreetmap.fr/get_geojson.py?id=${OSMADMINID}\&params=0 -O data/$event.geojson
  cp data/$event.geojson $DEST_GEOJSON


done
