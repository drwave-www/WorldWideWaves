package com.worldwidewaves.activities.event

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.play.core.splitcompat.SplitCompat
import com.worldwidewaves.R
import com.worldwidewaves.compose.common.AutoResizeSingleLineText
import com.worldwidewaves.compose.common.ButtonWave
import com.worldwidewaves.compose.common.DividerLine
import com.worldwidewaves.compose.common.EventOverlayDone
import com.worldwidewaves.compose.common.EventOverlaySoonOrRunning
import com.worldwidewaves.compose.common.WWWSocialNetworks
import com.worldwidewaves.compose.map.AndroidEventMap
import com.worldwidewaves.shared.MokoRes
import com.worldwidewaves.shared.WWWGlobals.Companion.Dimensions
import com.worldwidewaves.shared.WWWGlobals.Companion.Event
import com.worldwidewaves.shared.WWWGlobals.Companion.Wave
import com.worldwidewaves.shared.WWWPlatform
import com.worldwidewaves.shared.WWWSimulation
import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.events.IWWWEvent.Status
import com.worldwidewaves.shared.events.IWWWEvent.WaveNumbersLiterals
import com.worldwidewaves.shared.events.utils.IClock
import com.worldwidewaves.shared.events.utils.Log
import com.worldwidewaves.shared.format.DateTimeFormats
import com.worldwidewaves.theme.commonTextStyle
import com.worldwidewaves.theme.extraBoldTextStyle
import com.worldwidewaves.theme.extraLightTextStyle
import com.worldwidewaves.theme.extraQuinaryColoredBoldTextStyle
import com.worldwidewaves.theme.onPrimaryLight
import com.worldwidewaves.theme.quinaryColoredTextStyle
import com.worldwidewaves.theme.quinaryLight
import com.worldwidewaves.theme.scrimLight
import com.worldwidewaves.viewmodels.MapFeatureState
import com.worldwidewaves.viewmodels.MapViewModel
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.koin.android.ext.android.inject
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import androidx.compose.ui.res.painterResource as painterResourceAndroid

@OptIn(ExperimentalTime::class)
class EventActivity : AbstractEventWaveActivity() {
    private val clock: IClock by inject()
    private val platform: WWWPlatform by inject()

    // ------------------------------------------------------------------------

