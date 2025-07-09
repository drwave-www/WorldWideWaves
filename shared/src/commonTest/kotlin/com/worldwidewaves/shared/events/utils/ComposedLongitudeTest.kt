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

import com.worldwidewaves.shared.events.utils.ComposedLongitude.Orientation
import com.worldwidewaves.shared.events.utils.ComposedLongitude.Side
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ComposedLongitudeTest {

    @Test
    fun testComposedLongitudeAdd() {
        val composedLongitude = ComposedLongitude()
        val position = Position(1.0, 1.0)
        composedLongitude.add(position)
        assertEquals(1, composedLongitude.getPositions().size)
        assertEquals(position, composedLongitude.getPositions().first())
    }

    @Test
    fun testComposedLongitudeAddAll() {
        val composedLongitude = ComposedLongitude()
        val positions = listOf(Position(1.0, 1.0), Position(2.0, 2.0))
        composedLongitude.addAll(positions)
        assertEquals(2, composedLongitude.getPositions().size)
        assertEquals(positions.map { it }, composedLongitude.getPositions())
    }

    @Test
    fun testComposedLongitudeIsPointOnLine() {
        val composedLongitude = ComposedLongitude()
        val position1 = Position(1.0, 1.0)
        val position2 = Position(2.0, 2.0)
        composedLongitude.add(position1)
        composedLongitude.add(position2)
        assertEquals(Side.ON, composedLongitude.isPointOnLine(Position(1.5, 1.5)))
        assertEquals(Side.EAST, composedLongitude.isPointOnLine(Position(3.0, 3.0)))
        assertEquals(Side.WEST, composedLongitude.isPointOnLine(Position(2.0, -1.0)))
    }

    @Test
    fun testComposedLongitudeIsPointOnLine2() {
        val composedLongitude = ComposedLongitude.fromLongitude(-3.0)
        assertEquals(Side.ON, composedLongitude.isPointOnLine(Position(1.5, -3.0)))
        assertEquals(Side.EAST, composedLongitude.isPointOnLine(Position(3.0, 3.0)))
        assertEquals(Side.WEST, composedLongitude.isPointOnLine(Position(2.0, -6.0)))
    }

    @Test
    fun testComposedLongitudeIntersectWithSegment() {
        val composedLongitude = ComposedLongitude()
        val position1 = Position(1.0, 1.0)
        val position2 = Position(2.0, 2.0)
        composedLongitude.add(position1)
        composedLongitude.add(position2)
        val segment = Segment(Position(0.0, 3.0), Position(3.0, 0.0))
        val cutPosition = composedLongitude.intersectWithSegment(1, segment)
        assertNotNull(cutPosition)
        assertEquals(1.5, cutPosition.lat)
        assertEquals(1.5, cutPosition.lng)
        assertEquals(1, cutPosition.cutId)
    }

    @Test
    fun testComposedLongitudeIntersectWithSegment2() {
        val composedLongitude = ComposedLongitude.fromPositions(
            Position(lat = -3.0, lng = -1.0),
            Position(lat = 1.0, lng = -1.0),
            Position(lat = 3.0, lng = 1.0)
        )
        val segment = Segment(Position(-2.0, -2.0), Position(-2.0, 2.0))
        val cutPosition = composedLongitude.intersectWithSegment(1, segment)
        assertNotNull(cutPosition)
        assertEquals(-2.0, cutPosition.lat)
        assertEquals(-1.0, cutPosition.lng)
    }

    @Test
    fun testComposedLongitudeIsValidArc() {
        val composedLongitude = ComposedLongitude()
        val positions = listOf(Position(1.0, 1.0), Position(2.0, 2.0), Position(3.0, 3.0))
        composedLongitude.addAll(positions)
        assertTrue(composedLongitude.isValidArc())

        composedLongitude.add(Position(4.0, 1.0))
        assertTrue(composedLongitude.isValidArc())

        composedLongitude.add(Position(5.0, 3.0))
        assertTrue(composedLongitude.isValidArc()) // This should be valid for wave fronts

        // Test a truly chaotic pattern that should be invalid
        val chaoticPositions = listOf(
            Position(1.0, 1.0), Position(2.0, 10.0), Position(3.0, -5.0),
            Position(4.0, 15.0), Position(5.0, -10.0), Position(6.0, 20.0),
            Position(7.0, -15.0)
        )
        assertFailsWith<IllegalArgumentException> {
            ComposedLongitude().addAll(chaoticPositions)
        }
    }

    @Test
    fun testComposedLongitudeSortPositions() {
        val composedLongitude = ComposedLongitude()
        val positions = listOf(Position(2.0, 2.0), Position(1.0, 1.0))
        composedLongitude.addAll(positions)
        assertEquals(listOf(Position(2.0, 2.0), Position(1.0, 1.0)), composedLongitude.getPositions())
        assertEquals(Orientation.SOUTH, composedLongitude.orientation)
    }

    @Test
    fun testComposedLongitudeSortPositions2() {
        val composedLongitude = ComposedLongitude()
        val positions = listOf(Position(1.0, 1.0), Position(2.0, 2.0))
        composedLongitude.addAll(positions)
        assertEquals(listOf(Position(1.0, 1.0), Position(2.0, 2.0)), composedLongitude.getPositions())
        assertEquals(Orientation.NORTH, composedLongitude.orientation)
    }

    @Test
    fun testComposedLongitudeIterator() {
        val composedLongitude = ComposedLongitude()
        val positions = listOf(Position(1.0, 1.0), Position(2.0, 2.0))
        composedLongitude.addAll(positions)
        val iterator = composedLongitude.iterator()
        assertTrue(iterator.hasNext())
        assertEquals(Position(1.0, 1.0), iterator.next())
        assertTrue(iterator.hasNext())
        assertEquals(Position(2.0, 2.0), iterator.next())
        assertFalse(iterator.hasNext())
    }

    @Test
    fun testComposedLongitudeReverseIterator() {
        val composedLongitude = ComposedLongitude()
        val positions = listOf(Position(1.0, 1.0), Position(2.0, 2.0))
        composedLongitude.addAll(positions)
        val reverseIterator = composedLongitude.reverseIterator()
        assertTrue(reverseIterator.hasNext())
        assertEquals(Position(2.0, 2.0), reverseIterator.next())
        assertTrue(reverseIterator.hasNext())
        assertEquals(Position(1.0, 1.0), reverseIterator.next())
        assertFalse(reverseIterator.hasNext())
    }

    @Test
    fun testComposedLongitudeIsValidArcWithTwoPositions() {
        val composedLongitude = ComposedLongitude()
        val positions = listOf(Position(1.0, 1.0), Position(2.0, 2.0))
        composedLongitude.addAll(positions)
        assertTrue(composedLongitude.isValidArc())
    }

    @Test
    fun testComposedLongitudeIsValidArcWithSameLongitude() {
        val composedLongitude = ComposedLongitude()
        val positions = listOf(Position(1.0, 1.0), Position(2.0, 1.0), Position(3.0, 1.0))
        composedLongitude.addAll(positions)
        assertTrue(composedLongitude.isValidArc())
    }

    @Test
    fun testComposedLongitudeIteratorEmpty() {
        val composedLongitude = ComposedLongitude()
        val iterator = composedLongitude.iterator()
        assertFalse(iterator.hasNext())
    }

    @Test
    fun testComposedLongitudeDirectionChange() {
        val composedLongitude = ComposedLongitude()
        val positions = listOf(Position(2.0, 2.0), Position(1.0, 1.0))
        composedLongitude.addAll(positions)
        assertEquals(Orientation.SOUTH, composedLongitude.orientation)
    }

}