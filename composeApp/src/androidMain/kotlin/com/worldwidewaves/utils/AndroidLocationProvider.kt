package com.worldwidewaves.utils

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

import android.annotation.SuppressLint
import android.location.LocationListener
import android.os.Looper
import android.util.Log
import com.worldwidewaves.shared.WWWGlobals.Timing
import com.worldwidewaves.shared.events.utils.Position
import com.worldwidewaves.shared.map.LocationProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.component.KoinComponent
import org.koin.java.KoinJavaComponent
import org.maplibre.android.location.engine.LocationEngineCallback
import org.maplibre.android.location.engine.LocationEngineProxy
import org.maplibre.android.location.engine.LocationEngineRequest
import org.maplibre.android.location.engine.LocationEngineResult

/**
 * Android-specific location provider
 */
class AndroidLocationProvider :
    KoinComponent,
    LocationProvider {
    private companion object {
        private const val TAG = "WWW.GPS.Provider"
    }

    val locationEngine: SimulationLocationEngine by KoinJavaComponent.inject(
        SimulationLocationEngine::class.java,
    )

    private val _currentLocation = MutableStateFlow<Position?>(null)
    override val currentLocation: StateFlow<Position?> = _currentLocation

    private var proxyLocationEngine: LocationEngineProxy<LocationListener>? = null
    private var locationCallback: LocationEngineCallback<LocationEngineResult>? = null

    @SuppressLint("MissingPermission")
    override fun startLocationUpdates(onLocationUpdate: (Position) -> Unit) {
        if (proxyLocationEngine != null) {
            Log.i(TAG, "Location updates already started (idempotent guard)")
            return // Already started
        }

        Log.i(TAG, "Starting GPS location updates (interval=${Timing.GPS_UPDATE_INTERVAL})")

        // Create location engine
        proxyLocationEngine = LocationEngineProxy(locationEngine)

        // Create callback
        locationCallback =
            object : LocationEngineCallback<LocationEngineResult> {
                override fun onSuccess(result: LocationEngineResult?) {
                    result?.lastLocation?.let { location ->
                        val position = Position(location.latitude, location.longitude)
                        _currentLocation.value = position
                        Log.d(TAG, "GPS position received: $position (accuracy=${location.accuracy}m)")
                        onLocationUpdate(position)
                    }
                }

                override fun onFailure(exception: Exception) {
                    Log.e(TAG, "GPS location update failed", exception)
                }
            }

        // Request location updates
        proxyLocationEngine?.requestLocationUpdates(
            buildLocationEngineRequest(),
            locationCallback!!,
            Looper.getMainLooper(),
        )
        Log.i(TAG, "GPS location updates requested successfully")

        // Immediately fetch last known location to trigger initial callback
        // This ensures PositionManager gets an immediate position update even in emulator
        proxyLocationEngine?.getLastLocation(locationCallback!!)
        Log.d(TAG, "Requested last known location for immediate callback")
    }

    override fun stopLocationUpdates() {
        if (proxyLocationEngine == null) {
            Log.d(TAG, "Location updates already stopped")
            return
        }

        Log.i(TAG, "Stopping GPS location updates")
        locationCallback?.let { callback ->
            proxyLocationEngine?.removeLocationUpdates(callback)
        }
        proxyLocationEngine = null
        locationCallback = null
        Log.i(TAG, "GPS location updates stopped successfully")
    }

    private fun buildLocationEngineRequest(): LocationEngineRequest =
        LocationEngineRequest
            .Builder(Timing.GPS_UPDATE_INTERVAL.inWholeMilliseconds)
            .setFastestInterval(Timing.GPS_UPDATE_INTERVAL.inWholeMilliseconds / 2)
            .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
            .build()
}
