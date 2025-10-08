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

import com.worldwidewaves.shared.events.utils.BoundingBox
import com.worldwidewaves.shared.events.utils.Position
import com.worldwidewaves.shared.utils.Log
import kotlin.experimental.ExperimentalNativeApi

/**
 * Camera command types for iOS map control.
 */
sealed class CameraCommand {
    data class AnimateToPosition(
        val position: Position,
        val zoom: Double?,
        val callbackId: String? = null,
    ) : CameraCommand()

    data class AnimateToBounds(
        val bounds: BoundingBox,
        val padding: Int = 0,
        val callbackId: String? = null,
    ) : CameraCommand()

    data class MoveToBounds(
        val bounds: BoundingBox,
    ) : CameraCommand()

    data class SetConstraintBounds(
        val bounds: BoundingBox,
    ) : CameraCommand()

    data class SetMinZoom(
        val minZoom: Double,
    ) : CameraCommand()

    data class SetMaxZoom(
        val maxZoom: Double,
    ) : CameraCommand()
}

/**
 * Registry to store MapLibreViewWrapper instances, polygon data, and camera commands.
 * This allows coordination between Kotlin (IosEventMap) and Swift (MapLibreViewWrapper).
 *
 * Architecture:
 * 1. Swift creates MapLibreViewWrapper in EventMapView
 * 2. Swift registers it here via registerWrapper(eventId, wrapper)
 * 3. Kotlin triggers immediate updates via callback system (direct dispatch)
 * 4. Swift receives callbacks and executes immediately on main thread
 *
 * Memory Management (STRONG REFERENCES):
 * - Uses STRONG references (not WeakReference) to prevent premature GC
 * - Wrappers survive entire screen session for dynamic updates
 * - CRITICAL: Must call unregisterWrapper() on screen exit to prevent leaks
 * - DisposableEffect in IosEventMap handles cleanup automatically
 * - No LRU eviction - explicit lifecycle management only
 */
@OptIn(ExperimentalNativeApi::class)
object MapWrapperRegistry {
    private const val TAG = "MapWrapperRegistry"

    /**
     * Strong references to wrappers.
     * Wrappers are kept alive for entire screen session.
     * Explicit cleanup via unregisterWrapper() is required on screen exit.
     *
     * Changed from WeakReference to strong references to prevent premature GC.
     * The wrapper MUST survive the entire screen session to handle dynamic updates.
     */
    private val wrappers = mutableMapOf<String, Any>()

    // Store pending polygon data that Swift will render
    private val pendingPolygons = mutableMapOf<String, PendingPolygonData>()

    // Store pending camera commands that Swift will execute
    private val pendingCameraCommands = mutableMapOf<String, CameraCommand>()

    // Store map click callbacks that Swift will invoke
    private val mapClickCallbacks = mutableMapOf<String, () -> Unit>()

    // Store render callbacks that Swift wrappers can register for immediate notifications
    private val renderCallbacks = mutableMapOf<String, () -> Unit>()

    // REMOVED: LRU eviction - no longer needed with explicit cleanup
    // Wrappers are now managed explicitly via registerWrapper/unregisterWrapper

    data class PendingPolygonData(
        val coordinates: List<List<Pair<Double, Double>>>, // List of polygons, each containing lat/lng pairs
        val clearExisting: Boolean,
    )

    /**
     * Register a MapLibreViewWrapper for an event.
     * Called from Swift after wrapper creation.
     * Uses STRONG reference - wrapper survives entire screen session.
     * MUST call unregisterWrapper() on screen exit to prevent memory leaks.
     */
    fun registerWrapper(
        eventId: String,
        wrapper: Any,
    ) {
        Log.d(TAG, "Registering wrapper for event: $eventId (total wrappers: ${wrappers.size})")

        // Store strong reference
        wrappers[eventId] = wrapper

        Log.i(TAG, "✅ Wrapper registered with STRONG reference for: $eventId")

        // If there are pending polygons, notify that they should be rendered
        if (pendingPolygons.containsKey(eventId)) {
            Log.i(TAG, "Wrapper registered, pending polygons available for: $eventId")
        }
    }

