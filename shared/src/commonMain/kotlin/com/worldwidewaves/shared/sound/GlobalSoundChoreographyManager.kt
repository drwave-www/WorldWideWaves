package com.worldwidewaves.shared.sound

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

import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.events.WWWEvents
import com.worldwidewaves.shared.events.utils.CoroutineScopeProvider
import com.worldwidewaves.shared.events.utils.DefaultCoroutineScopeProvider
import com.worldwidewaves.shared.events.utils.IClock
import com.worldwidewaves.shared.utils.Log
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.time.ExperimentalTime

/**
 * Global sound choreography manager that handles sound playback throughout the app
 * when the user is in an event area, regardless of which screen is currently displayed.
 *
 * This manager:
 * - Observes user's area status globally for all loaded events
 * - Automatically detects which event the user is currently in
 * - Starts sound choreography when user enters area and event is active
 * - Stops sound choreography when user leaves area or app goes to background
 * - Manages background audio permissions and lifecycle
 */
@OptIn(ExperimentalTime::class)
class GlobalSoundChoreographyManager(
    private val coroutineScopeProvider: CoroutineScopeProvider = DefaultCoroutineScopeProvider(),
) : KoinComponent {
    private val clock: IClock by inject()
    private val events: WWWEvents by inject()
    private var currentEvent: IWWWEvent? = null
    private var observationJob: Job? = null
    private var isActive = false
    private var isObservingAllEvents = false

    companion object {
        private const val TAG = "GlobalSoundChoreography"
    }

    /**
     * Start observing all loaded events for area status and sound choreography.
     * Automatically detects which event the user is currently in.
     */
    fun startObservingAllEvents() {
        // Prevent double initialization
        if (isObservingAllEvents) {
            Log.d(TAG, "Already observing all events, skipping duplicate initialization")
            return
        }

        Log.d(TAG, "Starting global sound choreography observation for all events")

        // Stop any existing observation
        stopObserving()
        isObservingAllEvents = true

        observationJob =
            coroutineScopeProvider.scopeDefault().launch {
                // Get all loaded events
                val allEvents = events.list()
                Log.d(TAG, "Observing ${allEvents.size} events for area status")

                // Monitor all events and find the one where user is in area
                allEvents.forEach { event ->
                    coroutineScopeProvider.scopeDefault().launch {
                        event.observer.userIsInArea.collect { isInArea: Boolean ->
                            Log.d(TAG, "Event ${event.id}: User area status changed: isInArea=$isInArea")

                            if (isInArea && currentEvent != event) {
                                // User entered a new event area
                                if (currentEvent != null) {
                                    Log.d(TAG, "Switching from event ${currentEvent!!.id} to ${event.id}")
                                    stopSoundChoreography()
                                }
                                currentEvent = event
                                startSoundChoreography(event)
                            } else if (!isInArea && currentEvent == event) {
                                // User left the current event area
                                Log.d(TAG, "User left event area: ${event.id}")
                                stopSoundChoreography()
                                currentEvent = null
                            }
                        }
                    }
                }
            }
    }

    /**
     * Start observing the given event for area status and sound choreography.
     * Only one event can be observed at a time.
     */
    fun startObserving(event: IWWWEvent) {
        Log.d(TAG, "Starting global sound choreography observation for event: ${event.id}")

        // Stop any existing observation
        stopObserving()

        currentEvent = event

        observationJob =
            coroutineScopeProvider.scopeDefault().launch {
                // Observe user's area status
                event.observer.userIsInArea.collect { isInArea ->
                    Log.d(TAG, "User area status changed: isInArea=$isInArea for event=${event.id}")

                    if (isInArea && !isActive) {
                        startSoundChoreography(event)
                    } else if (!isInArea && isActive) {
                        stopSoundChoreography()
                    }
                }
            }
    }

    /**
     * Stop observing sound choreography and clean up resources.
     */
    fun stopObserving() {
        Log.d(TAG, "Stopping global sound choreography observation")

        observationJob?.cancel()
        observationJob = null
        isObservingAllEvents = false

        if (isActive) {
            stopSoundChoreography()
        }

        currentEvent = null
    }

    /**
     * Start sound choreography for the current event.
     */
    private suspend fun startSoundChoreography(event: IWWWEvent) {
        Log.i(TAG, "Starting sound choreography for event: ${event.id}")
        isActive = true

        // Start observing wave hits and play sound when hit
        coroutineScopeProvider.scopeDefault().launch {
            event.observer.userHasBeenHit.collect { hasBeenHit ->
                if (hasBeenHit && isActive) {
                    try {
                        val note = event.warming.playCurrentSoundChoreographyTone()
                        Log.d(TAG, "Played sound choreography tone: $note")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error playing sound choreography tone", e)
                    }
                }
            }
        }
    }

    /**
     * Stop sound choreography and clean up audio resources.
     */
    private fun stopSoundChoreography() {
        Log.i(TAG, "Stopping sound choreography")
        isActive = false
    }

    /**
     * Pause sound choreography (e.g., when app goes to background).
     */
    fun pause() {
        Log.d(TAG, "Pausing sound choreography")
        if (isActive) {
            stopSoundChoreography()
        }
    }

    /**
     * Resume sound choreography (e.g., when app comes to foreground).
     */
    fun resume() {
        Log.d(TAG, "Resuming sound choreography")
        currentEvent?.let { event ->
            coroutineScopeProvider.scopeDefault().launch {
                // Check if user is still in area
                val isInArea = event.observer.userIsInArea.value
                if (isInArea && !isActive) {
                    startSoundChoreography(event)
                }
            }
        }
    }

    /**
     * Check if sound choreography is currently active.
     */
    fun isChoreographyActive(): Boolean = isActive
}
