package com.worldwidewaves.shared.ui.components.choreographies

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
import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.events.IWWWEvent.Status
import com.worldwidewaves.shared.events.WWWEvent
import com.worldwidewaves.shared.ui.theme.WorldWideWavesTheme
import kotlinx.datetime.TimeZone
import org.junit.Rule
import org.junit.Test
import kotlin.time.ExperimentalTime

/**
 * Tests for real WaveChoreographies component
 * Validates wave animation choreography system and timing
 */
class WaveChoreographiesTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @OptIn(ExperimentalTime::class)
    private fun createTestEvent(status: Status): IWWWEvent {
        return WWWEvent(
            id = "test-choreography-event",
            type = "city",
            name = "Test Wave Event",
            date = "2025-01-01",
            startHour = "18:00",
            timeZone = "UTC",
            country = "France",
            status = status,
            waves = emptyList(),
            instagramAccount = "test",
            instagramHashtag = "#test"
        )
    }

    @Test
    fun waveChoreographies_runningEvent_displaysChoreography() {
        // Test wave choreographies with running event
        val runningEvent = createTestEvent(Status.RUNNING)

        composeTestRule.setContent {
            WorldWideWavesTheme {
                WaveChoreographies(
                    event = runningEvent,
                    modifier = Modifier.testTag("wave-choreographies")
                )
            }
        }

        // Verify choreography component is displayed
        composeTestRule.onNodeWithTag("wave-choreographies").assertIsDisplayed()

        // Should show wave animation elements for running events
        composeTestRule.onNodeWithTag("choreography-animation").assertIsDisplayed()
    }

    @Test
    fun waveChoreographies_waitingEvent_showsCountdown() {
        // Test wave choreographies with waiting event
        val waitingEvent = createTestEvent(Status.WAITING)

        composeTestRule.setContent {
            WorldWideWavesTheme {
                WaveChoreographies(
                    event = waitingEvent,
                    modifier = Modifier.testTag("waiting-choreographies")
                )
            }
        }

        // Verify countdown is displayed for waiting events
        composeTestRule.onNodeWithTag("waiting-choreographies").assertIsDisplayed()
        composeTestRule.onNodeWithTag("event-countdown").assertIsDisplayed()
    }

    @Test
    fun waveChoreographies_doneEvent_showsCompletionState() {
        // Test wave choreographies with completed event
        val doneEvent = createTestEvent(Status.DONE)

        composeTestRule.setContent {
            WorldWideWavesTheme {
                WaveChoreographies(
                    event = doneEvent,
                    modifier = Modifier.testTag("done-choreographies")
                )
            }
        }

        // Verify completion state is displayed
        composeTestRule.onNodeWithTag("done-choreographies").assertIsDisplayed()
        composeTestRule.onNodeWithTag("completion-state").assertIsDisplayed()
    }
}