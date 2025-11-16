# Android Development Guide

> **Quick Links**: [Setup](#android-studio-setup) | [Build Commands](#build-commands) | [Debugging](#debugging) | [Testing](#testing) | [Common Issues](#common-issues)

## Project Overview

WorldWideWaves Android app is built using **Kotlin Multiplatform Mobile (KMM)** with **Jetpack Compose** for UI. The Android implementation shares 90%+ of its codebase with iOS through the `shared` module, with platform-specific code in `composeApp/src/androidMain/` and `shared/src/androidMain/`.

### Key Technologies

- **UI Framework**: Jetpack Compose (100% declarative UI)
- **Architecture**: MVVM + Clean Architecture + Koin DI
- **Maps**: MapLibre GL Native (offline-first)
- **Location**: Fused Location Provider + SimulationLocationEngine
- **Testing**: JUnit4 + Compose UI Testing + Espresso
- **Build**: Gradle 8.5+ with Kotlin DSL

---

## Android Studio Setup

### 1. Installation

**Recommended Version**: Android Studio Hedgehog (2023.1.1) or later

```bash
# macOS (via Homebrew Cask)
brew install --cask android-studio

# Linux (manual download)
# Download from: https://developer.android.com/studio
# Extract and run: ./android-studio/bin/studio.sh
```

### 2. Required Plugins

**Pre-installed**:

- ✅ Kotlin Multiplatform Mobile
- ✅ Android Gradle Plugin
- ✅ Jetpack Compose

**Additional (Optional)**:

- **Detekt**: Static analysis (already configured in build)
- **Rainbow Brackets**: Visual aid for nested code
- **GitToolBox**: Enhanced Git integration

### 3. SDK Manager Setup

**Required SDK Components**:

```
Tools:
  ✓ Android SDK Build-Tools 34.0.0
  ✓ Android Emulator
  ✓ Android SDK Platform-Tools
  ✓ Google Play Services

SDK Platforms:
  ✓ Android 14.0 (API 34) - Target SDK
  ✓ Android 8.0 (API 26) - Minimum SDK
```

**Install via command line**:

```bash
sdkmanager "platforms;android-34" \
  "build-tools;34.0.0" \
  "platform-tools" \
  "emulator" \
  "system-images;android-34;google_apis;arm64-v8a"
```

### 4. Emulator Configuration

**Recommended Emulator Setup**:

| Setting | Value |
| --------- | ------- |

| Device | Pixel 6 Pro |
| System Image | Android 14 (API 34) with Google APIs |
| RAM | 4096 MB |
| Internal Storage | 8192 MB |
| Graphics | Hardware - GLES 3.0 |

**Create emulator via command line**:

```bash
avdmanager create avd \
  -n "Pixel_6_Pro_API_34" \
  -k "system-images;android-34;google_apis;arm64-v8a" \
  -d "pixel_6_pro" \
  --force
```

**Enable location services**:

```bash
# Start emulator
emulator -avd Pixel_6_Pro_API_34 &

# Set location (San Francisco example)
adb emu geo fix -122.4194 37.7749
```

### 5. Project Import

```bash
# 1. Clone repository
git clone https://github.com/yourusername/WorldWideWaves.git
cd WorldWideWaves

# 2. Open in Android Studio
open -a "Android Studio" .

# 3. Sync Gradle (automatic on first open)
# Or manually: File > Sync Project with Gradle Files

# 4. Wait for indexing to complete
# Check bottom-right status bar for "Indexing..."
```

**First Build** (may take 5-10 minutes):

```bash
./gradlew :composeApp:assembleDebug
```

---

## Build Commands

### Standard Builds

```bash
# Debug build (development)
./gradlew :composeApp:assembleDebug

# Release build (production - requires signing)
./gradlew :composeApp:assembleRelease

# Install debug APK on connected device/emulator
./gradlew :composeApp:installDebug

# Clean build (when encountering cache issues)
./gradlew clean :composeApp:assembleDebug
```

### Build Variants

WorldWideWaves uses **40+ product flavors** for different city maps:

```bash
# Build specific city variant
./gradlew :composeApp:assembleNew_york_usaDebug
./gradlew :composeApp:assembleParis_franceDebug
./gradlew :composeApp:assembleTokyo_japanDebug

# List all available variants
./gradlew :composeApp:tasks --group="build"
```

### Shared Module Build

```bash
# Build shared KMM module (both Android and iOS)
./gradlew :shared:build

# Android-specific shared module
./gradlew :shared:compileDebugKotlinAndroid

# Run shared module unit tests
./gradlew :shared:testDebugUnitTest
```

### Bundle for Play Store

```bash
# Create Android App Bundle (.aab)
./gradlew :composeApp:bundleRelease

# Output: composeApp/build/outputs/bundle/release/composeApp-release.aab
```

---

## Debugging

### 1. Logcat Filtering

WorldWideWaves uses custom log tags with `WWW` prefix:

```bash
# Filter all app logs
adb logcat -s "WWW*:V"

# Specific component logs
adb logcat -s "EventMap:V" "AndroidEventMap:V"
adb logcat -s "PositionManager:V" "AndroidLocationProvider:V"
adb logcat -s "MainApplication:V" "MainActivity:V"

# Multiple tags with grep
adb logcat | grep -E "WWW|EventMap|MainActivity"

# Save logs to file
adb logcat -s "WWW*:V" > app_logs.txt
```

**Log Levels**:

- `Log.v()` - Verbose (development)
- `Log.d()` - Debug (detailed info)
- `Log.i()` - Info (general info)
- `Log.w()` - Warning (potential issues)
- `Log.e()` - Error (failures)

### 2. Breakpoint Debugging

**Android Studio Debugger**:

```bash
# 1. Set breakpoints (click left gutter in code editor)

# 2. Run in debug mode
# Menu: Run > Debug 'composeApp'
# Or shortcut: Shift+F9

# 3. Debug controls
# F8 - Step over
# F7 - Step into
# Shift+F8 - Step out
# F9 - Resume program
```

**Attach to Running Process**:

```bash
# 1. Install debug APK
./gradlew :composeApp:installDebug

# 2. Launch app
adb shell am start -D -n com.worldwidewaves/.activities.MainActivity

# 3. Attach debugger
# Android Studio: Run > Attach Debugger to Android Process
# Select "com.worldwidewaves" process
```

### 3. Memory Profiling

**Android Profiler** (built into Android Studio):

1. **Start profiling**: View > Tool Windows > Profiler
2. **Select device/process**: com.worldwidewaves
3. **Monitor**:
   - Memory usage (heap allocations)
   - CPU usage (method tracing)
   - Network activity (HTTP requests)

**Memory leak detection**:

```bash
# Capture heap dump
adb shell am dumpheap com.worldwidewaves /data/local/tmp/heap.hprof

# Pull to local machine
adb pull /data/local/tmp/heap.hprof .

# Analyze with Android Studio
# File > Open > heap.hprof
```

### 4. Network Inspection

**OkHttp Logging Interceptor** (if added):

```kotlin
// Add to build.gradle.kts dependencies
implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

// Enable in code
val interceptor = HttpLoggingInterceptor().apply {
    level = HttpLoggingInterceptor.Level.BODY
}
```

**Charles Proxy / Proxyman** (external tools):

```bash
# Configure proxy on emulator
adb shell settings put global http_proxy <host>:<port>

# Remove proxy
adb shell settings put global http_proxy :0
```

### 5. MapLibre Debugging

**Enable MapLibre verbose logging**:

```kotlin
// In MainActivity.onCreate()
MapLibre.getInstance(this).apply {
    setLoggingEnabled(BuildConfig.DEBUG)
}
```

**Logcat filter for MapLibre**:

```bash
# MapLibre GL native logs
adb logcat -s "Mbgl-*:V" "MLN*:V"

# Combined with app logs
adb logcat -s "WWW*:V" "Mbgl-*:V" "EventMap:V"
```

**Common MapLibre issues**:

- Tiles not loading: Check `style.json` path in logcat
- Camera bounds errors: Check `MapBoundsEnforcer` logs
- Location dot not showing: Check `LocationComponent` activation logs

### 6. Layout Inspector

**Inspect Compose UI hierarchy**:

1. Tools > Layout Inspector
2. Select running device
3. View Compose layout tree
4. Check modifiers, semantics, bounds

**Useful for**:

- Debugging touch target sizes (48dp minimum)
- Verifying accessibility contentDescription
- Checking padding/spacing values

---

## Testing

### Unit Tests

**Comprehensive shared unit test suite** + Android-specific tests

```bash
# Run all unit tests
./gradlew :shared:testDebugUnitTest

# Run specific test class
./gradlew :shared:testDebugUnitTest --tests "com.worldwidewaves.shared.domain.observation.*"

# Generate HTML report
./gradlew :shared:testDebugUnitTest
# Open: shared/build/reports/tests/testDebugUnitTest/index.html

# Run with coverage
./gradlew :shared:testDebugUnitTest jacocoTestReport
```

**Expected results**:

- ✅ All tests passing
- ✅ Fast execution time
- ✅ 0 failures, 0 skipped

### Instrumented Tests

**UI tests with Compose Testing + Espresso**

**Prerequisites**:

```bash
# 1. Start emulator
emulator -avd Pixel_6_Pro_API_34 &

# 2. Verify device connected
adb devices
# Should show: emulator-5554 device

# 3. Grant location permissions
adb shell pm grant com.worldwidewaves android.permission.ACCESS_FINE_LOCATION
adb shell pm grant com.worldwidewaves android.permission.ACCESS_COARSE_LOCATION
```

**Run tests**:

```bash
# All instrumented tests
ANDROID_SERIAL=emulator-5556 ./gradlew :composeApp:connectedDebugAndroidTest

# Specific test class
./gradlew :composeApp:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.worldwidewaves.compose.events.EventsListScreenTest

# Generate report
# Open: composeApp/build/reports/androidTests/connected/index.html
```

**Test categories**:

- **Accessibility**: `AccessibilityTest.kt`, `AutomatedAccessibilityScanTest.kt`
- **Map integration**: `MapIntegrationTest.kt`, `EventDetailScreenMapTest.kt`
- **E2E flows**: `CompleteWaveParticipationE2ETest.kt`
- **Performance**: `PerformanceMemoryTest.kt`

### Testing from Android Studio

**Run single test**:

1. Open test file (e.g., `EventsListScreenTest.kt`)
2. Click green arrow next to test function
3. Select "Run 'testEventListLoads()'"

**Run test class**:

1. Right-click test file
2. Select "Run 'EventsListScreenTest'"

**Debug tests**:

1. Set breakpoints in test or app code
2. Right-click test
3. Select "Debug 'testName'"

---

## Android-Specific Architecture

### Activity Lifecycle

**Main Activities**:

| Activity | Purpose | Lifecycle Notes |
| ---------- | --------- | ---------------- |

| `MainActivity` | Entry point, event list | Hosts `MainScreen` Composable |
| `EventActivity` | Event detail screen | Receives `eventId` via Intent |
| `WaveActivity` | Wave participation | Manages real-time coordination |
| `EventFullMapActivity` | Full-screen map | Receives `eventId` via Intent |

**Lifecycle flow**:

```
MainActivity
    onCreate() → installSplashScreen() → setContent { MainScreen() }
    onResume() → mainActivityImpl.onResume()
    onPause() → mainActivityImpl.onPause()
    onDestroy() → mainActivityImpl.onDestroy()
```

**File**: `composeApp/src/androidMain/kotlin/com/worldwidewaves/activities/MainActivity.kt`

### Permission Handling

**Location Permissions** (runtime):

```kotlin
// Request permissions (Compose)
@Composable
fun requestLocationPermission(): Boolean {
    val permissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    var granted by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        granted = results.all { it.value }
    }

    LaunchedEffect(Unit) {
        launcher.launch(permissions)
    }

    return granted
}
```

**File**: `shared/src/androidMain/kotlin/com/worldwidewaves/shared/ui/components/permissions/LocationPermissions.android.kt`

**Declared in AndroidManifest.xml**:

```xml
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

### Background Services

**No long-running background services** - ephemeral app design

- Wave coordination happens in foreground only
- No WorkManager jobs
- No foreground services
- Location updates stop when app is paused

### AndroidManifest Configuration

**Key configurations**:

```xml
<application
    android:name=".MainApplication"
    android:allowBackup="false"
    android:theme="@style/Theme.WorldWideWaves.Splash"
    android:localeConfig="@xml/locales_config"
    android:networkSecurityConfig="@xml/network_security_config">

    <!-- Activities locked to portrait -->
    <activity android:screenOrientation="portrait" />

    <!-- SplitCompat for dynamic feature modules -->
    <!-- Removed InitializationProvider for manual control -->
</application>
```

**File**: `composeApp/src/androidMain/AndroidManifest.xml`

---

## Platform Services

### 1. AndroidLocationProvider

**Real GPS location provider** using MapLibre's LocationEngine:

```kotlin
class AndroidLocationProvider : LocationProvider {
    private val locationEngine: SimulationLocationEngine by inject()
    private val _currentLocation = MutableStateFlow<Position?>(null)
    override val currentLocation: StateFlow<Position?> = _currentLocation

    @SuppressLint("MissingPermission")
    override fun startLocationUpdates(onLocationUpdate: (Position) -> Unit) {
        proxyLocationEngine = LocationEngineProxy(locationEngine)
        proxyLocationEngine?.requestLocationUpdates(
            buildLocationEngineRequest(),
            locationCallback,
            Looper.getMainLooper()
        )
    }
}
```

**Features**:

- GPS update interval: `WWWGlobals.Timing.GPS_UPDATE_INTERVAL` (default 1s)
- High accuracy mode
- Reactive StateFlow updates
- Automatic lifecycle management

**File**: `composeApp/src/androidMain/kotlin/com/worldwidewaves/utils/AndroidLocationProvider.kt`

### 2. AndroidPlatformEnabler

**Platform-specific navigation and utilities**:

```kotlin
class AndroidPlatformEnabler(val context: Context?) : PlatformEnabler {
    // Navigate to event detail
    override fun openEventActivity(eventId: String) {
        context.startActivity(
            Intent(context, EventActivity::class.java).apply {
                putExtra("eventId", eventId)
            }
        )
    }

    // Show toast notification
    override fun toast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    // Open external URL
    override fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    }
}
```

**File**: `composeApp/src/androidMain/kotlin/com/worldwidewaves/utils/AndroidPlatformEnabler.kt`

### 3. SimulationLocationEngine

**Testing-only location provider** for UI tests and development:

```kotlin
class SimulationLocationEngine(private val platform: WWWPlatform) {
    fun setLocation(latitude: Double, longitude: Double) {
        platform.getSimulation()?.setUserPosition(Position(latitude, longitude))
    }
}
```

**Usage**:

```bash
# Enable simulation mode
# In MainApplication.onCreate():
initializeSimulationMode(platform, BuildConfig.ENABLE_SIMULATION_MODE)
```

**File**: `composeApp/src/androidMain/kotlin/com/worldwidewaves/utils/SimulationLocationEngine.kt`

### 4. Android DI Modules

**Koin dependency injection setup**:

```kotlin
// androidModule (shared/src/androidMain)
val androidModule = module {
    single<WWWPlatform> { WWWPlatform("Android ${Build.VERSION.SDK_INT}", get()) }
    single<SoundPlayer> { AndroidSoundPlayer(get()) }
    single<DataStoreFactory> { DefaultDataStoreFactory() }
    single<FavoriteEventsStore> { AndroidFavoriteEventsStore(get()) }
}

