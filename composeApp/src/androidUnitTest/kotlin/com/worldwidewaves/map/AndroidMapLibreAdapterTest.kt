@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.worldwidewaves.map

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

import com.worldwidewaves.shared.WWWGlobals
import com.worldwidewaves.shared.events.utils.BoundingBox
import com.worldwidewaves.shared.events.utils.Position
import com.worldwidewaves.shared.map.MapCameraCallback
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Projection
import org.maplibre.android.maps.Style
import org.maplibre.android.maps.UiSettings
import org.maplibre.geojson.Polygon
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Comprehensive test suite for AndroidMapLibreAdapter.
 *
 * Tests cover:
 * - Map initialization (setMap, setStyle)
 * - Camera operations (moveCamera, animateCamera, animateCameraToBounds)
 * - Zoom control (setMinZoomPreference, setMaxZoomPreference, getMinZoomLevel)
 * - Bounds operations (setBoundsForCameraTarget, getVisibleRegion)
 * - Wave polygon rendering (addWavePolygons with clear/append modes)
 * - Listeners (setOnMapClickListener, addOnCameraIdleListener)
 * - StateFlow updates (currentPosition, currentZoom)
 * - Layer management and cleanup
 */
class AndroidMapLibreAdapterTest {
    private lateinit var adapter: AndroidMapLibreAdapter
    private lateinit var mockMap: MapLibreMap
    private lateinit var mockStyle: Style
    private lateinit var mockCameraPosition: CameraPosition
    private lateinit var mockProjection: Projection
    private lateinit var mockUiSettings: UiSettings
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @BeforeTest
    fun setUp() {
        // Create mocks
        mockCameraPosition = mockk(relaxed = true)
        mockProjection = mockk(relaxed = true)
        mockUiSettings = mockk(relaxed = true)
        mockStyle = mockk(relaxed = true)
        mockMap = mockk(relaxed = true)

        // Configure camera position mock
        every { mockCameraPosition.target } returns LatLng(48.8566, 2.3522) // Paris
        every { mockCameraPosition.zoom } returns 12.0

        // Configure map mocks
        every { mockMap.cameraPosition } returns mockCameraPosition
        every { mockMap.projection } returns mockProjection
        every { mockMap.uiSettings } returns mockUiSettings
        every { mockMap.width } returns 1080f
        every { mockMap.height } returns 1920f
        every { mockMap.minZoomLevel } returns 0.0
        every { mockMap.addOnCameraIdleListener(any()) } just Runs
        every { mockMap.removeOnMapClickListener(any()) } just Runs
        every { mockMap.addOnMapClickListener(any()) } just Runs
        every { mockMap.moveCamera(any()) } just Runs
        every { mockMap.animateCamera(any(), any(), any()) } just Runs
        every { mockMap.setMinZoomPreference(any()) } just Runs
        every { mockMap.setMaxZoomPreference(any()) } just Runs
        every { mockMap.setLatLngBoundsForCameraTarget(any()) } just Runs

        // Configure style mock
        every { mockStyle.removeLayer(any<String>()) } returns true
        every { mockStyle.removeSource(any<String>()) } returns true
        every { mockStyle.addSource(any()) } just Runs
        every { mockStyle.addLayer(any()) } just Runs
        every { mockMap.style } returns mockStyle
        every { mockMap.getStyle(any()) } answers {
            val callback = firstArg<Style.OnStyleLoaded>()
            callback.onStyleLoaded(mockStyle)
        }

        // Configure projection mock for visible region
        val mockBounds = mockk<LatLngBounds>(relaxed = true)
        every { mockBounds.getLatNorth() } returns 48.9
        every { mockBounds.getLatSouth() } returns 48.8
        every { mockBounds.getLonEast() } returns 2.4
        every { mockBounds.getLonWest() } returns 2.3

        every { mockProjection.visibleRegion.latLngBounds } returns mockBounds

        // Create adapter (without map initially)
        adapter = AndroidMapLibreAdapter()
    }

    @AfterTest
    fun tearDown() {
        unmockkAll()
    }

    // ============================================================
    // MAP INITIALIZATION TESTS
    // ============================================================

