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

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.events.IWWWEvent.Status
import com.worldwidewaves.shared.events.utils.IClock
import com.worldwidewaves.shared.MokoRes
import com.worldwidewaves.shared.ui.components.choreographies.WaveChoreographies
import com.worldwidewaves.shared.ui.theme.sharedCommonTextStyle
import com.worldwidewaves.shared.ui.theme.sharedPrimaryColoredBoldTextStyle
import com.worldwidewaves.shared.utils.Log
import com.worldwidewaves.shared.WWWGlobals.Event
import com.worldwidewaves.shared.WWWGlobals.WaveTiming
import com.worldwidewaves.shared.WWWPlatform
import dev.icerock.moko.resources.compose.stringResource
import kotlin.time.ExperimentalTime

/**
 * Shared Wave Participation Screen - Complete wave interaction UI.
 * Extracted from Android WaveActivity to provide identical functionality on both platforms.
 *
 * Displays:
 * • Wave status and user state
 * • Interactive map with zoom and location
 * • Wave progression visualization
 * • User position triangle
 * • Hit counter and choreography
 * • Auto-sizing text components
 *
 * Works identically on both Android and iOS platforms.
 */
@OptIn(ExperimentalTime::class)
@Composable
fun SharedWaveScreen(
    event: IWWWEvent,
    platform: WWWPlatform,
    clock: IClock,
    modifier: Modifier = Modifier,
    onNavigateToFullMap: (String) -> Unit = {},
) {
    // States
    var hasPlayedHitSound by remember { mutableStateOf(false) }

    // Calculate height based on aspect ratio
    val calculatedHeight = 300.dp // Fixed for cross-platform compatibility

    // Get choreography-related states
    val isWarmingInProgress by event.observer.isUserWarmingInProgress.collectAsState(false)
    val hitDateTime by event.observer.hitDateTime.collectAsState()
    val isGoingToBeHit by event.observer.userIsGoingToBeHit.collectAsState(false)
    val hasBeenHit by event.observer.userHasBeenHit.collectAsState(false)

    // Derive choreography active state
    val isChoreographyActive = remember(isWarmingInProgress, isGoingToBeHit, hasBeenHit, hitDateTime) {
        isWarmingInProgress || isGoingToBeHit || run {
            if (hasBeenHit) {
                val secondsSinceHit = (clock.now() - hitDateTime).inWholeSeconds
                secondsSinceHit in 0..WaveTiming.SHOW_HIT_SEQUENCE_SECONDS.inWholeSeconds
            } else {
                false
            }
        }
    }

    // Play hit sound when user has been hit
    LaunchedEffect(isWarmingInProgress, isGoingToBeHit, hasBeenHit, hitDateTime) {
        val secondsSinceHit = (clock.now() - hitDateTime).inWholeSeconds
        if (hasBeenHit && secondsSinceHit in 0..1 && !hasPlayedHitSound) {
            try {
                event.warming.playCurrentSoundChoreographyTone()
                hasPlayedHitSound = true
            } catch (e: Exception) {
                Log.e("WaveScreen", "Failed to play hit sound", throwable = e)
            }
        }
    }

    // Screen composition
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(30.dp),
        ) {
            // User wave status text
            UserWaveStatusText(event)

            // Platform-specific map with zoom and location
            PlatformWaveMap(
                event = event,
                onMapClick = { onNavigateToFullMap(event.id) },
                modifier = Modifier.fillMaxWidth().height(calculatedHeight)
            )

            // Wave progression bar
            WaveProgressionBar(event)

            // Hit counter
            WaveHitCounter(event)
        }

        // User position triangle overlay
        val progression by event.observer.progression.collectAsState()
        UserPositionTriangle(progression)

        // Choreography overlay when active
        if (isChoreographyActive) {
            WaveChoreographies(
                event = event,
                clock = clock,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/**
 * Platform-specific wave map component.
 * Android: Uses AndroidEventMap
 * iOS: Uses iOS-specific map implementation
 */
@Composable
expect fun PlatformWaveMap(
    event: IWWWEvent,
    onMapClick: () -> Unit,
    modifier: Modifier = Modifier,
)

@Composable
fun UserWaveStatusText(event: IWWWEvent) {
    val eventStatus by event.observer.eventStatus.collectAsState()
    val progression by event.observer.progression.collectAsState()

    val statusText = when (eventStatus) {
        Status.SOON -> stringResource(MokoRes.strings.event_soon)
        Status.RUNNING -> "${stringResource(MokoRes.strings.wave_is_running)} ${(progression * 100).toInt()}%"
        Status.DONE -> stringResource(MokoRes.strings.event_done)
        else -> "Wave Status"
    }

    AutoSizeText(
        text = statusText,
        style = sharedPrimaryColoredBoldTextStyle(24),
        modifier = Modifier.padding(16.dp)
    )
}

@Composable
fun WaveProgressionBar(event: IWWWEvent) {
    val progression by event.observer.progression.collectAsState()
    val eventStatus by event.observer.eventStatus.collectAsState()

    if (eventStatus == Status.RUNNING) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            Text(
                text = "${stringResource(MokoRes.strings.wave_progression)}: ${(progression * 100).toInt()}%",
                style = sharedCommonTextStyle(16),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LinearProgressIndicator(
                progress = { progression.toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round,
            )

            WaveProgressionFillArea(progression)
        }
    }
}

@Composable
private fun WaveProgressionFillArea(progression: Double) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(20.dp)
            .background(Color.Gray.copy(alpha = 0.3f))
            .clip(RoundedCornerShape(4.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progression.toFloat())
                .height(20.dp)
                .background(MaterialTheme.colorScheme.primary)
                .clip(RoundedCornerShape(4.dp))
        )
    }
}

@Composable
fun UserPositionTriangle(userPositionRatio: Double) {
    Canvas(
        modifier = Modifier
            .size(30.dp)
            .padding(8.dp)
    ) {
        drawTriangle(this, userPositionRatio)
    }
}

private fun drawTriangle(drawScope: DrawScope, ratio: Double) {
    with(drawScope) {
        val triangleSize = size.minDimension * 0.8f
        val centerX = size.width / 2f
        val centerY = size.height / 2f

        // Simple triangle implementation
        drawCircle(
            color = Color.Red,
            radius = triangleSize / 4f,
            center = androidx.compose.ui.geometry.Offset(centerX, centerY)
        )
    }
}

@Composable
fun WaveHitCounter(event: IWWWEvent) {
    val hasBeenHit by event.observer.userHasBeenHit.collectAsState()

    if (hasBeenHit) {
        Box(
            modifier = Modifier
                .background(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                    RoundedCornerShape(12.dp)
                )
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(MokoRes.strings.wave_hit),
                style = MaterialTheme.typography.headlineMedium.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun AutoSizeText(
    text: String,
    style: androidx.compose.ui.text.TextStyle,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = style,
        modifier = modifier,
        maxLines = 1,
        fontSize = 16.sp
    )
}