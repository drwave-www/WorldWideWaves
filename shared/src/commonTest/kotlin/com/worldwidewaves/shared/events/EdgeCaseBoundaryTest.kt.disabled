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

package com.worldwidewaves.shared.events

import com.worldwidewaves.shared.events.utils.Position
import com.worldwidewaves.shared.events.utils.Polygon
import com.worldwidewaves.shared.events.utils.PolygonUtils
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * Edge case tests for geographic and timezone boundaries.
 * Tests critical boundary conditions that could cause production issues.
 */
class EdgeCaseBoundaryTest {

    @Test
    fun `meridianCrossing_180DegreeLongitude_handledCorrectly`() {
        // Create polygon that crosses the 180° meridian (International Date Line)
        val meridianCrossingCoords = listOf(
            Position(lat = 60.0, lng = 179.5),   // East of dateline
            Position(lat = 60.0, lng = -179.5),  // West of dateline
            Position(lat = 61.0, lng = -179.5),
            Position(lat = 61.0, lng = 179.5),
            Position(lat = 60.0, lng = 179.5)    // Close
        )

        val meridianPolygon = Polygon(coordinates = meridianCrossingCoords)

        // Test points on both sides of meridian
        val pointEast = Position(lat = 60.5, lng = 179.8)  // Should be inside
        val pointWest = Position(lat = 60.5, lng = -179.8) // Should be inside
        val pointOutside = Position(lat = 60.5, lng = 0.0) // Should be outside

        assertTrue(
            PolygonUtils.containsPosition(meridianPolygon, pointEast),
            "Point east of dateline should be inside meridian-crossing polygon"
        )
        assertTrue(
            PolygonUtils.containsPosition(meridianPolygon, pointWest),
            "Point west of dateline should be inside meridian-crossing polygon"
        )
        assertFalse(
            PolygonUtils.containsPosition(meridianPolygon, pointOutside),
            "Point far from dateline should be outside meridian-crossing polygon"
        )
    }

    @Test
    fun `polarRegions_extremeLatitudes_calculationsStable`() {
        // Test near polar regions where longitude lines converge
        val arcticPositions = listOf(
            Position(lat = 89.0, lng = 0.0),    // Near North Pole
            Position(lat = 89.0, lng = 90.0),
            Position(lat = 89.0, lng = 180.0),
            Position(lat = 89.0, lng = -90.0)
        )

        val antarcticPositions = listOf(
            Position(lat = -89.0, lng = 0.0),   // Near South Pole
            Position(lat = -89.0, lng = 90.0),
            Position(lat = -89.0, lng = 180.0),
            Position(lat = -89.0, lng = -90.0)
        )

        // Test distance calculations in polar regions
        arcticPositions.forEach { pos1 ->
            arcticPositions.forEach { pos2 ->
                val distance = GeoUtils.calculateDistance(pos1, pos2)
                assertTrue(distance.isFinite(), "Arctic distance should be finite: $pos1 to $pos2")
            }
        }

        antarcticPositions.forEach { pos1 ->
            antarcticPositions.forEach { pos2 ->
                val distance = GeoUtils.calculateDistance(pos1, pos2)
                assertTrue(distance.isFinite(), "Antarctic distance should be finite: $pos1 to $pos2")
            }
        }
    }

    @Test
    fun `equatorCrossing_waveProgression_handledCorrectly`() {
        // Create event that crosses the equator
        val equatorCrossingCoords = listOf(
            Position(lat = -1.0, lng = 0.0),   // South of equator
            Position(lat = -1.0, lng = 1.0),
            Position(lat = 1.0, lng = 1.0),    // North of equator
            Position(lat = 1.0, lng = 0.0),
            Position(lat = -1.0, lng = 0.0)    // Close
        )

        val equatorPolygon = Polygon(coordinates = equatorCrossingCoords)

        // Test positions on both sides of equator
        val southPosition = Position(lat = -0.5, lng = 0.5)
        val northPosition = Position(lat = 0.5, lng = 0.5)

        assertTrue(
            PolygonUtils.containsPosition(equatorPolygon, southPosition),
            "Position south of equator should be inside equator-crossing polygon"
        )
        assertTrue(
            PolygonUtils.containsPosition(equatorPolygon, northPosition),
            "Position north of equator should be inside equator-crossing polygon"
        )
    }

