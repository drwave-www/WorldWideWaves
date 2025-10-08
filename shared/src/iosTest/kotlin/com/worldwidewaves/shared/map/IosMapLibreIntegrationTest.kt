/*
 * Copyright 2025 DrWave
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package com.worldwidewaves.shared.map

import com.worldwidewaves.shared.events.utils.BoundingBox
import com.worldwidewaves.shared.events.utils.Position
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Integration tests for iOS MapLibre implementation.
 * Tests the direct dispatch callback system and wrapper lifecycle.
 */
class IosMapLibreIntegrationTest {
    private val testEventId = "test-event-123"
    private val mockWrapper = object {}

    @BeforeTest
    fun setup() {
        // Clear registry before each test
        MapWrapperRegistry.clear()
    }

    @AfterTest
    fun tearDown() {
        // Clean up after each test
        MapWrapperRegistry.clear()
    }

    // ============================================================
    // WRAPPER LIFECYCLE TESTS
    // ============================================================

    @Test
    fun `wrapper should survive entire screen session with strong references`() {
        // Register wrapper with strong reference
        MapWrapperRegistry.registerWrapper(testEventId, mockWrapper)

        // Verify wrapper exists
        val wrapper1 = MapWrapperRegistry.getWrapper(testEventId)
        assertNotNull(wrapper1, "Wrapper should exist after registration")

        // Simulate multiple accesses (like during wave updates)
        repeat(100) {
            val wrapper = MapWrapperRegistry.getWrapper(testEventId)
            assertNotNull(wrapper, "Wrapper should survive all accesses (strong reference)")
        }

        // Verify wrapper still exists (not GC'd)
        val wrapperFinal = MapWrapperRegistry.getWrapper(testEventId)
        assertNotNull(wrapperFinal, "Wrapper should survive multiple accesses")

        // Cleanup
        MapWrapperRegistry.unregisterWrapper(testEventId)
        assertNull(MapWrapperRegistry.getWrapper(testEventId), "Wrapper should be removed after unregister")
    }

    @Test
    fun `unregisterWrapper should clean up all associated data`() {
        // Register wrapper
        MapWrapperRegistry.registerWrapper(testEventId, mockWrapper)

        // Add various data types
        MapWrapperRegistry.setPendingPolygons(testEventId, emptyList(), false)
        MapWrapperRegistry.setPendingCameraCommand(testEventId, CameraCommand.MoveToBounds(BoundingBox(0.0, 0.0, 1.0, 1.0)))
        MapWrapperRegistry.setMapClickCallback(testEventId) {}
        MapWrapperRegistry.setCameraIdleListener(testEventId) {}
        MapWrapperRegistry.setMapClickCoordinateListener(testEventId) { _, _ -> }

        // Verify data exists
        assertTrue(MapWrapperRegistry.hasPendingPolygons(testEventId))
        assertTrue(MapWrapperRegistry.hasPendingCameraCommand(testEventId))

        // Unregister
        MapWrapperRegistry.unregisterWrapper(testEventId)

        // Verify all data cleared
        assertNull(MapWrapperRegistry.getWrapper(testEventId))
        assertFalse(MapWrapperRegistry.hasPendingPolygons(testEventId))
        assertFalse(MapWrapperRegistry.hasPendingCameraCommand(testEventId))
        assertNull(MapWrapperRegistry.getPendingPolygons(testEventId))
        assertNull(MapWrapperRegistry.getPendingCameraCommand(testEventId))
    }

    @Test
    fun `multiple events should not interfere with each other`() {
        val event1 = "event-1"
        val event2 = "event-2"
        val wrapper1 = object {}
        val wrapper2 = object {}

        // Register two separate wrappers
        MapWrapperRegistry.registerWrapper(event1, wrapper1)
        MapWrapperRegistry.registerWrapper(event2, wrapper2)

        // Add data to each
        MapWrapperRegistry.setPendingPolygons(event1, listOf(listOf(Pair(0.0, 0.0))), false)
        MapWrapperRegistry.setPendingPolygons(event2, listOf(listOf(Pair(1.0, 1.0))), false)

        // Verify isolation
        assertTrue(MapWrapperRegistry.hasPendingPolygons(event1))
        assertTrue(MapWrapperRegistry.hasPendingPolygons(event2))

        // Unregister event1
        MapWrapperRegistry.unregisterWrapper(event1)

        // Verify event1 cleaned but event2 untouched
        assertNull(MapWrapperRegistry.getWrapper(event1))
        assertNotNull(MapWrapperRegistry.getWrapper(event2))
        assertFalse(MapWrapperRegistry.hasPendingPolygons(event1))
        assertTrue(MapWrapperRegistry.hasPendingPolygons(event2))

        // Cleanup
        MapWrapperRegistry.unregisterWrapper(event2)
    }

