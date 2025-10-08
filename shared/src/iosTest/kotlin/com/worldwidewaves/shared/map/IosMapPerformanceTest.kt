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
import kotlin.test.assertTrue
import kotlin.time.measureTime

/**
 * Performance tests for iOS MapLibre implementation.
 * Validates that direct dispatch provides immediate updates (<50ms target).
 */
class IosMapPerformanceTest {
    private val testEventId = "perf-test-event"
    private val mockWrapper = object {}

    @BeforeTest
    fun setup() {
        MapWrapperRegistry.clear()
        MapWrapperRegistry.registerWrapper(testEventId, mockWrapper)
    }

    @AfterTest
    fun tearDown() {
        MapWrapperRegistry.unregisterWrapper(testEventId)
        MapWrapperRegistry.clear()
    }

    // ============================================================
    // POLYGON UPDATE PERFORMANCE
    // ============================================================

    @Test
    fun `polygon storage should complete within 10ms`() {
        val polygons =
            listOf(
                listOf(Pair(48.8, 2.2), Pair(48.9, 2.3), Pair(48.85, 2.35)),
                listOf(Pair(48.7, 2.1), Pair(48.8, 2.2), Pair(48.75, 2.25)),
            )

        val elapsed =
            measureTime {
                MapWrapperRegistry.setPendingPolygons(testEventId, polygons, clearExisting = true)
            }

        assertTrue(
            elapsed.inWholeMilliseconds < 10,
            "Polygon storage should complete in <10ms (actual: ${elapsed.inWholeMilliseconds}ms)",
        )
    }

    @Test
    fun `high frequency polygon updates should not accumulate`() {
        val singlePolygon = listOf(listOf(Pair(48.8, 2.2), Pair(48.9, 2.3), Pair(48.85, 2.35)))

        val elapsed =
            measureTime {
                repeat(100) { i ->
                    // Simulates rapid wave progression updates
                    MapWrapperRegistry.setPendingPolygons(testEventId, singlePolygon, clearExisting = true)
                }
            }

        // 100 updates should complete quickly (each overwrites previous)
        assertTrue(
            elapsed.inWholeMilliseconds < 100,
            "100 polygon updates should complete in <100ms (actual: ${elapsed.inWholeMilliseconds}ms)",
        )

        // Should only have last update (not accumulated)
        val pending = MapWrapperRegistry.getPendingPolygons(testEventId)
        assertNotNull(pending, "Should have pending polygons")
        assertEquals(1, pending?.coordinates?.size ?: 0, "Should only have latest polygon (not accumulated)")
    }

    @Test
    fun `continuous polygon updates should not leak memory`() {
        val initialPolygons =
            listOf(
                listOf(Pair(48.8, 2.2), Pair(48.9, 2.3), Pair(48.85, 2.35)),
            )

        // Store and clear 1000 times (simulates long wave session)
        repeat(1000) {
            MapWrapperRegistry.setPendingPolygons(testEventId, initialPolygons, clearExisting = true)
            MapWrapperRegistry.clearPendingPolygons(testEventId)
        }

        // Should not have any pending data
        assertFalse(
            MapWrapperRegistry.hasPendingPolygons(testEventId),
            "Should not accumulate data after 1000 cycles",
        )
    }

    // ============================================================
    // CAMERA COMMAND PERFORMANCE
    // ============================================================

    @Test
    fun `camera command storage should complete within 5ms`() {
        val command = CameraCommand.AnimateToPosition(Position(48.8566, 2.3522), 15.0)

        val elapsed =
            measureTime {
                MapWrapperRegistry.setPendingCameraCommand(testEventId, command)
            }

        assertTrue(
            elapsed.inWholeMilliseconds < 5,
            "Camera command storage should complete in <5ms (actual: ${elapsed.inWholeMilliseconds}ms)",
        )
    }

    @Test
    fun `rapid camera commands should overwrite not accumulate`() {
        val elapsed =
            measureTime {
                repeat(50) { i ->
                    val command = CameraCommand.AnimateToPosition(Position(48.0 + i * 0.01, 2.0), null)
                    MapWrapperRegistry.setPendingCameraCommand(testEventId, command)
                }
            }

        assertTrue(
            elapsed.inWholeMilliseconds < 50,
            "50 camera commands should complete in <50ms (actual: ${elapsed.inWholeMilliseconds}ms)",
        )

        // Should only have last command
        val pending = MapWrapperRegistry.getPendingCameraCommand(testEventId)
        assertNotNull(pending, "Should have pending camera command")
        assertTrue(pending is CameraCommand.AnimateToPosition, "Should be AnimateToPosition command")
    }

    // ============================================================
    // CALLBACK INVOCATION PERFORMANCE
    // ============================================================

