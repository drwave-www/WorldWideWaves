package com.worldwidewaves.compose

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.location.Location
import android.net.Uri
import android.os.Bundle
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.worldwidewaves.shared.events.WWWEvent
import com.worldwidewaves.shared.events.getMapBbox
import com.worldwidewaves.shared.events.getMapCenter
import com.worldwidewaves.shared.events.getMapStyleUri
import com.worldwidewaves.utils.requestLocationPermission
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.location.LocationComponent
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.LocationComponentOptions
import org.maplibre.android.location.engine.LocationEngineRequest
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapLibreMapOptions
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import java.io.File

/*
 * Copyright 2024 DrWave
 *
 * WorldWideWaves is an ephemeral mobile app designed to orchestrate human waves through cities and countries,
 * culminating in a global wave. The project aims to transcend physical and cultural boundaries, fostering unity,
 * community, and shared human experience by leveraging real-time coordination and location-based services.
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

class WWWEventMap(private val event: WWWEvent) {

    enum class CameraPosition {
        BOUNDS,
        DEFAULT_CENTER
    }

    @SuppressLint("MissingPermission")
    @Composable
    fun Screen(
        modifier: Modifier,
        initialCameraPosition: CameraPosition? = CameraPosition.BOUNDS,
    ) {
        val configuration = LocalConfiguration.current
        val context = LocalContext.current
        val mapView = rememberMapViewWithLifecycle()
        val styleUri = remember { mutableStateOf<Uri?>(null) }
        val mapStyle = remember { mutableStateOf<Style?>(null) }

        val locationComponent = remember { mutableStateOf<LocationComponent?>(null) }
        val (cLat, cLong) = event.getMapCenter()

        //val userLocation = remember { mutableStateOf<Location?>(null) }

        val hasLocationPermission = requestLocationPermission()

        // Setup Map properties
        LaunchedEffect(Unit) {
            styleUri.value = event.getMapStyleUri()?.let { Uri.fromFile(File(it)) }
        }

        // Update the position marker on user location change
//        LaunchedEffect(userLocation.value) {
//            userLocation.value?.let {
//                locationComponent.value?.forceLocationUpdate(it)
//            }
//        }

        // Calculate height based on aspect ratio and available width
        val calculatedHeight = configuration.screenWidthDp.dp / (16f / 9f)

        // The map view
        AndroidView(
            modifier = modifier
                .fillMaxWidth()
                .height(calculatedHeight),
            factory = { mapView },
            update = { mv ->
                mv.getMapAsync { map ->
                    styleUri.value?.let { uri ->
                        map.setStyle(
                            Style.Builder()
                                .fromUri(uri.toString())
                        ) { style ->
                            mapStyle.value = style
                            map.uiSettings.setAttributionMargins(15, 0, 0, 15)

                            // Add a marker for the user's position
                            if (hasLocationPermission) {
                                locationComponent.value = map.locationComponent
                                val locationComponentOptions =
                                    LocationComponentOptions.builder(context)
                                        .pulseEnabled(true)
                                        .pulseColor(Color.RED)
                                        .foregroundTintColor(Color.BLACK)
                                        .build()
                                val locationComponentActivationOptions =
                                    buildLocationComponentActivationOptions(
                                        context,
                                        style,
                                        locationComponentOptions
                                    )
                                locationComponent.value!!.activateLocationComponent(
                                    locationComponentActivationOptions
                                )
                                locationComponent.value!!.isLocationComponentEnabled = true
                                val location = Location("").apply {
                                    this.latitude = cLat
                                    this.longitude = cLong
                                }
                                locationComponent.value?.forceLocationUpdate(location)
                                locationComponent.value!!.cameraMode = CameraMode.NONE
                            }
                        }
                    }

                    setCameraPosition(initialCameraPosition, map)
                }
            }
        )
    }

    // -- Private functions ---------------------------------------------------

    private fun buildLocationComponentActivationOptions(
        context: Context,
        style: Style,
        locationComponentOptions: LocationComponentOptions
    ): LocationComponentActivationOptions {
        return LocationComponentActivationOptions
            .builder(context, style)
            .locationComponentOptions(locationComponentOptions)
            .useDefaultLocationEngine(true)
            .locationEngineRequest(
                LocationEngineRequest.Builder(1500)
                    .setFastestInterval(750)
                    .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
                    .build()
            )
            .build()
    }

    private fun setCameraPosition(
        initialCameraPosition: CameraPosition?,
        map: MapLibreMap
    ) {
        when (initialCameraPosition) {
            CameraPosition.DEFAULT_CENTER -> {
                val (cLat, cLng) = event.getMapCenter()
                map.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(cLat, cLng),
                        event.mapDefaultzoom ?: event.mapMinzoom.toDouble()
                    )
                )
            }

            CameraPosition.BOUNDS -> {
                val (swLng, swLat, neLng, neLat) = event.getMapBbox()
                val bounds = LatLngBounds.Builder()
                    .include(LatLng(swLat, swLng)) // Southwest corner
                    .include(LatLng(neLat, neLng)) // Northeast corner
                    .build()
                map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 0))
            }

            null -> {}
        }
    }

}

// -- Use the MapLibre MapView as a composable --------------------------------

/**
 * Remembers a MapView and gives it the lifecycle of the current LifecycleOwner
 * source : https://gist.github.com/PiotrPrus/d65378c36b0a0c744e647946f344103c
 */
@Composable
fun rememberMapViewWithLifecycle(): MapView {
    val context = LocalContext.current

    val maplibreMapOptions = MapLibreMapOptions.createFromAttributes(context)
    maplibreMapOptions.apply {
        //apiBaseUri("https://demotiles.maplibre.org/tiles/tiles.json")
        camera(
            CameraPosition.Builder()
                .bearing(0.0)
                .target(LatLng(48.8619, 2.3417))
                .zoom(10.0)
                .tilt(0.0)
                .build()
        )
        maxZoomPreference(14.0)
        minZoomPreference(10.0)
        localIdeographFontFamily("Droid Sans")

        compassEnabled(true)
        compassFadesWhenFacingNorth(true)

        zoomGesturesEnabled(false)
        scrollGesturesEnabled(false)
        horizontalScrollGesturesEnabled(false)
        rotateGesturesEnabled(false)
        tiltGesturesEnabled(false)
        doubleTapGesturesEnabled(false)
    }
    MapLibre.getInstance(context)

    val mapView = remember { MapView(context, maplibreMapOptions) }

    // Makes MapView follow the lifecycle of this composable
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, mapView) {
        val lifecycleObserver = getMapLifecycleObserver(mapView)
        lifecycle.addObserver(lifecycleObserver)
        onDispose {
            lifecycle.removeObserver(lifecycleObserver)
        }
    }

    return mapView
}

private fun getMapLifecycleObserver(mapView: MapView): LifecycleEventObserver =
    LifecycleEventObserver { _, event ->
        when (event) {
            Lifecycle.Event.ON_CREATE -> mapView.onCreate(Bundle())
            Lifecycle.Event.ON_START -> mapView.onStart()
            Lifecycle.Event.ON_RESUME -> mapView.onResume()
            Lifecycle.Event.ON_PAUSE -> mapView.onPause()
            Lifecycle.Event.ON_STOP -> mapView.onStop()
            Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
            else -> throw IllegalStateException()
        }
    }
