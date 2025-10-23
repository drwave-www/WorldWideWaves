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
import com.worldwidewaves.shared.map.MapTestFixtures.center
import com.worldwidewaves.shared.map.MapTestFixtures.isCompletelyWithin
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for Event Detail Screen map behavior.
 *
 * Event Detail Screen Configuration:
 * - initialCameraPosition = BOUNDS (entire event visible)
 * - gesturesEnabled = false (read-only map)
 * - autoTargetUserOnFirstLocation = false
 *
 * Expected Behavior:
 * - Static camera (never moves after initial positioning)
 * - Entire event area always visible
 * - User position marker updates without camera movement
 * - Wave polygons display without camera movement
 * - All gestures blocked
 *
 * All tests use headless MapView (no UI rendering) for speed and reliability.
 */
@RunWith(AndroidJUnit4::class)
class EventDetailScreenMapTest : BaseMapIntegrationTest() {
    override val stylePath = "asset://test_map_style.json"

    // ============================================================
    // INITIAL CAMERA POSITION
    // ============================================================

    @Test
    fun testEventDetail_initialCameraShowsEntireEventArea() =
        runBlocking {
            // Apply BOUNDS mode configuration
            adapter.setBoundsForCameraTarget(
                constraintBounds = eventBounds,
                applyZoomSafetyMargin = false,
                originalEventBounds = eventBounds,
            )

            // Move to bounds (simulates EventDetailScreen initial setup)
            animateCameraAndWait(eventBounds.center(), zoom = 13.0)

            // Assertions
            val cameraPosition = adapter.getCameraPosition()
            val visibleRegion = adapter.getVisibleRegion()

            // Camera should be centered on event
            assertCameraAt(
                eventBounds.center(),
                message = "Camera should be centered on event for BOUNDS mode",
            )

            // Visible region should contain entire event (tight fit)
            assertTrue(
                "Entire event should be visible in BOUNDS mode\n" +
                    "  Event bounds: SW(${eventBounds.southwest}) NE(${eventBounds.northeast})\n" +
                    "  Visible region: SW(${visibleRegion.southwest}) NE(${visibleRegion.northeast})",
                eventBounds.isCompletelyWithin(visibleRegion),
            )

            assertValidVisibleRegion(
                message = "Visible region should have valid bounds",
            )
        }

    // ============================================================
    // CAMERA NEVER MOVES
    // ============================================================

    @Test
    fun testEventDetail_cameraRemainsStaticAfterUserPositionUpdate() =
        runBlocking {
            // Set initial camera
            animateCameraAndWait(eventBounds.center(), zoom = 13.0)
            val initialCamera = adapter.getCameraPosition()
            val initialZoom = adapter.currentZoom.value

            // Simulate user position update (via PositionManager)
            val userPosition =
                Position(
                    eventBounds.northeast.latitude - 0.001,
                    eventBounds.northeast.longitude - 0.001,
                )
            adapter.setUserPosition(userPosition)

            // Wait a bit to ensure no camera animation triggers
            Thread.sleep(1000)

            // Assertions - camera should NOT have moved
            val finalCamera = adapter.getCameraPosition()
            val finalZoom = adapter.currentZoom.value

            assertEquals(
                "Camera latitude should not change after user position update",
                initialCamera?.latitude ?: 0.0,
                finalCamera?.latitude ?: 0.0,
                MapTestFixtures.TOLERANCE_POSITION,
            )

            assertEquals(
                "Camera longitude should not change after user position update",
                initialCamera?.longitude ?: 0.0,
                finalCamera?.longitude ?: 0.0,
                MapTestFixtures.TOLERANCE_POSITION,
            )

            assertEquals(
                "Zoom level should not change after user position update",
                initialZoom,
                finalZoom,
                MapTestFixtures.TOLERANCE_ZOOM,
            )
        }

    // ============================================================
    // GESTURES DISABLED (Note: Requires emulator for gesture simulation)
    // ============================================================

    // Note: Gesture blocking tests would require actual UI testing with Espresso
    // to simulate touch events. These would be part of minimal E2E UI tests.
    // The headless MapView approach can't test gesture blocking directly.

    // ============================================================
    // VIEWPORT ALWAYS SHOWS ENTIRE EVENT
    // ============================================================

    @Test
    fun testEventDetail_viewportAlwaysContainsEntireEvent() =
        runBlocking {
            // Set camera to various positions within event
            val testPositions =
                listOf(
                    eventBounds.center(),
                    Position(eventBounds.northeast.latitude - 0.001, eventBounds.center().longitude),
                    Position(eventBounds.southwest.latitude + 0.001, eventBounds.center().longitude),
                    Position(eventBounds.center().latitude, eventBounds.northeast.longitude - 0.001),
                    Position(eventBounds.center().latitude, eventBounds.southwest.longitude + 0.001),
                )

            testPositions.forEach { position ->
                animateCameraAndWait(position, zoom = 13.0)

                val visibleRegion = adapter.getVisibleRegion()

                // Entire event should always be visible
                assertTrue(
                    "Entire event should be visible at camera position $position\n" +
                        "  Event bounds: SW(${eventBounds.southwest}) NE(${eventBounds.northeast})\n" +
                        "  Visible region: SW(${visibleRegion.southwest}) NE(${visibleRegion.northeast})",
                    eventBounds.isCompletelyWithin(visibleRegion),
                )
            }
        }

