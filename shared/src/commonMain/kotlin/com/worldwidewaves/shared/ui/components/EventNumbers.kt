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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.worldwidewaves.shared.MokoRes
import com.worldwidewaves.shared.WWWGlobals.Dimensions
import com.worldwidewaves.shared.WWWGlobals.Event
import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.events.IWWWEvent.WaveNumbersLiterals
import com.worldwidewaves.shared.format.DateTimeFormats
import com.worldwidewaves.shared.ui.theme.quinaryLight
import com.worldwidewaves.shared.ui.utils.AutoResizeSingleLineText
import com.worldwidewaves.shared.ui.utils.formatDurationMinutes
import dev.icerock.moko.resources.compose.stringResource
import kotlin.time.ExperimentalTime

/**
 * Shared EventNumbers component - EXACT replica of original Android implementation.
 * Uses platform abstractions for styling while maintaining identical business logic.
 * Works identically on both Android and iOS platforms.
 */
@OptIn(ExperimentalTime::class)
@Composable
fun SharedEventNumbers(
    event: IWWWEvent,
    modifier: Modifier = Modifier,
) {
    var waveNumbers by remember { mutableStateOf<WaveNumbersLiterals?>(null) }
    var totalMinutes by remember { mutableStateOf<Long?>(null) }
    var startTimeText by remember { mutableStateOf<String?>(null) }
    var endTimeText by remember { mutableStateOf<String?>(null) }
    val progression by event.observer.progression.collectAsState()
    val startWarmingInProgress by event.observer.isStartWarmingInProgress.collectAsState()
    val warmingText = stringResource(MokoRes.strings.wave_warming)

    // EXACT original: Initial load – compute static numbers & start time
    LaunchedEffect(event.id) {
        waveNumbers = event.getAllNumbers()
        val start = event.getStartDateTime()
        startTimeText = DateTimeFormats.timeShort(start, event.getTZ())
    }

    // EXACT original: Recompute values that depend on polygons/duration
    LaunchedEffect(event.id, progression) {
        totalMinutes = event.getTotalTime().inWholeMinutes
        endTimeText = DateTimeFormats.timeShort(event.getEndDateTime(), event.getTZ())
    }

    val eventNumbers by remember(waveNumbers) {
        derivedStateOf {
            waveNumbers?.let {
                mapOf(
                    MokoRes.strings.wave_start_time to it.waveStartTime,
                    MokoRes.strings.wave_end_time to it.waveEndTime,
                    MokoRes.strings.wave_speed to it.waveSpeed,
                    MokoRes.strings.wave_total_time to it.waveTotalTime,
                    MokoRes.strings.wave_progression to
                        if (startWarmingInProgress) {
                            warmingText
                        } else {
                            event.wave.getLiteralFromProgression(progression)
                        },
                )
            } ?: emptyMap()
        }
    }

    val eventTimeZone = waveNumbers?.waveTimezone
    val orderedLabels = listOf(
        MokoRes.strings.wave_start_time,
        MokoRes.strings.wave_end_time,
        MokoRes.strings.wave_speed,
        MokoRes.strings.wave_total_time,
        MokoRes.strings.wave_progression,
    )

    Box(
        modifier = modifier.padding(
            start = Dimensions.DEFAULT_EXT_PADDING.dp,
            end = Dimensions.DEFAULT_EXT_PADDING.dp
        )
    ) {
        Box(
            modifier = Modifier
                .border(
                    width = Event.NUMBERS_BORDERWIDTH.dp,
                    color = quinaryLight,
                    shape = RoundedCornerShape(
                        topStart = Event.NUMBERS_BORDERROUND.dp,
                        bottomEnd = Event.NUMBERS_BORDERROUND.dp,
                    ),
                )
                .padding(Dimensions.DEFAULT_EXT_PADDING.dp),
        ) {
            Column(
                modifier = Modifier.padding(
                    start = Dimensions.DEFAULT_INT_PADDING.dp,
                    end = Dimensions.DEFAULT_INT_PADDING.dp
                )
            ) {
                AutoResizeSingleLineText(
                    text = stringResource(MokoRes.strings.be_waved),
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = Event.NUMBERS_TITLE_FONTSIZE.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE0E0E0)
                    )
                        .copy(textAlign = TextAlign.End),
                    textAlign = TextAlign.End,
                )
                Spacer(modifier = Modifier.height(Event.NUMBERS_SPACER.dp))
                if (eventNumbers.isNotEmpty()) {
                    orderedLabels.forEach { key ->
                        val value = eventNumbers[key]!!
                        val displayValue = if (key == MokoRes.strings.wave_total_time) {
                            formatDurationMinutes(totalMinutes, value)
                        } else if (key == MokoRes.strings.wave_start_time && startTimeText != null) {
                            startTimeText!!
                        } else if (key == MokoRes.strings.wave_end_time && endTimeText != null) {
                            endTimeText!!
                        } else {
                            value
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = stringResource(key),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = Event.NUMBERS_LABEL_FONTSIZE.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE0E0E0)
                                ),
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = displayValue,
                                    style = MaterialTheme.typography.headlineLarge.copy(
                                        fontSize = Event.NUMBERS_VALUE_FONTSIZE.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = when (key) {
                                            MokoRes.strings.wave_progression -> MaterialTheme.colorScheme.secondary
                                            MokoRes.strings.wave_start_time -> Color.Yellow
                                            else -> MaterialTheme.colorScheme.primary
                                        },
                                    ),
                                )
                                if (key in listOf(
                                        MokoRes.strings.wave_start_time,
                                        MokoRes.strings.wave_end_time,
                                    )
                                ) {
                                    Text(
                                        text = " $eventTimeZone",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = Event.NUMBERS_TZ_FONTSIZE.sp,
                                            fontWeight = FontWeight.Light,
                                            color = when (key) {
                                                MokoRes.strings.wave_start_time -> Color.Yellow
                                                else -> MaterialTheme.colorScheme.primary
                                            },
                                        ),
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(Event.NUMBERS_SPACER.dp / 2))
                    }
                }
            }
        }
    }
}