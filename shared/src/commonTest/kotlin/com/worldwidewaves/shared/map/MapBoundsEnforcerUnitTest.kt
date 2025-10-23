package com.worldwidewaves.shared.map

/* * Copyright 2025 DrWave
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
 * limitations under the License. */

import com.worldwidewaves.shared.events.utils.BoundingBox
import com.worldwidewaves.shared.events.utils.Position
import com.worldwidewaves.shared.map.MapTestFixtures.STANDARD_EVENT_BOUNDS
import com.worldwidewaves.shared.map.MapTestFixtures.TOLERANCE_POSITION
import com.worldwidewaves.shared.map.MapTestFixtures.center
import com.worldwidewaves.shared.map.MapTestFixtures.isApproximately
import com.worldwidewaves.shared.map.MapTestFixtures.isCompletelyWithin
import com.worldwidewaves.shared.map.MapTestFixtures.isValid
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for MapBoundsEnforcer constraint calculation logic.
 * These tests validate core logic WITHOUT MapLibre dependencies.
 *
 * Test Categories:
 * - Constraint bounds calculation (BOUNDS vs WINDOW mode)
 * - Padding calculation from viewport dimensions
 * - Bounds validation (inverted, invalid, edge cases)
 * - Bounds shrinking for viewport edge clamping
 * - Safe bounds calculation for camera positioning
 */
class MapBoundsEnforcerUnitTest {
    // ============================================================
    // TEST HELPER: MOCK ADAPTER
    // ============================================================

    /**
     * Minimal mock adapter for testing MapBoundsEnforcer logic
     * without real MapLibre dependencies
     */
    private class MockMapLibreAdapter : MapLibreAdapter<Unit> {
        private var _minZoomLevel: Double = 0.0
        private var _maxZoomLevel: Double = 22.0
        var mockVisibleRegion: BoundingBox = STANDARD_EVENT_BOUNDS
        var mockCameraPosition: Position = STANDARD_EVENT_BOUNDS.center()
        var appliedConstraintBounds: BoundingBox? = null

        override val currentPosition = MutableStateFlow<Position?>(null)
        override val currentZoom = MutableStateFlow(15.0)

        override fun setMap(map: Unit) {}

        override fun setStyle(
            stylePath: String,
            callback: () -> Unit?,
        ) {
            callback()
        }

        override fun getWidth(): Double = 1080.0

        override fun getHeight(): Double = 1920.0

        override fun getCameraPosition(): Position = mockCameraPosition

        override fun getVisibleRegion(): BoundingBox = mockVisibleRegion

        override fun setBoundsForCameraTarget(
            constraintBounds: BoundingBox,
            applyZoomSafetyMargin: Boolean,
            originalEventBounds: BoundingBox?,
        ) {
            appliedConstraintBounds = constraintBounds
        }

        override fun getMinZoomLevel(): Double = _minZoomLevel

        override fun setMinZoomPreference(minZoom: Double) {
            _minZoomLevel = minZoom
        }

        override fun setMaxZoomPreference(maxZoom: Double) {
            _maxZoomLevel = maxZoom
        }

        // Unused methods for unit tests
        override fun moveCamera(bounds: BoundingBox) {}

        override fun animateCamera(
            position: Position,
            zoom: Double?,
            callback: MapCameraCallback?,
        ) {}

        override fun animateCameraToBounds(
            bounds: BoundingBox,
            padding: Int,
            callback: MapCameraCallback?,
        ) {}

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

        override fun onMapSet(callback: (MapLibreAdapter<*>) -> Unit) {}

        override fun enableLocationComponent(enabled: Boolean) {}

        override fun setUserPosition(position: Position) {}

        override fun setGesturesEnabled(enabled: Boolean) {}
    }

    // ============================================================
    // BOUNDS MODE TESTS (Event Detail / Wave Screens)
    // ============================================================

