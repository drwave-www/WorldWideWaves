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

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.worldwidewaves.shared.utils.Log
import java.util.concurrent.TimeUnit
import kotlin.time.Duration

/**
 * Android implementation of NotificationManager using WorkManager for scheduled notifications.
 *
 * ## Architecture
 * - **Scheduled**: WorkManager OneTimeWorkRequest with delay
 * - **Immediate**: NotificationCompat.Builder + NotificationManagerCompat
 * - **Persistence**: WorkManager handles across app restarts
 * - **Threading**: WorkManager handles background execution
 *
 * ## Work Naming Strategy
 * Unique work names ensure idempotency and enable cancellation:
 * - Format: `notification_${eventId}_${trigger.id}`
 * - Examples:
 *   - `notification_event123_start_60m` (1h before)
 *   - `notification_event123_finished`
 *   - `notification_event123_wave_hit`
 *
 * ## Update Policy
 * Uses `ExistingWorkPolicy.REPLACE` to update notifications when event details change.
 *
 * ## Notification Limits
 * Android has ~500 pending notification limit (varies by OEM).
 * Favorites-only design keeps volume under 60 notifications typical.
 *
 * ## Thread Safety
 * All methods are suspend functions. WorkManager handles internal thread-safety.
 *
 * @property context Android application context (from Koin DI)
 * @property workManager WorkManager instance for scheduling
 *
 * @see NotificationWorker for notification delivery logic
 * @see NotificationChannelManager for Android O+ channel setup
 */
class AndroidNotificationManager(
    private val context: Context,
    private val workManager: WorkManager,
) : NotificationManager {
    companion object {
        private const val TAG = "AndroidNotificationManager"
        private const val WORK_TAG_PREFIX = "event_"
        const val NOTIFICATION_CHANNEL_ID = "WAVE_EVENTS_CHANNEL"
        const val INPUT_EVENT_ID = "eventId"
        const val INPUT_TRIGGER_ID = "triggerId"
        const val INPUT_TITLE_KEY = "titleKey"
        const val INPUT_BODY_KEY = "bodyKey"
        const val INPUT_BODY_ARGS = "bodyArgs"
        const val INPUT_DEEP_LINK = "deepLink"
    }

    override suspend fun scheduleNotification(
        eventId: String,
        trigger: NotificationTrigger,
        delay: Duration,
        content: NotificationContent,
    ) {
        try {
            val workName = buildWorkName(eventId, trigger)
            val inputData = buildInputData(eventId, trigger, content)

            val workRequest =
                OneTimeWorkRequestBuilder<NotificationWorker>()
                    .setInputData(inputData)
                    .setInitialDelay(delay.inWholeMilliseconds, TimeUnit.MILLISECONDS)
                    .addTag(WORK_TAG_PREFIX + eventId)
                    .build()

            workManager.enqueueUniqueWork(
                workName,
                ExistingWorkPolicy.REPLACE,
                workRequest,
            )

            Log.d(TAG, "Scheduled notification: $workName with delay ${delay.inWholeMinutes}m")
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
            val title = resolveString(content.titleKey, emptyArray())
            val body = resolveString(content.bodyKey, content.bodyArgs.toTypedArray())

            val intent =
                Intent(Intent.ACTION_VIEW, Uri.parse(content.deepLink)).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }

            val pendingIntent =
                PendingIntent.getActivity(
                    context,
                    eventId.hashCode(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )

            val notification =
                NotificationCompat
                    .Builder(context, NOTIFICATION_CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_dialog_info) // TODO: Use app icon
                    .setContentTitle(title)
                    .setContentText(body)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .build()

            NotificationManagerCompat
                .from(context)
                .notify(buildNotificationId(eventId, trigger), notification)

            Log.d(TAG, "Delivered immediate notification for event $eventId (${trigger.id})")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to deliver immediate notification for event $eventId", throwable = e)
        }
    }

    override suspend fun cancelNotification(
        eventId: String,
        trigger: NotificationTrigger,
    ) {
        try {
            val workName = buildWorkName(eventId, trigger)
            workManager.cancelUniqueWork(workName)

            // Also cancel any delivered notification
            NotificationManagerCompat
                .from(context)
                .cancel(buildNotificationId(eventId, trigger))

            Log.d(TAG, "Cancelled notification: $workName")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cancel notification for event $eventId", throwable = e)
        }
    }

    override suspend fun cancelAllNotifications(eventId: String) {
        try {
            workManager.cancelAllWorkByTag(WORK_TAG_PREFIX + eventId)

            // Cancel all delivered notifications for this event
            NotificationTrigger.EventStarting(kotlin.time.Duration.ZERO).let { /* Dummy for exhaustive when */ }
            val triggers =
                listOf(
                    NotificationTrigger.EventFinished,
                    NotificationTrigger.WaveHit,
                )

            triggers.forEach { trigger ->
                NotificationManagerCompat
                    .from(context)
                    .cancel(buildNotificationId(eventId, trigger))
            }

            // Cancel common EventStarting notifications
            listOf(60, 30, 10, 5, 1).forEach { minutes ->
                val trigger = NotificationTrigger.EventStarting(kotlin.time.Duration.parse("${minutes}m"))
                NotificationManagerCompat
                    .from(context)
                    .cancel(buildNotificationId(eventId, trigger))
            }

            Log.d(TAG, "Cancelled all notifications for event $eventId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cancel all notifications for event $eventId", throwable = e)
        }
    }

    /**
     * Builds a unique work name for WorkManager.
     *
     * Format: `notification_${eventId}_${trigger.id}`
     *
     * Examples:
     * - `notification_event123_start_60m`
     * - `notification_event456_finished`
     */
    private fun buildWorkName(
        eventId: String,
        trigger: NotificationTrigger,
    ): String = "notification_${eventId}_${trigger.id}"

    /**
     * Builds a unique notification ID for NotificationManager.
     *
     * Uses hash of work name to generate stable integer ID.
     */
    private fun buildNotificationId(
        eventId: String,
        trigger: NotificationTrigger,
    ): Int = buildWorkName(eventId, trigger).hashCode()

    /**
     * Builds input data for NotificationWorker.
     *
     * Includes localization keys (not resolved strings) because WorkManager
     * runs in a separate process context that cannot access MokoRes.
     */
    private fun buildInputData(
        eventId: String,
        trigger: NotificationTrigger,
        content: NotificationContent,
    ) = workDataOf(
        INPUT_EVENT_ID to eventId,
        INPUT_TRIGGER_ID to trigger.id,
        INPUT_TITLE_KEY to content.titleKey,
        INPUT_BODY_KEY to content.bodyKey,
        INPUT_BODY_ARGS to content.bodyArgs.toTypedArray(),
        INPUT_DEEP_LINK to content.deepLink,
    )

    /**
     * Resolves a localization key to a string using Android resources.
     *
     * Falls back to the key itself if resource not found (development safety).
     *
     * @param key Localization key (e.g., "notification_event_starting_soon")
     * @param args Arguments to interpolate into the string
     * @return Resolved string or key if not found
     */
    private fun resolveString(
        key: String,
        args: Array<String>,
    ): String {
        val resId = context.resources.getIdentifier(key, "string", context.packageName)
        return if (resId != 0) {
            context.getString(resId, *args)
        } else {
            Log.w(TAG, "Localization key not found: $key (using key as fallback)")
            key
        }
    }
}
