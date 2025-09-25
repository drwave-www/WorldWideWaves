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

// Removed Android-specific imports for cross-platform compatibility
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
// Removed Android-specific EventActivity import
import com.worldwidewaves.shared.ui.TabScreen
import com.worldwidewaves.shared.ui.components.EventOverlayDone
import com.worldwidewaves.shared.ui.components.EventOverlaySoonOrRunning
import com.worldwidewaves.shared.MokoRes
import com.worldwidewaves.shared.WWWGlobals.Dimensions
import com.worldwidewaves.shared.WWWGlobals.EventsList
import com.worldwidewaves.shared.data.SetEventFavorite
import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.format.DateTimeFormats
import com.worldwidewaves.shared.generated.resources.downloaded_icon
import com.worldwidewaves.shared.generated.resources.favorite_off
import com.worldwidewaves.shared.generated.resources.favorite_on
import com.worldwidewaves.shared.utils.Log
import com.worldwidewaves.theme.commonTextStyle
import com.worldwidewaves.theme.extendedLight
import com.worldwidewaves.theme.primaryColoredBoldTextStyle
import com.worldwidewaves.theme.quaternaryColoredTextStyle
import com.worldwidewaves.theme.quinaryColoredTextStyle
import com.worldwidewaves.theme.scrimLight
import com.worldwidewaves.utils.MapAvailabilityChecker
import com.worldwidewaves.shared.viewmodels.EventsViewModel
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import kotlin.time.ExperimentalTime
import com.worldwidewaves.shared.generated.resources.Res as ShRes

// ----------------------------

/**
 * Tab that shows the full WorldWideWaves event catalogue.
 *
 * Implements [TabScreen] and provides:
 * • Three-way filter (All / Favorites / Downloaded maps)
 * • Live observation of map-module availability through [MapAvailabilityChecker]
 * • Navigation to [EventActivity] on row click
 * • Reactive updates via [EventsViewModel] (status, favorites, cache)
 *
 * Overlay badges (soon/running, done, favorite, downloaded) are composed on top
 * of each event thumbnail to surface real-time state.
 */
