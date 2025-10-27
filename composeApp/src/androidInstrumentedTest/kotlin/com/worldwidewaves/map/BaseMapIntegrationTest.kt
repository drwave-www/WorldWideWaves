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
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.worldwidewaves.shared.events.utils.BoundingBox
import com.worldwidewaves.shared.events.utils.Position
import com.worldwidewaves.shared.map.MapTestFixtures
import com.worldwidewaves.shared.map.MapTestFixtures.PORTRAIT_PHONE
import com.worldwidewaves.shared.map.MapTestFixtures.STANDARD_EVENT_BOUNDS
import com.worldwidewaves.shared.map.MapTestFixtures.TOLERANCE_POSITION
import com.worldwidewaves.shared.map.MapTestFixtures.center
import com.worldwidewaves.shared.map.MapTestFixtures.isApproximately
import com.worldwidewaves.shared.map.MapTestFixtures.isCompletelyWithin
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.runner.RunWith
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Base class for Android MapLibre integration tests using Activity-based MapView.
 *
 * This approach provides:
 * - Real MapLibre map instance with OpenGL rendering context
 * - Activity lifecycle management for MapView initialization
 * - Programmatic camera control (no gesture simulation)
 * - Direct API access for validation
 *
 * The MapView requires a real Activity with OpenGL context for getMapAsync
 * callback to fire. Using ActivityScenarioRule with TestMapActivity solves
 * the headless MapView callback issue.
 *
 * Usage example shows how to extend this base class for specific map tests.
 * Subclasses can use mapView and adapter properties directly after setup.
 */
@RunWith(AndroidJUnit4::class)
abstract class BaseMapIntegrationTest {
    // ============================================================
    // ACTIVITY SCENARIO
    // ============================================================

    protected lateinit var activityScenario: ActivityScenario<TestMapActivity>
    // ============================================================
    // PROTECTED PROPERTIES
    // ============================================================

    protected lateinit var context: Context
    protected lateinit var mapView: MapView
    protected lateinit var mapLibreMap: MapLibreMap
    protected lateinit var adapter: AndroidMapLibreAdapter

    protected var eventBounds: BoundingBox = STANDARD_EVENT_BOUNDS
    protected var screenWidth: Double = PORTRAIT_PHONE.width
    protected var screenHeight: Double = PORTRAIT_PHONE.height

    // Style JSON for test maps
    // Use inline minimal style (no network or assets required)
    protected open val styleJson: String =
        """
        {
          "version": 8,
          "name": "Test Style",
          "sources": {},
          "layers": [
            {
              "id": "background",
              "type": "background",
              "paint": {"background-color": "#f0f0f0"}
            }
          ]
        }
        """.trimIndent()

    // Test event ID
    protected open val testEventId: String = "test-event-001"

    // ============================================================
    // SETUP / TEARDOWN
    // ============================================================

