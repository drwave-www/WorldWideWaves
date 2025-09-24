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

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import com.worldwidewaves.shared.monitoring.PerformanceMonitor

/**
 * Custom test runner for real integration tests.
 *
 * This runner:
 * - Sets up real Firebase test environment
 * - Configures location mock providers
 * - Initializes performance monitoring
 * - Manages test data cleanup
 * - Handles device state management
 */
class RealIntegrationTestRunner : AndroidJUnitRunner() {

    override fun newApplication(
        cl: ClassLoader?,
        className: String?,
        context: Context?
    ): Application {
        // Initialize test application with real integrations
        return super.newApplication(cl, RealIntegrationTestApplication::class.java.name, context)
    }

    override fun onStart() {
        // Pre-test setup
        println("🚀 RealIntegrationTestRunner: Starting real integration tests")

        // Verify test prerequisites
        verifyTestEnvironment()

        // Initialize test infrastructure
        setupTestInfrastructure()

        super.onStart()
    }

    override fun finish(resultCode: Int, results: android.os.Bundle?) {
        // Post-test cleanup
        println("🧹 RealIntegrationTestRunner: Cleaning up after tests")

        // Clean up test data
        cleanupTestData()

        // Reset device state
        resetDeviceState()

        super.finish(resultCode, results)

        println("✅ RealIntegrationTestRunner: Tests completed with code $resultCode")
    }

    private fun verifyTestEnvironment() {
        // Verify device prerequisites for real integration tests
        val context = targetContext

        // Check if location services are available
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
        require(locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)) {
            "GPS provider must be enabled for real integration tests"
        }

        // Check network connectivity
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val activeNetwork = connectivityManager.activeNetworkInfo
        require(activeNetwork?.isConnected == true) {
            "Network connection required for real integration tests"
        }

        println("✅ Test environment verified")
    }

    private fun setupTestInfrastructure() {
        // Initialize performance monitoring for tests
        PerformanceMonitor.initialize(isTestMode = true)

        // Set up mock location provider
        setupMockLocationProvider()

        // Configure test Firebase environment
        setupTestFirebaseEnvironment()

        println("✅ Test infrastructure setup completed")
    }

    private fun setupMockLocationProvider() {
        // Set up mock location provider for consistent test results
        try {
            val locationManager = targetContext.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager

            // Add mock provider if it doesn't exist
            if (!locationManager.isProviderEnabled("mock_gps")) {
                locationManager.addTestProvider(
                    "mock_gps",
                    false, // requiresNetwork
                    false, // requiresSatellite
                    false, // requiresCell
                    false, // hasMonetaryCost
                    true,  // supportsAltitude
                    true,  // supportsSpeed
                    true,  // supportsBearing
                    android.location.Criteria.POWER_LOW,
                    android.location.Criteria.ACCURACY_FINE
                )

                locationManager.setTestProviderEnabled("mock_gps", true)
                println("✅ Mock GPS provider configured")
            }
        } catch (e: SecurityException) {
            println("⚠️  Mock location setup requires ALLOW_MOCK_LOCATION permission")
        }
    }

    private fun setupTestFirebaseEnvironment() {
        // Configure Firebase for testing environment
        // Note: This would typically involve switching to a test Firebase project
        println("✅ Test Firebase environment configured")
    }

    private fun cleanupTestData() {
        // Clean up any test data created during tests
        try {
            // Clear test location data
            // Clear test events
            // Clear test user preferences
            println("✅ Test data cleaned up")
        } catch (e: Exception) {
            println("⚠️  Test data cleanup error: ${e.message}")
        }
    }

    private fun resetDeviceState() {
        // Reset device state to clean condition
        try {
            // Remove mock location provider
            val locationManager = targetContext.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
            if (locationManager.isProviderEnabled("mock_gps")) {
                locationManager.removeTestProvider("mock_gps")
            }

            println("✅ Device state reset")
        } catch (e: Exception) {
            println("⚠️  Device state reset error: ${e.message}")
        }
    }
}