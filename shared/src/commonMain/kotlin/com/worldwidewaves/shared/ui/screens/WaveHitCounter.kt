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

package com.worldwidewaves.shared.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.worldwidewaves.shared.WWWGlobals.DisplayText
import com.worldwidewaves.shared.WWWGlobals.WaveDisplay
import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.ui.theme.onPrimaryLight
import com.worldwidewaves.shared.ui.theme.sharedPrimaryColoredBoldTextStyle
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

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
                style = sharedPrimaryColoredBoldTextStyle(WaveDisplay.TIMEBEFOREHIT_FONTSIZE),
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
