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

import android.content.Context
import android.view.View
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.worldwidewaves.shared.events.utils.BoundingBox
import com.worldwidewaves.shared.events.utils.Position
import com.worldwidewaves.shared.map.AndroidMapLibreAdapter
import com.worldwidewaves.shared.map.MapTestFixtures
import com.worldwidewaves.shared.map.MapTestFixtures.PORTRAIT_PHONE
import com.worldwidewaves.shared.map.MapTestFixtures.STANDARD_EVENT_BOUNDS
import com.worldwidewaves.shared.map.MapTestFixtures.TOLERANCE_POSITION
import com.worldwidewaves.shared.map.MapTestFixtures.center
import com.worldwidewaves.shared.map.MapTestFixtures.isApproximately
import com.worldwidewaves.shared.map.MapTestFixtures.isCompletelyWithin
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Base class for Android MapLibre integration tests using headless MapView.
 *
 * This approach provides:
 * - Real MapLibre map instance (not mocked)
 * - Headless rendering (no UI, faster execution)
 * - Programmatic camera control (no gesture simulation)
 * - Direct API access for validation
 *
 * Usage:
 * ```kotlin
 * @RunWith(AndroidJUnit4::class)
 * class MyMapTest : BaseMapIntegrationTest() {
 *     @Test
 *     fun testSomething() {
 *         // mapView and adapter are ready to use
 *         adapter.animateCamera(Position(48.8566, 2.3522), zoom = 15.0)
 *         waitForIdle()
 *         val visibleRegion = adapter.getVisibleRegion()
 *         // assertions...
 *     }
 * }
 * ```
 */
@RunWith(AndroidJUnit4::class)
abstract class BaseMapIntegrationTest {
    // ============================================================
    // PROTECTED PROPERTIES
    // ============================================================

    protected lateinit var context: Context
    protected lateinit var mapView: MapView
    protected lateinit var mapboxMap: MapboxMap
    protected lateinit var adapter: AndroidMapLibreAdapter

    protected var eventBounds: BoundingBox = STANDARD_EVENT_BOUNDS
    protected var screenWidth: Double = PORTRAIT_PHONE.width
    protected var screenHeight: Double = PORTRAIT_PHONE.height

    // Style path for test maps (using local test tiles)
    protected open val stylePath: String = "asset://test_map_style.json"

    // Test event ID
    protected open val testEventId: String = "test-event-001"

    // ============================================================
    // SETUP / TEARDOWN
    // ============================================================

    @Before
    open fun setUp() {
        context = ApplicationProvider.getApplicationContext()

        // Create headless MapView (no activity needed)
        mapView = createHeadlessMapView()

        // Initialize adapter
        adapter = AndroidMapLibreAdapter(testEventId)

        // Load map asynchronously and wait
        val mapLoadedLatch = CountDownLatch(1)

        mapView.getMapAsync { map ->
            mapboxMap = map
            adapter.setMap(map)

            // Load style and wait
            adapter.setStyle(stylePath) {
                mapLoadedLatch.countDown()
            }
        }

        // Wait up to 10 seconds for map to load
        val loaded = mapLoadedLatch.await(10, TimeUnit.SECONDS)
        if (!loaded) {
            fail("MapView failed to load within timeout")
        }

        // Give map time to fully initialize
        Thread.sleep(500)
    }

    @After
    open fun tearDown() {
        // Clean up map resources
        mapView.onDestroy()
    }

    // ============================================================
    // HEADLESS MAP CREATION
    // ============================================================

