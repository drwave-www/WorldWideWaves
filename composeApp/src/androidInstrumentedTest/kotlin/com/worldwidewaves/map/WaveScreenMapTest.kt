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

package com.worldwidewaves.map

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.worldwidewaves.shared.events.utils.BoundingBox
import com.worldwidewaves.shared.events.utils.Position
import com.worldwidewaves.shared.map.MapTestFixtures.center
import com.worldwidewaves.shared.map.MapTestFixtures.height
import com.worldwidewaves.shared.map.MapTestFixtures.isCompletelyWithin
import com.worldwidewaves.shared.map.MapTestFixtures.width
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertTrue

/**
 * Integration tests for Wave Participation Screen map behavior.
 *
 * Wave Screen Configuration:
 * - initialCameraPosition = BOUNDS (entire event visible)
 * - gesturesEnabled = false (read-only map)
 * - autoTargetUserOnFirstLocation = false
 * - Auto-tracking enabled (via MapZoomAndLocationUpdate component)
 *
 * Expected Behavior:
 * - Initial: entire event visible (BOUNDS mode)
 * - When user enters area: auto-tracks user + wave
 * - Throttled updates: every 1 second real time
 * - Auto-tracking respects event bounds constraints
 * - All gestures blocked
 *
 * All tests use headless MapView (no UI rendering) for speed and reliability.
 */
@RunWith(AndroidJUnit4::class)
class WaveScreenMapTest : BaseMapIntegrationTest() {
    // ============================================================
    // INITIAL CAMERA POSITION
    // ============================================================

    @Test
    fun testWave_initialCameraShowsEntireEventArea() =
        runBlocking {
            // Apply BOUNDS mode configuration
            adapter.setBoundsForCameraTarget(
                constraintBounds = eventBounds,
                applyZoomSafetyMargin = false,
                originalEventBounds = eventBounds,
            )

            // Move to bounds (simulates WaveScreen initial setup)
            animateCameraAndWait(eventBounds.center(), zoom = 13.0)

            // Assertions
            val visibleRegion = adapter.getVisibleRegion()

            // Camera should be centered on event
            assertCameraAt(
                eventBounds.center(),
                message = "Camera should be centered on event initially",
            )

            // Entire event should be visible
            assertTrue(
                eventBounds.isCompletelyWithin(visibleRegion),
                "Entire event should be visible initially in Wave screen\n" +
                    "  Event bounds: SW(${eventBounds.southwest}) NE(${eventBounds.northeast})\n" +
                    "  Visible region: SW(${visibleRegion.southwest}) NE(${visibleRegion.northeast})",
            )
        }

    // ============================================================
    // AUTO-TRACKING BOUNDS CONSTRAINTS
    // ============================================================

    @Test
    fun testWave_targetUserAndWave_createsBoundsWithinEvent() =
        runBlocking {
            val userPosition =
                Position(
                    eventBounds.northeast.latitude - 0.002,
                    eventBounds.northeast.longitude - 0.002,
                )

            val wavePosition =
                Position(
                    userPosition.latitude,
                    eventBounds.southwest.longitude + 0.002, // Wave at opposite side
                )

            // Create bounds containing user and wave (simulates targetUserAndWave)
            val userWaveBounds = BoundingBox.fromCorners(listOf(userPosition, wavePosition))!!

            // Apply 20% horizontal, 10% vertical padding (as per spec)
            val horizontalPadding = eventBounds.width * 0.2
            val verticalPadding = eventBounds.height * 0.1

            val paddedBounds =
                BoundingBox.fromCorners(
                    Position(
                        kotlin.math.max(
                            userWaveBounds.southwest.latitude - verticalPadding,
                            eventBounds.southwest.latitude,
                        ),
                        kotlin.math.max(
                            userWaveBounds.southwest.longitude - horizontalPadding,
                            eventBounds.southwest.longitude,
                        ),
                    ),
                    Position(
                        kotlin.math.min(
                            userWaveBounds.northeast.latitude + verticalPadding,
                            eventBounds.northeast.latitude,
                        ),
                        kotlin.math.min(
                            userWaveBounds.northeast.longitude + horizontalPadding,
                            eventBounds.northeast.longitude,
                        ),
                    ),
                )

            // Animate to padded bounds
            adapter.animateCameraToBounds(paddedBounds!!)
            waitForIdle()

            val visibleRegion = adapter.getVisibleRegion()

            // Assertions
            assertTrue(
                visibleRegion.isCompletelyWithin(eventBounds),
                "Auto-tracking viewport should remain within event bounds\n" +
                    "  Event bounds: SW(${eventBounds.southwest}) NE(${eventBounds.northeast})\n" +
                    "  Visible region: SW(${visibleRegion.southwest}) NE(${visibleRegion.northeast})",
            )

            // Check that both user and wave positions are visible
            assertTrue(
                visibleRegion.contains(userPosition),
                "User position should be visible in auto-tracking viewport",
            )

            assertTrue(
                visibleRegion.contains(wavePosition),
                "Wave position should be visible in auto-tracking viewport",
            )
        }

