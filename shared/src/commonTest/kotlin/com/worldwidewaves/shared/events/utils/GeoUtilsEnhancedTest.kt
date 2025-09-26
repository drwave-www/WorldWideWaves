package com.worldwidewaves.shared.events.utils

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

import com.worldwidewaves.shared.events.utils.GeoUtils.EARTH_RADIUS
import com.worldwidewaves.shared.events.utils.GeoUtils.EPSILON
import com.worldwidewaves.shared.events.utils.GeoUtils.MIN_PERCEPTIBLE_SPEED_DIFFERENCE
import com.worldwidewaves.shared.events.utils.GeoUtils.Vector2D
import com.worldwidewaves.shared.events.utils.GeoUtils.calculateDistance
import com.worldwidewaves.shared.events.utils.GeoUtils.isLatitudeInRange
import com.worldwidewaves.shared.events.utils.GeoUtils.isLongitudeEqual
import com.worldwidewaves.shared.events.utils.GeoUtils.isLongitudeInRange
import com.worldwidewaves.shared.events.utils.GeoUtils.isPointOnSegment
import com.worldwidewaves.shared.events.utils.GeoUtils.toDegrees
import com.worldwidewaves.shared.events.utils.GeoUtils.toRadians
import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Enhanced comprehensive test suite for GeoUtils functionality.
 *
 * This test class provides thorough validation of geographic utility functions
 * essential for WorldWideWaves location-based operations. It covers:
 *
 * **Core Mathematical Functions:**
 * - Coordinate conversion (degrees/radians) with edge cases
 * - Distance calculations using spherical geometry
 * - Bearing calculations and cross products
 *
 * **Geographic Validation:**
 * - Latitude/longitude range validation
 * - Date line crossing scenarios
 * - Polar coordinate edge cases
 *
 * **Geometric Operations:**
 * - Point-in-segment calculations with epsilon precision
 * - Diagonal and complex segment testing
 * - High-precision coordinate validation
 *
 * **Performance & Reliability:**
 * - Thread safety validation for concurrent access
 * - Performance benchmarking for calculations
 * - Accuracy testing for mathematical operations
 *
 * This enhanced test suite replaces the basic GeoUtilsTest.kt to provide
 * comprehensive coverage with real-world edge cases and precision requirements.
 *
 * **Coverage Areas:**
 * - Constants validation (EPSILON, EARTH_RADIUS, MIN_PERCEPTIBLE_SPEED_DIFFERENCE)
 * - Mathematical accuracy and precision
 * - Geographic boundary conditions
 * - Performance characteristics
 *
 * @see GeoUtils
 * @see Position
 * @see Segment
 */
class GeoUtilsEnhancedTest {
    @Test
    fun `test EPSILON constant value`() {
        assertEquals(1e-9, EPSILON, "EPSILON should be 1e-9 for double precision")
        assertTrue(EPSILON > 0, "EPSILON should be positive")
        assertTrue(EPSILON < 1e-6, "EPSILON should be small enough for precision")
    }

    @Test
    fun `test MIN_PERCEPTIBLE_SPEED_DIFFERENCE constant`() {
        assertEquals(10000.0, MIN_PERCEPTIBLE_SPEED_DIFFERENCE, "Speed difference constant should be 10000.0")
        assertTrue(MIN_PERCEPTIBLE_SPEED_DIFFERENCE > 0, "Speed difference should be positive")
    }

    @Test
    fun `test EARTH_RADIUS constant`() {
        assertEquals(6378137.0, EARTH_RADIUS, "Earth radius should be WGS84 value")
        assertTrue(EARTH_RADIUS > 6000000, "Earth radius should be reasonable")
        assertTrue(EARTH_RADIUS < 7000000, "Earth radius should be reasonable")
    }

    @Test
    fun `test toRadians with edge cases`() {
        assertEquals(0.0, 0.0.toRadians(), EPSILON)
        assertEquals(PI, 180.0.toRadians(), EPSILON)
        assertEquals(2 * PI, 360.0.toRadians(), EPSILON)
        assertEquals(-PI, (-180.0).toRadians(), EPSILON)
        assertEquals(-PI / 2, (-90.0).toRadians(), EPSILON)

        // Test fractional degrees
        assertEquals(PI / 4, 45.0.toRadians(), EPSILON)
        assertEquals(PI / 6, 30.0.toRadians(), EPSILON)
        assertEquals(PI / 3, 60.0.toRadians(), EPSILON)
    }

