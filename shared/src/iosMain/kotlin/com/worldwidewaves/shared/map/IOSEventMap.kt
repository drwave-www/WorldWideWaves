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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.events.utils.Polygon
import com.worldwidewaves.shared.events.utils.Position
import com.worldwidewaves.shared.utils.Log
import org.koin.core.component.inject
import platform.UIKit.UIImage

/**
 * iOS-native implementation of EventMap using SwiftUI integration.
 *
 * This provides a minimal map implementation for iOS while maintaining
 * compatibility with the shared AbstractEventMap interface.
 */
class IOSEventMap(
    event: IWWWEvent,
    private val onMapLoaded: () -> Unit = {},
    onLocationUpdate: (Position) -> Unit = {},
    mapConfig: EventMapConfig = EventMapConfig(),
) : AbstractEventMap<UIImage>(event, mapConfig, onLocationUpdate) {
    override val mapLibreAdapter: MapLibreAdapter<UIImage> by inject()
    override val locationProvider: WWWLocationProvider? by inject()

    override fun updateWavePolygons(
        wavePolygons: List<Polygon>,
        clearPolygons: Boolean,
    ) {
        Log.d("IOSEventMap", "updateWavePolygons called with ${wavePolygons.size} polygons, clearPolygons=$clearPolygons")
        // iOS: Minimal implementation - polygons are handled by MapLibre adapter
    }

    @Composable
    override fun Draw(
        autoMapDownload: Boolean,
        modifier: Modifier,
    ) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "iOS Map View\n${event.id}\nAutoDownload: $autoMapDownload",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
