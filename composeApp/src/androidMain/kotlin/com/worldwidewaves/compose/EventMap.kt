package com.worldwidewaves.compose

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

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.util.Log
import androidx.compose.foundation.Image
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
import com.worldwidewaves.shared.events.utils.Quadruple
import com.worldwidewaves.shared.generated.resources.map_error
import com.worldwidewaves.shared.toLatLngBounds
import com.worldwidewaves.theme.extendedLight
import com.worldwidewaves.utils.requestLocationPermission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.painterResource
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
import org.maplibre.android.maps.MapLibreMap.CancelableCallback
import org.maplibre.android.maps.MapLibreMapOptions
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import java.io.File
import kotlin.math.abs
import com.worldwidewaves.shared.generated.resources.Res as ShRes

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

    enum class MapCameraPosition { WINDOW, BOUNDS, DEFAULT_CENTER }

    // -------------------------

    @Composable
    fun Screen(modifier: Modifier) {
        val context = LocalContext.current
        val mapView = rememberMapViewWithLifecycle()

        var mapLoaded by remember { mutableStateOf(false) }
        var mapError by remember { mutableStateOf(false) }
        val coroutineScope = rememberCoroutineScope()
        var hasLocationPermission by remember { mutableStateOf(false) }

        // Request GPS location Android permissions
        hasLocationPermission = requestLocationPermission()

        // Setup Map Style and properties, initialize the map view
        LaunchedEffect(Unit) {

            val styleUri = withContext(Dispatchers.IO) {
                event.map.getStyleUri()?.let { Uri.fromFile(File(it)) }
            }

            styleUri?.let { uri ->
                mapView.getMapAsync { map ->

                    map.setStyle(Style.Builder().fromUri(uri.toString())) { style ->
                        map.uiSettings.setAttributionMargins(0, 0, 0, 0)

                        setCameraPosition(mapConfig.initialCameraPosition, map, coroutineScope) {
                            if (hasLocationPermission) {
                                addLocationMarkerToMap(map, context, coroutineScope, style)
                            }
                        }

                        onMapClick?.let { clickListener ->
                            map.addOnMapClickListener { point ->
                                clickListener(point.latitude, point.longitude)
                                true
                            }
                        }

                        onMapLoaded()
                        mapLoaded = true
                    }
                }
            } ?: run {
                mapError = true
            }
        }

        // The map view
        BoxWithConstraints(modifier = modifier) {
            if (!mapLoaded) {
                if (!mapError) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = extendedLight.quinary.color,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(maxWidth / 3)
                    )
                } else {
                    Image(
                        modifier = Modifier
                            .size(maxWidth / 4)
                            .align(Alignment.Center),
                        painter = painterResource(ShRes.drawable.map_error),
                        contentDescription = "error"
                    )
                }
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
        map: MapLibreMap,
        coroutineScope: CoroutineScope,
        onCameraPositionSet: (() -> Unit)? = null
    ) {
        when (initialCameraPosition) {
            MapCameraPosition.DEFAULT_CENTER -> moveToCenter(map, coroutineScope, onCameraPositionSet)
            MapCameraPosition.BOUNDS -> moveToMapBounds(map, coroutineScope, onCameraPositionSet)
            MapCameraPosition.WINDOW -> moveToWindowBounds(map, coroutineScope, onCameraPositionSet)
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
        var lastLocation: Location? = null
        var userHasBeenLocated = false

        map.locationComponent.activateLocationComponent(
            buildLocationComponentActivationOptions(context, style)
        )
        map.locationComponent.isLocationComponentEnabled = true
        map.locationComponent.cameraMode = CameraMode.NONE // Do not track user

        map.locationComponent.locationEngine?.requestLocationUpdates(
            buildLocationEngineRequest(),
            object : LocationEngineCallback<LocationEngineResult> {
                override fun onSuccess(result: LocationEngineResult?) {
                    result?.lastLocation?.let { location ->
                        // Notify the UI of the user's location
                        onLocationUpdate(LatLng(location.latitude, location.longitude))

                        // Follow user, the first time only
                        if (!userHasBeenLocated && mapConfig.initialCameraPosition == MapCameraPosition.WINDOW) {
                            moveToLocation(coroutineScope, location, map) // Area bounds are checked here
                            userHasBeenLocated = true
                        }

                        // Record the new location
                        lastLocation = location
                    }
                }
                override fun onFailure(exception: Exception) {
                    Log.e("EventMap","Failed to get location: $exception")
                }
            },
            Looper.getMainLooper()
        )

        // Allow the wave to now the current location of the user
        event.wave.setPositionRequester {
            lastLocation?.let { Position(it.latitude, it.longitude) }
        }
    }

    // ------------------------

    /**
     * Animates the MapLibre camera to the given location if the location is within the event area.
     *
     * @param coroutineScope The coroutine scope in which to launch the animation.
     * @param location The location to move the camera to.
     * @param map The MapLibre map object.
     */
    private fun moveToLocation(coroutineScope: CoroutineScope, location: Location, map: MapLibreMap) {
        coroutineScope.launch {
            val isWithin = withContext(Dispatchers.IO) {
                event.area.isPositionWithin(Position(location.latitude, location.longitude))
            }
            if (isWithin) {
                val currentZoom = map.cameraPosition.zoom
                map.animateCamera(
                    CameraUpdateFactory.newCameraPosition(
                        CameraPosition.Builder()
                            .target(LatLng(location.latitude, location.longitude))
                            .zoom(currentZoom)
                            .build()
                    )
                )
            }
        }
    }

    /**
     * Animates the MapLibre camera to the center of the event map with the default zoom level.
     * If the default zoom level is not available, it uses the minimum zoom level.
     *
     * @param map The MapLibre map object.
     */
    private fun moveToCenter(
        map: MapLibreMap,
        coroutineScope: CoroutineScope,
        onCameraPositionSet: (() -> Unit)?
    ) {
        coroutineScope.launch {
            val (cLat, cLng) = withContext(Dispatchers.IO) { event.area.getCenter() }
            map.animateCamera(
                CameraUpdateFactory.newLatLng(LatLng(cLat, cLng)),
                object : CancelableCallback {
                    override fun onFinish() { onCameraPositionSet?.invoke() }
                    override fun onCancel() {}
                }
            )
        }
    }

    /**
     * Adjusts the MapLibre camera to fit the bounds of the event map,
     * while maintaining the correct aspect ratio and centering.*
     * @param map The MapLibre map object.
     */
    private fun moveToWindowBounds(
        map: MapLibreMap,
        coroutineScope: CoroutineScope,
        onCameraPositionSet: (() -> Unit)?
    ) {
        coroutineScope.launch {
            val bbox = event.area.getBoundingBox()

            // Maximize the view to the map
            val (sw, ne) = bbox
            val eventMapWidth = ne.lng - sw.lng
            val eventMapHeight = ne.lat - sw.lat
            val (centerLat, centerLng) = event.area.getCenter()

            // Calculate the aspect ratios of the event map and MapLibre component.
            val eventAspectRatio = eventMapWidth / eventMapHeight
            val mapLibreAspectRatio = map.width.toDouble() / map.height.toDouble()

            // Calculate the new southwest and northeast longitudes or latitudes,
            // depending on whether the event map is wider or taller than the MapLibre component.
            val (newSwLat, newNeLat, newSwLng, newNeLng) = if (eventAspectRatio > mapLibreAspectRatio) {
                val lngDiff = eventMapHeight * mapLibreAspectRatio / 2
                Quadruple(sw.lat, ne.lat, centerLng - lngDiff, centerLng + lngDiff)
            } else {
                val latDiff = eventMapWidth / mapLibreAspectRatio / 2
                Quadruple(centerLat - latDiff, centerLat + latDiff, sw.lng, ne.lng)
            }

            val bounds = LatLngBounds.Builder()
                .include(LatLng(newSwLat, newSwLng))
                .include(LatLng(newNeLat, newNeLng))
                .build()

            map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds,0),
                object: CancelableCallback {
                    override fun onFinish() {
                        // Set the min/max camera zoom level
                        map.setMinZoomPreference(map.cameraPosition.zoom)
                        map.setMaxZoomPreference(event.map.maxZoom)

                        // Constrain the camera movement to the bounds of the map
                        map.addOnCameraMoveListener {
                            constrainCameraOnMap(map, coroutineScope)
                        }

                        onCameraPositionSet?.invoke()
                    }
                    override fun onCancel() {}
                }
            )
        }
    }

    /**
     * Constrains the map camera movement to stay within the event area's bounding box.
     *
     * This function adds a camera move listener to the map. When the camera moves, it checks if the
     * visible region extends beyond the event area's bounding box. If so, it adjusts the camera bounds
     * to ensure the visible region is contained within the bounding box.
     *
     */
    private val epsilon = 1e-7
    private var dimPosition = Position(0.0, 0.0)

    private fun constrainCameraOnMap(
        map: MapLibreMap,
        coroutineScope: CoroutineScope
    ) {
        val viewBounds = map.projection.visibleRegion.latLngBounds
        val dimLat = (viewBounds.latitudeNorth - viewBounds.latitudeSouth) / 2
        val dimLng = (viewBounds.longitudeEast - viewBounds.longitudeWest) / 2

        if (abs(dimLat - dimPosition.lat) > epsilon || abs(dimLng - dimPosition.lng) > epsilon) {
            dimPosition = Position(dimLat, dimLng)

            coroutineScope.launch {
                val mapBounds = event.area.getBoundingBox()
                val bounds = LatLngBounds.Builder()
                    .include(LatLng(mapBounds.sw.lat + dimLat, mapBounds.sw.lng + dimLng))
                    .include(LatLng(mapBounds.ne.lat - dimLat, mapBounds.ne.lng - dimLng))
                    .build()
                map.setLatLngBoundsForCameraTarget(bounds)
            }
        }
    }

    /**
     * Adjusts the MapLibre camera to fit the bounds of the event map.
     *
     * @param map The MapLibre map object.
     */
    private fun moveToMapBounds(
        map: MapLibreMap,
        coroutineScope: CoroutineScope,
        onCameraPositionSet: (() -> Unit)?
    ) = coroutineScope.launch {
        map.animateCamera(
            CameraUpdateFactory.newLatLngBounds(
                event.area.getBoundingBox().toLatLngBounds(), 0
            ),
            object : CancelableCallback {
                override fun onFinish() { onCameraPositionSet?.invoke() }
                override fun onCancel() {}
            }
        )
    }

    // -------------------------------------------------------------------------

    /**
     * Builds `LocationComponentActivationOptions` for configuring the Mapbox location component.
     *
     * This function sets up the location component enabling a
     * pulsing animation around the user location.
     *
     * @param context The context of the application.
     * @param style The map style to use with the location component.
     * @return The configured `LocationComponentActivationOptions` object.
     */
    private fun buildLocationComponentActivationOptions(
        context: Context,
        style: Style
    ): LocationComponentActivationOptions {

        return LocationComponentActivationOptions.builder(context, style)
            .locationComponentOptions(
                LocationComponentOptions.builder(context)
                    .pulseEnabled(true)
                    .pulseColor(Color.RED)
                    .foregroundTintColor(Color.BLACK)
                    .build()
            )
            .useDefaultLocationEngine(true)
            .locationEngineRequest(buildLocationEngineRequest())
            .build()
    }

    /**
     * Builds a `LocationEngineRequest` for location updates.
     *
     * @return The configured `LocationEngineRequest` object.
     */
    private fun buildLocationEngineRequest(): LocationEngineRequest =
        LocationEngineRequest.Builder(CONST_TIMER_GPS_UPDATE.toLong())
            .setFastestInterval(CONST_TIMER_GPS_UPDATE.toLong() / 2)
            .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
            .build()

    // -------------------------------------------------------------------------

    /**
     * Remembers a MapView and gives it the lifecycle of the current LifecycleOwner
     * source : https://gist.github.com/PiotrPrus/d65378c36b0a0c744e647946f344103c
     */
    @Composable
    fun rememberMapViewWithLifecycle(): MapView {
        val context = LocalContext.current

        val maplibreMapOptions = MapLibreMapOptions.createFromAttributes(context)
        maplibreMapOptions.apply {
            camera(CameraPosition.Builder()
                .padding(0.0,0.0,0.0,0.0)
                .bearing(0.0)
                .tilt(0.0)
                .build()
            )

            localIdeographFontFamily("Droid Sans") // TODO: replace, cf https://github.com/maplibre/font-maker

            compassEnabled(true)
            compassFadesWhenFacingNorth(true)

            val activateMapGestures = mapConfig.initialCameraPosition == MapCameraPosition.WINDOW

            zoomGesturesEnabled(activateMapGestures)
            scrollGesturesEnabled(activateMapGestures)
            doubleTapGesturesEnabled(activateMapGestures)

            // Always deactivate rotation gestures
            rotateGesturesEnabled(false)
            tiltGesturesEnabled(false)
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

/**
 * Creates a `LifecycleEventObserver` that synchronizes the lifecycle of a `MapView` with the
 * lifecycle of a `LifecycleOwner`.
 *
 * This observer ensures that the `MapView` receives the corresponding lifecycle events
 * (`onCreate`, `onStart`, `onResume`, `onPause`, `onStop`, `onDestroy`) as the
 * `LifecycleOwner` transitions through its lifecycle states.
 *
 * @param mapView The `MapView` to observe lifecycle events for.
 * @return A `LifecycleEventObserver` that manages the `MapView`'s lifecycle.
 */
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
