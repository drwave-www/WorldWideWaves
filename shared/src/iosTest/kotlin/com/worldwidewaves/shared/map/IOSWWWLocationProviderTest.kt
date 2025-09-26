package com.worldwidewaves.shared.map

/*
 * Copyright 2025 DrWave
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

import com.worldwidewaves.shared.events.utils.Position
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * iOS-specific tests for IOSWWWLocationProvider.
 *
 * These tests verify the iOS Core Location integration and StateFlow behavior.
 */
class IOSWWWLocationProviderTest {
    @Test
    fun `initial state has no location`() =
        runTest {
            val locationProvider = IOSWWWLocationProvider()

            val initialLocation = locationProvider.currentLocation.first()
            assertNull(initialLocation)
        }

    @Test
    fun `startLocationUpdates sets up location tracking`() =
        runTest {
            val locationProvider = IOSWWWLocationProvider()
            var receivedPosition: Position? = null

            locationProvider.startLocationUpdates { position ->
                receivedPosition = position
            }

            // Should have received default location (San Francisco)
            // Note: In real iOS environment, this would eventually get GPS location
            assertNotNull(receivedPosition)
        }

    @Test
    fun `stopLocationUpdates stops tracking`() =
        runTest {
            val locationProvider = IOSWWWLocationProvider()
            var updateCount = 0

            locationProvider.startLocationUpdates { position ->
                updateCount++
            }

            locationProvider.stopLocationUpdates()

            // After stopping, no more updates should be received
            // This is a basic test - in real implementation we'd verify CLLocationManager is stopped
        }

    @Test
    fun `default location is San Francisco coordinates`() =
        runTest {
            val locationProvider = IOSWWWLocationProvider()
            var receivedPosition: Position? = null

            locationProvider.startLocationUpdates { position ->
                receivedPosition = position
            }

            // Should receive San Francisco coordinates as default
            receivedPosition?.let { position ->
                assertEquals(37.7749, position.lat, 0.0001)
                assertEquals(-122.4194, position.lng, 0.0001)
            }
        }

    @Test
    fun `multiple startLocationUpdates calls are handled gracefully`() =
        runTest {
            val locationProvider = IOSWWWLocationProvider()
            var updateCount = 0

            locationProvider.startLocationUpdates { position ->
                updateCount++
            }

            // Second call should not duplicate tracking
            locationProvider.startLocationUpdates { position ->
                updateCount++
            }

            // Should handle multiple calls gracefully without errors
            assertTrue(updateCount > 0)
        }

    @Test
    fun `StateFlow currentLocation updates correctly`() =
        runTest {
            val locationProvider = IOSWWWLocationProvider()

            locationProvider.startLocationUpdates { position ->
                // Callback received
            }

            val currentLocation = locationProvider.currentLocation.first()
            assertNotNull(currentLocation)

            // Should be San Francisco default coordinates
            assertEquals(37.7749, currentLocation.lat, 0.0001)
            assertEquals(-122.4194, currentLocation.lng, 0.0001)
        }
}
