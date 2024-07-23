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

import com.worldwidewaves.shared.generated.resources.Res.readBytes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.ExperimentalResourceApi
import kotlinx.coroutines.*

class WWWEvents {

    private val _eventsFlow = MutableStateFlow<List<WWWEvent>>(emptyList())
    val eventsFlow = _eventsFlow.asStateFlow()
    private var loadJob : Job? = null

    init {
        loadEvents()
    }

    fun resetEventsFlow() {
        _eventsFlow.value = emptyList()
    }

    fun getLoadingJob(): Job? = loadJob

    @OptIn(ExperimentalResourceApi::class)
    fun loadEvents(): WWWEvents {
        if (loadJob == null)
            loadJob = CoroutineScope(Dispatchers.IO).launch {
                val eventsConf = readBytes("files/events.json").decodeToString()
                val loadedEvents = Json {
                    ignoreUnknownKeys = true
                }.decodeFromString<List<WWWEvent>>(eventsConf)
                loadedEvents.forEach { // Read favorite status from DataStore
                    it.initFavoriteStatus()
                }
                _eventsFlow.value = loadedEvents
            }

        return this
    }

    // ---------------------------

    fun events(): StateFlow<List<WWWEvent>> {
        return eventsFlow
    }

    fun getEventById(id: String): WWWEvent? {
        return eventsFlow.value.find { it.id == id }
    }

}