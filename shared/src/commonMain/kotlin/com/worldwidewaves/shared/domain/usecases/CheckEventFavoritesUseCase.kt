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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Use case for checking and managing favorite event status.
 *
 * This use case encapsulates the business logic for determining whether
 * the current event list contains any favorite events, which is used
 * to show/hide UI elements related to favorites filtering.
 */
class CheckEventFavoritesUseCase {

    /**
     * Checks if any events in the provided list are marked as favorites.
     *
     * @param events List of events to check
     * @return true if at least one event is marked as favorite, false otherwise
     */
    suspend fun hasFavoriteEvents(events: List<IWWWEvent>): Boolean {
        return events.any { it.favorite }
    }

    /**
     * Creates a reactive flow that tracks whether any events are favorites.
     *
     * @param eventsFlow Flow of event lists to monitor
     * @return Flow emitting boolean values indicating if favorites exist
     */
    fun hasFavoriteEventsFlow(eventsFlow: Flow<List<IWWWEvent>>): Flow<Boolean> {
        return eventsFlow.map { events ->
            events.any { it.favorite }
        }
    }

    /**
     * Gets all favorite events from the provided list.
     *
     * @param events List of events to filter
     * @return List containing only events marked as favorites
     */
    suspend fun getFavoriteEvents(events: List<IWWWEvent>): List<IWWWEvent> {
        return events.filter { it.favorite }
    }

    /**
     * Gets the count of favorite events.
     *
     * @param events List of events to count
     * @return Number of events marked as favorites
     */
    suspend fun getFavoriteEventsCount(events: List<IWWWEvent>): Int {
        return events.count { it.favorite }
    }

    /**
     * Creates a reactive flow that tracks the count of favorite events.
     *
     * @param eventsFlow Flow of event lists to monitor
     * @return Flow emitting the count of favorite events
     */
    fun getFavoriteEventsCountFlow(eventsFlow: Flow<List<IWWWEvent>>): Flow<Int> {
        return eventsFlow.map { events ->
            events.count { it.favorite }
        }
    }
}