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
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.annotation.UiThread
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
import androidx.compose.runtime.collectAsState
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.worldwidewaves.map.MapLibreAdapter
import com.worldwidewaves.shared.WWWGlobals.Companion.CONST_TIMER_GPS_UPDATE
import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.events.utils.BoundingBox
import com.worldwidewaves.shared.events.utils.Position
import com.worldwidewaves.shared.generated.resources.map_error
import com.worldwidewaves.shared.map.AbstractEventMap
import com.worldwidewaves.shared.map.EventMapConfig
import com.worldwidewaves.shared.map.LocationProvider
import com.worldwidewaves.shared.map.MapCameraPosition
import com.worldwidewaves.shared.map.MapConstraintManager
import com.worldwidewaves.theme.extendedLight
import com.worldwidewaves.utils.AndroidLocationProvider
import com.worldwidewaves.utils.CheckGPSEnable
import com.worldwidewaves.utils.requestLocationPermission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.painterResource
import org.koin.core.component.KoinComponent
import org.koin.java.KoinJavaComponent.inject
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.LocationComponentOptions
import org.maplibre.android.location.engine.LocationEngineProxy
import org.maplibre.android.location.engine.LocationEngineRequest
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapLibreMapOptions
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.geojson.Polygon
import java.io.File
import com.worldwidewaves.shared.generated.resources.Res as ShRes

/**
 * Android-specific implementation of the EventMap
 */