// applicationModule (composeApp/src/androidMain)
val applicationModule = module {
    single<PlatformEnabler> { AndroidPlatformEnabler() }
    single<MapAvailabilityChecker> { AndroidMapAvailabilityChecker(androidContext()) }
    viewModel { EventsViewModel(...) }
    viewModel { AndroidMapViewModel(get()) }
}
```

**Initialization** (MainApplication.kt):

```kotlin
override fun onCreate() {
    startKoin {
        androidContext(this@MainApplication)
        androidLogger()
        modules(sharedModule + androidModule + applicationModule)
    }
}
```

**Files**:

- `shared/src/androidMain/kotlin/com/worldwidewaves/shared/di/AndroidModule.kt`
- `composeApp/src/androidMain/kotlin/com/worldwidewaves/di/ApplicationModule.kt`

---

## Common Issues

### 1. Gradle Sync Failures

**Symptom**: "Could not resolve dependencies" or "Plugin not found"

**Solutions**:

```bash
# Clear Gradle cache
./gradlew clean
rm -rf ~/.gradle/caches/

# Invalidate Android Studio caches
# File > Invalidate Caches > Invalidate and Restart

# Update Gradle wrapper
./gradlew wrapper --gradle-version=8.5

# Check internet connection (Gradle downloads dependencies)
```

### 2. MapLibre Tile Loading Issues

**Symptom**: Blank map or "Failed to load style" errors

**Diagnosis**:

```bash
# Check style.json exists
adb logcat -s "EventMap:V" | grep "style"

