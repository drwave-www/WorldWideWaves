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

import com.worldwidewaves.shared.WWWShutdownHandler
import com.worldwidewaves.shared.domain.observation.DefaultPositionObserver
import com.worldwidewaves.shared.domain.observation.PositionObserver
import com.worldwidewaves.shared.domain.progression.DefaultWaveProgressionTracker
import com.worldwidewaves.shared.domain.progression.WaveProgressionTracker
import com.worldwidewaves.shared.domain.scheduling.DefaultObservationScheduler
import com.worldwidewaves.shared.domain.scheduling.ObservationScheduler
import com.worldwidewaves.shared.domain.state.DefaultEventStateHolder
import com.worldwidewaves.shared.domain.state.EventStateHolder
import com.worldwidewaves.shared.events.config.DefaultEventsConfigurationProvider
import com.worldwidewaves.shared.events.config.EventsConfigurationProvider
import com.worldwidewaves.shared.events.data.DefaultGeoJsonDataProvider
import com.worldwidewaves.shared.events.data.DefaultMapDataProvider
import com.worldwidewaves.shared.events.data.GeoJsonDataProvider
import com.worldwidewaves.shared.events.data.MapDataProvider
import com.worldwidewaves.shared.events.decoding.DefaultEventsDecoder
import com.worldwidewaves.shared.events.decoding.EventsDecoder
import com.worldwidewaves.shared.events.utils.CoroutineScopeProvider
import com.worldwidewaves.shared.events.utils.DefaultCoroutineScopeProvider
import com.worldwidewaves.shared.events.utils.IClock
import com.worldwidewaves.shared.events.utils.SystemClock
import com.worldwidewaves.shared.position.PositionManager
import com.worldwidewaves.shared.utils.CloseableCoroutineScope
import org.koin.dsl.module

/**
 * DI module providing utility services and domain logic for wave event management.
 *
 * ## Module Purpose
 * Provides core infrastructure and domain services including:
 * - Position tracking and observation
 * - Wave progression monitoring
 * - Event state management
 * - Observation scheduling
 * - Event data loading and decoding
 * - Coroutine scope management
 * - Time/clock abstraction
 *
 * ## Initialization
 * - **Load order**: Second module loaded in sharedModule (after commonModule)
 * - **Platform**: Common (shared between Android and iOS)
 * - **Required modules**: commonModule (for WWWEvents)
 * - **Dependencies**: Platform-specific implementations injected separately
 *
 * ## Scoping Strategy
 * - **Singletons**: Most dependencies are singletons to maintain consistent state
 *   - Position tracking (PositionManager, PositionObserver)
 *   - Wave progression and scheduling
 *   - Event configuration and data providers
 *   - Shared coroutine scopes
 * - **Factories**: WWWShutdownHandler (created per cleanup request)
 *
 * ## Platform Considerations
 * - **Android**: GPS provider injected from androidMain
 * - **iOS**: GPS provider injected from iosMain (must be thread-safe)
 * - **Thread-safety**: All singletons are thread-safe using coroutines and StateFlow
 *
 * @see PositionManager for centralized position tracking
 * @see PositionObserver for wave event observation
 * @see WaveProgressionTracker for wave progression monitoring
 * @see EventStateManager for event state coordination
 */
