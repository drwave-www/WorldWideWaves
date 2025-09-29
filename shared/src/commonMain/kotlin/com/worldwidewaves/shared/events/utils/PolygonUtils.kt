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
     * Note: Currently unused but kept for potential future optimization.
     */
    @Suppress("UnusedPrivateMember")
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

            val ayNegative = ay < 0
            val byNegative = by < 0
            val ayPositive = ay > 0
            val byPositive = by > 0
            val axNegative = ax < 0
            val bxNegative = bx < 0

            val bothYNegative = ayNegative && byNegative
            val bothYPositive = ayPositive && byPositive
            val bothXNegative = axNegative && bxNegative
            if (bothYNegative || bothYPositive || bothXNegative) {
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

        val hasLessThanThreeVertices = size < 3
        if (hasLessThanThreeVertices) {
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
            val isOnVertex = kotlin.math.abs(xi - tap.lng) < epsilon && kotlin.math.abs(yi - tap.lat) < epsilon
            if (isOnVertex) {
                return true
            }

            // Ray-casting algorithm
            val yiAboveTap = yi > tap.lat
            val yjAboveTap = yj > tap.lat
            val straddles = yiAboveTap != yjAboveTap
            val tapLngIsLeft = tap.lng < (xj - xi) * (tap.lat - yi) / (yj - yi) + xi
            if (straddles && tapLngIsLeft) {
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

        val isInvalidPolygon = workingPolygon.isEmpty() || workingPolygon.size < 3
        if (isInvalidPolygon) {
            return empty().also { workingPolygon.close() }
        }

        val minLongitude = workingPolygon.bbox().minLongitude
        val maxLongitude = workingPolygon.bbox().maxLongitude
        val lngBbox = lngToCut.bbox()

        val cutCompletelyEast = lngBbox.minLongitude > maxLongitude
        if (cutCompletelyEast) {
            val closed = workingPolygon.move().close()
            return SplitResult(listOf(closed), emptyList())
        }

        val cutCompletelyWest = lngBbox.maxLongitude < minLongitude
        if (cutCompletelyWest) {
            val closed = workingPolygon.move().close()
            return SplitResult(emptyList(), listOf(closed))
        }

        val isSingleVerticalLine = lngToCut.size() == 1
        if (isSingleVerticalLine) {
            return splitByVerticalLine(workingPolygon, lngToCut.getPositions().first().lng)
        }

        return splitByComposedLongitude(workingPolygon, lngToCut, lngBbox)
    }

    private fun splitByVerticalLine(
        workingPolygon: Polygon,
        cutLng: Double,
    ): SplitResult {
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

    private fun splitByComposedLongitude(
        workingPolygon: Polygon,
        lngToCut: ComposedLongitude,
        lngBbox: BoundingBox,
    ): SplitResult {
        val leftPoly = Polygon()
        val rightPoly = Polygon()
        val leftList = mutableListOf<Polygon>()
        val rightList = mutableListOf<Polygon>()

        var hasIntersection = false

        val points = workingPolygon.toList()
        for (i in points.indices) {
            val start = points[i]
            val end = points[(i + 1) % points.size]

            val sideStart = lngToCut.isPointOnLine(start)
            val sideEnd = lngToCut.isPointOnLine(end)
            val intersection = lngToCut.intersectWithSegment(Segment(start, end))

            addPointToSides(start, sideStart, leftPoly, rightPoly)

            intersection?.let {
                hasIntersection = true
                addIntersectionPoint(it, leftPoly, rightPoly)

                val crossingWestToEast = sideStart.isWest() && sideEnd.isEast()
                val crossingEastToWest = sideStart.isEast() && sideEnd.isWest()
                if (crossingWestToEast) {
                    addPolygonPartIfNeeded(leftPoly, leftList)
                } else if (crossingEastToWest) {
                    addPolygonPartIfNeeded(rightPoly, rightList)
                }
            }

            val isLastSegment = i == points.size - 1
            if (isLastSegment) {
                addPointToSides(end, sideEnd, leftPoly, rightPoly)
            }
        }

        addPolygonPartIfNeeded(leftPoly, leftList)
        addPolygonPartIfNeeded(rightPoly, rightList)

        if (!hasIntersection) {
            return handleNoIntersectionCase(workingPolygon, lngToCut, lngBbox)
        }

        return finalizePolygonSplit(lngToCut, leftList, rightList, workingPolygon)
    }

    private fun addPointToSides(
        point: Position,
        side: ComposedLongitude.Side,
        leftPoly: Polygon,
        rightPoly: Polygon,
    ) {
        when {
            side.isWest() -> leftPoly.add(point)
            side.isEast() -> rightPoly.add(point)
            side.isOn() -> {
                leftPoly.add(point)
                rightPoly.add(point)
            }
        }
    }

    private fun addIntersectionPoint(
        intersection: Position,
        leftPoly: Polygon,
        rightPoly: Polygon,
    ) {
        if (leftPoly.last() != intersection) leftPoly.add(intersection)
        if (rightPoly.last() != intersection) rightPoly.add(intersection)
    }

    private fun addPolygonPartIfNeeded(
        poly: Polygon,
        list: MutableList<Polygon>,
    ) {
        val keep = poly.last()
        val hasEnoughVertices = poly.size >= 3
        if (hasEnoughVertices) {
            list.add(poly.createNew().xferFrom(poly).close())
        } else {
            poly.clear()
        }
        keep?.let { poly.add(it) }
    }

    private fun handleNoIntersectionCase(
        workingPolygon: Polygon,
        lngToCut: ComposedLongitude,
        lngBbox: BoundingBox,
    ): SplitResult {
        val polyBbox = workingPolygon.bbox()
        val centerLat = (polyBbox.sw.lat + polyBbox.ne.lat) / 2
        val cutLngAtCenter = lngToCut.lngAt(centerLat)

        if (cutLngAtCenter != null) {
            return determinePolygonSideWithCutLng(workingPolygon, polyBbox, cutLngAtCenter)
        }

        return determinePolygonSideWithBbox(workingPolygon, polyBbox, lngBbox)
    }

    private fun determinePolygonSideWithCutLng(
        workingPolygon: Polygon,
        polyBbox: BoundingBox,
        cutLngAtCenter: Double,
    ): SplitResult {
        val closedPoly = workingPolygon.move().close()

        val entirelyEast = polyBbox.minLongitude >= cutLngAtCenter
        if (entirelyEast) {
            return SplitResult(emptyList(), listOf(closedPoly))
        }

        val entirelyWest = polyBbox.maxLongitude <= cutLngAtCenter
        if (entirelyWest) {
            return SplitResult(listOf(closedPoly), emptyList())
        }

        val centerLng = (polyBbox.sw.lng + polyBbox.ne.lng) / 2
        val centerIsEast = centerLng >= cutLngAtCenter
        return if (centerIsEast) {
            SplitResult(emptyList(), listOf(closedPoly))
        } else {
            SplitResult(listOf(closedPoly), emptyList())
        }
    }

    private fun determinePolygonSideWithBbox(
        workingPolygon: Polygon,
        polyBbox: BoundingBox,
        lngBbox: BoundingBox,
    ): SplitResult {
        val closedPoly = workingPolygon.move().close()

        val entirelyEastOfCutBbox = polyBbox.minLongitude >= lngBbox.maxLongitude
        if (entirelyEastOfCutBbox) {
            return SplitResult(emptyList(), listOf(closedPoly))
        }

        val entirelyWestOfCutBbox = polyBbox.maxLongitude <= lngBbox.minLongitude
        if (entirelyWestOfCutBbox) {
            return SplitResult(listOf(closedPoly), emptyList())
        }

        val centerLng = (polyBbox.sw.lng + polyBbox.ne.lng) / 2
        val cutCenterLng = (lngBbox.sw.lng + lngBbox.ne.lng) / 2
        val centerIsEast = centerLng >= cutCenterLng
        return if (centerIsEast) {
            SplitResult(emptyList(), listOf(closedPoly))
        } else {
            SplitResult(listOf(closedPoly), emptyList())
        }
    }

    private fun finalizePolygonSplit(
        lngToCut: ComposedLongitude,
        leftList: MutableList<Polygon>,
        rightList: MutableList<Polygon>,
        workingPolygon: Polygon,
    ): SplitResult {
        val completedLeft = completeLongitudePoints(lngToCut, leftList, workingPolygon, true)
        val completedRight = completeLongitudePoints(lngToCut, rightList, workingPolygon, false)

        val filteredLeft = completedLeft.filter { it.size >= 3 }
        val filteredRight = completedRight.filter { it.size >= 3 }

        val normLeft = filteredLeft.map { removeConsecutiveDuplicates(it) }
        val normRight = filteredRight.map { removeConsecutiveDuplicates(it) }

        return SplitResult(normLeft, normRight).also { workingPolygon.close() }
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
     * lies inside the original polygon.  This prevents bridging concave "mouths" where
     * the composed longitude would otherwise connect regions outside the polygon.
     */
    private fun completeLongitudePoints(
        lngToCut: ComposedLongitude,
        polygons: List<Polygon>,
        source: Polygon,
        @Suppress("UNUSED_PARAMETER") forLeft: Boolean,
    ): List<Polygon> {
        val needsIntermediatePoints = lngToCut.size() > 1
        if (!needsIntermediatePoints) {
            return polygons
        }

        return polygons.map { polygon ->
            processPolygonAnchors(polygon, lngToCut, source)
        }
    }

    private fun processPolygonAnchors(
        polygon: Polygon,
        lngToCut: ComposedLongitude,
        source: Polygon,
    ): Polygon {
        val onLine = polygon.filter { lngToCut.isPointOnLine(it) == ComposedLongitude.Side.ON }

        val hasLessThanTwoAnchors = onLine.size < 2
        if (hasLessThanTwoAnchors) {
            return polygon
        }

        val uniqueAnchors = onLine.distinctBy { "${it.lat},${it.lng}" }.sortedBy { it.lat }

        for (idx in 0 until uniqueAnchors.size - 1) {
            val anchor1 = uniqueAnchors[idx]
            val anchor2 = uniqueAnchors[idx + 1]
            processAnchorPair(anchor1, anchor2, polygon, lngToCut, source)
        }

        return polygon
    }

    private fun processAnchorPair(
        anchor1: Position,
        anchor2: Position,
        polygon: Polygon,
        lngToCut: ComposedLongitude,
        source: Polygon,
    ) {
        val midLat = (anchor1.lat + anchor2.lat) / 2
        val midLng = lngToCut.lngAt(midLat) ?: return

        val insideMid = source.containsPosition(Position(midLat, midLng))
        if (!insideMid) return

        val between = lngToCut.positionsBetween(anchor1.lat, anchor2.lat)

        val anchor1Pos = polygon.indexOfFirst { it.lat == anchor1.lat && it.lng == anchor1.lng }
        val anchor2Pos = polygon.indexOfFirst { it.lat == anchor2.lat && it.lng == anchor2.lng }

        val anchorsNotFound = anchor1Pos == -1 || anchor2Pos == -1
        if (anchorsNotFound) return

        insertIntermediatePoints(polygon, anchor1Pos, anchor2Pos, between)
    }

    private fun insertIntermediatePoints(
        polygon: Polygon,
        anchor1Pos: Int,
        anchor2Pos: Int,
        between: List<Position>,
    ) {
        val isConsecutive = (anchor2Pos == anchor1Pos + 1) || (anchor1Pos == polygon.size - 1 && anchor2Pos == 0)

        var current = polygon.elementAt(anchor1Pos)
        for (p in between) {
            val alreadyExists = polygon.any { it.lat == p.lat && it.lng == p.lng }
            if (!alreadyExists) {
                current = polygon.insertAfter(p, current.id)
            }
        }
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

        val initialQuad =
            Quad(
                Double.POSITIVE_INFINITY,
                Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY,
                Double.NEGATIVE_INFINITY,
            )

        val result =
            polygons.fold(initialQuad) { quad, polygon ->
                val bbox = polygon.bbox()
                Quad(
                    minOf(quad.first, bbox.sw.lat),
                    minOf(quad.second, bbox.sw.lng),
                    maxOf(quad.third, bbox.ne.lat),
                    maxOf(quad.fourth, bbox.ne.lng),
                )
            }

        val minLat = result.first
        val minLng = result.second
        val maxLat = result.third
        val maxLng = result.fourth

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
