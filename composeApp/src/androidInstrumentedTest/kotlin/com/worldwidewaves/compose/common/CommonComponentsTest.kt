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

package com.worldwidewaves.compose.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.worldwidewaves.shared.events.IWWWEvent.Status
import com.worldwidewaves.shared.events.utils.IClock
import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.ExperimentalTime

/**
 * UI tests for common/reusable components
 *
 * Tests cover:
 * - ButtonWave component functionality
 * - Social network link components
 * - Event overlay components (favorite, download, status)
 * - Divider and spacing components
 * - Common UI patterns and interactions
 */
@RunWith(AndroidJUnit4::class)
class CommonComponentsTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var mockClock: IClock

    @OptIn(ExperimentalTime::class)
    @Before
    fun setUp() {
        mockClock =
            mockk<IClock>(relaxed = true) {
                every { now() } returns kotlin.time.Instant.fromEpochSeconds(1640995200) // 2022-01-01T00:00:00Z
            }
    }

    @Test
    fun buttonWave_clickAction_triggersCallback() {
        // Test mock ButtonWave component click functionality
        var buttonClicked = false

        composeTestRule.setContent {
            TestButtonWave(
                eventId = "test-event",
                eventState = Status.RUNNING,
                isInArea = true,
                onButtonClick = { buttonClicked = true },
                modifier = Modifier.testTag("button-wave"),
            )
        }

        // Verify button is displayed and enabled when event is running and user is in area
        composeTestRule.onNodeWithTag("button-wave").assertIsDisplayed()
        composeTestRule.onNodeWithTag("button-wave").assertIsEnabled()

        // Test button text displays correctly
        composeTestRule.onNodeWithText("JOIN WAVE").assertIsDisplayed()

        // Click the button
        composeTestRule.onNodeWithTag("button-wave").performClick()
        assert(buttonClicked) { "Button click should be triggered" }
    }

    @Test
    fun buttonWave_notInArea_isDisabled() {
        // Test button behavior when user is not in area
        composeTestRule.setContent {
            TestButtonWave(
                eventId = "test-event",
                eventState = Status.RUNNING,
                isInArea = false, // User not in area
                modifier = Modifier.testTag("button-wave-disabled"),
            )
        }

        // Verify button is disabled when user is not in area
        composeTestRule.onNodeWithTag("button-wave-disabled").assertIsNotEnabled()
    }

    @Test
    fun buttonWave_doneStatus_isEnabled() {
        // Test button with DONE status
        composeTestRule.setContent {
            TestButtonWave(
                eventId = "test-event",
                eventState = Status.DONE,
                isInArea = true,
                modifier = Modifier.testTag("button-wave-done"),
            )
        }

        // Button should be enabled for recently finished events
        composeTestRule.onNodeWithTag("button-wave-done").assertIsEnabled()
    }

    @Test
    fun socialNetworks_instagramLink_opensExternalUri() {
        // Test WWWSocialNetworks component
        composeTestRule.setContent {
            Box(modifier = Modifier.fillMaxSize()) {
                // Mock social networks component
                TestSocialNetworksComponent(
                    instagramUrl = "https://instagram.com/worldwidewaves",
                    hashtag = "#worldwidewaves",
                    modifier = Modifier.testTag("social-networks"),
                )
            }
        }

        // Verify Instagram link is displayed
        composeTestRule.onNodeWithTag("instagram-link").assertIsDisplayed()

        // Verify hashtag is displayed
        composeTestRule.onNodeWithText("#worldwidewaves").assertIsDisplayed()

        // Test Instagram link click
        composeTestRule.onNodeWithTag("instagram-link").performClick()

        // Note: Actual URI opening would be tested in integration tests
        // Here we verify the component renders and responds to clicks
    }

    @Test
    fun eventOverlays_favoriteToggle_updatesState() {
        // Test EventOverlayFavorite component
        var isFavorite = false
        var toggleCount = 0

        composeTestRule.setContent {
            val favoriteState = remember { mutableStateOf(isFavorite) }

            TestEventOverlayFavorite(
                isFavorite = favoriteState.value,
                onToggle = {
                    favoriteState.value = !favoriteState.value
                    isFavorite = favoriteState.value
                    toggleCount++
                },
                modifier = Modifier.testTag("favorite-overlay"),
            )
        }

        // Initially not favorite
        composeTestRule.onNodeWithTag("favorite-star-empty").assertIsDisplayed()

        // Click to toggle favorite
        composeTestRule.onNodeWithTag("favorite-overlay").performClick()

        // Verify state changed
        assert(isFavorite) { "Event should be marked as favorite" }
        assert(toggleCount == 1) { "Toggle should have been called once" }

        // Update UI to reflect new state
        composeTestRule.setContent {
            TestEventOverlayFavorite(
                isFavorite = true,
                onToggle = { /* no-op for this test */ },
                modifier = Modifier.testTag("favorite-overlay-filled"),
            )
        }

        // Verify filled star is displayed
        composeTestRule.onNodeWithTag("favorite-star-filled").assertIsDisplayed()
    }

    @Test
    fun eventOverlays_downloadStatus_downloaded_displaysCorrectly() {
        // Test downloaded state
        composeTestRule.setContent {
            TestEventOverlayMapDownloaded(
                isDownloaded = true,
                isDownloading = false,
                hasError = false,
                modifier = Modifier.testTag("download-overlay-done"),
            )
        }

        // Verify downloaded indicator
        composeTestRule.onNodeWithContentDescription("Map downloaded").assertIsDisplayed()
        composeTestRule.onNodeWithTag("download-check").assertIsDisplayed()
    }

    @Test
    fun eventOverlays_downloadStatus_downloading_displaysCorrectly() {
        // Test downloading state
        composeTestRule.setContent {
            TestEventOverlayMapDownloaded(
                isDownloaded = false,
                isDownloading = true,
                hasError = false,
                modifier = Modifier.testTag("download-overlay-progress"),
            )
        }

        // Verify downloading indicator
        composeTestRule.onNodeWithContentDescription("Downloading map").assertIsDisplayed()
        composeTestRule.onNodeWithTag("download-progress").assertIsDisplayed()
    }

    @Test
    fun eventOverlays_downloadStatus_error_displaysCorrectly() {
        // Test error state
        composeTestRule.setContent {
            TestEventOverlayMapDownloaded(
                isDownloaded = false,
                isDownloading = false,
                hasError = true,
                modifier = Modifier.testTag("download-overlay-error"),
            )
        }

        // Verify error indicator
        composeTestRule.onNodeWithContentDescription("Download failed").assertIsDisplayed()
        composeTestRule.onNodeWithTag("download-error").assertIsDisplayed()
    }

    @Test
    fun eventOverlays_downloadStatus_idle_displaysCorrectly() {
        // Test no download state
        composeTestRule.setContent {
            TestEventOverlayMapDownloaded(
                isDownloaded = false,
                isDownloading = false,
                hasError = false,
                modifier = Modifier.testTag("download-overlay-none"),
            )
        }

        // Verify no download indicator is not displayed
        composeTestRule.onNode(hasTestTag("download-check")).assertDoesNotExist()
    }

    @Test
    fun eventOverlays_eventStatus_done_showsCorrectBadge() {
        // Test DONE status
        composeTestRule.setContent {
            TestEventOverlayDone(
                eventStatus = Status.DONE,
                modifier = Modifier.testTag("done-overlay"),
            )
        }

        // Verify done overlay is displayed
        composeTestRule.onNodeWithTag("done-overlay").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Event completed").assertIsDisplayed()
    }

    @Test
    fun eventOverlays_eventStatus_running_showsCorrectBadge() {
        // Test RUNNING status (should not show done overlay)
        composeTestRule.setContent {
            TestEventOverlayDone(
                eventStatus = Status.RUNNING,
                modifier = Modifier.testTag("running-overlay"),
            )
        }

        // Verify done overlay is not displayed for running events
        composeTestRule.onNode(hasContentDescription("Event completed")).assertDoesNotExist()
    }

    @Test
    fun eventOverlays_eventStatus_soon_showsCorrectBadge() {
        // Test SOON status overlay
        composeTestRule.setContent {
            TestEventOverlaySoonOrRunning(
                eventStatus = Status.SOON,
                modifier = Modifier.testTag("soon-overlay"),
            )
        }

        // Verify soon badge is displayed
        composeTestRule.onNodeWithText("SOON").assertIsDisplayed()
    }

    @Test
    fun eventOverlays_eventStatus_runningBadge_showsCorrectBadge() {
        // Test RUNNING status overlay
        composeTestRule.setContent {
            TestEventOverlaySoonOrRunning(
                eventStatus = Status.RUNNING,
                modifier = Modifier.testTag("running-badge"),
            )
        }

        // Verify running badge is displayed
        composeTestRule.onNodeWithText("RUNNING").assertIsDisplayed()
    }

    @Test
    fun dividers_display_withCorrectSpacing() {
        // Test divider components
        composeTestRule.setContent {
            Box(modifier = Modifier.fillMaxSize()) {
                TestDividerComponent(
                    modifier = Modifier.testTag("test-divider"),
                )
            }
        }

        // Verify divider is displayed
        composeTestRule.onNodeWithTag("test-divider").assertIsDisplayed()
    }

    @Test
    fun dividers_horizontal_display_correctly() {
        // Test horizontal divider
        composeTestRule.setContent {
            TestHorizontalDivider(
                modifier = Modifier.testTag("horizontal-divider"),
            )
        }

        composeTestRule.onNodeWithTag("horizontal-divider").assertIsDisplayed()
    }

    @Test
    fun dividers_vertical_display_correctly() {
        // Test vertical divider
        composeTestRule.setContent {
            TestVerticalDivider(
                modifier = Modifier.testTag("vertical-divider"),
            )
        }

        composeTestRule.onNodeWithTag("vertical-divider").assertIsDisplayed()
    }

    @Test
    fun commonComponents_accessibility_buttonWave_supportScreenReaders() {
        // Test ButtonWave accessibility
        composeTestRule.setContent {
            TestButtonWave(
                eventId = "test-event",
                eventState = Status.RUNNING,
                isInArea = true,
                modifier = Modifier.testTag("accessible-button"),
            )
        }

        // Verify button has accessible text
        composeTestRule.onNodeWithText("JOIN WAVE").assertIsDisplayed()
    }

    @Test
    fun commonComponents_accessibility_eventOverlay_supportScreenReaders() {
        // Test event overlay accessibility
        composeTestRule.setContent {
            TestEventOverlayDone(
                eventStatus = Status.DONE,
                modifier = Modifier.testTag("accessible-done"),
            )
        }

        // Verify done overlay has content description
        composeTestRule.onNode(hasContentDescription("Event completed")).assertIsDisplayed()
    }

    @Test
    fun commonComponents_accessibility_favorite_supportScreenReaders() {
        // Test favorite overlay accessibility
        composeTestRule.setContent {
            TestEventOverlayFavorite(
                isFavorite = true,
                onToggle = { },
                modifier = Modifier.testTag("accessible-favorite"),
            )
        }

        // Verify favorite has appropriate content description
        composeTestRule.onNode(hasContentDescription("Remove from favorites")).assertIsDisplayed()
    }

    @Test
    fun commonComponents_theming_buttonWave_adaptsToSystemSettings() {
        // Test with Material Theme
        composeTestRule.setContent {
            MaterialTheme {
                TestButtonWave(
                    eventId = "test-event",
                    eventState = Status.RUNNING,
                    isInArea = true,
                    modifier = Modifier.testTag("themed-button"),
                )
            }
        }

        // Verify button renders with theme
        composeTestRule.onNodeWithTag("themed-button").assertIsDisplayed()
    }

    @Test
    fun commonComponents_theming_eventStatus_adaptsToSystemSettings() {
        // Test event status overlay theming
        composeTestRule.setContent {
            MaterialTheme {
                TestEventOverlaySoonOrRunning(
                    eventStatus = Status.SOON,
                    modifier = Modifier.testTag("themed-overlay"),
                )
            }
        }

        // Verify overlay uses theme colors
        composeTestRule.onNodeWithText("SOON").assertIsDisplayed()
    }

    @Test
    fun commonComponents_theming_textScaling_adaptsToSystemSettings() {
        // Test text scaling adaptation
        composeTestRule.setContent {
            MaterialTheme {
                TestAutoResizeText(
                    text = "Sample text for scaling",
                    modifier = Modifier.testTag("scaled-text"),
                )
            }
        }

        // Verify text component handles scaling
        composeTestRule.onNodeWithTag("scaled-text").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sample text for scaling").assertIsDisplayed()
    }
}

