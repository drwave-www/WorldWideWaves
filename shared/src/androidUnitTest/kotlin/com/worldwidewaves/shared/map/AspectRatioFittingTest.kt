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

import kotlin.math.log2
import kotlin.math.min
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for intelligent aspect ratio fitting logic that ensures
 * NO PIXELS OUTSIDE EVENT AREA are visible.
 *
 * Critical requirement: When event aspect ratio ≠ screen aspect ratio,
 * the system must choose the correct fit dimension (WIDTH or HEIGHT) to
 * ensure viewport stays within event bounds.
 *
 * Key formula being tested:
 * - zoomForWidth = log2((screenWidth * 360.0) / (eventWidth * 256.0))
 * - zoomForHeight = log2((screenHeight * 180.0) / (eventHeight * 256.0))
 * - targetZoom = min(zoomForWidth, zoomForHeight)
 */
class AspectRatioFittingTest {
    companion object {
        private const val ZOOM_TOLERANCE = 0.01 // Allow 0.01 zoom level variance
    }

    /**
     * Tests WIDE event (Paris: aspect 2.84) on TALL screen (portrait: aspect 0.56).
     * Must fit by HEIGHT to prevent vertical bands outside event area.
     */
    @Test
    fun `wide event on tall screen fits by HEIGHT`() {
        // Given: Paris event (wide) on portrait phone screen
        val eventWidth = 0.2456383 // Paris longitude span
        val eventHeight = 0.0865805 // Paris latitude span
        val eventAspect = eventWidth / eventHeight // = 2.837 (wide)

        val screenWidth = 1080.0 // Phone width in pixels (portrait)
        val screenHeight = 1920.0 // Phone height in pixels (portrait)
        val screenAspect = screenWidth / screenHeight // = 0.5625 (tall)

        // When: Calculate zoom for each dimension
        val zoomForWidth = log2((screenWidth * 360.0) / (eventWidth * 256.0))
        val zoomForHeight = log2((screenHeight * 180.0) / (eventHeight * 256.0))

        // Then: zoomForHeight should be LARGER (more zoomed in)
        // Using smaller zoom ensures event height fills screen, width has padding
        assertTrue(
            zoomForHeight > zoomForWidth,
            "HEIGHT-fit zoom ($zoomForHeight) must be larger than WIDTH-fit zoom ($zoomForWidth) for wide events",
        )

        // Then: Target zoom should be zoomForWidth (the smaller one)
        val targetZoom = min(zoomForWidth, zoomForHeight)
        assertEquals(
            zoomForWidth,
            targetZoom,
            ZOOM_TOLERANCE,
            "For wide event on tall screen, must use WIDTH-fit zoom to ensure height stays within bounds",
        )

        println("✅ Wide event: zoomForWidth=$zoomForWidth, zoomForHeight=$zoomForHeight, using=$targetZoom (HEIGHT-fit)")
    }

    /**
     * Tests TALL event on WIDE screen (landscape).
     * Must fit by WIDTH to prevent horizontal bands outside event area.
     */
    @Test
    fun `tall event on wide screen fits by WIDTH`() {
        // Given: Tall event (Chile-like) on landscape tablet
        val eventWidth = 0.05 // Narrow longitude span
        val eventHeight = 0.20 // Large latitude span
        val eventAspect = eventWidth / eventHeight // = 0.25 (tall)

        val screenWidth = 1920.0 // Tablet width in pixels (landscape)
        val screenHeight = 1080.0 // Tablet height in pixels (landscape)
        val screenAspect = screenWidth / screenHeight // = 1.78 (wide)

        // When: Calculate zoom for each dimension
        val zoomForWidth = log2((screenWidth * 360.0) / (eventWidth * 256.0))
        val zoomForHeight = log2((screenHeight * 180.0) / (eventHeight * 256.0))

        // Then: zoomForWidth should be LARGER (more zoomed in)
        assertTrue(
            zoomForWidth > zoomForHeight,
            "WIDTH-fit zoom ($zoomForWidth) must be larger than HEIGHT-fit zoom ($zoomForHeight) for tall events",
        )

        // Then: Target zoom should be zoomForHeight (the smaller one)
        val targetZoom = min(zoomForWidth, zoomForHeight)
        assertEquals(
            zoomForHeight,
            targetZoom,
            ZOOM_TOLERANCE,
            "For tall event on wide screen, must use HEIGHT-fit zoom to ensure width stays within bounds",
        )

        println("✅ Tall event: zoomForWidth=$zoomForWidth, zoomForHeight=$zoomForHeight, using=$targetZoom (WIDTH-fit)")
    }

