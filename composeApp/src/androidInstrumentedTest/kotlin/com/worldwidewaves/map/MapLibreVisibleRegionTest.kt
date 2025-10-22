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
import com.worldwidewaves.shared.events.utils.Position
import com.worldwidewaves.shared.map.MapTestFixtures
import com.worldwidewaves.shared.map.MapTestFixtures.PORTRAIT_PHONE
import com.worldwidewaves.shared.map.MapTestFixtures.TOLERANCE_DIMENSION
import com.worldwidewaves.shared.map.MapTestFixtures.TOLERANCE_POSITION
import com.worldwidewaves.shared.map.MapTestFixtures.center
import com.worldwidewaves.shared.map.MapTestFixtures.height
import com.worldwidewaves.shared.map.MapTestFixtures.isApproximately
import com.worldwidewaves.shared.map.MapTestFixtures.width
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Integration tests for MapLibre visible region queries.
 *
 * These tests verify that MapLibre's native `getVisibleRegion()` API returns
 * accurate viewport bounds in various scenarios:
 * - Initial camera position
 * - After pan gestures (programmatic)
 * - After zoom gestures (programmatic)
 * - Calculated viewport matching
 * - Consistency across camera/zoom APIs
 * - During animations
 * - At boundary constraints
 * - Performance validation
 *
 * All tests use headless MapView (no UI rendering) for speed and reliability.
 */
@RunWith(AndroidJUnit4::class)
class MapLibreVisibleRegionTest : BaseMapIntegrationTest() {
    // ============================================================
    // VISIBLE REGION ACCURACY - INITIAL STATE
    // ============================================================

    @Test
    fun testVisibleRegionMatchesInitialCameraPosition() {
        // Set initial camera position
        val targetPosition = eventBounds.center()
        val targetZoom = 15.0

        animateCameraAndWait(targetPosition, targetZoom)

        // Query visible region
        val visibleRegion = adapter.getVisibleRegion()
        val cameraPosition = adapter.getCameraPosition()

        // Assertions
        assertVisibleRegionCenterMatchesCamera(
            message = "Visible region center should match camera position after initial positioning",
        )

        assertValidVisibleRegion(
            message = "Visible region should have valid bounds",
        )

        assertVisibleRegionWithinBounds(
            message = "Visible region should be completely within event bounds",
        )
    }

    // ============================================================
    // VISIBLE REGION AFTER PAN
    // ============================================================

    @Test
    fun testVisibleRegionUpdatesAfterPan() {
        // Start at center
        animateCameraAndWait(eventBounds.center(), zoom = 15.0)
        val initialRegion = adapter.getVisibleRegion()

        // Pan north (programmatically)
        val northPosition =
            Position(
                eventBounds.center().latitude + 0.002,
                eventBounds.center().longitude,
            )
        animateCameraAndWait(northPosition, zoom = 15.0)
        val regionAfterPan = adapter.getVisibleRegion()

        // Assertions
        assertTrue(
            "After panning north, visible region should move north\n" +
                "  Initial NE lat: ${initialRegion.northeast.latitude}\n" +
                "  After pan NE lat: ${regionAfterPan.northeast.latitude}",
            regionAfterPan.northeast.latitude > initialRegion.northeast.latitude,
        )

        assertVisibleRegionWithinBounds(
            message = "Visible region should remain within event bounds after pan",
        )

        assertValidVisibleRegion(
            message = "Visible region should have valid bounds after pan",
        )
    }

    @Test
    fun testVisibleRegionUpdatesAfterPanEast() {
        // Start at center
        animateCameraAndWait(eventBounds.center(), zoom = 15.0)
        val initialRegion = adapter.getVisibleRegion()

        // Pan east (programmatically)
        val eastPosition =
            Position(
                eventBounds.center().latitude,
                eventBounds.center().longitude + 0.002,
            )
        animateCameraAndWait(eastPosition, zoom = 15.0)
        val regionAfterPan = adapter.getVisibleRegion()

        // Assertions
        assertTrue(
            "After panning east, visible region should move east\n" +
                "  Initial NE lng: ${initialRegion.northeast.longitude}\n" +
                "  After pan NE lng: ${regionAfterPan.northeast.longitude}",
            regionAfterPan.northeast.longitude > initialRegion.northeast.longitude,
        )

        assertVisibleRegionWithinBounds(
            message = "Visible region should remain within event bounds after pan",
        )
    }

