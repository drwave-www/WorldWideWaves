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
import com.worldwidewaves.shared.WWWGlobals
import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.map.AbstractEventMap
import com.worldwidewaves.shared.utils.Log
import kotlinx.coroutines.delay

/**
 * Shared map zoom and location update component.
 * Automatically targets the user and wave when the user enters the event area.
 * Continuously updates camera to show user+wave as wave progresses.
 * Throttling: 1s when event running (wave moving), 5s when event done (wave stationary, track user only).
 * Works with any AbstractEventMap implementation (Android, iOS).
 */
@Composable
fun MapZoomAndLocationUpdate(
    event: IWWWEvent,
    eventMap: AbstractEventMap<*>?,
) {
    val isInArea by event.observer.userIsInArea.collectAsState()

    // Time-based throttling: Dynamic interval based on event status (real time, not simulated)
    // 1s when running (wave moving), 5s when done (wave stationary, only track user position)
    LaunchedEffect(isInArea) {
        if (!isInArea || eventMap == null) {
            Log.d("MapZoomAndLocationUpdate", "LaunchedEffect: isInArea=$isInArea, eventMap=$eventMap - not starting loop")
            return@LaunchedEffect
        }

        Log.d("MapZoomAndLocationUpdate", "LaunchedEffect: Starting for event ${event.id}")

        // Initial update when entering area
        Log.d(
            "MapZoomAndLocationUpdate",
            "Initial targetUserAndWave() for event: ${event.id} (entered area)",
        )

        try {
            eventMap.targetUserAndWave()
            Log.d("MapZoomAndLocationUpdate", "Initial targetUserAndWave() completed")
        } catch (e: Exception) {
            Log.e("MapZoomAndLocationUpdate", "Initial targetUserAndWave() failed", e)
        }

        // Continuous updates with dynamic throttle based on event status
        Log.d("MapZoomAndLocationUpdate", "Starting continuous loop for event ${event.id}")
        while (isInArea) {
            val isDone = event.isDone()
            val intervalMs =
                if (isDone) {
                    WWWGlobals.Timing.MAP_CAMERA_UPDATE_INTERVAL_DONE_MS
                } else {
                    WWWGlobals.Timing.MAP_CAMERA_UPDATE_INTERVAL_MS
                }

            delay(intervalMs.toLong())
            val intervalSeconds = intervalMs / WWWGlobals.Timing.MILLIS_PER_SECOND
            Log.v(
                "MapZoomAndLocationUpdate",
                "Periodic targetUserAndWave() for event: ${event.id} (${intervalSeconds}s throttle, done=$isDone)",
            )

            try {
                eventMap.targetUserAndWave()
                Log.v("MapZoomAndLocationUpdate", "Periodic targetUserAndWave() completed")
            } catch (e: Exception) {
                Log.e("MapZoomAndLocationUpdate", "Periodic targetUserAndWave() failed", e)
            }
        }

        Log.d("MapZoomAndLocationUpdate", "Loop exited for event ${event.id} (isInArea=$isInArea)")
    }
}

/**
 * Displays wave polygons on map WITHOUT camera movement.
 * Used in EventDetailScreen where map shows wave progression but stays on static BOUNDS view.
 * Similar to MapZoomAndLocationUpdate but only renders polygons (no auto-follow).
 *
 * Note: Parameters are unused but kept for API consistency with MapZoomAndLocationUpdate.
 * The AbstractEventMap automatically observes event.wave.polygonSets and renders them.
 */
@Suppress("UnusedParameter")
@Composable
fun MapPolygonDisplay(
    event: IWWWEvent,
    eventMap: AbstractEventMap<*>?,
) {
    // No camera movement - just let the wave observer update polygons via eventMap
    // The AbstractEventMap already observes event.wave.polygonSets and renders them
    // This is a placeholder to document the intentional lack of camera tracking
    // Camera remains on initial BOUNDS position set during map initialization
}
