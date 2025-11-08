# Maps Module

Dynamic Feature modules containing offline map data for 40+ cities worldwide. Each city is packaged as an Android Dynamic Feature to minimize app size and enable on-demand downloads.

## Structure

```
maps/
├── android/                     # Android Dynamic Feature modules
│   ├── paris_france/           # Example city module
│   │   ├── build.gradle.kts    # Dynamic feature configuration
│   │   └── src/main/
│   │       ├── AndroidManifest.xml    # Feature manifest
│   │       ├── assets/
│   │       │   ├── paris_france.mbtiles    # Offline map tiles
│   │       │   └── paris_france.geojson    # City boundary
│   │       └── res/values/
│   │           └── strings.xml             # City display names
│   ├── london_england/         # Another city...
│   ├── tokyo_japan/
│   └── ... (40+ cities)
```

## How It Works

### Dynamic Features

Each city is an Android Dynamic Feature module that:

- Contains offline map data for a specific city
- Gets downloaded on-demand when user requests that city's map
- Can be uninstalled to free storage space
- Reduces initial app download size

### Map Data Components

Each city module contains:

- **`.mbtiles`** - Offline map tiles (generated from OpenStreetMap)
- **`.geojson`** - City administrative boundaries
- **`strings.xml`** - Localized city name
- **Manifest** - Feature metadata and dependencies

## Available Cities

Current cities (40+):

- **Europe**: Paris, London, Berlin, Madrid, Rome, Moscow
- **North America**: New York, Los Angeles, Chicago, Toronto, Vancouver
- **Asia**: Tokyo, Beijing, Shanghai, Mumbai, Delhi, Bangkok
- **South America**: São Paulo, Buenos Aires, Bogotá, Lima, Santiago
- **Africa**: Cairo, Lagos, Johannesburg, Nairobi, Kinshasa
- **Middle East**: Dubai, Tehran, Istanbul
- **Oceania**: Sydney, Melbourne

## Adding a New City

### 1. Generate Map Data

```bash
# Navigate to scripts
cd scripts/maps/

# Add city to events configuration
# Edit data/events.properties
echo "new_city_country.bbox=minlon,minlat,maxlon,maxlat" >> data/events.properties

# Generate map tiles
./10-download_osm.sh new_city_country
./20-generate_mbtiles.sh new_city_country
```

### 2. Create Dynamic Feature Module

```bash
# Create city module directory
mkdir maps/android/new_city_country
cd maps/android/new_city_country
```

### 3. Add Module Configuration

Create `build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.android.dynamic.feature)
    alias(libs.plugins.jetbrains.kotlin.android)
}

android {
    namespace = "com.worldwidewaves.maps.new_city_country"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlin {
        jvmToolchain(11)
    }
}

dependencies {
    implementation(project(":composeApp"))
}
```

### 4. Create Module Structure

```bash
# Create directory structure
mkdir -p src/main/assets
mkdir -p src/main/res/values

# Copy generated map data
cp ../../scripts/maps/tmp/new_city_country.mbtiles src/main/assets/
cp ../../scripts/maps/tmp/new_city_country.geojson src/main/assets/
```

### 5. Add Manifest

Create `src/main/AndroidManifest.xml`:

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <dist:module
        xmlns:dist="http://schemas.android.com/apk/distribution"
        dist:instant="false"
        dist:title="@string/new_city_country_title">
        <dist:delivery>
            <dist:on-demand />
        </dist:delivery>
        <dist:fusing dist:include="true" />
    </dist:module>
</manifest>
```

### 6. Add Strings

Create `src/main/res/values/strings.xml`:

```xml
<resources>
    <string name="new_city_country_title">New City, Country</string>
</resources>
```

### 7. Register Module

Add to root `settings.gradle.kts`:

```kotlin
include(":maps:android:new_city_country")
```

Add to `composeApp/build.gradle.kts` dynamic features:

```kotlin
dynamicFeatures += setOf(
    // ... existing cities ...
    ":maps:android:new_city_country"
)
```

### 8. Update Shared Module

Add city configuration to `shared/src/commonMain/kotlin/.../CityRegistry.kt` (or equivalent).

## Usage in Code

### Loading Map Data

```kotlin
// Check if city module is available
if (isModuleAvailable("paris_france")) {
    // Load map tiles
    val mapView = MapLibreMap()
    mapView.setOfflineSource("file:///android_asset/paris_france.mbtiles")
    
    // Load city boundaries
    val geoJson = loadAssetString("paris_france.geojson")
}
```

### Dynamic Feature Management

```kotlin
// Install city on-demand
splitInstallManager.startInstall(
    SplitInstallRequest.newBuilder()
        .addModule("paris_france")
        .build()
)

// Check installation status
splitInstallManager.installedModules.contains("paris_france")
```

## Map Data Sources

- **Tiles**: OpenStreetMap data processed through OpenMapTiles
- **Boundaries**: Administrative boundaries from OSM
- **Style**: Custom MapLibre style optimized for wave visualization
- **Zoom levels**: 0-14 (city-wide to street level)

## Development Notes

### File Size Considerations

- Each city module: ~5-50MB depending on city size
- Balance between detail and download size
- Use appropriate zoom level limits

### Testing New Cities

```bash
# Test map generation
./scripts/maps/test_city.sh new_city_country

# Verify tiles are valid
./gradlew :maps:android:new_city_country:assembleDebug
```

### iOS Equivalent

For iOS, use **App Store On-Demand Resources (ODR)** instead of Dynamic Features:

- Create ODR tags with same city names
- Package same `.mbtiles` and `.geojson` files
- Use `NSBundleResourceRequest` for downloads

## Troubleshooting

### Common Issues

1. **Module not found**: Check `settings.gradle.kts` includes the module
2. **Asset not loading**: Verify file naming matches module name exactly
3. **Large download**: Consider reducing zoom levels or bbox size
4. **Build fails**: Ensure all cities listed in `dynamicFeatures` exist

### Map Data Issues

1. **Empty tiles**: Check bbox coordinates are valid
2. **Missing boundaries**: Verify GeoJSON is properly formatted
3. **Style not loading**: Ensure MapLibre style references correct sources
