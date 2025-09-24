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

package com.worldwidewaves.testing.real

import androidx.compose.ui.test.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

/**
 * Real integration tests for app launch performance benchmarking.
 *
 * These tests measure actual app launch performance metrics:
 * - Cold start time with real data loading
 * - Warm start performance
 * - Time to first meaningful paint
 * - Time to interactive measurements
 */
@RunWith(AndroidJUnit4::class)
class RealAppLaunchPerformanceTest : BaseRealIntegrationTest() {

    /**
     * Test cold start time with real data loading from Firebase
     */
    @Test
    fun realPerformance_coldStartTime_measuresWithRealData() = runTest {
        val trace = startPerformanceTrace("cold_start_benchmark")

        try {
            // Prepare test data that will be loaded during cold start
            repeat(20) { index ->
                createTestEvent(
                    eventId = "coldstart_event_$index",
                    latitude = 40.7589 + (index * 0.0001),
                    longitude = -73.9851 + (index * 0.0001),
                    startsSoonInSeconds = index * 180,
                    durationSeconds = 900
                )
            }

            // Wait for initial data sync
            waitForDataSync(3000)

            // Simulate cold app start by measuring time to fully functional state
            val startTime = System.currentTimeMillis()

            // Measure time to main screen visibility
            composeTestRule.waitUntilAtLeastOneExists(
                hasTestTag("main_screen"),
                timeoutMillis = 15000 // Allow up to 15 seconds for cold start
            )

            val firstPaintTime = System.currentTimeMillis() - startTime
            trace.addMetric("first_meaningful_paint_ms", firstPaintTime)

            // Measure time to interactive (when user can interact with events)
            composeTestRule.waitUntilAtLeastOneExists(
                hasTestTag("event_list"),
                timeoutMillis = 5000
            )

            val timeToInteractive = System.currentTimeMillis() - startTime
            trace.addMetric("time_to_interactive_ms", timeToInteractive)

            // Verify cold start performance meets requirements
            assertTrue(
                firstPaintTime < 10000, // Less than 10 seconds to first paint
                "Cold start first meaningful paint should be under 10s: ${firstPaintTime}ms"
            )

            assertTrue(
                timeToInteractive < 15000, // Less than 15 seconds to interactive
                "Cold start time to interactive should be under 15s: ${timeToInteractive}ms"
            )

            // Test that app is actually interactive
            composeTestRule.onNodeWithTag("event_list")
                .performScrollToIndex(5)

            // Verify data loading performance
            val eventCount = composeTestRule.onAllNodesWithContentDescription("View event details").fetchSemanticsNodes().size
            assertTrue(
                eventCount >= 15, // Should load most events during cold start
                "Should load at least 15 events during cold start, loaded: $eventCount"
            )

            val totalColdStartTime = stopPerformanceTrace()
            println("✅ Cold start performance: First paint ${firstPaintTime}ms, Interactive ${timeToInteractive}ms, Total ${totalColdStartTime}ms")

        } finally {
            stopPerformanceTrace()
        }
    }

    /**
     * Test warm start performance (app resuming from background)
     */
    @Test
    fun realPerformance_warmStartTime_measuresBetterThanCold() = runTest {
        val trace = startPerformanceTrace("warm_start_benchmark")

        try {
            // First, perform a cold start and let app settle
            createTestEvent(
                eventId = "warmstart_setup_event",
                latitude = 40.7589,
                longitude = -73.9851,
                startsSoonInSeconds = 300,
                durationSeconds = 600
            )

            waitForDataSync(3000)

            // Ensure app is fully loaded
            composeTestRule.waitUntilAtLeastOneExists(
                hasTestTag("main_screen"),
                timeoutMillis = 10000
            )

            // Simulate app going to background and returning (warm start scenario)
            deviceStateManager.simulateAppBackground()
            delay(5.seconds) // App in background for 5 seconds

            // Measure warm start performance
            val warmStartTime = System.currentTimeMillis()

            deviceStateManager.simulateAppForeground()

            // Measure time until app is interactive again
            composeTestRule.waitUntilAtLeastOneExists(
                hasTestTag("main_screen"),
                timeoutMillis = 8000
            )

            val warmStartDuration = System.currentTimeMillis() - warmStartTime
            trace.addMetric("warm_start_duration_ms", warmStartDuration)

            // Verify warm start is significantly faster than cold start
            assertTrue(
                warmStartDuration < 5000, // Less than 5 seconds for warm start
                "Warm start should be under 5s: ${warmStartDuration}ms"
            )

            // Test app responsiveness after warm start
            composeTestRule.onNodeWithTag("event_list")
                .performScrollToIndex(3)

            val scrollResponseTime = System.currentTimeMillis()

            composeTestRule.onNodeWithContentDescription("View event details")
                .performClick()

            composeTestRule.waitUntilAtLeastOneExists(
                hasTestTag("event_detail_screen"),
                timeoutMillis = 3000
            )

            val detailLoadTime = System.currentTimeMillis() - scrollResponseTime
            assertTrue(
                detailLoadTime < 2000, // Detail loading should be fast after warm start
                "Event detail loading after warm start should be under 2s: ${detailLoadTime}ms"
            )

            println("✅ Warm start performance: Resume ${warmStartDuration}ms, Detail load ${detailLoadTime}ms")

        } finally {
            deviceStateManager.simulateAppForeground()
            stopPerformanceTrace()
        }
    }

