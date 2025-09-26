package com.worldwidewaves.shared.ui.screens

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

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.worldwidewaves.shared.events.IWWWEvent.Status
import com.worldwidewaves.shared.events.utils.Position
import com.worldwidewaves.shared.events.utils.SystemClock
import com.worldwidewaves.shared.ui.theme.WorldWideWavesTheme
import org.junit.Rule
import org.junit.Test
import kotlin.time.ExperimentalTime

/**
 * Tests for real WaveScreen component
 * Validates wave participation screen behavior and state management
 */
class WaveScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @OptIn(ExperimentalTime::class)
    private val clock = SystemClock()

    @Test
    fun waveScreen_displaysCorrectly() {
        // Test that WaveScreen renders with proper content
        composeTestRule.setContent {
            WorldWideWavesTheme {
                WaveScreen(
                    eventId = "test-wave-event",
                    userPosition = Position(48.8566, 2.3522), // Paris
                    eventStatus = Status.RUNNING,
                    modifier = Modifier.testTag("wave-screen")
                )
            }
        }

        // Verify wave screen is displayed
        composeTestRule.onNodeWithTag("wave-screen").assertIsDisplayed()

        // Verify event information is shown
        composeTestRule.onNodeWithTag("event-info").assertIsDisplayed()

        // Verify wave participation area is displayed
        composeTestRule.onNodeWithTag("wave-participation").assertIsDisplayed()
    }

    @Test
    fun waveScreen_runningEvent_showsActiveState() {
        // Test WaveScreen with running event status
        composeTestRule.setContent {
            WorldWideWavesTheme {
                WaveScreen(
                    eventId = "test-running-event",
                    userPosition = Position(48.8566, 2.3522),
                    eventStatus = Status.RUNNING,
                    modifier = Modifier.testTag("running-wave-screen")
                )
            }
        }

        // Verify running state UI elements
        composeTestRule.onNodeWithTag("running-wave-screen").assertIsDisplayed()

        // Should show wave controls for running events
        composeTestRule.onNodeWithTag("wave-controls").assertIsDisplayed()
    }

    @Test
    fun waveScreen_waitingEvent_showsWaitingState() {
        // Test WaveScreen with waiting event status
        composeTestRule.setContent {
            WorldWideWavesTheme {
                WaveScreen(
                    eventId = "test-waiting-event",
                    userPosition = Position(48.8566, 2.3522),
                    eventStatus = Status.WAITING,
                    modifier = Modifier.testTag("waiting-wave-screen")
                )
            }
        }

        // Verify waiting state UI
        composeTestRule.onNodeWithTag("waiting-wave-screen").assertIsDisplayed()

        // Should show countdown or waiting indicator
        composeTestRule.onNodeWithTag("event-countdown").assertIsDisplayed()
    }
}