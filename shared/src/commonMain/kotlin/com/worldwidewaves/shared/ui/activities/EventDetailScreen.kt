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
import com.worldwidewaves.shared.ui.components.EventLayout
import com.worldwidewaves.shared.ui.components.MapPolygonDisplay
import com.worldwidewaves.shared.ui.formatters.calculateEventMapHeight
import com.worldwidewaves.shared.utils.Log
import com.worldwidewaves.shared.viewmodels.EventsViewModel
import com.worldwidewaves.shared.viewmodels.MapViewModel
import org.koin.core.component.inject
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class EventDetailScreen(
    eventId: String,
    platformEnabler: PlatformEnabler,
    val mapViewModel: MapViewModel,
    showSplash: Boolean = false,
) : BaseWaveActivityScreen(eventId, platformEnabler, showSplash) {
    private val eventsViewModel: EventsViewModel by inject()

    override fun onFavoriteChanged() {
        eventsViewModel.refreshEvents()
    }

    @Composable
    override fun Event(
        event: IWWWEvent,
        modifier: Modifier,
    ) {
        // Map availability for simulation button
        val mapFeatureState by mapViewModel.featureState.collectAsState()
        var showMapRequiredDialog by remember { mutableStateOf(false) }

        // Map availability check - ensures state initialized before map renders
        // Check when state is NotChecked, NotAvailable, Failed, or Available
        // Re-check Available state to handle external uninstalls (e.g., from EventsListScreen)
        // Don't re-check during active operations (Downloading, Installing, Pending)
        LaunchedEffect(event.id, mapFeatureState) {
            val shouldCheck =
                when (mapFeatureState) {
                    is com.worldwidewaves.shared.map.MapFeatureState.NotChecked -> true
                    is com.worldwidewaves.shared.map.MapFeatureState.NotAvailable -> true
                    is com.worldwidewaves.shared.map.MapFeatureState.Failed -> true
                    is com.worldwidewaves.shared.map.MapFeatureState.Available -> true // Re-check to validate
                    else -> false // Don't re-check during Downloading, Installing, Pending, etc.
                }

            Log.d(
                "EventDetailScreen",
                "Map state check: event=${event.id}, state=$mapFeatureState, shouldCheck=$shouldCheck",
            )

            if (shouldCheck) {
                mapViewModel.checkIfMapIsAvailable(event.id, autoDownload = false)
            }
        }

        // Calculate responsive map height
        val calculatedHeight = calculateEventMapHeight()

        // Display wave polygons on map (like WaveScreen) but WITHOUT auto-follow camera
        // This allows users to see wave progression visually while keeping camera static on event bounds
        MapPolygonDisplay(event, eventMap)

        // Use simplified shared standard event layout
        EventLayout(
            event = event,
            mapFeatureState = mapFeatureState,
            onNavigateToWave = { eventId -> platformEnabler.openWaveActivity(eventId) },
            onSimulationStarted = { message -> platformEnabler.toast(message) },
            onSimulationStopped = { message -> platformEnabler.toast(message) },
            onSimulationError = { title, message -> platformEnabler.toast("$title: $message") },
            onMapNotAvailable = { showMapRequiredDialog = true },
            onUrlOpen = { url -> platformEnabler.openUrl(url) },
            modifier = modifier,
            mapHeight = calculatedHeight,
            mapArea = {
                eventMap?.Draw(
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
