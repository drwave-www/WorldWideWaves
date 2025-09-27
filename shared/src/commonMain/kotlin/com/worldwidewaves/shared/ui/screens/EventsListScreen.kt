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

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.worldwidewaves.shared.MokoRes
import com.worldwidewaves.shared.WWWGlobals.Dimensions
import com.worldwidewaves.shared.WWWGlobals.EventsList
import com.worldwidewaves.shared.data.SetEventFavorite
import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.format.DateTimeFormats
import com.worldwidewaves.shared.generated.resources.Res
import com.worldwidewaves.shared.generated.resources.downloaded_icon
import com.worldwidewaves.shared.generated.resources.favorite_off
import com.worldwidewaves.shared.generated.resources.favorite_on
import com.worldwidewaves.shared.ui.components.event.EventOverlayDone
import com.worldwidewaves.shared.ui.components.event.EventOverlaySoonOrRunning
import com.worldwidewaves.shared.ui.theme.sharedCommonTextStyle
import com.worldwidewaves.shared.ui.theme.sharedExtendedLight
import com.worldwidewaves.shared.ui.theme.sharedPrimaryColoredBoldTextStyle
import com.worldwidewaves.shared.ui.theme.sharedQuaternaryColoredTextStyle
import com.worldwidewaves.shared.ui.theme.sharedQuinaryColoredTextStyle
import com.worldwidewaves.shared.utils.Log
import dev.icerock.moko.resources.compose.stringResource
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import kotlin.time.ExperimentalTime

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
    modifier: Modifier = Modifier,
) {
    Log.i("SharedEventsListScreen", "SharedEventsListScreen starting with ${events.size} events")

    // Filter state - exact Android match
    var starredSelected by remember { mutableStateOf(false) }
    var downloadedSelected by remember { mutableStateOf(false) }
    var filteredEvents by remember { mutableStateOf(events) }

    // Filter logic - EXACT Android match
    LaunchedEffect(starredSelected, downloadedSelected, events) {
        filteredEvents =
            when {
                starredSelected -> events.filter { it.favorite }
                downloadedSelected -> events.filter { mapStates[it.id] == true }
                else -> events
            }
        Log.i(
            "SharedEventsListScreen",
            "Filtered events: ${filteredEvents.size} (starred: $starredSelected, downloaded: $downloadedSelected)",
        )
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
    )
}

@Composable
private fun EventsList(
    modifier: Modifier,
    events: List<IWWWEvent>,
    mapStates: Map<String, Boolean>,
    filterState: EventsFilterState,
    filterCallbacks: EventsFilterCallbacks,
    onEventClick: (String) -> Unit,
    setEventFavorite: SetEventFavorite?,
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
            modifier = Modifier.weight(1f),
        )
    }
}

/**
 * Three-segment control that lets the user switch between All / Favorites /
 * Downloaded filters. Visually implemented with a rounded container and
 * three equal-width clickable boxes.
 */
@Composable
private fun FavoritesSelector(
    starredSelected: Boolean,
    downloadedSelected: Boolean,
    onAllEventsClicked: () -> Unit,
    onFavoriteEventsClicked: () -> Unit,
    onDownloadedEventsClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Layout proportions for event selector buttons - EXACT Android match
    val allEventsButtonWidth = 1f / 3f
    val favoritesButtonWidth = 0.5f

    // Determine colors and weights based on which tab is selected - EXACT Android logic
    val allSelected = !starredSelected && !downloadedSelected

    val allColor = if (allSelected) sharedExtendedLight.quinary else sharedExtendedLight.quaternary
    val starredColor = if (starredSelected) sharedExtendedLight.quinary else sharedExtendedLight.quaternary
    val downloadedColor = if (downloadedSelected) sharedExtendedLight.quinary else sharedExtendedLight.quaternary

    val allWeight = if (allSelected) FontWeight.Bold else FontWeight.Normal
    val starredWeight = if (starredSelected) FontWeight.Bold else FontWeight.Normal
    val downloadedWeight = if (downloadedSelected) FontWeight.Bold else FontWeight.Normal

    Box(
        modifier =
            modifier
                .clip(RoundedCornerShape(EventsList.SELECTOR_ROUND.dp))
                .background(sharedExtendedLight.quaternary.color),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            SelectorBox(
                modifier = Modifier.fillMaxWidth(allEventsButtonWidth),
                backgroundColor = allColor.color,
                onClick = onAllEventsClicked,
                textColor = allColor.onColor,
                fontWeight = allWeight,
                text = stringResource(MokoRes.strings.events_select_all),
            )
            SelectorBox(
                modifier = Modifier.fillMaxWidth(favoritesButtonWidth),
                backgroundColor = starredColor.color,
                onClick = onFavoriteEventsClicked,
                textColor = starredColor.onColor,
                fontWeight = starredWeight,
                text = stringResource(MokoRes.strings.events_select_starred),
            )
            SelectorBox(
                modifier = Modifier.fillMaxWidth(1f),
                backgroundColor = downloadedColor.color,
                onClick = onDownloadedEventsClicked,
                textColor = downloadedColor.onColor,
                fontWeight = downloadedWeight,
                text = stringResource(MokoRes.strings.events_select_downloaded),
            )
        }
    }
}

