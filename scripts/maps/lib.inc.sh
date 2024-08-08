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

# Events configuration file
EVENTS_FILE=../events.yaml

# List of all comfigured events
EVENTS=$(cat $EVENTS_FILE | ./bin/yq '.events[].event.id')

# Download yq for YAML decoding with bash
mkdir -p ./bin
[ ! -f ./bin/yq ] && wget -np -q https://github.com/mikefarah/yq/releases/latest/download/yq_linux_386 -O ./bin/yq && chmod +x ./bin/yq

# ----------

function conf() { # Read event configuration property value
                  # CALL: conf $event $prop
  cat $EVENTS_FILE | ./bin/yq '.events[] | select(.event.id == "'$1'") | .event.'$2
}

# ----------

function tpl() { # Replace event configuration values in template file
                 # use placeholders in the form #prop#
                 # CALL: tlp $event $template_file $output_file
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