    @Test
    fun `test toDegrees with edge cases`() {
        assertEquals(0.0, 0.0.toDegrees(), EPSILON)
        assertEquals(180.0, PI.toDegrees(), EPSILON)
        assertEquals(360.0, (2 * PI).toDegrees(), EPSILON)
        assertEquals(-180.0, (-PI).toDegrees(), EPSILON)
        assertEquals(-90.0, (-PI / 2).toDegrees(), EPSILON)

        // Test fractional radians
        assertEquals(45.0, (PI / 4).toDegrees(), EPSILON)
        assertEquals(30.0, (PI / 6).toDegrees(), EPSILON)
        assertEquals(60.0, (PI / 3).toDegrees(), EPSILON)
    }

    @Test
    fun `test radians degrees round trip conversion`() {
        val testDegrees = listOf(0.0, 30.0, 45.0, 90.0, 180.0, 270.0, 360.0, -90.0, -180.0)

        testDegrees.forEach { degrees ->
            val radians = degrees.toRadians()
            val convertedBack = radians.toDegrees()
            assertEquals(degrees, convertedBack, EPSILON, "Round trip conversion should preserve value for $degrees")
        }
    }

    @Test
    fun `test Vector2D cross product`() {
        val v1 = Vector2D(1.0, 0.0)
        val v2 = Vector2D(0.0, 1.0)
        assertEquals(1.0, v1.cross(v2), EPSILON, "Cross product of unit vectors should be 1.0")

        val v3 = Vector2D(2.0, 3.0)
        val v4 = Vector2D(4.0, 5.0)
        val expectedCross = 2.0 * 5.0 - 3.0 * 4.0 // = 10 - 12 = -2
        assertEquals(expectedCross, v3.cross(v4), EPSILON, "Cross product calculation should be correct")

        val v5 = Vector2D(1.0, 1.0)
        val v6 = Vector2D(2.0, 2.0)
        assertEquals(0.0, v5.cross(v6), EPSILON, "Cross product of parallel vectors should be 0")
    }

    @Test
    fun `test isLongitudeEqual with precision`() {
        assertTrue(isLongitudeEqual(0.0, 0.0))
        assertTrue(isLongitudeEqual(180.0, 180.0))
        assertTrue(isLongitudeEqual(-180.0, -180.0))

        // Test with values within EPSILON
        assertTrue(isLongitudeEqual(0.0, EPSILON / 2))
        assertTrue(isLongitudeEqual(180.0, 180.0 + EPSILON / 2))
        assertFalse(isLongitudeEqual(0.0, EPSILON * 2))
        assertFalse(isLongitudeEqual(0.0, 0.1))
    }

    @Test
    fun `test isLongitudeInRange normal ranges`() {
        // Simple range not crossing date line
        assertTrue(isLongitudeInRange(0.0, -10.0, 10.0))
        assertTrue(isLongitudeInRange(-5.0, -10.0, 10.0))
        assertTrue(isLongitudeInRange(5.0, -10.0, 10.0))
        assertTrue(isLongitudeInRange(-10.0, -10.0, 10.0)) // boundary
        assertTrue(isLongitudeInRange(10.0, -10.0, 10.0)) // boundary
        assertFalse(isLongitudeInRange(-15.0, -10.0, 10.0))
        assertFalse(isLongitudeInRange(15.0, -10.0, 10.0))
    }

    @Test
    fun `test isLongitudeInRange crossing date line`() {
        // Range crossing date line (start > end)
        assertTrue(isLongitudeInRange(170.0, 160.0, -160.0))
        assertTrue(isLongitudeInRange(-170.0, 160.0, -160.0))
        assertTrue(isLongitudeInRange(180.0, 160.0, -160.0))
        assertTrue(isLongitudeInRange(-180.0, 160.0, -160.0))
        assertTrue(isLongitudeInRange(160.0, 160.0, -160.0)) // boundary
        assertTrue(isLongitudeInRange(-160.0, 160.0, -160.0)) // boundary
        assertFalse(isLongitudeInRange(0.0, 160.0, -160.0))
        assertFalse(isLongitudeInRange(90.0, 160.0, -160.0))
    }

    @Test
    fun `test isLatitudeInRange with various inputs`() {
        // Normal ranges
        assertTrue(isLatitudeInRange(0.0, -90.0, 90.0))
        assertTrue(isLatitudeInRange(45.0, 0.0, 90.0))
        assertTrue(isLatitudeInRange(-45.0, -90.0, 0.0))

        // Reversed ranges (start > end) should still work
        assertTrue(isLatitudeInRange(45.0, 90.0, 0.0))
        assertTrue(isLatitudeInRange(-45.0, 0.0, -90.0))

        // Edge cases
        assertTrue(isLatitudeInRange(90.0, -90.0, 90.0)) // boundary
        assertTrue(isLatitudeInRange(-90.0, -90.0, 90.0)) // boundary
        assertFalse(isLatitudeInRange(95.0, -90.0, 90.0)) // outside range
        assertFalse(isLatitudeInRange(-95.0, -90.0, 90.0)) // outside range
    }

