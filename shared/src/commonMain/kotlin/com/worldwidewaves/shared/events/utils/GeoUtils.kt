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

// ----------------------------------------------------------------------------

object GeoUtils {

    const val EPSILON = 1e-9 // A small tolerance value for double precision errors

    const val MIN_PERCEPTIBLE_DIFFERENCE = 10.0 // 10 meters - perceptible speed difference
    const val EARTH_RADIUS = 6371000.0 // Radius of the Earth in meters

    // Extension function to convert degrees to radians
    fun Double.toRadians(): Double = this * (PI / 180)
    fun Double.toDegrees(): Double = this * 180.0 / PI

    data class Vector2D(val x: Double, val y: Double) {
        fun cross(other: Vector2D): Double = this.x * other.y - this.y * other.x
    }

    /**
     * Normalizes a longitude to the range [-180, 180].
     */
    fun normalizeLongitude(lon: Double): Double {
        var normalizedLon = lon % 360
        if (normalizedLon > 180) normalizedLon -= 360
        if (normalizedLon < -180) normalizedLon += 360
        return normalizedLon
    }

    fun normalizedLongitudeDifference(lng1: Double, lng2: Double): Double {
        var diff = normalizeLongitude(lng1) - normalizeLongitude(lng2)
        if (diff > 180) diff -= 360
        if (diff <= -180) diff += 360
        return diff
    }

    fun isLongitudeEqual(lng1: Double, lng2: Double): Boolean {
        return abs(normalizeLongitude(lng1 - lng2)) < EPSILON
    }

    fun isLongitudeInRange(lng: Double, start: Double, end: Double): Boolean {
        val normalizedLng = normalizeLongitude(lng)
        val normalizedStart = normalizeLongitude(start)
        val normalizedEnd = normalizeLongitude(end)

        return if (normalizedStart <= normalizedEnd) {
            // The range doesn't cross the date line
            normalizedLng in normalizedStart..normalizedEnd
        } else {
            // The range crosses the date line
            normalizedLng >= normalizedStart || normalizedLng <= normalizedEnd
        }
    }

    fun isLatitudeInRange(lat: Double, start: Double, end: Double): Boolean {
        return lat in min(start, end)..max(start, end)
    }

    /**
     * Calculates the distance between two longitudes at a given latitude using the Haversine formula.
     *
     * @param lon1 The first longitude in degrees.
     * @param lon2 The second longitude in degrees.
     * @param lat The latitude in degrees.
     * @return The distance between the two longitudes at the given latitude in meters.
     */
    fun calculateDistance(lon1: Double, lon2: Double, lat: Double): Double {
        val dLon = (lon2 - lon1) * (PI / 180) // Convert degrees to radians
        val latRad = lat * (PI / 180) // Convert degrees to radians
        val distance = EARTH_RADIUS * dLon * cos(latRad)
        return abs(distance)
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
        // Normalize longitudes
        val startLng = normalizeLongitude(segment.start.lng)
        val endLng = normalizeLongitude(segment.end.lng)
        val pointLng = normalizeLongitude(point.lng)

        // Calculate the differences
        val dLat = segment.end.lat - segment.start.lat
        val dLng = normalizeLongitude(endLng - startLng)

        // Handle special cases: horizontal and vertical segments
        if (abs(dLat) < EPSILON) {  // Horizontal segment
            return abs(point.lat - segment.start.lat) < EPSILON &&
                    isLongitudeInRange(pointLng, startLng, endLng)
        }
        if (abs(dLng) < EPSILON) {  // Vertical segment
            return isLongitudeEqual(pointLng, startLng) &&
                    point.lat in minOf(segment.start.lat, segment.end.lat)..maxOf(segment.start.lat, segment.end.lat)
        }

        // Calculate the parametric value t for the point
        val tLat = (point.lat - segment.start.lat) / dLat
        val tLng = normalizeLongitude(pointLng - startLng) / dLng

        // Check if t values are the same and within the range [0, 1]
        return abs(tLat - tLng) < EPSILON && tLat in 0.0..1.0
    }

}