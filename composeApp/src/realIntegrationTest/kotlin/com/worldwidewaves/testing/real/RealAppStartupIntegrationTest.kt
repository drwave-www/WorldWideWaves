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
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Duration.Companion.seconds

/**
 * Real integration tests for complete app startup flow.
 *
 * These tests run against real services and verify the entire startup chain:
 * - Cold app launch performance
 * - Splash screen behavior and timing
 * - Real Firebase data loading and caching
 * - Navigation from splash to main content
 * - Error handling during startup
 * - Network connectivity during startup
 */
@RunWith(AndroidJUnit4::class)
class RealAppStartupIntegrationTest : BaseRealIntegrationTest() {

    @Test
    fun realStartup_coldLaunch_completesWithinPerformanceThresholds() = runTest {
        val trace = startPerformanceTrace("cold_launch_real")

        // Ensure clean state
        deviceStateManager.reset()

        // Wait for network connectivity
        waitForNetworkConnectivity(timeoutMs = 10.seconds.inWholeMilliseconds)

        // Launch app and measure performance
        composeTestRule.activityRule.launchActivity(null)

        // Wait for splash screen to appear
        composeTestRule.waitUntil(timeoutMillis = 5.seconds.inWholeMilliseconds) {
            try {
                composeTestRule.onNodeWithContentDescription("Splash screen").assertExists()
                true
            } catch (e: AssertionError) {
                false
            }
        }

        // Verify splash screen is displayed
        composeTestRule.onNodeWithContentDescription("Splash screen").assertIsDisplayed()

        // Wait for main content to load
        composeTestRule.waitUntil(timeoutMillis = 30.seconds.inWholeMilliseconds) {
            try {
                composeTestRule.onNodeWithText("WorldWideWaves").assertExists()
                true
            } catch (e: AssertionError) {
                false
            }
        }

        val launchTime = stopPerformanceTrace()

        // Verify performance requirements
        assertTrue("Cold launch should complete within 10 seconds", launchTime < 10000)
        assertTrue("Cold launch should be faster than 5 seconds for good UX", launchTime < 5000)

        // Verify main UI is accessible
        composeTestRule.onNodeWithText("WorldWideWaves").assertIsDisplayed()

        println("✅ Cold launch completed in ${launchTime}ms")
    }

    @Test
    fun realStartup_firebaseDataLoading_loadsEventsSuccessfully() = runTest {
        val trace = startPerformanceTrace("firebase_data_loading_real")

        // Wait for network connectivity
        waitForNetworkConnectivity()

        // Create test events in Firebase
        createTestEvent("startup_test_event_1", 40.7128, -74.0060) // New York
        createTestEvent("startup_test_event_2", 48.8566, 2.3522)   // Paris

        // Wait for Firebase sync
        waitForDataSync(timeoutMs = 10.seconds.inWholeMilliseconds)

        // Launch app
        composeTestRule.activityRule.launchActivity(null)

        // Wait for app to load and data to sync
        composeTestRule.waitForIdle()
        waitForDataSync()

        // Verify events are loaded and displayed
        composeTestRule.waitUntil(timeoutMillis = 15.seconds.inWholeMilliseconds) {
            try {
                // Look for event indicators or event list
                composeTestRule.onNode(hasText("startup_test_event_1") or hasContentDescription("Events loaded")).assertExists()
                true
            } catch (e: AssertionError) {
                false
            }
        }

        val dataLoadTime = stopPerformanceTrace()

        // Verify performance requirements
        assertTrue("Firebase data loading should complete within 15 seconds", dataLoadTime < 15000)

        println("✅ Firebase data loading completed in ${dataLoadTime}ms")
    }

    @Test
    fun realStartup_noNetworkConnection_showsOfflineState() = runTest {
        val trace = startPerformanceTrace("offline_startup_real")

        // Simulate offline condition (note: on real devices, this requires network tools)
        simulateNetworkConditions(NetworkCondition.OFFLINE)

        // Launch app
        composeTestRule.activityRule.launchActivity(null)

        // Wait for offline state to be detected
        composeTestRule.waitUntil(timeoutMillis = 10.seconds.inWholeMilliseconds) {
            try {
                composeTestRule.onNode(
                    hasContentDescription("Offline mode") or
                    hasText("No connection") or
                    hasText("Offline")
                ).assertExists()
                true
            } catch (e: AssertionError) {
                false
            }
        }

        // Verify offline indicators are shown
        val offlineNodeExists = try {
            composeTestRule.onNode(
                hasContentDescription("Offline mode") or
                hasText("No connection") or
                hasText("Offline")
            ).assertExists()
            true
        } catch (e: AssertionError) {
            false
        }

        assertTrue("App should show offline indicators when no network is available", offlineNodeExists)

        val offlineTime = stopPerformanceTrace()
        println("✅ Offline startup handling completed in ${offlineTime}ms")
    }

