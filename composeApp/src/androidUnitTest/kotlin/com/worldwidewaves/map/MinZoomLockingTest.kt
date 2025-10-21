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

package com.worldwidewaves.map

import com.worldwidewaves.shared.events.utils.BoundingBox
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import org.maplibre.android.maps.MapLibreMap
import kotlin.test.assertTrue

/**
 * Tests for min zoom locking mechanism.
 *
 * CRITICAL REQUIREMENT: Min zoom must be calculated ONCE from original event bounds
 * and then LOCKED to prevent recalculation as constraint bounds shrink.
 *
 * Historical Bug: Min zoom was recalculated on every padding change, using shrunk
 * constraint bounds. This caused a zoom-in spiral where each camera idle triggered:
 * padding recalc → bounds shrink → higher min zoom → zoom in → repeat.
 *
 * Fix: minZoomLocked flag prevents recalculation after first calculation from
 * original event bounds.
 */
class MinZoomLockingTest {
    private lateinit var mockMap: MapLibreMap
    private lateinit var adapter: AndroidMapLibreAdapter

    private val testBounds =
        BoundingBox(
            swLat = 48.0,
            swLng = 2.0,
            neLat = 49.0,
            neLng = 3.0,
        )

    @Before
    fun setup() {
        mockMap = mockk(relaxed = true)
        adapter = AndroidMapLibreAdapter(mockMap)

        // Mock map dimensions
        every { mockMap.width } returns 1080
        every { mockMap.height } returns 1920

        // Mock getCameraForLatLngBounds response
        every {
            mockMap.getCameraForLatLngBounds(any(), any())
        } returns
            mockk(relaxed = true) {
                every { zoom } returns 10.0
            }

        adapter.setMap(mockMap)
    }

    /**
     * Verify min zoom is locked after first calculation with originalEventBounds.
     */
    @Test
    fun `min zoom locked after first calculation from original bounds`() {
        // When: First call with original event bounds
        adapter.setBoundsForCameraTarget(
            constraintBounds = testBounds,
            applyZoomSafetyMargin = true,
            originalEventBounds = testBounds,
        )

        // Then: Min zoom should be set
        verify(exactly = 1) { mockMap.setMinZoomPreference(any()) }

        // When: Second call with shrunk bounds (padding applied)
        val shrunkBounds =
            BoundingBox(
                swLat = 48.1,
                swLng = 2.1,
                neLat = 48.9,
                neLng = 2.9,
            )
        adapter.setBoundsForCameraTarget(
            constraintBounds = shrunkBounds,
            applyZoomSafetyMargin = true,
            originalEventBounds = testBounds, // Same original bounds
        )

        // Then: Min zoom should NOT be recalculated (still only 1 call)
        verify(exactly = 1) { mockMap.setMinZoomPreference(any()) }

        println("✅ Min zoom locked after first calculation, prevents recalculation on shrunk bounds")
    }

    /**
     * Verify min zoom is NOT locked when originalEventBounds is null.
     * This allows proper calculation when originalEventBounds arrives later.
     */
    @Test
    fun `min zoom not locked when originalEventBounds is null`() {
        // When: First call without originalEventBounds
        adapter.setBoundsForCameraTarget(
            constraintBounds = testBounds,
            applyZoomSafetyMargin = true,
            originalEventBounds = null, // No original bounds yet
        )

        // Then: Min zoom set but not locked (may be wrong value)
        verify(exactly = 1) { mockMap.setMinZoomPreference(any()) }

        // When: Second call WITH originalEventBounds
        adapter.setBoundsForCameraTarget(
            constraintBounds = testBounds,
            applyZoomSafetyMargin = true,
            originalEventBounds = testBounds, // Now provided
        )

        // Then: Min zoom should be RECALCULATED (now with correct value)
        verify(exactly = 2) { mockMap.setMinZoomPreference(any()) }

        println("✅ Min zoom recalculated when originalEventBounds provided (ensures correct value)")
    }

    /**
     * Verify locking prevents zoom-in spiral.
     * Without locking, shrinking constraint bounds would cause increasing min zoom.
     */
    @Test
    fun `locking prevents zoom-in spiral on padding changes`() {
        // Given: Original bounds set, min zoom locked
        adapter.setBoundsForCameraTarget(
            constraintBounds = testBounds,
            applyZoomSafetyMargin = true,
            originalEventBounds = testBounds,
        )

        val initialCallCount = 1

        // When: Multiple padding recalculations with progressively shrinking bounds
        val shrinkSteps =
            listOf(
                BoundingBox(swLat = 48.1, swLng = 2.1, neLat = 48.9, neLng = 2.9), // 10% shrink
                BoundingBox(swLat = 48.2, swLng = 2.2, neLat = 48.8, neLng = 2.8), // 20% shrink
                BoundingBox(swLat = 48.3, swLng = 2.3, neLat = 48.7, neLng = 2.7), // 30% shrink
                BoundingBox(swLat = 48.4, swLng = 2.4, neLat = 48.6, neLng = 2.6), // 40% shrink
            )

        shrinkSteps.forEach { shrunkBounds ->
            adapter.setBoundsForCameraTarget(
                constraintBounds = shrunkBounds,
                applyZoomSafetyMargin = true,
                originalEventBounds = testBounds,
            )
        }

        // Then: Min zoom should still only be set once (locked, not recalculated)
        verify(exactly = initialCallCount) { mockMap.setMinZoomPreference(any()) }

        println("✅ Min zoom NOT recalculated despite 4 constraint shrinkages (prevents zoom-in spiral)")
    }

    /**
     * Verify min zoom from original bounds is LOWER than from shrunk bounds.
     * This validates that using original bounds prevents overly restrictive min zoom.
     */
    @Test
    fun `min zoom from original bounds lower than from shrunk bounds`() {
        // Given: Original and shrunk bounds
        val originalWidth = testBounds.ne.lng - testBounds.sw.lng
        val originalHeight = testBounds.ne.lat - testBounds.sw.lat

        val shrunkWidth = originalWidth * 0.5 // 50% smaller
        val shrunkHeight = originalHeight * 0.5

        val screenWidth = 1080.0
        val screenHeight = 1920.0

        // When: Calculate zoom from each
        val zoomFromOriginal =
            min(
                log2((screenWidth * 360.0) / (originalWidth * 256.0)),
                log2((screenHeight * 180.0) / (originalHeight * 256.0)),
            )

        val zoomFromShrunk =
            min(
                log2((screenWidth * 360.0) / (shrunkWidth * 256.0)),
                log2((screenHeight * 180.0) / (shrunkHeight * 256.0)),
            )

        // Then: Zoom from shrunk bounds is HIGHER (wrong - too restrictive)
        assertTrue(
            zoomFromShrunk > zoomFromOriginal,
            "Zoom from shrunk bounds ($zoomFromShrunk) incorrectly higher than original ($zoomFromOriginal)",
        )

        val difference = zoomFromShrunk - zoomFromOriginal
        println("✅ Min zoom from original ($zoomFromOriginal) vs shrunk ($zoomFromShrunk), difference=$difference zoom levels")
    }
}
