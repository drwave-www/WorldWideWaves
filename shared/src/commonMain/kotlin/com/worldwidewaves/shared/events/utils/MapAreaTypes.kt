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
import com.worldwidewaves.shared.events.utils.Position.Companion.nextId

// ----------------------------------------------------------------------------

/**
 * Represents a geographic position with latitude and longitude coordinates.
 *
 */
open class Position(val lat: Double, val lng: Double,
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

    internal open fun xfer() = Position(lat, lng).init() // Polygon detach / reattach

    internal open fun detached() = Position(lat, lng)

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
    /**
     * Calculates the intersection of the segment with a given longitude
     * and returns a CutPosition if the segment intersects the longitude.
     */
    fun intersectWithLng(cutId: Int, cutLng: Double): CutPosition? {
        val lat = start.lat + (end.lat - start.lat) * (cutLng - start.lng) / (end.lng - start.lng)
        return when {
            start.lng == cutLng && end.lng == cutLng -> null // No intersection
            start.lng < cutLng && end.lng > cutLng ->
                CutPosition(lat, cutLng, cutId = cutId,
                    cutLeft = start.detached(), cutRight = end.detached()) // Detach left and right
            start.lng > cutLng && end.lng < cutLng ->
                CutPosition(lat, cutLng, cutId = cutId,
                    cutLeft = end.detached(), cutRight = start.detached()) // Detach left and right
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

    // --------------------------------

    private fun indexNewPosition(newPosition: Position) {
        positionsIndex[newPosition.id] = newPosition
        if (newPosition is CutPosition) cutPositions.add(newPosition)
    }

    private fun removePositionFromIndex(id: Int) : Position {
        val positionToRemove = positionsIndex.remove(id) ?: throw IllegalArgumentException("Position with id $id not found")
        if (positionToRemove is CutPosition) cutPositions.remove(positionToRemove)
        return positionToRemove
    }

    // --------------------------------

    private var isClockwise: Boolean = true
    private var area: Double = 0.0

    fun isClockwise(): Boolean = when {
        size < 3 -> throw IllegalArgumentException("Polygon must have at least 3 points to determine direction")
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
        // Close the polygon
        if (head != null && tail != null) {
            updateAreaAndDirection(tail, head)
        }
    }

    // --------------------------------

    fun add(position: Position) : Position {
        if (tail != null && position == tail)
            return tail!!
        val addPosition = position.xfer()

        indexNewPosition(addPosition)

        if (head == null) {
            head = addPosition
        } else {
            addPosition.prev = tail
            tail?.next = addPosition
        }

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

        when (positionToRemove) {
            head -> {
                head = positionToRemove.next
                head?.prev = null
                if (head == null) tail = null
            }
            tail -> {
                tail = positionToRemove.prev
                tail?.next = null
            }
            else -> {
                positionToRemove.prev?.next = positionToRemove.next
                positionToRemove.next?.prev = positionToRemove.prev
            }
        }

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
        updateAreaAndDirection(addPosition, current)
        updateAreaAndDirection(addPosition.prev, addPosition)
        return addPosition
    }

    // --------------------------------

    fun subList(start: Position, lastId: Int) = createNew().apply {
        if (this@Polygon.isEmpty())
            throw IllegalArgumentException("Polygon subList: 'start' cannot be found in an empty polygon")

        if (!this@Polygon.positionsIndex.containsKey(lastId))
            throw IllegalArgumentException("Polygon subList: 'lastId' cannot be found in the polygon")

        if (start.id == lastId) add(start).also { return@apply } // start == end
        
        var current = start
        do {
            add(current)
            current = current.next ?: this@Polygon.first()!!
            if (current.id == start.id) // Extra safety check
                throw IllegalArgumentException("Polygon subList: 'last' cannot be found in the polygon")
        } while (current.id != lastId)
    }

    fun dropLast(n: Int = 1): Polygon = createNew().apply {
        val newSize = (this@Polygon.size - n).coerceAtLeast(0)
        var current = this@Polygon.head
        repeat(newSize) {
            add(current!!)
            current = current!!.next
        }
    }

    fun deletePointsUpTo(pointId: Int): Boolean {
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

    // --------------------------------

    operator fun plus(other: Polygon) = createNew().apply {
        this@Polygon.forEach { add(it) }
        other.forEach { add(it) }
    }

    // --------------------------------

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

    open fun copy() = createNew().apply {
        this@Polygon.forEach { add(it) }
    }

    fun pop(): Position? {
        val last = tail ?: return null
        remove(last.id)
        return last
    }

    fun clear(): Polygon {
        positionsIndex.clear()
        cutPositions.clear()
        head = null
        tail = null
        area = 0.0
        isClockwise = true
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