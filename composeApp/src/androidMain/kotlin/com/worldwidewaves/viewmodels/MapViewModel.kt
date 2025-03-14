package com.worldwidewaves.viewmodels

/*
 * Copyright 2024 DrWave
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
import android.content.Context
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
import com.worldwidewaves.shared.getMapFileAbsolutePath
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Represents possible states during map feature installation.
 */
sealed class MapFeatureState {
    data object NotChecked : MapFeatureState()
    data object Available : MapFeatureState()
    data object NotAvailable : MapFeatureState()
    data object Pending : MapFeatureState()
    data class Downloading(val progress: Int) : MapFeatureState()
    data object Installed : MapFeatureState()
    data class Failed(val errorCode: Int, val errorMessage: String? = null) : MapFeatureState()
    data class RequiresUserConfirmation(val sessionState: SplitInstallSessionState) : MapFeatureState()
    data object Canceling : MapFeatureState()
    data object Unknown : MapFeatureState()
    data class Retrying(val attempt: Int, val maxAttempts: Int) : MapFeatureState()
}

/**
 * ViewModel that manages downloading and installing dynamic feature modules for maps.
 * Follows Google's Play Feature Delivery best practices.
 */
class MapFeatureViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context
        get() = getApplication()

    private val splitInstallManager: SplitInstallManager = SplitInstallManagerFactory.create(context)
    private val _featureState = MutableStateFlow<MapFeatureState>(MapFeatureState.NotChecked)
    val featureState: StateFlow<MapFeatureState> = _featureState

    private var currentSessionId = 0
    private var installStateListener: SplitInstallStateUpdatedListener? = null
    private var currentMapId: String? = null

    private var retryCount = 0
    private val maxRetries = 3
    private val retryDelayMillis = 1000L // Base delay for exponential backoff

    init {
        registerListener()
    }

    private fun registerListener() {
        installStateListener = SplitInstallStateUpdatedListener { state ->
            // Only process updates for our current session
            if (state.sessionId() == currentSessionId || currentSessionId == 0) {
                updateStateFromInstallState(state)
            }
        }
        installStateListener?.let {
            splitInstallManager.registerListener(it)
        }
    }

    override fun onCleared() {
        installStateListener?.let {
            splitInstallManager.unregisterListener(it)
        }
        super.onCleared()
    }

    private fun updateStateFromInstallState(state: SplitInstallSessionState) {
        // Special handling for SERVICE_DIED error
        if (state.status() == SplitInstallSessionStatus.FAILED &&
            state.errorCode() == SplitInstallErrorCode.SERVICE_DIED) {

            // Get current module from state if possible, otherwise use last known module
            val moduleId = getCurrentModuleFromState(state) ?: currentMapId

            if (moduleId != null && retryCount < maxRetries) {
                // Prepare retry with exponential backoff
                val delay = retryDelayMillis * (1 shl retryCount)
                retryCount++

                _featureState.value = MapFeatureState.Retrying(retryCount, maxRetries)

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
                val totalBytes = state.totalBytesToDownload()
                val downloadedBytes = state.bytesDownloaded()
                val progressPercent = if (totalBytes > 0) {
                    (downloadedBytes * 100 / totalBytes).toInt()
                } else 0

                _featureState.value = MapFeatureState.Downloading(progressPercent)
            }
            SplitInstallSessionStatus.DOWNLOADED -> {
                _featureState.value = MapFeatureState.Downloading(100)
            }
            SplitInstallSessionStatus.INSTALLING -> {
                _featureState.value = MapFeatureState.Downloading(100)
            }
            SplitInstallSessionStatus.INSTALLED -> {
                _featureState.value = MapFeatureState.Installed
                currentSessionId = 0
            }
            SplitInstallSessionStatus.PENDING -> {
                _featureState.value = MapFeatureState.Pending
            }
            SplitInstallSessionStatus.FAILED -> {
                _featureState.value = MapFeatureState.Failed(
                    state.errorCode(),
                    getErrorMessage(state.errorCode())
                )
                currentSessionId = 0
            }
            SplitInstallSessionStatus.CANCELED -> {
                _featureState.value = MapFeatureState.NotAvailable
                currentSessionId = 0
            }
            SplitInstallSessionStatus.CANCELING -> {
                _featureState.value = MapFeatureState.Canceling
            }
            SplitInstallSessionStatus.REQUIRES_USER_CONFIRMATION -> {
                _featureState.value = MapFeatureState.RequiresUserConfirmation(state)
            }
            SplitInstallSessionStatus.UNKNOWN -> {
                _featureState.value = MapFeatureState.Unknown
            }
            else -> {
                _featureState.value = MapFeatureState.Unknown
            }
        }
    }

    // Utility method to extract module ID from session state
    private fun getCurrentModuleFromState(state: SplitInstallSessionState): String? {
        return try {
            state.moduleNames().firstOrNull()
        } catch (e: Exception) {
            null
        }
    }

    private fun getErrorMessage(errorCode: Int): String {
        return when (errorCode) {
            SplitInstallErrorCode.NETWORK_ERROR ->
                "Network error. Please check your connection."
            SplitInstallErrorCode.INSUFFICIENT_STORAGE ->
                "Insufficient storage space."
            SplitInstallErrorCode.MODULE_UNAVAILABLE ->
                "Map module unavailable."
            SplitInstallErrorCode.ACTIVE_SESSIONS_LIMIT_EXCEEDED ->
                "Too many sessions in progress. Try again later."
            SplitInstallErrorCode.INVALID_REQUEST ->
                "Invalid request. Please try again."
            SplitInstallErrorCode.API_NOT_AVAILABLE ->
                "Play Store API is not available on this device."
            SplitInstallErrorCode.INCOMPATIBLE_WITH_EXISTING_SESSION ->
                "Incompatible with existing session."
            SplitInstallErrorCode.SERVICE_DIED ->
                "Service died unexpectedly. Please try again."
            SplitInstallErrorCode.ACCESS_DENIED ->
                "Access denied."
            else -> "Unknown error occurred: $errorCode"
        }
    }

    /**
     * Checks if a map module is available/installed.
     */
    fun checkIfMapIsAvailable(mapId: String) {
        viewModelScope.launch {
            currentMapId = mapId
            if (splitInstallManager.installedModules.contains(mapId)) {
                _featureState.value = MapFeatureState.Available
            } else {
                _featureState.value = MapFeatureState.NotAvailable
            }
        }
    }

    /**
     * Requests installation of a map module.
     */
    fun downloadMap(mapId: String) {
        if (_featureState.value is MapFeatureState.Downloading ||
            _featureState.value is MapFeatureState.Pending) {
            return // Already downloading
        }

        _featureState.value = MapFeatureState.Pending
        retryCount = 0 // Reset retry count
        currentMapId = mapId

        val request = SplitInstallRequest.newBuilder()
            .addModule(mapId)
            .build()

        startInstallWithRetry(request, mapId)
    }

    private fun startInstallWithRetry(request: SplitInstallRequest, mapId: String, delay: Long = retryDelayMillis) {
        splitInstallManager.startInstall(request)
            .addOnSuccessListener { sessionId ->
                currentSessionId = sessionId
                retryCount = 0 // Reset retry count on success
            }
            .addOnFailureListener { exception ->
                // Check if it's worth retrying
                if (retryCount < maxRetries) {
                    // Use exponential backoff for retries
                    val nextDelay = delay * (1 shl retryCount)
                    retryCount++

                    _featureState.value = MapFeatureState.Retrying(retryCount, maxRetries)

                    // Schedule retry after delay
                    Handler(Looper.getMainLooper()).postDelayed({
                        startInstallWithRetry(request, mapId, nextDelay)
                    }, nextDelay)
                } else {
                    // We've exhausted our retry attempts
                    _featureState.value = MapFeatureState.Failed(0,
                        exception.message ?: "Failed to start download after multiple attempts"
                    )
                }
            }
    }

    fun cancelDownload() {
        if (currentSessionId > 0) {
            splitInstallManager.cancelInstall(currentSessionId)
            // State will be updated via the listener when cancellation completes
        }
    }

    fun isMapReady(mapId: String): Boolean {
        return splitInstallManager.installedModules.contains(mapId)
    }

    fun getMapFilePath(mapId: String, extension: String): String? {
        if (!isMapReady(mapId)) {
            return null
        }

        // This uses runBlocking which is generally not recommended in a ViewModel,
        // but here it's used to provide a synchronous API for simplicity
        return runBlocking {
            getMapFileAbsolutePath(mapId, extension)
        }
    }

}