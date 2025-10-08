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
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.worldwidewaves.shared.position.PositionManager
import com.worldwidewaves.shared.ui.components.DownloadProgressIndicator
import com.worldwidewaves.shared.ui.components.LoadingIndicator
import com.worldwidewaves.shared.utils.Log
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.koin.mp.KoinPlatform
import platform.UIKit.UIImage

/**
 * iOS implementation of EventMap providing functional map display.
 *
 * This implementation provides a rich map experience for iOS users with:
 * - Static map image display (same as Android fallback)
 * - Location information and wave status
 * - Map download status and controls
 * - User position and wave polygon information display
 * - Proper iOS integration using safe dependency injection
 */
class IosEventMap(
    event: IWWWEvent,
    private val onMapLoaded: () -> Unit = {},
    onLocationUpdate: (Position) -> Unit = {},
    mapConfig: EventMapConfig = EventMapConfig(),
    private val onMapClick: (() -> Unit)? = null,
) : AbstractEventMap<UIImage>(event, mapConfig, onLocationUpdate) {
    // Create event-specific adapter instance (not singleton) to enable per-event camera control
    override val mapLibreAdapter: MapLibreAdapter<UIImage> =
        IosMapLibreAdapter(event.id)
    override val locationProvider: LocationProvider? =
        KoinPlatform.getKoin().getOrNull<LocationProvider>()

    private var currentPolygons = mutableListOf<Polygon>()
    private val mapScope = CoroutineScope(SupervisorJob())
    private var setupMapCalled = false

    override fun updateWavePolygons(
        wavePolygons: List<Polygon>,
        clearPolygons: Boolean,
    ) {
        val timestamp = System.currentTimeMillis()
        Log.i("IosEventMap", "🌊 [$timestamp] updateWavePolygons: ${wavePolygons.size} polygons, clear=$clearPolygons")

        if (clearPolygons) {
            currentPolygons.clear()
        }
        currentPolygons.addAll(wavePolygons)

        Log.d("IosEventMap", "iOS map now tracking ${currentPolygons.size} wave polygons")

        // Store polygon data in registry for Swift to render
        storePolygonsForRendering(wavePolygons, clearPolygons)
        Log.v("IosEventMap", "✅ [$timestamp] Polygons stored in registry")
    }

    private fun storePolygonsForRendering(
        polygons: List<Polygon>,
        clearExisting: Boolean,
    ) {
        // Convert polygons to simple coordinate pairs (lat, lng)
        val coordinates: List<List<Pair<Double, Double>>> =
            polygons.map { polygon: Polygon ->
                polygon.map { position: Position ->
                    Pair(position.lat, position.lng)
                }
            }

        // Store in registry - Swift will poll and render these
        MapWrapperRegistry.setPendingPolygons(event.id, coordinates, clearExisting)
        Log.i("IosEventMap", "Stored ${polygons.size} polygons in registry for Swift to render")
    }

    @OptIn(ExperimentalForeignApi::class)
    @Composable
    override fun Draw(
        autoMapDownload: Boolean,
        modifier: Modifier,
    ) {
        Log.i("IosEventMap", "Draw() called for event: ${event.id}, autoMapDownload=$autoMapDownload")

        // Get unified position from PositionManager (same as Android)
        val positionManager = KoinPlatform.getKoin().get<PositionManager>()
        val platformMapManager = KoinPlatform.getKoin().get<PlatformMapManager>()

        // Use shared EventMapDownloadManager for download state management
        val downloadCoordinator =
            remember {
                Log.d("IosEventMap", "Creating EventMapDownloadManager for: ${event.id}")
                EventMapDownloadManager(platformMapManager)
            }
        val downloadState by downloadCoordinator.getDownloadState(event.id).collectAsState()

        var mapIsLoaded by remember { mutableStateOf(false) }

        // Check map availability and trigger auto-download if needed
        LaunchedEffect(event.id, autoMapDownload) {
            Log.i("IosEventMap", "LaunchedEffect triggered: event=${event.id}, autoDownload=$autoMapDownload")
            downloadCoordinator.autoDownloadIfNeeded(event.id, autoMapDownload)
            Log.d("IosEventMap", "autoDownloadIfNeeded completed for: ${event.id}")
        }

        // Register map click callback if provided
        LaunchedEffect(event.id, onMapClick) {
            if (onMapClick != null) {
                Log.i("IosEventMap", "👆 Registering map click callback for: ${event.id}")
                MapWrapperRegistry.setMapClickCallback(event.id, onMapClick)
                Log.i("IosEventMap", "✅ Map click callback registered for: ${event.id}")
            } else {
                Log.w("IosEventMap", "⚠️ No map click callback provided for: ${event.id}")
            }
        }

        // Initialize position system integration (same as AbstractEventMap)
        LaunchedEffect(Unit) {
            // Start location updates and integrate with PositionManager
            locationProvider?.startLocationUpdates { rawPosition ->
                // Update PositionManager with GPS position
                positionManager.updatePosition(PositionManager.PositionSource.GPS, rawPosition)
            }

            mapIsLoaded = true
            onMapLoaded()
        }

        Box(modifier = modifier.fillMaxSize()) {
            // Static map image as fallback background (matches Android implementation)
            Image(
                modifier = Modifier.fillMaxSize(),
                painter = painterResource(event.getMapImage() as DrawableResource),
                contentDescription = "defaultMap",
                contentScale = ContentScale.Crop,
            )

            // Load style URL asynchronously - don't block UI on fresh simulator
            // Reload when download completes (downloadState.isAvailable changes)
            var styleURL by remember { mutableStateOf<String?>(null) }

            LaunchedEffect(event.id, downloadState.isAvailable) {
                Log.d("IosEventMap", "Loading style URL for: ${event.id}, isAvailable=${downloadState.isAvailable}")
                styleURL = event.map.getStyleUri()
                Log.i("IosEventMap", "Style URL loaded: $styleURL")

                // Call setupMap() to initialize camera, constraints, and bounds (like Android)
                if (!setupMapCalled && styleURL != null && downloadState.isAvailable) {
                    Log.i("IosEventMap", "Calling setupMap() for: ${event.id}")
                    setupMapCalled = true

                    // Create a dummy UIImage for the map parameter (adapter doesn't use it)
                    // The adapter routes all operations through MapWrapperRegistry
                    val dummyMap = UIImage()

                    setupMap(
                        map = dummyMap,
                        scope = mapScope,
                        stylePath = styleURL!!,
                        onMapLoaded = {
                            Log.i("IosEventMap", "setupMap completed for: ${event.id}")
                        },
                        onMapClick =
                            onMapClick?.let { callback ->
                                { _: Double, _: Double ->
                                    Log.d("IosEventMap", "Map click from setupMap for: ${event.id}")
                                    callback()
                                }
                            },
                    )
                    Log.i("IosEventMap", "setupMap() completed, constraints initialized for: ${event.id}")
                }
            }

            // Show map OR overlays based on state (mutually exclusive, like Android)
            // Log current download state for debugging
            Log.v(
                "IosEventMap",
                "Map state: ${event.id} | isAvailable=${downloadState.isAvailable}, " +
                    "isDownloading=${downloadState.isDownloading}, error=${downloadState.error}, " +
                    "styleURL=${styleURL != null}, mapIsLoaded=$mapIsLoaded",
            )

            when {
                // Priority 1: Show map if available and styleURL loaded
                styleURL != null && downloadState.isAvailable -> {
                    Log.d("IosEventMap", "Showing map for ${event.id}, styleURL ready")
                    // CRITICAL FIX: Use only event.id in key(), not styleURL
                    // styleURL in key() causes wrapper deallocation when URL changes null→loaded
                    // This destroys the wrapper, stops the timer, and loses all state
                    key(event.id) {
                        @Suppress("DEPRECATION")
                        UIKitViewController(
                            factory = {
                                Log.i(
                                    "IosEventMap",
                                    "Creating native map view controller for: ${event.id}, styleURL=${styleURL!!.take(80)}",
                                )
                                createNativeMapViewController(event, styleURL!!) as platform.UIKit.UIViewController
                            },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }

                // Priority 2: Show download progress
                downloadState.isDownloading && downloadState.error == null -> {
                    Log.d("IosEventMap", "Showing MapDownloadOverlay: ${event.id} progress=${downloadState.progress}%")
                    MapDownloadOverlay(
                        progress = downloadState.progress,
                        onCancel = {
                            Log.i("IosEventMap", "Download cancelled by user: ${event.id}")
                            downloadCoordinator.cancelDownload(event.id)
                        },
                    )
                }

                // Priority 3: Show error with retry
                downloadState.error != null -> {
                    Log.d("IosEventMap", "Showing MapErrorOverlay: ${event.id} error=${downloadState.error}")
                    MapErrorOverlay(
                        errorMessage = downloadState.error!!,
                        onRetry = {
                            Log.i("IosEventMap", "Retry clicked for: ${event.id}")
                            MainScope().launch {
                                downloadCoordinator.downloadMap(event.id)
                            }
                        },
                    )
                }

                // Priority 4: Show download button if not available
                !downloadState.isAvailable && !downloadState.isDownloading -> {
                    Log.d("IosEventMap", "Showing MapDownloadButton for: ${event.id}")
                    MapDownloadButton {
                        Log.i("IosEventMap", "Download button clicked for: ${event.id}")
                        MainScope().launch {
                            downloadCoordinator.downloadMap(event.id)
                        }
                    }
                }

                // Priority 5: Show loading while waiting for styleURL
                else -> {
                    Log.d("IosEventMap", "Showing loading indicator for: ${event.id}")
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