    @Test
    fun testBoundsMode_zeroPadding_constraintBoundsEqualEventBounds() {
        val adapter = MockMapLibreAdapter()
        adapter.mockVisibleRegion = STANDARD_EVENT_BOUNDS

        val enforcer =
            MapBoundsEnforcer(
                mapBounds = STANDARD_EVENT_BOUNDS,
                mapLibreAdapter = adapter,
                isWindowMode = false,
            )

        val constraintBounds = enforcer.calculateConstraintBounds()

        // BOUNDS mode: zero padding, so constraint bounds = event bounds
        assertTrue(
            constraintBounds.isApproximately(STANDARD_EVENT_BOUNDS, TOLERANCE_POSITION),
            "BOUNDS mode should have constraint bounds equal to event bounds",
        )
    }

    @Test
    fun testBoundsMode_validBoundsCheck() {
        val adapter = MockMapLibreAdapter()
        val enforcer =
            MapBoundsEnforcer(
                mapBounds = STANDARD_EVENT_BOUNDS,
                mapLibreAdapter = adapter,
                isWindowMode = false,
            )

        val constraintBounds = enforcer.calculateConstraintBounds()

        assertTrue(constraintBounds.isValid(), "Constraint bounds should be valid")
        assertTrue(
            constraintBounds.northeast.latitude > constraintBounds.southwest.latitude,
            "NE latitude should be > SW latitude",
        )
        assertTrue(
            constraintBounds.northeast.longitude > constraintBounds.southwest.longitude,
            "NE longitude should be > SW longitude",
        )
    }

    // ============================================================
    // WINDOW MODE TESTS (Full Map Screen)
    // ============================================================

    @Test
    fun testWindowMode_viewportPadding_shrinksBounds() {
        val adapter = MockMapLibreAdapter()

        // Simulate viewport smaller than event bounds
        val viewportHalfHeight = 0.002 // ~220 meters
        val viewportHalfWidth = 0.002

        adapter.mockVisibleRegion =
            BoundingBox.fromCorners(
                Position(
                    STANDARD_EVENT_BOUNDS.center().latitude - viewportHalfHeight,
                    STANDARD_EVENT_BOUNDS.center().longitude - viewportHalfWidth,
                ),
                Position(
                    STANDARD_EVENT_BOUNDS.center().latitude + viewportHalfHeight,
                    STANDARD_EVENT_BOUNDS.center().longitude + viewportHalfWidth,
                ),
            )

        val enforcer =
            MapBoundsEnforcer(
                mapBounds = STANDARD_EVENT_BOUNDS,
                mapLibreAdapter = adapter,
                isWindowMode = true,
            )

        // Apply constraints to calculate padding
        enforcer.applyConstraints()

        val constraintBounds = enforcer.calculateConstraintBounds()

        // WINDOW mode uses viewport-based padding (half viewport size)
        // Constraint bounds should be smaller than event bounds (shrunk by half viewport)
        val expectedPaddingLat = viewportHalfHeight
        val expectedPaddingLng = viewportHalfWidth

        // Constraint bounds = event bounds shrunk by padding
        val expectedBounds =
            BoundingBox.fromCorners(
                Position(
                    STANDARD_EVENT_BOUNDS.sw.lat + expectedPaddingLat,
                    STANDARD_EVENT_BOUNDS.sw.lng + expectedPaddingLng,
                ),
                Position(
                    STANDARD_EVENT_BOUNDS.ne.lat - expectedPaddingLat,
                    STANDARD_EVENT_BOUNDS.ne.lng - expectedPaddingLng,
                ),
            )

        assertTrue(
            constraintBounds.isApproximately(expectedBounds, TOLERANCE_POSITION),
            "WINDOW mode should shrink bounds by half viewport size for camera center constraints",
        )
        assertTrue(
            constraintBounds.isCompletelyWithin(STANDARD_EVENT_BOUNDS),
            "Constraint bounds should be within event bounds",
        )
    }

