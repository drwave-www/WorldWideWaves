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
import com.worldwidewaves.shared.map.MapConstraintManager.VisibleRegionPadding
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Platform-independent camera manager for map implementations
 * Handles camera state and animation coordination for both Android and iOS
 * 
 * The actual constraint enforcement is delegated to platform implementations
 * which have the proper map-specific logic to enforce exact bounds.
 */
class SharedCameraManager(private val platformOperations: PlatformMapOperations) {

    // -- Camera state management --
    
    private val _currentPosition = MutableStateFlow<Position?>(null)
    val currentPosition: StateFlow<Position?> = _currentPosition

    private val _currentZoom = MutableStateFlow(0.0)
    val currentZoom: StateFlow<Double> = _currentZoom

    // Store exact bounds for validation purposes
    private var mapBounds: BoundingBox? = null

    // Platform-independent helper that handles padding / nearest-point maths
    private var constraintManager: MapConstraintManager? = null

    // Cached bounds after padding is applied so we don’t recalc every frame
    private var currentConstraintBounds: BoundingBox? = null
    
    // -- Camera update methods --

    /**
     * Updates the current camera position and zoom values
     */
    fun updateCameraInfo(position: Position?, zoom: Double) {
        position?.let {
            _currentPosition.value = it
        }
        _currentZoom.value = zoom
    }

    /**
     * Animates camera to a specific position with optional zoom level
     */
    fun animateCamera(position: Position, zoom: Double?, callback: MapCameraCallback? = null) {
        // Delegate to platform-specific implementation
        // The platform will handle bounds checking and constraint enforcement
        platformOperations.animateCamera(position, zoom) { success ->
            if (success) {
                callback?.onFinish()
            } else {
                callback?.onCancel()
            }
        }
    }

    /**
     * Animates camera to fit a bounding box with optional padding
     */
    fun animateCameraToBounds(bounds: BoundingBox, padding: Int = 0, callback: MapCameraCallback? = null) {
        // Validate bounds before animation
        if (!isValidBounds(bounds)) {
            callback?.onCancel()
            return
        }

        // Delegate to platform-specific implementation
        platformOperations.animateCameraToBounds(bounds, padding) { success ->
            if (success) {
                callback?.onFinish()
            } else {
                callback?.onCancel()
            }
        }
    }

    // -- Constraint management --

    /**
     * Sets the bounds constraints for the map
     * Creates a [MapConstraintManager] that will later be used to
     * calculate padded bounds and nearest valid camera positions.
     */
    fun setBoundsConstraints(bounds: BoundingBox) {
        // Store original bounds
        mapBounds = bounds
        
        // Instantiate helper for later calculations
        constraintManager = MapConstraintManager(bounds)
        currentConstraintBounds = constraintManager!!.calculateConstraintBounds()

        // Delegate to platform – this will typically set hard camera target bounds
        platformOperations.setBoundsConstraints(bounds)
    }

    /**
     * Platform layer should call this whenever visible region changes
     * (e.g. after zoom) so padding can be re-evaluated.
     */
    fun updateVisibleRegionPadding(latPadding: Double, lngPadding: Double) {
        val manager = constraintManager ?: return
        val newPadding = VisibleRegionPadding(latPadding, lngPadding)

        if (manager.hasSignificantPaddingChange(newPadding)) {
            manager.setVisibleRegionPadding(newPadding)
            currentConstraintBounds = manager.calculateConstraintBounds()
        }
    }

    /**
     * Constrains the camera to stay within valid bounds
     * Returns true if adjustments were required.
     */
    fun constrainCamera(): Boolean {
        val currentPos = _currentPosition.value ?: return false
        val manager = constraintManager ?: return false
        val bounds = currentConstraintBounds ?: manager.calculateConstraintBounds()

        // 1) if bounds too small for view, recalc a safer box centred on camera
        if (!manager.isValidBounds(bounds, currentPos)) {
            val safe = manager.calculateSafeBounds(currentPos)
            platformOperations.animateCameraToBounds(safe, 0) { /*ignore*/ }
            currentConstraintBounds = safe
            return true
        }

        // 2) if camera outside bounds, snap to nearest valid point
        if (!bounds.contains(currentPos)) {
            val nearest = manager.getNearestValidPoint(currentPos, bounds)
            platformOperations.moveCamera(nearest)
            return true
        }
        return false
    }

    /**
     * Checks if the provided bounds are valid
     */
    fun isValidBounds(bounds: BoundingBox): Boolean {
        // Basic validation - ensure northeast is actually northeast of southwest
        return bounds.northeast.latitude > bounds.southwest.latitude &&
               bounds.northeast.longitude > bounds.southwest.longitude
    }
    
    /**
     * Sets minimum zoom preference
     */
    fun setMinZoomPreference(minZoom: Double) {
        platformOperations.setMinZoomPreference(minZoom)
    }

    /**
     * Sets maximum zoom preference
     */
    fun setMaxZoomPreference(maxZoom: Double) {
        platformOperations.setMaxZoomPreference(maxZoom)
    }
}

/**
 * Interface for platform-specific map operations
 * Implemented by platform-specific adapters (Android, iOS)
 */
interface PlatformMapOperations {
    /**
     * Animate camera to position with optional zoom
     */
    fun animateCamera(position: Position, zoom: Double?, onComplete: (Boolean) -> Unit)
    
    /**
     * Animate camera to fit bounds with padding
     */
    fun animateCameraToBounds(bounds: BoundingBox, padding: Int, onComplete: (Boolean) -> Unit)
    
    /**
     * Set bounds constraints for the map
     */
    fun setBoundsConstraints(bounds: BoundingBox)
    
    /**
     * Set minimum zoom level
     */
    fun setMinZoomPreference(minZoom: Double)
    
    /**
     * Set maximum zoom level
     */
    fun setMaxZoomPreference(maxZoom: Double)
    
    /**
     * Moves the camera to the specified position
     * Used for constraint enforcement
     */
    fun moveCamera(position: Position)
}

/**
 * Interface for platforms that implement constraint enforcement
 */
interface ConstraintEnforcer {
    /**
     * Constrains the camera to stay within bounds
     * Returns true if constraints were applied
     */
    fun constrainCamera(): Boolean
}
