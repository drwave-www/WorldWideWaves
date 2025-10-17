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
import java.util.UUID

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
        private const val TAG = "AndroidMapLibreAdapter"
        private const val MIN_LATITUDE = -90.0
        private const val MAX_LATITUDE = 90.0
        private const val MIN_LONGITUDE = -180.0
        private const val MAX_LONGITUDE = 180.0
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

        // Debug: log initial camera details
        Log.d(
            "Camera",
            "Initial camera: target=${map.cameraPosition.target?.latitude}," +
                "${map.cameraPosition.target?.longitude} " +
                "zoom=${map.cameraPosition.zoom}",
        )

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
        // Log style application start – helps diagnose early style failures
        Log.d(TAG, "Applying style from URI: $stylePath")

        mapLibreMap!!.setStyle(Style.Builder().fromUri(stylePath)) { _ ->
            // Log successful style load – confirms MapLibre has parsed the style
            Log.i(TAG, "Style loaded successfully")
            styleLoaded = true

            // Render pending polygons that arrived before style loaded
            // Only the most recent set matters (wave progression contains all previous circles)
            pendingPolygons?.let { polygons ->
                Log.i(TAG, "Rendering pending polygons: ${polygons.size} polygons")
                addWavePolygons(polygons, clearExisting = true)
                pendingPolygons = null
            }

            callback()
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
        return mapLibreMap!!.minZoomLevel
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
        Log.v(
            "Camera",
            "Moving camera to bounds: SW=${bounds.southwest.latitude},${bounds.southwest.longitude} " +
                "NE=${bounds.northeast.latitude},${bounds.northeast.longitude}",
        )
        val cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds.toLatLngBounds(), 0)
        mapLibreMap!!.moveCamera(cameraUpdate)
    }

    override fun animateCamera(
        position: Position,
        zoom: Double?,
        callback: MapCameraCallback?,
    ) {
        val map = mapLibreMap ?: return

        Log.v(
            "Camera",
            "Animating to position: lat=${position.latitude}, " +
                "lng=${position.longitude}, zoom=$zoom",
        )

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
        val map = mapLibreMap ?: return

        Log.v(
            "Camera",
            "Animating to bounds: SW=${bounds.southwest.latitude}," +
                "${bounds.southwest.longitude} " +
                "NE=${bounds.northeast.latitude},${bounds.northeast.longitude} " +
                "padding=$padding",
        )

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

    override fun setBoundsForCameraTarget(constraintBounds: BoundingBox) {
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

        Log.v(
            "Camera",
            "Setting camera target bounds constraint: SW=${constraintBounds.southwest.latitude}," +
                "${constraintBounds.southwest.longitude} " +
                "NE=${constraintBounds.northeast.latitude},${constraintBounds.northeast.longitude}",
        )

        // Store constraint bounds for viewport clamping (matches iOS pattern)
        currentConstraintBounds = constraintBounds

        // Set the underlying MapLibre bounds (constrains camera center only)
        mapLibreMap!!.setLatLngBoundsForCameraTarget(constraintBounds.toLatLngBounds())
    }

    // -- Add the Wave polygons to the map

    @Suppress("ReturnCount") // Multiple returns OK for guard clauses (null check, empty check, style not loaded)
    override fun addWavePolygons(
        polygons: List<Any>,
        clearExisting: Boolean,
    ) {
        val map = mapLibreMap ?: return
        val wavePolygons = polygons.filterIsInstance<Polygon>()
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

        // Style is loaded - render immediately
        map.getStyle { style ->
            try {
                // -- Clear existing dynamic layers/sources when requested ----
                if (clearExisting) {
                    waveLayerIds.forEach { style.removeLayer(it) }
                    waveSourceIds.forEach { style.removeSource(it) }
                    waveLayerIds.clear()
                    waveSourceIds.clear()
                }

                // -- Add each polygon on its own layer -----------------------
                wavePolygons.forEachIndexed { index, polygon ->
                    // Add UUID to prevent ID conflicts during rapid updates (matches iOS pattern)
                    val uuid = UUID.randomUUID().toString()
                    val sourceId = "wave-polygons-source-$index-$uuid"
                    val layerId = "wave-polygons-layer-$index-$uuid"

                    // GeoJSON source with a single polygon
                    val src =
                        GeoJsonSource(sourceId).apply {
                            setGeoJson(Feature.fromGeometry(polygon))
                        }
                    style.addSource(src)

                    // Fill layer
                    val layer =
                        FillLayer(layerId, sourceId).withProperties(
                            PropertyFactory.fillColor(Wave.BACKGROUND_COLOR.toColorInt()),
                            PropertyFactory.fillOpacity(Wave.BACKGROUND_OPACITY),
                        )
                    style.addLayer(layer)

                    // Track for next cleanup
                    waveSourceIds.add(sourceId)
                    waveLayerIds.add(layerId)
                }
            } catch (ise: IllegalStateException) {
                Log.e("MapUpdate", "Map style in invalid state for wave polygons", ise)
            } catch (iae: IllegalArgumentException) {
                Log.e("MapUpdate", "Invalid arguments for wave polygon styling", iae)
            } catch (uoe: UnsupportedOperationException) {
                Log.e("MapUpdate", "Unsupported map operation", uoe)
            }
        }
    }

    // --------------------------------

    override fun drawOverridenBbox(bbox: BoundingBox) {
        require(mapLibreMap != null)

        Log.v(
            "Camera",
            "Drawing override bbox: SW=${bbox.southwest.latitude}," +
                "${bbox.southwest.longitude} " +
                "NE=${bbox.northeast.latitude},${bbox.northeast.longitude}",
        )

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
        // This is a no-op since the location component is activated through
        // setupMapLocationComponent() in AndroidEventMap.kt
        Log.d(TAG, "enableLocationComponent: $enabled (no-op on Android, managed by AndroidEventMap)")
    }

    override fun setUserPosition(position: Position) {
        // On Android, user position is managed automatically by LocationEngineProxy
        // which feeds positions from LocationProvider to MapLibre's location component
        // This is a no-op since the position updates happen through the LocationEngine
        Log.v(TAG, "setUserPosition: (${position.lat}, ${position.lng}) (no-op on Android, managed by LocationEngineProxy)")
    }
}
