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
import com.worldwidewaves.shared.map.MapTestFixtures
import com.worldwidewaves.shared.map.MapTestFixtures.TALL_EVENT_BOUNDS
import com.worldwidewaves.shared.map.MapTestFixtures.WIDE_EVENT_BOUNDS
import com.worldwidewaves.shared.map.MapTestFixtures.aspectRatio
import com.worldwidewaves.shared.map.MapTestFixtures.center
import com.worldwidewaves.shared.map.MapTestFixtures.height
import com.worldwidewaves.shared.map.MapTestFixtures.width
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertTrue

/**
 * Integration tests for Full Map Screen behavior.
 *
 * Full Map Screen Configuration:
 * - initialCameraPosition = WINDOW (fits constraining dimension to screen)
 * - gesturesEnabled = true (full user control)
 * - autoTargetUserOnFirstLocation = true
 *
 * Expected Behavior:
 * - WINDOW mode: fits smallest dimension fully, prevents outside pixels
 * - Gestures enabled: user can pan/zoom within constraints
 * - Dynamic constraint bounds: change with zoom level
 * - Edge/corner sticking allowed
 * - Min zoom shows one full dimension (smallest)
 *
 * All tests use headless MapView (no UI rendering) for speed and reliability.
 */
@RunWith(AndroidJUnit4::class)
class FullMapScreenMapTest : BaseMapIntegrationTest() {
    // ============================================================
    // WINDOW MODE: ASPECT RATIO FITTING
    // ============================================================

    @Test
    fun testFullMap_wideEvent_fitsHeightDimension() =
        runBlocking {
            // Use wide event (aspect > screen aspect)
            eventBounds = WIDE_EVENT_BOUNDS
            val eventAspect = eventBounds.aspectRatio
            val screenAspect = MapTestFixtures.PORTRAIT_PHONE.aspectRatio

            assertTrue(
                eventAspect > screenAspect,
                "Test requires wide event (eventAspect=$eventAspect should be > screenAspect=$screenAspect)",
            )

            // Calculate min zoom for constraining dimension (HEIGHT for wide events)
            val constrainedWidth = eventBounds.height * screenAspect
            val aspectMatchedBounds =
                BoundingBox.fromCorners(
                    Position(
                        eventBounds.southwest.latitude,
                        eventBounds.center().longitude - constrainedWidth / 2.0,
                    ),
                    Position(
                        eventBounds.northeast.latitude,
                        eventBounds.center().longitude + constrainedWidth / 2.0,
                    ),
                )!!

            val minZoom = MapTestFixtures.calculateMinZoomToFit(aspectMatchedBounds, MapTestFixtures.PORTRAIT_PHONE)

            // Apply constraints
            adapter.setMinZoomPreference(minZoom)
            adapter.setBoundsForCameraTarget(eventBounds, applyZoomSafetyMargin = false, originalEventBounds = eventBounds)

            // Zoom to min zoom at event center
            animateCameraAndWait(eventBounds.center(), minZoom)

            val visibleRegion = adapter.getVisibleRegion()

            // Assertions - HEIGHT should fit completely
            val heightDifference = kotlin.math.abs(visibleRegion.height - eventBounds.height) / eventBounds.height

            assertTrue(
                heightDifference < MapTestFixtures.TOLERANCE_DIMENSION,
                "For wide event, HEIGHT (smallest dimension) should fit completely\n" +
                    "  Event height: ${eventBounds.height}\n" +
                    "  Viewport height: ${visibleRegion.height}\n" +
                    "  Difference: ${heightDifference * 100}%",
            )

            // WIDTH should be partially visible (NO outside pixels)
            assertVisibleRegionWithinBounds(
                "Viewport should not show pixels outside event bounds",
            )
        }

