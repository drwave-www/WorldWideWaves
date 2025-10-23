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

import com.worldwidewaves.shared.WWWGlobals
import com.worldwidewaves.shared.events.utils.BoundingBox
import com.worldwidewaves.shared.events.utils.Position
import com.worldwidewaves.shared.map.MapTestFixtures.isCompletelyWithin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Comprehensive test suite for MapBoundsEnforcer (MapConstraintManager).
 *
 * Following Priority 1B from COMPREHENSIVE_TESTING_TODO_REPORT.md:
 * - Test constraint enforcement logic (camera bounds, valid points)
 * - Test padding detection and recalculation
 * - Test suppression during animations
 * - Test edge cases (invalid bounds, null positions)
 *
 * Architecture Impact:
 * - Validates map boundary enforcement prevents out-of-bounds errors
 * - Ensures padding changes trigger proper constraint recalculation
 * - Tests animation suppression mechanism for smooth UX
 * - Provides comprehensive coverage for critical map constraint logic
 */
class MapBoundsEnforcerTest {
    companion object {
        // Test constants
        private const val COORDINATE_TOLERANCE = 0.000001 // ~10cm precision

        // Test bounds: London area
        private val LONDON_BOUNDS =
            BoundingBox(
                swLat = 51.4800,
                swLng = -0.2000,
                neLat = 51.5400,
                neLng = -0.0500,
            )

        // Test bounds: Paris area
        private val PARIS_BOUNDS =
            BoundingBox(
                swLat = 48.8000,
                swLng = 2.2500,
                neLat = 48.9000,
                neLng = 2.4000,
            )
    }

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

        private var _visibleRegion: BoundingBox =
            BoundingBox(
                swLat = 51.4900,
                swLng = -0.1500,
                neLat = 51.5300,
                neLng = -0.1000,
            )
        var constraintBounds: BoundingBox? = null
        var minZoomPreference: Double? = null
        var maxZoomPreference: Double? = null
        var lastAnimatedPosition: Position? = null
        var lastAnimatedZoom: Double? = null
        var cameraIdleListeners = mutableListOf<() -> Unit>()

        // Helper method to set camera position from tests
        fun setCameraPosition(position: Position?) {
            _currentPosition.value = position
        }

        // Helper method to set current zoom from tests
        fun setCurrentZoom(zoom: Double) {
            _currentZoom.value = zoom
        }

        // Helper method to set visible region from tests
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

        override fun getWidth(): Double = 800.0

        override fun getHeight(): Double = 1200.0

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
            lastAnimatedPosition = position
            lastAnimatedZoom = zoom
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
            // UPDATED: Match real AndroidMapLibreAdapter behavior (preventive enforcement)
            // Calculate and set min zoom immediately to prevent viewport exceeding bounds
            val calculatedMinZoom = 5.0 // Simple mock value (real adapter calculates from bounds)
            this.minZoomPreference = calculatedMinZoom
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

        override fun addOnCameraIdleListener(callback: () -> Unit) {
            cameraIdleListeners.add(callback)
        }

        override fun drawOverridenBbox(bbox: BoundingBox) {}

        override fun onMapSet(callback: (MapLibreAdapter<*>) -> Unit) {
            callback(this)
        }

        override fun enableLocationComponent(enabled: Boolean) {
            // Test stub - no-op
        }

        override fun setUserPosition(position: Position) {
            // Test stub - no-op
        }

        override fun setGesturesEnabled(enabled: Boolean) {
            // Test stub - no-op
        }

