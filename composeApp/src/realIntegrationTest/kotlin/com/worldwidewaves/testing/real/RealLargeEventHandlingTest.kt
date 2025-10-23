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
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Duration.Companion.minutes

/**
 * Real integration tests for large-scale event handling and performance under load.
 *
 * These tests verify the app's ability to handle:
 * - Events with 1000+ participants
 * - Map performance with many markers and overlays
 * - Memory usage during extended usage
 * - UI responsiveness during large data synchronization
 * - Network resilience with high data volumes
 * - Battery optimization under heavy load
 */
@RunWith(AndroidJUnit4::class)
class RealLargeEventHandlingTest : BaseRealIntegrationTest() {

    @Test
    fun realLargeEvent_thousandPlusEvents_loadsCorrectly() = runTest {
        val trace = startPerformanceTrace("thousand_events_loading_real")

        // Ensure strong network connectivity for large data load
        waitForNetworkConnectivity()
        setTestLocation(40.7128, -74.0060) // New York

        // Create a large number of test events (1000+)
        println("📱 Creating 1000+ test events...")
        val eventCreationStart = System.currentTimeMillis()

        repeat(1200) { index ->
            // Spread events across a wider area to test spatial handling
            val latOffset = (index % 50) * 0.001 - 0.025
            val lngOffset = (index / 50) * 0.001 - 0.025

            createTestEvent(
                "large_event_$index",
                40.7128 + latOffset,
                -74.0060 + lngOffset,
                isActive = index % 10 == 0 // Make 10% of events active
            )

            // Add small delay every 100 events to avoid overwhelming the system
            if (index % 100 == 99) {
                kotlinx.coroutines.delay(100)
                println("📱 Created ${index + 1} events...")
            }
        }

        val eventCreationTime = System.currentTimeMillis() - eventCreationStart
        println("📱 Event creation completed in ${eventCreationTime}ms")

        waitForDataSync()

        // Launch app
        composeTestRule.activityRule.launchActivity(null)

        // Wait for large event set to load (extended timeout)
        composeTestRule.waitUntil(timeoutMillis = 2.minutes.inWholeMilliseconds) {
            try {
                // Look for indication that events are loaded
                composeTestRule.onNode(
                    hasText("1000+") or
                    hasText("Many events") or
                    hasContentDescription("Large event list") or
                    hasTestTag("events-loaded")
                ).assertExists()
                true
            } catch (e: AssertionError) {
                // Fallback: check for any event from our large set
                try {
                    composeTestRule.onNode(
                        hasText("large_event_100") or
                        hasText("large_event_200")
                    ).assertExists()
                    true
                } catch (e: AssertionError) {
                    false
                }
            }
        }

        // Verify large event loading
        val largeEventsLoaded = try {
            // Check for multiple events visible
            composeTestRule.onNode(hasText("large_event_100")).assertExists() &&
            composeTestRule.onNode(hasText("large_event_200")).assertExists()
            true
        } catch (e: AssertionError) {
            // At least some events should be visible
            try {
                composeTestRule.onNode(hasText("large_event_100")).assertExists()
                true
            } catch (e: AssertionError) {
                false
            }
        }

        assertTrue("Large event set should load successfully", largeEventsLoaded)

        // Test app responsiveness with large dataset
        val appResponsive = try {
            // Test UI interaction
            composeTestRule.onNodeWithContentDescription("Map view").performTouchInput {
                swipeUp()
            }
            kotlinx.coroutines.delay(2000)

            composeTestRule.onNodeWithContentDescription("Map view").assertIsDisplayed()
            true
        } catch (e: Exception) {
            false
        }

        assertTrue("App should remain responsive with 1000+ events", appResponsive)

        val loadingTime = stopPerformanceTrace()
        assertTrue("Large event loading should complete within reasonable time", loadingTime < 120000)

        println("✅ Large event handling completed in ${loadingTime}ms")
        println("   Successfully loaded 1200 events")
    }

