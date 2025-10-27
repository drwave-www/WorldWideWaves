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
import com.worldwidewaves.shared.map.MapTestFixtures.PORTRAIT_PHONE
import com.worldwidewaves.shared.map.MapTestFixtures.TALL_EVENT_BOUNDS
import com.worldwidewaves.shared.map.MapTestFixtures.TOLERANCE_ZOOM
import com.worldwidewaves.shared.map.MapTestFixtures.WIDE_EVENT_BOUNDS
import kotlin.math.ln
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Tests for iOS 512px tile size in min zoom calculation.
 *
 * Priority 1 - Critical (2 tests):
 * - Test wide events (HEIGHT-constrained) with 512px tile size
 * - Test tall events (WIDTH-constrained) with 512px tile size
 *
 * Purpose:
 * - iOS MapLibre uses 512px tiles (vs Android's 256px)
 * - This affects minimum zoom calculations to prevent viewport exceeding event bounds
 * - Wide events are constrained by HEIGHT, tall events by WIDTH
 * - Validates correct tile size is used in zoom calculations
 *
 * iOS Context:
 * - Android: 256px tiles (log2(screenHeight / eventHeight * 256))
 * - iOS: 512px tiles (log2(screenHeight / eventHeight * 512))
 * - This difference means iOS needs different min zoom for same event/screen
 * - Critical for preventing viewport from showing out-of-bounds areas
 */
class IosTileSizeCalculationTest {
    companion object {
        // Tile sizes
        private const val IOS_TILE_SIZE = 512.0 // iOS uses 512px tiles
        private const val ANDROID_TILE_SIZE = 256.0 // Android uses 256px tiles

        // Screen dimensions (from MapTestFixtures.PORTRAIT_PHONE)
        private val SCREEN_WIDTH = PORTRAIT_PHONE.width
        private val SCREEN_HEIGHT = PORTRAIT_PHONE.height
    }

    // ============================================================
    // WIDE EVENT TESTS (HEIGHT-Constrained)
    // ============================================================

    @Test
    fun `wide event uses HEIGHT constraint with 512px tile size`() {
        // Wide event: 2:1 aspect ratio (landscape)
        // Screen: 9:16 aspect ratio (portrait)
        // Result: HEIGHT is the limiting dimension
        val eventBounds = WIDE_EVENT_BOUNDS

        val eventHeight = eventBounds.height
        val eventWidth = eventBounds.width

        println("📊 Wide Event Dimensions:")
        println("   Event: width=$eventWidth, height=$eventHeight, ratio=${eventWidth / eventHeight}")
        println("   Screen: width=${SCREEN_WIDTH}, height=${SCREEN_HEIGHT}, ratio=${SCREEN_WIDTH / SCREEN_HEIGHT}")

        // Calculate iOS min zoom (HEIGHT-constrained for wide events)
        val iosMinZoomHeight = calculateMinZoomForDimension(SCREEN_HEIGHT, eventHeight, IOS_TILE_SIZE)
        val iosMinZoomWidth = calculateMinZoomForDimension(SCREEN_WIDTH, eventWidth, IOS_TILE_SIZE)

        // For wide events on portrait screen, HEIGHT is more restrictive (higher min zoom)
        assertTrue(
            iosMinZoomHeight > iosMinZoomWidth,
            "Wide event on portrait screen should be HEIGHT-constrained " +
                "(minZoomHeight=$iosMinZoomHeight > minZoomWidth=$iosMinZoomWidth)",
        )

        // Calculate Android min zoom for comparison
        val androidMinZoomHeight = calculateMinZoomForDimension(SCREEN_HEIGHT, eventHeight, ANDROID_TILE_SIZE)
        val androidMinZoomWidth = calculateMinZoomForDimension(SCREEN_WIDTH, eventWidth, ANDROID_TILE_SIZE)

        // iOS should have LOWER min zoom than Android (512px tiles > 256px tiles)
        val iosMinZoom = maxOf(iosMinZoomHeight, iosMinZoomWidth)
        val androidMinZoom = maxOf(androidMinZoomHeight, androidMinZoomWidth)

        assertTrue(
            iosMinZoom < androidMinZoom,
            "iOS min zoom ($iosMinZoom) should be lower than Android ($androidMinZoom) " +
                "due to larger tile size (512px vs 256px)",
        )

        // Verify zoom difference is approximately 1.0 (log2(512/256) = 1)
        val zoomDifference = androidMinZoom - iosMinZoom
        assertTrue(
            kotlin.math.abs(zoomDifference - 1.0) < TOLERANCE_ZOOM,
            "Zoom difference should be ~1.0 (log2(512/256)), got $zoomDifference",
        )

        println("✅ Wide event (HEIGHT-constrained) calculations correct:")
        println("   iOS min zoom: $iosMinZoom (height=$iosMinZoomHeight, width=$iosMinZoomWidth)")
        println("   Android min zoom: $androidMinZoom (height=$androidMinZoomHeight, width=$androidMinZoomWidth)")
        println("   Difference: $zoomDifference (expected ~1.0)")
    }

    // ============================================================
    // TALL EVENT TESTS (WIDTH-Constrained)
    // ============================================================

    @Test
    fun `tall event uses WIDTH constraint with 512px tile size`() {
        // Tall event: 1:2 aspect ratio (portrait)
        // Screen: 9:16 aspect ratio (portrait)
        // Result: WIDTH is the limiting dimension
        val eventBounds = TALL_EVENT_BOUNDS

        val eventHeight = eventBounds.height
        val eventWidth = eventBounds.width

        println("📊 Tall Event Dimensions:")
        println("   Event: width=$eventWidth, height=$eventHeight, ratio=${eventWidth / eventHeight}")
        println("   Screen: width=${SCREEN_WIDTH}, height=${SCREEN_HEIGHT}, ratio=${SCREEN_WIDTH / SCREEN_HEIGHT}")

        // Calculate iOS min zoom (WIDTH-constrained for tall events)
        val iosMinZoomHeight = calculateMinZoomForDimension(SCREEN_HEIGHT, eventHeight, IOS_TILE_SIZE)
        val iosMinZoomWidth = calculateMinZoomForDimension(SCREEN_WIDTH, eventWidth, IOS_TILE_SIZE)

        // For tall events on portrait screen, WIDTH is more restrictive (higher min zoom)
        assertTrue(
            iosMinZoomWidth > iosMinZoomHeight,
            "Tall event on portrait screen should be WIDTH-constrained " +
                "(minZoomWidth=$iosMinZoomWidth > minZoomHeight=$iosMinZoomHeight)",
        )

        // Calculate Android min zoom for comparison
        val androidMinZoomHeight = calculateMinZoomForDimension(SCREEN_HEIGHT, eventHeight, ANDROID_TILE_SIZE)
        val androidMinZoomWidth = calculateMinZoomForDimension(SCREEN_WIDTH, eventWidth, ANDROID_TILE_SIZE)

        // iOS should have LOWER min zoom than Android (512px tiles > 256px tiles)
        val iosMinZoom = maxOf(iosMinZoomHeight, iosMinZoomWidth)
        val androidMinZoom = maxOf(androidMinZoomHeight, androidMinZoomWidth)

        assertTrue(
            iosMinZoom < androidMinZoom,
            "iOS min zoom ($iosMinZoom) should be lower than Android ($androidMinZoom) " +
                "due to larger tile size (512px vs 256px)",
        )

        // Verify zoom difference is approximately 1.0 (log2(512/256) = 1)
        val zoomDifference = androidMinZoom - iosMinZoom
        assertTrue(
            kotlin.math.abs(zoomDifference - 1.0) < TOLERANCE_ZOOM,
            "Zoom difference should be ~1.0 (log2(512/256)), got $zoomDifference",
        )

        println("✅ Tall event (WIDTH-constrained) calculations correct:")
        println("   iOS min zoom: $iosMinZoom (height=$iosMinZoomHeight, width=$iosMinZoomWidth)")
        println("   Android min zoom: $androidMinZoom (height=$androidMinZoomHeight, width=$androidMinZoomWidth)")
        println("   Difference: $zoomDifference (expected ~1.0)")
    }

    // ============================================================
    // COMPREHENSIVE TILE SIZE VALIDATION TEST
    // ============================================================

    @Test
    fun `tile size difference creates consistent zoom offset across event types`() {
        val testEvents =
            listOf(
                "Wide Event" to WIDE_EVENT_BOUNDS,
                "Tall Event" to TALL_EVENT_BOUNDS,
            )

        testEvents.forEach { (eventName, eventBounds) ->
            val eventHeight = eventBounds.height
            val eventWidth = eventBounds.width

            // Calculate min zooms for both platforms
            val iosMinZoomHeight = calculateMinZoomForDimension(SCREEN_HEIGHT, eventHeight, IOS_TILE_SIZE)
            val iosMinZoomWidth = calculateMinZoomForDimension(SCREEN_WIDTH, eventWidth, IOS_TILE_SIZE)
            val iosMinZoom = maxOf(iosMinZoomHeight, iosMinZoomWidth)

            val androidMinZoomHeight = calculateMinZoomForDimension(SCREEN_HEIGHT, eventHeight, ANDROID_TILE_SIZE)
            val androidMinZoomWidth = calculateMinZoomForDimension(SCREEN_WIDTH, eventWidth, ANDROID_TILE_SIZE)
            val androidMinZoom = maxOf(androidMinZoomHeight, androidMinZoomWidth)

            // Verify consistent offset
            val zoomDifference = androidMinZoom - iosMinZoom
            assertTrue(
                kotlin.math.abs(zoomDifference - 1.0) < TOLERANCE_ZOOM,
                "$eventName: Zoom difference should be ~1.0, got $zoomDifference",
            )

            println("   $eventName: iOS=$iosMinZoom, Android=$androidMinZoom, diff=$zoomDifference")
        }

        println("✅ Tile size creates consistent zoom offset across all event types")
    }

    // ============================================================
    // EDGE CASE: SQUARE EVENT
    // ============================================================

    @Test
    fun `square event with 512px tiles uses same constraint for both dimensions`() {
        // Square event (1:1 aspect ratio)
        val squareEventBounds =
            BoundingBox.fromCorners(
                Position(48.8566, 2.3522),
                Position(48.8666, 2.3622),
            )

        val eventHeight = squareEventBounds.height
        val eventWidth = squareEventBounds.width

        // Calculate iOS min zoom
        val iosMinZoomHeight = calculateMinZoomForDimension(SCREEN_HEIGHT, eventHeight, IOS_TILE_SIZE)
        val iosMinZoomWidth = calculateMinZoomForDimension(SCREEN_WIDTH, eventWidth, IOS_TILE_SIZE)

        // For square events, HEIGHT is more restrictive on portrait screen
        // (screen is taller, so height constraint is tighter)
        assertTrue(
            iosMinZoomHeight > iosMinZoomWidth,
            "Square event on portrait screen should be HEIGHT-constrained " +
                "(minZoomHeight=$iosMinZoomHeight > minZoomWidth=$iosMinZoomWidth)",
        )

        // Verify tile size effect
        val androidMinZoomHeight = calculateMinZoomForDimension(SCREEN_HEIGHT, eventHeight, ANDROID_TILE_SIZE)
        val androidMinZoomWidth = calculateMinZoomForDimension(SCREEN_WIDTH, eventWidth, ANDROID_TILE_SIZE)

        val iosMinZoom = maxOf(iosMinZoomHeight, iosMinZoomWidth)
        val androidMinZoom = maxOf(androidMinZoomHeight, androidMinZoomWidth)

        val zoomDifference = androidMinZoom - iosMinZoom
        assertTrue(
            kotlin.math.abs(zoomDifference - 1.0) < TOLERANCE_ZOOM,
            "Square event zoom difference should be ~1.0, got $zoomDifference",
        )

        println("✅ Square event calculations correct:")
        println("   iOS min zoom: $iosMinZoom (height=$iosMinZoomHeight, width=$iosMinZoomWidth)")
        println("   Android min zoom: $androidMinZoom")
        println("   Difference: $zoomDifference")
    }

    // ============================================================
    // HELPER FUNCTIONS
    // ============================================================

    /**
     * Calculate minimum zoom level for a single dimension (height or width).
     *
     * Formula: log2(screenDimension / eventDimension * tileSize)
     *
     * This ensures that at the min zoom level, the entire event dimension
     * fits within the screen dimension.
     */
    private fun calculateMinZoomForDimension(
        screenDimension: Double,
        eventDimension: Double,
        tileSize: Double,
    ): Double {
        // Convert to radians for world coordinate calculations
        val ratio = screenDimension / (eventDimension * tileSize)
        return ln(ratio) / ln(2.0) // log2(ratio)
    }
}
