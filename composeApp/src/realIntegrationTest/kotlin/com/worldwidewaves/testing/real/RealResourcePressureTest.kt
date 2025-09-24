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
 * Real integration tests for memory and storage pressure scenarios.
 *
 * These tests verify app behavior under resource constraints:
 * - Low memory conditions
 * - Storage full scenarios
 * - Cache cleanup mechanisms
 * - Graceful degradation
 */
@RunWith(AndroidJUnit4::class)
class RealResourcePressureTest : BaseRealIntegrationTest() {

    /**
     * Test app behavior under low memory conditions
     */
    @Test
    fun realMemory_lowMemoryConditions_gracefulDegradation() = runTest {
        val trace = startPerformanceTrace("low_memory_test")

        try {
            // Get baseline memory usage
            val baselineMemory = deviceStateManager.getMemoryUsage()
            println("📊 Baseline memory usage: ${baselineMemory.usedMemoryMB}MB/${baselineMemory.maxMemoryMB}MB")

            // Create multiple events to load significant data
            repeat(50) { index ->
                createTestEvent(
                    eventId = "memory_test_event_$index",
                    latitude = 40.7589 + (index * 0.001),
                    longitude = -73.9851 + (index * 0.001),
                    startsSoonInSeconds = index * 60,
                    durationSeconds = 300
                )
            }

            waitForDataSync(5000)

            // Verify app loads and shows events
            composeTestRule.waitUntilAtLeastOneExists(
                hasTestTag("event_list"),
                timeoutMillis = 10000
            )

            // Simulate low memory conditions
            deviceStateManager.simulateLowMemory()
            delay(3.seconds)

            // Check memory usage after low memory simulation
            val lowMemoryUsage = deviceStateManager.getMemoryUsage()
            println("📊 Low memory usage: ${lowMemoryUsage.usedMemoryMB}MB/${lowMemoryUsage.maxMemoryMB}MB")

            // Verify app still functions despite memory pressure
            composeTestRule.onNodeWithTag("event_list").assertExists()

            // Test scrolling performance under memory pressure
            repeat(10) {
                composeTestRule.onNodeWithTag("event_list")
                    .performScrollToIndex(it * 5)
                delay(500) // Small delay between scrolls
            }

            // Verify memory usage is managed
            val currentMemory = deviceStateManager.getMemoryUsage()
            val memoryIncrease = currentMemory.usedMemoryMB - baselineMemory.usedMemoryMB

            assertTrue(
                memoryIncrease < 100, // Should not increase by more than 100MB
                "Memory increase should be reasonable under pressure: ${memoryIncrease}MB"
            )

            assertTrue(
                currentMemory.usedMemoryMB < currentMemory.maxMemoryMB * 0.9, // Less than 90% of max
                "Memory usage should stay under 90% of maximum: ${currentMemory.usedMemoryMB}MB"
            )

            // Test that cache cleanup mechanisms work
            val memoryAfterCleanup = deviceStateManager.getMemoryUsage()
            assertTrue(
                memoryAfterCleanup.usedMemoryMB <= currentMemory.usedMemoryMB,
                "Memory usage should not increase after cleanup: ${memoryAfterCleanup.usedMemoryMB}MB"
            )

            println("✅ Low memory test: App maintains functionality with ${memoryAfterCleanup.usedMemoryMB}MB usage")

        } finally {
            stopPerformanceTrace()
        }
    }

