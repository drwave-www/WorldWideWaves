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
import kotlin.time.ExperimentalTime

/**
 * Use case for retrieving and sorting events by their start date/time.
 *
 * This use case encapsulates the business logic for event sorting, ensuring
 * that events are consistently ordered by their scheduled start time across
 * the application.
 *
 * @param eventsRepository Repository providing access to events data
 */
@OptIn(ExperimentalTime::class)
class GetSortedEventsUseCase(
    private val eventsRepository: EventsRepository
) {
    /**
     * Retrieves all events sorted by their start date/time in ascending order.
     *
     * @return Flow of events sorted by start date, with earliest events first
     */
    suspend operator fun invoke(): Flow<List<IWWWEvent>> =
        eventsRepository.getEvents().map { events ->
            events.sortedBy { it.getStartDateTime() }
        }

    /**
     * Retrieves events sorted by start date with an optional limit.
     *
     * @param limit Maximum number of events to return (null for all events)
     * @return Flow of events sorted by start date, limited to the specified count
     */
    suspend fun invoke(limit: Int?): Flow<List<IWWWEvent>> =
        eventsRepository.getEvents().map { events ->
            val sorted = events.sortedBy { it.getStartDateTime() }
            if (limit != null && limit > 0) {
                sorted.take(limit)
            } else {
                sorted
            }
        }
}

/**
 * Repository interface for events data access.
 * This interface will be implemented in the repository layer.
 */
interface EventsRepository {
    suspend fun getEvents(): Flow<List<IWWWEvent>>
    suspend fun loadEvents(onLoadingError: (Exception) -> Unit)
}
