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

import com.worldwidewaves.shared.events.utils.BoundingBox
import com.worldwidewaves.shared.events.utils.Position
import kotlinx.coroutines.flow.StateFlow

/**
 * Map interface that platform-specific implementations must implement
 * It provides convenient delegation helpers to the shared `cameraManager`
 * and `mapStateManager`, exposing a single cross-platform API surface.
 *
 * Constraint handling is now performed directly inside `SharedCameraManager`
 * and respects the **exact bounding box** supplied via `setBoundsConstraints`
 * (no extra padding is applied).  
 * The adapter exposes `constrainCamera()` so platform code can snap the
 * camera back inside those bounds after user gestures or other movements.
 */
interface MapLibreAdapter : PlatformMapOperations, PlatformMapRenderer {

    /* --------------------------------------------------------------------- */
    /* Shared helpers that handle cross-platform logic                       */
    /* --------------------------------------------------------------------- */

    /**
     * Shared camera controller responsible for camera state & animations.
     * Platform implementations **must** create it with `this`
     * (because this adapter fulfils [PlatformMapOperations]).
     */
    val cameraManager: SharedCameraManager

    /**
     * Shared map-state controller (wave polygons, click listeners, etc.).
     * Platform implementations **must** create it with `this`
     * (because this adapter fulfils [PlatformMapRenderer]).
     */
    val mapStateManager: SharedMapStateManager

    /* --------------------------------------------------------------------- */
    /* Reactive state – just proxy to the camera manager                     */
    /* --------------------------------------------------------------------- */

    val currentPosition: StateFlow<Position?>
        get() = cameraManager.currentPosition

    val currentZoom: StateFlow<Double>
        get() = cameraManager.currentZoom

    /* --------------------------------------------------------------------- */
    /* Public API – kept for backward compatibility, now delegating          */
    /* --------------------------------------------------------------------- */

    fun animateCamera(
        position: Position,
        zoom: Double? = null,
        callback: MapCameraCallback? = null
    ) = cameraManager.animateCamera(position, zoom, callback)

    fun animateCameraToBounds(
        bounds: BoundingBox,
        padding: Int = 0,
        callback: MapCameraCallback? = null
    ) = cameraManager.animateCameraToBounds(bounds, padding, callback)

    override fun setBoundsConstraints(bounds: BoundingBox) =
        cameraManager.setBoundsConstraints(bounds)

    override fun setMinZoomPreference(minZoom: Double) =
        cameraManager.setMinZoomPreference(minZoom)

    override fun setMaxZoomPreference(maxZoom: Double) =
        cameraManager.setMaxZoomPreference(maxZoom)

    fun addWavePolygons(polygons: List<Any>, clearExisting: Boolean = false) =
        mapStateManager.updateWavePolygons(polygons, clearExisting)

    fun setOnMapClickListener(listener: ((Double, Double) -> Unit)?) =
        mapStateManager.setMapClickListener(listener)

    /**
     * Ensures the camera remains inside previously set bounds constraints.
     *
     * @return `true` if the camera had to be moved to satisfy the constraints,
     *         `false` if no adjustment was necessary.
     */
    fun constrainCamera(): Boolean = cameraManager.constrainCamera()

    /* --------------------------------------------------------------------- */
    /* Lifecycle helpers – platform impls must call them at the right time   */
    /* --------------------------------------------------------------------- */

    /**
     * Must be invoked by the platform side once the underlying map view is
     * fully ready (after style load, etc.). Executes pending callbacks and
     * renders any queued layers.
     */
    fun onMapReady() = mapStateManager.notifyMapReady()

    /**
     * Call when the map view is about to be destroyed to release resources
     * and cancel listeners.
     */
    fun cleanup() {
        mapStateManager.cleanup()
    }
}

