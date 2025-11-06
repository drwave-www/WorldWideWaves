@file:OptIn(kotlin.time.ExperimentalTime::class)

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

import com.worldwidewaves.shared.WWWPlatform
import com.worldwidewaves.shared.data.FavoriteEventsStore
import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.events.utils.IClock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/**
 * Manages notification scheduling logic for wave events.
 *
 * ## Purpose
 * Determines when and which notifications should be scheduled for favorited events:
 * - Calculates notification timing (1h, 30m, 10m, 5m, 1m before start)
 * - Checks eligibility (favorited events only)
 * - Validates simulation mode compatibility (speed == 1 only)
 * - Coordinates with NotificationManager for actual scheduling
 *
 * ## Eligibility Criteria
 * Notifications are scheduled only if:
 * 1. Event is favorited by user
 * 2. Simulation mode is off OR speed == 1 (realistic mode)
 * 3. Event hasn't started yet
 *
 * ## Simulation Mode Behavior
 * - **Real events**: Always schedule ✅
 * - **Realistic simulation (speed=1)**: Schedule (real-time compatible) ✅
 * - **Accelerated simulation (speed>1)**: Skip (time mismatch) ❌
 *
 * ## Usage
 * ```kotlin
 * class EventFavoriteHandler(
 *     private val scheduler: NotificationScheduler,
 *     private val notificationManager: NotificationManager
 * ) {
 *     suspend fun onEventFavorited(event: IWWWEvent) {
 *         if (scheduler.shouldScheduleNotifications(event)) {
 *             scheduler.scheduleAllNotifications(event)
 *         }
 *     }
 * }
 * ```
 *
 * @see NotificationManager for platform-specific scheduling
 * @see FavoriteEventsStore for favorite status
 */
interface NotificationScheduler {
    /**
     * Determines if notifications should be scheduled for an event.
     *
     * ## Eligibility Checks
     * 1. Event is favorited
     * 2. Simulation mode is compatible (off or speed == 1)
     * 3. Event hasn't started yet
     *
     * ## Example
     * ```kotlin
     * if (scheduler.shouldScheduleNotifications(event)) {
     *     // Safe to schedule
     *     scheduler.scheduleAllNotifications(event)
     * } else {
     *     // Don't schedule (unfavorited or incompatible simulation)
     * }
     * ```
     *
     * @param event The event to check
     * @return True if notifications should be scheduled
     */
    suspend fun shouldScheduleNotifications(event: IWWWEvent): Boolean

    /**
     * Schedules all time-based notifications for an event.
     *
     * ## Scheduled Notifications
     * - 1 hour before start
     * - 30 minutes before start
     * - 10 minutes before start
     * - 5 minutes before start
     * - 1 minute before start
     * - Event finished (at end time)
     *
     * Total: 6 notifications per favorited event
     *
     * ## Requirements
     * Must call `shouldScheduleNotifications()` first to verify eligibility.
     *
     * ## Example
     * ```kotlin
     * // User favorites an event
     * favoriteStore.setFavoriteStatus(event.id, true)
     * if (scheduler.shouldScheduleNotifications(event)) {
     *     scheduler.scheduleAllNotifications(event)
     * }
     * ```
     *
     * @param event The event to schedule notifications for
     */
    suspend fun scheduleAllNotifications(event: IWWWEvent)

    /**
     * Cancels all scheduled notifications for an event.
     *
     * ## Use Cases
     * - User unfavorites event
     * - Event cancelled in backend
     * - Event time changed (will reschedule)
     *
     * ## Example
     * ```kotlin
     * // User unfavorites event
     * favoriteStore.setFavoriteStatus(event.id, false)
     * scheduler.cancelAllNotifications(event.id)
     * ```
     *
     * @param eventId The event ID to cancel notifications for
     */
    suspend fun cancelAllNotifications(eventId: String)

    /**
     * Syncs scheduled notifications with current state.
     *
     * ## Purpose
     * Called on app launch to ensure notification state is current:
     * - Cancels notifications for unfavorited events
     * - Cancels notifications for cancelled events
     * - Reschedules if event time changed
     *
     * ## Example
     * ```kotlin
     * // In app initialization
     * lifecycleScope.launch {
     *     val favoriteIds = favoriteStore.getAllFavoriteIds()
     *     val events = eventRepository.getEvents(favoriteIds)
     *     scheduler.syncNotifications(favoriteIds, events)
     * }
     * ```
     *
     * @param favoriteIds List of currently favorited event IDs
     * @param currentEvents List of current events from repository
     */
    suspend fun syncNotifications(
        favoriteIds: Set<String>,
        currentEvents: List<IWWWEvent>,
    )
}

/**
 * Default implementation of NotificationScheduler.
 *
 * ## Thread Safety
 * All methods are suspend functions and use appropriate dispatchers.
 * Platform-specific notification scheduling handles thread safety internally.
 *
 * ## Simulation Support
 * Checks `WWWPlatform.getSimulation()?.speed` to determine compatibility:
 * - `null` (no simulation): Enable notifications ✅
 * - `speed == 1`: Enable notifications ✅
 * - `speed > 1`: Disable notifications ❌
 *
 * @param clock Clock for time calculations (IClock for testability)
 * @param platform Platform instance for simulation checks
 * @param favoriteStore Store for checking favorite status
 * @param notificationManager Manager for platform-specific scheduling
 * @param contentProvider Provider for generating notification content
 */
