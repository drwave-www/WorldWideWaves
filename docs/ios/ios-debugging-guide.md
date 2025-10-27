# iOS Debugging Guide - Step by Step WWWMainActivity Implementation

## 🎯 Critical First Step: Identify the Correct App

### Problem
Multiple iOS apps with different bundle IDs can exist in simulator:
- `com.worldwidewaves.iosApp` (often broken/crashes)
- `com.worldwidewaves.WorldWideWavesDrWaves` (may be working)
- `com.worldwidewaves.shared.Shared` (framework test)

### Solution
**ALWAYS verify which app you're testing**:

```bash
# 1. List all WorldWideWaves apps in simulator
xcrun simctl listapps DEVICE_ID | grep worldwidewaves

# 2. Identify the bundle ID from build output
cd iosApp && xcodebuild -showBuildSettings -scheme SCHEME | grep PRODUCT_BUNDLE_IDENTIFIER

# 3. Launch the CORRECT app (match bundle ID from build)
xcrun simctl launch DEVICE_ID CORRECT_BUNDLE_ID

# 4. Verify app is actually running and not crashing
sleep 5 && xcrun simctl io DEVICE_ID screenshot test.png
```

## 📱 iOS App Architecture Debugging

### Step 1: Verify Basic iOS Project Setup

```bash
# Check iOS targets are configured
grep -A 5 "listOf.*ios" shared/build.gradle.kts

# Expected output:
# iosX64()
# iosArm64()
# iosSimulatorArm64()

# Check framework configuration
grep -A 10 "binaries.framework" shared/build.gradle.kts

# Verify bundle ID configuration
grep -E "BUNDLE_ID|bundleId" iosApp/project.yml
```

### Step 2: Monitor Complete Initialization Flow

**Essential Logging Points:**
```swift
// In ContentView.swift
NSLog("📱 ContentView: makeUIViewController called")
NSLog("📱 ContentView: About to initialize Koin DI")
HelperKt.doInitKoin()
NSLog("📱 ContentView: doInitKoin completed successfully")
NSLog("📱 ContentView: About to call MainViewController")
let controller = MainViewControllerKt.MainViewController()
NSLog("📱 ContentView: MainViewController created successfully")
```

**Log Monitoring Command:**
```bash
xcrun simctl spawn DEVICE_ID log show --style compact \
  --predicate 'process == "APP_NAME"' --info --debug \
  --start 'YYYY-MM-DD HH:MM:SS' | \
  grep -E "(ContentView|MainViewController|WWWMainActivity|🎯)"
```

### Step 3: Identify Crash Patterns

**Pattern 1: Immediate Crash (return to home screen)**
- **Symptom**: White screen → home screen immediately
- **Logs**: Missing MainViewController or ComposeUIViewController logs
- **Cause**: Usually coroutine deadlocks or Android-only dependencies

**Pattern 2: Infrastructure Crash (SIGABRT)**
- **Symptom**: App launches but crashes during initialization
- **Logs**: Shows initialization but terminates with exception
- **Debug**: Check crash reports in `~/Library/Logs/DiagnosticReports/`

**Pattern 3: Stable But No UI (splash/loading)**
- **Symptom**: App stays open but shows white/empty content
- **Logs**: Complete initialization but no UI rendering
- **Cause**: Resource loading issues or splash screen logic

## 🔧 Systematic Dependency Debugging

### Phase 1: Identify Android Leakage

```bash
# Find Android-only imports in commonMain
find shared/src/commonMain -name "*.kt" -exec grep -Hn \
  "androidx\.lifecycle\|androidx\.activity\|android\." {} \;

# Check dependency tree for androidx artifacts in commonMain
./gradlew :shared:dependencies --configuration commonMainImplementationDependenciesMetadata | \
  grep -E "(androidx\.lifecycle|androidx\.activity)"

# Expected: NONE found in commonMain/iOS configurations
```

### Phase 2: Apply Targeted Exclusions

```kotlin
// In shared/build.gradle.kts
configurations.configureEach {
    if (name.contains("commonMain", ignoreCase = true) || name.contains("ios", ignoreCase = true)) {
        // Block specific problematic modules
        exclude(group = "org.jetbrains.androidx.lifecycle", module = "lifecycle-runtime-compose")
        exclude(group = "androidx.activity")
        exclude(group = "androidx.compose.ui", module = "ui-tooling")
    }
}
```

### Phase 3: Fix Coroutine Deadlocks

**Hunt for Blocking Operations:**
```bash
# Search for blocking patterns
find shared/src/commonMain -name "*.kt" -exec grep -Hn \
  "runBlocking\|\.join()\|Dispatchers\.Main.*init" {} \;
```

**Critical Fix Pattern:**
```kotlin
// BAD (causes deadlock):
private fun updateCache() {
    runBlocking {
        mutex.withLock { /* ... */ }
    }
}

// GOOD (non-blocking):
private suspend fun updateCache() {
    mutex.withLock { /* ... */ }
}
// Call with: backgroundScope.launch { updateCache() }
```

## 🧪 Systematic Testing Approach

### Test Progression

1. **Empty Compose Test**
```kotlin
fun MainViewController(): UIViewController = ComposeUIViewController { }
```

2. **Basic Text Test**
```kotlin
fun MainViewController(): UIViewController = ComposeUIViewController {
    androidx.compose.material3.Text("Hello iOS!")
}
```

3. **WWWMainActivity Integration**
```kotlin
fun MainViewController(): UIViewController = ComposeUIViewController {
    WWWMainActivity(IOSPlatformEnabler(), showSplash = false).Draw()
}
```

### Log Verification at Each Step

