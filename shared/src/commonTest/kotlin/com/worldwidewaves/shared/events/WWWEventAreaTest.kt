package com.worldwidewaves.shared.events

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
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

    // -----------------------

    @Test
    fun testSimplePolygon() {
        val polygon = listOf(
            Position(0.0, 0.0),
            Position(0.0, 1.0),
            Position(1.0, 1.0),
            Position(1.0, 0.0),
            Position(0.0, 0.0)
        )
        val expectedBbox = BoundingBox(0.0, 0.0, 1.0, 1.0)
        val actualBbox = polygonBbox(polygon)
        assertEquals(expectedBbox, actualBbox)
    }

    @Test
    fun testBadPolygon() {
        val polygon = emptyList<Position>()
        assertFailsWith<IllegalArgumentException> {
            polygonBbox(polygon)
        }
    }

    @Test
    fun testComplexPolygon() {
        val polygon = listOf(
            Position(0.0, 0.0),
            Position(0.0, 2.0),
            Position(1.0, 1.0),
            Position(2.0, 2.0),
            Position(2.0, 0.0),
            Position(0.0, 0.0)
        )
        val expectedBbox = BoundingBox(0.0, 0.0, 2.0, 2.0)
        val actualBbox = polygonBbox(polygon)
        assertEquals(expectedBbox, actualBbox)
    }

    @Test
    fun testPointOnEdge() {
        val polygon = listOf(
            Position(0.0, 0.0),
            Position(0.0, 1.0),
            Position(1.0, 1.0),
            Position(1.0, 0.0),
            Position(0.0, 0.0)
        )
        val pointOnEdge = Position(0.5, 0.0)
        assertTrue(isPointInPolygon(pointOnEdge, polygon))
    }

    @Test
    fun testPolygonWithHole() {
        val outerPolygon = listOf(
            Position(0.0, 0.0),
            Position(0.0, 3.0),
            Position(3.0, 3.0),
            Position(3.0, 0.0),
            Position(0.0, 0.0)
        )
        val innerPolygon = listOf(
            Position(1.0, 1.0),
            Position(1.0, 2.0),
            Position(2.0, 2.0),
            Position(2.0, 1.0),
            Position(1.0, 1.0)
        )
        val combinedPolygon = outerPolygon + innerPolygon
        val expectedBbox = BoundingBox(0.0, 0.0, 3.0, 3.0)
        val actualBbox = polygonBbox(combinedPolygon)
        assertEquals(expectedBbox, actualBbox)
    }

    @Test
    fun testDegeneratePolygon() {
        val polygon = listOf(
            Position(1.0, 1.0),
            Position(1.0, 1.0),
            Position(1.0, 1.0)
        )
        val expectedBbox = BoundingBox(1.0, 1.0, 1.0, 1.0)
        val actualBbox = polygonBbox(polygon)
        assertEquals(expectedBbox, actualBbox)
    }

    // -----------------------

    private fun createMockGeoJsonDataProvider(geoJson: String): GeoJsonDataProvider {
        return object: GeoJsonDataProvider {
            override suspend fun getGeoJsonData(eventId: String): JsonObject {
                return Json.parseToJsonElement(geoJson).jsonObject
            }
        }
    }

    private fun createWWWEventArea(event: WWWEvent, geoJson: String): WWWEventArea {
        val geoJsonDataProvider = createMockGeoJsonDataProvider(geoJson)
        return WWWEventArea(event, geoJsonDataProvider)
    }

    private val polygonGeoJson = """
        {"type":"Polygon","coordinates":[[[0.0,0.0],[0.0,10.0],[10.0,10.0],[10.0,0.0],[0.0,0.0]]]}
    """.trimIndent()

    private val emptyPolygonGeoJson = """
        {"type":"Polygon","coordinates":[]}
    """.trimIndent()

    @Test
    fun testIsPositionWithin() {
        val randomEvent = createRandomWWWEvent("test_event")
        val eventArea = createWWWEventArea(randomEvent, polygonGeoJson)
        val position = Position(5.0, 5.0) // Position within the polygon

        runBlocking {
            assertTrue(eventArea.isPositionWithin(position), "Expected position to be within the event area")
        }
    }

    @Test
    fun testGetBoundingBox() {
        val randomEvent = createRandomWWWEvent("test_event")
        val eventArea = createWWWEventArea(randomEvent, polygonGeoJson)

        runBlocking {
            val boundingBox = eventArea.getBoundingBox()
            assertNotNull(boundingBox, "Expected bounding box to be not null")
            assertTrue(boundingBox.minLatitude <= boundingBox.maxLatitude, "Invalid bounding box coordinates")
            assertTrue(boundingBox.minLongitude <= boundingBox.maxLongitude, "Invalid bounding box coordinates")
        }
    }

    @Test
    fun testGetCachedPolygon() {
        val randomEvent = createRandomWWWEvent("test_event")
        val eventArea = createWWWEventArea(randomEvent, polygonGeoJson)

        runBlocking {
            val polygon = eventArea.getPolygon()
            val expectedPolygon = listOf(
                Position(latitude=0.0, longitude=0.0),
                Position(latitude=10.0, longitude=0.0),
                Position(latitude=10.0, longitude=10.0),
                Position(latitude=0.0, longitude=10.0),
                Position(latitude=0.0, longitude=0.0)
            )
            assertEquals(expectedPolygon, polygon, "Expected polygon to match the predefined polygon")
        }
    }

    @Test
    fun testGetCachedPolygonEmpty() {
        val randomEvent = createRandomWWWEvent("test_event")
        val eventArea = createWWWEventArea(randomEvent, emptyPolygonGeoJson)

        runBlocking {
            val polygon = eventArea.getPolygon()
            assertTrue(polygon.isEmpty(), "Expected polygon to be empty")
        }
    }
}
