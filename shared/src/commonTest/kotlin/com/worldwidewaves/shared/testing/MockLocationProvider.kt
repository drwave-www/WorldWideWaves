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

import com.worldwidewaves.shared.events.utils.Position
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Mock location provider for testing location-dependent functionality.
 *
 * Simulates GPS location updates, movement patterns, and location-based scenarios
 * without requiring actual device location services.
 */
class MockLocationProvider {

    private var currentPosition: Position = TestHelpers.TestLocations.PARIS
    private var accuracy: Float = 10.0f // meters
    private var isLocationEnabled: Boolean = true
    private var hasLocationPermission: Boolean = true

    private val locationHistory = mutableListOf<LocationUpdate>()
    private val listeners = mutableListOf<LocationListener>()

    // Simulation state
    private var isSimulatingMovement = false
    private var movementPath: List<Position> = emptyList()
    private var currentPathIndex = 0

    /**
     * Gets the current mock location.
     */
    fun getCurrentLocation(): Position = currentPosition

    /**
     * Sets the current mock location.
     */
    fun setLocation(position: Position, accuracy: Float = 10.0f) {
        this.currentPosition = position
        this.accuracy = accuracy
        recordLocationUpdate(position, accuracy)
        notifyListeners(position, accuracy)
    }

    /**
     * Simulates movement to a new location over time.
     */
    fun moveTo(
        destination: Position,
        duration: Duration = 30.seconds,
        steps: Int = 10
    ) {
        val path = createMovementPath(currentPosition, destination, steps)
        simulateMovementAlongPath(path, duration)
    }

    /**
     * Simulates movement along a predefined path.
     */
    fun simulateMovementAlongPath(
        path: List<Position>,
        totalDuration: Duration = 60.seconds
    ) {
        movementPath = path
        currentPathIndex = 0
        isSimulatingMovement = true

        val stepDuration = totalDuration / path.size

        path.forEachIndexed { index, position ->
            currentPathIndex = index
            setLocation(position)
            // In a real implementation, this would use actual delays
            // For testing, we just immediately update positions
        }

        isSimulatingMovement = false
    }

    /**
     * Simulates random movement within a bounded area.
     */
    fun simulateRandomMovement(
        center: Position,
        radius: Double = 0.001, // degrees (~100m)
        steps: Int = 20
    ) {
        val randomPath = generateRandomPath(center, radius, steps)
        simulateMovementAlongPath(randomPath)
    }

    /**
     * Simulates GPS signal loss.
     */
    fun simulateSignalLoss() {
        isLocationEnabled = false
        notifyListeners(null, 0.0f, "GPS signal lost")
    }

    /**
     * Simulates GPS signal recovery.
     */
    fun simulateSignalRecovery() {
        isLocationEnabled = true
        notifyListeners(currentPosition, accuracy)
    }

    /**
     * Simulates location permission being denied.
     */
    fun simulatePermissionDenied() {
        hasLocationPermission = false
        notifyListeners(null, 0.0f, "Location permission denied")
    }

    /**
     * Simulates location permission being granted.
     */
    fun simulatePermissionGranted() {
        hasLocationPermission = true
        notifyListeners(currentPosition, accuracy)
    }

    /**
     * Simulates poor GPS accuracy.
     */
    fun simulatePoorAccuracy(accuracyMeters: Float = 100.0f) {
        this.accuracy = accuracyMeters
        notifyListeners(currentPosition, accuracy)
    }

    /**
     * Simulates high GPS accuracy.
     */
    fun simulateHighAccuracy(accuracyMeters: Float = 3.0f) {
        this.accuracy = accuracyMeters
        notifyListeners(currentPosition, accuracy)
    }

    /**
     * Teleports instantly to a new location (simulates user disabling location, traveling, then re-enabling).
     */
    fun teleportTo(position: Position) {
        setLocation(position)
    }

    /**
     * Checks if location services are enabled.
     */
    fun isLocationEnabled(): Boolean = isLocationEnabled

    /**
     * Checks if location permission is granted.
     */
    fun hasLocationPermission(): Boolean = hasLocationPermission

    /**
     * Gets the current location accuracy in meters.
     */
    fun getAccuracy(): Float = accuracy

    /**
     * Gets the location history for verification.
     */
    fun getLocationHistory(): List<LocationUpdate> = locationHistory.toList()

