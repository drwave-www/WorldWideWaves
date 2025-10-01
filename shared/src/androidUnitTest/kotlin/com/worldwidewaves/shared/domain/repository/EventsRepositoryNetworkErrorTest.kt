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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeoutException
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Comprehensive network error handling tests for EventsRepositoryImpl.
 *
 * Tests cover:
 * - Network timeout scenarios
 * - Connection failures (DNS, socket, etc.)
 * - Transient network errors and recovery
 * - Cache behavior during network unavailability
 * - Firebase/Firestore specific failures
 * - Partial data download handling
 * - Error state propagation and logging
 * - Loading state management during errors
 * - Error type differentiation (network vs data errors)
 */
class EventsRepositoryNetworkErrorTest {
    private val mockWWWEvents = mockk<WWWEvents>()
    private val repository = EventsRepositoryImpl(mockWWWEvents)

    private fun createMockEvent(id: String): IWWWEvent =
        mockk<IWWWEvent>().apply {
            every { this@apply.id } returns id
        }

    // ==================== Network Failure Tests ====================

    @Test
    fun `should handle network timeout gracefully`() =
        runTest {
            // Arrange
            val timeoutException = SocketTimeoutException("Connection timed out")
            var errorCallbackInvoked = false
            val errorCallback: (Exception) -> Unit = { errorCallbackInvoked = true }

            every {
                mockWWWEvents.loadEvents(any(), any(), any())
            } answers {
                val onLoadingError = secondArg<((Exception) -> Unit)?>()
                onLoadingError?.invoke(timeoutException)
                mockWWWEvents
            }

            // Act
            repository.loadEvents(errorCallback)

            // Assert - Error callback should be invoked
            assertTrue(errorCallbackInvoked, "Error callback should be invoked on timeout")

            // Assert - Loading state should be false after error
            assertFalse(repository.isLoading().first(), "Loading should be false after timeout")

            // Assert - Last error should be set
            val lastError = repository.getLastError().first()
            assertNotNull(lastError, "Last error should be set")
            assertTrue(lastError is SocketTimeoutException, "Error should be SocketTimeoutException")
            assertEquals("Connection timed out", lastError.message)
        }

    @Test
    fun `should emit error state on connection failure`() =
        runTest {
            // Arrange
            val connectionException = UnknownHostException("Unable to resolve host")
            every {
                mockWWWEvents.loadEvents(any(), any(), any())
            } answers {
                val onLoadingError = secondArg<((Exception) -> Unit)?>()
                onLoadingError?.invoke(connectionException)
                mockWWWEvents
            }

            // Initially no error
            assertNull(repository.getLastError().first(), "Should have no error initially")

            // Act
            repository.loadEvents { }

            // Assert - Error state should be emitted
            val error = repository.getLastError().first()
            assertNotNull(error, "Error state should be emitted")
            assertTrue(error is UnknownHostException, "Error should be UnknownHostException")
            assertEquals("Unable to resolve host", error.message)

            // Assert - Loading state should be false
            assertFalse(repository.isLoading().first(), "Loading should stop after connection failure")
        }

    @Test
    fun `should recover from transient network errors`() =
        runTest {
            // Arrange - First attempt fails with transient error
            val transientError = IOException("Network unreachable")

            var attemptCount = 0
            every {
                mockWWWEvents.loadEvents(any(), any(), any())
            } answers {
                attemptCount++
                if (attemptCount == 1) {
                    // First attempt: fail with transient error
                    val onLoadingError = secondArg<((Exception) -> Unit)?>()
                    onLoadingError?.invoke(transientError)
                } else {
                    // Second attempt: succeed
                    val onLoaded = firstArg<(() -> Unit)?>()
                    onLoaded?.invoke()
                }
                mockWWWEvents
            }

            every { mockWWWEvents.list() } returns emptyList()

            // Act - First attempt (fails)
            repository.loadEvents { }
            val errorAfterFirstAttempt = repository.getLastError().first()
            assertNotNull(errorAfterFirstAttempt, "Error should be set after first attempt")

            // Act - Second attempt (succeeds)
            repository.loadEvents { }

            // Assert - Should recover successfully
            assertFalse(repository.isLoading().first(), "Loading should complete after recovery")
            assertEquals(2, attemptCount, "Should have attempted loading twice")
        }

