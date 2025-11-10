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
import com.worldwidewaves.shared.events.data.GeoJsonDataProvider
import com.worldwidewaves.shared.utils.Log
import com.worldwidewaves.shared.viewmodels.MapDownloadCoordinator
import com.worldwidewaves.shared.viewmodels.MapDownloadUtils
import com.worldwidewaves.shared.viewmodels.MapViewModel
import com.worldwidewaves.shared.viewmodels.PlatformMapDownloadAdapter
import com.worldwidewaves.utils.AndroidMapAvailabilityChecker
import dev.icerock.moko.resources.desc.Resource
import dev.icerock.moko.resources.desc.ResourceFormatted
import dev.icerock.moko.resources.desc.StringDesc
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Android implementation of MapViewModel using pure business logic composition.
 * Handles Android-specific Play Core integration while delegating download logic to MapDownloadCoordinator.
 *
 * RESPONSIBILITIES:
 * • Android UI lifecycle management (AndroidViewModel)
 * • Play Core SplitInstallManager integration
 * • Android-specific error message localization
 * • Play Store listener management
 * • Delegates business logic to MapDownloadCoordinator
 */
class AndroidMapViewModel(
    application: Application,
) : AndroidViewModel(application),
    MapViewModel,
    KoinComponent {
    private val geoJsonDataProvider: GeoJsonDataProvider by inject()
    private val markDownloadedEventAsFavorite: com.worldwidewaves.shared.data.MarkDownloadedEventAsFavorite by inject()
    private val mapAvailabilityChecker: com.worldwidewaves.shared.domain.usecases.MapAvailabilityChecker by inject()

    private companion object {
        private const val TAG = "WWW.ViewModel.MapAndroid"
        private const val PLAY_STORE_AUTH_ERROR_CODE = -100
    }

    // Android-specific Play Core components
    private val splitInstallManager: SplitInstallManager = SplitInstallManagerFactory.create(application)
    private var currentSessionId = 0
    private var installStateListener: SplitInstallStateUpdatedListener? = null
    private var retryRunnable: Runnable? = null
    private val retryHandler = Handler(Looper.getMainLooper())
    private val processedInstalledSessions = mutableSetOf<Int>() // Track processed INSTALLED events

    // Log-once tracking for unavailable maps (prevents spam for undownloaded maps)
    private val loggedUnavailableMaps =
        java.util.Collections.newSetFromMap(
            java.util.concurrent.ConcurrentHashMap<String, Boolean>(),
        )

    // Platform adapter for business logic
    private val platformAdapter =
        object : PlatformMapDownloadAdapter {
            @Suppress("ReturnCount") // Guard clauses for clarity
            override suspend fun isMapInstalled(mapId: String): Boolean {
                // Check forcedUnavailable FIRST (user intention overrides system state)
                // This ensures maps marked for uninstall appear as unavailable even if files still exist
                val androidChecker = mapAvailabilityChecker as? AndroidMapAvailabilityChecker
                if (androidChecker != null) {
                    // Use the reactive state from the availability checker which respects forcedUnavailable
                    val checkerState = androidChecker.mapStates.value[mapId]
                    if (checkerState != null) {
                        // Availability checker has definitive answer
                        return checkerState
                    }
                }

                // Fallback to file-based check if not tracked by availability checker
                val moduleInstalled = splitInstallManager.installedModules.contains(mapId)
                if (!moduleInstalled) {
                    return false
                }

                // Verify that actual map files exist and are accessible
                val cacheDir = application.cacheDir
                val mbtilesFile = java.io.File(cacheDir, "$mapId.mbtiles")
                val geojsonFile = java.io.File(cacheDir, "$mapId.geojson")

                val mbtilesInCache = mbtilesFile.exists() && mbtilesFile.canRead() && mbtilesFile.length() > 0
                val geojsonInCache = geojsonFile.exists() && geojsonFile.canRead() && geojsonFile.length() > 0

                // Fast path: both files already in cache
                if (mbtilesInCache && geojsonInCache) {
                    return true
                }

                // Check if files are accessible from bundled split assets
                // This handles the case where dynamic feature modules are bundled from Android Studio
                // but files haven't been copied to cache yet
                val mbtilesInAssets = if (!mbtilesInCache) isAssetAccessible(mapId, "mbtiles") else true
                val geojsonInAssets = if (!geojsonInCache) isAssetAccessible(mapId, "geojson") else true

                val result = (mbtilesInCache || mbtilesInAssets) && (geojsonInCache || geojsonInAssets)

                if (!result) {
                    // Log once per map (expected for undownloaded maps)
                    if (loggedUnavailableMaps.add(mapId)) {
                        Log.v(
                            TAG,
                            "isMapInstalled: $mapId not available (module=${true}, " +
                                "mbtiles: cache=$mbtilesInCache assets=$mbtilesInAssets, " +
                                "geojson: cache=$geojsonInCache assets=$geojsonInAssets)",
                        )
                    }
                }

                return result
            }

            override suspend fun startPlatformDownload(
                mapId: String,
                onMapDownloaded: (() -> Unit)?,
            ) {
                // NOTE: forcedUnavailable clearing now happens in MapDownloadCoordinator.downloadMap()
                // BEFORE this method is called, ensuring all availability checks see correct state.
                // This is defense-in-depth - the primary clear happens earlier in the download flow.

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
                // Cancel any pending retry
                retryRunnable?.let { runnable ->
                    retryHandler.removeCallbacks(runnable)
                    retryRunnable = null
                    Log.i(TAG, "Cancelled pending retry")
                }

                // Cancel active Play Store download
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

            override suspend fun clearForcedUnavailableIfNeeded(mapId: String) {
                // Android-specific implementation to handle Play Core deferred uninstall
                val androidChecker = mapAvailabilityChecker as? AndroidMapAvailabilityChecker
                if (androidChecker != null) {
                    val wasCleared = androidChecker.clearForcedUnavailable(mapId)
                    if (wasCleared) {
                        Log.i(TAG, "Cleared forcedUnavailable for $mapId before availability check (re-download scenario)")
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

                    // Store the runnable so we can cancel it later if needed
                    retryRunnable =
                        Runnable {
                            viewModelScope.launch {
                                downloadManager.downloadMap(mapId, onMapDownloaded)
                            }
                            retryRunnable = null // Clear after execution
                        }

                    retryHandler.postDelayed(retryRunnable!!, delay)
                } else {
                    downloadManager.handleDownloadFailure(0, shouldRetry = false)
                }
            }
        }

    // Pure business logic (no UI lifecycle concerns)
    private val downloadManager: MapDownloadCoordinator =
        MapDownloadCoordinator(
            platformAdapter = platformAdapter,
            geoJsonDataProvider = geoJsonDataProvider,
            markDownloadedEventAsFavorite = markDownloadedEventAsFavorite,
            coroutineScope = viewModelScope,
        )

    // Delegate public interface to business logic
    override val featureState = downloadManager.featureState

    init {
        registerAndroidListeners()
    }

    /**
     * Checks if a specific asset file is accessible in the split context.
     * Used to verify bundled dynamic feature module assets.
     *
     * @param mapId The map/event ID
     * @param extension The file extension (without dot)
     * @return true if the asset can be opened, false otherwise
     */
    private fun isAssetAccessible(
        mapId: String,
        extension: String,
    ): Boolean {
        val assetName = "$mapId.$extension"
        return try {
            // Try to create split context for this map
            val splitContext =
                if (android.os.Build.VERSION.SDK_INT >= 26) {
                    runCatching {
                        getApplication<Application>().createContextForSplit(mapId)
                    }.getOrNull() ?: getApplication()
                } else {
                    getApplication()
                }

            // Try to open the asset (just check existence, don't read)
            splitContext.assets.open(assetName).use { true }
        } catch (
            @Suppress("SwallowedException") _: Exception,
        ) {
            // Asset doesn't exist - expected for undownloaded maps
            false
        }
    }

    // ------------------------------------------------------------------------
    // Public API (delegates to shared logic)
    // ------------------------------------------------------------------------

    override fun checkIfMapIsAvailable(
        mapId: String,
        autoDownload: Boolean,
    ) {
        viewModelScope.launch {
            // First, check if there's an existing download session for this map
            queryExistingDownloadSession(mapId)

            downloadManager.checkIfMapIsAvailable(mapId, autoDownload)
        }
    }

    /**
     * Queries PlayCore for existing download sessions and reconnects to them.
     * This handles cases where download was started in a previous session or different screen.
     */
    private fun queryExistingDownloadSession(mapId: String) {
        try {
            splitInstallManager
                .sessionStates
                .addOnSuccessListener { sessionStates ->
                    val existingSession =
                        sessionStates.firstOrNull { state ->
                            state.moduleNames().contains(mapId)
                        }

                    if (existingSession != null && existingSession.sessionId() != currentSessionId) {
                        Log.i(
                            TAG,
                            "Found existing download session for $mapId: " +
                                "id=${existingSession.sessionId()}, status=${existingSession.status()}",
                        )
                        currentSessionId = existingSession.sessionId()
                        // Process current state to update UI
                        processAndroidInstallState(existingSession)
                    }
                }.addOnFailureListener { e ->
                    Log.w(TAG, "Failed to query existing download sessions: ${e.message}")
                }
        } catch (e: Exception) {
            Log.w(TAG, "Exception querying download sessions: ${e.message}")
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
                // Deduplicate: Only process each session's INSTALLED event once
                val sessionId = state.sessionId()
                if (sessionId in processedInstalledSessions) {
                    Log.d(TAG, "Ignoring duplicate INSTALLED event for session=$sessionId")
                    return
                }
                processedInstalledSessions.add(sessionId)

                Log.i(TAG, "Status: INSTALLED – modules=${state.moduleNames()}")
                val moduleIds = getModuleIdsFromState(state)

                // Reset log flags for installed modules (allow logging if issues persist)
                moduleIds.forEach { mapId ->
                    loggedUnavailableMaps.remove(mapId)
                    Log.v(TAG, "Reset unavailable log flag for $mapId after installation")
                }

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
        // Clean up listeners and handlers
        installStateListener?.let {
            splitInstallManager.unregisterListener(it)
        }
        retryRunnable?.let { runnable ->
            retryHandler.removeCallbacks(runnable)
        }
        super.onCleared()
    }
}
