package com.worldwidewaves.activities.event

/*
 * Copyright 2024 DrWave
 *
 * WorldWideWaves is an ephemeral mobile app designed to orchestrate human waves through cities and
 * countries, culminating in a global wave. The project aims to transcend physical and cultural
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

import android.annotation.SuppressLint
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.worldwidewaves.compose.map.AndroidEventMap
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_EVENT_MAP_RATIO
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_WAVE_BEREADY_FONTSIZE
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_WAVE_BEREADY_PADDING
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_WAVE_PROGRESSION_FONTSIZE
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_WAVE_PROGRESSION_HEIGHT
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_WAVE_TIMEBEFOREHIT_FONTSIZE
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_WAVE_TRIANGLE_SIZE
import com.worldwidewaves.shared.WWWGlobals.Companion.WAVE_SHOW_HIT_SEQUENCE_SECONDS
import com.worldwidewaves.shared.choreographies.ChoreographyManager.DisplayableSequence
import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.events.IWWWEvent.Status
import com.worldwidewaves.shared.events.utils.IClock
import com.worldwidewaves.shared.generated.resources.wave_be_ready
import com.worldwidewaves.shared.generated.resources.wave_done
import com.worldwidewaves.shared.generated.resources.wave_hit
import com.worldwidewaves.shared.generated.resources.wave_is_running
import com.worldwidewaves.theme.extendedLight
import com.worldwidewaves.theme.extraElementsLight
import com.worldwidewaves.theme.onPrimaryLight
import com.worldwidewaves.theme.onQuaternaryLight
import com.worldwidewaves.theme.onQuinaryLight
import com.worldwidewaves.theme.primaryColoredBoldTextStyle
import com.worldwidewaves.theme.quinaryColoredBoldTextStyle
import com.worldwidewaves.theme.quinaryLight
import com.worldwidewaves.theme.tertiaryLight
import com.worldwidewaves.viewmodels.WaveViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.android.ext.android.inject
import kotlin.math.min
import kotlin.time.Duration
import kotlin.time.Duration.Companion.INFINITE
import kotlin.time.Duration.Companion.hours
import com.worldwidewaves.shared.generated.resources.Res as ShRes

class WaveActivity : AbstractEventWaveActivity() {

    private val clock: IClock by inject()

    // ------------------------------------------------------------------------

    @Composable
    override fun Screen(modifier: Modifier, event: IWWWEvent) {
        val context = LocalContext.current

        // States
        var hasPlayedHitSound = false

        // Calculate height based on aspect ratio and available width
        val configuration = LocalConfiguration.current
        val calculatedHeight = configuration.screenWidthDp.dp / DIM_EVENT_MAP_RATIO

        // Get choreography-related states
        val isWarmingInProgress by waveViewModel.getIsWarmingInProgressFlow(observerId).collectAsState()
        val isGoingToBeHit by waveViewModel.getIsGoingToBeHitFlow(observerId).collectAsState()
        val hasBeenHit by waveViewModel.getHasBeenHitFlow(observerId).collectAsState()
        val hitDateTime by waveViewModel.getHitDateTimeFlow(observerId).collectAsState()

        // Construct the event Map
        val eventMap = remember(event.id) {
            AndroidEventMap(event,
                onLocationUpdate = { newLocation ->
                    waveViewModel.updateUserLocation(observerId, newLocation)
                },
                onMapClick = {
                    context.startActivity(Intent(context, EventFullMapActivity::class.java).apply {
                        putExtra("eventId", event.id)
                    })
                }
            )
        }

        // Start event/map coordination
        ObserveEventMap(event, eventMap)

        // - For manual testing purposes - very noisy
        /*LaunchedEffect(true) {
            while (true) {
                delay(50)
                scope.launch {
                    event.warming.playCurrentSoundChoreographyTone()
                }
            }
        }*/

        // Play the hit sound when the user has been hit
        LaunchedEffect(isWarmingInProgress, isGoingToBeHit, hasBeenHit, hitDateTime) {
            val secondsSinceHit = (clock.now() - hitDateTime).inWholeSeconds
            if (hasBeenHit && secondsSinceHit in 0.. 1 && !hasPlayedHitSound) {
                event.warming.playCurrentSoundChoreographyTone()
                hasPlayedHitSound = true
            }
        }

        // Always target the closest view to have user and wave in the same view
        MapZoomAndLocationUpdate(waveViewModel, observerId, eventMap)

        // Screen composition
        Box(modifier = modifier.fillMaxSize()) {

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(30.dp)
            ) {
                BeReady(waveViewModel, observerId)
                eventMap.Screen(modifier = Modifier
                    .fillMaxWidth()
                    .height(calculatedHeight))
                WaveProgressionBar(waveViewModel, observerId)
                WaveHitCounter(waveViewModel, observerId, clock)
            }

            // Pass the visibility state to WaveChoreographies for coordination
            WaveChoreographies(event, waveViewModel, observerId, clock, Modifier.fillMaxSize().zIndex(10f))

        }
    }

}

