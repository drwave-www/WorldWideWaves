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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.worldwidewaves.shared.WWWGlobals.Dimensions
import com.worldwidewaves.shared.WWWGlobals.Event
import com.worldwidewaves.shared.events.IWWWEvent.Status
import com.worldwidewaves.shared.ui.theme.platformDateMiter
import com.worldwidewaves.shared.ui.theme.platformDateStroke
import com.worldwidewaves.shared.ui.theme.platformQuinaryLight

/**
 * Shared EventOverlayDate component - EXACT replica of original Android implementation.
 * Uses platform abstractions for complex stroke effects while maintaining identical appearance.
 * Works identically on both Android and iOS platforms.
 */
@Composable
fun SharedEventOverlayDate(
    eventStatus: Status,
    eventDate: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .let {
                if (eventStatus == Status.DONE)
                    it.padding(bottom = Dimensions.DEFAULT_EXT_PADDING.dp)
                else it
            },
        contentAlignment = if (eventStatus == Status.DONE) Alignment.BottomCenter else Alignment.Center,
    ) {
        val textStyle = MaterialTheme.typography.headlineLarge.copy(
            fontSize = Event.DATE_FONTSIZE.sp,
            fontWeight = FontWeight.ExtraBold
        )

        // Background text with quinaryLight color
        Text(
            text = eventDate,
            style = textStyle.copy(color = platformQuinaryLight),
        )

        // Foreground text with stroke effect
        Text(
            text = eventDate,
            style = textStyle.copy(
                color = MaterialTheme.colorScheme.primary,
                drawStyle = Stroke(
                    miter = platformDateMiter,
                    width = platformDateStroke,
                    join = StrokeJoin.Miter,
                ),
            ),
        )
    }
}