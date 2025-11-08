# Polygons

Geographic boundary processing and validation tools for city definitions in WorldWideWaves.

## Purpose

Processes geographic data for city boundaries:

- **Administrative boundary extraction** from OpenStreetMap
- **Polygon validation and simplification**
- **GeoJSON generation** for city overlays  
- **Boundary visualization** and debugging

## Structure

```
polygons/
├── extract_boundaries.py   # Extract city boundaries from OSM
├── validate_polygons.py    # Validate and fix polygon data
├── simplify_geojson.py    # Reduce polygon complexity
├── visualize_bounds.py    # Generate boundary preview images
└── README.md              # This file
```

## Usage

### Extract City Boundaries

```bash
# Extract boundary for specific city from OSM relation ID
python extract_boundaries.py --relation-id 71525 --output paris_france.geojson

# Extract using city name (searches OSM)
python extract_boundaries.py --city "Paris, France" --country FR
```

### Validate Polygons

```bash
# Check polygon validity and fix common issues
python validate_polygons.py paris_france.geojson

# Batch validate all city boundaries
python validate_polygons.py data/*.geojson --fix-errors
```

### Simplify GeoJSON

```bash
# Reduce polygon complexity for mobile rendering
python simplify_geojson.py paris_france.geojson --tolerance 0.001

# Batch simplify for all cities
./simplify_all.sh data/ --output simplified/
```

### Visualize Boundaries

```bash
# Generate preview image of city boundary
python visualize_bounds.py paris_france.geojson --output previews/paris_preview.png

# Create comparison before/after simplification
python visualize_bounds.py original.geojson simplified.geojson --compare
```

## Data Sources

### OpenStreetMap Relations

Most city boundaries come from OSM administrative relations:

- **Level 8** - City/Municipality boundaries
- **Level 6** - County/District boundaries (for larger areas)
- **Level 4** - State/Province boundaries (for special cases)

### Finding OSM Relation IDs

```bash
# Using Overpass API
curl -G "https://overpass-api.de/api/interpreter" \
  --data-urlencode 'data=[out:json];relation["name"="Paris"]["admin_level"="8"];out;'

# Using Nominatim
curl "https://nominatim.openstreetmap.org/search?city=Paris&country=France&format=json&extratags=1"
```

## Data Processing Pipeline

### 1. Extraction

```python
# extract_boundaries.py workflow
def extract_city_boundary(relation_id):
    # Download OSM relation data
    osm_data = download_osm_relation(relation_id)
    
    # Convert to polygon geometry  
    polygon = osm_to_polygon(osm_data)
    
    # Create GeoJSON
    geojson = create_geojson(polygon, metadata)
    
    return geojson
```

### 2. Validation

```python
# Common polygon issues and fixes
def validate_polygon(geojson):
    # Check for self-intersections
    if has_self_intersection(polygon):
        polygon = fix_self_intersection(polygon)
    
    # Ensure correct winding order (counterclockwise)
    if is_clockwise(polygon):
        polygon = reverse_coordinates(polygon)
        
    # Remove duplicate points
    polygon = remove_duplicates(polygon)
    
    return polygon
```

### 3. Simplification  

```python
# Reduce polygon complexity for mobile performance
def simplify_polygon(geojson, tolerance=0.001):
    # Douglas-Peucker algorithm
    simplified = douglas_peucker(geojson.coordinates, tolerance)
    
    # Maintain topology (avoid creating holes or overlaps)
    simplified = preserve_topology(simplified)
    
    return simplified
```

## Configuration

### Simplification Settings

```python
# Balance between accuracy and performance
SIMPLIFICATION_SETTINGS = {
    'mobile_rendering': 0.001,    # High detail for city view
    'overview_map': 0.005,        # Medium detail for country view  
    'global_view': 0.01           # Low detail for world view
}
```

### Validation Rules

```python
VALIDATION_RULES = {
    'min_area_km2': 1.0,          # Minimum city area
    'max_points': 10000,          # Point limit for mobile
    'allow_holes': True,          # Interior boundaries (parks, etc.)
    'enforce_simple': True        # No self-intersections
}
```

## GeoJSON Format

### Standard Structure

