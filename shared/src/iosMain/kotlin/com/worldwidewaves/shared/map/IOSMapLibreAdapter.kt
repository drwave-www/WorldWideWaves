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
import com.worldwidewaves.shared.utils.WWWLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

private const val TAG = "IOSMapLibreAdapter"

/**
 * iOS implementation of MapLibreAdapter using iOS MapLibre SDK via Swift wrapper.
 *
 * This adapter bridges Kotlin shared logic to Swift MapLibreViewWrapper which wraps
 * the native iOS MapLibre SDK (MLNMapView). The Swift wrapper is injected via setMap()
 * and must conform to the expected interface (defined in MapLibreViewWrapper.swift).
 *
 * Architecture:
 * - Kotlin (this class) ← → Swift (MapLibreViewWrapper) ← → Objective-C/Swift (MapLibre SDK)
 *
 * Note: Swift classes are accessed via dynamic dispatch since Kotlin/Native can only directly
 * interop with Objective-C (not Swift). The Swift wrapper uses @objc annotations to expose
 * its interface.
 */
class IOSMapLibreAdapter : MapLibreAdapter<Any> {
    // Swift wrapper instance - accessed via runtime checks
    private var wrapper: Any? = null

    private val _currentPosition = MutableStateFlow<Position?>(null)
    private val _currentZoom = MutableStateFlow(10.0)

    override val currentPosition: StateFlow<Position?> = _currentPosition
    override val currentZoom: StateFlow<Double> = _currentZoom

    /**
     * Sets the MapLibre wrapper (expects MapLibreViewWrapper Swift object).
     * The wrapper should already have the MLNMapView configured.
     */
    override fun setMap(map: Any) {
        this.wrapper = map
        WWWLogger.d(TAG, "Map wrapper set")

        // Note: Camera position and zoom updates will be pushed from Swift delegate callbacks
        // via updateCameraPosition() and updateZoom() methods
    }

    override fun setStyle(
        stylePath: String,
        callback: () -> Unit?,
    ) {
        if (wrapper == null) {
            WWWLogger.w(TAG, "Cannot set style - wrapper not initialized")
            return
        }

        WWWLogger.d(TAG, "Setting map style: $stylePath")

        // NOTE: Swift wrapper call will be implemented via cinterop
        // For now, just invoke callback to prevent blocking
        callback.invoke()
    }

    override fun getWidth(): Double {
        if (wrapper == null) return 375.0 // Default iPhone width

        // NOTE: Swift wrapper call will be implemented via cinterop
        return 375.0
    }

    override fun getHeight(): Double {
        if (wrapper == null) return 812.0 // Default iPhone height

        // NOTE: Swift wrapper call will be implemented via cinterop
        return 812.0
    }

    override fun getCameraPosition(): Position? {
        if (wrapper == null) return null

        // NOTE: Swift wrapper call will be implemented via cinterop
        return null
    }

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
        if (wrapper != null) {
            WWWLogger.d("IOSMapLibreAdapter", "Moving camera to bounds")

            // NOTE: Implement iOS MapLibre camera movement
            // Will be implemented via cinterop bindings
        }
    }

    override fun animateCamera(
        position: Position,
        zoom: Double?,
        callback: MapCameraCallback?,
    ) {
        if (wrapper != null) {
            WWWLogger.d("IOSMapLibreAdapter", "Animating camera to position: ${position.lat}, ${position.lng}")

            // NOTE: Implement iOS MapLibre camera animation
            // Will be implemented via cinterop bindings

            callback?.onFinish()
        }
    }

    override fun animateCameraToBounds(
        bounds: BoundingBox,
        padding: Int,
        callback: MapCameraCallback?,
    ) {
        if (wrapper != null) {
            WWWLogger.d("IOSMapLibreAdapter", "Animating camera to bounds with padding: $padding")

            // NOTE: Implement iOS MapLibre bounds animation
            // Will be implemented via cinterop bindings

            callback?.onFinish()
        }
    }

    override fun setBoundsForCameraTarget(constraintBounds: BoundingBox) {
        WWWLogger.d("IOSMapLibreAdapter", "Setting camera constraint bounds")

        // NOTE: Implement iOS MapLibre camera constraints
        // Set bounds within which the camera can move
    }

    override fun getMinZoomLevel(): Double {
        // NOTE: Implement with proper MapLibre iOS bindings
        return 0.0
    }

    override fun setMinZoomPreference(minZoom: Double) {
        if (wrapper != null) {
            // NOTE: Implement via cinterop bindings
            // wrapper.minimumZoomLevel = minZoom
            WWWLogger.d("IOSMapLibreAdapter", "Set minimum zoom level: $minZoom")
        }
    }

    override fun setMaxZoomPreference(maxZoom: Double) {
        if (wrapper != null) {
            // NOTE: Implement via cinterop bindings
            // wrapper.maximumZoomLevel = maxZoom
            WWWLogger.d("IOSMapLibreAdapter", "Set maximum zoom level: $maxZoom")
        }
    }

    override fun setAttributionMargins(
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
    ) {
        WWWLogger.d("IOSMapLibreAdapter", "Setting attribution margins: $left, $top, $right, $bottom")

        // NOTE: Implement iOS MapLibre attribution positioning
        // Set attribution view margins
    }

    override fun addWavePolygons(
        polygons: List<Any>,
        clearExisting: Boolean,
    ) {
        if (wrapper != null) {
            WWWLogger.d("IOSMapLibreAdapter", "Adding ${polygons.size} wave polygons, clearExisting: $clearExisting")

            // NOTE: Implement iOS MapLibre polygon rendering
            // Will be implemented via cinterop bindings
            // Convert polygons to iOS MapLibre format and add to map
            // Handle clearExisting flag to remove previous polygons
        }
    }

    override fun setOnMapClickListener(listener: ((Double, Double) -> Unit)?) {
        WWWLogger.d("IOSMapLibreAdapter", "Setting map click listener")

        // NOTE: Implement iOS MapLibre tap gesture handling
        // Add tap gesture recognizer and forward coordinates to listener
    }

    override fun addOnCameraIdleListener(callback: () -> Unit) {
        WWWLogger.d("IOSMapLibreAdapter", "Adding camera idle listener")

        // NOTE: Implement iOS MapLibre camera idle detection
        // Listen for camera movement completion and call callback
    }

    override fun drawOverridenBbox(bbox: BoundingBox) {
        WWWLogger.d("IOSMapLibreAdapter", "Drawing override bounding box")

        // NOTE: Implement iOS MapLibre bounding box overlay
        // Draw visual bounds overlay on map
    }

    override fun onMapSet(callback: (MapLibreAdapter<*>) -> Unit) {
        WWWLogger.d("IOSMapLibreAdapter", "Map set callback")
        // NOTE: Implement map ready callback for iOS
        callback(this)
    }
}
