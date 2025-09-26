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

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.worldwidewaves.shared.WWWGlobals.Dimensions
import com.worldwidewaves.shared.WWWGlobals.Event
import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.ui.theme.sharedQuinaryColoredBoldTextStyle
import dev.icerock.moko.resources.compose.stringResource

/**
 * Shared EventDescription component - EXACT replica of original Android implementation.
 * Uses platform abstractions for styling while maintaining identical layout and behavior.
 * Works identically on both Android and iOS platforms.
 */
@Composable
fun EventDescription(
    event: IWWWEvent,
    modifier: Modifier = Modifier,
) {
    val dir = LocalLayoutDirection.current
    Text(
        modifier = modifier.padding(horizontal = Dimensions.DEFAULT_EXT_PADDING.dp),
        text = stringResource(event.getDescription()),
        style = sharedQuinaryColoredBoldTextStyle(Event.DESC_FONTSIZE),
        textAlign = if (dir == LayoutDirection.Rtl) TextAlign.Start else TextAlign.Justify,
    )
}
