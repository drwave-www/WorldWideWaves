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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.systemBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.map.AbstractEventMap
import com.worldwidewaves.shared.ui.components.MapZoomAndLocationUpdate
import com.worldwidewaves.shared.ui.components.wave.UserWaveStatusText
import com.worldwidewaves.shared.ui.components.wave.WaveHitCounter
import com.worldwidewaves.shared.ui.components.wave.WaveProgressionBar
import com.worldwidewaves.shared.ui.components.wave.choreographies.WaveChoreographies
import com.worldwidewaves.shared.ui.formatters.calculateEventMapHeight
import kotlin.time.ExperimentalTime

/**
 * Complete Wave Screen implementation with exact same behavior and look as the working version.
 * Restored from working commit with proper choreography display and counter positioning.
 *
 * Map dimensions: Uses same responsive height calculation as Event Detail screen
 * (calculateEventMapHeight) to ensure visual consistency.
 */
@OptIn(ExperimentalTime::class)
@Composable
fun WaveScreen(
    event: IWWWEvent,
    eventMap: AbstractEventMap<*>?,
    modifier: Modifier = Modifier,
) {
    // Start event/map coordination and map zoom/location updates
    MapZoomAndLocationUpdate(event, eventMap)

    // Calculate height based on aspect ratio and available width (matches Event Detail screen)
    val calculatedHeight = calculateEventMapHeight()

    // Track countdown position and height for dynamic choreography positioning
    val density = LocalDensity.current
    val windowInfo = LocalWindowInfo.current
    var countdownHeightPx by remember { mutableStateOf(0) }
    var countdownTopPx by remember { mutableStateOf(0f) }
    val countdownHeight = with(density) { countdownHeightPx.toDp() }

    // Calculate safe area bottom padding
    val safeAreaBottomPadding = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding()

    // Calculate choreography bottom padding dynamically
    // This should account for: countdown height + space below it (will be dynamic based on remaining space)
    // For now, we use a reasonable estimate that will be refined after layout
    val choreographyBottomPadding: Dp = countdownHeight + safeAreaBottomPadding + 60.dp

    // Calculate max height for choreographies as 2/3 of screen size
    val maxChoreographyHeight: Dp =
        with(density) {
            val screenHeightDp = windowInfo.containerSize.height.toDp()
            val twoThirdsHeight = screenHeightDp * (2f / 3f)
            // Clamp between 300dp min and 600dp max
            twoThirdsHeight.coerceIn(300.dp, 600.dp)
        }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(30.dp),
        ) {
            UserWaveStatusText(event)

            eventMap?.Draw(
                autoMapDownload = true,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(calculatedHeight),
            )

            WaveProgressionBar(event)

            // Flexible spacing above countdown to center it vertically
            Spacer(modifier = Modifier.weight(1f))
            WaveHitCounter(
                event = event,
                modifier =
                    Modifier
                        .onSizeChanged { size ->
                            countdownHeightPx = size.height
                        }.onGloballyPositioned { coordinates ->
                            countdownTopPx = coordinates.positionInRoot().y
                        },
            )
            // Flexible spacing below countdown to center it vertically
            Spacer(modifier = Modifier.weight(1f))
            // Add safe area padding for devices with notches/home indicators
            Spacer(modifier = Modifier.height(safeAreaBottomPadding))
        }

        // Choreographies centered on screen with 2/3 screen height
        WaveChoreographies(
            event = event,
            bottomPadding = 0.dp,
            maxHeight = maxChoreographyHeight,
            modifier = Modifier.zIndex(10f),
        )
    }
}
