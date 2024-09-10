package com.worldwidewaves.shared.events.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

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

class MapAreaTypesTest {

    @Test
    fun testCreateNewLeftCutPolygon() {
        val leftCutPolygon = LeftCutPolygon(1)
        val newPolygon: LeftCutPolygon = leftCutPolygon.createNew()
        assertEquals(1, newPolygon.cutId)
    }

    @Test
    fun testCreateNewRightCutPolygon() {
        val rightCutPolygon = RightCutPolygon(2)
        val newPolygon: RightCutPolygon = rightCutPolygon.createNew()
        assertEquals(2, newPolygon.cutId)
    }

    @Test
    fun testAddPosition() {
        val polygon = Polygon()
        val position = Position(1.0, 1.0)
        polygon.add(position)
        assertEquals(1, polygon.size)
        assertNotNull(polygon.first())
    }

    @Test
    fun testRemovePosition() {
        val polygon = Polygon()
        val position = Position(1.0, 1.0)
        polygon.add(position)
        assertTrue(polygon.remove(polygon.first()!!.id))
        assertEquals(0, polygon.size)
    }

    @Test
    fun testInsertAfter() {
        val polygon = Polygon()
        val position1 = Position(1.0, 1.0)
        val position2 = Position(2.0, 2.0)
        polygon.add(position1)
        assertTrue(polygon.insertAfter(position2, polygon.first()!!.id))
        assertEquals(2, polygon.size)
    }

    @Test
    fun testInsertBefore() {
        val polygon = Polygon()
        val position1 = Position(1.0, 1.0)
        val position2 = Position(2.0, 2.0)
        polygon.add(position1)
        assertTrue(polygon.insertBefore(position2, polygon.first()!!.id))
        assertEquals(2, polygon.size)
    }

    @Test
    fun testSubList() {
        val position1 = Position(1.0, 1.0)
        val position2 = Position(2.0, 2.0)
        val position3 = Position(3.0, 3.0)
        val polygon = Polygon().apply {
            add(position1)
            add(position2)
            add(position3)
        }
        val subPolygon: Polygon = polygon.subList(polygon.first()!!, polygon.last()!!.id)
        assertEquals(2, subPolygon.size)
    }

    @Test
    fun testDropLast() {
        val position1 = Position(1.0, 1.0)
        val position2 = Position(2.0, 2.0)
        val polygon = Polygon().apply {
            add(position1)
            add(position2)
        }
        val newPolygon: Polygon = polygon.dropLast()
        assertEquals(1, newPolygon.size)
    }

    @Test
    fun testPlusOperator() {
        val position1 = Position(1.0, 1.0)
        val position2 = Position(2.0, 2.0)
        val polygon1 = Polygon().apply { add(position1) }
        val polygon2 = Polygon().apply { add(position2) }
        val combinedPolygon = polygon1 + polygon2
        assertEquals(2, combinedPolygon.size)
    }

    @Test
    fun testCopy() {
        val position = Position(1.0, 1.0)
        val polygon = Polygon().apply { add(position) }
        val copiedPolygon: Polygon = polygon.copy()
        assertEquals(1, copiedPolygon.size)
        assertNotNull(copiedPolygon.first())
    }

    @Test
    fun testClear() {
        val position = Position(1.0, 1.0)
        val polygon = Polygon().apply { add(position) }
        polygon.clear()
        assertEquals(0, polygon.size)
    }

    @Test
    fun testListToPolygon() {
        val positions = listOf(
            Position(1.0, 1.0),
            Position(2.0, 2.0),
            Position(3.0, 3.0)
        )
        val polygon = positions.toPolygon
        assertEquals(positions.size, polygon.size)
        val iterator = polygon.iterator()
        positions.forEach { position ->
            val polyPosition = iterator.next()
            assertEquals(position.lat, polyPosition.lat)
            assertEquals(position.lng, polyPosition.lng)
        }
    }

    @Test
    fun testListToLeftPolygon() {
        val positions = listOf(
            Position(1.0, 1.0),
            Position(2.0, 2.0),
            Position(3.0, 3.0)
        )
        val cutId = 1
        val leftCutPolygon = positions.toLeftPolygon(cutId)
        assertEquals(cutId, leftCutPolygon.cutId)
        assertEquals(positions.size, leftCutPolygon.size)
        val iterator = leftCutPolygon.iterator()
        positions.forEach { position ->
            val polyPosition = iterator.next()
            assertEquals(position.lat, polyPosition.lat)
            assertEquals(position.lng, polyPosition.lng)
        }
    }

