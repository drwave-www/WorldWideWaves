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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.worldwidewaves.compose.map.AndroidEventMap
import com.worldwidewaves.shared.MokoRes
import com.worldwidewaves.shared.WWWGlobals.Dimensions
import com.worldwidewaves.shared.WWWGlobals.Event
import com.worldwidewaves.shared.WWWGlobals.Wave
import com.worldwidewaves.shared.WWWPlatform
import com.worldwidewaves.shared.WWWSimulation
import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.events.IWWWEvent.Status
import com.worldwidewaves.shared.events.utils.IClock
import com.worldwidewaves.shared.format.DateTimeFormats
import com.worldwidewaves.shared.ui.components.ButtonWave
import com.worldwidewaves.shared.ui.components.DividerLine
import com.worldwidewaves.shared.ui.components.SharedEventOverlay
import com.worldwidewaves.shared.ui.components.SharedSimulationButton
import com.worldwidewaves.shared.ui.components.SharedEventDescription
import com.worldwidewaves.shared.ui.components.SharedEventNumbers
import com.worldwidewaves.shared.ui.components.SharedNotifyAreaUserPosition
import com.worldwidewaves.shared.ui.components.SharedWWWEventSocialNetworks
import com.worldwidewaves.shared.ui.components.SharedAlertMapNotDownloadedOnSimulationLaunch
import com.worldwidewaves.shared.ui.components.WWWSocialNetworks
import com.worldwidewaves.shared.ui.components.WaveNavigator
import com.worldwidewaves.shared.utils.Log
import com.worldwidewaves.theme.onPrimaryLight
import com.worldwidewaves.shared.map.MapFeatureState
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

    companion object {
        private const val TAG = "EventActivity"
    }

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

        // Map availability for simulation button
        val mapViewModel: MapViewModel = viewModel()
        val mapFeatureState by mapViewModel.featureState.collectAsState()
        var showMapRequiredDialog by remember { mutableStateOf(false) }

        // Check map availability for simulation
        LaunchedEffect(Unit) {
            mapViewModel.checkIfMapIsAvailable(event.id, autoDownload = false)
        }

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
                SharedEventOverlay(event)
                SharedEventDescription(event)
                DividerLine()

                Box(modifier = Modifier.fillMaxWidth()) {
                    ButtonWave(
                        event.id,
                        eventStatus,
                        endDateTime.value,
                        clock,
                        isInArea,
                        onNavigateToWave = WaveNavigator { eventId ->
                            context.startActivity(
                                Intent(context, WaveActivity::class.java).apply {
                                    putExtra("eventId", eventId)
                                }
                            )
                        },
                        modifier = Modifier.align(Alignment.Center),
                    )

                    // Launch simulation button
                    if (isSimulationModeEnabled) {
                        SharedSimulationButton(
                            event = event,
                            platform = platform,
                            mapFeatureState = mapFeatureState,
                            onMapNotAvailable = { showMapRequiredDialog = true },
                            onSimulationStarted = { message ->
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                            },
                            onSimulationStopped = { message ->
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                            },
                            onError = { title, message ->
                                Toast.makeText(context, "$title: $message", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }

                eventMap.Screen(modifier = Modifier.fillMaxWidth().height(calculatedHeight))
                SharedNotifyAreaUserPosition(event)
                SharedEventNumbers(event)
                SharedWWWEventSocialNetworks(event)
            }

            // Show map required dialog for simulation
            if (showMapRequiredDialog) {
                SharedAlertMapNotDownloadedOnSimulationLaunch { showMapRequiredDialog = false }
            }
        }
    }

}