@Composable
private fun SelectorBox(
    modifier: Modifier,
    backgroundColor: Color,
    onClick: () -> Unit,
    textColor: Color,
    fontWeight: FontWeight,
    text: String,
) {
    Box(
        modifier =
            modifier
                .clip(RoundedCornerShape(EventsList.SELECTOR_ROUND.dp))
                .height(EventsList.SELECTOR_HEIGHT.dp)
                .background(backgroundColor)
                .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style =
                sharedCommonTextStyle(EventsList.SELECTOR_FONTSIZE).copy(
                    color = textColor,
                    fontWeight = fontWeight,
                ),
        )
    }
}

@Composable
private fun Events(
    events: List<IWWWEvent>,
    mapStates: Map<String, Boolean>,
    starredSelected: Boolean,
    downloadedSelected: Boolean,
    onEventClick: (String) -> Unit,
    setEventFavorite: SetEventFavorite?,
    modifier: Modifier = Modifier,
) {
    val state = rememberLazyListState()

    LazyColumn(
        state = state,
        modifier = modifier,
    ) {
        if (events.isNotEmpty()) {
            items(events) { event ->
                val isMapInstalled = mapStates[event.id] ?: false
                Event(event, isMapInstalled, starredSelected, onEventClick, setEventFavorite)
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

@Composable
private fun Event(
    event: IWWWEvent,
    isMapInstalled: Boolean,
    starredSelected: Boolean,
    onEventClick: (String) -> Unit,
    setEventFavorite: SetEventFavorite?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier.clickable {
                onEventClick(event.id)
            },
    ) {
        EventOverlay(event, isMapInstalled, starredSelected, setEventFavorite)
        EventLocationAndDate(event)
    }
}

@Composable
private fun EventOverlay(
    event: IWWWEvent,
    isMapInstalled: Boolean,
    starredSelected: Boolean,
    setEventFavorite: SetEventFavorite?,
    modifier: Modifier = Modifier,
) {
    val heightModifier = Modifier.height(EventsList.OVERLAY_HEIGHT.dp)
    val eventStatus by event.observer.eventStatus.collectAsState()

    Box(modifier = heightModifier) {
        Box(modifier = heightModifier) {
            // Event background image - EXACT Android match
            val eventImageResource = event.getLocationImage() as? DrawableResource
            if (eventImageResource != null) {
                Image(
                    modifier = modifier.fillMaxWidth(),
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.TopCenter,
                    painter = painterResource(eventImageResource),
                    contentDescription = stringResource(event.getLocation()),
                )
            } else {
                // Fallback background color
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                )
            }
        }

        EventOverlayCountryAndCommunityFlags(event, heightModifier)

        EventOverlaySoonOrRunning(eventStatus)
        EventOverlayDone(eventStatus)
        EventOverlayMapDownloaded(event.id, isMapInstalled)
        EventOverlayFavorite(event, starredSelected, setEventFavorite)
    }
}

@Composable
private fun EventOverlayCountryAndCommunityFlags(
    event: IWWWEvent,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxHeight(),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        // Community flag (top-left) - EXACT Android match
        event.community?.let {
            val communityImageResource = event.getCommunityImage() as? DrawableResource
            if (communityImageResource != null) {
                EventFlag(
                    modifier =
                        Modifier.padding(
                            start = Dimensions.DEFAULT_INT_PADDING.dp,
                            top = Dimensions.DEFAULT_INT_PADDING.dp,
                        ),
                    imageResource = communityImageResource,
                    contentDescription = event.community!!,
                )
            }
        }

        // Country flag (bottom-left) - EXACT Android match
        event.country?.let {
            val countryImageResource = event.getCountryImage() as? DrawableResource
            if (countryImageResource != null) {
                EventFlag(
                    modifier =
                        Modifier.padding(
                            start = Dimensions.DEFAULT_INT_PADDING.dp,
                            bottom = Dimensions.DEFAULT_INT_PADDING.dp,
                        ),
                    imageResource = countryImageResource,
                    contentDescription = event.country!!,
                )
            }
        }
    }
}

@Composable
private fun EventFlag(
    modifier: Modifier,
    imageResource: DrawableResource,
    contentDescription: String,
) {
    Image(
        modifier = modifier.width(EventsList.FLAG_WIDTH.dp),
        contentScale = ContentScale.FillWidth,
        painter = painterResource(imageResource),
        contentDescription = contentDescription,
    )
}

@Composable
private fun EventOverlayMapDownloaded(
    @Suppress("UNUSED_PARAMETER") eventId: String,
    isMapInstalled: Boolean,
    modifier: Modifier = Modifier,
) {
    if (isMapInstalled) {
        Box(
            modifier =
                modifier
                    .fillMaxSize()
                    .padding(
                        end = Dimensions.DEFAULT_INT_PADDING.dp * 2 + EventsList.MAPDL_IMAGE_SIZE.dp,
                        bottom = Dimensions.DEFAULT_INT_PADDING.dp,
                    ),
            contentAlignment = Alignment.BottomEnd,
        ) {
            Image(
                modifier =
                    Modifier
                        .size(EventsList.MAPDL_IMAGE_SIZE.dp)
                        .clickable {
                            // NOTE: Map uninstall dialog implementation pending
                        },
                painter = painterResource(Res.drawable.downloaded_icon),
                contentDescription = stringResource(MokoRes.strings.map_downloaded),
            )
        }
    }
}

@Composable
private fun EventOverlayFavorite(
    event: IWWWEvent,
    @Suppress("UNUSED_PARAMETER") starredSelected: Boolean,
    setEventFavorite: SetEventFavorite?,
    modifier: Modifier = Modifier,
) {
    var isFavorite by remember { mutableStateOf(event.favorite) }
    var pendingFavoriteToggle by remember { mutableStateOf(false) }

    // Handle favorite toggle
    LaunchedEffect(pendingFavoriteToggle) {
        if (pendingFavoriteToggle) {
            setEventFavorite?.let { favoriteSetter ->
                try {
                    isFavorite = !isFavorite
                    favoriteSetter.call(event, isFavorite)
                    Log.i("SharedEventsListScreen", "Favorite toggled for ${event.id}: $isFavorite")
                } catch (e: Exception) {
                    // Revert on error
                    isFavorite = !isFavorite
                    Log.e("SharedEventsListScreen", "Failed to toggle favorite for ${event.id}", e)
                }
            }
            pendingFavoriteToggle = false
        }
    }

    // Sync with event favorite state changes - EXACT Android match
    LaunchedEffect(event.favorite) {
        isFavorite = event.favorite
    }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .padding(
                    end = Dimensions.DEFAULT_INT_PADDING.dp,
                    bottom = Dimensions.DEFAULT_INT_PADDING.dp,
                ),
        contentAlignment = Alignment.BottomEnd,
    ) {
        Surface(
            modifier = Modifier.clip(CircleShape),
            color = MaterialTheme.colorScheme.primary,
        ) {
            Image(
                modifier =
                    Modifier
                        .size(EventsList.FAVS_IMAGE_SIZE.dp)
                        .clickable {
                            setEventFavorite?.let {
                                pendingFavoriteToggle = true
                            }
                        },
                painter =
                    painterResource(
                        if (isFavorite) Res.drawable.favorite_on else Res.drawable.favorite_off,
                    ),
                contentDescription =
                    stringResource(
                        if (isFavorite) MokoRes.strings.event_favorite_on else MokoRes.strings.event_favorite_off,
                    ),
            )
        }
    }
}

