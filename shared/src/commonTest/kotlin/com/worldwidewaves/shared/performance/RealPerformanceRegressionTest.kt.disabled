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

package com.worldwidewaves.shared.performance

import com.worldwidewaves.shared.events.utils.Position
import com.worldwidewaves.shared.events.utils.Polygon
import com.worldwidewaves.shared.events.utils.PolygonUtils
import com.worldwidewaves.shared.events.utils.GeoUtils
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.measureTime
import kotlin.time.Duration.Companion.milliseconds

/**
 * Real performance regression tests using actual WorldWideWaves algorithms.
 * Establishes performance baselines and detects regressions.
 */
class RealPerformanceRegressionTest {

    companion object {
        // Performance budgets based on actual requirements
        const val POINT_IN_POLYGON_BUDGET_MS = 2L
        const val DISTANCE_CALCULATION_BUDGET_MS = 1L
        const val POLYGON_SPLIT_BUDGET_MS = 50L
        const val BATCH_OPERATION_BUDGET_MS = 100L
    }

    @Test
    fun `pointInPolygon_realWorldPolygon_withinPerformanceBudget`() {
        // Use realistic Central Park polygon approximation
        val centralParkPolygon = createCentralParkPolygon()
        val testPoint = Position(lat = 40.7831, lng = -73.9712) // Inside Central Park

        val duration = measureTime {
            PolygonUtils.containsPosition(centralParkPolygon, testPoint)
        }

        assertTrue(
            duration < POINT_IN_POLYGON_BUDGET_MS.milliseconds,
            "Point-in-polygon for realistic polygon took ${duration.inWholeMilliseconds}ms, exceeds budget of ${POINT_IN_POLYGON_BUDGET_MS}ms"
        )
    }

    @Test
    fun `distanceCalculation_realWorldDistances_withinBudget`() {
        // Real world distances: NYC to LA, London to Paris, etc.
        val cityPairs = listOf(
            Position(lat = 40.7831, lng = -73.9712) to Position(lat = 34.0522, lng = -118.2437), // NYC to LA
            Position(lat = 51.5074, lng = -0.1278) to Position(lat = 48.8566, lng = 2.3522), // London to Paris
            Position(lat = 35.6762, lng = 139.6503) to Position(lat = 37.7749, lng = -122.4194) // Tokyo to SF
        )

        cityPairs.forEach { (city1, city2) ->
            val duration = measureTime {
                GeoUtils.calculateDistance(city1, city2)
            }

            assertTrue(
                duration < DISTANCE_CALCULATION_BUDGET_MS.milliseconds,
                "Distance calculation took ${duration.inWholeMilliseconds}ms, exceeds budget"
            )
        }
    }

    @Test
    fun `polygonSplitting_complexRealWorldPolygon_withinBudget`() {
        // Create complex polygon representing a real city boundary
        val complexCityPolygon = createComplexCityPolygon()
        val splitLongitude = -73.9712 // Longitude through NYC

        val duration = measureTime {
            PolygonUtils.splitByLongitude(complexCityPolygon, splitLongitude)
        }

        assertTrue(
            duration < POLYGON_SPLIT_BUDGET_MS.milliseconds,
            "Complex polygon splitting took ${duration.inWholeMilliseconds}ms, exceeds budget of ${POLYGON_SPLIT_BUDGET_MS}ms"
        )
    }

    @Test
    fun `batchPositionProcessing_thousandPositions_withinBudget`() {
        val testPolygon = createCentralParkPolygon()
        val testPositions = generateNYCAreaPositions(1000)

        val duration = measureTime {
            testPositions.forEach { position ->
                PolygonUtils.containsPosition(testPolygon, position)
            }
        }

        assertTrue(
            duration < BATCH_OPERATION_BUDGET_MS.milliseconds,
            "Batch processing 1000 positions took ${duration.inWholeMilliseconds}ms, exceeds budget of ${BATCH_OPERATION_BUDGET_MS}ms"
        )
    }

