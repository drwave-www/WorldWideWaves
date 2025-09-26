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
import PerformanceTrace
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Real integration tests for battery and power management scenarios.
 *
 * These tests verify app behavior under various power management conditions:
 * - Battery saver mode impact
 * - Doze mode and background restrictions
 * - Location tracking power optimization
 * - Background task efficiency
 */
@RunWith(AndroidJUnit4::class)
class RealBatteryManagementTest : BaseRealIntegrationTest() {

    /**
     * Test app behavior under battery saver mode
     */
    @Test
    fun realBattery_batterySaverMode_maintainsCoreFunctionality() = runTest {
        val trace = startPerformanceTrace("battery_saver_test")

        try {
            // Create test event for battery testing
            createTestEvent(
                eventId = "battery_test_event",
                latitude = 40.7589,
                longitude = -73.9851,
                startsSoonInSeconds = 30,
                durationSeconds = 300
            )

            // Simulate battery saver mode conditions
            deviceStateManager.simulateBatterySaverMode(true)

            // Wait for app to adapt to battery saver mode
            delay(2.seconds)

            // Verify app launches and shows main screen
            composeTestRule.waitUntilAtLeastOneExists(
                hasTestTag("main_screen"),
                timeoutMillis = 10000
            )

            // Test core functionality still works
            composeTestRule.onNodeWithTag("event_list").assertExists()

            // Verify location services are optimized but functional
            val locationOptimized = deviceStateManager.verifyLocationOptimization()
            assertTrue(locationOptimized, "Location services should be optimized in battery saver mode")

            // Test event viewing still works
            composeTestRule.onNodeWithTag("event_list")
                .performScrollToNode(hasText("battery_test_event"))

            composeTestRule.onNodeWithContentDescription("View event details")
                .performClick()

            composeTestRule.waitUntilAtLeastOneExists(
                hasTestTag("event_detail_screen"),
                timeoutMillis = 5000
            )

            // Verify battery usage is within acceptable limits
            val batteryUsage = trace.getBatteryUsage()
            assertTrue(
                batteryUsage.backgroundCpuMs < 5000, // Less than 5 seconds CPU in background
                "Background CPU usage should be minimal in battery saver mode: ${batteryUsage.backgroundCpuMs}ms"
            )

            println("✅ Battery saver mode test: Core functionality maintained with optimized usage")

        } finally {
            // Disable battery saver simulation
            deviceStateManager.simulateBatterySaverMode(false)
            stopPerformanceTrace()
        }
    }

    /**
     * Test location tracking behavior with doze mode
     */
    @Test
    fun realBattery_dozeMode_maintainsLocationAccuracy() = runTest {
        val trace = startPerformanceTrace("doze_mode_test")

        try {
            // Set test location for consistent testing
            setTestLocation(40.7128, -74.0060) // NYC coordinates

            // Create active wave event
            createTestEvent(
                eventId = "doze_test_wave",
                latitude = 40.7128,
                longitude = -74.0060,
                isActive = true,
                durationSeconds = 600
            )

            // Wait for data sync
            waitForDataSync(3000)

            // Join the wave to enable location tracking
            composeTestRule.onNodeWithTag("join_wave_button").performClick()

            // Simulate device entering doze mode
            deviceStateManager.simulateDozeMode(true)
            delay(5.seconds)

            // Verify location updates continue with reduced frequency
            val locationUpdates = deviceStateManager.getLocationUpdateFrequency()
            assertTrue(
                locationUpdates.intervalMs >= 30000, // At least 30 second intervals in doze
                "Location updates should be reduced in doze mode: ${locationUpdates.intervalMs}ms"
            )

            assertTrue(
                locationUpdates.stillReceiving,
                "Should still receive location updates in doze mode for active waves"
            )

            // Test wake-up for important wave events
            deviceStateManager.simulateWaveProgressionAlert()
            delay(2.seconds)

            // Verify app wakes up and processes wave updates
            composeTestRule.onNodeWithTag("wave_progress_indicator")
                .assertExists()

            // Check that doze mode doesn't break wave coordination
            val waveStatus = deviceStateManager.getWaveCoordinationStatus()
            assertTrue(
                waveStatus.isActive && waveStatus.isReceivingUpdates,
                "Wave coordination should remain active during doze mode"
            )

            println("✅ Doze mode test: Location tracking optimized but functional")

        } finally {
            deviceStateManager.simulateDozeMode(false)
            stopPerformanceTrace()
        }
    }

