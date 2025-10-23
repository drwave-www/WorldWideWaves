# Android Map Constraint - Working Code Patterns

## 1. Min Zoom Calculation Pattern (WINDOW Mode)

**Location**: `AndroidMapLibreAdapter.kt` lines 400-464

```kotlin
// CRITICAL: Different calculation for WINDOW vs BOUNDS mode
val baseMinZoom: Double

if (applyZoomSafetyMargin) {  // WINDOW MODE
    // WINDOW MODE: Fit the SMALLEST event dimension (prevents outside pixels)
    // For wide event on tall screen: fit height (smaller zoom)
    // For tall event on wide screen: fit width (smaller zoom)
    val eventWidth = boundsForMinZoom.ne.lng - boundsForMinZoom.sw.lng
    val eventHeight = boundsForMinZoom.ne.lat - boundsForMinZoom.sw.lat
    val mapWidth = getWidth()
    val mapHeight = getHeight()

    // Validate dimensions before calculation (prevent division by zero)
    if (mapWidth <= 0 || mapHeight <= 0 || eventWidth <= 0 || eventHeight <= 0) {
        Log.w("Camera", "Invalid dimensions: using fallback min zoom")
        calculatedMinZoom = mapLibreMap!!.minZoomLevel
        mapLibreMap!!.setMinZoomPreference(calculatedMinZoom)
        minZoomLocked = true
        return
    }

    // Determine which dimension is constraining
    val eventAspect = eventWidth / eventHeight
    val screenAspect = mapWidth / mapHeight

    // Use MapLibre to calculate zoom for the constraining dimension
    // Wide event on tall screen: use height-constrained bounds
    // Tall event on wide screen: use width-constrained bounds
    val constrainingBounds =
        if (eventAspect > screenAspect) {
            // Event is wider than screen aspect → constrained by HEIGHT
            // Create bounds with event height but screen-proportional width
            val constrainedWidth = eventHeight * screenAspect
            val centerLng = (boundsForMinZoom.sw.lng + boundsForMinZoom.ne.lng) / 2.0
            BoundingBox.fromCorners(
                Position(boundsForMinZoom.sw.lat, centerLng - constrainedWidth / 2),
                Position(boundsForMinZoom.ne.lat, centerLng + constrainedWidth / 2),
            )
        } else {
            // Event is taller than screen aspect → constrained by WIDTH
            val constrainedHeight = eventWidth / screenAspect
            val centerLat = (boundsForMinZoom.sw.lat + boundsForMinZoom.ne.lat) / 2.0
            BoundingBox.fromCorners(
                Position(centerLat - constrainedHeight / 2, boundsForMinZoom.sw.lng),
                Position(centerLat + constrainedHeight / 2, boundsForMinZoom.ne.lng),
            )
        }

    val latLngBounds = constrainingBounds.toLatLngBounds()
    val cameraPosition = mapLibreMap!!.getCameraForLatLngBounds(latLngBounds, intArrayOf(0, 0, 0, 0))
    baseMinZoom = cameraPosition?.zoom ?: mapLibreMap!!.minZoomLevel

    Log.i(
        "Camera",
        "🎯 WINDOW mode: eventAspect=$eventAspect, screenAspect=$screenAspect, " +
            "constrainedBy=${if (eventAspect > screenAspect) "HEIGHT" else "WIDTH"}, " +
            "minZoom=$baseMinZoom",
    )
} else {  // BOUNDS MODE
    // BOUNDS MODE: Use MapLibre's calculation (shows entire event)
    val latLngBounds = boundsForMinZoom.toLatLngBounds()
    val cameraPosition = mapLibreMap!!.getCameraForLatLngBounds(latLngBounds, intArrayOf(0, 0, 0, 0))
    baseMinZoom = cameraPosition?.zoom ?: mapLibreMap!!.minZoomLevel

    Log.i(
        "Camera",
        "🎯 BOUNDS mode min zoom: base=$baseMinZoom (entire event visible)",
    )
}

// No safety margin - base min zoom already ensures event fits in viewport
// The max() calculation ensures BOTH dimensions fit, allowing full event visibility
calculatedMinZoom = baseMinZoom

Log.i("Camera", "🎯 Final min zoom: $baseMinZoom (allows seeing full event, no margin)")

// Set min zoom IMMEDIATELY
mapLibreMap!!.setMinZoomPreference(calculatedMinZoom)
minZoomLocked = true
```

