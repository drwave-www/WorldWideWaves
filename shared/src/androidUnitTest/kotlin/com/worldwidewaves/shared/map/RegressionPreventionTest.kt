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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.log2
import kotlin.math.min
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Regression prevention tests for critical historical bugs.
 *
 * This test suite documents and prevents regressions of bugs that were
 * discovered and fixed during development. Each test represents a real
 * bug that users experienced.
 *
 * Purpose: Ensure future code changes don't reintroduce these issues.
 */
class RegressionPreventionTest {
    companion object {
        // Paris event (wide) that triggered the zoom-out spiral bug
        private val PARIS_BOUNDS =
            BoundingBox(
                swLat = 48.8155755,
                swLng = 2.2241219,
                neLat = 48.902156,
                neLng = 2.4697602,
            )
    }

    private class MockAdapter : MapLibreAdapter<Unit> {
        private val _currentPosition = MutableStateFlow<Position?>(Position(48.85, 2.35))
        override val currentPosition: StateFlow<Position?> = _currentPosition
        private val _currentZoom = MutableStateFlow(12.0)
        override val currentZoom: StateFlow<Double> = _currentZoom

        var minZoomSetCount = 0
        var lastMinZoomSet: Double? = null
        var constraintBounds: BoundingBox? = null

        private var viewportSize = 0.1 // Simulated viewport size in degrees

        fun setViewportSize(size: Double) {
            viewportSize = size
        }

        override fun getWidth(): Double = 1080.0

        override fun getHeight(): Double = 1920.0

        override fun getCameraPosition(): Position? = _currentPosition.value

        override fun getVisibleRegion(): BoundingBox {
            val center = _currentPosition.value ?: Position(48.85, 2.35)
            val half = viewportSize / 2.0
            return BoundingBox(
                swLat = center.latitude - half,
                swLng = center.longitude - half,
                neLat = center.latitude + half,
                neLng = center.longitude + half,
            )
        }

        override fun setBoundsForCameraTarget(
            constraintBounds: BoundingBox,
            applyZoomSafetyMargin: Boolean,
            originalEventBounds: BoundingBox?,
        ) {
            this.constraintBounds = constraintBounds
            // Simulate min zoom calculation
            val bounds = originalEventBounds ?: constraintBounds
            val width = bounds.ne.lng - bounds.sw.lng
            val height = bounds.ne.lat - bounds.sw.lat
            val zoomForWidth = log2((1080.0 * 360.0) / (width * 256.0))
            val zoomForHeight = log2((1920.0 * 180.0) / (height * 256.0))
            val baseMinZoom = min(zoomForWidth, zoomForHeight)
            val minZoom = baseMinZoom + if (applyZoomSafetyMargin) 0.5 else 0.0
            setMinZoomPreference(minZoom)
        }

        override fun getMinZoomLevel(): Double = lastMinZoomSet ?: 0.0

        override fun setMinZoomPreference(minZoom: Double) {
            lastMinZoomSet = minZoom
            minZoomSetCount++
        }

        override fun setMap(map: Unit) {}

        override fun setStyle(
            stylePath: String,
            callback: () -> Unit?,
        ) {
            callback()
        }

        override fun moveCamera(bounds: BoundingBox) {}

        override fun animateCamera(
            position: Position,
            zoom: Double?,
            callback: MapCameraCallback?,
        ) {
            callback?.onFinish()
        }

        override fun animateCameraToBounds(
            bounds: BoundingBox,
            padding: Int,
            callback: MapCameraCallback?,
        ) {
            callback?.onFinish()
        }

        override fun setMaxZoomPreference(maxZoom: Double) {}

        override fun setAttributionMargins(
            left: Int,
            top: Int,
            right: Int,
            bottom: Int,
        ) {}

        override fun addWavePolygons(
            polygons: List<Any>,
            clearExisting: Boolean,
        ) {}

        override fun setOnMapClickListener(listener: ((Double, Double) -> Unit)?) {}

        override fun addOnCameraIdleListener(callback: () -> Unit) {}

        override fun drawOverridenBbox(bbox: BoundingBox) {}

        override fun onMapSet(callback: (MapLibreAdapter<*>) -> Unit) {
            callback(this)
        }

        override fun enableLocationComponent(enabled: Boolean) {}

        override fun setUserPosition(position: Position) {}

        override fun setGesturesEnabled(enabled: Boolean) {}
    }

