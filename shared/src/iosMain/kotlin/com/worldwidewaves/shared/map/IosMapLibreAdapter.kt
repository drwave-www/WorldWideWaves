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
import kotlin.random.Random

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

    @Suppress("ReturnCount") // Multiple returns for guard clauses (null wrapper, zero dimension, valid dimension)
    override fun getWidth(): Double {
        // Get actual map view dimensions from Swift wrapper (not hardcoded defaults)
        val wrapper = MapWrapperRegistry.getWrapper(eventId)
        if (wrapper == null) {
            Log.w(TAG, "getWidth: wrapper is null, returning default: $DEFAULT_WIDTH")
            return DEFAULT_WIDTH
        }

        // Synchronously get width from Swift wrapper
        val actualWidth = MapWrapperRegistry.getMapWidth(eventId)
        if (actualWidth > 0) {
            Log.v(TAG, "getWidth: returning actual map width: $actualWidth")
            return actualWidth
        }

        Log.w(TAG, "getWidth: actual width is 0, returning default: $DEFAULT_WIDTH")
        return DEFAULT_WIDTH
    }

    @Suppress("ReturnCount") // Multiple returns for guard clauses (null wrapper, zero dimension, valid dimension)
    override fun getHeight(): Double {
        // Get actual map view dimensions from Swift wrapper (not hardcoded defaults)
        val wrapper = MapWrapperRegistry.getWrapper(eventId)
        if (wrapper == null) {
            Log.w(TAG, "getHeight: wrapper is null, returning default: $DEFAULT_HEIGHT")
            return DEFAULT_HEIGHT
        }

        // Synchronously get height from Swift wrapper
        val actualHeight = MapWrapperRegistry.getMapHeight(eventId)
        if (actualHeight > 0) {
            Log.v(TAG, "getHeight: returning actual map height: $actualHeight")
            return actualHeight
        }

        Log.w(TAG, "getHeight: actual height is 0, returning default: $DEFAULT_HEIGHT")
        return DEFAULT_HEIGHT
    }

    override fun getCameraPosition(): Position? {
        // Try to get from registry first (updated by Swift)
        val registryPosition = MapWrapperRegistry.getCameraPosition(eventId)
        if (registryPosition != null) {
            return Position(registryPosition.first, registryPosition.second)
        }
        return _currentPosition.value
    }

    @Suppress("ReturnCount") // Early returns for guard clauses - improves readability
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
     * Currently unused - camera updates happen via MapWrapperRegistry.updateCameraPosition/Zoom
     * Kept for potential future use with polling fallback.
     */
    @Suppress("UnusedPrivateMember")
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

        // Generate unique callback ID for this animation
        val callbackId =
            if (callback != null) {
                "$eventId-animate-${Random.nextLong()}"
            } else {
                null
            }

        // Store callback for async completion
        if (callback != null && callbackId != null) {
            MapWrapperRegistry.setCameraAnimationCallback(callbackId, callback)
        }

        MapWrapperRegistry.setPendingCameraCommand(
            eventId,
            CameraCommand.AnimateToPosition(position, zoom, callbackId),
        )
    }

    override fun animateCameraToBounds(
        bounds: BoundingBox,
        padding: Int,
        callback: MapCameraCallback?,
    ) {
        Log.d(TAG, "Animating camera to bounds with padding: $padding for event: $eventId")

        // Generate unique callback ID for this animation
        val callbackId =
            if (callback != null) {
                "$eventId-bounds-${Random.nextLong()}"
            } else {
                null
            }

        // Store callback for async completion
        if (callback != null && callbackId != null) {
            MapWrapperRegistry.setCameraAnimationCallback(callbackId, callback)
        }

        MapWrapperRegistry.setPendingCameraCommand(
            eventId,
            CameraCommand.AnimateToBounds(bounds, padding, callbackId),
        )
    }

    override fun setBoundsForCameraTarget(
        constraintBounds: BoundingBox,
        applyZoomSafetyMargin: Boolean,
    ) {
        Log.d(TAG, "Setting camera constraint bounds for event: $eventId, applyZoomSafetyMargin=$applyZoomSafetyMargin")
        Log.d(
            TAG,
            "BBox details: minLat=${constraintBounds.minLatitude}, maxLat=${constraintBounds.maxLatitude}, " +
                "minLng=${constraintBounds.minLongitude}, maxLng=${constraintBounds.maxLongitude}",
        )
        Log.d(
            TAG,
            "SW/NE: SW(${constraintBounds.sw.lat},${constraintBounds.sw.lng}) NE(${constraintBounds.ne.lat},${constraintBounds.ne.lng})",
        )
        // Note: iOS implementation handles zoom safety via Swift (not passed through command)
        // The applyZoomSafetyMargin parameter is Android-specific for now
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

        // Synchronously update actualMinZoom from Swift before querying
        // This prevents race condition where cached registry value returns 0.0
        // while map view has the correct constraint-based min zoom
        MapWrapperRegistry.syncActualMinZoomFromWrapper(eventId)

        val actualMinZoom = MapWrapperRegistry.getActualMinZoom(eventId)
        Log.d(TAG, "getMinZoomLevel: returning actual map value: $actualMinZoom for event: $eventId")
        return actualMinZoom
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
        Log.d(TAG, "Setting attribution margins: left=$left, top=$top, right=$right, bottom=$bottom for event: $eventId")
        MapWrapperRegistry.setAttributionMarginsCommand(eventId, left, top, right, bottom)
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
        Log.d(TAG, "Registering onMapSet callback for event: $eventId")

        // Store callback in registry (will be invoked after style loads)
        MapWrapperRegistry.addOnMapReadyCallback(eventId) {
            Log.d(TAG, "Map ready callback invoked for event: $eventId")
            callback(this)
        }

        // Check if style is already loaded (edge case: callback registered late)
        val isStyleLoaded = MapWrapperRegistry.isStyleLoaded(eventId)
        if (isStyleLoaded) {
            Log.i(TAG, "Style already loaded, invoking callback immediately for event: $eventId")
            MapWrapperRegistry.invokeMapReadyCallbacks(eventId)
        }
    }

    override fun enableLocationComponent(enabled: Boolean) {
        Log.i(TAG, "enableLocationComponent: $enabled for event: $eventId")
        // Call Swift wrapper via registry callback
        MapWrapperRegistry.enableLocationComponentOnWrapper(eventId, enabled)
    }

    override fun setUserPosition(position: Position) {
        Log.i(TAG, "📍 setUserPosition: (${position.lat}, ${position.lng}) for event: $eventId")

        // Verify callback is registered
        val hasCallback = MapWrapperRegistry.hasUserPositionCallback(eventId)
        Log.d(TAG, "User position callback registered: $hasCallback")

        // Call Swift wrapper via registry callback
        MapWrapperRegistry.setUserPositionOnWrapper(eventId, position.lat, position.lng)
        Log.v(TAG, "✅ setUserPositionOnWrapper called for event: $eventId")
    }

    override fun setGesturesEnabled(enabled: Boolean) {
        Log.i(TAG, "setGesturesEnabled: $enabled for event: $eventId")
        // Call Swift wrapper via registry callback
        MapWrapperRegistry.setGesturesEnabledOnWrapper(eventId, enabled)
    }
}
