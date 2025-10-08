# iOS MapLibre Implementation Refactor - Comprehensive TODO

**Date Created**: October 8, 2025
**Status**: 🔴 CRITICAL REFACTOR REQUIRED
**Estimated Effort**: 9-13 days
**Context**: iOS MapLibre uses broken polling/registry architecture instead of Android's direct dispatch

---

## 📊 Executive Summary

**Current State**: iOS MapLibre has only **33% feature parity** with Android (4/12 features implemented)

**Core Problem**: Registry + Polling pattern causes:
- ❌ Wave progression stuttering (100ms+ delay vs Android's <16ms)
- ❌ Map clicks not working (registry lookup failures)
- ❌ Wrapper premature deallocation (weak references GC'd)
- ❌ Constraint bounds timing failures (style loading race conditions)
- ❌ CPU/battery waste (10 polls/second even when idle)

**Solution**: Replace registry/polling with **direct dispatch pattern** (like Android)

**Impact**: Fixes all 3 critical user-reported issues + achieves feature parity

---

## 🎯 Phase 1: Eliminate Registry/Polling Architecture (CRITICAL - 2-3 days)

### Background
**Android Architecture** (composeApp/src/androidMain/kotlin/com/worldwidewaves/compose/map/AndroidEventMap.kt:946-954):
```kotlin
override fun updateWavePolygons(wavePolygons: List<Polygon>, clearPolygons: Boolean) {
    context.runOnUiThread {  // Direct UI thread dispatch
        val mapLibrePolygons = wavePolygons.map { it.toMapLibrePolygon() }
        mapLibreAdapter.addWavePolygons(mapLibrePolygons, clearPolygons)
    }
}
```

**iOS Current** (shared/src/iosMain/kotlin/com/worldwidewaves/shared/map/IosEventMap.kt:99-132):
```kotlin
override fun updateWavePolygons(wavePolygons: List<Polygon>, clearPolygons: Boolean) {
    storePolygonsForRendering(wavePolygons, clearPolygons)  // Store in registry
    // Rendering happens 100ms+ later via polling timer ❌
}
```

**iOS Target**:
```kotlin
override fun updateWavePolygons(wavePolygons: List<Polygon>, clearPolygons: Boolean) {
    dispatch_async(dispatch_get_main_queue()) {  // Direct dispatch like Android
        val wrapper = MapWrapperRegistry.getWrapper(eventId) as? MapLibreViewWrapper
        wrapper?.addWavePolygons(coordinates, clearPolygons)
    }
}
```

### Tasks

#### 1.1 Add Direct Dispatch for Wave Polygons ✅ HIGH PRIORITY
**File**: `shared/src/iosMain/kotlin/com/worldwidewaves/shared/map/IosEventMap.kt`
**Lines**: 99-132 (updateWavePolygons)

**Changes**:
- [ ] Remove `storePolygonsForRendering()` call
- [ ] Add `dispatch_async(dispatch_get_main_queue())` wrapper
- [ ] Get wrapper from registry with strong reference
- [ ] Call `wrapper.addWavePolygons()` directly
- [ ] Remove `setPendingPolygons()` from MapWrapperRegistry

**Expected Result**: Polygons render in <16ms (60 FPS), not 100ms+ delayed

**Test Verification**:
```bash
# Should see immediate polygon rendering in logs
grep "WaveObserver.*Updating\|Rendered.*polygons" /tmp/logs_new | head -20
# Count should match: X updates = X renders
```

---

#### 1.2 Add Direct Dispatch for Camera Commands ✅ HIGH PRIORITY
**File**: `shared/src/iosMain/kotlin/com/worldwidewaves/shared/map/IosMapLibreAdapter.kt`
**Lines**: 129-166 (moveCamera, animateCamera, animateCameraToBounds)

**Current**:
```kotlin
override fun animateCamera(position: Position, zoom: Double?, callback: MapCameraCallback?) {
    MapWrapperRegistry.setPendingCameraCommand(eventId, CameraCommand.AnimateToPosition(...))
    // Executed 100ms+ later by polling timer ❌
}
```

**Target**:
```kotlin
override fun animateCamera(position: Position, zoom: Double?, callback: MapCameraCallback?) {
    dispatch_async(dispatch_get_main_queue()) {
        val wrapper = MapWrapperRegistry.getWrapper(eventId) as? MapLibreViewWrapper
        wrapper?.animateCamera(latitude: position.lat, longitude: position.lng, zoom: zoom, callback: callback)
    }
}
```

**Changes**:
- [ ] Replace `setPendingCameraCommand()` with direct dispatch
- [ ] Call wrapper methods directly (animateCamera, moveCamera, etc.)
- [ ] Remove CameraCommand enum (no longer needed)
- [ ] Update callback handling for async completion

**Expected Result**: Camera movements immediate, not delayed

---

#### 1.3 Remove Polling Timer ✅ HIGH PRIORITY
**File**: `iosApp/worldwidewaves/MapLibre/MapLibreViewWrapper.swift`
**Lines**: 709-752 (startContinuousPolling, timer)

**Changes**:
- [ ] Delete `startContinuousPolling()` method
- [ ] Delete `stopContinuousPolling()` method
- [ ] Delete `commandPollingTimer` property
- [ ] Remove timer start from `didFinishLoading style:` (line 783)
- [ ] Remove timer stop from `deinit` (line 67)

**Expected Result**: Zero CPU cycles wasted on polling

---

#### 1.4 Replace Weak References with Strong References ✅ HIGH PRIORITY
**File**: `shared/src/iosMain/kotlin/com/worldwidewaves/shared/map/MapWrapperRegistry.kt`
**Lines**: 138-156 (getWrapper with weak references)

**Current**:
```kotlin
private val wrappers = mutableMapOf<String, WrapperEntry>()

data class WrapperEntry(
    val weakRef: WeakReference,  // ❌ Premature GC
    var lastAccessed: Long
)
```

**Target**:
```kotlin
private val wrappers = mutableMapOf<String, Any>()  // ✅ Strong references

fun registerWrapper(eventId: String, wrapper: Any) {
    wrappers[eventId] = wrapper  // Strong reference
}

fun unregisterWrapper(eventId: String) {
    wrappers.remove(eventId)  // Explicit cleanup on screen exit
}
```

**Changes**:
- [ ] Change `WrapperEntry` to direct wrapper storage
- [ ] Remove weak reference wrapping
- [ ] Remove LRU cache eviction logic
- [ ] Add explicit `unregisterWrapper()` call in screen cleanup
- [ ] Update all `getWrapper()` call sites

**Expected Result**: Wrapper survives entire screen session, zero premature deallocations

---

#### 1.5 Update EventMapView to Call Unregister ⚠️ MEDIUM PRIORITY
**File**: `iosApp/worldwidewaves/MapLibre/EventMapView.swift`
**Lines**: 38-116 (makeUIView, updateUIView)

**Changes**:
- [ ] Add `onDisappear()` or equivalent SwiftUI lifecycle hook
- [ ] Call `MapWrapperRegistry.shared.unregisterWrapper(eventId: eventId)`
- [ ] Ensure cleanup happens when screen exits

**Expected Result**: No memory leaks, clean wrapper lifecycle

---

## 🎯 Phase 2: Fix Map Click (HIGH PRIORITY - 1 day)

### Background
**Problem**: Registry callback lookup fails (weak references, timing issues)

**Android Approach** (composeApp/src/androidMain/kotlin/com/worldwidewaves/map/AndroidMapLibreAdapter.kt:159-179):
```kotlin
override fun setOnMapClickListener(listener: ((Double, Double) -> Unit)?) {
    if (listener != null) {
        val newListener = MapLibreMap.OnMapClickListener { point ->
            listener(point.latitude, point.longitude)  // Direct invocation ✅
            true
        }
        map.addOnMapClickListener(newListener)
    }
}
```

**iOS Current** (iosApp/worldwidewaves/MapLibre/MapLibreViewWrapper.swift:474-484):
```swift
if let eventId = eventId {
    let invoked = Shared.MapWrapperRegistry.shared.invokeMapClickCallback(eventId: eventId)
    // ❌ Lookup often fails: callback GC'd or eventId mismatch
}
```

### Tasks

#### 2.1 Store Callback Directly in Wrapper ✅ HIGH PRIORITY
**File**: `iosApp/worldwidewaves/MapLibre/MapLibreViewWrapper.swift`
**Lines**: 34, 436-484

**Current**:
```swift
private var onMapClick: ((Double, Double) -> Void)?  // Coordinate callback (unused)
// ... later ...
Shared.MapWrapperRegistry.shared.invokeMapClickCallback(eventId: eventId)  // Registry lookup ❌
```

**Target**:
```swift
private var onMapClickNavigation: (() -> Void)?  // Navigation callback

@objc public func setMapClickCallback(_ callback: @escaping () -> Void) {
    self.onMapClickNavigation = callback
}

@objc private func handleMapTap(_ gesture: UITapGestureRecognizer) {
    onMapClickNavigation?()  // Direct invocation ✅
}
```

**Changes**:
- [ ] Add `onMapClickNavigation` property to wrapper
- [ ] Add `setMapClickCallback()` method to wrapper
- [ ] Update `handleMapTap()` to call callback directly
- [ ] Remove registry lookup code

**Expected Result**: Map clicks work 100% of the time

---

#### 2.2 Update IosEventMap to Set Callback Directly ✅ HIGH PRIORITY
**File**: `shared/src/iosMain/kotlin/com/worldwidewaves/shared/map/IosEventMap.kt`
**Lines**: 164-172 (map click callback registration)

**Current**:
```kotlin
LaunchedEffect(event.id, onMapClick) {
    if (onMapClick != null) {
        MapWrapperRegistry.setMapClickCallback(event.id, onMapClick)  // Registry storage ❌
    }
}
```

**Target**:
```kotlin
LaunchedEffect(event.id, onMapClick) {
    if (onMapClick != null) {
        val wrapper = MapWrapperRegistry.getWrapper(event.id) as? MapLibreViewWrapper
        wrapper?.setMapClickCallback(onMapClick)  // Direct storage ✅
    }
}
```

**Changes**:
- [ ] Replace `MapWrapperRegistry.setMapClickCallback()` with direct wrapper call
- [ ] Remove `setMapClickCallback()` from MapWrapperRegistry.kt
- [ ] Remove `mapClickCallbacks` storage from MapWrapperRegistry.kt

**Expected Result**: Callback reliably set and invoked

---

## 🎯 Phase 3: Fix Constraint Bounds (HIGH PRIORITY - 1 day)

### Background
**Problem**: Constraint bounds fail if style not loaded yet (timing race)

**Current Flow**:
```
setupMap() called → setBoundsForCameraTarget() called
→ Style not loaded yet → Command fails ❌
→ Command cleared from registry
→ Style loads later → No command to retry
→ Map unconstrained forever
```

### Tasks

#### 3.1 Queue Constraints Until Style Loads ✅ HIGH PRIORITY
**File**: `iosApp/worldwidewaves/MapLibre/MapLibreViewWrapper.swift`
**Lines**: 255-292 (setBoundsForCameraTarget)

**Add**:
```swift
private var pendingConstraintBounds: MLNCoordinateBounds?

@objc public func setBoundsForCameraTarget(...) -> Bool {
    guard let mapView = mapView else { return false }

    let southwest = CLLocationCoordinate2D(latitude: swLat, longitude: swLng)
    let northeast = CLLocationCoordinate2D(latitude: neLat, longitude: neLng)
    let bounds = MLNCoordinateBounds(sw: southwest, ne: northeast)

    if !styleIsLoaded || mapView.style == nil {
        pendingConstraintBounds = bounds  // Queue for later
        return true  // Will apply when style loads
    }

    mapView.setVisibleCoordinateBounds(bounds, animated: false)
    pendingConstraintBounds = nil
    return true
}
```

**Changes**:
- [ ] Add `pendingConstraintBounds` property
- [ ] Queue bounds if style not loaded
- [ ] Return true (don't fail)
- [ ] Apply queued bounds in `didFinishLoading style:` callback

**Expected Result**: Constraints always applied after style loads

---

#### 3.2 Apply Queued Constraints After Style Loads ✅ HIGH PRIORITY
**File**: `iosApp/worldwidewaves/MapLibre/MapLibreViewWrapper.swift`
**Lines**: 757-791 (didFinishLoading style delegate)

**Add to callback** (after line 775):
```swift
// Apply queued constraint bounds if any
if let bounds = pendingConstraintBounds {
    WWWLog.i(Self.tag, "Applying queued constraint bounds after style load")
    mapView.setVisibleCoordinateBounds(bounds, animated: false)
    pendingConstraintBounds = nil
    WWWLog.i(Self.tag, "✅ Constraint bounds applied successfully")
}
```

**Changes**:
- [ ] Check for `pendingConstraintBounds` after style loads
- [ ] Apply bounds to mapView
- [ ] Clear pending bounds
- [ ] Add success logging

**Expected Result**: Map properly constrained to event area

---

## 🎯 Phase 4: Implement Missing Adapter Features (MEDIUM PRIORITY - 3-5 days)

### 4.1 Implement getMinZoomLevel() ⚠️ MEDIUM PRIORITY
**File**: `shared/src/iosMain/kotlin/com/worldwidewaves/shared/map/IosMapLibreAdapter.kt`
**Lines**: 185-188

**Current**:
```kotlin
override fun getMinZoomLevel(): Double {
    // NOTE: Implement with proper MapLibre iOS bindings
    return 0.0  // STUB ❌
}
```

**Target**:
```kotlin
override fun getMinZoomLevel(): Double {
    val wrapper = MapWrapperRegistry.getWrapper(eventId) as? MapLibreViewWrapper
    return wrapper?.getMinZoom() ?: 0.0
}
```

**Changes**:
- [ ] Call wrapper's `getMinZoom()` method (already exists at MapLibreViewWrapper.swift:302)
- [ ] Return actual zoom level from MapLibre

**Expected Result**: Zoom constraints work correctly

---

### 4.2 Implement setMinZoomPreference() ⚠️ MEDIUM PRIORITY
**File**: `shared/src/iosMain/kotlin/com/worldwidewaves/shared/map/IosMapLibreAdapter.kt`
**Lines**: 190-196

**Current**:
```kotlin
override fun setMinZoomPreference(minZoom: Double) {
    if (wrapper != null) {
        // NOTE: Implement via cinterop bindings
        Log.d("IosMapLibreAdapter", "Set minimum zoom level: $minZoom")  // STUB ❌
    }
}
```

**Target**:
```kotlin
override fun setMinZoomPreference(minZoom: Double) {
    val wrapper = MapWrapperRegistry.getWrapper(eventId) as? MapLibreViewWrapper
    wrapper?.setMinZoom(minZoom)
}
```

**Swift Method Already Exists**: `MapLibreViewWrapper.swift:294` (`setMinZoom()`)

**Changes**:
- [ ] Call wrapper's `setMinZoom()` method
- [ ] Remove stub comment

**Expected Result**: Minimum zoom enforced

---

### 4.3 Implement setMaxZoomPreference() ⚠️ MEDIUM PRIORITY
**File**: `shared/src/iosMain/kotlin/com/worldwidewaves/shared/map/IosMapLibreAdapter.kt`
**Lines**: 198-204

**Swift Method Already Exists**: `MapLibreViewWrapper.swift:298` (`setMaxZoom()`)

**Changes**:
- [ ] Call wrapper's `setMaxZoom()` method
- [ ] Remove stub comment

---

### 4.4 Implement setOnMapClickListener() ⚠️ MEDIUM PRIORITY
**File**: `shared/src/iosMain/kotlin/com/worldwidewaves/shared/map/IosMapLibreAdapter.kt`
**Lines**: 228-233

**Target** (using direct callback):
```kotlin
override fun setOnMapClickListener(listener: ((Double, Double) -> Unit)?) {
    val wrapper = MapWrapperRegistry.getWrapper(eventId) as? MapLibreViewWrapper
    wrapper?.setOnMapClickCoordinate(listener)
}
```

**Changes**:
- [ ] Add `setOnMapClickCoordinate()` to MapLibreViewWrapper.swift
- [ ] Store listener in wrapper
- [ ] Call from `handleMapTap()` with coordinates

---

### 4.5 Implement addOnCameraIdleListener() ⚠️ MEDIUM PRIORITY
**File**: `shared/src/iosMain/kotlin/com/worldwidewaves/shared/map/IosMapLibreAdapter.kt`
**Lines**: 235-240

**Android** (line 203-206):
```kotlin
override fun addOnCameraIdleListener(callback: () -> Unit) {
    mapLibreMap?.addOnCameraIdleListener { callback() }
}
```

**iOS Target**:
```kotlin
override fun addOnCameraIdleListener(callback: () -> Unit) {
    val wrapper = MapWrapperRegistry.getWrapper(eventId) as? MapLibreViewWrapper
    wrapper?.setOnCameraIdleListener(callback)
}
```

**Swift Implementation Needed** (MapLibreViewWrapper.swift):
```swift
private var onCameraIdleCallback: (() -> Void)?

@objc public func setOnCameraIdleListener(_ callback: @escaping () -> Void) {
    self.onCameraIdleCallback = callback
}

// In regionDidChangeAnimated delegate:
public func mapView(_ mapView: MLNMapView, regionDidChangeAnimated animated: Bool) {
    onCameraIdle?()  // Existing
    onCameraIdleCallback?()  // New
}
```

**Changes**:
- [ ] Add Swift property and setter
- [ ] Invoke in `regionDidChangeAnimated` delegate
- [ ] Update Kotlin adapter to call wrapper

---

### 4.6 Implement drawOverridenBbox() 🔵 LOW PRIORITY
**File**: `shared/src/iosMain/kotlin/com/worldwidewaves/shared/map/IosMapLibreAdapter.kt`
**Lines**: 242-247

**Android** (line 409-444):
- Draws red outline polygon for debugging
- Shows constraint bounds visually

**Changes**:
- [ ] Add Swift method to draw debug polygon
- [ ] Use MLNLineStyleLayer with red stroke
- [ ] Store layer ID for cleanup

**Use Case**: Debugging constraint bounds

---

## 🎯 Phase 5: Fix Wrapper Lifecycle (HIGH PRIORITY - 1 day)

### 5.1 Fix View Controller Caching ✅ HIGH PRIORITY
**File**: `shared/src/iosMain/kotlin/com/worldwidewaves/shared/map/IosEventMap.kt`
**Lines**: 196-266

**Problem**: `downloadState.isAvailable` changes cause `when{}` branch switching → view disposal

**Current**:
```kotlin
when {
    styleURL != null && downloadState.isAvailable -> {
        key(event.id) {
            val viewController = remember(event.id) { ... }  // ❌ Still recreated when when{} switches
        }
    }
}
```

**Target** (already partially implemented):
```kotlin
// BEFORE when{}:
val viewController = remember(event.id) { mutableStateOf<UIViewController?>(null) }

LaunchedEffect(event.id, styleURL) {
    if (styleURL != null && viewController.value == null) {
        viewController.value = createNativeMapViewController(event, styleURL!!)
    }
}

when {
    viewController.value != null -> {
        UIKitViewController(factory = { viewController.value!! })
    }
}
```

**Status**: ✅ ALREADY IMPLEMENTED (commit 27dea9fe)

**Verify**: Check logs for zero deallocations during wave screen session

---

### 5.2 Add Wrapper Cleanup on Screen Exit ⚠️ MEDIUM PRIORITY
**File**: `shared/src/iosMain/kotlin/com/worldwidewaves/shared/map/IosEventMap.kt`

**Add**:
```kotlin
DisposableEffect(event.id) {
    onDispose {
        Log.i("IosEventMap", "Screen disposing, unregistering wrapper for: ${event.id}")
        MapWrapperRegistry.unregisterWrapper(event.id)
    }
}
```

**Changes**:
- [ ] Add `DisposableEffect` for cleanup
- [ ] Call `unregisterWrapper()` on dispose
- [ ] Verify wrapper deallocated only once at screen exit

---

## 🎯 Phase 6: Add Comprehensive Tests (MEDIUM PRIORITY - 2-3 days)

### Background
**Android Tests**: Full MapLibre SDK integration tests
**iOS Tests**: Trivial unit tests, no SDK integration

### 6.1 Create iOS MapLibre Integration Test Suite ⚠️ MEDIUM PRIORITY
**File**: `shared/src/iosTest/kotlin/com/worldwidewaves/shared/map/IosMapLibreIntegrationTest.kt` (CREATE NEW)

**Tests to Add**:

```kotlin
class IosMapLibreIntegrationTest {

    @Test
    fun `wrapper should survive entire screen session`() {
        // Create wrapper
        // Trigger download state changes
        // Verify wrapper not deallocated
    }

    @Test
    fun `wave polygons should render immediately`() {
        // Call updateWavePolygons()
        // Measure time to render
        // Assert < 50ms latency
    }

    @Test
    fun `camera commands should execute immediately`() {
        // Call animateCamera()
        // Verify command executed without polling
        // Assert < 50ms latency
    }

    @Test
    fun `map click callback should invoke reliably`() {
        // Register callback
        // Simulate tap gesture
        // Verify callback invoked
    }

    @Test
    fun `constraint bounds should apply after style loads`() {
        // Call setBoundsForCameraTarget() before style
        // Load style
        // Verify bounds applied
    }

    @Test
    fun `visible region should return actual bounds`() {
        // Pan map to specific region
        // Call getVisibleRegion()
        // Verify actual visible bounds returned, not fallback
    }

    @Test
    fun `continuous wave updates should all render`() {
        // Send 100 polygon updates
        // Count renders
        // Assert 100 renders (or close, allowing some frame skipping)
    }
}
```

**Changes**:
- [ ] Create test file
- [ ] Add MapLibre test setup helpers
- [ ] Add wrapper lifecycle test helpers
- [ ] Test all critical paths
- [ ] Achieve >80% code coverage

---

### 6.2 Add Wrapper Lifecycle Tests ⚠️ MEDIUM PRIORITY
**File**: `shared/src/iosTest/kotlin/com/worldwidewaves/shared/map/MapWrapperRegistryTest.kt` (CREATE NEW)

**Tests**:
```kotlin
@Test
fun `strong references should prevent premature deallocation`()

@Test
fun `unregisterWrapper should clean up on screen exit`()

@Test
fun `multiple events should not interfere with each other`()
```

---

### 6.3 Add Performance Tests ⚠️ MEDIUM PRIORITY
**File**: `shared/src/iosTest/kotlin/com/worldwidewaves/shared/map/IosMapPerformanceTest.kt` (CREATE NEW)

**Tests**:
```kotlin
@Test
fun `polygon updates should render within 50ms`()

@Test
fun `camera commands should execute within 50ms`()

@Test
fun `no CPU polling when idle`()
```

---

## 🎯 Phase 7: Additional Improvements (LOW PRIORITY - 2-3 days)

### 7.1 Implement Position Tracking StateFlow ⚠️ LOW PRIORITY
**File**: `shared/src/iosMain/kotlin/com/worldwidewaves/shared/map/IosMapLibreAdapter.kt`
**Lines**: 47-48

**Android**:
```kotlin
override val currentPosition: StateFlow<Position?> = _currentPosition.asStateFlow()
// Reactive updates via Flow
```

**iOS Current**:
```kotlin
private val _currentPosition = MutableStateFlow<Position?>(null)
override val currentPosition: StateFlow<Position?> = _currentPosition

// But never updated! Position changes don't flow
```

**Changes**:
- [ ] Add camera position delegate in Swift wrapper
- [ ] Call `IosMapLibreAdapter.updateCameraPosition()` on camera move
- [ ] Emit position updates to flow

---

### 7.2 Implement Location Component 🔵 LOW PRIORITY
**Android**: Full location component with user position marker (line 710-758)
**iOS**: Missing entirely

**Changes**:
- [ ] Research iOS MapLibre location component API
- [ ] Add user position marker to map
- [ ] Update marker on position changes
- [ ] Match Android visual style

---

### 7.3 Fix SwiftLint Warnings 🔵 LOW PRIORITY

**Remaining Warnings**:
- File length violations (SceneDelegate.swift, IOSMapBridge.swift, MapLibreViewWrapper.swift)
- Function body length (CompleteWaveParticipationUITest.swift)
- Blanket disable command (MapLibreViewWrapper.swift)

**Changes**:
- [ ] Split large files into smaller modules
- [ ] Extract helper methods from long functions
- [ ] Use specific swiftlint:disable directives

---

## 📋 Implementation Order (Recommended)

### Week 1: Critical Path (Fixes 3 User Issues)
**Goal**: Make iOS feature-complete for current use cases

1. **Day 1-2**: Phase 1.1-1.4 - Eliminate polling, direct dispatch
2. **Day 3**: Phase 2.1-2.2 - Fix map click
3. **Day 4**: Phase 3.1-3.2 - Fix constraint bounds
4. **Day 5**: Testing & bug fixes

**Outcome**: All 3 user issues resolved

### Week 2: Feature Parity & Quality
**Goal**: Match Android feature completeness

5. **Day 6-7**: Phase 4 - Implement missing adapter methods
6. **Day 8-9**: Phase 6 - Add comprehensive tests
7. **Day 10**: Phase 5.2, 7.1-7.3 - Polish & cleanup

**Outcome**: 100% feature parity with Android

---

## 🧪 Validation Checklist

After each phase, verify:

### Phase 1 Validation (Eliminate Polling)
- [ ] Grep logs: zero "Polling active" messages
- [ ] Grep logs: zero "setPendingPolygons" calls
- [ ] Grep logs: all "WaveObserver updates" match "Rendered polygons" count
- [ ] Run app: wave progresses smoothly in real-time
- [ ] Check wrapper lifecycle: zero deallocations during wave screen

### Phase 2 Validation (Map Click)
- [ ] Grep logs: "👆 Map tap detected" when user taps
- [ ] Grep logs: callback invoked successfully (100% success rate)
- [ ] Run app: tapping map navigates to full-screen map
- [ ] Verify: works on both event screen and wave screen

### Phase 3 Validation (Constraint Bounds)
- [ ] Grep logs: "✅ Constraint bounds applied successfully"
- [ ] Run app: cannot pan outside event area
- [ ] Run app: cannot zoom beyond min/max limits
- [ ] Test: bounds applied even if set before style loads

### Phase 4 Validation (Missing Features)
- [ ] All 12 adapter methods implemented (0 stubs)
- [ ] Zoom constraints enforced
- [ ] Camera idle listener fires
- [ ] Attribution margins adjustable

### Phase 6 Validation (Tests)
- [ ] All iOS MapLibre tests pass
- [ ] Code coverage >80% on iOS map layer
- [ ] Performance tests pass (<50ms latency)
- [ ] No memory leaks in lifecycle tests

---

## 📊 Effort Estimation

| Phase | Tasks | Complexity | Days | Priority |
|-------|-------|------------|------|----------|
| Phase 1: Eliminate Polling | 5 tasks | High | 2-3 | CRITICAL |
| Phase 2: Fix Map Click | 2 tasks | Medium | 1 | HIGH |
| Phase 3: Fix Constraint Bounds | 2 tasks | Medium | 1 | HIGH |
| Phase 4: Missing Features | 6 tasks | Medium | 3-5 | MEDIUM |
| Phase 5: Wrapper Lifecycle | 2 tasks | Low | 1 | MEDIUM |
| Phase 6: Comprehensive Tests | 3 tasks | Medium | 2-3 | MEDIUM |
| Phase 7: Polish | 3 tasks | Low | 1-2 | LOW |
| **TOTAL** | **23 tasks** | - | **11-16 days** | - |

**Critical Path** (Week 1): Phases 1-3 = 4-5 days to fix all user issues

---

## 🔍 Known Issues from Previous Sessions

### Issue Summary from NEXT_SESSION_iOS_MAP.md

**From /tmp/logs_10 Analysis**:

1. ✅ **Constraint Bounds**: getVisibleRegion() now implemented (commit 27dea9fe)
2. ⚠️ **Wrapper Deallocation**: View controller cached (commit 27dea9fe), but needs strong references
3. ❌ **Wave Progression**: Still 74 updates → 1 render (polling unreliable)
4. ❌ **Map Click**: Zero tap detections (gesture recognizer issues)

**Next Steps**: Phases 1-2 above directly address issues 2-4

---

## 📁 Key File Reference

### Files to Modify

**Phase 1 (Eliminate Polling)**:
- `shared/src/iosMain/kotlin/com/worldwidewaves/shared/map/IosEventMap.kt` (updateWavePolygons)
- `shared/src/iosMain/kotlin/com/worldwidewaves/shared/map/IosMapLibreAdapter.kt` (camera methods)
- `shared/src/iosMain/kotlin/com/worldwidewaves/shared/map/MapWrapperRegistry.kt` (strong refs)
- `iosApp/worldwidewaves/MapLibre/MapLibreViewWrapper.swift` (remove polling)

**Phase 2 (Map Click)**:
- `iosApp/worldwidewaves/MapLibre/MapLibreViewWrapper.swift` (direct callback)
- `shared/src/iosMain/kotlin/com/worldwidewaves/shared/map/IosEventMap.kt` (callback registration)

**Phase 3 (Constraint Bounds)**:
- `iosApp/worldwidewaves/MapLibre/MapLibreViewWrapper.swift` (queue bounds)

**Phase 4 (Missing Features)**:
- `shared/src/iosMain/kotlin/com/worldwidewaves/shared/map/IosMapLibreAdapter.kt` (all stubs)

**Phase 6 (Tests)**:
- Create `shared/src/iosTest/kotlin/com/worldwidewaves/shared/map/IosMapLibreIntegrationTest.kt`
- Create `shared/src/iosTest/kotlin/com/worldwidewaves/shared/map/MapWrapperRegistryTest.kt`
- Create `shared/src/iosTest/kotlin/com/worldwidewaves/shared/map/IosMapPerformanceTest.kt`

### Files to Reference (Don't Modify)

**Android Reference** (for implementation patterns):
- `composeApp/src/androidMain/kotlin/com/worldwidewaves/compose/map/AndroidEventMap.kt`
- `composeApp/src/androidMain/kotlin/com/worldwidewaves/map/AndroidMapLibreAdapter.kt`

**Shared Code**:
- `shared/src/commonMain/kotlin/com/worldwidewaves/shared/map/AbstractEventMap.kt`
- `shared/src/commonMain/kotlin/com/worldwidewaves/shared/map/MapLibreAdapter.kt`
- `shared/src/commonMain/kotlin/com/worldwidewaves/shared/map/MapBoundsEnforcer.kt`

---

## 🚀 Quick Start for Next Session

### Context
You're refactoring iOS MapLibre to match Android's architecture. Current iOS implementation uses broken polling/registry pattern. Need to eliminate polling and use direct dispatch.

### First Steps
1. Read this entire TODO document
2. Read the agent analysis report (created this session)
3. Read `/Users/ldiasdasilva/StudioProjects/WorldWideWaves/NEXT_SESSION_iOS_MAP.md`
4. Read `/Users/ldiasdasilva/StudioProjects/WorldWideWaves/ANDROID_VS_IOS_EVENTMAP_ANALYSIS.md`

### Start Here
**Phase 1.1**: Implement direct dispatch for wave polygons (highest impact)

**File**: `shared/src/iosMain/kotlin/com/worldwidewaves/shared/map/IosEventMap.kt:99-132`

**Replace**:
```kotlin
override fun updateWavePolygons(wavePolygons: List<Polygon>, clearPolygons: Boolean) {
    Log.i("IosEventMap", "🌊 updateWavePolygons: ${wavePolygons.size} polygons, clear=$clearPolygons")

    if (clearPolygons) {
        currentPolygons.clear()
    }
    currentPolygons.addAll(wavePolygons)

    // Store polygon data in registry for Swift to render
    storePolygonsForRendering(wavePolygons, clearPolygons)
    Log.v("IosEventMap", "✅ Polygons stored in registry")
}
```

**With**:
```kotlin
@OptIn(ExperimentalForeignApi::class)
override fun updateWavePolygons(wavePolygons: List<Polygon>, clearPolygons: Boolean) {
    Log.i("IosEventMap", "🌊 updateWavePolygons: ${wavePolygons.size} polygons, clear=$clearPolygons")

    if (clearPolygons) {
        currentPolygons.clear()
    }
    currentPolygons.addAll(wavePolygons)

    // Direct dispatch to main queue (like Android's runOnUiThread)
    dispatch_async(dispatch_get_main_queue()) {
        val wrapper = MapWrapperRegistry.getWrapper(event.id)
        if (wrapper != null) {
            // Convert to coordinate arrays for Swift
            val coordinates: List<List<Pair<Double, Double>>> = wavePolygons.map { polygon ->
                polygon.map { position -> Pair(position.lat, position.lng) }
            }

            // Call Swift method directly (no registry)
            IOSMapBridge.renderPolygonsDirectly(
                eventId = event.id,
                wrapper = wrapper,
                coordinates = coordinates,
                clearExisting = clearPolygons
            )
            Log.i("IosEventMap", "✅ Polygons dispatched to main queue for immediate rendering")
        } else {
            Log.e("IosEventMap", "❌ Wrapper not found for event: ${event.id}")
        }
    }
}
```

**Test**: Run app, watch wave progress continuously without stuttering

---

## 📝 Progress Tracking Template

Use this template to track progress in each session:

```markdown
## Session [DATE]

### Completed
- [ ] Phase 1.1: Direct dispatch for wave polygons
- [ ] Phase 1.2: Direct dispatch for camera commands
- [ ] Phase 1.3: Remove polling timer
- [ ] Phase 1.4: Strong references

### In Progress
- [ ] Current task...

### Blocked
- [ ] Any blockers...

### Next Session
- [ ] Next tasks to tackle...

### Testing Results
- [ ] All tests passing: X/X
- [ ] iOS feature parity: X/12 (X%)
- [ ] User issues resolved: X/3
```

---

## 🎓 Learning Resources

**MapLibre iOS SDK Documentation**:
- https://maplibre.org/maplibre-native/ios/latest/documentation/maplibre/
- MLNMapView API reference
- MLNMapViewDelegate callbacks
- Camera animation methods

**Kotlin/Native iOS Interop**:
- https://kotlinlang.org/docs/native-objc-interop.html
- dispatch_async with Kotlin/Native
- Memory management (strong vs weak references)

**Android Reference Implementation**:
- Study `AndroidEventMap.kt` for proven patterns
- Study `AndroidMapLibreAdapter.kt` for feature completeness

---

## 💡 Key Insights

1. **Registry pattern was architectural mistake**: Introduced to work around Kotlin→Swift communication, but causes more problems than it solves

2. **Direct dispatch is proven**: Android uses `context.runOnUiThread()`, iOS should use `dispatch_async(dispatch_get_main_queue())`

3. **Polling is never the answer**: 100ms polling cannot compete with 16ms frame time (60 FPS)

4. **Weak references inappropriate here**: Wrapper must survive entire screen session, not be GC'd arbitrarily

5. **Feature parity matters**: 33% implementation means 67% of functionality broken on iOS

---

## 🚨 Critical Warnings

⚠️ **DO NOT**:
- Add more polling mechanisms
- Increase polling frequency (wastes more CPU)
- Add more registry storage (architectural debt)
- Use weak references for critical objects

✅ **DO**:
- Follow Android's proven architecture
- Use direct dispatch for UI updates
- Store callbacks directly in wrapper
- Add comprehensive tests before refactoring
- Test each phase incrementally

---

## 📞 Support References

**Related Documentation**:
- `NEXT_SESSION_iOS_MAP.md` - Previous session context
- `ANDROID_VS_IOS_EVENTMAP_ANALYSIS.md` - Architecture comparison
- `iOS_MAP_IMPLEMENTATION_STATUS.md` - Historical status
- `docs/iOS_DEBUGGING_GUIDE.md` - Debugging techniques

**Git History**:
- Commit `27dea9fe`: getVisibleRegion implementation
- Commit `67cf5887`: BoundingBox fixes
- Commit `ab50d41d`: Wrapper lifecycle fix attempt

**Log Analysis**:
- `/tmp/logs_10` - Most recent test results
- Shows: padding=90×180 (world-size), wrapper deallocations, missing tap events

---

## 🎯 Success Criteria

**Phase 1 Complete When**:
- ✅ Wave updates render immediately (<50ms latency)
- ✅ Zero "Polling active" logs
- ✅ WaveObserver update count = Render count
- ✅ Wrapper survives entire screen session (zero deallocations)

**Phase 2 Complete When**:
- ✅ Map taps detected and logged
- ✅ Navigation to full-screen map works 100% of time
- ✅ Zero registry lookup failures

**Phase 3 Complete When**:
- ✅ Map constrained to Paris area (can't pan to London)
- ✅ Constraint bounds applied even before style loads
- ✅ All constraint commands succeed

**All Phases Complete When**:
- ✅ iOS feature parity: 12/12 (100%)
- ✅ All user issues resolved: 3/3
- ✅ Test coverage >80%
- ✅ Zero architectural inefficiencies
- ✅ iOS map behavior indistinguishable from Android

---

**Document Version**: 1.0
**Last Updated**: October 8, 2025
**Author**: Claude Code Analysis
**Next Review**: After Phase 1 completion
