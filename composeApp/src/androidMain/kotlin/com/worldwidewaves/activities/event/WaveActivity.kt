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
@file:OptIn(ExperimentalTime::class)

package com.worldwidewaves.activities.event

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.worldwidewaves.compose.map.AndroidEventMap
import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.events.utils.IClock
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class WaveActivity : AbstractEventWaveActivity() {
    private val clock: IClock by inject()

    @Composable
    override fun Screen(
        modifier: Modifier,
        event: IWWWEvent,
    ) {
        val context = LocalContext.current

        // Construct the event Map
        val eventMap =
            remember(event.id) {
                AndroidEventMap(
                    event,
                    activityContext = context,
                    onMapClick = {
                        context.startActivity(
                            Intent(context, EventFullMapActivity::class.java).apply {
                                putExtra("eventId", event.id)
                            },
                        )
                    },
                )
            }

        // Start event/map coordination
        ObserveEventMapProgression(event, eventMap)

        // Always target the closest view to have user and wave in the same view
        MapZoomAndLocationUpdate(event, eventMap)

        // Use the complete shared wave screen with exact working behavior
        com.worldwidewaves.shared.ui.screens.SharedWaveScreen(
            event = event,
            modifier = modifier,
            mapContent = { mapModifier ->
                eventMap.Screen(
                    autoMapDownload = true,
                    modifier = mapModifier,
                )
            }
        )
    }
}

@Composable
fun MapZoomAndLocationUpdate(
    event: IWWWEvent,
    eventMap: AndroidEventMap,
) {
    val scope = rememberCoroutineScope()
    val progression by event.observer.progression.collectAsState()
    val isInArea by event.observer.userIsInArea.collectAsState()

    LaunchedEffect(progression, isInArea) {
        if (isInArea) {
            scope.launch {
                eventMap.targetUserAndWave()
            }
        }
    }
}

