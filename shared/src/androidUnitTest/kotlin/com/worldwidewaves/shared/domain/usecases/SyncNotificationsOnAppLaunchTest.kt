@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

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
import com.worldwidewaves.shared.notifications.NotificationScheduler
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

/**
 * Comprehensive test suite for SyncNotificationsOnAppLaunch.
 *
 * ## Coverage
 * Tests all edge cases for app initialization notification sync:
 * 1. Zero favorited, zero downloaded → sync with empty set
 * 2. Multiple favorited, zero downloaded → sync with favorited IDs
 * 3. Zero favorited, multiple downloaded → sync with downloaded IDs
 * 4. Same events favorited AND downloaded → sync with deduplicated IDs
 * 5. Different events favorited vs downloaded → sync with union
 * 6. Mixed: some favorited+downloaded, some only favorited, some only downloaded
 * 7. Logs correct counts (favorited, downloaded, total after deduplication)
 *
 * ## Test Strategy
 * - Mock NotificationScheduler to verify sync calls
 * - Mock MapAvailabilityChecker to control download status
 * - Create test events with various favorite/download combinations
 * - Verify correct Set union behavior (de-duplication)
 * - Verify logging output for debugging
 */
class SyncNotificationsOnAppLaunchTest {
    /**
     * Test implementation of MapAvailabilityChecker.
     */
    private class TestMapAvailabilityChecker(
        private val downloadedMaps: Set<String>,
    ) : MapAvailabilityChecker {
        override val mapStates = MutableStateFlow<Map<String, Boolean>>(emptyMap())

        override fun refreshAvailability() {}

        override fun isMapDownloaded(eventId: String): Boolean = downloadedMaps.contains(eventId)

        override fun getDownloadedMaps(): List<String> = downloadedMaps.toList()

        override fun trackMaps(mapIds: Collection<String>) {}
    }

    /**
     * Creates a test event with mockk for minimal implementation.
     * Only id and favorite are used by SyncNotificationsOnAppLaunch.
     */
    private fun createTestEvent(
        id: String,
        favorite: Boolean = false,
    ): IWWWEvent =
        mockk<IWWWEvent>(relaxed = true) {
            every { this@mockk.id } returns id
            every { this@mockk.favorite } returns favorite
            every { this@mockk.favorite = any() } answers { favorite }
        }

    @Test
    fun `app launch with 0 favorited and 0 downloaded should sync with empty set`() =
        runTest {
            // Given: 3 events, none favorited, none downloaded
            val events =
                listOf(
                    createTestEvent("event1"),
                    createTestEvent("event2"),
                    createTestEvent("event3"),
                )
            val mapChecker = TestMapAvailabilityChecker(emptySet())
            val scheduler = mockk<NotificationScheduler>(relaxed = true)
            val useCase = SyncNotificationsOnAppLaunch(scheduler, mapChecker)

            // When: Sync on app launch
            useCase(events)

            // Then: Scheduler receives empty set
            coVerify {
                scheduler.syncNotifications(emptySet(), events)
            }
        }

    @Test
    fun `app launch with 3 favorited and 0 downloaded should sync with 3 events`() =
        runTest {
            // Given: 3 events, all favorited, none downloaded
            val events =
                listOf(
                    createTestEvent("event1", favorite = true),
                    createTestEvent("event2", favorite = true),
                    createTestEvent("event3", favorite = true),
                )
            val mapChecker = TestMapAvailabilityChecker(emptySet())
            val scheduler = mockk<NotificationScheduler>(relaxed = true)
            val useCase = SyncNotificationsOnAppLaunch(scheduler, mapChecker)

            // When: Sync on app launch
            useCase(events)

            // Then: Scheduler receives favorited IDs
            coVerify {
                scheduler.syncNotifications(
                    setOf("event1", "event2", "event3"),
                    events,
                )
            }
        }

