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

/**
 * Platform-agnostic representation of notification content.
 *
 * ## Purpose
 * Provides a common data structure for notification content that can be used
 * by both Android and iOS platform-specific implementations.
 *
 * ## Localization Strategy
 * **IMPORTANT**: This class contains localization KEYS, not final strings.
 *
 * Platform workers cannot access MokoRes directly (different process context), so:
 * 1. Shared code: Calculate which string key to use
 * 2. Android: Resolve `context.getString(R.string.notification_1h_before)`
 * 3. iOS: Use `content.titleLocKey = "notification_1h_before"`
 *
 * ## Example
 * ```kotlin
 * // Shared code (NotificationContentProvider)
 * val content = NotificationContent(
 *     titleKey = "notification_event_starting_soon",
 *     bodyKey = "notification_1h_before",
 *     bodyArgs = listOf(event.getLocation().getString()),
 *     deepLink = "worldwidewaves://event?id=${event.id}"
 * )
 *
 * // Android (NotificationWorker)
 * val title = context.getString(R.string.notification_event_starting_soon)
 * val body = context.getString(R.string.notification_1h_before, args[0])
 *
 * // iOS (IOSNotificationManager)
 * content.titleLocKey = "notification_event_starting_soon"
 * content.bodyLocKey = "notification_1h_before"
 * content.bodyLocArgs = ["New York"]
 * ```
 *
 * @property titleKey Localization key for notification title
 * @property bodyKey Localization key for notification body
 * @property bodyArgs Arguments to interpolate into body string (%1$s, %2$s, etc.)
 * @property deepLink Deep link URL to open when notification tapped
 *
 * @see NotificationContentProvider for content generation
 */
data class NotificationContent(
    val titleKey: String,
    val bodyKey: String,
    val bodyArgs: List<String> = emptyList(),
    val deepLink: String,
)
