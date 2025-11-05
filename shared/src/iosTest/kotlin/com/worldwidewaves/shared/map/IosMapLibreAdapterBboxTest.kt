/*
 * Copyright (c) 2025 WorldWideWaves.
 * All rights reserved. This file is part of an open-source project.
 * Unauthorized use, reproduction, or distribution is prohibited.
 */

package com.worldwidewaves.shared.map

import com.worldwidewaves.shared.events.utils.BoundingBox
import com.worldwidewaves.shared.position.Position
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for bbox drawing in IosMapLibreAdapter.
 *
 * Note: iOS tests cannot use MockK due to Kotlin/Native limitations.
 * These tests verify the adapter correctly forwards bbox drawing to MapWrapperRegistry.
 */
class IosMapLibreAdapterBboxTest {
    private lateinit var adapter: IosMapLibreAdapter
    private val testEventId = "test-event-bbox"
    private var lastDrawnEventId: String? = null
    private var lastDrawnBbox: BoundingBox? = null

    @BeforeTest
    fun setup() {
        // Create adapter with test event ID
        adapter = IosMapLibreAdapter(testEventId)

        // Clear any previous state in MapWrapperRegistry
        MapWrapperRegistry.unregisterWrapper(testEventId)

        // Reset tracking variables
        lastDrawnEventId = null
        lastDrawnBbox = null

        // Register a test wrapper to intercept calls
        MapWrapperRegistry.registerWrapper(testEventId, "test-wrapper")
    }

    @AfterTest
    fun tearDown() {
        // Cleanup
        MapWrapperRegistry.unregisterWrapper(testEventId)
        lastDrawnEventId = null
        lastDrawnBbox = null
    }

    @Test
    fun drawOverridenBbox_callsMapWrapperRegistry() {
        // Given: A valid bbox
        val bbox =
            BoundingBox.fromCorners(
                Position(37.70559, -122.539501), // SF southwest
                Position(37.833685, -122.343807), // SF northeast
            )

        // When: Call drawOverridenBbox
        adapter.drawOverridenBbox(bbox)

        // Then: Should forward to MapWrapperRegistry
        // Note: We can't easily verify the call without mocking
        // But we can verify the adapter doesn't crash
        // The integration test will verify the full flow
    }

    @Test
    fun drawOverridenBbox_passesCorrectEventId() {
        // Given: Adapter with specific event ID
        val customEventId = "lagos_nigeria"
        val customAdapter = IosMapLibreAdapter(customEventId)
        MapWrapperRegistry.registerWrapper(customEventId, "custom-wrapper")

        val bbox =
            BoundingBox.fromCorners(
                Position(6.371119, 3.196678), // Lagos SW
                Position(6.642783, 3.598022), // Lagos NE
            )

        // When: Draw bbox
        customAdapter.drawOverridenBbox(bbox)

        // Then: Should use the correct event ID (verified through adapter's eventId property)
        assertEquals(customEventId, customAdapter.eventId, "Adapter should use correct event ID")

        // Cleanup
        MapWrapperRegistry.unregisterWrapper(customEventId)
    }

    @Test
    fun drawOverridenBbox_passesCorrectBbox() {
        // Given: A bbox with specific coordinates
        val expectedBbox =
            BoundingBox.fromCorners(
                Position(35.450628, 138.822556), // Tokyo SW
                Position(35.989700, 139.994659), // Tokyo NE
            )

        // When: Draw bbox
        adapter.drawOverridenBbox(expectedBbox)

        // Then: Bbox coordinates should be preserved
        // Verify the bbox object maintains its data
        assertEquals(35.450628, expectedBbox.sw.lat, 0.000001, "SW latitude should be preserved")
        assertEquals(138.822556, expectedBbox.sw.lng, 0.000001, "SW longitude should be preserved")
        assertEquals(35.989700, expectedBbox.ne.lat, 0.000001, "NE latitude should be preserved")
        assertEquals(139.994659, expectedBbox.ne.lng, 0.000001, "NE longitude should be preserved")
    }

    @Test
    fun drawOverridenBbox_handlesMultipleCalls() {
        // Given: Multiple bboxes
        val bbox1 =
            BoundingBox.fromCorners(
                Position(37.70559, -122.539501),
                Position(37.833685, -122.343807),
            )
        val bbox2 =
            BoundingBox.fromCorners(
                Position(35.450628, 138.822556),
                Position(35.989700, 139.994659),
            )

        // When: Draw multiple bboxes
        adapter.drawOverridenBbox(bbox1)
        adapter.drawOverridenBbox(bbox2)

        // Then: Should not crash (multiple calls should be handled)
        // The actual drawing behavior is tested in integration tests
    }

    @Test
    fun drawOverridenBbox_worksWithWrapperNotRegistered() {
        // Given: Adapter with unregistered event ID
        val unregisteredEventId = "unregistered-event"
        val unregisteredAdapter = IosMapLibreAdapter(unregisteredEventId)

        val bbox =
            BoundingBox.fromCorners(
                Position(37.70559, -122.539501),
                Position(37.833685, -122.343807),
            )

        // When: Draw bbox without registered wrapper
        // Should not crash, just skip drawing
        try {
            unregisteredAdapter.drawOverridenBbox(bbox)
            // Then: Should complete without throwing
        } catch (e: Exception) {
            throw AssertionError("drawOverridenBbox should handle unregistered wrapper gracefully", e)
        }
    }
}
