WorldWideWaves is an ephemeral mobile app designed to orchestrate human waves through cities and
countries, culminating in a global wave. The project aims to transcend physical and cultural
boundaries,
fostering unity, community, and shared human experience by leveraging real-time coordination and
location-based services.

To get BBOX on specific city : http://polygons.openstreetmap.fr/
To get GEOjson data from OSM admin bounday : http://polygons.openstreetmap.fr/?id=71525
Where admin ID is get from OSM

## Project Structure

### `scripts/maps/libs/lib.inc.sh`

This script contains utility functions and configurations used by other scripts. It includes:

- **conf**: Reads event configuration property values.
- **tpl**: Replaces event configuration values in a template file.

### `scripts/maps/libs/generate_map.dep.sh`

This script sets up the environment and generates map tiles for a specified event. It performs the
following steps:

1. Checks for required Docker and Docker Compose versions.
2. Pulls or refreshes OpenMapTiles Docker images.
3. Initializes directories and cleans up old files.
4. Imports OpenStreetMap data and Wikidata.
5. Generates MBTiles files using PostGIS.

### `scripts/maps/10-download_osm.sh`

This script downloads OpenStreetMap (OSM) data for specified events and extracts the bounding box (
BBOX) for each event. It performs the following steps:

1. Downloads OSM data for the specified area.
2. Extracts the BBOX from the downloaded OSM data.
3. Generates environment and tileset definition files for each event.

### `scripts/maps/20-generate_mbtiles.sh`

This script generates MBTiles files from the downloaded OSM data for each event. It
calls `generate_map.dep.sh` for each event to perform the map generation process.
