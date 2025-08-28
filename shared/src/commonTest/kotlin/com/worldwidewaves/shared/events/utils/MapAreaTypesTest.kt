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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MapAreaTypesTest {


    @Test
    fun testCutPositionInitialization() {
        val position = Position(1.0, 1.0)
        val cutLeft = Position(0.0, 0.0)
        val cutRight = Position(2.0, 2.0)
        val cutPosition = position.toCutPosition(42, cutLeft, cutRight)

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
        val cutPosition1 = position.toCutPosition(42, cutLeft, cutRight)
        val cutPosition2 = position.toCutPosition(42, cutLeft, cutRight)

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
    fun testCutPositionHashCode() {
        val position = Position(1.0, 1.0)
        val cutLeft = Position(0.0, 0.0)
        val cutRight = Position(2.0, 2.0)
        val cutPosition1 = position.toCutPosition(42, cutLeft, cutRight)
        val cutPosition2 = position.toCutPosition(42, cutLeft, cutRight)
        val cutPosition3 = position.toCutPosition(43, cutLeft, cutRight)

        assertEquals(cutPosition1.hashCode(), cutPosition2.hashCode())
        assertNotEquals(cutPosition1.hashCode(), cutPosition3.hashCode())
    }

    @Test
    fun testSegmentIntersection() {
        val segment = Segment(Position(0.0, 0.0), Position(2.0, 2.0))
        val cutPosition = segment.intersectWithLng(1, 1.0)
        assertNotNull(cutPosition)
        assertEquals(1.0, cutPosition.lat)
        assertEquals(1.0, cutPosition.lng)
        assertEquals(1, cutPosition.cutId)

        // Test no intersection
        val noIntersectSegment = Segment(Position(0.0, 0.0), Position(2.0, 0.0))
        assertNull(noIntersectSegment.intersectWithLng(1, 1.0))
    }

}