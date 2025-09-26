# ComposeApp Module

The main Android application module built with Jetpack Compose and Compose Multiplatform. Provides the native Android UI that consumes shared business logic from the `shared` module.

## Architecture

```
composeApp/
├── src/androidMain/
│   ├── kotlin/com/worldwidewaves/
│   │   ├── activities/              # Android Activities
│   │   │   ├── MainActivity.kt      # Tab-based main screen
│   │   │   ├── event/              # Event-related activities
│   │   │   │   ├── EventActivity.kt        # Event details
│   │   │   │   ├── EventFullMapActivity.kt # Full map view
│   │   │   │   └── WaveActivity.kt         # Wave choreography
│   │   │   └── utils/              # Activity utilities
│   │   ├── compose/                # Compose UI components
│   │   │   ├── tabs/               # Tab screens
│   │   │   ├── common/             # Reusable components
│   │   │   ├── choreographies/     # Animation components
│   │   │   └── map/                # Map components
│   │   ├── viewmodels/             # Android ViewModels
│   │   ├── theme/                  # App theming
│   │   ├── utils/                  # Android utilities
│   │   ├── map/                    # MapLibre integration
│   │   └── di/                     # Android DI modules
│   └── res/                        # Android resources
├── build.gradle.kts                # Module configuration
└── proguard-rules.pro             # ProGuard configuration
```

## Application Flow

### 1. SplashActivity (Implicit)
- Shows app logo with minimum display time
- Loads initial events in background
- Transitions to MainActivity

### 2. MainActivity
- **Tab-based navigation** with bottom navigation bar
- **Tabs**: Events List, About, Settings (future)
- **State management** with TabManager
- **Persistent tab selection** across app restarts

### 3. Event Flow
```
EventsListScreen → EventActivity → EventFullMapActivity
                                 ↘ WaveActivity (when wave starts)
```

## Key Components

### 🏠 Activities (`/activities`)

#### MainActivity
```kotlin
// Main entry point with tab navigation
class MainActivity : AppCompatActivity() {
    // Sets up bottom navigation with:
    // - Events List tab
    // - About tab  
    // - Settings tab (future)
}
```

#### EventActivity
```kotlin
// Event details with countdown and preview
class EventActivity : AbstractEventBackActivity() {
    // Shows:
    // - Event countdown timer
    // - Event details and description  
    // - Map preview button
    // - Join wave button (when active)
}
```

#### EventFullMapActivity
```kotlin
// Full-screen offline map view
class EventFullMapActivity : AbstractEventBackActivity() {
    // Features:
    // - MapLibre offline maps
    // - Wave radius visualization
    // - City boundary overlays
    // - Dynamic feature loading
}
```

#### WaveActivity
```kotlin
// Real-time wave choreography experience
class WaveActivity : AbstractEventWaveActivity() {
    // Provides:
    // - Frame-based animations
    // - Audio synchronization
    // - Device motion integration
    // - Real-time coordination
}
```

### 🎨 Compose UI (`/compose`)

#### Tab Screens (`/tabs`)
```kotlin
// EventsListScreen.kt - Main events listing
@Composable
fun EventsListScreen() {
    LazyColumn {
        items(events) { event ->
            EventCard(
                event = event,
                onClick = { navigateToEvent(event) }
            )
        }
    }
}

// AboutScreen.kt - App information
@Composable
fun AboutScreen() {
    // Static content about the app
}
```

#### Common Components (`/common`)
- `ButtonWave` - Animated wave-style button
- `EventOverlays` - Event status indicators
- `Indicators` - Loading and status components
- `SocialNetworks` - Social sharing components
- `TextUtils` - Text formatting utilities

#### Choreography (`/choreographies`)
```kotlin
// Choreography.kt - Wave animation rendering
@Composable
fun ChoreographyView(
    choreographyManager: ChoreographyManager,
    onFrameUpdate: (ChoreographyFrame) -> Unit
) {
    // Renders frame-based wave animations
}
```

#### Map Components (`/map`)
```kotlin
// AndroidEventMap.kt - MapLibre integration
@Composable
fun AndroidEventMap(
    event: WWWEvent,
    showFullscreen: Boolean = false
) {
    AndroidView(
        factory = { context ->
            MapView(context).apply {
                // Configure MapLibre map
                setupOfflineMap(event.cityName)
            }
        }
    )
}
```

### 🧠 ViewModels (`/viewmodels`)

#### EventsViewModel
```kotlin
class EventsViewModel : ViewModel() {
    private val eventsFlow = MutableStateFlow<List<WWWEvent>>(emptyList())
    
    fun loadEvents() {
        viewModelScope.launch {
            // Load events from shared module
            val events = WWWEventObserver.loadEvents()
            eventsFlow.value = events
        }
    }
}
```

#### MapViewModel
```kotlin
class MapViewModel : ViewModel() {
    fun loadCityMap(cityName: String) {
        // Handle dynamic feature loading
        // Configure MapLibre with offline tiles
    }
}
```

### 🎯 Theme (`/theme`)
- `Color.kt` - App color palette
- `Type.kt` - Typography definitions
- `Theme.kt` - Main theme configuration

