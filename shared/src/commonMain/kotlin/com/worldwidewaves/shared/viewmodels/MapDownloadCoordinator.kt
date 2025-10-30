package com.worldwidewaves.shared.viewmodels

/*
 * Copyright 2025 DrWave
 *
 * WorldWideWaves is an ephemeral mobile app designed to orchestrate human waves through cities and
 * countries. The project aims to transcend physical and cultural
 * boundaries, fostering unity, community, and shared human experience by leveraging real-time
 * coordination and location-based services.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.worldwidewaves.shared.map.MapFeatureState
import com.worldwidewaves.shared.utils.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Coordinates map download operations across platforms.
 *
 * Contains all platform-agnostic download workflow logic without UI lifecycle concerns.
 * Used by composition in platform-specific ViewModels (AndroidMapViewModel, IosMapViewModel).
 *
 * RESPONSIBILITIES:
 * • Download state management
 * • Retry logic with exponential backoff
 * • Progress tracking
 * • Error handling and recovery
 * • Platform-agnostic download workflow
 *
 * DEPENDENCIES:
 * • PlatformMapDownloadAdapter (for platform-specific operations)
 *
 * USED BY:
 * • AndroidMapViewModel (composition)
 * • IosMapViewModel (composition)
 */
class MapDownloadCoordinator(
    private val platformAdapter: PlatformMapDownloadAdapter,
) : IMapDownloadManager {
    private val _featureState = MutableStateFlow<MapFeatureState>(MapFeatureState.NotChecked)
    override val featureState: StateFlow<MapFeatureState> = _featureState

    var currentMapId: String? = null
    val retryManager = MapDownloadUtils.RetryManager()

    companion object {
        private const val TAG = "MapDownloadCoordinator"
    }

    // ------------------------------------------------------------------------
    // Public API - IMapDownloadManager implementation
    // ------------------------------------------------------------------------

    override suspend fun checkIfMapIsAvailable(
        mapId: String,
        autoDownload: Boolean,
    ) {
        Log.d(TAG, "checkIfMapIsAvailable id=$mapId auto=$autoDownload")
        currentMapId = mapId

        if (platformAdapter.isMapInstalled(mapId)) {
            _featureState.value = MapFeatureState.Available
        } else {
            _featureState.value = MapFeatureState.NotAvailable
            if (autoDownload) {
                downloadMap(mapId)
            }
        }
    }

    override suspend fun downloadMap(
        mapId: String,
        onMapDownloaded: (() -> Unit)?,
    ) {
        Log.i(TAG, "downloadMap called for $mapId")

        // Prevent concurrent downloads
        if (MapDownloadUtils.isActiveDownload(_featureState.value)) {
            Log.w(TAG, "downloadMap ignored – already downloading")
            return
        }

        // Early-exit if map is already fully installed with valid files
        if (platformAdapter.isMapInstalled(mapId)) {
            Log.i(TAG, "downloadMap skipped – map already installed: $mapId")
            _featureState.value = MapFeatureState.Installed
            onMapDownloaded?.invoke()
            return
        }

        // Only reset retry count if this is a new download (not a retry)
        // If we're coming from a Retrying state, preserve the counter
        val isRetrying = _featureState.value is MapFeatureState.Retrying
        _featureState.value = MapFeatureState.Pending
        if (!isRetrying) {
            retryManager.resetRetryCount()
        }
        currentMapId = mapId

        platformAdapter.startPlatformDownload(mapId, onMapDownloaded)
    }

    override suspend fun cancelDownload() {
        Log.i(TAG, "cancelDownload called")
        platformAdapter.cancelPlatformDownload()
    }

    override fun getErrorMessage(errorCode: Int): String = platformAdapter.getLocalizedErrorMessage(errorCode)

    // ------------------------------------------------------------------------
    // Helper methods for platform adapters
    // ------------------------------------------------------------------------

    fun handleDownloadProgress(
        totalBytes: Long,
        downloadedBytes: Long,
    ) {
        val progressPercent = MapDownloadUtils.calculateProgressPercent(totalBytes, downloadedBytes)
        _featureState.value = MapFeatureState.Downloading(progressPercent)
    }

    fun handleDownloadSuccess() {
        Log.i(TAG, "Download completed successfully")
        _featureState.value = MapFeatureState.Installed
        retryManager.resetRetryCount()
    }

    fun handleDownloadFailure(
        errorCode: Int,
        shouldRetry: Boolean = true,
    ) {
        Log.e(TAG, "Download failed with error code: $errorCode")

        if (shouldRetry && retryManager.canRetry()) {
            val retryCount = retryManager.incrementRetryCount()
            _featureState.value = MapFeatureState.Retrying(retryCount, MapDownloadUtils.RetryManager.MAX_RETRIES)
            Log.i(TAG, "Scheduling retry #$retryCount")
        } else {
            _featureState.value = MapFeatureState.Failed(errorCode, platformAdapter.getLocalizedErrorMessage(errorCode))
            retryManager.resetRetryCount()
        }
    }

    fun handleDownloadCancellation() {
        Log.w(TAG, "Download canceled")
        _featureState.value = MapFeatureState.NotAvailable
        retryManager.resetRetryCount()
    }

    @Suppress("UnusedParameter") // moduleIds kept for API compatibility and future use
    fun handleInstallComplete(moduleIds: List<String>) {
        // Don't clear cache immediately on installation - this causes a race condition
        // where MapLibre tries to load the mbtiles file while it's being deleted.
        // The cache will be naturally cleaned up by stale file detection (isCachedFileStale).
        handleDownloadSuccess()
    }

    // State update methods for platform adapters
    fun setStateInstalling() {
        _featureState.value = MapFeatureState.Installing
    }

    fun setStatePending() {
        _featureState.value = MapFeatureState.Pending
    }

    fun setStateCanceling() {
        _featureState.value = MapFeatureState.Canceling
    }

    fun setStateUnknown() {
        _featureState.value = MapFeatureState.Unknown
    }

    fun setStateRetrying(
        retryCount: Int,
        maxRetries: Int,
    ) {
        _featureState.value = MapFeatureState.Retrying(retryCount, maxRetries)
    }
}

/**
 * Platform adapter interface for MapDownloadCoordinator.
 *
 * RESPONSIBILITIES:
 * • Platform-specific map installation checks
 * • Platform-specific download initiation
 * • Platform-specific cancellation
 * • Platform-specific error message localization
 * • Platform-specific cache management
 *
 * IMPLEMENTORS:
 * • AndroidMapViewModel (anonymous object)
 * • IosMapViewModel (anonymous object)
 */
interface PlatformMapDownloadAdapter {
    suspend fun isMapInstalled(mapId: String): Boolean

    suspend fun startPlatformDownload(
        mapId: String,
        onMapDownloaded: (() -> Unit)?,
    )

    suspend fun cancelPlatformDownload()

    fun getLocalizedErrorMessage(errorCode: Int): String

    fun clearCacheForInstalledMaps(mapIds: List<String>)
}
