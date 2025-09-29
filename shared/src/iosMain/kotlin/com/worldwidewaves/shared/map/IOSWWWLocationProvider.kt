package com.worldwidewaves.shared.map

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

import com.worldwidewaves.shared.WWWGlobals
import com.worldwidewaves.shared.events.utils.Position
import com.worldwidewaves.shared.utils.Log
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusDenied
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.CoreLocation.kCLAuthorizationStatusRestricted
import platform.CoreLocation.kCLLocationAccuracyBest
import platform.Foundation.NSError
import platform.darwin.NSObject

/**
 * iOS-specific location provider using Core Location framework.
 *
 * Provides real GPS location services for iOS using CLLocationManager,
 * with proper permission handling and error management.
 */
@OptIn(ExperimentalForeignApi::class)
class IOSWWWLocationProvider : WWWLocationProvider {
    companion object {
        private const val TAG = "IOSWWWLocationProvider"
    }

    private val _currentLocation = MutableStateFlow<Position?>(null)
    override val currentLocation: StateFlow<Position?> = _currentLocation

    private val locationManager = CLLocationManager()
    private val locationDelegate =
        IOSLocationDelegate { location ->
            updateLocation(location)
        }

    private var isUpdating = false
    private var onLocationUpdate: ((Position) -> Unit)? = null

    init {
        setupLocationManager()
    }

    @Throws(Throwable::class)
    private fun setupLocationManager() {
        try {
            locationManager.delegate = locationDelegate
            locationManager.desiredAccuracy = kCLLocationAccuracyBest
            locationManager.distanceFilter = WWWGlobals.Wave.LINEAR_METERS_REFRESH // 10 meters

            Log.v(TAG, "Core Location manager setup completed")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to setup location manager", e)
            throw e
        }
    }

    override fun startLocationUpdates(onLocationUpdate: (Position) -> Unit) {
        if (isUpdating) {
            Log.d(TAG, "Location updates already started")
            return
        }

        this.onLocationUpdate = onLocationUpdate
        isUpdating = true

        Log.d(TAG, "Starting iOS location updates...")

        try {
            val authStatus = locationManager.authorizationStatus
            Log.d(TAG, "Current authorization status: $authStatus")

            when (authStatus) {
                kCLAuthorizationStatusNotDetermined -> {
                    Log.d(TAG, "Requesting location permission")
                    locationManager.requestWhenInUseAuthorization()
                    // Provide temporary fallback while waiting for permission
                    provideFallbackLocation()
                }
                kCLAuthorizationStatusAuthorizedWhenInUse,
                kCLAuthorizationStatusAuthorizedAlways,
                -> {
                    Log.d(TAG, "Location permission granted, starting updates")
                    locationManager.startUpdatingLocation()
                    // Also provide immediate fallback until first GPS fix
                    provideFallbackLocation()
                }
                kCLAuthorizationStatusDenied,
                kCLAuthorizationStatusRestricted,
                -> {
                    Log.w(TAG, "Location permission denied or restricted")
                    // Provide fallback behavior without location
                    provideFallbackLocation()
                }
                else -> {
                    Log.w(TAG, "Unknown location authorization status: $authStatus")
                    provideFallbackLocation()
                }
            }

            Log.d(TAG, "Started location updates with current status: $authStatus")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start location updates, using fallback", e)
            provideFallbackLocation()
        }
    }

    /**
     * Provide a fallback location for development and testing
     * This ensures the app remains functional even when GPS is unavailable
     */
    private fun provideFallbackLocation() {
        // Use a reasonable default location for development/testing
        val fallbackLocation =
            Position(
                lat = 37.7749, // San Francisco
                lng = -122.4194,
            )

        Log.d(TAG, "Providing fallback location: SF coordinates")
        _currentLocation.value = fallbackLocation
        onLocationUpdate?.invoke(fallbackLocation)
    }

    override fun stopLocationUpdates() {
        if (!isUpdating) return

        try {
            locationManager.stopUpdatingLocation()
            isUpdating = false
            onLocationUpdate = null
            _currentLocation.value = null

            Log.d(TAG, "Stopped location updates")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping location updates", e)
        }
    }

    private fun updateLocation(location: CLLocation) {
        if (!isUpdating) return

        try {
            location.coordinate.useContents {
                val position =
                    Position(
                        lat = latitude,
                        lng = longitude,
                    )

                // Validate position is reasonable
                if (isValidPosition(position)) {
                    _currentLocation.value = position
                    onLocationUpdate?.invoke(position)
                    Log.v(TAG, "Location updated: lat=${position.lat}, lng=${position.lng}, accuracy=${location.horizontalAccuracy}")
                } else {
                    Log.w(TAG, "Invalid location received: lat=${position.lat}, lng=${position.lng}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing location update", e)
        }
    }

    private fun isValidPosition(position: Position): Boolean =
        position.lat in WWWGlobals.Geodetic.MIN_LATITUDE..WWWGlobals.Geodetic.MAX_LATITUDE &&
            position.lng in WWWGlobals.Geodetic.MIN_LONGITUDE..WWWGlobals.Geodetic.MAX_LONGITUDE &&
            !position.lat.isNaN() &&
            !position.lng.isNaN() &&
            position.lat.isFinite() &&
            position.lng.isFinite()
}

/**
 * Core Location delegate implementation
 */
@OptIn(ExperimentalForeignApi::class)
private class IOSLocationDelegate(
    private val onLocationUpdate: (CLLocation) -> Unit,
) : NSObject(),
    CLLocationManagerDelegateProtocol {
    private val tag = "IOSLocationDelegate"

    override fun locationManager(
        manager: CLLocationManager,
        didUpdateLocations: List<*>,
    ) {
        try {
            val locations = didUpdateLocations.filterIsInstance<CLLocation>()
            val mostRecentLocation = locations.lastOrNull()

            mostRecentLocation?.let { location ->
                // Only use locations with reasonable accuracy
                if (location.horizontalAccuracy <= WWWGlobals.LocationAccuracy.GPS_LOW_ACCURACY_THRESHOLD) {
                    onLocationUpdate(location)
                } else {
                    Log.v(tag, "Ignoring inaccurate location: accuracy=${location.horizontalAccuracy}")
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error in didUpdateLocations", e)
        }
    }

    override fun locationManager(
        manager: CLLocationManager,
        didFailWithError: NSError,
    ) {
        Log.e(tag, "Location manager failed with error: ${didFailWithError.localizedDescription}")
    }

    override fun locationManager(
        manager: CLLocationManager,
        didChangeAuthorizationStatus: Int,
    ) {
        Log.d(tag, "Location authorization status changed: $didChangeAuthorizationStatus")

        when (didChangeAuthorizationStatus) {
            kCLAuthorizationStatusAuthorizedWhenInUse,
            kCLAuthorizationStatusAuthorizedAlways,
            -> {
                Log.d(tag, "Location permission granted, starting location updates")
                manager.startUpdatingLocation()
            }
            kCLAuthorizationStatusDenied -> {
                Log.w(tag, "Location permission denied by user")
            }
            kCLAuthorizationStatusRestricted -> {
                Log.w(tag, "Location services restricted")
            }
            kCLAuthorizationStatusNotDetermined -> {
                Log.d(tag, "Location permission not yet determined")
            }
            else -> {
                Log.w(tag, "Unknown location authorization status: $didChangeAuthorizationStatus")
            }
        }
    }
}
