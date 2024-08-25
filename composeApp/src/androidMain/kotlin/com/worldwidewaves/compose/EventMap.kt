package com.worldwidewaves.compose

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.worldwidewaves.shared.WWWGlobals.Companion.CONST_TIMER_GPS_UPDATE
import com.worldwidewaves.shared.events.WWWEvent
import com.worldwidewaves.shared.events.utils.Position
import com.worldwidewaves.theme.extendedLight
import com.worldwidewaves.utils.requestLocationPermission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.LocationComponentOptions
import org.maplibre.android.location.engine.LocationEngineCallback
import org.maplibre.android.location.engine.LocationEngineRequest
import org.maplibre.android.location.engine.LocationEngineResult
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapLibreMapOptions
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import java.io.File
import kotlin.math.log2

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

class EventMap(
    private val event: WWWEvent,
    private val onMapLoaded: () -> Unit = {},
    private val onLocationUpdate: (LatLng) -> Unit = {},
    private val onMapClick: ((latitude: Double, longitude: Double) -> Unit)? = null,
    private val mapConfig: EventMapConfig = EventMapConfig()
) {

    data class EventMapConfig(
        val initialCameraPosition: MapCameraPosition? = MapCameraPosition.BOUNDS
    )

    enum class MapCameraPosition {
        WINDOW,
        BOUNDS,
        DEFAULT_CENTER
    }

    // -------------------------

    @Composable
    fun Screen(modifier: Modifier) {
        val context = LocalContext.current
        val mapView = rememberMapViewWithLifecycle()
        var mapLoaded by remember { mutableStateOf(false) }
        val coroutineScope = rememberCoroutineScope()

        // Request GPS location Android permissions
        val hasLocationPermission = requestLocationPermission()

        // DEBUG
        mapLoaded = true

        // Setup Map Style and properties
        LaunchedEffect(Unit) {
            val styleUri = event.map.getStyleUri()?.let { Uri.fromFile(File(it)) }
            mapView.getMapAsync { map ->

                setCameraPosition(mapConfig.initialCameraPosition, map)

                styleUri?.let { uri ->
                    map.setStyle( Style.Builder().fromUri(uri.toString()) ) { style ->
                        map.uiSettings.setAttributionMargins(15, 0, 0, 15)

                        // Add a marker for the user's position
                        if (hasLocationPermission) {
                            addLocationMarkerToMap(map, context, coroutineScope, style)
                        }

                        map.addOnMapClickListener { point ->
                            onMapClick?.invoke(point.latitude, point.longitude)
                            true
                        }

                        // TODO: determine the accurate min and max zoom values
//                        map.setMinZoomPreference(event.mapMinzoom)
//                        map.setMaxZoomPreference(event.mapMaxzoom)
                        // TODO: determine the accurate bounds
//                        val (swLng, swLat, neLng, neLat) = event.map.getBbox()
//                        map.setLatLngBoundsForCameraTarget(LatLngBounds.Builder()
//                            .include(LatLng(swLat, swLng)) // Southwest corner
//                            .include(LatLng(neLat, neLng)) // Northeast corner
//                            .build())

                        onMapLoaded()
                        mapLoaded = true
                    }
                }
            }
        }

        // The map view
        BoxWithConstraints(modifier = modifier) {
            if (!mapLoaded) {
                val size = maxWidth / 3
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = extendedLight.quinary.color,
                    modifier = Modifier.align(Alignment.Center).size(size)
                )
            }
            AndroidView(
                factory = { mapView },
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(if (mapLoaded) 1f else 0f)
            )
        }
    }

    // -------------------------

    private fun setCameraPosition(
        initialCameraPosition: MapCameraPosition?,
        map: MapLibreMap
    ) {
        when (initialCameraPosition) {
            MapCameraPosition.DEFAULT_CENTER -> {
                val (cLat, cLng) = event.map.getCenter()
                map.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(cLat, cLng),
                        event.mapDefaultzoom ?: event.mapMinzoom
                    )
                )
            }

            MapCameraPosition.BOUNDS -> {
                val (swLng, swLat, neLng, neLat) = event.map.getBbox()
                val bounds = LatLngBounds.Builder()
                    .include(LatLng(swLat, swLng)) // Southwest corner
                    .include(LatLng(neLat, neLng)) // Northeast corner
                    .build()
                map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 0))
            }

            MapCameraPosition.WINDOW -> { // TODO: calcul is wrong
                val (swLng, swLat, neLng, neLat) = event.map.getBbox()
                val bounds = LatLngBounds.Builder()
                    .include(LatLng(swLat, swLng)) // Southwest corner
                    .include(LatLng(neLat, neLng)) // Northeast corner
                    .build()

                // Calculate the aspect ratio of the map view
                val mapViewWidth = map.width.toDouble()
                val mapViewHeight = map.height.toDouble()
                val mapViewAspectRatio = mapViewWidth / mapViewHeight

                // Calculate the aspect ratio of the bounding box
                val boundsWidth = bounds.longitudeEast - bounds.longitudeWest
                val boundsHeight = bounds.latitudeNorth - bounds.latitudeSouth
                val boundsAspectRatio = boundsWidth / boundsHeight

                // Adjust the zoom level to fit the bounding box within the map view
                val newZoom = if (mapViewAspectRatio > boundsAspectRatio) {
                    // Fit to height
                    val heightZoom = map.cameraPosition.zoom + log2(mapViewHeight / boundsHeight)
                    heightZoom
                } else {
                    // Fit to width
                    val widthZoom = map.cameraPosition.zoom + log2(mapViewWidth / boundsWidth)
                    widthZoom
                }

                map.animateCamera(CameraUpdateFactory.newLatLngZoom(bounds.center, newZoom))
            }

            null -> {}
        }
    }

    // -- Private Map location setup functions --------------------------------

    @SuppressLint("MissingPermission")
    private fun addLocationMarkerToMap(
        map: MapLibreMap,
        context: Context,
        coroutineScope: CoroutineScope,
        style: Style
    ) {
        map.locationComponent.activateLocationComponent(
            buildLocationComponentActivationOptions(context, style)
        )
        map.locationComponent.isLocationComponentEnabled = true
        map.locationComponent.cameraMode = CameraMode.NONE

        map.locationComponent.locationEngine?.requestLocationUpdates(
            buildLocationEngineRequest(),
            object : LocationEngineCallback<LocationEngineResult> {
                override fun onSuccess(result: LocationEngineResult?) {
                    result?.lastLocation?.let { location ->

                        onLocationUpdate(
                            LatLng(
                                location.latitude,
                                location.longitude
                            )
                        )

                        // Follow user while he's within bounds
                        if (mapConfig.initialCameraPosition == MapCameraPosition.WINDOW) {
                            coroutineScope.launch {
                                if (event.area.isPositionWithin(Position(location.latitude, location.longitude))) {
                                    map.animateCamera(
                                        CameraUpdateFactory.newCameraPosition(
                                            CameraPosition.Builder()
                                                .target(LatLng(location.latitude, location.longitude))
                                                .build()
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                override fun onFailure(exception: Exception) {
                    // Handle failure if needed
                }
            },
            Looper.getMainLooper()
        )
    }

    // ------------------------

    private fun buildLocationComponentActivationOptions(
        context: Context,
        style: Style
    ): LocationComponentActivationOptions {

        return LocationComponentActivationOptions
            .builder(context, style)
            .locationComponentOptions(
                LocationComponentOptions.builder(context)
                    .pulseEnabled(true)
                    .pulseColor(Color.RED)
                    .foregroundTintColor(Color.BLACK)
                    .build()
            )
            .useDefaultLocationEngine(true)
            .locationEngineRequest(
                buildLocationEngineRequest()
            )
            .build()
    }

    private fun buildLocationEngineRequest(): LocationEngineRequest =
        LocationEngineRequest.Builder(CONST_TIMER_GPS_UPDATE.toLong())
            .setFastestInterval(CONST_TIMER_GPS_UPDATE.toLong() / 2)
            .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
            .build()

    // ------------------------

    /**
     * Remembers a MapView and gives it the lifecycle of the current LifecycleOwner
     * source : https://gist.github.com/PiotrPrus/d65378c36b0a0c744e647946f344103c
     */
    @Composable
    fun rememberMapViewWithLifecycle(): MapView {
        val context = LocalContext.current

        val maplibreMapOptions = MapLibreMapOptions.createFromAttributes(context)
        maplibreMapOptions.apply {
            camera(CameraPosition.Builder().bearing(0.0).tilt(0.0).build())

            localIdeographFontFamily("Droid Sans") // TODO: replace, cf https://github.com/maplibre/font-maker

            compassEnabled(true)
            compassFadesWhenFacingNorth(true)

            val activateMapGestures = mapConfig.initialCameraPosition == MapCameraPosition.WINDOW

            zoomGesturesEnabled(activateMapGestures)
            scrollGesturesEnabled(activateMapGestures)
            horizontalScrollGesturesEnabled(activateMapGestures)
            rotateGesturesEnabled(activateMapGestures)
            tiltGesturesEnabled(activateMapGestures)
            doubleTapGesturesEnabled(activateMapGestures)
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

}

// -- Use the MapLibre MapView as a composable --------------------------------

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
