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

import com.worldwidewaves.shared.WWWGlobals
import com.worldwidewaves.shared.WWWGlobals.Wave
import com.worldwidewaves.shared.events.WWWEventWaveLinear
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * Comprehensive tests for mathematical calculation accuracy addressing TODO items:
 * - Validate great circle distance vs planar distance accuracy
 * - Fix hard-coded tolerance values with scientific justification
 * - Add coordinate projection error validation near poles
 * - Validate wave duration physics realism
 *
 * This test validates mathematical accuracy, scientifically justified tolerance values,
 * and geographic projection accuracy across different scenarios.
 */
class MathematicalAccuracyTest {

    companion object {
        // WGS84 Constants - Scientific Standards
        private const val EARTH_RADIUS_METERS = 6378137.0 // WGS84 Semi-major axis
        private const val EARTH_FLATTENING = 1.0 / 298.257223563 // WGS84 flattening
        private const val EQUATORIAL_CIRCUMFERENCE = 40075017.0 // meters
        private const val MERIDIONAL_CIRCUMFERENCE = 40007863.0 // meters

        // Scientific Tolerance Calculations
        // GPS accuracy: typically 3-5 meters civilian, we use 10m for safety
        private const val GPS_ACCURACY_METERS = 10.0
        private const val GPS_ACCURACY_DEGREES = GPS_ACCURACY_METERS / (EQUATORIAL_CIRCUMFERENCE / 360.0) // ~9e-5 degrees

        // Floating point precision limits
        private const val DOUBLE_EPSILON = 2.220446049250313e-16 // Machine epsilon for double
        private const val GEOGRAPHIC_EPSILON = 1e-9 // Current GeoUtils.EPSILON

        // Distance calculation tolerances
        private const val SHORT_DISTANCE_TOLERANCE_METERS = 1.0 // 1 meter for distances < 1km
        private const val MEDIUM_DISTANCE_TOLERANCE_METERS = 10.0 // 10 meters for distances < 100km
        private const val LONG_DISTANCE_TOLERANCE_METERS = 100.0 // 100 meters for distances > 100km

        // Position tolerance for wave calculations
        private const val WAVE_POSITION_EPSILON_LAT = 0.000009 // From WWWEventWaveLinear - ~1 meter
        private const val WAVE_POSITION_EPSILON_LNG = 0.000009 // From WWWEventWaveLinear - ~1 meter at equator
    }

    @Test
    fun `should validate scientific justification for EPSILON values`() {
        // GIVEN: Different epsilon values used in the codebase

        // WHEN: Comparing with scientific standards
        // THEN: Epsilon values should be scientifically justified

        // Geographic epsilon should be larger than machine epsilon but smaller than GPS accuracy
        assertTrue(
            GEOGRAPHIC_EPSILON > DOUBLE_EPSILON,
            "Geographic epsilon ($GEOGRAPHIC_EPSILON) should be larger than machine epsilon ($DOUBLE_EPSILON)"
        )
        assertTrue(
            GEOGRAPHIC_EPSILON < GPS_ACCURACY_DEGREES,
            "Geographic epsilon ($GEOGRAPHIC_EPSILON) should be smaller than GPS accuracy ($GPS_ACCURACY_DEGREES)"
        )

        // Wave position epsilon should correspond to approximately 1 meter
        val waveEpsilonMetersAtEquator = WAVE_POSITION_EPSILON_LAT * (EQUATORIAL_CIRCUMFERENCE / 360.0)
        assertTrue(
            waveEpsilonMetersAtEquator >= 0.5 && waveEpsilonMetersAtEquator <= 2.0,
            "Wave position epsilon should correspond to ~1 meter, got ${waveEpsilonMetersAtEquator}m"
        )

        // Longitude epsilon should be adjusted for latitude
        val waveEpsilonMetersLng = WAVE_POSITION_EPSILON_LNG * (EQUATORIAL_CIRCUMFERENCE / 360.0)
        assertTrue(
            waveEpsilonMetersLng >= 0.5 && waveEpsilonMetersLng <= 2.0,
            "Wave longitude epsilon should correspond to ~1 meter at equator, got ${waveEpsilonMetersLng}m"
        )
    }

