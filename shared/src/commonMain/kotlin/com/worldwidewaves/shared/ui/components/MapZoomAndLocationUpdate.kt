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

package com.worldwidewaves.shared.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.map.AbstractEventMap

/**
 * Shared map zoom and location update component.
 * Automatically targets the user and wave when the user enters the event area.
 * Continuously updates camera to show user+wave as wave progresses (with throttling).
 * Works with any AbstractEventMap implementation (Android, iOS).
 */
@Composable
fun MapZoomAndLocationUpdate(
    event: IWWWEvent,
    eventMap: AbstractEventMap<*>?,
) {
    val isInArea by event.observer.userIsInArea.collectAsState()
    val progression by event.observer.progression.collectAsState()

    com.worldwidewaves.shared.utils.Log.d(
        "MapZoomAndLocationUpdate",
        "Composed for event: ${event.id}, isInArea=$isInArea, progression=$progression, eventMap=${eventMap != null}",
    )

    // Trigger targetUserAndWave when entering area OR when progression changes
    // Progression changes trigger camera updates to track the moving wave
    LaunchedEffect(isInArea, progression) {
        com.worldwidewaves.shared.utils.Log.i(
            "MapZoomAndLocationUpdate",
            "LaunchedEffect triggered for event: ${event.id}, isInArea=$isInArea, progression=$progression",
        )
        if (isInArea && eventMap != null) {
            com.worldwidewaves.shared.utils.Log.i(
                "MapZoomAndLocationUpdate",
                "Calling targetUserAndWave() for event: ${event.id} (progression=$progression)",
            )
            eventMap.targetUserAndWave()
        }
    }
}