    @Test
    fun testWindowMode_viewportLargerThanEvent_clampsPadding() {
        val adapter = MockMapLibreAdapter()

        // Simulate viewport LARGER than event bounds (zoomed out)
        val viewportHalfHeight = 0.02 // Much larger than event
        val viewportHalfWidth = 0.02

        adapter.mockVisibleRegion =
            BoundingBox.fromCorners(
                Position(
                    STANDARD_EVENT_BOUNDS.center().latitude - viewportHalfHeight,
                    STANDARD_EVENT_BOUNDS.center().longitude - viewportHalfWidth,
                ),
                Position(
                    STANDARD_EVENT_BOUNDS.center().latitude + viewportHalfHeight,
                    STANDARD_EVENT_BOUNDS.center().longitude + viewportHalfWidth,
                ),
            )

        val enforcer =
            MapBoundsEnforcer(
                mapBounds = STANDARD_EVENT_BOUNDS,
                mapLibreAdapter = adapter,
                isWindowMode = true,
            )

        val constraintBounds = enforcer.calculateConstraintBounds()

        // Padding should be clamped to 49% of event size to prevent invalid bounds
        assertTrue(constraintBounds.isValid(), "Constraint bounds should remain valid")
        assertTrue(
            (constraintBounds.ne.lng - constraintBounds.sw.lng) > 0.0,
            "Constraint bounds should have positive width even with large viewport",
        )
        assertTrue(
            (constraintBounds.ne.lat - constraintBounds.sw.lat) > 0.0,
            "Constraint bounds should have positive height even with large viewport",
        )
    }

    @Test
    fun testWindowMode_paddingClampingPreventsInversion() {
        val adapter = MockMapLibreAdapter()

        // Extreme case: viewport much larger than event
        val viewportHalfHeight = 0.1 // 10x larger than event
        val viewportHalfWidth = 0.1

        adapter.mockVisibleRegion =
            BoundingBox.fromCorners(
                Position(
                    STANDARD_EVENT_BOUNDS.center().latitude - viewportHalfHeight,
                    STANDARD_EVENT_BOUNDS.center().longitude - viewportHalfWidth,
                ),
                Position(
                    STANDARD_EVENT_BOUNDS.center().latitude + viewportHalfHeight,
                    STANDARD_EVENT_BOUNDS.center().longitude + viewportHalfWidth,
                ),
            )

        val enforcer =
            MapBoundsEnforcer(
                mapBounds = STANDARD_EVENT_BOUNDS,
                mapLibreAdapter = adapter,
                isWindowMode = true,
            )

        val constraintBounds = enforcer.calculateConstraintBounds()

        // Should NOT invert bounds (SW > NE)
        assertTrue(
            constraintBounds.northeast.latitude > constraintBounds.southwest.latitude,
            "NE latitude must be > SW latitude (no inversion)",
        )
        assertTrue(
            constraintBounds.northeast.longitude > constraintBounds.southwest.longitude,
            "NE longitude must be > SW longitude (no inversion)",
        )
    }

    // ============================================================
    // BOUNDS VALIDATION TESTS
    // ============================================================

    @Test
    fun testIsValidBounds_validBounds_returnsTrue() {
        val adapter = MockMapLibreAdapter()

        // Set up visible region - use a small viewport so padding is small
        val testPosition = STANDARD_EVENT_BOUNDS.center()
        adapter.mockVisibleRegion =
            BoundingBox.fromCorners(
                Position(testPosition.latitude - 0.001, testPosition.longitude - 0.001),
                Position(testPosition.latitude + 0.001, testPosition.longitude + 0.001),
            )

        val enforcer =
            MapBoundsEnforcer(
                mapBounds = STANDARD_EVENT_BOUNDS,
                mapLibreAdapter = adapter,
                isWindowMode = false,
            )

        // Apply constraints to initialize padding
        enforcer.applyConstraints()

        // Create valid bounds that contain the test position and have reasonable size
        val validBounds =
            BoundingBox.fromCorners(
                Position(testPosition.latitude - 0.005, testPosition.longitude - 0.005),
                Position(testPosition.latitude + 0.005, testPosition.longitude + 0.005),
            )

        assertTrue(
            enforcer.isValidBounds(validBounds, testPosition),
            "Valid bounds should pass validation",
        )
    }

    @Test
    fun testIsValidBounds_invertedBounds_returnsFalse() {
        val adapter = MockMapLibreAdapter()
        val enforcer =
            MapBoundsEnforcer(
                mapBounds = STANDARD_EVENT_BOUNDS,
                mapLibreAdapter = adapter,
                isWindowMode = false,
            )

        // Inverted bounds (NE < SW)
        val invertedBounds =
            BoundingBox.fromCorners(
                Position(48.87, 2.37),
                Position(48.86, 2.36),
            )

        assertFalse(
            enforcer.isValidBounds(invertedBounds, STANDARD_EVENT_BOUNDS.center()),
            "Inverted bounds should fail validation",
        )
    }

