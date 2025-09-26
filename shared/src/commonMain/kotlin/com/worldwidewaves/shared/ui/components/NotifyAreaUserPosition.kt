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

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.worldwidewaves.shared.MokoRes
import com.worldwidewaves.shared.WWWGlobals.Dimensions
import com.worldwidewaves.shared.WWWGlobals.Event
import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.events.utils.IClock
import com.worldwidewaves.shared.utils.Log
import dev.icerock.moko.resources.compose.stringResource
import kotlin.time.ExperimentalTime

/**
 * Shared NotifyAreaUserPosition component - EXACT replica of original Android implementation.
 * Uses platform abstractions for text formatting while maintaining identical behavior.
 * Works identically on both Android and iOS platforms.
 */
@OptIn(ExperimentalTime::class)
@Composable
fun NotifyAreaUserPosition(
    event: IWWWEvent,
    modifier: Modifier = Modifier,
) {
    val isInArea by event.observer.userIsInArea.collectAsState()
    val hitDateTime by event.observer.hitDateTime.collectAsState()

    val formattedTime = remember(hitDateTime) {
        try {
            IClock.instantToLiteral(hitDateTime, event.getTZ())
        } catch (e: Exception) {
            Log.w("NotifyAreaUserPosition", "Failed to format hit time", e)
            ""
        }
    }

    val geolocText = if (isInArea) {
        stringResource(MokoRes.strings.geoloc_yourein_at, formattedTime)
    } else {
        stringResource(MokoRes.strings.geoloc_yourenotin)
    }

    // Simplified text display for cross-platform compatibility
    val displayText = geolocText

    Row(
        modifier = modifier
            .height(Event.GEOLOCME_HEIGHT.dp)
            .padding(
                start = Dimensions.DEFAULT_EXT_PADDING.dp,
                end = Dimensions.DEFAULT_EXT_PADDING.dp
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .border(Event.GEOLOCME_BORDER.dp, MaterialTheme.colorScheme.primary)
                .fillMaxHeight()
                .weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = displayText,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = Event.GEOLOCME_FONTSIZE.sp,
                    color = Color(0xFFE0E0E0) // quinaryLight equivalent
                ),
            )
        }
    }
}