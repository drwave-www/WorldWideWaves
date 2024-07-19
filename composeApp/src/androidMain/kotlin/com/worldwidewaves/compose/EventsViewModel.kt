package com.worldwidewaves.compose

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.worldwidewaves.shared.events.WWWEvent
import com.worldwidewaves.shared.events.WWWEvents
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EventsViewModel : ViewModel() {

    private val _events = MutableStateFlow<List<WWWEvent>>(emptyList())
    val events: StateFlow<List<WWWEvent>> = _events.asStateFlow()

    init {
        loadEvents()
    }

    private fun loadEvents() {
        viewModelScope.launch {
            WWWEvents().eventsFlow.collect { eventsList ->
                _events.value = eventsList
            }
        }
    }
}
