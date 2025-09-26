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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.worldwidewaves.shared.WWWGlobals.WaveTiming
import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.events.utils.IClock
import com.worldwidewaves.shared.map.AbstractEventMap
import com.worldwidewaves.shared.ui.components.MapZoomAndLocationUpdate
import com.worldwidewaves.shared.ui.components.choreographies.WorkingWaveChoreographies
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.time.ExperimentalTime

// Constants
private const val MAP_HEIGHT_DP = 300
private const val PROGRESSION_BAR_HEIGHT_DP = 40
private const val TRIANGLE_SIZE_PX = 20f
private const val HIT_COUNTER_WIDTH_DP = 200
private const val STATUS_TEXT_FONT_SIZE = 24
private const val PROGRESSION_FONT_SIZE = 16

// UI Colors
private const val PROGRESS_COLOR = 0xFF2196F3 // Blue
private const val REMAINING_COLOR = 0xFFE0E0E0 // Light gray

/**
 * Complete Wave Screen implementation with exact same behavior and look as the working version.
 * Restored from working commit with proper choreography display and counter positioning.
 */
@OptIn(ExperimentalTime::class)
@Composable
fun WaveScreen(
    event: IWWWEvent,
    eventMap: AbstractEventMap<*>,
    modifier: Modifier = Modifier,
    mapContent: @Composable (Modifier) -> Unit,
) {
    val clockComponent =
        object : KoinComponent {
            val clock: IClock by inject()
        }
    val clock = clockComponent.clock

    // Start event/map coordination and map zoom/location updates
    MapZoomAndLocationUpdate(event, eventMap)

    // States for sound coordination
    var hasPlayedHitSound by remember { mutableStateOf(false) }

    // Calculate height based on aspect ratio and available width (exact working implementation)
    val calculatedHeight = MAP_HEIGHT_DP.dp

    // Get choreography-related states
    val isWarmingInProgress by event.observer.isUserWarmingInProgress.collectAsState(false)
    val hitDateTime by event.observer.hitDateTime.collectAsState()
    val isGoingToBeHit by event.observer.userIsGoingToBeHit.collectAsState(false)
    val hasBeenHit by event.observer.userHasBeenHit.collectAsState(false)

    // Derive choreography active state (exact working logic)
    val isChoreographyActive =
        remember(isWarmingInProgress, isGoingToBeHit, hasBeenHit, hitDateTime) {
            isWarmingInProgress ||
                isGoingToBeHit ||
                run {
                    if (hasBeenHit) {
                        val secondsSinceHit = (clock.now() - hitDateTime).inWholeSeconds
                        secondsSinceHit in 0..WaveTiming.SHOW_HIT_SEQUENCE_SECONDS.inWholeSeconds
                    } else {
                        false
                    }
                }
        }

    // Play the hit sound when the user has been hit (exact working implementation)
    LaunchedEffect(isWarmingInProgress, isGoingToBeHit, hasBeenHit, hitDateTime) {
        val secondsSinceHit = (clock.now() - hitDateTime).inWholeSeconds
        if (hasBeenHit && secondsSinceHit in 0..1 && !hasPlayedHitSound) {
            event.warming.playCurrentSoundChoreographyTone()
            hasPlayedHitSound = true
        }
    }

    // EXACT historical screen composition that was working
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(30.dp),
        ) {
            UserWaveStatusText(event)

            mapContent(
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
        WorkingWaveChoreographies(
            event = event,
            modifier = Modifier.zIndex(10f),
        )
    }
}