class EventMap(
    event: IWWWEvent,
    private val onMapLoaded: () -> Unit = {},
    onLocationUpdate: (Position) -> Unit = {},
    private val onMapClick: (() -> Unit)? = null,
    mapConfig: EventMapConfig = EventMapConfig()
) : KoinComponent, AbstractEventMap(event, mapConfig, onLocationUpdate) {

    // Overrides properties from AbstractEventMap
    override val locationProvider: LocationProvider by inject(AndroidLocationProvider::class.java)
    override val platformMap: MapLibreAdapter by lazy { MapLibreAdapter() }

    /**
     * The Compose UI for the map
     */
    @Composable
    fun Screen(modifier: Modifier) {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val mapView: MapView = rememberMapViewWithLifecycle()

        val isMapLoaded by platformMap.isLoaded.collectAsState()
        val mapError by platformMap.isError.collectAsState()
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
                    platformMap.setMap(map)

                    map.setStyle(Style.Builder().fromUri(uri.toString())) { style ->
                        map.uiSettings.setAttributionMargins(0, 0, 0, 0)

                        // Initialize location provider if we have permission
                        if (hasLocationPermission) {
                            setupAndroidLocationComponent(map, context, style)
                        }

                        // Initialize view and setup listeners
                        setupMap(scope,
                            onMapLoaded = {
                                onMapLoaded()
                                platformMap.setLoaded(true)
                            },
                            onMapClick = { _, _ ->
                                onMapClick?.invoke()
                            }
                        )

                        // Update adapter with initial camera position
                        platformMap.updateCameraInfo(map)

                        // Set camera movement listener to update position
                        map.addOnCameraIdleListener {
                            platformMap.updateCameraInfo(map)
                            platformMap.constrainCamera()
                        }

                    }
                }
            } ?: run {
                platformMap.setError(true)
            }
        }

        // The map view
        BoxWithConstraints(modifier = modifier) {
            if (!isMapLoaded) {
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
                    .alpha(if (isMapLoaded) 1f else 0f)
            )
        }
    }

    /**
     * Sets up the Android location component
     */
    @SuppressLint("MissingPermission")
    private fun setupAndroidLocationComponent(map: MapLibreMap, context: Context, style: Style) {
        // Activate location component
        map.locationComponent.activateLocationComponent(
            buildLocationComponentActivationOptions(context, style)
        )
        map.locationComponent.isLocationComponentEnabled = true
        map.locationComponent.cameraMode = CameraMode.NONE // Do not track user
    }

    /**
     * Builds LocationComponentActivationOptions for configuring the MapLibre location component
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
            .locationEngine(LocationEngineProxy((locationProvider as AndroidLocationProvider).locationEngine))
            .locationEngineRequest(buildLocationEngineRequest())
            .build()
    }

    /**
     * Builds a LocationEngineRequest for location updates
     */
    private fun buildLocationEngineRequest(): LocationEngineRequest =
        LocationEngineRequest.Builder(CONST_TIMER_GPS_UPDATE.inWholeMilliseconds)
            .setFastestInterval(CONST_TIMER_GPS_UPDATE.inWholeMilliseconds / 2)
            .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
            .build()

    /**
     * Remembers a MapView and gives it the lifecycle of the current LifecycleOwner
     */
    @Composable
    fun rememberMapViewWithLifecycle(): MapView {
        val context = LocalContext.current

        // Build the MapLibre view
        val maplibreMapOptions = MapLibreMapOptions.createFromAttributes(context)
        maplibreMapOptions.apply {
            camera(CameraPosition.Builder()
                .padding(0.0, 0.0, 0.0, 0.0)
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

    /**
     * Update wave polygons on the map
     */
    fun updateWavePolygons(context: Context, wavePolygons: List<Polygon>, clearPolygons: Boolean) {
        (context as? AppCompatActivity)?.runOnUiThread {
            platformMap.addWavePolygons(wavePolygons, clearPolygons)
        }
    }
}

/**
 * MapLibre-specific implementation of map constraints
 */
class MapLibreConstraintHandler(mapBounds: BoundingBox) {
    private val constraintManager = MapConstraintManager(mapBounds)
    private var constraintBounds: LatLngBounds? = null
    private var constraintsApplied = false

    /**
     * Apply constraints to a MapLibre map
     */
    @UiThread
    fun applyConstraints(map: MapLibreMap) {
        // Calculate visible region padding from the map
        updateVisibleRegionPadding(map)

        // Apply the constraints with the current padding
        applyConstraintsWithPadding(map)

        // Set up camera movement listener to update constraints on zoom changes
        map.addOnCameraIdleListener {
            val newPadding = calculateVisibleRegionPadding(map)

            if (constraintManager.hasSignificantPaddingChange(
                    MapConstraintManager.VisibleRegionPadding(newPadding.latPadding, newPadding.lngPadding)
                )) {
                constraintManager.setVisibleRegionPadding(
                    MapConstraintManager.VisibleRegionPadding(newPadding.latPadding, newPadding.lngPadding)
                )
                applyConstraintsWithPadding(map)
            }
        }

        constraintsApplied = true
    }

    /**
     * Apply constraints based on the current padding
     */
    private fun applyConstraintsWithPadding(map: MapLibreMap) {
        try {
            // Get platform-independent bounds from the constraint manager
            val paddedBounds = constraintManager.calculateConstraintBounds()

            // Convert to MapLibre LatLngBounds
            val latLngBounds = convertToLatLngBounds(paddedBounds)
            constraintBounds = latLngBounds

            // Apply constraints to the map
            map.setLatLngBoundsForCameraTarget(latLngBounds)

            // Also set min zoom
            val minZoom = map.minZoomLevel
            map.setMinZoomPreference(minZoom)

            // Check if our constrained area is too small for the current view
            val currentPosition = map.cameraPosition.target?.let { Position(it.latitude, it.longitude) }

            if (!constraintManager.isValidBounds(paddedBounds, currentPosition)) {
                // If bounds are too small, calculate safer bounds centered around current position
                currentPosition?.let {
                    val safeBounds = constraintManager.calculateSafeBounds(it)
                    fitMapToBounds(map, convertToLatLngBounds(safeBounds))
                }
            }
        } catch (e: Exception) {
            Log.e("MapConstraints", "Error applying constraints: ${e.message}")
        }
    }

    /**
     * Constrain the camera to the valid bounds if needed
     */
    fun constrainCamera(map: MapLibreMap) {
        val position = map.cameraPosition
        if (constraintBounds != null && !isCameraWithinConstraints(position)) {
            position.target?.let { target ->
                val mapPosition = Position(target.latitude, target.longitude)
                val constraintBoundsMapped = constraintBounds?.let { bounds ->
                    BoundingBox(
                        Position(bounds.getLatSouth(), bounds.getLonWest()),
                        Position(bounds.getLatNorth(), bounds.getLonEast())
                    )
                }

                constraintBoundsMapped?.let { bounds ->
                    val nearestValid = constraintManager.getNearestValidPoint(mapPosition, bounds)
                    val latLng = LatLng(nearestValid.latitude, nearestValid.longitude)
                    map.animateCamera(CameraUpdateFactory.newLatLng(latLng))
                }
            }
        }
    }

    /**
     * Checks if camera is within the constraint bounds
     */
    private fun isCameraWithinConstraints(cameraPosition: CameraPosition): Boolean =
        cameraPosition.target?.let { constraintBounds?.contains(it) } ?: true

    /**
     * Moves the camera to fit specified bounds
     */
    private fun fitMapToBounds(map: MapLibreMap, bounds: LatLngBounds) {
        val cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, 0)
        map.moveCamera(cameraUpdate)
    }

    /**
     * Updates visible region padding from the map
     */
    private fun updateVisibleRegionPadding(map: MapLibreMap) {
        val padding = calculateVisibleRegionPadding(map)
        constraintManager.setVisibleRegionPadding(
            MapConstraintManager.VisibleRegionPadding(padding.latPadding, padding.lngPadding)
        )
    }

    /**
     * Calculates visible region padding from the map projection
     */
    private data class MapLibrePadding(
        val latPadding: Double,
        val lngPadding: Double
    )

    private fun calculateVisibleRegionPadding(map: MapLibreMap): MapLibrePadding {
        // Get the visible region from the current map view
        val visibleRegion = map.projection.visibleRegion

        // Calculate padding as half the visible region dimensions
        val latPadding = (visibleRegion.latLngBounds.getLatNorth() -
                visibleRegion.latLngBounds.getLatSouth()) / 2.0
        val lngPadding = (visibleRegion.latLngBounds.getLonEast() -
                visibleRegion.latLngBounds.getLonWest()) / 2.0

        return MapLibrePadding(latPadding, lngPadding)
    }

    /**
     * Converts platform-independent MapBounds to MapLibre LatLngBounds
     */
    private fun convertToLatLngBounds(bounds: BoundingBox): LatLngBounds {
        return LatLngBounds.Builder()
            .include(LatLng(bounds.southwest.latitude, bounds.southwest.longitude))
            .include(LatLng(bounds.northeast.latitude, bounds.northeast.longitude))
            .build()
    }
}

/**
 * Creates a `LifecycleEventObserver` that synchronizes the lifecycle of a `MapView`
 * with the lifecycle of a `LifecycleOwner`.
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