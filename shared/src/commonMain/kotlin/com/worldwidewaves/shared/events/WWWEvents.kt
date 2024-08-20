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

import com.worldwidewaves.shared.InitFavoriteEvent
import com.worldwidewaves.shared.WWWGlobals.Companion.FS_EVENTS_CONF
import com.worldwidewaves.shared.generated.resources.Res
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.ExperimentalResourceApi

// ---------------------------

interface EventsConfigurationProvider {
    suspend fun geoEventsConfiguration(): String
}

class DefaultEventsConfigurationProvider : EventsConfigurationProvider {
    @OptIn(ExperimentalResourceApi::class)
    override suspend fun geoEventsConfiguration(): String {
        return withContext(Dispatchers.IO) {
            Res.readBytes(FS_EVENTS_CONF).decodeToString()
        }
    }
}

// ---------------------------

class WWWEvents(
    private val initFavoriteEvent: InitFavoriteEvent,
    private val eventsConfigurationProvider: EventsConfigurationProvider = DefaultEventsConfigurationProvider()
) {

    private val _eventsFlow = MutableStateFlow<List<WWWEvent>>(emptyList())
    val eventsFlow = _eventsFlow.asStateFlow()

    init {
        loadEvents()
    }

    fun resetEventsFlow() {
        _eventsFlow.value = emptyList()
    }

    // ---------------------------

    private var loadJob: Job? = null
    private val jsonDecoder = Json { ignoreUnknownKeys = true }

    private fun loadEvents() = apply {
        loadJob = loadJob ?: loadEventsJob()
    }

    private fun loadEventsJob() = CoroutineScope(Dispatchers.IO).launch {
        val eventsJsonString = eventsConfigurationProvider.geoEventsConfiguration()
        val events = jsonDecoder.decodeFromString<List<WWWEvent>>(eventsJsonString)

        val validationResults = isValidEventsData(events)

        validationResults.filterValues { !it.first }
            .mapNotNull { it.value.second }
            .forEach { errorMessage ->
                Napier.e("Validation Error: $errorMessage")
            }

        _eventsFlow.value = validationResults.filterValues { it.first }
            .keys
            .onEach { initFavoriteEvent.call(it) }
            .toList()
    }

    // ---------------------------

    fun events(): StateFlow<List<WWWEvent>> {
        return eventsFlow
    }

    fun getEventById(id: String): WWWEvent? {
        return eventsFlow.value.find { it.id == id }
    }

    fun invokeWhenLoaded(function: () -> Job) {
        this.loadJob?.invokeOnCompletion { function() }
    }

    // ---------------------------

    private fun isValidEventsData(events: List<WWWEvent>): Map<WWWEvent, Pair<Boolean, String?>> {
        return events.associateWith { event ->
            when {
                event.id.isEmpty() -> Pair(false, "ID is empty")
                event.type.isEmpty() -> Pair(false, "Type is empty")
                event.location.isEmpty() -> Pair(false, "Location is empty")
                !event.date.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) -> Pair(false, "Date format is invalid")
                !event.startHour.matches(Regex("\\d{2}:\\d{2}")) -> Pair(false, "Start hour format is invalid")
                event.speed <= 0 -> Pair(false, "Speed must be a positive integer")
                event.description.isEmpty() -> Pair(false, "Description is empty")
                event.instagramAccount.isEmpty() -> Pair(false, "Instagram account is empty")
                !event.instagramUrl.startsWith("https://") -> Pair(false, "Instagram URL must start with https://")
                event.instagramHashtag.isEmpty() -> Pair(false, "Instagram hashtag is empty")
                event.mapBbox.split(",").size != 4 -> Pair(false, "Map Bbox must have 4 elements")
                event.mapCenter.split(",").size != 2 -> Pair(false, "Map Center must have 2 elements")
                event.mapOsmadminid.toString().toIntOrNull() == null -> Pair(false, "Map Osmadminid must be an integer")
                event.mapMinzoom.toString().toDoubleOrNull() == null -> Pair(false, "Map Minzoom must be a double")
                event.mapMaxzoom.toString().toDoubleOrNull() == null -> Pair(false, "Map Maxzoom must be a double")
                event.mapLanguage.isEmpty() -> Pair(false, "Map language is empty")
                event.mapOsmarea.isEmpty() -> Pair(false, "Map Osmarea is empty")
                event.timeZone.isEmpty() -> Pair(false, "Time zone is empty")
                else -> Pair(true, null)
            }
        }
    }

}