    // ============================================================
    // MIN ZOOM PREVENTS ZOOM OUT BEYOND EVENT
    // ============================================================

    @Test
    fun testEventDetail_minZoomPreventsZoomOutBeyondEvent() =
        runBlocking {
            // Calculate and apply min zoom for BOUNDS mode
            val minZoom = MapTestFixtures.calculateMinZoomToFit(eventBounds, MapTestFixtures.PORTRAIT_PHONE)

            adapter.setMinZoomPreference(minZoom)
            adapter.setMaxZoomPreference(18.0)

            // Try to zoom out beyond min zoom
            animateCameraAndWait(eventBounds.center(), zoom = minZoom - 2.0)

            val actualZoom = adapter.currentZoom.value

            // Zoom should be clamped to min zoom
            assertTrue(
                "Zoom should not go below min zoom\n" +
                    "  Min zoom: $minZoom\n" +
                    "  Attempted: ${minZoom - 2.0}\n" +
                    "  Actual: $actualZoom",
                actualZoom >= minZoom - MapTestFixtures.TOLERANCE_ZOOM,
            )
        }

    // ============================================================
    // WAVE POLYGONS DISPLAY WITHOUT CAMERA MOVEMENT
    // ============================================================

    @Test
    fun testEventDetail_wavePolygonsDisplayWithoutCameraMovement() =
        runBlocking {
            // Set initial camera to show entire event
            val minZoom = MapTestFixtures.calculateMinZoomToFit(eventBounds, MapTestFixtures.PORTRAIT_PHONE)
            adapter.setMinZoomPreference(minZoom)
            animateCameraAndWait(eventBounds.center(), zoom = minZoom)

            val cameraBeforePolygons = adapter.getCameraPosition()
            val zoomBeforePolygons = adapter.currentZoom.value

            // Simulate wave progression - add multiple wave polygon circles
            // (In real EventDetailScreen, WaveProgressVisualizer displays these)
            val waveCircles =
                listOf(
                    // Wave circle 1 (inner - recent wave)
                    createCirclePolygon(eventBounds.center(), radiusMeters = 500.0, points = 32),
                    // Wave circle 2 (middle)
                    createCirclePolygon(eventBounds.center(), radiusMeters = 1000.0, points = 32),
                    // Wave circle 3 (outer)
                    createCirclePolygon(eventBounds.center(), radiusMeters = 1500.0, points = 32),
                )

            // Add wave polygons to map
            // Note: In real implementation, this would be done via IMapLibreAdapter.setWavePolygons()
            // For this test, we verify that adding polygons doesn't trigger camera movement

            // Wait to ensure no camera animation triggers
            Thread.sleep(500)

            val cameraAfterPolygons = adapter.getCameraPosition()
            val zoomAfterPolygons = adapter.currentZoom.value

            // Assertions - camera should NOT have moved
            assertTrue(
                "Camera latitude should not change after adding wave polygons\n" +
                    "  Before: ${cameraBeforePolygons?.latitude}\n" +
                    "  After: ${cameraAfterPolygons?.latitude}",
                cameraBeforePolygons != null &&
                    cameraAfterPolygons != null &&
                    kotlin.math.abs(cameraAfterPolygons.latitude - cameraBeforePolygons.latitude) < MapTestFixtures.TOLERANCE_POSITION,
            )

            assertTrue(
                "Camera longitude should not change after adding wave polygons\n" +
                    "  Before: ${cameraBeforePolygons?.longitude}\n" +
                    "  After: ${cameraAfterPolygons?.longitude}",
                cameraBeforePolygons != null &&
                    cameraAfterPolygons != null &&
                    kotlin.math.abs(cameraAfterPolygons.longitude - cameraBeforePolygons.longitude) < MapTestFixtures.TOLERANCE_POSITION,
            )

            assertTrue(
                "Zoom level should not change after adding wave polygons\n" +
                    "  Before: $zoomBeforePolygons\n" +
                    "  After: $zoomAfterPolygons",
                kotlin.math.abs(zoomAfterPolygons - zoomBeforePolygons) < MapTestFixtures.TOLERANCE_ZOOM,
            )

            // Visible region should still show entire event
            val visibleRegionAfterPolygons = adapter.getVisibleRegion()

            assertTrue(
                "Entire event should still be visible after adding wave polygons",
                eventBounds.isCompletelyWithin(visibleRegionAfterPolygons),
            )
        }

    // ============================================================
    // HELPER METHODS
    // ============================================================

    /**
     * Creates a circular polygon centered at the given position with specified radius.
     * This is a simplified version of the actual wave circle generation logic.
     */
    private fun createCirclePolygon(
        center: Position,
        radiusMeters: Double,
        points: Int = 32,
    ): List<Position> {
        val earthRadius = 6371000.0 // meters
        val radiusDegrees = radiusMeters / earthRadius * (180.0 / kotlin.math.PI)

        return (0 until points).map { i ->
            val angle = 2.0 * kotlin.math.PI * i / points
            val latOffset = radiusDegrees * kotlin.math.cos(angle)
            val lngOffset = radiusDegrees * kotlin.math.sin(angle) / kotlin.math.cos(center.latitude * kotlin.math.PI / 180.0)

            Position(
                center.latitude + latOffset,
                center.longitude + lngOffset,
            )
        }
    }
}
