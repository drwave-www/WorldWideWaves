# Scripts

Development tools and automation scripts for WorldWideWaves. Contains utilities for map generation, build processes, image processing, and project maintenance.

## Overview

```
scripts/
├── maps/           # Map tile generation and processing
├── images/         # Image processing and generation
├── instagram/      # Social media content automation
├── licenses/       # License management and reporting
├── polygons/       # Geographic boundary processing
├── style/          # Map style generation
└── translate/      # Localization and translation tools
```

## Quick Start

### Verify Development Environment
Before using any scripts, verify your development environment is properly configured:

```bash
# Run setup verification script
./scripts/verify-setup.sh
```

This will check:
- Platform detection (macOS, Linux, Windows)
- Required tools (Java 17+, Node.js 16+, Git, ripgrep)
- Platform-specific tools (Xcode on macOS, KVM on Linux)
- Android SDK configuration
- Project configuration (local.properties, Firebase)
- Build system functionality

### Prerequisites
- **Docker & Docker Compose** (for map generation)
- **Node.js 16+** (for map processing)
- **OpenStreetMap tools** (osmosis, osm2pgsql)
- **ImageMagick** (for image processing)

### Common Workflows

#### Generate Maps for New City
```bash
cd scripts/maps/

# Generate offline map tiles
./10-download_osm.sh new_city
./20-generate_mbtiles.sh new_city
```

#### Process Images
```bash
cd scripts/images/
./generate_app_icons.sh        # Generate app icons
./optimize_images.sh           # Optimize all images
```

#### Update Licenses
```bash
cd scripts/licenses/
./generate_license_report.sh   # Generate dependency licenses
./check_compliance.sh          # Verify license compliance
```

## Module Details

### ✅ verify-setup.sh - **Environment Verification**
Comprehensive development environment verification script.

**Purpose:**
- Verify all required development tools are installed
- Check platform-specific requirements (Xcode, KVM, etc.)
- Validate Android SDK configuration
- Confirm project configuration files exist
- Test Gradle build system functionality

**Usage:**
```bash
./scripts/verify-setup.sh
```

**Checks Performed:**
- ✅ Platform detection (macOS, Linux, Windows Git Bash)
- ✅ Java JDK 17+ installation
- ✅ Node.js 16+ and npm
- ✅ Git and ripgrep
- ✅ Android SDK (ANDROID_HOME/ANDROID_SDK_ROOT)
- ✅ Platform tools (Xcode on macOS, KVM on Linux)
- ✅ Project files (local.properties, Firebase config)
- ✅ Gradle wrapper and build system
- ⚠️  Optional tools (Docker, gcloud, CocoaPods, SwiftLint)

**Exit Codes:**
- `0` - All required checks passed (warnings allowed)
- `1` - One or more critical checks failed

**Platform Compatibility:**
- macOS (tested on macOS 15.6+)
- Linux (Ubuntu, Debian, Fedora)
- Windows (Git Bash/MSYS2/Cygwin)

### 🗺️ [Maps](./maps/) - **Primary Tool**
Complete map generation pipeline for offline city maps using OpenStreetMap and Docker.

**📚 [Full Documentation](./maps/README.md)** - Comprehensive guide to the 5-stage pipeline

**Key Scripts:**
- `10-download_osm.sh` - Download OpenStreetMap `.pbf` data
- `20-generate_mbtiles.sh` - Generate offline map tiles (Docker + PostgreSQL)
- `30-retrieve-geojson.sh` - Extract city boundaries
- `35-generate-default-map-images.sh` - Generate map preview images
- `40-generate-modules.sh` - Create Android Dynamic Feature Modules

**Generates:**
- `.mbtiles` files for offline maps (100-800MB per city)
- `.geojson` city boundary polygons
- MapLibre-compatible vector tiles
- Android Dynamic Feature Modules for on-demand delivery

**Requirements**: Docker, Docker Compose, 8GB+ RAM

### 🖼️ [Images](./images/)
Image processing and asset generation.

**Purpose:**
- App icon generation in multiple sizes
- Image optimization for mobile
- Asset preparation for different platforms

### 📱 [Instagram](./instagram/)
Social media automation and content generation.

**Purpose:**
- Automated post generation
- Content scheduling
- Social media asset creation

### 📜 [Licenses](./licenses/)
License compliance and dependency management.

**Purpose:**
- Generate license reports
- Check dependency compliance
- Maintain legal documentation

### 🌍 [Polygons](./polygons/)
Geographic boundary processing and validation.

