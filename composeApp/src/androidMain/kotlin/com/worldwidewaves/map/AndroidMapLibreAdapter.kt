package com.worldwidewaves.map

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

import android.graphics.Color
import android.os.Looper
import android.util.Log
import androidx.core.graphics.toColorInt
import com.worldwidewaves.shared.WWWGlobals
import com.worldwidewaves.shared.WWWGlobals.Wave
import com.worldwidewaves.shared.events.utils.BoundingBox
import com.worldwidewaves.shared.events.utils.Position
import com.worldwidewaves.shared.map.MapCameraCallback
import com.worldwidewaves.shared.map.MapLibreAdapter
import com.worldwidewaves.shared.toLatLngBounds
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapLibreMap.CancelableCallback
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.PropertyFactory.lineColor
import org.maplibre.android.style.layers.PropertyFactory.lineDasharray
import org.maplibre.android.style.layers.PropertyFactory.lineOpacity
import org.maplibre.android.style.layers.PropertyFactory.lineWidth
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.Point
import org.maplibre.geojson.Polygon

/**
 * Android-specific implementation of the shared [MapLibreAdapter].
 *
 * Wraps MapLibre Android's `MapLibreMap` and exposes the platform-agnostic API
 * expected by the shared `AbstractEventMap` / `AndroidEventMap` layers:
 * • Style / source initialisation and dynamic updates (wave polygons, bbox)
 * • Camera helpers (bounds, animate/move, zoom & position flows)
 * • Click & camera listeners wiring with Kotlin callbacks
 *
 * This adapter is strictly *glue* code – all high-level map logic remains in the
 * shared module so iOS can provide its own counterpart.
 */
