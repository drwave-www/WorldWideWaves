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

import com.worldwidewaves.shared.choreographies.SoundChoreographyPlayer
import com.worldwidewaves.shared.data.createSpriteCachePreferences
import com.worldwidewaves.shared.events.WWWEvents
import com.worldwidewaves.shared.localization.LocalizationManager
import com.worldwidewaves.shared.map.SpriteCache
import com.worldwidewaves.shared.sound.SoundChoreographyCoordinator
import kotlinx.coroutines.MainScope
import org.koin.dsl.module

/**
 * DI module providing core event and sound choreography dependencies.
 *
 * ## Module Purpose
 * Provides essential global components for the WorldWideWaves application:
 * - Event management and lifecycle
 * - Sound choreography playback and coordination
 * - Core audio-visual synchronization infrastructure
 *
 * ## Initialization
 * - **Load order**: First module loaded in sharedModule list (loads before helpers, datastore, ui)
 * - **Platform**: Common (shared between Android and iOS)
 * - **Required modules**: None (no dependencies on other modules)
 * - **Initialization timing**: SoundChoreographyPlayer is eagerly initialized at app startup
 *
 * ## Scoping Strategy
 * - **Singletons**: All dependencies are singletons to ensure consistent state across the app
 *   - WWWEvents: Single source of truth for event data
 *   - SoundChoreographyPlayer: Single audio playback instance (eager initialization)
 *   - SoundChoreographyCoordinator: Single coordinator for synchronized audio playback
 *
 * ## Platform Considerations
 * - **Android**: Uses Android MIDI API for sound playback
 * - **iOS**: Uses iOS CoreMIDI for sound playback
 * - **Thread-safety**: All components handle thread-safety internally using coroutines
 *
 * @see WWWEvents for event data management
 * @see SoundChoreographyPlayer for audio playback
 * @see SoundChoreographyCoordinator for synchronization
 */
val commonModule =
    module {
        /**
         * Provides [WWWEvents] as singleton for managing event data and lifecycle.
         *
         * **Scope**: Singleton - single source of truth for all event data in the application
         * **Thread-safety**: Yes - internally uses coroutines and StateFlow for thread-safe updates
         * **Lifecycle**: Created on first access, lives for entire app lifecycle
         * **Dependencies**: None
         *
         * WWWEvents manages the global list of wave events, including:
         * - Loading events from Firebase/local storage
         * - Providing reactive streams of event data
         * - Managing event state changes
         *
         * @see WWWEvents for detailed API documentation
         */
        single { WWWEvents() }

        /**
         * Provides [SoundChoreographyPlayer] as eager singleton for MIDI-based sound playback.
         *
         * **Scope**: Singleton - single audio player instance for the entire application
         * **Thread-safety**: Yes - thread-safe MIDI API usage
         * **Lifecycle**: Created immediately at app startup (createdAtStart = true)
         * **Dependencies**: None
         * **Eager initialization**: Required to prepare MIDI subsystem before first wave event
         *
         * The SoundChoreographyPlayer is created eagerly to:
         * 1. Initialize platform-specific MIDI subsystems early
         * 2. Avoid latency on first sound playback
         * 3. Ensure audio is ready when first wave event triggers
         *
         * Platform implementations:
         * - Android: Uses MediaPlayer with MIDI soundfont
         * - iOS: Uses AVAudioEngine with MIDI synthesis
         *
         * @see SoundChoreographyPlayer for audio playback API
         */
        single(createdAtStart = true) { SoundChoreographyPlayer() }

        /**
         * Provides [SoundChoreographyCoordinator] as singleton for synchronized audio-visual coordination.
         *
         * **Scope**: Singleton - single coordinator for all wave events
         * **Thread-safety**: Yes - uses coroutine-based synchronization
         * **Lifecycle**: Created on first access, lives for entire app lifecycle
         * **Dependencies**: None (but typically used with SoundChoreographyPlayer)
         *
         * The SoundChoreographyCoordinator ensures:
         * - Synchronized audio playback across multiple devices
         * - Timing coordination between sound and visual wave effects
         * - Proper scheduling of sound cues relative to wave progression
         *
         * @see SoundChoreographyCoordinator for coordination API
         */
        single { SoundChoreographyCoordinator() }

        /**
         * Provides [LocalizationManager] as singleton for runtime locale change detection.
         *
         * **Scope**: Singleton - single locale observer for entire application
         * **Thread-safety**: Yes - uses StateFlow for thread-safe locale emission
         * **Lifecycle**: Created on first access, lives for entire app lifecycle
         * **Dependencies**: None
         *
         * The LocalizationManager enables:
         * - Runtime language switching without app restart
         * - Reactive UI updates when device language changes
         * - Platform-agnostic locale change observation (Android + iOS)
         *
         * Platform detection:
         * - Android: MainActivity.onConfigurationChanged() calls notifyLocaleChanged()
         * - iOS: SceneDelegate NotificationCenter observer calls LocalizationBridge.notifyLocaleChanged()
         *
         * @see LocalizationManager for locale change API
         */
        single { LocalizationManager() }

        /**
         * Provides [SpriteCachePreferences] as singleton for sprite cache state persistence.
         *
         * **Scope**: Singleton - single preferences instance for entire application
         * **Thread-safety**: Yes - platform-specific implementations are thread-safe
         * **Lifecycle**: Created on first access, lives for entire app lifecycle
         * **Dependencies**: None
         *
         * The SpriteCachePreferences stores:
         * - Cache completion flag (true/false)
         * - Cached app version (for invalidation on updates)
         * - Completion timestamp
         *
         * Platform implementations:
         * - Android: Uses SharedPreferences
         * - iOS: Uses NSUserDefaults
         *
         * @see SpriteCachePreferences for persistence API
         */
        single { createSpriteCachePreferences() }

        /**
         * Provides [SpriteCache] as singleton for background sprite/glyph caching.
         *
         * **Scope**: Singleton - single cache manager for entire application
         * **Thread-safety**: Yes - uses mutex for cache operation synchronization
         * **Lifecycle**: Created eagerly, starts background caching immediately
         * **Dependencies**: SpriteCachePreferences
         * **iOS Safety**: Uses MainScope().launch{} to avoid init{} deadlocks
         *
         * The SpriteCache:
         * - Pre-caches 775 sprite/glyph files (~6.4MB) in background on app launch
         * - Eliminates 10-20 second delay on first map load
         * - Provides reactive StateFlow for UI progress indicators
         * - Handles version invalidation, disk space checks, integrity verification
         *
         * Performance impact:
         * - Without SpriteCache: 10-20 second delay on EVERY first map load
         * - With SpriteCache: <1 second on ALL map loads (after background cache completes)
         *
         * Background caching is started immediately via MainScope().launch{} to ensure:
         * - Non-blocking app startup
         * - Cache completion before user opens first map
         * - iOS Kotlin/Native deadlock prevention (no init{} coroutines)
         *
         * @see SpriteCache for caching API and state management
         */
        single {
            SpriteCache(
                preferences = get(),
                scope = MainScope(),
            ).apply {
                // Start background caching immediately (non-blocking)
                // iOS SAFE: This runs AFTER Koin initialization completes, not during init{}
                startBackgroundCache()
            }
        }
    }
