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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.map.AbstractEventMap
import kotlin.math.floor

/**
 * Shared map zoom and location update component.
 * Automatically targets the user and wave when the user enters the event area.
 * Continuously updates camera to show user+wave as wave progresses (throttled to every 5% progression).
 * Works with any AbstractEventMap implementation (Android, iOS).
 */
@Composable
fun MapZoomAndLocationUpdate(
    event: IWWWEvent,
    eventMap: AbstractEventMap<*>?,
) {
    val isInArea by event.observer.userIsInArea.collectAsState()
    val progression by event.observer.progression.collectAsState()

    // Throttle progression to every 5 percentage points (avoids animation spam)
    // This gives ~20 camera updates over entire wave duration (reasonable for smooth tracking)
    val throttledProgression = remember(progression) { (floor(progression / 5.0) * 5.0).toInt() }

    // Trigger targetUserAndWave when entering area OR every 5% progression increase
    LaunchedEffect(isInArea, throttledProgression) {
        if (isInArea && eventMap != null) {
            com.worldwidewaves.shared.utils.Log.i(
                "MapZoomAndLocationUpdate",
                "Calling targetUserAndWave() for event: ${event.id} (progression=$progression, throttled=$throttledProgression%)",
            )
            eventMap.targetUserAndWave()
        }
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
