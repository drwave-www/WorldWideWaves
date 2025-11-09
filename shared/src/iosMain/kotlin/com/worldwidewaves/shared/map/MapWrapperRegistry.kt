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
        val constraintBounds: BoundingBox,
        val originalEventBounds: BoundingBox?,
        val applyZoomSafetyMargin: Boolean,
    ) : CameraCommand()

    data class SetMinZoom(
        val minZoom: Double,
    ) : CameraCommand()

    data class SetMaxZoom(
        val maxZoom: Double,
    ) : CameraCommand()

    data class SetAttributionMargins(
        val left: Int,
        val top: Int,
        val right: Int,
        val bottom: Int,
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
 * - LRU eviction (MAX_WRAPPERS=10): Prevents unbounded growth if cleanup fails
 */
@OptIn(ExperimentalNativeApi::class)
object MapWrapperRegistry {
    private const val TAG = "MapWrapperRegistry"

    /**
     * Maximum number of wrappers to keep in memory simultaneously.
     * When exceeded, least recently used wrappers are evicted.
     * This prevents unbounded memory growth if DisposableEffect cleanup doesn't execute.
     */
    private const val MAX_WRAPPERS = 10

    /**
     * Strong references to wrappers.
     * Wrappers are kept alive for entire screen session.
     * Explicit cleanup via unregisterWrapper() is required on screen exit.
     *
     * Changed from WeakReference to strong references to prevent premature GC.
     * The wrapper MUST survive the entire screen session to handle dynamic updates.
     *
     * LRU eviction policy: When MAX_WRAPPERS is exceeded, oldest wrapper is evicted.
     */
    private val wrappers = mutableMapOf<String, Any>()

    /**
     * Track wrapper access order for LRU eviction.
     * Most recently accessed wrapper is at the end of the list.
     */
    private val wrapperAccessOrder = mutableListOf<String>()

    // Store pending polygon data that Swift will render
    private val pendingPolygons = mutableMapOf<String, PendingPolygonData>()

    // Store pending bbox draw requests that Swift will render
    private val pendingBboxDraws = mutableMapOf<String, BoundingBox>()

    // Store pending camera commands that Swift will execute
    // Configuration commands (bounds/zoom) are stored separately and applied immediately
    // Animation commands (AnimateToPosition, AnimateToBounds) use single slot (latest wins)
    private val pendingAnimationCommands = mutableMapOf<String, CameraCommand>()
    private val pendingConfigCommands = mutableMapOf<String, MutableList<CameraCommand>>()

    // Store map click callbacks that Swift will invoke
    private val mapClickCallbacks = mutableMapOf<String, () -> Unit>()

    // Store render callbacks that Swift wrappers can register for immediate notifications
    private val renderCallbacks = mutableMapOf<String, () -> Unit>()

    data class PendingPolygonData(
        val coordinates: List<List<Pair<Double, Double>>>, // List of polygons, each containing lat/lng pairs
        val clearExisting: Boolean,
    )

    /**
     * Register a MapLibreViewWrapper for an event.
     * Called from Swift after wrapper creation.
     * Uses STRONG reference - wrapper survives entire screen session.
     * MUST call unregisterWrapper() on screen exit to prevent memory leaks.
     *
     * LRU Eviction: If MAX_WRAPPERS is exceeded, evicts least recently used wrapper.
     */
    fun registerWrapper(
        eventId: String,
        wrapper: Any,
    ) {
        Log.d(TAG, "Registering wrapper for event: $eventId (total wrappers: ${wrappers.size})")

        // Check if we need to evict LRU wrapper
        if (wrappers.size >= MAX_WRAPPERS && !wrappers.containsKey(eventId)) {
            val lruEventId = wrapperAccessOrder.firstOrNull()
            if (lruEventId != null) {
                Log.w(TAG, "MAX_WRAPPERS ($MAX_WRAPPERS) exceeded, evicting LRU wrapper: $lruEventId")
                unregisterWrapper(lruEventId)
            }
        }

        // Remove from access order if already exists (will re-add at end)
        wrapperAccessOrder.remove(eventId)

        // Store strong reference
        wrappers[eventId] = wrapper

        // Add to end of access order (most recent)
        wrapperAccessOrder.add(eventId)

        Log.i(TAG, "Wrapper registered with STRONG reference for: $eventId")

        // If there are pending polygons, notify that they should be rendered
        if (pendingPolygons.containsKey(eventId)) {
            Log.i(TAG, "Wrapper registered, pending polygons available for: $eventId")
        }
    }

    /**
     * Get the registered wrapper for an event.
     * Returns null if not yet registered.
     * With strong references, wrapper is guaranteed to exist until unregisterWrapper() is called.
     *
     * Updates LRU access order (moves wrapper to end of list as most recently used).
     */
    fun getWrapper(eventId: String): Any? {
        val wrapper = wrappers[eventId]
        if (wrapper == null) {
            Log.w(TAG, "No wrapper registered for event: $eventId")
            return null
        }

        // Update LRU access order (move to end = most recently used)
        wrapperAccessOrder.remove(eventId)
        wrapperAccessOrder.add(eventId)

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
            "[WAVE] Storing ${coordinates.size} pending polygons for event: $eventId " +
                "($totalPoints total points, clearExisting=$clearExisting)",
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
        Log.i(TAG, "Unregistering wrapper and cleaning up for event: $eventId")

        // Remove wrapper (releases strong reference)
        wrappers.remove(eventId)

        // Remove from LRU access order
        wrapperAccessOrder.remove(eventId)

        // Clean up all associated data and callbacks
        pendingPolygons.remove(eventId)
        pendingConfigCommands.remove(eventId)
        pendingAnimationCommands.remove(eventId)
        mapClickCallbacks.remove(eventId)
        mapClickRegistrationCallbacks.remove(eventId)
        renderCallbacks.remove(eventId)
        cameraCallbacks.remove(eventId)
        visibleRegions.remove(eventId)
        minZoomLevels.remove(eventId)
        mapWidths.remove(eventId)
        mapHeights.remove(eventId)
        cameraIdleListeners.remove(eventId)
        onMapReadyCallbacks.remove(eventId)
        styleLoadedStates.remove(eventId)
        locationComponentCallbacks.remove(eventId)
        setUserPositionCallbacks.remove(eventId)
        pendingLocationComponentStates.remove(eventId)
        pendingUserPositions.remove(eventId)
        setGesturesEnabledCallbacks.remove(eventId)
        pendingGesturesStates.remove(eventId)

        Log.i(TAG, "Wrapper unregistered and cleanup complete for: $eventId")
    }

    // REMOVED: pruneStaleReferences() - no longer needed with strong references

    /**
     * Store a camera command to be executed.
     * Called from Kotlin when camera needs to be controlled.
     *
     * Architecture:
     * - Configuration commands (SetConstraintBounds, SetMinZoom, SetMaxZoom) are queued
     *   and executed in order. These must all apply to configure map constraints correctly.
     * - Animation commands (AnimateToPosition, AnimateToBounds, MoveToBounds) use single slot
     *   where the latest command replaces any pending animation (cancel previous, animate to new target).
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
                is CameraCommand.SetAttributionMargins ->
                    "SetAttributionMargins(${command.left},${command.top},${command.right},${command.bottom})"
            }
        Log.i(TAG, "[CAMERA] Storing camera command for event: $eventId -> $commandDetails")

        // Separate configuration commands (must all execute) from animation commands (latest wins)
        when (command) {
            is CameraCommand.SetConstraintBounds,
            is CameraCommand.SetMinZoom,
            is CameraCommand.SetMaxZoom,
            is CameraCommand.SetAttributionMargins,
            -> {
                // Queue configuration commands (all must execute in order)
                val queue = pendingConfigCommands.getOrPut(eventId) { mutableListOf() }
                queue.add(command)
                Log.d(TAG, "Config command queued, queue size=${queue.size}")
            }
            is CameraCommand.AnimateToPosition,
            is CameraCommand.AnimateToBounds,
            is CameraCommand.MoveToBounds,
            -> {
                // Single slot for animations (latest wins, cancel previous)
                pendingAnimationCommands[eventId] = command
                Log.d(TAG, "Animation command stored (overwrites previous animation)")
            }
        }

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
            Log.i(TAG, "[CAMERA] Triggering immediate camera execution for event: $eventId")
            platform.darwin.dispatch_async(platform.darwin.dispatch_get_main_queue()) {
                callback.invoke()
            }
        } else {
            Log.v(TAG, "No camera callback registered for event: $eventId (will fall back to polling)")
        }
    }

    /**
     * Get next pending camera command for an event.
     * Swift calls this to retrieve camera commands that need to be executed.
     * Returns null if no pending commands.
     *
     * Priority: Configuration commands execute first (in queue order), then animation commands.
     * This ensures map constraints are set before animations run.
     */
    fun getPendingCameraCommand(eventId: String): CameraCommand? {
        // Check configuration queue first (must execute before animations)
        val configQueue = pendingConfigCommands[eventId]
        if (configQueue != null && configQueue.isNotEmpty()) {
            return configQueue.first() // Return oldest config command (FIFO)
        }

        // Then check animation slot
        return pendingAnimationCommands[eventId]
    }

    /**
     * Check if there is a pending camera command for an event.
     */
    fun hasPendingCameraCommand(eventId: String): Boolean {
        val hasConfig = pendingConfigCommands[eventId]?.isNotEmpty() == true
        val hasAnimation = pendingAnimationCommands.containsKey(eventId)
        return hasConfig || hasAnimation
    }

    /**
     * Clear pending camera command after it's been executed.
     * Swift calls this after successfully executing the command.
     *
     * Removes the command that was just returned by getPendingCameraCommand().
     * Configuration commands are removed from queue (FIFO), animation commands clear the slot.
     */
    fun clearPendingCameraCommand(eventId: String) {
        Log.v(TAG, "Clearing pending camera command for event: $eventId")

        // Try to clear from config queue first (FIFO removal)
        val configQueue = pendingConfigCommands[eventId]
        if (configQueue != null && configQueue.isNotEmpty()) {
            val removed = configQueue.removeAt(0)
            Log.v(TAG, "Removed config command: ${removed::class.simpleName}, remaining=${configQueue.size}")
            if (configQueue.isEmpty()) {
                pendingConfigCommands.remove(eventId)
            }
            return
        }

        // Then try animation slot
        if (pendingAnimationCommands.containsKey(eventId)) {
            pendingAnimationCommands.remove(eventId)
            Log.v(TAG, "Cleared animation command")
        }
    }

    /**
     * Set map click callback for an event.
     * Swift will invoke this when the map is tapped.
     */
    fun setMapClickCallback(
        eventId: String,
        callback: () -> Unit,
    ) {
        Log.i(TAG, "Registering map click callback for event: $eventId")
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
            Log.i(TAG, "Triggering map click callback registration for event: $eventId")
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

    // Store map dimensions that Swift can update
    private val mapWidths = mutableMapOf<String, Double>()
    private val mapHeights = mutableMapOf<String, Double>()

    // Store camera idle listeners (support multiple listeners per event like Android)
    private val cameraIdleListeners = mutableMapOf<String, MutableList<() -> Unit>>()

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
     * Get min zoom level for event (cached from Swift).
     */
    fun getMinZoom(eventId: String): Double = minZoomLevels[eventId] ?: 0.0

    /**
     * Update map width from Swift.
     * Called by Swift when map view bounds change.
     */
    fun updateMapWidth(
        eventId: String,
        width: Double,
    ) {
        mapWidths[eventId] = width
        Log.v(TAG, "Map width updated: $width for event: $eventId")
    }

    /**
     * Update map height from Swift.
     * Called by Swift when map view bounds change.
     */
    fun updateMapHeight(
        eventId: String,
        height: Double,
    ) {
        mapHeights[eventId] = height
        Log.v(TAG, "Map height updated: $height for event: $eventId")
    }

    /**
     * Get map width for event.
     * Returns actual map view width from Swift wrapper.
     */
    fun getMapWidth(eventId: String): Double = mapWidths[eventId] ?: 0.0

    /**
     * Get map height for event.
     * Returns actual map view height from Swift wrapper.
     */
    fun getMapHeight(eventId: String): Double = mapHeights[eventId] ?: 0.0

    /**
     * Get actual min zoom from map view (bypasses cache to prevent race condition).
     * This directly queries the Swift wrapper's map view for the current minimum zoom level.
     * Called from IosMapLibreAdapter.getMinZoomLevel() to get real-time value.
     *
     * NOTE: This method should be called from Swift via IOSMapBridge to populate
     * the actualMinZoomLevels map with real-time values before querying.
     */
    private val actualMinZoomLevels = mutableMapOf<String, Double>()

    fun getActualMinZoom(eventId: String): Double {
        val wrapper =
            getWrapper(eventId) ?: run {
                Log.w(TAG, "getActualMinZoom: wrapper is null for event: $eventId")
                return 0.0
            }

        // Call wrapper's getMinZoom() which queries mapView.minimumZoomLevel directly
        return actualMinZoomLevels[eventId] ?: run {
            Log.v(TAG, "getActualMinZoom: no cached value for $eventId, requesting from Swift")
            // Will be populated by Swift calling updateActualMinZoom()
            0.0
        }
    }

    /**
     * Update actual min zoom from Swift (called synchronously before getActualMinZoom).
     */
    fun updateActualMinZoom(
        eventId: String,
        actualMinZoom: Double,
    ) {
        actualMinZoomLevels[eventId] = actualMinZoom
        Log.v(TAG, "updateActualMinZoom: $eventId -> $actualMinZoom")
    }

    /**
     * Synchronously fetch actualMinZoom from wrapper and update cache.
     * This is called before getActualMinZoom() to ensure fresh value.
     *
     * NOTE: This triggers the registered callback which will call updateActualMinZoom().
     */
    fun syncActualMinZoomFromWrapper(eventId: String) {
        val wrapper = getWrapper(eventId)
        if (wrapper != null) {
            // Trigger getActualMinZoom callback (registered by Swift wrapper)
            val callback = getActualMinZoomCallbacks[eventId]
            if (callback != null) {
                callback.invoke()
                Log.v(TAG, "syncActualMinZoomFromWrapper: triggered callback for $eventId")
            } else {
                Log.w(TAG, "syncActualMinZoomFromWrapper: no callback registered for $eventId")
            }
        }
    }

    private val getActualMinZoomCallbacks = mutableMapOf<String, () -> Unit>()

    /**
     * Register callback for getActualMinZoom requests.
     * Swift wrapper registers this to provide real-time min zoom values.
     */
    fun setGetActualMinZoomCallback(
        eventId: String,
        callback: () -> Unit,
    ) {
        getActualMinZoomCallbacks[eventId] = callback
        Log.d(TAG, "Registered getActualMinZoom callback for event: $eventId")
    }

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
     * Set attribution margins command (Swift will execute).
     */
    fun setAttributionMarginsCommand(
        eventId: String,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
    ) {
        Log.d(TAG, "Setting attribution margins command: ($left,$top,$right,$bottom) for event: $eventId")
        setPendingCameraCommand(eventId, CameraCommand.SetAttributionMargins(left, top, right, bottom))
    }

    /**
     * Set camera idle listener for event.
     * Supports multiple listeners like Android MapLibre API.
     */
    fun setCameraIdleListener(
        eventId: String,
        callback: () -> Unit,
    ) {
        Log.d(TAG, "Adding camera idle listener for event: $eventId")
        val listeners = cameraIdleListeners.getOrPut(eventId) { mutableListOf() }
        listeners.add(callback)
        Log.d(TAG, "Camera idle listeners for $eventId: ${listeners.size}")
    }

    /**
     * Invoke all camera idle listeners (called from Swift).
     * Executes all registered callbacks in order.
     */
    fun invokeCameraIdleListener(eventId: String) {
        val listeners = cameraIdleListeners[eventId]
        if (listeners == null || listeners.isEmpty()) {
            return
        }

        Log.v(TAG, "Invoking ${listeners.size} camera idle callback(s) for event: $eventId")
        // Create a copy to avoid ConcurrentModificationException if callbacks add more listeners
        listeners.toList().forEach { callback ->
            try {
                callback.invoke()
            } catch (e: Exception) {
                Log.e(TAG, "Error invoking camera idle callback for event: $eventId", throwable = e)
            }
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
     * Stores bbox as pending, to be rendered by Swift via IOSMapBridge.
     */
    fun drawDebugBbox(
        eventId: String,
        bbox: BoundingBox,
    ) {
        Log.d(TAG, "Storing pending bbox for event: $eventId")
        pendingBboxDraws[eventId] = bbox
    }

    /**
     * Check if there is a pending bbox draw for this event.
     * Called from Swift IOSMapBridge to poll for bbox requests.
     */
    fun hasPendingBboxDraw(eventId: String): Boolean = pendingBboxDraws.containsKey(eventId)

    /**
     * Get and clear pending bbox draw.
     * Called from Swift IOSMapBridge after rendering.
     */
    fun getPendingBboxDraw(eventId: String): BoundingBox? = pendingBboxDraws.remove(eventId)

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
    @Suppress("ReturnCount") // Early returns for error handling - clearer than nested conditionals
    fun invokeMapClickCallback(eventId: String): Boolean {
        Log.i(TAG, "invokeMapClickCallback called for event: $eventId")
        val callback = mapClickCallbacks[eventId]
        if (callback != null) {
            Log.i(TAG, "Map click callback found, invoking for event: $eventId")
            try {
                callback.invoke()
                Log.i(TAG, "Map click callback invoked successfully for event: $eventId")
                return true
            } catch (e: Exception) {
                Log.e(TAG, "ERROR: invoking map click callback for event: $eventId", throwable = e)
                return false
            }
        }
        Log.w(TAG, "WARNING: No map click callback registered for event: $eventId (available: ${mapClickCallbacks.keys})")
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
     * Get the registered render callback for an event.
     * Used for synchronous polygon rendering.
     */
    fun getRenderCallback(eventId: String): (() -> Unit)? = renderCallbacks[eventId]

    // Store callbacks for location component control
    private val locationComponentCallbacks = mutableMapOf<String, (Boolean) -> Unit>()
    private val setUserPositionCallbacks = mutableMapOf<String, (Double, Double) -> Unit>()

    // Store callbacks for gesture control
    private val setGesturesEnabledCallbacks = mutableMapOf<String, (Boolean) -> Unit>()

    // Store pending user position (for race condition handling)
    private val pendingUserPositions = mutableMapOf<String, Pair<Double, Double>>()

    // Store pending gestures state (for race condition handling)
    private val pendingGesturesStates = mutableMapOf<String, Boolean>()

    /**
     * Register callback for enabling/disabling location component.
     * Swift wrapper registers this to receive location component enable/disable commands.
     */
    fun setLocationComponentCallback(
        eventId: String,
        callback: (Boolean) -> Unit,
    ) {
        locationComponentCallbacks[eventId] = callback
        Log.d(TAG, "Location component callback registered for event: $eventId")

        // Apply pending location component state if it was set before callback registered
        val pendingState = pendingLocationComponentStates[eventId]
        if (pendingState != null) {
            Log.i(TAG, "Applying pending location component state: $pendingState for event: $eventId")
            platform.darwin.dispatch_async(platform.darwin.dispatch_get_main_queue()) {
                callback.invoke(pendingState)
            }
            pendingLocationComponentStates.remove(eventId)
        }
    }

    /**
     * Register callback for setting user position.
     * Swift wrapper registers this to receive user position updates.
     */
    fun setUserPositionCallback(
        eventId: String,
        callback: (Double, Double) -> Unit,
    ) {
        setUserPositionCallbacks[eventId] = callback
        Log.d(TAG, "User position callback registered for event: $eventId")

        // Apply pending user position if it was set before callback registered
        val pendingPosition = pendingUserPositions[eventId]
        if (pendingPosition != null) {
            Log.i(TAG, "Applying pending user position: (${pendingPosition.first}, ${pendingPosition.second}) for event: $eventId")
            platform.darwin.dispatch_async(platform.darwin.dispatch_get_main_queue()) {
                callback.invoke(pendingPosition.first, pendingPosition.second)
            }
            pendingUserPositions.remove(eventId)
        }
    }

    /**
     * Check if user position callback is registered for an event.
     */
    fun hasUserPositionCallback(eventId: String): Boolean = setUserPositionCallbacks.containsKey(eventId)

    // Store pending location component state (for race condition handling)
    private val pendingLocationComponentStates = mutableMapOf<String, Boolean>()

    /**
     * Enable or disable location component on the map wrapper.
     */
    fun enableLocationComponentOnWrapper(
        eventId: String,
        enabled: Boolean,
    ) {
        val callback = locationComponentCallbacks[eventId]
        if (callback != null) {
            Log.d(TAG, "Invoking location component callback: $enabled for event: $eventId")
            platform.darwin.dispatch_async(platform.darwin.dispatch_get_main_queue()) {
                callback.invoke(enabled)
            }
            // Clear pending state after successful invocation
            pendingLocationComponentStates.remove(eventId)
        } else {
            Log.w(TAG, "No location component callback registered for event: $eventId - storing pending state")
            // Store pending state to apply when callback is registered
            pendingLocationComponentStates[eventId] = enabled
        }
    }

    /**
     * Update user position on the map wrapper.
     */
    fun setUserPositionOnWrapper(
        eventId: String,
        latitude: Double,
        longitude: Double,
    ) {
        Log.i(TAG, "[POSITION] setUserPositionOnWrapper called: ($latitude, $longitude) for event: $eventId")

        val callback = setUserPositionCallbacks[eventId]
        if (callback != null) {
            Log.d(TAG, "Dispatching position update to Swift via callback")
            platform.darwin.dispatch_async(platform.darwin.dispatch_get_main_queue()) {
                callback.invoke(latitude, longitude)
                Log.v(TAG, "Position callback invoked on main queue")
            }
            // Clear pending position after successful dispatch
            pendingUserPositions.remove(eventId)
        } else {
            Log.w(TAG, "WARNING: No user position callback registered for event: $eventId - storing latest position")
            // Store only the latest position (overwrites previous)
            pendingUserPositions[eventId] = Pair(latitude, longitude)
        }
    }

    /**
     * Register callback for enabling/disabling gestures.
     * Swift wrapper registers this to receive gesture enable/disable commands.
     */
    fun setGesturesEnabledCallback(
        eventId: String,
        callback: (Boolean) -> Unit,
    ) {
        setGesturesEnabledCallbacks[eventId] = callback
        Log.d(TAG, "Gestures callback registered for event: $eventId")

        // Apply pending gestures state if it was set before callback registered
        val pendingState = pendingGesturesStates[eventId]
        if (pendingState != null) {
            Log.i(TAG, "Applying pending gestures state: $pendingState for event: $eventId")
            platform.darwin.dispatch_async(platform.darwin.dispatch_get_main_queue()) {
                callback.invoke(pendingState)
            }
            pendingGesturesStates.remove(eventId)
        }
    }

    /**
     * Enable or disable gestures on the map wrapper.
     */
    fun setGesturesEnabledOnWrapper(
        eventId: String,
        enabled: Boolean,
    ) {
        val callback = setGesturesEnabledCallbacks[eventId]
        if (callback != null) {
            Log.d(TAG, "Invoking gestures callback: $enabled for event: $eventId")
            platform.darwin.dispatch_async(platform.darwin.dispatch_get_main_queue()) {
                callback.invoke(enabled)
            }
            // Clear pending state after successful invocation
            pendingGesturesStates.remove(eventId)
        } else {
            Log.w(TAG, "No gestures callback registered for event: $eventId - storing pending state")
            // Store pending state to apply when callback is registered
            pendingGesturesStates[eventId] = enabled
        }
    }

    /**
     * Request immediate render of pending polygons.
     * Invokes the registered render callback if available.
     * Called from Kotlin when polygons are updated.
     */
    fun requestImmediateRender(eventId: String) {
        val callback = renderCallbacks[eventId]
        if (callback != null) {
            Log.i(TAG, "[RENDER] Triggering immediate render callback for event: $eventId")
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
        // Create a copy to avoid ConcurrentModificationException if callbacks add more callbacks
        callbacks.toList().forEach { callback ->
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
        wrapperAccessOrder.clear()
        pendingPolygons.clear()
        pendingConfigCommands.clear()
        pendingAnimationCommands.clear()
        mapClickCallbacks.clear()
        mapClickRegistrationCallbacks.clear()
        mapClickCoordinateListeners.clear()
        renderCallbacks.clear()
        cameraCallbacks.clear()
        visibleRegions.clear()
        minZoomLevels.clear()
        mapWidths.clear()
        mapHeights.clear()
        cameraIdleListeners.clear()
        cameraPositions.clear()
        cameraZooms.clear()
        cameraAnimationCallbacks.clear()
        onMapReadyCallbacks.clear()
        styleLoadedStates.clear()
        locationComponentCallbacks.clear()
        setUserPositionCallbacks.clear()
        pendingLocationComponentStates.clear()
        pendingUserPositions.clear()
        setGesturesEnabledCallbacks.clear()
        pendingGesturesStates.clear()
    }
}
