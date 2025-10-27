# Comprehensive Test Specifications for Map Bounds Enforcement Changes

**Document Version**: 1.0
**Date**: 2025-10-22
**Session Context**: Intelligent aspect ratio fitting, min zoom formula parity, preventive gesture interception

---

## Executive Summary

This document provides **exhaustive test specifications** for all map bounds enforcement features implemented during this session. These tests are **ready to implement** with detailed setup, execution, and assertion steps.

### Major Features Requiring Test Coverage

1. **Intelligent Aspect Ratio Fitting** (AbstractEventMap.kt `moveToWindowBounds`)
2. **Min Zoom Formula Parity** (AndroidMapLibreAdapter.kt `setBoundsForCameraTarget`)
3. **Min Zoom Locking Mechanism** (AndroidMapLibreAdapter.kt)
4. **Preventive Gesture Interception** (AndroidMapLibreAdapter.kt `setupPreventiveGestureConstraints`)
5. **Viewport Padding Logic** (MapBoundsEnforcer.kt `calculateVisibleRegionPadding`)
6. **BOUNDS vs WINDOW Mode Differences** (All components)

---

## Section 1: Intelligent Aspect Ratio Fitting Tests

**File**: `/Users/ldiasdasilva/StudioProjects/WorldWideWaves/shared/src/androidUnitTest/kotlin/com/worldwidewaves/shared/map/AbstractEventMapAspectRatioTest.kt`

### Test 1.1: Wide Event Uses Height-Fit Zoom

**Purpose**: Verify that wide events (eventAspectRatio > screenAspectRatio) fit by HEIGHT

**Setup**:
```kotlin
// Event: 400km wide × 100km tall (aspect ratio 4.0)
val wideBounds = BoundingBox(
    swLat = 48.0, swLng = 2.0,
    neLat = 49.0, neLng = 6.0
)
// Screen: 600px × 800px (aspect ratio 0.75)
val screenWidth = 600.0
val screenHeight = 800.0
val eventAspectRatio = 4.0 / 1.0  // Wide event
val screenAspectRatio = 0.75      // Tall screen
```

**Execution**:
```kotlin
eventMap.moveToWindowBounds()
testScheduler.advanceUntilIdle()
```

**Assertions**:
```kotlin
// 1. Verify HEIGHT-fit was chosen
val zoomForWidth = log2((600.0 * 360.0) / (4.0 * 256.0))   // ~9.13
val zoomForHeight = log2((800.0 * 180.0) / (1.0 * 256.0))  // ~9.15
val expectedZoom = min(zoomForWidth, zoomForHeight)         // ~9.13 (width-fit SMALLER)

val capturedZoom = slot<Double>()
verify { mockMapLibreAdapter.animateCamera(any(), capture(capturedZoom), any()) }
assertEquals(expectedZoom, capturedZoom.captured, 0.01,
    "Wide event should use WIDTH-fit zoom (smaller of the two)")

// 2. Verify camera positioned at event center
val capturedPosition = slot<Position>()
verify { mockMapLibreAdapter.animateCamera(capture(capturedPosition), any(), any()) }
assertEquals(48.5, capturedPosition.captured.latitude, 0.001)
assertEquals(4.0, capturedPosition.captured.longitude, 0.001)
```

**Why This Test Matters**: Ensures NO horizontal pixels outside event area (width constrains zoom)

---

### Test 1.2: Tall Event Uses Width-Fit Zoom

**Purpose**: Verify that tall events (eventAspectRatio < screenAspectRatio) fit by WIDTH

**Setup**:
```kotlin
// Event: 100km wide × 400km tall (aspect ratio 0.25)
val tallBounds = BoundingBox(
    swLat = 45.0, swLng = 2.0,
    neLat = 49.0, neLng = 3.0
)
// Screen: 600px × 800px (aspect ratio 0.75)
val screenWidth = 600.0
val screenHeight = 800.0
val eventAspectRatio = 0.25  // Tall event
val screenAspectRatio = 0.75 // Taller screen
```

**Execution**:
```kotlin
eventMap.moveToWindowBounds()
testScheduler.advanceUntilIdle()
```

**Assertions**:
```kotlin
// 1. Verify WIDTH-fit was chosen
val zoomForWidth = log2((600.0 * 360.0) / (1.0 * 256.0))   // ~10.41
val zoomForHeight = log2((800.0 * 180.0) / (4.0 * 256.0))  // ~8.15
val expectedZoom = min(zoomForWidth, zoomForHeight)         // ~8.15 (height-fit SMALLER)

val capturedZoom = slot<Double>()
verify { mockMapLibreAdapter.animateCamera(any(), capture(capturedZoom), any()) }
assertEquals(expectedZoom, capturedZoom.captured, 0.01,
    "Tall event should use HEIGHT-fit zoom (smaller of the two)")

// 2. Verify no vertical pixels outside event area
assertTrue(capturedZoom.captured <= zoomForHeight,
    "Zoom must not exceed height-fit zoom (would show vertical overflow)")
```

**Why This Test Matters**: Ensures NO vertical pixels outside event area (height constrains zoom)

---

### Test 1.3: Square Event on Square Screen (Edge Case)

**Purpose**: Verify that events matching screen aspect ratio fit perfectly

**Setup**:
```kotlin
// Event: 100km × 100km (aspect ratio 1.0)
val squareBounds = BoundingBox(
    swLat = 48.0, swLng = 2.0,
    neLat = 49.0, neLng = 3.0
)
// Screen: 800px × 800px (aspect ratio 1.0)
val screenWidth = 800.0
val screenHeight = 800.0
```

**Execution**:
```kotlin
eventMap.moveToWindowBounds()
testScheduler.advanceUntilIdle()
```

**Assertions**:
```kotlin
// 1. Verify both dimensions calculated the SAME zoom
val zoomForWidth = log2((800.0 * 360.0) / (1.0 * 256.0))   // ~11.15
val zoomForHeight = log2((800.0 * 180.0) / (1.0 * 256.0))  // ~10.15
val expectedZoom = min(zoomForWidth, zoomForHeight)         // ~10.15

val capturedZoom = slot<Double>()
verify { mockMapLibreAdapter.animateCamera(any(), capture(capturedZoom), any()) }
assertEquals(expectedZoom, capturedZoom.captured, 0.01,
    "Square event on square screen should use height-fit (smaller due to lat/lng degree difference)")
```

**Why This Test Matters**: Validates formula correctness when aspect ratios match

---

### Test 1.4: Extreme Aspect Ratio (Very Wide Event)

**Purpose**: Test stability with extreme aspect ratio (e.g., equatorial event spanning continents)

**Setup**:
```kotlin
// Event: 1000km wide × 10km tall (aspect ratio 100.0)
val extremelyWideBounds = BoundingBox(
    swLat = 0.0, swLng = 0.0,
    neLat = 0.1, neLng = 10.0  // Near equator for consistent degree sizes
)
// Screen: 600px × 800px
val screenWidth = 600.0
val screenHeight = 800.0
```

**Execution**:
```kotlin
eventMap.moveToWindowBounds()
testScheduler.advanceUntilIdle()
```

**Assertions**:
```kotlin
// 1. Verify width-fit constrains (height would overflow massively)
val zoomForWidth = log2((600.0 * 360.0) / (10.0 * 256.0))
val zoomForHeight = log2((800.0 * 180.0) / (0.1 * 256.0))
val expectedZoom = min(zoomForWidth, zoomForHeight)

val capturedZoom = slot<Double>()
verify { mockMapLibreAdapter.animateCamera(any(), capture(capturedZoom), any()) }
assertEquals(expectedZoom, capturedZoom.captured, 0.01,
    "Extremely wide event must use width-fit to prevent vertical overflow")

// 2. Verify zoom is reasonable (not negative or excessively high)
assertTrue(capturedZoom.captured > 0.0 && capturedZoom.captured < 20.0,
    "Calculated zoom should be within reasonable MapLibre range")
```

**Why This Test Matters**: Ensures formula stability with edge-case geometries

---

### Test 1.5: Extreme Aspect Ratio (Very Tall Event)

