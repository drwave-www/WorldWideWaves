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

import androidx.annotation.VisibleForTesting
import com.worldwidewaves.shared.events.utils.GeoUtils.EPSILON
import com.worldwidewaves.shared.events.utils.GeoUtils.isLongitudeEqual
import com.worldwidewaves.shared.events.utils.GeoUtils.isPointOnSegment
import com.worldwidewaves.shared.events.utils.GeoUtils.normalizeLongitude
import com.worldwidewaves.shared.events.utils.Position.Companion.nextId
import kotlin.Double.Companion.NEGATIVE_INFINITY
import kotlin.Double.Companion.POSITIVE_INFINITY
import kotlin.math.abs
import kotlin.math.sign

// ----------------------------------------------------------------------------

/**
 * Represents a geographic position with latitude and longitude coordinates.
 *
 */
open class Position(val lat: Double, val lng: Double, // Element of the double LL Polygon
                    internal var prev: Position? = null,
                    internal var next: Position? = null) {

    companion object { internal var nextId = 42 }

    var id: Int = -1
        internal set // Cannot be set outside of the module
        get() { // Cannot be read before being initialized (added to a Polygon)
            if (field == -1) throw IllegalStateException("ID has not been initialized")
            return field
        }

    open operator fun component1() = lat
    open operator fun component2() = lng

    fun toCutPosition(cutId: Int, cutLeft: Position, cutRight: Position) =
        CutPosition(lat, lng, cutId, cutLeft, cutRight).init()

    fun toPointCut(cutId: Int) =
        CutPosition(lat, lng, cutId, this, this).init()

    fun copy(lat: Double? = null, lng: Double? = null): Position {
        return Position(lat ?: this.lat, lng ?: this.lng)
    }

    internal open fun xfer() = Position(lat, lng).init() // Polygon detach / reattach

    internal open fun detached() = Position(lat, lng)

    fun normalized() = Position(lat,normalizeLongitude(lng))

    override fun equals(other: Any?): Boolean =
        this === other || (other is Position && lat == other.lat && lng == other.lng)
    override fun toString(): String = "($lat, $lng)"
    override fun hashCode(): Int = 31 * lat.hashCode() + lng.hashCode()

}

// Can only be initialized from internal context
@VisibleForTesting
internal fun <T : Position> T.init(): T = apply {
    id = nextId++
    if (id == Int.MAX_VALUE) 
        throw IllegalStateException("Reached maximum capacity for Polygon positions") 
}

// ------------------

class CutPosition( // A position that has been cut
    lat: Double, lng: Double,
    val cutId: Int, val cutLeft: Position, val cutRight: Position
) : Position(lat, lng) {

    // Id which is shared/can be compared between the two cut positions
    val pairId: Double by lazy { (cutId + cutLeft.id + cutRight.id).toDouble() }

    override fun xfer() = CutPosition(lat, lng, cutId, cutLeft, cutRight).init()

    override fun equals(other: Any?): Boolean =
        this === other || (other is Position && super.equals(other) && if (other is CutPosition) cutId == other.cutId else true)
    override fun hashCode(): Int = 31 * super.hashCode() + cutId.hashCode()
}

// ------------------------------------

/**
 * Represents a segment defined by its start and end positions.
 */
data class Segment(val start: Position, val end: Position) {

    fun normalized(): Segment = Segment(start.normalized(), end.normalized())

    /**
     * Calculates the intersection of the segment with a given longitude
     * and returns a CutPosition if the segment intersects the longitude.
     */
    fun intersectWithLng(cutId: Int, cutLng: Double): CutPosition? {
        val normalizedCutLng = normalizeLongitude(cutLng)
        val normalizedStartLng = normalizeLongitude(start.lng)
        val normalizedEndLng = normalizeLongitude(end.lng)

        // Calculate the latitude of intersection
        val latDiff = end.lat - start.lat
        val lngDiff = normalizeLongitude(normalizedEndLng - normalizedStartLng)

        // Check for vertical line - No unique intersection for a vertical line
        if (abs(lngDiff) < EPSILON) return null

        val t = normalizeLongitude(normalizedCutLng - normalizedStartLng) / lngDiff
        val lat = start.lat + t * latDiff

        // Check if the intersection point is on the segment
        if (t < 0 || t > 1) return null

        // Determine the direction and create the CutPosition
        return when {
            normalizedStartLng == normalizedCutLng && normalizedEndLng == normalizedCutLng ->
                null // No intersection for a vertical line
            normalizeLongitude(normalizedEndLng - normalizedStartLng) > 0 ->
                // Moving eastward
                CutPosition(lat = lat, lng = cutLng, cutId = cutId,
                    cutLeft = start.detached(), cutRight = end.detached()
                )
            else ->
                // Moving westward
                CutPosition(lat = lat, lng = cutLng, cutId = cutId,
                    cutLeft = end.detached(), cutRight = start.detached()
                )
        }
    }

