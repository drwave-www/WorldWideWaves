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

import com.worldwidewaves.shared.domain.repository.EventsRepository
import com.worldwidewaves.shared.domain.repository.EventsRepositoryImpl
import com.worldwidewaves.shared.domain.usecases.CheckEventFavoritesUseCase
import com.worldwidewaves.shared.domain.usecases.FilterEventsUseCase
import com.worldwidewaves.shared.domain.usecases.GetSortedEventsUseCase
import com.worldwidewaves.shared.ui.AboutTabScreen
import com.worldwidewaves.shared.ui.EventsListScreen
import org.koin.dsl.module

/**
 * DI module providing UI layer dependencies for shared Compose components.
 *
 * ## Module Purpose
 * Provides UI-related dependencies including:
 * - Shared Compose screen components
 * - Domain layer (repositories, use cases)
 * - Business logic for event management
 *
 * ## Initialization
 * - **Load order**: Fourth (final) module loaded in sharedModule
 * - **Platform**: Common (shared between Android and iOS)
 * - **Required modules**: All previous modules (common, helpers, datastore)
 * - **UI Framework**: Compose Multiplatform (100% shared UI code)
 *
 * ## Scoping Strategy
 * - **Singletons**: All dependencies are singletons for consistent state
 *   - Screen components: Single instance per screen (lightweight, stateless)
 *   - Repositories: Single source of truth for data access
 *   - Use cases: Stateless business logic (safe as singletons)
 *
 * ## Platform Considerations
 * - **Android**: Uses AndroidX Compose
 * - **iOS**: Uses Compose Multiplatform via ComposeUIViewController
 * - **ViewModels**: Platform-specific (not in this module)
 *   - Android: AndroidViewModel with MapAvailabilityChecker
 *   - iOS: iOS-specific ViewModel with platform dependencies
 *
 * ## Architecture Notes
 * This module follows Clean Architecture principles:
 * - **Repository Layer**: Data access abstraction (EventsRepository)
 * - **Use Cases Layer**: Business logic operations
 * - **UI Layer**: Composable screens (stateless,ViewModel-driven)
 *
 * EventsViewModel is intentionally kept in platform-specific modules because it has
 * platform-specific dependencies (MapAvailabilityChecker, platform-specific lifecycle).
 *
 * @see EventsRepository for data access
 * @see EventsListScreen for shared events UI
 * @see AboutTabScreen for shared about UI
 */
