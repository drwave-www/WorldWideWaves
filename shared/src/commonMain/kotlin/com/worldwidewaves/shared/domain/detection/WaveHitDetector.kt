package com.worldwidewaves.shared.domain.detection

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

import com.worldwidewaves.shared.domain.state.EventState
import com.worldwidewaves.shared.domain.state.EventStateHolder
import com.worldwidewaves.shared.domain.state.EventStateInput
import com.worldwidewaves.shared.domain.state.StateValidationIssue
import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.events.IWWWEvent.Status
import com.worldwidewaves.shared.events.utils.IClock
import com.worldwidewaves.shared.position.PositionManager
import com.worldwidewaves.shared.utils.Log
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.ExperimentalTime

/**
 * Wave hit detection algorithms and state calculation.
 *
 * This class is responsible for:
 * - Calculating event state based on user position and event progression
 * - Determining if/when the user will be hit by the wave
 * - Validating state transitions
 * - Managing hit detection timing
 *
 * ## Detection Accuracy
 * - Wave hit detection accuracy: ±50ms (for sound synchronization)
 * - Position-based hit calculations use geographic distance
 * - Time-based predictions account for wave propagation speed
 *
 * ## Thread Safety
 * All calculation methods are thread-safe and can be called from any dispatcher.
 *
 * ## iOS Safety
 * This class follows iOS safety patterns:
 * - No KoinComponent dependency (pure calculation class)
 * - No coroutine launches in initialization
 * - Stateless calculations (no mutable state)
 */
@OptIn(ExperimentalTime::class)
class WaveHitDetector(
    private val eventStateHolder: EventStateHolder,
    private val clock: IClock,
    private val positionManager: PositionManager,
) {
    /**
     * Calculates the complete event state for the current user position and event progression.
     *
     * @param event The event to calculate state for
     * @param progression The current wave progression (0.0 to 100.0)
     * @param status The current event status
     * @param userIsInArea Whether the user is currently in the event area
     * @return The calculated EventState, or null if calculation fails
     */
    suspend fun calculateEventState(
        event: IWWWEvent,
        progression: Double,
        status: Status,
        userIsInArea: Boolean,
    ): EventState? =
        try {
            val stateInput =
                EventStateInput(
                    progression = progression,
                    status = status,
                    userPosition = positionManager.getCurrentPosition(),
                    currentTime = clock.now(),
                )

            eventStateHolder.calculateEventState(
                event = event,
                input = stateInput,
                userIsInArea = userIsInArea,
            )
        } catch (e: IllegalStateException) {
            Log.e("WaveHitDetector", "State error calculating event state: $e")
            null
        } catch (e: CancellationException) {
            throw e // Re-throw cancellation
        } catch (e: Exception) {
            Log.e("WaveHitDetector", "Unexpected error calculating event state: $e")
            null
        }

    /**
     * Validates the calculated state against the input parameters.
     *
     * @param input The input parameters used for calculation
     * @param calculatedState The calculated state to validate
     * @return List of validation issues, empty if state is valid
     */
    fun validateState(
        input: EventStateInput,
        calculatedState: EventState,
    ): List<StateValidationIssue> =
        try {
            eventStateHolder.validateState(input, calculatedState)
        } catch (e: IllegalStateException) {
            Log.e("WaveHitDetector", "State error validating state: $e")
            emptyList()
        } catch (e: Exception) {
            Log.e("WaveHitDetector", "Unexpected error validating state: $e")
            emptyList()
        }

    /**
     * Validates a state transition from one state to another.
     *
     * @param currentState The current event state (can be null for initial state)
     * @param newState The new state to transition to
     * @return List of validation issues, empty if transition is valid
     */
    fun validateStateTransition(
        currentState: EventState?,
        newState: EventState,
    ): List<StateValidationIssue> =
        try {
            eventStateHolder.validateStateTransition(currentState, newState)
        } catch (e: IllegalStateException) {
            Log.e("WaveHitDetector", "State error validating state transition: $e")
            emptyList()
        } catch (e: Exception) {
            Log.e("WaveHitDetector", "Unexpected error validating state transition: $e")
            emptyList()
        }

    /**
     * Creates an EventState from current StateFlow values.
     * Used for state transition validation.
     *
     * @param progression Current wave progression
     * @param status Current event status
     * @param isUserWarmingInProgress Whether user warming is in progress
     * @param isStartWarmingInProgress Whether start warming is in progress
     * @param userIsGoingToBeHit Whether user is going to be hit
     * @param userHasBeenHit Whether user has been hit
     * @param userPositionRatio User's position ratio in the wave area
     * @param timeBeforeHit Time remaining before hit
     * @param hitDateTime Expected hit date/time
     * @param userIsInArea Whether user is in the event area
     * @return The created EventState, or null if creation fails
     */
    fun createEventState(
        progression: Double,
        status: Status,
        isUserWarmingInProgress: Boolean,
        isStartWarmingInProgress: Boolean,
        userIsGoingToBeHit: Boolean,
        userHasBeenHit: Boolean,
        userPositionRatio: Double,
        timeBeforeHit: kotlin.time.Duration,
        hitDateTime: kotlin.time.Instant,
        userIsInArea: Boolean,
    ): EventState? =
        try {
            EventState(
                progression = progression,
                status = status,
                isUserWarmingInProgress = isUserWarmingInProgress,
                isStartWarmingInProgress = isStartWarmingInProgress,
                userIsGoingToBeHit = userIsGoingToBeHit,
                userHasBeenHit = userHasBeenHit,
                userPositionRatio = userPositionRatio,
                timeBeforeHit = timeBeforeHit,
                hitDateTime = hitDateTime,
                userIsInArea = userIsInArea,
                timestamp = clock.now(),
            )
        } catch (e: IllegalStateException) {
            Log.e("WaveHitDetector", "State error creating EventState: $e")
            null
        } catch (e: Exception) {
            Log.e("WaveHitDetector", "Unexpected error creating EventState: $e")
            null
        }
}
