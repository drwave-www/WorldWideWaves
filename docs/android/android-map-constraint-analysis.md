# Android Map Constraint Implementation Analysis

## Executive Summary

The Android map constraint system provides a **preventive enforcement model** for keeping camera and viewport within event boundaries. It uses two complementary techniques:

1. **Min Zoom Calculation** - Ensures viewport can never zoom out beyond the event area
2. **Preventive Gesture Clamping** - Blocks invalid pan gestures in real-time

This approach prevents invalid states rather than correcting them after-the-fact.

---

## Architecture Overview

### Layer 1: MapBoundsEnforcer (Platform-Independent Logic)
**File**: `shared/src/commonMain/kotlin/com/worldwidewaves/shared/map/MapBoundsEnforcer.kt`

**Responsibility**:
- Calculate constraint bounds (padded or zero)
- Manage viewport padding logic
- Track constraint state transitions

**Key Methods**:
```kotlin
fun applyConstraints()                    // Entry point, handles both modes
fun calculateConstraintBounds()           // Returns padded bounds based on mode
private fun calculateVisibleRegionPadding() // WINDOW vs BOUNDS mode logic
```

### Layer 2: MapLibreAdapter (Platform Interface)
**File**: `shared/src/commonMain/kotlin/com/worldwidewaves/shared/map/MapLibreAdapter.kt`

**Defines Interface**:
```kotlin
fun setBoundsForCameraTarget(
    constraintBounds: BoundingBox,
    applyZoomSafetyMargin: Boolean,
    originalEventBounds: BoundingBox?
)
fun setMinZoomPreference(minZoom: Double)
fun setMaxZoomPreference(maxZoom: Double)
```

### Layer 3: AndroidMapLibreAdapter (Android Implementation)
**File**: `composeApp/src/androidMain/kotlin/com/worldwidewaves/map/AndroidMapLibreAdapter.kt`

**Responsibilities**:
- Calculate min zoom from MapLibre camera API
- Set bounds and min zoom on native MapLibre
- Setup preventive gesture clamping listeners

---

## Mode-Specific Constraint Logic

### BOUNDS Mode (Event Detail Screen)
**Configuration**: `MapCameraPosition.BOUNDS`, gestures disabled

**Constraint Application**:
1. **Zero Padding**: `calculateVisibleRegionPadding()` returns (0, 0)
2. **Constraint Bounds** = Event bounds (unchanged)
3. **Min Zoom Calculation**:
   - Uses `getCameraForLatLngBounds(eventBounds, padding=[0,0,0,0])`
   - Shows entire event in viewport
   - No zooming in beyond point where event fills screen
4. **Gesture Enforcement**: Not applicable (gestures disabled)

**Result**: Entire event area always visible, no panning allowed

### WINDOW Mode (Full Map Screen)
**Configuration**: `MapCameraPosition.WINDOW`, gestures enabled

**Constraint Application**:
1. **Viewport-Based Padding**:
   ```kotlin
   // Calculate current viewport dimensions
   viewportHalfHeight = (viewportNE.lat - viewportSW.lat) / 2
   viewportHalfWidth = (viewportNE.lng - viewportSW.lng) / 2

   // Result: padding shrinks bounds by viewport size
   ```
2. **Constraint Bounds** = Event bounds shrunk by viewport half-size
3. **Min Zoom Calculation**:
   - Compares event aspect ratio to screen aspect ratio
   - Fits by CONSTRAINING DIMENSION (smaller fit)
   - Wide event on tall screen: fit by HEIGHT
   - Tall event on wide screen: fit by WIDTH
4. **Gesture Enforcement**:
   - Listens to `onCameraMove` during gestures
   - Clamps camera position to prevent viewport overflow
   - Applied ONCE via `setupPreventiveGestureConstraints()`

**Result**: User can pan/zoom within bounds, viewport stays inside event area

---

## Min Zoom Calculation (Critical Detail)

### WINDOW Mode: Aspect Ratio Fitting

```kotlin
// Step 1: Determine constraining dimension
val eventAspect = eventWidth / eventHeight
val screenAspect = screenWidth / screenHeight

if (eventAspect > screenAspect) {
    // Event is WIDER than screen → constrained by HEIGHT
    // Create bounds with event height, screen-proportional width
    val constrainedWidth = eventHeight * screenAspect
    // This ensures: event height fills screen, width stays inside
} else {
    // Event is TALLER than screen → constrained by WIDTH
    // Create bounds with event width, screen-proportional height
    val constrainedHeight = eventWidth / screenAspect
    // This ensures: event width fills screen, height stays inside
}

// Step 2: Calculate zoom for constraining bounds
val cameraPosition = mapLibreMap.getCameraForLatLngBounds(constrainingBounds, padding=[0,0,0,0])
calculatedMinZoom = cameraPosition.zoom

// Step 3: Set immediately (preventive)
mapLibreMap.setMinZoomPreference(calculatedMinZoom)
```

**Logic**: `min(zoomForWidth, zoomForHeight)` ensures BOTH dimensions fit