    // ============================================================
    // VISIBLE REGION AFTER ZOOM
    // ============================================================

    @Test
    fun testVisibleRegionUpdatesAfterZoom() {
        // Start at initial zoom
        val centerPosition = eventBounds.center()
        val initialZoom = 13.0
        animateCameraAndWait(centerPosition, initialZoom)

        val initialRegion = adapter.getVisibleRegion()
        val initialWidth = initialRegion.width
        val initialHeight = initialRegion.height

        // Zoom in (smaller viewport)
        val zoomedInZoom = 15.0
        animateCameraAndWait(centerPosition, zoomedInZoom)

        val regionAfterZoomIn = adapter.getVisibleRegion()
        val zoomedInWidth = regionAfterZoomIn.width
        val zoomedInHeight = regionAfterZoomIn.height

        // Assertions for zoom in
        assertTrue(
            "After zooming in, visible region width should decrease\n" +
                "  Initial width: $initialWidth\n" +
                "  Zoomed in width: $zoomedInWidth",
            zoomedInWidth < initialWidth,
        )

        assertTrue(
            "After zooming in, visible region height should decrease\n" +
                "  Initial height: $initialHeight\n" +
                "  Zoomed in height: $zoomedInHeight",
            zoomedInHeight < initialHeight,
        )

        assertVisibleRegionWithinBounds(
            message = "Visible region should remain within event bounds after zoom in",
        )

        // Zoom back out
        animateCameraAndWait(centerPosition, initialZoom)
        val regionAfterZoomOut = adapter.getVisibleRegion()
        val zoomedOutWidth = regionAfterZoomOut.width
        val zoomedOutHeight = regionAfterZoomOut.height

        // Assertions for zoom out (should return to approximately initial dimensions)
        val widthDifference = abs(zoomedOutWidth - initialWidth) / initialWidth
        val heightDifference = abs(zoomedOutHeight - initialHeight) / initialHeight

        assertTrue(
            "After zooming back out, visible region width should match initial width\n" +
                "  Initial width: $initialWidth\n" +
                "  Zoomed out width: $zoomedOutWidth\n" +
                "  Difference: ${widthDifference * 100}%",
            widthDifference < TOLERANCE_DIMENSION,
        )

        assertTrue(
            "After zooming back out, visible region height should match initial height\n" +
                "  Initial height: $initialHeight\n" +
                "  Zoomed out height: $zoomedOutHeight\n" +
                "  Difference: ${heightDifference * 100}%",
            heightDifference < TOLERANCE_DIMENSION,
        )
    }

    // ============================================================
    // VISIBLE REGION MATCHES CALCULATED VIEWPORT
    // ============================================================

    @Test
    fun testVisibleRegionMatchesCalculatedViewport() {
        val cameraPosition = eventBounds.center()
        val zoom = 14.0

        animateCameraAndWait(cameraPosition, zoom)

        val actualRegion = adapter.getVisibleRegion()
        val actualCenter = actualRegion.center()

        // Calculate expected viewport dimensions
        val (expectedWidth, expectedHeight) =
            MapTestFixtures.calculateViewportDimensions(
                cameraPosition.latitude,
                zoom,
                PORTRAIT_PHONE,
            )

        // Compare calculated vs actual
        val actualWidth = actualRegion.width
        val actualHeight = actualRegion.height

        val widthDifference = abs(actualWidth - expectedWidth) / expectedWidth
        val heightDifference = abs(actualHeight - expectedHeight) / expectedHeight

        assertTrue(
            "Actual region center should match camera position\n" +
                "  Camera: (${cameraPosition.latitude}, ${cameraPosition.longitude})\n" +
                "  Region center: (${actualCenter.latitude}, ${actualCenter.longitude})",
            actualCenter.isApproximately(cameraPosition, TOLERANCE_POSITION),
        )

        assertTrue(
            "Actual region width should match calculated width\n" +
                "  Expected width: $expectedWidth\n" +
                "  Actual width: $actualWidth\n" +
                "  Difference: ${widthDifference * 100}%",
            widthDifference < TOLERANCE_DIMENSION,
        )

        assertTrue(
            "Actual region height should match calculated height\n" +
                "  Expected height: $expectedHeight\n" +
                "  Actual height: $actualHeight\n" +
                "  Difference: ${heightDifference * 100}%",
            heightDifference < TOLERANCE_DIMENSION,
        )
    }

