package com.worldwidewaves.compose.tabs

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.worldwidewaves.activities.event.EventActivity
import com.worldwidewaves.shared.ui.TabScreen
import com.worldwidewaves.shared.viewmodels.EventsViewModel
import com.worldwidewaves.utils.MapAvailabilityChecker

/**
 * Android wrapper for SharedEventsListScreen.
 * Handles Android-specific navigation and lifecycle management while
 * delegating all UI to the shared component for perfect parity.
 */
class EventsListScreen(
    private val viewModel: EventsViewModel,
    private val mapChecker: MapAvailabilityChecker,
) : TabScreen {
    override val name = "Events"

    @Composable
    override fun Screen(modifier: Modifier) {
        val events by viewModel.events.collectAsState()
        val mapStates by mapChecker.mapStates.collectAsState()
        val context = LocalContext.current

        // Refresh map availability when screen resumes
        val lifecycleOwner = LocalLifecycleOwner.current
        DisposableEffect(lifecycleOwner) {
            val observer =
                LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        mapChecker.refreshAvailability()
                    }
                }

            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }

        // Pre-track all event IDs
        LaunchedEffect(events) {
            mapChecker.trackMaps(events.map { it.id })
            mapChecker.refreshAvailability()
        }

        // Use shared EventsListScreen for perfect UI parity
        com.worldwidewaves.shared.ui.screens.SharedEventsListScreen(
            events = events,
            mapStates = mapStates,
            onEventClick = { eventId ->
                context.startActivity(
                    Intent(context, EventActivity::class.java).apply {
                        putExtra("eventId", eventId)
                    }
                )
            },
            modifier = modifier
        )
    }
}