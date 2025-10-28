# WorldWideWaves Debug & Troubleshooting Logging Report

## Executive Summary

Comprehensive search of the WorldWideWaves codebase identified **59 files** containing `Log.d()` and `Log.v()` calls. Most logging statements are legitimate production logging with appropriate use cases. However, several categories of logging warrant review:

1. **Excessive/Verbose Logging** in critical paths (MapWrapperRegistry.kt with 50+ logs)
2. **Debug Comments** indicating temporary logging (BoundingBox.kt, IosSoundPlayer.kt)
3. **Conditional Debug Logging** that should use LogConfig guards (PositionManager.kt, DefaultPositionObserver.kt)
4. **Commented-out Debug Logs** that should be cleaned up (Platform.android.kt)

---

## Files with Concerning Debug/Verbose Logging

### TIER 1: HIGH VOLUME EXCESSIVE LOGGING

#### 1. `/Users/ldiasdasilva/StudioProjects/WorldWideWaves/shared/src/iosMain/kotlin/com/worldwidewaves/shared/map/MapWrapperRegistry.kt`

**Issue**: 50+ `Log.d()` and `Log.v()` calls for state synchronization
**Severity**: MEDIUM - Creates verbose logs for every state mutation
**Lines with Log calls**: Multiple throughout file (approximately 60+ log statements)

**Sample problematic logs:**
- Line ~80: `Log.d(TAG, "Registering wrapper for event: $eventId (total wrappers: ${wrappers.size})")`
- Line ~190: Multiple `Log.v()` calls for every getter/setter
- Line ~250: `Log.v(TAG, "Map width updated: $width for event: $eventId")`
- Line ~255: `Log.v(TAG, "Map height updated: $height for event: $eventId")`

**Assessment**: These logs track internal state management (polygon registration, camera commands, callbacks). While useful for debugging, they will spam logs whenever:
- Camera position changes
- Map dimensions update
- Callbacks register/unregister
- Zoom levels change

**Recommendation**: Convert to sampled logging or guard with `shouldLog()` checks

---

#### 2. `/Users/ldiasdasilva/StudioProjects/WorldWideWaves/shared/src/iosMain/kotlin/com/worldwidewaves/shared/map/IosPlatformMapManager.kt`

**Issue**: 15+ `Log.d()` calls for map ODR (On-Demand Resources) management
**Severity**: LOW-MEDIUM - Legitimate but verbose
**Lines with Log calls**: ~50, 75, 150, 155, 175, 180, 220, 225, 230

**Problematic logs:**
```kotlin
Log.d(TAG, "Checking map availability for: $mapId")
Log.d(TAG, "Found via pathForResource: $eventId.$extension in $sub")
Log.d(TAG, "Creating new NSBundleResourceRequest for tag: $mapId")
Log.d(TAG, "Progress ticker already running for: $mapId")
Log.v(TAG, "Progress tick for $mapId: $p%")  // Updates every progress interval!
```

**Assessment**: Progress ticker logs every increment (1% intervals = 90+ logs per download). The "already running" and "creating request" logs repeat frequently.

**Recommendation**: Use sampled logging for progress ticks

---

#### 3. `/Users/ldiasdasilva/StudioProjects/WorldWideWaves/shared/src/commonMain/kotlin/com/worldwidewaves/shared/map/EventMapDownloadManager.kt`

**Issue**: Progress tracking with verbose verbose logs
**Severity**: LOW - Progress tracking logs are expected
**Lines with Log calls**: ~65, 75, 85, 95, 110, 115

**Sample logs:**
```kotlin
Log.d(TAG, "Checking availability for: $mapId")
Log.v(TAG, "Download progress: $mapId -> $progress%")  // Called frequently
Log.d(TAG, "Clearing completed downloads from cache")
Log.v(TAG, "Removed completed download: ${entry.key}")
```

**Assessment**: The `Log.v()` for progress updates will emit for every percent change. More moderate than MapWrapperRegistry but still adds verbosity.

**Recommendation**: Document that `Log.v()` calls happen frequently during downloads

---

### TIER 2: DEBUG COMMENTS INDICATING TEMPORARY LOGGING

#### 4. `/Users/ldiasdasilva/StudioProjects/WorldWideWaves/shared/src/commonMain/kotlin/com/worldwidewaves/shared/events/utils/BoundingBox.kt`

