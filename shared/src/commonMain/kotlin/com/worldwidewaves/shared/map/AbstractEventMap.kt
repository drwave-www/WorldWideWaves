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
import com.worldwidewaves.shared.events.utils.Polygon
import com.worldwidewaves.shared.events.utils.Position
import com.worldwidewaves.shared.position.PositionManager
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
)

enum class MapCameraPosition {
    WINDOW, // Fit map to window with proper aspect ratio
    BOUNDS, // Show entire event bounds
    DEFAULT_CENTER, // Center on the event's center point
}

// ----------------------------------------------------------------------------

/**
 * Abstract base class for EventMap functionality that's shared across platforms
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
     * constraint corrections so that the ConstraintManager does not
     * fight against the animation.
     */
    private suspend inline fun runCameraAnimation(crossinline block: (MapCameraCallback) -> Unit) {
        suppressCorrections = true

        // Save current constraint manager (may be null) and relax bounds to full event area
        val originalConstraintManager = constraintManager
        if (originalConstraintManager != null) {
            // Allow the camera to move freely inside the full event area during animation
            mapLibreAdapter.setBoundsForCameraTarget(event.area.bbox())
        }

        block(
            object : MapCameraCallback {
                override fun onFinish() {
                    suppressCorrections = false
                    // Re-apply constraints if they were configured
                    originalConstraintManager?.applyConstraints()
                }

                override fun onCancel() {
                    suppressCorrections = false
                    // Ensure constraints are reapplied even if animation is cancelled
                    originalConstraintManager?.applyConstraints()
                }
            },
        )
    }

    /**
     * Moves the camera to view the event bounds
     */
    suspend fun moveToMapBounds(onComplete: () -> Unit = {}) {
        com.worldwidewaves.shared.utils.Log.i(
            "AbstractEventMap",
            "📐 moveToMapBounds: Starting for event ${event.id}, bounds=${event.area.bbox()}",
        )
        // Initialize constraint manager (same as moveToWindowBounds)
        constraintManager = MapBoundsEnforcer(event.area.bbox(), mapLibreAdapter) { suppressCorrections }

        val bounds = event.area.bbox()
        com.worldwidewaves.shared.utils.Log.d(
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
                            com.worldwidewaves.shared.utils.Log.i(
                                "AbstractEventMap",
                                "✅ moveToMapBounds animation completed for event ${event.id}",
                            )
                            // Apply constraints FIRST - this calculates and sets the correct min zoom
                            // based on bounds that fit perfectly without padding
                            constraintManager?.applyConstraints()

                            // Get the calculated min zoom from constraints
                            val calculatedMinZoom = mapLibreAdapter.getMinZoomLevel()
                            com.worldwidewaves.shared.utils.Log.d(
                                "AbstractEventMap",
                                "BOUNDS mode: Min zoom from constraints: $calculatedMinZoom",
                            )

                            mapLibreAdapter.setMaxZoomPreference(event.map.maxZoom)
                            cb.onFinish()
                            onComplete()
                        }

                        override fun onCancel() {
                            com.worldwidewaves.shared.utils.Log.w(
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
     * Adjusts the camera to fit the bounds of the event map with proper aspect ratio
     */
    suspend fun moveToWindowBounds(onComplete: () -> Unit = {}) {
        // Capture event bbox before animation (needed for callback which is not suspend)
        val eventBbox = event.area.bbox()

        // Prepare bounds enforcer – actual constraints will be applied
        // after the initial animation finishes (see onFinish below).
        constraintManager = MapBoundsEnforcer(eventBbox, mapLibreAdapter) { suppressCorrections }

        val (sw, ne) = eventBbox
        val eventMapWidth = ne.lng - sw.lng
        val eventMapHeight = ne.lat - sw.lat
        val (centerLat, centerLng) = event.area.getCenter()

        // Calculate the aspect ratios of the event map and MapLibre component.
        val eventAspectRatio = eventMapWidth / eventMapHeight

        // Calculate the new southwest and northeast longitudes or latitudes,
        // depending on whether the event map is wider or taller than the MapLibre component.
        val screenComponentRatio = screenWidth / screenHeight
        val newSwLat: Double
        val newNeLat: Double
        val newSwLng: Double
        val newNeLng: Double

        // When event is WIDER than screen (eventAspectRatio > screenComponentRatio):
        // - Keep event width (longitude span) unchanged
        // - Expand height (latitude span) to match screen aspect ratio
        // When event is TALLER than screen (eventAspectRatio < screenComponentRatio):
        // - Keep event height (latitude span) unchanged
        // - Expand width (longitude span) to match screen aspect ratio
        if (eventAspectRatio > screenComponentRatio) {
            // Event is wider - expand latitude to fill screen height
            val latDiff = eventMapWidth / screenComponentRatio / 2
            newSwLat = centerLat - latDiff
            newNeLat = centerLat + latDiff
            newSwLng = sw.lng
            newNeLng = ne.lng
        } else {
            // Event is taller - expand longitude to fill screen width
            val lngDiff = eventMapHeight * screenComponentRatio / 2
            newSwLat = sw.lat
            newNeLat = ne.lat
            newSwLng = centerLng - lngDiff
            newNeLng = centerLng + lngDiff
        }

        val expandedBounds =
            BoundingBox.fromCorners(
                Position(newSwLat, newSwLng),
                Position(newNeLat, newNeLng),
            )

        runCameraAnimation { cb ->
            mapLibreAdapter.animateCameraToBounds(
                expandedBounds,
                padding = 0,
                object : MapCameraCallback {
                    override fun onFinish() {
                        // Apply constraints - this will set min zoom based on expanded WINDOW bounds
                        constraintManager?.applyConstraints()

                        // Get the calculated min zoom
                        val calculatedMinZoom = mapLibreAdapter.getMinZoomLevel()
                        com.worldwidewaves.shared.utils.Log.d(
                            "AbstractEventMap",
                            "WINDOW mode: Min zoom from constraints: $calculatedMinZoom",
                        )

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
        // FIXED: Use unified PositionManager (respects SIMULATION > GPS priority)
        val currentLocation = positionManager.getCurrentPosition() ?: return
        val closestWaveLongitude = event.wave.userClosestWaveLongitude() ?: return

        val wavePosition = Position(currentLocation.latitude, closestWaveLongitude)
        runCameraAnimation { _ ->
            mapLibreAdapter.animateCamera(wavePosition, MapDisplay.TARGET_WAVE_ZOOM)
        }
    }

    /**
     * Moves the camera to the current user position
     */
    suspend fun targetUser() {
        // FIXED: Use unified PositionManager (respects SIMULATION > GPS priority)
        val userPosition = positionManager.getCurrentPosition() ?: return
        runCameraAnimation { _ ->
            mapLibreAdapter.animateCamera(userPosition, MapDisplay.TARGET_USER_ZOOM)
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
     * Moves the camera to show both the user and wave positions with good padding.
     * Respects constraint bounds to prevent fighting with MapBoundsEnforcer.
     */
    suspend fun targetUserAndWave() {
        // FIXED: Use unified PositionManager instead of locationProvider
        // PositionManager respects SIMULATION > GPS priority
        val userPosition = positionManager.getCurrentPosition()
        val closestWaveLongitude = event.wave.userClosestWaveLongitude()

        com.worldwidewaves.shared.utils.Log.i(
            "AbstractEventMap",
            "🎯 targetUserAndWave called for event: ${event.id}, userPos=$userPosition, waveLng=$closestWaveLongitude",
        )

        if (userPosition == null || closestWaveLongitude == null) {
            com.worldwidewaves.shared.utils.Log.w(
                "AbstractEventMap",
                "⚠️ targetUserAndWave: missing data (userPos=$userPosition, waveLng=$closestWaveLongitude)",
            )
            return
        }

        val wavePosition = Position(userPosition.latitude, closestWaveLongitude)

        // Create the bounds containing user and wave positions
        val bounds = BoundingBox.fromCorners(listOf(userPosition, wavePosition))
        if (bounds == null) {
            com.worldwidewaves.shared.utils.Log
                .e("AbstractEventMap", "Failed to create bounds from user+wave positions")
            return
        }

        com.worldwidewaves.shared.utils.Log.d(
            "AbstractEventMap",
            "User+Wave bounds: SW(${bounds.sw.lat}, ${bounds.sw.lng}) NE(${bounds.ne.lat}, ${bounds.ne.lng})",
        )

        // Get the area's bounding box (or constraint bounds if more restrictive)
        val areaBbox = event.area.bbox()
        val constraintBbox = constraintManager?.calculateConstraintBounds() ?: areaBbox
        com.worldwidewaves.shared.utils.Log.d(
            "AbstractEventMap",
            "Area bbox: SW(${areaBbox.sw.lat}, ${areaBbox.sw.lng}) NE(${areaBbox.ne.lat}, ${areaBbox.ne.lng})",
        )
        com.worldwidewaves.shared.utils.Log.d(
            "AbstractEventMap",
            "Constraint bbox: SW(${constraintBbox.sw.lat}, ${constraintBbox.sw.lng}) " +
                "NE(${constraintBbox.ne.lat}, ${constraintBbox.ne.lng})",
        )

        // Calculate padding as percentages of the area's dimensions
        val horizontalPadding = (areaBbox.ne.lng - areaBbox.sw.lng) * 0.2
        val verticalPadding = (areaBbox.ne.lat - areaBbox.sw.lat) * 0.1

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

        com.worldwidewaves.shared.utils.Log.d(
            "AbstractEventMap",
            "Padded bounds: SW(${paddedBounds!!.sw.lat}, ${paddedBounds.sw.lng}) " +
                "NE(${paddedBounds.ne.lat}, ${paddedBounds.ne.lng})",
        )

        // Limit bounds to a reasonable size to keep camera focused on user+wave
        // Without this, bounds can become too large when user is far from event area
        // Max size: 50% of event area in each dimension (keeps focus tight)
        val maxLatSpan = (areaBbox.ne.lat - areaBbox.sw.lat) * 0.5
        val maxLngSpan = (areaBbox.ne.lng - areaBbox.sw.lng) * 0.5

        val currentLatSpan = paddedBounds.ne.lat - paddedBounds.sw.lat
        val currentLngSpan = paddedBounds.ne.lng - paddedBounds.sw.lng

        val finalBounds =
            if (currentLatSpan > maxLatSpan || currentLngSpan > maxLngSpan) {
                // Bounds too large - center on user+wave midpoint with max span
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

        com.worldwidewaves.shared.utils.Log.i(
            "AbstractEventMap",
            "🎬 targetUserAndWave: Final bounds " +
                "SW(${finalBounds.sw.lat}, ${finalBounds.sw.lng}) NE(${finalBounds.ne.lat}, ${finalBounds.ne.lng})",
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
        // Pass the map to the adapter
        mapLibreAdapter.setMap(map)

        // Set screen dimensions
        this.screenWidth = mapLibreAdapter.getWidth()
        this.screenHeight = mapLibreAdapter.getHeight()

        mapLibreAdapter.setStyle(stylePath) {
            // Set Attribution margins to 0
            mapLibreAdapter.setAttributionMargins(0, 0, 0, 0)

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

            // Apply bounds constraints if required
            mapLibreAdapter.addOnCameraIdleListener {
                constraintManager?.constrainCamera()

                // WINDOW mode: Recalculate bounds when dimensions change significantly
                if (windowBoundsNeedRecalculation && mapConfig.initialCameraPosition == MapCameraPosition.WINDOW) {
                    val currentWidth = mapLibreAdapter.getWidth()
                    val currentHeight = mapLibreAdapter.getHeight()

                    // Check if dimensions changed significantly
                    val widthChange = kotlin.math.abs(currentWidth - screenWidth) / screenWidth
                    val heightChange = kotlin.math.abs(currentHeight - screenHeight) / screenHeight

                    if (widthChange > DIMENSION_CHANGE_THRESHOLD || heightChange > DIMENSION_CHANGE_THRESHOLD) {
                        com.worldwidewaves.shared.utils.Log.i(
                            "AbstractEventMap",
                            "📐 Dimensions changed significantly (${screenWidth}x$screenHeight → ${currentWidth}x$currentHeight), " +
                                "recalculating WINDOW bounds",
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
                com.worldwidewaves.shared.utils.Log.i(
                    "AbstractEventMap",
                    "🎥 Setting initial camera position: ${mapConfig.initialCameraPosition} for event: ${event.id}",
                )
                when (mapConfig.initialCameraPosition) {
                    MapCameraPosition.DEFAULT_CENTER -> {
                        com.worldwidewaves.shared.utils.Log
                            .d("AbstractEventMap", "Calling moveToCenter")
                        moveToCenter(onMapLoaded)
                    }
                    MapCameraPosition.BOUNDS -> {
                        com.worldwidewaves.shared.utils.Log.d(
                            "AbstractEventMap",
                            "Calling moveToMapBounds for event: ${event.id}",
                        )
                        moveToMapBounds(onMapLoaded)
                    }
                    MapCameraPosition.WINDOW -> {
                        com.worldwidewaves.shared.utils.Log.d(
                            "AbstractEventMap",
                            "Calling moveToWindowBounds (initial, dimensions may change)",
                        )
                        // Mark that we should watch for dimension changes and recalculate
                        windowBoundsNeedRecalculation = true
                        moveToWindowBounds(onMapLoaded)
                    }
                }
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
        val shouldAutoTarget =
            mapConfig.autoTargetUserOnFirstLocation &&
                !userHasBeenLocated &&
                !userInteracted &&
                mapConfig.initialCameraPosition == MapCameraPosition.WINDOW

        if (shouldAutoTarget) {
            scope.launch {
                targetUser()
            }
            userHasBeenLocated = true
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