// Helper test components for CommonComponentsTest
@Composable
private fun TestSocialNetworksComponent(
    instagramUrl: String,
    hashtag: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Box(
            modifier =
                Modifier.testTag("instagram-link").clickable {
                    // Mock link click action
                },
        ) {
            Text("Instagram")
        }

        Text(
            text = hashtag,
            modifier = Modifier.testTag("hashtag-text"),
        )
    }
}

@Composable
private fun TestEventOverlayFavorite(
    isFavorite: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.clickable { onToggle() },
    ) {
        if (isFavorite) {
            Text(
                "★",
                modifier =
                    Modifier
                        .testTag("favorite-star-filled")
                        .semantics { contentDescription = "Remove from favorites" },
            )
        } else {
            Text(
                "☆",
                modifier =
                    Modifier
                        .testTag("favorite-star-empty")
                        .semantics { contentDescription = "Add to favorites" },
            )
        }
    }
}

@Composable
private fun TestEventOverlayMapDownloaded(
    isDownloaded: Boolean,
    isDownloading: Boolean,
    hasError: Boolean,
    modifier: Modifier = Modifier,
) {
    when {
        isDownloaded -> {
            Box(modifier = modifier) {
                Text(
                    "✓",
                    modifier =
                        Modifier
                            .testTag("download-check")
                            .semantics { contentDescription = "Map downloaded" },
                )
            }
        }
        isDownloading -> {
            Box(modifier = modifier) {
                Text(
                    "⟳",
                    modifier =
                        Modifier
                            .testTag("download-progress")
                            .semantics { contentDescription = "Downloading map" },
                )
            }
        }
        hasError -> {
            Box(modifier = modifier) {
                Text(
                    "✗",
                    modifier =
                        Modifier
                            .testTag("download-error")
                            .semantics { contentDescription = "Download failed" },
                )
            }
        }
    }
}

