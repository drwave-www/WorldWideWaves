package com.worldwidewaves.map

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

import android.util.Log
import androidx.core.graphics.toColorInt
import com.worldwidewaves.shared.WWWGlobals.Companion.WAVE_BACKGROUND_COLOR
import com.worldwidewaves.shared.WWWGlobals.Companion.WAVE_BACKGROUND_OPACITY
import com.worldwidewaves.shared.events.utils.BoundingBox
import com.worldwidewaves.shared.events.utils.Position
import com.worldwidewaves.shared.map.BaseMapLibreAdapter
import com.worldwidewaves.shared.map.ConstraintEnforcer
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
 * Android-specific implementation of MapLibreAdapter
 * Uses MapLibre Android SDK for map operations
 */
class AndroidMapLibreAdapter(
    private var mapLibreMap: MapLibreMap? = null
) : BaseMapLibreAdapter(), ConstraintEnforcer {

    // -- Private properties --
    
    private var currentMapClickListener: MapLibreMap.OnMapClickListener? = null
    private var onMapSetCallbacks = mutableListOf<(AndroidMapLibreAdapter) -> Unit>()
    
    // -- Map initialization --
    
    /**
     * Sets the MapLibre map instance and initializes the adapter
     */
    fun setMap(map: MapLibreMap) {
        mapLibreMap = map

        // Update adapter with initial camera position
        updateCameraInfoFromMap(map)
        
        // Calculate initial visible region padding
        updateVisibleRegionPadding(map)

        // Set camera movement listener to update position and padding
        map.addOnCameraIdleListener {
            updateCameraInfoFromMap(map)
            updateVisibleRegionPadding(map)
            constrainCamera()
        }

        // Execute any pending callbacks
        onMapSetCallbacks.forEach { callback ->
            callback(this)
        }
        onMapSetCallbacks.clear()
        
        // Notify that the map is ready
        onMapReady()
    }

    /**
     * Register a callback to be executed when the map is set
     */
    fun onMapSet(callback: (AndroidMapLibreAdapter) -> Unit) {
        if (mapLibreMap != null) {
            // Map is already set, execute callback immediately
            callback(this)
        } else {
            // Store callback for execution when map is set
            onMapSetCallbacks.add(callback)
        }
    }

    // -- Platform-specific implementations --
    
    /**
     * Update camera information from the MapLibre map
     */
    private fun updateCameraInfoFromMap(map: MapLibreMap) {
        map.cameraPosition.target?.let { target ->
            updateCameraInfo(Position(target.latitude, target.longitude), map.cameraPosition.zoom)
        }
    }
    
    /**
     * Updates the visible region padding in the camera manager
     * This is crucial for the constraint system to work properly
     */
    private fun updateVisibleRegionPadding(map: MapLibreMap) {
        // Get the visible region from the current map view
        val visibleRegion = map.projection.visibleRegion
        
        // Calculate padding as half the visible region dimensions
        val latPadding = (visibleRegion.latLngBounds.getLatNorth() -
                visibleRegion.latLngBounds.getLatSouth()) / 2.0
        val lngPadding = (visibleRegion.latLngBounds.getLonEast() -
                visibleRegion.latLngBounds.getLonWest()) / 2.0
        
        // Update the camera manager with new padding values
        cameraManager.updateVisibleRegionPadding(latPadding, lngPadding)
    }

    /**
     * Platform-specific implementation of camera animation
     */
    override fun performAnimateCamera(position: Position, zoom: Double?, onComplete: (Boolean) -> Unit) {
        val map = mapLibreMap ?: run {
            onComplete(false)
            return
        }

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
                    updateCameraInfoFromMap(map)
                    onComplete(true)
                }
                override fun onCancel() {
                    onComplete(false)
                }
            }
        )
    }

    /**
     * Platform-specific implementation of bounds animation
     */
    override fun performAnimateCameraToBounds(bounds: BoundingBox, padding: Int, onComplete: (Boolean) -> Unit) {
        val map = mapLibreMap ?: run {
            onComplete(false)
            return
        }

        val latLngBounds = LatLngBounds.Builder()
            .include(LatLng(bounds.southwest.latitude, bounds.southwest.longitude))
            .include(LatLng(bounds.northeast.latitude, bounds.northeast.longitude))
            .build()

        map.animateCamera(
            CameraUpdateFactory.newLatLngBounds(latLngBounds, padding),
            500, // Animation duration
            object : CancelableCallback {
                override fun onFinish() {
                    updateCameraInfoFromMap(map)
                    onComplete(true)
                }
                override fun onCancel() {
                    onComplete(false)
                }
            }
        )
    }

    /**
     * Platform-specific implementation of minimum zoom preference
     */
    override fun performSetMinZoomPreference(minZoom: Double) {
        mapLibreMap?.setMinZoomPreference(minZoom)
    }

    /**
     * Platform-specific implementation of maximum zoom preference
     */
    override fun performSetMaxZoomPreference(maxZoom: Double) {
        mapLibreMap?.setMaxZoomPreference(maxZoom)
    }

    /**
     * Platform-specific implementation of wave polygon rendering
     */
    override fun performRenderWavePolygons(polygons: List<Any>, clearExisting: Boolean) {
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
                        PropertyFactory.fillColor(WAVE_BACKGROUND_COLOR.toColorInt()),
                        PropertyFactory.fillOpacity(WAVE_BACKGROUND_OPACITY)
                    )
                    style.addLayer(fillLayer)
                }

            } catch (e: Exception) {
                logError("Error updating wave polygons: ${e.message}")
            }
        }
    }

    /**
     * Platform-specific implementation of wave polygon clearing
     */
    override fun performClearWavePolygons() {
        val map = mapLibreMap ?: return
        
        map.getStyle { style ->
            val layerId = "wave-polygons-layer"
            val sourceId = "wave-polygons-source"
            
            try {
                style.removeLayer(layerId)
                style.removeSource(sourceId)
            } catch (e: Exception) {
                logError("Error clearing wave polygons: ${e.message}")
            }
        }
    }

    /**
     * Platform-specific implementation of map click listener
     */
    override fun performSetMapClickListener(listener: ((Double, Double) -> Unit)?) {
        val map = mapLibreMap ?: return

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
    
    /**
     * Sets bounds constraints for the map (PlatformMapOperations requirement)
     */
    override fun setBoundsConstraints(bounds: BoundingBox) {
        val map = mapLibreMap ?: return
        
        // Convert to MapLibre LatLngBounds
        val latLngBounds = LatLngBounds.Builder()
            .include(LatLng(bounds.southwest.latitude, bounds.southwest.longitude))
            .include(LatLng(bounds.northeast.latitude, bounds.northeast.longitude))
            .build()
        
        // Apply the bounds directly to the map - this tells MapLibre to restrict camera target
        map.setLatLngBoundsForCameraTarget(latLngBounds)
        
        // Update visible region padding to ensure constraints work properly
        updateVisibleRegionPadding(map)
        
        // Immediately enforce constraints in case we're already outside bounds
        constrainCamera()
    }
    
    // -- ConstraintEnforcer implementation --
    
    /**
     * Constrains the camera to stay within valid bounds
     * Returns true if constraints were applied (camera was moved)
     * Delegates to SharedCameraManager which uses calculateSafeBounds
     */
    override fun constrainCamera(): Boolean {
        // Delegate to the SharedCameraManager which now properly implements
        // the constraint system with calculateSafeBounds
        return cameraManager.constrainCamera()
    }

    /**
     * Moves the camera to the specified position
     * Used for constraint enforcement
     */
    override fun moveCamera(position: Position) {
        val map = mapLibreMap ?: return
        
        // Use immediate camera update (no animation) for constraint enforcement
        map.moveCamera(CameraUpdateFactory.newLatLng(
            LatLng(position.latitude, position.longitude)
        ))
        
        // Update camera info
        updateCameraInfo(position, cameraManager.currentZoom.value)
    }
    
    // -- Overrides --
    
    /**
     * Override to use Android-specific logging
     */
    override fun logError(message: String) {
        Log.e("AndroidMapLibreAdapter", message)
    }
    
    /**
     * Extended cleanup that also handles platform-specific resources
     */
    override fun cleanup() {
        super.cleanup()
        
        // Clear map click listener
        currentMapClickListener?.let { listener ->
            mapLibreMap?.removeOnMapClickListener(listener)
        }
        currentMapClickListener = null
        
        // Clear callbacks
        onMapSetCallbacks.clear()
        
        // Clear map reference
        mapLibreMap = null
    }
}
