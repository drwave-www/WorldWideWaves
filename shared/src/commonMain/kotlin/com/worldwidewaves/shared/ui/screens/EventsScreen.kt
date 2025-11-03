package com.worldwidewaves.shared.ui.screens

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.worldwidewaves.shared.data.SetEventFavorite
import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.ui.components.EventsList
import com.worldwidewaves.shared.utils.Log

/**
 * Shared EventsListScreen - Identical UI on both Android and iOS.
 *
 * This is moved from Android-specific implementation to ensure
 * perfect UI parity between platforms.
 *
 * Features:
 * • Three-way filter (All / Favorites / Downloaded maps)
 * • Event overlay badges (soon/running, done, favorite, downloaded)
 * • Reactive updates via event state and favorites
 * • Navigation callback for event click handling
 */

data class EventsFilterState(
    val starredSelected: Boolean,
    val downloadedSelected: Boolean,
)

data class EventsFilterCallbacks(
    val onAllEventsClicked: () -> Unit,
    val onFavoriteEventsClicked: () -> Unit,
    val onDownloadedEventsClicked: () -> Unit,
)

@Composable
fun EventsScreen(
    events: List<IWWWEvent>,
    mapStates: Map<String, Boolean> = emptyMap(),
    onEventClick: (String) -> Unit = {},
    setEventFavorite: SetEventFavorite? = null,
    onMapUninstallRequested: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Log.i("SharedEventsListScreen", "SharedEventsListScreen starting with ${events.size} events")

    // Filter state - exact Android match
    var starredSelected by remember { mutableStateOf(false) }
    var downloadedSelected by remember { mutableStateOf(false) }
    var filteredEvents by remember { mutableStateOf(events) }

    // Refresh trigger - incremented when favorites change to force re-filtering
    var refreshTrigger by remember { mutableStateOf(0) }

    // Filter logic - EXACT Android match with refresh trigger
    // Note: mapStates dependency ensures filter updates when maps are downloaded
    LaunchedEffect(starredSelected, downloadedSelected, events, mapStates, refreshTrigger) {
        filteredEvents =
            when {
                starredSelected -> events.filter { it.favorite }
                downloadedSelected -> events.filter { mapStates[it.id] == true }
                else -> events
            }
        Log.i(
            "SharedEventsListScreen",
            "Filtered events: ${filteredEvents.size} (starred: $starredSelected, downloaded: $downloadedSelected, refresh: $refreshTrigger)",
        )
    }

    // Create callback to refresh filter when favorites change
    val onFavoriteChanged: () -> Unit = {
        refreshTrigger++
        Log.i("SharedEventsListScreen", "Favorite changed, refresh trigger: $refreshTrigger")
    }

    EventsList(
        modifier = modifier,
        events = filteredEvents,
        mapStates = mapStates,
        filterState =
            EventsFilterState(
                starredSelected = starredSelected,
                downloadedSelected = downloadedSelected,
            ),
        filterCallbacks =
            EventsFilterCallbacks(
                onAllEventsClicked = {
                    starredSelected = false
                    downloadedSelected = false
                },
                onFavoriteEventsClicked = {
                    starredSelected = true
                    downloadedSelected = false
                },
                onDownloadedEventsClicked = {
                    starredSelected = false
                    downloadedSelected = true
                },
            ),
        onEventClick = onEventClick,
        setEventFavorite = setEventFavorite,
        onFavoriteChanged = onFavoriteChanged,
        onMapUninstallRequested = onMapUninstallRequested,
    )
}
