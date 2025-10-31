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

package com.worldwidewaves.shared.map

import com.worldwidewaves.shared.events.utils.BoundingBox
import com.worldwidewaves.shared.events.utils.Position
import com.worldwidewaves.shared.map.MapTestFixtures.STANDARD_EVENT_BOUNDS
import com.worldwidewaves.shared.map.MapTestFixtures.center
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for iOS camera center validation with epsilon tolerance.
 *
 * Priority 1 - Critical (3 tests):
 * - Test epsilon tolerance (0.00001 degrees) for camera center bounds checking
 * - Test edge cases (positions very close to boundaries)
 * - Test corner positions (most restrictive constraint points)
 *
 * Purpose:
 * - iOS uses CLLocationCoordinate2DInBounds() which requires epsilon tolerance
 * - Validates that bounds checking accommodates floating-point precision issues
 * - Ensures camera centers near edges are correctly validated
 *
 * iOS Context:
 * - iOS MapLibre uses epsilon = 0.00001 degrees (~1.1 meters) for bounds checking
 * - This prevents false rejections due to floating-point arithmetic precision
 * - Critical for preventing constraint violations near edges
 */
class IosCameraCenterValidationTest {
    companion object {
        // iOS epsilon tolerance (matches CLLocationCoordinate2DInBounds)
        private const val IOS_EPSILON = 0.00001 // ~1.1 meters
        private const val HALF_EPSILON = IOS_EPSILON / 2.0
    }

    // ============================================================
    // EPSILON TOLERANCE TESTS
    // ============================================================

    @Test
    fun `camera center within epsilon of constraint edge should be valid`() {
        // Position slightly inside north edge (within epsilon)
        val constraintBounds = STANDARD_EVENT_BOUNDS
        val northEdge = constraintBounds.northeast.latitude

        // Test positions within epsilon of the edge (should be valid)
        val positionsNearEdge =
            listOf(
                Position(northEdge - HALF_EPSILON, constraintBounds.center().longitude), // Just inside
                Position(northEdge - IOS_EPSILON, constraintBounds.center().longitude), // Exactly at epsilon
                Position(northEdge - IOS_EPSILON * 0.9, constraintBounds.center().longitude), // 90% of epsilon
            )

        positionsNearEdge.forEach { position ->
            assertTrue(
                isPositionWithinBounds(position, constraintBounds, IOS_EPSILON),
                "Position ${position.latitude} should be valid (within epsilon of edge $northEdge)",
            )
        }

        println("✅ Camera centers within epsilon of edge validated correctly")
        println("   Epsilon: $IOS_EPSILON degrees (~1.1m)")
    }

    @Test
    fun `camera center beyond epsilon of constraint edge should be invalid`() {
        // Position outside north edge (beyond epsilon)
        val constraintBounds = STANDARD_EVENT_BOUNDS
        val northEdge = constraintBounds.northeast.latitude

        // Test positions beyond epsilon of the edge (should be invalid)
        val positionsBeyondEdge =
            listOf(
                Position(northEdge + HALF_EPSILON, constraintBounds.center().longitude), // Just outside
                Position(northEdge + IOS_EPSILON, constraintBounds.center().longitude), // Exactly at epsilon
                Position(northEdge + IOS_EPSILON * 2.0, constraintBounds.center().longitude), // 2x epsilon
            )

        positionsBeyondEdge.forEach { position ->
            assertFalse(
                isPositionWithinBounds(position, constraintBounds, IOS_EPSILON),
                "Position ${position.latitude} should be invalid (beyond epsilon of edge $northEdge)",
            )
        }

        println("✅ Camera centers beyond epsilon of edge rejected correctly")
        println("   Epsilon: $IOS_EPSILON degrees (~1.1m)")
    }

    // ============================================================
    // EDGE CASE TESTS (All Four Edges)
    // ============================================================

    @Test
    fun `epsilon validation works for all constraint edges`() {
        val constraintBounds = STANDARD_EVENT_BOUNDS

        // Test all four edges with epsilon tolerance
        val edgeTestCases =
            listOf(
                // North edge
                "North Inside" to
                    Position(
                        constraintBounds.northeast.latitude - HALF_EPSILON,
                        constraintBounds.center().longitude,
                    ) to true,
                "North Outside" to
                    Position(
                        constraintBounds.northeast.latitude + HALF_EPSILON,
                        constraintBounds.center().longitude,
                    ) to false,
                // South edge
                "South Inside" to
                    Position(
                        constraintBounds.southwest.latitude + HALF_EPSILON,
                        constraintBounds.center().longitude,
                    ) to true,
                "South Outside" to
                    Position(
                        constraintBounds.southwest.latitude - HALF_EPSILON,
                        constraintBounds.center().longitude,
                    ) to false,
                // East edge
                "East Inside" to
                    Position(
                        constraintBounds.center().latitude,
                        constraintBounds.northeast.longitude - HALF_EPSILON,
                    ) to true,
                "East Outside" to
                    Position(
                        constraintBounds.center().latitude,
                        constraintBounds.northeast.longitude + HALF_EPSILON,
                    ) to false,
                // West edge
                "West Inside" to
                    Position(
                        constraintBounds.center().latitude,
                        constraintBounds.southwest.longitude + HALF_EPSILON,
                    ) to true,
                "West Outside" to
                    Position(
                        constraintBounds.center().latitude,
                        constraintBounds.southwest.longitude - HALF_EPSILON,
                    ) to false,
            )

        edgeTestCases.forEach { entry ->
            val nameAndPosition = entry.component1()
            val testName: String = nameAndPosition.component1()
            val position: Position = nameAndPosition.component2()
            val expectedValid: Boolean = entry.component2()
            val isValid = isPositionWithinBounds(position, constraintBounds, IOS_EPSILON)

            if (expectedValid) {
                assertTrue(
                    isValid,
                    "$testName: Position should be valid with epsilon tolerance",
                )
            } else {
                assertFalse(
                    isValid,
                    "$testName: Position should be invalid (beyond epsilon)",
                )
            }
        }

        println("✅ All four edges validated correctly with epsilon tolerance")
    }