    @Composable
    override fun Screen(
        modifier: Modifier,
        event: IWWWEvent,
    ) {
        val context = LocalContext.current
        // Ensure dynamic-feature splits are available immediately
        SplitCompat.install(context)
        val scope = rememberCoroutineScope()
        val eventStatus by event.observer.eventStatus.collectAsState(Status.UNDEFINED)
        val endDateTime = remember { mutableStateOf<Instant?>(null) }
        val progression by event.observer.progression.collectAsState()
        val isInArea by event.observer.userIsInArea.collectAsState()
        val isSimulationModeEnabled by platform.simulationModeEnabled.collectAsState()

        // Recompute end date-time each time progression changes (after polygons load, duration becomes accurate)
        LaunchedEffect(event.id, progression) {
            endDateTime.value = event.getEndDateTime()
        }

        // Calculate height based on aspect ratio and available width
        val windowInfo = LocalWindowInfo.current
        val density = LocalDensity.current
        val screenWidthDp = with(density) { windowInfo.containerSize.width.toDp() }
        val calculatedHeight = screenWidthDp / Event.MAP_RATIO

        // Construct the event map
        val eventMap =
            remember(event.id) {
                AndroidEventMap(
                    event,
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

        // Screen composition
        Box {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(30.dp),
            ) {
                EventOverlay(event)
                EventDescription(event)
                DividerLine()

                Box(modifier = Modifier.fillMaxWidth()) {
                    ButtonWave(
                        event.id,
                        eventStatus,
                        endDateTime.value,
                        clock,
                        isInArea,
                        modifier = Modifier.align(Alignment.Center),
                    )

                    // Launch simulation button
                    if (isSimulationModeEnabled) {
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
        context: Context,
    ) {
        // Simulation button states: idle, loading, active
        var simulationButtonState by remember { mutableStateOf("idle") }
        val isSimulationEnabled by platform.simulationModeEnabled.collectAsState()

        // Map availability check
        val mapViewModel: MapViewModel = viewModel()
        val mapFeatureState by mapViewModel.featureState.collectAsState()
        var showMapRequiredDialog by remember { mutableStateOf(false) }

        // Check if map is available for simulation
        val isMapAvailableForSimulation =
            when (mapFeatureState) {
                is MapFeatureState.Installed -> true
                is MapFeatureState.Available -> true
                else -> false
            }

        // Check map availability when component initializes
        LaunchedEffect(Unit) {
            mapViewModel.checkIfMapIsAvailable(event.id, autoDownload = false)
        }

        Box(
            modifier =
                Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp)
                    .offset(y = (-8).dp)
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(onPrimaryLight)
                    .clickable(enabled = simulationButtonState != "loading") {
                        if (simulationButtonState == "idle") {
                            if (!isMapAvailableForSimulation) {
                                showMapRequiredDialog = true
                                return@clickable
                            }
                            simulationButtonState = "loading"
                            scope.launch {
                                try {
                                    // Generate random position within event area
                                    val position = event.area.generateRandomPositionInArea()

                                    // Calculate time 5 minutes before event start
                                    val simulationDelay = 0.minutes // Start NOW
                                    val simulationTime = event.getStartDateTime() + simulationDelay

                                    // Reset any existing simulation
                                    platform.disableSimulation()

                                    // Create new simulation with the calculated time and position
                                    val simulation =
                                        WWWSimulation(
                                            startDateTime = simulationTime,
                                            userPosition = position,
                                            initialSpeed = Wave.DEFAULT_SPEED_SIMULATION, // Use current default speed
                                        )

                                    // Set the simulation
                                    Log.i("Simulation", "Setting simulation starting time to $simulationTime from event ${event.id}")
                                    Log.i("Simulation", "Setting simulation user position to $position from event ${event.id}")
                                    platform.setSimulation(simulation)

                                    // Restart event observation to apply simulation (observation delay changes)
                                    event.observer.stopObservation()
                                    event.observer.startObservation()

                                    // Show feedback
                                    Toast
                                        .makeText(
                                            context,
                                            context.getString(MokoRes.strings.test_simulation_started.resourceId),
                                            Toast.LENGTH_SHORT,
                                        ).show()

                                    simulationButtonState = "active"
                                } catch (ise: IllegalStateException) {
                                    simulationButtonState = "idle"
                                    Log.e("Simulation", "Invalid state for simulation setup", ise)
                                } catch (iae: IllegalArgumentException) {
                                    simulationButtonState = "idle"
                                    Log.e("Simulation", "Invalid simulation parameters", iae)
                                } catch (uoe: UnsupportedOperationException) {
                                    simulationButtonState = "idle"
                                    Log.e("Simulation", "Unsupported simulation operation", uoe)
                                }
                            }
                        } else if (simulationButtonState == "active") {
                            // Stop simulation
                            simulationButtonState = "idle"
                            scope.launch {
                                platform.disableSimulation()
                                event.observer.stopObservation()
                                event.observer.startObservation()
                                Toast
                                    .makeText(
                                        context,
                                        "Simulation stopped",
                                        Toast.LENGTH_SHORT,
                                    ).show()
                            }
                        }
                    },
            contentAlignment = Alignment.Center,
        ) {
            when (simulationButtonState) {
                "idle" -> {
                    Icon(
                        painter = painterResourceAndroid(R.drawable.ic_timer),
                        contentDescription = stringResource(MokoRes.strings.test_simulation),
                        tint = Color.Red,
                        modifier = Modifier.size(24.dp),
                    )
                }
                "loading" -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = Color.Red,
                    )
                }
                "active" -> {
                    Icon(
                        painter = painterResourceAndroid(R.drawable.ic_stop),
                        contentDescription = "Stop simulation",
                        tint = Color.Red,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }

        // Reset button state when simulation is disabled externally
        LaunchedEffect(isSimulationEnabled) {
            if (!isSimulationEnabled && simulationButtonState == "active") {
                simulationButtonState = "idle"
            }
        }

        // Show map required dialog
        if (showMapRequiredDialog) {
            AlertMapNotDownloadedOnSimulationLaunch { showMapRequiredDialog = false }
        }
    }
}

@Composable
private fun AlertMapNotDownloadedOnSimulationLaunch(hideDialog: () -> Unit) {
    AlertDialog(
        onDismissRequest = hideDialog,
        title = {
            Text(
                text = stringResource(MokoRes.strings.simulation_map_required_title),
                style = commonTextStyle().copy(color = scrimLight),
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Text(
                text = stringResource(MokoRes.strings.simulation_map_required_message),
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            Button(
                onClick = hideDialog,
            ) {
                Text(stringResource(MokoRes.strings.ok))
            }
        },
    )
}

// ----------------------------------------------------------------------------

@Composable
private fun EventDescription(
    event: IWWWEvent,
    modifier: Modifier = Modifier,
) {
    val dir = LocalLayoutDirection.current
    Text(
        modifier = modifier.padding(horizontal = Dimensions.DEFAULT_EXT_PADDING.dp),
        text = stringResource(event.getDescription()),
        style = extraQuinaryColoredBoldTextStyle(),
        fontSize = Event.DESC_FONTSIZE.sp,
        textAlign = if (dir == LayoutDirection.Rtl) TextAlign.Start else TextAlign.Justify,
    )
}

// ----------------------------------------------------------------------------

@OptIn(ExperimentalTime::class)
@Composable
private fun EventOverlay(event: IWWWEvent) {
    val eventStatus by event.observer.eventStatus.collectAsState(Status.UNDEFINED)

    val localizedDate =
        remember(event.id) {
            DateTimeFormats.dayMonth(event.getStartDateTime(), event.getTZ())
        }

    Box {
        Image(
            modifier = Modifier.fillMaxWidth(),
            contentScale = ContentScale.FillWidth,
            painter = painterResource(event.getLocationImage() as DrawableResource),
            contentDescription = stringResource(event.getLocation()),
        )
        Box(modifier = Modifier.matchParentSize()) {
            EventOverlaySoonOrRunning(eventStatus)
            EventOverlayDone(eventStatus)
            EventOverlayDate(eventStatus, localizedDate)
        }
    }
}

// ----------------------------------------------------------------------------

@Composable
private fun EventOverlayDate(
    eventStatus: Status,
    eventDate: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .let { if (eventStatus == Status.DONE) it.padding(bottom = Dimensions.DEFAULT_EXT_PADDING.dp) else it },
        contentAlignment = if (eventStatus == Status.DONE) Alignment.BottomCenter else Alignment.Center,
    ) {
        val textStyle = extraBoldTextStyle(Event.DATE_FONTSIZE)
        Text(
            text = eventDate,
            style = textStyle.copy(color = quinaryLight),
        )
        Text(
            text = eventDate,
            style =
                textStyle.copy(
                    color = MaterialTheme.colorScheme.primary,
                    drawStyle =
                        Stroke(
                            miter = Event.DATE_MITER,
                            width = Event.DATE_STROKE,
                            join = StrokeJoin.Miter,
                        ),
                ),
        )
    }
}

// ----------------------------------------------------------------------------

@Composable
private fun WWWEventSocialNetworks(
    event: IWWWEvent,
    modifier: Modifier = Modifier,
) {
    WWWSocialNetworks(
        modifier = modifier,
        instagramAccount = event.instagramAccount,
        instagramHashtag = event.instagramHashtag,
    )
}

// ----------------------------------------------------------------------------

@OptIn(ExperimentalTime::class)
@Composable
private fun NotifyAreaUserPosition(
    event: IWWWEvent,
    modifier: Modifier = Modifier,
) {
    val isInArea by event.observer.userIsInArea.collectAsState()
    val hitDateTime by event.observer.hitDateTime.collectAsState()

    val formattedTime = IClock.instantToLiteral(hitDateTime, event.getTZ())

    val geolocText =
        if (isInArea) {
            stringResource(MokoRes.strings.geoloc_yourein_at, formattedTime)
        } else {
            stringResource(MokoRes.strings.geoloc_yourenotin)
        }

    val displayText = BidiFormatter.getInstance().unicodeWrap(geolocText)

    Row(
        modifier =
            modifier
                .height(Event.GEOLOCME_HEIGHT.dp)
                .padding(start = Dimensions.DEFAULT_EXT_PADDING.dp, end = Dimensions.DEFAULT_EXT_PADDING.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .border(Event.GEOLOCME_BORDER.dp, MaterialTheme.colorScheme.primary)
                    .fillMaxHeight()
                    .weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = displayText,
                style = quinaryColoredTextStyle(Event.GEOLOCME_FONTSIZE),
            )
        }
    }
}

// ----------------------------------------------------------------------------

@OptIn(ExperimentalTime::class)
@Composable
private fun EventNumbers(
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

    // Initial load – compute static numbers & start time (doesn't depend on polygons)
    LaunchedEffect(event.id) {
        waveNumbers = event.getAllNumbers()

        val start = event.getStartDateTime()
        startTimeText = DateTimeFormats.timeShort(start, event.getTZ())
    }

    // Recompute values that depend on polygons/duration each time progression updates
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
    val orderedLabels =
        listOf(
            MokoRes.strings.wave_start_time,
            MokoRes.strings.wave_end_time,
            MokoRes.strings.wave_speed,
            MokoRes.strings.wave_total_time,
            MokoRes.strings.wave_progression,
        )

    Box(modifier = modifier.padding(start = Dimensions.DEFAULT_EXT_PADDING.dp, end = Dimensions.DEFAULT_EXT_PADDING.dp)) {
        Box(
            modifier =
                Modifier
                    .border(
                        width = Event.NUMBERS_BORDERWIDTH.dp,
                        color = quinaryLight,
                        shape =
                            RoundedCornerShape(
                                topStart = Event.NUMBERS_BORDERROUND.dp,
                                bottomEnd = Event.NUMBERS_BORDERROUND.dp,
                            ),
                    ).padding(Dimensions.DEFAULT_EXT_PADDING.dp),
        ) {
            Column(modifier = Modifier.padding(start = Dimensions.DEFAULT_INT_PADDING.dp, end = Dimensions.DEFAULT_INT_PADDING.dp)) {
                AutoResizeSingleLineText(
                    text = stringResource(MokoRes.strings.be_waved),
                    modifier = Modifier.fillMaxWidth(),
                    style =
                        extraQuinaryColoredBoldTextStyle(Event.NUMBERS_TITLE_FONTSIZE)
                            .copy(textAlign = TextAlign.End),
                    textAlign = TextAlign.End,
                )
                Spacer(modifier = Modifier.height(Event.NUMBERS_SPACER.dp))
                if (eventNumbers.isNotEmpty()) {
                    orderedLabels.forEach { key ->
                        val value = eventNumbers[key]!!
                        val displayValue =
                            if (key == MokoRes.strings.wave_total_time) {
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
                            // Label
                            Text(
                                text = stringResource(key),
                                style =
                                    extraQuinaryColoredBoldTextStyle(
                                        Event.NUMBERS_LABEL_FONTSIZE,
                                    ),
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Value
                                Text(
                                    text = displayValue,
                                    style =
                                        extraBoldTextStyle(Event.NUMBERS_VALUE_FONTSIZE).copy(
                                            color =
                                                when (key) {
                                                    MokoRes.strings.wave_progression -> MaterialTheme.colorScheme.secondary
                                                    MokoRes.strings.wave_start_time -> Color.Yellow
                                                    else -> MaterialTheme.colorScheme.primary
                                                },
                                        ),
                                )
                                // optional Timezone
                                if (key in
                                    listOf(
                                        MokoRes.strings.wave_start_time,
                                        MokoRes.strings.wave_end_time,
                                    )
                                ) {
                                    Text(
                                        text = " $eventTimeZone",
                                        style =
                                            extraLightTextStyle(Event.NUMBERS_TZ_FONTSIZE).copy(
                                                color =
                                                    when (key) {
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

// Formats a duration in minutes into a human-readable string (e\.g\., "1 hour 5 minutes")\.
// If totalMinutes is null, returns the original value string\.
@Composable
private fun formatDurationMinutes(
    totalMinutes: Long?,
    defaultValue: String,
): String =
    totalMinutes?.let { mins ->
        val hours = (mins / 60).toInt()
        val minutesLeft = (mins % 60).toInt()
        val parts =
            buildList {
                if (hours > 0) {
                    add(
                        if (hours == 1) {
                            stringResource(MokoRes.strings.hour_singular, hours)
                        } else {
                            stringResource(MokoRes.strings.hour_plural, hours)
                        },
                    )
                }
                if (minutesLeft > 0) {
                    add(
                        if (minutesLeft == 1) {
                            stringResource(MokoRes.strings.minute_singular, minutesLeft)
                        } else {
                            stringResource(MokoRes.strings.minute_plural, minutesLeft)
                        },
                    )
                }
            }
        if (parts.isNotEmpty()) parts.joinToString(" ") else defaultValue
    } ?: defaultValue
