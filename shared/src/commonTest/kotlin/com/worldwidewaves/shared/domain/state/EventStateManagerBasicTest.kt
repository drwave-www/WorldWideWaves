@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.worldwidewaves.shared.domain.state

import com.worldwidewaves.shared.domain.progression.WaveProgressionTracker
import com.worldwidewaves.shared.events.IWWWEvent.Status
import com.worldwidewaves.shared.events.utils.IClock
import com.worldwidewaves.shared.events.utils.Position
import io.mockk.every
import io.mockk.mockk
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.INFINITE
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant.Companion.DISTANT_FUTURE

class EventStateManagerBasicTest {

    private val mockWaveProgressionTracker = mockk<WaveProgressionTracker>()
    private val mockClock = mockk<IClock>()

    private val eventStateManager = DefaultEventStateManager(
        mockWaveProgressionTracker,
        mockClock
    )

    @Test
    fun `can create DefaultEventStateManager`() {
        val manager = DefaultEventStateManager(
            mockWaveProgressionTracker,
            mockClock
        )

        assertTrue(manager is EventStateManager)
    }

    @Test
    fun `validateState returns no issues for valid progression`() {
        val input = EventStateInput(
            progression = 50.0,
            status = Status.RUNNING,
            userPosition = Position(40.7128, -74.0060),
            currentTime = Instant.fromEpochSeconds(1000)
        )

        val state = EventState(
            progression = 50.0,
            status = Status.RUNNING,
            isUserWarmingInProgress = false,
            isStartWarmingInProgress = false,
            userIsGoingToBeHit = false,
            userHasBeenHit = false,
            userPositionRatio = 0.5,
            timeBeforeHit = 30.seconds,
            hitDateTime = DISTANT_FUTURE,
            userIsInArea = true,
            timestamp = Instant.fromEpochSeconds(1000)
        )

        val issues = eventStateManager.validateState(input, state)

        assertTrue(issues.isEmpty(), "Should have no validation issues for valid state")
    }

    @Test
    fun `validateState detects progression out of bounds`() {
        val validProgression = listOf(0.0, 50.0, 100.0)
        val invalidProgression = listOf(-1.0, 101.0, Double.NaN, Double.POSITIVE_INFINITY)

        // Test valid progressions
        validProgression.forEach { progression ->
            val input = createTestInput(progression = progression)
            val state = createTestState(progression = progression)
            val issues = eventStateManager.validateState(input, state)

            val progressionIssues = issues.filter { it.field == "progression" }
            assertTrue(progressionIssues.isEmpty(), "Progression $progression should be valid")
        }

        // Test invalid progressions
        invalidProgression.forEach { progression ->
            val input = createTestInput(progression = progression)
            val state = createTestState(progression = progression)
            val issues = eventStateManager.validateState(input, state)

            val progressionIssues = issues.filter { it.field == "progression" }
            assertTrue(progressionIssues.isNotEmpty(), "Progression $progression should be invalid")
        }
    }

    @Test
    fun `validateState detects status-progression inconsistencies`() {
        // DONE status should have 100% progression
        val doneInput = createTestInput(progression = 50.0, status = Status.DONE)
        val doneState = createTestState(progression = 50.0, status = Status.DONE)
        val doneIssues = eventStateManager.validateState(doneInput, doneState)

        assertTrue(doneIssues.any { it.field == "status" && it.issue.contains("DONE") })

        // RUNNING status should have > 0% progression
        val runningInput = createTestInput(progression = 0.0, status = Status.RUNNING)
        val runningState = createTestState(progression = 0.0, status = Status.RUNNING)
        val runningIssues = eventStateManager.validateState(runningInput, runningState)

        assertTrue(runningIssues.any { it.field == "status" && it.issue.contains("RUNNING") })
    }

    @Test
    fun `validateState detects conflicting user states`() {
        // User cannot be both "going to be hit" and "has been hit"
        val input = createTestInput()
        val state = createTestState(
            userIsGoingToBeHit = true,
            userHasBeenHit = true
        )

        val issues = eventStateManager.validateState(input, state)

        assertTrue(issues.any {
            it.field == "userState" &&
            it.issue.contains("both 'going to be hit' and 'has been hit'") &&
            it.severity == StateValidationIssue.Severity.ERROR
        })
    }

    @Test
    fun `validateStateTransition returns no issues for valid transitions`() {
        val previousState = createTestState(
            progression = 25.0,
            status = Status.SOON
        )

        val newState = createTestState(
            progression = 50.0,
            status = Status.RUNNING
        )

        val issues = eventStateManager.validateStateTransition(previousState, newState)

        assertTrue(issues.isEmpty(), "Valid state transition should have no issues")
    }

    @Test
    fun `validateStateTransition detects backward progression`() {
        val previousState = createTestState(
            progression = 75.0,
            status = Status.RUNNING
        )

        val newState = createTestState(
            progression = 50.0,
            status = Status.RUNNING
        )

        val issues = eventStateManager.validateStateTransition(previousState, newState)

        assertTrue(issues.any {
            it.field == "progression" &&
            it.issue.contains("went backwards")
        })
    }

    @Test
    fun `validateStateTransition detects invalid status transitions`() {
        // Test DONE -> RUNNING (invalid)
        val doneState = createTestState(status = Status.DONE)
        val runningState = createTestState(status = Status.RUNNING)

        val doneToRunningIssues = eventStateManager.validateStateTransition(doneState, runningState)
        assertTrue(doneToRunningIssues.any { it.field == "status" && it.issue.contains("DONE to RUNNING") })

        // Test RUNNING -> SOON (invalid backward)
        val runningToPrevious = createTestState(status = Status.RUNNING)
        val soonState = createTestState(status = Status.SOON)

        val backwardIssues = eventStateManager.validateStateTransition(runningToPrevious, soonState)
        assertTrue(backwardIssues.any { it.field == "status" && it.issue.contains("RUNNING to SOON") })
    }

    @Test
    fun `validateStateTransition prevents hit state reversal`() {
        val hitState = createTestState(userHasBeenHit = true)
        val notHitState = createTestState(userHasBeenHit = false)

        val issues = eventStateManager.validateStateTransition(hitState, notHitState)

        assertTrue(issues.any {
            it.field == "userHasBeenHit" &&
            it.issue.contains("cannot transition from 'has been hit' to 'not hit'") &&
            it.severity == StateValidationIssue.Severity.ERROR
        })
    }

    // Helper functions to create test data
    private fun createTestInput(
        progression: Double = 50.0,
        status: Status = Status.RUNNING,
        userPosition: Position? = Position(40.7128, -74.0060),
        currentTime: Instant = Instant.fromEpochSeconds(1000)
    ) = EventStateInput(progression, status, userPosition, currentTime)

    private fun createTestState(
        progression: Double = 50.0,
        status: Status = Status.RUNNING,
        isUserWarmingInProgress: Boolean = false,
        isStartWarmingInProgress: Boolean = false,
        userIsGoingToBeHit: Boolean = false,
        userHasBeenHit: Boolean = false,
        userPositionRatio: Double = 0.5,
        timeBeforeHit: kotlin.time.Duration = 30.seconds,
        hitDateTime: Instant = DISTANT_FUTURE,
        userIsInArea: Boolean = true,
        timestamp: Instant = Instant.fromEpochSeconds(1000)
    ) = EventState(
        progression,
        status,
        isUserWarmingInProgress,
        isStartWarmingInProgress,
        userIsGoingToBeHit,
        userHasBeenHit,
        userPositionRatio,
        timeBeforeHit,
        hitDateTime,
        userIsInArea,
        timestamp
    )
}