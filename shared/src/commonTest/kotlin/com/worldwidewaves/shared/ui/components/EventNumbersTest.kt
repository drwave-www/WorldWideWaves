package com.worldwidewaves.shared.ui.components

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
import androidx.compose.ui.test.onNodeWithText
import com.worldwidewaves.shared.ui.theme.WorldWideWavesTheme
import org.junit.Rule
import org.junit.Test

/**
 * Tests for real EventNumbers component
 * Validates event statistics display and formatting
 */
class EventNumbersTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun eventNumbers_displaysParticipantCount() {
        // Test participant count display
        composeTestRule.setContent {
            WorldWideWavesTheme {
                EventNumbers(
                    participantCount = 1250,
                    modifier = Modifier.testTag("event-numbers")
                )
            }
        }

        // Verify component is displayed
        composeTestRule.onNodeWithTag("event-numbers").assertIsDisplayed()

        // Verify participant count is shown
        composeTestRule.onNodeWithText("1,250").assertIsDisplayed()
    }

    @Test
    fun eventNumbers_zeroParticipants_displaysCorrectly() {
        // Test zero participant count display
        composeTestRule.setContent {
            WorldWideWavesTheme {
                EventNumbers(
                    participantCount = 0,
                    modifier = Modifier.testTag("zero-participants")
                )
            }
        }

        // Should display zero appropriately
        composeTestRule.onNodeWithText("0").assertIsDisplayed()
    }

    @Test
    fun eventNumbers_largeNumbers_formatsCorrectly() {
        // Test large number formatting
        composeTestRule.setContent {
            WorldWideWavesTheme {
                EventNumbers(
                    participantCount = 123456,
                    modifier = Modifier.testTag("large-numbers")
                )
            }
        }

        // Should format large numbers with appropriate separators
        composeTestRule.onNodeWithText("123,456").assertIsDisplayed()
    }
}