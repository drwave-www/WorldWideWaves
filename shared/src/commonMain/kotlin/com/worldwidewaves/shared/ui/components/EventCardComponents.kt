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

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
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
import com.worldwidewaves.shared.ui.theme.sharedPrimaryColoredBoldTextStyle
import com.worldwidewaves.shared.ui.theme.sharedQuaternaryColoredTextStyle
import com.worldwidewaves.shared.ui.theme.sharedQuinaryColoredTextStyle
import com.worldwidewaves.shared.ui.utils.focusIndicator
import com.worldwidewaves.shared.utils.Log
import dev.icerock.moko.resources.compose.stringResource
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import kotlin.time.ExperimentalTime

/**
 * Event flag component for country/community images.
 */
@Composable
fun EventFlag(
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
 * Map downloaded indicator overlay.
 */
@Composable
fun EventOverlayMapDownloaded(
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
            Box(
                modifier =
                    Modifier
                        .size(48.dp)
                        .clickable {
                            // NOTE: Map uninstall dialog implementation pending
                        },
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    modifier = Modifier.size(EventsList.MAPDL_IMAGE_SIZE.dp),
                    painter = painterResource(Res.drawable.downloaded_icon),
                    contentDescription = stringResource(MokoRes.strings.map_downloaded),
                )
            }
        }
    }
}

/**
 * Favorite toggle overlay with state management.
 */
@Composable
fun EventOverlayFavorite(
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
        Box(
            modifier =
                Modifier
                    .size(48.dp)
                    .focusIndicator()
                    .clickable {
                        setEventFavorite?.let {
                            pendingFavoriteToggle = true
                        }
                    }.semantics {
                        role = Role.Checkbox
                        contentDescription = "Favorite"
                        toggleableState = if (isFavorite) ToggleableState.On else ToggleableState.Off
                        stateDescription = if (isFavorite) "Favorited" else "Not favorited"
                    },
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier.clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
            ) {
                Image(
                    modifier = Modifier.size(EventsList.FAVS_IMAGE_SIZE.dp),
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
}

/**
 * Displays event location, date and "country / community" using
 * localized date formatting.
 */
@Composable
@OptIn(ExperimentalTime::class)
fun EventLocationAndDate(
    event: IWWWEvent,
    modifier: Modifier = Modifier,
) {
    val eventDate =
        remember(event.id) {
            try {
                DateTimeFormats.dayMonth(event.getStartDateTime(), event.getTZ())
            } catch (e: Exception) {
                Log.w("EventLocationAndDate", "Failed to format event date for event ${event.id}", e)
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
