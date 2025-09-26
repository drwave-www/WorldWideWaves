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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * iOS-specific location provider using Core Location framework.
 *
 * This implementation provides GPS location services for iOS, bridging with
 * the shared WWWLocationProvider interface. Currently provides a working
 * foundation with default location while full Core Location integration
 * will be completed with native iOS code integration.
 *
 * Features:
 * • StateFlow integration for reactive UI updates
 * • Error handling and logging
 * • Default location support for development
 * • Foundation for native Core Location integration
 *
 * Usage:
 * The location provider is automatically injected via Koin DI and used by:
 * • MapViewModels for user positioning
 * • Event area detection
 * • Wave progression tracking
 */
class IOSWWWLocationProvider : WWWLocationProvider {
    private val _currentLocation = MutableStateFlow<Position?>(null)
    override val currentLocation: StateFlow<Position?> = _currentLocation

    private var isUpdating = false
    private var onLocationUpdate: ((Position) -> Unit)? = null

    override fun startLocationUpdates(onLocationUpdate: (Position) -> Unit) {
        if (isUpdating) {
            WWWLogger.d("IOSWWWLocationProvider", "Location updates already started")
            return
        }

        this.onLocationUpdate = onLocationUpdate
        isUpdating = true

        // Provide default location for now
        // Full Core Location integration will be completed with native iOS code
        val defaultLocation =
            Position(
                lat = 37.7749, // San Francisco coordinates as default
                lng = -122.4194,
            )

        _currentLocation.value = defaultLocation
        onLocationUpdate(defaultLocation)

        WWWLogger.d("IOSWWWLocationProvider", "Started location updates with default location")
        WWWLogger.i("IOSWWWLocationProvider", "iOS location provider ready - Core Location integration pending")
    }

    override fun stopLocationUpdates() {
        isUpdating = false
        onLocationUpdate = null
        WWWLogger.d("IOSWWWLocationProvider", "Stopped location updates")
    }

    /**
     * Update location from iOS native code integration.
     * This method will be called from Swift/iOS native code once Core Location is integrated.
     */
    fun updateLocationFromNative(
        latitude: Double,
        longitude: Double,
    ) {
        if (isUpdating) {
            val position = Position(lat = latitude, lng = longitude)
            _currentLocation.value = position
            onLocationUpdate?.invoke(position)
            WWWLogger.v("IOSWWWLocationProvider", "Native location update: $latitude, $longitude")
        }
    }
}
