package com.worldwidewaves.shared.events.utils

/*
 * Copyright 2024 DrWave
 *
 * WorldWideWaves is an ephemeral mobile app designed to orchestrate human waves through cities and countries,
 * culminating in a global wave. The project aims to transcend physical and cultural boundaries, fostering unity,
 * community, and shared human experience by leveraging real-time coordination and location-based services.
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

data class Segment(val start: Position, val end: Position)

// ----------------------------------------------------------------------------

open class Position(val lat: Double, val lng: Double, var next: Position? = null) {
    var id: Int = -1
        get() {
            if (field == -1) throw IllegalStateException("ID has not been initialized")
            return field
        }

    internal fun init() = apply { id = nextId++ }

    open operator fun component1() = lat
    open operator fun component2() = lng

    companion object {
        internal var nextId = 42
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Position) return false
        return lat == other.lat && lng == other.lng
    }

    override fun toString(): String = "($lat, $lng)"
    override fun hashCode(): Int = id.hashCode()
}

class CutPosition( // A position that has been cut
    position: Position,
    val cutLeft: Position,
    val cutRight: Position
) : Position(position.lat, position.lng)

// ----------------------------------------------------------------------------

abstract class CutPolygon(val cutId: Int) : Polygon() { // Part of a polygon after cut
    abstract override fun createNew(): CutPolygon
    abstract override fun <T : Polygon> createList(): MutableList<T>
}

class LeftCutPolygon(cutId: Int) : CutPolygon(cutId) { // Left part of a polygon after cut
    override fun createNew(): CutPolygon = LeftCutPolygon(cutId)
    override fun <T : Polygon> createList(): MutableList<T> = mutableListOf()
}

class RightCutPolygon(cutId: Int) : CutPolygon(cutId) { // Right part of a polygon after cut
    override fun createNew(): CutPolygon = RightCutPolygon(cutId)
    override fun <T : Polygon> createList(): MutableList<T> = mutableListOf()
}

// ------------------------------------

open class Polygon(position: Position? = null) : Iterable<Position> {

    private var head: Position? = null
    private var tail: Position? = null
    private val cutPositions = mutableListOf<CutPosition>()
    private val positionsIndex = mutableMapOf<Int, Position>()

    // --------------------------------

    init { position?.let { add(it) } }

    // --------------------------------

    open fun createNew(): Polygon = Polygon() // Ensure the right type is created
    open fun <T : Polygon> createList(): MutableList<T> = mutableListOf()

    // --------------------------------

    fun getCutPositions(): List<CutPosition> = cutPositions

    // --------------------------------

    fun add(position: Position) {
        val addPosition = Position(position.lat, position.lng).init()

        positionsIndex[addPosition.id] = addPosition
        if (addPosition is CutPosition) {
            cutPositions.add(addPosition)
        }
        if (head == null) {
            head = addPosition
        } else {
            tail?.next = addPosition
        }
        tail = addPosition
    }

    fun remove(id: Int): Boolean {
        val position = positionsIndex.remove(id) ?: return false
        if (position is CutPosition) {
            cutPositions.remove(position)
        }

        head = if (position == head) position.next else {
            val previous = positionsIndex.values.find { it.next?.id == id }
            previous?.next = position.next
            head
        }

        tail = positionsIndex.values.find { it.next?.id == position.id }?.apply { next = null }

        return true
    }

    fun insertAfter(newPosition: Position, id: Int): Boolean {
        val current = positionsIndex[id] ?: return false
        val addPosition = Position(newPosition.lat, newPosition.lng).init()

        addPosition.next = current.next
        current.next = addPosition

        positionsIndex[addPosition.id] = addPosition
        if (addPosition is CutPosition) {
            cutPositions.add(addPosition)
        }
        return true
    }

    fun insertBefore(newPosition: Position, id1: Int): Boolean {
        val current = positionsIndex[id1] ?: return false
        val addPosition = Position(newPosition.lat, newPosition.lng).init()

        addPosition.next = current
        if (current == head) {
            head = addPosition
        } else {
            val previous = positionsIndex.values.find { it.next?.id == id1 }
            previous?.next = addPosition
        }

        positionsIndex[addPosition.id] = addPosition
        if (addPosition is CutPosition) {
            cutPositions.add(addPosition)
        }
        return true
    }

    // --------------------------------

    fun subList(start: Position, lastId: Int): Polygon = createNew().apply {
        if (this@Polygon.isEmpty()) return this

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

    operator fun plus(other: Polygon): Polygon = createNew().apply {
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

    open fun copy(): Polygon = createNew().apply {
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

fun polygonOf(vararg positions: Position): Polygon = Polygon().apply { positions.forEach { add(it) } }

val List<Position>.toPolygon: Polygon
    get() {
        val polygon = Polygon()
        for (element in this) {
            polygon.add(element)
        }
        return polygon
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