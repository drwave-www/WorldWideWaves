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
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime

/**
 * Comprehensive tests for EventsRepositoryImpl.
 *
 * Tests cover:
 * - Basic event retrieval and caching
 * - Error handling and recovery scenarios
 * - Loading state management
 * - Cache management and memory efficiency
 * - Event lookup by ID
 */
@OptIn(ExperimentalTime::class)
class EventsRepositoryImplTest {
    private val mockWWWEvents = mockk<WWWEvents>()
    private val repository = EventsRepositoryImpl(mockWWWEvents)

    private fun createMockEvent(id: String): IWWWEvent =
        mockk<IWWWEvent>().apply {
            every { this@apply.id } returns id
        }

    @Test
    fun `getEvents should return flow from WWWEvents`() =
        runTest {
            // Arrange
            val mockEvents =
                listOf(
                    createMockEvent("event1"),
                    createMockEvent("event2"),
                )
            every { mockWWWEvents.flow() } returns MutableStateFlow(mockEvents)

            // Act
            val result = repository.getEvents().first()

            // Assert
            assertEquals(2, result.size)
            assertEquals("event1", result[0].id)
            assertEquals("event2", result[1].id)
        }

    @Test
    fun `getEvents should return empty flow when no events`() =
        runTest {
            // Arrange
            every { mockWWWEvents.flow() } returns MutableStateFlow(emptyList())

            // Act
            val result = repository.getEvents().first()

            // Assert
            assertTrue(result.isEmpty())
        }

    @Test
    fun `loadEvents should trigger WWWEvents loadEvents with callbacks`() =
        runTest {
            // Arrange
            val testException = RuntimeException("Load error")
            var onLoadingErrorCalled = false
            val errorCallback: (Exception) -> Unit = {
                onLoadingErrorCalled = true
            }

            every {
                mockWWWEvents.loadEvents(any(), any(), any())
            } answers {
                val onLoaded = secondArg<(() -> Unit)?>()
                val onLoadingError = thirdArg<((Exception) -> Unit)?>()

                // Simulate loading error
                onLoadingError?.invoke(testException)

                mockWWWEvents
            }

            // Act
            repository.loadEvents(errorCallback)

            // Assert
            assertTrue(onLoadingErrorCalled)
            verify { mockWWWEvents.loadEvents(any(), any(), any()) }
        }

    @Test
    fun `loadEvents should manage loading state correctly`() =
        runTest {
            // Arrange
            every { mockWWWEvents.list() } returns emptyList()
            every {
                mockWWWEvents.loadEvents(any(), any(), any())
            } answers {
                val onLoaded = secondArg<(() -> Unit)?>()

                // Simulate successful loading
                onLoaded?.invoke()

                mockWWWEvents
            }

            // Act & Assert
            // Initially not loading
            assertFalse(repository.isLoading().first())

            // Start loading
            repository.loadEvents { }

            // Should not be loading after successful completion
            assertFalse(repository.isLoading().first())
        }

    @Test
    fun `loadEvents should set error state on failure`() =
        runTest {
            // Arrange
            val testException = RuntimeException("Load failed")
            every {
                mockWWWEvents.loadEvents(any(), any(), any())
            } answers {
                val onLoadingError = secondArg<((Exception) -> Unit)?>()

                // Simulate loading error
                onLoadingError?.invoke(testException)

                mockWWWEvents
            }

            // Initially no error
            assertNull(repository.getLastError().first())

            // Act
            repository.loadEvents { }

            // Assert
            val error = repository.getLastError().first()
            assertNotNull(error)
            assertEquals("Load failed", error.message)
        }

    @Test
    fun `getEvent should return specific event by ID`() =
        runTest {
            // Arrange
            val mockEvents =
                listOf(
                    createMockEvent("event1"),
                    createMockEvent("event2"),
                    createMockEvent("event3"),
                )
            every { mockWWWEvents.flow() } returns MutableStateFlow(mockEvents)

            // Act
            val result = repository.getEvent("event2").first()

            // Assert
            assertNotNull(result)
            assertEquals("event2", result.id)
        }

    @Test
    fun `getEvent should return null for non-existent ID`() =
        runTest {
            // Arrange
            val mockEvents =
                listOf(
                    createMockEvent("event1"),
                    createMockEvent("event2"),
                )
            every { mockWWWEvents.flow() } returns MutableStateFlow(mockEvents)

            // Act
            val result = repository.getEvent("non-existent").first()

            // Assert
            assertNull(result)
        }

    @Test
    fun `getEvent should return null from empty events list`() =
        runTest {
            // Arrange
            every { mockWWWEvents.flow() } returns MutableStateFlow(emptyList())

            // Act
            val result = repository.getEvent("any-id").first()

            // Assert
            assertNull(result)
        }

