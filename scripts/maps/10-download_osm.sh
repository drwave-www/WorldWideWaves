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

#set -x

mkdir -p ./bin
mkdir -p ./data

# ---------- Download dependencies --------------------------------------------

# git clone openmpatiles-tools to download OSM areas
[ ! -d openmaptiles-tools ] && git clone git@github.com:openmaptiles/openmaptiles-tools.git && rm -rf openmaptiles-tools/.git

[ ! -f ./bin/osmconvert ] && wget http://m.m.i24.cc/osmconvert64 -O ./bin/osmconvert && chnmod +x ./bin/osmconvert

# ---------- Vars and support functions ---------------------------------------
. ./libs/lib.inc.sh

# ----------

# DEBUG
#EVENTS=paris_france

for event in $EVENTS; do # Download OSM area as PBF file 
                         # and generates a dedicated PBF file for corresponding BBOX
                         # EVENTS is defined in lib.inc.sh
  echo "==> EVENT $event"
  BBOX=$(conf $event mapBbox)
  AREA=$(conf $event mapOsmarea)
  SPBF=data/osm-$(echo $AREA | sed -e 's/\//_/g').osm.pbf
  DPBF=data/www-${event}.osm.pbf

  echo "-- Download area $AREA from OSM.."
  [ ! -f $SPBF ] && ./openmaptiles-tools/bin/download-osm $AREA -o $SPBF

  echo "-- Extract bbox $BBOX from area $AREA.."
  [ ! -f $DPBF ] && ./bin/osmconvert $SPBF -b=$BBOX -o=$DPBF

  echo "-- Generates OpenMapTiles environment for event $event"
  tpl $event templates/.env-template data/.env-${event}

  echo "-- Generates OpenMapTiles tileset definition for event $event"
  tpl $event templates/template-omt.yaml data/${event}.yaml

done
