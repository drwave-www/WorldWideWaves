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
import android.util.Log
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
import com.worldwidewaves.shared.clearEventCache
import com.worldwidewaves.shared.map.MapFeatureState
import dev.icerock.moko.resources.desc.Resource
import dev.icerock.moko.resources.desc.ResourceFormatted
import dev.icerock.moko.resources.desc.StringDesc
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// ----------------------------------------------------------------------------

/**
 * ViewModel that manages downloading and installing dynamic feature modules for maps.
 * Follows Google's Play Feature Delivery best practices.
 */
class MapViewModel(
    application: Application,
) : AndroidViewModel(application) {
    // ---------------------------------------------------------------------
    // Logging
    // ---------------------------------------------------------------------

    private val splitInstallManager: SplitInstallManager = SplitInstallManagerFactory.create(application)

    private val _featureState = MutableStateFlow<MapFeatureState>(MapFeatureState.NotChecked)
    val featureState: StateFlow<MapFeatureState> = _featureState

    private var currentSessionId = 0
    private var installStateListener: SplitInstallStateUpdatedListener? = null
    private var currentMapId: String? = null

    private var retryCount = 0

    // Constants for retry logic and progress calculation
    private companion object {
        private const val TAG = "MapInstall"
        private const val MAX_RETRIES = 3
        private const val RETRY_DELAY_MILLIS = 1000L
        private const val PROGRESS_MULTIPLIER = 100
        private const val PLAY_STORE_AUTH_ERROR_CODE = -100
    }

    // ------------------------------------------------------------------------

    init {
        registerListener()
    }

    // ------------------------------------------------------------------------

    private fun registerListener() {
        installStateListener =
            SplitInstallStateUpdatedListener { state ->
                // Only process updates for our current session
                if (state.sessionId() != currentSessionId && currentSessionId != 0) {
                    return@SplitInstallStateUpdatedListener
                }

                updateStateFromInstallState(state)
            }
        installStateListener?.let {
            splitInstallManager.registerListener(it)
        }
    }

    // ------------------------------------------------------------------------

    private fun updateStateFromInstallState(state: SplitInstallSessionState) {
        // Handle SERVICE_DIED error with retry logic
        if (handleServiceDiedError(state)) {
            return
        }

        // Handle the main install status
        handleInstallStatus(state)
    }

    private fun handleServiceDiedError(state: SplitInstallSessionState): Boolean {
        @Suppress("DEPRECATION")
        if (state.status() == SplitInstallSessionStatus.FAILED &&
            state.errorCode() == SplitInstallErrorCode.SERVICE_DIED
        ) {
            Log.w(TAG, "SERVICE_DIED for session ${state.sessionId()} – scheduling retry")

            val moduleId = getCurrentModuleFromState(state) ?: currentMapId
            if (moduleId != null && retryCount < MAX_RETRIES) {
                scheduleRetryForServiceDied(moduleId)
                return true
            }
        }
        return false
    }

    private fun scheduleRetryForServiceDied(moduleId: String) {
        val delay = RETRY_DELAY_MILLIS * (1 shl retryCount)
        retryCount++

        _featureState.value = MapFeatureState.Retrying(retryCount, MAX_RETRIES)
        Log.i(TAG, "Retry #$retryCount for $moduleId after ${delay}ms")

        val request =
            SplitInstallRequest
                .newBuilder()
                .addModule(moduleId)
                .build()

        Handler(Looper.getMainLooper()).postDelayed({
            startInstallWithRetry(request, moduleId, delay)
        }, delay)
    }

    private fun handleInstallStatus(state: SplitInstallSessionState) {
        when (state.status()) {
            SplitInstallSessionStatus.DOWNLOADING -> handleDownloadingStatus(state)
            SplitInstallSessionStatus.DOWNLOADED -> handleDownloadedStatus()
            SplitInstallSessionStatus.INSTALLING -> handleInstallingStatus()
            SplitInstallSessionStatus.INSTALLED -> handleInstalledStatus(state)
            SplitInstallSessionStatus.PENDING -> handlePendingStatus()
            SplitInstallSessionStatus.FAILED -> handleFailedStatus(state)
            SplitInstallSessionStatus.CANCELED -> handleCanceledStatus()
            SplitInstallSessionStatus.CANCELING -> handleCancelingStatus()
            SplitInstallSessionStatus.REQUIRES_USER_CONFIRMATION -> handleUserConfirmationStatus(state)
            SplitInstallSessionStatus.UNKNOWN -> handleUnknownStatus()
            else -> handleUnknownStatus()
        }
    }

    private fun handleDownloadingStatus(state: SplitInstallSessionState) {
        val totalBytes = state.totalBytesToDownload()
        val downloadedBytes = state.bytesDownloaded()
        val progressPercent =
            if (totalBytes > 0) {
                (downloadedBytes * PROGRESS_MULTIPLIER / totalBytes).toInt()
            } else {
                0
            }
        _featureState.value = MapFeatureState.Downloading(progressPercent)
    }

    private fun handleDownloadedStatus() {
        _featureState.value = MapFeatureState.Downloading(PROGRESS_MULTIPLIER)
    }

    private fun handleInstallingStatus() {
        _featureState.value = MapFeatureState.Installing
    }

    private fun handleInstalledStatus(state: SplitInstallSessionState) {
        Log.i(TAG, "Status: INSTALLED – modules=${state.moduleNames()}")
        clearCacheForInstalledModules(state)
        _featureState.value = MapFeatureState.Installed
        currentSessionId = 0
    }

    private fun clearCacheForInstalledModules(state: SplitInstallSessionState) {
        try {
            val moduleIds: List<String> = getModuleIdsFromState(state)
            moduleIds.forEach { id ->
                try {
                    clearEventCache(id)
                } catch (_: Exception) {
                    // Do not crash the ViewModel because of cache cleanup issues
                }
            }
        } catch (_: Exception) {
            // Defensive catch – no-op on failure
        }
    }

    private fun getModuleIdsFromState(state: SplitInstallSessionState): List<String> =
        try {
            state.moduleNames().toList()
        } catch (_: Exception) {
            emptyList()
        }.ifEmpty {
            currentMapId?.let { listOf(it) }.orEmpty()
        }

    private fun handlePendingStatus() {
        _featureState.value = MapFeatureState.Pending
    }

    private fun handleFailedStatus(state: SplitInstallSessionState) {
        Log.e(TAG, "Status: FAILED code=${state.errorCode()}")
        _featureState.value =
            MapFeatureState.Failed(
                state.errorCode(),
                getErrorMessage(state.errorCode()),
            )
        currentSessionId = 0
    }

    private fun handleCanceledStatus() {
        Log.w(TAG, "Status: CANCELED")
        _featureState.value = MapFeatureState.NotAvailable
        currentSessionId = 0
    }

    private fun handleCancelingStatus() {
        Log.w(TAG, "Status: CANCELING")
        _featureState.value = MapFeatureState.Canceling
    }

    private fun handleUserConfirmationStatus(state: SplitInstallSessionState) {
        Log.i(TAG, "Status: REQUIRES_USER_CONFIRMATION")
        // Handle user confirmation by treating as pending
        _featureState.value = MapFeatureState.Pending
    }

    private fun handleUnknownStatus() {
        _featureState.value = MapFeatureState.Unknown
    }

    // ------------------------------------------------------------------------

    // Utility method to extract module ID from session state
    private fun getCurrentModuleFromState(state: SplitInstallSessionState): String? =
        try {
            state.moduleNames().firstOrNull()
        } catch (_: Exception) {
            null
        }

    // ------------------------------------------------------------------------

    private fun getErrorMessage(errorCode: Int): String {
        @Suppress("DEPRECATION")
        val res =
            when (errorCode) {
                SplitInstallErrorCode.NETWORK_ERROR ->
                    MokoRes.strings.map_error_network
                SplitInstallErrorCode.INSUFFICIENT_STORAGE ->
                    MokoRes.strings.map_error_insufficient_storage
                SplitInstallErrorCode.MODULE_UNAVAILABLE ->
                    MokoRes.strings.map_error_module_unavailable
                SplitInstallErrorCode.ACTIVE_SESSIONS_LIMIT_EXCEEDED ->
                    MokoRes.strings.map_error_active_sessions_limit
                SplitInstallErrorCode.INVALID_REQUEST ->
                    MokoRes.strings.map_error_invalid_request
                SplitInstallErrorCode.API_NOT_AVAILABLE ->
                    MokoRes.strings.map_error_api_not_available
                SplitInstallErrorCode.INCOMPATIBLE_WITH_EXISTING_SESSION ->
                    MokoRes.strings.map_error_incompatible_with_existing_session
                SplitInstallErrorCode.SERVICE_DIED ->
                    MokoRes.strings.map_error_service_died
                SplitInstallErrorCode.ACCESS_DENIED ->
                    MokoRes.strings.map_error_access_denied
                PLAY_STORE_AUTH_ERROR_CODE ->
                    // Error code -100 typically indicates Google Play Store authentication issues
                    // This corresponds to Play Store error code 1010 (account no longer exists)
                    MokoRes.strings.map_error_account_issue
                else ->
                    // Generic unknown error with code placeholder
                    MokoRes.strings.map_error_unknown
            }

        return if (res == MokoRes.strings.map_error_unknown) {
            StringDesc.ResourceFormatted(res, errorCode).toString(getApplication())
        } else {
            StringDesc.Resource(res).toString(getApplication())
        }
    }

    // ------------------------------------------------------------------------

    fun checkIfMapIsAvailable(
        mapId: String,
        autoDownload: Boolean = false,
    ) {
        viewModelScope.launch {
            Log.d(TAG, "checkIfMapIsAvailable id=$mapId auto=$autoDownload")
            currentMapId = mapId
            if (splitInstallManager.installedModules.contains(mapId)) {
                _featureState.value = MapFeatureState.Available
            } else {
                _featureState.value = MapFeatureState.NotAvailable
                // Only auto-download if explicitly requested
                if (autoDownload) {
                    downloadMap(mapId)
                }
            }
        }
    }

    // ------------------------------------------------------------------------

    fun downloadMap(
        mapId: String,
        onMapDownloaded: (() -> Unit)? = null,
    ) {
        Log.i(TAG, "downloadMap called for $mapId")
        if (_featureState.value is MapFeatureState.Downloading ||
            _featureState.value is MapFeatureState.Pending
        ) {
            Log.w(TAG, "downloadMap ignored – already downloading")
            return // Already downloading
        }

        _featureState.value = MapFeatureState.Pending
        retryCount = 0 // Reset retry count
        currentMapId = mapId

        val request =
            SplitInstallRequest
                .newBuilder()
                .addModule(mapId)
                .build()

        startInstallWithRetry(request, mapId, onMapDownloaded = onMapDownloaded)
    }

    // ------------------------------------------------------------------------

    private fun startInstallWithRetry(
        request: SplitInstallRequest,
        mapId: String,
        delay: Long = RETRY_DELAY_MILLIS,
        onMapDownloaded: (() -> Unit)? = null,
    ) {
        splitInstallManager
            .startInstall(request)
            .addOnSuccessListener { sessionId ->
                Log.i(TAG, "startInstall success session=$sessionId")
                currentSessionId = sessionId
                retryCount = 0 // Reset retry count on success
                onMapDownloaded?.invoke()
            }.addOnFailureListener { exception ->
                Log.e(TAG, "startInstall failure ${exception.message}")
                // Check if it's worth retrying
                if (retryCount < MAX_RETRIES) {
                    // Use exponential backoff for retries
                    val nextDelay = delay * (1 shl retryCount)
                    retryCount++

                    _featureState.value = MapFeatureState.Retrying(retryCount, MAX_RETRIES)
                    Log.i(TAG, "Scheduling retry #$retryCount after ${nextDelay}ms")

                    // Schedule retry after delay
                    Handler(Looper.getMainLooper()).postDelayed({
                        startInstallWithRetry(request, mapId, nextDelay)
                    }, nextDelay)
                } else {
                    // We've exhausted our retry attempts
                    Log.e(TAG, "Exhausted retries for $mapId")
                    _featureState.value =
                        MapFeatureState.Failed(
                            0,
                            StringDesc
                                .Resource(
                                    MokoRes.strings.map_error_failed_after_retries,
                                ).toString(getApplication()),
                        )
                }
            }
    }

    // ------------------------------------------------------------------------

    fun cancelDownload() {
        if (currentSessionId > 0) {
            Log.i(TAG, "cancelDownload session=$currentSessionId")
            splitInstallManager.cancelInstall(currentSessionId)
            // State will be updated via the listener when cancellation completes
        }
    }

    // ------------------------------------------------------------------------

    override fun onCleared() {
        installStateListener?.let {
            splitInstallManager.unregisterListener(it)
        }
        super.onCleared()
    }
}