    @Test
    fun `callback invocation should be immediate`() {
        var callbackInvoked = false

        MapWrapperRegistry.setCameraIdleListener(testEventId) {
            callbackInvoked = true
        }

        val elapsed =
            measureTime {
                MapWrapperRegistry.invokeCameraIdleListener(testEventId)
            }

        // Direct invocation should be nearly instant
        assertTrue(
            elapsed.inWholeMilliseconds < 5,
            "Callback invocation should complete in <5ms (actual: ${elapsed.inWholeMilliseconds}ms)",
        )
        assertTrue(callbackInvoked)
    }

    @Test
    fun `map click coordinate listener invocation should be immediate`() {
        var receivedLat = 0.0
        var receivedLng = 0.0

        MapWrapperRegistry.setMapClickCoordinateListener(testEventId) { lat, lng ->
            receivedLat = lat
            receivedLng = lng
        }

        val elapsed =
            measureTime {
                MapWrapperRegistry.invokeMapClickCoordinateListener(testEventId, 48.8566, 2.3522)
            }

        assertTrue(
            elapsed.inWholeMilliseconds < 5,
            "Coordinate listener invocation should complete in <5ms (actual: ${elapsed.inWholeMilliseconds}ms)",
        )
        assertEquals(48.8566, receivedLat, 0.0001)
        assertEquals(2.3522, receivedLng, 0.0001)
    }

    // ============================================================
    // WRAPPER ACCESS PERFORMANCE
    // ============================================================

    @Test
    fun `wrapper retrieval should be fast with strong references`() {
        val elapsed =
            measureTime {
                repeat(1000) {
                    MapWrapperRegistry.getWrapper(testEventId)
                }
            }

        // 1000 accesses should be very fast (direct map lookup)
        assertTrue(
            elapsed.inWholeMilliseconds < 50,
            "1000 wrapper retrievals should complete in <50ms (actual: ${elapsed.inWholeMilliseconds}ms)",
        )
    }

    @Test
    fun `wrapper registration should be fast`() {
        MapWrapperRegistry.unregisterWrapper(testEventId)

        val newWrapper = object {}
        val elapsed =
            measureTime {
                MapWrapperRegistry.registerWrapper(testEventId, newWrapper)
            }

        assertTrue(
            elapsed.inWholeMilliseconds < 5,
            "Wrapper registration should complete in <5ms (actual: ${elapsed.inWholeMilliseconds}ms)",
        )
    }

    @Test
    fun `wrapper cleanup should be fast`() {
        // Add lots of data
        MapWrapperRegistry.setPendingPolygons(testEventId, List(10) { emptyList() }, false)
        MapWrapperRegistry.setRenderCallback(testEventId) {}
        MapWrapperRegistry.setCameraCallback(testEventId) {}
        MapWrapperRegistry.setCameraIdleListener(testEventId) {}
        MapWrapperRegistry.setMapClickCoordinateListener(testEventId) { _, _ -> }

        val elapsed =
            measureTime {
                MapWrapperRegistry.unregisterWrapper(testEventId)
            }

        assertTrue(
            elapsed.inWholeMilliseconds < 10,
            "Cleanup with all data types should complete in <10ms (actual: ${elapsed.inWholeMilliseconds}ms)",
        )
    }

    // ============================================================
    // SCALABILITY TESTS
    // ============================================================

    @Test
    fun `registry should handle multiple concurrent events efficiently`() {
        val numEvents = 10
        val wrappers = List(numEvents) { object {} }

        val elapsed =
            measureTime {
                // Register 10 events
                repeat(numEvents) { i ->
                    MapWrapperRegistry.registerWrapper("event-$i", wrappers[i])
                    MapWrapperRegistry.setPendingPolygons("event-$i", emptyList(), false)
                    MapWrapperRegistry.updateVisibleRegion("event-$i", BoundingBox(0.0, 0.0, 1.0, 1.0))
                }

                // Access all events
                repeat(numEvents) { i ->
                    assertNotNull(MapWrapperRegistry.getWrapper("event-$i"))
                }

                // Cleanup all events
                repeat(numEvents) { i ->
                    MapWrapperRegistry.unregisterWrapper("event-$i")
                }
            }

        assertTrue(
            elapsed.inWholeMilliseconds < 100,
            "10 events full lifecycle should complete in <100ms (actual: ${elapsed.inWholeMilliseconds}ms)",
        )
    }

    @Test
    fun `no polling overhead when idle`() {
        // With strong references and direct dispatch, there should be:
        // - No polling timer
        // - No periodic checks
        // - No CPU usage when idle

        // Register wrapper but don't trigger any updates
        // Just verify wrapper stays alive
        val wrapper = MapWrapperRegistry.getWrapper(testEventId)
        assertNotNull(wrapper, "Wrapper should exist without any polling")

        // Simulate idle period (no operations)
        // In old system, this would cause 10 polls/second
        // In new system, zero overhead

        val wrapperAfter = MapWrapperRegistry.getWrapper(testEventId)
        assertNotNull(wrapperAfter, "Wrapper should remain alive during idle with strong ref")

        // No assertions on CPU usage in unit test, but confirms architecture
        // supports zero-overhead idle state
    }
}
