package com.worldwidewaves.compose.map

/*
 * Copyright 2025 DrWave
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
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.play.core.splitcompat.SplitCompat
import com.worldwidewaves.compose.DownloadProgressIndicator
import com.worldwidewaves.compose.ErrorMessage
import com.worldwidewaves.compose.LoadingIndicator
import com.worldwidewaves.map.AndroidMapLibreAdapter
import com.worldwidewaves.shared.MokoRes
import com.worldwidewaves.shared.WWWGlobals.Companion.CONST_TIMER_GPS_UPDATE
import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.events.utils.Position
import com.worldwidewaves.shared.map.AbstractEventMap
import com.worldwidewaves.shared.map.EventMapConfig
import com.worldwidewaves.shared.map.MapCameraPosition
import com.worldwidewaves.shared.map.WWWLocationProvider
import com.worldwidewaves.utils.AndroidWWWLocationProvider
import com.worldwidewaves.utils.CheckGPSEnable
import com.worldwidewaves.utils.MapAvailabilityChecker
import com.worldwidewaves.utils.requestLocationPermission
import com.worldwidewaves.viewmodels.MapFeatureState
import com.worldwidewaves.viewmodels.MapViewModel
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.DrawableResource
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

/**
 * Android implementation of the shared **EventMap** adapter.
 *
 * Bridges the platform-agnostic map layer defined in the shared module with the
 * MapLibre Android SDK by:
 * • Loading the per-event style **URI** produced by [IWWWEvent.map]  
 * • Instantiating / configuring the underlying `MapView` & `MapLibreMap`
 *   (gestures, camera bounds, location component, click callbacks)  
 * • Delegating tile/style generation & map-specific validation to
 *   [com.worldwidewaves.shared.events.WWWEventMap] in the shared code while exposing a Compose `Screen`
 *   convenience wrapper for UI layers.  
 *
 * All logic that can stay platform-agnostic (camera enums, location providers,
 * polygon updates…) lives in [AbstractEventMap]; this class only contains the
 * Android-specific glue code required to render and interact with the map on
 * the device.
 */
