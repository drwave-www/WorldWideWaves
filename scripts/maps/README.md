# Map Generation Pipeline

**Purpose**: Complete offline map generation system for 40+ cities using OpenStreetMap data and MapLibre.

## Overview

This pipeline downloads OpenStreetMap data, generates offline MBTiles, extracts city boundaries (GeoJSON), and creates Android Dynamic Feature Modules for on-demand map delivery.

## Pipeline Stages

```
1. Download OSM → 2. Generate MBTiles → 3. Extract GeoJSON → 4. Generate Images → 5. Create Modules
```

### Stage 1: Download OSM Data
**Script**: `10-download_osm.sh`

Downloads OpenStreetMap `.pbf` files for configured events/cities.

```bash
# Download all configured events
./10-download_osm.sh

# Download specific event
./10-download_osm.sh paris_france

# Force re-download
./10-download_osm.sh paris_france --force
```

**What it does**:
- Reads event configuration from `shared/src/commonMain/composeResources/files/events.json`
- Downloads OSM data using `openmaptiles-tools`
- Extracts bounding box (BBOX) for each event area
- Caches downloaded data in `data/` directory

### Stage 2: Generate MBTiles
**Script**: `20-generate_mbtiles.sh`

Generates offline map tiles in MBTiles format using OpenMapTiles + Docker.

```bash
# Generate tiles for all events
./20-generate_mbtiles.sh

# Generate for specific event
./20-generate_mbtiles.sh new_york_usa
```

**What it does**:
- Uses `openmaptiles/openmaptiles` Docker container
- Processes OSM data through PostgreSQL + PostGIS
- Generates vector tiles at multiple zoom levels
- Outputs `.mbtiles` files ready for MapLibre

**Requirements**:
- Docker & Docker Compose
- 8GB+ RAM (PostgreSQL processing)
- 512MB shared memory configured in docker-compose

### Stage 3: Extract GeoJSON Boundaries
**Script**: `30-retrieve-geojson.sh`

Extracts city administrative boundaries as GeoJSON polygons.

```bash
./30-retrieve-geojson.sh
```

**What it does**:
- Queries OpenStreetMap Nominatim API
- Retrieves administrative boundary polygons
- Saves as `.geojson` files for map overlays
- Used for event area visualization

### Stage 4: Generate Default Map Images
**Script**: `35-generate-default-map-images.sh`

Creates default map preview images for events.

```bash
./35-generate-default-map-images.sh
```

**Output**: PNG images used as map placeholders before tiles load.

### Stage 5: Create Android Modules
**Script**: `40-generate-modules.sh`

Generates Android Dynamic Feature Modules for on-demand map delivery.

```bash
./40-generate-modules.sh
```

**What it does**:
- Creates separate module for each city
- Packages MBTiles into Android assets
- Generates module manifest and build files
- Enables Play Store on-demand delivery

**Output**: Creates `maps/<city>/` modules in project root.

## Configuration

### Event Configuration File
**Location**: `shared/src/commonMain/composeResources/files/events.json`

```json
[
  {
    "id": "paris_france",
    "area": {
      "osmAdminids": ["7444"],
      "bbox": {
        "north": 48.9021,
        "south": 48.8155,
        "east": 2.4699,
        "west": 2.2241
      }
    }
  }
]
```

**Key fields**:
- `id`: Event/city identifier (used in filenames)
- `osmAdminids`: OpenStreetMap administrative boundary IDs
- `bbox`: Bounding box coordinates (optional, auto-calculated if missing)

### Library Functions
**Location**: `libs/lib.inc.sh`

Shared utilities used across all scripts:

```bash
# Read event configuration
conf <event_id> <property>
#  Example: conf paris_france area.osmAdminids

# Get admin boundary IDs (handles legacy format)
get_osmAdminids <event_id>

# Get bounding box
get_event_bbox <event_id>

# Check if event exists
exists <event_id>
```

## Dependencies

### Required Tools
- **Docker**: OpenMapTiles container runtime
- **Docker Compose**: Container orchestration
- **jq**: JSON parsing (auto-downloaded to `bin/jq`)
- **yq**: YAML processing (auto-downloaded to `bin/yq`)
- **wget**: Downloading dependencies
- **Node.js 16+**: Map processing scripts

### Optional Tools
- **ImageMagick**: Image generation (stage 4)
- **osmium**: OSM data manipulation

### Auto-installed
The scripts auto-download these tools to `./bin/`:
- `jq` (Linux/macOS)
- `yq` (Linux/macOS)
- `openmaptiles-tools` (pip install)

## Directory Structure

```
maps/
├── bin/                   # Auto-downloaded binaries (jq, yq)
├── data/                  # Downloaded OSM .pbf files
│   ├── paris_france.pbf
│   ├── paris_france.yaml  # OpenMapTiles config
│   └── .env-paris_france  # Environment variables
├── libs/                  # Shared library functions
│   ├── lib.inc.sh         # Common utilities
│   ├── get_bbox.dep.sh    # BBOX calculation
│   └── generate_map.dep.sh # Map generation logic
├── openmaptiles/          # Cloned OpenMapTiles repo
│   └── docker-compose.yml # Modified for 512MB shm
├── output/                # Generated .mbtiles files
│   └── paris_france.mbtiles
└── *.sh                   # Pipeline scripts (10-*.sh, 20-*.sh, etc.)
```

