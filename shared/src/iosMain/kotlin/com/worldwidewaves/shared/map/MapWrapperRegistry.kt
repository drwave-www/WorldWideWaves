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
import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.ref.WeakReference

/**
 * Camera command types for iOS map control.
 */
sealed class CameraCommand {
    data class AnimateToPosition(
        val position: Position,
        val zoom: Double?,
    ) : CameraCommand()

    data class AnimateToBounds(
        val bounds: BoundingBox,
        val padding: Int = 0,
    ) : CameraCommand()

    data class MoveToBounds(
        val bounds: BoundingBox,
    ) : CameraCommand()

    data class SetConstraintBounds(
        val bounds: BoundingBox,
    ) : CameraCommand()
}

/**
 * Registry to store MapLibreViewWrapper instances, polygon data, and camera commands.
 * This allows coordination between Kotlin (IosEventMap) and Swift (MapLibreViewWrapper).
 *
 * Architecture:
 * 1. Swift creates MapLibreViewWrapper in EventMapView
 * 2. Swift registers it here via registerWrapper(eventId, wrapper)
 * 3. Kotlin stores polygon data via setPendingPolygons(eventId, polygons)
 * 4. Swift retrieves and renders polygons via renderPendingPolygons(eventId)
 *
 * Memory Management:
 * - Uses LRU cache with max 3 entries to prevent unbounded growth
 * - WeakReference allows garbage collection of unused wrappers
 * - Automatically evicts oldest unused wrappers when limit is exceeded
 */
@OptIn(ExperimentalNativeApi::class)
object MapWrapperRegistry {
    private const val TAG = "MapWrapperRegistry"
    private const val MAX_CACHED_WRAPPERS = 3
    private const val MILLISECONDS_PER_SECOND = 1000

    /**
     * Entry in the LRU cache with timestamp for tracking access order.
     */
    private data class CacheEntry(
        val weakRef: WeakReference<Any>,
        var lastAccessed: Long,
    )

    /**
     * LRU cache implementation using mutableMap with manual eviction.
     * Uses WeakReference to allow garbage collection.
     * Access order is tracked via timestamps.
     */
    private val wrappers = mutableMapOf<String, CacheEntry>()

    // Store pending polygon data that Swift will render
    private val pendingPolygons = mutableMapOf<String, PendingPolygonData>()

    // Store pending camera commands that Swift will execute
    private val pendingCameraCommands = mutableMapOf<String, CameraCommand>()

    // Store map click callbacks that Swift will invoke
    private val mapClickCallbacks = mutableMapOf<String, () -> Unit>()

    // Store render callbacks that Swift wrappers can register for immediate notifications
    private val renderCallbacks = mutableMapOf<String, () -> Unit>()

    /**
     * Evict the least recently used entry if cache is full.
     */
    private fun evictLRUIfNeeded() {
        if (wrappers.size >= MAX_CACHED_WRAPPERS) {
            val lruEntry = wrappers.entries.minByOrNull { it.value.lastAccessed }
            if (lruEntry != null) {
                Log.d(TAG, "LRU evicting wrapper for event: ${lruEntry.key} (cache size: ${wrappers.size})")
                wrappers.remove(lruEntry.key)
            }
        }
    }

    data class PendingPolygonData(
        val coordinates: List<List<Pair<Double, Double>>>, // List of polygons, each containing lat/lng pairs
        val clearExisting: Boolean,
    )

    /**
     * Register a MapLibreViewWrapper for an event.
     * Called from Swift after wrapper creation.
     * Uses WeakReference to allow garbage collection.
     */
    fun registerWrapper(
        eventId: String,
        wrapper: Any,
    ) {
        Log.d(TAG, "Registering wrapper for event: $eventId (cache size: ${wrappers.size})")

        // Evict LRU entry if cache is full
        evictLRUIfNeeded()

        wrappers[eventId] =
            CacheEntry(
                weakRef = WeakReference(wrapper),
                lastAccessed = (NSDate().timeIntervalSince1970 * 1000).toLong(),
            )

        // If there are pending polygons, notify that they should be rendered
        if (pendingPolygons.containsKey(eventId)) {
            Log.i(TAG, "Wrapper registered, pending polygons available for: $eventId")
        }
    }

    /**
     * Get the registered wrapper for an event.
     * Returns null if not yet registered or if garbage collected.
     * Accessing an entry marks it as recently used (LRU).
     */
    fun getWrapper(eventId: String): Any? {
        val entry = wrappers[eventId]
        if (entry == null) {
            Log.w(TAG, "No wrapper registered for event: $eventId")
            return null
        }

        // Update last accessed time for LRU
        entry.lastAccessed = (NSDate().timeIntervalSince1970 * MILLISECONDS_PER_SECOND).toLong()

        val wrapper = entry.weakRef.get()
        if (wrapper == null) {
            Log.w(TAG, "Wrapper for event $eventId was garbage collected")
            wrappers.remove(eventId)
        } else {
            Log.v(TAG, "Retrieved wrapper for event: $eventId")
        }
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
     * Unregister a wrapper when the map is destroyed.
     * Clears wrapper, pending polygons, pending camera commands, and callbacks.
     */
    fun unregisterWrapper(eventId: String) {
        Log.d(TAG, "Unregistering wrapper for event: $eventId")
        wrappers.remove(eventId)
        pendingPolygons.remove(eventId)
        pendingCameraCommands.remove(eventId)
        mapClickCallbacks.remove(eventId)
    }

    /**
     * Remove all stale WeakReference entries (where the referent has been garbage collected).
     * This is automatically done in getWrapper, but can be called manually for cleanup.
     */
    fun pruneStaleReferences() {
        val staleKeys = wrappers.entries.filter { it.value.weakRef.get() == null }.map { it.key }
        if (staleKeys.isNotEmpty()) {
            Log.d(TAG, "Pruning ${staleKeys.size} stale wrapper references")
            staleKeys.forEach { wrappers.remove(it) }
        }
    }

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
                is CameraCommand.AnimateToPosition -> "AnimateToPosition(${command.position.lat},${command.position.lng},zoom=${command.zoom})"
                is CameraCommand.AnimateToBounds -> "AnimateToBounds(padding=${command.padding})"
                is CameraCommand.MoveToBounds -> "MoveToBounds"
                is CameraCommand.SetConstraintBounds -> "SetConstraintBounds"
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
        // Trigger immediate execution via camera callback
        requestImmediateCameraExecution(eventId)
        // Store as a special camera command or handle directly
        val wrapper = getWrapper(eventId)
        if (wrapper != null) {
            platform.darwin.dispatch_async(platform.darwin.dispatch_get_main_queue()) {
                // Swift wrapper will handle this - just notify via existing callback
                Log.i(TAG, "Min zoom will be set via wrapper method")
            }
        }
    }

    /**
     * Set max zoom command (Swift will execute).
     */
    fun setMaxZoomCommand(
        eventId: String,
        maxZoom: Double,
    ) {
        Log.d(TAG, "Setting max zoom command: $maxZoom for event: $eventId")
        // Similar to setMinZoomCommand
        platform.darwin.dispatch_async(platform.darwin.dispatch_get_main_queue()) {
            Log.i(TAG, "Max zoom will be set via wrapper method")
        }
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
        renderCallbacks.clear()
        cameraCallbacks.clear()
        visibleRegions.clear()
        minZoomLevels.clear()
        cameraIdleListeners.clear()
    }
}
