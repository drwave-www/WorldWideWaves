package com.worldwidewaves.activities.event

/*
 * Copyright 2025 DrWave
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

import android.content.Context
import android.content.Intent
import android.text.BidiFormatter
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.worldwidewaves.BuildConfig
import com.worldwidewaves.compose.ButtonWave
import com.worldwidewaves.compose.DividerLine
import com.worldwidewaves.compose.EventOverlayDone
import com.worldwidewaves.compose.EventOverlaySoonOrRunning
import com.worldwidewaves.compose.WWWSocialNetworks
import com.worldwidewaves.compose.map.AndroidEventMap
import com.worldwidewaves.shared.MokoRes
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_DEFAULT_EXT_PADDING
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_DEFAULT_INT_PADDING
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_EVENT_DATE_FONTSIZE
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_EVENT_DATE_MITER
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_EVENT_DATE_STROKE
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_EVENT_DESC_FONTSIZE
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_EVENT_GEOLOCME_BORDER
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_EVENT_GEOLOCME_FONTSIZE
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_EVENT_GEOLOCME_HEIGHT
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_EVENT_MAP_RATIO
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_EVENT_NUMBERS_BORDERROUND
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_EVENT_NUMBERS_BORDERWIDTH
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_EVENT_NUMBERS_LABEL_FONTSIZE
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_EVENT_NUMBERS_SPACER
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_EVENT_NUMBERS_TITLE_FONTSIZE
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_EVENT_NUMBERS_TZ_FONTSIZE
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_EVENT_NUMBERS_VALUE_FONTSIZE
import com.worldwidewaves.shared.WWWPlatform
import com.worldwidewaves.shared.WWWSimulation
import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.events.IWWWEvent.Status
import com.worldwidewaves.shared.events.IWWWEvent.WaveNumbersLiterals
import com.worldwidewaves.shared.events.utils.IClock
import com.worldwidewaves.shared.events.utils.Log
import com.worldwidewaves.theme.extraBoldTextStyle
import com.worldwidewaves.theme.extraLightTextStyle
import com.worldwidewaves.theme.extraQuinaryColoredBoldTextStyle
import com.worldwidewaves.theme.onPrimaryLight
import com.worldwidewaves.theme.quinaryColoredTextStyle
import com.worldwidewaves.theme.quinaryLight
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.koin.android.ext.android.inject
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
class EventActivity : AbstractEventWaveActivity() {

    private val clock: IClock by inject()
    private val platform: WWWPlatform by inject()

    // ------------------------------------------------------------------------

    @Composable
    override fun Screen(modifier: Modifier, event: IWWWEvent) {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val eventStatus by event.observer.eventStatus.collectAsState(Status.UNDEFINED)
        val endDateTime = remember { mutableStateOf<Instant?>(null) }

        LaunchedEffect(event) {
            endDateTime.value = event.getEndDateTime()
        }

        // Calculate height based on aspect ratio and available width
        val windowInfo = LocalWindowInfo.current
        val density = LocalDensity.current
        val screenWidthDp = with(density) { windowInfo.containerSize.width.toDp() }
        val calculatedHeight = screenWidthDp / DIM_EVENT_MAP_RATIO

        // Construct the event map
        val eventMap = remember(event.id) {
            AndroidEventMap(event,
                onMapClick = {
                    context.startActivity(Intent(context, EventFullMapActivity::class.java).apply {
                        putExtra("eventId", event.id)
                    })
                }
            )
        }

        // Start event/map coordination
        ObserveEventMapProgression(event, eventMap)

        // Screen composition
        Box {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(30.dp)
            ) {
                EventOverlay(event)
                EventDescription(event)
                DividerLine()
                
                // Wave button row with relative positioning for test button
                Box(modifier = Modifier.fillMaxWidth()) {
                    ButtonWave(
                        event.id, 
                        eventStatus, 
                        endDateTime.value, 
                        clock,
                        modifier = Modifier.align(Alignment.Center)
                    )
                    
                    // Debug test button
                    if (BuildConfig.DEBUG) {
                        SimulationButton(scope, event, context)
                    }
                }
                
                eventMap.Screen(modifier = Modifier.fillMaxWidth().height(calculatedHeight))
                NotifyAreaUserPosition(event)
                EventNumbers(event)
                WWWEventSocialNetworks(event)
            }
        }
    }

    @Composable
    private fun BoxScope.SimulationButton(
        scope: CoroutineScope,
        event: IWWWEvent,
        context: Context
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp)
                .offset(y = (-8).dp)
                .size(48.dp)
                .clip(CircleShape)
                .background(onPrimaryLight)
                .clickable {
                    scope.launch {
                        // Generate random position within event area
                        val position = event.area.generateRandomPositionInArea()

                        // Calculate time 5 minutes before event start
                        val simulationDelay = 0.minutes // Start NOW
                        val simulationTime = event.getStartDateTime() + simulationDelay

                        // Reset any existing simulation
                        platform.disableSimulation()

                        // Create new simulation with the calculated time and position
                        val simulation = WWWSimulation(
                            startDateTime = simulationTime,
                            userPosition = position,
                            initialSpeed = 50 // Use current default speed
                        )

                        // Set the simulation
                        Log.i("Simulation", "Setting simulation starting time to $simulationTime from event ${event.id}")
                        Log.i("Simulation", "Setting simulation user position to $position from event ${event.id}")
                        platform.setSimulation(simulation)

                        // Restart event observation to apply simulation (observation delay changes)
                        event.observer.stopObservation()
                        event.observer.startObservation()

                        // Show feedback
                        Toast.makeText(
                            context,
                            stringResource(MokoRes.strings.test_simulation_started),
                            Toast.LENGTH_SHORT
                        ).show()

                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Timer,
                contentDescription = stringResource(MokoRes.strings.test_simulation),
                tint = Color.Red,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

// ----------------------------------------------------------------------------

@Composable
private fun EventDescription(event: IWWWEvent, modifier: Modifier = Modifier) {
    val dir = LocalLayoutDirection.current
    Text(
        modifier = modifier.padding(horizontal = DIM_DEFAULT_EXT_PADDING.dp),
        text = stringResource(event.getDescription()),
        style = extraQuinaryColoredBoldTextStyle(),
        fontSize = DIM_EVENT_DESC_FONTSIZE.sp,
        textAlign = if (dir == LayoutDirection.Rtl) TextAlign.Start else TextAlign.Justify
    )
}

// ----------------------------------------------------------------------------

@Composable
private fun EventOverlay(event: IWWWEvent) {
    val eventStatus by event.observer.eventStatus.collectAsState(Status.UNDEFINED)

    Box {
        Image(
            modifier = Modifier.fillMaxWidth(),
            contentScale = ContentScale.FillWidth,
            painter = painterResource(event.getLocationImage() as DrawableResource),
            contentDescription = stringResource(event.getLocation())
        )
        Box(modifier = Modifier.matchParentSize()) {
            EventOverlaySoonOrRunning(eventStatus)
            EventOverlayDate(eventStatus, event.getLiteralStartDateSimple())
            EventOverlayDone(eventStatus)
        }
    }
}

// ----------------------------------------------------------------------------

@Composable
private fun EventOverlayDate(eventStatus: Status, eventDate: String, modifier: Modifier = Modifier) {

    Box(
        modifier = modifier
            .fillMaxSize()
            .let { if (eventStatus == Status.DONE) it.padding(bottom = DIM_DEFAULT_EXT_PADDING.dp) else it },
        contentAlignment = if (eventStatus == Status.DONE) Alignment.BottomCenter else Alignment.Center
    ) {
        val textStyle = extraBoldTextStyle(DIM_EVENT_DATE_FONTSIZE)
        Text(
            text = eventDate,
            style = textStyle.copy(color = quinaryLight)
        )
        Text(
            text = eventDate,
            style = textStyle.copy(
                color = MaterialTheme.colorScheme.primary,
                drawStyle = Stroke(
                    miter = DIM_EVENT_DATE_MITER,
                    width = DIM_EVENT_DATE_STROKE,
                    join = StrokeJoin.Miter
                )
            )
        )
    }
}

// ----------------------------------------------------------------------------

@Composable
private fun WWWEventSocialNetworks(event: IWWWEvent, modifier: Modifier = Modifier) {
    WWWSocialNetworks(
        modifier = modifier,
        instagramAccount = event.instagramAccount,
        instagramHashtag = event.instagramHashtag
    )
}

// ----------------------------------------------------------------------------

@OptIn(ExperimentalTime::class)
@Composable
private fun NotifyAreaUserPosition(event: IWWWEvent, modifier: Modifier = Modifier) {
    val isInArea by event.observer.userIsInArea.collectAsState()
    val hitDateTime by event.observer.hitDateTime.collectAsState()

    val geolocText = if (isInArea) {
        val time = hitDateTime.toLocalDateTime(event.getTZ()).time
        "${stringResource(MokoRes.strings.geoloc_yourein)} " +
                "(${time.hour.toString().padStart(2, '0')}" +
                ":${time.minute.toString().padStart(2, '0')})"
    } else {
        stringResource(MokoRes.strings.geoloc_yourenotin)
    }
    
    val displayText = BidiFormatter.getInstance().unicodeWrap(geolocText)

    Row(
        modifier = modifier
            .height(DIM_EVENT_GEOLOCME_HEIGHT.dp)
            .padding(start = DIM_DEFAULT_EXT_PADDING.dp, end = DIM_DEFAULT_EXT_PADDING.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .border(DIM_EVENT_GEOLOCME_BORDER.dp, MaterialTheme.colorScheme.primary)
                .fillMaxHeight()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = displayText,
                style = quinaryColoredTextStyle(DIM_EVENT_GEOLOCME_FONTSIZE)
            )
        }
    }
}

// ----------------------------------------------------------------------------

@Composable
private fun EventNumbers(event: IWWWEvent, modifier: Modifier = Modifier) {
    var waveNumbers by remember { mutableStateOf<WaveNumbersLiterals?>(null) }
    val progression by event.observer.progression.collectAsState()
    val startWarmingInProgress by event.observer.isStartWarmingInProgress.collectAsState()
    val warmingText = stringResource(MokoRes.strings.wave_warming)

    LaunchedEffect(event.id) {
        waveNumbers = event.getAllNumbers()
    }

    val eventNumbers by remember(waveNumbers) {
        derivedStateOf {
            waveNumbers?.let {
                mapOf(
                    MokoRes.strings.wave_start_time to it.waveStartTime,
                    MokoRes.strings.wave_end_time to it.waveEndTime,
                    MokoRes.strings.wave_speed to it.waveSpeed,
                    MokoRes.strings.wave_total_time to it.waveTotalTime,
                    MokoRes.strings.wave_progression to if (startWarmingInProgress)
                        warmingText
                    else
                        event.wave.getLiteralFromProgression(progression)
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
        MokoRes.strings.wave_progression
    )

    Box(modifier = modifier.padding(start = DIM_DEFAULT_EXT_PADDING.dp, end = DIM_DEFAULT_EXT_PADDING.dp)) {
        Box(
            modifier = Modifier
                .border(
                    width = DIM_EVENT_NUMBERS_BORDERWIDTH.dp,
                    color = quinaryLight,
                    shape = RoundedCornerShape(
                        topStart = DIM_EVENT_NUMBERS_BORDERROUND.dp,
                        bottomEnd = DIM_EVENT_NUMBERS_BORDERROUND.dp
                    )
                )
                .padding(DIM_DEFAULT_EXT_PADDING.dp)
        ) {
            Column(modifier = Modifier.padding(start = DIM_DEFAULT_INT_PADDING.dp, end = DIM_DEFAULT_INT_PADDING.dp)) {
                Text(
                    text = stringResource(MokoRes.strings.be_waved),
                    modifier = Modifier.fillMaxWidth(),
                    style = extraQuinaryColoredBoldTextStyle(DIM_EVENT_NUMBERS_TITLE_FONTSIZE).copy(
                        textAlign = TextAlign.End
                    )
                )
                Spacer(modifier = Modifier.height(DIM_EVENT_NUMBERS_SPACER.dp))
                if (eventNumbers.isNotEmpty()) {
                    orderedLabels.forEach { key ->
                        val value = eventNumbers[key]!!

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Label
                            Text(
                                text = stringResource(key),
                                style = extraQuinaryColoredBoldTextStyle(
                                    DIM_EVENT_NUMBERS_LABEL_FONTSIZE
                                )
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Value
                                Text(
                                    text = value,
                                    style = extraBoldTextStyle(DIM_EVENT_NUMBERS_VALUE_FONTSIZE).copy(
                                        color = when (key) {
                                            MokoRes.strings.wave_progression -> MaterialTheme.colorScheme.secondary
                                            MokoRes.strings.wave_start_time -> Color.Yellow
                                            else -> MaterialTheme.colorScheme.primary
                                        }
                                    )
                                )
                                // optional Timezone
                                if (key in listOf(
                                        MokoRes.strings.wave_start_time,
                                        MokoRes.strings.wave_end_time
                                    )
                                ) {
                                    Text(
                                        text = " $eventTimeZone",
                                        style = extraLightTextStyle(DIM_EVENT_NUMBERS_TZ_FONTSIZE).copy(
                                            color = when (key) {
                                                MokoRes.strings.wave_start_time -> Color.Yellow
                                                else -> MaterialTheme.colorScheme.primary
                                            }
                                        )
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(DIM_EVENT_NUMBERS_SPACER.dp / 2))
                    }
                }
            }
        }
    }
}
