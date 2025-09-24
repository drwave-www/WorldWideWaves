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

import com.worldwidewaves.shared.events.IWWWEvent.Status
import com.worldwidewaves.shared.events.utils.Position
import kotlin.time.Duration
import kotlin.time.Instant

/**
 * Represents the complete state of an event for a user at a specific moment in time.
 * This encapsulates all the calculated state values that the UI needs to display.
 */
data class EventState(
    val progression: Double,
    val status: Status,
    val isUserWarmingInProgress: Boolean,
    val isStartWarmingInProgress: Boolean,
    val userIsGoingToBeHit: Boolean,
    val userHasBeenHit: Boolean,
    val userPositionRatio: Double,
    val timeBeforeHit: Duration,
    val hitDateTime: Instant,
    val userIsInArea: Boolean,
    val timestamp: Instant,
)

/**
 * Input data required for calculating event state.
 * Contains all the raw data needed to compute the user's current state relative to the event.
 */
data class EventStateInput(
    val progression: Double,
    val status: Status,
    val userPosition: Position?,
    val currentTime: Instant,
)

/**
 * Represents validation issues found during state calculation.
 */
data class StateValidationIssue(
    val field: String,
    val issue: String,
    val severity: Severity = Severity.WARNING,
) {
    enum class Severity {
        WARNING,
        ERROR,
    }
}
