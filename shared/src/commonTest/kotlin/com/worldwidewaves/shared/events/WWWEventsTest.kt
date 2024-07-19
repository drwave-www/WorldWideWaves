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

package com.worldwidewaves.shared.events

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class WWWEventsTest {

    private lateinit var wwwEvents: WWWEvents

    @BeforeTest
    fun setUp() {
        wwwEvents = WWWEvents().apply {
            resetEventsFlow()
        }
    }

    @Test
    fun loadEventsPopulatesEventsFlow() = runTest {
        wwwEvents.loadEvents()
        val events = wwwEvents.events().value
        assertTrue(events.isNotEmpty(), "Events flow should be populated after loading events.")
    }

    @Test
    fun getEventByIdReturnsNullForUnknownId() = runTest {
        wwwEvents.loadEvents()
        val event = wwwEvents.getEventById("unknown_id")
        assertNull(event, "Event with unknown id should not be found.")
    }

}