/**
 * Displays event location, date and "country / community" using
 * localized date formatting.
 */
@Composable
@OptIn(ExperimentalTime::class)
private fun EventLocationAndDate(
    event: IWWWEvent,
    modifier: Modifier = Modifier,
) {
    val eventDate =
        remember(event.id) {
            try {
                DateTimeFormats.dayMonth(event.getStartDateTime(), event.getTZ())
            } catch (e: Exception) {
                "Dec 24" // Fallback date
            }
        }

    Box(modifier = modifier) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            // Row 1: Location (left) + Date (right) - EXACT Android layout
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = stringResource(event.getLocation()),
                    style = sharedQuinaryColoredTextStyle(EventsList.EVENT_LOCATION_FONTSIZE),
                )
                Text(
                    text = eventDate,
                    modifier = Modifier.padding(end = 2.dp),
                    style = sharedPrimaryColoredBoldTextStyle(EventsList.EVENT_DATE_FONTSIZE),
                )
            }

            // Row 2: Country / Community with -8dp offset - EXACT Android
            Row(
                modifier = Modifier.padding(top = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                event.country?.let { country ->
                    Text(
                        text = stringResource(event.getLiteralCountry()),
                        style = sharedQuinaryColoredTextStyle(EventsList.EVENT_COUNTRY_FONTSIZE),
                        modifier = Modifier.offset(y = (-8).dp).padding(start = 2.dp),
                    )
                }
                Text(
                    text = " / ",
                    style = sharedQuinaryColoredTextStyle(EventsList.EVENT_COUNTRY_FONTSIZE),
                    modifier = Modifier.offset(y = (-8).dp).padding(start = 2.dp),
                )
                event.community?.let { community ->
                    Text(
                        text = stringResource(event.getLiteralCommunity()),
                        style = sharedQuaternaryColoredTextStyle(EventsList.EVENT_COMMUNITY_FONTSIZE),
                        modifier = Modifier.offset(y = (-8).dp).padding(start = 2.dp),
                    )
                }
            }
        }
    }
}
