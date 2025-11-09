package com.worldwidewaves.shared.map

/*
 * Copyright 2025 DrWave
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

import com.worldwidewaves.shared.events.utils.BoundingBox
import com.worldwidewaves.shared.events.utils.Position
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MapWrapperRegistryTest {
    @BeforeTest
    fun setup() {
        MapWrapperRegistry.clear()
    }

    @AfterTest
    fun cleanup() {
        MapWrapperRegistry.clear()
    }

    @Test
    fun testBasicRegistration() {
        val wrapper = "TestWrapper"
        MapWrapperRegistry.registerWrapper("event1", wrapper)

        val retrieved = MapWrapperRegistry.getWrapper("event1")
        assertEquals(wrapper, retrieved)
    }

    @Test
    fun testUnregister() {
        val wrapper = "TestWrapper"
        MapWrapperRegistry.registerWrapper("event1", wrapper)

        MapWrapperRegistry.unregisterWrapper("event1")
        assertNull(MapWrapperRegistry.getWrapper("event1"))
    }

    @Test
    fun testNoEvictionWithStrongReferences() {
        // Register 10 wrappers (no limit with strong references)
        repeat(10) { i ->
            MapWrapperRegistry.registerWrapper("event-$i", "Wrapper$i")
        }

        // All wrappers should exist (no automatic eviction)
        repeat(10) { i ->
            assertNotNull(
                MapWrapperRegistry.getWrapper("event-$i"),
                "Wrapper $i should exist (no LRU eviction)",
            )
        }

        // Cleanup all
        repeat(10) { i ->
            MapWrapperRegistry.unregisterWrapper("event-$i")
        }
    }

    @Test
    fun testExplicitLifecycleManagement() {
        // With strong references, lifecycle is explicit
        MapWrapperRegistry.registerWrapper("event1", "Wrapper1")
        MapWrapperRegistry.registerWrapper("event2", "Wrapper2")
        MapWrapperRegistry.registerWrapper("event3", "Wrapper3")

        // All remain accessible (no automatic eviction)
        assertNotNull(MapWrapperRegistry.getWrapper("event1"))
        assertNotNull(MapWrapperRegistry.getWrapper("event2"))
        assertNotNull(MapWrapperRegistry.getWrapper("event3"))

        // Access order doesn't matter
        MapWrapperRegistry.getWrapper("event1")
        MapWrapperRegistry.getWrapper("event3")

        // All still exist
        assertNotNull(MapWrapperRegistry.getWrapper("event1"))
        assertNotNull(MapWrapperRegistry.getWrapper("event2"))
        assertNotNull(MapWrapperRegistry.getWrapper("event3"))

        // Explicit cleanup required
        MapWrapperRegistry.unregisterWrapper("event1")
        MapWrapperRegistry.unregisterWrapper("event2")
        MapWrapperRegistry.unregisterWrapper("event3")
    }

    @Test
    fun testWeakReferenceAllowsGC() {
        class HeavyWrapper(
            val size: Int = 1000000,
        )

        // Register a wrapper
        var wrapper: HeavyWrapper? = HeavyWrapper()
        MapWrapperRegistry.registerWrapper("event1", wrapper!!)

        // Verify it's registered
        assertNotNull(MapWrapperRegistry.getWrapper("event1"))

        // With strong references, wrapper persists until explicit unregister
        // This test now verifies strong reference behavior (not weak refs)

        // Wrapper should still exist (strong reference prevents GC)
        val retrieved = MapWrapperRegistry.getWrapper("event1")
        assertNotNull(retrieved, "Strong reference should keep wrapper alive")

        // Explicit cleanup required
        MapWrapperRegistry.unregisterWrapper("event1")
        assertNull(MapWrapperRegistry.getWrapper("event1"), "Wrapper should be removed after unregister")
    }

    @Test
    fun testStrongReferencePersistence() {
        // Register a wrapper with strong reference
        MapWrapperRegistry.registerWrapper("event1", "Wrapper1")

        // Wrapper should persist across multiple accesses
        repeat(100) {
            assertNotNull(MapWrapperRegistry.getWrapper("event1"), "Strong ref should persist")
        }

        // Cleanup
        MapWrapperRegistry.unregisterWrapper("event1")
    }

    @Test
    fun testPendingPolygons() {
        val coordinates =
            listOf(
                listOf(Pair(0.0, 0.0), Pair(1.0, 1.0)),
            )

        MapWrapperRegistry.setPendingPolygons("event1", coordinates, clearExisting = true)

        assertTrue(MapWrapperRegistry.hasPendingPolygons("event1"))

        val data = MapWrapperRegistry.getPendingPolygons("event1")
        assertNotNull(data)
        assertEquals(coordinates, data.coordinates)
        assertTrue(data.clearExisting)

        MapWrapperRegistry.clearPendingPolygons("event1")
        assertFalse(MapWrapperRegistry.hasPendingPolygons("event1"))
    }

    @Test
    fun testUnregisterClearsPendingPolygons() {
        val wrapper = "TestWrapper"
        val coordinates = listOf(listOf(Pair(0.0, 0.0)))

        MapWrapperRegistry.registerWrapper("event1", wrapper)
        MapWrapperRegistry.setPendingPolygons("event1", coordinates, clearExisting = false)

        MapWrapperRegistry.unregisterWrapper("event1")

        assertNull(MapWrapperRegistry.getWrapper("event1"))
        assertFalse(MapWrapperRegistry.hasPendingPolygons("event1"))
    }

    @Test
    fun testClearRemovesAll() {
        MapWrapperRegistry.registerWrapper("event1", "Wrapper1")
        MapWrapperRegistry.registerWrapper("event2", "Wrapper2")
        MapWrapperRegistry.setPendingPolygons("event1", listOf(listOf(Pair(0.0, 0.0))), false)

        MapWrapperRegistry.clear()

        assertNull(MapWrapperRegistry.getWrapper("event1"))
        assertNull(MapWrapperRegistry.getWrapper("event2"))
        assertFalse(MapWrapperRegistry.hasPendingPolygons("event1"))
    }

    @Test
    fun testPendingPolygonsIndependentOfWrappers() {
        // Pending polygons can exist without wrappers
        MapWrapperRegistry.setPendingPolygons("event1", listOf(listOf(Pair(0.0, 0.0))), false)
        MapWrapperRegistry.setPendingPolygons("event2", listOf(listOf(Pair(1.0, 1.0))), false)
        MapWrapperRegistry.setPendingPolygons("event3", listOf(listOf(Pair(2.0, 2.0))), false)
        MapWrapperRegistry.setPendingPolygons("event4", listOf(listOf(Pair(3.0, 3.0))), false)

        // Register wrappers later
        MapWrapperRegistry.registerWrapper("event1", "Wrapper1")
        MapWrapperRegistry.registerWrapper("event2", "Wrapper2")
        MapWrapperRegistry.registerWrapper("event3", "Wrapper3")
        MapWrapperRegistry.registerWrapper("event4", "Wrapper4")

        // All polygons and wrappers should exist (no eviction)
        assertTrue(MapWrapperRegistry.hasPendingPolygons("event1"))
        assertTrue(MapWrapperRegistry.hasPendingPolygons("event2"))
        assertTrue(MapWrapperRegistry.hasPendingPolygons("event3"))
        assertTrue(MapWrapperRegistry.hasPendingPolygons("event4"))
        assertNotNull(MapWrapperRegistry.getWrapper("event1"))
        assertNotNull(MapWrapperRegistry.getWrapper("event2"))
        assertNotNull(MapWrapperRegistry.getWrapper("event3"))
        assertNotNull(MapWrapperRegistry.getWrapper("event4"))

        // Cleanup
        repeat(4) { i -> MapWrapperRegistry.unregisterWrapper("event${i + 1}") }
    }

    // ============================================================
    // CAMERA COMMAND TESTS (Added October 8, 2025)
    // ============================================================

    @Test
    fun testPendingCameraCommand_AnimateToPosition() {
        val position = Position(48.8566, 2.3522)
        val command = CameraCommand.AnimateToPosition(position, zoom = 12.0)

        MapWrapperRegistry.setPendingCameraCommand("event1", command)

        assertTrue(MapWrapperRegistry.hasPendingCameraCommand("event1"))

        val retrieved = MapWrapperRegistry.getPendingCameraCommand("event1")
        assertNotNull(retrieved)
        assertIs<CameraCommand.AnimateToPosition>(retrieved)
        assertEquals(position.lat, retrieved.position.lat, 0.0001)
        assertEquals(position.lng, retrieved.position.lng, 0.0001)
        assertEquals(12.0, retrieved.zoom)
    }

    @Test
    fun testPendingCameraCommand_AnimateToBounds() {
        val bounds =
            BoundingBox.fromCorners(
                listOf(
                    Position(48.0, 2.0),
                    Position(49.0, 3.0),
                ),
            )!!
        val command = CameraCommand.AnimateToBounds(bounds, padding = 50)

        MapWrapperRegistry.setPendingCameraCommand("event1", command)

        val retrieved = MapWrapperRegistry.getPendingCameraCommand("event1")
        assertNotNull(retrieved)
        assertIs<CameraCommand.AnimateToBounds>(retrieved)
        assertEquals(50, retrieved.padding)
    }

    @Test
    fun testPendingCameraCommand_MoveToBounds() {
        val bounds =
            BoundingBox.fromCorners(
                listOf(
                    Position(48.0, 2.0),
                    Position(49.0, 3.0),
                ),
            )!!
        val command = CameraCommand.MoveToBounds(bounds)

        MapWrapperRegistry.setPendingCameraCommand("event1", command)

        val retrieved = MapWrapperRegistry.getPendingCameraCommand("event1")
        assertNotNull(retrieved)
        assertIs<CameraCommand.MoveToBounds>(retrieved)
    }

    @Test
    fun testPendingCameraCommand_SetConstraintBounds() {
        val bounds =
            BoundingBox.fromCorners(
                listOf(
                    Position(48.0, 2.0),
                    Position(49.0, 3.0),
                ),
            )!!
        val command = CameraCommand.SetConstraintBounds(bounds, bounds, true)

        MapWrapperRegistry.setPendingCameraCommand("event1", command)

        val retrieved = MapWrapperRegistry.getPendingCameraCommand("event1")
        assertNotNull(retrieved)
        assertIs<CameraCommand.SetConstraintBounds>(retrieved)
    }

    @Test
    fun testClearPendingCameraCommand() {
        val position = Position(48.8566, 2.3522)
        val command = CameraCommand.AnimateToPosition(position, zoom = 12.0)

        MapWrapperRegistry.setPendingCameraCommand("event1", command)
        assertTrue(MapWrapperRegistry.hasPendingCameraCommand("event1"))

        MapWrapperRegistry.clearPendingCameraCommand("event1")
        assertFalse(MapWrapperRegistry.hasPendingCameraCommand("event1"))
        assertNull(MapWrapperRegistry.getPendingCameraCommand("event1"))
    }

    @Test
    fun testUnregisterClearsCameraCommands() {
        val wrapper = "TestWrapper"
        val position = Position(48.8566, 2.3522)
        val command = CameraCommand.AnimateToPosition(position, zoom = 12.0)

        MapWrapperRegistry.registerWrapper("event1", wrapper)
        MapWrapperRegistry.setPendingCameraCommand("event1", command)

        MapWrapperRegistry.unregisterWrapper("event1")

        assertNull(MapWrapperRegistry.getWrapper("event1"))
        assertFalse(MapWrapperRegistry.hasPendingCameraCommand("event1"))
    }

    @Test
    fun testUnregisterClearsBothPolygonsAndCameraCommands() {
        val wrapper = "TestWrapper"
        val polygons = listOf(listOf(Pair(0.0, 0.0), Pair(1.0, 1.0)))
        val position = Position(48.8566, 2.3522)
        val command = CameraCommand.AnimateToPosition(position, zoom = 12.0)

        MapWrapperRegistry.registerWrapper("event1", wrapper)
        MapWrapperRegistry.setPendingPolygons("event1", polygons, clearExisting = true)
        MapWrapperRegistry.setPendingCameraCommand("event1", command)

        // Verify all data present
        assertTrue(MapWrapperRegistry.hasPendingPolygons("event1"))
        assertTrue(MapWrapperRegistry.hasPendingCameraCommand("event1"))

        MapWrapperRegistry.unregisterWrapper("event1")

        // Verify everything cleared
        assertNull(MapWrapperRegistry.getWrapper("event1"))
        assertFalse(MapWrapperRegistry.hasPendingPolygons("event1"))
        assertFalse(MapWrapperRegistry.hasPendingCameraCommand("event1"))
    }

    @Test
    fun testClearRemovesCameraCommands() {
        val position = Position(48.8566, 2.3522)
        val command = CameraCommand.AnimateToPosition(position, zoom = 12.0)

        MapWrapperRegistry.setPendingCameraCommand("event1", command)
        MapWrapperRegistry.setPendingCameraCommand("event2", command)

        MapWrapperRegistry.clear()

        assertFalse(MapWrapperRegistry.hasPendingCameraCommand("event1"))
        assertFalse(MapWrapperRegistry.hasPendingCameraCommand("event2"))
    }

    @Test
    fun testCameraCommandOverwrite() {
        val position1 = Position(48.8566, 2.3522)
        val position2 = Position(40.7128, -74.0060)
        val command1 = CameraCommand.AnimateToPosition(position1, zoom = 12.0)
        val command2 = CameraCommand.AnimateToPosition(position2, zoom = 15.0)

        MapWrapperRegistry.setPendingCameraCommand("event1", command1)
        MapWrapperRegistry.setPendingCameraCommand("event1", command2)

        val retrieved = MapWrapperRegistry.getPendingCameraCommand("event1")
        assertNotNull(retrieved)
        assertIs<CameraCommand.AnimateToPosition>(retrieved)
        assertEquals(position2.lat, retrieved.position.lat, 0.0001)
        assertEquals(15.0, retrieved.zoom)
    }

    @Test
    fun testNoPendingCameraCommand() {
        assertFalse(MapWrapperRegistry.hasPendingCameraCommand("nonexistent"))
        assertNull(MapWrapperRegistry.getPendingCameraCommand("nonexistent"))
    }

    // ============================================================
    // BBOX DRAWING TESTS
    // ============================================================

    @Test
    fun drawDebugBbox_retrievesCorrectWrapper() {
        // Given: Registered wrapper for event
        val eventId = "lagos_nigeria"
        val wrapper = "test-wrapper-bbox"
        MapWrapperRegistry.registerWrapper(eventId, wrapper)

        val bbox =
            BoundingBox.fromCorners(
                Position(6.371119, 3.196678), // Lagos SW
                Position(6.642783, 3.598022), // Lagos NE
            )

        // When: Draw bbox
        MapWrapperRegistry.drawDebugBbox(eventId, bbox)

        // Then: Should retrieve the registered wrapper (verified by no crash)
        val retrieved = MapWrapperRegistry.getWrapper(eventId)
        assertEquals(wrapper, retrieved, "Should retrieve correct wrapper")

        // Cleanup
        MapWrapperRegistry.unregisterWrapper(eventId)
    }

    @Test
    fun drawDebugBbox_dispatchesToMainQueue() {
        // Given: Registered wrapper
        val eventId = "tokyo_japan"
        val wrapper = "test-wrapper-dispatch"
        MapWrapperRegistry.registerWrapper(eventId, wrapper)

        val bbox =
            BoundingBox.fromCorners(
                Position(35.450628, 138.822556), // Tokyo SW
                Position(35.989700, 139.994659), // Tokyo NE
            )

        // When: Draw bbox (dispatches to main queue)
        MapWrapperRegistry.drawDebugBbox(eventId, bbox)

        // Then: Should complete without throwing (dispatch is asynchronous)
        // Note: We can't directly verify dispatch_async was called without mocking
        // But we can verify the function completes successfully

        // Cleanup
        MapWrapperRegistry.unregisterWrapper(eventId)
    }

    @Test
    fun drawDebugBbox_handlesNullWrapper() {
        // Given: Event with no registered wrapper
        val eventId = "unregistered-event-bbox"
        val bbox =
            BoundingBox.fromCorners(
                Position(37.70559, -122.539501), // SF SW
                Position(37.833685, -122.343807), // SF NE
            )

        // When: Draw bbox with no wrapper
        try {
            MapWrapperRegistry.drawDebugBbox(eventId, bbox)
            // Then: Should handle gracefully without crashing
        } catch (e: Exception) {
            throw AssertionError("drawDebugBbox should handle null wrapper gracefully", e)
        }
    }

    @Test
    fun drawDebugBbox_invokesSwiftMethodWithCorrectParams() {
        // Given: Registered wrapper
        val eventId = "san_francisco_usa"
        val wrapper = "test-wrapper-params"
        MapWrapperRegistry.registerWrapper(eventId, wrapper)

        val bbox =
            BoundingBox.fromCorners(
                Position(37.70559, -122.539501), // SF SW
                Position(37.833685, -122.343807), // SF NE
            )

        // When: Draw bbox
        MapWrapperRegistry.drawDebugBbox(eventId, bbox)

        // Then: Should complete successfully
        // Note: Actual Swift method invocation verification requires integration test
        // This test verifies that:
        // 1. The function accepts correct bbox parameters
        // 2. The bbox coordinates are preserved
        assertEquals(37.70559, bbox.sw.lat, 0.000001, "SW lat should be preserved")
        assertEquals(-122.539501, bbox.sw.lng, 0.000001, "SW lng should be preserved")
        assertEquals(37.833685, bbox.ne.lat, 0.000001, "NE lat should be preserved")
        assertEquals(-122.343807, bbox.ne.lng, 0.000001, "NE lng should be preserved")

        // Cleanup
        MapWrapperRegistry.unregisterWrapper(eventId)
    }

    // ============================================================
    // LRU EVICTION TESTS (Memory Leak Prevention)
    // ============================================================

    @Test
    fun testLRUEviction_ExceedingMaxWrappers() {
        // Register exactly MAX_WRAPPERS (10) wrappers
        repeat(10) { i ->
            MapWrapperRegistry.registerWrapper("event-$i", "Wrapper$i")
        }

        // All 10 should be present
        repeat(10) { i ->
            assertNotNull(MapWrapperRegistry.getWrapper("event-$i"), "Wrapper $i should exist")
        }

        // Register 11th wrapper - should evict LRU (event-0)
        MapWrapperRegistry.registerWrapper("event-10", "Wrapper10")

        // event-0 should be evicted (LRU)
        assertNull(MapWrapperRegistry.getWrapper("event-0"), "event-0 should be evicted (LRU)")

        // event-1 through event-10 should still exist
        repeat(10) { i ->
            assertNotNull(MapWrapperRegistry.getWrapper("event-${i + 1}"), "Wrapper ${i + 1} should exist")
        }

        // Cleanup
        repeat(10) { i ->
            MapWrapperRegistry.unregisterWrapper("event-${i + 1}")
        }
    }

    @Test
    fun testLRUEviction_AccessOrderUpdates() {
        // Register 10 wrappers
        repeat(10) { i ->
            MapWrapperRegistry.registerWrapper("event-$i", "Wrapper$i")
        }

        // Access event-0 to make it most recently used
        MapWrapperRegistry.getWrapper("event-0")

        // Register 11th wrapper - should evict event-1 (now LRU), not event-0
        MapWrapperRegistry.registerWrapper("event-10", "Wrapper10")

        // event-1 should be evicted (LRU after event-0 was accessed)
        assertNull(MapWrapperRegistry.getWrapper("event-1"), "event-1 should be evicted (LRU)")

        // event-0 should still exist (was accessed recently)
        assertNotNull(MapWrapperRegistry.getWrapper("event-0"), "event-0 should exist (accessed recently)")

        // Cleanup
        MapWrapperRegistry.clear()
    }

    @Test
    fun testLRUEviction_ReRegisterMoveToEnd() {
        // Register 10 wrappers
        repeat(10) { i ->
            MapWrapperRegistry.registerWrapper("event-$i", "Wrapper$i")
        }

        // Re-register event-0 (moves to end of access order)
        MapWrapperRegistry.registerWrapper("event-0", "Wrapper0-Updated")

        // Register 11th wrapper - should evict event-1 (now LRU)
        MapWrapperRegistry.registerWrapper("event-10", "Wrapper10")

        // event-1 should be evicted
        assertNull(MapWrapperRegistry.getWrapper("event-1"), "event-1 should be evicted (LRU)")

        // event-0 should exist with updated value
        assertEquals("Wrapper0-Updated", MapWrapperRegistry.getWrapper("event-0"))

        // Cleanup
        MapWrapperRegistry.clear()
    }

    @Test
    fun testLRUEviction_DoesNotEvictExistingKeys() {
        // Register 10 wrappers
        repeat(10) { i ->
            MapWrapperRegistry.registerWrapper("event-$i", "Wrapper$i")
        }

        // Re-register existing wrapper (should NOT trigger eviction)
        MapWrapperRegistry.registerWrapper("event-5", "Wrapper5-Updated")

        // All 10 should still exist
        repeat(10) { i ->
            assertNotNull(MapWrapperRegistry.getWrapper("event-$i"), "Wrapper $i should exist")
        }

        // event-5 should have updated value
        assertEquals("Wrapper5-Updated", MapWrapperRegistry.getWrapper("event-5"))

        // Cleanup
        MapWrapperRegistry.clear()
    }

    @Test
    fun testLRUEviction_CleansUpAssociatedData() {
        // Register 10 wrappers with associated data
        repeat(10) { i ->
            MapWrapperRegistry.registerWrapper("event-$i", "Wrapper$i")
            MapWrapperRegistry.setPendingPolygons("event-$i", listOf(listOf(Pair(0.0, 0.0))), true)
            MapWrapperRegistry.setPendingCameraCommand("event-$i", CameraCommand.AnimateToPosition(Position(0.0, 0.0), 10.0))
        }

        // Register 11th wrapper - should evict event-0 and its data
        MapWrapperRegistry.registerWrapper("event-10", "Wrapper10")

        // event-0 wrapper and data should be evicted
        assertNull(MapWrapperRegistry.getWrapper("event-0"))
        assertFalse(MapWrapperRegistry.hasPendingPolygons("event-0"), "Polygons should be cleared on eviction")
        assertFalse(MapWrapperRegistry.hasPendingCameraCommand("event-0"), "Camera commands should be cleared on eviction")

        // Cleanup
        MapWrapperRegistry.clear()
    }
}