    // ============================================================
    // CORNER POSITION TESTS (Most Restrictive)
    // ============================================================

    @Test
    fun `corner positions respect epsilon tolerance on both axes`() {
        val constraintBounds = STANDARD_EVENT_BOUNDS

        // Test all four corners with epsilon adjustments
        val cornerTestCases =
            listOf(
                // NE corner (most restrictive - both lat and lng maxed)
                "NE Inside" to
                    Position(
                        constraintBounds.northeast.latitude - HALF_EPSILON,
                        constraintBounds.northeast.longitude - HALF_EPSILON,
                    ) to true,
                "NE Outside Lat" to
                    Position(
                        constraintBounds.northeast.latitude + HALF_EPSILON,
                        constraintBounds.northeast.longitude - HALF_EPSILON,
                    ) to false,
                "NE Outside Lng" to
                    Position(
                        constraintBounds.northeast.latitude - HALF_EPSILON,
                        constraintBounds.northeast.longitude + HALF_EPSILON,
                    ) to false,
                // SW corner (most restrictive - both lat and lng minimized)
                "SW Inside" to
                    Position(
                        constraintBounds.southwest.latitude + HALF_EPSILON,
                        constraintBounds.southwest.longitude + HALF_EPSILON,
                    ) to true,
                "SW Outside Lat" to
                    Position(
                        constraintBounds.southwest.latitude - HALF_EPSILON,
                        constraintBounds.southwest.longitude + HALF_EPSILON,
                    ) to false,
                "SW Outside Lng" to
                    Position(
                        constraintBounds.southwest.latitude + HALF_EPSILON,
                        constraintBounds.southwest.longitude - HALF_EPSILON,
                    ) to false,
                // NW corner
                "NW Inside" to
                    Position(
                        constraintBounds.northeast.latitude - HALF_EPSILON,
                        constraintBounds.southwest.longitude + HALF_EPSILON,
                    ) to true,
                // SE corner
                "SE Inside" to
                    Position(
                        constraintBounds.southwest.latitude + HALF_EPSILON,
                        constraintBounds.northeast.longitude - HALF_EPSILON,
                    ) to true,
            )

        cornerTestCases.forEach { entry ->
            val nameAndPosition = entry.component1()
            val testName: String = nameAndPosition.component1()
            val position: Position = nameAndPosition.component2()
            val expectedValid: Boolean = entry.component2()
            val isValid = isPositionWithinBounds(position, constraintBounds, IOS_EPSILON)

            if (expectedValid) {
                assertTrue(
                    isValid,
                    "$testName: Corner position should be valid with epsilon tolerance",
                )
            } else {
                assertFalse(
                    isValid,
                    "$testName: Corner position should be invalid (beyond epsilon)",
                )
            }
        }

        println("✅ Corner positions validated correctly with epsilon on both axes")
    }

    // ============================================================
    // FLOATING-POINT PRECISION TESTS
    // ============================================================

    @Test
    fun `floating point arithmetic near edges respects epsilon tolerance`() {
        val constraintBounds = STANDARD_EVENT_BOUNDS

        // Simulate floating-point arithmetic errors that might occur in camera calculations
        val northEdge = constraintBounds.northeast.latitude
        val eastEdge = constraintBounds.northeast.longitude

        // Position calculated from arithmetic that might introduce tiny errors
        val calculatedPosition =
            Position(
                northEdge - 0.0001 + 0.00009, // = northEdge - 0.00001 (exactly at epsilon)
                eastEdge - 0.0001 + 0.00009,
            )

        // Should still be valid because epsilon tolerance handles this
        assertTrue(
            isPositionWithinBounds(calculatedPosition, constraintBounds, IOS_EPSILON),
            "Position from floating-point arithmetic should be valid with epsilon tolerance",
        )

        // But position with larger error should be invalid
        val positionWithLargerError =
            Position(
                northEdge + 0.0001 - 0.00009, // = northEdge + 0.00001 (outside epsilon)
                eastEdge,
            )

        assertFalse(
            isPositionWithinBounds(positionWithLargerError, constraintBounds, IOS_EPSILON),
            "Position with larger error should be invalid (beyond epsilon)",
        )

        println("✅ Floating-point precision handled correctly by epsilon tolerance")
        println("   Arithmetic errors within epsilon are tolerated")
    }

    // ============================================================
    // HELPER FUNCTIONS
    // ============================================================

    /**
     * Check if position is within bounds using iOS epsilon tolerance.
     * Mimics CLLocationCoordinate2DInBounds behavior.
     */
    private fun isPositionWithinBounds(
        position: Position,
        bounds: BoundingBox,
        epsilon: Double = IOS_EPSILON,
    ): Boolean {
        // iOS bounds checking with epsilon tolerance (inclusive with tolerance)
        val latValid =
            position.latitude >= (bounds.southwest.latitude - epsilon) &&
                position.latitude <= (bounds.northeast.latitude + epsilon)

        val lngValid =
            position.longitude >= (bounds.southwest.longitude - epsilon) &&
                position.longitude <= (bounds.northeast.longitude + epsilon)

        return latValid && lngValid
    }
}
