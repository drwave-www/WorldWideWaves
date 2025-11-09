package com.worldwidewaves.shared.map

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

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitViewController
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.worldwidewaves.shared.MokoRes
import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.events.utils.Polygon
import com.worldwidewaves.shared.events.utils.Position
import com.worldwidewaves.shared.map.EventMapDownloadManager
import com.worldwidewaves.shared.ui.components.DownloadProgressIndicator
import com.worldwidewaves.shared.ui.components.LoadingIndicator
import com.worldwidewaves.shared.utils.Log
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.koin.mp.KoinPlatform
import platform.UIKit.UIImage

/**
 * iOS EventMap implementation with MapLibre integration.
 *
 * Provides map display with download management, wave polygon rendering,
 * and user position tracking using safe iOS dependency injection patterns.
 */
class IosEventMap(
    event: IWWWEvent,
    private val onMapLoaded: () -> Unit = {},
    onLocationUpdate: (Position) -> Unit = {},
    mapConfig: EventMapConfig = EventMapConfig(),
    private val onMapClick: (() -> Unit)? = null,
    private val registryKey: String? = null, // Optional unique key for wrapper registry (prevents conflicts)
    private val mapViewModel: com.worldwidewaves.shared.viewmodels.MapViewModel? = null, // For syncing download state
) : AbstractEventMap<UIImage>(event, mapConfig, onLocationUpdate) {
    // Use unique registry key if provided (e.g., "paris_france-fullmap" vs "paris_france-event")
    // This prevents wrapper conflicts when multiple screens show the same event
    private val mapRegistryKey = registryKey ?: event.id

    // Create event-specific adapter instance (not singleton) to enable per-event camera control
    override val mapLibreAdapter: MapLibreAdapter<UIImage> =
        IosMapLibreAdapter(mapRegistryKey)
    override val locationProvider: LocationProvider? =
        KoinPlatform.getKoin().getOrNull<LocationProvider>()

    private var currentPolygons = mutableListOf<Polygon>()
    private val mapScope = CoroutineScope(SupervisorJob())
    private var setupMapCalled = false

    @OptIn(ExperimentalForeignApi::class)
    override fun updateWavePolygons(
        wavePolygons: List<Polygon>,
        clearPolygons: Boolean,
    ) {
        if (clearPolygons) currentPolygons.clear()
        currentPolygons.addAll(wavePolygons)

        storePolygonsForRendering(wavePolygons, clearPolygons)

        // Render immediately if wrapper ready, otherwise queue for async render
        val wrapper = MapWrapperRegistry.getWrapper(mapRegistryKey)
        if (wrapper != null && MapWrapperRegistry.isStyleLoaded(mapRegistryKey)) {
            MapWrapperRegistry.getRenderCallback(mapRegistryKey)?.invoke()
        } else {
            MapWrapperRegistry.requestImmediateRender(mapRegistryKey)
        }
    }

    private fun storePolygonsForRendering(
        polygons: List<Polygon>,
        clearExisting: Boolean,
    ) {
        val coordinates: List<List<Pair<Double, Double>>> =
            polygons.map { polygon -> polygon.map { Pair(it.lat, it.lng) } }

        MapWrapperRegistry.setPendingPolygons(mapRegistryKey, coordinates, clearExisting)
    }

    @OptIn(ExperimentalForeignApi::class)
    @Composable
    @Suppress("LongMethod", "CyclomaticComplexMethod")
    override fun Draw(
        autoMapDownload: Boolean,
        modifier: Modifier,
    ) {
        // Get platform map manager for download coordination
        val platformMapManager = KoinPlatform.getKoin().get<PlatformMapManager>()

        val downloadCoordinator = remember { EventMapDownloadManager(platformMapManager) }
        val downloadState by downloadCoordinator.getDownloadState(event.id).collectAsState()

        DisposableEffect(mapRegistryKey) {
            onDispose {
                Log.d("IosEventMap", "Disposing map for event: $mapRegistryKey, cancelling mapScope and adapter")
                mapScope.cancel()
                (mapLibreAdapter as? IosMapLibreAdapter)?.cleanup()
                MapWrapperRegistry.unregisterWrapper(mapRegistryKey)
            }
        }

        LaunchedEffect(event.id, autoMapDownload) {
            downloadCoordinator.autoDownloadIfNeeded(event.id, autoMapDownload)
        }

        LaunchedEffect(mapRegistryKey, onMapClick) {
            onMapClick?.let {
                MapWrapperRegistry.requestMapClickCallbackRegistration(mapRegistryKey, it)
            }
        }

        Box(modifier = modifier.fillMaxSize()) {
            // Static map image as fallback background (matches Android implementation)
            Image(
                modifier = Modifier.fillMaxSize(),
                painter = painterResource(event.getMapImage() as DrawableResource),
                contentDescription = "defaultMap",
                contentScale = ContentScale.Crop,
            )

            // Cache view controller at top level to prevent deallocation when download state changes
            val viewController =
                remember(event.id) {
                    mutableStateOf<platform.UIKit.UIViewController?>(null)
                }

            var styleURL by remember { mutableStateOf<String?>(null) }
            var styleLoadError by remember { mutableStateOf<String?>(null) }
            val maxRetries = 3

            LaunchedEffect(event.id, downloadState.isAvailable) {
                // Reset error state on new attempt
                styleLoadError = null
                styleURL = null

                // Try to load style with retries
                var retryCount = 0
                while (retryCount < maxRetries && styleURL == null && downloadState.isAvailable) {
                    styleURL = event.map.getStyleUri()

                    if (styleURL == null) {
                        retryCount++
                        if (retryCount < maxRetries) {
                            delay((100 * retryCount).toLong()) // Progressive backoff
                        }
                    }
                }

                // If still null after retries, set error state
                if (styleURL == null && downloadState.isAvailable) {
                    styleLoadError = "Failed to load map style after $maxRetries attempts. Map files may be corrupted."
                }

                // Register setupMap() to be called after style loads
                if (!setupMapCalled && styleURL != null && downloadState.isAvailable) {
                    setupMapCalled = true

                    MapWrapperRegistry.addOnMapReadyCallback(mapRegistryKey) {
                        setupMap(
                            map = UIImage(),
                            scope = mapScope,
                            stylePath = styleURL!!,
                            onMapLoaded = onMapLoaded,
                            onMapClick = onMapClick?.let { callback -> { _: Double, _: Double -> callback() } },
                        )
                    }
                }
            }

            LaunchedEffect(event.id, styleURL) {
                if (styleURL != null && viewController.value == null) {
                    // Enable gestures only for full map screen (WINDOW + autoTarget)
                    val enableGestures =
                        mapConfig.initialCameraPosition == MapCameraPosition.WINDOW &&
                            mapConfig.autoTargetUserOnFirstLocation
                    viewController.value =
                        createNativeMapViewController(event, styleURL!!, enableGestures, mapRegistryKey) as platform.UIKit.UIViewController
                }
            }

            when {
                styleURL != null && downloadState.isAvailable && viewController.value != null -> {
                    key(event.id) {
                        @Suppress("DEPRECATION")
                        UIKitViewController(
                            factory = { viewController.value!! },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }

                styleLoadError != null -> {
                    // Style loading failed after retries
                    MapErrorOverlay(
                        errorMessage = styleLoadError!!,
                        onRetry = {
                            // Reset error and retry
                            MainScope().launch {
                                styleLoadError = null
                                setupMapCalled = false
                                event.map.clearStyleUriCache()
                                downloadCoordinator.autoDownloadIfNeeded(event.id, autoDownload = true)
                            }
                        },
                    )
                }

                downloadState.isDownloading && downloadState.error == null -> {
                    MapDownloadOverlay(
                        progress = downloadState.progress,
                        onCancel = { downloadCoordinator.cancelDownload(event.id) },
                    )
                }

                downloadState.error != null -> {
                    MapErrorOverlay(
                        errorMessage = downloadState.error!!,
                        onRetry = {
                            MainScope().launch {
                                downloadCoordinator.downloadMap(
                                    mapId = event.id,
                                    onDownloadComplete = {
                                        // Sync MapViewModel state after download completes
                                        mapViewModel?.checkIfMapIsAvailable(event.id, autoDownload = false)
                                    },
                                )
                            }
                        },
                    )
                }

                !downloadState.isAvailable && !downloadState.isDownloading -> {
                    MapDownloadButton {
                        MainScope().launch {
                            downloadCoordinator.downloadMap(
                                mapId = event.id,
                                onDownloadComplete = {
                                    // Sync MapViewModel state after download completes
                                    mapViewModel?.checkIfMapIsAvailable(event.id, autoDownload = false)
                                },
                            )
                        }
                    }
                }

                else -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        LoadingIndicator("Loading map...")
                    }
                }
            }
        }
    }

    @Composable
    @Suppress("FunctionName")
    private fun MapDownloadOverlay(
        progress: Int,
        onCancel: () -> Unit,
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background.copy(alpha = 0.8f),
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (progress > 0) {
                    DownloadProgressIndicator(
                        progress = progress,
                        message = stringResource(MokoRes.strings.map_downloading),
                        onCancel = onCancel,
                    )
                } else {
                    LoadingIndicator(message = stringResource(MokoRes.strings.map_starting_download))
                }
            }
        }
    }

    @Composable
    @Suppress("FunctionName")
    private fun MapErrorOverlay(
        errorMessage: String,
        onRetry: () -> Unit,
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background.copy(alpha = 0.9f),
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "Download Failed",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.error,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onRetry) {
                    Text("Retry")
                }
            }
        }
    }

    @Composable
    @Suppress("FunctionName")
    private fun MapDownloadButton(onClick: () -> Unit) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background.copy(alpha = 0.5f),
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Button(onClick = onClick) {
                    Text(
                        text = stringResource(MokoRes.strings.map_download),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }
    }
}
