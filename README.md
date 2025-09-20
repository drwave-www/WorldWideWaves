# WorldWideWaves

A Kotlin Multiplatform app for synchronized global wave events. Join people worldwide in coordinated "waves" through real-time mapping, location services, and choreographed animations.

## What is WorldWideWaves?

WorldWideWaves orchestrates human waves through cities globally. Users can:
- View upcoming wave events on interactive offline maps
- Join synchronized waves at specific times and locations
- Experience real-time choreographed animations and audio
- Connect with participants across 40+ major cities worldwide

## Quick Start

### Prerequisites
- **Android Studio** (latest stable)
- **JDK 11** or higher
- **Xcode 15+** (for iOS development)
- **Docker** (for map generation - optional)

### Build & Run

```bash
# Build entire project
./gradlew build

# Run Android debug
./gradlew :composeApp:assembleDebug

# Run tests
./gradlew test
```

### Development Commands
See [CLAUDE.md](./CLAUDE.md) for complete development guide.

## Project Structure

```
WorldWideWaves/
├── composeApp/          # Android app with Compose UI
├── shared/              # Kotlin Multiplatform business logic
├── iosApp/              # iOS SwiftUI application
├── maps/                # 40+ city offline map modules
├── scripts/             # Build tools and map generation
└── gradle/              # Gradle configuration
```

### Key Modules

| Module | Purpose | Technology |
|--------|---------|------------|
| **`composeApp/`** | Main Android application | Compose Multiplatform, Activities |
| **`shared/`** | Cross-platform business logic | KMP, Coroutines, Koin DI |
| **`iosApp/`** | iOS application entry point | SwiftUI, iOS frameworks |
| **`maps/`** | City-specific offline map data | Android Dynamic Features |
| **`scripts/`** | Development and build tools | Shell scripts, Node.js |

## Architecture Overview

**Flow**: SplashScreen → EventsList → EventDetails → FullMap → WaveChoreography

- **Event System**: Load global events with countdowns and location data
- **Offline Maps**: MapLibre-based offline maps for 40+ cities
- **Choreography**: Frame-based synchronized animations with audio
- **Location Services**: GPS tracking with simulation mode for testing
- **Cross-platform**: Shared business logic, platform-specific UI

## Key Features

### 🌍 Global Events
- Real-time event loading from remote sources
- Countdown timers for upcoming waves
- Location-based event filtering

### 🗺️ Offline Maps
- Self-contained city map modules (40+ cities)
- On-demand loading to minimize app size
- Custom map styles and wave radius visualization

### 🎭 Wave Choreography
- Synchronized animations across devices
- Audio coordination with visual effects
- Frame-based timing system

### 📱 Cross-Platform
- Shared Kotlin business logic
- Native UI: Android Compose + iOS SwiftUI
- Platform-specific services (location, audio, maps)

## Development

### Adding a New City
1. Generate map data with `scripts/maps/`
2. Create new dynamic feature module in `maps/android/`
3. Add city configuration to shared module
4. Test offline map loading

### iOS Development Status
Currently implementing iOS version. See:
- `IOS_ADAPTATION_PLAN.md` - Architecture roadmap
- `NEXT_STEPS_ORDER.md` - Implementation steps

### Testing
```bash
# Unit tests (shared logic)
./gradlew :shared:testDebugUnitTest

# Android tests
./gradlew :composeApp:testDebugUnitTest

# Skip common tests (if needed)
./gradlew :shared:testDebugUnitTest -PdisableCommonTest
```

## Contributing

1. Check existing issues or create new ones
2. Follow the project's architectural patterns
3. Add tests for new functionality
4. Update relevant documentation

## Technologies

- **Kotlin Multiplatform** - Shared business logic
- **Compose Multiplatform** - UI framework
- **MapLibre** - Offline mapping
- **Koin** - Dependency injection
- **Coroutines** - Async programming
- **DataStore/UserDefaults** - Settings persistence
- **Firebase** - Analytics and crash reporting

---

**License**: See [LICENSE](./LICENSE)  
**Documentation**: [CLAUDE.md](./CLAUDE.md) | [iOS Plan](./IOS_ADAPTATION_PLAN.md)
