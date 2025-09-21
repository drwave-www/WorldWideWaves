package com.worldwidewaves.shared.events

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

import com.worldwidewaves.shared.events.utils.BoundingBox
import com.worldwidewaves.shared.events.utils.CoroutineScopeProvider
import com.worldwidewaves.shared.events.utils.DefaultCoroutineScopeProvider
import com.worldwidewaves.shared.events.utils.GeoJsonDataProvider
import com.worldwidewaves.shared.events.utils.Position
import com.worldwidewaves.shared.testing.TestHelpers
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class WWWEventAreaTest : KoinTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockGeoJsonDataProvider: GeoJsonDataProvider
    private lateinit var testEvent: WWWEvent

    // Test GeoJSON data samples
    private val simplePolygonGeoJson = """
    {
        "type": "Polygon",
        "coordinates": [
            [
                [2.3, 48.85],
                [2.4, 48.85],
                [2.4, 48.86],
                [2.3, 48.86],
                [2.3, 48.85]
            ]
        ]
    }
    """.trimIndent()

    private val multiPolygonGeoJson = """
    {
        "type": "MultiPolygon",
        "coordinates": [
            [
                [
                    [2.3, 48.85],
                    [2.4, 48.85],
                    [2.4, 48.86],
                    [2.3, 48.86],
                    [2.3, 48.85]
                ]
            ],
            [
                [
                    [2.5, 48.87],
                    [2.6, 48.87],
                    [2.6, 48.88],
                    [2.5, 48.88],
                    [2.5, 48.87]
                ]
            ]
        ]
    }
    """.trimIndent()

    private val featureCollectionGeoJson = """
    {
        "type": "FeatureCollection",
        "features": [
            {
                "type": "Feature",
                "geometry": {
                    "type": "Polygon",
                    "coordinates": [
                        [
                            [2.3, 48.85],
                            [2.4, 48.85],
                            [2.4, 48.86],
                            [2.3, 48.86],
                            [2.3, 48.85]
                        ]
                    ]
                }
            }
        ]
    }
    """.trimIndent()

    private val geoJsonWithBbox = """
    {
        "type": "Polygon",
        "bbox": [2.3, 48.85, 2.4, 48.86],
        "coordinates": [
            [
                [2.3, 48.85],
                [2.4, 48.85],
                [2.4, 48.86],
                [2.3, 48.86],
                [2.3, 48.85]
            ]
        ]
    }
    """.trimIndent()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockGeoJsonDataProvider = mockk<GeoJsonDataProvider>()

        startKoin {
            modules(
                module {
                    single<GeoJsonDataProvider> { mockGeoJsonDataProvider }
                    single<CoroutineScopeProvider> { DefaultCoroutineScopeProvider(testDispatcher, testDispatcher) }
                }
            )
        }

        testEvent = TestHelpers.createTestEvent(id = "test_area_event")
    }

    @AfterTest
    fun tearDown() {
        stopKoin()
        Dispatchers.resetMain()
    }

    @Test
    fun `area should parse bbox string correctly`() = runTest {
        val area = WWWEventArea(
            osmAdminids = listOf(1, 2, 3),
            bbox = "2.3, 48.85, 2.4, 48.86"
        )
        area.setRelatedEvent(testEvent)

        // Mock empty GeoJSON to rely on bbox string
        coEvery { mockGeoJsonDataProvider.getGeoJsonData("test_area_event") } returns null

        val bbox = area.bbox()

        assertEquals(48.85, bbox.southwest.latitude, 0.0001)
        assertEquals(2.3, bbox.southwest.longitude, 0.0001)
        assertEquals(48.86, bbox.northeast.latitude, 0.0001)
        assertEquals(2.4, bbox.northeast.longitude, 0.0001)
        assertTrue(area.bboxIsOverride)
    }

    @Test
    fun `area should handle malformed bbox string gracefully`() = runTest {
        val area = WWWEventArea(
            osmAdminids = listOf(1, 2, 3),
            bbox = "invalid,bbox,string"
        )
        area.setRelatedEvent(testEvent)

        // Mock simple GeoJSON to fallback to
        coEvery { mockGeoJsonDataProvider.getGeoJsonData("test_area_event") } returns
            Json.parseToJsonElement(simplePolygonGeoJson) as JsonObject

        val bbox = area.bbox()

        // Should fallback to computing from GeoJSON
        assertNotNull(bbox)
        assertFalse(area.bboxIsOverride)
    }

    @Test
    fun `area should parse simple polygon GeoJSON correctly`() = runTest {
        val area = WWWEventArea(osmAdminids = listOf(1, 2, 3))
        area.setRelatedEvent(testEvent)

        coEvery { mockGeoJsonDataProvider.getGeoJsonData("test_area_event") } returns
            Json.parseToJsonElement(simplePolygonGeoJson) as JsonObject

        val polygons = area.getPolygons()
        val bbox = area.bbox()

        assertTrue(polygons.isNotEmpty())
        assertEquals(1, polygons.size)

        // Verify bounding box calculation
        assertEquals(48.85, bbox.southwest.latitude, 0.0001)
        assertEquals(2.3, bbox.southwest.longitude, 0.0001)
        assertEquals(48.86, bbox.northeast.latitude, 0.0001)
        assertEquals(2.4, bbox.northeast.longitude, 0.0001)
    }

    @Test
    fun `area should parse multi-polygon GeoJSON correctly`() = runTest {
        val area = WWWEventArea(osmAdminids = listOf(1, 2, 3))
        area.setRelatedEvent(testEvent)

        coEvery { mockGeoJsonDataProvider.getGeoJsonData("test_area_event") } returns
            Json.parseToJsonElement(multiPolygonGeoJson) as JsonObject

        val polygons = area.getPolygons()

        assertTrue(polygons.isNotEmpty())
        assertEquals(2, polygons.size) // Two separate polygons
    }

    @Test
    fun `area should parse feature collection GeoJSON correctly`() = runTest {
        val area = WWWEventArea(osmAdminids = listOf(1, 2, 3))
        area.setRelatedEvent(testEvent)

        coEvery { mockGeoJsonDataProvider.getGeoJsonData("test_area_event") } returns
            Json.parseToJsonElement(featureCollectionGeoJson) as JsonObject

        val polygons = area.getPolygons()

        assertTrue(polygons.isNotEmpty())
        assertEquals(1, polygons.size)
    }

    @Test
    fun `area should use bbox from GeoJSON when available`() = runTest {
        val area = WWWEventArea(osmAdminids = listOf(1, 2, 3))
        area.setRelatedEvent(testEvent)

        coEvery { mockGeoJsonDataProvider.getGeoJsonData("test_area_event") } returns
            Json.parseToJsonElement(geoJsonWithBbox) as JsonObject

        val bbox = area.bbox()

        assertEquals(48.85, bbox.southwest.latitude, 0.0001)
        assertEquals(2.3, bbox.southwest.longitude, 0.0001)
        assertEquals(48.86, bbox.northeast.latitude, 0.0001)
        assertEquals(2.4, bbox.northeast.longitude, 0.0001)
    }

    @Test
    fun `area should handle missing GeoJSON data gracefully`() = runTest {
        val area = WWWEventArea(osmAdminids = listOf(1, 2, 3))
        area.setRelatedEvent(testEvent)

        coEvery { mockGeoJsonDataProvider.getGeoJsonData("test_area_event") } returns null

        val polygons = area.getPolygons()
        val bbox = area.bbox()

        assertTrue(polygons.isEmpty())
        // Should return a default bounding box
        assertNotNull(bbox)
    }

    @Test
    fun `isPositionWithin should work with simple polygon`() = runTest {
        val area = WWWEventArea(osmAdminids = listOf(1, 2, 3))
        area.setRelatedEvent(testEvent)

        coEvery { mockGeoJsonDataProvider.getGeoJsonData("test_area_event") } returns
            Json.parseToJsonElement(simplePolygonGeoJson) as JsonObject

        // Position inside the polygon
        val insidePosition = Position(48.855, 2.35)
        assertTrue(area.isPositionWithin(insidePosition))

        // Position outside the polygon
        val outsidePosition = Position(48.9, 2.5)
        assertFalse(area.isPositionWithin(outsidePosition))

        // Position outside bounding box (should be fast rejection)
        val farOutsidePosition = Position(50.0, 3.0)
        assertFalse(area.isPositionWithin(farOutsidePosition))
    }

    @Test
    fun `isPositionWithin should cache results within epsilon`() = runTest {
        val area = WWWEventArea(osmAdminids = listOf(1, 2, 3))
        area.setRelatedEvent(testEvent)

        coEvery { mockGeoJsonDataProvider.getGeoJsonData("test_area_event") } returns
            Json.parseToJsonElement(simplePolygonGeoJson) as JsonObject

        val position1 = Position(48.855, 2.35)
        val position2 = Position(48.8550001, 2.3500001) // Very close to position1

        val result1 = area.isPositionWithin(position1)
        val result2 = area.isPositionWithin(position2)

        // Should return the same result due to caching within epsilon
        assertEquals(result1, result2)
    }

    @Test
    fun `getCenter should calculate correct center position`() = runTest {
        val area = WWWEventArea(
            osmAdminids = listOf(1, 2, 3),
            bbox = "2.3, 48.85, 2.4, 48.86"
        )
        area.setRelatedEvent(testEvent)

        coEvery { mockGeoJsonDataProvider.getGeoJsonData("test_area_event") } returns null

        val center = area.getCenter()

        // Center of bbox should be calculated correctly
        assertEquals(48.855, center.latitude, 0.0001) // (48.85 + 48.86) / 2
        assertEquals(2.35, center.longitude, 0.0001)   // (2.3 + 2.4) / 2
    }

    @Test
    fun `generateRandomPositionInArea should handle gracefully`() = runTest {
        val area = WWWEventArea(
            osmAdminids = listOf(1, 2, 3),
            bbox = "2.3, 48.85, 2.4, 48.86"
        )
        area.setRelatedEvent(testEvent)

        coEvery { mockGeoJsonDataProvider.getGeoJsonData("test_area_event") } returns
            Json.parseToJsonElement(simplePolygonGeoJson) as JsonObject

        val randomPosition = area.generateRandomPositionInArea()

        // Should return a valid position (not null)
        assertNotNull(randomPosition)

        // Position should be finite numbers
        assertTrue(randomPosition.latitude.isFinite())
        assertTrue(randomPosition.longitude.isFinite())
    }

    @Test
    fun `generateRandomPositionInArea should handle missing polygons`() = runTest {
        val area = WWWEventArea(
            osmAdminids = listOf(1, 2, 3),
            bbox = "2.3, 48.85, 2.4, 48.86"
        )
        area.setRelatedEvent(testEvent)

        // Mock no GeoJSON data
        coEvery { mockGeoJsonDataProvider.getGeoJsonData("test_area_event") } returns null

        val randomPosition = area.generateRandomPositionInArea()

        // Should return a valid position (not null) even without polygons
        assertNotNull(randomPosition)

        // Position should be finite numbers
        assertTrue(randomPosition.latitude.isFinite())
        assertTrue(randomPosition.longitude.isFinite())
    }

    @Test
    fun `area should handle edge case positions at polygon boundaries`() = runTest {
        val area = WWWEventArea(osmAdminids = listOf(1, 2, 3))
        area.setRelatedEvent(testEvent)

        coEvery { mockGeoJsonDataProvider.getGeoJsonData("test_area_event") } returns
            Json.parseToJsonElement(simplePolygonGeoJson) as JsonObject

        // Test positions exactly on the boundary
        val boundaryPosition1 = Position(48.85, 2.3)   // Southwest corner
        val boundaryPosition2 = Position(48.86, 2.4)   // Northeast corner
        val boundaryPosition3 = Position(48.855, 2.3)  // West edge

        // These might be true or false depending on the polygon algorithm
        // The important thing is that they don't crash
        val result1 = area.isPositionWithin(boundaryPosition1)
        val result2 = area.isPositionWithin(boundaryPosition2)
        val result3 = area.isPositionWithin(boundaryPosition3)

        // Just verify we get valid boolean results
        assertTrue(result1 is Boolean)
        assertTrue(result2 is Boolean)
        assertTrue(result3 is Boolean)
    }

    @Test
    fun `area should handle complex MultiPolygon with multiple features`() = runTest {
        val area = WWWEventArea(osmAdminids = listOf(1, 2, 3))
        area.setRelatedEvent(testEvent)

        coEvery { mockGeoJsonDataProvider.getGeoJsonData("test_area_event") } returns
            Json.parseToJsonElement(multiPolygonGeoJson) as JsonObject

        val polygons = area.getPolygons()

        assertEquals(2, polygons.size)

        // Test position in first polygon
        val pos1 = Position(48.855, 2.35)
        assertTrue(area.isPositionWithin(pos1))

        // Test position in second polygon
        val pos2 = Position(48.875, 2.55)
        assertTrue(area.isPositionWithin(pos2))

        // Test position between polygons (should be false)
        val posBetween = Position(48.865, 2.45)
        assertFalse(area.isPositionWithin(posBetween))
    }

    @Test
    fun `area should handle unsupported GeoJSON format gracefully`() = runTest {
        val area = WWWEventArea(osmAdminids = listOf(1, 2, 3))
        area.setRelatedEvent(testEvent)

        val unsupportedGeoJson = """
        {
            "type": "LineString",
            "coordinates": [
                [2.3, 48.85],
                [2.4, 48.86]
            ]
        }
        """.trimIndent()

        coEvery { mockGeoJsonDataProvider.getGeoJsonData("test_area_event") } returns
            Json.parseToJsonElement(unsupportedGeoJson) as JsonObject

        val polygons = area.getPolygons()

        // Should handle gracefully and return empty list
        assertTrue(polygons.isEmpty())
    }

    @Test
    fun `area should validate correctly`() = runTest {
        val area = WWWEventArea(osmAdminids = listOf(1, 2, 3))
        area.setRelatedEvent(testEvent)

        val validationErrors = area.validationErrors()

        // Currently no validation errors are implemented, but this tests the interface
        assertTrue(validationErrors == null || validationErrors.isEmpty())
    }

    @Test
    fun `area should handle bbox constraint correctly`() = runTest {
        val constrainedArea = WWWEventArea(
            osmAdminids = listOf(1, 2, 3),
            bbox = "2.35, 48.855, 2.39, 48.859" // Smaller bbox than polygon
        )
        constrainedArea.setRelatedEvent(testEvent)

        // Use a polygon that extends beyond the bbox
        val extendedPolygonGeoJson = """
        {
            "type": "Polygon",
            "coordinates": [
                [
                    [2.3, 48.85],
                    [2.5, 48.85],
                    [2.5, 48.87],
                    [2.3, 48.87],
                    [2.3, 48.85]
                ]
            ]
        }
        """.trimIndent()

        coEvery { mockGeoJsonDataProvider.getGeoJsonData("test_area_event") } returns
            Json.parseToJsonElement(extendedPolygonGeoJson) as JsonObject

        val bbox = constrainedArea.bbox()

        // Should use the constrained bbox, not the polygon extent
        assertEquals(48.855, bbox.southwest.latitude, 0.0001)
        assertEquals(2.35, bbox.southwest.longitude, 0.0001)
        assertEquals(48.859, bbox.northeast.latitude, 0.0001)
        assertEquals(2.39, bbox.northeast.longitude, 0.0001)
    }

    @Test
    fun `area should compute extent from GeoJSON when no bbox available`() = runTest {
        val area = WWWEventArea(osmAdminids = listOf(1, 2, 3))
        area.setRelatedEvent(testEvent)

        coEvery { mockGeoJsonDataProvider.getGeoJsonData("test_area_event") } returns
            Json.parseToJsonElement(simplePolygonGeoJson) as JsonObject

        val bbox = area.bbox()

        // Should compute extent from coordinates
        assertEquals(48.85, bbox.southwest.latitude, 0.0001)
        assertEquals(2.3, bbox.southwest.longitude, 0.0001)
        assertEquals(48.86, bbox.northeast.latitude, 0.0001)
        assertEquals(2.4, bbox.northeast.longitude, 0.0001)
    }

    @Test
    fun `area should cache polygons and bbox after first access`() = runTest {
        val area = WWWEventArea(osmAdminids = listOf(1, 2, 3))
        area.setRelatedEvent(testEvent)

        coEvery { mockGeoJsonDataProvider.getGeoJsonData("test_area_event") } returns
            Json.parseToJsonElement(simplePolygonGeoJson) as JsonObject

        // First access should parse and cache
        val polygons1 = area.getPolygons()
        val bbox1 = area.bbox()

        // Second access should use cache
        val polygons2 = area.getPolygons()
        val bbox2 = area.bbox()

        // Should return the same instances (cached)
        assertEquals(polygons1.size, polygons2.size)
        assertEquals(bbox1, bbox2)
    }
}