@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.worldwidewaves.shared.domain.scheduling

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
import com.worldwidewaves.shared.events.utils.IClock
import com.worldwidewaves.shared.utils.Log
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.INFINITE
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Default implementation of ObservationScheduler.
 *
 * This implementation provides adaptive observation scheduling based on event timing
 * and user proximity, with intelligent battery optimization.
 *
 * ## Observation Intervals (Adaptive):
 * - **> 1 hour**: 1 hour intervals (minimal battery usage)
 * - **5-60 minutes**: 5 minute intervals (moderate monitoring)
 * - **35-300 seconds**: 1 second intervals (active monitoring)
 * - **0-35 seconds**: 500ms intervals (real-time updates)
 * - **Hit critical (< 1s)**: 50ms intervals (sound accuracy)
 * - **5s buffer**: 200ms intervals (battery optimization)
 *
 * ## Battery Optimization Features:
 * - Adaptive intervals minimize CPU wake-ups when event is distant
 * - Infinite interval stops observation when event is no longer relevant
 * - Progressive interval reduction as event approaches
 * - Critical timing accuracy for sound synchronization
 */
class DefaultObservationScheduler(
    private val clock: IClock,
) : ObservationScheduler {
    override suspend fun calculateObservationInterval(event: IWWWEvent): Duration {
        val now = clock.now()
        val eventStartTime = event.getStartDateTime()
        val timeBeforeEvent = eventStartTime - now
        val timeBeforeHit = event.wave.timeBeforeUserHit()

        return when {
            timeBeforeEvent > 1.hours + 5.minutes -> 1.hours
            timeBeforeEvent > 5.minutes + 30.seconds -> 5.minutes
            timeBeforeEvent > 35.seconds -> 1.seconds
            timeBeforeHit != null && timeBeforeHit < ZERO -> INFINITE
            timeBeforeHit != null && timeBeforeHit < 1.seconds -> 50.milliseconds
            timeBeforeHit != null && timeBeforeHit < 5.seconds -> 200.milliseconds
            timeBeforeEvent > 0.seconds || event.isRunning() -> 500.milliseconds
            else -> 30.seconds
        }
    }

    override suspend fun shouldObserveContinuously(event: IWWWEvent): Boolean {
        val shouldObserve = event.isRunning() || (event.isSoon() && event.isNearTime())
        Log.v(
            "DefaultObservationScheduler",
            "Should observe continuously: $shouldObserve " +
                "(running=${event.isRunning()}, soon=${event.isSoon()}, nearTime=${event.isNearTime()})",
        )
        return shouldObserve
    }

    override fun createObservationFlow(event: IWWWEvent): Flow<Unit> =
        callbackFlow {
            Log.v("DefaultObservationScheduler", "Creating observation flow for event ${event.id}")

            try {
                if (shouldObserveContinuously(event)) {
                    Log.v("DefaultObservationScheduler", "Starting continuous observation for event ${event.id}")

                    while (!event.isDone()) {
                        // Emit observation trigger
                        send(Unit)

                        // Calculate next observation interval
                        val observationDelay = calculateObservationInterval(event)

                        if (!observationDelay.isFinite()) {
                            Log.v("DefaultObservationScheduler", "Stopping observation flow due to infinite interval")
                            break
                        }

                        // Wait for next observation
                        clock.delay(observationDelay)
                    }

                    // Final emission when event is done
                    Log.v("DefaultObservationScheduler", "Event ${event.id} done, final observation emission")
                    send(Unit)
                } else {
                    // For events not ready for continuous observation, emit once
                    Log.v("DefaultObservationScheduler", "Event ${event.id} not ready for continuous observation, emitting once")
                    send(Unit)
                }
            } catch (e: Exception) {
                Log.e("DefaultObservationScheduler", "Error in observation flow for event ${event.id}: $e")
            }

            awaitClose {
                Log.v("DefaultObservationScheduler", "Closing observation flow for event ${event.id}")
            }
        }

    override suspend fun getObservationSchedule(event: IWWWEvent): ObservationSchedule {
        val shouldObserve = shouldObserveContinuously(event)
        val interval = calculateObservationInterval(event)
        val phase = determineObservationPhase(event)

        val nextObservationTime =
            if (shouldObserve && interval.isFinite()) {
                clock.now() + interval
            } else {
                null
            }

        val reason = buildReasonString(event, phase, interval)

        return ObservationSchedule(
            shouldObserve = shouldObserve,
            interval = interval,
            phase = phase,
            nextObservationTime = nextObservationTime,
            reason = reason,
        )
    }

    /**
     * Determines the current observation phase for an event.
     */
    private suspend fun determineObservationPhase(event: IWWWEvent): ObservationPhase {
        val now = clock.now()
        val eventStartTime = event.getStartDateTime()
        val timeBeforeEvent = eventStartTime - now
        val timeBeforeHit = event.wave.timeBeforeUserHit()

        return when {
            event.isDone() -> ObservationPhase.INACTIVE
            timeBeforeHit != null && timeBeforeHit < 5.seconds && timeBeforeHit > ZERO -> ObservationPhase.CRITICAL
            timeBeforeEvent <= 35.seconds || event.isRunning() -> ObservationPhase.ACTIVE
            timeBeforeEvent <= 5.minutes + 30.seconds -> ObservationPhase.NEAR
            timeBeforeEvent <= 1.hours + 5.minutes -> ObservationPhase.APPROACHING
            else -> ObservationPhase.DISTANT
        }
    }

    /**
     * Builds a human-readable reason string for the observation schedule.
     */
    private suspend fun buildReasonString(
        event: IWWWEvent,
        phase: ObservationPhase,
        interval: Duration,
    ): String {
        val now = clock.now()
        val eventStartTime = event.getStartDateTime()
        val timeBeforeEvent = eventStartTime - now
        val timeBeforeHit = event.wave.timeBeforeUserHit()

        return when (phase) {
            ObservationPhase.DISTANT -> "Event is $timeBeforeEvent away, minimal monitoring"
            ObservationPhase.APPROACHING -> "Event approaching in $timeBeforeEvent, moderate monitoring"
            ObservationPhase.NEAR -> "Event near ($timeBeforeEvent), active monitoring"
            ObservationPhase.ACTIVE -> "Event active or starting, real-time monitoring"
            ObservationPhase.CRITICAL -> "Critical hit timing ($timeBeforeHit), maximum accuracy"
            ObservationPhase.INACTIVE -> "Event done or irrelevant, no observation needed"
        }
    }
}
