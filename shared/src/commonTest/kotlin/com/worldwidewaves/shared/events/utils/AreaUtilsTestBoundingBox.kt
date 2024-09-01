package com.worldwidewaves.shared.events.utils

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
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

class AreaUtilsTestBoundingBox {

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
        val polygon : Polygon = emptyList()
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

    // ========================================================================

    private fun createMockGeoJsonDataProvider(geoJson: String): GeoJsonDataProvider {
        return object: GeoJsonDataProvider {
            override suspend fun getGeoJsonData(eventId: String): JsonObject {
                return Json.parseToJsonElement(geoJson).jsonObject
            }
        }
    }

}