    // ============================================================
    // VISIBLE REGION CONSISTENCY ACROSS PLATFORM APIS
    // ============================================================

    @Test
    fun testVisibleRegionConsistentWithCameraAndZoom() {
        val targetPosition = eventBounds.center()
        val targetZoom = 14.0

        animateCameraAndWait(targetPosition, targetZoom)

        val visibleRegion = adapter.getVisibleRegion()
        val cameraPosition = adapter.getCameraPosition()
        val zoomLevel = runBlocking { adapter.currentZoom.value }

        assertVisibleRegionCenterMatchesCamera(
            message = "Visible region center should match camera position from getCameraPosition()",
        )

        // Verify zoom affects dimensions inversely
        val currentWidth = visibleRegion.width
        val currentHeight = visibleRegion.height

        // Zoom in more (viewport should shrink)
        val higherZoom = targetZoom + 2.0
        animateCameraAndWait(targetPosition, higherZoom)

        val smallerRegion = adapter.getVisibleRegion()
        val smallerWidth = smallerRegion.width
        val smallerHeight = smallerRegion.height

        assertTrue(
            "Higher zoom should result in smaller visible region width\n" +
                "  Zoom $targetZoom width: $currentWidth\n" +
                "  Zoom $higherZoom width: $smallerWidth",
            smallerWidth < currentWidth,
        )

        assertTrue(
            "Higher zoom should result in smaller visible region height\n" +
                "  Zoom $targetZoom height: $currentHeight\n" +
                "  Zoom $higherZoom height: $smallerHeight",
            smallerHeight < currentHeight,
        )
    }

    // ============================================================
    // VISIBLE REGION AT BOUNDARY CONSTRAINTS
    // ============================================================

    @Test
    fun testVisibleRegionStaysWithinBoundsAtEdges() {
        // Apply constraints
        applyConstraintsAndVerify(eventBounds, isWindowMode = true)

        val zoom = MapTestFixtures.calculateMinZoomToFit(eventBounds, PORTRAIT_PHONE)

        // Test north edge
        val northEdgePosition =
            Position(
                eventBounds.northeast.latitude - 0.001,
                eventBounds.center().longitude,
            )
        animateCameraAndWait(northEdgePosition, zoom)
        assertVisibleRegionWithinBounds("Visible region should stay within bounds at north edge")

        // Test south edge
        val southEdgePosition =
            Position(
                eventBounds.southwest.latitude + 0.001,
                eventBounds.center().longitude,
            )
        animateCameraAndWait(southEdgePosition, zoom)
        assertVisibleRegionWithinBounds("Visible region should stay within bounds at south edge")

        // Test east edge
        val eastEdgePosition =
            Position(
                eventBounds.center().latitude,
                eventBounds.northeast.longitude - 0.001,
            )
        animateCameraAndWait(eastEdgePosition, zoom)
        assertVisibleRegionWithinBounds("Visible region should stay within bounds at east edge")

        // Test west edge
        val westEdgePosition =
            Position(
                eventBounds.center().latitude,
                eventBounds.southwest.longitude + 0.001,
            )
        animateCameraAndWait(westEdgePosition, zoom)
        assertVisibleRegionWithinBounds("Visible region should stay within bounds at west edge")
    }