class AndroidEventMap(
    event: IWWWEvent,
    private val onMapLoaded: () -> Unit = {},
    onLocationUpdate: (Position) -> Unit = {},
    private val onMapClick: (() -> Unit)? = null,
    mapConfig: EventMapConfig = EventMapConfig()
) : KoinComponent, AbstractEventMap<MapLibreMap>(event, mapConfig, onLocationUpdate) {

    private companion object {
        private const val TAG = "EventMap"
    }

    // Overrides properties from AbstractEventMap
    override val locationProvider: WWWLocationProvider by inject(AndroidWWWLocationProvider::class.java)
    override val mapLibreAdapter: AndroidMapLibreAdapter by lazy { AndroidMapLibreAdapter() }

    /** Holds the last [MapLibreMap] provided by MapView so we can (re-)enable
     *  the location component whenever permission or provider state changes. */
    private var currentMap: MapLibreMap? = null

    // Map availability and download state tracking
    private val mapAvailabilityChecker: MapAvailabilityChecker by inject(MapAvailabilityChecker::class.java)

    /**
     * The Compose UI for the map
     */
    @Composable
    fun Screen(autoMapDownload: Boolean = false, modifier: Modifier) {
        Log.i(TAG, "Screen composable entered: eventId=${event.id}, autoMapDownload=$autoMapDownload")
        
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        var isMapLoaded by remember { mutableStateOf(false) }
        var mapError by remember { mutableStateOf(false) }
        var hasLocationPermission by remember { mutableStateOf(false) }
        var isMapAvailable by remember { mutableStateOf(false) }
        var isMapDownloading by remember { mutableStateOf(false) }
        // Guard to avoid auto-re-download after the user explicitly cancels
        var userCanceled by remember { mutableStateOf(false) }

        // Re-create the MapView whenever availability flips so a fresh
        // split-aware AssetManager is used.
        val mapLibreView: MapView =
            rememberMapLibreViewWithLifecycle(key = "${event.id}-${isMapAvailable}")

        val mapViewModel: MapViewModel = viewModel()
        val mapFeatureState by mapViewModel.featureState.collectAsState()

        // Check if map is downloaded
        LaunchedEffect(Unit) {
            Log.d(TAG, "Initial check for map availability: ${event.id}")
            // Check only – let the user trigger the download via the UI button
            mapViewModel.checkIfMapIsAvailable(event.id, autoDownload = false)
            isMapAvailable = mapAvailabilityChecker.isMapDownloaded(event.id)
            Log.i(TAG, "Initial map availability check result: ${event.id} available=$isMapAvailable")
        }

        // Update download state based on MapViewModel state
        LaunchedEffect(mapFeatureState) {
            Log.d(TAG, "MapFeatureState changed: ${mapFeatureState::class.simpleName}")
            
            when (mapFeatureState) {
                is MapFeatureState.Downloading -> {
                    val progress = (mapFeatureState as MapFeatureState.Downloading).progress
                    Log.d(TAG, "Map download in progress: ${event.id}, progress=$progress%")
                    isMapDownloading = true
                }
                is MapFeatureState.Pending -> {
                    Log.d(TAG, "Map download pending: ${event.id}")
                    isMapDownloading = true
                }
                is MapFeatureState.Installed -> {
                    Log.i(TAG, "Map installed: ${event.id}")
                    // Make the just-installed split immediately visible to this
                    // running Activity – required for MapLibre to see assets.
                    (context as? AppCompatActivity)?.let { 
                        Log.d(TAG, "Installing SplitCompat on Activity after map installation")
                        SplitCompat.installActivity(it) 
                    }
                    isMapDownloading = false
                    isMapAvailable = true
                    mapError = false
                }
                is MapFeatureState.Failed -> {
                    val errorCode = (mapFeatureState as MapFeatureState.Failed).errorCode
                    Log.e(TAG, "Map download failed: ${event.id}, errorCode=$errorCode")
                    mapError = true
                    isMapDownloading = false
                }
                is MapFeatureState.Canceling -> {
                    Log.d(TAG, "Map download canceling: ${event.id}")
                    isMapDownloading = false
                }
                is MapFeatureState.Installing -> {
                    Log.d(TAG, "Map installing: ${event.id}")
                    isMapDownloading = true
                }
                is MapFeatureState.NotAvailable -> {
                    Log.d(TAG, "Map not available: ${event.id}")
                    isMapDownloading = false
                }
                else -> {
                    Log.d(TAG, "Other map state: ${mapFeatureState::class.simpleName}")
                }
            }
        }

        // Request GPS location Android permissions
        hasLocationPermission = requestLocationPermission()
        if (hasLocationPermission) {
            Log.d(TAG, "Location permission granted, checking GPS")
            CheckGPSEnable()
        } else {
            Log.d(TAG, "Location permission not granted")
        }

        /* -----------------------------------------------------------------
         * Re-check permission & GPS provider when activity resumes
         * ----------------------------------------------------------------- */
        val lifecycleOwner = LocalLifecycleOwner.current
        DisposableEffect(lifecycleOwner) {
            Log.d(TAG, "Setting up lifecycle observer for permission checks")
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    Log.d(TAG, "Activity resumed, re-checking location permissions")
                    val granted = isLocationPermissionGranted(context)
                    hasLocationPermission = granted
                    updateLocationComponent(context, granted)
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { 
                Log.d(TAG, "Removing lifecycle observer")
                lifecycleOwner.lifecycle.removeObserver(observer) 
            }
        }

        /* -----------------------------------------------------------------
         * Monitor GPS provider toggles coming from system settings
         * ----------------------------------------------------------------- */
        DisposableEffect(Unit) {
            Log.d(TAG, "Setting up GPS provider change receiver")
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(c: Context?, i: Intent?) {
                    Log.d(TAG, "GPS provider changed, re-checking permissions")
                    val granted = isLocationPermissionGranted(context)
                    hasLocationPermission = granted
                    updateLocationComponent(context, granted)
                }
            }
            val filter = IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION)
            context.registerReceiver(receiver, filter)
            onDispose { 
                Log.d(TAG, "Unregistering GPS provider change receiver")
                context.unregisterReceiver(receiver) 
            }
        }

        // Setup Map Style and properties, initialize the map view if map is available
        LaunchedEffect(isMapAvailable) {
            Log.i(TAG, "Map availability changed: ${event.id}, available=$isMapAvailable")
            if (isMapAvailable) {
                Log.d(TAG, "Starting map load process")
                loadMap(
                    context = context,
                    scope = scope,
                    mapLibreView = mapLibreView,
                    hasLocationPermission = hasLocationPermission,
                    onMapLoaded = {
                        Log.i(TAG, "Map successfully loaded: ${event.id}")
                        isMapLoaded = true
                        onMapLoaded()
                    },
                    onMapError = { 
                        Log.e(TAG, "Error loading map: ${event.id}")
                        mapError = true 
                    }
                )
            } else if (autoMapDownload && !userCanceled) {
                Log.i(TAG, "Auto-downloading map: ${event.id}")
                mapViewModel.downloadMap(event.id)
            }
        }

        // The map view
        Box(modifier = modifier) {
            // Default map image as background
            Image(
                modifier = Modifier.fillMaxSize(),
                painter = painterResource(event.getMapImage() as DrawableResource),
                contentDescription = "defaultMap",
                contentScale = ContentScale.Crop
            )

            // LibreMap as Android Composable - only visible when map is loaded
            AndroidView(
                factory = { mapLibreView },
                modifier = Modifier.fillMaxSize().alpha(if (isMapLoaded) 1f else 0f)
            )

            // Show appropriate UI based on map state
            when {
                isMapLoaded -> {
                    // Map is loaded and visible, show nothing extra
                    Log.d(TAG, "UI state: Map loaded and visible")
                }
                isMapDownloading && !mapError -> {
                    // Semi-transparent overlay for download UI
                    Log.d(TAG, "UI state: Map downloading overlay")
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.background.copy(alpha = 0.8f),
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                when (val state = mapFeatureState) {
                                    is MapFeatureState.Downloading -> {
                                        Log.d(TAG, "Showing download progress: ${state.progress}%")
                                        DownloadProgressIndicator(
                                            progress = state.progress,
                                            message = stringResource(MokoRes.strings.map_downloading),
                                            onCancel = {
                                                Log.i(TAG, "User canceled download: ${event.id}")
                                                userCanceled = true
                                                mapViewModel.cancelDownload()
                                            }
                                        )
                                    }
                                    is MapFeatureState.Pending -> {
                                        Log.d(TAG, "Showing pending download indicator")
                                        LoadingIndicator(message = stringResource(MokoRes.strings.map_starting_download))
                                    }
                                    is MapFeatureState.Retrying -> {
                                        Log.d(TAG, "Showing retry indicator: attempt=${state.attempt}/${state.maxAttempts}")
                                        DownloadProgressIndicator(
                                            message = "${stringResource(MokoRes.strings.map_retrying_download)} (${state.attempt}/${state.maxAttempts})...",
                                            onCancel = {
                                                Log.i(TAG, "User canceled retry download: ${event.id}")
                                                isMapDownloading = false
                                                userCanceled = true
                                                mapViewModel.cancelDownload()
                                            }
                                        )
                                    }
                                    is MapFeatureState.Installing -> {
                                        Log.d(TAG, "Showing installing indicator")
                                        DownloadProgressIndicator(
                                            message = "Installing..",
                                            onCancel = {
                                                Log.i(TAG, "User canceled install: ${event.id}")
                                                userCanceled = true
                                                mapViewModel.cancelDownload()
                                            }
                                        )
                                    }
                                    else -> {
                                        // Generic loading indicator for other download states
                                        Log.d(TAG, "Showing generic loading indicator")
                                        LoadingIndicator(message = stringResource(MokoRes.strings.map_loading))
                                    }
                                }
                            }
                        }
                    }
                }
                mapError && isMapDownloading -> {
                    // Show error with retry option
                    Log.d(TAG, "UI state: Map download error")
                    Surface(modifier = Modifier.fillMaxSize()) {
                        ErrorMessage(
                            message = stringResource(MokoRes.strings.map_error_download),
                            onRetry = {
                                Log.i(TAG, "User requested retry after error: ${event.id}")
                                mapError = false
                                mapViewModel.downloadMap(event.id)
                            }
                        )
                    }
                }
                !isMapAvailable -> {
                    // Show download button overlay
                    Log.d(TAG, "UI state: Map download button")
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Button(
                            onClick = {
                                Log.i(TAG, "User clicked download button: ${event.id}")
                                // Immediately reflect downloading state for better UX
                                mapError = false
                                isMapDownloading = true
                                userCanceled = false
                                mapViewModel.downloadMap(event.id)
                            },
                            modifier = Modifier.size(width = 200.dp, height = 60.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Text(
                                text = stringResource(MokoRes.strings.map_download),
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------------

    /**
     * Helper function to load the map
     */
    private fun loadMap(
        context: Context,
        scope: CoroutineScope,
        mapLibreView: MapView,
        hasLocationPermission: Boolean,
        onMapLoaded: () -> Unit,
        onMapError: () -> Unit = {}
    ) {
        Log.i(TAG, "loadMap started for event: ${event.id}")
        // Ensure SplitCompat is installed before accessing any dynamic feature assets
        SplitCompat.install(context)
        Log.d(TAG, "SplitCompat.install called on context")
        
        scope.launch {
            withContext(Dispatchers.IO) { // IO actions
                Log.d(TAG, "Attempting to resolve style URI")
                var stylePath: String? = null
                repeat(10) { attempt ->
                    val candidate = event.map.getStyleUri()
                    if (candidate != null && File(candidate).exists()) {
                        Log.i(TAG, "Style URI resolved on attempt ${attempt+1}: $candidate")
                        stylePath = candidate
                        return@repeat
                    }
                    Log.w(TAG, "Style URI resolution attempt ${attempt+1} failed, retrying...")
                    // Give Play-Core/asset manager some time to expose freshly
                    // installed split assets before next try.
                    delay(200)
                }

                if (stylePath == null) {
                    Log.e(TAG, "Failed to resolve style URI after multiple attempts")
                    onMapError()
                    return@withContext
                }

                val uri = Uri.fromFile(File(stylePath!!))
                Log.d(TAG, "Style URI converted to file URI: $uri")
                
                scope.launch { // UI actions
                    Log.d(TAG, "Posting getMapAsync to MapView UI queue")
                    mapLibreView.post {
                        Log.d(TAG, "Invoking getMapAsync now")
                        mapLibreView.getMapAsync { map ->
                            Log.i(TAG, "MapLibreMap instance received")
                            // Save reference so we can refresh location component later
                            currentMap = map
                            // Setup Map
                            Log.d(TAG, "Setting up map with style: $uri")
                            this@AndroidEventMap.setupMap(
                                map,
                                scope,
                                uri.toString(),
                                onMapLoaded = {
                                    Log.i(TAG, "Map setup complete, initializing location if needed")
                                    // Initialize location component if permission granted
                                    if (hasLocationPermission) {
                                        Log.d(TAG, "Setting up location component (permission granted)")
                                        setupMapLocationComponent(map, context)
                                    } else {
                                        Log.d(TAG, "Skipping location component (no permission)")
                                    }
                                    onMapLoaded()
                                },
                                onMapClick = { _, _ ->
                                    Log.d(TAG, "Map clicked")
                                    onMapClick?.invoke()
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------------

    /**
     * Sets up the Android location component
     */
    @SuppressLint("MissingPermission")
    private fun setupMapLocationComponent(map: MapLibreMap, context: Context) {
        Log.i(TAG, "Setting up map location component")
        map.style?.let { style ->
            // Activate location component
            Log.d(TAG, "Activating location component")
            map.locationComponent.activateLocationComponent(
                buildLocationComponentActivationOptions(context, style)
            )
            Log.d(TAG, "Enabling location component")
            map.locationComponent.isLocationComponentEnabled = true
            Log.d(TAG, "Setting camera mode to NONE (no tracking)")
            map.locationComponent.cameraMode = CameraMode.NONE // Do not track user
            Log.i(TAG, "Location component setup complete")
        } ?: run {
            Log.e(TAG, "Cannot setup location component - map style is null")
        }
    }

    /**
     * Builds LocationComponentActivationOptions for configuring the MapLibre location component
     */
    private fun buildLocationComponentActivationOptions(
        context: Context,
        style: Style
    ): LocationComponentActivationOptions {
        Log.d(TAG, "Building location component options")
        return LocationComponentActivationOptions.builder(context, style)
            .locationComponentOptions(
                LocationComponentOptions.builder(context)
                    .pulseEnabled(true)
                    .pulseColor(Color.RED)
                    .foregroundTintColor(Color.BLACK)
                    .build()
            )
            .useDefaultLocationEngine(false)
            .locationEngine(LocationEngineProxy((locationProvider as AndroidWWWLocationProvider).locationEngine))
            .locationEngineRequest(buildLocationEngineRequest())
            .build()
    }

    /**
     * Builds a LocationEngineRequest for location updates
     */
    private fun buildLocationEngineRequest(): LocationEngineRequest {
        Log.d(TAG, "Building location engine request with interval: ${CONST_TIMER_GPS_UPDATE.inWholeMilliseconds}ms")
        return LocationEngineRequest.Builder(CONST_TIMER_GPS_UPDATE.inWholeMilliseconds)
            .setFastestInterval(CONST_TIMER_GPS_UPDATE.inWholeMilliseconds / 2)
            .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
            .build()
    }

    // ------------------------------------------------------------------------

    /**
     * Utility to check if both fine & coarse location permissions are already
     * granted – used when the app returns to foreground or when the user toggles
     * GPS from system settings so we can update the MapLibre location component
     * without re-prompting.
     */
    private fun isLocationPermissionGranted(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        
        Log.d(TAG, "Location permissions check: fine=$fine, coarse=$coarse")
        return fine && coarse
    }

    /**
     * Enable / disable the MapLibre location component depending on the latest
     * permission + provider state.  Called from lifecycle & GPS receivers.
     */
    private fun updateLocationComponent(context: Context, hasPermission: Boolean) {
        Log.i(TAG, "Updating location component, permission=$hasPermission")
        val map = currentMap
        if (map == null) {
            Log.w(TAG, "Cannot update location component - map is null")
            return
        }
        
        map.style?.let {
            try {
                // Always disable first to avoid stale state
                Log.d(TAG, "Disabling location component to avoid stale state")
                map.locationComponent.isLocationComponentEnabled = false

                if (hasPermission) {
                    Log.d(TAG, "Re-enabling location component with fresh state")
                    setupMapLocationComponent(map, context)
                } else {
                    Log.d(TAG, "Leaving location component disabled (no permission)")
                }
            } catch (se: SecurityException) {
                // Permission might have been revoked between check and use
                Log.w(TAG, "Location permission missing when enabling component", se)
            } catch (t: Throwable) {
                // Catch-all to avoid crashing the map in unexpected situations
                Log.e(TAG, "Failed to update location component", t)
            }
        } ?: run {
            Log.w(TAG, "Cannot update location component - map style is null")
        }
    }

    // ------------------------------------------------------------------------

    /**
     * Remembers a MapView and gives it the lifecycle of the current LifecycleOwner
     */
    @Composable
    fun rememberMapLibreViewWithLifecycle(key: Any? = Unit): MapView {
        Log.i(TAG, "Creating MapLibreView with key: $key")
        val context = LocalContext.current

        // Build the MapLibre view
        val maplibreMapOptions = MapLibreMapOptions.createFromAttributes(context)
        Log.d(TAG, "Configuring MapLibreMapOptions")
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
            Log.d(TAG, "Map gestures enabled: $activateMapGestures (based on camera position: ${mapConfig.initialCameraPosition})")

            zoomGesturesEnabled(activateMapGestures)
            scrollGesturesEnabled(activateMapGestures)
            doubleTapGesturesEnabled(activateMapGestures)

            // Always deactivate rotation gestures
            rotateGesturesEnabled(false)
            tiltGesturesEnabled(false)
        }

        // Ensure MapLibre initialises with a split-aware AssetManager so freshly
        // installed dynamic-feature resources (mbtiles, sprites, …) are visible
        Log.d(TAG, "Installing SplitCompat on context before MapLibre initialization")
        SplitCompat.install(context)

        Log.d(TAG, "Initializing MapLibre instance")
        MapLibre.getInstance(context) // Required by the API

        // The key makes Compose recreate the MapView when it changes
        val mapView = remember(key) { 
            Log.i(TAG, "Creating new MapView instance with key: $key")
            MapView(context, maplibreMapOptions) 
        }

        // Makes MapView follow the lifecycle of this composable
        val lifecycle = LocalLifecycleOwner.current.lifecycle
        DisposableEffect(lifecycle, mapView) {
            Log.d(TAG, "Setting up MapView lifecycle observer")
            val lifecycleObserver = getMapLifecycleObserver(mapView)
            lifecycle.addObserver(lifecycleObserver)
            onDispose {
                Log.d(TAG, "Removing MapView lifecycle observer")
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
        Log.i(TAG, "Updating wave polygons: count=${wavePolygons.size}, clear=$clearPolygons")
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
            Lifecycle.Event.ON_CREATE -> {
                Log.d("EventMap", "MapView lifecycle: onCreate")
                mapView.onCreate(Bundle())
            }
            Lifecycle.Event.ON_START -> {
                Log.d("EventMap", "MapView lifecycle: onStart")
                mapView.onStart()
            }
            Lifecycle.Event.ON_RESUME -> {
                Log.d("EventMap", "MapView lifecycle: onResume")
                mapView.onResume()
            }
            Lifecycle.Event.ON_PAUSE -> {
                Log.d("EventMap", "MapView lifecycle: onPause")
                mapView.onPause()
            }
            Lifecycle.Event.ON_STOP -> {
                Log.d("EventMap", "MapView lifecycle: onStop")
                mapView.onStop()
            }
            Lifecycle.Event.ON_DESTROY -> {
                Log.d("EventMap", "MapView lifecycle: onDestroy")
                mapView.onDestroy()
            }
            else -> throw IllegalStateException()
        }
    }
