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

import com.worldwidewaves.shared.events.utils.PolygonUtils.splitByLongitude
import io.github.aakira.napier.Napier
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PolygonUtilsSplitPolygonTest {

    /**
     *
     */
    @Test
    fun testSplitPolygonByLongitude() {
        // GIVEN
        val polygon = Polygon.fromPositions(
            Position(lat = -12.0, lng = -6.0),
            Position(lat = -13.0, lng = -3.0),
            Position(lat = -11.0, lng = -3.0),
            Position(lat = -9.0, lng = -3.0),
            Position(lat = -8.0, lng = -6.0),
            Position(lat = -7.0, lng = -3.0),
            Position(lat = -6.0, lng = -6.0),
            Position(lat = -5.0, lng = -3.0),
            Position(lat = -3.0, lng = 0.0),
            Position(lat = -2.0, lng = -3.0),
            Position(lat = -1.0, lng = 2.0),
            Position(lat = 1.0, lng = 2.0),
            Position(lat = 3.0, lng = -8.0),
            Position(lat = 5.0, lng = -8.0),
            Position(lat = 7.0, lng = -7.0),
            Position(lat = 8.0, lng = -5.0),
            Position(lat = 9.0, lng = -1.0),
            Position(lat = 9.0, lng = 2.0),
            Position(lat = 14.0, lng = 2.0),
            Position(lat = 14.0, lng = -5.0),
            Position(lat = 12.0, lng = -5.0),
            Position(lat = 12.0, lng = -1.0),
            Position(lat = 10.0, lng = 1.0),
            Position(lat = 10.0, lng = -7.0),
            Position(lat = 10.0, lng = -9.0),
            Position(lat = -11.0, lng = -9.0)
        )

        val expectedLeftSide1 = Polygon.fromPositions(
            Position(lat = -12.0, lng = -6.0),
            Position(lat = -13.0, lng = -3.0), // <- cut
            Position(lat = -11.0, lng = -3.0), // <- cut
            Position(lat = -9.0, lng = -3.0), // <- cut
            Position(lat = -8.0, lng = -6.0),
            Position(lat = -7.0, lng = -3.0), // <- cut
            Position(lat = -6.0, lng = -6.0),
            Position(lat = -4.5, lng = -3.0), // <- cut
            Position(lat = -2.0, lng = -3.0),
            Position(lat = 2.0, lng = -3.0), // <- cut
            Position(lat = 3.0, lng = -8.0),
            Position(lat = 5.0, lng = -8.0),
            Position(lat = 7.0, lng = -7.0),
            Position(lat = 8.0, lng = -5.0),
            Position(lat = 8.5, lng = -3.0), // <- cut
            Position(lat = 10.0, lng = -3.0), // <- cut
            Position(lat = 10.0, lng = -7.0),
            Position(lat = 10.0, lng = -9.0),
            Position(lat = -11.0, lng = -9.0)
        )

        val expectedLeftSide2 = Polygon.fromPositions(
            Position(lat = 12.0, lng = -5.0),
            Position(lat = 12.0, lng = -3.0), // <- cut
            Position(lat = 14.0, lng = -3.0), // <- cut
            Position(lat = 14.0, lng = -5.0),
            Position(lat = 12.0, lng = -5.0)
        )

        val expectedRightSide1 = Polygon.fromPositions(
            Position(lat = -4.5, lng = -3.0), // <- cut
            Position(lat = -3.0, lng = 0.0),
            Position(lat = -2.0, lng = -3.0), // <- cut
            Position(lat = -5.0, lng = -3.0)
        )

        val expectedRightSide2 = Polygon.fromPositions(
            Position(lat = -2.0, lng = -3.0), // <- cut
            Position(lat = -1.0, lng = 2.0),
            Position(lat = 1.0, lng = 2.0),
            Position(lat = 2.0, lng = -3.0), // <- cut
            Position(lat = -2.0, lng = -3.0)
        )

        val expectedRightSide3 = Polygon.fromPositions(
            Position(lat = 8.5, lng = -3.0), // <- cut
            Position(lat = 9.0, lng = -1.0),
            Position(lat = 9.0, lng = 2.0),
            Position(lat = 14.0, lng = 2.0),
            Position(lat = 14.0, lng = -3.0), // <- cut
            Position(lat = 12.0, lng = -3.0), // <- cut
            Position(lat = 12.0, lng = -1.0),
            Position(lat = 10.0, lng = 1.0),
            Position(lat = 10.0, lng = -3.0), // <- cut
            Position(lat = 8.5, lng = -3.0)
        )

        // WHEN
        val longitudeToCut = -3.0
        val result = polygon.splitByLongitude(longitudeToCut)

        // THEN
        assertEquals(2,result.left.size)
        assertEquals(19, result.left[0].size)
        assertEquals(8, result.left[0].cutPositions.size)
        assertTrue(areRingPolygonsEqual(result.left[0], expectedLeftSide1))
        assertEquals(5, result.left[1].size)
        assertEquals(2, result.left[1].cutPositions.size)
        assertTrue(areRingPolygonsEqual(result.left[1], expectedLeftSide2))
        assertEquals(3, result.right.size)
        assertEquals(4, result.right[0].size)
        assertEquals(2, result.right[0].cutPositions.size)
        assertTrue(areRingPolygonsEqual(result.right[0], expectedRightSide1))
        assertEquals(5, result.right[1].size)
        assertEquals(2, result.right[1].cutPositions.size)
        assertTrue(areRingPolygonsEqual(result.right[1], expectedRightSide2))
        assertEquals(10, result.right[2].size)
        assertEquals(4, result.right[2].cutPositions.size)
        assertTrue(areRingPolygonsEqual(result.right[2], expectedRightSide3))
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
        val minPoint = polygon.minWithOrNull(compareBy({ it.lat }, { it.lng }))
            ?: return polygon

        // Rotate the polygon to start from the smallest point
        return (
                polygon.subList(minPoint, polygon.last()!!.id) +
                        polygon.subList(polygon.last()!!, minPoint.id)
                )
    }

    private fun removeRepeatingPoint(polygon: Polygon): Polygon {
        if (polygon.size > 1 && polygon.first() == polygon.last()) {
            return polygon.dropLast()
        }
        return polygon
    }

}