# Verify map module downloaded
adb shell pm list packages | grep worldwidewaves

# Check file paths
adb logcat | grep "getStyleUri"
```

**Solutions**:

```kotlin
// Clear map cache (in debug screen or manually)
val cacheDir = context.cacheDir
File(cacheDir, "$eventId.mbtiles").delete()
File(cacheDir, "$eventId.geojson").delete()

// Re-download map feature module
mapViewModel.downloadMap(eventId)
```

### 3. Location Permission Denials

**Symptom**: Location dot not showing on map, "Permission denied" logs

**Check permissions**:

```bash
# List granted permissions
adb shell dumpsys package com.worldwidewaves | grep permission

# Grant manually
adb shell pm grant com.worldwidewaves android.permission.ACCESS_FINE_LOCATION
adb shell pm grant com.worldwidewaves android.permission.ACCESS_COARSE_LOCATION
```

**Runtime permission flow**:

```kotlin
// Verify permissions granted
val hasPermission = ContextCompat.checkSelfPermission(
    context,
    Manifest.permission.ACCESS_FINE_LOCATION
) == PackageManager.PERMISSION_GRANTED

// Request if not granted
ActivityCompat.requestPermissions(
    activity,
    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
    REQUEST_CODE
)
```

### 4. Compose Rendering Issues

**Symptom**: UI not updating, blank screens, wrong state

**Debug steps**:

```kotlin
// Add logging to composables
@Composable
fun MyScreen() {
    val state by viewModel.state.collectAsState()

    Log.d("MyScreen", "Recomposing with state: $state")

    // Check if state changes trigger recomposition
    LaunchedEffect(state) {
        Log.d("MyScreen", "State changed: $state")
    }
}
```

**Common fixes**:

```kotlin
// Ensure mutableStateOf in remember
var value by remember { mutableStateOf(initialValue) }

