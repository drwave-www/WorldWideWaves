package com.worldwidewaves.shared.events

/*
 * Copyright 2024 DrWave
 *
 * WorldWideWaves is an ephemeral mobile app designed to orchestrate human waves through cities and countries,
 * culminating in a global wave. The project aims to transcend physical and cultural boundaries, fostering unity,
 * community, and shared human experience by leveraging real-time coordination and location-based services.
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

import com.worldwidewaves.shared.data.InitFavoriteEvent
import com.worldwidewaves.shared.events.utils.CoroutineScopeProvider
import com.worldwidewaves.shared.events.utils.DefaultCoroutineScopeProvider
import com.worldwidewaves.shared.events.utils.EventsConfigurationProvider
import com.worldwidewaves.shared.events.utils.EventsDecoder
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.inject
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue

class WWWEventsTest : KoinTest {

    private val dispatcher = StandardTestDispatcher() // Create dispatcher instance

    private val mockedKoinDeclaration: KoinAppDeclaration = {
        modules(
            module {
                single { spyk(WWWEvents()) }
                single<InitFavoriteEvent> { mockk() }
                single<EventsConfigurationProvider> {
                    spyk(object : EventsConfigurationProvider {
                        override suspend fun geoEventsConfiguration(): String = "[]" // Empty events
                    })
                }
                single<EventsDecoder> { spyk(object : EventsDecoder{
                    override fun decodeFromJson(jsonString: String): List<WWWEvent> = emptyList()
                }) }
                single<CoroutineScopeProvider> { DefaultCoroutineScopeProvider(dispatcher, dispatcher) }
            }
        )
    }

    // ----------------------------

    @OptIn(ExperimentalCoroutinesApi::class)
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        startKoin { mockedKoinDeclaration() }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @AfterTest
    fun tearDown() {
        stopKoin()
        Dispatchers.resetMain()
    }

    // ----------------------------

    @Test
    fun `WWWEvents should be initialized with empty events`() {
        // GIVEN
        val events: WWWEvents by inject()

        // THEN
        assertTrue(events.flow().value.isEmpty())
        assertTrue(events.list().isEmpty())
        assertTrue(events.getEventById("1") == null)
    }

    @Test
    fun `WWWEvents should callback on loaded through loadEvents`()  = runTest {
        // GIVEN
        var callbackCalled = false // capture() doesn't work here
        val events: WWWEvents by inject()

        // WHEN
        events.loadEvents {
            callbackCalled = true
        }

        testScheduler.advanceUntilIdle()

        // THEN
        assertTrue { callbackCalled }
    }

}