// ------------------------------------------------------------------------

@Composable
fun MapZoomAndLocationUpdate(waveViewModel: WaveViewModel, observerId: String, eventMap: AndroidEventMap) {
    val scope = rememberCoroutineScope()
    val progression by waveViewModel.getProgressionFlow(observerId).collectAsState()
    val isInArea by waveViewModel.getIsInAreaFlow(observerId).collectAsState()

    LaunchedEffect(progression, isInArea) {
        if (isInArea) {
            scope.launch {
                eventMap.targetUserAndWave()
            }
        }
    }
}

// ------------------------------------------------------------------------

@Composable
fun BeReady(waveViewModel: WaveViewModel, observerId: String, modifier: Modifier = Modifier) {
    val eventStatus by waveViewModel.getEventStatusFlow(observerId).collectAsState(Status.UNDEFINED)
    val hasBeenHit by waveViewModel.getHasBeenHitFlow(observerId).collectAsState()
    val isInArea by waveViewModel.getIsInAreaFlow(observerId).collectAsState()

    val message = if (eventStatus == Status.DONE)
        ShRes.string.wave_done
    else if (hasBeenHit)
        ShRes.string.wave_hit
    else if (isInArea)
        ShRes.string.wave_be_ready
    else
        ShRes.string.wave_is_running

    Box(
        modifier = modifier.padding(vertical = DIM_WAVE_BEREADY_PADDING.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(message),
            style = quinaryColoredBoldTextStyle(DIM_WAVE_BEREADY_FONTSIZE)
        )
    }
}

// ------------------------------------------------------------------------

@SuppressLint("DefaultLocale")
@Composable
fun WaveProgressionBar(waveViewModel: WaveViewModel, observerId: String, modifier: Modifier = Modifier) {
    val progression by waveViewModel.getProgressionFlow(observerId).collectAsState()
    val isInArea by waveViewModel.getIsInAreaFlow(observerId).collectAsState()
    val userPositionRatio by waveViewModel.getUserPositionRatioFlow(observerId).collectAsState()
    val isGoingToBeHit by waveViewModel.getIsGoingToBeHitFlow(observerId).collectAsState()
    val hasBeenHit by waveViewModel.getHasBeenHitFlow(observerId).collectAsState()

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val barWidth = screenWidth * 0.8f

    val density = LocalDensity.current
    val triangleSize = with(density) { DIM_WAVE_TRIANGLE_SIZE.dp.toPx() }

    Column(
        modifier = modifier.width(barWidth),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(DIM_WAVE_PROGRESSION_HEIGHT.dp)
                .clip(RoundedCornerShape(25.dp))
                .background(extendedLight.quaternary.color),
            contentAlignment = Alignment.Center
        ) {
            WaveProgression(progression)

            Text(
                text = "${String.format("%.1f", progression)}%",
                style = primaryColoredBoldTextStyle(DIM_WAVE_PROGRESSION_FONTSIZE),
                color = Color.Black,
                textAlign = TextAlign.Center
            )
        }
        if (isInArea) {
            UserPositionTriangle(userPositionRatio, triangleSize, isGoingToBeHit, hasBeenHit)
        }
    }
}

