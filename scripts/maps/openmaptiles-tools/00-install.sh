python3 -m pip install -r requirements.txt
PYTHONPATH=$PWD python3 bin/generate-imposm3 ../openmaptiles/openmaptiles.yaml
apt install osmctools
