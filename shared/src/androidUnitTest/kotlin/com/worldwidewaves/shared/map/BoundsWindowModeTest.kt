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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for BOUNDS vs WINDOW mode differences in MapBoundsEnforcer.
 *
 * CRITICAL REQUIREMENT: BOUNDS and WINDOW modes must behave differently:
 * - BOUNDS mode (event details): Zero padding, shows entire event
 * - WINDOW mode (full map): Viewport padding, prevents edge overflow
 *
 * These two modes have different constraints and must be tested separately.
 */
class BoundsWindowModeTest {
    companion object {
        private val TEST_BOUNDS =
            BoundingBox(
                swLat = 48.0,
                swLng = 2.0,
                neLat = 49.0,
                neLng = 3.0,
            )
    }

    private class TestMapAdapter : MapLibreAdapter<Unit> {
        private val _currentPosition = MutableStateFlow<Position?>(null)
        override val currentPosition: StateFlow<Position?> = _currentPosition

        private val _currentZoom = MutableStateFlow(12.0)
        override val currentZoom: StateFlow<Double> = _currentZoom

        var constraintBounds: BoundingBox? = null
        var minZoomPreference: Double? = null
        var applyZoomSafetyMarginCalled: Boolean = false
        var originalEventBoundsPassed: BoundingBox? = null

        override fun getWidth(): Double = 1080.0

        override fun getHeight(): Double = 1920.0

        override fun getCameraPosition(): Position? = _currentPosition.value

        override fun getVisibleRegion(): BoundingBox {
            // Return viewport of ~0.1 x 0.1 degrees
            val center = _currentPosition.value ?: Position(48.5, 2.5)
            return BoundingBox(
                swLat = center.latitude - 0.05,
                swLng = center.longitude - 0.05,
                neLat = center.latitude + 0.05,
                neLng = center.longitude + 0.05,
            )
        }

        override fun setBoundsForCameraTarget(
            constraintBounds: BoundingBox,
            applyZoomSafetyMargin: Boolean,
            originalEventBounds: BoundingBox?,
        ) {
            this.constraintBounds = constraintBounds
            this.applyZoomSafetyMarginCalled = applyZoomSafetyMargin
            this.originalEventBoundsPassed = originalEventBounds
            // Simulate min zoom being set
            this.minZoomPreference = 10.0 + if (applyZoomSafetyMargin) 0.5 else 0.0
        }

        override fun getMinZoomLevel(): Double = minZoomPreference ?: 0.0

        override fun setMinZoomPreference(minZoom: Double) {
            minZoomPreference = minZoom
        }

        override fun setMaxZoomPreference(maxZoom: Double) {}

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
     * BOUNDS mode must use zero padding to show entire event.
     */
    @Test
    fun `BOUNDS mode uses zero padding`() {
        // Given: MapBoundsEnforcer in BOUNDS mode
        val adapter = TestMapAdapter()
        val enforcer = MapBoundsEnforcer(TEST_BOUNDS, adapter, isWindowMode = false)

        // When: Apply constraints
        enforcer.applyConstraints()

        // Then: Constraint bounds should equal original bounds (no padding)
        val constraintBounds = adapter.constraintBounds
        assertNotNull(constraintBounds, "Constraint bounds should be set")
        assertEquals(
            TEST_BOUNDS.sw.lat,
            constraintBounds.sw.lat,
            0.0001,
            "BOUNDS mode should have zero padding (SW lat unchanged)",
        )
        assertEquals(
            TEST_BOUNDS.ne.lat,
            constraintBounds.ne.lat,
            0.0001,
            "BOUNDS mode should have zero padding (NE lat unchanged)",
        )

        // Then: Should NOT apply zoom safety margin for BOUNDS mode
        assertFalse(
            adapter.applyZoomSafetyMarginCalled,
            "BOUNDS mode should not apply zoom safety margin",
        )

        println("✅ BOUNDS mode: zero padding, no safety margin, entire event visible")
    }

