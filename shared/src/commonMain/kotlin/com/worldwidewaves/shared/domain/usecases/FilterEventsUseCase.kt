package com.worldwidewaves.shared.domain.usecases

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

import com.worldwidewaves.shared.events.IWWWEvent

/**
 * Use case for filtering events based on various criteria.
 *
 * This use case encapsulates the business logic for event filtering, providing
 * consistent filtering behavior across different UI components that display events.
 *
 * @param mapAvailabilityChecker Service for checking map download status
 */
class FilterEventsUseCase(
    private val mapAvailabilityChecker: MapAvailabilityChecker
) {
    /**
     * Filters a list of events based on the specified criteria.
     *
     * @param events List of events to filter
     * @param criteria Filtering criteria to apply
     * @return Filtered list of events
     */
    suspend operator fun invoke(
        events: List<IWWWEvent>,
        criteria: EventFilterCriteria
    ): List<IWWWEvent> {
        // Refresh map availability data before filtering
        mapAvailabilityChecker.refreshAvailability()

        return events.filter { event ->
            when {
                criteria.onlyFavorites -> event.favorite
                criteria.onlyDownloaded -> mapAvailabilityChecker.isMapDownloaded(event.id)
                criteria.onlyRunning -> event.isRunning()
                criteria.onlyUpcoming -> !event.isDone() && !event.isRunning()
                criteria.onlyCompleted -> event.isDone()
                else -> true // Show all events when no specific criteria
            }
        }
    }

    /**
     * Convenience method for filtering with simple boolean flags.
     *
     * @param events List of events to filter
     * @param onlyFavorites Show only favorite events
     * @param onlyDownloaded Show only events with downloaded maps
     * @return Filtered list of events
     */
    suspend fun filter(
        events: List<IWWWEvent>,
        onlyFavorites: Boolean = false,
        onlyDownloaded: Boolean = false
    ): List<IWWWEvent> {
        return invoke(
            events = events,
            criteria = EventFilterCriteria(
                onlyFavorites = onlyFavorites,
                onlyDownloaded = onlyDownloaded
            )
        )
    }
}

/**
 * Data class representing event filtering criteria.
 *
 * @param onlyFavorites Filter to show only favorite events
 * @param onlyDownloaded Filter to show only events with downloaded maps
 * @param onlyRunning Filter to show only currently running events
 * @param onlyUpcoming Filter to show only upcoming events (not running or completed)
 * @param onlyCompleted Filter to show only completed events
 * @param eventIds Optional list of specific event IDs to include (overrides other filters)
 */
data class EventFilterCriteria(
    val onlyFavorites: Boolean = false,
    val onlyDownloaded: Boolean = false,
    val onlyRunning: Boolean = false,
    val onlyUpcoming: Boolean = false,
    val onlyCompleted: Boolean = false,
    val eventIds: List<String>? = null
)

/**
 * Interface for checking map availability/download status.
 * This interface will be implemented by platform-specific map checkers.
 */
interface MapAvailabilityChecker {
    fun refreshAvailability()
    fun isMapDownloaded(eventId: String): Boolean
    fun getDownloadedMaps(): List<String>
}
