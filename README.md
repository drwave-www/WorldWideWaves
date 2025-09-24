# WorldWideWaves

[![Code Quality & Standards](https://github.com/mglcel/WorldWideWaves/actions/workflows/03-code-quality.yml/badge.svg)](https://github.com/mglcel/WorldWideWaves/actions/workflows/03-code-quality.yml)
[![Android Build](https://github.com/mglcel/WorldWideWaves/actions/workflows/01-build-android.yml/badge.svg)](https://github.com/mglcel/WorldWideWaves/actions/workflows/01-build-android.yml)
[![iOS Build](https://github.com/mglcel/WorldWideWaves/actions/workflows/02-build-ios.yml/badge.svg)](https://github.com/mglcel/WorldWideWaves/actions/workflows/02-build-ios.yml)
[![Android UI Tests](https://github.com/mglcel/WorldWideWaves/actions/workflows/05-ui-tests-android.yml/badge.svg)](https://github.com/mglcel/WorldWideWaves/actions/workflows/05-ui-tests-android.yml)
[![End-to-End Tests](https://github.com/mglcel/WorldWideWaves/actions/workflows/06-e2e-tests.yml/badge.svg)](https://github.com/mglcel/WorldWideWaves/actions/workflows/06-e2e-tests.yml)

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
- **Crowd Sound Simulation**: Test real audio playback for crowd choreography

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

# Sound Choreography Tests
./gradlew crowdSoundSimulation  # Mathematical crowd simulation
ANDROID_SERIAL=emulator-5556 ./gradlew :composeApp:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.worldwidewaves.compose.choreographies.RealAudioCrowdSimulationTest
```

## Sound Choreography Simulation

WorldWideWaves includes comprehensive sound choreography simulation to test how music sounds when played by large crowds.

### 🎵 Features

**Mathematical Simulation (`CrowdSoundChoreographySimulationTest`):**
- Simulates 50 simultaneous people playing MIDI notes
- Each person plays every 100ms along the full duration of the MIDI file
- Analyzes pitch distribution and timing to ensure song recognizability
- Validates that overlapping notes create harmonic richness without losing melody

**Real Audio Testing (`RealAudioCrowdSimulationTest`):**
- Plays actual sound through Android device speakers
- Tests single notes, MIDI sequences, and full crowd simulation
- Uses `AndroidSoundPlayer` with different waveforms (SINE, SQUARE, SAWTOOTH)
- Simulates 5-person crowd with 500ms intervals for audible demonstration

**Interactive Audio Demo (`AudioTestActivity`):**
- Android Activity with Compose UI for real-time audio testing
- Three test modes: single note, MIDI sequence, and crowd simulation
- Loads real MIDI files or creates demo tracks as fallback
- Real-time progress indicators and audio warnings

### 🔧 Usage

Run the mathematical simulation:
```bash
./gradlew crowdSoundSimulation
```

Test real audio on Android device:
```bash
# Make sure Android emulator/device has audio enabled
ANDROID_SERIAL=emulator-5556 ./gradlew :composeApp:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.worldwidewaves.compose.choreographies.RealAudioCrowdSimulationTest
```

Launch interactive audio demo:
```bash
# Install and launch AudioTestActivity
./gradlew :composeApp:assembleDebug
adb install -r composeApp/build/outputs/apk/debug/composeApp-debug.apk
adb shell am start -n com.worldwidewaves/.debug.AudioTestActivity
```

### 📊 Technical Implementation

- **MIDI File Parsing**: Real MIDI file loading with `MidiParser.parseMidiFile()`
- **Wave Progression**: Time-based progression through musical score
- **Crowd Modeling**: Mathematical simulation of 50 people with timing variations
- **Real Audio Output**: Android `AudioTrack` integration for speaker playback
- **Multiple Waveforms**: Each simulated person uses different waveforms for variety

The simulation proves that when 50 people play the same MIDI file with slight timing variations (every 100ms), the song remains recognizable and creates rich, layered sound - perfect for coordinated crowd events.

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