val uiModule =
    module {
        /**
         * Provides [AboutTabScreen] as singleton Compose screen component.
         *
         * **Scope**: Singleton - single screen instance for the app
         * **Thread-safety**: Yes - Compose is thread-safe
         * **Lifecycle**: Lives for entire app lifecycle (lightweight, no state)
         * **Dependencies**: Platform abstraction (injected)
         *
         * AboutTabScreen is a stateless Compose component showing:
         * - App version and build information
         * - Credits and acknowledgments
         * - License information
         * - Contact/support links
         *
         * Singleton scope is safe because:
         * - Component is stateless (no mutable state)
         * - Lightweight (just function references)
         * - No per-instance resources to manage
         *
         * @see AboutTabScreen for screen implementation
         */
        single { AboutTabScreen(get()) }

        /**
         * Provides [EventsListScreen] as singleton Compose screen component.
         *
         * **Scope**: Singleton - single screen instance for the app
         * **Thread-safety**: Yes - Compose is thread-safe
         * **Lifecycle**: Lives for entire app lifecycle (lightweight, no state)
         * **Dependencies**: EventsViewModel, MapAvailabilityChecker, SetEventFavorite
         *
         * EventsListScreen is the main screen showing:
         * - List of available wave events
         * - Event filtering and sorting
         * - Favorite events toggle
         * - Event detail navigation
         *
         * The screen is stateless - all state is managed by EventsViewModel:
         * - ViewModel is injected (platform-specific)
         * - Screen observes ViewModel state via Compose State
         * - User actions are delegated to ViewModel
         *
         * Singleton scope is safe because:
         * - Component is stateless (state in ViewModel)
         * - Lightweight (just Composable function)
         * - No per-instance resources
         *
         * @see EventsListScreen for screen implementation
         */
        single { EventsListScreen(viewModel = get(), mapChecker = get(), setEventFavorite = get()) }

        // Repository layer -------------------------------------------------------

        /**
         * Provides [EventsRepository] as singleton for event data access.
         *
         * **Scope**: Singleton - single source of truth for event data
         * **Thread-safety**: Yes - uses coroutines and StateFlow
         * **Lifecycle**: Lives for entire app lifecycle
         * **Dependencies**: WWWEvents (core event manager)
         *
         * EventsRepository abstracts event data access:
         * - Loads events from Firebase/local storage
         * - Provides reactive event streams
         * - Caches event data in memory
         * - Handles event updates and invalidation
         *
         * This is the data layer in Clean Architecture:
         * - Domain layer (use cases) depends on this
         * - UI layer accesses data via use cases
         * - Provides platform-agnostic data access
         *
         * @see EventsRepository for repository interface
         * @see EventsRepositoryImpl for implementation
         */
        single<EventsRepository> { EventsRepositoryImpl(get()) }

        // Use cases layer --------------------------------------------------------

        /**
         * Provides [GetSortedEventsUseCase] for retrieving sorted events.
         *
         * **Scope**: Singleton - stateless business logic
         * **Thread-safety**: Yes - stateless, thread-safe operations
         * **Lifecycle**: Lives for entire app lifecycle
         * **Dependencies**: EventsRepository
         *
         * GetSortedEventsUseCase encapsulates event sorting logic:
         * - Sorts events by start time (chronological)
         * - Filters out past events
         * - Applies user preferences (if configured)
         *
         * Singleton scope is safe because:
         * - Use case is stateless (no mutable state)
         * - Pure business logic (deterministic)
         * - Lightweight (no resources to manage)
         *
         * Usage pattern:
         * ```kotlin
         * val getSorted: GetSortedEventsUseCase = get()
         * val sortedEvents = getSorted.execute()
         * ```
         *
         * @see GetSortedEventsUseCase for sorting logic
         */
        single { GetSortedEventsUseCase(get()) }

        /**
         * Provides [FilterEventsUseCase] for filtering events by criteria.
         *
         * **Scope**: Singleton - stateless business logic
         * **Thread-safety**: Yes - stateless, thread-safe operations
         * **Lifecycle**: Lives for entire app lifecycle
         * **Dependencies**: EventsRepository
         *
         * FilterEventsUseCase encapsulates event filtering logic:
         * - Filter by location proximity
         * - Filter by event type
         * - Filter by favorite status
         * - Filter by time range
         *
         * Singleton scope is safe because:
         * - Use case is stateless (filtering is pure function)
         * - No mutable state to share
         * - Lightweight operation
         *
         * Usage pattern:
         * ```kotlin
         * val filter: FilterEventsUseCase = get()
         * val filtered = filter.execute(
         *     events = allEvents,
         *     criteria = FilterCriteria(onlyFavorites = true)
         * )
         * ```
         *
         * @see FilterEventsUseCase for filtering logic
         */
        single { FilterEventsUseCase(get()) }

        /**
         * Provides [CheckEventFavoritesUseCase] for checking favorite status.
         *
         * **Scope**: Singleton - stateless business logic
         * **Thread-safety**: Yes - stateless, thread-safe operations
         * **Lifecycle**: Lives for entire app lifecycle
         * **Dependencies**: None (accesses FavoriteEventsStore via repository)
         *
         * CheckEventFavoritesUseCase encapsulates favorite checking logic:
         * - Checks if event is marked as favorite
         * - Retrieves all favorite event IDs
         * - Synchronizes favorite state
         *
         * Singleton scope is safe because:
         * - Use case is stateless (reads from store)
         * - No mutable state in use case
         * - Lightweight read operation
         *
         * Usage pattern:
         * ```kotlin
         * val checkFavorites: CheckEventFavoritesUseCase = get()
         * val isFavorite = checkFavorites.isEventFavorite(eventId)
         * ```
         *
         * @see CheckEventFavoritesUseCase for favorite checking logic
         */
        single { CheckEventFavoritesUseCase() }
    }