    // ============================================================
    // VISIBLE REGION VALIDATION - INVALID CASES
    // ============================================================

    @Test
    fun testVisibleRegionNeverInvalid() {
        // Perform 10 random camera movements
        val positions =
            listOf(
                eventBounds.center(),
                Position(eventBounds.northeast.latitude - 0.001, eventBounds.center().longitude),
                Position(eventBounds.southwest.latitude + 0.001, eventBounds.center().longitude),
                Position(eventBounds.center().latitude, eventBounds.northeast.longitude - 0.001),
                Position(eventBounds.center().latitude, eventBounds.southwest.longitude + 0.001),
                Position(
                    eventBounds.center().latitude + 0.002,
                    eventBounds.center().longitude + 0.002,
                ),
                Position(
                    eventBounds.center().latitude - 0.002,
                    eventBounds.center().longitude - 0.002,
                ),
                Position(
                    eventBounds.center().latitude + 0.001,
                    eventBounds.center().longitude - 0.001,
                ),
                Position(
                    eventBounds.center().latitude - 0.001,
                    eventBounds.center().longitude + 0.001,
                ),
                eventBounds.center(),
            )

        val zooms = listOf(13.0, 14.0, 15.0, 16.0, 14.5, 13.5, 15.5, 14.0, 15.0, 14.0)

        positions.zip(zooms).forEach { (position, zoom) ->
            animateCameraAndWait(position, zoom)

            val visibleRegion = adapter.getVisibleRegion()

            // Validate no inverted bounds
            assertTrue(
                "Visible region should not have inverted latitude bounds",
                visibleRegion.northeast.latitude > visibleRegion.southwest.latitude,
            )
            assertTrue(
                "Visible region should not have inverted longitude bounds",
                visibleRegion.northeast.longitude > visibleRegion.southwest.longitude,
            )

            // Validate positive dimensions
            val width = visibleRegion.width
            val height = visibleRegion.height

            assertTrue("Visible region width should be positive", width > 0.0)
            assertTrue("Visible region height should be positive", height > 0.0)

            // Validate reasonable dimensions (not absurd)
            assertTrue("Visible region width should not exceed 10°", width < 10.0)
            assertTrue("Visible region height should not exceed 10°", height < 10.0)

            // Validate within event bounds
            assertVisibleRegionWithinBounds(
                "Visible region should be within event bounds (iteration ${positions.indexOf(position)})",
            )
        }
    }

    // ============================================================
    // VISIBLE REGION PERFORMANCE
    // ============================================================

    @Test
    fun testVisibleRegionQueryPerformance() {
        // Set up map
        animateCameraAndWait(eventBounds.center(), zoom = 14.0)

        // Measure query performance
        val iterations = 100
        val startTime = System.nanoTime()

        repeat(iterations) {
            adapter.getVisibleRegion()
        }

        val endTime = System.nanoTime()
        val averageTimeMs = (endTime - startTime) / iterations / 1_000_000.0

        // Average query should be under 5ms
        assertTrue(
            "Average getVisibleRegion() query time should be < 5ms\n" +
                "  Actual: ${averageTimeMs}ms",
            averageTimeMs < 5.0,
        )

        // Validate all queries returned valid regions
        repeat(10) {
            assertValidVisibleRegion(
                message = "getVisibleRegion() should always return valid bounds",
            )
        }
    }

    // ============================================================
    // VISIBLE REGION DURING ANIMATION
    // ============================================================

