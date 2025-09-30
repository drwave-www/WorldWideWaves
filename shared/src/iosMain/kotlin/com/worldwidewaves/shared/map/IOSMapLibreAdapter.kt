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
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import platform.darwin.NSObject

private const val TAG = "IOSMapLibreAdapter"

// Default dimensions for iPhone (used when wrapper not available)
private const val DEFAULT_WIDTH = 375.0
private const val DEFAULT_HEIGHT = 812.0

/**
 * iOS implementation of MapLibreAdapter using iOS MapLibre SDK via Swift wrapper.
 *
 * This adapter bridges Kotlin shared logic to Swift MapLibreViewWrapper.
 * The Swift wrapper (MapLibreViewWrapper from iosApp) is passed via setMap()
 * as an NSObject and methods are called via Kotlin/Native ObjC interop.
 *
 * Architecture:
 * - Kotlin (this class) ← ObjC interop → Swift (@objc MapLibreViewWrapper) ← → MapLibre SDK
 *
 * The wrapper must be an @objc Swift class with @objc methods to be callable from Kotlin.
 */
@OptIn(ExperimentalForeignApi::class)
class IOSMapLibreAdapter : MapLibreAdapter<Any> {
    // Swift wrapper instance as NSObject (all @objc Swift classes inherit from NSObject)
    private var wrapper: NSObject? = null

    private val _currentPosition = MutableStateFlow<Position?>(null)
    private val _currentZoom = MutableStateFlow(10.0)

    override val currentPosition: StateFlow<Position?> = _currentPosition
    override val currentZoom: StateFlow<Double> = _currentZoom

    /**
     * Sets the MapLibre wrapper (expects @objc Swift MapLibreViewWrapper).
     * The wrapper should already have the MLNMapView configured.
     */
    override fun setMap(map: Any) {
        this.wrapper = map as? NSObject
        if (wrapper != null) {
            WWWLogger.i(TAG, "Map wrapper set successfully (type: ${wrapper!!::class.simpleName})")
            // Update initial camera state from wrapper
            updateCameraStateFromWrapper()
        } else {
            WWWLogger.e(TAG, "Failed to cast map to NSObject")
        }
    }

    /**
     * Update camera state from wrapper's current values using performSelector
     */
    private fun updateCameraStateFromWrapper() {
        wrapper?.let { w ->
            try {
                // Note: Actual method calls will need to use performSelector or
                // the wrapper methods need to be accessible via platform imports
                // For now, just log that wrapper is set
                WWWLogger.d(TAG, "Wrapper ready for method calls")
            } catch (e: Exception) {
                WWWLogger.e(TAG, "Error updating camera state: ${e.message}")
            }
        }
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
        if (wrapper == null) {
            WWWLogger.w(TAG, "getWidth() called with null wrapper, returning default")
            return DEFAULT_WIDTH
        }

        // TODO: Call wrapper.getWidth() via performSelector or ObjC runtime
        // For now return default until method calling is implemented
        return DEFAULT_WIDTH
    }

    override fun getHeight(): Double {
        if (wrapper == null) {
            WWWLogger.w(TAG, "getHeight() called with null wrapper, returning default")
            return DEFAULT_HEIGHT
        }

        // TODO: Call wrapper.getHeight() via performSelector or ObjC runtime
        // For now return default until method calling is implemented
        return DEFAULT_HEIGHT
    }

    override fun getCameraPosition(): Position? {
        if (wrapper == null) {
            WWWLogger.w(TAG, "getCameraPosition() called with null wrapper")
            return null
        }

        // TODO: Call wrapper.getCameraCenterLatitude/Longitude() via performSelector
        // For now return current state
        return _currentPosition.value
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