---

## 2. Preventive Gesture Clamping Pattern

**Location**: `AndroidMapLibreAdapter.kt` lines 516-591

```kotlin
private fun setupPreventiveGestureConstraints() {
    val map = mapLibreMap ?: return

    Log.i(TAG, "Setting up preventive gesture constraints (one-time setup)")

    // Track when gestures start
    map.addOnCameraMoveStartedListener { reason ->
        isGestureInProgress =
            when (reason) {
                MapLibreMap.OnCameraMoveStartedListener.REASON_API_GESTURE,
                MapLibreMap.OnCameraMoveStartedListener.REASON_API_ANIMATION,
                MapLibreMap.OnCameraMoveStartedListener.REASON_DEVELOPER_ANIMATION,
                -> false // All programmatic movements (including button animations)
                else -> true // Only user gestures (pan, pinch, etc.)
            }

        if (!isGestureInProgress) {
            // Save position before programmatic animation
            lastValidCameraPosition = map.cameraPosition.target
        }
    }

    // Intercept camera movements during gestures (PREVENTIVE)
    map.addOnCameraMoveListener {
        if (!isGestureInProgress) return@addOnCameraMoveListener

        val currentCamera = map.cameraPosition.target ?: return@addOnCameraMoveListener
        val viewport = getVisibleRegion()
        val constraintBounds = currentConstraintBounds ?: return@addOnCameraMoveListener

        // Check if viewport exceeds event bounds
        if (!isViewportWithinBounds(viewport, constraintBounds)) {
            // Viewport exceeds bounds - clamp camera position IMMEDIATELY
            val clampedPosition =
                clampCameraToKeepViewportInside(
                    Position(currentCamera.latitude, currentCamera.longitude),
                    viewport,
                    constraintBounds,
                )

            // Only move if position actually changed (avoid infinite loop)
            if (kotlin.math.abs(clampedPosition.latitude - currentCamera.latitude) > 0.000001 ||
                kotlin.math.abs(clampedPosition.longitude - currentCamera.longitude) > 0.000001
            ) {
                Log.v(TAG, "Gesture intercepted: viewport would exceed bounds, clamping camera")

                // Move camera to clamped position WITHOUT animation (instant)
                map.cameraPosition =
                    CameraPosition
                        .Builder()
                        .target(
                            org.maplibre.android.geometry
                                .LatLng(clampedPosition.latitude, clampedPosition.longitude),
                        ).zoom(map.cameraPosition.zoom)
                        .build()
            }
        }
    }

    // Track when gesture ends
    map.addOnCameraIdleListener {
        if (isGestureInProgress) {
            // Gesture ended - save this as last valid position
            lastValidCameraPosition = map.cameraPosition.target
            isGestureInProgress = false
            Log.v(TAG, "Gesture ended, saved valid position")
        }
    }

    Log.i(TAG, "✅ Preventive gesture constraints active")
}

/**
 * Check if entire viewport is within event bounds.
 */
private fun isViewportWithinBounds(
    viewport: BoundingBox,
    eventBounds: BoundingBox,
): Boolean =
    viewport.southLatitude >= eventBounds.southwest.latitude &&
        viewport.northLatitude <= eventBounds.northeast.latitude &&
        viewport.westLongitude >= eventBounds.southwest.longitude &&
        viewport.eastLongitude <= eventBounds.northeast.longitude

/**
 * Clamp camera position to ensure viewport stays within event bounds.
 */
private fun clampCameraToKeepViewportInside(
    currentCamera: Position,
    currentViewport: BoundingBox,
    eventBounds: BoundingBox,
): Position {
    // Calculate viewport half-dimensions
    val viewportHalfHeight = (currentViewport.northLatitude - currentViewport.southLatitude) / 2.0
    val viewportHalfWidth = (currentViewport.eastLongitude - currentViewport.westLongitude) / 2.0

    // Calculate valid camera center range
    val minValidLat = eventBounds.southwest.latitude + viewportHalfHeight
    val maxValidLat = eventBounds.northeast.latitude - viewportHalfHeight
    val minValidLng = eventBounds.southwest.longitude + viewportHalfWidth
    val maxValidLng = eventBounds.northeast.longitude + viewportHalfWidth

    // Handle case where viewport > event bounds (shouldn't happen with min zoom)
    if (minValidLat > maxValidLat || minValidLng > maxValidLng) {
        val centerLat = (eventBounds.southwest.latitude + eventBounds.northeast.latitude) / 2.0
        val centerLng = (eventBounds.southwest.longitude + eventBounds.northeast.longitude) / 2.0
        return Position(centerLat, centerLng)
    }

    // Clamp camera position to valid range
    val clampedLat = currentCamera.latitude.coerceIn(minValidLat, maxValidLat)
    val clampedLng = currentCamera.longitude.coerceIn(minValidLng, maxValidLng)

    return Position(clampedLat, clampedLng)
}
```

