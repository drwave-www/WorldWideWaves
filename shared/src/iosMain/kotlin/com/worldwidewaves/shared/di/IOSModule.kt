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

import com.worldwidewaves.shared.PlatformEnabler
import com.worldwidewaves.shared.WWWPlatform
import com.worldwidewaves.shared.choreographies.ChoreographyManager
import com.worldwidewaves.shared.debugBuild
import com.worldwidewaves.shared.domain.usecases.IMapAvailabilityChecker
import com.worldwidewaves.shared.map.IOSMapLibreAdapter
import com.worldwidewaves.shared.map.IOSPlatformMapManager
import com.worldwidewaves.shared.map.IOSWWWLocationProvider
import com.worldwidewaves.shared.map.MapLibreAdapter
import com.worldwidewaves.shared.map.MapStateManager
import com.worldwidewaves.shared.map.PlatformMapManager
import com.worldwidewaves.shared.map.WWWLocationProvider
import com.worldwidewaves.shared.sound.IOSSoundPlayer
import com.worldwidewaves.shared.sound.SoundPlayer
import com.worldwidewaves.shared.ui.DebugTabScreen
import com.worldwidewaves.shared.utils.IOSImageResolver
import com.worldwidewaves.shared.utils.IOSPlatformEnabler
import com.worldwidewaves.shared.utils.ImageResolver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.koin.dsl.module
import platform.UIKit.UIDevice
import platform.UIKit.UIImage

val IOSModule =
    module {
        single<SoundPlayer> { IOSSoundPlayer() }
        single<ImageResolver<UIImage>> { IOSImageResolver() }
        single<WWWLocationProvider> { IOSWWWLocationProvider() }

        single<PlatformEnabler> { IOSPlatformEnabler() }

        // Platform descriptor for iOS
        single<WWWPlatform> {
            debugBuild()
            val device = UIDevice.currentDevice
            WWWPlatform("iOS ${device.systemVersion}", get())
        }

        // ChoreographyManager for iOS
        single(createdAtStart = true) { ChoreographyManager<UIImage>() }

        // iOS Map Availability Checker (simplified for iOS)
        single<IMapAvailabilityChecker> {
            object : IMapAvailabilityChecker {
                override val mapStates: StateFlow<Map<String, Boolean>> =
                    MutableStateFlow(emptyMap())

                override fun refreshAvailability() {
                    // iOS: Maps are bundled, always available
                }

                override fun isMapDownloaded(eventId: String): Boolean = true

                override fun getDownloadedMaps(): List<String> = emptyList()

                override fun trackMaps(mapIds: Collection<String>) {
                    // iOS: No-op, maps are bundled
                }
            }
        }

        // Debug screen - iOS implementation
        single<DebugTabScreen?> { DebugTabScreen() }

        // Map services
        single<PlatformMapManager> { IOSPlatformMapManager() }
        single<MapLibreAdapter<Any>> { IOSMapLibreAdapter() }
        single { MapStateManager(get()) }
    }
