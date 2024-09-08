package com.worldwidewaves.shared.events.utils

import com.worldwidewaves.shared.events.utils.PolygonUtils.isPointInPolygon
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

class AreaUtilsPointInPolygonTest {

    /**
     * Test points inside and outside a simple square polygon
     *
     * This test validates the correctness of the `isPointInPolygon` function by testing both an inside point
     * and an outside point relative to a simple square polygon.
     *
     * The square polygon is defined with vertices at the following coordinates:
     * (0.0, 0.0), (0.0, 1.0), (1.0, 1.0), (1.0, 0.0), and closes back at (0.0, 0.0).
     *
     * Coordinate system:
     *
     *      ^ Latitude (Y)
     *    3 |
     *      |
     *      |
     *    2 |
     *      |           O
     *      |
     *    1 o-------o
     *      |       |
     *      |   X   |
     *      |       |
     *    0 o-------o-------o-------o---> Longitude (X)
     *      0       1       2       3
     *
     * In this system:
     * - The **polygon** is a square with vertices at (0.0, 0.0), (0.0, 1.0), (1.0, 1.0), and (1.0, 0.0).
     * - The **inside point** (X) is at (0.5, 0.5), which lies within the bounds of the polygon.
     * - The **outside point** (O) is at (2.0, 2.0), outside the polygon.
     *
     * The function first checks if the inside point is correctly identified as within the polygon,
     * and then verifies that the outside point is correctly identified as outside the polygon.
     */
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

        assertTrue(
            isPointInPolygon(insidePoint, polygon),
            "Expected point to be inside the polygon"
        )

        // Debug information
        println("Testing point outside polygon: $outsidePoint")

        assertFalse(
            isPointInPolygon(outsidePoint, polygon),
            "Expected point to be outside the polygon"
        )
    }

    /**
     * Test a point outside a simple square polygon
     *
     * This test validates the correctness of the `isPointInPolygon` function by testing a point
     * that is located outside a simple square polygon. The polygon is defined as a square,
     * and the point is located outside of this polygon above the square.
     *
     * The square polygon is defined by the following coordinates:
     * (0.0, 0.0), (0.0, 1.0), (1.0, 1.0), (1.0, 0.0), and closes back at (0.0, 0.0).
     *
     * Coordinate system:
     *
     *      ^ Latitude (Y)
     *    3 |
     *      |
     *      |
     *    2 |   O
     *      |
     *    1 o-------o
     *      |       |
     *      |       |
     *      |       |
     *    0 o-------o-------o-------o---> Longitude (X)
     *      0       1       2       3
     *
     * In this system:
     * - The **polygon** is a simple square with vertices at (0.0, 0.0), (0.0, 1.0), (1.0, 1.0), and (1.0, 0.0).
     * - The **point** is at (1.5, 2.0), which is located outside of the polygon.
     *
     * The function asserts that the point is correctly identified as outside the polygon.
     */
    @Test
    fun testIsPointOutsideSimplePolygon() {
        val point = Position(1.5, 0.5)
        val polygon = listOf(
            Position(0.0, 0.0),
            Position(0.0, 1.0),
            Position(1.0, 1.0), Position(1.0, 0.0),
            Position(0.0, 0.0)
        )
        assertFalse(isPointInPolygon(point, polygon))
    }

    /**
     * Test a point inside a concave polygon
     *
     * This test checks whether the `isPointInPolygon` function can correctly identify a point
     * located inside a concave polygon. The polygon in this test forms a simple concave shape,
     * where one vertex indents into the polygon, creating a concave structure.
     *
     * The concave polygon is defined by the following coordinates:
     * (0.0, 0.0), (0.0, 1.0), (0.5, 0.5), (1.0, 1.0), (1.0, 0.0), and closes back at (0.0, 0.0).
     *
     * Coordinate system:
     *
     *      ^ Latitude (Y)
     *    3 |
     *      |
     *      |
     *    2 |
     *      |
     *      |
     *    1 o-------o
     *      |     /
     *      |   X
     *      |     \
     *    0 o-------o-------o-------o---> Longitude (X)
     *      0       1       2       3
     *
     * In this system:
     * - The **concave polygon** is formed by the vertices, creating a concave "V" shape.
     * - The **point** is at (0.5, 0.5), which is inside the concave part of the polygon.
     *
     * The function tests whether the point is correctly identified as being inside the concave polygon.
     */
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
