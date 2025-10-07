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
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Integration tests for iOS map workflows.
 * Tests the complete flow from Kotlin command storage to Swift execution readiness.
 *
 * Note: These tests verify the Kotlin side of the workflow. Actual Swift execution
 * and UI rendering must be verified via manual iOS testing.
 */
class IosMapWorkflowIntegrationTest {
    private val testEventId = "test_event_123"

    @BeforeTest
    fun setup() {
        MapWrapperRegistry.clear()
    }

    @AfterTest
    fun cleanup() {
        MapWrapperRegistry.clear()
    }

    // ============================================================
    // CAMERA COMMAND WORKFLOW TESTS
    // ============================================================

    @Test
    fun testInitialCameraPositioningWorkflow() {
        // Simulate setupMap() calling animateCameraToBounds for initial positioning
        val eventBounds =
            BoundingBox.fromCorners(
                listOf(
                    Position(48.8, 2.2),
                    Position(48.9, 2.4),
                ),
            )!!

        // 1. Kotlin: setupMap() stores AnimateToBounds command
        val command = CameraCommand.AnimateToBounds(eventBounds, padding = 50)
        MapWrapperRegistry.setPendingCameraCommand(testEventId, command)

        // 2. Verify command is retrievable by Swift
        assertTrue(MapWrapperRegistry.hasPendingCameraCommand(testEventId))
        val retrieved = MapWrapperRegistry.getPendingCameraCommand(testEventId)
        assertNotNull(retrieved)

        // 3. Simulate Swift execution
        MapWrapperRegistry.clearPendingCameraCommand(testEventId)
        assertFalse(MapWrapperRegistry.hasPendingCameraCommand(testEventId))
    }

    @Test
    fun testConstraintEnforcementWorkflow() {
        // Simulate setupMap() applying bounds constraints
        val constraintBounds =
            BoundingBox.fromCorners(
                listOf(
                    Position(48.8, 2.2),
                    Position(48.9, 2.4),
                ),
            )!!

        // 1. Kotlin: setBoundsForCameraTarget stores SetConstraintBounds command
        val command = CameraCommand.SetConstraintBounds(constraintBounds)
        MapWrapperRegistry.setPendingCameraCommand(testEventId, command)

        // 2. Verify Swift can retrieve and identify constraint command
        val retrieved = MapWrapperRegistry.getPendingCameraCommand(testEventId)
        assertNotNull(retrieved)
        assertTrue(retrieved is CameraCommand.SetConstraintBounds)

        // 3. Swift executes and clears
        MapWrapperRegistry.clearPendingCameraCommand(testEventId)
        assertFalse(MapWrapperRegistry.hasPendingCameraCommand(testEventId))
    }

    @Test
    fun testAutoFollowingWorkflow() {
        // Simulate MapZoomAndLocationUpdate calling targetUserAndWave()
        val userPosition = Position(48.85, 2.3)
        val wavePosition = Position(48.87, 2.3)

        // Create bounds containing both user and wave
        val bounds = BoundingBox.fromCorners(listOf(userPosition, wavePosition))!!

        // 1. Kotlin: targetUserAndWave() stores AnimateToBounds command
        val command = CameraCommand.AnimateToBounds(bounds, padding = 100)
        MapWrapperRegistry.setPendingCameraCommand(testEventId, command)

        // 2. Verify command available for Swift polling
        assertTrue(MapWrapperRegistry.hasPendingCameraCommand(testEventId))

        // 3. Simulate continuous polling finding and executing command
        val retrieved = MapWrapperRegistry.getPendingCameraCommand(testEventId)
        assertNotNull(retrieved)
        MapWrapperRegistry.clearPendingCameraCommand(testEventId)

        // 4. Next poll should find no command
        assertFalse(MapWrapperRegistry.hasPendingCameraCommand(testEventId))
    }

    @Test
    fun testMultipleCameraCommandsInSequence() {
        // Simulate multiple camera operations in quick succession
        val position1 = Position(48.85, 2.3)
        val position2 = Position(48.87, 2.35)

        // 1. First command
        MapWrapperRegistry.setPendingCameraCommand(testEventId, CameraCommand.AnimateToPosition(position1, 12.0))
        assertTrue(MapWrapperRegistry.hasPendingCameraCommand(testEventId))

        // 2. Swift executes first command
        MapWrapperRegistry.clearPendingCameraCommand(testEventId)

        // 3. Second command arrives
        MapWrapperRegistry.setPendingCameraCommand(testEventId, CameraCommand.AnimateToPosition(position2, 14.0))

        // 4. Verify second command is retrievable
        val retrieved = MapWrapperRegistry.getPendingCameraCommand(testEventId)
        assertNotNull(retrieved)
        assertTrue(retrieved is CameraCommand.AnimateToPosition)
        assertEquals(position2.lat, (retrieved as CameraCommand.AnimateToPosition).position.lat, 0.0001)
    }