    /**
     * Test power-efficient operation during extended usage
     */
    @Test
    fun realBattery_extendedUsage_remainsPowerEfficient() = runTest {
        val trace = startPerformanceTrace("extended_usage_test")

        try {
            // Create multiple test events for extended testing
            repeat(5) { index ->
                createTestEvent(
                    eventId = "extended_test_event_$index",
                    latitude = 40.7589 + (index * 0.001),
                    longitude = -73.9851 + (index * 0.001),
                    startsSoonInSeconds = index * 120, // Staggered start times
                    durationSeconds = 300
                )
            }

            waitForDataSync(5000)

            // Simulate extended app usage (30 minutes)
            val extendedUsageDuration = 30.minutes
            val startTime = System.currentTimeMillis()

            // Perform realistic user interactions over time
            while (System.currentTimeMillis() - startTime < extendedUsageDuration.inWholeMilliseconds) {
                // Browse events
                composeTestRule.onNodeWithTag("event_list")
                    .performScrollToIndex(kotlin.random.Random.nextInt(0, 5))

                delay(5.seconds)

                // View event details
                composeTestRule.onAllNodesWithContentDescription("View event details")
                    .onFirst()
                    .performClick()

                delay(3.seconds)

                // Go back to list
                composeTestRule.onNodeWithContentDescription("Navigate back")
                    .performClick()

                delay(2.seconds)

                // Check battery usage periodically
                val currentBatteryUsage = trace.getBatteryUsage()
                if (currentBatteryUsage.totalPowerMah > 50) {
                    // If power usage exceeds 50mAh, we should investigate
                    println("⚠️  High power usage detected: ${currentBatteryUsage.totalPowerMah}mAh")
                    break
                }

                // Short break between interactions
                delay(10.seconds)
            }

            // Verify final battery usage metrics
            val finalBatteryUsage = trace.getBatteryUsage()

            assertTrue(
                finalBatteryUsage.totalPowerMah < 100, // Less than 100mAh for 30 min usage
                "Extended usage should be power efficient: ${finalBatteryUsage.totalPowerMah}mAh"
            )

            assertTrue(
                finalBatteryUsage.averageCpuPercent < 15, // Less than 15% average CPU
                "CPU usage should remain reasonable: ${finalBatteryUsage.averageCpuPercent}%"
            )

            // Verify location tracking efficiency
            val locationEfficiency = deviceStateManager.getLocationTrackingEfficiency()
            assertTrue(
                locationEfficiency.powerEfficiencyScore > 0.8, // 80%+ efficiency
                "Location tracking should be power efficient: ${locationEfficiency.powerEfficiencyScore}"
            )

            println("✅ Extended usage test: Power consumption within acceptable limits")

        } finally {
            stopPerformanceTrace()
        }
    }

    /**
     * Test background restrictions impact on app functionality
     */
    @Test
    fun realBattery_backgroundRestrictions_maintainsEssentialServices() = runTest {
        val trace = startPerformanceTrace("background_restrictions_test")

        try {
            // Create critical wave event
            createTestEvent(
                eventId = "critical_wave_test",
                latitude = 40.7589,
                longitude = -73.9851,
                isActive = true,
                durationSeconds = 900
            )

            // Join the wave
            composeTestRule.onNodeWithTag("join_wave_button").performClick()
            delay(2.seconds)

            // Simulate strict background app restrictions
            deviceStateManager.simulateBackgroundRestrictions(strict = true)

            // Put app in background
            deviceStateManager.simulateAppBackground()
            delay(10.seconds)

            // Verify essential services still function
            val backgroundServices = deviceStateManager.getBackgroundServiceStatus()

            assertTrue(
                backgroundServices.waveCoordinationActive,
                "Wave coordination should remain active despite background restrictions"
            )

            assertTrue(
                backgroundServices.criticalNotificationsEnabled,
                "Critical notifications should work despite restrictions"
            )

            // Test app returning to foreground
            deviceStateManager.simulateAppForeground()
            delay(3.seconds)

            // Verify app state is properly restored
            composeTestRule.onNodeWithTag("wave_active_indicator")
                .assertExists()

            // Test that non-essential background tasks are properly limited
            val backgroundTaskUsage = trace.getBackgroundTaskUsage()
            assertTrue(
                backgroundTaskUsage.nonEssentialTasksLimited,
                "Non-essential background tasks should be limited under restrictions"
            )

            assertTrue(
                backgroundTaskUsage.essentialTasksMaintained,
                "Essential tasks should be maintained under background restrictions"
            )

            println("✅ Background restrictions test: Essential services maintained")

        } finally {
            deviceStateManager.simulateBackgroundRestrictions(strict = false)
            deviceStateManager.simulateAppForeground()
            stopPerformanceTrace()
        }
    }
}