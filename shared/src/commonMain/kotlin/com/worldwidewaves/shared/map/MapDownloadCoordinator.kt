package com.worldwidewaves.shared.map

/*
 * Copyright 2025 DrWave
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * https://www.apache.org/licenses/LICENSE-2.0
 */

import com.worldwidewaves.shared.utils.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Shared coordinator for map download operations across Android and iOS.
 *
 * Provides unified download state management and orchestration logic:
 * - Availability checking
 * - Progress tracking (0-100%)
 * - Error handling
 * - Auto-download triggers
 *
 * Platform-specific download mechanisms (Play Dynamic Features vs ODR)
 * are delegated to PlatformMapManager implementations.
 */
class MapDownloadCoordinator(
    private val platformMapManager: PlatformMapManager,
) {
    private companion object {
        private const val TAG = "MapDownloadCoordinator"
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

    /**
     * Get download state flow for a specific map
     */
    fun getDownloadState(mapId: String): StateFlow<DownloadState> =
        _downloadStates
            .getOrPut(mapId) {
                MutableStateFlow(DownloadState())
            }.asStateFlow()

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
     */
    suspend fun downloadMap(mapId: String) {
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
        checkAvailability(mapId)
        val state = getDownloadState(mapId).value

        if (!state.isAvailable && autoDownload && !state.isDownloading) {
            Log.i(TAG, "Auto-download triggered for: $mapId")
            downloadMap(mapId)
        }
    }

    private fun updateState(
        mapId: String,
        update: (DownloadState) -> DownloadState,
    ) {
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