**Issue**: Debug logging with explicit comment
**Severity**: LOW - Single log statement, commented as debug
**Lines**: 75-79

**Code:**
```kotlin
// Debug logging for iOS coordinate issues
Log.d(
    "BoundingBox",
    "constructor: input SW($swLat,$swLng) NE($neLat,$neLng) → output SW(${sw.lat},${sw.lng}) NE(${ne.lat},${ne.lng})",
)
```

**Assessment**: This log runs on **every BoundingBox instantiation** - potentially thousands of times. Comment says "Debug logging for iOS coordinate issues" suggesting it was added for troubleshooting a specific iOS issue that may be resolved.

**Recommendation**: Remove or make conditional with `shouldLog()` check

---

#### 5. `/Users/ldiasdasilva/StudioProjects/WorldWideWaves/shared/src/iosMain/kotlin/com/worldwidewaves/shared/utils/IosAppFinisher.kt`

**Issue**: UI state debug logs
**Severity**: LOW - Only logs on app termination
**Lines**: ~50, ~55

**Code:**
```kotlin
Log.d(TAG, "finishIosApp: dismissed modal VC")
Log.d(TAG, "finishIosApp: popped from navigation stack")
```

**Assessment**: These logs fire when app is closing. Not problematic in frequency but labeled as debugging platform-specific behavior.

**Recommendation**: Keep these - they're low frequency and useful for diagnostics

---

#### 6. `/Users/ldiasdasilva/StudioProjects/WorldWideWaves/shared/src/iosMain/kotlin/com/worldwidewaves/shared/sound/IosSoundPlayer.kt`

**Issue**: Audio engine setup and playback logging
**Severity**: LOW - Audio operations are infrequent
**Lines**: Multiple (audio session setup, engine setup, volume changes, playback)

**Sample logs:**
```kotlin
Log.v(TAG, "Audio session setup completed")
Log.d(TAG, "Valid audio format detected: sampleRate=$sampleRate, channels=$channelCount")
Log.v(TAG, "Audio engine setup completed successfully")
Log.v(TAG, "Set mixer volume to $level")
Log.v(TAG, "Set volume to max (1.0) from $originalMixerVolume")
Log.v(TAG, "Playing tone: freq=$frequency, amp=$amplitude, dur=$duration, wave=$waveform")
Log.v(TAG, "iOS audio playback completed (${samples.size} samples)")
Log.v(TAG, "Restored volume to $originalMixerVolume")
Log.v(TAG, "iOS sound player released")
```

**Assessment**: These are legitimate audio diagnostics. Frequency is low because audio initialization and playback are infrequent. Each log provides useful debugging context.

**Recommendation**: Keep these - they're appropriate verbose logging for low-frequency operations

---

### TIER 3: CONDITIONAL DEBUG LOGGING THAT NEEDS GUARDS

#### 7. `/Users/ldiasdasilva/StudioProjects/WorldWideWaves/shared/src/commonMain/kotlin/com/worldwidewaves/shared/position/PositionManager.kt`

**Issue**: Position update logging with `[DEBUG]` prefix in messages
**Severity**: MEDIUM - High frequency + conditional but not guarded
**Lines**: 99, 104, 109, 114, 118

**Code:**
```kotlin
if (WWWGlobals.LogConfig.ENABLE_POSITION_TRACKING_LOGGING) {
    Log.v(TAG, "[DEBUG] Position update from $source: $newPosition")
}
// ... more instances
Log.v(TAG, "[DEBUG] Rejected position update from $source...")
Log.v(TAG, "[DEBUG] Skipped duplicate position update")
Log.v(TAG, "[DEBUG] Stored pending update: $newState, debounceDelay=$debounceDelay")
Log.v(TAG, "[DEBUG] Applied debounced position: ${finalState.position} from ${finalState.source}")
```

**Assessment**: Position updates fire frequently (every GPS or simulation update). Guard is correct, but:
1. Messages explicitly say `[DEBUG]` suggesting temporary logging
2. Condition check is in place (good!)
3. Unconditional logs also present:
   - Line 132: `Log.v("PositionManager", "Cleared all position data")` - not guarded!
   - Line 139: `Log.v("PositionManager", "Cleaned up resources")` - not guarded!