    @Test
    fun `setMap initializes adapter with map instance`() =
        testScope.runTest {
            // When: Set map
            adapter.setMap(mockMap)

            // Then: Camera idle listener should be added
            verify { mockMap.addOnCameraIdleListener(any()) }
        }

    @Test
    fun `setMap updates currentPosition and currentZoom flows`() =
        testScope.runTest {
            // Given: Initial flows are null/zero
            assertNull(adapter.currentPosition.value)
            assertEquals(0.0, adapter.currentZoom.value)

            // When: Set map
            adapter.setMap(mockMap)
            advanceUntilIdle()

            // Then: Flows should be updated with camera position
            val position = adapter.currentPosition.first()
            assertNotNull(position)
            assertEquals(48.8566, position.latitude, 0.0001)
            assertEquals(2.3522, position.longitude, 0.0001)
            assertEquals(12.0, adapter.currentZoom.first())
        }

    @Test
    fun `setMap executes pending onMapSet callbacks`() =
        testScope.runTest {
            // Given: Callbacks registered before map is set
            var callback1Executed = false
            var callback2Executed = false

            adapter.onMapSet { callback1Executed = true }
            adapter.onMapSet { callback2Executed = true }

            // When: Set map
            adapter.setMap(mockMap)

            // Then: Both callbacks should execute
            assertTrue(callback1Executed)
            assertTrue(callback2Executed)
        }

    @Test
    fun `onMapSet executes immediately when map already set`() {
        // Given: Map already set
        adapter.setMap(mockMap)

        // When: Register callback
        var callbackExecuted = false
        adapter.onMapSet { callbackExecuted = true }

        // Then: Callback should execute immediately
        assertTrue(callbackExecuted)
    }

    @Test
    fun `setStyle applies style URI and invokes callback`() {
        // Given: Map is set
        adapter.setMap(mockMap)
        val styleUri = "https://tiles.example.com/style.json"

        // When: Set style
        var callbackExecuted = false
        adapter.setStyle(styleUri) {
            callbackExecuted = true
            Unit
        }

        // Then: Style should be loaded and callback invoked
        verify { mockMap.setStyle(any<Style.Builder>(), any<Style.OnStyleLoaded>()) }
        assertTrue(callbackExecuted)
    }

    @Test
    fun `setStyle fails with IllegalArgumentException when map not set`() {
        // When/Then: setStyle without map should fail
        assertFailsWith<IllegalArgumentException> {
            adapter.setStyle("https://example.com/style.json") { Unit }
        }
    }

    // ============================================================
    // CAMERA OPERATIONS TESTS
    // ============================================================

    @Test
    fun `moveCamera moves to bounding box`() {
        // Given: Map is set
        adapter.setMap(mockMap)
        val bbox =
            BoundingBox.fromCorners(
                Position(48.8, 2.3),
                Position(48.9, 2.4),
            )

        // When: Move camera
        adapter.moveCamera(bbox)

        // Then: Camera should be moved
        verify { mockMap.moveCamera(any()) }
    }

    @Test
    fun `moveCamera fails when map not set`() {
        // Given: Map not set
        val bbox =
            BoundingBox.fromCorners(
                Position(48.8, 2.3),
                Position(48.9, 2.4),
            )

        // When/Then: Should fail
        assertFailsWith<IllegalArgumentException> {
            adapter.moveCamera(bbox)
        }
    }

    @Test
    fun `animateCamera to position with zoom`() =
        testScope.runTest {
            // Given: Map is set
            adapter.setMap(mockMap)
            val position = Position(48.8566, 2.3522)
            val zoom = 15.0
            var finishCalled = false

            val callback =
                object : MapCameraCallback {
                    override fun onFinish() {
                        finishCalled = true
                    }

                    override fun onCancel() {}
                }

            // When: Animate camera
            adapter.animateCamera(position, zoom, callback)

            // Then: Should call animateCamera with correct duration
            verify {
                mockMap.animateCamera(
                    any(),
                    WWWGlobals.Timing.MAP_CAMERA_ANIMATION_DURATION_MS,
                    any(),
                )
            }
        }

