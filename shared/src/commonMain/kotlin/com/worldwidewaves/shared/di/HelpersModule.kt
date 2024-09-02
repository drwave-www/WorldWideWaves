package com.worldwidewaves.shared.di

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

import com.worldwidewaves.shared.events.utils.CoroutineScopeProvider
import com.worldwidewaves.shared.events.utils.DefaultCoroutineScopeProvider
import com.worldwidewaves.shared.events.utils.DefaultEventsConfigurationProvider
import com.worldwidewaves.shared.events.utils.DefaultEventsDecoder
import com.worldwidewaves.shared.events.utils.DefaultGeoJsonDataProvider
import com.worldwidewaves.shared.events.utils.DefaultMapDataProvider
import com.worldwidewaves.shared.events.utils.EventsConfigurationProvider
import com.worldwidewaves.shared.events.utils.EventsDecoder
import com.worldwidewaves.shared.events.utils.GeoJsonDataProvider
import com.worldwidewaves.shared.events.utils.IClock
import com.worldwidewaves.shared.events.utils.MapDataProvider
import com.worldwidewaves.shared.events.utils.SystemClock
import org.koin.dsl.module

val helpersModule = module {
    single<CoroutineScopeProvider> { DefaultCoroutineScopeProvider() }
    single<IClock> { SystemClock() }
    single<EventsConfigurationProvider> { DefaultEventsConfigurationProvider(get()) }
    single<GeoJsonDataProvider> { DefaultGeoJsonDataProvider() }
    single<MapDataProvider> { DefaultMapDataProvider() }
    single<EventsDecoder> { DefaultEventsDecoder() }
}