---

## 3. Viewport Padding Calculation Pattern (BOUNDS vs WINDOW)

**Location**: `MapBoundsEnforcer.kt` lines 383-435

```kotlin
@Suppress("ReturnCount") // Multiple returns for different modes
private fun calculateVisibleRegionPadding(): VisibleRegionPadding {
    // CRITICAL DISTINCTION: WINDOW mode vs BOUNDS mode
    //
    // WINDOW mode (full map screen with gestures):
    //   - Need viewport padding to prevent edge overflow
    //   - setLatLngBoundsForCameraTarget() only constrains camera CENTER
    //   - Shrink bounds by viewport half-size to keep viewport inside event area
    //
    // BOUNDS mode (event detail screen, no gestures):
    //   - Want to show ENTIRE event area
    //   - No viewport padding needed (gestures disabled, no panning to edges)
    //   - Zero padding = constraint bounds = event bounds = entire event visible

    if (!isWindowMode) {
        // BOUNDS mode: No padding, show entire event
        Log.d(
            "MapBoundsEnforcer",
            "BOUNDS mode: Using zero padding (want entire event visible, gestures disabled)",
        )
        return VisibleRegionPadding(0.0, 0.0)
    }

    // WINDOW mode: Calculate padding from current viewport to constrain camera center
    // iOS: Uses these bounds directly (no runtime clamping, shouldChangeFrom only checks zoom)
    // Android: Initial constraints, then preventive gesture system clamps in real-time
    // Padding = viewport half-size ensures camera center range keeps viewport inside event bounds

    val viewport = mapLibreAdapter.getVisibleRegion()

    // Detect invalid viewport (>10° span indicates uninitialized map)
    val viewportLatSpan = viewport.ne.lat - viewport.sw.lat
    val viewportLngSpan = viewport.ne.lng - viewport.sw.lng

    if (viewportLatSpan > 10.0 || viewportLngSpan > 10.0) {
        Log.d(
            "MapBoundsEnforcer",
            "WINDOW mode: Invalid viewport detected ($viewportLatSpan° x $viewportLngSpan°), using zero padding until initialized",
        )
        return VisibleRegionPadding(0.0, 0.0)
    }

    val viewportHalfHeight = viewportLatSpan / 2.0
    val viewportHalfWidth = viewportLngSpan / 2.0

    Log.d(
        "MapBoundsEnforcer",
        "WINDOW mode: Viewport-based padding (viewport: $viewportLatSpan° x $viewportLngSpan°, " +
            "padding: $viewportHalfHeight° x $viewportHalfWidth°)",
    )

    return VisibleRegionPadding(viewportHalfHeight, viewportHalfWidth)
}

private fun calculatePaddedBounds(padding: VisibleRegionPadding): BoundingBox {
    Log.d("MapBoundsEnforcer", "mapBounds: SW(${mapBounds.sw.lat},${mapBounds.sw.lng}) NE(${mapBounds.ne.lat},${mapBounds.ne.lng})")
    Log.d("MapBoundsEnforcer", "padding: lat=${padding.latPadding}, lng=${padding.lngPadding}")

    // VIEWPORT EDGE CLAMPING: Shrink constraint bounds by viewport size
    // This ensures camera center cannot move where viewport edges would exceed event bounds
    // padding = viewportSize / 2, so we add/subtract it from event bounds to get valid camera center range

    // CRITICAL iOS FIX: Prevent bounds inversion when viewport > event (e.g., zoomed out on full map)
    // If padding >= eventSize/2, the result would have SW > NE, which iOS rejects as invalid
    // Clamp padding to 49% of event size to guarantee valid bounds with small center region
    val eventLatSpan = mapBounds.northeast.latitude - mapBounds.southwest.latitude
    val eventLngSpan = mapBounds.northeast.longitude - mapBounds.southwest.longitude

    // Use 49% (not 50%) to ensure a small valid region remains even when heavily zoomed out
    val maxLatPadding = eventLatSpan * 0.49
    val maxLngPadding = eventLngSpan * 0.49

    val effectiveLatPadding = min(padding.latPadding, maxLatPadding)
    val effectiveLngPadding = min(padding.lngPadding, maxLngPadding)

    if (effectiveLatPadding < padding.latPadding || effectiveLngPadding < padding.lngPadding) {
        Log.d(
            "MapBoundsEnforcer",
            "Clamping padding to prevent invalid bounds on iOS: " +
                "requested=(${padding.latPadding}, ${padding.lngPadding}), " +
                "clamped=($effectiveLatPadding, $effectiveLngPadding)",
        )
    }

    return BoundingBox.fromCorners(
        Position(
            mapBounds.southwest.latitude + effectiveLatPadding, // SW moves inward
            mapBounds.southwest.longitude + effectiveLngPadding, // SW moves inward
        ),
        Position(
            mapBounds.northeast.latitude - effectiveLatPadding, // NE moves inward
            mapBounds.northeast.longitude - effectiveLngPadding, // NE moves inward
        ),
    )
}
```