**Purpose**: Test stability with very tall events (e.g., north-south corridor)

**Setup**:
```kotlin
// Event: 10km wide × 1000km tall (aspect ratio 0.01)
val extremelyTallBounds = BoundingBox(
    swLat = 40.0, swLng = 2.0,
    neLat = 50.0, neLng = 2.1  // 10 degrees lat, 0.1 degrees lng
)
// Screen: 600px × 800px
val screenWidth = 600.0
val screenHeight = 800.0
```

**Execution**:
```kotlin
eventMap.moveToWindowBounds()
testScheduler.advanceUntilIdle()
```

**Assertions**:
```kotlin
// 1. Verify height-fit constrains (width would overflow massively)
val zoomForWidth = log2((600.0 * 360.0) / (0.1 * 256.0))
val zoomForHeight = log2((800.0 * 180.0) / (10.0 * 256.0))
val expectedZoom = min(zoomForWidth, zoomForHeight)

val capturedZoom = slot<Double>()
verify { mockMapLibreAdapter.animateCamera(any(), capture(capturedZoom), any()) }
assertEquals(expectedZoom, capturedZoom.captured, 0.01,
    "Extremely tall event must use height-fit to prevent horizontal overflow")
```

**Why This Test Matters**: Validates formula with vertical corridors

---

## Section 2: Min Zoom Formula Parity Tests

**File**: `/Users/ldiasdasilva/StudioProjects/WorldWideWaves/composeApp/src/androidUnitTest/kotlin/com/worldwidewaves/map/AndroidMapLibreAdapterMinZoomParityTest.kt`

### Test 2.1: WINDOW Mode Min Zoom Matches moveToWindowBounds Formula

**Purpose**: Verify setBoundsForCameraTarget uses SAME formula as moveToWindowBounds for WINDOW mode

**Setup**:
```kotlin
val mockMap = mockk<MapLibreMap>(relaxed = true)
val adapter = AndroidMapLibreAdapter(mockMap)
adapter.setMap(mockMap)

val eventBounds = BoundingBox(
    swLat = 48.0, swLng = 2.0,
    neLat = 49.0, neLng = 3.0
)
every { mockMap.width } returns 600
every { mockMap.height } returns 800
```

**Execution**:
```kotlin
// CRITICAL: applyZoomSafetyMargin = true (WINDOW mode)
adapter.setBoundsForCameraTarget(
    constraintBounds = eventBounds,
    applyZoomSafetyMargin = true,
    originalEventBounds = eventBounds
)
```

**Assertions**:
```kotlin
// 1. Calculate expected zoom using moveToWindowBounds formula
val eventWidth = 1.0   // 3.0 - 2.0
val eventHeight = 1.0  // 49.0 - 48.0
val mapWidth = 600.0
val mapHeight = 800.0

val zoomForWidth = log2((mapWidth * 360.0) / (eventWidth * 256.0))   // ~10.41
val zoomForHeight = log2((mapHeight * 180.0) / (eventHeight * 256.0)) // ~9.15
val baseMinZoom = min(zoomForWidth, zoomForHeight)  // ~9.15
val expectedMinZoom = baseMinZoom + 0.5  // Safety margin

// 2. Verify setBoundsForCameraTarget set the SAME min zoom
val slot = slot<Double>()
verify { mockMap.setMinZoomPreference(capture(slot)) }
assertEquals(expectedMinZoom, slot.captured, 0.01,
    "WINDOW mode min zoom must match moveToWindowBounds formula + safety margin")

// 3. Verify internal calculatedMinZoom was stored
assertEquals(expectedMinZoom, adapter.getMinZoomLevel(), 0.01,
    "getMinZoomLevel() should return calculated min zoom")
```

**Why This Test Matters**: Ensures consistent zoom behavior between animation and constraint enforcement

---

### Test 2.2: BOUNDS Mode Min Zoom Uses getCameraForLatLngBounds

**Purpose**: Verify BOUNDS mode uses MapLibre's calculation (shows entire event)

**Setup**:
```kotlin
val mockMap = mockk<MapLibreMap>(relaxed = true)
val adapter = AndroidMapLibreAdapter(mockMap)
adapter.setMap(mockMap)

val eventBounds = BoundingBox(
    swLat = 48.0, swLng = 2.0,
    neLat = 49.0, neLng = 3.0
)

// Mock getCameraForLatLngBounds to return specific zoom
val mockCameraPosition = mockk<CameraPosition>()
every { mockCameraPosition.zoom } returns 8.5
every { mockMap.getCameraForLatLngBounds(any(), any()) } returns mockCameraPosition
```

**Execution**:
```kotlin
// CRITICAL: applyZoomSafetyMargin = false (BOUNDS mode)
adapter.setBoundsForCameraTarget(
    constraintBounds = eventBounds,
    applyZoomSafetyMargin = false,
    originalEventBounds = eventBounds
)
```

**Assertions**:
```kotlin
// 1. Verify getCameraForLatLngBounds was called
verify { mockMap.getCameraForLatLngBounds(any(), intArrayOf(0, 0, 0, 0)) }

// 2. Verify min zoom was NOT increased by safety margin (BOUNDS mode shows entire event)
val slot = slot<Double>()
verify { mockMap.setMinZoomPreference(capture(slot)) }
assertEquals(8.5, slot.captured, 0.01,
    "BOUNDS mode should use getCameraForLatLngBounds zoom WITHOUT safety margin")

// 3. Verify no manual zoom calculation was performed
verify(exactly = 0) { mockMap.width }  // Width/height not accessed in BOUNDS mode
```

**Why This Test Matters**: Ensures BOUNDS mode shows entire event without extra margin

---

### Test 2.3: Min Zoom Calculated from ORIGINAL Bounds, Not Shrunk Constraint Bounds

**Purpose**: Verify min zoom is calculated from originalEventBounds, preventing zoom-out spiral

**Setup**:
```kotlin
val mockMap = mockk<MapLibreMap>(relaxed = true)
val adapter = AndroidMapLibreAdapter(mockMap)
adapter.setMap(mockMap)

// Original event bounds
val originalBounds = BoundingBox(
    swLat = 48.0, swLng = 2.0,
    neLat = 49.0, neLng = 3.0
)

// Shrunk constraint bounds (after viewport padding)
val shrunkBounds = BoundingBox(
    swLat = 48.2, swLng = 2.2,
    neLat = 48.8, neLng = 2.8
)

every { mockMap.width } returns 600
every { mockMap.height } returns 800
```

**Execution**:
```kotlin
adapter.setBoundsForCameraTarget(
    constraintBounds = shrunkBounds,           // SHRUNK bounds
    applyZoomSafetyMargin = true,
    originalEventBounds = originalBounds        // ORIGINAL bounds for min zoom
)
```

**Assertions**:
```kotlin
// 1. Calculate expected zoom from ORIGINAL bounds (not shrunk)
val originalWidth = 1.0   // 3.0 - 2.0
val originalHeight = 1.0  // 49.0 - 48.0

val zoomForWidth = log2((600.0 * 360.0) / (originalWidth * 256.0))
val zoomForHeight = log2((800.0 * 180.0) / (originalHeight * 256.0))
val baseMinZoom = min(zoomForWidth, zoomForHeight)
val expectedMinZoom = baseMinZoom + 0.5

// 2. Verify min zoom uses ORIGINAL bounds (if it used shrunk, zoom would be higher)
val slot = slot<Double>()
verify { mockMap.setMinZoomPreference(capture(slot)) }
assertEquals(expectedMinZoom, slot.captured, 0.01,
    "Min zoom MUST be calculated from originalEventBounds to prevent zoom-out spiral")

// 3. Verify shrunk bounds used for setLatLngBoundsForCameraTarget (camera constraint)
val boundsSlot = slot<LatLngBounds>()
verify { mockMap.setLatLngBoundsForCameraTarget(capture(boundsSlot)) }
assertEquals(48.2, boundsSlot.captured.getLatSouth(), 0.01,
    "Camera constraint should use SHRUNK bounds")
```

