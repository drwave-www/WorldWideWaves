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
import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration
import kotlin.time.Instant

/**
 * Manages the observation scheduling for WWWEvent instances.
 *
 * This interface abstracts the complex logic of determining when and how often
 * to observe events based on their current state, timing, and user proximity.
 *
 * The ObservationScheduler is responsible for:
 * - Calculating adaptive observation intervals based on event timing
 * - Determining whether an event should be observed continuously
 * - Creating observation flows with proper lifecycle management
 * - Optimizing battery usage through intelligent scheduling
 */
interface ObservationScheduler {
    /**
     * Calculates the appropriate observation interval for an event.
     *
     * The interval is dynamically calculated based on:
     * - Time until event start
     * - Time until user will be hit by the wave
     * - Current event status and phase
     *
     * @param event The event to calculate interval for
     * @return Duration representing the optimal observation interval
     */
    suspend fun calculateObservationInterval(event: IWWWEvent): Duration

    /**
     * Determines if an event should be continuously observed.
     *
     * Continuous observation is enabled when:
     * - Event is currently running
     * - Event is soon and near its start time
     * - User is in critical hit phase
     *
     * @param event The event to check
     * @return True if continuous observation should be active
     */
    suspend fun shouldObserveContinuously(event: IWWWEvent): Boolean

    /**
     * Creates a flow that emits observation triggers at appropriate intervals.
     *
     * The flow will:
     * - Emit at adaptive intervals based on event timing
     * - Stop when the event is done or no longer relevant
     * - Handle lifecycle and cancellation properly
     *
     * @param event The event to create observation flow for
     * @return Flow that emits Unit at observation intervals
     */
    fun createObservationFlow(event: IWWWEvent): Flow<Unit>

    /**
     * Gets the current observation schedule for an event.
     *
     * @param event The event to get schedule for
     * @return ObservationSchedule containing timing and phase information
     */
    suspend fun getObservationSchedule(event: IWWWEvent): ObservationSchedule
}

/**
 * Represents the observation schedule for an event.
 */
data class ObservationSchedule(
    val shouldObserve: Boolean,
    val interval: Duration,
    val phase: ObservationPhase,
    val nextObservationTime: Instant?,
    val reason: String,
)

/**
 * Represents different observation phases with different scheduling requirements.
 */
enum class ObservationPhase {
    /** Event is far in the future (>1 hour) */
    DISTANT,

    /** Event is approaching (5-60 minutes) */
    APPROACHING,

    /** Event is near (35 seconds - 5 minutes) */
    NEAR,

    /** Event is active or about to start (0-35 seconds) */
    ACTIVE,

    /** User is in critical hit phase (<5 seconds) */
    CRITICAL,

    /** Event is completed or irrelevant */
    INACTIVE,
}