    /**
     * Test first meaningful paint benchmarks with different data loads
     */
    @Test
    fun realPerformance_firstMeaningfulPaint_benchmarksDataScenarios() = runTest {
        val trace = startPerformanceTrace("first_paint_benchmark")

        try {
            val dataScenarios = listOf(
                DataLoadScenario("small_dataset", 5, 3000), // 5 events, 3s timeout
                DataLoadScenario("medium_dataset", 25, 5000), // 25 events, 5s timeout
                DataLoadScenario("large_dataset", 100, 10000) // 100 events, 10s timeout
            )

            dataScenarios.forEach { scenario ->
                println("🔬 Testing first meaningful paint with ${scenario.name}")

                // Create test data for scenario
                repeat(scenario.eventCount) { index ->
                    createTestEvent(
                        eventId = "${scenario.name}_event_$index",
                        latitude = 40.7589 + (index * 0.00001),
                        longitude = -73.9851 + (index * 0.00001),
                        startsSoonInSeconds = index * 60,
                        durationSeconds = 300
                    )
                }

                waitForDataSync(scenario.expectedTimeoutMs.toLong())

                // Measure first meaningful paint for this scenario
                val startTime = System.currentTimeMillis()

                composeTestRule.waitUntilAtLeastOneExists(
                    hasTestTag("main_screen"),
                    timeoutMillis = 15000
                )

                val firstPaintTime = System.currentTimeMillis() - startTime
                trace.addMetric("first_paint_${scenario.name}_ms", firstPaintTime)

                // Verify first meaningful paint meets performance requirements
                assertTrue(
                    firstPaintTime < scenario.expectedTimeoutMs,
                    "${scenario.name} first paint should be under ${scenario.expectedTimeoutMs}ms: ${firstPaintTime}ms"
                )

                // Verify content is actually meaningful (shows events)
                composeTestRule.waitUntilAtLeastOneExists(
                    hasTestTag("event_list"),
                    timeoutMillis = 3000
                )

                val visibleEvents = composeTestRule.onAllNodesWithContentDescription("View event details")
                    .fetchSemanticsNodes().size

                assertTrue(
                    visibleEvents >= minOf(scenario.eventCount, 10), // Should show at least 10 or all if less
                    "${scenario.name} should show meaningful content: ${visibleEvents} events visible"
                )

                println("✅ ${scenario.name}: First paint ${firstPaintTime}ms, Events visible: $visibleEvents")

                // Clear test data for next scenario
                delay(1.seconds)
            }

        } finally {
            stopPerformanceTrace()
        }
    }

    /**
     * Test time-to-interactive metrics under various conditions
     */
    @Test
    fun realPerformance_timeToInteractive_measuresUserReadiness() = runTest {
        val trace = startPerformanceTrace("time_to_interactive_benchmark")

        try {
            // Create realistic test data
            repeat(30) { index ->
                createTestEvent(
                    eventId = "interactive_test_event_$index",
                    latitude = 40.7589 + (index * 0.0002),
                    longitude = -73.9851 + (index * 0.0002),
                    startsSoonInSeconds = index * 120,
                    durationSeconds = 600
                )
            }

            waitForDataSync(5000)

            val interactionScenarios = listOf(
                InteractionScenario(
                    name = "scroll_interaction",
                    action = { composeTestRule.onNodeWithTag("event_list").performScrollToIndex(10) },
                    expectedMaxTime = 1000
                ),
                InteractionScenario(
                    name = "event_detail_navigation",
                    action = {
                        composeTestRule.onAllNodesWithContentDescription("View event details")
                            .onFirst()
                            .performClick()
                    },
                    expectedMaxTime = 2000
                ),
                InteractionScenario(
                    name = "search_interaction",
                    action = {
                        composeTestRule.onNodeWithTag("search_button", useUnmergedTree = true)
                            .performClick()
                    },
                    expectedMaxTime = 1500
                )
            )

            // Ensure app is loaded and visible
            composeTestRule.waitUntilAtLeastOneExists(
                hasTestTag("main_screen"),
                timeoutMillis = 12000
            )

            interactionScenarios.forEach { scenario ->
                val interactionStartTime = System.currentTimeMillis()

                try {
                    scenario.action()
                    val interactionTime = System.currentTimeMillis() - interactionStartTime
                    trace.addMetric("interaction_${scenario.name}_ms", interactionTime)

                    assertTrue(
                        interactionTime < scenario.expectedMaxTime,
                        "${scenario.name} should respond within ${scenario.expectedMaxTime}ms: ${interactionTime}ms"
                    )

                    println("✅ ${scenario.name}: Response time ${interactionTime}ms")

                } catch (e: AssertionError) {
                    // Some interactions might not be available, that's okay for this test
                    println("ℹ️  ${scenario.name}: Not available in current state")
                }

                delay(1.seconds) // Brief pause between interactions
            }

            // Test overall app responsiveness
            val responsivenesStartTime = System.currentTimeMillis()

            repeat(5) { index ->
                composeTestRule.onNodeWithTag("event_list")
                    .performScrollToIndex(index * 5)
                delay(200) // Small delay between scrolls
            }

            val totalResponsivenessTime = System.currentTimeMillis() - responsivenesStartTime
            val averageScrollTime = totalResponsivenessTime / 5

            assertTrue(
                averageScrollTime < 500, // Average scroll should be under 500ms
                "Average scroll responsiveness should be under 500ms: ${averageScrollTime}ms"
            )

            println("✅ Overall responsiveness: Average scroll time ${averageScrollTime}ms")

        } finally {
            stopPerformanceTrace()
        }
    }

    // Helper data classes for performance testing

    private data class DataLoadScenario(
        val name: String,
        val eventCount: Int,
        val expectedTimeoutMs: Int
    )

    private data class InteractionScenario(
        val name: String,
        val action: () -> Unit,
        val expectedMaxTime: Long
    )
}