    @Test
    fun testIsValidBounds_nullPosition_returnsFalse() {
        val adapter = MockMapLibreAdapter()
        val enforcer =
            MapBoundsEnforcer(
                mapBounds = STANDARD_EVENT_BOUNDS,
                mapLibreAdapter = adapter,
                isWindowMode = false,
            )

        val validBounds =
            BoundingBox.fromCorners(
                Position(48.86, 2.36),
                Position(48.87, 2.37),
            )

        assertFalse(
            enforcer.isValidBounds(validBounds, null),
            "Null position should fail validation",
        )
    }

    // ============================================================
    // SAFE BOUNDS CALCULATION TESTS
    // ============================================================

    @Test
    fun testCalculateSafeBounds_centerPosition_returnsValidBounds() {
        val adapter = MockMapLibreAdapter()

        // Set up visible region padding
        adapter.mockVisibleRegion =
            BoundingBox.fromCorners(
                Position(48.86, 2.36),
                Position(48.862, 2.362),
            )

        val enforcer =
            MapBoundsEnforcer(
                mapBounds = STANDARD_EVENT_BOUNDS,
                mapLibreAdapter = adapter,
                isWindowMode = true,
            )

        enforcer.setVisibleRegionPadding(
            MapBoundsEnforcer.VisibleRegionPadding(0.001, 0.001),
        )

        val safeBounds = enforcer.calculateSafeBounds(STANDARD_EVENT_BOUNDS.center())

        assertTrue(safeBounds.isValid(), "Safe bounds should be valid")
        assertTrue(
            safeBounds.isCompletelyWithin(STANDARD_EVENT_BOUNDS),
            "Safe bounds should be within event bounds",
        )
    }

    @Test
    fun testCalculateSafeBounds_edgePosition_constrainedToEvent() {
        val adapter = MockMapLibreAdapter()

        adapter.mockVisibleRegion =
            BoundingBox.fromCorners(
                Position(48.86, 2.36),
                Position(48.862, 2.362),
            )

        val enforcer =
            MapBoundsEnforcer(
                mapBounds = STANDARD_EVENT_BOUNDS,
                mapLibreAdapter = adapter,
                isWindowMode = true,
            )

        enforcer.setVisibleRegionPadding(
            MapBoundsEnforcer.VisibleRegionPadding(0.001, 0.001),
        )

        // Position near event edge
        val edgePosition = Position(48.8665, 2.3621) // NE corner

        val safeBounds = enforcer.calculateSafeBounds(edgePosition)

        assertTrue(safeBounds.isValid(), "Safe bounds should be valid")
        assertTrue(
            safeBounds.isCompletelyWithin(STANDARD_EVENT_BOUNDS),
            "Safe bounds should be within event bounds even for edge position",
        )
    }

    // ============================================================
    // NEAREST VALID POINT TESTS
    // ============================================================

    @Test
    fun testGetNearestValidPoint_insideBounds_returnsSamePoint() {
        val adapter = MockMapLibreAdapter()
        val enforcer =
            MapBoundsEnforcer(
                mapBounds = STANDARD_EVENT_BOUNDS,
                mapLibreAdapter = adapter,
                isWindowMode = false,
            )

        val insidePoint = STANDARD_EVENT_BOUNDS.center()
        val nearest = enforcer.getNearestValidPoint(insidePoint, STANDARD_EVENT_BOUNDS)

        assertTrue(
            nearest.isApproximately(insidePoint, TOLERANCE_POSITION),
            "Point already inside bounds should remain unchanged",
        )
    }

