package com.worldwidewaves.shared

import kotlinx.serialization.json.Json

class WWWEvents(eventsConf: String) {

    private val events: List<WWWEvent>
    private val ids: Map<String, WWWEvent>

    init {
        events = Json { ignoreUnknownKeys = true }.decodeFromString<List<WWWEvent>>(eventsConf)
        ids = events.associateBy { it.id }
    }

    fun events(): List<WWWEvent> {
        return events
    }

    fun getEventById(id: String): WWWEvent? {
        return events.find { it.id.toString() == id }
    }

}