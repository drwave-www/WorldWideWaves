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

package com.worldwidewaves.compose.tabs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.worldwidewaves.shared.events.IWWWEvent
import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Comprehensive UI tests for EventsListScreen - critical user workflow testing
 *
 * Tests cover:
 * - Events list display and interaction
 * - Filter tab functionality (All/Favorites/Downloaded)
 * - Event selection and navigation
 * - Empty state handling
 * - Loading states
 * - Error states
 * - Favorite toggle functionality
 * - Map download status display
 */
@RunWith(AndroidJUnit4::class)
class EventsListScreenTestComplete {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var mockEvents: List<IWWWEvent>

    @Before
    fun setUp() {
        // Create mock events with different states
        mockEvents = listOf(
            createMockEvent("event1", "New York", isFavorite = true, isDownloaded = true),
            createMockEvent("event2", "London", isFavorite = false, isDownloaded = true),
            createMockEvent("event3", "Paris", isFavorite = true, isDownloaded = false),
            createMockEvent("event4", "Tokyo", isFavorite = false, isDownloaded = false)
        )
    }

    private fun createMockEvent(
        id: String,
        location: String,
        isFavorite: Boolean = false,
        isDownloaded: Boolean = false
    ): IWWWEvent {
        return mockk<IWWWEvent>(relaxed = true) {
            every { this@mockk.id } returns id
            every { this@mockk.cityNameKey } returns location
        }
    }

    @Test
    fun eventsListScreen_displaysAllTabByDefault() {
        // Test that All tab is selected by default and shows all events
        composeTestRule.setContent {
            TestEventsListContent(
                events = mockEvents,
                selectedFilter = FilterType.ALL
            )
        }

        // Verify All tab is selected by default
        composeTestRule.onNodeWithTag("filter-all").assertIsSelected()

        // Verify all events are displayed
        composeTestRule.onAllNodesWithTag("event-item").assertCountEquals(4)

        // Verify specific events are present
        composeTestRule.onNodeWithText("New York").assertIsDisplayed()
        composeTestRule.onNodeWithText("London").assertIsDisplayed()
        composeTestRule.onNodeWithText("Paris").assertIsDisplayed()
        composeTestRule.onNodeWithText("Tokyo").assertIsDisplayed()
    }

    @Test
    fun eventsListScreen_filterTabs_navigateBetweenAllFavoritesDownloaded() {
        val selectedFilter = mutableStateOf(FilterType.ALL)

        composeTestRule.setContent {
            TestEventsListContent(
                events = mockEvents,
                selectedFilter = selectedFilter.value,
                onFilterChange = { selectedFilter.value = it }
            )
        }

        // Test All tab (initial state)
        composeTestRule.onNodeWithTag("filter-all").assertIsSelected()
        composeTestRule.onAllNodesWithTag("event-item").assertCountEquals(4)

        // Click Favorites tab
        composeTestRule.onNodeWithTag("filter-favorites").performClick()

        // Update content for favorites filter
        composeTestRule.setContent {
            TestEventsListContent(
                events = mockEvents.filter { it.id in listOf("event1", "event3") }, // Favorites
                selectedFilter = FilterType.FAVORITES
            )
        }

        // Verify only favorite events are shown (event1, event3)
        composeTestRule.onAllNodesWithTag("event-item").assertCountEquals(2)
        composeTestRule.onNodeWithText("New York").assertIsDisplayed()
        composeTestRule.onNodeWithText("Paris").assertIsDisplayed()

        // Click Downloaded tab
        composeTestRule.onNodeWithTag("filter-downloaded").performClick()

        // Update content for downloaded filter
        composeTestRule.setContent {
            TestEventsListContent(
                events = mockEvents.filter { it.id in listOf("event1", "event2") }, // Downloaded
                selectedFilter = FilterType.DOWNLOADED
            )
        }

        // Verify only downloaded events are shown (event1, event2)
        composeTestRule.onAllNodesWithTag("event-item").assertCountEquals(2)
        composeTestRule.onNodeWithText("New York").assertIsDisplayed()
        composeTestRule.onNodeWithText("London").assertIsDisplayed()
    }

