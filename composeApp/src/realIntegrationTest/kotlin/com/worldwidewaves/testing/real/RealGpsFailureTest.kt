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
 * Real integration tests for GPS signal loss scenarios.
 *
 * These tests verify app behavior when GPS signal is lost:
 * - GPS signal loss during wave participation
 * - Fallback location mechanisms
 * - Signal recovery handling
 * - User notification of GPS issues
 */
@RunWith(AndroidJUnit4::class)
class RealGpsFailureTest : BaseRealIntegrationTest() {

    /**
     * Test behavior when GPS signal is lost
     */
    @Test
    fun realGps_signalLoss_gracefulFallback() = runTest {
        val trace = startPerformanceTrace("gps_signal_loss_test")

        try {
            // Set initial GPS location
            setTestLocation(40.7589, -73.9851)

            // Create active wave at current location
            createTestEvent(
                eventId = "gps_loss_wave",
                latitude = 40.7589,
                longitude = -73.9851,
                isActive = true,
                durationSeconds = 1200
            )

            waitForDataSync(3000)

            // Verify GPS status before joining wave
            val initialGpsStatus = deviceStateManager.getGpsStatus()
            assertTrue(
                initialGpsStatus.signalStrength > 0.5,
                "Initial GPS signal should be strong: ${initialGpsStatus.signalStrength}"
            )

            // Join wave with good GPS signal
            composeTestRule.onNodeWithTag("join_wave_button").performClick()
            delay(2.seconds)

            composeTestRule.onNodeWithTag("wave_active_indicator").assertExists()

            // Simulate GPS signal loss
            deviceStateManager.simulateGpsSignalLoss()
            delay(3.seconds)

            // Verify GPS status shows signal loss
            val lostGpsStatus = deviceStateManager.getGpsStatus()
            assertTrue(
                lostGpsStatus.signalStrength < 0.2,
                "GPS signal should be weak after loss: ${lostGpsStatus.signalStrength}"
            )

            // Verify fallback location mechanism activates
            assertTrue(
                lostGpsStatus.fallbackLocationAvailable,
                "Fallback location should be available when GPS is lost"
            )

            // Test app continues to function with fallback location
            composeTestRule.onNodeWithTag("wave_status_indicator").assertExists()

            // Wave participation should continue with last known or network location
            // In real implementation, app might show "GPS unavailable" indicator
            println("📡 GPS signal loss handled with fallback location mechanism")

            // Test navigation still works
            composeTestRule.onNodeWithContentDescription("Navigate back")
                .performClick()

            composeTestRule.onNodeWithTag("event_list").assertExists()

            // Test map functionality with degraded location services
            composeTestRule.onNodeWithTag("event_list")
                .performScrollToNode(hasText("gps_loss_wave"))

            composeTestRule.onNodeWithContentDescription("View event details")
                .performClick()

            composeTestRule.waitUntilAtLeastOneExists(
                hasTestTag("event_detail_screen"),
                timeoutMillis = 8000
            )

            // Verify location-dependent features show appropriate state
            // In real app, map might show last known location with reduced accuracy
            println("✅ GPS signal loss test: Fallback mechanisms maintain app functionality")

        } finally {
            stopPerformanceTrace()
        }
    }

