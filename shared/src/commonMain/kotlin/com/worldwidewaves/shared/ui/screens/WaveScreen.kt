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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

            // Always show counter in the proper position with spacing (exact working layout)
            Spacer(modifier = Modifier.weight(1f))
            WaveHitCounter(event)
            Spacer(modifier = Modifier.height(30.dp))
        }

        // Working choreographies with proper z-index

        WaveChoreographies(
            event = event,
            modifier = Modifier.zIndex(10f),
        )
    }
}