    // ============================================================
    // WAVE POLYGON WORKFLOW TESTS
    // ============================================================

    @Test
    fun testWavePolygonRenderingWorkflow() {
        // Simulate WaveProgressionObserver updating wave polygons
        val polygon1 =
            listOf(
                Pair(48.85, 2.30),
                Pair(48.86, 2.30),
                Pair(48.86, 2.31),
                Pair(48.85, 2.31),
            )

        // 1. Kotlin: updateWavePolygons() stores polygons
        MapWrapperRegistry.setPendingPolygons(testEventId, listOf(polygon1), clearExisting = true)

        // 2. Verify Swift can retrieve polygons
        assertTrue(MapWrapperRegistry.hasPendingPolygons(testEventId))
        val polygonData = MapWrapperRegistry.getPendingPolygons(testEventId)
        assertNotNull(polygonData)
        assertEquals(1, polygonData.coordinates.size)
        assertTrue(polygonData.clearExisting)

        // 3. Swift renders and clears
        MapWrapperRegistry.clearPendingPolygons(testEventId)
        assertFalse(MapWrapperRegistry.hasPendingPolygons(testEventId))
    }

    @Test
    fun testRealTimeWaveProgressionWorkflow() {
        // Simulate wave progression updates every 250ms
        val progression1 = listOf(listOf(Pair(48.85, 2.30), Pair(48.86, 2.30)))
        val progression2 = listOf(listOf(Pair(48.85, 2.30), Pair(48.87, 2.30)))
        val progression3 = listOf(listOf(Pair(48.85, 2.30), Pair(48.88, 2.30)))

        // 1. First update
        MapWrapperRegistry.setPendingPolygons(testEventId, progression1, clearExisting = false)
        assertTrue(MapWrapperRegistry.hasPendingPolygons(testEventId))
        MapWrapperRegistry.clearPendingPolygons(testEventId)

        // 2. Second update (wave progressed)
        MapWrapperRegistry.setPendingPolygons(testEventId, progression2, clearExisting = false)
        assertTrue(MapWrapperRegistry.hasPendingPolygons(testEventId))
        MapWrapperRegistry.clearPendingPolygons(testEventId)

        // 3. Third update (wave progressed more)
        MapWrapperRegistry.setPendingPolygons(testEventId, progression3, clearExisting = false)
        val retrieved = MapWrapperRegistry.getPendingPolygons(testEventId)
        assertNotNull(retrieved)
        assertFalse(retrieved.clearExisting) // Should be incremental
    }

    // ============================================================
    // MAP CLICK WORKFLOW TESTS
    // ============================================================

    @Test
    fun testMapClickNavigationWorkflow() {
        var callbackInvoked = false
        var receivedEventId: String? = null

        // 1. Kotlin: Register callback for navigation
        val callback: () -> Unit = {
            callbackInvoked = true
            receivedEventId = testEventId
        }
        MapWrapperRegistry.setMapClickCallback(testEventId, callback)

        // 2. Verify callback is stored
        // Note: Can't directly test invokeMapClickCallback from Kotlin test
        // (it's designed to be called from Swift)
        // But we can verify storage and retrieval work

        // 3. Simulate Swift invocation
        val invoked = MapWrapperRegistry.invokeMapClickCallback(testEventId)
        assertTrue(invoked)
        assertTrue(callbackInvoked)
        assertEquals(testEventId, receivedEventId)
    }

    @Test
    fun testMapClickWithNoCallback() {
        // Simulate Swift tap when no callback registered
        val invoked = MapWrapperRegistry.invokeMapClickCallback("nonexistent_event")
        assertFalse(invoked)
    }

    // ============================================================
    // COMBINED WORKFLOW TESTS
    // ============================================================

    @Test
    fun testCompleteMapInitializationWorkflow() {
        // Simulate complete map initialization sequence

        // 1. Wrapper registered (Swift side)
        val mockWrapper = "TestWrapper"
        MapWrapperRegistry.registerWrapper(testEventId, mockWrapper)

        // 2. setupMap() called (Kotlin side)
        val initialBounds =
            BoundingBox.fromCorners(
                listOf(Position(48.8, 2.2), Position(48.9, 2.4)),
            )!!

        // Initial camera positioning
        MapWrapperRegistry.setPendingCameraCommand(
            testEventId,
            CameraCommand.AnimateToBounds(initialBounds, padding = 0),
        )

        // Constraint bounds
        MapWrapperRegistry.setPendingCameraCommand(
            testEventId,
            CameraCommand.SetConstraintBounds(initialBounds),
        )

        // 3. Map click callback registered
        var clicked = false
        MapWrapperRegistry.setMapClickCallback(testEventId) { clicked = true }

        // 4. Initial wave polygons
        val initialPolygons = listOf(listOf(Pair(48.85, 2.3), Pair(48.86, 2.3)))
        MapWrapperRegistry.setPendingPolygons(testEventId, initialPolygons, clearExisting = true)

        // 5. Verify all data ready for Swift
        assertNotNull(MapWrapperRegistry.getWrapper(testEventId))
        assertTrue(MapWrapperRegistry.hasPendingCameraCommand(testEventId))
        assertTrue(MapWrapperRegistry.hasPendingPolygons(testEventId))

        // 6. Simulate Swift execution
        MapWrapperRegistry.getPendingCameraCommand(testEventId)
        MapWrapperRegistry.clearPendingCameraCommand(testEventId)
        MapWrapperRegistry.renderPendingPolygons(testEventId)
        MapWrapperRegistry.clearPendingPolygons(testEventId)

        // 7. Simulate map click
        MapWrapperRegistry.invokeMapClickCallback(testEventId)
        assertTrue(clicked)
    }

