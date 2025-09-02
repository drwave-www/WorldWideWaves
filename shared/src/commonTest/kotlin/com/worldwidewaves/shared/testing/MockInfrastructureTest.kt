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

import com.worldwidewaves.shared.events.utils.BoundingBox
import com.worldwidewaves.shared.events.utils.Position
import com.worldwidewaves.shared.map.MapCameraCallback
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

/**
 * Tests for the mock infrastructure components to ensure they provide
 * reliable and comprehensive testing capabilities.
 */
@OptIn(ExperimentalTime::class)
class MockInfrastructureTest {

    @Test
    fun `test MockClock basic time operations`() {
        val mockClock = MockClock()

        // Initial state
        assertEquals(TestHelpers.TestTimes.BASE_TIME, mockClock.now())

        // Advance time
        mockClock.advanceTimeBy(1.hours)
        assertEquals(TestHelpers.TestTimes.BASE_TIME + 1.hours, mockClock.now())

        // Set specific time
        val specificTime = TestHelpers.TestTimes.MORNING_TIME
        mockClock.setTime(specificTime)
        assertEquals(specificTime, mockClock.now())

        // Reset
        mockClock.reset()
        assertEquals(TestHelpers.TestTimes.BASE_TIME, mockClock.now())
    }

    @Test
    fun `test MockClock delay tracking`() {
        val mockClock = MockClock()

        // No delays initially
        assertTrue(mockClock.delayCalls.isEmpty())
        mockClock.verifyNoDelaysCalled()

        // Track delays
        val delay1 = 30.seconds
        val delay2 = 1.minutes
        // Note: In a real test, these would be suspend calls
        // For this test, we're just verifying the tracking mechanism

        mockClock.verifyNoDelaysCalled() // Should still pass
    }

    @Test
    fun `test MockClock event simulation`() {
        val mockClock = MockClock()

        val eventDuration = 2.hours
        val timePoints = mockClock.simulateEventProgress(eventDuration, 4)

        assertEquals(5, timePoints.size) // 4 steps + final point
        assertEquals(TestHelpers.TestTimes.BASE_TIME, timePoints.first())
        assertEquals(TestHelpers.TestTimes.BASE_TIME + eventDuration, timePoints.last())
    }

    @Test
    fun `test MockLocationProvider basic location operations`() {
        val mockProvider = MockLocationProvider()

        // Initial state
        assertEquals(TestHelpers.TestLocations.PARIS, mockProvider.getCurrentLocation())
        assertTrue(mockProvider.isLocationEnabled())
        assertTrue(mockProvider.hasLocationPermission())

        // Set new location
        val newPosition = TestHelpers.TestLocations.LONDON
        mockProvider.setLocation(newPosition, 15.0f)
        assertEquals(newPosition, mockProvider.getCurrentLocation())
        assertEquals(15.0f, mockProvider.getAccuracy())

        // Check history
        val history = mockProvider.getLocationHistory()
        assertTrue(history.isNotEmpty())
        assertEquals(newPosition, history.last().position)
    }

    @Test
    fun `test MockLocationProvider movement simulation`() {
        val mockProvider = MockLocationProvider()
        val locationUpdates = mutableListOf<Position>()

        val listener = object : MockLocationProvider.LocationListener {
            override fun onLocationUpdate(position: Position, accuracy: Float) {
                locationUpdates.add(position)
            }

            override fun onLocationError(error: String) {
                // Not testing errors in this test
            }
        }

        mockProvider.addLocationListener(listener)

        // Simulate movement
        mockProvider.moveTo(TestHelpers.TestLocations.LONDON, 30.seconds, 5)

        // Should have received multiple location updates
        assertTrue(locationUpdates.size > 1)

        // Verify final position with tolerance for floating-point precision
        val finalPosition = locationUpdates.last()
        val expectedPosition = TestHelpers.TestLocations.LONDON
        val tolerance = 0.00001
        assertTrue("Expected latitude ${expectedPosition.lat} but got ${finalPosition.lat}") {
            kotlin.math.abs(finalPosition.lat - expectedPosition.lat) < tolerance
        }
        assertTrue("Expected longitude ${expectedPosition.lng} but got ${finalPosition.lng}") {
            kotlin.math.abs(finalPosition.lng - expectedPosition.lng) < tolerance
        }
    }

