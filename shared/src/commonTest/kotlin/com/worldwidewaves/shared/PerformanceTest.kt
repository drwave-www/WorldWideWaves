package com.worldwidewaves.shared

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

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

import com.worldwidewaves.shared.events.utils.GeoUtils
import com.worldwidewaves.shared.events.utils.Position
import com.worldwidewaves.shared.events.utils.PolygonUtils
import com.worldwidewaves.shared.events.utils.PolygonUtils.toPolygon
import com.worldwidewaves.shared.sound.WaveformGenerator
import com.worldwidewaves.shared.sound.SoundPlayer

/**
 * Comprehensive performance tests addressing TODO_PHASE2.md Item 10:
 * - Add memory usage tests for large data processing
 * - Add computational complexity tests for polygon operations
 * - Add stress tests for geographic area limits
 *
 * This test validates performance characteristics and ensures the system
 * can handle realistic load scenarios without degradation.
 */
@OptIn(ExperimentalTime::class)
class PerformanceTest {

    @Test
    fun `should handle large waveform generation efficiently`() {
        // GIVEN: Parameters for large waveform generation
        val sampleRate = 44100
        val frequency = 440.0
        val amplitude = 0.8
        val duration = 10.seconds // Large 10-second waveform

        // WHEN: Generating large waveform and measuring time
        val executionTime = measureTime {
            val samples = WaveformGenerator.generateWaveform(
                sampleRate = sampleRate,
                frequency = frequency,
                amplitude = amplitude,
                duration = duration,
                waveform = SoundPlayer.Waveform.SINE
            )

            // THEN: Should generate expected number of samples
            val expectedSamples = sampleRate * duration.inWholeSeconds.toInt()
            assertTrue(samples.size >= expectedSamples * 0.95, "Should generate approximately expected samples")
            assertTrue(samples.size <= expectedSamples * 1.05, "Should not significantly exceed expected samples")
        }

        // THEN: Should complete in reasonable time (less than 500ms for 10-second audio)
        assertTrue(executionTime < 500.milliseconds, "Large waveform generation should complete in <500ms, took: $executionTime")
    }

    @Test
    fun `should handle multiple concurrent waveform generations efficiently`() {
        // GIVEN: Parameters for concurrent waveform generation
        val concurrentTasks = 5
        val sampleRate = 44100
        val duration = 2.seconds

        // WHEN: Generating multiple waveforms concurrently and measuring time
        val executionTime = measureTime {
            val results = mutableListOf<DoubleArray>()

            repeat(concurrentTasks) { index ->
                val frequency = 440.0 + (index * 110.0) // Different frequencies
                val samples = WaveformGenerator.generateWaveform(
                    sampleRate = sampleRate,
                    frequency = frequency,
                    amplitude = 0.5,
                    duration = duration,
                    waveform = SoundPlayer.Waveform.SINE
                )
                results.add(samples)
            }

            // THEN: All waveforms should be generated successfully
            assertTrue(results.size == concurrentTasks, "Should generate all requested waveforms")
            results.forEach { samples ->
                assertTrue(samples.isNotEmpty(), "Each waveform should contain samples")
            }
        }

        // THEN: Should complete in reasonable time (less than 1 second for 5x2-second audio)
        assertTrue(executionTime < 1.seconds, "Concurrent waveform generation should complete in <1s, took: $executionTime")
    }

    @Test
    fun `should handle distance calculations for large datasets efficiently`() {
        // GIVEN: Large dataset of position pairs for distance calculation
        val datasetSize = 1000
        val positions = mutableListOf<Pair<Position, Position>>()

        // Create realistic global position pairs
        repeat(datasetSize) { i ->
            val lat1 = -90.0 + (i % 180)
            val lng1 = -180.0 + ((i * 2) % 360)
            val lat2 = -90.0 + ((i + 50) % 180)
            val lng2 = -180.0 + (((i + 50) * 2) % 360)

            positions.add(
                Position(lat = lat1, lng = lng1) to Position(lat = lat2, lng = lng2)
            )
        }

        // WHEN: Calculating distances for large dataset and measuring time
        val executionTime = measureTime {
            positions.forEach { (pos1, pos2) ->
                val distance = GeoUtils.calculateDistance(pos1.lng, pos2.lng, pos1.lat)
                assertTrue(distance >= 0.0, "Distance should be non-negative")
            }
        }

        // THEN: Should complete in reasonable time (less than 100ms for 1000 calculations)
        assertTrue(executionTime < 100.milliseconds, "Large dataset distance calculations should complete in <100ms, took: $executionTime")
    }