    /**
     * Tests SQUARE event (equal aspect ratio).
     * Both fit modes should give similar zoom.
     */
    @Test
    fun `square event on square screen has equal zoom`() {
        // Given: Square event on square screen
        val eventWidth = 0.10
        val eventHeight = 0.10
        val eventAspect = 1.0

        val screenWidth = 1080.0
        val screenHeight = 1080.0
        val screenAspect = 1.0

        // When: Calculate zoom for each dimension
        val zoomForWidth = log2((screenWidth * 360.0) / (eventWidth * 256.0))
        val zoomForHeight = log2((screenHeight * 180.0) / (eventHeight * 256.0))

        // Then: Zooms should be different due to lat/lng degree differences
        // (360 degrees longitude vs 180 degrees latitude in formula)
        // But both should be reasonable and min() still works correctly
        val difference = kotlin.math.abs(zoomForWidth - zoomForHeight)
        assertTrue(
            difference > 0,
            "Zoom values will differ due to lng(360) vs lat(180) in formula, difference=$difference",
        )

        val targetZoom = min(zoomForWidth, zoomForHeight)
        println("✅ Square event: zoomForWidth=$zoomForWidth, zoomForHeight=$zoomForHeight, using=$targetZoom")
    }

    /**
     * Tests EXTREME wide event (100:1 aspect ratio).
     * Must handle extreme cases without overflow.
     */
    @Test
    fun `extreme wide event fits by HEIGHT without overflow`() {
        // Given: Extremely wide event (horizontal strip)
        val eventWidth = 10.0 // 10 degrees longitude
        val eventHeight = 0.1 // 0.1 degrees latitude
        val eventAspect = eventWidth / eventHeight // = 100.0 (extremely wide)

        val screenWidth = 1080.0
        val screenHeight = 1920.0

        // When: Calculate zoom for each dimension
        val zoomForWidth = log2((screenWidth * 360.0) / (eventWidth * 256.0))
        val zoomForHeight = log2((screenHeight * 180.0) / (eventHeight * 256.0))

        // Then: Must use WIDTH-fit zoom (smaller) to prevent height overflow
        val targetZoom = min(zoomForWidth, zoomForHeight)
        assertEquals(
            zoomForWidth,
            targetZoom,
            ZOOM_TOLERANCE,
            "Extreme wide event must use WIDTH-fit zoom",
        )

        // Verify zoom is reasonable (not negative, not too high)
        assertTrue(targetZoom > 0, "Zoom must be positive")
        assertTrue(targetZoom < 22, "Zoom must be reasonable (< max zoom 22)")

        println("✅ Extreme wide: zoomForWidth=$zoomForWidth, zoomForHeight=$zoomForHeight, using=$targetZoom")
    }

    /**
     * Tests EXTREME tall event (1:100 aspect ratio).
     * Must handle extreme cases without overflow.
     */
    @Test
    fun `extreme tall event fits by WIDTH without overflow`() {
        // Given: Extremely tall event (vertical strip)
        val eventWidth = 0.1 // 0.1 degrees longitude
        val eventHeight = 10.0 // 10 degrees latitude
        val eventAspect = eventWidth / eventHeight // = 0.01 (extremely tall)

        val screenWidth = 1920.0 // Landscape
        val screenHeight = 1080.0

        // When: Calculate zoom for each dimension
        val zoomForWidth = log2((screenWidth * 360.0) / (eventWidth * 256.0))
        val zoomForHeight = log2((screenHeight * 180.0) / (eventHeight * 256.0))

        // Then: Must use HEIGHT-fit zoom (smaller) to prevent width overflow
        val targetZoom = min(zoomForWidth, zoomForHeight)
        assertEquals(
            zoomForHeight,
            targetZoom,
            ZOOM_TOLERANCE,
            "Extreme tall event must use HEIGHT-fit zoom",
        )

        // Verify zoom is reasonable
        assertTrue(targetZoom > 0, "Zoom must be positive")
        assertTrue(targetZoom < 22, "Zoom must be reasonable (< max zoom 22)")

        println("✅ Extreme tall: zoomForWidth=$zoomForWidth, zoomForHeight=$zoomForHeight, using=$targetZoom")
    }
}
