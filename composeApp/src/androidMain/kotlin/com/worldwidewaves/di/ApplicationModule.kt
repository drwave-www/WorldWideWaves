package com.worldwidewaves.di

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

import com.worldwidewaves.BuildConfig
import com.worldwidewaves.shared.PlatformEnabler
import com.worldwidewaves.shared.domain.usecases.IMapAvailabilityChecker
import com.worldwidewaves.shared.ui.DebugScreen
import com.worldwidewaves.shared.utils.CloseableCoroutineScope
import com.worldwidewaves.shared.utils.Log
import com.worldwidewaves.shared.viewmodels.EventsViewModel
import com.worldwidewaves.utils.AndroidPlatformEnabler
import com.worldwidewaves.utils.AndroidWWWLocationProvider
import com.worldwidewaves.utils.MapAvailabilityChecker
import com.worldwidewaves.utils.WWWSimulationEnabledLocationEngine
import com.worldwidewaves.viewmodels.AndroidMapViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val applicationModule =
    module {

        single<PlatformEnabler> { AndroidPlatformEnabler() }
        single<IMapAvailabilityChecker> { get<MapAvailabilityChecker>() }

        // Map availability checker as a singleton
        single {
            MapAvailabilityChecker(androidContext()).apply {
                // Register for cleanup when the app is terminated
                get<CloseableCoroutineScope>().registerForCleanup {
                    this.destroy()
                }
            }
        }

        viewModel {
            EventsViewModel(
                eventsRepository = get(),
                getSortedEventsUseCase = get(),
                filterEventsUseCase = get(),
                checkEventFavoritesUseCase = get(),
                platform = get(),
            )
        }

        viewModel { AndroidMapViewModel(get()) }

        // Location engine and provider for Android
        single { WWWSimulationEnabledLocationEngine(get()) }
        factory { AndroidWWWLocationProvider() }

        // Debug screen - only in debug builds
        single<DebugScreen?> {
            val isDebug = BuildConfig.DEBUG
            Log.d("ApplicationModule", "Debug screen injection: BuildConfig.DEBUG=$isDebug")
            if (isDebug) {
                DebugScreen()
            } else {
                null
            }
        }
    }