    @Test
    fun `animateCamera without zoom uses current zoom`() =
        testScope.runTest {
            // Given: Map is set
            adapter.setMap(mockMap)
            val position = Position(51.5074, -0.1278) // London

            // When: Animate without zoom
            adapter.animateCamera(position, null, null)

            // Then: Should animate to position
            verify {
                mockMap.animateCamera(
                    any(),
                    WWWGlobals.Timing.MAP_CAMERA_ANIMATION_DURATION_MS,
                    any(),
                )
            }
        }

    @Test
    fun `animateCamera invokes callback onFinish`() =
        testScope.runTest {
            // Given: Map is set and callback captures the CancelableCallback
            adapter.setMap(mockMap)
            val callbackSlot = slot<MapLibreMap.CancelableCallback>()
            every { mockMap.animateCamera(any(), any(), capture(callbackSlot)) } just Runs

            var finishCalled = false
            val callback =
                object : MapCameraCallback {
                    override fun onFinish() {
                        finishCalled = true
                    }

                    override fun onCancel() {}
                }

            // When: Animate camera
            adapter.animateCamera(Position(48.8566, 2.3522), 12.0, callback)

            // Then: Invoke the captured callback's onFinish
            callbackSlot.captured.onFinish()
            assertTrue(finishCalled)
        }

    @Test
    fun `animateCamera invokes callback onCancel`() =
        testScope.runTest {
            // Given: Map is set and callback captures the CancelableCallback
            adapter.setMap(mockMap)
            val callbackSlot = slot<MapLibreMap.CancelableCallback>()
            every { mockMap.animateCamera(any(), any(), capture(callbackSlot)) } just Runs

            var cancelCalled = false
            val callback =
                object : MapCameraCallback {
                    override fun onFinish() {}

                    override fun onCancel() {
                        cancelCalled = true
                    }
                }

            // When: Animate camera
            adapter.animateCamera(Position(48.8566, 2.3522), 12.0, callback)

            // Then: Invoke the captured callback's onCancel
            callbackSlot.captured.onCancel()
            assertTrue(cancelCalled)
        }

    @Test
    fun `animateCameraToBounds animates to bounding box with padding`() =
        testScope.runTest {
            // Given: Map is set
            adapter.setMap(mockMap)
            val bbox =
                BoundingBox.fromCorners(
                    Position(48.8, 2.3),
                    Position(48.9, 2.4),
                )
            val padding = 100

            // When: Animate to bounds
            adapter.animateCameraToBounds(bbox, padding, null)

            // Then: Should animate with correct duration
            verify {
                mockMap.animateCamera(
                    any(),
                    WWWGlobals.Timing.MAP_CAMERA_ANIMATION_DURATION_MS,
                    any(),
                )
            }
        }

    @Test
    fun `animateCameraToBounds invokes callback onFinish`() =
        testScope.runTest {
            // Given: Map is set and callback captures the CancelableCallback
            adapter.setMap(mockMap)
            val callbackSlot = slot<MapLibreMap.CancelableCallback>()
            every { mockMap.animateCamera(any(), any(), capture(callbackSlot)) } just Runs

            val bbox =
                BoundingBox.fromCorners(
                    Position(48.8, 2.3),
                    Position(48.9, 2.4),
                )
            var finishCalled = false
            val callback =
                object : MapCameraCallback {
                    override fun onFinish() {
                        finishCalled = true
                    }

                    override fun onCancel() {}
                }

            // When: Animate to bounds
            adapter.animateCameraToBounds(bbox, 50, callback)

            // Then: Invoke the captured callback's onFinish
            callbackSlot.captured.onFinish()
            assertTrue(finishCalled)
        }

    // ============================================================
    // ZOOM CONTROL TESTS
    // ============================================================

    @Test
    fun `setMinZoomPreference sets minimum zoom`() {
        // Given: Map is set
        adapter.setMap(mockMap)

        // When: Set min zoom
        adapter.setMinZoomPreference(5.0)

        // Then: Should call map method
        verify { mockMap.setMinZoomPreference(5.0) }
    }

    @Test
    fun `setMaxZoomPreference sets maximum zoom`() {
        // Given: Map is set
        adapter.setMap(mockMap)

        // When: Set max zoom
        adapter.setMaxZoomPreference(18.0)

        // Then: Should call map method
        verify { mockMap.setMaxZoomPreference(18.0) }
    }