class EventsListScreen(
    private val viewModel: EventsViewModel,
    private val mapChecker: MapAvailabilityChecker,
    private val setEventFavorite: SetEventFavorite,
) : TabScreen {
    override val name = "Events"

    private var firstLaunch = true

    companion object {
        // Layout proportions for event selector buttons
        private const val ALL_EVENTS_BUTTON_WIDTH = 1f / 3f
        private const val FAVORITES_BUTTON_WIDTH = 0.5f
    }

    // ----------------------------

    @Composable
    /**
     * Root Composable: renders the selector chips + lazy list and refreshes map
     * availability on lifecycle resume.  Filtering is performed via
     * [EventsViewModel.filterEvents].
     */
    override fun Screen(modifier: Modifier) {
        val events by viewModel.events.collectAsState()
        val hasFavorites by viewModel.hasFavorites.collectAsState()
        val mapStates by mapChecker.mapStates.collectAsState()

        var starredSelected by rememberSaveable { mutableStateOf(false) }
        var downloadedSelected by rememberSaveable { mutableStateOf(false) }

        if (firstLaunch) { // Select favorites at launch if any
            firstLaunch = false
            starredSelected = hasFavorites
        }

        // Trigger filtering only when toggles actually change
        LaunchedEffect(starredSelected, downloadedSelected) {
            viewModel.filterEvents(starredSelected, downloadedSelected)
        }

        // Re-apply filtering when map install state changes while Downloaded tab is shown
        LaunchedEffect(mapStates, downloadedSelected) {
            if (downloadedSelected) {
                viewModel.filterEvents(onlyDownloaded = true)
            }
        }

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

        fun selectTab(
            starred: Boolean = false,
            downloaded: Boolean = false,
        ) {
            starredSelected = starred
            downloadedSelected = downloaded
        }

        EventsList(
            modifier = modifier,
            events = events,
            mapStates = mapStates,
            filterState =
                EventsFilterState(
                    starredSelected = starredSelected,
                    downloadedSelected = downloadedSelected,
                ),
            filterCallbacks =
                EventsFilterCallbacks(
                    onAllEventsClicked = { selectTab() },
                    onFavoriteEventsClicked = { selectTab(starred = true) },
                    onDownloadedEventsClicked = { selectTab(downloaded = true) },
                ),
        )
    }

    // ----------------------------

    private data class EventsFilterState(
        val starredSelected: Boolean,
        val downloadedSelected: Boolean,
    )

    private data class EventsFilterCallbacks(
        val onAllEventsClicked: () -> Unit,
        val onFavoriteEventsClicked: () -> Unit,
        val onDownloadedEventsClicked: () -> Unit,
    )

    @Composable
    private fun EventsList(
        modifier: Modifier,
        events: List<IWWWEvent>,
        mapStates: Map<String, Boolean>,
        filterState: EventsFilterState,
        filterCallbacks: EventsFilterCallbacks,
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
                viewModel = viewModel,
                events = events,
                mapStates = mapStates,
                starredSelected = filterState.starredSelected,
                downloadedSelected = filterState.downloadedSelected,
                modifier = Modifier.weight(1f),
            )
        }
    }

    // ----------------------------

    /**
     * Three-segment control that lets the user switch between All / Favorites /
     * Downloaded filters.  Visually implemented with a rounded container and
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
        // Determine colors and weights based on which tab is selected
        val allSelected = !starredSelected && !downloadedSelected

        val allColor = if (allSelected) extendedLight.quinary else extendedLight.quaternary
        val starredColor = if (starredSelected) extendedLight.quinary else extendedLight.quaternary
        val downloadedColor = if (downloadedSelected) extendedLight.quinary else extendedLight.quaternary

        val allWeight = if (allSelected) FontWeight.Bold else FontWeight.Normal
        val starredWeight = if (starredSelected) FontWeight.Bold else FontWeight.Normal
        val downloadedWeight = if (downloadedSelected) FontWeight.Bold else FontWeight.Normal

        Box(
            modifier =
                modifier
                    .clip(RoundedCornerShape(25.dp))
                    .background(extendedLight.quaternary.color),
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                SelectorBox(
                    modifier = Modifier.fillMaxWidth(ALL_EVENTS_BUTTON_WIDTH),
                    backgroundColor = allColor.color,
                    onClick = onAllEventsClicked,
                    textColor = allColor.onColor,
                    fontWeight = allWeight,
                    text = stringResource(MokoRes.strings.events_select_all),
                )
                SelectorBox(
                    modifier = Modifier.fillMaxWidth(FAVORITES_BUTTON_WIDTH),
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
                    commonTextStyle(EventsList.SELECTOR_FONTSIZE).copy(
                        color = textColor,
                        fontWeight = fontWeight,
                    ),
            )
        }
    }

    // ----------------------------

    @Composable
    fun Events(
        viewModel: EventsViewModel,
        events: List<IWWWEvent>,
        mapStates: Map<String, Boolean>,
        starredSelected: Boolean,
        downloadedSelected: Boolean,
        modifier: Modifier = Modifier,
    ) {
        val state = rememberLazyListState()
        val hasLoadingError by viewModel.hasLoadingError.collectAsState()

        LazyColumn(
            state = state,
            modifier = modifier,
        ) {
            if (events.isNotEmpty()) {
                items(events) { event ->
                    val isMapInstalled = mapStates[event.id] ?: false
                    Event(viewModel, event, isMapInstalled, starredSelected)
                }
            } else {
                item {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text =
                            stringResource(
                                when {
                                    hasLoadingError -> MokoRes.strings.events_loading_error
                                    starredSelected -> MokoRes.strings.events_favorites_empty
                                    downloadedSelected -> MokoRes.strings.events_downloaded_empty
                                    else -> MokoRes.strings.events_empty
                                },
                            ),
                        style =
                            quinaryColoredTextStyle(EventsList.NOEVENTS_FONTSIZE).copy(
                                textAlign = TextAlign.Center,
                            ),
                    )
                }
            }
        }
    }

    @Composable
    fun Event(
        viewModel: EventsViewModel,
        event: IWWWEvent,
        isMapInstalled: Boolean,
        starredSelected: Boolean,
        modifier: Modifier = Modifier,
    ) {
        val context = LocalContext.current

        Column(
            modifier =
                modifier.clickable(
                    onClick = {
                        context.startActivity(
                            Intent(context, EventActivity::class.java).apply {
                                putExtra("eventId", event.id)
                            },
                        )
                    },
                ),
        ) {
            EventOverlay(viewModel, event, isMapInstalled, starredSelected)
            EventLocationAndDate(event)
        }
    }

    // ----------------------------

    @Composable
    private fun EventOverlay(
        viewModel: EventsViewModel,
        event: IWWWEvent,
        isMapInstalled: Boolean,
        starredSelected: Boolean,
        modifier: Modifier = Modifier,
    ) {
        val heightModifier = Modifier.height(EventsList.OVERLAY_HEIGHT.dp)
        val eventStatus by event.observer.eventStatus.collectAsState()

        Box(modifier = heightModifier) {
            Box(modifier = heightModifier) {
                Image(
                    modifier = modifier.fillMaxWidth(),
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.TopCenter,
                    painter = painterResource(event.getLocationImage() as DrawableResource),
                    contentDescription = stringResource(event.getLocation()),
                )
            }

            EventOverlayCountryAndCommunityFlags(event, heightModifier)
            EventOverlaySoonOrRunning(eventStatus)
            EventOverlayDone(eventStatus)
            EventOverlayMapDownloaded(event.id, isMapInstalled)
            EventOverlayFavorite(viewModel, event, starredSelected)
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
            event.community?.let {
                EventFlag(
                    modifier =
                        Modifier.padding(
                            start = Dimensions.DEFAULT_INT_PADDING.dp,
                            top = Dimensions.DEFAULT_INT_PADDING.dp,
                        ),
                    imageResource = event.getCommunityImage() as DrawableResource,
                    contentDescription = event.community!!,
                )
            }

            event.country?.let {
                EventFlag(
                    modifier =
                        Modifier.padding(
                            start = Dimensions.DEFAULT_INT_PADDING.dp,
                            bottom = Dimensions.DEFAULT_INT_PADDING.dp,
                        ),
                    imageResource = event.getCountryImage() as DrawableResource,
                    contentDescription = event.country!!,
                )
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

    /**
     * Shows the “map downloaded” badge and handles **uninstall** flow:
     * confirmation dialog, async removal via [MapAvailabilityChecker],
     * progress/lock-state and final result dialog.
     */
    @Composable
    private fun EventOverlayMapDownloaded(
        eventId: String,
        isMapInstalled: Boolean,
        modifier: Modifier = Modifier,
    ) {
        // State for controlling dialog visibility
        var showUninstallDialog by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()

        // Check if the map can be uninstalled
        // Consider it uninstallable only when the module is effectively installed *now*
        val canUninstall = isMapInstalled

        var isUninstalling by remember { mutableStateOf(false) }
        var showUninstallResult by remember { mutableStateOf(false) }
        var uninstallSucceeded by remember { mutableStateOf(false) }

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
                            .clickable { showUninstallDialog = true },
                    // Add clickable to show dialog
                    painter = painterResource(ShRes.drawable.downloaded_icon),
                    contentDescription = stringResource(MokoRes.strings.map_downloaded),
                )

                // Show confirmation dialog when clicked
                if (showUninstallDialog) {
                    AlertDialog(
                        onDismissRequest = { showUninstallDialog = false },
                        title = {
                            Text(
                                stringResource(MokoRes.strings.events_uninstall_map_title),
                                style = commonTextStyle().copy(color = scrimLight),
                                fontWeight = FontWeight.Bold,
                            )
                        },
                        text = {
                            if (canUninstall) {
                                Text(
                                    stringResource(MokoRes.strings.events_uninstall_map_confirmation),
                                    style = commonTextStyle().copy(color = scrimLight),
                                )
                            } else {
                                Text(
                                    stringResource(MokoRes.strings.events_cannot_uninstall_map_message),
                                    style = commonTextStyle().copy(color = scrimLight),
                                )
                            }
                        },
                        confirmButton = {
                            if (canUninstall) {
                                Button(
                                    enabled = !isUninstalling,
                                    onClick = {
                                        scope.launch {
                                            isUninstalling = true
                                            try {
                                                // Use the new suspend uninstall API – returns true on success
                                                val success = mapChecker.uninstallMap(eventId)
                                                uninstallSucceeded = success
                                            } catch (e: IllegalStateException) {
                                                Log.e(
                                                    "EventOverlayMapDownloaded",
                                                    "Invalid state while uninstalling map for event $eventId",
                                                    e,
                                                )
                                            } catch (e: SecurityException) {
                                                Log.e(
                                                    "EventOverlayMapDownloaded",
                                                    "Security error while uninstalling map for event $eventId",
                                                    e,
                                                )
                                                uninstallSucceeded = false
                                            } finally {
                                                isUninstalling = false
                                                showUninstallDialog = false
                                                showUninstallResult = true
                                            }
                                        }
                                    },
                                ) {
                                    Text(
                                        if (isUninstalling) {
                                            "..."
                                        } else {
                                            stringResource(MokoRes.strings.events_uninstall)
                                        },
                                    )
                                }
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showUninstallDialog = false }) {
                                Text(stringResource(MokoRes.strings.events_uninstall_cancel))
                            }
                        },
                    )
                }
            }
        }

        // Result dialog
        if (showUninstallResult) {
            AlertDialog(
                onDismissRequest = { showUninstallResult = false },
                title = {
                    Text(
                        stringResource(MokoRes.strings.events_uninstall_map_title),
                        style = commonTextStyle().copy(color = scrimLight),
                        fontWeight = FontWeight.Bold,
                    )
                },
                text = {
                    Text(
                        if (uninstallSucceeded) {
                            stringResource(MokoRes.strings.events_uninstall_completed)
                        } else {
                            stringResource(MokoRes.strings.events_uninstall_failed)
                        },
                        style = commonTextStyle().copy(color = scrimLight),
                    )
                },
                confirmButton = {
                    TextButton(onClick = { showUninstallResult = false }) {
                        Text(stringResource(MokoRes.strings.ok))
                    }
                },
            )
        }
    }

    /**
     * Star toggle – persists new state through [SetEventFavorite] and re-filters
     * the list immediately if the Favorites tab is active.
     */
    @Composable
    private fun EventOverlayFavorite(
        viewModel: EventsViewModel,
        event: IWWWEvent,
        starredSelected: Boolean,
        modifier: Modifier = Modifier,
    ) {
        var isFavorite by remember { mutableStateOf(event.favorite) }
        val scope = rememberCoroutineScope()

        LaunchedEffect(event.favorite) {
            isFavorite = event.favorite
        }

        Box(
            modifier =
                modifier
                    .fillMaxSize()
                    .padding(end = Dimensions.DEFAULT_INT_PADDING.dp, bottom = Dimensions.DEFAULT_INT_PADDING.dp),
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
                                scope.launch {
                                    isFavorite = !isFavorite
                                    setEventFavorite.call(event, isFavorite)
                                    if (starredSelected) {
                                        viewModel.filterEvents(onlyFavorites = true)
                                    }
                                }
                            },
                    painter =
                        painterResource(
                            if (isFavorite) ShRes.drawable.favorite_on else ShRes.drawable.favorite_off,
                        ),
                    contentDescription =
                        stringResource(
                            if (isFavorite) MokoRes.strings.event_favorite_on else MokoRes.strings.event_favorite_off,
                        ),
                )
            }
        }
    }

    // ----------------------------

    /**
     * Displays event location, date and “country / community” using bidi-safe
     * wrapping and localized date formatting via [DateTimeFormats].
     */
    @Composable
    @OptIn(ExperimentalTime::class)
    private fun EventLocationAndDate(
        event: IWWWEvent,
        modifier: Modifier = Modifier,
    ) {
        val eventDate =
            remember(event.id) {
                DateTimeFormats.dayMonth(event.getStartDateTime(), event.getTZ())
            }
        val bidi = BidiFormatter.getInstance()
        val countryText = bidi.unicodeWrap(stringResource(event.getLiteralCountry()))
        val communityText = bidi.unicodeWrap(stringResource(event.getLiteralCommunity()))

        Box(modifier = modifier) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Text(
                        text = stringResource(event.getLocation()),
                        style = quinaryColoredTextStyle(EventsList.EVENT_LOCATION_FONTSIZE),
                    )
                    Text(
                        text = eventDate,
                        modifier = Modifier.padding(end = 2.dp),
                        style = primaryColoredBoldTextStyle(EventsList.EVENT_DATE_FONTSIZE),
                    )
                }

                // Country if present
                Row(
                    modifier = Modifier.padding(top = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = countryText,
                        style = quinaryColoredTextStyle(EventsList.EVENT_COUNTRY_FONTSIZE),
                        modifier = Modifier.offset(y = (-8).dp).padding(start = 2.dp),
                    )
                    Text(
                        text = " / ",
                        style = quinaryColoredTextStyle(EventsList.EVENT_COUNTRY_FONTSIZE),
                        modifier = Modifier.offset(y = (-8).dp).padding(start = 2.dp),
                    )
                    Text(
                        text = communityText,
                        style = quaternaryColoredTextStyle(EventsList.EVENT_COMMUNITY_FONTSIZE),
                        modifier = Modifier.offset(y = (-8).dp).padding(start = 2.dp),
                    )
                }
            }
        }
    }
}
