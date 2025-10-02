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
import com.worldwidewaves.shared.choreographies.ChoreographyManager
import com.worldwidewaves.shared.data.DataStoreFactory
import com.worldwidewaves.shared.data.DefaultDataStoreFactory
import com.worldwidewaves.shared.data.FavoriteEventsStore
import com.worldwidewaves.shared.data.FavoriteEventsStoreAndroid
import com.worldwidewaves.shared.data.keyValueStorePath
import com.worldwidewaves.shared.sound.SoundPlayer
import com.worldwidewaves.shared.sound.SoundPlayerAndroid
import com.worldwidewaves.shared.utils.ImageResolver
import com.worldwidewaves.shared.utils.ImageResolverAndroid
import org.jetbrains.compose.resources.DrawableResource
import org.koin.dsl.module

val androidModule =
    module {
        single<WWWPlatform> {
            WWWPlatform("Android ${Build.VERSION.SDK_INT}", get())
        }
        single<ImageResolver<DrawableResource>> { ImageResolverAndroid() }
        single(createdAtStart = true) { ChoreographyManager<DrawableResource>() }

        single<SoundPlayer> { SoundPlayerAndroid(get()) }

        single<DataStoreFactory> { DefaultDataStoreFactory() }
        single { get<DataStoreFactory>().create { keyValueStorePath() } }
        single<FavoriteEventsStore> { FavoriteEventsStoreAndroid(get()) }
    }
