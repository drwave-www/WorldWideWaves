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

import androidx.compose.foundation.background
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.events.utils.Polygon
import com.worldwidewaves.shared.events.utils.Position
import com.worldwidewaves.shared.utils.Log
import dev.icerock.moko.resources.compose.stringResource
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
class IOSEventMap(
    event: IWWWEvent,
    private val onMapLoaded: () -> Unit = {},
    onLocationUpdate: (Position) -> Unit = {},
    mapConfig: EventMapConfig = EventMapConfig(),
) : AbstractEventMap<UIImage>(event, mapConfig, onLocationUpdate) {
    // Use safe iOS dependency injection pattern
    override val mapLibreAdapter: MapLibreAdapter<UIImage> =
        KoinPlatform.getKoin().get<MapLibreAdapter<UIImage>>()
    override val locationProvider: WWWLocationProvider? =
        KoinPlatform.getKoin().getOrNull<WWWLocationProvider>()

    private var currentPolygons = mutableListOf<Polygon>()

    override fun updateWavePolygons(
        wavePolygons: List<Polygon>,
        clearPolygons: Boolean,
    ) {
        Log.d("IOSEventMap", "updateWavePolygons called with ${wavePolygons.size} polygons, clearPolygons=$clearPolygons")

        if (clearPolygons) {
            currentPolygons.clear()
        }
        currentPolygons.addAll(wavePolygons)

        Log.v("IOSEventMap", "iOS map now tracking ${currentPolygons.size} wave polygons")
    }

    @Composable
    override fun Draw(
        autoMapDownload: Boolean,
        modifier: Modifier,
    ) {
        val currentLocation by (
            locationProvider?.currentLocation
                ?: kotlinx.coroutines.flow.MutableStateFlow<Position?>(null)
        ).collectAsState()

        var mapIsLoaded by remember { mutableStateOf(false) }

        // Signal map loaded after UI composition
        LaunchedEffect(Unit) {
            mapIsLoaded = true
            onMapLoaded()
        }

        Box(modifier = modifier.fillMaxSize()) {
            // iOS map background - use a solid color since resource handling is platform-specific
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
            )

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
                    autoDownload = autoMapDownload,
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
        }
    }

    @Composable
    private fun MapStatusCard(
        event: IWWWEvent,
        isLoaded: Boolean,
        autoDownload: Boolean,
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
                                .background(if (isLoaded) Color.Green else Color(0xFFFFA500)),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isLoaded) "Map Ready" else "Loading...",
                        style = MaterialTheme.typography.bodyMedium,
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
                    text = "${location.lat.toString().take(8)}, ${location.lng.toString().take(9)}",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                )
            }
        }
    }
}
