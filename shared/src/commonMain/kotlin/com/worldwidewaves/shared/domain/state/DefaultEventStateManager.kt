@file:OptIn(kotlin.time.ExperimentalTime::class)

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

import com.worldwidewaves.shared.WWWGlobals.WaveTiming
import com.worldwidewaves.shared.domain.progression.WaveProgressionTracker
import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.events.IWWWEvent.Status
import com.worldwidewaves.shared.events.utils.IClock
import com.worldwidewaves.shared.utils.Log
import kotlin.time.Duration
import kotlin.time.Duration.Companion.INFINITE
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Instant
import kotlin.time.Instant.Companion.DISTANT_FUTURE

/**
 * Default implementation of EventStateManager.
 *
 * This implementation encapsulates the complex state calculation logic that was previously
 * scattered throughout WWWEventObserver, providing:
 * - Clean separation of concerns
 * - Testable state calculation logic
 * - Consistent state validation
 * - Proper error handling and logging
 */
class DefaultEventStateManager(
    private val waveProgressionTracker: WaveProgressionTracker,
    @Suppress("UnusedPrivateProperty") // Injected for timing-critical state calculations
    private val clock: IClock,
) : EventStateManager {
    override suspend fun calculateEventState(
        event: IWWWEvent,
        input: EventStateInput,
        userIsInArea: Boolean,
    ): EventState {
        // Calculate warming phases
        val warmingInProgress = calculateWarmingPhase(event)
        val isStartWarmingInProgress = calculateStartWarmingPhase(event, input.currentTime)

        // Calculate hit-related states
        val timeBeforeHit = event.wave.timeBeforeUserHit() ?: INFINITE
        val userIsGoingToBeHit = calculateUserGoingToBeHit(timeBeforeHit, userIsInArea)
        val userHasBeenHit = event.wave.hasUserBeenHitInCurrentPosition()

        // Calculate additional timing and position data
        val userPositionRatio = event.wave.userPositionToWaveRatio() ?: 0.0
        val hitDateTime = event.wave.userHitDateTime() ?: DISTANT_FUTURE

        // Record progression snapshot for tracking
        try {
            waveProgressionTracker.recordProgressionSnapshot(event, input.userPosition)
        } catch (e: Exception) {
            Log.e("DefaultEventStateManager", "Error recording progression snapshot: $e")
        }

        return EventState(
            progression = input.progression,
            status = input.status,
            isUserWarmingInProgress = warmingInProgress && !userIsGoingToBeHit && !userHasBeenHit,
            isStartWarmingInProgress = isStartWarmingInProgress,
            userIsGoingToBeHit = userIsGoingToBeHit && !userHasBeenHit,
            userHasBeenHit = userHasBeenHit,
            userPositionRatio = userPositionRatio,
            timeBeforeHit = timeBeforeHit,
            hitDateTime = hitDateTime,
            userIsInArea = userIsInArea,
            timestamp = input.currentTime,
        )
    }

    override fun validateState(
        input: EventStateInput,
        calculatedState: EventState,
    ): List<StateValidationIssue> {
        val issues = mutableListOf<StateValidationIssue>()

        // Validate progression bounds - extract complex condition into named booleans
        val isProgressionOutOfRange = input.progression < 0.0 || input.progression > 100.0
        val isProgressionInvalid = input.progression.isNaN() || input.progression.isInfinite()

        if (isProgressionOutOfRange || isProgressionInvalid) {
            issues.add(
                StateValidationIssue(
                    field = "progression",
                    issue = "Progression ${input.progression} is out of bounds (should be 0-100)",
                    severity = StateValidationIssue.Severity.WARNING,
                ),
            )
        }

        // Validate status consistency with progression
        when (input.status) {
            Status.DONE -> {
                if (input.progression < 100.0) {
                    issues.add(
                        StateValidationIssue(
                            field = "status",
                            issue = "Status is DONE but progression is ${input.progression} (should be 100.0)",
                            severity = StateValidationIssue.Severity.WARNING,
                        ),
                    )
                }
            }
            Status.RUNNING -> {
                if (input.progression <= 0.0) {
                    issues.add(
                        StateValidationIssue(
                            field = "status",
                            issue = "Status is RUNNING but progression is ${input.progression} (should be > 0)",
                            severity = StateValidationIssue.Severity.WARNING,
                        ),
                    )
                }
            }
            else -> {
                // Other statuses can have any progression value
            }
        }

        // Validate state consistency
        if (calculatedState.userIsGoingToBeHit && calculatedState.userHasBeenHit) {
            issues.add(
                StateValidationIssue(
                    field = "userState",
                    issue = "User cannot be both 'going to be hit' and 'has been hit' simultaneously",
                    severity = StateValidationIssue.Severity.ERROR,
                ),
            )
        }

        if (calculatedState.userIsGoingToBeHit && !calculatedState.userIsInArea) {
            issues.add(
                StateValidationIssue(
                    field = "userState",
                    issue = "User is 'going to be hit' but not in area",
                    severity = StateValidationIssue.Severity.WARNING,
                ),
            )
        }

        return issues
    }

    override fun validateStateTransition(
        previousState: EventState?,
        newState: EventState,
    ): List<StateValidationIssue> {
        val issues = mutableListOf<StateValidationIssue>()

        if (previousState == null) {
            return issues // No previous state to compare against
        }

        // Validate progression transitions
        if (newState.progression < previousState.progression && newState.status != Status.DONE) {
            issues.add(
                StateValidationIssue(
                    field = "progression",
                    issue =
                        "Progression went backwards: ${previousState.progression} -> " +
                            "${newState.progression} (status: ${newState.status})",
                    severity = StateValidationIssue.Severity.WARNING,
                ),
            )
        }

        // Validate status transitions follow logical order
        when (previousState.status) {
            Status.DONE -> {
                if (newState.status != Status.DONE) {
                    issues.add(
                        StateValidationIssue(
                            field = "status",
                            issue = "Invalid transition from DONE to ${newState.status}",
                            severity = StateValidationIssue.Severity.WARNING,
                        ),
                    )
                }
            }
            Status.RUNNING -> {
                if (newState.status == Status.NEXT || newState.status == Status.SOON) {
                    issues.add(
                        StateValidationIssue(
                            field = "status",
                            issue = "Invalid backward transition from RUNNING to ${newState.status}",
                            severity = StateValidationIssue.Severity.WARNING,
                        ),
                    )
                }
            }
            Status.SOON -> {
                if (newState.status == Status.NEXT) {
                    issues.add(
                        StateValidationIssue(
                            field = "status",
                            issue = "Invalid backward transition from SOON to ${newState.status}",
                            severity = StateValidationIssue.Severity.WARNING,
                        ),
                    )
                }
            }
            else -> {
                // NEXT, SOON, and UNDEFINED can transition to any state
            }
        }

        // Validate that hit states don't reverse
        if (previousState.userHasBeenHit && !newState.userHasBeenHit) {
            issues.add(
                StateValidationIssue(
                    field = "userHasBeenHit",
                    issue = "User cannot transition from 'has been hit' to 'not hit'",
                    severity = StateValidationIssue.Severity.ERROR,
                ),
            )
        }

        return issues
    }

    /**
     * Calculates if the user warming phase is in progress.
     */
    private suspend fun calculateWarmingPhase(event: IWWWEvent): Boolean =
        try {
            event.warming.isUserWarmingStarted()
        } catch (e: Exception) {
            Log.e("DefaultEventStateManager", "Error checking warming phase: $e")
            false
        }

    /**
     * Calculates if the start warming phase is in progress (between event start and wave start).
     */
    private fun calculateStartWarmingPhase(
        event: IWWWEvent,
        currentTime: Instant,
    ): Boolean =
        try {
            currentTime > event.getStartDateTime() && currentTime < event.getWaveStartDateTime()
        } catch (e: Exception) {
            Log.e("DefaultEventStateManager", "Error checking start warming phase: $e")
            false
        }

    /**
     * Calculates if the user is about to be hit by the wave.
     */
    private fun calculateUserGoingToBeHit(
        timeBeforeHit: Duration,
        userIsInArea: Boolean,
    ): Boolean = userIsInArea && timeBeforeHit > ZERO && timeBeforeHit <= WaveTiming.WARN_BEFORE_HIT
}
