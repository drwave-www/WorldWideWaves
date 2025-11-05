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

import com.worldwidewaves.shared.data.InitFavoriteEvent
import com.worldwidewaves.shared.data.SetEventFavorite
import org.koin.dsl.module

/**
 * DI module providing data persistence and storage-related dependencies.
 *
 * ## Module Purpose
 * Provides data layer dependencies for persistent storage operations:
 * - Event favorites management
 * - User preferences persistence
 * - Local data caching (future)
 *
 * ## Initialization
 * - **Load order**: Third module loaded in sharedModule (after commonModule, helpersModule)
 * - **Platform**: Common (shared between Android and iOS)
 * - **Required modules**: helpersModule (for data providers)
 * - **Storage backend**: Platform-specific (SharedPreferences on Android, UserDefaults on iOS)
 *
 * ## Scoping Strategy
 * - **Factories**: All dependencies are factories to support per-request operations
 *   - InitFavoriteEvent: Initializes favorites state (created per initialization)
 *   - SetEventFavorite: Updates favorites (created per update operation)
 *
 * Factory scope is used because these are command/action objects that:
 * 1. Execute a single operation
 * 2. Don't maintain long-lived state
 * 3. Should be garbage collected after operation completes
 *
 * ## Platform Considerations
 * - **Android**: Uses SharedPreferences via multiplatform-settings
 * - **iOS**: Uses UserDefaults via multiplatform-settings
 * - **Thread-safety**: FavoriteEventsStore handles thread-safe reads/writes
 *
 * ## Storage Architecture
 * The favorite events store is injected from platform-specific modules:
 * - Android: Provides FavoriteEventsStore backed by SharedPreferences
 * - iOS: Provides FavoriteEventsStore backed by UserDefaults
 *
 * @see InitFavoriteEvent for favorites initialization
 * @see SetEventFavorite for favorites updates
 */
val datastoreModule =
    module {

        // Persistent stores ------------------------------------------------------

        /**
         * Provides [InitFavoriteEvent] as factory for initializing event favorites.
         *
         * **Scope**: Factory - created per initialization request
         * **Thread-safety**: Yes - FavoriteEventsStore handles thread-safety
         * **Lifecycle**: Created on-demand, disposed after initialization completes
         * **Dependencies**: FavoriteEventsStore (injected from platform modules)
         *
         * InitFavoriteEvent is used during app startup to:
         * 1. Load saved favorite events from persistent storage
         * 2. Populate in-memory favorite state
         * 3. Reconcile favorites with available events
         *
         * Factory scope ensures:
         * - Fresh instance for each initialization
         * - No lingering state between initializations
         * - Memory efficiency (garbage collected after use)
         *
         * @see InitFavoriteEvent for initialization logic
         */
        factory { InitFavoriteEvent(favoriteEventsStore = get()) }

        /**
         * Provides [SetEventFavorite] as factory for updating event favorites.
         *
         * **Scope**: Factory - created per favorite update request
         * **Thread-safety**: Yes - FavoriteEventsStore handles thread-safe writes
         * **Lifecycle**: Created on-demand, disposed after update completes
         * **Dependencies**:
         * - FavoriteEventsStore (injected from platform modules)
         * - NotificationScheduler (optional, injected if available)
         *
         * SetEventFavorite is used when users:
         * 1. Mark an event as favorite (star/heart action) → schedules notifications
         * 2. Remove an event from favorites (unstar/unheart action) → cancels notifications
         * 3. Sync favorites across sessions
         *
         * ## Phase 4 Integration
         * When favoriting an event, SetEventFavorite automatically:
         * - Schedules 6 time-based notifications (1h, 30m, 10m, 5m, 1m, finished)
         * - Validates simulation mode compatibility (speed == 1 only)
         * - Checks event hasn't started yet
         *
         * When unfavoriting, cancels all scheduled notifications.
         *
         * Factory scope ensures:
         * - Fresh instance for each update operation
         * - No shared mutable state between operations
         * - Clean memory footprint
         *
         * Usage pattern:
         * ```kotlin
         * val setFavorite: SetEventFavorite = get()
         * setFavorite.call(event, isFavorite = true)
         * ```
         *
         * @see SetEventFavorite for favorite update logic
         * @see com.worldwidewaves.shared.notifications.NotificationScheduler for scheduling logic
         */
        factory {
            SetEventFavorite(
                favoriteEventsStore = get(),
                notificationScheduler = getOrNull(),
            )
        }
    }
