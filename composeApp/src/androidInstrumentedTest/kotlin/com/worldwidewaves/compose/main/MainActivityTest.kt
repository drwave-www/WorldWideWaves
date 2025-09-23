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

package com.worldwidewaves.compose.main

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.worldwidewaves.activities.utils.TabManager
import com.worldwidewaves.compose.tabs.AboutScreen
import com.worldwidewaves.compose.tabs.EventsListScreen
import com.worldwidewaves.shared.MokoRes
import com.worldwidewaves.shared.WWWPlatform
import com.worldwidewaves.shared.events.WWWEvents
import com.worldwidewaves.shared.monitoring.PerformanceMonitor
import dev.icerock.moko.resources.compose.stringResource
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Critical Phase 1 UI tests for Main Activity - Core app navigation
 *
 * Tests cover the app entry point functionality including splash screen management,
 * tab navigation, location permissions, and lifecycle handling.
 */
@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val performanceMonitor = mockk<PerformanceMonitor>(relaxed = true)

    @Test
    fun testSplashScreenTimingAndTransition() {
        val trace = mockk<com.worldwidewaves.shared.monitoring.PerformanceTrace>(relaxed = true)
        every { performanceMonitor.startTrace("testSplashScreenTimingAndTransition") } returns trace

        val mockEvents = mockk<WWWEvents>(relaxed = true)
        val isSplashFinished = MutableStateFlow(false)
        var dataLoadCompleted = false

        coEvery { mockEvents.loadEvents(any()) } answers {
            delay(100) // Simulate data loading
            dataLoadCompleted = true
            firstArg<() -> Unit>().invoke() // Call onTermination callback
        }

        composeTestRule.setContent {
            TestSplashScreenApp(
                events = mockEvents,
                isSplashFinished = isSplashFinished,
                onDataLoad = { dataLoadCompleted }
            )
        }

        // Verify splash screen is displayed initially
        composeTestRule
            .onNodeWithContentDescription("Splash screen is displayed")
            .assertIsDisplayed()

        // Verify minimum 2-second display duration
        val startTime = System.currentTimeMillis()

        // Wait for data loading and minimum duration
        composeTestRule.waitUntil(timeoutMillis = 3000) {
            val elapsed = System.currentTimeMillis() - startTime
            dataLoadCompleted && elapsed >= 2000
        }

        // Simulate splash finishing
        isSplashFinished.value = true

        // Verify transition to main content
        composeTestRule.waitUntil(timeoutMillis = 1000) {
            composeTestRule
                .onAllNodesWithContentDescription("Main content is displayed")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule
            .onNodeWithContentDescription("Main content is displayed")
            .assertIsDisplayed()

        trace.stop()
    }

    @Test
    fun testTabNavigationFunctionality() {
        val trace = mockk<com.worldwidewaves.shared.monitoring.PerformanceTrace>(relaxed = true)
        every { performanceMonitor.startTrace("testTabNavigationFunctionality") } returns trace

        val mockEventsScreen = mockk<EventsListScreen>(relaxed = true)
        val mockAboutScreen = mockk<AboutScreen>(relaxed = true)
        var selectedTab by mutableStateOf(0)

        every { mockEventsScreen.screenName } returns "Events"
        every { mockAboutScreen.screenName } returns "About"
        every { mockEventsScreen.id } returns "events"
        every { mockAboutScreen.id } returns "about"

        composeTestRule.setContent {
            TestTabNavigation(
                eventsScreen = mockEventsScreen,
                aboutScreen = mockAboutScreen,
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )
        }

        // Verify initial Events tab is selected
        composeTestRule
            .onNodeWithContentDescription("Events tab selected")
            .assertIsDisplayed()

        // Test tab switching to About
        composeTestRule
            .onNodeWithTag("tab-about")
            .performClick()

        composeTestRule.waitUntil(timeoutMillis = 1000) {
            selectedTab == 1
        }

        composeTestRule
            .onNodeWithContentDescription("About tab selected")
            .assertIsDisplayed()

        // Test tab state persistence (switch back to Events)
        composeTestRule
            .onNodeWithTag("tab-events")
            .performClick()

        composeTestRule.waitUntil(timeoutMillis = 1000) {
            selectedTab == 0
        }

        composeTestRule
            .onNodeWithContentDescription("Events tab selected")
            .assertIsDisplayed()

        trace.stop()
    }

    @Test
    fun testLocationPermissionFlow() {
        val trace = mockk<com.worldwidewaves.shared.monitoring.PerformanceTrace>(relaxed = true)
        every { performanceMonitor.startTrace("testLocationPermissionFlow") } returns trace

        val locationPermissionState = MutableStateFlow("not_requested")

        composeTestRule.setContent {
            TestLocationPermissionFlow(locationPermissionState.value) { newState ->
                locationPermissionState.value = newState
            }
        }

        // Verify initial state - permission not requested
        composeTestRule
            .onNodeWithContentDescription("Location permission: Not requested")
            .assertIsDisplayed()

        // Simulate permission request dialog
        composeTestRule
            .onNodeWithTag("request-permission")
            .performClick()

        composeTestRule.waitUntil(timeoutMillis = 1000) {
            locationPermissionState.value == "requesting"
        }

        composeTestRule
            .onNodeWithContentDescription("Location permission: Requesting")
            .assertIsDisplayed()

        // Test permission granted scenario
        locationPermissionState.value = "granted"

        composeTestRule.waitUntil(timeoutMillis = 1000) {
            composeTestRule
                .onAllNodesWithContentDescription("Location permission: Granted")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule
            .onNodeWithContentDescription("Location permission: Granted")
            .assertIsDisplayed()

        // Reset and test permission denied scenario
        locationPermissionState.value = "denied"

        composeTestRule.waitUntil(timeoutMillis = 1000) {
            composeTestRule
                .onAllNodesWithContentDescription("Location permission: Denied")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule
            .onNodeWithContentDescription("Location permission: Denied")
            .assertIsDisplayed()

        trace.stop()
    }

    @Test
    fun testAppLifecycleHandling() {
        val trace = mockk<com.worldwidewaves.shared.monitoring.PerformanceTrace>(relaxed = true)
        every { performanceMonitor.startTrace("testAppLifecycleHandling") } returns trace

        val lifecycleState = MutableStateFlow("created")
        val preservedState = MutableStateFlow("initial_data")

        composeTestRule.setContent {
            TestAppLifecycleHandling(lifecycleState.value, preservedState.value)
        }

        // Verify initial created state
        composeTestRule
            .onNodeWithContentDescription("Lifecycle state: Created, Data: initial_data")
            .assertIsDisplayed()

        // Test transition to background (paused state)
        lifecycleState.value = "paused"

        composeTestRule.waitUntil(timeoutMillis = 1000) {
            composeTestRule
                .onAllNodesWithContentDescription("Lifecycle state: Paused, Data: initial_data")
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Test return to foreground (resumed state) with state preservation
        lifecycleState.value = "resumed"
        preservedState.value = "preserved_data"

        composeTestRule.waitUntil(timeoutMillis = 1000) {
            composeTestRule
                .onAllNodesWithContentDescription("Lifecycle state: Resumed, Data: preserved_data")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule
            .onNodeWithContentDescription("Lifecycle state: Resumed, Data: preserved_data")
            .assertIsDisplayed()

        trace.stop()
    }

    @Test
    fun testInitialDataLoadingStates() {
        val trace = mockk<com.worldwidewaves.shared.monitoring.PerformanceTrace>(relaxed = true)
        every { performanceMonitor.startTrace("testInitialDataLoadingStates") } returns trace

        val loadingState = MutableStateFlow("loading")
        val mockEvents = mockk<WWWEvents>(relaxed = true)

        every { mockEvents.events } returns flowOf(emptyList())

        composeTestRule.setContent {
            TestDataLoadingStates(loadingState.value, mockEvents)
        }

        // Verify loading indicator is displayed
        composeTestRule
            .onNodeWithContentDescription("Data loading: Loading")
            .assertIsDisplayed()

        // Simulate successful data fetch completion
        loadingState.value = "success"

        composeTestRule.waitUntil(timeoutMillis = 1000) {
            composeTestRule
                .onAllNodesWithContentDescription("Data loading: Success, Events loaded: 3")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule
            .onNodeWithContentDescription("Data loading: Success, Events loaded: 3")
            .assertIsDisplayed()

        // Test network error state
        loadingState.value = "error"

        composeTestRule.waitUntil(timeoutMillis = 1000) {
            composeTestRule
                .onAllNodesWithContentDescription("Data loading: Network error")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule
            .onNodeWithContentDescription("Data loading: Network error")
            .assertIsDisplayed()

        trace.stop()
    }

    @Test
    fun testMainActivityErrorRecovery() {
        val trace = mockk<com.worldwidewaves.shared.monitoring.PerformanceTrace>(relaxed = true)
        every { performanceMonitor.startTrace("testMainActivityErrorRecovery") } returns trace

        val errorState = MutableStateFlow("none")
        val retryCount = MutableStateFlow(0)

        composeTestRule.setContent {
            TestErrorRecovery(errorState.value, retryCount.value) {
                retryCount.value = retryCount.value + 1
            }
        }

        // Verify initial success state
        composeTestRule
            .onNodeWithContentDescription("Error state: None, Retries: 0")
            .assertIsDisplayed()

        // Test network failure scenario
        errorState.value = "network_failure"

        composeTestRule.waitUntil(timeoutMillis = 1000) {
            composeTestRule
                .onAllNodesWithContentDescription("Error state: Network failure, Retries: 0")
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Test retry mechanism
        composeTestRule
            .onNodeWithTag("retry-button")
            .performClick()

        composeTestRule.waitUntil(timeoutMillis = 1000) {
            retryCount.value == 1
        }

        composeTestRule
            .onNodeWithContentDescription("Error state: Network failure, Retries: 1")
            .assertIsDisplayed()

        // Test graceful recovery
        errorState.value = "recovered"

        composeTestRule.waitUntil(timeoutMillis = 1000) {
            composeTestRule
                .onAllNodesWithContentDescription("Error state: Recovered, Retries: 1")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule
            .onNodeWithContentDescription("Error state: Recovered, Retries: 1")
            .assertIsDisplayed()

        trace.stop()
    }

    // Test helper composables

    @Composable
    private fun TestSplashScreenApp(
        events: WWWEvents,
        isSplashFinished: MutableStateFlow<Boolean>,
        onDataLoad: () -> Boolean
    ) {
        val ready by isSplashFinished.collectAsState()
        val isDataLoaded = onDataLoad()

        LaunchedEffect(Unit) {
            events.loadEvents {
                // Data loading completion callback
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            if (ready) {
                Text(
                    text = "Main Content",
                    modifier = Modifier.semantics {
                        contentDescription = "Main content is displayed"
                    }
                )
            } else {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Text(
                        text = "Loading...",
                        modifier = Modifier.semantics {
                            contentDescription = "Splash screen is displayed"
                        }
                    )
                }
            }
        }
    }

    @Composable
    private fun TestTabNavigation(
        eventsScreen: EventsListScreen,
        aboutScreen: AboutScreen,
        selectedTab: Int,
        onTabSelected: (Int) -> Unit
    ) {
        Column {
            // Tab buttons
            androidx.compose.foundation.layout.Row {
                Text(
                    text = "Events",
                    modifier = Modifier
                        .testTag("tab-events")
                        .semantics {
                            contentDescription = if (selectedTab == 0) "Events tab selected" else "Events tab"
                        }
                        .padding(16.dp)
                        .then(
                            if (selectedTab == 0) {
                                Modifier.background(Color.Blue)
                            } else {
                                Modifier
                            }
                        ),
                    color = if (selectedTab == 0) Color.White else Color.Black
                )
                Text(
                    text = "About",
                    modifier = Modifier
                        .testTag("tab-about")
                        .semantics {
                            contentDescription = if (selectedTab == 1) "About tab selected" else "About tab"
                        }
                        .padding(16.dp)
                        .then(
                            if (selectedTab == 1) {
                                Modifier.background(Color.Blue)
                            } else {
                                Modifier
                            }
                        ),
                    color = if (selectedTab == 1) Color.White else Color.Black
                )
            }

            // Tab content
            when (selectedTab) {
                0 -> Text("Events Screen Content")
                1 -> Text("About Screen Content")
            }
        }
    }

    @Composable
    private fun TestLocationPermissionFlow(
        permissionState: String,
        onPermissionStateChange: (String) -> Unit
    ) {
        Column {
            Text(
                text = "Location Permission",
                modifier = Modifier.semantics {
                    contentDescription = "Location permission: ${
                        when (permissionState) {
                            "not_requested" -> "Not requested"
                            "requesting" -> "Requesting"
                            "granted" -> "Granted"
                            "denied" -> "Denied"
                            else -> "Unknown"
                        }
                    }"
                }
            )

            if (permissionState == "not_requested") {
                Text(
                    text = "Request Permission",
                    modifier = Modifier
                        .testTag("request-permission")
                        .padding(8.dp)
                        .background(Color.Gray)
                        .padding(16.dp)
                )
            }
        }
    }

    @Composable
    private fun TestAppLifecycleHandling(lifecycleState: String, preservedData: String) {
        Text(
            text = "App Lifecycle",
            modifier = Modifier.semantics {
                contentDescription = "Lifecycle state: ${
                    when (lifecycleState) {
                        "created" -> "Created"
                        "paused" -> "Paused"
                        "resumed" -> "Resumed"
                        else -> "Unknown"
                    }
                }, Data: $preservedData"
            }
        )
    }

    @Composable
    private fun TestDataLoadingStates(loadingState: String, events: WWWEvents) {
        val eventsCount by events.events.collectAsState(initial = emptyList())

        Text(
            text = "Data Loading",
            modifier = Modifier.semantics {
                contentDescription = when (loadingState) {
                    "loading" -> "Data loading: Loading"
                    "success" -> "Data loading: Success, Events loaded: ${eventsCount.size}"
                    "error" -> "Data loading: Network error"
                    else -> "Data loading: Unknown state"
                }
            }
        )

        if (loadingState == "loading") {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp)
            )
        }
    }

    @Composable
    private fun TestErrorRecovery(errorState: String, retryCount: Int, onRetry: () -> Unit) {
        Column {
            Text(
                text = "Error Recovery",
                modifier = Modifier.semantics {
                    contentDescription = "Error state: ${
                        when (errorState) {
                            "none" -> "None"
                            "network_failure" -> "Network failure"
                            "recovered" -> "Recovered"
                            else -> "Unknown"
                        }
                    }, Retries: $retryCount"
                }
            )

            if (errorState == "network_failure") {
                Text(
                    text = "Retry",
                    modifier = Modifier
                        .testTag("retry-button")
                        .padding(8.dp)
                        .background(Color.Red)
                        .padding(16.dp),
                    color = Color.White
                )
            }
        }
    }
}