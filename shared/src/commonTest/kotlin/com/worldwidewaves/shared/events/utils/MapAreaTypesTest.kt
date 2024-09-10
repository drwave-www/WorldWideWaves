package com.worldwidewaves.shared.events.utils

import kotlin.test.Test
import kotlin.test.assertEquals
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
        val newPolygon = leftCutPolygon.createNew()
        assertTrue(newPolygon is LeftCutPolygon)
        assertEquals(1, newPolygon.cutId)
    }

    @Test
    fun testCreateNewRightCutPolygon() {
        val rightCutPolygon = RightCutPolygon(2)
        val newPolygon = rightCutPolygon.createNew()
        assertTrue(newPolygon is RightCutPolygon)
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
        val subPolygon = polygon.subList(polygon.first()!!, polygon.last()!!.id)
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
        val newPolygon = polygon.dropLast()
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
        val copiedPolygon = polygon.copy()
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



}