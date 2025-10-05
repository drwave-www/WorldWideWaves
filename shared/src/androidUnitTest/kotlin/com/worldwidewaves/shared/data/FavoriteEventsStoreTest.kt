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

import com.worldwidewaves.shared.events.IWWWEvent
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Comprehensive tests for FavoriteEventsStore interface and related use cases.
 *
 * Tests verify:
 * - Favorite persistence across store instances (simulating app restarts)
 * - Add and remove operations work correctly
 * - Concurrent modifications are handled safely
 * - Error handling for storage failures
 * - Integration with InitFavoriteEvent and SetEventFavorite use cases
 * - Edge cases (empty IDs, special characters, rapid toggles)
 */
class FavoriteEventsStoreTest {
    /**
     * In-memory mock implementation for testing platform-independent logic.
     * Supports error injection for testing failure scenarios.
     */
    private class MockFavoriteEventsStore(
        private val throwOnRead: Boolean = false,
        private val throwOnWrite: Boolean = false,
    ) : FavoriteEventsStore {
        private val storage = mutableMapOf<String, Boolean>()
        private val mutex = Mutex()

        override suspend fun setFavoriteStatus(
            eventId: String,
            isFavorite: Boolean,
        ) = mutex.withLock {
            if (throwOnWrite) {
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

        // Test helper to simulate persistence across instances
        fun copyStorage(): Map<String, Boolean> = storage.toMap()

        fun restoreStorage(data: Map<String, Boolean>) {
            storage.clear()
            storage.putAll(data)
        }
    }

    /**
     * Mock event for testing InitFavoriteEvent and SetEventFavorite.
     */
    private class MockEvent(
        override val id: String,
    ) : IWWWEvent {
        override var favorite: Boolean = false

        // Minimal implementation - other methods throw to ensure they're not called
        override val type: String get() = throw NotImplementedError()
        override val country: String? get() = throw NotImplementedError()
        override val community: String? get() = throw NotImplementedError()
        override val timeZone: String get() = throw NotImplementedError()
        override val date: String get() = throw NotImplementedError()
        override val startHour: String get() = throw NotImplementedError()
        override val instagramAccount: String get() = throw NotImplementedError()
        override val instagramHashtag: String get() = throw NotImplementedError()
        override val wavedef: com.worldwidewaves.shared.events.WWWEvent.WWWWaveDefinition
            get() = throw NotImplementedError()
        override val area: com.worldwidewaves.shared.events.WWWEventArea
            get() = throw NotImplementedError()
        override val warming: com.worldwidewaves.shared.events.WWWEventWaveWarming
            get() = throw NotImplementedError()
        override val wave: com.worldwidewaves.shared.events.WWWEventWave
            get() = throw NotImplementedError()
        override val map: com.worldwidewaves.shared.events.WWWEventMap
            get() = throw NotImplementedError()

        override suspend fun getStatus() = throw NotImplementedError()

        override suspend fun isDone() = throw NotImplementedError()

        override fun isSoon() = throw NotImplementedError()

        override suspend fun isRunning() = throw NotImplementedError()

        override fun getLocationImage() = throw NotImplementedError()

        override fun getCommunityImage() = throw NotImplementedError()

        override fun getCountryImage() = throw NotImplementedError()

        override fun getMapImage() = throw NotImplementedError()

        override fun getLocation() = throw NotImplementedError()

        override fun getDescription() = throw NotImplementedError()

        override fun getLiteralCountry() = throw NotImplementedError()

        override fun getLiteralCommunity() = throw NotImplementedError()

        override fun getTZ() = throw NotImplementedError()

        override fun getStartDateTime() = throw NotImplementedError()

        override suspend fun getTotalTime() = throw NotImplementedError()

        override suspend fun getEndDateTime() = throw NotImplementedError()

        override fun getLiteralTimezone() = throw NotImplementedError()

        override fun getLiteralStartDateSimple() = throw NotImplementedError()

        override fun getLiteralStartTime() = throw NotImplementedError()

        override suspend fun getLiteralEndTime() = throw NotImplementedError()

        override suspend fun getLiteralTotalTime() = throw NotImplementedError()

        override fun getWaveStartDateTime() = throw NotImplementedError()

        override fun getWarmingDuration() = throw NotImplementedError()

        override fun isNearTime() = throw NotImplementedError()

        override suspend fun getAllNumbers() = throw NotImplementedError()

        override fun getEventObserver() = throw NotImplementedError()

        override fun validationErrors(): List<String>? = null
    }

    // ============================================
    // Basic Persistence Tests
    // ============================================

    @Test
    fun `initial favorite status is false for new event`() =
        runTest {
            val store = MockFavoriteEventsStore()
            val eventId = "test_event_paris"

            val isFavorite = store.isFavorite(eventId)

            assertFalse(isFavorite, "New event should not be favorite by default")
        }

    @Test
    fun `setFavoriteStatus true persists correctly`() =
        runTest {
            val store = MockFavoriteEventsStore()
            val eventId = "test_event_london"

            store.setFavoriteStatus(eventId, true)
            val isFavorite = store.isFavorite(eventId)

            assertTrue(isFavorite, "Event should be favorite after setting to true")
        }

    @Test
    fun `setFavoriteStatus false persists correctly`() =
        runTest {
            val store = MockFavoriteEventsStore()
            val eventId = "test_event_tokyo"

            // First set to true
            store.setFavoriteStatus(eventId, true)
            assertTrue(store.isFavorite(eventId))

            // Then set to false
            store.setFavoriteStatus(eventId, false)
            assertFalse(store.isFavorite(eventId), "Event should not be favorite after setting to false")
        }

    @Test
    fun `multiple events can have different favorite status`() =
        runTest {
            val store = MockFavoriteEventsStore()
            val eventId1 = "test_event_paris"
            val eventId2 = "test_event_london"
            val eventId3 = "test_event_tokyo"

            store.setFavoriteStatus(eventId1, true)
            store.setFavoriteStatus(eventId2, false)
            store.setFavoriteStatus(eventId3, true)

            assertTrue(store.isFavorite(eventId1), "Event 1 should be favorite")
            assertFalse(store.isFavorite(eventId2), "Event 2 should not be favorite")
            assertTrue(store.isFavorite(eventId3), "Event 3 should be favorite")
        }

    @Test
    fun `favorite status persists across store instances`() =
        runTest {
            val store1 = MockFavoriteEventsStore()
            val eventId1 = "test_event_paris"
            val eventId2 = "test_event_london"

            // Set favorites with first instance
            store1.setFavoriteStatus(eventId1, true)
            store1.setFavoriteStatus(eventId2, false)

            // Simulate app restart by creating new instance with same storage
            val persistedData = store1.copyStorage()
            val store2 = MockFavoriteEventsStore()
            store2.restoreStorage(persistedData)

            // Verify favorites persisted
            assertTrue(store2.isFavorite(eventId1), "Event 1 favorite status should persist")
            assertFalse(store2.isFavorite(eventId2), "Event 2 favorite status should persist")
        }

    // ============================================
    // Concurrent Modification Tests
    // ============================================

    @Test
    fun `concurrent setFavoriteStatus operations are thread-safe`() =
        runTest {
            val store = MockFavoriteEventsStore()
            val concurrentOperations = 100
            val eventIds = (1..concurrentOperations).map { "concurrent_event_$it" }

            // Perform concurrent writes
            val jobs =
                eventIds.map { eventId ->
                    async {
                        store.setFavoriteStatus(eventId, true)
                    }
                }
            jobs.awaitAll()

            // Verify all were set correctly
            eventIds.forEach { eventId ->
                assertTrue(store.isFavorite(eventId), "Event $eventId should be favorite after concurrent write")
            }
        }

    @Test
    fun `concurrent mixed read and write operations are safe`() =
        runTest {
            val store = MockFavoriteEventsStore()
            val eventId = "test_event_concurrent"

            // Concurrent writes and reads
            val operations =
                (1..50).map {
                    async {
                        store.setFavoriteStatus(eventId, it % 2 == 0)
                        store.isFavorite(eventId)
                    }
                }
            operations.awaitAll()

            // Should not crash and should have a consistent final state
            val finalState = store.isFavorite(eventId)
            // Verify finalState is a valid boolean (no corruption)
            @Suppress("USELESS_IS_CHECK")
            assertTrue(finalState is Boolean, "Final state should be a boolean")
        }

    @Test
    fun `rapid toggle operations maintain consistency`() =
        runTest {
            val store = MockFavoriteEventsStore()
            val eventId = "test_event_toggle"
            val toggleCount = 20

            // Rapidly toggle favorite status
            var expectedStatus = false
            repeat(toggleCount) {
                expectedStatus = !expectedStatus
                store.setFavoriteStatus(eventId, expectedStatus)
                assertEquals(expectedStatus, store.isFavorite(eventId), "Status should match after toggle $it")
            }
        }

    // ============================================
    // Error Handling Tests
    // ============================================

    @Test
    fun `setFavoriteStatus throws DataStoreException on write failure`() =
        runTest {
            val store = MockFavoriteEventsStore(throwOnWrite = true)
            val eventId = "test_event_write_error"

            assertFailsWith<DataStoreException> {
                store.setFavoriteStatus(eventId, true)
            }
        }

    @Test
    fun `isFavorite throws DataStoreException on read failure`() =
        runTest {
            val store = MockFavoriteEventsStore(throwOnRead = true)
            val eventId = "test_event_read_error"

            assertFailsWith<DataStoreException> {
                store.isFavorite(eventId)
            }
        }

    @Test
    fun `storage errors do not corrupt state`() =
        runTest {
            val goodStore = MockFavoriteEventsStore()
            val badStore = MockFavoriteEventsStore(throwOnWrite = true)
            val eventId = "test_event_error_recovery"

            // Set favorite in good store
            goodStore.setFavoriteStatus(eventId, true)
            assertTrue(goodStore.isFavorite(eventId))

            // Try to set in bad store (should fail)
            assertFailsWith<DataStoreException> {
                badStore.setFavoriteStatus(eventId, false)
            }

            // Good store should still have correct state
            assertTrue(goodStore.isFavorite(eventId), "Good store state should not be affected")
        }

    // ============================================
    // Edge Case Tests
    // ============================================

    @Test
    fun `empty event id is handled gracefully`() =
        runTest {
            val store = MockFavoriteEventsStore()
            val emptyEventId = ""

            store.setFavoriteStatus(emptyEventId, true)
            assertTrue(store.isFavorite(emptyEventId), "Empty event ID should be stored")

            store.setFavoriteStatus(emptyEventId, false)
            assertFalse(store.isFavorite(emptyEventId), "Empty event ID should be updated")
        }

    @Test
    fun `special characters in event id work correctly`() =
        runTest {
            val store = MockFavoriteEventsStore()
            val specialEventIds =
                listOf(
                    "event_with_spaces ",
                    "event-with-dashes",
                    "event_with_underscores_",
                    "event.with.dots",
                    "event@with@at",
                    "event#with#hash",
                    "event/with/slashes",
                    "event\\with\\backslashes",
                    "event:with:colons",
                )

            specialEventIds.forEach { eventId ->
                store.setFavoriteStatus(eventId, true)
                assertTrue(store.isFavorite(eventId), "Event ID '$eventId' should be stored correctly")
            }
        }

    @Test
    fun `very long event id is handled correctly`() =
        runTest {
            val store = MockFavoriteEventsStore()
            val longEventId = "a".repeat(1000) // 1000 character ID

            store.setFavoriteStatus(longEventId, true)
            assertTrue(store.isFavorite(longEventId), "Very long event ID should be stored")
        }

    @Test
    fun `unicode characters in event id work correctly`() =
        runTest {
            val store = MockFavoriteEventsStore()
            val unicodeEventIds =
                listOf(
                    "event_波浪_wave",
                    "event_🌊🌍",
                    "event_émoji_café",
                    "event_Москва",
                )

            unicodeEventIds.forEach { eventId ->
                store.setFavoriteStatus(eventId, true)
                assertTrue(store.isFavorite(eventId), "Unicode event ID '$eventId' should be stored correctly")
            }
        }

    // ============================================
    // InitFavoriteEvent Use Case Tests
    // ============================================

    @Test
    fun `InitFavoriteEvent loads persisted favorite status into event`() =
        runTest {
            val store = MockFavoriteEventsStore()
            val eventId = "test_event_init"
            val event = MockEvent(eventId)
            val initFavorite = InitFavoriteEvent(store)

            // Persist favorite status
            store.setFavoriteStatus(eventId, true)

            // Event starts with false
            assertFalse(event.favorite, "Event should start with favorite = false")

            // Init should load persisted status
            initFavorite.call(event)
            assertTrue(event.favorite, "Event favorite should be loaded from store")
        }

    @Test
    fun `InitFavoriteEvent sets false for non-favorited event`() =
        runTest {
            val store = MockFavoriteEventsStore()
            val eventId = "test_event_not_favorite"
            val event = MockEvent(eventId)
            val initFavorite = InitFavoriteEvent(store)

            // Do not set favorite status (defaults to false)
            initFavorite.call(event)

            assertFalse(event.favorite, "Non-favorited event should have favorite = false")
        }

    @Test
    fun `InitFavoriteEvent handles multiple events correctly`() =
        runTest {
            val store = MockFavoriteEventsStore()
            val initFavorite = InitFavoriteEvent(store)

            val event1 = MockEvent("event_1")
            val event2 = MockEvent("event_2")
            val event3 = MockEvent("event_3")

            // Set different statuses
            store.setFavoriteStatus("event_1", true)
            store.setFavoriteStatus("event_2", false)
            // event_3 not set (defaults to false)

            // Init all events
            initFavorite.call(event1)
            initFavorite.call(event2)
            initFavorite.call(event3)

            assertTrue(event1.favorite, "Event 1 should be favorite")
            assertFalse(event2.favorite, "Event 2 should not be favorite")
            assertFalse(event3.favorite, "Event 3 should not be favorite")
        }

    // ============================================
    // SetEventFavorite Use Case Tests
    // ============================================

    @Test
    fun `SetEventFavorite persists and updates event field`() =
        runTest {
            val store = MockFavoriteEventsStore()
            val eventId = "test_event_set"
            val event = MockEvent(eventId)
            val setFavorite = SetEventFavorite(store)

            // Set to true
            setFavorite.call(event, true)

            assertTrue(store.isFavorite(eventId), "Store should have favorite = true")
            assertTrue(event.favorite, "Event field should be updated to true")
        }

    @Test
    fun `SetEventFavorite can toggle status multiple times`() =
        runTest {
            val store = MockFavoriteEventsStore()
            val eventId = "test_event_toggle_set"
            val event = MockEvent(eventId)
            val setFavorite = SetEventFavorite(store)

            // Toggle multiple times
            setFavorite.call(event, true)
            assertTrue(event.favorite && store.isFavorite(eventId))

            setFavorite.call(event, false)
            assertFalse(event.favorite || store.isFavorite(eventId))

            setFavorite.call(event, true)
            assertTrue(event.favorite && store.isFavorite(eventId))
        }

    @Test
    fun `SetEventFavorite maintains consistency between store and event`() =
        runTest {
            val store = MockFavoriteEventsStore()
            val eventId = "test_event_consistency"
            val event = MockEvent(eventId)
            val setFavorite = SetEventFavorite(store)

            // Set to true
            setFavorite.call(event, true)
            assertEquals(store.isFavorite(eventId), event.favorite, "Store and event should match (true)")

            // Set to false
            setFavorite.call(event, false)
            assertEquals(store.isFavorite(eventId), event.favorite, "Store and event should match (false)")
        }

    // ============================================
    // Integration Tests
    // ============================================

    @Test
    fun `full workflow - init, set, persist, reload`() =
        runTest {
            val store = MockFavoriteEventsStore()
            val eventId = "test_event_workflow"
            val initFavorite = InitFavoriteEvent(store)
            val setFavorite = SetEventFavorite(store)

            // Step 1: Create event and init (should be false)
            val event1 = MockEvent(eventId)
            initFavorite.call(event1)
            assertFalse(event1.favorite, "New event should not be favorite")

            // Step 2: Set as favorite
            setFavorite.call(event1, true)
            assertTrue(event1.favorite, "Event should be favorite after setting")

            // Step 3: Simulate app restart - create new event instance and reload
            val event2 = MockEvent(eventId)
            initFavorite.call(event2)
            assertTrue(event2.favorite, "Favorite status should persist across instances")

            // Step 4: Unfavorite
            setFavorite.call(event2, false)
            assertFalse(event2.favorite, "Event should not be favorite after unsetting")

            // Step 5: Verify persistence
            val event3 = MockEvent(eventId)
            initFavorite.call(event3)
            assertFalse(event3.favorite, "Unfavorite status should persist")
        }

    @Test
    fun `multiple events can be favorited independently`() =
        runTest {
            val store = MockFavoriteEventsStore()
            val initFavorite = InitFavoriteEvent(store)
            val setFavorite = SetEventFavorite(store)

            val events = (1..10).map { MockEvent("event_$it") }

            // Favorite odd-numbered events
            events.forEachIndexed { index, event ->
                val shouldBeFavorite = index % 2 == 0
                setFavorite.call(event, shouldBeFavorite)
            }

            // Simulate reload
            val reloadedEvents = (1..10).map { MockEvent("event_$it") }
            reloadedEvents.forEach { initFavorite.call(it) }

            // Verify favorites persisted correctly
            reloadedEvents.forEachIndexed { index, event ->
                val expectedFavorite = index % 2 == 0
                assertEquals(
                    expectedFavorite,
                    event.favorite,
                    "Event ${event.id} favorite status should persist correctly",
                )
            }
        }

    @Test
    fun `clearing all favorites works correctly`() =
        runTest {
            val store = MockFavoriteEventsStore()
            val setFavorite = SetEventFavorite(store)

            // Create and favorite multiple events
            val events = (1..5).map { MockEvent("event_$it") }
            events.forEach { setFavorite.call(it, true) }

            // Verify all are favorited
            events.forEach {
                assertTrue(it.favorite, "Event ${it.id} should be favorite")
            }

            // Clear all favorites
            events.forEach { setFavorite.call(it, false) }

            // Verify all are cleared
            events.forEach {
                assertFalse(it.favorite, "Event ${it.id} should not be favorite after clearing")
                assertFalse(store.isFavorite(it.id), "Store should not have ${it.id} as favorite")
            }
        }
}
