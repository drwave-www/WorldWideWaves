package com.worldwidewaves.shared.events.utils

/*
 * Copyright 2025 DrWave
 *
 * WorldWideWaves is an ephemeral mobile app designed to orchestrate human waves through cities and
 * countries. The project aims to transcend physical and cultural
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


import com.worldwidewaves.shared.events.utils.PolygonUtils.convertPolygonsToGeoJson
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PolygonUtilsMiscTest {

    @Test
    fun testConvertPolygonsToGeoJson() {
        val polygon1 = Polygon.fromPositions(
            Position(0.0, 0.0),
            Position(1.0, 0.0),
            Position(1.0, 1.0),
            Position(0.0, 1.0)
        )
        val polygon2 = Polygon.fromPositions(
            Position(2.0, 2.0),
            Position(3.0, 2.0),
            Position(3.0, 3.0),
            Position(2.0, 3.0)
        )

        val geoJson = convertPolygonsToGeoJson(listOf(polygon1, polygon2))

        // Basic structure checks
        assertTrue(geoJson.contains("\"type\": \"FeatureCollection\""))
        assertTrue(geoJson.contains("\"type\": \"Feature\""))
        assertTrue(geoJson.contains("\"type\": \"Polygon\""))

        // Check for coordinates
        assertTrue(geoJson.contains("[0.0, 0.0]"))
        assertTrue(geoJson.contains("[1.0, 0.0]"))
        assertTrue(geoJson.contains("[2.0, 2.0]"))
        assertTrue(geoJson.contains("[3.0, 3.0]"))
    }

    // --------------------------------------------------------

    @Test
    fun testSimpleClockwiseTriangle() {
        val polygon = Polygon()
        polygon.add(Position(0.0, 0.0))
        polygon.add(Position(1.0, 0.0))
        polygon.add(Position(0.0, 1.0))
        assertTrue(polygon.isClockwise(), "Simple clockwise triangle should be identified as clockwise")
    }

    @Test
    fun testSimpleCounterclockwiseTriangle() {
        val polygon = Polygon()
        polygon.add(Position(0.0, 0.0))
        polygon.add(Position(0.0, 1.0))
        polygon.add(Position(1.0, 0.0))
        assertFalse(polygon.isClockwise(), "Simple counterclockwise triangle should be identified as counterclockwise")
    }

    @Test
    fun testClockwiseSquare() {
        val polygon = Polygon()
        polygon.add(Position(0.0, 0.0))
        polygon.add(Position(1.0, 0.0))
        polygon.add(Position(1.0, 1.0))
        polygon.add(Position(0.0, 1.0))
        assertTrue(polygon.isClockwise(), "Clockwise square should be identified as clockwise")
    }

    @Test
    fun testCounterclockwiseSquare() {
        val polygon = Polygon()
        polygon.add(Position(0.0, 0.0))
        polygon.add(Position(0.0, 1.0))
        polygon.add(Position(1.0, 1.0))
        polygon.add(Position(1.0, 0.0))
        assertFalse(polygon.isClockwise(), "Counterclockwise square should be identified as counterclockwise")
    }

    @Test
    fun testComplexClockwisePolygon() {
        val polygon = Polygon()
        polygon.add(Position(0.0, 0.0))
        polygon.add(Position(2.0, 0.0))
        polygon.add(Position(2.0, 1.0))
        polygon.add(Position(1.0, 1.0))
        polygon.add(Position(1.0, 2.0))
        polygon.add(Position(0.0, 2.0))
        assertTrue(polygon.isClockwise(), "Complex clockwise polygon should be identified as clockwise")
    }

    @Test
    fun testComplexCounterclockwisePolygon() {
        val polygon = Polygon()
        polygon.add(Position(0.0, 0.0))
        polygon.add(Position(0.0, 2.0))
        polygon.add(Position(1.0, 2.0))
        polygon.add(Position(1.0, 1.0))
        polygon.add(Position(2.0, 1.0))
        polygon.add(Position(2.0, 0.0))
        assertFalse(polygon.isClockwise(), "Complex counterclockwise polygon should be identified as counterclockwise")
    }

    @Test
    fun testClockwiseDirectionChangeOnRemoval() {
        val polygon = Polygon()
        polygon.add(Position(0.0, 0.0))
        polygon.add(Position(0.0, 1.0))
        polygon.add(Position(1.0, 1.0))
        polygon.add(Position(1.0, 0.0))
        assertFalse(polygon.isClockwise(), "Initial polygon should be counterclockwise")

        polygon.remove(polygon.first()!!.id)
        assertFalse(polygon.isClockwise(), "Polygon should remain counterclockwise after removal")
    }

    @Test
    fun testClockwiseDirectionDoNotChangeOnInsertion() {
        val polygon = Polygon()
        polygon.add(Position(0.0, 0.0))
        polygon.add(Position(1.0, 0.0))
        polygon.add(Position(1.0, 1.0))
        assertTrue(polygon.isClockwise(), "Initial polygon should be clockwise")

        polygon.insertAfter(Position(0.0, 1.0), polygon.last()!!.id)
        assertTrue(polygon.isClockwise(), "Polygon should be clockwise after insertion")
    }

    @Test
    fun testClockwiseDirectionDoNotChangeOnInsertionAfterClosed() {
        val polygon = Polygon()
        polygon.add(Position(0.0, 0.0))
        polygon.add(Position(1.0, 0.0))
        val insertPosition = polygon.add(Position(1.0, 1.0))
        polygon.close()
        assertTrue(polygon.isClockwise(), "Initial polygon should be clockwise")

        polygon.insertAfter(Position(0.0, 1.0), insertPosition.id)
        assertTrue(polygon.isClockwise(), "Polygon should be clockwise after insertion")
    }

    @Test
    fun testClockwiseDirectionDoNotChangeOnInsertionBeforeClosed() {
        val polygon = Polygon()
        val insertPosition = polygon.add(Position(0.0, 0.0))
        polygon.add(Position(1.0, 0.0))
        polygon.add(Position(1.0, 1.0))
        polygon.close()
        assertTrue(polygon.isClockwise(), "Initial polygon should be clockwise")

        polygon.insertBefore(Position(0.0, 1.0), insertPosition.id)
        assertTrue(polygon.isClockwise(), "Polygon should be clockwise after insertion")
    }

    @Test
    fun testOrientationChangeOnSpecificInsertion() {
        val polygon = Polygon()
        polygon.add(Position(0.0, 0.0))
        polygon.add(Position(1.0, 0.0))
        polygon.add(Position(1.0, 1.0))
        assertTrue(polygon.isClockwise(), "Initial polygon should be clockwise")

        // Insert a point that changes the shape significantly
        polygon.add(Position(2.0, -0.5))
        assertFalse(polygon.isClockwise(), "Polygon should become counterclockwise after this specific insertion")
    }

    @Test
    fun testEmptyPolygon() {
        val polygon = Polygon()
        assertTrue(polygon.isClockwise(), "Empty polygon should default to clockwise")
    }

    @Test
    fun testTwoPointPolygon() {
        val polygon = Polygon()
        polygon.add(Position(0.0, 0.0))
        polygon.add(Position(1.0, 0.0))
        assertTrue(polygon.isClockwise(), "Two-point polygon should be considered clockwise")
    }

    @Test
    fun testPolygonWithColinearPoints() {
        val polygon = Polygon()
        polygon.add(Position(0.0, 0.0))
        polygon.add(Position(1.0, 0.0))
        polygon.add(Position(2.0, 0.0))
        polygon.add(Position(2.0, 1.0))
        polygon.add(Position(0.0, 1.0))
        assertTrue(polygon.isClockwise(), "Polygon with colinear points should still be correctly identified as clockwise")
    }

}