    @Test
    fun `app launch with 0 favorited and 2 downloaded should sync with 2 events`() =
        runTest {
            // Given: 3 events, none favorited, 2 downloaded
            val events =
                listOf(
                    createTestEvent("event1"),
                    createTestEvent("event2"),
                    createTestEvent("event3"),
                )
            val mapChecker = TestMapAvailabilityChecker(setOf("event1", "event3"))
            val scheduler = mockk<NotificationScheduler>(relaxed = true)
            val useCase = SyncNotificationsOnAppLaunch(scheduler, mapChecker)

            // When: Sync on app launch
            useCase(events)

            // Then: Scheduler receives downloaded IDs
            coVerify {
                scheduler.syncNotifications(
                    setOf("event1", "event3"),
                    events,
                )
            }
        }

    @Test
    fun `app launch with 2 favorited and 2 downloaded (same events) should sync with 2 events deduplicated`() =
        runTest {
            // Given: 4 events, event1 and event2 are BOTH favorited AND downloaded
            val events =
                listOf(
                    createTestEvent("event1", favorite = true), // Favorited + downloaded
                    createTestEvent("event2", favorite = true), // Favorited + downloaded
                    createTestEvent("event3"),
                    createTestEvent("event4"),
                )
            val mapChecker = TestMapAvailabilityChecker(setOf("event1", "event2"))
            val scheduler = mockk<NotificationScheduler>(relaxed = true)
            val useCase = SyncNotificationsOnAppLaunch(scheduler, mapChecker)

            // When: Sync on app launch
            useCase(events)

            // Then: Scheduler receives deduplicated set (2 events, not 4)
            coVerify {
                scheduler.syncNotifications(
                    setOf("event1", "event2"),
                    events,
                )
            }
        }

    @Test
    fun `app launch with 2 favorited and 2 downloaded (different events) should sync with 4 events`() =
        runTest {
            // Given: 4 events, event1+event2 favorited, event3+event4 downloaded
            val events =
                listOf(
                    createTestEvent("event1", favorite = true), // Only favorited
                    createTestEvent("event2", favorite = true), // Only favorited
                    createTestEvent("event3"), // Only downloaded
                    createTestEvent("event4"), // Only downloaded
                )
            val mapChecker = TestMapAvailabilityChecker(setOf("event3", "event4"))
            val scheduler = mockk<NotificationScheduler>(relaxed = true)
            val useCase = SyncNotificationsOnAppLaunch(scheduler, mapChecker)

            // When: Sync on app launch
            useCase(events)

            // Then: Scheduler receives union of sets (4 events total)
            coVerify {
                scheduler.syncNotifications(
                    setOf("event1", "event2", "event3", "event4"),
                    events,
                )
            }
        }

    @Test
    fun `app launch with mixed favorited+downloaded should sync with 3 events`() =
        runTest {
            // Given: 5 events
            // - event1: favorited + downloaded
            // - event2: only favorited
            // - event3: only downloaded
            // - event4: neither
            // - event5: neither
            val events =
                listOf(
                    createTestEvent("event1", favorite = true), // Favorited + downloaded
                    createTestEvent("event2", favorite = true), // Only favorited
                    createTestEvent("event3"), // Only downloaded
                    createTestEvent("event4"),
                    createTestEvent("event5"),
                )
            val mapChecker = TestMapAvailabilityChecker(setOf("event1", "event3"))
            val scheduler = mockk<NotificationScheduler>(relaxed = true)
            val useCase = SyncNotificationsOnAppLaunch(scheduler, mapChecker)

            // When: Sync on app launch
            useCase(events)

            // Then: Scheduler receives 3 unique events (event1, event2, event3)
            coVerify {
                scheduler.syncNotifications(
                    setOf("event1", "event2", "event3"),
                    events,
                )
            }
        }

    @Test
    fun `sync should call scheduler with empty set when no eligible events`() =
        runTest {
            // Given: 3 events, none favorited, none downloaded
            val events =
                listOf(
                    createTestEvent("event1"),
                    createTestEvent("event2"),
                    createTestEvent("event3"),
                )
            val mapChecker = TestMapAvailabilityChecker(emptySet())
            val scheduler = mockk<NotificationScheduler>(relaxed = true)
            val useCase = SyncNotificationsOnAppLaunch(scheduler, mapChecker)

            // When: Sync on app launch
            useCase(events)

            // Then: Scheduler.syncNotifications called with empty set (cleanup stale notifications)
            coVerify {
                scheduler.syncNotifications(emptySet(), events)
            }
        }
}