    @Test
    fun testMapCleanupWorkflow() {
        // Simulate complete cleanup when map is destroyed

        // 1. Set up everything
        MapWrapperRegistry.registerWrapper(testEventId, "TestWrapper")
        MapWrapperRegistry.setPendingCameraCommand(
            testEventId,
            CameraCommand.AnimateToPosition(Position(48.85, 2.3), 12.0),
        )
        MapWrapperRegistry.setPendingPolygons(testEventId, listOf(listOf(Pair(0.0, 0.0))), false)
        MapWrapperRegistry.setMapClickCallback(testEventId) {}

        // 2. Verify all data present
        assertNotNull(MapWrapperRegistry.getWrapper(testEventId))
        assertTrue(MapWrapperRegistry.hasPendingCameraCommand(testEventId))
        assertTrue(MapWrapperRegistry.hasPendingPolygons(testEventId))

        // 3. Unregister (map destroyed)
        MapWrapperRegistry.unregisterWrapper(testEventId)

        // 4. Verify complete cleanup
        assertFalse(MapWrapperRegistry.hasPendingCameraCommand(testEventId))
        assertFalse(MapWrapperRegistry.hasPendingPolygons(testEventId))
        // Note: invokeMapClickCallback will return false if callback cleared
        val invoked = MapWrapperRegistry.invokeMapClickCallback(testEventId)
        assertFalse(invoked)
    }

    @Test
    fun testPolygonQueueingBeforeStyleLoads() {
        // Simulate polygons arriving before Swift style loads
        val polygon1 = listOf(Pair(48.85, 2.30), Pair(48.86, 2.30), Pair(48.86, 2.31))
        val polygon2 = listOf(Pair(48.87, 2.30), Pair(48.88, 2.30), Pair(48.88, 2.31))

        // 1. Store multiple polygon updates
        MapWrapperRegistry.setPendingPolygons(testEventId, listOf(polygon1), clearExisting = true)
        MapWrapperRegistry.setPendingPolygons(testEventId, listOf(polygon2), clearExisting = false)

        // 2. Both should be stored (last one wins in registry)
        assertTrue(MapWrapperRegistry.hasPendingPolygons(testEventId))

        // 3. Swift renders when style loads
        val data = MapWrapperRegistry.getPendingPolygons(testEventId)
        assertNotNull(data)
        MapWrapperRegistry.clearPendingPolygons(testEventId)

        // 4. Verify cleared
        assertFalse(MapWrapperRegistry.hasPendingPolygons(testEventId))
    }

    @Test
    fun testKeyStabilityDoesNotAffectRegistry() {
        // Verify that Compose key() changes don't affect MapWrapperRegistry data
        val wrapper1 = "Wrapper1"
        val polygons = listOf(listOf(Pair(48.85, 2.30)))
        val command = CameraCommand.AnimateToPosition(Position(48.85, 2.3), 12.0)

        // 1. Initial registration
        MapWrapperRegistry.registerWrapper(testEventId, wrapper1)
        MapWrapperRegistry.setPendingPolygons(testEventId, polygons, false)
        MapWrapperRegistry.setPendingCameraCommand(testEventId, command)

        // 2. Simulate key() change causing re-registration (wrapper changes but eventId same)
        val wrapper2 = "Wrapper2"
        MapWrapperRegistry.registerWrapper(testEventId, wrapper2)

        // 3. Verify data persists across wrapper changes (same eventId)
        assertTrue(MapWrapperRegistry.hasPendingPolygons(testEventId))
        assertTrue(MapWrapperRegistry.hasPendingCameraCommand(testEventId))
        assertNotNull(MapWrapperRegistry.getWrapper(testEventId))
        assertEquals(wrapper2, MapWrapperRegistry.getWrapper(testEventId))
    }

    // Helper function to simulate Swift calling renderPendingPolygons
    private fun renderPendingPolygons(eventId: String) {
        if (MapWrapperRegistry.hasPendingPolygons(eventId)) {
            val data = MapWrapperRegistry.getPendingPolygons(eventId)
            // Simulate rendering
            MapWrapperRegistry.clearPendingPolygons(eventId)
        }
    }
}
