package com.worldwidewaves.shared.events.utils

import com.worldwidewaves.shared.events.utils.PolygonUtils.splitPolygonByLongitude
import io.github.aakira.napier.Napier
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
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

class MapAreaUtilsSplitPolygonTest {

    @Test
    fun testSplitPolygonByLongitude() {
        val polygon = polygonOf(
            Position(lat = 0.0, lng = 0.0),
            Position(lat = 0.0, lng = 2.0),
            Position(lat = 2.0, lng = 2.0),
            Position(lat = 2.0, lng = 0.0),
            Position(lat = 0.0, lng = 0.0)
        )
        val longitudeToCut = 1.0

        val result = splitPolygonByLongitude(polygon, longitudeToCut)

        val expectedLeftSide = polygonOf(
            Position(lat = 0.0, lng = 0.0),
            Position(lat = 0.0, lng = 1.0),
            Position(lat = 2.0, lng = 1.0),
            Position(lat = 2.0, lng = 0.0),
            Position(lat = 0.0, lng = 0.0)
        )
        val expectedRightSide = polygonOf(
            Position(lat = 0.0, lng = 1.0),
            Position(lat = 0.0, lng = 2.0),
            Position(lat = 2.0, lng = 2.0),
            Position(lat = 2.0, lng = 1.0),
            Position(lat = 0.0, lng = 1.0)
        )

        assertTrue(result.left.size == 1 && areRingPolygonsEqual(result.left[0], expectedLeftSide), "Expected left side of the split polygon to match")
        assertTrue(result.right.size == 1 && areRingPolygonsEqual(result.right[0], expectedRightSide), "Expected right side of the split polygon to match")
    }

    @Test
    fun testSplitPolygonByLongitudeWithVertexOnCut() {
        val polygon = polygonOf(
            Position(lat = 0.0, lng = 0.0),
            Position(lat = 0.0, lng = 1.0),
            Position(lat = 1.0, lng = 1.0),
            Position(lat = 1.0, lng = 0.0),
            Position(lat = 0.0, lng = 0.0)
        )
        val longitudeToCut = 1.0

        val result = splitPolygonByLongitude(polygon, longitudeToCut)

        val expectedLeftSide = polygonOf(
            Position(lat = 0.0, lng = 0.0),
            Position(lat = 0.0, lng = 1.0),
            Position(lat = 1.0, lng = 1.0),
            Position(lat = 1.0, lng = 0.0),
            Position(lat = 0.0, lng = 0.0)
        )

        assertTrue(result.left.size == 1 && areRingPolygonsEqual(result.left[0], expectedLeftSide), "Expected left side of the split polygon to match")
        assertTrue(result.right.isEmpty(), "Expected right side of the split polygon to be empty")
    }

    @Test
    fun testSplitPolygonByLongitudeWithDegenerateShape() {
        val polygon = polygonOf(
            Position(lat = 1.0, lng = 0.0),
            Position(lat = 1.0, lng = 1.0),
            Position(lat = 1.0, lng = 2.0),
            Position(lat = 1.0, lng = 3.0),
            Position(lat = 1.0, lng = 0.0)
        )
        val longitudeToCut = 1.0

        val result = splitPolygonByLongitude(polygon, longitudeToCut)

        assertEquals(emptyList(), result.left, "Expected left side of the split polygon to be empty")
        assertEquals(emptyList(), result.right, "Expected right side of the split polygon to be empty")
    }

    @Test
    fun testSplitPolygonByLongitude_SinglePointPolygon() {
        val polygon = polygonOf(Position(0.0, 0.0))
        val longitudeToCut = 1.0
        val result = splitPolygonByLongitude(polygon, longitudeToCut)
        assertEquals(emptyList(), result.left)
        assertEquals(emptyList(), result.right)
    }

    @Test
    fun testSplitPolygonByLongitude_TwoPointPolygon() {
        val polygon = polygonOf(Position(0.0, 0.0), Position(0.0, 1.0))
        val longitudeToCut = 1.0
        val result = splitPolygonByLongitude(polygon, longitudeToCut)

        assertEquals(emptyList(), result.left)
        assertEquals(emptyList(), result.right)
    }

    @Test
    fun testSplitPolygonByLongitude_AllPointsOnCutLine() {val polygon = polygonOf(
        Position(0.0, 1.0),
        Position(1.0, 1.0),
        Position(2.0, 1.0),
        Position(0.0, 1.0)
    )
        val longitudeToCut = 1.0
        val result = splitPolygonByLongitude(polygon, longitudeToCut)

        assertEquals(emptyList(), result.left)
        assertEquals(emptyList(), result.right)
    }

