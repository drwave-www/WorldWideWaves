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

import com.worldwidewaves.shared.WWWGlobals.MapDisplay
import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.events.utils.BoundingBox
import com.worldwidewaves.shared.events.utils.Polygon
import com.worldwidewaves.shared.events.utils.PolygonUtils.Quad
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
    // Properties that must be implemented by platform-specific subclasses
    abstract val mapLibreAdapter: MapLibreAdapter<T> // MapLibre is native map library
    abstract val locationProvider: WWWLocationProvider? // LocationProvider is native location provider

    // Dependencies
    private val positionManager: PositionManager by inject()

    // Class variables
    private var constraintManager: MapConstraintManager? = null // Map constraint manager
    private var screenHeight: Double = 800.0
    private var screenWidth: Double = 600.0
    private var userHasBeenLocated = false
    private var lastKnownPosition: Position? = null
    private var userInteracted = false

    /** When true the MapConstraintManager is not allowed to move the camera. */
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
        val bounds = event.area.bbox()
        runCameraAnimation { cb ->
            mapLibreAdapter.animateCameraToBounds(
                bounds,
                callback =
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

    /**
     * Adjusts the camera to fit the bounds of the event map with proper aspect ratio
     */
    suspend fun moveToWindowBounds(onComplete: () -> Unit = {}) {
        // Prepare constraint manager – actual constraints will be applied
        // after the initial animation finishes (see onFinish below).
        constraintManager = MapConstraintManager(event.area.bbox(), mapLibreAdapter) { suppressCorrections }

        val (sw, ne) = event.area.bbox()
        val eventMapWidth = ne.lng - sw.lng
        val eventMapHeight = ne.lat - sw.lat
        val (centerLat, centerLng) = event.area.getCenter()

        // Calculate the aspect ratios of the event map and MapLibre component.
        val eventAspectRatio = eventMapWidth / eventMapHeight

        // Calculate the new southwest and northeast longitudes or latitudes,
        // depending on whether the event map is wider or taller than the MapLibre component.
        val screenComponentRatio = screenWidth / screenHeight
        val (newSwLat, newNeLat, newSwLng, newNeLng) =
            if (eventAspectRatio > screenComponentRatio) {
                val lngDiff = eventMapHeight * screenComponentRatio / 2
                Quad(sw.lat, ne.lat, centerLng - lngDiff, centerLng + lngDiff)
            } else {
                val latDiff = eventMapWidth / screenComponentRatio / 2
                Quad(centerLat - latDiff, centerLat + latDiff, sw.lng, ne.lng)
            }

        val bounds =
            BoundingBox.fromCorners(
                Position(newSwLat, newSwLng),
                Position(newNeLat, newNeLng),
            )

        runCameraAnimation { cb ->
            mapLibreAdapter.animateCameraToBounds(
                bounds,
                padding = 0,
                object : MapCameraCallback {
                    override fun onFinish() {
                        // Now that the camera is fitted, we can apply constraints safely
                        constraintManager?.applyConstraints()
                        mapLibreAdapter.setMinZoomPreference(mapLibreAdapter.currentZoom.value)
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
        val currentLocation = locationProvider?.currentLocation?.value ?: return
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
        val userPosition = locationProvider?.currentLocation?.value ?: return
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
     * Moves the camera to show both the user and wave positions with good padding
     */
    suspend fun targetUserAndWave() {
        val userPosition = locationProvider?.currentLocation?.value ?: return
        val closestWaveLongitude = event.wave.userClosestWaveLongitude() ?: return
        val wavePosition = Position(userPosition.latitude, closestWaveLongitude)

        // Create the bounds containing user and wave positions
        val bounds = BoundingBox.fromCorners(listOf(userPosition, wavePosition)) ?: return

        // Get the area's bounding box
        val areaBbox = event.area.bbox()

        // Calculate padding as percentages of the area's dimensions
        val horizontalPadding = (areaBbox.ne.lng - areaBbox.sw.lng) * 0.2
        val verticalPadding = (areaBbox.ne.lat - areaBbox.sw.lat) * 0.1

        // Create new bounds with padding, constrained by the area's bounding box
        val newBounds =
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
            mapLibreAdapter.animateCameraToBounds(newBounds)
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
            mapLibreAdapter.addOnCameraIdleListener { constraintManager?.constrainCamera() }

            // Configure initial camera position
            scope.launch {
                when (mapConfig.initialCameraPosition) {
                    MapCameraPosition.DEFAULT_CENTER -> moveToCenter(onMapLoaded)
                    MapCameraPosition.BOUNDS -> moveToMapBounds(onMapLoaded)
                    MapCameraPosition.WINDOW -> moveToWindowBounds(onMapLoaded)
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
        if (mapConfig.autoTargetUserOnFirstLocation &&
            !userHasBeenLocated &&
            !userInteracted &&
            mapConfig.initialCameraPosition == MapCameraPosition.WINDOW
        ) {
            scope.launch {
                targetUser()
            }
            userHasBeenLocated = true
        }

        if (lastKnownPosition == null || lastKnownPosition != position) {
            // Position is now managed by PositionManager through unified system

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
        clearPolygons: Boolean
    )

}