    /**
     * Get the registered wrapper for an event.
     * Returns null if not yet registered.
     * With strong references, wrapper is guaranteed to exist until unregisterWrapper() is called.
     */
    fun getWrapper(eventId: String): Any? {
        val wrapper = wrappers[eventId]
        if (wrapper == null) {
            Log.w(TAG, "No wrapper registered for event: $eventId")
            return null
        }

        Log.v(TAG, "Retrieved wrapper for event: $eventId")
        return wrapper
    }

    /**
     * Store polygon data to be rendered.
     * Called from Kotlin when polygons need to be displayed.
     */
    fun setPendingPolygons(
        eventId: String,
        coordinates: List<List<Pair<Double, Double>>>,
        clearExisting: Boolean,
    ) {
        val totalPoints = coordinates.sumOf { it.size }
        Log.i(
            TAG,
            "🌊 Storing ${coordinates.size} pending polygons for event: $eventId ($totalPoints total points, clearExisting=$clearExisting)",
        )
        pendingPolygons[eventId] = PendingPolygonData(coordinates, clearExisting)
        Log.d(TAG, "Polygons stored, hasPending=${hasPendingPolygons(eventId)}, registrySize=${pendingPolygons.size}")
    }

    /**
     * Get pending polygon data for an event.
     * Swift calls this to retrieve polygons that need to be rendered.
     * Returns null if no pending polygons.
     */
    fun getPendingPolygons(eventId: String): PendingPolygonData? = pendingPolygons[eventId]

    /**
     * Check if there are pending polygons for an event.
     */
    fun hasPendingPolygons(eventId: String): Boolean = pendingPolygons.containsKey(eventId)

    /**
     * Clear pending polygons after they've been rendered.
     * Swift calls this after successfully rendering polygons.
     */
    fun clearPendingPolygons(eventId: String) {
        Log.v(TAG, "Clearing pending polygons for event: $eventId")
        pendingPolygons.remove(eventId)
    }

    /**
     * Unregister a wrapper when the map screen is exited.
     * CRITICAL: Must be called to release strong reference and prevent memory leaks.
     * Clears wrapper, pending data, and all callbacks for the event.
     */
    fun unregisterWrapper(eventId: String) {
        Log.i(TAG, "🧹 Unregistering wrapper and cleaning up for event: $eventId")

        // Remove wrapper (releases strong reference)
        wrappers.remove(eventId)

        // Clean up all associated data and callbacks
        pendingPolygons.remove(eventId)
        pendingCameraCommands.remove(eventId)
        mapClickCallbacks.remove(eventId)
        mapClickRegistrationCallbacks.remove(eventId)
        renderCallbacks.remove(eventId)
        cameraCallbacks.remove(eventId)
        visibleRegions.remove(eventId)
        minZoomLevels.remove(eventId)
        cameraIdleListeners.remove(eventId)
        onMapReadyCallbacks.remove(eventId)
        styleLoadedStates.remove(eventId)

        Log.i(TAG, "✅ Wrapper unregistered and cleanup complete for: $eventId")
    }

    // REMOVED: pruneStaleReferences() - no longer needed with strong references