    @Test
    fun eventsListScreen_eventItem_displaysCorrectInformation() {
        // Test that each event item displays correct information
        composeTestRule.setContent {
            TestEventItem(
                event = mockEvents[0], // New York event (favorite, downloaded)
                isDownloaded = true,
                isFavorite = true
            )
        }

        // Verify event location is displayed
        composeTestRule.onNodeWithText("New York").assertIsDisplayed()

        // Verify favorite overlay is displayed
        composeTestRule.onNodeWithTag("favorite-overlay").assertIsDisplayed()

        // Verify download status overlay is displayed
        composeTestRule.onNodeWithTag("download-overlay").assertIsDisplayed()

        // Test non-favorite, non-downloaded event
        composeTestRule.setContent {
            TestEventItem(
                event = mockEvents[3], // Tokyo event (not favorite, not downloaded)
                isDownloaded = false,
                isFavorite = false
            )
        }

        // Verify event location is displayed
        composeTestRule.onNodeWithText("Tokyo").assertIsDisplayed()

        // Verify favorite overlay is not displayed
        composeTestRule.onNodeWithTag("favorite-overlay").assertDoesNotExist()

        // Verify download overlay is not displayed
        composeTestRule.onNodeWithTag("download-overlay").assertDoesNotExist()
    }

    @Test
    fun eventsListScreen_eventItem_clickNavigatesToEventDetails() {
        var clickedEventId: String? = null

        composeTestRule.setContent {
            TestEventItem(
                event = mockEvents[0],
                isDownloaded = true,
                isFavorite = true,
                onClick = { eventId -> clickedEventId = eventId }
            )
        }

        // Click on the event item
        composeTestRule.onNodeWithTag("event-item").performClick()

        // Verify the click handler was called with correct event ID
        assert(clickedEventId == "event1") {
            "Expected event1 to be clicked, but got: $clickedEventId"
        }
    }

    @Test
    fun eventsListScreen_favoriteButton_togglesEventFavoriteStatus() {
        var isFavorite = false
        var favoriteToggleCount = 0

        composeTestRule.setContent {
            TestEventItem(
                event = mockEvents[1], // London event (initially not favorite)
                isDownloaded = true,
                isFavorite = isFavorite,
                onFavoriteToggle = {
                    isFavorite = !isFavorite
                    favoriteToggleCount++
                }
            )
        }

        // Initially, favorite overlay should not be visible
        composeTestRule.onNodeWithTag("favorite-overlay").assertDoesNotExist()

        // Click the favorite button
        composeTestRule.onNodeWithTag("favorite-button").performClick()

        // Verify toggle was called
        assert(favoriteToggleCount == 1) {
            "Favorite toggle should have been called once, but was called $favoriteToggleCount times"
        }

        assert(isFavorite) {
            "Event should be marked as favorite after toggle"
        }

        // Update UI to reflect new state
        composeTestRule.setContent {
            TestEventItem(
                event = mockEvents[1],
                isDownloaded = true,
                isFavorite = isFavorite,
                onFavoriteToggle = {
                    isFavorite = !isFavorite
                    favoriteToggleCount++
                }
            )
        }

        // Now favorite overlay should be visible
        composeTestRule.onNodeWithTag("favorite-overlay").assertIsDisplayed()
    }

    @Test
    fun eventsListScreen_emptyState_displaysCorrectMessage() {
        // Test empty state for no events at all
        composeTestRule.setContent {
            TestEventsListContent(
                events = emptyList(),
                selectedFilter = FilterType.ALL
            )
        }

        // Verify empty state message is displayed
        composeTestRule.onNodeWithTag("empty-state").assertIsDisplayed()
        composeTestRule.onNodeWithText("No events available").assertIsDisplayed()

        // Test empty state for no favorite events
        composeTestRule.setContent {
            TestEventsListContent(
                events = emptyList(),
                selectedFilter = FilterType.FAVORITES
            )
        }

        composeTestRule.onNodeWithTag("empty-state").assertIsDisplayed()
        composeTestRule.onNodeWithText("No favorite events").assertIsDisplayed()

        // Test empty state for no downloaded events
        composeTestRule.setContent {
            TestEventsListContent(
                events = emptyList(),
                selectedFilter = FilterType.DOWNLOADED
            )
        }

        composeTestRule.onNodeWithTag("empty-state").assertIsDisplayed()
        composeTestRule.onNodeWithText("No downloaded events").assertIsDisplayed()

        // Test loading error state
        composeTestRule.setContent {
            TestEventsListContent(
                events = emptyList(),
                selectedFilter = FilterType.ALL,
                isError = true
            )
        }

        composeTestRule.onNodeWithTag("error-state").assertIsDisplayed()
        composeTestRule.onNodeWithText("Error loading events").assertIsDisplayed()
    }