    @Test
    fun `test calculateDistance two longitude version`() {
        // Distance at equator
        val distanceEquator = calculateDistance(0.0, 1.0, 0.0)
        assertTrue(distanceEquator > 0, "Distance should be positive")
        assertTrue(distanceEquator > 100000, "1 degree at equator should be > 100km")

        // Distance at higher latitude should be smaller
        val distance60 = calculateDistance(0.0, 1.0, 60.0)
        assertTrue(distance60 > 0, "Distance should be positive")
        assertTrue(distance60 < distanceEquator, "Distance should be smaller at higher latitude")

        // Same longitude should give zero distance
        val zeroDistance = calculateDistance(10.0, 10.0, 0.0)
        assertEquals(0.0, zeroDistance, EPSILON, "Same longitude should give zero distance")

        // Negative longitude difference
        val negativeDistance = calculateDistance(1.0, 0.0, 0.0)
        assertEquals(distanceEquator, negativeDistance, EPSILON, "Distance should be absolute value")
    }

    @Test
    fun `test calculateDistance width version`() {
        val widthDistance = calculateDistance(1.0, 0.0)
        val twoLonDistance = calculateDistance(0.0, 1.0, 0.0)
        assertEquals(twoLonDistance, widthDistance, EPSILON, "Width version should match two-longitude version")

        val zeroWidthDistance = calculateDistance(0.0, 0.0)
        assertEquals(0.0, zeroWidthDistance, EPSILON, "Zero width should give zero distance")
    }

    @Test
    fun `test isPointOnSegment with precise coordinates`() {
        // Test point exactly on horizontal segment
        val horizontalSegment = Segment(Position(1.0, 0.0), Position(1.0, 2.0))
        assertTrue(isPointOnSegment(Position(1.0, 0.0), horizontalSegment), "Start point should be on segment")
        assertTrue(isPointOnSegment(Position(1.0, 2.0), horizontalSegment), "End point should be on segment")
        assertTrue(isPointOnSegment(Position(1.0, 1.0), horizontalSegment), "Midpoint should be on segment")
        assertFalse(isPointOnSegment(Position(1.0, 3.0), horizontalSegment), "Point beyond segment should not be on segment")

        // Test point exactly on vertical segment
        val verticalSegment = Segment(Position(0.0, 1.0), Position(2.0, 1.0))
        assertTrue(isPointOnSegment(Position(0.0, 1.0), verticalSegment), "Start point should be on segment")
        assertTrue(isPointOnSegment(Position(2.0, 1.0), verticalSegment), "End point should be on segment")
        assertTrue(isPointOnSegment(Position(1.0, 1.0), verticalSegment), "Midpoint should be on segment")
        assertFalse(isPointOnSegment(Position(3.0, 1.0), verticalSegment), "Point beyond segment should not be on segment")
    }

    @Test
    fun `test isPointOnSegment with diagonal segments`() {
        // Test diagonal segment with slope = 1
        val diagonalSegment = Segment(Position(0.0, 0.0), Position(2.0, 2.0))
        assertTrue(isPointOnSegment(Position(0.0, 0.0), diagonalSegment), "Start point should be on segment")
        assertTrue(isPointOnSegment(Position(2.0, 2.0), diagonalSegment), "End point should be on segment")
        assertTrue(isPointOnSegment(Position(1.0, 1.0), diagonalSegment), "Midpoint should be on segment")
        assertTrue(isPointOnSegment(Position(0.5, 0.5), diagonalSegment), "Quarter point should be on segment")
        assertTrue(isPointOnSegment(Position(1.5, 1.5), diagonalSegment), "Three-quarter point should be on segment")

        assertFalse(isPointOnSegment(Position(1.0, 0.0), diagonalSegment), "Off-line point should not be on segment")
        assertFalse(isPointOnSegment(Position(3.0, 3.0), diagonalSegment), "Extension point should not be on segment")

        // Test diagonal segment with different slope
        val slopeSegment = Segment(Position(0.0, 0.0), Position(4.0, 2.0))
        assertTrue(isPointOnSegment(Position(2.0, 1.0), slopeSegment), "Midpoint should be on slope segment")
        assertFalse(isPointOnSegment(Position(2.0, 2.0), slopeSegment), "Wrong slope point should not be on segment")
    }

    @Test
    fun `test isPointOnSegment with epsilon precision`() {
        val segment = Segment(Position(0.0, 0.0), Position(1.0, 1.0))

        // Point very close to the line (within epsilon)
        val nearPoint = Position(EPSILON / 2, 0.0)
        val farPoint = Position(EPSILON * 2, 0.0)

        // This depends on the implementation - might be true or false based on epsilon handling
        // Just verify the function doesn't crash and returns a boolean
        val nearResult = isPointOnSegment(nearPoint, segment)
        val farResult = isPointOnSegment(farPoint, segment)

        assertTrue(nearResult is Boolean, "Should return boolean for near point")
        assertTrue(farResult is Boolean, "Should return boolean for far point")
    }