// Use derivedStateOf for computed values
val isValid by remember {
    derivedStateOf { field1.isNotEmpty() && field2.isNotEmpty() }
}

// Ensure StateFlow collected properly
val state by viewModel.stateFlow.collectAsState()
```

### 5. Emulator Connectivity Issues

**Symptom**: `adb devices` shows no devices, emulator offline

**Solutions**:

```bash
# Restart ADB
adb kill-server
adb start-server
adb devices

# Restart emulator
adb reboot

# Check emulator process
ps aux | grep emulator

# Re-create emulator
avdmanager delete avd -n Pixel_6_Pro_API_34
avdmanager create avd -n Pixel_6_Pro_API_34 -k "system-images;android-34;google_apis;arm64-v8a"
```

### 6. Build Cache Issues

**Symptom**: Builds fail with "duplicate class" or stale resources

**Nuclear option**:

```bash
# Clean everything
./gradlew clean
rm -rf build/
rm -rf composeApp/build/
rm -rf shared/build/
rm -rf ~/.gradle/caches/
rm -rf .idea/

# Rebuild
./gradlew :composeApp:assembleDebug
```

### 7. MapLibre Crashes

**Symptom**: Native crash in `libmaplibre.so`

**Common causes**:

- Accessing map before style loaded
- UI thread violations
- Invalid camera bounds

**Fix**:

```kotlin
// Always check style loaded
map.style?.let { style ->
    // Safe to access layers/sources
    style.addSource(...)
}

