package com.worldwidewaves.shared.domain.repository

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
import com.worldwidewaves.shared.events.WWWEvents
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * Tests for EventsRepositoryImpl cleanup functionality to prevent memory leaks.
 */
class EventsRepositoryCleanupTest {
    @Test
    fun `should cancel background scope on cleanup`() =
        runTest {
            // Given
            val eventsFlow = MutableStateFlow<List<IWWWEvent>>(emptyList())
            val wwwEvents = mockk<WWWEvents>(relaxed = true)
            every { wwwEvents.flow() } returns eventsFlow
            coEvery { wwwEvents.list() } returns emptyList()

            val repository = EventsRepositoryImpl(wwwEvents)

            // When
            repository.cleanup()

            // Then - Verify scope is cancelled by attempting to access loading state
            // After cleanup, the repository should not be able to perform background operations
            val loadingState = repository.isLoading().first()
            assertFalse(loadingState, "Loading state should be false after cleanup")
        }

    @Test
    fun `should clear cache on cleanup`() =
        runTest {
            // Given
            val mockEvents = listOf(mockk<IWWWEvent>(relaxed = true), mockk<IWWWEvent>(relaxed = true))
            val eventsFlow = MutableStateFlow<List<IWWWEvent>>(mockEvents)
            val wwwEvents = mockk<WWWEvents>(relaxed = true)
            every { wwwEvents.flow() } returns eventsFlow
            coEvery { wwwEvents.list() } returns mockEvents

            val repository = EventsRepositoryImpl(wwwEvents)

            // Load some events to populate the cache
            repository.loadEvents(onLoadingError = {})

            // When
            repository.cleanup()

            // Then - Verify cache is cleared
            // Note: getCachedEventsCount() returns wwwEvents.list().size when cache is invalid
            // After cleanup, cache is invalidated but wwwEvents still has events
            // To verify cache is truly cleared, we need to mock wwwEvents.list() to return empty
            coEvery { wwwEvents.list() } returns emptyList()
            val cachedCount = repository.getCachedEventsCount()
            assertEquals(0, cachedCount, "Cache should be empty after cleanup")
        }

    @Test
    fun `should handle multiple cleanup calls gracefully`() =
        runTest {
            // Given
            val eventsFlow = MutableStateFlow<List<IWWWEvent>>(emptyList())
            val wwwEvents = mockk<WWWEvents>(relaxed = true)
            every { wwwEvents.flow() } returns eventsFlow
            coEvery { wwwEvents.list() } returns emptyList()

            val repository = EventsRepositoryImpl(wwwEvents)

            // When - Call cleanup multiple times
            repository.cleanup()
            repository.cleanup()
            repository.cleanup()

            // Then - Should not throw exception
            val cachedCount = repository.getCachedEventsCount()
            assertEquals(0, cachedCount, "Cache should remain empty after multiple cleanups")
        }

    @Test
    fun `should clear cache after loading events`() =
        runTest {
            // Given
            val mockEvents =
                listOf(
                    mockk<IWWWEvent>(relaxed = true),
                    mockk<IWWWEvent>(relaxed = true),
                    mockk<IWWWEvent>(relaxed = true),
                )
            val eventsFlow = MutableStateFlow<List<IWWWEvent>>(mockEvents)
            val wwwEvents = mockk<WWWEvents>(relaxed = true)
            every { wwwEvents.flow() } returns eventsFlow

            // Use a mutable list that we can change later
            val eventsList = mutableListOf<IWWWEvent>()
            eventsList.addAll(mockEvents)
            coEvery { wwwEvents.list() } answers { eventsList.toList() }

            val repository = EventsRepositoryImpl(wwwEvents)

            // Simulate loading events
            every { wwwEvents.loadEvents(any(), any(), any()) } answers {
                val onLoaded = firstArg<() -> Unit>()
                onLoaded()
                wwwEvents
            }

            // Load events to populate cache
            repository.loadEvents(onLoadingError = {})

            // When - Cleanup after loading
            repository.cleanup()

            // Then - Cache should be cleared
            // Note: getCachedEventsCount() returns wwwEvents.list().size when cache is invalid
            // After cleanup, cache is invalidated but wwwEvents still has events
            // To verify cache is truly cleared, we clear the backing list and check count
            eventsList.clear()
            val cachedCount = repository.getCachedEventsCount()
            assertEquals(0, cachedCount, "Cache should be cleared after cleanup even after loading events")
        }
}
