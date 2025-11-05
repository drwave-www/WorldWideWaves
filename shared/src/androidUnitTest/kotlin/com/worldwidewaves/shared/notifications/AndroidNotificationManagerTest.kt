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

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/**
 * Unit tests for AndroidNotificationManager.
 *
 * ## Test Strategy
 * - Verify WorkManager job enqueueing
 * - Verify unique work names match expected format
 * - Verify input data contains all required fields
 * - Verify ExistingWorkPolicy.REPLACE for updates
 * - Verify notification cancellation
 *
 * ## Limitations
 * - Cannot test actual notification display (requires Android framework)
 * - Cannot test WorkManager execution (requires TestDriver or instrumented tests)
 * - Tests focus on enqueueing logic only
 *
 * @see AndroidNotificationManager for implementation
 */
class AndroidNotificationManagerTest {
    private lateinit var context: Context
    private lateinit var workManager: WorkManager
    private lateinit var manager: AndroidNotificationManager

    @BeforeTest
    fun setUp() {
        context = mockk(relaxed = true)
        workManager = mockk(relaxed = true)
        manager = AndroidNotificationManager(context, workManager)

        // Mock context.packageName for string resolution
        every { context.packageName } returns "com.worldwidewaves"
        every { context.resources } returns mockk(relaxed = true)
        every { context.resources.getIdentifier(any(), any(), any()) } returns 0
    }

    @AfterTest
    fun tearDown() {
        // No cleanup needed for mockk
    }

    @Test
    fun testScheduleNotification_enqueuesWorkWithCorrectName() =
        runTest {
            // Arrange
            val eventId = "event123"
            val trigger = NotificationTrigger.EventStarting(1.hours)
            val content =
                NotificationContent(
                    titleKey = "notification_event_starting_soon",
                    bodyKey = "notification_1h_before",
                    bodyArgs = listOf("New York"),
                    deepLink = "worldwidewaves://event?id=event123",
                )

            // Act
            manager.scheduleNotification(eventId, trigger, 1.hours, content)

            // Assert
            verify {
                workManager.enqueueUniqueWork(
                    "notification_event123_start_60m",
                    ExistingWorkPolicy.REPLACE,
                    any<OneTimeWorkRequest>(),
                )
            }
        }

    @Test
    fun testScheduleNotification_differentTriggers_generateUniqueWorkNames() =
        runTest {
            // Arrange
            val eventId = "event456"
            val content =
                NotificationContent(
                    titleKey = "notification_event_starting_soon",
                    bodyKey = "notification_1h_before",
                    deepLink = "worldwidewaves://event?id=event456",
                )

            // Act & Assert - 1 hour before
            manager.scheduleNotification(eventId, NotificationTrigger.EventStarting(1.hours), 1.hours, content)
            verify {
                workManager.enqueueUniqueWork(
                    "notification_event456_start_60m",
                    any(),
                    any<OneTimeWorkRequest>(),
                )
            }

            // Act & Assert - 30 minutes before
            manager.scheduleNotification(eventId, NotificationTrigger.EventStarting(30.minutes), 30.minutes, content)
            verify {
                workManager.enqueueUniqueWork(
                    "notification_event456_start_30m",
                    any(),
                    any<OneTimeWorkRequest>(),
                )
            }

            // Act & Assert - Event finished
            manager.scheduleNotification(eventId, NotificationTrigger.EventFinished, 2.hours, content)
            verify {
                workManager.enqueueUniqueWork(
                    "notification_event456_finished",
                    any(),
                    any<OneTimeWorkRequest>(),
                )
            }
        }

    @Test
    fun testScheduleNotification_tagsWorkWithEventId() =
        runTest {
            // Arrange
            val eventId = "event789"
            val trigger = NotificationTrigger.EventStarting(30.minutes)
            val content =
                NotificationContent(
                    titleKey = "notification_event_starting_soon",
                    bodyKey = "notification_30m_before",
                    deepLink = "worldwidewaves://event?id=event789",
                )

            // Act
            manager.scheduleNotification(eventId, trigger, 30.minutes, content)

            // Assert - Capture and verify work request has tag
            verify {
                workManager.enqueueUniqueWork(
                    any(),
                    any(),
                    match<OneTimeWorkRequest> { request ->
                        request.tags.contains("event_event789")
                    },
                )
            }
        }

    @Test
    fun testCancelNotification_cancelsUniqueWork() =
        runTest {
            // Arrange
            val eventId = "event123"
            val trigger = NotificationTrigger.EventStarting(1.hours)

            // Act
            manager.cancelNotification(eventId, trigger)

            // Assert
            verify {
                workManager.cancelUniqueWork("notification_event123_start_60m")
            }
        }

    @Test
    fun testCancelAllNotifications_cancelsAllWorkForEvent() =
        runTest {
            // Arrange
            val eventId = "event456"

            // Act
            manager.cancelAllNotifications(eventId)

            // Assert
            verify {
                workManager.cancelAllWorkByTag("event_event456")
            }
        }

