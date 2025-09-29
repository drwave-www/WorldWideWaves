# iOS MapLibre Integration

This directory contains the iOS MapLibre integration for WorldWideWaves.

## Architecture

The iOS map implementation uses a **pure SwiftUI + Kotlin business logic** approach (per CLAUDE.md to avoid Compose/iOS crashes):

- **MapLibreViewWrapper.swift**: Swift wrapper around MapLibre Native SDK (`MLNMapView`) - @objc compatible
- **EventMapView.swift**: SwiftUI `UIViewRepresentable` for displaying interactive maps
- **MapLibreWrapperProtocol.h**: Objective-C protocol defining the wrapper interface
- **IOSEventMap.kt**: Kotlin status/fallback UI with download management (shows cards, not interactive map)
- **IOSMapLibreAdapter.kt**: Scaffolded adapter (for future cinterop integration if needed)

## Two-Layer Architecture

### Layer 1: Swift UI (Interactive Map Display)
**For Production Use** - Use this in your iOS app:

```swift
import SwiftUI
import Shared

struct EventScreen: View {
    let event: IWWWEvent
    @State private var mapWrapper: MapLibreViewWrapper?

    var body: some View {
        EventMapView(
            styleURL: getStyleURL(for: event),
            initialLatitude: event.map.center.lat,
            initialLongitude: event.map.center.lng,
            initialZoom: 12.0,
            wrapper: $mapWrapper
        )
        .onAppear {
            // Initialize Kotlin business logic
            // Generate wave polygons via Kotlin
            // Call mapWrapper?.addWavePolygons(...) to display them
        }
    }
}
```

### Layer 2: Kotlin Status UI (Fallback/Debug)
**IOSEventMap** provides download management and status display but does NOT render an interactive map. It shows:
- Download progress indicator
- ODR status and errors
- GPS coordinates (text)
- Wave polygon count
- Download/retry/cancel buttons

This layer is useful for:
- Testing download logic without full map
- Status monitoring during development
- Fallback UI if map fails to load

## Why Not Kotlin/Native Cinterop?

Per CLAUDE.md guidelines, we avoid Compose UI on iOS due to lifecycle crashes. Instead:

1. **Swift UI Layer**: EventMapView provides the visual map component
2. **Kotlin Business Logic**: IOSEventMap handles ODR downloads, position updates, event data
3. **Communication**: Kotlin exposes state/data, Swift UI observes and renders

## Usage in SwiftUI

```swift
import SwiftUI
import Shared // Kotlin Multiplatform module

struct EventScreen: View {
    let event: IWWWEvent
    @State private var mapWrapper: MapLibreViewWrapper?

    var body: some View {
        VStack {
            // Map view
            EventMapView(
                styleURL: event.map.getStyleUri(),
                initialLatitude: event.map.center.lat,
                initialLongitude: event.map.center.lng,
                initialZoom: 12.0,
                wrapper: $mapWrapper
            )
            .edgesIgnoringSafeArea(.all)

            // Kotlin business logic can call wrapper methods:
            // mapWrapper?.addWavePolygons(polygons, clearExisting: true)
        }
        .onAppear {
            // Initialize Kotlin business logic if needed
        }
    }
}
```

## MapLibreViewWrapper API

### Map Setup
- `setMapView(_: MLNMapView)` - Bind wrapper to map view
- `setStyle(styleURL:completion:)` - Set map style

### Camera
- `getCameraCenterLatitude() -> Double`
- `getCameraCenterLongitude() -> Double`
- `getCameraZoom() -> Double`
- `moveCamera(latitude:longitude:zoom:)` - Instant camera move
- `animateCamera(latitude:longitude:zoom:callback:)` - Animated camera move

### Wave Polygons
- `addWavePolygons(_:clearExisting:)` - Add wave visualization
- `clearWavePolygons()` - Remove all waves

### Dimensions
- `getWidth() -> Double`
- `getHeight() -> Double`

## IOSEventMap (Kotlin)

The Kotlin `IOSEventMap` class provides:

- **ODR Integration**: Automatic map download via `PlatformMapManager`
- **Position Management**: GPS integration via `PositionManager`
- **Wave Polygon Generation**: Event-specific wave visualization
- **Status UI**: Download progress, errors, availability feedback

Currently displays a status card. To integrate with the actual map:

1. Swift app creates `EventMapView`
2. Swift passes `mapWrapper` reference to Kotlin (if needed)
3. Kotlin business logic generates wave polygons
4. Swift calls `mapWrapper.addWavePolygons()` with Kotlin-generated data

## Dependencies

- **MapLibre**: maplibre-gl-native-distribution 6.19.1+ (Swift Package Manager)
- **Module**: `import MapLibre`
- **Classes**: `MLN*` prefix (MLNMapView, MLNStyle, MLNMapViewDelegate, etc.)

## Testing

Build the Xcode project:
```bash
xcodebuild -project worldwidewaves.xcodeproj -scheme worldwidewaves -destination 'platform=iOS Simulator,name=iPhone 16'
```

## Notes

- ⚠️ **No Compose on iOS**: Per CLAUDE.md, avoid `ComposeUIViewController` (crashes)
- ✅ **Pure SwiftUI**: Use SwiftUI views calling Kotlin business logic functions
- ✅ **@objc Compatible**: All wrapper methods use @objc-compatible types (NSNumber, separate lat/lng methods)
