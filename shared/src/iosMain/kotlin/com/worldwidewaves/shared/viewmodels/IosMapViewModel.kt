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

import com.worldwidewaves.shared.MokoRes
import com.worldwidewaves.shared.events.data.GeoJsonDataProvider
import com.worldwidewaves.shared.map.PlatformMapManager
import com.worldwidewaves.shared.ui.BaseViewModel
import com.worldwidewaves.shared.utils.Log
import dev.icerock.moko.resources.desc.Resource
import dev.icerock.moko.resources.desc.ResourceFormatted
import dev.icerock.moko.resources.desc.StringDesc
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

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
class IosMapViewModel(
    private val platformMapManager: PlatformMapManager,
) : BaseViewModel(),
    MapViewModel,
    KoinComponent {
    private val geoJsonDataProvider: GeoJsonDataProvider by inject()

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
                            Log.i("IosMapViewModel", "Map download completed successfully: $mapId")
                        },
                        onError = { errorCode, message ->
                            downloadManager.handleDownloadFailure(errorCode, shouldRetry = false)
                            Log.e("IosMapViewModel", "Map download failed for $mapId: $message (code: $errorCode)")
                        },
                    )
                } catch (e: Exception) {
                    downloadManager.handleDownloadFailure(-1, shouldRetry = false)
                    Log.e("IosMapViewModel", "Exception during map download: $mapId", throwable = e)
                }
            }

            override suspend fun cancelPlatformDownload() {
                downloadManager.currentMapId?.let { mapId ->
                    platformMapManager.cancelDownload(mapId)
                    downloadManager.handleDownloadCancellation()
                }
            }

            override fun getLocalizedErrorMessage(errorCode: Int): String {
                // Map iOS ODR error codes to localized strings (same pattern as Android)
                val resource =
                    when (errorCode) {
                        -100 -> MokoRes.strings.map_error_insufficient_storage
                        -101 -> MokoRes.strings.map_error_network
                        -102 -> MokoRes.strings.map_error_service_died
                        -103 -> MokoRes.strings.map_error_module_unavailable
                        -104 -> MokoRes.strings.map_error_invalid_request
                        -2 -> MokoRes.strings.map_error_download // Legacy: caching failed
                        -3 -> MokoRes.strings.map_error_download // Legacy: download in progress
                        else -> MokoRes.strings.map_error_unknown
                    }

                return if (resource == MokoRes.strings.map_error_unknown) {
                    // Format with error code for unknown errors
                    StringDesc.ResourceFormatted(resource, errorCode).localized()
                } else {
                    // Use simple localized string
                    StringDesc.Resource(resource).localized()
                }
            }

            override fun clearCacheForInstalledMaps(mapIds: List<String>) {
                Log.d("IosMapViewModel", "clearCacheForInstalledMaps for: ${mapIds.joinToString()}")
                // iOS: ODR manages its own cache, no manual clearing needed
            }
        }

    // Pure business logic (no UI lifecycle concerns)
    private val downloadManager: MapDownloadCoordinator = MapDownloadCoordinator(platformAdapter, geoJsonDataProvider)

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
