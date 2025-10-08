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
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for iOS/Android map parity features in MapWrapperRegistry.
 * These tests verify camera command handling that matches Android behavior:
 * - SetAttributionMargins command creation and storage
 * - Command queue ordering (config vs animation)
 * - Command execution priority (config commands execute first)
 */
class MapWrapperRegistryParityTest {
    @BeforeTest
    fun setup() {
        MapWrapperRegistry.clear()
    }

    @AfterTest
    fun cleanup() {
        MapWrapperRegistry.clear()
    }

    // ============================================================
    // SETATTRIBUTIONMARGINS COMMAND TESTS (iOS Parity)
    // ============================================================

    @Test
    fun `setAttributionMarginsCommand creates correct command`() {
        // Given
        val eventId = "test_event"
        val left = 10
        val top = 20
        val right = 30
        val bottom = 40

        // When
        MapWrapperRegistry.setAttributionMarginsCommand(eventId, left, top, right, bottom)

        // Then - should create SetAttributionMargins command
        assertTrue(MapWrapperRegistry.hasPendingCameraCommand(eventId))

        val command = MapWrapperRegistry.getPendingCameraCommand(eventId)
        assertNotNull(command)
        assertIs<CameraCommand.SetAttributionMargins>(command)
        assertEquals(left, command.left)
        assertEquals(top, command.top)
        assertEquals(right, command.right)
        assertEquals(bottom, command.bottom)
    }

    @Test
    fun `setAttributionMarginsCommand queues in config commands`() {
        // Given
        val eventId = "test_event"

        // When - add attribution margins command
        MapWrapperRegistry.setAttributionMarginsCommand(eventId, 10, 20, 30, 40)

        // Then - should be stored in config queue (not animation slot)
        val command = MapWrapperRegistry.getPendingCameraCommand(eventId)
        assertIs<CameraCommand.SetAttributionMargins>(command)

        // Clear and verify it was in config queue
        MapWrapperRegistry.clearPendingCameraCommand(eventId)
        assertEquals(false, MapWrapperRegistry.hasPendingCameraCommand(eventId))
    }

    // ============================================================
    // COMMAND QUEUE ORDERING TESTS (iOS Parity)
    // ============================================================

    @Test
    fun `config commands execute before animation commands`() {
        // Given
        val eventId = "test_event"

        // Add animation command first
        val position = Position(48.8566, 2.3522)
        MapWrapperRegistry.setPendingCameraCommand(
            eventId,
            CameraCommand.AnimateToPosition(position, zoom = 12.0),
        )

        // Add config command after
        MapWrapperRegistry.setAttributionMarginsCommand(eventId, 10, 20, 30, 40)

        // When - get next command
        val firstCommand = MapWrapperRegistry.getPendingCameraCommand(eventId)

        // Then - config command should be returned first (higher priority)
        assertNotNull(firstCommand)
        assertIs<CameraCommand.SetAttributionMargins>(firstCommand)

        // Clear config command
        MapWrapperRegistry.clearPendingCameraCommand(eventId)

        // When - get next command again
        val secondCommand = MapWrapperRegistry.getPendingCameraCommand(eventId)

        // Then - animation command should be returned now
        assertNotNull(secondCommand)
        assertIs<CameraCommand.AnimateToPosition>(secondCommand)
    }

    @Test
    fun `multiple config commands execute in FIFO order`() {
        // Given
        val eventId = "test_event"

        // Add multiple config commands
        MapWrapperRegistry.setMinZoomCommand(eventId, 10.0)
        MapWrapperRegistry.setMaxZoomCommand(eventId, 18.0)
        MapWrapperRegistry.setAttributionMarginsCommand(eventId, 10, 20, 30, 40)

        // When/Then - commands should execute in order (FIFO)

        // First command: SetMinZoom
        val cmd1 = MapWrapperRegistry.getPendingCameraCommand(eventId)
        assertNotNull(cmd1)
        assertIs<CameraCommand.SetMinZoom>(cmd1)
        assertEquals(10.0, cmd1.minZoom)
        MapWrapperRegistry.clearPendingCameraCommand(eventId)

        // Second command: SetMaxZoom
        val cmd2 = MapWrapperRegistry.getPendingCameraCommand(eventId)
        assertNotNull(cmd2)
        assertIs<CameraCommand.SetMaxZoom>(cmd2)
        assertEquals(18.0, cmd2.maxZoom)
        MapWrapperRegistry.clearPendingCameraCommand(eventId)

        // Third command: SetAttributionMargins
        val cmd3 = MapWrapperRegistry.getPendingCameraCommand(eventId)
        assertNotNull(cmd3)
        assertIs<CameraCommand.SetAttributionMargins>(cmd3)
        assertEquals(10, cmd3.left)
        assertEquals(20, cmd3.top)
        assertEquals(30, cmd3.right)
        assertEquals(40, cmd3.bottom)
        MapWrapperRegistry.clearPendingCameraCommand(eventId)

        // No more commands
        assertEquals(false, MapWrapperRegistry.hasPendingCameraCommand(eventId))
    }

