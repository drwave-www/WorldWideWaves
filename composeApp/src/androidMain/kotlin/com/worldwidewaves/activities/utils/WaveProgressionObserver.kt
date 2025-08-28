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
import com.worldwidewaves.shared.events.WWWEventWave.WaveMode
import com.worldwidewaves.shared.toMapLibrePolygon
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


/**
 * Coordination of map, event and viewmodel during wave observation
 */
class WaveProgressionObserver(
    private val context: Context,
    private val scope: CoroutineScope,
    private val eventMap: AndroidEventMap?,
    private val event: IWWWEvent?,
    private val observerId: String // Stable ID kept for future usage
) {

    private var statusJob: Job? = null
    private var polygonsJob: Job? = null

    fun startObservation() {
        val eventMap = eventMap ?: return
        val event = event ?: return

        scope.launch {
            // Initial observation setup
            // Initial observation setup
            when {
                event.isRunning() -> startPolygonsObservation(event, eventMap)
                event.isDone() -> addFullWavePolygons(event, eventMap)
                else -> { /* wait for RUNNING status */ }
            }

            // Observe status changes
            startStatusObservation(event, eventMap)
        }
    }

    /**
     * Start collecting progression to update polygons on the map with a small rate-limit.
     */
    private fun startPolygonsObservation(event: IWWWEvent, eventMap: AndroidEventMap) {
        polygonsJob?.cancel()

        val updateIntervalMs = 250L
        var lastUpdateTime = 0L

        polygonsJob = scope.launch(Dispatchers.Default) {
            event.observer.progression.collect {
                val now = System.currentTimeMillis()
                if (now - lastUpdateTime >= updateIntervalMs) {
                    lastUpdateTime = now
                    updateWavePolygons(event, eventMap)
                }
            }
        }
    }

    private fun addFullWavePolygons(event: IWWWEvent, eventMap: AndroidEventMap) {
        eventMap.mapLibreAdapter.onMapSet { mapLibre ->
            scope.launch(Dispatchers.Main) {
                mapLibre.addWavePolygons(
                    event.area.getPolygons().map { it.toMapLibrePolygon() },
                    true
                )
            }
        }
    }

    private fun startStatusObservation(event: IWWWEvent, eventMap: AndroidEventMap) {
        statusJob?.cancel()
        statusJob = scope.launch(Dispatchers.Default) {
            event.observer.eventStatus
                .collect { status ->
                    when (status) {
                        IWWWEvent.Status.RUNNING -> startPolygonsObservation(event, eventMap)
                        IWWWEvent.Status.DONE -> addFullWavePolygons(event, eventMap)
                        else -> { /* No-op */ }
                    }
                }
        }
    }

    fun stopObservation() {
        pauseObservation()
    }

    /**
     * Temporarily pause observation without fully removing the ViewModel observer.
     *
     * This keeps all StateFlow instances alive (so the UI still holds the same
     * reference) but stops the coroutine that was listening to status changes.
     * Call {@link startObservation} to resume.
     */
    fun pauseObservation() {
        statusJob?.cancel()
        statusJob = null

        polygonsJob?.cancel()
        polygonsJob = null
    }

    /**
     * Compute and push wave polygons to the map.
     */
    private suspend fun updateWavePolygons(event: IWWWEvent, eventMap: AndroidEventMap) {
        if (!event.isRunning() && !event.isDone()) return

        val polygons = withContext(Dispatchers.Default) {
            event.wave
                .getWavePolygons(null, WaveMode.ADD)
                ?.traversedPolygons
                ?.map { it.toMapLibrePolygon() }
                ?: emptyList()
        }

        eventMap.updateWavePolygons(context, polygons, true)
    }
}