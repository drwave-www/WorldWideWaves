package com.worldwidewaves.shared.map

/* * Copyright 2025 DrWave
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
 * limitations under the License. */


import com.worldwidewaves.shared.events.utils.BoundingBox
import com.worldwidewaves.shared.events.utils.Position

/**
 * Test fixtures and utilities for map testing across all platforms.
 * These fixtures provide standardized test data for unit tests, integration tests, and E2E tests.
 */
object MapTestFixtures {
    // ============================================================
    // STANDARD EVENT CONFIGURATIONS
    // ============================================================

    /**
     * Standard square event centered on Paris (approximately 1km x 1km)
     */
    val STANDARD_EVENT_BOUNDS =
        BoundingBox.fromCorners(
            Position(48.8566, 2.3522), // SW - Paris center
            Position(48.8666, 2.3622), // NE - ~1km x 1km
        )

    /**
     * Wide event with 2:1 aspect ratio (landscape orientation)
     * Useful for testing WINDOW mode aspect ratio fitting
     */
    val WIDE_EVENT_BOUNDS =
        BoundingBox.fromCorners(
            Position(48.8566, 2.3422), // SW
            Position(48.8616, 2.3722), // NE - 2:1 aspect ratio
        )

    /**
     * Tall event with 1:2 aspect ratio (portrait orientation)
     * Useful for testing WINDOW mode aspect ratio fitting
     */
    val TALL_EVENT_BOUNDS =
        BoundingBox.fromCorners(
            Position(48.8466, 2.3522), // SW
            Position(48.8666, 2.3572), // NE - 1:2 aspect ratio
        )

    /**
     * Small event for testing zoom constraints
     */
    val SMALL_EVENT_BOUNDS =
        BoundingBox.fromCorners(
            Position(48.8600, 2.3550), // SW
            Position(48.8620, 2.3570), // NE - ~220m x 220m
        )

    /**
     * Large event for testing performance
     */
    val LARGE_EVENT_BOUNDS =
        BoundingBox.fromCorners(
            Position(48.8000, 2.2000), // SW
            Position(48.9000, 2.4000), // NE - ~11km x 15km
        )

    // ============================================================
    // USER POSITIONS
    // ============================================================

    /**
     * Position at the center of STANDARD_EVENT_BOUNDS
     */
    val USER_INSIDE_EVENT = Position(48.8616, 2.3572)

    /**
     * Position at the north edge of STANDARD_EVENT_BOUNDS
     */
    val USER_AT_NORTH_EDGE = Position(48.8665, 2.3572)

    /**
     * Position at the south edge of STANDARD_EVENT_BOUNDS
     */
    val USER_AT_SOUTH_EDGE = Position(48.8567, 2.3572)

    /**
     * Position at the east edge of STANDARD_EVENT_BOUNDS
     */
    val USER_AT_EAST_EDGE = Position(48.8616, 2.3621)

    /**
     * Position at the west edge of STANDARD_EVENT_BOUNDS
     */
    val USER_AT_WEST_EDGE = Position(48.8616, 2.3523)

    /**
     * Position at the north-west corner of STANDARD_EVENT_BOUNDS
     */
    val USER_AT_NW_CORNER = Position(48.8665, 2.3523)

    /**
     * Position at the north-east corner of STANDARD_EVENT_BOUNDS
     */
    val USER_AT_NE_CORNER = Position(48.8665, 2.3621)

    /**
     * Position at the south-west corner of STANDARD_EVENT_BOUNDS
     */
    val USER_AT_SW_CORNER = Position(48.8567, 2.3523)

    /**
     * Position at the south-east corner of STANDARD_EVENT_BOUNDS
     */
    val USER_AT_SE_CORNER = Position(48.8567, 2.3621)

    /**
     * Position outside STANDARD_EVENT_BOUNDS (north of event)
     */
    val USER_OUTSIDE_EVENT = Position(48.8700, 2.3700)