    @Test
    fun `getMinZoomLevel returns minimum zoom from map`() {
        // Given: Map is set with min zoom
        every { mockMap.minZoomLevel } returns 3.5
        adapter.setMap(mockMap)

        // When: Get min zoom
        val minZoom = adapter.getMinZoomLevel()

        // Then: Should return map's value
        assertEquals(3.5, minZoom)
    }

    @Test
    fun `setMinZoomPreference fails when map not set`() {
        // When/Then: Should fail
        assertFailsWith<IllegalArgumentException> {
            adapter.setMinZoomPreference(5.0)
        }
    }

    // ============================================================
    // BOUNDS OPERATIONS TESTS
    // ============================================================

    @Test
    fun `setBoundsForCameraTarget sets camera constraint bounds`() {
        // Given: Map is set
        adapter.setMap(mockMap)
        val constraintBounds =
            BoundingBox.fromCorners(
                Position(48.0, 2.0),
                Position(49.0, 3.0),
            )

        // When: Set bounds constraint
        adapter.setBoundsForCameraTarget(constraintBounds)

        // Then: Should call map method
        verify { mockMap.setLatLngBoundsForCameraTarget(any()) }
    }

    @Test
    fun `getVisibleRegion returns current visible bounding box`() {
        // Given: Map is set
        adapter.setMap(mockMap)

        // When: Get visible region
        val visibleRegion = adapter.getVisibleRegion()

        // Then: Should return correct bounds
        assertEquals(48.8, visibleRegion.southwest.latitude, 0.01)
        assertEquals(2.3, visibleRegion.southwest.longitude, 0.01)
        assertEquals(48.9, visibleRegion.northeast.latitude, 0.01)
        assertEquals(2.4, visibleRegion.northeast.longitude, 0.01)
    }

    @Test
    fun `getCameraPosition returns current camera position`() {
        // Given: Map is set
        adapter.setMap(mockMap)

        // When: Get camera position
        val position = adapter.getCameraPosition()

        // Then: Should return current position
        assertNotNull(position)
        assertEquals(48.8566, position.latitude, 0.0001)
        assertEquals(2.3522, position.longitude, 0.0001)
    }

    @Test
    fun `getCameraPosition returns null when target is null`() {
        // Given: Map is set but camera position has null target
        every { mockCameraPosition.target } returns null
        adapter.setMap(mockMap)

        // When: Get camera position
        val position = adapter.getCameraPosition()

        // Then: Should return null
        assertNull(position)
    }

    // ============================================================
    // WAVE POLYGON RENDERING TESTS
    // ============================================================

    @Test
    fun `addWavePolygons clears existing layers when clearExisting is true`() {
        // Given: Map is set with existing wave layers
        adapter.setMap(mockMap)

        // Add initial polygons
        val polygon1 =
            Polygon.fromLngLats(
                listOf(
                    listOf(
                        org.maplibre.geojson.Point
                            .fromLngLat(2.3, 48.8),
                        org.maplibre.geojson.Point
                            .fromLngLat(2.4, 48.8),
                        org.maplibre.geojson.Point
                            .fromLngLat(2.4, 48.9),
                        org.maplibre.geojson.Point
                            .fromLngLat(2.3, 48.9),
                        org.maplibre.geojson.Point
                            .fromLngLat(2.3, 48.8),
                    ),
                ),
            )
        adapter.addWavePolygons(listOf(polygon1), clearExisting = false)

        // When: Add new polygons with clearExisting = true
        val polygon2 =
            Polygon.fromLngLats(
                listOf(
                    listOf(
                        org.maplibre.geojson.Point
                            .fromLngLat(2.5, 48.8),
                        org.maplibre.geojson.Point
                            .fromLngLat(2.6, 48.8),
                        org.maplibre.geojson.Point
                            .fromLngLat(2.6, 48.9),
                        org.maplibre.geojson.Point
                            .fromLngLat(2.5, 48.9),
                        org.maplibre.geojson.Point
                            .fromLngLat(2.5, 48.8),
                    ),
                ),
            )
        adapter.addWavePolygons(listOf(polygon2), clearExisting = true)

        // Then: Old layers should be removed
        verify(atLeast = 1) { mockStyle.removeLayer("wave-polygons-layer-0") }
        verify(atLeast = 1) { mockStyle.removeSource("wave-polygons-source-0") }
    }

