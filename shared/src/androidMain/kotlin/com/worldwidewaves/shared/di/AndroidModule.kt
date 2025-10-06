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

import android.os.Build
import com.worldwidewaves.shared.WWWPlatform
import com.worldwidewaves.shared.choreographies.ChoreographySequenceBuilder
import com.worldwidewaves.shared.data.AndroidFavoriteEventsStore
import com.worldwidewaves.shared.data.DataStoreFactory
import com.worldwidewaves.shared.data.DefaultDataStoreFactory
import com.worldwidewaves.shared.data.FavoriteEventsStore
import com.worldwidewaves.shared.data.keyValueStorePath
import com.worldwidewaves.shared.sound.AndroidSoundPlayer
import com.worldwidewaves.shared.sound.SoundPlayer
import com.worldwidewaves.shared.utils.AndroidImageResolver
import com.worldwidewaves.shared.utils.ImageResolver
import org.jetbrains.compose.resources.DrawableResource
import org.koin.dsl.module

val androidModule =
    module {
        single<WWWPlatform> {
            WWWPlatform("Android ${Build.VERSION.SDK_INT}", get())
        }
        single<ImageResolver<DrawableResource>> { AndroidImageResolver() }
        single(createdAtStart = true) { ChoreographySequenceBuilder<DrawableResource>() }

        single<SoundPlayer> { AndroidSoundPlayer(get()) }

        single<DataStoreFactory> { DefaultDataStoreFactory() }
        single { get<DataStoreFactory>().create { keyValueStorePath() } }
        single<FavoriteEventsStore> { AndroidFavoriteEventsStore(get()) }
    }

/**
 * Initialize simulation mode based on build configuration.
 * Call this after Koin initialization with the BuildConfig flag.
 */
fun initializeSimulationMode(
    platform: WWWPlatform,
    enableSimulation: Boolean,
) {
    if (enableSimulation) {
        platform.enableSimulationMode()
    }
}
