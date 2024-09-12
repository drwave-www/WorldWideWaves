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

import com.worldwidewaves.shared.events.utils.GeoUtils.isPointOnLineSegment
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GeoUtilsTest {

    @Test
    fun pointOnHorizontalSegment() {
        val segment = Segment(Position(1.0, 0.0), Position(1.0, 2.0))
        val point = Position(1.0, 1.0)
        assertTrue(isPointOnLineSegment(point, segment))
    }

    @Test
    fun pointOnVerticalSegment() {
        val segment = Segment(Position(0.0, 1.0), Position(2.0, 1.0))
        val point = Position(1.0, 1.0)
        assertTrue(isPointOnLineSegment(point, segment))
    }

    @Test
    fun pointOnDiagonalSegment() {
        val segment = Segment(Position(0.0, 0.0), Position(2.0, 2.0))
        val point = Position(1.0, 1.0)
        assertTrue(isPointOnLineSegment(point, segment))
    }

    @Test
    fun pointOutsideSegmentHorizontal() {
        val segment = Segment(Position(1.0, 0.0), Position(1.0, 2.0))
        val point = Position(2.0, 1.0)
        assertFalse(isPointOnLineSegment(point, segment))
    }

    @Test
    fun pointOutsideSegmentVertical() {
        val segment = Segment(Position(0.0, 1.0), Position(2.0, 1.0))
        val point = Position(1.0, 2.0)
        assertFalse(isPointOnLineSegment(point, segment))
    }

    @Test
    fun pointOutsideSegmentDiagonal() {
        val segment = Segment(Position(0.0, 0.0), Position(2.0, 2.0))
        val point = Position(1.0, 0.0)
        assertFalse(isPointOnLineSegment(point, segment))
    }

    @Test
    fun pointOnSegmentEndpoint() {
        val segment = Segment(Position(0.0, 0.0), Position(2.0, 2.0))
        val point = Position(0.0, 0.0)
        assertTrue(isPointOnLineSegment(point, segment))
    }

    @Test
    fun pointCollinearButOutsideSegment() {
        val segment = Segment(Position(0.0, 0.0), Position(2.0, 2.0))
        val point = Position(3.0, 3.0)
        assertFalse(isPointOnLineSegment(point, segment))
    }

    @Test
    fun pointNotCollinear() {
        val segment = Segment(Position(0.0, 0.0), Position(2.0, 2.0))
        val point = Position(1.0, 2.0)
        assertFalse(isPointOnLineSegment(point, segment))
    }

}