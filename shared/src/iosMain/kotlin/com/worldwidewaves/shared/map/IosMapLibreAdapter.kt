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
import platform.UIKit.UIImage

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
) : MapLibreAdapter<UIImage> {
    // Wrapper instance (not directly used - commands go through MapWrapperRegistry)
    private var wrapper: UIImage? = null

    private val _currentPosition = MutableStateFlow<Position?>(null)
    private val _currentZoom = MutableStateFlow(10.0)

    override val currentPosition: StateFlow<Position?> = _currentPosition
    override val currentZoom: StateFlow<Double> = _currentZoom

    /**
     * Sets the map wrapper.
     * Map rendering happens via SwiftUI EventMapView embedded in Compose.
     */
    override fun setMap(map: UIImage) {
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

    override fun getCameraPosition(): Position? {
        // Try to get from registry first (updated by Swift)
        val registryPosition = MapWrapperRegistry.getCameraPosition(eventId)
        if (registryPosition != null) {
            return Position(registryPosition.first, registryPosition.second)
        }
        return _currentPosition.value
    }

    override fun getVisibleRegion(): BoundingBox {
        val w = wrapper
        if (w == null) {
            Log.w(TAG, "getVisibleRegion: wrapper is null, returning fallback")
            return createFallbackBounds()
        }

        // Get visible region from Swift wrapper
        val visibleRegion = MapWrapperRegistry.getVisibleRegion(eventId)
        if (visibleRegion == null) {
            Log.w(TAG, "getVisibleRegion: no visible region available, returning fallback")
            return createFallbackBounds()
        }

        Log.d(TAG, "getVisibleRegion: got region from Swift wrapper")
        return visibleRegion
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
     * Now bridges from MapWrapperRegistry to StateFlow.
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
     * Now bridges from MapWrapperRegistry to StateFlow.
     */
    fun updateZoom(zoom: Double) {
        _currentZoom.value = zoom
    }

    /**
     * Poll registry for camera updates and update StateFlows.
     * Called periodically or on demand to sync registry → StateFlow.
     */
    private fun syncCameraStateFromRegistry() {
        val position = MapWrapperRegistry.getCameraPosition(eventId)
        if (position != null) {
            _currentPosition.value = Position(position.first, position.second)
        }

        val zoom = MapWrapperRegistry.getCameraZoom(eventId)
        if (zoom != null) {
            _currentZoom.value = zoom
        }
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
        Log.d(
            TAG,
            "BBox details: minLat=${constraintBounds.minLatitude}, maxLat=${constraintBounds.maxLatitude}, " +
                "minLng=${constraintBounds.minLongitude}, maxLng=${constraintBounds.maxLongitude}",
        )
        Log.d(
            TAG,
            "SW/NE: SW(${constraintBounds.sw.lat},${constraintBounds.sw.lng}) NE(${constraintBounds.ne.lat},${constraintBounds.ne.lng})",
        )
        MapWrapperRegistry.setPendingCameraCommand(
            eventId,
            CameraCommand.SetConstraintBounds(constraintBounds),
        )
    }

    override fun getMinZoomLevel(): Double {
        val wrapper = MapWrapperRegistry.getWrapper(eventId)
        if (wrapper == null) {
            Log.w(TAG, "getMinZoomLevel: wrapper is null, returning 0.0")
            return 0.0
        }

        // Request min zoom via registry (Swift will provide)
        return MapWrapperRegistry.getMinZoom(eventId)
    }

    override fun setMinZoomPreference(minZoom: Double) {
        Log.d(TAG, "Setting minimum zoom preference: $minZoom for event: $eventId")
        MapWrapperRegistry.setMinZoomCommand(eventId, minZoom)
    }

    override fun setMaxZoomPreference(maxZoom: Double) {
        Log.d(TAG, "Setting maximum zoom preference: $maxZoom for event: $eventId")
        MapWrapperRegistry.setMaxZoomCommand(eventId, maxZoom)
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
        Log.d(TAG, "Setting map click coordinate listener for event: $eventId")

        if (listener != null) {
            // Store listener in registry, Swift wrapper will invoke with coordinates
            MapWrapperRegistry.setMapClickCoordinateListener(eventId, listener)
        } else {
            MapWrapperRegistry.clearMapClickCoordinateListener(eventId)
        }
    }

    override fun addOnCameraIdleListener(callback: () -> Unit) {
        Log.d(TAG, "Adding camera idle listener for event: $eventId")
        MapWrapperRegistry.setCameraIdleListener(eventId, callback)
    }

    override fun drawOverridenBbox(bbox: BoundingBox) {
        Log.d(TAG, "Drawing override bounding box for event: $eventId")

        // Trigger bbox drawing via registry
        MapWrapperRegistry.drawDebugBbox(eventId, bbox)
    }

    override fun onMapSet(callback: (MapLibreAdapter<*>) -> Unit) {
        Log.d("IosMapLibreAdapter", "Map set callback")
        // NOTE: Implement map ready callback for iOS
        callback(this)
    }
}
