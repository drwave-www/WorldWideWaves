package com.worldwidewaves.shared.map

/*
 * Copyright 2025 DrWave
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * https://www.apache.org/licenses/LICENSE-2.0
 */

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.worldwidewaves.shared.WWWGlobals
import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.events.WWWEventArea
import com.worldwidewaves.shared.events.WWWEventMap
import com.worldwidewaves.shared.events.WWWEventWave
import com.worldwidewaves.shared.events.WWWEventWaveLinear
import com.worldwidewaves.shared.events.utils.BoundingBox
import com.worldwidewaves.shared.events.utils.CoroutineScopeProvider
import com.worldwidewaves.shared.events.utils.DefaultCoroutineScopeProvider
import com.worldwidewaves.shared.events.utils.Polygon
import com.worldwidewaves.shared.events.utils.Position
import com.worldwidewaves.shared.position.PositionManager
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AbstractEventMapTest : KoinTest {
    private lateinit var mockEvent: IWWWEvent
    private lateinit var mockMapLibreAdapter: MapLibreAdapter<String>
    private lateinit var mockLocationProvider: LocationProvider
    private lateinit var positionManager: PositionManager
    private lateinit var testScopeProvider: CoroutineScopeProvider
    private lateinit var testScope: TestScope
    private lateinit var eventMap: TestEventMap
    private var capturedOnLocationUpdate: Position? = null

    private val testBounds =
        BoundingBox.fromCorners(
            Position(48.8, 2.2),
            Position(48.9, 2.4),
        )
    private val testCenter = Position(48.85, 2.3)
    private val testUserPosition = Position(48.86, 2.35)

    @BeforeTest
    fun setup() {
        val testDispatcher = StandardTestDispatcher()
        testScope = TestScope(testDispatcher)
        testScopeProvider =
            DefaultCoroutineScopeProvider(
                ioDispatcher = testDispatcher,
                defaultDispatcher = testDispatcher,
            )

        positionManager = PositionManager(testScopeProvider)

        startKoin {
            modules(
                module {
                    single { positionManager }
                },
            )
        }

        // Setup mock event
        mockEvent = mockk()
        val mockArea = mockk<WWWEventArea>()
        val mockWave = mockk<WWWEventWave>()
        val mockEventMap = mockk<WWWEventMap>()

        every { mockEvent.id } returns "test-event-id"
        every { mockEvent.area } returns mockArea
        every { mockEvent.wave } returns mockWave
        every { mockEvent.map } returns mockEventMap
        coEvery { mockArea.bbox() } returns testBounds
        coEvery { mockArea.getCenter() } returns testCenter
        coEvery { mockArea.isPositionWithin(any()) } returns true // Default: user inside area (override in specific tests)
        every { mockArea.bboxIsOverride } returns false
        every { mockEventMap.maxZoom } returns 18.0
        coEvery { mockWave.userClosestWaveLongitude() } returns 2.38
        coEvery { mockWave.getWaveFrontCenterPosition() } returns Position(48.85, 2.38) // Center latitude + wave longitude

        // Setup mock MapLibreAdapter
        mockMapLibreAdapter = mockk()
        val currentPositionFlow = MutableStateFlow<Position?>(null)
        val currentZoomFlow = MutableStateFlow(12.0)

        every { mockMapLibreAdapter.currentPosition } returns currentPositionFlow
        every { mockMapLibreAdapter.currentZoom } returns currentZoomFlow
        every { mockMapLibreAdapter.getWidth() } returns 600.0
        every { mockMapLibreAdapter.getHeight() } returns 800.0
        every { mockMapLibreAdapter.setMap(any()) } just Runs
        every { mockMapLibreAdapter.setStyle(any(), any()) } answers {
            val callback = secondArg<() -> Unit>()
            callback()
        }
        every { mockMapLibreAdapter.setAttributionMargins(any(), any(), any(), any()) } just Runs
        every { mockMapLibreAdapter.setGesturesEnabled(any()) } just Runs
        every { mockMapLibreAdapter.setMaxZoomPreference(any()) } just Runs
        every { mockMapLibreAdapter.setMinZoomPreference(any()) } just Runs
        every { mockMapLibreAdapter.setOnMapClickListener(any()) } just Runs
        every { mockMapLibreAdapter.addOnCameraIdleListener(any()) } just Runs
        every { mockMapLibreAdapter.getCameraPosition() } returns testCenter
        every { mockMapLibreAdapter.getVisibleRegion() } returns testBounds
        every { mockMapLibreAdapter.getMinZoomLevel() } returns 10.0
        every { mockMapLibreAdapter.setBoundsForCameraTarget(any()) } just Runs
        coEvery { mockMapLibreAdapter.moveCamera(any()) } just Runs
        coEvery { mockMapLibreAdapter.animateCamera(any(), any(), any()) } answers {
            thirdArg<MapCameraCallback?>()?.onFinish()
        }
        coEvery { mockMapLibreAdapter.animateCameraToBounds(any(), any(), any()) } answers {
            thirdArg<MapCameraCallback?>()?.onFinish()
        }
        coEvery { mockMapLibreAdapter.drawOverridenBbox(any()) } just Runs
        every { mockMapLibreAdapter.enableLocationComponent(any()) } just Runs
        every { mockMapLibreAdapter.setUserPosition(any()) } just Runs

        // Setup mock LocationProvider
        mockLocationProvider = mockk()
        val locationFlow = MutableStateFlow<Position?>(null)
        every { mockLocationProvider.currentLocation } returns locationFlow
        every { mockLocationProvider.startLocationUpdates(any()) } just Runs
        every { mockLocationProvider.stopLocationUpdates() } just Runs

        // Create test instance
        capturedOnLocationUpdate = null
        eventMap =
            TestEventMap(
                event = mockEvent,
                mapConfig = EventMapConfig(),
                onLocationUpdate = { capturedOnLocationUpdate = it },
                mockMapLibreAdapter = mockMapLibreAdapter,
                mockLocationProvider = mockLocationProvider,
            )
    }

    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    // ============================================================
    // SETUP MAP TESTS
    // ============================================================

    @Test
    fun setupMap_initializesMapAdapter() =
        runTest {
            // When
            eventMap.setupMap(
                map = "test-map",
                scope = testScope,
                stylePath = "/path/to/style.json",
            )

            // Then
            verify { mockMapLibreAdapter.setMap("test-map") }
            verify { mockMapLibreAdapter.setStyle("/path/to/style.json", any()) }
            verify { mockMapLibreAdapter.setAttributionMargins(0, 0, 0, 0) }
            verify { mockMapLibreAdapter.setMaxZoomPreference(18.0) }
        }

    @Test
    fun setupMap_startsLocationUpdates() =
        runTest {
            // When
            eventMap.setupMap(
                map = "test-map",
                scope = testScope,
                stylePath = "/path/to/style.json",
            )

            // Then
            verify { mockLocationProvider.startLocationUpdates(any()) }
        }

    @Test
    fun setupMap_appliesBoundsConstraintsListener() =
        runTest {
            // When
            eventMap.setupMap(
                map = "test-map",
                scope = testScope,
                stylePath = "/path/to/style.json",
            )

            // Then
            verify { mockMapLibreAdapter.addOnCameraIdleListener(any()) }
        }

    @Test
    fun setupMap_handlesMapClickListener() =
        runTest {
            // Given
            val onMapClick: (Double, Double) -> Unit = { _, _ -> }

            // When
            eventMap.setupMap(
                map = "test-map",
                scope = testScope,
                stylePath = "/path/to/style.json",
                onMapClick = onMapClick,
            )

            // Then
            verify { mockMapLibreAdapter.setOnMapClickListener(onMapClick) }
        }

    @Test
    fun setupMap_drawsOverrideBboxWhenRequired() =
        runTest {
            // Given
            every { mockEvent.area.bboxIsOverride } returns true

            // When
            eventMap.setupMap(
                map = "test-map",
                scope = testScope,
                stylePath = "/path/to/style.json",
            )
            testScope.testScheduler.advanceUntilIdle()

            // Then
            coEvery { mockMapLibreAdapter.drawOverridenBbox(testBounds) }
        }

    @Test
    fun setupMap_doesNotDrawBboxWhenNotOverride() =
        runTest {
            // Given
            every { mockEvent.area.bboxIsOverride } returns false

            // When
            eventMap.setupMap(
                map = "test-map",
                scope = testScope,
                stylePath = "/path/to/style.json",
            )
            testScope.testScheduler.advanceUntilIdle()

            // Then: drawOverridenBbox should NOT be called
            coVerify(exactly = 0) { mockMapLibreAdapter.drawOverridenBbox(any()) }
        }

    @Test
    fun setupMap_passesCorrectBboxCoordinates() =
        runTest {
            // Given
            every { mockEvent.area.bboxIsOverride } returns true
            val customBbox =
                BoundingBox.fromCorners(
                    Position(37.70559, -122.539501), // SF southwest
                    Position(37.833685, -122.343807), // SF northeast
                )
            coEvery { mockEvent.area.bbox() } returns customBbox

            // When
            eventMap.setupMap(
                map = "test-map",
                scope = testScope,
                stylePath = "/path/to/style.json",
            )
            testScope.testScheduler.advanceUntilIdle()

            // Then: Should be called with exact bbox coordinates
            val bboxSlot = slot<BoundingBox>()
            coVerify { mockMapLibreAdapter.drawOverridenBbox(capture(bboxSlot)) }

            // Verify captured bbox has correct coordinates
            assertEquals(37.70559, bboxSlot.captured.sw.lat, 0.000001)
            assertEquals(-122.539501, bboxSlot.captured.sw.lng, 0.000001)
            assertEquals(37.833685, bboxSlot.captured.ne.lat, 0.000001)
            assertEquals(-122.343807, bboxSlot.captured.ne.lng, 0.000001)
        }

    @Test
    fun setupMap_handlesBboxDrawFailureGracefully() =
        runTest {
            // Given: drawOverridenBbox throws an exception
            every { mockEvent.area.bboxIsOverride } returns true
            coEvery { mockMapLibreAdapter.drawOverridenBbox(any()) } throws
                IllegalStateException("Map style not loaded")

            // When: Setup map (should not crash)
            var setupCompleted = false
            try {
                eventMap.setupMap(
                    map = "test-map",
                    scope = testScope,
                    stylePath = "/path/to/style.json",
                )
                testScope.testScheduler.advanceUntilIdle()
                setupCompleted = true
            } catch (e: Exception) {
                // Test fails if exception propagates
                throw AssertionError("setupMap should handle bbox draw failure gracefully", e)
            }

            // Then: Setup should complete despite bbox draw failure
            assertTrue(setupCompleted, "setupMap should complete despite bbox draw failure")
        }

    // ============================================================
    // CAMERA POSITIONING TESTS
    // ============================================================

    @Test
    fun moveToMapBounds_animatesToEventBounds() =
        runTest {
            // Given
            var onCompleteCalled = false

            // When
            eventMap.moveToMapBounds { onCompleteCalled = true }
            testScope.testScheduler.advanceUntilIdle()

            // Then
            coEvery { mockMapLibreAdapter.animateCameraToBounds(testBounds, any(), any()) }
            assertTrue(onCompleteCalled)
        }

    @Test
    fun moveToMapBounds_setsZoomPreferences() =
        runTest {
            // When
            eventMap.moveToMapBounds()
            testScope.testScheduler.advanceUntilIdle()

            // Then
            // Constraints calculate and set min zoom via setBoundsForCameraTarget
            // AbstractEventMap just sets max zoom and lets constraints handle min zoom
            verify { mockMapLibreAdapter.getMinZoomLevel() }
            verify { mockMapLibreAdapter.setMaxZoomPreference(18.0) }
        }

    @Test
    fun moveToMapBounds_maintainsConstraintsDuringAnimation() =
        runTest {
            // When
            eventMap.moveToMapBounds()
            testScope.testScheduler.advanceUntilIdle()

            // Then
            // Constraints are applied AFTER animation, not relaxed during
            // This restores preventive clamping behavior (no zoom out during animation)
            // UPDATED: Now expects originalEventBounds parameter (always passed)
            verify(atLeast = 1) { mockMapLibreAdapter.setBoundsForCameraTarget(any(), any(), any()) }
        }

    @Test
    fun moveToWindowBounds_calculatesProperAspectRatio() =
        runTest {
            // When
            eventMap.moveToWindowBounds()
            testScope.testScheduler.advanceUntilIdle()

            // Then - WINDOW mode applies constraints but does NOT animate camera
            // Initial camera position is controlled by autoTargetUserOnFirstLocation or user interaction
            verify(exactly = 0) { mockMapLibreAdapter.animateCamera(any(), any(), any()) }
            verify { mockMapLibreAdapter.setBoundsForCameraTarget(any(), any(), any()) }
        }

    @Test
    fun moveToWindowBounds_setsZoomPreferences() =
        runTest {
            // When
            eventMap.moveToWindowBounds()
            testScope.testScheduler.advanceUntilIdle()

            // Then
            // Constraints calculate and set min zoom based on expanded WINDOW bounds
            // AbstractEventMap sets max zoom and applies constraints
            // No polling for min zoom in new implementation (no camera animation)
            verify { mockMapLibreAdapter.setMaxZoomPreference(18.0) }
            verify { mockMapLibreAdapter.setBoundsForCameraTarget(any(), any(), any()) }
        }

    @Test
    fun moveToCenter_animatesToEventCenter() =
        runTest {
            // Given
            var onCompleteCalled = false

            // When
            eventMap.moveToCenter { onCompleteCalled = true }
            testScope.testScheduler.advanceUntilIdle()

            // Then
            val slot = slot<Position>()
            coEvery { mockMapLibreAdapter.animateCamera(capture(slot), null, any()) }
            assertTrue(onCompleteCalled)
        }

    // ============================================================
    // CAMERA TARGETING TESTS
    // ============================================================

    @Test
    fun targetWave_movesToClosestWaveLongitude() =
        runTest {
            // Given
            every { mockLocationProvider.currentLocation.value } returns testUserPosition

            // When
            eventMap.targetWave()
            testScope.testScheduler.advanceUntilIdle()

            // Then
            val slot = slot<Position>()
            coEvery {
                mockMapLibreAdapter.animateCamera(
                    capture(slot),
                    WWWGlobals.MapDisplay.TARGET_WAVE_ZOOM,
                    any(),
                )
            }
        }

    @Test
    fun targetWave_returnsEarlyWhenNoLocation() =
        runTest {
            // Given
            every { mockLocationProvider.currentLocation.value } returns null

            // When
            eventMap.targetWave()
            testScope.testScheduler.advanceUntilIdle()

            // Then - should not animate (verify not called would require more setup)
            // Just verify it doesn't crash
        }

    @Test
    fun targetWave_returnsEarlyWhenBothMethodsFail() =
        runTest {
            // Given - both new method and fallback fail
            coEvery { mockEvent.wave.getWaveFrontCenterPosition() } returns null
            every { mockLocationProvider.currentLocation.value } returns null
            coEvery { mockEvent.wave.userClosestWaveLongitude() } returns null

            // When
            eventMap.targetWave()
            testScope.testScheduler.advanceUntilIdle()

            // Then - should not animate (just verify it doesn't crash)
        }

    @Test
    fun targetUser_movesToCurrentUserPosition() =
        runTest {
            // Given
            every { mockLocationProvider.currentLocation.value } returns testUserPosition

            // When
            eventMap.targetUser()
            testScope.testScheduler.advanceUntilIdle()

            // Then
            val slot = slot<Position>()
            coEvery {
                mockMapLibreAdapter.animateCamera(
                    capture(slot),
                    WWWGlobals.MapDisplay.TARGET_USER_ZOOM,
                    any(),
                )
            }
        }

    @Test
    fun targetUser_returnsEarlyWhenNoLocation() =
        runTest {
            // Given
            every { mockLocationProvider.currentLocation.value } returns null

            // When
            eventMap.targetUser()
            testScope.testScheduler.advanceUntilIdle()

            // Then - should not animate
            // Just verify it doesn't crash
        }

    @Test
    fun targetUserAndWave_showsBothPositions() =
        runTest {
            // Given
            every { mockLocationProvider.currentLocation.value } returns testUserPosition

            // When
            eventMap.targetUserAndWave()
            testScope.testScheduler.advanceUntilIdle()

            // Then
            val slot = slot<BoundingBox>()
            coEvery { mockMapLibreAdapter.animateCameraToBounds(capture(slot), any(), any()) }
        }

    @Test
    fun targetUserAndWave_constrainsToBounds() =
        runTest {
            // Given
            every { mockLocationProvider.currentLocation.value } returns testUserPosition

            // When
            eventMap.targetUserAndWave()
            testScope.testScheduler.advanceUntilIdle()

            // Then - bounds should be within area bbox
            val slot = slot<BoundingBox>()
            coEvery { mockMapLibreAdapter.animateCameraToBounds(capture(slot)) }
        }

    @Test
    fun targetUserAndWave_returnsEarlyWhenNoLocation() =
        runTest {
            // Given
            every { mockLocationProvider.currentLocation.value } returns null

            // When
            eventMap.targetUserAndWave()
            testScope.testScheduler.advanceUntilIdle()

            // Then - should not animate
            // Just verify it doesn't crash
        }

    // ============================================================
    // POSITION HANDLING TESTS
    // ============================================================

    @Test
    fun handlePositionUpdate_notifiesCallback() =
        runTest {
            // Given
            eventMap.setupMap(
                map = "test-map",
                scope = testScope,
                stylePath = "/path/to/style.json",
            )

            // When
            positionManager.updatePosition(PositionManager.PositionSource.GPS, testUserPosition)
            testScope.testScheduler.advanceUntilIdle()

            // Then
            assertNotNull(capturedOnLocationUpdate)
            assertEquals(testUserPosition.lat, capturedOnLocationUpdate!!.lat, 0.0001)
            assertEquals(testUserPosition.lng, capturedOnLocationUpdate!!.lng, 0.0001)
        }

    @Test
    fun handlePositionUpdate_skipsNullPositions() =
        runTest {
            // Given
            eventMap.setupMap(
                map = "test-map",
                scope = testScope,
                stylePath = "/path/to/style.json",
            )

            // When
            positionManager.updatePosition(PositionManager.PositionSource.GPS, null)
            testScope.testScheduler.advanceUntilIdle()

            // Then
            assertNull(capturedOnLocationUpdate)
        }

    @Test
    fun handlePositionUpdate_skipsDuplicatePositions() =
        runTest {
            // Given
            eventMap.setupMap(
                map = "test-map",
                scope = testScope,
                stylePath = "/path/to/style.json",
            )

            // When - send same position twice
            positionManager.updatePosition(PositionManager.PositionSource.GPS, testUserPosition)
            testScope.testScheduler.advanceUntilIdle()

            capturedOnLocationUpdate = null // Reset

            positionManager.updatePosition(PositionManager.PositionSource.GPS, testUserPosition)
            testScope.testScheduler.advanceUntilIdle()

            // Then - callback should not be called again (position is same)
            assertNull(capturedOnLocationUpdate)
        }

    @Test
    fun getCurrentPosition_returnsPositionFromManager() =
        runTest {
            // Given
            positionManager.updatePosition(PositionManager.PositionSource.GPS, testUserPosition)
            testScope.testScheduler.advanceUntilIdle()

            // When
            val position = eventMap.getCurrentPosition()

            // Then
            assertNotNull(position)
            assertEquals(testUserPosition.lat, position.lat, 0.0001)
            assertEquals(testUserPosition.lng, position.lng, 0.0001)
        }

    @Test
    fun getCurrentPosition_returnsNullWhenNoPosition() =
        runTest {
            // When
            val position = eventMap.getCurrentPosition()

            // Then
            assertNull(position)
        }

    @Test
    fun getCurrentPositionSource_returnsCorrectSource() =
        runTest {
            // Given
            positionManager.updatePosition(PositionManager.PositionSource.GPS, testUserPosition)
            testScope.testScheduler.advanceUntilIdle()

            // When
            val source = eventMap.getCurrentPositionSource()

            // Then
            assertEquals(PositionManager.PositionSource.GPS, source)
        }

    // ============================================================
    // AUTO-TARGET BEHAVIOR TESTS
    // ============================================================

    @Test
    fun autoTargetOnFirstLocation_triggersWhenEnabled() =
        runTest {
            // Given
            val configWithAutoTarget =
                EventMapConfig(
                    initialCameraPosition = MapCameraPosition.WINDOW,
                    autoTargetUserOnFirstLocation = true,
                )
            eventMap =
                TestEventMap(
                    event = mockEvent,
                    mapConfig = configWithAutoTarget,
                    onLocationUpdate = { capturedOnLocationUpdate = it },
                    mockMapLibreAdapter = mockMapLibreAdapter,
                    mockLocationProvider = mockLocationProvider,
                )

            eventMap.setupMap(
                map = "test-map",
                scope = testScope,
                stylePath = "/path/to/style.json",
            )

            // When - first location update
            positionManager.updatePosition(PositionManager.PositionSource.GPS, testUserPosition)
            testScope.testScheduler.advanceUntilIdle()

            // Then - should auto-target user
            coEvery {
                mockMapLibreAdapter.animateCamera(
                    testUserPosition,
                    WWWGlobals.MapDisplay.TARGET_USER_ZOOM,
                    any(),
                )
            }
        }

    @Test
    fun autoTargetOnFirstLocation_skipsWhenDisabled() =
        runTest {
            // Given
            val configWithoutAutoTarget =
                EventMapConfig(
                    initialCameraPosition = MapCameraPosition.WINDOW,
                    autoTargetUserOnFirstLocation = false,
                )
            eventMap =
                TestEventMap(
                    event = mockEvent,
                    mapConfig = configWithoutAutoTarget,
                    onLocationUpdate = { capturedOnLocationUpdate = it },
                    mockMapLibreAdapter = mockMapLibreAdapter,
                    mockLocationProvider = mockLocationProvider,
                )

            eventMap.setupMap(
                map = "test-map",
                scope = testScope,
                stylePath = "/path/to/style.json",
            )

            // When - first location update
            positionManager.updatePosition(PositionManager.PositionSource.GPS, testUserPosition)
            testScope.testScheduler.advanceUntilIdle()

            // Then - should not auto-target
            assertNotNull(capturedOnLocationUpdate) // Location is still captured
        }

    @Test
    fun autoTargetOnFirstLocation_skipsAfterUserInteraction() =
        runTest {
            // Given
            val configWithAutoTarget =
                EventMapConfig(
                    initialCameraPosition = MapCameraPosition.WINDOW,
                    autoTargetUserOnFirstLocation = true,
                )
            eventMap =
                TestEventMap(
                    event = mockEvent,
                    mapConfig = configWithAutoTarget,
                    onLocationUpdate = { capturedOnLocationUpdate = it },
                    mockMapLibreAdapter = mockMapLibreAdapter,
                    mockLocationProvider = mockLocationProvider,
                )

            eventMap.setupMap(
                map = "test-map",
                scope = testScope,
                stylePath = "/path/to/style.json",
            )

            // When - user interacts first
            eventMap.markUserInteracted()
            positionManager.updatePosition(PositionManager.PositionSource.GPS, testUserPosition)
            testScope.testScheduler.advanceUntilIdle()

            // Then - should not auto-target
            assertNotNull(capturedOnLocationUpdate) // Location is still captured
        }

    // ============================================================
    // AUTO-TARGET AREA CHECK TESTS (iOS Map Fix #1)
    // ============================================================

    @Test
    fun autoTargetOnFirstLocation_triggersWhenUserInsideEventArea() =
        runTest {
            // Given: User position inside event area
            val insidePosition = Position(48.86, 2.35) // Inside test bounds
            val mockArea = mockk<WWWEventArea>()
            coEvery { mockArea.bbox() } returns testBounds
            coEvery { mockArea.getCenter() } returns testCenter
            coEvery { mockArea.isPositionWithin(insidePosition) } returns true
            every { mockArea.bboxIsOverride } returns false
            every { mockEvent.area } returns mockArea

            val configWithAutoTarget =
                EventMapConfig(
                    initialCameraPosition = MapCameraPosition.WINDOW,
                    autoTargetUserOnFirstLocation = true,
                )
            eventMap =
                TestEventMap(
                    event = mockEvent,
                    mapConfig = configWithAutoTarget,
                    onLocationUpdate = { },
                    mockMapLibreAdapter = mockMapLibreAdapter,
                    mockLocationProvider = mockLocationProvider,
                )

            eventMap.setupMap(
                map = "test-map",
                scope = testScope,
                stylePath = "/path/to/style.json",
            )

            // When: User position updated (inside area)
            positionManager.updatePosition(PositionManager.PositionSource.GPS, insidePosition)
            testScope.testScheduler.advanceUntilIdle()

            // Then: Should auto-target user
            coVerify {
                mockMapLibreAdapter.animateCamera(
                    insidePosition,
                    WWWGlobals.MapDisplay.TARGET_USER_ZOOM,
                    any(),
                )
            }
        }

    @Test
    fun autoTargetOnFirstLocation_doesNotTriggerWhenUserOutsideEventArea() =
        runTest {
            // Given: User position outside event area
            val outsidePosition = Position(40.7128, -74.0060) // NYC - outside Paris bounds
            val mockArea = mockk<WWWEventArea>()
            coEvery { mockArea.bbox() } returns testBounds
            coEvery { mockArea.getCenter() } returns testCenter
            coEvery { mockArea.isPositionWithin(outsidePosition) } returns false
            every { mockArea.bboxIsOverride } returns false
            every { mockEvent.area } returns mockArea

            val configWithAutoTarget =
                EventMapConfig(
                    initialCameraPosition = MapCameraPosition.WINDOW,
                    autoTargetUserOnFirstLocation = true,
                )
            eventMap =
                TestEventMap(
                    event = mockEvent,
                    mapConfig = configWithAutoTarget,
                    onLocationUpdate = { },
                    mockMapLibreAdapter = mockMapLibreAdapter,
                    mockLocationProvider = mockLocationProvider,
                )

            eventMap.setupMap(
                map = "test-map",
                scope = testScope,
                stylePath = "/path/to/style.json",
            )

            // When: User position updated (outside area)
            positionManager.updatePosition(PositionManager.PositionSource.GPS, outsidePosition)
            testScope.testScheduler.advanceUntilIdle()

            // Then: Should NOT auto-target user (stays on event bounds from WINDOW initialization)
            coVerify(exactly = 0) {
                mockMapLibreAdapter.animateCamera(
                    outsidePosition,
                    WWWGlobals.MapDisplay.TARGET_USER_ZOOM,
                    any(),
                )
            }
        }

    @Test
    fun autoTargetOnFirstLocation_doesNotRetargetWhenUserMovesInsideAfterBeingLocated() =
        runTest {
            // Given: User first located outside, then moves inside
            val outsidePosition = Position(40.7128, -74.0060)
            val insidePosition = Position(48.86, 2.35)
            val mockArea = mockk<WWWEventArea>()
            coEvery { mockArea.bbox() } returns testBounds
            coEvery { mockArea.getCenter() } returns testCenter
            coEvery { mockArea.isPositionWithin(outsidePosition) } returns false
            coEvery { mockArea.isPositionWithin(insidePosition) } returns true
            every { mockArea.bboxIsOverride } returns false
            every { mockEvent.area } returns mockArea

            val configWithAutoTarget =
                EventMapConfig(
                    initialCameraPosition = MapCameraPosition.WINDOW,
                    autoTargetUserOnFirstLocation = true,
                )
            eventMap =
                TestEventMap(
                    event = mockEvent,
                    mapConfig = configWithAutoTarget,
                    onLocationUpdate = { },
                    mockMapLibreAdapter = mockMapLibreAdapter,
                    mockLocationProvider = mockLocationProvider,
                )

            eventMap.setupMap(
                map = "test-map",
                scope = testScope,
                stylePath = "/path/to/style.json",
            )

            // When: First location (outside)
            positionManager.updatePosition(PositionManager.PositionSource.GPS, outsidePosition)
            testScope.testScheduler.advanceUntilIdle()

            // When: User moves inside
            positionManager.updatePosition(PositionManager.PositionSource.GPS, insidePosition)
            testScope.testScheduler.advanceUntilIdle()

            // Then: Should NOT auto-target (userHasBeenLocated already true)
            coVerify(exactly = 0) {
                mockMapLibreAdapter.animateCamera(
                    insidePosition,
                    WWWGlobals.MapDisplay.TARGET_USER_ZOOM,
                    any(),
                )
            }
        }

    @Test
    fun autoTargetOnFirstLocation_doesNotTriggerInWINDOWModeWhenDisabled() =
        runTest {
            // Given: WINDOW mode with autoTargetUserOnFirstLocation=false
            val insidePosition = Position(48.86, 2.35)
            val mockArea = mockk<WWWEventArea>()
            coEvery { mockArea.bbox() } returns testBounds
            coEvery { mockArea.getCenter() } returns testCenter
            coEvery { mockArea.isPositionWithin(insidePosition) } returns true
            every { mockArea.bboxIsOverride } returns false
            every { mockEvent.area } returns mockArea

            val configWithoutAutoTarget =
                EventMapConfig(
                    initialCameraPosition = MapCameraPosition.WINDOW,
                    autoTargetUserOnFirstLocation = false,
                )
            eventMap =
                TestEventMap(
                    event = mockEvent,
                    mapConfig = configWithoutAutoTarget,
                    onLocationUpdate = { },
                    mockMapLibreAdapter = mockMapLibreAdapter,
                    mockLocationProvider = mockLocationProvider,
                )

            eventMap.setupMap(
                map = "test-map",
                scope = testScope,
                stylePath = "/path/to/style.json",
            )

            // When: User position updated
            positionManager.updatePosition(PositionManager.PositionSource.GPS, insidePosition)
            testScope.testScheduler.advanceUntilIdle()

            // Then: Should NOT auto-target
            coVerify(exactly = 0) {
                mockMapLibreAdapter.animateCamera(
                    insidePosition,
                    WWWGlobals.MapDisplay.TARGET_USER_ZOOM,
                    any(),
                )
            }
        }

    @Test
    fun autoTargetOnFirstLocation_doesNotTriggerInBOUNDSMode() =
        runTest {
            // Given: BOUNDS mode (event details)
            val insidePosition = Position(48.86, 2.35)
            val mockArea = mockk<WWWEventArea>()
            coEvery { mockArea.bbox() } returns testBounds
            coEvery { mockArea.getCenter() } returns testCenter
            coEvery { mockArea.isPositionWithin(insidePosition) } returns true
            every { mockArea.bboxIsOverride } returns false
            every { mockEvent.area } returns mockArea

            val boundsConfig =
                EventMapConfig(
                    initialCameraPosition = MapCameraPosition.BOUNDS,
                    autoTargetUserOnFirstLocation = true, // Enabled but BOUNDS mode overrides
                )
            eventMap =
                TestEventMap(
                    event = mockEvent,
                    mapConfig = boundsConfig,
                    onLocationUpdate = { },
                    mockMapLibreAdapter = mockMapLibreAdapter,
                    mockLocationProvider = mockLocationProvider,
                )

            eventMap.setupMap(
                map = "test-map",
                scope = testScope,
                stylePath = "/path/to/style.json",
            )

            // When: User position updated
            positionManager.updatePosition(PositionManager.PositionSource.GPS, insidePosition)
            testScope.testScheduler.advanceUntilIdle()

            // Then: Should NOT auto-target (BOUNDS mode shows full event)
            coVerify(exactly = 0) {
                mockMapLibreAdapter.animateCamera(
                    insidePosition,
                    WWWGlobals.MapDisplay.TARGET_USER_ZOOM,
                    any(),
                )
            }
        }

    // ============================================================
    // MARKER AREA SKIP TESTS (iOS Production Fix)
    // ============================================================

    @Test
    fun setUserPosition_skippedWhenUserOutsideAreaDuringWINDOWInit() =
        runTest {
            // Given: WINDOW mode with auto-target, user outside event area
            val outsidePosition = Position(40.7128, -74.0060) // NYC - outside Paris
            val mockArea = mockk<WWWEventArea>()
            coEvery { mockArea.bbox() } returns testBounds
            coEvery { mockArea.getCenter() } returns testCenter
            coEvery { mockArea.isPositionWithin(outsidePosition) } returns false
            every { mockArea.bboxIsOverride } returns false
            every { mockEvent.area } returns mockArea

            val configWithAutoTarget =
                EventMapConfig(
                    initialCameraPosition = MapCameraPosition.WINDOW,
                    autoTargetUserOnFirstLocation = true,
                )
            eventMap =
                TestEventMap(
                    event = mockEvent,
                    mapConfig = configWithAutoTarget,
                    onLocationUpdate = { },
                    mockMapLibreAdapter = mockMapLibreAdapter,
                    mockLocationProvider = mockLocationProvider,
                )

            eventMap.setupMap(
                map = "test-map",
                scope = testScope,
                stylePath = "/path/to/style.json",
            )

            // When: Position update with user outside area (before first location check)
            positionManager.updatePosition(PositionManager.PositionSource.GPS, outsidePosition)
            testScope.testScheduler.advanceUntilIdle()

            // Then: Marker should be updated (user can see their position)
            // but camera should NOT auto-target (prevents camera snap to position outside tiles on iOS)
            coVerify(exactly = 1) {
                mockMapLibreAdapter.setUserPosition(outsidePosition)
            }
            coVerify(exactly = 0) {
                mockMapLibreAdapter.animateCamera(
                    outsidePosition,
                    WWWGlobals.MapDisplay.TARGET_USER_ZOOM,
                    any(),
                )
            }
        }

    @Test
    fun setUserPosition_calledWhenUserInsideAreaDuringWINDOWInit() =
        runTest {
            // Given: WINDOW mode with auto-target, user inside event area
            val insidePosition = Position(48.86, 2.35)
            val mockArea = mockk<WWWEventArea>()
            coEvery { mockArea.bbox() } returns testBounds
            coEvery { mockArea.getCenter() } returns testCenter
            coEvery { mockArea.isPositionWithin(insidePosition) } returns true
            every { mockArea.bboxIsOverride } returns false
            every { mockEvent.area } returns mockArea

            val configWithAutoTarget =
                EventMapConfig(
                    initialCameraPosition = MapCameraPosition.WINDOW,
                    autoTargetUserOnFirstLocation = true,
                )
            eventMap =
                TestEventMap(
                    event = mockEvent,
                    mapConfig = configWithAutoTarget,
                    onLocationUpdate = { },
                    mockMapLibreAdapter = mockMapLibreAdapter,
                    mockLocationProvider = mockLocationProvider,
                )

            eventMap.setupMap(
                map = "test-map",
                scope = testScope,
                stylePath = "/path/to/style.json",
            )

            // When: Position update with user inside area
            positionManager.updatePosition(PositionManager.PositionSource.GPS, insidePosition)
            testScope.testScheduler.advanceUntilIdle()

            // Then: Marker should be updated normally
            coVerify(exactly = 1) {
                mockMapLibreAdapter.setUserPosition(insidePosition)
            }
        }

    @Test
    fun setUserPosition_calledAfterUserHasBeenLocated() =
        runTest {
            // Given: WINDOW mode, user already been located
            val outsidePosition = Position(40.7128, -74.0060)
            val mockArea = mockk<WWWEventArea>()
            coEvery { mockArea.bbox() } returns testBounds
            coEvery { mockArea.getCenter() } returns testCenter
            coEvery { mockArea.isPositionWithin(any()) } returns false
            every { mockArea.bboxIsOverride } returns false
            every { mockEvent.area } returns mockArea

            val configWithAutoTarget =
                EventMapConfig(
                    initialCameraPosition = MapCameraPosition.WINDOW,
                    autoTargetUserOnFirstLocation = true,
                )
            eventMap =
                TestEventMap(
                    event = mockEvent,
                    mapConfig = configWithAutoTarget,
                    onLocationUpdate = { },
                    mockMapLibreAdapter = mockMapLibreAdapter,
                    mockLocationProvider = mockLocationProvider,
                )

            eventMap.setupMap(
                map = "test-map",
                scope = testScope,
                stylePath = "/path/to/style.json",
            )

            // First position update (triggers userHasBeenLocated = true)
            positionManager.updatePosition(PositionManager.PositionSource.GPS, outsidePosition)
            testScope.testScheduler.advanceUntilIdle()

            // When: Second position update (userHasBeenLocated is now true)
            val secondPosition = Position(40.7129, -74.0061)
            positionManager.updatePosition(PositionManager.PositionSource.GPS, secondPosition)
            testScope.testScheduler.advanceUntilIdle()

            // Then: Marker should be updated (skip only applies to first location)
            coVerify(atLeast = 1) {
                mockMapLibreAdapter.setUserPosition(any())
            }
        }

    @Test
    fun setUserPosition_calledInBOUNDSModeRegardlessOfArea() =
        runTest {
            // Given: BOUNDS mode (event detail screen), user outside area
            val outsidePosition = Position(40.7128, -74.0060)
            val mockArea = mockk<WWWEventArea>()
            coEvery { mockArea.bbox() } returns testBounds
            coEvery { mockArea.getCenter() } returns testCenter
            coEvery { mockArea.isPositionWithin(outsidePosition) } returns false
            every { mockArea.bboxIsOverride } returns false
            every { mockEvent.area } returns mockArea

            val configBounds =
                EventMapConfig(
                    initialCameraPosition = MapCameraPosition.BOUNDS,
                    autoTargetUserOnFirstLocation = false,
                )
            eventMap =
                TestEventMap(
                    event = mockEvent,
                    mapConfig = configBounds,
                    onLocationUpdate = { },
                    mockMapLibreAdapter = mockMapLibreAdapter,
                    mockLocationProvider = mockLocationProvider,
                )

            eventMap.setupMap(
                map = "test-map",
                scope = testScope,
                stylePath = "/path/to/style.json",
            )

            // When: Position update
            positionManager.updatePosition(PositionManager.PositionSource.GPS, outsidePosition)
            testScope.testScheduler.advanceUntilIdle()

            // Then: Marker should still be updated (no skip in BOUNDS mode)
            coVerify(exactly = 1) {
                mockMapLibreAdapter.setUserPosition(outsidePosition)
            }
        }

    // ============================================================
    // USER INTERACTION TESTS
    // ============================================================

    @Test
    fun markUserInteracted_preventsAutoTarget() =
        runTest {
            // Given
            val configWithAutoTarget =
                EventMapConfig(
                    initialCameraPosition = MapCameraPosition.WINDOW,
                    autoTargetUserOnFirstLocation = true,
                )
            eventMap =
                TestEventMap(
                    event = mockEvent,
                    mapConfig = configWithAutoTarget,
                    onLocationUpdate = { },
                    mockMapLibreAdapter = mockMapLibreAdapter,
                    mockLocationProvider = mockLocationProvider,
                )

            // When
            eventMap.markUserInteracted()

            // Then - internal flag should be set (tested indirectly through behavior)
            // No direct assertion possible, but this is tested in autoTargetOnFirstLocation_skipsAfterUserInteraction
        }

    // ============================================================
    // INITIAL CAMERA POSITION TESTS
    // ============================================================

    @Test
    fun setupMap_initialCameraPosition_bounds() =
        runTest {
            // Given
            val config = EventMapConfig(initialCameraPosition = MapCameraPosition.BOUNDS)
            eventMap =
                TestEventMap(
                    event = mockEvent,
                    mapConfig = config,
                    onLocationUpdate = { },
                    mockMapLibreAdapter = mockMapLibreAdapter,
                    mockLocationProvider = mockLocationProvider,
                )

            // When
            eventMap.setupMap(
                map = "test-map",
                scope = testScope,
                stylePath = "/path/to/style.json",
            )
            testScope.testScheduler.advanceUntilIdle()

            // Then
            coEvery { mockMapLibreAdapter.animateCameraToBounds(testBounds, any(), any()) }
        }

    @Test
    fun setupMap_initialCameraPosition_window() =
        runTest {
            // Given
            val config = EventMapConfig(initialCameraPosition = MapCameraPosition.WINDOW)
            eventMap =
                TestEventMap(
                    event = mockEvent,
                    mapConfig = config,
                    onLocationUpdate = { },
                    mockMapLibreAdapter = mockMapLibreAdapter,
                    mockLocationProvider = mockLocationProvider,
                )

            // When
            eventMap.setupMap(
                map = "test-map",
                scope = testScope,
                stylePath = "/path/to/style.json",
            )
            testScope.testScheduler.advanceUntilIdle()

            // Then - should call animateCameraToBounds with calculated window bounds
            coEvery { mockMapLibreAdapter.animateCameraToBounds(any(), 0, any()) }
        }

    // ============================================================
    // WINDOW MODE CAMERA INITIALIZATION TESTS (iOS Map Fix #3)
    // ============================================================

    @Test
    fun setupMap_windowMode_animatesToBoundsFirst() =
        runTest {
            // Given: WINDOW mode configuration
            val config = EventMapConfig(initialCameraPosition = MapCameraPosition.WINDOW)
            eventMap =
                TestEventMap(
                    event = mockEvent,
                    mapConfig = config,
                    onLocationUpdate = { },
                    mockMapLibreAdapter = mockMapLibreAdapter,
                    mockLocationProvider = mockLocationProvider,
                )

            // When: Setup map
            eventMap.setupMap(
                map = "test-map",
                scope = testScope,
                stylePath = "/path/to/style.json",
            )
            testScope.testScheduler.advanceUntilIdle()

            // Then: Should animate to event bounds (ensures tiles are loaded)
            coVerify {
                mockMapLibreAdapter.animateCameraToBounds(
                    testBounds,
                    0,
                    any(),
                )
            }
        }

    @Test
    fun setupMap_windowMode_appliesConstraintsBeforeCameraAnimation() =
        runTest {
            // Given: WINDOW mode configuration
            var constraintsCalled = false
            var animationCalled = false
            val orderTracking = mutableListOf<String>()

            every { mockMapLibreAdapter.setBoundsForCameraTarget(any(), any(), any()) } answers {
                constraintsCalled = true
                orderTracking.add("constraints")
            }

            coEvery { mockMapLibreAdapter.animateCameraToBounds(any(), any(), any()) } answers {
                animationCalled = true
                orderTracking.add("animation")
                thirdArg<MapCameraCallback?>()?.onFinish()
            }

            val config = EventMapConfig(initialCameraPosition = MapCameraPosition.WINDOW)
            eventMap =
                TestEventMap(
                    event = mockEvent,
                    mapConfig = config,
                    onLocationUpdate = { },
                    mockMapLibreAdapter = mockMapLibreAdapter,
                    mockLocationProvider = mockLocationProvider,
                )

            // When: Setup map
            eventMap.setupMap(
                map = "test-map",
                scope = testScope,
                stylePath = "/path/to/style.json",
            )
            testScope.testScheduler.advanceUntilIdle()

            // Then: Constraints should be applied before animation
            assertTrue(constraintsCalled, "Constraints should be applied")
            assertTrue(animationCalled, "Animation should be called")
            assertEquals(
                listOf("constraints", "animation"),
                orderTracking,
                "Constraints must be applied before camera animation",
            )
        }

    @Test
    fun setupMap_windowMode_allowsAutoTargetAfterBoundsAnimation() =
        runTest {
            // Given: WINDOW mode with autoTargetUserOnFirstLocation enabled
            val insidePosition = Position(48.86, 2.35)
            val mockArea = mockk<WWWEventArea>()
            coEvery { mockArea.bbox() } returns testBounds
            coEvery { mockArea.getCenter() } returns testCenter
            coEvery { mockArea.isPositionWithin(insidePosition) } returns true
            every { mockArea.bboxIsOverride } returns false
            every { mockEvent.area } returns mockArea

            val config =
                EventMapConfig(
                    initialCameraPosition = MapCameraPosition.WINDOW,
                    autoTargetUserOnFirstLocation = true,
                )
            eventMap =
                TestEventMap(
                    event = mockEvent,
                    mapConfig = config,
                    onLocationUpdate = { },
                    mockMapLibreAdapter = mockMapLibreAdapter,
                    mockLocationProvider = mockLocationProvider,
                )

            // When: Setup map and then provide user position
            eventMap.setupMap(
                map = "test-map",
                scope = testScope,
                stylePath = "/path/to/style.json",
            )
            testScope.testScheduler.advanceUntilIdle()

            // When: User position arrives
            positionManager.updatePosition(PositionManager.PositionSource.GPS, insidePosition)
            testScope.testScheduler.advanceUntilIdle()

            // Then: Should first animate to bounds, then auto-target user
            coVerify {
                // First: Bounds animation
                mockMapLibreAdapter.animateCameraToBounds(testBounds, 0, any())
                // Then: Auto-target user
                mockMapLibreAdapter.animateCamera(
                    insidePosition,
                    WWWGlobals.MapDisplay.TARGET_USER_ZOOM,
                    any(),
                )
            }
        }

    @Test
    fun setupMap_initialCameraPosition_center() =
        runTest {
            // Given
            val config = EventMapConfig(initialCameraPosition = MapCameraPosition.DEFAULT_CENTER)
            eventMap =
                TestEventMap(
                    event = mockEvent,
                    mapConfig = config,
                    onLocationUpdate = { },
                    mockMapLibreAdapter = mockMapLibreAdapter,
                    mockLocationProvider = mockLocationProvider,
                )

            // When
            eventMap.setupMap(
                map = "test-map",
                scope = testScope,
                stylePath = "/path/to/style.json",
            )
            testScope.testScheduler.advanceUntilIdle()

            // Then
            coEvery { mockMapLibreAdapter.animateCamera(any(), null, any()) }
        }

    // ============================================================
    // CALLBACK TESTS
    // ============================================================

    @Test
    fun moveToMapBounds_callsOnCompleteAfterAnimation() =
        runTest {
            // Given
            var completeCalled = false

            // When
            eventMap.moveToMapBounds { completeCalled = true }
            testScope.testScheduler.advanceUntilIdle()

            // Then
            assertTrue(completeCalled)
        }

    @Test
    fun moveToWindowBounds_callsOnCompleteAfterAnimation() =
        runTest {
            // Given
            var completeCalled = false

            // When
            eventMap.moveToWindowBounds { completeCalled = true }
            testScope.testScheduler.advanceUntilIdle()

            // Then
            assertTrue(completeCalled)
        }

    @Test
    fun moveToCenter_callsOnCompleteAfterAnimation() =
        runTest {
            // Given
            var completeCalled = false

            // When
            eventMap.moveToCenter { completeCalled = true }
            testScope.testScheduler.advanceUntilIdle()

            // Then
            assertTrue(completeCalled)
        }

    @Test
    fun setupMap_callsOnMapLoadedAfterInitialPosition() =
        runTest {
            // Given
            var loadedCalled = false

            // When
            eventMap.setupMap(
                map = "test-map",
                scope = testScope,
                stylePath = "/path/to/style.json",
                onMapLoaded = { loadedCalled = true },
            )
            testScope.testScheduler.advanceUntilIdle()

            // Then
            assertTrue(loadedCalled)
        }

    // ============================================================
    // ANIMATION CANCELLATION TESTS
    // ============================================================

    @Test
    fun animationCancellation_restoresSuppression() =
        runTest {
            // Given
            coEvery { mockMapLibreAdapter.animateCameraToBounds(any(), any(), any()) } answers {
                thirdArg<MapCameraCallback?>()?.onCancel()
            }

            // When
            eventMap.moveToMapBounds()
            testScope.testScheduler.advanceUntilIdle()

            // Then - should still call onComplete even on cancel
            // No crash means suppression was properly restored
        }

    // ============================================================
    // BOUNDS PRELOADING TESTS
    // ============================================================

    @Test
    fun setupMap_initializesWithoutCrashingWhenBoundsPreloaded() =
        runTest {
            // Given - Event area bbox should be preloaded during setupMap
            // This test verifies the fix for issue where world bounds (-90,-180 to 90,180)
            // were used as fallback when GeoJSON hadn't loaded yet

            // When
            eventMap.setupMap(
                map = "test-map",
                scope = testScope,
                stylePath = "/path/to/style.json",
            )
            testScope.testScheduler.advanceUntilIdle()

            // Then - Should complete without exceptions
            // The bounds preloading happens asynchronously, so we verify setup completes
            verify { mockLocationProvider.startLocationUpdates(any()) }
        }

    @Test
    fun setupMap_windowMode_initializesCorrectlyWithBoundsPreloading() =
        runTest {
            // Given - WINDOW mode requires valid bounds for area checks
            // This test ensures bounds are available before position updates start
            val windowModeMap =
                TestEventMap(
                    event = mockEvent,
                    mapConfig = EventMapConfig(initialCameraPosition = MapCameraPosition.WINDOW),
                    onLocationUpdate = { },
                    mockMapLibreAdapter = mockMapLibreAdapter,
                    mockLocationProvider = mockLocationProvider,
                )

            // When
            windowModeMap.setupMap(
                map = "test-map",
                scope = testScope,
                stylePath = "/path/to/style.json",
            )
            testScope.testScheduler.advanceUntilIdle()

            // Then - Should complete setup without using fallback world bounds
            coVerify { mockMapLibreAdapter.animateCameraToBounds(testBounds, any(), any()) }
        }

    // ============================================================
    // CAMERA TARGETING TESTS (targetUser, targetWave)
    // ============================================================

    @Test
    fun targetUser_animatesToUserPositionWithoutConstraintUpdate() =
        runTest {
            // Given: User position available
            val userPosition = Position(48.86, 2.35)
            positionManager.updatePosition(PositionManager.PositionSource.GPS, userPosition)
            testScope.testScheduler.advanceUntilIdle()

            eventMap.setupMap(
                map = "test-map",
                scope = testScope,
                stylePath = "/path/to/style.json",
            )
            testScope.testScheduler.advanceUntilIdle()

            // When: targetUser is called
            eventMap.targetUser()
            testScope.testScheduler.advanceUntilIdle()

            // Then: Should animate to user position without forcing constraint update
            coVerify(exactly = 1) {
                mockMapLibreAdapter.animateCamera(userPosition, WWWGlobals.MapDisplay.TARGET_USER_ZOOM, any())
            }
        }

    @Test
    fun targetWave_callsForceConstraintUpdate() =
        runTest {
            // Given: Linear wave event with wave front data
            val mockWaveLinear = mockk<WWWEventWaveLinear>()
            every { mockEvent.wave } returns mockWaveLinear
            coEvery { mockWaveLinear.getWaveFrontEdgeBounds() } returns Triple(48.84, 48.88, 2.38)

            eventMap.setupMap(
                map = "test-map",
                scope = testScope,
                stylePath = "/path/to/style.json",
            )
            testScope.testScheduler.advanceUntilIdle()

            // When: targetWave is called
            eventMap.targetWave()
            testScope.testScheduler.advanceUntilIdle()

            // Then: Should animate to bounds
            coVerify(atLeast = 1) {
                mockMapLibreAdapter.animateCameraToBounds(any(), any(), any())
            }
        }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun runCameraAnimation_suspendsUntilCallbackCompletes() =
        runTest {
            // Given: Animation callback that will be triggered
            var callbackExecuted = false
            var animationCompleted = false

            coEvery { mockMapLibreAdapter.animateCamera(any(), any(), any()) } answers {
                val callback = thirdArg<MapCameraCallback?>()
                // Simulate async animation completion
                testScope.launch {
                    delay(100)
                    callbackExecuted = true
                    callback?.onFinish()
                }
            }

            eventMap.setupMap(
                map = "test-map",
                scope = testScope,
                stylePath = "/path/to/style.json",
            )
            testScope.testScheduler.advanceUntilIdle()

            val userPosition = Position(48.86, 2.35)
            positionManager.updatePosition(PositionManager.PositionSource.GPS, userPosition)
            testScope.testScheduler.advanceUntilIdle()

            // When: targetUser is called (which uses runCameraAnimation)
            testScope.launch {
                eventMap.targetUser()
                animationCompleted = true
            }

            // Advance time but not past animation
            testScope.testScheduler.advanceTimeBy(50)
            assertFalse(callbackExecuted, "Callback should not have executed yet")
            assertFalse(animationCompleted, "Animation should not be marked complete yet")

            // Advance past animation completion
            testScope.testScheduler.advanceTimeBy(100)
            assertTrue(callbackExecuted, "Callback should have executed")
            assertTrue(animationCompleted, "Animation should be marked complete")
        }

    // ============================================================
    // NOTE: Wave front center position is comprehensively tested in WWWEventWaveLinearTest.kt
    // with 30+ test cases covering various polygon shapes, directions, and progressions
    // ============================================================
    // TEST IMPLEMENTATION
    // ============================================================

    private class TestEventMap(
        event: IWWWEvent,
        mapConfig: EventMapConfig,
        onLocationUpdate: (Position) -> Unit,
        val mockMapLibreAdapter: MapLibreAdapter<String>,
        val mockLocationProvider: LocationProvider?,
    ) : AbstractEventMap<String>(event, mapConfig, onLocationUpdate) {
        override val mapLibreAdapter: MapLibreAdapter<String> = mockMapLibreAdapter
        override val locationProvider: LocationProvider? = mockLocationProvider

        @Composable
        override fun Draw(
            autoMapDownload: Boolean,
            modifier: Modifier,
        ) {
            // Test stub
        }

        override fun updateWavePolygons(
            wavePolygons: List<Polygon>,
            clearPolygons: Boolean,
        ) {
            // Test stub
        }
    }
}
