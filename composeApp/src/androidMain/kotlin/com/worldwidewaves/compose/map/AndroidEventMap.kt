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
import android.util.Log
import android.view.View
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import com.worldwidewaves.R
import com.worldwidewaves.activities.event.EventFullMapActivity
import com.worldwidewaves.map.AndroidMapLibreAdapter
import com.worldwidewaves.shared.MokoRes
import com.worldwidewaves.shared.WWWGlobals.Timing
import com.worldwidewaves.shared.WWWPlatform
import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.events.utils.Polygon
import com.worldwidewaves.shared.events.utils.Position
import com.worldwidewaves.shared.map.AbstractEventMap
import com.worldwidewaves.shared.map.EventMapConfig
import com.worldwidewaves.shared.map.LocationProvider
import com.worldwidewaves.shared.map.MapCameraPosition
import com.worldwidewaves.shared.map.MapFeatureState
import com.worldwidewaves.shared.toMapLibrePolygon
import com.worldwidewaves.shared.ui.components.DownloadProgressIndicator
import com.worldwidewaves.shared.ui.components.LoadingIndicator
import com.worldwidewaves.utils.AndroidLocationProvider
import com.worldwidewaves.utils.AndroidMapAvailabilityChecker
import com.worldwidewaves.utils.CheckGPSEnable
import com.worldwidewaves.utils.requestLocationPermission
import com.worldwidewaves.viewmodels.AndroidMapViewModel
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
    private val context: AppCompatActivity, // MANDATORY - required for wave layer UI thread operations
    private val onMapLoaded: () -> Unit = {},
    onLocationUpdate: (Position) -> Unit = {},
    mapConfig: EventMapConfig = EventMapConfig(),
) : AbstractEventMap<MapLibreMap>(event, mapConfig, onLocationUpdate),
    KoinComponent {
    private companion object {
        private const val TAG = "WWW.Map.EventMap"

        // UI Constants
        private const val DOWNLOAD_PROGRESS_MAX = 100
        private const val BUTTON_WIDTH_DP = 200f
        private const val BUTTON_HEIGHT_DP = 60f
    }

    // Overrides properties from AbstractEventMap
    override val locationProvider: LocationProvider by inject(AndroidLocationProvider::class.java)
    override val mapLibreAdapter: AndroidMapLibreAdapter by lazy { AndroidMapLibreAdapter() }

    /** Holds the last [MapLibreMap] provided by MapView so we can (re-)enable
     *  the location component whenever permission or provider state changes. */
    private var currentMap: MapLibreMap? = null

    // Map availability and download state tracking
    private val mapAvailabilityChecker: AndroidMapAvailabilityChecker by inject(AndroidMapAvailabilityChecker::class.java)

    // Platform for observing simulation changes
    private val platform: WWWPlatform by inject(WWWPlatform::class.java)

    /**
     * Setup map state variables and return them as a data class
     */
    @Composable
    private fun setupMapState(): MapState {
        val scope = rememberCoroutineScope()
        var isMapLoaded by remember { mutableStateOf(false) }
        var mapError by remember { mutableStateOf(false) }
        // Initialize with actual permission state instead of false
        var hasLocationPermission by remember { mutableStateOf(isLocationPermissionGranted(context)) }
        var isMapAvailable by remember { mutableStateOf(false) }
        var isMapDownloading by remember { mutableStateOf(false) }
        // Guard to avoid auto-re-download after the user explicitly cancels
        var userCanceled by remember { mutableStateOf(false) }
        // Guard to avoid double initialization attempts from AndroidView.update
        var initStarted by remember { mutableStateOf(false) }
        // Track current MapView instance so we can detect recreations
        var lastMapView by remember { mutableStateOf<MapView?>(null) }

        // MapView should be created once and reused (don't recreate on map availability change)
        // The style will be set/updated when map becomes available
        val mapLibreView: MapView =
            rememberMapLibreViewWithLifecycle(key = event.id)

        val mapViewModel: AndroidMapViewModel = viewModel()
        val mapFeatureState by mapViewModel.featureState.collectAsState()

        return MapState(
            scope = scope,
            isMapLoaded = isMapLoaded,
            mapError = mapError,
            hasLocationPermission = hasLocationPermission,
            isMapAvailable = isMapAvailable,
            isMapDownloading = isMapDownloading,
            userCanceled = userCanceled,
            initStarted = initStarted,
            lastMapView = lastMapView,
            mapLibreView = mapLibreView,
            mapViewModel = mapViewModel,
            mapFeatureState = mapFeatureState,
            setIsMapLoaded = { isMapLoaded = it },
            setMapError = { mapError = it },
            setHasLocationPermission = { hasLocationPermission = it },
            setIsMapAvailable = { isMapAvailable = it },
            setIsMapDownloading = { isMapDownloading = it },
            setUserCanceled = { userCanceled = it },
            setInitStarted = { initStarted = it },
            setLastMapView = { lastMapView = it },
        )
    }

    /**
     * Data class to hold map state variables and their setters
     */
    private data class MapState(
        val scope: CoroutineScope,
        val isMapLoaded: Boolean,
        val mapError: Boolean,
        val hasLocationPermission: Boolean,
        val isMapAvailable: Boolean,
        val isMapDownloading: Boolean,
        val userCanceled: Boolean,
        val initStarted: Boolean,
        val lastMapView: MapView?,
        val mapLibreView: MapView,
        val mapViewModel: AndroidMapViewModel,
        val mapFeatureState: MapFeatureState,
        val setIsMapLoaded: (Boolean) -> Unit,
        val setMapError: (Boolean) -> Unit,
        val setHasLocationPermission: (Boolean) -> Unit,
        val setIsMapAvailable: (Boolean) -> Unit,
        val setIsMapDownloading: (Boolean) -> Unit,
        val setUserCanceled: (Boolean) -> Unit,
        val setInitStarted: (Boolean) -> Unit,
        val setLastMapView: (MapView?) -> Unit,
    )

    /**
     * The Compose UI for the map
     */
    @Composable
    override fun Draw(
        autoMapDownload: Boolean,
        modifier: Modifier,
    ) {
        val mapState = setupMapState()

        // Handle map availability and download state
        HandleMapAvailability(mapState)

        // Handle location permissions and GPS
        HandleLocationPermissions(mapState, autoMapDownload)

        // Render the map content
        RenderMapContent(mapState, modifier)
    }

    /**
     * Handle map availability checks and download state updates
     */
    @Composable
    @Suppress("FunctionName")
    private fun HandleMapAvailability(mapState: MapState) {
        // Update download state based on MapViewModel state
        LaunchedEffect(mapState.mapFeatureState) {
            when (mapState.mapFeatureState) {
                is MapFeatureState.Downloading,
                MapFeatureState.Pending,
                MapFeatureState.Installing,
                -> mapState.setIsMapDownloading(true)

                is MapFeatureState.Installed -> {
                    Log.i(TAG, "Map installed: ${event.id}")
                    mapState.setIsMapDownloading(false)
                    mapState.setIsMapAvailable(true)
                    mapState.setMapError(false)
                    mapState.setInitStarted(false)
                }
                is MapFeatureState.Failed -> {
                    Log.e(TAG, "Map download failed: ${event.id}, errorCode=${mapState.mapFeatureState.errorCode}")
                    // Only set error state if not currently downloading
                    // This prevents map load errors from showing error UI during active download
                    if (!mapState.isMapDownloading) {
                        mapState.setMapError(true)
                    }
                    mapState.setIsMapDownloading(false)
                    mapState.setInitStarted(false)
                }
                MapFeatureState.Canceling,
                MapFeatureState.NotAvailable,
                -> mapState.setIsMapDownloading(false)

                else -> {}
            }
        }
    }

    /**
     * Handle location permissions and GPS setup
     */
    @Composable
    @Suppress("FunctionName")
    private fun HandleLocationPermissions(
        mapState: MapState,
        autoMapDownload: Boolean,
    ) {
        // Request GPS location Android permissions
        mapState.setHasLocationPermission(requestLocationPermission())
        if (mapState.hasLocationPermission) {
            CheckGPSEnable()
        }

        // Monitor permissions and GPS provider changes
        val lifecycleOwner = LocalLifecycleOwner.current
        DisposableEffect(lifecycleOwner) {
            val observer =
                LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        val granted = isLocationPermissionGranted(context)
                        mapState.setHasLocationPermission(granted)
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
                        mapState.setHasLocationPermission(granted)
                        updateLocationComponent(context, granted)
                    }
                }
            val filter = IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION)
            context.registerReceiver(receiver, filter)
            onDispose {
                context.unregisterReceiver(receiver)
            }
        }

        // Map availability check and auto-download logic
        LaunchedEffect(event.id, mapState.isMapAvailable, autoMapDownload, mapState.userCanceled) {
            // Initial availability check
            if (!mapState.isMapAvailable) {
                mapState.mapViewModel.checkIfMapIsAvailable(event.id, autoDownload = false)
                mapState.setIsMapAvailable(mapAvailabilityChecker.isMapDownloaded(event.id))
            }

            // Auto-download if needed
            if (!mapState.isMapAvailable && autoMapDownload && !mapState.userCanceled) {
                mapState.mapViewModel.downloadMap(
                    mapId = event.id,
                    onMapDownloaded = {
                        // Sync MapViewModel state after download completes
                        // Ensures map availability is rechecked after successful download
                        launch {
                            mapState.mapViewModel.checkIfMapIsAvailable(event.id, autoDownload = false)
                            mapState.setIsMapAvailable(mapAvailabilityChecker.isMapDownloaded(event.id))
                        }
                    },
                )
            }
        }

        // Observe simulation changes and refresh LocationComponent
        // Fixes issue where marker doesn't appear on event detail map when simulation
        // starts after map is already initialized
        LaunchedEffect(event.id) {
            platform.simulationChanged.collect {
                // Simulation state changed (started, stopped, or reset)
                val map = currentMap
                if (map != null && map.locationComponent.isLocationComponentActivated) {
                    Log.i(TAG, "Simulation changed, refreshing LocationComponent for event ${event.id}")
                    // Launch on Main dispatcher as LocationComponent must be updated on UI thread
                    launch(Dispatchers.Main) {
                        try {
                            // Force LocationComponent to re-query the location engine
                            // This ensures it picks up simulation position changes
                            updateLocationComponentWithStyle(map, context, mapState.hasLocationPermission)
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to refresh LocationComponent on simulation change", e)
                        }
                    }
                }
            }
        }
    }

    @Composable
    @Suppress("FunctionName")
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
    @Suppress("FunctionName")
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
    @Suppress("FunctionName")
    private fun ErrorMessage(
        message: String,
        onRetry: () -> Unit,
        modifier: Modifier = Modifier,
    ) {
        Column(
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
    @Suppress("FunctionName")
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

    /**
     * Render the main map content UI
     */
    @Composable
    @Suppress("FunctionName")
    private fun RenderMapContent(
        mapState: MapState,
        modifier: Modifier,
    ) {
        val lifecycleOwner = LocalLifecycleOwner.current

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
                factory = { mapState.mapLibreView },
                update = { v ->
                    if (mapState.lastMapView !== v) {
                        mapState.setLastMapView(v)
                        mapState.setIsMapLoaded(false)
                        mapState.setInitStarted(false)
                        mapState.setMapError(false)
                    }

                    // Check mapFeatureState directly to determine if map files are ready
                    // This eliminates race condition with async isMapAvailable state updates
                    // Reading mapFeatureState creates Compose dependency for recomposition
                    val mapFilesReady =
                        when (mapState.mapFeatureState) {
                            is MapFeatureState.Installed, // Just completed download
                            is MapFeatureState.Available, // Already cached from previous session
                            -> true
                            else -> false // Downloading, NotAvailable, Failed, etc.
                        }

                    // Only attempt to initialize map when tiles are actually available (downloaded)
                    // Attempting to load before download completes causes gray screen (no tiles)
                    // and wrong camera position (moves to bounds before tiles exist)
                    if (!mapState.isMapLoaded && !mapState.initStarted && mapFilesReady) {
                        mapState.setInitStarted(true)
                        loadMap(
                            context = context,
                            scope = mapState.scope,
                            mapLibreView = v,
                            hasLocationPermission = mapState.hasLocationPermission,
                            lifecycle = lifecycleOwner.lifecycle,
                            onMapLoaded = {
                                mapState.setIsMapLoaded(true)
                                onMapLoaded()
                            },
                            onMapError = {
                                Log.e(TAG, "Error loading map: ${event.id}")
                                // Don't set error during active download - files aren't ready yet
                                if (!mapState.isMapDownloading) {
                                    mapState.setMapError(true)
                                    mapState.setInitStarted(false)
                                }
                            },
                        )
                    }
                },
                modifier =
                    Modifier
                        .fillMaxSize()
                        .alpha(if (mapState.isMapLoaded) 1f else 0f),
            )

            // Show appropriate UI based on map state
            when {
                mapState.isMapLoaded -> Unit
                mapState.isMapDownloading && !mapState.mapError ->
                    MapDownloadOverlay(mapState.mapFeatureState) {
                        mapState.setUserCanceled(true)
                        mapState.mapViewModel.cancelDownload()
                    }
                mapState.mapError && !mapState.isMapDownloading ->
                    MapErrorOverlay {
                        mapState.setMapError(false)
                        mapState.setInitStarted(false)
                        mapState.mapViewModel.downloadMap(event.id)
                    }
                !mapState.isMapAvailable ->
                    MapDownloadButton {
                        mapState.setMapError(false)
                        mapState.setIsMapDownloading(true)
                        mapState.setUserCanceled(false)
                        mapState.mapViewModel.downloadMap(event.id)
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
        lifecycle: Lifecycle,
        onMapLoaded: () -> Unit,
        onMapError: () -> Unit = {},
    ) {
        scope.launch {
            // Resolve style URI on IO thread
            val (stylePath, uri) =
                withContext(Dispatchers.IO) {
                    // Get style URI (uses cached value on subsequent calls)
                    // getStyleUri() already validates file existence, no need for redundant check
                    val path: String? = event.map.getStyleUri()

                    if (path == null) {
                        Log.e(TAG, "Failed to resolve style URI for event ${event.id}")
                        return@withContext null to null
                    }

                    path to Uri.fromFile(File(path))
                }

            if (stylePath == null || uri == null) {
                onMapError()
                return@launch
            }

            // Setup map callback
            fun invokeGetMapAsync() {
                mapLibreView.getMapAsync { map ->
                    // Save reference so we can refresh location component later
                    currentMap = map
                    // Setup Map
                    this@AndroidEventMap.setupMap(
                        map,
                        scope,
                        uri.toString(),
                        onMapLoaded = {
                            // Initialize location component in parallel after style is loaded
                            // Runs on main thread but doesn't block onMapLoaded completion
                            Log.d(
                                TAG,
                                "onMapLoaded callback: hasLocationPermission=$hasLocationPermission, " +
                                    "lifecycle=${lifecycle.currentState}",
                            )
                            if (hasLocationPermission &&
                                lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
                            ) {
                                scope.launch(Dispatchers.Main) {
                                    Log.d(TAG, "Calling setupMapLocationComponent")
                                    setupMapLocationComponent(map, context)
                                }
                            } else {
                                Log.w(
                                    TAG,
                                    "Skipping location component setup: permission=$hasLocationPermission, " +
                                        "lifecycle=${lifecycle.currentState}",
                                )
                            }
                            onMapLoaded()
                        },
                        onMapClick = { _, _ ->
                            context.startActivity(
                                Intent(context, EventFullMapActivity::class.java).apply {
                                    putExtra("eventId", event.id)
                                },
                            )
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
                            invokeGetMapAsync()
                        }

                        override fun onViewDetachedFromWindow(v: View) = Unit
                    }
                mapLibreView.addOnAttachStateChangeListener(listener)
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
        Log.d(TAG, "setupMapLocationComponent called, style=${map.style != null}")
        map.style?.let { style ->
            try {
                // Check if already activated to avoid double activation
                if (!map.locationComponent.isLocationComponentActivated) {
                    Log.d(TAG, "Activating location component")
                    map.locationComponent.activateLocationComponent(
                        buildLocationComponentActivationOptions(context, style),
                    )
                } else {
                    Log.d(TAG, "Location component already activated")
                }
                map.locationComponent.isLocationComponentEnabled = true
                map.locationComponent.cameraMode = CameraMode.NONE
                Log.i(TAG, "Location component setup complete and enabled")
            } catch (e: IllegalStateException) {
                Log.e(TAG, "Failed to setup location component - invalid state", e)
            } catch (e: UnsupportedOperationException) {
                Log.e(TAG, "Failed to setup location component - operation not supported", e)
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
            .locationEngine(LocationEngineProxy((locationProvider as AndroidLocationProvider).locationEngine))
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
     * Enable/disable the MapLibre location component based on permission state.
     * Called from lifecycle and GPS receivers.
     */
    private fun updateLocationComponent(
        context: Context,
        hasPermission: Boolean,
    ) {
        val map = currentMap
        if (map == null) {
            Log.w(TAG, "Cannot update location component - map is null")
            return
        }

        val style = map.style
        if (style == null) {
            Log.w(TAG, "Cannot update location component - map style is null")
            return
        }

        updateLocationComponentWithStyle(map, context, hasPermission)
    }

    /**
     * Helper method to update location component when map and style are available
     */
    private fun updateLocationComponentWithStyle(
        map: MapLibreMap,
        context: Context,
        hasPermission: Boolean,
    ) {
        try {
            disableLocationComponentIfActivated(map)

            if (hasPermission) {
                setupMapLocationComponent(map, context)
            }
        } catch (se: SecurityException) {
            handleLocationPermissionError(se)
        } catch (ise: IllegalStateException) {
            handleLocationComponentStateError(ise, map, context, hasPermission)
        } catch (uoe: UnsupportedOperationException) {
            Log.e(TAG, "Unsupported map operation", uoe)
        }
    }

    /**
     * Disable location component if it's currently activated
     */
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun disableLocationComponentIfActivated(map: MapLibreMap) {
        if (map.locationComponent.isLocationComponentActivated) {
            // Always disable first to avoid stale state
            map.locationComponent.isLocationComponentEnabled = false
        }
    }

    /**
     * Handle permission errors when updating location component
     */
    private fun handleLocationPermissionError(exception: SecurityException) {
        Log.w(TAG, "Location permission missing when enabling component", exception)
    }

    /**
     * Handle illegal state errors when updating location component
     */
    private fun handleLocationComponentStateError(
        exception: IllegalStateException,
        map: MapLibreMap,
        context: Context,
        hasPermission: Boolean,
    ) {
        val message = exception.message
        if (message?.contains("LocationComponent has to be activated") == true) {
            if (hasPermission) {
                setupMapLocationComponent(map, context)
            }
        } else {
            Log.e(TAG, "Map component in invalid state", exception)
        }
    }

    // ------------------------------------------------------------------------

    /**
     * Remembers a MapView and gives it the lifecycle of the current LifecycleOwner
     */
    @Composable
    fun rememberMapLibreViewWithLifecycle(key: Any? = Unit): MapView {
        val context = LocalContext.current

        // Build the MapLibre view
        val maplibreMapOptions = MapLibreMapOptions.createFromAttributes(context)
        maplibreMapOptions.apply {
            // Set initial zoom to avoid zoom-out flash on load
            camera(
                CameraPosition
                    .Builder()
                    .zoom(11.0) // City-level zoom (final zoom set by constraints)
                    .padding(0.0, 0.0, 0.0, 0.0)
                    .bearing(0.0)
                    .tilt(0.0)
                    .build(),
            )

            localIdeographFontFamily("Droid Sans")

            compassEnabled(true)
            compassFadesWhenFacingNorth(true)

            val activateMapGestures = mapConfig.initialCameraPosition == MapCameraPosition.WINDOW

            zoomGesturesEnabled(activateMapGestures)
            scrollGesturesEnabled(activateMapGestures)
            doubleTapGesturesEnabled(activateMapGestures)

            // Always disable rotation and tilt
            rotateGesturesEnabled(false)
            tiltGesturesEnabled(false)
        }

        MapLibre.getInstance(context)

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
    override fun updateWavePolygons(
        wavePolygons: List<Polygon>,
        clearPolygons: Boolean,
    ) {
        context.runOnUiThread {
            val mapLibrePolygons = wavePolygons.map { it.toMapLibrePolygon() }
            mapLibreAdapter.addWavePolygons(mapLibrePolygons, clearPolygons)
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
            else -> error("Unexpected lifecycle event: $event")
        }
    }