    @Test
    fun testGetNearestValidPoint_outsideNorth_clampsToNorthEdge() {
        val adapter = MockMapLibreAdapter()
        val enforcer =
            MapBoundsEnforcer(
                mapBounds = STANDARD_EVENT_BOUNDS,
                mapLibreAdapter = adapter,
                isWindowMode = false,
            )

        val outsidePoint = Position(48.9000, 2.3572) // North of event
        val nearest = enforcer.getNearestValidPoint(outsidePoint, STANDARD_EVENT_BOUNDS)

        assertEquals(
            STANDARD_EVENT_BOUNDS.northeast.latitude,
            nearest.latitude,
            TOLERANCE_POSITION,
            "Latitude should be clamped to north edge",
        )
        assertEquals(
            outsidePoint.longitude,
            nearest.longitude,
            TOLERANCE_POSITION,
            "Longitude should remain unchanged",
        )
    }

    @Test
    fun testGetNearestValidPoint_outsideSouth_clampsToSouthEdge() {
        val adapter = MockMapLibreAdapter()
        val enforcer =
            MapBoundsEnforcer(
                mapBounds = STANDARD_EVENT_BOUNDS,
                mapLibreAdapter = adapter,
                isWindowMode = false,
            )

        val outsidePoint = Position(48.8000, 2.3572) // South of event
        val nearest = enforcer.getNearestValidPoint(outsidePoint, STANDARD_EVENT_BOUNDS)

        assertEquals(
            STANDARD_EVENT_BOUNDS.southwest.latitude,
            nearest.latitude,
            TOLERANCE_POSITION,
            "Latitude should be clamped to south edge",
        )
    }

    @Test
    fun testGetNearestValidPoint_outsideEast_clampsToEastEdge() {
        val adapter = MockMapLibreAdapter()
        val enforcer =
            MapBoundsEnforcer(
                mapBounds = STANDARD_EVENT_BOUNDS,
                mapLibreAdapter = adapter,
                isWindowMode = false,
            )

        val outsidePoint = Position(48.8616, 2.4000) // East of event
        val nearest = enforcer.getNearestValidPoint(outsidePoint, STANDARD_EVENT_BOUNDS)

        assertEquals(
            STANDARD_EVENT_BOUNDS.northeast.longitude,
            nearest.longitude,
            TOLERANCE_POSITION,
            "Longitude should be clamped to east edge",
        )
    }

    @Test
    fun testGetNearestValidPoint_outsideWest_clampsToWestEdge() {
        val adapter = MockMapLibreAdapter()
        val enforcer =
            MapBoundsEnforcer(
                mapBounds = STANDARD_EVENT_BOUNDS,
                mapLibreAdapter = adapter,
                isWindowMode = false,
            )

        val outsidePoint = Position(48.8616, 2.3000) // West of event
        val nearest = enforcer.getNearestValidPoint(outsidePoint, STANDARD_EVENT_BOUNDS)

        assertEquals(
            STANDARD_EVENT_BOUNDS.southwest.longitude,
            nearest.longitude,
            TOLERANCE_POSITION,
            "Longitude should be clamped to west edge",
        )
    }

    @Test
    fun testGetNearestValidPoint_outsideCorner_clampsToCorner() {
        val adapter = MockMapLibreAdapter()
        val enforcer =
            MapBoundsEnforcer(
                mapBounds = STANDARD_EVENT_BOUNDS,
                mapLibreAdapter = adapter,
                isWindowMode = false,
            )

        val outsidePoint = Position(48.9000, 2.4000) // NE of event
        val nearest = enforcer.getNearestValidPoint(outsidePoint, STANDARD_EVENT_BOUNDS)

        assertEquals(
            STANDARD_EVENT_BOUNDS.northeast.latitude,
            nearest.latitude,
            TOLERANCE_POSITION,
            "Latitude should be clamped to NE corner",
        )
        assertEquals(
            STANDARD_EVENT_BOUNDS.northeast.longitude,
            nearest.longitude,
            TOLERANCE_POSITION,
            "Longitude should be clamped to NE corner",
        )
    }

    // ============================================================
    // DYNAMIC CONSTRAINT BOUNDS TESTS
    // ============================================================

