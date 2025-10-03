package com.worldwidewaves.shared.domain.state

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

/**
 * Holds and calculates event state for users.
 *
 * This interface abstracts the complex logic of determining a user's current state
 * relative to an event, including warming phases, hit detection, and timing calculations.
 *
 * The EventStateHolder is responsible for:
 * - Calculating user-specific event states (warming, about to be hit, etc.)
 * - Validating state transitions and data consistency
 * - Providing clean, testable state calculation logic
 * - Abstracting complex timing and positioning logic
 */
interface EventStateHolder {
    /**
     * Calculates the complete event state for a user at the current moment.
     *
     * @param event The event to calculate state for
     * @param input Input data including progression, status, position, and current time
     * @param userIsInArea Whether the user is currently in the event area
     * @return Complete EventState with all calculated values
     */
    suspend fun calculateEventState(
        event: IWWWEvent,
        input: EventStateInput,
        userIsInArea: Boolean,
    ): EventState

    /**
     * Validates the input parameters and calculated state for consistency.
     *
     * @param input Input data to validate
     * @param calculatedState The calculated state to validate
     * @return List of validation issues found (empty if all valid)
     */
    fun validateState(
        input: EventStateInput,
        calculatedState: EventState,
    ): List<StateValidationIssue>

    /**
     * Validates that a state transition is logical and allowed.
     *
     * @param previousState Previous event state (null if this is the first state)
     * @param newState New event state being transitioned to
     * @return List of validation issues found (empty if transition is valid)
     */
    fun validateStateTransition(
        previousState: EventState?,
        newState: EventState,
    ): List<StateValidationIssue>
}
