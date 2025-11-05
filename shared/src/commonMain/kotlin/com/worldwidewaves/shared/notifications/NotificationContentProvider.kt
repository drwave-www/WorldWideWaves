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
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/**
 * Generates notification content with localization keys.
 *
 * ## Purpose
 * Creates platform-agnostic notification content that can be resolved to
 * localized strings by Android and iOS platform layers.
 *
 * ## Localization Strategy
 * Returns localization **keys** (not final strings) because:
 * - WorkManager/UNUserNotificationCenter run in separate process contexts
 * - MokoRes not accessible in platform notification workers
 * - Platform layers resolve keys using native localization APIs
 *
 * ## Example Flow
 * ```kotlin
 * // 1. Shared code generates content with keys
 * val content = contentProvider.generateStartingNotification(event, 1.hours)
 * // content.titleKey = "notification_event_starting_soon"
 * // content.bodyKey = "notification_1h_before"
 * // content.bodyArgs = ["New York"]
 *
 * // 2. Android resolves at delivery time
 * val title = context.getString(R.string.notification_event_starting_soon)
 * val body = context.getString(R.string.notification_1h_before, "New York")
 *
 * // 3. iOS resolves at delivery time
 * content.titleLocKey = "notification_event_starting_soon"
 * content.bodyLocKey = "notification_1h_before"
 * content.bodyLocArgs = ["New York"]
 * ```
 *
 * @see NotificationContent for content structure
 */
interface NotificationContentProvider {
    /**
     * Generates content for "event starting" notifications.
     *
     * ## Supported Intervals
     * - 1 hour: "notification_1h_before"
     * - 30 minutes: "notification_30m_before"
     * - 10 minutes: "notification_10m_before"
     * - 5 minutes: "notification_5m_before"
     * - 1 minute: "notification_1m_before"
     *
     * ## String Format
     * Body string format: "%1$s starts in [duration]"
     * - %1$s = event location name (e.g., "New York")
     *
     * @param event The event triggering the notification
     * @param timeUntilStart Duration until event starts
     * @return Notification content with localization keys
     */
    fun generateStartingNotification(
        event: IWWWEvent,
        timeUntilStart: Duration,
    ): NotificationContent

    /**
     * Generates content for "event finished" notification.
     *
     * ## String Format
     * - Title: "notification_event_finished" → "Wave completed"
     * - Body: "notification_event_finished_body" → "The %1$s wave has finished"
     *   - %1$s = event location name
     *
     * @param event The event that finished
     * @return Notification content with localization keys
     */
    fun generateFinishedNotification(event: IWWWEvent): NotificationContent

    /**
     * Generates content for "wave hit" notification.
     *
     * ## Requirements
     * Only called when app is open/backgrounded during wave hit detection.
     *
     * ## String Format
     * - Title: "notification_wave_hit" → "Wave hit!"
     * - Body: "notification_wave_hit_body" → "The wave just reached you in %1$s!"
     *   - %1$s = event location name
     *
     * @param event The event whose wave hit the user
     * @return Notification content with localization keys
     */
    fun generateWaveHitNotification(event: IWWWEvent): NotificationContent
}

/**
 * Default implementation of NotificationContentProvider.
 *
 * ## Localization Keys
 * All string keys correspond to entries in:
 * - `shared/src/commonMain/moko-resources/base/strings.xml`
 * - Translated versions in 32 languages
 *
 * ## Deep Links
 * All notifications include deep link: `worldwidewaves://event?id={eventId}`
 * - Android: Opens event detail screen via MainActivity intent filter
 * - iOS: Opens event detail screen via SceneDelegate URL handling
 */
class DefaultNotificationContentProvider : NotificationContentProvider {
    override fun generateStartingNotification(
        event: IWWWEvent,
        timeUntilStart: Duration,
    ): NotificationContent {
        val bodyKey =
            when (timeUntilStart) {
                1.hours -> "notification_1h_before"
                30.minutes -> "notification_30m_before"
                10.minutes -> "notification_10m_before"
                5.minutes -> "notification_5m_before"
                1.minutes -> "notification_1m_before"
                else -> "notification_event_starting_soon" // Fallback
            }

        return NotificationContent(
            titleKey = "notification_event_starting_soon",
            bodyKey = bodyKey,
            bodyArgs = listOf(event.getLocation().resourceId.toString()),
            deepLink = "worldwidewaves://event?id=${event.id}",
        )
    }

    override fun generateFinishedNotification(event: IWWWEvent): NotificationContent =
        NotificationContent(
            titleKey = "notification_event_finished",
            bodyKey = "notification_event_finished_body",
            bodyArgs = listOf(event.getLocation().resourceId.toString()),
            deepLink = "worldwidewaves://event?id=${event.id}",
        )

    override fun generateWaveHitNotification(event: IWWWEvent): NotificationContent =
        NotificationContent(
            titleKey = "notification_wave_hit",
            bodyKey = "notification_wave_hit_body",
            bodyArgs = listOf(event.getLocation().resourceId.toString()),
            deepLink = "worldwidewaves://event?id=${event.id}",
        )
}
