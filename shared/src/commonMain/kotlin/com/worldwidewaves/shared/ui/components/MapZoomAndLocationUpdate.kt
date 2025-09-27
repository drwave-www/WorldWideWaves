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
import androidx.compose.runtime.rememberCoroutineScope
import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.map.AbstractEventMap
import kotlinx.coroutines.launch

/**
 * Shared map zoom and location update component.
 * Automatically targets the user and wave when the user is in the event area.
 * Works with any AbstractEventMap implementation (Android, iOS).
 */
@Composable
fun MapZoomAndLocationUpdate(
    event: IWWWEvent,
    eventMap: AbstractEventMap<*>?,
) {
    val scope = rememberCoroutineScope()
    val progression by event.observer.progression.collectAsState()
    val isInArea by event.observer.userIsInArea.collectAsState()

    LaunchedEffect(progression, isInArea) {
        if (isInArea) {
            scope.launch {
                eventMap?.targetUserAndWave()
            }
        }
    }
}