    /**
     * Calculates the intersection of the segment with a given other segment
     * and returns a CutPosition if the segments intersects.
     */
    fun intersectWithSegment(cutId: Int, other: Segment): CutPosition? {
        val (x1, y1) = start.lng to start.lat
        val (x2, y2) = end.lng to end.lat
        val (x3, y3) = other.start.lng to other.start.lat
        val (x4, y4) = other.end.lng to other.end.lat

        val denominator = (y4 - y3) * (x2 - x1) - (x4 - x3) * (y2 - y1)
        if (abs(denominator) < 1e-9) return null // Lines are parallel

        val ua = ((x4 - x3) * (y1 - y3) - (y4 - y3) * (x1 - x3)) / denominator
        val ub = ((x2 - x1) * (y1 - y3) - (y2 - y1) * (x1 - x3)) / denominator

        if (ua < 0 || ua > 1 || ub < 0 || ub > 1) return null // Intersection point is outside of both line segments

        val x = normalizeLongitude(x1 + ua * (x2 - x1))
        val y = y1 + ua * (y2 - y1)
        val intersect = Position(x, y)

        return when {
            isPointOnSegment(intersect, this) && isPointOnSegment(intersect, other) ->
                CutPosition(lat = y, lng = x, cutId = cutId,
                    cutLeft = if (x1 < x2) start.detached() else end.detached(),
                    cutRight = if (x1 < x2) end.detached() else start.detached()
                )
            else -> null
        }
    }

}

// ------------------------------------

open class Polygon(position: Position? = null) : Iterable<Position> { // Not thread-safe

    internal var head: Position? = null
    internal var tail: Position? = null
    internal val cutPositions = mutableSetOf<CutPosition>()
    internal val positionsIndex = mutableMapOf<Int, Position>()

    private var isClockwise: Boolean = true
    private var area: Double = 0.0

    private var minLat: Double = POSITIVE_INFINITY
    private var minLng: Double = POSITIVE_INFINITY
    private var maxLat: Double = NEGATIVE_INFINITY
    private var maxLng: Double = NEGATIVE_INFINITY
    private var boundingBoxValid: Boolean = false

    // --------------------------------

    init { position?.let { add(it) } }

    // --------------------------------

    open fun createNew(): Polygon = Polygon() // Ensure the right type is created

    companion object {
        fun fromPositions(vararg positions: Position): Polygon =
            Polygon().apply { positions.forEach { add(it) } }
    }

    // --------------------------------

    fun getCutPositions(): Set<CutPosition> = cutPositions

    // -- index helpers ---------------

    private fun indexNewPosition(newPosition: Position) {
        positionsIndex[newPosition.id] = newPosition
        if (newPosition is CutPosition) cutPositions.add(newPosition)
    }

    private fun removePositionFromIndex(id: Int) : Position {
        val positionToRemove = positionsIndex.remove(id) ?: throw IllegalArgumentException("Position with id $id not found")
        if (positionToRemove is CutPosition) cutPositions.remove(positionToRemove)
        return positionToRemove
    }

    // -- Direction logic -------------

    fun isClockwise(): Boolean = when {
        size < 3 -> true // Two-point polygon is considered clockwise
        else -> isClockwise
    }

    private fun updateAreaAndDirection(p1: Position?, p2: Position?, isRemoving: Boolean = false) {
        if (p1 == null || p2 == null) return

        val signedArea = (p2.lng - p1.lng) * (p2.lat + p1.lat)
        area += if (isRemoving) -signedArea else signedArea

        // Update isClockwise based on the sign of the total area
        isClockwise = area > 0
    }

    fun forceDirectionComputation() {
        area = 0.0
        var current = head
        while (current?.next != null) {
            updateAreaAndDirection(current, current.next)
            current = current.next
        }
        // Close the polygon direction calculation
        if (head != null && tail != null) {
            updateAreaAndDirection(tail, head)
        }
    }

