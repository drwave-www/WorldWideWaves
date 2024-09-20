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

    private const val EPSILON = 1e-10 // A small tolerance value for double precision errors

    const val MIN_PERCEPTIBLE_DIFFERENCE = 10.0 // 10 meters - perceptible speed difference
    const val EARTH_RADIUS = 6371000.0 // Radius of the Earth in meters

    // Extension function to convert degrees to radians
    fun Double.toRadians(): Double = this * (PI / 180)
    fun Double.toDegrees(): Double = this * 180.0 / PI

    /**
     * Normalizes a longitude to the range [-180, 180].
     */
    fun normalizeLongitude(lon: Double): Double {
        var normalizedLon = lon % 360
        if (normalizedLon > 180) normalizedLon -= 360
        if (normalizedLon < -180) normalizedLon += 360
        return normalizedLon
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
        val earthRadius = 6371000.0 // Earth radius in meters
        val dLon = (lon2 - lon1) * (PI / 180) // Convert degrees to radians
        val latRad = lat * (PI / 180) // Convert degrees to radians
        val distance = earthRadius * dLon * cos(latRad)
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
    fun isPointOnLineSegment(point: Position, segment: Segment): Boolean {
        val crossProduct = (segment.end.lat - segment.start.lat) * (point.lng - segment.start.lng) -
                (segment.end.lng - segment.start.lng) * (point.lat - segment.start.lat)
        return abs(crossProduct) < EPSILON &&
                point.lat in min(segment.start.lat, segment.end.lat)..max(
            segment.start.lat,
            segment.end.lat
        ) && point.lng in min(segment.start.lng, segment.end.lng)..max(
            segment.start.lng,
            segment.end.lng
        )
    }

}