**Purpose:**
- Process city administrative boundaries
- Validate geographic data
- Generate boundary overlays

### 🎨 [Style](./style/)
Map style generation and customization.

**Purpose:**
- Generate custom MapLibre styles
- Theme map appearances
- Optimize styles for mobile rendering

### 🌐 [Translate](./translate/)
Localization and translation management.

**Purpose:**
- Manage app translations
- Automate localization workflows
- Validate translation completeness

## Development Integration

### Build Process Integration
```bash
# In main project build
./gradlew build                    # Builds app
scripts/maps/update_all.sh        # Updates map data
scripts/licenses/generate_report.sh # Updates licenses
```

### CI/CD Integration
```yaml
# GitHub Actions example
- name: Generate Maps
  run: |
    cd scripts/maps
    ./generate_all.sh

- name: Process Assets
  run: |
    cd scripts/images
    ./optimize_all.sh
```

## Adding New Tools

### Creating a New Script Module
```bash
# 1. Create module directory
mkdir scripts/new_tool/

# 2. Create main script
cat > scripts/new_tool/process.sh << 'EOF'
#!/bin/bash
# Tool description and purpose

set -e
cd "$(dirname "$0")"

echo "Processing with new tool..."
# Implementation here
EOF

# 3. Make executable
chmod +x scripts/new_tool/process.sh

# 4. Create README
cat > scripts/new_tool/README.md << 'EOF'
# New Tool

Description of what this tool does and how to use it.

## Usage
```bash
./process.sh [options]
```

## Configuration
- Setting 1: Description
- Setting 2: Description
EOF
```

### Integration Patterns
```bash
# Common script patterns used throughout
source "$(dirname "$0")/libs/common.sh"  # Shared utilities
check_dependencies                        # Verify prereqs
load_configuration                        # Load settings
process_with_logging                      # Execute with logs
cleanup_on_exit                          # Cleanup handler
```

## Configuration Management

### Environment Variables
```bash
# Common configuration
export WWW_DATA_DIR="./data"
export WWW_OUTPUT_DIR="./output"
export WWW_CACHE_DIR="./cache"
export WWW_LOG_LEVEL="INFO"
```

### Configuration Files
```bash
# Most tools use .properties files
data/config.properties        # Global configuration
data/events.properties       # Event-specific settings
data/cities.properties       # City definitions
```

## Logging and Monitoring

### Log Files
```bash
logs/
├── map_generation.log       # Map processing logs
├── image_processing.log     # Image tool logs
├── build_process.log        # Build integration logs
└── errors.log              # Consolidated errors
```

### Monitoring Commands
```bash
# Monitor map generation
tail -f logs/map_generation.log

# Check for errors across all tools
grep ERROR logs/*.log

# View recent activity
ls -lat logs/
```

## Troubleshooting

### Common Issues

1. **Docker not available**
   ```bash
   # Check Docker installation
   docker --version
   docker-compose --version
   ```

2. **Permissions errors**
   ```bash
   # Fix script permissions
   find scripts/ -name "*.sh" -exec chmod +x {} \;
   ```

3. **Missing dependencies**
   ```bash
   # Check requirements for each module
   scripts/maps/check_deps.sh
   scripts/images/check_deps.sh
   ```

4. **Disk space issues**
   ```bash
   # Clean up generated files
   scripts/cleanup.sh

   # Check disk usage
   du -sh scripts/*/tmp/
   ```

### Debug Mode
```bash
# Run any script in debug mode
DEBUG=1 ./script_name.sh

# Or set verbose output
set -x
./script_name.sh
```

## Performance Optimization

### Parallel Processing
Most scripts support parallel execution:
```bash
# Process multiple cities in parallel
./generate_maps.sh --parallel 4 city1 city2 city3 city4

# Optimize images concurrently
./optimize_images.sh --jobs 8
```

### Caching
```bash
# Use cache directories to speed up rebuilds
WWW_ENABLE_CACHE=true ./generate_maps.sh

# Clear cache when needed
./clear_cache.sh
```

## Contributing

### Adding New Scripts
1. Follow existing patterns and conventions
2. Include comprehensive error handling
3. Add logging and progress indicators
4. Create corresponding README.md
5. Test with various inputs
6. Update main scripts/README.md

### Best Practices
- Use `set -e` for error handling
- Include usage documentation in script headers
- Validate inputs before processing
- Provide progress feedback for long operations
- Clean up temporary files on exit