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
import com.worldwidewaves.shared.utils.Log
import dev.icerock.moko.resources.StringResource
import dev.icerock.moko.resources.desc.desc
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSNumber
import platform.Foundation.NSString
import platform.Foundation.stringWithFormat
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNTimeIntervalNotificationTrigger
import platform.UserNotifications.UNUserNotificationCenter
import kotlin.time.Duration

/**
 * iOS implementation of NotificationManager using UNUserNotificationCenter.
 *
 * ## Architecture
 * - **Scheduled**: UNNotificationRequest with UNTimeIntervalNotificationTrigger
 * - **Immediate**: 0.1 second delay trigger (closest to immediate on iOS)
 * - **Persistence**: UNUserNotificationCenter handles across app restarts
 * - **Threading**: All methods use suspend and dispatch to main queue internally
 *
 * ## iOS Safety [CRITICAL]
 * **CLASS-based implementation (not object)** - Following SystemClock pattern:
 * - ✅ Uses lazy initialization for UNUserNotificationCenter
 * - ✅ NO `init{}` block with DI calls
 * - ✅ NO `object : KoinComponent` pattern
 * - ✅ NO `by inject()` in property initialization
 *
 * ## Notification Identifiers
 * Format: `event_${eventId}_${trigger.id}`
 * Examples:
 * - `event_123_start_60m` (1h before)
 * - `event_123_finished`
 * - `event_123_wave_hit`
 *
 * ## iOS 64 Notification Limit
 * iOS allows max 64 pending notifications. This implementation:
 * - Relies on favorites-only design (~60 max per user)
 * - Could implement LRU eviction if needed (future enhancement)
 *
 * ## Localization Strategy
 * Uses `titleLocKey` and `bodyLocKey` on UNMutableNotificationContent:
 * - iOS resolves localization keys at notification delivery time
 * - Respects user's current locale setting
 * - No need to resolve strings in Kotlin layer
 *
 * ## Deep Link Handling
 * - Stores deep link in `userInfo` dictionary
 * - SceneDelegate intercepts notification tap
 * - Routes to appropriate view controller
 *
 * ## Thread Safety
 * All UNUserNotificationCenter operations are dispatched to main queue.
 *
 * @see NotificationManager for interface documentation
 * @see UNUserNotificationCenter for iOS notification API
 */
@OptIn(ExperimentalForeignApi::class)
class IOSNotificationManager : NotificationManager {
    companion object {
        private const val TAG = "IOSNotificationManager"
        private const val IMMEDIATE_DELAY_SECONDS = 0.1
    }

    /**
     * Lazy initialization of UNUserNotificationCenter.
     *
     * iOS Safety: Prevents initialization in `init{}` block which could cause deadlocks.
     * Following the pattern from SystemClock (see ClockProvider.kt).
     */
    private val notificationCenter: UNUserNotificationCenter by lazy {
        UNUserNotificationCenter.currentNotificationCenter()
    }

