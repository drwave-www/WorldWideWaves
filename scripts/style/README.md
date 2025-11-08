# Style

Map style generation and customization tools for WorldWideWaves offline maps.

## Purpose

Creates and manages custom MapLibre styles:

- **Custom map styles** optimized for wave visualization
- **Theme variations** (light, dark, high contrast)
- **Style optimization** for mobile rendering
- **Dynamic styling** based on app state

## Structure

```
style/
├── templates/           # Base style templates
├── generate_style.py    # Main style generator
├── optimize_style.py    # Style optimization for mobile
├── theme_variants.py    # Generate theme variations
└── README.md           # This file
```

## Usage

### Generate Base Style

```bash
# Create main app style from template
python generate_style.py --template base --output www_style.json

# Generate with specific tile source
python generate_style.py --template base --tiles-url "mbtiles:///{city}.mbtiles"
```

### Create Theme Variants

```bash
# Generate light and dark variants
python theme_variants.py www_style.json

# Output: www_style_light.json, www_style_dark.json
```

### Optimize for Mobile

```bash
# Optimize style for mobile rendering performance
python optimize_style.py www_style.json --target mobile

# Remove unused layers, simplify expressions
```

## Style Features

### Wave-Optimized Design

- **Subtle base map** that doesn't compete with wave overlays
- **High contrast boundaries** for city limits
- **Muted colors** for roads and buildings
- **Clear water features** for coastal cities

### Performance Optimizations

- **Minimal layer count** for faster rendering
- **Optimized zoom ranges** for mobile viewports
- **Simplified expressions** to reduce GPU load
- **Efficient sprite usage**

### Accessibility

- **High contrast mode** for visibility issues
- **Colorblind-friendly palette**
- **Large text sizes** for readability
- **Clear visual hierarchy**

## Style Configuration

### Base Style Template

```json
{
  "version": 8,
  "name": "WorldWideWaves Base",
  "sources": {
    "offline": {
      "type": "vector",
      "url": "mbtiles://{city}.mbtiles"
    }
  },
  "layers": [
    {
      "id": "background",
      "type": "background",
      "paint": {
        "background-color": "#f8f8f8"
      }
    }
  ]
}
```

### Wave Overlay Layers

```json
{
  "id": "wave-radius",
  "type": "circle",
  "source": "wave-data",
  "paint": {
    "circle-radius": ["get", "radius"],
    "circle-color": "rgba(33, 150, 243, 0.3)",
    "circle-stroke-color": "#2196f3",
    "circle-stroke-width": 2
  }
}
```

### City Boundary Layer

```json
{
  "id": "city-boundary",
  "type": "line", 
  "source": "boundaries",
  "paint": {
    "line-color": "#1976d2",
    "line-width": 3,
    "line-opacity": 0.8
  }
}
```

## Theme Variants

### Light Theme

```json
{
  "background_color": "#ffffff",
  "water_color": "#a0c8f0", 
  "land_color": "#f8f8f8",
  "road_color": "#e0e0e0",
  "text_color": "#333333"
}
```

### Dark Theme  

```json
{
  "background_color": "#1a1a1a",
  "water_color": "#2c5aa0",
  "land_color": "#2d2d2d", 
  "road_color": "#404040",
  "text_color": "#ffffff"
}
```

### High Contrast Theme

```json
{
  "background_color": "#000000",
  "water_color": "#0066cc",
  "land_color": "#ffffff",
  "road_color": "#ffff00", 
  "text_color": "#ffffff"
}
```

## Style Generation

### Template System

```python
def generate_style(template_name, config):
    # Load base template
    template = load_template(template_name)
    
    # Apply configuration
    style = apply_config(template, config)
    
    # Optimize for target platform
    style = optimize_for_mobile(style)
    
    # Validate style schema
    validate_maplibre_style(style)
    
    return style
```

### Dynamic Configuration

```python
# Generate city-specific styles
for city in cities:
    config = {
        'city_name': city.name,
        'tile_source': f'mbtiles://{city.id}.mbtiles',
        'boundary_source': f'geojson://{city.id}.geojson',
        'primary_color': city.brand_color
    }
    
    style = generate_style('base', config)
    save_style(style, f'{city.id}_style.json')
```

## Integration with Maps

### Android MapLibre

```kotlin
// Load custom style in Android app
fun loadCustomStyle(mapView: MapView, cityName: String) {
    val styleUrl = "asset://styles/${cityName}_style.json"
    mapView.setStyle(Style.Builder().fromUri(styleUrl))
}
```

### iOS MapLibre