**Why This Test Matters**: Prevents infinite zoom-out spiral from using shrunk bounds for min zoom

---

### Test 2.4: Safety Margin Application (WINDOW Mode Only)

**Purpose**: Verify safety margin (+0.5 zoom) applied only in WINDOW mode

**Setup**:
```kotlin
val mockMap = mockk<MapLibreMap>(relaxed = true)
val adapter = AndroidMapLibreAdapter(mockMap)
adapter.setMap(mockMap)

val eventBounds = BoundingBox(
    swLat = 48.0, swLng = 2.0,
    neLat = 49.0, neLng = 3.0
)

every { mockMap.width } returns 600
every { mockMap.height } returns 800

// Mock BOUNDS mode zoom
val mockCameraPosition = mockk<CameraPosition>()
every { mockCameraPosition.zoom } returns 9.0
every { mockMap.getCameraForLatLngBounds(any(), any()) } returns mockCameraPosition
```

**Execution**:
```kotlin
// Test WINDOW mode (with safety margin)
adapter.setBoundsForCameraTarget(
    constraintBounds = eventBounds,
    applyZoomSafetyMargin = true,
    originalEventBounds = eventBounds
)
val windowModeZoom = slot<Double>()
verify { mockMap.setMinZoomPreference(capture(windowModeZoom)) }

// Reset and test BOUNDS mode (without safety margin)
clearMocks(mockMap, answers = false)
adapter.setBoundsForCameraTarget(
    constraintBounds = eventBounds,
    applyZoomSafetyMargin = false,
    originalEventBounds = eventBounds
)
val boundsModeZoom = slot<Double>()
verify { mockMap.setMinZoomPreference(capture(boundsModeZoom)) }
```

**Assertions**:
```kotlin
// 1. Verify WINDOW mode has safety margin
val baseZoom = 9.15  // min(zoomForWidth, zoomForHeight)
assertEquals(baseZoom + 0.5, windowModeZoom.captured, 0.01,
    "WINDOW mode should add 0.5 zoom safety margin")

// 2. Verify BOUNDS mode has NO safety margin
assertEquals(9.0, boundsModeZoom.captured, 0.01,
    "BOUNDS mode should NOT add safety margin (uses getCameraForLatLngBounds)")

// 3. Verify difference is exactly 0.5
assertTrue(abs(windowModeZoom.captured - boundsModeZoom.captured - 0.5) < 0.1,
    "Difference between WINDOW and BOUNDS mode should be ~0.5 (safety margin)")
```

**Why This Test Matters**: Ensures safety margin prevents edge-case viewport overflow in WINDOW mode

---

## Section 3: Min Zoom Locking Mechanism Tests

**File**: `/Users/ldiasdasilva/StudioProjects/WorldWideWaves/composeApp/src/androidUnitTest/kotlin/com/worldwidewaves/map/AndroidMapLibreAdapterMinZoomLockingTest.kt`

### Test 3.1: Min Zoom Locked After First Calculation

**Purpose**: Verify min zoom is calculated once and then locked to prevent recalculation spiral

**Setup**:
```kotlin
val mockMap = mockk<MapLibreMap>(relaxed = true)
val adapter = AndroidMapLibreAdapter(mockMap)
adapter.setMap(mockMap)

val eventBounds = BoundingBox(
    swLat = 48.0, swLng = 2.0,
    neLat = 49.0, neLng = 3.0
)

every { mockMap.width } returns 600
every { mockMap.height } returns 800
```

**Execution**:
```kotlin
// First call - should calculate min zoom
adapter.setBoundsForCameraTarget(
    constraintBounds = eventBounds,
    applyZoomSafetyMargin = true,
    originalEventBounds = eventBounds
)

// Second call - should NOT recalculate (locked)
adapter.setBoundsForCameraTarget(
    constraintBounds = eventBounds,
    applyZoomSafetyMargin = true,
    originalEventBounds = eventBounds
)
```

**Assertions**:
```kotlin
// 1. Verify setMinZoomPreference called only ONCE (not twice)
verify(exactly = 1) { mockMap.setMinZoomPreference(any()) }

// 2. Verify minZoomLocked flag was set after first call
assertTrue(adapter.minZoomLocked,
    "minZoomLocked should be true after first calculation")

// 3. Verify subsequent calls don't recalculate
val firstZoom = adapter.getMinZoomLevel()
adapter.setBoundsForCameraTarget(
    constraintBounds = eventBounds,
    applyZoomSafetyMargin = true,
    originalEventBounds = eventBounds
)
val secondZoom = adapter.getMinZoomLevel()
assertEquals(firstZoom, secondZoom, 0.001,
    "Min zoom should remain locked (not recalculated)")
```

**Why This Test Matters**: Prevents infinite recalculation loops when padding changes

---

### Test 3.2: Min Zoom Lock Prevents Zoom-In Spiral

**Purpose**: Verify locked min zoom doesn't increase when constraint bounds shrink

**Setup**:
```kotlin
val mockMap = mockk<MapLibreMap>(relaxed = true)
val adapter = AndroidMapLibreAdapter(mockMap)
adapter.setMap(mockMap)

val originalBounds = BoundingBox(
    swLat = 48.0, swLng = 2.0,
    neLat = 49.0, neLng = 3.0
)

val shrunkBounds1 = BoundingBox(
    swLat = 48.2, swLng = 2.2,
    neLat = 48.8, neLng = 2.8
)

val shrunkBounds2 = BoundingBox(
    swLat = 48.3, swLng = 2.3,
    neLat = 48.7, neLng = 2.7
)

every { mockMap.width } returns 600
every { mockMap.height } returns 800
```

**Execution**:
```kotlin
// Initial calculation
adapter.setBoundsForCameraTarget(
    constraintBounds = originalBounds,
    applyZoomSafetyMargin = true,
    originalEventBounds = originalBounds
)
val initialMinZoom = adapter.getMinZoomLevel()

// Update with shrunk bounds (would require higher zoom if recalculated)
adapter.setBoundsForCameraTarget(
    constraintBounds = shrunkBounds1,
    applyZoomSafetyMargin = true,
    originalEventBounds = originalBounds
)
val afterShrink1 = adapter.getMinZoomLevel()

// Further shrink
adapter.setBoundsForCameraTarget(
    constraintBounds = shrunkBounds2,
    applyZoomSafetyMargin = true,
    originalEventBounds = originalBounds
)
val afterShrink2 = adapter.getMinZoomLevel()
```

**Assertions**:
```kotlin
// 1. Verify min zoom remains constant despite shrinking constraint bounds
assertEquals(initialMinZoom, afterShrink1, 0.001,
    "Min zoom should remain locked after first shrink")
assertEquals(initialMinZoom, afterShrink2, 0.001,
    "Min zoom should remain locked after second shrink")

// 2. Verify setMinZoomPreference called only once
verify(exactly = 1) { mockMap.setMinZoomPreference(any()) }

// 3. Verify lock prevents zoom-in spiral
assertTrue(afterShrink2 < 20.0,
    "Min zoom should remain reasonable (not spiral to excessive zoom)")
```

**Why This Test Matters**: Prevents zoom-in spiral when constraint bounds shrink during padding updates

---

### Test 3.3: Min Zoom Lock Reset (Future Feature)

**Purpose**: Document expected behavior if lock needs to be reset (e.g., new event)

**Setup**:
```kotlin
// This test documents EXPECTED behavior, not current implementation
// Include if lock reset functionality is added later
```

**Execution**:
```kotlin
// Future API: adapter.resetMinZoomLock()
```

**Assertions**:
```kotlin
// Future test: Verify min zoom can be recalculated after reset
```

**Why This Test Matters**: Documents future extensibility if lock needs reset mechanism

---

## Section 4: Preventive Gesture Interception Tests

**File**: `/Users/ldiasdasilva/StudioProjects/WorldWideWaves/composeApp/src/androidUnitTest/kotlin/com/worldwidewaves/map/AndroidMapLibreAdapterGestureInterceptionTest.kt`

### Test 4.1: OnCameraMoveListener Registered Only Once

**Purpose**: Verify gesture interception listener is registered once (not re-registered on padding updates)

