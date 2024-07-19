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

class WWWEvents {

    private val _eventsFlow = MutableStateFlow<List<WWWEvent>>(emptyList())
    val eventsFlow = _eventsFlow.asStateFlow()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            loadEvents()
        }
    }

    @OptIn(ExperimentalResourceApi::class)
    private suspend fun loadEvents() {
        val eventsConf = readBytes("files/events.json").decodeToString()
        val loadedEvents = Json {
            ignoreUnknownKeys = true
        }.decodeFromString<List<WWWEvent>>(eventsConf)
        _eventsFlow.value = loadedEvents
    }

    // ---------------------------

    fun events(): StateFlow<List<WWWEvent>> {
        return eventsFlow
    }

    fun getEventById(id: String): WWWEvent? {
        return eventsFlow.value.find { it.id == id }
    }

}