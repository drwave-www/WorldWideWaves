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
 *
 * @param mapBounds The event area bounding box to constrain the map to
 * @param mapLibreAdapter Platform-specific map adapter
 * @param isWindowMode True for WINDOW mode (full map with gestures), false for BOUNDS mode (event detail, no gestures)
 * @param isSuppressed Lambda that returns true when constraint enforcement should be suppressed (during animations)
 */
class MapBoundsEnforcer(
    private val mapBounds: BoundingBox,
    private val mapLibreAdapter: MapLibreAdapter<*>,
    private val isWindowMode: Boolean = false,
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
    private var skipNextRecalculation = false // Skip one recalculation after programmatic zoom

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
        // Updates constraint bounds dynamically when viewport changes due to manual zoom
        if (!constraintsApplied) {
            mapLibreAdapter.addOnCameraIdleListener {
                // Skip recalculation if programmatic animation just completed
                if (skipNextRecalculation) {
                    Log.v("MapBoundsEnforcer", "Skipping recalculation after programmatic zoom")
                    skipNextRecalculation = false
                    return@addOnCameraIdleListener
                }

                // Prevent recalculation if animation/user interaction is in progress
                if (isSuppressed()) {
                    Log.v("MapBoundsEnforcer", "Camera idle callback suppressed (animation in progress)")
                    // Set flag to skip NEXT idle after suppression ends
                    skipNextRecalculation = true
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
            // NOTE: setBoundsForCameraTarget() now sets min zoom IMMEDIATELY (preventive enforcement)
            // Pass isWindowMode to apply safety margin only for full map screen (not event details)
            // CRITICAL: Pass ORIGINAL event bounds for min zoom calculation (not shrunk paddedBounds)
            mapLibreAdapter.setBoundsForCameraTarget(
                constraintBounds = paddedBounds,
                applyZoomSafetyMargin = isWindowMode,
                originalEventBounds = mapBounds, // CRITICAL: Original event bounds for min zoom
            )

            // Get the calculated min zoom for logging (already set by adapter)
            val minZoom = mapLibreAdapter.getMinZoomLevel()

            Log.i(
                "MapBoundsEnforcer",
                "✅ Applied strict constraints: minZoom=$minZoom (set preventively), " +
                    "bounds=SW(${paddedBounds.sw.lat},${paddedBounds.sw.lng}) " +
                    "NE(${paddedBounds.ne.lat},${paddedBounds.ne.lng})",
            )

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
     * Constrain the camera to the valid bounds if needed.
     *
     * DEPRECATED: This reactive approach (animating AFTER camera moves) has been replaced with
     * preventive enforcement via MapLibre's native constraints (setLatLngBoundsForCameraTarget + minZoom).
     *
     * The old reactive approach caused unwanted post-gesture animations and fought with user input.
     * The new approach prevents invalid gestures from being applied in the first place.
     *
     * This function is kept for compatibility but should NOT be called for Android.
     * iOS may still use reactive corrections due to platform API limitations.
     */
    @Suppress("ReturnCount") // Early returns are guard clauses for cleaner code
    @Deprecated(
        message = "Use preventive constraints (setBoundsForCameraTarget + minZoom) instead of reactive animations",
        replaceWith = ReplaceWith("applyConstraints()"),
    )
    fun constrainCamera() {
        // ANDROID: Skip reactive animations - rely on preventive constraints
        // MapLibre's setLatLngBoundsForCameraTarget() + setMinZoomPreference() handle this natively
        if (!isSuppressed()) {
            Log.v(
                "MapBoundsEnforcer",
                "constrainCamera() called but skipped (using preventive constraints instead)",
            )
        }

        // NOTE: iOS may still need this for gesture clamping if MLNMapViewDelegate.shouldChangeFrom
        // is not sufficient. Keep this code for potential iOS fallback but don't use on Android.
        /*
        val target = mapLibreAdapter.getCameraPosition() ?: return

        // Get current viewport to check if any part extends beyond event bounds
        val viewport = mapLibreAdapter.getVisibleRegion()

        // Check if viewport is completely within event bounds
        val isViewportValid = isViewportWithinEventBounds(viewport)

        if (!isViewportValid) {
            // Viewport extends beyond event bounds - clamp camera to keep viewport inside
            val clampedPosition = clampCameraToKeepViewportInside(target, viewport)

            // Only animate if position actually changed (avoid infinite loop)
            val positionChanged =
                abs(clampedPosition.latitude - target.latitude) > 0.000001 ||
                    abs(clampedPosition.longitude - target.longitude) > 0.000001

            if (positionChanged) {
                Log.d(
                    "MapBoundsEnforcer",
                    "${if (isWindowMode) "WINDOW" else "BOUNDS"} mode: Viewport exceeded event bounds, " +
                        "clamping camera from (${target.latitude}, ${target.longitude}) to " +
                        "(${clampedPosition.latitude}, ${clampedPosition.longitude})",
                )

                mapLibreAdapter.animateCamera(clampedPosition)
            } else {
                Log.v(
                    "MapBoundsEnforcer",
                    "Viewport exceeds bounds but camera already at clamped position - skipping animation",
                )
            }
            return
        }

        // Additional check: Camera center within constraint bounds (legacy behavior)
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
                Log.v(
                    "MapBoundsEnforcer",
                    "${if (isWindowMode) "WINDOW" else "BOUNDS"} mode: Camera center out of constraint bounds, " +
                        "moving to nearest valid point",
                )
                mapLibreAdapter.animateCamera(Position(nearestValid.latitude, nearestValid.longitude))
            }
        }
         */
    }

    /**
     * Checks if the entire viewport is within the event bounds.
     * Returns true only if all four corners of the viewport are inside the event area.
     */
    @Suppress("UnusedPrivateMember") // Used in constrainCamera (deprecated but kept for compatibility)
    private fun isViewportWithinEventBounds(viewport: BoundingBox): Boolean {
        // All viewport corners must be within event bounds
        val withinBounds =
            viewport.southLatitude >= mapBounds.southwest.latitude &&
                viewport.northLatitude <= mapBounds.northeast.latitude &&
                viewport.westLongitude >= mapBounds.southwest.longitude &&
                viewport.eastLongitude <= mapBounds.northeast.longitude

        if (!withinBounds) {
            Log.v(
                "MapBoundsEnforcer",
                "Viewport exceeds event bounds: " +
                    "viewport=SW(${viewport.southLatitude},${viewport.westLongitude}) " +
                    "NE(${viewport.northLatitude},${viewport.eastLongitude}), " +
                    "event=SW(${mapBounds.southwest.latitude},${mapBounds.southwest.longitude}) " +
                    "NE(${mapBounds.northeast.latitude},${mapBounds.northeast.longitude})",
            )
        }

        return withinBounds
    }

    /**
     * Clamps the camera position to ensure the viewport stays completely within event bounds.
     * Calculates the nearest valid camera position that keeps all viewport edges inside the event area.
     *
     * Special case: If the viewport is larger than the event bounds (e.g., zoomed out too far),
     * center the camera on the event bounds instead of trying to constrain to an invalid range.
     */
    @Suppress("UnusedPrivateMember") // Used in constrainCamera (deprecated but kept for compatibility)
    private fun clampCameraToKeepViewportInside(
        currentCamera: Position,
        currentViewport: BoundingBox,
    ): Position {
        // Calculate viewport dimensions (half-width and half-height from camera center)
        val viewportHalfHeight = (currentViewport.northLatitude - currentViewport.southLatitude) / 2.0
        val viewportHalfWidth = (currentViewport.eastLongitude - currentViewport.westLongitude) / 2.0

        // Calculate valid camera position range (camera center must stay within these bounds
        // to keep viewport edges inside event bounds)
        val minValidLat = mapBounds.southwest.latitude + viewportHalfHeight
        val maxValidLat = mapBounds.northeast.latitude - viewportHalfHeight
        val minValidLng = mapBounds.southwest.longitude + viewportHalfWidth
        val maxValidLng = mapBounds.northeast.longitude - viewportHalfWidth

        // Check if viewport is larger than bounds (invalid range where min > max)
        // This happens when the user is zoomed out too far
        val isViewportTooLarge = minValidLat > maxValidLat || minValidLng > maxValidLng

        if (isViewportTooLarge) {
            // Viewport is larger than event bounds - center camera on event bounds
            val centerLat = (mapBounds.southwest.latitude + mapBounds.northeast.latitude) / 2.0
            val centerLng = (mapBounds.southwest.longitude + mapBounds.northeast.longitude) / 2.0

            Log.d(
                "MapBoundsEnforcer",
                "Viewport larger than bounds (viewport: ${viewportHalfHeight * 2}° x ${viewportHalfWidth * 2}°, " +
                    "bounds: ${mapBounds.northeast.latitude - mapBounds.southwest.latitude}° x " +
                    "${mapBounds.northeast.longitude - mapBounds.southwest.longitude}°), " +
                    "centering camera on event bounds",
            )

            return Position(centerLat, centerLng)
        }

        // Clamp camera position to valid range (normal case)
        val clampedLat = currentCamera.latitude.coerceIn(minValidLat, maxValidLat)
        val clampedLng = currentCamera.longitude.coerceIn(minValidLng, maxValidLng)

        return Position(clampedLat, clampedLng)
    }

    /**
     * Checks if camera is within the constraint bounds
     */
    @Suppress("UnusedPrivateMember") // Used in constrainCamera (deprecated but kept for compatibility)
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

    @Suppress("ReturnCount") // Multiple returns for different modes
    private fun calculateVisibleRegionPadding(): VisibleRegionPadding {
        // CRITICAL DISTINCTION: WINDOW mode vs BOUNDS mode
        //
        // WINDOW mode (full map screen with gestures):
        //   - Need viewport padding to prevent edge overflow
        //   - setLatLngBoundsForCameraTarget() only constrains camera CENTER
        //   - Shrink bounds by viewport half-size to keep viewport inside event area
        //
        // BOUNDS mode (event detail screen, no gestures):
        //   - Want to show ENTIRE event area
        //   - No viewport padding needed (gestures disabled, no panning to edges)
        //   - Zero padding = constraint bounds = event bounds = entire event visible

        if (!isWindowMode) {
            // BOUNDS mode: No padding, show entire event
            Log.d(
                "MapBoundsEnforcer",
                "BOUNDS mode: Using zero padding (want entire event visible, gestures disabled)",
            )
            return VisibleRegionPadding(0.0, 0.0)
        }

        // WINDOW mode: Calculate padding from current viewport to constrain camera center
        // iOS: Uses these bounds directly (no runtime clamping, shouldChangeFrom only checks zoom)
        // Android: Initial constraints, then preventive gesture system clamps in real-time
        // Padding = viewport half-size ensures camera center range keeps viewport inside event bounds

        val viewport = mapLibreAdapter.getVisibleRegion()

        // Detect invalid viewport (>10° span indicates uninitialized map)
        val viewportLatSpan = viewport.ne.lat - viewport.sw.lat
        val viewportLngSpan = viewport.ne.lng - viewport.sw.lng

        if (viewportLatSpan > 10.0 || viewportLngSpan > 10.0) {
            Log.d(
                "MapBoundsEnforcer",
                "WINDOW mode: Invalid viewport detected ($viewportLatSpan° x $viewportLngSpan°), using zero padding until initialized",
            )
            return VisibleRegionPadding(0.0, 0.0)
        }

        val viewportHalfHeight = viewportLatSpan / 2.0
        val viewportHalfWidth = viewportLngSpan / 2.0

        Log.d(
            "MapBoundsEnforcer",
            "WINDOW mode: Viewport-based padding (viewport: $viewportLatSpan° x $viewportLngSpan°, " +
                "padding: $viewportHalfHeight° x $viewportHalfWidth°)",
        )

        return VisibleRegionPadding(viewportHalfHeight, viewportHalfWidth)
    }

    private fun calculatePaddedBounds(padding: VisibleRegionPadding): BoundingBox {
        Log.d("MapBoundsEnforcer", "mapBounds: SW(${mapBounds.sw.lat},${mapBounds.sw.lng}) NE(${mapBounds.ne.lat},${mapBounds.ne.lng})")
        Log.d("MapBoundsEnforcer", "padding: lat=${padding.latPadding}, lng=${padding.lngPadding}")

        // VIEWPORT EDGE CLAMPING: Shrink constraint bounds by viewport size
        // This ensures camera center cannot move where viewport edges would exceed event bounds
        // padding = viewportSize / 2, so we add/subtract it from event bounds to get valid camera center range

        // CRITICAL iOS FIX: Prevent bounds inversion when viewport > event (e.g., zoomed out on full map)
        // If padding >= eventSize/2, the result would have SW > NE, which iOS rejects as invalid
        // Clamp padding to 49% of event size to guarantee valid bounds with small center region
        val eventLatSpan = mapBounds.northeast.latitude - mapBounds.southwest.latitude
        val eventLngSpan = mapBounds.northeast.longitude - mapBounds.southwest.longitude

        // Use 49% (not 50%) to ensure a small valid region remains even when heavily zoomed out
        val maxLatPadding = eventLatSpan * 0.49
        val maxLngPadding = eventLngSpan * 0.49

        val effectiveLatPadding = min(padding.latPadding, maxLatPadding)
        val effectiveLngPadding = min(padding.lngPadding, maxLngPadding)

        if (effectiveLatPadding < padding.latPadding || effectiveLngPadding < padding.lngPadding) {
            Log.d(
                "MapBoundsEnforcer",
                "Clamping padding to prevent invalid bounds on iOS: " +
                    "requested=(${padding.latPadding}, ${padding.lngPadding}), " +
                    "clamped=($effectiveLatPadding, $effectiveLngPadding)",
            )
        }

        return BoundingBox.fromCorners(
            Position(
                mapBounds.southwest.latitude + effectiveLatPadding, // SW moves inward
                mapBounds.southwest.longitude + effectiveLngPadding, // SW moves inward
            ),
            Position(
                mapBounds.northeast.latitude - effectiveLatPadding, // NE moves inward
                mapBounds.northeast.longitude - effectiveLngPadding, // NE moves inward
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
