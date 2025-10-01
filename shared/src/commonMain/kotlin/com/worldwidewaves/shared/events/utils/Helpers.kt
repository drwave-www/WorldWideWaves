package com.worldwidewaves.shared.events.utils

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

import com.worldwidewaves.shared.WWWGlobals.FileSystem
import com.worldwidewaves.shared.WWWPlatform
import com.worldwidewaves.shared.data.readGeoJson
import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.events.WWWEvent
import com.worldwidewaves.shared.format.DateTimeFormats
import com.worldwidewaves.shared.generated.resources.Res
import com.worldwidewaves.shared.utils.Log
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

// ---------------------------

interface DataValidator {
    fun validationErrors(): List<String>?
}

// ---------------------------

@OptIn(ExperimentalTime::class)
interface IClock {
    fun now(): Instant

    suspend fun delay(duration: Duration)

    companion object {
        fun instantToLiteral(
            instant: Instant,
            timeZone: TimeZone,
        ): String {
            // Delegate to shared, locale-aware formatter so both Android & iOS
            // use the same logic (12/24 h handled per platform conventions).
            return DateTimeFormats.timeShort(instant, timeZone)
        }
    }
}

@OptIn(ExperimentalTime::class)
class SystemClock :
    IClock,
    KoinComponent {
    private var platform: WWWPlatform? = null

    // iOS FIX: Removed init{} block that calls DI get() to prevent potential deadlocks
    // Platform is now resolved lazily on first access

    private fun getPlatformSafely(): WWWPlatform? {
        if (platform == null) {
            try {
                platform = get()
            } catch (_: Exception) {
                Napier.w("${SystemClock::class.simpleName}: Platform not found, simulation disabled")
            }
        }
        return platform
    }

    override fun now(): Instant =
        if (getPlatformSafely()?.isOnSimulation() == true) {
            platform!!.getSimulation()!!.now()
        } else {
            Clock.System.now()
        }

    override suspend fun delay(duration: Duration) {
        val simulation = getPlatformSafely()?.takeIf { it.isOnSimulation() }?.getSimulation()

        if (simulation != null) {
            val speed =
                simulation.speed.takeIf { it > 0.0 } ?: run {
                    Napier.w("${SystemClock::class.simpleName}: Simulation speed is ${simulation.speed}, using 1.0 instead")
                    1.0
                }
            val adjustedDuration = maxOf(duration / speed.toDouble(), 50.milliseconds) // Minimum 50 ms
            kotlinx.coroutines.delay(adjustedDuration)
        } else {
            kotlinx.coroutines.delay(duration)
        }
    }
}

// ---------------------------

interface CoroutineScopeProvider {
    suspend fun <T> withIOContext(block: suspend CoroutineScope.() -> T): T

    suspend fun <T> withDefaultContext(block: suspend CoroutineScope.() -> T): T

    fun launchIO(block: suspend CoroutineScope.() -> Unit): Job

    fun launchDefault(block: suspend CoroutineScope.() -> Unit): Job

    fun scopeIO(): CoroutineScope

    fun scopeDefault(): CoroutineScope

    fun cancelAllCoroutines()
}

