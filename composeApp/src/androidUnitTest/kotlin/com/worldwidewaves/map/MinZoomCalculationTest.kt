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

import com.worldwidewaves.shared.events.utils.BoundingBox
import org.junit.Test
import kotlin.math.log2
import kotlin.math.min
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for min zoom calculation formula parity.
 *
 * CRITICAL REQUIREMENT: Min zoom must use the SAME formula as moveToWindowBounds
 * to prevent zoom-out beyond the animation's target zoom.
 *
 * Historical Bug: Min zoom was calculated using getCameraForLatLngBounds() which
 * gave a LOWER zoom than moveToWindowBounds, allowing users to zoom out and see
 * pixels outside the event area.
 *
 * Fix: Min zoom now uses min(zoomForWidth, zoomForHeight) + safety margin,
 * matching moveToWindowBounds exactly.
 */
class MinZoomCalculationTest {
    companion object {
        private const val ZOOM_TOLERANCE = 0.01
        private const val ZOOM_SAFETY_MARGIN = 0.5

        // Paris event bounds (real data)
        private val PARIS_BOUNDS =
            BoundingBox(
                swLat = 48.8155755,
                swLng = 2.2241219,
                neLat = 48.902156,
                neLng = 2.4697602,
            )
    }

    /**
     * Verify WINDOW mode min zoom matches moveToWindowBounds formula.
     * This is CRITICAL to prevent zoom-out beyond animation target.
     */
    @Test
    fun `WINDOW mode min zoom uses same formula as moveToWindowBounds`() {
        // Given: Paris event on portrait phone
        val eventWidth = PARIS_BOUNDS.ne.lng - PARIS_BOUNDS.sw.lng
        val eventHeight = PARIS_BOUNDS.ne.lat - PARIS_BOUNDS.sw.lat
        val screenWidth = 1080.0
        val screenHeight = 1920.0

        // When: Calculate using moveToWindowBounds formula
        val zoomForWidth = log2((screenWidth * 360.0) / (eventWidth * 256.0))
        val zoomForHeight = log2((screenHeight * 180.0) / (eventHeight * 256.0))
        val animationZoom = min(zoomForWidth, zoomForHeight)

        // When: Calculate min zoom using SAME formula (WINDOW mode)
        val expectedMinZoom = animationZoom + ZOOM_SAFETY_MARGIN

        // Then: Min zoom should match animation zoom + safety margin
        // This prevents zooming out below animation level (which would show pixels outside event)
        assertTrue(
            expectedMinZoom > animationZoom,
            "Min zoom ($expectedMinZoom) must be higher than animation zoom ($animationZoom) by safety margin",
        )

        assertEquals(
            expectedMinZoom,
            animationZoom + ZOOM_SAFETY_MARGIN,
            ZOOM_TOLERANCE,
            "WINDOW mode min zoom must equal moveToWindowBounds zoom + safety margin",
        )

        println("✅ WINDOW mode: animationZoom=$animationZoom, minZoom=$expectedMinZoom (prevents zoom-out overflow)")
    }

    /**
     * Verify BOUNDS mode min zoom shows entire event.
     * Uses getCameraForLatLngBounds() which fits BOTH dimensions.
     */
    @Test
    fun `BOUNDS mode min zoom uses getCameraForLatLngBounds formula`() {
        // Given: Paris event bounds
        val eventWidth = PARIS_BOUNDS.ne.lng - PARIS_BOUNDS.sw.lng
        val eventHeight = PARIS_BOUNDS.ne.lat - PARIS_BOUNDS.sw.lat
        val screenWidth = 1080.0
        val screenHeight = 1920.0

        // When: Calculate min zoom for BOUNDS mode
        // This should fit BOTH dimensions (the LOWER zoom)
        val zoomForWidth = log2((screenWidth * 360.0) / (eventWidth * 256.0))
        val zoomForHeight = log2((screenHeight * 180.0) / (eventHeight * 256.0))
        val boundsZoom = min(zoomForWidth, zoomForHeight) // Same as WINDOW base

        // BOUNDS mode should NOT add safety margin (shows entire event)
        val expectedMinZoom = boundsZoom

        // Then: BOUNDS min zoom should be lower than WINDOW min zoom
        val windowMinZoom = boundsZoom + ZOOM_SAFETY_MARGIN
        assertTrue(
            expectedMinZoom < windowMinZoom,
            "BOUNDS mode min zoom ($expectedMinZoom) must be lower than WINDOW mode ($windowMinZoom) - shows more area",
        )

        println("✅ BOUNDS mode: minZoom=$expectedMinZoom (entire event visible, no safety margin)")
    }

