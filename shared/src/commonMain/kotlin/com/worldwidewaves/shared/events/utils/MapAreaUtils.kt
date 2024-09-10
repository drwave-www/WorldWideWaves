package com.worldwidewaves.shared.events.utils

/*
 * Copyright 2024 DrWave
 *
 * WorldWideWaves is an ephemeral mobile app designed to orchestrate human waves through cities and countries,
 * culminating in a global wave. The project aims to transcend physical and cultural boundaries, fostering unity,
 * community, and shared human experience by leveraging real-time coordination and location-based services.
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

import com.worldwidewaves.shared.events.utils.PolygonUtils.SplitPolygonResult.ResultPosition.LEFT
import com.worldwidewaves.shared.events.utils.PolygonUtils.SplitPolygonResult.ResultPosition.RIGHT
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

// ----------------------------------------------------------------------------

object PolygonUtils {

    /**
     * Determines if a point is inside a polygon.
     *
     * This function implements a ray casting algorithm to determine if a given point(`tap`) lies
     * inside a polygon.
     *
     * It's based on the algorithm described in
     * [https://github.com/KohlsAdrian/google_maps_utils/blob/master/lib/poly_utils.dart](https://github.com/KohlsAdrian/google_maps_utils/blob/master/lib/poly_utils.dart).
     *
     */
    fun isPointInPolygon(tap: Position, polygon: Polygon): Boolean {
        if (polygon.isEmpty()) return false
        var (bx, by) = polygon.last()!!.let { it.lat - tap.lat to it.lng - tap.lng }
        var depth = 0

        for (point in polygon) {
            val (ax, ay) = bx to by
            bx = point.lat - tap.lat
            by = point.lng - tap.lng

            if ((ay < 0 && by < 0) || (ay > 0 && by > 0) || (ax < 0 && bx < 0))
                continue

            val lx = ax - ay * (bx - ax) / (by - ay)
            if (lx == 0.0)
                return true
            if (lx > 0) depth++
        }

        return (depth and 1) == 1
    }

    /**
     * Determines if a point is inside any of the given polygons.
     */
    fun isPointInPolygons(tap: Position, polygons: List<Polygon>): Boolean {
        return polygons.any { isPointInPolygon(tap, it) }
    }

// ----------------------------------------------------------------------------

    /**
     * Calculates the bounding box of a polygon.
     *
     * This function takes a polygon represented as a list of [Position]objects and returns a
     * [BoundingBox] object that encompasses the entire polygon.
     *
     * It throws an [IllegalArgumentException] if the input polygon is empty.
     *
     */

    data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

    fun polygonsBbox(polygons: List<Polygon>): BoundingBox {
        if (polygons.isEmpty() || polygons.all { it.isEmpty() })
            throw IllegalArgumentException("Event area cannot be empty, cannot determine bounding box")

        val (minLatitude, minLongitude, maxLatitude, maxLongitude) = polygons.flatten().fold(
            Quadruple(
                Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY
            )
        ) { (minLat, minLon, maxLat, maxLon), pos ->
            Quadruple(
                minOf(minLat, pos.lat), minOf(minLon, pos.lng),
                maxOf(maxLat, pos.lat), maxOf(maxLon, pos.lng)
            )
        }

        return BoundingBox(
            sw = Position(minLatitude, minLongitude),
            ne = Position(maxLatitude, maxLongitude)
        )
    }

    fun polygonBbox(polygon: Polygon): BoundingBox = polygonsBbox(listOf(polygon))