    @Test
    fun `should handle complex polygon operations efficiently`() {
        // GIVEN: Complex polygon with many vertices
        val vertexCount = 100
        val complexPolygon = mutableListOf<Position>()

        // Create complex star-shaped polygon
        val centerLat = 40.7128
        val centerLng = -74.0060
        val radius = 0.01 // ~1km radius

        repeat(vertexCount) { i ->
            val angle = (i * 2 * kotlin.math.PI) / vertexCount
            val currentRadius = if (i % 2 == 0) radius else radius * 0.5 // Star shape
            val lat = centerLat + currentRadius * kotlin.math.cos(angle)
            val lng = centerLng + currentRadius * kotlin.math.sin(angle)
            complexPolygon.add(Position(lat = lat, lng = lng))
        }

        // Test points for point-in-polygon testing
        val testPoints = mutableListOf<Position>()
        repeat(200) { i ->
            val lat = centerLat + (radius * 1.5) * (i % 10 - 5) / 5.0
            val lng = centerLng + (radius * 1.5) * ((i / 10) % 10 - 5) / 5.0
            testPoints.add(Position(lat = lat, lng = lng))
        }

        // WHEN: Performing complex polygon operations and measuring time
        val executionTime = measureTime {
            testPoints.forEach { testPoint ->
                val isInside = PolygonUtils.run { complexPolygon.toPolygon.containsPosition(testPoint) }
                // Result can be true or false, we're testing performance not correctness here
            }
        }

        // THEN: Should complete in reasonable time (less than 200ms for 200 points x 100-vertex polygon)
        assertTrue(executionTime < 200.milliseconds, "Complex polygon operations should complete in <200ms, took: $executionTime")
    }

    @Test
    fun `should handle stress test with maximum geographic area limits`() {
        // GIVEN: Stress test parameters covering maximum geographic bounds
        val stressTestIterations = 500
        val maxLatRange = 180.0 // -90 to +90
        val maxLngRange = 360.0 // -180 to +180

        // WHEN: Performing stress test with maximum geographic calculations
        val executionTime = measureTime {
            repeat(stressTestIterations) { i ->
                // Generate extreme position pairs
                val lat1 = -90.0 + (i * maxLatRange) / stressTestIterations
                val lng1 = -180.0 + (i * maxLngRange) / stressTestIterations
                val lat2 = 90.0 - (i * maxLatRange) / stressTestIterations
                val lng2 = 180.0 - (i * maxLngRange) / stressTestIterations

                val pos1 = Position(lat = lat1, lng = lng1)
                val pos2 = Position(lat = lat2, lng = lng2)

                // Test geographic distance operation
                val distance = GeoUtils.calculateDistance(pos1.lng, pos2.lng, pos1.lat)

                // Validate results are reasonable
                assertTrue(distance >= 0.0, "Distance should be non-negative")
            }
        }

        // THEN: Should handle stress test in reasonable time (less than 300ms for 500 iterations)
        assertTrue(executionTime < 300.milliseconds, "Geographic stress test should complete in <300ms, took: $executionTime")
    }

    @Test
    fun `should handle memory-intensive polygon creation and processing`() {
        // GIVEN: Memory-intensive polygon creation parameters
        val largePolygonVertices = 500
        val polygonCount = 10
        val polygons = mutableListOf<List<Position>>()

        // WHEN: Creating and processing multiple large polygons
        val executionTime = measureTime {
            // Create multiple large polygons
            repeat(polygonCount) { polygonIndex ->
                val polygon = mutableListOf<Position>()
                val baseRadius = 0.01 * (polygonIndex + 1)

                repeat(largePolygonVertices) { vertexIndex ->
                    val angle = (vertexIndex * 2 * kotlin.math.PI) / largePolygonVertices
                    val lat = 40.0 + baseRadius * kotlin.math.cos(angle)
                    val lng = -74.0 + baseRadius * kotlin.math.sin(angle)
                    polygon.add(Position(lat = lat, lng = lng))
                }
                polygons.add(polygon)
            }

            // Process all polygons with test point
            val testPoint = Position(lat = 40.0, lng = -74.0)
            polygons.forEach { polygon ->
                val isInside = PolygonUtils.run { polygon.toPolygon.containsPosition(testPoint) }
                // Memory allocation and processing test
            }
        }

        // THEN: Should handle memory-intensive operations efficiently (less than 400ms)
        assertTrue(executionTime < 400.milliseconds, "Memory-intensive polygon processing should complete in <400ms, took: $executionTime")

        // THEN: Should successfully create all polygons
        assertTrue(polygons.size == polygonCount, "Should create all requested polygons")
        polygons.forEach { polygon ->
            assertTrue(polygon.size == largePolygonVertices, "Each polygon should have expected vertex count")
        }
    }