// Use UI thread for map operations
context.runOnUiThread {
    mapLibreAdapter.updateCamera(...)
}

// Validate bounds before setting
val isValid = validateBounds(constraintBounds)
if (isValid) {
    map.setLatLngBoundsForCameraTarget(constraintBounds)
}
```

---

## Performance Best Practices

### 1. Minimize Recompositions

```kotlin
// Use keys to avoid unnecessary recompositions
LazyColumn {
    items(events, key = { it.id }) { event ->
        EventCard(event)
    }
}

// Extract stable composables
@Composable
fun EventCard(event: Event, modifier: Modifier = Modifier) {
    // Recomposes only when event or modifier changes
}
```

### 2. Optimize Map Updates

```kotlin
// Batch polygon updates
override fun updateWavePolygons(wavePolygons: List<Polygon>, clearPolygons: Boolean) {
    context.runOnUiThread {
        // Process all polygons in single UI thread operation
        val mapLibrePolygons = wavePolygons.map { it.toMapLibrePolygon() }
        mapLibreAdapter.addWavePolygons(mapLibrePolygons, clearPolygons)
    }
}
```

### 3. Location Update Debouncing

**Already implemented in PositionManager**:

- Filters duplicate positions
- Debounces rapid updates
- Priority: SIMULATION > GPS

### 4. Memory Management

```kotlin
// Clean up in onDestroy
override fun onDestroy() {
    locationProvider.stopLocationUpdates()
    mapLibreAdapter.cleanup()
    super.onDestroy()
}

