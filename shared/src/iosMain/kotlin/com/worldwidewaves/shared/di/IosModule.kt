package com.worldwidewaves.shared.di

/* * Copyright 2025 DrWave
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
 * limitations under the License. */

import com.worldwidewaves.shared.WWWPlatform
import com.worldwidewaves.shared.choreographies.ChoreographyManager
import com.worldwidewaves.shared.data.FavoriteEventsStore
import com.worldwidewaves.shared.data.IosFavoriteEventsStore
import com.worldwidewaves.shared.domain.usecases.IosMapAvailabilityChecker
import com.worldwidewaves.shared.domain.usecases.MapAvailabilityChecker
import com.worldwidewaves.shared.map.IosMapLibreAdapter
import com.worldwidewaves.shared.map.IosPlatformMapManager
import com.worldwidewaves.shared.map.IosWwwLocationProvider
import com.worldwidewaves.shared.map.MapLibreAdapter
import com.worldwidewaves.shared.map.MapStateManager
import com.worldwidewaves.shared.map.PlatformMapManager
import com.worldwidewaves.shared.map.WWWLocationProvider
import com.worldwidewaves.shared.sound.IosSoundPlayer
import com.worldwidewaves.shared.sound.SoundPlayer
import com.worldwidewaves.shared.ui.DebugTabScreen
import com.worldwidewaves.shared.utils.ImageResolver
import com.worldwidewaves.shared.utils.IosImageResolver
import com.worldwidewaves.shared.viewmodels.EventsViewModel
import com.worldwidewaves.shared.viewmodels.IosMapViewModel
import com.worldwidewaves.shared.viewmodels.MapViewModel
import org.jetbrains.compose.resources.DrawableResource
import org.koin.dsl.module
import platform.UIKit.UIDevice

val IosModule =
    module {
        single<SoundPlayer> { IosSoundPlayer() }
        single<ImageResolver<DrawableResource>> { IosImageResolver() }
        single<WWWLocationProvider> { IosWwwLocationProvider() }

        // Note: PlatformEnabler is injected into koin by Swift IOS

        // Platform descriptor for iOS
        single<WWWPlatform> {
            val device = UIDevice.currentDevice
            WWWPlatform("iOS ${device.systemVersion}", get())
        }

        single {
            EventsViewModel(
                eventsRepository = get(),
                getSortedEventsUseCase = get(),
                filterEventsUseCase = get(),
                checkEventFavoritesUseCase = get(),
                platform = get(),
            )
        }

        // ChoreographyManager for iOS - using DrawableResource for cross-platform compatibility
        single(createdAtStart = true) { ChoreographyManager<DrawableResource>() }

        // iOS Map Availability Checker (production-grade iOS implementation)
        single<MapAvailabilityChecker> { IosMapAvailabilityChecker() }

        // Debug screen - iOS implementation
        single<DebugTabScreen?> { DebugTabScreen() }

        // Data persistence
        single<FavoriteEventsStore> { IosFavoriteEventsStore() }

        // Map services
        single<PlatformMapManager> { IosPlatformMapManager() }
        single<MapLibreAdapter<Any>> { IosMapLibreAdapter() }
        single { MapStateManager(get()) }
        // Note: NativeMapViewProvider is registered by iOS app (SwiftNativeMapViewProvider)
        // If not registered, MapViewFactory will use IosNativeMapViewProvider as fallback

        // iOS MapViewModel
        single<MapViewModel> { IosMapViewModel(get()) }
    }
