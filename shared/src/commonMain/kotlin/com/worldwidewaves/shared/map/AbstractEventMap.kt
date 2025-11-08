package com.worldwidewaves.shared.map

/*
 * Copyright 2025 DrWave
 *
 * WorldWideWaves is an ephemeral mobile app designed to orchestrate human waves through cities and
 * countries. The project aims to transcend physical and cultural
 * boundaries, fostering unity, community, and shared human experience by leveraging real-time
 * coordination and location-based services.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.worldwidewaves.shared.WWWGlobals.MapDisplay
import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.events.WWWEventWaveLinear
import com.worldwidewaves.shared.events.utils.BoundingBox
import com.worldwidewaves.shared.events.utils.Polygon
import com.worldwidewaves.shared.events.utils.Position
import com.worldwidewaves.shared.position.PositionManager
import com.worldwidewaves.shared.utils.Log
import com.worldwidewaves.shared.utils.PerformanceTracer
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

// ----------------------------------------------------------------------------

interface MapCameraCallback { // Call back for camera animations
    fun onFinish()

    fun onCancel()
}

data class EventMapConfig( // Type of EventMap initial view setup
    val initialCameraPosition: MapCameraPosition = MapCameraPosition.BOUNDS,
    val autoTargetUserOnFirstLocation: Boolean = false,
    val gesturesEnabled: Boolean = false, // Controls whether user can interact with map (pan/zoom/rotate)
)

enum class MapCameraPosition {
    WINDOW, // Fit map to window with proper aspect ratio
    BOUNDS, // Show entire event bounds
    DEFAULT_CENTER, // Center on the event's center point
}

// ----------------------------------------------------------------------------

/**
 * Abstract base class for EventMap functionality that's shared across platforms.
 *
 * This class provides the core map interaction logic for event visualization, including:
 * - Camera positioning and animation (bounds, center, user/wave targeting)
 * - Constraint enforcement to keep map view within event area
 * - Location tracking and position updates via PositionManager
 * - Wave polygon rendering and progression tracking
 * - Gesture control and user interaction handling
 *
 * Platform-specific subclasses provide:
 * - MapLibreAdapter implementation for native map library
 * - LocationProvider for platform-specific GPS/location services
 *
 * Architecture documentation:
 * - See docs/architecture/map-architecture-analysis.md for detailed system design
 * - See docs/ios/ios-map-implementation-status.md for iOS-specific implementation
 *
 * @param T The platform-specific map type (e.g., MapView on Android, MLNMapView on iOS)
 * @param event The event to display on the map
 * @param mapConfig Configuration for initial camera position and gesture control
 * @param onLocationUpdate Callback invoked when user position changes
 */
