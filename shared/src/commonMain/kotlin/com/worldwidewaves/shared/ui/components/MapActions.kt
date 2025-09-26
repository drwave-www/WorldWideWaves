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

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.worldwidewaves.shared.MokoRes
import com.worldwidewaves.shared.WWWGlobals.Dimensions
import com.worldwidewaves.shared.WWWGlobals.Event
import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.events.IWWWEvent.Status
import com.worldwidewaves.shared.events.utils.IClock
import com.worldwidewaves.shared.generated.resources.Res
import com.worldwidewaves.shared.generated.resources.target_wave_active
import com.worldwidewaves.shared.generated.resources.target_wave_inactive
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.time.ExperimentalTime

/**
 * Shared map actions component for map interaction controls.
 * Provides target wave and center wave functionality.
 * Works identically on both Android and iOS platforms.
 */
@OptIn(ExperimentalTime::class)
@Composable
fun SharedMapActions(
    event: IWWWEvent,
    modifier: Modifier = Modifier,
    onTargetWave: () -> Unit = {},
    onCenterWave: () -> Unit = {},
) {
    val clockComponent =
        object : KoinComponent {
            val clock: IClock by inject()
        }
    val clock = clockComponent.clock

    val scope = rememberCoroutineScope()
    val eventStatus by event.observer.eventStatus.collectAsState(Status.UNDEFINED)
    val isInArea by event.observer.userIsInArea.collectAsState()

    val isRunning = eventStatus == Status.RUNNING

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .padding(
                    end = Dimensions.DEFAULT_INT_PADDING.dp,
                    bottom = Dimensions.DEFAULT_INT_PADDING.dp,
                ),
        contentAlignment = Alignment.BottomEnd,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(Dimensions.DEFAULT_INT_PADDING.dp)) {
            // Target wave button
            Image(
                modifier =
                    Modifier
                        .size(Event.TARGET_WAVE_IMAGE_SIZE.dp)
                        .clickable {
                            if (isRunning && (clock.now() > event.getWaveStartDateTime())) {
                                scope.launch {
                                    onTargetWave()
                                }
                            }
                        },
                painter =
                    painterResource(
                        if (isRunning) Res.drawable.target_wave_active else Res.drawable.target_wave_inactive,
                    ),
                contentDescription =
                    stringResource(
                        if (isRunning) MokoRes.strings.event_target_wave_on else MokoRes.strings.event_target_wave_off,
                    ),
            )

            // Center wave button
            Image(
                modifier =
                    Modifier
                        .size(Event.TARGET_WAVE_IMAGE_SIZE.dp)
                        .clickable {
                            scope.launch {
                                onCenterWave()
                            }
                        },
                painter = painterResource(Res.drawable.target_wave_inactive),
                contentDescription = stringResource(MokoRes.strings.map_cancel_download),
            )
        }
    }
}
