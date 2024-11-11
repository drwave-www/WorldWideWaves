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
import com.worldwidewaves.shared.events.utils.PolygonUtils.CutPolygon
import kotlin.Double.Companion.NEGATIVE_INFINITY
import kotlin.Double.Companion.POSITIVE_INFINITY

typealias Area = List<Polygon>
typealias MutableArea = MutableList<Polygon>

open class Polygon(position: Position? = null) : Iterable<Position> { // Not thread-safe

    internal var head: Position? = null
    internal var tail: Position? = null
    internal val cutPositions = mutableSetOf<CutPosition>()
    internal val positionsIndex = mutableMapOf<Int, Position>()

    private var isClockwise: Boolean = true

    @VisibleForTesting
    var area: Double = 0.0

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
        fun fromPositions(positions: List<Position>): Polygon =
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
        val positionToRemove = requireNotNull(positionsIndex.remove(id)) {
            "Position with id $id not found"
        }
        if (positionToRemove is CutPosition) cutPositions.remove(positionToRemove)
        return positionToRemove
    }

    // -- Direction logic -------------

    fun isClockwise(): Boolean {
        val retClockwise = when {
            size < 3 -> true // Two-point polygon is considered clockwise
            else -> area + (head!!.lng - tail!!.lng) * (head!!.lat + tail!!.lat) > 0 // Ensure closing
        }
        return retClockwise
    }

    private fun updateAreaAndDirection(p1: Position?, p2: Position?, isRemoving: Boolean = false) {
        require(p1 != null && p2 != null) { return }

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
        require(!isEmpty()) { "Polygon bbox: cannot compute bounding box of an empty polygon" }
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
        if (tail != null && position == tail) { // Do not add consecutive same point
            return tail!!
        }

        val addPosition = position.xfer() // Disconnect the position from a possible other polygon
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

    fun addAll(polygon: Polygon) = polygon.forEach { add(it) }

    fun remove(id: Int): Boolean {
        val positionToRemove = removePositionFromIndex(id)

        updateAreaAndDirection(positionToRemove.prev, positionToRemove, true)
        updateAreaAndDirection(positionToRemove, positionToRemove.next, true)
        forceDirectionComputation()

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
        val current = requireNotNull(positionsIndex[id]) {
            "Position with id $id not found"
        }
        val addPosition = newPosition.xfer().apply {
            next = current.next
            prev = current
            current.next = this
            next?.prev = this
        }

        indexNewPosition(addPosition)
        if (current == tail) {
            tail = addPosition
        }

        updateBoundingBoxForPosition(addPosition)
        forceDirectionComputation()
        return addPosition
    }

    fun insertBefore(newPosition: Position, id: Int): Position {
        val current = requireNotNull(positionsIndex[id]) { "Position with id $id not found" }
        val addPosition = newPosition.xfer().apply {
            next = current
            prev = current.prev
            current.prev = this

            if (current == head) head = this
            else prev?.next = this
        }

        indexNewPosition(addPosition)
        updateBoundingBoxForPosition(addPosition)
        forceDirectionComputation()
        return addPosition
    }

    fun removePositionsUpTo(pointId: Int): Boolean {
        // Check if the polygon is empty or if the position id doesn't exist
        require(isNotEmpty() && positionsIndex.containsKey(pointId)) { return false }

        // If toCut is the head, nothing to delete
        if (head?.id == pointId) {
            return true
        }

        var current = head
        while (current != null && current.id != pointId) {
            val next = current.next
            remove(current.id)
            current = next
        }

        // Update the head to be the point
        head = current
        head?.prev = null

        updateBoundingBox()
        forceDirectionComputation()
        return true
    }

    fun pop(): Position? {
        val last = requireNotNull(tail) { return null }
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
            val result = requireNotNull(current) { throw NoSuchElementException() }
            current = current?.next
            return result
        }
    }

    fun loopIterator(): LoopIterator<Position> = object : LoopIterator<Position> {
        private var current = head
        override fun hasNext(): Boolean = head != null
        override fun viewCurrent(): Position = requireNotNull(current ?: head) { NoSuchElementException() }
        override fun next(): Position {
            val result = requireNotNull(current) { throw NoSuchElementException() }
            current = current?.next ?: head
            return result
        }
    }

    fun reverseIterator(): Iterator<Position> = object : Iterator<Position> {
        private var current = tail
        override fun hasNext(): Boolean = current != null
        override fun next(): Position {
            val result = requireNotNull(current) { throw NoSuchElementException() }
            current = current?.prev
            return result
        }
    }

    fun reverseLoopIterator(): LoopIterator<Position> = object : LoopIterator<Position> {
        private var current = tail
        override fun hasNext(): Boolean = tail != null
        override fun viewCurrent(): Position = requireNotNull(current ?: tail) { throw NoSuchElementException() }
        override fun next(): Position {
            val result = requireNotNull(current) { throw NoSuchElementException() }
            current = current?.prev ?: tail
            return result
        }
    }

    fun cutIterator() = cutPositions.iterator()

    // -------------------------------

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

    fun isClosed(): Boolean = isEmpty() || first() == last()
    fun first(): Position? = head
    fun last(): Position? = tail
    fun search(current: Position): Position? = this.find { it == current }
    val size: Int get() = positionsIndex.size
    val cutSize: Int get() = cutPositions.size
    fun isEmpty(): Boolean = positionsIndex.isEmpty()
    fun isCutEmpty(): Boolean = cutPositions.isEmpty()
    fun isNotEmpty(): Boolean = positionsIndex.isNotEmpty()
    fun isNotCutEmpty(): Boolean = cutPositions.isNotEmpty()
    fun getPosition(id: Int): Position? = positionsIndex[id]

    // --------------------------------

    override fun toString(): String {
        val maxPointsToShow = 75
        val pointsString = take(maxPointsToShow).joinToString(", ") { "(${it.lat}, ${it.lng})" }
        val pointsDisplay = if (size > maxPointsToShow) "$pointsString, ..." else pointsString
        val closedStatus = if (isNotEmpty()) ", closed=${first() == last()}" else ""
        val cutIdStatus = if (this is CutPolygon) ", cutId=$cutId" else ""

        return "Polygon(size=$size$closedStatus$cutIdStatus, points=[$pointsDisplay])"
    }

}

// -- Additional type preservation Polygon methods ----------

fun <T: Polygon> T.subList(start: Position, lastId: Int) = createNew().apply {
    require(!this@subList.isEmpty()) { "Polygon subList: 'start' cannot be found in an empty polygon" }
    require(this@subList.positionsIndex.containsKey(lastId)) { "Polygon subList: 'lastId' cannot be found in the polygon" }

    if (start.id == lastId) add(start).also { return@apply } // start == end

    var current = start
    do {
        add(current)
        current = current.next ?: this@subList.first()!!
        require(current.id != start.id) { "Polygon subList: 'last' cannot be found in the polygon" }
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

fun <T: Polygon> T.inverted(): Polygon = createNew().apply {
    this@inverted.reverseIterator().forEach { add(it) }
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
        add(first()!!.detached())
    }
    return this
}
