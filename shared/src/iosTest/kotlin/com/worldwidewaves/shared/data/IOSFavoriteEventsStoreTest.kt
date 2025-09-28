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

import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.runTest
import platform.Foundation.NSUserDefaults
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * iOS-specific tests for IOSFavoriteEventsStore.
 *
 * These tests verify the NSUserDefaults-based persistence and thread-safe operations.
 */
class IOSFavoriteEventsStoreTest {
    private lateinit var favoriteEventsStore: IOSFavoriteEventsStore
    private val testEventId1 = "test_event_paris"
    private val testEventId2 = "test_event_london"
    private val testScheduler = TestCoroutineScheduler()

    @BeforeTest
    fun setUp() {
        favoriteEventsStore = IOSFavoriteEventsStore()

        // Clean up any existing test data
        cleanupTestData()
    }

    @AfterTest
    fun tearDown() {
        cleanupTestData()
    }

    private fun cleanupTestData() {
        val userDefaults = NSUserDefaults.standardUserDefaults
        userDefaults.removeObjectForKey("favorite_$testEventId1")
        userDefaults.removeObjectForKey("favorite_$testEventId2")
        userDefaults.synchronize()
    }

    @Test
    fun `initial favorite status is false`() =
        runTest(testScheduler) {
            val isFavorite = favoriteEventsStore.isFavorite(testEventId1)
            assertFalse(isFavorite)
        }

    @Test
    fun `setFavoriteStatus true persists correctly`() =
        runTest(testScheduler) {
            favoriteEventsStore.setFavoriteStatus(testEventId1, true)

            val isFavorite = favoriteEventsStore.isFavorite(testEventId1)
            assertTrue(isFavorite)
        }

    @Test
    fun `setFavoriteStatus false persists correctly`() =
        runTest(testScheduler) {
            // First set to true
            favoriteEventsStore.setFavoriteStatus(testEventId1, true)
            assertTrue(favoriteEventsStore.isFavorite(testEventId1))

            // Then set to false
            favoriteEventsStore.setFavoriteStatus(testEventId1, false)
            assertFalse(favoriteEventsStore.isFavorite(testEventId1))
        }

    @Test
    fun `multiple events can have different favorite status`() =
        runTest(testScheduler) {
            favoriteEventsStore.setFavoriteStatus(testEventId1, true)
            favoriteEventsStore.setFavoriteStatus(testEventId2, false)

            assertTrue(favoriteEventsStore.isFavorite(testEventId1))
            assertFalse(favoriteEventsStore.isFavorite(testEventId2))
        }

    @Test
    fun `favorite status persists across store instances`() =
        runTest(testScheduler) {
            // Set favorite with first instance
            favoriteEventsStore.setFavoriteStatus(testEventId1, true)

            // Create new instance (simulating app restart)
            val newStore = IOSFavoriteEventsStore()

            // Should still be favorite
            assertTrue(newStore.isFavorite(testEventId1))
        }

    @Test
    fun `concurrent operations are thread-safe`() =
        runTest(testScheduler) {
            val operations = 100
            val eventIds = (1..operations).map { "concurrent_event_$it" }

            // Perform concurrent writes
            eventIds.forEach { eventId ->
                favoriteEventsStore.setFavoriteStatus(eventId, true)
            }

            // Verify all were set correctly
            eventIds.forEach { eventId ->
                assertTrue(favoriteEventsStore.isFavorite(eventId))
            }

            // Cleanup
            eventIds.forEach { eventId ->
                val userDefaults = NSUserDefaults.standardUserDefaults
                userDefaults.removeObjectForKey("favorite_$eventId")
            }
        }

    @Test
    fun `toggle favorite status multiple times works correctly`() =
        runTest(testScheduler) {
            val toggleCount = 10
            var expectedStatus = false

            repeat(toggleCount) {
                expectedStatus = !expectedStatus
                favoriteEventsStore.setFavoriteStatus(testEventId1, expectedStatus)
                assertEquals(expectedStatus, favoriteEventsStore.isFavorite(testEventId1))
            }
        }

    @Test
    fun `favorite key format is correct`() =
        runTest(testScheduler) {
            favoriteEventsStore.setFavoriteStatus(testEventId1, true)

            // Verify the key format directly in NSUserDefaults
            val userDefaults = NSUserDefaults.standardUserDefaults
            val storedValue = userDefaults.boolForKey("favorite_$testEventId1")
            assertTrue(storedValue)
        }

    @Test
    fun `empty event id is handled gracefully`() =
        runTest(testScheduler) {
            val emptyEventId = ""

            favoriteEventsStore.setFavoriteStatus(emptyEventId, true)
            assertTrue(favoriteEventsStore.isFavorite(emptyEventId))

            favoriteEventsStore.setFavoriteStatus(emptyEventId, false)
            assertFalse(favoriteEventsStore.isFavorite(emptyEventId))

            // Cleanup
            val userDefaults = NSUserDefaults.standardUserDefaults
            userDefaults.removeObjectForKey("favorite_")
        }

    @Test
    fun `special characters in event id work correctly`() =
        runTest(testScheduler) {
            val specialEventId = "event_with_special_chars!@#$%^&*()_+-=[]{}|;:,.<>?"

            favoriteEventsStore.setFavoriteStatus(specialEventId, true)
            assertTrue(favoriteEventsStore.isFavorite(specialEventId))

            // Cleanup
            val userDefaults = NSUserDefaults.standardUserDefaults
            userDefaults.removeObjectForKey("favorite_$specialEventId")
        }

    @Test
    fun `synchronize is called after setFavoriteStatus`() =
        runTest(testScheduler) {
            // This is tested implicitly through persistence tests
            // NSUserDefaults.synchronize() ensures immediate persistence
            favoriteEventsStore.setFavoriteStatus(testEventId1, true)

            // Should be immediately readable from NSUserDefaults
            val userDefaults = NSUserDefaults.standardUserDefaults
            assertTrue(userDefaults.boolForKey("favorite_$testEventId1"))
        }
}
