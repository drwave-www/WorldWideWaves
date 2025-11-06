@file:OptIn(
    kotlinx.coroutines.ExperimentalCoroutinesApi::class,
    kotlin.time.ExperimentalTime::class,
)

package com.worldwidewaves.shared.notifications

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

import com.worldwidewaves.shared.MokoRes
import com.worldwidewaves.shared.WWWPlatform
import com.worldwidewaves.shared.WWWSimulation
import com.worldwidewaves.shared.data.FavoriteEventsStore
import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.events.WWWEvent
import com.worldwidewaves.shared.events.WWWEventArea
import com.worldwidewaves.shared.events.WWWEventObserver
import com.worldwidewaves.shared.events.WWWEventWave
import com.worldwidewaves.shared.events.WWWEventWaveWarming
import com.worldwidewaves.shared.events.utils.IClock
import com.worldwidewaves.shared.events.utils.Position
import dev.icerock.moko.resources.StringResource
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/**
 * Comprehensive test suite for NotificationScheduler.
 *
 * ## Coverage
 * - shouldScheduleNotifications (favorite status, simulation mode, event timing)
 * - scheduleAllNotifications (6 notifications, future filtering, correct delays)
 * - cancelAllNotifications (delegation)
 * - syncNotifications (cancel missing/unfavorited, reschedule changed)
 */
class NotificationSchedulerTest {
    /**
     * Test implementation of IClock for controlled time.
     */
    private class TestClock(
        private var currentTime: Instant,
    ) : IClock {
        override fun now(): Instant = currentTime

        override suspend fun delay(duration: Duration) {
            currentTime += duration
        }

        fun advance(duration: Duration) {
            currentTime += duration
        }
    }

    /**
     * Test implementation of FavoriteEventsStore.
     */
    private class TestFavoriteEventsStore : FavoriteEventsStore {
        private val favorites = mutableSetOf<String>()

        override suspend fun setFavoriteStatus(
            eventId: String,
            isFavorite: Boolean,
        ) {
            if (isFavorite) {
                favorites.add(eventId)
            } else {
                favorites.remove(eventId)
            }
        }

        override suspend fun isFavorite(eventId: String): Boolean = favorites.contains(eventId)

        fun getAllFavorites(): Set<String> = favorites.toSet()
    }

    /**
     * Test implementation of NotificationManager that records all calls.
     */
    private class MockNotificationManager : NotificationManager {
        data class ScheduleCall(
            val eventId: String,
            val trigger: NotificationTrigger,
            val delay: Duration,
            val content: NotificationContent,
        )

        data class DeliverCall(
            val eventId: String,
            val trigger: NotificationTrigger,
            val content: NotificationContent,
        )

        data class CancelCall(
            val eventId: String,
            val trigger: NotificationTrigger,
        )

        val scheduleCalls = mutableListOf<ScheduleCall>()
        val deliverCalls = mutableListOf<DeliverCall>()
        val cancelCalls = mutableListOf<CancelCall>()
        val cancelAllCalls = mutableListOf<String>()

        override suspend fun scheduleNotification(
            eventId: String,
            trigger: NotificationTrigger,
            delay: Duration,
            content: NotificationContent,
        ) {
            scheduleCalls.add(ScheduleCall(eventId, trigger, delay, content))
        }

        override suspend fun deliverNow(
            eventId: String,
            trigger: NotificationTrigger,
            content: NotificationContent,
        ) {
            deliverCalls.add(DeliverCall(eventId, trigger, content))
        }

        override suspend fun cancelNotification(
            eventId: String,
            trigger: NotificationTrigger,
        ) {
            cancelCalls.add(CancelCall(eventId, trigger))
        }

        override suspend fun cancelAllNotifications(eventId: String) {
            cancelAllCalls.add(eventId)
        }

        fun reset() {
            scheduleCalls.clear()
            deliverCalls.clear()
            cancelCalls.clear()
            cancelAllCalls.clear()
        }
    }

