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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.worldwidewaves.shared.MokoRes
import com.worldwidewaves.shared.WWWGlobals.EventsList
import com.worldwidewaves.shared.ui.theme.sharedCommonTextStyle
import com.worldwidewaves.shared.ui.theme.sharedExtendedLight
import com.worldwidewaves.shared.ui.utils.focusIndicator
import dev.icerock.moko.resources.compose.stringResource

/**
 * Three-segment control that lets the user switch between All / Favorites /
 * Downloaded filters. Visually implemented with a rounded container and
 * three equal-width clickable boxes.
 */
@Composable
fun FavoritesSelector(
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
    val isSelected = fontWeight == FontWeight.Bold
    Box(
        modifier =
            modifier
                .clip(RoundedCornerShape(EventsList.SELECTOR_ROUND.dp))
                .height(EventsList.SELECTOR_HEIGHT.dp)
                .background(backgroundColor)
                .focusIndicator()
                .clickable { onClick() }
                .semantics {
                    role = Role.Tab
                    contentDescription = text
                    selected = isSelected
                    stateDescription = if (isSelected) "Selected" else "Not selected"
                },
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
