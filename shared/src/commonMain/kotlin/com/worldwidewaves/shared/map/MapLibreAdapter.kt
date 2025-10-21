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

import com.worldwidewaves.shared.events.utils.BoundingBox
import com.worldwidewaves.shared.events.utils.Position
import kotlinx.coroutines.flow.StateFlow

/**
 * Map interface that platform-specific implementations must implement
 */
interface MapLibreAdapter<T> {
    fun setMap(map: T)

    fun setStyle(
        stylePath: String,
        callback: () -> Unit?,
    )

    val currentPosition: StateFlow<Position?>
    val currentZoom: StateFlow<Double>

    fun getWidth(): Double

    fun getHeight(): Double

    fun getCameraPosition(): Position?

    fun getVisibleRegion(): BoundingBox

    fun moveCamera(bounds: BoundingBox)

    fun animateCamera(
        position: Position,
        zoom: Double? = null,
        callback: MapCameraCallback? = null,
    )

    fun animateCameraToBounds(
        bounds: BoundingBox,
        padding: Int = 0,
        callback: MapCameraCallback? = null,
    )

    fun setBoundsForCameraTarget(
        constraintBounds: BoundingBox,
        applyZoomSafetyMargin: Boolean = false,
        originalEventBounds: BoundingBox? = null,
    )

    fun getMinZoomLevel(): Double

    fun setMinZoomPreference(minZoom: Double)

    fun setMaxZoomPreference(maxZoom: Double)

    fun setAttributionMargins(
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
    )

    fun addWavePolygons(
        polygons: List<Any>,
        clearExisting: Boolean = false,
    )

    fun setOnMapClickListener(listener: ((Double, Double) -> Unit)?)

    fun addOnCameraIdleListener(callback: () -> Unit)

    fun drawOverridenBbox(bbox: BoundingBox)

    fun onMapSet(callback: (MapLibreAdapter<*>) -> Unit)

    /**
     * Enable or disable the user location marker on the map.
     * On Android, this activates the location component.
     * On iOS, this enables showsUserLocation on the map view.
     */
    fun enableLocationComponent(enabled: Boolean)

    /**
     * Update the user position marker on the map.
     * Used during simulation to show simulated position (not device GPS).
     * On Android, this is handled automatically by LocationEngineProxy.
     * On iOS, this manually updates the location marker.
     */
    fun setUserPosition(position: Position)

    /**
     * Enable or disable user gestures on the map (pan, zoom, rotate, tilt).
     * When disabled, the map becomes non-interactive - useful for preview/display maps
     * like event detail and wave screens where user shouldn't manipulate the view.
     *
     * @param enabled true to allow gestures, false to disable all map interactions
     */
    fun setGesturesEnabled(enabled: Boolean)
}
