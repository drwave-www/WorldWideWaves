package com.worldwidewaves.shared

import kotlinx.serialization.json.Json

class WWWEvents(eventsConf: String) {

    private val events: List<WWWEvent>

    init {
        events = Json { ignoreUnknownKeys = true }.decodeFromString<List<WWWEvent>>(eventsConf)
    }

    fun events(): List<WWWEvent> {
        return events
    }
}