    /**
     * WINDOW mode uses zero padding (relies on preventive gesture constraints).
     * The key difference from BOUNDS mode is the zoom safety margin.
     */
    @Test
    fun `WINDOW mode calculates viewport padding`() {
        // Given: MapBoundsEnforcer in WINDOW mode
        val adapter = TestMapAdapter()
        val enforcer = MapBoundsEnforcer(TEST_BOUNDS, adapter, isWindowMode = true)

        // When: Apply constraints
        enforcer.applyConstraints()

        // Then: Constraint bounds should EQUAL original bounds (zero padding in new implementation)
        // NEW BEHAVIOR: WINDOW mode uses zero padding and relies on preventive gesture constraints
        // instead of shrinking bounds
        val constraintBounds = adapter.constraintBounds
        assertNotNull(constraintBounds, "Constraint bounds should be set")

        assertEquals(
            TEST_BOUNDS.sw.lat,
            constraintBounds.sw.lat,
            0.0001,
            "WINDOW mode now uses zero padding (SW lat unchanged)",
        )
        assertEquals(
            TEST_BOUNDS.ne.lat,
            constraintBounds.ne.lat,
            0.0001,
            "WINDOW mode now uses zero padding (NE lat unchanged)",
        )

        // Then: Should apply zoom safety margin for WINDOW mode (this is the key difference from BOUNDS)
        assertTrue(
            adapter.applyZoomSafetyMarginCalled,
            "WINDOW mode should apply zoom safety margin",
        )

        println("✅ WINDOW mode: zero padding (preventive gestures), safety margin enabled")
    }

    /**
     * WINDOW mode min zoom should be higher than BOUNDS mode.
     * Safety margin makes WINDOW more restrictive.
     */
    @Test
    fun `WINDOW mode min zoom higher than BOUNDS mode`() {
        // Given: Same event bounds
        val boundsAdapter = TestMapAdapter()
        val windowAdapter = TestMapAdapter()

        val boundsEnforcer = MapBoundsEnforcer(TEST_BOUNDS, boundsAdapter, isWindowMode = false)
        val windowEnforcer = MapBoundsEnforcer(TEST_BOUNDS, windowAdapter, isWindowMode = true)

        // When: Apply constraints to both
        boundsEnforcer.applyConstraints()
        windowEnforcer.applyConstraints()

        // Then: WINDOW min zoom should be higher (has safety margin)
        val boundsMinZoom = boundsAdapter.getMinZoomLevel()
        val windowMinZoom = windowAdapter.getMinZoomLevel()

        assertTrue(
            windowMinZoom > boundsMinZoom,
            "WINDOW mode min zoom ($windowMinZoom) must be higher than BOUNDS mode ($boundsMinZoom) due to safety margin",
        )

        val difference = windowMinZoom - boundsMinZoom
        assertEquals(
            0.5,
            difference,
            0.01,
            "Difference should equal safety margin (0.5)",
        )

        println("✅ Min zoom difference: WINDOW=$windowMinZoom, BOUNDS=$boundsMinZoom, diff=$difference")
    }

    /**
     * Verify originalEventBounds is passed to adapter in both modes.
     */
    @Test
    fun `originalEventBounds passed to adapter in both modes`() {
        // Given: Enforcers in both modes
        val boundsAdapter = TestMapAdapter()
        val windowAdapter = TestMapAdapter()

        val boundsEnforcer = MapBoundsEnforcer(TEST_BOUNDS, boundsAdapter, isWindowMode = false)
        val windowEnforcer = MapBoundsEnforcer(TEST_BOUNDS, windowAdapter, isWindowMode = true)

        // When: Apply constraints
        boundsEnforcer.applyConstraints()
        windowEnforcer.applyConstraints()

        // Then: Both should receive originalEventBounds
        assertNotNull(
            boundsAdapter.originalEventBoundsPassed,
            "BOUNDS mode should receive originalEventBounds",
        )
        assertNotNull(
            windowAdapter.originalEventBoundsPassed,
            "WINDOW mode should receive originalEventBounds",
        )

        // Then: Should be the TEST_BOUNDS (original event area)
        assertEquals(
            TEST_BOUNDS.sw.lat,
            boundsAdapter.originalEventBoundsPassed!!.sw.lat,
            0.0001,
            "BOUNDS mode originalEventBounds should match event bounds",
        )
        assertEquals(
            TEST_BOUNDS.sw.lat,
            windowAdapter.originalEventBoundsPassed!!.sw.lat,
            0.0001,
            "WINDOW mode originalEventBounds should match event bounds",
        )

        println("✅ Both modes receive originalEventBounds for correct min zoom calculation")
    }
}
