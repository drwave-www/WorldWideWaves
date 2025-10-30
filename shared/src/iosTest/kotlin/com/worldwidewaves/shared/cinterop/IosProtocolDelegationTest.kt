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

package com.worldwidewaves.shared.cinterop

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.Foundation.NSError
import platform.darwin.NSObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for iOS Objective-C protocol delegation via Kotlin/Native.
 *
 * This test suite validates the critical pattern of Kotlin classes implementing
 * Objective-C protocols (specifically CLLocationManagerDelegateProtocol), which is
 * used extensively throughout WorldWideWaves iOS implementation.
 *
 * The pattern tested here is fundamental to iOS interop:
 * - Kotlin class extends NSObject() (required for ObjC protocols)
 * - Implements CLLocationManagerDelegateProtocol
 * - Provides callback methods that iOS Core Location can invoke
 * - Maintains memory safety and proper lifecycle
 *
 * This pattern is used in production in IosLocationProvider.kt (lines 205-266).
 */
@OptIn(ExperimentalForeignApi::class)
class IosProtocolDelegationTest {
    @Test
    fun `delegate should properly extend NSObject`() {
        // Validates the foundational requirement: delegates MUST extend NSObject
        // to be compatible with Objective-C protocols
        var callbackInvoked = false
        val delegate = TestLocationDelegate { callbackInvoked = true }

        // Verify delegate is an NSObject instance
        assertTrue(delegate is NSObject, "Delegate must extend NSObject for ObjC protocol compatibility")
    }

    @Test
    fun `delegate should implement CLLocationManagerDelegateProtocol`() {
        // Validates that Kotlin class correctly implements ObjC protocol interface
        // This is the core of Kotlin/Native <-> Objective-C interop
        val delegate = TestLocationDelegate { }

        // Verify delegate implements the protocol
        assertTrue(
            delegate is CLLocationManagerDelegateProtocol,
            "Delegate must implement CLLocationManagerDelegateProtocol",
        )
    }

    @Test
    fun `delegate should invoke callback when didUpdateLocations is called`() {
        // Validates that protocol method implementation correctly invokes Kotlin callback
        // This is how iOS location updates flow into Kotlin code
        var updateCount = 0
        var lastLocation: CLLocation? = null
        val delegate =
            TestLocationDelegate { location ->
                updateCount++
                lastLocation = location
            }

        // Create a test CLLocation (any valid location will do)
        val testLocation = CLLocation(latitude = 37.7749, longitude = -122.4194)
        val locations = listOf(testLocation)

        // Simulate iOS calling the delegate method
        val locationManager = CLLocationManager()
        delegate.locationManager(
            manager = locationManager,
            didUpdateLocations = locations,
        )

        // Verify callback was invoked
        assertTrue(delegate.didUpdateCalled, "didUpdateLocations delegate method should be called")
        assertEquals(1, updateCount, "Location update callback should be invoked once")
        assertNotNull(lastLocation, "Callback should receive the location object")
    }

    @Test
    fun `delegate should handle didFailWithError correctly`() {
        // Validates error handling delegate method implementation
        var callbackInvoked = false
        val delegate = TestLocationDelegate { callbackInvoked = true }

        val locationManager = CLLocationManager()
        val error = NSError.errorWithDomain("TestDomain", code = 1, userInfo = null)

        // Simulate iOS calling error delegate method
        delegate.locationManager(
            manager = locationManager,
            didFailWithError = error,
        )

        // Verify error delegate method was called (but doesn't invoke update callback)
        assertTrue(delegate.didFailCalled, "didFailWithError delegate method should be called")
        assertFalse(
            callbackInvoked,
            "Update callback should not be invoked on error",
        )
    }

    @Test
    fun `delegate should handle authorization status changes`() {
        // Validates authorization change delegate method implementation
        var callbackInvoked = false
        val delegate = TestLocationDelegate { callbackInvoked = true }

        val locationManager = CLLocationManager()
        val authStatus = 3 // kCLAuthorizationStatusAuthorizedWhenInUse

        // Simulate iOS calling authorization change delegate method
        delegate.locationManager(
            manager = locationManager,
            didChangeAuthorizationStatus = authStatus,
        )

        // Verify authorization change delegate method was called
        assertTrue(
            delegate.authorizationChangedCalled,
            "didChangeAuthorizationStatus delegate method should be called",
        )
        assertFalse(
            callbackInvoked,
            "Update callback should not be invoked on authorization change",
        )
    }

    @Test
    fun `delegate should handle multiple location updates independently`() {
        // Validates that delegate can process multiple location updates correctly
        // Simulates real-world scenario of continuous GPS updates
        val updateHistory = mutableListOf<CLLocation>()
        val delegate =
            TestLocationDelegate { location ->
                updateHistory.add(location)
            }

        val locationManager = CLLocationManager()

        // Simulate multiple iOS location updates
        val location1 = CLLocation(latitude = 37.7749, longitude = -122.4194)
        val location2 = CLLocation(latitude = 37.7750, longitude = -122.4195)
        val location3 = CLLocation(latitude = 37.7751, longitude = -122.4196)

        delegate.locationManager(locationManager, didUpdateLocations = listOf(location1))
        delegate.locationManager(locationManager, didUpdateLocations = listOf(location2))
        delegate.locationManager(locationManager, didUpdateLocations = listOf(location3))

        // Verify all updates were processed
        assertEquals(3, updateHistory.size, "Should process all location updates")
    }

