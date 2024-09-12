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

import com.worldwidewaves.shared.events.utils.GeoUtils.isPointOnLineSegment
import com.worldwidewaves.shared.events.utils.PolygonUtils.SplitPolygonResult.LeftOrRight.LEFT
import com.worldwidewaves.shared.events.utils.PolygonUtils.SplitPolygonResult.LeftOrRight.RIGHT
import kotlin.random.Random

// ----------------------------------------------------------------------------

object PolygonUtils {

    data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

    // ------------------------------------------------------------------------

    abstract class CutPolygon(val cutId: Int) : Polygon() { // Part of a polygon after cut
        abstract override fun createNew() : CutPolygon
    }

    class LeftCutPolygon(cutId: Int) : CutPolygon(cutId) { // Left part of a polygon after cut
        override fun createNew() = LeftCutPolygon(cutId)
    }

    class RightCutPolygon(cutId: Int) : CutPolygon(cutId) { // Right part of a polygon after cut
        override fun createNew() = RightCutPolygon(cutId)
    }

    data class SplitPolygonResult(val left: List<LeftCutPolygon>, val right: List<RightCutPolygon>) {
        enum class LeftOrRight { LEFT, RIGHT }
        companion object {

            fun fromSinglePolygon(
                polygon: Polygon, cutId: Int, leftOrRight: LeftOrRight = RIGHT
            ): SplitPolygonResult {
                return if (polygon.size > 1) {
                    when (leftOrRight) {
                        LEFT -> SplitPolygonResult(listOf(polygon.toLeft(cutId)), emptyList())
                        RIGHT -> SplitPolygonResult(emptyList(), listOf(polygon.toRight(cutId)))
                    }
                } else empty()
            }

            fun empty() = SplitPolygonResult(emptyList(), emptyList())
        }
    }

    // ------------------------------------------------------------------------

    val List<Position>.toPolygon : Polygon
        get() = Polygon().apply { this@toPolygon.forEach { add(it) } }

    val List<Position>.toLeftPolygon: (Int) -> LeftCutPolygon
        get() = { cutId -> LeftCutPolygon(cutId).apply { this@toLeftPolygon.forEach { add(it) } } }

    val List<Position>.toRightPolygon: (Int) -> RightCutPolygon
        get() = { cutId -> RightCutPolygon(cutId).apply { this@toRightPolygon.forEach { add(it) } } }

    // ------------------------------------------------------------------------

    fun Polygon.toLeft(cutId: Int) =
        LeftCutPolygon(cutId).convertFrom<LeftCutPolygon>(this)

    fun Polygon.toRight(cutId: Int) =
        RightCutPolygon(cutId).convertFrom<RightCutPolygon>(this)

    // ------------------------------------------------------------------------

    /**
     * Calculates the bounding box of a polygon.
     */
    fun Polygon.bbox(): BoundingBox = polygonsBbox(listOf(this))

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
    fun Polygon.containsPosition(tap: Position): Boolean {
        if (isEmpty()) return false
        var (bx, by) = last()!!.let { it.lat - tap.lat to it.lng - tap.lng }
        var depth = 0

        for (point in this) {
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
    fun Polygon.splitByLongitude(longitudeToCut: Double): SplitPolygonResult {
        val cutId = Random.nextInt(1, Int.MAX_VALUE)
        val leftSide = LeftCutPolygon(cutId)
        val rightSide = RightCutPolygon(cutId)

        val minLongitude = minOfOrNull { it.lng } ?: return SplitPolygonResult.empty()
        val maxLongitude = maxOfOrNull { it.lng } ?: return SplitPolygonResult.empty()

        return when {
            longitudeToCut > maxLongitude ->
                SplitPolygonResult.fromSinglePolygon(this, cutId, LEFT)
            longitudeToCut < minLongitude ->
                SplitPolygonResult.fromSinglePolygon(this, cutId, RIGHT)
            else -> {
                // Separate the polygon into two parts based on the cut longitude
                for (point in this) {
                    val nextPoint = point.next ?: first()!!

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

    // ------------------------------------------------------------------------

    /**
     * Determines if a point is inside any of the given polygons.
     */
    fun isPointInPolygons(tap: Position, polygons: List<Polygon>): Boolean {
        return polygons.any { it.containsPosition(tap) }
    }

    /**
     * Calculates the bounding box of a multi-polygon.
     *
     * This function takes a multi-polygon represented as a list of [Position]objects and returns a
     * [BoundingBox] object that encompasses the entire polygon.
     *
     * It throws an [IllegalArgumentException] if the input polygon is empty.
     *
     */
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

    // ------------------------------------------------------------------------

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
                    point.id != compPoint.id && point.id != nextCompPoint.id &&
                            isPointOnLineSegment(point, Segment(compPoint, nextCompPoint)) &&
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

    // ------------------------------------------------------------------------

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


