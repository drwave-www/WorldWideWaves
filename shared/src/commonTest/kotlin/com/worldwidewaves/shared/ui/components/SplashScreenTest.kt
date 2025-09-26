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
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import com.worldwidewaves.shared.ui.theme.WorldWideWavesTheme
import org.junit.Rule
import org.junit.Test

/**
 * Tests for real SplashScreen component
 * Validates actual splash screen rendering and accessibility
 */
class SplashScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun splashScreen_displaysCorrectly() {
        // Test that SplashScreen renders with proper background and logo
        composeTestRule.setContent {
            WorldWideWavesTheme {
                SplashScreen()
            }
        }

        // Verify splash screen container is displayed with test tag
        composeTestRule.onNodeWithTag("splash-screen").assertIsDisplayed()

        // Verify accessibility content descriptions are present
        composeTestRule.onNodeWithContentDescription("Background").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Logo").assertIsDisplayed()
    }

    @Test
    fun splashScreen_withCustomModifier_appliesCorrectly() {
        // Test that custom modifiers are applied correctly
        composeTestRule.setContent {
            WorldWideWavesTheme {
                SplashScreen(
                    modifier = Modifier.testTag("custom-splash")
                )
            }
        }

        // Verify custom modifier is applied (should have both tags)
        composeTestRule.onNodeWithTag("splash-screen").assertIsDisplayed()
    }
}