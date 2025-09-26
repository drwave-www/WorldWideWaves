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
 * Tests for real AboutComponents
 * Validates about screen content display and formatting
 */
class AboutComponentsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun aboutInfo_displaysAppInformation() {
        // Test about info component displays correctly
        composeTestRule.setContent {
            WorldWideWavesTheme {
                AboutInfo(
                    modifier = Modifier.testTag("about-info")
                )
            }
        }

        // Verify about info is displayed
        composeTestRule.onNodeWithTag("about-info").assertIsDisplayed()

        // Should display app name and description
        composeTestRule.onNodeWithText("WorldWideWaves").assertIsDisplayed()
    }

    @Test
    fun aboutVersion_displaysVersionInformation() {
        // Test version component displays correctly
        composeTestRule.setContent {
            WorldWideWavesTheme {
                AboutVersion(
                    version = "1.0.0",
                    buildNumber = "123",
                    modifier = Modifier.testTag("about-version")
                )
            }
        }

        // Verify version information is displayed
        composeTestRule.onNodeWithTag("about-version").assertIsDisplayed()
        composeTestRule.onNodeWithText("1.0.0").assertIsDisplayed()
        composeTestRule.onNodeWithText("123").assertIsDisplayed()
    }

    @Test
    fun aboutLegal_displaysLegalInformation() {
        // Test legal information component
        composeTestRule.setContent {
            WorldWideWavesTheme {
                AboutLegal(
                    modifier = Modifier.testTag("about-legal")
                )
            }
        }

        // Verify legal information is displayed
        composeTestRule.onNodeWithTag("about-legal").assertIsDisplayed()

        // Should display copyright and license information
        composeTestRule.onNodeWithText("Copyright").assertIsDisplayed()
        composeTestRule.onNodeWithText("Apache License").assertIsDisplayed()
    }

    @Test
    fun aboutContact_displaysContactInformation() {
        // Test contact information component
        composeTestRule.setContent {
            WorldWideWavesTheme {
                AboutContact(
                    modifier = Modifier.testTag("about-contact")
                )
            }
        }

        // Verify contact information is displayed
        composeTestRule.onNodeWithTag("about-contact").assertIsDisplayed()

        // Should display contact details
        composeTestRule.onNodeWithText("Contact").assertIsDisplayed()
    }
}