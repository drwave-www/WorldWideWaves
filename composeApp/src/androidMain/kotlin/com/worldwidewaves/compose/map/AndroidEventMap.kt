package com.worldwidewaves.compose.map

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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.worldwidewaves.map.AndroidMapLibreAdapter
import com.worldwidewaves.shared.WWWGlobals.Companion.CONST_TIMER_GPS_UPDATE
import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.events.utils.Position
import com.worldwidewaves.shared.generated.resources.map_error
import com.worldwidewaves.shared.map.AbstractEventMap
import com.worldwidewaves.shared.map.EventMapConfig
import com.worldwidewaves.shared.map.LocationProvider
import com.worldwidewaves.shared.map.MapCameraPosition
import com.worldwidewaves.theme.extendedLight
import com.worldwidewaves.utils.AndroidLocationProvider
import com.worldwidewaves.utils.CheckGPSEnable
import com.worldwidewaves.utils.requestLocationPermission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.painterResource
import org.koin.core.component.KoinComponent
import org.koin.java.KoinJavaComponent.inject
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
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
class AndroidEventMap(
    event: IWWWEvent,
    private val onMapLoaded: () -> Unit = {},
    onLocationUpdate: (Position) -> Unit = {},
    private val onMapClick: (() -> Unit)? = null,
    mapConfig: EventMapConfig = EventMapConfig()
) : KoinComponent, AbstractEventMap(event, mapConfig, onLocationUpdate) {

    // Overrides properties from AbstractEventMap
    override val locationProvider: LocationProvider by inject(AndroidLocationProvider::class.java)
    override val mapLibreAdapter: AndroidMapLibreAdapter by lazy { AndroidMapLibreAdapter() }

    /**
     * The Compose UI for the map
     */
    @Composable
    fun Screen(modifier: Modifier) {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val mapLibreView: MapView = rememberMapLibreViewWithLifecycle()

        var isMapLoaded by remember { mutableStateOf(false) }
        var mapError by remember { mutableStateOf(false) }
        var hasLocationPermission by remember { mutableStateOf(false) }

        // Request GPS location Android permissions
        hasLocationPermission = requestLocationPermission()
        if (hasLocationPermission) CheckGPSEnable()

        // Setup Map Style and properties, initialize the map view
        LaunchedEffect(Unit) {
            withContext(Dispatchers.IO) { // IO actions
                event.map.getStyleUri()?.let {
                    val uri = Uri.fromFile(File(it))

                    scope.launch { // UI actions
                        mapLibreView.getMapAsync { map ->
                            map.setStyle(Style.Builder().fromUri(uri.toString())) { style ->
                                map.uiSettings.setAttributionMargins(0, 0, 0, 0)

                                // Provide Adapter with Android MapLibre instance
                                mapLibreAdapter.setMap(map)

                                // Initialize location provider if we have permission
                                if (hasLocationPermission) {
                                    setupMapLocationComponent(map, context, style)
                                }

                                // Initialize view and setup listeners
                                setupMap(
                                    scope, map.width.toDouble(), map.height.toDouble(),
                                    onMapLoaded = {
                                        onMapLoaded()
                                        isMapLoaded = true // Map is loaded
                                    },
                                    onMapClick = { _, _ ->
                                        onMapClick?.invoke()
                                    }
                                )

                            }
                        }
                    }
                } ?: run {
                    mapError = true
                }
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

            // LibreMap as Android Composable
            AndroidView(
                factory = { mapLibreView },
                modifier = Modifier.fillMaxSize().alpha(if (isMapLoaded) 1f else 0f)
            )
        }
    }

    // ------------------------------------------------------------------------

    /**
     * Sets up the Android location component
     */
    @SuppressLint("MissingPermission")
    private fun setupMapLocationComponent(map: MapLibreMap, context: Context, style: Style) {
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

    // ------------------------------------------------------------------------

    /**
     * Remembers a MapView and gives it the lifecycle of the current LifecycleOwner
     */
    @Composable
    fun rememberMapLibreViewWithLifecycle(): MapView {
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

    // ------------------------------------------------------------------------

    /**
     * Update wave polygons on the map
     */
    fun updateWavePolygons(context: Context, wavePolygons: List<Polygon>, clearPolygons: Boolean) {
        (context as? AppCompatActivity)?.runOnUiThread {
            mapLibreAdapter.addWavePolygons(wavePolygons, clearPolygons)
        }
    }
}

// ----------------------------------------------------------------------------

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