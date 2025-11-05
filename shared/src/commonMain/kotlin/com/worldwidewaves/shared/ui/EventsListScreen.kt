package com.worldwidewaves.shared.ui

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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.worldwidewaves.shared.PlatformEnabler
import com.worldwidewaves.shared.data.SetEventFavorite
import com.worldwidewaves.shared.domain.usecases.MapAvailabilityChecker
import com.worldwidewaves.shared.ui.components.AlertMapUninstall
import com.worldwidewaves.shared.ui.screens.EventsScreen
import com.worldwidewaves.shared.utils.Log
import com.worldwidewaves.shared.viewmodels.EventsViewModel
import org.koin.core.component.KoinComponent

/**
 * Android wrapper for SharedEventsListScreen.
 * Handles Android-specific navigation and lifecycle management while
 * delegating all UI to the shared component for perfect parity.
 */
class EventsListScreen(
    private val viewModel: EventsViewModel,
    private val mapChecker: MapAvailabilityChecker,
    private val setEventFavorite: SetEventFavorite,
) : TabScreen,
    KoinComponent {
    override val name = "Events"

    @Composable
    override fun Screen(
        platformEnabler: PlatformEnabler,
        modifier: Modifier,
    ) {
        val events by viewModel.events.collectAsState()
        val mapStates by mapChecker.mapStates.collectAsState()
        val viewModelRefreshTrigger by viewModel.refreshTrigger.collectAsState()

        // Dialog state for map uninstall confirmation
        var showUninstallDialog by remember { mutableStateOf(false) }
        var pendingUninstallEventId by remember { mutableStateOf<String?>(null) }

        // Initialize EventsViewModel (iOS-safe pattern, Android compatible)
        LaunchedEffect(Unit) {
            viewModel.loadEvents()
        }

        // Refresh map availability when screen loads (KMM-compatible approach)
        LaunchedEffect(Unit) {
            mapChecker.refreshAvailability()
        }

        // Pre-track all event IDs
        LaunchedEffect(events) {
            mapChecker.trackMaps(events.map { it.id })
            mapChecker.refreshAvailability()
        }

        // Map uninstall request callback
        val onMapUninstallRequested: (String) -> Unit = { eventId ->
            pendingUninstallEventId = eventId
            showUninstallDialog = true
        }

        // Handle uninstall confirmation
        LaunchedEffect(showUninstallDialog) {
            if (!showUninstallDialog && pendingUninstallEventId != null) {
                val eventId = pendingUninstallEventId!!
                Log.i("EventsListScreen", "Uninstalling map for event: $eventId")
                val success = mapChecker.requestMapUninstall(eventId)
                if (success) {
                    Log.i("EventsListScreen", "Map uninstall successful for: $eventId")
                    mapChecker.refreshAvailability() // Ensure state is refreshed
                } else {
                    Log.e("EventsListScreen", "Failed to uninstall map for: $eventId")
                    // TODO: Show error feedback to user (toast/snackbar)
                }
                pendingUninstallEventId = null
            }
        }

        // Show uninstall confirmation dialog
        if (showUninstallDialog && pendingUninstallEventId != null) {
            AlertMapUninstall(
                onConfirm = { showUninstallDialog = false },
                onDismiss = {
                    showUninstallDialog = false
                    pendingUninstallEventId = null
                },
            )
        }

        // Use shared EventsListScreen for perfect UI parity
        EventsScreen(
            events = events,
            mapStates = mapStates,
            viewModelRefreshTrigger = viewModelRefreshTrigger,
            onEventClick = { eventId -> platformEnabler.openEventActivity(eventId) },
            setEventFavorite = setEventFavorite,
            onMapUninstallRequested = onMapUninstallRequested,
            modifier = modifier,
        )
    }
}