    /**
     * Store a camera command to be executed.
     * Called from Kotlin when camera needs to be controlled.
     */
    fun setPendingCameraCommand(
        eventId: String,
        command: CameraCommand,
    ) {
        val commandDetails =
            when (command) {
                is CameraCommand.AnimateToPosition ->
                    "AnimateToPosition(${command.position.lat},${command.position.lng},zoom=${command.zoom})"
                is CameraCommand.AnimateToBounds -> "AnimateToBounds(padding=${command.padding})"
                is CameraCommand.MoveToBounds -> "MoveToBounds"
                is CameraCommand.SetConstraintBounds -> "SetConstraintBounds"
                is CameraCommand.SetMinZoom -> "SetMinZoom(${command.minZoom})"
                is CameraCommand.SetMaxZoom -> "SetMaxZoom(${command.maxZoom})"
            }
        Log.i(TAG, "📸 Storing camera command for event: $eventId → $commandDetails")
        pendingCameraCommands[eventId] = command
        Log.d(TAG, "Camera command stored, hasPending=${hasPendingCameraCommand(eventId)}")

        // Trigger immediate execution (like wave polygons)
        requestImmediateCameraExecution(eventId)
    }

    // Store camera execution callbacks
    private val cameraCallbacks = mutableMapOf<String, () -> Unit>()

    /**
     * Register a callback that Swift wrapper will invoke when camera command needs execution.
     * This enables direct dispatch pattern (no polling).
     */
    fun setCameraCallback(
        eventId: String,
        callback: () -> Unit,
    ) {
        Log.d(TAG, "Setting camera callback for event: $eventId")
        cameraCallbacks[eventId] = callback
    }

    /**
     * Request immediate execution of pending camera command.
     * Invokes the registered camera callback if available.
     */
    fun requestImmediateCameraExecution(eventId: String) {
        val callback = cameraCallbacks[eventId]
        if (callback != null) {
            Log.i(TAG, "📸 Triggering immediate camera execution for event: $eventId")
            platform.darwin.dispatch_async(platform.darwin.dispatch_get_main_queue()) {
                callback.invoke()
            }
        } else {
            Log.v(TAG, "No camera callback registered for event: $eventId (will fall back to polling)")
        }
    }

    /**
     * Get pending camera command for an event.
     * Swift calls this to retrieve camera commands that need to be executed.
     * Returns null if no pending commands.
     */
    fun getPendingCameraCommand(eventId: String): CameraCommand? = pendingCameraCommands[eventId]

    /**
     * Check if there is a pending camera command for an event.
     */
    fun hasPendingCameraCommand(eventId: String): Boolean = pendingCameraCommands.containsKey(eventId)

    /**
     * Clear pending camera command after it's been executed.
     * Swift calls this after successfully executing the command.
     */
    fun clearPendingCameraCommand(eventId: String) {
        Log.v(TAG, "Clearing pending camera command for event: $eventId")
        pendingCameraCommands.remove(eventId)
    }

    /**
     * Set map click callback for an event.
     * Swift will invoke this when the map is tapped.
     */
    fun setMapClickCallback(
        eventId: String,
        callback: () -> Unit,
    ) {
        Log.i(TAG, "👆 Registering map click callback for event: $eventId")
        mapClickCallbacks[eventId] = callback
        Log.d(TAG, "Map click callback registered, totalCallbacks=${mapClickCallbacks.size}")
    }

    // Store map click registration callbacks (for direct wrapper registration)
    private val mapClickRegistrationCallbacks = mutableMapOf<String, (() -> Unit) -> Unit>()

    /**
     * Register a callback that Swift wrapper will invoke to register map click callbacks.
     * This enables direct callback storage in wrapper (no registry lookup).
     */
    fun setMapClickRegistrationCallback(
        eventId: String,
        callback: ((() -> Unit) -> Unit),
    ) {
        Log.d(TAG, "Setting map click registration callback for event: $eventId")
        mapClickRegistrationCallbacks[eventId] = callback
    }

