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
import com.worldwidewaves.shared.sound.AndroidSoundPlayer
import com.worldwidewaves.shared.choreographies.ChoreographyManager
import com.worldwidewaves.shared.sound.SoundPlayer
import com.worldwidewaves.shared.debugBuild
import com.worldwidewaves.shared.utils.AndroidImageResolver
import com.worldwidewaves.shared.utils.ImageResolver
import org.jetbrains.compose.resources.DrawableResource
import org.koin.dsl.module

val androidModule = module {
    single<WWWPlatform> {
        debugBuild()
        WWWPlatform("Android ${Build.VERSION.SDK_INT}")
    }
    single<ImageResolver<DrawableResource>> { AndroidImageResolver() }
    single(createdAtStart = true) { ChoreographyManager<DrawableResource>() }

    single<SoundPlayer> { AndroidSoundPlayer(get()) }
}