    @Test
    fun testFullMap_tallEvent_fitsWidthDimension() =
        runBlocking {
            // Use tall event (aspect < screen aspect)
            eventBounds = TALL_EVENT_BOUNDS
            val eventAspect = eventBounds.aspectRatio
            val screenAspect = MapTestFixtures.PORTRAIT_PHONE.aspectRatio

            assertTrue(
                eventAspect < screenAspect,
                "Test requires tall event (eventAspect=$eventAspect should be < screenAspect=$screenAspect)",
            )

            // Calculate min zoom for constraining dimension (WIDTH for tall events)
            val constrainedHeight = eventBounds.width / screenAspect
            val aspectMatchedBounds =
                BoundingBox.fromCorners(
                    Position(
                        eventBounds.center().latitude - constrainedHeight / 2.0,
                        eventBounds.southwest.longitude,
                    ),
                    Position(
                        eventBounds.center().latitude + constrainedHeight / 2.0,
                        eventBounds.northeast.longitude,
                    ),
                )!!

            val minZoom = MapTestFixtures.calculateMinZoomToFit(aspectMatchedBounds, MapTestFixtures.PORTRAIT_PHONE)

            // Apply constraints
            adapter.setMinZoomPreference(minZoom)
            adapter.setBoundsForCameraTarget(eventBounds, applyZoomSafetyMargin = false, originalEventBounds = eventBounds)

            // Zoom to min zoom at event center
            animateCameraAndWait(eventBounds.center(), minZoom)

            val visibleRegion = adapter.getVisibleRegion()

            // Assertions - WIDTH should fit completely
            val widthDifference = kotlin.math.abs(visibleRegion.width - eventBounds.width) / eventBounds.width

            assertTrue(
                widthDifference < MapTestFixtures.TOLERANCE_DIMENSION,
                "For tall event, WIDTH (smallest dimension) should fit completely\n" +
                    "  Event width: ${eventBounds.width}\n" +
                    "  Viewport width: ${visibleRegion.width}\n" +
                    "  Difference: ${widthDifference * 100}%",
            )

            // HEIGHT should be partially visible (NO outside pixels)
            assertVisibleRegionWithinBounds(
                "Viewport should not show pixels outside event bounds",
            )
        }

    // ============================================================
    // EDGE & CORNER STICKING
    // ============================================================

    @Test
    fun testFullMap_canStickToEdges() =
        runBlocking {
            val minZoom = MapTestFixtures.calculateMinZoomToFit(eventBounds, MapTestFixtures.PORTRAIT_PHONE)

            adapter.setMinZoomPreference(minZoom)
            adapter.setBoundsForCameraTarget(eventBounds, applyZoomSafetyMargin = false, originalEventBounds = eventBounds)

            // Test north edge
            val northEdgePos = Position(eventBounds.northeast.latitude - 0.001, eventBounds.center().longitude)
            animateCameraAndWait(northEdgePos, minZoom)

            val northRegion = adapter.getVisibleRegion()
            assertTrue(
                northRegion.northeast.latitude <= eventBounds.northeast.latitude + MapTestFixtures.TOLERANCE_EDGE,
                "Viewport north edge should align with event north edge",
            )
            assertVisibleRegionWithinBounds("North edge viewport should stay within bounds")

            // Test south edge
            val southEdgePos = Position(eventBounds.southwest.latitude + 0.001, eventBounds.center().longitude)
            animateCameraAndWait(southEdgePos, minZoom)

            val southRegion = adapter.getVisibleRegion()
            assertTrue(
                southRegion.southwest.latitude >= eventBounds.southwest.latitude - MapTestFixtures.TOLERANCE_EDGE,
                "Viewport south edge should align with event south edge",
            )
            assertVisibleRegionWithinBounds("South edge viewport should stay within bounds")

            // Test east edge
            val eastEdgePos = Position(eventBounds.center().latitude, eventBounds.northeast.longitude - 0.001)
            animateCameraAndWait(eastEdgePos, minZoom)

            val eastRegion = adapter.getVisibleRegion()
            assertTrue(
                eastRegion.northeast.longitude <= eventBounds.northeast.longitude + MapTestFixtures.TOLERANCE_EDGE,
                "Viewport east edge should align with event east edge",
            )
            assertVisibleRegionWithinBounds("East edge viewport should stay within bounds")

            // Test west edge
            val westEdgePos = Position(eventBounds.center().latitude, eventBounds.southwest.longitude + 0.001)
            animateCameraAndWait(westEdgePos, minZoom)

            val westRegion = adapter.getVisibleRegion()
            assertTrue(
                westRegion.southwest.longitude >= eventBounds.southwest.longitude - MapTestFixtures.TOLERANCE_EDGE,
                "Viewport west edge should align with event west edge",
            )
            assertVisibleRegionWithinBounds("West edge viewport should stay within bounds")
        }