    /**
     * Request map click callback registration on wrapper.
     * Invokes the registered callback to set the click listener directly on the wrapper.
     */
    fun requestMapClickCallbackRegistration(
        eventId: String,
        clickCallback: () -> Unit,
    ) {
        val registrationCallback = mapClickRegistrationCallbacks[eventId]
        if (registrationCallback != null) {
            Log.i(TAG, "🚀 Triggering map click callback registration for event: $eventId")
            platform.darwin.dispatch_async(platform.darwin.dispatch_get_main_queue()) {
                registrationCallback.invoke(clickCallback)
            }
        } else {
            Log.v(TAG, "No registration callback for event: $eventId (falling back to legacy registry)")
            // Fallback to legacy registry-based approach
            setMapClickCallback(eventId, clickCallback)
        }
    }

    // Store visible region that Swift can update
    private val visibleRegions = mutableMapOf<String, BoundingBox>()

    // Store zoom levels that Swift can update
    private val minZoomLevels = mutableMapOf<String, Double>()

    // Store camera idle listeners
    private val cameraIdleListeners = mutableMapOf<String, () -> Unit>()

    // Store camera positions and zoom for StateFlow updates
    private val cameraPositions = mutableMapOf<String, Pair<Double, Double>>()
    private val cameraZooms = mutableMapOf<String, Double>()

    // Store camera animation callbacks (for async completion signaling)
    private val cameraAnimationCallbacks = mutableMapOf<String, MapCameraCallback>()

    // Store map click coordinate listeners (for tap with coordinates)
    private val mapClickCoordinateListeners = mutableMapOf<String, (Double, Double) -> Unit>()

    // Store map ready callbacks (invoked after style loads)
    private val onMapReadyCallbacks = mutableMapOf<String, MutableList<() -> Unit>>()

    // Track style loaded state
    private val styleLoadedStates = mutableMapOf<String, Boolean>()

    /**
     * Update visible region from Swift.
     * Called by Swift when map region changes.
     */
    fun updateVisibleRegion(
        eventId: String,
        bbox: BoundingBox,
    ) {
        visibleRegions[eventId] = bbox
    }

    /**
     * Get visible region from wrapper.
     * Returns the current visible bounds of the map.
     */
    fun getVisibleRegion(eventId: String): BoundingBox? = visibleRegions[eventId]

    /**
     * Update min zoom level from Swift.
     */
    fun updateMinZoom(
        eventId: String,
        minZoom: Double,
    ) {
        minZoomLevels[eventId] = minZoom
    }

    /**
     * Get min zoom level for event.
     */
    fun getMinZoom(eventId: String): Double = minZoomLevels[eventId] ?: 0.0

    /**
     * Set min zoom command (Swift will execute).
     */
    fun setMinZoomCommand(
        eventId: String,
        minZoom: Double,
    ) {
        Log.d(TAG, "Setting min zoom command: $minZoom for event: $eventId")
        setPendingCameraCommand(eventId, CameraCommand.SetMinZoom(minZoom))
    }

    /**
     * Set max zoom command (Swift will execute).
     */
    fun setMaxZoomCommand(
        eventId: String,
        maxZoom: Double,
    ) {
        Log.d(TAG, "Setting max zoom command: $maxZoom for event: $eventId")
        setPendingCameraCommand(eventId, CameraCommand.SetMaxZoom(maxZoom))
    }

    /**
     * Set camera idle listener for event.
     */
    fun setCameraIdleListener(
        eventId: String,
        callback: () -> Unit,
    ) {
        Log.d(TAG, "Setting camera idle listener for event: $eventId")
        cameraIdleListeners[eventId] = callback
    }

    /**
     * Invoke camera idle listener (called from Swift).
     */
    fun invokeCameraIdleListener(eventId: String) {
        val callback = cameraIdleListeners[eventId]
        if (callback != null) {
            Log.v(TAG, "Invoking camera idle callback for event: $eventId")
            callback.invoke()
        }
    }

    /**
     * Set map click coordinate listener (invoked with tap coordinates).
     */
    fun setMapClickCoordinateListener(
        eventId: String,
        listener: (Double, Double) -> Unit,
    ) {
        Log.d(TAG, "Setting map click coordinate listener for event: $eventId")
        mapClickCoordinateListeners[eventId] = listener
    }

