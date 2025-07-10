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

import com.worldwidewaves.shared.events.utils.PolygonUtils.containsPosition
import com.worldwidewaves.shared.events.utils.PolygonUtils.polygonsBbox
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BoundingBoxTest {

    @Test
    fun testSimplePolygon() {
        val polygon = Polygon.fromPositions(
            Position(0.0, 0.0),
            Position(0.0, 1.0),
            Position(1.0, 1.0),
            Position(1.0, 0.0),
            Position(0.0, 0.0)
        )
        val expectedBbox = BoundingBox(0.0, 0.0, 1.0, 1.0)
        val actualBbox = polygon.bbox()
        assertEquals(expectedBbox, actualBbox)
    }

    @Test
    fun testBadPolygon() {
        val polygon = Polygon()
        assertFailsWith<IllegalArgumentException> {
            polygon.bbox()
        }
    }

    @Test
    fun testComplexPolygon() {
        val polygon = Polygon.fromPositions(
            Position(0.0, 0.0),
            Position(0.0, 2.0),
            Position(1.0, 1.0),
            Position(2.0, 2.0),
            Position(2.0, 0.0),
            Position(0.0, 0.0)
        )
        val expectedBbox = BoundingBox(0.0, 0.0, 2.0, 2.0)
        val actualBbox = polygon.bbox()
        assertEquals(expectedBbox, actualBbox)
    }

    @Test
    fun testBoundingBoxInitialization() {
        val sw = Position(0.0, 0.0)
        val ne = Position(1.0, 1.0)
        val bbox = BoundingBox.fromPositions(sw, ne)
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
    fun testPointOnEdge() {
        val polygon = Polygon.fromPositions(
            Position(0.0, 0.0),
            Position(0.0, 1.0),
            Position(1.0, 1.0),
            Position(1.0, 0.0),
            Position(0.0, 0.0)
        )
        val pointOnEdge = Position(0.5, 0.0)
        assertTrue(polygon.containsPosition(pointOnEdge))
    }

    @Test
    fun testPolygonWithHole() {
        val outerPolygon = Polygon.fromPositions(
            Position(0.0, 0.0),
            Position(0.0, 3.0),
            Position(3.0, 3.0),
            Position(3.0, 0.0),
            Position(0.0, 0.0)
        )
        val innerPolygon = Polygon.fromPositions(
            Position(1.0, 1.0),
            Position(1.0, 2.0),
            Position(2.0, 2.0),
            Position(2.0, 1.0),
            Position(1.0, 1.0)
        )
        val combinedPolygon = outerPolygon + innerPolygon
        val expectedBbox = BoundingBox(0.0, 0.0, 3.0, 3.0)
        val actualBbox = combinedPolygon.bbox()
        assertEquals(expectedBbox, actualBbox)
    }

    @Test
    fun testDegeneratePolygon() {
        val polygon = Polygon.fromPositions(
            Position(1.0, 1.0),
            Position(1.0, 1.0),
            Position(1.0, 1.0)
        )
        val expectedBbox = BoundingBox(1.0, 1.0, 1.0, 1.0)
        val actualBbox = polygon.bbox()
        assertEquals(expectedBbox, actualBbox)
    }

    @Test
    fun testPolygonsBbox() {
        val polygon1 = Polygon.fromPositions(
            Position(0.0, 0.0),
            Position(2.0, 0.0),
            Position(2.0, 2.0),
            Position(0.0, 2.0)
        )
        val polygon2 = Polygon.fromPositions(
            Position(1.0, 1.0),
            Position(3.0, 1.0),
            Position(3.0, 3.0),
            Position(1.0, 3.0)
        )

        val bbox = polygonsBbox(listOf(polygon1, polygon2))

        assertEquals(0.0, bbox.sw.lat)
        assertEquals(0.0, bbox.sw.lng)
        assertEquals(3.0, bbox.ne.lat)
        assertEquals(3.0, bbox.ne.lng)

        // Test with empty list
        assertFailsWith<IllegalArgumentException> {
            polygonsBbox(emptyList())
        }
    }

    @Test
    fun `constructor should create valid bounding box`() {
        val bbox = BoundingBox(0.0, 0.0, 1.0, 1.0)
        assertEquals(0.0, bbox.minLatitude)
        assertEquals(0.0, bbox.minLongitude)
        assertEquals(1.0, bbox.maxLatitude)
        assertEquals(1.0, bbox.maxLongitude)
    }

    @Test
    fun `constructor should handle inverted coordinates`() {
        val bbox = BoundingBox(1.0, 1.0, 0.0, 0.0)
        assertEquals(0.0, bbox.minLatitude)
        assertEquals(0.0, bbox.minLongitude)
        assertEquals(1.0, bbox.maxLatitude)
        assertEquals(1.0, bbox.maxLongitude)
        assertEquals(1.0, bbox.height)
        assertEquals(1.0, bbox.width)
    }

    @Test
    fun `width and height should be calculated correctly`() {
        val bbox = BoundingBox(0.0, 0.0, 1.0, 2.0)
        assertEquals(1.0, bbox.height)
        assertEquals(2.0, bbox.width)
    }

    @Test
    fun `contains should return true for point inside bounding box`() {
        val bbox = BoundingBox(0.0, 0.0, 2.0, 2.0)
        assertTrue(bbox.contains(Position(1.0, 1.0)))
    }

    @Test
    fun `contains should return false for point outside bounding box`() {
        val bbox = BoundingBox(0.0, 0.0, 2.0, 2.0)
        assertFalse(bbox.contains(Position(3.0, 3.0)))
    }

    @Test
    fun `intersects should return true for overlapping bounding boxes`() {
        val bbox1 = BoundingBox(0.0, 0.0, 2.0, 2.0)
        val bbox2 = BoundingBox(1.0, 1.0, 3.0, 3.0)
        assertTrue(bbox1.intersects(bbox2))
    }

    @Test
    fun `intersects should return false for non-overlapping bounding boxes`() {
        val bbox1 = BoundingBox(0.0, 0.0, 1.0, 1.0)
        val bbox2 = BoundingBox(2.0, 2.0, 3.0, 3.0)
        assertFalse(bbox1.intersects(bbox2))
    }

    @Test
    fun `expand should create larger bounding box`() {
        val original = BoundingBox(0.0, 0.0, 1.0, 1.0)
        val expanded = original.expand(2.0)
        assertEquals(-0.5, expanded.minLatitude)
        assertEquals(-0.5, expanded.minLongitude)
        assertEquals(1.5, expanded.maxLatitude)
        assertEquals(1.5, expanded.maxLongitude)
    }

    @Test
    fun `fromPositions should create correct bounding box`() {
        val positions = listOf(
            Position(0.0, 0.0),
            Position(1.0, 1.0),
            Position(-1.0, -1.0)
        )
        val bbox = BoundingBox.fromPositions(positions)
        assertEquals(-1.0, bbox?.minLatitude)
        assertEquals(-1.0, bbox?.minLongitude)
        assertEquals(1.0, bbox?.maxLatitude)
        assertEquals(1.0, bbox?.maxLongitude)
    }

    @Test
    fun `fromPositions should return null for empty list`() {
        val bbox = BoundingBox.fromPositions(emptyList())
        assertNull(bbox)
    }

    @Test
    fun `equals should return true for identical bounding boxes`() {
        val bbox1 = BoundingBox(0.0, 0.0, 1.0, 1.0)
        val bbox2 = BoundingBox(0.0, 0.0, 1.0, 1.0)
        assertEquals(bbox1, bbox2)
    }

    @Test
    fun `equals should return false for different bounding boxes`() {
        val bbox1 = BoundingBox(0.0, 0.0, 1.0, 1.0)
        val bbox2 = BoundingBox(0.0, 0.0, 2.0, 2.0)
        assertNotEquals(bbox1, bbox2)
    }

    @Test
    fun `hashCode should be the same for identical bounding boxes`() {
        val bbox1 = BoundingBox(0.0, 0.0, 1.0, 1.0)
        val bbox2 = BoundingBox(0.0, 0.0, 1.0, 1.0)
        assertEquals(bbox1.hashCode(), bbox2.hashCode())
    }

    @Test
    fun `latitudeOfWidestPart should return 0_0 when box crosses the equator`() {
        val bbox = BoundingBox(-1.0, 0.0, 1.0, 1.0)
        assertEquals(0.0, bbox.latitudeOfWidestPart())
    }

    @Test
    fun `latitudeOfWidestPart should return southern latitude when it is closer to equator`() {
        val bbox = BoundingBox(-1.0, 0.0, -2.0, 1.0)
        assertEquals(-1.0, bbox.latitudeOfWidestPart())
    }

    @Test
    fun `latitudeOfWidestPart should return northern latitude when it is closer to equator`() {
        val bbox = BoundingBox(-3.0, 0.0, -1.0, 1.0)
        assertEquals(-1.0, bbox.latitudeOfWidestPart())
    }

    @Test
    fun `latitudeOfWidestPart should return northern latitude when box is entirely in northern hemisphere`() {
        val bbox = BoundingBox(1.0, 0.0, 3.0, 1.0)
        assertEquals(1.0, bbox.latitudeOfWidestPart())
    }

    @Test
    fun `latitudeOfWidestPart should return southern latitude when box is entirely in southern hemisphere`() {
        val bbox = BoundingBox(-3.0, 0.0, -1.0, 1.0)
        assertEquals(-1.0, bbox.latitudeOfWidestPart())
    }

    @Test
    fun `width should return correct value when bounding box does not cross antimeridian`() {
        val bbox = BoundingBox(0.0, 0.0, 1.0, 2.0)
        assertEquals(2.0, bbox.width)
    }

    @Test
    fun `height should return correct value`() {
        val bbox = BoundingBox(0.0, 0.0, 2.0, 1.0)
        assertEquals(2.0, bbox.height)
    }

}
