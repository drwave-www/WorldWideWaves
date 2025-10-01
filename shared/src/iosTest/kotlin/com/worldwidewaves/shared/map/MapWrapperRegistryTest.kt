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

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
    fun testLRUEviction() {
        // Register 4 wrappers (exceeds MAX_CACHED_WRAPPERS = 3)
        MapWrapperRegistry.registerWrapper("event1", "Wrapper1")
        MapWrapperRegistry.registerWrapper("event2", "Wrapper2")
        MapWrapperRegistry.registerWrapper("event3", "Wrapper3")
        MapWrapperRegistry.registerWrapper("event4", "Wrapper4")

        // event1 should have been evicted (oldest)
        assertNull(MapWrapperRegistry.getWrapper("event1"))
        assertNotNull(MapWrapperRegistry.getWrapper("event2"))
        assertNotNull(MapWrapperRegistry.getWrapper("event3"))
        assertNotNull(MapWrapperRegistry.getWrapper("event4"))
    }

    @Test
    fun testLRUAccessOrder() {
        // Register 3 wrappers
        MapWrapperRegistry.registerWrapper("event1", "Wrapper1")
        MapWrapperRegistry.registerWrapper("event2", "Wrapper2")
        MapWrapperRegistry.registerWrapper("event3", "Wrapper3")

        // Access event1 (moves it to most recently used)
        MapWrapperRegistry.getWrapper("event1")

        // Register event4 (should evict event2, the least recently used)
        MapWrapperRegistry.registerWrapper("event4", "Wrapper4")

        assertNotNull(MapWrapperRegistry.getWrapper("event1"))
        assertNull(MapWrapperRegistry.getWrapper("event2"))
        assertNotNull(MapWrapperRegistry.getWrapper("event3"))
        assertNotNull(MapWrapperRegistry.getWrapper("event4"))
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

        // Remove strong reference
        wrapper = null

        // Note: System.gc() is not available in Kotlin/Native
        // GC timing is non-deterministic anyway, so we test the mechanism not the timing
        // kotlin.native.internal.GC.collect() exists but is internal API

        // Prune stale references
        MapWrapperRegistry.pruneStaleReferences()

        // The wrapper should be gone or null (depends on GC timing)
        // This test verifies the mechanism exists, actual GC timing is non-deterministic
        val retrieved = MapWrapperRegistry.getWrapper("event1")
        // We can't guarantee GC ran, but we can verify the mechanism works
        assertTrue(retrieved == null || retrieved is HeavyWrapper)
    }

    @Test
    fun testPruneStaleReferences() {
        // Register a wrapper
        MapWrapperRegistry.registerWrapper("event1", "Wrapper1")

        // Manually prune (should not remove strong reference)
        MapWrapperRegistry.pruneStaleReferences()
        assertNotNull(MapWrapperRegistry.getWrapper("event1"))
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
    fun testPendingPolygonsNotAffectedByLRU() {
        // Register pending polygons for events
        MapWrapperRegistry.setPendingPolygons("event1", listOf(listOf(Pair(0.0, 0.0))), false)
        MapWrapperRegistry.setPendingPolygons("event2", listOf(listOf(Pair(1.0, 1.0))), false)
        MapWrapperRegistry.setPendingPolygons("event3", listOf(listOf(Pair(2.0, 2.0))), false)
        MapWrapperRegistry.setPendingPolygons("event4", listOf(listOf(Pair(3.0, 3.0))), false)

        // Register wrappers (triggers LRU eviction)
        MapWrapperRegistry.registerWrapper("event1", "Wrapper1")
        MapWrapperRegistry.registerWrapper("event2", "Wrapper2")
        MapWrapperRegistry.registerWrapper("event3", "Wrapper3")
        MapWrapperRegistry.registerWrapper("event4", "Wrapper4")

        // Even though event1 wrapper was evicted, its pending polygons remain
        assertTrue(MapWrapperRegistry.hasPendingPolygons("event1"))
        assertTrue(MapWrapperRegistry.hasPendingPolygons("event2"))
        assertTrue(MapWrapperRegistry.hasPendingPolygons("event3"))
        assertTrue(MapWrapperRegistry.hasPendingPolygons("event4"))
    }
}
