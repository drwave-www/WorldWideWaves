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

import com.worldwidewaves.shared.events.utils.GeoUtils.EPSILON
import com.worldwidewaves.shared.events.utils.GeoUtils.Vector2D
import com.worldwidewaves.shared.events.utils.GeoUtils.isLongitudeEqual
import com.worldwidewaves.shared.events.utils.GeoUtils.isPointOnSegment
import kotlin.math.abs
import kotlin.math.sign

open class ComposedLongitude(position: Position? = null) : Iterable<Position> {

    private val positions = mutableListOf<Position>()
    private var swLat: Double = Double.POSITIVE_INFINITY
    private var swLng: Double = Double.POSITIVE_INFINITY
    private var neLat: Double = Double.NEGATIVE_INFINITY
    private var neLng: Double = Double.NEGATIVE_INFINITY

    var orientation: Orientation = Orientation.NORTH
        private set

    // --- Nested classes and enums

    enum class Orientation { NORTH, SOUTH }
    enum class Side { EAST, WEST, ON ;
        fun isOn(): Boolean = this == ON
        fun isEast(): Boolean = this == EAST
        fun isWest(): Boolean = this == WEST
    }

    // ------------------------

    init { position?.let { add(it) } }

    companion object {
        fun fromPositions(vararg positions: Position): ComposedLongitude =
            ComposedLongitude().apply { addAll(positions.toList()) }
        fun fromPositions(positions: List<Position>): ComposedLongitude =
            ComposedLongitude().apply { addAll(positions.toList()) }
        fun fromLongitude(longitude: Double) = ComposedLongitude(Position(0.0, longitude))
    }

    // --- Public methods -----

    fun add(position: Position) {
        val tempPositions = positions.toMutableList()
        tempPositions.add(position)

        if (isValidArc(tempPositions)) {
            positions.add(position)
            updateBoundingBox(listOf(position))
            sortPositions()
        } else throw IllegalArgumentException("Invalid arc")
    }

    fun addAll(newPositions: List<Position>) {
        val tempPositions = positions.toMutableList()
        tempPositions.addAll(newPositions)

        if (isValidArc(tempPositions)) {
            positions.addAll(newPositions)
            updateBoundingBox(newPositions)
            sortPositions()
        } else throw IllegalArgumentException("Invalid arc")
    }

    fun clear() : ComposedLongitude {
        positions.clear()
        resetBoundingBox()
        orientation = Orientation.NORTH
        return this
    }

    // ------------------------------------------------------------------------

    fun isPointOnLine(point: Position): Side {
        if (positions.isEmpty()) return Side.EAST // Arbitrary choice when empty

        // Handle vertical line (single longitude) case
        if (positions.size == 1 || positions.all { isLongitudeEqual(it.lng, positions.first().lng) }) {
            val lineLng = positions.first().lng
            return when {
                isLongitudeEqual(point.lng, lineLng) -> Side.ON
                point.lng - lineLng > 0 -> Side.EAST
                else -> Side.WEST
            }
        }

        for (i in 0 until positions.size - 1) {
            val start = positions[i]
            val end = positions[i + 1]
            val segment = Segment(start, end)

            if (isPointOnSegment(point, segment))
                return Side.ON

            if (point.lat < minOf(start.lat, end.lat) || point.lat > maxOf(start.lat, end.lat))
                continue

            // Calculate vectors
            val lineVector = Vector2D(end.lng - start.lng, end.lat - start.lat)
            val pointVector = Vector2D(point.lng - start.lng, point.lat - start.lat)

            // Calculate cross product
            val crossProduct = lineVector.cross(pointVector)

            if (abs(crossProduct) > EPSILON) {
                return if (crossProduct < 0) Side.EAST else Side.WEST
            }
        }

        // If we've reached here, the point is not on the line and we couldn't determine the side
        // This could happen if the point is exactly on a vertical extension of the line
        // We'll make an arbitrary choice to return EAST in this case
        return Side.EAST
    }

    fun intersectWithSegment(cutId: Int, segment: Segment): CutPosition? {
        if (positions.isEmpty()) return null
        if (positions.size == 1) return segment.intersectWithLng(cutId, positions.first().lng)

        return positions.zipWithNext { start, end ->
            Segment(start, end)
        }.firstNotNullOfOrNull { lineSegment ->
            lineSegment.intersectWithSegment(cutId, segment)
        }
    }

