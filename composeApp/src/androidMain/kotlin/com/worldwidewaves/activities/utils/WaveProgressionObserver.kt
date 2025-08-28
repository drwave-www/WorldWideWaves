/*
 * Copyright 2025 DrWave
 *
 * WorldWideWaves is an ephemeral mobile app designed to orchestrate human waves through cities and
 * countries, culminating in a global wave. The project aims to transcend physical and cultural
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
package com.worldwidewaves.activities.utils

import android.content.Context
import com.worldwidewaves.compose.map.AndroidEventMap
import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.toMapLibrePolygon
import com.worldwidewaves.viewmodels.WaveViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch

/**
 * Coordination of map, event and viewmodel during wave observation
 */
class WaveProgressionObserver(
    private val context: Context,
    private val scope: CoroutineScope,
    private val eventMap: AndroidEventMap?,
    private val event: IWWWEvent?,
    private val waveViewModel: WaveViewModel,
    private val observerId: String // Add observer ID
) {

    private var statusJob: Job? = null

    fun startObservation() {
        val eventMap = eventMap ?: return
        val event = event ?: return

        scope.launch {
            // Initial observation setup
            when {
                event.isRunning() -> startObservationWithPolygons(event, eventMap)
                event.isDone() -> {
                    waveViewModel.startObservation(observerId, event)
                    addFullWavePolygons(event, eventMap)
                }
                else -> waveViewModel.startObservation(observerId, event)
            }

            // Set initial user location
            eventMap.locationProvider.currentLocation
                .filterNotNull()
                .take(1)
                .collect { location ->
                    waveViewModel.updateUserLocation(observerId, location)
                }

            // Observe status changes
            startStatusObservation(event, eventMap)
        }
    }

    private fun startObservationWithPolygons(event: IWWWEvent, eventMap: AndroidEventMap) {
        waveViewModel.startObservation(observerId, event) { wavePolygons, clearPolygons ->
            eventMap.updateWavePolygons(context, wavePolygons, clearPolygons)
        }
    }

    private fun addFullWavePolygons(event: IWWWEvent, eventMap: AndroidEventMap) {
        eventMap.mapLibreAdapter.onMapSet { mapLibre ->
            scope.launch {
                mapLibre.addWavePolygons(
                    event.area.getPolygons().map { it.toMapLibrePolygon() },
                    true
                )
            }
        }
    }

    private fun startStatusObservation(event: IWWWEvent, eventMap: AndroidEventMap) {
        statusJob?.cancel()
        statusJob = scope.launch {
            event.eventStatus
                .collect { status ->
                    when (status) {
                        IWWWEvent.Status.RUNNING -> startObservationWithPolygons(event, eventMap)
                        IWWWEvent.Status.DONE -> addFullWavePolygons(event, eventMap)
                        else -> { /* No-op */ }
                    }
                }
        }
    }

    fun stopObservation() {
        waveViewModel.stopObservation(observerId)
        statusJob?.cancel()
        statusJob = null
    }
}