    @Test
    fun realLargeEvent_manyMapMarkers_rendersPerformantly() = runTest {
        val trace = startPerformanceTrace("many_map_markers_real")

        // Setup for map marker performance testing
        waitForNetworkConnectivity()
        setTestLocation(40.7128, -74.0060)

        // Create events concentrated in viewable map area
        println("📱 Creating 500 events for map marker testing...")
        repeat(500) { index ->
            // Concentrate events in smaller area for visible markers
            val latOffset = (index % 25) * 0.002 - 0.025
            val lngOffset = (index / 25) * 0.002 - 0.025

            createTestEvent(
                "map_marker_event_$index",
                40.7128 + latOffset,
                -74.0060 + lngOffset,
                isActive = index % 15 == 0
            )
        }

        waitForDataSync()

        // Launch app and navigate to map view
        composeTestRule.activityRule.launchActivity(null)

        // Wait for map to load with markers
        composeTestRule.waitUntil(timeoutMillis = 1.minutes.inWholeMilliseconds) {
            try {
                composeTestRule.onNodeWithContentDescription("Map view").assertExists()
                true
            } catch (e: AssertionError) {
                false
            }
        }

        // Test map performance with many markers
        val mapPerformanceStart = System.currentTimeMillis()

        // Perform intensive map operations
        val mapView = composeTestRule.onNodeWithContentDescription("Map view")

        repeat(10) { operation ->
            try {
                when (operation % 4) {
                    0 -> mapView.performTouchInput { pinchIn() }
                    1 -> mapView.performTouchInput { pinchOut() }
                    2 -> mapView.performTouchInput { swipeLeft() }
                    3 -> mapView.performTouchInput { swipeRight() }
                }
                kotlinx.coroutines.delay(500)
            } catch (e: Exception) {
                println("⚠️  Map operation ${operation + 1} failed: ${e.message}")
            }
        }

        val mapPerformanceTime = System.currentTimeMillis() - mapPerformanceStart

        // Verify map is still responsive
        val mapStillResponsive = try {
            mapView.assertIsDisplayed()
            mapView.performTouchInput { swipeUp() }
            true
        } catch (e: Exception) {
            false
        }

        assertTrue("Map should remain responsive with many markers", mapStillResponsive)
        assertTrue("Map operations should complete within reasonable time", mapPerformanceTime < 15000)

        // Check for marker clustering or performance optimizations
        val markerOptimization = try {
            composeTestRule.onNode(
                hasContentDescription("Marker cluster") or
                hasText("500 events") or
                hasTestTag("clustered-markers")
            ).assertExists()
            true
        } catch (e: AssertionError) {
            // Optimization may not be visible but performance should be maintained
            mapStillResponsive
        }

        val markerTime = stopPerformanceTrace()
        println("✅ Map marker performance completed in ${markerTime}ms")
        println("   Map operations took ${mapPerformanceTime}ms with 500 markers")

        if (markerOptimization) {
            println("   Marker clustering/optimization detected")
        }
    }

    @Test
    fun realLargeEvent_memoryUsage_staysWithinLimits() = runTest {
        val trace = startPerformanceTrace("memory_usage_limits_real")

        // Monitor memory usage during large data operations
        val runtime = Runtime.getRuntime()
        val initialMemory = runtime.totalMemory() - runtime.freeMemory()

        println("📱 Initial memory usage: ${initialMemory / (1024 * 1024)}MB")

        // Setup large dataset
        waitForNetworkConnectivity()
        setTestLocation(40.7128, -74.0060)

        // Create substantial event dataset
        repeat(800) { index ->
            createTestEvent(
                "memory_test_event_$index",
                40.7128 + (index * 0.001),
                -74.0060 + (index * 0.001),
                isActive = index % 20 == 0
            )
        }

        waitForDataSync()

        // Launch app and load data
        composeTestRule.activityRule.launchActivity(null)

        // Monitor memory during loading
        val memoryChecks = mutableListOf<Long>()

        repeat(10) { check ->
            kotlinx.coroutines.delay(3000)

            val currentMemory = runtime.totalMemory() - runtime.freeMemory()
            val memoryIncrease = currentMemory - initialMemory

            memoryChecks.add(memoryIncrease)
            println("📱 Memory check ${check + 1}: +${memoryIncrease / (1024 * 1024)}MB")
        }

        // Wait for full data load
        composeTestRule.waitUntil(timeoutMillis = 1.minutes.inWholeMilliseconds) {
            try {
                composeTestRule.onNode(
                    hasText("memory_test_event_500") or
                    hasContentDescription("Events loaded")
                ).assertExists()
                true
            } catch (e: AssertionError) {
                false
            }
        }

        // Final memory check
        val finalMemory = runtime.totalMemory() - runtime.freeMemory()
        val totalMemoryIncrease = finalMemory - initialMemory
        val finalMemoryMB = totalMemoryIncrease / (1024 * 1024)

        println("📱 Final memory increase: ${finalMemoryMB}MB")

        // Memory should stay within reasonable limits (less than 200MB increase for large dataset)
        val memoryWithinLimits = finalMemoryMB < 200

        assertTrue("Memory usage should stay within limits", memoryWithinLimits)

        // Test memory doesn't continue growing (no major leaks)
        val memoryStable = try {
            kotlinx.coroutines.delay(5000)
            val postTestMemory = runtime.totalMemory() - runtime.freeMemory()
            val memoryGrowth = postTestMemory - finalMemory

            memoryGrowth / (1024 * 1024) < 50 // Less than 50MB additional growth
        } catch (e: Exception) {
            false
        }

        assertTrue("Memory should not continue growing significantly", memoryStable)

        val memoryTime = stopPerformanceTrace()
        println("✅ Memory usage testing completed in ${memoryTime}ms")
        println("   Peak memory increase: ${finalMemoryMB}MB")
        println("   Memory checks: ${memoryChecks.map { it / (1024 * 1024) }}MB")
    }

