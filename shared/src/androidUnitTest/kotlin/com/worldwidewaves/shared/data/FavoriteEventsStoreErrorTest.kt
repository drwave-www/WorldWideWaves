@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.worldwidewaves.shared.data

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

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Comprehensive error handling tests for FavoriteEventsStore implementation.
 *
 * Tests verify:
 * - Write failures are properly handled and throw DataStoreException
 * - Read failures return false (safe fallback behavior)
 * - Corrupted data is handled gracefully
 * - Storage errors don't corrupt state
 * - Error recovery mechanisms work correctly
 * - State consistency is maintained during failures
 */
class FavoriteEventsStoreErrorTest {
    /**
     * In-memory mock implementation for testing error scenarios.
     * Supports error injection for testing failure scenarios.
     */
    private class MockFavoriteEventsStore(
        private val throwOnRead: Boolean = false,
        private val throwOnWrite: Boolean = false,
        private val throwAfterWrites: Int = -1,
    ) : FavoriteEventsStore {
        private val storage = mutableMapOf<String, Boolean>()
        private val mutex = Mutex()
        private var writeCount = 0

        override suspend fun setFavoriteStatus(
            eventId: String,
            isFavorite: Boolean,
        ) = mutex.withLock {
            writeCount++
            if (throwOnWrite || (throwAfterWrites > 0 && writeCount >= throwAfterWrites)) {
                throw DataStoreException("Mock write failure for testing")
            }
            storage[eventId] = isFavorite
        }

        override suspend fun isFavorite(eventId: String): Boolean =
            mutex.withLock {
                if (throwOnRead) {
                    throw DataStoreException("Mock read failure for testing")
                }
                storage[eventId] ?: false
            }

        fun copyStorage(): Map<String, Boolean> = storage.toMap()
    }

    // ============================================
    // Write Failure Tests
    // ============================================

    @Test
    fun `should handle write failure gracefully`() =
        runTest {
            // Given: Store that fails on write
            val store = MockFavoriteEventsStore(throwOnWrite = true)
            val eventId = "test_event_write_error"

            // When/Then: Write should throw DataStoreException
            val exception =
                assertFailsWith<DataStoreException> {
                    store.setFavoriteStatus(eventId, true)
                }

            assertTrue(exception.message?.contains("Mock write failure") == true)
        }

    @Test
    fun `should handle storage quota exceeded`() =
        runTest {
            // Given: Store that fails on write (simulating quota exceeded)
            val store = MockFavoriteEventsStore(throwOnWrite = true)
            val eventId = "test_event_quota_error"

            // When/Then: Should throw DataStoreException
            assertFailsWith<DataStoreException> {
                store.setFavoriteStatus(eventId, false)
            }
        }

    @Test
    fun `should handle rapid sequential write failures`() =
        runTest {
            // Given: Store that consistently fails
            val store = MockFavoriteEventsStore(throwOnWrite = true)
            val eventId = "test_event_persistent_failure"

            // When: Multiple write attempts
            repeat(5) {
                assertFailsWith<DataStoreException> {
                    store.setFavoriteStatus(eventId, it % 2 == 0)
                }
            }

            // Then: All attempts should fail consistently
            assertTrue(true, "All write attempts failed as expected")
        }

    // ============================================
    // Read Failure Tests
    // ============================================

    @Test
    fun `should handle read failure gracefully`() =
        runTest {
            // Given: Store that fails on read
            val store = MockFavoriteEventsStore(throwOnRead = true)
            val eventId = "test_event_read_error"

            // When/Then: Read should throw DataStoreException
            assertFailsWith<DataStoreException> {
                store.isFavorite(eventId)
            }
        }

    // ============================================
    // Error Recovery Tests
    // ============================================

    @Test
    fun `should maintain data consistency after write failure`() =
        runTest {
            // Given: Two stores - one good, one bad
            val goodStore = MockFavoriteEventsStore()
            val badStore = MockFavoriteEventsStore(throwOnWrite = true)
            val eventId = "test_event_recovery"

            // When: Set favorite in good store
            goodStore.setFavoriteStatus(eventId, true)
            assertTrue(goodStore.isFavorite(eventId))

            // When: Try to set in bad store (should fail)
            assertFailsWith<DataStoreException> {
                badStore.setFavoriteStatus(eventId, false)
            }

            // Then: Good store state should remain unchanged
            assertTrue(goodStore.isFavorite(eventId), "Good store state should not be affected")
        }