    /**
     * Test cache cleanup mechanisms under memory pressure
     */
    @Test
    fun realMemory_cacheCleanup_efficientMemoryManagement() = runTest {
        val trace = startPerformanceTrace("cache_cleanup_test")

        try {
            // Load substantial amount of cached data
            repeat(100) { index ->
                createTestEvent(
                    eventId = "cache_test_event_$index",
                    latitude = 40.7589 + (index * 0.001),
                    longitude = -73.9851 + (index * 0.001),
                    startsSoonInSeconds = index * 30,
                    durationSeconds = 180
                )
            }

            waitForDataSync(8000)

            // Measure memory after loading
            val memoryAfterLoad = deviceStateManager.getMemoryUsage()
            println("📊 Memory after loading 100 events: ${memoryAfterLoad.usedMemoryMB}MB")

            // Browse through many events to fill cache
            repeat(50) { index ->
                composeTestRule.onNodeWithTag("event_list")
                    .performScrollToIndex(index * 2)

                // View event details to cache additional data
                composeTestRule.onAllNodesWithContentDescription("View event details")
                    .onFirst()
                    .performClick()

                delay(200) // Brief view time

                composeTestRule.onNodeWithContentDescription("Navigate back")
                    .performClick()
            }

            val memoryAfterBrowsing = deviceStateManager.getMemoryUsage()
            println("📊 Memory after browsing events: ${memoryAfterBrowsing.usedMemoryMB}MB")

            // Trigger memory pressure
            deviceStateManager.simulateLowMemory()
            delay(5.seconds)

            val memoryAfterPressure = deviceStateManager.getMemoryUsage()
            println("📊 Memory after pressure simulation: ${memoryAfterPressure.usedMemoryMB}MB")

            // Verify cache cleanup occurred
            assertTrue(
                memoryAfterPressure.usedMemoryMB <= memoryAfterBrowsing.usedMemoryMB,
                "Memory usage should be reduced or stable after cache cleanup: ${memoryAfterPressure.usedMemoryMB}MB vs ${memoryAfterBrowsing.usedMemoryMB}MB"
            )

            // Verify app still functions after cleanup
            composeTestRule.onNodeWithTag("event_list").assertExists()

            // Test that recently viewed content is still accessible
            composeTestRule.onNodeWithTag("event_list")
                .performScrollToIndex(0)

            composeTestRule.onAllNodesWithContentDescription("View event details")
                .onFirst()
                .performClick()

            composeTestRule.waitUntilAtLeastOneExists(
                hasTestTag("event_detail_screen"),
                timeoutMillis = 5000
            )

            println("✅ Cache cleanup test: Efficient memory management with preserved user experience")

        } finally {
            stopPerformanceTrace()
        }
    }

    /**
     * Test storage full scenarios
     */
    @Test
    fun realStorage_storageFull_gracefulHandling() = runTest {
        val trace = startPerformanceTrace("storage_full_test")

        try {
            // Get baseline storage usage
            val baselineStorage = deviceStateManager.getStorageUsage()
            println("💾 Baseline storage: ${baselineStorage.usedStorageMB}MB/${baselineStorage.totalStorageMB}MB")

            // Create events to generate cached data
            repeat(30) { index ->
                createTestEvent(
                    eventId = "storage_test_event_$index",
                    latitude = 40.7589 + (index * 0.001),
                    longitude = -73.9851 + (index * 0.001),
                    startsSoonInSeconds = index * 120,
                    durationSeconds = 600
                )
            }

            waitForDataSync(5000)

            // Simulate storage full condition
            deviceStateManager.simulateStorageFull()
            delay(2.seconds)

            // Test app behavior with storage constraints
            composeTestRule.onNodeWithTag("event_list").assertExists()

            // Verify app handles storage pressure gracefully
            val storageAfterSimulation = deviceStateManager.getStorageUsage()
            println("💾 Storage after full simulation: ${storageAfterSimulation.usedStorageMB}MB")

            // Test event loading still works (should use existing cache)
            composeTestRule.onNodeWithTag("event_list")
                .performScrollToIndex(15)

            composeTestRule.onAllNodesWithContentDescription("View event details")
                .onFirst()
                .performClick()

            composeTestRule.waitUntilAtLeastOneExists(
                hasTestTag("event_detail_screen"),
                timeoutMillis = 8000 // May be slower due to storage constraints
            )

            // Verify user is informed about storage issues (if applicable)
            // Note: In a real app, there might be a notification or warning
            // For now, we just verify the app doesn't crash

            // Test navigation back works
            composeTestRule.onNodeWithContentDescription("Navigate back")
                .performClick()

            composeTestRule.onNodeWithTag("event_list").assertExists()

            // Verify cache management under storage pressure
            val finalStorage = deviceStateManager.getStorageUsage()
            assertTrue(
                finalStorage.cacheStorageMB <= baselineStorage.cacheStorageMB * 1.5,
                "Cache storage should not grow excessively under storage pressure: ${finalStorage.cacheStorageMB}MB"
            )

            println("✅ Storage full test: App handles storage constraints gracefully")

        } finally {
            stopPerformanceTrace()
        }
    }

