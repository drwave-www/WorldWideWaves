package com.worldwidewaves.viewmodels

/*
 * Copyright 2025 DrWave
 *
 * WorldWideWaves is an ephemeral mobile app designed to orchestrate human waves through cities and
 * countries, culminating in a global wave. The project aims to transcend physical and cultural
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
import dev.icerock.moko.resources.desc.Resource
import dev.icerock.moko.resources.desc.ResourceFormatted
import dev.icerock.moko.resources.desc.StringDesc
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Represents possible states during map feature installation.
 */
sealed class MapFeatureState {
    data object NotChecked : MapFeatureState()
    data object Available : MapFeatureState()
    data object NotAvailable : MapFeatureState()
    data object Pending : MapFeatureState()
    data class Downloading(val progress: Int) : MapFeatureState()
    data object Installing : MapFeatureState()
    data object Installed : MapFeatureState()
    data class Failed(val errorCode: Int, val errorMessage: String? = null) : MapFeatureState()
    data class RequiresUserConfirmation(val sessionState: SplitInstallSessionState) : MapFeatureState()
    data object Canceling : MapFeatureState()
    data object Unknown : MapFeatureState()
    data class Retrying(val attempt: Int, val maxAttempts: Int) : MapFeatureState()
}

// ----------------------------------------------------------------------------

/**
 * ViewModel that manages downloading and installing dynamic feature modules for maps.
 * Follows Google's Play Feature Delivery best practices.
 */
class MapViewModel(application: Application) : AndroidViewModel(application) {

    /* --------------------------------------------------------------------- */
    /*  Logging                                                              */
    /* --------------------------------------------------------------------- */
    private companion object {
        private const val TAG = "MapInstall"
    }

    private val splitInstallManager: SplitInstallManager = SplitInstallManagerFactory.create(application)

    private val _featureState = MutableStateFlow<MapFeatureState>(MapFeatureState.NotChecked)
    val featureState: StateFlow<MapFeatureState> = _featureState

    private var currentSessionId = 0
    private var installStateListener: SplitInstallStateUpdatedListener? = null
    private var currentMapId: String? = null

    private var retryCount = 0
    private val maxRetries = 3
    private val retryDelayMillis = 1000L // Base delay for exponential backoff

    // ------------------------------------------------------------------------

    init {
        registerListener()
    }

    // ------------------------------------------------------------------------

