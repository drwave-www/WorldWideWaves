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
    private val cache = mutableMapOf<String, JsonObject?>()
    private val lastAttemptTime = mutableMapOf<String, Instant>()
    private val attemptCount = mutableMapOf<String, Int>()

    override suspend fun getGeoJsonData(eventId: String): JsonObject? {
        // Check cache first
        if (cache.containsKey(eventId)) {
            return cache[eventId]
        }

        // Rate limiting for ODR requests to prevent infinite loops
        val now = Clock.System.now()
        val lastAttempt = lastAttemptTime[eventId]
        val attempts = attemptCount[eventId] ?: 0

        if (lastAttempt != null && isODRUnavailable(eventId)) {
            val timeSinceLastAttempt = now - lastAttempt
            val minDelay =
                when {
                    attempts < 3 -> Duration.parse("1s") // First 3 attempts: 1 second apart
                    attempts < 10 -> Duration.parse("5s") // Next 7 attempts: 5 seconds apart
                    else -> Duration.parse("30s") // After that: 30 seconds apart
                }

            if (timeSinceLastAttempt < minDelay) {
//                Log.v(
//                    ::getGeoJsonData.name,
//                    "Rate limiting $eventId: last attempt ${timeSinceLastAttempt.inWholeSeconds}s ago, need ${minDelay.inWholeSeconds}s (attempt $attempts)",
//                )
                return null // Return null without incrementing attempts
            }

            // Stop trying after many attempts
            if (attempts >= 20) {
                Log.w(::getGeoJsonData.name, "Giving up on $eventId after $attempts attempts")
                cache[eventId] = null // Cache the failure to stop retrying
                return null
            }
        }

        // Update attempt tracking
        lastAttemptTime[eventId] = now
        attemptCount[eventId] = attempts + 1

        // Not in cache, load and cache the result
        val result =
            try {
                Log.i(::getGeoJsonData.name, "Loading geojson data for event $eventId")

                val geojsonData = readGeoJson(eventId)

                if (geojsonData != null) {
                    val preview = geojsonData.take(80).replace("\n", "")
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

                    jsonObj
                } else {
                    Log.d(::getGeoJsonData.name, "Geojson data is null for event $eventId")
                    null
                }
            } catch (e: Exception) {
                Log.e(::getGeoJsonData.name, "Error loading geojson data for event $eventId: ${e.message}")
                null
            }

        // Reset attempt tracking on successful load
        if (result != null) {
            lastAttemptTime.remove(eventId)
            attemptCount.remove(eventId)
            Log.v(::getGeoJsonData.name, "Successfully loaded $eventId, reset attempt tracking")
        }

        // Cache successful results. For null results, only cache if it's not due to ODR unavailability
        // This allows retry when ODR resources become available later
        val shouldCache = result != null || !isODRUnavailable(eventId)
        if (shouldCache) {
            cache[eventId] = result
            Log.v(::getGeoJsonData.name, "Cached GeoJSON result for $eventId (success=${result != null})")
        } else {
            Log.i(::getGeoJsonData.name, "Not caching null result for $eventId (ODR may become available)")
        }

        return result
    }

    /**
     * Check if GeoJSON failure is due to ODR resources being unavailable.
     * On iOS, this allows retry when ODR downloads complete.
     */
    private fun isODRUnavailable(eventId: String): Boolean =
        try {
            // This is a platform-specific check - only meaningful on iOS
            val platform = get<WWWPlatform>()
            platform.name.contains("iOS", ignoreCase = true)
        } catch (e: Exception) {
            // If we can't determine platform, err on the side of allowing retry
            Log.v(::isODRUnavailable.name, "Could not determine platform for ODR check: ${e.message}")
            true
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
            Log.i(::geoMapStyleData.name, "Loading map style data from ${FileSystem.MAPS_STYLE}")
            Res.readBytes(FileSystem.MAPS_STYLE).decodeToString()
        }
}
