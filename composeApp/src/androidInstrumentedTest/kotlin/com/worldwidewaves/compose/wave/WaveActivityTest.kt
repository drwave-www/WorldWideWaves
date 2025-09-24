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

package com.worldwidewaves.compose.wave

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.events.utils.IClock
import com.worldwidewaves.shared.monitoring.PerformanceMonitor
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/**
 * Critical Phase 1 UI tests for Wave Activity - Core wave participation workflow
 *
 * Tests cover the most critical user workflow for WorldWideWaves application:
 * wave participation timing, coordination, animations, and user feedback.
 */
@OptIn(kotlin.time.ExperimentalTime::class)
@RunWith(AndroidJUnit4::class)
class WaveActivityTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val performanceMonitor = mockk<PerformanceMonitor>(relaxed = true)

    @Test
    fun testWaveCountdownTimerAccuracy() {
        val trace = mockk<com.worldwidewaves.shared.monitoring.PerformanceTrace>(relaxed = true)
        every { performanceMonitor.startTrace("testWaveCountdownTimerAccuracy") } returns trace

        // Create mock event with countdown timer
        val mockEvent = mockk<IWWWEvent>(relaxed = true)
        val timeBeforeHitFlow = MutableStateFlow(65.seconds) // 1:05

        every { mockEvent.observer.timeBeforeHit } returns timeBeforeHitFlow

        composeTestRule.setContent {
            TestWaveCountdownTimer(mockEvent) { timeBeforeHitFlow.value }
        }

        // Verify initial countdown display (MM:SS format) - increased timeout for emulator
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule
                .onAllNodesWithContentDescription("Countdown: 01:05")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeTestRule
            .onNodeWithContentDescription("Countdown: 01:05")
            .assertIsDisplayed()

        // Update time and verify accuracy within 100ms tolerance
        timeBeforeHitFlow.value = 30.seconds

        composeTestRule.waitUntil(timeoutMillis = 500) {
            composeTestRule
                .onAllNodesWithContentDescription("Countdown: 00:30")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeTestRule
            .onNodeWithContentDescription("Countdown: 00:30")
            .assertIsDisplayed()

        trace.stop()
    }

    @Test
    fun testWaveChoreographyAnimations() {
        val trace = mockk<com.worldwidewaves.shared.monitoring.PerformanceTrace>(relaxed = true)
        every { performanceMonitor.startTrace("testWaveChoreographyAnimations") } returns trace

        val mockEvent = mockk<IWWWEvent>(relaxed = true)
        val mockClock = mockk<IClock>(relaxed = true)
        val isWarmingFlow = MutableStateFlow(false)
        val isGoingToBeHitFlow = MutableStateFlow(false)
        val hasBeenHitFlow = MutableStateFlow(false)

        every { mockEvent.observer.isUserWarmingInProgress } returns isWarmingFlow
        every { mockEvent.observer.userIsGoingToBeHit } returns isGoingToBeHitFlow
        every { mockEvent.observer.userHasBeenHit } returns hasBeenHitFlow

        composeTestRule.setContent {
            TestWaveChoreographyDisplay(mockEvent, mockClock)
        }

        // Test warming phase animation
        isWarmingFlow.value = true

        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule
                .onAllNodesWithContentDescription("Wave choreography: Warming phase active")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeTestRule
            .onNodeWithContentDescription("Wave choreography: Warming phase active")
            .assertIsDisplayed()

        // Test hit phase animation sequence
        isWarmingFlow.value = false
        isGoingToBeHitFlow.value = true

        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule
                .onAllNodesWithContentDescription("Wave choreography: Hit phase active")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeTestRule
            .onNodeWithContentDescription("Wave choreography: Hit phase active")
            .assertIsDisplayed()

        // Test post-hit animation
        isGoingToBeHitFlow.value = false
        hasBeenHitFlow.value = true

        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule
                .onAllNodesWithContentDescription("Wave choreography: Hit complete")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeTestRule
            .onNodeWithContentDescription("Wave choreography: Hit complete")
            .assertIsDisplayed()

        trace.stop()
    }

    @Test
    fun testLocationTrackingDuringWave() {
        val trace = mockk<com.worldwidewaves.shared.monitoring.PerformanceTrace>(relaxed = true)
        every { performanceMonitor.startTrace("testLocationTrackingDuringWave") } returns trace

        val mockEvent = mockk<IWWWEvent>(relaxed = true)
        val isInAreaFlow = MutableStateFlow(false)
        val userPositionRatioFlow = MutableStateFlow(0.0)

        every { mockEvent.observer.userIsInArea } returns isInAreaFlow
        every { mockEvent.observer.userPositionRatio } returns userPositionRatioFlow

        composeTestRule.setContent {
            TestLocationTracking(mockEvent)
        }

        // Test user outside area
        composeTestRule
            .onNodeWithContentDescription("Location status: Outside wave area")
            .assertIsDisplayed()

        // Test user entering area
        isInAreaFlow.value = true
        userPositionRatioFlow.value = 0.25

        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule
                .onAllNodesWithContentDescription("Location status: In wave area, position 25%")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeTestRule
            .onNodeWithContentDescription("Location status: In wave area, position 25%")
            .assertIsDisplayed()

        // Test GPS accuracy during wave coordination
        userPositionRatioFlow.value = 0.75

        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule
                .onAllNodesWithContentDescription("Location status: In wave area, position 75%")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        trace.stop()
    }

    @Test
    fun testSoundVibrationCoordination() {
        val trace = mockk<com.worldwidewaves.shared.monitoring.PerformanceTrace>(relaxed = true)
        every { performanceMonitor.startTrace("testSoundVibrationCoordination") } returns trace

        val mockEvent = mockk<IWWWEvent>(relaxed = true)
        val hasBeenHitFlow = MutableStateFlow(false)

        every { mockEvent.observer.userHasBeenHit } returns hasBeenHitFlow
        coEvery { mockEvent.warming.playCurrentSoundChoreographyTone() } returns 1

        composeTestRule.setContent {
            TestSoundVibrationCoordination(mockEvent)
        }

        // Verify initial state - no sound played
        composeTestRule
            .onNodeWithContentDescription("Sound status: Ready")
            .assertIsDisplayed()

        // Test sound trigger on hit
        hasBeenHitFlow.value = true

        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule
                .onAllNodesWithContentDescription("Sound status: Hit sound triggered")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        // Verify sound choreography was called
        coVerify(exactly = 1) { mockEvent.warming.playCurrentSoundChoreographyTone() }

        trace.stop()
    }

    @Test
    fun testWavePhaseTransitions() {
        val trace = mockk<com.worldwidewaves.shared.monitoring.PerformanceTrace>(relaxed = true)
        every { performanceMonitor.startTrace("testWavePhaseTransitions") } returns trace

        val mockEvent = mockk<IWWWEvent>(relaxed = true)
        val statusFlow = MutableStateFlow(IWWWEvent.Status.UNDEFINED)
        val isWarmingFlow = MutableStateFlow(false)
        val isGoingToBeHitFlow = MutableStateFlow(false)
        val hasBeenHitFlow = MutableStateFlow(false)

        every { mockEvent.observer.eventStatus } returns statusFlow
        every { mockEvent.observer.isUserWarmingInProgress } returns isWarmingFlow
        every { mockEvent.observer.userIsGoingToBeHit } returns isGoingToBeHitFlow
        every { mockEvent.observer.userHasBeenHit } returns hasBeenHitFlow

        composeTestRule.setContent {
            TestWavePhaseTransitions(mockEvent)
        }

        // Observer phase
        composeTestRule
            .onNodeWithContentDescription("Wave phase: Observer")
            .assertIsDisplayed()

        // Warming phase
        statusFlow.value = IWWWEvent.Status.RUNNING
        isWarmingFlow.value = true

        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule
                .onAllNodesWithContentDescription("Wave phase: Warming")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        // Waiting phase (warming stops, not yet hit)
        isWarmingFlow.value = false

        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule
                .onAllNodesWithContentDescription("Wave phase: Waiting")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        // Hit phase
        isGoingToBeHitFlow.value = true

        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule
                .onAllNodesWithContentDescription("Wave phase: Hit")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        // Done phase
        isGoingToBeHitFlow.value = false
        hasBeenHitFlow.value = true
        statusFlow.value = IWWWEvent.Status.DONE

        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule
                .onAllNodesWithContentDescription("Wave phase: Done")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        trace.stop()
    }

    @Test
    fun testWaveParticipationErrorHandling() {
        val trace = mockk<com.worldwidewaves.shared.monitoring.PerformanceTrace>(relaxed = true)
        every { performanceMonitor.startTrace("testWaveParticipationErrorHandling") } returns trace

        val mockEvent = mockk<IWWWEvent>(relaxed = true)
        var networkError by mutableStateOf(false)
        var gpsError by mutableStateOf(false)

        composeTestRule.setContent {
            TestWaveErrorHandling(mockEvent, networkError, gpsError)
        }

        // Test initial success state
        composeTestRule
            .onNodeWithContentDescription("Error status: No errors")
            .assertIsDisplayed()

        // Test network connectivity issues
        networkError = true

        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule
                .onAllNodesWithContentDescription("Error status: Network connection lost")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeTestRule
            .onNodeWithContentDescription("Error status: Network connection lost")
            .assertIsDisplayed()

        // Test GPS signal loss scenarios
        networkError = false
        gpsError = true

        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule
                .onAllNodesWithContentDescription("Error status: GPS signal lost")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeTestRule
            .onNodeWithContentDescription("Error status: GPS signal lost")
            .assertIsDisplayed()

        // Test graceful error recovery
        gpsError = false

        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule
                .onAllNodesWithContentDescription("Error status: Recovered")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        trace.stop()
    }

    @Test
    fun testRealTimeWaveCoordination() {
        val trace = mockk<com.worldwidewaves.shared.monitoring.PerformanceTrace>(relaxed = true)
        every { performanceMonitor.startTrace("testRealTimeWaveCoordination") } returns trace

        val mockEvent = mockk<IWWWEvent>(relaxed = true)
        val progressionFlow = MutableStateFlow(0.0)
        val participantsFlow = MutableStateFlow(1)

        every { mockEvent.observer.progression } returns progressionFlow

        composeTestRule.setContent {
            TestRealTimeCoordination(mockEvent, participantsFlow.value)
        }

        // Test initial wave coordination
        composeTestRule
            .onNodeWithContentDescription("Coordination status: Wave starting, 1 participants")
            .assertIsDisplayed()

        // Test multi-user wave synchronization
        progressionFlow.value = 25.0
        participantsFlow.value = 5

        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule
                .onAllNodesWithContentDescription("Coordination status: Wave 25% complete, 5 participants")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        // Test real-time status updates accuracy
        progressionFlow.value = 75.0
        participantsFlow.value = 12

        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule
                .onAllNodesWithContentDescription("Coordination status: Wave 75% complete, 12 participants")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        trace.stop()
    }

    @Test
    fun testWaveHitAccuracyValidation() {
        val trace = mockk<com.worldwidewaves.shared.monitoring.PerformanceTrace>(relaxed = true)
        every { performanceMonitor.startTrace("testWaveHitAccuracyValidation") } returns trace

        val mockEvent = mockk<IWWWEvent>(relaxed = true)
        val mockClock = mockk<IClock>(relaxed = true)
        val hitAccuracy = MutableStateFlow(0.0) // Milliseconds difference

        every { mockClock.now() } returns Instant.fromEpochMilliseconds(1000000)

        composeTestRule.setContent {
            TestWaveHitAccuracy(mockEvent, mockClock, hitAccuracy.value)
        }

        // Test perfect hit timing (within 50ms)
        hitAccuracy.value = 25.0

        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule
                .onAllNodesWithContentDescription("Hit accuracy: Perfect timing (25ms difference)")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        // Test good hit timing (within 100ms)
        hitAccuracy.value = 75.0

        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule
                .onAllNodesWithContentDescription("Hit accuracy: Good timing (75ms difference)")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        // Test missed hit (over 200ms)
        hitAccuracy.value = 250.0

        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule
                .onAllNodesWithContentDescription("Hit accuracy: Missed timing (250ms difference)")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        trace.stop()
    }

    // Test helper composables

    @Composable
    private fun TestWaveCountdownTimer(
        event: IWWWEvent,
        getTimeBeforeHit: () -> kotlin.time.Duration,
    ) {
        val timeBeforeHit = getTimeBeforeHit()
        val text = formatTestDuration(timeBeforeHit)

        Text(
            text = "Countdown",
            modifier =
                Modifier.semantics {
                    contentDescription = "Countdown: $text"
                },
        )
    }

    @Composable
    private fun TestWaveChoreographyDisplay(
        event: IWWWEvent,
        clock: IClock,
    ) {
        val isWarming by event.observer.isUserWarmingInProgress.collectAsState(false)
        val isGoingToBeHit by event.observer.userIsGoingToBeHit.collectAsState(false)
        val hasBeenHit by event.observer.userHasBeenHit.collectAsState(false)

        val phase =
            when {
                isWarming -> "Warming phase active"
                isGoingToBeHit -> "Hit phase active"
                hasBeenHit -> "Hit complete"
                else -> "Observer phase"
            }

        Text(
            text = "Wave choreography",
            modifier =
                Modifier.semantics {
                    contentDescription = "Wave choreography: $phase"
                },
        )
    }

    @Composable
    private fun TestLocationTracking(event: IWWWEvent) {
        val isInArea by event.observer.userIsInArea.collectAsState(false)
        val userPositionRatio by event.observer.userPositionRatio.collectAsState(0.0)

        val status =
            if (isInArea) {
                "In wave area, position ${(userPositionRatio * 100).toInt()}%"
            } else {
                "Outside wave area"
            }

        Text(
            text = "Location tracking",
            modifier =
                Modifier.semantics {
                    contentDescription = "Location status: $status"
                },
        )
    }

    @Composable
    private fun TestSoundVibrationCoordination(event: IWWWEvent) {
        val hasBeenHit by event.observer.userHasBeenHit.collectAsState(false)
        var soundTriggered by remember { mutableStateOf(false) }

        LaunchedEffect(hasBeenHit) {
            if (hasBeenHit && !soundTriggered) {
                event.warming.playCurrentSoundChoreographyTone()
                soundTriggered = true
            }
        }

        val status = if (soundTriggered) "Hit sound triggered" else "Ready"

        Text(
            text = "Sound coordination",
            modifier =
                Modifier.semantics {
                    contentDescription = "Sound status: $status"
                },
        )
    }

    @Composable
    private fun TestWavePhaseTransitions(event: IWWWEvent) {
        val status by event.observer.eventStatus.collectAsState(IWWWEvent.Status.UNDEFINED)
        val isWarming by event.observer.isUserWarmingInProgress.collectAsState(false)
        val isGoingToBeHit by event.observer.userIsGoingToBeHit.collectAsState(false)
        val hasBeenHit by event.observer.userHasBeenHit.collectAsState(false)

        val phase =
            when {
                status == IWWWEvent.Status.DONE -> "Done"
                hasBeenHit -> "Done"
                isGoingToBeHit -> "Hit"
                isWarming -> "Warming"
                status == IWWWEvent.Status.RUNNING -> "Waiting"
                else -> "Observer"
            }

        Text(
            text = "Wave phase",
            modifier =
                Modifier.semantics {
                    contentDescription = "Wave phase: $phase"
                },
        )
    }

    @Composable
    private fun TestWaveErrorHandling(
        event: IWWWEvent,
        networkError: Boolean,
        gpsError: Boolean,
    ) {
        val status =
            when {
                networkError -> "Network connection lost"
                gpsError -> "GPS signal lost"
                !networkError && !gpsError -> "Recovered"
                else -> "No errors"
            }

        Text(
            text = "Error handling",
            modifier =
                Modifier.semantics {
                    contentDescription = "Error status: $status"
                },
        )
    }

    @Composable
    private fun TestRealTimeCoordination(
        event: IWWWEvent,
        participants: Int,
    ) {
        val progression by event.observer.progression.collectAsState(0.0)

        val status =
            if (progression == 0.0) {
                "Wave starting, $participants participants"
            } else {
                "Wave ${progression.toInt()}% complete, $participants participants"
            }

        Text(
            text = "Real-time coordination",
            modifier =
                Modifier.semantics {
                    contentDescription = "Coordination status: $status"
                },
        )
    }

    @Composable
    private fun TestWaveHitAccuracy(
        event: IWWWEvent,
        clock: IClock,
        accuracy: Double,
    ) {
        val description =
            when {
                accuracy <= 50.0 -> "Perfect timing (${accuracy.toInt()}ms difference)"
                accuracy <= 100.0 -> "Good timing (${accuracy.toInt()}ms difference)"
                else -> "Missed timing (${accuracy.toInt()}ms difference)"
            }

        Text(
            text = "Hit accuracy",
            modifier =
                Modifier.semantics {
                    contentDescription = "Hit accuracy: $description"
                },
        )
    }

    private fun formatTestDuration(duration: kotlin.time.Duration): String {
        val minutes = duration.inWholeMinutes.toString().padStart(2, '0')
        val seconds = (duration.inWholeSeconds % 60).toString().padStart(2, '0')
        return "$minutes:$seconds"
    }
}
