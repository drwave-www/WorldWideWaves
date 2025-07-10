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

import androidx.annotation.UiThread
import com.worldwidewaves.shared.events.utils.BoundingBox
import com.worldwidewaves.shared.events.utils.Position
import io.github.aakira.napier.Napier
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Platform-independent map constraint management that handles the logic
 * for keeping the map view within bounds.
 */
class MapConstraintManager(private val mapBounds: BoundingBox, private val mapLibreAdapter: MapLibreAdapter<*>) {

    data class VisibleRegionPadding(
        var latPadding: Double = 0.0,
        var lngPadding: Double = 0.0
    )

    private var visibleRegionPadding = VisibleRegionPadding()
    private var constraintBounds: BoundingBox? = null
    private var constraintsApplied = false

    fun setVisibleRegionPadding(padding: VisibleRegionPadding) {
        visibleRegionPadding = padding
    }

    fun calculateConstraintBounds(): BoundingBox {
        return calculatePaddedBounds(visibleRegionPadding)
    }

    /**
     * Apply constraints to a MapLibre map
     */
    @UiThread
    fun applyConstraints() {
        // Calculate visible region padding from the map
        updateVisibleRegionPadding()

        // Apply the constraints with the current padding
        applyConstraintsWithPadding()

        // Set up camera movement listener to update constraints on zoom changes
        mapLibreAdapter.addOnCameraIdleListener {
            val newPadding = calculateVisibleRegionPadding()

            if (hasSignificantPaddingChange(
                VisibleRegionPadding(
                        newPadding.latPadding,
                        newPadding.lngPadding
                    )
                )) {
                    setVisibleRegionPadding(
                        VisibleRegionPadding(
                        newPadding.latPadding,
                        newPadding.lngPadding
                    )
                )
                applyConstraintsWithPadding()
            }
        }

        constraintsApplied = true
    }

    /**
     * Apply constraints based on the current padding
     */
    private fun applyConstraintsWithPadding() {
        try {
            // Get platform-independent bounds from the constraint manager
            val paddedBounds = calculateConstraintBounds()
            constraintBounds = paddedBounds

            // Apply constraints to the map
            mapLibreAdapter.setBoundsForCameraTarget(paddedBounds)

            // Also set min zoom
            val minZoom = mapLibreAdapter.getMinZoomLevel()
            mapLibreAdapter.setMinZoomPreference(minZoom)

            // Check if our constrained area is too small for the current view
            val currentPosition = mapLibreAdapter.getCameraPosition()?.let {
                Position(
                    it.latitude,
                    it.longitude
                )
            }

            if (!isValidBounds(paddedBounds, currentPosition)) {
                // If bounds are too small, calculate safer bounds centered around current position
                currentPosition?.let {
                    val safeBounds = calculateSafeBounds(it)
                    fitMapToBounds(safeBounds)
                }
            }
        } catch (e: Exception) {
            Napier.e("Error applying constraints: ${e.message}")
        }
    }

    /**
     * Constrain the camera to the valid bounds if needed
     */
    fun constrainCamera() {
        val target = mapLibreAdapter.getCameraPosition() ?: return
        if (constraintBounds != null && !isCameraWithinConstraints(target)) {
            val mapPosition = Position(target.latitude, target.longitude)
            val constraintBoundsMapped = constraintBounds?.let { bounds ->
                BoundingBox.fromCorners(
                    Position(bounds.southwest.latitude, bounds.southwest.longitude),
                    Position(bounds.northeast.latitude, bounds.northeast.longitude)
                )
            }

            constraintBoundsMapped?.let { bounds ->
                val nearestValid = getNearestValidPoint(mapPosition, bounds)
                mapLibreAdapter.animateCamera(Position(nearestValid.latitude, nearestValid.longitude))
            }
        }
    }

    /**
     * Checks if camera is within the constraint bounds
     */
    private fun isCameraWithinConstraints(cameraPosition: Position): Boolean =
        constraintBounds?.contains(cameraPosition) ?: false

    /**
     * Moves the camera to fit specified bounds
     */
    private fun fitMapToBounds(bounds: BoundingBox) {
        mapLibreAdapter.moveCamera(bounds)
    }

    /**
     * Updates visible region padding from the map
     */
    private fun updateVisibleRegionPadding() {
        val padding = calculateVisibleRegionPadding()
        setVisibleRegionPadding(
            VisibleRegionPadding(padding.latPadding, padding.lngPadding)
        )
    }

    private fun calculateVisibleRegionPadding(): VisibleRegionPadding {
        // Get the visible region from the current map view
        val visibleRegion = mapLibreAdapter.getVisibleRegion()

        // Calculate padding as half the visible region dimensions
        val latPadding = (visibleRegion.northLatitude -
                visibleRegion.southLatitude) / 2.0
        val lngPadding = (visibleRegion.eastLongitude -
                visibleRegion.westLongitude) / 2.0

        return VisibleRegionPadding(latPadding, lngPadding)
    }

