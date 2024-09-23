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

import com.worldwidewaves.shared.events.utils.GeoUtils.EPSILON
import com.worldwidewaves.shared.events.utils.GeoUtils.Vector2D
import com.worldwidewaves.shared.events.utils.GeoUtils.isLongitudeEqual
import com.worldwidewaves.shared.events.utils.GeoUtils.isPointOnSegment
import com.worldwidewaves.shared.events.utils.GeoUtils.normalizeLongitude
import com.worldwidewaves.shared.events.utils.GeoUtils.normalizedLongitudeDifference
import kotlin.math.abs
import kotlin.math.sign

open class ComposedLongitude(position: Position? = null) : Iterable<Position> {

    private val positions = mutableListOf<Position>()

    private var swLat: Double = Double.POSITIVE_INFINITY
    private var swLng: Double = Double.POSITIVE_INFINITY
    private var neLat: Double = Double.NEGATIVE_INFINITY
    private var neLng: Double = Double.NEGATIVE_INFINITY

    var direction: Direction = Direction.NORTH
        private set

    // --- Nested classes and enums

    enum class Direction { NORTH, SOUTH }
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
        fun fromLongitude(longitude: Double) = ComposedLongitude(Position(0.0, longitude))
    }

    // --- Public methods -----

    fun add(position: Position) {
        val normalizedPosition = position.copy(lng = normalizeLongitude(position.lng))
        val tempPositions = positions.toMutableList()
        tempPositions.add(normalizedPosition)

        if (isValidArc(tempPositions)) {
            positions.add(normalizedPosition)
            updateBoundingBox(listOf(normalizedPosition))
            sortPositions()
        } else throw IllegalArgumentException("Invalid arc")
    }

    fun addAll(newPositions: Collection<Position>) {
        val normalizedNewPositions = newPositions.map { it.copy(lng = normalizeLongitude(it.lng)) }
        val tempPositions = positions.toMutableList()
        tempPositions.addAll(normalizedNewPositions)

        if (isValidArc(tempPositions)) {
            positions.addAll(normalizedNewPositions)
            updateBoundingBox(normalizedNewPositions)
            sortPositions()
        } else throw IllegalArgumentException("Invalid arc")
    }

    fun isPointOnLine(point: Position): Side {
        val normalizedPoint = point.copy(lng = normalizeLongitude(point.lng))

        if (positions.isEmpty()) return Side.EAST // Arbitrary choice when empty

        // Handle vertical line (single longitude) case
        if (positions.size == 1 || positions.all { isLongitudeEqual(it.lng, positions.first().lng) }) {
            val lineLng = normalizeLongitude(positions.first().lng)
            return when {
                isLongitudeEqual(normalizedPoint.lng, lineLng) -> Side.ON
                normalizedLongitudeDifference(normalizedPoint.lng, lineLng) > 0 -> Side.EAST
                else -> Side.WEST
            }
        }

        for (i in 0 until positions.size - 1) {
            val start = positions[i]
            val end = positions[i + 1]
            val segment = Segment(start, end)

            if (isPointOnSegment(normalizedPoint, segment)) return Side.ON

            // Calculate vectors
            val lineVector = Vector2D(end.lng - start.lng, end.lat - start.lat)
            val pointVector = Vector2D(normalizedPoint.lng - start.lng, normalizedPoint.lat - start.lat)

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

    fun positionsBetween(minLat: Double, maxLat: Double): List<Position> =
         positions.filter { it.lat > minLat && it.lat < maxLat }.sortedBy { it.lat }

    fun isValidArc(positions: List<Position> = this.positions): Boolean {
        if (positions.size <= 2) return true
        val normalizedPositions = positions.map { it.normalized() }
        val differences = normalizedPositions.zipWithNext { a, b -> normalizeLongitude(b.lng - a.lng) }
        val signs = differences.map { it.sign }
        val changes = signs.zipWithNext { a, b -> a != b }.count { it }
        val distinctSigns = signs.distinct().size
        return changes <= 1 && distinctSigns <= 2
    }

    fun bbox(): BoundingBox = BoundingBox(
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
        direction = if (positions.size <= 1 || positions.first().lat == southToNorth.first().lat) {
            Direction.NORTH
        } else {
            Direction.SOUTH
        }
        positions.clear()
        positions.addAll(if (direction == Direction.NORTH) southToNorth else southToNorth.reversed())
    }

    private fun updateBoundingBox(newPositions: List<Position>) {
        swLat = minOf(swLat, newPositions.minOf { it.lat })
        swLng = minOf(swLng, newPositions.minOf { it.lng })
        neLat = maxOf(neLat, newPositions.maxOf { it.lat })
        neLng = maxOf(neLng, newPositions.maxOf { it.lng })
    }

}