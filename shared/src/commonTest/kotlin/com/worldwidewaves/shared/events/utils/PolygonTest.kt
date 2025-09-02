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

import com.worldwidewaves.shared.events.utils.PolygonUtils.toPolygon
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@Suppress("VisibleForTests")
class PolygonTest {

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
        assertNotNull(polygon.insertAfter(position2, polygon.first()!!.id))
        assertEquals(2, polygon.size)
    }

    @Test
    fun testInsertBefore() {
        val polygon = Polygon()
        val position1 = Position(1.0, 1.0)
        val position2 = Position(2.0, 2.0)
        polygon.add(position1)
        assertNotNull(polygon.insertBefore(position2, polygon.first()!!.id))
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
        val newPolygon: Polygon = polygon.withoutLast()
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
        val copiedPolygon: Polygon = polygon.move()
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
    fun testRemoveNonExistentPosition() {
        val polygon = Polygon()
        var position = Position(1.0, 1.0).init()
        position = polygon.add(position)
        assertTrue(polygon.remove(position.id))
        assertFailsWith<IllegalArgumentException> { polygon.remove(position.id) } // Try removing again
    }

    @Test
    fun testInsertAfterNonExistentPosition() {
        val polygon = Polygon()
        val position = Position(1.0, 1.0).init()
        polygon.add(position)
        val newPosition = Position(2.0, 2.0)
        assertFailsWith<IllegalArgumentException> { polygon.insertAfter(newPosition, -1) } // Invalid ID
    }

    @Test
    fun testInsertBeforeNonExistentPosition() {
        val polygon = Polygon()
        val position = Position(1.0, 1.0).init()
        polygon.add(position)
        val newPosition = Position(2.0, 2.0)
        assertFailsWith<IllegalArgumentException> { polygon.insertBefore(newPosition, -1) } // Invalid ID
    }

    @Test
    fun testAddImmediateDuplicatePositions() {
        val polygon = Polygon()
        val position = Position(1.0, 1.0).init()
        polygon.add(position)
        polygon.add(position) // Add the same position again
        assertEquals(1, polygon.size) // Size should be 1
    }

    @Test
    fun testEmptyPolygonOperations() {
        val polygon = Polygon()
        assertTrue(polygon.isEmpty())
        assertFailsWith<NoSuchElementException> {
            polygon.iterator().next()
        }
        assertFailsWith<IllegalArgumentException> {
            polygon.subList(Position(0.0, 0.0), -1)
        }
    }

    @Test
    fun testLoopIterator() {
        val position1 = Position(1.0, 1.0)
        val position2 = Position(2.0, 2.0)
        val polygon = Polygon().apply {
            add(position1)
            add(position2)
        }
        val iterator = polygon.loopIterator()
        assertTrue(iterator.hasNext())
        assertEquals(position1, iterator.next())
        assertTrue(iterator.hasNext())
        assertEquals(position2, iterator.next())
        assertTrue(iterator.hasNext())
        assertEquals(position1, iterator.next()) // Loops back to the start
    }

    @Test
    fun testCutIterator() {
        val cutPosition1 = Position(1.0, 1.0).toCutPosition(1, Position(0.0, 0.0), Position(2.0, 2.0))
        val cutPosition2 = Position(2.0, 2.0).toCutPosition(2, Position(1.0, 1.0), Position(3.0, 3.0))
        val polygon = Polygon().apply {
            add(cutPosition1)
            add(cutPosition2)
        }
        val iterator = polygon.cutIterator()
        assertTrue(iterator.hasNext())
        assertEquals(cutPosition1, iterator.next())
        assertTrue(iterator.hasNext())
        assertEquals(cutPosition2, iterator.next())
    }

    @Test
    fun testFirst() {
        val position = Position(1.0, 1.0)
        val polygon = Polygon().apply { add(position) }
        assertEquals(position, polygon.first())
    }

    @Test
    fun testLast() {
        val position = Position(1.0, 1.0)
        val polygon = Polygon().apply { add(position) }
        assertEquals(position, polygon.last())
    }

    @Test
    fun testSize() {
        val position1 = Position(1.0, 1.0)
        val position2 = Position(2.0, 2.0)
        val polygon = Polygon().apply {
            add(position1)
            add(position2)
        }
        assertEquals(2, polygon.size)
    }

    @Test
    fun testCutSize() {
        val cutPosition1 = Position(1.0, 1.0).toCutPosition(1, Position(0.0, 0.0), Position(2.0, 2.0))
        val cutPosition2 = Position(2.0, 2.0).toCutPosition(2, Position(1.0, 1.0), Position(3.0, 3.0))
        val polygon = Polygon().apply {
            add(cutPosition1)
            add(cutPosition2)
        }
        assertEquals(2, polygon.cutSize)
    }

    @Test
    fun testIsEmpty() {
        val polygon = Polygon()
        assertTrue(polygon.isEmpty())
    }

    @Test
    fun testIsCutEmpty() {
        val polygon = Polygon()
        assertTrue(polygon.isCutEmpty())
    }

    @Test
    fun testIsNotEmpty() {
        val position = Position(1.0, 1.0)
        val polygon = Polygon().apply { add(position) }
        assertTrue(polygon.isNotEmpty())
    }

    @Test
    fun testIsNotCutEmpty() {
        val cutPosition = Position(1.0, 1.0).toCutPosition(1, Position(0.0, 0.0), Position(2.0, 2.0))
        val polygon = Polygon().apply { add(cutPosition) }
        assertTrue(polygon.isNotCutEmpty())
    }

    @Test
    fun testPolygonIteratorEdgeCases() {
        val emptyPolygon = Polygon()
        val emptyIterator = emptyPolygon.iterator()
        assertFalse(emptyIterator.hasNext())
        assertFailsWith<NoSuchElementException> { emptyIterator.next() }

        val singleElementPolygon = Polygon()
        singleElementPolygon.add(Position(1.0, 1.0))
        val singleIterator = singleElementPolygon.iterator()
        assertTrue(singleIterator.hasNext())
        assertNotNull(singleIterator.next())
        assertFalse(singleIterator.hasNext())
    }

    @Test
    fun testPolygonLoopIterator() {
        val polygon = Polygon()
        polygon.add(Position(1.0, 1.0))
        polygon.add(Position(2.0, 2.0))
        polygon.add(Position(3.0, 3.0))

        val loopIterator = polygon.loopIterator()
        repeat(6) { // Test two full loops
            assertTrue(loopIterator.hasNext())
            assertNotNull(loopIterator.next())
        }
    }

    @Test
    fun testPolygonSubListEdgeCases() {
        val polygon = Polygon()
        val pos1 = polygon.add(Position(1.0, 1.0))
        polygon.add(Position(2.0, 2.0))
        val pos3 = polygon.add(Position(3.0, 3.0))

        // Test subList with start and end being the same
        val singleElementSubList = polygon.subList(pos1, pos1.id)
        assertEquals(1, singleElementSubList.size)

        // Test subList with full polygon (last one is excluded)
        val fullSubList = polygon.subList(pos1, pos3.id)
        assertEquals(2, fullSubList.size)

        // Test invalid subList (non-existent lastId)
        assertFailsWith<IllegalArgumentException> {
            polygon.subList(pos1, -1)
        }

        // Test subList with empty polygon
        val emptyPolygon = Polygon()
        assertFailsWith<IllegalArgumentException> {
            emptyPolygon.subList(pos1, pos1.id)
        }

        // Test subList where 'last' cannot be found (cyclic case)
        val cyclicPolygon = Polygon()
        val cyclicPos = cyclicPolygon.add(Position(1.0, 1.0))
        cyclicPos.next = cyclicPos // Create a cycle
        assertFailsWith<IllegalArgumentException> {
            cyclicPolygon.subList(cyclicPos, -1) // Any non-existing ID to force full loop
        }
    }

    @Test
    fun testCutPositionPairId() {
        val polygon = Polygon()
        val cutLeft = polygon.add(Position(0.0, 0.0))
        val cutRight = polygon.add(Position(2.0, 2.0))
        val cutPos1 = polygon.add(Position(1.0, 1.0).toCutPosition(1, cutLeft, cutRight)) as CutPosition
        val cutPos2 = polygon.add(Position(1.0, 1.0).toCutPosition(1, cutLeft, cutRight)) as CutPosition
        val cutPos3 = polygon.add(Position(1.5, 1.5).toCutPosition(2, cutLeft, cutRight)) as CutPosition

        assertEquals(cutPos1.pairId, cutPos2.pairId)
        assertNotEquals(cutPos1.pairId, cutPos3.pairId)
    }

    @Test
    fun testBoundingBoxCreationAndMethods() {
        val bbox = BoundingBox(0.0, 0.0, 2.0, 2.0)
        assertEquals(0.0, bbox.minLatitude)
        assertEquals(2.0, bbox.maxLatitude)
        assertEquals(0.0, bbox.minLongitude)
        assertEquals(2.0, bbox.maxLongitude)

        // Test equality
        val sameBbox = BoundingBox(0.0, 0.0, 2.0, 2.0)
        assertEquals(bbox, sameBbox)
        assertNotEquals(bbox, BoundingBox(0.0, 0.0, 3.0, 3.0))

        // Test hashCode
        assertEquals(bbox.hashCode(), sameBbox.hashCode())
    }

    @Test
    fun testPolygonOperationsWithCutPositions() {
        val polygon = Polygon()
        val cutLeft = Position(0.0, 0.0)
        val cutRight = Position(2.0, 2.0)
        val cutPos1 = polygon.add(Position(1.0, 1.0).toCutPosition(1, cutLeft, cutRight))
        val cutPos2 = polygon.add(Position(1.5, 1.5).toCutPosition(2, cutLeft, cutRight))

        assertEquals(2, polygon.size)
        assertEquals(2, polygon.cutSize)

        assertTrue(polygon.remove(cutPos1.id))
        assertEquals(1, polygon.size)
        assertEquals(1, polygon.cutSize)

        val cutIterator = polygon.cutIterator()
        assertTrue(cutIterator.hasNext())
        assertEquals(cutPos2, cutIterator.next())
    }

    @Test
    fun testPolygonBoundingBoxSinglePosition() {
        val polygon = Polygon(Position(1.0, 1.0))
        val bbox = polygon.bbox()
        assertEquals(Position(1.0, 1.0), bbox.sw)
        assertEquals(Position(1.0, 1.0), bbox.ne)
    }

    @Test
    fun testPolygonBoundingBoxMultiplePositions() {
        val polygon = Polygon()
        polygon.add(Position(1.0, 1.0))
        polygon.add(Position(2.0, 2.0))
        val bbox = polygon.bbox()
        assertEquals(Position(1.0, 1.0), bbox.sw)
        assertEquals(Position(2.0, 2.0), bbox.ne)
    }

    @Test
    fun testPolygonAreaAndDirectionSinglePosition() {
        val polygon = Polygon(Position(1.0, 1.0))
        assertTrue(polygon.isClockwise())
    }

    @Test
    fun testPolygonAreaAndDirectionMultiplePositions() {
        val polygon = Polygon()
        polygon.add(Position(1.0, 1.0))
        polygon.add(Position(2.0, 2.0))
        polygon.add(Position(3.0, 1.0))
        polygon.forceDirectionComputation()
        assertFalse(polygon.isClockwise())
    }

    @Test
    fun testPolygonAddToEmpty() {
        val polygon = Polygon()
        polygon.add(Position(1.0, 1.0))
        assertEquals(1, polygon.size)
    }

    @Test
    fun testPolygonInsertAfter() {
        val polygon = Polygon()
        val pos1 = polygon.add(Position(1.0, 1.0))
        val pos2 = polygon.insertAfter(Position(2.0, 2.0), pos1.id)
        assertEquals(pos2, pos1.next)
    }

    @Test
    fun testPolygonInsertBefore() {
        val polygon = Polygon()
        val pos1 = polygon.add(Position(1.0, 1.0))
        val pos2 = polygon.insertBefore(Position(0.0, 0.0), pos1.id)
        assertEquals(pos2, pos1.prev)
    }

}