    /**
     * Clear map click coordinate listener.
     */
    fun clearMapClickCoordinateListener(eventId: String) {
        mapClickCoordinateListeners.remove(eventId)
    }

    /**
     * Invoke map click coordinate listener (called from Swift with tap coordinates).
     */
    fun invokeMapClickCoordinateListener(
        eventId: String,
        latitude: Double,
        longitude: Double,
    ) {
        val listener = mapClickCoordinateListeners[eventId]
        if (listener != null) {
            Log.d(TAG, "Invoking map click coordinate listener: ($latitude, $longitude)")
            listener.invoke(latitude, longitude)
        }
    }

    /**
     * Draw debug bounding box overlay (for testing constraint bounds).
     */
    @Suppress("UnusedParameter")
    fun drawDebugBbox(
        eventId: String,
        bbox: BoundingBox,
    ) {
        Log.d(TAG, "Drawing debug bbox for event: $eventId")
        val wrapper = getWrapper(eventId)
        if (wrapper != null) {
            platform.darwin.dispatch_async(platform.darwin.dispatch_get_main_queue()) {
                // Swift wrapper will handle drawing via IOSMapBridge or direct call
                Log.i(TAG, "Debug bbox draw dispatched for: $eventId")
                // Note: Swift wrapper already has drawOverrideBbox() method at line 438
                // bbox parameter available for future use in direct Swift call
            }
        }
    }

    /**
     * Update camera position from Swift (for StateFlow reactive updates).
     * Called when map camera moves.
     */
    fun updateCameraPosition(
        eventId: String,
        latitude: Double,
        longitude: Double,
    ) {
        cameraPositions[eventId] = Pair(latitude, longitude)
        Log.v(TAG, "Camera position updated: ($latitude, $longitude)")
    }

    /**
     * Update camera zoom from Swift (for StateFlow reactive updates).
     * Called when map zoom changes.
     */
    fun updateCameraZoom(
        eventId: String,
        zoom: Double,
    ) {
        cameraZooms[eventId] = zoom
        Log.v(TAG, "Camera zoom updated: $zoom")
    }

    /**
     * Get camera position for event.
     */
    fun getCameraPosition(eventId: String): Pair<Double, Double>? = cameraPositions[eventId]

    /**
     * Get camera zoom for event.
     */
    fun getCameraZoom(eventId: String): Double? = cameraZooms[eventId]

    /**
     * Store camera animation callback for async completion.
     */
    fun setCameraAnimationCallback(
        callbackId: String,
        callback: MapCameraCallback,
    ) {
        cameraAnimationCallbacks[callbackId] = callback
    }

    /**
     * Invoke camera animation callback (called from Swift when animation completes).
     */
    fun invokeCameraAnimationCallback(
        callbackId: String,
        success: Boolean,
    ) {
        val callback = cameraAnimationCallbacks[callbackId]
        if (callback != null) {
            if (success) {
                callback.onFinish()
            } else {
                callback.onCancel()
            }
            cameraAnimationCallbacks.remove(callbackId)
        }
    }

    /**
     * Get and invoke map click callback for an event.
     * Swift calls this when map is tapped.
     * Returns true if callback was found and invoked.
     */
    fun invokeMapClickCallback(eventId: String): Boolean {
        Log.i(TAG, "👆 invokeMapClickCallback called for event: $eventId")
        val callback = mapClickCallbacks[eventId]
        if (callback != null) {
            Log.i(TAG, "✅ Map click callback found, invoking for event: $eventId")
            try {
                callback.invoke()
                Log.i(TAG, "✅ Map click callback invoked successfully for event: $eventId")
                return true
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error invoking map click callback for event: $eventId", throwable = e)
                return false
            }
        }
        Log.w(TAG, "❌ No map click callback registered for event: $eventId (available: ${mapClickCallbacks.keys})")
        return false
    }