    @Test
    fun `addWavePolygons appends layers when clearExisting is false`() {
        // Given: Map is set
        adapter.setMap(mockMap)

        // When: Add polygons without clearing
        val polygon1 =
            Polygon.fromLngLats(
                listOf(
                    listOf(
                        org.maplibre.geojson.Point
                            .fromLngLat(2.3, 48.8),
                        org.maplibre.geojson.Point
                            .fromLngLat(2.4, 48.8),
                        org.maplibre.geojson.Point
                            .fromLngLat(2.4, 48.9),
                        org.maplibre.geojson.Point
                            .fromLngLat(2.3, 48.8),
                    ),
                ),
            )
        adapter.addWavePolygons(listOf(polygon1), clearExisting = false)

        val polygon2 =
            Polygon.fromLngLats(
                listOf(
                    listOf(
                        org.maplibre.geojson.Point
                            .fromLngLat(2.5, 48.8),
                        org.maplibre.geojson.Point
                            .fromLngLat(2.6, 48.8),
                        org.maplibre.geojson.Point
                            .fromLngLat(2.6, 48.9),
                        org.maplibre.geojson.Point
                            .fromLngLat(2.5, 48.8),
                    ),
                ),
            )
        adapter.addWavePolygons(listOf(polygon2), clearExisting = false)

        // Then: Both layers should be added (defensive cleanup + addition for each)
        verify(atLeast = 2) { mockStyle.addLayer(any()) }
        verify(atLeast = 2) { mockStyle.addSource(any()) }
    }

    @Test
    fun `addWavePolygons filters out non-Polygon objects`() {
        // Given: Map is set
        adapter.setMap(mockMap)

        // When: Add list with non-Polygon objects
        val validPolygon =
            Polygon.fromLngLats(
                listOf(
                    listOf(
                        org.maplibre.geojson.Point
                            .fromLngLat(2.3, 48.8),
                        org.maplibre.geojson.Point
                            .fromLngLat(2.4, 48.8),
                        org.maplibre.geojson.Point
                            .fromLngLat(2.4, 48.9),
                        org.maplibre.geojson.Point
                            .fromLngLat(2.3, 48.8),
                    ),
                ),
            )
        adapter.addWavePolygons(listOf(validPolygon, "invalid", 123), clearExisting = false)

        // Then: Only valid polygon should be processed
        verify(atLeast = 1) { mockStyle.addLayer(any()) }
    }

    @Test
    fun `addWavePolygons does nothing when list is empty`() {
        // Given: Map is set
        adapter.setMap(mockMap)

        // When: Add empty list
        adapter.addWavePolygons(emptyList<Any>(), clearExisting = false)

        // Then: No layers should be added (only defensive cleanup)
        verify(exactly = 0) { mockStyle.addSource(any()) }
        verify(exactly = 0) { mockStyle.addLayer(any()) }
    }

    @Test
    fun `addWavePolygons does nothing when all objects are invalid`() {
        // Given: Map is set
        adapter.setMap(mockMap)

        // When: Add list with only invalid objects
        adapter.addWavePolygons(listOf<Any>("string", 123, "test"), clearExisting = false)

        // Then: No layers should be added
        verify(exactly = 0) { mockStyle.addSource(any()) }
        verify(exactly = 0) { mockStyle.addLayer(any()) }
    }

    // ============================================================
    // LISTENER TESTS
    // ============================================================

    @Test
    fun `setOnMapClickListener adds click listener`() {
        // Given: Map is set
        adapter.setMap(mockMap)

        // When: Set click listener
        var clickedLat = 0.0
        var clickedLng = 0.0
        adapter.setOnMapClickListener { lat: Double, lng: Double ->
            clickedLat = lat
            clickedLng = lng
        }

        // Then: Listener should be added
        verify { mockMap.addOnMapClickListener(any()) }
    }

    @Test
    fun `setOnMapClickListener removes previous listener`() {
        // Given: Map is set with existing listener
        adapter.setMap(mockMap)
        adapter.setOnMapClickListener { _: Double, _: Double -> Unit }

        // When: Set new listener
        adapter.setOnMapClickListener { _: Double, _: Double -> Unit }

        // Then: Old listener should be removed first
        verify(atLeast = 1) { mockMap.removeOnMapClickListener(any()) }
    }