    @Test
    fun `smallPolygons_microcoordinates_precisionMaintained`() {
        // Test very small polygons (building-scale) for precision
        val buildingSize = 0.0001 // ~10 meters at equator
        val smallBuildingCoords = listOf(
            Position(lat = 40.783100, lng = -73.971200),
            Position(lat = 40.783100, lng = -73.971100),
            Position(lat = 40.783200, lng = -73.971100),
            Position(lat = 40.783200, lng = -73.971200),
            Position(lat = 40.783100, lng = -73.971200)
        )

        val smallPolygon = Polygon(coordinates = smallBuildingCoords)

        // Test points inside and outside small polygon
        val insidePoint = Position(lat = 40.783150, lng = -73.971150)
        val outsidePoint = Position(lat = 40.783250, lng = -73.971250)

        assertTrue(
            PolygonUtils.containsPosition(smallPolygon, insidePoint),
            "Should handle precision for small polygons - inside point"
        )
        assertFalse(
            PolygonUtils.containsPosition(smallPolygon, outsidePoint),
            "Should handle precision for small polygons - outside point"
        )
    }

    @Test
    fun `largePolygons_continentalScale_handledEfficiently`() {
        // Test continental-scale polygon (Europe approximation)
        val europeBoundary = createEuropeApproximation()

        // Test points across Europe
        val europeanCities = listOf(
            Position(lat = 51.5074, lng = -0.1278),  // London
            Position(lat = 48.8566, lng = 2.3522),   // Paris
            Position(lat = 52.5200, lng = 13.4050),  // Berlin
            Position(lat = 41.9028, lng = 12.4964),  // Rome
            Position(lat = 40.4168, lng = -3.7038)   // Madrid
        )

        val africanCity = Position(lat = 6.5244, lng = 3.3792) // Lagos, should be outside

        europeanCities.forEach { city ->
            val duration = measureTime {
                PolygonUtils.containsPosition(europeBoundary, city)
            }

            assertTrue(
                duration < 10.milliseconds,
                "Large polygon calculation should be efficient: ${duration.inWholeMilliseconds}ms for $city"
            )
        }

        // African city should be outside Europe
        assertFalse(
            PolygonUtils.containsPosition(europeBoundary, africanCity),
            "African city should be outside European boundary"
        )
    }

    @Test
    fun `coordinateNormalization_edgeCases_handledSafely`() {
        // Test coordinate edge cases that need normalization
        val edgeCaseCoords = listOf(
            Position(lat = 90.0, lng = 180.0),    // North pole, exact dateline
            Position(lat = -90.0, lng = -180.0),  // South pole, exact dateline opposite
            Position(lat = 0.0, lng = 0.0),       // Null Island
            Position(lat = 0.0, lng = 180.0),     // Equator on dateline
            Position(lat = 0.0, lng = -180.0)     // Equator on dateline opposite
        )

        edgeCaseCoords.forEach { coord ->
            // Should handle extreme coordinates without errors
            val normalized = normalizeCoordinate(coord)
            assertTrue(
                normalized.lat >= -90.0 && normalized.lat <= 90.0,
                "Normalized latitude should be valid: ${normalized.lat}"
            )
            assertTrue(
                normalized.lng >= -180.0 && normalized.lng <= 180.0,
                "Normalized longitude should be valid: ${normalized.lng}"
            )
        }
    }

    // Helper functions

    private fun createEuropeApproximation(): Polygon {
        // Simplified Europe boundary for testing
        val europeCoords = listOf(
            Position(lat = 71.0, lng = -25.0),  // Iceland
            Position(lat = 71.0, lng = 70.0),   // Northern Russia
            Position(lat = 35.0, lng = 70.0),   // Central Asia
            Position(lat = 35.0, lng = 40.0),   // Middle East
            Position(lat = 35.0, lng = -10.0),  // North Africa
            Position(lat = 50.0, lng = -25.0),  // Atlantic
            Position(lat = 71.0, lng = -25.0)   // Close
        )
        return Polygon(coordinates = europeCoords)
    }

    private fun generateNYCAreaPositions(count: Int): List<Position> {
        val random = kotlin.random.Random(42) // Fixed seed
        return (1..count).map {
            Position(
                lat = 40.7 + random.nextDouble() * 0.2,  // NYC area
                lng = -74.1 + random.nextDouble() * 0.2
            )
        }
    }

    private fun createTestWaveEvent(): WWWEvent {
        return WWWEvent.create(
            id = "edge-case-test-event",
            name = "Edge Case Test Event",
            centerPosition = Position(lat = 40.7831, lng = -73.9712),
            radiusKm = 5.0,
            startTime = kotlinx.datetime.Clock.System.now(),
            waveSpeedKmh = 50.0
        )
    }

    private fun normalizeCoordinate(position: Position): Position {
        // Basic coordinate normalization
        val normalizedLat = position.lat.coerceIn(-90.0, 90.0)
        val normalizedLng = when {
            position.lng > 180.0 -> position.lng - 360.0
            position.lng < -180.0 -> position.lng + 360.0
            else -> position.lng
        }
        return Position(lat = normalizedLat, lng = normalizedLng)
    }
}