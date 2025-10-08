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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.interop.UIKitViewController
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
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
import kotlinx.coroutines.MainScope
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

    private fun formatCoordinates(location: Position): String =
        "${location.lat.toString().take(COORDINATE_DISPLAY_LAT_LENGTH)}, " +
            location.lng.toString().take(COORDINATE_DISPLAY_LNG_LENGTH)

    companion object {
        private val LOADING_COLOR = Color(0xFFFFA500)
        private const val COORDINATE_DISPLAY_LAT_LENGTH = 8
        private const val COORDINATE_DISPLAY_LNG_LENGTH = 9
    }

    override fun updateWavePolygons(
        wavePolygons: List<Polygon>,
        clearPolygons: Boolean,
    ) {
        Log.d("IosEventMap", "updateWavePolygons called with ${wavePolygons.size} polygons, clearPolygons=$clearPolygons")

        if (clearPolygons) {
            currentPolygons.clear()
        }
        currentPolygons.addAll(wavePolygons)

        Log.v("IosEventMap", "iOS map now tracking ${currentPolygons.size} wave polygons")

        // Store polygon data in registry for Swift to render
        storePolygonsForRendering(wavePolygons, clearPolygons)
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
        val currentLocation by positionManager.position.collectAsState()

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
            }

            // Show map or loading indicator based on style availability
            if (styleURL != null) {
                Log.d("IosEventMap", "Using style URL for ${event.id}: ${styleURL!!.take(100)}...")

                // Use key() to recreate map when styleURL changes (after download)
                key("${event.id}-$styleURL") {
                    @Suppress("DEPRECATION")
                    UIKitViewController(
                        factory = {
                            Log.i("IosEventMap", "Creating native map view controller for: ${event.id}")
                            createNativeMapViewController(event, styleURL!!) as platform.UIKit.UIViewController
                        },
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .clickable(enabled = onMapClick != null) {
                                    Log.d("IosEventMap", "Map clicked for event: ${event.id}")
                                    onMapClick?.invoke()
                                },
                    )
                }
            } else {
                // Show loading while waiting for files to download/cache
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    LoadingIndicator("Loading map...")
                }
            }

            // Overlay with map information and controls
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Map status card at top
                MapStatusCard(
                    event = event,
                    isLoaded = mapIsLoaded,
                    downloadState = downloadState,
                    polygonCount = currentPolygons.size,
                )

                Spacer(modifier = Modifier.weight(1f))

                // Location info at bottom if available
                currentLocation?.let { location ->
                    LocationInfoCard(location)
                }

                if (currentLocation == null) {
                    Card(
                        colors =
                            CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                            ),
                        modifier = Modifier.padding(8.dp),
                    ) {
                        Text(
                            text = "Location not available",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(12.dp),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            // Download overlay UI (matching Android behavior)
            // Log current download state for debugging
            Log.v(
                "IosEventMap",
                "Overlay decision: ${event.id} | isAvailable=${downloadState.isAvailable}, " +
                    "isDownloading=${downloadState.isDownloading}, error=${downloadState.error}, progress=${downloadState.progress}",
            )

            when {
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

                !downloadState.isAvailable && !downloadState.isDownloading -> {
                    Log.d("IosEventMap", "Showing MapDownloadButton for: ${event.id}")
                    MapDownloadButton {
                        Log.i("IosEventMap", "Download button clicked for: ${event.id}")
                        MainScope().launch {
                            Log.i("IosEventMap", "Launching download coroutine for: ${event.id}")
                            downloadCoordinator.downloadMap(event.id)
                        }
                    }
                }

                else -> {
                    Log.v("IosEventMap", "No overlay shown: ${event.id} (map loaded or downloading)")
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

    @Composable
    private fun MapStatusCard(
        event: IWWWEvent,
        isLoaded: Boolean,
        downloadState: EventMapDownloadManager.DownloadState,
        polygonCount: Int,
    ) {
        Card(
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                ),
            modifier = Modifier.padding(8.dp),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(event.getLocation()),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row {
                    // Map status indicator
                    Box(
                        modifier =
                            Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        downloadState.error != null -> Color.Red
                                        downloadState.isAvailable && isLoaded -> Color.Green
                                        else -> LOADING_COLOR
                                    },
                                ),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text =
                            when {
                                downloadState.error != null -> "Error"
                                downloadState.isDownloading -> "Downloading: ${downloadState.progress}%"
                                !downloadState.isAvailable -> "Map Not Available"
                                isLoaded -> "Map Ready"
                                else -> "Loading..."
                            },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                if (downloadState.error != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Download failed: ${downloadState.error}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                if (polygonCount > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Wave Polygons: $polygonCount",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }

    @Composable
    private fun LocationInfoCard(location: Position) {
        Card(
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f),
                ),
            modifier = Modifier.padding(8.dp),
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Your Location",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                )

                Text(
                    text = formatCoordinates(location),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                )
            }
        }
    }
}
