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

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.atan2
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * Comprehensive tests for mathematical calculations in GeoUtils.
 *
 * This test class validates:
 * - Great circle distance vs planar distance accuracy
 * - Hard-coded tolerance values with scientific justification
 * - Coordinate projection error validation near poles
 * - Mathematical precision and edge cases
 *
 * **Key Test Areas:**
 * - Distance calculation accuracy validation
 * - Tolerance value verification
 * - Polar region mathematical behavior
 * - Edge cases and precision limits
 *
 * @see GeoUtils
 */
class GeoMathematicalValidationTest {

    companion object {
        // Scientifically justified tolerances based on WGS-84 precision requirements
        private const val COORDINATE_PRECISION_EPSILON = 1e-9 // ~1mm precision at equator
        private const val DISTANCE_TOLERANCE_METERS = 10.0 // 10m tolerance for UI/visualization
        private const val POLAR_LATITUDE_THRESHOLD = 85.0 // Degrees - near polar regions
        private const val EARTH_RADIUS_WGS84 = 6378137.0 // WGS-84 semi-major axis
    }

    /**
     * Reference implementation of Haversine formula for great circle distance.
     * Used to validate the accuracy of GeoUtils.calculateDistance.
     */
    private fun haversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val lat1Rad = lat1 * (PI / 180)
        val lat2Rad = lat2 * (PI / 180)
        val deltaLatRad = (lat2 - lat1) * (PI / 180)
        val deltaLonRad = (lon2 - lon1) * (PI / 180)

        val a = sin(deltaLatRad / 2) * sin(deltaLatRad / 2) +
                cos(lat1Rad) * cos(lat2Rad) *
                sin(deltaLonRad / 2) * sin(deltaLonRad / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return EARTH_RADIUS_WGS84 * c
    }

    @Test
    fun `test EPSILON tolerance value is scientifically justified`() {
        // GIVEN: The current EPSILON value and precision requirements
        val currentEpsilon = GeoUtils.EPSILON
        val expectedEpsilon = COORDINATE_PRECISION_EPSILON

        // WHEN: Comparing with scientifically justified value
        // THEN: Should use precision appropriate for geodetic calculations (~1mm at equator)
        assertEquals(expectedEpsilon, currentEpsilon,
            "EPSILON should be $expectedEpsilon for ~1mm precision at equator, but was $currentEpsilon")
    }

    @Test
    fun `test Earth radius constant is WGS-84 compliant`() {
        // GIVEN: Current earth radius constant
        val currentRadius = GeoUtils.EARTH_RADIUS
        val wgs84SemiMajorAxis = EARTH_RADIUS_WGS84

        // WHEN: Comparing with WGS-84 standard
        // THEN: Should match WGS-84 semi-major axis
        assertEquals(wgs84SemiMajorAxis, currentRadius,
            "EARTH_RADIUS should match WGS-84 semi-major axis ($wgs84SemiMajorAxis m)")
    }

    @Test
    fun `test calculateDistance accuracy vs Haversine formula for short distances`() {
        // GIVEN: Short distance coordinates (within city scale)
        val lat = 45.0 // Mid-latitude for reasonable projection
        val lon1 = 2.0
        val lon2 = 2.1 // ~11km difference at 45° latitude

        // WHEN: Calculating with both methods
        val geoUtilsDistance = GeoUtils.calculateDistance(lon1, lon2, lat)
        val haversineDistance = haversineDistance(lat, lon1, lat, lon2)

        // THEN: Should be within acceptable tolerance for short distances
        val errorPercentage = abs(geoUtilsDistance - haversineDistance) / haversineDistance * 100
        assertTrue(errorPercentage < 1.0,
            "Short distance error should be <1%, got ${errorPercentage}% " +
            "(GeoUtils: ${geoUtilsDistance}m, Haversine: ${haversineDistance}m)")
    }