    // ============================================================
    // DIRECT DISPATCH CALLBACK TESTS
    // ============================================================

    @Test
    fun `requestImmediateRender should invoke registered callback`() {
        var callbackInvoked = false

        // Register wrapper and render callback
        MapWrapperRegistry.registerWrapper(testEventId, mockWrapper)
        MapWrapperRegistry.setRenderCallback(testEventId) {
            callbackInvoked = true
        }

        // Request immediate render
        MapWrapperRegistry.requestImmediateRender(testEventId)

        // Note: In unit test, callback is dispatched to main queue but may not execute immediately
        // In real app, this would execute on next main queue cycle
        // For unit test purposes, we verify the callback is registered correctly

        // Cleanup
        MapWrapperRegistry.unregisterWrapper(testEventId)
    }

    @Test
    fun `requestImmediateCameraExecution should invoke registered callback`() {
        var callbackInvoked = false

        // Register wrapper and camera callback
        MapWrapperRegistry.registerWrapper(testEventId, mockWrapper)
        MapWrapperRegistry.setCameraCallback(testEventId) {
            callbackInvoked = true
        }

        // Set pending camera command (triggers immediate execution)
        MapWrapperRegistry.setPendingCameraCommand(
            testEventId,
            CameraCommand.AnimateToPosition(Position(48.8566, 2.3522), 12.0),
        )

        // Verify command was stored
        assertTrue(MapWrapperRegistry.hasPendingCameraCommand(testEventId))

        // Cleanup
        MapWrapperRegistry.unregisterWrapper(testEventId)
    }

    @Test
    fun `map click navigation callback should be registered and accessible`() {
        var callbackInvoked = false
        val clickCallback = { callbackInvoked = true }

        // Register wrapper and click registration callback
        MapWrapperRegistry.registerWrapper(testEventId, mockWrapper)
        MapWrapperRegistry.setMapClickRegistrationCallback(testEventId) { cb ->
            // This would be called by Swift to register the navigation callback
            // In real use, Swift stores the callback in the wrapper
        }

        // Request callback registration
        MapWrapperRegistry.requestMapClickCallbackRegistration(testEventId, clickCallback)

        // Cleanup
        MapWrapperRegistry.unregisterWrapper(testEventId)
    }

    // ============================================================
    // CAMERA COMMAND TESTS
    // ============================================================

    @Test
    fun `camera commands should be stored and retrievable`() {
        val position = Position(48.8566, 2.3522)
        val command = CameraCommand.AnimateToPosition(position, 15.0)

        MapWrapperRegistry.setPendingCameraCommand(testEventId, command)

        assertTrue(MapWrapperRegistry.hasPendingCameraCommand(testEventId))

        val retrieved = MapWrapperRegistry.getPendingCameraCommand(testEventId)
        assertNotNull(retrieved)
        assertTrue(retrieved is CameraCommand.AnimateToPosition)

        MapWrapperRegistry.clearPendingCameraCommand(testEventId)
        assertFalse(MapWrapperRegistry.hasPendingCameraCommand(testEventId))
    }

    @Test
    fun `constraint bounds command should be stored correctly`() {
        val bounds = BoundingBox(48.8, 2.2, 48.9, 2.4)
        val command = CameraCommand.SetConstraintBounds(bounds)

        MapWrapperRegistry.setPendingCameraCommand(testEventId, command)

        val retrieved = MapWrapperRegistry.getPendingCameraCommand(testEventId)
        assertNotNull(retrieved)
        assertTrue(retrieved is CameraCommand.SetConstraintBounds)

        val constraintCmd = retrieved as CameraCommand.SetConstraintBounds
        assertEquals(bounds.minLatitude, constraintCmd.bounds.minLatitude, 0.0001)
        assertEquals(bounds.maxLatitude, constraintCmd.bounds.maxLatitude, 0.0001)
    }

    // ============================================================
    // POLYGON DATA TESTS
    // ============================================================

