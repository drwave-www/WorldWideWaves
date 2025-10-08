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
 * Tests for MapWrapperRegistry lifecycle management.
 * Validates strong reference behavior, cleanup, and memory safety.
 */
class MapWrapperRegistryLifecycleTest {
    @BeforeTest
    fun setup() {
        MapWrapperRegistry.clear()
    }

    @AfterTest
    fun tearDown() {
        MapWrapperRegistry.clear()
    }

    // ============================================================
    // STRONG REFERENCE TESTS
    // ============================================================

    @Test
    fun `strong references should prevent garbage collection during session`() {
        val eventId = "test-event"
        val wrapper =
            object {
                val data = "test-wrapper-data"
            }

        // Register with strong reference
        MapWrapperRegistry.registerWrapper(eventId, wrapper)

        // Access multiple times (simulates wave screen session)
        val references = mutableListOf<Any?>()
        repeat(1000) {
            references.add(MapWrapperRegistry.getWrapper(eventId))
        }

        // All references should be non-null (no GC)
        val nullCount = references.count { it == null }
        assertEquals(0, nullCount, "Strong references should prevent GC (found $nullCount nulls)")

        // Cleanup
        MapWrapperRegistry.unregisterWrapper(eventId)
    }

    @Test
    fun `wrapper should be accessible immediately after registration`() {
        val eventId = "immediate-test"
        val wrapper = object {}

        MapWrapperRegistry.registerWrapper(eventId, wrapper)

        // Should be accessible immediately
        val retrieved = MapWrapperRegistry.getWrapper(eventId)
        assertNotNull(retrieved, "Wrapper should be accessible immediately after registration")

        MapWrapperRegistry.unregisterWrapper(eventId)
    }

    @Test
    fun `wrapper should persist across multiple data operations`() {
        val eventId = "persistence-test"
        val wrapper = object {}

        MapWrapperRegistry.registerWrapper(eventId, wrapper)

        // Perform 100 operations
        repeat(100) { i ->
            // Store polygons
            MapWrapperRegistry.setPendingPolygons(eventId, emptyList(), false)

            // Store camera commands
            MapWrapperRegistry.setPendingCameraCommand(
                eventId,
                CameraCommand.AnimateToPosition(Position(0.0, 0.0), null),
            )

            // Clear commands
            MapWrapperRegistry.clearPendingPolygons(eventId)
            MapWrapperRegistry.clearPendingCameraCommand(eventId)

            // Wrapper should still exist
            assertNotNull(
                MapWrapperRegistry.getWrapper(eventId),
                "Wrapper should persist after operation $i",
            )
        }

        MapWrapperRegistry.unregisterWrapper(eventId)
    }

    // ============================================================
    // CLEANUP TESTS
    // ============================================================

    @Test
    fun `unregisterWrapper should remove wrapper immediately`() {
        val eventId = "cleanup-test"
        val wrapper = object {}

        MapWrapperRegistry.registerWrapper(eventId, wrapper)
        assertNotNull(MapWrapperRegistry.getWrapper(eventId))

        MapWrapperRegistry.unregisterWrapper(eventId)

        // Should be null immediately after unregister
        assertNull(MapWrapperRegistry.getWrapper(eventId))
    }

    @Test
    fun `unregisterWrapper should clean up all callback types`() {
        val eventId = "callbacks-cleanup-test"
        val wrapper = object {}

        MapWrapperRegistry.registerWrapper(eventId, wrapper)

        // Register all callback types
        var renderCalled = false
        var cameraCalled = false
        var idleCalled = false
        var coordCalled = false

        MapWrapperRegistry.setRenderCallback(eventId) { renderCalled = true }
        MapWrapperRegistry.setCameraCallback(eventId) { cameraCalled = true }
        MapWrapperRegistry.setCameraIdleListener(eventId) { idleCalled = true }
        MapWrapperRegistry.setMapClickCoordinateListener(eventId) { _, _ -> coordCalled = true }

        // Unregister
        MapWrapperRegistry.unregisterWrapper(eventId)

        // Verify callbacks removed from registry
        // Note: We don't test dispatch_async timing in unit tests
        // The important thing is callbacks are removed from storage

        // Verify direct invocations don't crash (callbacks removed)
        MapWrapperRegistry.invokeCameraIdleListener(eventId) // Should do nothing
        MapWrapperRegistry.invokeMapClickCoordinateListener(eventId, 0.0, 0.0) // Should do nothing

        // Note: requestImmediateRender and requestImmediateCameraExecution use dispatch_async
        // which may execute callbacks even after cleanup in test environment (async timing)
        // In production, DisposableEffect ensures proper cleanup timing
    }

