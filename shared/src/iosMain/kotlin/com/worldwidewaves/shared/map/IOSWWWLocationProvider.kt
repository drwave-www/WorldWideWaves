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

import com.worldwidewaves.shared.events.utils.Position
import com.worldwidewaves.shared.utils.WWWLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusDenied
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.CoreLocation.kCLAuthorizationStatusRestricted
import platform.Foundation.NSError
import platform.darwin.NSObject

/**
 * iOS-specific location provider using Core Location framework.
 *
 * This implementation provides GPS location services for iOS using CLLocationManager,
 * with proper permission handling and error management. It bridges iOS Core Location
 * with the shared WWWLocationProvider interface.
 *
 * Note: This is a basic implementation. Full Core Location integration will be
 * completed in iOS native code to avoid complex interop issues.
 *
 * Features:
 * • StateFlow integration for reactive UI updates
 * • Error handling and logging
 * • Mock location support for development
 *
 * Usage:
 * The location provider is automatically injected via Koin DI and used by:
 * • MapViewModels for user positioning
 * • Event area detection
 * • Wave progression tracking
 */
class IOSWWWLocationProvider : WWWLocationProvider, NSObject(), CLLocationManagerDelegateProtocol {

    private val _currentLocation = MutableStateFlow<Position?>(null)
    override val currentLocation: StateFlow<Position?> = _currentLocation

    private var isUpdating = false
    private var onLocationUpdate: ((Position) -> Unit)? = null

    private val locationManager = CLLocationManager()
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    init {
        locationManager.delegate = this
        locationManager.desiredAccuracy = platform.CoreLocation.kCLLocationAccuracyBest
    }

    override fun startLocationUpdates(onLocationUpdate: (Position) -> Unit) {
        if (isUpdating) {
            WWWLogger.d("IOSWWWLocationProvider", "Location updates already started")
            return
        }

        this.onLocationUpdate = onLocationUpdate
        isUpdating = true

        WWWLogger.d("IOSWWWLocationProvider", "Starting iOS Core Location updates")

        // Check current authorization status
        when (locationManager.authorizationStatus) {
            kCLAuthorizationStatusNotDetermined -> {
                WWWLogger.i("IOSWWWLocationProvider", "Requesting location permission")
                locationManager.requestWhenInUseAuthorization()
            }
            kCLAuthorizationStatusAuthorizedWhenInUse -> {
                WWWLogger.d("IOSWWWLocationProvider", "Permission granted, starting location updates")
                locationManager.startUpdatingLocation()
            }
            kCLAuthorizationStatusDenied, kCLAuthorizationStatusRestricted -> {
                WWWLogger.w("IOSWWWLocationProvider", "Location permission denied, using default location")
                useDefaultLocation()
            }
            else -> {
                WWWLogger.w("IOSWWWLocationProvider", "Unknown location permission status, using default location")
                useDefaultLocation()
            }
        }
    }

    override fun stopLocationUpdates() {
        isUpdating = false
        onLocationUpdate = null
        locationManager.stopUpdatingLocation()
        WWWLogger.d("IOSWWWLocationProvider", "Stopped location updates")
    }

    /**
     * CLLocationManagerDelegate method - called when authorization status changes
     */
    override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
        WWWLogger.d("IOSWWWLocationProvider", "Authorization status changed to: ${manager.authorizationStatus}")

        when (manager.authorizationStatus) {
            kCLAuthorizationStatusAuthorizedWhenInUse -> {
                if (isUpdating) {
                    WWWLogger.d("IOSWWWLocationProvider", "Permission granted, starting location updates")
                    manager.startUpdatingLocation()
                }
            }
            kCLAuthorizationStatusDenied, kCLAuthorizationStatusRestricted -> {
                WWWLogger.w("IOSWWWLocationProvider", "Location permission denied")
                useDefaultLocation()
            }
            else -> {
                // Handle other states if needed
            }
        }
    }

    /**
     * CLLocationManagerDelegate method - called when new location is received
     */
    override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
        val locations = didUpdateLocations.filterIsInstance<CLLocation>()
        val location = locations.lastOrNull()

        location?.let { loc ->
            val position = Position(
                lat = loc.coordinate.latitude,
                lng = loc.coordinate.longitude
            )

            coroutineScope.launch {
                _currentLocation.value = position
                onLocationUpdate?.invoke(position)
                WWWLogger.v("IOSWWWLocationProvider", "Location update: ${position.lat}, ${position.lng}")
            }
        }
    }

    /**
     * CLLocationManagerDelegate method - called when location update fails
     */
    override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) {
        WWWLogger.e("IOSWWWLocationProvider", "Location update failed: ${didFailWithError.localizedDescription}")
        useDefaultLocation()
    }

    /**
     * Fallback to default location when GPS is unavailable
     */
    private fun useDefaultLocation() {
        val defaultLocation = Position(
            lat = 37.7749, // San Francisco coordinates as default
            lng = -122.4194
        )

        coroutineScope.launch {
            _currentLocation.value = defaultLocation
            onLocationUpdate?.invoke(defaultLocation)
        }

        WWWLogger.i("IOSWWWLocationProvider", "Using default location: San Francisco")
    }
}