    @Test
    fun `setOnMapClickListener with null removes listener`() {
        // Given: Map is set with existing listener
        adapter.setMap(mockMap)
        adapter.setOnMapClickListener { _: Double, _: Double -> Unit }

        // When: Set null listener
        adapter.setOnMapClickListener(null)

        // Then: Listener should be removed
        verify(atLeast = 1) { mockMap.removeOnMapClickListener(any()) }
    }

    @Test
    fun `addOnCameraIdleListener adds camera idle callback`() {
        // Given: Map is set
        adapter.setMap(mockMap)

        // When: Add camera idle listener
        var callbackExecuted = false
        adapter.addOnCameraIdleListener { callbackExecuted = true }

        // Then: Listener should be added
        verify(atLeast = 2) { mockMap.addOnCameraIdleListener(any()) } // One from setMap, one from this call
    }

    // ============================================================
    // STATEFLOW UPDATE TESTS
    // ============================================================

    @Test
    fun `currentZoom StateFlow updates when camera moves`() =
        testScope.runTest {
            // Given: Map is set and camera idle listener is captured
            adapter.setMap(mockMap)
            val listenerSlot = slot<MapLibreMap.OnCameraIdleListener>()
            verify { mockMap.addOnCameraIdleListener(capture(listenerSlot)) }

            // When: Camera position changes and idle listener is triggered
            every { mockCameraPosition.zoom } returns 15.5
            listenerSlot.captured.onCameraIdle()
            advanceUntilIdle()

            // Then: currentZoom should update
            assertEquals(15.5, adapter.currentZoom.first())
        }

    @Test
    fun `currentPosition StateFlow updates when camera moves`() =
        testScope.runTest {
            // Given: Map is set and camera idle listener is captured
            adapter.setMap(mockMap)
            val listenerSlot = slot<MapLibreMap.OnCameraIdleListener>()
            verify { mockMap.addOnCameraIdleListener(capture(listenerSlot)) }

            // When: Camera position changes and idle listener is triggered
            val newTarget = LatLng(51.5074, -0.1278) // London
            every { mockCameraPosition.target } returns newTarget
            listenerSlot.captured.onCameraIdle()
            advanceUntilIdle()

            // Then: currentPosition should update
            val position = adapter.currentPosition.first()
            assertNotNull(position)
            assertEquals(51.5074, position!!.latitude, 0.0001)
            assertEquals(-0.1278, position.longitude, 0.0001)
        }

    // ============================================================
    // DIMENSION TESTS
    // ============================================================

    @Test
    fun `getWidth returns map width`() {
        // Given: Map is set
        every { mockMap.width } returns 1080f
        adapter.setMap(mockMap)

        // When: Get width
        val width = adapter.getWidth()

        // Then: Should return correct width
        assertEquals(1080.0, width)
    }

    @Test
    fun `getHeight returns map height`() {
        // Given: Map is set
        every { mockMap.height } returns 1920f
        adapter.setMap(mockMap)

        // When: Get height
        val height = adapter.getHeight()

        // Then: Should return correct height
        assertEquals(1920.0, height)
    }

    // ============================================================
    // UI SETTINGS TESTS
    // ============================================================

    @Test
    fun `setAttributionMargins sets correct margins`() {
        // Given: Map is set
        adapter.setMap(mockMap)

        // When: Set attribution margins
        adapter.setAttributionMargins(10, 20, 30, 40)

        // Then: Should call uiSettings method
        verify { mockUiSettings.setAttributionMargins(10, 20, 30, 40) }
    }

    // ============================================================
    // OVERRIDE BBOX DRAWING TEST
    // ============================================================

    @Test
    fun `drawOverridenBbox adds debug bbox layer`() {
        // Given: Map is set
        adapter.setMap(mockMap)
        val bbox =
            BoundingBox.fromCorners(
                Position(48.8, 2.3),
                Position(48.9, 2.4),
            )

        // When: Draw override bbox
        adapter.drawOverridenBbox(bbox)

        // Then: Should add source and line layer
        verify { mockStyle.addSource(any()) }
        verify { mockStyle.addLayer(any()) }
    }
}