### 🔧 Utils (`/utils`)
- `LocationAccessHelpers` - Location permission handling
- `MapAvailabilityChecker` - Dynamic feature availability
- `CoroutineHelpers` - Coroutine utilities
- `AndroidWWWLocationProvider` - Android location service

## Configuration

### Build Setup (`build.gradle.kts`)
```kotlin
android {
    namespace = "com.worldwidewaves"
    
    // Dynamic Features - 40+ city maps
    dynamicFeatures += setOf(
        ":maps:android:paris_france",
        ":maps:android:london_england",
        // ... all other cities
    )
    
    buildFeatures {
        compose = true
        viewBinding = true
        buildConfig = true
    }
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
}

dependencies {
    implementation(projects.shared)           // Shared KMP module
    implementation(libs.androidx.activity.compose)
    implementation(compose.runtime)
    implementation(compose.foundation)
    implementation(compose.material)
    implementation(libs.maplibre.android)     // Offline maps
    implementation(libs.koin.android)         // DI
    implementation(libs.firebase.crashlytics) // Crash reporting
}
```

### Key Dependencies
- **Shared KMP module** - Business logic
- **Compose Multiplatform** - UI framework
- **MapLibre Android** - Offline mapping
- **Koin** - Dependency injection
- **Firebase** - Analytics and crashlytics
- **AndroidX** - Navigation, lifecycle, etc.

## Development Patterns

### Consuming Shared Module
```kotlin
class EventsViewModel : ViewModel() {
    private val eventObserver: WWWEventObserver by inject()
    
    fun loadEvents() {
        viewModelScope.launch {
            // Use shared business logic
            val events = eventObserver.loadEvents()
            _events.value = events
        }
    }
}
```

### MapLibre Integration
```kotlin
// Load offline map for city
private fun setupOfflineMap(cityName: String) {
    val mapView = MapView(context)
    
    // Check if dynamic feature is available
    if (MapAvailabilityChecker.isAvailable(cityName)) {
        // Load offline tiles from assets
        val tilesPath = "file:///android_asset/$cityName.mbtiles"
        mapView.setOfflineSource(tilesPath)
    }
}
```

### Choreography Integration
```kotlin
@Composable
fun WaveScreen(event: WWWEvent) {
    val choreographyManager: ChoreographyManager by inject()
    
    LaunchedEffect(event) {
        choreographyManager.startWaveSequence(event) { frame ->
            // Update animation frame
            currentFrame = frame
        }
    }
    
    ChoreographyView(
        frame = currentFrame,
        modifier = Modifier.fillMaxSize()
    )
}
```

## Testing

### Android Unit Tests
```kotlin
@Test
fun testEventViewModel() {
    val viewModel = EventsViewModel()
    viewModel.loadEvents()
    
    // Verify events loaded
    assert(viewModel.events.value.isNotEmpty())
}
```

### UI Tests (Future)
```kotlin
@Test
fun testEventNavigation() {
    composeTestRule.setContent {
        EventsListScreen()
    }
    
    composeTestRule
        .onNodeWithText("Event Title")
        .performClick()
    
    // Verify navigation to EventActivity
}
```

## Running the App

### Debug Build
```bash
# Build and install debug APK
./gradlew :composeApp:assembleDebug
./gradlew :composeApp:installDebug

# Run on connected device
./gradlew :composeApp:run
```

### Release Build
```bash
# Build optimized release APK
./gradlew :composeApp:assembleRelease

# Bundle for Play Store (with dynamic features)
./gradlew :composeApp:bundleRelease
```

## Dynamic Features

### City Map Loading
```kotlin
class MapAvailabilityChecker {
    fun isAvailable(cityName: String): Boolean {
        return splitInstallManager
            .installedModules
            .contains(cityName)
    }
    
    fun requestInstall(cityName: String) {
        val request = SplitInstallRequest.newBuilder()
            .addModule(cityName)
            .build()
        
        splitInstallManager.startInstall(request)
    }
}
```

### On-Demand Loading
- Maps are loaded when user requests specific city
- Reduces initial app size significantly
- Graceful fallback when map unavailable
- Progress indicators during download

## Adding New Features

### New Activity
1. Create activity class extending appropriate base class
2. Add to `AndroidManifest.xml`
3. Update navigation logic
4. Add corresponding Compose screens

### New Compose Screen
1. Create `@Composable` function
2. Add to appropriate package (`/tabs`, `/common`, etc.)
3. Integrate with navigation
4. Add preview functions for development

### New ViewModel
1. Extend `ViewModel` 
2. Inject shared module dependencies
3. Expose `StateFlow`/`LiveData` for UI
4. Handle lifecycle properly

## Troubleshooting

### Common Issues
1. **Dynamic feature not loading**: Check module is listed in `dynamicFeatures`
2. **Compose preview issues**: Verify preview parameters
3. **Map not rendering**: Check offline tiles path and availability
4. **Navigation issues**: Verify activity declarations in manifest

### Debug Tips
```bash
# Check APK dynamic features
./gradlew :composeApp:analyzeDebugBundle

# View APK contents
unzip -l composeApp/build/outputs/apk/debug/composeApp-debug.apk

# Monitor dynamic feature installs
adb logcat | grep SplitInstall
```