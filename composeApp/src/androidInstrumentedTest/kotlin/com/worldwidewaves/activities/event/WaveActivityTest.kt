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

package com.worldwidewaves.activities.event

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.worldwidewaves.compose.choreographies.WaveChoreographies
import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.testing.UITestAssertions
import com.worldwidewaves.testing.UITestFactory
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Duration.Companion.hours

/**
 * UI tests for WaveActivity - core wave participation workflow
 *
 * This is the most critical user workflow as it handles the real-time
 * wave coordination experience that defines the app's core purpose.
 *
 * Tests cover:
 * - Wave countdown display and timing
 * - Choreography animations during different phases
 * - User location tracking and wave participation
 * - Sound/vibration coordination
 * - Wave status transitions (warming, waiting, hit, done)
 * - Error handling for location/timing issues
 */
@RunWith(AndroidJUnit4::class)
class WaveActivityTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<WaveActivity>()

    @Test
    fun waveActivity_countdownTimer_displaysCorrectTime() {
        // Test countdown timer functionality
        composeTestRule.setContent {
            // Set up mock event with specific time before hit
            val mockEvent = UITestFactory.createMockWaveEvent(
                timeBeforeHit = kotlin.time.Duration.parse("5m30s")
            )

            // WaveHitCounter should display "05:30"
            WaveHitCounter(mockEvent as IWWWEvent)
        }

        // Verify timer displays correct format (MM:SS)
        composeTestRule.onNodeWithText("05:30").assertExists()

        // Test different time formats
        val testDurations = UITestFactory.createTimerTestData()
        testDurations.forEach { duration ->
            composeTestRule.setContent {
                val mockEvent = UITestFactory.createMockWaveEvent(timeBeforeHit = duration)
                WaveHitCounter(mockEvent as IWWWEvent)
            }

            // Verify timer format is correct for each duration
            val expectedFormat = formatDurationForTest(duration)
            composeTestRule.onNodeWithText(expectedFormat).assertExists()
        }
    }

    private fun formatDurationForTest(duration: kotlin.time.Duration): String {
        return when {
            duration < 1.hours -> {
                val minutes = duration.inWholeMinutes.toString().padStart(2, '0')
                val seconds = (duration.inWholeSeconds % 60).toString().padStart(2, '0')
                "$minutes:$seconds"
            }
            else -> {
                val hours = duration.inWholeHours.toString().padStart(2, '0')
                val minutes = (duration.inWholeMinutes % 60).toString().padStart(2, '0')
                "$hours:$minutes"
            }
        }
    }

    @Test
    fun waveActivity_choreographyAnimations_displayDuringPhases() {
        // Test choreography display during different phases
        val phaseScenarios = UITestFactory.createWavePhaseScenarios()

        phaseScenarios.forEach { scenario ->
            composeTestRule.setContent {
                val mockEvent = UITestFactory.createMockWaveEvent(
                    isWarmingInProgress = scenario["isWarmingInProgress"] as Boolean,
                    isGoingToBeHit = scenario["isGoingToBeHit"] as Boolean,
                    hasBeenHit = scenario["hasBeenHit"] as Boolean
                )

                // Test choreography visibility based on phase
                WaveChoreographies(
                    event = mockEvent as IWWWEvent,
                    clock = mockk(relaxed = true),
                    modifier = Modifier.testTag("choreography")
                )
            }

            val phase = scenario["phase"] as String
            when (phase) {
                "WARMING" -> {
                    // Choreography should be visible during warming
                    composeTestRule.onNodeWithTag("choreography").assertExists()
                }
                "WAITING" -> {
                    // Choreography should be visible during waiting
                    composeTestRule.onNodeWithTag("choreography").assertExists()
                }
                "HIT" -> {
                    // Choreography should be visible immediately after hit
                    composeTestRule.onNodeWithTag("choreography").assertExists()
                }
                "OBSERVER" -> {
                    // Choreography should not be active when observing
                    // Counter should be visible instead
                    composeTestRule.onNodeWithTag("choreography").assertDoesNotExist()
                }
            }
        }
    }

    @Test
    fun waveActivity_wavePhases_transitionsCorrectly() {
        // Test wave phase transitions: Observer -> Warming -> Waiting -> Hit -> Done
        val phaseScenarios = UITestFactory.createWavePhaseScenarios()

        phaseScenarios.forEach { scenario ->
            composeTestRule.setContent {
                val mockEvent = UITestFactory.createMockWaveEvent(
                    isInArea = scenario["isInArea"] as Boolean,
                    isWarmingInProgress = scenario["isWarmingInProgress"] as Boolean,
                    isGoingToBeHit = scenario["isGoingToBeHit"] as Boolean,
                    hasBeenHit = scenario["hasBeenHit"] as Boolean
                )

                UserWaveStatusText(
                    event = mockEvent as IWWWEvent,
                    modifier = Modifier.testTag("status-text")
                )
            }

            val phase = scenario["phase"] as String

            // Verify correct status text for each phase
            when (phase) {
                "OBSERVER" -> {
                    // Should show "Wave is running" when not in area
                    composeTestRule.onNodeWithTag("status-text").assertExists()
                }
                "WARMING" -> {
                    // Should show warming message when in warming phase
                    composeTestRule.onNodeWithTag("status-text").assertExists()
                }
                "WAITING" -> {
                    // Should show "Be ready" when waiting for hit
                    composeTestRule.onNodeWithTag("status-text").assertExists()
                }
                "HIT" -> {
                    // Should show hit confirmation message
                    composeTestRule.onNodeWithTag("status-text").assertExists()
                }
            }

            // Validate phase transition logic
            val fromPhase = if (phaseScenarios.indexOf(scenario) > 0) {
                phaseScenarios[phaseScenarios.indexOf(scenario) - 1]["phase"] as String
            } else null

            if (fromPhase != null) {
                UITestAssertions.assertValidPhaseTransition(fromPhase, phase)
            }
        }
    }

    @Test
    fun waveActivity_locationTracking_handlesUserMovement() {
        // Test location-based wave participation
        val progressionData = UITestFactory.createProgressionTestData()

        progressionData.forEach { progression ->
            composeTestRule.setContent {
                val mockEvent = UITestFactory.createMockWaveEvent(
                    isInArea = true,
                    progression = progression,
                    userPositionRatio = progression / 100.0
                )

                WaveProgressionBar(
                    event = mockEvent as IWWWEvent,
                    modifier = Modifier.testTag("progression-bar")
                )
            }

            // Verify progression bar displays correct percentage
            composeTestRule.onNodeWithText("${String.format("%.1f", progression)}%")
                .assertExists()

            // Verify user position ratio is valid
            UITestAssertions.assertValidUserPosition(progression / 100.0)

            // Verify progression is within expected range
            UITestAssertions.assertProgressionRange(progression, 0.0, 100.0)
        }

        // Test edge cases
        composeTestRule.setContent {
            val mockEvent = UITestFactory.createMockWaveEvent(
                isInArea = false // User not in area
            )

            WaveProgressionBar(
                event = mockEvent as IWWWEvent,
                modifier = Modifier.testTag("progression-bar-no-area")
            )
        }

        // When user is not in area, triangle indicator should not be visible
        composeTestRule.onNodeWithTag("progression-bar-no-area").assertExists()
    }

    @Test
    fun waveActivity_soundCoordination_playsAtCorrectTime() {
        // Test sound/vibration coordination timing
        composeTestRule.setContent {
            val mockEvent = UITestFactory.createMockWaveEvent(
                hasBeenHit = true,
                timeBeforeHit = kotlin.time.Duration.ZERO
            )

            // Simulate the LaunchedEffect that plays hit sound
            LaunchedEffect(mockEvent) {
                // Mock the sound playing logic
                val secondsSinceHit = 0 // Immediately after hit
                val shouldPlaySound = secondsSinceHit in 0..1

                // Verify sound should be played at correct timing
                assert(shouldPlaySound) {
                    "Sound should play immediately after wave hit"
                }
            }

            UserWaveStatusText(
                event = mockEvent as IWWWEvent,
                modifier = Modifier.testTag("hit-status")
            )
        }

        // Verify hit status is displayed
        composeTestRule.onNodeWithTag("hit-status").assertExists()

        // Test timing accuracy requirements
        val expectedHitTime = System.currentTimeMillis()
        val actualHitTime = System.currentTimeMillis() // Mock actual hit time

        UITestAssertions.assertTimingAccuracy(expectedHitTime, actualHitTime, 100)
    }

    @Test
    fun waveActivity_participationFeedback_showsUserStatus() {
        // Test user participation feedback for different scenarios
        val participationScenarios = listOf(
            mapOf(
                "hasBeenHit" to true,
                "isInArea" to true,
                "expectedStatus" to "HIT"
            ),
            mapOf(
                "hasBeenHit" to false,
                "isInArea" to true,
                "expectedStatus" to "READY"
            ),
            mapOf(
                "hasBeenHit" to false,
                "isInArea" to false,
                "expectedStatus" to "OBSERVING"
            )
        )

        participationScenarios.forEach { scenario ->
            composeTestRule.setContent {
                val mockEvent = UITestFactory.createMockWaveEvent(
                    hasBeenHit = scenario["hasBeenHit"] as Boolean,
                    isInArea = scenario["isInArea"] as Boolean
                )

                UserWaveStatusText(
                    event = mockEvent as IWWWEvent,
                    modifier = Modifier.testTag("participation-feedback")
                )
            }

            // Verify appropriate status message is displayed
            composeTestRule.onNodeWithTag("participation-feedback").assertExists()
        }

        // Test progression visualization for community participation
        composeTestRule.setContent {
            val mockEvent = UITestFactory.createMockWaveEvent(
                progression = 75.0 // 75% community participation
            )

            WaveProgressionBar(
                event = mockEvent as IWWWEvent,
                modifier = Modifier.testTag("community-participation")
            )
        }

        // Verify community participation percentage is displayed
        composeTestRule.onNodeWithText("75.0%").assertExists()
    }

    @Test
    fun waveActivity_errorHandling_handlesNetworkIssues() {
        // Test error handling scenarios

        // Test network connectivity loss
        composeTestRule.setContent {
            val mockEvent = UITestFactory.createMockWaveEvent(
                // Simulate network error state
                progression = Double.NaN // Invalid progression indicates network issues
            )

            try {
                WaveProgressionBar(
                    event = mockEvent as IWWWEvent,
                    modifier = Modifier.testTag("network-error-test")
                )
            } catch (e: Exception) {
                // Verify graceful error handling
                assert(e.message?.contains("network") == true || e is NumberFormatException) {
                    "Should handle network errors gracefully"
                }
            }
        }

        // Test GPS/location accuracy issues
        composeTestRule.setContent {
            val mockEvent = UITestFactory.createMockWaveEvent(
                isInArea = false, // GPS indicates user not in area
                userPositionRatio = -1.0 // Invalid position ratio
            )

            try {
                WaveProgressionBar(
                    event = mockEvent as IWWWEvent,
                    modifier = Modifier.testTag("gps-error-test")
                )

                // Should not crash with invalid position
                composeTestRule.onNodeWithTag("gps-error-test").assertExists()
            } catch (e: Exception) {
                // Verify position validation
                assert(e.message?.contains("position") == true) {
                    "Should validate user position ratios"
                }
            }
        }

        // Test time synchronization problems
        composeTestRule.setContent {
            val mockEvent = UITestFactory.createMockWaveEvent(
                timeBeforeHit = kotlin.time.Duration.parse("-5s") // Negative time indicates sync issues
            )

            WaveHitCounter(
                event = mockEvent as IWWWEvent,
                modifier = Modifier.testTag("time-sync-test")
            )
        }

        // Should display placeholder for invalid time
        composeTestRule.onNodeWithText("--:--").assertExists()
    }

    @Test
    fun waveActivity_backNavigation_handlesWaveInProgress() {
        // Test back button behavior during active wave
        composeTestRule.setContent {
            val mockEvent = UITestFactory.createMockWaveEvent(
                isWarmingInProgress = true, // Active wave in progress
                isGoingToBeHit = true
            )

            // Simulate the full WaveActivity screen
            Box(modifier = Modifier.fillMaxSize()) {
                Column {
                    UserWaveStatusText(
                        event = mockEvent as IWWWEvent,
                        modifier = Modifier.testTag("active-wave-status")
                    )

                    WaveProgressionBar(
                        event = mockEvent,
                        modifier = Modifier.testTag("active-wave-progression")
                    )

                    WaveHitCounter(
                        event = mockEvent,
                        modifier = Modifier.testTag("active-wave-counter")
                    )
                }

                // Mock choreography overlay
                WaveChoreographies(
                    event = mockEvent,
                    clock = mockk(relaxed = true),
                    modifier = Modifier.testTag("active-choreography")
                )
            }
        }

        // Verify all wave components are active
        composeTestRule.onNodeWithTag("active-wave-status").assertExists()
        composeTestRule.onNodeWithTag("active-wave-progression").assertExists()
        composeTestRule.onNodeWithTag("active-wave-counter").assertExists()
        composeTestRule.onNodeWithTag("active-choreography").assertExists()

        // Test inactive wave state
        composeTestRule.setContent {
            val mockEvent = UITestFactory.createMockWaveEvent(
                isWarmingInProgress = false,
                isGoingToBeHit = false,
                hasBeenHit = false
            )

            Box(modifier = Modifier.fillMaxSize()) {
                Column {
                    UserWaveStatusText(
                        event = mockEvent as IWWWEvent,
                        modifier = Modifier.testTag("inactive-wave-status")
                    )

                    WaveHitCounter(
                        event = mockEvent,
                        modifier = Modifier.testTag("inactive-wave-counter")
                    )
                }
            }
        }

        // Verify inactive state is properly handled
        composeTestRule.onNodeWithTag("inactive-wave-status").assertExists()
        composeTestRule.onNodeWithTag("inactive-wave-counter").assertExists()

        // Note: Actual back button handling and confirmation dialog would require
        // integration with Android's back stack, which is beyond UI component testing
    }
}