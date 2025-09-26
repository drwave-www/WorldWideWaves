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

package com.worldwidewaves.shared.events.utils

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.random.Random

/**
 * Property-based tests using real WorldWideWaves geometric classes.
 * Verifies mathematical properties that must hold for all valid inputs.
 */
class RealGeometricPropertyTest {

    companion object {
        private const val PROPERTY_TEST_ITERATIONS = 100
        private const val FIXED_SEED = 42L
    }

    @Test
    fun `geoUtils_distanceCalculation_symmetryProperty`() {
        val random = Random(FIXED_SEED)

        repeat(PROPERTY_TEST_ITERATIONS) {
            val pointA = generateRandomValidPosition(random)
            val pointB = generateRandomValidPosition(random)

            val distAB = GeoUtils.calculateDistance(pointA, pointB)
            val distBA = GeoUtils.calculateDistance(pointB, pointA)

            assertTrue(
                kotlin.math.abs(distAB - distBA) < 1e-9,
                "Distance should be symmetric: d($pointA,$pointB)=$distAB != d($pointB,$pointA)=$distBA"
            )
        }
    }

    @Test
    fun `geoUtils_triangleInequality_property`() {
        val random = Random(FIXED_SEED)

        repeat(PROPERTY_TEST_ITERATIONS) {
            val pointA = generateRandomValidPosition(random)
            val pointB = generateRandomValidPosition(random)
            val pointC = generateRandomValidPosition(random)

            val distAB = GeoUtils.calculateDistance(pointA, pointB)
            val distBC = GeoUtils.calculateDistance(pointB, pointC)
            val distAC = GeoUtils.calculateDistance(pointA, pointC)

            // Triangle inequality: d(A,C) ≤ d(A,B) + d(B,C)
            assertTrue(
                distAC <= distAB + distBC + 1e-6, // Small epsilon for floating point
                "Triangle inequality violated: d($pointA,$pointC)=$distAC > d($pointA,$pointB)+d($pointB,$pointC)=${distAB + distBC}"
            )
        }
    }

    @Test
    fun `polygon_pointInPolygon_consistencyProperty`() {
        val random = Random(FIXED_SEED)

        repeat(PROPERTY_TEST_ITERATIONS / 2) { // Fewer iterations due to polygon complexity
            val polygon = generateRandomValidPolygon(random)
            val testPoint = generateRandomValidPosition(random)

            // Test multiple calls should give consistent results
            val result1 = PolygonUtils.containsPosition(polygon, testPoint)
            val result2 = PolygonUtils.containsPosition(polygon, testPoint)
            val result3 = PolygonUtils.containsPosition(polygon, testPoint)

            assertTrue(
                result1 == result2 && result2 == result3,
                "Point-in-polygon should be deterministic: results varied for polygon=$polygon, point=$testPoint"
            )
        }
    }

    @Test
    fun `boundingBox_containsAllVertices_property`() {
        val random = Random(FIXED_SEED)

        repeat(PROPERTY_TEST_ITERATIONS / 2) {
            val polygon = generateRandomValidPolygon(random)
            val boundingBox = polygon.getBounds()

            polygon.coordinates.forEach { vertex ->
                assertTrue(
                    vertex.lat >= boundingBox.minLat && vertex.lat <= boundingBox.maxLat &&
                        vertex.lng >= boundingBox.minLng && vertex.lng <= boundingBox.maxLng,
                    "Bounding box should contain all vertices: $vertex not in $boundingBox"
                )
            }
        }
    }

    @Test
    fun `polygonUtils_splitByLongitude_preservesArea`() {
        val random = Random(FIXED_SEED)

        repeat(PROPERTY_TEST_ITERATIONS / 4) { // Complex operation, fewer iterations
            val polygon = generateRandomValidPolygon(random, minSize = 4, maxSize = 8)
            val splitLongitude = random.nextDouble() * 360.0 - 180.0

            val splitResult = PolygonUtils.splitByLongitude(polygon, splitLongitude)

            if (splitResult.isNotEmpty()) {
                val originalArea = polygon.calculateArea()
                val totalSplitArea = splitResult.sumOf { it.calculateArea() }

                // Area should be approximately preserved (within 1% tolerance)
                val areaRatio = kotlin.math.abs(totalSplitArea - originalArea) / originalArea
                assertTrue(
                    areaRatio < 0.01,
                    "Split operation should preserve area: original=$originalArea, split=$totalSplitArea, ratio=$areaRatio"
                )
            }
        }
    }