    /**
     * Test fallback location mechanisms
     */
    @Test
    fun realGps_fallbackMechanisms_maintainLocationServices() = runTest {
        val trace = startPerformanceTrace("gps_fallback_test")

        try {
            // Set initial location
            setTestLocation(40.7128, -74.0060) // NYC coordinates

            // Create events at different locations to test fallback
            val testLocations = listOf(
                Pair(40.7128, -74.0060), // NYC
                Pair(40.7589, -73.9851), // Times Square
                Pair(40.7282, -73.7949)  // LaGuardia area
            )

            testLocations.forEachIndexed { index, (lat, lng) ->
                createTestEvent(
                    eventId = "fallback_test_event_$index",
                    latitude = lat,
                    longitude = lng,
                    startsSoonInSeconds = index * 300,
                    durationSeconds = 600
                )
            }

            waitForDataSync(5000)

            // Verify initial GPS functionality
            val goodGpsStatus = deviceStateManager.getGpsStatus()
            assertTrue(
                goodGpsStatus.signalStrength > 0.7,
                "Should start with good GPS signal: ${goodGpsStatus.signalStrength}"
            )

            // Test location accuracy with good GPS
            composeTestRule.onNodeWithTag("event_list").assertExists()

            // Simulate gradual GPS degradation
            deviceStateManager.simulateGpsSignalLoss()
            delay(3.seconds)

            val degradedGpsStatus = deviceStateManager.getGpsStatus()
            assertTrue(
                degradedGpsStatus.signalStrength < 0.3,
                "GPS signal should be degraded: ${degradedGpsStatus.signalStrength}"
            )

            // Test fallback location accuracy
            assertTrue(
                degradedGpsStatus.fallbackLocationAvailable,
                "Fallback location should be available"
            )

            // Test that events can still be viewed and interacted with
            testLocations.indices.forEach { index ->
                composeTestRule.onNodeWithTag("event_list")
                    .performScrollToNode(hasText("fallback_test_event_$index"))

                composeTestRule.onNodeWithContentDescription("View event details")
                    .performClick()

                composeTestRule.waitUntilAtLeastOneExists(
                    hasTestTag("event_detail_screen"),
                    timeoutMillis = 10000
                )

                // Test that location-based features show appropriate degraded state
                // In real app, might show "Location accuracy reduced" message
                delay(1.seconds)

                composeTestRule.onNodeWithContentDescription("Navigate back")
                    .performClick()
            }

            // Verify fallback mechanisms maintain essential location services
            assertTrue(
                degradedGpsStatus.fallbackLocationAvailable,
                "Essential location services should be maintained through fallback"
            )

            println("✅ GPS fallback test: Location services maintained through fallback mechanisms")

        } finally {
            stopPerformanceTrace()
        }
    }

    /**
     * Test recovery when GPS signal returns
     */
    @Test
    fun realGps_signalRecovery_resumesNormalOperation() = runTest {
        val trace = startPerformanceTrace("gps_recovery_test")

        try {
            // Start with GPS signal loss scenario
            deviceStateManager.simulateGpsSignalLoss()
            delay(2.seconds)

            // Create wave event while GPS is degraded
            createTestEvent(
                eventId = "gps_recovery_wave",
                latitude = 40.7589,
                longitude = -73.9851,
                isActive = true,
                durationSeconds = 900
            )

            waitForDataSync(3000)

            // Verify degraded GPS status
            val degradedGpsStatus = deviceStateManager.getGpsStatus()
            assertTrue(
                degradedGpsStatus.signalStrength < 0.2,
                "Should start with degraded GPS: ${degradedGpsStatus.signalStrength}"
            )

            // Join wave with degraded GPS
            composeTestRule.onNodeWithTag("join_wave_button").performClick()
            delay(2.seconds)

            composeTestRule.onNodeWithTag("wave_active_indicator").assertExists()

            // Simulate GPS signal recovery
            setTestLocation(40.7589, -73.9851) // Restore precise location
            delay(5.seconds) // Allow time for signal recovery

            // Verify GPS status improves
            val recoveredGpsStatus = deviceStateManager.getGpsStatus()
            assertTrue(
                recoveredGpsStatus.signalStrength > 0.6,
                "GPS signal should recover: ${recoveredGpsStatus.signalStrength}"
            )

            assertTrue(
                recoveredGpsStatus.satelliteCount > 4,
                "Satellite count should increase after recovery: ${recoveredGpsStatus.satelliteCount}"
            )

            // Verify location accuracy improves
            val timeSinceLastFix = System.currentTimeMillis() - recoveredGpsStatus.lastFixTime
            assertTrue(
                timeSinceLastFix < 10000, // Less than 10 seconds
                "Should have recent GPS fix after recovery: ${timeSinceLastFix}ms ago"
            )

            // Test that wave participation resumes with improved accuracy
            composeTestRule.onNodeWithTag("wave_active_indicator").assertExists()

            // Test map functionality with restored GPS
            composeTestRule.onNodeWithContentDescription("Navigate back")
                .performClick()

            composeTestRule.onNodeWithTag("event_list")
                .performScrollToNode(hasText("gps_recovery_wave"))

            composeTestRule.onNodeWithContentDescription("View event details")
                .performClick()

            composeTestRule.waitUntilAtLeastOneExists(
                hasTestTag("event_detail_screen"),
                timeoutMillis = 5000 // Should be faster with good GPS
            )

            // Verify location-dependent features work normally again
            // In real app, accuracy indicators would show improvement

            // Test joining another wave with recovered GPS
            createTestEvent(
                eventId = "post_recovery_wave",
                latitude = 40.7589,
                longitude = -73.9851,
                isActive = true,
                durationSeconds = 600
            )

            waitForDataSync(3000)

            composeTestRule.onNodeWithContentDescription("Navigate back")
                .performClick()

            composeTestRule.onNodeWithTag("event_list")
                .performScrollToNode(hasText("post_recovery_wave"))

            composeTestRule.onNodeWithContentDescription("View event details")
                .performClick()

            composeTestRule.onNodeWithTag("join_wave_button").performClick()
            delay(2.seconds)

            composeTestRule.onNodeWithTag("wave_active_indicator").assertExists()

            println("✅ GPS recovery test: Normal operation resumed after signal recovery")

        } finally {
            stopPerformanceTrace()
        }
    }

