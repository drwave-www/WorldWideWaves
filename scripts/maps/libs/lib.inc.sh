# Events configuration file
EVENTS_FILE=../../shared/src/commonMain/composeResources/files/events.json

# List of all configured events
EVENTS=$(jq -r '.[].id' $EVENTS_FILE)

# Download jq for JSON decoding with bash
mkdir -p ./bin
[ ! -f ./bin/jq ] && wget -np -q https://github.com/stedolan/jq/releases/latest/download/jq-linux64 -O ./bin/jq && chmod +x ./bin/jq

# ----------

function conf() { # Read event configuration property value
                  # CALL: conf $event $prop
  jq -r --arg event "$1" --arg prop "$2" '.[] | select(.id == $event) | .[$prop]' $EVENTS_FILE
}

# ----------

function tpl() { # Replace event configuration values in template file
                 # use placeholders in the form #prop#
                 # CALL: tpl $event $template_file $output_file
  TPL=$(mktemp)
  cp $2 $TPL
  for prop in $(jq -r --arg event "$1" '.[] | select(.id == $event) | keys[]' $EVENTS_FILE); do
    TMP=$(mktemp)
    cat $TPL | sed -e 's/#'$prop'#/'"$(conf $1 $prop | sed -e 's/\//\\\//g')"'/g' > $TMP
    rm -f $TPL
    TPL=$TMP
  done
  mv $TPL $3
}