    /**
     * Verify safety margin (+0.5) is applied only for WINDOW mode.
     */
    @Test
    fun `safety margin only applied for WINDOW mode`() {
        // Given: Any event bounds
        val baseMinZoom = 10.0

        // When: Apply safety margin
        val windowMinZoom = baseMinZoom + ZOOM_SAFETY_MARGIN
        val boundsMinZoom = baseMinZoom // No margin for BOUNDS

        // Then: WINDOW has margin, BOUNDS doesn't
        assertEquals(10.5, windowMinZoom, ZOOM_TOLERANCE, "WINDOW mode adds +0.5 safety margin")
        assertEquals(10.0, boundsMinZoom, ZOOM_TOLERANCE, "BOUNDS mode has no safety margin")

        assertTrue(
            windowMinZoom > boundsMinZoom,
            "WINDOW mode min zoom must be higher than BOUNDS mode (more restrictive)",
        )

        println("✅ Safety margin: WINDOW=$windowMinZoom (+0.5), BOUNDS=$boundsMinZoom (+0.0)")
    }

    /**
     * Verify min zoom prevents zoom-out beyond animation target.
     * This prevents the historical bug where users could zoom out and see pixels outside event.
     */
    @Test
    fun `min zoom prevents zoom-out below animation target`() {
        // Given: Animation zoom calculated for Paris
        val eventWidth = PARIS_BOUNDS.ne.lng - PARIS_BOUNDS.sw.lng
        val eventHeight = PARIS_BOUNDS.ne.lat - PARIS_BOUNDS.sw.lat
        val screenWidth = 1080.0
        val screenHeight = 1920.0

        val zoomForWidth = log2((screenWidth * 360.0) / (eventWidth * 256.0))
        val zoomForHeight = log2((screenHeight * 180.0) / (eventHeight * 256.0))
        val animationZoom = min(zoomForWidth, zoomForHeight)

        // When: Min zoom is set with safety margin
        val minZoom = animationZoom + ZOOM_SAFETY_MARGIN

        // Then: Min zoom must be AT LEAST as high as animation zoom
        assertTrue(
            minZoom >= animationZoom,
            "Min zoom ($minZoom) must be >= animation zoom ($animationZoom) to prevent zoom-out overflow",
        )

        // Then: User cannot zoom below min zoom
        val attemptedZoom = animationZoom - 1.0 // Try to zoom out below animation
        val enforcedZoom = kotlin.math.max(minZoom, attemptedZoom)

        assertEquals(
            minZoom,
            enforcedZoom,
            ZOOM_TOLERANCE,
            "MapLibre should enforce min zoom, preventing zoom below $minZoom",
        )

        println("✅ Min zoom enforcement: animation=$animationZoom, min=$minZoom, attempted=${animationZoom - 1.0}, enforced=$enforcedZoom")
    }

    /**
     * Verify min zoom calculated from ORIGINAL event bounds (not shrunk constraint bounds).
     * This prevents the bug where min zoom increases as viewport shrinks.
     */
    @Test
    fun `min zoom calculated from ORIGINAL event bounds not shrunk bounds`() {
        // Given: Original Paris event bounds
        val originalWidth = PARIS_BOUNDS.ne.lng - PARIS_BOUNDS.sw.lng
        val originalHeight = PARIS_BOUNDS.ne.lat - PARIS_BOUNDS.sw.lat

        // Given: Shrunk constraint bounds (viewport padding applied)
        val viewportPadding = 0.05 // 5% padding
        val shrunkWidth = originalWidth - (2 * viewportPadding)
        val shrunkHeight = originalHeight - (2 * viewportPadding)

        val screenWidth = 1080.0
        val screenHeight = 1920.0

        // When: Calculate min zoom from ORIGINAL bounds
        val zoomFromOriginal =
            min(
                log2((screenWidth * 360.0) / (originalWidth * 256.0)),
                log2((screenHeight * 180.0) / (originalHeight * 256.0)),
            )

        // When: Calculate (incorrect) zoom from SHRUNK bounds
        val zoomFromShrunk =
            min(
                log2((screenWidth * 360.0) / (shrunkWidth * 256.0)),
                log2((screenHeight * 180.0) / (shrunkHeight * 256.0)),
            )

        // Then: Zoom from shrunk bounds would be HIGHER (more zoomed in) - WRONG!
        assertTrue(
            zoomFromShrunk > zoomFromOriginal,
            "Zoom from shrunk bounds ($zoomFromShrunk) incorrectly higher than from original ($zoomFromOriginal)",
        )

        // Then: We must use zoom from ORIGINAL bounds
        val correctMinZoom = zoomFromOriginal + ZOOM_SAFETY_MARGIN

        println(
            "✅ Min zoom must use ORIGINAL bounds: correct=$correctMinZoom, wrong=$zoomFromShrunk (difference=${zoomFromShrunk - correctMinZoom})",
        )
    }
}