    @Test
    fun testSplitPolygonByLongitude_ComplexShape1() {
        val polygon = polygonOf(
            Position(0.0, 0.0),
            Position(0.0, 3.0),
            Position(1.0, 2.0),
            Position(2.0, 3.0),
            Position(2.0, 0.0),
            Position(0.0, 0.0)
        )
        val longitudeToCut = 1.0
        val result = splitPolygonByLongitude(polygon, longitudeToCut)

        assertTrue(result.left.all { it.first() == it.last() })
        assertTrue(result.right.all { it.first() == it.last() })
    }

    @Test
    fun testSplitPolygonByLongitude_ComplexShape2() {
        val polygon = polygonOf(
            Position(0.0, 0.0),
            Position(1.0, 1.0),
            Position(2.0, 0.0),
            Position(2.0, 2.0),
            Position(1.0, 3.0),
            Position(0.0, 2.0),
            Position(0.0, 0.0)
        )
        val longitudeToCut = 1.5
        val result = splitPolygonByLongitude(polygon, longitudeToCut)

        assertTrue(result.left.all { it.first() == it.last() })
        assertTrue(result.right.all { it.first() == it.last() })
    }

    @Test
    fun testSplitPolygonByLongitude_LongitudeOutsidePolygon() {
        val polygon = polygonOf(
            Position(0.0, 0.0),
            Position(0.0, 1.0),
            Position(1.0, 1.0),
            Position(1.0, 0.0),
            Position(0.0, 0.0)
        )
        val longitudeToCut = 2.0
        val result = splitPolygonByLongitude(polygon, longitudeToCut)

        assertTrue(areRingPolygonsEqual(polygon, result.left.first()))
        assertEquals(emptyList(), result.right)
    }

    @Test
    fun testSplitPolygonByLongitude_EmptyPolygon() {
        val polygon = Polygon()
        val longitudeToCut = 1.0
        val result = splitPolygonByLongitude(polygon, longitudeToCut)

        assertEquals(emptyList(), result.left)
        assertEquals(emptyList(), result.right)
    }

    @Test
    fun testSplitLargePolygonWithConcaveSections() {
        val polygon = polygonOf(
            Position(0.0, 0.0),
            Position(0.0, 5.0),
            Position(2.0, 3.0),
            Position(4.0, 5.0),
            Position(4.0, 0.0),
            Position(3.0,1.0),
            Position(1.0, 1.0),
            Position(0.0, 0.0)
        )
        val longitudeToCut = 2.5
        val result = splitPolygonByLongitude(polygon, longitudeToCut)

        assertTrue(result.left.isNotEmpty())
        assertTrue(result.right.isNotEmpty())
        assertTrue(result.left.all { it.first() == it.last() })
        assertTrue(result.right.all { it.first() == it.last() })
    }

    @Test
    fun testSplitPolygonWithZigzagPattern() {
        val polygon = polygonOf(
            Position(0.0, 0.0),
            Position(0.0, 2.0),
            Position(1.0, 1.0),
            Position(1.0, 3.0),
            Position(2.0, 2.0),
            Position(2.0, 0.0),
            Position(0.0, 0.0)
        )

        val longitudeToCut = 1.5
        val result = splitPolygonByLongitude(polygon, longitudeToCut)

        val expectedLeftSide = polygonOf(
            Position(lat = 0.0, lng = 0.0),
            Position(lat = 0.0, lng = 1.5),
            Position(lat = 0.5, lng = 1.5),
            Position(lat = 1.0, lng = 1.0),
            Position(lat = 1.0, lng = 1.5),
            Position(lat = 2.0, lng = 1.5),
            Position(lat = 2.0, lng = 0.0),
            Position(lat = 0.0, lng = 0.0)
        )

        val expectedRightSide1 = polygonOf(
            Position(lat = 0.0, lng = 1.5),
            Position(lat = 0.0, lng = 2.0),
            Position(lat = 0.5, lng = 1.5),
            Position(lat = 0.0, lng = 1.5)
        )
        val expectedRightSide2 = polygonOf(
            Position(lat = 1.0, lng = 1.5),
            Position(lat = 1.0, lng = 3.0),
            Position(lat = 2.0, lng = 2.0),
            Position(lat = 2.0, lng = 1.5),
            Position(lat = 1.0, lng = 1.5)
        )

        assertTrue(result.left.isNotEmpty())
        assertTrue(result.right.isNotEmpty())
        assertTrue(areRingPolygonsEqual(expectedLeftSide, result.left[0]), "Expected left side of the split polygon to match")
        assertTrue(areRingPolygonsEqual(expectedRightSide1, result.right[0]), "Expected right side of the split polygon to match")
        assertTrue(areRingPolygonsEqual(expectedRightSide2, result.right[1]), "Expected right side of the split polygon to match")
    }

