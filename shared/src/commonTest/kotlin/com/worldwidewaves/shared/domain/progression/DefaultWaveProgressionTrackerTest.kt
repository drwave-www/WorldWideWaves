package com.worldwidewaves.shared.domain.progression

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
import com.worldwidewaves.shared.events.WWWEventWave
import com.worldwidewaves.shared.events.utils.IClock
import com.worldwidewaves.shared.events.utils.Position
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class DefaultWaveProgressionTrackerTest {

    private val mockClock = mockk<IClock>()
    private val tracker = DefaultWaveProgressionTracker(mockClock)

    @Test
    fun `calculateProgression returns 0 percent when event not running`() = runTest {
        // Given
        val mockEvent = mockk<IWWWEvent>()
        every { mockEvent.isDone() } returns false
        every { mockEvent.isRunning() } returns false

        // When
        val progression = tracker.calculateProgression(mockEvent)

        // Then
        assertEquals(0.0, progression)
    }

    @Test
    fun `calculateProgression returns 100 percent when event is done`() = runTest {
        // Given
        val mockEvent = mockk<IWWWEvent>()
        every { mockEvent.isDone() } returns true

        // When
        val progression = tracker.calculateProgression(mockEvent)

        // Then
        assertEquals(100.0, progression)
    }

    @Test
    fun `calculateProgression returns 50 percent when half duration elapsed`() = runTest {
        // Given
        val startTime = Instant.fromEpochSeconds(1000)
        val currentTime = Instant.fromEpochSeconds(1300) // 300 seconds later
        val duration = 600.seconds // Total duration 600 seconds

        val mockEvent = mockk<IWWWEvent>()
        val mockWave = mockk<WWWEventWave>()

        every { mockEvent.isDone() } returns false
        every { mockEvent.isRunning() } returns true
        every { mockEvent.getWaveStartDateTime() } returns startTime
        every { mockEvent.wave } returns mockWave
        every { mockWave.getWaveDuration() } returns duration
        every { mockClock.now() } returns currentTime

        // When
        val progression = tracker.calculateProgression(mockEvent)

        // Then
        assertEquals(50.0, progression, 0.1)
    }

    @Test
    fun `calculateProgression clamps to 100 percent when time exceeds duration`() = runTest {
        // Given
        val startTime = Instant.fromEpochSeconds(1000)
        val currentTime = Instant.fromEpochSeconds(2000) // 1000 seconds later
        val duration = 600.seconds // Total duration 600 seconds

        val mockEvent = mockk<IWWWEvent>()
        val mockWave = mockk<WWWEventWave>()

        every { mockEvent.isDone() } returns false
        every { mockEvent.isRunning() } returns true
        every { mockEvent.getWaveStartDateTime() } returns startTime
        every { mockEvent.wave } returns mockWave
        every { mockWave.getWaveDuration() } returns duration
        every { mockClock.now() } returns currentTime

        // When
        val progression = tracker.calculateProgression(mockEvent)

        // Then
        assertEquals(100.0, progression)
    }

    @Test
    fun `calculateProgression returns 0 when total time is invalid`() = runTest {
        // Given
        val startTime = Instant.fromEpochSeconds(1000)
        val currentTime = Instant.fromEpochSeconds(1300)
        val duration = 0.seconds // Invalid duration

        val mockEvent = mockk<IWWWEvent>()
        val mockWave = mockk<WWWEventWave>()

        every { mockEvent.isDone() } returns false
        every { mockEvent.isRunning() } returns true
        every { mockEvent.getWaveStartDateTime() } returns startTime
        every { mockEvent.wave } returns mockWave
        every { mockWave.getWaveDuration() } returns duration
        every { mockClock.now() } returns currentTime
        every { mockEvent.id } returns "test-event"

        // When
        val progression = tracker.calculateProgression(mockEvent)

        // Then
        assertEquals(0.0, progression)
    }

    @Test
    fun `isUserInWaveArea returns false when no polygons available`() = runTest {
        // Given
        val userPosition = Position(40.7128, -74.0060)
        val mockArea = mockk<WWWEventArea>()
        coEvery { mockArea.getPolygons() } returns emptyList()

        // When
        val result = tracker.isUserInWaveArea(userPosition, mockArea)

        // Then
        assertFalse(result)
    }

    @Test
    fun `isUserInWaveArea returns true when position is within area`() = runTest {
        // Given
        val userPosition = Position(40.7128, -74.0060)
        val mockArea = mockk<WWWEventArea>()

        coEvery { mockArea.getPolygons() } returns listOf(mockk()) // Non-empty list
        coEvery { mockArea.isPositionWithin(userPosition) } returns true

        // When
        val result = tracker.isUserInWaveArea(userPosition, mockArea)

        // Then
        assertTrue(result)
    }

    @Test
    fun `isUserInWaveArea returns false when position is outside area`() = runTest {
        // Given
        val userPosition = Position(40.7128, -74.0060)
        val mockArea = mockk<WWWEventArea>()

        coEvery { mockArea.getPolygons() } returns listOf(mockk()) // Non-empty list
        coEvery { mockArea.isPositionWithin(userPosition) } returns false

        // When
        val result = tracker.isUserInWaveArea(userPosition, mockArea)

        // Then
        assertFalse(result)
    }

    @Test
    fun `isUserInWaveArea returns false on exception for safety`() = runTest {
        // Given
        val userPosition = Position(40.7128, -74.0060)
        val mockArea = mockk<WWWEventArea>()

        coEvery { mockArea.getPolygons() } throws RuntimeException("Test exception")

        // When
        val result = tracker.isUserInWaveArea(userPosition, mockArea)

        // Then
        assertFalse(result)
    }

    @Test
    fun `recordProgressionSnapshot adds to history`() = runTest {
        // Given
        val startTime = Instant.fromEpochSeconds(1000)
        val currentTime = Instant.fromEpochSeconds(1150) // 150 seconds later
        val duration = 600.seconds
        val userPosition = Position(40.7128, -74.0060)

        val mockEvent = mockk<IWWWEvent>()
        val mockWave = mockk<WWWEventWave>()
        val mockArea = mockk<WWWEventArea>()

        every { mockEvent.isDone() } returns false
        every { mockEvent.isRunning() } returns true
        every { mockEvent.getWaveStartDateTime() } returns startTime
        every { mockEvent.wave } returns mockWave
        every { mockEvent.area } returns mockArea
        every { mockWave.getWaveDuration() } returns duration
        every { mockClock.now() } returns currentTime
        coEvery { mockArea.getPolygons() } returns listOf(mockk())
        coEvery { mockArea.isPositionWithin(userPosition) } returns true

        // When
        tracker.recordProgressionSnapshot(mockEvent, userPosition)

        // Then
        val history = tracker.getProgressionHistory()
        assertEquals(1, history.size)
        assertEquals(25.0, history.first().progression, 0.1) // 150/600 = 25%
        assertEquals(userPosition, history.first().userPosition)
        assertTrue(history.first().isInWaveArea)
    }

    @Test
    fun `recordProgressionSnapshot handles null position`() = runTest {
        // Given
        val mockEvent = mockk<IWWWEvent>()
        every { mockEvent.isDone() } returns false
        every { mockEvent.isRunning() } returns false

        // When
        tracker.recordProgressionSnapshot(mockEvent, null)

        // Then
        val history = tracker.getProgressionHistory()
        assertEquals(1, history.size)
        assertEquals(null, history.first().userPosition)
        assertFalse(history.first().isInWaveArea)
    }

    @Test
    fun `progression history is limited to maxHistorySize`() = runTest {
        // Given
        val mockEvent = mockk<IWWWEvent>()
        every { mockEvent.isDone() } returns false
        every { mockEvent.isRunning() } returns false

        // When - Record more than max size
        repeat(150) {
            tracker.recordProgressionSnapshot(mockEvent, null)
        }

        // Then
        val history = tracker.getProgressionHistory()
        assertEquals(100, history.size) // Should be limited to maxHistorySize
    }

    @Test
    fun `clearProgressionHistory empties the history`() = runTest {
        // Given
        val mockEvent = mockk<IWWWEvent>()
        every { mockEvent.isDone() } returns false
        every { mockEvent.isRunning() } returns false

        tracker.recordProgressionSnapshot(mockEvent, null)
        assertEquals(1, tracker.getProgressionHistory().size)

        // When
        tracker.clearProgressionHistory()

        // Then
        assertEquals(0, tracker.getProgressionHistory().size)
    }

    @Test
    fun `getProgressionHistory returns defensive copy`() = runTest {
        // Given
        val mockEvent = mockk<IWWWEvent>()
        every { mockEvent.isDone() } returns false
        every { mockEvent.isRunning() } returns false

        tracker.recordProgressionSnapshot(mockEvent, null)
        val history1 = tracker.getProgressionHistory()
        val history2 = tracker.getProgressionHistory()

        // Then
        assertTrue(history1 !== history2) // Different instances
        assertEquals(history1.size, history2.size) // Same content
    }

    private suspend fun runTest(block: suspend () -> Unit) {
        block()
    }
}