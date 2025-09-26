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

import com.worldwidewaves.shared.events.utils.PolygonUtils.SplitResult.Companion.empty
import kotlin.math.max
import kotlin.math.min

// ----------------------------------------------------------------------------

object PolygonUtils {
    private const val FLOATING_POINT_EPSILON = 1e-12

    data class Quad<A, B, C, D>(
        val first: A,
        val second: B,
        val third: C,
        val fourth: D,
    )

    // ------------------------------------------------------------------------
    // Spatial indexing for large polygon optimization
    // ------------------------------------------------------------------------

    /**
     * Spatial index for polygon edges to accelerate point-in-polygon tests.
     * Uses a simple grid-based spatial index for large polygons.
     */
    private data class SpatialIndex(
        val bbox: BoundingBox,
        val gridSize: Int = 16, // 16x16 grid for reasonable granularity
        val edgesByCell: Array<MutableList<Pair<Position, Position>>>,
    ) {
        companion object {
            fun build(polygon: Polygon): SpatialIndex? {
                if (polygon.size < 100) return null // Only optimize large polygons

                val bbox = polygon.bbox()
                val gridSize = min(16, max(4, polygon.size / 20)) // Adaptive grid size
                val edgesByCell = Array(gridSize * gridSize) { mutableListOf<Pair<Position, Position>>() }

                val latStep = (bbox.ne.lat - bbox.sw.lat) / gridSize
                val lngStep = (bbox.ne.lng - bbox.sw.lng) / gridSize

                // Add edges to appropriate grid cells
                val points = polygon.toList()
                for (i in 0 until points.size - 1) {
                    val start = points[i]
                    val next = points[i + 1]

                    // Find all grid cells this edge crosses
                    val minLat = min(start.lat, next.lat)
                    val maxLat = max(start.lat, next.lat)
                    val minLng = min(start.lng, next.lng)
                    val maxLng = max(start.lng, next.lng)

                    val startRow = ((minLat - bbox.sw.lat) / latStep).toInt().coerceIn(0, gridSize - 1)
                    val endRow = ((maxLat - bbox.sw.lat) / latStep).toInt().coerceIn(0, gridSize - 1)
                    val startCol = ((minLng - bbox.sw.lng) / lngStep).toInt().coerceIn(0, gridSize - 1)
                    val endCol = ((maxLng - bbox.sw.lng) / lngStep).toInt().coerceIn(0, gridSize - 1)

                    // Add edge to all cells it might intersect
                    for (row in startRow..endRow) {
                        for (col in startCol..endCol) {
                            val cellIndex = row * gridSize + col
                            edgesByCell[cellIndex].add(Pair(start, next))
                        }
                    }
                }

                return SpatialIndex(bbox, gridSize, edgesByCell)
            }
        }

        fun getCellEdges(position: Position): List<Pair<Position, Position>> {
            val latStep = (bbox.ne.lat - bbox.sw.lat) / gridSize
            val lngStep = (bbox.ne.lng - bbox.sw.lng) / gridSize

            val row = ((position.lat - bbox.sw.lat) / latStep).toInt().coerceIn(0, gridSize - 1)
            val col = ((position.lng - bbox.sw.lng) / lngStep).toInt().coerceIn(0, gridSize - 1)

            val cellIndex = row * gridSize + col
            return edgesByCell[cellIndex]
        }
    }

    // Cache for spatial indices
    private val spatialIndexCache = mutableMapOf<Int, SpatialIndex?>()

    /**
     * Point-in-polygon test using the standard ray-casting algorithm.
     * This implementation prioritizes correctness over performance optimization.
     */
    fun Polygon.containsPositionOptimized(tap: Position): Boolean {
        require(isNotEmpty()) { return false }
        return containsPosition(tap)
    }

