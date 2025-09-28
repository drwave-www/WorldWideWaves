package com.worldwidewaves.shared.domain.usecases

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

import com.worldwidewaves.shared.domain.repository.EventsRepository
import com.worldwidewaves.shared.events.IWWWEvent
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
class GetSortedEventsUseCaseTest {
    private val mockRepository = mockk<EventsRepository>()
    private val useCase = GetSortedEventsUseCase(mockRepository)

    private fun createMockEvent(
        id: String,
        startTime: Instant,
    ): IWWWEvent =
        mockk<IWWWEvent>().apply {
            coEvery { this@apply.id } returns id
            coEvery { getStartDateTime() } returns startTime
        }

    @Test
    fun `invoke returns events sorted by start date ascending`() =
        runTest {
            // Given: Events with different start times
            val laterTime = Instant.fromEpochSeconds(1000)
            val earlierTime = Instant.fromEpochSeconds(500)
            val middleTime = Instant.fromEpochSeconds(750)

            val event1 = createMockEvent("event1", laterTime)
            val event2 = createMockEvent("event2", earlierTime)
            val event3 = createMockEvent("event3", middleTime)

            val unsortedEvents = listOf(event1, event2, event3)
            coEvery { mockRepository.getEvents() } returns flowOf(unsortedEvents)

            // When: Invoking the use case
            val result = useCase.invoke().first()

            // Then: Events should be sorted by start time (earliest first)
            assertEquals(3, result.size)
            assertEquals("event2", result[0].id) // earlierTime
            assertEquals("event3", result[1].id) // middleTime
            assertEquals("event1", result[2].id) // laterTime
        }

    @Test
    fun `invoke returns empty list when no events available`() =
        runTest {
            // Given: Empty events list
            coEvery { mockRepository.getEvents() } returns flowOf(emptyList())

            // When: Invoking the use case
            val result = useCase.invoke().first()

            // Then: Should return empty list
            assertTrue(result.isEmpty())
        }

    @Test
    fun `invoke returns single event when only one event available`() =
        runTest {
            // Given: Single event
            val event = createMockEvent("single_event", Instant.fromEpochSeconds(1000))
            coEvery { mockRepository.getEvents() } returns flowOf(listOf(event))

            // When: Invoking the use case
            val result = useCase.invoke().first()

            // Then: Should return the single event
            assertEquals(1, result.size)
            assertEquals("single_event", result[0].id)
        }

    @Test
    fun `invoke handles events with same start time`() =
        runTest {
            // Given: Events with identical start times
            val sameTime = Instant.fromEpochSeconds(1000)
            val event1 = createMockEvent("event1", sameTime)
            val event2 = createMockEvent("event2", sameTime)
            val event3 = createMockEvent("event3", sameTime)

            val events = listOf(event1, event2, event3)
            coEvery { mockRepository.getEvents() } returns flowOf(events)

            // When: Invoking the use case
            val result = useCase.invoke().first()

            // Then: Should return all events (order may vary for same timestamp)
            assertEquals(3, result.size)
            assertTrue(result.all { it.getStartDateTime() == sameTime })
        }

    @Test
    fun `invoke with limit returns limited number of events`() =
        runTest {
            // Given: Multiple events
            val events =
                listOf(
                    createMockEvent("event1", Instant.fromEpochSeconds(100)),
                    createMockEvent("event2", Instant.fromEpochSeconds(200)),
                    createMockEvent("event3", Instant.fromEpochSeconds(300)),
                    createMockEvent("event4", Instant.fromEpochSeconds(400)),
                )
            coEvery { mockRepository.getEvents() } returns flowOf(events)

            // When: Invoking with limit of 2
            val result = useCase.invoke(limit = 2).first()

            // Then: Should return only first 2 events (sorted)
            assertEquals(2, result.size)
            assertEquals("event1", result[0].id)
            assertEquals("event2", result[1].id)
        }

    @Test
    fun `invoke with null limit returns all events`() =
        runTest {
            // Given: Multiple events
            val events =
                listOf(
                    createMockEvent("event1", Instant.fromEpochSeconds(100)),
                    createMockEvent("event2", Instant.fromEpochSeconds(200)),
                    createMockEvent("event3", Instant.fromEpochSeconds(300)),
                )
            coEvery { mockRepository.getEvents() } returns flowOf(events)

            // When: Invoking with null limit
            val result = useCase.invoke(limit = null).first()

            // Then: Should return all events
            assertEquals(3, result.size)
        }

    @Test
    fun `invoke with zero limit returns all events`() =
        runTest {
            // Given: Multiple events
            val events =
                listOf(
                    createMockEvent("event1", Instant.fromEpochSeconds(100)),
                    createMockEvent("event2", Instant.fromEpochSeconds(200)),
                )
            coEvery { mockRepository.getEvents() } returns flowOf(events)

            // When: Invoking with limit of 0
            val result = useCase.invoke(limit = 0).first()

            // Then: Should return all events (0 is treated as no limit)
            assertEquals(2, result.size)
        }

    @Test
    fun `invoke with negative limit returns all events`() =
        runTest {
            // Given: Multiple events
            val events =
                listOf(
                    createMockEvent("event1", Instant.fromEpochSeconds(100)),
                    createMockEvent("event2", Instant.fromEpochSeconds(200)),
                )
            coEvery { mockRepository.getEvents() } returns flowOf(events)

            // When: Invoking with negative limit
            val result = useCase.invoke(limit = -1).first()

            // Then: Should return all events (negative limit is treated as no limit)
            assertEquals(2, result.size)
        }

    @Test
    fun `invoke maintains chronological order for realistic event times`() =
        runTest {
            // Given: Events with realistic timestamps (representing different dates)
            val event1 = createMockEvent("paris_wave", Instant.parse("2025-10-01T10:00:00Z"))
            val event2 = createMockEvent("london_wave", Instant.parse("2025-09-15T14:30:00Z"))
            val event3 = createMockEvent("tokyo_wave", Instant.parse("2025-11-20T09:00:00Z"))
            val event4 = createMockEvent("sydney_wave", Instant.parse("2025-09-20T16:00:00Z"))

            val events = listOf(event1, event2, event3, event4)
            coEvery { mockRepository.getEvents() } returns flowOf(events)

            // When: Invoking the use case
            val result = useCase.invoke().first()

            // Then: Should be in chronological order
            assertEquals("london_wave", result[0].id) // Sep 15
            assertEquals("sydney_wave", result[1].id) // Sep 20
            assertEquals("paris_wave", result[2].id) // Oct 1
            assertEquals("tokyo_wave", result[3].id) // Nov 20
        }
}
