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

import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.events.WWWEvent
import com.worldwidewaves.shared.events.WWWEventArea
import com.worldwidewaves.shared.events.WWWEventObserver
import com.worldwidewaves.shared.events.WWWEventWave
import com.worldwidewaves.shared.events.WWWEventWaveWarming
import dev.icerock.moko.resources.StringResource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/**
 * Tests for NotificationContentProvider.
 *
 * ## Coverage
 * - generateStartingNotification for all standard intervals (1h, 30m, 10m, 5m, 1m)
 * - generateFinishedNotification
 * - generateWaveHitNotification
 * - Deep link format validation
 * - Localization key mapping
 * - Body argument formatting
 */
class NotificationContentProviderTest {
    /**
     * Mock event for testing notification content generation.
     */
    private class MockEvent(
        override val id: String = "test-event-123",
        private val locationName: String = "New York",
    ) : IWWWEvent {
        override val type: String = "test"
        override val country: String? = "US"
        override val community: String? = null
        override val timeZone: String = "America/New_York"
        override val date: String = "2025-01-15"
        override val startHour: String = "14:00"
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

        override fun getStartDateTime(): Instant = Instant.fromEpochMilliseconds(1000000)

        override suspend fun getStatus(): IWWWEvent.Status = IWWWEvent.Status.UNDEFINED

        override suspend fun isDone(): Boolean = false

        override fun isSoon(): Boolean = false

        override suspend fun isRunning(): Boolean = false

        override fun isNearTime(): Boolean = false

        override fun getLocationImage(): Any? = null

        override fun getCommunityImage(): Any? = null

        override fun getCountryImage(): Any? = null

        override fun getMapImage(): Any? = null

        override fun getLocation(): StringResource =
            object : StringResource {
                override val resourceId: String = locationName
            }

        override fun getDescription(): StringResource = throw NotImplementedError("Mock method")

        override fun getLiteralCountry(): StringResource = throw NotImplementedError("Mock method")

        override fun getLiteralCommunity(): StringResource = throw NotImplementedError("Mock method")

        override fun getTZ(): kotlinx.datetime.TimeZone = kotlinx.datetime.TimeZone.UTC

        override suspend fun getTotalTime(): Duration = 60.minutes

        override suspend fun getEndDateTime(): Instant = getStartDateTime() + 60.minutes

        override fun getLiteralTimezone(): String = "UTC"

        override fun getLiteralStartDateSimple(): String = "2025-01-15"

        override fun getLiteralStartTime(): String = "14:00"

        override suspend fun getLiteralEndTime(): String = "15:00"

        override suspend fun getLiteralTotalTime(): String = "60m"

        override fun getWaveStartDateTime(): Instant = getStartDateTime()

        override fun getWarmingDuration(): Duration = 5.minutes

        override suspend fun getAllNumbers(): IWWWEvent.WaveNumbersLiterals = IWWWEvent.WaveNumbersLiterals()

        override fun getEventObserver(): WWWEventObserver = throw NotImplementedError("Mock method")

        override fun validationErrors(): List<String>? = null
    }

    private val contentProvider = DefaultNotificationContentProvider()
    private val testEvent = MockEvent(id = "event-123", locationName = "San Francisco")

    // ========================================
    // Test 1: Starting Notification - 1 Hour
    // ========================================
    @Test
    fun `generateStartingNotification should use correct localization key for 1 hour`() {
        // ARRANGE: 1 hour before start
        val timeUntilStart = 1.hours

        // ACT: Generate content
        val content = contentProvider.generateStartingNotification(testEvent, timeUntilStart)

        // ASSERT: Should use correct localization keys
        assertEquals("notification_event_starting_soon", content.titleKey)
        assertEquals("notification_1h_before", content.bodyKey)
    }

    // ========================================
    // Test 2: Starting Notification - 30 Minutes
    // ========================================
    @Test
    fun `generateStartingNotification should use correct localization key for 30 minutes`() {
        // ARRANGE: 30 minutes before start
        val timeUntilStart = 30.minutes

        // ACT: Generate content
        val content = contentProvider.generateStartingNotification(testEvent, timeUntilStart)

        // ASSERT: Should use correct localization keys
        assertEquals("notification_event_starting_soon", content.titleKey)
        assertEquals("notification_30m_before", content.bodyKey)
    }