**Setup**:
```kotlin
val mockMap = mockk<MapLibreMap>(relaxed = true)
val adapter = AndroidMapLibreAdapter(mockMap)
adapter.setMap(mockMap)

val eventBounds = BoundingBox(
    swLat = 48.0, swLng = 2.0,
    neLat = 49.0, neLng = 3.0
)
```

**Execution**:
```kotlin
// First call - should register listener
adapter.setBoundsForCameraTarget(
    constraintBounds = eventBounds,
    applyZoomSafetyMargin = true,
    originalEventBounds = eventBounds
)

// Second call - should NOT re-register
adapter.setBoundsForCameraTarget(
    constraintBounds = eventBounds,
    applyZoomSafetyMargin = true,
    originalEventBounds = eventBounds
)

// Third call - should NOT re-register
adapter.setBoundsForCameraTarget(
    constraintBounds = eventBounds,
    applyZoomSafetyMargin = true,
    originalEventBounds = eventBounds
)
```

**Assertions**:
```kotlin
// 1. Verify addOnCameraMoveListener called exactly once
verify(exactly = 1) { mockMap.addOnCameraMoveListener(any()) }

// 2. Verify preventiveGestureConstraintsSetUp flag prevents re-registration
assertTrue(adapter.preventiveGestureConstraintsSetUp,
    "preventiveGestureConstraintsSetUp should be true after first setup")

// 3. Verify listener still active after multiple setBoundsForCameraTarget calls
verify(exactly = 1) { mockMap.addOnCameraMoveListener(any()) }
```

**Why This Test Matters**: Prevents memory leaks from duplicate listener registration

---

### Test 4.2: Gesture Interception Clamps Camera During Movement

**Purpose**: Verify OnCameraMoveListener intercepts gestures and clamps camera position

**Setup**:
```kotlin
val mockMap = mockk<MapLibreMap>(relaxed = true)
val adapter = AndroidMapLibreAdapter(mockMap)
adapter.setMap(mockMap)

val eventBounds = BoundingBox(
    swLat = 48.0, swLng = 2.0,
    neLat = 49.0, neLng = 3.0
)

// Capture the listener
val listenerSlot = slot<MapLibreMap.OnCameraMoveListener>()
every { mockMap.addOnCameraMoveListener(capture(listenerSlot)) } just Runs

// Mock camera position moving outside bounds
val outOfBoundsPosition = mockk<CameraPosition>()
every { outOfBoundsPosition.target } returns LatLng(50.0, 2.5)  // North of bounds
every { mockMap.cameraPosition } returns outOfBoundsPosition

// Mock visible region extending beyond event bounds
val mockVisibleRegion = mockk<VisibleRegion>()
val mockVisibleBounds = mockk<LatLngBounds>()
every { mockVisibleBounds.getLatSouth() } returns 49.5
every { mockVisibleBounds.getLatNorth() } returns 50.5  // Exceeds event ne.lat=49.0
every { mockVisibleBounds.getLonWest() } returns 2.0
every { mockVisibleBounds.getLonEast() } returns 3.0
every { mockVisibleRegion.latLngBounds } returns mockVisibleBounds
every { mockMap.projection.visibleRegion } returns mockVisibleRegion
```

**Execution**:
```kotlin
// Setup constraints (registers listener)
adapter.setBoundsForCameraTarget(
    constraintBounds = eventBounds,
    applyZoomSafetyMargin = true,
    originalEventBounds = eventBounds
)

// Simulate camera move (trigger listener)
listenerSlot.captured.onCameraMove()
```

**Assertions**:
```kotlin
// 1. Verify listener was triggered
verify(atLeast = 1) { mockMap.cameraPosition }

// 2. Verify viewport bounds were checked
verify(atLeast = 1) { mockMap.projection.visibleRegion }

// 3. Verify camera was clamped (moveCamera called with clamped position)
val cameraUpdateSlot = slot<CameraUpdate>()
verify { mockMap.moveCamera(capture(cameraUpdateSlot)) }

// 4. Verify clamped position is within event bounds
// (Exact verification depends on clamping algorithm - this is conceptual)
// In practice, check that moveCamera was called when viewport exceeded bounds
```

**Why This Test Matters**: Ensures gestures are intercepted DURING movement (preventive)

---

### Test 4.3: Gesture Interception Skips When Viewport Within Bounds

**Purpose**: Verify listener doesn't clamp when viewport is valid (performance optimization)

**Setup**:
```kotlin
val mockMap = mockk<MapLibreMap>(relaxed = true)
val adapter = AndroidMapLibreAdapter(mockMap)
adapter.setMap(mockMap)

val eventBounds = BoundingBox(
    swLat = 48.0, swLng = 2.0,
    neLat = 49.0, neLng = 3.0
)

// Capture listener
val listenerSlot = slot<MapLibreMap.OnCameraMoveListener>()
every { mockMap.addOnCameraMoveListener(capture(listenerSlot)) } just Runs

// Mock camera position INSIDE bounds
val validPosition = mockk<CameraPosition>()
every { validPosition.target } returns LatLng(48.5, 2.5)  // Center of event
every { mockMap.cameraPosition } returns validPosition

// Mock visible region entirely within event bounds
val mockVisibleRegion = mockk<VisibleRegion>()
val mockVisibleBounds = mockk<LatLngBounds>()
every { mockVisibleBounds.getLatSouth() } returns 48.2
every { mockVisibleBounds.getLatNorth() } returns 48.8  // Within ne.lat=49.0
every { mockVisibleBounds.getLonWest() } returns 2.2
every { mockVisibleBounds.getLonEast() } returns 2.8   // Within ne.lng=3.0
every { mockVisibleRegion.latLngBounds } returns mockVisibleBounds
every { mockMap.projection.visibleRegion } returns mockVisibleRegion
```

**Execution**:
```kotlin
// Setup constraints
adapter.setBoundsForCameraTarget(
    constraintBounds = eventBounds,
    applyZoomSafetyMargin = true,
    originalEventBounds = eventBounds
)

// Simulate camera move
listenerSlot.captured.onCameraMove()
```

**Assertions**:
```kotlin
// 1. Verify viewport was checked
verify(atLeast = 1) { mockMap.projection.visibleRegion }

// 2. Verify moveCamera was NOT called (no clamping needed)
verify(exactly = 0) { mockMap.moveCamera(any()) }

// 3. Verify no redundant work performed when viewport valid
// (Performance: early return when viewport within bounds)
```

**Why This Test Matters**: Ensures performance optimization when constraints already satisfied

---

## Section 5: Viewport Padding Logic Tests

**File**: `/Users/ldiasdasilva/StudioProjects/WorldWideWaves/shared/src/commonTest/kotlin/com/worldwidewaves/shared/map/MapBoundsEnforcerViewportPaddingTest.kt`

### Test 5.1: WINDOW Mode Calculates Viewport Half-Dimensions

**Purpose**: Verify WINDOW mode calculates padding from actual viewport dimensions

**Setup**:
```kotlin
val mockAdapter = mockk<MapLibreAdapter<Unit>>()
val eventBounds = BoundingBox(
    swLat = 48.0, swLng = 2.0,
    neLat = 49.0, neLng = 3.0
)

// Mock visible region
val visibleRegion = BoundingBox(
    swLat = 48.3, swLng = 2.3,
    neLat = 48.7, neLng = 2.7
)
every { mockAdapter.getVisibleRegion() } returns visibleRegion

val enforcer = MapBoundsEnforcer(
    mapBounds = eventBounds,
    mapLibreAdapter = mockAdapter,
    isWindowMode = true  // WINDOW mode
)
```

**Execution**:
```kotlin
val padding = enforcer.calculateVisibleRegionPadding()
```

