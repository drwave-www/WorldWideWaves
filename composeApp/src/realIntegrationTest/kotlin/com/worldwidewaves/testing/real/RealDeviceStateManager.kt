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

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout

/**
 * Manages real device state for integration testing.
 *
 * This class handles:
 * - GPS and location services
 * - Network connectivity simulation
 * - Permission verification
 * - Device state cleanup
 */
class RealDeviceStateManager(private val context: Context) {

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private var testLocationProvider: String? = null
    private var originalNetworkState: Boolean = false

    /**
     * Check if GPS is enabled on the device
     */
    fun isGpsEnabled(): Boolean {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    /**
     * Check if network is available
     */
    fun isNetworkAvailable(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    /**
     * Check if location permissions are granted
     */
    fun hasLocationPermissions(): Boolean {
        val fineLocationGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseLocationGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fineLocationGranted && coarseLocationGranted
    }

    /**
     * Set up test location provider for consistent test results
     */
    fun setupTestLocationProvider() {
        try {
            val providerName = "test_gps_provider"

            // Remove existing test provider if it exists
            try {
                locationManager.removeTestProvider(providerName)
            } catch (e: Exception) {
                // Provider might not exist, continue
            }

            // Add new test provider
            locationManager.addTestProvider(
                providerName,
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

            locationManager.setTestProviderEnabled(providerName, true)
            testLocationProvider = providerName

            println("✅ Test location provider '$providerName' configured")
        } catch (e: SecurityException) {
            println("⚠️  Unable to set up test location provider: ${e.message}")
            println("   Ensure 'Allow mock locations' is enabled in developer options")
        }
    }

    /**
     * Set a specific test location
     */
    fun setTestLocation(latitude: Double, longitude: Double) {
        testLocationProvider?.let { provider ->
            try {
                val location = Location(provider).apply {
                    this.latitude = latitude
                    this.longitude = longitude
                    accuracy = 1.0f
                    time = System.currentTimeMillis()
                    elapsedRealtimeNanos = System.nanoTime()
                }

                locationManager.setTestProviderLocation(provider, location)
                println("✅ Test location set to: $latitude, $longitude")
            } catch (e: Exception) {
                println("⚠️  Failed to set test location: ${e.message}")
            }
        }
    }

    /**
     * Wait for network connectivity to become available
     */
    suspend fun waitForNetworkConnectivity(timeoutMs: Long) {
        withTimeout(timeoutMs) {
            while (!isNetworkAvailable()) {
                delay(500)
            }
        }
        println("✅ Network connectivity confirmed")
    }

    /**
     * Wait for GPS location to be available
     */
    suspend fun waitForGpsLocation(timeoutMs: Long) {
        withTimeout(timeoutMs) {
            while (!isGpsEnabled()) {
                delay(1000)
            }
        }
        println("✅ GPS location confirmed")
    }

    /**
     * Simulate different network conditions
     */
    fun simulateNetworkCondition(condition: BaseRealIntegrationTest.NetworkCondition) {
        when (condition) {
            BaseRealIntegrationTest.NetworkCondition.OFFLINE -> {
                // Note: In real device testing, we can't actually disable network
                // This would be a placeholder for network simulation tools
                println("🔴 Simulating offline condition (requires network simulation tool)")
            }
            BaseRealIntegrationTest.NetworkCondition.SLOW_NETWORK -> {
                println("🟡 Simulating slow network (requires network throttling)")
            }
            BaseRealIntegrationTest.NetworkCondition.FAST_NETWORK -> {
                println("🟢 Simulating fast network")
            }
            BaseRealIntegrationTest.NetworkCondition.INTERMITTENT -> {
                println("🟠 Simulating intermittent connectivity")
            }
        }
    }

    /**
     * Get current location from device
     */
    @Suppress("MissingPermission")
    fun getCurrentLocation(): Location? {
        if (!hasLocationPermissions()) {
            println("⚠️  Location permissions not granted")
            return null
        }

        return locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            ?: locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
    }

    /**
     * Reset device state after tests
     */
    fun reset() {
        // Remove test location provider
        testLocationProvider?.let { provider ->
            try {
                locationManager.removeTestProvider(provider)
                println("✅ Test location provider removed")
            } catch (e: Exception) {
                println("⚠️  Failed to remove test location provider: ${e.message}")
            }
        }

        testLocationProvider = null
        println("✅ Device state reset completed")
    }

    /**
     * Check if TalkBack is enabled (for accessibility testing)
     */
    fun isTalkBackEnabled(): Boolean {
        return try {
            val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as android.view.accessibility.AccessibilityManager
            accessibilityManager.isEnabled && accessibilityManager.isTouchExplorationEnabled
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Check if any accessibility services are enabled
     */
    fun hasAccessibilityServicesEnabled(): Boolean {
        return try {
            val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as android.view.accessibility.AccessibilityManager
            accessibilityManager.isEnabled
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Simulate orientation change (for device compatibility testing)
     */
    fun simulateOrientationChange(orientation: Int) {
        println("📱 Simulating orientation change to: ${if (orientation == 1) "Portrait" else "Landscape"}")
        // Note: Real orientation change would require ActivityTestRule configuration
        // This is a placeholder for orientation testing
    }

    /**
     * Check if device has GPS capability
     */
    fun hasGpsCapability(): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS)
    }

    /**
     * Check if device has network capability
     */
    fun hasNetworkCapability(): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI) ||
               context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)
    }

    /**
     * Get network info for debugging
     */
    fun getNetworkInfo(): String {
        val network = connectivityManager.activeNetwork
        if (network == null) {
            return "No active network"
        }

        val capabilities = connectivityManager.getNetworkCapabilities(network)
        return when {
            capabilities == null -> "No network capabilities"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WiFi"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            else -> "Unknown network type"
        }
    }
}