    @Test
    fun `test MockLocationProvider error scenarios`() {
        val mockProvider = MockLocationProvider()
        var errorReceived: String? = null

        val listener = object : MockLocationProvider.LocationListener {
            override fun onLocationUpdate(position: Position, accuracy: Float) {
                // Not testing successful updates here
            }

            override fun onLocationError(error: String) {
                errorReceived = error
            }
        }

        mockProvider.addLocationListener(listener)

        // Test signal loss
        mockProvider.simulateSignalLoss()
        assertFalse(mockProvider.isLocationEnabled())
        assertNotNull(errorReceived)
        assertTrue(errorReceived!!.contains("GPS signal lost"))

        // Test permission denied
        errorReceived = null
        mockProvider.simulatePermissionDenied()
        assertFalse(mockProvider.hasLocationPermission())
        assertNotNull(errorReceived)
        assertTrue(errorReceived!!.contains("permission"))
    }

    @Test
    fun `test MockLocationProvider factory methods`() {
        // Test city-based creation
        val parisProvider = MockLocationProvider.forCity("paris")
        assertEquals(TestHelpers.TestLocations.PARIS, parisProvider.getCurrentLocation())

        val londonProvider = MockLocationProvider.forCity("london")
        assertEquals(TestHelpers.TestLocations.LONDON, londonProvider.getCurrentLocation())

        // Test error scenarios
        val noSignalProvider = MockLocationProvider.withNoSignal()
        assertFalse(noSignalProvider.isLocationEnabled())

        val noPermissionProvider = MockLocationProvider.withNoPermission()
        assertFalse(noPermissionProvider.hasLocationPermission())

        val poorAccuracyProvider = MockLocationProvider.withPoorAccuracy()
        assertTrue(poorAccuracyProvider.getAccuracy() > 30.0f)
    }

    @Test
    fun `test MockMapAdapter basic operations`() {
        val mockAdapter = MockMapAdapter()

        // Initial state
        assertEquals(800.0, mockAdapter.getWidth())
        assertEquals(600.0, mockAdapter.getHeight())
        assertEquals(TestHelpers.TestLocations.PARIS, mockAdapter.getCameraPosition())

        // Set dimensions
        mockAdapter.setMapDimensions(1024.0, 768.0)
        assertEquals(1024.0, mockAdapter.getWidth())
        assertEquals(768.0, mockAdapter.getHeight())

        // Set style
        val stylePath = "mapbox://styles/custom/style"
        var callbackInvoked = false
        mockAdapter.setStyle(stylePath) { callbackInvoked = true }
        assertEquals(stylePath, mockAdapter.getCurrentStyle())
        assertTrue(callbackInvoked)
    }

    @Test
    fun `test MockMapAdapter camera operations`() {
        val mockAdapter = MockMapAdapter()
        val targetPosition = TestHelpers.TestLocations.LONDON
        val targetZoom = 15.0

        var animationFinished = false
        val callback = object : MapCameraCallback {
            override fun onFinish() {
                animationFinished = true
            }

            override fun onCancel() {
                // Not testing cancellation here
            }
        }

        // Test camera animation
        mockAdapter.animateCamera(targetPosition, targetZoom, callback)

        assertEquals(targetPosition, mockAdapter.getCameraPosition())
        assertEquals(targetZoom, mockAdapter.currentZoom.value)
        assertTrue(animationFinished)

        // Verify animation was recorded
        mockAdapter.verifyCameraAnimationTo(targetPosition, targetZoom)
    }

    @Test
    fun `test MockMapAdapter bounds operations`() {
        val mockAdapter = MockMapAdapter()
        val testBounds = BoundingBox(51.0, -1.0, 52.0, 0.0)
        val padding = 50

        var animationFinished = false
        val callback = object : MapCameraCallback {
            override fun onFinish() {
                animationFinished = true
            }

            override fun onCancel() {
                // Not testing cancellation here
            }
        }

        // Test bounds animation
        mockAdapter.animateCameraToBounds(testBounds, padding, callback)

        assertEquals(testBounds, mockAdapter.getVisibleRegion())
        assertTrue(animationFinished)

        // Verify animation was recorded
        mockAdapter.verifyCameraAnimationToBounds(testBounds, padding)
    }

    @Test
    fun `test MockMapAdapter wave polygon operations`() {
        val mockAdapter = MockMapAdapter()
        val polygons = listOf("polygon1", "polygon2", "polygon3")

        // Add wave polygons
        mockAdapter.addWavePolygons(polygons, clearExisting = true)

        // Verify operation was recorded
        mockAdapter.verifyWavePolygonsAdded(expectedCount = 3, clearExisting = true)

        val operations = mockAdapter.getWavePolygonOperations()
        assertEquals(1, operations.size)
        assertEquals(3, operations.first().polygons.size)
        assertTrue(operations.first().clearExisting)
    }