@Composable
private fun WaveProgression(progression: Double) {
    val density = LocalDensity.current
    val barHeight = with(density) { DIM_WAVE_PROGRESSION_HEIGHT.dp.toPx() }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(barHeight.dp)
    ) {
        val width = size.width
        val height = barHeight // Adjusted height
        val traversedWidth = (width * min(progression, 100.0).toFloat() / 100f)

        // Draw the progression bar
        drawRect(
            color = onQuinaryLight,
            size = Size(traversedWidth, height)
        )
        drawRect(
            color = quinaryLight,
            topLeft = Offset(traversedWidth, 0f),
            size = Size(width - traversedWidth, height)
        )

    }
}

@Composable
fun UserPositionTriangle(userPositionRatio: Double, triangleSize: Float, isGoingToBeHit: Boolean, hasBeenHit: Boolean) {
    val normalColor = extraElementsLight
    val alertColor = tertiaryLight

    var triangleColor = normalColor

    if (isGoingToBeHit) {
        val infiniteTransition = rememberInfiniteTransition(label = "BlinkingTriangleTransition")
        val animatedColor by infiniteTransition.animateColor(
            initialValue = normalColor,
            targetValue = alertColor,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 500),
                repeatMode = RepeatMode.Reverse
            ), label = "BlinkingTriangleColorAnimation"
        )
        triangleColor = animatedColor
    } else if (hasBeenHit) {
        triangleColor = onQuaternaryLight
    }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(triangleSize.toInt().dp)
            .padding(top = 4.dp)
    ) {
        val width = size.width
        val trianglePosition = (width * userPositionRatio).toFloat().coerceIn(0f, width)
        val path = Path().apply {
            moveTo(trianglePosition, 0f)
            lineTo(trianglePosition - triangleSize / 2f, triangleSize)
            lineTo(trianglePosition + triangleSize / 2f, triangleSize)
            close()
        }
        drawPath(path, triangleColor, style = Fill)
    }
}

// ------------------------------------------------------------------------

@Composable
fun WaveHitCounter(waveViewModel: WaveViewModel, observerId: String, clock: IClock, modifier: Modifier = Modifier) {
    val progression by waveViewModel.getProgressionFlow(observerId).collectAsState(0.0)
    val timeBeforeHitProgression by waveViewModel.getTimeBeforeHitFlow(observerId).collectAsState(INFINITE)
    val userHitDateTime by waveViewModel.getHitDateTimeFlow(observerId).collectAsState()
    var timeBeforeHit by remember { mutableStateOf(INFINITE) }

    // Recalculate timeBeforeHit every second until wave is progression
    LaunchedEffect(Unit) {
        while (progression == 0.0) {
            delay(1000L)
            timeBeforeHit = userHitDateTime - clock.now()
        }
    }

    val text = formatDuration(minOf(timeBeforeHit, timeBeforeHitProgression))

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val boxWidth = screenWidth * 0.5f

    Box(
        modifier = modifier
            .width(boxWidth)
            .border(2.dp, onPrimaryLight),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = primaryColoredBoldTextStyle(DIM_WAVE_TIMEBEFOREHIT_FONTSIZE),
            color = Color.White,
            textAlign = TextAlign.Center
        )
    }
}

