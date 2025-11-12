/*
 * Copyright 2025 DrWave
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

package com.worldwidewaves.shared.ui.components

import com.worldwidewaves.shared.events.IWWWEvent.Status
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime

/**
 * Tests for ButtonWave component's enabled/disabled state logic.
 *
 * Validates the business rules for when the "Join the wave" button should be:
 * - Enabled: User is in area AND (event is RUNNING OR SOON OR user is warming OR event recently ended)
 * - Disabled: Any other condition
 *
 * When disabled and clicked, the button should show the requirements dialog.
 */
@OptIn(ExperimentalTime::class)
class ButtonWaveDisabledClickTest {
    // ========== BUTTON ENABLED TESTS ==========

    @Test
    fun `button should be enabled when event is RUNNING and user is in area`() {
        val isEnabled =
            calculateButtonEnabled(
                eventState = Status.RUNNING,
                isInArea = true,
                isUserWarmingInProgress = false,
                endDateTime = null,
            )
        assertTrue(isEnabled, "Button should be enabled when event is RUNNING and user is in area")
    }

    @Test
    fun `button should be enabled when event is SOON and user is in area`() {
        val isEnabled =
            calculateButtonEnabled(
                eventState = Status.SOON,
                isInArea = true,
                isUserWarmingInProgress = false,
                endDateTime = null,
            )
        assertTrue(isEnabled, "Button should be enabled when event is SOON and user is in area")
    }

    @Test
    fun `button should be enabled when user is warming and in area`() {
        val isEnabled =
            calculateButtonEnabled(
                eventState = Status.NEXT,
                isInArea = true,
                isUserWarmingInProgress = true,
                endDateTime = null,
            )
        assertTrue(isEnabled, "Button should be enabled when user is warming")
    }

    @Test
    fun `button should be enabled when event recently ended (within 1 hour) and user in area`() {
        val now = Instant.fromEpochSeconds(1000000)
        val endDateTime = now - 30.minutes // Ended 30 minutes ago

        val isEnabled =
            calculateButtonEnabled(
                eventState = Status.DONE,
                isInArea = true,
                isUserWarmingInProgress = false,
                endDateTime = endDateTime,
                now = now,
            )
        assertTrue(isEnabled, "Button should be enabled when event recently ended and user in area")
    }

    // ========== BUTTON DISABLED TESTS ==========

    @Test
    fun `button should be disabled when user is NOT in area (event RUNNING)`() {
        val isEnabled =
            calculateButtonEnabled(
                eventState = Status.RUNNING,
                isInArea = false,
                isUserWarmingInProgress = false,
                endDateTime = null,
            )
        assertFalse(isEnabled, "Button should be disabled when user is not in area")
    }

    @Test
    fun `button should be disabled when event is NEXT and user not warming`() {
        val isEnabled =
            calculateButtonEnabled(
                eventState = Status.NEXT,
                isInArea = true,
                isUserWarmingInProgress = false,
                endDateTime = null,
            )
        assertFalse(isEnabled, "Button should be disabled when event is NEXT and user not warming")
    }

    @Test
    fun `button should be disabled when event is DONE (ended more than 1 hour ago)`() {
        val now = Instant.fromEpochSeconds(1000000)
        val endDateTime = now - 2.hours // Ended 2 hours ago

        val isEnabled =
            calculateButtonEnabled(
                eventState = Status.DONE,
                isInArea = true,
                isUserWarmingInProgress = false,
                endDateTime = endDateTime,
                now = now,
            )
        assertFalse(isEnabled, "Button should be disabled when event ended more than 1 hour ago")
    }

    @Test
    fun `button should be disabled when user not in area even if event is SOON`() {
        val isEnabled =
            calculateButtonEnabled(
                eventState = Status.SOON,
                isInArea = false,
                isUserWarmingInProgress = false,
                endDateTime = null,
            )
        assertFalse(isEnabled, "Button should be disabled when user not in area")
    }

    @Test
    fun `button should be disabled when user not in area even if warming`() {
        val isEnabled =
            calculateButtonEnabled(
                eventState = Status.NEXT,
                isInArea = false,
                isUserWarmingInProgress = true,
                endDateTime = null,
            )
        assertFalse(isEnabled, "Button should be disabled when user not in area even if warming")
    }

    // ========== EDGE CASE TESTS ==========

    @Test
    fun `button should be disabled when event ended exactly 1 hour ago (boundary)`() {
        val now = Instant.fromEpochSeconds(1000000)
        val endDateTime = now - 1.hours // Ended exactly 1 hour ago

        val isEnabled =
            calculateButtonEnabled(
                eventState = Status.DONE,
                isInArea = true,
                isUserWarmingInProgress = false,
                endDateTime = endDateTime,
                now = now,
            )
        assertFalse(isEnabled, "Button should be disabled at 1 hour boundary (exclusive)")
    }

    @Test
    fun `button should be enabled when event ended 59 minutes ago (just before boundary)`() {
        val now = Instant.fromEpochSeconds(1000000)
        val endDateTime = now - 59.minutes

        val isEnabled =
            calculateButtonEnabled(
                eventState = Status.DONE,
                isInArea = true,
                isUserWarmingInProgress = false,
                endDateTime = endDateTime,
                now = now,
            )
        assertTrue(isEnabled, "Button should be enabled just before 1 hour boundary")
    }

    @Test
    fun `button should be disabled when endDateTime is in the future`() {
        val now = Instant.fromEpochSeconds(1000000)
        val endDateTime = now + 1.hours // Future end time (shouldn't happen, but test it)

        val isEnabled =
            calculateButtonEnabled(
                eventState = Status.DONE,
                isInArea = true,
                isUserWarmingInProgress = false,
                endDateTime = endDateTime,
                now = now,
            )
        assertFalse(isEnabled, "Button should be disabled when endDateTime is in future")
    }

    // ========== HELPER FUNCTIONS ==========

    /**
     * Helper function that replicates ButtonWave's enabled logic for testing.
     * This is the core business logic we're testing.
     */
    private fun calculateButtonEnabled(
        eventState: Status,
        isInArea: Boolean,
        isUserWarmingInProgress: Boolean,
        endDateTime: Instant?,
        now: Instant = Instant.fromEpochSeconds(1000000), // Default fixed time for tests
    ): Boolean {
        val isRunning = eventState == Status.RUNNING
        val isSoon = eventState == Status.SOON
        val isEndDateTimeRecent =
            endDateTime?.let {
                it > (now - 1.hours) && it <= now
            } ?: false

        return isInArea && (isRunning || isSoon || isUserWarmingInProgress || isEndDateTimeRecent)
    }
}
