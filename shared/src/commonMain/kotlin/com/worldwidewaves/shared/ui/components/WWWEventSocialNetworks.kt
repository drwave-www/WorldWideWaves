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

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.worldwidewaves.shared.events.IWWWEvent

/**
 * Shared WWWEventSocialNetworks component - EXACT replica of original Android implementation.
 * Delegates to existing WWWSocialNetworks with event-specific properties.
 * Works identically on both Android and iOS platforms.
 */
@Composable
fun WWWEventSocialNetworks(
    event: IWWWEvent,
    modifier: Modifier = Modifier,
    onUrlOpen: (String) -> Unit = {},
) {
    WWWSocialNetworks(
        modifier = modifier,
        instagramAccount = event.instagramAccount,
        instagramHashtag = event.instagramHashtag,
        onUrlOpen = onUrlOpen,
    )
}
