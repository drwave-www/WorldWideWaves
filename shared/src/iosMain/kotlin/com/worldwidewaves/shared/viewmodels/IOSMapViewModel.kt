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

import com.worldwidewaves.shared.map.PlatformMapManager
import com.worldwidewaves.shared.ui.BaseViewModel
import com.worldwidewaves.shared.utils.Log
import com.worldwidewaves.shared.viewmodels.MapDownloadManager
import com.worldwidewaves.shared.viewmodels.PlatformMapDownloadAdapter
import kotlinx.coroutines.launch

/**
 * iOS implementation of MapViewModel using pure business logic composition.
 * Handles iOS-specific ODR integration while delegating download logic to MapDownloadLogic.
 *
 * RESPONSIBILITIES:
 * • iOS UI lifecycle management (BaseViewModel)
 * • ODR (On-Demand Resources) integration
 * • iOS-specific error message localization
 * • Delegates business logic to MapDownloadLogic
 */
class IOSMapViewModel(
    private val platformMapManager: PlatformMapManager,
) : BaseViewModel(),
    MapViewModel {
    // Platform adapter for business logic
    private val platformAdapter =
        object : PlatformMapDownloadAdapter {
            override suspend fun isMapInstalled(mapId: String): Boolean = platformMapManager.isMapAvailable(mapId)

            override suspend fun startPlatformDownload(
                mapId: String,
                onMapDownloaded: (() -> Unit)?,
            ) {
                try {
                    platformMapManager.downloadMap(
                        mapId = mapId,
                        onProgress = { progress ->
                            downloadManager.handleDownloadProgress(100, progress.toLong())
                        },
                        onSuccess = {
                            downloadManager.handleDownloadSuccess()
                            onMapDownloaded?.invoke()
                            Log.i("IOSMapViewModel", "Map download completed successfully: $mapId")
                        },
                        onError = { errorCode, message ->
                            downloadManager.handleDownloadFailure(errorCode, shouldRetry = false)
                            Log.e("IOSMapViewModel", "Map download failed for $mapId: $message (code: $errorCode)")
                        },
                    )
                } catch (e: Exception) {
                    downloadManager.handleDownloadFailure(-1, shouldRetry = false)
                    Log.e("IOSMapViewModel", "Exception during map download: $mapId", throwable = e)
                }
            }

            override suspend fun cancelPlatformDownload() {
                downloadManager.currentMapId?.let { mapId ->
                    platformMapManager.cancelDownload(mapId)
                    downloadManager.handleDownloadCancellation()
                }
            }

            override fun getLocalizedErrorMessage(errorCode: Int): String =
                when (errorCode) {
                    -1 -> "ODR resource download failed"
                    -2 -> "Unknown error during ODR download"
                    -3 -> "Download already in progress"
                    else -> "Unknown error (code: $errorCode)"
                }

            override fun clearCacheForInstalledMaps(mapIds: List<String>) {
                Log.d("IOSMapViewModel", "clearCacheForInstalledMaps for: ${mapIds.joinToString()}")
                // iOS: ODR manages its own cache, no manual clearing needed
            }
        }

    // Pure business logic (no UI lifecycle concerns)
    private val downloadManager: MapDownloadManager = MapDownloadManager(platformAdapter)

    // Delegate public interface to business logic (same pattern as AndroidMapViewModel)
    override val featureState = downloadManager.featureState

    override fun checkIfMapIsAvailable(
        mapId: String,
        autoDownload: Boolean,
    ) {
        scope.launch {
            downloadManager.checkIfMapIsAvailable(mapId, autoDownload)
        }
    }

    override fun downloadMap(
        mapId: String,
        onMapDownloaded: (() -> Unit)?,
    ) {
        scope.launch {
            downloadManager.downloadMap(mapId, onMapDownloaded)
        }
    }

    override fun cancelDownload() {
        scope.launch {
            downloadManager.cancelDownload()
        }
    }
}