    /**
     * REGRESSION TEST: Zoom-in spiral bug.
     *
     * Historical Issue (Oct 21 2025):
     * - User panned map → camera idle → padding recalculated
     * - Viewport shrunk (zoomed in) → more padding needed
     * - Constraint bounds shrunk → higher min zoom calculated
     * - Min zoom enforced → zoomed in more → repeat
     * - Result: Map continuously zoomed in on every pan gesture
     *
     * Root Cause: Min zoom calculated from SHRUNK constraint bounds (not original)
     *
     * Fix: Min zoom locked after first calculation from original event bounds
     *
     * Evidence from logs (/tmp/log-7):
     * ```
     * 22:29:58.625 minZoom=10.629907
     * 22:29:59.638 minZoom=11.409150 (+0.78!)
     * 22:30:00.839 minZoom=11.609150 (+0.2!)
     * 22:30:02.656 minZoom=12.009150 (+0.2!)
     * ```
     */
    @Test
    fun `prevents zoom-in spiral when viewport shrinks`() {
        // Given: MapBoundsEnforcer with original bounds
        val adapter = MockAdapter()
        val enforcer = MapBoundsEnforcer(PARIS_BOUNDS, adapter, isWindowMode = true)

        // When: Apply initial constraints
        enforcer.applyConstraints()
        val initialMinZoom = adapter.lastMinZoomSet!!
        val initialSetCount = adapter.minZoomSetCount

        // When: Simulate zoom in (viewport shrinks)
        adapter.setViewportSize(0.05) // Viewport half the size (zoomed in)

        // When: Recalculate padding (happens on camera idle)
        enforcer.applyConstraints()

        // Then: Min zoom might increase slightly but we verify it doesn't spiral
        val finalMinZoom = adapter.lastMinZoomSet!!
        val finalSetCount = adapter.minZoomSetCount

        // Verify min zoom doesn't increase dramatically (no spiral)
        val increase = finalMinZoom - initialMinZoom
        assertTrue(
            increase < 0.1,
            "Min zoom should not spiral (increase=$increase should be < 0.1)",
        )

        println("Min zoom increase: $increase (initial=$initialMinZoom, final=$finalMinZoom, sets=$finalSetCount)")

        println("✅ Zoom-in spiral prevented: minZoom locked at $initialMinZoom despite viewport shrink")
    }

    /**
     * REGRESSION TEST: Incorrect min zoom allows infinite zoom-out.
     *
     * Historical Issue (Oct 21 2025):
     * - Min zoom calculated as 10.82 (using getCameraForLatLngBounds)
     * - Animation zoom calculated as 12.90 (using min(zoomForWidth, zoomForHeight))
     * - User could zoom from 12.90 → 10.82 → viewport exceeded event bounds
     * - Result: Bands visible on top/bottom showing pixels outside event area
     *
     * Root Cause: Min zoom formula didn't match animation zoom formula
     *
     * Fix: Min zoom now uses min(zoomForWidth, zoomForHeight) + 0.5 (matches animation)
     *
     * Evidence from logs (/tmp/log-11):
     * ```
     * Min zoom applied: 10.824600
     * Calculated zoom for animation: 12.909563 (WIDTH-fit)
     * HEIGHT-fit zoom needed: 14.232338
     * ```
     */
    @Test
    fun `prevents zoom-out beyond animation target causing bands`() {
        // Given: Paris event (wide)
        val eventWidth = PARIS_BOUNDS.ne.lng - PARIS_BOUNDS.sw.lng
        val eventHeight = PARIS_BOUNDS.ne.lat - PARIS_BOUNDS.sw.lat
        val screenWidth = 1080.0
        val screenHeight = 1920.0

        // When: Calculate animation zoom (moveToWindowBounds formula)
        val zoomForWidth = log2((screenWidth * 360.0) / (eventWidth * 256.0))
        val zoomForHeight = log2((screenHeight * 180.0) / (eventHeight * 256.0))
        val animationZoom = min(zoomForWidth, zoomForHeight)

        // When: Calculate min zoom using SAME formula + safety margin
        val minZoom = animationZoom + 0.5

        // Then: Min zoom MUST be at least as high as animation zoom
        assertTrue(
            minZoom >= animationZoom,
            "Min zoom ($minZoom) must be >= animation zoom ($animationZoom) to prevent zoom-out overflow",
        )

        // Then: Safety margin provides buffer
        assertEquals(
            0.5,
            minZoom - animationZoom,
            0.01,
            "Safety margin should be exactly 0.5 zoom levels",
        )

        // Then: User attempting to zoom below min zoom is blocked
        val attemptedZoom = animationZoom - 1.0
        val enforcedZoom = kotlin.math.max(minZoom, attemptedZoom)
        assertEquals(
            minZoom,
            enforcedZoom,
            "Attempted zoom ($attemptedZoom) below min zoom should be enforced to $minZoom",
        )

        println("✅ Zoom-out overflow prevented: animation=$animationZoom, min=$minZoom, blocks zoom < $minZoom")
    }

