package com.worldwidewaves.shared.events.utils

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

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min

object GeoUtils {

    const val EPSILON = 1e-9 // A small tolerance value for double precision errors

    /**
     * The minimum perceptible difference in meters for calculating optimal latitude band widths.
     *
     * Value: 10.0 meters
     *
     * Rationale for this value:
     * 1. Visual Perception: At common digital map zoom levels, 10 meters is generally visible
     *    and distinguishable to the human eye.
     *
     * 2. GPS Accuracy: Exceeds typical consumer GPS accuracy (3-5 meters), avoiding false precision.
     *
     * 3. Map Scales: At a 1:25,000 scale (common for detailed topographic maps), 10 meters
     *    corresponds to 0.4 mm, which is easily visible on physical maps.
     *
     * 4. Display Resolution: Visible on high-resolution displays (300-500 PPI) at reasonable zoom levels.
     *
     * 5. Performance: Provides a good balance between precision and computational efficiency
     *    for latitude band calculations.
     *
     * 6. Versatility: Suitable for general-purpose mapping and navigation applications dealing
     *    with global or regional scales.
     *
     */
    const val MIN_PERCEPTIBLE_DIFFERENCE = 10.0 // 10 meters - perceptible speed difference
    const val EARTH_RADIUS = 6378137.0 // WGS84 Ellipsoid: Semi-major axis (equatorial radius), in meters

    // Extension function to convert degrees to radians
    fun Double.toRadians(): Double = this * (PI / 180)
    fun Double.toDegrees(): Double = this * 180.0 / PI

    data class Vector2D(val x: Double, val y: Double) {
        fun cross(other: Vector2D): Double = this.x * other.y - this.y * other.x
    }

    // ----------------------------------------------------------------------------

    fun isLongitudeEqual(lng1: Double, lng2: Double): Boolean =
        abs(lng1 - lng2) < EPSILON

    fun isLongitudeInRange(lng: Double, start: Double, end: Double): Boolean {
        return if (start <= end) {
            // The range doesn't cross the date line
            lng in start..end
        } else {
            // The range crosses the date line
            lng >= start || lng <= end
        }
    }

    fun isLatitudeInRange(lat: Double, start: Double, end: Double): Boolean =
        lat in min(start, end)..max(start, end)

    // ----------------------------------------------------------------------------

    /**
     * Calculates the distance between two longitudes at a given latitude using the Haversine formula.
     *
     */
    fun calculateDistance(lon1: Double, lon2: Double, lat: Double): Double {
        val dLon = (lon2 - lon1) * (PI / 180) // Convert degrees to radians
        val latRad = lat * (PI / 180) // Convert degrees to radians
        return abs(EARTH_RADIUS * dLon * cos(latRad))
    }

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
    fun isPointOnSegment(point: Position, segment: Segment): Boolean {
        // Calculate the differences
        val dLat = segment.end.lat - segment.start.lat
        val dLng = segment.end.lng - segment.start.lng

        // Handle special cases: horizontal and vertical segments
        if (abs(dLat) < EPSILON)  // Horizontal segment
            return abs(point.lat - segment.start.lat) < EPSILON &&
                    isLongitudeInRange(point.lng, segment.start.lng, segment.end.lng)

        if (abs(dLng) < EPSILON)  // Vertical segment
            return isLongitudeEqual(point.lng, segment.start.lng) &&
                    point.lat in minOf(segment.start.lat, segment.end.lat)..maxOf(segment.start.lat, segment.end.lat)

        // Calculate the parametric value t for the point
        val tLat = (point.lat - segment.start.lat) / dLat
        val tLng = (point.lng - segment.start.lng) / dLng

        // Check if t values are the same and within the range [0, 1]
        return abs(tLat - tLng) < EPSILON && tLat in 0.0..1.0
    }

}