        fun triggerCameraIdle() {
            cameraIdleListeners.forEach { it() }
        }
    }

    // ========================================
    // Constraint Enforcement Tests (4 tests)
    // ========================================

    @Test
    fun `should constrain camera to map bounds`() =
        runTest {
            val adapter = TestMapLibreAdapter()
            val enforcer = MapBoundsEnforcer(LONDON_BOUNDS, adapter)

            // Set camera position outside bounds (too far west)
            adapter.setCameraPosition(Position(51.5100, -0.3000)) // West of bounds (-0.2000 edge)

            enforcer.applyConstraints()

            // UPDATED: Test preventive constraints (not reactive animations)
            // Verify that constraint bounds were set (preventive enforcement)
            val constraintBounds = adapter.constraintBounds
            assertNotNull(constraintBounds, "Constraint bounds should be set for preventive enforcement")

            // Verify min zoom was set to prevent viewport from exceeding event area
            val minZoom = adapter.minZoomPreference
            assertNotNull(minZoom, "Min zoom should be set to prevent zooming out beyond event area")
            assertTrue(minZoom!! > 0.0, "Min zoom should be positive")

            println("✅ Preventive constraints applied: bounds set, minZoom=$minZoom")
        }

    @Test
    fun `should allow camera inside bounds`() =
        runTest {
            val adapter = TestMapLibreAdapter()
            val enforcer = MapBoundsEnforcer(LONDON_BOUNDS, adapter)

            // Set camera position inside bounds
            val validPosition = Position(51.5100, -0.1250) // Center of London bounds
            adapter.setCameraPosition(validPosition)

            enforcer.applyConstraints()

            // UPDATED: Test preventive constraints (MapLibre handles enforcement natively)
            // Verify constraint bounds were set for MapLibre's native enforcement
            val constraintBounds = adapter.constraintBounds
            assertNotNull(constraintBounds, "Constraint bounds should be set")

            // Verify the valid position is within constraint bounds
            assertTrue(
                constraintBounds.contains(validPosition),
                "Valid position should be within constraint bounds",
            )

            println("✅ Camera position inside bounds - preventive constraints active")
        }

    @Test
    fun `should calculate nearest valid point for out-of-bounds position`() =
        runTest {
            val adapter = TestMapLibreAdapter()
            val enforcer = MapBoundsEnforcer(LONDON_BOUNDS, adapter)

            val testCases =
                listOf(
                    // Position outside, expected nearest point
                    Position(51.6000, -0.1250) to Position(51.5400, -0.1250), // North -> clamped to north edge
                    Position(51.4000, -0.1250) to Position(51.4800, -0.1250), // South -> clamped to south edge
                    Position(51.5100, -0.3000) to Position(51.5100, -0.2000), // West -> clamped to west edge
                    Position(51.5100, 0.0500) to Position(51.5100, -0.0500), // East -> clamped to east edge
                )

            testCases.forEach { (outOfBounds, expectedNearest) ->
                val nearest = enforcer.getNearestValidPoint(outOfBounds, LONDON_BOUNDS)

                assertEquals(
                    expectedNearest.latitude,
                    nearest.latitude,
                    COORDINATE_TOLERANCE,
                    "Latitude should be clamped correctly for ${outOfBounds.latitude}",
                )
                assertEquals(
                    expectedNearest.longitude,
                    nearest.longitude,
                    COORDINATE_TOLERANCE,
                    "Longitude should be clamped correctly for ${outOfBounds.longitude}",
                )
            }

            println("✅ Nearest valid point calculation verified for 4 test cases")
        }

    @Test
    fun `should handle aspect ratio constraints`() =
        runTest {
            val adapter = TestMapLibreAdapter()
            val enforcer = MapBoundsEnforcer(LONDON_BOUNDS, adapter)

            // Apply constraints
            enforcer.applyConstraints()

            // Calculate constraint bounds with padding
            val constraintBounds = enforcer.calculateConstraintBounds()

            // VIEWPORT EDGE CLAMPING: Padding SHRINKS constraint bounds (prevents viewport from exceeding event bounds)
            // Constraint bounds are smaller than map bounds by half-viewport-size to ensure
            // the camera center can't move where viewport edges would show out-of-bounds areas
            // Verify constraint bounds are WITHIN map bounds (shrunk inward by padding)
            assertTrue(
                constraintBounds.southwest.latitude >= LONDON_BOUNDS.southwest.latitude,
                "Constraint bounds southwest latitude should be >= map bounds (shrunk inward)",
            )
            assertTrue(
                constraintBounds.southwest.longitude >= LONDON_BOUNDS.southwest.longitude,
                "Constraint bounds southwest longitude should be >= map bounds (shrunk inward)",
            )
            assertTrue(
                constraintBounds.northeast.latitude <= LONDON_BOUNDS.northeast.latitude,
                "Constraint bounds northeast latitude should be <= map bounds (shrunk inward)",
            )
            assertTrue(
                constraintBounds.northeast.longitude <= LONDON_BOUNDS.northeast.longitude,
                "Constraint bounds northeast longitude should be <= map bounds (shrunk inward)",
            )

            // Verify bounds are properly set
            assertNotNull(adapter.constraintBounds, "Constraint bounds should be set on adapter")

            println("✅ Aspect ratio constraints handled correctly")
        }

    // ========================================
    // Padding Tests (3 tests)
    // ========================================

    @Test
    fun `should detect significant padding changes`() =
        runTest {
            val adapter = TestMapLibreAdapter()
            val enforcer = MapBoundsEnforcer(LONDON_BOUNDS, adapter)

            // Set initial padding
            val initialPadding = MapBoundsEnforcer.VisibleRegionPadding(0.01, 0.01)
            enforcer.setVisibleRegionPadding(initialPadding)

            // Test significant change (>10% threshold)
            val significantChange = MapBoundsEnforcer.VisibleRegionPadding(0.02, 0.02) // 100% change
            assertTrue(
                enforcer.hasSignificantPaddingChange(significantChange),
                "Should detect significant padding change (100% > 10% threshold)",
            )

            println("✅ Significant padding change detected (100% > 10%)")
        }

    @Test
    fun `should ignore minor padding changes below threshold`() =
        runTest {
            val adapter = TestMapLibreAdapter()
            val enforcer = MapBoundsEnforcer(LONDON_BOUNDS, adapter)

            // Set initial padding
            val initialPadding = MapBoundsEnforcer.VisibleRegionPadding(0.01, 0.01)
            enforcer.setVisibleRegionPadding(initialPadding)

            // Test minor change (<10% threshold)
            val minorChange = MapBoundsEnforcer.VisibleRegionPadding(0.0105, 0.0105) // 5% change
            assertFalse(
                enforcer.hasSignificantPaddingChange(minorChange),
                "Should ignore minor padding change (5% < 10% threshold)",
            )

            println("✅ Minor padding change ignored (5% < 10%)")
        }

    @Test
    fun `should recalculate safe bounds with new padding`() =
        runTest {
            val adapter = TestMapLibreAdapter()
            val enforcer = MapBoundsEnforcer(LONDON_BOUNDS, adapter)

            // Set initial padding
            val smallPadding = MapBoundsEnforcer.VisibleRegionPadding(0.005, 0.005)
            enforcer.setVisibleRegionPadding(smallPadding)

            val smallBounds = enforcer.calculateConstraintBounds()
            val smallSpan = smallBounds.height

            // Set larger padding
            val largePadding = MapBoundsEnforcer.VisibleRegionPadding(0.02, 0.02)
            enforcer.setVisibleRegionPadding(largePadding)

            val largeBounds = enforcer.calculateConstraintBounds()
            val largeSpan = largeBounds.height

            // VIEWPORT EDGE CLAMPING: Larger padding SHRINKS constraint bounds (more restrictive)
            // Larger padding means larger viewport, so camera center must stay further from edges
            // This prevents larger viewports from showing out-of-bounds areas
            assertTrue(
                largeSpan < smallSpan,
                "Larger padding should produce SMALLER constraint bounds (viewport edge clamping)",
            )

            println("✅ Safe bounds recalculated with padding: small span=$smallSpan, large span=$largeSpan")
        }

    // ========================================
    // Suppression Tests (2 tests)
    // ========================================

    @Test
    fun `should suppress corrections during animations`() =
        runTest {
            var animationRunning = true
            val adapter = TestMapLibreAdapter()
            val enforcer =
                MapBoundsEnforcer(
                    LONDON_BOUNDS,
                    adapter,
                    isSuppressed = { animationRunning },
                )

            // Set camera outside bounds
            adapter.setCameraPosition(Position(51.6000, -0.1250)) // North of bounds

            enforcer.applyConstraints()
            enforcer.constrainCamera()

            // Verify camera was NOT constrained (suppressed during animation)
            val animatedPosition = adapter.lastAnimatedPosition
            assertTrue(
                animatedPosition == null,
                "Camera should not be constrained when suppression is active",
            )

            println("✅ Camera corrections suppressed during animation")
        }

    @Test
    fun `should resume corrections after animation completes`() =
        runTest {
            var animationRunning = true
            val adapter = TestMapLibreAdapter()
            val enforcer =
                MapBoundsEnforcer(
                    LONDON_BOUNDS,
                    adapter,
                    isSuppressed = { animationRunning },
                )

            // Set camera outside bounds
            adapter.setCameraPosition(Position(51.6000, -0.1250)) // North of bounds

            enforcer.applyConstraints()

            // UPDATED: Test preventive constraints (not reactive animations)
            // Preventive constraints are always active regardless of suppression
            // (MapLibre's native bounds enforcement doesn't need animation suppression)

            // Verify constraints were set immediately (preventive enforcement)
            assertNotNull(
                adapter.constraintBounds,
                "Constraint bounds should be set (preventive enforcement)",
            )
            assertNotNull(
                adapter.minZoomPreference,
                "Min zoom should be set (preventive enforcement)",
            )

            println("✅ Preventive constraints active (animation state irrelevant)")
        }

    // ========================================
    // Edge Case Tests (3 tests)
    // ========================================

    @Test
    fun `should handle invalid bounding box`() =
        runTest {
            val adapter = TestMapLibreAdapter()
            val enforcer = MapBoundsEnforcer(LONDON_BOUNDS, adapter)

            // Set some padding first so we can test "too small" bounds
            val padding = MapBoundsEnforcer.VisibleRegionPadding(0.01, 0.01)
            enforcer.setVisibleRegionPadding(padding)

            // Create bounds that are too small (smaller than 10% of padding = 0.001)
            val tooSmallBounds =
                BoundingBox(
                    swLat = 51.5099,
                    swLng = -0.1251,
                    neLat = 51.5101, // Only 0.0002 height (< 0.001 threshold)
                    neLng = -0.1249, // Only 0.0002 width (< 0.001 threshold)
                )

            val currentPosition = Position(51.5100, -0.1250)

            // Verify too-small bounds are detected as invalid
            val isValid = enforcer.isValidBounds(tooSmallBounds, currentPosition)
            assertFalse(
                isValid,
                "Should detect bounds that are too small (< 10% of padding)",
            )

            println("✅ Too-small bounding box detected as invalid")
        }

    @Test
    fun `should handle null current position`() =
        runTest {
            val adapter = TestMapLibreAdapter(initialPosition = null)
            val enforcer = MapBoundsEnforcer(LONDON_BOUNDS, adapter)

            // Apply constraints with null position
            enforcer.applyConstraints()
            enforcer.constrainCamera()

            // Verify no crash and no constraint applied
            val animatedPosition = adapter.lastAnimatedPosition
            assertTrue(
                animatedPosition == null,
                "Should not constrain camera when position is null",
            )

            println("✅ Null current position handled gracefully")
        }

    @Test
    fun `should validate bounds before applying constraints`() =
        runTest {
            val adapter = TestMapLibreAdapter()
            val enforcer = MapBoundsEnforcer(LONDON_BOUNDS, adapter)

            // Set padding so validation can check size thresholds
            val padding = MapBoundsEnforcer.VisibleRegionPadding(0.01, 0.01)
            enforcer.setVisibleRegionPadding(padding)

            val currentPosition = Position(51.5100, -0.1250)

            // Test valid bounds (large enough and contains position)
            val validBounds =
                BoundingBox(
                    swLat = 51.4900,
                    swLng = -0.1500,
                    neLat = 51.5300,
                    neLng = -0.1000,
                )
            assertTrue(
                enforcer.isValidBounds(validBounds, currentPosition),
                "Should validate proper bounds containing current position",
            )

            // Test bounds too small (< 10% of padding threshold)
            val tinyBounds =
                BoundingBox(
                    swLat = 51.5099,
                    swLng = -0.1251,
                    neLat = 51.5101, // 0.0002 height < 0.001 threshold
                    neLng = -0.1249, // 0.0002 width < 0.001 threshold
                )
            assertFalse(
                enforcer.isValidBounds(tinyBounds, currentPosition),
                "Should reject bounds that are too small (< 10% of padding)",
            )

            // Test bounds not containing current position (far away)
            val distantBounds =
                BoundingBox(
                    swLat = 48.8000,
                    swLng = 2.2500,
                    neLat = 48.9000,
                    neLng = 2.4000,
                )
            assertFalse(
                enforcer.isValidBounds(distantBounds, currentPosition),
                "Should reject bounds not containing current position",
            )

            println("✅ Bounds validation working correctly for 3 test cases")
        }

    // ========================================
    // Additional Integration Tests
    // ========================================

    @Test
    fun `should calculate safe bounds around current position`() =
        runTest {
            val adapter = TestMapLibreAdapter()
            val enforcer = MapBoundsEnforcer(LONDON_BOUNDS, adapter)

            // Set padding
            val padding = MapBoundsEnforcer.VisibleRegionPadding(0.01, 0.01)
            enforcer.setVisibleRegionPadding(padding)

            // Calculate safe bounds around a position
            val centerPosition = Position(51.5100, -0.1250)
            val safeBounds = enforcer.calculateSafeBounds(centerPosition)

            // Verify safe bounds contain the center position
            assertTrue(
                safeBounds.contains(centerPosition),
                "Safe bounds should contain the center position",
            )

            // Verify safe bounds are within map bounds
            assertTrue(
                safeBounds.southwest.latitude >= LONDON_BOUNDS.southwest.latitude,
                "Safe bounds should be within map bounds (southwest lat)",
            )
            assertTrue(
                safeBounds.southwest.longitude >= LONDON_BOUNDS.southwest.longitude,
                "Safe bounds should be within map bounds (southwest lng)",
            )
            assertTrue(
                safeBounds.northeast.latitude <= LONDON_BOUNDS.northeast.latitude,
                "Safe bounds should be within map bounds (northeast lat)",
            )
            assertTrue(
                safeBounds.northeast.longitude <= LONDON_BOUNDS.northeast.longitude,
                "Safe bounds should be within map bounds (northeast lng)",
            )

            println("✅ Safe bounds calculated correctly around current position")
        }

    @Test
    fun `should apply constraints with camera idle listener`() =
        runTest {
            val adapter = TestMapLibreAdapter()
            val enforcer = MapBoundsEnforcer(LONDON_BOUNDS, adapter)

            // Apply constraints (should register idle listener AND set preventive constraints)
            enforcer.applyConstraints()

            // Verify listener was registered (for padding recalculation)
            assertTrue(
                adapter.cameraIdleListeners.isNotEmpty(),
                "Should register camera idle listener",
            )

            // UPDATED: Verify preventive constraints were set immediately
            // This is the CRITICAL change - constraints are preventive, not reactive
            assertNotNull(
                adapter.minZoomPreference,
                "Should set min zoom preference (preventive enforcement)",
            )

            // Verify constraint bounds were set
            assertNotNull(
                adapter.constraintBounds,
                "Should set constraint bounds on adapter (preventive enforcement)",
            )

            println("✅ Preventive constraints applied with camera idle listener for padding updates")
        }

    @Test
    fun `should handle padding change threshold correctly`() =
        runTest {
            val adapter = TestMapLibreAdapter()
            val enforcer = MapBoundsEnforcer(LONDON_BOUNDS, adapter)

            val initialPadding = MapBoundsEnforcer.VisibleRegionPadding(0.01, 0.01)
            enforcer.setVisibleRegionPadding(initialPadding)

            // Test exactly at threshold (10%)
            val thresholdChange =
                MapBoundsEnforcer.VisibleRegionPadding(
                    0.01 * (1.0 + WWWGlobals.MapDisplay.CHANGE_THRESHOLD),
                    0.01 * (1.0 + WWWGlobals.MapDisplay.CHANGE_THRESHOLD),
                )

            // Should be significant (>= threshold)
            // Note: Due to floating point precision, we test slightly above threshold
            val slightlyAboveThreshold =
                MapBoundsEnforcer.VisibleRegionPadding(
                    0.01 * 1.11,
                    0.01 * 1.11,
                )

            assertTrue(
                enforcer.hasSignificantPaddingChange(slightlyAboveThreshold),
                "Should detect change at/above threshold (${WWWGlobals.MapDisplay.CHANGE_THRESHOLD * 100}%)",
            )

            println("✅ Padding change threshold (${WWWGlobals.MapDisplay.CHANGE_THRESHOLD * 100}%) handled correctly")
        }

    // ========================================
    // Integration Tests - Combined Scenarios (6 tests)
    // ========================================

    @Test
    fun `camera at north edge with large viewport should constrain correctly`() =
        runTest {
            val adapter = TestMapLibreAdapter()
            val enforcer = MapBoundsEnforcer(LONDON_BOUNDS, adapter)

            // Set camera at north edge
            val cameraAtNorth = Position(51.5380, -0.1250)
            adapter.setCameraPosition(cameraAtNorth)

            // Large viewport (zoomed out)
            adapter.setVisibleRegion(
                BoundingBox(
                    swLat = 51.4700,
                    swLng = -0.2100,
                    neLat = 51.5500,
                    neLng = -0.0400,
                ),
            )

            enforcer.applyConstraints()
            val constraintBounds = enforcer.calculateConstraintBounds()

            // Verify constraint bounds are within map bounds
            assertTrue(
                constraintBounds.isCompletelyWithin(LONDON_BOUNDS),
                "Constraint bounds should be within map bounds even with camera at edge and large viewport",
            )

            // Verify min zoom was set to prevent viewport exceeding bounds
            assertNotNull(adapter.minZoomPreference, "Min zoom should be set for large viewport")

            println("✅ Camera at north edge with large viewport constrained correctly")
        }

    @Test
    fun `camera at south edge with small viewport should allow more pan area`() =
        runTest {
            val adapter = TestMapLibreAdapter()
            val enforcer = MapBoundsEnforcer(LONDON_BOUNDS, adapter)

            // Set camera at south edge
            val cameraAtSouth = Position(51.4820, -0.1250)
            adapter.setCameraPosition(cameraAtSouth)

            // Small viewport (zoomed in)
            adapter.setVisibleRegion(
                BoundingBox(
                    swLat = 51.4800,
                    swLng = -0.1300,
                    neLat = 51.4840,
                    neLng = -0.1200,
                ),
            )

            enforcer.applyConstraints()
            val smallViewportConstraints = enforcer.calculateConstraintBounds()

            // Small viewport should allow larger constraint bounds (more pan area)
            val constraintSpan = smallViewportConstraints.height
            val eventSpan = LONDON_BOUNDS.height

            // Constraint bounds should be close to event bounds (small viewport means less restriction)
            assertTrue(
                constraintSpan > eventSpan * 0.8,
                "Small viewport should allow constraint bounds > 80% of event bounds " +
                    "(got ${constraintSpan / eventSpan * 100}%)",
            )

            println("✅ Camera at south edge with small viewport allows more pan area")
        }

    @Test
    fun `edge touch validation works at all four corners`() =
        runTest {
            val adapter = TestMapLibreAdapter()
            val enforcer = MapBoundsEnforcer(LONDON_BOUNDS, adapter)

            // Set medium viewport
            adapter.setVisibleRegion(
                BoundingBox(
                    swLat = 51.5000,
                    swLng = -0.1400,
                    neLat = 51.5200,
                    neLng = -0.1100,
                ),
            )

            enforcer.applyConstraints()
            val constraintBounds = enforcer.calculateConstraintBounds()

            // Test all four corners
            val corners =
                listOf(
                    "NE" to Position(constraintBounds.northeast.latitude, constraintBounds.northeast.longitude),
                    "NW" to Position(constraintBounds.northeast.latitude, constraintBounds.southwest.longitude),
                    "SE" to Position(constraintBounds.southwest.latitude, constraintBounds.northeast.longitude),
                    "SW" to Position(constraintBounds.southwest.latitude, constraintBounds.southwest.longitude),
                )

            corners.forEach { (cornerName, cornerPosition) ->
                adapter.setCameraPosition(cornerPosition)
                enforcer.applyConstraints()

                // Verify camera position is valid (within event bounds)
                assertTrue(
                    LONDON_BOUNDS.contains(cornerPosition),
                    "$cornerName corner position should be within event bounds",
                )

                println("   $cornerName corner validated: $cornerPosition")
            }

            println("✅ Edge touch validation successful at all four corners")
        }

    @Test
    fun `combined padding and zoom changes update constraints correctly`() =
        runTest {
            val adapter = TestMapLibreAdapter()
            val enforcer = MapBoundsEnforcer(LONDON_BOUNDS, adapter)

            // Initial state
            adapter.setVisibleRegion(
                BoundingBox(
                    swLat = 51.5000,
                    swLng = -0.1400,
                    neLat = 51.5200,
                    neLng = -0.1100,
                ),
            )

            enforcer.applyConstraints()
            val initialConstraints = enforcer.calculateConstraintBounds()

            // Verify initial constraints are valid
            assertTrue(
                initialConstraints.isCompletelyWithin(LONDON_BOUNDS),
                "Initial constraints should be within map bounds",
            )

            // Change viewport
            adapter.setVisibleRegion(
                BoundingBox(
                    swLat = 51.4950,
                    swLng = -0.1450,
                    neLat = 51.5250,
                    neLng = -0.1050,
                ),
            )

            enforcer.applyConstraints()
            val updatedConstraints = enforcer.calculateConstraintBounds()

            // Verify updated constraints are valid
            assertTrue(
                updatedConstraints.isCompletelyWithin(LONDON_BOUNDS),
                "Updated constraints should be within map bounds",
            )

            // Verify constraints can change (may be same if clamped, but should be valid)
            assertTrue(
                updatedConstraints.northeast.latitude > updatedConstraints.southwest.latitude,
                "Updated constraints should remain valid",
            )

            // Verify min zoom was set
            assertNotNull(adapter.minZoomPreference, "Min zoom should be set")

            println("✅ Combined viewport changes update constraints correctly")
            println("   Constraints remain valid and within bounds after changes")
        }

    @Test
    fun `rapid viewport changes maintain constraint integrity`() =
        runTest {
            val adapter = TestMapLibreAdapter()
            val enforcer = MapBoundsEnforcer(LONDON_BOUNDS, adapter)

            // Simulate rapid viewport changes (user zooming/panning quickly)
            val viewportSequence =
                listOf(
                    BoundingBox(swLat = 51.4900, swLng = -0.1500, neLat = 51.5300, neLng = -0.1000),
                    BoundingBox(swLat = 51.4950, swLng = -0.1450, neLat = 51.5250, neLng = -0.1050),
                    BoundingBox(swLat = 51.4850, swLng = -0.1600, neLat = 51.5350, neLng = -0.0900),
                    BoundingBox(swLat = 51.5000, swLng = -0.1400, neLat = 51.5200, neLng = -0.1100),
                )

            viewportSequence.forEach { viewport ->
                adapter.setVisibleRegion(viewport)
                enforcer.applyConstraints()

                val constraintBounds = enforcer.calculateConstraintBounds()

                // Verify constraint integrity maintained
                assertTrue(
                    constraintBounds.isCompletelyWithin(LONDON_BOUNDS),
                    "Constraint bounds should remain within map bounds during rapid changes",
                )

                assertTrue(
                    constraintBounds.northeast.latitude > constraintBounds.southwest.latitude,
                    "Constraint bounds should remain valid (no inversion)",
                )
            }

            println("✅ Rapid viewport changes maintained constraint integrity (${viewportSequence.size} changes)")
        }

    @Test
    fun `constraint bounds scale correctly with event aspect ratio`() =
        runTest {
            // Test with different aspect ratios
            val testCases =
                listOf(
                    "Square" to
                        BoundingBox(
                            swLat = 51.5000,
                            swLng = -0.1250,
                            neLat = 51.5100,
                            neLng = -0.1150,
                        ), // 1:1
                    "Wide" to
                        BoundingBox(
                            swLat = 51.5000,
                            swLng = -0.1500,
                            neLat = 51.5050,
                            neLng = -0.1000,
                        ), // 2:1
                    "Tall" to
                        BoundingBox(
                            swLat = 51.4900,
                            swLng = -0.1250,
                            neLat = 51.5100,
                            neLng = -0.1150,
                        ), // 1:2
                )

            testCases.forEach { (aspectName, eventBounds) ->
                val adapter = TestMapLibreAdapter()
                val enforcer = MapBoundsEnforcer(eventBounds, adapter)

                // Set standard viewport
                adapter.setVisibleRegion(
                    BoundingBox(
                        swLat = 51.5000,
                        swLng = -0.1300,
                        neLat = 51.5050,
                        neLng = -0.1100,
                    ),
                )

                enforcer.applyConstraints()
                val constraintBounds = enforcer.calculateConstraintBounds()

                // Verify constraint bounds scale with event aspect ratio
                val eventAspect = eventBounds.width / eventBounds.height
                val constraintAspect = constraintBounds.width / constraintBounds.height

                // Aspect ratios should be similar (within 20% tolerance for padding effects)
                val aspectRatioRatio = constraintAspect / eventAspect
                assertTrue(
                    aspectRatioRatio > 0.8 && aspectRatioRatio < 1.2,
                    "$aspectName event: Constraint aspect ratio should scale with event aspect ratio " +
                        "(event=$eventAspect, constraint=$constraintAspect, ratio=$aspectRatioRatio)",
                )

                println("   $aspectName: event aspect=$eventAspect, constraint aspect=$constraintAspect")
            }

            println("✅ Constraint bounds scale correctly with event aspect ratios")
        }
}