    /**
     * Overload of [intersectWithSegment] that drops cut-tracking information.
     *
     * Returns the plain intersection [Position] of this composed-longitude and the provided
     * [segment] or `null` when they do not intersect.
     */
    fun intersectWithSegment(segment: Segment): Position? {
        if (positions.isEmpty()) return null
        if (positions.size == 1) {
            return segment.intersectWithLng(positions.first().lng)
        }

        return positions.zipWithNext { start, end ->
            Segment(start, end)
        }.firstNotNullOfOrNull { lineSegment ->
            lineSegment.intersectWithSegment(segment)
        }
    }

    /**
     * Returns the longitude value of this composed-longitude at the given latitude, when the
     * latitude lies on (or between vertices of) the poly-line.  For a vertical straight line
     * the single longitude is returned.  If the latitude does not intersect the composed
     * longitude, `null` is returned.
     */
    fun lngAt(lat: Double): Double? {
        if (positions.isEmpty()) return null
        if (positions.size == 1) return positions.first().lng

        positions.zipWithNext { a, b ->
            val minLat = minOf(a.lat, b.lat)
            val maxLat = maxOf(a.lat, b.lat)
            if (lat + EPSILON >= minLat && lat - EPSILON <= maxLat && abs(b.lat - a.lat) > EPSILON) {
                val t = (lat - a.lat) / (b.lat - a.lat)
                return a.lng + t * (b.lng - a.lng)
            }
        }
        return null
    }

    fun positionsBetween(minLat: Double, maxLat: Double): List<Position> =
         positions.filter { it.lat > minLat && it.lat < maxLat }.sortedBy { it.lat }

    fun isValidArc(positions: List<Position> = this.positions): Boolean {
        if (positions.size <= 2) return true

        val differences = positions.zipWithNext { a, b -> b.lng - a.lng }
        val signs = differences.map { it.sign }
        val changes = signs.zipWithNext { a, b -> a != b }.count { it }
        val distinctSigns = signs.distinct().size

        // For wave fronts, we need to be more permissive
        // Wave fronts can have curved patterns due to Earth's curvature
        // Allow more sign changes but still validate against completely chaotic patterns

        // Check if the pattern is reasonable for a wave front
        val nonZeroSigns = signs.filter { it != 0.0 }
        val nonZeroChanges = nonZeroSigns.zipWithNext { a, b -> a != b }.count { it }

        // Allow more flexibility for wave fronts:
        // - Allow more sign changes (up to 4-5 for complex wave fronts)
        // - Allow all 3 signs (positive, negative, zero) for curved wave fronts
        // - But check that non-zero signs don't change too frequently
        return changes <= 5 && distinctSigns <= 3 && nonZeroChanges <= 3
    }

    fun bbox(): BoundingBox = BoundingBox.fromCorners(
        sw = Position(swLat, swLng),
        ne = Position(neLat, neLng)
    )

    // -------------------------

    fun size() = positions.size
    fun getPositions(): List<Position> = positions.toList()
    override fun iterator(): Iterator<Position> = positions.iterator()
    fun reverseIterator(): Iterator<Position> = positions.asReversed().iterator()

    // -------------------------

    private fun sortPositions() {
        val southToNorth = positions.sortedBy { it.lat }
        orientation = if (positions.size <= 1 || positions.first().lat == southToNorth.first().lat) {
            Orientation.NORTH
        } else {
            Orientation.SOUTH
        }
        positions.clear()
        positions.addAll(if (orientation == Orientation.NORTH) southToNorth else southToNorth.reversed())
    }

    private fun updateBoundingBox(newPositions: List<Position>) {
        swLat = minOf(swLat, newPositions.minOf { it.lat })
        swLng = minOf(swLng, newPositions.minOf { it.lng })
        neLat = maxOf(neLat, newPositions.maxOf { it.lat })
        neLng = maxOf(neLng, newPositions.maxOf { it.lng })
    }

    private fun resetBoundingBox() {
        swLat = Double.POSITIVE_INFINITY
        swLng = Double.POSITIVE_INFINITY
        neLat = Double.NEGATIVE_INFINITY
        neLng = Double.NEGATIVE_INFINITY
    }

}