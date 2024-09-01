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
import com.worldwidewaves.shared.generated.resources.Res
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.jetbrains.compose.resources.ExperimentalResourceApi

// ---------------------------

interface IClock {
    fun now(): Instant
}
class SystemClock : IClock {
    override fun now(): Instant = Clock.System.now()
}

// ---------------------------

interface ICoroutineScopeProvider {
    val scopeIO: CoroutineScope
    val scopeDefault: CoroutineScope
    suspend fun <T> withIOContext(block: suspend CoroutineScope.() -> T): T
    suspend fun <T> withDefaultContext(block: suspend CoroutineScope.() -> T): T
}
class DefaultCoroutineScopeProvider : ICoroutineScopeProvider {
    override val scopeIO: CoroutineScope = CoroutineScope(Dispatchers.IO)
    override val scopeDefault: CoroutineScope = CoroutineScope(Dispatchers.Default)

    override suspend fun <T> withIOContext(block: suspend CoroutineScope.() -> T): T {
        return withContext(Dispatchers.IO, block)
    }

    override suspend fun <T> withDefaultContext(block: suspend CoroutineScope.() -> T): T {
        return withContext(Dispatchers.Default, block)
    }
}

// ---------------------------

interface EventsConfigurationProvider {
    suspend fun geoEventsConfiguration(): String
}

class DefaultEventsConfigurationProvider(
    private val coroutineScopeProvider: ICoroutineScopeProvider = DefaultCoroutineScopeProvider()
) : EventsConfigurationProvider {
    @OptIn(ExperimentalResourceApi::class)
    override suspend fun geoEventsConfiguration(): String {
        return coroutineScopeProvider.withIOContext {
            Napier.i("Loading events configuration from $FS_EVENTS_CONF")
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
                Napier.i("Loading geojson data for event $eventId")
                Res.readBytes("$FS_MAPS_FOLDER/$eventId.geojson").decodeToString()
            }
            Json.parseToJsonElement(geojsonData).jsonObject
        } catch (e: Exception) {
            Napier.e("Error loading geojson data for event $eventId", e)
            null
        }
    }
}

// ---------------------------

interface MapDataProvider {
    suspend fun geoMapStyleData(): String
}

class DefaultMapDataProvider : MapDataProvider {
    @OptIn(ExperimentalResourceApi::class)
    override suspend fun geoMapStyleData(): String {
        return withContext(Dispatchers.IO) {
            Napier.i("Loading map style data from $FS_MAPS_STYLE")
            Res.readBytes(FS_MAPS_STYLE).decodeToString()
        }
    }
}