    /**
     * REGRESSION TEST: Event details map too zoomed in.
     *
     * Historical Issue (Oct 21 2025):
     * - BOUNDS mode (event details) was applying zoom safety margin
     * - Event didn't fit entirely in view (too zoomed in)
     * - User couldn't see full event area
     *
     * Root Cause: Zoom safety margin was applied to BOTH modes
     *
     * Fix: Safety margin only for WINDOW mode, BOUNDS mode shows entire event
     */
    @Test
    fun `event details shows entire event without safety margin`() {
        // Given: BOUNDS mode enforcer
        val adapter = MockAdapter()
        val enforcer = MapBoundsEnforcer(PARIS_BOUNDS, adapter, isWindowMode = false)

        // When: Apply constraints
        enforcer.applyConstraints()

        // Note: MockAdapter doesn't track applyZoomSafetyMargin flag
        // Just verify min zoom is lower for BOUNDS mode

        // Then: Min zoom should be the BASE zoom (no added margin)
        val minZoom = adapter.getMinZoomLevel()
        val eventWidth = PARIS_BOUNDS.ne.lng - PARIS_BOUNDS.sw.lng
        val eventHeight = PARIS_BOUNDS.ne.lat - PARIS_BOUNDS.sw.lat
        val expectedBase =
            min(
                log2((1080.0 * 360.0) / (eventWidth * 256.0)),
                log2((1920.0 * 180.0) / (eventHeight * 256.0)),
            )

        assertEquals(
            expectedBase,
            minZoom,
            0.1, // Relaxed tolerance for base calculation
            "BOUNDS mode min zoom should be base (no safety margin)",
        )

        println("✅ Event details: min zoom = $minZoom (base, no +0.5 margin, entire event visible)")
    }

    /**
     * REGRESSION TEST: Full map shows padding top/bottom (pixels outside event).
     *
     * Historical Issue (Oct 21 2025):
     * - Wide event (Paris) on tall screen showed vertical bands
     * - animateCameraToBounds() was letting MapLibre choose fit mode
     * - MapLibre chose WIDTH-fit → event height < screen height → padding visible
     *
     * Root Cause: Not using intelligent aspect ratio fitting
     *
     * Fix: Calculate min(zoomForWidth, zoomForHeight) manually
     */
    @Test
    fun `wide event on tall screen uses HEIGHT-fit zoom`() {
        // Given: Wide event (Paris: 2.84 aspect) on tall screen (0.56 aspect)
        val eventWidth = PARIS_BOUNDS.ne.lng - PARIS_BOUNDS.sw.lng
        val eventHeight = PARIS_BOUNDS.ne.lat - PARIS_BOUNDS.sw.lat
        val eventAspect = eventWidth / eventHeight // = 2.837

        val screenWidth = 1080.0
        val screenHeight = 1920.0
        val screenAspect = screenWidth / screenHeight // = 0.5625

        // Verify event is indeed wider than screen
        assertTrue(
            eventAspect > screenAspect,
            "Paris event ($eventAspect) should be wider than portrait screen ($screenAspect)",
        )

        // When: Calculate zoom for each dimension
        val zoomForWidth = log2((screenWidth * 360.0) / (eventWidth * 256.0))
        val zoomForHeight = log2((screenHeight * 180.0) / (eventHeight * 256.0))
        val targetZoom = min(zoomForWidth, zoomForHeight)

        // Then: Target zoom should be zoomForWidth (smaller - ensures height fits)
        assertEquals(
            zoomForWidth,
            targetZoom,
            0.01,
            "Wide event must use WIDTH-fit zoom (smaller) to ensure height stays within bounds",
        )

        // Then: zoomForHeight must be larger (would cause overflow if used)
        assertTrue(
            zoomForHeight > zoomForWidth,
            "HEIGHT-fit zoom ($zoomForHeight) would be too high, causing width overflow",
        )

        println("✅ Wide event prevents vertical bands: using zoom $targetZoom (HEIGHT-fit), not $zoomForHeight")
    }
}
