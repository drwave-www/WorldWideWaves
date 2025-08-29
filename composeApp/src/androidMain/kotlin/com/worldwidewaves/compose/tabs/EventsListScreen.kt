package com.worldwidewaves.compose.tabs

/*
 * Copyright 2025 DrWave
 *
 * WorldWideWaves is an ephemeral mobile app designed to orchestrate human waves through cities and
 * countries, culminating in a global wave. The project aims to transcend physical and cultural
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.worldwidewaves.activities.event.EventActivity
import com.worldwidewaves.activities.utils.TabScreen
import com.worldwidewaves.compose.EventOverlayDone
import com.worldwidewaves.compose.EventOverlaySoonOrRunning
import com.worldwidewaves.shared.MokoRes
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_DEFAULT_EXT_PADDING
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_DEFAULT_INT_PADDING
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_DEFAULT_SPACER_MEDIUM
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_EVENTS_EVENT_COMMUNITY_FONSIZE
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_EVENTS_EVENT_COUNTRY_FONSIZE
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_EVENTS_EVENT_DATE_FONSIZE
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_EVENTS_EVENT_LOCATION_FONSIZE
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_EVENTS_FAVS_IMAGE_SIZE
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_EVENTS_FLAG_WIDTH
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_EVENTS_MAPDL_IMAGE_SIZE
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_EVENTS_NOEVENTS_FONTSIZE
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_EVENTS_OVERLAY_HEIGHT
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_EVENTS_SELECTOR_FONTSIZE
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_EVENTS_SELECTOR_HEIGHT
import com.worldwidewaves.shared.WWWGlobals.Companion.DIM_EVENTS_SELECTOR_ROUND
import com.worldwidewaves.shared.data.SetEventFavorite
import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.events.utils.Log
import com.worldwidewaves.shared.generated.resources.downloaded_icon
import com.worldwidewaves.shared.generated.resources.event_favorite_off
import com.worldwidewaves.shared.generated.resources.event_favorite_on
import com.worldwidewaves.shared.generated.resources.events_cannot_uninstall_map_message
import com.worldwidewaves.shared.generated.resources.events_downloaded_empty
import com.worldwidewaves.shared.generated.resources.events_empty
import com.worldwidewaves.shared.generated.resources.events_favorites_empty
import com.worldwidewaves.shared.generated.resources.events_loading_error
import com.worldwidewaves.shared.generated.resources.events_uninstall
import com.worldwidewaves.shared.generated.resources.events_uninstall_cancel
import com.worldwidewaves.shared.generated.resources.events_uninstall_map_confirmation
import com.worldwidewaves.shared.generated.resources.events_uninstall_map_title
import com.worldwidewaves.shared.generated.resources.favorite_off
import com.worldwidewaves.shared.generated.resources.favorite_on
import com.worldwidewaves.shared.generated.resources.map_downloaded
import com.worldwidewaves.theme.commonTextStyle
import com.worldwidewaves.theme.extendedLight
import com.worldwidewaves.theme.primaryColoredBoldTextStyle
import com.worldwidewaves.theme.quaternaryColoredTextStyle
import com.worldwidewaves.theme.quinaryColoredTextStyle
import com.worldwidewaves.utils.MapAvailabilityChecker
import com.worldwidewaves.viewmodels.EventsViewModel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import com.worldwidewaves.shared.generated.resources.Res as ShRes

// ----------------------------

class EventsListScreen(
    private val viewModel: EventsViewModel,
    private val mapChecker: MapAvailabilityChecker,
    private val setEventFavorite: SetEventFavorite
) : TabScreen {
    override val name = "Events"

    private var firstLaunch = true

    // ----------------------------

    @Composable
    override fun Screen(modifier: Modifier) {
        val events by viewModel.events.collectAsState()
        val hasFavorites by viewModel.hasFavorites.collectAsState()
        val mapStates by mapChecker.mapStates.collectAsState()

        // Convert to Compose state (save across config changes)
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
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    // Refresh availability when screen resumes
                    mapChecker.refreshAvailability()
                }
            }

            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }

        // Pre-track all event IDs
        LaunchedEffect(viewModel.originalEvents) {
            mapChecker.trackMaps(viewModel.originalEvents.map { it.id })
            mapChecker.refreshAvailability()
        }

        fun selectTab(starred: Boolean = false, downloaded: Boolean = false) {
            starredSelected = starred
            downloadedSelected = downloaded
        }

        EventsList(
            modifier = modifier,
            events = events,
            mapStates = mapStates,
            starredSelected = starredSelected,
            downloadedSelected = downloadedSelected,
            onAllEventsClicked = { selectTab() },
            onFavoriteEventsClicked = { selectTab(starred = true) },
            onDownloadedEventsClicked = { selectTab(downloaded = true) }
        )
    }


    // ----------------------------

    @Composable
    private fun EventsList(
        modifier: Modifier,
        events: List<IWWWEvent>,
        mapStates: Map<String, Boolean>,
        starredSelected: Boolean,
        downloadedSelected: Boolean,
        onAllEventsClicked: () -> Unit,
        onFavoriteEventsClicked: () -> Unit,
        onDownloadedEventsClicked: () -> Unit
    ) {
        Column(
            modifier = modifier
                .fillMaxHeight()
                .padding(DIM_DEFAULT_EXT_PADDING.dp)
        ) {
            FavoritesSelector(
                starredSelected = starredSelected,
                downloadedSelected = downloadedSelected,
                onAllEventsClicked = onAllEventsClicked,
                onFavoriteEventsClicked = onFavoriteEventsClicked,
                onDownloadedEventsClicked = onDownloadedEventsClicked
            )
            Spacer(modifier = Modifier.size(DIM_DEFAULT_SPACER_MEDIUM.dp))
            Events(
                viewModel = viewModel,
                events = events,
                mapStates = mapStates,
                starredSelected = starredSelected,
                downloadedSelected = downloadedSelected,
                modifier = Modifier.weight(1f)
            )
        }
    }

    // ----------------------------

    @Composable
    private fun FavoritesSelector(
        starredSelected: Boolean,
        downloadedSelected: Boolean,
        onAllEventsClicked: () -> Unit,
        onFavoriteEventsClicked: () -> Unit,
        onDownloadedEventsClicked: () -> Unit,
        modifier: Modifier = Modifier
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
            modifier = modifier
                .clip(RoundedCornerShape(25.dp))
                .background(extendedLight.quaternary.color)
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                SelectorBox(
                    modifier = Modifier.fillMaxWidth(1/3f),
                    backgroundColor = allColor.color,
                    onClick = onAllEventsClicked,
                    textColor = allColor.onColor,
                    fontWeight = allWeight,
                    text = stringResource(MokoRes.strings.events_select_all.resourceId)
                )
                SelectorBox(
                    modifier = Modifier.fillMaxWidth(0.5f),
                    backgroundColor = starredColor.color,
                    onClick = onFavoriteEventsClicked,
                    textColor = starredColor.onColor,
                    fontWeight = starredWeight,
                    text = stringResource(MokoRes.strings.events_select_starred.resourceId)
                )
                SelectorBox(
                    modifier = Modifier.fillMaxWidth(1f),
                    backgroundColor = downloadedColor.color,
                    onClick = onDownloadedEventsClicked,
                    textColor = downloadedColor.onColor,
                    fontWeight = downloadedWeight,
                    text = stringResource(MokoRes.strings.events_select_downloaded.resourceId)
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
        text: String
    ) {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(DIM_EVENTS_SELECTOR_ROUND.dp))
                .height(DIM_EVENTS_SELECTOR_HEIGHT.dp)
                .background(backgroundColor)
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = commonTextStyle(DIM_EVENTS_SELECTOR_FONTSIZE).copy(
                    color = textColor,
                    fontWeight = fontWeight
                )
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
        modifier: Modifier = Modifier
    ) {
        val state = rememberLazyListState()
        val hasLoadingError by viewModel.hasLoadingError.collectAsState()

        LazyColumn(
            state = state,
            modifier = modifier
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
                        text = stringResource(
                            when {
                                hasLoadingError -> ShRes.string.events_loading_error
                                starredSelected -> ShRes.string.events_favorites_empty
                                downloadedSelected -> ShRes.string.events_downloaded_empty
                                else -> ShRes.string.events_empty
                            }
                        ),
                        style = quinaryColoredTextStyle(DIM_EVENTS_NOEVENTS_FONTSIZE).copy(
                            textAlign = TextAlign.Center
                        )
                    )
                }
            }
        }
    }

    @Composable
    fun Event(viewModel: EventsViewModel, event: IWWWEvent, isMapInstalled: Boolean, starredSelected: Boolean, modifier: Modifier = Modifier) {
        val context = LocalContext.current

        Column(modifier = modifier.clickable(
            onClick = {
                context.startActivity(Intent(context, EventActivity::class.java).apply {
                    putExtra("eventId", event.id)
                })
            }
        )) {
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
        modifier: Modifier = Modifier
    ) {
        val heightModifier = Modifier.height(DIM_EVENTS_OVERLAY_HEIGHT.dp)
        val eventStatus by event.observer.eventStatus.collectAsState()

        Box(modifier = heightModifier) {
            Box(modifier = heightModifier) {
                Image(
                    modifier = modifier.fillMaxWidth(),
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.TopCenter,
                    painter = painterResource(event.getLocationImage() as DrawableResource),
                    contentDescription = event.location
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
        modifier: Modifier = Modifier
    ) {
        Column(
            modifier = modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            event.community?.let {
                EventFlag(
                    modifier = Modifier.padding(start = DIM_DEFAULT_INT_PADDING.dp, top = DIM_DEFAULT_INT_PADDING.dp),
                    imageResource = event.getCommunityImage() as DrawableResource,
                    contentDescription = event.community!!
                )
            }

            event.country?.let {
                EventFlag(
                    modifier = Modifier.padding(start = DIM_DEFAULT_INT_PADDING.dp, bottom = DIM_DEFAULT_INT_PADDING.dp),
                    imageResource = event.getCountryImage() as DrawableResource,
                    contentDescription = event.country!!
                )
            }
        }
    }

    @Composable
    private fun EventFlag(
        modifier: Modifier,
        imageResource: DrawableResource,
        contentDescription: String
    ) {
        Image(
            modifier = modifier.width(DIM_EVENTS_FLAG_WIDTH.dp),
            contentScale = ContentScale.FillWidth,
            painter = painterResource(imageResource),
            contentDescription = contentDescription
        )
    }

    @Composable
    private fun EventOverlayMapDownloaded(
        eventId: String,
        isMapInstalled: Boolean,
        modifier: Modifier = Modifier
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
                modifier = modifier
                    .fillMaxSize()
                    .padding(
                        end = DIM_DEFAULT_INT_PADDING.dp * 2 + DIM_EVENTS_MAPDL_IMAGE_SIZE.dp,
                        bottom = DIM_DEFAULT_INT_PADDING.dp
                    ),
                contentAlignment = Alignment.BottomEnd
            ) {
                Image(
                    modifier = Modifier
                        .size(DIM_EVENTS_MAPDL_IMAGE_SIZE.dp)
                        .clickable { showUninstallDialog = true }, // Add clickable to show dialog
                    painter = painterResource(ShRes.drawable.downloaded_icon),
                    contentDescription = stringResource(ShRes.string.map_downloaded),
                )

                // Show confirmation dialog when clicked
                if (showUninstallDialog) {
                    AlertDialog(
                        onDismissRequest = { showUninstallDialog = false },
                        title = { Text(stringResource(ShRes.string.events_uninstall_map_title)) },
                        text = {
                            if (canUninstall) {
                                Text(stringResource(ShRes.string.events_uninstall_map_confirmation))
                            } else {
                                Text(stringResource(ShRes.string.events_cannot_uninstall_map_message))
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
                                                mapChecker.uninstallMap(eventId)

                                                var success = false
                                                repeat(20) { // ~5 seconds max
                                                    kotlinx.coroutines.delay(250)
                                                    mapChecker.refreshAvailability()
                                                    if (!mapChecker.isMapDownloaded(eventId)) {
                                                        success = true
                                                        return@repeat
                                                    }
                                                }
                                                uninstallSucceeded = success
                                            } catch (e: Exception) {
                                                Log.e("EventOverlayMapDownloaded", "Error uninstalling map for event $eventId", e)
                                                uninstallSucceeded = false
                                            } finally {
                                                isUninstalling = false
                                                showUninstallDialog = false
                                                showUninstallResult = true
                                            }
                                        }
                                    }
                                ) {
                                    Text(
                                        if (isUninstalling)
                                            "..."
                                        else
                                            stringResource(ShRes.string.events_uninstall)
                                    )
                                }
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showUninstallDialog = false }) {
                                Text(stringResource(ShRes.string.events_uninstall_cancel))
                            }
                        }
                    )
                }
            }
        }

        // Result dialog
        if (showUninstallResult) {
            AlertDialog(
                onDismissRequest = { showUninstallResult = false },
                title = { Text(stringResource(ShRes.string.events_uninstall_map_title)) },
                text = {
                    Text(
                        if (uninstallSucceeded)
                            "Uninstall completed"
                        else
                            "Uninstall failed"
                    )
                },
                confirmButton = {
                    TextButton(onClick = { showUninstallResult = false }) {
                        Text("OK")
                    }
                }
            )
        }
    }

    @Composable
    private fun EventOverlayFavorite(
        viewModel: EventsViewModel,
        event: IWWWEvent,
        starredSelected: Boolean,
        modifier: Modifier = Modifier
    ) {
        var isFavorite by remember { mutableStateOf(event.favorite) }
        val scope = rememberCoroutineScope()

        LaunchedEffect(event.favorite) {
            isFavorite = event.favorite
        }

        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(end = DIM_DEFAULT_INT_PADDING.dp, bottom = DIM_DEFAULT_INT_PADDING.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            Surface(
                modifier = Modifier.clip(CircleShape),
                color = MaterialTheme.colorScheme.primary
            ) {
                Image(
                    modifier = Modifier
                        .size(DIM_EVENTS_FAVS_IMAGE_SIZE.dp)
                        .clickable {
                            scope.launch {
                                isFavorite = !isFavorite
                                setEventFavorite.call(event, isFavorite)
                                if (starredSelected) {
                                    viewModel.filterEvents(onlyFavorites = true)
                                }
                            }
                        },
                    painter = painterResource(if (isFavorite) ShRes.drawable.favorite_on else ShRes.drawable.favorite_off),
                    contentDescription = stringResource(if (isFavorite) ShRes.string.event_favorite_on else ShRes.string.event_favorite_off),
                )
            }
        }
    }

    // ----------------------------

    @Composable
    private fun EventLocationAndDate(event: IWWWEvent, modifier: Modifier = Modifier) {
        val eventDate = event.getLiteralStartDateSimple()

        Box(modifier = modifier) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = event.location.uppercase(),
                        style = quinaryColoredTextStyle(DIM_EVENTS_EVENT_LOCATION_FONSIZE)
                    )
                    Text(
                        text = eventDate,
                        modifier = Modifier.padding(end = 2.dp),
                        style = primaryColoredBoldTextStyle(DIM_EVENTS_EVENT_DATE_FONSIZE)
                    )
                }

                // Country if present
                Row(
                    modifier = Modifier.padding(top = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = event.getLiteralCountry(),
                        style = quinaryColoredTextStyle(DIM_EVENTS_EVENT_COUNTRY_FONSIZE),
                        modifier = Modifier.offset(y = (-8).dp).padding(start = 2.dp)
                    )
                    Text(
                        text = " / ",
                        style = quinaryColoredTextStyle(DIM_EVENTS_EVENT_COUNTRY_FONSIZE),
                        modifier = Modifier.offset(y = (-8).dp).padding(start = 2.dp)
                    )
                    Text(
                        text = event.getLiteralCommunity(),
                        style = quaternaryColoredTextStyle(DIM_EVENTS_EVENT_COMMUNITY_FONSIZE),
                        modifier = Modifier.offset(y = (-8).dp).padding(start = 2.dp)
                    )
                }
            }
        }
    }
}