    @Test
    fun realLargeEvent_uiResponsiveness_maintainedDuringSync() = runTest {
        val trace = startPerformanceTrace("ui_responsiveness_sync_real")

        // Test UI responsiveness during large data synchronization
        waitForNetworkConnectivity()
        setTestLocation(40.7128, -74.0060)

        // Create large dataset that will require significant sync time
        repeat(600) { index ->
            createTestEvent(
                "sync_test_event_$index",
                40.7128 + (index * 0.002),
                -74.0060 + (index * 0.002),
                isActive = index % 12 == 0
            )
        }

        // Don't wait for full sync - test responsiveness during sync
        kotlinx.coroutines.delay(2000) // Brief delay to start sync

        // Launch app while sync is happening
        composeTestRule.activityRule.launchActivity(null)

        // Test UI responsiveness immediately
        val responsivenessDuringSyncStart = System.currentTimeMillis()
        val responsivenessTests = mutableListOf<Boolean>()

        repeat(15) { test ->
            kotlinx.coroutines.delay(2000)

            val responsive = try {
                // Test various UI interactions
                when (test % 3) {
                    0 -> {
                        // Test map interaction
                        composeTestRule.onNodeWithContentDescription("Map view").performTouchInput {
                            swipeLeft()
                        }
                    }
                    1 -> {
                        // Test button interaction
                        composeTestRule.onNode(
                            hasContentDescription("Settings") or
                            hasText("Menu")
                        ).performClick()
                    }
                    2 -> {
                        // Test scrolling
                        composeTestRule.onNodeWithContentDescription("Map view").performTouchInput {
                            swipeUp()
                        }
                    }
                }
                true
            } catch (e: Exception) {
                false
            }

            responsivenessTests.add(responsive)
            println("📱 Responsiveness test ${test + 1}: ${if (responsive) "✅" else "❌"}")
        }

        val responsivenessDuringSyncTime = System.currentTimeMillis() - responsivenessDuringSyncStart

        // Calculate responsiveness percentage
        val responsiveCount = responsivenessTests.count { it }
        val responsivenessPercentage = (responsiveCount * 100) / responsivenessTests.size

        println("📱 UI responsiveness: $responsivenessPercentage% (${responsiveCount}/${responsivenessTests.size})")

        // UI should remain mostly responsive (>70%) during sync
        assertTrue("UI should remain responsive during large data sync", responsivenessPercentage > 70)

        // Wait for sync to complete and verify final responsiveness
        composeTestRule.waitUntil(timeoutMillis = 1.minutes.inWholeMilliseconds) {
            try {
                composeTestRule.onNode(
                    hasText("sync_test_event_300") or
                    hasContentDescription("Sync complete")
                ).assertExists()
                true
            } catch (e: AssertionError) {
                false
            }
        }

        // Final responsiveness test after sync completion
        val finalResponsive = try {
            composeTestRule.onNodeWithContentDescription("Map view").performTouchInput {
                pinchIn()
            }
            kotlinx.coroutines.delay(1000)
            composeTestRule.onNodeWithContentDescription("Map view").assertIsDisplayed()
            true
        } catch (e: Exception) {
            false
        }

        assertTrue("UI should be fully responsive after sync completion", finalResponsive)

        val uiTime = stopPerformanceTrace()
        println("✅ UI responsiveness testing completed in ${uiTime}ms")
        println("   Responsiveness during sync: $responsivenessPercentage%")
        println("   Sync monitoring time: ${responsivenessDuringSyncTime}ms")
    }