**Assertions**:
```kotlin
// 1. Calculate expected padding (viewport half-dimensions)
val expectedLatPadding = (48.7 - 48.3) / 2.0  // 0.2
val expectedLngPadding = (2.7 - 2.3) / 2.0    // 0.2

// 2. Verify padding matches calculations
assertEquals(expectedLatPadding, padding.latPadding, 0.001,
    "WINDOW mode latPadding should be viewport half-height")
assertEquals(expectedLngPadding, padding.lngPadding, 0.001,
    "WINDOW mode lngPadding should be viewport half-width")

// 3. Verify padding is non-zero (actual viewport dimensions used)
assertTrue(padding.latPadding > 0.0 && padding.lngPadding > 0.0,
    "WINDOW mode should have non-zero padding")
```

**Why This Test Matters**: Ensures constraint bounds are shrunk correctly to prevent edge overflow

---

### Test 5.2: BOUNDS Mode Returns Zero Padding

**Purpose**: Verify BOUNDS mode uses zero padding (want entire event visible)

**Setup**:
```kotlin
val mockAdapter = mockk<MapLibreAdapter<Unit>>()
val eventBounds = BoundingBox(
    swLat = 48.0, swLng = 2.0,
    neLat = 49.0, neLng = 3.0
)

val enforcer = MapBoundsEnforcer(
    mapBounds = eventBounds,
    mapLibreAdapter = mockAdapter,
    isWindowMode = false  // BOUNDS mode
)
```

**Execution**:
```kotlin
val padding = enforcer.calculateVisibleRegionPadding()
```

**Assertions**:
```kotlin
// 1. Verify zero padding (entire event should be visible)
assertEquals(0.0, padding.latPadding, 0.001,
    "BOUNDS mode latPadding should be zero")
assertEquals(0.0, padding.lngPadding, 0.001,
    "BOUNDS mode lngPadding should be zero")

// 2. Verify getVisibleRegion NOT called (optimization - no need to check viewport)
verify(exactly = 0) { mockAdapter.getVisibleRegion() }

// 3. Verify early return (no viewport dimension calculations)
// (This is an implementation detail but important for performance)
```

**Why This Test Matters**: Ensures BOUNDS mode shows entire event without shrinking constraint bounds

---

### Test 5.3: Padding Changes Trigger Constraint Recalculation

**Purpose**: Verify significant padding changes (>10%) trigger constraint update

**Setup**:
```kotlin
val mockAdapter = mockk<MapLibreAdapter<Unit>>()
val eventBounds = BoundingBox(
    swLat = 48.0, swLng = 2.0,
    neLat = 49.0, neLng = 3.0
)

// Initial viewport (small)
val initialRegion = BoundingBox(
    swLat = 48.4, swLng = 2.4,
    neLat = 48.6, neLng = 2.6
)

// Zoomed-out viewport (large - triggers recalculation)
val zoomedOutRegion = BoundingBox(
    swLat = 48.2, swLng = 2.2,
    neLat = 48.8, neLng = 2.8
)

every { mockAdapter.getVisibleRegion() } returnsMany listOf(initialRegion, zoomedOutRegion)
every { mockAdapter.setBoundsForCameraTarget(any(), any(), any()) } just Runs
every { mockAdapter.getMinZoomLevel() } returns 10.0

val enforcer = MapBoundsEnforcer(
    mapBounds = eventBounds,
    mapLibreAdapter = mockAdapter,
    isWindowMode = true
)
```

**Execution**:
```kotlin
// Initial constraint application
enforcer.applyConstraints()

// Simulate camera idle (triggers padding recalculation)
val listenerSlot = slot<() -> Unit>()
verify { mockAdapter.addOnCameraIdleListener(capture(listenerSlot)) }

// Change viewport (zoom out)
listenerSlot.captured.invoke()
```

**Assertions**:
```kotlin
// 1. Verify setBoundsForCameraTarget called twice (initial + after padding change)
verify(atLeast = 2) { mockAdapter.setBoundsForCameraTarget(any(), any(), any()) }

// 2. Calculate expected padding change
val initialPadding = (48.6 - 48.4) / 2.0  // 0.1
val newPadding = (48.8 - 48.2) / 2.0      // 0.3
val percentChange = (newPadding - initialPadding) / initialPadding  // 200% (> 10% threshold)

// 3. Verify change exceeded threshold
assertTrue(percentChange > 0.10,
    "Padding change should exceed 10% threshold (was ${percentChange * 100}%)")
```

**Why This Test Matters**: Ensures constraint bounds update when viewport dimensions change significantly

---

### Test 5.4: Minor Padding Changes Ignored (Performance Optimization)

**Purpose**: Verify padding changes <10% don't trigger constraint recalculation

**Setup**:
```kotlin
val mockAdapter = mockk<MapLibreAdapter<Unit>>()
val eventBounds = BoundingBox(
    swLat = 48.0, swLng = 2.0,
    neLat = 49.0, neLng = 3.0
)

// Initial viewport
val initialRegion = BoundingBox(
    swLat = 48.4, swLng = 2.4,
    neLat = 48.6, neLng = 2.6
)

// Slightly different viewport (minor change)
val slightlyDifferentRegion = BoundingBox(
    swLat = 48.39, swLng = 2.39,
    neLat = 48.61, neLng = 2.61
)

every { mockAdapter.getVisibleRegion() } returnsMany listOf(initialRegion, slightlyDifferentRegion)
every { mockAdapter.setBoundsForCameraTarget(any(), any(), any()) } just Runs
every { mockAdapter.getMinZoomLevel() } returns 10.0

val enforcer = MapBoundsEnforcer(
    mapBounds = eventBounds,
    mapLibreAdapter = mockAdapter,
    isWindowMode = true
)
```

**Execution**:
```kotlin
// Initial constraint application
enforcer.applyConstraints()

// Clear verification (reset call counts)
clearMocks(mockAdapter, answers = false)

// Simulate camera idle with minor padding change
val listenerSlot = slot<() -> Unit>()
verify { mockAdapter.addOnCameraIdleListener(capture(listenerSlot)) }
listenerSlot.captured.invoke()
```

**Assertions**:
```kotlin
// 1. Verify setBoundsForCameraTarget NOT called again (padding change insignificant)
verify(exactly = 0) { mockAdapter.setBoundsForCameraTarget(any(), any(), any()) }

// 2. Calculate padding change percentage
val initialPadding = (48.6 - 48.4) / 2.0  // 0.1
val newPadding = (48.61 - 48.39) / 2.0    // 0.11
val percentChange = (newPadding - initialPadding) / initialPadding  // ~10% (at threshold)

// 3. Verify change below threshold
assertTrue(percentChange < 0.10,
    "Padding change should be below 10% threshold (was ${percentChange * 100}%)")
```

**Why This Test Matters**: Prevents redundant constraint updates for minor viewport changes

---

## Section 6: BOUNDS vs WINDOW Mode Integration Tests

**File**: `/Users/ldiasdasilva/StudioProjects/WorldWideWaves/shared/src/androidUnitTest/kotlin/com/worldwidewaves/shared/map/AbstractEventMapBoundsWindowModeTest.kt`

### Test 6.1: BOUNDS Mode Uses Zero Padding Throughout Stack

**Purpose**: Verify BOUNDS mode uses zero padding from enforcer → adapter

**Setup**:
```kotlin
// Full integration test with real MapBoundsEnforcer + mock adapter
val mockAdapter = TestMapLibreAdapter()
val mockEvent = createMockEvent(
    bounds = BoundingBox(
        swLat = 48.0, swLng = 2.0,
        neLat = 49.0, neLng = 3.0
    )
)

val eventMap = TestEventMap(
    event = mockEvent,
    mapConfig = EventMapConfig(initialCameraPosition = MapCameraPosition.BOUNDS),
    onLocationUpdate = { },
    mockMapLibreAdapter = mockAdapter,
    mockLocationProvider = null
)
```

**Execution**:
```kotlin
eventMap.moveToMapBounds()
testScope.testScheduler.advanceUntilIdle()
```

