package com.worldwidewaves.shared.ui.components

/* * Copyright 2025 DrWave
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
 * limitations under the License. */

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import com.worldwidewaves.shared.MokoRes
import com.worldwidewaves.shared.WWWGlobals.EventsList
import com.worldwidewaves.shared.data.SetEventFavorite
import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.generated.resources.Res
import com.worldwidewaves.shared.generated.resources.favorite_off
import com.worldwidewaves.shared.generated.resources.favorite_on
import com.worldwidewaves.shared.ui.utils.focusIndicator
import com.worldwidewaves.shared.utils.Log
import dev.icerock.moko.resources.compose.stringResource
import org.jetbrains.compose.resources.painterResource

/**
 * Favorite button designed for title bars.
 * Displays a circular favorite toggle button with accessibility support.
 *
 * iOS Safety: No DI access in @Composable - setEventFavorite passed as parameter.
 *
 * @param event The event to favorite/unfavorite
 * @param setEventFavorite The favorite setter (from DI, passed as parameter)
 * @param onFavoriteChanged Callback when favorite state changes
 * @param modifier Modifier for the button
 */
@Composable
fun TitleBarFavoriteButton(
    event: IWWWEvent,
    setEventFavorite: SetEventFavorite?,
    onFavoriteChanged: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var isFavorite by remember { mutableStateOf(event.favorite) }
    var pendingFavoriteToggle by remember { mutableStateOf(false) }

    // Handle favorite toggle
    LaunchedEffect(pendingFavoriteToggle) {
        if (pendingFavoriteToggle) {
            Log.d("TitleBarFavoriteButton", "pendingFavoriteToggle=true, processing...")
            setEventFavorite?.let { favoriteSetter ->
                try {
                    isFavorite = !isFavorite
                    Log.i("TitleBarFavoriteButton", "Calling setEventFavorite for ${event.id}: $isFavorite")
                    favoriteSetter.call(event, isFavorite)
                    Log.i("TitleBarFavoriteButton", "Calling onFavoriteChanged callback")
                    onFavoriteChanged()
                    Log.i("TitleBarFavoriteButton", "Favorite toggled for ${event.id}: $isFavorite")
                } catch (e: Exception) {
                    // Revert on error
                    isFavorite = !isFavorite
                    Log.e("TitleBarFavoriteButton", "Failed to toggle favorite for ${event.id}", e)
                }
            } ?: Log.w("TitleBarFavoriteButton", "setEventFavorite is null!")
            pendingFavoriteToggle = false
        }
    }

    // Sync with event favorite state changes
    LaunchedEffect(event.favorite) {
        isFavorite = event.favorite
    }

    val favoriteContentDesc = stringResource(MokoRes.strings.accessibility_favorite_button)
    val favoritedStateDesc = stringResource(MokoRes.strings.accessibility_favorited)
    val notFavoritedStateDesc = stringResource(MokoRes.strings.accessibility_not_favorited)

    Box(
        modifier = modifier.padding(end = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .testTag("TitleBarFavoriteButton_${event.id}")
                    .size(48.dp)
                    .focusIndicator()
                    .clickable {
                        setEventFavorite?.let {
                            pendingFavoriteToggle = true
                        }
                    }.semantics {
                        role = Role.Checkbox
                        contentDescription = favoriteContentDesc
                        toggleableState = if (isFavorite) ToggleableState.On else ToggleableState.Off
                        stateDescription = if (isFavorite) favoritedStateDesc else notFavoritedStateDesc
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
