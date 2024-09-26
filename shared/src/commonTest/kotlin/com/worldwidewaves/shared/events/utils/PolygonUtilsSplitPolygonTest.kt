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

import com.worldwidewaves.shared.events.utils.PolygonUtils.recomposeCutPolygons
import com.worldwidewaves.shared.events.utils.PolygonUtils.splitByLongitude
import com.worldwidewaves.shared.events.utils.polygon_testcases.PolygonUtilsTestCases
import com.worldwidewaves.shared.events.utils.polygon_testcases.PolygonUtilsTestCases.TestCasePolygon
import io.github.aakira.napier.Antilog
import io.github.aakira.napier.LogLevel
import io.github.aakira.napier.Napier
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PolygonUtilsSplitPolygonTest {

    @BeforeTest
    fun setUp() {
        Napier.base(object : Antilog() {
            override fun performLog(priority: LogLevel, tag: String?, throwable: Throwable?, message: String?) {
                println(message)
            }
        })
    }

    @Test
    fun testSplitPolygonByLongitude() = runTest {
        PolygonUtilsTestCases.testCases.filterIndexed { idx, _ -> idx == 4 }.forEachIndexed { idx, testCase ->
            val result = testSplitPolygonCase(idx, testCase)
            testRecomposePolygonCase(idx, testCase, result)
        }
    }

    private fun testSplitPolygonCase(idx: Int, testCase: TestCasePolygon): PolygonUtils.PolygonSplitResult {
        Napier.i("==> Testing split of polygon testcase $idx")

        val result = when {
            testCase.longitudeToCut != null -> testCase.polygon.splitByLongitude(testCase.longitudeToCut)
            testCase.composedLongitudeToCut != null -> testCase.polygon.splitByLongitude(testCase.composedLongitudeToCut)
            else -> throw IllegalArgumentException("Invalid test case")
        }

        listOf(
            Pair(TestCasePolygon::leftExpected, result.left),
            Pair(TestCasePolygon::rightExpected, result.right)
        ).forEach { (selector, result) ->
            val expectedPolygons = selector(testCase)
            assertEquals(expectedPolygons.size, result.size)
            expectedPolygons.forEachIndexed { index, expectedPolygon ->
                assertEquals(expectedPolygon.polygon.size, result[index].size)
                assertEquals(expectedPolygon.nbCutPositions, result[index].cutPositions.size)
                assertTrue(areRingPolygonsEqual(expectedPolygon.polygon, result[index]))
            }
        }

        return result
    }

    private fun testRecomposePolygonCase(idx: Int, testCase: TestCasePolygon, result: PolygonUtils.PolygonSplitResult) {
        Napier.i("==> Testing recompose of polygon testcase $idx")

        val recomposedPolygons = recomposeCutPolygons(result.left + result.right)

        assertEquals(1, recomposedPolygons.size)
        assertEquals(0, recomposedPolygons[0].cutPositions.size)
        assertEquals(testCase.recomposedPolygon.size, recomposedPolygons[0].size)
        assertTrue(areRingPolygonsEqual(testCase.recomposedPolygon, recomposedPolygons[0]))
    }

    // ------------------------------------------------------------------------

    private fun areRingPolygonsEqual(polygon1: Polygon, polygon2: Polygon): Boolean {
        if (polygon1.size != polygon2.size) {
            Napier.d("Polygons are not equal: different sizes. Polygon1 size: ${polygon1.size}, Polygon2 size: ${polygon2.size}")
            return false
        }

        // Remove the repeating point from the end of each polygon
        val cleanedPolygon1 = if (polygon1.isClockwise() != polygon2.isClockwise()) {
            removeRepeatingPoint(polygon1.inverted())
        } else {
            removeRepeatingPoint(polygon1)
        }
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
        return if (polygon.last() == minPoint) {
            polygon.subList(polygon.last()!!, polygon.last()!!.id) +
                    polygon.subList(polygon.first()!!, polygon.last()!!.id)
        } else {
            polygon.subList(minPoint, polygon.last()!!.id) +
                    polygon.subList(polygon.last()!!, minPoint.id)
        }
    }

    private fun removeRepeatingPoint(polygon: Polygon): Polygon {
        if (polygon.size > 1 && polygon.first() == polygon.last()) {
            return polygon.withoutLast()
        }
        return polygon
    }

}