    // ========================================
    // Test 3: Starting Notification - 10 Minutes
    // ========================================
    @Test
    fun `generateStartingNotification should use correct localization key for 10 minutes`() {
        // ARRANGE: 10 minutes before start
        val timeUntilStart = 10.minutes

        // ACT: Generate content
        val content = contentProvider.generateStartingNotification(testEvent, timeUntilStart)

        // ASSERT: Should use correct localization keys
        assertEquals("notification_event_starting_soon", content.titleKey)
        assertEquals("notification_10m_before", content.bodyKey)
    }

    // ========================================
    // Test 4: Starting Notification - 5 Minutes
    // ========================================
    @Test
    fun `generateStartingNotification should use correct localization key for 5 minutes`() {
        // ARRANGE: 5 minutes before start
        val timeUntilStart = 5.minutes

        // ACT: Generate content
        val content = contentProvider.generateStartingNotification(testEvent, timeUntilStart)

        // ASSERT: Should use correct localization keys
        assertEquals("notification_event_starting_soon", content.titleKey)
        assertEquals("notification_5m_before", content.bodyKey)
    }

    // ========================================
    // Test 5: Starting Notification - 1 Minute
    // ========================================
    @Test
    fun `generateStartingNotification should use correct localization key for 1 minute`() {
        // ARRANGE: 1 minute before start
        val timeUntilStart = 1.minutes

        // ACT: Generate content
        val content = contentProvider.generateStartingNotification(testEvent, timeUntilStart)

        // ASSERT: Should use correct localization keys
        assertEquals("notification_event_starting_soon", content.titleKey)
        assertEquals("notification_1m_before", content.bodyKey)
    }

    // ========================================
    // Test 6: Starting Notification - Fallback for Unknown Duration
    // ========================================
    @Test
    fun `generateStartingNotification should use fallback for non-standard durations`() {
        // ARRANGE: Non-standard duration (15 minutes)
        val timeUntilStart = 15.minutes

        // ACT: Generate content
        val content = contentProvider.generateStartingNotification(testEvent, timeUntilStart)

        // ASSERT: Should use fallback body key
        assertEquals("notification_event_starting_soon", content.titleKey)
        assertEquals("notification_event_starting_soon", content.bodyKey)
    }

    // ========================================
    // Test 7: Starting Notification - Body Args Include Location
    // ========================================
    @Test
    fun `generateStartingNotification should include location in body args`() {
        // ARRANGE: Standard duration
        val timeUntilStart = 1.hours

        // ACT: Generate content
        val content = contentProvider.generateStartingNotification(testEvent, timeUntilStart)

        // ASSERT: Body args should contain location name
        assertEquals(1, content.bodyArgs.size)
        assertEquals("San Francisco", content.bodyArgs[0])
    }

    // ========================================
    // Test 8: Starting Notification - Deep Link Format
    // ========================================
    @Test
    fun `generateStartingNotification should include correct deep link`() {
        // ARRANGE: Standard duration
        val timeUntilStart = 1.hours

        // ACT: Generate content
        val content = contentProvider.generateStartingNotification(testEvent, timeUntilStart)

        // ASSERT: Deep link should match format
        assertEquals("worldwidewaves://event?id=event-123", content.deepLink)
    }

    // ========================================
    // Test 9: Finished Notification - Localization Keys
    // ========================================
    @Test
    fun `generateFinishedNotification should use correct localization keys`() {
        // ACT: Generate content
        val content = contentProvider.generateFinishedNotification(testEvent)

        // ASSERT: Should use correct localization keys
        assertEquals("notification_event_finished", content.titleKey)
        assertEquals("notification_event_finished_body", content.bodyKey)
    }

    // ========================================
    // Test 10: Finished Notification - Body Args
    // ========================================
    @Test
    fun `generateFinishedNotification should include location in body args`() {
        // ACT: Generate content
        val content = contentProvider.generateFinishedNotification(testEvent)

        // ASSERT: Body args should contain location name
        assertEquals(1, content.bodyArgs.size)
        assertEquals("San Francisco", content.bodyArgs[0])
    }

    // ========================================
    // Test 11: Finished Notification - Deep Link
    // ========================================
    @Test
    fun `generateFinishedNotification should include correct deep link`() {
        // ACT: Generate content
        val content = contentProvider.generateFinishedNotification(testEvent)

        // ASSERT: Deep link should match format
        assertEquals("worldwidewaves://event?id=event-123", content.deepLink)
    }