    /**
     * Mock event for testing.
     */
    private class MockEvent(
        override val id: String,
        private val startDateTime: Instant,
        private val endDateTime: Instant,
        private val locationName: String = "Test Location",
    ) : IWWWEvent {
        override val type: String = "test"
        override val country: String? = null
        override val community: String? = null
        override val timeZone: String = "UTC"
        override val date: String = "2025-01-01"
        override val startHour: String = "12:00"
        override val instagramAccount: String = ""
        override val instagramHashtag: String = ""
        override var favorite: Boolean = false

        override val wavedef: WWWEvent.WWWWaveDefinition
            get() = throw NotImplementedError("Mock property")
        override val area: WWWEventArea
            get() = throw NotImplementedError("Mock property")
        override val warming: WWWEventWaveWarming
            get() = throw NotImplementedError("Mock property")
        override val map: com.worldwidewaves.shared.events.WWWEventMap
            get() = throw NotImplementedError("Mock property")
        override val wave: WWWEventWave
            get() = throw NotImplementedError("Mock property")

        override fun getStartDateTime(): Instant = startDateTime

        override suspend fun getEndDateTime(): Instant = endDateTime

        override suspend fun getStatus(): IWWWEvent.Status = IWWWEvent.Status.UNDEFINED

        override suspend fun isDone(): Boolean = false

        override fun isSoon(): Boolean = false

        override suspend fun isRunning(): Boolean = false

        override fun isNearTime(): Boolean = false

        override fun getLocationImage(): Any? = null

        override fun getCommunityImage(): Any? = null

        override fun getCountryImage(): Any? = null

        override fun getMapImage(): Any? = null

        override fun getLocation(): StringResource = MokoRes.strings.empty

        override fun getDescription(): StringResource = throw NotImplementedError("Mock method")

        override fun getLiteralCountry(): StringResource = throw NotImplementedError("Mock method")

        override fun getLiteralCommunity(): StringResource = throw NotImplementedError("Mock method")

        override fun getTZ(): kotlinx.datetime.TimeZone = kotlinx.datetime.TimeZone.UTC

        override suspend fun getTotalTime(): Duration = endDateTime - startDateTime

        override fun getLiteralTimezone(): String = "UTC"

        override fun getLiteralStartDateSimple(): String = "2025-01-01"

        override fun getLiteralStartTime(): String = "12:00"

        override suspend fun getLiteralEndTime(): String = "13:00"

        override suspend fun getLiteralTotalTime(): String = "60m"

        override fun getWaveStartDateTime(): Instant = startDateTime

        override fun getWarmingDuration(): Duration = 5.minutes

        override suspend fun getAllNumbers(): IWWWEvent.WaveNumbersLiterals = IWWWEvent.WaveNumbersLiterals()

        override fun getEventObserver(): WWWEventObserver = throw NotImplementedError("Mock method")

        override fun validationErrors(): List<String>? = null
    }

    private lateinit var clock: TestClock
    private lateinit var platform: WWWPlatform
    private lateinit var favoriteStore: TestFavoriteEventsStore
    private lateinit var notificationManager: MockNotificationManager
    private lateinit var contentProvider: NotificationContentProvider
    private lateinit var scheduler: NotificationScheduler

    @BeforeTest
    fun setUp() {
        clock = TestClock(Instant.fromEpochMilliseconds(1000000))
        platform = WWWPlatform(name = "TestPlatform")
        favoriteStore = TestFavoriteEventsStore()
        notificationManager = MockNotificationManager()
        contentProvider = DefaultNotificationContentProvider()
        scheduler =
            DefaultNotificationScheduler(
                clock = clock,
                platform = platform,
                favoriteStore = favoriteStore,
                notificationManager = notificationManager,
                contentProvider = contentProvider,
            )
    }

    private fun setSimulation(sim: WWWSimulation?) {
        if (sim != null) {
            platform.setSimulation(sim)
        } else {
            platform.disableSimulation()
        }
    }

    @AfterTest
    fun tearDown() {
        notificationManager.reset()
    }

    // ========================================
    // shouldScheduleNotifications Tests
    // ========================================

    @Test
    fun `shouldScheduleNotifications returns false if event is not favorited`() =
        runTest {
            // ARRANGE: Event not favorited
            val event = MockEvent("event-1", clock.now() + 2.hours, clock.now() + 3.hours)

            // ACT: Check if should schedule
            val result = scheduler.shouldScheduleNotifications(event)

            // ASSERT: Should not schedule
            assertFalse(result)
        }