    @Test
    fun realStartup_slowNetwork_handlesGracefully() = runTest {
        val trace = startPerformanceTrace("slow_network_startup_real")

        // Simulate slow network conditions
        simulateNetworkConditions(NetworkCondition.SLOW_NETWORK)

        // Launch app
        composeTestRule.activityRule.launchActivity(null)

        // Wait for loading indicators
        composeTestRule.waitUntil(timeoutMillis = 5.seconds.inWholeMilliseconds) {
            try {
                composeTestRule.onNode(
                    hasContentDescription("Loading") or
                    hasText("Loading events...") or
                    hasTestTag("loading-indicator")
                ).assertExists()
                true
            } catch (e: AssertionError) {
                false
            }
        }

        // Verify loading indicators are shown during slow network
        val loadingIndicatorExists = try {
            composeTestRule.onNode(
                hasContentDescription("Loading") or
                hasText("Loading events...") or
                hasTestTag("loading-indicator")
            ).assertExists()
            true
        } catch (e: AssertionError) {
            false
        }

        assertTrue("App should show loading indicators during slow network", loadingIndicatorExists)

        // Wait for eventual completion (with longer timeout for slow network)
        composeTestRule.waitUntil(timeoutMillis = 45.seconds.inWholeMilliseconds) {
            try {
                composeTestRule.onNodeWithText("WorldWideWaves").assertExists()
                true
            } catch (e: AssertionError) {
                false
            }
        }

        val slowNetworkTime = stopPerformanceTrace()
        assertTrue("Slow network startup should complete within 45 seconds", slowNetworkTime < 45000)

        println("✅ Slow network startup completed in ${slowNetworkTime}ms")
    }

    @Test
    fun realStartup_appResume_maintainsState() = runTest {
        val trace = startPerformanceTrace("app_resume_real")

        // Launch app initially
        composeTestRule.activityRule.launchActivity(null)

        // Wait for app to fully load
        composeTestRule.waitUntil(timeoutMillis = 15.seconds.inWholeMilliseconds) {
            try {
                composeTestRule.onNodeWithText("WorldWideWaves").assertExists()
                true
            } catch (e: AssertionError) {
                false
            }
        }

        // Navigate to background (simulate user pressing home)
        composeTestRule.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.STARTED)

        // Wait a moment
        delay(2000)

        // Resume app
        composeTestRule.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)

        // Verify app state is maintained
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("WorldWideWaves").assertIsDisplayed()

        val resumeTime = stopPerformanceTrace()
        assertTrue("App resume should be fast", resumeTime < 2000)

        println("✅ App resume completed in ${resumeTime}ms")
    }

    @Test
    fun realStartup_memoryPressure_handlesGracefully() = runTest {
        val trace = startPerformanceTrace("memory_pressure_startup_real")

        // Create multiple test events to increase memory usage
        repeat(100) { index ->
            createTestEvent("memory_test_event_$index", 40.7128 + (index * 0.001), -74.0060 + (index * 0.001))
        }

        // Wait for data sync
        waitForDataSync(timeoutMs = 30.seconds.inWholeMilliseconds)

        // Launch app with large dataset
        composeTestRule.activityRule.launchActivity(null)

        // Monitor memory usage during startup
        val runtime = Runtime.getRuntime()
        val initialMemory = runtime.totalMemory() - runtime.freeMemory()

        // Wait for app to load
        composeTestRule.waitUntil(timeoutMillis = 30.seconds.inWholeMilliseconds) {
            try {
                composeTestRule.onNodeWithText("WorldWideWaves").assertExists()
                true
            } catch (e: AssertionError) {
                false
            }
        }

        val finalMemory = runtime.totalMemory() - runtime.freeMemory()
        val memoryIncrease = finalMemory - initialMemory
        val memoryIncreasePercent = (memoryIncrease.toDouble() / initialMemory) * 100

        val memoryPressureTime = stopPerformanceTrace()

        // Verify memory usage is reasonable
        assertTrue("Memory increase should be reasonable", memoryIncreasePercent < 200.0)
        assertTrue("Startup under memory pressure should complete within 30 seconds", memoryPressureTime < 30000)

        println("✅ Memory pressure startup completed in ${memoryPressureTime}ms")
        println("   Memory usage increased by ${String.format("%.1f", memoryIncreasePercent)}%")
    }

    @Test
    fun realStartup_deviceRotation_maintainsStartupFlow() = runTest {
        val trace = startPerformanceTrace("rotation_startup_real")

        // Launch app
        composeTestRule.activityRule.launchActivity(null)

        // Wait for initial load
        composeTestRule.waitUntil(timeoutMillis = 10.seconds.inWholeMilliseconds) {
            try {
                composeTestRule.onNodeWithContentDescription("Splash screen").assertExists()
                true
            } catch (e: AssertionError) {
                false
            }
        }

        // Simulate device rotation during startup
        composeTestRule.activityRule.scenario.onActivity { activity ->
            activity.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }

        // Wait for orientation change to complete
        delay(2000)

        // Verify app continues to load properly
        composeTestRule.waitUntil(timeoutMillis = 20.seconds.inWholeMilliseconds) {
            try {
                composeTestRule.onNodeWithText("WorldWideWaves").assertExists()
                true
            } catch (e: AssertionError) {
                false
            }
        }

        // Rotate back to portrait
        composeTestRule.activityRule.scenario.onActivity { activity ->
            activity.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        delay(1000)

        // Verify app is still functional
        composeTestRule.onNodeWithText("WorldWideWaves").assertIsDisplayed()

        val rotationTime = stopPerformanceTrace()
        println("✅ Rotation during startup completed in ${rotationTime}ms")
    }
}