```swift
// Load custom style in iOS app
func loadCustomStyle(mapView: MLNMapView, cityName: String) {
    if let styleURL = Bundle.main.url(forResource: "\(cityName)_style", withExtension: "json") {
        mapView.styleURL = styleURL
    }
}
```

### Dynamic Theme Switching

```kotlin
// Switch between light/dark themes
fun switchTheme(mapView: MapView, isDark: Boolean) {
    val styleName = if (isDark) "www_style_dark" else "www_style_light"
    val styleUrl = "asset://styles/${styleName}.json"
    mapView.setStyle(Style.Builder().fromUri(styleUrl))
}
```

## Style Validation

### Schema Compliance

```python
# Validate against MapLibre style spec
def validate_style(style_json):
    schema = load_maplibre_schema()
    validator = jsonschema.Draft7Validator(schema)
    
    errors = list(validator.iter_errors(style_json))
    if errors:
        for error in errors:
            print(f"Validation error: {error.message}")
        return False
    return True
```

### Performance Testing

```python
# Test style performance metrics
def analyze_style_performance(style):
    metrics = {
        'layer_count': len(style['layers']),
        'expression_complexity': calculate_expression_complexity(style),
        'estimated_memory_usage': estimate_memory_usage(style),
        'render_time_estimate': estimate_render_time(style)
    }
    return metrics
```

## Optimization Techniques

### Layer Optimization

```python
# Remove unnecessary layers for mobile
def optimize_layers(style):
    # Remove layers not visible at mobile zoom levels
    mobile_layers = []
    for layer in style['layers']:
        if is_visible_on_mobile(layer):
            mobile_layers.append(optimize_layer(layer))
    
    style['layers'] = mobile_layers
    return style
```

### Expression Simplification

```python
# Simplify complex expressions
def simplify_expressions(layer):
    if 'paint' in layer:
        for property_name, expression in layer['paint'].items():
            if is_complex_expression(expression):
                layer['paint'][property_name] = simplify_expression(expression)
    return layer
```

### Resource Optimization

```python
# Optimize sprite and glyph usage
def optimize_resources(style):
    # Remove unused sprite images
    used_sprites = find_used_sprites(style)
    style['sprite'] = filter_sprite_images(style['sprite'], used_sprites)
    
    # Optimize glyph ranges
    used_glyphs = find_used_glyphs(style) 
    style['glyphs'] = optimize_glyph_ranges(style['glyphs'], used_glyphs)
    
    return style
```

## Build Integration

### Automatic Style Generation

```bash
# Generate all city styles
./generate_all_styles.sh

# Copy to app resources
cp generated_styles/*.json ../shared/src/commonMain/composeResources/files/styles/
```

### CI/CD Integration

```yaml
# GitHub Actions
- name: Generate Map Styles
  run: |
    cd scripts/style
    python generate_all_styles.py
    
- name: Validate Styles
  run: |
    cd scripts/style  
    python validate_all_styles.py
```

## Tools and Dependencies

### Required Packages

```bash
pip install jsonschema requests pillow maplibre-gl-native
```

### MapLibre Tools

```bash
# MapLibre style validation
npm install -g @maplibre/maplibre-gl-style-spec

# Validate style
gl-style-validate style.json
```

## Troubleshooting

### Common Issues

1. **Invalid style schema**

   ```bash
   # Validate against official schema
   python validate_style.py --schema maplibre-gl-style.json
   ```

2. **Missing resources**

   ```bash
   # Check for missing sprites/glyphs
   python check_resources.py style.json
   ```

3. **Poor mobile performance**

   ```bash
   # Analyze and optimize
   python analyze_performance.py style.json
   python optimize_style.py style.json --aggressive
   ```

### Debug Mode

```bash
# Generate style with debug information
DEBUG=1 python generate_style.py --template base --verbose
```

## Style Customization

### Brand Colors

```python
# Apply brand colors to style
BRAND_COLORS = {
    'primary': '#2196f3',      # Wave blue
    'secondary': '#ff9800',    # Accent orange
    'surface': '#f5f5f5',      # Light gray
    'on_surface': '#212121'    # Dark gray
}

def apply_branding(style, colors):
    # Update relevant layer colors
    for layer in style['layers']:
        if 'paint' in layer:
            update_layer_colors(layer, colors)
    return style
```

### City-Specific Customization

```python
# Customize style for specific cities
CITY_CUSTOMIZATIONS = {
    'paris_france': {
        'accent_color': '#c8aa6e',  # Gold for Paris
        'highlight_landmarks': True
    },
    'tokyo_japan': {
        'accent_color': '#e60012',  # Red for Tokyo
        'show_districts': True
    }
}
```
