package com.worldwidewaves.map

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
import io.mockk.mockk
import io.mockk.verify
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.geojson.Polygon
import kotlin.test.Test
import kotlin.test.assertFailsWith

/**
 * Simplified tests for Android-iOS map parity features.
 * These tests focus on behavior validation rather than complex mock interactions.
 */
class AndroidMapLibreAdapterSimpleParityTest {
    // ============================================================
    // POLYGON QUEUEING TESTS (Behavioral)
    // ============================================================

    @Test
    fun `addWavePolygons does not interact with map when style not loaded`() {
        // Given
        val mockMap = mockk<MapLibreMap>(relaxed = true)
        val adapter = AndroidMapLibreAdapter(mockMap)

        val polygon = mockk<Polygon>(relaxed = true)
        val polygons = listOf(polygon)

        // When - add polygons before style loads
        adapter.addWavePolygons(polygons, clearExisting = true)

        // Then - should NOT call any map methods (polygons are queued)
        verify(exactly = 0) { mockMap.getStyle(any()) }
    }

    @Test
    fun `addWavePolygons accepts multiple calls before style loads`() {
        // Given
        val mockMap = mockk<MapLibreMap>(relaxed = true)
        val adapter = AndroidMapLibreAdapter(mockMap)

        val polygon1 = mockk<Polygon>(relaxed = true)
        val polygon2 = mockk<Polygon>(relaxed = true)

        // When - queue multiple polygon sets before style loads
        adapter.addWavePolygons(listOf(polygon1), clearExisting = true)
        adapter.addWavePolygons(listOf(polygon2), clearExisting = true)

        // Then - should NOT crash, all calls are queued
        verify(exactly = 0) { mockMap.getStyle(any()) }
    }

    // ============================================================
    // BOUNDS VALIDATION TESTS
    // ============================================================

    @Test
    fun `setBoundsForCameraTarget validates latitude range`() {
        // Given
        val mockMap = mockk<MapLibreMap>(relaxed = true)
        val adapter = AndroidMapLibreAdapter(mockMap)
        adapter.setMap(mockMap)

        // Invalid latitude (BoundingBox.fromCorners auto-fixes, so test with invalid direct value)
        val invalidBounds =
            BoundingBox.fromCorners(
                Position(-100.0, 2.0), // Will be clamped by BoundingBox, but validation will catch
                Position(49.0, 3.0),
            )

        // When/Then
        assertFailsWith<IllegalArgumentException> {
            adapter.setBoundsForCameraTarget(invalidBounds)
        }
    }

    @Test
    fun `setBoundsForCameraTarget validates longitude range`() {
        // Given
        val mockMap = mockk<MapLibreMap>(relaxed = true)
        val adapter = AndroidMapLibreAdapter(mockMap)
        adapter.setMap(mockMap)

        // Invalid longitude
        val invalidBounds =
            BoundingBox.fromCorners(
                Position(48.0, -200.0),
                Position(49.0, 3.0),
            )

        // When/Then
        assertFailsWith<IllegalArgumentException> {
            adapter.setBoundsForCameraTarget(invalidBounds)
        }
    }

    @Test
    fun `setBoundsForCameraTarget accepts valid bounds`() {
        // Given
        val mockMap = mockk<MapLibreMap>(relaxed = true)
        val adapter = AndroidMapLibreAdapter(mockMap)
        adapter.setMap(mockMap)

        // Valid bounds
        val validBounds =
            BoundingBox.fromCorners(
                Position(48.0, 2.0),
                Position(49.0, 3.0),
            )

        // When/Then - should NOT throw
        adapter.setBoundsForCameraTarget(validBounds)

        // Verify method was called
        verify { mockMap.setLatLngBoundsForCameraTarget(any()) }
    }
}
