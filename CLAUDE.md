# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Development Commands

### Build Commands
- `./gradlew build` - Build all modules (shared, composeApp, map modules)
- `./gradlew assembleDebug` - Build debug variant
- `./gradlew assembleRelease` - Build release variant with ProGuard optimization
- `./gradlew :composeApp:build` - Build main application
- `./gradlew :shared:build` - Build shared Kotlin Multiplatform module

### Testing Commands
- `./gradlew test` - Run unit tests for all modules
- `./gradlew :shared:testDebugUnitTest` - Run shared module tests
- `./gradlew :composeApp:testDebugUnitTest` - Run Android unit tests
- `./gradlew :shared:testDebugUnitTest -PdisableCommonTest` - Skip common tests (useful for Android-specific testing)

### Development Tools
- `./gradlew tasks --all` - List all available Gradle tasks
- `./gradlew androidDependencies` - Display Android dependencies
- `./gradlew signingReport` - Display signing configurations

## Project Architecture

### High-Level Structure
This is a Kotlin Multiplatform (KMP) project targeting Android and iOS with these main modules:

- **`/composeApp`** - Main Android application using Compose Multiplatform
- **`/shared`** - Common business logic, data models, and resources shared between platforms
- **`/iosApp`** - iOS application entry point (SwiftUI integration)
- **`/maps/android/[city_name]`** - Dynamic Feature modules for offline city maps (40+ cities)

### Key Technologies
- **Kotlin Multiplatform** with expect/actual pattern for platform-specific implementations
- **Compose Multiplatform** for shared UI components and resources
- **MapLibre** for offline mapping (Android: MapLibre-Android, iOS: MapLibre-iOS)
- **Koin** for dependency injection
- **Kotlinx.coroutines** for asynchronous programming
- **DataStore** for preferences (Android) / UserDefaults (iOS)
- **Firebase** for crash reporting and analytics

### Application Flow
1. **SplashActivity** → **MainActivity** (tab-based navigation)
2. **EventsListScreen** - Lists worldwide events with countdown timers
3. **EventActivity** - Event details with map preview and countdown
4. **EventFullMapActivity** - Full offline map view with wave radius visualization
5. **WaveActivity** - Real-time synchronized wave choreography with audio

### Shared Module (`/shared`)
Contains platform-agnostic code:
- **Event management** (`WWWEvents`) - Loading and parsing event data
- **Choreography system** (`ChoreographyManager`) - Frame-based animation sequences
- **Location services** - Geolocation and map coordinate utilities
- **Resources** - Images, fonts, map styles, localization files
- **Data models** - Event, Location, ChoreographyFrame structures

### Dynamic Feature Modules
Each city has its own Android Dynamic Feature module:
- Contains offline map tiles (`.mbtiles`), GeoJSON boundaries, and manifest
- Loaded on-demand to reduce app size
- iOS equivalent uses App Store On-Demand Resources (ODR)

### Platform-Specific Implementations
- **Android**: Traditional Activity-based architecture with Compose UI
- **iOS**: SwiftUI views consuming shared KMP business logic
- **Location**: AndroidWWWLocationProvider vs iOS CoreLocation integration
- **Map rendering**: MapLibre-Android vs MapLibre-iOS SDK
- **Audio playback**: Platform-specific sound choreography managers

### Key Components
- **TabManager** - Handles bottom navigation state persistence
- **ChoreographyManager** - Coordinates timed animations and audio
- **MapViewModel** - Manages map state and offline tile loading
- **EventsViewModel** - Handles event loading and refresh logic
- **WWWLocationProvider** - Abstract location service with simulation mode

### iOS Adaptation Status
Currently implementing iOS version following the roadmap in `IOS_ADAPTATION_PLAN.md` and `NEXT_STEPS_ORDER.md`. The shared module is iOS-ready with framework binaries, and iOS-specific SwiftUI views are being developed to consume the shared business logic.

### Testing Strategy
- **Shared module**: Common business logic tested with `kotlin.test`
- **Platform-specific**: Unit tests for ViewModels and platform services
- **Integration**: End-to-end testing of event loading and choreography playback
- **Map testing**: Offline tile loading and MapLibre integration validation