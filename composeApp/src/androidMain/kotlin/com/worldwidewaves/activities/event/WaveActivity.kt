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
@file:OptIn(ExperimentalTime::class)

package com.worldwidewaves.activities.event

import com.worldwidewaves.shared.ui.activities.WaveParticipationScreen
import com.worldwidewaves.utils.AndroidPlatformEnabler
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class WaveActivity : AbstractEventAndroidActivity<WaveParticipationScreen>() {
    override fun createActivityImpl(
        eventId: String,
        platformEnabler: AndroidPlatformEnabler,
    ): WaveParticipationScreen = WaveParticipationScreen(eventId, platformEnabler)

    override fun createEventMapBuilder(): (com.worldwidewaves.shared.events.IWWWEvent) -> com.worldwidewaves.compose.map.AndroidEventMap =
        { event ->
            com.worldwidewaves.compose.map.AndroidEventMap(
                event,
                context = this,
                mapConfig =
                    com.worldwidewaves.shared.map.EventMapConfig(
                        initialCameraPosition = com.worldwidewaves.shared.map.MapCameraPosition.BOUNDS,
                        autoTargetUserOnFirstLocation = false,
                        gesturesEnabled = false, // Wave screen map is non-interactive (display only)
                    ),
            )
            // BOUNDS mode: Shows full event area initially (matches event detail screen)
            // Then targetUserAndWave() from MapZoomAndLocationUpdate takes over when user enters area
            // Map is non-interactive - gestures disabled
        }
}
