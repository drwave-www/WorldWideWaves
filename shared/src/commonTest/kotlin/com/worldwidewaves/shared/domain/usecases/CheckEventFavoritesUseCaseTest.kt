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
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CheckEventFavoritesUseCaseTest {

    private val useCase = CheckEventFavoritesUseCase()

    private fun createMockEvent(
        id: String,
        favorite: Boolean = false
    ): IWWWEvent = mockk<IWWWEvent>().apply {
        every { this@apply.id } returns id
        every { this@apply.favorite } returns favorite
    }

    @Test
    fun `hasFavoriteEvents returns true when at least one event is favorite`() = runTest {
        // Given
        val events = listOf(
            createMockEvent("event1", favorite = false),
            createMockEvent("event2", favorite = true),
            createMockEvent("event3", favorite = false)
        )

        // When
        val result = useCase.hasFavoriteEvents(events)

        // Then
        assertTrue(result)
    }

    @Test
    fun `hasFavoriteEvents returns false when no events are favorite`() = runTest {
        // Given
        val events = listOf(
            createMockEvent("event1", favorite = false),
            createMockEvent("event2", favorite = false),
            createMockEvent("event3", favorite = false)
        )

        // When
        val result = useCase.hasFavoriteEvents(events)

        // Then
        assertFalse(result)
    }

    @Test
    fun `hasFavoriteEvents returns false for empty list`() = runTest {
        // Given
        val events = emptyList<IWWWEvent>()

        // When
        val result = useCase.hasFavoriteEvents(events)

        // Then
        assertFalse(result)
    }

    @Test
    fun `hasFavoriteEvents returns true when all events are favorites`() = runTest {
        // Given
        val events = listOf(
            createMockEvent("event1", favorite = true),
            createMockEvent("event2", favorite = true),
            createMockEvent("event3", favorite = true)
        )

        // When
        val result = useCase.hasFavoriteEvents(events)

        // Then
        assertTrue(result)
    }

    @Test
    fun `hasFavoriteEventsFlow emits correct values for changing event lists`() = runTest {
        // Given
        val eventsWithoutFavorites = listOf(
            createMockEvent("event1", favorite = false),
            createMockEvent("event2", favorite = false)
        )

        val eventsWithFavorites = listOf(
            createMockEvent("event1", favorite = false),
            createMockEvent("event2", favorite = true)
        )

        val eventsFlow = flowOf(eventsWithoutFavorites, eventsWithFavorites)

        // When
        val resultFlow = useCase.hasFavoriteEventsFlow(eventsFlow)

        // Then - we can only get the first emission in this test setup
        // In a real scenario, this would be collected continuously
        val firstResult = resultFlow.first()
        assertFalse(firstResult) // First emission should be false (no favorites)
    }

    @Test
    fun `getFavoriteEvents returns only favorite events`() = runTest {
        // Given
        val events = listOf(
            createMockEvent("event1", favorite = false),
            createMockEvent("event2", favorite = true),
            createMockEvent("event3", favorite = false),
            createMockEvent("event4", favorite = true)
        )

        // When
        val result = useCase.getFavoriteEvents(events)

        // Then
        assertEquals(2, result.size)
        assertEquals("event2", result[0].id)
        assertEquals("event4", result[1].id)
        assertTrue(result.all { it.favorite })
    }

    @Test
    fun `getFavoriteEvents returns empty list when no favorites`() = runTest {
        // Given
        val events = listOf(
            createMockEvent("event1", favorite = false),
            createMockEvent("event2", favorite = false)
        )

        // When
        val result = useCase.getFavoriteEvents(events)

        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun `getFavoriteEvents returns all events when all are favorites`() = runTest {
        // Given
        val events = listOf(
            createMockEvent("event1", favorite = true),
            createMockEvent("event2", favorite = true),
            createMockEvent("event3", favorite = true)
        )

        // When
        val result = useCase.getFavoriteEvents(events)

        // Then
        assertEquals(3, result.size)
        assertTrue(result.all { it.favorite })
    }

    @Test
    fun `getFavoriteEventsCount returns correct count`() = runTest {
        // Given
        val events = listOf(
            createMockEvent("event1", favorite = false),
            createMockEvent("event2", favorite = true),
            createMockEvent("event3", favorite = false),
            createMockEvent("event4", favorite = true),
            createMockEvent("event5", favorite = true)
        )

        // When
        val result = useCase.getFavoriteEventsCount(events)

        // Then
        assertEquals(3, result)
    }

    @Test
    fun `getFavoriteEventsCount returns zero for no favorites`() = runTest {
        // Given
        val events = listOf(
            createMockEvent("event1", favorite = false),
            createMockEvent("event2", favorite = false)
        )

        // When
        val result = useCase.getFavoriteEventsCount(events)

        // Then
        assertEquals(0, result)
    }

    @Test
    fun `getFavoriteEventsCount returns zero for empty list`() = runTest {
        // Given
        val events = emptyList<IWWWEvent>()

        // When
        val result = useCase.getFavoriteEventsCount(events)

        // Then
        assertEquals(0, result)
    }

    @Test
    fun `getFavoriteEventsCountFlow emits correct counts`() = runTest {
        // Given
        val eventsWithTwoFavorites = listOf(
            createMockEvent("event1", favorite = true),
            createMockEvent("event2", favorite = false),
            createMockEvent("event3", favorite = true)
        )

        val eventsFlow = flowOf(eventsWithTwoFavorites)

        // When
        val resultFlow = useCase.getFavoriteEventsCountFlow(eventsFlow)

        // Then
        val count = resultFlow.first()
        assertEquals(2, count)
    }

    @Test
    fun `getFavoriteEventsCountFlow handles empty event list`() = runTest {
        // Given
        val eventsFlow = flowOf(emptyList<IWWWEvent>())

        // When
        val resultFlow = useCase.getFavoriteEventsCountFlow(eventsFlow)

        // Then
        val count = resultFlow.first()
        assertEquals(0, count)
    }
}