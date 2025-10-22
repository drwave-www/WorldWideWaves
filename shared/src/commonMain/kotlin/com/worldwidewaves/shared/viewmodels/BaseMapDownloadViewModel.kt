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
import com.worldwidewaves.shared.ui.BaseViewModel
import com.worldwidewaves.shared.utils.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Shared base ViewModel for map download management.
 * Contains all platform-agnostic logic extracted from Android MapViewModel.
 * Platform-specific subclasses provide download implementation details.
 */
abstract class BaseMapDownloadViewModel :
    BaseViewModel(),
    IMapDownloadManager {
    companion object {
        private const val TAG = "WWW.ViewModel.Map"
    }

    private val featureStateMutable = MutableStateFlow<MapFeatureState>(MapFeatureState.NotChecked)

    override val featureState: StateFlow<MapFeatureState> = featureStateMutable

    var currentMapId: String? = null
    val retryManager = MapDownloadUtils.RetryManager()

    // ------------------------------------------------------------------------
    // Abstract methods for platform-specific implementation
    // ------------------------------------------------------------------------

    /**
     * Platform-specific check if map module is installed.
     */
    protected abstract suspend fun isMapInstalled(mapId: String): Boolean

    /**
     * Platform-specific map download initiation.
     * Should call handleDownloadProgress, handleDownloadSuccess, or handleDownloadFailure.
     */
    protected abstract suspend fun startPlatformDownload(
        mapId: String,
        onMapDownloaded: (() -> Unit)?,
    )

    /**
     * Platform-specific download cancellation.
     */
    protected abstract suspend fun cancelPlatformDownload()

    /**
     * Platform-specific error code to string resource mapping.
     */
    protected abstract fun getLocalizedErrorMessage(errorCode: Int): String

    /**
     * Platform-specific cleanup when maps are installed.
     */
    protected abstract fun clearCacheForInstalledMaps(mapIds: List<String>)

    // ------------------------------------------------------------------------
    // Shared implementation (extracted from Android MapViewModel)
    // ------------------------------------------------------------------------

    override suspend fun checkIfMapIsAvailable(
        mapId: String,
        autoDownload: Boolean,
    ) {
        Log.d(TAG, "checkIfMapIsAvailable id=$mapId auto=$autoDownload")
        currentMapId = mapId

        if (isMapInstalled(mapId)) {
            featureStateMutable.value = MapFeatureState.Available
        } else {
            featureStateMutable.value = MapFeatureState.NotAvailable
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
        if (MapDownloadUtils.isActiveDownload(featureStateMutable.value)) {
            Log.w(TAG, "downloadMap ignored – already downloading")
            return
        }

        featureStateMutable.value = MapFeatureState.Pending
        retryManager.resetRetryCount()
        currentMapId = mapId

        startPlatformDownload(mapId, onMapDownloaded)
    }

    override suspend fun cancelDownload() {
        Log.i(TAG, "cancelDownload called")
        cancelPlatformDownload()
    }

    override fun getErrorMessage(errorCode: Int): String = getLocalizedErrorMessage(errorCode)

    // ------------------------------------------------------------------------
    // Shared helper methods for platform implementations
    // ------------------------------------------------------------------------

    fun handleDownloadProgress(
        totalBytes: Long,
        downloadedBytes: Long,
    ) {
        val progressPercent = MapDownloadUtils.calculateProgressPercent(totalBytes, downloadedBytes)
        featureStateMutable.value = MapFeatureState.Downloading(progressPercent)
    }

    fun handleDownloadSuccess() {
        Log.i(TAG, "Download completed successfully")
        featureStateMutable.value = MapFeatureState.Installed
        retryManager.resetRetryCount()
    }

    fun handleDownloadFailure(
        errorCode: Int,
        shouldRetry: Boolean = true,
    ) {
        Log.e(TAG, "Download failed with error code: $errorCode")

        if (shouldRetry && retryManager.canRetry()) {
            val retryCount = retryManager.incrementRetryCount()
            featureStateMutable.value = MapFeatureState.Retrying(retryCount, MapDownloadUtils.RetryManager.MAX_RETRIES)
            Log.i(TAG, "Scheduling retry #$retryCount")
        } else {
            featureStateMutable.value = MapFeatureState.Failed(errorCode, getErrorMessage(errorCode))
            retryManager.resetRetryCount()
        }
    }

    fun handleDownloadCancellation() {
        Log.w(TAG, "Download canceled")
        featureStateMutable.value = MapFeatureState.NotAvailable
        retryManager.resetRetryCount()
    }

    fun handleInstallComplete(moduleIds: List<String>) {
        clearCacheForInstalledMaps(moduleIds)
        handleDownloadSuccess()
    }

    // Public methods for specific state updates needed by composition pattern
    fun setStateInstalling() {
        featureStateMutable.value = MapFeatureState.Installing
    }

    fun setStatePending() {
        featureStateMutable.value = MapFeatureState.Pending
    }

    fun setStateCanceling() {
        featureStateMutable.value = MapFeatureState.Canceling
    }

    fun setStateUnknown() {
        featureStateMutable.value = MapFeatureState.Unknown
    }

    fun setStateRetrying(
        retryCount: Int,
        maxRetries: Int,
    ) {
        featureStateMutable.value = MapFeatureState.Retrying(retryCount, maxRetries)
    }
}