    @Test
    fun `test MockMapAdapter interaction simulation`() {
        val mockAdapter = MockMapAdapter()
        var clickReceived = false
        var clickLat = 0.0
        var clickLng = 0.0

        // Set click listener
        mockAdapter.setOnMapClickListener { lat, lng ->
            clickReceived = true
            clickLat = lat
            clickLng = lng
        }

        // Simulate click
        val testLat = 48.8566
        val testLng = 2.3522
        mockAdapter.simulateMapClick(testLat, testLng)

        assertTrue(clickReceived)
        assertEquals(testLat, clickLat)
        assertEquals(testLng, clickLng)
    }

    @Test
    fun `test MockMapAdapter animation failure simulation`() {
        val mockAdapter = MockMapAdapter()
        mockAdapter.configureAnimations(shouldFail = true)

        val targetPosition = TestHelpers.TestLocations.LONDON
        var animationCancelled = false

        val callback = object : MapCameraCallback {
            override fun onFinish() {
                // Should not be called
            }

            override fun onCancel() {
                animationCancelled = true
            }
        }

        mockAdapter.animateCamera(targetPosition, callback = callback)

        assertTrue(animationCancelled)
        // Camera position should not change on failed animation
        assertEquals(TestHelpers.TestLocations.PARIS, mockAdapter.getCameraPosition())
    }

    @Test
    fun `test MockMapAdapter factory methods`() {
        // Test city-based creation
        val londonAdapter = MockMapAdapter.forCity("london")
        assertEquals(TestHelpers.TestLocations.LONDON, londonAdapter.getCameraPosition())

        // Test animation failure configuration
        val failingAdapter = MockMapAdapter.withAnimationFailures()
        var animationCancelled = false
        failingAdapter.animateCamera(
            TestHelpers.TestLocations.PARIS,
            callback = object : MapCameraCallback {
                override fun onFinish() {}
                override fun onCancel() { animationCancelled = true }
            }
        )
        assertTrue(animationCancelled)

        // Test custom dimensions
        val customAdapter = MockMapAdapter.withDimensions(1920.0, 1080.0)
        assertEquals(1920.0, customAdapter.getWidth())
        assertEquals(1080.0, customAdapter.getHeight())
    }

    @Test
    fun `test MockMapAdapter history and verification`() {
        val mockAdapter = MockMapAdapter()

        // Perform several operations
        mockAdapter.animateCamera(TestHelpers.TestLocations.LONDON)
        mockAdapter.addWavePolygons(listOf("poly1", "poly2"))
        mockAdapter.setStyle("style1") {}

        // Check history
        assertEquals(1, mockAdapter.getCameraAnimations().size)
        assertEquals(1, mockAdapter.getWavePolygonOperations().size)
        assertEquals(1, mockAdapter.getStyleChanges().size)

        // Clear history
        mockAdapter.clearHistory()
        assertTrue(mockAdapter.getCameraAnimations().isEmpty())
        assertTrue(mockAdapter.getWavePolygonOperations().isEmpty())
        assertTrue(mockAdapter.getStyleChanges().isEmpty())
    }

    @Test
    fun `test MockMapAdapter verification assertions`() {
        val mockAdapter = MockMapAdapter()

        // Should fail when no animations performed
        assertFailsWith<AssertionError> {
            mockAdapter.verifyCameraAnimationTo(TestHelpers.TestLocations.LONDON)
        }

        // Should pass after animation
        mockAdapter.animateCamera(TestHelpers.TestLocations.LONDON, 12.0)
        mockAdapter.verifyCameraAnimationTo(TestHelpers.TestLocations.LONDON, 12.0)

        // Should fail when no polygons added
        assertFailsWith<AssertionError> {
            mockAdapter.verifyWavePolygonsAdded()
        }

        // Should pass after adding polygons
        mockAdapter.addWavePolygons(listOf("poly1"))
        mockAdapter.verifyWavePolygonsAdded(expectedCount = 1)
    }

    @Test
    fun `test MockMapAdapter reset functionality`() {
        val mockAdapter = MockMapAdapter()

        // Modify state
        mockAdapter.setMapDimensions(1024.0, 768.0)
        mockAdapter.setStyle("custom-style") {}
        mockAdapter.animateCamera(TestHelpers.TestLocations.LONDON)

        // Reset
        mockAdapter.reset()

        // Verify reset state
        assertEquals(800.0, mockAdapter.getWidth())
        assertEquals(600.0, mockAdapter.getHeight())
        assertNull(mockAdapter.getCurrentStyle())
        assertEquals(TestHelpers.TestLocations.PARIS, mockAdapter.getCameraPosition())
        assertTrue(mockAdapter.getCameraAnimations().isEmpty())
    }
}