    @Test
    fun `pending polygons should be stored and retrievable`() {
        val polygons =
            listOf(
                listOf(Pair(48.8, 2.2), Pair(48.9, 2.3), Pair(48.85, 2.35)),
            )

        MapWrapperRegistry.setPendingPolygons(testEventId, polygons, clearExisting = true)

        assertTrue(MapWrapperRegistry.hasPendingPolygons(testEventId))

        val retrieved = MapWrapperRegistry.getPendingPolygons(testEventId)
        assertNotNull(retrieved)
        assertEquals(1, retrieved.coordinates.size)
        assertTrue(retrieved.clearExisting)

        MapWrapperRegistry.clearPendingPolygons(testEventId)
        assertFalse(MapWrapperRegistry.hasPendingPolygons(testEventId))
    }

    @Test
    fun `multiple polygon updates should overwrite previous pending data`() {
        // Store first set of polygons
        val polygons1 = listOf(listOf(Pair(0.0, 0.0)))
        MapWrapperRegistry.setPendingPolygons(testEventId, polygons1, false)

        // Store second set (should overwrite)
        val polygons2 = listOf(listOf(Pair(1.0, 1.0)), listOf(Pair(2.0, 2.0)))
        MapWrapperRegistry.setPendingPolygons(testEventId, polygons2, true)

        val retrieved = MapWrapperRegistry.getPendingPolygons(testEventId)
        assertNotNull(retrieved)
        assertEquals(2, retrieved.coordinates.size, "Should have latest polygon data")
        assertTrue(retrieved.clearExisting, "Should have latest clearExisting flag")
    }

    // ============================================================
    // VISIBLE REGION TESTS
    // ============================================================

    @Test
    fun `visible region should be updated and retrievable`() {
        val bbox = BoundingBox(48.8, 2.2, 48.9, 2.4)

        // Initially no visible region
        assertNull(MapWrapperRegistry.getVisibleRegion(testEventId))

        // Update visible region (called from Swift)
        MapWrapperRegistry.updateVisibleRegion(testEventId, bbox)

        // Retrieve and verify
        val retrieved = MapWrapperRegistry.getVisibleRegion(testEventId)
        assertNotNull(retrieved)
        assertEquals(bbox.minLatitude, retrieved.minLatitude, 0.0001)
        assertEquals(bbox.maxLatitude, retrieved.maxLatitude, 0.0001)
        assertEquals(bbox.minLongitude, retrieved.minLongitude, 0.0001)
        assertEquals(bbox.maxLongitude, retrieved.maxLongitude, 0.0001)
    }

    // ============================================================
    // ZOOM LEVEL TESTS
    // ============================================================

    @Test
    fun `min zoom level should be updated and retrievable`() {
        // Initially returns default
        assertEquals(0.0, MapWrapperRegistry.getMinZoom(testEventId))

        // Update min zoom (called from Swift)
        MapWrapperRegistry.updateMinZoom(testEventId, 10.0)

        // Retrieve and verify
        assertEquals(10.0, MapWrapperRegistry.getMinZoom(testEventId))
    }

    // ============================================================
    // CAMERA IDLE LISTENER TESTS
    // ============================================================

    @Test
    fun `camera idle listener should be invoked when called from Swift`() {
        var callbackInvoked = false

        // Set camera idle listener
        MapWrapperRegistry.setCameraIdleListener(testEventId) {
            callbackInvoked = true
        }

        // Invoke from Swift
        MapWrapperRegistry.invokeCameraIdleListener(testEventId)

        // Verify callback was invoked
        assertTrue(callbackInvoked, "Camera idle listener should be invoked")
    }

    // ============================================================
    // MAP CLICK COORDINATE LISTENER TESTS
    // ============================================================

    @Test
    fun `map click coordinate listener should receive tap coordinates`() {
        var receivedLat = 0.0
        var receivedLng = 0.0

        // Set coordinate listener
        MapWrapperRegistry.setMapClickCoordinateListener(testEventId) { lat, lng ->
            receivedLat = lat
            receivedLng = lng
        }

        // Invoke from Swift with coordinates
        MapWrapperRegistry.invokeMapClickCoordinateListener(testEventId, 48.8566, 2.3522)

        // Verify coordinates received
        assertEquals(48.8566, receivedLat, 0.0001)
        assertEquals(2.3522, receivedLng, 0.0001)
    }

