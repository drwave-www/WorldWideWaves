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
import com.worldwidewaves.shared.ui.AboutScreen
import com.worldwidewaves.shared.ui.EventsListScreen
import org.koin.dsl.module

/**
 * UI-related dependencies for shared components.
 * This includes basic Use Cases and Repository implementations
 * that are needed for shared UI components like EventsListScreen.
 *
 * Note: EventsViewModel is kept in platform-specific modules since it
 * may have platform-specific dependencies like MapAvailabilityChecker.
 */
val uiModule =
    module {
        // Repository layer
        single<EventsRepository> { EventsRepositoryImpl(get()) }

        // Use cases layer - only the ones without platform dependencies
        single { GetSortedEventsUseCase(get()) }
        single { FilterEventsUseCase(get()) }
        single { CheckEventFavoritesUseCase() }

        single { AboutScreen(get()) }
        single { EventsListScreen(viewModel = get(), mapChecker = get(), setEventFavorite = get()) }

        // Repository layer
        single<EventsRepository> { EventsRepositoryImpl(get()) }

        // Use cases layer
        single { GetSortedEventsUseCase(get()) }
        single { FilterEventsUseCase(get()) }
        single { CheckEventFavoritesUseCase() }

    }