    // -- Bounding Box logic ----------

    fun bbox(): BoundingBox {
        if (isEmpty()) throw IllegalArgumentException("Polygon bbox: cannot compute bounding box of an empty polygon")
        if (!boundingBoxValid) {
            updateBoundingBox()
        }
        return BoundingBox(
            sw = Position(minLat, minLng),
            ne = Position(maxLat, maxLng)
        )
    }

    private fun updateBoundingBox() {
        if (isEmpty() || !boundingBoxValid) {
            minLat = POSITIVE_INFINITY
            minLng = POSITIVE_INFINITY
            maxLat = NEGATIVE_INFINITY
            maxLng = NEGATIVE_INFINITY
            boundingBoxValid = true
            if (isEmpty()) return
        }

        forEach { position ->
            updateBoundingBoxForPosition(position)
        }

        boundingBoxValid = true
    }

    private fun updateBoundingBoxForPosition(position: Position) {
        if (size == 1) {
            head?.let { firstPoint ->
                minLat = firstPoint.lat
                minLng = firstPoint.lng
                maxLat = firstPoint.lat
                maxLng = firstPoint.lng
                boundingBoxValid = true
            }
        }
        if (boundingBoxValid) {
            minLat = minOf(minLat, position.lat)
            minLng = minOf(minLng, position.lng)
            maxLat = maxOf(maxLat, position.lat)
            maxLng = maxOf(maxLng, position.lng)
        }
    }

    // -- Add/Remove positions --------

    fun add(position: Position) : Position {
        if (tail != null && position == tail)
            return tail!! // skip us time

        val addPosition = position.xfer()
        indexNewPosition(addPosition)

        if (head == null) {
            head = addPosition
        } else {
            addPosition.prev = tail
            tail?.next = addPosition
        }

        updateBoundingBoxForPosition(addPosition)
        updateAreaAndDirection(tail, addPosition)
        return addPosition.apply { tail = this }
    }

    fun addAll(polygon: Polygon) {
        polygon.forEach { add(it) }
    }

    fun remove(id: Int): Boolean {
        val positionToRemove = removePositionFromIndex(id)

        updateAreaAndDirection(positionToRemove.prev, positionToRemove, true)
        updateAreaAndDirection(positionToRemove, positionToRemove.next, true)

        when (positionToRemove.id) {
            head?.id -> {
                head = positionToRemove.next
                head?.prev = null
                if (head == null) tail = null
            }
            tail?.id -> {
                tail = positionToRemove.prev
                tail?.next = null
            }
            else -> {
                positionToRemove.prev?.next = positionToRemove.next
                positionToRemove.next?.prev = positionToRemove.prev
            }
        }

        boundingBoxValid = false  // Invalidate the bounding box, no way to optimize this
        return true
    }

    fun insertAfter(newPosition: Position, id: Int): Position {
        val current = positionsIndex[id] ?: throw IllegalArgumentException("Position with id $id not found")
        val addPosition = newPosition.xfer().apply {
            next = current.next
            prev = current
            current.next = this
            next?.prev = this
        }

        indexNewPosition(addPosition)
        if (current == tail) tail = addPosition

        updateBoundingBoxForPosition(addPosition)
        updateAreaAndDirection(current, addPosition)
        updateAreaAndDirection(addPosition, addPosition.next)
        return addPosition
    }

    fun insertBefore(newPosition: Position, id: Int): Position {
        val current = positionsIndex[id] ?: throw IllegalArgumentException("Position with id $id not found")
        val addPosition = newPosition.xfer().apply {
            next = current
            prev = current.prev
            current.prev = this
            if (current == head)
                head = this
            else
                prev?.next = this
        }

        indexNewPosition(addPosition)
        updateBoundingBoxForPosition(addPosition)
        updateAreaAndDirection(addPosition, current)
        updateAreaAndDirection(addPosition.prev, addPosition)
        return addPosition
    }

    fun removePositionsUpTo(pointId: Int): Boolean {
        // Check if the polygon is empty or if the toCut id doesn't exist
        if (isEmpty() || !positionsIndex.containsKey(pointId)) return false

        // If toCut is the head, nothing to delete
        if (head?.id == pointId) return true

        var current = head
        while (current != null && current.id != pointId) {
            val next = current.next
            remove(current.id)
            current = next
        }

        // Update the head to be the point
        head = current
        head?.prev = null

        return true
    }

