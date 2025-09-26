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

package com.worldwidewaves.shared.ui.components.choreographies

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.worldwidewaves.shared.WWWGlobals.WaveTiming
import com.worldwidewaves.shared.choreographies.ChoreographyManager.DisplayableSequence
import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.events.utils.IClock
import com.worldwidewaves.shared.ui.theme.sharedQuinaryColoredBoldTextStyle
import com.worldwidewaves.shared.utils.Log
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.math.min
import kotlin.time.ExperimentalTime

// Constants for choreography display
private object WorkingChoreographyConstants {
    const val CHOREOGRAPHY_PADDING = 24f
    const val CHOREOGRAPHY_TEXT_SIZE = 24f
}

/**
 * High-level choreography container displayed on the **Wave** screen.
 *
 * This is the working implementation restored from the commit that had
 * properly functioning choreography display and counter positioning.
 */
@OptIn(ExperimentalTime::class)
@Composable
fun WorkingWaveChoreographies(
    event: IWWWEvent,
    modifier: Modifier = Modifier,
) {
    val clockComponent =
        object : KoinComponent {
            val clock: IClock by inject()
        }
    val clock = clockComponent.clock
    val isWarmingInProgress by event.observer.isUserWarmingInProgress.collectAsState()
    val isGoingToBeHit by event.observer.userIsGoingToBeHit.collectAsState()
    val hasBeenHit by event.observer.userHasBeenHit.collectAsState()
    val hitDateTime by event.observer.hitDateTime.collectAsState()

    // Debug logging for choreography states
    LaunchedEffect(isWarmingInProgress, isGoingToBeHit, hasBeenHit) {
        Log.v(
            "WaveChoreographies",
            "[CHOREO_DEBUG] State change for ${event.id}: " +
                "warming=$isWarmingInProgress, goingToBeHit=$isGoingToBeHit, hasBeenHit=$hasBeenHit",
        )
    }

    // State to track if we should show the hit sequence
    var showHitSequence by remember { mutableStateOf(false) }

    // For warming sequences - use recomposition to update sequence
    var warmingKey by remember { mutableIntStateOf(0) }

    // Calculate and schedule the hiding of the hit sequence
    LaunchedEffect(hasBeenHit, hitDateTime) {
        @OptIn(ExperimentalTime::class)
        if (hasBeenHit) {
            val currentTime = clock.now()
            val secondsSinceHit = (currentTime - hitDateTime).inWholeSeconds

            if (secondsSinceHit in 0..WaveTiming.SHOW_HIT_SEQUENCE_SECONDS.inWholeSeconds) {
                showHitSequence = true

                // Calculate remaining time to show
                val remainingTimeMs =
                    kotlin.math.max(
                        0,
                        WaveTiming.SHOW_HIT_SEQUENCE_SECONDS.inWholeMilliseconds -
                            (currentTime - hitDateTime).inWholeMilliseconds,
                    )

                // Schedule hiding after the remaining time
                delay(remainingTimeMs)
                showHitSequence = false
            } else {
                showHitSequence = false
            }
        } else {
            showHitSequence = false
        }
    }

    // Only show choreography content in the center, leaving bottom space free
    when {
        // Show warming choreography with sequence refresh
        isWarmingInProgress -> {
            // Get the current sequence
            var warmingSequence by remember { mutableStateOf<DisplayableSequence<DrawableResource>?>(null) }

            LaunchedEffect(warmingKey) {
                warmingSequence = event.warming.getCurrentChoregraphySequence()
            }

            // When this sequence ends, request a new one
            if (warmingSequence != null) {
                TimedSequenceDisplay(
                    sequence = warmingSequence,
                    modifier =
                        modifier
                            .fillMaxWidth()
                            .padding(bottom = 120.dp),
                    onSequenceComplete = { warmingKey++ },
                )
            }
        }

        // Show waiting choreography when going to be hit
        isGoingToBeHit -> {
            ChoreographyDisplay(
                event.wave.waitingChoregraphySequence(),
                modifier
                    .fillMaxWidth()
                    .padding(bottom = 120.dp),
            )
        }

        // Show hit choreography when user has been hit and within time window
        showHitSequence -> {
            ChoreographyDisplay(
                event.wave.hitChoregraphySequence(),
                modifier
                    .fillMaxWidth()
                    .padding(bottom = 120.dp),
            )
        }
    }
}

