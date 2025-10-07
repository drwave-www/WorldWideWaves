# iOS MapLibre Refactor - Next Session Prompt

**Date**: October 8, 2025 (End of Analysis Session)
**Status**: 🔴 **CRITICAL ARCHITECTURE REFACTOR REQUIRED**
**Context**: Deep analysis revealed iOS MapLibre uses fundamentally broken polling/registry pattern

---

## 🚨 CRITICAL DISCOVERY

The iOS MapLibre implementation has a **fundamental architectural flaw**:

### The Problem
```
Android: updateWavePolygons() → runOnUiThread{} → IMMEDIATE render (16ms)
iOS:     updateWavePolygons() → store in registry → poll every 100ms → render (100-200ms)
```

**Result**:
- Wave progression stutters/freezes
- 74 wave updates but only 1 render
- Map clicks don't work (registry lookup fails)
- Wrapper deallocates prematurely (weak references)

### Root Cause Analysis (from logs_10)

**Constraint Bounds Failure**:
```
MapBoundsEnforcer: mapBounds: SW(48.8, 2.2) NE(48.9, 2.4)  ✅ Paris bounds correct
MapBoundsEnforcer: padding: lat=90.0, lng=180.0  ❌ ENTIRE WORLD!
Result: minLat=-41, maxLat=138, minLng=-177, maxLng=182  ❌ Invalid!
```

**Why**: `IosMapLibreAdapter.getVisibleRegion()` was **not implemented** (line 84-91), always returned fallback (entire world). Fixed in commit 27dea9fe, but not the core issue.

**Wrapper Deallocation**:
```
downloadState.isAvailable: false → true → false → true
→ when{} branch switching
→ View controller disposed
→ Wrapper deallocated
→ 74 updates, 1 render
```

**Why**: Even with `remember(event.id)`, the view controller cache was inside the when{} block. Fixed in commit 27dea9fe, but polling architecture still broken.

---

## 🎯 YOUR MISSION (Next Session)

**Replace the iOS polling/registry architecture with Android's direct dispatch pattern**

This single change will fix:
- ✅ Wave progression (immediate updates)
- ✅ Map clicks (direct callbacks)
- ✅ Wrapper lifecycle (strong references)
- ✅ Performance (no polling waste)

---

## 📖 Required Reading (BEFORE STARTING)

**Read in this order**:

