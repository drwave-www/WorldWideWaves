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

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import com.worldwidewaves.shared.WWWGlobals.Companion.CONST_TIMER_GPS_UPDATE
import com.worldwidewaves.shared.WWWPlatform
import com.worldwidewaves.shared.events.utils.Position
import com.worldwidewaves.shared.generated.resources.ask_gps_enable
import com.worldwidewaves.shared.generated.resources.no
import com.worldwidewaves.shared.generated.resources.yes
import com.worldwidewaves.shared.map.LocationProvider
import com.worldwidewaves.shared.toLocation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.compose.resources.stringResource
import org.koin.core.component.KoinComponent
import org.koin.java.KoinJavaComponent.inject
import org.maplibre.android.location.engine.LocationEngineCallback
import org.maplibre.android.location.engine.LocationEngineProxy
import org.maplibre.android.location.engine.LocationEngineRequest
import org.maplibre.android.location.engine.LocationEngineResult
import org.maplibre.android.location.engine.MapLibreFusedLocationEngineImpl
import com.worldwidewaves.shared.generated.resources.Res as ShRes

@Composable
fun requestLocationPermission(): Boolean {
    val context = LocalContext.current
    var permissionGranted by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissionGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true &&
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    LaunchedEffect(Unit) {
        val fineLocationGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseLocationGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (fineLocationGranted && coarseLocationGranted) {
            permissionGranted = true
        } else {
            launcher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    return permissionGranted
}

@Composable
fun CheckGPSEnable() {
    val context = LocalContext.current
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    if (requestLocationPermission() && !locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
        AlertDialog.Builder(context)
            .setMessage(stringResource(ShRes.string.ask_gps_enable))
            .setCancelable(false)
            .setPositiveButton(stringResource(ShRes.string.yes)) { _, _ ->
                startActivity(context, Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), null)
            }
            .setNegativeButton(stringResource(ShRes.string.no)) { dialog, _ -> dialog.cancel() }
            .create()
            .show()
    }
}

/**
 * `WWWSimulationEnabledLocationEngine` is a custom implementation of `MapLibreFusedLocationEngineImpl`
 * that provides simulated location data when the platform is in simulation mode.
 *
 * This class overrides the default location engine behavior to return simulated locations
 * if the platform indicates that it is under simulation. Otherwise, it falls back to the
 * default location engine behavior.
 *
 */
class WWWSimulationEnabledLocationEngine(
    context: Context
) : KoinComponent, MapLibreFusedLocationEngineImpl(context) {

    private val platform: WWWPlatform by inject(WWWPlatform::class.java)

    private fun getSimulatedLocation(): Location? {
        return if (platform.isUnderSimulation()) {
            val simulation = platform.getSimulation()!!
            simulation.getUserPosition().toLocation(simulation.now())
        } else null
    }

    override fun getLastLocation(callback: LocationEngineCallback<LocationEngineResult>) =
        getSimulatedLocation()?.let { location ->
            callback.onSuccess(LocationEngineResult.create(location))
        } ?: super.getLastLocation(callback)

    override fun requestLocationUpdates(
        request: LocationEngineRequest,
        listener: LocationListener,
        looper: Looper?
    ) = getSimulatedLocation()?.let { location ->
        listener.onLocationChanged(location)
    } ?: super.requestLocationUpdates(request, listener, looper)
}

/**
 * Android-specific location provider
 */
class AndroidLocationProvider : KoinComponent, LocationProvider {

    val locationEngine: WWWSimulationEnabledLocationEngine by inject(WWWSimulationEnabledLocationEngine::class.java)

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
        LocationEngineRequest.Builder(CONST_TIMER_GPS_UPDATE.inWholeMilliseconds)
            .setFastestInterval(CONST_TIMER_GPS_UPDATE.inWholeMilliseconds / 2)
            .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
            .build()
}