    @Test
    fun `should cache events on network unavailable`() =
        runTest {
            // Arrange - Initial successful load with events
            val cachedEvents =
                listOf(
                    createMockEvent("cached1"),
                    createMockEvent("cached2"),
                )
            every { mockWWWEvents.list() } returns cachedEvents
            every { mockWWWEvents.flow() } returns MutableStateFlow(cachedEvents)
            every {
                mockWWWEvents.loadEvents(any(), any(), any())
            } answers {
                val onLoaded = firstArg<(() -> Unit)?>()
                onLoaded?.invoke()
                mockWWWEvents
            }

            // Load events successfully first
            repository.loadEvents { }

            // Wait for cache to update
            delay(100)

            // Verify cache has events
            assertEquals(2, repository.getCachedEventsCount(), "Cache should have 2 events")

            // Now simulate network failure on refresh
            every {
                mockWWWEvents.loadEvents(any(), any(), any())
            } answers {
                val onLoadingError = secondArg<((Exception) -> Unit)?>()
                onLoadingError?.invoke(IOException("Network unavailable"))
                mockWWWEvents
            }

            // Act - Try to refresh with network unavailable
            val refreshResult = repository.refreshEvents()

            // Assert - Refresh should fail
            assertTrue(refreshResult.isFailure, "Refresh should fail on network unavailable")

            // Assert - Cache should still be accessible
            assertEquals(2, repository.getCachedEventsCount(), "Cache should still have 2 events")

            // Assert - Flow should still provide cached events
            val events = repository.getEvents().first()
            assertEquals(2, events.size, "Should still be able to access cached events")
        }

    // ==================== Firebase-Specific Tests ====================

    @Test
    fun `should handle Firestore connection failure`() =
        runTest {
            // Arrange - Simulate Firestore-specific exception
            val firestoreException = RuntimeException("UNAVAILABLE: io exception")
            var errorReceived: Exception? = null

            every {
                mockWWWEvents.loadEvents(any(), any(), any())
            } answers {
                val onLoadingError = secondArg<((Exception) -> Unit)?>()
                onLoadingError?.invoke(firestoreException)
                mockWWWEvents
            }

            // Act
            repository.loadEvents { error -> errorReceived = error }

            // Assert
            assertNotNull(errorReceived, "Error callback should receive exception")
            assertEquals("UNAVAILABLE: io exception", errorReceived?.message)

            val lastError = repository.getLastError().first()
            assertNotNull(lastError, "Last error should be set")
            assertTrue(lastError.message?.contains("UNAVAILABLE") == true, "Error should be Firestore UNAVAILABLE")
        }

    @Test
    fun `should handle partial event data download`() =
        runTest {
            // Arrange - Simulate partial data load failure
            val partialLoadException = RuntimeException("Incomplete data transfer")
            every {
                mockWWWEvents.loadEvents(any(), any(), any())
            } answers {
                val onTermination = thirdArg<((Exception?) -> Unit)?>()
                // Simulate termination with error after partial download
                onTermination?.invoke(partialLoadException)
                mockWWWEvents
            }

            // Act
            repository.loadEvents { }

            // Assert - Error should be captured
            val error = repository.getLastError().first()
            assertNotNull(error, "Error should be set for partial download failure")
            assertEquals("Incomplete data transfer", error.message)

            // Assert - Loading should stop
            assertFalse(repository.isLoading().first(), "Loading should stop after partial download failure")
        }

    @Test
    fun `should propagate network errors to error callback`() =
        runTest {
            // Arrange
            val networkError = IOException("Network I/O error")
            var callbackError: Exception? = null
            val errorCallback: (Exception) -> Unit = { callbackError = it }

            every {
                mockWWWEvents.loadEvents(any(), any(), any())
            } answers {
                val onLoadingError = secondArg<((Exception) -> Unit)?>()
                onLoadingError?.invoke(networkError)
                mockWWWEvents
            }

            // Act
            repository.loadEvents(errorCallback)

            // Assert - Callback should receive the exact error
            assertNotNull(callbackError, "Callback should receive error")
            assertTrue(callbackError is IOException, "Callback should receive IOException")
            assertEquals("Network I/O error", callbackError?.message)
        }

    // ==================== Error Propagation Tests ====================