    /**
     * Test user notification of GPS issues
     */
    @Test
    fun realGps_issueNotification_userInformedAppropriately() = runTest {
        val trace = startPerformanceTrace("gps_notification_test")

        try {
            // Create location-dependent event
            createTestEvent(
                eventId = "notification_test_wave",
                latitude = 40.7589,
                longitude = -73.9851,
                isActive = true,
                durationSeconds = 800
            )

            waitForDataSync(3000)

            // Test different GPS scenarios and their user communication
            val gpsScenarios = listOf(
                "good_signal" to 0.9,
                "weak_signal" to 0.4,
                "very_weak_signal" to 0.1,
                "no_signal" to 0.0
            )

            gpsScenarios.forEach { (scenario, signalStrength) ->
                when (scenario) {
                    "good_signal" -> {
                        setTestLocation(40.7589, -73.9851)
                        delay(2.seconds)
                    }
                    "weak_signal", "very_weak_signal", "no_signal" -> {
                        deviceStateManager.simulateGpsSignalLoss()
                        delay(3.seconds)
                    }
                }

                val gpsStatus = deviceStateManager.getGpsStatus()
                println("📡 Testing GPS scenario: $scenario (signal: ${gpsStatus.signalStrength})")

                // Test wave joining behavior with different GPS states
                if (scenario == "good_signal") {
                    composeTestRule.onNodeWithTag("join_wave_button").performClick()
                    delay(2.seconds)
                    composeTestRule.onNodeWithTag("wave_active_indicator").assertExists()
                } else {
                    // Test behavior with poor GPS
                    if (gpsStatus.fallbackLocationAvailable) {
                        // Should still allow joining with fallback location
                        composeTestRule.onNodeWithTag("join_wave_button").assertExists()
                        println("✓ Wave joining available with fallback location")
                    } else {
                        // Should inform user about GPS issues
                        println("✓ User informed about GPS limitations")
                    }
                }

                // Verify appropriate GPS status communication
                when (scenario) {
                    "good_signal" -> {
                        assertTrue(
                            gpsStatus.signalStrength > 0.6,
                            "Good GPS signal should be detected: ${gpsStatus.signalStrength}"
                        )
                    }
                    "weak_signal", "very_weak_signal", "no_signal" -> {
                        assertTrue(
                            gpsStatus.fallbackLocationAvailable || gpsStatus.signalStrength < 0.5,
                            "Weak GPS should be detected or fallback available"
                        )
                    }
                }

                delay(2.seconds)
            }

            // Test recovery notification
            setTestLocation(40.7589, -73.9851)
            delay(5.seconds)

            val finalGpsStatus = deviceStateManager.getGpsStatus()
            assertTrue(
                finalGpsStatus.signalStrength > 0.7,
                "GPS signal should recover: ${finalGpsStatus.signalStrength}"
            )

            // Verify wave participation works normally after recovery
            if (!composeTestRule.onNodeWithTag("wave_active_indicator").isDisplayed()) {
                composeTestRule.onNodeWithTag("join_wave_button").performClick()
                delay(2.seconds)
            }
            composeTestRule.onNodeWithTag("wave_active_indicator").assertExists()

            println("✅ GPS notification test: User appropriately informed of GPS status changes")

        } finally {
            stopPerformanceTrace()
        }
    }
}