    @Test
    fun testVisibleRegionUpdatesSmoothlyDuringAnimation() {
        // This test verifies that visible region updates smoothly during camera animation
        // without jumps or inconsistencies

        val startPosition = eventBounds.center()
        val startZoom = 13.0

        // Set initial position
        animateCameraAndWait(startPosition, startZoom)

        val initialRegion = adapter.getVisibleRegion()

        // Target position (pan to NE corner)
        val targetPosition =
            Position(
                eventBounds.northeast.latitude - 0.002,
                eventBounds.northeast.longitude - 0.002,
            )
        val targetZoom = 15.0

        // Start animation (don't wait for completion)
        adapter.animateCamera(targetPosition, targetZoom)

        // Sample visible region during animation (multiple times)
        val sampledRegions = mutableListOf<com.worldwidewaves.shared.events.utils.BoundingBox>()
        val sampleCount = 10
        val animationDurationMs = 1500L // Typical animation duration
        val sampleIntervalMs = animationDurationMs / sampleCount

        repeat(sampleCount) { i ->
            Thread.sleep(sampleIntervalMs)
            val region = adapter.getVisibleRegion()
            sampledRegions.add(region)

            // Validate each sampled region
            assertValidVisibleRegion(
                message = "Visible region should be valid during animation (sample ${i + 1}/$sampleCount)",
            )

            assertVisibleRegionWithinBounds(
                message = "Visible region should stay within bounds during animation (sample ${i + 1}/$sampleCount)",
            )
        }

        // Wait for animation to complete
        waitForIdle()

        val finalRegion = adapter.getVisibleRegion()

        // Verify smooth progression
        // 1. Regions should progressively move toward target
        val firstRegionCenter = sampledRegions.first().center()
        val lastRegionCenter = sampledRegions.last().center()

        assertTrue(
            "Visible region should move north during animation to NE corner\n" +
                "  First sample center lat: ${firstRegionCenter.latitude}\n" +
                "  Last sample center lat: ${lastRegionCenter.latitude}",
            lastRegionCenter.latitude > firstRegionCenter.latitude,
        )

        assertTrue(
            "Visible region should move east during animation to NE corner\n" +
                "  First sample center lng: ${firstRegionCenter.longitude}\n" +
                "  Last sample center lng: ${lastRegionCenter.longitude}",
            lastRegionCenter.longitude > firstRegionCenter.longitude,
        )

        // 2. Regions should progressively get smaller (zoom in)
        val firstRegionHeight = sampledRegions.first().height
        val lastRegionHeight = sampledRegions.last().height

        assertTrue(
            "Visible region should get smaller during zoom in animation\n" +
                "  First sample height: $firstRegionHeight\n" +
                "  Last sample height: $lastRegionHeight",
            lastRegionHeight < firstRegionHeight,
        )

        // 3. No discontinuities (regions should change gradually)
        for (i in 0 until sampledRegions.size - 1) {
            val current = sampledRegions[i]
            val next = sampledRegions[i + 1]

            val latDiff = next.center().latitude - current.center().latitude
            val lngDiff = next.center().longitude - current.center().longitude
            val centerMovement = sqrt(latDiff.pow(2.0) + lngDiff.pow(2.0))

            // Movement between samples should be reasonable (not teleporting)
            assertTrue(
                "Visible region should move smoothly without jumps\n" +
                    "  Sample ${i + 1} to ${i + 2} movement: $centerMovement degrees\n" +
                    "  Should be < 0.05 degrees per sample",
                centerMovement < 0.05, // Max 0.05 degrees per sample interval
            )

            val heightChange = kotlin.math.abs(next.height - current.height) / current.height

            // Height change should be gradual
            assertTrue(
                "Visible region height should change smoothly\n" +
                    "  Sample ${i + 1} to ${i + 2} height change: ${heightChange * 100}%\n" +
                    "  Should be < 30% per sample",
                heightChange < 0.3, // Max 30% height change per sample
            )
        }

        // 4. Final region should match target position
        assertCameraAt(
            targetPosition,
            tolerance = 0.01,
            message = "Final camera position should match target after animation completes",
        )

        assertZoomLevel(
            targetZoom,
            tolerance = 0.5,
            message = "Final zoom level should match target after animation completes",
        )

        // 5. Final region should be valid and within bounds
        assertValidVisibleRegion(
            message = "Final visible region should be valid after animation",
        )

        assertVisibleRegionWithinBounds(
            message = "Final visible region should be within bounds after animation",
        )
    }
}
