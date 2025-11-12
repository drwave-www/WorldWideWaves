@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.worldwidewaves.shared.domain.observation

/* * Copyright 2025 DrWave
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
 * limitations under the License. */

import com.worldwidewaves.shared.domain.progression.WaveProgressionTracker
import com.worldwidewaves.shared.events.utils.IClock
import com.worldwidewaves.shared.events.utils.Position
import com.worldwidewaves.shared.position.PositionManager
import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PositionObserverBasicTest {
    private val mockPositionManager = mockk<PositionManager>()
    private val mockWaveProgressionTracker = mockk<WaveProgressionTracker>()
    private val mockClock = mockk<IClock>()

    private val observer =
        DefaultPositionObserver(
            mockPositionManager,
            mockWaveProgressionTracker,
            mockClock,
        )

    @Test
    fun `can create DefaultPositionObserver`() {
        val observer =
            DefaultPositionObserver(
                mockPositionManager,
                mockWaveProgressionTracker,
                mockClock,
            )

        assertFalse(observer.isObserving())
    }

    @Test
    fun `getCurrentPosition delegates to manager`() {
        val expectedPosition = Position(40.7128, -74.0060)
        every { mockPositionManager.getCurrentPosition() } returns expectedPosition

        val result = observer.getCurrentPosition()

        assertEquals(expectedPosition, result)
    }

    @Test
    fun `isValidPosition returns true for valid coordinates`() {
        val validPositions =
            listOf(
                Position(0.0, 0.0), // Equator/Prime Meridian
                Position(40.7128, -74.0060), // New York
                Position(-33.8688, 151.2093), // Sydney
                Position(89.9, 179.9), // Near North Pole
                Position(-89.9, -179.9), // Near South Pole
            )

        validPositions.forEach { position ->
            assertTrue(observer.isValidPosition(position), "Position $position should be valid")
        }
    }

    @Test
    fun `Position constructor rejects invalid coordinates`() {
        // Position now validates coordinates in constructor and throws IllegalArgumentException
        // Test that all invalid coordinates are rejected

        // Latitude too high
        assertFailsWith<IllegalArgumentException> {
            Position(91.0, 0.0)
        }

        // Latitude too low
        assertFailsWith<IllegalArgumentException> {
            Position(-91.0, 0.0)
        }

        // Longitude too high
        assertFailsWith<IllegalArgumentException> {
            Position(0.0, 181.0)
        }

        // Longitude too low
        assertFailsWith<IllegalArgumentException> {
            Position(0.0, -181.0)
        }

        // NaN latitude
        assertFailsWith<IllegalArgumentException> {
            Position(Double.NaN, 0.0)
        }

        // NaN longitude
        assertFailsWith<IllegalArgumentException> {
            Position(0.0, Double.NaN)
        }

        // Infinite latitude
        assertFailsWith<IllegalArgumentException> {
            Position(Double.POSITIVE_INFINITY, 0.0)
        }

        // Infinite longitude
        assertFailsWith<IllegalArgumentException> {
            Position(0.0, Double.NEGATIVE_INFINITY)
        }
    }

    @Test
    fun `calculateDistance returns zero for same coordinates`() {
        val position = Position(40.7128, -74.0060)

        val distance = observer.calculateDistance(position, position)

        assertEquals(0.0, distance, 0.1)
    }

    @Test
    fun `calculateDistance works correctly for valid coordinates`() {
        // Position now validates coordinates, so we test with valid coordinates only
        val newYork = Position(40.7128, -74.0060)
        val london = Position(51.5074, -0.1278)

        val distance = observer.calculateDistance(newYork, london)

        // Distance should be positive, finite, and reasonable for trans-Atlantic distance
        // (between 3000 km and 20000 km = 3,000,000 to 20,000,000 meters)
        assertTrue(
            distance > 3_000_000.0 && distance < 20_000_000.0,
            "Distance should be a reasonable trans-Atlantic distance, got $distance meters",
        )
        assertTrue(distance.isFinite() && !distance.isNaN(), "Distance should be finite and not NaN")
    }

    @Test
    fun `calculateDistance returns non-negative finite distance for valid coordinates`() {
        // Basic sanity check for distance calculation
        val pos1 = Position(40.7128, -74.0060) // NYC
        val pos2 = Position(34.0522, -118.2437) // Los Angeles

        val distance = observer.calculateDistance(pos1, pos2)

        // Just check it's not negative, not zero, not infinity, and is finite
        assertTrue(
            distance > 0.0 && distance.isFinite() && !distance.isNaN(),
            "Distance $distance should be positive and finite",
        )
    }

    @Test
    fun `stopObservation sets observing to false`() {
        observer.stopObservation()

        assertFalse(observer.isObserving())
    }
}