    @Test
    fun testWave_targetUserAndWave_respectsMaxBoundsSize() =
        runBlocking {
            // User far from wave (near opposite edges)
            val userPosition =
                Position(
                    eventBounds.northeast.latitude - 0.001,
                    eventBounds.northeast.longitude - 0.001,
                )

            val wavePosition =
                Position(
                    userPosition.latitude,
                    eventBounds.southwest.longitude + 0.001,
                )

            // Create initial bounds
            val userWaveBounds = BoundingBox.fromCorners(listOf(userPosition, wavePosition))!!

            // Apply padding
            val horizontalPadding = eventBounds.width * 0.2
            val verticalPadding = eventBounds.height * 0.1

            var paddedBounds =
                BoundingBox.fromCorners(
                    Position(
                        kotlin.math.max(
                            userWaveBounds.southwest.latitude - verticalPadding,
                            eventBounds.southwest.latitude,
                        ),
                        kotlin.math.max(
                            userWaveBounds.southwest.longitude - horizontalPadding,
                            eventBounds.southwest.longitude,
                        ),
                    ),
                    Position(
                        kotlin.math.min(
                            userWaveBounds.northeast.latitude + verticalPadding,
                            eventBounds.northeast.latitude,
                        ),
                        kotlin.math.min(
                            userWaveBounds.northeast.longitude + horizontalPadding,
                            eventBounds.northeast.longitude,
                        ),
                    ),
                )!!

            // Apply max bounds size limit (50% of event area)
            val maxLatSpan = eventBounds.height * 0.5
            val maxLngSpan = eventBounds.width * 0.5

            if (paddedBounds.height > maxLatSpan || paddedBounds.width > maxLngSpan) {
                val midLat = (paddedBounds.southwest.latitude + paddedBounds.northeast.latitude) / 2.0
                val midLng = (paddedBounds.southwest.longitude + paddedBounds.northeast.longitude) / 2.0

                val useLat = kotlin.math.min(paddedBounds.height, maxLatSpan) / 2.0
                val useLng = kotlin.math.min(paddedBounds.width, maxLngSpan) / 2.0

                paddedBounds =
                    BoundingBox.fromCorners(
                        Position(
                            kotlin.math.max(midLat - useLat, eventBounds.southwest.latitude),
                            kotlin.math.max(midLng - useLng, eventBounds.southwest.longitude),
                        ),
                        Position(
                            kotlin.math.min(midLat + useLat, eventBounds.northeast.latitude),
                            kotlin.math.min(midLng + useLng, eventBounds.northeast.longitude),
                        ),
                    )!!
            }

            adapter.animateCameraToBounds(paddedBounds)
            waitForIdle()

            val visibleRegion = adapter.getVisibleRegion()

            // Assertions - should be limited to 50% of event area
            assertTrue(
                visibleRegion.height <= eventBounds.height * 0.51, // Small tolerance
                "Auto-tracking viewport height should not exceed 50% of event area\n" +
                    "  Event height: ${eventBounds.height}\n" +
                    "  Viewport height: ${visibleRegion.height}\n" +
                    "  Max allowed: ${eventBounds.height * 0.5}",
            )

            assertTrue(
                visibleRegion.width <= eventBounds.width * 0.51,
                "Auto-tracking viewport width should not exceed 50% of event area\n" +
                    "  Event width: ${eventBounds.width}\n" +
                    "  Viewport width: ${visibleRegion.width}\n" +
                    "  Max allowed: ${eventBounds.width * 0.5}",
            )
        }

    // ============================================================
    // VIEWPORT STAYS WITHIN EVENT BOUNDS
    // ============================================================

    @Test
    fun testWave_autoTrackingViewportNeverExceedsEventBounds() =
        runBlocking {
            // Test multiple user+wave position combinations
            val testCases =
                listOf(
                    // User center, wave at west edge
                    Pair(eventBounds.center(), Position(eventBounds.center().latitude, eventBounds.southwest.longitude + 0.001)),
                    // User at north edge, wave at south edge
                    Pair(
                        Position(eventBounds.northeast.latitude - 0.001, eventBounds.center().longitude),
                        Position(eventBounds.southwest.latitude + 0.001, eventBounds.center().longitude),
                    ),
                    // User at NE corner, wave at SW corner
                    Pair(
                        Position(eventBounds.northeast.latitude - 0.001, eventBounds.northeast.longitude - 0.001),
                        Position(eventBounds.southwest.latitude + 0.001, eventBounds.southwest.longitude + 0.001),
                    ),
                )

            testCases.forEach { (userPos, wavePos) ->
                val bounds = BoundingBox.fromCorners(listOf(userPos, wavePos))!!

                // Apply padding (20% horizontal, 10% vertical)
                val horizontalPadding = eventBounds.width * 0.2
                val verticalPadding = eventBounds.height * 0.1

                val paddedBounds =
                    BoundingBox.fromCorners(
                        Position(
                            kotlin.math.max(bounds.southwest.latitude - verticalPadding, eventBounds.southwest.latitude),
                            kotlin.math.max(bounds.southwest.longitude - horizontalPadding, eventBounds.southwest.longitude),
                        ),
                        Position(
                            kotlin.math.min(bounds.northeast.latitude + verticalPadding, eventBounds.northeast.latitude),
                            kotlin.math.min(bounds.northeast.longitude + horizontalPadding, eventBounds.northeast.longitude),
                        ),
                    )!!

                adapter.animateCameraToBounds(paddedBounds)
                waitForIdle()

                val visibleRegion = adapter.getVisibleRegion()

                // Assertions
                assertVisibleRegionWithinBounds(
                    "Auto-tracking viewport should stay within event bounds for user=$userPos, wave=$wavePos",
                )

                assertTrue(
                    visibleRegion.contains(userPos),
                    "Viewport should contain user position",
                )

                assertTrue(
                    visibleRegion.contains(wavePos),
                    "Viewport should contain wave position",
                )
            }
        }
}
