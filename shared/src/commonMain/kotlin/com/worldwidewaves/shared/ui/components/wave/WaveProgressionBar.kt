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

package com.worldwidewaves.shared.ui.components.wave

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.worldwidewaves.shared.WWWGlobals.WaveDisplay
import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.ui.theme.onQuaternaryLight
import com.worldwidewaves.shared.ui.theme.onQuinaryLight
import com.worldwidewaves.shared.ui.theme.quinaryLight
import com.worldwidewaves.shared.ui.theme.sharedExtendedLight
import com.worldwidewaves.shared.ui.theme.sharedPrimaryColoredBoldTextStyle
import com.worldwidewaves.shared.ui.theme.tertiaryLight
import kotlin.math.min

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
                    .background(sharedExtendedLight.quaternary.color),
            contentAlignment = Alignment.Center,
        ) {
            WaveProgressionFillArea(progression)

            Text(
                text = "${progression.toString().take(4)}%",
                style = sharedPrimaryColoredBoldTextStyle(WaveDisplay.PROGRESSION_FONTSIZE),
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
                    initialValue = onQuaternaryLight,
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
            else -> onQuaternaryLight
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
