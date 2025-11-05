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
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.launch
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
    private val eventMap: AbstractEventMap<*>?,
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

        // Check current status to determine if we should clear polygons
        // For DONE events, don't clear - the full wave area should remain visible
        val currentStatus = event.observer.eventStatus.value

        if (currentStatus != IWWWEvent.Status.DONE) {
            // Clear existing polygons for non-DONE events
            // This ensures clean state when:
            // 1. Simulation starts (all observers restart via simulationChanged)
            // 2. Observer restarts for any reason
            // 3. User stops simulation and returns to real-time observation
            lastWavePolygons = emptyList()
            eventMap?.updateWavePolygons(emptyList(), clearPolygons = true)
            Log.d("WaveObserver", "Cleared wave polygons for event ${event.id} on observation start (status: $currentStatus)")
        }

        scope.launch {
            // Initial observation setup
            when {
                event.isRunning() -> {
                    startPolygonsObservation(event, eventMap)
                }
                event.isDone() -> {
                    // Event is already done - show polygons once and don't observe progression
                    // Polygons were NOT cleared above, so full wave area will be visible immediately
                    addFullWavePolygons(event, eventMap)
                    Log.d("WaveObserver", "Event already DONE - showing final polygons without observation")
                }
                else -> {
                    // Event not started yet - wait for status changes
                    Log.d("WaveObserver", "Event ${event.id} not started - waiting for RUNNING status")
                }
            }

            // Observe status changes
            startStatusObservation(event, eventMap)
        }
    }

    /**
     * Start collecting progression to update polygons on the map with a small rate-limit.
     */
    @OptIn(FlowPreview::class)
    private fun startPolygonsObservation(
        event: IWWWEvent,
        eventMap: AbstractEventMap<*>?,
    ) {
        polygonsJob?.cancel()

        // Use Flow.sample() for efficient throttling instead of manual time tracking
        polygonsJob =
            scope.launch {
                event.observer.progression
                    .sample(250.milliseconds) // Built-in throttling for better performance
                    .collect {
                        updateWavePolygons(event, eventMap)
                    }
            }
    }

    private fun addFullWavePolygons(
        event: IWWWEvent,
        eventMap: AbstractEventMap<*>?,
    ) {
        eventMap?.mapLibreAdapter?.onMapSet { mapLibre ->
            scope.launch {
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
        eventMap: AbstractEventMap<*>?,
    ) {
        statusJob?.cancel()
        statusJob =
            scope.launch {
                event.observer.eventStatus
                    .collect { status ->
                        when (status) {
                            IWWWEvent.Status.RUNNING -> startPolygonsObservation(event, eventMap)
                            IWWWEvent.Status.DONE -> {
                                // Stop polygon observation to prevent continuous updates
                                polygonsJob?.cancel()
                                polygonsJob = null
                                // Clear progressive wave polygons before showing full area
                                lastWavePolygons = emptyList()
                                eventMap?.updateWavePolygons(emptyList(), clearPolygons = true)
                                Log.d("WaveObserver", "Event ${event.id} DONE - clearing progressive polygons")
                                // Show full wave polygons once
                                addFullWavePolygons(event, eventMap)
                            }
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
        eventMap: AbstractEventMap<*>?,
    ) {
        if (!event.isRunning() && !event.isDone()) return

        val polygons =
            event.wave
                .getWavePolygons()
                ?.traversedPolygons
                ?: emptyList()

        if (polygons.isEmpty()) {
            if (lastWavePolygons.isNotEmpty()) {
                // Keep displaying the previous frame to avoid flicker when the
                // shared layer temporarily returns an empty list.
                Log.v("WaveObserver", "📊 Empty polygons, keeping last frame: ${lastWavePolygons.size} polygons")
                eventMap?.updateWavePolygons(lastWavePolygons, false)
            }
            return
        } else {
            lastWavePolygons = polygons
            // Log at VERBOSE level to reduce production log volume (called every 250ms)
            Log.v("WaveObserver", "📊 Updating wave polygons: ${polygons.size} polygons")
            eventMap?.updateWavePolygons(polygons, true)
            Log.v("WaveObserver", "✅ updateWavePolygons called successfully")
        }
    }
}
