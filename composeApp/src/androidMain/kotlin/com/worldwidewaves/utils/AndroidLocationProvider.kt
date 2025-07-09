package com.worldwidewaves.utils

/*
 * Copyright 2024 DrWave
 *
 * WorldWideWaves is an ephemeral mobile app designed to orchestrate human waves through cities and
 * countries, culminating in a global wave. The project aims to transcend physical and cultural
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
import com.worldwidewaves.shared.WWWGlobals
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
class AndroidLocationProvider : KoinComponent, LocationProvider {

    val locationEngine: WWWSimulationEnabledLocationEngine by KoinJavaComponent.inject(
        WWWSimulationEnabledLocationEngine::class.java
    )

    private val _currentLocation = MutableStateFlow<Position?>(null)
    override val currentLocation: StateFlow<Position?> = _currentLocation

    private var proxyLocationEngine: LocationEngineProxy<LocationListener>? = null
    private var locationCallback: LocationEngineCallback<LocationEngineResult>? = null

    @SuppressLint("MissingPermission")
    override fun startLocationUpdates(onLocationUpdate: (Position) -> Unit) {
        if (proxyLocationEngine != null) return // Already started

        // Create location engine
        proxyLocationEngine = LocationEngineProxy(locationEngine)

        // Create callback
        locationCallback = object : LocationEngineCallback<LocationEngineResult> {
            override fun onSuccess(result: LocationEngineResult?) {
                result?.lastLocation?.let { location ->
                    val position = Position(location.latitude, location.longitude)
                    _currentLocation.value = position
                    onLocationUpdate(position)
                }
            }

            override fun onFailure(exception: Exception) {
                Log.e("EventMap", "Failed to get location: $exception")
            }
        }

        // Request location updates
        proxyLocationEngine?.requestLocationUpdates(
            buildLocationEngineRequest(),
            locationCallback!!,
            Looper.getMainLooper()
        )
    }

    override fun stopLocationUpdates() {
        locationCallback?.let { callback ->
            proxyLocationEngine?.removeLocationUpdates(callback)
        }
        proxyLocationEngine = null
        locationCallback = null
    }

    private fun buildLocationEngineRequest(): LocationEngineRequest =
        LocationEngineRequest.Builder(WWWGlobals.CONST_TIMER_GPS_UPDATE.inWholeMilliseconds)
            .setFastestInterval(WWWGlobals.CONST_TIMER_GPS_UPDATE.inWholeMilliseconds / 2)
            .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
            .build()
}