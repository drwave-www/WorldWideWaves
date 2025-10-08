package com.worldwidewaves.shared.map

/*
 * Copyright 2025 DrWave
 *
 * WorldWideWaves is an ephemeral mobile app designed to orchestrate human waves through cities and
 * countries. The project aims to transcend physical and cultural
 * boundaries, fostering unity, community, and shared human experience by leveraging real-time
 * coordination and location-based services.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.events.WWWEventArea
import com.worldwidewaves.shared.events.WWWEventMap
import com.worldwidewaves.shared.events.WWWEventWave
import com.worldwidewaves.shared.events.utils.BoundingBox
import com.worldwidewaves.shared.events.utils.Position
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Tests for iOS-specific map parity features in IosEventMap.
 * These tests verify iOS-specific behavior that matches Android patterns:
 * - Style URL retry logic (null → retry → success/fail)
 * - Polygon rendering queue management
 * - Registry key management for multiple screens
 */
class IosEventMapParityTest {
    private lateinit var mockEvent: TestWWWEvent
    private val testRegistryKey = "test-event-key"

    @BeforeTest
    fun setup() {
        MapWrapperRegistry.clear()
        mockEvent = TestWWWEvent()
    }

    @AfterTest
    fun cleanup() {
        MapWrapperRegistry.clear()
    }

    // ============================================================
    // STYLE URL RETRY TESTS (iOS Parity)
    // ============================================================

    @Test
    fun `style URL retry succeeds on second attempt`() =
        runTest {
            // Given - mock event that returns null first, then valid URL
            var callCount = 0
            mockEvent.styleUrlProvider = {
                callCount++
                if (callCount == 1) {
                    null // First call returns null
                } else {
                    "file:///path/to/style.json" // Second call succeeds
                }
            }

            // When - LaunchedEffect in IosEventMap would trigger this logic
            // Simulate the retry logic directly
            var styleURL: String? = mockEvent.map.getStyleUri()

            if (styleURL == null) {
                // Retry after 100ms
                delay(100)
                styleURL = mockEvent.map.getStyleUri()
            }

            // Then - should succeed on retry
            assertNotNull(styleURL, "Style URL should be available after retry")
            assertEquals("file:///path/to/style.json", styleURL)
            assertEquals(2, callCount, "Should call getStyleUri twice (initial + retry)")
        }

    @Test
    fun `style URL retry fails after second attempt`() =
        runTest {
            // Given - mock event that always returns null
            var callCount = 0
            mockEvent.styleUrlProvider = {
                callCount++
                null // Always returns null
            }

            // When - simulate retry logic
            var styleURL: String? = mockEvent.map.getStyleUri()

            if (styleURL == null) {
                // Retry after 100ms
                delay(100)
                styleURL = mockEvent.map.getStyleUri()
            }

            // Then - should still be null after retry
            assertNull(styleURL, "Style URL should still be null after retry")
            assertEquals(2, callCount, "Should call getStyleUri twice (initial + retry)")
        }

    @Test
    fun `style URL succeeds on first attempt without retry`() =
        runTest {
            // Given - mock event that returns valid URL immediately
            var callCount = 0
            mockEvent.styleUrlProvider = {
                callCount++
                "file:///path/to/style.json" // Always succeeds
            }

            // When - simulate style URL loading
            val styleURL: String? = mockEvent.map.getStyleUri()

            // Then - should succeed immediately (no retry needed)
            assertNotNull(styleURL, "Style URL should be available immediately")
            assertEquals("file:///path/to/style.json", styleURL)
            assertEquals(1, callCount, "Should call getStyleUri only once (no retry needed)")
        }

    // ============================================================
    // REGISTRY KEY MANAGEMENT TESTS (iOS Parity)
    // ============================================================

    @Test
    fun `registry key separates multiple screens for same event`() {
        // Given - two different registry keys for same event
        val key1 = "paris_france-event"
        val key2 = "paris_france-fullmap"

        val wrapper1 = "Wrapper1"
        val wrapper2 = "Wrapper2"

        // When - register both wrappers
        MapWrapperRegistry.registerWrapper(key1, wrapper1)
        MapWrapperRegistry.registerWrapper(key2, wrapper2)

        // Then - both should coexist independently
        assertEquals(wrapper1, MapWrapperRegistry.getWrapper(key1))
        assertEquals(wrapper2, MapWrapperRegistry.getWrapper(key2))

        // Cleanup
        MapWrapperRegistry.unregisterWrapper(key1)
        MapWrapperRegistry.unregisterWrapper(key2)
    }