---

## 4. Mode-Specific Constraint Application (AbstractEventMap)

**Location**: `AbstractEventMap.kt` lines 124-228

```kotlin
/**
 * Moves the camera to view the event bounds
 * BOUNDS MODE: Shows entire event, gestures disabled
 */
suspend fun moveToMapBounds(onComplete: () -> Unit = {}) {
    Log.i(
        "AbstractEventMap",
        "📐 moveToMapBounds: Starting for event ${event.id}, bounds=${event.area.bbox()}",
    )
    // Initialize constraint manager with BOUNDS mode (zero padding for tight fit)
    constraintManager = MapBoundsEnforcer(event.area.bbox(), mapLibreAdapter, isWindowMode = false) { suppressCorrections }

    val bounds = event.area.bbox()
    Log.d(
        "AbstractEventMap",
        "Animating camera to bounds: SW(${bounds.sw.lat}, ${bounds.sw.lng}) NE(${bounds.ne.lat}, ${bounds.ne.lng})",
    )
    runCameraAnimation { cb ->
        mapLibreAdapter.animateCameraToBounds(
            bounds,
            padding = 0, // No padding - constraints handle viewport clamping
            callback =
                object : MapCameraCallback {
                    override fun onFinish() {
                        Log.i(
                            "AbstractEventMap",
                            "✅ moveToMapBounds animation completed for event ${event.id}",
                        )
                        // Apply constraints FIRST - this calculates and sets the correct min zoom
                        // based on bounds that fit perfectly without padding
                        constraintManager?.applyConstraints()

                        // Get the calculated min zoom from constraints
                        val calculatedMinZoom = mapLibreAdapter.getMinZoomLevel()
                        Log.d(
                            "AbstractEventMap",
                            "BOUNDS mode: Min zoom from constraints: $calculatedMinZoom",
                        )

                        mapLibreAdapter.setMaxZoomPreference(event.map.maxZoom)
                        cb.onFinish()
                        onComplete()
                    }

                    override fun onCancel() {
                        Log.w(
                            "AbstractEventMap",
                            "⚠️ moveToMapBounds animation CANCELLED for event ${event.id}",
                        )
                        cb.onCancel()
                        onComplete()
                    }
                },
        )
    }
}

/**
 * Adjusts the camera to fit the event area with strict bounds (no expansion beyond event area).
 * WINDOW MODE: User can pan/zoom within bounds
 */
suspend fun moveToWindowBounds(onComplete: () -> Unit = {}) {
    // Capture event bbox before animation (needed for callback which is not suspend)
    val eventBbox = event.area.bbox()

    // Prepare bounds enforcer with WINDOW mode (strict viewport enforcement)
    constraintManager = MapBoundsEnforcer(eventBbox, mapLibreAdapter, isWindowMode = true) { suppressCorrections }

    val (sw, ne) = eventBbox
    val eventMapWidth = ne.lng - sw.lng
    val eventMapHeight = ne.lat - sw.lat
    val (centerLat, centerLng) = event.area.getCenter()

    // Calculate the aspect ratios of the event map and screen
    val eventAspectRatio = eventMapWidth / eventMapHeight
    val screenComponentRatio = screenWidth / screenHeight

    // CRITICAL: Determine which dimension to fit by
    // Rule: The dimension that would cause overflow must be the constraining dimension
    val fitByHeight = eventAspectRatio > screenComponentRatio

    Log.i(
        "AbstractEventMap",
        "📐 moveToWindowBounds: event=${event.id}, " +
            "eventSize=$eventMapWidth°x$eventMapHeight° (aspect=$eventAspectRatio), " +
            "screenSize=${screenWidth}x${screenHeight}px (aspect=$screenComponentRatio), " +
            "fitBy=${if (fitByHeight) "HEIGHT" else "WIDTH"} " +
            "(ensures NO pixels outside event area)",
    )

    // CRITICAL: Apply constraints BEFORE any camera movement
    // This ensures min zoom is set IMMEDIATELY (preventive enforcement)
    constraintManager?.applyConstraints()

    // WINDOW mode: Don't animate camera - let autoTargetUserOnFirstLocation or manual gestures control initial view
    // Constraints are still applied to enforce boundaries
    Log.i(
        "AbstractEventMap",
        "✅ moveToWindowBounds: Constraints applied (WINDOW mode - no camera animation, " +
            "initial view controlled by autoTargetUserOnFirstLocation or user interaction)",
    )
    mapLibreAdapter.setMaxZoomPreference(event.map.maxZoom)
    onComplete()
}
```