// ----------------------------------------------------------------------------

    /**
     * Data class representing the result of splitting a polygon into two parts.
     *
     */
    data class SplitPolygonResult(val left: List<LeftCutPolygon>, val right: List<RightCutPolygon>) {
        enum class ResultPosition { LEFT, RIGHT }
        companion object {
            fun fromPolygon(
                polygon: CutPolygon,
                resultPosition: ResultPosition = RIGHT
            ): SplitPolygonResult {
                if (resultPosition == LEFT && polygon !is LeftCutPolygon || resultPosition == RIGHT && polygon !is RightCutPolygon)
                    throw IllegalArgumentException("Invalid polygon type for result position")
                return if (polygon.size > 1) {
                    when (resultPosition) {
                        LEFT -> SplitPolygonResult(listOf(polygon as LeftCutPolygon), emptyList())
                        RIGHT -> SplitPolygonResult(emptyList(), listOf(polygon as RightCutPolygon))
                    }
                } else empty()
            }
            fun empty() = SplitPolygonResult(emptyList(), emptyList())
        }
        fun polygons() = left + right
    }

    /**
     * Splits a polygon by a given longitude.
     *
     * This function takes a polygon represented as a list of [Position] objects and a longitude value
     * (`longitudeToCut`) as input. It splits the polygon into two parts: the left part containing
     * points with longitudes less than or equal to `longitudeToCut`, and the right part containing
     * points with longitudes greater than or equal to `longitudeToCut`.*
     *
     * If the `longitudeToCut` is completely outside the bounds of the polygon, the entire polygon is
     * returned either on the left or right side, depending on whether the cut is to the east or west
     * of the polygon.
     *
     * The function handles cases where the polygon intersects the cut line by calculating intersection
     * points and adding them to both the left and right sides.
     *
     */
    fun splitPolygonByLongitude(
        polygon: Polygon,
        longitudeToCut: Double
    ): SplitPolygonResult {
        val cutId = Random.nextInt(1, Int.MAX_VALUE)
        val leftSide = LeftCutPolygon(cutId)
        val rightSide = RightCutPolygon(cutId)

        val minLongitude = polygon.minOfOrNull { it.lng } ?: return SplitPolygonResult.empty()
        val maxLongitude = polygon.maxOfOrNull { it.lng } ?: return SplitPolygonResult.empty()

        return when {
            longitudeToCut > maxLongitude ->
                SplitPolygonResult.fromPolygon(LeftCutPolygon.convert(polygon, cutId), LEFT)
            longitudeToCut < minLongitude ->
                SplitPolygonResult.fromPolygon(RightCutPolygon.convert(polygon, cutId), RIGHT)
            else -> {
                // Separate the polygon into two parts based on the cut longitude
                for (point in polygon) {
                    val nextPoint = point.next ?: polygon.first()!!

                    val intersection = Position(point.lat +
                            (nextPoint.lat - point.lat) *
                            (longitudeToCut - point.lng) /
                            (nextPoint.lng - point.lng),
                        longitudeToCut
                    )

                    if (point.lng <= longitudeToCut) leftSide.add(point)
                    if (point.lng >= longitudeToCut) rightSide.add(point)

                    when { // If required cut the polygon at the intersection point
                           // and add the cut point on both sides
                        point.lng < longitudeToCut && nextPoint.lng > longitudeToCut ->
                            intersection.toCutPosition(cutId = cutId, cutLeft = point, cutRight = nextPoint)
                        point.lng > longitudeToCut && nextPoint.lng < longitudeToCut ->
                            intersection.toCutPosition(cutId = cutId, cutLeft = nextPoint, cutRight = point)
                        else -> null
                    }?.let {
                        leftSide.add(it)
                        rightSide.add(it)
                    }
                }

                // Close the polygons if they are not already closed
                if (leftSide.isNotEmpty() && leftSide.first() != leftSide.last())
                    leftSide.add(leftSide.first()!!)
                if (rightSide.isNotEmpty() && rightSide.first() != rightSide.last())
                    rightSide.add(rightSide.first()!!)

                // Group the points into ring polygons
                val leftPolygons = groupIntoRingPolygons(leftSide).filter { it.size > 1 }
                val rightPolygons = groupIntoRingPolygons(rightSide).filter { it.size > 1 }

                SplitPolygonResult(leftPolygons, rightPolygons)
            }
        }
    }

    /**
     * Groups a list of positions into ring polygons.
     *
     * This function takes a list of positions representing a polygon and splits it into multiple ring polygons.
     * A ring polygon is a closed loop of positions where the first and last positions are the same.
     *
     */
    private fun <T : CutPolygon> groupIntoRingPolygons(polygon: T): List<T> {
        // Polygon cut type conservation
        val polygons: MutableList<Polygon> = mutableListOf()

        // Polygon cut type conservation
        val currentPolygon  = polygon.createNew()

        for (point in polygon) {
            if ((point.id != polygon.first()!!.id) && point == polygon.last()!!)
                break // Do not take the last point
            currentPolygon.add(point)

            if (currentPolygon.size > 1) {
                val shouldSplit = polygon.any { compPoint ->
                    val nextCompPoint = compPoint.next ?: polygon.first()!!
                    val segment = Segment(compPoint, nextCompPoint)
                    point.id != compPoint.id && point.id != nextCompPoint.id && // FIXME check
                            isPointOnLineSegment(point, segment) &&
                            (point.id != nextCompPoint.id && point != compPoint)
                }
                if (shouldSplit) {
                    if (point != currentPolygon.first()!!)
                        currentPolygon.add(currentPolygon.first()!!)
                    polygons.add(currentPolygon.copy())
                    currentPolygon.clear()
                    currentPolygon.add(point)
                    continue
                }
            }

            if (currentPolygon.size > 1 && point == currentPolygon.first()) {
                polygons.add(currentPolygon.copy())
                currentPolygon.clear()
            }
        }

        // Close the last polygon if it is not already closed
        if (currentPolygon.isNotEmpty() && currentPolygon.first() != currentPolygon.last()) {
            currentPolygon.add(currentPolygon.first()!!)
            polygons.add(currentPolygon)
        }

        @Suppress("UNCHECKED_CAST")
        return (polygons as List<T>).filter { // Filter out invalid polygons (lines and dots)
            it.size >= 3 && !(it.all { p -> p.lat == it.first()!!.lat } || it.all { p -> p.lng == it.first()!!.lng })
        }
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
    private const val EPSILON =
        1e-10 // A small tolerance value used to account for floating-point precision errors.

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

// ----------------------------------------------------------------------------

    /**
     * Converts a list of polygons into a GeoJSON string.
     *
     */
    fun convertPolygonsToGeoJson(polygons: List<Polygon>): String {
        val features = polygons.map { polygon ->
            val coordinates = polygon.map { listOf(it.lng, it.lat) }
            """
        {
            "type": "Feature",
            "geometry": {
                "type": "Polygon",
                "coordinates": [${coordinates}]
            }
        }
        """.trimIndent()
        }
        return """
    {
        "type": "FeatureCollection",
        "features": [${features.joinToString(",")}]
    }
    """.trimIndent()
    }
}

// ----------------------------------------------------------------------------

object GeoUtils {

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

}
