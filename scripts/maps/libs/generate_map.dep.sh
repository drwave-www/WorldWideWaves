#!/bin/bash
## Created from original openmaptiles/quickstart.sh
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
# CALL: ./generate_map.sh [--empty] event_id

DEST_DIR=../../../shared/src/commonMain/composeResources/files/maps

cd "$(dirname "$0")"/..

set -o errexit
set -o pipefail
set -o nounset
# set -x

# Use a preloaded docker image to speed up, unless the --empty flag is used.
# If --empty is not given, use preloaded docker image to speed up
if [ $# -gt 0 ] && [[ $1 == --empty ]]; then
  export USE_PRELOADED_IMAGE=""
  shift
else
  export USE_PRELOADED_IMAGE=true
fi

# Check area specified in argument --------------------------------------------
if [ $# -eq 0 ]; then
  echo "no area specified"
  exit 1
else
  export area=$1
fi

# Setup configuration files before execution ----------------------------------
if [ ! -f ./data/.env-$area ]; then
  echo "Environment has not been created for area $area, launch './download.sh' in parent directory, ensure an event has been configured too in ../../events.yaml"
  exit 1
else
  cat .omt-env-global ./data/.env-$area > ./openmaptiles/.env
  cp ./data/${area}.yaml openmaptiles/
fi


# ========== PWD to OpenMapTiles environment ==================================
# =============================================================================
# =============================================================================
#
                             cd openmaptiles
#
# =============================================================================
# =============================================================================


##  Min versions ... ----------------------------------------------------------
MIN_COMPOSE_VER=1.7.1
MIN_DOCKER_VER=1.12.3
STARTTIME=$(date +%s)
STARTDATE=$(date +"%Y-%m-%dT%H:%M%z")

log_file=./quickstart.log
rm -f $log_file

# DOCKER and DOCKER COMPOSE checks --------------------------------------------
#if ! command -v docker-compose &> /dev/null; then
  DOCKER_COMPOSE_HYPHEN=false
#else
#  DOCKER_COMPOSE_HYPHEN=true
#fi

function docker_compose_command () {
    if $DOCKER_COMPOSE_HYPHEN; then
      docker-compose $@
    else
      docker compose $@
    fi
}

docker --version
docker_compose_command version

# based on: http://stackoverflow.com/questions/16989598/bash-comparing-version-numbers
function version { echo "$@" | tr -d 'v' | tr -cs '0-9.' '.' | awk -F. '{ printf("%03d%03d%03d\n", $1,$2,$3); }'; }

COMPOSE_VER=$(docker_compose_command version --short)
if [ "$(version "$COMPOSE_VER")" -lt "$(version "$MIN_COMPOSE_VER")" ]; then
  echo "ERR: Your Docker-compose version is known to have bugs, please update docker-compose!"
  exit 1
fi

DOCKER_VER="$(docker -v | awk -F '[ ,]+' '{ print $3 }')"
if [ "$(version "$DOCKER_VER")" -lt "$(version "$MIN_DOCKER_VER")" ]; then
  echo "ERR: Your Docker version is not compatible. Please Update docker!"
  exit 1
fi

# Pulling or refreshing OpenMapTiles docker images ----------------------------
make refresh-docker-images


# backup log from here ...
exec &> >(tee -a "$log_file")

# OS check
if [[ "$OSTYPE" == "linux-gnu" ]]; then
    KERNEL_CPU_VER=$(uname -m)
    if [ "$KERNEL_CPU_VER" != "x86_64" ]; then
      echo "ERR: Sorry this is working only on x86_64!"
      exit 1
    fi
else
    echo " "
    echo "Warning : Platforms other than Linux are less tested"
    echo " "
fi

# Start generation process ----------------------------------------------------

MBTILES_FILE=${MBTILES_FILE:-$(source .env ; echo "$MBTILES_FILE")}

echo "====> : Stopping running services & removing old containers"
echo
make destroy-db

echo "====> : Existing OpenMapTiles docker images. Will use version $(source .env && echo "$TOOLS_VERSION")"
echo
docker images | grep openmaptiles

echo "====> : Create directories if they don't exist"
echo
make init-dirs

echo "====> : Removing old MBTILES if exists ( ./data/$MBTILES_FILE ) "
echo
rm -f "./data/$MBTILES_FILE"

echo "====> : Copy downloaded PBF file ${area} (./10-download.sh) ..."
echo
cp -f ../data/www-${area}.osm.pbf data/${area}.osm.pbf


echo "====> : Remove old generated source files ( ./build/* ) ( if they exist ) "
echo
make clean

echo "====> : Code generating from the layer definitions ( ./build/mapping.yaml; ./build/sql/* )"
echo
make all

echo "-------------------------------------------------------------------------------------"
if [[ "$USE_PRELOADED_IMAGE" == true ]]; then
  echo "====> : Start PostgreSQL service using postgis image preloaded with this data:"
  make start-db-preloaded
else
  echo "====> : Start PostgreSQL service using empty database and importing all the data:"
  make start-db
  make import-data
fi
echo

echo "-------------------------------------------------------------------------------------"
echo "====> : Start importing OpenStreetMap data: ${area} -> imposm3[./build/mapping.yaml] -> PostgreSQL"
echo
make import-osm

echo "-------------------------------------------------------------------------------------"
echo "====> : Start importing Wikidata: Wikidata Query Service -> PostgreSQL"
echo
make import-wikidata

echo "-------------------------------------------------------------------------------------"
echo "====> : Start SQL postprocessing:  ./build/sql/* -> PostgreSQL "
echo
# If the output contains a WARNING, stop further processing
# Adapted from https://unix.stackexchange.com/questions/307562
make import-sql

echo "-------------------------------------------------------------------------------------"
echo "====> : Analyze PostgreSQL tables"
echo
make analyze-db

echo "-------------------------------------------------------------------------------------"
echo "====> : Testing PostgreSQL tables to match layer definitions metadata"
echo
make test-perf-null

echo "-------------------------------------------------------------------------------------"
echo "====> : Start generating MBTiles (containing gzipped MVT PBF) using PostGIS. "
echo "      : Output MBTiles: $MBTILES_FILE  "
echo
make generate-tiles-pg

echo "-------------------------------------------------------------------------------------"
echo "====> : Stop PostgreSQL service ( but we keep PostgreSQL data volume for debugging )"
echo
make stop-db

ENDTIME=$(date +%s)

echo
echo "-------------------------------------------------------------------------------------"
echo "--                           S u m m a r y                                         --"
echo "-------------------------------------------------------------------------------------"
echo
echo "====> : (disk space) We have created a lot of docker images: "
echo "      : Hint: you can remove with:  docker rmi IMAGE "
echo
docker images | grep openmaptiles

echo "-------------------------------------------------------------------------------------"
echo "The generation for  $area  is finished! "
echo "It took $((ENDTIME - STARTTIME)) seconds to complete"
echo "We saved the log file to $log_file  (for debugging) You can compare with the travis log !"

# Copy the MBTILES output to our working directory ----------------------------
DEST_MBTILES=$DEST_DIR/$MBTILES_FILE
cp ./data/$MBTILES_FILE ../data
cp ./data/$MBTILES_FILE $DEST_MBTILES
echo
echo "MBTILES output file is now available in $DEST_MBTILES"

rm -rf data/
