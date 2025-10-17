package com.worldwidewaves.shared.map

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

import com.worldwidewaves.shared.WWWGlobals
import com.worldwidewaves.shared.events.utils.BoundingBox
import com.worldwidewaves.shared.events.utils.Position
import com.worldwidewaves.shared.utils.Log
import io.github.aakira.napier.Napier
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Platform-independent map bounds enforcement that handles the logic
 * for keeping the map view within bounds.
 */
class MapBoundsEnforcer(
    private val mapBounds: BoundingBox,
    private val mapLibreAdapter: MapLibreAdapter<*>,
    /**
     * When this lambda returns true the enforcer will not attempt to correct /
     * move the camera.  Used while a user-initiated or automatic animation is
     * already running to avoid fighting with it.
     */
    private val isSuppressed: () -> Boolean = { false },
) {
    data class VisibleRegionPadding(
        var latPadding: Double = 0.0,
        var lngPadding: Double = 0.0,
    )

    private var visibleRegionPadding = VisibleRegionPadding()
    private var constraintBounds: BoundingBox? = null
    private var constraintsApplied = false
    private var lastAppliedBounds: BoundingBox? = null // Track last applied bounds to prevent redundant updates

    fun setVisibleRegionPadding(padding: VisibleRegionPadding) {
        visibleRegionPadding = padding
    }

    fun calculateConstraintBounds(): BoundingBox = calculatePaddedBounds(visibleRegionPadding)

    /**
     * Apply constraints to a MapLibre map
     */
    fun applyConstraints() {
        // Calculate visible region padding from the map
        updateVisibleRegionPadding()

        // Apply the constraints with the current padding
        applyConstraintsWithPadding()

        // Register the camera-idle listener only once
        if (!constraintsApplied) {
            mapLibreAdapter.addOnCameraIdleListener {
                // Prevent recalculation if animation/user interaction is in progress
                if (isSuppressed()) {
                    Log.v("MapBoundsEnforcer", "Camera idle callback suppressed (animation in progress)")
                    return@addOnCameraIdleListener
                }

                val newPadding = calculateVisibleRegionPadding()

                if (
                    hasSignificantPaddingChange(
                        VisibleRegionPadding(newPadding.latPadding, newPadding.lngPadding),
                    )
                ) {
                    Log.d("MapBoundsEnforcer", "Significant padding change detected, updating constraints")
                    setVisibleRegionPadding(
                        VisibleRegionPadding(newPadding.latPadding, newPadding.lngPadding),
                    )
                    applyConstraintsWithPadding()
                } else {
                    Log.v("MapBoundsEnforcer", "Padding change insignificant, skipping constraint update")
                }
            }
            constraintsApplied = true
        }
    }

    /**
     * Apply constraints based on the current padding
     */
    private fun applyConstraintsWithPadding() {
        try {
            // Compute padded bounds and store them
            val paddedBounds = calculateConstraintBounds()
            constraintBounds = paddedBounds

            // Validate bounds are reasonable (not inverted or too small)
            if (paddedBounds.northeast.latitude <= paddedBounds.southwest.latitude ||
                paddedBounds.northeast.longitude <= paddedBounds.southwest.longitude
            ) {
                Log.w(
                    "MapBoundsEnforcer",
                    "Skipping constraint application - bounds are invalid/inverted " +
                        "(map not fully initialized, will retry on next camera idle)",
                )
                return
            }

            // Prevent infinite loop: skip if bounds haven't changed significantly (iOS triggers camera idle on every setBounds)
            if (lastAppliedBounds != null && boundsAreSimilar(lastAppliedBounds!!, paddedBounds)) {
                Log.v("MapBoundsEnforcer", "Bounds unchanged, skipping redundant constraint update")
                return
            }

            // Apply bounds & min-zoom to the map – no immediate camera move
            mapLibreAdapter.setBoundsForCameraTarget(paddedBounds)
            val minZoom = mapLibreAdapter.getMinZoomLevel()
            mapLibreAdapter.setMinZoomPreference(minZoom)

            // Track the bounds we just applied
            lastAppliedBounds = paddedBounds
        } catch (e: Exception) {
            Napier.e("Error applying constraints: ${e.message}")
        }
    }

    /**
     * Check if two bounding boxes are similar enough to be considered the same (within 0.1% tolerance).
     * This prevents infinite loops caused by floating-point precision issues and iOS camera idle callbacks.
     */
    private fun boundsAreSimilar(
        bounds1: BoundingBox,
        bounds2: BoundingBox,
    ): Boolean {
        val tolerance = 0.001 // 0.1% tolerance for coordinate comparison

        val latDiff =
            abs(bounds1.southwest.latitude - bounds2.southwest.latitude) +
                abs(bounds1.northeast.latitude - bounds2.northeast.latitude)
        val lngDiff =
            abs(bounds1.southwest.longitude - bounds2.southwest.longitude) +
                abs(bounds1.northeast.longitude - bounds2.northeast.longitude)

        val isSimilar = latDiff < tolerance && lngDiff < tolerance

        if (!isSimilar) {
            Log.d("MapBoundsEnforcer", "Bounds changed significantly: latDiff=$latDiff, lngDiff=$lngDiff")
        }

        return isSimilar
    }

    /**
     * Constrain the camera to the valid bounds if needed
     */
    fun constrainCamera() {
        if (isSuppressed()) return
        val target = mapLibreAdapter.getCameraPosition() ?: return
        if (constraintBounds != null && !isCameraWithinConstraints(target)) {
            val mapPosition = Position(target.latitude, target.longitude)
            val constraintBoundsMapped =
                constraintBounds?.let { bounds ->
                    BoundingBox.fromCorners(
                        Position(bounds.southwest.latitude, bounds.southwest.longitude),
                        Position(bounds.northeast.latitude, bounds.northeast.longitude),
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
    private fun isCameraWithinConstraints(cameraPosition: Position): Boolean = constraintBounds?.contains(cameraPosition) ?: false

    // fitMapToBounds removed - was unused

    /**
     * Updates visible region padding from the map
     */
    private fun updateVisibleRegionPadding() {
        val padding = calculateVisibleRegionPadding()
        setVisibleRegionPadding(
            VisibleRegionPadding(padding.latPadding, padding.lngPadding),
        )
    }

    private fun calculateVisibleRegionPadding(): VisibleRegionPadding {
        // Get the visible region from the current map view
        val visibleRegion = mapLibreAdapter.getVisibleRegion()

        // Calculate padding as half the visible region dimensions
        val latPadding =
            (
                visibleRegion.northLatitude -
                    visibleRegion.southLatitude
            ) / 2.0
        val lngPadding =
            (
                visibleRegion.eastLongitude -
                    visibleRegion.westLongitude
            ) / 2.0

        // Guard against invalid dimensions (happens when map not fully initialized)
        // Visible region can be huge before map dimensions are known, causing out-of-range errors
        val mapLatSpan = mapBounds.northeast.latitude - mapBounds.southwest.latitude
        val mapLngSpan = mapBounds.northeast.longitude - mapBounds.southwest.longitude

        // Clamp padding to reasonable values (max 50% of map dimensions)
        val clampedLatPadding = latPadding.coerceIn(0.0, mapLatSpan * 0.5)
        val clampedLngPadding = lngPadding.coerceIn(0.0, mapLngSpan * 0.5)

        if (latPadding != clampedLatPadding || lngPadding != clampedLngPadding) {
            Log.w(
                "MapBoundsEnforcer",
                "Clamped excessive padding: " +
                    "lat $latPadding→$clampedLatPadding, lng $lngPadding→$clampedLngPadding " +
                    "(map not fully initialized)",
            )
        }

        return VisibleRegionPadding(clampedLatPadding, clampedLngPadding)
    }

    private fun calculatePaddedBounds(padding: VisibleRegionPadding): BoundingBox {
        Log.d("MapBoundsEnforcer", "mapBounds: SW(${mapBounds.sw.lat},${mapBounds.sw.lng}) NE(${mapBounds.ne.lat},${mapBounds.ne.lng})")
        Log.d("MapBoundsEnforcer", "padding: lat=${padding.latPadding}, lng=${padding.lngPadding}")

        // VIEWPORT EDGE CLAMPING: Shrink constraint bounds by viewport size
        // This ensures camera center cannot move where viewport edges would exceed event bounds
        // padding = viewportSize / 2, so we add/subtract it from event bounds to get valid camera center range
        return BoundingBox.fromCorners(
            Position(
                mapBounds.southwest.latitude + padding.latPadding, // SW moves inward
                mapBounds.southwest.longitude + padding.lngPadding, // SW moves inward
            ),
            Position(
                mapBounds.northeast.latitude - padding.latPadding, // NE moves inward
                mapBounds.northeast.longitude - padding.lngPadding, // NE moves inward
            ),
        )
    }

    fun isValidBounds(
        bounds: BoundingBox,
        currentPosition: Position?,
    ): Boolean {
        if (currentPosition == null) return false

        // Check if bounds are valid (not inverted or too small)
        val validSize =
            bounds.northeast.latitude > bounds.southwest.latitude &&
                bounds.northeast.longitude > bounds.southwest.longitude &&
                (bounds.height) > visibleRegionPadding.latPadding * 0.1 &&
                (bounds.width) > visibleRegionPadding.lngPadding * 0.1

        // Check if the current position is within or close to the bounds
        val containsTarget =
            bounds.contains(currentPosition) ||
                isNearBounds(
                    currentPosition,
                    bounds,
                    visibleRegionPadding.latPadding * 0.5,
                    visibleRegionPadding.lngPadding * 0.5,
                )

        return validSize && containsTarget
    }

    private fun isNearBounds(
        point: Position,
        bounds: BoundingBox,
        latPadding: Double,
        lngPadding: Double,
    ): Boolean {
        val nearLat =
            point.latitude >= bounds.southwest.latitude - latPadding &&
                point.latitude <= bounds.northeast.latitude + latPadding
        val nearLng =
            point.longitude >= bounds.southwest.longitude - lngPadding &&
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
        centerLat =
            centerLat.coerceIn(
                mapBounds.southwest.latitude + usableLatPadding,
                mapBounds.northeast.latitude - usableLatPadding,
            )
        centerLng =
            centerLng.coerceIn(
                mapBounds.southwest.longitude + usableLngPadding,
                mapBounds.northeast.longitude - usableLngPadding,
            )

        // Calculate the corners of our safe bounds
        val safeSouth = max(mapBounds.southwest.latitude, centerLat - usableLatPadding)
        val safeNorth = min(mapBounds.northeast.latitude, centerLat + usableLatPadding)
        val safeWest = max(mapBounds.southwest.longitude, centerLng - usableLngPadding)
        val safeEast = min(mapBounds.northeast.longitude, centerLng + usableLngPadding)

        // Ensure the bounds have minimum size (at least 20% of padding)
        val minLatSpan = visibleRegionPadding.latPadding * 0.2
        val minLngSpan = visibleRegionPadding.lngPadding * 0.2

        val finalNorth = min(mapBounds.northeast.latitude, max(safeNorth, safeSouth + minLatSpan))
        val finalEast = min(mapBounds.northeast.longitude, max(safeEast, safeWest + minLngSpan))

        return BoundingBox.fromCorners(
            Position(safeSouth, safeWest),
            Position(finalNorth, finalEast),
        )
    }

    fun hasSignificantPaddingChange(newPadding: VisibleRegionPadding): Boolean {
        // Determine if padding change is significant enough to update constraints
        val latChange =
            abs(newPadding.latPadding - visibleRegionPadding.latPadding) /
                visibleRegionPadding.latPadding
        val lngChange =
            abs(newPadding.lngPadding - visibleRegionPadding.lngPadding) /
                visibleRegionPadding.lngPadding

        return latChange > WWWGlobals.MapDisplay.CHANGE_THRESHOLD ||
            lngChange > WWWGlobals.MapDisplay.CHANGE_THRESHOLD // 10% change threshold
    }

    fun getNearestValidPoint(
        point: Position,
        bounds: BoundingBox,
    ): Position {
        val lat = point.latitude.coerceIn(bounds.minLatitude, bounds.maxLatitude)
        val lng = point.longitude.coerceIn(bounds.minLongitude, bounds.maxLongitude)
        return Position(lat, lng)
    }
}