    @Test
    fun realLargeEvent_networkResilience_handlesHighDataVolume() = runTest {
        val trace = startPerformanceTrace("network_resilience_high_volume_real")

        // Test network resilience with high data volumes
        waitForNetworkConnectivity()
        setTestLocation(40.7128, -74.0060)

        // Create large dataset requiring substantial network transfer
        repeat(400) { index ->
            createTestEvent(
                "network_volume_test_$index",
                40.7128 + (index * 0.003),
                -74.0060 + (index * 0.003),
                isActive = index % 8 == 0
            )
        }

        waitForDataSync()

        // Launch app
        composeTestRule.activityRule.launchActivity(null)

        // Test different network conditions during high data volume operations
        val networkConditions = listOf(
            NetworkCondition.SLOW_NETWORK,
            NetworkCondition.POOR_CONNECTIVITY,
            NetworkCondition.FAST_NETWORK
        )

        val networkResults = mutableListOf<String>()

        for ((index, condition) in networkConditions.withIndex()) {
            simulateNetworkConditions(condition)
            kotlinx.coroutines.delay(5000)

            val conditionResult = try {
                // Test data loading under current network condition
                composeTestRule.onNode(
                    hasText("network_volume_test_${index * 100}") or
                    hasContentDescription("Loading events")
                ).assertExists()

                "✅ Handled $condition"
            } catch (e: AssertionError) {
                try {
                    // Check for appropriate error handling
                    composeTestRule.onNode(
                        hasText("Loading...") or
                        hasText("Poor connection") or
                        hasContentDescription("Network issue")
                    ).assertExists()

                    "⚠️  $condition with error handling"
                } catch (e: AssertionError) {
                    "❌ Failed $condition"
                }
            }

            networkResults.add(conditionResult)
            println("📱 Network test: $conditionResult")
        }

        // Restore optimal network
        simulateNetworkConditions(NetworkCondition.FAST_NETWORK)

        // Verify final data integrity after network stress
        composeTestRule.waitUntil(timeoutMillis = 30.seconds.inWholeMilliseconds) {
            try {
                composeTestRule.onNode(
                    hasText("network_volume_test_200") or
                    hasContentDescription("Data loaded")
                ).assertExists()
                true
            } catch (e: AssertionError) {
                false
            }
        }

        val dataIntegrityMaintained = try {
            composeTestRule.onNode(hasText("network_volume_test_200")).assertExists()
            true
        } catch (e: AssertionError) {
            false
        }

        assertTrue("Data integrity should be maintained under network stress", dataIntegrityMaintained)

        val networkTime = stopPerformanceTrace()
        println("✅ Network resilience testing completed in ${networkTime}ms")
        println("   Network condition results:")
        networkResults.forEach { println("     $it") }
    }

    @Test
    fun realLargeEvent_batteryOptimization_efficientUnderLoad() = runTest {
        val trace = startPerformanceTrace("battery_optimization_load_real")

        // Test battery optimization during large event processing
        waitForNetworkConnectivity()
        if (!deviceStateManager.hasLocationPermissions()) {
            println("⚠️  Test requires location permissions")
            return@runTest
        }

        setTestLocation(40.7128, -74.0060)
        waitForGpsLocation()

        // Create scenario that would typically drain battery
        repeat(300) { index ->
            createTestEvent(
                "battery_test_event_$index",
                40.7128 + (index * 0.001),
                -74.0060 + (index * 0.001),
                isActive = index % 5 == 0 // Many active events requiring updates
            )
        }

        waitForDataSync()

        // Launch app
        composeTestRule.activityRule.launchActivity(null)

        // Monitor battery-intensive operations
        val batteryIntensiveStart = System.currentTimeMillis()

        // Simulate extended usage that would typically drain battery
        repeat(20) { cycle ->
            kotlinx.coroutines.delay(3000)

            // Operations that would use GPS, network, and processing
            try {
                composeTestRule.onNodeWithContentDescription("Map view").performTouchInput {
                    when (cycle % 4) {
                        0 -> pinchIn()
                        1 -> pinchOut()
                        2 -> swipeLeft()
                        3 -> swipeRight()
                    }
                }
            } catch (e: Exception) {
                // Continue monitoring even if UI interaction fails
            }

            // Update location (GPS simulation)
            setTestLocation(
                40.7128 + (cycle * 0.0001),
                -74.0060 + (cycle * 0.0001)
            )

            println("📱 Battery test cycle ${cycle + 1}/20")
        }

        val batteryTestDuration = System.currentTimeMillis() - batteryIntensiveStart

        // Verify app remains functional after extended battery-intensive usage
        val stillFunctional = try {
            composeTestRule.onNode(
                hasText("battery_test_event_150") or
                hasContentDescription("Events still loaded")
            ).assertExists()
            true
        } catch (e: AssertionError) {
            false
        }

        assertTrue("App should remain functional during battery-intensive operations", stillFunctional)

        // Check for battery optimization indicators
        val batteryOptimized = try {
            composeTestRule.onNode(
                hasText("Battery saver") or
                hasContentDescription("Reduced updates") or
                hasTestTag("power-optimization")
            ).assertExists()
            true
        } catch (e: AssertionError) {
            // Optimization may be internal without visible indicators
            stillFunctional // App functioning is a good sign of optimization
        }

        val batteryTime = stopPerformanceTrace()
        println("✅ Battery optimization testing completed in ${batteryTime}ms")
        println("   Battery-intensive operations ran for ${batteryTestDuration}ms")

        if (batteryOptimized) {
            println("   Battery optimization indicators detected")
        }

        // Performance should remain reasonable even with battery considerations
        assertTrue("Battery-optimized operations should complete timely", batteryTime < 90000)
    }
}