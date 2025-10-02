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

import com.worldwidewaves.shared.WWWShutdownHandler
import com.worldwidewaves.shared.domain.observation.DefaultPositionObserver
import com.worldwidewaves.shared.domain.observation.PositionObserver
import com.worldwidewaves.shared.domain.progression.DefaultWaveProgressionTracker
import com.worldwidewaves.shared.domain.progression.WaveProgressionTracker
import com.worldwidewaves.shared.domain.scheduling.DefaultObservationScheduler
import com.worldwidewaves.shared.domain.scheduling.ObservationScheduler
import com.worldwidewaves.shared.domain.state.DefaultEventStateManager
import com.worldwidewaves.shared.domain.state.EventStateManager
import com.worldwidewaves.shared.events.config.DefaultEventsConfigurationProvider
import com.worldwidewaves.shared.events.config.EventsConfigurationProvider
import com.worldwidewaves.shared.events.data.DefaultGeoJsonDataProvider
import com.worldwidewaves.shared.events.data.DefaultMapDataProvider
import com.worldwidewaves.shared.events.data.GeoJsonDataProvider
import com.worldwidewaves.shared.events.data.MapDataProvider
import com.worldwidewaves.shared.events.decoding.DefaultEventsDecoder
import com.worldwidewaves.shared.events.decoding.EventsDecoder
import com.worldwidewaves.shared.events.utils.CoroutineScopeProvider
import com.worldwidewaves.shared.events.utils.DefaultCoroutineScopeProvider
import com.worldwidewaves.shared.events.utils.IClock
import com.worldwidewaves.shared.events.utils.SystemClock
import com.worldwidewaves.shared.position.PositionManager
import com.worldwidewaves.shared.utils.CloseableCoroutineScope
import org.koin.dsl.module

val helpersModule =
    module {
        single<CoroutineScopeProvider> { DefaultCoroutineScopeProvider() }
        single<PositionManager> { PositionManager(get()) }
        single<WaveProgressionTracker> { DefaultWaveProgressionTracker(get()) }
        single<PositionObserver> { DefaultPositionObserver(get(), get(), get()) }
        single<EventStateManager> { DefaultEventStateManager(get(), get()) }
        single<ObservationScheduler> { DefaultObservationScheduler(get()) }
        factory { WWWShutdownHandler(get()) }
        single<IClock> { SystemClock() }
        single<EventsConfigurationProvider> { DefaultEventsConfigurationProvider(get()) }
        single<GeoJsonDataProvider> { DefaultGeoJsonDataProvider() }
        single<MapDataProvider> { DefaultMapDataProvider() }
        single<EventsDecoder> { DefaultEventsDecoder() }
        single { CloseableCoroutineScope() }
    }
