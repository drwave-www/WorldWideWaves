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
import androidx.work.WorkManager
import org.koin.java.KoinJavaComponent.inject

/**
 * Android platform implementation factory for NotificationManager.
 *
 * ## Dependencies (from Koin DI)
 * - Context: Android application context
 * - WorkManager: For scheduled notification delivery
 *
 * ## Setup Requirements
 * Before using notifications:
 * 1. Create notification channel: `NotificationChannelManager.createChannel(context)`
 * 2. Request POST_NOTIFICATIONS permission (Android 13+)
 *
 * ## Example (in MainActivity.onCreate)
 * ```kotlin
 * class MainActivity : ComponentActivity() {
 *     override fun onCreate(savedInstanceState: Bundle?) {
 *         super.onCreate(savedInstanceState)
 *         NotificationChannelManager.createChannel(this)
 *         // ... rest of initialization
 *     }
 * }
 * ```
 *
 * @return AndroidNotificationManager instance
 * @see AndroidNotificationManager for implementation details
 * @see NotificationChannelManager for channel setup
 */
actual fun createPlatformNotificationManager(): NotificationManager {
    val context: Context by inject(Context::class.java)
    val workManager = WorkManager.getInstance(context)
    return AndroidNotificationManager(context, workManager)
}