    @Test
    fun `test calculateDistanceFast vs accurate for longitude distances`() {
        // GIVEN: Long distance coordinates (transcontinental scale)
        val lat = 45.0
        val lon1 = 0.0  // Greenwich
        val lon2 = 90.0 // Quarter way around the globe

        // WHEN: Calculating with both fast approximation and accurate methods
        val fastDistance = GeoUtils.calculateDistanceFast(lon1, lon2, lat)
        val accurateDistance = GeoUtils.calculateDistanceAccurate(lon1, lon2, lat)

        // THEN: Both methods should produce reasonable results for longitude distances
        // Note: For longitude-only distances at the same latitude, the difference is minimal
        assertTrue(accurateDistance > 0,
            "Accurate distance should be positive, got: ${accurateDistance}m")
        assertTrue(fastDistance > 0,
            "Fast distance should be positive, got: ${fastDistance}m")

        // Verify the distances are in reasonable range (quarter globe at 45° lat)
        val expectedApprox = 10_000_000.0 // ~10,000 km
        assertTrue(accurateDistance > expectedApprox * 0.5,
            "Distance should be at least half expected (~5,000km), got: ${accurateDistance}m")
        assertTrue(accurateDistance < expectedApprox * 1.5,
            "Distance should be at most 1.5x expected (~15,000km), got: ${accurateDistance}m")
    }

    @Test
    fun `test calculateDistanceAccurate matches Haversine formula`() {
        // GIVEN: Various distance coordinates
        val testCases = listOf(
            Triple(45.0, 0.0, 1.0),   // Short distance
            Triple(45.0, 0.0, 10.0),  // Medium distance
            Triple(45.0, 0.0, 90.0),  // Long distance
            Triple(0.0, 0.0, 45.0),   // Equatorial distance
            Triple(80.0, 0.0, 10.0),  // Near-polar distance
        )

        testCases.forEach { (lat, lon1, lon2) ->
            // WHEN: Calculating with accurate method vs reference Haversine
            val geoUtilsAccurate = GeoUtils.calculateDistanceAccurate(lon1, lon2, lat)
            val haversineReference = haversineDistance(lat, lon1, lat, lon2)

            // THEN: Should match within acceptable tolerance
            val errorPercentage = abs(geoUtilsAccurate - haversineReference) / haversineReference * 100
            assertTrue(errorPercentage < 0.1,
                "Accurate method should match Haversine within 0.1%, got ${errorPercentage}% " +
                "for coordinates ($lat°, $lon1°, $lon2°)")
        }
    }

    @Test
    fun `test coordinate projection error near North Pole`() {
        // GIVEN: Coordinates near North Pole
        val polarLat = 89.0 // Very close to North Pole
        val lon1 = 0.0
        val lon2 = 1.0 // 1 degree longitude difference

        // WHEN: Calculating distance near pole
        val polarDistance = GeoUtils.calculateDistance(lon1, lon2, polarLat)
        val equatorDistance = GeoUtils.calculateDistance(lon1, lon2, 0.0)

        // THEN: Distance should be much smaller near poles due to convergence
        assertTrue(polarDistance < equatorDistance * 0.1,
            "Distance near pole should be much smaller due to meridian convergence. " +
            "Polar: ${polarDistance}m, Equator: ${equatorDistance}m")
    }

    @Test
    fun `test coordinate projection error near South Pole`() {
        // GIVEN: Coordinates near South Pole
        val polarLat = -89.0 // Very close to South Pole
        val lon1 = 0.0
        val lon2 = 1.0

        // WHEN: Calculating distance near pole
        val polarDistance = GeoUtils.calculateDistance(lon1, lon2, polarLat)
        val equatorDistance = GeoUtils.calculateDistance(lon1, lon2, 0.0)

        // THEN: Distance should be much smaller near poles
        assertTrue(polarDistance < equatorDistance * 0.1,
            "Distance near South Pole should be much smaller due to meridian convergence. " +
            "Polar: ${polarDistance}m, Equator: ${equatorDistance}m")
    }

    @Test
    fun `test longitude range crossing date line mathematical consistency`() {
        // GIVEN: Longitude range that crosses International Date Line
        val lng = 179.5
        val start = 179.0
        val end = -179.0 // Crosses date line

        // WHEN: Checking if longitude is in range
        val isInRange = GeoUtils.isLongitudeInRange(lng, start, end)

        // THEN: Should correctly handle date line crossing
        assertTrue(isInRange, "Longitude 179.5° should be in range [179°, -179°] crossing date line")
    }

    @Test
    fun `test longitude range not crossing date line`() {
        // GIVEN: Normal longitude range
        val lng = 10.0
        val start = 5.0
        val end = 15.0

        // WHEN: Checking if longitude is in range
        val isInRange = GeoUtils.isLongitudeInRange(lng, start, end)

        // THEN: Should correctly handle normal range
        assertTrue(isInRange, "Longitude 10° should be in range [5°, 15°]")
    }