```json
{
  "type": "FeatureCollection",
  "features": [{
    "type": "Feature",
    "properties": {
      "name": "Paris",
      "country": "France", 
      "osm_relation_id": 71525,
      "admin_level": 8,
      "area_km2": 105.4,
      "population": 2165423
    },
    "geometry": {
      "type": "Polygon",
      "coordinates": [[
        [2.2241, 48.8156],
        [2.4699, 48.8156], 
        [2.4699, 48.9022],
        [2.2241, 48.9022],
        [2.2241, 48.8156]
      ]]
    }
  }]
}
```

### Multi-Polygon Support

```json
{
  "geometry": {
    "type": "MultiPolygon",
    "coordinates": [
      [[[outer_ring_1]], [[hole_1]]],
      [[[outer_ring_2]], [[hole_2]]]
    ]
  }
}
```

## Integration with Maps

### Using in Android App

```kotlin
// Load city boundary in MapLibre
fun loadCityBoundary(mapView: MapView, cityName: String) {
    val geoJsonSource = GeoJsonSource(
        "city-boundary",
        "file:///android_asset/${cityName}.geojson"
    )
    mapView.style?.addSource(geoJsonSource)
    
    // Add boundary stroke layer
    val boundaryLayer = LineLayer("city-boundary-line", "city-boundary")
    boundaryLayer.setProperties(
        lineColor(Color.BLUE),
        lineWidth(2f),
        lineOpacity(0.8f)
    )
    mapView.style?.addLayer(boundaryLayer)
}
```

### iOS Integration

```swift
// Load boundary in MapLibre iOS
func loadCityBoundary(mapView: MLNMapView, cityName: String) {
    guard let url = Bundle.main.url(forResource: cityName, withExtension: "geojson") else { return }
    
    let source = MLNShapeSource(identifier: "city-boundary", url: url, options: nil)
    mapView.style?.addSource(source)
    
    let layer = MLNLineStyleLayer(identifier: "city-boundary-line", source: source)
    layer.lineColor = NSExpression(forConstantValue: UIColor.blue)
    layer.lineWidth = NSExpression(forConstantValue: 2.0)
    mapView.style?.addLayer(layer)
}
```

## Quality Control

### Automated Checks

```bash
# Run full validation suite
./validate_all_cities.sh

# Checks performed:
# - Polygon validity (no self-intersections)
# - Coordinate bounds (reasonable lat/lng values)
# - File size limits (< 1MB per city)
# - GeoJSON schema compliance
```

### Visual Inspection

```python
# Generate boundary overlays for manual review
def create_inspection_map(geojson_file):
    # Load boundary on satellite imagery
    # Highlight potential issues (gaps, overlaps)
    # Export as PNG for review
    pass
```

## Tools and Dependencies

### Required Packages

```bash
pip install shapely geojson requests fiona geopandas matplotlib
```

### External Tools

- **GDAL/OGR** - Geospatial data processing
- **PostGIS** - Spatial database operations (optional)
- **QGIS** - Manual boundary editing (when needed)

### Verification

```bash
# Test installation
python -c "import shapely, geojson, geopandas; print('All packages available')"

# Check GDAL
ogrinfo --version
```

## Troubleshooting

### Common Issues

1. **Invalid polygons after extraction**

   ```bash
   # Use buffer(0) to fix topology
   python -c "
   from shapely.geometry import shape
   from shapely.ops import unary_union
   polygon = shape(geojson['geometry']).buffer(0)
   "
   ```

2. **File too large for mobile**

   ```bash
   # Increase simplification tolerance
   python simplify_geojson.py city.geojson --tolerance 0.005
   ```

3. **Missing OSM relation**

   ```bash
   # Search for alternative boundary sources
   python find_alternative_boundaries.py --city "City Name"
   ```

### Debug Mode

```bash
# Verbose processing with intermediate files
DEBUG=1 python extract_boundaries.py --relation-id 71525 --keep-temp
```

## Adding New Cities

### Workflow

1. **Find OSM relation ID** for city boundary
2. **Extract boundary** using relation ID
3. **Validate polygon** for errors
4. **Simplify if needed** for mobile performance  
5. **Test in app** to ensure proper rendering
6. **Add to city registry** in shared module

### Documentation

```bash
# Document new city addition
echo "paris_france,71525,Paris,France,105.4,2165423" >> cities_registry.csv
```

### Integration  

```bash
# Copy to app resources
cp paris_france.geojson ../maps/android/paris_france/src/main/assets/
cp paris_france.geojson ../shared/src/commonMain/composeResources/files/boundaries/
```
