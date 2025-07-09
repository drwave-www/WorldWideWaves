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

import com.worldwidewaves.shared.events.utils.GeoUtils.EPSILON
import com.worldwidewaves.shared.events.utils.GeoUtils.isLatitudeInRange
import com.worldwidewaves.shared.events.utils.GeoUtils.isLongitudeEqual
import com.worldwidewaves.shared.events.utils.GeoUtils.isLongitudeInRange
import com.worldwidewaves.shared.events.utils.GeoUtils.isPointOnSegment
import com.worldwidewaves.shared.events.utils.GeoUtils.toDegrees
import com.worldwidewaves.shared.events.utils.GeoUtils.toRadians
import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GeoUtilsTest {

    @Test
    fun pointOnHorizontalSegment() {
        val segment = Segment(Position(1.0, 0.0), Position(1.0, 2.0))
        val point = Position(1.0, 1.0)
        assertTrue(isPointOnSegment(point, segment))
    }

    @Test
    fun pointOnVerticalSegment() {
        val segment = Segment(Position(0.0, 1.0), Position(2.0, 1.0))
        val point = Position(1.0, 1.0)
        assertTrue(isPointOnSegment(point, segment))
    }

    @Test
    fun pointOnDiagonalSegment() {
        val segment = Segment(Position(0.0, 0.0), Position(2.0, 2.0))
        val point = Position(1.0, 1.0)
        assertTrue(isPointOnSegment(point, segment))
    }

    @Test
    fun pointOutsideSegmentHorizontal() {
        val segment = Segment(Position(1.0, 0.0), Position(1.0, 2.0))
        val point = Position(2.0, 1.0)
        assertFalse(isPointOnSegment(point, segment))
    }

    @Test
    fun pointOutsideSegmentVertical() {
        val segment = Segment(Position(0.0, 1.0), Position(2.0, 1.0))
        val point = Position(1.0, 2.0)
        assertFalse(isPointOnSegment(point, segment))
    }

    @Test
    fun pointOutsideSegmentDiagonal() {
        val segment = Segment(Position(0.0, 0.0), Position(2.0, 2.0))
        val point = Position(1.0, 0.0)
        assertFalse(isPointOnSegment(point, segment))
    }

    @Test
    fun pointOnSegmentEndpoint() {
        val segment = Segment(Position(0.0, 0.0), Position(2.0, 2.0))
        val point = Position(0.0, 0.0)
        assertTrue(isPointOnSegment(point, segment))
    }

    @Test
    fun pointCollinearButOutsideSegment() {
        val segment = Segment(Position(0.0, 0.0), Position(2.0, 2.0))
        val point = Position(3.0, 3.0)
        assertFalse(isPointOnSegment(point, segment))
    }

    @Test
    fun pointNotCollinear() {
        val segment = Segment(Position(0.0, 0.0), Position(2.0, 2.0))
        val point = Position(1.0, 2.0)
        assertFalse(isPointOnSegment(point, segment))
    }

    @Test
    fun testToRadians() {
        assertEquals(0.0, 0.0.toRadians(), EPSILON)
        assertEquals(PI / 2, 90.0.toRadians(), EPSILON)
        assertEquals(PI, 180.0.toRadians(), EPSILON)
        assertEquals(3 * PI / 2, 270.0.toRadians(), EPSILON)
        assertEquals(2 * PI, 360.0.toRadians(), EPSILON)
    }

    @Test
    fun testToDegrees() {
        assertEquals(0.0, 0.0.toDegrees(), EPSILON)
        assertEquals(90.0, (PI / 2).toDegrees(), EPSILON)
        assertEquals(180.0, PI.toDegrees(), EPSILON)
        assertEquals(270.0, (3 * PI / 2).toDegrees(), EPSILON)
        assertEquals(360.0, (2 * PI).toDegrees(), EPSILON)
    }

    @Test
    fun testIsLongitudeEqual() {
        assertTrue(isLongitudeEqual(0.0, 0.0))
        assertFalse(isLongitudeEqual(0.0, 180.0))
    }

    @Test
    fun testIsLongitudeInRange() {
        assertTrue(isLongitudeInRange(0.0, -180.0, 180.0))
        assertTrue(isLongitudeInRange(180.0, -180.0, 180.0))
        assertTrue(isLongitudeInRange(170.0, 160.0, 180.0))
    }

    @Test
    fun testIsLatitudeInRange() {
        assertTrue(isLatitudeInRange(0.0, -90.0, 90.0))
        assertTrue(isLatitudeInRange(90.0, -90.0, 90.0))
        assertFalse(isLatitudeInRange(-100.0, -90.0, 90.0))
        assertTrue(isLatitudeInRange(45.0, 0.0, 90.0))
        assertFalse(isLatitudeInRange(100.0, 0.0, 90.0))
    }

}