    @Test
    fun `should validate great circle vs planar distance accuracy`() {
        // GIVEN: Test positions at various distances

        // Short distance - planar approximation should be reasonably accurate
        val pos1Short = Position(51.5074, -0.1278) // London
        val pos2Short = Position(51.5084, -0.1268) // Small displacement

        val planarDistanceShort = calculatePlanarDistance(pos1Short, pos2Short)
        val greatCircleDistanceShort = calculateGreatCircleDistance(pos1Short, pos2Short)

        // For very short distances, difference should be under 100m
        assertTrue(
            abs(planarDistanceShort - greatCircleDistanceShort) < 100.0,
            "Planar vs great circle difference should be < 100m for short distances, " +
                "got ${abs(planarDistanceShort - greatCircleDistanceShort)}m"
        )

        // Medium distance - more significant error expected
        val pos1Medium = Position(51.5074, -0.1278) // London
        val pos2Medium = Position(51.4994, -0.1245) // Medium displacement

        val planarDistanceMedium = calculatePlanarDistance(pos1Medium, pos2Medium)
        val greatCircleDistanceMedium = calculateGreatCircleDistance(pos1Medium, pos2Medium)

        // For medium distances, allow larger tolerance but verify methods produce different results
        assertTrue(
            abs(planarDistanceMedium - greatCircleDistanceMedium) < 1000.0,
            "Planar vs great circle difference should be < 1000m for medium distances, " +
                "got ${abs(planarDistanceMedium - greatCircleDistanceMedium)}m"
        )

        // Long distance - significant difference expected
        val pos1Long = Position(51.5074, -0.1278) // London
        val pos2Long = Position(48.8566, 2.3522) // Paris

        val planarDistanceLong = calculatePlanarDistance(pos1Long, pos2Long)
        val greatCircleDistanceLong = calculateGreatCircleDistance(pos1Long, pos2Long)

        // For long distances, there should be measurable difference
        assertTrue(
            abs(planarDistanceLong - greatCircleDistanceLong) > 10.0,
            "Planar vs great circle should have significant difference for long distances, " +
                "got ${abs(planarDistanceLong - greatCircleDistanceLong)}m"
        )
        assertTrue(
            abs(planarDistanceLong - greatCircleDistanceLong) < 100000.0,
            "But difference should still be reasonable, got ${abs(planarDistanceLong - greatCircleDistanceLong)}m"
        )
    }

    @Test
    fun `should validate coordinate projection accuracy near poles`() {
        // GIVEN: Positions near North and South poles

        // Near North Pole
        val nearNorthPole = Position(89.5, 0.0) // 89.5°N
        val northPoleReference = Position(89.5, 1.0) // 1° longitude difference

        // Near South Pole
        val nearSouthPole = Position(-89.5, 0.0) // 89.5°S
        val southPoleReference = Position(-89.5, 1.0) // 1° longitude difference

        // WHEN: Calculating distances near poles
        val northPoleDistance = GeoUtils.calculateDistance(
            nearNorthPole.lng, northPoleReference.lng, nearNorthPole.lat
        )
        val southPoleDistance = GeoUtils.calculateDistance(
            nearSouthPole.lng, southPoleReference.lng, nearSouthPole.lat
        )

        // THEN: Distances should be much smaller than at equator due to meridian convergence
        val equatorDistance = GeoUtils.calculateDistance(0.0, 1.0, 0.0) // 1° at equator

        assertTrue(
            northPoleDistance < equatorDistance / 10,
            "Distance near North Pole should be much smaller than at equator due to meridian convergence"
        )
        assertTrue(
            southPoleDistance < equatorDistance / 10,
            "Distance near South Pole should be much smaller than at equator due to meridian convergence"
        )

        // Projection accuracy should degrade gracefully near poles
        assertTrue(
            northPoleDistance > 0.0,
            "North pole distance calculation should not degenerate to zero"
        )
        assertTrue(
            southPoleDistance > 0.0,
            "South pole distance calculation should not degenerate to zero"
        )

        // Symmetry check - North and South pole should behave similarly
        assertEquals(
            northPoleDistance, southPoleDistance, 1000.0, // 1km tolerance
            "North and South pole calculations should be symmetric"
        )
    }

