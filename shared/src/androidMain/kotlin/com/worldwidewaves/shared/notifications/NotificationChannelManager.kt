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

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.worldwidewaves.shared.utils.Log

/**
 * Manager for creating and configuring notification channels on Android O+ (API 26+).
 *
 * ## Android Notification Channels
 * Starting with Android 8.0 (API 26), all notifications must be assigned to a channel.
 * Channels allow users to control notification settings per category.
 *
 * ## Channel Configuration
 * - **ID**: `WAVE_EVENTS_CHANNEL`
 * - **Name**: "Wave Events" (user-visible in Settings)
 * - **Importance**: HIGH (time-sensitive wave events)
 * - **Behavior**: Vibration enabled, badge enabled, no sound (users can configure)
 *
 * ## Initialization
 * Must be called before any notifications are shown, typically in:
 * - MainActivity.onCreate() (most common)
 * - MainApplication.onCreate() (alternative)
 * - Before first AndroidNotificationManager usage
 *
 * ## Idempotency
 * Safe to call multiple times - Android ignores duplicate channel creation.
 *
 * ## Example Usage
 * ```kotlin
 * // In MainActivity.onCreate() or MainApplication.onCreate()
 * class MainActivity : ComponentActivity() {
 *     override fun onCreate(savedInstanceState: Bundle?) {
 *         super.onCreate(savedInstanceState)
 *         NotificationChannelManager.createChannel(this)
 *         // ... rest of initialization
 *     }
 * }
 * ```
 *
 * ## User Control
 * After channel creation, users can modify settings in:
 * Settings → Apps → WorldWideWaves → Notifications → Wave Events
 *
 * @see AndroidNotificationManager for notification delivery
 * @see NotificationWorker for scheduled notifications
 */
object NotificationChannelManager {
    private const val TAG = "NotificationChannelManager"
    private const val CHANNEL_ID = "WAVE_EVENTS_CHANNEL"
    private const val CHANNEL_NAME = "Wave Events"
    private const val CHANNEL_DESCRIPTION = "Notifications for wave event timings and updates"

    /**
     * Creates the notification channel for wave events.
     *
     * ## Behavior
     * - Android O+ (API 26+): Creates NotificationChannel
     * - Pre-O: No-op (channels not supported)
     * - Idempotent: Safe to call multiple times
     *
     * ## Channel Settings
     * - High importance (pops up on screen)
     * - Vibration enabled
     * - Badge enabled (notification dot on app icon)
     * - No default sound (users can configure)
     *
     * @param context Android application context
     */
    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = CHANNEL_DESCRIPTION
                    enableVibration(true)
                    setShowBadge(true)
                    // No default sound - users can configure in settings
                }

            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)

            Log.d(TAG, "Created notification channel: $CHANNEL_ID")
        } else {
            Log.d(TAG, "Notification channels not supported on API ${Build.VERSION.SDK_INT}")
        }
    }

    /**
     * Deletes the notification channel (use with caution).
     *
     * ## Warning
     * Deleting a channel removes user preferences. Only use for:
     * - Testing/development
     * - Major app redesign requiring new channel structure
     *
     * Users will need to reconfigure notification settings after deletion.
     *
     * @param context Android application context
     */
    fun deleteChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager?.deleteNotificationChannel(CHANNEL_ID)
            Log.w(TAG, "Deleted notification channel: $CHANNEL_ID (user preferences lost)")
        }
    }

    /**
     * Checks if notification channel is enabled by user.
     *
     * ## Use Cases
     * - Show UI hint if user disabled channel
     * - Skip notification scheduling if disabled
     *
     * @param context Android application context
     * @return true if channel enabled, false if disabled by user or not created
     */
    fun isChannelEnabled(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            val channel = notificationManager?.getNotificationChannel(CHANNEL_ID)
            return channel?.importance != NotificationManager.IMPORTANCE_NONE
        }
        // Pre-O: No channels, assume enabled if permission granted
        return true
    }
}