    // ========================================
    // Test 12: Wave Hit Notification - Localization Keys
    // ========================================
    @Test
    fun `generateWaveHitNotification should use correct localization keys`() {
        // ACT: Generate content
        val content = contentProvider.generateWaveHitNotification(testEvent)

        // ASSERT: Should use correct localization keys
        assertEquals("notification_wave_hit", content.titleKey)
        assertEquals("notification_wave_hit_body", content.bodyKey)
    }

    // ========================================
    // Test 13: Wave Hit Notification - Body Args
    // ========================================
    @Test
    fun `generateWaveHitNotification should include location in body args`() {
        // ACT: Generate content
        val content = contentProvider.generateWaveHitNotification(testEvent)

        // ASSERT: Body args should contain location name
        assertEquals(1, content.bodyArgs.size)
        assertEquals("San Francisco", content.bodyArgs[0])
    }

    // ========================================
    // Test 14: Wave Hit Notification - Deep Link
    // ========================================
    @Test
    fun `generateWaveHitNotification should include correct deep link`() {
        // ACT: Generate content
        val content = contentProvider.generateWaveHitNotification(testEvent)

        // ASSERT: Deep link should match format
        assertEquals("worldwidewaves://event?id=event-123", content.deepLink)
    }

    // ========================================
    // Test 15: All Notifications - Deep Link Format Consistency
    // ========================================
    @Test
    fun `all notification types should use consistent deep link format`() {
        // ACT: Generate all notification types
        val startingContent = contentProvider.generateStartingNotification(testEvent, 1.hours)
        val finishedContent = contentProvider.generateFinishedNotification(testEvent)
        val waveHitContent = contentProvider.generateWaveHitNotification(testEvent)

        // ASSERT: All should have same deep link format
        val expectedDeepLink = "worldwidewaves://event?id=event-123"
        assertEquals(expectedDeepLink, startingContent.deepLink)
        assertEquals(expectedDeepLink, finishedContent.deepLink)
        assertEquals(expectedDeepLink, waveHitContent.deepLink)
    }

    // ========================================
    // Test 16: Localization Keys - No Hardcoded Strings
    // ========================================
    @Test
    fun `all notification content should use localization keys not hardcoded strings`() {
        // ACT: Generate all notification types
        val startingContent = contentProvider.generateStartingNotification(testEvent, 1.hours)
        val finishedContent = contentProvider.generateFinishedNotification(testEvent)
        val waveHitContent = contentProvider.generateWaveHitNotification(testEvent)

        // ASSERT: All keys should start with "notification_" prefix
        assertTrue(startingContent.titleKey.startsWith("notification_"))
        assertTrue(startingContent.bodyKey.startsWith("notification_"))
        assertTrue(finishedContent.titleKey.startsWith("notification_"))
        assertTrue(finishedContent.bodyKey.startsWith("notification_"))
        assertTrue(waveHitContent.titleKey.startsWith("notification_"))
        assertTrue(waveHitContent.bodyKey.startsWith("notification_"))
    }

    // ========================================
    // Test 17: Different Event IDs - Deep Link Uniqueness
    // ========================================
    @Test
    fun `different events should generate different deep links`() {
        // ARRANGE: Two different events
        val event1 = MockEvent(id = "event-001", locationName = "Paris")
        val event2 = MockEvent(id = "event-002", locationName = "Tokyo")

        // ACT: Generate content for both
        val content1 = contentProvider.generateStartingNotification(event1, 1.hours)
        val content2 = contentProvider.generateStartingNotification(event2, 1.hours)

        // ASSERT: Deep links should be different
        assertEquals("worldwidewaves://event?id=event-001", content1.deepLink)
        assertEquals("worldwidewaves://event?id=event-002", content2.deepLink)
        assert(content1.deepLink != content2.deepLink)
    }

    // ========================================
    // Test 18: Different Locations - Body Args Uniqueness
    // ========================================
    @Test
    fun `different event locations should generate different body args`() {
        // ARRANGE: Two events with different locations
        val event1 = MockEvent(id = "event-001", locationName = "London")
        val event2 = MockEvent(id = "event-002", locationName = "Berlin")

        // ACT: Generate content for both
        val content1 = contentProvider.generateStartingNotification(event1, 1.hours)
        val content2 = contentProvider.generateStartingNotification(event2, 1.hours)

        // ASSERT: Body args should reflect different locations
        assertEquals("London", content1.bodyArgs[0])
        assertEquals("Berlin", content2.bodyArgs[0])
        assert(content1.bodyArgs[0] != content2.bodyArgs[0])
    }
}