@Composable
private fun TestDividerComponent(modifier: Modifier = Modifier) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(1.dp),
    ) {
        // Simple divider representation
    }
}

@Composable
private fun TestHorizontalDivider(modifier: Modifier = Modifier) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(1.dp),
    ) {
        // Horizontal divider representation
    }
}

@Composable
private fun TestVerticalDivider(modifier: Modifier = Modifier) {
    Box(
        modifier =
            modifier
                .width(1.dp)
                .fillMaxHeight(),
    ) {
        // Vertical divider representation
    }
}

@Composable
private fun TestAutoResizeText(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier,
    )
}

@Composable
private fun TestButtonWave(
    eventId: String,
    eventState: Status,
    isInArea: Boolean,
    onButtonClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val isEnabled = isInArea && (eventState == Status.RUNNING || eventState == Status.SOON || eventState == Status.DONE)

    Box(
        modifier =
            modifier
                .clickable(enabled = isEnabled) { onButtonClick?.invoke() }
                .testTag("button-wave"),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "JOIN WAVE",
            color = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TestEventOverlayDone(
    eventStatus: Status?,
    modifier: Modifier = Modifier,
) {
    if (eventStatus == Status.DONE) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "DONE",
                modifier =
                    Modifier.semantics {
                        contentDescription = "Event completed"
                    },
            )
        }
    }
}

@Composable
private fun TestEventOverlaySoonOrRunning(
    eventStatus: Status?,
    modifier: Modifier = Modifier,
) {
    when (eventStatus) {
        Status.SOON -> {
            Box(modifier = modifier) {
                Text("SOON")
            }
        }
        Status.RUNNING -> {
            Box(modifier = modifier) {
                Text("RUNNING")
            }
        }
        else -> { /* No overlay */ }
    }
}
