package com.worldwidewaves.shared.ui.components.choreographies

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
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.worldwidewaves.shared.MokoRes
import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.events.utils.IClock
import com.worldwidewaves.shared.ui.theme.sharedQuinaryColoredBoldTextStyle
import com.worldwidewaves.shared.utils.Log
import dev.icerock.moko.resources.compose.stringResource

// Constants for choreography display
private object ChoreographyConstants {
    const val ALPHA_OPACITY = 0.8f
    const val CHOREOGRAPHY_PADDING = 24
    const val CHOREOGRAPHY_TITLE_SIZE = 24
    const val ORANGE_COLOR = 0xFFFF9800
    const val RED_COLOR = 0xFFFF5722
    const val GREEN_COLOR = 0xFF4CAF50
}

/**
 * Shared wave choreography component that displays choreography states.
 * Works identically on both Android and iOS platforms.
 *
 * Provides visual feedback for wave states:
 * - Warming phase preparation
 * - Ready state before wave hit
 * - Hit confirmation
 * - Running state display
 */
@Composable
fun WaveChoreographies(
    event: IWWWEvent,
    @Suppress("UnusedParameter") clock: IClock,
    modifier: Modifier = Modifier,
) {
    val isWarmingInProgress by event.observer.isUserWarmingInProgress.collectAsState()
    val isGoingToBeHit by event.observer.userIsGoingToBeHit.collectAsState()
    val hasBeenHit by event.observer.userHasBeenHit.collectAsState()

    Log.v("WaveChoreographies", "State: warming=$isWarmingInProgress, goingToBeHit=$isGoingToBeHit, hasBeenHit=$hasBeenHit")

    Box(
        modifier = modifier.fillMaxSize().padding(bottom = 120.dp), // Leave space for counter
        contentAlignment = Alignment.Center,
    ) {
        when {
            isWarmingInProgress -> {
                ChoreographyStateDisplay(
                    title = stringResource(MokoRes.strings.wave_warming),
                    description = "Prepare for the wave...",
                    backgroundColor = Color(ChoreographyConstants.ORANGE_COLOR).copy(alpha = ChoreographyConstants.ALPHA_OPACITY)
                )
            }
            isGoingToBeHit -> {
                ChoreographyStateDisplay(
                    title = stringResource(MokoRes.strings.wave_be_ready),
                    description = "Wave is coming!",
                    backgroundColor = Color(ChoreographyConstants.RED_COLOR).copy(alpha = ChoreographyConstants.ALPHA_OPACITY)
                )
            }
            hasBeenHit -> {
                ChoreographyStateDisplay(
                    title = stringResource(MokoRes.strings.wave_hit),
                    description = "You've been hit by the wave!",
                    backgroundColor = Color(ChoreographyConstants.GREEN_COLOR).copy(alpha = ChoreographyConstants.ALPHA_OPACITY)
                )
            }
            else -> {
                ChoreographyStateDisplay(
                    title = stringResource(MokoRes.strings.wave_is_running),
                    description = "Wave is in progress...",
                    backgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = ChoreographyConstants.ALPHA_OPACITY)
                )
            }
        }
    }
}

@Composable
private fun ChoreographyStateDisplay(
    title: String,
    description: String,
    backgroundColor: Color,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(ChoreographyConstants.CHOREOGRAPHY_PADDING.dp)
            .shadow(8.dp, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(2.dp, Color.White, RoundedCornerShape(12.dp))
            .padding(ChoreographyConstants.CHOREOGRAPHY_PADDING.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = title,
                style = sharedQuinaryColoredBoldTextStyle(ChoreographyConstants.CHOREOGRAPHY_TITLE_SIZE),
                color = Color.White,
                textAlign = TextAlign.Center,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
        }
    }
}
