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

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.play.core.splitcompat.SplitCompat
import com.worldwidewaves.compose.map.AndroidEventMap
import com.worldwidewaves.shared.WWWPlatform
import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.events.utils.IClock
import com.worldwidewaves.shared.ui.components.AlertMapNotDownloadedOnSimulationLaunch
import com.worldwidewaves.shared.ui.components.StandardEventLayout
import com.worldwidewaves.shared.ui.utils.calculateEventMapHeight
import com.worldwidewaves.shared.ui.utils.rememberEventState
import com.worldwidewaves.shared.map.MapFeatureState
import com.worldwidewaves.viewmodels.MapViewModel
import org.koin.android.ext.android.inject
import kotlin.time.ExperimentalTime

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

        // Map availability for simulation button
        val mapViewModel: MapViewModel = viewModel()
        val mapFeatureState by mapViewModel.featureState.collectAsState()
        var showMapRequiredDialog by remember { mutableStateOf(false) }

        // Check map availability for simulation
        LaunchedEffect(Unit) {
            mapViewModel.checkIfMapIsAvailable(event.id, autoDownload = false)
        }

        // Calculate responsive map height
        val calculatedHeight = calculateEventMapHeight()

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

        // Use simplified shared standard event layout
        StandardEventLayout(
            event = event,
            platform = platform,
            clock = clock,
            mapFeatureState = mapFeatureState,
            onNavigateToWave = { eventId ->
                context.startActivity(
                    Intent(context, WaveActivity::class.java).apply {
                        putExtra("eventId", eventId)
                    }
                )
            },
            onSimulationStarted = { message ->
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            },
            onSimulationStopped = { message ->
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            },
            onSimulationError = { title, message ->
                Toast.makeText(context, "$title: $message", Toast.LENGTH_SHORT).show()
            },
            onMapNotAvailable = { showMapRequiredDialog = true },
            modifier = modifier,
            mapHeight = calculatedHeight,
            mapArea = {
                eventMap.Screen(modifier = Modifier.fillMaxWidth().height(calculatedHeight))
            }
        )

        // Show map required dialog for simulation
        if (showMapRequiredDialog) {
            AlertMapNotDownloadedOnSimulationLaunch { showMapRequiredDialog = false }
        }
    }

}