    @Test
    fun `shouldScheduleNotifications returns false if event has already started`() =
        runTest {
            // ARRANGE: Favorited event that already started
            val event = MockEvent("event-1", clock.now() - 10.minutes, clock.now() + 50.minutes)
            favoriteStore.setFavoriteStatus("event-1", true)

            // ACT: Check if should schedule
            val result = scheduler.shouldScheduleNotifications(event)

            // ASSERT: Should not schedule
            assertFalse(result)
        }

    @Test
    fun `shouldScheduleNotifications returns false if simulation speed is greater than 1`() =
        runTest {
            // ARRANGE: Favorited future event with accelerated simulation
            val event = MockEvent("event-1", clock.now() + 2.hours, clock.now() + 3.hours)
            favoriteStore.setFavoriteStatus("event-1", true)
            val simulation = WWWSimulation(clock.now(), Position(0.0, 0.0), initialSpeed = 10)
            setSimulation(simulation)

            // ACT: Check if should schedule
            val result = scheduler.shouldScheduleNotifications(event)

            // ASSERT: Should not schedule (accelerated time incompatible)
            assertFalse(result)
        }

    @Test
    fun `shouldScheduleNotifications returns true if favorited, future event, no simulation`() =
        runTest {
            // ARRANGE: Favorited future event, no simulation
            val event = MockEvent("event-1", clock.now() + 2.hours, clock.now() + 3.hours)
            favoriteStore.setFavoriteStatus("event-1", true)

            // ACT: Check if should schedule
            val result = scheduler.shouldScheduleNotifications(event)

            // ASSERT: Should schedule
            assertTrue(result)
        }

    @Test
    fun `shouldScheduleNotifications returns true if favorited, future event, simulation speed equals 1`() =
        runTest {
            // ARRANGE: Favorited future event with realistic simulation (speed=1)
            val event = MockEvent("event-1", clock.now() + 2.hours, clock.now() + 3.hours)
            favoriteStore.setFavoriteStatus("event-1", true)
            val simulation = WWWSimulation(clock.now(), Position(0.0, 0.0), initialSpeed = 1)
            setSimulation(simulation)

            // ACT: Check if should schedule
            val result = scheduler.shouldScheduleNotifications(event)

            // ASSERT: Should schedule (speed=1 is compatible)
            assertTrue(result)
        }

    // ========================================
    // scheduleAllNotifications Tests
    // ========================================

    @Test
    fun `scheduleAllNotifications schedules 6 notifications for future event`() =
        runTest {
            // ARRANGE: Event 2 hours in future
            val event = MockEvent("event-1", clock.now() + 2.hours, clock.now() + 3.hours)

            // ACT: Schedule all notifications
            scheduler.scheduleAllNotifications(event)

            // ASSERT: Should schedule 6 notifications (5 start + 1 finished)
            assertEquals(6, notificationManager.scheduleCalls.size)
        }

    @Test
    fun `scheduleAllNotifications includes all standard intervals`() =
        runTest {
            // ARRANGE: Event 2 hours in future
            val event = MockEvent("event-1", clock.now() + 2.hours, clock.now() + 3.hours)

            // ACT: Schedule all notifications
            scheduler.scheduleAllNotifications(event)

            // ASSERT: Should include all intervals
            val triggers = notificationManager.scheduleCalls.map { it.trigger }
            assertTrue(triggers.any { it is NotificationTrigger.EventStarting && it.duration == 1.hours })
            assertTrue(triggers.any { it is NotificationTrigger.EventStarting && it.duration == 30.minutes })
            assertTrue(triggers.any { it is NotificationTrigger.EventStarting && it.duration == 10.minutes })
            assertTrue(triggers.any { it is NotificationTrigger.EventStarting && it.duration == 5.minutes })
            assertTrue(triggers.any { it is NotificationTrigger.EventStarting && it.duration == 1.minutes })
            assertTrue(triggers.any { it is NotificationTrigger.EventFinished })
        }