    fun pop(): Position? {
        val last = tail ?: return null
        remove(last.id)
        return last
    }

    // -- Iterators -------------------

    interface LoopIterator<T> : Iterator<T> {
        fun viewCurrent(): T
    }

    override fun iterator(): Iterator<Position> = object : Iterator<Position> {
        private var current = head
        override fun hasNext(): Boolean = current != null
        override fun next(): Position {
            val result = current ?: throw NoSuchElementException()
            current = current?.next
            return result
        }
    }

    fun loopIterator(): LoopIterator<Position> = object : LoopIterator<Position> {
        private var current = head
        override fun hasNext(): Boolean = head != null
        override fun viewCurrent(): Position = current ?: head ?: throw NoSuchElementException()
        override fun next(): Position {
            val result = current ?: throw NoSuchElementException()
            current = current?.next ?: head
            return result
        }
    }

    fun reverseIterator(): Iterator<Position> = object : Iterator<Position> {
        private var current = tail
        override fun hasNext(): Boolean = current != null
        override fun next(): Position {
            val result = current ?: throw NoSuchElementException()
            current = current?.prev
            return result
        }
    }

    fun reverseLoopIterator(): LoopIterator<Position> = object : LoopIterator<Position> {
        private var current = tail
        override fun hasNext(): Boolean = tail != null
        override fun viewCurrent(): Position = current ?: tail ?: throw NoSuchElementException()
        override fun next(): Position {
            val result = current ?: throw NoSuchElementException()
            current = current?.prev ?: tail
            return result
        }
    }

    fun cutIterator() = cutPositions.iterator()

    // --------------------------------

    fun clear(): Polygon {
        positionsIndex.clear()
        cutPositions.clear()
        head = null
        tail = null
        area = 0.0
        isClockwise = true
        minLat = POSITIVE_INFINITY
        minLng = POSITIVE_INFINITY
        maxLat = NEGATIVE_INFINITY
        maxLng = NEGATIVE_INFINITY
        boundingBoxValid = true
        return this
    }

    // --------------------------------

    fun first(): Position? = head
    fun last(): Position? = tail
    val size: Int get() = positionsIndex.size
    val cutSize: Int get() = cutPositions.size
    fun isEmpty(): Boolean = positionsIndex.isEmpty()
    fun isCutEmpty(): Boolean = cutPositions.isEmpty()
    fun isNotEmpty(): Boolean = positionsIndex.isNotEmpty()
    fun isNotCutEmpty(): Boolean = cutPositions.isNotEmpty()
    fun getPosition(id: Int): Position? = positionsIndex[id]

    // --------------------------------

    override fun toString(): String {
        val maxPointsToShow = 30
        val pointsString = take(maxPointsToShow).joinToString(", ") { "(${it.lat}, ${it.lng})" }
        val pointsDisplay = if (size > maxPointsToShow) "$pointsString, ..." else pointsString
        val closedStatus = if (isNotEmpty()) ", closed=${first() == last()}" else ""
        val cutIdStatus = if (this is PolygonUtils.CutPolygon) ", cutId=$cutId" else ""

        return "Polygon(size=$size$closedStatus$cutIdStatus, points=[$pointsDisplay])"
    }

}

// -- Additional type preservation Polygon methods ----------

fun <T: Polygon> T.subList(start: Position, lastId: Int) = createNew().apply {
    if (this@subList.isEmpty())
        throw IllegalArgumentException("Polygon subList: 'start' cannot be found in an empty polygon")

    if (!this@subList.positionsIndex.containsKey(lastId))
        throw IllegalArgumentException("Polygon subList: 'lastId' cannot be found in the polygon")

    if (start.id == lastId) add(start).also { return@apply } // start == end

    var current = start
    do {
        add(current)
        current = current.next ?: this@subList.first()!!
        if (current.id == start.id) // Extra safety check
            throw IllegalArgumentException("Polygon subList: 'last' cannot be found in the polygon")
    } while (current.id != lastId)
}

fun <T: Polygon> T.withoutLast(n: Int = 1): Polygon = createNew().apply {
    val newSize = (this@withoutLast.size - n).coerceAtLeast(0)
    var current = this@withoutLast.head
    repeat(newSize) {
        add(current!!)
        current = current!!.next
    }
}

inline fun <reified T: Polygon> T.move() : T = createNew().xferFrom(this) as T


