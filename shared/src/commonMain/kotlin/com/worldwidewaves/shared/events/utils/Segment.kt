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

import com.worldwidewaves.shared.events.utils.GeoUtils.EPSILON
import kotlin.math.abs

/**
 * Represents a segment defined by its start and end positions.
 */
data class Segment(
    val start: Position,
    val end: Position,
) {
    /**
     * Calculates the intersection of the segment with a given longitude
     * and returns a CutPosition if the segment intersects the longitude.
     */
    fun intersectWithLng(
        cutId: Int,
        cutLng: Double,
    ): CutPosition? {
        // Calculate the latitude of intersection
        val latDiff = end.lat - start.lat
        val lngDiff = end.lng - start.lng

        // Check for vertical line - No unique intersection for a vertical line
        if (abs(lngDiff) < EPSILON) return null

        val t = (cutLng - start.lng) / lngDiff
        val lat = start.lat + t * latDiff

        // Check if the intersection point is on the segment
        if (t < 0 || t > 1) return null

        // Determine the direction and create the CutPosition
        return when {
            start.lng == cutLng && end.lng == cutLng ->
                null // No intersection for a vertical line
            (end.lng - start.lng) > 0 -> // Moving eastward
                CutPosition(lat = lat, lng = cutLng, cutId = cutId, cutLeft = start, cutRight = end)
            else -> // Moving westward
                CutPosition(lat = lat, lng = cutLng, cutId = cutId, cutLeft = end, cutRight = start)
        }
    }

    /**
     * Calculates the intersection of the segment with a given other segment
     * and returns a CutPosition if the segments intersects.
     */
    fun intersectWithSegment(
        cutId: Int,
        other: Segment,
    ): CutPosition? {
        val (x1, y1) = start.lng to start.lat
        val (x2, y2) = end.lng to end.lat
        val (x3, y3) = other.start.lng to other.start.lat
        val (x4, y4) = other.end.lng to other.end.lat

        val denominator = (y4 - y3) * (x2 - x1) - (x4 - x3) * (y2 - y1)
        if (abs(denominator) < EPSILON) return null // Lines are parallel

        val ua = ((x4 - x3) * (y1 - y3) - (y4 - y3) * (x1 - x3)) / denominator
        val ub = ((x2 - x1) * (y1 - y3) - (y2 - y1) * (x1 - x3)) / denominator

        if (ua < 0 || ua > 1 || ub < 0 || ub > 1) return null // Intersection point is outside of both line segments

        val x = x1 + ua * (x2 - x1)
        val y = y1 + ua * (y2 - y1)

        // Determine cut side from the *polygon* segment (`other`)
        val (polyStart, polyEnd) = other.start to other.end
        val (west, east) =
            if (polyStart.lng <= polyEnd.lng) {
                polyStart to polyEnd
            } else {
                polyEnd to polyStart
            }

        return CutPosition(
            lat = y,
            lng = x,
            cutId = cutId,
            cutLeft = west,
            cutRight = east,
        )
    }

    // --------------------------------------------------------------------
    // Plain (no cut-tracking) intersection helpers
    // --------------------------------------------------------------------

    /**
     * Calculates the intersection of this segment with a vertical longitude line
     * and returns the intersection point as a [Position] or `null` when no
     * intersection occurs (parallel or out of segment bounds).
     */
    fun intersectWithLng(cutLng: Double): Position? {
        val latDiff = end.lat - start.lat
        val lngDiff = end.lng - start.lng

        // Vertical segment – no unique intersection
        if (abs(lngDiff) < EPSILON) return null

        val t = (cutLng - start.lng) / lngDiff
        if (t < 0 || t > 1) return null // outside segment

        val lat = start.lat + t * latDiff
        return Position(lat, cutLng)
    }

    /**
     * Calculates the intersection point of this segment with [other] and returns it
     * as a plain [Position], or `null` when the segments do not intersect.
     */
    fun intersectWithSegment(other: Segment): Position? {
        val (x1, y1) = start.lng to start.lat
        val (x2, y2) = end.lng to end.lat
        val (x3, y3) = other.start.lng to other.start.lat
        val (x4, y4) = other.end.lng to other.end.lat

        val denominator = (y4 - y3) * (x2 - x1) - (x4 - x3) * (y2 - y1)
        if (abs(denominator) < EPSILON) return null // parallel

        val ua = ((x4 - x3) * (y1 - y3) - (y4 - y3) * (x1 - x3)) / denominator
        val ub = ((x2 - x1) * (y1 - y3) - (y2 - y1) * (x1 - x3)) / denominator
        if (ua < 0 || ua > 1 || ub < 0 || ub > 1) return null // outside segments

        val x = x1 + ua * (x2 - x1)
        val y = y1 + ua * (y2 - y1)
        return Position(lat = y, lng = x)
    }
}