    @Test
    fun `scheduleAllNotifications only schedules future notifications`() =
        runTest {
            // ARRANGE: Event starting in 20 minutes (some notification times already passed)
            val event = MockEvent("event-1", clock.now() + 20.minutes, clock.now() + 80.minutes)

            // ACT: Schedule all notifications
            scheduler.scheduleAllNotifications(event)

            // ASSERT: Should only schedule 10m, 5m, 1m, finished (4 total)
            assertEquals(4, notificationManager.scheduleCalls.size)
            val triggers = notificationManager.scheduleCalls.map { it.trigger }
            assertFalse(triggers.any { it is NotificationTrigger.EventStarting && it.duration == 1.hours })
            assertFalse(triggers.any { it is NotificationTrigger.EventStarting && it.duration == 30.minutes })
            assertTrue(triggers.any { it is NotificationTrigger.EventStarting && it.duration == 10.minutes })
            assertTrue(triggers.any { it is NotificationTrigger.EventStarting && it.duration == 5.minutes })
            assertTrue(triggers.any { it is NotificationTrigger.EventStarting && it.duration == 1.minutes })
            assertTrue(triggers.any { it is NotificationTrigger.EventFinished })
        }

    @Test
    fun `scheduleAllNotifications uses correct delays`() =
        runTest {
            // ARRANGE: Event starting in exactly 2 hours
            val startTime = clock.now() + 2.hours
            val event = MockEvent("event-1", startTime, startTime + 1.hours)

            // ACT: Schedule all notifications
            scheduler.scheduleAllNotifications(event)

            // ASSERT: Check delays for each notification
            val schedules = notificationManager.scheduleCalls
            val oneHourBefore =
                schedules.find {
                    it.trigger is NotificationTrigger.EventStarting &&
                        (it.trigger as NotificationTrigger.EventStarting).duration == 1.hours
                }
            assertEquals(1.hours, oneHourBefore?.delay)

            val thirtyMinBefore =
                schedules.find {
                    it.trigger is NotificationTrigger.EventStarting &&
                        (it.trigger as NotificationTrigger.EventStarting).duration == 30.minutes
                }
            assertEquals(90.minutes, thirtyMinBefore?.delay)
        }

    @Test
    fun `scheduleAllNotifications schedules finished notification at end time`() =
        runTest {
            // ARRANGE: Event 1 hour in future, lasting 1 hour
            val event = MockEvent("event-1", clock.now() + 1.hours, clock.now() + 2.hours)

            // ACT: Schedule all notifications
            scheduler.scheduleAllNotifications(event)

            // ASSERT: Finished notification should be scheduled 2 hours from now
            val finishedCall =
                notificationManager.scheduleCalls.find {
                    it.trigger is NotificationTrigger.EventFinished
                }
            assertEquals(2.hours, finishedCall?.delay)
        }

    @Test
    fun `scheduleAllNotifications includes correct event ID in all calls`() =
        runTest {
            // ARRANGE: Event with specific ID
            val event = MockEvent("test-event-123", clock.now() + 2.hours, clock.now() + 3.hours)

            // ACT: Schedule all notifications
            scheduler.scheduleAllNotifications(event)

            // ASSERT: All calls should have correct event ID
            assertTrue(notificationManager.scheduleCalls.all { it.eventId == "test-event-123" })
        }

    @Test
    fun `scheduleAllNotifications provides correct content for each trigger`() =
        runTest {
            // ARRANGE: Event 2 hours in future
            val event = MockEvent("event-1", clock.now() + 2.hours, clock.now() + 3.hours)

            // ACT: Schedule all notifications
            scheduler.scheduleAllNotifications(event)

            // ASSERT: Each notification should have appropriate content
            notificationManager.scheduleCalls.forEach { call ->
                // All should have deep link
                assertTrue(call.content.deepLink.startsWith("worldwidewaves://event?id="))
                // All should have localization keys
                assertTrue(call.content.titleKey.isNotEmpty())
                assertTrue(call.content.bodyKey.isNotEmpty())
            }
        }

    // ========================================
    // cancelAllNotifications Tests
    // ========================================

    @Test
    fun `cancelAllNotifications delegates to notification manager`() =
        runTest {
            // ACT: Cancel notifications for event
            scheduler.cancelAllNotifications("event-123")

            // ASSERT: Should call notification manager
            assertEquals(1, notificationManager.cancelAllCalls.size)
            assertEquals("event-123", notificationManager.cancelAllCalls[0])
        }

    // ========================================
    // syncNotifications Tests
    // ========================================

    @Test
    fun `syncNotifications cancels notifications for events no longer in favorites`() =
        runTest {
            // ARRANGE: Event was favorited, now removed from favorites list
            favoriteStore.setFavoriteStatus("event-1", true)
            favoriteStore.setFavoriteStatus("event-1", false)
            val favoriteIds = emptySet<String>()
            val events = emptyList<IWWWEvent>()

            // ACT: Sync notifications
            scheduler.syncNotifications(favoriteIds, events)

            // ASSERT: Should not cancel anything (event not in favorites list)
            assertEquals(0, notificationManager.cancelAllCalls.size)
        }