abstract class AbstractEventMap<T>(
    protected val event: IWWWEvent,
    protected val mapConfig: EventMapConfig = EventMapConfig(),
    private val onLocationUpdate: (Position) -> Unit,
) : KoinComponent {
    companion object {
        // Threshold for detecting significant dimension changes (10%)
        private const val DIMENSION_CHANGE_THRESHOLD = 0.1
    }

    // Properties that must be implemented by platform-specific subclasses
    abstract val mapLibreAdapter: MapLibreAdapter<T> // MapLibre is native map library
    abstract val locationProvider: LocationProvider? // LocationProvider is native location provider

    // Dependencies
    private val positionManager: PositionManager by inject()

    // Class variables
    private var constraintManager: MapBoundsEnforcer? = null // Map bounds enforcer
    private var screenHeight: Double = 800.0
    private var screenWidth: Double = 600.0
    private var userHasBeenLocated = false
    private var lastKnownPosition: Position? = null
    private var userInteracted = false
    private var windowBoundsNeedRecalculation = false // Track if WINDOW bounds need recalc after dimension change

    /** When true the MapBoundsEnforcer is not allowed to move the camera. */
    private var suppressCorrections = false

    // GPS lifecycle tracking
    private var locationUpdatesActive = false

    // Camera position methods - shared logic for all platforms ---------------

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

    /**
     * Moves the camera to view the event bounds
     */
    suspend fun moveToMapBounds(onComplete: () -> Unit = {}) {
        val bounds = event.area.bbox()

        // Defensive logging: Detect invalid bbox that would cause world view
        val isInvalidBbox =
            bounds.sw.lat == 0.0 &&
                bounds.sw.lng == 0.0 &&
                bounds.ne.lat == 0.0 &&
                bounds.ne.lng == 0.0
        if (isInvalidBbox) {
            Log.d(
                "AbstractEventMap",
                "moveToMapBounds: Invalid bbox (0,0,0,0) for event ${event.id}. " +
                    "Skipping camera movement - GeoJSON not loaded yet (expected for non-downloaded maps).",
            )
            onComplete()
            return
        }

        Log.d(
            "AbstractEventMap",
            "moveToMapBounds: Using bbox SW(${bounds.sw.lat},${bounds.sw.lng}) " +
                "NE(${bounds.ne.lat},${bounds.ne.lng}) for event ${event.id}",
        )

        // Initialize constraint manager with BOUNDS mode (zero padding for tight fit)
        constraintManager = MapBoundsEnforcer(bounds, mapLibreAdapter, isWindowMode = false) { suppressCorrections }

        runCameraAnimation { cb ->
            mapLibreAdapter.animateCameraToBounds(
                bounds,
                padding = 0, // No padding - constraints handle viewport clamping
                callback =
                    object : MapCameraCallback {
                        override fun onFinish() {
                            // Apply constraints FIRST - this calculates and sets the correct min zoom
                            // based on bounds that fit perfectly without padding
                            constraintManager?.applyConstraints()

                            // Verify min zoom was calculated (required by tests)
                            mapLibreAdapter.getMinZoomLevel()

                            mapLibreAdapter.setMaxZoomPreference(event.map.maxZoom)
                            cb.onFinish()
                            onComplete()
                        }

                        override fun onCancel() {
                            cb.onCancel()
                            onComplete()
                        }
                    },
            )
        }
    }

    /**
     * Adjusts the camera to fit the event area with strict bounds (no expansion beyond event area).
     *
     * WINDOW mode (full map with gestures):
     * - Constraints are applied to prevent viewport from exceeding event boundaries
     * - Camera initializes to event bounds to ensure tiles are visible
     * - Then autoTargetUserOnFirstLocation can move to user position (if configured)
     * - Min zoom calculated to prevent zooming out beyond event area
     */
    suspend fun moveToWindowBounds(onComplete: () -> Unit = {}) {
        // Capture event bbox before animation (needed for callback which is not suspend)
        val eventBbox = event.area.bbox()

        // Defensive logging: Detect invalid bbox that would cause world view
        val isInvalidBbox =
            eventBbox.sw.lat == 0.0 &&
                eventBbox.sw.lng == 0.0 &&
                eventBbox.ne.lat == 0.0 &&
                eventBbox.ne.lng == 0.0
        if (isInvalidBbox) {
            Log.w(
                "AbstractEventMap",
                "moveToWindowBounds: Invalid bbox (0,0,0,0) detected for event ${event.id}. " +
                    "Camera will show entire world. GeoJSON may not be loaded yet.",
            )
        } else {
            Log.d(
                "AbstractEventMap",
                "moveToWindowBounds: Using bbox SW(${eventBbox.sw.lat},${eventBbox.sw.lng}) " +
                    "NE(${eventBbox.ne.lat},${eventBbox.ne.lng}) for event ${event.id}",
            )
        }

        // Prepare bounds enforcer with WINDOW mode (strict viewport enforcement)
        constraintManager = MapBoundsEnforcer(eventBbox, mapLibreAdapter, isWindowMode = true) { suppressCorrections }

        // Apply constraints BEFORE any camera movement
        // This ensures min zoom is set IMMEDIATELY (preventive enforcement)
        constraintManager?.applyConstraints()

        // WINDOW mode: Initialize camera to event bounds first to ensure tiles are visible
        // This prevents gray screen when user is outside tile coverage area
        // After this, autoTargetUserOnFirstLocation can move camera to user position
        runCameraAnimation { cb ->
            mapLibreAdapter.animateCameraToBounds(
                eventBbox,
                padding = 0,
                callback =
                    object : MapCameraCallback {
                        override fun onFinish() {
                            mapLibreAdapter.setMaxZoomPreference(event.map.maxZoom)
                            cb.onFinish()
                            onComplete()
                        }

                        override fun onCancel() {
                            mapLibreAdapter.setMaxZoomPreference(event.map.maxZoom)
                            cb.onCancel()
                            onComplete()
                        }
                    },
            )
        }
    }

    /**
     * Moves the camera to the center of the event
     */
    suspend fun moveToCenter(onComplete: () -> Unit = {}) {
        val (centerLat, centerLng) = event.area.getCenter()
        runCameraAnimation { cb ->
            mapLibreAdapter.animateCamera(
                Position(centerLat, centerLng),
                null,
                object : MapCameraCallback {
                    override fun onFinish() {
                        cb.onFinish()
                        onComplete()
                    }

                    override fun onCancel() {
                        cb.onCancel()
                        onComplete()
                    }
                },
            )
        }
    }

    // Camera targeting methods - shared logic for all platforms --------------

    /**
     * Moves the camera to show the wave front edge, maximizing detail.
     * Creates a bounding box that spans the full vertical extent of the wave front edge
     * (min/max latitude) at the actual wave front longitude, focusing the camera on the wave.
     * When wave edge bounds are unavailable, uses fixed zoom level centered on wave front.
     * Falls back to user latitude + wave longitude if wave front is unavailable.
     */
    @Suppress("ComplexCondition") // Necessary validation: null + NaN checks prevent IllegalArgumentException
    suspend fun targetWave() {
        val userPosition = positionManager.getCurrentPosition()

        // Get wave edge bounds for proper vertical extent
        val waveEdgeBounds =
            (event.wave as? WWWEventWaveLinear)?.getWaveFrontEdgeBounds()

        // If wave edge bounds are available, use dynamic zoom focusing on wave front
        if (waveEdgeBounds != null) {
            // Get the area's bounding box for constraints
            val areaBbox = event.area.bbox()

            // Extract wave front coordinates: (minLatitude, maxLatitude, leadingEdgeLongitude)
            val waveLongitude = waveEdgeBounds.third

            // Constrain wave front coordinates to event area bounds to prevent camera going outside
            val constrainedLongitude = waveLongitude.coerceIn(areaBbox.sw.lng, areaBbox.ne.lng)
            val constrainedMinLat = waveEdgeBounds.first.coerceIn(areaBbox.sw.lat, areaBbox.ne.lat)
            val constrainedMaxLat = waveEdgeBounds.second.coerceIn(areaBbox.sw.lat, areaBbox.ne.lat)

            // Create bounds that include full wave edge vertical extent
            // This ensures the wave front is visible with proper vertical extent
            val positions =
                listOf(
                    Position(constrainedMinLat, constrainedLongitude), // Wave edge min latitude
                    Position(constrainedMaxLat, constrainedLongitude), // Wave edge max latitude
                )

            val bounds = BoundingBox.fromCorners(positions)
            if (bounds == null) {
                Log.e("AbstractEventMap", "Failed to create bounds from wave edge, using fallback")
                // Fallback to old behavior
                targetWaveFallback(userPosition)
                return
            }
            val eventLatSpan = areaBbox.ne.lat - areaBbox.sw.lat
            val eventLngSpan = areaBbox.ne.lng - areaBbox.sw.lng

            // Calculate current bounds span
            val currentHorizontalSpan = bounds.eastLongitude - bounds.westLongitude
            val currentVerticalSpan = bounds.northLatitude - bounds.southLatitude

            // Define minimum spans to ensure we use available screen space (25% horizontal, 20% vertical)
            val minHorizontalSpan = eventLngSpan * 0.25
            val minVerticalSpan = eventLatSpan * 0.20

            // Extend bounds to minimum span if too narrow (uses full available screen space)
            val extendedBounds =
                if (currentHorizontalSpan < minHorizontalSpan || currentVerticalSpan < minVerticalSpan) {
                    // Calculate center of current bounds
                    val boundsCenter =
                        Position(
                            (bounds.northLatitude + bounds.southLatitude) / 2,
                            (bounds.eastLongitude + bounds.westLongitude) / 2,
                        )

                    // Use maximum of current span and minimum span
                    val finalHorizontalSpan = maxOf(currentHorizontalSpan, minHorizontalSpan)
                    val finalVerticalSpan = maxOf(currentVerticalSpan, minVerticalSpan)

                    // Create extended bounds centered on original bounds center
                    BoundingBox.fromCorners(
                        Position(
                            boundsCenter.lat - finalVerticalSpan / 2,
                            boundsCenter.lng - finalHorizontalSpan / 2,
                        ),
                        Position(
                            boundsCenter.lat + finalVerticalSpan / 2,
                            boundsCenter.lng + finalHorizontalSpan / 2,
                        ),
                    ) ?: bounds // Fallback to original if extension fails
                } else {
                    bounds // Bounds already large enough
                }

            // Calculate smaller padding since we've already ensured minimum span
            val horizontalPadding = eventLngSpan * 0.08
            val verticalPadding = eventLatSpan * 0.05

            // Create padded bounds ensuring we stay within event area boundaries
            val finalBounds =
                BoundingBox.fromCorners(
                    Position(
                        maxOf(
                            extendedBounds.southLatitude - minOf(verticalPadding, extendedBounds.southLatitude - areaBbox.sw.lat),
                            areaBbox.sw.lat,
                        ),
                        maxOf(
                            extendedBounds.westLongitude - minOf(horizontalPadding, extendedBounds.westLongitude - areaBbox.sw.lng),
                            areaBbox.sw.lng,
                        ),
                    ),
                    Position(
                        minOf(
                            extendedBounds.northLatitude + minOf(verticalPadding, areaBbox.ne.lat - extendedBounds.northLatitude),
                            areaBbox.ne.lat,
                        ),
                        minOf(
                            extendedBounds.eastLongitude + minOf(horizontalPadding, areaBbox.ne.lng - extendedBounds.eastLongitude),
                            areaBbox.ne.lng,
                        ),
                    ),
                )

            runCameraAnimation { cb ->
                mapLibreAdapter.animateCameraToBounds(
                    finalBounds,
                    callback =
                        object : MapCameraCallback {
                            override fun onFinish() {
                                // Re-apply constraints to ensure map bounds are enforced after animation
                                // This prevents users from panning outside event area after targetWave()
                                constraintManager?.applyConstraints()
                                cb.onFinish()
                            }

                            override fun onCancel() {
                                cb.onCancel()
                            }
                        },
                )
            }
        } else {
            // No user position or wave edge bounds available, use fallback
            targetWaveFallback(userPosition)
        }
    }

    /**
     * Fallback implementation for targetWave when edge bounds are not available.
     * Uses wave front center position or fixed zoom.
     */
    private suspend fun targetWaveFallback(userPosition: Position?) {
        var wavePosition = event.wave.getWaveFrontCenterPosition()

        // Fallback to previous implementation if new method fails
        if (wavePosition == null) {
            Log.w("AbstractEventMap", "Wave front center position not available, falling back to user position")
            val currentLocation = userPosition
            val closestWaveLongitude = event.wave.userClosestWaveLongitude()

            if (currentLocation != null && closestWaveLongitude != null) {
                wavePosition = Position(currentLocation.latitude, closestWaveLongitude)
            } else {
                Log.w("AbstractEventMap", "targetWave called but position not available")
                return
            }
        }

        // Use fixed zoom centered on wave
        runCameraAnimation { cb ->
            mapLibreAdapter.animateCamera(wavePosition, MapDisplay.TARGET_WAVE_ZOOM, cb)
        }
    }

    /**
     * Moves the camera to the current user position
     */
    suspend fun targetUser() {
        val userPosition = positionManager.getCurrentPosition()
        if (userPosition == null) {
            Log.w("AbstractEventMap", "targetUser called but position not available")
            return
        }

        runCameraAnimation { cb ->
            mapLibreAdapter.animateCamera(
                userPosition,
                MapDisplay.TARGET_USER_ZOOM,
                object : MapCameraCallback {
                    override fun onFinish() {
                        // Re-apply constraints to ensure map bounds are enforced after animation
                        // This prevents unexpected camera movement from stale constraint bounds
                        constraintManager?.applyConstraints()
                        cb.onFinish()
                    }

                    override fun onCancel() {
                        cb.onCancel()
                    }
                },
            )
        }
    }

    /**
     * Marks that the user has manually interacted with the map (button click, gesture, …).
     * Call this from UI code before executing a manual camera action so that automatic
     * first-location targeting does not fight with the user’s intention.
     */
    fun markUserInteracted() {
        userInteracted = true
    }

    /**
     * Moves the camera to show both the user and wave positions with dynamic bounding box zoom.
     * Creates a bounding box containing both positions with 20% horizontal and 10% vertical padding,
     * ensuring both user position and wave front center are always visible on screen.
     * Falls back to user latitude + wave longitude if wave front center is unavailable.
     * Zoom level adapts automatically to fit both positions within the event area boundaries.
     */
    @Suppress("ComplexCondition") // Necessary validation: null + NaN checks prevent IllegalArgumentException
    suspend fun targetUserAndWave() {
        val userPosition = positionManager.getCurrentPosition()
        var wavePosition = event.wave.getWaveFrontCenterPosition()

        // Fallback to previous implementation if new method fails
        if (wavePosition == null && userPosition != null) {
            val closestWaveLongitude = event.wave.userClosestWaveLongitude()
            if (closestWaveLongitude != null) {
                wavePosition = Position(userPosition.latitude, closestWaveLongitude)
                Log.w("AbstractEventMap", "Wave front center position not available, using fallback position")
            }
        }

        // Validate that we have valid data (not null and not NaN)
        if (userPosition == null ||
            wavePosition == null ||
            userPosition.latitude.isNaN() ||
            userPosition.longitude.isNaN() ||
            wavePosition.latitude.isNaN() ||
            wavePosition.longitude.isNaN()
        ) {
            Log.w(
                "AbstractEventMap",
                "WARNING: targetUserAndWave: missing or invalid data (userPos=$userPosition, wavePos=$wavePosition)",
            )
            return
        }

        // Create the bounds containing user and wave positions
        val bounds = BoundingBox.fromCorners(listOf(userPosition, wavePosition))
        if (bounds == null) {
            Log.e("AbstractEventMap", "Failed to create bounds from user+wave positions")
            return
        }

        // Get the area's bounding box for constraints
        val areaBbox = event.area.bbox()
        val eventLatSpan = areaBbox.ne.lat - areaBbox.sw.lat
        val eventLngSpan = areaBbox.ne.lng - areaBbox.sw.lng

        // Calculate padding as percentages of the event area dimensions
        val horizontalPadding = eventLngSpan * 0.2
        val verticalPadding = eventLatSpan * 0.1

        // Create padded bounds ensuring we stay within event area boundaries
        val finalBounds =
            BoundingBox.fromCorners(
                Position(
                    maxOf(bounds.southLatitude - minOf(verticalPadding, bounds.southLatitude - areaBbox.sw.lat), areaBbox.sw.lat),
                    maxOf(bounds.westLongitude - minOf(horizontalPadding, bounds.westLongitude - areaBbox.sw.lng), areaBbox.sw.lng),
                ),
                Position(
                    minOf(bounds.northLatitude + minOf(verticalPadding, areaBbox.ne.lat - bounds.northLatitude), areaBbox.ne.lat),
                    minOf(bounds.eastLongitude + minOf(horizontalPadding, areaBbox.ne.lng - bounds.eastLongitude), areaBbox.ne.lng),
                ),
            )

        runCameraAnimation { _ ->
            mapLibreAdapter.animateCameraToBounds(finalBounds)
        }
    }

    // ------------------------------------------------------------------------

    /**
     * Sets up the map with initial configuration
     */
    fun setupMap(
        map: T,
        scope: CoroutineScope,
        stylePath: String,
        onMapLoaded: () -> Unit = {},
        onMapClick: ((Double, Double) -> Unit)? = null,
    ) {
        val trace = PerformanceTracer.startTrace("map_rendering")
        // Pass the map to the adapter
        mapLibreAdapter.setMap(map)

        // Set screen dimensions
        this.screenWidth = mapLibreAdapter.getWidth()
        this.screenHeight = mapLibreAdapter.getHeight()
        trace.putMetric("map_width_px", screenWidth.toLong())
        trace.putMetric("map_height_px", screenHeight.toLong())

        Log.i(
            "AbstractEventMap",
            "DIMENSIONS: Screen dimensions set on map init: ${screenWidth}x$screenHeight px (aspect: ${screenWidth / screenHeight})",
        )

        mapLibreAdapter.setStyle(stylePath) {
            trace.putMetric("style_loaded", 1)
            // Set Attribution margins to 0
            mapLibreAdapter.setAttributionMargins(0, 0, 0, 0)

            // Configure gesture interaction based on map config
            mapLibreAdapter.setGesturesEnabled(mapConfig.gesturesEnabled)

            // Enable location component to show user position marker
            mapLibreAdapter.enableLocationComponent(true)

            // Add an explicit zone if area bbox has been overridden regarding the GEOJson standard area
            if (event.area.bboxIsOverride) {
                scope.launch {
                    mapLibreAdapter.drawOverridenBbox(event.area.bbox())
                }
            }

            // Set the click listener - platform-specific
            mapLibreAdapter.setOnMapClickListener(onMapClick)

            // Set the max zoom level from the event configuration
            mapLibreAdapter.setMaxZoomPreference(event.map.maxZoom)

            // Camera idle listener for WINDOW mode dimension changes
            mapLibreAdapter.addOnCameraIdleListener {
                // WINDOW mode: Recalculate bounds when dimensions change significantly
                if (windowBoundsNeedRecalculation && mapConfig.initialCameraPosition == MapCameraPosition.WINDOW) {
                    val currentWidth = mapLibreAdapter.getWidth()
                    val currentHeight = mapLibreAdapter.getHeight()

                    // Check if dimensions changed significantly
                    val widthChange = kotlin.math.abs(currentWidth - screenWidth) / screenWidth
                    val heightChange = kotlin.math.abs(currentHeight - screenHeight) / screenHeight

                    if (widthChange > DIMENSION_CHANGE_THRESHOLD || heightChange > DIMENSION_CHANGE_THRESHOLD) {
                        Log.i(
                            "AbstractEventMap",
                            "DIMENSIONS: Dimensions changed significantly " +
                                "(${screenWidth}x$screenHeight -> ${currentWidth}x$currentHeight), recalculating WINDOW bounds",
                        )
                        screenWidth = currentWidth
                        screenHeight = currentHeight
                        scope.launch {
                            moveToWindowBounds()
                        }
                    } else {
                        // Dimensions stabilized
                        windowBoundsNeedRecalculation = false
                    }
                }
            }

            // Pre-load event area bounds BEFORE camera setup
            // This ensures valid bounds are available for camera positioning and area checks
            // Prevents using fallback world bounds (-90,-180 to 90,180) during initialization
            // CRITICAL: This must complete BEFORE camera positioning to avoid race condition
            val bboxPreload =
                scope.async {
                    event.area.bbox()
                }

            // Configure initial camera position
            scope.launch {
                // CRITICAL FIX: Wait for bbox to be loaded from GeoJSON before positioning camera
                // Without this await(), bbox() returns cached (0,0,0,0) causing camera positioning to fail
                bboxPreload.await()
                // Wait for initial camera setup to complete before starting position updates
                // This prevents race conditions between constraint setup and auto-target animations
                val cameraSetupComplete = CompletableDeferred<Unit>()

                when (mapConfig.initialCameraPosition) {
                    MapCameraPosition.DEFAULT_CENTER -> {
                        moveToCenter {
                            trace.putMetric("camera_position", 1) // DEFAULT_CENTER
                            onMapLoaded()
                            trace.stop()
                            cameraSetupComplete.complete(Unit)
                        }
                    }
                    MapCameraPosition.BOUNDS -> {
                        moveToMapBounds {
                            trace.putMetric("camera_position", 2) // BOUNDS
                            onMapLoaded()
                            trace.stop()
                            cameraSetupComplete.complete(Unit)
                        }
                    }
                    MapCameraPosition.WINDOW -> {
                        // Mark that we should watch for dimension changes and recalculate
                        windowBoundsNeedRecalculation = true
                        moveToWindowBounds {
                            trace.putMetric("camera_position", 3) // WINDOW
                            onMapLoaded()
                            trace.stop()
                            cameraSetupComplete.complete(Unit)
                        }
                    }
                }

                // Wait for camera setup to complete before starting position updates
                cameraSetupComplete.await()
            }

            // Start location updates and integrate with PositionManager
            startLocationUpdatesInternal()

            // Subscribe to unified position updates from PositionManager
            positionManager.position
                .onEach { unifiedPosition ->
                    handlePositionUpdate(scope, unifiedPosition)
                }.launchIn(scope)

            Unit // Explicit return to satisfy callback
        }
    }

    /**
     * Handles unified position updates from PositionManager
     */
    private fun handlePositionUpdate(
        scope: CoroutineScope,
        position: Position?,
    ) {
        if (position == null) {
            // Position cleared - reset state
            lastKnownPosition = null
            return
        }

        // Auto-target the user the first time (optional) if no interaction yet
        // BUT ONLY if user is within the event area - don't move camera to positions outside tile coverage
        // Also handle marker updates to prevent camera interference on iOS
        if (mapConfig.autoTargetUserOnFirstLocation &&
            !userHasBeenLocated &&
            !userInteracted &&
            mapConfig.initialCameraPosition == MapCameraPosition.WINDOW
        ) {
            scope.launch {
                val isUserInEventArea = event.area.isPositionWithin(position)
                if (isUserInEventArea) {
                    targetUser()
                    Log.i("AbstractEventMap", "User in event area, auto-targeted user position")
                    // Also update marker when user is inside area
                    if (lastKnownPosition == null || lastKnownPosition != position) {
                        mapLibreAdapter.setUserPosition(position)
                    }
                } else {
                    // User outside event area - don't target to prevent showing position without tiles
                    // Also skip marker update to prevent camera interference on iOS
                    Log.i(
                        "AbstractEventMap",
                        "User outside event area (${position.latitude}, ${position.longitude}), keeping camera on event bounds, skipping marker",
                    )
                }
                // Always mark as located to prevent retrying on every position update
                // This preserves "first location" semantics even when user is outside area
                userHasBeenLocated = true

                // Update tracking state
                if (lastKnownPosition == null || lastKnownPosition != position) {
                    onLocationUpdate(position)
                    lastKnownPosition = position
                }
            }
        } else if (lastKnownPosition == null || lastKnownPosition != position) {
            // Regular position updates (not first location in WINDOW mode)
            // Always update marker and notify
            mapLibreAdapter.setUserPosition(position)
            onLocationUpdate(position)
            lastKnownPosition = position
        }
    }

    /**
     * Gets the current unified position from PositionManager
     */
    fun getCurrentPosition(): Position? = positionManager.getCurrentPosition()

    /**
     * Gets the current position source from PositionManager
     */
    fun getCurrentPositionSource(): PositionManager.PositionSource? = positionManager.getCurrentSource()

    // ============================================================
    // LIFECYCLE MANAGEMENT
    // ============================================================

    /**
     * Internal method to start location updates.
     * Marked as internal to allow testing and lifecycle management.
     */
    private fun startLocationUpdatesInternal() {
        if (locationUpdatesActive) {
            Log.d("AbstractEventMap", "Location updates already active")
            return
        }

        locationProvider?.startLocationUpdates { rawPosition ->
            // Update PositionManager with GPS position
            positionManager.updatePosition(PositionManager.PositionSource.GPS, rawPosition)
        }
        locationUpdatesActive = true
        Log.i("AbstractEventMap", "Location updates started for event ${event.id}")
    }

    /**
     * Called when the map/activity is paused (backgrounded).
     * Stops location updates to save battery.
     * IMPORTANT: Does NOT clear cached GPS position (needed for simulation mode).
     */
    fun onPause() {
        if (!locationUpdatesActive) {
            Log.d("AbstractEventMap", "Location updates already stopped")
            return
        }

        Log.i("AbstractEventMap", "onPause: Stopping location updates for event ${event.id}")
        locationProvider?.stopLocationUpdates()
        locationUpdatesActive = false
        // NOTE: Intentionally NOT clearing lastGPSPosition in PositionManager
        // SimulationButton depends on cached GPS position
    }

    /**
     * Called when the map/activity is resumed (foregrounded).
     * Restarts location updates to get fresh position data.
     */
    fun onResume() {
        Log.i("AbstractEventMap", "onResume: Restarting location updates for event ${event.id}")
        startLocationUpdatesInternal()
    }

    // ============================================================
    // ADAPTIVE CAMERA HELPER FUNCTIONS
    // ============================================================

    /**
     * Calculate current wave progression as ratio (0.0 to 1.0)
     * 0.0 = wave just started, 1.0 = wave completed
     */
    @OptIn(kotlin.time.ExperimentalTime::class)
    @Suppress("ReturnCount") // Early returns for guard clauses are appropriate
    // ============================================================

    abstract fun updateWavePolygons(
        wavePolygons: List<Polygon>,
        clearPolygons: Boolean,
    )

    @Composable
    abstract fun Draw(
        autoMapDownload: Boolean,
        modifier: Modifier,
    )
}
