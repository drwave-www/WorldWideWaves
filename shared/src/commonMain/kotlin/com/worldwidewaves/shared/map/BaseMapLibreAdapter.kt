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

import com.worldwidewaves.shared.events.utils.BoundingBox
import com.worldwidewaves.shared.events.utils.Position

/**
 * Base implementation of MapLibreAdapter that provides common functionality
 * for both Android and iOS implementations.
 *
 * This abstract class handles shared state management and provides a foundation
 * for platform-specific implementations.
 */
abstract class BaseMapLibreAdapter : MapLibreAdapter {

    // -- Shared managers (initialized in init block) --

    // Initialize shared managers
    override val cameraManager: SharedCameraManager = SharedCameraManager(this)
    override val mapStateManager: SharedMapStateManager = SharedMapStateManager(this)

    // -- Initialization --

    // -- PlatformMapOperations implementation --
    
    /**
     * Animate camera to position with optional zoom
     * Platform implementations must call cameraManager.updateCameraInfo after animation
     */
    override fun animateCamera(position: Position, zoom: Double?, onComplete: (Boolean) -> Unit) {
        try {
            // Validate position
            if (!isValidPosition(position)) {
                logError("Invalid position for camera animation: $position")
                onComplete(false)
                return
            }
            
            // Delegate to platform-specific implementation
            performAnimateCamera(position, zoom, onComplete)
        } catch (e: Exception) {
            logError("Error animating camera: ${e.message}")
            onComplete(false)
        }
    }
    
    /**
     * Animate camera to fit bounds with padding
     * Platform implementations must call cameraManager.updateCameraInfo after animation
     */
    override fun animateCameraToBounds(bounds: BoundingBox, padding: Int, onComplete: (Boolean) -> Unit) {
        try {
            // Validate bounds
            if (!isValidBounds(bounds)) {
                logError("Invalid bounds for camera animation: $bounds")
                onComplete(false)
                return
            }
            
            // Delegate to platform-specific implementation
            performAnimateCameraToBounds(bounds, padding, onComplete)
        } catch (e: Exception) {
            logError("Error animating camera to bounds: ${e.message}")
            onComplete(false)
        }
    }
    
    /**
     * PlatformMapRenderer implementation--
     */
    
    /**
     * Renders wave polygons on the map
     */
    override fun renderWavePolygons(polygons: List<Any>, clearExisting: Boolean) {
        try {
            if (polygons.isEmpty() && !clearExisting) {
                // Nothing to do
                return
            }
            
            performRenderWavePolygons(polygons, clearExisting)
        } catch (e: Exception) {
            logError("Error rendering wave polygons: ${e.message}")
        }
    }
    
    /**
     * Clears all wave polygons from the map
     */
    override fun clearWavePolygons() {
        try {
            performClearWavePolygons()
        } catch (e: Exception) {
            logError("Error clearing wave polygons: ${e.message}")
        }
    }
    
    /**
     * Sets a click listener for the map
     */
    override fun setMapClickListener(listener: ((Double, Double) -> Unit)?) {
        try {
            performSetMapClickListener(listener)
        } catch (e: Exception) {
            logError("Error setting map click listener: ${e.message}")
        }
    }
    
    // -- Abstract methods for platform-specific implementations --
    
    /**
     * Platform-specific implementation of camera animation
     */
    protected abstract fun performAnimateCamera(
        position: Position, 
        zoom: Double?, 
        onComplete: (Boolean) -> Unit
    )
    
    /**
     * Platform-specific implementation of bounds animation
     */
    protected abstract fun performAnimateCameraToBounds(
        bounds: BoundingBox, 
        padding: Int, 
        onComplete: (Boolean) -> Unit
    )
    
    /**
     * Platform-specific implementation of minimum zoom preference
     */
    override fun setMinZoomPreference(minZoom: Double) {
        try {
            performSetMinZoomPreference(minZoom)
        } catch (e: Exception) {
            logError("Error setting min zoom preference: ${e.message}")
        }
    }
    
    /**
     * Platform-specific implementation of maximum zoom preference
     */
    override fun setMaxZoomPreference(maxZoom: Double) {
        try {
            performSetMaxZoomPreference(maxZoom)
        } catch (e: Exception) {
            logError("Error setting max zoom preference: ${e.message}")
        }
    }
    
    /**
     * Platform-specific implementation of minimum zoom preference
     */
    protected abstract fun performSetMinZoomPreference(minZoom: Double)
    
    /**
     * Platform-specific implementation of maximum zoom preference
     */
    protected abstract fun performSetMaxZoomPreference(maxZoom: Double)
    
    /**
     * Platform-specific implementation of wave polygon rendering
     */
    protected abstract fun performRenderWavePolygons(polygons: List<Any>, clearExisting: Boolean)
    
    /**
     * Platform-specific implementation of wave polygon clearing
     */
    protected abstract fun performClearWavePolygons()
    
    /**
     * Platform-specific implementation of map click listener
     */
    protected abstract fun performSetMapClickListener(listener: ((Double, Double) -> Unit)?)
    
    // -- Helper methods --
    
    /**
     * Validates a position (latitude/longitude)
     */
    protected fun isValidPosition(position: Position): Boolean {
        return position.latitude >= -90.0 && position.latitude <= 90.0 &&
                position.longitude >= -180.0 && position.longitude <= 180.0
    }
    
    /**
     * Validates bounds
     */
    protected fun isValidBounds(bounds: BoundingBox): Boolean {
        return isValidPosition(bounds.southwest) && 
               isValidPosition(bounds.northeast) &&
               bounds.northeast.latitude > bounds.southwest.latitude &&
               bounds.northeast.longitude > bounds.southwest.longitude
    }
    
    /**
     * Updates camera information from platform-specific implementations
     * Should be called after camera movements
     */
    protected fun updateCameraInfo(position: Position?, zoom: Double) {
        cameraManager.updateCameraInfo(position, zoom)
    }
    
    /**
     * Logs an error message
     * Platform implementations can override this to use platform-specific logging
     */
    protected open fun logError(message: String) {
        println("[MapLibreAdapter Error] $message")
    }

}