// Use viewModelScope for coroutines (auto-canceled)
class EventsViewModel : ViewModel() {
    fun loadEvents() {
        viewModelScope.launch {
            // Automatically canceled when ViewModel cleared
        }
    }
}
```

---

## Accessibility

**Android TalkBack support** (WCAG 2.1 Level AA compliant):

```kotlin
// All interactive elements have semantics
Button(
    onClick = { action() },
    modifier = Modifier.semantics {
        role = Role.Button
        contentDescription = "Clear action description"
    }
)

// Touch targets ≥ 48dp
Box(
    modifier = Modifier
        .size(48.dp) // Minimum touch target
        .clickable { action() }
)

// Dynamic content announcements
LaunchedEffect(countdown) {
    if (countdown == 5) {
        // TalkBack announcement via Toast
        platformEnabler.toast("Wave starting in 5 seconds")
    }
}
```

**Testing**:

```bash
# Enable TalkBack on emulator
adb shell settings put secure enabled_accessibility_services com.google.android.marvin.talkback/com.google.android.marvin.talkback.TalkBackService

# Navigate app with TalkBack
# Swipe right: Next element
# Swipe left: Previous element
# Double-tap: Activate
```

**Documentation**: [docs/accessibility-guide.md](../accessibility-guide.md)

---

## Related Documentation

### Android-Specific

- **[Android Map Constraints](./android-map-constraint-index.md)** - Map constraint implementation
- **[Android Patterns Quick Reference](./android-patterns-quick-reference.md)** - Common code patterns
- **[Android Source File Reference](./android-source-file-reference.md)** - File locations

### General

- **[Development Workflow](../development.md)** - Cross-platform development
- **[Testing Guide](../testing/README.md)** - Comprehensive testing
- **[CI/CD Pipeline](../ci-cd.md)** - GitHub Actions workflows
- **[Accessibility Guide](../accessibility-guide.md)** - WCAG compliance

### iOS Comparison

- **[iOS Development Guide](../ios/CLAUDE_iOS.md)** - iOS equivalent documentation
- **[iOS/Android Parity](../ios/ios-android-map-parity-gap-analysis.md)** - Platform differences

---

## Quick Reference Commands

```bash
# Build & Install
./gradlew :composeApp:assembleDebug
./gradlew :composeApp:installDebug

# Testing
./gradlew :shared:testDebugUnitTest
ANDROID_SERIAL=emulator-5556 ./gradlew :composeApp:connectedDebugAndroidTest

# Debugging
adb logcat -s "WWW*:V"
adb shell am start -D -n com.worldwidewaves/.activities.MainActivity

# Device Management
adb devices
emulator -avd Pixel_6_Pro_API_34 &
adb emu geo fix -122.4194 37.7749

# Permissions
adb shell pm grant com.worldwidewaves android.permission.ACCESS_FINE_LOCATION

# Clean Build
./gradlew clean
rm -rf build/ composeApp/build/ shared/build/
```

---

**Maintainer**: WorldWideWaves Development Team