class AndroidMapLibreAdapter(
    private var mapLibreMap: MapLibreMap? = null,
) : MapLibreAdapter<MapLibreMap> {
    companion object {
        private const val TAG = "WWW.Map.Android"
        private const val MIN_LATITUDE = -90.0
        private const val MAX_LATITUDE = 90.0
        private const val MIN_LONGITUDE = -180.0
        private const val MAX_LONGITUDE = 180.0

        // ZOOM_SAFETY_MARGIN removed - base min zoom calculation already ensures event fits
        // The min(zoomForWidth, zoomForHeight) ensures BOTH dimensions fit in viewport
    }

    // -- Public/Override properties

    private val _currentPosition = MutableStateFlow<Position?>(null)
    override val currentPosition: StateFlow<Position?> = _currentPosition

    private val _currentZoom = MutableStateFlow(0.0)
    override val currentZoom: StateFlow<Double> = _currentZoom

    // Queue for polygons that arrive before style loads
    // Only stores the most recent set since wave progression contains all previous circles
    private var pendingPolygons: List<Polygon>? = null
    private var styleLoaded = false

    // Cache for camera bounds calculations to avoid expensive getCameraForLatLngBounds calls
    private data class BoundsCacheKey(
        val bounds: BoundingBox,
        val mapWidth: Double,
        val mapHeight: Double,
        val isWindowMode: Boolean,
    )

    private val cameraBoundsCache = mutableMapOf<BoundsCacheKey, Double>()

    override fun getWidth(): Double {
        require(mapLibreMap != null)
        return mapLibreMap!!.width.toDouble()
    }

    override fun getHeight(): Double {
        require(mapLibreMap != null)
        return mapLibreMap!!.height.toDouble()
    }

    // -- Private properties

    private var currentMapClickListener: MapLibreMap.OnMapClickListener? = null

    /* ---------------------------------------------------------------------
     * Dynamic layers used for the wave rendering.  We keep their ids so we
     * can reliably remove them the next time addWavePolygons() is invoked or
     * when the caller requests a clear.
     * -------------------------------------------------------------------- */
    private val waveLayerIds = mutableListOf<String>()
    private val waveSourceIds = mutableListOf<String>()

    private var onMapSetCallbacks = mutableListOf<(AndroidMapLibreAdapter) -> Unit>()

    override fun setMap(map: MapLibreMap) {
        mapLibreMap = map

        // Update adapter with initial camera position
        updateCameraInfo()

        // Set camera movement listener to update position
        map.addOnCameraIdleListener {
            updateCameraInfo()
        }

        // Execute any pending callbacks
        onMapSetCallbacks.forEach { callback ->
            callback(this)
        }
        onMapSetCallbacks.clear()
    }

    override fun setStyle(
        stylePath: String,
        callback: () -> Unit?,
    ) {
        require(mapLibreMap != null)
        Log.i(TAG, "Applying style from URI: $stylePath")

        mapLibreMap!!.setStyle(Style.Builder().fromUri(stylePath)) { _ ->
            Log.i(TAG, "Style loaded successfully")
            styleLoaded = true

            // Invoke callback first to proceed with map setup
            callback()

            // Hybrid approach: minimum delay to let tiles start loading + IdleHandler for completion
            // MapLibre's tile rendering happens on background threads, so IdleHandler alone fires too early
            pendingPolygons?.let { polygons ->
                android.os.Handler(Looper.getMainLooper()).postDelayed({
                    // After minimum delay, wait for main thread to be idle before rendering waves
                    Looper.myQueue().addIdleHandler {
                        Log.i(TAG, "Rendering pending wave polygons after tile render delay + idle check")
                        addWavePolygons(polygons, clearExisting = true)
                        pendingPolygons = null
                        false // Remove handler after execution
                    }
                }, @Suppress("MagicNumber") 150) // 150ms minimum delay (50% faster than original 300ms, but allows tile loading)
            }
        }
    }

    override fun onMapSet(callback: (MapLibreAdapter<*>) -> Unit) {
        if (mapLibreMap != null) {
            // Map is already set, execute callback immediately
            callback(this)
        } else {
            // Store callback for execution when map is set
            onMapSetCallbacks.add(callback)
        }
    }

    // -- Setters -------------------------------------------------------------

    override fun setOnMapClickListener(listener: ((Double, Double) -> Unit)?) {
        mapLibreMap?.let { map ->

            // First remove any existing listener
            currentMapClickListener?.let { existingListener ->
                map.removeOnMapClickListener(existingListener)
                currentMapClickListener = null
            }

            // Then add the new listener if not null
            if (listener != null) {
                val newListener =
                    MapLibreMap.OnMapClickListener { point ->
                        listener(point.latitude, point.longitude)
                        true
                    }
                map.addOnMapClickListener(newListener)
                currentMapClickListener = newListener
            }
        }
    }

    override fun setMinZoomPreference(minZoom: Double) {
        require(mapLibreMap != null)
        mapLibreMap!!.setMinZoomPreference(minZoom)
    }

    override fun setMaxZoomPreference(maxZoom: Double) {
        require(mapLibreMap != null)
        mapLibreMap!!.setMaxZoomPreference(maxZoom)
    }

    override fun setAttributionMargins(
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
    ) {
        require(mapLibreMap != null)
        mapLibreMap!!.uiSettings.setAttributionMargins(left, top, right, bottom)
    }

    // ------------------------------------------------------------------------

    override fun addOnCameraIdleListener(callback: () -> Unit) {
        require(mapLibreMap != null)
        mapLibreMap!!.addOnCameraIdleListener(callback)
    }

    // Method to update the camera position and zoom
    private fun updateCameraInfo() {
        require(mapLibreMap != null)
        mapLibreMap!!.cameraPosition.target?.let { target ->
            _currentPosition.value = Position(target.latitude, target.longitude)
        }
        _currentZoom.value = mapLibreMap!!.cameraPosition.zoom
    }

    // -- Camera animations ---------------------------------------------------

    override fun getMinZoomLevel(): Double {
        require(mapLibreMap != null)
        // Return the calculated constraint-based min zoom if available (matches iOS pattern)
        // Otherwise fall back to MapLibre's tile-based minimum
        return if (calculatedMinZoom > 0.0) calculatedMinZoom else mapLibreMap!!.minZoomLevel
    }

    override fun getCameraPosition(): Position? {
        require(mapLibreMap != null)
        return mapLibreMap!!.cameraPosition.target?.let {
            Position(
                it.latitude,
                it.longitude,
            )
        }
    }

    override fun getVisibleRegion(): BoundingBox {
        require(mapLibreMap != null)
        return mapLibreMap!!.projection.visibleRegion.let { visibleRegion ->
            BoundingBox.fromCorners(
                Position(visibleRegion.latLngBounds.getLatSouth(), visibleRegion.latLngBounds.getLonWest()),
                Position(visibleRegion.latLngBounds.getLatNorth(), visibleRegion.latLngBounds.getLonEast()),
            )
        }
    }

    override fun moveCamera(bounds: BoundingBox) {
        require(mapLibreMap != null)
        val cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds.toLatLngBounds(), 0)
        mapLibreMap!!.moveCamera(cameraUpdate)
    }

    override fun animateCamera(
        position: Position,
        zoom: Double?,
        callback: MapCameraCallback?,
    ) {
        val map = mapLibreMap
        if (map == null) {
            Log.w("AndroidMapLibreAdapter", "animateCamera: Map not initialized, calling callback.onCancel()")
            callback?.onCancel()
            return
        }

        val builder =
            CameraPosition
                .Builder()
                .target(LatLng(position.latitude, position.longitude))

        if (zoom != null) {
            builder.zoom(zoom)
        }

        map.animateCamera(
            CameraUpdateFactory.newCameraPosition(builder.build()),
            WWWGlobals.Timing.MAP_CAMERA_ANIMATION_DURATION_MS,
            object : CancelableCallback {
                override fun onFinish() {
                    _currentZoom.value = map.cameraPosition.zoom
                    callback?.onFinish()
                }

                override fun onCancel() {
                    callback?.onCancel()
                }
            },
        )
    }

    // --------------------------------

    override fun animateCameraToBounds(
        bounds: BoundingBox,
        padding: Int,
        callback: MapCameraCallback?,
    ) {
        val map = mapLibreMap
        if (map == null) {
            Log.w("AndroidMapLibreAdapter", "animateCameraToBounds: Map not initialized, calling callback.onCancel()")
            callback?.onCancel()
            return
        }

        val latLngBounds =
            LatLngBounds
                .Builder()
                .include(LatLng(bounds.southwest.latitude, bounds.southwest.longitude))
                .include(LatLng(bounds.northeast.latitude, bounds.northeast.longitude))
                .build()

        map.animateCamera(
            CameraUpdateFactory.newLatLngBounds(latLngBounds, padding),
            WWWGlobals.Timing.MAP_CAMERA_ANIMATION_DURATION_MS,
            object : CancelableCallback {
                override fun onFinish() {
                    _currentZoom.value = map.cameraPosition.zoom
                    callback?.onFinish()
                }

                override fun onCancel() {
                    callback?.onCancel()
                }
            },
        )
    }

    // Track constraint bounds for viewport clamping (matches iOS pattern)
    private var currentConstraintBounds: BoundingBox? = null
    private var calculatedMinZoom: Double = 0.0
    private var eventBounds: BoundingBox? = null // Original event bounds (not shrunk)
    private var isGestureInProgress = false
    private var gestureConstraintsActive = false // Track if gesture listeners are already set up
    private var minZoomLocked = false // Track if min zoom has been set (to prevent zoom-in spiral)
    private var lastValidCameraPosition: LatLng? = null

    @Suppress("CyclomaticComplexMethod", "NestedBlockDepth")
    // Complex camera bounds logic - candidate for future refactoring
    override fun setBoundsForCameraTarget(
        constraintBounds: BoundingBox,
        applyZoomSafetyMargin: Boolean,
        originalEventBounds: BoundingBox?,
    ) {
        require(mapLibreMap != null)

        // Validate bounds before setting (matches iOS pattern)
        require(constraintBounds.ne.lat > constraintBounds.sw.lat) {
            "Invalid bounds: ne.lat (${constraintBounds.ne.lat}) must be > sw.lat (${constraintBounds.sw.lat})"
        }
        require(constraintBounds.sw.lat >= MIN_LATITUDE && constraintBounds.ne.lat <= MAX_LATITUDE) {
            "Latitude out of range: must be between $MIN_LATITUDE and $MAX_LATITUDE"
        }
        require(constraintBounds.sw.lng >= MIN_LONGITUDE && constraintBounds.ne.lng <= MAX_LONGITUDE) {
            "Longitude out of range: must be between $MIN_LONGITUDE and $MAX_LONGITUDE"
        }

        // Store constraint bounds for viewport clamping (matches iOS pattern)
        currentConstraintBounds = constraintBounds

        // Only calculate min zoom if not locked OR if we're now getting originalEventBounds
        // This ensures we calculate from ORIGINAL bounds (not shrunk), preventing infinite zoom out
        val shouldCalculateMinZoom = !minZoomLocked || (originalEventBounds != null && !minZoomLocked)

        if (shouldCalculateMinZoom && originalEventBounds != null) {
            val boundsForMinZoom = originalEventBounds

            // Different calculation for WINDOW vs BOUNDS mode
            val baseMinZoom: Double

            if (applyZoomSafetyMargin) {
                // WINDOW MODE: Fit the constraining dimension to prevent viewport overflow
                val eventWidth = boundsForMinZoom.ne.lng - boundsForMinZoom.sw.lng
                val eventHeight = boundsForMinZoom.ne.lat - boundsForMinZoom.sw.lat
                val mapWidth = getWidth()
                val mapHeight = getHeight()

                // Validate dimensions before calculation
                @Suppress("ComplexCondition") // Comprehensive dimension validation
                if (mapWidth <= 0 || mapHeight <= 0 || eventWidth <= 0 || eventHeight <= 0) {
                    Log.w(
                        "Camera",
                        "Invalid dimensions: map=${mapWidth}x$mapHeight, event=${eventWidth}x$eventHeight, " +
                            "using fallback min zoom",
                    )
                    calculatedMinZoom = mapLibreMap!!.minZoomLevel
                    mapLibreMap!!.setMinZoomPreference(calculatedMinZoom)
                    minZoomLocked = true
                    return
                }

                // Check cache first to avoid expensive calculation
                val cacheKey = BoundsCacheKey(boundsForMinZoom, mapWidth, mapHeight, isWindowMode = true)
                baseMinZoom = cameraBoundsCache[cacheKey] ?: run {
                    // Cache miss - calculate zoom
                    // Determine which dimension is constraining
                    val eventAspect = eventWidth / eventHeight
                    val screenAspect = mapWidth / mapHeight

                    // Calculate zoom for the constraining dimension
                    val constrainingBounds =
                        if (eventAspect > screenAspect) {
                            // Event wider than screen → constrained by HEIGHT
                            val constrainedWidth = eventHeight * screenAspect
                            val centerLng = (boundsForMinZoom.sw.lng + boundsForMinZoom.ne.lng) / 2.0
                            BoundingBox.fromCorners(
                                Position(boundsForMinZoom.sw.lat, centerLng - constrainedWidth / 2),
                                Position(boundsForMinZoom.ne.lat, centerLng + constrainedWidth / 2),
                            )
                        } else {
                            // Event taller than screen → constrained by WIDTH
                            val constrainedHeight = eventWidth / screenAspect
                            val centerLat = (boundsForMinZoom.sw.lat + boundsForMinZoom.ne.lat) / 2.0
                            BoundingBox.fromCorners(
                                Position(centerLat - constrainedHeight / 2, boundsForMinZoom.sw.lng),
                                Position(centerLat + constrainedHeight / 2, boundsForMinZoom.ne.lng),
                            )
                        }

                    val latLngBounds = constrainingBounds.toLatLngBounds()
                    val cameraPosition = mapLibreMap!!.getCameraForLatLngBounds(latLngBounds, intArrayOf(0, 0, 0, 0))
                    val calculatedZoom = cameraPosition?.zoom ?: mapLibreMap!!.minZoomLevel

                    // Cache the result
                    cameraBoundsCache[cacheKey] = calculatedZoom
                    Log.i("Camera", "Cached min zoom calculation for window mode: $calculatedZoom")
                    calculatedZoom
                }
            } else {
                // BOUNDS MODE: Use MapLibre's calculation (shows entire event)
                val mapWidth = getWidth()
                val mapHeight = getHeight()

                // Check cache first
                val cacheKey = BoundsCacheKey(boundsForMinZoom, mapWidth, mapHeight, isWindowMode = false)
                baseMinZoom = cameraBoundsCache[cacheKey] ?: run {
                    // Cache miss - calculate zoom
                    val latLngBounds = boundsForMinZoom.toLatLngBounds()
                    val cameraPosition = mapLibreMap!!.getCameraForLatLngBounds(latLngBounds, intArrayOf(0, 0, 0, 0))
                    val calculatedZoom = cameraPosition?.zoom ?: mapLibreMap!!.minZoomLevel

                    // Cache the result
                    cameraBoundsCache[cacheKey] = calculatedZoom
                    Log.i("Camera", "Cached min zoom calculation for bounds mode: $calculatedZoom")
                    calculatedZoom
                }
            }

            // Base min zoom ensures event fits in viewport
            calculatedMinZoom = baseMinZoom

            // Set min zoom preference
            mapLibreMap!!.setMinZoomPreference(calculatedMinZoom)
            minZoomLocked = true
            Log.i("Camera", "Min zoom set: $calculatedMinZoom (${if (applyZoomSafetyMargin) "window mode" else "bounds mode"})")
        } else if (!shouldCalculateMinZoom) {
            Log.i("Camera", "Min zoom already locked at $calculatedMinZoom")
        }

        // Set the underlying MapLibre bounds (constrains camera center only)
        mapLibreMap!!.setLatLngBoundsForCameraTarget(constraintBounds.toLatLngBounds())

        // Setup preventive gesture constraints to enforce viewport bounds
        // Constraint bounds are now calculated from viewport at MIN ZOOM (not current)
        // This allows zooming to min zoom while preventing viewport overflow
        if (applyZoomSafetyMargin && !gestureConstraintsActive) {
            setupPreventiveGestureConstraints()
            gestureConstraintsActive = true
        }
    }

    /**
     * Setup preventive gesture constraints to prevent camera from moving outside bounds.
     * This provides iOS-like gesture clamping by intercepting movements during gestures.
     * Called once when constraints are first applied.
     */
    private fun setupPreventiveGestureConstraints() {
        val map = mapLibreMap ?: return

        Log.i(TAG, "Setting up preventive gesture constraints")

        // Track when gestures start
        map.addOnCameraMoveStartedListener { reason ->
            isGestureInProgress =
                when (reason) {
                    MapLibreMap.OnCameraMoveStartedListener.REASON_API_GESTURE,
                    MapLibreMap.OnCameraMoveStartedListener.REASON_API_ANIMATION,
                    MapLibreMap.OnCameraMoveStartedListener.REASON_DEVELOPER_ANIMATION,
                    -> false // All programmatic movements (including button animations)
                    else -> true // Only user gestures (pan, pinch, etc.)
                }

            if (!isGestureInProgress) {
                // Save position before programmatic animation
                lastValidCameraPosition = map.cameraPosition.target
            }
        }

        // Intercept camera movements during gestures (PREVENTIVE)
        map.addOnCameraMoveListener {
            if (!isGestureInProgress) return@addOnCameraMoveListener

            val currentCamera = map.cameraPosition.target ?: return@addOnCameraMoveListener
            val viewport = getVisibleRegion()
            val constraintBounds = currentConstraintBounds ?: return@addOnCameraMoveListener

            // Check if viewport exceeds event bounds
            if (!isViewportWithinBounds(viewport, constraintBounds)) {
                // Viewport exceeds bounds - clamp camera position immediately
                val clampedPosition =
                    clampCameraToKeepViewportInside(
                        Position(currentCamera.latitude, currentCamera.longitude),
                        viewport,
                        constraintBounds,
                    )

                // Only move if position actually changed (avoid infinite loop)
                @Suppress("MagicNumber") // Epsilon for floating point comparison
                if (kotlin.math.abs(clampedPosition.latitude - currentCamera.latitude) > 0.000001 ||
                    kotlin.math.abs(clampedPosition.longitude - currentCamera.longitude) > 0.000001
                ) {
                    // Move camera to clamped position without animation
                    map.cameraPosition =
                        CameraPosition
                            .Builder()
                            .target(LatLng(clampedPosition.latitude, clampedPosition.longitude))
                            .zoom(map.cameraPosition.zoom)
                            .build()
                }
            }
        }

        // Track when gesture ends
        map.addOnCameraIdleListener {
            if (isGestureInProgress) {
                // Gesture ended - save this as last valid position
                lastValidCameraPosition = map.cameraPosition.target
                isGestureInProgress = false
            }
        }

        Log.i(TAG, "Preventive gesture constraints active")
    }

    /**
     * Check if entire viewport is within event bounds.
     */
    private fun isViewportWithinBounds(
        viewport: BoundingBox,
        eventBounds: BoundingBox,
    ): Boolean =
        viewport.southLatitude >= eventBounds.southwest.latitude &&
            viewport.northLatitude <= eventBounds.northeast.latitude &&
            viewport.westLongitude >= eventBounds.southwest.longitude &&
            viewport.eastLongitude <= eventBounds.northeast.longitude

    /**
     * Clamp camera position to ensure viewport stays within event bounds.
     */
    private fun clampCameraToKeepViewportInside(
        currentCamera: Position,
        currentViewport: BoundingBox,
        eventBounds: BoundingBox,
    ): Position {
        // Calculate viewport half-dimensions
        val viewportHalfHeight = (currentViewport.northLatitude - currentViewport.southLatitude) / 2.0
        val viewportHalfWidth = (currentViewport.eastLongitude - currentViewport.westLongitude) / 2.0

        // Calculate valid camera center range
        val minValidLat = eventBounds.southwest.latitude + viewportHalfHeight
        val maxValidLat = eventBounds.northeast.latitude - viewportHalfHeight
        val minValidLng = eventBounds.southwest.longitude + viewportHalfWidth
        val maxValidLng = eventBounds.northeast.longitude - viewportHalfWidth

        // Handle case where viewport > event bounds (shouldn't happen with min zoom)
        if (minValidLat > maxValidLat || minValidLng > maxValidLng) {
            val centerLat = (eventBounds.southwest.latitude + eventBounds.northeast.latitude) / 2.0
            val centerLng = (eventBounds.southwest.longitude + eventBounds.northeast.longitude) / 2.0
            return Position(centerLat, centerLng)
        }

        // Clamp camera position to valid range
        val clampedLat = currentCamera.latitude.coerceIn(minValidLat, maxValidLat)
        val clampedLng = currentCamera.longitude.coerceIn(minValidLng, maxValidLng)

        return Position(clampedLat, clampedLng)
    }

    // -- Add the Wave polygons to the map

    @Suppress("ReturnCount") // Multiple returns OK for guard clauses (null check, empty check, style not loaded)
    override fun addWavePolygons(
        polygons: List<Any>,
        clearExisting: Boolean,
    ) {
        val map = mapLibreMap ?: return
        val wavePolygons = polygons.filterIsInstance<Polygon>()

        // Handle clearing polygons when empty list is provided with clearExisting flag
        // This is used when simulation stops to remove all wave polygons from the map
        if (wavePolygons.isEmpty() && clearExisting) {
            Log.i(TAG, "Clearing all wave polygons from map (${waveLayerIds.size} layers)")
            // Clear pending polygons if style not loaded yet
            pendingPolygons = null
            // Clear map layers if style is loaded
            map.getStyle { style ->
                try {
                    // Remove all wave polygon layers and sources
                    waveLayerIds.forEach { layerId ->
                        style.removeLayer(layerId)
                    }
                    waveSourceIds.forEach { sourceId ->
                        style.removeSource(sourceId)
                    }
                    // Clear tracking arrays
                    waveLayerIds.clear()
                    waveSourceIds.clear()
                } catch (e: Exception) {
                    Log.e(TAG, "Error clearing wave polygons", e)
                }
            }
            return
        }

        if (wavePolygons.isEmpty()) {
            Log.w(TAG, "No valid Polygon objects found in ${polygons.size} input polygons")
            return
        }

        // If style not loaded yet, store most recent polygons for later
        // Only the most recent set matters (wave progression contains all previous circles)
        if (!styleLoaded) {
            Log.w(TAG, "Style not ready - storing ${wavePolygons.size} polygons (most recent)")
            pendingPolygons = wavePolygons
            return
        }

        // Style is loaded - render with iOS-style layer reuse pattern
        Log.d(TAG, "Rendering ${wavePolygons.size} wave polygons")
        map.getStyle { style ->
            try {
                // Phase 1: Remove excess layers if polygon count decreased (iOS pattern)
                if (wavePolygons.size < waveLayerIds.size) {
                    for (index in wavePolygons.size until waveLayerIds.size) {
                        style.removeLayer(waveLayerIds[index])
                        style.removeSource(waveSourceIds[index])
                    }
                    // Shrink arrays to match current polygon count
                    waveLayerIds.subList(wavePolygons.size, waveLayerIds.size).clear()
                    waveSourceIds.subList(wavePolygons.size, waveSourceIds.size).clear()
                }

                // Phase 2: Update or create each polygon layer (iOS pattern)
                wavePolygons.forEachIndexed { index, polygon ->
                    // Use deterministic IDs (index-based, not UUID-based) for layer reuse
                    val sourceId =
                        if (index < waveSourceIds.size) {
                            waveSourceIds[index] // Reuse existing ID
                        } else {
                            "wave-polygons-source-$index" // Deterministic, no UUID
                        }
                    val layerId =
                        if (index < waveLayerIds.size) {
                            waveLayerIds[index] // Reuse existing ID
                        } else {
                            "wave-polygons-layer-$index" // Deterministic, no UUID
                        }

                    if (index < waveSourceIds.size) {
                        // Phase 3A: Update existing source (no flickering)
                        updateExistingPolygon(style, sourceId, layerId, polygon)
                    } else {
                        // Phase 3B: Add new polygon (first time or expansion)
                        addNewPolygon(style, sourceId, layerId, polygon)
                    }
                }
                Log.d(TAG, "Successfully rendered ${wavePolygons.size} wave polygons")
            } catch (ise: IllegalStateException) {
                Log.e(TAG, "Map style in invalid state for wave polygons", ise)
            } catch (iae: IllegalArgumentException) {
                Log.e(TAG, "Invalid arguments for wave polygon styling", iae)
            } catch (uoe: UnsupportedOperationException) {
                Log.e(TAG, "Unsupported map operation", uoe)
            }
        }
    }

    /**
     * Updates an existing polygon source with new geometry (iOS pattern).
     * Prevents flickering by updating geometry in place rather than recreating layers.
     */
    private fun updateExistingPolygon(
        style: Style,
        sourceId: String,
        layerId: String,
        polygon: Polygon,
    ) {
        val source = style.getSource(sourceId) as? GeoJsonSource
        if (source != null) {
            // Update geometry only (prevents flickering) - iOS pattern
            source.setGeoJson(Feature.fromGeometry(polygon))
        } else {
            // Fallback: recreate if source missing (shouldn't happen)
            Log.w(TAG, "Source $sourceId missing, recreating layer")
            val newSource =
                GeoJsonSource(sourceId).apply {
                    setGeoJson(Feature.fromGeometry(polygon))
                }
            style.addSource(newSource)

            val layer =
                FillLayer(layerId, sourceId).withProperties(
                    PropertyFactory.fillColor(Wave.BACKGROUND_COLOR.toColorInt()),
                    PropertyFactory.fillOpacity(Wave.BACKGROUND_OPACITY),
                )
            style.addLayer(layer)
        }
    }

    /**
     * Adds a new polygon layer and tracks its IDs for future reuse (iOS pattern).
     */
    private fun addNewPolygon(
        style: Style,
        sourceId: String,
        layerId: String,
        polygon: Polygon,
    ) {
        val source =
            GeoJsonSource(sourceId).apply {
                setGeoJson(Feature.fromGeometry(polygon))
            }
        style.addSource(source)

        val layer =
            FillLayer(layerId, sourceId).withProperties(
                PropertyFactory.fillColor(Wave.BACKGROUND_COLOR.toColorInt()),
                PropertyFactory.fillOpacity(Wave.BACKGROUND_OPACITY),
            )
        style.addLayer(layer)

        // Track IDs for future reuse (iOS pattern)
        waveSourceIds.add(sourceId)
        waveLayerIds.add(layerId)
    }

    // --------------------------------

    override fun drawOverridenBbox(bbox: BoundingBox) {
        require(mapLibreMap != null)

        mapLibreMap!!.style?.let { style ->
            val rectangleCoordinates =
                listOf(
                    listOf(
                        Point.fromLngLat(bbox.sw.lng, bbox.sw.lat),
                        Point.fromLngLat(bbox.ne.lng, bbox.sw.lat),
                        Point.fromLngLat(bbox.ne.lng, bbox.ne.lat),
                        Point.fromLngLat(bbox.sw.lng, bbox.ne.lat),
                        Point.fromLngLat(bbox.sw.lng, bbox.sw.lat),
                    ),
                )

            style.addSource(GeoJsonSource("bbox-override-source", Polygon.fromLngLats(rectangleCoordinates)))

            style.addLayer(
                LineLayer("bbox-override-line", "bbox-override-source").apply {
                    setProperties(
                        lineColor(Color.RED),
                        lineWidth(1f),
                        lineOpacity(1.0f),
                        lineDasharray(arrayOf(5f, 2f)),
                    )
                },
            )
        }
    }

    override fun enableLocationComponent(enabled: Boolean) {
        // On Android, location component is managed by AndroidEventMap
        // Location component is activated through setupMapLocationComponent() in AndroidEventMap.kt
    }

    override fun setUserPosition(position: Position) {
        // On Android, user position is managed automatically by LocationEngineProxy
        // Position updates flow from LocationProvider through LocationEngine to MapLibre
    }

    override fun setGesturesEnabled(enabled: Boolean) {
        require(mapLibreMap != null)

        mapLibreMap!!.uiSettings.apply {
            // Enable/disable scroll and zoom gestures
            isScrollGesturesEnabled = enabled
            isZoomGesturesEnabled = enabled
            // Rotation and tilt always disabled (not controlled by enabled parameter)
            isRotateGesturesEnabled = false
            isTiltGesturesEnabled = false
        }
    }

    /**
     * Temporarily removes native MapLibre camera target constraints.
     * Clears setLatLngBoundsForCameraTarget() by passing null.
     * Must be paired with restoreConstraints() - use try-finally pattern.
     */
    override fun temporarilyRemoveConstraints() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            Log.w(TAG, "temporarilyRemoveConstraints called off main thread - ignoring")
            return
        }

        val map = mapLibreMap
        if (map == null) {
            Log.w(TAG, "temporarilyRemoveConstraints: Map not initialized")
            return
        }

        Log.d(TAG, "Temporarily removing camera target constraints")
        map.setLatLngBoundsForCameraTarget(null)
    }

    /**
     * Restores previously set native MapLibre camera target constraints.
     * Must be called after temporarilyRemoveConstraints() to re-enable gesture safety.
     */
    override fun restoreConstraints() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            Log.w(TAG, "restoreConstraints called off main thread - ignoring")
            return
        }

        val map = mapLibreMap
        if (map == null) {
            Log.w(TAG, "restoreConstraints: Map not initialized")
            return
        }

        val bounds = currentConstraintBounds
        if (bounds == null) {
            Log.d(TAG, "No cached constraint bounds to restore")
            return
        }

        Log.d(TAG, "Restoring camera target constraints: $bounds")
        map.setLatLngBoundsForCameraTarget(bounds.toLatLngBounds())
    }
}