    @Test
    fun testWindowMode_constraintBoundsChangeWithZoom() {
        val adapter = MockMapLibreAdapter()

        // Small viewport (zoomed in)
        adapter.mockVisibleRegion =
            BoundingBox.fromCorners(
                Position(48.860, 2.360),
                Position(48.862, 2.362),
            )

        val enforcer =
            MapBoundsEnforcer(
                mapBounds = STANDARD_EVENT_BOUNDS,
                mapLibreAdapter = adapter,
                isWindowMode = true,
            )

        enforcer.applyConstraints()
        val constraintsWhenZoomedIn = enforcer.calculateConstraintBounds()

        // Larger viewport (zoomed out)
        adapter.mockVisibleRegion =
            BoundingBox.fromCorners(
                Position(48.855, 2.350),
                Position(48.867, 2.372),
            )

        enforcer.setVisibleRegionPadding(
            MapBoundsEnforcer.VisibleRegionPadding(0.006, 0.011),
        )

        val constraintsWhenZoomedOut = enforcer.calculateConstraintBounds()

        // When zoomed in (small viewport), constraint bounds should be larger (more pan area)
        val zoomedInWidth = constraintsWhenZoomedIn.ne.lng - constraintsWhenZoomedIn.sw.lng
        val zoomedOutWidth = constraintsWhenZoomedOut.ne.lng - constraintsWhenZoomedOut.sw.lng
        val zoomedInHeight = constraintsWhenZoomedIn.ne.lat - constraintsWhenZoomedIn.sw.lat
        val zoomedOutHeight = constraintsWhenZoomedOut.ne.lat - constraintsWhenZoomedOut.sw.lat

        assertTrue(
            zoomedInWidth > zoomedOutWidth,
            "Zoomed in (small viewport) should have larger constraint bounds (more pan area)",
        )
        assertTrue(
            zoomedInHeight > zoomedOutHeight,
            "Zoomed in (small viewport) should have larger constraint bounds (more pan area)",
        )
    }

    @Test
    fun testWindowMode_invalidViewportDetection_usesZeroPadding() {
        val adapter = MockMapLibreAdapter()

        // Simulate invalid viewport (>10 degrees - uninitialized map)
        adapter.mockVisibleRegion =
            BoundingBox.fromCorners(
                Position(0.0, 0.0),
                Position(90.0, 180.0), // Absurd viewport (entire hemisphere!)
            )

        val enforcer =
            MapBoundsEnforcer(
                mapBounds = STANDARD_EVENT_BOUNDS,
                mapLibreAdapter = adapter,
                isWindowMode = true,
            )

        val constraintBounds = enforcer.calculateConstraintBounds()

        // Should use zero padding (fallback) for invalid viewport
        // This means constraint bounds ≈ event bounds
        assertTrue(
            constraintBounds.isApproximately(STANDARD_EVENT_BOUNDS, 0.001),
            "Invalid viewport (>10°) should trigger zero padding fallback",
        )
    }

    // ============================================================
    // PADDING CHANGE DETECTION TESTS
    // ============================================================

    @Test
    fun testHasSignificantPaddingChange_smallChange_returnsFalse() {
        val adapter = MockMapLibreAdapter()
        val enforcer =
            MapBoundsEnforcer(
                mapBounds = STANDARD_EVENT_BOUNDS,
                mapLibreAdapter = adapter,
                isWindowMode = true,
            )

        enforcer.setVisibleRegionPadding(
            MapBoundsEnforcer.VisibleRegionPadding(0.001, 0.001),
        )

        // 5% change (below 10% threshold)
        val newPadding = MapBoundsEnforcer.VisibleRegionPadding(0.00105, 0.00105)

        assertFalse(
            enforcer.hasSignificantPaddingChange(newPadding),
            "5% padding change should not be significant",
        )
    }

    @Test
    fun testHasSignificantPaddingChange_largeChange_returnsTrue() {
        val adapter = MockMapLibreAdapter()
        val enforcer =
            MapBoundsEnforcer(
                mapBounds = STANDARD_EVENT_BOUNDS,
                mapLibreAdapter = adapter,
                isWindowMode = true,
            )

        enforcer.setVisibleRegionPadding(
            MapBoundsEnforcer.VisibleRegionPadding(0.001, 0.001),
        )

        // 20% change (above 10% threshold)
        val newPadding = MapBoundsEnforcer.VisibleRegionPadding(0.0012, 0.0012)

        assertTrue(
            enforcer.hasSignificantPaddingChange(newPadding),
            "20% padding change should be significant",
        )
    }