### BOUNDS Mode: Full Event Fitting

```kotlin
// Simply fit entire event bounds
val cameraPosition = mapLibreMap.getCameraForLatLngBounds(eventBounds, padding=[0,0,0,0])
calculatedMinZoom = cameraPosition.zoom
```

**Logic**: Calculated from full event bounds, no padding

### Why No Safety Margin?

**Previous Code** (commented out):
```kotlin
// ZOOM_SAFETY_MARGIN removed - base min zoom calculation already ensures event fits
// The min(zoomForWidth, zoomForHeight) ensures BOTH dimensions fit in viewport
```

The min() logic already prevents pixels outside event area - additional margin would only zoom in further.

---

## Preventing Gesture Overflow (WINDOW Mode Only)

### Setup: Called Once
```kotlin
private fun setupPreventiveGestureConstraints() {
    // Track gesture type (user gesture vs programmatic animation)
    map.addOnCameraMoveStartedListener { reason ->
        isGestureInProgress = (reason == REASON_API_GESTURE) // user pan/pinch
    }

    // Intercept camera movement during user gestures
    map.addOnCameraMoveListener {
        if (!isGestureInProgress) return // Skip programmatic moves

        val viewport = getVisibleRegion()
        if (!isViewportWithinBounds(viewport, constraintBounds)) {
            // Clamp camera IMMEDIATELY
            val clampedPosition = clampCameraToKeepViewportInside(
                currentCamera, viewport, constraintBounds
            )
            map.cameraPosition = clampedPosition
        }
    }
}
```

### Viewport Validation
```kotlin
private fun isViewportWithinBounds(viewport: BoundingBox, eventBounds: BoundingBox): Boolean {
    return viewport.southLatitude >= eventBounds.southwest.latitude &&
           viewport.northLatitude <= eventBounds.northeast.latitude &&
           viewport.westLongitude >= eventBounds.southwest.longitude &&
           viewport.eastLongitude <= eventBounds.northeast.longitude
}
```

### Camera Clamping
```kotlin
private fun clampCameraToKeepViewportInside(
    currentCamera: Position,
    currentViewport: BoundingBox,
    eventBounds: BoundingBox
): Position {
    // Calculate how much camera can move before viewport exceeds bounds
    val viewportHalfHeight = (currentViewport.northLatitude - currentViewport.southLatitude) / 2
    val viewportHalfWidth = (currentViewport.eastLongitude - currentViewport.westLongitude) / 2

    // Valid camera center range (keeps viewport inside event)
    val minValidLat = eventBounds.southwest.latitude + viewportHalfHeight
    val maxValidLat = eventBounds.northeast.latitude - viewportHalfHeight
    val minValidLng = eventBounds.southwest.longitude + viewportHalfWidth
    val maxValidLng = eventBounds.northeast.longitude + viewportHalfWidth

    // Clamp to valid range
    return Position(
        currentCamera.latitude.coerceIn(minValidLat, maxValidLat),
        currentCamera.longitude.coerceIn(minValidLng, maxValidLng)
    )
}
```

**Note**: If viewport > event (zoomed out beyond min zoom), camera centers on event

---

## Constraint State Transitions

### Initial Setup
```kotlin
// In AbstractEventMap.moveToWindowBounds()
constraintManager = MapBoundsEnforcer(
    eventBbox,
    mapLibreAdapter,
    isWindowMode = true  // For WINDOW screen
)

// Apply constraints IMMEDIATELY (before any gestures)
constraintManager?.applyConstraints()
```

### Dynamic Recalculation
MapBoundsEnforcer registers a camera-idle listener that:

1. **Detects significant viewport changes** (zoom changes, orientation)
2. **Recalculates padding** if changed > 10% threshold
3. **Updates constraint bounds** if padding differs
4. **Skips redundant updates** if bounds similar (within 0.1% tolerance)

```kotlin
mapLibreAdapter.addOnCameraIdleListener {
    if (skipNextRecalculation) {
        skipNextRecalculation = false
        return@addOnCameraIdleListener
    }

    val newPadding = calculateVisibleRegionPadding()
    if (hasSignificantPaddingChange(newPadding)) {
        applyConstraintsWithPadding()
    }
}
```

### Bounds Similarity Check
```kotlin
private fun boundsAreSimilar(bounds1: BoundingBox, bounds2: BoundingBox): Boolean {
    val tolerance = 0.001 // 0.1% tolerance

    val latDiff = abs(bounds1.southwest.lat - bounds2.southwest.lat) +
                  abs(bounds1.northeast.lat - bounds2.northeast.lat)
    val lngDiff = abs(bounds1.southwest.lng - bounds2.southwest.lng) +
                  abs(bounds1.northeast.lng - bounds2.northeast.lng)

    return latDiff < tolerance && lngDiff < tolerance
}
```

---

## Integration with AbstractEventMap

### When Constraints Applied

