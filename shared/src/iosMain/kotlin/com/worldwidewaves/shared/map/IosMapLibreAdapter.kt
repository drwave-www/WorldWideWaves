package com.worldwidewaves.shared.map

/*
 * Copyright 2025 DrWave
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

import com.worldwidewaves.shared.WWWGlobals
import com.worldwidewaves.shared.events.utils.BoundingBox
import com.worldwidewaves.shared.events.utils.Position
import com.worldwidewaves.shared.utils.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

private const val TAG = "IosMapLibreAdapter"

// Default dimensions for iPhone (used when wrapper not available)
private const val DEFAULT_WIDTH = 375.0
private const val DEFAULT_HEIGHT = 812.0

/**
 * iOS implementation of MapLibreAdapter using iOS MapLibre SDK via Swift wrapper.
 *
 * This adapter bridges Kotlin shared logic to Swift MapLibreViewWrapper via MapWrapperRegistry.
 * Camera commands are stored in the registry and polled/executed by Swift.
 *
 * Architecture:
 * - Kotlin (this class) → MapWrapperRegistry (camera commands) → Swift MapLibreViewWrapper → MapLibre SDK
 *
 * Similar to polygon rendering, camera commands use the registry pattern for Kotlin-Swift coordination.
 *
 * Note: Each IosEventMap creates its own adapter instance with event-specific ID for
 * proper camera command routing through the registry.
 */
