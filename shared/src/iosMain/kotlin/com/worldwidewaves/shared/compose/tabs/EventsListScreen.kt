package com.worldwidewaves.shared.compose.tabs

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.worldwidewaves.shared.PlatformEnabler
import com.worldwidewaves.shared.ui.TabScreen
import com.worldwidewaves.shared.ui.screens.SharedEventsListScreen
import com.worldwidewaves.shared.viewmodels.EventsViewModel
import com.worldwidewaves.shared.utils.Log

/**
 * iOS wrapper for SharedEventsListScreen.
 * Handles iOS-specific navigation while delegating all UI to the shared component for perfect parity.
 */
class EventsListScreen(
    private val viewModel: EventsViewModel,
    private val setEventFavorite: com.worldwidewaves.shared.data.SetEventFavorite,
) : TabScreen {
    override val name = "Events"

    @Composable
    override fun Screen(platformEnabler: PlatformEnabler, modifier: Modifier) {
        val events by viewModel.events.collectAsState()

        // Use shared EventsListScreen for perfect UI parity
        SharedEventsListScreen(
            events = events,
            mapStates = emptyMap(), // iOS simplified implementation for now
            onEventClick = { eventId ->
                Log.i("EventsListScreen", "iOS Event click: $eventId")
                // TODO: Navigate to event detail screen on iOS
            },
            setEventFavorite = setEventFavorite,
            modifier = modifier,
        )
    }
}