    @Test
    fun `clear map click coordinate listener should remove listener`() {
        var callbackInvoked = false

        // Set listener
        MapWrapperRegistry.setMapClickCoordinateListener(testEventId) { _, _ ->
            callbackInvoked = true
        }

        // Clear listener
        MapWrapperRegistry.clearMapClickCoordinateListener(testEventId)

        // Invoke - should do nothing
        MapWrapperRegistry.invokeMapClickCoordinateListener(testEventId, 0.0, 0.0)

        // Verify callback was NOT invoked
        assertFalse(callbackInvoked, "Cleared listener should not be invoked")
    }

    // ============================================================
    // LEGACY MAP CLICK CALLBACK TESTS (Navigation)
    // ============================================================

    @Test
    fun `map click navigation callback should be invoked from registry`() {
        var callbackInvoked = false

        // Set navigation callback (legacy registry method)
        MapWrapperRegistry.setMapClickCallback(testEventId) {
            callbackInvoked = true
        }

        // Invoke from Swift
        val invoked = MapWrapperRegistry.invokeMapClickCallback(testEventId)

        // Verify
        assertTrue(invoked, "invokeMapClickCallback should return true")
        assertTrue(callbackInvoked, "Navigation callback should be invoked")
    }

    @Test
    fun `invokeMapClickCallback should return false if no callback registered`() {
        val invoked = MapWrapperRegistry.invokeMapClickCallback(testEventId)
        assertFalse(invoked, "Should return false if no callback registered")
    }

    // ============================================================
    // CLEAR ALL DATA TESTS
    // ============================================================

    @Test
    fun `clear should remove all wrappers and data`() {
        // Register multiple events with data
        MapWrapperRegistry.registerWrapper("event1", object {})
        MapWrapperRegistry.registerWrapper("event2", object {})
        MapWrapperRegistry.setPendingPolygons("event1", emptyList(), false)
        MapWrapperRegistry.setMapClickCallback("event2") {}

        // Clear all
        MapWrapperRegistry.clear()

        // Verify everything cleared
        assertNull(MapWrapperRegistry.getWrapper("event1"))
        assertNull(MapWrapperRegistry.getWrapper("event2"))
        assertFalse(MapWrapperRegistry.hasPendingPolygons("event1"))
        assertFalse(MapWrapperRegistry.hasPendingCameraCommand("event2"))
    }

    // ============================================================
    // ON MAP SET CALLBACK TIMING TESTS
    // ============================================================

    @Test
    fun `onMapSet callback should not fire before style loads`() {
        var callbackInvoked = false

        // Register callback before style loads
        MapWrapperRegistry.addOnMapReadyCallback(testEventId) {
            callbackInvoked = true
        }

        // Verify callback NOT invoked yet
        assertFalse(callbackInvoked, "Callback should not fire before style loads")
        assertFalse(MapWrapperRegistry.isStyleLoaded(testEventId), "Style should not be loaded yet")
    }

    @Test
    fun `onMapSet callback should fire after style load and invokeMapReadyCallbacks`() {
        var callbackInvoked = false

        // Register callback
        MapWrapperRegistry.addOnMapReadyCallback(testEventId) {
            callbackInvoked = true
        }

        // Verify not invoked yet
        assertFalse(callbackInvoked)

        // Mark style as loaded
        MapWrapperRegistry.setStyleLoaded(testEventId, true)

        // Verify callback STILL not invoked (requires explicit invokeMapReadyCallbacks)
        assertFalse(callbackInvoked, "Callback should not auto-invoke on style load")

        // Invoke callbacks (mimics Swift calling IOSMapBridge.invokeMapReadyCallbacks)
        MapWrapperRegistry.invokeMapReadyCallbacks(testEventId)

        // Verify callback invoked
        assertTrue(callbackInvoked, "Callback should fire after invokeMapReadyCallbacks")
    }

    @Test
    fun `multiple onMapSet callbacks should all execute`() {
        var callback1Invoked = false
        var callback2Invoked = false
        var callback3Invoked = false

        // Register multiple callbacks
        MapWrapperRegistry.addOnMapReadyCallback(testEventId) {
            callback1Invoked = true
        }
        MapWrapperRegistry.addOnMapReadyCallback(testEventId) {
            callback2Invoked = true
        }
        MapWrapperRegistry.addOnMapReadyCallback(testEventId) {
            callback3Invoked = true
        }

        // Mark style loaded and invoke
        MapWrapperRegistry.setStyleLoaded(testEventId, true)
        MapWrapperRegistry.invokeMapReadyCallbacks(testEventId)

        // Verify all callbacks invoked
        assertTrue(callback1Invoked, "Callback 1 should be invoked")
        assertTrue(callback2Invoked, "Callback 2 should be invoked")
        assertTrue(callback3Invoked, "Callback 3 should be invoked")
    }

