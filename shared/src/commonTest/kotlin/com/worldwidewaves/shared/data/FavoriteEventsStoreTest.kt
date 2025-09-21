package com.worldwidewaves.shared.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.events.utils.Log
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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

/**
 * Comprehensive tests for FavoriteEventsStore, InitFavoriteEvent, and SetEventFavorite
 * including persistence, error handling, and edge cases.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FavoriteEventsStoreTest {

    private lateinit var testDataStore: DataStore<Preferences>
    private lateinit var favoriteEventsStore: FavoriteEventsStore

    @BeforeTest
    fun setUp() {
        // Mock the Log object
        mockkObject(Log)
        justRun { Log.e(any(), any(), any()) }

        // Create test DataStore
        testDataStore = PreferenceDataStoreFactory.createWithPath {
            "/tmp/test_favorites_${System.currentTimeMillis()}.preferences_pb".toPath()
        }

        favoriteEventsStore = FavoriteEventsStore(testDataStore, Dispatchers.Default)
    }

    @AfterTest
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `test setFavoriteStatus saves favorite flag correctly`() = runTest {
        val eventId = "test_event_1"

        // Set as favorite
        favoriteEventsStore.setFavoriteStatus(eventId, true)

        // Verify in datastore
        val preferences = testDataStore.data.first()
        val key = booleanPreferencesKey("favorite_$eventId")
        assertTrue(preferences[key] ?: false)
    }

    @Test
    fun `test setFavoriteStatus saves non-favorite flag correctly`() = runTest {
        val eventId = "test_event_2"

        // Set as non-favorite
        favoriteEventsStore.setFavoriteStatus(eventId, false)

        // Verify in datastore
        val preferences = testDataStore.data.first()
        val key = booleanPreferencesKey("favorite_$eventId")
        assertFalse(preferences[key] ?: true)
    }

    @Test
    fun `test isFavorite returns correct status for favorite event`() = runTest {
        val eventId = "test_event_3"

        // Set as favorite
        favoriteEventsStore.setFavoriteStatus(eventId, true)

        // Check status
        val isFavorite = favoriteEventsStore.isFavorite(eventId)
        assertTrue(isFavorite)
    }

    @Test
    fun `test isFavorite returns correct status for non-favorite event`() = runTest {
        val eventId = "test_event_4"

        // Set as non-favorite
        favoriteEventsStore.setFavoriteStatus(eventId, false)

        // Check status
        val isFavorite = favoriteEventsStore.isFavorite(eventId)
        assertFalse(isFavorite)
    }

    @Test
    fun `test isFavorite returns false for non-existent event`() = runTest {
        val nonExistentEventId = "non_existent_event"

        // Check status without setting
        val isFavorite = favoriteEventsStore.isFavorite(nonExistentEventId)
        assertFalse(isFavorite)
    }

    @Test
    fun `test toggling favorite status works correctly`() = runTest {
        val eventId = "toggle_test_event"

        // Initially not favorite
        assertFalse(favoriteEventsStore.isFavorite(eventId))

        // Set as favorite
        favoriteEventsStore.setFavoriteStatus(eventId, true)
        assertTrue(favoriteEventsStore.isFavorite(eventId))

        // Toggle back to non-favorite
        favoriteEventsStore.setFavoriteStatus(eventId, false)
        assertFalse(favoriteEventsStore.isFavorite(eventId))

        // Toggle back to favorite
        favoriteEventsStore.setFavoriteStatus(eventId, true)
        assertTrue(favoriteEventsStore.isFavorite(eventId))
    }

    @Test
    fun `test multiple events can be favorited independently`() = runTest {
        val event1Id = "multi_test_event_1"
        val event2Id = "multi_test_event_2"
        val event3Id = "multi_test_event_3"

        // Set different favorite statuses
        favoriteEventsStore.setFavoriteStatus(event1Id, true)
        favoriteEventsStore.setFavoriteStatus(event2Id, false)
        favoriteEventsStore.setFavoriteStatus(event3Id, true)

        // Verify independent statuses
        assertTrue(favoriteEventsStore.isFavorite(event1Id))
        assertFalse(favoriteEventsStore.isFavorite(event2Id))
        assertTrue(favoriteEventsStore.isFavorite(event3Id))
    }

    @Test
    fun `test concurrent favorite operations are thread-safe`() = runTest {
        val eventIds = (1..10).map { "concurrent_event_$it" }

        // Launch concurrent operations
        val operations = eventIds.mapIndexed { index, eventId ->
            async {
                favoriteEventsStore.setFavoriteStatus(eventId, index % 2 == 0)
            }
        }

        // Wait for all operations
        operations.awaitAll()

        // Verify results
        eventIds.forEachIndexed { index, eventId ->
            val expectedFavorite = index % 2 == 0
            assertEquals(expectedFavorite, favoriteEventsStore.isFavorite(eventId))
        }
    }

    @Test
    fun `test error handling in isFavorite when datastore fails`() = runTest {
        // Create a mock datastore that throws exceptions
        val mockDataStore = mockk<DataStore<Preferences>>()
        val faultyStore = FavoriteEventsStore(mockDataStore, Dispatchers.Default)

        // Configure mock to return a flow that throws an exception
        val errorFlow = flow<Preferences> { throw RuntimeException("DataStore error") }
        every { mockDataStore.data } returns errorFlow

        // Should handle error gracefully and return false
        val result = faultyStore.isFavorite("error_test_event")
        assertFalse(result)

        // Verify error was logged
        verify { Log.e(any(), any(), any()) }
    }

    @Test
    fun `test InitFavoriteEvent loads persisted favorite status`() = runTest {
        val mockEvent = mockk<IWWWEvent>(relaxed = true)
        every { mockEvent.id } returns "init_test_event"

        // Set favorite status in store
        favoriteEventsStore.setFavoriteStatus("init_test_event", true)

        // Create and call InitFavoriteEvent
        val initFavoriteEvent = InitFavoriteEvent(favoriteEventsStore)
        initFavoriteEvent.call(mockEvent)

        // Verify event.favorite was set
        verify { mockEvent.favorite = true }
    }

    @Test
    fun `test InitFavoriteEvent sets false for non-existent event`() = runTest {
        val mockEvent = mockk<IWWWEvent>(relaxed = true)
        every { mockEvent.id } returns "non_existent_init_event"

        // Create and call InitFavoriteEvent without setting favorite status
        val initFavoriteEvent = InitFavoriteEvent(favoriteEventsStore)
        initFavoriteEvent.call(mockEvent)

        // Verify event.favorite was set to false
        verify { mockEvent.favorite = false }
    }

    @Test
    fun `test SetEventFavorite persists and updates event object`() = runTest {
        val mockEvent = mockk<IWWWEvent>(relaxed = true)
        every { mockEvent.id } returns "set_test_event"

        val setEventFavorite = SetEventFavorite(favoriteEventsStore)

        // Set as favorite
        setEventFavorite.call(mockEvent, true)

        // Verify persistence and event update
        assertTrue(favoriteEventsStore.isFavorite("set_test_event"))
        verify { mockEvent.favorite = true }
    }

    @Test
    fun `test SetEventFavorite with false value`() = runTest {
        val mockEvent = mockk<IWWWEvent>(relaxed = true)
        every { mockEvent.id } returns "set_false_test_event"

        val setEventFavorite = SetEventFavorite(favoriteEventsStore)

        // Set as non-favorite
        setEventFavorite.call(mockEvent, false)

        // Verify persistence and event update
        assertFalse(favoriteEventsStore.isFavorite("set_false_test_event"))
        verify { mockEvent.favorite = false }
    }

    @Test
    fun `test complete workflow with InitFavoriteEvent and SetEventFavorite`() = runTest {
        val mockEvent = mockk<IWWWEvent>(relaxed = true)
        val eventId = "workflow_test_event"
        every { mockEvent.id } returns eventId

        val initFavoriteEvent = InitFavoriteEvent(favoriteEventsStore)
        val setEventFavorite = SetEventFavorite(favoriteEventsStore)

        // Initial state - should be false
        initFavoriteEvent.call(mockEvent)
        verify { mockEvent.favorite = false }

        // Set as favorite
        setEventFavorite.call(mockEvent, true)
        assertTrue(favoriteEventsStore.isFavorite(eventId))
        verify { mockEvent.favorite = true }

        // Create new event instance and init - should load true
        val mockEvent2 = mockk<IWWWEvent>(relaxed = true)
        every { mockEvent2.id } returns eventId
        initFavoriteEvent.call(mockEvent2)
        verify { mockEvent2.favorite = true }

        // Toggle to false
        setEventFavorite.call(mockEvent2, false)
        assertFalse(favoriteEventsStore.isFavorite(eventId))
        verify { mockEvent2.favorite = false }
    }

    @Test
    fun `test favorite key generation is consistent`() = runTest {
        val eventId = "key_test_event"

        // Set favorite through store
        favoriteEventsStore.setFavoriteStatus(eventId, true)

        // Manually check the key exists in datastore
        val preferences = testDataStore.data.first()
        val expectedKey = booleanPreferencesKey("favorite_$eventId")
        assertTrue(preferences[expectedKey] ?: false)
    }

    @Test
    fun `test special characters in event IDs are handled correctly`() = runTest {
        val specialEventIds = listOf(
            "event-with-dashes",
            "event_with_underscores",
            "event.with.dots",
            "event with spaces",
            "event@with#special!chars",
        )

        // Set all as favorites
        specialEventIds.forEach { eventId ->
            favoriteEventsStore.setFavoriteStatus(eventId, true)
        }

        // Verify all are correctly stored and retrieved
        specialEventIds.forEach { eventId ->
            assertTrue(favoriteEventsStore.isFavorite(eventId), "Failed for eventId: $eventId")
        }
    }

    @Test
    fun `test dispatcher is used for all operations`() = runTest {
        // This test verifies that the dispatcher parameter is respected
        // We can't easily test the dispatcher directly, but we can verify
        // operations complete successfully with a custom dispatcher

        val customDispatcher = Dispatchers.Default
        val storeWithCustomDispatcher = FavoriteEventsStore(testDataStore, customDispatcher)

        val eventId = "dispatcher_test_event"

        // Operations should work with custom dispatcher
        storeWithCustomDispatcher.setFavoriteStatus(eventId, true)
        assertTrue(storeWithCustomDispatcher.isFavorite(eventId))
    }
}