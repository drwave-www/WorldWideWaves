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
import com.worldwidewaves.shared.domain.usecases.MapAvailabilityChecker
import com.worldwidewaves.shared.ui.DebugTabScreen
import com.worldwidewaves.shared.utils.CloseableCoroutineScope
import com.worldwidewaves.shared.utils.Log
import com.worldwidewaves.shared.viewmodels.EventsViewModel
import com.worldwidewaves.utils.AndroidLocationProvider
import com.worldwidewaves.utils.AndroidMapAvailabilityChecker
import com.worldwidewaves.utils.AndroidPlatformEnabler
import com.worldwidewaves.utils.SimulationLocationEngine
import com.worldwidewaves.viewmodels.AndroidMapViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val applicationModule =
    module {

        single<PlatformEnabler> { AndroidPlatformEnabler() }
        single<MapAvailabilityChecker> { get<AndroidMapAvailabilityChecker>() }

        // Map availability checker as a singleton
        single {
            AndroidMapAvailabilityChecker(androidContext()).apply {
                // Register for cleanup when the app is terminated
                get<CloseableCoroutineScope>().registerForCleanup {
                    this.destroy()
                }
            }
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

        viewModel { AndroidMapViewModel(get()) }

        // Location engine and provider for Android
        single { SimulationLocationEngine(get()) }
        factory { AndroidLocationProvider() }

        // Debug screen - only register in debug builds (not as nullable type)
        if (BuildConfig.DEBUG) {
            Log.d("ApplicationModule", "Registering debug screen (BuildConfig.DEBUG=true)")
            single { DebugTabScreen() }
        }
    }
