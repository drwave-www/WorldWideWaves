package com.worldwidewaves.shared.events.utils

/*
 * Copyright 2025 DrWave
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

import com.worldwidewaves.shared.events.utils.PolygonUtils.PolygonSplitResult.Companion.fromSinglePolygon
import com.worldwidewaves.shared.events.utils.PolygonUtils.PolygonSplitResult.LeftOrRight.LEFT
import com.worldwidewaves.shared.events.utils.PolygonUtils.PolygonSplitResult.LeftOrRight.RIGHT
import kotlin.random.Random

// ----------------------------------------------------------------------------

object PolygonUtils {

    data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

    // -- Add-on structures and methods for Polygon ---------------------------

    abstract class CutPolygon(val cutId: Int) : Polygon() { // Part of an initial polygon after cut
        abstract override fun createNew() : CutPolygon
    }

    class LeftCutPolygon(cutId: Int) : CutPolygon(cutId) { // Left part of a polygon after cut
        override fun createNew(): LeftCutPolygon = LeftCutPolygon(cutId)
    }

    class RightCutPolygon(cutId: Int) : CutPolygon(cutId) { // Right part of a polygon after cut
        override fun createNew(): RightCutPolygon = RightCutPolygon(cutId)
    }

    data class PolygonSplitResult(val cutId: Int, val left: List<LeftCutPolygon>, val right: List<RightCutPolygon>) {
        enum class LeftOrRight { LEFT, RIGHT }
        companion object {

            fun fromSinglePolygon(
                polygon: Polygon, cutId: Int, leftOrRight: LeftOrRight = RIGHT
            ): PolygonSplitResult {
                return if (polygon.size > 1) {
                    when (leftOrRight) {
                        LEFT -> PolygonSplitResult(cutId, listOf(polygon.toLeft(cutId)), emptyList())
                        RIGHT -> PolygonSplitResult(cutId, emptyList(), listOf(polygon.toRight(cutId)))
                    }
                } else empty(cutId)
            }

            fun empty(cutId: Int) = PolygonSplitResult(cutId, emptyList(), emptyList())
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
        LeftCutPolygon(cutId).xferFrom(this)

    fun Polygon.toRight(cutId: Int) =
        RightCutPolygon(cutId).xferFrom(this)

    // ------------------------------------------------------------------------

    /**
     * Determines if a point is inside a polygon.
     *
     * This function implements a ray-casting algorithm to determine if a given point(`tap`) lies
     * inside a polygon.
     *
     * It's based on the algorithm available at
     * https://github.com/KohlsAdrian/google_maps_utils/blob/master/lib/poly_utils.dart
     *
     */
    fun Polygon.containsPosition(tap: Position): Boolean {
        require (isNotEmpty()) { return false }
        var (bx, by) = last()!!.let { it.lat - tap.lat to it.lng - tap.lng }
        var depth = 0

        for (point in this) {
            val (ax, ay) = bx to by
            bx = point.lat - tap.lat
            by = point.lng - tap.lng

            if ((ay < 0 && by < 0) || (ay > 0 && by > 0) || (ax < 0 && bx < 0)) {
                continue
            }

            val lx = ax - ay * (bx - ax) / (by - ay)
            if (lx == 0.0) return true
            if (lx > 0) depth++
        }

        return (depth and 1) == 1
    }

    /**
     * Splits a polygon by a given longitude.
     *
     * This function takes a polygon represented as a list of \[Position\] objects and a composed longitude
     * (\`longitudeToCut\`) as input. It splits the polygon into two parts: the left part containing
     * points with longitudes less than or equal to \`longitudeToCut\`, and the right part containing
     * points with longitudes greater than or equal to \`longitudeToCut\`.
     *
     * If the \`longitudeToCut\` is completely outside the bounds of the polygon, the entire polygon is
     * returned either on the left or right side, depending on whether the cut is to the east or west
     * of the polygon.
     *
     * The function handles cases where the polygon intersects the cut line by calculating intersection
     * points and adding them to both the left and right sides.
     *
     */
    fun splitByLongitude(polygon: Polygon, lngToCut: Double): PolygonSplitResult =
        splitByLongitude(polygon,ComposedLongitude.fromLongitude(lngToCut))

    fun splitByLongitude(polygon: Polygon, lngToCut: ComposedLongitude): PolygonSplitResult {
        val workingPolygon = Polygon()
        workingPolygon.addAll(polygon)
        workingPolygon.close().pop() // Ensure the polygon is closed and remove the last point

        val cutId = Random.nextInt(1, Int.MAX_VALUE)

        require(workingPolygon.isNotEmpty() && workingPolygon.size >= 3) {
            return PolygonSplitResult.empty(cutId).also { workingPolygon.close() }
        }

        val minLongitude = workingPolygon.bbox().minLongitude
        val maxLongitude = workingPolygon.bbox().maxLongitude

        val lngBbox = lngToCut.bbox()

        return when {
            lngBbox.minLongitude > maxLongitude -> fromSinglePolygon(workingPolygon, cutId, LEFT)
            lngBbox.maxLongitude < minLongitude -> fromSinglePolygon(workingPolygon, cutId, RIGHT)
            else -> { // Separate the polygon into two parts based on the cut longitude
                // ------------------------------------------------------------------
                // New simple splitter : single pass, single polygon per side
                // ------------------------------------------------------------------

                val leftPoly  = LeftCutPolygon(cutId)
                val rightPoly = RightCutPolygon(cutId)

                fun addPointToSides(point: Position, side: ComposedLongitude.Side) {
                    when {
                        side.isWest() -> leftPoly.add(point)
                        side.isEast() -> rightPoly.add(point)
                        side.isOn()   -> {
                            val cutPt = point.toPointCut(cutId)
                            leftPoly.add(cutPt)
                            rightPoly.add(cutPt)
                        }
                    }
                }

                val points = workingPolygon.toList()
                for (i in points.indices) {
                    val start = points[i]
                    val end   = points[(i + 1) % points.size] // close ring

                    val sideStart = lngToCut.isPointOnLine(start)
                    val sideEnd   = lngToCut.isPointOnLine(end)
                    val intersection = lngToCut.intersectWithSegment(cutId, Segment(start, end))

                    // Emit start point
                    addPointToSides(start, sideStart)

                    // Emit intersection if any
                    intersection?.let {
                        leftPoly.add(it)
                        rightPoly.add(it)
                    }

                    // If this is the last segment, also emit end (otherwise next loop will do)
                    if (i == points.size - 1) {
                        addPointToSides(end, sideEnd)
                    }
                }

                // Prepare resulting lists (only keep valid polygons)
                val leftList = mutableListOf<LeftCutPolygon>()
                val rightList = mutableListOf<RightCutPolygon>()

                if (leftPoly.size >= 3) leftList.add(leftPoly.close())
                if (rightPoly.size >= 3) rightList.add(rightPoly.close())

                // Inject intermediate longitude points if needed
                val completedLeft  = completeLongitudePoints(cutId, lngToCut, leftList)
                val completedRight = completeLongitudePoints(cutId, lngToCut, rightList)

                return PolygonSplitResult(cutId, completedLeft, completedRight)
                    .also { workingPolygon.close() }
            }
        }
    }

    /**
     * Completes the longitude points for a list of polygons.
     *
     * This function takes a `ComposedLongitude` object and a list of polygons, and ensures that the
     * polygons have the necessary points along the longitude. If the `ComposedLongitude` object has
     * more than one position, it inserts new points between the minimum and maximum cut positions
     * of each polygon.
     *
     */
    private inline fun <reified T : CutPolygon> completeLongitudePoints(
        propCutId: Int,
        lngToCut: ComposedLongitude,
        polygons: List<T>
    ): List<T> = if (lngToCut.size() > 1) { // Nothing to complete on straight longitude line
        polygons.map { polygon ->
            val cutPositions = polygon.getCutPositions().filter { it.cutId == propCutId }.sortedBy { it.lat }
            if (cutPositions.size < 2) return@map polygon

            val minCut = cutPositions.minByOrNull { it.lat } // Complete between min and max cuts
            val maxCut = cutPositions.maxByOrNull { it.lat }

            if (minCut != null && maxCut != null) {
                val newPositions = lngToCut.positionsBetween(minCut.lat, maxCut.lat)
                if (newPositions.isNotEmpty()) { // If there are some longitude points in between
                    var currentPosition : Position = if (polygon is LeftCutPolygon) minCut else maxCut
                    for (newPosition in newPositions) {
                        currentPosition = polygon.insertAfter(newPosition, currentPosition.id)
                    }
                }
            }
            polygon
        }
    } else polygons

    /**
     * Adds a polygon part to the list if needed and prepare for next iteration.
     *
     * This function checks if the given polygon has more than two points. If it does, it moves
     * the polygon to the provided list and then clears the polygon, adding back the last point.
     *
     */
    private inline fun <reified T : CutPolygon> addPolygonPartIfNeeded(polygon: T, polygonList: MutableList<T>) {
        if (polygon.isEmpty()) return
        val lastPoint = polygon.last()
        if (polygon.size > 2) {
            polygonList.add(polygon.move())
        }
        polygon.clear().add(lastPoint!!)
    }

    // ------------------------------------------------------------------------

    /**
     * Reconstructs the side polygons from the given list of poly-lines.
     *
     * This function reconstructs poly-lines into polygons.
     * Each polyline should cut the longitude twice and have more than two points.
     *
     */
    @Deprecated("Algorithm not reliable yet")
    private inline fun <reified T : CutPolygon> reconstructSide(propCutId: Int, side: MutableList<T>, initPolygon: T): List<T> =
        side.asSequence()
            .filter { it.size > 2 && it.cutPositions.filter { it2 -> it2.cutId == propCutId }.size == 2 } // Each polyline should cut the lng twice
            .sortedBy { it.cutPositions.filter { it2 -> it2.cutId == propCutId }.minOf { cutPos -> cutPos.lat } } // Grow latitude from min
            .let { connectPolylines(it.toList(), initPolygon) }


    /**
     * Reconstructs polygons from a list of poly-lines.
     *
     * This function takes a list of poly-lines and an initial polygon, and reconstructs them into
     * a list of polygons. It handles self-intersecting polygons on longitude cuts by adding
     * poly-lines to the current polygon if they intersect with the current polygon's latitude range.
     * If they do not intersect, the current polygon is closed moved to the result list,
     * and a new polygon is started.
     *
     */
    @Deprecated("Algorithm not reliable yet")
    private inline fun <reified T : CutPolygon> connectPolylines(polyLines: List<T>, initPolygon: T): List<T> {
        val result = mutableListOf<T>()
        initPolygon.clear()
        var current: T = initPolygon

        for (polyLine in polyLines) {
            val firstNextLat = polyLine.first()!!.lat
            val lastLat by lazy { current.last()!!.lat }
            val firstCurrentLat by lazy { current.first()!!.lat }

            if (current.isEmpty() || firstNextLat in minOf(lastLat, firstCurrentLat)..maxOf(lastLat, firstCurrentLat)) {
                // Here we accept to have self-intersecting polygons on longitude cut
                // ex: (Lat,lng): (-2,0),(-1,2),(0,0),(1,2),(2,0),(-2,0)
                current.addAll(polyLine)
            } else {
                result.add(current.close().move())
                initPolygon.clear().addAll(polyLine)
                current = initPolygon
            }
        }

        if (current.isNotEmpty()) {
            result.add(current.close().move())
        }

        return result
    }

    // ------------------------------------------------------------------------

    /**
     * Determines if a point is inside any of the given polygons.
     */
    fun isPointInPolygons(tap: Position, polygons: Area): Boolean =
        polygons.any { it.containsPosition(tap) }

    /**
     * Calculates the bounding box of a multi-polygon.
     *
     * This function takes a multi-polygon represented as a list of [Position]objects and returns a
     * [BoundingBox] object that encompasses the entire polygon.
     *
     * It throws an [IllegalArgumentException] if the input polygon is empty.
     *
     */
    fun polygonsBbox(polygons: Area): BoundingBox {
        require(polygons.isNotEmpty() && polygons.all { it.isNotEmpty() }) {
            "Event area cannot be empty, cannot determine bounding box"
        }

        val (minLat, minLng, maxLat, maxLng) = polygons.fold(
            Quad(
                Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY
            )
        ) { (minLat, minLng, maxLat, maxLng), polygon ->
            val bbox = polygon.bbox()
            Quad(
                minOf(minLat, bbox.sw.lat), minOf(minLng, bbox.sw.lng),
                maxOf(maxLat, bbox.ne.lat), maxOf(maxLng, bbox.ne.lng)
            )
        }

        return BoundingBox.fromCorners(
            sw = Position(minLat, minLng),
            ne = Position(maxLat, maxLng)
        )
    }

    // ------------------------------------------------------------------------

    /**
     * Converts a list of polygons into a GeoJSON string.
     *
     */
    fun convertPolygonsToGeoJson(polygons: Area): String {
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