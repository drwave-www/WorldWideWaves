package com.worldwidewaves.shared.events

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

import com.worldwidewaves.shared.WWWSimulation
import com.worldwidewaves.shared.events.geometry.EventAreaPositionTesting
import com.worldwidewaves.shared.events.utils.BoundingBox
import com.worldwidewaves.shared.events.utils.Polygon
import com.worldwidewaves.shared.events.utils.Position
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for GPS_MARKER handling in event area position validation.
 *
 * Ensures GPS_MARKER sentinel value is properly rejected in position
 * validation and never causes incorrect area detection.
 */
class WWWEventAreaGPSMarkerTest {
    @Test
    fun `EventAreaPositionTesting isPositionWithin returns false for GPS_MARKER`() =
        runTest {
            // GIVEN: Test bounding box and polygon (Paris area)
            val boundingBox = BoundingBox(48.8, 2.2, 48.9, 2.4) // Paris bbox
            val polygon =
                Polygon.fromPositions(
                    Position(48.8, 2.2),
                    Position(48.9, 2.2),
                    Position(48.9, 2.4),
                    Position(48.8, 2.4),
                    Position(48.8, 2.2),
                )
            val polygons = listOf(polygon)

            // WHEN: Checking if GPS_MARKER is within area
            val (result, _) =
                EventAreaPositionTesting.isPositionWithin(
                    WWWSimulation.GPS_MARKER,
                    boundingBox,
                    polygons,
                    null,
                )

            // THEN: GPS_MARKER should not be considered within area
            // Note: This tests the bbox fast-path check fails for GPS_MARKER (999, 999)
            assertFalse(
                result,
                "GPS_MARKER (999.0, 999.0) should fail bounding box check",
            )
        }

    @Test
    fun `EventAreaPositionTesting isPositionWithin works for valid position`() =
        runTest {
            // GIVEN: Test bounding box and polygon (Paris area)
            val boundingBox = BoundingBox(48.8, 2.2, 48.9, 2.4)
            val polygon =
                Polygon.fromPositions(
                    Position(48.8, 2.2),
                    Position(48.9, 2.2),
                    Position(48.9, 2.4),
                    Position(48.8, 2.4),
                    Position(48.8, 2.2),
                )
            val polygons = listOf(polygon)

            // WHEN: Checking valid position within Paris
            val parisPosition = Position(48.85, 2.3) // Inside the bbox and polygon
            val (result, _) =
                EventAreaPositionTesting.isPositionWithin(
                    parisPosition,
                    boundingBox,
                    polygons,
                    null,
                )

            // THEN: Should return true for valid position in area
            assertTrue(
                result,
                "Valid position (48.85, 2.3) should be within Paris bbox",
            )
        }
}