**Recommendation**:
- Keep guarded logs - they're properly protected
- Add guards to unguarded verbose logs
- Change `[DEBUG]` prefix to remove if permanent

---

#### 8. `/Users/ldiasdasilva/StudioProjects/WorldWideWaves/shared/src/commonMain/kotlin/com/worldwidewaves/shared/domain/observation/DefaultPositionObserver.kt`

**Issue**: Position observation logging with conditional guard
**Severity**: LOW - Properly guarded with guard condition
**Lines**: 71, 160

**Code:**
```kotlin
if (WWWGlobals.LogConfig.ENABLE_POSITION_TRACKING_LOGGING) {
    Log.v("DefaultPositionObserver", "Starting position observation for event ${event.id}")
}
// ...
if (WWWGlobals.LogConfig.ENABLE_POSITION_TRACKING_LOGGING) {
    Log.v("DefaultPositionObserver", "Stopping position observation")
}
```

**Assessment**:
- Properly guarded with configuration check
- Only logs on observation start/stop (low frequency)
- This is correct pattern

**Recommendation**: Keep as-is - proper guard pattern

---

### TIER 4: COMMENTED-OUT DEBUG LOGS (SHOULD CLEAN UP)

#### 9. `/Users/ldiasdasilva/StudioProjects/WorldWideWaves/shared/src/androidMain/kotlin/com/worldwidewaves/shared/Platform.android.kt`

**Issue**: Multiple commented-out debug logs (11+ lines)
**Severity**: LOW - Already commented, but clutters code
**Lines**: Multiple commented lines

**Examples:**
```kotlin
//    Log.d("clearUnavailableGeoJsonCache", "Cleared cache for event $eventId")
//        Log.d(::readGeoJson.name, "GeoJSON file not available for event $eventId")
//                Log.d(::getMapFileAbsolutePath.name, "Cache file doesn't exist for $eventId.$extension")
//                Log.d(::getMapFileAbsolutePath.name, "Metadata file doesn't exist for $eventId.$extension")
//                    Log.d(
//                    Log.d(
//        Log.d(::getMapFileAbsolutePath.name, "Map feature not downloaded for $eventId.$extension...")
//                Log.d(::getMapFileAbsolutePath.name, "Asset not accessible on attempt...")
//        Log.d(::getMapFileAbsolutePath.name, "Map feature not available: $eventId.$extension...")
```

**Assessment**: 11+ commented-out log lines for map file operations. These were likely disabled during debugging map store implementation.

**Recommendation**: Remove commented-out debug logs (code cleanliness)

---

### TIER 5: LEGITIMATE PRODUCTION LOGGING (KEEP)

These are properly implemented and should remain:

#### ✅ `/Users/ldiasdasilva/StudioProjects/WorldWideWaves/shared/src/iosMain/kotlin/com/worldwidewaves/shared/utils/PerformanceTracer.ios.kt`

**Log:** `Log.d("WWW.Perf", "trace=$name duration_ms=$duration metrics=[$metricsStr]")`
- Purpose: Performance measurement (appropriate for Log.d)
- Frequency: Only at trace completion (low frequency)

#### ✅ `/Users/ldiasdasilva/StudioProjects/WorldWideWaves/shared/src/androidMain/kotlin/com/worldwidewaves/shared/utils/PerformanceTracer.android.kt`

**Log:** `Log.d("WWW.Perf", "trace=$name duration_ms=$duration metrics=[$metricsStr]")`
- Purpose: Performance measurement (appropriate for Log.d)
- Frequency: Only at trace completion (low frequency)

#### ✅ `/Users/ldiasdasilva/StudioProjects/WorldWideWaves/shared/src/commonMain/kotlin/com/worldwidewaves/shared/sound/SoundChoreographyCoordinator.kt`

**Logs:**
```kotlin
Log.d(TAG, "Starting global sound choreography observation for all events")
Log.d(TAG, "Observing ${allEvents.size} events for area status")
Log.d(TAG, "Area status changed - eventInArea: ${eventInArea?.id}")
Log.d(TAG, "User entered event area: ${eventInArea.id}")
```
- Purpose: User action tracking and event coordination
- Frequency: Low (triggered by user entering/leaving areas)
- Assessment: Appropriate usage

#### ✅ `/Users/ldiasdasilva/StudioProjects/WorldWideWaves/shared/src/commonMain/kotlin/com/worldwidewaves/shared/choreographies/SoundChoreographyPlayer.kt`

