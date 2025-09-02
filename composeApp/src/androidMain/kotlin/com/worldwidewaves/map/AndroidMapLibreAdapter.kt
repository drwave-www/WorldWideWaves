package com.worldwidewaves.map

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

import android.graphics.Color
import android.util.Log
import androidx.core.graphics.toColorInt
import com.worldwidewaves.shared.WWWGlobals.Companion.WAVE_BACKGROUND_COLOR
import com.worldwidewaves.shared.WWWGlobals.Companion.WAVE_BACKGROUND_OPACITY
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
 * MapLibre adapter that implements the PlatformMap interface
 */
class AndroidMapLibreAdapter(private var mapLibreMap: MapLibreMap? = null) : MapLibreAdapter<MapLibreMap> {

    // -- Public/Override properties

    private val _currentPosition = MutableStateFlow<Position?>(null)
    override val currentPosition: StateFlow<Position?> = _currentPosition

    private val _currentZoom = MutableStateFlow(0.0)
    override val currentZoom: StateFlow<Double> = _currentZoom

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

    // --------------------------------

    /* ---------------------------------------------------------------------
     * Dynamic layers used for the wave rendering.  We keep their ids so we
     * can reliably remove them the next time addWavePolygons() is invoked or
     * when the caller requests a clear.
     * -------------------------------------------------------------------- */
    private val waveLayerIds  = mutableListOf<String>()
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

    override fun setStyle(stylePath: String, callback: () -> Unit?) {
        require(mapLibreMap != null)
        mapLibreMap!!.setStyle(Style.Builder().fromUri(stylePath)) { style -> callback() }
    }

    fun onMapSet(callback: (AndroidMapLibreAdapter) -> Unit) {
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
                val newListener = MapLibreMap.OnMapClickListener { point ->
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

    override fun setAttributionMargins(left: Int, top: Int, right: Int, bottom: Int) {
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
                it.longitude
            )
        }
    }

    override fun getVisibleRegion(): BoundingBox {
        require(mapLibreMap != null)
        return mapLibreMap!!.projection.visibleRegion.let { visibleRegion ->
            BoundingBox.fromCorners(
                Position(visibleRegion.latLngBounds.getLatSouth(), visibleRegion.latLngBounds.getLonWest()),
                Position(visibleRegion.latLngBounds.getLatNorth(), visibleRegion.latLngBounds.getLonEast())
            )
        }
    }

    override fun moveCamera(bounds: BoundingBox) {
        require(mapLibreMap != null)
        val cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds.toLatLngBounds(), 0)
        mapLibreMap!!.moveCamera(cameraUpdate)
    }

    override fun animateCamera(position: Position, zoom: Double?, callback: MapCameraCallback?) {
        val map = mapLibreMap ?: return

        val builder = CameraPosition.Builder()
            .target(LatLng(position.latitude, position.longitude))

        if (zoom != null) {
            builder.zoom(zoom)
        }

        map.animateCamera(
            CameraUpdateFactory.newCameraPosition(builder.build()),
            500, // Animation duration
            object : CancelableCallback {
                override fun onFinish() {
                    _currentZoom.value = map.cameraPosition.zoom
                    callback?.onFinish()
                }
                override fun onCancel() {
                    callback?.onCancel()
                }
            }
        )
    }

    // --------------------------------

    override fun animateCameraToBounds(bounds: BoundingBox, padding: Int, callback: MapCameraCallback?) {
        val map = mapLibreMap ?: return

        val latLngBounds = LatLngBounds.Builder()
            .include(LatLng(bounds.southwest.latitude, bounds.southwest.longitude))
            .include(LatLng(bounds.northeast.latitude, bounds.northeast.longitude))
            .build()

        map.animateCamera(
            CameraUpdateFactory.newLatLngBounds(latLngBounds, padding),
            500, // Animation duration
            object : CancelableCallback {
                override fun onFinish() {
                    _currentZoom.value = map.cameraPosition.zoom
                    Log.i(::animateCameraToBounds.name, "Current Map zoom level: ${_currentZoom.value}")
                    callback?.onFinish()
                }
                override fun onCancel() {
                    callback?.onCancel()
                }
            }
        )
    }

    override fun setBoundsForCameraTarget(constraintBounds: BoundingBox) {
        require(mapLibreMap != null)
        mapLibreMap!!.setLatLngBoundsForCameraTarget(constraintBounds.toLatLngBounds())
    }

    // -- Add the Wave polygons to the map

    override fun addWavePolygons(polygons: List<Any>, clearExisting: Boolean) {
        val map = mapLibreMap ?: return
        val wavePolygons = polygons.filterIsInstance<Polygon>()
        if (wavePolygons.isEmpty()) return

        map.getStyle { style ->
            try {
                /* -- Clear existing dynamic layers/sources when requested ---- */
                if (clearExisting) {
                    waveLayerIds.forEach { style.removeLayer(it) }
                    waveSourceIds.forEach { style.removeSource(it) }
                    waveLayerIds.clear()
                    waveSourceIds.clear()
                }

                /* -- Add each polygon on its own layer ----------------------- */
                wavePolygons.forEachIndexed { index, polygon ->
                    val sourceId = "wave-polygons-source-$index"
                    val layerId  = "wave-polygons-layer-$index"

                    /* Defensive cleanup in case ids are reused                */
                    style.removeLayer(layerId)
                    style.removeSource(sourceId)

                    /* GeoJSON source with a single polygon                    */
                    val src = GeoJsonSource(sourceId).apply {
                        setGeoJson(Feature.fromGeometry(polygon))
                    }
                    style.addSource(src)

                    /* Fill layer                                              */
                    val layer = FillLayer(layerId, sourceId).withProperties(
                        PropertyFactory.fillColor(WAVE_BACKGROUND_COLOR.toColorInt()),
                        PropertyFactory.fillOpacity(WAVE_BACKGROUND_OPACITY)
                    )
                    style.addLayer(layer)

                    /* Track for next cleanup                                  */
                    waveSourceIds.add(sourceId)
                    waveLayerIds.add(layerId)
                }
            } catch (e: Exception) {
                Log.e("MapUpdate", "Error updating wave polygons", e)
            }
        }
    }

    // --------------------------------

    override fun drawOverridenBbox(bbox: BoundingBox) {
        require(mapLibreMap != null)

        mapLibreMap!!.style?.let { style ->
            val rectangleCoordinates = listOf(
                listOf(
                    Point.fromLngLat(bbox.sw.lng, bbox.sw.lat),
                    Point.fromLngLat(bbox.ne.lng, bbox.sw.lat),
                    Point.fromLngLat(bbox.ne.lng, bbox.ne.lat),
                    Point.fromLngLat(bbox.sw.lng, bbox.ne.lat),
                    Point.fromLngLat(bbox.sw.lng, bbox.sw.lat)
                )
            )

            style.addSource(GeoJsonSource("bbox-override-source", Polygon.fromLngLats(rectangleCoordinates)))

            style.addLayer(LineLayer("bbox-override-line", "bbox-override-source").apply {
                setProperties(
                    lineColor(Color.RED),
                    lineWidth(1f),
                    lineOpacity(1.0f),
                    lineDasharray(arrayOf(5f, 2f))
                )
            })
        }
    }

}