package com.worldwidewaves.compose

/*
 * Copyright 2024 DrWave
 *
 * WorldWideWaves is an ephemeral mobile app designed to orchestrate human waves through cities and countries,
 * culminating in a global wave. The project aims to transcend physical and cultural boundaries, fostering unity,
 * community, and shared human experience by leveraging real-time coordination and location-based services.
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
import androidx.compose.foundation.border
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.worldwidewaves.activities.EventActivity
import com.worldwidewaves.activities.utils.TabScreen
import com.worldwidewaves.models.EventsViewModel
import com.worldwidewaves.shared.SetEventFavorite
import com.worldwidewaves.shared.events.WWWEvent
import com.worldwidewaves.shared.events.getCommunityImage
import com.worldwidewaves.shared.events.getCountryImage
import com.worldwidewaves.shared.events.getFormattedSimpleDate
import com.worldwidewaves.shared.events.getLocationImage
import com.worldwidewaves.shared.generated.resources.event_favorite_off
import com.worldwidewaves.shared.generated.resources.event_favorite_on
import com.worldwidewaves.shared.generated.resources.events_select_all
import com.worldwidewaves.shared.generated.resources.events_select_starred
import com.worldwidewaves.shared.generated.resources.favorite_off
import com.worldwidewaves.shared.generated.resources.favorite_on
import com.worldwidewaves.theme.displayFontFamily
import com.worldwidewaves.theme.extendedLight
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import com.worldwidewaves.shared.generated.resources.Res as ShRes

// ----------------------------

class EventsListScreen(
    private val viewModel: EventsViewModel,
    private val setEventFavorite: SetEventFavorite
) : TabScreen {

    private var starredSelected = false
    private var firstLaunch = true

    override fun getName(): String = "Events"

    // ----------------------------

    @Composable
    override fun Screen(modifier: Modifier) {
        val events by viewModel.events.collectAsState()
        val hasFavorites by viewModel.hasFavorites.collectAsState()

        if (firstLaunch) { // Select favorites at launch if any
            firstLaunch = false
            starredSelected = hasFavorites
        }

        viewModel.filterEvents(starredSelected)

        EventsList(
            modifier, events,
            onAllEventsCLicked = { if (starredSelected) toggleStarredSelection() },
            onFavoriteEventsClicked = { if (!starredSelected) toggleStarredSelection() }
        )
    }

    private fun toggleStarredSelection() {
        starredSelected = !starredSelected
        viewModel.filterEvents(starredSelected)
    }

    // ----------------------------

    @Composable
    private fun EventsList(
        modifier: Modifier,
        events: List<WWWEvent>,
        onAllEventsCLicked: () -> Unit,
        onFavoriteEventsClicked: () -> Unit
    ) {
        Column(
            modifier = modifier
                .fillMaxHeight()
                .padding(start = 20.dp, end = 20.dp, top = 20.dp)
        ) {
            FavoritesSelector(onAllEventsCLicked, onFavoriteEventsClicked)
            Spacer(modifier = Modifier.size(20.dp))
            Events(viewModel, events, modifier = Modifier.weight(1f))
        }
    }

    // ----------------------------

    @Composable
    private fun FavoritesSelector(
        onAllEventsCLicked: () -> Unit,
        onFavoriteEventsClicked: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        val allColor = if (this.starredSelected) extendedLight.quaternary else extendedLight.quinary
        val starredColor = if (starredSelected) extendedLight.quinary else extendedLight.quaternary

        val allWeight = if (starredSelected) FontWeight.Normal else FontWeight.Bold
        val starredWeight = if (starredSelected) FontWeight.Bold else FontWeight.Normal

        Box(
            modifier = modifier
                .clip(RoundedCornerShape(25.dp))
                .background(extendedLight.quaternary.color)
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                SelectorBox(
                    modifier = Modifier.fillMaxWidth(.5f),
                    backgroundColor = allColor.color,
                    onClick = onAllEventsCLicked,
                    textColor = allColor.onColor,
                    fontWeight = allWeight,
                    text = stringResource(ShRes.string.events_select_all)
                )
                SelectorBox(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = starredColor.color,
                    onClick = onFavoriteEventsClicked,
                    textColor = starredColor.onColor,
                    fontWeight = starredWeight,
                    text = stringResource(ShRes.string.events_select_starred)
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
                .clip(RoundedCornerShape(25.dp))
                .height(50.dp)
                .background(backgroundColor)
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                color = textColor,
                fontWeight = fontWeight,
                fontSize = 16.sp,
                text = text,
                fontFamily = displayFontFamily
            )
        }
    }

    // ----------------------------

    @Composable
    fun Events(viewModel: EventsViewModel, events: List<WWWEvent>, modifier: Modifier = Modifier) {
        val state = rememberLazyListState()

        LazyColumn(
            state = state,
            modifier = modifier
        ) {
            if (events.isNotEmpty()) {
                items(events) { event -> Event(viewModel, event) }
            } else {
                item {
                    Text( // TODO: better explanation of favorites
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        text = "No events found",
                        fontFamily = displayFontFamily,
                        style = TextStyle(
                            color = extendedLight.quinary.color,
                            fontFamily = MaterialTheme.typography.bodyMedium.fontFamily,
                            fontSize = 24.sp
                        )
                    )
                }
            }
        }
    }

    @Composable
    fun Event(viewModel: EventsViewModel, event: WWWEvent, modifier: Modifier = Modifier) {
        val context = LocalContext.current

        Column(modifier = modifier.clickable(onClick = {
            context.startActivity(Intent(context, EventActivity::class.java).apply {
                putExtra("eventId", event.id)
            })
        })) {
            EventOverlay(viewModel, event)
            EventLocationAndDate(event)
        }
    }

    // ----------------------------

    @Composable
    private fun EventOverlay(viewModel: EventsViewModel, event: WWWEvent, modifier: Modifier = Modifier) {
        val heightModifier = Modifier.height(159.dp)

        Box(modifier = heightModifier) {
            // Main Image
            Box(modifier = heightModifier) {
                Image(
                    modifier = modifier,
                    contentScale = ContentScale.FillWidth,
                    painter = painterResource(event.getLocationImage() as DrawableResource),
                    contentDescription = event.location
                )
            }

            EventOverlayCountryAndCommunityFlags(event, heightModifier)
            EventOverlaySoonOrRunning(event)
            EventOverlayDone(event)
            EventOverlayFavorite(viewModel, event)
        }
    }

    @Composable
    private fun EventOverlayCountryAndCommunityFlags(event: WWWEvent, modifier: Modifier = Modifier) {
        Column(
            modifier = modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            event.community?.let {
                EventFlag(
                    modifier = Modifier.padding(start = 10.dp, top = 10.dp),
                    imageResource = event.getCommunityImage() as DrawableResource,
                    contentDescription = event.community!!
                )
            }

            event.country?.let {
                EventFlag(
                    modifier = Modifier.padding(start = 10.dp, bottom = 10.dp),
                    imageResource = event.getCountryImage() as DrawableResource,
                    contentDescription = event.community!!
                )
            }
        }
    }

    @Composable
    private fun EventFlag(modifier: Modifier, imageResource: DrawableResource, contentDescription: String) {
        Image(
            modifier = modifier
                .width(65.dp)
                .border(1.dp, Color.White),
            contentScale = ContentScale.FillWidth,
            painter = painterResource(imageResource),
            contentDescription = contentDescription
        )
    }

    @Composable
    private fun EventOverlayFavorite(viewModel: EventsViewModel, event: WWWEvent, modifier: Modifier = Modifier) {
        var isFavorite by remember { mutableStateOf(event.favorite) }
        val scope = rememberCoroutineScope()

        LaunchedEffect(event.favorite) {
            isFavorite = event.favorite
        }

        Box(modifier = modifier
            .fillMaxSize()
            .padding(end = 10.dp, bottom = 10.dp), contentAlignment = Alignment.BottomEnd) {
            Surface(modifier = Modifier.clip(CircleShape),
                color = MaterialTheme.colorScheme.primary
            ) {
                Image(
                    modifier = Modifier
                        .size(36.dp)
                        .clickable {
                            scope.launch {
                                isFavorite = !isFavorite
                                setEventFavorite.call(event, isFavorite)
                                if (starredSelected) { // Refresh the list
                                    viewModel.filterFavoriteEvents()
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
    private fun EventLocationAndDate(event: WWWEvent, modifier: Modifier = Modifier) {
        val eventDate = event.getFormattedSimpleDate()

        Box(modifier = modifier) {
            Column {

                // Location and date
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = event.location.uppercase(),
                        style = TextStyle(
                            color = extendedLight.quinary.color,
                            fontFamily = MaterialTheme.typography.bodyMedium.fontFamily,
                            fontSize = 28.sp
                        )
                    )
                    Text(
                        text = eventDate,
                        modifier = Modifier.padding(end = 2.dp),
                        style = TextStyle(
                            color = MaterialTheme.colorScheme.primary,
                            fontFamily = MaterialTheme.typography.bodyMedium.fontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 34.sp
                        )
                    )
                }

                // Country if present
                Text(
                    text = event.country?.lowercase()?.replaceFirstChar(Char::titlecaseChar) ?: "",
                    style = TextStyle(
                        color = extendedLight.quinary.color,
                        fontFamily = MaterialTheme.typography.bodyMedium.fontFamily,
                        fontSize = 14.sp
                    ),
                    modifier = Modifier
                        .offset(y = (-8).dp)
                        .padding(start = 2.dp)
                )
            }
        }
    }
}