val helpersModule =
    module {
        /**
         * Provides [CoroutineScopeProvider] for managing coroutine scopes.
         *
         * **Scope**: Singleton - single provider for coroutine scope creation
         * **Thread-safety**: Yes - scope creation is thread-safe
         * **Lifecycle**: Lives for entire app lifecycle
         * **Dependencies**: None
         *
         * Provides scopes for background work isolated from specific components.
         *
         * @see CoroutineScopeProvider for scope creation API
         */
        single<CoroutineScopeProvider> { DefaultCoroutineScopeProvider() }

        /**
         * Provides [PositionManager] as singleton for centralized position tracking.
         *
         * **Scope**: Singleton - single source of truth for user position
         * **Thread-safety**: Yes - uses StateFlow for thread-safe position updates
         * **Lifecycle**: Lives for entire app lifecycle
         * **Dependencies**: Platform-specific GPS provider (injected from platform modules)
         *
         * PositionManager is the ONLY source of user position in the app:
         * - Manages position source priority (simulation > GPS)
         * - Provides debounced position updates via StateFlow
         * - Handles position accuracy validation
         *
         * **Critical**: All position consumers must observe PositionManager.positionFlow,
         * never create separate position sources.
         *
         * @see PositionManager for position management API
         */
        single<PositionManager> { PositionManager(get()) }

        /**
         * Provides [WaveProgressionTracker] for monitoring wave progression.
         *
         * **Scope**: Singleton - single tracker for all wave events
         * **Thread-safety**: Yes - uses coroutine-based tracking
         * **Lifecycle**: Lives for entire app lifecycle
         * **Dependencies**: IClock for time-based calculations
         *
         * Tracks wave progression across the event boundary, calculating:
         * - Current wave position
         * - Time until wave arrival
         * - Wave phase (approaching, passing, completed)
         *
         * @see WaveProgressionTracker for progression tracking API
         */
        single<WaveProgressionTracker> { DefaultWaveProgressionTracker(get()) }

        /**
         * Provides [PositionObserver] for observing user position relative to wave events.
         *
         * **Scope**: Singleton - single observer for all position-based logic
         * **Thread-safety**: Yes - uses coroutine-based observation
         * **Lifecycle**: Lives for entire app lifecycle
         * **Dependencies**: PositionManager, WaveProgressionTracker, EventStateHolder
         *
         * PositionObserver is the core of wave detection logic:
         * - Monitors user position via PositionManager.positionFlow
         * - Detects when user crosses wave boundaries
         * - Triggers events when user "catches" a wave
         * - Coordinates with EventStateHolder for state updates
         *
         * This is the unified observer replacing multiple separate observation streams.
         *
         * @see PositionObserver for observation API
         */
        single<PositionObserver> { DefaultPositionObserver(get(), get(), get()) }

        /**
         * Provides [EventStateHolder] for managing event lifecycle state.
         *
         * **Scope**: Singleton - single state holder for all events
         * **Thread-safety**: Yes - uses StateFlow for state updates
         * **Lifecycle**: Lives for entire app lifecycle
         * **Dependencies**: WaveProgressionTracker, ObservationScheduler
         *
         * EventStateHolder coordinates event state transitions:
         * - Scheduled -> Observing -> Active -> Completed
         * - Manages observation start/stop scheduling
         * - Coordinates cleanup of completed events
         *
         * @see EventStateHolder for state management API
         */
        single<EventStateHolder> { DefaultEventStateHolder(get(), get()) }

        /**
         * Provides [ObservationScheduler] for scheduling observation windows.
         *
         * **Scope**: Singleton - single scheduler for all observation timing
         * **Thread-safety**: Yes - coroutine-based scheduling
         * **Lifecycle**: Lives for entire app lifecycle
         * **Dependencies**: IClock for time calculations
         *
         * ObservationScheduler manages when to start/stop observing events:
         * - Schedules observation start before event begins
         * - Automatically stops observation after event ends
         * - Respects configured observation windows
         *
         * @see ObservationScheduler for scheduling API
         */
        single<ObservationScheduler> { DefaultObservationScheduler(get()) }

        /**
         * Provides [WWWShutdownHandler] as factory for cleanup operations.
         *
         * **Scope**: Factory - new instance per cleanup request
         * **Thread-safety**: Yes - each instance is independent
         * **Lifecycle**: Created on-demand, disposed after cleanup completes
         * **Dependencies**: CloseableCoroutineScope for coroutine cleanup
         *
         * Factory scope is used because cleanup is a transient operation:
         * - Each cleanup operation gets fresh handler
         * - No shared state between cleanup operations
         * - Garbage collected after cleanup completes
         *
         * @see WWWShutdownHandler for cleanup API
         */
        factory { WWWShutdownHandler(get()) }

        /**
         * Provides [IClock] abstraction for time-based operations.
         *
         * **Scope**: Singleton - single time source for the app
         * **Thread-safety**: Yes - reading system time is thread-safe
         * **Lifecycle**: Lives for entire app lifecycle
         * **Dependencies**: None
         *
         * IClock enables testing of time-dependent logic:
         * - Production: Uses system clock (SystemClock)
         * - Testing: Can inject TestClock for time manipulation
         *
         * @see IClock for clock API
         */
        single<IClock> { SystemClock() }

        /**
         * Provides [EventsConfigurationProvider] for event configuration management.
         *
         * **Scope**: Singleton - single configuration provider
         * **Thread-safety**: Yes - configuration is immutable after load
         * **Lifecycle**: Lives for entire app lifecycle
         * **Dependencies**: Platform-specific file access (injected)
         *
         * Loads event configuration from JSON files:
         * - Event metadata (name, description, timing)
         * - Wave parameters (speed, width, frequency)
         * - Observation windows
         *
         * @see EventsConfigurationProvider for configuration API
         */
        single<EventsConfigurationProvider> { DefaultEventsConfigurationProvider(get()) }

        /**
         * Provides [GeoJsonDataProvider] for loading GeoJSON event boundaries.
         *
         * **Scope**: Singleton - single provider for GeoJSON data
         * **Thread-safety**: Yes - data loading is thread-safe
         * **Lifecycle**: Lives for entire app lifecycle
         * **Dependencies**: None (uses platform-agnostic file access)
         *
         * Loads GeoJSON files defining event boundaries:
         * - Event perimeter geometries
         * - Wave progression paths
         * - Checkpoint locations
         *
         * @see GeoJsonDataProvider for GeoJSON loading API
         */
        single<GeoJsonDataProvider> { DefaultGeoJsonDataProvider() }

        /**
         * Provides [MapDataProvider] for loading map tile data.
         *
         * **Scope**: Singleton - single provider for map data
         * **Thread-safety**: Yes - data loading is thread-safe
         * **Lifecycle**: Lives for entire app lifecycle
         * **Dependencies**: None (uses MapLibre-compatible data sources)
         *
         * Provides map tile data for event visualization:
         * - Base map tiles (streets, satellite)
         * - Event-specific overlays
         * - Offline map caching (future enhancement)
         *
         * @see MapDataProvider for map data API
         */
        single<MapDataProvider> { DefaultMapDataProvider() }

        /**
         * Provides [EventsDecoder] for decoding event data formats.
         *
         * **Scope**: Singleton - single decoder for all events
         * **Thread-safety**: Yes - decoding is stateless and thread-safe
         * **Lifecycle**: Lives for entire app lifecycle
         * **Dependencies**: None
         *
         * Decodes event data from various formats:
         * - JSON event configurations
         * - GeoJSON boundaries
         * - Binary event archives (future)
         *
         * @see EventsDecoder for decoding API
         */
        single<EventsDecoder> { DefaultEventsDecoder() }

        /**
         * Provides [CloseableCoroutineScope] for managed coroutine lifecycle.
         *
         * **Scope**: Singleton - shared closeable scope
         * **Thread-safety**: Yes - coroutine scope is thread-safe
         * **Lifecycle**: Lives for entire app lifecycle, closed on shutdown
         * **Dependencies**: None
         *
         * Provides a coroutine scope that can be explicitly closed:
         * - Used for app-wide background operations
         * - Cleanly cancels all jobs on app shutdown
         * - Prevents coroutine leaks
         *
         * @see CloseableCoroutineScope for scope management API
         */
        single { CloseableCoroutineScope() }
    }