---

## 5. Min Zoom Locking Pattern

**Location**: `AndroidMapLibreAdapter.kt` lines 396-502

```kotlin
// Track constraint bounds for viewport clamping (matches iOS pattern)
private var currentConstraintBounds: BoundingBox? = null
private var calculatedMinZoom: Double = 0.0
private var minZoomLocked = false // Track if min zoom has been set (to prevent zoom-in spiral)

override fun setBoundsForCameraTarget(
    constraintBounds: BoundingBox,
    applyZoomSafetyMargin: Boolean,
    originalEventBounds: BoundingBox?,
) {
    require(mapLibreMap != null)

    // ... validation code ...

    // Store constraint bounds for viewport clamping (matches iOS pattern)
    currentConstraintBounds = constraintBounds

    // CRITICAL: Only calculate min zoom if not locked OR if we're now getting originalEventBounds
    // This ensures we calculate from ORIGINAL bounds (not shrunk), preventing infinite zoom out
    val shouldCalculateMinZoom = !minZoomLocked || (originalEventBounds != null && !minZoomLocked)

    if (shouldCalculateMinZoom && originalEventBounds != null) {
        // ... calculate min zoom (see pattern above) ...

        // Set min zoom IMMEDIATELY
        mapLibreMap!!.setMinZoomPreference(calculatedMinZoom)
        Log.e(
            "Camera",
            "🚨 SET MIN ZOOM: $calculatedMinZoom - ${if (applyZoomSafetyMargin) "NO PIXELS OUTSIDE" else "ENTIRE EVENT"} 🚨",
        )

        minZoomLocked = true
        Log.i("Camera", "✅ Min zoom LOCKED at $calculatedMinZoom")
    } else if (!shouldCalculateMinZoom) {
        Log.v(
            "Camera",
            "Min zoom already locked at $calculatedMinZoom, skipping recalculation",
        )
    } else {
        Log.w("Camera", "⚠️ Min zoom NOT calculated (originalEventBounds is null)")
    }

    // Set the underlying MapLibre bounds (constrains camera center only)
    mapLibreMap!!.setLatLngBoundsForCameraTarget(constraintBounds.toLatLngBounds())

    // Setup preventive gesture constraints to enforce viewport bounds
    // Constraint bounds are now calculated from viewport at MIN ZOOM (not current)
    // This allows zooming to min zoom while preventing viewport overflow
    if (applyZoomSafetyMargin && !gestureConstraintsActive) {
        setupPreventiveGestureConstraints()
        gestureConstraintsActive = true
    }
}
```