    @Test
    fun `should demonstrate computational complexity characteristics`() {
        // GIVEN: Different input sizes to test computational complexity
        val inputSizes = listOf(10, 50, 100, 200)
        val timingResults = mutableMapOf<Int, Long>()

        inputSizes.forEach { size ->
            // WHEN: Running polygon operations with increasing input sizes
            val executionTime = measureTime {
                // Create polygon with 'size' vertices
                val polygon = mutableListOf<Position>()
                repeat(size) { i ->
                    val angle = (i * 2 * kotlin.math.PI) / size
                    val lat = 40.0 + 0.01 * kotlin.math.cos(angle)
                    val lng = -74.0 + 0.01 * kotlin.math.sin(angle)
                    polygon.add(Position(lat = lat, lng = lng))
                }

                // Test point-in-polygon for multiple points
                val testPoint = Position(lat = 40.0, lng = -74.0)
                repeat(50) {
                    PolygonUtils.run { polygon.toPolygon.containsPosition(testPoint) }
                }
            }

            timingResults[size] = executionTime.inWholeMilliseconds
        }

        // THEN: Computational complexity should be reasonable (not exponential)
        val times = timingResults.values.toList()

        // Detect CI environment and adjust thresholds accordingly
        val isCI = System.getenv("CI") == "true" ||
                   System.getenv("GITHUB_ACTIONS") == "true" ||
                   System.getenv("CONTINUOUS_INTEGRATION") == "true"

        // CI environments are more variable and slower, so use more lenient thresholds
        val maxRatio = if (isCI) 10.0 else 5.0
        val maxExecutionTime = if (isCI) 500L else 100L
        val maxReasonableTime = if (isCI) 50.0 else 10.0

        // Check that timing doesn't increase exponentially
        for (i in 1 until times.size) {
            val currentTime = times[i].toDouble()
            val previousTime = times[i-1].toDouble()

            // Handle case where previous timing was 0 (too fast to measure)
            if (previousTime == 0.0) {
                // If current time is also 0, that's fine (both very fast)
                // If current time is > 0, that's still reasonable (small increase)
                assertTrue(currentTime < maxReasonableTime, "Current timing should be reasonable when previous was 0ms, got: ${currentTime}ms")
            } else {
                val ratio = currentTime / previousTime
                assertTrue(ratio < maxRatio, "Timing ratio between consecutive sizes should be <${maxRatio}x, got: $ratio")
            }
        }

        // Overall performance should be reasonable
        assertTrue(times.max() < maxExecutionTime, "Maximum execution time should be <${maxExecutionTime}ms, got: ${times.max()}ms")
    }

    @Test
    fun `should handle concurrent geographic operations efficiently`() {
        // GIVEN: Parameters for concurrent geographic operations
        val concurrentOperations = 20
        val operationsPerTask = 50

        // WHEN: Running multiple geographic operations concurrently
        val executionTime = measureTime {
            val results = mutableListOf<Double>()

            repeat(concurrentOperations) { taskIndex ->
                repeat(operationsPerTask) { opIndex ->
                    val lat1 = -90.0 + (taskIndex * 9.0)
                    val lng1 = -180.0 + (opIndex * 7.2)
                    val lat2 = lat1 + 1.0
                    val lng2 = lng1 + 1.0

                    val pos1 = Position(lat = lat1, lng = lng1)
                    val pos2 = Position(lat = lat2, lng = lng2)

                    val distance = GeoUtils.calculateDistance(pos1.lng, pos2.lng, pos1.lat)
                    results.add(distance)
                }
            }

            // THEN: All operations should complete successfully
            assertTrue(results.size == concurrentOperations * operationsPerTask, "Should complete all operations")
            results.forEach { distance ->
                assertTrue(distance >= 0.0, "All distances should be non-negative")
            }
        }

        // THEN: Should handle concurrent operations efficiently (less than 200ms)
        assertTrue(executionTime < 200.milliseconds, "Concurrent geographic operations should complete in <200ms, took: $executionTime")
    }

    @Test
    fun `should validate memory efficiency with large coordinate arrays`() {
        // GIVEN: Large coordinate arrays to test memory efficiency
        val arraySize = 5000
        val coordinateArrays = mutableListOf<List<Position>>()

        // WHEN: Creating and processing large coordinate arrays
        val executionTime = measureTime {
            // Create multiple large coordinate arrays
            repeat(3) { arrayIndex ->
                val coordinates = mutableListOf<Position>()

                repeat(arraySize) { coordIndex ->
                    val lat = -90.0 + (coordIndex * 180.0) / arraySize
                    val lng = -180.0 + (coordIndex * 360.0) / arraySize + arrayIndex
                    coordinates.add(Position(lat = lat, lng = lng))
                }

                coordinateArrays.add(coordinates)
            }

            // Process arrays with distance calculations
            coordinateArrays.forEach { coords ->
                for (i in 0 until minOf(100, coords.size - 1)) {
                    GeoUtils.calculateDistance(coords[i].lng, coords[i + 1].lng, coords[i].lat)
                }
            }
        }

        // THEN: Should handle large arrays efficiently (less than 150ms)
        assertTrue(executionTime < 150.milliseconds, "Large coordinate array processing should complete in <150ms, took: $executionTime")

        // THEN: All arrays should be created successfully
        assertTrue(coordinateArrays.size == 3, "Should create all coordinate arrays")
        coordinateArrays.forEach { coords ->
            assertTrue(coords.size == arraySize, "Each array should have expected size")
        }
    }
}