package com.worldwidewaves.shared.events

import kotlin.test.Test
import kotlin.test.assertFalse
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

class WWWEventAreaTest {

    @Test
    fun testIsPointInSimplePolygon() {
        val polygon = listOf(
            Position(0.0, 0.0),
            Position(0.0, 1.0),
            Position(1.0, 1.0),
            Position(1.0, 0.0),
            Position(0.0, 0.0)
        )
        val insidePoint = Position(0.5, 0.5)
        val outsidePoint = Position(1.5, 1.5)

        // Debug information
        println("Testing point inside polygon: $insidePoint")
        println("Polygon vertices: $polygon")

        assertTrue(isPointInPolygon(insidePoint, polygon), "Expected point to be inside the polygon")

        // Debug information
        println("Testing point outside polygon: $outsidePoint")

        assertFalse(isPointInPolygon(outsidePoint, polygon), "Expected point to be outside the polygon")
    }

    @Test
    fun testIsPointOutsideSimplePolygon() {
        val point = Position(1.5, 0.5)
        val polygon = listOf(
            Position(0.0, 0.0),
            Position(0.0, 1.0),
            Position(1.0, 1.0),Position(1.0, 0.0),
            Position(0.0, 0.0)
        )
        assertFalse(isPointInPolygon(point, polygon))
    }

    @Test
    fun testIsPointInConcavePolygon() {
        val point = Position(0.5, 0.5)
        val polygon = listOf(
            Position(0.0, 0.0),
            Position(0.0, 1.0),
            Position(0.5, 0.5),
            Position(1.0, 1.0),
            Position(1.0, 0.0),
            Position(0.0, 0.0)
        )
        assertTrue(isPointInPolygon(point, polygon))
    }

    @Test
    fun testIsPointOutsideConcavePolygon() {
        val point = Position(0.2, 0.8)
        val polygon = listOf(
            Position(0.0, 0.0),
            Position(0.0, 1.0),
            Position(0.5, 0.5),
            Position(1.0, 1.0),
            Position(1.0, 0.0),
            Position(0.0, 0.0)
        )
        assertFalse(isPointInPolygon(point, polygon))
    }

    // Edge Cases

    @Test
    fun testPointOnVertex() {
        val point = Position(0.0, 0.0)
        val polygon = listOf(
            Position(0.0, 0.0),
            Position(0.0, 1.0),
            Position(1.0, 1.0),
            Position(1.0, 0.0),
            Position(0.0, 0.0)
        )
        assertTrue(isPointInPolygon(point, polygon)) // Consider a point on a vertex as inside
    }

    @Test
    fun testPointOnHorizontalEdge() {
        val point = Position(0.5, 0.0)
        val polygon = listOf(
            Position(0.0, 0.0),
            Position(0.0, 1.0),
            Position(1.0, 1.0),
            Position(1.0, 0.0),
            Position(0.0, 0.0)
        )
        assertTrue(isPointInPolygon(point, polygon)) // Consider a point on an edge as inside
    }

    @Test
    fun testPointOnVerticalEdge() {
        val point = Position(0.0, 0.5)
        val polygon = listOf(
            Position(0.0, 0.0),
            Position(0.0, 1.0),
            Position(1.0, 1.0),
            Position(1.0, 0.0),
            Position(0.0, 0.0)
        )
        assertTrue(isPointInPolygon(point, polygon)) // Consider a point on an edge as inside
    }

}