    /**
     * Creates a headless MapView without attaching to an activity.
     * The view is measured and laid out to simulate real rendering dimensions.
     */
    private fun createHeadlessMapView(): MapView {
        val view = MapView(context)

        // Measure and layout the view to set dimensions
        // This makes MapLibre think it's in a real layout
        view.measure(
            View.MeasureSpec.makeMeasureSpec(screenWidth.toInt(), View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(screenHeight.toInt(), View.MeasureSpec.EXACTLY),
        )
        view.layout(0, 0, screenWidth.toInt(), screenHeight.toInt())

        // Trigger onCreate lifecycle event (required for MapView initialization)
        view.onCreate(null)

        return view
    }

    // ============================================================
    // TEST HELPER METHODS
    // ============================================================

    /**
     * Wait for map camera to become idle.
     * Useful after programmatic camera movements.
     */
    protected fun waitForIdle(timeoutMs: Long = 2000) {
        val idleLatch = CountDownLatch(1)

        adapter.addOnCameraIdleListener {
            idleLatch.countDown()
        }

        val idle = idleLatch.await(timeoutMs, TimeUnit.MILLISECONDS)
        if (!idle) {
            fail("Map did not become idle within $timeoutMs ms")
        }

        // Give map extra time to settle
        Thread.sleep(200)
    }

    /**
     * Animate camera and wait for completion
     */
    protected fun animateCameraAndWait(
        position: Position,
        zoom: Double? = null,
        timeoutMs: Long = 2000,
    ) {
        adapter.animateCamera(position, zoom)
        waitForIdle(timeoutMs)
    }

    /**
     * Set camera constraints and verify they were applied
     */
    protected fun applyConstraintsAndVerify(
        constraintBounds: BoundingBox,
        isWindowMode: Boolean = false,
    ) {
        adapter.setBoundsForCameraTarget(
            constraintBounds = constraintBounds,
            applyZoomSafetyMargin = isWindowMode,
            originalEventBounds = eventBounds,
        )

        // Give MapLibre time to apply constraints
        Thread.sleep(300)
    }

    // ============================================================
    // ASSERTION HELPERS
    // ============================================================

    /**
     * Assert that visible region is completely within event bounds
     */
    protected fun assertVisibleRegionWithinBounds(message: String = "Visible region should be completely within event bounds") {
        val visibleRegion = adapter.getVisibleRegion()
        assertNotNull(visibleRegion, "Visible region should not be null")

        assertTrue(
            visibleRegion.isCompletelyWithin(eventBounds),
            "$message\n" +
                "  Visible: SW(${visibleRegion.southwest.latitude}, ${visibleRegion.southwest.longitude}) " +
                "NE(${visibleRegion.northeast.latitude}, ${visibleRegion.northeast.longitude})\n" +
                "  Event: SW(${eventBounds.southwest.latitude}, ${eventBounds.southwest.longitude}) " +
                "NE(${eventBounds.northeast.latitude}, ${eventBounds.northeast.longitude})",
        )
    }

    /**
     * Assert that camera position is approximately at expected position
     */
    protected fun assertCameraAt(
        expectedPosition: Position,
        tolerance: Double = TOLERANCE_POSITION,
        message: String = "Camera should be at expected position",
    ) {
        val actualPosition = adapter.getCameraPosition()
        assertNotNull(actualPosition, "Camera position should not be null")

        assertTrue(
            actualPosition.isApproximately(expectedPosition, tolerance),
            "$message\n" +
                "  Expected: (${expectedPosition.latitude}, ${expectedPosition.longitude})\n" +
                "  Actual: (${actualPosition.latitude}, ${actualPosition.longitude})\n" +
                "  Tolerance: $tolerance",
        )
    }

    /**
     * Assert that visible region center matches camera position
     */
    protected fun assertVisibleRegionCenterMatchesCamera(
        tolerance: Double = TOLERANCE_POSITION,
        message: String = "Visible region center should match camera position",
    ) {
        val visibleRegion = adapter.getVisibleRegion()
        val cameraPosition = adapter.getCameraPosition()

        assertNotNull(visibleRegion, "Visible region should not be null")
        assertNotNull(cameraPosition, "Camera position should not be null")

        val regionCenter = visibleRegion.center()

        assertTrue(
            regionCenter.isApproximately(cameraPosition, tolerance),
            "$message\n" +
                "  Region center: (${regionCenter.latitude}, ${regionCenter.longitude})\n" +
                "  Camera: (${cameraPosition.latitude}, ${cameraPosition.longitude})\n" +
                "  Tolerance: $tolerance",
        )
    }

    /**
     * Assert that zoom level is approximately at expected level
     */
    protected fun assertZoomLevel(
        expectedZoom: Double,
        tolerance: Double = MapTestFixtures.TOLERANCE_ZOOM,
        message: String = "Zoom level should be at expected value",
    ) {
        val actualZoom = runBlocking { adapter.currentZoom.value }

        val difference = kotlin.math.abs(actualZoom - expectedZoom)
        assertTrue(
            difference <= tolerance,
            "$message\n" +
                "  Expected: $expectedZoom\n" +
                "  Actual: $actualZoom\n" +
                "  Tolerance: $tolerance\n" +
                "  Difference: $difference",
        )
    }

    /**
     * Assert that visible region has valid bounds (not inverted or invalid)
     */
    protected fun assertValidVisibleRegion(message: String = "Visible region should have valid bounds") {
        val visibleRegion = adapter.getVisibleRegion()
        assertNotNull(visibleRegion, "Visible region should not be null")

        assertTrue(
            visibleRegion.northeast.latitude > visibleRegion.southwest.latitude,
            "$message: NE latitude should be > SW latitude",
        )
        assertTrue(
            visibleRegion.northeast.longitude > visibleRegion.southwest.longitude,
            "$message: NE longitude should be > SW longitude",
        )

        val width = visibleRegion.northeast.longitude - visibleRegion.southwest.longitude
        val height = visibleRegion.northeast.latitude - visibleRegion.southwest.latitude

        assertTrue(width > 0.0, "$message: Width should be positive")
        assertTrue(height > 0.0, "$message: Height should be positive")
        assertTrue(width < 10.0, "$message: Width should not be absurd (>10°)")
        assertTrue(height < 10.0, "$message: Height should not be absurd (>10°)")
    }
}