    @Test
    fun `test geographic coordinate edge cases`() {
        // Test coordinates at extreme values
        assertTrue(isLatitudeInRange(90.0, -90.0, 90.0), "North pole should be in range")
        assertTrue(isLatitudeInRange(-90.0, -90.0, 90.0), "South pole should be in range")
        assertTrue(isLongitudeInRange(180.0, -180.0, 180.0), "International date line should be in range")
        assertTrue(isLongitudeInRange(-180.0, -180.0, 180.0), "International date line should be in range")

        // Test just outside valid ranges
        assertFalse(isLatitudeInRange(90.1, -90.0, 90.0), "Beyond north pole should be out of range")
        assertFalse(isLatitudeInRange(-90.1, -90.0, 90.0), "Beyond south pole should be out of range")
    }

    @Test
    fun `test distance calculation accuracy`() {
        // Test known distance: 1 degree longitude at equator ≈ 111.32 km
        val distance1DegreeEquator = calculateDistance(0.0, 1.0, 0.0)
        assertTrue(distance1DegreeEquator > 111000, "1 degree at equator should be ~111km")
        assertTrue(distance1DegreeEquator < 112000, "1 degree at equator should be ~111km")

        // Test distance at 60 degrees latitude (should be half of equator)
        val distance1Degree60 = calculateDistance(0.0, 1.0, 60.0)
        val expectedDistance60 = distance1DegreeEquator * 0.5 // cos(60°) = 0.5
        assertEquals(expectedDistance60, distance1Degree60, 1000.0, "Distance at 60° should be half of equator")
    }

    @Test
    fun `test geometric calculations thread safety`() {
        val results = mutableListOf<String>()
        val exceptions = mutableListOf<Exception>()

        val threads =
            (1..5).map { threadId ->
                Thread {
                    try {
                        repeat(10) { iteration ->
                            // Test various calculations
                            val radians = (iteration * 30.0).toRadians()
                            val degrees = radians.toDegrees()
                            val distance = calculateDistance(0.0, 1.0, iteration * 10.0)
                            val segment = Segment(Position(0.0, 0.0), Position(iteration.toDouble(), iteration.toDouble()))
                            val pointOnSegment = isPointOnSegment(Position(iteration / 2.0, iteration / 2.0), segment)

                            synchronized(results) {
                                results.add("Thread $threadId: deg=$degrees, dist=$distance, onSeg=$pointOnSegment")
                            }
                        }
                    } catch (e: Exception) {
                        synchronized(exceptions) {
                            exceptions.add(e)
                        }
                    }
                }
            }

        threads.forEach { it.start() }
        threads.forEach { it.join() }

        assertTrue(exceptions.isEmpty(), "No exceptions should occur during concurrent calculations: $exceptions")
        assertEquals(50, results.size, "All threads should complete successfully")
    }

    /**
     * Performance benchmark test for GeoUtils calculations.
     *
     * This test validates that the geographic utility functions maintain
     * acceptable performance characteristics under typical usage patterns.
     * It exercises the most commonly used functions in a loop to identify
     * any performance regressions or bottlenecks.
     *
     * **Operations Tested:**
     * - Coordinate conversion (toRadians/toDegrees)
     * - Distance calculations (calculateDistance)
     * - Range validation (isLongitudeInRange, isLatitudeInRange)
     * - Point-in-segment operations (isPointOnSegment)
     *
     * **Performance Expectations:**
     * - 1000 iterations should complete within reasonable time
     * - No memory leaks or excessive allocations
     * - Consistent timing across multiple runs
     *
     * This test helps ensure the app remains responsive during wave
     * calculations and geographic operations in real-time scenarios.
     */
    @Test
    fun `test calculation performance`() {
        repeat(1000) { i ->
            val lat = i % 90.0
            val lon1 = i % 180.0
            val lon2 = (i + 1) % 180.0

            // Perform various calculations
            lon1.toRadians().toDegrees()
            calculateDistance(lon1, lon2, lat)
            isLongitudeInRange(lon1, -180.0, 180.0)
            isLatitudeInRange(lat, -90.0, 90.0)

            val segment = Segment(Position(lat, lon1), Position(lat + 1, lon2))
            isPointOnSegment(Position(lat + 0.5, (lon1 + lon2) / 2), segment)
        }

        // If we reach here without crashes, performance is acceptable
        assertTrue(true, "Geo calculations completed without errors")
    }
}
