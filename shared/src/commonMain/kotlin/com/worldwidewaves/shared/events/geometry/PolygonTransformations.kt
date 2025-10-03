package com.worldwidewaves.shared.events.geometry

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

import com.worldwidewaves.shared.events.geometry.PolygonOperations.containsPosition
import com.worldwidewaves.shared.events.utils.Area
import com.worldwidewaves.shared.events.utils.BoundingBox
import com.worldwidewaves.shared.events.utils.ComposedLongitude
import com.worldwidewaves.shared.events.utils.Polygon
import com.worldwidewaves.shared.events.utils.Position
import com.worldwidewaves.shared.events.utils.Segment
import com.worldwidewaves.shared.events.utils.close
import com.worldwidewaves.shared.events.utils.move
import com.worldwidewaves.shared.events.utils.xferFrom

/**
 * Polygon Transformations Module
 *
 * This file provides advanced polygon manipulation operations:
 * - Polygon splitting by longitude (Sutherland-Hodgman clipping)
 * - Half-plane clipping for vertical lines
 * - Topology correction for complex composed longitudes
 * - Polygon cleanup and normalization
 *
 * These operations are critical for handling geographic waves that cross
 * time zones, meridians, and complex boundaries. The algorithms preserve
 * topological correctness while handling edge cases like concave regions,
 * self-intersections, and degenerate geometry.
 *
 * Key algorithms:
 * - Sutherland-Hodgman: O(n) polygon clipping
 * - Composed longitude splitting: O(n × k) where k = longitude segments
 * - Topology completion: O(n²) worst case for complex polygons
 *
 * @see PolygonOperations for containment and bounding box operations
 * @see PolygonExtensions for utility methods
 */

object PolygonTransformations {
    private const val FLOATING_POINT_EPSILON = 1e-12

    // --------------------------------------------------------------------
    //  Split result data class
    // --------------------------------------------------------------------

    /**
     * Result of splitting a polygon by a longitude line.
     *
     * Contains the polygon fragments on the left (west) and right (east)
     * sides of the cut longitude.
     *
     * @property left Polygons west of the cut longitude
     * @property right Polygons east of the cut longitude
     */
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
     * Splits a polygon by a given longitude.
     *
     * This function divides a polygon into two parts along a vertical meridian
     * or a composed longitude (piecewise vertical line). The algorithm handles:
     * - Simple vertical line cuts
     * - Composed (multi-segment) longitude cuts
     * - Edge cases where cut is completely outside polygon bounds
     * - Intersection calculation and topology preservation
     *
     * **Algorithm**: Adaptive splitting based on longitude type
     * - Single longitude: Sutherland-Hodgman clipping
     * - Composed longitude: Segment-by-segment intersection tracking
     * - No intersection: Whole-polygon assignment based on position
     *
     * **Time Complexity**:
     * - Simple cut: O(n) where n = vertices
     * - Composed cut: O(n × k) where k = longitude segments
     *
     * **Visual Representation**:
     * ```
     *    Cut longitude
     *         │
     *    ╭────┼────╮         ╭────╮    │    ╭────╮
     *    │    │    │   →     │    │    │    │    │
     *    ╰────┼────╯         ╰────╯    │    ╰────╯
     *         │               Left          Right
     * ```
     *
     * **Edge Cases**:
     * - Empty polygon: returns empty result
     * - < 3 vertices: returns empty result
     * - Cut completely east: entire polygon goes left
     * - Cut completely west: entire polygon goes right
     * - Polygon touches but doesn't cross cut: assigned to one side
     *
     * @param polygon The polygon to split
     * @param lngToCut The longitude value to split at
     * @return SplitResult with left (west) and right (east) polygon fragments
     *
     * @see splitByLongitude(Polygon, ComposedLongitude) for composed longitude variant
     * @see clipToHalfPlane for the underlying clipping algorithm
     */
    fun splitByLongitude(
        polygon: Polygon,
        lngToCut: Double,
    ): SplitResult = splitByLongitude(polygon, ComposedLongitude.fromLongitude(lngToCut))