    @Test
    fun testListToRightPolygon() {
        val positions = listOf(
            Position(1.0, 1.0),
            Position(2.0, 2.0),
            Position(3.0, 3.0)
        )
        val cutId = 2
        val rightCutPolygon = positions.toRightPolygon(cutId)
        assertEquals(cutId, rightCutPolygon.cutId)
        assertEquals(positions.size, rightCutPolygon.size)
        val iterator = rightCutPolygon.iterator()
        positions.forEach { position ->
            val polyPosition = iterator.next()
            assertEquals(position.lat, polyPosition.lat)
            assertEquals(position.lng, polyPosition.lng)
        }
    }

    @Test
    fun testCutPositionInitialization() {
        val position = Position(1.0, 1.0)
        val cutLeft = Position(0.0, 0.0)
        val cutRight = Position(2.0, 2.0)
        val cutPosition = CutPosition(position, cutLeft, cutRight)

        assertEquals(position.lat, cutPosition.lat)
        assertEquals(position.lng, cutPosition.lng)
        assertEquals(cutLeft, cutPosition.cutLeft)
        assertEquals(cutRight, cutPosition.cutRight)
    }

    @Test
    fun testCutPositionEquality() {
        val position = Position(1.0, 1.0)
        val cutLeft = Position(0.0, 0.0)
        val cutRight = Position(2.0, 2.0)
        val cutPosition1 = CutPosition(position, cutLeft, cutRight)
        val cutPosition2 = CutPosition(position, cutLeft, cutRight)

        assertEquals(cutPosition1, cutPosition2)
    }

    @Test
    fun testPositionInitialization() {
        val position = Position(1.0, 1.0).init()
        assertEquals(1.0, position.lat)
        assertEquals(1.0, position.lng)
        assertTrue(position.id >= 42)
    }

    @Test
    fun testPositionIdUninitialized() {
        val position = Position(1.0, 1.0)
        assertFailsWith<IllegalStateException> { position.id }
    }

    @Test
    fun testPositionEquality() {
        val position1 = Position(1.0, 1.0).init()
        val position2 = Position(1.0, 1.0).init()
        assertEquals(position1, position2)
    }

    @Test
    fun testPositionToString() {
        val position = Position(1.0, 1.0)
        assertEquals("(1.0, 1.0)", position.toString())
    }

    @Test
    fun testLeftCutPolygonInitialization() {
        val cutId = 1
        val leftCutPolygon = LeftCutPolygon(cutId)
        assertEquals(cutId, leftCutPolygon.cutId)
    }

    @Test
    fun testRightCutPolygonInitialization() {
        val cutId = 2
        val rightCutPolygon = RightCutPolygon(cutId)
        assertEquals(cutId, rightCutPolygon.cutId)
    }

    @Test
    fun testLeftCutPolygonCreateNew() {
        val cutId = 1
        val leftCutPolygon = LeftCutPolygon(cutId)
        val newPolygon: LeftCutPolygon = leftCutPolygon.createNew()
        assertEquals(cutId, newPolygon.cutId)
    }

    @Test
    fun testRightCutPolygonCreateNew() {
        val cutId = 2
        val rightCutPolygon = RightCutPolygon(cutId)
        val newPolygon: RightCutPolygon = rightCutPolygon.createNew()
        assertEquals(cutId, newPolygon.cutId)
    }

    @Test
    fun testLeftCutPolygonCreateList() {
        val leftCutPolygon = LeftCutPolygon(1)
        val list: MutableList<LeftCutPolygon> = leftCutPolygon.createList()
        assertTrue(list.isEmpty())
    }

    @Test
    fun testRightCutPolygonCreateList() {
        val rightCutPolygon = RightCutPolygon(2)
        val list: MutableList<RightCutPolygon> = rightCutPolygon.createList()
        assertTrue(list.isEmpty())
    }

    @Test
    fun testLeftCutPolygonConvert() {
        val polygon = Polygon().apply {
            add(Position(1.0, 1.0))
            add(Position(2.0, 2.0))
        }
        val cutId = 1
        val leftCutPolygon = LeftCutPolygon.convert(polygon, cutId)
        assertEquals(cutId, leftCutPolygon.cutId)
        assertEquals(polygon.size, leftCutPolygon.size)
    }