    /**
     * Ray-casting algorithm optimized with spatial indexing.
     * Only tests edges in the same grid cell as the query point.
     */
    private fun Polygon.containsPositionWithSpatialIndex(
        tap: Position,
        spatialIndex: SpatialIndex,
    ): Boolean {
        val relevantEdges = spatialIndex.getCellEdges(tap)
        var depth = 0

        // Process only edges in the relevant grid cell(s) using the same algorithm as the standard version
        for ((currentPoint, nextPoint) in relevantEdges) {
            val (bx, by) = currentPoint.lat - tap.lat to currentPoint.lng - tap.lng
            val (ax, ay) = nextPoint.lat - tap.lat to nextPoint.lng - tap.lng

            if ((ay < 0 && by < 0) || (ay > 0 && by > 0) || (ax < 0 && bx < 0)) {
                continue
            }

            val lx = ax - ay * (bx - ax) / (by - ay)
            if (lx == 0.0) return true
            if (lx > 0) depth++
        }

        return (depth and 1) == 1
    }

    // --------------------------------------------------------------------
    //  New simple split result (plain polygons, no cutId bookkeeping)
    // --------------------------------------------------------------------

    data class SplitResult(
        val left: Area,
        val right: Area,
    ) {
        companion object {
            fun empty() = SplitResult(emptyList(), emptyList())
        }
    }

    // ------------------------------------------------------------------------