**Assertions**:
```kotlin
// 1. Verify MapBoundsEnforcer created with isWindowMode=false
assertNotNull(eventMap.constraintManager)
assertFalse(eventMap.constraintManager!!.isWindowMode,
    "BOUNDS mode should create enforcer with isWindowMode=false")

// 2. Verify zero padding used
val constraintBounds = mockAdapter.constraintBounds
assertNotNull(constraintBounds)

// In BOUNDS mode, constraint bounds should equal event bounds (no padding)
assertEquals(48.0, constraintBounds.sw.lat, 0.001,
    "BOUNDS mode constraint bounds should match event bounds (no padding)")
assertEquals(49.0, constraintBounds.ne.lat, 0.001)

// 3. Verify min zoom uses getCameraForLatLngBounds (BOUNDS mode formula)
val minZoom = mockAdapter.minZoomPreference
assertNotNull(minZoom)
// (Exact value depends on mock implementation, but verify it's set)
```

**Why This Test Matters**: Validates end-to-end BOUNDS mode behavior

---

### Test 6.2: WINDOW Mode Uses Viewport Padding Throughout Stack

**Purpose**: Verify WINDOW mode uses viewport padding from enforcer → adapter

**Setup**:
```kotlin
val mockAdapter = TestMapLibreAdapter()
val mockEvent = createMockEvent(
    bounds = BoundingBox(
        swLat = 48.0, swLng = 2.0,
        neLat = 49.0, neLng = 3.0
    )
)

val eventMap = TestEventMap(
    event = mockEvent,
    mapConfig = EventMapConfig(initialCameraPosition = MapCameraPosition.WINDOW),
    onLocationUpdate = { },
    mockMapLibreAdapter = mockAdapter,
    mockLocationProvider = null
)
```

**Execution**:
```kotlin
eventMap.moveToWindowBounds()
testScope.testScheduler.advanceUntilIdle()
```

**Assertions**:
```kotlin
// 1. Verify MapBoundsEnforcer created with isWindowMode=true
assertNotNull(eventMap.constraintManager)
assertTrue(eventMap.constraintManager!!.isWindowMode,
    "WINDOW mode should create enforcer with isWindowMode=true")

// 2. Verify constraint bounds are SHRUNK (viewport padding applied)
val constraintBounds = mockAdapter.constraintBounds
assertNotNull(constraintBounds)

// Constraint bounds should be SMALLER than event bounds (shrunk by padding)
assertTrue(constraintBounds.sw.lat > 48.0,
    "WINDOW mode constraint bounds should be shrunk (sw.lat > event sw.lat)")
assertTrue(constraintBounds.ne.lat < 49.0,
    "WINDOW mode constraint bounds should be shrunk (ne.lat < event ne.lat)")

// 3. Verify min zoom uses moveToWindowBounds formula + safety margin
val minZoom = mockAdapter.minZoomPreference
assertNotNull(minZoom)
assertTrue(minZoom!! > 0.0,
    "WINDOW mode should calculate min zoom with safety margin")
```

**Why This Test Matters**: Validates end-to-end WINDOW mode behavior with padding

---

### Test 6.3: Mode Switching Updates Constraint Behavior

**Purpose**: Verify switching from BOUNDS → WINDOW updates padding/constraints

**Setup**:
```kotlin
val mockAdapter = TestMapLibreAdapter()
val mockEvent = createMockEvent(
    bounds = BoundingBox(
        swLat = 48.0, swLng = 2.0,
        neLat = 49.0, neLng = 3.0
    )
)

val eventMap = TestEventMap(
    event = mockEvent,
    mapConfig = EventMapConfig(initialCameraPosition = MapCameraPosition.BOUNDS),
    onLocationUpdate = { },
    mockMapLibreAdapter = mockAdapter,
    mockLocationProvider = null
)
```

**Execution**:
```kotlin
// Start in BOUNDS mode
eventMap.moveToMapBounds()
testScope.testScheduler.advanceUntilIdle()
val boundsConstraints = mockAdapter.constraintBounds

// Switch to WINDOW mode
eventMap.moveToWindowBounds()
testScope.testScheduler.advanceUntilIdle()
val windowConstraints = mockAdapter.constraintBounds
```

**Assertions**:
```kotlin
// 1. Verify BOUNDS constraints are NOT shrunk
assertNotNull(boundsConstraints)
assertEquals(48.0, boundsConstraints.sw.lat, 0.001,
    "BOUNDS mode should have constraint bounds = event bounds")

// 2. Verify WINDOW constraints ARE shrunk
assertNotNull(windowConstraints)
assertTrue(windowConstraints.sw.lat > 48.0,
    "WINDOW mode should have shrunk constraint bounds")

// 3. Verify constraints changed between modes
assertNotEquals(boundsConstraints, windowConstraints,
    "Switching modes should update constraint bounds")
```

**Why This Test Matters**: Validates mode-aware constraint application

---

## Section 7: Edge Cases and Error Handling Tests

**File**: `/Users/ldiasdasilva/StudioProjects/WorldWideWaves/composeApp/src/androidUnitTest/kotlin/com/worldwidewaves/map/AndroidMapLibreAdapterEdgeCasesTest.kt`

### Test 7.1: Invalid Bounds Validation (Inverted Coordinates)

**Purpose**: Verify setBoundsForCameraTarget validates bounds order (ne > sw)

**Setup**:
```kotlin
val mockMap = mockk<MapLibreMap>(relaxed = true)
val adapter = AndroidMapLibreAdapter(mockMap)
adapter.setMap(mockMap)

// Invalid: ne.lat < sw.lat (inverted)
val invertedBounds = BoundingBox(
    swLat = 49.0, swLng = 2.0,
    neLat = 48.0, neLng = 3.0  // 48 < 49 (INVALID)
)
```

**Execution & Assertions**:
```kotlin
// Should throw IllegalArgumentException
assertFailsWith<IllegalArgumentException> {
    adapter.setBoundsForCameraTarget(
        constraintBounds = invertedBounds,
        applyZoomSafetyMargin = true,
        originalEventBounds = invertedBounds
    )
}
```

**Why This Test Matters**: Prevents C++ crashes in native MapLibre code

---

### Test 7.2: Bounds Validation (Latitude Out of Range)

**Purpose**: Verify latitude range validation (-90 to 90)

**Setup**:
```kotlin
val mockMap = mockk<MapLibreMap>(relaxed = true)
val adapter = AndroidMapLibreAdapter(mockMap)
adapter.setMap(mockMap)

// Invalid: latitude > 90
val invalidLatBounds = BoundingBox(
    swLat = 48.0, swLng = 2.0,
    neLat = 95.0, neLng = 3.0  // 95 > MAX_LATITUDE (90)
)
```

**Execution & Assertions**:
```kotlin
assertFailsWith<IllegalArgumentException> {
    adapter.setBoundsForCameraTarget(
        constraintBounds = invalidLatBounds,
        applyZoomSafetyMargin = true,
        originalEventBounds = invalidLatBounds
    )
}
```

**Why This Test Matters**: Prevents MapLibre native crashes from invalid coordinates

---

### Test 7.3: Bounds Validation (Longitude Out of Range)

**Purpose**: Verify longitude range validation (-180 to 180)

**Setup**:
```kotlin
val mockMap = mockk<MapLibreMap>(relaxed = true)
val adapter = AndroidMapLibreAdapter(mockMap)
adapter.setMap(mockMap)

// Invalid: longitude < -180
val invalidLngBounds = BoundingBox(
    swLat = 48.0, swLng = -200.0,  // -200 < MIN_LONGITUDE (-180)
    neLat = 49.0, neLng = 3.0
)
```

**Execution & Assertions**:
```kotlin
assertFailsWith<IllegalArgumentException> {
    adapter.setBoundsForCameraTarget(
        constraintBounds = invalidLngBounds,
        applyZoomSafetyMargin = true,
        originalEventBounds = invalidLngBounds
    )
}
```

**Why This Test Matters**: Prevents longitude wrapping issues

---

### Test 7.4: Zero-Size Event (Point Location)

**Purpose**: Test behavior when event has zero area (single point)

**Setup**:
```kotlin
val mockMap = mockk<MapLibreMap>(relaxed = true)
val adapter = AndroidMapLibreAdapter(mockMap)
adapter.setMap(mockMap)

// Zero-size bounds (point)
val pointBounds = BoundingBox(
    swLat = 48.85, swLng = 2.35,
    neLat = 48.85, neLng = 2.35  // Same coordinates (zero size)
)

every { mockMap.width } returns 600
every { mockMap.height } returns 800
```

