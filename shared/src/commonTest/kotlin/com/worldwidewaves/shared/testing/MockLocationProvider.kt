package com.worldwidewaves.shared.testing

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

import com.worldwidewaves.shared.utils.GeoPosition
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.time.Duration

/**
 * A mock location provider for testing location-dependent functionality.
 *
 * This mock allows tests to:
 * - Set specific user positions for predictable testing
 * - Simulate user movement patterns
 * - Test location permission scenarios
 * - Record location requests for verification
 */
class MockLocationProvider {
    private val _currentPosition = MutableStateFlow<GeoPosition?>(null)
    val currentPosition: StateFlow<GeoPosition?> = _currentPosition.asStateFlow()

    private val _isLocationEnabled = MutableStateFlow(true)
    val isLocationEnabled: StateFlow<Boolean> = _isLocationEnabled.asStateFlow()

    private val _hasLocationPermission = MutableStateFlow(true)
    val hasLocationPermission: StateFlow<Boolean> = _hasLocationPermission.asStateFlow()

    private val _locationRequests = mutableListOf<String>()
    val locationRequests: List<String> get() = _locationRequests.toList()

    private var _isTracking = false
    val isTracking: Boolean get() = _isTracking

    /**
     * Set the current user position.
     */
    fun setPosition(position: GeoPosition?) {
        _currentPosition.value = position
    }

    /**
     * Set location service availability.
     */
    fun setLocationEnabled(enabled: Boolean) {
        _isLocationEnabled.value = enabled
        if (!enabled) {
            _currentPosition.value = null
        }
    }

    /**
     * Set location permission status.
     */
    fun setLocationPermission(granted: Boolean) {
        _hasLocationPermission.value = granted
        if (!granted) {
            _currentPosition.value = null
        }
    }

    /**
     * Simulate requesting location.
     */
    fun requestLocation(reason: String = "test"): GeoPosition? {
        _locationRequests.add(reason)
        return if (_hasLocationPermission.value && _isLocationEnabled.value) {
            _currentPosition.value
        } else {
            null
        }
    }

    /**
     * Start location tracking.
     */
    fun startTracking() {
        _isTracking = true
        _locationRequests.add("start_tracking")
    }

    /**
     * Stop location tracking.
     */
    fun stopTracking() {
        _isTracking = false
        _locationRequests.add("stop_tracking")
    }

    /**
     * Simulate user movement along a path.
     */
    fun simulateMovement(
        path: List<GeoPosition>,
        stepDelay: Duration = Duration.ZERO
    ) {
        path.forEach { position ->
            setPosition(position)
            // In real implementation, this would include actual delays
            // For testing, we just update positions immediately
        }
    }

    /**
     * Simulate user walking towards a destination.
     */
    fun simulateWalkingTowards(
        destination: GeoPosition,
        steps: Int = 10,
        startPosition: GeoPosition = TestHelpers.TestLocations.PARIS
    ) {
        val path = generatePathBetween(startPosition, destination, steps)
        simulateMovement(path)
    }

    /**
     * Generate a path between two positions with the specified number of steps.
     */
    private fun generatePathBetween(
        start: GeoPosition,
        end: GeoPosition,
        steps: Int
    ): List<GeoPosition> {
        val path = mutableListOf<GeoPosition>()

        for (i in 0..steps) {
            val progress = i.toDouble() / steps
            val lat = start.latitude + (end.latitude - start.latitude) * progress
            val lng = start.longitude + (end.longitude - start.longitude) * progress
            path.add(GeoPosition(lat, lng))
        }

        return path
    }

    /**
     * Simulate GPS accuracy issues.
     */
    fun simulateGpsNoise(
        basePosition: GeoPosition,
        noiseRadius: Double = 0.001 // Degrees
    ) {
        val noisyLat = basePosition.latitude + (Math.random() - 0.5) * noiseRadius
        val noisyLng = basePosition.longitude + (Math.random() - 0.5) * noiseRadius
        setPosition(GeoPosition(noisyLat, noisyLng))
    }

    /**
     * Reset all state and recorded interactions.
     */
    fun reset() {
        _currentPosition.value = null
        _isLocationEnabled.value = true
        _hasLocationPermission.value = true
        _locationRequests.clear()
        _isTracking = false
    }

    /**
     * Verify that location was requested with specific reason.
     */
    fun verifyLocationRequested(reason: String) {
        if (reason !in _locationRequests) {
            throw AssertionError("Expected location request with reason '$reason', but got: $_locationRequests")
        }
    }

    /**
     * Verify that tracking was started.
     */
    fun verifyTrackingStarted() {
        if (!_locationRequests.contains("start_tracking")) {
            throw AssertionError("Expected tracking to be started")
        }
    }

    /**
     * Verify that tracking was stopped.
     */
    fun verifyTrackingStopped() {
        if (!_locationRequests.contains("stop_tracking")) {
            throw AssertionError("Expected tracking to be stopped")
        }
    }

    companion object {
        /**
         * Create a mock location provider with user in Paris.
         */
        fun atParis(): MockLocationProvider {
            return MockLocationProvider().apply {
                setPosition(TestHelpers.TestLocations.PARIS)
            }
        }

        /**
         * Create a mock location provider with no location permission.
         */
        fun withoutPermission(): MockLocationProvider {
            return MockLocationProvider().apply {
                setLocationPermission(false)
            }
        }

        /**
         * Create a mock location provider with location services disabled.
         */
        fun withLocationDisabled(): MockLocationProvider {
            return MockLocationProvider().apply {
                setLocationEnabled(false)
            }
        }

        /**
         * Create a mock location provider with no position set.
         */
        fun withoutPosition(): MockLocationProvider {
            return MockLocationProvider()
        }
    }
}