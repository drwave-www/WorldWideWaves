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

import android.content.Context
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.worldwidewaves.activities.MainActivity
import com.worldwidewaves.shared.monitoring.PerformanceMonitor
import com.worldwidewaves.shared.monitoring.PerformanceTrace
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith

/**
 * Base class for real integration tests that use actual device capabilities.
 *
 * This base class provides:
 * - Real Firebase integration
 * - Actual GPS location services
 * - Network connectivity management
 * - Performance monitoring
 * - Device state management
 * - Test data cleanup
 *
 * Tests extending this class will run against real services and require:
 * - Connected Android device with location services enabled
 * - Internet connectivity
 * - Test Firebase project configuration
 */
@RunWith(AndroidJUnit4::class)
abstract class BaseRealIntegrationTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    protected lateinit var context: Context
    protected lateinit var performanceMonitor: PerformanceMonitor
    protected lateinit var deviceStateManager: RealDeviceStateManager
    protected lateinit var testDataManager: TestDataManager

    private var currentTrace: PerformanceTrace? = null

    @Before
    open fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        performanceMonitor = PerformanceMonitor()
        deviceStateManager = RealDeviceStateManager(context)
        testDataManager = TestDataManager()

        // Verify test prerequisites
        verifyDeviceState()

        // Initialize test environment
        setupTestEnvironment()

        println("✅ BaseRealIntegrationTest: Setup completed for ${this.javaClass.simpleName}")
    }

    @After
    open fun tearDown() {
        // Stop any running performance trace
        currentTrace?.stop()

        // Clean up test data
        testDataManager.cleanup()

        // Reset device state
        deviceStateManager.reset()

        println("🧹 BaseRealIntegrationTest: Teardown completed for ${this.javaClass.simpleName}")
    }

    /**
     * Start performance monitoring for a test method
     */
    protected fun startPerformanceTrace(testName: String): PerformanceTrace {
        currentTrace = performanceMonitor.startTrace("real_integration_$testName")
        return currentTrace!!
    }

    /**
     * Stop the current performance trace and return metrics
     */
    protected fun stopPerformanceTrace(): Long {
        val trace = currentTrace ?: return 0L
        trace.stop()
        currentTrace = null
        return trace.getDurationMs()
    }

    /**
     * Wait for network connectivity to be available
     */
    protected suspend fun waitForNetworkConnectivity(timeoutMs: Long = 10000) {
        deviceStateManager.waitForNetworkConnectivity(timeoutMs)
    }

    /**
     * Wait for GPS location to be available
     */
    protected suspend fun waitForGpsLocation(timeoutMs: Long = 15000) {
        deviceStateManager.waitForGpsLocation(timeoutMs)
    }

    /**
     * Set test location for consistent testing
     */
    protected fun setTestLocation(latitude: Double, longitude: Double) {
        deviceStateManager.setTestLocation(latitude, longitude)
    }

    /**
     * Simulate network conditions
     */
    protected fun simulateNetworkConditions(condition: NetworkCondition) {
        deviceStateManager.simulateNetworkCondition(condition)
    }

    /**
     * Create test event data for integration tests
     */
    protected fun createTestEvent(eventId: String, latitude: Double, longitude: Double) {
        testDataManager.createTestEvent(eventId, latitude, longitude)
    }

    /**
     * Wait for real-time data synchronization
     */
    protected suspend fun waitForDataSync(timeoutMs: Long = 5000) {
        testDataManager.waitForDataSync(timeoutMs)
    }

    private fun verifyDeviceState() {
        // Verify GPS is enabled
        require(deviceStateManager.isGpsEnabled()) {
            "GPS must be enabled for real integration tests"
        }

        // Verify network connectivity
        require(deviceStateManager.isNetworkAvailable()) {
            "Network connectivity required for real integration tests"
        }

        // Verify location permissions
        require(deviceStateManager.hasLocationPermissions()) {
            "Location permissions required for real integration tests"
        }

        println("✅ Device state verified for real integration tests")
    }

    private fun setupTestEnvironment() {
        // Configure Firebase for test environment
        testDataManager.initializeTestFirebase()

        // Set up test location provider
        deviceStateManager.setupTestLocationProvider()

        // Initialize performance monitoring
        startPerformanceTrace("test_setup")

        println("✅ Test environment setup completed")
    }

    enum class NetworkCondition {
        OFFLINE,
        SLOW_NETWORK,
        FAST_NETWORK,
        INTERMITTENT
    }
}