private fun formatDuration(duration: Duration): String {
    return when {
        duration.isInfinite() -> "--:--"
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
}

// ------------------------------------------------------------------------

@Composable
fun WaveChoreographies(
    event: IWWWEvent,
    waveViewModel: WaveViewModel,
    observerId: String,
    clock: IClock,
    modifier: Modifier = Modifier
) {
    val isWarmingInProgress by waveViewModel.getIsWarmingInProgressFlow(observerId).collectAsState()
    val isGoingToBeHit by waveViewModel.getIsGoingToBeHitFlow(observerId).collectAsState()
    val hasBeenHit by waveViewModel.getHasBeenHitFlow(observerId).collectAsState()
    val hitDateTime by waveViewModel.getHitDateTimeFlow(observerId).collectAsState()

    // State to track if we should show the hit sequence
    var showHitSequence by remember { mutableStateOf(false) }

    // For warming sequences - use recomposition to update sequence
    var warmingKey by remember { mutableIntStateOf(0) }

    // Calculate and schedule the hiding of the hit sequence
    LaunchedEffect(hasBeenHit, hitDateTime) {
        if (hasBeenHit) {
            val currentTime = clock.now()
            val secondsSinceHit = (currentTime - hitDateTime).inWholeSeconds

            if (secondsSinceHit in 0..WAVE_SHOW_HIT_SEQUENCE_SECONDS.inWholeSeconds) {
                showHitSequence = true

                // Calculate remaining time to show
                val remainingTimeMs = maxOf(0,
                    WAVE_SHOW_HIT_SEQUENCE_SECONDS.inWholeMilliseconds -
                            (currentTime - hitDateTime).inWholeMilliseconds
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

    when {

        // Show warming choreography with sequence refresh
        isWarmingInProgress -> {
            // Get the current sequence
            val warmingSequence = remember(warmingKey) {
                event.warming.getCurrentChoregraphySequence()
            }

            // When this sequence ends, request a new one
            if (warmingSequence != null) {
                TimedSequenceDisplay(
                    sequence = warmingSequence,
                    clock = clock,
                    modifier = modifier.zIndex(10f),
                    onSequenceComplete = { warmingKey++ }
                )
            }
        }

        // Show waiting choreography when going to be hit
        isGoingToBeHit -> {
            ChoreographyDisplay(event.wave.waitingChoregraphySequence(), clock, modifier.zIndex(10f))
        }

        // Show hit choreography when user has been hit and within time window
        showHitSequence -> {
            ChoreographyDisplay(event.wave.hitChoregraphySequence(), clock, modifier.zIndex(10f))
        }
    }
}

@Composable
fun TimedSequenceDisplay(
    sequence: DisplayableSequence<DrawableResource>?,
    clock: IClock,
    modifier: Modifier = Modifier,
    onSequenceComplete: () -> Unit
) {
    if (sequence == null) return

    ChoreographyDisplay(sequence, clock, modifier)

    LaunchedEffect(sequence) {
        delay(sequence.remainingDuration ?: sequence.timing)
        onSequenceComplete()
    }
}

@Composable
fun ChoreographyDisplay(
    sequence: DisplayableSequence<DrawableResource>?,
    clock: IClock,
    modifier: Modifier = Modifier
) {
    if (sequence == null || sequence.images.isEmpty()) return

    var currentImageIndex by remember { mutableIntStateOf(0) }
    var isVisible by remember { mutableStateOf(true) }
    val remainingTime by remember(sequence) { mutableStateOf(sequence.remainingDuration) }

    // Create a timer to cycle through images
    LaunchedEffect(sequence) {
        val startTime = clock.now()

        while (this.isActive) {
            // Check if we should stop showing the sequence
            if (remainingTime != null) {
                val elapsed = clock.now() - startTime
                if (elapsed > remainingTime!!) break
            }

            delay(sequence.timing.inWholeMilliseconds)
            isVisible = false

            if (sequence.loop || currentImageIndex < sequence.images.size - 1) {
                currentImageIndex = (currentImageIndex + 1) % sequence.images.size
                isVisible = true
            } else {
                break // Stop if we've shown all images and not looping
            }
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 400.dp) // Maximum width
                .heightIn(max = 600.dp) // Maximum height
                .padding(24.dp) // Outer padding
                .shadow(8.dp)
                .background(Color.Black.copy(alpha = 0.7f))
                .border(2.dp, Color.White, RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp))
                .padding(24.dp), // Inner padding
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    this@Column.AnimatedVisibility(visible = isVisible) {
                        Image(
                            painter = painterResource(sequence.images[currentImageIndex]),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Text(
                    modifier = Modifier.padding(top = 16.dp),
                    text = sequence.text,
                    style = quinaryColoredBoldTextStyle(24),
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}