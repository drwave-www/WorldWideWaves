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

import com.worldwidewaves.shared.events.utils.PolygonUtils.SplitResult.Companion.empty

// ----------------------------------------------------------------------------

object PolygonUtils {

    data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

    // --------------------------------------------------------------------
    //  New simple split result (plain polygons, no cutId bookkeeping)
    // --------------------------------------------------------------------

    data class SplitResult(val left: Area, val right: Area) {
        companion object {
            fun empty() = SplitResult(emptyList(), emptyList())
        }
    }

    // ------------------------------------------------------------------------

    val List<Position>.toPolygon : Polygon
        get() = Polygon().apply { this@toPolygon.forEach { add(it) } }

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
    fun splitByLongitude(polygon: Polygon, lngToCut: Double): SplitResult =
        splitByLongitude(polygon, ComposedLongitude.fromLongitude(lngToCut))

    fun splitByLongitude(polygon: Polygon, lngToCut: ComposedLongitude): SplitResult {
        val workingPolygon = Polygon()
        workingPolygon.addAll(polygon)
        workingPolygon.close().pop() // Ensure the polygon is closed and remove the last point

        require(workingPolygon.isNotEmpty() && workingPolygon.size >= 3) {
            return empty().also { workingPolygon.close() }
        }

        val minLongitude = workingPolygon.bbox().minLongitude
        val maxLongitude = workingPolygon.bbox().maxLongitude

        val lngBbox = lngToCut.bbox()

        return when {
            // Cut line completely EAST of the polygon – everything ends up on the LEFT side
            lngBbox.minLongitude > maxLongitude -> {
                val closed = workingPolygon.move().close()
                SplitResult(listOf(closed), emptyList())
            }
            // Cut line completely WEST of the polygon – everything ends up on the RIGHT side
            lngBbox.maxLongitude < minLongitude -> {
                val closed = workingPolygon.move().close()
                SplitResult(emptyList(), listOf(closed))
            }
            else -> { // Separate the polygon into two parts based on the cut longitude
                // ------------------------------------------------------------------
                // New simple splitter : single pass, single polygon per side
                // ------------------------------------------------------------------

                // current builders
                val leftPoly  = Polygon()
                val rightPoly = Polygon()
                // final pieces lists
                val leftList  = mutableListOf<Polygon>()
                val rightList = mutableListOf<Polygon>()

                // Helper that closes & pushes a polygon when it currently contains
                // at least three vertices, then starts a new part beginning with
                // the last (intersection) vertex so that continuity is preserved.
                fun addPolygonPartIfNeeded(poly: Polygon, list: MutableList<Polygon>) {
                    val keep = poly.last()
                    if (poly.size >= 3) {
                        list.add(poly.createNew().xferFrom(poly).close())
                    } else {
                        poly.clear()
                    }
                    keep?.let { poly.add(it) }
                }

                fun addPointToSides(point: Position, side: ComposedLongitude.Side) {
                    when {
                        side.isWest() -> leftPoly.add(point)
                        side.isEast() -> rightPoly.add(point)
                        side.isOn()   -> {
                            // Add the raw point to both sides
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
                    val intersection = lngToCut.intersectWithSegment(Segment(start, end))

                    // Emit start point
                    addPointToSides(start, sideStart)

                    // Emit intersection if any
                    intersection?.let {
                        // avoid consecutive duplicates
                        if (leftPoly.last() != it)  leftPoly.add(it)
                        if (rightPoly.last() != it) rightPoly.add(it)
                        
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
                val completedLeft  = completeLongitudePoints(lngToCut, leftList)
                val completedRight = completeLongitudePoints(lngToCut, rightList)

                // Filter out any degenerate polygons that could slip through
                val filteredLeft  = completedLeft.filter  { it.size >= 3 }
                val filteredRight = completedRight.filter { it.size >= 3 }

                return SplitResult(filteredLeft, filteredRight)
                    .also { workingPolygon.close() }
            }
        }
    }

    // ------------------------------------------------------------------------

    /**
     * Adds intermediate points along a composed longitude to polygons.
     *
     * For each polygon, finds ON-line anchor points and inserts intermediate points
     * from the composed longitude between them.
     */
    private fun completeLongitudePoints(
        lngToCut: ComposedLongitude,  // the composed longitude used for the cut
        polygons: List<Polygon>
    ): List<Polygon> = if (lngToCut.size() > 1) {   // nothing to add for a straight vertical line
        polygons.map { polygon ->
            // Anchor points already lying exactly on the composed longitude
            val onLine = polygon
                .filter { lngToCut.isPointOnLine(it) == ComposedLongitude.Side.ON }
                .sortedBy { it.lat }

            if (onLine.size < 2) return@map polygon

            /* Insert intermediate points for every consecutive pair of ON-line anchors
               keeping the original traversal order of the polygon ring. */
            val anchorsInOrder = onLine
            for (idx in 0 until anchorsInOrder.size - 1) {
                var current = anchorsInOrder[idx]
                val next    = anchorsInOrder[idx + 1]

                // Collect composed-longitude points strictly between current & next
                val between = if (current.lat <= next.lat) {
                    lngToCut.positionsBetween(current.lat, next.lat)
                } else {
                    lngToCut.positionsBetween(next.lat, current.lat).asReversed()
                }

                // Insert each intermediate point right after the running anchor
                for (p in between) {
                    // Avoid duplicates with last inserted / anchor
                    if (p != current) {
                        current = polygon.insertAfter(p, current.id)
                    }
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