class DefaultCoroutineScopeProvider(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : CoroutineScopeProvider {
    private val supervisorJob = SupervisorJob()
    private val exceptionHandler =
        CoroutineExceptionHandler { _, exception ->
            Napier.e("CoroutineExceptionHandler caught unhandled exception: $exception", exception)
            // Log additional context for debugging
            Napier.e("Exception type: ${exception::class.simpleName}")
            Napier.e("Exception message: ${exception.message}")
            // Don't rethrow - this prevents the app crash
        }

    // Create scopes with exception handler included
    private val ioScope = CoroutineScope(supervisorJob + ioDispatcher + exceptionHandler)
    private val defaultScope = CoroutineScope(supervisorJob + defaultDispatcher + exceptionHandler)

    // Note: Unused scope kept for potential future use
    @Suppress("unused")
    private val scope = CoroutineScope(supervisorJob + defaultDispatcher + exceptionHandler)

    override fun launchIO(block: suspend CoroutineScope.() -> Unit): Job = ioScope.launch(block = block)

    override fun launchDefault(block: suspend CoroutineScope.() -> Unit): Job = defaultScope.launch(block = block)

    override fun scopeIO(): CoroutineScope = ioScope

    override fun scopeDefault(): CoroutineScope = defaultScope

    override suspend fun <T> withIOContext(block: suspend CoroutineScope.() -> T): T = withContext(ioDispatcher) { block() }

    override suspend fun <T> withDefaultContext(block: suspend CoroutineScope.() -> T): T = withContext(defaultDispatcher) { block() }

    override fun cancelAllCoroutines() {
        supervisorJob.cancel()
    }
}

// ---------------------------

interface EventsConfigurationProvider {
    suspend fun geoEventsConfiguration(): String
}

class DefaultEventsConfigurationProvider(
    private val coroutineScopeProvider: CoroutineScopeProvider = DefaultCoroutineScopeProvider(),
) : EventsConfigurationProvider {
    @OptIn(ExperimentalResourceApi::class)
    override suspend fun geoEventsConfiguration(): String =
        coroutineScopeProvider.withIOContext {
            Log.i("EventsConfigurationProvider", "=== STARTING EVENTS CONFIGURATION LOAD ===")
            Log.i("EventsConfigurationProvider", "Target file: ${FileSystem.EVENTS_CONF}")

            try {
                Log.i("EventsConfigurationProvider", "Attempting Res.readBytes() call...")
                val bytes = Res.readBytes(FileSystem.EVENTS_CONF)
                Log.i("EventsConfigurationProvider", "Successfully read ${bytes.size} bytes from Compose Resources")

                val result = bytes.decodeToString()
                Log.i("EventsConfigurationProvider", "Successfully decoded ${result.length} characters")
                Log.i("EventsConfigurationProvider", "First 100 chars: ${result.take(100)}")
                Log.i("EventsConfigurationProvider", "=== EVENTS CONFIGURATION LOAD SUCCESSFUL ===")
                result
            } catch (e: Exception) {
                Log.e("EventsConfigurationProvider", "=== EVENTS CONFIGURATION LOAD FAILED ===")
                Log.e("EventsConfigurationProvider", "Exception type: ${e::class.simpleName}")
                Log.e("EventsConfigurationProvider", "Exception message: ${e.message}")
                Log.e("EventsConfigurationProvider", "Falling back to hardcoded minimal event for iOS debugging")

                // Fallback to hardcoded minimal event JSON for debugging
                """[
                {
                    "id": "debug_event_ios",
                    "title": "iOS Debug Event",
                    "location": {
                        "latitude": 40.7589,
                        "longitude": -73.9851,
                        "address": "New York, NY"
                    },
                    "scheduledStartTime": "2025-09-27T12:00:00Z",
                    "duration": "PT30M",
                    "description": "Debug event for iOS Compose Resources issue",
                    "status": "upcoming",
                    "waveSettings": {
                        "radius": 100,
                        "speed": 0.5,
                        "color": "#FF0000"
                    },
                    "soundChoreographyId": "debug_sound"
                }
                ]"""
            }
        }
}

// ---------------------------

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
    companion object {
        private const val MAX_CACHE_SIZE = 10
        private const val MAX_ODR_ATTEMPTS = 20
        private const val ATTEMPTS_FAST_RETRY = 3
        private const val ATTEMPTS_MEDIUM_RETRY = 10
        private const val GEOJSON_PREVIEW_LENGTH = 80
    }

    private val cache =
        object : LinkedHashMap<String, JsonObject?>(
            MAX_CACHE_SIZE,
            0.75f,
            true, // access-order for LRU
        ) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, JsonObject?>?): Boolean {
                val shouldRemove = size > MAX_CACHE_SIZE
                if (shouldRemove && eldest != null) {
                    lastAttemptTime.remove(eldest.key)
                    attemptCount.remove(eldest.key)
                    Log.v("GeoJsonDataProvider", "Evicted cache entry for ${eldest.key} (LRU), cleaned up metadata")
                }
                return shouldRemove
            }
        }

    private val lastAttemptTime = mutableMapOf<String, Instant>()
    private val attemptCount = mutableMapOf<String, Int>()

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
            CachedResult(cache[eventId])
        } else {
            null
        }
    }

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
            cache[eventId] = null // LinkedHashMap auto-evicts if needed
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
            cache[eventId] = result // LinkedHashMap auto-evicts LRU if size > MAX_CACHE_SIZE
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

// ---------------------------

interface EventsDecoder {
    fun decodeFromJson(jsonString: String): List<IWWWEvent>
}

class DefaultEventsDecoder : EventsDecoder {
    private val jsonDecoder = Json { ignoreUnknownKeys = true }

    override fun decodeFromJson(jsonString: String) = jsonDecoder.decodeFromString<List<WWWEvent>>(jsonString)
}

// ---------------------------

interface MapDataProvider {
    suspend fun geoMapStyleData(): String
}

class DefaultMapDataProvider : MapDataProvider {
    @OptIn(ExperimentalResourceApi::class)
    override suspend fun geoMapStyleData(): String =
        withContext(Dispatchers.IO) {
            Log.i(::geoMapStyleData.name, "Loading map style template from ${FileSystem.MAPS_STYLE}")
            val bytes = Res.readBytes(FileSystem.MAPS_STYLE)
            Log.d(::geoMapStyleData.name, "Read ${bytes.size} bytes from style template")
            val result = bytes.decodeToString()
            Log.i(::geoMapStyleData.name, "Style template decoded: ${result.length} chars")
            result
        }
}
