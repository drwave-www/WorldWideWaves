package com.worldwidewaves.compose.map

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
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import com.worldwidewaves.R
import com.worldwidewaves.compose.common.DownloadProgressIndicator
import com.worldwidewaves.compose.common.LoadingIndicator
import com.worldwidewaves.map.AndroidMapLibreAdapter
import com.worldwidewaves.shared.MokoRes
import com.worldwidewaves.shared.WWWGlobals.Companion.Timing
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
import androidx.compose.ui.res.painterResource as painterResourceAndroid

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
    mapConfig: EventMapConfig = EventMapConfig(),
) : AbstractEventMap<MapLibreMap>(event, mapConfig, onLocationUpdate),
    KoinComponent {
    private companion object {
        private const val TAG = "EventMap"

        // Map loading constants
        private const val MAX_STYLE_RESOLUTION_ATTEMPTS = 10
        private const val STYLE_RESOLUTION_DELAY_MS = 200L
        private const val MAP_ATTACH_TIMEOUT_MS = 1500L

        // UI Constants
        private const val DOWNLOAD_PROGRESS_MAX = 100
        private const val BUTTON_WIDTH_DP = 200f
        private const val BUTTON_HEIGHT_DP = 60f
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
    fun Screen(
        autoMapDownload: Boolean = false,
        modifier: Modifier,
    ) {
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
        // Guard to avoid double initialization attempts from AndroidView.update
        var initStarted by remember { mutableStateOf(false) }
        // Track current MapView instance so we can detect recreations
        var lastMapView by remember { mutableStateOf<MapView?>(null) }

        // Re-create the MapView whenever availability flips so a fresh
        // split-aware AssetManager is used.
        val mapLibreView: MapView =
            rememberMapLibreViewWithLifecycle(key = "${event.id}-$isMapAvailable")

        val mapViewModel: MapViewModel = viewModel()
        val mapFeatureState by mapViewModel.featureState.collectAsState()

        // Check if map is downloaded
        LaunchedEffect(Unit) {
            mapViewModel.checkIfMapIsAvailable(event.id, autoDownload = false)
            isMapAvailable = mapAvailabilityChecker.isMapDownloaded(event.id)
            Log.i(TAG, "Initial map availability check result: ${event.id} available=$isMapAvailable")
        }

        // Update download state based on MapViewModel state
        LaunchedEffect(mapFeatureState) {
            when (mapFeatureState) {
                is MapFeatureState.Downloading -> {
                    isMapDownloading = true
                }
                is MapFeatureState.Pending -> isMapDownloading = true
                is MapFeatureState.Installed -> {
                    Log.i(TAG, "Map installed: ${event.id}")
                    // Make the just-installed split immediately visible to this
                    // running Activity – required for MapLibre to see assets.
                    (context as? AppCompatActivity)?.let {
                        SplitCompat.installActivity(it)
                    }
                    isMapDownloading = false
                    isMapAvailable = true
                    mapError = false
                    // Allow a fresh initialization attempt now that install finished
                    initStarted = false
                }
                is MapFeatureState.Failed -> {
                    val errorCode = (mapFeatureState as MapFeatureState.Failed).errorCode
                    Log.e(TAG, "Map download failed: ${event.id}, errorCode=$errorCode")
                    mapError = true
                    isMapDownloading = false
                    // Reset init flag so a new attempt can be triggered after failure
                    initStarted = false
                }
                is MapFeatureState.Canceling -> isMapDownloading = false
                is MapFeatureState.Installing -> isMapDownloading = true
                is MapFeatureState.NotAvailable -> isMapDownloading = false
                else -> {}
            }
        }

        // Request GPS location Android permissions
        hasLocationPermission = requestLocationPermission()
        if (hasLocationPermission) {
            CheckGPSEnable()
        }

        // Monitor permissions and GPS provider changes
        val lifecycleOwner = LocalLifecycleOwner.current
        DisposableEffect(lifecycleOwner) {
            val observer =
                LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        val granted = isLocationPermissionGranted(context)
                        hasLocationPermission = granted
                        updateLocationComponent(context, granted)
                    }
                }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }

        DisposableEffect(Unit) {
            val receiver =
                object : BroadcastReceiver() {
                    override fun onReceive(
                        c: Context?,
                        i: Intent?,
                    ) {
                        val granted = isLocationPermissionGranted(context)
                        hasLocationPermission = granted
                        updateLocationComponent(context, granted)
                    }
                }
            val filter = IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION)
            context.registerReceiver(receiver, filter)
            onDispose {
                context.unregisterReceiver(receiver)
            }
        }

        // Auto-download map once when requested by caller
        LaunchedEffect(isMapAvailable, autoMapDownload, userCanceled) {
            if (!isMapAvailable && autoMapDownload && !userCanceled) {
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
                contentScale = ContentScale.Crop,
            )

            // LibreMap as Android Composable - only visible when map is loaded
            AndroidView(
                factory = { mapLibreView },
                update = { v ->
                    if (lastMapView !== v) {
                        lastMapView = v
                        isMapLoaded = false
                        initStarted = false
                        mapError = false
                    }

                    if (isMapAvailable && !isMapLoaded && !initStarted) {
                        initStarted = true
                        Log.i(TAG, "Starting map init from AndroidView.update")
                        loadMap(
                            context = context,
                            scope = scope,
                            mapLibreView = v,
                            hasLocationPermission = hasLocationPermission,
                            lifecycle = lifecycleOwner.lifecycle,
                            onMapLoaded = {
                                Log.i(TAG, "Map successfully loaded: ${event.id}")
                                isMapLoaded = true
                                onMapLoaded()
                            },
                            onMapError = {
                                Log.e(TAG, "Error loading map: ${event.id}")
                                mapError = true
                                initStarted = false // allow retry
                            },
                        )
                    }
                },
                modifier =
                    Modifier
                        .fillMaxSize()
                        .alpha(if (isMapLoaded) 1f else 0f),
            )

            // Show appropriate UI based on map state
            when {
                isMapLoaded -> Unit
                isMapDownloading && !mapError ->
                    MapDownloadOverlay(mapFeatureState) {
                        userCanceled = true
                        mapViewModel.cancelDownload()
                    }
                mapError && isMapDownloading ->
                    MapErrorOverlay {
                        mapError = false
                        initStarted = false
                        mapViewModel.downloadMap(event.id)
                    }
                !isMapAvailable ->
                    MapDownloadButton {
                        mapError = false
                        isMapDownloading = true
                        userCanceled = false
                        mapViewModel.downloadMap(event.id)
                    }
            }
        }
    }

    @Composable
    private fun MapDownloadOverlay(
        state: MapFeatureState,
        onCancel: () -> Unit,
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background.copy(alpha = 0.8f),
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    when (state) {
                        is MapFeatureState.Downloading ->
                            DownloadProgressIndicator(
                                progress = state.progress,
                                message = stringResource(MokoRes.strings.map_downloading),
                                onCancel = onCancel,
                            )
                        is MapFeatureState.Pending ->
                            LoadingIndicator(message = stringResource(MokoRes.strings.map_starting_download))
                        is MapFeatureState.Retrying ->
                            DownloadProgressIndicator(
                                message = "${stringResource(
                                    MokoRes.strings.map_retrying_download,
                                )} (${state.attempt}/${state.maxAttempts})...",
                                onCancel = onCancel,
                            )
                        is MapFeatureState.Installing ->
                            DownloadProgressIndicator(
                                progress = DOWNLOAD_PROGRESS_MAX,
                                message = stringResource(MokoRes.strings.map_installing),
                                onCancel = onCancel,
                            )
                        else ->
                            LoadingIndicator(message = stringResource(MokoRes.strings.map_loading))
                    }
                }
            }
        }
    }

    @Composable
    private fun MapErrorOverlay(onRetry: () -> Unit) {
        Surface(modifier = Modifier.fillMaxSize()) {
            ErrorMessage(
                message = stringResource(MokoRes.strings.map_error_download),
                onRetry = onRetry,
            )
        }
    }

    // ------------------------------------------------------------------------
    // Local copy of ErrorMessage (was previously in Commons)
    // ------------------------------------------------------------------------

    @Composable
    private fun ErrorMessage(
        message: String,
        onRetry: () -> Unit,
        modifier: Modifier = Modifier,
    ) {
        androidx.compose.foundation.layout.Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = modifier.padding(16.dp),
        ) {
            Icon(
                painter = painterResourceAndroid(R.drawable.ic_info),
                contentDescription = stringResource(MokoRes.strings.error),
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.error,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onRetry,
                modifier = Modifier,
            ) {
                Icon(
                    painter = painterResourceAndroid(R.drawable.ic_refresh),
                    contentDescription = stringResource(MokoRes.strings.map_retry_download),
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(MokoRes.strings.map_retry_download))
            }
        }
    }

    @Composable
    private fun MapDownloadButton(onClick: () -> Unit) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Button(
                onClick = onClick,
                modifier = Modifier.size(width = BUTTON_WIDTH_DP.dp, height = BUTTON_HEIGHT_DP.dp),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
            ) {
                Text(
                    text = stringResource(MokoRes.strings.map_download),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
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
        lifecycle: Lifecycle,
        onMapLoaded: () -> Unit,
        onMapError: () -> Unit = {},
    ) {
        Log.i(TAG, "Loading map for event: ${event.id}")
        // Ensure SplitCompat is installed before accessing any dynamic feature assets
        SplitCompat.install(context)

        scope.launch {
            withContext(Dispatchers.IO) {
                // IO actions
                var stylePath: String? = null
                var attempts = 0
                repeat(MAX_STYLE_RESOLUTION_ATTEMPTS) { attempt ->
                    attempts = attempt + 1
                    val candidate = event.map.getStyleUri()
                    if (candidate != null && File(candidate).exists()) {
                        Log.i(TAG, "Style URI resolved: $candidate")
                        stylePath = candidate
                        return@repeat
                    }

                    if (attempt == MAX_STYLE_RESOLUTION_ATTEMPTS - 1) { // Log warning only on last attempts
                        Log.w(TAG, "Style URI resolution attempts: $attempts, retrying...")
                    }

                    // Give Play-Core/asset manager time to expose freshly installed split assets
                    delay(STYLE_RESOLUTION_DELAY_MS)
                }

                if (stylePath == null) {
                    Log.e(TAG, "Failed to resolve style URI after $attempts attempts")
                    onMapError()
                    return@withContext
                }

                val uri = Uri.fromFile(File(stylePath))

                scope.launch {
                    // UI actions
                    // Encapsulate the original logic so we can call it from multiple places
                    fun invokeGetMapAsync() {
                        mapLibreView.getMapAsync { map ->
                            Log.i(TAG, "MapLibreMap instance received")
                            // Save reference so we can refresh location component later
                            currentMap = map
                            // Setup Map
                            this@AndroidEventMap.setupMap(
                                map,
                                scope,
                                uri.toString(),
                                onMapLoaded = {
                                    Log.i(TAG, "Map setup complete, initializing location if needed")
                                    // Initialize location component only if permission granted
                                    // and the lifecycle is at least STARTED (Activity/Fragment visible).
                                    if (hasLocationPermission &&
                                        lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
                                    ) {
                                        setupMapLocationComponent(map, context)
                                    }
                                    onMapLoaded()
                                },
                                onMapClick = { _, _ ->
                                    onMapClick?.invoke()
                                },
                            )
                        }
                    }

                    if (mapLibreView.isAttachedToWindow) {
                        invokeGetMapAsync()
                    } else {
                        // Listener to detect when the view gets attached
                        val listener =
                            object : View.OnAttachStateChangeListener {
                                override fun onViewAttachedToWindow(v: View) {
                                    v.removeOnAttachStateChangeListener(this)
                                    v.post { invokeGetMapAsync() }
                                }

                                override fun onViewDetachedFromWindow(v: View) = Unit
                            }
                        mapLibreView.addOnAttachStateChangeListener(listener)

                        // Safety timeout in case attachment never happens
                        Handler(Looper.getMainLooper()).postDelayed({
                            if (mapLibreView.isAttachedToWindow.not()) {
                                Log.w(TAG, "MapView still not attached after timeout; forcing getMapAsync")
                                try {
                                    mapLibreView.removeOnAttachStateChangeListener(listener)
                                } catch (_: IllegalStateException) {
                                    // ignore listener removal errors
                                }
                                invokeGetMapAsync()
                            }
                        }, MAP_ATTACH_TIMEOUT_MS)
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
    private fun setupMapLocationComponent(
        map: MapLibreMap,
        context: Context,
    ) {
        Log.i(TAG, "Setting up map location component")
        map.style?.let { style ->
            try {
                // Check if already activated to avoid double activation
                if (!map.locationComponent.isLocationComponentActivated) {
                    // Activate location component
                    map.locationComponent.activateLocationComponent(
                        buildLocationComponentActivationOptions(context, style),
                    )
                }
                map.locationComponent.isLocationComponentEnabled = true
                map.locationComponent.cameraMode = CameraMode.NONE // Do not track user
                Log.i(TAG, "Location component setup complete")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to setup location component", e)
            }
        } ?: run {
            Log.e(TAG, "Cannot setup location component - map style is null")
        }
    }

    /**
     * Builds LocationComponentActivationOptions for configuring the MapLibre location component
     */
    private fun buildLocationComponentActivationOptions(
        context: Context,
        style: Style,
    ): LocationComponentActivationOptions =
        LocationComponentActivationOptions
            .builder(context, style)
            .locationComponentOptions(
                LocationComponentOptions
                    .builder(context)
                    .pulseEnabled(true)
                    .pulseColor(Color.RED)
                    .foregroundTintColor(Color.BLACK)
                    .build(),
            ).useDefaultLocationEngine(false)
            .locationEngine(LocationEngineProxy((locationProvider as AndroidWWWLocationProvider).locationEngine))
            .locationEngineRequest(buildLocationEngineRequest())
            .build()

    /**
     * Builds a LocationEngineRequest for location updates
     */
    private fun buildLocationEngineRequest(): LocationEngineRequest =
        LocationEngineRequest
            .Builder(Timing.GPS_UPDATE_INTERVAL.inWholeMilliseconds)
            .setFastestInterval(Timing.GPS_UPDATE_INTERVAL.inWholeMilliseconds / 2)
            .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
            .build()

    // ------------------------------------------------------------------------

    /**
     * Utility to check if both fine & coarse location permissions are already
     * granted – used when the app returns to foreground or when the user toggles
     * GPS from system settings so we can update the MapLibre location component
     * without re-prompting.
     */
    private fun isLocationPermissionGranted(context: Context): Boolean {
        val fine =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION,
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val coarse =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        return fine && coarse
    }

    /**
     * Enable / disable the MapLibre location component depending on the latest
     * permission + provider state.  Called from lifecycle & GPS receivers.
     */
    private fun updateLocationComponent(
        context: Context,
        hasPermission: Boolean,
    ) {
        Log.i(TAG, "Updating location component, permission=$hasPermission")
        val map = currentMap
        if (map == null) {
            Log.w(TAG, "Cannot update location component - map is null")
            return
        }

        map.style?.let {
            try {
                // Check if location component is activated before trying to disable it
                if (map.locationComponent.isLocationComponentActivated) {
                    // Always disable first to avoid stale state
                    map.locationComponent.isLocationComponentEnabled = false
                }

                if (hasPermission) {
                    setupMapLocationComponent(map, context)
                }
            } catch (se: SecurityException) {
                // Permission might have been revoked between check and use
                Log.w(TAG, "Location permission missing when enabling component", se)
            } catch (ise: IllegalStateException) {
                // Map component might be in invalid state
                Log.e(TAG, "Map component in invalid state", ise)
            } catch (uoe: UnsupportedOperationException) {
                // Map operation not supported
                Log.e(TAG, "Unsupported map operation", uoe)
            } catch (e: RuntimeException) {
                // Location component not initialized - this is expected on first run
                if (e.message?.contains("LocationComponent has to be activated") == true) {
                    Log.d(TAG, "Location component not yet initialized, will activate it")
                    if (hasPermission) {
                        setupMapLocationComponent(map, context)
                    }
                } else {
                    Log.e(TAG, "Unexpected runtime error with location component", e)
                }
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
        Log.d(TAG, "Creating MapLibreView with key: $key")
        val context = LocalContext.current

        // Build the MapLibre view
        val maplibreMapOptions = MapLibreMapOptions.createFromAttributes(context)
        maplibreMapOptions.apply {
            camera(
                CameraPosition
                    .Builder()
                    .padding(0.0, 0.0, 0.0, 0.0)
                    .bearing(0.0)
                    .tilt(0.0)
                    .build(),
            )

            localIdeographFontFamily("Droid Sans") // FIXME: replace with MapLibre font-maker solution

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

        // Ensure MapLibre initialises with a split-aware AssetManager
        SplitCompat.install(context)
        MapLibre.getInstance(context) // Required by the API

        // The key makes Compose recreate the MapView when it changes
        val mapView =
            remember(key) {
                MapView(context, maplibreMapOptions)
            }

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
    fun updateWavePolygons(
        context: Context,
        wavePolygons: List<Polygon>,
        clearPolygons: Boolean,
    ) {
        Log.v(TAG, "Updating wave polygons: count=${wavePolygons.size}, clear=$clearPolygons")
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
