package com.worldwidewaves.shared.map

/*
 * Copyright 2025 DrWave
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * https://www.apache.org/licenses/LICENSE-2.0
 */

import com.worldwidewaves.shared.utils.Log
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * EventMap-specific download manager for map resources.
 *
 * Provides unified download state management and orchestration logic:
 * - Availability checking
 * - Progress tracking (0-100%)
 * - Error handling
 * - Auto-download triggers
 * - Multi-map state tracking and cache management
 *
 * Platform-specific download mechanisms (Play Dynamic Features vs ODR)
 * are delegated to PlatformMapManager implementations.
 *
 * NOTE: This is distinct from viewmodels.MapDownloadCoordinator which serves
 * the ViewModel layer with adapter-based platform integration.
 */
class EventMapDownloadManager(
    private val platformMapManager: PlatformMapManager,
) {
    private companion object {
        private const val TAG = "EventMapDownloadManager"

        /**
         * Maximum number of download states to track simultaneously.
         * When exceeded, least recently used completed downloads are evicted.
         */
        private const val MAX_DOWNLOAD_STATES = 50
    }

    /**
     * Download state for a specific map
     */
    data class DownloadState(
        val isAvailable: Boolean = false,
        val isDownloading: Boolean = false,
        val progress: Int = 0,
        val error: String? = null,
    )

    private val _downloadStates = mutableMapOf<String, MutableStateFlow<DownloadState>>()
    private val accessOrder = mutableListOf<String>()

    /**
     * Get download state flow for a specific map.
     * Implements LRU eviction to prevent unbounded memory growth.
     */
    fun getDownloadState(mapId: String): StateFlow<DownloadState> {
        // Evict LRU completed downloads if we've hit the limit
        if (_downloadStates.size >= MAX_DOWNLOAD_STATES && !_downloadStates.containsKey(mapId)) {
            evictLRUCompletedDownload()
        }

        // Update access order
        accessOrder.remove(mapId)
        accessOrder.add(mapId)

        return _downloadStates
            .getOrPut(mapId) {
                MutableStateFlow(DownloadState())
            }.asStateFlow()
    }

    /**
     * Evict the least recently used completed download to free memory.
     * Only evicts downloads that are completed and available (not active or failed).
     */
    private fun evictLRUCompletedDownload() {
        // Find LRU completed download
        for (mapId in accessOrder) {
            val state = _downloadStates[mapId]?.value
            if (state != null && !state.isDownloading && state.error == null && state.isAvailable) {
                Log.d(TAG, "Evicting LRU completed download: $mapId (limit=$MAX_DOWNLOAD_STATES)")
                _downloadStates.remove(mapId)
                accessOrder.remove(mapId)
                return
            }
        }

        // If no completed downloads found, evict oldest entry regardless
        val lruMapId = accessOrder.firstOrNull()
        if (lruMapId != null) {
            Log.w(TAG, "No completed downloads to evict, removing oldest: $lruMapId")
            _downloadStates.remove(lruMapId)
            accessOrder.removeAt(0)
        }
    }

    /**
     * Check if map is available and update state
     */
    suspend fun checkAvailability(mapId: String) {
        Log.d(TAG, "Checking availability for: $mapId")
        val isAvailable = platformMapManager.isMapAvailable(mapId)
        updateState(mapId) { it.copy(isAvailable = isAvailable, error = null) }
        Log.i(TAG, "Map availability: $mapId -> $isAvailable")
    }

    /**
     * Start map download with auto state management
     * @param onDownloadComplete Optional callback invoked after successful download and state update
     */
    suspend fun downloadMap(
        mapId: String,
        onDownloadComplete: (suspend () -> Unit)? = null,
    ) {
        Log.i(TAG, "Starting download for: $mapId")

        // Reset state
        updateState(mapId) { it.copy(isDownloading = true, progress = 0, error = null) }

        platformMapManager.downloadMap(
            mapId = mapId,
            onProgress = { progress ->
                Log.v(TAG, "Download progress: $mapId -> $progress%")
                updateState(mapId) { it.copy(progress = progress) }
            },
            onSuccess = {
                Log.i(TAG, "Download success: $mapId")
                updateState(mapId) {
                    it.copy(
                        isAvailable = true,
                        isDownloading = false,
                        progress = 100,
                        error = null,
                    )
                }
                // Notify caller that download completed (launch in coroutine since callback is suspend)
                onDownloadComplete?.let { callback ->
                    MainScope().launch {
                        callback()
                    }
                }
            },
            onError = { code, message ->
                Log.e(TAG, "Download failed: $mapId (code=$code, message=$message)")
                updateState(mapId) {
                    it.copy(
                        isDownloading = false,
                        error = message ?: "Download failed (code: $code)",
                    )
                }
            },
        )
    }

    /**
     * Cancel ongoing download
     */
    fun cancelDownload(mapId: String) {
        Log.i(TAG, "Cancelling download for: $mapId")
        platformMapManager.cancelDownload(mapId)
        updateState(mapId) { it.copy(isDownloading = false) }
    }

    /**
     * Auto-download map if not available and auto-download is enabled
     */
    suspend fun autoDownloadIfNeeded(
        mapId: String,
        autoDownload: Boolean,
    ) {
        Log.i(TAG, "autoDownloadIfNeeded called: mapId=$mapId, autoDownload=$autoDownload")
        checkAvailability(mapId)
        val state = getDownloadState(mapId).value

        Log.d(TAG, "After checkAvailability: mapId=$mapId, isAvailable=${state.isAvailable}, isDownloading=${state.isDownloading}")

        if (!state.isAvailable && autoDownload && !state.isDownloading) {
            Log.i(TAG, "Auto-download triggered for: $mapId")
            downloadMap(mapId)
        } else {
            Log.d(
                TAG,
                "Auto-download NOT triggered: " +
                    "isAvailable=${state.isAvailable}, " +
                    "autoDownload=$autoDownload, " +
                    "isDownloading=${state.isDownloading}",
            )
        }
    }

    /**
     * Clear completed downloads from the cache to prevent memory accumulation.
     * Preserves active downloads and failed downloads that may be retried.
     */
    fun clearCompletedDownloads() {
        Log.d(TAG, "Clearing completed downloads from cache")
        val toRemove = mutableListOf<String>()

        _downloadStates.forEach { (mapId, stateFlow) ->
            val state = stateFlow.value
            // Remove only completed successful downloads (not downloading, no error)
            if (!state.isDownloading && state.error == null && state.isAvailable) {
                toRemove.add(mapId)
            }
        }

        toRemove.forEach { mapId ->
            _downloadStates.remove(mapId)
            accessOrder.remove(mapId)
            Log.v(TAG, "Removed completed download: $mapId")
        }

        Log.i(TAG, "Cleared ${toRemove.size} completed downloads from cache")
    }

    /**
     * Get the current number of tracked download states.
     * Useful for monitoring cache size and memory usage.
     */
    fun getTrackedDownloadCount(): Int = _downloadStates.size

    private fun updateState(
        mapId: String,
        update: (DownloadState) -> DownloadState,
    ) {
        // Evict LRU completed downloads if we've hit the limit
        if (_downloadStates.size >= MAX_DOWNLOAD_STATES && !_downloadStates.containsKey(mapId)) {
            evictLRUCompletedDownload()
        }

        // Update access order (move to end = most recently used)
        accessOrder.remove(mapId)
        accessOrder.add(mapId)

        val flow = _downloadStates.getOrPut(mapId) { MutableStateFlow(DownloadState()) }
        val oldState = flow.value
        val newState = update(oldState)
        flow.value = newState
        Log.v(
            TAG,
            "State updated: $mapId | isDownloading: ${oldState.isDownloading}->${newState.isDownloading}, " +
                "progress: ${oldState.progress}->${newState.progress}, " +
                "available: ${oldState.isAvailable}->${newState.isAvailable}",
        )
    }
}