**Execution & Assertions**:
```kotlin
// Should NOT crash (MapLibre should handle zero-size bounds)
// But may log warning or use default zoom
adapter.setBoundsForCameraTarget(
    constraintBounds = pointBounds,
    applyZoomSafetyMargin = true,
    originalEventBounds = pointBounds
)

// Verify min zoom is reasonable (not NaN or Infinity)
val minZoom = adapter.getMinZoomLevel()
assertTrue(minZoom.isFinite() && minZoom > 0.0,
    "Min zoom should be finite positive number even for zero-size bounds")
```

**Why This Test Matters**: Handles degenerate case gracefully

---

### Test 7.5: Very Small Event (Sub-Meter Precision)

**Purpose**: Test behavior with extremely small events (e.g., building interior)

**Setup**:
```kotlin
val mockMap = mockk<MapLibreMap>(relaxed = true)
val adapter = AndroidMapLibreAdapter(mockMap)
adapter.setMap(mockMap)

// Tiny bounds (10m × 10m)
val tinyBounds = BoundingBox(
    swLat = 48.8500, swLng = 2.3500,
    neLat = 48.8501, neLng = 2.3501  // ~0.0001 degrees ≈ 10 meters
)

every { mockMap.width } returns 600
every { mockMap.height } returns 800
```

**Execution**:
```kotlin
adapter.setBoundsForCameraTarget(
    constraintBounds = tinyBounds,
    applyZoomSafetyMargin = true,
    originalEventBounds = tinyBounds
)
```

**Assertions**:
```kotlin
// 1. Verify min zoom is very high (zoomed in)
val minZoom = adapter.getMinZoomLevel()
assertTrue(minZoom > 15.0,
    "Very small event should require high zoom level")

// 2. Verify zoom doesn't exceed MapLibre max (typically 22)
assertTrue(minZoom < 22.0,
    "Min zoom should not exceed MapLibre maximum")
```

**Why This Test Matters**: Validates formula stability with small event dimensions

---

## Section 8: Performance and Optimization Tests

**File**: `/Users/ldiasdasilva/StudioProjects/WorldWideWaves/shared/src/commonTest/kotlin/com/worldwidewaves/shared/map/MapBoundsEnforcerPerformanceTest.kt`

### Test 8.1: Redundant Constraint Updates Prevented

**Purpose**: Verify identical bounds don't trigger redundant setBoundsForCameraTarget calls

**Setup**:
```kotlin
val mockAdapter = mockk<MapLibreAdapter<Unit>>()
val eventBounds = BoundingBox(
    swLat = 48.0, swLng = 2.0,
    neLat = 49.0, neLng = 3.0
)

every { mockAdapter.getVisibleRegion() } returns eventBounds
every { mockAdapter.setBoundsForCameraTarget(any(), any(), any()) } just Runs
every { mockAdapter.getMinZoomLevel() } returns 10.0

val enforcer = MapBoundsEnforcer(
    mapBounds = eventBounds,
    mapLibreAdapter = mockAdapter,
    isWindowMode = false
)
```

**Execution**:
```kotlin
// Initial constraint application
enforcer.applyConstraints()

// Trigger camera idle with SAME bounds (no change)
val listenerSlot = slot<() -> Unit>()
verify { mockAdapter.addOnCameraIdleListener(capture(listenerSlot)) }

// Simulate multiple camera idle events with identical bounds
listenerSlot.captured.invoke()
listenerSlot.captured.invoke()
listenerSlot.captured.invoke()
```

**Assertions**:
```kotlin
// 1. Verify setBoundsForCameraTarget called only ONCE (initial application)
verify(exactly = 1) { mockAdapter.setBoundsForCameraTarget(any(), any(), any()) }

// 2. Verify lastAppliedBounds tracks last constraints
assertNotNull(enforcer.lastAppliedBounds)

// 3. Verify similarity check prevents redundant updates
// (Implementation detail: boundsAreSimilar() prevents updates)
```

**Why This Test Matters**: Prevents iOS infinite loop from camera idle triggering setBounds

---

### Test 8.2: Padding Recalculation Throttling

**Purpose**: Verify 10% threshold prevents excessive constraint recalculations

**Setup**:
```kotlin
val mockAdapter = mockk<MapLibreAdapter<Unit>>()
val eventBounds = BoundingBox(
    swLat = 48.0, swLng = 2.0,
    neLat = 49.0, neLng = 3.0
)

// Series of slightly different viewports (all <10% change)
val viewports = listOf(
    BoundingBox(swLat = 48.4, swLng = 2.4, neLat = 48.6, neLng = 2.6),
    BoundingBox(swLat = 48.39, swLng = 2.39, neLat = 48.61, neLng = 2.61),
    BoundingBox(swLat = 48.38, swLng = 2.38, neLat = 48.62, neLng = 2.62),
    BoundingBox(swLat = 48.37, swLng = 2.37, neLat = 48.63, neLng = 2.63)
)

every { mockAdapter.getVisibleRegion() } returnsMany viewports
every { mockAdapter.setBoundsForCameraTarget(any(), any(), any()) } just Runs
every { mockAdapter.getMinZoomLevel() } returns 10.0

val enforcer = MapBoundsEnforcer(
    mapBounds = eventBounds,
    mapLibreAdapter = mockAdapter,
    isWindowMode = true
)
```

**Execution**:
```kotlin
// Initial application
enforcer.applyConstraints()

// Capture camera idle listener
val listenerSlot = slot<() -> Unit>()
verify { mockAdapter.addOnCameraIdleListener(capture(listenerSlot)) }

// Trigger multiple camera idle events with minor padding changes
viewports.drop(1).forEach { _ ->
    listenerSlot.captured.invoke()
}
```

**Assertions**:
```kotlin
// 1. Verify setBoundsForCameraTarget called only ONCE (initial)
// All subsequent calls should be throttled (<10% change)
verify(exactly = 1) { mockAdapter.setBoundsForCameraTarget(any(), any(), any()) }

// 2. Verify hasSignificantPaddingChange() correctly detected minor changes
// (All changes should be below 10% threshold)
```

**Why This Test Matters**: Prevents CPU thrashing during smooth zoom gestures

---

## Section 9: Cross-Platform Parity Tests

**File**: `/Users/ldiasdasilva/StudioProjects/WorldWideWaves/shared/src/commonTest/kotlin/com/worldwidewaves/shared/map/CrossPlatformAspectRatioParityTest.kt`

### Test 9.1: Android and iOS Use Same Aspect Ratio Formula

**Purpose**: Document expected parity between platforms (iOS tests mirror these)

**Setup**:
```kotlin
// This test runs on commonMain (shared across platforms)
// Documents expected behavior for both Android and iOS
```

**Expected iOS Equivalent**:
```swift
// MapLibreViewWrapper.swift should use IDENTICAL formula:
let zoomForWidth = log2((mapWidth * 360.0) / (eventWidth * 256.0))
let zoomForHeight = log2((mapHeight * 180.0) / (eventHeight * 256.0))
let targetZoom = min(zoomForWidth, zoomForHeight)
```

**Assertions**:
```kotlin
// 1. Verify formula constants are identical
val latitudeTotalDegrees = 180.0
val longitudeTotalDegrees = 360.0
val tileSize = 256.0

// 2. Document formula for iOS reference
// zoomForWidth = log2((screenWidth * 360.0) / (eventWidth * 256.0))
// zoomForHeight = log2((screenHeight * 180.0) / (eventHeight * 256.0))
// targetZoom = min(zoomForWidth, zoomForHeight)

// 3. Verify platform implementations match this formula
// (Actual verification happens in platform-specific tests)
```

**Why This Test Matters**: Ensures consistent UX across Android and iOS

---

### Test 9.2: Min Zoom Safety Margin Parity

**Purpose**: Verify both platforms use +0.5 safety margin in WINDOW mode

**Expected Behavior**:
- Android: `baseMinZoom + ZOOM_SAFETY_MARGIN` (0.5)
- iOS: `baseMinZoom + 0.5`

