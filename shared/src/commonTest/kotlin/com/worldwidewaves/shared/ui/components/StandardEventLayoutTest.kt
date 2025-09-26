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
import androidx.compose.ui.test.performClick
import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.events.IWWWEvent.Status
import com.worldwidewaves.shared.events.WWWEvent
import com.worldwidewaves.shared.events.utils.Position
import com.worldwidewaves.shared.map.MapFeatureState
import com.worldwidewaves.shared.ui.theme.WorldWideWavesTheme
import org.junit.Rule
import org.junit.Test
import kotlin.time.ExperimentalTime

/**
 * Tests for real StandardEventLayout component
 * Validates standard event layout pattern with real event data
 */
class StandardEventLayoutTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @OptIn(ExperimentalTime::class)
    private fun createTestEvent(status: Status): IWWWEvent {
        return WWWEvent(
            id = "test-layout-event",
            type = "city",
            name = "Test Event Layout",
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
    fun standardEventLayout_runningEvent_displaysAllComponents() {
        // Test standard layout with running event
        val runningEvent = createTestEvent(Status.RUNNING)
        var navigationTriggered = false

        composeTestRule.setContent {
            WorldWideWavesTheme {
                StandardEventLayout(
                    event = runningEvent,
                    mapFeatureState = MapFeatureState.Available,
                    onNavigateToWave = { navigationTriggered = true },
                    onNavigateToMap = { },
                    onNavigateToFullMap = { },
                    onToggleFavorite = { },
                    onCancelDownload = { },
                    userPosition = Position(48.8566, 2.3522),
                    isInArea = true,
                    isFavorite = false,
                    modifier = Modifier.testTag("standard-layout")
                )
            }
        }

        // Verify main layout components are displayed
        composeTestRule.onNodeWithTag("standard-layout").assertIsDisplayed()

        // Should show event title
        composeTestRule.onNodeWithText("Test Event Layout").assertIsDisplayed()

        // Should show event status
        composeTestRule.onNodeWithTag("event-status").assertIsDisplayed()

        // Should show wave button for running events
        composeTestRule.onNodeWithTag("wave-button").assertIsDisplayed()

        // Should show map preview
        composeTestRule.onNodeWithTag("map-preview").assertIsDisplayed()
    }

    @Test
    fun standardEventLayout_waitingEvent_showsWaitingState() {
        // Test standard layout with waiting event
        val waitingEvent = createTestEvent(Status.WAITING)

        composeTestRule.setContent {
            WorldWideWavesTheme {
                StandardEventLayout(
                    event = waitingEvent,
                    mapFeatureState = MapFeatureState.Available,
                    onNavigateToWave = { },
                    onNavigateToMap = { },
                    onNavigateToFullMap = { },
                    onToggleFavorite = { },
                    onCancelDownload = { },
                    userPosition = Position(48.8566, 2.3522),
                    isInArea = true,
                    isFavorite = false,
                    modifier = Modifier.testTag("waiting-layout")
                )
            }
        }

        // Should show waiting state
        composeTestRule.onNodeWithTag("waiting-layout").assertIsDisplayed()
        composeTestRule.onNodeWithTag("event-countdown").assertIsDisplayed()
    }

    @Test
    fun standardEventLayout_favoriteToggle_triggersCallback() {
        // Test favorite functionality
        val testEvent = createTestEvent(Status.RUNNING)
        var favoriteToggled = false

        composeTestRule.setContent {
            WorldWideWavesTheme {
                StandardEventLayout(
                    event = testEvent,
                    mapFeatureState = MapFeatureState.Available,
                    onNavigateToWave = { },
                    onNavigateToMap = { },
                    onNavigateToFullMap = { },
                    onToggleFavorite = { favoriteToggled = true },
                    onCancelDownload = { },
                    userPosition = Position(48.8566, 2.3522),
                    isInArea = true,
                    isFavorite = false,
                    modifier = Modifier.testTag("favorite-layout")
                )
            }
        }

        // Click favorite button
        composeTestRule.onNodeWithTag("favorite-button").performClick()
        assert(favoriteToggled) { "Favorite toggle should be triggered" }
    }

    @Test
    fun standardEventLayout_mapDownloading_showsProgress() {
        // Test layout with downloading map state
        val testEvent = createTestEvent(Status.RUNNING)

        composeTestRule.setContent {
            WorldWideWavesTheme {
                StandardEventLayout(
                    event = testEvent,
                    mapFeatureState = MapFeatureState.Downloading(50),
                    onNavigateToWave = { },
                    onNavigateToMap = { },
                    onNavigateToFullMap = { },
                    onToggleFavorite = { },
                    onCancelDownload = { },
                    userPosition = Position(48.8566, 2.3522),
                    isInArea = true,
                    isFavorite = false,
                    modifier = Modifier.testTag("downloading-layout")
                )
            }
        }

        // Should show download progress
        composeTestRule.onNodeWithTag("downloading-layout").assertIsDisplayed()
        composeTestRule.onNodeWithTag("download-progress").assertIsDisplayed()
        composeTestRule.onNodeWithText("50%").assertIsDisplayed()
    }
}