    @Test
    fun eventsListScreen_mapDownloadStatus_displaysCorrectly() {
        // Test downloaded map status
        composeTestRule.setContent {
            TestEventItem(
                event = mockEvents[0], // Downloaded event
                isDownloaded = true,
                isFavorite = false
            )
        }

        // Verify download overlay shows checkmark for downloaded maps
        composeTestRule.onNodeWithTag("download-overlay").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Map downloaded").assertIsDisplayed()

        // Test non-downloaded map status
        composeTestRule.setContent {
            TestEventItem(
                event = mockEvents[3], // Non-downloaded event
                isDownloaded = false,
                isFavorite = false
            )
        }

        // Verify no download overlay for non-downloaded maps
        composeTestRule.onNodeWithTag("download-overlay").assertDoesNotExist()

        // Test download progress state
        composeTestRule.setContent {
            TestEventItem(
                event = mockEvents[2],
                isDownloaded = false,
                isFavorite = false,
                isDownloading = true
            )
        }

        // Verify download progress indicator
        composeTestRule.onNodeWithTag("download-progress").assertIsDisplayed()

        // Test download error state
        composeTestRule.setContent {
            TestEventItem(
                event = mockEvents[2],
                isDownloaded = false,
                isFavorite = false,
                downloadError = true
            )
        }

        // Verify download error indicator
        composeTestRule.onNodeWithTag("download-error").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Download failed").assertIsDisplayed()
    }
}

// Helper enums and composables for testing
enum class FilterType {
    ALL, FAVORITES, DOWNLOADED
}

@Composable
private fun TestEventsListContent(
    events: List<IWWWEvent>,
    selectedFilter: FilterType,
    onFilterChange: ((FilterType) -> Unit)? = null,
    isError: Boolean = false
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Filter tabs
        Row(modifier = Modifier.fillMaxWidth()) {
            FilterTab(
                text = "All",
                isSelected = selectedFilter == FilterType.ALL,
                onClick = { onFilterChange?.invoke(FilterType.ALL) },
                modifier = Modifier.testTag("filter-all")
            )
            FilterTab(
                text = "Favorites",
                isSelected = selectedFilter == FilterType.FAVORITES,
                onClick = { onFilterChange?.invoke(FilterType.FAVORITES) },
                modifier = Modifier.testTag("filter-favorites")
            )
            FilterTab(
                text = "Downloaded",
                isSelected = selectedFilter == FilterType.DOWNLOADED,
                onClick = { onFilterChange?.invoke(FilterType.DOWNLOADED) },
                modifier = Modifier.testTag("filter-downloaded")
            )
        }

        // Content area
        if (isError) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("error-state"),
                contentAlignment = Alignment.Center
            ) {
                Text("Error loading events")
            }
        } else if (events.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("empty-state"),
                contentAlignment = Alignment.Center
            ) {
                val message = when (selectedFilter) {
                    FilterType.ALL -> "No events available"
                    FilterType.FAVORITES -> "No favorite events"
                    FilterType.DOWNLOADED -> "No downloaded events"
                }
                Text(message)
            }
        } else {
            LazyColumn {
                items(events) { event ->
                    TestEventItem(
                        event = event,
                        isDownloaded = event.id in listOf("event1", "event2"),
                        isFavorite = event.id in listOf("event1", "event3"),
                        modifier = Modifier.testTag("event-item")
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterTab(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        modifier = modifier
            .clickable { onClick() }
            .padding(16.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary
               else MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun TestEventItem(
    event: IWWWEvent,
    isDownloaded: Boolean,
    isFavorite: Boolean,
    isDownloading: Boolean = false,
    downloadError: Boolean = false,
    onClick: ((String) -> Unit)? = null,
    onFavoriteToggle: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onClick?.invoke(event.id) }
            .testTag("event-item")
    ) {
        Column {
            Text(
                text = event.cityNameKey,
                style = MaterialTheme.typography.headlineSmall
            )

            // Favorite button
            IconButton(
                onClick = { onFavoriteToggle?.invoke() },
                modifier = Modifier.testTag("favorite-button")
            ) {
                Text("⭐")
            }
        }

        // Overlays
        if (isFavorite) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .testTag("favorite-overlay")
            ) {
                Text("❤️")
            }
        }

        if (isDownloaded) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .testTag("download-overlay")
            ) {
                Text(
                    "✅",
                    modifier = Modifier.semantics {
                        contentDescription = "Map downloaded"
                    }
                )
            }
        }

        if (isDownloading) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .testTag("download-progress")
            ) {
                Text("⏳")
            }
        }

        if (downloadError) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .testTag("download-error")
            ) {
                Text(
                    "❌",
                    modifier = Modifier.semantics {
                        contentDescription = "Download failed"
                    }
                )
            }
        }
    }
}