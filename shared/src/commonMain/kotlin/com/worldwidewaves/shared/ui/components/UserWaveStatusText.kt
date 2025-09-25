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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.worldwidewaves.shared.MokoRes
import com.worldwidewaves.shared.WWWGlobals.WaveDisplay
import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.events.IWWWEvent.Status
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.worldwidewaves.shared.ui.utils.AutoResizeSingleLineText
import dev.icerock.moko.resources.compose.stringResource

/**
 * Shared UserWaveStatusText component - EXACT replica of original Android implementation.
 * Uses platform abstractions for styling while maintaining identical status logic.
 * Works identically on both Android and iOS platforms.
 */
@Composable
fun SharedUserWaveStatusText(
    event: IWWWEvent,
    modifier: Modifier = Modifier,
) {
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
        modifier = modifier.padding(vertical = WaveDisplay.BEREADY_PADDING.dp),
        contentAlignment = Alignment.Center,
    ) {
        AutoResizeSingleLineText(
            text = stringResource(message),
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = WaveDisplay.BEREADY_FONTSIZE.sp,
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
    }
}