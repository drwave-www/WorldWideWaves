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

import com.worldwidewaves.shared.events.IWWWEvent
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FilterEventsUseCaseTest {
    private val mockMapChecker = mockk<MapAvailabilityChecker>()
    private val useCase = FilterEventsUseCase(mockMapChecker)

    private fun createMockEvent(
        id: String,
        favorite: Boolean = false,
        isRunning: Boolean = false,
        isDone: Boolean = false,
    ): IWWWEvent =
        mockk<IWWWEvent>().apply {
            every { this@apply.id } returns id
            every { this@apply.favorite } returns favorite
            coEvery { isRunning() } returns isRunning
            coEvery { isDone() } returns isDone
        }

    @Test
    fun `filter returns all events when no criteria specified`() =
        runTest {
            // Given
            every { mockMapChecker.refreshAvailability() } returns Unit
            val events =
                listOf(
                    createMockEvent("event1", favorite = true),
                    createMockEvent("event2", favorite = false),
                    createMockEvent("event3", favorite = true),
                )

            // When
            val result = useCase.invoke(events, EventFilterCriteria())

            // Then
            assertEquals(3, result.size)
            verify { mockMapChecker.refreshAvailability() }
        }

    @Test
    fun `filter returns only favorite events when onlyFavorites is true`() =
        runTest {
            // Given
            every { mockMapChecker.refreshAvailability() } returns Unit
            val events =
                listOf(
                    createMockEvent("event1", favorite = true),
                    createMockEvent("event2", favorite = false),
                    createMockEvent("event3", favorite = true),
                )

            val criteria = EventFilterCriteria(onlyFavorites = true)

            // When
            val result = useCase.invoke(events, criteria)

            // Then
            assertEquals(2, result.size)
            assertEquals("event1", result[0].id)
            assertEquals("event3", result[1].id)
        }

    @Test
    fun `filter returns only events with downloaded maps when onlyDownloaded is true`() =
        runTest {
            // Given
            every { mockMapChecker.refreshAvailability() } returns Unit
            every { mockMapChecker.isMapDownloaded("event1") } returns true
            every { mockMapChecker.isMapDownloaded("event2") } returns false
            every { mockMapChecker.isMapDownloaded("event3") } returns true

            val events =
                listOf(
                    createMockEvent("event1"),
                    createMockEvent("event2"),
                    createMockEvent("event3"),
                )

            val criteria = EventFilterCriteria(onlyDownloaded = true)

            // When
            val result = useCase.invoke(events, criteria)

            // Then
            assertEquals(2, result.size)
            assertEquals("event1", result[0].id)
            assertEquals("event3", result[1].id)
        }

    @Test
    fun `filter returns only running events when onlyRunning is true`() =
        runTest {
            // Given
            every { mockMapChecker.refreshAvailability() } returns Unit
            val events =
                listOf(
                    createMockEvent("event1", isRunning = true),
                    createMockEvent("event2", isRunning = false),
                    createMockEvent("event3", isRunning = true),
                )

            val criteria = EventFilterCriteria(onlyRunning = true)

            // When
            val result = useCase.invoke(events, criteria)

            // Then
            assertEquals(2, result.size)
            assertEquals("event1", result[0].id)
            assertEquals("event3", result[1].id)
        }

    @Test
    fun `filter returns only upcoming events when onlyUpcoming is true`() =
        runTest {
            // Given
            every { mockMapChecker.refreshAvailability() } returns Unit
            val events =
                listOf(
                    createMockEvent("event1", isRunning = false, isDone = false), // upcoming
                    createMockEvent("event2", isRunning = true, isDone = false), // running
                    createMockEvent("event3", isRunning = false, isDone = true), // completed
                    createMockEvent("event4", isRunning = false, isDone = false), // upcoming
                )

            val criteria = EventFilterCriteria(onlyUpcoming = true)

            // When
            val result = useCase.invoke(events, criteria)

            // Then
            assertEquals(2, result.size)
            assertEquals("event1", result[0].id)
            assertEquals("event4", result[1].id)
        }

    @Test
    fun `filter returns only completed events when onlyCompleted is true`() =
        runTest {
            // Given
            every { mockMapChecker.refreshAvailability() } returns Unit
            val events =
                listOf(
                    createMockEvent("event1", isRunning = false, isDone = false), // upcoming
                    createMockEvent("event2", isRunning = true, isDone = false), // running
                    createMockEvent("event3", isRunning = false, isDone = true), // completed
                    createMockEvent("event4", isRunning = false, isDone = true), // completed
                )

            val criteria = EventFilterCriteria(onlyCompleted = true)

            // When
            val result = useCase.invoke(events, criteria)

            // Then
            assertEquals(2, result.size)
            assertEquals("event3", result[0].id)
            assertEquals("event4", result[1].id)
        }

    @Test
    fun `filter returns empty list when no events match criteria`() =
        runTest {
            // Given
            every { mockMapChecker.refreshAvailability() } returns Unit
            val events =
                listOf(
                    createMockEvent("event1", favorite = false),
                    createMockEvent("event2", favorite = false),
                )

            val criteria = EventFilterCriteria(onlyFavorites = true)

            // When
            val result = useCase.invoke(events, criteria)

            // Then
            assertTrue(result.isEmpty())
        }

    @Test
    fun `filter handles empty event list`() =
        runTest {
            // Given
            every { mockMapChecker.refreshAvailability() } returns Unit
            val events = emptyList<IWWWEvent>()
            val criteria = EventFilterCriteria(onlyFavorites = true)

            // When
            val result = useCase.invoke(events, criteria)

            // Then
            assertTrue(result.isEmpty())
        }

    @Test
    fun `convenience filter method works with boolean flags`() =
        runTest {
            // Given
            every { mockMapChecker.refreshAvailability() } returns Unit
            every { mockMapChecker.isMapDownloaded("event1") } returns true
            every { mockMapChecker.isMapDownloaded("event2") } returns false

            val events =
                listOf(
                    createMockEvent("event1", favorite = true),
                    createMockEvent("event2", favorite = false),
                )

            // When
            val favoriteResult = useCase.filter(events, onlyFavorites = true)
            val downloadedResult = useCase.filter(events, onlyDownloaded = true)
            val allResult = useCase.filter(events)

            // Then
            assertEquals(1, favoriteResult.size)
            assertEquals("event1", favoriteResult[0].id)

            assertEquals(1, downloadedResult.size)
            assertEquals("event1", downloadedResult[0].id)

            assertEquals(2, allResult.size)
        }

    @Test
    fun `filter combines multiple criteria correctly`() =
        runTest {
            // Given
            every { mockMapChecker.refreshAvailability() } returns Unit
            val events =
                listOf(
                    createMockEvent("event1", favorite = true, isRunning = true), // favorite + running
                    createMockEvent("event2", favorite = false, isRunning = true), // running only
                    createMockEvent("event3", favorite = true, isRunning = false), // favorite only
                )

            // When: Multiple criteria should be OR-ed (first matching criteria wins)
            val runningCriteria = EventFilterCriteria(onlyRunning = true)
            val favoriteCriteria = EventFilterCriteria(onlyFavorites = true)

            val runningResult = useCase.invoke(events, runningCriteria)
            val favoriteResult = useCase.invoke(events, favoriteCriteria)

            // Then
            assertEquals(2, runningResult.size) // event1 and event2 are running
            assertEquals(2, favoriteResult.size) // event1 and event3 are favorites
        }
}
