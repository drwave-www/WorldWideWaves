package com.worldwidewaves.shared.map

/*
 * Copyright 2024 DrWave
 *
 * WorldWideWaves is an ephemeral mobile app designed to orchestrate human waves through cities and
 * countries, culminating in a global wave. The project aims to transcend physical and cultural
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

import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.events.utils.BoundingBox
import com.worldwidewaves.shared.events.utils.Position
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

// ----------------------------------------------------------------------------

interface MapCameraCallback {
    fun onFinish()
    fun onCancel()
}

data class EventMapConfig(
    val initialCameraPosition: MapCameraPosition = MapCameraPosition.BOUNDS
)

enum class MapCameraPosition {
    WINDOW,   // Fit map to window with proper aspect ratio
    BOUNDS,   // Show entire event bounds
    DEFAULT_CENTER // Center on the event's center point
}

// ----------------------------------------------------------------------------

/**
 * Abstract base class for EventMap functionality that's shared across platforms
 */
abstract class AbstractEventMap(
    protected val event: IWWWEvent,
    protected val mapConfig: EventMapConfig = EventMapConfig(),
    private val onLocationUpdate: (Position) -> Unit
) {
    // Properties that will be implemented by platform-specific subclasses
    abstract val platformMap: PlatformMap
    abstract val locationProvider: LocationProvider?

    // Class variables
    private var mapMinZoomLevel: Double? = null
    private var userHasBeenLocated = false

    // Camera position methods - shared logic for all platforms

    /**
     * Moves the camera to view the event bounds
     */
    suspend fun moveToMapBounds(onComplete: () -> Unit = {}) {
        val bounds = event.area.bbox()

        platformMap.animateCameraToBounds(bounds, callback = object : MapCameraCallback {
            override fun onFinish() {
                mapMinZoomLevel = platformMap.currentZoom.value
                onComplete()
            }
            override fun onCancel() {}
        })
    }

    /**
     * Adjusts the camera to fit the bounds of the event map with proper aspect ratio
     */
    suspend fun moveToWindowBounds(onComplete: () -> Unit = {}) {
        val mapBounds = event.area.bbox()
        configureMapConstraints() // Apply constraints first

        platformMap.animateCameraToBounds(mapBounds, padding = 0, object : MapCameraCallback {
            override fun onFinish() {
                mapMinZoomLevel = platformMap.currentZoom.value
                onComplete()
            }
            override fun onCancel() {}
        })
    }

    /**
     * Moves the camera to the center of the event
     */
    suspend fun moveToCenter(onComplete: () -> Unit = {}) {
        val (centerLat, centerLng) = event.area.getCenter()
        platformMap.animateCamera(Position(centerLat, centerLng), null, object : MapCameraCallback {
            override fun onFinish() {
                onComplete()
            }

            override fun onCancel() {}
        })
    }

    /**
     * Moves the camera to the current wave position
     */
    suspend fun targetWave() {
        val currentLocation = locationProvider?.currentLocation?.value ?: return
        val closestWaveLongitude = event.wave.userClosestWaveLongitude() ?: return

        val wavePosition = Position(currentLocation.latitude, closestWaveLongitude)
        platformMap.animateCamera(wavePosition, CONST_MAPLIBRE_TARGET_WAVE_ZOOM)
    }

    /**
     * Moves the camera to the current user position
     */
    fun targetUser() {
        val userPosition = locationProvider?.currentLocation?.value ?: return
        platformMap.animateCamera(userPosition, CONST_MAPLIBRE_TARGET_USER_ZOOM)
    }

    /**
     * Moves the camera to show both the user and wave positions
     */
    suspend fun targetUserAndWave() {
        val userPosition = locationProvider?.currentLocation?.value ?: return
        val closestWaveLongitude = event.wave.userClosestWaveLongitude() ?: return
        val wavePosition = Position(userPosition.latitude, closestWaveLongitude)

        // Create bounds with just the two points
        val bounds = BoundingBox(
            Position(
                minOf(userPosition.latitude, wavePosition.latitude),
                minOf(userPosition.longitude, wavePosition.longitude)
            ),
            Position(
                maxOf(userPosition.latitude, wavePosition.latitude),
                maxOf(userPosition.longitude, wavePosition.longitude)
            )
        )

        platformMap.animateCameraToBounds(bounds, padding = 100)
    }

    /**
     * Configures map constraints based on the event area.
     * This is meant to be called by platform-specific implementations
     * during initialization.
     */
    private suspend fun configureMapConstraints() =
        platformMap.setConstraints(event.area.bbox())

    /**
     * Sets up the map with initial configuration
     */
    fun setupMap(
        scope: CoroutineScope,
        onMapLoaded: () -> Unit = {},
        onMapClick: ((Double, Double) -> Unit)? = null
    ) {
        platformMap.setOnMapClickListener(onMapClick)

        // Set the max zoom level from the event configuration
        platformMap.setMaxZoomPreference(event.map.maxZoom)

        // Configure initial camera position
        scope.launch {
            when (mapConfig.initialCameraPosition) {
                MapCameraPosition.DEFAULT_CENTER -> moveToCenter(onMapLoaded)
                MapCameraPosition.BOUNDS -> moveToMapBounds(onMapLoaded)
                MapCameraPosition.WINDOW -> {
                    configureMapConstraints()
                    moveToWindowBounds(onMapLoaded)
                }
            }
        }

        // Start location updates
        locationProvider?.startLocationUpdates { mapPosition ->

            // Target user, the first time only
            if (!userHasBeenLocated && mapConfig.initialCameraPosition == MapCameraPosition.WINDOW) {
                targetUser()
                userHasBeenLocated = true
            }

            // Allow the wave to know the current location of the user for computations
            event.wave.setPositionRequester { mapPosition }

            // Notify caller
            onLocationUpdate(mapPosition)
        }
    }

    companion object {
        const val CONST_MAPLIBRE_TARGET_USER_ZOOM = 18.0
        const val CONST_MAPLIBRE_TARGET_WAVE_ZOOM = 16.0
    }
}

