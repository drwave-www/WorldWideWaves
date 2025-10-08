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
     * Clear all registered wrappers and pending data.
     * Useful for cleanup during app termination or testing.
     */
    fun clear() {
        Log.d(TAG, "Clearing all registered wrappers, pending data, and callbacks")
        wrappers.clear()
        pendingPolygons.clear()
        pendingCameraCommands.clear()
        mapClickCallbacks.clear()
    }
}