    override suspend fun scheduleNotification(
        eventId: String,
        trigger: NotificationTrigger,
        delay: Duration,
        content: NotificationContent,
    ) {
        try {
            val identifier = buildNotificationIdentifier(eventId, trigger)
            val notificationContent = buildUNContent(content, eventId)
            val notificationTrigger =
                UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(
                    timeInterval = delay.inWholeSeconds.toDouble(),
                    repeats = false,
                )

            val request =
                UNNotificationRequest.requestWithIdentifier(
                    identifier = identifier,
                    content = notificationContent,
                    trigger = notificationTrigger,
                )

            notificationCenter.addNotificationRequest(request) { error ->
                if (error != null) {
                    Log.e(TAG, "Failed to schedule notification: $identifier", throwable = Exception(error.localizedDescription))
                } else {
                    Log.d(TAG, "Scheduled notification: $identifier with delay ${delay.inWholeMinutes}m")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule notification for event $eventId", throwable = e)
        }
    }

    override suspend fun deliverNow(
        eventId: String,
        trigger: NotificationTrigger,
        content: NotificationContent,
    ) {
        try {
            val identifier = buildNotificationIdentifier(eventId, trigger)
            val notificationContent = buildUNContent(content, eventId)

            // iOS doesn't support true immediate notifications
            // Use 0.1 second delay as closest approximation
            val notificationTrigger =
                UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(
                    timeInterval = IMMEDIATE_DELAY_SECONDS,
                    repeats = false,
                )

            val request =
                UNNotificationRequest.requestWithIdentifier(
                    identifier = identifier,
                    content = notificationContent,
                    trigger = notificationTrigger,
                )

            notificationCenter.addNotificationRequest(request) { error ->
                if (error != null) {
                    Log.e(TAG, "Failed to deliver immediate notification: $identifier", throwable = Exception(error.localizedDescription))
                } else {
                    Log.d(TAG, "Delivered immediate notification for event $eventId (${trigger.id})")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to deliver immediate notification for event $eventId", throwable = e)
        }
    }

    override suspend fun cancelNotification(
        eventId: String,
        trigger: NotificationTrigger,
    ) {
        try {
            val identifier = buildNotificationIdentifier(eventId, trigger)
            val identifiers = listOf(identifier)

            @Suppress("UNCHECKED_CAST")
            notificationCenter.removePendingNotificationRequestsWithIdentifiers(identifiers as List<Any?>)

            @Suppress("UNCHECKED_CAST")
            notificationCenter.removeDeliveredNotificationsWithIdentifiers(identifiers as List<Any?>)

            Log.d(TAG, "Cancelled notification: $identifier")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cancel notification for event $eventId", throwable = e)
        }
    }

    override suspend fun cancelAllNotifications(eventId: String) {
        try {
            // Build all possible notification identifiers for this event
            val identifiers =
                buildList {
                    // EventFinished and WaveHit
                    add(buildNotificationIdentifier(eventId, NotificationTrigger.EventFinished))
                    add(buildNotificationIdentifier(eventId, NotificationTrigger.WaveHit))

                    // Common EventStarting notifications
                    listOf(60, 30, 10, 5, 1).forEach { minutes ->
                        val trigger = NotificationTrigger.EventStarting(Duration.parse("${minutes}m"))
                        add(buildNotificationIdentifier(eventId, trigger))
                    }
                }

            @Suppress("UNCHECKED_CAST")
            notificationCenter.removePendingNotificationRequestsWithIdentifiers(identifiers as List<Any?>)

            @Suppress("UNCHECKED_CAST")
            notificationCenter.removeDeliveredNotificationsWithIdentifiers(identifiers as List<Any?>)

            Log.d(TAG, "Cancelled all notifications for event $eventId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cancel all notifications for event $eventId", throwable = e)
        }
    }

    /**
     * Builds a unique notification identifier for UNNotificationRequest.
     *
     * Format: `event_${eventId}_${trigger.id}`
     *
     * Examples:
     * - `event_event123_start_60m`
     * - `event_event456_finished`
     */
    private fun buildNotificationIdentifier(
        eventId: String,
        trigger: NotificationTrigger,
    ): String = "event_${eventId}_${trigger.id}"

    /**
     * Maps a localization key to a MokoRes StringResource.
     *
     * This function provides runtime lookup of MokoRes strings by their key name,
     * similar to Android's `resources.getIdentifier()` but using compile-time safe mapping.
     *
     * @param key The localization key (e.g., "notification_event_starting_soon")
     * @return The corresponding StringResource, or null if not found
     */
    private fun getStringResourceByKey(key: String): StringResource? =
        when (key) {
            // Notification titles
            "notification_event_starting_soon" -> MokoRes.strings.notification_event_starting_soon
            "notification_event_finished" -> MokoRes.strings.notification_event_finished
            "notification_wave_hit" -> MokoRes.strings.notification_wave_hit

            // Notification bodies
            "notification_1h_before" -> MokoRes.strings.notification_1h_before
            "notification_30m_before" -> MokoRes.strings.notification_30m_before
            "notification_10m_before" -> MokoRes.strings.notification_10m_before
            "notification_5m_before" -> MokoRes.strings.notification_5m_before
            "notification_1m_before" -> MokoRes.strings.notification_1m_before
            "notification_event_finished_body" -> MokoRes.strings.notification_event_finished_body
            "notification_wave_hit_body" -> MokoRes.strings.notification_wave_hit_body

            // Permission-related
            "notification_permission_rationale" -> MokoRes.strings.notification_permission_rationale
            "notification_permission_denied" -> MokoRes.strings.notification_permission_denied

            else -> null
        }

    /**
     * Resolves a localization key to a formatted string using MokoRes.
     *
     * This mirrors Android's `resolveString()` method but uses MokoRes instead of Android resources.
     *
     * ## Process
     * 1. Map key name to MokoRes StringResource
     * 2. Resolve StringResource to localized string
     * 3. Format string with arguments if provided
     *
     * ## Example
     * ```kotlin
     * resolveString("notification_1h_before", listOf("New York"))
     * // Returns: "New York wave starts in 1 hour" (in user's locale)
     * ```
     *
     * @param key Localization key (e.g., "notification_event_starting_soon")
     * @param args Arguments to interpolate into the string (%1$s, %2$s, etc.)
     * @return Resolved and formatted string, or the key itself if not found
     */
    @Suppress("UNCHECKED_CAST")
    private fun resolveString(
        key: String,
        args: List<String>,
    ): String {
        val stringResource = getStringResourceByKey(key)

        if (stringResource == null) {
            Log.w(TAG, "Localization key not found: $key (using key as fallback)")
            return key
        }

        // Resolve to localized string using MokoRes
        val localizedString = stringResource.desc().localized()

        // If no arguments, return as-is
        if (args.isEmpty()) {
            return localizedString
        }

        // Format string with arguments
        // IMPORTANT: Store NSString references in variables to prevent premature deallocation
        // Direct casting in varargs (args[0] as NSString) causes EXC_BAD_ACCESS on iOS
        return when (args.size) {
            1 -> {
                val arg0: NSString = args[0] as NSString
                NSString.stringWithFormat(localizedString, arg0) as String
            }
            2 -> {
                val arg0: NSString = args[0] as NSString
                val arg1: NSString = args[1] as NSString
                NSString.stringWithFormat(localizedString, arg0, arg1) as String
            }
            3 -> {
                val arg0: NSString = args[0] as NSString
                val arg1: NSString = args[1] as NSString
                val arg2: NSString = args[2] as NSString
                NSString.stringWithFormat(localizedString, arg0, arg1, arg2) as String
            }
            else -> {
                Log.w(TAG, "Too many arguments for string formatting: ${args.size} (max 3 supported)")
                localizedString
            }
        }
    }

    /**
     * Builds UNMutableNotificationContent with localized strings.
     *
     * ## Localization
     * Resolves MokoRes string keys to localized strings before setting them on the notification.
     * This ensures notifications display in the user's current locale.
     *
     * ## Deep Link
     * Stores deep link in `userInfo` dictionary for SceneDelegate to handle.
     *
     * @param content Notification content from shared code
     * @param eventId Event identifier for deep linking
     * @return Configured UNMutableNotificationContent ready for UNNotificationRequest
     */
    @Suppress("UNCHECKED_CAST")
    private fun buildUNContent(
        content: NotificationContent,
        eventId: String,
    ): UNMutableNotificationContent {
        val notificationContent = UNMutableNotificationContent()

        // Resolve localization keys to actual strings using MokoRes
        val resolvedTitle = resolveString(content.titleKey, emptyList())
        val resolvedBody = resolveString(content.bodyKey, content.bodyArgs)

        notificationContent.setTitle(resolvedTitle)
        notificationContent.setBody(resolvedBody)

        // Set sound
        notificationContent.setSound(UNNotificationSound.defaultSound())

        // Store deep link and event ID in userInfo for SceneDelegate routing
        notificationContent.setUserInfo(
            mapOf(
                "deepLink" to content.deepLink,
                "eventId" to eventId,
            ) as Map<Any?, *>,
        )

        // Set badge to increment on notification delivery
        notificationContent.setBadge(NSNumber(1))

        return notificationContent
    }
}