class IosMapLibreAdapter(
    private val eventId: String,
) : MapLibreAdapter<Any> {
    // Wrapper instance (not directly used - commands go through MapWrapperRegistry)
    private var wrapper: Any? = null

    private val _currentPosition = MutableStateFlow<Position?>(null)
    private val _currentZoom = MutableStateFlow(10.0)

    override val currentPosition: StateFlow<Position?> = _currentPosition
    override val currentZoom: StateFlow<Double> = _currentZoom

    /**
     * Sets the map wrapper.
     * Map rendering happens via SwiftUI EventMapView embedded in Compose.
     */
    override fun setMap(map: Any) {
        this.wrapper = map
        Log.d(TAG, "Map wrapper set for eventId: $eventId")
    }

    override fun setStyle(
        stylePath: String,
        callback: () -> Unit?,
    ) {
        if (wrapper == null) {
            Log.w(TAG, "Cannot set style - wrapper not initialized")
            return
        }

        Log.d(TAG, "Setting map style: $stylePath")

        // NOTE: Swift wrapper call will be implemented via cinterop
        // For now, just invoke callback to prevent blocking
        callback.invoke()
    }

    override fun getWidth(): Double = DEFAULT_WIDTH

    override fun getHeight(): Double = DEFAULT_HEIGHT

    override fun getCameraPosition(): Position? = _currentPosition.value

    override fun getVisibleRegion(): BoundingBox {
        if (wrapper == null) {
            return createFallbackBounds()
        }

        // NOTE: Swift wrapper call will be implemented via cinterop
        return createFallbackBounds()
    }

    private fun createFallbackBounds(): BoundingBox =
        BoundingBox.fromCorners(
            listOf(
                Position(WWWGlobals.Geodetic.MIN_LATITUDE, WWWGlobals.Geodetic.MIN_LONGITUDE),
                Position(WWWGlobals.Geodetic.MAX_LATITUDE, WWWGlobals.Geodetic.MAX_LONGITUDE),
            ),
        )!!

    /**
     * Update camera position from Swift delegate callback.
     * Called by Swift code when map camera changes.
     */
    fun updateCameraPosition(
        latitude: Double,
        longitude: Double,
    ) {
        _currentPosition.value = Position(latitude, longitude)
    }

    /**
     * Update zoom level from Swift delegate callback.
     * Called by Swift code when map zoom changes.
     */
    fun updateZoom(zoom: Double) {
        _currentZoom.value = zoom
    }

    override fun moveCamera(bounds: BoundingBox) {
        Log.d(TAG, "Moving camera to bounds for event: $eventId")
        MapWrapperRegistry.setPendingCameraCommand(
            eventId,
            CameraCommand.MoveToBounds(bounds),
        )
    }

    override fun animateCamera(
        position: Position,
        zoom: Double?,
        callback: MapCameraCallback?,
    ) {
        Log.d(TAG, "Animating camera to position: ${position.lat}, ${position.lng}, zoom=$zoom for event: $eventId")
        MapWrapperRegistry.setPendingCameraCommand(
            eventId,
            CameraCommand.AnimateToPosition(position, zoom),
        )
        // Note: Callback is invoked immediately. For proper callback timing,
        // Swift would need to signal back through registry when animation completes.
        // For now, immediate callback prevents blocking.
        callback?.onFinish()
    }

    override fun animateCameraToBounds(
        bounds: BoundingBox,
        padding: Int,
        callback: MapCameraCallback?,
    ) {
        Log.d(TAG, "Animating camera to bounds with padding: $padding for event: $eventId")
        MapWrapperRegistry.setPendingCameraCommand(
            eventId,
            CameraCommand.AnimateToBounds(bounds, padding),
        )
        // Note: Callback is invoked immediately. For proper callback timing,
        // Swift would need to signal back through registry when animation completes.
        callback?.onFinish()
    }

    override fun setBoundsForCameraTarget(constraintBounds: BoundingBox) {
        Log.d(TAG, "Setting camera constraint bounds for event: $eventId")
        MapWrapperRegistry.setPendingCameraCommand(
            eventId,
            CameraCommand.SetConstraintBounds(constraintBounds),
        )
    }

    override fun getMinZoomLevel(): Double {
        // NOTE: Implement with proper MapLibre iOS bindings
        return 0.0
    }

    override fun setMinZoomPreference(minZoom: Double) {
        if (wrapper != null) {
            // NOTE: Implement via cinterop bindings
            // wrapper.minimumZoomLevel = minZoom
            Log.d("IosMapLibreAdapter", "Set minimum zoom level: $minZoom")
        }
    }

    override fun setMaxZoomPreference(maxZoom: Double) {
        if (wrapper != null) {
            // NOTE: Implement via cinterop bindings
            // wrapper.maximumZoomLevel = maxZoom
            Log.d("IosMapLibreAdapter", "Set maximum zoom level: $maxZoom")
        }
    }

    override fun setAttributionMargins(
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
    ) {
        Log.d("IosMapLibreAdapter", "Setting attribution margins: $left, $top, $right, $bottom")

        // NOTE: Implement iOS MapLibre attribution positioning
        // Set attribution view margins
    }

    override fun addWavePolygons(
        polygons: List<Any>,
        clearExisting: Boolean,
    ) {
        Log.d(TAG, "addWavePolygons called - delegated to IosEventMap.updateWavePolygons()")
        // Note: Polygon rendering is handled by IosEventMap.updateWavePolygons()
        // which stores polygons in MapWrapperRegistry for Swift to render.
        // This method is not called in the current iOS architecture.
    }

    override fun setOnMapClickListener(listener: ((Double, Double) -> Unit)?) {
        Log.d("IosMapLibreAdapter", "Setting map click listener")

        // NOTE: Implement iOS MapLibre tap gesture handling
        // Add tap gesture recognizer and forward coordinates to listener
    }

    override fun addOnCameraIdleListener(callback: () -> Unit) {
        Log.d("IosMapLibreAdapter", "Adding camera idle listener")

        // NOTE: Implement iOS MapLibre camera idle detection
        // Listen for camera movement completion and call callback
    }

    override fun drawOverridenBbox(bbox: BoundingBox) {
        Log.d("IosMapLibreAdapter", "Drawing override bounding box")

        // NOTE: Implement iOS MapLibre bounding box overlay
        // Draw visual bounds overlay on map
    }

    override fun onMapSet(callback: (MapLibreAdapter<*>) -> Unit) {
        Log.d("IosMapLibreAdapter", "Map set callback")
        // NOTE: Implement map ready callback for iOS
        callback(this)
    }
}