    /**
     * Splits a polygon by a composed longitude (piecewise vertical line).
     *
     * A composed longitude is a sequence of vertical line segments that may
     * follow a non-straight path (e.g., along irregular time zone boundaries).
     *
     * **Algorithm**: Multi-phase splitting with topology correction
     *
     * 1. **Boundary Check**: If cut is completely outside polygon bounds,
     *    return entire polygon on appropriate side
     *
     * 2. **Simple Case**: If composed longitude is a single vertical line,
     *    delegate to [splitByVerticalLine]
     *
     * 3. **Complex Case**: Track segment-by-segment intersections:
     *    - For each polygon edge, determine which side of cut it's on
     *    - Calculate intersection points
     *    - Build left/right polygon fragments
     *    - Handle transitions (west→east, east→west)
     *
     * 4. **Topology Completion**: Insert intermediate points along the
     *    composed longitude to preserve topology
     *
     * **Time Complexity**: O(n × k) where n = vertices, k = longitude segments
     * **Space Complexity**: O(n + k) for intermediate polygon storage
     *
     * **Intersection Tracking**:
     * ```
     * Polygon edge crosses composed longitude:
     *
     *    West │ East
     *         │
     *    ●────┼────●  Edge crosses → add intersection to both sides
     *         ×
     *         │
     * ```
     *
     * **Topology Completion**:
     * When polygon intersects composed longitude at multiple points,
     * intermediate longitude points must be inserted to preserve the
     * "cut edge" topology:
     *
     * ```
     *         Composed longitude
     *              │╲
     *    ╭─────────┼─╲────╮
     *    │         │  ╲   │   Without completion: gap in cut edge
     *    │    ●────×───●──│   With completion: smooth boundary
     *    │         │      │
     *    ╰─────────┼──────╯
     *              │
     * ```
     *
     * @param polygon The polygon to split
     * @param lngToCut The composed longitude to split along
     * @return SplitResult with left (west) and right (east) polygon fragments
     *
     * @see completeLongitudePoints for topology correction algorithm
     * @see ComposedLongitude.intersectWithSegment for intersection calculation
     */
    fun splitByLongitude(
        polygon: Polygon,
        lngToCut: ComposedLongitude,
    ): SplitResult {
        val workingPolygon = Polygon()
        workingPolygon.addAll(polygon)
        workingPolygon.close().pop() // Ensure the polygon is closed and remove the last point

        val isInvalidPolygon = workingPolygon.isEmpty() || workingPolygon.size < 3
        if (isInvalidPolygon) {
            return SplitResult.empty().also { workingPolygon.close() }
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
     * Clips a polygon ring against a vertical half-plane using Sutherland-Hodgman algorithm.
     *
     * This function clips a closed polygon against an infinite vertical line
     * defined by x = [cutLng]. It returns all vertices that fall on the specified
     * side of the line, plus any intersection points with the clipping plane.
     *
     * **Algorithm**: Sutherland-Hodgman specialized for vertical clipping plane
     *
     * The classic Sutherland-Hodgman algorithm processes each edge:
     * ```
     * For each edge (prev, curr):
     *   Case 1: Both inside  → Add curr
     *   Case 2: Leaving      → Add intersection
     *   Case 3: Entering     → Add intersection + curr
     *   Case 4: Both outside → Add nothing
     * ```
     *
     * **Time Complexity**: O(n) where n = number of vertices
     * **Space Complexity**: O(n) for output polygon
     *
     * **Visual Example** (keepLeft = true, cutLng = 0):
     * ```
     *         Cut line
     *            │
     *     ╭──────┼───╮          ╭──────●
     *     │      │   │    →     │      │
     *     │   ●──┼───╯          │   ●──●
     *     ╰──────┼               ╰──────
     *            │
     *    KEEP    │   DISCARD
     * ```
     *
     * **Epsilon Handling**:
     * - Uses FLOATING_POINT_EPSILON (1e-12) for boundary tolerance
     * - Points exactly on the line are considered "inside" both sides
     * - Prevents numerical errors from discarding boundary vertices
     *
     * **Edge Cases**:
     * - Polygon entirely on one side: returns full polygon or empty list
     * - Polygon crosses line multiple times: correctly handles all intersections
     * - Vertices exactly on cut line: included in output
     * - < 3 vertices: returns empty list (degenerate polygon)
     *
     * @param points The polygon vertices (not required to be closed)
     * @param cutLng The longitude of the vertical clipping line
     * @param keepLeft If true, keeps vertices with lng ≤ cutLng; else keeps lng ≥ cutLng
     * @return List of clipped vertices (not closed, may be empty)
     *
     * **Reference**: Sutherland, I.E.; Hodgman, G.W. (1974).
     * "Reentrant Polygon Clipping". Communications of the ACM.
     */
    fun clipToHalfPlane(
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
     * preserving order and closure (first == last).
     *
     * **Algorithm**: Single-pass deduplication with closure preservation
     * - Iterate through vertices, tracking last added vertex
     * - Skip consecutive duplicates
     * - Ensure final polygon is closed (first == last)
     *
     * **Time Complexity**: O(n)
     * **Space Complexity**: O(n) for cleaned polygon
     *
     * **Why This Matters**:
     * Consecutive duplicates can arise from:
     * - Intersection calculations producing near-identical points
     * - Polygon clipping creating redundant boundary vertices
     * - Topology completion inserting existing points
     *
     * Removing them prevents:
     * - Degenerate edges (zero length)
     * - Numerical instability in downstream algorithms
     * - Visual rendering artifacts
     *
     * **Closure Handling**:
     * ```
     * Input:  [A, B, B, C, A]  → Already closed, has duplicate
     * Output: [A, B, C, A]     → Cleaned, still closed
     *
     * Input:  [A, B, C]        → Not closed
     * Output: [A, B, C, A]     → Cleaned and closed
     * ```
     *
     * @param polygon The polygon to clean
     * @return New polygon instance (same type) with duplicates removed
     */
    fun removeConsecutiveDuplicates(polygon: Polygon): Polygon {
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
     * Adds intermediate points along a composed longitude to polygon fragments.
     *
     * **Purpose**: Preserve topological correctness after splitting
     *
     * When a polygon is split by a composed longitude (piecewise vertical line),
     * the resulting fragments may have "gaps" along the cut boundary where the
     * composed longitude itself has intermediate vertices. This function fills
     * those gaps by inserting the intermediate longitude points.
     *
     * **Algorithm**: Anchor-based insertion with containment validation
     *
     * 1. **Find Anchors**: Locate polygon vertices that lie on the composed longitude
     * 2. **Process Pairs**: For each consecutive pair of anchors:
     *    - Find intermediate longitude points between them
     *    - Check if midpoint is inside original polygon (prevents bridging concave regions)
     *    - Insert intermediate points between anchors
     *
     * **Time Complexity**: O(n² × k) worst case
     * - n = polygon vertices
     * - k = longitude segments
     * - Containment check is O(n), done for each anchor pair
     *
     * **Visual Example**:
     * ```
     * Original polygon | Composed longitude | Without completion | With completion
     *                  |    │╲               |                    |
     *      ╭───────╮   |    │ ╲              |   ╭───●            |   ╭───●
     *      │       │   |    │  ●             |   │                |   │   │
     *      │   ●───┼───┼───●│              │   ●   │            |   │   ● (inserted)
     *      ╰───────╯   |    │                |   ╰───●            |   ╰───●
     *                  |    │                |                    |
     *                  | Anchors: ●          | Gap in boundary!   | Smooth boundary
     * ```
     *
     * **Concavity Protection**:
     * The midpoint containment check prevents inserting points that would
     * "bridge" across concave regions:
     *
     * ```
     *     Concave "mouth"           Without check         With check
     *          │                         │                    │
     *      ╭───┼───╮                 ╭───●───╮            ╭───●
     *      │   │   │                 │   │   │            │   │
     *      ╰───┼───╯    →            │   ●   │     vs     │   │
     *          │                     │       │            ╰───●
     *          ● Midpoint            ╰───────╯ WRONG!         │ CORRECT
     *       (outside!)
     * ```
     *
     * **Edge Cases**:
     * - Single longitude segment: no intermediate points, returns unchanged
     * - < 2 anchors per polygon: no completion needed
     * - Anchors not in polygon: skipped safely
     * - Midpoint outside original: intermediate points not inserted
     *
     * @param lngToCut The composed longitude to insert points from
     * @param polygons The polygon fragments to complete
     * @param source The original (unsplit) polygon for containment checks
     * @param forLeft Whether processing left (west) or right (east) fragments (currently unused)
     * @return Polygons with intermediate longitude points inserted
     */
    fun completeLongitudePoints(
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
}