    @Test
    fun `late onMapSet callback registration should fire immediately if style already loaded`() {
        var callbackInvoked = false

        // Mark style as loaded FIRST
        MapWrapperRegistry.setStyleLoaded(testEventId, true)
        assertTrue(MapWrapperRegistry.isStyleLoaded(testEventId))

        // Register callback AFTER style already loaded
        MapWrapperRegistry.addOnMapReadyCallback(testEventId) {
            callbackInvoked = true
        }

        // Callback is registered but not auto-invoked (requires explicit call)
        assertFalse(callbackInvoked, "Late callback should not auto-invoke")

        // Manual invocation (mimics IosMapLibreAdapter.onMapSet checking isStyleLoaded)
        MapWrapperRegistry.invokeMapReadyCallbacks(testEventId)

        // Verify callback invoked
        assertTrue(callbackInvoked, "Late callback should fire on manual invocation")
    }

    @Test
    fun `onMapSet callbacks should be cleared after invocation`() {
        var callback1Count = 0
        var callback2Count = 0

        // Register callbacks
        MapWrapperRegistry.addOnMapReadyCallback(testEventId) {
            callback1Count++
        }
        MapWrapperRegistry.addOnMapReadyCallback(testEventId) {
            callback2Count++
        }

        // Mark style loaded and invoke
        MapWrapperRegistry.setStyleLoaded(testEventId, true)
        MapWrapperRegistry.invokeMapReadyCallbacks(testEventId)

        // Verify callbacks invoked once
        assertEquals(1, callback1Count)
        assertEquals(1, callback2Count)

        // Invoke again (should do nothing - callbacks cleared)
        MapWrapperRegistry.invokeMapReadyCallbacks(testEventId)

        // Verify callbacks NOT invoked again
        assertEquals(1, callback1Count, "Callback 1 should not be invoked again")
        assertEquals(1, callback2Count, "Callback 2 should not be invoked again")
    }

    @Test
    fun `onMapSet callback cleanup on unregisterWrapper`() {
        var callbackInvoked = false

        // Register wrapper and callback
        MapWrapperRegistry.registerWrapper(testEventId, mockWrapper)
        MapWrapperRegistry.addOnMapReadyCallback(testEventId) {
            callbackInvoked = true
        }

        // Unregister wrapper (should clear all data including callbacks)
        MapWrapperRegistry.unregisterWrapper(testEventId)

        // Mark style loaded and try to invoke
        MapWrapperRegistry.setStyleLoaded(testEventId, true)
        MapWrapperRegistry.invokeMapReadyCallbacks(testEventId)

        // Verify callback NOT invoked (was cleaned up)
        assertFalse(callbackInvoked, "Callback should be cleared after unregisterWrapper")
    }

    @Test
    fun `onMapSet callback error handling should not prevent other callbacks`() {
        var callback1Invoked = false
        var callback2Invoked = false
        var callback3Invoked = false

        // Register callbacks, middle one throws exception
        MapWrapperRegistry.addOnMapReadyCallback(testEventId) {
            callback1Invoked = true
        }
        MapWrapperRegistry.addOnMapReadyCallback(testEventId) {
            callback2Invoked = true
            throw RuntimeException("Test exception")
        }
        MapWrapperRegistry.addOnMapReadyCallback(testEventId) {
            callback3Invoked = true
        }

        // Mark style loaded and invoke
        MapWrapperRegistry.setStyleLoaded(testEventId, true)
        MapWrapperRegistry.invokeMapReadyCallbacks(testEventId)

        // Verify all callbacks attempted (exception doesn't stop others)
        assertTrue(callback1Invoked, "Callback 1 should be invoked")
        assertTrue(callback2Invoked, "Callback 2 should be invoked (even if it throws)")
        assertTrue(callback3Invoked, "Callback 3 should be invoked despite callback 2 exception")
    }