    @Test
    fun testSplitPolygonWithSpiralShape() {
        val polygon = Polygon(Position(0.0, 0.0))
        var angle = 0.0
        val radius = 1.0
        val numPoints = 36
        for (i in 1..numPoints) {
            angle += PI / 12
            val x = radius * cos(angle)
            val y = radius * sin(angle)
            polygon.add(Position(x, y))
        }
        polygon.add(Position(0.0, 0.0)) // Close the polygon

        val longitudeToCut = 0.5
        val result = splitPolygonByLongitude(polygon, longitudeToCut)

        assertTrue(result.left.isNotEmpty())
        assertTrue(result.right.isNotEmpty())
        assertTrue(result.left.all { it.first() == it.last() })
        assertTrue(result.right.all { it.first() == it.last() })
    }

    @Test
    fun testSplitPolygonWithMultipleIntersections() {
        val polygon = polygonOf(
            Position(0.0, 0.0),
            Position(0.0, 4.0),
            Position(1.0, 3.0),
            Position(2.0, 4.0),
            Position(2.0, 2.0),
            Position(3.0, 3.0),
            Position(4.0, 2.0),
            Position(4.0, 0.0),
            Position(0.0, 0.0)
        )
        val longitudeToCut = 2.5
        val result = splitPolygonByLongitude(polygon, longitudeToCut)

        assertTrue(result.left.isNotEmpty())
        assertTrue(result.right.isNotEmpty())
        assertTrue(result.left.all { it.first() == it.last() })
        assertTrue(result.right.all { it.first() == it.last() })
    }

    @Test
    fun testSplitConcavePolygon_MultipleRightRings() {
        val polygon = polygonOf(
        Position(0.0, 0.0),
        Position(0.0, 2.0),
        Position(1.0, 1.0),
        Position(2.0, 2.0),
        Position(2.0, 0.0),
        Position(0.0, 0.0)
        )

        val longitudeToCut = 1.0
        val result = splitPolygonByLongitude(polygon, longitudeToCut)

        val expectedLeftSide = polygonOf(
            Position(0.0, 0.0),
            Position(0.0, 1.0),
            Position(1.0, 1.0),
            Position(2.0, 1.0),
            Position(2.0, 0.0),
            Position(0.0, 0.0)
        )
        val expectedRightSide1 = polygonOf(
            Position(0.0, 1.0),
            Position(0.0, 2.0),
            Position(1.0, 1.0),
            Position(0.0, 1.0) // Closed ring polygon
        )
        val expectedRightSide2 = polygonOf(
            Position(1.0, 1.0),
            Position(2.0, 2.0),
            Position(2.0, 1.0),
            Position(1.0, 1.0) // Closed ring polygon
        )

        assertTrue(areRingPolygonsEqual(expectedLeftSide, result.left[0]), "Expected left side of the split polygon to match")
        assertTrue(areRingPolygonsEqual(expectedRightSide1, result.right[0]), "Expected right side of the split polygon to match")
        assertTrue(areRingPolygonsEqual(expectedRightSide2, result.right[1]), "Expected right side of the split polygon to match")
    }

    // -----------------------

    private fun areRingPolygonsEqual(polygon1: Polygon, polygon2: Polygon): Boolean {
        if (polygon1.size != polygon2.size) {
            Napier.d("Polygons are not equal: different sizes. Polygon1 size: ${polygon1.size}, Polygon2 size: ${polygon2.size}")
            return false
        }

        // Remove the repeating point from the end of each polygon
        val cleanedPolygon1 = removeRepeatingPoint(polygon1)
        val cleanedPolygon2 = removeRepeatingPoint(polygon2)

        // Normalize both polygons to start from the same point
        val normalizedPolygon1 = normalizePolygon(cleanedPolygon1)
        val normalizedPolygon2 = normalizePolygon(cleanedPolygon2)

        // Check if all points match
        var point2 = normalizedPolygon2.first()
        for (point in normalizedPolygon1) {
            if (point != point2) {
                Napier.d("Polygons are not equal: mismatch at index ${point.id}. Polygon1: $normalizedPolygon1, Polygon2: $normalizedPolygon2")
                return false
            }
            point2 = point2.next
        }

        return true
    }

    private fun <T: Polygon> normalizePolygon(polygon: T): Polygon {
        if (polygon.isEmpty()) return polygon

        // Find the smallest point lexicographically to use as the starting point
        val minPoint = polygon.minWithOrNull(compareBy({ it.lat }, { it.lng })) ?: return polygon

        // Rotate the polygon to start from the smallest point
        return (
                polygon.subList(minPoint, polygon.last()!!.id) +
                        polygon.subList(polygon.last()!!, minPoint.id)
                )
    }

    private fun removeRepeatingPoint(polygon: Polygon): Polygon {
        if (polygon.size > 1 && polygon.first() == polygon.last()) {
            return polygon.dropLast(1)
        }
        return polygon
    }

}