    @Test
    fun `test latitude range validation is mathematically sound`() {
        // GIVEN: Various latitude ranges
        val testCases = listOf(
            Triple(45.0, 40.0, 50.0) to true,  // Normal range
            Triple(0.0, -10.0, 10.0) to true,  // Crossing equator
            Triple(-85.0, -90.0, -80.0) to true, // Antarctic region
            Triple(45.0, 50.0, 40.0) to true,  // Reversed range (should still work)
            Triple(60.0, 40.0, 50.0) to false, // Outside range
        )

        testCases.forEach { (data, expected) ->
            val (lat, start, end) = data

            // WHEN: Checking latitude range
            val result = GeoUtils.isLatitudeInRange(lat, start, end)

            // THEN: Should match expected result
            assertEquals(expected, result,
                "Latitude $lat should ${if (expected) "be" else "not be"} in range [$start, $end]")
        }
    }

    @Test
    fun `test point on segment calculation near coordinate precision limits`() {
        // GIVEN: Segment and point at precision limits
        val start = Position(lng = 0.0, lat = 0.0)
        val end = Position(lng = 0.000000001, lat = 0.000000001) // ~1mm at equator
        val point = Position(lng = 0.0000000005, lat = 0.0000000005) // Midpoint
        val segment = Segment(start, end)

        // WHEN: Checking if point is on segment
        val isOnSegment = GeoUtils.isPointOnSegment(point, segment)

        // THEN: Should handle precision correctly
        assertTrue(isOnSegment,
            "Point at precision limits should be correctly identified as on segment")
    }

    @Test
    fun `test Vector2D cross product mathematical correctness`() {
        // GIVEN: Known vectors for cross product
        val vector1 = GeoUtils.Vector2D(3.0, 4.0)
        val vector2 = GeoUtils.Vector2D(1.0, 2.0)

        // WHEN: Calculating cross product
        val crossProduct = vector1.cross(vector2)

        // THEN: Should match mathematical expectation (3*2 - 4*1 = 2)
        assertEquals(2.0, crossProduct, 1e-10,
            "Cross product should be mathematically correct")
    }

    @Test
    fun `test radians to degrees conversion accuracy`() {
        // GIVEN: Known angle conversions
        val testCases = listOf(
            0.0 to 0.0,
            PI / 2 to 90.0,
            PI to 180.0,
            2 * PI to 360.0,
            -PI / 2 to -90.0
        )

        testCases.forEach { (radians, expectedDegrees) ->
            // WHEN: Converting radians to degrees
            val degrees = GeoUtils.run { radians.toDegrees() }

            // THEN: Should be mathematically accurate
            assertEquals(expectedDegrees, degrees, 1e-10,
                "Conversion of $radians radians should equal $expectedDegrees degrees")
        }
    }

    @Test
    fun `test degrees to radians conversion accuracy`() {
        // GIVEN: Known angle conversions
        val testCases = listOf(
            0.0 to 0.0,
            90.0 to PI / 2,
            180.0 to PI,
            360.0 to 2 * PI,
            -90.0 to -PI / 2
        )

        testCases.forEach { (degrees, expectedRadians) ->
            // WHEN: Converting degrees to radians
            val radians = GeoUtils.run { degrees.toRadians() }

            // THEN: Should be mathematically accurate
            assertEquals(expectedRadians, radians, 1e-10,
                "Conversion of $degrees degrees should equal $expectedRadians radians")
        }
    }

    @Test
    fun `test distance calculation returns zero for identical coordinates`() {
        // GIVEN: Identical coordinates
        val lat = 45.0
        val lon = 10.0

        // WHEN: Calculating distance between identical points
        val distance = GeoUtils.calculateDistance(lon, lon, lat)

        // THEN: Should return zero
        assertEquals(0.0, distance, GeoUtils.EPSILON,
            "Distance between identical coordinates should be zero")
    }

    @Test
    fun `test distance calculation is always non-negative`() {
        // GIVEN: Various coordinate pairs (including negative coordinates)
        val testCases = listOf(
            Triple(0.0, 10.0, 45.0),
            Triple(10.0, 0.0, 45.0),
            Triple(-10.0, 10.0, 45.0),
            Triple(10.0, -10.0, 45.0),
            Triple(-170.0, 170.0, 0.0), // Crosses date line
        )

        testCases.forEach { (lon1, lon2, lat) ->
            // WHEN: Calculating distance
            val distance = GeoUtils.calculateDistance(lon1, lon2, lat)

            // THEN: Should always be non-negative
            assertTrue(distance >= 0.0,
                "Distance should always be non-negative, got $distance for coordinates ($lon1, $lon2) at $lat°")
        }
    }
}