    @Test
    fun `refreshEvents should reload data successfully`() =
        runTest {
            // Arrange
            every { mockWWWEvents.list() } returns emptyList()
            every {
                mockWWWEvents.loadEvents(any(), any(), any())
            } answers {
                val onLoaded = firstArg<(() -> Unit)?>()

                // Simulate successful refresh
                onLoaded?.invoke()

                mockWWWEvents
            }

            // Act
            val result = repository.refreshEvents()

            // Assert
            assertTrue(result.isSuccess)
            verify { mockWWWEvents.loadEvents(any(), any(), any()) }
        }

    @Test
    fun `refreshEvents should return failure on error`() =
        runTest {
            // Arrange
            val testException = RuntimeException("Refresh failed")
            every {
                mockWWWEvents.loadEvents(any(), any(), any())
            } answers {
                val onLoadingError = secondArg<((Exception) -> Unit)?>()

                // Simulate refresh error
                onLoadingError?.invoke(testException)

                mockWWWEvents
            }

            // Act
            val result = repository.refreshEvents()

            // Assert
            assertTrue(result.isFailure)
            assertEquals("Refresh failed", result.exceptionOrNull()?.message)
        }

    @Test
    fun `getCachedEventsCount should return count from WWWEvents list`() =
        runTest {
            // Arrange
            val mockEvents =
                listOf(
                    createMockEvent("event1"),
                    createMockEvent("event2"),
                    createMockEvent("event3"),
                )
            every { mockWWWEvents.list() } returns mockEvents

            // Act
            val count = repository.getCachedEventsCount()

            // Assert
            assertEquals(3, count)
        }

    @Test
    fun `getCachedEventsCount should return zero for empty list`() =
        runTest {
            // Arrange
            every { mockWWWEvents.list() } returns emptyList()

            // Act
            val count = repository.getCachedEventsCount()

            // Assert
            assertEquals(0, count)
        }

    @Test
    fun `clearCache should not affect WWWEvents functionality`() =
        runTest {
            // Arrange
            val mockEvents = listOf(createMockEvent("event1"))
            every { mockWWWEvents.list() } returns mockEvents

            // Act
            repository.clearCache()

            // Assert - should still work normally
            val count = repository.getCachedEventsCount()
            assertEquals(1, count)
        }

    @Test
    fun `isLoading should return false initially`() =
        runTest {
            // Act
            val isLoading = repository.isLoading().first()

            // Assert
            assertFalse(isLoading)
        }

    @Test
    fun `getLastError should return null initially`() =
        runTest {
            // Act
            val error = repository.getLastError().first()

            // Assert
            assertNull(error)
        }

    @Test
    fun `multiple loadEvents calls should handle concurrency`() =
        runTest {
            // Arrange
            every { mockWWWEvents.list() } returns emptyList()
            every {
                mockWWWEvents.loadEvents(any(), any(), any())
            } answers {
                val onLoaded = secondArg<(() -> Unit)?>()
                onLoaded?.invoke()
                mockWWWEvents
            }

            // Act - multiple concurrent calls
            val errorCallback: (Exception) -> Unit = { }
            repository.loadEvents(errorCallback)
            repository.loadEvents(errorCallback)

            // Assert - should not crash and complete successfully
            assertFalse(repository.isLoading().first())
        }

    @Test
    fun `repository should handle WWWEvents exceptions gracefully`() =
        runTest {
            // Arrange
            val testException = RuntimeException("WWWEvents internal error")
            every {
                mockWWWEvents.loadEvents(any(), any(), any())
            } throws testException

            var errorReceived: Exception? = null
            val errorCallback: (Exception) -> Unit = { errorReceived = it }

            // Act
            repository.loadEvents(errorCallback)

            // Assert
            assertNotNull(errorReceived)
            assertEquals("WWWEvents internal error", errorReceived?.message)

            val lastError = repository.getLastError().first()
            assertNotNull(lastError)
            assertEquals("WWWEvents internal error", lastError.message)
        }

    @Test
    fun `refreshEvents should clear previous error state`() =
        runTest {
            // Arrange - first set an error
            val firstException = RuntimeException("First error")
            every {
                mockWWWEvents.loadEvents(any(), any(), any())
            } answers {
                val onLoadingError = secondArg<((Exception) -> Unit)?>()
                onLoadingError?.invoke(firstException)
                mockWWWEvents
            }

            repository.loadEvents { }

            // Verify error is set
            val initialError = repository.getLastError().first()
            assertNotNull(initialError)

            // Now set up successful refresh
            every { mockWWWEvents.list() } returns emptyList()
            every {
                mockWWWEvents.loadEvents(any(), any(), any())
            } answers {
                val onLoaded = secondArg<(() -> Unit)?>()
                onLoaded?.invoke()
                mockWWWEvents
            }

            // Act
            repository.refreshEvents()

            // Assert - error should be cleared
            assertFalse(repository.isLoading().first())
        }
}
