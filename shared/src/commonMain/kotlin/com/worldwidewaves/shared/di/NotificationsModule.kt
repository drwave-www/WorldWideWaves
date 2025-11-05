package com.worldwidewaves.shared.di

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

import com.worldwidewaves.shared.notifications.DefaultNotificationContentProvider
import com.worldwidewaves.shared.notifications.DefaultNotificationScheduler
import com.worldwidewaves.shared.notifications.NotificationContentProvider
import com.worldwidewaves.shared.notifications.NotificationManager
import com.worldwidewaves.shared.notifications.NotificationScheduler
import com.worldwidewaves.shared.notifications.createPlatformNotificationManager
import org.koin.dsl.module

/**
 * DI module providing push notification dependencies for favorited wave events.
 *
 * ## Module Purpose
 * Provides notification infrastructure for time-based alerts:
 * - Notification scheduling logic (when to schedule)
 * - Platform-specific notification delivery (Android WorkManager, iOS UNUserNotificationCenter)
 * - Localized notification content generation
 *
 * ## Notification Strategy
 * **Favorites-only approach**:
 * - Only favorited events trigger notifications
 * - Keeps notification count under platform limits (iOS: 64, Android: ~500)
 * - High relevance (user explicitly opted in)
 *
 * ## Notification Types
 * 1. **Time-based**: 1h, 30m, 10m, 5m, 1m before event starts
 * 2. **Event finished**: When event completes
 * 3. **Wave hit**: When wave reaches user (app open/backgrounded only)
 *
 * ## Simulation Mode Support
 * - **Real events**: Notifications enabled ✅
 * - **Realistic simulation (speed=1)**: Notifications enabled ✅
 * - **Accelerated simulation (speed>1)**: Notifications disabled ❌
 *
 * ## Initialization
 * - **Load order**: Fourth module loaded in sharedModule (after commonModule, helpersModule, datastoreModule)
 * - **Platform**: Common + platform-specific implementations
 * - **Required modules**: helpersModule (IClock, WWWPlatform), datastoreModule (FavoriteEventsStore)
 *
 * ## Scoping Strategy
 * - **Singletons**: NotificationScheduler, NotificationContentProvider (stateless, reusable)
 * - **Platform-specific**: NotificationManager (expect/actual pattern for Android/iOS)
 *
 * ## Platform Implementations
 * - **Android**: `AndroidNotificationManager` using WorkManager for scheduled notifications
 * - **iOS**: `IOSNotificationManager` using UNUserNotificationCenter with lazy initialization
 *
 * ## Thread Safety
 * All components handle thread-safety internally:
 * - NotificationScheduler: Uses suspend functions, delegates to NotificationManager
 * - NotificationManager: Platform-specific thread handling (WorkManager/UNUserNotificationCenter)
 * - NotificationContentProvider: Stateless, thread-safe
 *
 * @see NotificationScheduler for scheduling logic
 * @see NotificationManager for platform-specific delivery
 * @see NotificationContentProvider for content generation
 */
val notificationsModule =
    module {
        /**
         * Provides [NotificationScheduler] as singleton for managing notification scheduling.
         *
         * **Scope**: Singleton - single scheduler for entire app lifecycle
         * **Thread-safety**: Yes - uses suspend functions and platform-safe notification delivery
         * **Lifecycle**: Created on first access, lives for entire app lifecycle
         * **Dependencies**:
         * - IClock: For time calculations (simulation-aware)
         * - WWWPlatform: For simulation mode checking
         * - FavoriteEventsStore: For checking favorite status
         * - NotificationManager: For platform-specific scheduling
         * - NotificationContentProvider: For generating notification content
         *
         * The NotificationScheduler:
         * - Determines eligibility (favorited + simulation compatible)
         * - Calculates notification timings (1h, 30m, 10m, 5m, 1m before)
         * - Coordinates with NotificationManager for actual delivery
         * - Syncs notification state on app launch
         *
         * Singleton scope ensures:
         * - Consistent scheduling logic across app
         * - Single coordination point for all notifications
         * - Memory efficient (single instance)
         *
         * @see DefaultNotificationScheduler for implementation
         */
        single<NotificationScheduler> {
            DefaultNotificationScheduler(
                clock = get(),
                platform = get(),
                favoriteStore = get(),
                notificationManager = get(),
                contentProvider = get(),
            )
        }

        /**
         * Provides [NotificationManager] as singleton via platform-specific factory.
         *
         * **Scope**: Singleton - single manager for entire app lifecycle
         * **Thread-safety**: Yes - platform implementations handle thread-safety
         * **Lifecycle**: Created on first access, lives for entire app lifecycle
         * **Dependencies**: Platform-specific (Context on Android, nothing on iOS)
         *
         * Platform implementations:
         * - **Android**: `AndroidNotificationManager`
         *   - Uses WorkManager for scheduled notifications
         *   - Uses NotificationManager for immediate notifications (wave hit)
         *   - Requires Context for NotificationCompat.Builder
         *
         * - **iOS**: `IOSNotificationManager`
         *   - Uses UNUserNotificationCenter for all notifications
         *   - Lazy initialization to prevent iOS deadlocks
         *   - No dependencies (accesses system APIs directly)
         *
         * Singleton scope ensures:
         * - Single notification delivery channel per platform
         * - Consistent notification IDs (prevents duplicates)
         * - Proper notification limit management
         *
         * @see createPlatformNotificationManager for expect/actual factory
         */
        single<NotificationManager> { createPlatformNotificationManager() }

        /**
         * Provides [NotificationContentProvider] as singleton for generating localized content.
         *
         * **Scope**: Singleton - single provider for entire app lifecycle
         * **Thread-safety**: Yes - stateless, pure functions
         * **Lifecycle**: Created on first access, lives for entire app lifecycle
         * **Dependencies**: None
         *
         * The NotificationContentProvider:
         * - Generates localization keys (not final strings)
         * - Maps notification triggers to string resource keys
         * - Provides deep link URLs for notification taps
         *
         * Returns localization keys because:
         * - WorkManager/UNUserNotificationCenter run in separate process contexts
         * - MokoRes not accessible in notification workers
         * - Platform layers resolve keys using native localization APIs
         *
         * Singleton scope appropriate because:
         * - Stateless (no mutable state)
         * - Pure functions (same input → same output)
         * - Memory efficient (single instance)
         *
         * @see DefaultNotificationContentProvider for implementation
         */
        single<NotificationContentProvider> { DefaultNotificationContentProvider() }
    }