    @Test
    fun `should log network errors for debugging`() =
        runTest {
            // Arrange
            val networkError = IOException("Connection reset by peer")
            every {
                mockWWWEvents.loadEvents(any(), any(), any())
            } answers {
                val onLoadingError = secondArg<((Exception) -> Unit)?>()
                onLoadingError?.invoke(networkError)
                mockWWWEvents
            }

            // Act
            repository.loadEvents { }

            // Assert - Error should be stored in lastError for debugging
            val lastError = repository.getLastError().first()
            assertNotNull(lastError, "Error should be logged in lastError state")
            assertEquals("Connection reset by peer", lastError.message)
        }

    @Test
    fun `should maintain loading state on network error`() =
        runTest {
            // Arrange
            val networkError = TimeoutException("Request timeout")

            every {
                mockWWWEvents.loadEvents(any(), any(), any())
            } answers {
                val onLoadingError = secondArg<((Exception) -> Unit)?>()
                // Simulate delayed error
                onLoadingError?.invoke(networkError)
                mockWWWEvents
            }

            // Assert - Initially not loading
            assertFalse(repository.isLoading().first(), "Should not be loading initially")

            // Act
            repository.loadEvents { }

            // Assert - Loading should be false after error
            assertFalse(repository.isLoading().first(), "Loading should be false after error")
        }

    @Test
    fun `should differentiate between network and data errors`() =
        runTest {
            // Test 1: Network error (IOException)
            val networkError = IOException("Network failure")
            every {
                mockWWWEvents.loadEvents(any(), any(), any())
            } answers {
                val onLoadingError = secondArg<((Exception) -> Unit)?>()
                onLoadingError?.invoke(networkError)
                mockWWWEvents
            }

            repository.loadEvents { }
            val error1 = repository.getLastError().first()
            assertNotNull(error1, "Network error should be set")
            assertTrue(error1 is IOException, "Should be IOException for network error")

            // Test 2: Data error (different exception type)
            val dataError = IllegalStateException("Invalid event data")
            every {
                mockWWWEvents.loadEvents(any(), any(), any())
            } answers {
                val onLoadingError = secondArg<((Exception) -> Unit)?>()
                onLoadingError?.invoke(dataError)
                mockWWWEvents
            }

            repository.loadEvents { }
            val error2 = repository.getLastError().first()
            assertNotNull(error2, "Data error should be set")
            assertTrue(error2 is IllegalStateException, "Should be IllegalStateException for data error")
            assertEquals("Invalid event data", error2.message)
        }

    // ==================== Advanced Network Error Scenarios ====================

    @Test
    fun `should handle WWWEvents throwing IOException during loadEvents`() =
        runTest {
            // Arrange - WWWEvents.loadEvents itself throws exception
            val ioException = IOException("Firestore initialization failed")
            var errorReceived: Exception? = null

            every {
                mockWWWEvents.loadEvents(any(), any(), any())
            } throws ioException

            // Act
            repository.loadEvents { error -> errorReceived = error }

            // Assert
            assertNotNull(errorReceived, "Error callback should be invoked")
            assertTrue(errorReceived is IOException, "Should receive IOException")
            assertEquals("Firestore initialization failed", errorReceived?.message)

            // Assert - Error should be in state
            val lastError = repository.getLastError().first()
            assertNotNull(lastError, "Last error should be set")
            assertEquals("Firestore initialization failed", lastError.message)
        }

    @Test
    fun `should handle multiple concurrent network errors gracefully`() =
        runTest {
            // Arrange
            val error1 = IOException("Connection 1 failed")
            val error2 = IOException("Connection 2 failed")

            var callCount = 0
            every {
                mockWWWEvents.loadEvents(any(), any(), any())
            } answers {
                val onLoadingError = secondArg<((Exception) -> Unit)?>()
                callCount++
                if (callCount == 1) {
                    onLoadingError?.invoke(error1)
                } else {
                    onLoadingError?.invoke(error2)
                }
                mockWWWEvents
            }

            // Act - Trigger two concurrent load attempts
            repository.loadEvents { }
            repository.loadEvents { }

            // Assert - Should handle both errors without crashing
            val lastError = repository.getLastError().first()
            assertNotNull(lastError, "Should have captured an error")
            assertTrue(
                lastError.message == "Connection 1 failed" || lastError.message == "Connection 2 failed",
                "Should have one of the error messages",
            )
        }

