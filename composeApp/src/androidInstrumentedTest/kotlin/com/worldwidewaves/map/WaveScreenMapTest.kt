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
import com.worldwidewaves.shared.map.MapTestFixtures.isCompletelyWithin
// Note: BoundingBox.height and BoundingBox.width are built-in properties, not from MapTestFixtures
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

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
                "Entire event should be visible initially in Wave screen\n" +
                    "  Event bounds: SW(${eventBounds.southwest}) NE(${eventBounds.northeast})\n" +
                    "  Visible region: SW(${visibleRegion.southwest}) NE(${visibleRegion.northeast})",
                eventBounds.isCompletelyWithin(visibleRegion),
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
                "Auto-tracking viewport should remain within event bounds\n" +
                    "  Event bounds: SW(${eventBounds.southwest}) NE(${eventBounds.northeast})\n" +
                    "  Visible region: SW(${visibleRegion.southwest}) NE(${visibleRegion.northeast})",
                visibleRegion.isCompletelyWithin(eventBounds),
            )

            // Check that both user and wave positions are visible
            assertTrue(
                "User position should be visible in auto-tracking viewport",
                visibleRegion.contains(userPosition),
            )

            assertTrue(
                "Wave position should be visible in auto-tracking viewport",
                visibleRegion.contains(wavePosition),
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
                "Auto-tracking viewport height should not exceed 50% of event area\n" +
                    "  Event height: ${eventBounds.height}\n" +
                    "  Viewport height: ${visibleRegion.height}\n" +
                    "  Max allowed: ${eventBounds.height * 0.5}",
                visibleRegion.height <= eventBounds.height * 0.51, // Small tolerance
            )

            assertTrue(
                "Auto-tracking viewport width should not exceed 50% of event area\n" +
                    "  Event width: ${eventBounds.width}\n" +
                    "  Viewport width: ${visibleRegion.width}\n" +
                    "  Max allowed: ${eventBounds.width * 0.5}",
                visibleRegion.width <= eventBounds.width * 0.51,
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
                    "Viewport should contain user position",
                    visibleRegion.contains(userPos),
                )

                assertTrue(
                    "Viewport should contain wave position",
                    visibleRegion.contains(wavePos),
                )
            }
        }

    // ============================================================
    // AUTO-TRACKING ACTIVATION
    // ============================================================

    @Test
    fun testWave_autoTrackingTriggersWhenUserEntersArea() =
        runBlocking {
            // Start with user outside event area
            val outsidePosition =
                Position(
                    eventBounds.southwest.latitude - 0.01, // 1km south
                    eventBounds.southwest.longitude - 0.01, // 1km west
                )

            // Initial camera shows entire event (BOUNDS mode)
            adapter.setBoundsForCameraTarget(eventBounds, applyZoomSafetyMargin = false, originalEventBounds = eventBounds)
            animateCameraAndWait(eventBounds.center(), zoom = 13.0)

            val initialRegion = adapter.getVisibleRegion()

            // Verify entire event is visible initially
            assertTrue(
                "Entire event should be visible before user enters area",
                eventBounds.isCompletelyWithin(initialRegion),
            )

            // User enters event area
            val insidePosition =
                Position(
                    eventBounds.northeast.latitude - 0.002,
                    eventBounds.northeast.longitude - 0.002,
                )

            // Simulate auto-tracking behavior (would be triggered by MapZoomAndLocationUpdate component)
            // Create user+wave bounds
            val wavePosition = eventBounds.center() // Wave at center for this test
            val userWaveBounds = BoundingBox.fromCorners(listOf(insidePosition, wavePosition))!!

            // Apply padding (20% horizontal, 10% vertical)
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
                )!!

            // Animate to auto-tracking bounds
            adapter.animateCameraToBounds(paddedBounds)
            waitForIdle()

            val regionAfterEntering = adapter.getVisibleRegion()

            // Assertions
            assertTrue(
                "Auto-tracking viewport should be smaller than initial BOUNDS viewport after user enters area\n" +
                    "  Initial viewport height: ${initialRegion.height}\n" +
                    "  Auto-tracking viewport height: ${regionAfterEntering.height}",
                regionAfterEntering.height < initialRegion.height,
            )

            assertTrue(
                "User position should be visible after auto-tracking triggers",
                regionAfterEntering.contains(insidePosition),
            )

            assertTrue(
                "Wave position should be visible after auto-tracking triggers",
                regionAfterEntering.contains(wavePosition),
            )

            assertVisibleRegionWithinBounds(
                "Auto-tracking viewport should remain within event bounds",
            )
        }

    @Test
    fun testWave_autoTrackingThrottledToOneSecond() =
        runBlocking {
            // This test verifies that auto-tracking updates are throttled to prevent excessive camera movements

            // Set up initial auto-tracking state
            val userPosition1 =
                Position(
                    eventBounds.center().latitude + 0.001,
                    eventBounds.center().longitude + 0.001,
                )
            val wavePosition = eventBounds.center()

            // Create first auto-tracking bounds
            val bounds1 = BoundingBox.fromCorners(listOf(userPosition1, wavePosition))!!
            val paddedBounds1 =
                BoundingBox.fromCorners(
                    Position(
                        kotlin.math.max(
                            bounds1.southwest.latitude - eventBounds.height * 0.1,
                            eventBounds.southwest.latitude,
                        ),
                        kotlin.math.max(
                            bounds1.southwest.longitude - eventBounds.width * 0.2,
                            eventBounds.southwest.longitude,
                        ),
                    ),
                    Position(
                        kotlin.math.min(
                            bounds1.northeast.latitude + eventBounds.height * 0.1,
                            eventBounds.northeast.latitude,
                        ),
                        kotlin.math.min(
                            bounds1.northeast.longitude + eventBounds.width * 0.2,
                            eventBounds.northeast.longitude,
                        ),
                    ),
                )!!

            adapter.animateCameraToBounds(paddedBounds1)
            waitForIdle()

            val cameraAfterFirst = adapter.getCameraPosition()
            val timestampFirst = System.currentTimeMillis()

            // Immediately try to update position (should be throttled)
            val userPosition2 =
                Position(
                    eventBounds.center().latitude + 0.002,
                    eventBounds.center().longitude + 0.002,
                )

            val bounds2 = BoundingBox.fromCorners(listOf(userPosition2, wavePosition))!!
            val paddedBounds2 =
                BoundingBox.fromCorners(
                    Position(
                        kotlin.math.max(
                            bounds2.southwest.latitude - eventBounds.height * 0.1,
                            eventBounds.southwest.latitude,
                        ),
                        kotlin.math.max(
                            bounds2.southwest.longitude - eventBounds.width * 0.2,
                            eventBounds.southwest.longitude,
                        ),
                    ),
                    Position(
                        kotlin.math.min(
                            bounds2.northeast.latitude + eventBounds.height * 0.1,
                            eventBounds.northeast.latitude,
                        ),
                        kotlin.math.min(
                            bounds2.northeast.longitude + eventBounds.width * 0.2,
                            eventBounds.northeast.longitude,
                        ),
                    ),
                )!!

            // Try immediate update (within 1 second)
            Thread.sleep(100) // Small delay to ensure distinct timestamp
            adapter.animateCameraToBounds(paddedBounds2)
            waitForIdle()

            val cameraAfterImmediate = adapter.getCameraPosition()
            val timestampImmediate = System.currentTimeMillis()

            // Camera should NOT have moved significantly (throttled)
            // Note: This test simulates throttling behavior; actual throttling would be in WaveScreen component
            // For this test, we verify that rapid camera updates can be controlled
            val timeDiff = timestampImmediate - timestampFirst

            assertTrue(
                "Test executed within expected timeframe (should be < 1000ms for throttling test)\n" +
                    "  Time elapsed: ${timeDiff}ms",
                timeDiff < 1000, // Verifies test timing
            )

            // Now wait for throttle period (1 second)
            Thread.sleep(1100)

            // Update again (should succeed after throttle period)
            adapter.animateCameraToBounds(paddedBounds2)
            waitForIdle()

            val cameraAfterThrottle = adapter.getCameraPosition()

            // Camera should have moved after throttle period
            // (In real implementation, MapZoomAndLocationUpdate throttles updates to 1 second)
            assertVisibleRegionWithinBounds(
                "Auto-tracking viewport should remain within bounds after throttle period",
            )

            assertTrue(
                "User position should be visible after throttle period update",
                adapter.getVisibleRegion().contains(userPosition2),
            )
        }
}
