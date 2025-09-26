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

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.width
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
import kotlin.time.Duration.Companion.hours
import java.util.Locale

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
    clock: IClock,
    modifier: Modifier = Modifier,
    onNavigateToFullMap: (String) -> Unit = {},
) {
    // States
    var hasPlayedHitSound by remember { mutableStateOf(false) }

    // Calculate height based on aspect ratio
    val calculatedHeight = MAP_HEIGHT_DP.dp // Fixed for cross-platform compatibility

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

        // User position triangle overlay (removed as it's now integrated in WaveProgressionBar)

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
    val eventStatus by event.observer.eventStatus.collectAsState(Status.UNDEFINED)
    val hasBeenHit by event.observer.userHasBeenHit.collectAsState()
    val isInArea by event.observer.userIsInArea.collectAsState()
    val isWarming by event.observer.isUserWarmingInProgress.collectAsState()

    val message = when {
        eventStatus == Status.DONE -> MokoRes.strings.wave_done
        hasBeenHit -> MokoRes.strings.wave_hit
        isWarming && isInArea -> MokoRes.strings.wave_warming
        isInArea -> MokoRes.strings.wave_be_ready
        else -> MokoRes.strings.wave_is_running
    }

    Box(
        modifier = Modifier.padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        com.worldwidewaves.shared.ui.utils.AutoResizeSingleLineText(
            text = stringResource(message),
            style = sharedPrimaryColoredBoldTextStyle(STATUS_TEXT_FONT_SIZE),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun WaveProgressionBar(event: IWWWEvent) {
    val progression by event.observer.progression.collectAsState()
    val isInArea by event.observer.userIsInArea.collectAsState()
    val userPositionRatio by event.observer.userPositionRatio.collectAsState()
    val isGoingToBeHit by event.observer.userIsGoingToBeHit.collectAsState()
    val hasBeenHit by event.observer.userHasBeenHit.collectAsState()

    // Calculate responsive width - 80% of screen width
    val triangleSize = TRIANGLE_SIZE_PX // Fixed triangle size

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(PROGRESSION_BAR_HEIGHT_DP.dp) // WaveDisplay.PROGRESSION_HEIGHT
                .clip(RoundedCornerShape(25.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            WaveProgressionFillArea(progression)

            Text(
                text = "${String.format(Locale.getDefault(), "%.1f", progression)}%",
                style = sharedPrimaryColoredBoldTextStyle(PROGRESSION_FONT_SIZE),
                color = Color.Black,
                textAlign = TextAlign.Center,
            )
        }
        if (isInArea) {
            UserPositionTriangle(userPositionRatio, triangleSize, isGoingToBeHit, hasBeenHit)
        }
    }
}

@Composable
private fun WaveProgressionFillArea(progression: Double) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(PROGRESSION_BAR_HEIGHT_DP.dp),
    ) {
        val width = size.width
        val height = size.height
        val traversedWidth = (width * kotlin.math.min(progression, 100.0).toFloat() / 100f)

        // Draw the progression bar - filled area (blue/primary)
        drawRect(
            color = androidx.compose.ui.graphics.Color(PROGRESS_COLOR), // Blue color for progress
            size = androidx.compose.ui.geometry.Size(traversedWidth, height),
        )
        // Draw the remaining area (gray)
        drawRect(
            color = androidx.compose.ui.graphics.Color(REMAINING_COLOR), // Light gray for remaining
            topLeft = androidx.compose.ui.geometry.Offset(traversedWidth, 0f),
            size = androidx.compose.ui.geometry.Size(width - traversedWidth, height),
        )
    }
}

@Composable
fun UserPositionTriangle(
    userPositionRatio: Double,
    triangleSize: Float,
    isGoingToBeHit: Boolean,
    hasBeenHit: Boolean,
) {
    val triangleColor = when {
        isGoingToBeHit -> androidx.compose.ui.graphics.Color(0xFFFF6B35) // Orange for about to be hit
        hasBeenHit -> androidx.compose.ui.graphics.Color(0xFF4CAF50) // Green for hit
        else -> androidx.compose.ui.graphics.Color(0xFF9E9E9E) // Gray for idle
    }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(triangleSize.toInt().dp)
            .padding(top = 4.dp),
    ) {
        val width = size.width
        val trianglePosition = (width * userPositionRatio).toFloat().coerceIn(0f, width)
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(trianglePosition, 0f)
            lineTo(trianglePosition - triangleSize / 2f, triangleSize)
            lineTo(trianglePosition + triangleSize / 2f, triangleSize)
            close()
        }
        drawPath(path, triangleColor, style = androidx.compose.ui.graphics.drawscope.Fill)
    }
}


@Composable
fun WaveHitCounter(event: IWWWEvent) {
    val timeBeforeHit by event.observer.timeBeforeHit.collectAsState()

    val text = formatDuration(timeBeforeHit)

    if (text != "--:--") {
        val boxWidth = HIT_COUNTER_WIDTH_DP.dp // Fixed width for cross-platform compatibility

        Box(
            modifier = Modifier
                .width(boxWidth)
                .background(Color.Transparent)
                .padding(2.dp),
            contentAlignment = Alignment.Center,
        ) {
            AutoSizeText(
                text = text,
                style = sharedPrimaryColoredBoldTextStyle(STATUS_TEXT_FONT_SIZE),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
            )
        }
    }
}

private fun formatDuration(duration: kotlin.time.Duration): String =
    when {
        duration.isInfinite() || duration < kotlin.time.Duration.ZERO -> "--:--" // Protection
        duration < 1.hours -> {
            val minutes = duration.inWholeMinutes.toString().padStart(2, '0')
            val seconds = (duration.inWholeSeconds % 60).toString().padStart(2, '0')
            "$minutes:$seconds"
        }

        duration < 99.hours -> {
            val hours = duration.inWholeHours.toString().padStart(2, '0')
            val minutes = (duration.inWholeMinutes % 60).toString().padStart(2, '0')
            "$hours:$minutes"
        }

        else -> "--:--"
    }

@Composable
fun AutoSizeText(
    text: String,
    style: androidx.compose.ui.text.TextStyle,
    modifier: Modifier = Modifier,
) {
    var fontSize by remember { mutableStateOf(style.fontSize) }

    Text(
        text = text,
        style = style.copy(fontSize = fontSize),
        color = Color.White,
        textAlign = TextAlign.Center,
        maxLines = 1,
        softWrap = false,
        onTextLayout = { textLayoutResult ->
            if (textLayoutResult.hasVisualOverflow) {
                fontSize = fontSize * 0.9f
            }
        },
        modifier = modifier,
    )
}
