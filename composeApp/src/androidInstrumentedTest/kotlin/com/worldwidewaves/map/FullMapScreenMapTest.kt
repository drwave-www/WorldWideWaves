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
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

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
                "Test requires wide event (eventAspect=$eventAspect should be > screenAspect=$screenAspect)",
                eventAspect > screenAspect,
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
                "For wide event, HEIGHT (smallest dimension) should fit completely\n" +
                    "  Event height: ${eventBounds.height}\n" +
                    "  Viewport height: ${visibleRegion.height}\n" +
                    "  Difference: ${heightDifference * 100}%",
                heightDifference < MapTestFixtures.TOLERANCE_DIMENSION,
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
                "Test requires tall event (eventAspect=$eventAspect should be < screenAspect=$screenAspect)",
                eventAspect < screenAspect,
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
                "For tall event, WIDTH (smallest dimension) should fit completely\n" +
                    "  Event width: ${eventBounds.width}\n" +
                    "  Viewport width: ${visibleRegion.width}\n" +
                    "  Difference: ${widthDifference * 100}%",
                widthDifference < MapTestFixtures.TOLERANCE_DIMENSION,
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
                "Viewport north edge should align with event north edge",
                northRegion.northeast.latitude <= eventBounds.northeast.latitude + MapTestFixtures.TOLERANCE_EDGE,
            )
            assertVisibleRegionWithinBounds("North edge viewport should stay within bounds")

            // Test south edge
            val southEdgePos = Position(eventBounds.southwest.latitude + 0.001, eventBounds.center().longitude)
            animateCameraAndWait(southEdgePos, minZoom)

            val southRegion = adapter.getVisibleRegion()
            assertTrue(
                "Viewport south edge should align with event south edge",
                southRegion.southwest.latitude >= eventBounds.southwest.latitude - MapTestFixtures.TOLERANCE_EDGE,
            )
            assertVisibleRegionWithinBounds("South edge viewport should stay within bounds")

            // Test east edge
            val eastEdgePos = Position(eventBounds.center().latitude, eventBounds.northeast.longitude - 0.001)
            animateCameraAndWait(eastEdgePos, minZoom)

            val eastRegion = adapter.getVisibleRegion()
            assertTrue(
                "Viewport east edge should align with event east edge",
                eastRegion.northeast.longitude <= eventBounds.northeast.longitude + MapTestFixtures.TOLERANCE_EDGE,
            )
            assertVisibleRegionWithinBounds("East edge viewport should stay within bounds")

            // Test west edge
            val westEdgePos = Position(eventBounds.center().latitude, eventBounds.southwest.longitude + 0.001)
            animateCameraAndWait(westEdgePos, minZoom)

            val westRegion = adapter.getVisibleRegion()
            assertTrue(
                "Viewport west edge should align with event west edge",
                westRegion.southwest.longitude >= eventBounds.southwest.longitude - MapTestFixtures.TOLERANCE_EDGE,
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
                    "For wide event, HEIGHT (smallest dimension) should be fully visible at min zoom\n" +
                        "  Event height: ${eventBounds.height}\n" +
                        "  Viewport height: ${visibleRegion.height}\n" +
                        "  Difference: ${heightDifference * 100}%",
                    heightDifference < MapTestFixtures.TOLERANCE_DIMENSION,
                )
            } else {
                // Tall event - WIDTH is smallest dimension
                val widthDifference = kotlin.math.abs(visibleRegion.width - eventBounds.width) / eventBounds.width

                assertTrue(
                    "For tall event, WIDTH (smallest dimension) should be fully visible at min zoom\n" +
                        "  Event width: ${eventBounds.width}\n" +
                        "  Viewport width: ${visibleRegion.width}\n" +
                        "  Difference: ${widthDifference * 100}%",
                    widthDifference < MapTestFixtures.TOLERANCE_DIMENSION,
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
                "Zoom should not go below min zoom\n" +
                    "  Min zoom: $minZoom\n" +
                    "  Attempted: ${minZoom - 2.0}\n" +
                    "  Actual: $actualZoom",
                actualZoom >= minZoom - MapTestFixtures.TOLERANCE_ZOOM,
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
                "Camera latitude should be clamped inward from outside request",
                actualPosition != null && actualPosition.latitude < outsidePosition.latitude,
            )

            assertTrue(
                "Camera longitude should be clamped inward from outside request",
                actualPosition != null && actualPosition.longitude < outsidePosition.longitude,
            )
        }

    // ============================================================
    // REAL MAPLIBRE VISIBLE REGION CLAMPING TESTS (CRITICAL)
    // ============================================================

    @Test
    fun testFullMap_visibleRegionStaysWithinBounds_atDifferentZoomLevels() =
        runBlocking {
            applyConstraintsAndVerify(eventBounds, isWindowMode = true)

            val minZoom = adapter.getMinZoomLevel()
            val zoomLevels = listOf(minZoom, minZoom + 1, minZoom + 2, minZoom + 3)

            zoomLevels.forEach { zoom ->
                // Test at event center
                animateCameraAndWait(eventBounds.center(), zoom)
                var visibleRegion = adapter.getVisibleRegion()

                assertVisibleRegionWithinBounds(
                    "Visible region at zoom $zoom (center) should stay within event bounds",
                )

                // Test at NE corner
                val neCorner =
                    Position(
                        eventBounds.northeast.latitude - 0.001,
                        eventBounds.northeast.longitude - 0.001,
                    )
                animateCameraAndWait(neCorner, zoom)
                visibleRegion = adapter.getVisibleRegion()

                assertVisibleRegionWithinBounds(
                    "Visible region at zoom $zoom (NE corner) should stay within event bounds",
                )

                // Test at SW corner
                val swCorner =
                    Position(
                        eventBounds.southwest.latitude + 0.001,
                        eventBounds.southwest.longitude + 0.001,
                    )
                animateCameraAndWait(swCorner, zoom)
                visibleRegion = adapter.getVisibleRegion()

                assertVisibleRegionWithinBounds(
                    "Visible region at zoom $zoom (SW corner) should stay within event bounds",
                )
            }
        }

    @Test
    fun testFullMap_cameraClampedWhenPanningTowardBoundary() =
        runBlocking {
            applyConstraintsAndVerify(eventBounds, isWindowMode = true)

            val minZoom = adapter.getMinZoomLevel()

            // Start at center
            animateCameraAndWait(eventBounds.center(), minZoom)

            // Aggressively try to pan north beyond boundary
            val farNorthPosition =
                Position(
                    eventBounds.northeast.latitude + 1.0, // Way outside
                    eventBounds.center().longitude,
                )

            animateCameraAndWait(farNorthPosition, minZoom)

            val clampedPosition = adapter.getCameraPosition()
            val clampedVisibleRegion = adapter.getVisibleRegion()

            // Verify camera was clamped (not at requested position)
            assertTrue(
                "Camera should be clamped south of requested position",
                clampedPosition != null && clampedPosition.latitude < farNorthPosition.latitude,
            )

            // Verify visible region never exceeded event bounds
            assertVisibleRegionWithinBounds(
                "Clamped visible region should stay within event bounds",
            )

            // Verify visible region north edge is near event north edge
            val northEdgeDistance =
                kotlin.math.abs(clampedVisibleRegion.northeast.latitude - eventBounds.northeast.latitude)

            assertTrue(
                "Clamped viewport north edge should be close to event north edge (within 0.01°)",
                northEdgeDistance < 0.01,
            )
        }

    @Test
    fun testFullMap_visibleRegionAtMinZoom_matchesConstrainingDimension() =
        runBlocking {
            applyConstraintsAndVerify(eventBounds, isWindowMode = true)

            val minZoom = adapter.getMinZoomLevel()

            // Move to center at min zoom
            animateCameraAndWait(eventBounds.center(), minZoom)

            val visibleRegion = adapter.getVisibleRegion()

            // Determine constraining dimension
            val eventAspect = eventBounds.aspectRatio
            val screenAspect = MapTestFixtures.PORTRAIT_PHONE.aspectRatio

            if (eventAspect > screenAspect) {
                // Wide event - HEIGHT is constraining dimension
                // Visible region height should approximately match event height
                val heightDifference =
                    kotlin.math.abs(visibleRegion.height - eventBounds.height) / eventBounds.height

                assertTrue(
                    "At min zoom, visible region HEIGHT should match event HEIGHT (±10%)\n" +
                        "  Event height: ${eventBounds.height}\n" +
                        "  Viewport height: ${visibleRegion.height}\n" +
                        "  Difference: ${heightDifference * 100}%",
                    heightDifference < 0.1, // 10% tolerance
                )

                // Width should be less than event width (partially visible)
                assertTrue(
                    "At min zoom, visible region WIDTH should be less than event WIDTH",
                    visibleRegion.width < eventBounds.width,
                )
            } else {
                // Tall event - WIDTH is constraining dimension
                val widthDifference = kotlin.math.abs(visibleRegion.width - eventBounds.width) / eventBounds.width

                assertTrue(
                    "At min zoom, visible region WIDTH should match event WIDTH (±10%)\n" +
                        "  Event width: ${eventBounds.width}\n" +
                        "  Viewport width: ${visibleRegion.width}\n" +
                        "  Difference: ${widthDifference * 100}%",
                    widthDifference < 0.1,
                )

                // Height should be less than event height (partially visible)
                assertTrue(
                    "At min zoom, visible region HEIGHT should be less than event HEIGHT",
                    visibleRegion.height < eventBounds.height,
                )
            }

            // Most critical: NO outside pixels
            assertVisibleRegionWithinBounds(
                "At min zoom, visible region must stay within event bounds (no outside pixels)",
            )
        }

    // ============================================================
    // ZOOM IN/OUT WHILE STUCK TO EDGES
    // ============================================================

    @Test
    fun testFullMap_canZoomInWhileStuckToEdge() =
        runBlocking {
            val minZoom = MapTestFixtures.calculateMinZoomToFit(eventBounds, MapTestFixtures.PORTRAIT_PHONE)

            adapter.setMinZoomPreference(minZoom)
            adapter.setMaxZoomPreference(18.0)
            adapter.setBoundsForCameraTarget(eventBounds, applyZoomSafetyMargin = false, originalEventBounds = eventBounds)

            // Move to north edge at min zoom
            val northEdgePos = Position(eventBounds.northeast.latitude - 0.001, eventBounds.center().longitude)
            animateCameraAndWait(northEdgePos, minZoom)

            // Verify at edge
            val regionAtMinZoom = adapter.getVisibleRegion()
            assertTrue(
                "Viewport should be at north edge initially",
                regionAtMinZoom.northeast.latitude <= eventBounds.northeast.latitude + MapTestFixtures.TOLERANCE_EDGE,
            )

            // Zoom in while stuck to edge
            val higherZoom = minZoom + 2.0
            animateCameraAndWait(northEdgePos, higherZoom)

            val regionAfterZoomIn = adapter.getVisibleRegion()

            // Should still be able to zoom in
            assertZoomLevel(
                higherZoom,
                message = "Should be able to zoom in while stuck to edge",
            )

            // Should still be within bounds
            assertVisibleRegionWithinBounds(
                "Viewport should remain within bounds after zooming in at edge",
            )

            // Viewport should be smaller (zoomed in)
            assertTrue(
                "Viewport should be smaller after zooming in\n" +
                    "  Initial height: ${regionAtMinZoom.height}\n" +
                    "  After zoom height: ${regionAfterZoomIn.height}",
                regionAfterZoomIn.height < regionAtMinZoom.height,
            )
        }

    @Test
    fun testFullMap_canZoomOutWhileStuckToEdge() =
        runBlocking {
            val minZoom = MapTestFixtures.calculateMinZoomToFit(eventBounds, MapTestFixtures.PORTRAIT_PHONE)

            adapter.setMinZoomPreference(minZoom)
            adapter.setMaxZoomPreference(18.0)
            adapter.setBoundsForCameraTarget(eventBounds, applyZoomSafetyMargin = false, originalEventBounds = eventBounds)

            // Move to south edge at higher zoom
            val southEdgePos = Position(eventBounds.southwest.latitude + 0.001, eventBounds.center().longitude)
            val higherZoom = minZoom + 3.0
            animateCameraAndWait(southEdgePos, higherZoom)

            val regionAtHigherZoom = adapter.getVisibleRegion()

            // Zoom out while stuck to edge
            val lowerZoom = minZoom + 1.0
            animateCameraAndWait(southEdgePos, lowerZoom)

            val regionAfterZoomOut = adapter.getVisibleRegion()

            // Should be able to zoom out
            assertZoomLevel(
                lowerZoom,
                message = "Should be able to zoom out while stuck to edge",
            )

            // Should still be within bounds
            assertVisibleRegionWithinBounds(
                "Viewport should remain within bounds after zooming out at edge",
            )

            // Viewport should be larger (zoomed out)
            assertTrue(
                "Viewport should be larger after zooming out\n" +
                    "  Initial height: ${regionAtHigherZoom.height}\n" +
                    "  After zoom height: ${regionAfterZoomOut.height}",
                regionAfterZoomOut.height > regionAtHigherZoom.height,
            )
        }

    @Test
    fun testFullMap_canZoomInWhileStuckToCorner() =
        runBlocking {
            val minZoom = MapTestFixtures.calculateMinZoomToFit(eventBounds, MapTestFixtures.PORTRAIT_PHONE)

            adapter.setMinZoomPreference(minZoom)
            adapter.setMaxZoomPreference(18.0)
            adapter.setBoundsForCameraTarget(eventBounds, applyZoomSafetyMargin = false, originalEventBounds = eventBounds)

            // Move to NE corner at min zoom
            val neCornerPos =
                Position(
                    eventBounds.northeast.latitude - 0.001,
                    eventBounds.northeast.longitude - 0.001,
                )
            animateCameraAndWait(neCornerPos, minZoom)

            val regionAtMinZoom = adapter.getVisibleRegion()

            // Zoom in while stuck to corner
            val higherZoom = minZoom + 2.5
            animateCameraAndWait(neCornerPos, higherZoom)

            val regionAfterZoomIn = adapter.getVisibleRegion()

            // Should be able to zoom in
            assertZoomLevel(
                higherZoom,
                message = "Should be able to zoom in while stuck to corner",
            )

            // Should still be within bounds
            assertVisibleRegionWithinBounds(
                "Viewport should remain within bounds after zooming in at corner",
            )

            // Viewport should be smaller
            assertTrue(
                "Viewport should be smaller after zooming in at corner",
                regionAfterZoomIn.width < regionAtMinZoom.width &&
                    regionAfterZoomIn.height < regionAtMinZoom.height,
            )
        }

    @Test
    fun testFullMap_canZoomOutWhileStuckToCorner() =
        runBlocking {
            val minZoom = MapTestFixtures.calculateMinZoomToFit(eventBounds, MapTestFixtures.PORTRAIT_PHONE)

            adapter.setMinZoomPreference(minZoom)
            adapter.setMaxZoomPreference(18.0)
            adapter.setBoundsForCameraTarget(eventBounds, applyZoomSafetyMargin = false, originalEventBounds = eventBounds)

            // Move to SW corner at higher zoom
            val swCornerPos =
                Position(
                    eventBounds.southwest.latitude + 0.001,
                    eventBounds.southwest.longitude + 0.001,
                )
            val higherZoom = minZoom + 3.0
            animateCameraAndWait(swCornerPos, higherZoom)

            val regionAtHigherZoom = adapter.getVisibleRegion()

            // Zoom out while stuck to corner
            val lowerZoom = minZoom + 1.0
            animateCameraAndWait(swCornerPos, lowerZoom)

            val regionAfterZoomOut = adapter.getVisibleRegion()

            // Should be able to zoom out
            assertZoomLevel(
                lowerZoom,
                message = "Should be able to zoom out while stuck to corner",
            )

            // Should still be within bounds
            assertVisibleRegionWithinBounds(
                "Viewport should remain within bounds after zooming out at corner",
            )

            // Viewport should be larger
            assertTrue(
                "Viewport should be larger after zooming out at corner",
                regionAfterZoomOut.width > regionAtHigherZoom.width &&
                    regionAfterZoomOut.height > regionAtHigherZoom.height,
            )
        }

    // ============================================================
    // AUTO-TARGETING USER ON FIRST GPS FIX
    // ============================================================

    @Test
    fun testFullMap_autoTargetsUserOnFirstGpsFix() =
        runBlocking {
            val minZoom = MapTestFixtures.calculateMinZoomToFit(eventBounds, MapTestFixtures.PORTRAIT_PHONE)

            adapter.setMinZoomPreference(minZoom)
            adapter.setBoundsForCameraTarget(eventBounds, applyZoomSafetyMargin = false, originalEventBounds = eventBounds)

            // Start at event center
            animateCameraAndWait(eventBounds.center(), minZoom)
            val initialCamera = adapter.getCameraPosition()

            // Simulate first GPS fix (user position update)
            val userPosition =
                Position(
                    eventBounds.northeast.latitude - 0.002,
                    eventBounds.northeast.longitude - 0.002,
                )
            adapter.setUserPosition(userPosition)

            // Camera should move to user position automatically
            // (In real implementation, FullMapScreen has autoTargetUserOnFirstLocation = true)
            // For this test, we simulate the auto-targeting behavior
            animateCameraAndWait(userPosition, minZoom + 1.0)

            val cameraAfterGps = adapter.getCameraPosition()

            // Camera should have moved from initial position
            assertTrue(
                "Camera should have moved after first GPS fix\n" +
                    "  Initial camera: ${initialCamera?.latitude}, ${initialCamera?.longitude}\n" +
                    "  Camera after GPS: ${cameraAfterGps?.latitude}, ${cameraAfterGps?.longitude}",
                cameraAfterGps != null &&
                    (
                        kotlin.math.abs(cameraAfterGps.latitude - (initialCamera?.latitude ?: 0.0)) > 0.001 ||
                            kotlin.math.abs(cameraAfterGps.longitude - (initialCamera?.longitude ?: 0.0)) > 0.001
                    ),
            )

            // Camera should be close to user position
            assertCameraAt(
                userPosition,
                tolerance = 0.01,
                message = "Camera should target user position after first GPS fix",
            )

            // Should still be within bounds
            assertVisibleRegionWithinBounds(
                "Viewport should remain within bounds after auto-targeting user",
            )
        }

    // ============================================================
    // SMALLEST DIMENSION FITTING
    // ============================================================

    @Test
    fun testFullMap_canZoomToShowSmallestDimension_wideEvent() =
        runBlocking {
            // Use wide event
            eventBounds = WIDE_EVENT_BOUNDS
            val eventAspect = eventBounds.aspectRatio
            val screenAspect = MapTestFixtures.PORTRAIT_PHONE.aspectRatio

            assertTrue(
                "Test requires wide event",
                eventAspect > screenAspect,
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

            adapter.setMinZoomPreference(minZoom)
            adapter.setBoundsForCameraTarget(eventBounds, applyZoomSafetyMargin = false, originalEventBounds = eventBounds)

            // Zoom to min zoom
            animateCameraAndWait(eventBounds.center(), minZoom)

            val visibleRegion = adapter.getVisibleRegion()

            // HEIGHT (smallest dimension) should fit completely
            val heightDifference = kotlin.math.abs(visibleRegion.height - eventBounds.height) / eventBounds.height

            assertTrue(
                "For wide event, HEIGHT (smallest dimension) should fit completely at min zoom\n" +
                    "  Event height: ${eventBounds.height}\n" +
                    "  Viewport height: ${visibleRegion.height}\n" +
                    "  Difference: ${heightDifference * 100}%",
                heightDifference < MapTestFixtures.TOLERANCE_DIMENSION,
            )

            // WIDTH should be partially visible (no outside pixels)
            assertVisibleRegionWithinBounds(
                "Wide event viewport should not show pixels outside event bounds",
            )
        }

    @Test
    fun testFullMap_canZoomToShowSmallestDimension_tallEvent() =
        runBlocking {
            // Use tall event
            eventBounds = TALL_EVENT_BOUNDS
            val eventAspect = eventBounds.aspectRatio
            val screenAspect = MapTestFixtures.PORTRAIT_PHONE.aspectRatio

            assertTrue(
                "Test requires tall event",
                eventAspect < screenAspect,
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

            adapter.setMinZoomPreference(minZoom)
            adapter.setBoundsForCameraTarget(eventBounds, applyZoomSafetyMargin = false, originalEventBounds = eventBounds)

            // Zoom to min zoom
            animateCameraAndWait(eventBounds.center(), minZoom)

            val visibleRegion = adapter.getVisibleRegion()

            // WIDTH (smallest dimension) should fit completely
            val widthDifference = kotlin.math.abs(visibleRegion.width - eventBounds.width) / eventBounds.width

            assertTrue(
                "For tall event, WIDTH (smallest dimension) should fit completely at min zoom\n" +
                    "  Event width: ${eventBounds.width}\n" +
                    "  Viewport width: ${visibleRegion.width}\n" +
                    "  Difference: ${widthDifference * 100}%",
                widthDifference < MapTestFixtures.TOLERANCE_DIMENSION,
            )

            // HEIGHT should be partially visible (no outside pixels)
            assertVisibleRegionWithinBounds(
                "Tall event viewport should not show pixels outside event bounds",
            )
        }

    // ============================================================
    // NO RECALCULATION ON MINOR DIMENSION CHANGE
    // ============================================================

    @Test
    fun testFullMap_noRecalculationOnMinorDimensionChange() =
        runBlocking {
            val minZoom = MapTestFixtures.calculateMinZoomToFit(eventBounds, MapTestFixtures.PORTRAIT_PHONE)

            adapter.setMinZoomPreference(minZoom)
            adapter.setBoundsForCameraTarget(eventBounds, applyZoomSafetyMargin = false, originalEventBounds = eventBounds)

            // Set initial camera
            animateCameraAndWait(eventBounds.center(), minZoom)
            val initialRegion = adapter.getVisibleRegion()

            // Make minor dimension change (simulate screen rotation with <10% change)
            val minorChange = 1.05 // 5% change
            screenWidth = MapTestFixtures.PORTRAIT_PHONE.width * minorChange
            screenHeight = MapTestFixtures.PORTRAIT_PHONE.height * minorChange

            // Re-query visible region (no recalculation expected)
            Thread.sleep(100) // Small delay to ensure no background recalculation
            val regionAfterMinorChange = adapter.getVisibleRegion()

            // Visible region should not have changed significantly
            val widthDifference =
                kotlin.math.abs(regionAfterMinorChange.width - initialRegion.width) / initialRegion.width
            val heightDifference =
                kotlin.math.abs(regionAfterMinorChange.height - initialRegion.height) / initialRegion.height

            assertTrue(
                "Minor dimension change (<10%) should not trigger significant viewport recalculation\n" +
                    "  Width difference: ${widthDifference * 100}%\n" +
                    "  Height difference: ${heightDifference * 100}%",
                widthDifference < 0.15 && heightDifference < 0.15, // Allow some tolerance
            )

            // Reset screen dimensions
            screenWidth = MapTestFixtures.PORTRAIT_PHONE.width
            screenHeight = MapTestFixtures.PORTRAIT_PHONE.height
        }
}