1. **This file** (you're reading it)
2. **iOS_MAP_REFACTOR_TODO.md** (comprehensive task breakdown)
3. **Android reference implementation**:
   - `composeApp/src/androidMain/kotlin/com/worldwidewaves/compose/map/AndroidEventMap.kt` (lines 946-954)
   - `composeApp/src/androidMain/kotlin/com/worldwidewaves/map/AndroidMapLibreAdapter.kt` (full file)
4. **Current iOS implementation**:
   - `shared/src/iosMain/kotlin/com/worldwidewaves/shared/map/IosEventMap.kt`
   - `shared/src/iosMain/kotlin/com/worldwidewaves/shared/map/IosMapLibreAdapter.kt`
   - `iosApp/worldwidewaves/MapLibre/MapLibreViewWrapper.swift`
5. **Architecture comparison**:
   - `ANDROID_VS_IOS_EVENTMAP_ANALYSIS.md`

---

## 🚀 Quick Start Guide

### Step 1: Understand the Architecture Difference

**Android (Direct Integration)**:
```kotlin
// In AndroidEventMap.kt:946-954
override fun updateWavePolygons(wavePolygons: List<Polygon>, clearPolygons: Boolean) {
    context.runOnUiThread {  // Direct UI thread dispatch ✅
        val mapLibrePolygons = wavePolygons.map { it.toMapLibrePolygon() }
        mapLibreAdapter.addWavePolygons(mapLibrePolygons, clearPolygons)
    }
}

// In AndroidMapLibreAdapter.kt:348-368
override fun addWavePolygons(polygons: List<Any>, clearExisting: Boolean) {
    val fillLayer = style.getLayerAs<FillLayer>("wave-polygons-layer")
    fillLayer?.setProperties(...)  // IMMEDIATE rendering ✅
}
```

**iOS (Registry + Polling)** ❌:
```kotlin
// In IosEventMap.kt:99-132
override fun updateWavePolygons(wavePolygons: List<Polygon>, clearPolygons: Boolean) {
    MapWrapperRegistry.setPendingPolygons(event.id, coordinates, clearPolygons)  // Store ❌
    // Rendering happens 100ms later when polling timer fires ❌
}

// In MapLibreViewWrapper.swift:718-739
Timer.scheduledTimer(withTimeInterval: 0.1, repeats: true) {  // Polling ❌
    IOSMapBridge.renderPendingPolygons(eventId: eventId)
}
```

### Step 2: Implement Direct Dispatch (START HERE)

**File to Modify**: `shared/src/iosMain/kotlin/com/worldwidewaves/shared/map/IosEventMap.kt`
**Method**: `updateWavePolygons()` (lines 99-132)

**Replace with**:
```kotlin
@OptIn(ExperimentalForeignApi::class)
override fun updateWavePolygons(wavePolygons: List<Polygon>, clearPolygons: Boolean) {
    Log.i("IosEventMap", "🌊 updateWavePolygons: ${wavePolygons.size} polygons")

    if (clearPolygons) {
        currentPolygons.clear()
    }
    currentPolygons.addAll(wavePolygons)

    // Direct dispatch to main queue (like Android's runOnUiThread)
    dispatch_async(dispatch_get_main_queue()) {
        val wrapper = MapWrapperRegistry.getWrapper(event.id)
        if (wrapper != null) {
            // Convert polygons
            val coords: List<List<Pair<Double, Double>>> = wavePolygons.map { polygon ->
                polygon.map { position -> Pair(position.lat, position.lng) }
            }

            // Render immediately (no registry, no polling)
            IOSMapBridge.renderPolygonsDirectly(
                eventId = event.id,
                wrapper = wrapper,
                coordinates = coords,
                clearExisting = clearPolygons
            )
            Log.i("IosEventMap", "✅ Polygons rendered immediately")
        }
    }
}
```

**Also Add** (IOSMapBridge.swift):
```swift
@objc public static func renderPolygonsDirectly(
    eventId: String,
    wrapper: Any,
    coordinates: [[KotlinPair<KotlinDouble, KotlinDouble>]],
    clearExisting: Bool
) {
    guard let mapWrapper = wrapper as? MapLibreViewWrapper else { return }

    let coordinateArrays = coordinates.map { polygon in
        polygon.compactMap { pair in
            guard let lat = pair.first?.doubleValue,
                  let lng = pair.second?.doubleValue else { return nil }
            return CLLocationCoordinate2D(latitude: lat, longitude: lng)
        }
    }

    mapWrapper.addWavePolygons(polygons: coordinateArrays, clearExisting: clearExisting)
}
```

**Test**:
```bash
# Run app, watch logs
grep "WaveObserver.*Updating\|rendered immediately" /tmp/logs_new | head -40
# Every update should show "rendered immediately" right after
```

### Step 3: Remove Polling

**After** Step 2 works:

1. Delete polling timer from `MapLibreViewWrapper.swift` (lines 709-752)
2. Delete `setPendingPolygons()` from `MapWrapperRegistry.kt`
3. Delete `renderPendingPolygons()` from `IOSMapBridge.swift`

**Verify**: No "Polling active" logs, wave still renders continuously

---

## 🧪 Validation After Each Change

### After Phase 1.1 (Direct Wave Polygons)
```bash
# Should see immediate renders
grep "WaveObserver.*Updating" /tmp/logs | wc -l
grep "rendered immediately\|Rendered.*polygons" /tmp/logs | wc -l
# Counts should match!

# Should see NO registry storage
grep "setPendingPolygons\|Storing.*polygons" /tmp/logs
# Should be empty (or only initial setup)

# Should see NO polling
grep "Polling active" /tmp/logs
# Should be empty
```

### After Phase 1.4 (Strong References)
```bash
# Should see ZERO deallocations during wave screen
grep "Deinitializing MapLibreViewWrapper" /tmp/logs
# Should only appear when screen exits, not during

# Should see ONE wrapper creation per screen
grep "Creating native map view controller" /tmp/logs | wc -l
# Should be 1 (or 2 if user navigated to multiple screens)
```

### After Phase 2 (Map Click)
```bash
# Tap map, should see:
grep "👆 Map tap detected" /tmp/logs
# Should appear

grep "Map click callback invoked successfully" /tmp/logs
# Should appear with 100% success rate
```

---

## 📊 Current Status (Before Refactor)

### Feature Parity
- **Android**: 12/12 features (100%)
- **iOS**: 4/12 features (33%)
- **Gap**: 8 missing features

### Architecture Quality
- **Android**: Direct SDK integration ✅
- **iOS**: Registry + Polling ❌
- **Performance Gap**: 100ms+ vs 16ms

### User Issues
- ❌ Constraint bounds not enforced (getVisibleRegion fixed, but architecture issue remains)
- ❌ Wave progression not updating continuously (polling too slow)
- ❌ Map click not working (registry lookup fails)

### Test Coverage
- **Android**: Full integration tests ✅
- **iOS**: Trivial unit tests only ❌
- **Gap**: No iOS MapLibre integration tests

---

## 🎯 Target State (After Refactor)

### Feature Parity
- **iOS**: 12/12 features (100%) ✅

### Architecture Quality
- **iOS**: Direct dispatch (matches Android) ✅
- **Performance**: <50ms latency for all operations ✅

### User Issues
- ✅ All 3 issues resolved
- ✅ Wave progression smooth
- ✅ Map clicks work reliably
- ✅ Constraints enforced

### Test Coverage
- ✅ >80% code coverage on iOS map layer
- ✅ Integration tests with real MapLibre SDK
- ✅ Performance tests (<50ms latency)
- ✅ Lifecycle tests (no leaks)

---

## 🛠️ Development Environment Setup

### Before Starting

1. **Ensure clean build**:
```bash
./gradlew clean
rm -rf shared/build/xcode-frameworks
cd iosApp && xcodebuild clean
```

2. **Verify all tests pass**:
```bash
./gradlew :shared:testDebugUnitTest
# Should show: 902+ tests passing
```

3. **Check current status**:
```bash
git status
git log --oneline -10
# Review recent commits
```

### During Development

**Run tests after each change**:
```bash
./gradlew :shared:testDebugUnitTest
```

**Build iOS after Kotlin changes**:
```bash
cd iosApp
xcodebuild -project worldwidewaves.xcodeproj -scheme worldwidewaves \
  -destination 'platform=iOS Simulator,name=iPhone 16' build
```

**Capture logs for analysis**:
```bash
xcrun simctl spawn booted log stream \
  --predicate 'process == "worldwidewaves"' \
  --level debug > /tmp/logs_refactor_test_1
```

---

## 📝 Commit Strategy

### Commit Message Template
```
refactor(ios): [Phase X.Y] Brief description

**Phase**: [Phase number and name]
**Changes**:
- Bullet list of changes
- File paths and line numbers

**Impact**:
- What this fixes/improves
- Performance impact if applicable

**Testing**:
- All X tests passing
- Verified: [specific verification]

**Before/After**:
- Before: [old behavior]
- After: [new behavior]

🤖 Generated with [Claude Code](https://claude.com/claude-code)

Co-Authored-By: Claude <noreply@anthropic.com>
```

### Commit Frequency
- Commit after each Phase task completion
- Run all tests before each commit
- Include test results in commit message

---

## 🎓 Implementation Tips

### Kotlin/Native iOS Interop

**Import dispatch_async**:
```kotlin
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import kotlinx.cinterop.ExperimentalForeignApi
```

**Usage**:
```kotlin
@OptIn(ExperimentalForeignApi::class)
fun someMethod() {
    dispatch_async(dispatch_get_main_queue()) {
        // This runs on iOS main thread ✅
        // Equivalent to Android's context.runOnUiThread {}
    }
}
```

### Strong References in Kotlin/Native

**Correct**:
```kotlin
private val wrappers = mutableMapOf<String, Any>()
wrappers[id] = wrapper  // Strong reference ✅
```

**Incorrect**:
```kotlin
private val wrappers = mutableMapOf<String, WeakReference>()
wrappers[id] = WeakReference(wrapper)  // Premature GC ❌
```

### Swift Method Naming for Kotlin

Swift methods are automatically exposed to Kotlin:
```swift
@objc public func setMinZoom(_ minZoom: Double) { }
```

Becomes in Kotlin:
```kotlin
wrapper.setMinZoom(10.0)  // Works automatically ✅
```

---

## 🔍 Debugging Tips

### Check if Direct Dispatch Works
```bash
# After implementing Phase 1.1
# Run app and capture logs

# Wave updates should show immediate rendering
grep -A 1 "WaveObserver.*Updating wave polygons" /tmp/logs | grep "rendered immediately"
# Every update should be followed by "rendered immediately"

# Should see NO registry storage
grep "Storing.*pending polygons" /tmp/logs
# Should be empty
```

### Check Wrapper Lifecycle
```bash
# Should see 1 creation, 0 deallocations during wave screen
grep "Creating native map view controller" /tmp/logs
# Count: 1 (per screen)

grep "Deinitializing MapLibreViewWrapper" /tmp/logs
# Count: 0 (during wave screen)
# Count: 1 (only when screen exits)
```

### Check Performance
```bash
# Measure time between update and render
grep "WaveObserver.*Updating\|rendered immediately" /tmp/logs | \
  awk '/Updating/{t=$1} /rendered/{print $1-t}'
# Should be <0.05 seconds (50ms)
```

---

## 📚 Reference Implementation (Android)

### Wave Polygon Rendering (AndroidEventMap.kt:946-954)
```kotlin
override fun updateWavePolygons(
    wavePolygons: List<Polygon>,
    clearPolygons: Boolean,
) {
    context.runOnUiThread {
        val mapLibrePolygons = wavePolygons.map { it.toMapLibrePolygon() }
        mapLibreAdapter.addWavePolygons(mapLibrePolygons, clearPolygons)
    }
}
```

**Key Points**:
- Uses `context.runOnUiThread{}` for thread safety
- Direct adapter call (no registry)
- No polling needed
- Maps Polygon → MapLibre format inline

### Camera Animation (AndroidMapLibreAdapter.kt:259-291)
```kotlin
override fun animateCamera(
    position: Position,
    zoom: Double?,
    callback: MapCameraCallback?,
) {
    val cameraPosition = CameraPosition.Builder()
        .target(LatLng(position.lat, position.lng))
        .zoom(zoom ?: currentZoom.value)
        .build()

    val cameraUpdate = CameraUpdateFactory.newCameraPosition(cameraPosition)

    mapLibreMap!!.animateCamera(cameraUpdate, object : MapLibreMap.CancelableCallback {
        override fun onFinish() { callback?.onFinish() }
        override fun onCancel() { callback?.onCancel() }
    })
}
```

**Key Points**:
- Direct MapLibre SDK calls
- No registry, no commands
- Callback properly wired

### Map Click (AndroidMapLibreAdapter.kt:159-179)
```kotlin
override fun setOnMapClickListener(listener: ((Double, Double) -> Unit)?) {
    mapLibreMap?.let { map ->
        if (listener != null) {
            val newListener = MapLibreMap.OnMapClickListener { point ->
                listener(point.latitude, point.longitude)
                true
            }
            map.addOnMapClickListener(newListener)
            currentMapClickListener = newListener
        }
    }
}
```

**Key Points**:
- Direct listener registration with SDK
- No registry lookup
- Listener stored strongly in adapter

---

## 🔬 Testing Strategy

### Phase 1 Testing (After Direct Dispatch Implementation)

**Test 1: Wave Progression Smooth**
1. Start simulation
2. Watch wave progress for 30 seconds
3. **Expected**: Smooth animation, no stuttering
4. **Logs**: All WaveObserver updates show immediate rendering

**Test 2: No Polling Active**
1. Run app
2. Capture logs
3. **Expected**: Zero "Polling active" messages
4. **Verify**: `grep "Polling active" /tmp/logs` returns nothing

**Test 3: Wrapper Survives**
1. Enter wave screen
2. Watch wave for 60 seconds
3. Exit wave screen
4. **Expected**: Wrapper deallocated only at exit
5. **Logs**: Only 1 "Deinitializing" message

### Phase 2 Testing (After Map Click Fix)

**Test 1: Map Click Navigation**
1. Open event screen (small map)
2. Tap center of map
3. **Expected**: Navigates to full-screen map
4. **Logs**: "👆 Map tap detected" + callback invoked

**Test 2: Click Reliability**
1. Tap map 10 times
2. **Expected**: 10/10 navigation events
3. **Logs**: 10 tap detections, 10 callback invocations

### Phase 3 Testing (After Constraint Bounds Fix)

**Test 1: Map Bounded**
1. Open Paris event
2. Try to pan to London
3. **Expected**: Cannot pan outside Paris area
4. **Logs**: "✅ Constraint bounds applied successfully"

**Test 2: Bounds Applied Before Style**
1. Open event screen (style not loaded yet)
2. **Expected**: Bounds queued, applied after style loads
3. **Logs**: "Queueing constraint bounds" → "Applying queued bounds"

---

## 🐛 Common Pitfalls

### Pitfall 1: Dispatch Queue Crashes
**Problem**: Calling `dispatch_async` without `@OptIn(ExperimentalForeignApi::class)`

**Solution**:
```kotlin
@OptIn(ExperimentalForeignApi::class)
override fun updateWavePolygons(...) {
    dispatch_async(dispatch_get_main_queue()) { ... }
}
```

### Pitfall 2: Wrapper Type Casting
**Problem**: `getWrapper()` returns `Any`, need to cast

**Solution**:
```kotlin
val wrapper = MapWrapperRegistry.getWrapper(eventId) as? MapLibreViewWrapper
if (wrapper == null) {
    Log.e(TAG, "Wrapper not found or wrong type for event: $eventId")
    return
}
```

### Pitfall 3: Callback Threading
**Problem**: Kotlin callbacks invoked from Swift main thread might cause issues

**Solution**: Already on main thread via dispatch_async, safe to call ✅

### Pitfall 4: Memory Management
**Problem**: Strong references without cleanup cause leaks

**Solution**:
- Use strong references during screen session
- Call `unregisterWrapper()` in `DisposableEffect` cleanup

---

## 📊 Progress Tracking

Use this checklist to track progress:

### Week 1: Critical Path
- [ ] Day 1: Phase 1.1 - Direct dispatch for wave polygons
- [ ] Day 2: Phase 1.2 - Direct dispatch for camera commands
- [ ] Day 2: Phase 1.3 - Remove polling timer
- [ ] Day 3: Phase 1.4 - Strong references
- [ ] Day 3: Phase 2.1-2.2 - Fix map click
- [ ] Day 4: Phase 3.1-3.2 - Fix constraint bounds
- [ ] Day 5: Testing & bug fixes

**Milestone**: All 3 user issues resolved

### Week 2: Feature Parity
- [ ] Day 6-7: Phase 4 - Implement missing adapter methods (8 stubs)
- [ ] Day 8-9: Phase 6 - Add comprehensive tests
- [ ] Day 10: Phase 5, 7 - Polish & cleanup

**Milestone**: 100% feature parity with Android

---

## 🎯 Definition of Done

### Phase 1 Done When:
- ✅ Zero polling timer logs
- ✅ Zero registry storage logs
- ✅ WaveObserver update count = Render count
- ✅ Latency <50ms for all operations
- ✅ Zero wrapper deallocations during screen session
- ✅ All 902+ tests still passing

### Phase 2 Done When:
- ✅ Map tap detection 100% reliable
- ✅ Navigation works on every tap
- ✅ No registry lookup code remains

### Phase 3 Done When:
- ✅ Map cannot pan outside event area
- ✅ Constraints applied even before style loads
- ✅ getVisibleRegion() returns actual bounds

### All Phases Done When:
- ✅ iOS feature parity: 12/12 (100%)
- ✅ All user issues resolved: 3/3
- ✅ Test coverage >80%
- ✅ iOS map behavior identical to Android

---

## 🚨 Critical Notes

### DO NOT:
- ❌ Increase polling frequency (still wrong architecture)
- ❌ Add more registry storage (technical debt)
- ❌ Use weak references for wrappers (causes GC issues)
- ❌ Skip tests (causes regressions)

### DO:
- ✅ Follow Android's proven patterns exactly
- ✅ Use direct dispatch for all UI updates
- ✅ Test incrementally after each phase
- ✅ Commit frequently with clear messages
- ✅ Verify in simulator after each change

---

## 📞 Help & References

### If Stuck
1. Re-read Android implementation (`AndroidEventMap.kt`, `AndroidMapLibreAdapter.kt`)
2. Check Kotlin/Native iOS interop docs
3. Review MapLibre iOS SDK documentation
4. Check previous session logs for context

### Key Commits
- `27dea9fe`: getVisibleRegion implementation (good pattern)
- `67cf5887`: BoundingBox fixes
- `ab50d41d`: Wrapper lifecycle fixes (partial)

### Log Analysis
- `/tmp/logs_10`: Shows padding=90×180 bug
- `/tmp/logs_9`: Shows wrapper deallocation pattern
- `/tmp/logs_8`: Shows wave update count mismatch

---

## 🎉 Expected Outcome

After completing all phases:

**User Experience**:
- ✅ Wave animation smooth (60 FPS)
- ✅ Map clicks responsive
- ✅ Map properly bounded to event area
- ✅ No crashes or freezes

**Code Quality**:
- ✅ iOS architecture matches Android
- ✅ Zero architectural inefficiencies
- ✅ Clean, maintainable code
- ✅ Comprehensive test coverage

**Performance**:
- ✅ <16ms rendering latency (60 FPS)
- ✅ Zero CPU waste on polling
- ✅ Improved battery life

**Maintainability**:
- ✅ Single source of truth (no registry duplication)
- ✅ Easy to debug (direct call stack)
- ✅ Easy to add features (just implement in wrapper)

---

**Ready to Start**: Begin with Phase 1.1 (Direct wave polygon dispatch)
**Estimated Completion**: 11-16 days for full refactor
**Quick Win**: Phase 1 alone (2-3 days) fixes most critical issues

Good luck! 🚀