**Logs:**
```kotlin
Log.d("SoundChoreographyManager", "Attempting to preload MIDI file: $midiResourcePath")
Log.d("SoundChoreographyManager", "Successfully preloaded MIDI file: $midiResourcePath")
```
- Purpose: MIDI resource lifecycle
- Frequency: Low (only during initialization)

---

## Summary by Severity

### CRITICAL (Remove/Fix)
- **0 items** - No critical debug logging found

### HIGH (Review & Reduce)
1. BoundingBox.kt - Debug logging on every constructor call

### MEDIUM (Refactor)
1. MapWrapperRegistry.kt - 50+ logs for state synchronization
2. IosPlatformMapManager.kt - Progress ticker logs frequent updates
3. PositionManager.kt - Some unguarded verbose logs

### LOW (Clean up / Document)
1. Platform.android.kt - 11+ commented-out logs
2. EventMapDownloadManager.kt - High-frequency progress logs
3. Various low-frequency debug logs (acceptable)

---

## Recommendations

### 1. Immediate Actions (High Priority)

**Remove commented-out logs (Platform.android.kt)**
- Delete ~11 lines of commented debug logs
- Improves code cleanliness with no functional impact

**Add guard to unguarded PositionManager logs**
```kotlin
// Line 132 - Add guard
if (WWWGlobals.LogConfig.ENABLE_POSITION_TRACKING_LOGGING) {
    Log.v("PositionManager", "Cleared all position data")
}

// Line 139 - Add guard
if (WWWGlobals.LogConfig.ENABLE_POSITION_TRACKING_LOGGING) {
    Log.v("PositionManager", "Cleaned up resources")
}
```

**Remove or guard BoundingBox.kt debug log**
```kotlin
// Option 1: Remove entirely
// The coordinate transformation is already validated by tests

// Option 2: Guard with config check
if (RuntimeLogConfig.shouldLog("BoundingBox", LogLevel.DEBUG)) {
    Log.d("BoundingBox", "constructor: input SW($swLat,$swLng) NE($neLat,$neLng) → output...")
}
```

### 2. Medium Priority (Review in next refactor)

**MapWrapperRegistry.kt** - Consider reducing state logs:
- Convert routine state logs to `Log.v()` with sampled logging
- Keep state transition logs at `Log.d()` level
- Use sampled logging: `Log.vSampled(TAG, message, sampleRate=100)`

**IosPlatformMapManager.kt** - Sample progress ticker:
```kotlin
// Instead of every percent:
Log.v(TAG, "Progress tick for $mapId: $p%")

// Use sampled logging:
if (p % 10 == 0) {  // Log every 10%
    Log.v(TAG, "Progress tick for $mapId: $p%")
}
```

### 3. Documentation (Low Priority)

Document the following patterns in code style guide:
- Position tracking logs should use `ENABLE_POSITION_TRACKING_LOGGING` guard
- Progress tracking logs can use high frequency but should consider sampled logging
- Audio operations can use verbose logging (low frequency)
- State mutation logging should be guarded or sampled

---

## Files Analyzed

### Summary Statistics

| Category | Count |
|----------|-------|
| Total files with Log.d/v | 59 |
| Shared commonMain | ~35 |
| Shared iosMain | ~25 |
| Shared androidMain | ~10 |
| ComposeApp (Android) | ~1 |
| Test files (excluded) | ~5 |

### Files with Multiple Log Calls (20+)

1. MapWrapperRegistry.kt - ~60 logs
2. IosPlatformMapManager.kt - ~20 logs
3. WWWEventMap.kt - ~15 logs
4. EventMapDownloadManager.kt - ~15 logs
5. SoundChoreographyCoordinator.kt - ~10 logs
6. MainScreen.kt - ~3 logs (minor)

---

## Conclusion

The codebase is **reasonably clean** with appropriate use of debug/verbose logging. The main areas for improvement are:

1. **Code cleanliness**: Remove ~11 commented-out debug logs
2. **Guard consistency**: Add guards to 2 unguarded PositionManager logs
3. **Performance**: Consider sampling for high-frequency state logs in MapWrapperRegistry
4. **High-frequency logs**: Document/sample progress ticker logs in map downloads

**No critical debug logging issues found that would impact production performance.**
