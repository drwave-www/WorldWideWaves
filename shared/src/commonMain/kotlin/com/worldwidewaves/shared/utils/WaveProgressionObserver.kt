/*
 * Copyright 2024 DrWave
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
package com.worldwidewaves.shared.utils

import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.events.utils.Polygon
import com.worldwidewaves.shared.map.AbstractEventMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds

/**
 * Observes a single wave **progression** and mirrors it on the map.
 *
 * Responsibilities:
 * • At start-up it inspects the current event status and either begins a
 *   running-phase polygon stream or instantly draws the final wave outline
 *   (DONE).                                                       <br/>
 * • Listens to [IWWWEvent.observer] so it can switch between the
 *   two behaviours when the status flips from *RUNNING → DONE*.    <br/>
 * • While *RUNNING*, collects the wave's `progression` flow and pushes
 *   traversed polygons to [AbstractEventMap] – throttled to one update every
 *   250 ms to avoid flooding the UI thread.                         <br/>
 * • Persists the last non-empty polygon list so, if the shared logic delivers
 *   an empty set for a short period, the map keeps showing the previous frame
 *   instead of flickering.
 *
 * Public API is intentionally minimal: [startObservation], [pauseObservation]
 * (also aliased by [stopObservation]) and lifecycle-safe internal clean-up.
 */
class WaveProgressionObserver(
    private val scope: CoroutineScope,
    private val eventMap: AbstractEventMap<*>,
    private val event: IWWWEvent?,
) {
    private var statusJob: Job? = null
    private var polygonsJob: Job? = null
    private var lastWavePolygons: List<Polygon> = emptyList()

    /**
     * Entry-point – inspects current state then launches coroutines that will
     * keep the map in sync with the wave until [pauseObservation] is called.
     */
    fun startObservation() {
        val event = event ?: return

        scope.launch {
            // Initial observation setup
            val status = when {
                event.isRunning() -> "RUNNING"
                event.isDone() -> "DONE"
                else -> "OTHER"
            }
            com.worldwidewaves.shared.utils.Log.i("WaveProgressionObserver", "Starting observation for event ${event.id}, status: $status")

            when {
                event.isRunning() -> {
                    com.worldwidewaves.shared.utils.Log.i("WaveProgressionObserver", "Event ${event.id} is RUNNING - starting polygon observation")
                    startPolygonsObservation(event, eventMap)
                }
                event.isDone() -> {
                    com.worldwidewaves.shared.utils.Log.i("WaveProgressionObserver", "Event ${event.id} is DONE - adding full wave polygons")
                    addFullWavePolygons(event, eventMap)
                }
                else -> {
                    com.worldwidewaves.shared.utils.Log.i("WaveProgressionObserver", "Event ${event.id} is not running/done - waiting for status change")
                }
            }

            // Observe status changes
            startStatusObservation(event, eventMap)
        }
    }

    /**
     * Start collecting progression to update polygons on the map with a small rate-limit.
     */
    private fun startPolygonsObservation(
        event: IWWWEvent,
        eventMap: AbstractEventMap<*>,
    ) {
        polygonsJob?.cancel()

        // Use Flow.sample() for efficient throttling instead of manual time tracking
        polygonsJob =
            scope.launch(Dispatchers.Default) {
                event.observer.progression
                    .sample(250.milliseconds) // Built-in throttling for better performance
                    .collect { progression ->
                        com.worldwidewaves.shared.utils.Log.v("WaveProgressionObserver", "Progression update for ${event.id}: $progression")
                        updateWavePolygons(event, eventMap)
                    }
            }
    }

    private fun addFullWavePolygons(
        event: IWWWEvent,
        eventMap: AbstractEventMap<*>,
    ) {
        eventMap.mapLibreAdapter.onMapSet { mapLibre ->
            scope.launch(Dispatchers.Main) {
                // Render all original polygons independently (no holes merge)
                val polygons = event.area.getPolygons()

                mapLibre.addWavePolygons(polygons, true)
            }
        }
    }

    /**
     * Subscribes to the event **status** flow and switches the active polygon
     * coroutine when the wave enters *RUNNING* or *DONE*.
     */
    private fun startStatusObservation(
        event: IWWWEvent,
        eventMap: AbstractEventMap<*>,
    ) {
        statusJob?.cancel()
        statusJob =
            scope.launch(Dispatchers.Default) {
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
    private suspend fun updateWavePolygons(
        event: IWWWEvent,
        eventMap: AbstractEventMap<*>,
    ) {
        if (!event.isRunning() && !event.isDone()) {
            com.worldwidewaves.shared.utils.Log.v("WaveProgressionObserver", "Event ${event.id} not running/done - skipping polygon update")
            return
        }

        val polygons =
            withContext(Dispatchers.Default) {
                event.wave
                    .getWavePolygons()
                    ?.traversedPolygons
                    ?: emptyList()
            }

        com.worldwidewaves.shared.utils.Log.v("WaveProgressionObserver", "Generated ${polygons.size} wave polygons for event ${event.id}")

        if (polygons.isEmpty()) {
            if (lastWavePolygons.isNotEmpty()) {
                // Keep displaying the previous frame to avoid flicker when the
                // shared layer temporarily returns an empty list.
                com.worldwidewaves.shared.utils.Log.v("WaveProgressionObserver", "Using cached polygons (${lastWavePolygons.size}) for event ${event.id}")
                eventMap.updateWavePolygons(lastWavePolygons, false)
            } else {
                com.worldwidewaves.shared.utils.Log.v("WaveProgressionObserver", "No polygons to display for event ${event.id}")
            }
            return
        } else {
            lastWavePolygons = polygons
            com.worldwidewaves.shared.utils.Log.i("WaveProgressionObserver", "Updating map with ${polygons.size} polygons for event ${event.id}")
            eventMap.updateWavePolygons(polygons, true)
        }
    }
}