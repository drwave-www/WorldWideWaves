package com.worldwidewaves.map

import android.util.Log
import androidx.core.graphics.toColorInt
import com.worldwidewaves.compose.MapLibreConstraintHandler
import com.worldwidewaves.shared.events.utils.BoundingBox
import com.worldwidewaves.shared.events.utils.Position
import com.worldwidewaves.shared.map.MapCameraCallback
import com.worldwidewaves.shared.map.PlatformMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapLibreMap.CancelableCallback
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Polygon

/**
 * MapLibre adapter that implements the PlatformMap interface
 */
class MapLibreAdapter(private var mapLibreMap: MapLibreMap? = null) : PlatformMap {
    private val _isLoaded = MutableStateFlow(false)
    override val isLoaded: StateFlow<Boolean> = _isLoaded

    private val _isError = MutableStateFlow(false)
    override val isError: StateFlow<Boolean> = _isError

    private val _currentPosition = MutableStateFlow<Position?>(null)
    override val currentPosition: StateFlow<Position?> = _currentPosition

    private val _currentZoom = MutableStateFlow(0.0)
    override val currentZoom: StateFlow<Double> = _currentZoom

    private var animationInProgress = false
    private var constraintHandler: MapLibreConstraintHandler? = null

    private var currentMapClickListener: MapLibreMap.OnMapClickListener? = null

    fun setLoaded(loaded: Boolean) {
        _isLoaded.value = loaded
    }

    fun setError(error: Boolean) {
        _isError.value = error
    }

    fun setMap(map: MapLibreMap) {
        mapLibreMap = map
    }

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

    override fun animateCamera(position: Position, zoom: Double?, callback: MapCameraCallback?) {
        if (animationInProgress) return
        val map = mapLibreMap ?: return

        animationInProgress = true

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
                    animationInProgress = false
                    _currentZoom.value = map.cameraPosition.zoom
                    callback?.onFinish()
                }
                override fun onCancel() {
                    animationInProgress = false
                    callback?.onCancel()
                }
            }
        )
    }

    override fun animateCameraToBounds(bounds: BoundingBox, padding: Int, callback: MapCameraCallback?) {
        if (animationInProgress) return
        val map = mapLibreMap ?: return

        animationInProgress = true

        val latLngBounds = LatLngBounds.Builder()
            .include(LatLng(bounds.southwest.latitude, bounds.southwest.longitude))
            .include(LatLng(bounds.northeast.latitude, bounds.northeast.longitude))
            .build()

        map.animateCamera(
            CameraUpdateFactory.newLatLngBounds(latLngBounds, padding),
            500, // Animation duration
            object : CancelableCallback {
                override fun onFinish() {
                    animationInProgress = false
                    _currentZoom.value = map.cameraPosition.zoom
                    callback?.onFinish()
                }
                override fun onCancel() {
                    animationInProgress = false
                    callback?.onCancel()
                }
            }
        )
    }

    override fun setConstraints(bounds: BoundingBox) {
        val map = mapLibreMap ?: return

        constraintHandler = MapLibreConstraintHandler(bounds)
        constraintHandler?.applyConstraints(map)
    }

    override fun setMinZoomPreference(minZoom: Double) {
        mapLibreMap?.setMinZoomPreference(minZoom)
    }

    override fun setMaxZoomPreference(maxZoom: Double) {
        mapLibreMap?.setMaxZoomPreference(maxZoom)
    }

    override fun addWavePolygons(polygons: List<Any>, clearExisting: Boolean) {
        val map = mapLibreMap ?: return
        val wavePolygons = polygons.filterIsInstance<Polygon>()

        map.getStyle { style ->
            val sourceId = "wave-polygons-source"
            val layerId = "wave-polygons-layer"

            try {
                if (clearExisting) {
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

    // Method to update the camera position and zoom
    fun updateCameraInfo(map: MapLibreMap) {
        map.cameraPosition.target?.let { target ->
            _currentPosition.value = Position(target.latitude, target.longitude)
        }
        _currentZoom.value = map.cameraPosition.zoom
    }

    // Check and constrain camera if needed
    fun constrainCamera() {
        mapLibreMap?.let { map ->
            constraintHandler?.constrainCamera(map)
        }
    }
}