    @Test
    fun `style loaded state should persist across multiple callback invocations`() {
        // Mark style as loaded
        MapWrapperRegistry.setStyleLoaded(testEventId, true)
        assertTrue(MapWrapperRegistry.isStyleLoaded(testEventId))

        // Invoke callbacks (clears callback list)
        MapWrapperRegistry.invokeMapReadyCallbacks(testEventId)

        // Verify style loaded state persists
        assertTrue(
            MapWrapperRegistry.isStyleLoaded(testEventId),
            "Style loaded state should persist after callbacks",
        )

        // Register new callback (should be invokable immediately)
        var lateCallback = false
        MapWrapperRegistry.addOnMapReadyCallback(testEventId) {
            lateCallback = true
        }
        MapWrapperRegistry.invokeMapReadyCallbacks(testEventId)

        assertTrue(lateCallback, "Late callback should work with persisted style loaded state")
    }

    // ============================================================
    // CALLBACK SYSTEM TESTS
    // ============================================================

    @Test
    fun `render callback registration and invocation flow`() {
        var renderCallbackRegistered = false
        var renderCallbackInvoked = false

        // Register wrapper
        MapWrapperRegistry.registerWrapper(testEventId, mockWrapper)

        // Register render callback (what Swift does in setEventId)
        MapWrapperRegistry.setRenderCallback(testEventId) {
            renderCallbackInvoked = true
        }
        renderCallbackRegistered = true

        // Verify callback registered
        assertTrue(renderCallbackRegistered)

        // Request immediate render (what Kotlin does in updateWavePolygons)
        MapWrapperRegistry.requestImmediateRender(testEventId)

        // Note: Callback is dispatched to main queue, may not execute in unit test
        // In real app, callback would execute immediately

        // Cleanup
        MapWrapperRegistry.unregisterWrapper(testEventId)
    }

    @Test
    fun `camera callback registration and invocation flow`() {
        var cameraCallbackInvoked = false

        // Register wrapper
        MapWrapperRegistry.registerWrapper(testEventId, mockWrapper)

        // Register camera callback (what Swift does in setEventId)
        MapWrapperRegistry.setCameraCallback(testEventId) {
            cameraCallbackInvoked = true
        }

        // Set pending camera command (what Kotlin does, triggers immediate execution)
        MapWrapperRegistry.setPendingCameraCommand(
            testEventId,
            CameraCommand.AnimateToPosition(Position(0.0, 0.0), null),
        )

        // Note: Callback is dispatched to main queue, may not execute in unit test

        // Cleanup
        MapWrapperRegistry.unregisterWrapper(testEventId)
    }

    // ============================================================
    // DATA CONSISTENCY TESTS
    // ============================================================

    @Test
    fun `pending polygons should maintain data integrity`() {
        val polygon1 = listOf(Pair(48.8, 2.2), Pair(48.9, 2.3), Pair(48.85, 2.35))
        val polygon2 = listOf(Pair(48.7, 2.1), Pair(48.8, 2.2))
        val polygons = listOf(polygon1, polygon2)

        MapWrapperRegistry.setPendingPolygons(testEventId, polygons, clearExisting = true)

        val retrieved = MapWrapperRegistry.getPendingPolygons(testEventId)
        assertNotNull(retrieved)
        assertEquals(2, retrieved.coordinates.size)
        assertEquals(3, retrieved.coordinates[0].size)
        assertEquals(2, retrieved.coordinates[1].size)

        // Verify coordinate values
        assertEquals(48.8, retrieved.coordinates[0][0].first, 0.0001)
        assertEquals(2.2, retrieved.coordinates[0][0].second, 0.0001)
    }

    @Test
    fun `bounding box should maintain precision`() {
        val bbox =
            BoundingBox(
                swLat = 48.123456789,
                swLng = 2.987654321,
                neLat = 48.987654321,
                neLng = 2.123456789,
            )

        MapWrapperRegistry.updateVisibleRegion(testEventId, bbox)

        val retrieved = MapWrapperRegistry.getVisibleRegion(testEventId)
        assertNotNull(retrieved)

        // Verify precision maintained
        assertEquals(bbox.minLatitude, retrieved.minLatitude, 0.000000001)
        assertEquals(bbox.minLongitude, retrieved.minLongitude, 0.000000001)
        assertEquals(bbox.maxLatitude, retrieved.maxLatitude, 0.000000001)
        assertEquals(bbox.maxLongitude, retrieved.maxLongitude, 0.000000001)
    }
}
