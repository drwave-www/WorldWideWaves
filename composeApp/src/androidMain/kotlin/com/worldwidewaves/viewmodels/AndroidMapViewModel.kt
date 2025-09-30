package com.worldwidewaves.viewmodels

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

import android.app.Application
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.play.core.splitinstall.SplitInstallManager
import com.google.android.play.core.splitinstall.SplitInstallManagerFactory
import com.google.android.play.core.splitinstall.SplitInstallRequest
import com.google.android.play.core.splitinstall.SplitInstallSessionState
import com.google.android.play.core.splitinstall.SplitInstallStateUpdatedListener
import com.google.android.play.core.splitinstall.model.SplitInstallErrorCode
import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus
import com.worldwidewaves.shared.MokoRes
import com.worldwidewaves.shared.data.clearEventCache
import com.worldwidewaves.shared.utils.Log
import com.worldwidewaves.shared.viewmodels.MapDownloadManager
import com.worldwidewaves.shared.viewmodels.MapDownloadUtils
import com.worldwidewaves.shared.viewmodels.MapViewModel
import com.worldwidewaves.shared.viewmodels.PlatformMapDownloadAdapter
import dev.icerock.moko.resources.desc.Resource
import dev.icerock.moko.resources.desc.ResourceFormatted
import dev.icerock.moko.resources.desc.StringDesc
import kotlinx.coroutines.launch

/**
 * Android implementation of MapViewModel using pure business logic composition.
 * Handles Android-specific Play Core integration while delegating download logic to MapDownloadLogic.
 *
 * RESPONSIBILITIES:
 * • Android UI lifecycle management (AndroidViewModel)
 * • Play Core SplitInstallManager integration
 * • Android-specific error message localization
 * • Play Store listener management
 * • Delegates business logic to MapDownloadLogic
 */
