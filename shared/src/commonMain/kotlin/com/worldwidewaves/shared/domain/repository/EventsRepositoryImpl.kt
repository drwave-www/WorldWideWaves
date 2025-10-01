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
import com.worldwidewaves.shared.events.WWWEvents
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Implementation of EventsRepository that wraps the existing WWWEvents data source.
 *
 * This implementation provides a clean repository interface over the legacy WWWEvents
 * system, adding proper error handling, caching, and state management while maintaining
 * backward compatibility.
 *
 * Key features:
 * - Thread-safe event caching with mutex protection
 * - Reactive state management for loading and error states
 * - Comprehensive error handling and recovery
 * - Memory-efficient event lookup by ID
 */
class EventsRepositoryImpl(
    private val wwwEvents: WWWEvents,
) : EventsRepository {
    private val cacheMutex = Mutex()
    private var cachedEvents: List<IWWWEvent> = emptyList()
    private var cacheValid = false

    // Background scope for cache operations to prevent main thread blocking
    private val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _isLoading = MutableStateFlow(false)
    private val _lastError = MutableStateFlow<Exception?>(null)

    override suspend fun getEvents(): Flow<List<IWWWEvent>> = wwwEvents.flow()

    override suspend fun loadEvents(onLoadingError: (Exception) -> Unit) {
        _isLoading.value = true
        _lastError.value = null

        try {
            wwwEvents.loadEvents(
                onLoadingError = { exception ->
                    _lastError.value = exception
                    _isLoading.value = false
                    onLoadingError(exception)
                },
                onLoaded = {
                    _isLoading.value = false
                    backgroundScope.launch { updateCache() }
                },
                onTermination = { exception ->
                    _isLoading.value = false
                    exception?.let {
                        _lastError.value = it
                    }
                },
            )
        } catch (e: Exception) {
            _isLoading.value = false
            _lastError.value = e
            onLoadingError(e)
        }
    }

    override suspend fun getEvent(eventId: String): Flow<IWWWEvent?> =
        wwwEvents.flow().map { events ->
            events.find { it.id == eventId }
        }

    override suspend fun refreshEvents(): Result<Unit> =
        try {
            _isLoading.value = true
            _lastError.value = null
            invalidateCache()

            // Use CompletableDeferred to wait for async loading completion
            val loadDeferred = CompletableDeferred<Result<Unit>>()

            wwwEvents.loadEvents(
                onLoadingError = { exception ->
                    _lastError.value = exception
                    _isLoading.value = false
                    loadDeferred.complete(Result.failure(exception))
                },
                onLoaded = {
                    _isLoading.value = false
                    backgroundScope.launch { updateCache() }
                    loadDeferred.complete(Result.success(Unit))
                },
                onTermination = { exception ->
                    _isLoading.value = false
                    exception?.let {
                        _lastError.value = it
                        loadDeferred.complete(Result.failure(it))
                    } ?: loadDeferred.complete(Result.success(Unit))
                },
            )

            // Wait for loading to complete
            loadDeferred.await()
        } catch (e: Exception) {
            _isLoading.value = false
            _lastError.value = e
            Result.failure(e)
        }

    override suspend fun getCachedEventsCount(): Int =
        cacheMutex.withLock {
            if (cacheValid) cachedEvents.size else wwwEvents.list().size
        }

    override suspend fun clearCache() {
        cacheMutex.withLock {
            cachedEvents = emptyList()
            cacheValid = false
        }
    }

    override fun isLoading(): Flow<Boolean> = _isLoading.asStateFlow()

    override fun getLastError(): Flow<Exception?> = _lastError.asStateFlow()

    /**
     * Updates the internal cache with the latest events from WWWEvents.
     * This method is thread-safe and should be called when events are loaded.
     */
    private suspend fun updateCache() {
        // Cache update doesn't require suspending since we're just copying data
        // that's already loaded in memory from WWWEvents
        val currentEvents = wwwEvents.list()
        cacheMutex.withLock {
            cachedEvents = currentEvents
            cacheValid = true
        }
    }

    /**
     * Invalidates the internal cache, forcing a reload on next access.
     */
    private suspend fun invalidateCache() {
        cacheMutex.withLock {
            cacheValid = false
        }
    }
}