operator fun <T: Polygon> T.plus(other: Polygon) = createNew().apply {
    this@plus.forEach { add(it) }
    other.forEach { add(it) }
}

fun <T: Polygon> T.xferFrom(polygon: Polygon) : T {
    head = polygon.head
    tail = polygon.tail
    positionsIndex.putAll(polygon.positionsIndex)
    cutPositions.addAll(polygon.cutPositions)
    polygon.clear()
    return this
}

fun <T: Polygon> T.close() : T {
    if (isNotEmpty() && first() != last()) {
        add(first()!!)
    }
    return this
}

// ----------------------------------------------------------------------------

open class ComposedLongitude(position: Position? = null) : Iterable<Position> {

    private val positions = mutableListOf<Position>()
    var direction: Direction = Direction.NORTH
        private set

    enum class Direction { NORTH, SOUTH }

    init {
        position?.let { add(it) }
    }

    fun add(position: Position) {
        val normalizedPosition = position.copy(lng = normalizeLongitude(position.lng))
        val tempPositions = positions.toMutableList()
        tempPositions.add(normalizedPosition)

        if (isValidArc(tempPositions)) {
            positions.add(normalizedPosition)
            sortPositions()
        } else throw IllegalArgumentException("Invalid arc")
    }

    fun addAll(newPositions: Collection<Position>) {
        val normalizedNewPositions = newPositions.map { it.copy(lng = normalizeLongitude(it.lng)) }
        val tempPositions = positions.toMutableList()
        tempPositions.addAll(normalizedNewPositions)

        if (isValidArc(tempPositions)) {
            positions.addAll(normalizedNewPositions)
            sortPositions()
        } else throw IllegalArgumentException("Invalid arc")
    }

    fun isPointOnLine(point: Position): Boolean {
        val normalizedPoint = point.copy(lng = normalizeLongitude(point.lng))
        if (positions.isEmpty()) return false
        if (positions.size == 1) return isLongitudeEqual(normalizedPoint.lng, positions.first().lng)
        return positions.zipWithNext { start, end ->
            Segment(start, end)
        }.any { segment ->
            isPointOnSegment(normalizedPoint, segment)
        }
    }

    fun intersectWithSegment(cutId: Int, segment: Segment): CutPosition? {
        val normalizedSegment = Segment(
            segment.start.copy(lng = normalizeLongitude(segment.start.lng)),
            segment.end.copy(lng = normalizeLongitude(segment.end.lng))
        )

        if (positions.isEmpty()) return null
        if (positions.size == 1) return normalizedSegment.intersectWithLng(cutId, positions.first().lng)

        return positions.zipWithNext { start, end ->
            Segment(start, end)
        }.firstNotNullOfOrNull { lineSegment ->
            lineSegment.intersectWithSegment(cutId, normalizedSegment)
        }
    }

    fun isValidArc(positions: List<Position> = this.positions): Boolean {
        if (positions.size <= 2) return true
        val normalizedPositions = positions.map { it.normalized() }
        val differences = normalizedPositions.zipWithNext { a, b -> normalizeLongitude(b.lng - a.lng) }
        val signs = differences.map { it.sign }
        val changes = signs.zipWithNext { a, b -> a != b }.count { it }
        val distinctSigns = signs.distinct().size
        return changes <= 1 && distinctSigns <= 2
    }

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


    fun getPositions(): List<Position> = positions.toList()
    override fun iterator(): Iterator<Position> = positions.iterator()
    fun reverseIterator(): Iterator<Position> = positions.asReversed().iterator()
}

// ----------------------------------------------------------------------------

/**
 * Represents a bounding box defined by its southwest and northeast corners.
 *
 */
data class BoundingBox(
    val sw: Position,
    val ne: Position
) {
    constructor(swLat: Double, swLng: Double, neLat: Double, neLng: Double) : this(
        sw = Position(swLat, swLng).init(),
        ne = Position(neLat, neLng).init()
    )

    val minLatitude: Double get() = sw.lat
    val maxLatitude: Double get() = ne.lat
    val minLongitude: Double get() = sw.lng
    val maxLongitude: Double get() = ne.lng

    override fun equals(other: Any?): Boolean =
        this === other || (other is BoundingBox && sw == other.sw && ne == other.ne)
    override fun hashCode(): Int = 31 * sw.hashCode() + ne.hashCode()
}