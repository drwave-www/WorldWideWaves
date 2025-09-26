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
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.worldwidewaves.shared.ui.theme.WorldWideWavesTheme
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Rule
import org.junit.Test

/**
 * Tests for real SimulationButton component
 * Validates simulation mode toggle functionality and state management
 */
class SimulationButtonTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testScope = TestScope(UnconfinedTestDispatcher())

    @Test
    fun simulationButton_initialState_showsCorrectText() {
        // Test initial simulation button state
        composeTestRule.setContent {
            WorldWideWavesTheme {
                SimulationButton(
                    simulationButtonState = "DISABLED",
                    onStateChange = { },
                    scope = testScope,
                    modifier = Modifier.testTag("simulation-button")
                )
            }
        }

        // Verify button is displayed
        composeTestRule.onNodeWithTag("simulation-button").assertIsDisplayed()

        // Verify correct text is shown for disabled state
        composeTestRule.onNodeWithText("Enable Simulation").assertIsDisplayed()
    }

    @Test
    fun simulationButton_enabledState_showsCorrectText() {
        // Test enabled simulation button state
        composeTestRule.setContent {
            WorldWideWavesTheme {
                SimulationButton(
                    simulationButtonState = "ENABLED",
                    onStateChange = { },
                    scope = testScope,
                    modifier = Modifier.testTag("simulation-button-enabled")
                )
            }
        }

        // Verify enabled state shows correct text
        composeTestRule.onNodeWithText("Disable Simulation").assertIsDisplayed()
    }

    @Test
    fun simulationButton_clickAction_triggersStateChange() {
        // Test button click triggers state change callback
        var stateChangeTriggered = false
        var newState = ""

        composeTestRule.setContent {
            WorldWideWavesTheme {
                SimulationButton(
                    simulationButtonState = "DISABLED",
                    onStateChange = { state ->
                        stateChangeTriggered = true
                        newState = state
                    },
                    scope = testScope,
                    modifier = Modifier.testTag("clickable-simulation-button")
                )
            }
        }

        // Click the button
        composeTestRule.onNodeWithTag("clickable-simulation-button").performClick()

        // Verify state change was triggered
        assert(stateChangeTriggered) { "State change should be triggered when button is clicked" }
        assert(newState == "ENABLED") { "State should change from DISABLED to ENABLED" }
    }
}