    @Before
    open fun setUp() {
        context = ApplicationProvider.getApplicationContext()

        // Initialize MapLibre on main thread (MapLibre requires UI thread for getInstance)
        // This must run before ActivityScenario.launch to avoid threading issues
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().run {
            runOnMainSync {
                org.maplibre.android.MapLibre
                    .getInstance(context)
            }
        }

        val mapLoadedLatch = CountDownLatch(1)
        var setupError: Throwable? = null

        // Launch activity without LAUNCHER intent category
        val intent =
            android.content.Intent(context, TestMapActivity::class.java).apply {
                // Clear default flags to avoid process mismatch
                flags = 0
            }
        activityScenario = ActivityScenario.launch(intent)

        // Get MapView from Activity
        activityScenario.onActivity { activity ->
            try {
                System.out.println("BaseMapIntegrationTest: Getting MapView from TestMapActivity...")
                mapView = activity.mapView

                System.out.println("BaseMapIntegrationTest: Creating adapter...")
                adapter = AndroidMapLibreAdapter()

                System.out.println("BaseMapIntegrationTest: Calling getMapAsync...")
                // Load map asynchronously and wait
                mapView.getMapAsync { map ->
                    try {
                        System.out.println("BaseMapIntegrationTest: getMapAsync callback received!")
                        mapLibreMap = map
                        activity.mapLibreMap = map

                        // Set map on UI thread (MapLibre requirement)
                        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().runOnMainSync {
                            adapter.setMap(map)
                        }

                        System.out.println("BaseMapIntegrationTest: Loading inline JSON style...")
                        // Load style from inline JSON (no network/assets required) - already on UI thread
                        mapLibreMap.setStyle(
                            org.maplibre.android.maps.Style
                                .Builder()
                                .fromJson(styleJson),
                        ) {
                            System.out.println("BaseMapIntegrationTest: Style loaded successfully!")
                            mapLoadedLatch.countDown()
                        }
                    } catch (e: Throwable) {
                        System.err.println("BaseMapIntegrationTest: Error in getMapAsync callback: ${e.message}")
                        e.printStackTrace()
                        setupError = e
                        mapLoadedLatch.countDown()
                    }
                }
                System.out.println("BaseMapIntegrationTest: getMapAsync called, waiting for callback...")
            } catch (e: Throwable) {
                System.err.println("BaseMapIntegrationTest: Error in onActivity: ${e.message}")
                e.printStackTrace()
                setupError = e
                mapLoadedLatch.countDown()
            }
        }

        try {
            // Wait up to 15 seconds for map to load
            val loaded = mapLoadedLatch.await(15, TimeUnit.SECONDS)

            // Check for errors during setup
            setupError?.let { throw AssertionError("Map setup failed", it) }

            if (!loaded) {
                fail("MapView failed to load within timeout (inline JSON style)")
            }

            // Give map time to fully initialize
            Thread.sleep(500)
        } catch (e: Throwable) {
            // Clean up activity on failure
            if (::activityScenario.isInitialized) {
                activityScenario.close()
            }
            throw e
        }
    }

    @After
    open fun tearDown() {
        // Close activity scenario to clean up resources
        if (::activityScenario.isInitialized) {
            activityScenario.close()
            System.out.println("BaseMapIntegrationTest: tearDown - activity scenario closed")
        }
    }

    // ============================================================
    // TEST HELPER METHODS
    // ============================================================

    /**
     * Run code on UI thread (required for MapLibre operations).
     * Uses Instrumentation.runOnMainSync to ensure execution on main thread.
     */
    protected fun <T> runOnUiThread(block: () -> T): T {
        var result: T? = null
        var error: Throwable? = null

        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().runOnMainSync {
            try {
                result = block()
            } catch (e: Throwable) {
                error = e
            }
        }

        error?.let { throw it }
        @Suppress("UNCHECKED_CAST")
        return result as T
    }