/**
 * Helper that renders [sequence] for its remaining duration and then
 * triggers [onSequenceComplete] so the caller can request the next sequence.
 */
@Composable
fun TimedSequenceDisplay(
    sequence: DisplayableSequence<DrawableResource>?,
    modifier: Modifier = Modifier,
    onSequenceComplete: () -> Unit,
) {
    if (sequence == null) return

    ChoreographyDisplay(sequence, modifier)

    LaunchedEffect(sequence) {
        delay(sequence.remainingDuration ?: sequence.duration)
        onSequenceComplete()
    }
}

/**
 * Low-level renderer that plays a sprite-sheet based animation.
 *
 * This is the complete working implementation with proper frame rendering,
 * positioning, scaling, and visual styling that was working two days ago.
 */
@Composable
fun ChoreographyDisplay(
    sequence: DisplayableSequence<DrawableResource>?,
    modifier: Modifier = Modifier,
) {
    if (sequence == null || sequence.image == null) return

    val clockComponent =
        object : KoinComponent {
            val clock: IClock by inject()
        }
    val clock = clockComponent.clock

    var currentImageIndex by remember { mutableIntStateOf(0) }
    val remainingTime by remember(sequence) { mutableStateOf(sequence.remainingDuration) }

    // Get the painter
    val painter = painterResource(sequence.image!!)

    // Create a timer to cycle through images
    LaunchedEffect(sequence) {
        @OptIn(ExperimentalTime::class)
        val startTime = clock.now()

        while (this.isActive) {
            // Check if we should stop showing the sequence
            @OptIn(ExperimentalTime::class)
            val elapsed = clock.now() - startTime
            if (remainingTime != null) {
                if (elapsed >= remainingTime!!) break
            } else if (elapsed >= sequence.duration) {
                break
            }

            delay(sequence.timing.inWholeMilliseconds)

            // Only advance frame if we haven't reached the last frame or if looping
            if (sequence.loop) {
                currentImageIndex = (currentImageIndex + 1) % sequence.frameCount
            } else if (currentImageIndex < sequence.frameCount - 1) {
                currentImageIndex++
            }
        }
    }

    // Position the choreography in the center without taking full screen space
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .widthIn(max = 400.dp)
                    .heightIn(max = 600.dp)
                    .padding(WorkingChoreographyConstants.CHOREOGRAPHY_PADDING.dp)
                    .shadow(8.dp)
                    .background(Color.Black.copy(alpha = 0.7f))
                    .border(2.dp, Color.White, RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp))
                    .padding(WorkingChoreographyConstants.CHOREOGRAPHY_PADDING.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Canvas(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .weight(1f),
                ) {
                    val frameWidthPx = sequence.frameWidth.toFloat()
                    val frameHeightPx = sequence.frameHeight.toFloat()

                    // Calculate scale to fit the frame within canvas bounds
                    val scaleX = size.width / frameWidthPx
                    val scaleY = size.height / frameHeightPx
                    val scale = min(scaleX, scaleY)

                    // Calculate centered position
                    val scaledWidth = frameWidthPx * scale
                    val scaledHeight = frameHeightPx * scale
                    val offsetX = (size.width - scaledWidth) / 2f
                    val offsetY = (size.height - scaledHeight) / 2f

                    clipRect(
                        left = offsetX,
                        top = offsetY,
                        right = offsetX + scaledWidth,
                        bottom = offsetY + scaledHeight,
                    ) {
                        translate(
                            left = offsetX - (currentImageIndex * scaledWidth),
                            top = offsetY,
                        ) {
                            with(painter) {
                                draw(
                                    size =
                                        Size(
                                            width = scaledWidth * 4, // Always 4 frames per slide
                                            height = scaledHeight,
                                        ),
                                )
                            }
                        }
                    }
                }

                Text(
                    text = stringResource(sequence.text),
                    style = sharedQuinaryColoredBoldTextStyle(WorkingChoreographyConstants.CHOREOGRAPHY_TEXT_SIZE.toInt()),
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