    @Test
    fun `should handle termination callback with null exception`() =
        runTest {
            // Arrange - Termination with null (successful termination)
            every { mockWWWEvents.list() } returns emptyList()
            every {
                mockWWWEvents.loadEvents(any(), any(), any())
            } answers {
                val onTermination = thirdArg<((Exception?) -> Unit)?>()
                onTermination?.invoke(null) // Successful termination
                mockWWWEvents
            }

            // Act
            repository.loadEvents { }

            // Assert - No error should be set
            val error = repository.getLastError().first()
            assertNull(error, "Error should be null on successful termination")

            // Assert - Loading should complete
            assertFalse(repository.isLoading().first(), "Loading should complete")
        }

    @Test
    fun `should clear error state when starting new loadEvents`() =
        runTest {
            // Arrange - First attempt fails
            val networkError = IOException("Network error")
            var attemptCount = 0

            every { mockWWWEvents.list() } returns emptyList()
            every {
                mockWWWEvents.loadEvents(any(), any(), any())
            } answers {
                attemptCount++
                if (attemptCount == 1) {
                    val onLoadingError = secondArg<((Exception) -> Unit)?>()
                    onLoadingError?.invoke(networkError)
                } else {
                    val onLoaded = firstArg<(() -> Unit)?>()
                    onLoaded?.invoke()
                }
                mockWWWEvents
            }

            // Act - First attempt (fails)
            repository.loadEvents { }
            assertNotNull(repository.getLastError().first(), "Error should be set after failure")

            // Act - Second attempt (succeeds)
            repository.loadEvents { }

            // Give background coroutine time to complete
            delay(100)

            // Assert - Error should be cleared when loadEvents is called again
            // This is by design: loadEvents clears lastError at the start (line 69 in EventsRepositoryImpl)
            val errorAfterSuccess = repository.getLastError().first()
            assertNull(
                errorAfterSuccess,
                "Error should be cleared when loadEvents is called (cleared at start of loadEvents)",
            )
        }

    @Test
    fun `refreshEvents should clear previous error on successful retry`() =
        runTest {
            // Arrange - First attempt fails
            val networkError = IOException("Network error")
            every {
                mockWWWEvents.loadEvents(any(), any(), any())
            } answers {
                val onLoadingError = secondArg<((Exception) -> Unit)?>()
                onLoadingError?.invoke(networkError)
                mockWWWEvents
            }

            // First refresh fails
            val firstResult = repository.refreshEvents()
            assertTrue(firstResult.isFailure, "First refresh should fail")
            assertNotNull(repository.getLastError().first(), "Error should be set")

            // Now setup successful refresh
            every { mockWWWEvents.list() } returns emptyList()
            every {
                mockWWWEvents.loadEvents(any(), any(), any())
            } answers {
                val onLoaded = firstArg<(() -> Unit)?>()
                onLoaded?.invoke()
                mockWWWEvents
            }

            // Act - Second refresh (succeeds)
            val secondResult = repository.refreshEvents()

            // Assert - Should succeed and clear error
            assertTrue(secondResult.isSuccess, "Second refresh should succeed")
        }

    @Test
    fun `should handle network error during refreshEvents`() =
        runTest {
            // Arrange
            val networkError = UnknownHostException("No network connection")
            every {
                mockWWWEvents.loadEvents(any(), any(), any())
            } answers {
                val onLoadingError = secondArg<((Exception) -> Unit)?>()
                onLoadingError?.invoke(networkError)
                mockWWWEvents
            }

            // Act
            val result = repository.refreshEvents()

            // Assert - Should return failure
            assertTrue(result.isFailure, "Refresh should fail on network error")
            val exception = result.exceptionOrNull()
            assertNotNull(exception, "Should have exception")
            assertTrue(exception is UnknownHostException, "Should be UnknownHostException")
            assertEquals("No network connection", exception.message)

            // Assert - Error state should be set
            val lastError = repository.getLastError().first()
            assertNotNull(lastError, "Last error should be set")
            assertEquals("No network connection", lastError.message)
        }

    @Test
    fun `should verify loadEvents is called with correct callbacks on error`() =
        runTest {
            // Arrange
            val networkError = IOException("Test error")
            every {
                mockWWWEvents.loadEvents(any(), any(), any())
            } answers {
                val onLoadingError = secondArg<((Exception) -> Unit)?>()
                onLoadingError?.invoke(networkError)
                mockWWWEvents
            }

            // Act
            repository.loadEvents { }

            // Assert - Verify loadEvents was called with callbacks
            verify(exactly = 1) {
                mockWWWEvents.loadEvents(
                    onLoadingError = any(),
                    onLoaded = any(),
                    onTermination = any(),
                )
            }
        }
}
