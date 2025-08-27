/*
 * Copyright 2024 DrWave
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

    fun startObservation() {
        eventMap?.let { eventMap ->
            event?.let { event ->
                scope.launch {
                    if (event.isRunning()) {
                        waveViewModel.startObservation(observerId, event) { wavePolygons, clearPolygons ->
                            eventMap.updateWavePolygons(context, wavePolygons, clearPolygons)
                        }
                    } else {
                        waveViewModel.startObservation(observerId, event)
                        if (event.isDone()) {
                            // Set full wave polygons when MapLibre is set
                            eventMap.mapLibreAdapter.onMapSet {
                                scope.launch {
                                    it.addWavePolygons(
                                        event.area.getPolygons().map { it.toMapLibrePolygon() },
                                        true
                                    )
                                }
                            }
                        }
                    }

                    // Set first user location value, later managed by MapLibre
                    eventMap.locationProvider.currentLocation.filterNotNull().take(1).collect { location ->
                        waveViewModel.updateUserLocation(observerId, location)
                    }
                }
            }
        }
    }

    fun stopObservation() {
        waveViewModel.stopObservation(observerId)
    }
}