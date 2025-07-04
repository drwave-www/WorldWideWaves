package com.worldwidewaves.di

import com.worldwidewaves.compose.tabs.AboutScreen
import com.worldwidewaves.compose.tabs.EventsListScreen
import com.worldwidewaves.compose.tabs.about.AboutFaqScreen
import com.worldwidewaves.compose.tabs.about.AboutInfoScreen
import com.worldwidewaves.utils.AndroidLocationProvider
import com.worldwidewaves.utils.MapAvailabilityChecker
import com.worldwidewaves.utils.WWWSimulationEnabledLocationEngine
import com.worldwidewaves.viewmodels.EventsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/*
 * Copyright 2025 DrWave
 *
 * WorldWideWaves is an ephemeral mobile app designed to orchestrate human waves through cities and
 * countries, culminating in a global wave. The project aims to transcend physical and cultural
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

val applicationModule = module {
    single { EventsListScreen(viewModel = get(), mapChecker = get(), setEventFavorite = get()) }

    viewModel { EventsViewModel(wwwEvents = get(), mapChecker = get(), platform = get()) }

    single { AboutScreen(get(), get()) }
    single { AboutInfoScreen() }
    single { AboutFaqScreen() }

    // Map availability checker as a singleton
    single {
        MapAvailabilityChecker(androidContext()).apply {
            // Register for cleanup when the app is terminated
            get<CloseableCoroutineScope>().registerForCleanup {
                this.destroy()
            }
        }
    }

    // A closeable coroutine scope for cleanup
    single { CloseableCoroutineScope() }

    // Location engine and provider for Android
    single { WWWSimulationEnabledLocationEngine(get()) }
    factory { AndroidLocationProvider() }
}

/**
 * A coroutine scope that can be closed and helps with resource cleanup
 */
class CloseableCoroutineScope : CoroutineScope {
    private val job = SupervisorJob()
    override val coroutineContext = job + Dispatchers.Main

    private val cleanupActions = mutableListOf<() -> Unit>()

    fun registerForCleanup(action: () -> Unit) {
        cleanupActions.add(action)
    }

    fun close() {
        cleanupActions.forEach { it() }
        job.cancel()
    }
}