    /**
     * Position far outside STANDARD_EVENT_BOUNDS
     */
    val USER_FAR_OUTSIDE_EVENT = Position(48.9000, 2.4000)

    // ============================================================
    // SCREEN DIMENSIONS
    // ============================================================

    /**
     * Portrait phone screen (9:16 aspect ratio)
     */
    data class ScreenDimensions(
        val width: Double,
        val height: Double,
    ) {
        val aspectRatio: Double get() = width / height
    }

    val PORTRAIT_PHONE = ScreenDimensions(1080.0, 1920.0) // 9:16
    val LANDSCAPE_PHONE = ScreenDimensions(1920.0, 1080.0) // 16:9
    val PORTRAIT_TABLET = ScreenDimensions(1536.0, 2048.0) // 3:4
    val LANDSCAPE_TABLET = ScreenDimensions(2048.0, 1536.0) // 4:3
    val SQUARE_SCREEN = ScreenDimensions(1080.0, 1080.0) // 1:1

    // ============================================================
    // ZOOM LEVELS
    // ============================================================

    const val ZOOM_WORLD = 0.0 // Entire world visible
    const val ZOOM_COUNTRY = 5.0 // Country level
    const val ZOOM_CITY = 10.0 // City level
    const val ZOOM_NEIGHBORHOOD = 13.0 // Neighborhood level
    const val ZOOM_STREET = 15.0 // Street level
    const val ZOOM_BUILDING = 18.0 // Building level
    const val ZOOM_MAX = 22.0 // Maximum zoom

    // ============================================================
    // TOLERANCE VALUES
    // ============================================================

    const val TOLERANCE_POSITION = 0.0001 // ~11 meters at equator
    const val TOLERANCE_ZOOM = 0.1 // Acceptable zoom difference
    const val TOLERANCE_DIMENSION = 0.05 // 5% dimension difference
    const val TOLERANCE_EDGE = 0.0001 // Tolerance for edge sticking

    // ============================================================
    // HELPER FUNCTIONS
    // ============================================================

    /**
     * Calculate the center position of a bounding box
     */
    fun BoundingBox.center(): Position {
        val centerLat = (southwest.latitude + northeast.latitude) / 2.0
        val centerLng = (southwest.longitude + northeast.longitude) / 2.0
        return Position(centerLat, centerLng)
    }

    /**
     * Calculate the width of a bounding box (longitude span)
     */
    val BoundingBox.width: Double
        get() = northeast.longitude - southwest.longitude

    /**
     * Calculate the height of a bounding box (latitude span)
     */
    val BoundingBox.height: Double
        get() = northeast.latitude - southwest.latitude

    /**
     * Calculate the aspect ratio of a bounding box (width / height)
     */
    val BoundingBox.aspectRatio: Double
        get() = width / height

    /**
     * Check if a bounding box is completely within another bounding box
     */
    fun BoundingBox.isCompletelyWithin(bounds: BoundingBox): Boolean =
        southwest.latitude >= bounds.southwest.latitude &&
            southwest.longitude >= bounds.southwest.longitude &&
            northeast.latitude <= bounds.northeast.latitude &&
            northeast.longitude <= bounds.northeast.longitude

    /**
     * Check if a bounding box is valid (not inverted or zero-sized)
     */
    fun BoundingBox.isValid(): Boolean =
        northeast.latitude > southwest.latitude &&
            northeast.longitude > southwest.longitude &&
            width > 0.0 &&
            height > 0.0

    /**
     * Check if two positions are approximately equal within tolerance
     */
    fun Position.isApproximately(
        other: Position,
        tolerance: Double = TOLERANCE_POSITION,
    ): Boolean =
        kotlin.math.abs(latitude - other.latitude) < tolerance &&
            kotlin.math.abs(longitude - other.longitude) < tolerance