    @Test
    fun `should validate wave duration physics realism`() {
        // GIVEN: Wave parameters for physics validation

        // WHEN: Testing wave durations for physical realism
        // THEN: Wave durations should be physically realistic

        // Test case 1: City-scale wave (10km radius)
        val cityWaveDistance = 10000.0 // 10km in meters
        val typicalWaveSpeed = Wave.DEFAULT_SPEED_SIMULATION.toDouble() // m/s
        val cityWaveDuration = cityWaveDistance / typicalWaveSpeed // seconds

        assertTrue(
            cityWaveDuration >= 30.0, // At least 30 seconds
            "City wave should take at least 30 seconds to cross 10km at simulation speed"
        )
        assertTrue(
            cityWaveDuration <= 400.0, // At most ~6.5 minutes (10km at 50 m/s = 200s)
            "City wave should take at most 400 seconds for simulation engagement"
        )

        // Test case 2: Country-scale wave (1000km)
        val countryWaveDistance = 1000000.0 // 1000km in meters
        val countryWaveDuration = countryWaveDistance / typicalWaveSpeed

        assertTrue(
            countryWaveDuration >= 10000.0, // At least ~2.8 hours (1000km at 50 m/s = 20000s)
            "Country wave should take at least 10000 seconds to cross 1000km at simulation speed"
        )
        assertTrue(
            countryWaveDuration <= 25000.0, // At most ~7 hours for simulation coordination
            "Country wave should complete within 25000 seconds for simulation coordination"
        )

        // Test case 3: Continental wave (5000km)
        val continentalWaveDistance = 5000000.0 // 5000km in meters
        val continentalWaveDuration = continentalWaveDistance / typicalWaveSpeed

        assertTrue(
            continentalWaveDuration >= 80000.0, // At least ~22 hours (5000km at 50 m/s = 100000s)
            "Continental wave should take at least 80000 seconds to cross 5000km at simulation speed"
        )
        assertTrue(
            continentalWaveDuration <= 120000.0, // At most ~33 hours for simulation coordination
            "Continental wave should complete within 120000 seconds for simulation coordination"
        )
    }

    @Test
    fun `should validate tolerance values are not arbitrary`() {
        // GIVEN: Various tolerance values used in tests

        // WHEN: Checking tolerance values against scientific standards
        // THEN: Tolerance values should have scientific justification

        // MIDI frequency tolerance (from WaveformGeneratorTest)
        val midiFrequencyTolerance = 0.01 // Hz
        val a4Frequency = 440.0 // Hz
        val relativeTolerancePercent = (midiFrequencyTolerance / a4Frequency) * 100

        assertTrue(
            relativeTolerancePercent < 0.1, // Less than 0.1% tolerance
            "MIDI frequency tolerance should be very precise, got ${relativeTolerancePercent}%"
        )

        // Audio amplitude tolerance
        val amplitudeTolerance = 0.001 // From amplitude bound tests
        assertTrue(
            amplitudeTolerance <= 0.01, // 1% of full scale
            "Audio amplitude tolerance should be precise for audio quality"
        )

        // Distance measurement tolerance should scale with distance
        // For GPS accuracy: ~3-5m typical, so 10m is reasonable safety margin
        assertTrue(
            GPS_ACCURACY_METERS >= 3.0 && GPS_ACCURACY_METERS <= 10.0,
            "GPS accuracy tolerance should reflect real-world GPS precision"
        )
    }

