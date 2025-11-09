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
 * iOS MapLibreAdapter implementation.
 *
 * Bridges Kotlin shared logic to Swift MapLibreViewWrapper via MapWrapperRegistry.
 * Camera commands are stored in registry and executed immediately via callbacks.
 *
 * Architecture: Kotlin → MapWrapperRegistry → Swift MapLibreViewWrapper → MapLibre SDK
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

    @Suppress("ReturnCount")
    override fun getWidth(): Double {
        val wrapper = MapWrapperRegistry.getWrapper(eventId) ?: return DEFAULT_WIDTH
        val actualWidth = MapWrapperRegistry.getMapWidth(eventId)
        return if (actualWidth > 0) actualWidth else DEFAULT_WIDTH
    }

    @Suppress("ReturnCount")
    override fun getHeight(): Double {
        val wrapper = MapWrapperRegistry.getWrapper(eventId) ?: return DEFAULT_HEIGHT
        val actualHeight = MapWrapperRegistry.getMapHeight(eventId)
        return if (actualHeight > 0) actualHeight else DEFAULT_HEIGHT
    }

    override fun getCameraPosition(): Position? {
        // Try to get from registry first (updated by Swift)
        val registryPosition = MapWrapperRegistry.getCameraPosition(eventId)
        if (registryPosition != null) {
            return Position(registryPosition.first, registryPosition.second)
        }
        return _currentPosition.value
    }

    @Suppress("ReturnCount")
    override fun getVisibleRegion(): BoundingBox {
        if (wrapper == null) return createFallbackBounds()
        return MapWrapperRegistry.getVisibleRegion(eventId) ?: createFallbackBounds()
    }

    private fun createFallbackBounds(): BoundingBox =
        BoundingBox.fromCorners(
            listOf(
                Position(WWWGlobals.Geodetic.MIN_LATITUDE, WWWGlobals.Geodetic.MIN_LONGITUDE),
                Position(WWWGlobals.Geodetic.MAX_LATITUDE, WWWGlobals.Geodetic.MAX_LONGITUDE),
            ),
        )!!

    /** Update camera position from Swift delegate callback. */
    fun updateCameraPosition(
        latitude: Double,
        longitude: Double,
    ) {
        _currentPosition.value = Position(latitude, longitude)
    }

    /** Update zoom level from Swift delegate callback. */
    fun updateZoom(zoom: Double) {
        _currentZoom.value = zoom
    }

    override fun moveCamera(bounds: BoundingBox) {
        MapWrapperRegistry.setPendingCameraCommand(eventId, CameraCommand.MoveToBounds(bounds))
    }

    override fun animateCamera(
        position: Position,
        zoom: Double?,
        callback: MapCameraCallback?,
    ) {
        val callbackId = if (callback != null) "$eventId-animate-${Random.nextLong()}" else null

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
        val callbackId = if (callback != null) "$eventId-bounds-${Random.nextLong()}" else null

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
        originalEventBounds: BoundingBox?,
    ) {
        // Pass constraint bounds (for gestures) and original bounds (for min zoom) to iOS
        MapWrapperRegistry.setPendingCameraCommand(
            eventId,
            CameraCommand.SetConstraintBounds(constraintBounds, originalEventBounds, applyZoomSafetyMargin),
        )
    }

    override fun getMinZoomLevel(): Double {
        val wrapper = MapWrapperRegistry.getWrapper(eventId) ?: return 0.0
        MapWrapperRegistry.syncActualMinZoomFromWrapper(eventId)
        return MapWrapperRegistry.getActualMinZoom(eventId)
    }

    override fun setMinZoomPreference(minZoom: Double) {
        MapWrapperRegistry.setMinZoomCommand(eventId, minZoom)
    }

    override fun setMaxZoomPreference(maxZoom: Double) {
        MapWrapperRegistry.setMaxZoomCommand(eventId, maxZoom)
    }

    override fun setAttributionMargins(
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
    ) {
        MapWrapperRegistry.setAttributionMarginsCommand(eventId, left, top, right, bottom)
    }

    override fun addWavePolygons(
        polygons: List<Any>,
        clearExisting: Boolean,
    ) {
        // Polygon rendering handled by IosEventMap.updateWavePolygons()
    }

    override fun setOnMapClickListener(listener: ((Double, Double) -> Unit)?) {
        if (listener != null) {
            MapWrapperRegistry.setMapClickCoordinateListener(eventId, listener)
        } else {
            MapWrapperRegistry.clearMapClickCoordinateListener(eventId)
        }
    }

    override fun addOnCameraIdleListener(callback: () -> Unit) {
        MapWrapperRegistry.setCameraIdleListener(eventId, callback)
    }

    override fun drawOverridenBbox(bbox: BoundingBox) {
        MapWrapperRegistry.drawDebugBbox(eventId, bbox)
    }

    override fun onMapSet(callback: (MapLibreAdapter<*>) -> Unit) {
        MapWrapperRegistry.addOnMapReadyCallback(eventId) {
            callback(this)
        }

        if (MapWrapperRegistry.isStyleLoaded(eventId)) {
            MapWrapperRegistry.invokeMapReadyCallbacks(eventId)
        }
    }

    override fun enableLocationComponent(enabled: Boolean) {
        MapWrapperRegistry.enableLocationComponentOnWrapper(eventId, enabled)
    }

    override fun setUserPosition(position: Position) {
        MapWrapperRegistry.setUserPositionOnWrapper(eventId, position.lat, position.lng)
    }

    override fun setGesturesEnabled(enabled: Boolean) {
        MapWrapperRegistry.setGesturesEnabledOnWrapper(eventId, enabled)
    }

    /**
     * Cleanup adapter resources.
     * Resets StateFlows to ensure no lingering subscriptions.
     * Called from IosEventMap.onDispose.
     */
    fun cleanup() {
        _currentPosition.value = null
        _currentZoom.value = 10.0
        wrapper = null
    }
}
