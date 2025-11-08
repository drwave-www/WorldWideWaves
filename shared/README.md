# Shared Module

The heart of WorldWideWaves - Kotlin Multiplatform module containing business logic, data models, and resources shared between Android and iOS platforms.

## Architecture

```
shared/
├── src/
│   ├── commonMain/          # Cross-platform code
│   │   ├── kotlin/
│   │   │   └── com/worldwidewaves/shared/
│   │   │       ├── events/           # Event management system
│   │   │       ├── choreographies/   # Wave animation coordination
│   │   │       ├── map/             # Geographic utilities
│   │   │       ├── sound/           # Audio coordination
│   │   │       ├── data/            # Data models and serialization
│   │   │       ├── utils/           # Common utilities
│   │   │       ├── format/          # Data formatting helpers
│   │   │       └── di/              # Dependency injection
│   │   └── composeResources/        # Shared assets and strings
│   ├── androidMain/         # Android-specific implementations
│   ├── iosMain/             # iOS-specific implementations
│   ├── commonTest/          # Cross-platform tests
│   └── androidUnitTest/     # Android-specific tests
└── build.gradle.kts         # Module configuration
```

## Core Packages

### 📅 Events (`/events`)

Manages global wave events and their lifecycle.

**Key Classes:**

- `WWWEvents` - Central event repository (singleton-like)
- `WWWEvent` - Core event data model (implements IWWWEvent)
- `WWWEventWave` - Wave-specific event data
- `WWWEventMap` - Geographic event information
- `WWWEventObserver` - Event state monitoring and updates

**Usage:**

```kotlin
// Load events with callbacks
WWWEvents.loadEvents(
    onLoaded = { println("Events loaded successfully") },
    onLoadingError = { error -> println("Failed to load: $error") }
)

// Access events via StateFlow
val eventsFlow: StateFlow<List<IWWWEvent>> = WWWEvents.flow()

// Or get current list directly
val currentEvents = WWWEvents.list()

// Filter events by location
val nearbyEvents = currentEvents.filter { 
    it.location.distanceTo(userLocation) < maxDistance 
}

// Get specific event by ID
val event = WWWEvents.getEventById("paris_2025_09_15")
```

### 🎭 Choreographies (`/choreographies`)

Frame-based animation system for synchronized waves.

**Key Classes:**

- `ChoreographyManager` - Central animation coordinator
- `ChoreographyFrame` - Individual animation frame
- `ChoreographySequence` - Sequence of frames over time

**Usage:**

```kotlin
// Get current warming sequence during event buildup
val warmingSequence = choreographyManager.getCurrentWarmingSequence(event.startTime)

// Get waiting sequence before wave hits
val waitingSequence = choreographyManager.getWaitingSequence()

// Get hit sequence when wave occurs
val hitSequence = choreographyManager.getHitSequence()

// Each sequence provides displayable frames
warmingSequence?.let { sequence ->
    val currentFrame = sequence.getCurrentFrame(clock.now())
    updateWaveAnimation(currentFrame)
}
```

### 🗺️ Map (`/map`)

Geographic calculations and map utilities.

**Key Classes:**

- `WWWLocationProvider` - Abstract location service
- `MapCoordinates` - Coordinate utilities
- `MapBounds` - Bounding box calculations

**Usage:**

```kotlin
// Calculate distance between points
val distance = coordinates1.distanceTo(coordinates2)

// Check if point is within bounds
val isInCity = cityBounds.contains(userLocation)

// Get current location
locationProvider.getCurrentLocation { location ->
    // Handle location update
}
```

### 🔊 Sound (`/sound`)

Audio coordination for wave experiences.

**Key Classes:**

- `SoundChoreographyManager` - Audio-visual synchronization
- `WWWSoundPlayer` - Cross-platform audio interface

### 📊 Data (`/data`)

Shared data models and serialization.

**Key Classes:**

- Core data structures used across platforms
- JSON serialization configurations
- Network response models

### 🔧 Utils (`/utils`)

Common utilities and helpers.

**Key Classes:**

- `Helpers` - General utility functions
- `ImageResolver` - Cross-platform image loading
- `ByteArrayReader` - Data parsing utilities

### 🏗️ DI (`/di`)

Dependency injection setup for shared components.

**Modules:**

- `CommonModule` - Core shared services
- `DatastoreModule` - Settings and preferences
- `HelpersModule` - Utility dependencies

## Platform-Specific Code

### Android (`androidMain`)

```kotlin
// Example: Android-specific location provider
actual class AndroidLocationProvider : WWWLocationProvider {
    actual override suspend fun getCurrentLocation(): Location {
        // Use Android LocationManager
    }
}
```

