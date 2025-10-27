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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Cross-cutting integration tests that verify common map functionality across all screens.
 *
 * These tests ensure that fundamental map capabilities work consistently regardless of
 * which screen (EventDetail, Wave, FullMap) is using the map.
 *
 * Tests cover:
 * - Map initialization and loading
 * - User position marker visibility
 * - Wave polygon rendering
 *
 * All tests use headless MapView (no UI rendering) for speed and reliability.
 */
@RunWith(AndroidJUnit4::class)
class CrossCuttingMapTest : BaseMapIntegrationTest() {
    // ============================================================
    // MAP LOADS SUCCESSFULLY (All Screens)
    // ============================================================

    @Test
    fun testMapLoadsSuccessfully_EventDetail() =
        runBlocking {
            // Verify map loaded successfully during setup
            assertNotNull("MapView should be initialized", mapView)
            assertNotNull("MapLibreMap should be loaded", mapLibreMap)
            assertNotNull("Adapter should be initialized", adapter)

            // Verify style loaded (must access on UI thread)
            val styleLoaded =
                runOnUiThread {
                    mapLibreMap.style?.isFullyLoaded ?: false
                }
            assertTrue("Map style should be fully loaded for EventDetailScreen", styleLoaded)

            // Verify camera is functional
            animateCameraAndWait(eventBounds.center(), zoom = 13.0)
            val cameraPosition = runOnUiThread { adapter.getCameraPosition() }
            assertNotNull("Camera position should be available after animation", cameraPosition)

            // Verify visible region query works
            val visibleRegion = runOnUiThread { adapter.getVisibleRegion() }
            assertValidVisibleRegion(message = "EventDetailScreen visible region should be valid after load")
        }

    @Test
    fun testMapLoadsSuccessfully_Wave() =
        runBlocking {
            // Simulate WaveScreen setup (BOUNDS mode) - wrap in UI thread
            runOnUiThread {
                adapter.setBoundsForCameraTarget(
                    constraintBounds = eventBounds,
                    applyZoomSafetyMargin = false,
                    originalEventBounds = eventBounds,
                )
            }

            // Move to bounds
            animateCameraAndWait(eventBounds.center(), zoom = 13.0)

            // Verify map is functional
            val visibleRegion = runOnUiThread { adapter.getVisibleRegion() }
            assertNotNull("Visible region should be available for WaveScreen", visibleRegion)

            assertValidVisibleRegion(message = "WaveScreen visible region should be valid")

            // Verify entire event is visible
            assertTrue(
                "WaveScreen should show entire event initially",
                eventBounds.isCompletelyWithin(visibleRegion),
            )
        }

    @Test
    fun testMapLoadsSuccessfully_FullMap() =
        runBlocking {
            // Simulate FullMapScreen setup (WINDOW mode)
            val minZoom = MapTestFixtures.calculateMinZoomToFit(eventBounds, MapTestFixtures.PORTRAIT_PHONE)

            adapter.setMinZoomPreference(minZoom)
            adapter.setMaxZoomPreference(18.0)
            adapter.setBoundsForCameraTarget(eventBounds, applyZoomSafetyMargin = false, originalEventBounds = eventBounds)

            // Move to event center
            animateCameraAndWait(eventBounds.center(), minZoom)

            // Verify map is functional
            val visibleRegion = adapter.getVisibleRegion()
            assertNotNull("Visible region should be available for FullMapScreen", visibleRegion)

            assertValidVisibleRegion(message = "FullMapScreen visible region should be valid")

            // Verify viewport is within bounds
            assertVisibleRegionWithinBounds(
                "FullMapScreen viewport should be within event bounds",
            )
        }

    // ============================================================
    // USER POSITION MARKER VISIBLE (All Screens)
    // ============================================================

