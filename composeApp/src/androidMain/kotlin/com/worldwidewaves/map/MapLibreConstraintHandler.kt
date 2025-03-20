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
import androidx.annotation.UiThread
import com.worldwidewaves.shared.events.utils.BoundingBox
import com.worldwidewaves.shared.events.utils.Position
import com.worldwidewaves.shared.map.MapConstraintManager
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap

/**
 * MapLibre-specific implementation of map constraints
 */
class MapLibreConstraintHandler(mapBounds: BoundingBox) {
    private val constraintManager = MapConstraintManager(mapBounds)
    private var constraintBounds: LatLngBounds? = null
    private var constraintsApplied = false

    /**
     * Apply constraints to a MapLibre map
     */
    @UiThread
    fun applyConstraints(map: MapLibreMap) {
        // Calculate visible region padding from the map
        updateVisibleRegionPadding(map)

        // Apply the constraints with the current padding
        applyConstraintsWithPadding(map)

        // Set up camera movement listener to update constraints on zoom changes
        map.addOnCameraIdleListener {
            val newPadding = calculateVisibleRegionPadding(map)

            if (constraintManager.hasSignificantPaddingChange(
                    MapConstraintManager.VisibleRegionPadding(
                        newPadding.latPadding,
                        newPadding.lngPadding
                    )
                )) {
                constraintManager.setVisibleRegionPadding(
                    MapConstraintManager.VisibleRegionPadding(
                        newPadding.latPadding,
                        newPadding.lngPadding
                    )
                )
                applyConstraintsWithPadding(map)
            }
        }

        constraintsApplied = true
    }

    /**
     * Apply constraints based on the current padding
     */
    private fun applyConstraintsWithPadding(map: MapLibreMap) {
        try {
            // Get platform-independent bounds from the constraint manager
            val paddedBounds = constraintManager.calculateConstraintBounds()

            // Convert to MapLibre LatLngBounds
            val latLngBounds = convertToLatLngBounds(paddedBounds)
            constraintBounds = latLngBounds

            // Apply constraints to the map
            map.setLatLngBoundsForCameraTarget(latLngBounds)

            // Also set min zoom
            val minZoom = map.minZoomLevel
            map.setMinZoomPreference(minZoom)

            // Check if our constrained area is too small for the current view
            val currentPosition = map.cameraPosition.target?.let {
                Position(
                    it.latitude,
                    it.longitude
                )
            }

            if (!constraintManager.isValidBounds(paddedBounds, currentPosition)) {
                // If bounds are too small, calculate safer bounds centered around current position
                currentPosition?.let {
                    val safeBounds = constraintManager.calculateSafeBounds(it)
                    fitMapToBounds(map, convertToLatLngBounds(safeBounds))
                }
            }
        } catch (e: Exception) {
            Log.e("MapConstraints", "Error applying constraints: ${e.message}")
        }
    }

    /**
     * Constrain the camera to the valid bounds if needed
     */
    fun constrainCamera(map: MapLibreMap) {
        val position = map.cameraPosition
        if (constraintBounds != null && !isCameraWithinConstraints(position)) {
            position.target?.let { target ->
                val mapPosition = Position(target.latitude, target.longitude)
                val constraintBoundsMapped = constraintBounds?.let { bounds ->
                    BoundingBox(
                        Position(bounds.getLatSouth(), bounds.getLonWest()),
                        Position(bounds.getLatNorth(), bounds.getLonEast())
                    )
                }

                constraintBoundsMapped?.let { bounds ->
                    val nearestValid = constraintManager.getNearestValidPoint(mapPosition, bounds)
                    val latLng = LatLng(nearestValid.latitude, nearestValid.longitude)
                    map.animateCamera(CameraUpdateFactory.newLatLng(latLng))
                }
            }
        }
    }

    /**
     * Checks if camera is within the constraint bounds
     */
    private fun isCameraWithinConstraints(cameraPosition: CameraPosition): Boolean =
        cameraPosition.target?.let { constraintBounds?.contains(it) } ?: true

    /**
     * Moves the camera to fit specified bounds
     */
    private fun fitMapToBounds(map: MapLibreMap, bounds: LatLngBounds) {
        val cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, 0)
        map.moveCamera(cameraUpdate)
    }

    /**
     * Updates visible region padding from the map
     */
    private fun updateVisibleRegionPadding(map: MapLibreMap) {
        val padding = calculateVisibleRegionPadding(map)
        constraintManager.setVisibleRegionPadding(
            MapConstraintManager.VisibleRegionPadding(padding.latPadding, padding.lngPadding)
        )
    }

    /**
     * Calculates visible region padding from the map projection
     */
    private data class MapLibrePadding(
        val latPadding: Double,
        val lngPadding: Double
    )

    private fun calculateVisibleRegionPadding(map: MapLibreMap): MapLibrePadding {
        // Get the visible region from the current map view
        val visibleRegion = map.projection.visibleRegion

        // Calculate padding as half the visible region dimensions
        val latPadding = (visibleRegion.latLngBounds.getLatNorth() -
                visibleRegion.latLngBounds.getLatSouth()) / 2.0
        val lngPadding = (visibleRegion.latLngBounds.getLonEast() -
                visibleRegion.latLngBounds.getLonWest()) / 2.0

        return MapLibrePadding(latPadding, lngPadding)
    }

    /**
     * Converts platform-independent MapBounds to MapLibre LatLngBounds
     */
    private fun convertToLatLngBounds(bounds: BoundingBox): LatLngBounds {
        return LatLngBounds.Builder()
            .include(LatLng(bounds.southwest.latitude, bounds.southwest.longitude))
            .include(LatLng(bounds.northeast.latitude, bounds.northeast.longitude))
            .build()
    }
}