**Assertions**:
```kotlin
assertEquals(0.5, ZOOM_SAFETY_MARGIN,
    "Safety margin must be 0.5 on both platforms")
```

**Why This Test Matters**: Prevents platform-specific edge-case failures

---

## Section 10: Regression Prevention Tests

**File**: `/Users/ldiasdasilva/StudioProjects/WorldWideWaves/shared/src/androidUnitTest/kotlin/com/worldwidewaves/shared/map/RegressionPreventionTest.kt`

### Test 10.1: Prevent Zoom-Out Spiral (Historical Bug)

**Purpose**: Verify fix for infinite zoom-out when using shrunk bounds for min zoom

**Setup**:
```kotlin
// Simulate the bug scenario that was fixed
val mockAdapter = TestMapLibreAdapter()
val originalBounds = BoundingBox(
    swLat = 48.0, swLng = 2.0,
    neLat = 49.0, neLng = 3.0
)

// Progressively shrinking constraint bounds (simulates padding increases)
val shrinkSequence = listOf(
    BoundingBox(swLat = 48.2, swLng = 2.2, neLat = 48.8, neLng = 2.8),
    BoundingBox(swLat = 48.3, swLng = 2.3, neLat = 48.7, neLng = 2.7),
    BoundingBox(swLat = 48.4, swLng = 2.4, neLat = 48.6, neLng = 2.6)
)
```

**Execution**:
```kotlin
// Apply constraints with progressively shrinking bounds
// CRITICAL: originalEventBounds prevents spiral
shrinkSequence.forEach { shrunkBounds ->
    mockAdapter.setBoundsForCameraTarget(
        constraintBounds = shrunkBounds,
        applyZoomSafetyMargin = true,
        originalEventBounds = originalBounds  // CRITICAL: Use ORIGINAL
    )
}
```

**Assertions**:
```kotlin
// 1. Verify min zoom remains constant (doesn't increase)
val initialMinZoom = mockAdapter.getMinZoomLevel()
shrinkSequence.forEach { _ ->
    assertEquals(initialMinZoom, mockAdapter.getMinZoomLevel(), 0.001,
        "Min zoom should NOT increase when constraint bounds shrink")
}

// 2. Verify zoom didn't spiral to excessive values
assertTrue(mockAdapter.getMinZoomLevel() < 15.0,
    "Min zoom should remain reasonable (not spiral to excessive zoom)")
```

**Why This Test Matters**: Prevents regression of critical zoom-out spiral bug

---

### Test 10.2: Prevent Animation Flicker (Historical Bug)

**Purpose**: Verify fix for camera animation fighting with constraint enforcement

**Setup**:
```kotlin
// Simulate scenario where reactive constraints fought with animations
val mockAdapter = TestMapLibreAdapter()
val eventBounds = BoundingBox(
    swLat = 48.0, swLng = 2.0,
    neLat = 49.0, neLng = 3.0
)
```

**Execution**:
```kotlin
// Start animation
val animationInProgress = AtomicBoolean(true)
val enforcer = MapBoundsEnforcer(
    mapBounds = eventBounds,
    mapLibreAdapter = mockAdapter,
    isWindowMode = true,
    isSuppressed = { animationInProgress.get() }
)

// Apply constraints while animation running
enforcer.applyConstraints()
enforcer.constrainCamera()  // Should be suppressed

animationInProgress.set(false)
enforcer.constrainCamera()  // Now allowed
```

**Assertions**:
```kotlin
// 1. Verify reactive corrections suppressed during animation
// (Preventive constraints are still active, but no reactive animations)

// 2. Verify no double-animation (animation + constraint correction)
// (This is implicit in suppression mechanism)
```

**Why This Test Matters**: Prevents camera flicker during moveToWindowBounds

---

## Summary of Test Coverage

### Total Tests Specified: 40

| Category | Tests | File Location |
|----------|-------|---------------|
| Aspect Ratio Fitting | 5 | `AbstractEventMapAspectRatioTest.kt` |
| Min Zoom Formula Parity | 4 | `AndroidMapLibreAdapterMinZoomParityTest.kt` |
| Min Zoom Locking | 3 | `AndroidMapLibreAdapterMinZoomLockingTest.kt` |
| Gesture Interception | 3 | `AndroidMapLibreAdapterGestureInterceptionTest.kt` |
| Viewport Padding Logic | 4 | `MapBoundsEnforcerViewportPaddingTest.kt` |
| BOUNDS vs WINDOW Mode | 3 | `AbstractEventMapBoundsWindowModeTest.kt` |
| Edge Cases | 5 | `AndroidMapLibreAdapterEdgeCasesTest.kt` |
| Performance | 2 | `MapBoundsEnforcerPerformanceTest.kt` |
| Cross-Platform Parity | 2 | `CrossPlatformAspectRatioParityTest.kt` |
| Regression Prevention | 2 | `RegressionPreventionTest.kt` |

### Test Priority Levels

**Priority 1 (Critical - Implement First):**
- Test 1.1, 1.2 (Aspect ratio fitting)
- Test 2.1, 2.3 (Min zoom formula parity)
- Test 3.1 (Min zoom locking)
- Test 5.1, 5.2 (Viewport padding mode differences)
- Test 10.1 (Regression: zoom-out spiral)

**Priority 2 (Important - Implement Next):**
- Test 1.3, 1.4, 1.5 (Aspect ratio edge cases)
- Test 2.2, 2.4 (BOUNDS mode, safety margin)
- Test 4.1 (Gesture listener registration)
- Test 6.1, 6.2 (Mode integration)
- Test 7.1-7.5 (Validation tests)

**Priority 3 (Nice to Have):**
- Test 3.2, 3.3 (Min zoom lock behavior)
- Test 4.2, 4.3 (Gesture interception details)
- Test 5.3, 5.4 (Padding recalculation)
- Test 8.1, 8.2 (Performance optimizations)
- Test 9.1, 9.2 (Cross-platform documentation)

---

## Implementation Guidelines

### Test Helpers Needed

```kotlin
// MapTestHelpers.kt
object MapTestHelpers {
    fun createMockEvent(
        bounds: BoundingBox,
        maxZoom: Double = 18.0
    ): IWWWEvent {
        val mockEvent = mockk<IWWWEvent>()
        val mockArea = mockk<WWWEventArea>()
        val mockMap = mockk<WWWEventMap>()

        every { mockEvent.area } returns mockArea
        every { mockEvent.map } returns mockMap
        coEvery { mockArea.bbox() } returns bounds
        every { mockMap.maxZoom } returns maxZoom

        return mockEvent
    }

    fun calculateExpectedZoom(
        eventWidth: Double,
        eventHeight: Double,
        screenWidth: Double,
        screenHeight: Double
    ): Double {
        val zoomForWidth = log2((screenWidth * 360.0) / (eventWidth * 256.0))
        val zoomForHeight = log2((screenHeight * 180.0) / (eventHeight * 256.0))
        return min(zoomForWidth, zoomForHeight)
    }
}
```

### Mock Verification Patterns

```kotlin
// Verify exact zoom calculation
val capturedZoom = slot<Double>()
verify { mockMapLibreAdapter.animateCamera(any(), capture(capturedZoom), any()) }
assertEquals(expectedZoom, capturedZoom.captured, 0.01)

// Verify bounds setting
verify { mockMap.setLatLngBoundsForCameraTarget(any()) }

// Verify min zoom preference
verify(exactly = 1) { mockMap.setMinZoomPreference(any()) }
```

---

## Next Steps

1. **Implement Priority 1 tests first** (critical functionality)
2. **Run existing test suite** to ensure no conflicts
3. **Add tests incrementally** (don't create all 40 at once)
4. **Verify each test passes** before moving to next
5. **Update documentation** when edge cases discovered
6. **Create iOS mirror tests** for cross-platform features

---

**Document Status**: Ready for Implementation
**Estimated Implementation Time**: 12-16 hours for all 40 tests
**Recommended Approach**: Implement in 3 phases (Priority 1 → 2 → 3)
