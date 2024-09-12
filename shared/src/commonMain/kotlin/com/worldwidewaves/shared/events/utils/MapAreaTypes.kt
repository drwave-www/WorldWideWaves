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

import com.worldwidewaves.shared.events.utils.Position.Companion.nextId

// ----------------------------------------------------------------------------

data class Segment(val start: Position, val end: Position)

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

    internal open fun xfer() = Position(lat, lng).init() // Polygon detach / reattach
    override fun equals(other: Any?): Boolean =
        this === other || (other is Position && lat == other.lat && lng == other.lng)
    override fun toString(): String = "($lat, $lng)"
    override fun hashCode(): Int = 31 * lat.hashCode() + lng.hashCode()
}

internal fun <T : Position> T.init(): T = apply { id = nextId++ } // Can only be initialized from internal context

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

open class Polygon(position: Position? = null) : Iterable<Position> {

    internal var head: Position? = null
    internal var tail: Position? = null
    internal val cutPositions = mutableListOf<CutPosition>()
    internal val positionsIndex = mutableMapOf<Int, Position>()

    // --------------------------------

    init { position?.let { add(it) } }

    // --------------------------------

    open fun createNew() = Polygon() // Ensure the right type is created

    companion object {
        fun fromPositions(vararg positions: Position): Polygon =
            Polygon().apply { positions.forEach { add(it) } }
    }

    // --------------------------------

    fun getCutPositions(): List<CutPosition> = cutPositions

    // --------------------------------

    fun add(position: Position) : Position {
        val addPosition = position.xfer()

        positionsIndex[addPosition.id] = addPosition
        if (addPosition is CutPosition) cutPositions.add(addPosition)

        if (head == null) {
            head = addPosition
        } else {
            addPosition.prev = tail
            tail?.next = addPosition
        }

        return addPosition.apply { tail = this }
    }

    fun remove(id: Int): Boolean {
        val positionToRemove = positionsIndex.remove(id) ?: return false
        if (positionToRemove is CutPosition) cutPositions.remove(positionToRemove)

        if (positionToRemove == head) {
            head = positionToRemove.next
            if (head == null) tail = null else head!!.prev = null
            return true
        }

        positionsIndex.values.find { it.next == positionToRemove }?.apply {
            next = positionToRemove.next
            next?.prev = this
            if (positionToRemove == tail) tail = this
            return true
        }

        return false // Position not found
    }

    fun insertAfter(newPosition: Position, id: Int): Position? {
        val current = positionsIndex[id] ?: return null
        val addPosition = newPosition.xfer().apply {
            next = current.next
            prev = current
            current.next = this
            next?.prev = this
        }

        positionsIndex[addPosition.id] = addPosition
        if (addPosition is CutPosition) cutPositions.add(addPosition)
        if (current == tail) tail = addPosition

        return addPosition
    }

    fun insertBefore(newPosition: Position, id1: Int): Position? {
        val current = positionsIndex[id1] ?: return null
        val addPosition = newPosition.xfer().apply {
            next = current
            prev = current.prev
            current.prev = this
            if (current == head)
                head = this
            else
                prev?.next = this
        }

        positionsIndex[addPosition.id] = addPosition
        if (addPosition is CutPosition) cutPositions.add(addPosition)

        return addPosition
    }

    // --------------------------------

    fun subList(start: Position, lastId: Int) = createNew().apply {
        if (this@Polygon.isEmpty())
            throw IllegalArgumentException("Polygon subList: 'start' cannot be found in an empty polygon")

        var current = start
        do {
            add(current)
            current = current.next ?: this@Polygon.first()!!
            if (current.id == start.id)
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

    // --------------------------------

    operator fun plus(other: Polygon) = createNew().apply {
        this@Polygon.forEach { add(it) }
        other.forEach { add(it) }
    }

    // --------------------------------

    override fun iterator(): Iterator<Position> = object : Iterator<Position> {
        private var current = head
        override fun hasNext(): Boolean = current != null
        override fun next(): Position {
            val result = current ?: throw NoSuchElementException()
            current = current?.next
            return result
        }
    }

    fun loopIterator(): Iterator<Position> = object : Iterator<Position> {
        private var current = head
        override fun hasNext(): Boolean = head != null
        override fun next(): Position {
            val result = current ?: throw NoSuchElementException()
            current = current?.next ?: head
            return result
        }
    }

    fun cutIterator() = cutPositions.iterator()

    // --------------------------------

    open fun copy() = createNew().apply {
        this@Polygon.forEach { add(it) }
    }

    fun clear() {
        positionsIndex.clear()
        cutPositions.clear()
        head = null
        tail = null
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