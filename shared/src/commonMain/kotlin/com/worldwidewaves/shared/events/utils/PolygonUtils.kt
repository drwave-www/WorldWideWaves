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

import com.worldwidewaves.shared.events.utils.PolygonUtils.SplitPolygonResult.Companion.fromSinglePolygon
import com.worldwidewaves.shared.events.utils.PolygonUtils.SplitPolygonResult.LeftOrRight.LEFT
import com.worldwidewaves.shared.events.utils.PolygonUtils.SplitPolygonResult.LeftOrRight.RIGHT
import kotlin.random.Random

// ----------------------------------------------------------------------------

object PolygonUtils {

    data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

    // ------------------------------------------------------------------------

    abstract class CutPolygon(val cutId: Int) : Polygon() { // Part of a polygon after cut
        abstract override fun createNew() : CutPolygon
    }

    class LeftCutPolygon(cutId: Int) : CutPolygon(cutId) { // Left part of a polygon after cut
        override fun createNew(): LeftCutPolygon = LeftCutPolygon(cutId)
    }

    class RightCutPolygon(cutId: Int) : CutPolygon(cutId) { // Right part of a polygon after cut
        override fun createNew(): RightCutPolygon = RightCutPolygon(cutId)
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
        LeftCutPolygon(cutId).convertFrom(this)

    fun Polygon.toRight(cutId: Int) =
        RightCutPolygon(cutId).convertFrom(this)

    private fun <T: Polygon> T.convertFrom(polygon: Polygon) : T {
        head = polygon.head
        tail = polygon.tail
        positionsIndex.putAll(polygon.positionsIndex)
        cutPositions.addAll(polygon.cutPositions)
        return this
    }

    private fun <T: Polygon> T.close() : T {
        if (isNotEmpty() && first() != last()) {
            add(first()!!)
        }
        return this
    }

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
    fun Polygon.splitByLongitude(lngToCut: Double): SplitPolygonResult { // FIXME: implement move = deepCopy + clean
        this.close() // Ensure the polygon is closed
        if (isEmpty() || size < 4) return SplitPolygonResult.empty()

        val cutId = Random.nextInt(1, Int.MAX_VALUE)
        val leftSide =  mutableListOf<LeftCutPolygon>()
        val rightSide = mutableListOf<RightCutPolygon>()
        val currentLeft = LeftCutPolygon(cutId)
        val currentRight = RightCutPolygon(cutId)

        val minLongitude = minOfOrNull { it.lng } ?: return SplitPolygonResult.empty()
        val maxLongitude = maxOfOrNull { it.lng } ?: return SplitPolygonResult.empty()

        return when {
            lngToCut > maxLongitude -> fromSinglePolygon(this, cutId, LEFT)
            lngToCut < minLongitude -> fromSinglePolygon(this, cutId, RIGHT)
            else -> { // Separate the polygon into two parts based on the cut longitude

                val iterator = if (isClockwise()) loopIterator() else reverseLoopIterator()
                var stopPoint = if (isClockwise()) last()!! else first()!! // Security stop
                var prev : Position? = null

                while (iterator.hasNext()) { // Clockwise loop
                    val point = iterator.next()
                    if (point.id == stopPoint.id) break // Stop at the starting point

                    if (prev != null && point == prev) continue // Skip identical points
                    val nextPoint = iterator.viewNext()

                    // Calculate the intersection point with the cut longitude
                    val intersection = Segment(point, nextPoint).intersectWithLng(cutId, lngToCut)

                    fun computeSplitForCurrentPoint() {
                        // Add point to left and/or right side
                        val cutPosition = if (point.lng == lngToCut) point.toPointCut(cutId) else point
                        if (point.lng <= lngToCut) currentLeft.add(cutPosition)
                        if (point.lng >= lngToCut) currentRight.add(cutPosition)

                        // Check if the segment intersects the cut lng and add the intersection point
                        intersection?.let {
                            currentLeft.add(it)
                            currentRight.add(it)
                        }

                        // When we encounter a second cut point, we record the current polygon
                        // and start a new one from the last point
                        if (point.lng == lngToCut || intersection != null) {
                            addPolygonPartIfNeeded(currentLeft, leftSide)
                            addPolygonPartIfNeeded(currentRight, rightSide)
                        }
                    }

                    // Really start to compute from here : start iteration on a CutPosition
                    if ((point.lng == lngToCut || intersection != null) && currentLeft.isEmpty() && currentRight.isEmpty()) {
                        stopPoint = point // Mark the real loop stop
                        computeSplitForCurrentPoint()
                    } else if (currentLeft.isNotEmpty() || currentRight.isNotEmpty()) {
                        computeSplitForCurrentPoint()
                    } else {
                        /* loop until we find a CutPosition */
                    }

                    prev = point
                }

                // Add the last polygons, completing them, to the left and/or right side
                if (leftSide.size > 1) leftSide.add(currentLeft.apply {
                    add(stopPoint.toPointCut(cutId))
                })
                if (rightSide.size > 1) rightSide.add(currentRight.apply {
                    add(stopPoint.toPointCut(cutId))
                })

                // Close the polygons and group the points into ring polygons
                return SplitPolygonResult(
                    reconstructSide(leftSide, currentLeft),
                    reconstructSide(rightSide, currentRight)
                )
            }
        }
    }

    private fun <T : CutPolygon> reconstructSide(side: MutableList<T>, initPolygon: T): List<T> {
        return side.asSequence()
            .filter { it.size > 2 && it.cutPositions.size == 2 }
            .sortedBy { it.cutPositions.minOf { cutPos -> cutPos.lat } }
            .let { reconstructPolygons(it.toList(), initPolygon) }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : CutPolygon> reconstructPolygons(polyLines: List<T>, initPolygon: T): List<T> {
        val result = mutableListOf<T>()
        var current: T = initPolygon.clear() as T

        for (polyLine in polyLines) {
            if (current.isEmpty() || current.last()!!.lng <= polyLine.first()!!.lng) {
                current.addAll(polyLine)
            } else {
                result.add(current.close().copy() as T)
                current = (initPolygon.clear() as T).apply { addAll(polyLine) }
            }
        }

        if (current.isNotEmpty()) {
            result.add(current.close().copy() as T)
        }

        return result
    }

    private fun <T : CutPolygon> addPolygonPartIfNeeded(polygon: T, polygonList: MutableList<T>) {
        if (polygon.size > 2) {
            val lastPoint = polygon.last()!!
            @Suppress("UNCHECKED_CAST")
            polygonList.add(polygon.copy() as T)
            polygon.clear().add(lastPoint)
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

        val (minLat, minLng, maxLat, maxLng) = polygons.flatten().fold(
            Quad(
                Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY
            )
        ) { (minLat, minLon, maxLat, maxLon), pos ->
            Quad(
                minOf(minLat, pos.lat), minOf(minLon, pos.lng),
                maxOf(maxLat, pos.lat), maxOf(maxLon, pos.lng)
            )
        }

        return BoundingBox(
            sw = Position(minLat, minLng),
            ne = Position(maxLat, maxLng)
        )
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