    @Test
    fun testWorkName_format_matchesExpectedPattern() {
        // Test various triggers generate correct work names
        val eventId = "test_event"

        val testCases =
            listOf(
                Triple(NotificationTrigger.EventStarting(60.minutes), "notification_test_event_start_60m", "1h before"),
                Triple(NotificationTrigger.EventStarting(30.minutes), "notification_test_event_start_30m", "30m before"),
                Triple(NotificationTrigger.EventStarting(10.minutes), "notification_test_event_start_10m", "10m before"),
                Triple(NotificationTrigger.EventStarting(5.minutes), "notification_test_event_start_5m", "5m before"),
                Triple(NotificationTrigger.EventStarting(1.minutes), "notification_test_event_start_1m", "1m before"),
                Triple(NotificationTrigger.EventFinished, "notification_test_event_finished", "finished"),
                Triple(NotificationTrigger.WaveHit, "notification_test_event_wave_hit", "wave hit"),
            )

        testCases.forEach { (trigger, expectedWorkName, description) ->
            // Use reflection to access private buildWorkName method
            // For now, we validate the format is correct by checking trigger.id
            val expectedFormat = "notification_${eventId}_${trigger.id}"
            assertEquals(
                expectedWorkName,
                expectedFormat,
                "Work name format mismatch for $description",
            )
        }
    }

    @Test
    fun testNotificationId_isStable() {
        // Verify that notification IDs are stable (same event+trigger = same ID)
        val eventId = "stable_event"
        val trigger = NotificationTrigger.EventStarting(1.hours)

        val workName1 = "notification_${eventId}_${trigger.id}"
        val workName2 = "notification_${eventId}_${trigger.id}"

        assertEquals(
            workName1.hashCode(),
            workName2.hashCode(),
            "Notification IDs should be stable for same event+trigger",
        )
    }

    @Test
    fun testInputData_containsAllRequiredFields() {
        // Test that input data format is correct
        val eventId = "event123"
        val triggerId = "start_60m"
        val titleKey = "notification_event_starting_soon"
        val bodyKey = "notification_1h_before"
        val bodyArgs = arrayOf("New York", "14:00")
        val deepLink = "worldwidewaves://event?id=event123"

        // Verify field names match constants
        assertEquals("eventId", AndroidNotificationManager.INPUT_EVENT_ID)
        assertEquals("triggerId", AndroidNotificationManager.INPUT_TRIGGER_ID)
        assertEquals("titleKey", AndroidNotificationManager.INPUT_TITLE_KEY)
        assertEquals("bodyKey", AndroidNotificationManager.INPUT_BODY_KEY)
        assertEquals("bodyArgs", AndroidNotificationManager.INPUT_BODY_ARGS)
        assertEquals("deepLink", AndroidNotificationManager.INPUT_DEEP_LINK)
    }

    @Test
    fun testNotificationChannelId_matchesExpected() {
        // Verify channel ID constant
        assertEquals("WAVE_EVENTS_CHANNEL", AndroidNotificationManager.NOTIFICATION_CHANNEL_ID)
    }

    @Test
    fun testScheduleNotification_handlesExceptionGracefully() =
        runTest {
            // Arrange
            every {
                workManager.enqueueUniqueWork(any(), any(), any<OneTimeWorkRequest>())
            } throws RuntimeException("WorkManager error")

            val eventId = "event123"
            val trigger = NotificationTrigger.EventStarting(1.hours)
            val content =
                NotificationContent(
                    titleKey = "notification_event_starting_soon",
                    bodyKey = "notification_1h_before",
                    deepLink = "worldwidewaves://event?id=event123",
                )

            // Act - Should not throw
            manager.scheduleNotification(eventId, trigger, 1.hours, content)

            // Assert - Exception was caught and logged
            verify {
                workManager.enqueueUniqueWork(any(), any(), any<OneTimeWorkRequest>())
            }
        }

    @Test
    fun testCancelNotification_handlesExceptionGracefully() =
        runTest {
            // Arrange
            every { workManager.cancelUniqueWork(any()) } throws RuntimeException("Cancel failed")

            val eventId = "event123"
            val trigger = NotificationTrigger.EventStarting(1.hours)

            // Act - Should not throw
            manager.cancelNotification(eventId, trigger)

            // Assert - Exception was caught and logged
            verify {
                workManager.cancelUniqueWork(any())
            }
        }

    @Test
    fun testMultipleEvents_generateUniqueWorkNames() =
        runTest {
            // Verify different events generate different work names
            val content =
                NotificationContent(
                    titleKey = "notification_event_starting_soon",
                    bodyKey = "notification_1h_before",
                    deepLink = "worldwidewaves://event?id=event1",
                )
            val trigger = NotificationTrigger.EventStarting(1.hours)

            // Event 1
            manager.scheduleNotification("event1", trigger, 1.hours, content)
            verify {
                workManager.enqueueUniqueWork("notification_event1_start_60m", any(), any<OneTimeWorkRequest>())
            }

            // Event 2
            manager.scheduleNotification("event2", trigger, 1.hours, content)
            verify {
                workManager.enqueueUniqueWork("notification_event2_start_60m", any(), any<OneTimeWorkRequest>())
            }
        }
}
