package com.worldwidewaves.shared.ui.components

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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.worldwidewaves.shared.MokoRes
import com.worldwidewaves.shared.WWWGlobals.Dimensions
import com.worldwidewaves.shared.WWWGlobals.EventsList
import com.worldwidewaves.shared.data.SetEventFavorite
import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.ui.screens.EventsFilterCallbacks
import com.worldwidewaves.shared.ui.screens.EventsFilterState
import com.worldwidewaves.shared.ui.theme.sharedQuinaryColoredTextStyle
import dev.icerock.moko.resources.compose.stringResource

/**
 * Main events list component that displays the filter selector and events list.
 */
@Composable
fun EventsList(
    modifier: Modifier,
    events: List<IWWWEvent>,
    mapStates: Map<String, Boolean>,
    filterState: EventsFilterState,
    filterCallbacks: EventsFilterCallbacks,
    onEventClick: (String) -> Unit,
    setEventFavorite: SetEventFavorite?,
    onFavoriteChanged: () -> Unit = {},
    onMapUninstallRequested: (String) -> Unit = {},
) {
    Column(
        modifier =
            modifier
                .fillMaxHeight()
                .padding(Dimensions.DEFAULT_EXT_PADDING.dp),
    ) {
        FavoritesSelector(
            starredSelected = filterState.starredSelected,
            downloadedSelected = filterState.downloadedSelected,
            onAllEventsClicked = filterCallbacks.onAllEventsClicked,
            onFavoriteEventsClicked = filterCallbacks.onFavoriteEventsClicked,
            onDownloadedEventsClicked = filterCallbacks.onDownloadedEventsClicked,
        )
        Spacer(modifier = Modifier.size(Dimensions.SPACER_MEDIUM.dp))
        Events(
            events = events,
            mapStates = mapStates,
            starredSelected = filterState.starredSelected,
            downloadedSelected = filterState.downloadedSelected,
            onEventClick = onEventClick,
            setEventFavorite = setEventFavorite,
            onFavoriteChanged = onFavoriteChanged,
            onMapUninstallRequested = onMapUninstallRequested,
            modifier = Modifier.weight(1f),
        )
    }
}

/**
 * Scrollable list of events with empty state handling.
 */
@Composable
private fun Events(
    events: List<IWWWEvent>,
    mapStates: Map<String, Boolean>,
    starredSelected: Boolean,
    downloadedSelected: Boolean,
    onEventClick: (String) -> Unit,
    setEventFavorite: SetEventFavorite?,
    onFavoriteChanged: () -> Unit = {},
    onMapUninstallRequested: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val state = rememberLazyListState()

    LazyColumn(
        state = state,
        modifier = modifier.testTag("EventsList"),
    ) {
        if (events.isNotEmpty()) {
            items(events) { event ->
                val isMapInstalled = mapStates[event.id] ?: false
                EventCard(
                    event,
                    isMapInstalled,
                    starredSelected,
                    onEventClick,
                    setEventFavorite,
                    onFavoriteChanged,
                    onMapUninstallRequested,
                )
            }
        } else {
            item {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text =
                        stringResource(
                            when {
                                starredSelected -> MokoRes.strings.events_favorites_empty
                                downloadedSelected -> MokoRes.strings.events_downloaded_empty
                                else -> MokoRes.strings.events_empty
                            },
                        ),
                    style =
                        sharedQuinaryColoredTextStyle(EventsList.NOEVENTS_FONTSIZE).copy(
                            textAlign = TextAlign.Center,
                        ),
                )
            }
        }
    }
}
