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
import com.worldwidewaves.shared.ui.components.AutoResizeSingleLineText
import com.worldwidewaves.shared.ui.theme.sharedQuinaryColoredBoldTextStyle
import dev.icerock.moko.resources.compose.stringResource

@Composable
fun UserWaveStatusText(
    event: IWWWEvent,
    modifier: Modifier = Modifier,
) {
    val eventStatus by event.observer.eventStatus.collectAsState(IWWWEvent.Status.UNDEFINED)
    val hasBeenHit by event.observer.userHasBeenHit.collectAsState()
    val isInArea by event.observer.userIsInArea.collectAsState()
    val isWarming by event.observer.isUserWarmingInProgress.collectAsState()

    val message =
        when {
            eventStatus == IWWWEvent.Status.DONE -> MokoRes.strings.wave_done
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
            style = sharedQuinaryColoredBoldTextStyle(WaveDisplay.BEREADY_FONTSIZE),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
    }
}