    // ============================================================
    // PADDING VERIFICATION TESTS (Priority 3 - 3 tests)
    // ============================================================

    @Test
    fun testPaddingCalculation_constraintBoundsShrinkByExactPaddingAmount() {
        val adapter = MockMapLibreAdapter()

        // Set specific visible region to control padding calculation
        val viewportHalfHeight = 0.003 // ~330 meters
        val viewportHalfWidth = 0.003

        val centerPosition = STANDARD_EVENT_BOUNDS.center()
        adapter.mockVisibleRegion =
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

        val enforcer =
            MapBoundsEnforcer(
                mapBounds = STANDARD_EVENT_BOUNDS,
                mapLibreAdapter = adapter,
                isWindowMode = true,
            )

        enforcer.applyConstraints()
        val constraintBounds = enforcer.calculateConstraintBounds()

        // Constraint bounds should be event bounds shrunk by half viewport size
        val expectedSwLat = STANDARD_EVENT_BOUNDS.southwest.latitude + viewportHalfHeight
        val expectedSwLng = STANDARD_EVENT_BOUNDS.southwest.longitude + viewportHalfWidth
        val expectedNeLat = STANDARD_EVENT_BOUNDS.northeast.latitude - viewportHalfHeight
        val expectedNeLng = STANDARD_EVENT_BOUNDS.northeast.longitude - viewportHalfWidth

        assertTrue(
            kotlin.math.abs(constraintBounds.southwest.latitude - expectedSwLat) < TOLERANCE_POSITION,
            "SW latitude should be shrunk by viewport half height " +
                "(expected=$expectedSwLat, got=${constraintBounds.southwest.latitude})",
        )
        assertTrue(
            kotlin.math.abs(constraintBounds.southwest.longitude - expectedSwLng) < TOLERANCE_POSITION,
            "SW longitude should be shrunk by viewport half width " +
                "(expected=$expectedSwLng, got=${constraintBounds.southwest.longitude})",
        )
        assertTrue(
            kotlin.math.abs(constraintBounds.northeast.latitude - expectedNeLat) < TOLERANCE_POSITION,
            "NE latitude should be shrunk by viewport half height " +
                "(expected=$expectedNeLat, got=${constraintBounds.northeast.latitude})",
        )
        assertTrue(
            kotlin.math.abs(constraintBounds.northeast.longitude - expectedNeLng) < TOLERANCE_POSITION,
            "NE longitude should be shrunk by viewport half width " +
                "(expected=$expectedNeLng, got=${constraintBounds.northeast.longitude})",
        )

        println("✅ Padding calculation verified: constraint bounds shrink by exact padding amount")
        println("   Viewport half: height=$viewportHalfHeight, width=$viewportHalfWidth")
    }

    @Test
    fun testPaddingSymmetry_constraintBoundsCenteredWithinEventBounds() {
        val adapter = MockMapLibreAdapter()

        // Set viewport with specific padding
        val viewportHalfHeight = 0.002
        val viewportHalfWidth = 0.0015

        val centerPosition = STANDARD_EVENT_BOUNDS.center()
        adapter.mockVisibleRegion =
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

        val enforcer =
            MapBoundsEnforcer(
                mapBounds = STANDARD_EVENT_BOUNDS,
                mapLibreAdapter = adapter,
                isWindowMode = true,
            )

        enforcer.applyConstraints()
        val constraintBounds = enforcer.calculateConstraintBounds()

        // Verify constraint bounds are centered within event bounds
        val eventCenter = STANDARD_EVENT_BOUNDS.center()
        val constraintCenter = constraintBounds.center()

        assertTrue(
            constraintCenter.isApproximately(eventCenter, TOLERANCE_POSITION),
            "Constraint bounds should be centered within event bounds " +
                "(event center=$eventCenter, constraint center=$constraintCenter)",
        )

        // Verify symmetric shrinkage
        val northShrinkage = STANDARD_EVENT_BOUNDS.northeast.latitude - constraintBounds.northeast.latitude
        val southShrinkage = constraintBounds.southwest.latitude - STANDARD_EVENT_BOUNDS.southwest.latitude
        val eastShrinkage = STANDARD_EVENT_BOUNDS.northeast.longitude - constraintBounds.northeast.longitude
        val westShrinkage = constraintBounds.southwest.longitude - STANDARD_EVENT_BOUNDS.southwest.longitude

        assertTrue(
            kotlin.math.abs(northShrinkage - southShrinkage) < TOLERANCE_POSITION,
            "North and south shrinkage should be equal (north=$northShrinkage, south=$southShrinkage)",
        )
        assertTrue(
            kotlin.math.abs(eastShrinkage - westShrinkage) < TOLERANCE_POSITION,
            "East and west shrinkage should be equal (east=$eastShrinkage, west=$westShrinkage)",
        )

        println("✅ Padding symmetry verified: constraint bounds centered with symmetric shrinkage")
        println("   Shrinkage: N/S=$northShrinkage, E/W=$eastShrinkage")
    }

