# Events configuration file
EVENTS_FILE=../../shared/src/commonMain/composeResources/files/events.json
echo "--> Using events file $EVENTS_FILE"

# Download jq for JSON decoding with bash
mkdir -p ./bin
[ ! -f ./bin/jq ] && wget -np -q https://github.com/stedolan/jq/releases/latest/download/jq-linux64 -O ./bin/jq && chmod +x ./bin/jq

# List of all configured events
EVENTS=$(./bin/jq -r --arg unknown "unknown" '.[] | select(.mapBbox != $unknown) | .id' $EVENTS_FILE)

# ----------

function conf() { # Read event configuration property value
                  # CALL: conf $event $prop
  ./bin/jq -r --arg event "$1" --arg prop "$2" '.[] | select(.id == $event) | .[$prop]' $EVENTS_FILE
}

# ----------

function tpl() { # Replace event configuration values in template file
                 # use placeholders in the form #prop#
                 # CALL: tpl $event $template_file $output_file
  TPL=$(mktemp)
  cp $2 $TPL

  mapOsmadminid=$(conf $1 mapOsmadminid)
  bbox_output=$(./libs/get_bbox.dep.sh $mapOsmadminid)

  bbox=$(echo "$bbox_output" | head -n 1 | cut -d ':' -f 2)
  center=$(echo "$bbox_output" | tail -n 1 | cut -d ':' -f 2)

  for prop in $(./bin/jq -r --arg event "$1" '.[] | select(.id == $event) | keys[]' $EVENTS_FILE); do
    TMP=$(mktemp)
    cat $TPL | sed \
      -e 's/#'$prop'#/'"$(conf $1 $prop | sed -e 's/\//\\\//g')"'/g' \
      -e "s/#mapCenter#/$center/g" \
      -e "s/#mapBbox#/$bbox/g" \
      > $TMP
    rm -f $TPL
    TPL=$TMP
  done
  mv $TPL $3
}
