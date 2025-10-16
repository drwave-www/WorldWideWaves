package com.worldwidewaves.shared.events.data

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

import com.worldwidewaves.shared.WWWPlatform
import com.worldwidewaves.shared.data.readGeoJson
import com.worldwidewaves.shared.utils.Log
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

interface GeoJsonDataProvider {
    suspend fun getGeoJsonData(eventId: String): JsonObject?

    /**
     * Invalidate cached data for a specific event.
     * Called when event data (e.g., downloaded maps) changes.
     */
    fun invalidateCache(eventId: String)

    /**
     * Clear all cached data.
     */
    fun clearCache()
}

@OptIn(ExperimentalTime::class)
class DefaultGeoJsonDataProvider :
    GeoJsonDataProvider,
    KoinComponent {
    // Simple LRU cache using mutableMapOf (LinkedHashMap cannot be extended in KMP common code)
    private val cache = mutableMapOf<String, JsonObject?>()
    private val cacheAccessOrder = mutableListOf<String>() // Track access order for LRU
    private val lastAttemptTime = mutableMapOf<String, kotlin.time.Instant>()
    private val attemptCount = mutableMapOf<String, Int>()

    companion object {
        private const val MAX_CACHE_SIZE = 10
        private const val MAX_ODR_ATTEMPTS = 20
        private const val ATTEMPTS_FAST_RETRY = 3
        private const val ATTEMPTS_MEDIUM_RETRY = 10
        private const val GEOJSON_PREVIEW_LENGTH = 80
    }

    private fun evictLRUIfNeeded() {
        if (cache.size >= MAX_CACHE_SIZE && cacheAccessOrder.isNotEmpty()) {
            val lruKey = cacheAccessOrder.removeAt(0)
            cache.remove(lruKey)
            lastAttemptTime.remove(lruKey)
            attemptCount.remove(lruKey)
            Log.v("GeoJsonDataProvider", "Evicted LRU cache entry for $lruKey")
        }
    }

    private fun recordCacheAccess(eventId: String) {
        cacheAccessOrder.remove(eventId) // Remove if exists
        cacheAccessOrder.add(eventId) // Add to end (most recently used)
    }

    @Suppress("ReturnCount") // Early returns for guard clauses improve readability
    override suspend fun getGeoJsonData(eventId: String): JsonObject? {
        val cachedResult = getCachedResult(eventId)
        if (cachedResult != null) {
            return cachedResult.value
        }

        val rateLimitResult = checkRateLimit(eventId)
        if (rateLimitResult != null) {
            return rateLimitResult
        }

        updateAttemptTracking(eventId)

        val result = loadGeoJsonData(eventId)

        handleLoadResult(eventId, result)

        return result
    }

    private data class CachedResult(
        val value: JsonObject?,
    )

    private fun getCachedResult(eventId: String): CachedResult? {
        val hasCachedValue = cache.containsKey(eventId)
        return if (hasCachedValue) {
            recordCacheAccess(eventId) // Update LRU order
            CachedResult(cache[eventId])
        } else {
            null
        }
    }

    @Suppress("ReturnCount") // Early returns for guard clauses improve readability
    private fun checkRateLimit(eventId: String): JsonObject? {
        val lastAttempt = lastAttemptTime[eventId] ?: return null
        val isOdrUnavailable = isODRUnavailable(eventId)
        if (!isOdrUnavailable) return null

        val now = Clock.System.now()
        val attempts = attemptCount[eventId] ?: 0
        val timeSinceLastAttempt = now - lastAttempt

        val minDelay = calculateMinDelay(attempts)

        val shouldWait = timeSinceLastAttempt < minDelay
        if (shouldWait) {
            return null
        }

        val maxAttemptsReached = attempts >= MAX_ODR_ATTEMPTS
        if (maxAttemptsReached) {
            Log.w(::getGeoJsonData.name, "Giving up on $eventId after $attempts attempts")
            evictLRUIfNeeded()
            cache[eventId] = null
            recordCacheAccess(eventId)
            return null
        }

        return null
    }

    private fun calculateMinDelay(attempts: Int): Duration =
        when {
            attempts < ATTEMPTS_FAST_RETRY -> Duration.parse("1s")
            attempts < ATTEMPTS_MEDIUM_RETRY -> Duration.parse("5s")
            else -> Duration.parse("30s")
        }

    private fun updateAttemptTracking(eventId: String) {
        lastAttemptTime[eventId] = Clock.System.now()
        val currentAttempts = attemptCount[eventId] ?: 0
        attemptCount[eventId] = currentAttempts + 1
    }

    private suspend fun loadGeoJsonData(eventId: String): JsonObject? =
        try {
            Log.i(::getGeoJsonData.name, "Loading geojson data for event $eventId")

            val geojsonData = readGeoJson(eventId)

            parseGeoJsonData(eventId, geojsonData)
        } catch (e: Exception) {
            Log.e(::getGeoJsonData.name, "Error loading geojson data for event $eventId: ${e.message}")
            null
        }

    private fun parseGeoJsonData(
        eventId: String,
        geojsonData: String?,
    ): JsonObject? {
        if (geojsonData == null) {
            Log.d(::getGeoJsonData.name, "Geojson data is null for event $eventId")
            return null
        }

        val preview = geojsonData.take(GEOJSON_PREVIEW_LENGTH).replace("\n", "")
        Log.i(
            ::getGeoJsonData.name,
            "Retrieved geojson string (length=${geojsonData.length}) preview=\"${preview}\"",
        )

        val jsonObj = Json.parseToJsonElement(geojsonData).jsonObject
        val keysSummary = jsonObj.keys.joinToString(", ")
        val rootType = jsonObj["type"]?.toString()
        Log.d(
            ::getGeoJsonData.name,
            "Parsed geojson top-level keys=[$keysSummary], type=$rootType",
        )

        return jsonObj
    }

    private fun handleLoadResult(
        eventId: String,
        result: JsonObject?,
    ) {
        val loadSuccessful = result != null
        if (loadSuccessful) {
            lastAttemptTime.remove(eventId)
            attemptCount.remove(eventId)
            Log.v(::getGeoJsonData.name, "Successfully loaded $eventId, reset attempt tracking")
        }

        val shouldCache = result != null || !isODRUnavailable(eventId)
        if (shouldCache) {
            evictLRUIfNeeded()
            cache[eventId] = result
            recordCacheAccess(eventId)
            Log.v(::getGeoJsonData.name, "Cached GeoJSON result for $eventId (success=$loadSuccessful)")
        } else {
            Log.i(::getGeoJsonData.name, "Not caching null result for $eventId (ODR may become available)")
        }
    }

    /**
     * Check if GeoJSON failure is due to ODR resources being unavailable but will become available.
     * On iOS, this allows retry when ODR downloads complete.
     * Returns true only if: platform is iOS AND (download explicitly requested OR initial install tag).
     */
    private fun isODRUnavailable(eventId: String): Boolean =
        try {
            // This is a platform-specific check - only meaningful on iOS
            val platform = get<WWWPlatform>()
            val isIOS = platform.name.contains("iOS", ignoreCase = true)

            if (!isIOS) {
                false
            } else {
                // Retry if download was explicitly requested
                val downloadRequested =
                    com.worldwidewaves.shared.data.MapDownloadGate
                        .isAllowed(eventId)

                val shouldRetry = downloadRequested
                if (!shouldRetry) {
                    Log.v(::isODRUnavailable.name, "ODR not requested for $eventId, no retry")
                }
                shouldRetry
            }
        } catch (e: Exception) {
            // If we can't determine platform, don't retry to avoid log spam
            Log.v(::isODRUnavailable.name, "Could not check ODR status: ${e.message}")
            false
        }

    override fun invalidateCache(eventId: String) {
        if (cache.remove(eventId) != null) {
            Log.d(::invalidateCache.name, "Invalidated cache for event $eventId")
        }
        // Also reset attempt tracking to allow immediate retry
        lastAttemptTime.remove(eventId)
        attemptCount.remove(eventId)
        Log.d(::invalidateCache.name, "Reset attempt tracking for $eventId")
    }

    override fun clearCache() {
        val size = cache.size
        cache.clear()
        lastAttemptTime.clear()
        attemptCount.clear()
        Log.d(::clearCache.name, "Cleared GeoJSON cache ($size entries)")
    }
}