    @Test
    fun testFullMap_canStickToCorners() =
        runBlocking {
            val minZoom = MapTestFixtures.calculateMinZoomToFit(eventBounds, MapTestFixtures.PORTRAIT_PHONE)

            adapter.setMinZoomPreference(minZoom)
            adapter.setBoundsForCameraTarget(eventBounds, applyZoomSafetyMargin = false, originalEventBounds = eventBounds)

            // Test NW corner
            val nwCornerPos =
                Position(
                    eventBounds.northeast.latitude - 0.001,
                    eventBounds.southwest.longitude + 0.001,
                )
            animateCameraAndWait(nwCornerPos, minZoom)

            assertVisibleRegionWithinBounds("NW corner viewport should stay within bounds")

            // Test NE corner
            val neCornerPos =
                Position(
                    eventBounds.northeast.latitude - 0.001,
                    eventBounds.northeast.longitude - 0.001,
                )
            animateCameraAndWait(neCornerPos, minZoom)

            assertVisibleRegionWithinBounds("NE corner viewport should stay within bounds")

            // Test SW corner
            val swCornerPos =
                Position(
                    eventBounds.southwest.latitude + 0.001,
                    eventBounds.southwest.longitude + 0.001,
                )
            animateCameraAndWait(swCornerPos, minZoom)

            assertVisibleRegionWithinBounds("SW corner viewport should stay within bounds")

            // Test SE corner
            val seCornerPos =
                Position(
                    eventBounds.southwest.latitude + 0.001,
                    eventBounds.northeast.longitude - 0.001,
                )
            animateCameraAndWait(seCornerPos, minZoom)

            assertVisibleRegionWithinBounds("SE corner viewport should stay within bounds")
        }

    // ============================================================
    // NO PIXELS OUTSIDE EVENT AREA
    // ============================================================

    @Test
    fun testFullMap_neverShowsPixelsOutsideEventArea() =
        runBlocking {
            val minZoom = MapTestFixtures.calculateMinZoomToFit(eventBounds, MapTestFixtures.PORTRAIT_PHONE)

            adapter.setMinZoomPreference(minZoom)
            adapter.setBoundsForCameraTarget(eventBounds, applyZoomSafetyMargin = false, originalEventBounds = eventBounds)

            // Test various camera positions at min zoom
            val testPositions =
                listOf(
                    eventBounds.center(),
                    Position(eventBounds.northeast.latitude - 0.001, eventBounds.center().longitude),
                    Position(eventBounds.southwest.latitude + 0.001, eventBounds.center().longitude),
                    Position(eventBounds.center().latitude, eventBounds.northeast.longitude - 0.001),
                    Position(eventBounds.center().latitude, eventBounds.southwest.longitude + 0.001),
                    Position(eventBounds.northeast.latitude - 0.001, eventBounds.northeast.longitude - 0.001),
                    Position(eventBounds.southwest.latitude + 0.001, eventBounds.southwest.longitude + 0.001),
                )

            testPositions.forEach { position ->
                animateCameraAndWait(position, minZoom)

                assertVisibleRegionWithinBounds(
                    "Viewport at position $position should never show pixels outside event area",
                )
            }
        }

