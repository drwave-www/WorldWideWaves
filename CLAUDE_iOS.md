# WorldWideWaves - iOS Development Guide

> **Parent Documentation**: [CLAUDE.md](./CLAUDE.md)
> **Status**: ✅ STABLE (October 2025) | **Bundle**: com.worldwidewaves

---

## Table of Contents

1. [Quick Start](#quick-start)
2. [Architecture Overview](#architecture-overview)
3. [iOS Deadlock Prevention (CRITICAL)](#ios-deadlock-prevention-critical)
4. [Setup and Configuration](#setup-and-configuration)
5. [Building and Running](#building-and-running)
6. [Debugging Guide](#debugging-guide)
7. [Testing](#testing)
8. [Common Issues and Solutions](#common-issues-and-solutions)
9. [Advanced Topics](#advanced-topics)

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
cd /Users/ldiasdasilva/StudioProjects/WorldWideWaves

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
./scripts/verify-ios-safety.sh

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
./scripts/verify-ios-safety.sh
# Exit code 0 = safe
# Exit code 1 = violations found
```

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

**SceneDelegate.swift** (lines 75-90):

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

    // 2. Install Kotlin/Native hooks
    KnHookKt.installKNHook()

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
./scripts/verify-ios-safety.sh

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

### Issue 1: App Freezes on Launch

**Symptoms**: White screen, unresponsive UI, no logs

**Causes**:
1. DI violation (object : KoinComponent in Composable)
2. Coroutine launch in init{}
3. runBlocking before ComposeUIViewController

**Solutions**:
```bash
# Run verification
./scripts/verify-ios-safety.sh

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

        BindIosLifecycle(host)

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

## References

### Essential Documentation
- [CLAUDE.md](./CLAUDE.md) - Main development guide
- [docs/iOS_VIOLATION_TRACKER.md](docs/iOS_VIOLATION_TRACKER.md) - Historical violations
- [docs/iOS_SUCCESS_STATE.md](docs/iOS_SUCCESS_STATE.md) - Success criteria
- [docs/iOS_DEBUGGING_GUIDE.md](docs/iOS_DEBUGGING_GUIDE.md) - Advanced debugging
- [iOS_MAP_IMPLEMENTATION_STATUS.md](docs/ios/IOS_MAP_IMPLEMENTATION_STATUS.md) - Map status

### External Resources
- [Kotlin Multiplatform Mobile](https://kotlinlang.org/docs/multiplatform-mobile-getting-started.html)
- [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/)
- [Kotlin/Native Memory Management](https://kotlinlang.org/docs/native-memory-manager.html)
- [Koin Documentation](https://insert-koin.io/)

---

**Last Updated**: October 1, 2025
**Version**: 1.0
**Status**: Production Ready (All critical violations fixed)
**Maintainer**: WorldWideWaves Development Team
