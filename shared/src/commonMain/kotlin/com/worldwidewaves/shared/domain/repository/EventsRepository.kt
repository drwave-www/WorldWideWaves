package com.worldwidewaves.shared.domain.repository

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

/**
 * Repository interface for accessing and managing events data.
 *
 * This repository provides a clean abstraction over data sources,
 * handling caching, error recovery, and data synchronization.
 * It follows the Repository pattern to separate business logic
 * from data access concerns.
 */
interface EventsRepository {

    /**
     * Observes all available events as a reactive stream.
     *
     * @return Flow of event lists, automatically updated when data changes
     */
    suspend fun getEvents(): Flow<List<IWWWEvent>>

    /**
     * Loads events from all data sources, triggering cache refresh.
     *
     * @param onLoadingError Callback invoked when loading fails
     */
    suspend fun loadEvents(onLoadingError: (Exception) -> Unit)

    /**
     * Gets a specific event by its identifier.
     *
     * @param eventId Unique identifier of the event
     * @return Flow emitting the event if found, empty if not found
     */
    suspend fun getEvent(eventId: String): Flow<IWWWEvent?>

    /**
     * Refreshes all event data from remote sources.
     *
     * @return Result indicating success or failure
     */
    suspend fun refreshEvents(): Result<Unit>

    /**
     * Gets cached events count for performance monitoring.
     *
     * @return Number of events currently cached
     */
    suspend fun getCachedEventsCount(): Int

    /**
     * Clears all cached event data.
     */
    suspend fun clearCache()

    /**
     * Checks if events are currently being loaded.
     *
     * @return Flow indicating loading state
     */
    fun isLoading(): Flow<Boolean>

    /**
     * Observes the last error that occurred during data operations.
     *
     * @return Flow of the most recent error, null if no errors
     */
    fun getLastError(): Flow<Exception?>
}