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

@file:OptIn(kotlin.time.ExperimentalTime::class)

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
import kotlin.time.ExperimentalTime

// Constants for choreography display
private object ChoreographyConstants {
    // UI Dimensions
    const val CHOREOGRAPHY_PADDING = 24f
    const val CHOREOGRAPHY_TEXT_SIZE = 24f
}

/**
 * Shared high-level choreography container displayed on the Wave screen.
 * Works identically on both Android and iOS platforms.
 *
 * Behaviour:
 * • Subscribes to various flags exposed by IWWWEvent.observer to know whether
 *   the user is currently warming-up, about to be hit, or has just been hit.
 * • According to those flags it queries the proper ChoreographyManager
 *   (warming / waiting / hit) to obtain a DisplayableSequence.
 * • The resulting animation is shown while leaving room at the
 *   bottom for the count-down timer that the parent screen overlays.
 */
@Composable
fun WaveChoreographies(
    event: IWWWEvent,
    clock: IClock,
    modifier: Modifier = Modifier,
) {
    val isWarmingInProgress by event.observer.isUserWarmingInProgress.collectAsState()
    val isGoingToBeHit by event.observer.userIsGoingToBeHit.collectAsState()
    val hasBeenHit by event.observer.userHasBeenHit.collectAsState()
    val hitDateTime by event.observer.hitDateTime.collectAsState()

    // Debug logging for choreography states
    LaunchedEffect(isWarmingInProgress, isGoingToBeHit, hasBeenHit) {
        Log.v(
            "WaveChoreographies",
            "Choreography state changed: warming=$isWarmingInProgress, goingToBeHit=$isGoingToBeHit, hasBeenHit=$hasBeenHit"
        )
    }

    when {
        isWarmingInProgress -> {
            // Generate warming sequence based on event start time and current time
            val warmingDifference = remember(hitDateTime) {
                val hit = hitDateTime
                if (hit != null) {
                    try {
                        val currentTime = clock.nowAsInstant()
                        val hitTime = hit.epochSeconds
                        val currentTimeSeconds = currentTime.epochSeconds
                        val differenceSeconds = (hitTime - currentTimeSeconds).toInt()

                        // Calculate warming sequence based on time difference
                        when {
                            differenceSeconds > WaveTiming.WARMING_PHASE_6_SECONDS -> 6
                            differenceSeconds > WaveTiming.WARMING_PHASE_5_SECONDS -> 5
                            differenceSeconds > WaveTiming.WARMING_PHASE_4_SECONDS -> 4
                            differenceSeconds > WaveTiming.WARMING_PHASE_3_SECONDS -> 3
                            differenceSeconds > WaveTiming.WARMING_PHASE_2_SECONDS -> 2
                            else -> 1
                        }
                    } catch (e: Exception) {
                        Log.e("WaveChoreographies", "Error calculating warming phase", throwable = e)
                        1
                    }
                } else 1
            }

            val warmingSequence = remember(warmingDifference) {
                try {
                    event.getWarmingChoreographyManager()?.getDisplayableSequence(warmingDifference)
                } catch (e: Exception) {
                    Log.e("WaveChoreographies", "Error getting warming sequence", throwable = e)
                    null
                }
            }

            TimedSequenceDisplay(
                sequence = warmingSequence,
                clock = clock,
                modifier = modifier
            )
        }
        isGoingToBeHit -> {
            val waitingSequence = remember {
                try {
                    event.getWaitingChoreographyManager()?.getDisplayableSequence(1)
                } catch (e: Exception) {
                    Log.e("WaveChoreographies", "Error getting waiting sequence", throwable = e)
                    null
                }
            }

            TimedSequenceDisplay(
                sequence = waitingSequence,
                clock = clock,
                modifier = modifier
            )
        }
        hasBeenHit -> {
            val hitSequence = remember {
                try {
                    event.getHitChoreographyManager()?.getDisplayableSequence(1)
                } catch (e: Exception) {
                    Log.e("WaveChoreographies", "Error getting hit sequence", throwable = e)
                    null
                }
            }

            TimedSequenceDisplay(
                sequence = hitSequence,
                clock = clock,
                modifier = modifier
            )
        }
    }
}

/**
 * Shared timed sequence display component.
 * Renders choreography sequences with proper timing.
 */
@Composable
fun TimedSequenceDisplay(
    sequence: DisplayableSequence<DrawableResource>?,
    clock: IClock,
    modifier: Modifier = Modifier,
) {
    if (sequence != null) {
        ChoreographyDisplay(
            sequence = sequence,
            clock = clock,
            modifier = modifier
        )
    }
}

/**
 * Shared choreography display component with Canvas rendering.
 * Handles the actual visual choreography animation.
 */
@Composable
fun ChoreographyDisplay(
    sequence: DisplayableSequence<DrawableResource>?,
    clock: IClock,
    modifier: Modifier = Modifier,
) {
    var currentStepIndex by remember { mutableIntStateOf(0) }
    var hasStarted by remember { mutableStateOf(false) }

    sequence?.let { seq ->
        val steps = seq.steps

        // Animation timing logic
        LaunchedEffect(seq.id) {
            hasStarted = false
            currentStepIndex = 0

            // Wait for sequence start time
            while (isActive && !hasStarted) {
                val currentTime = clock.nowAsInstant()
                if (currentTime >= seq.startTime) {
                    hasStarted = true
                } else {
                    delay(50) // Check every 50ms
                }
            }

            // Step through sequence
            while (isActive && hasStarted && currentStepIndex < steps.size) {
                delay(steps[currentStepIndex].durationMs)
                currentStepIndex++
            }
        }

        if (hasStarted && currentStepIndex < steps.size) {
            val currentStep = steps[currentStepIndex]

            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(ChoreographyConstants.CHOREOGRAPHY_PADDING.dp)
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.8f))
                    .border(
                        width = 2.dp,
                        color = Color.White,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Display choreography text
                    Text(
                        text = stringResource(currentStep.textResource),
                        style = sharedQuinaryColoredBoldTextStyle(ChoreographyConstants.CHOREOGRAPHY_TEXT_SIZE.toInt()),
                        textAlign = TextAlign.Center,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Display choreography image if available
                    currentStep.imageResource?.let { imageRes ->
                        androidx.compose.foundation.Image(
                            painter = painterResource(imageRes),
                            contentDescription = null,
                            modifier = Modifier
                                .widthIn(max = 200.dp)
                                .heightIn(max = 200.dp)
                        )
                    }
                }
            }
        }
    }
}