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
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.worldwidewaves.shared.events.IWWWEvent.Status
import com.worldwidewaves.shared.map.MapFeatureState
import com.worldwidewaves.shared.ui.theme.WorldWideWavesTheme
import org.junit.Rule
import org.junit.Test

/**
 * Tests for real EventOverlays components
 * Validates event status badges, favorite functionality, and download states
 */
class EventOverlaysTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun eventOverlayStatus_runningEvent_showsRunningBadge() {
        // Test running event status overlay
        composeTestRule.setContent {
            WorldWideWavesTheme {
                EventOverlayStatus(
                    status = Status.RUNNING,
                    modifier = Modifier.testTag("status-overlay")
                )
            }
        }

        // Verify running status is displayed
        composeTestRule.onNodeWithTag("status-overlay").assertIsDisplayed()
        composeTestRule.onNodeWithText("RUNNING").assertIsDisplayed()
    }

    @Test
    fun eventOverlayStatus_soonEvent_showsSoonBadge() {
        // Test soon event status overlay
        composeTestRule.setContent {
            WorldWideWavesTheme {
                EventOverlayStatus(
                    status = Status.SOON,
                    modifier = Modifier.testTag("soon-status")
                )
            }
        }

        // Verify soon status is displayed
        composeTestRule.onNodeWithText("SOON").assertIsDisplayed()
    }

    @Test
    fun eventOverlayStatus_doneEvent_showsDoneBadge() {
        // Test done event status overlay
        composeTestRule.setContent {
            WorldWideWavesTheme {
                EventOverlayStatus(
                    status = Status.DONE,
                    modifier = Modifier.testTag("done-status")
                )
            }
        }

        // Verify done status is displayed
        composeTestRule.onNodeWithText("DONE").assertIsDisplayed()
    }

    @Test
    fun eventOverlayFavorite_notFavorite_showsEmptyStar() {
        // Test favorite overlay when not favorited
        var favoriteToggled = false

        composeTestRule.setContent {
            WorldWideWavesTheme {
                EventOverlayFavorite(
                    isFavorite = false,
                    onToggle = { favoriteToggled = true },
                    modifier = Modifier.testTag("favorite-overlay")
                )
            }
        }

        // Verify empty star is displayed
        composeTestRule.onNodeWithTag("favorite-overlay").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Add to favorites").assertIsDisplayed()

        // Click to favorite
        composeTestRule.onNodeWithTag("favorite-overlay").performClick()
        assert(favoriteToggled) { "Favorite toggle should be triggered" }
    }

    @Test
    fun eventOverlayFavorite_isFavorite_showsFilledStar() {
        // Test favorite overlay when favorited
        var favoriteToggled = false

        composeTestRule.setContent {
            WorldWideWavesTheme {
                EventOverlayFavorite(
                    isFavorite = true,
                    onToggle = { favoriteToggled = true },
                    modifier = Modifier.testTag("favorite-filled")
                )
            }
        }

        // Verify filled star is displayed
        composeTestRule.onNodeWithContentDescription("Remove from favorites").assertIsDisplayed()

        // Click to unfavorite
        composeTestRule.onNodeWithTag("favorite-filled").performClick()
        assert(favoriteToggled) { "Favorite toggle should be triggered" }
    }

    @Test
    fun eventOverlayDownload_downloading_showsProgress() {
        // Test download overlay during download
        composeTestRule.setContent {
            WorldWideWavesTheme {
                EventOverlayDownload(
                    downloadState = MapFeatureState.Downloading(45),
                    onCancel = { },
                    modifier = Modifier.testTag("download-overlay")
                )
            }
        }

        // Verify download progress is displayed
        composeTestRule.onNodeWithTag("download-overlay").assertIsDisplayed()
        composeTestRule.onNodeWithText("45%").assertIsDisplayed()
    }

    @Test
    fun eventOverlayDownload_completed_showsDownloadedState() {
        // Test download overlay when completed
        composeTestRule.setContent {
            WorldWideWavesTheme {
                EventOverlayDownload(
                    downloadState = MapFeatureState.Installed,
                    onCancel = { },
                    modifier = Modifier.testTag("downloaded-overlay")
                )
            }
        }

        // Verify downloaded state is displayed
        composeTestRule.onNodeWithTag("downloaded-overlay").assertIsDisplayed()
        composeTestRule.onNodeWithText("DOWNLOADED").assertIsDisplayed()
    }

    @Test
    fun eventOverlayDownload_failed_showsErrorState() {
        // Test download overlay when failed
        composeTestRule.setContent {
            WorldWideWavesTheme {
                EventOverlayDownload(
                    downloadState = MapFeatureState.Failed(404, "Map not found"),
                    onCancel = { },
                    modifier = Modifier.testTag("failed-overlay")
                )
            }
        }

        // Verify error state is displayed
        composeTestRule.onNodeWithTag("failed-overlay").assertIsDisplayed()
        composeTestRule.onNodeWithText("ERROR").assertIsDisplayed()
    }
}