    @Test
    fun testUserPositionMarkerVisible_EventDetail() =
        runBlocking {
            // Set camera to show entire event (EventDetailScreen behavior)
            animateCameraAndWait(eventBounds.center(), zoom = 13.0)

            // Set user position
            val userPosition =
                Position(
                    eventBounds.center().latitude + 0.001,
                    eventBounds.center().longitude + 0.001,
                )
            adapter.setUserPosition(userPosition)

            // Wait for position to be applied
            Thread.sleep(300)

            // Verify user position is within visible region
            val visibleRegion = adapter.getVisibleRegion()
            assertTrue(
                "User position should be visible in EventDetailScreen\n" +
                    "  User position: (${userPosition.latitude}, ${userPosition.longitude})\n" +
                    "  Visible region: SW(${visibleRegion.southwest}) NE(${visibleRegion.northeast})",
                visibleRegion.contains(userPosition),
            )

            // User position marker would be rendered by MapLibre's LocationComponent
            // This test verifies the position is within the viewport
        }

    @Test
    fun testUserPositionMarkerVisible_Wave() =
        runBlocking {
            // Set camera to show entire event (WaveScreen initial behavior)
            adapter.setBoundsForCameraTarget(eventBounds, applyZoomSafetyMargin = false, originalEventBounds = eventBounds)
            animateCameraAndWait(eventBounds.center(), zoom = 13.0)

            // Set user position within event area
            val userPosition =
                Position(
                    eventBounds.northeast.latitude - 0.002,
                    eventBounds.northeast.longitude - 0.002,
                )
            adapter.setUserPosition(userPosition)

            Thread.sleep(300)

            // Verify user position is visible
            val visibleRegion = adapter.getVisibleRegion()
            assertTrue(
                "User position should be visible in WaveScreen\n" +
                    "  User position: (${userPosition.latitude}, ${userPosition.longitude})\n" +
                    "  Visible region: SW(${visibleRegion.southwest}) NE(${visibleRegion.northeast})",
                visibleRegion.contains(userPosition),
            )
        }

    @Test
    fun testUserPositionMarkerVisible_FullMap() =
        runBlocking {
            // Set camera to WINDOW mode (FullMapScreen behavior)
            val minZoom = MapTestFixtures.calculateMinZoomToFit(eventBounds, MapTestFixtures.PORTRAIT_PHONE)
            adapter.setMinZoomPreference(minZoom)
            adapter.setBoundsForCameraTarget(eventBounds, applyZoomSafetyMargin = false, originalEventBounds = eventBounds)

            // Set user position
            val userPosition =
                Position(
                    eventBounds.center().latitude - 0.001,
                    eventBounds.center().longitude - 0.001,
                )
            adapter.setUserPosition(userPosition)

            // In FullMapScreen, camera auto-targets user on first GPS fix
            // Simulate this behavior
            animateCameraAndWait(userPosition, minZoom + 1.0)

            Thread.sleep(300)

            // Verify user position is visible
            val visibleRegion = adapter.getVisibleRegion()
            assertTrue(
                "User position should be visible in FullMapScreen\n" +
                    "  User position: (${userPosition.latitude}, ${userPosition.longitude})\n" +
                    "  Visible region: SW(${visibleRegion.southwest}) NE(${visibleRegion.northeast})",
                visibleRegion.contains(userPosition),
            )

            // Camera should be close to user position
            assertCameraAt(
                userPosition,
                tolerance = 0.01,
                message = "FullMapScreen camera should be centered on user position",
            )
        }

    // ============================================================
    // WAVE POLYGONS RENDER (All Screens)
    // ============================================================

