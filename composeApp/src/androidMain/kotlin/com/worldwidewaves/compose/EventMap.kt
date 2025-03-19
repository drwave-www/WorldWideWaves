package com.worldwidewaves.compose

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
import android.content.Context
import android.graphics.Color
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.toColorInt
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.worldwidewaves.shared.WWWGlobals.Companion.CONST_MAPLIBRE_TARGET_USER_ZOOM
import com.worldwidewaves.shared.WWWGlobals.Companion.CONST_MAPLIBRE_TARGET_WAVE_ZOOM
import com.worldwidewaves.shared.WWWGlobals.Companion.CONST_TIMER_GPS_UPDATE
import com.worldwidewaves.shared.WWWPlatform
import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.events.utils.BoundingBox
import com.worldwidewaves.shared.events.utils.PolygonUtils.Quad
import com.worldwidewaves.shared.events.utils.Position
import com.worldwidewaves.shared.generated.resources.map_error
import com.worldwidewaves.shared.toLatLngBounds
import com.worldwidewaves.theme.extendedLight
import com.worldwidewaves.utils.CheckGPSEnable
import com.worldwidewaves.utils.WWWSimulationEnabledLocationEngine
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
import org.maplibre.android.location.engine.LocationEngineProxy
import org.maplibre.android.location.engine.LocationEngineRequest
import org.maplibre.android.location.engine.LocationEngineResult
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapLibreMap.CancelableCallback
import org.maplibre.android.maps.MapLibreMapOptions
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Polygon
import java.io.File
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import com.worldwidewaves.shared.generated.resources.Res as ShRes