    @Test
    fun `position_distanceToSelf_isZero`() {
        val random = Random(FIXED_SEED)

        repeat(PROPERTY_TEST_ITERATIONS) {
            val position = generateRandomValidPosition(random)
            val distanceToSelf = GeoUtils.calculateDistance(position, position)

            assertTrue(
                distanceToSelf < 1e-10,
                "Distance to self should be zero: $position has distance $distanceToSelf to itself"
            )
        }
    }

    @Test
    fun `polygon_centerPoint_alwaysInsideConvexPolygon`() {
        val random = Random(FIXED_SEED)

        repeat(PROPERTY_TEST_ITERATIONS / 2) {
            val polygon = generateRandomConvexPolygon(random)
            val center = polygon.getCenter()

            val isInside = PolygonUtils.containsPosition(polygon, center)

            assertTrue(
                isInside,
                "Center point should always be inside convex polygon: center=$center not in $polygon"
            )
        }
    }

    @Test
    fun `geoUtils_bearing_rangeProperty`() {
        val random = Random(FIXED_SEED)

        repeat(PROPERTY_TEST_ITERATIONS) {
            val pointA = generateRandomValidPosition(random)
            val pointB = generateRandomValidPosition(random)

            val bearing = GeoUtils.calculateBearing(pointA, pointB)

            // Bearing should always be in range [0, 360)
            assertTrue(
                bearing >= 0.0 && bearing < 360.0,
                "Bearing should be in [0, 360) range: got $bearing for $pointA to $pointB"
            )
        }
    }

    // Helper functions for generating test data

    private fun generateRandomValidPosition(random: Random): Position {
        val lat = random.nextDouble() * 180.0 - 90.0  // -90 to 90
        val lng = random.nextDouble() * 360.0 - 180.0 // -180 to 180
        return Position(lat = lat, lng = lng)
    }

    private fun generateRandomValidPolygon(
        random: Random,
        minSize: Int = 3,
        maxSize: Int = 10
    ): Polygon {
        val numVertices = minSize + random.nextInt(maxSize - minSize + 1)
        val centerLat = random.nextDouble() * 60.0 - 30.0 // -30 to 30
        val centerLng = random.nextDouble() * 120.0 - 60.0 // -60 to 60
        val radius = 0.01 + random.nextDouble() * 0.1 // 0.01 to 0.11 degrees

        val vertices = (0 until numVertices).map { i ->
            val angle = 2 * kotlin.math.PI * i / numVertices
            Position(
                lat = centerLat + radius * kotlin.math.cos(angle),
                lng = centerLng + radius * kotlin.math.sin(angle)
            )
        }

        return Polygon(coordinates = vertices + vertices.first()) // Close polygon
    }

    private fun generateRandomConvexPolygon(
        random: Random,
        numVertices: Int = 6
    ): Polygon {
        val centerLat = random.nextDouble() * 60.0 - 30.0
        val centerLng = random.nextDouble() * 120.0 - 60.0
        val radius = 0.01 + random.nextDouble() * 0.05

        // Generate points in angular order to ensure convexity
        val vertices = (0 until numVertices).map { i ->
            val angle = 2 * kotlin.math.PI * i / numVertices
            val r = radius * (0.8 + random.nextDouble() * 0.4) // Slight radius variation
            Position(
                lat = centerLat + r * kotlin.math.cos(angle),
                lng = centerLng + r * kotlin.math.sin(angle)
            )
        }

        return Polygon(coordinates = vertices + vertices.first())
    }
}