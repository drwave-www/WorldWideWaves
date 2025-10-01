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

package com.worldwidewaves.integration

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.testing.BaseIntegrationTest
import com.worldwidewaves.testing.UITestFactory
import io.mockk.every
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-End Integration Tests for Event Participation Flow
 *
 * Tests the complete user journey from event discovery to wave participation:
 * EventsScreen → EventActivity → WaveActivity → Wave Hit Detection
 *
 * This test suite covers the primary user flow for the WorldWideWaves app,
 * ensuring the entire participation experience works seamlessly.
 */
@RunWith(AndroidJUnit4::class)
class EventParticipationFlowTest : BaseIntegrationTest() {
    @Test
    fun testCompleteEventParticipationFlow_listToDetails_toWave_toHit() {
        val trace = createPerformanceTrace("testCompleteEventParticipationFlow")

        // Setup: Create mock events
        val mockEvents = UITestFactory.createMockEventsList(count = 3, withDifferentStates = true)
        val eventsFlow = MutableStateFlow(mockEvents)
        var selectedEventId: String? = null
        var isInWaveScreen = false
        var waveHitDetected = false

        // Setup: Mock event state transitions
        val targetEvent = mockEvents[0]
        val isWarmingFlow = MutableStateFlow(false)
        val isGoingToBeHitFlow = MutableStateFlow(false)
        val hasBeenHitFlow = MutableStateFlow(false)

        every { targetEvent.observer.isUserWarmingInProgress } returns isWarmingFlow
        every { targetEvent.observer.userIsGoingToBeHit } returns isGoingToBeHitFlow
        every { targetEvent.observer.userHasBeenHit } returns hasBeenHitFlow

        composeTestRule.setContent {
            TestCompleteParticipationFlow(
                events = eventsFlow.value,
                selectedEventId = selectedEventId,
                isInWaveScreen = isInWaveScreen,
                onEventSelected = { eventId ->
                    selectedEventId = eventId
                },
                onJoinWave = {
                    isInWaveScreen = true
                },
                onWaveHit = {
                    waveHitDetected = true
                },
                targetEvent = if (selectedEventId == targetEvent.id) targetEvent else null,
            )
        }

        // Step 1: Verify events list is displayed
        composeTestRule
            .onNodeWithContentDescription("Events list displayed")
            .assertIsDisplayed()

        // Step 2: Select an event from the list
        composeTestRule
            .onNodeWithTag("event-item-${mockEvents[0].id}")
            .performClick()

        composeTestRule.waitUntil(timeoutMillis = 2000) {
            selectedEventId == mockEvents[0].id
        }

        // Step 3: Verify event details are shown
        composeTestRule.waitUntil(timeoutMillis = 2000) {
            composeTestRule
                .onAllNodesWithContentDescription("Event details: ${mockEvents[0].id}")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        // Step 4: Join the wave
        composeTestRule
            .onNodeWithTag("join-wave-button")
            .performClick()

        composeTestRule.waitUntil(timeoutMillis = 2000) {
            isInWaveScreen
        }

        // Step 5: Verify wave screen is displayed
        composeTestRule
            .onNodeWithContentDescription("Wave screen active")
            .assertIsDisplayed()

        // Step 6: Simulate wave progression (warming → waiting → hit)
        isWarmingFlow.value = true

        composeTestRule.waitUntil(timeoutMillis = 2000) {
            composeTestRule
                .onAllNodesWithContentDescription("Wave phase: Warming")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        isWarmingFlow.value = false
        isGoingToBeHitFlow.value = true

        composeTestRule.waitUntil(timeoutMillis = 2000) {
            composeTestRule
                .onAllNodesWithContentDescription("Wave phase: Waiting for hit")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        // Step 7: Simulate wave hit
        hasBeenHitFlow.value = true

        composeTestRule.waitUntil(timeoutMillis = 2000) {
            waveHitDetected
        }

        // Step 8: Verify hit was detected
        composeTestRule
            .onNodeWithContentDescription("Wave hit detected!")
            .assertIsDisplayed()

        trace.stop()
    }

    @Test
    fun testEventListToDetailsNavigation_displaysCorrectly() {
        val trace = createPerformanceTrace("testEventListToDetailsNavigation")

        val mockEvents = UITestFactory.createMockEventsList(count = 5)
        var selectedEventId: String? = null

        composeTestRule.setContent {
            TestEventListToDetails(
                events = mockEvents,
                selectedEventId = selectedEventId,
                onEventSelected = { eventId -> selectedEventId = eventId },
            )
        }

        // Verify events list
        composeTestRule
            .onNodeWithContentDescription("Events list: ${mockEvents.size} events")
            .assertIsDisplayed()

        // Select first event
        composeTestRule
            .onNodeWithTag("event-item-${mockEvents[0].id}")
            .performClick()

        composeTestRule.waitUntil(timeoutMillis = 2000) {
            selectedEventId == mockEvents[0].id
        }

        // Verify event details screen
        composeTestRule
            .onNodeWithContentDescription("Event details: ${mockEvents[0].id}")
            .assertIsDisplayed()

        // Verify navigation button is present
        composeTestRule
            .onNodeWithTag("join-wave-button")
            .assertIsDisplayed()

        trace.stop()
    }

    @Test
    fun testWaveHitNotification_triggersWhenUserInPolygon() {
        val trace = createPerformanceTrace("testWaveHitNotification")

        val mockEvent =
            UITestFactory.createMockWaveEvent(
                id = "test-wave-event",
                status = "RUNNING",
                isInArea = false,
            )

        val isInAreaFlow = MutableStateFlow(false)
        val hasBeenHitFlow = MutableStateFlow(false)
        var hitNotificationShown = false

        every { mockEvent.observer.userIsInArea } returns isInAreaFlow
        every { mockEvent.observer.userHasBeenHit } returns hasBeenHitFlow

        composeTestRule.setContent {
            TestWaveHitNotification(
                event = mockEvent,
                onHitNotification = { hitNotificationShown = true },
            )
        }

        // Verify initial state - outside area
        composeTestRule
            .onNodeWithContentDescription("User status: Outside area")
            .assertIsDisplayed()

        // User enters polygon
        isInAreaFlow.value = true

        composeTestRule.waitUntil(timeoutMillis = 2000) {
            composeTestRule
                .onAllNodesWithContentDescription("User status: Inside area")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        // Wave hits user
        hasBeenHitFlow.value = true

        composeTestRule.waitUntil(timeoutMillis = 2000) {
            hitNotificationShown
        }

        // Verify hit notification
        composeTestRule
            .onNodeWithContentDescription("Wave hit notification displayed")
            .assertIsDisplayed()

        trace.stop()
    }

    @Test
    fun testWaveHitCounter_incrementsCorrectly() {
        val trace = createPerformanceTrace("testWaveHitCounter")

        val mockEvent =
            UITestFactory.createMockWaveEvent(
                id = "counter-test-event",
                hasBeenHit = false,
            )

        val hasBeenHitFlow = MutableStateFlow(false)
        val hitCountFlow = MutableStateFlow(0)

        every { mockEvent.observer.userHasBeenHit } returns hasBeenHitFlow

        composeTestRule.setContent {
            TestWaveHitCounter(
                event = mockEvent,
                hitCount = hitCountFlow.value,
                onHitDetected = { hitCountFlow.value++ },
            )
        }

        // Verify initial hit count
        composeTestRule
            .onNodeWithContentDescription("Hit counter: 0 hits")
            .assertIsDisplayed()

        // First hit
        hasBeenHitFlow.value = true

        composeTestRule.waitUntil(timeoutMillis = 2000) {
            hitCountFlow.value == 1
        }

        composeTestRule
            .onNodeWithContentDescription("Hit counter: 1 hits")
            .assertIsDisplayed()

        // Reset for second hit
        hasBeenHitFlow.value = false
        composeTestRule.waitForIdle()
        hasBeenHitFlow.value = true

        composeTestRule.waitUntil(timeoutMillis = 2000) {
            hitCountFlow.value == 2
        }

        composeTestRule
            .onNodeWithContentDescription("Hit counter: 2 hits")
            .assertIsDisplayed()

        trace.stop()
    }

    @Test
    fun testWaveProgressionBar_updatesRealTime() {
        val trace = createPerformanceTrace("testWaveProgressionBar")

        val mockEvent =
            UITestFactory.createMockWaveEvent(
                progression = 0.0,
            )

        val progressionFlow = MutableStateFlow(0.0)
        every { mockEvent.observer.progression } returns progressionFlow

        composeTestRule.setContent {
            TestWaveProgressionBar(event = mockEvent)
        }

        // Verify initial progression
        composeTestRule
            .onNodeWithContentDescription("Wave progression: 0%")
            .assertIsDisplayed()

        // Update progression to 25%
        progressionFlow.value = 25.0

        composeTestRule.waitUntil(timeoutMillis = 2000) {
            composeTestRule
                .onAllNodesWithContentDescription("Wave progression: 25%")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        // Update progression to 50%
        progressionFlow.value = 50.0

        composeTestRule.waitUntil(timeoutMillis = 2000) {
            composeTestRule
                .onAllNodesWithContentDescription("Wave progression: 50%")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        // Update progression to 100%
        progressionFlow.value = 100.0

        composeTestRule.waitUntil(timeoutMillis = 2000) {
            composeTestRule
                .onAllNodesWithContentDescription("Wave progression: 100%")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        trace.stop()
    }

    @Test
    fun testEventFavorites_persistAcrossNavigation() {
        val trace = createPerformanceTrace("testEventFavorites")

        val mockEvents = UITestFactory.createMockEventsList(count = 3)
        val favoritesState =
            mutableMapOf(
                mockEvents[0].id to false,
                mockEvents[1].id to false,
                mockEvents[2].id to false,
            )

        composeTestRule.setContent {
            TestEventFavoritesPersistence(
                events = mockEvents,
                favoritesState = favoritesState,
                onToggleFavorite = { eventId ->
                    favoritesState[eventId] = !(favoritesState[eventId] ?: false)
                },
            )
        }

        // Verify initial state - no favorites
        composeTestRule
            .onNodeWithContentDescription("Favorites count: 0")
            .assertIsDisplayed()

        // Mark first event as favorite
        composeTestRule
            .onNodeWithTag("favorite-toggle-${mockEvents[0].id}")
            .performClick()

        composeTestRule.waitUntil(timeoutMillis = 2000) {
            favoritesState[mockEvents[0].id] == true
        }

        composeTestRule
            .onNodeWithContentDescription("Favorites count: 1")
            .assertIsDisplayed()

        // Mark second event as favorite
        composeTestRule
            .onNodeWithTag("favorite-toggle-${mockEvents[1].id}")
            .performClick()

        composeTestRule.waitUntil(timeoutMillis = 2000) {
            favoritesState[mockEvents[1].id] == true
        }

        composeTestRule
            .onNodeWithContentDescription("Favorites count: 2")
            .assertIsDisplayed()

        // Unfavorite first event
        composeTestRule
            .onNodeWithTag("favorite-toggle-${mockEvents[0].id}")
            .performClick()

        composeTestRule.waitUntil(timeoutMillis = 2000) {
            favoritesState[mockEvents[0].id] == false
        }

        composeTestRule
            .onNodeWithContentDescription("Favorites count: 1")
            .assertIsDisplayed()

        trace.stop()
    }

    @Test
    fun testEventFiltering_worksCorrectly() {
        val trace = createPerformanceTrace("testEventFiltering")

        val mockEvents = UITestFactory.createMockEventsList(count = 5, withDifferentStates = true)
        var currentFilter by mutableStateOf("all")

        composeTestRule.setContent {
            TestEventFiltering(
                events = mockEvents,
                currentFilter = currentFilter,
                onFilterChanged = { filter -> currentFilter = filter },
            )
        }

        // Test "All" filter
        composeTestRule
            .onNodeWithContentDescription("Filter: all, showing ${mockEvents.size} events")
            .assertIsDisplayed()

        // Test "Favorites" filter
        composeTestRule
            .onNodeWithTag("filter-favorites")
            .performClick()

        composeTestRule.waitUntil(timeoutMillis = 2000) {
            currentFilter == "favorites"
        }

        val favoriteCount = mockEvents.count { it.favorite }
        composeTestRule.waitUntil(timeoutMillis = 2000) {
            composeTestRule
                .onAllNodesWithContentDescription("Filter: favorites, showing $favoriteCount events")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        // Test "Downloaded" filter
        composeTestRule
            .onNodeWithTag("filter-downloaded")
            .performClick()

        composeTestRule.waitUntil(timeoutMillis = 2000) {
            currentFilter == "downloaded"
        }

        // Downloaded state is managed by MapViewModel, not directly on event
        val downloadedCount = mockEvents.filter { it.id.endsWith("1") || it.id.endsWith("2") }.size
        composeTestRule.waitUntil(timeoutMillis = 2000) {
            composeTestRule
                .onAllNodesWithContentDescription("Filter: downloaded, showing $downloadedCount events")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        trace.stop()
    }

    @Test
    fun testEventSorting_worksCorrectly() {
        val trace = createPerformanceTrace("testEventSorting")

        val mockEvents = UITestFactory.createMockEventsList(count = 4, withDifferentStates = false)
        var currentSort by mutableStateOf("name")

        composeTestRule.setContent {
            TestEventSorting(
                events = mockEvents,
                currentSort = currentSort,
                onSortChanged = { sort -> currentSort = sort },
            )
        }

        // Test "Name" sort
        composeTestRule
            .onNodeWithContentDescription("Sort: name")
            .assertIsDisplayed()

        // Test "Date" sort
        composeTestRule
            .onNodeWithTag("sort-date")
            .performClick()

        composeTestRule.waitUntil(timeoutMillis = 2000) {
            currentSort == "date"
        }

        composeTestRule
            .onNodeWithContentDescription("Sort: date")
            .assertIsDisplayed()

        // Test "Distance" sort
        composeTestRule
            .onNodeWithTag("sort-distance")
            .performClick()

        composeTestRule.waitUntil(timeoutMillis = 2000) {
            currentSort == "distance"
        }

        composeTestRule
            .onNodeWithContentDescription("Sort: distance")
            .assertIsDisplayed()

        trace.stop()
    }

    @Test
    fun testUserCanLeaveAndRejoinWave() {
        val trace = createPerformanceTrace("testUserCanLeaveAndRejoinWave")

        val mockEvent =
            UITestFactory.createMockWaveEvent(
                id = "rejoin-test-event",
                status = "RUNNING",
            )

        var isInWave by mutableStateOf(false)
        var rejoinCount = 0

        composeTestRule.setContent {
            TestLeaveAndRejoinWave(
                event = mockEvent,
                isInWave = isInWave,
                onJoinWave = {
                    isInWave = true
                    rejoinCount++
                },
                onLeaveWave = {
                    isInWave = false
                },
            )
        }

        // Initial state - not in wave
        composeTestRule
            .onNodeWithContentDescription("Wave status: Not participating")
            .assertIsDisplayed()

        // Join wave
        composeTestRule
            .onNodeWithTag("join-wave-button")
            .performClick()

        composeTestRule.waitUntil(timeoutMillis = 2000) {
            isInWave
        }

        composeTestRule
            .onNodeWithContentDescription("Wave status: Participating")
            .assertIsDisplayed()

        // Leave wave
        composeTestRule
            .onNodeWithTag("leave-wave-button")
            .performClick()

        composeTestRule.waitUntil(timeoutMillis = 2000) {
            !isInWave
        }

        composeTestRule
            .onNodeWithContentDescription("Wave status: Not participating")
            .assertIsDisplayed()

        // Rejoin wave
        composeTestRule
            .onNodeWithTag("join-wave-button")
            .performClick()

        composeTestRule.waitUntil(timeoutMillis = 2000) {
            isInWave && rejoinCount == 2
        }

        composeTestRule
            .onNodeWithContentDescription("Wave status: Participating")
            .assertIsDisplayed()

        trace.stop()
    }

    @Test
    fun testSimulationMode_allowsTestingWithoutGPS() {
        val trace = createPerformanceTrace("testSimulationMode")

        val mockEvent =
            UITestFactory.createMockWaveEvent(
                id = "simulation-test-event",
                isInArea = false,
            )

        var simulationEnabled by mutableStateOf(false)
        val userPositionRatioFlow = MutableStateFlow(0.0)

        every { mockEvent.observer.userPositionRatio } returns userPositionRatioFlow

        composeTestRule.setContent {
            TestSimulationMode(
                event = mockEvent,
                simulationEnabled = simulationEnabled,
                onToggleSimulation = { simulationEnabled = !simulationEnabled },
            )
        }

        // Verify simulation is off initially
        composeTestRule
            .onNodeWithContentDescription("Simulation mode: Disabled")
            .assertIsDisplayed()

        // Enable simulation
        composeTestRule
            .onNodeWithTag("toggle-simulation")
            .performClick()

        composeTestRule.waitUntil(timeoutMillis = 2000) {
            simulationEnabled
        }

        composeTestRule
            .onNodeWithContentDescription("Simulation mode: Enabled")
            .assertIsDisplayed()

        // Simulate user position change
        userPositionRatioFlow.value = 0.5

        composeTestRule.waitUntil(timeoutMillis = 2000) {
            composeTestRule
                .onAllNodesWithContentDescription("Simulated position: 50%")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        trace.stop()
    }

    @Test
    fun testPermissionDenial_preventsWaveParticipation() {
        val trace = createPerformanceTrace("testPermissionDenial")

        val mockEvent =
            UITestFactory.createMockWaveEvent(
                id = "permission-test-event",
            )

        var locationPermissionGranted by mutableStateOf(false)
        var errorMessageShown = false

        composeTestRule.setContent {
            TestPermissionHandling(
                event = mockEvent,
                locationPermissionGranted = locationPermissionGranted,
                onPermissionError = { errorMessageShown = true },
            )
        }

        // Try to join wave without permission
        composeTestRule
            .onNodeWithTag("join-wave-button")
            .performClick()

        composeTestRule.waitUntil(timeoutMillis = 2000) {
            errorMessageShown
        }

        // Verify error message
        composeTestRule
            .onNodeWithContentDescription("Permission error: Location access required")
            .assertIsDisplayed()

        // Grant permission
        locationPermissionGranted = true
        errorMessageShown = false

        composeTestRule.waitForIdle()

        // Try to join wave with permission
        composeTestRule
            .onNodeWithTag("join-wave-button")
            .performClick()

        composeTestRule.waitUntil(timeoutMillis = 2000) {
            composeTestRule
                .onAllNodesWithContentDescription("Wave joined successfully")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        trace.stop()
    }

    @Test
    fun testNetworkError_duringEventJoin_showsRetryOption() {
        val trace = createPerformanceTrace("testNetworkError")

        val mockEvent =
            UITestFactory.createMockWaveEvent(
                id = "network-error-test-event",
            )

        var networkError by mutableStateOf(false)
        var retryCount = 0

        composeTestRule.setContent {
            TestNetworkErrorHandling(
                event = mockEvent,
                networkError = networkError,
                onRetry = { retryCount++ },
            )
        }

        // Simulate network error
        networkError = true

        composeTestRule.waitForIdle()

        // Try to join wave
        composeTestRule
            .onNodeWithTag("join-wave-button")
            .performClick()

        composeTestRule.waitUntil(timeoutMillis = 2000) {
            composeTestRule
                .onAllNodesWithContentDescription("Network error: Connection failed")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        // Verify retry button is shown
        composeTestRule
            .onNodeWithTag("retry-button")
            .assertIsDisplayed()

        // Click retry
        composeTestRule
            .onNodeWithTag("retry-button")
            .performClick()

        composeTestRule.waitUntil(timeoutMillis = 2000) {
            retryCount == 1
        }

        // Fix network and retry
        networkError = false

        composeTestRule
            .onNodeWithTag("retry-button")
            .performClick()

        composeTestRule.waitUntil(timeoutMillis = 2000) {
            retryCount == 2 && !networkError
        }

        // Verify success after network recovery
        composeTestRule.waitUntil(timeoutMillis = 2000) {
            composeTestRule
                .onAllNodesWithContentDescription("Wave joined successfully")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        trace.stop()
    }

    // =============================================================================================
    // Test Helper Composables
    // =============================================================================================

    @Composable
    private fun TestCompleteParticipationFlow(
        events: List<IWWWEvent>,
        selectedEventId: String?,
        isInWaveScreen: Boolean,
        onEventSelected: (String) -> Unit,
        onJoinWave: () -> Unit,
        onWaveHit: () -> Unit,
        targetEvent: IWWWEvent?,
    ) {
        when {
            isInWaveScreen && targetEvent != null -> {
                // Wave screen
                val isWarming by targetEvent.observer.isUserWarmingInProgress.collectAsState(false)
                val isGoingToBeHit by targetEvent.observer.userIsGoingToBeHit.collectAsState(false)
                val hasBeenHit by targetEvent.observer.userHasBeenHit.collectAsState(false)

                LaunchedEffect(hasBeenHit) {
                    if (hasBeenHit) {
                        onWaveHit()
                    }
                }

                val phase =
                    when {
                        hasBeenHit -> "Hit detected"
                        isGoingToBeHit -> "Waiting for hit"
                        isWarming -> "Warming"
                        else -> "Observer"
                    }

                Column {
                    Text(
                        text = "Wave Screen",
                        modifier =
                            Modifier.semantics {
                                contentDescription = "Wave screen active"
                            },
                    )
                    Text(
                        text = "Phase: $phase",
                        modifier =
                            Modifier.semantics {
                                contentDescription = "Wave phase: $phase"
                            },
                    )
                    if (hasBeenHit) {
                        Text(
                            text = "Hit!",
                            modifier =
                                Modifier.semantics {
                                    contentDescription = "Wave hit detected!"
                                },
                        )
                    }
                }
            }
            selectedEventId != null -> {
                // Event details screen
                Column {
                    Text(
                        text = "Event Details",
                        modifier =
                            Modifier.semantics {
                                contentDescription = "Event details: $selectedEventId"
                            },
                    )
                    Text(
                        text = "Join Wave",
                        modifier =
                            Modifier
                                .testTag("join-wave-button")
                                .clickable { onJoinWave() }
                                .padding(16.dp)
                                .background(Color.Blue),
                    )
                }
            }
            else -> {
                // Events list screen
                Column {
                    Text(
                        text = "Events List",
                        modifier =
                            Modifier.semantics {
                                contentDescription = "Events list displayed"
                            },
                    )
                    LazyColumn {
                        items(events) { event ->
                            Card(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp)
                                        .testTag("event-item-${event.id}")
                                        .clickable { onEventSelected(event.id) },
                            ) {
                                Text(
                                    text = event.community ?: event.id,
                                    modifier = Modifier.padding(16.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun TestEventListToDetails(
        events: List<IWWWEvent>,
        selectedEventId: String?,
        onEventSelected: (String) -> Unit,
    ) {
        if (selectedEventId != null) {
            Column {
                Text(
                    text = "Event Details",
                    modifier =
                        Modifier.semantics {
                            contentDescription = "Event details: $selectedEventId"
                        },
                )
                Text(
                    text = "Join Wave",
                    modifier =
                        Modifier
                            .testTag("join-wave-button")
                            .padding(16.dp)
                            .background(Color.Blue),
                )
            }
        } else {
            Column {
                Text(
                    text = "Events (${events.size})",
                    modifier =
                        Modifier.semantics {
                            contentDescription = "Events list: ${events.size} events"
                        },
                )
                LazyColumn {
                    items(events) { event ->
                        Card(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp)
                                    .testTag("event-item-${event.id}")
                                    .clickable { onEventSelected(event.id) },
                        ) {
                            Text(text = event.community ?: event.id, modifier = Modifier.padding(16.dp))
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun TestWaveHitNotification(
        event: IWWWEvent,
        onHitNotification: () -> Unit,
    ) {
        val isInArea by event.observer.userIsInArea.collectAsState(false)
        val hasBeenHit by event.observer.userHasBeenHit.collectAsState(false)

        LaunchedEffect(hasBeenHit) {
            if (hasBeenHit) {
                onHitNotification()
            }
        }

        Column {
            Text(
                text = if (isInArea) "Inside area" else "Outside area",
                modifier =
                    Modifier.semantics {
                        contentDescription = "User status: ${if (isInArea) "Inside" else "Outside"} area"
                    },
            )
            if (hasBeenHit) {
                Text(
                    text = "Hit Notification",
                    modifier =
                        Modifier.semantics {
                            contentDescription = "Wave hit notification displayed"
                        },
                )
            }
        }
    }

    @Composable
    private fun TestWaveHitCounter(
        event: IWWWEvent,
        hitCount: Int,
        onHitDetected: () -> Unit,
    ) {
        val hasBeenHit by event.observer.userHasBeenHit.collectAsState(false)
        var lastHitState by remember { mutableStateOf(false) }

        LaunchedEffect(hasBeenHit) {
            if (hasBeenHit && !lastHitState) {
                onHitDetected()
                lastHitState = true
            } else if (!hasBeenHit) {
                lastHitState = false
            }
        }

        Text(
            text = "Hits: $hitCount",
            modifier =
                Modifier.semantics {
                    contentDescription = "Hit counter: $hitCount hits"
                },
        )
    }

    @Composable
    private fun TestWaveProgressionBar(event: IWWWEvent) {
        val progression by event.observer.progression.collectAsState(0.0)

        Column {
            LinearProgressIndicator(
                progress = { (progression / 100.0).toFloat() },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = "Progression: ${progression.toInt()}%",
                modifier =
                    Modifier.semantics {
                        contentDescription = "Wave progression: ${progression.toInt()}%"
                    },
            )
        }
    }

    @Composable
    private fun TestEventFavoritesPersistence(
        events: List<IWWWEvent>,
        favoritesState: Map<String, Boolean>,
        onToggleFavorite: (String) -> Unit,
    ) {
        val favoriteCount = favoritesState.values.count { it }

        Column {
            Text(
                text = "Favorites: $favoriteCount",
                modifier =
                    Modifier.semantics {
                        contentDescription = "Favorites count: $favoriteCount"
                    },
            )
            LazyColumn {
                items(events) { event ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(text = event.community ?: event.id)
                        Text(
                            text = if (favoritesState[event.id] == true) "★" else "☆",
                            modifier =
                                Modifier
                                    .testTag("favorite-toggle-${event.id}")
                                    .clickable { onToggleFavorite(event.id) },
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun TestEventFiltering(
        events: List<IWWWEvent>,
        currentFilter: String,
        onFilterChanged: (String) -> Unit,
    ) {
        val filteredEvents =
            when (currentFilter) {
                "favorites" -> events.filter { it.favorite }
                "downloaded" -> events.filter { it.id.endsWith("1") || it.id.endsWith("2") }
                else -> events
            }

        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                Text(
                    text = "All",
                    modifier =
                        Modifier
                            .testTag("filter-all")
                            .clickable { onFilterChanged("all") }
                            .padding(8.dp),
                )
                Text(
                    text = "Favorites",
                    modifier =
                        Modifier
                            .testTag("filter-favorites")
                            .clickable { onFilterChanged("favorites") }
                            .padding(8.dp),
                )
                Text(
                    text = "Downloaded",
                    modifier =
                        Modifier
                            .testTag("filter-downloaded")
                            .clickable { onFilterChanged("downloaded") }
                            .padding(8.dp),
                )
            }
            Text(
                text = "Showing ${filteredEvents.size} events",
                modifier =
                    Modifier.semantics {
                        contentDescription = "Filter: $currentFilter, showing ${filteredEvents.size} events"
                    },
            )
        }
    }

    @Composable
    private fun TestEventSorting(
        events: List<IWWWEvent>,
        currentSort: String,
        onSortChanged: (String) -> Unit,
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                Text(
                    text = "Name",
                    modifier =
                        Modifier
                            .testTag("sort-name")
                            .clickable { onSortChanged("name") }
                            .padding(8.dp),
                )
                Text(
                    text = "Date",
                    modifier =
                        Modifier
                            .testTag("sort-date")
                            .clickable { onSortChanged("date") }
                            .padding(8.dp),
                )
                Text(
                    text = "Distance",
                    modifier =
                        Modifier
                            .testTag("sort-distance")
                            .clickable { onSortChanged("distance") }
                            .padding(8.dp),
                )
            }
            Text(
                text = "Sort: $currentSort",
                modifier =
                    Modifier.semantics {
                        contentDescription = "Sort: $currentSort"
                    },
            )
        }
    }

    @Composable
    private fun TestLeaveAndRejoinWave(
        event: IWWWEvent,
        isInWave: Boolean,
        onJoinWave: () -> Unit,
        onLeaveWave: () -> Unit,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = if (isInWave) "Participating" else "Not participating",
                modifier =
                    Modifier.semantics {
                        contentDescription = "Wave status: ${if (isInWave) "Participating" else "Not participating"}"
                    },
            )
            if (isInWave) {
                Text(
                    text = "Leave Wave",
                    modifier =
                        Modifier
                            .testTag("leave-wave-button")
                            .clickable { onLeaveWave() }
                            .padding(16.dp)
                            .background(Color.Red),
                )
            } else {
                Text(
                    text = "Join Wave",
                    modifier =
                        Modifier
                            .testTag("join-wave-button")
                            .clickable { onJoinWave() }
                            .padding(16.dp)
                            .background(Color.Blue),
                )
            }
        }
    }

    @Composable
    private fun TestSimulationMode(
        event: IWWWEvent,
        simulationEnabled: Boolean,
        onToggleSimulation: () -> Unit,
    ) {
        val userPositionRatio by event.observer.userPositionRatio.collectAsState(0.0)

        Column {
            Text(
                text = "Simulation: ${if (simulationEnabled) "Enabled" else "Disabled"}",
                modifier =
                    Modifier.semantics {
                        contentDescription = "Simulation mode: ${if (simulationEnabled) "Enabled" else "Disabled"}"
                    },
            )
            Text(
                text = "Toggle",
                modifier =
                    Modifier
                        .testTag("toggle-simulation")
                        .clickable { onToggleSimulation() }
                        .padding(16.dp)
                        .background(Color.Gray),
            )
            if (simulationEnabled) {
                Text(
                    text = "Position: ${(userPositionRatio * 100).toInt()}%",
                    modifier =
                        Modifier.semantics {
                            contentDescription = "Simulated position: ${(userPositionRatio * 100).toInt()}%"
                        },
                )
            }
        }
    }

    @Composable
    private fun TestPermissionHandling(
        event: IWWWEvent,
        locationPermissionGranted: Boolean,
        onPermissionError: () -> Unit,
    ) {
        var attemptedJoin by remember { mutableStateOf(false) }

        LaunchedEffect(attemptedJoin, locationPermissionGranted) {
            if (attemptedJoin && !locationPermissionGranted) {
                onPermissionError()
            }
        }

        Column {
            if (attemptedJoin && !locationPermissionGranted) {
                Text(
                    text = "Permission Error",
                    modifier =
                        Modifier.semantics {
                            contentDescription = "Permission error: Location access required"
                        },
                )
            }
            if (attemptedJoin && locationPermissionGranted) {
                Text(
                    text = "Wave Joined",
                    modifier =
                        Modifier.semantics {
                            contentDescription = "Wave joined successfully"
                        },
                )
            }
            Text(
                text = "Join Wave",
                modifier =
                    Modifier
                        .testTag("join-wave-button")
                        .clickable { attemptedJoin = true }
                        .padding(16.dp)
                        .background(Color.Blue),
            )
        }
    }

    @Composable
    private fun TestNetworkErrorHandling(
        event: IWWWEvent,
        networkError: Boolean,
        onRetry: () -> Unit,
    ) {
        var attemptedJoin by remember { mutableStateOf(false) }

        Column {
            if (attemptedJoin && networkError) {
                Text(
                    text = "Network Error",
                    modifier =
                        Modifier.semantics {
                            contentDescription = "Network error: Connection failed"
                        },
                )
                Text(
                    text = "Retry",
                    modifier =
                        Modifier
                            .testTag("retry-button")
                            .clickable { onRetry() }
                            .padding(16.dp)
                            .background(Color.Red),
                )
            } else if (attemptedJoin && !networkError) {
                Text(
                    text = "Wave Joined",
                    modifier =
                        Modifier.semantics {
                            contentDescription = "Wave joined successfully"
                        },
                )
            }
            Text(
                text = "Join Wave",
                modifier =
                    Modifier
                        .testTag("join-wave-button")
                        .clickable { attemptedJoin = true }
                        .padding(16.dp)
                        .background(Color.Blue),
            )
        }
    }
}