    private fun registerListener() {
        Log.d(TAG, "registerListener()")
        installStateListener = SplitInstallStateUpdatedListener { state ->
            // Only process updates for our current session
            Log.d(
                TAG,
                "Listener update  session=${state.sessionId()}  status=${state.status()}  err=${state.errorCode()}  modules=${state.moduleNames()}  " +
                    "bytes=${state.bytesDownloaded()}/${state.totalBytesToDownload()}"
            )
            
            if (state.sessionId() != currentSessionId && currentSessionId != 0) {
                Log.d(TAG, "Ignoring state for unrelated session ${state.sessionId()}")
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

        // Special handling for SERVICE_DIED error
        @Suppress("DEPRECATION")
        if (state.status() == SplitInstallSessionStatus.FAILED &&
            state.errorCode() == SplitInstallErrorCode.SERVICE_DIED) {

            Log.w(TAG, "SERVICE_DIED for session ${state.sessionId()} – scheduling retry")
            // Get current module from state if possible, otherwise use last known module
            val moduleId = getCurrentModuleFromState(state) ?: currentMapId

            if (moduleId != null && retryCount < maxRetries) {

                // Prepare retry with exponential backoff
                val delay = retryDelayMillis * (1 shl retryCount)
                retryCount++

                _featureState.value = MapFeatureState.Retrying(retryCount, maxRetries)
                Log.i(TAG, "Retry #$retryCount for $moduleId after ${delay}ms")

                // Create a new request and retry
                val request = SplitInstallRequest.newBuilder()
                    .addModule(moduleId)
                    .build()

                Handler(Looper.getMainLooper()).postDelayed({
                    startInstallWithRetry(request, moduleId, delay)
                }, delay)

                return
            }
        }

        when (state.status()) {
            SplitInstallSessionStatus.DOWNLOADING -> {
                Log.d(TAG, "Status: DOWNLOADING bytes=${state.bytesDownloaded()}/${state.totalBytesToDownload()}")
                val totalBytes = state.totalBytesToDownload()
                val downloadedBytes = state.bytesDownloaded()
                val progressPercent = if (totalBytes > 0) {
                    (downloadedBytes * 100 / totalBytes).toInt()
                } else 0

                _featureState.value = MapFeatureState.Downloading(progressPercent)
            }
            SplitInstallSessionStatus.DOWNLOADED -> {
                Log.d(TAG, "Status: DOWNLOADED")
                _featureState.value = MapFeatureState.Downloading(100)
            }
            SplitInstallSessionStatus.INSTALLING -> {
                Log.d(TAG, "Status: INSTALLING")
                _featureState.value = MapFeatureState.Installing
            }
            SplitInstallSessionStatus.INSTALLED -> {
                Log.i(TAG, "Status: INSTALLED – modules=${state.moduleNames()}")
                // ------------------------------------------------------------------
                //  Invalidate any cached resources that belong to the module(s)
                //  that have just been installed to ensure we don't use stale files.
                // ------------------------------------------------------------------
                try {
                    // Prefer the module list from the state; fall back to currentMapId
                    val moduleIds: List<String> =
                        try {
                            state.moduleNames().toList()
                        } catch (_: Exception) {
                            emptyList()
                        }.ifEmpty {
                            currentMapId?.let { listOf(it) }.orEmpty()
                        }

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

                _featureState.value = MapFeatureState.Installed
                currentSessionId = 0
            }
            SplitInstallSessionStatus.PENDING -> {
                Log.d(TAG, "Status: PENDING")
                _featureState.value = MapFeatureState.Pending
            }
            SplitInstallSessionStatus.FAILED -> {
                Log.e(TAG, "Status: FAILED code=${state.errorCode()}")
                _featureState.value = MapFeatureState.Failed(
                    state.errorCode(),
                    getErrorMessage(state.errorCode())
                )
                currentSessionId = 0
            }
            SplitInstallSessionStatus.CANCELED -> {
                Log.w(TAG, "Status: CANCELED")
                _featureState.value = MapFeatureState.NotAvailable
                currentSessionId = 0
            }
            SplitInstallSessionStatus.CANCELING -> {
                Log.w(TAG, "Status: CANCELING")
                _featureState.value = MapFeatureState.Canceling
            }
            SplitInstallSessionStatus.REQUIRES_USER_CONFIRMATION -> {
                Log.i(TAG, "Status: REQUIRES_USER_CONFIRMATION")
                _featureState.value = MapFeatureState.RequiresUserConfirmation(state)
            }
            SplitInstallSessionStatus.UNKNOWN -> {
                Log.w(TAG, "Status: UNKNOWN")
                _featureState.value = MapFeatureState.Unknown
            }
            else -> {
                Log.w(TAG, "Status: default->UNKNOWN")
                _featureState.value = MapFeatureState.Unknown
            }
        }
    }

    // ------------------------------------------------------------------------

    // Utility method to extract module ID from session state
    private fun getCurrentModuleFromState(state: SplitInstallSessionState): String? {
        return try {
            state.moduleNames().firstOrNull()
        } catch (_: Exception) {
            null
        }
    }

    // ------------------------------------------------------------------------

    private fun getErrorMessage(errorCode: Int): String {
        @Suppress("DEPRECATION")
        val res = when (errorCode) {
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

    fun checkIfMapIsAvailable(mapId: String, autoDownload: Boolean = false) {
        viewModelScope.launch {
            Log.d(TAG, "checkIfMapIsAvailable id=$mapId auto=$autoDownload")
            currentMapId = mapId
            if (splitInstallManager.installedModules.contains(mapId)) {
                Log.d(TAG, "Map $mapId already installed")
                _featureState.value = MapFeatureState.Available
            } else {
                Log.d(TAG, "Map $mapId NOT installed")
                _featureState.value = MapFeatureState.NotAvailable
                // Only auto-download if explicitly requested
                if (autoDownload) {
                    Log.d(TAG, "Auto-downloading $mapId")
                    downloadMap(mapId)
                }
            }
        }
    }

    // ------------------------------------------------------------------------

    fun downloadMap(mapId: String, onMapDownloaded: (() -> Unit)? = null) {
        Log.i(TAG, "downloadMap called for $mapId")
        if (_featureState.value is MapFeatureState.Downloading ||
            _featureState.value is MapFeatureState.Pending) {
            Log.w(TAG, "downloadMap ignored – already downloading")
            return // Already downloading
        }

        _featureState.value = MapFeatureState.Pending
        retryCount = 0 // Reset retry count
        currentMapId = mapId

        val request = SplitInstallRequest.newBuilder()
            .addModule(mapId)
            .build()

        startInstallWithRetry(request, mapId, onMapDownloaded = onMapDownloaded)
    }

    // ------------------------------------------------------------------------

    private fun startInstallWithRetry(
        request: SplitInstallRequest,
        mapId: String,
        delay: Long = retryDelayMillis,
        onMapDownloaded: (() -> Unit)? = null
    ) {
        splitInstallManager.startInstall(request)
            .addOnSuccessListener { sessionId ->
                Log.i(TAG, "startInstall success session=$sessionId")
                currentSessionId = sessionId
                retryCount = 0 // Reset retry count on success
                onMapDownloaded?.invoke()
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "startInstall failure ${exception.message}")
                // Check if it's worth retrying
                if (retryCount < maxRetries) {
                    // Use exponential backoff for retries
                    val nextDelay = delay * (1 shl retryCount)
                    retryCount++

                    _featureState.value = MapFeatureState.Retrying(retryCount, maxRetries)
                    Log.i(TAG, "Scheduling retry #$retryCount after ${nextDelay}ms")

                    // Schedule retry after delay
                    Handler(Looper.getMainLooper()).postDelayed({
                        startInstallWithRetry(request, mapId, nextDelay)
                    }, nextDelay)
                } else {
                    // We've exhausted our retry attempts
                    Log.e(TAG, "Exhausted retries for $mapId")
                    _featureState.value = MapFeatureState.Failed(
                        0,
                        StringDesc.Resource(
                            MokoRes.strings.map_error_failed_after_retries
                        ).toString(getApplication())
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
        } else {
            Log.d(TAG, "cancelDownload ignored – no active session")
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