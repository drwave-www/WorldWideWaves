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
import androidx.compose.ui.Modifier
// REMOVED: Android-only lifecycle imports causing iOS crashes
// import androidx.lifecycle.Lifecycle
// import androidx.lifecycle.LifecycleEventObserver
// import androidx.lifecycle.compose.LocalLifecycleOwner
import com.worldwidewaves.shared.PlatformEnabler
import com.worldwidewaves.shared.data.SetEventFavorite
import com.worldwidewaves.shared.domain.usecases.IMapAvailabilityChecker
import com.worldwidewaves.shared.ui.screens.EventsScreen
import com.worldwidewaves.shared.viewmodels.EventsViewModel
import org.koin.core.component.KoinComponent

/**
 * Android wrapper for SharedEventsListScreen.
 * Handles Android-specific navigation and lifecycle management while
 * delegating all UI to the shared component for perfect parity.
 */
class EventsListScreen(
    private val viewModel: EventsViewModel,
    private val mapChecker: IMapAvailabilityChecker,
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

        // Initialize EventsViewModel (iOS-safe pattern, Android compatible)
        LaunchedEffect(Unit) {
            viewModel.loadEvents()
        }

        // TODO: Refresh map availability when screen resumes
        // FIXED: Use KMM-compatible approach instead of Android lifecycle
        LaunchedEffect(Unit) {
            mapChecker.refreshAvailability()
        }

        // Pre-track all event IDs
        LaunchedEffect(events) {
            mapChecker.trackMaps(events.map { it.id })
            mapChecker.refreshAvailability()
        }

        // Use shared EventsListScreen for perfect UI parity
        EventsScreen(
            events = events,
            mapStates = mapStates,
            onEventClick = { eventId -> platformEnabler.openEventActivity(eventId) },
            setEventFavorite = setEventFavorite,
            modifier = modifier,
        )
    }
}
