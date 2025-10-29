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
import com.worldwidewaves.shared.events.utils.BoundingBox
import com.worldwidewaves.shared.events.utils.IClock
import com.worldwidewaves.shared.events.utils.Polygon
import com.worldwidewaves.shared.events.utils.Position
import com.worldwidewaves.shared.position.PositionManager
import com.worldwidewaves.shared.utils.Log
import com.worldwidewaves.shared.utils.PerformanceTracer
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
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
        // Initialize constraint manager with BOUNDS mode (zero padding for tight fit)
        constraintManager = MapBoundsEnforcer(event.area.bbox(), mapLibreAdapter, isWindowMode = false) { suppressCorrections }

        val bounds = event.area.bbox()
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
     * Moves the camera to the current wave longitude, keeping current latitude
     */
    suspend fun targetWave() {
        // Use unified PositionManager (respects SIMULATION > GPS priority)
        val currentLocation = positionManager.getCurrentPosition() ?: return
        val closestWaveLongitude = event.wave.userClosestWaveLongitude() ?: return

        val wavePosition = Position(currentLocation.latitude, closestWaveLongitude)
        runCameraAnimation { cb ->
            mapLibreAdapter.animateCamera(wavePosition, MapDisplay.TARGET_WAVE_ZOOM, cb)
        }
    }

    /**
     * Moves the camera to the current user position
     */
    suspend fun targetUser() {
        val userPosition = positionManager.getCurrentPosition() ?: return
        runCameraAnimation { cb ->
            mapLibreAdapter.animateCamera(userPosition, MapDisplay.TARGET_USER_ZOOM, cb)
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
     * Moves the camera to show both the user and wave positions with adaptive zoom.
     * Uses intelligent zoom strategy based on:
     * - Wave progression (early = tight focus, late = full coverage)
     * - User-wave distance (expands if needed)
     * - Event edge proximity (ensures boundary visibility)
     */
    @Suppress("ComplexCondition") // Necessary validation: null + NaN checks prevent IllegalArgumentException
    suspend fun targetUserAndWave() {
        val userPosition = positionManager.getCurrentPosition()
        val closestWaveLongitude = event.wave.userClosestWaveLongitude()

        // Validate that we have valid data (not null and not NaN)
        if (userPosition == null ||
            closestWaveLongitude == null ||
            closestWaveLongitude.isNaN() ||
            userPosition.latitude.isNaN() ||
            userPosition.longitude.isNaN()
        ) {
            Log.w(
                "AbstractEventMap",
                "WARNING: targetUserAndWave: missing or invalid data (userPos=$userPosition, waveLng=$closestWaveLongitude)",
            )
            return
        }

        val wavePosition = Position(userPosition.latitude, closestWaveLongitude)

        // Create the bounds containing user and wave positions
        val bounds = BoundingBox.fromCorners(listOf(userPosition, wavePosition))
        if (bounds == null) {
            Log.e("AbstractEventMap", "Failed to create bounds from user+wave positions")
            return
        }

        // Get the area's bounding box
        val areaBbox = event.area.bbox()

        // ============================================================
        // ADAPTIVE LOGIC: Calculate context-aware maximum span
        // ============================================================

        val eventLatSpan = areaBbox.ne.lat - areaBbox.sw.lat
        val eventLngSpan = areaBbox.ne.lng - areaBbox.sw.lng

        // 1. Calculate wave progression (0.0 = start, 1.0 = end)
        val waveProgression = calculateWaveProgression()

        // 2. Calculate user-wave distance as percentage of event size
        val userWaveLatDistance = kotlin.math.abs(userPosition.latitude - wavePosition.latitude)
        val userWaveLngDistance = kotlin.math.abs(userPosition.longitude - wavePosition.longitude)
        val latDistanceRatio = userWaveLatDistance / eventLatSpan
        val lngDistanceRatio = userWaveLngDistance / eventLngSpan

        // 3. Check if user or wave is near event edges
        val userNearEdge = isNearEdge(userPosition, areaBbox, MapDisplay.ADAPTIVE_CAMERA_EDGE_THRESHOLD)
        val waveNearEdge = isNearEdge(wavePosition, areaBbox, MapDisplay.ADAPTIVE_CAMERA_EDGE_THRESHOLD)

        // 4. Determine adaptive maximum span based on progression phase
        val baseMaxSpanRatio =
            when {
                // Phase 3: Late wave (70-100%) - prioritize visibility
                waveProgression >= MapDisplay.ADAPTIVE_CAMERA_PHASE_3_START -> MapDisplay.ADAPTIVE_CAMERA_PHASE_3_MAX_SPAN

                // Phase 2: Mid wave (40-70%) - balanced view
                waveProgression >= MapDisplay.ADAPTIVE_CAMERA_PHASE_2_START -> MapDisplay.ADAPTIVE_CAMERA_PHASE_2_MAX_SPAN

                // Phase 1: Early wave (0-40%) - tight focus
                else -> MapDisplay.ADAPTIVE_CAMERA_PHASE_1_MAX_SPAN
            }

        // 5. Apply adaptive overrides
        val adaptiveMaxLatRatio =
            when {
                // Override: User-wave distance requires more space
                latDistanceRatio > baseMaxSpanRatio ->
                    kotlin.math.min(latDistanceRatio * MapDisplay.ADAPTIVE_CAMERA_DISTANCE_PADDING, 1.0)

                // Override: Near edge - need more context
                userNearEdge || waveNearEdge ->
                    kotlin.math.max(baseMaxSpanRatio, MapDisplay.ADAPTIVE_CAMERA_EDGE_MIN_SPAN)

                // Normal: Use phase-based maximum
                else -> baseMaxSpanRatio
            }

        val adaptiveMaxLngRatio =
            when {
                lngDistanceRatio > baseMaxSpanRatio ->
                    kotlin.math.min(lngDistanceRatio * MapDisplay.ADAPTIVE_CAMERA_DISTANCE_PADDING, 1.0)
                userNearEdge || waveNearEdge ->
                    kotlin.math.max(baseMaxSpanRatio, MapDisplay.ADAPTIVE_CAMERA_EDGE_MIN_SPAN)
                else -> baseMaxSpanRatio
            }

        val maxLatSpan = eventLatSpan * adaptiveMaxLatRatio
        val maxLngSpan = eventLngSpan * adaptiveMaxLngRatio

        // ============================================================
        // EXISTING PADDING AND BOUNDS LOGIC
        // ============================================================

        // Calculate padding as percentages of the area's dimensions
        val horizontalPadding = eventLngSpan * 0.2
        val verticalPadding = eventLatSpan * 0.1

        // Create new bounds with padding, constrained by the area's bounding box
        val paddedBounds =
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

        val currentLatSpan = paddedBounds!!.ne.lat - paddedBounds.sw.lat
        val currentLngSpan = paddedBounds.ne.lng - paddedBounds.sw.lng

        // Apply adaptive maximum span
        val finalBounds =
            if (currentLatSpan > maxLatSpan || currentLngSpan > maxLngSpan) {
                // Bounds exceed adaptive maximum - center on user+wave midpoint with max span
                val midLat = (paddedBounds.sw.lat + paddedBounds.ne.lat) / 2.0
                val midLng = (paddedBounds.sw.lng + paddedBounds.ne.lng) / 2.0

                val useLat = minOf(currentLatSpan, maxLatSpan) / 2.0
                val useLng = minOf(currentLngSpan, maxLngSpan) / 2.0

                BoundingBox.fromCorners(
                    Position(
                        maxOf(midLat - useLat, areaBbox.sw.lat),
                        maxOf(midLng - useLng, areaBbox.sw.lng),
                    ),
                    Position(
                        minOf(midLat + useLat, areaBbox.ne.lat),
                        minOf(midLng + useLng, areaBbox.ne.lng),
                    ),
                )
            } else {
                paddedBounds
            }

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

            // Configure initial camera position
            scope.launch {
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
            locationProvider?.startLocationUpdates { rawPosition ->
                // Update PositionManager with GPS position
                positionManager.updatePosition(PositionManager.PositionSource.GPS, rawPosition)
            }

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
        if (mapConfig.autoTargetUserOnFirstLocation &&
            !userHasBeenLocated &&
            !userInteracted &&
            mapConfig.initialCameraPosition == MapCameraPosition.WINDOW
        ) {
            scope.launch {
                val isUserInEventArea = event.area.isPositionWithin(position)
                if (isUserInEventArea) {
                    targetUser()
                    Log.i("AbstractEventMap", "User in event area, auto-targeting user position")
                } else {
                    Log.i(
                        "AbstractEventMap",
                        "User outside event area (${position.latitude}, ${position.longitude}), keeping camera on event bounds",
                    )
                }
                userHasBeenLocated = true
            }
        }

        if (lastKnownPosition == null || lastKnownPosition != position) {
            // Position is now managed by PositionManager through unified system

            // Update visual location marker on map (important for iOS simulation)
            mapLibreAdapter.setUserPosition(position)

            // Notify caller
            onLocationUpdate(position)

            // Save current known position on map
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
    // ADAPTIVE CAMERA HELPER FUNCTIONS
    // ============================================================

    /**
     * Calculate current wave progression as ratio (0.0 to 1.0)
     * 0.0 = wave just started, 1.0 = wave completed
     */
    @OptIn(kotlin.time.ExperimentalTime::class)
    @Suppress("ReturnCount") // Early returns for guard clauses are appropriate
    private suspend fun calculateWaveProgression(): Double {
        val clock: IClock by inject()
        val now = clock.now()
        val waveStart = event.getWaveStartDateTime()
        val waveEnd = event.getEndDateTime()

        if (now < waveStart) return 0.0
        if (now >= waveEnd) return 1.0

        val elapsed = (now - waveStart).inWholeMilliseconds.toDouble()
        val total = (waveEnd - waveStart).inWholeMilliseconds.toDouble()

        return (elapsed / total).coerceIn(0.0, 1.0)
    }

    /**
     * Check if position is near event area edges
     * @param position Position to check
     * @param bbox Event area bounding box
     * @param threshold Distance from edge as ratio (0.2 = within 20% of boundary)
     * @return true if position is within threshold distance from any edge
     */
    private fun isNearEdge(
        position: Position,
        bbox: BoundingBox,
        threshold: Double,
    ): Boolean {
        val latMargin = (bbox.ne.lat - bbox.sw.lat) * threshold
        val lngMargin = (bbox.ne.lng - bbox.sw.lng) * threshold

        val nearSouth = position.latitude < (bbox.sw.lat + latMargin)
        val nearNorth = position.latitude > (bbox.ne.lat - latMargin)
        val nearWest = position.longitude < (bbox.sw.lng + lngMargin)
        val nearEast = position.longitude > (bbox.ne.lng - lngMargin)

        return nearSouth || nearNorth || nearWest || nearEast
    }

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
