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
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for EventsRepository interface contract.
 *
 * These tests verify that the repository interface is correctly defined
 * and that any implementation must satisfy the expected contract.
 */
class EventsRepositoryTest {

    /**
     * Mock implementation for testing interface contract
     */
    private class TestEventsRepository : EventsRepository {
        private var events = emptyList<IWWWEvent>()
        private var loadingState = false
        private var lastError: Exception? = null

        fun setMockEvents(events: List<IWWWEvent>) {
            this.events = events
        }

        fun setMockError(error: Exception?) {
            this.lastError = error
        }

        override suspend fun getEvents(): Flow<List<IWWWEvent>> = flowOf(events)

        override suspend fun loadEvents(onLoadingError: (Exception) -> Unit) {
            lastError?.let { onLoadingError(it) }
        }

        override suspend fun getEvent(eventId: String): Flow<IWWWEvent?> =
            flowOf(events.find { it.id == eventId })

        override suspend fun refreshEvents(): Result<Unit> =
            if (lastError != null) Result.failure(lastError!!) else Result.success(Unit)

        override suspend fun getCachedEventsCount(): Int = events.size

        override suspend fun clearCache() {
            events = emptyList()
        }

        override fun isLoading(): Flow<Boolean> = flowOf(loadingState)

        override fun getLastError(): Flow<Exception?> = flowOf(lastError)
    }

    @Test
    fun `EventsRepository interface should have all required methods`() {
        // Test that we can instantiate the interface through implementation
        val repository: EventsRepository = TestEventsRepository()
        assertNotNull(repository)

        // Verify all methods are available
        val methods = EventsRepository::class.members

        val methodNames = methods.map { it.name }.toSet()
        assertTrue(methodNames.contains("getEvents"))
        assertTrue(methodNames.contains("loadEvents"))
        assertTrue(methodNames.contains("getEvent"))
        assertTrue(methodNames.contains("refreshEvents"))
        assertTrue(methodNames.contains("getCachedEventsCount"))
        assertTrue(methodNames.contains("clearCache"))
        assertTrue(methodNames.contains("isLoading"))
        assertTrue(methodNames.contains("getLastError"))
    }

    @Test
    fun `repository implementation should handle empty events list`() = runTest {
        // Arrange
        val repository = TestEventsRepository()
        repository.setMockEvents(emptyList())

        // Act
        val events = repository.getEvents()
        val count = repository.getCachedEventsCount()

        // Assert
        assertNotNull(events)
        assertEquals(0, count)
    }

    @Test
    fun `repository implementation should handle events list with data`() = runTest {
        // Arrange
        val mockEvents = listOf<IWWWEvent>(mockk(), mockk(), mockk())
        val repository = TestEventsRepository()
        repository.setMockEvents(mockEvents)

        // Act
        val count = repository.getCachedEventsCount()

        // Assert
        assertEquals(3, count)
    }

    @Test
    fun `repository should handle error states`() = runTest {
        // Arrange
        val testError = RuntimeException("Test error")
        val repository = TestEventsRepository()
        repository.setMockError(testError)

        // Act
        val refreshResult = repository.refreshEvents()
        var errorReceived: Exception? = null
        repository.loadEvents { error ->
            errorReceived = error
        }

        // Assert
        assertTrue(refreshResult.isFailure)
        assertEquals("Test error", refreshResult.exceptionOrNull()?.message)
        assertNotNull(errorReceived)
        assertEquals("Test error", errorReceived?.message)
    }

    @Test
    fun `repository should support cache operations`() = runTest {
        // Arrange
        val mockEvents = listOf<IWWWEvent>(mockk())
        val repository = TestEventsRepository()
        repository.setMockEvents(mockEvents)

        // Act
        val initialCount = repository.getCachedEventsCount()
        repository.clearCache()
        val clearedCount = repository.getCachedEventsCount()

        // Assert
        assertEquals(1, initialCount)
        assertEquals(0, clearedCount)
    }
}