    @Test
    fun testPaddingPreventsInvalidBounds_clampedAt49Percent() {
        val adapter = MockMapLibreAdapter()

        // Set viewport MUCH larger than event (extreme zoom out)
        val viewportHalfHeight = 0.05 // 5x larger than event
        val viewportHalfWidth = 0.05

        val centerPosition = STANDARD_EVENT_BOUNDS.center()
        adapter.mockVisibleRegion =
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

        val enforcer =
            MapBoundsEnforcer(
                mapBounds = STANDARD_EVENT_BOUNDS,
                mapLibreAdapter = adapter,
                isWindowMode = true,
            )

        enforcer.applyConstraints()
        val constraintBounds = enforcer.calculateConstraintBounds()

        // Verify padding was clamped to prevent invalid bounds
        // Padding should be at most 49% of event size (prevents inversion)
        val eventHeight = STANDARD_EVENT_BOUNDS.height
        val eventWidth = STANDARD_EVENT_BOUNDS.width

        val actualHeightShrinkage =
            (STANDARD_EVENT_BOUNDS.northeast.latitude - constraintBounds.northeast.latitude) +
                (constraintBounds.southwest.latitude - STANDARD_EVENT_BOUNDS.southwest.latitude)
        val actualWidthShrinkage =
            (STANDARD_EVENT_BOUNDS.northeast.longitude - constraintBounds.northeast.longitude) +
                (constraintBounds.southwest.longitude - STANDARD_EVENT_BOUNDS.southwest.longitude)

        // Use lenient comparison for floating-point precision (allow 0.1% tolerance)
        val maxHeightShrinkage = eventHeight * 0.98 * 1.001 // 0.1% tolerance
        val maxWidthShrinkage = eventWidth * 0.98 * 1.001

        assertTrue(
            actualHeightShrinkage <= maxHeightShrinkage,
            "Height shrinkage should be clamped to prevent invalid bounds " +
                "(shrinkage=$actualHeightShrinkage, max=${eventHeight * 0.98})",
        )
        assertTrue(
            actualWidthShrinkage <= maxWidthShrinkage,
            "Width shrinkage should be clamped to prevent invalid bounds " +
                "(shrinkage=$actualWidthShrinkage, max=${eventWidth * 0.98})",
        )

        // Verify constraint bounds remain valid (not inverted)
        assertTrue(constraintBounds.isValid(), "Constraint bounds should remain valid despite extreme viewport")
        assertTrue(
            constraintBounds.northeast.latitude > constraintBounds.southwest.latitude,
            "NE latitude should be > SW latitude (no inversion)",
        )
        assertTrue(
            constraintBounds.northeast.longitude > constraintBounds.southwest.longitude,
            "NE longitude should be > SW longitude (no inversion)",
        )

        println("✅ Padding clamping verified: prevents invalid bounds with extreme viewport")
        println("   Viewport half: $viewportHalfHeight (5x event size)")
        println("   Actual shrinkage: height=$actualHeightShrinkage, width=$actualWidthShrinkage")
        println("   Bounds remain valid: ${constraintBounds.isValid()}")
    }
}