    @Test
    fun `animation commands use single slot latest wins`() {
        // Given
        val eventId = "test_event"

        // Add multiple animation commands
        val position1 = Position(48.8566, 2.3522) // Paris
        val position2 = Position(40.7128, -74.0060) // New York
        val position3 = Position(51.5074, -0.1278) // London

        MapWrapperRegistry.setPendingCameraCommand(
            eventId,
            CameraCommand.AnimateToPosition(position1, zoom = 12.0),
        )
        MapWrapperRegistry.setPendingCameraCommand(
            eventId,
            CameraCommand.AnimateToPosition(position2, zoom = 13.0),
        )
        MapWrapperRegistry.setPendingCameraCommand(
            eventId,
            CameraCommand.AnimateToPosition(position3, zoom = 14.0),
        )

        // When - get animation command
        val command = MapWrapperRegistry.getPendingCameraCommand(eventId)

        // Then - should get only the LATEST animation command (position3)
        assertNotNull(command)
        assertIs<CameraCommand.AnimateToPosition>(command)
        assertEquals(position3.lat, command.position.lat, 0.0001)
        assertEquals(position3.lng, command.position.lng, 0.0001)
        assertEquals(14.0, command.zoom)
    }

    // ============================================================
    // COMMAND EXECUTION ORDER TESTS (iOS Parity)
    // ============================================================

    @Test
    fun `setAttributionMargins executes before camera animations`() {
        // Given
        val eventId = "test_event"

        // Simulate AbstractEventMap.setupMap() sequence:
        // 1. setAttributionMargins (config)
        // 2. setMaxZoomPreference (config)
        // 3. moveToWindowBounds (animation)

        MapWrapperRegistry.setAttributionMarginsCommand(eventId, 0, 0, 0, 0)
        MapWrapperRegistry.setMaxZoomCommand(eventId, 18.0)

        val bounds =
            BoundingBox.fromCorners(
                Position(48.0, 2.0),
                Position(49.0, 3.0),
            )!!
        MapWrapperRegistry.setPendingCameraCommand(
            eventId,
            CameraCommand.AnimateToBounds(bounds, padding = 50),
        )

        // When - execute commands in order
        val executionOrder = mutableListOf<String>()

        while (MapWrapperRegistry.hasPendingCameraCommand(eventId)) {
            val command = MapWrapperRegistry.getPendingCameraCommand(eventId)
            assertNotNull(command)

            executionOrder.add(command::class.simpleName ?: "Unknown")
            MapWrapperRegistry.clearPendingCameraCommand(eventId)
        }

        // Then - config commands should execute before animation
        assertEquals(3, executionOrder.size)
        assertEquals("SetAttributionMargins", executionOrder[0])
        assertEquals("SetMaxZoom", executionOrder[1])
        assertEquals("AnimateToBounds", executionOrder[2])
    }

    @Test
    fun `constraint bounds command executes before camera movements`() {
        // Given
        val eventId = "test_event"

        // Simulate constraint bounds setup before movement
        val constraintBounds =
            BoundingBox.fromCorners(
                Position(48.0, 2.0),
                Position(49.0, 3.0),
            )!!

        MapWrapperRegistry.setPendingCameraCommand(
            eventId,
            CameraCommand.SetConstraintBounds(constraintBounds),
        )

        val targetPosition = Position(48.5, 2.5)
        MapWrapperRegistry.setPendingCameraCommand(
            eventId,
            CameraCommand.AnimateToPosition(targetPosition, zoom = 12.0),
        )

        // When - get first command
        val firstCommand = MapWrapperRegistry.getPendingCameraCommand(eventId)

        // Then - should be constraint bounds (config)
        assertNotNull(firstCommand)
        assertIs<CameraCommand.SetConstraintBounds>(firstCommand)

        MapWrapperRegistry.clearPendingCameraCommand(eventId)

        // When - get second command
        val secondCommand = MapWrapperRegistry.getPendingCameraCommand(eventId)

        // Then - should be animation
        assertNotNull(secondCommand)
        assertIs<CameraCommand.AnimateToPosition>(secondCommand)
    }

    // ============================================================
    // COMMAND CLEANUP TESTS (iOS Parity)
    // ============================================================

    @Test
    fun `unregisterWrapper clears all pending commands`() {
        // Given
        val eventId = "test_event"
        val wrapper = "TestWrapper"

        MapWrapperRegistry.registerWrapper(eventId, wrapper)

        // Add various commands
        MapWrapperRegistry.setAttributionMarginsCommand(eventId, 10, 20, 30, 40)
        MapWrapperRegistry.setMinZoomCommand(eventId, 10.0)

        val position = Position(48.8566, 2.3522)
        MapWrapperRegistry.setPendingCameraCommand(
            eventId,
            CameraCommand.AnimateToPosition(position, zoom = 12.0),
        )

        // Verify commands exist
        assertTrue(MapWrapperRegistry.hasPendingCameraCommand(eventId))

        // When - unregister wrapper
        MapWrapperRegistry.unregisterWrapper(eventId)

        // Then - all commands should be cleared
        assertEquals(false, MapWrapperRegistry.hasPendingCameraCommand(eventId))
    }
}
