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
import com.worldwidewaves.shared.map.MapTestFixtures.STANDARD_EVENT_BOUNDS
import com.worldwidewaves.shared.map.MapTestFixtures.TOLERANCE_EDGE
import com.worldwidewaves.shared.map.MapTestFixtures.TOLERANCE_POSITION
import com.worldwidewaves.shared.map.MapTestFixtures.center
import com.worldwidewaves.shared.map.MapTestFixtures.isCompletelyWithin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Tests for viewport extending beyond camera constraint bounds.
 *
 * Priority 1 - Critical (3 tests):
 * - Viewport extending beyond bounds at low zoom
 * - Viewport extending beyond bounds at medium zoom
 * - Viewport extending beyond bounds at high zoom
 * - Verify viewport reaches event edges when camera is at constraint edges
 *
 * Purpose:
 * - Validate that when camera is at the constraint edge, the viewport can extend
 *   to the event boundary without showing out-of-bounds areas
 * - Test across different zoom levels (low, medium, high)
 * - Ensure proper coordination between constraint bounds and min zoom calculations
 */
class ViewportExtensionTest {
    /**
     * Mock MapLibreAdapter for testing without real map dependencies
     */
    private class TestMapLibreAdapter(
        initialPosition: Position? = null,
        initialZoom: Double = 13.0,
    ) : MapLibreAdapter<Unit> {
        private val _currentPosition = MutableStateFlow(initialPosition)
        override val currentPosition: StateFlow<Position?> = _currentPosition

        private val _currentZoom = MutableStateFlow(initialZoom)
        override val currentZoom: StateFlow<Double> = _currentZoom

        private var _visibleRegion: BoundingBox = STANDARD_EVENT_BOUNDS
        var constraintBounds: BoundingBox? = null
        var minZoomPreference: Double? = null
        var maxZoomPreference: Double? = null

        fun setCameraPosition(position: Position?) {
            _currentPosition.value = position
        }

        fun setCurrentZoom(zoom: Double) {
            _currentZoom.value = zoom
        }

        fun setVisibleRegion(region: BoundingBox) {
            _visibleRegion = region
        }

        override fun setMap(map: Unit) {}

        override fun setStyle(
            stylePath: String,
            callback: () -> Unit?,
        ) {
            callback()
        }

        override fun getWidth(): Double = PORTRAIT_PHONE.width

        override fun getHeight(): Double = PORTRAIT_PHONE.height

        override fun getCameraPosition(): Position? = _currentPosition.value

        override fun getVisibleRegion(): BoundingBox = _visibleRegion

        override fun moveCamera(bounds: BoundingBox) {
            val center =
                Position(
                    (bounds.southwest.latitude + bounds.northeast.latitude) / 2,
                    (bounds.southwest.longitude + bounds.northeast.longitude) / 2,
                )
            _currentPosition.value = center
        }

        override fun animateCamera(
            position: Position,
            zoom: Double?,
            callback: MapCameraCallback?,
        ) {
            _currentPosition.value = position
            zoom?.let { _currentZoom.value = it }
            callback?.onFinish()
        }

        override fun animateCameraToBounds(
            bounds: BoundingBox,
            padding: Int,
            callback: MapCameraCallback?,
        ) {
            val center =
                Position(
                    (bounds.southwest.latitude + bounds.northeast.latitude) / 2,
                    (bounds.southwest.longitude + bounds.northeast.longitude) / 2,
                )
            _currentPosition.value = center
            callback?.onFinish()
        }

        override fun setBoundsForCameraTarget(
            constraintBounds: BoundingBox,
            applyZoomSafetyMargin: Boolean,
            originalEventBounds: BoundingBox?,
        ) {
            this.constraintBounds = constraintBounds
        }

        override fun getMinZoomLevel(): Double = 5.0

        override fun setMinZoomPreference(minZoom: Double) {
            this.minZoomPreference = minZoom
        }

        override fun setMaxZoomPreference(maxZoom: Double) {
            this.maxZoomPreference = maxZoom
        }

        override fun setAttributionMargins(
            left: Int,
            top: Int,
            right: Int,
            bottom: Int,
        ) {
        }

        override fun addWavePolygons(
            polygons: List<Any>,
            clearExisting: Boolean,
        ) {
        }

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

    // ============================================================
    // VIEWPORT EXTENSION TESTS (Low Zoom)
    // ============================================================

    @Test
    fun `viewport extending beyond bounds at low zoom level`() {
        val adapter = TestMapLibreAdapter()
        val enforcer =
            MapBoundsEnforcer(
                STANDARD_EVENT_BOUNDS,
                adapter,
                isWindowMode = true,
            )

        // Low zoom (zoomed out, large viewport)
        val lowZoom = 11.0
        adapter.setCurrentZoom(lowZoom)

        // Create visible region (large viewport for low zoom)
        // Viewport half-size: ~0.004 degrees (~440 meters)
        val viewportHalfHeight = 0.004
        val viewportHalfWidth = 0.004

        val centerPosition = STANDARD_EVENT_BOUNDS.center()
        val visibleRegion =
            BoundingBox.fromCorners(
                Position(
                    centerPosition.latitude - viewportHalfHeight,
                    centerPosition.longitude - viewportHalfWidth,
                ),
                Position(
                    centerPosition.latitude + viewportHalfHeight,
                    centerPosition.longitude + viewportHalfWidth,
                ),
            )
        adapter.setVisibleRegion(visibleRegion)

        enforcer.applyConstraints()

        val constraintBounds = enforcer.calculateConstraintBounds()

        // Verify constraint bounds are valid and within event bounds
        assertTrue(
            constraintBounds.isCompletelyWithin(STANDARD_EVENT_BOUNDS),
            "Constraint bounds should be within event bounds",
        )

        // When camera is at north constraint edge, viewport should reach event north edge
        // constraint north edge + viewport half height = event north edge
        val northConstraintEdge = constraintBounds.northeast.latitude
        val expectedViewportNorth = northConstraintEdge + viewportHalfHeight

        assertTrue(
            kotlin.math.abs(expectedViewportNorth - STANDARD_EVENT_BOUNDS.northeast.latitude) < TOLERANCE_EDGE,
            "Viewport should reach event north edge when camera is at north constraint edge " +
                "(expected ~${STANDARD_EVENT_BOUNDS.northeast.latitude}, " +
                "got $expectedViewportNorth)",
        )

        println("✅ Low zoom: Viewport extends to event edge correctly")
        println("   Zoom: $lowZoom, Viewport half: $viewportHalfHeight, Constraint height: ${constraintBounds.height}")
    }

    // ============================================================
    // VIEWPORT EXTENSION TESTS (Medium Zoom)
    // ============================================================

    @Test
    fun `viewport extending beyond bounds at medium zoom level`() {
        val adapter = TestMapLibreAdapter()
        val enforcer =
            MapBoundsEnforcer(
                STANDARD_EVENT_BOUNDS,
                adapter,
                isWindowMode = true,
            )

        // Medium zoom (typical viewing zoom)
        val mediumZoom = 14.0
        adapter.setCurrentZoom(mediumZoom)

        // Create visible region (medium viewport for medium zoom)
        // Viewport half-size: ~0.002 degrees (~220 meters)
        val viewportHalfHeight = 0.002
        val viewportHalfWidth = 0.002

        val centerPosition = STANDARD_EVENT_BOUNDS.center()
        val visibleRegion =
            BoundingBox.fromCorners(
                Position(
                    centerPosition.latitude - viewportHalfHeight,
                    centerPosition.longitude - viewportHalfWidth,
                ),
                Position(
                    centerPosition.latitude + viewportHalfHeight,
                    centerPosition.longitude + viewportHalfWidth,
                ),
            )
        adapter.setVisibleRegion(visibleRegion)

        enforcer.applyConstraints()

        val constraintBounds = enforcer.calculateConstraintBounds()

        // Verify constraint bounds are valid and within event bounds
        assertTrue(
            constraintBounds.isCompletelyWithin(STANDARD_EVENT_BOUNDS),
            "Constraint bounds should be within event bounds",
        )

        // When camera is at south constraint edge, viewport should reach event south edge
        // constraint south edge - viewport half height = event south edge
        val southConstraintEdge = constraintBounds.southwest.latitude
        val expectedViewportSouth = southConstraintEdge - viewportHalfHeight

        assertTrue(
            kotlin.math.abs(expectedViewportSouth - STANDARD_EVENT_BOUNDS.southwest.latitude) < TOLERANCE_EDGE,
            "Viewport should reach event south edge when camera is at south constraint edge " +
                "(expected ~${STANDARD_EVENT_BOUNDS.southwest.latitude}, " +
                "got $expectedViewportSouth)",
        )

        println("✅ Medium zoom: Viewport extends to event edge correctly")
        println("   Zoom: $mediumZoom, Viewport half: $viewportHalfHeight, Constraint height: ${constraintBounds.height}")
    }

    // ============================================================
    // VIEWPORT EXTENSION TESTS (High Zoom)
    // ============================================================

    @Test
    fun `viewport extending beyond bounds at high zoom level`() {
        val adapter = TestMapLibreAdapter()
        val enforcer =
            MapBoundsEnforcer(
                STANDARD_EVENT_BOUNDS,
                adapter,
                isWindowMode = true,
            )

        // High zoom (zoomed in, small viewport)
        val highZoom = 16.0
        adapter.setCurrentZoom(highZoom)

        // Create visible region (small viewport for high zoom)
        // Viewport half-size: ~0.001 degrees (~110 meters)
        val viewportHalfHeight = 0.001
        val viewportHalfWidth = 0.001

        val centerPosition = STANDARD_EVENT_BOUNDS.center()
        val visibleRegion =
            BoundingBox.fromCorners(
                Position(
                    centerPosition.latitude - viewportHalfHeight,
                    centerPosition.longitude - viewportHalfWidth,
                ),
                Position(
                    centerPosition.latitude + viewportHalfHeight,
                    centerPosition.longitude + viewportHalfWidth,
                ),
            )
        adapter.setVisibleRegion(visibleRegion)

        enforcer.applyConstraints()

        val constraintBounds = enforcer.calculateConstraintBounds()

        // Verify constraint bounds are valid and within event bounds
        assertTrue(
            constraintBounds.isCompletelyWithin(STANDARD_EVENT_BOUNDS),
            "Constraint bounds should be within event bounds",
        )

        // When camera is at east constraint edge, viewport should reach event east edge
        // constraint east edge + viewport half width = event east edge
        val eastConstraintEdge = constraintBounds.northeast.longitude
        val expectedViewportEast = eastConstraintEdge + viewportHalfWidth

        assertTrue(
            kotlin.math.abs(expectedViewportEast - STANDARD_EVENT_BOUNDS.northeast.longitude) < TOLERANCE_EDGE,
            "Viewport should reach event east edge when camera is at east constraint edge " +
                "(expected ~${STANDARD_EVENT_BOUNDS.northeast.longitude}, " +
                "got $expectedViewportEast)",
        )

        println("✅ High zoom: Viewport extends to event edge correctly")
        println("   Zoom: $highZoom, Viewport half: $viewportHalfWidth, Constraint width: ${constraintBounds.width}")
    }

    // ============================================================
    // COMPREHENSIVE EDGE VERIFICATION TEST
    // ============================================================

    @Test
    fun `viewport reaches all event edges when camera at constraint edges`() {
        val adapter = TestMapLibreAdapter()
        val enforcer =
            MapBoundsEnforcer(
                STANDARD_EVENT_BOUNDS,
                adapter,
                isWindowMode = true,
            )

        val testZoom = 14.0
        adapter.setCurrentZoom(testZoom)

        // Fixed viewport size for all edge tests
        val viewportHalfHeight = 0.002
        val viewportHalfWidth = 0.002

        val centerPosition = STANDARD_EVENT_BOUNDS.center()
        val visibleRegion =
            BoundingBox.fromCorners(
                Position(
                    centerPosition.latitude - viewportHalfHeight,
                    centerPosition.longitude - viewportHalfWidth,
                ),
                Position(
                    centerPosition.latitude + viewportHalfHeight,
                    centerPosition.longitude + viewportHalfWidth,
                ),
            )
        adapter.setVisibleRegion(visibleRegion)

        enforcer.applyConstraints()
        val constraintBounds = enforcer.calculateConstraintBounds()

        // Verify constraint bounds are valid
        assertTrue(
            constraintBounds.isCompletelyWithin(STANDARD_EVENT_BOUNDS),
            "Constraint bounds should be within event bounds",
        )

        // Test all four edges with the same viewport
        val edgeTests =
            mapOf(
                "North" to
                    Pair(
                        constraintBounds.northeast.latitude + viewportHalfHeight,
                        STANDARD_EVENT_BOUNDS.northeast.latitude,
                    ),
                "South" to
                    Pair(
                        constraintBounds.southwest.latitude - viewportHalfHeight,
                        STANDARD_EVENT_BOUNDS.southwest.latitude,
                    ),
                "East" to
                    Pair(
                        constraintBounds.northeast.longitude + viewportHalfWidth,
                        STANDARD_EVENT_BOUNDS.northeast.longitude,
                    ),
                "West" to
                    Pair(
                        constraintBounds.southwest.longitude - viewportHalfWidth,
                        STANDARD_EVENT_BOUNDS.southwest.longitude,
                    ),
            )

        edgeTests.forEach { (edgeName, edgeTest) ->
            val (calculatedEdge, expectedEdge) = edgeTest
            assertTrue(
                kotlin.math.abs(calculatedEdge - expectedEdge) < TOLERANCE_POSITION,
                "$edgeName edge: Viewport should reach event boundary " +
                    "(expected=$expectedEdge, got=$calculatedEdge)",
            )
        }

        println("✅ All edges: Viewport reaches event edges correctly")
        println("   Viewport half: height=$viewportHalfHeight, width=$viewportHalfWidth")
    }
}
