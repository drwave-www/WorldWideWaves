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

import kotlin.time.Duration

/**
 * Platform-agnostic interface for scheduling and delivering notifications.
 *
 * ## Purpose
 * Provides a unified API for notification management across Android and iOS:
 * - Schedule future notifications (time-based triggers)
 * - Deliver immediate notifications (wave hit)
 * - Cancel scheduled notifications
 *
 * ## Platform Implementations
 * - **Android**: `AndroidNotificationManager` using WorkManager for scheduled notifications
 * - **iOS**: `IOSNotificationManager` using UNUserNotificationCenter
 *
 * ## Notification Limits
 * - **iOS**: 64 pending notifications max
 * - **Android**: ~500 pending notifications (varies by OEM)
 * - **Mitigation**: Favorites-only keeps volume low (~60 max per user)
 *
 * ## Thread Safety
 * All methods are suspend functions and must be called from appropriate coroutine contexts.
 * Platform implementations handle thread-safety internally.
 *
 * ## Usage Example
 * ```kotlin
 * class EventNotificationScheduler(
 *     private val notificationManager: NotificationManager,
 *     private val contentProvider: NotificationContentProvider
 * ) {
 *     suspend fun scheduleNotificationsForEvent(event: IWWWEvent) {
 *         val triggers = listOf(
 *             NotificationTrigger.EventStarting(1.hours),
 *             NotificationTrigger.EventStarting(30.minutes)
 *         )
 *
 *         triggers.forEach { trigger ->
 *             val delay = calculateDelay(event, trigger)
 *             val content = contentProvider.generateContent(event, trigger)
 *             notificationManager.scheduleNotification(event.id, trigger, delay, content)
 *         }
 *     }
 * }
 * ```
 *
 * @see AndroidNotificationManager for Android implementation
 * @see IOSNotificationManager for iOS implementation
 */
interface NotificationManager {
    /**
     * Schedules a notification to be delivered after the specified delay.
     *
     * ## Behavior
     * - Creates a platform-specific notification request
     * - Schedules delivery for `now() + delay`
     * - Notification persists across app restarts (WorkManager/UNUserNotificationCenter)
     *
     * ## Platform Details
     * - **Android**: Enqueues WorkManager OneTimeWorkRequest
     * - **iOS**: Creates UNNotificationRequest with UNTimeIntervalNotificationTrigger
     *
     * ## Duplicate Handling
     * If a notification with the same `eventId + trigger.id` already exists:
     * - Android: `ExistingWorkPolicy.REPLACE` updates the notification
     * - iOS: New request replaces old request with same identifier
     *
     * ## Limits
     * - iOS: Will silently fail if 64 pending notifications exceeded
     * - Android: More lenient but still has limits (~500)
     *
     * @param eventId Unique event identifier
     * @param trigger Type of notification trigger
     * @param delay Duration until notification should fire
     * @param content Notification content (localization keys + deep link)
     */
    suspend fun scheduleNotification(
        eventId: String,
        trigger: NotificationTrigger,
        delay: Duration,
        content: NotificationContent,
    )

    /**
     * Delivers a notification immediately.
     *
     * ## Use Case
     * Wave hit detection when app is open/backgrounded:
     * ```kotlin
     * if (waveHitsUser && favoriteStore.isFavorite(eventId)) {
     *     notificationManager.deliverNow(eventId, NotificationTrigger.WaveHit, content)
     * }
     * ```
     *
     * ## Platform Details
     * - **Android**: Shows notification immediately via NotificationManager
     * - **iOS**: Schedules with 0.1s delay trigger (closest to immediate)
     *
     * ## Permissions
     * Requires notification permission granted:
     * - Android 13+: POST_NOTIFICATIONS permission
     * - iOS: Authorization status .authorized
     *
     * If permission denied, notification will silently fail.
     *
     * @param eventId Unique event identifier
     * @param trigger Type of notification (typically WaveHit)
     * @param content Notification content (localization keys + deep link)
     */
    suspend fun deliverNow(
        eventId: String,
        trigger: NotificationTrigger,
        content: NotificationContent,
    )

    /**
     * Cancels a specific notification for an event.
     *
     * ## Use Case
     * ```kotlin
     * // User unfavorites event - cancel all notifications
     * NotificationTrigger.entries.forEach { trigger ->
     *     notificationManager.cancelNotification(eventId, trigger)
     * }
     * ```
     *
     * ## Platform Details
     * - **Android**: Cancels WorkManager work by unique name
     * - **iOS**: Removes pending notification request by identifier
     *
     * ## Safety
     * Safe to call even if notification doesn't exist (no-op).
     *
     * @param eventId Unique event identifier
     * @param trigger Notification trigger to cancel
     */
    suspend fun cancelNotification(
        eventId: String,
        trigger: NotificationTrigger,
    )

    /**
     * Cancels all notifications for an event.
     *
     * ## Use Case
     * ```kotlin
     * // User unfavorites event
     * notificationManager.cancelAllNotifications(eventId)
     *
     * // Event cancelled in Firestore (detected on app launch)
     * if (event == null || event.isCancelled) {
     *     notificationManager.cancelAllNotifications(eventId)
     * }
     * ```
     *
     * ## Platform Details
     * - **Android**: Cancels all WorkManager tasks tagged with `event_$eventId`
     * - **iOS**: Removes all pending notification requests matching `event_$eventId_*`
     *
     * ## Performance
     * More efficient than calling `cancelNotification()` for each trigger individually.
     *
     * @param eventId Unique event identifier
     */
    suspend fun cancelAllNotifications(eventId: String)
}

/**
 * Factory function for creating platform-specific NotificationManager instances.
 *
 * ## Platform Resolution
 * Uses expect/actual pattern to create correct implementation:
 * - Android: Returns `AndroidNotificationManager`
 * - iOS: Returns `IOSNotificationManager`
 *
 * ## Usage (in Koin module)
 * ```kotlin
 * single<NotificationManager> { createPlatformNotificationManager() }
 * ```
 *
 * @return Platform-specific NotificationManager implementation
 */
expect fun createPlatformNotificationManager(): NotificationManager