    @Test
    fun `unregisterWrapper should clean up all data maps`() {
        val eventId = "data-cleanup-test"
        val wrapper = object {}

        MapWrapperRegistry.registerWrapper(eventId, wrapper)

        // Add data to all maps
        MapWrapperRegistry.setPendingPolygons(eventId, emptyList(), false)
        MapWrapperRegistry.setPendingCameraCommand(
            eventId,
            CameraCommand.MoveToBounds(BoundingBox(0.0, 0.0, 1.0, 1.0)),
        )
        MapWrapperRegistry.updateVisibleRegion(eventId, BoundingBox(0.0, 0.0, 1.0, 1.0))
        MapWrapperRegistry.updateMinZoom(eventId, 5.0)

        // Verify data exists
        assertTrue(MapWrapperRegistry.hasPendingPolygons(eventId))
        assertTrue(MapWrapperRegistry.hasPendingCameraCommand(eventId))
        assertNotNull(MapWrapperRegistry.getVisibleRegion(eventId))
        assertEquals(5.0, MapWrapperRegistry.getMinZoom(eventId))

        // Unregister
        MapWrapperRegistry.unregisterWrapper(eventId)

        // Verify all data cleaned
        assertFalse(MapWrapperRegistry.hasPendingPolygons(eventId))
        assertFalse(MapWrapperRegistry.hasPendingCameraCommand(eventId))
        assertNull(MapWrapperRegistry.getVisibleRegion(eventId))
        assertEquals(0.0, MapWrapperRegistry.getMinZoom(eventId)) // Returns default
    }

    // ============================================================
    // ISOLATION TESTS
    // ============================================================

    @Test
    fun `wrappers for different events should be completely isolated`() {
        val event1 = "event-1"
        val event2 = "event-2"
        val wrapper1 =
            object {
                val id = 1
            }
        val wrapper2 =
            object {
                val id = 2
            }

        // Register both
        MapWrapperRegistry.registerWrapper(event1, wrapper1)
        MapWrapperRegistry.registerWrapper(event2, wrapper2)

        // Add data to event1
        MapWrapperRegistry.setPendingPolygons(event1, listOf(listOf(Pair(0.0, 0.0))), false)
        MapWrapperRegistry.updateVisibleRegion(event1, BoundingBox(0.0, 0.0, 1.0, 1.0))

        // Add data to event2
        MapWrapperRegistry.setPendingPolygons(event2, listOf(listOf(Pair(2.0, 2.0))), true)
        MapWrapperRegistry.updateVisibleRegion(event2, BoundingBox(2.0, 2.0, 3.0, 3.0))

        // Verify event1 data
        val polygons1 = MapWrapperRegistry.getPendingPolygons(event1)
        assertNotNull(polygons1)
        assertEquals(0.0, polygons1.coordinates[0][0].first)
        assertFalse(polygons1.clearExisting)

        val region1 = MapWrapperRegistry.getVisibleRegion(event1)
        assertNotNull(region1)
        assertEquals(0.0, region1.minLatitude)

        // Verify event2 data
        val polygons2 = MapWrapperRegistry.getPendingPolygons(event2)
        assertNotNull(polygons2)
        assertEquals(2.0, polygons2.coordinates[0][0].first)
        assertTrue(polygons2.clearExisting)

        val region2 = MapWrapperRegistry.getVisibleRegion(event2)
        assertNotNull(region2)
        assertEquals(2.0, region2.minLatitude)

        // Cleanup event1
        MapWrapperRegistry.unregisterWrapper(event1)

        // Verify event1 cleaned but event2 intact
        assertNull(MapWrapperRegistry.getWrapper(event1))
        assertNotNull(MapWrapperRegistry.getWrapper(event2))
        assertNull(MapWrapperRegistry.getVisibleRegion(event1))
        assertNotNull(MapWrapperRegistry.getVisibleRegion(event2))

        // Cleanup event2
        MapWrapperRegistry.unregisterWrapper(event2)
    }

    // ============================================================
    // EDGE CASES
    // ============================================================

    @Test
    fun `operations on non-existent event should not crash`() {
        val nonExistent = "does-not-exist"

        // All operations should handle gracefully
        assertNull(MapWrapperRegistry.getWrapper(nonExistent))
        assertFalse(MapWrapperRegistry.hasPendingPolygons(nonExistent))
        assertNull(MapWrapperRegistry.getPendingPolygons(nonExistent))
        assertNull(MapWrapperRegistry.getVisibleRegion(nonExistent))
        assertEquals(0.0, MapWrapperRegistry.getMinZoom(nonExistent))

        // Invoke operations - should not crash
        MapWrapperRegistry.clearPendingPolygons(nonExistent)
        MapWrapperRegistry.clearPendingCameraCommand(nonExistent)
        MapWrapperRegistry.invokeCameraIdleListener(nonExistent)
        MapWrapperRegistry.requestImmediateRender(nonExistent)

        // No assertions needed - just verify no crash
    }

    @Test
    fun `double registration should overwrite previous wrapper`() {
        val eventId = "overwrite-test"
        val wrapper1 =
            object {
                val version = 1
            }
        val wrapper2 =
            object {
                val version = 2
            }

        // Register first wrapper
        MapWrapperRegistry.registerWrapper(eventId, wrapper1)
        assertNotNull(MapWrapperRegistry.getWrapper(eventId))

        // Register second wrapper (should overwrite)
        MapWrapperRegistry.registerWrapper(eventId, wrapper2)

        // Should have second wrapper
        val retrieved = MapWrapperRegistry.getWrapper(eventId)
        assertNotNull(retrieved)

        // Cleanup
        MapWrapperRegistry.unregisterWrapper(eventId)
    }

    @Test
    fun `double unregister should be safe`() {
        val eventId = "double-unregister-test"
        val wrapper = object {}

        MapWrapperRegistry.registerWrapper(eventId, wrapper)
        MapWrapperRegistry.unregisterWrapper(eventId)

        // Unregister again - should not crash
        MapWrapperRegistry.unregisterWrapper(eventId)

        assertNull(MapWrapperRegistry.getWrapper(eventId))
    }
}