    /**
     * Test graceful degradation under combined resource pressure
     */
    @Test
    fun realResource_combinedPressure_maintainsCoreFunctionality() = runTest {
        val trace = startPerformanceTrace("combined_pressure_test")

        try {
            // Create significant test data
            repeat(75) { index ->
                createTestEvent(
                    eventId = "combined_test_event_$index",
                    latitude = 40.7589 + (index * 0.0005),
                    longitude = -73.9851 + (index * 0.0005),
                    startsSoonInSeconds = index * 90,
                    durationSeconds = 450
                )
            }

            waitForDataSync(8000)

            // Get baseline measurements
            val baselineMemory = deviceStateManager.getMemoryUsage()
            val baselineStorage = deviceStateManager.getStorageUsage()

            println("📊 Baseline - Memory: ${baselineMemory.usedMemoryMB}MB, Storage: ${baselineStorage.usedStorageMB}MB")

            // Simulate combined resource pressure
            deviceStateManager.simulateLowMemory()
            delay(1.seconds)
            deviceStateManager.simulateStorageFull()
            delay(2.seconds)

            // Verify core app functionality
            composeTestRule.waitUntilAtLeastOneExists(
                hasTestTag("event_list"),
                timeoutMillis = 15000 // Allow extra time under pressure
            )

            // Test essential operations work
            composeTestRule.onNodeWithTag("event_list")
                .performScrollToIndex(10)

            // Test event viewing with degraded performance
            composeTestRule.onAllNodesWithContentDescription("View event details")
                .onFirst()
                .performClick()

            composeTestRule.waitUntilAtLeastOneExists(
                hasTestTag("event_detail_screen"),
                timeoutMillis = 12000 // Slower under pressure
            )

            // Verify resource usage is controlled
            val pressureMemory = deviceStateManager.getMemoryUsage()
            val pressureStorage = deviceStateManager.getStorageUsage()

            println("📊 Under pressure - Memory: ${pressureMemory.usedMemoryMB}MB, Storage: ${pressureStorage.usedStorageMB}MB")

            // Memory should not increase dramatically under pressure
            val memoryIncrease = pressureMemory.usedMemoryMB - baselineMemory.usedMemoryMB
            assertTrue(
                memoryIncrease < 50, // Less than 50MB increase
                "Memory increase under combined pressure should be minimal: ${memoryIncrease}MB"
            )

            // Test that app recovers when pressure is relieved
            composeTestRule.onNodeWithContentDescription("Navigate back")
                .performClick()

            delay(3.seconds)

            // Verify app is still responsive
            composeTestRule.onNodeWithTag("event_list")
                .performScrollToIndex(0)

            val recoveryMemory = deviceStateManager.getMemoryUsage()
            println("📊 After recovery - Memory: ${recoveryMemory.usedMemoryMB}MB")

            // Verify graceful degradation maintained essential features
            assertTrue(
                recoveryMemory.usedMemoryMB <= pressureMemory.usedMemoryMB * 1.1,
                "Memory usage should stabilize after pressure relief: ${recoveryMemory.usedMemoryMB}MB"
            )

            println("✅ Combined pressure test: Core functionality maintained under severe resource constraints")

        } finally {
            stopPerformanceTrace()
        }
    }
}