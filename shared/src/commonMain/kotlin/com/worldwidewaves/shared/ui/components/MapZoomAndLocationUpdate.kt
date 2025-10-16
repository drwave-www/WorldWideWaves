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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.map.AbstractEventMap
import kotlin.math.floor

/**
 * Shared map zoom and location update component.
 * Automatically targets the user and wave when the user enters the event area.
 * Continuously updates camera to show user+wave as wave progresses (with throttling to 1 update/second).
 * Works with any AbstractEventMap implementation (Android, iOS).
 */
@Composable
fun MapZoomAndLocationUpdate(
    event: IWWWEvent,
    eventMap: AbstractEventMap<*>?,
) {
    val isInArea by event.observer.userIsInArea.collectAsState()
    val progression by event.observer.progression.collectAsState()

    // Throttle progression updates to whole percentage points (avoids 60 FPS animation spam)
    val throttledProgression = remember(progression) { floor(progression).toInt() }

    // Track when camera was last updated to ensure we don't spam animations
    var lastUpdateTime by remember { mutableStateOf(0L) }
    val currentTime = System.currentTimeMillis()

    // Trigger targetUserAndWave when entering area OR when throttled progression changes
    // Throttling to 1-second intervals prevents animation restart loops
    LaunchedEffect(isInArea, throttledProgression) {
        val timeSinceLastUpdate = currentTime - lastUpdateTime

        if (isInArea && eventMap != null && timeSinceLastUpdate >= 1000) {
            com.worldwidewaves.shared.utils.Log.i(
                "MapZoomAndLocationUpdate",
                "Calling targetUserAndWave() for event: ${event.id} (progression=$progression)",
            )
            eventMap.targetUserAndWave()
            lastUpdateTime = currentTime
        }
    }
}