```kotlin
// BOUNDS Mode (event detail)
suspend fun moveToMapBounds() {
    constraintManager = MapBoundsEnforcer(..., isWindowMode = false)

    // Animate to bounds
    mapLibreAdapter.animateCameraToBounds(bounds, padding = 0)

    // Apply constraints AFTER animation
    constraintManager?.applyConstraints()

    // Get calculated min zoom
    val minZoom = mapLibreAdapter.getMinZoomLevel()

    // Set max zoom
    mapLibreAdapter.setMaxZoomPreference(event.map.maxZoom)
}

// WINDOW Mode (full map)
suspend fun moveToWindowBounds() {
    constraintManager = MapBoundsEnforcer(..., isWindowMode = true)

    // Apply constraints BEFORE animation (preventive)
    constraintManager?.applyConstraints()

    // Set max zoom
    mapLibreAdapter.setMaxZoomPreference(event.map.maxZoom)

    // No animation - gestures control view
}
```

---

## Max Zoom Handling

**Simple Rule**: Set from event configuration
```kotlin
mapLibreAdapter.setMaxZoomPreference(event.map.maxZoom)  // Usually 16
```

**Applied in**:
- `moveToMapBounds()` after min zoom is set
- `moveToWindowBounds()` after constraints applied
- Initial `setupMap()` callback

**Consistency**: Always 16 for WorldWideWaves events

---

## Critical Implementation Details

### 1. Padding Clamping (iOS Compatibility)
```kotlin
// Prevent bounds inversion when viewport > event
val maxLatPadding = eventLatSpan * 0.49  // Use 49%, not 50%
val maxLngPadding = eventLngSpan * 0.49

val effectiveLatPadding = min(padding.latPadding, maxLatPadding)
val effectiveLngPadding = min(padding.lngPadding, maxLngPadding)
```

If padding >= 50% of event size, bounds would have SW > NE (invalid).

### 2. Min Zoom Locking
```kotlin
private var minZoomLocked = false

// Only calculate once, prevent infinite zoom-out spiral
if (!minZoomLocked || originalEventBounds != null) {
    calculateAndSetMinZoom()
    minZoomLocked = true
}
```

### 3. Suppression During Animations
```kotlin
// In AbstractEventMap
private var suppressCorrections = false

private suspend inline fun runCameraAnimation(block: ...) {
    suppressCorrections = true
    // ... animation ...
    suppressCorrections = false
}

// In MapBoundsEnforcer
if (isSuppressed()) {
    Log.v("...", "Camera idle callback suppressed (animation in progress)")
    return@addOnCameraIdleListener
}
```

Prevents reactive corrections from fighting animations.

### 4. Gesture Distinction
```kotlin
map.addOnCameraMoveStartedListener { reason ->
    isGestureInProgress = when (reason) {
        REASON_API_GESTURE,
        REASON_API_ANIMATION,
        REASON_DEVELOPER_ANIMATION -> false // Programmatic
        else -> true // User gesture
    }
}
```

Only clamps for user gestures, not programmatic animations.

---

## Test Coverage

### AspectRatioFittingTest.kt
Tests min zoom calculation for various aspect ratio combinations:
- Wide event (Paris 2.84:1) on tall screen → fits by HEIGHT
- Tall event (Chile 0.25:1) on wide screen → fits by WIDTH
- Extreme cases (100:1, 1:100) without overflow

### BoundsWindowModeTest.kt
Tests viewport padding calculation for WINDOW mode

### RegressionPreventionTest.kt
Tests edge cases and constraint state consistency

---

## Key Takeaways for iOS Implementation

### 1. Min Zoom Must Be Set Immediately
```
Do NOT wait for constraint bounds to be set.
Min zoom must be calculated and set BEFORE first gesture.
```

### 2. Aspect Ratio Fitting Is Critical
```
eventAspect vs screenAspect determines which dimension constrains.
Use min(zoomForWidth, zoomForHeight).
```

### 3. Padding Logic Differs by Mode
```
BOUNDS: Zero padding, show entire event
WINDOW: Viewport-based padding, prevent overflow
```

### 4. Prevent Infinite Loops
```
Track "last applied bounds" with 0.1% tolerance.
Skip redundant updates.
Lock min zoom after first calculation.
```

### 5. Gesture Clamping Is Optional But Valuable
```
Android uses preventive gesture interception.
iOS delegate might provide equivalent via shouldChangeFrom.
Both require viewport validation logic.
```

---

## Files Containing Working Patterns

1. **AndroidMapLibreAdapter.kt** (lines 367-514)
   - `setBoundsForCameraTarget()` - min zoom calculation
   - `setupPreventiveGestureConstraints()` - gesture clamping

2. **MapBoundsEnforcer.kt** (lines 384-435)
   - `calculateVisibleRegionPadding()` - WINDOW vs BOUNDS mode logic
   - `calculatePaddedBounds()` - padding application

3. **AbstractEventMap.kt** (lines 124-228)
   - `moveToMapBounds()` - BOUNDS mode constraint flow
   - `moveToWindowBounds()` - WINDOW mode constraint flow

4. **AspectRatioFittingTest.kt**
   - Validates min zoom calculations