```bash
# Launch app and capture process ID
APP_PID=$(xcrun simctl launch DEVICE_ID BUNDLE_ID)

# Monitor logs for initialization sequence
xcrun simctl spawn DEVICE_ID log show --style compact \
  --predicate "processID == $APP_PID" --info --debug | \
  grep -E "(🎯|ContentView|MainViewController|WWWMainActivity)"

# Expected successful sequence:
# ContentView: makeUIViewController called
# ContentView: doInitKoin completed successfully
# MainViewController: Creating iOS main view controller
# WWWMainActivity: === INITIALIZING WWWMainActivity ===
# WWWMainActivity: Events loading completed
```

## 🚨 Crash Report Analysis

### Get Symbolicated Crash Reports

```bash
# Enable symbolication in shared/build.gradle.kts
targets.withType(org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget::class) {
    binaries.all {
        freeCompilerArgs += listOf("-Xbinary=sourceInfoType=libbacktrace")
    }
}

# Find crash reports
find ~/Library/Logs/DiagnosticReports -name "*APP_NAME*" -newermt 'YYYY-MM-DD HH:MM:SS'

# Analyze crash signature
grep -A 20 -B 5 "terminateWithUnhandledException\|BlockingCoroutine\|runBlocking" CRASH_REPORT.ips
```

### Crash Signature Patterns

**Coroutine Deadlock:**
```
"symbol":"kfun:kotlinx.coroutines.BlockingCoroutine.joinBlocking#internal"
"sourceFile":"Builders.kt","sourceLine":137
```

**Android Dependency Conflict:**
```
"symbol":"androidx.lifecycle.LifecycleOwner"
"exception":"NoClassDefFoundError"
```

## 📦 Resource Loading Debugging

### Verify Resource Packaging

```bash
# Check if events.json is in Shared.framework
find FRAMEWORK_PATH/Shared.framework -name "*events*"

# Expected path:
# Shared.framework/WorldWideWaves:shared.bundle/Contents/Resources/compose-resources/.../files/events.json

# If missing, check Gradle resource tasks
./gradlew :shared:copyResourcesDebugFrameworkIosSimulatorArm64
```

### Resource Loading Logs

```bash
# Monitor resource loading
xcrun simctl spawn DEVICE_ID log show --style compact \
  --predicate 'process == "APP_NAME"' | \
  grep -E "(EventsConfiguration|readBytes|Resource|LOAD)"

# Successful pattern:
# EventsConfigurationProvider: Successfully read XXXXX bytes
# EventsConfigurationProvider: === EVENTS CONFIGURATION LOAD SUCCESSFUL ===

# Failure pattern:
# EventsConfigurationProvider: === EVENTS CONFIGURATION LOAD FAILED ===
# Exception type: MissingResourceException
```

## 🎯 WWWMainActivity Step-by-Step Integration

### Phase 1: Verify Basic Infrastructure

1. **Test Simple MainViewController**
```kotlin
fun MainViewController(): UIViewController = ComposeUIViewController {
    androidx.compose.material3.Text("iOS Working!")
}
```

2. **Verify Logs Appear**
```bash
# Should see in logs:
# ContentView: MainViewController created successfully
```

3. **Add Basic Koin Test**
```kotlin
fun MainViewController(): UIViewController = ComposeUIViewController {
    val platform: WWWPlatform by inject()
    androidx.compose.material3.Text("Koin works: ${platform.name()}")
}
```

### Phase 2: Add Events Loading

1. **Test Events Loading Without UI**
```kotlin
fun MainViewController(): UIViewController = ComposeUIViewController {
    val events: WWWEvents by inject()
    var eventCount by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        events.loadEvents(onTermination = {
            eventCount = events.list().size
        })
    }

    androidx.compose.material3.Text("Events: $eventCount")
}
```

2. **Monitor Events Loading**
```bash
# Should see in logs:
# WWWEvents.loadEventsJob: === STARTING loadEventsJob() ===
# EventsConfigurationProvider: Successfully read XXXXX bytes
# WWWEvents.loadEventsJob: Successfully decoded XX events
```

### Phase 3: Full WWWMainActivity Integration

1. **Test WWWMainActivity Creation**
```kotlin
fun MainViewController(): UIViewController = ComposeUIViewController {
    val mainActivity = WWWMainActivity(IOSPlatformEnabler(), showSplash = false)
    mainActivity.Draw()
}
```

2. **Monitor Full Initialization**
```bash
# Expected complete sequence:
# WWWMainActivity: === INITIALIZING WWWMainActivity ===
# WWWMainActivity: Events loading completed
# WWWMainActivity: Splash conditions met, dismissing splash screen
```

## 🏁 Success Criteria

### Working App Indicators
- ✅ App launches and stays open (no return to home screen)
- ✅ Navigation structure visible (back arrow + app title)
- ✅ Events list appears with real event names
- ✅ No crashes during interaction

### Final Verification
```bash
# Take screenshot after 10 seconds to ensure stability
sleep 10 && xcrun simctl io DEVICE_ID screenshot final_success.png

# Check for events in UI
# Expected: List showing Paris, Rio de Janeiro, United States, etc.
```

## 🚨 Common Pitfalls

1. **Testing Wrong App**: Always verify bundle ID matches build output
2. **Cached Builds**: Clear DerivedData between major changes
3. **Resource Path Mismatches**: Ensure compose.resources config matches imports
4. **Coroutine Deadlocks**: Never use runBlocking in init blocks or callbacks
5. **Android Dependencies**: Remove androidx.lifecycle from commonMain completely

## 🔄 Iterative Debugging Approach

1. Start with simplest possible Compose
2. Add complexity incrementally
3. Test at each step with proper logging
4. Commit working states as checkpoints
5. Never make multiple changes without testing

**Key**: Always verify you're testing the correct app at each step!