    @Test
    fun testRightCutPolygonConvert() {
        val polygon = Polygon().apply {
            add(Position(1.0, 1.0))
            add(Position(2.0, 2.0))
        }
        val cutId = 2
        val rightCutPolygon = RightCutPolygon.convert(polygon, cutId)
        assertEquals(cutId, rightCutPolygon.cutId)
        assertEquals(polygon.size, rightCutPolygon.size)
    }

    @Test
    fun testBoundingBoxInitialization() {
        val sw = Position(0.0, 0.0)
        val ne = Position(1.0, 1.0)
        val bbox = BoundingBox(sw, ne)
        assertEquals(sw, bbox.sw)
        assertEquals(ne, bbox.ne)
    }

    @Test
    fun testBoundingBoxInitializationWithCoordinates() {
        val bbox = BoundingBox(0.0, 0.0, 1.0, 1.0)
        assertEquals(0.0, bbox.sw.lat)
        assertEquals(0.0, bbox.sw.lng)
        assertEquals(1.0, bbox.ne.lat)
        assertEquals(1.0, bbox.ne.lng)
    }

    @Test
    fun testBoundingBoxProperties() {
        val bbox = BoundingBox(0.0, 0.0, 1.0, 1.0)
        assertEquals(0.0, bbox.minLatitude)
        assertEquals(1.0, bbox.maxLatitude)
        assertEquals(0.0, bbox.minLongitude)
        assertEquals(1.0, bbox.maxLongitude)
    }

    @Test
    fun testBoundingBoxEquals() {
        val bbox1 = BoundingBox(0.0, 0.0, 1.0, 1.0)
        val bbox2 = BoundingBox(0.0, 0.0, 1.0, 1.0)
        val bbox3 = BoundingBox(0.0, 0.0, 2.0, 2.0)
        assertEquals(bbox1, bbox2)
        assertNotEquals(bbox1, bbox3)
    }

    @Test
    fun testBoundingBoxHashCode() {
        val bbox1 = BoundingBox(0.0, 0.0, 1.0, 1.0)
        val bbox2 = BoundingBox(0.0, 0.0, 1.0, 1.0)
        val bbox3 = BoundingBox(0.0, 0.0, 2.0, 2.0)
        assertEquals(bbox1.hashCode(), bbox2.hashCode())
        assertNotEquals(bbox1.hashCode(), bbox3.hashCode())
    }

    @Test
    fun testAddInvalidPosition() {
        val polygon = Polygon()
        val invalidPosition = Position(Double.NaN, Double.NaN)
        assertFailsWith<IllegalArgumentException> {
            polygon.add(invalidPosition)
        }
    }

    @Test
    fun testRemoveNonExistentPosition() {
        val polygon = Polygon()
        val position = Position(1.0, 1.0).init()
        polygon.add(position)
        assertTrue(polygon.remove(position.id))
        assertFalse(polygon.remove(position.id)) // Try removing again
    }

    @Test
    fun testInsertAfterNonExistentPosition() {
        val polygon = Polygon()
        val position = Position(1.0, 1.0).init()
        polygon.add(position)
        val newPosition = Position(2.0, 2.0)
        assertFalse(polygon.insertAfter(newPosition, -1)) // Invalid ID
    }

    @Test
    fun testInsertBeforeNonExistentPosition() {
        val polygon = Polygon()
        val position = Position(1.0, 1.0).init()
        polygon.add(position)
        val newPosition = Position(2.0, 2.0)
        assertFalse(polygon.insertBefore(newPosition, -1)) // Invalid ID
    }

    @Test
    fun testAddDuplicatePositions() {
        val polygon = Polygon()
        val position = Position(1.0, 1.0).init()
        polygon.add(position)
        polygon.add(position) // Add the same position again
        assertEquals(1, polygon.size) // Size should still be 1
    }

    @Test
    fun testEmptyPolygonOperations() {
        val polygon = Polygon()
        assertTrue(polygon.isEmpty())
        assertFailsWith<NoSuchElementException> {
            polygon.iterator().next()
        }
        assertFailsWith<IllegalArgumentException> {
            polygon.subList<Polygon>(Position(0.0, 0.0), -1)
        }
    }

}