## Workflow Examples

### Add New City

1. **Add event to configuration**:
   ```bash
   # Edit shared/src/commonMain/composeResources/files/events.json
   {
     "id": "tokyo_japan",
     "area": {
       "osmAdminids": ["1543125"]
     }
   }
   ```

2. **Run full pipeline**:
   ```bash
   cd scripts/maps/
   ./10-download_osm.sh tokyo_japan
   ./20-generate_mbtiles.sh tokyo_japan
   ./30-retrieve-geojson.sh
   ./35-generate-default-map-images.sh
   ./40-generate-modules.sh
   ```

3. **Verify output**:
   ```bash
   ls output/tokyo_japan.mbtiles
   ls ../../maps/tokyo_japan/
   ```

### Update Existing City

```bash
# Force re-download and regenerate
./10-download_osm.sh paris_france --force
./20-generate_mbtiles.sh paris_france
```

### Clean Generated Data

```bash
./clean.sh                  # Remove all generated files
rm -rf data/ output/        # Manual cleanup
rm -rf openmaptiles/        # Force re-clone
```

## Performance & Optimization

### Parallel Processing
The scripts support processing multiple cities concurrently:

```bash
# Process 3 cities in parallel
./20-generate_mbtiles.sh paris &
./20-generate_mbtiles.sh london &
./20-generate_mbtiles.sh tokyo &
wait
```

### Disk Space Requirements
- **OSM .pbf files**: 50-500MB per city
- **PostgreSQL data**: 1-5GB during processing
- **Output .mbtiles**: 100-800MB per city
- **Total workspace**: ~10GB for 40 cities

### Memory Requirements
- **Minimum**: 8GB RAM
- **Recommended**: 16GB RAM
- **PostgreSQL**: 512MB shared memory (configured in docker-compose)

## Troubleshooting

### Docker Issues

**Problem**: `Cannot connect to the Docker daemon`
```bash
# Check Docker is running
docker ps
docker-compose --version
```

**Problem**: `PostgreSQL shared memory error`
```bash
# Increase shm_size in openmaptiles/docker-compose.yml
# (Script does this automatically)
shm_size: "512m"
```

### OSM Download Issues

**Problem**: `Download fails for large cities`
```bash
# Use --force to retry
./10-download_osm.sh large_city --force

# Check disk space
df -h
```

### MBTiles Generation Fails

**Problem**: `Out of memory during tile generation`
```bash
# Check Docker memory limits
docker stats

# Reduce zoom level in config if needed
# Edit data/<event>.yaml
```

### Missing Dependencies

**Problem**: `jq: command not found`
```bash
# Scripts auto-download to ./bin/
# Ensure wget is available
which wget

# Or install system-wide
# macOS: brew install jq yq
# Linux: apt-get install jq
```

## Integration with Project

### Gradle Integration
```bash
# From project root
./gradlew :maps:generateAllMaps
```

### iOS ODR Integration
Map data is synchronized to iOS On-Demand Resources:
```bash
../../scripts/sync-ios-odr-maps.gradle.kts
```

### Android Dynamic Features
Generated modules in `maps/<city>/` are auto-included in build:
```groovy
// settings.gradle.kts
include(":maps:paris_france")
include(":maps:new_york_usa")
// ... (auto-generated)
```

## Best Practices

1. **Test on small city first**: Try luxembourg or monaco before large cities
2. **Monitor disk space**: Pipeline can use 10GB+ during processing
3. **Use specific event IDs**: Avoid processing all 40 cities unless needed
4. **Cache OSM data**: Don't use `--force` unless OSM data changed
5. **Run in Docker**: OpenMapTiles requires Docker (no native support)
6. **Check logs**: Each script outputs to `logs/<script>.log`

## Advanced Configuration

### Custom Zoom Levels
Edit `data/<event>.yaml`:
```yaml
minzoom: 0
maxzoom: 14  # Lower = faster, larger tiles
```

### Custom Tile Layers
Modify `openmaptiles/openmaptiles.yaml` to include/exclude layers.

### Parallel Docker Instances
Run multiple `20-generate_mbtiles.sh` in parallel with different ports:
```bash
POSTGRES_PORT=5433 ./20-generate_mbtiles.sh city1 &
POSTGRES_PORT=5434 ./20-generate_mbtiles.sh city2 &
```

## Contributing

When adding new map scripts:
1. Follow the numbering convention (`<order>-<name>.sh`)
2. Source `libs/lib.inc.sh` for shared utilities
3. Support both single event and batch processing
4. Add comprehensive error handling
5. Output progress indicators for long operations
6. Document in this README

## See Also

- [Main scripts documentation](../README.md)
- [Android Dynamic Features](https://developer.android.com/guide/playcore/feature-delivery)
- [OpenMapTiles](https://openmaptiles.org/)
- [MapLibre](https://maplibre.org/)
