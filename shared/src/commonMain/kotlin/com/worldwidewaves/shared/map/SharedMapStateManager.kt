package com.worldwidewaves.shared.map

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

import com.worldwidewaves.shared.events.utils.Position
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Platform-independent map state manager for map implementations
 * Handles common map state and interactions for both Android and iOS
 */
class SharedMapStateManager(private val platformMapRenderer: PlatformMapRenderer) {

    // -- Map initialization state --
    
    private var isMapInitialized = false
    private val onMapReadyCallbacks = mutableListOf<() -> Unit>()
    
    // -- Wave polygon state --
    
    private val _wavePolygonsVisible = MutableStateFlow(true)
    val wavePolygonsVisible: StateFlow<Boolean> = _wavePolygonsVisible
    
    private var currentWavePolygons: List<Any> = emptyList()
    
    // -- Map interaction state --
    
    private var mapClickListener: ((Double, Double) -> Unit)? = null
    
    /**
     * Notifies that the map is ready for use
     * Executes any pending callbacks
     */
    fun notifyMapReady() {
        isMapInitialized = true
        
        // Execute any pending callbacks
        val callbacksToExecute = ArrayList(onMapReadyCallbacks)
        onMapReadyCallbacks.clear()
        
        callbacksToExecute.forEach { callback ->
            callback()
        }
        
        // If we have wave polygons and they should be visible, render them
        if (currentWavePolygons.isNotEmpty() && _wavePolygonsVisible.value) {
            platformMapRenderer.renderWavePolygons(currentWavePolygons, true)
        }
        
        // Set click listener if one is registered
        mapClickListener?.let { listener ->
            platformMapRenderer.setMapClickListener(listener)
        }
    }
    
    /**
     * Register a callback to be executed when the map is ready
     * If the map is already ready, the callback is executed immediately
     */
    fun onMapReady(callback: () -> Unit) {
        if (isMapInitialized) {
            // Map is already initialized, execute callback immediately
            callback()
        } else {
            // Store callback for execution when map is ready
            onMapReadyCallbacks.add(callback)
        }
    }
    
    // -- Wave polygon management --
    
    /**
     * Updates the wave polygons on the map
     */
    fun updateWavePolygons(polygons: List<Any>, clearExisting: Boolean = false) {
        // Store the current polygons for later use if the map isn't ready yet
        currentWavePolygons = polygons
        
        // Only render if map is initialized and polygons should be visible
        if (isMapInitialized && _wavePolygonsVisible.value) {
            platformMapRenderer.renderWavePolygons(polygons, clearExisting)
        }
    }
    
    /**
     * Sets the visibility of wave polygons
     */
    fun setWavePolygonsVisible(visible: Boolean) {
        _wavePolygonsVisible.value = visible
        
        if (isMapInitialized) {
            if (visible && currentWavePolygons.isNotEmpty()) {
                // Show polygons
                platformMapRenderer.renderWavePolygons(currentWavePolygons, true)
            } else if (!visible) {
                // Hide polygons
                platformMapRenderer.clearWavePolygons()
            }
        }
    }
    
    /**
     * Toggles the visibility of wave polygons
     */
    fun toggleWavePolygonsVisibility() {
        setWavePolygonsVisible(!_wavePolygonsVisible.value)
    }
    
    // -- Map interaction management --
    
    /**
     * Sets a click listener for the map
     */
    fun setMapClickListener(listener: ((Double, Double) -> Unit)?) {
        mapClickListener = listener
        
        if (isMapInitialized) {
            platformMapRenderer.setMapClickListener(listener)
        }
    }
    
    /**
     * Handles a map click event from the platform
     * This should be called by platform implementations when a map click is detected
     */
    fun handleMapClick(latitude: Double, longitude: Double) {
        mapClickListener?.invoke(latitude, longitude)
    }
    
    /**
     * Clears all map resources
     * Should be called when the map is being destroyed
     */
    fun cleanup() {
        onMapReadyCallbacks.clear()
        mapClickListener = null
        currentWavePolygons = emptyList()
        isMapInitialized = false
    }
}

/**
 * Interface for platform-specific map rendering operations
 * Implemented by platform-specific adapters (Android, iOS)
 */
interface PlatformMapRenderer {
    /**
     * Renders wave polygons on the map
     */
    fun renderWavePolygons(polygons: List<Any>, clearExisting: Boolean)
    
    /**
     * Clears all wave polygons from the map
     */
    fun clearWavePolygons()
    
    /**
     * Sets a click listener for the map
     */
    fun setMapClickListener(listener: ((Double, Double) -> Unit)?)
}
