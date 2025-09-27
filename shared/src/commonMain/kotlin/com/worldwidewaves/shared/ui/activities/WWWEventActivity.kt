package com.worldwidewaves.shared.ui.activities

/* * Copyright 2025 DrWave
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
 * limitations under the License. */

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
import com.worldwidewaves.shared.PlatformEnabler
import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.ui.components.AlertMapNotDownloadedOnSimulationLaunch
import com.worldwidewaves.shared.ui.components.StandardEventLayout
import com.worldwidewaves.shared.ui.utils.calculateEventMapHeight
import com.worldwidewaves.shared.viewmodels.MapViewModel
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class WWWEventActivity(
    eventId: String,
    platformEnabler: PlatformEnabler,
    val mapViewModel: MapViewModel,
    showSplash: Boolean = false,
) : WWWAbstractEventWaveActivity(eventId, platformEnabler, showSplash) {
    @Composable
    override fun Event(
        event: IWWWEvent,
        modifier: Modifier,
    ) {
        // Map availability for simulation button
        val mapFeatureState by mapViewModel.featureState.collectAsState()
        var showMapRequiredDialog by remember { mutableStateOf(false) }

        // Check map availability for simulation
        LaunchedEffect(Unit) {
            mapViewModel.checkIfMapIsAvailable(event.id, autoDownload = false)
        }

        // Calculate responsive map height
        val calculatedHeight = calculateEventMapHeight()

        // Use simplified shared standard event layout
        StandardEventLayout(
            event = event,
            mapFeatureState = mapFeatureState,
            onNavigateToWave = { eventId -> platformEnabler.openWaveActivity(eventId) },
            onSimulationStarted = { message -> platformEnabler.toast(message) },
            onSimulationStopped = { message -> platformEnabler.toast(message) },
            onSimulationError = { title, message -> platformEnabler.toast("$title: $message") },
            onMapNotAvailable = { showMapRequiredDialog = true },
            modifier = modifier,
            mapHeight = calculatedHeight,
            mapArea = {
                eventMap?.Screen(
                    autoMapDownload = false,
                    modifier = Modifier.fillMaxWidth().height(calculatedHeight),
                )
            },
        )

        // Show map required dialog for simulation
        if (showMapRequiredDialog) {
            AlertMapNotDownloadedOnSimulationLaunch { showMapRequiredDialog = false }
        }
    }
}
