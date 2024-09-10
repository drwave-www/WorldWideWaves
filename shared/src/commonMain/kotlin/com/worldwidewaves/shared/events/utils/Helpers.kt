package com.worldwidewaves.shared.events.utils

/*
 * Copyright 2024 DrWave
 *
 * WorldWideWaves is an ephemeral mobile app designed to orchestrate human waves through cities and countries,
 * culminating in a global wave. The project aims to transcend physical and cultural boundaries, fostering unity,
 * community, and shared human experience by leveraging real-time coordination and location-based services.
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

import com.worldwidewaves.shared.WWWGlobals.Companion.FS_EVENTS_CONF
import com.worldwidewaves.shared.WWWGlobals.Companion.FS_MAPS_FOLDER
import com.worldwidewaves.shared.WWWGlobals.Companion.FS_MAPS_STYLE
import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.events.WWWEvent
import com.worldwidewaves.shared.generated.resources.Res
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.jetbrains.compose.resources.ExperimentalResourceApi

// ---------------------------

interface DataValidator {
    fun validationErrors(): List<String>?
}

// ---------------------------

interface IClock {
    fun now(): Instant

    companion object {
        fun instantToLiteral(instant: Instant, timeZone: TimeZone): String {
            val localDateTime = instant.toLocalDateTime(timeZone)
            val hour = localDateTime.hour.toString().padStart(2, '0')
            val minute = localDateTime.minute.toString().padStart(2, '0')
            return "$hour:$minute"
        }
    }
}
class SystemClock : IClock {
    override fun now(): Instant {
        val instant = Instant.parse("2024-03-19T13:00:00Z")
        val timeZone = TimeZone.of("America/Sao_Paulo")
        return instant.toLocalDateTime(timeZone).toInstant(timeZone)
    } // = Clock.System.now() // FIXME DEBUG
}

// ---------------------------

interface CoroutineScopeProvider {
    val scopeIO: CoroutineScope
    val scopeDefault: CoroutineScope
    suspend fun <T> withIOContext(block: suspend CoroutineScope.() -> T): T
    suspend fun <T> withDefaultContext(block: suspend CoroutineScope.() -> T): T
}

class DefaultCoroutineScopeProvider(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
) : CoroutineScopeProvider {

    override val scopeIO: CoroutineScope = CoroutineScope(ioDispatcher)
    override val scopeDefault: CoroutineScope = CoroutineScope(defaultDispatcher)

    override suspend fun <T> withIOContext(block: suspend CoroutineScope.() -> T): T {
        return withContext(ioDispatcher, block)
    }

    override suspend fun <T> withDefaultContext(block: suspend CoroutineScope.() -> T): T {
        return withContext(defaultDispatcher, block)
    }
}

// ---------------------------

interface EventsConfigurationProvider {
    suspend fun geoEventsConfiguration(): String
}

class DefaultEventsConfigurationProvider(
    private val coroutineScopeProvider: CoroutineScopeProvider = DefaultCoroutineScopeProvider()
) : EventsConfigurationProvider {
    @OptIn(ExperimentalResourceApi::class)
    override suspend fun geoEventsConfiguration(): String {
        return coroutineScopeProvider.withIOContext {
            Log.i(::geoEventsConfiguration.name, "Loading events configuration from $FS_EVENTS_CONF")
            Res.readBytes(FS_EVENTS_CONF).decodeToString()
        }
    }
}

// ---------------------------

interface GeoJsonDataProvider {
    suspend fun getGeoJsonData(eventId: String): JsonObject?
}

class DefaultGeoJsonDataProvider : GeoJsonDataProvider {
    @OptIn(ExperimentalResourceApi::class)
    override suspend fun getGeoJsonData(eventId: String): JsonObject? {
        return try {
            val geojsonData = withContext(Dispatchers.IO) {
                Log.i(::getGeoJsonData.name, "Loading geojson data for event $eventId")
                Res.readBytes("$FS_MAPS_FOLDER/$eventId.geojson").decodeToString()
            }
            Json.parseToJsonElement(geojsonData).jsonObject
        } catch (e: Exception) {
            Log.e(::getGeoJsonData.name, "Error loading geojson data for event $eventId", throwable = e)
            null
        }
    }
}

// ---------------------------

interface EventsDecoder {
    fun decodeFromJson(jsonString: String): List<IWWWEvent>
}
class DefaultEventsDecoder : EventsDecoder {
    private val jsonDecoder = Json { ignoreUnknownKeys = true }
    override fun decodeFromJson(jsonString: String) =
        jsonDecoder.decodeFromString<List<WWWEvent>>(jsonString)
}

// ---------------------------

interface MapDataProvider {
    suspend fun geoMapStyleData(): String
}

class DefaultMapDataProvider : MapDataProvider {
    @OptIn(ExperimentalResourceApi::class)
    override suspend fun geoMapStyleData(): String {
        return withContext(Dispatchers.IO) {
            Log.i(::geoMapStyleData.name,"Loading map style data from $FS_MAPS_STYLE")
            Res.readBytes(FS_MAPS_STYLE).decodeToString()
        }
    }
}