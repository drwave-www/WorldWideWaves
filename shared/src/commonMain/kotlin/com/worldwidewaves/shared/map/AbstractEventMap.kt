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

import com.worldwidewaves.shared.WWWGlobals.Companion.CONST_MAPLIBRE_TARGET_USER_ZOOM
import com.worldwidewaves.shared.WWWGlobals.Companion.CONST_MAPLIBRE_TARGET_WAVE_ZOOM
import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.events.utils.BoundingBox
import com.worldwidewaves.shared.events.utils.PolygonUtils.Quad
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
    abstract val mapLibreAdapter: MapLibreAdapter
    abstract val locationProvider: LocationProvider?

    // Class variables
    private var screenHeight: Double = 800.0
    private var screenWidth: Double = 600.0
    private var userHasBeenLocated = false

    /**
     * Configures map constraints based on the event area.
     * This is meant to be called by platform-specific implementations
     * during initialization.
     */
    private suspend fun configureMapConstraints() =
        mapLibreAdapter.setConstraints(event.area.bbox())

    // Camera position methods - shared logic for all platforms

    /**
     * Moves the camera to view the event bounds
     */
    suspend fun moveToMapBounds(onComplete: () -> Unit = {}) {
        val bounds = event.area.bbox()

        mapLibreAdapter.animateCameraToBounds(bounds, callback = object : MapCameraCallback {
            override fun onFinish() { onComplete() }
            override fun onCancel() { onComplete() }
        })
    }

    /**
     * Adjusts the camera to fit the bounds of the event map with proper aspect ratio
     */
    suspend fun moveToWindowBounds(onComplete: () -> Unit = {}) {
        configureMapConstraints() // Apply constraints first

        val (sw, ne) = event.area.bbox()
        val eventMapWidth = ne.lng - sw.lng
        val eventMapHeight = ne.lat - sw.lat
        val (centerLat, centerLng) = event.area.getCenter()

        // Calculate the aspect ratios of the event map and MapLibre component.
        val eventAspectRatio = eventMapWidth / eventMapHeight

        // Calculate the new southwest and northeast longitudes or latitudes,
        // depending on whether the event map is wider or taller than the MapLibre component.
        val screenComponentRatio = screenWidth / screenHeight
        val (newSwLat, newNeLat, newSwLng, newNeLng) = if (eventAspectRatio > screenComponentRatio) {
            val lngDiff = eventMapHeight * screenComponentRatio / 2
            Quad(sw.lat, ne.lat, centerLng - lngDiff, centerLng + lngDiff)
        } else {
            val latDiff = eventMapWidth / screenComponentRatio / 2
            Quad(centerLat - latDiff, centerLat + latDiff, sw.lng, ne.lng)
        }

        val bounds = BoundingBox(
            Position(newSwLat, newSwLng),
            Position(newNeLat, newNeLng)
        )

        mapLibreAdapter.animateCameraToBounds(bounds, padding = 0, object : MapCameraCallback {
            override fun onFinish() {
                mapLibreAdapter.setMinZoomPreference(mapLibreAdapter.currentZoom.value)
                mapLibreAdapter.setMaxZoomPreference(event.map.maxZoom)
                onComplete()
            }
            override fun onCancel() { onComplete() }
        })
    }

    /**
     * Moves the camera to the center of the event
     */
    suspend fun moveToCenter(onComplete: () -> Unit = {}) {
        val (centerLat, centerLng) = event.area.getCenter()
        mapLibreAdapter.animateCamera(Position(centerLat, centerLng), null, object : MapCameraCallback {
            override fun onFinish() { onComplete() }
            override fun onCancel() { onComplete() }
        })
    }

    // Camera targeting methods - shared logic for all platforms

    /**
     * Moves the camera to the current wave position
     */
    suspend fun targetWave() {
        val currentLocation = locationProvider?.currentLocation?.value ?: return
        val closestWaveLongitude = event.wave.userClosestWaveLongitude() ?: return

        val wavePosition = Position(currentLocation.latitude, closestWaveLongitude)
        mapLibreAdapter.animateCamera(wavePosition, CONST_MAPLIBRE_TARGET_WAVE_ZOOM)
    }

    /**
     * Moves the camera to the current user position
     */
    fun targetUser() {
        val userPosition = locationProvider?.currentLocation?.value ?: return
        mapLibreAdapter.animateCamera(userPosition, CONST_MAPLIBRE_TARGET_USER_ZOOM)
    }

    /**
     * Moves the camera to show both the user and wave positions
     */
    suspend fun targetUserAndWave() {
        val userPosition = locationProvider?.currentLocation?.value ?: return
        val closestWaveLongitude = event.wave.userClosestWaveLongitude() ?: return
        val wavePosition = Position(userPosition.latitude, closestWaveLongitude)

        // Create the bounds containing user and wave positions
        val bounds = BoundingBox.fromPositions(listOf(userPosition, wavePosition)) ?: return

        // Get the area's bounding box
        val areaBbox = event.area.bbox()

        // Calculate padding as percentages of the area's dimensions
        val horizontalPadding = (areaBbox.ne.lng - areaBbox.sw.lng) * 0.2
        val verticalPadding = (areaBbox.ne.lat - areaBbox.sw.lat) * 0.1

        // Create new bounds with padding, constrained by the area's bounding box
        val newBounds = BoundingBox(
            Position(
                maxOf(bounds.southLatitude - minOf(verticalPadding, bounds.southLatitude - areaBbox.sw.lat), areaBbox.sw.lat),
                maxOf(bounds.westLongitude - minOf(horizontalPadding, bounds.westLongitude - areaBbox.sw.lng), areaBbox.sw.lng)
            ),
            Position(
                minOf(bounds.northLatitude + minOf(verticalPadding, areaBbox.ne.lat - bounds.northLatitude), areaBbox.ne.lat),
                minOf(bounds.eastLongitude + minOf(horizontalPadding, areaBbox.ne.lng - bounds.eastLongitude), areaBbox.ne.lng)
            )
        )

        mapLibreAdapter.animateCameraToBounds(newBounds)
    }

    /**
     * Sets up the map with initial configuration
     */
    fun setupMap(
        scope: CoroutineScope,
        screenWidth: Double,
        screenHeight: Double,
        onMapLoaded: () -> Unit = {},
        onMapClick: ((Double, Double) -> Unit)? = null
    ) {
        this.screenWidth = screenWidth
        this.screenHeight = screenHeight

        // Set the click listener
        mapLibreAdapter.setOnMapClickListener(onMapClick)

        // Set the max zoom level from the event configuration
        mapLibreAdapter.setMaxZoomPreference(event.map.maxZoom)

        // Configure initial camera position
        scope.launch {
            when (mapConfig.initialCameraPosition) {
                MapCameraPosition.DEFAULT_CENTER -> moveToCenter(onMapLoaded)
                MapCameraPosition.BOUNDS -> moveToMapBounds(onMapLoaded)
                MapCameraPosition.WINDOW -> moveToWindowBounds(onMapLoaded)
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

}