@Composable
fun UserWaveStatusText(
    event: IWWWEvent,
    modifier: Modifier = Modifier,
) {
    val eventStatus by event.observer.eventStatus.collectAsState(Status.UNDEFINED)
    val hasBeenHit by event.observer.userHasBeenHit.collectAsState()
    val isInArea by event.observer.userIsInArea.collectAsState()
    val isWarming by event.observer.isUserWarmingInProgress.collectAsState()

    val message =
        when {
            eventStatus == Status.DONE -> MokoRes.strings.wave_done
            hasBeenHit -> MokoRes.strings.wave_hit
            isWarming && isInArea -> MokoRes.strings.wave_warming
            isInArea -> MokoRes.strings.wave_be_ready
            else -> MokoRes.strings.wave_is_running
        }

    Box(
        modifier = modifier.padding(vertical = WaveDisplay.BEREADY_PADDING.dp),
        contentAlignment = Alignment.Center,
    ) {
        AutoResizeSingleLineText(
            text = stringResource(message),
            style = quinaryColoredBoldTextStyle(WaveDisplay.BEREADY_FONTSIZE),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
    }
}

@SuppressLint("DefaultLocale")
@Composable
fun WaveProgressionBar(
    event: IWWWEvent,
    modifier: Modifier = Modifier,
) {
    val progression by event.observer.progression.collectAsState()
    val isInArea by event.observer.userIsInArea.collectAsState()
    val userPositionRatio by event.observer.userPositionRatio.collectAsState()
    val isGoingToBeHit by event.observer.userIsGoingToBeHit.collectAsState()
    val hasBeenHit by event.observer.userHasBeenHit.collectAsState()

    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current
    val screenWidth = with(density) { windowInfo.containerSize.width.toDp() }
    val barWidth = screenWidth * 0.8f

    val triangleSize = with(density) { WaveDisplay.TRIANGLE_SIZE.dp.toPx() }

    Column(
        modifier = modifier.width(barWidth),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(WaveDisplay.PROGRESSION_HEIGHT.dp)
                    .clip(RoundedCornerShape(25.dp))
                    .background(extendedLight.quaternary.color),
            contentAlignment = Alignment.Center,
        ) {
            WaveProgressionFillArea(progression)

            Text(
                text = "${String.format("%.1f", progression)}%",
                style = primaryColoredBoldTextStyle(WaveDisplay.PROGRESSION_FONTSIZE),
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
    val density = LocalDensity.current
    val barHeight = with(density) { WaveDisplay.PROGRESSION_HEIGHT.dp.toPx() }

    Canvas(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(barHeight.dp),
    ) {
        val width = size.width
        val height = barHeight
        val traversedWidth = (width * min(progression, 100.0).toFloat() / 100f)

        drawRect(
            color = onQuinaryLight,
            size = Size(traversedWidth, height),
        )
        drawRect(
            color = quinaryLight,
            topLeft = Offset(traversedWidth, 0f),
            size = Size(width - traversedWidth, height),
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
    val triangleColor =
        when {
            isGoingToBeHit -> {
                val infiniteTransition = rememberInfiniteTransition(label = "BlinkingTriangleTransition")
                val animatedColor by infiniteTransition.animateColor(
                    initialValue = extraElementsLight,
                    targetValue = tertiaryLight,
                    animationSpec =
                        infiniteRepeatable(
                            animation = tween(durationMillis = 500),
                            repeatMode = RepeatMode.Reverse,
                        ),
                    label = "BlinkingTriangleColorAnimation",
                )
                animatedColor
            }
            hasBeenHit -> onQuaternaryLight
            else -> extraElementsLight
        }

    Canvas(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(triangleSize.toInt().dp)
                .padding(top = 4.dp),
    ) {
        val width = size.width
        val trianglePosition = (width * userPositionRatio).toFloat().coerceIn(0f, width)
        val path =
            Path().apply {
                moveTo(trianglePosition, 0f)
                lineTo(trianglePosition - triangleSize / 2f, triangleSize)
                lineTo(trianglePosition + triangleSize / 2f, triangleSize)
                close()
            }
        drawPath(path, triangleColor, style = Fill)
    }
}

@Composable
fun WaveHitCounter(
    event: IWWWEvent,
    modifier: Modifier = Modifier,
) {
    val timeBeforeHitProgression by event.observer.timeBeforeHit.collectAsState()
    val timeBeforeHit by event.observer.timeBeforeHit.collectAsState()

    val text = formatDuration(minOf(timeBeforeHit, timeBeforeHitProgression))

    if (text != DisplayText.EMPTY_COUNTER) {
        val windowInfo = LocalWindowInfo.current
        val density = LocalDensity.current
        val screenWidthDp = with(density) { windowInfo.containerSize.width.toDp() }
        val boxWidth = screenWidthDp * 0.5f

        Box(
            modifier =
                modifier
                    .width(boxWidth)
                    .border(2.dp, onPrimaryLight),
            contentAlignment = Alignment.Center,
        ) {
            AutoSizeText(
                text = text,
                style = primaryColoredBoldTextStyle(WaveDisplay.TIMEBEFOREHIT_FONTSIZE),
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
            )
        }
    }
}

@Composable
fun AutoSizeText(
    text: String,
    style: TextStyle,
    color: Color,
    textAlign: TextAlign,
    maxLines: Int,
    modifier: Modifier = Modifier,
) {
    var fontSize by remember { mutableStateOf(style.fontSize) }

    Text(
        text = text,
        style = style.copy(fontSize = fontSize),
        color = color,
        textAlign = textAlign,
        maxLines = maxLines,
        softWrap = false,
        onTextLayout = { textLayoutResult ->
            if (textLayoutResult.hasVisualOverflow) {
                fontSize = fontSize * 0.9f
            }
        },
        modifier = modifier,
    )
}

private fun formatDuration(duration: Duration): String =
    when {
        duration.isInfinite() || duration < Duration.ZERO -> "--:--"
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

        else -> DisplayText.EMPTY_COUNTER
    }