### iOS (`iosMain`)

```swift
// iOS consumes shared code through generated framework
import Shared

let eventObserver = WWWEventObserver()
eventObserver.loadEvents { events in
    // Handle events in SwiftUI
}
```

## Shared Resources (`composeResources`)

### Structure

```
composeResources/
├── drawable/                # Images and icons
│   ├── background.webp
│   ├── waves-icon.webp
│   └── ...
├── files/                   # Data files
│   └── style/              # MapLibre style definitions
└── values/                 # Strings and configurations
```

### Usage

```kotlin
// Access shared resources
val logo = Res.drawable.www_logo_transparent
val mapStyle = Res.files.style_map_style_json

// In Compose
Image(
    painter = painterResource(logo),
    contentDescription = "WWW Logo"
)
```

## Configuration

### Build Setup (`build.gradle.kts`)

```kotlin
kotlin {
    // Target platforms
    androidTarget()
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    
    // iOS Framework
    iosTarget.binaries.framework {
        baseName = "Shared"
        isStatic = true
    }
}
```

### Key Dependencies

- **kotlinx.serialization** - JSON handling
- **kotlinx.coroutines** - Async programming
- **kotlinx.datetime** - Date/time operations
- **koin** - Dependency injection
- **compose.resources** - Shared assets
- **datastore** - Settings persistence
- **napier** - Logging

## Development Patterns

### Expect/Actual Pattern

```kotlin
// Common interface
expect class PlatformLocationProvider : WWWLocationProvider

// Android implementation
actual class PlatformLocationProvider : WWWLocationProvider {
    // Android-specific code
}

// iOS implementation  
actual class PlatformLocationProvider : WWWLocationProvider {
    // iOS-specific code
}
```

### Resource Access

```kotlin
// Load shared strings
val welcomeText = getString(Res.string.welcome_message)

// Load shared images
val waveIcon = imageResource(Res.drawable.waves_icon)

// Load data files
val styleJson = readFileAsString(Res.files.map_style)
```

## Testing

### Common Tests (`commonTest`)

```kotlin
class EventTests {
    @Test
    fun testEventCountdown() {
        val event = WWWEvent(startTime = Clock.System.now().plus(1.hours))
        assertTrue(event.isUpcoming())
    }
}
```

### Android Tests (`androidUnitTest`)

```kotlin
class AndroidLocationTests {
    @Test
    fun testLocationPermissions() {
        // Android-specific location testing
    }
}
```

### Running Tests

```bash
# All shared tests
./gradlew :shared:testDebugUnitTest

# Skip common tests if needed
./gradlew :shared:testDebugUnitTest -PdisableCommonTest

# Android-specific tests only
./gradlew :shared:testDebugUnitTest -PdisableCommonTest
```

## Adding New Features

### 1. Core Logic (commonMain)

```kotlin
// Add to appropriate package
class NewFeature {
    fun processData(): Result<Data> {
        // Cross-platform business logic
    }
}
```

### 2. Platform Interfaces (expect/actual)

```kotlin
// commonMain - interface
expect class PlatformSpecificService {
    fun performAction(): Boolean
}

// androidMain - implementation
actual class PlatformSpecificService {
    actual fun performAction(): Boolean {
        // Android implementation
    }
}

// iosMain - implementation
actual class PlatformSpecificService {
    actual fun performAction(): Boolean {
        // iOS implementation
    }
}
```

### 3. Add Dependencies

Update `build.gradle.kts` commonMain dependencies:

```kotlin
commonMain.dependencies {
    implementation("new.library:artifact:version")
}
```

### 4. Add Tests

Create tests in `commonTest/` for business logic and platform-specific tests as needed.

## iOS Integration

### Framework Generation

The module generates an iOS framework (`Shared.framework`) that iOS code can import:

```swift
import Shared

// Use shared classes
let eventObserver = WWWEventObserver()
let choreographyManager = ChoreographyManager()
```

### Resource Access in iOS

Shared resources are accessible through generated iOS bundle:

```swift
// Access shared images
let image = SharedRes.images().waves_icon.toUIImage()

// Access shared strings  
let text = SharedRes.strings().welcome_message.localized()
```

## Troubleshooting

### Common Issues

1. **Build failures**: Check all platforms have required dependencies
2. **Resource not found**: Verify resource is in correct composeResources folder
3. **iOS framework issues**: Clean and rebuild shared module
4. **expect/actual mismatch**: Ensure all expect declarations have actual implementations

### Debug Tips

```bash
# Clean shared module
./gradlew :shared:clean

# Rebuild iOS framework
./gradlew :shared:linkDebugFrameworkIosArm64

# Check all tests pass
./gradlew :shared:check
```