    private fun calculatePaddedBounds(padding: VisibleRegionPadding): BoundingBox {
        return BoundingBox.fromCorners(
            Position(
                mapBounds.southwest.latitude + padding.latPadding,
                mapBounds.southwest.longitude + padding.lngPadding
            ),
            Position(
                mapBounds.northeast.latitude - padding.latPadding,
                mapBounds.northeast.longitude - padding.lngPadding
            )
        )
    }

    fun isValidBounds(bounds: BoundingBox, currentPosition: Position?): Boolean {
        if (currentPosition == null) return false

        // Check if bounds are valid (not inverted or too small)
        val validSize = bounds.northeast.latitude > bounds.southwest.latitude &&
                bounds.northeast.longitude > bounds.southwest.longitude &&
                (bounds.height) > visibleRegionPadding.latPadding * 0.1 &&
                (bounds.width) > visibleRegionPadding.lngPadding * 0.1

        // Check if the current position is within or close to the bounds
        val containsTarget = bounds.contains(currentPosition) ||
                isNearBounds(currentPosition, bounds, visibleRegionPadding.latPadding * 0.5,
                    visibleRegionPadding.lngPadding * 0.5)

        return validSize && containsTarget
    }

    private fun isNearBounds(point: Position, bounds: BoundingBox, latPadding: Double, lngPadding: Double): Boolean {
        val nearLat = point.latitude >= bounds.southwest.latitude - latPadding &&
                point.latitude <= bounds.northeast.latitude + latPadding
        val nearLng = point.longitude >= bounds.southwest.longitude - lngPadding &&
                point.longitude <= bounds.northeast.longitude + lngPadding
        return nearLat && nearLng
    }

    fun calculateSafeBounds(centerPosition: Position): BoundingBox {
        // Calculate how much space we need (similar to visible region padding)
        val neededLatPadding = visibleRegionPadding.latPadding * 1.5 // Add extra margin
        val neededLngPadding = visibleRegionPadding.lngPadding * 1.5

        // Calculate map dimensions
        val mapLatSpan = mapBounds.northeast.latitude - mapBounds.southwest.latitude
        val mapLngSpan = mapBounds.northeast.longitude - mapBounds.southwest.longitude

        // Make sure our needed padding isn't larger than the map itself
        val usableLatPadding = min(neededLatPadding, mapLatSpan * 0.4) // Max 40% of map height
        val usableLngPadding = min(neededLngPadding, mapLngSpan * 0.4) // Max 40% of map width

        // Try to center the bounds around the current position
        var centerLat = centerPosition.latitude
        var centerLng = centerPosition.longitude

        // But constrain the center to be within the map bounds (with padding)
        centerLat = centerLat.coerceIn(
            mapBounds.southwest.latitude + usableLatPadding,
            mapBounds.northeast.latitude - usableLatPadding
        )
        centerLng = centerLng.coerceIn(
            mapBounds.southwest.longitude + usableLngPadding,
            mapBounds.northeast.longitude - usableLngPadding
        )

        // Calculate the corners of our safe bounds
        val safeSouth = max(mapBounds.southwest.latitude, centerLat - usableLatPadding)
        val safeNorth = min(mapBounds.northeast.latitude, centerLat + usableLatPadding)
        val safeWest = max(mapBounds.southwest.longitude, centerLng - usableLngPadding)
        val safeEast = min(mapBounds.northeast.longitude, centerLng + usableLngPadding)

        // Ensure the bounds have minimum size (at least 20% of padding)
        val minLatSpan = visibleRegionPadding.latPadding * 0.2
        val minLngSpan = visibleRegionPadding.lngPadding * 0.2

        // val finalNorth = max(safeNorth, safeSouth + minLatSpan)
        // val finalEast = max(safeEast, safeWest + minLngSpan)
        val finalNorth = min(mapBounds.northeast.latitude, max(safeNorth, safeSouth + minLatSpan))
        val finalEast = min(mapBounds.northeast.longitude, max(safeEast, safeWest + minLngSpan))

        return BoundingBox.fromCorners(
            Position(safeSouth, safeWest),
            Position(finalNorth, finalEast)
        )
    }

    fun hasSignificantPaddingChange(newPadding: VisibleRegionPadding): Boolean {
        // Determine if padding change is significant enough to update constraints
        val latChange = abs(newPadding.latPadding - visibleRegionPadding.latPadding) /
                visibleRegionPadding.latPadding
        val lngChange = abs(newPadding.lngPadding - visibleRegionPadding.lngPadding) /
                visibleRegionPadding.lngPadding

        return latChange > 0.1 || lngChange > 0.1 // 10% change threshold
    }

    fun getNearestValidPoint(point: Position, bounds: BoundingBox): Position {
        val lat = point.latitude.coerceIn(bounds.minLatitude, bounds.maxLatitude)
        val lng = point.longitude.coerceIn(bounds.minLongitude, bounds.maxLongitude)
        return Position(lat, lng)
    }
}