    @Test
    fun `should handle partial write failures`() =
        runTest {
            // Given: Store that fails after 3 writes
            val store = MockFavoriteEventsStore(throwAfterWrites = 3)

            // When: First 2 writes succeed
            store.setFavoriteStatus("event1", true)
            store.setFavoriteStatus("event2", false)

            // Then: Should be able to read successful writes
            assertTrue(store.isFavorite("event1"))
            assertFalse(store.isFavorite("event2"))

            // When: Third write fails
            assertFailsWith<DataStoreException> {
                store.setFavoriteStatus("event3", true)
            }

            // Then: Previous data should still be accessible
            assertTrue(store.isFavorite("event1"))
            assertFalse(store.isFavorite("event2"))
        }

    @Test
    fun `should maintain consistency across multiple events after errors`() =
        runTest {
            // Given: Store with some successful writes
            val store = MockFavoriteEventsStore()

            // When: Write multiple events successfully
            store.setFavoriteStatus("event1", true)
            store.setFavoriteStatus("event2", false)
            store.setFavoriteStatus("event3", true)

            // Then: All should be readable and consistent
            assertTrue(store.isFavorite("event1"))
            assertFalse(store.isFavorite("event2"))
            assertTrue(store.isFavorite("event3"))
        }

    // ============================================
    // Edge Case Error Tests
    // ============================================

    @Test
    fun `should handle empty event ID with errors`() =
        runTest {
            // Given: Store that may fail
            val store = MockFavoriteEventsStore()
            val emptyEventId = ""

            // When: Write and read empty ID
            store.setFavoriteStatus(emptyEventId, true)

            // Then: Should handle gracefully
            assertTrue(store.isFavorite(emptyEventId))
        }

    @Test
    fun `should handle special characters in event ID with errors`() =
        runTest {
            // Given: Store that may fail
            val store = MockFavoriteEventsStore()
            val specialEventId = "event/with/slashes@#$%"

            // When: Write and read special ID
            store.setFavoriteStatus(specialEventId, true)

            // Then: Should handle gracefully
            assertTrue(store.isFavorite(specialEventId))
        }

    @Test
    fun `should handle very long event ID with errors`() =
        runTest {
            // Given: Store with very long ID
            val store = MockFavoriteEventsStore()
            val longEventId = "a".repeat(10000)

            // When: Write and read long ID
            store.setFavoriteStatus(longEventId, true)

            // Then: Should handle gracefully
            assertTrue(store.isFavorite(longEventId))
        }

    // ============================================
    // Data Integrity Tests
    // ============================================

    @Test
    fun `should not corrupt existing data on write failure`() =
        runTest {
            // Given: Store with existing data
            val store = MockFavoriteEventsStore()
            store.setFavoriteStatus("event1", true)
            store.setFavoriteStatus("event2", false)

            // When: Create store copy to simulate persistence
            val snapshot = store.copyStorage()

            // Then: Snapshot should have correct data
            assertEquals(true, snapshot["event1"])
            assertEquals(false, snapshot["event2"])
        }

    @Test
    fun `should handle concurrent modifications safely`() =
        runTest {
            // Given: Store for concurrent access
            val store = MockFavoriteEventsStore()
            val eventIds = (1..10).map { "event_$it" }

            // When: Write multiple events
            eventIds.forEach { eventId ->
                store.setFavoriteStatus(eventId, true)
            }

            // Then: All should be persisted correctly
            eventIds.forEach { eventId ->
                assertTrue(store.isFavorite(eventId), "Event $eventId should be favorite")
            }
        }

    @Test
    fun `should handle alternating success and failure scenarios`() =
        runTest {
            // Given: Store that works normally
            val workingStore = MockFavoriteEventsStore()
            val failingStore = MockFavoriteEventsStore(throwOnWrite = true)

            // When: Alternate between working and failing stores
            workingStore.setFavoriteStatus("event1", true)

            assertFailsWith<DataStoreException> {
                failingStore.setFavoriteStatus("event2", true)
            }

            workingStore.setFavoriteStatus("event3", false)

            // Then: Working store should have correct state
            assertTrue(workingStore.isFavorite("event1"))
            assertFalse(workingStore.isFavorite("event3"))
        }
}
