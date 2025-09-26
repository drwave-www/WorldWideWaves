package com.worldwidewaves.shared.ui.screens

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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.worldwidewaves.shared.MokoRes
import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.ui.components.DownloadProgressIndicator
import com.worldwidewaves.shared.utils.Log
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.launch

/**
 * Shared Event Map Screen - Map UI components abstracted from platform-specific rendering.
 * Provides identical map overlays and controls on both Android and iOS platforms.
 *
 * Separates concerns:
 * - UI overlays, controls, download states → Shared
 * - Map rendering (MapLibre/iOS Maps) → Platform-specific
 */
@Composable
fun SharedEventMapScreen(
    event: IWWWEvent,
    autoMapDownload: Boolean = false,
    modifier: Modifier = Modifier,
    onMapClick: () -> Unit = {},
    onMapDownload: () -> Unit = {},
    onMapCancel: () -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    var isMapLoaded by remember { mutableStateOf(false) }
    var mapError by remember { mutableStateOf(false) }
    var isMapDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(0) }

    Log.i("SharedEventMapScreen", "Map screen for event: ${event.id}")

    Box(modifier = modifier.fillMaxSize()) {
        // Platform-specific map rendering
        PlatformMapRenderer(
            event = event,
            onMapLoaded = { isMapLoaded = true },
            onMapError = { mapError = true },
            onMapClick = onMapClick,
            modifier = Modifier.fillMaxSize(),
        )

        // Shared UI overlays
        when {
            mapError -> {
                MapErrorOverlay(
                    onRetry = {
                        mapError = false
                        scope.launch {
                            onMapDownload()
                        }
                    },
                )
            }
            isMapDownloading -> {
                MapDownloadOverlay(
                    progress = downloadProgress,
                    onCancel = {
                        isMapDownloading = false
                        onMapCancel()
                    },
                )
            }
            !isMapLoaded && autoMapDownload -> {
                LaunchedEffect(Unit) {
                    isMapDownloading = true
                    onMapDownload()
                }
            }
        }
    }
}

/**
 * Platform-specific map renderer.
 * Android: MapLibre integration
 * iOS: iOS Maps or MapLibre equivalent
 */
@Composable
expect fun PlatformMapRenderer(
    event: IWWWEvent,
    onMapLoaded: () -> Unit,
    onMapError: () -> Unit,
    onMapClick: () -> Unit,
    modifier: Modifier = Modifier,
)

@Composable
private fun MapDownloadOverlay(
    progress: Int,
    onCancel: () -> Unit,
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background.copy(alpha = 0.8f),
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                DownloadProgressIndicator(
                    progress = progress,
                    message = stringResource(MokoRes.strings.map_downloading),
                    onCancel = onCancel,
                )
            }
        }
    }
}

@Composable
private fun MapErrorOverlay(onRetry: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Button(
                onClick = onRetry,
                modifier = Modifier.padding(16.dp),
            ) {
                Text(stringResource(MokoRes.strings.map_cancel_download))
            }
        }
    }
}
