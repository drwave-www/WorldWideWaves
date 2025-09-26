package com.worldwidewaves.compose.common

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
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.worldwidewaves.shared.events.IWWWEvent.Status
import com.worldwidewaves.shared.events.utils.SystemClock
import com.worldwidewaves.shared.ui.components.ButtonWave
import com.worldwidewaves.shared.ui.components.WaveNavigator
import com.worldwidewaves.shared.ui.theme.WorldWideWavesTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime

/**
 * UI tests for real common/reusable components
 * Tests the actual ButtonWave component with real business logic
 */
@RunWith(AndroidJUnit4::class)
class CommonComponentsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @OptIn(ExperimentalTime::class)
    private val clock = SystemClock()

    @OptIn(ExperimentalTime::class)
    @Test
    fun buttonWave_runningEventInArea_isEnabledAndClickable() {
        // Test real ButtonWave component with RUNNING event when user is in area
        var navigationTriggered = false
        var navigatedEventId = ""

        val navigator = WaveNavigator { eventId ->
            navigationTriggered = true
            navigatedEventId = eventId
        }

        composeTestRule.setContent {
            WorldWideWavesTheme {
                ButtonWave(
                    eventId = "test-running-event",
                    eventState = Status.RUNNING,
                    endDateTime = null,
                    isInArea = true,
                    onNavigateToWave = navigator,
                    modifier = Modifier.testTag("real-button-wave")
                )
            }
        }

        // Verify button is displayed and enabled for running event in area
        composeTestRule.onNodeWithTag("real-button-wave").assertIsDisplayed()
        composeTestRule.onNodeWithTag("real-button-wave").assertIsEnabled()

        // Click the button and verify navigation
        composeTestRule.onNodeWithTag("real-button-wave").performClick()

        assert(navigationTriggered) { "Navigation should be triggered when button is clicked" }
        assert(navigatedEventId == "test-running-event") { "Should navigate to correct event ID" }
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun buttonWave_runningEventNotInArea_isDisabled() {
        // Test real ButtonWave behavior when user is not in event area
        val navigator = WaveNavigator { }

        composeTestRule.setContent {
            WorldWideWavesTheme {
                ButtonWave(
                    eventId = "test-event-not-in-area",
                    eventState = Status.RUNNING,
                    endDateTime = null,
                    isInArea = false, // User not in area
                    onNavigateToWave = navigator,
                    modifier = Modifier.testTag("button-wave-not-in-area")
                )
            }
        }

        // Verify button is disabled when user is not in area
        composeTestRule.onNodeWithTag("button-wave-not-in-area").assertIsDisplayed()
        composeTestRule.onNodeWithTag("button-wave-not-in-area").assertIsNotEnabled()
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun buttonWave_recentEndedEvent_isEnabled() {
        // Test ButtonWave with recently ended event (within 1 hour)
        val navigator = WaveNavigator { }
        val recentEndTime = clock.now() - 30.minutes // 30 minutes ago

        composeTestRule.setContent {
            WorldWideWavesTheme {
                ButtonWave(
                    eventId = "test-recent-ended-event",
                    eventState = Status.DONE,
                    endDateTime = recentEndTime,
                    isInArea = true,
                    onNavigateToWave = navigator,
                    modifier = Modifier.testTag("button-wave-recent-ended")
                )
            }
        }

        // Verify button is enabled for recently ended events
        composeTestRule.onNodeWithTag("button-wave-recent-ended").assertIsDisplayed()
        composeTestRule.onNodeWithTag("button-wave-recent-ended").assertIsEnabled()
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun buttonWave_oldEndedEvent_isDisabled() {
        // Test ButtonWave with event that ended long ago
        val navigator = WaveNavigator { }
        val oldEndTime = clock.now() - 2.hours // 2 hours ago (beyond 1 hour threshold)

        composeTestRule.setContent {
            WorldWideWavesTheme {
                ButtonWave(
                    eventId = "test-old-ended-event",
                    eventState = Status.DONE,
                    endDateTime = oldEndTime,
                    isInArea = true,
                    onNavigateToWave = navigator,
                    modifier = Modifier.testTag("button-wave-old-ended")
                )
            }
        }

        // Verify button is disabled for events that ended long ago
        composeTestRule.onNodeWithTag("button-wave-old-ended").assertIsDisplayed()
        composeTestRule.onNodeWithTag("button-wave-old-ended").assertIsNotEnabled()
    }
}