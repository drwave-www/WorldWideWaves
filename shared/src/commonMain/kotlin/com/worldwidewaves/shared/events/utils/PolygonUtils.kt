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
                
                // Mutable lists to collect multiple pieces per side
                val leftList  = mutableListOf<LeftCutPolygon>()
                val rightList = mutableListOf<RightCutPolygon>()
                
                // Helper that closes & pushes a polygon when it currently contains
                // at least three vertices, then starts a new part beginning with
                // the last (intersection) vertex so that continuity is preserved.
                fun <T: CutPolygon> addPolygonPartIfNeeded(poly: T, list: MutableList<T>) {
                    val last = poly.last()
                    if (poly.size >= 3) list.add(poly.close())
                    poly.clear()
                    last?.let { poly.add(it) }
                }

                fun addPointToSides(point: Position, side: ComposedLongitude.Side) {
                    when {
                        side.isWest() -> leftPoly.add(point)
                        side.isEast() -> rightPoly.add(point)
                        side.isOn()   -> {
                            // Add the raw point to both sides (no CutPosition)
                            leftPoly.add(point)
                            rightPoly.add(point)
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

                    // Emit intersection if any, converting to plain Position
                    intersection?.let {
                        val intersectionPoint = Position(it.lat, it.lng)
                        leftPoly.add(intersectionPoint)
                        rightPoly.add(intersectionPoint)
                        
                        // flush polygons when crossing occurs
                        if (sideStart.isWest() && sideEnd.isEast()) {
                            addPolygonPartIfNeeded(leftPoly, leftList)
                        } else if (sideStart.isEast() && sideEnd.isWest()) {
                            addPolygonPartIfNeeded(rightPoly, rightList)
                        }
                    }

                    // If this is the last segment, also emit end (otherwise next loop will do)
                    if (i == points.size - 1) {
                        addPointToSides(end, sideEnd)
                    }
                }

                // flush remaining current builders
                addPolygonPartIfNeeded(leftPoly, leftList)
                addPolygonPartIfNeeded(rightPoly, rightList)

                // Inject intermediate longitude points if needed
                val completedLeft  = completeLongitudePoints(cutId, lngToCut, leftList)
                val completedRight = completeLongitudePoints(cutId, lngToCut, rightList)

                return PolygonSplitResult(cutId, completedLeft, completedRight)
                    .also { workingPolygon.close() }
            }
        }
    }

    // ------------------------------------------------------------------------

    /**
     * Converts a list of polygons into a GeoJSON string.
     *
     */
    private inline fun <reified T : CutPolygon> completeLongitudePoints(
        propCutId: Int,               // kept for signature stability
        lngToCut: ComposedLongitude,  // the composed longitude used for the cut
        polygons: List<T>
    ): List<T> = if (lngToCut.size() > 1) {   // nothing to add for a straight vertical line
        polygons.map { polygon ->
            // Anchor points already lying exactly on the composed longitude
            val onLine = polygon
                .filter { lngToCut.isPointOnLine(it) == ComposedLongitude.Side.ON }
                .sortedBy { it.lat }

            if (onLine.size < 2) return@map polygon

            val minCut = onLine.first()
            val maxCut = onLine.last()

            // Positions of the composed longitude between those two anchors
            val intermediate = lngToCut.positionsBetween(minCut.lat, maxCut.lat)
            if (intermediate.isNotEmpty()) {
                var current: Position = if (polygon is LeftCutPolygon) minCut else maxCut
                for (p in intermediate) {
                    current = polygon.insertAfter(p, current.id)
                }
            }
            polygon
        }
    } else polygons

    // ------------------------------------------------------------------------

    /**
     * Determines if a point is inside any of the given polygons.
     */
    fun isPointInPolygons(tap: Position, polygons: Area): Boolean =
        polygons.any { it.containsPosition(tap) }

    /**
     * Calculates the bounding box of a multi-polygon.
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