class DefaultNotificationScheduler(
    private val clock: IClock,
    private val platform: WWWPlatform,
    private val favoriteStore: FavoriteEventsStore,
    private val notificationManager: NotificationManager,
    private val contentProvider: NotificationContentProvider,
) : NotificationScheduler {
    companion object {
        /**
         * Standard notification intervals before event start.
         *
         * Ordered from earliest to latest for readability.
         */
        val NOTIFICATION_INTERVALS =
            listOf(
                1.hours,
                30.minutes,
                10.minutes,
                5.minutes,
                1.minutes,
            )
    }

    override suspend fun shouldScheduleNotifications(event: IWWWEvent): Boolean {
        val TAG = "NotificationScheduler"

        // Check if event is favorited
        val isFavorite = favoriteStore.isFavorite(event.id)
        com.worldwidewaves.shared.utils.Log
            .d(TAG, "Event ${event.id} favorite status: $isFavorite")
        if (!isFavorite) {
            com.worldwidewaves.shared.utils.Log
                .d(TAG, "Event ${event.id} NOT favorited - skipping notifications")
            return false
        }

        // Check if event has already started
        val now = clock.now()
        val startTime = event.getStartDateTime()
        com.worldwidewaves.shared.utils.Log
            .d(TAG, "Event ${event.id} timing - now: $now, starts: $startTime")
        if (startTime <= now) {
            com.worldwidewaves.shared.utils.Log
                .w(TAG, "Event ${event.id} already started - skipping notifications")
            return false
        }

        // Check simulation mode compatibility
        val simulation = platform.getSimulation()
        val simSpeed = simulation?.speed
        com.worldwidewaves.shared.utils.Log
            .d(TAG, "Simulation mode - active: ${simulation != null}, speed: $simSpeed")
        if (simulation != null && simulation.speed != 1) {
            // Accelerated simulation - notifications would fire at wrong times
            com.worldwidewaves.shared.utils.Log
                .w(TAG, "Simulation speed is $simSpeed (not 1) - skipping notifications (incompatible)")
            return false
        }

        // All checks passed
        com.worldwidewaves.shared.utils.Log.i(
            TAG,
            "Event ${event.id} ELIGIBLE for notifications (favorited, future, simulation compatible)",
        )
        return true
    }

    override suspend fun scheduleAllNotifications(event: IWWWEvent) {
        val TAG = "NotificationScheduler"
        val now = clock.now()
        val startTime = event.getStartDateTime()

        com.worldwidewaves.shared.utils.Log
            .i(TAG, "Scheduling notifications for event ${event.id} (starts: $startTime, now: $now)")

        var scheduledCount = 0

        // Schedule start notifications (1h, 30m, 10m, 5m, 1m)
        NOTIFICATION_INTERVALS.forEach { interval ->
            val notificationTime = startTime - interval
            if (notificationTime > now) {
                // Only schedule if notification time is in the future
                val delay = notificationTime - now
                val trigger = NotificationTrigger.EventStarting(interval)
                // ContentProvider will resolve the location StringResource to a localized string
                val content = contentProvider.generateStartingNotification(event, interval)

                com.worldwidewaves.shared.utils.Log.d(
                    TAG,
                    "Scheduling ${interval.inWholeMinutes}m before notification for ${event.id} (delay: ${delay.inWholeMinutes}m)",
                )

                com.worldwidewaves.shared.utils.Log
                    .i(TAG, "NotificationManager type: ${notificationManager::class.simpleName}")

                try {
                    notificationManager.scheduleNotification(
                        eventId = event.id,
                        trigger = trigger,
                        delay = delay,
                        content = content,
                    )
                    com.worldwidewaves.shared.utils.Log
                        .d(TAG, "scheduleNotification() completed for ${trigger.id}")
                    scheduledCount++
                } catch (e: Exception) {
                    com.worldwidewaves.shared.utils.Log
                        .e(TAG, "scheduleNotification() failed for ${trigger.id}", throwable = e)
                }
            } else {
                com.worldwidewaves.shared.utils.Log
                    .d(TAG, "Skipping ${interval.inWholeMinutes}m before notification (time already passed)")
            }
        }

        // Schedule event finished notification
        val endTime = event.getEndDateTime()
        if (endTime > now) {
            val delay = endTime - now
            // ContentProvider will resolve the location StringResource to a localized string
            val content = contentProvider.generateFinishedNotification(event)

            com.worldwidewaves.shared.utils.Log
                .d(TAG, "Scheduling event finished notification (delay: ${delay.inWholeMinutes}m)")

            try {
                notificationManager.scheduleNotification(
                    eventId = event.id,
                    trigger = NotificationTrigger.EventFinished,
                    delay = delay,
                    content = content,
                )
                com.worldwidewaves.shared.utils.Log
                    .d(TAG, "scheduleNotification() completed for finished")
                scheduledCount++
            } catch (e: Exception) {
                com.worldwidewaves.shared.utils.Log
                    .e(TAG, "scheduleNotification() failed for finished", throwable = e)
            }
        }

        com.worldwidewaves.shared.utils.Log
            .i(TAG, "Scheduled $scheduledCount notifications for event ${event.id}")
    }

    override suspend fun cancelAllNotifications(eventId: String) {
        notificationManager.cancelAllNotifications(eventId)
    }

    override suspend fun syncNotifications(
        favoriteIds: Set<String>,
        currentEvents: List<IWWWEvent>,
    ) {
        // Build map of current events
        val eventMap = currentEvents.associateBy { it.id }

        // For each favorited event
        favoriteIds.forEach { eventId ->
            val event = eventMap[eventId]

            when {
                // Event no longer exists or is cancelled - cancel notifications
                event == null -> {
                    cancelAllNotifications(eventId)
                }
                // Event exists - check if we should reschedule
                else -> {
                    if (shouldScheduleNotifications(event)) {
                        // Cancel existing and reschedule (simpler than diff)
                        cancelAllNotifications(eventId)
                        scheduleAllNotifications(event)
                    } else {
                        // No longer eligible - cancel
                        cancelAllNotifications(eventId)
                    }
                }
            }
        }
    }
}