class EventMap(
    private val platform: WWWPlatform?,
    private val event: IWWWEvent,
    private val onMapLoaded: () -> Unit = {},
    private val onLocationUpdate: (LatLng) -> Unit = {},
    private val onMapClick: ((latitude: Double, longitude: Double) -> Unit)? = null,
    private val mapConfig: EventMapConfig = EventMapConfig()
) {

    data class EventMapConfig(
        val initialCameraPosition: MapCameraPosition? = MapCameraPosition.BOUNDS
    )

    enum class MapCameraPosition { WINDOW, BOUNDS, DEFAULT_CENTER }
    var mapMinZoomLevel: Double? = null

    private var mapViewState: MapView? = null
    private var lastLocation: Location? = null
    private var userHasBeenLocated = false

    // -------------------------

    @Composable
    fun Screen(modifier: Modifier) {
        val context = LocalContext.current
        val mapView = rememberMapViewWithLifecycle()

        // Store mapView in shared state
        mapViewState = mapView

        var mapLoaded by remember { mutableStateOf(false) }
        var mapError by remember { mutableStateOf(false) }
        val coroutineScope = rememberCoroutineScope()
        var hasLocationPermission by remember { mutableStateOf(false) }

        // Request GPS location Android permissions
        hasLocationPermission = requestLocationPermission()
        if (hasLocationPermission) CheckGPSEnable()

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
            } ?: run { mapError = true }
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

    // -- Wave polygons -------------------------------------------------------

    fun updateWavePolygons(context: Context, wavePolygons: List<Polygon>, clearPolygons: Boolean) {
        (context as? AppCompatActivity)?.runOnUiThread {
            mapViewState?.getMapAsync { map ->
                map.getStyle { style ->
                    val sourceId = "wave-polygons-source"
                    val layerId = "wave-polygons-layer"

                    try {
                        if (clearPolygons) {
                            style.removeLayer(layerId)
                            style.removeSource(sourceId)
                        }

                        // Create or update the source with new polygons
                        val geoJsonSource = style.getSourceAs(sourceId) ?: GeoJsonSource(sourceId)

                        geoJsonSource.setGeoJson(FeatureCollection.fromFeatures(wavePolygons.map {
                            Feature.fromGeometry(it)
                        }))
                        if (style.getSource(sourceId) == null) {
                            style.addSource(geoJsonSource)
                        }

                        // FIXME
//                        if (clearPolygons) {
//                            style.removeLayer(layerId)
//                            style.removeSource(sourceId)
//                        } else {
//                            // If not clearing, add new polygons to existing ones
//                            val existingFeatures = geoJsonSource.querySourceFeatures(null).mapNotNull { it.geometry() as? Polygon }
//                            val combinedPolygons = existingFeatures + wavePolygons
//                            geoJsonSource.setGeoJson(FeatureCollection.fromFeatures(combinedPolygons.map { Feature.fromGeometry(it) }))
//                        }

                        if (style.getSource(sourceId) == null) {
                            style.addSource(geoJsonSource)
                        }

                        // Create or update the layer
                        if (style.getLayer(layerId) == null) {
                            val fillLayer = FillLayer(layerId, sourceId).withProperties(
                                PropertyFactory.fillColor("#D33682".toColorInt()),
                                PropertyFactory.fillOpacity(0.5f)
                            )
                            style.addLayer(fillLayer)
                        }
                    } catch (e: Exception) {
                        Log.e("MapUpdate", "Error updating wave polygons", e)
                    }
                }
            }
        }
    }

    // -- Map camera managers -------------------------------------------------

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
     * Animates the MapLibre camera to the center of the event map area
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
            val bbox = event.area.bbox()

            // Maximize the view to the map // FIXME: move to shared
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
                Quad(sw.lat, ne.lat, centerLng - lngDiff, centerLng + lngDiff)
            } else {
                val latDiff = eventMapWidth / mapLibreAspectRatio / 2
                Quad(centerLat - latDiff, centerLat + latDiff, sw.lng, ne.lng)
            }

            val bounds = LatLngBounds.Builder()
                .include(LatLng(newSwLat, newSwLng))
                .include(LatLng(newNeLat, newNeLng))
                .build()

            val mapConstraints = MapConstraints(bbox)
            // mapConstraints.applyConstraints(map)

            map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds,0),
                object: CancelableCallback {
                    override fun onFinish() {
                        // Set the min/max camera zoom level
                        mapMinZoomLevel = map.cameraPosition.zoom
                        map.setMinZoomPreference(map.cameraPosition.zoom)
                        map.setMaxZoomPreference(event.map.maxZoom)

                        mapConstraints.applyConstraints(map)
                        map.addOnCameraIdleListener {
                            mapConstraints.constrainCamera(map)
                        }

                        onCameraPositionSet?.invoke()
                    }
                    override fun onCancel() {}
                }
            )
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
                event.area.bbox().toLatLngBounds(), 0
            ),
            object : CancelableCallback {
                override fun onFinish() {
                    mapMinZoomLevel = map.cameraPosition.zoom
                    onCameraPositionSet?.invoke()
                }
                override fun onCancel() {}
            }
        )
    }

    // -- Target user ans wave ------------------------------------------------

    /**
     * Moves the camera to the current wave longitude with proper animation handling
     */
    fun targetWave(uiScope: CoroutineScope) {
        var animationInProgress = false

        mapViewState?.getMapAsync { map ->
            if (animationInProgress) return@getMapAsync

            animationInProgress = true

            uiScope.launch {
                try {
                    // Calculate wave position in a structured way
                    val waveBbox = event.area.bbox()
                    val closestWaveLongitude = event.wave.userClosestWaveLongitude()

                    if (closestWaveLongitude != null) {
                        val currentCameraPosition = map.cameraPosition
                        val currentMapLatitude = currentCameraPosition.target?.latitude
                            ?: ((waveBbox.ne.lat + waveBbox.sw.lat) / 2.0)
                        val waveLatLng = LatLng(currentMapLatitude, closestWaveLongitude)

                        val cameraPosition = CameraPosition.Builder()
                            .target(waveLatLng)
                            .zoom(CONST_MAPLIBRE_TARGET_WAVE_ZOOM)
                            .build()

                        withContext(Dispatchers.Main) {
                            map.animateCamera(
                                CameraUpdateFactory.newCameraPosition(cameraPosition),
                                500, // Animation duration in milliseconds
                                object : CancelableCallback {
                                    override fun onFinish() {
                                        animationInProgress = false
                                        // Force a final camera update to ensure position is correct
                                        map.moveCamera(
                                            CameraUpdateFactory.newCameraPosition(
                                                cameraPosition
                                            )
                                        )
                                    }

                                    override fun onCancel() {
                                        Log.d("EventMap", "targetWave animation canceled")
                                        animationInProgress = false
                                    }
                                }
                            )
                        }
                    }
                } catch (e: Exception) {
                    Log.e("EventMap", "Error in targetWave: ${e.message}")
                    animationInProgress = false
                }
            }
        }
    }

    /**
     * Moves the camera to the last known user's position with proper animation handling
     */
    fun targetUser(uiScope: CoroutineScope) {
        var animationInProgress = false

        mapViewState?.getMapAsync { map ->
            if (animationInProgress) return@getMapAsync

            val location = lastLocation ?: return@getMapAsync

            animationInProgress = true

            val userLatLng = LatLng(location.latitude, location.longitude)
            val cameraPosition = CameraPosition.Builder()
                .target(userLatLng)
                .zoom(CONST_MAPLIBRE_TARGET_USER_ZOOM)
                .build()

            uiScope.launch {
                try {
                    withContext(Dispatchers.Main) {
                        map.animateCamera(
                            CameraUpdateFactory.newCameraPosition(cameraPosition),
                            500, // Animation duration in milliseconds
                            object : CancelableCallback {
                                override fun onFinish() {
                                    animationInProgress = false
                                    // Force a final camera update to ensure position is correct
                                    map.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
                                }

                                override fun onCancel() {
                                    Log.d("EventMap", "targetUser animation canceled")
                                    animationInProgress = false
                                }
                            }
                        )
                    }
                } catch (e: Exception) {
                    Log.e("EventMap", "Error in targetUser: ${e.message}")
                    animationInProgress = false
                }
            }
        }
    }

    /**
     * Moves the camera to show both the user and wave positions with proper positioning:
     * - Both positions are always visible in the current window
     * - View is centered between user and wave
     * - User is positioned at 1/3 of the window, wave at 2/3 (horizontally)
     * - Respects minimum and maximum zoom levels
     */
    fun targetUserAndWave(uiScope: CoroutineScope) {
        var animationInProgress = false

        mapViewState?.getMapAsync { map ->
            if (animationInProgress) return@getMapAsync

            val location = lastLocation ?: return@getMapAsync

            uiScope.launch {
                // Get the event area bounds
                val bbox = event.area.bbox()

                // Get wave position
                val closestWaveLongitude = event.wave.userClosestWaveLongitude() ?: return@launch

                animationInProgress = true

                // Create LatLng objects for user and wave
                val userLatLng = LatLng(location.latitude, location.longitude)
                val waveLatLng = LatLng(location.latitude, closestWaveLongitude)

                // Create bounds with just the two actual points
                val bounds = LatLngBounds.Builder()
                    .include(userLatLng)
                    .include(waveLatLng)
                    .build()

                val screenWidth = map.width
                val screenHeight = map.height
                val bboxWidth = bbox.ne.lng - bbox.sw.lng
                val bboxHeight = bbox.ne.lat - bbox.sw.lat

                // Default padding (initial values)
                val horizontalPadding = (bboxWidth * 0.3) // 30% padding horizontally
                val verticalPadding = (bboxHeight * 0.2) // 20% padding vertically

                val distanceLeft = bounds.getLonWest() - bbox.sw.lng
                val paddingLeft = minOf(horizontalPadding, distanceLeft)

                val distanceRight = bbox.ne.lng - bounds.getLonEast()
                val paddingRight = minOf(horizontalPadding, distanceRight)

                val distanceTop = bbox.ne.lat - bounds.getLatNorth()
                val paddingTop = minOf(verticalPadding, distanceTop)

                val distanceBottom = bounds.getLatSouth() - bbox.sw.lat
                val paddingBottom = minOf(verticalPadding, distanceBottom)

                val cameraUpdate = CameraUpdateFactory.newLatLngBounds(
                    bounds,
                    ((maxOf(paddingLeft, 0.0) / bboxWidth) * screenWidth).toInt(), // left
                    ((maxOf(paddingTop, 0.0) / bboxHeight) * screenHeight).toInt(), // top
                    ((maxOf(paddingRight, 0.0) / bboxWidth) * screenWidth).toInt(), // right
                    ((maxOf(paddingBottom, 0.0)/ bboxHeight) * screenHeight).toInt() // bottom
                )

                map.animateCamera(cameraUpdate, 500,
                    object : CancelableCallback {
                        override fun onFinish() {
                            animationInProgress = false
                        }
                        override fun onCancel() {
                            Log.d("EventMap", "targetUserAndWave animation canceled")
                            animationInProgress = false
                        }
                    }
                )
            }
        }
    }

    // -- Location marker builders  -------------------------------------------

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

        // Allow the wave to know the current location of the user
        event.wave.setPositionRequester {
            lastLocation?.let { Position(it.latitude, it.longitude) }
        }
    }

    /**
     * Builds `LocationComponentActivationOptions` for configuring the Mapbox location component.
     *
     * This function sets up the location component enabling a
     * pulsing animation around the user location.
     *
     */
    /**
     * Builds `LocationComponentActivationOptions` for configuring the Mapbox location component.
     *
     * This function sets up the location component enabling a
     * pulsing animation around the user location.
     *
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
            .useDefaultLocationEngine(false)
            .locationEngine(LocationEngineProxy( // Manage with location simulation
                WWWSimulationEnabledLocationEngine(context, platform)
            ))
            .locationEngineRequest(buildLocationEngineRequest())
            .build()
    }

    /**
     * Builds a `LocationEngineRequest` for location updates.
     *
     */
    /**
     * Builds a `LocationEngineRequest` for location updates.
     *
     */
    private fun buildLocationEngineRequest(): LocationEngineRequest =
        LocationEngineRequest.Builder(CONST_TIMER_GPS_UPDATE.inWholeMilliseconds)
            .setFastestInterval(CONST_TIMER_GPS_UPDATE.inWholeMilliseconds / 2)
            .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
            .build()

    // -- Use the MapLibre MapView as a composable --------------------------------

    /**
     * Remembers a MapView and gives it the lifecycle of the current LifecycleOwner
     * source : https://gist.github.com/PiotrPrus/d65378c36b0a0c744e647946f344103c
     */
    /**
     * Remembers a MapView and gives it the lifecycle of the current LifecycleOwner
     * source : https://gist.github.com/PiotrPrus/d65378c36b0a0c744e647946f344103c
     */
    @Composable
    fun rememberMapViewWithLifecycle(): MapView {
        val context = LocalContext.current

        // Build the MapLibre view
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

        MapLibre.getInstance(context) // Required by the API

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

// --------------------------------------------------------------------------------

/**
 * Constrains the map view to a given bounding box
 */
class MapConstraints(private val mapBounds: BoundingBox) {

    private var constraintBounds: LatLngBounds? = null
    private var visibleRegionPadding = LatLngPadding()
    private var constraintsApplied = false

    private data class LatLngPadding(
        var latPadding: Double = 0.0,
        var lngPadding: Double = 0.0
    )

    fun applyConstraints(map: MapLibreMap) {
        // Always recalculate constraints based on current zoom level
        updateVisibleRegionPadding(map)
        applyConstraintsWithPadding(map)

        // Set up camera movement listener to update constraints on zoom changes
        map.addOnCameraIdleListener {
            val newPadding = calculateVisibleRegionPadding(map)
            if (hasSignificantPaddingChange(newPadding)) {
                visibleRegionPadding = newPadding
                applyConstraintsWithPadding(map)
            }
        }

        constraintsApplied = true
    }

    private fun applyConstraintsWithPadding(map: MapLibreMap) {
        // Calculate padded bounds - SHRINK the allowed area by the visible region dimensions
        // This ensures corners of the map can't go past screen edges
        val paddedBounds = LatLngBounds.Builder()
            .include(LatLng(
                mapBounds.sw.lat + visibleRegionPadding.latPadding,
                mapBounds.sw.lng + visibleRegionPadding.lngPadding
            ))
            .include(LatLng(
                mapBounds.ne.lat - visibleRegionPadding.latPadding,
                mapBounds.ne.lng - visibleRegionPadding.lngPadding
            ))
            .build()

        constraintBounds = paddedBounds

        // Apply constraints to the map
        map.setLatLngBoundsForCameraTarget(paddedBounds)

        // Also set min/max zoom
        val minZoom = map.minZoomLevel
        map.setMinZoomPreference(minZoom)

        // Check if our constrained area is too small for the current view
        if (!isValidBounds(paddedBounds, map)) {
            // If bounds are too small, calculate safer bounds centered around current position
            fitMapToBounds(map, calculateSafeBounds(map))
        }
    }

    private fun isValidBounds(bounds: LatLngBounds, map: MapLibreMap): Boolean {
        // Get current camera position
        val target = map.cameraPosition.target ?: return false

        // Check if bounds are valid (not inverted or too small)
        val validSize = bounds.getLatNorth() > bounds.getLatSouth() &&
                bounds.getLonEast() > bounds.getLonWest() &&
                (bounds.getLatNorth() - bounds.getLatSouth()) > visibleRegionPadding.latPadding * 0.1 &&
                (bounds.getLonEast() - bounds.getLonWest()) > visibleRegionPadding.lngPadding * 0.1

        // Check if the current position is within or close to the bounds
        val containsTarget = bounds.contains(target) ||
                isNearBounds(target, bounds, visibleRegionPadding.latPadding * 0.5,
                    visibleRegionPadding.lngPadding * 0.5)

        return validSize && containsTarget
    }

    /**
     * Checks if a point is near the bounds within the specified padding.
     */
    private fun isNearBounds(point: LatLng, bounds: LatLngBounds, latPadding: Double, lngPadding: Double): Boolean {
        val nearLat = point.latitude >= bounds.getLatSouth() - latPadding &&
                point.latitude <= bounds.getLatNorth() + latPadding
        val nearLng = point.longitude >= bounds.getLonWest() - lngPadding &&
                point.longitude <= bounds.getLonEast() + lngPadding
        return nearLat && nearLng
    }

    /**
     * Calculates safe bounds that are centered around the current camera position
     * while still ensuring they are valid and within the overall map bounds.
     */
    private fun calculateSafeBounds(map: MapLibreMap): LatLngBounds {
        // Get current camera position
        val currentPosition = map.cameraPosition.target
            ?: return LatLngBounds.Builder()
                .include(LatLng(mapBounds.sw.lat, mapBounds.sw.lng))
                .include(LatLng(mapBounds.ne.lat, mapBounds.ne.lng))
                .build()

        // If no position, return original bounds

        // Calculate how much space we need (similar to visible region padding)
        val neededLatPadding = visibleRegionPadding.latPadding * 1.5 // Add extra margin
        val neededLngPadding = visibleRegionPadding.lngPadding * 1.5

        // Calculate map dimensions
        val mapLatSpan = mapBounds.ne.lat - mapBounds.sw.lat
        val mapLngSpan = mapBounds.ne.lng - mapBounds.sw.lng

        // Make sure our needed padding isn't larger than the map itself
        val usableLatPadding = min(neededLatPadding, mapLatSpan * 0.4) // Max 40% of map height
        val usableLngPadding = min(neededLngPadding, mapLngSpan * 0.4) // Max 40% of map width

        // Try to center the bounds around the current position
        var centerLat = currentPosition.latitude
        var centerLng = currentPosition.longitude

        // But constrain the center to be within the map bounds (with padding)
        centerLat = centerLat.coerceIn(
            mapBounds.sw.lat + usableLatPadding,
            mapBounds.ne.lat - usableLatPadding
        )
        centerLng = centerLng.coerceIn(
            mapBounds.sw.lng + usableLngPadding,
            mapBounds.ne.lng - usableLngPadding
        )

        // Calculate the corners of our safe bounds
        val safeSouth = max(mapBounds.sw.lat, centerLat - usableLatPadding)
        val safeNorth = min(mapBounds.ne.lat, centerLat + usableLatPadding)
        val safeWest = max(mapBounds.sw.lng, centerLng - usableLngPadding)
        val safeEast = min(mapBounds.ne.lng, centerLng + usableLngPadding)

        // Ensure the bounds have minimum size (at least 20% of padding)
        val minLatSpan = visibleRegionPadding.latPadding * 0.2
        val minLngSpan = visibleRegionPadding.lngPadding * 0.2

        val finalNorth = max(safeNorth, safeSouth + minLatSpan)
        val finalEast = max(safeEast, safeWest + minLngSpan)

        // Build and return the safe bounds
        return LatLngBounds.Builder()
            .include(LatLng(safeSouth, safeWest))
            .include(LatLng(finalNorth, finalEast))
            .build()
    }

    private fun updateVisibleRegionPadding(map: MapLibreMap) {
        visibleRegionPadding = calculateVisibleRegionPadding(map)
    }

    private fun calculateVisibleRegionPadding(map: MapLibreMap): LatLngPadding {
        // Get the visible region from the current map view
        val visibleRegion = map.projection.visibleRegion

        // Calculate padding as half the visible region dimensions
        val latPadding = (visibleRegion.latLngBounds.getLatNorth() -
                visibleRegion.latLngBounds.getLatSouth()) / 2.0
        val lngPadding = (visibleRegion.latLngBounds.getLonEast() -
                visibleRegion.latLngBounds.getLonWest()) / 2.0

        return LatLngPadding(latPadding, lngPadding)
    }

    private fun hasSignificantPaddingChange(newPadding: LatLngPadding): Boolean {
        // Determine if padding change is significant enough to update constraints
        val latChange = abs(newPadding.latPadding - visibleRegionPadding.latPadding) /
                visibleRegionPadding.latPadding
        val lngChange = abs(newPadding.lngPadding - visibleRegionPadding.lngPadding) /
                visibleRegionPadding.lngPadding

        return latChange > 0.1 || lngChange > 0.1 // 10% change threshold
    }

    private fun fitMapToBounds(map: MapLibreMap, bounds: LatLngBounds) {
        // Adjust camera to fit bounds
        val cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, 0)
        map.moveCamera(cameraUpdate)
    }

    fun constrainCamera(map: MapLibreMap) {
        val position = map.cameraPosition
        if (constraintBounds != null && !isCameraWithinConstraints(position)) {
            val nearestValid = position.target?.let { getNearestValidPoint(it, constraintBounds!!) }
            nearestValid?.let { CameraUpdateFactory.newLatLng(it) }?.let { map.animateCamera(it) }
        }
    }

    private fun isCameraWithinConstraints(cameraPosition: CameraPosition): Boolean =
        cameraPosition.target?.let { constraintBounds?.contains(it) } ?: true

    // Helper to find nearest valid point within bounds
    private fun getNearestValidPoint(point: LatLng, bounds: LatLngBounds): LatLng {
        val lat = point.latitude.coerceIn(bounds.getLatSouth(), bounds.getLatNorth())
        val lng = point.longitude.coerceIn(bounds.getLonWest(), bounds.getLonEast())
        return LatLng(lat, lng)
    }

}

// --------------------------------------------------------------------------------

/**
 * Creates a `LifecycleEventObserver` that synchronizes the lifecycle of a `MapView` with the
 * lifecycle of a `LifecycleOwner`.
 *
 * This observer ensures that the `MapView` receives the corresponding lifecycle events
 * (`onCreate`, `onStart`, `onResume`, `onPause`, `onStop`, `onDestroy`) as the
 * `LifecycleOwner` transitions through its lifecycle states.
 *
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

