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

import com.worldwidewaves.shared.data.DataStoreFactory
import com.worldwidewaves.shared.data.FavoriteEventsStore
import com.worldwidewaves.shared.data.HiddenMapsStore
import com.worldwidewaves.shared.data.InitFavoriteEvent
import com.worldwidewaves.shared.data.SetEventFavorite
import com.worldwidewaves.shared.data.TestDataStoreFactory
import org.koin.dsl.module

/**
 * Test-specific DataStore module that provides isolated DataStore instances for each test.
 * This prevents test interference and enables parallel test execution.
 */
val testDatastoreModule =
    module {
        single<DataStoreFactory> { TestDataStoreFactory() }
        factory { get<DataStoreFactory>().create { "/tmp/test_${System.currentTimeMillis()}_${kotlin.random.Random.nextInt()}.preferences_pb" } }

        // Persistent stores ------------------------------------------------------

        factory { FavoriteEventsStore(get()) }
        factory { HiddenMapsStore(get()) }

        factory { InitFavoriteEvent(favoriteEventsStore = get()) }
        factory { SetEventFavorite(favoriteEventsStore = get()) }
    }