    /**
     * Clear map click callback for an event.
     */
    fun clearMapClickCallback(eventId: String) {
        Log.v(TAG, "Clearing map click callback for event: $eventId")
        mapClickCallbacks.remove(eventId)
    }

    /**
     * Register a callback that Swift wrapper will invoke when immediate render is requested.
     * This enables direct dispatch pattern (no polling).
     */
    fun setRenderCallback(
        eventId: String,
        callback: () -> Unit,
    ) {
        Log.d(TAG, "Setting render callback for event: $eventId")
        renderCallbacks[eventId] = callback
    }

    /**
     * Request immediate render of pending polygons.
     * Invokes the registered render callback if available.
     * Called from Kotlin when polygons are updated.
     */
    fun requestImmediateRender(eventId: String) {
        val callback = renderCallbacks[eventId]
        if (callback != null) {
            Log.i(TAG, "🚀 Triggering immediate render callback for event: $eventId")
            // Dispatch to main queue to ensure UI thread execution
            platform.darwin.dispatch_async(platform.darwin.dispatch_get_main_queue()) {
                callback.invoke()
            }
        } else {
            Log.v(TAG, "No render callback registered for event: $eventId (will fall back to polling)")
        }
    }

    /**
     * Add a callback to be invoked when map style is loaded and ready.
     * If style is already loaded, callback is invoked immediately.
     */
    fun addOnMapReadyCallback(
        eventId: String,
        callback: () -> Unit,
    ) {
        Log.d(TAG, "Adding map ready callback for event: $eventId")
        val callbacks = onMapReadyCallbacks.getOrPut(eventId) { mutableListOf() }
        callbacks.add(callback)
    }

    /**
     * Mark style as loaded for an event.
     * Called from Swift after didFinishLoading style.
     */
    fun setStyleLoaded(
        eventId: String,
        loaded: Boolean,
    ) {
        Log.i(TAG, "Style loaded state updated: $loaded for event: $eventId")
        styleLoadedStates[eventId] = loaded
    }

    /**
     * Check if style is loaded for an event.
     */
    fun isStyleLoaded(eventId: String): Boolean = styleLoadedStates[eventId] ?: false

    /**
     * Invoke all registered map ready callbacks for an event.
     * Called from Swift after style loads.
     */
    fun invokeMapReadyCallbacks(eventId: String) {
        val callbacks = onMapReadyCallbacks[eventId]
        if (callbacks == null || callbacks.isEmpty()) {
            Log.v(TAG, "No map ready callbacks registered for event: $eventId")
            return
        }

        Log.i(TAG, "Invoking ${callbacks.size} map ready callback(s) for event: $eventId")
        callbacks.forEach { callback ->
            try {
                callback.invoke()
            } catch (e: Exception) {
                Log.e(TAG, "Error invoking map ready callback for event: $eventId", throwable = e)
            }
        }

        // Clear callbacks after invoking (one-time use)
        onMapReadyCallbacks.remove(eventId)
        Log.d(TAG, "Map ready callbacks cleared for event: $eventId")
    }

    /**
     * Clear all registered wrappers and pending data.
     * Useful for cleanup during app termination or testing.
     */
    fun clear() {
        Log.d(TAG, "Clearing all registered wrappers, pending data, and callbacks")
        wrappers.clear()
        pendingPolygons.clear()
        pendingCameraCommands.clear()
        mapClickCallbacks.clear()
        mapClickRegistrationCallbacks.clear()
        mapClickCoordinateListeners.clear()
        renderCallbacks.clear()
        cameraCallbacks.clear()
        visibleRegions.clear()
        minZoomLevels.clear()
        cameraIdleListeners.clear()
        cameraPositions.clear()
        cameraZooms.clear()
        cameraAnimationCallbacks.clear()
        onMapReadyCallbacks.clear()
        styleLoadedStates.clear()
    }
}
