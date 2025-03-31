package com.worldwidewaves.activities.utils

import android.content.Context
import com.worldwidewaves.compose.map.AndroidEventMap
import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.toMapLibrePolygon
import com.worldwidewaves.viewmodels.WaveViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

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

/**
 * Coordination of map, event and viewmodel during wave observation
 */
class WaveObserver(
    private val context: Context,
    private val scope: CoroutineScope,
    private val eventMap: AndroidEventMap?,
    private val event: IWWWEvent?,
    private val waveViewModel: WaveViewModel
) {

    fun startObservation() {
        eventMap?.let { eventMap ->
            event?.let { event ->
                scope.launch {
                    if (event.isRunning()) {
                        waveViewModel.startObservation(event) { wavePolygons, clearPolygons ->
                            eventMap.updateWavePolygons(context, wavePolygons, clearPolygons)
                        }
                    } else {
                        waveViewModel.startObservation(event)
                        if (event.isDone()) {
                            eventMap.updateWavePolygons(
                                context,
                                event.area.getPolygons().map { it.toMapLibrePolygon() },
                                true
                            )
                        }
                    }
                }
            }
        }
    }

    fun stopObservation() {
        waveViewModel.stopObservation()
    }

}