    val List<Position>.toPolygon: Polygon
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
     * Enhanced with improved numerical stability and edge case handling.
     */
    fun Polygon.containsPosition(tap: Position): Boolean {
        require(isNotEmpty()) { return false }

        // Handle simple polygons with less than 3 vertices
        if (size < 3) {
            return false
        }

        val points = this.toList()
        var inside = false
        var j = points.size - 1
        val epsilon = 1e-12

        // Use the robust ray-casting algorithm
        for (i in points.indices) {
            val xi = points[i].lng
            val yi = points[i].lat
            val xj = points[j].lng
            val yj = points[j].lat

            // Check if point is exactly on a vertex
            if (kotlin.math.abs(xi - tap.lng) < epsilon && kotlin.math.abs(yi - tap.lat) < epsilon) {
                return true
            }

            // Ray-casting algorithm
            if (((yi > tap.lat) != (yj > tap.lat)) &&
                (tap.lng < (xj - xi) * (tap.lat - yi) / (yj - yi) + xi)
            ) {
                inside = !inside
            }

            j = i
        }

        return inside
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
    fun splitByLongitude(
        polygon: Polygon,
        lngToCut: Double,
    ): SplitResult = splitByLongitude(polygon, ComposedLongitude.fromLongitude(lngToCut))

    fun splitByLongitude(
        polygon: Polygon,
        lngToCut: ComposedLongitude,
    ): SplitResult {
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
            // --------------------------------------------------------------
            // Simple vertical line (single position) – deterministic split
            // --------------------------------------------------------------
            lngToCut.size() == 1 -> {
                val cutLng = lngToCut.getPositions().first().lng

                // Run Sutherland–Hodgman half-plane clip once for each side
                val leftPts = clipToHalfPlane(workingPolygon.toList(), cutLng, keepLeft = true)
                val rightPts = clipToHalfPlane(workingPolygon.toList(), cutLng, keepLeft = false)

                val leftPolys =
                    if (leftPts.size >= 3) {
                        listOf(leftPts.toPolygon.close())
                    } else {
                        emptyList()
                    }
                val rightPolys =
                    if (rightPts.size >= 3) {
                        listOf(rightPts.toPolygon.close())
                    } else {
                        emptyList()
                    }

                return SplitResult(leftPolys, rightPolys)
            }
            else -> { // Separate the polygon into two parts based on the cut longitude
                // ------------------------------------------------------------------
                // New simple splitter : single pass, single polygon per side
                // ------------------------------------------------------------------

                // current builders
                val leftPoly = Polygon()
                val rightPoly = Polygon()
                // final pieces lists
                val leftList = mutableListOf<Polygon>()
                val rightList = mutableListOf<Polygon>()

                // Helper that closes & pushes a polygon when it currently contains
                // at least three vertices, then starts a new part beginning with
                // the last (intersection) vertex so that continuity is preserved.
                fun addPolygonPartIfNeeded(
                    poly: Polygon,
                    list: MutableList<Polygon>,
                ) {
                    val keep = poly.last()
                    if (poly.size >= 3) {
                        list.add(poly.createNew().xferFrom(poly).close())
                    } else {
                        poly.clear()
                    }
                    keep?.let { poly.add(it) }
                }

                fun addPointToSides(
                    point: Position,
                    side: ComposedLongitude.Side,
                ) {
                    when {
                        side.isWest() -> leftPoly.add(point)
                        side.isEast() -> rightPoly.add(point)
                        side.isOn() -> {
                            // Add the raw point to both sides
                            leftPoly.add(point)
                            rightPoly.add(point)
                        }
                    }
                }

                // Track if any intersections are found
                var hasIntersection = false

                val points = workingPolygon.toList()
                for (i in points.indices) {
                    val start = points[i]
                    val end = points[(i + 1) % points.size] // close ring

                    val sideStart = lngToCut.isPointOnLine(start)
                    val sideEnd = lngToCut.isPointOnLine(end)
                    val intersection = lngToCut.intersectWithSegment(Segment(start, end))

                    // Emit start point
                    addPointToSides(start, sideStart)

                    // Emit intersection if any
                    intersection?.let {
                        // Mark that we found an intersection
                        hasIntersection = true

                        // avoid consecutive duplicates
                        if (leftPoly.last() != it) leftPoly.add(it)
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

                // If no intersections were found but the bounding boxes overlap,
                // determine which side the polygon lies on
                if (!hasIntersection) {
                    val polyBbox = workingPolygon.bbox()
                    val centerLat = (polyBbox.sw.lat + polyBbox.ne.lat) / 2

                    // Try to get longitude at center latitude
                    val cutLngAtCenter = lngToCut.lngAt(centerLat)

                    if (cutLngAtCenter != null) {
                        // We have a longitude at this latitude, determine side
                        val closedPoly = workingPolygon.move().close()

                        if (polyBbox.minLongitude >= cutLngAtCenter) {
                            // Polygon is entirely to the east of the cut
                            return SplitResult(emptyList(), listOf(closedPoly))
                        } else if (polyBbox.maxLongitude <= cutLngAtCenter) {
                            // Polygon is entirely to the west of the cut
                            return SplitResult(listOf(closedPoly), emptyList())
                        } else {
                            // Cut longitude runs inside polygon horizontally but no intersection
                            // Use polygon center longitude to determine side
                            val centerLng = (polyBbox.sw.lng + polyBbox.ne.lng) / 2
                            if (centerLng >= cutLngAtCenter) {
                                return SplitResult(emptyList(), listOf(closedPoly))
                            } else {
                                return SplitResult(listOf(closedPoly), emptyList())
                            }
                        }
                    } else {
                        // The composed longitude doesn't cover this latitude range
                        // Fall back to comparing horizontal relation between bboxes
                        val closedPoly = workingPolygon.move().close()

                        if (polyBbox.minLongitude >= lngBbox.maxLongitude) {
                            // Polygon is entirely to the east of the cut's bbox
                            return SplitResult(emptyList(), listOf(closedPoly))
                        } else if (polyBbox.maxLongitude <= lngBbox.minLongitude) {
                            // Polygon is entirely to the west of the cut's bbox
                            return SplitResult(listOf(closedPoly), emptyList())
                        } else {
                            // Use polygon center vs cut bbox center
                            val centerLng = (polyBbox.sw.lng + polyBbox.ne.lng) / 2
                            val cutCenterLng = (lngBbox.sw.lng + lngBbox.ne.lng) / 2
                            if (centerLng >= cutCenterLng) {
                                return SplitResult(emptyList(), listOf(closedPoly))
                            } else {
                                return SplitResult(listOf(closedPoly), emptyList())
                            }
                        }
                    }
                }

                // Inject intermediate longitude points if needed
                val completedLeft = completeLongitudePoints(lngToCut, leftList, workingPolygon, true)
                val completedRight = completeLongitudePoints(lngToCut, rightList, workingPolygon, false)

                // Filter out any degenerate polygons that could slip through
                val filteredLeft = completedLeft.filter { it.size >= 3 }
                val filteredRight = completedRight.filter { it.size >= 3 }

                // Normalize polygons by removing consecutive duplicate vertices
                val normLeft = filteredLeft.map { removeConsecutiveDuplicates(it) }
                val normRight = filteredRight.map { removeConsecutiveDuplicates(it) }

                return SplitResult(normLeft, normRight)
                    .also { workingPolygon.close() }
            }
        }
    }

    // ------------------------------------------------------------------------

    /**
     * Clips a polygon ring (list of [Position]) against the vertical half-plane
     * defined by x&nbsp;≤&nbsp;[cutLng] when [keepLeft] is true, or x&nbsp;≥&nbsp;[cutLng]
     * when false.  Returns the resulting list of vertices (not closed).
     * Implements a Sutherland–Hodgman style algorithm specialised for a vertical line.
     */
    private fun clipToHalfPlane(
        points: List<Position>,
        cutLng: Double,
        keepLeft: Boolean,
    ): List<Position> {
        if (points.size < 3) return emptyList()

        val output = mutableListOf<Position>()

        fun isInside(p: Position): Boolean =
            if (keepLeft) {
                p.lng <= cutLng + FLOATING_POINT_EPSILON
            } else {
                p.lng >=
                    cutLng - FLOATING_POINT_EPSILON
            }

        var prev = points.last()
        var prevInside = isInside(prev)

        for (curr in points) {
            val currInside = isInside(curr)

            if (prevInside && currInside) {
                // Case 1: both inside – just add current
                output.add(curr)
            } else if (prevInside && !currInside) {
                // Case 2: leaving – add intersection
                Segment(prev, curr).intersectWithLng(cutLng)?.let { output.add(it) }
            } else if (!prevInside && currInside) {
                // Case 3: entering – add intersection then current
                Segment(prev, curr).intersectWithLng(cutLng)?.let { output.add(it) }
                output.add(curr)
            } // Case 4: both outside – add nothing

            prev = curr
            prevInside = currInside
        }

        return output
    }

    /**
     * Returns a copy of [polygon] with consecutive duplicate vertices removed while
     * preserving order and closure (first == last).  The returned instance shares
     * the same concrete type via [Polygon.createNew].
     */
    private fun removeConsecutiveDuplicates(polygon: Polygon): Polygon {
        if (polygon.size < 2) return polygon

        val cleaned = polygon.createNew()
        var lastAdded: Position? = null
        polygon.forEach { pt ->
            if (lastAdded == null || pt != lastAdded) {
                cleaned.add(pt)
                lastAdded = pt
            }
        }
        // Ensure closure with a single duplicate of the first vertex
        if (cleaned.isNotEmpty() && cleaned.first() != cleaned.last()) {
            cleaned.add(cleaned.first()!!.detached())
        }
        return cleaned
    }

    /**
     * Adds intermediate points along a composed longitude to polygons.
     *
     * For each polygon, finds ON-line anchor points and inserts intermediate points
     * from the composed longitude between them, but only if the midpoint between anchors
     * lies inside the original polygon.  This prevents bridging concave “mouths” where
     * the composed longitude would otherwise connect regions outside the polygon.
     */
    private fun completeLongitudePoints(
        lngToCut: ComposedLongitude, // the composed longitude used for the cut
        polygons: List<Polygon>, // the polygons to process
        source: Polygon, // the original polygon before splitting
        forLeft: Boolean, // true if processing left side, false for right side
    ): List<Polygon> =
        if (lngToCut.size() > 1) { // nothing to add for a straight vertical line
            polygons.map { polygon ->
                // Anchor points already lying exactly on the composed longitude
                val onLine =
                    polygon
                        .filter { lngToCut.isPointOnLine(it) == ComposedLongitude.Side.ON }

                if (onLine.size < 2) return@map polygon

                // Remove duplicate anchors and sort by latitude to get proper order along composed longitude
                val uniqueAnchors = onLine.distinctBy { "${it.lat},${it.lng}" }.sortedBy { it.lat }

                // Helper function to process anchor pair and insert intermediate points
                fun processAnchorPair(
                    anchor1: Position,
                    anchor2: Position,
                ) {
                    // Calculate midpoint latitude
                    val midLat = (anchor1.lat + anchor2.lat) / 2
                    val midLng = lngToCut.lngAt(midLat) ?: return

                    // Include intermediate points only when the composed-longitude
                    // segment between anchors lies inside the source polygon.
                    val insideMid = source.containsPosition(Position(midLat, midLng))
                    if (!insideMid) return

                    // Get intermediate points between anchors
                    val between = lngToCut.positionsBetween(anchor1.lat, anchor2.lat)

                    // Find the correct position in the polygon to insert intermediate points
                    // We need to find where anchor1 and anchor2 appear consecutively in the polygon
                    val anchor1Pos = polygon.indexOfFirst { it.lat == anchor1.lat && it.lng == anchor1.lng }
                    val anchor2Pos = polygon.indexOfFirst { it.lat == anchor2.lat && it.lng == anchor2.lng }

                    if (anchor1Pos != -1 && anchor2Pos != -1) {
                        // Check if they are consecutive (considering wrap-around)
                        val isConsecutive =
                            (anchor2Pos == anchor1Pos + 1) ||
                                (anchor1Pos == polygon.size - 1 && anchor2Pos == 0)

                        if (isConsecutive) {
                            // Insert intermediate points after anchor1
                            var current = polygon.elementAt(anchor1Pos)
                            for (p in between) {
                                if (!polygon.any { it.lat == p.lat && it.lng == p.lng }) {
                                    current = polygon.insertAfter(p, current.id)
                                }
                            }
                        } else {
                            // For non-consecutive anchors, we need a more sophisticated approach
                            // to determine the correct insertion point based on the polygon's traversal order
                            // For now, use a simple heuristic: insert after the first anchor found
                            var current = polygon.elementAt(anchor1Pos)
                            for (p in between) {
                                if (!polygon.any { it.lat == p.lat && it.lng == p.lng }) {
                                    current = polygon.insertAfter(p, current.id)
                                }
                            }
                        }
                    }
                }

                // Process consecutive anchors in latitude order (along composed longitude)
                for (idx in 0 until uniqueAnchors.size - 1) {
                    val anchor1 = uniqueAnchors[idx]
                    val anchor2 = uniqueAnchors[idx + 1]
                    processAnchorPair(anchor1, anchor2)
                }
                polygon
            }
        } else {
            polygons
        }

    // ------------------------------------------------------------------------

    /**
     * Determines if a point is inside any of the given polygons.
     * Uses optimized containment checks for large polygons.
     */
    fun isPointInPolygons(
        tap: Position,
        polygons: Area,
    ): Boolean {
        polygons.forEachIndexed { index, polygon ->
            val result = polygon.containsPositionOptimized(tap)
            if (result) {
                return true
            }
        }
        return false
    }

    /**
     * Clears the spatial index cache to free memory.
     * Should be called periodically or when polygon data changes.
     */
    fun clearSpatialIndexCache() {
        spatialIndexCache.clear()
    }

    /**
     * Calculates the bounding box of a multi-polygon.
     */
    fun polygonsBbox(polygons: Area): BoundingBox {
        require(polygons.isNotEmpty() && polygons.all { it.isNotEmpty() }) {
            "Event area cannot be empty, cannot determine bounding box"
        }

        val (minLat, minLng, maxLat, maxLng) =
            polygons.fold(
                Quad(
                    Double.POSITIVE_INFINITY,
                    Double.POSITIVE_INFINITY,
                    Double.NEGATIVE_INFINITY,
                    Double.NEGATIVE_INFINITY,
                ),
            ) { (minLat, minLng, maxLat, maxLng), polygon ->
                val bbox = polygon.bbox()
                Quad(
                    minOf(minLat, bbox.sw.lat),
                    minOf(minLng, bbox.sw.lng),
                    maxOf(maxLat, bbox.ne.lat),
                    maxOf(maxLng, bbox.ne.lng),
                )
            }

        return BoundingBox.fromCorners(
            sw = Position(minLat, minLng),
            ne = Position(maxLat, maxLng),
        )
    }

    // ------------------------------------------------------------------------

    fun convertPolygonsToGeoJson(polygons: Area): String {
        val features =
            polygons.map { polygon ->
                val coordinates = polygon.map { listOf(it.lng, it.lat) }
                """
                {
                    "type": "Feature",
                    "geometry": {
                        "type": "Polygon",
                        "coordinates": [$coordinates]
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