---

## 6. Bounds Similarity Check Pattern

**Location**: `MapBoundsEnforcer.kt` lines 177-197

```kotlin
/**
 * Check if two bounding boxes are similar enough to be considered the same (within 0.1% tolerance).
 * This prevents infinite loops caused by floating-point precision issues and iOS camera idle callbacks.
 */
private fun boundsAreSimilar(
    bounds1: BoundingBox,
    bounds2: BoundingBox,
): Boolean {
    val tolerance = 0.001 // 0.1% tolerance for coordinate comparison

    val latDiff =
        abs(bounds1.southwest.latitude - bounds2.southwest.latitude) +
            abs(bounds1.northeast.latitude - bounds2.northeast.latitude)
    val lngDiff =
        abs(bounds1.southwest.longitude - bounds2.southwest.longitude) +
            abs(bounds1.northeast.longitude - bounds2.northeast.longitude)

    val isSimilar = latDiff < tolerance && lngDiff < tolerance

    if (!isSimilar) {
        Log.d("MapBoundsEnforcer", "Bounds changed significantly: latDiff=$latDiff, lngDiff=$lngDiff")
    }

    return isSimilar
}

/**
 * Dynamic constraint update on camera idle
 */
private fun applyConstraintsWithPadding() {
    try {
        val paddedBounds = calculateConstraintBounds()
        constraintBounds = paddedBounds

        // Prevent infinite loop: skip if bounds haven't changed significantly
        if (lastAppliedBounds != null && boundsAreSimilar(lastAppliedBounds!!, paddedBounds)) {
            Log.v("MapBoundsEnforcer", "Bounds unchanged, skipping redundant constraint update")
            return
        }

        // Apply bounds & min-zoom to the map
        mapLibreAdapter.setBoundsForCameraTarget(
            constraintBounds = paddedBounds,
            applyZoomSafetyMargin = isWindowMode,
            originalEventBounds = mapBounds,
        )

        // Track the bounds we just applied
        lastAppliedBounds = paddedBounds
    } catch (e: Exception) {
        Napier.e("Error applying constraints: ${e.message}")
    }
}
```

---

## 7. Animation Suppression Pattern

**Location**: `AbstractEventMap.kt` lines 105-119

```kotlin
/**
 * Executes a map-camera animation while temporarily disabling
 * reactive constraint corrections so that the ConstraintManager does not
 * fight against the animation.
 *
 * NOTE: Does NOT relax bounds - constraints remain enforced during animation
 * to prevent zooming out beyond event area (preventive clamping).
 */
private suspend inline fun runCameraAnimation(crossinline block: (MapCameraCallback) -> Unit) {
    suppressCorrections = true

    block(
        object : MapCameraCallback {
            override fun onFinish() {
                suppressCorrections = false
            }

            override fun onCancel() {
                suppressCorrections = false
            }
        },
    )
}
```

And usage in MapBoundsEnforcer:

```kotlin
mapLibreAdapter.addOnCameraIdleListener {
    // Prevent recalculation if animation/user interaction is in progress
    if (isSuppressed()) {
        Log.v("MapBoundsEnforcer", "Camera idle callback suppressed (animation in progress)")
        // Set flag to skip NEXT idle after suppression ends
        skipNextRecalculation = true
        return@addOnCameraIdleListener
    }
    // ... rest of update logic ...
}
```

---

## Key Takeaways

1. **Min Zoom Locked Once**: Calculated and set immediately, never recalculated
2. **Aspect Ratio Critical**: Determines which dimension constrains (WIDTH or HEIGHT)
3. **Padding by Mode**: BOUNDS = 0, WINDOW = viewport half-size
4. **Gesture Distinction**: Only clamp user gestures, not programmatic animations
5. **Bounds Similarity**: Use 0.1% tolerance to prevent infinite loops
6. **Preventive > Reactive**: Set constraints before gestures, not after
