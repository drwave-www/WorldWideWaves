# WorldWideWaves - iOS Development Guide

> **Parent Documentation**: [CLAUDE.md](./CLAUDE.md)
> **Status**: ✅ STABLE | **Bundle**: com.worldwidewaves

---

## Table of Contents

1. [Quick Start](#quick-start)
2. [Architecture Overview](#architecture-overview)
3. [iOS Deadlock Prevention (CRITICAL)](#ios-deadlock-prevention-critical)
4. [iOS Cinterop Memory Safety (CRITICAL)](#ios-cinterop-memory-safety-critical)
5. [Setup and Configuration](#setup-and-configuration)
6. [Building and Running](#building-and-running)
7. [Debugging Guide](#debugging-guide)
8. [Testing](#testing)
9. [Common Issues and Solutions](#common-issues-and-solutions)
10. [Advanced Topics](#advanced-topics)

---

## Quick Start

### Prerequisites

- macOS 13.0+ (Ventura or later)
- Xcode 16.0+
- CocoaPods 1.12+
- JDK 17+
- Kotlin 1.9+

### First Time Setup

```bash
# 1. Clone and navigate to project
cd .

# 2. Build Kotlin framework
./gradlew :shared:embedAndSignAppleFrameworkForXcode

# 3. Open iOS project
cd iosApp
open worldwidewaves.xcodeproj

# 4. Select scheme and simulator
# - Scheme: worldwidewaves
# - Device: iPhone 15 Pro (iOS 18.5 Simulator)

# 5. Build and run (Cmd+R)
```

### Verify Installation

```bash
# Run iOS safety verification
./scripts/dev/verification/verify-ios-safety.sh

# All checks should pass:
# ✅ No Composable-scoped KoinComponent
# ✅ No init{} coroutine launches
# ✅ No init{} DI access
# ✅ IOSSafeDI singleton exists
```

---

## Architecture Overview

### Technology Stack

**UI Framework**: Compose Multiplatform (NOT SwiftUI)

- Shared Compose UI code between Android and iOS
- ComposeUIViewController bridges Kotlin Compose to UIKit
- 100% code reuse for UI components

**Integration Pattern**: SceneDelegate + RootController

- AppDelegate: URL routing and app lifecycle
- SceneDelegate: Window management and platform initialization
- RootController.kt: Kotlin ViewController factories
- IOSPlatformEnabler.swift: Swift-Kotlin bridge

### Architecture Flow

```
User Action
    ↓
AppDelegate (Swift) → URL routing
    ↓
SceneDelegate (Swift) → Platform initialization
    ↓
RootController.kt (Kotlin) → ViewController factory
    ↓
WWWActivity (Kotlin) → Activity wrapper
    ↓
Compose UI (Kotlin) → Shared UI components
    ↓
Business Logic (Kotlin) → Domain and data layers
```

### Key iOS Files

| File | Purpose | Critical? |
|------|---------|-----------|
| `AppDelegate.swift` | URL routing, app lifecycle | ✅ Yes |
| `SceneDelegate.swift` | Window management, initialization | ✅ Yes |
| `IOSPlatformEnabler.swift` | Swift-Kotlin bridge | ✅ Yes |
| `RootController.kt` | ViewController factories | ✅ Yes |
| `IOSSafeDI.kt` | Safe dependency injection | ✅ Yes |
| `Platform_ios.kt` | Koin + MokoRes initialization | ✅ Yes |

---

## iOS Deadlock Prevention [CRITICAL]

### Why iOS Deadlocks Occur

Kotlin/Native on iOS has strict threading requirements due to:

1. **Main thread freeze prevention**: iOS main thread must remain responsive
2. **Dispatcher initialization order**: `Dispatchers.Main` must be ready before use
3. **Koin initialization timing**: DI must be initialized on correct thread
4. **Compose lifecycle**: ComposeUIViewController requires specific initialization

**Violating these requirements causes immediate app deadlock on launch.**

### The 6 Absolute Rules

#### Rule 1: No `object : KoinComponent` in @Composable

```kotlin
// ❌ DEADLOCKS iOS
@Composable
fun EventScreen() {
    val deps = object : KoinComponent {
        val events by inject()  // DEADLOCK!
    }
}

// ✅ CORRECT: Use IOSSafeDI
@Composable
fun EventScreen() {
    val events = getIOSSafeEvents()  // Safe wrapper
}
```

**Why it deadlocks**: Creating object during Compose composition freezes main thread while Koin initializes on worker thread.

#### Rule 2: No `by inject()` During Composition

```kotlin
// ❌ DEADLOCKS iOS
@Composable
fun EventScreen() {
    val viewModel = remember {
        object {
            val vm by inject<EventViewModel>()  // DEADLOCK!
        }
    }
}

// ✅ CORRECT: Resolve before composition
@Composable
fun EventScreen() {
    val viewModel = LocalKoin.current.get<EventViewModel>()
}
```

#### Rule 3: No `runBlocking` Before ComposeUIViewController

```kotlin
// ❌ DEADLOCKS iOS
fun makeMainViewController(): UIViewController {
    runBlocking {  // DEADLOCK!
        initializeData()
    }
    return ComposeUIViewController { /* ... */ }
}

// ✅ CORRECT: Use suspend functions
suspend fun initializeData() { /* ... */ }

fun makeMainViewController(): UIViewController {
    return ComposeUIViewController {
        LaunchedEffect(Unit) {
            initializeData()  // Safe async init
        }
    }
}
```

#### Rule 4: No Coroutine Launches in init{}

```kotlin
// ❌ DEADLOCKS iOS
class EventViewModel {
    init {
        CoroutineScope.launch {  // DEADLOCK!
            loadEvents()
        }
    }
}

// ✅ CORRECT: Suspend initialization
class EventViewModel {
    suspend fun initialize() {
        loadEvents()
    }
}

@Composable
fun EventScreen() {
    val viewModel = remember { EventViewModel() }
    LaunchedEffect(Unit) {
        viewModel.initialize()
    }
}
```

#### Rule 5: No DI Access in init{}

```kotlin
// ❌ DEADLOCKS iOS
class EventRepository {
    init {
        val db = get<Database>()  // DEADLOCK!
    }
}

// ✅ CORRECT: Constructor injection
class EventRepository(
    private val db: Database  // Injected via constructor
)
```

#### Rule 6: No Dispatchers.Main in Constructors

```kotlin
// ❌ DEADLOCKS iOS
class MyViewModel {
    private val scope = CoroutineScope(Dispatchers.Main)  // DEADLOCK!
}

// ✅ CORRECT: Lazy initialization
class MyViewModel {
    private val scope by lazy {
        CoroutineScope(Dispatchers.Main)
    }
}
```

### The IOSSafeDI Pattern

**Purpose**: Provide safe dependency access from Composables on iOS.

**Implementation** (`shared/src/commonMain/kotlin/ui/utils/IOSSafeDI.kt`):

```kotlin
object IOSSafeDI : KoinComponent {
    val platform: WWWPlatform by inject()
    val clock: IClock by inject()
    val events: WWWEvents by inject()
    // ... other common dependencies
}

// Safe accessor functions
fun getIOSSafePlatform(): WWWPlatform = IOSSafeDI.platform
fun getIOSSafeClock(): IClock = IOSSafeDI.clock
fun getIOSSafeEvents(): WWWEvents = IOSSafeDI.events
```

**Why it's safe**:

- File-level object (initialized once at app start)
- Properties resolved lazily after Koin initialization
- No composition-time dependency resolution

**Usage in Composables**:

```kotlin
@Composable
fun MyScreen() {
    val clock = getIOSSafeClock()  // ✅ Safe
    val platform = getIOSSafePlatform()  // ✅ Safe

    // Use dependencies
    Text("Current time: ${clock.now()}")
}
```

### Verification Commands

Run these commands before every commit touching shared code:

```bash
# 1. ✅ Find Composable-scoped KoinComponent (MUST BE ZERO)
rg -B10 "object.*KoinComponent" shared/src/commonMain --type kotlin \
  | rg "@Composable" -A10 | rg "object.*KoinComponent"

# 2. ✅ Find init{} coroutine launches (MUST BE ZERO)
rg -n -A 5 "init\s*\{" shared/src/commonMain --type kotlin \
  | rg "launch\{|async\{|scope\."

# 3. ✅ Find init{} DI access (MUST BE ZERO)
rg -n -A 3 "init\s*\{" shared/src/commonMain --type kotlin \
  | rg "get\(\)|inject\(\)" | rg -v "// iOS FIX"

# 4. ℹ️ Verify IOSSafeDI exists (MUST BE ONE)
rg "object IOSSafeDI : KoinComponent" shared/src/commonMain --type kotlin

# 5. ℹ️ Review file-level singletons (verify intentional)
rg "^object.*: KoinComponent" shared/src/commonMain --type kotlin
```

**Expected Results**:

- Commands 1-3: **ZERO results**
- Command 4: **ONE result** (IOSSafeDI.kt)
- Command 5: **Few results** (intentional singletons)

**Automated verification**:

```bash
./scripts/dev/verification/verify-ios-safety.sh
# Exit code 0 = safe
# Exit code 1 = violations found
```

---

## iOS Cinterop Memory Safety [CRITICAL]

> **Status**: ✅ REQUIRED | **Tests**: All passing | **Violations**: None

### Memory Safety Rules

iOS Kotlin/Native requires **strict memory management** for C interop (CoreLocation, AVFoundation, POSIX APIs).

#### The 3 Memory Safety Rules

**❌ NEVER**:

1. Use `NSData.create()` without `usePinned { }`
2. Access struct fields (`.coordinate.latitude`) without `useContents { }`
3. Use `addressOf()` outside pinned scope

**✅ ALWAYS**:

1. Pin ByteArray with `usePinned { }` before passing to C APIs
2. Use `useContents { }` for struct field access
3. Keep pointers within pinned scope - never escape them

#### Quick Examples

```kotlin
// ✅ SAFE: Memory pinning
bytes.usePinned { pinned ->
    NSData.create(bytes = pinned.addressOf(0), length = bytes.size.toULong())
        .writeToFile(path, atomically = true)
}

// ✅ SAFE: Struct access
location.coordinate.useContents {
    val position = Position(lat = latitude, lng = longitude)
}

// ❌ UNSAFE: No pinning
val nsData = NSData.create(bytes = bytes[0], length = bytes.size.toULong())  // CRASH!

// ❌ UNSAFE: Direct struct access
val lat = location.coordinate.latitude  // UNDEFINED BEHAVIOR!
```

#### Verification

**Before EVERY commit** touching iOS platform code:

```bash
./scripts/dev/verification/verify-ios-safety.sh
```

**Expected**: Zero violations in checks 8-11 (cinterop safety).

**See**:

- [Cinterop Memory Safety Patterns](docs/ios/cinterop-memory-safety-patterns.md) - Complete guide
- [Platform API Usage Guide](docs/ios/platform-api-usage-guide.md) - Threading & safety
- [Swift-Kotlin Bridging Guide](docs/ios/swift-kotlin-bridging-guide.md) - Type conversions

---

## Setup and Configuration

### Xcode Project Structure

```
iosApp/
├── worldwidewaves.xcodeproj         # Xcode project
├── worldwidewaves/
│   ├── AppDelegate.swift            # App lifecycle, URL routing
│   ├── SceneDelegate.swift          # Scene management, initialization
│   ├── IOSPlatformEnabler.swift     # Swift-Kotlin bridge
│   ├── WWWLog.swift                 # Swift logging wrapper
│   ├── SwiftNativeMapViewProvider.swift  # Map provider
│   ├── Info.plist                   # App configuration
│   └── Assets.xcassets/             # App assets
└── Build Scripts/                   # Gradle integration
```

### Critical Plist Entries

**Info.plist** must contain:

```xml
<!-- Compose performance optimization -->
<key>CADisableMinimumFrameDurationOnPhone</key>
<true/>

<!-- Location permissions -->
<key>NSLocationWhenInUseUsageDescription</key>
<string>WorldWideWaves needs your location to show wave events near you</string>

<!-- Deep linking -->
<key>CFBundleURLTypes</key>
<array>
    <dict>
        <key>CFBundleURLSchemes</key>
        <array>
            <string>worldwidewaves</string>
        </array>
    </dict>
</array>
```

### Platform Initialization Sequence

**SceneDelegate.swift**:

```swift
private func installPlatform() {
    NSLog("[SceneDelegate] 📦 Installing platform...")

    // 1. Initialize Kotlin/Native platform (Koin + MokoRes)
    do {
        _ = try Platform_iosKt.doInitPlatform()
        NSLog("[SceneDelegate] ✅ Kotlin platform initialized")
    } catch {
        NSLog("[SceneDelegate] ❌ Platform initialization failed: \(error)")
    }

    // 2. Install iOS lifecycle hook
    IosLifecycleHookKt.installIosLifecycleHook()

    // 3. Register platform enabler (Swift → Kotlin bridge)
    let enabler = IOSPlatformEnabler()
    IOSPlatformEnablerKt.registerPlatformEnabler(enabler: enabler)

    // 4. Register native map provider
    let mapProvider = SwiftNativeMapViewProvider()
    NativeMapViewProviderRegistrationKt.registerNativeMapViewProvider(provider: mapProvider)

    NSLog("[SceneDelegate] ✅ Platform installation complete")
}
```

**Order is critical**: Koin must initialize before any DI access.

### Gradle Integration

Xcode build phase runs:

```bash
cd "$SRCROOT/.."
./gradlew :shared:embedAndSignAppleFrameworkForXcode
```

This:

1. Compiles Kotlin code
2. Generates framework at `shared/build/xcode-frameworks/`
3. Embeds framework in app bundle
4. Signs framework with development certificate

**Framework output**: `../shared/build/xcode-frameworks/Debug/iphonesimulator18.5/shared.framework`

---

## Building and Running

### From Xcode (Recommended)

```bash
# 1. Open project
cd iosApp
open worldwidewaves.xcodeproj

# 2. Select configuration
# - Scheme: worldwidewaves
# - Device: iPhone 15 Pro (Simulator)
# - Build Configuration: Debug

# 3. Build (Cmd+B) or Run (Cmd+R)
```

**First build**: Takes 5-10 minutes (Kotlin compilation)
**Incremental builds**: 30-60 seconds (Swift only)

### From Command Line

```bash
# Build Kotlin framework
./gradlew :shared:embedAndSignAppleFrameworkForXcode

# Build iOS app
cd iosApp
xcodebuild \
  -project worldwidewaves.xcodeproj \
  -scheme worldwidewaves \
  -configuration Debug \
  -destination 'platform=iOS Simulator,name=iPhone 15 Pro,OS=18.5' \
  build

# Install to simulator
SIMULATOR_ID=$(xcrun simctl list devices | grep "iPhone 15 Pro" | grep Booted | awk -F'[()]' '{print $2}')
xcrun simctl install $SIMULATOR_ID ~/Library/Developer/Xcode/DerivedData/worldwidewaves-*/Build/Products/Debug-iphonesimulator/worldwidewaves.app

# Launch app
xcrun simctl launch $SIMULATOR_ID com.worldwidewaves
```

### Clean Build

```bash
# Clean Gradle
./gradlew clean

# Clean Xcode
cd iosApp
xcodebuild clean \
  -project worldwidewaves.xcodeproj \
  -scheme worldwidewaves

# Clean derived data
rm -rf ~/Library/Developer/Xcode/DerivedData/worldwidewaves-*

# Rebuild
./gradlew :shared:embedAndSignAppleFrameworkForXcode
cd iosApp && xcodebuild -project worldwidewaves.xcodeproj -scheme worldwidewaves build
```

---

## Debugging Guide

### View Console Logs

```bash
# Stream all app logs
xcrun simctl spawn booted log stream \
  --predicate 'process == "worldwidewaves"' \
  --level debug

# Filter for specific tags
xcrun simctl spawn booted log stream \
  --predicate 'process == "worldwidewaves" && subsystem contains "RootController"' \
  --level debug

# Save logs to file
xcrun simctl spawn booted log stream \
  --predicate 'process == "worldwidewaves"' > ios_debug.log
```

### Debug Kotlin Code from Xcode

1. Set breakpoints in Swift files
2. When breakpoint hits, examine Kotlin stack:

   ```swift
   po exception  // View Kotlin exception details
   po Thread.callStackSymbols  // View full stack trace
   ```

3. Log Kotlin state from Swift:

   ```swift
   NSLog("Event count: \(KotlinEvents.shared.count)")
   ```

### Common Log Patterns

**Platform initialization**:

```
[SceneDelegate] 📦 Installing platform...
[Platform_ios] 🔧 Initializing Koin...
[Platform_ios] 🎨 Initializing MokoRes...
[SceneDelegate] ✅ Platform installation complete
```

**ViewController lifecycle**:

```
[RootController] >>> ENTERING IOS MAIN VIEW CONTROLLER
[WWWMainActivity] 🎬 Activity created
[WWWMainActivity] ▶️ Activity started
```

**Position updates**:

```
[PositionManager] 📍 Position updated: (48.8566, 2.3522) accuracy: 10.0m
[PositionObserver] 📏 Distance to event: 1234m
```

### Debugging Deadlocks

If app freezes on launch:

```bash
# 1. Get stack trace
xcrun simctl spawn booted lldb --attach-name worldwidewaves
(lldb) thread backtrace all

# 2. Look for blocked threads
# Common patterns:
# - "KoinComponent.inject" → DI violation
# - "runBlocking" → Coroutine violation
# - "Dispatchers.Main" → Main thread violation

# 3. Run verification
./scripts/dev/verification/verify-ios-safety.sh

# 4. Check recent changes
git diff HEAD~1 shared/src/commonMain
```

### Xcode Debugging Tools

**Memory Graph**: Cmd+Shift+M

- View object retention
- Detect memory leaks
- Inspect Kotlin/Native objects

**View Hierarchy**: Cmd+Shift+V (when app running)

- Inspect Compose UI structure
- View layout bounds
- Debug touch issues

**Instruments** (Cmd+I):

- Time Profiler: Find performance bottlenecks
- Allocations: Track memory usage
- Leaks: Detect memory leaks

---

## Testing

### Unit Tests (Shared Code)

```bash
# Run all unit tests
./gradlew :shared:testDebugUnitTest

# Run specific test class
./gradlew :shared:testDebugUnitTest --tests "*.PositionManagerTest"

# Run with iOS target
./gradlew :shared:iosSimulatorArm64Test
```

### iOS UI Tests (Pending)

```bash
# Run iOS UI tests from Xcode
cd iosApp
xcodebuild test \
  -project worldwidewaves.xcodeproj \
  -scheme worldwidewaves \
  -destination 'platform=iOS Simulator,name=iPhone 15 Pro'
```

**Status**: iOS UI tests are in development.

### Manual Testing Checklist

- [ ] App launches without freezing
- [ ] Main screen displays events list
- [ ] Tapping event opens event detail screen
- [ ] Map displays user location (blue dot)
- [ ] Wave animation plays at event time
- [ ] Sound plays during wave (if enabled)
- [ ] Deep links work: `worldwidewaves://event?id=123`
- [ ] App returns from background without crash
- [ ] Location permission prompt appears
- [ ] Settings screen opens and closes

---

## Common Issues and Solutions

### Issue 0: Xcode GUID Conflict Error (RECURRING) ⚠️

**Symptoms**:

```
Could not compute dependency graph: unable to load transferred PIF:
The workspace contains multiple references with the same GUID 'PACKAGE:...'
```

**Causes**:

- Swift Package Manager cache corruption
- Xcode DerivedData corruption
- Multiple concurrent Xcode operations
- Git operations while Xcode is open
- Xcode crashes during package resolution

**Prevention** (Run regularly):

```bash
# Clean Xcode state (recommended before important work)
./scripts/dev/build/clean_xcode.sh

# Or manual cleanup:
rm -rf ~/Library/Developer/Xcode/DerivedData/worldwidewaves-*
rm -rf iosApp/build
rm -rf iosApp/.swiftpm
rm -rf iosApp/worldwidewaves.xcodeproj/project.xcworkspace/xcshareddata/swiftpm
```

**When to Run Cleanup**:

- ✅ **Before opening Xcode after git pull/merge**
- ✅ **After Xcode crashes**
- ✅ **When seeing GUID errors**
- ✅ **After major dependency changes**
- ✅ **Before important builds/releases**
- ⚠️ **After any Swift Package Manager updates**

**Immediate Fix** (if error occurs):

1. Close Xcode completely (Cmd+Q)
2. Run `./scripts/dev/build/clean_xcode.sh`
3. Open Xcode
4. Let it re-resolve packages (File → Packages → Resolve Package Versions)
5. Build (Cmd+B)

**Best Practices**:

- Close Xcode before git operations (pull, merge, rebase)
- Don't interrupt Swift Package Manager resolution
- Run cleanup script weekly during active development
- Don't commit xcuserdata or .swiftpm directories

---

### Issue 1: App Freezes on Launch

**Symptoms**: White screen, unresponsive UI, no logs

**Causes**:

1. DI violation (object : KoinComponent in Composable)
2. Coroutine launch in init{}
3. runBlocking before ComposeUIViewController

**Solutions**:

```bash
# Run verification
./scripts/dev/verification/verify-ios-safety.sh

# Check recent changes
git diff HEAD~1 shared/src/commonMain

# Look for violations
rg -B10 "object.*KoinComponent" shared/src/commonMain --type kotlin \
  | rg "@Composable"
```

### Issue 2: Koin Initialization Failure

**Symptoms**: `KoinApplicationNotStartedException` in logs

**Causes**:

- `doInitPlatform()` not called
- Called on wrong thread
- Failed silently (swallowed exception)

**Solutions**:

```swift
// SceneDelegate.swift - Add logging
do {
    _ = try Platform_iosKt.doInitPlatform()
    NSLog("✅ Koin initialized")
} catch let e as NSError {
    NSLog("❌ Koin failed: \(e.localizedDescription)")
    NSLog("❌ Details: \(e)")
}
```

### Issue 3: Resources Not Loading

**Symptoms**: Missing images, strings, or fonts

**Causes**:

- MokoRes bundle not initialized
- Wrong resource path
- Resource not included in build

**Solutions**:

```bash
# Verify resources included
ls shared/src/commonMain/resources/

# Check initialization
grep "MokoRes" iosApp/worldwidewaves/SceneDelegate.swift

# Rebuild framework
./gradlew :shared:embedAndSignAppleFrameworkForXcode
```

### Issue 4: Map Not Displaying

**Symptoms**: Blank map area, no tiles loading

**Causes**:

- Native map provider not registered
- MapLibre initialization failed
- Missing map style URL

**Solutions**:

```swift
// Verify provider registration in SceneDelegate
NativeMapViewProviderRegistrationKt.registerNativeMapViewProvider(
    provider: SwiftNativeMapViewProvider()
)

// Check logs for map errors
xcrun simctl spawn booted log stream \
  --predicate 'process == "worldwidewaves" && subsystem contains "Map"'
```

### Issue 5: Compose UI Crash

**Symptoms**: Crash with "SKIKO" or "Metal" in stack trace

**Causes**:

- SKIKO_RENDER_API not set
- Metal not available on device
- Compose version incompatibility

**Solutions**:

```swift
// Verify SKIKO configuration in SceneDelegate
setenv("SKIKO_RENDER_API", "METAL", 1)

// Check Info.plist
// CADisableMinimumFrameDurationOnPhone = YES

// Update Compose version in build.gradle.kts
```

### Issue 6: Deep Links Not Working

**Symptoms**: URL opens browser instead of app

**Causes**:

- URL scheme not registered in Info.plist
- AppDelegate not handling URLs
- Wrong URL format

**Solutions**:

```xml
<!-- Verify Info.plist -->
<key>CFBundleURLTypes</key>
<array>
    <dict>
        <key>CFBundleURLSchemes</key>
        <array>
            <string>worldwidewaves</string>
        </array>
    </dict>
</array>
```

```bash
# Test deep link
xcrun simctl openurl booted "worldwidewaves://event?id=test123"

# Check logs
xcrun simctl spawn booted log stream \
  --predicate 'process == "worldwidewaves" && subsystem contains "AppDelegate"'
```

### Issue 7: Map Gestures Not Working

**Symptoms**: User cannot pan/zoom map, gestures appear disabled

**Root Cause**: Using wrong MLNMapView property names that don't exist

**Incorrect Names** (silently fail in Swift):

- `allowsZooming` - doesn't exist
- `allowsScrolling` - doesn't exist
- `allowsRotating` - doesn't exist
- `allowsTilting` - doesn't exist

**Correct Names** (from MLNMapView.h):

- `isZoomEnabled` - controls zoom gestures
- `isScrollEnabled` - controls pan gestures
- `isRotateEnabled` - controls rotation gestures
- `isPitchEnabled` - controls tilt gestures

**Solution**:

```swift
// ✅ CORRECT - these properties actually exist
mapView.isZoomEnabled = true
mapView.isScrollEnabled = true
mapView.isRotateEnabled = false
mapView.isPitchEnabled = false

// ❌ WRONG - these silently fail
mapView.allowsZooming = true  // Property doesn't exist!
mapView.allowsScrolling = true  // Property doesn't exist!
```

**Verification**:
Check `shouldChangeFrom` delegate is receiving gesture events:

- `reason=4` - MLNCameraChangeReasonGesturePan (pan gestures)
- `reason=8` - MLNCameraChangeReasonGesturePinch (zoom gestures)

If you only see `reason=1` (programmatic), gestures are not enabled.

**Files to Check**:

- `EventMapView.swift:66-97` - Gesture configuration
- `MapLibreViewWrapper.swift` - setGesturesEnabled callback

### Issue 8: User Can Pan Outside Event Area

**Symptoms**: Map allows panning beyond event boundaries

**Cause**: Camera bounds validation not enforced or using wrong validation approach

**Current Working Solution**:
iOS uses camera center validation (not viewport bounds):

```swift
public func mapView(_ mapView: MLNMapView, shouldChangeFrom oldCamera: MLNMapCamera,
                   to newCamera: MLNMapCamera, reason: MLNCameraChangeReason) -> Bool {
    guard let bounds = currentConstraintBounds else { return true }

    // Validate camera center is within constraint bounds
    let center = newCamera.centerCoordinate
    if center.latitude < bounds.sw.latitude || center.latitude > bounds.ne.latitude ||
       center.longitude < bounds.sw.longitude || center.longitude > bounds.ne.longitude {
        return false  // Reject gesture
    }
    return true
}
```

**Why Camera Center (Not Viewport)**:

- Replicates Android `setLatLngBoundsForCameraTarget()` behavior
- MapLibre natively clamps viewport edges to tile boundaries
- Simpler validation logic
- Allows user to touch map edges without rejection

**Files**:

- `MapLibreViewWrapper.swift:1166-1287` - shouldChangeFrom delegate

### Issue 9: Min Zoom Too Restrictive

**Symptoms**: User cannot see full event height at minimum zoom

**Cause**: Incorrect tile size in min zoom calculation

**Current Implementation** (512px tiles):

```swift
// iOS MapLibre uses 512px tiles (not 256px)
let zoomForHeight = log2((screenHeight * 360.0) / (boundsHeight * 512.0))
let zoomForWidth = log2((screenWidth * 360.0 * cos(latRadians)) / (boundsWidth * 512.0))
let minZoom = min(zoomForHeight, zoomForWidth)
```

**Result**:

- Min zoom slightly higher than theoretical (uses 512px assumption)
- User can see ~90-95% of event height at minZoom
- Acceptable per user decision (prevents over-zooming out)

**Trade-off**: Prioritizes preventing excessive zoom-out over perfect height visibility

**Files**:

- `MapLibreViewWrapper.swift:519-571` - setBoundsForCameraTarget

---

## Advanced Topics

### Adding a New iOS Screen

#### Step 1: Create Shared Compose UI

```kotlin
// shared/src/commonMain/.../ui/screens/NewFeatureScreen.kt
@Composable
fun NewFeatureScreen(
    featureId: String,
    onBack: () -> Unit,
    clock: IClock = getIOSSafeClock()  // ✅ Use IOSSafeDI
) {
    Column {
        Text("Feature: $featureId")
        Button(onClick = onBack) {
            Text("Back")
        }
    }
}
```

#### Step 2: Create Activity Wrapper

```kotlin
// shared/src/commonMain/.../ui/activities/WWWNewFeatureActivity.kt
class WWWNewFeatureActivity(
    private val featureId: String,
    private val platformEnabler: PlatformEnabler
) : WWWActivity() {

    @Composable
    fun Draw(onFinish: () -> Unit) {
        NewFeatureScreen(
            featureId = featureId,
            onBack = onFinish
        )
    }
}
```

#### Step 3: Add iOS ViewController Factory

```kotlin
// shared/src/iosMain/.../ui/RootController.kt
@Throws(Throwable::class)
fun makeNewFeatureViewController(featureId: String): UIViewController =
    makeComposeVC("IOS NEW FEATURE VIEW CONTROLLER") { finish ->
        val enabler = diEnabler()

        val host = remember(featureId) {
            WWWNewFeatureActivity(
                featureId = featureId,
                platformEnabler = enabler
            )
        }

        bindIosLifecycle(host)

        host.Draw(onFinish = finish)
    }
```

#### Step 4: Add iOS Routing

```swift
// iosApp/worldwidewaves/SceneDelegate.swift
func viewController(for url: URL) -> UIViewController? {
    // ... existing routes ...

    case "feature":
        guard let id = id else {
            NSLog("[\(tag)] ❌ feature route missing id")
            return nil
        }
        do {
            let vc = try RootControllerKt.makeNewFeatureViewController(featureId: id)
            NSLog("[\(tag)] ✅ routed -> NewFeatureViewController(id=\(id))")
            return vc
        } catch let e as NSError {
            NSLog("[\(tag)] ❌ Failed to create feature VC: \(e)")
            return nil
        }
}
```

#### Step 5: Test

```bash
# Open deep link
xcrun simctl openurl booted "worldwidewaves://feature?id=test123"

# Verify logs
xcrun simctl spawn booted log stream \
  --predicate 'process == "worldwidewaves"' | grep "FEATURE"
```

### Performance Optimization

#### Reduce Compose Recomposition

```kotlin
// ✅ GOOD: Stable parameters
@Composable
fun EventItem(event: IWWWEvent) {  // Interface is stable
    Text(event.title)
}

// ❌ BAD: Unstable parameters
@Composable
fun EventItem(event: WWWEvent) {  // Data class is unstable
    Text(event.title)
}
```

#### Optimize Position Updates

```kotlin
// ✅ GOOD: Debounced updates
positionManager.positionFlow
    .debounce(100.milliseconds)
    .distinctUntilChanged()
    .collect { position ->
        updateUI(position)
    }

// ❌ BAD: Every GPS update
positionManager.positionFlow.collect { position ->
    updateUI(position)
}
```

#### Reduce Framework Size

```bash
# Strip debug symbols (release builds)
./gradlew :shared:linkReleaseFrameworkIosArm64

# Check framework size
du -sh shared/build/xcode-frameworks/Release/iphoneos/shared.framework
```

### Memory Management

#### Avoid Retain Cycles

```kotlin
// ✅ GOOD: No retain cycles
@Composable
fun EventScreen(viewModel: EventViewModel) {
    val events by viewModel.events.collectAsState()
}

// ❌ BAD: Potential retain cycle
@Composable
fun EventScreen() {
    val viewModel = remember { EventViewModel() }
    // ViewModel may hold reference to Composable
}
```

#### Cancel Coroutines Properly

```kotlin
@Composable
fun EventScreen() {
    val scope = rememberCoroutineScope()

    DisposableEffect(Unit) {
        val job = scope.launch {
            observeEvents()
        }

        onDispose {
            job.cancel()  // ✅ Cleanup
        }
    }
}
```

---

## Location Marker Architecture

### Platform Differences (Android vs iOS)

WorldWideWaves uses different approaches for displaying user location markers on each platform due to integration requirements with PositionManager.

#### Android: Native LocationComponent

**Implementation**: Uses MapLibre's built-in LocationComponent

- **Integration**: LocationEngineProxy bridges PositionManager to native LocationComponent
- **Updates**: Automatic (no manual coordinate updates needed)
- **Rendering**: GPU-accelerated pulse animation
- **Performance**: High (native MapLibre rendering)
- **Code**: `AndroidEventMap.kt:setupMapLocationComponent()`

**Architecture**:

```
PositionManager → LocationEngineProxy → LocationComponent → MapLibre Native Rendering
```

**Configuration** (`AndroidMapLibreAdapter.kt`):

```kotlin
locationComponent.apply {
    isLocationComponentEnabled = true
    locationComponentOptions = LocationComponentOptions.builder(context)
        .pulseEnabled(true)
        .pulseColor(Color.RED)
        .foregroundTintColor(Color.BLACK)
        .build()
}
```

#### iOS: Custom MLNPointAnnotation

**Implementation**: Uses custom annotation with manual updates

- **Integration**: Direct callback from PositionManager via MapWrapperRegistry
- **Updates**: Manual (explicit `setUserPosition()` calls)
- **Rendering**: CoreAnimation-based pulse (CPU)
- **Performance**: Good (optimized for iOS)
- **Code**: `MapLibreViewWrapper.swift:502-577`

**Architecture**:

```
PositionManager → IosMapLibreAdapter → MapWrapperRegistry → setUserPosition callback → MLNPointAnnotation
```

**Configuration** (`MapLibreViewWrapper.swift:768-808`):

```swift
// Red pulse circle (40x40pt)
pulseView.backgroundColor = UIColor.systemRed.withAlphaComponent(0.3)

// Black center dot (10x10pt with white border)
dotView.backgroundColor = UIColor.black
dotView.layer.borderWidth = 2
dotView.layer.borderColor = UIColor.white.cgColor

// Pulse animation (1.5s, scale 1.0→1.3, infinite)
let pulseAnimation = CABasicAnimation(keyPath: "transform.scale")
pulseAnimation.duration = 1.5
pulseAnimation.fromValue = 1.0
pulseAnimation.toValue = 1.3
pulseAnimation.timingFunction = .easeInEaseOut
pulseAnimation.autoreverses = true
pulseAnimation.repeatCount = .infinity
```

### Why Different Architectures?

**Android**: Native LocationComponent is tightly integrated with MapLibre's location engine

- LocationEngineProxy allows custom position sources while maintaining native rendering
- Best performance with minimal code

**iOS**: Native location component expects CLLocationManager

- MapLibre iOS doesn't provide equivalent LocationEngineProxy pattern
- Custom annotation provides full control over position updates from PositionManager
- More code but better integration with reactive position flow

### Visual Appearance (Both Platforms)

Both platforms render the same visual appearance:

- **Pulse**: Red circle (40x40pt/dp), opacity 30%, scales 1.0→1.3 over 1.5s
- **Center**: Black dot (10x10pt/dp) with 2pt/dp white border
- **Effect**: Infinite pulsing animation to indicate live position

### Position Update Flow

**Android**:

```kotlin
PositionManager.positionFlow
  → LocationEngineProxy.onLocationChanged()
  → LocationComponent.forceLocationUpdate()
  → MapLibre native rendering (automatic)
```

**iOS**:

```kotlin
PositionManager.positionFlow
  → IosMapLibreAdapter.setUserPosition()
  → MapWrapperRegistry.setUserPositionOnWrapper()
  → MapLibreViewWrapper.setUserPosition() callback
  → updateUserLocationMarker() (manual annotation update)
  → MLNPointAnnotation.coordinate = newPosition
```

### Trade-offs

| Aspect | Android | iOS |
|--------|---------|-----|
| Code complexity | Low (native API) | Medium (manual updates) |
| Performance | Excellent (GPU) | Good (CPU animation) |
| Control | Limited (MapLibre API) | Full (custom rendering) |
| PositionManager integration | Via proxy | Direct callbacks |
| Memory usage | Low (single component) | Low (single annotation) |
| Maintenance | Easy (stable API) | Medium (manual coordination) |

### Future Considerations

**If iOS MapLibre adds LocationEngineProxy**:

- Could migrate to native LocationComponent
- Would reduce code by ~100 lines
- Would improve performance slightly (GPU vs CPU animation)
- Current architecture would remain compatible (adapter pattern)

**Current Status**: Custom annotation approach is stable, tested, and production-ready.

---

## References

### Essential Documentation

- [CLAUDE.md](./CLAUDE.md) - Main development guide
- [docs/ios/ios-violation-tracker.md](docs/ios/ios-violation-tracker.md) - Historical violations
- [docs/ios/ios-success-state.md](docs/ios/ios-success-state.md) - Success criteria
- [docs/ios/ios-debugging-guide.md](docs/ios/ios-debugging-guide.md) - Advanced debugging
- [docs/ios/ios-map-implementation-status.md](docs/ios/ios-map-implementation-status.md) - Map status
- [docs/ios/cinterop-memory-safety-patterns.md](docs/ios/cinterop-memory-safety-patterns.md) - Memory pinning & struct access
- [docs/ios/swift-kotlin-bridging-guide.md](docs/ios/swift-kotlin-bridging-guide.md) - Type conversions & protocols
- [docs/ios/platform-api-usage-guide.md](docs/ios/platform-api-usage-guide.md) - UIKit/Foundation/CoreLocation

### External Resources

- [Kotlin Multiplatform Mobile](https://kotlinlang.org/docs/multiplatform-mobile-getting-started.html)
- [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/)
- [Kotlin/Native Memory Management](https://kotlinlang.org/docs/native-memory-manager.html)
- [Koin Documentation](https://insert-koin.io/)

---

**Version**: 1.0
**Status**: Production Ready (All critical violations fixed)
**Maintainer**: WorldWideWaves Development Team