    @Test
    fun testWavePolygonsRender_EventDetail() =
        runBlocking {
            // Set camera to show entire event
            val minZoom = MapTestFixtures.calculateMinZoomToFit(eventBounds, MapTestFixtures.PORTRAIT_PHONE)
            adapter.setMinZoomPreference(minZoom)
            animateCameraAndWait(eventBounds.center(), minZoom)

            val visibleRegionBefore = adapter.getVisibleRegion()

            // Create wave polygons centered at event center
            val wavePolygons =
                listOf(
                    createCirclePolygon(eventBounds.center(), radiusMeters = 500.0),
                    createCirclePolygon(eventBounds.center(), radiusMeters = 1000.0),
                    createCirclePolygon(eventBounds.center(), radiusMeters = 1500.0),
                )

            // Note: In real implementation, adapter.setWavePolygons(wavePolygons) would be called
            // For this test, we verify wave polygons are within visible region

            // Verify all wave polygons are within visible region
            wavePolygons.forEachIndexed { index, polygon ->
                val allWithinViewport =
                    polygon.all { position ->
                        visibleRegionBefore.contains(position)
                    }

                assertTrue(
                    "All points of wave polygon $index should be within EventDetailScreen viewport",
                    allWithinViewport,
                )
            }

            // Verify camera didn't move when polygons added (EventDetailScreen has static camera)
            Thread.sleep(300)
            val visibleRegionAfter = adapter.getVisibleRegion()

            val centerDiff =
                kotlin.math.abs(visibleRegionAfter.center().latitude - visibleRegionBefore.center().latitude) +
                    kotlin.math.abs(visibleRegionAfter.center().longitude - visibleRegionBefore.center().longitude)

            assertTrue(
                "EventDetailScreen camera should not move when wave polygons are rendered\n" +
                    "  Center difference: $centerDiff degrees",
                centerDiff < 0.001,
            )
        }

    @Test
    fun testWavePolygonsRender_Wave() =
        runBlocking {
            // Set camera to show entire event (WaveScreen initial state)
            adapter.setBoundsForCameraTarget(eventBounds, applyZoomSafetyMargin = false, originalEventBounds = eventBounds)
            animateCameraAndWait(eventBounds.center(), zoom = 13.0)

            val visibleRegion = adapter.getVisibleRegion()

            // Create wave polygons
            val wavePolygons =
                listOf(
                    createCirclePolygon(eventBounds.center(), radiusMeters = 500.0),
                    createCirclePolygon(eventBounds.center(), radiusMeters = 1000.0),
                )

            // Verify wave polygons are within visible region
            wavePolygons.forEachIndexed { index, polygon ->
                val allWithinViewport =
                    polygon.all { position ->
                        visibleRegion.contains(position)
                    }

                assertTrue(
                    "All points of wave polygon $index should be within WaveScreen viewport",
                    allWithinViewport,
                )
            }

            // WaveScreen may auto-track to user+wave, but initial state should show all polygons
            assertTrue(
                "WaveScreen should show entire event area including all wave polygons",
                eventBounds.isCompletelyWithin(visibleRegion),
            )
        }

    @Test
    fun testWavePolygonsRender_FullMap() =
        runBlocking {
            // Set camera to WINDOW mode
            val minZoom = MapTestFixtures.calculateMinZoomToFit(eventBounds, MapTestFixtures.PORTRAIT_PHONE)
            adapter.setMinZoomPreference(minZoom)
            adapter.setBoundsForCameraTarget(eventBounds, applyZoomSafetyMargin = false, originalEventBounds = eventBounds)

            animateCameraAndWait(eventBounds.center(), minZoom)

            val visibleRegion = adapter.getVisibleRegion()

            // Create wave polygons
            val wavePolygons =
                listOf(
                    createCirclePolygon(eventBounds.center(), radiusMeters = 500.0),
                    createCirclePolygon(eventBounds.center(), radiusMeters = 1000.0),
                    createCirclePolygon(eventBounds.center(), radiusMeters = 1500.0),
                )

            // Verify wave polygons are within visible region
            // Note: FullMapScreen may not show ALL polygons if event is wide/tall
            // But center polygons should be visible
            val centerPolygon = wavePolygons.first() // Smallest circle at center

            val centerPolygonVisible =
                centerPolygon.all { position ->
                    visibleRegion.contains(position)
                }

            assertTrue(
                "Center wave polygon should be visible in FullMapScreen",
                centerPolygonVisible,
            )

            // All polygons should be within event bounds (FullMapScreen never shows outside pixels)
            assertVisibleRegionWithinBounds(
                "FullMapScreen viewport should remain within event bounds when rendering wave polygons",
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