    @Test
    fun `waveProgression_calculation_scalingPerformance`() {
        val waveEvent = createTestWaveEvent()
        val testPosition = Position(lat = 40.7831, lng = -73.9712)
        val testTime = kotlinx.datetime.Clock.System.now()

        // Test scaling: 1, 10, 100 calculations
        val iterationCounts = listOf(1, 10, 100)
        val timingResults = mutableListOf<Pair<Int, Long>>()

        iterationCounts.forEach { iterations ->
            val duration = measureTime {
                repeat(iterations) {
                    waveEvent.wave.calculateProgression(testPosition, testTime)
                }
            }

            timingResults.add(iterations to duration.inWholeMilliseconds)
        }

        // Performance should scale linearly (allowing for some overhead)
        val (one, oneTime) = timingResults[0]
        val (hundred, hundredTime) = timingResults[2]

        val expectedTime = oneTime * (hundred / one)
        val actualOverhead = hundredTime.toDouble() / expectedTime

        assertTrue(
            actualOverhead < 2.0, // No more than 2x overhead
            "Wave progression scaling: 1x=$oneTime ms, 100x=$hundredTime ms, overhead=${actualOverhead}x"
        )
    }

    @Test
    fun `memoryUsage_largeDatasets_staysWithinBounds`() {
        val runtime = Runtime.getRuntime()
        runtime.gc()

        val initialMemory = runtime.totalMemory() - runtime.freeMemory()

        // Process large datasets
        repeat(100) {
            val largePolygon = createComplexCityPolygon()
            val testPositions = generateNYCAreaPositions(100)

            testPositions.forEach { position ->
                PolygonUtils.containsPosition(largePolygon, position)
            }
        }

        runtime.gc()
        val finalMemory = runtime.totalMemory() - runtime.freeMemory()
        val memoryIncrease = finalMemory - initialMemory

        // Memory increase should be reasonable (less than 20MB)
        val maxMemoryIncreaseMB = 20
        val actualIncreaseMB = memoryIncrease / (1024 * 1024)

        assertTrue(
            actualIncreaseMB < maxMemoryIncreaseMB,
            "Memory usage increased by ${actualIncreaseMB}MB, exceeds limit of ${maxMemoryIncreaseMB}MB"
        )
    }

    // Helper functions for creating realistic test data

    private fun createCentralParkPolygon(): Polygon {
        // Approximate Central Park boundaries
        val centralParkCoords = listOf(
            Position(lat = 40.7829, lng = -73.9654), // SE corner
            Position(lat = 40.7829, lng = -73.9812), // SW corner
            Position(lat = 40.7972, lng = -73.9812), // NW corner
            Position(lat = 40.7972, lng = -73.9654), // NE corner
            Position(lat = 40.7829, lng = -73.9654)  // Close
        )
        return Polygon(coordinates = centralParkCoords)
    }

    private fun createComplexCityPolygon(): Polygon {
        // Complex polygon with 20+ vertices representing irregular city boundary
        val random = kotlin.random.Random(42) // Fixed seed
        val baseCoords = listOf(
            Position(lat = 40.7831, lng = -73.9712),
            Position(lat = 40.7851, lng = -73.9692),
            Position(lat = 40.7871, lng = -73.9672),
            Position(lat = 40.7891, lng = -73.9652),
            Position(lat = 40.7891, lng = -73.9632),
            Position(lat = 40.7871, lng = -73.9612),
            Position(lat = 40.7851, lng = -73.9592),
            Position(lat = 40.7831, lng = -73.9572),
            Position(lat = 40.7811, lng = -73.9592),
            Position(lat = 40.7791, lng = -73.9612),
            Position(lat = 40.7771, lng = -73.9632),
            Position(lat = 40.7771, lng = -73.9652),
            Position(lat = 40.7791, lng = -73.9672),
            Position(lat = 40.7811, lng = -73.9692),
            Position(lat = 40.7831, lng = -73.9712) // Close
        )

        // Add some irregularity
        val irregularCoords = baseCoords.map { pos ->
            Position(
                lat = pos.lat + (random.nextDouble() - 0.5) * 0.001,
                lng = pos.lng + (random.nextDouble() - 0.5) * 0.001
            )
        }

        return Polygon(coordinates = irregularCoords)
    }

    private fun generateNYCAreaPositions(count: Int): List<Position> {
        val random = kotlin.random.Random(42)
        return (1..count).map {
            Position(
                lat = 40.7 + random.nextDouble() * 0.2, // NYC area
                lng = -74.1 + random.nextDouble() * 0.2
            )
        }
    }

    private fun createTestWaveEvent(): WWWEvent {
        return WWWEvent.create(
            id = "performance-test-wave",
            name = "Performance Test Wave",
            centerPosition = Position(lat = 40.7831, lng = -73.9712),
            radiusKm = 5.0,
            startTime = kotlinx.datetime.Clock.System.now(),
            waveSpeedKmh = 50.0
        )
    }
}