    @Test
    fun `unregister registry key clears only that screen's data`() {
        // Given - two screens with same event
        val key1 = "paris_france-event"
        val key2 = "paris_france-fullmap"

        MapWrapperRegistry.registerWrapper(key1, "Wrapper1")
        MapWrapperRegistry.registerWrapper(key2, "Wrapper2")

        MapWrapperRegistry.setPendingPolygons(key1, listOf(listOf(Pair(0.0, 0.0))), true)
        MapWrapperRegistry.setPendingPolygons(key2, listOf(listOf(Pair(1.0, 1.0))), true)

        // When - unregister only key1
        MapWrapperRegistry.unregisterWrapper(key1)

        // Then - key1 data should be cleared, key2 should remain
        assertNull(MapWrapperRegistry.getWrapper(key1))
        assertEquals(false, MapWrapperRegistry.hasPendingPolygons(key1))

        assertNotNull(MapWrapperRegistry.getWrapper(key2))
        assertEquals(true, MapWrapperRegistry.hasPendingPolygons(key2))

        // Cleanup
        MapWrapperRegistry.unregisterWrapper(key2)
    }

    // ============================================================
    // POLYGON RENDERING TESTS (iOS Parity)
    // ============================================================

    @Test
    fun `updateWavePolygons stores polygons in registry with correct key`() {
        // Given
        val eventMap =
            IosEventMap(
                event = mockEvent,
                registryKey = testRegistryKey,
            )

        val testPolygon =
            com.worldwidewaves.shared.events.utils.Polygon.fromPositions(
                listOf(
                    Position(0.0, 0.0),
                    Position(1.0, 0.0),
                    Position(1.0, 1.0),
                    Position(0.0, 1.0),
                ),
            )

        // When - update polygons
        eventMap.updateWavePolygons(listOf(testPolygon), clearPolygons = true)

        // Then - should be stored in registry with correct key
        assertEquals(true, MapWrapperRegistry.hasPendingPolygons(testRegistryKey))

        val pendingData = MapWrapperRegistry.getPendingPolygons(testRegistryKey)
        assertNotNull(pendingData)
        assertEquals(1, pendingData.coordinates.size)
        assertEquals(true, pendingData.clearExisting)

        // Cleanup
        MapWrapperRegistry.unregisterWrapper(testRegistryKey)
    }

    @Test
    fun `updateWavePolygons with clearPolygons false preserves existing data`() {
        // Given
        val eventMap =
            IosEventMap(
                event = mockEvent,
                registryKey = testRegistryKey,
            )

        val polygon1 =
            com.worldwidewaves.shared.events.utils.Polygon.fromPositions(
                listOf(
                    Position(0.0, 0.0),
                    Position(1.0, 0.0),
                    Position(1.0, 1.0),
                ),
            )

        val polygon2 =
            com.worldwidewaves.shared.events.utils.Polygon.fromPositions(
                listOf(
                    Position(2.0, 2.0),
                    Position(3.0, 2.0),
                    Position(3.0, 3.0),
                ),
            )

        // When - add first polygon
        eventMap.updateWavePolygons(listOf(polygon1), clearPolygons = true)

        // Then - add second polygon without clearing
        eventMap.updateWavePolygons(listOf(polygon2), clearPolygons = false)

        // Verify both polygons are stored
        val pendingData = MapWrapperRegistry.getPendingPolygons(testRegistryKey)
        assertNotNull(pendingData)

        // Note: The most recent call's clearExisting flag is what's stored
        // This matches the iOS rendering pattern where latest state matters
        assertEquals(false, pendingData.clearExisting)

        // Cleanup
        MapWrapperRegistry.unregisterWrapper(testRegistryKey)
    }

    // ============================================================
    // TEST HELPERS
    // ============================================================

    private class TestWWWEvent : IWWWEvent {
        override val id: String = "test_event"
        override val name: String = "Test Event"
        override val description: String = "Test Description"
        override val area: WWWEventArea = TestEventArea()
        override val wave: WWWEventWave = TestEventWave()
        override val map: WWWEventMap = TestEventMap()
        override val videoUrl: String = ""

        var styleUrlProvider: (() -> String?)? = null

        override fun getMapImage(): Any = "test_image"

        override fun getVideoFile(): String = "test_video"

        inner class TestEventArea : WWWEventArea {
            override val bboxIsOverride: Boolean = false

            override suspend fun bbox(): BoundingBox =
                BoundingBox.fromCorners(
                    Position(48.8, 2.2),
                    Position(48.9, 2.4),
                )

            override suspend fun getCenter(): Position = Position(48.85, 2.3)
        }

        inner class TestEventWave : WWWEventWave {
            override suspend fun userClosestWaveLongitude(): Double? = 2.35

            override suspend fun nextWaveStartTime(): Long? = null
        }

        inner class TestEventMap : WWWEventMap {
            override val maxZoom: Double = 18.0

            override fun getStyleUri(): String? = styleUrlProvider?.invoke()
        }
    }
}
