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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.worldwidewaves.shared.MokoRes
import com.worldwidewaves.shared.WWWGlobals.EventsList
import com.worldwidewaves.shared.data.SetEventFavorite
import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.ui.components.event.EventOverlayDone
import com.worldwidewaves.shared.ui.components.event.EventOverlaySoonOrRunning
import com.worldwidewaves.shared.ui.utils.focusIndicator
import dev.icerock.moko.resources.compose.stringResource
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/**
 * Individual event card component displaying event image, overlays, and location/date info.
 */
@Composable
fun EventCard(
    event: IWWWEvent,
    isMapInstalled: Boolean,
    starredSelected: Boolean,
    onEventClick: (String) -> Unit,
    setEventFavorite: SetEventFavorite?,
    onFavoriteChanged: () -> Unit = {},
    onMapUninstallRequested: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val eventLocation = stringResource(event.getLocation())
    val accessibilityDescription = stringResource(MokoRes.strings.accessibility_event_in, eventLocation)
    Column(
        modifier =
            modifier
                .testTag("Event_${event.id}")
                .focusIndicator()
                .clickable {
                    onEventClick(event.id)
                }.semantics {
                    role = Role.Button
                    contentDescription = accessibilityDescription
                },
    ) {
        EventOverlay(
            event,
            isMapInstalled,
            starredSelected,
            setEventFavorite,
            onFavoriteChanged,
            onMapUninstallRequested,
        )
        EventLocationAndDate(event)
    }
}

/**
 * Event overlay containing background image, flags, status badges, and favorite/download indicators.
 */
@Composable
private fun EventOverlay(
    event: IWWWEvent,
    isMapInstalled: Boolean,
    starredSelected: Boolean,
    setEventFavorite: SetEventFavorite?,
    onFavoriteChanged: () -> Unit = {},
    onMapUninstallRequested: (String) -> Unit = {},
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
        EventOverlayMapDownloaded(
            event.id,
            isMapInstalled,
            onMapUninstallClick = { onMapUninstallRequested(event.id) },
        )
        EventOverlayFavorite(event, starredSelected, setEventFavorite, onFavoriteChanged)
    }
}

/**
 * Displays country and community flags on the event overlay.
 */
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
                            start = com.worldwidewaves.shared.WWWGlobals.Dimensions.DEFAULT_INT_PADDING.dp,
                            top = com.worldwidewaves.shared.WWWGlobals.Dimensions.DEFAULT_INT_PADDING.dp,
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
                            start = com.worldwidewaves.shared.WWWGlobals.Dimensions.DEFAULT_INT_PADDING.dp,
                            bottom = com.worldwidewaves.shared.WWWGlobals.Dimensions.DEFAULT_INT_PADDING.dp,
                        ),
                    imageResource = countryImageResource,
                    contentDescription = event.country!!,
                )
            }
        }
    }
}
