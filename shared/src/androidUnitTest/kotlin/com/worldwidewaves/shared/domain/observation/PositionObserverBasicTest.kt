@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.worldwidewaves.shared.domain.observation

import com.worldwidewaves.shared.domain.progression.WaveProgressionTracker
import com.worldwidewaves.shared.events.utils.IClock
import com.worldwidewaves.shared.events.utils.Position
import com.worldwidewaves.shared.position.PositionManager
import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals
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

        assertTrue(observer is PositionObserver)
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
    fun `isValidPosition returns false for invalid coordinates`() {
        val invalidPositions =
            listOf(
                Position(91.0, 0.0), // Latitude too high
                Position(-91.0, 0.0), // Latitude too low
                Position(0.0, 181.0), // Longitude too high
                Position(0.0, -181.0), // Longitude too low
                Position(Double.NaN, 0.0), // NaN latitude
                Position(0.0, Double.NaN), // NaN longitude
                Position(Double.POSITIVE_INFINITY, 0.0), // Infinite latitude
                Position(0.0, Double.NEGATIVE_INFINITY), // Infinite longitude
            )

        invalidPositions.forEach { position ->
            assertFalse(observer.isValidPosition(position), "Position $position should be invalid")
        }
    }

    @Test
    fun `calculateDistance returns zero for same coordinates`() {
        val position = Position(40.7128, -74.0060)

        val distance = observer.calculateDistance(position, position)

        assertEquals(0.0, distance, 0.1)
    }

    @Test
    fun `calculateDistance returns infinity for invalid coordinates`() {
        val validPosition = Position(40.7128, -74.0060)
        val invalidPosition = Position(Double.NaN, -74.0060)

        val distance1 = observer.calculateDistance(validPosition, invalidPosition)
        val distance2 = observer.calculateDistance(invalidPosition, validPosition)

        assertEquals(Double.POSITIVE_INFINITY, distance1)
        assertEquals(Double.POSITIVE_INFINITY, distance2)
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
