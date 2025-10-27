# WorldWideWaves Logging Guide

> **Last Updated**: October 27, 2025
> **Version**: 1.0
> **Status**: Production

## Table of Contents

1. [Overview](#overview)
2. [Logging Architecture](#logging-architecture)
3. [Log Levels and Usage Guidelines](#log-levels-and-usage-guidelines)
4. [Tag Naming Conventions](#tag-naming-conventions)
5. [What to Log (and When)](#what-to-log-and-when)
6. [Performance Considerations](#performance-considerations)
7. [Security and Privacy Guidelines](#security-and-privacy-guidelines)
8. [Platform-Specific Behavior](#platform-specific-behavior)
9. [Good vs Bad Logging Examples](#good-vs-bad-logging-examples)
10. [Structured Logging Patterns](#structured-logging-patterns)
11. [Testing Logging Output](#testing-logging-output)
12. [Troubleshooting and Debugging](#troubleshooting-and-debugging)

---

## Overview

WorldWideWaves uses a production-ready logging system that combines:
- **Napier**: Cross-platform logging library for Kotlin Multiplatform
- **Log Wrapper**: Custom wrapper (`com.worldwidewaves.shared.utils.Log`) that respects build configuration flags
- **BuildKonfig**: Build-time feature flags to control logging in different environments

### Key Principles

1. **Performance First**: Verbose/debug logging is automatically disabled in release builds
2. **Security Always**: Never log PII, precise coordinates, or sensitive data in production
3. **Actionable Logs**: Every log should have a clear purpose (debugging, monitoring, or alerting)
4. **Cross-Platform**: Consistent logging behavior across Android and iOS
5. **Build-Time Optimization**: Zero runtime overhead for disabled log levels

---

## Logging Architecture

### System Diagram

```
┌─────────────────────────────────────────────────────────┐
│                   Application Code                       │
│                                                           │
│  Log.v("TAG", "message")  ──────────────────┐           │
│  Log.d("TAG", "message")  ──────────────────┤           │
│  Log.i("TAG", "message")  ──────────────────┤           │
│  Log.w("TAG", "message")  ──────────────────┤           │
│  Log.e("TAG", "message", throwable)  ───────┤           │
│  Log.performance("TAG", "message")  ────────┤           │
│                                              │           │
└──────────────────────────────────────────────┼───────────┘
                                               │
                                               ▼
┌─────────────────────────────────────────────────────────┐
│         Log Wrapper (utils/Log.kt)                       │
│                                                           │
│  • Checks WWWGlobals.LogConfig flags                     │
│  • Filters out disabled levels at compile time           │
│  • Adds [PERF] prefix for performance logs               │
│  • Forwards to Napier if enabled                         │
│                                                           │
└──────────────────────────────────────────────┬───────────┘
                                               │
                                               ▼
┌─────────────────────────────────────────────────────────┐
│            Napier (Cross-Platform)                       │
│                                                           │
│  • Routes logs to platform-specific outputs              │
│  • Adds timestamps and thread info                       │
│  • Handles throwable formatting                          │
│                                                           │
└──────────────────────────────────────────────┬───────────┘
                                               │
                    ┌──────────────────────────┴───────────────┐
                    │                                          │
                    ▼                                          ▼
        ┌──────────────────────┐               ┌──────────────────────┐
        │  Android Logcat       │               │  iOS Unified Logging │
        │  (android.util.Log)   │               │  (os_log)            │
        └──────────────────────┘               └──────────────────────┘
```

### Build Configuration Flags

Defined in `WWWGlobals.kt`:

```kotlin
object LogConfig {
    val ENABLE_VERBOSE_LOGGING: Boolean = BuildKonfig.ENABLE_VERBOSE_LOGGING
    val ENABLE_DEBUG_LOGGING: Boolean = BuildKonfig.ENABLE_DEBUG_LOGGING
    val ENABLE_PERFORMANCE_LOGGING: Boolean = BuildKonfig.ENABLE_PERFORMANCE_LOGGING
    val ENABLE_POSITION_TRACKING_LOGGING: Boolean = BuildKonfig.ENABLE_POSITION_TRACKING_LOGGING
}
```

**Flag Behavior by Build Type**:

| Build Type | VERBOSE | DEBUG | PERFORMANCE | POSITION_TRACKING |
|------------|---------|-------|-------------|-------------------|
| Debug      | ✅ ON   | ✅ ON | ✅ ON       | ✅ ON             |
| Release    | ❌ OFF  | ❌ OFF| ❌ OFF      | ❌ OFF            |
| Beta       | ❌ OFF  | ✅ ON | ❌ OFF      | ❌ OFF            |

---

## Log Levels and Usage Guidelines

### VERBOSE (`Log.v`)

**Purpose**: Extremely detailed debugging information
**Enabled**: Debug builds only
**Performance Impact**: HIGH (use sparingly)

**Use for**:
- Step-by-step flow tracking
- Detailed state dumps
- High-frequency events (position updates, animation frames)
- Internal component lifecycle

**Example**:
```kotlin
Log.v("EventObserver", "Starting unified observation for event ${event.id}")
Log.v("EventObserver", "Simulation change detected for event ${event.id}")
Log.v("CameraController", "Gesture intercepted: viewport would exceed bounds, clamping camera")
```

**AVOID**:
- Production builds (automatically disabled)
- Hot paths without performance flags
- Logging every iteration of tight loops

---

### DEBUG (`Log.d`)

**Purpose**: General debugging information
**Enabled**: Debug and Beta builds
**Performance Impact**: MEDIUM

**Use for**:
- Function entry/exit points
- Important state transitions
- Parameter validation results
- Cache hits/misses
- Configuration changes

**Example**:
```kotlin
Log.d("AudioTest", "Loading MIDI file: ${FileSystem.CHOREOGRAPHIES_SOUND_MIDIFILE}")
Log.d("AudioTest", "Playing test note: A4 (440Hz)")
Log.d("MapViewModel", "Camera position changed: zoom=$zoom, center=$center")
```

**AVOID**:
- Sensitive data (credentials, tokens)
- Precise user coordinates
- High-frequency repeated messages

---

### INFO (`Log.i`)

**Purpose**: Important application state changes and user actions
**Enabled**: ALL builds (production-safe)
**Performance Impact**: LOW

**Use for**:
- User-initiated actions (button clicks, navigation)
- Major state transitions (event started, wave completed)
- Configuration updates
- Successful API calls
- Feature flag changes

**Example**:
```kotlin
Log.i("EventScreen", "User joined wave for event ${event.id}")
Log.i("CameraController", "Setting up preventive gesture constraints (one-time setup)")
Log.i("MapDownload", "Map tiles downloaded successfully for city: ${city.name}")
```

**AVOID**:
- PII or location data
- Internal implementation details
- High-frequency events

---

### WARNING (`Log.w`)

**Purpose**: Recoverable errors and important warnings
**Enabled**: ALL builds (production-safe)
**Performance Impact**: LOW

**Use for**:
- Recoverable errors (fallback used)
- Deprecated API usage
- Unusual but valid states
- Resource constraints (memory warnings)
- Missing optional data

**Example**:
```kotlin
Log.w("AudioTest", "Failed to load MIDI file, creating demo track: ${e.message}")
Log.w("CameraController", "Invalid dimensions: using fallback min zoom")
Log.w("EventObserver", "Platform not available for simulation observation")
```

**Include throwable when available**:
```kotlin
Log.w("DataSync", "Sync retry scheduled", throwable = e)
```

---

### ERROR (`Log.e`)

**Purpose**: Errors that need immediate attention
**Enabled**: ALL builds (production-safe)
**Performance Impact**: LOW

**Use for**:
- API failures
- Data corruption
- Unexpected exceptions
- State inconsistencies
- Critical resource failures

**Example**:
```kotlin
Log.e("EventObserver", "State error stopping unified observation: $e")
Log.e("MapStore", "Failed to load map tiles", throwable = e)
Log.e("FirebaseSync", "Event upload failed for event ${event.id}", throwable = e)
```

**ALWAYS include throwable**:
```kotlin
// ✅ CORRECT
try {
    dangerousOperation()
} catch (e: Exception) {
    Log.e(TAG, "Operation failed", throwable = e)
}

// ❌ WRONG - missing stack trace
catch (e: Exception) {
    Log.e(TAG, "Operation failed: ${e.message}")
}
```

---

### WTF (`Log.wtf`)

**Purpose**: "What a Terrible Failure" - catastrophic errors that should never happen
**Enabled**: ALL builds (production-safe)
**Performance Impact**: LOW

**Use for**:
- Impossible conditions (should never reach this code)
- Critical invariant violations
- Data corruption that breaks app functionality
- Logic errors in production

**Example**:
```kotlin
Log.wtf("EventValidator", "Event has no waves after validation", throwable = e)
Log.wtf("StateManager", "State machine reached invalid state: $currentState")

// Defensive programming
val waveDefinition = requireNotNull(linear ?: deep ?: linearSplit) {
    Log.wtf(TAG, "Wave definition must exist after validation")
    "Wave definition must exist after validation"
}
```

---

### PERFORMANCE (`Log.performance`)

**Purpose**: High-frequency performance measurements
**Enabled**: Debug builds with ENABLE_PERFORMANCE_LOGGING flag
**Performance Impact**: VERY HIGH

**Use for**:
- Hot path measurements
- Animation frame timings
- Position update frequencies
- Map rendering metrics
- Cache performance

**Example**:
```kotlin
Log.performance("MapRender", "Frame rendered in ${duration.inWholeMilliseconds}ms")
Log.performance("PositionUpdate", "GPS update latency: ${latency}ms")
Log.performance("WaveCalculation", "Polygon generation: ${polygons.size} polygons in ${duration}ms")
```

**CRITICAL**: Use feature flags to avoid overhead:
```kotlin
if (WWWGlobals.LogConfig.ENABLE_PERFORMANCE_LOGGING) {
    val startTime = TimeSource.Monotonic.markNow()
    performOperation()
    Log.performance(TAG, "Operation completed in ${startTime.elapsedNow()}")
}
```

---

## Tag Naming Conventions

### Hierarchical Tag Structure

Use hierarchical tags for organization and filtering:

```
WWW.Layer.Component.SubComponent
```

**Examples**:
```kotlin
"WWW.Data.EventRepository"
"WWW.Domain.WaveObserver"
"WWW.UI.EventScreen"
"WWW.Map.Camera"
```

### Standard Tag Patterns

**Single-Class Tags** (most common):
```kotlin
class EventObserver {
    companion object {
        private const val TAG = "EventObserver"
    }

    fun startObservation() {
        Log.v(TAG, "Starting observation")
    }
}
```

**Layer-Specific Tags**:
```kotlin
// Data Layer
private const val TAG = "MapStore"          // Data storage
private const val TAG = "EventRepository"   // Repository pattern
private const val TAG = "FirebaseSync"      // Remote sync

// Domain Layer
private const val TAG = "WaveObserver"      // Business logic
private const val TAG = "ProgressionTracker"// Wave progression
private const val TAG = "AreaDetection"     // Geofencing

// UI Layer
private const val TAG = "EventScreen"       // Screen composables
private const val TAG = "MapViewModel"      // View models
private const val TAG = "CameraController"  // UI controllers

// Platform Layer
private const val TAG = "AndroidMapAdapter" // Platform adapters
private const val TAG = "IOSMapLibre"       // iOS implementations
```

**Test Tags**:
```kotlin
private const val TAG = "Test.WaveObserver"
private const val TAG = "Test.EventRepository"
```

### Tag Length Guidelines

- **Maximum**: 23 characters (Android Logcat limit)
- **Recommended**: 12-18 characters (readable in tools)
- **Minimum**: 4 characters (avoid single letters)

```kotlin
// ✅ GOOD
"EventObserver"      // 13 chars - descriptive
"MapCamera"          // 9 chars - clear
"WaveProgression"    // 15 chars - specific

// ❌ BAD
"E"                  // Too short - unclear
"EventObserverForWaveDetection" // 30 chars - too long
```

---

## What to Log (and When)

### Application Lifecycle Events

**ALWAYS LOG**:
```kotlin
// App launch
Log.i("App", "WorldWideWaves started - version ${BuildConfig.VERSION_NAME}")

// Background/Foreground transitions
Log.i("Lifecycle", "App entered background")
Log.i("Lifecycle", "App resumed from background")

// Termination
Log.i("App", "App terminating gracefully")
```

### User Actions

**LOG at INFO level**:
```kotlin
// Button clicks
Log.i("EventScreen", "User tapped 'Join Wave' button")

// Navigation
Log.i("Navigation", "User navigated to event detail: ${event.id}")

// Settings changes
Log.i("Settings", "User enabled dark mode")

// Gestures
Log.i("Map", "User double-tapped map to zoom")
```

### State Transitions

**LOG at DEBUG/INFO level**:
```kotlin
// Event state changes
Log.i("EventObserver", "Event ${event.id} transitioned: $oldState -> $newState")

// Wave progression
Log.i("WaveTracker", "Wave reached user position at ${timestamp}")

// Connection state
Log.i("Firebase", "Connection state: CONNECTED")
```

### Data Operations

**Successful operations** (INFO):
```kotlin
Log.i("EventRepository", "Loaded ${events.size} events from cache")
Log.i("MapDownload", "Downloaded map tiles for ${city.name}")
```

**Failed operations** (ERROR):
```kotlin
Log.e("EventRepository", "Failed to fetch events from Firebase", throwable = e)
Log.e("MapDownload", "Map download failed for ${city.name}", throwable = e)
```

**Retries** (WARNING):
```kotlin
Log.w("NetworkRetry", "API call failed, retrying (attempt ${attempt}/${maxAttempts})")
```

### Performance-Critical Paths

**Use PERFORMANCE log level**:
```kotlin
// Only when ENABLE_PERFORMANCE_LOGGING is true
if (WWWGlobals.LogConfig.ENABLE_PERFORMANCE_LOGGING) {
    val start = TimeSource.Monotonic.markNow()
    renderMapFrame()
    Log.performance("MapRender", "Frame time: ${start.elapsedNow().inWholeMilliseconds}ms")
}
```

### Error Conditions

**Recoverable errors** (WARNING):
```kotlin
Log.w("AudioPlayer", "MIDI file not found, using silent mode")
Log.w("GPS", "Location accuracy low (${accuracy}m), continuing with reduced precision")
```

**Critical errors** (ERROR):
```kotlin
Log.e("Database", "Corrupted event data detected", throwable = e)
Log.e("Auth", "Authentication token expired", throwable = e)
```

**Impossible conditions** (WTF):
```kotlin
Log.wtf("Validator", "Event passed validation but has invalid coordinates")
```

---

## Performance Considerations

### Hot Paths and Logging Overhead

**Hot paths** are code sections executed frequently (>10 times/second):
- Position updates (GPS callbacks)
- Animation frames
- Map rendering
- Touch event handlers

**Rules for Hot Paths**:

1. **Use feature flags**:
```kotlin
// ✅ CORRECT - zero overhead when disabled
if (WWWGlobals.LogConfig.ENABLE_POSITION_TRACKING_LOGGING) {
    Log.v(TAG, "Position updated: $position")
}

// ❌ WRONG - string formatting happens even if logging disabled
Log.v(TAG, "Position updated: ${position.lat}, ${position.lng}")
// ^ String interpolation executes BEFORE Log.v() checks the flag
```

2. **Avoid string concatenation in hot paths**:
```kotlin
// ✅ CORRECT - lazy evaluation
if (WWWGlobals.LogConfig.ENABLE_VERBOSE_LOGGING) {
    Log.v(TAG, "Complex calculation: ${expensiveOperation()}")
}

// ❌ WRONG - expensiveOperation() always runs
Log.v(TAG, "Complex calculation: ${expensiveOperation()}")
```

3. **Use sampling for high-frequency events**:
```kotlin
private var logCounter = 0

fun onPositionUpdate(position: Position) {
    if (WWWGlobals.LogConfig.ENABLE_POSITION_TRACKING_LOGGING) {
        if (logCounter++ % 10 == 0) {  // Log every 10th update
            Log.v(TAG, "Position: $position")
        }
    }
}
```

### Performance Logging Best Practices

**Measure with minimal overhead**:
```kotlin
// ✅ CORRECT - measurement inside flag check
if (WWWGlobals.LogConfig.ENABLE_PERFORMANCE_LOGGING) {
    val start = TimeSource.Monotonic.markNow()
    performOperation()
    val duration = start.elapsedNow()
    Log.performance(TAG, "Operation took ${duration.inWholeMilliseconds}ms")
}

// ❌ WRONG - measurement overhead even when logging disabled
val start = TimeSource.Monotonic.markNow()
performOperation()
Log.performance(TAG, "Took ${start.elapsedNow().inWholeMilliseconds}ms")
```

**Batch performance logs**:
```kotlin
// ✅ CORRECT - accumulate metrics, log once
class MetricsCollector {
    private val samples = mutableListOf<Duration>()

    fun recordSample(duration: Duration) {
        if (WWWGlobals.LogConfig.ENABLE_PERFORMANCE_LOGGING) {
            samples.add(duration)
            if (samples.size >= 100) {
                val avg = samples.average()
                Log.performance(TAG, "Average over 100 samples: ${avg}ms")
                samples.clear()
            }
        }
    }
}
```

### Memory Considerations

**Avoid large object logging**:
```kotlin
// ❌ BAD - creates large string in memory
Log.d(TAG, "All events: ${events.joinToString()}")

// ✅ BETTER - log count and summary
Log.d(TAG, "Loaded ${events.size} events (${events.count { it.isActive }} active)")
```

---

## Security and Privacy Guidelines

### NEVER Log These in Production

**Personally Identifiable Information (PII)**:
```kotlin
// ❌ FORBIDDEN
Log.i(TAG, "User email: ${user.email}")
Log.i(TAG, "User name: ${user.displayName}")
Log.i(TAG, "User phone: ${user.phoneNumber}")
Log.i(TAG, "Device ID: ${deviceId}")
```

**Precise Location Coordinates**:
```kotlin
// ❌ FORBIDDEN in production (OK in debug)
Log.i(TAG, "User position: ${position.lat}, ${position.lng}")

// ✅ ACCEPTABLE - generalized location
Log.i(TAG, "User in city: ${city.name}")
Log.i(TAG, "User ${if (inEventArea) "inside" else "outside"} event area")
```

**Authentication Credentials**:
```kotlin
// ❌ FORBIDDEN
Log.d(TAG, "API token: $token")
Log.d(TAG, "Password: $password")
Log.d(TAG, "Bearer: $bearerToken")
```

### Safe Logging Patterns

**Use identifiers, not values**:
```kotlin
// ✅ CORRECT - log ID, not content
Log.i(TAG, "Event ${event.id} loaded")
Log.i(TAG, "Processing event with ${event.waves.size} waves")

// ❌ WRONG - logs entire event object
Log.i(TAG, "Event loaded: $event")
```

**Redact sensitive fields**:
```kotlin
// ✅ CORRECT - redacted sensitive data
data class User(
    val id: String,
    val email: String
) {
    override fun toString(): String = "User(id=$id, email=[REDACTED])"
}

Log.i(TAG, "User logged in: $user")  // Prints "User(id=abc123, email=[REDACTED])"
```

**Coordinate obfuscation**:
```kotlin
// ✅ ACCEPTABLE - reduced precision (city-level)
fun Position.toSafeString(): String {
    val roundedLat = (lat * 100).toInt() / 100.0  // 2 decimal places ≈ 1km
    val roundedLng = (lng * 100).toInt() / 100.0
    return "($roundedLat, $roundedLng)"
}

Log.i(TAG, "User near: ${position.toSafeString()}")
```

**Use debug-only logging for sensitive data**:
```kotlin
// ✅ CORRECT - detailed logs only in debug builds
if (WWWGlobals.LogConfig.ENABLE_DEBUG_LOGGING) {
    Log.d(TAG, "Precise position: ${position.lat}, ${position.lng}")
}

// Production builds won't log this at all
```

### Security Checklist

Before committing code, verify:
- [ ] No API keys, tokens, or secrets in log messages
- [ ] No user email, name, or phone numbers
- [ ] No precise coordinates in INFO/WARN/ERROR logs
- [ ] No password or credential logging (even hashed)
- [ ] Sensitive data only in DEBUG/VERBOSE with feature flags
- [ ] All production logs use `[REDACTED]` for sensitive fields

---

## Platform-Specific Behavior

### Android (Logcat)

**Viewing Logs**:
```bash
# All app logs
adb logcat -s "WorldWideWaves"

# Specific tag
adb logcat -s "EventObserver"

# Multiple tags
adb logcat -s "EventObserver:V" "MapViewModel:D"

# Filter by priority
adb logcat *:E  # Errors only
adb logcat *:W  # Warnings and above
```

**Tag Length Limit**: 23 characters (longer tags are truncated)

**Log Buffer**: Limited to ~256KB (older logs are discarded)

**Colors**: Logcat shows colors by priority:
- VERBOSE: Gray
- DEBUG: Blue
- INFO: Green
- WARN: Orange
- ERROR: Red

**Example Output**:
```
10-27 14:32:15.123  1234  5678 V EventObserver: Starting unified observation for event abc123
10-27 14:32:15.456  1234  5678 I MapViewModel: Camera position changed
10-27 14:32:16.789  1234  5678 E Firebase: Event upload failed
```

### iOS (Unified Logging)

**Viewing Logs**:
```bash
# Simulator logs (real-time)
xcrun simctl spawn booted log stream \
  --predicate 'process == "WorldWideWaves"' \
  --level debug

# Device logs
log stream --device \
  --predicate 'subsystem == "com.worldwidewaves"' \
  --level debug

# Filter by category
log show --predicate 'category == "EventObserver"' --last 1h
```

**Console.app**: Open Console.app and filter by "WorldWideWaves"

**Log Levels Mapping**:
```
Log.v  →  os_log(.debug)      (Debug builds only)
Log.d  →  os_log(.info)       (Debug/Beta builds)
Log.i  →  os_log(.default)    (All builds)
Log.w  →  os_log(.error)      (All builds)
Log.e  →  os_log(.fault)      (All builds)
```

**Privacy Redaction**:
iOS automatically redacts strings in logs unless marked public:
```swift
// Swift side (if needed)
os_log("User at %{public}@", position)  // Shows position
os_log("User at %@", position)           // Shows <private>
```

**Example Output**:
```
2025-10-27 14:32:15.123 WorldWideWaves[1234:5678] [EventObserver] Starting observation
2025-10-27 14:32:15.456 WorldWideWaves[1234:5678] [MapViewModel] Camera updated
2025-10-27 14:32:16.789 WorldWideWaves[1234:5678] [Firebase] Upload failed: <private>
```

### Cross-Platform Differences

| Feature | Android (Logcat) | iOS (Unified Logging) |
|---------|------------------|-----------------------|
| Tag length | 23 chars max | No limit |
| Privacy | Manual redaction | Auto-redacts strings |
| Buffer size | ~256KB | Much larger (system-managed) |
| Performance | Minimal overhead | Minimal overhead |
| Filtering | By tag/priority | By subsystem/category/predicate |
| Colors | Yes (in Android Studio) | No (monochrome) |
| Export | `adb logcat > file.log` | `log show --last 1h > file.log` |

---

## Good vs Bad Logging Examples

### Example 1: Exception Handling

```kotlin
// ❌ BAD - Missing throwable, vague message
try {
    loadEvents()
} catch (e: Exception) {
    Log.e(TAG, "Error")
}

// ❌ BAD - Only logging message, no stack trace
try {
    loadEvents()
} catch (e: Exception) {
    Log.e(TAG, "Failed to load events: ${e.message}")
}

// ✅ GOOD - Clear message with throwable
try {
    loadEvents()
} catch (e: Exception) {
    Log.e(TAG, "Failed to load events from Firebase", throwable = e)
}

// ✅ EXCELLENT - Context + specific error handling
try {
    loadEvents()
} catch (e: NetworkException) {
    Log.e(TAG, "Network error loading events (retry scheduled)", throwable = e)
} catch (e: DatabaseException) {
    Log.e(TAG, "Database error loading events (using cache)", throwable = e)
}
```

### Example 2: State Transitions

```kotlin
// ❌ BAD - Unclear, missing context
Log.d(TAG, "State changed")

// ❌ BAD - Too verbose, cluttered
Log.d(TAG, "The event state has been changed from $oldState to $newState at ${System.currentTimeMillis()} for event with ID ${event.id}")

// ✅ GOOD - Clear, concise, contextual
Log.i(TAG, "Event ${event.id} transitioned: $oldState → $newState")

// ✅ EXCELLENT - Includes reason for transition
Log.i(TAG, "Event ${event.id}: $oldState → $newState (wave started)")
```

### Example 3: Performance Logging

```kotlin
// ❌ BAD - No feature flag, overhead even when disabled
val start = TimeSource.Monotonic.markNow()
renderMap()
Log.performance(TAG, "Render: ${start.elapsedNow()}")

// ❌ BAD - Expensive operation in string interpolation
Log.performance(TAG, "Data: ${events.joinToString()}")

// ✅ GOOD - Feature flag + minimal overhead
if (WWWGlobals.LogConfig.ENABLE_PERFORMANCE_LOGGING) {
    val start = TimeSource.Monotonic.markNow()
    renderMap()
    Log.performance(TAG, "Map render: ${start.elapsedNow().inWholeMilliseconds}ms")
}

// ✅ EXCELLENT - Batched metrics
if (WWWGlobals.LogConfig.ENABLE_PERFORMANCE_LOGGING) {
    metrics.record(duration)
    if (metrics.sampleCount >= 100) {
        Log.performance(TAG, "Avg render (100 frames): ${metrics.average()}ms")
    }
}
```

### Example 4: User Actions

```kotlin
// ❌ BAD - Missing context
Log.i(TAG, "Button clicked")

// ❌ BAD - Contains PII
Log.i(TAG, "User ${user.email} joined event")

// ✅ GOOD - Clear action, no PII
Log.i(TAG, "User joined event ${event.id}")

// ✅ EXCELLENT - Action + outcome
Log.i(TAG, "User joined event ${event.id}, now has ${user.joinedEvents.size} active events")
```

### Example 5: Conditional Logging

```kotlin
// ❌ BAD - String interpolation happens regardless
Log.v(TAG, "Position: ${position.lat}, ${position.lng}")
// ^ Even if VERBOSE is disabled, string is built

// ✅ GOOD - Feature flag prevents string building
if (WWWGlobals.LogConfig.ENABLE_VERBOSE_LOGGING) {
    Log.v(TAG, "Position: ${position.lat}, ${position.lng}")
}

// ✅ EXCELLENT - Extension function for reusability
fun Position.toDebugString(): String {
    return if (WWWGlobals.LogConfig.ENABLE_DEBUG_LOGGING) {
        "Position(lat=$lat, lng=$lng, accuracy=$accuracy)"
    } else {
        "Position([REDACTED])"
    }
}

Log.v(TAG, "Updated: ${position.toDebugString()}")
```

### Example 6: Multi-Line Logs

```kotlin
// ❌ BAD - Hard to read, loses formatting in Logcat
Log.d(TAG, "Camera state: CameraState(zoom=${camera.zoom}, center=${camera.center}, bearing=${camera.bearing})")

// ✅ GOOD - Structured with line breaks (debug only)
if (WWWGlobals.LogConfig.ENABLE_DEBUG_LOGGING) {
    Log.d(TAG, """
        Camera state updated:
          Zoom: ${camera.zoom}
          Center: ${camera.center}
          Bearing: ${camera.bearing}
    """.trimIndent())
}

// ✅ EXCELLENT - Key-value pairs for easy parsing
Log.d(TAG, "Camera: zoom=${camera.zoom} center=${camera.center} bearing=${camera.bearing}")
```

---

## Structured Logging Patterns

### Key-Value Logging

Use consistent key-value patterns for easier parsing and filtering:

```kotlin
// ✅ GOOD - Parseable format
Log.i(TAG, "event_id=${event.id} status=active waves=${event.waves.size}")

// Log aggregation tools can easily extract:
// - event_id: "abc123"
// - status: "active"
// - waves: 3
```

### Event Correlation with IDs

**Use consistent identifiers across logs**:

```kotlin
class EventObserver(private val event: IWWWEvent) {
    private val eventId = event.id  // Cache ID

    fun startObservation() {
        Log.i(TAG, "event_id=$eventId action=start_observation")
        // ...
        Log.i(TAG, "event_id=$eventId action=observation_started waves=${event.waves.size}")
    }

    fun stopObservation() {
        Log.i(TAG, "event_id=$eventId action=stop_observation")
    }
}

// Later, grep all logs for a specific event:
// grep "event_id=abc123" logcat.txt
```

### Request/Response Logging

**Log request and response together**:

```kotlin
suspend fun loadEvent(eventId: String): Result<IWWWEvent> {
    Log.d(TAG, "api_call=load_event event_id=$eventId status=started")

    val result = try {
        val event = api.fetchEvent(eventId)
        Log.i(TAG, "api_call=load_event event_id=$eventId status=success waves=${event.waves.size}")
        Result.success(event)
    } catch (e: Exception) {
        Log.e(TAG, "api_call=load_event event_id=$eventId status=error", throwable = e)
        Result.failure(e)
    }

    return result
}
```

### Timing and Duration Logging

**Consistent timing format**:

```kotlin
// ✅ GOOD - Milliseconds for short operations
Log.performance(TAG, "operation=render_frame duration_ms=${duration.inWholeMilliseconds}")

// ✅ GOOD - Seconds for longer operations
Log.i(TAG, "operation=download_map duration_s=${duration.inWholeSeconds}")

// ✅ GOOD - Start/end markers for async operations
Log.d(TAG, "operation=async_task task_id=$id status=started")
// ... async work ...
Log.d(TAG, "operation=async_task task_id=$id status=completed duration_ms=$duration")
```

### Metrics and Counters

**Log cumulative metrics**:

```kotlin
object Metrics {
    private var eventLoadCount = 0
    private var cacheHits = 0
    private var cacheMisses = 0

    fun logEventLoad(fromCache: Boolean) {
        eventLoadCount++
        if (fromCache) cacheHits++ else cacheMisses++

        if (eventLoadCount % 100 == 0) {
            val hitRate = (cacheHits.toFloat() / eventLoadCount * 100).toInt()
            Log.i("Metrics", "events_loaded=$eventLoadCount cache_hit_rate=${hitRate}%")
        }
    }
}
```

### Hierarchical Context Logging

**Use breadcrumb-style logging for complex flows**:

```kotlin
class WaveParticipationFlow {
    fun execute(event: IWWWEvent, user: User) {
        Log.i(TAG, "flow=wave_participation event_id=${event.id} step=1_validate")

        if (!validateEvent(event)) {
            Log.w(TAG, "flow=wave_participation event_id=${event.id} step=1_validate result=invalid")
            return
        }

        Log.i(TAG, "flow=wave_participation event_id=${event.id} step=2_check_eligibility")
        // ...

        Log.i(TAG, "flow=wave_participation event_id=${event.id} step=3_join result=success")
    }
}

// Grep entire flow: grep "flow=wave_participation event_id=abc123"
```

---

## Testing Logging Output

### Unit Testing Log Statements

**Option 1: Verify log calls with test logger**:

```kotlin
@Test
fun testLoggingBehavior() = runTest {
    // Arrange: Inject test logger (if using dependency injection)
    val testLogger = TestLogger()
    val component = MyComponent(logger = testLogger)

    // Act
    component.performAction()

    // Assert
    assertTrue(testLogger.debugMessages.any { it.contains("Action performed") })
}
```

**Option 2: Test side effects, not logs**:

```kotlin
@Test
fun testErrorHandling() = runTest {
    // Arrange
    val repository = ErrorThrowingRepository()
    val component = MyComponent(repository)

    // Act & Assert
    val result = component.loadData()

    // Test the behavior, not the log statement
    assertTrue(result.isFailure)
    assertEquals(ErrorType.NETWORK, result.errorType)

    // Logging is a side effect - test the recovery behavior instead
}
```

### Instrumented Tests with Logcat Verification

**Capture logs during Android instrumented tests**:

```kotlin
@Test
fun testUserJoinsEvent() {
    // Clear logcat before test
    Runtime.getRuntime().exec("adb logcat -c")

    // Perform action
    onView(withId(R.id.joinButton)).perform(click())

    // Capture logs
    val logs = Runtime.getRuntime()
        .exec("adb logcat -d -s EventScreen:I")
        .inputStream
        .bufferedReader()
        .readText()

    // Verify log message
    assertTrue(logs.contains("User joined event"))
}
```

### Testing Log Levels

**Verify correct log level is used**:

```kotlin
@Test
fun testCriticalErrorUsesErrorLevel() {
    // Spy on Napier (if using test double)
    val napierSpy = mockk<Napier>(relaxed = true)

    // Trigger error condition
    component.handleCriticalError()

    // Verify ERROR level was used (not DEBUG or INFO)
    verify { napierSpy.e(any(), any(), any()) }
}
```

### Performance Log Testing

**Verify performance logs don't impact production**:

```kotlin
@Test
fun testPerformanceLoggingDisabledInRelease() {
    // Simulate release build configuration
    WWWGlobals.LogConfig.ENABLE_PERFORMANCE_LOGGING = false

    val startTime = TimeSource.Monotonic.markNow()

    repeat(10000) {
        // This should be nearly instant (no string building)
        Log.performance(TAG, "Hot path operation ${expensiveOperation()}")
    }

    val duration = startTime.elapsedNow()

    // Should complete in milliseconds, not seconds
    assertTrue(duration < 100.milliseconds)
}
```

### Log Output Validation

**Test log message format**:

```kotlin
@Test
fun testStructuredLoggingFormat() {
    val eventId = "test-event-123"
    val expectedPattern = "event_id=$eventId action=start.*".toRegex()

    // Capture log output
    val logMessage = captureLogMessage {
        observer.startObservation(eventId)
    }

    // Verify format matches
    assertTrue(expectedPattern.matches(logMessage))
}
```

---

## Troubleshooting and Debugging

### Common Logging Issues

#### Issue 1: Logs Not Appearing

**Symptoms**: Log statements don't show in Logcat/Console

**Causes**:
1. Log level disabled by build configuration
2. Tag filter too restrictive
3. Buffer overflow (old logs discarded)
4. Wrong process ID

**Solutions**:
```bash
# Android: Check all logs (remove filters)
adb logcat -c  # Clear buffer
adb logcat     # View all logs

# iOS: Check predicate syntax
log stream --predicate 'processImagePath CONTAINS "WorldWideWaves"' --level debug

# Verify build configuration
Log.d(TAG, "Debug logging enabled: ${WWWGlobals.LogConfig.ENABLE_DEBUG_LOGGING}")
```

#### Issue 2: Performance Degradation

**Symptoms**: App feels slow after adding logging

**Cause**: Hot path logging without feature flags

**Solution**:
```kotlin
// ❌ BEFORE (slow)
fun onPositionUpdate(position: Position) {
    Log.v(TAG, "Position: ${position.lat}, ${position.lng}")  // Runs on every GPS update!
}

// ✅ AFTER (fast)
fun onPositionUpdate(position: Position) {
    if (WWWGlobals.LogConfig.ENABLE_POSITION_TRACKING_LOGGING) {
        if (updateCount++ % 10 == 0) {  // Sample 1/10 updates
            Log.v(TAG, "Position: ${position.lat}, ${position.lng}")
        }
    }
}
```

#### Issue 3: Memory Issues

**Symptoms**: Out of memory errors, large heap usage

**Cause**: Logging large data structures

**Solution**:
```kotlin
// ❌ BEFORE (memory leak)
Log.d(TAG, "All events: ${events.joinToString { it.toString() }}")
// Creates huge string in memory

// ✅ AFTER (memory efficient)
Log.d(TAG, "Loaded ${events.size} events (${events.sumOf { it.waves.size }} total waves)")
```

### Debugging with Logs

**Pattern 1: Bisection Debugging**:

```kotlin
fun complexOperation() {
    Log.d(TAG, "complexOperation: START")

    val step1 = performStep1()
    Log.d(TAG, "complexOperation: step1 complete, result=${step1.size}")

    val step2 = performStep2(step1)
    Log.d(TAG, "complexOperation: step2 complete, result=${step2.status}")

    val step3 = performStep3(step2)
    Log.d(TAG, "complexOperation: step3 complete, result=$step3")

    Log.d(TAG, "complexOperation: END")
}

// If bug occurs between step2 and step3, add more logs there
```

**Pattern 2: State Dump Debugging**:

```kotlin
fun debugStateDump() {
    if (WWWGlobals.LogConfig.ENABLE_DEBUG_LOGGING) {
        Log.d(TAG, """
            === STATE DUMP ===
            Event ID: ${event.id}
            Waves: ${event.waves.size}
            Active: ${isActive}
            User Position: ${positionManager.currentPosition}
            Simulation: ${platform.getSimulation()?.isEnabled}
            Cache: ${cacheSize} items
            ==================
        """.trimIndent())
    }
}

// Call when debugging complex state issues
```

**Pattern 3: Timing Race Conditions**:

```kotlin
suspend fun debugRaceCondition() {
    val startTime = TimeSource.Monotonic.markNow()

    launch {
        delay(100)
        Log.d(TAG, "[${startTime.elapsedNow()}] Coroutine A started")
        // ...
    }

    launch {
        delay(50)
        Log.d(TAG, "[${startTime.elapsedNow()}] Coroutine B started")
        // ...
    }

    // Logs show relative timing:
    // [50ms] Coroutine B started
    // [100ms] Coroutine A started
}
```

### Production Debugging

**Remote log collection** (for production issues):

```kotlin
object ProductionLogger {
    private val recentLogs = CircularBuffer<String>(capacity = 1000)

    fun log(level: String, tag: String, message: String) {
        // Store in memory for crash reports
        recentLogs.add("[$level] $tag: $message")

        // Also log to system
        Log.i(tag, message)
    }

    fun getRecentLogs(): List<String> = recentLogs.toList()

    // Include in crash reports
    fun onCrash() {
        crashReporter.addCustomData("recent_logs", getRecentLogs().joinToString("\n"))
    }
}
```

---

## Quick Reference Card

### Log Level Decision Tree

```
Is it user-facing information?
├─ YES → Use INFO
│   └─ Examples: "User joined event", "Download complete"
│
└─ NO → Is it an error?
    ├─ YES → Can the app recover?
    │   ├─ YES → Use WARNING
    │   │   └─ Examples: "Cache miss", "Retry scheduled"
    │   │
    │   └─ NO → Is it impossible/catastrophic?
    │       ├─ YES → Use WTF
    │       │   └─ Examples: "Invalid state", "Data corruption"
    │       │
    │       └─ NO → Use ERROR
    │           └─ Examples: "API failure", "Database error"
    │
    └─ NO → Is it performance-related?
        ├─ YES → Use PERFORMANCE (with feature flag)
        │   └─ Examples: "Frame time: 16ms", "Cache hit rate: 95%"
        │
        └─ NO → Is it development-only?
            ├─ YES → Is it high-frequency?
            │   ├─ YES → Use VERBOSE (with sampling)
            │   │   └─ Examples: "Position update", "Animation frame"
            │   │
            │   └─ NO → Use DEBUG
            │       └─ Examples: "State transition", "Cache operation"
            │
            └─ Use INFO (low-frequency operational data)
```

### Quick Syntax Reference

```kotlin
// VERBOSE (debug builds only)
Log.v(TAG, "Detailed debug info")
Log.v(TAG, "Step-by-step trace", throwable)

// DEBUG (debug/beta builds)
Log.d(TAG, "Development information")
Log.d(TAG, "State change", throwable)

// INFO (all builds - production safe)
Log.i(TAG, "User action or important state")
Log.i(TAG, "Successful operation", throwable)

// WARNING (all builds - production safe)
Log.w(TAG, "Recoverable error or unusual state")
Log.w(TAG, "Retry scheduled", throwable)

// ERROR (all builds - production safe)
Log.e(TAG, "Error requiring attention")
Log.e(TAG, "Operation failed", throwable)

// WTF (all builds - production safe)
Log.wtf(TAG, "Impossible condition")
Log.wtf(TAG, "Catastrophic failure", throwable)

// PERFORMANCE (debug builds with flag)
Log.performance(TAG, "[PERF] Measurement")
```

### Build Configuration Flags

| Flag | Debug | Beta | Release |
|------|-------|------|---------|
| `ENABLE_VERBOSE_LOGGING` | ✅ | ❌ | ❌ |
| `ENABLE_DEBUG_LOGGING` | ✅ | ✅ | ❌ |
| `ENABLE_PERFORMANCE_LOGGING` | ✅ | ❌ | ❌ |
| `ENABLE_POSITION_TRACKING_LOGGING` | ✅ | ❌ | ❌ |

---

## Additional Resources

### Internal Documentation
- [Development Guide](./development.md) - General development practices
- [Architecture Guide](./architecture.md) - System architecture overview
- [Testing Strategy](./testing-strategy.md) - Testing best practices

### External Resources
- [Napier Documentation](https://github.com/AAkira/Napier) - Cross-platform logging library
- [Android Logcat](https://developer.android.com/studio/debug/logcat) - Android logging reference
- [iOS Unified Logging](https://developer.apple.com/documentation/os/logging) - iOS logging reference
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) - Async logging patterns

### Code Examples
- [Log.kt](../shared/src/commonMain/kotlin/com/worldwidewaves/shared/utils/Log.kt) - Log wrapper implementation
- [WWWGlobals.kt](../shared/src/commonMain/kotlin/com/worldwidewaves/shared/WWWGlobals.kt) - Build configuration flags
- [EventObserver.kt](../shared/src/commonMain/kotlin/com/worldwidewaves/shared/domain/observation/EventObserver.kt) - Real-world logging examples

---

**Document Version**: 1.0
**Last Updated**: October 27, 2025
**Maintained By**: WorldWideWaves Development Team
**Feedback**: Report issues or suggestions via project issue tracker