    /**
     * Clears the location history.
     */
    fun clearHistory() {
        locationHistory.clear()
    }

    /**
     * Adds a location listener.
     */
    fun addLocationListener(listener: LocationListener) {
        listeners.add(listener)
    }

    /**
     * Removes a location listener.
     */
    fun removeLocationListener(listener: LocationListener) {
        listeners.remove(listener)
    }

    /**
     * Removes all location listeners.
     */
    fun clearListeners() {
        listeners.clear()
    }

    /**
     * Resets the mock to default state.
     */
    fun reset() {
        currentPosition = TestHelpers.TestLocations.PARIS
        accuracy = 10.0f
        isLocationEnabled = true
        hasLocationPermission = true
        locationHistory.clear()
        listeners.clear()
        isSimulatingMovement = false
        movementPath = emptyList()
        currentPathIndex = 0
    }

    private fun createMovementPath(
        start: Position,
        end: Position,
        steps: Int
    ): List<Position> {
        val path = mutableListOf<Position>()
        val latStep = (end.latitude - start.latitude) / steps
        val lngStep = (end.longitude - start.longitude) / steps

        repeat(steps + 1) { i ->
            val lat = start.latitude + (latStep * i)
            val lng = start.longitude + (lngStep * i)
            path.add(Position(lat, lng))
        }

        return path
    }

    private fun generateRandomPath(
        center: Position,
        radius: Double,
        steps: Int
    ): List<Position> {
        val path = mutableListOf<Position>()
        var currentPos = currentPosition

        repeat(steps) {
            val angle = Math.random() * 2 * Math.PI
            val distance = Math.random() * radius

            val lat = center.latitude + distance * kotlin.math.cos(angle)
            val lng = center.longitude + distance * kotlin.math.sin(angle)

            currentPos = Position(lat, lng)
            path.add(currentPos)
        }

        return path
    }

    private fun recordLocationUpdate(position: Position, accuracy: Float) {
        locationHistory.add(
            LocationUpdate(
                position = position,
                accuracy = accuracy,
                timestamp = System.currentTimeMillis(),
                isEnabled = isLocationEnabled,
                hasPermission = hasLocationPermission
            )
        )
    }

    private fun notifyListeners(
        position: Position?,
        accuracy: Float,
        error: String? = null
    ) {
        listeners.forEach { listener ->
            if (position != null && error == null) {
                listener.onLocationUpdate(position, accuracy)
            } else {
                listener.onLocationError(error ?: "Unknown location error")
            }
        }
    }

    /**
     * Data class representing a location update event.
     */
    data class LocationUpdate(
        val position: Position,
        val accuracy: Float,
        val timestamp: Long,
        val isEnabled: Boolean,
        val hasPermission: Boolean
    )

    /**
     * Interface for listening to location updates.
     */
    interface LocationListener {
        fun onLocationUpdate(position: Position, accuracy: Float)
        fun onLocationError(error: String)
    }

    companion object {
        /**
         * Creates a mock location provider for city-based testing.
         */
        fun forCity(city: String): MockLocationProvider {
            val position = when (city.lowercase()) {
                "paris" -> TestHelpers.TestLocations.PARIS
                "london" -> TestHelpers.TestLocations.LONDON
                "newyork", "new_york" -> TestHelpers.TestLocations.NEW_YORK
                "tokyo" -> TestHelpers.TestLocations.TOKYO
                "sydney" -> TestHelpers.TestLocations.SYDNEY
                "saopaulo", "sao_paulo" -> TestHelpers.TestLocations.SAO_PAULO
                else -> TestHelpers.TestLocations.PARIS
            }

            return MockLocationProvider().apply {
                setLocation(position)
            }
        }

        /**
         * Creates a mock location provider that simulates no GPS signal.
         */
        fun withNoSignal(): MockLocationProvider {
            return MockLocationProvider().apply {
                simulateSignalLoss()
            }
        }

        /**
         * Creates a mock location provider that simulates no location permission.
         */
        fun withNoPermission(): MockLocationProvider {
            return MockLocationProvider().apply {
                simulatePermissionDenied()
            }
        }

        /**
         * Creates a mock location provider with poor GPS accuracy.
         */
        fun withPoorAccuracy(): MockLocationProvider {
            return MockLocationProvider().apply {
                simulatePoorAccuracy(50.0f)
            }
        }
    }
}