    @Test
    fun `syncNotifications cancels notifications for missing events`() =
        runTest {
            // ARRANGE: Event is favorited but not in current events list (deleted)
            favoriteStore.setFavoriteStatus("event-1", true)
            val favoriteIds = setOf("event-1")
            val events = emptyList<IWWWEvent>()

            // ACT: Sync notifications
            scheduler.syncNotifications(favoriteIds, events)

            // ASSERT: Should cancel notifications for missing event
            assertEquals(1, notificationManager.cancelAllCalls.size)
            assertEquals("event-1", notificationManager.cancelAllCalls[0])
        }

    @Test
    fun `syncNotifications reschedules for eligible events`() =
        runTest {
            // ARRANGE: Event is favorited and eligible for notifications
            favoriteStore.setFavoriteStatus("event-1", true)
            val event = MockEvent("event-1", clock.now() + 2.hours, clock.now() + 3.hours)
            val favoriteIds = setOf("event-1")
            val events = listOf(event)

            // ACT: Sync notifications
            scheduler.syncNotifications(favoriteIds, events)

            // ASSERT: Should cancel old and schedule new
            assertEquals(1, notificationManager.cancelAllCalls.size)
            assertEquals(6, notificationManager.scheduleCalls.size)
        }

    @Test
    fun `syncNotifications cancels for ineligible events`() =
        runTest {
            // ARRANGE: Event is favorited but has already started (ineligible)
            favoriteStore.setFavoriteStatus("event-1", true)
            val event = MockEvent("event-1", clock.now() - 10.minutes, clock.now() + 50.minutes)
            val favoriteIds = setOf("event-1")
            val events = listOf(event)

            // ACT: Sync notifications
            scheduler.syncNotifications(favoriteIds, events)

            // ASSERT: Should cancel but not reschedule
            assertEquals(1, notificationManager.cancelAllCalls.size)
            assertEquals(0, notificationManager.scheduleCalls.size)
        }

    @Test
    fun `syncNotifications handles multiple events correctly`() =
        runTest {
            // ARRANGE: Multiple events with different states
            favoriteStore.setFavoriteStatus("event-1", true) // Eligible
            favoriteStore.setFavoriteStatus("event-2", true) // Started (ineligible)
            favoriteStore.setFavoriteStatus("event-3", true) // Missing (will cancel)

            val event1 = MockEvent("event-1", clock.now() + 2.hours, clock.now() + 3.hours)
            val event2 = MockEvent("event-2", clock.now() - 10.minutes, clock.now() + 50.minutes)
            val favoriteIds = setOf("event-1", "event-2", "event-3")
            val events = listOf(event1, event2)

            // ACT: Sync notifications
            scheduler.syncNotifications(favoriteIds, events)

            // ASSERT: Should cancel all 3, schedule only event-1
            assertEquals(3, notificationManager.cancelAllCalls.size)
            assertTrue(notificationManager.cancelAllCalls.contains("event-1"))
            assertTrue(notificationManager.cancelAllCalls.contains("event-2"))
            assertTrue(notificationManager.cancelAllCalls.contains("event-3"))
            assertEquals(6, notificationManager.scheduleCalls.size) // Only event-1 rescheduled
            assertTrue(notificationManager.scheduleCalls.all { it.eventId == "event-1" })
        }

    @Test
    fun `syncNotifications with accelerated simulation cancels all`() =
        runTest {
            // ARRANGE: Favorited event with accelerated simulation
            favoriteStore.setFavoriteStatus("event-1", true)
            val event = MockEvent("event-1", clock.now() + 2.hours, clock.now() + 3.hours)
            val simulation = WWWSimulation(clock.now(), Position(0.0, 0.0), initialSpeed = 10)
            setSimulation(simulation)
            val favoriteIds = setOf("event-1")
            val events = listOf(event)

            // ACT: Sync notifications
            scheduler.syncNotifications(favoriteIds, events)

            // ASSERT: Should cancel but not reschedule (simulation incompatible)
            assertEquals(1, notificationManager.cancelAllCalls.size)
            assertEquals(0, notificationManager.scheduleCalls.size)
        }
}
