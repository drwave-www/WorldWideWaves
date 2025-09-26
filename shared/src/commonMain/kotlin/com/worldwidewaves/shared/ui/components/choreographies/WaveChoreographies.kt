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

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.worldwidewaves.shared.WWWGlobals.WaveTiming
import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.events.utils.IClock
import com.worldwidewaves.shared.utils.Log
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import kotlin.math.max
import kotlin.time.ExperimentalTime

// Constants for choreography display
private object ChoreographyConstants {
    const val CHOREOGRAPHY_SIZE = 200 // Size for choreography sprites
    const val WARMING_FRAME_DURATION_MS = 500L // Duration each warming frame shows
}

/**
 * EXACT historical choreography implementation moved to shared.
 * High-level choreography container displayed on the **Wave** screen.
 *
 * Behaviour:
 * • Subscribes to various flags exposed by [IWWWEvent.observer] to know whether
 *   the user is currently warming-up, about to be hit, or has just been hit.
 * • According to those flags it queries the proper `ChoreographyManager`
 *   (warming / waiting / hit) to obtain a [DisplayableSequence].
 * • The resulting animation is shown while leaving room at the
 *   bottom for the count-down timer that the parent screen overlays.
 */
@OptIn(ExperimentalTime::class)
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
            "[CHOREO_DEBUG] State change for ${event.id}: warming=$isWarmingInProgress, " +
                "goingToBeHit=$isGoingToBeHit, hasBeenHit=$hasBeenHit",
        )
    }

    // State to track if we should show the hit sequence
    var showHitSequence by remember { mutableStateOf(false) }

    // For warming sequences - use recomposition to update sequence
    var warmingKey by remember { mutableIntStateOf(0) }

    // Calculate and schedule the hiding of the hit sequence
    LaunchedEffect(hasBeenHit, hitDateTime) {
        if (hasBeenHit) {
            val currentTime = clock.now()
            val secondsSinceHit = (currentTime - hitDateTime).inWholeSeconds

            if (secondsSinceHit in 0..WaveTiming.SHOW_HIT_SEQUENCE_SECONDS.inWholeSeconds) {
                showHitSequence = true

                // Calculate remaining time to show
                val remainingTimeMs =
                    max(
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
            // Get the current sequence using suspend function
            var warmingSequence by remember { mutableStateOf<com.worldwidewaves.shared.choreographies.ChoreographyManager.DisplayableSequence<DrawableResource>?>(null) }

            LaunchedEffect(warmingKey) {
                val sequence = event.warming.getCurrentChoregraphySequence()
                Log.v("WaveChoreographies", "[CHOREO_DEBUG] Warming sequence for ${event.id}: $sequence")
                warmingSequence = sequence
            }

            // When this sequence ends, request a new one
            if (warmingSequence != null) {
                Log.v("WaveChoreographies", "[CHOREO_DEBUG] Showing warming sequence for ${event.id}")
                TimedSequenceDisplay(
                    sequence = warmingSequence,
                    clock = clock,
                    modifier = modifier.fillMaxWidth().padding(bottom = 120.dp), // Leave space for counter
                    onSequenceComplete = { warmingKey++ },
                )
            } else {
                Log.v("WaveChoreographies", "[CHOREO_DEBUG] Warming sequence is NULL for ${event.id}")
            }
        }

        // Show waiting choreography when going to be hit
        isGoingToBeHit -> {
            ChoreographyDisplay(
                event.wave.waitingChoregraphySequence(),
                clock,
                modifier.fillMaxWidth().padding(bottom = 120.dp), // Leave space for counter
            )
        }

        // Show hit choreography when user has been hit and within time window
        showHitSequence -> {
            ChoreographyDisplay(
                event.wave.hitChoregraphySequence(),
                clock,
                modifier.fillMaxWidth().padding(bottom = 120.dp), // Leave space for counter
            )
        }
    }
}

/**
 * Displays a single choreography sprite image.
 */
@Composable
private fun ChoreographySprite(
    resource: DrawableResource,
    contentDescription: String,
) {
    Image(
        painter = painterResource(resource),
        contentDescription = contentDescription,
        modifier = Modifier.size(ChoreographyConstants.CHOREOGRAPHY_SIZE.dp),
        contentScale = ContentScale.Fit,
    )
}

/**
 * Displays the warming sequence with frame animation.
 * Cycles through the 6 warming sequence frames.
 */
@Composable
private fun WarmingSequence(
    key: Int,
    onComplete: () -> Unit,
) {
    var currentFrame by remember(key) { mutableIntStateOf(0) }

    val warmingFrames = listOf(
        Res.drawable.e_choreography_warming_seq_1,
        Res.drawable.e_choreography_warming_seq_2,
        Res.drawable.e_choreography_warming_seq_3,
        Res.drawable.e_choreography_warming_seq_4,
        Res.drawable.e_choreography_warming_seq_5,
        Res.drawable.e_choreography_warming_seq_6,
    )

    // Auto-advance frames
    LaunchedEffect(key, currentFrame) {
        if (currentFrame < warmingFrames.size - 1) {
            delay(ChoreographyConstants.WARMING_FRAME_DURATION_MS)
            currentFrame++
        } else {
            delay(ChoreographyConstants.WARMING_FRAME_DURATION_MS)
            onComplete()
        }
    }

    if (currentFrame < warmingFrames.size) {
        ChoreographySprite(
            resource = warmingFrames[currentFrame],
            contentDescription = "Warming sequence frame ${currentFrame + 1}"
        )
    }
}
