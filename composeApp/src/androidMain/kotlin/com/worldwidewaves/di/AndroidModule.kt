package com.worldwidewaves.di

import com.worldwidewaves.compose.AboutFaqScreen
import com.worldwidewaves.compose.AboutInfoScreen
import com.worldwidewaves.compose.AboutScreen
import com.worldwidewaves.compose.EventsListScreen
import com.worldwidewaves.compose.SettingsScreen
import com.worldwidewaves.models.EventsViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

/*
 * Copyright 2024 DrWave
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

val androidModule = module {
    single { EventsListScreen(viewModel = get(), setEventFavorite = get()) }
    viewModel { EventsViewModel(wwwEvents = get()) }

    single { SettingsScreen() }
    single { AboutScreen(get(), get()) }
    single { AboutInfoScreen() }
    single { AboutFaqScreen() }

}