class AndroidMapViewModel(
    application: Application,
) : AndroidViewModel(application),
    MapViewModel {
    // Android-specific Play Core components
    private val splitInstallManager: SplitInstallManager = SplitInstallManagerFactory.create(application)
    private var currentSessionId = 0
    private var installStateListener: SplitInstallStateUpdatedListener? = null

    // Platform adapter for business logic
    private val platformAdapter =
        object : PlatformMapDownloadAdapter {
            override suspend fun isMapInstalled(mapId: String): Boolean = splitInstallManager.installedModules.contains(mapId)

            override suspend fun startPlatformDownload(
                mapId: String,
                onMapDownloaded: (() -> Unit)?,
            ) {
                val request = SplitInstallRequest.newBuilder().addModule(mapId).build()

                splitInstallManager
                    .startInstall(request)
                    .addOnSuccessListener { sessionId ->
                        Log.i(TAG, "startInstall success session=$sessionId")
                        currentSessionId = sessionId
                        downloadManager.retryManager.resetRetryCount()
                        onMapDownloaded?.invoke()
                    }.addOnFailureListener { exception ->
                        Log.e(TAG, "startInstall failure ${exception.message}")
                        handleRetryWithExponentialBackoff(mapId, onMapDownloaded)
                    }
            }

            override suspend fun cancelPlatformDownload() {
                if (currentSessionId > 0) {
                    splitInstallManager.cancelInstall(currentSessionId)
                }
            }

            override fun getLocalizedErrorMessage(errorCode: Int): String {
                @Suppress("DEPRECATION")
                val resource =
                    when (errorCode) {
                        SplitInstallErrorCode.NETWORK_ERROR -> MokoRes.strings.map_error_network
                        SplitInstallErrorCode.INSUFFICIENT_STORAGE -> MokoRes.strings.map_error_insufficient_storage
                        SplitInstallErrorCode.MODULE_UNAVAILABLE -> MokoRes.strings.map_error_module_unavailable
                        SplitInstallErrorCode.ACTIVE_SESSIONS_LIMIT_EXCEEDED -> MokoRes.strings.map_error_active_sessions_limit
                        SplitInstallErrorCode.INVALID_REQUEST -> MokoRes.strings.map_error_invalid_request
                        SplitInstallErrorCode.API_NOT_AVAILABLE -> MokoRes.strings.map_error_api_not_available
                        SplitInstallErrorCode.INCOMPATIBLE_WITH_EXISTING_SESSION ->
                            MokoRes.strings.map_error_incompatible_with_existing_session
                        SplitInstallErrorCode.SERVICE_DIED -> MokoRes.strings.map_error_service_died
                        SplitInstallErrorCode.ACCESS_DENIED -> MokoRes.strings.map_error_access_denied
                        PLAY_STORE_AUTH_ERROR_CODE -> MokoRes.strings.map_error_account_issue
                        else -> MokoRes.strings.map_error_unknown
                    }

                return if (resource == MokoRes.strings.map_error_unknown) {
                    StringDesc.ResourceFormatted(resource, errorCode).toString(application)
                } else {
                    StringDesc.Resource(resource).toString(application)
                }
            }

            override fun clearCacheForInstalledMaps(mapIds: List<String>) {
                mapIds.forEach { id ->
                    try {
                        clearEventCache(id)
                    } catch (_: Exception) {
                        // Non-critical cache cleanup failure
                    }
                }
            }

            private fun handleRetryWithExponentialBackoff(
                mapId: String,
                onMapDownloaded: (() -> Unit)?,
            ) {
                if (downloadManager.retryManager.canRetry()) {
                    val delay = downloadManager.retryManager.getNextRetryDelay()
                    val retryCount = downloadManager.retryManager.incrementRetryCount()
                    downloadManager.setStateRetrying(retryCount, MapDownloadUtils.RetryManager.MAX_RETRIES)

                    Handler(Looper.getMainLooper()).postDelayed({
                        viewModelScope.launch {
                            downloadManager.downloadMap(mapId, onMapDownloaded)
                        }
                    }, delay)
                } else {
                    downloadManager.handleDownloadFailure(0, shouldRetry = false)
                }
            }
        }

    // Pure business logic (no UI lifecycle concerns)
    private val downloadManager: MapDownloadManager = MapDownloadManager(platformAdapter)

    // Delegate public interface to business logic
    override val featureState = downloadManager.featureState

    private companion object Companion {
        private const val TAG = "MapViewModel"
        private const val PLAY_STORE_AUTH_ERROR_CODE = -100
    }

    init {
        registerAndroidListeners()
    }

    // ------------------------------------------------------------------------
    // Public API (delegates to shared logic)
    // ------------------------------------------------------------------------

    override fun checkIfMapIsAvailable(
        mapId: String,
        autoDownload: Boolean,
    ) {
        viewModelScope.launch {
            downloadManager.checkIfMapIsAvailable(mapId, autoDownload)
        }
    }

    override fun downloadMap(
        mapId: String,
        onMapDownloaded: (() -> Unit)?,
    ) {
        viewModelScope.launch {
            downloadManager.downloadMap(mapId, onMapDownloaded)
        }
    }

    override fun cancelDownload() {
        viewModelScope.launch {
            downloadManager.cancelDownload()
        }
    }

    // ------------------------------------------------------------------------
    // Android-specific Play Core integration (preserves original behavior)
    // ------------------------------------------------------------------------

    private fun registerAndroidListeners() {
        installStateListener =
            SplitInstallStateUpdatedListener { state ->
                // Only process updates for our current session (original behavior preserved)
                if (state.sessionId() != currentSessionId && currentSessionId != 0) {
                    return@SplitInstallStateUpdatedListener
                }
                processAndroidInstallState(state)
            }
        installStateListener?.let {
            splitInstallManager.registerListener(it)
        }
    }

    private fun processAndroidInstallState(state: SplitInstallSessionState) {
        when (state.status()) {
            SplitInstallSessionStatus.DOWNLOADING -> {
                val totalBytes = state.totalBytesToDownload()
                val downloadedBytes = state.bytesDownloaded()
                downloadManager.handleDownloadProgress(totalBytes, downloadedBytes)
            }
            SplitInstallSessionStatus.DOWNLOADED -> {
                downloadManager.handleDownloadProgress(100, 100)
            }
            SplitInstallSessionStatus.INSTALLING -> {
                downloadManager.setStateInstalling()
            }
            SplitInstallSessionStatus.INSTALLED -> {
                Log.i(TAG, "Status: INSTALLED – modules=${state.moduleNames()}")
                val moduleIds = getModuleIdsFromState(state)
                downloadManager.handleInstallComplete(moduleIds)
                currentSessionId = 0
            }
            SplitInstallSessionStatus.PENDING -> {
                downloadManager.setStatePending()
            }
            SplitInstallSessionStatus.FAILED -> {
                handleAndroidFailure(state)
            }
            SplitInstallSessionStatus.CANCELED -> {
                downloadManager.handleDownloadCancellation()
                currentSessionId = 0
            }
            SplitInstallSessionStatus.CANCELING -> {
                downloadManager.setStateCanceling()
            }
            SplitInstallSessionStatus.REQUIRES_USER_CONFIRMATION -> {
                downloadManager.setStatePending()
            }
            else -> {
                downloadManager.setStateUnknown()
            }
        }
    }

    private fun handleAndroidFailure(state: SplitInstallSessionState) {
        val errorCode = state.errorCode()
        Log.e(TAG, "Status: FAILED code=$errorCode")

        // Handle SERVICE_DIED with retry (original behavior preserved)
        @Suppress("DEPRECATION")
        val shouldRetry = errorCode == SplitInstallErrorCode.SERVICE_DIED

        downloadManager.handleDownloadFailure(errorCode, shouldRetry)
        currentSessionId = 0
    }

    private fun getModuleIdsFromState(state: SplitInstallSessionState): List<String> =
        try {
            state.moduleNames().toList()
        } catch (_: Exception) {
            emptyList()
        }.ifEmpty {
            downloadManager.currentMapId?.let { listOf(it) }.orEmpty()
        }

    override fun onCleared() {
        installStateListener?.let {
            splitInstallManager.unregisterListener(it)
        }
        super.onCleared()
    }
}
