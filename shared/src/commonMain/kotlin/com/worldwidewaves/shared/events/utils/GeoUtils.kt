package com.worldwidewaves.shared.events.utils

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

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.max
import kotlin.math.min
import kotlin.math.atan2
import kotlin.math.sqrt

object GeoUtils {
    /**
     * Collection of lightweight geodesic helpers reused across the shared module.
     *
     * Main responsibilities:
     * • Angle ↔ radians conversions (`toRadians` / `toDegrees`)
     * • Distance estimations on a WGS-84 ellipsoid (great-circle lon/lat helpers)
     * • Tiny predicates to compare / clamp coordinates while accounting for
     *   floating-point imprecision (EPSILON)
     * • Convenience functions used by wave algorithms & map logic
     *   (range checks, "point on segment", …).
     *
     * All maths stay intentionally *simple* to keep the code size and runtime
     * cost low – accuracy is "good enough" for UI visualisation and hit
     * predictions (< 10 m error margin).
     */
    // Scientifically justified coordinate precision epsilon:
    // IEEE 754 double precision provides ~15-17 decimal digits
    // At equatorial circumference (40,075,017m), 1e-9 degrees ≈ 0.11mm
    // This provides sub-millimeter precision for geodetic calculations
    const val EPSILON = 1e-9

    const val MIN_PERCEPTIBLE_SPEED_DIFFERENCE = 10000.0 // Adjustment variable to manage the nb of wave splits

    // WGS-84 Ellipsoid constants with scientific justification:
    // Semi-major axis (equatorial radius) as defined by WGS-84 datum
    // Reference: NIMA Technical Report TR8350.2 (2000)
    const val EARTH_RADIUS = 6378137.0 // meters

    // Extension function to convert degrees to radians
    fun Double.toRadians(): Double = this * (PI / 180)

    fun Double.toDegrees(): Double = this * 180.0 / PI

    data class Vector2D(
        val x: Double,
        val y: Double,
    ) {
        fun cross(other: Vector2D): Double = this.x * other.y - this.y * other.x
    }

    // ----------------------------------------------------------------------------

    fun isLongitudeEqual(
        lng1: Double,
        lng2: Double,
    ): Boolean = abs(lng1 - lng2) < EPSILON

    fun isLongitudeInRange(
        lng: Double,
        start: Double,
        end: Double,
    ): Boolean =
        if (start <= end) {
            // The range doesn't cross the date line
            lng in start..end
        } else {
            // The range crosses the date line
            lng >= start || lng <= end
        }

    fun isLatitudeInRange(
        lat: Double,
        start: Double,
        end: Double,
    ): Boolean = lat in min(start, end)..max(start, end)

    // ----------------------------------------------------------------------------

    /**
     * Calculates the approximate distance between two longitudes at a given latitude.
     * Uses a planar approximation that is fast but becomes inaccurate for long distances.
     *
     * Mathematical basis: Assumes the Earth is locally flat at the given latitude.
     * Error increases with distance and proximity to poles.
     * Acceptable for UI visualization and short distances (<100km).
     *
     * @param lon1 First longitude in degrees
     * @param lon2 Second longitude in degrees
     * @param lat Latitude in degrees where the distance is measured
     * @return Approximate distance in meters
     */
    fun calculateDistanceFast(
        lon1: Double,
        lon2: Double,
        lat: Double,
    ): Double {
        val dLon = (lon2 - lon1) * (PI / 180) // Convert degrees to radians
        val latRad = lat * (PI / 180) // Convert degrees to radians
        return abs(EARTH_RADIUS * dLon * cos(latRad))
    }

    /**
     * Calculates the great circle distance between two longitude points at a given latitude.
     * Uses the Haversine formula for accurate geodesic calculations.
     *
     * Mathematical basis: Treats the Earth as a perfect sphere and calculates
     * the shortest distance along the surface (great circle arc).
     * Accurate for all distances but computationally more expensive.
     *
     * @param lon1 First longitude in degrees
     * @param lon2 Second longitude in degrees
     * @param lat Latitude in degrees where the distance is measured
     * @return Accurate distance in meters using great circle calculation
     */
    fun calculateDistanceAccurate(
        lon1: Double,
        lon2: Double,
        lat: Double,
    ): Double {
        val lat1Rad = lat * (PI / 180)
        val lat2Rad = lat * (PI / 180) // Same latitude for longitude distance
        val deltaLonRad = (lon2 - lon1) * (PI / 180)

        val a = sin(0.0) * sin(0.0) + // deltaLat = 0 for longitude distance
                cos(lat1Rad) * cos(lat2Rad) *
                sin(deltaLonRad / 2) * sin(deltaLonRad / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return EARTH_RADIUS * c
    }

    /**
     * Calculates distance between two longitudes at a given latitude.
     * Uses fast approximation for backward compatibility with existing code.
     *
     * For new code, consider using calculateDistanceFast() or calculateDistanceAccurate()
     * explicitly based on your accuracy requirements.
     */
    fun calculateDistance(
        lon1: Double,
        lon2: Double,
        lat: Double,
    ): Double = calculateDistanceFast(lon1, lon2, lat)

    /**
     * Calculates the distance for a longitude width at a given latitude.
     * Uses fast approximation for backward compatibility.
     */
    fun calculateDistance(
        lonWidth: Double,
        lat: Double,
    ): Double = calculateDistance(0.0, lonWidth, lat)

    /**
     * Checks if a given point lies on a line segment.
     *
     * This function determines if a point is on a line segment by calculating the cross product
     * of the vectors formed by the segment's endpoints and the point. If the cross product is
     * close to zero (within a small tolerance), the point is considered to be on the line segment.
     * Additionally, the function checks if the point's coordinates are within the bounds of the
     * segment's endpoints.
     *
     */
    fun isPointOnSegment(
        point: Position,
        segment: Segment,
    ): Boolean {
        // Calculate the differences
        val dLat = segment.end.lat - segment.start.lat
        val dLng = segment.end.lng - segment.start.lng

        // Handle special cases: horizontal and vertical segments
        if (abs(dLat) < EPSILON) {
            // Horizontal segment
            return abs(point.lat - segment.start.lat) < EPSILON &&
                isLongitudeInRange(point.lng, segment.start.lng, segment.end.lng)
        }

        if (abs(dLng) < EPSILON) {
            // Vertical segment
            return isLongitudeEqual(point.lng, segment.start.lng) &&
                point.lat in minOf(segment.start.lat, segment.end.lat)..maxOf(segment.start.lat, segment.end.lat)
        }

        // Calculate the parametric value t for the point
        val tLat = (point.lat - segment.start.lat) / dLat
        val tLng = (point.lng - segment.start.lng) / dLng

        // Check if t values are the same and within the range [0, 1]
        return abs(tLat - tLng) < EPSILON && tLat in 0.0..1.0
    }
}
