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

import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.os.Looper
import android.util.Log
import com.worldwidewaves.shared.WWWPlatform
import com.worldwidewaves.shared.toLocation
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.java.KoinJavaComponent
import org.maplibre.android.location.engine.LocationEngineCallback
import org.maplibre.android.location.engine.LocationEngineRequest
import org.maplibre.android.location.engine.LocationEngineResult
import org.maplibre.android.location.engine.MapLibreFusedLocationEngineImpl
import kotlin.time.ExperimentalTime

/**
 * `SimulationLocationEngine` is a custom implementation of `MapLibreFusedLocationEngineImpl`
 * that provides simulated location data when the platform is in simulation mode.
 *
 * This class overrides the default location engine behavior to return simulated locations
 * if the platform indicates that it is under simulation. Otherwise, it falls back to the
 * default location engine behavior.
 *
 */
@OptIn(ExperimentalTime::class)
class SimulationLocationEngine(
    context: Context,
) : MapLibreFusedLocationEngineImpl(context),
    KoinComponent {
    private companion object {
        private const val TAG = "WWW.SimulationEngine"
    }

    private val platform: WWWPlatform by KoinJavaComponent.inject(WWWPlatform::class.java)

    private fun getSimulatedLocation(): Location? =
        if (platform.isOnSimulation()) {
            val simulation = platform.getSimulation()!!
            // Use runBlocking since simulation.now() is suspend but this context is not
            // Safe here as this is called during location updates (not on main thread)
            simulation.getUserPosition().toLocation(runBlocking { simulation.now() })
        } else {
            null
        }

    override fun getLastLocation(callback: LocationEngineCallback<LocationEngineResult>) {
        val simulatedLocation = getSimulatedLocation()
        if (simulatedLocation != null) {
            Log.d(TAG, "Providing simulated last location: ${simulatedLocation.latitude}, ${simulatedLocation.longitude}")
            callback.onSuccess(LocationEngineResult.create(simulatedLocation))
        } else {
            Log.d(TAG, "Not simulating, delegating getLastLocation to real GPS")
            super.getLastLocation(callback)
        }
    }

    override fun requestLocationUpdates(
        request: LocationEngineRequest,
        listener: LocationListener,
        looper: Looper?,
    ) {
        val simulatedLocation = getSimulatedLocation()
        if (simulatedLocation != null) {
            Log.i(TAG, "Simulation mode active, providing simulation location to LocationListener")
            listener.onLocationChanged(simulatedLocation)
            return
        }

        // Not simulating - delegate to real GPS (FusedLocationProvider)
        Log.i(TAG, "Not simulating, delegating requestLocationUpdates to real GPS")
        super.requestLocationUpdates(request, listener, looper)
    }
}
