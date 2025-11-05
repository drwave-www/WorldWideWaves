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
 * Represents the different types of notification triggers for wave events.
 *
 * ## Purpose
 * Defines all possible notification triggers in the WorldWideWaves app:
 * - Time-based notifications before event start
 * - Event completion notifications
 * - Wave hit notifications (when app is open/backgrounded)
 *
 * ## Notification Types
 * 1. **EventStarting**: 1h, 30min, 10min, 5min, 1min before event
 * 2. **EventFinished**: When event completes
 * 3. **WaveHit**: When wave reaches user (app must be open/backgrounded)
 *
 * ## Usage
 * ```kotlin
 * val triggers = listOf(
 *     NotificationTrigger.EventStarting(1.hours),
 *     NotificationTrigger.EventStarting(30.minutes),
 *     NotificationTrigger.EventFinished
 * )
 *
 * triggers.forEach { trigger ->
 *     notificationManager.scheduleNotification(eventId, trigger, content)
 * }
 * ```
 *
 * ## Platform Delivery
 * - **Android**: WorkManager for scheduled, immediate notification for wave hit
 * - **iOS**: UNUserNotificationCenter for scheduled, immediate for wave hit
 *
 * @see NotificationManager for scheduling API
 * @see NotificationScheduler for timing calculations
 */
sealed class NotificationTrigger {
    /**
     * Unique identifier for this trigger type.
     *
     * Used for:
     * - WorkManager/UNNotificationRequest unique IDs
     * - Cancelling specific notifications
     * - Tracking delivered notifications
     */
    abstract val id: String

    /**
     * Notification before event starts.
     *
     * ## Timing
     * Triggered at: `event.getStartDateTime() - duration`
     *
     * ## Supported Durations
     * - 1 hour: "Wave starting in 1 hour"
     * - 30 minutes: "Wave starting in 30 minutes"
     * - 10 minutes: "Wave starting in 10 minutes"
     * - 5 minutes: "Wave starting in 5 minutes"
     * - 1 minute: "Wave starting in 1 minute!"
     *
     * ## Example
     * ```kotlin
     * val trigger = NotificationTrigger.EventStarting(30.minutes)
     * // For event at 14:00, notification fires at 13:30
     * ```
     *
     * @param duration Time before event start to trigger notification
     */
    data class EventStarting(
        val duration: Duration,
    ) : NotificationTrigger() {
        override val id: String get() = "start_${duration.inWholeMinutes}m"
    }

    /**
     * Notification when event finishes.
     *
     * ## Timing
     * Triggered at: `event.getEndDateTime()`
     *
     * ## Content
     * - Title: "Wave completed"
     * - Body: "The [EventName] wave has finished"
     *
     * ## Example
     * ```kotlin
     * val trigger = NotificationTrigger.EventFinished
     * // For event ending at 15:30, notification fires at 15:30
     * ```
     */
    data object EventFinished : NotificationTrigger() {
        override val id: String get() = "finished"
    }

    /**
     * Notification when wave hits user's location.
     *
     * ## Requirements
     * **App must be open or backgrounded** - cannot trigger from true background.
     *
     * ## Detection
     * Detected by `WaveProgressionTracker.isUserInWaveArea()` during active observation:
     * - Position flow monitors user location
     * - Detects transition from `not in area` → `in area`
     * - Triggers immediate local notification
     *
     * ## Delivery
     * - Android: Immediate notification via NotificationManager
     * - iOS: UNUserNotificationCenter with 0.1s delay trigger
     *
     * ## Example
     * ```kotlin
     * // In EventObserver position monitoring
     * if (isInArea && !wasInArea && favoriteStore.isFavorite(eventId)) {
     *     notificationManager.deliverNow(
     *         NotificationTrigger.WaveHit,
     *         contentProvider.generateWaveHitNotification(event)
     *     )
     * }
     * ```
     */
    data object WaveHit : NotificationTrigger() {
        override val id: String get() = "wave_hit"
    }
}
