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
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.worldwidewaves.shared.utils.Log

/**
 * WorkManager worker for delivering scheduled notifications.
 *
 * ## Architecture
 * - Extends `CoroutineWorker` for suspend function support
 * - Retrieves notification data from `inputData` (set by AndroidNotificationManager)
 * - Resolves localization keys using Android resources (not MokoRes)
 * - Shows notification via NotificationCompat.Builder
 * - Handles deep links to open specific event screens
 *
 * ## Localization Strategy
 * WorkManager runs in a separate process context, so it cannot access MokoRes.
 * Instead:
 * 1. AndroidNotificationManager passes localization KEYS in inputData
 * 2. NotificationWorker resolves keys using `context.resources.getIdentifier()`
 * 3. Falls back to key itself if resource not found (development safety)
 *
 * ## Input Data Format
 * ```
 * eventId: String          // "event123"
 * triggerId: String        // "start_60m", "finished", "wave_hit"
 * titleKey: String         // "notification_event_starting_soon"
 * bodyKey: String          // "notification_1h_before"
 * bodyArgs: Array<String>  // ["New York", "14:00"]
 * deepLink: String         // "worldwidewaves://event?id=event123"
 * ```
 *
 * ## Deep Link Behavior
 * - Opens MainActivity with deep link intent
 * - Flags: NEW_TASK | CLEAR_TOP (brings app to foreground)
 * - PendingIntent: UPDATE_CURRENT | IMMUTABLE (Android 12+ requirement)
 *
 * ## Error Handling
 * - Returns `Result.failure()` if required input missing
 * - Returns `Result.success()` on successful delivery
 * - Logs errors but doesn't throw exceptions
 *
 * ## Example Usage
 * ```kotlin
 * // Enqueued by AndroidNotificationManager
 * val inputData = Data.Builder()
 *     .putString(INPUT_EVENT_ID, "event123")
 *     .putString(INPUT_TRIGGER_ID, "start_60m")
 *     .putString(INPUT_TITLE_KEY, "notification_event_starting_soon")
 *     .putString(INPUT_BODY_KEY, "notification_1h_before")
 *     .putStringArray(INPUT_BODY_ARGS, arrayOf("New York"))
 *     .putString(INPUT_DEEP_LINK, "worldwidewaves://event?id=event123")
 *     .build()
 *
 * val workRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
 *     .setInputData(inputData)
 *     .setInitialDelay(60, TimeUnit.MINUTES)
 *     .build()
 * ```
 *
 * @param context Android application context
 * @param params WorkerParameters containing input data
 *
 * @see AndroidNotificationManager for enqueueing logic
 * @see NotificationChannelManager for channel setup
 */
class NotificationWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    companion object {
        private const val TAG = "NotificationWorker"
    }

    override suspend fun doWork(): Result {
        try {
            // Extract input data
            val eventId = inputData.getString(AndroidNotificationManager.INPUT_EVENT_ID) ?: return Result.failure()
            val triggerId = inputData.getString(AndroidNotificationManager.INPUT_TRIGGER_ID) ?: return Result.failure()
            val titleKey = inputData.getString(AndroidNotificationManager.INPUT_TITLE_KEY) ?: return Result.failure()
            val bodyKey = inputData.getString(AndroidNotificationManager.INPUT_BODY_KEY) ?: return Result.failure()
            val bodyArgs = inputData.getStringArray(AndroidNotificationManager.INPUT_BODY_ARGS) ?: emptyArray()
            val deepLink = inputData.getString(AndroidNotificationManager.INPUT_DEEP_LINK) ?: return Result.failure()

            // Resolve localization keys
            val title = resolveString(titleKey, emptyArray())
            val body = resolveString(bodyKey, bodyArgs)

            // Create deep link intent
            val intent =
                Intent(Intent.ACTION_VIEW, Uri.parse(deepLink)).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }

            val pendingIntent =
                PendingIntent.getActivity(
                    applicationContext,
                    eventId.hashCode(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )

            // Build and show notification
            val notification =
                NotificationCompat
                    .Builder(applicationContext, AndroidNotificationManager.NOTIFICATION_CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_dialog_info) // TODO: Use app icon
                    .setContentTitle(title)
                    .setContentText(body)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .build()

            NotificationManagerCompat
                .from(applicationContext)
                .notify(buildNotificationId(eventId, triggerId), notification)

            Log.d(TAG, "Delivered notification for event $eventId ($triggerId)")
            return Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to deliver notification", e)
            return Result.failure()
        }
    }

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
        val resId = applicationContext.resources.getIdentifier(key, "string", applicationContext.packageName)
        return if (resId != 0) {
            applicationContext.getString(resId, *args)
        } else {
            Log.w(TAG, "Localization key not found: $key (using key as fallback)")
            key
        }
    }

    /**
     * Builds a unique notification ID for NotificationManager.
     *
     * Uses hash of eventId + triggerId to generate stable integer ID.
     */
    private fun buildNotificationId(
        eventId: String,
        triggerId: String,
    ): Int = "notification_${eventId}_$triggerId".hashCode()
}
