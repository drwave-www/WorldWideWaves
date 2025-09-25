package com.worldwidewaves.shared.ui.components

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

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.worldwidewaves.shared.WWWGlobals.DisplayText
import com.worldwidewaves.shared.WWWGlobals.WaveDisplay
import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.ui.theme.platformOnPrimaryLight
import com.worldwidewaves.shared.ui.utils.formatDuration

/**
 * Shared WaveHitCounter component - EXACT replica of original Android implementation.
 * Uses platform abstractions for styling while maintaining identical countdown behavior.
 * Works identically on both Android and iOS platforms.
 */
@Composable
fun SharedWaveHitCounter(
    event: IWWWEvent,
    modifier: Modifier = Modifier,
) {
    val timeBeforeHitProgression by event.observer.timeBeforeHit.collectAsState()
    val timeBeforeHit by event.observer.timeBeforeHit.collectAsState()

    val text = formatDuration(minOf(timeBeforeHit, timeBeforeHitProgression))

    if (text != DisplayText.EMPTY_COUNTER) {
        // Platform-specific screen width calculation
        val boxWidth = PlatformCalculateScreenWidth() * 0.5f

        Box(
            modifier = modifier
                .width(boxWidth)
                .border(2.dp, platformOnPrimaryLight),
            contentAlignment = Alignment.Center,
        ) {
            SharedAutoSizeText(
                text = text,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = WaveDisplay.TIMEBEFOREHIT_FONTSIZE.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                ),
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
            )
        }
    }
}

/**
 * Platform-specific screen width calculation.
 * Android: Uses WindowInfo
 * iOS: Uses appropriate screen width calculation
 */
@Composable
expect fun PlatformCalculateScreenWidth(): androidx.compose.ui.unit.Dp