    @Test
    fun testFullMap_minZoomShowsSmallestDimensionFully() =
        runBlocking {
            val minZoom = MapTestFixtures.calculateMinZoomToFit(eventBounds, MapTestFixtures.PORTRAIT_PHONE)

            adapter.setMinZoomPreference(minZoom)
            adapter.setBoundsForCameraTarget(eventBounds, applyZoomSafetyMargin = false, originalEventBounds = eventBounds)

            animateCameraAndWait(eventBounds.center(), minZoom)

            val visibleRegion = adapter.getVisibleRegion()

            val eventAspect = eventBounds.aspectRatio
            val screenAspect = MapTestFixtures.PORTRAIT_PHONE.aspectRatio

            if (eventAspect > screenAspect) {
                // Wide event - HEIGHT is smallest dimension
                val heightDifference = kotlin.math.abs(visibleRegion.height - eventBounds.height) / eventBounds.height

                assertTrue(
                    heightDifference < MapTestFixtures.TOLERANCE_DIMENSION,
                    "For wide event, HEIGHT (smallest dimension) should be fully visible at min zoom\n" +
                        "  Event height: ${eventBounds.height}\n" +
                        "  Viewport height: ${visibleRegion.height}\n" +
                        "  Difference: ${heightDifference * 100}%",
                )
            } else {
                // Tall event - WIDTH is smallest dimension
                val widthDifference = kotlin.math.abs(visibleRegion.width - eventBounds.width) / eventBounds.width

                assertTrue(
                    widthDifference < MapTestFixtures.TOLERANCE_DIMENSION,
                    "For tall event, WIDTH (smallest dimension) should be fully visible at min zoom\n" +
                        "  Event width: ${eventBounds.width}\n" +
                        "  Viewport width: ${visibleRegion.width}\n" +
                        "  Difference: ${widthDifference * 100}%",
                )
            }

            assertVisibleRegionWithinBounds(
                "Viewport should not show pixels outside event bounds at min zoom",
            )
        }

    // ============================================================
    // ZOOM CONSTRAINTS
    // ============================================================

    @Test
    fun testFullMap_cannotZoomOutBeyondMinZoom() =
        runBlocking {
            val minZoom = MapTestFixtures.calculateMinZoomToFit(eventBounds, MapTestFixtures.PORTRAIT_PHONE)

            adapter.setMinZoomPreference(minZoom)

            // Try to zoom out beyond min zoom
            animateCameraAndWait(eventBounds.center(), minZoom - 2.0)

            val actualZoom = adapter.currentZoom.value

            assertTrue(
                actualZoom >= minZoom - MapTestFixtures.TOLERANCE_ZOOM,
                "Zoom should not go below min zoom\n" +
                    "  Min zoom: $minZoom\n" +
                    "  Attempted: ${minZoom - 2.0}\n" +
                    "  Actual: $actualZoom",
            )

            assertVisibleRegionWithinBounds(
                "Viewport should remain within bounds even when trying to zoom out beyond min",
            )
        }

    @Test
    fun testFullMap_canZoomInAboveMinZoom() =
        runBlocking {
            val minZoom = MapTestFixtures.calculateMinZoomToFit(eventBounds, MapTestFixtures.PORTRAIT_PHONE)

            adapter.setMinZoomPreference(minZoom)
            adapter.setMaxZoomPreference(18.0)

            // Zoom in above min zoom
            val targetZoom = minZoom + 3.0
            animateCameraAndWait(eventBounds.center(), targetZoom)

            val actualZoom = adapter.currentZoom.value

            assertZoomLevel(
                targetZoom,
                message = "Should be able to zoom in above min zoom",
            )

            assertVisibleRegionWithinBounds(
                "Viewport should remain within bounds when zoomed in",
            )
        }

    // ============================================================
    // PREVENTIVE CONSTRAINT ENFORCEMENT
    // ============================================================

    @Test
    fun testFullMap_preventiveConstraints_neverShowOutsidePixels() =
        runBlocking {
            // Apply preventive constraints
            applyConstraintsAndVerify(eventBounds, isWindowMode = true)

            val minZoom = adapter.getMinZoomLevel()

            // Try to move camera outside event bounds (should be clamped)
            val outsidePosition =
                Position(
                    eventBounds.northeast.latitude + 0.01, // Way outside
                    eventBounds.northeast.longitude + 0.01,
                )

            animateCameraAndWait(outsidePosition, minZoom)

            val actualPosition = adapter.getCameraPosition()
            val visibleRegion = adapter.getVisibleRegion()

            // Camera should be clamped to keep viewport within bounds
            assertVisibleRegionWithinBounds(
                "Preventive constraints should prevent camera from showing outside pixels",
            )

            // Camera should NOT be at the requested outside position
            assertTrue(
                actualPosition != null && actualPosition.latitude < outsidePosition.latitude,
                "Camera latitude should be clamped inward from outside request",
            )

            assertTrue(
                actualPosition != null && actualPosition.longitude < outsidePosition.longitude,
                "Camera longitude should be clamped inward from outside request",
            )
        }
}
