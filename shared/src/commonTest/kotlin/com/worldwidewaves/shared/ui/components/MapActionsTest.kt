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
import androidx.compose.ui.test.performClick
import com.worldwidewaves.shared.events.utils.Position
import com.worldwidewaves.shared.ui.theme.WorldWideWavesTheme
import org.junit.Rule
import org.junit.Test

/**
 * Tests for real MapActions component
 * Validates map interaction controls and navigation functionality
 */
class MapActionsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun mapActions_displaysZoomControls() {
        // Test zoom controls display
        composeTestRule.setContent {
            WorldWideWavesTheme {
                MapActions(
                    onZoomIn = { },
                    onZoomOut = { },
                    onCenterUser = { },
                    onToggleMapType = { },
                    modifier = Modifier.testTag("map-actions")
                )
            }
        }

        // Verify zoom controls are displayed
        composeTestRule.onNodeWithTag("map-actions").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Zoom in").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Zoom out").assertIsDisplayed()
    }

    @Test
    fun mapActions_zoomInClick_triggersCallback() {
        // Test zoom in functionality
        var zoomInTriggered = false

        composeTestRule.setContent {
            WorldWideWavesTheme {
                MapActions(
                    onZoomIn = { zoomInTriggered = true },
                    onZoomOut = { },
                    onCenterUser = { },
                    onToggleMapType = { },
                    modifier = Modifier.testTag("zoom-controls")
                )
            }
        }

        // Click zoom in button
        composeTestRule.onNodeWithContentDescription("Zoom in").performClick()
        assert(zoomInTriggered) { "Zoom in callback should be triggered" }
    }

    @Test
    fun mapActions_zoomOutClick_triggersCallback() {
        // Test zoom out functionality
        var zoomOutTriggered = false

        composeTestRule.setContent {
            WorldWideWavesTheme {
                MapActions(
                    onZoomIn = { },
                    onZoomOut = { zoomOutTriggered = true },
                    onCenterUser = { },
                    onToggleMapType = { },
                    modifier = Modifier.testTag("zoom-controls")
                )
            }
        }

        // Click zoom out button
        composeTestRule.onNodeWithContentDescription("Zoom out").performClick()
        assert(zoomOutTriggered) { "Zoom out callback should be triggered" }
    }

    @Test
    fun mapActions_centerUserClick_triggersCallback() {
        // Test center user functionality
        var centerUserTriggered = false

        composeTestRule.setContent {
            WorldWideWavesTheme {
                MapActions(
                    onZoomIn = { },
                    onZoomOut = { },
                    onCenterUser = { centerUserTriggered = true },
                    onToggleMapType = { },
                    modifier = Modifier.testTag("user-controls")
                )
            }
        }

        // Click center user button
        composeTestRule.onNodeWithContentDescription("Center on user").performClick()
        assert(centerUserTriggered) { "Center user callback should be triggered" }
    }

    @Test
    fun mapActions_toggleMapTypeClick_triggersCallback() {
        // Test map type toggle functionality
        var mapTypeToggled = false

        composeTestRule.setContent {
            WorldWideWavesTheme {
                MapActions(
                    onZoomIn = { },
                    onZoomOut = { },
                    onCenterUser = { },
                    onToggleMapType = { mapTypeToggled = true },
                    modifier = Modifier.testTag("map-type-controls")
                )
            }
        }

        // Click map type toggle button
        composeTestRule.onNodeWithContentDescription("Toggle map type").performClick()
        assert(mapTypeToggled) { "Map type toggle callback should be triggered" }
    }
}