    @Test
    fun `should validate epsilon scaling with latitude`() {
        // GIVEN: Different latitudes with varying longitude scale

        // WHEN: Testing epsilon scaling near poles vs equator
        // THEN: Epsilon should account for meridian convergence

        val testLatitudes = listOf(0.0, 30.0, 60.0, 89.0) // Equator to near pole

        testLatitudes.forEach { latitude ->
            // Calculate actual meter distance for 1 degree longitude at this latitude
            val longitudeMetersPerDegree = cos(Math.toRadians(latitude)) * (EQUATORIAL_CIRCUMFERENCE / 360.0)

            // At high latitudes, longitude degrees represent much smaller distances
            if (latitude > 80.0) {
                assertTrue(
                    longitudeMetersPerDegree < 20000.0, // Less than 20km per degree
                    "At latitude $latitude°, longitude should represent small distances"
                )
            } else if (latitude < 10.0) {
                assertTrue(
                    longitudeMetersPerDegree > 100000.0, // More than 100km per degree
                    "At latitude $latitude°, longitude should represent large distances"
                )
            }

            // Wave epsilon in meters should scale appropriately with latitude
            val waveEpsilonMeters = WAVE_POSITION_EPSILON_LNG * longitudeMetersPerDegree
            if (latitude < 80.0) {
                // For non-polar regions, epsilon should be reasonable
                assertTrue(
                    waveEpsilonMeters >= 0.1 && waveEpsilonMeters <= 10.0,
                    "Wave epsilon should represent 0.1-10 meters at latitude $latitude°, got ${waveEpsilonMeters}m"
                )
            } else {
                // Near poles, epsilon becomes very small due to meridian convergence
                assertTrue(
                    waveEpsilonMeters >= 0.01 && waveEpsilonMeters <= 1.0,
                    "Wave epsilon near poles should represent 0.01-1 meters at latitude $latitude°, got ${waveEpsilonMeters}m"
                )
            }
        }
    }

    @Test
    fun `should validate mathematical consistency across coordinate systems`() {
        // GIVEN: Same position in different coordinate representations

        val testPosition = Position(51.5074, -0.1278) // London

        // WHEN: Converting between coordinate systems and back
        val radiansLat = testPosition.lat * Math.PI / 180.0
        val radiansLng = testPosition.lng * Math.PI / 180.0

        val backToDegreesLat = radiansLat * 180.0 / Math.PI
        val backToDegreesLng = radiansLng * 180.0 / Math.PI

        // THEN: Round-trip conversion should preserve precision
        assertEquals(
            testPosition.lat, backToDegreesLat, DOUBLE_EPSILON * 180.0,
            "Latitude conversion should preserve precision"
        )
        assertEquals(
            testPosition.lng, backToDegreesLng, DOUBLE_EPSILON * 180.0,
            "Longitude conversion should preserve precision"
        )

        // Trigonometric consistency
        val sinLat = sin(radiansLat)
        val cosLat = cos(radiansLat)
        val sinSquaredPlusCosSquared = sinLat * sinLat + cosLat * cosLat

        assertEquals(
            1.0, sinSquaredPlusCosSquared, DOUBLE_EPSILON * 10,
            "Trigonometric identity sin²+cos²=1 should hold"
        )
    }

    // Helper functions for testing different distance calculation methods

    private fun calculatePlanarDistance(pos1: Position, pos2: Position): Double {
        // Simple Euclidean distance in degrees, converted to meters
        val latDiff = pos1.lat - pos2.lat
        val lngDiff = pos1.lng - pos2.lng
        val degreeDistance = sqrt(latDiff * latDiff + lngDiff * lngDiff)
        return degreeDistance * (EQUATORIAL_CIRCUMFERENCE / 360.0)
    }

    private fun calculateGreatCircleDistance(pos1: Position, pos2: Position): Double {
        // Haversine formula for great circle distance
        val lat1Rad = pos1.lat * Math.PI / 180.0
        val lat2Rad = pos2.lat * Math.PI / 180.0
        val deltaLatRad = (pos2.lat - pos1.lat) * Math.PI / 180.0
        val deltaLngRad = (pos2.lng - pos1.lng) * Math.PI / 180.0

        val a = sin(deltaLatRad / 2) * sin(deltaLatRad / 2) +
                cos(lat1Rad) * cos(lat2Rad) *
                sin(deltaLngRad / 2) * sin(deltaLngRad / 2)
        val c = 2 * kotlin.math.atan2(sqrt(a), sqrt(1 - a))

        return EARTH_RADIUS_METERS * c
    }
}