    /**
     * Check if two bounding boxes are approximately equal within tolerance
     */
    fun BoundingBox.isApproximately(
        other: BoundingBox,
        tolerance: Double = TOLERANCE_POSITION,
    ): Boolean =
        southwest.isApproximately(other.southwest, tolerance) &&
            northeast.isApproximately(other.northeast, tolerance)

    /**
     * Expand a bounding box by a percentage
     */
    fun BoundingBox.expand(percentage: Double): BoundingBox {
        val latExpansion = height * percentage / 2.0
        val lngExpansion = width * percentage / 2.0

        return BoundingBox.fromCorners(
            Position(
                southwest.latitude - latExpansion,
                southwest.longitude - lngExpansion,
            ),
            Position(
                northeast.latitude + latExpansion,
                northeast.longitude + lngExpansion,
            ),
        )
    }

    /**
     * Shrink a bounding box by a percentage
     */
    fun BoundingBox.shrink(percentage: Double): BoundingBox {
        require(percentage < 0.5) { "Cannot shrink by more than 50%" }

        val latShrinkage = height * percentage / 2.0
        val lngShrinkage = width * percentage / 2.0

        return BoundingBox.fromCorners(
            Position(
                southwest.latitude + latShrinkage,
                southwest.longitude + lngShrinkage,
            ),
            Position(
                northeast.latitude - latShrinkage,
                northeast.longitude - lngShrinkage,
            ),
        )
    }

    /**
     * Calculate expected viewport dimensions for a given zoom level and screen dimensions
     * Based on Web Mercator projection formulas
     */
    fun calculateViewportDimensions(
        cameraLat: Double,
        zoom: Double,
        screenDimensions: ScreenDimensions,
    ): Pair<Double, Double> {
        // Meters per pixel at given zoom level and latitude
        // 2^zoom can be calculated using left shift or Math.pow
        val zoomScale = 1 shl zoom.toInt() // 2^zoom
        val metersPerPixel =
            156543.03392 * kotlin.math.cos(Math.toRadians(cameraLat)) / zoomScale

        // Convert to degrees (approximately)
        // 1 degree latitude ≈ 111,320 meters
        // 1 degree longitude ≈ 111,320 * cos(latitude) meters
        val viewportHeight = screenDimensions.height * metersPerPixel / 111320.0
        val viewportWidth =
            screenDimensions.width * metersPerPixel /
                (111320.0 * kotlin.math.cos(Math.toRadians(cameraLat)))

        return Pair(viewportWidth, viewportHeight)
    }

    /**
     * Calculate minimum zoom level needed to fit a bounding box on screen
     */
    fun calculateMinZoomToFit(
        bounds: BoundingBox,
        screenDimensions: ScreenDimensions,
    ): Double {
        val centerLat = bounds.center().latitude

        // Calculate zoom needed to fit width
        val zoomForWidth =
            kotlin.math.log2(
                (screenDimensions.width * 360.0) / (bounds.width * 256.0),
            )

        // Calculate zoom needed to fit height
        val zoomForHeight =
            kotlin.math.log2(
                (screenDimensions.height * 180.0) / (bounds.height * 256.0),
            )

        // Use the smaller zoom (ensures both dimensions fit)
        return kotlin.math.min(zoomForWidth, zoomForHeight)
    }

    /**
     * Create a BoundingBox representing the visible region at a given camera position and zoom
     */
    fun createVisibleRegion(
        cameraPosition: Position,
        zoom: Double,
        screenDimensions: ScreenDimensions,
    ): BoundingBox {
        val (viewportWidth, viewportHeight) =
            calculateViewportDimensions(
                cameraPosition.latitude,
                zoom,
                screenDimensions,
            )

        return BoundingBox.fromCorners(
            Position(
                cameraPosition.latitude - viewportHeight / 2.0,
                cameraPosition.longitude - viewportWidth / 2.0,
            ),
            Position(
                cameraPosition.latitude + viewportHeight / 2.0,
                cameraPosition.longitude + viewportWidth / 2.0,
            ),
        )
    }
}