    /**
     * Wait for map camera to become idle.
     * Useful after programmatic camera movements.
     */
    protected fun waitForIdle(timeoutMs: Long = 2000) {
        val idleLatch = CountDownLatch(1)

        runOnUiThread {
            adapter.addOnCameraIdleListener {
                idleLatch.countDown()
            }
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
        runOnUiThread {
            adapter.animateCamera(position, zoom)
        }
        waitForIdle(timeoutMs)
    }

    /**
     * Set camera constraints and verify they were applied (already wrapped in UI thread)
     */
    protected fun applyConstraintsAndVerify(
        constraintBounds: BoundingBox,
        isWindowMode: Boolean = false,
    ) {
        runOnUiThread {
            adapter.setBoundsForCameraTarget(
                constraintBounds = constraintBounds,
                applyZoomSafetyMargin = isWindowMode,
                originalEventBounds = eventBounds,
            )
        }

        // Give MapLibre time to apply constraints
        Thread.sleep(300)
    }

    /**
     * Set min/max zoom preferences on UI thread
     */
    protected fun setZoomPreferences(
        minZoom: Double? = null,
        maxZoom: Double? = null,
    ) {
        runOnUiThread {
            minZoom?.let { adapter.setMinZoomPreference(it) }
            maxZoom?.let { adapter.setMaxZoomPreference(it) }
        }
    }

    /**
     * Set user position on UI thread
     */
    protected fun setUserPosition(position: Position) {
        runOnUiThread {
            adapter.setUserPosition(position)
        }
    }

    /**
     * Animate camera to bounds and wait for completion
     */
    protected fun animateCameraToBoundsAndWait(
        bounds: BoundingBox,
        timeoutMs: Long = 2000,
    ) {
        runOnUiThread {
            adapter.animateCameraToBounds(bounds)
        }
        waitForIdle(timeoutMs)
    }

    // ============================================================
    // ASSERTION HELPERS
    // ============================================================

    /**
     * Assert that visible region is completely within event bounds
     */
    protected fun assertVisibleRegionWithinBounds(message: String = "Visible region should be completely within event bounds") {
        val visibleRegion = runOnUiThread { adapter.getVisibleRegion() }
        assertNotNull("Visible region should not be null", visibleRegion)

        assertTrue(
            "$message\n" +
                "  Visible: SW(${visibleRegion.southwest.latitude}, ${visibleRegion.southwest.longitude}) " +
                "NE(${visibleRegion.northeast.latitude}, ${visibleRegion.northeast.longitude})\n" +
                "  Event: SW(${eventBounds.southwest.latitude}, ${eventBounds.southwest.longitude}) " +
                "NE(${eventBounds.northeast.latitude}, ${eventBounds.northeast.longitude})",
            visibleRegion.isCompletelyWithin(eventBounds),
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
        val actualPosition = runOnUiThread { adapter.getCameraPosition() }
        assertNotNull("Camera position should not be null", actualPosition)

        assertTrue(
            "$message\n" +
                "  Expected: (${expectedPosition.latitude}, ${expectedPosition.longitude})\n" +
                "  Actual: (${actualPosition!!.latitude}, ${actualPosition.longitude})\n" +
                "  Tolerance: $tolerance",
            actualPosition.isApproximately(expectedPosition, tolerance),
        )
    }

    /**
     * Assert that visible region center matches camera position
     */
    protected fun assertVisibleRegionCenterMatchesCamera(
        tolerance: Double = TOLERANCE_POSITION,
        message: String = "Visible region center should match camera position",
    ) {
        val (visibleRegion, cameraPosition) =
            runOnUiThread {
                adapter.getVisibleRegion() to adapter.getCameraPosition()
            }

        assertNotNull("Visible region should not be null", visibleRegion)
        assertNotNull("Camera position should not be null", cameraPosition)

        val regionCenter = visibleRegion.center()

        assertTrue(
            "$message\n" +
                "  Region center: (${regionCenter.latitude}, ${regionCenter.longitude})\n" +
                "  Camera: (${cameraPosition!!.latitude}, ${cameraPosition.longitude})\n" +
                "  Tolerance: $tolerance",
            regionCenter.isApproximately(cameraPosition, tolerance),
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
            "$message\n" +
                "  Expected: $expectedZoom\n" +
                "  Actual: $actualZoom\n" +
                "  Tolerance: $tolerance\n" +
                "  Difference: $difference",
            difference <= tolerance,
        )
    }

    /**
     * Assert that visible region has valid bounds (not inverted or invalid)
     */
    protected fun assertValidVisibleRegion(message: String = "Visible region should have valid bounds") {
        val visibleRegion = runOnUiThread { adapter.getVisibleRegion() }
        assertNotNull("Visible region should not be null", visibleRegion)

        assertTrue(
            "$message: NE latitude should be > SW latitude",
            visibleRegion.northeast.latitude > visibleRegion.southwest.latitude,
        )
        assertTrue(
            "$message: NE longitude should be > SW longitude",
            visibleRegion.northeast.longitude > visibleRegion.southwest.longitude,
        )

        val width = visibleRegion.northeast.longitude - visibleRegion.southwest.longitude
        val height = visibleRegion.northeast.latitude - visibleRegion.southwest.latitude

        assertTrue("$message: Width should be positive", width > 0.0)
        assertTrue("$message: Height should be positive", height > 0.0)
        assertTrue("$message: Width should not be absurd (>10°)", width < 10.0)
        assertTrue("$message: Height should not be absurd (>10°)", height < 10.0)
    }
}
