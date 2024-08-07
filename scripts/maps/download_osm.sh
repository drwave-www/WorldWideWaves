#!/bin/bash
cd "$(dirname "$0")"

#set -x

mkdir -p ./bin
mkdir -p ./data

wget -np -q https://github.com/mikefarah/yq/releases/latest/download/yq_linux_386 -O ./bin/yq && chmod +x ./bin/yq

EVENTS_FILE=../events.yaml
EVENTS=$(cat $EVENTS_FILE | ./bin/yq '.events[].event.id')

function conf() {
  cat $EVENTS_FILE | ./bin/yq '.events[] | select(.event.id == "'$1'") | .event.'$2
}

function tpl() {
  TPL=$(mktemp)
  cp $2 $TPL
  for prop in $(cat $EVENTS_FILE | ./bin/yq '.events[] | select(.event.id == "'$event'") | .event' | sed -e 's/:.*//'); do 
    TMP=$(mktemp)
    cat $TPL | sed -e 's/#'$prop'#/'"$(conf $1 $prop | sed -e 's/\//\\\//g')"'/g' > $TMP
    rm -f $TPL
    TPL=$TMP
  done
  mv $TPL $3
}


for event in $EVENTS; do
  echo "==> EVENT $event"
  BBOX=$(conf $event bbox)
  AREA=$(conf $event osmarea)
  SPBF=data/osm-$(echo $AREA | sed -e 's/\//_/g').osm.pbf
  DPBF=data/www-${event}.osm.pbf

  echo "-- Download area $AREA from OSM.."
  [ ! -f $SPBF ] && ./openmaptiles-tools/bin/download-osm $AREA -o $SPBF

  echo "-- Extract bbox $BBOX from area $AREA.."
  [ ! -f $DPBF ] && osmconvert $SPBF -b=$BBOX -o=$DPBF

  echo "-- Generates OpenMapTiles environment for event $event"
  tpl $event .env-template data/.env-${event}

  echo "-- Generates OpenMapTiles tileset definition for event $event"
  tpl $event template-omt.yaml data/${event}.yaml

done