    @Test
    fun `multiple delegate instances should work independently`() {
        // Validates that multiple delegate instances maintain separate state
        // This is critical for proper encapsulation and avoiding shared mutable state
        var delegate1CallCount = 0
        var delegate2CallCount = 0

        val delegate1 = TestLocationDelegate { delegate1CallCount++ }
        val delegate2 = TestLocationDelegate { delegate2CallCount++ }

        val locationManager = CLLocationManager()
        val testLocation = CLLocation(latitude = 37.7749, longitude = -122.4194)

        // Update through delegate1
        delegate1.locationManager(locationManager, didUpdateLocations = listOf(testLocation))
        assertEquals(1, delegate1CallCount, "Delegate1 should be invoked")
        assertEquals(0, delegate2CallCount, "Delegate2 should not be affected")

        // Update through delegate2
        delegate2.locationManager(locationManager, didUpdateLocations = listOf(testLocation))
        assertEquals(1, delegate1CallCount, "Delegate1 should remain unchanged")
        assertEquals(1, delegate2CallCount, "Delegate2 should be invoked")

        // Verify state flags are independent
        assertTrue(delegate1.didUpdateCalled, "Delegate1 should have didUpdate called")
        assertTrue(delegate2.didUpdateCalled, "Delegate2 should have didUpdate called")
        assertFalse(delegate1.didFailCalled, "Delegate1 should not have didFail called")
        assertFalse(delegate2.didFailCalled, "Delegate2 should not have didFail called")
    }

    @Test
    fun `delegate should handle empty location list gracefully`() {
        // Validates edge case handling: empty location list should not crash
        var callbackInvoked = false
        val delegate = TestLocationDelegate { callbackInvoked = true }

        val locationManager = CLLocationManager()
        val emptyLocations = emptyList<Any>()

        // Simulate iOS calling with empty location list
        delegate.locationManager(
            manager = locationManager,
            didUpdateLocations = emptyLocations,
        )

        // Verify delegate method was called but callback was not (no locations)
        assertTrue(
            delegate.didUpdateCalled,
            "didUpdateLocations should be called even with empty list",
        )
        assertFalse(
            callbackInvoked,
            "Callback should not be invoked with empty location list",
        )
    }

    @Test
    fun `delegate should process only last location from multiple locations`() {
        // Validates the production pattern: when multiple locations arrive,
        // only process the most recent (last) one
        var processedLocationCount = 0
        var lastProcessedLatitude = 0.0
        val delegate =
            TestLocationDelegate { location ->
                processedLocationCount++
                lastProcessedLatitude = location.coordinate.useContents { latitude }
            }

        val locationManager = CLLocationManager()

        // Create multiple locations with different latitudes
        val location1 = CLLocation(latitude = 37.7749, longitude = -122.4194)
        val location2 = CLLocation(latitude = 37.7750, longitude = -122.4194)
        val location3 = CLLocation(latitude = 37.7751, longitude = -122.4194)

        // Simulate iOS sending all three locations at once (common behavior)
        delegate.locationManager(
            manager = locationManager,
            didUpdateLocations = listOf(location1, location2, location3),
        )

        // Verify only the last location was processed
        assertEquals(
            1,
            processedLocationCount,
            "Should process only the last location from the list",
        )
        assertEquals(
            37.7751,
            lastProcessedLatitude,
            0.0001,
            "Should process the last (most recent) location",
        )
    }
}

/**
 * Test delegate class that mimics the production IosLocationDelegate pattern.
 *
 * This test delegate:
 * - Extends NSObject() (required for ObjC protocols)
 * - Implements CLLocationManagerDelegateProtocol
 * - Provides tracking flags for test verification
 * - Implements all three delegate methods
 *
 * Mirrors the production implementation in IosLocationProvider.kt (lines 205-266).
 */
@OptIn(ExperimentalForeignApi::class)
private class TestLocationDelegate(
    private val onLocationUpdate: (CLLocation) -> Unit,
) : NSObject(),
    CLLocationManagerDelegateProtocol {
    // Tracking flags for test verification
    var didUpdateCalled = false
    var didFailCalled = false
    var authorizationChangedCalled = false

    override fun locationManager(
        manager: CLLocationManager,
        didUpdateLocations: List<*>,
    ) {
        didUpdateCalled = true

        // Mirror production pattern: filter to CLLocation, take last (most recent)
        val locations = didUpdateLocations.filterIsInstance<CLLocation>()
        locations.lastOrNull()?.let { location ->
            onLocationUpdate(location)
        }
    }

    override fun locationManager(
        manager: CLLocationManager,
        didFailWithError: NSError,
    ) {
        didFailCalled = true
        // Production would log error here
    }

    override fun locationManager(
        manager: CLLocationManager,
        didChangeAuthorizationStatus: Int,
    ) {
        authorizationChangedCalled = true
        // Production would handle authorization status here
    }
}
