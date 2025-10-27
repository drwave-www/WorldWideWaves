package com.worldwidewaves.shared.data

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

import com.worldwidewaves.shared.utils.Log
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

// -------- platform shims (implemented per platform) ----------
expect fun platformCacheRoot(): String

expect fun platformFileExists(path: String): Boolean

expect fun platformReadText(path: String): String

expect fun platformWriteText(
    path: String,
    content: String,
)

expect fun platformDeleteFile(path: String)

expect fun platformEnsureDir(path: String)

expect fun platformAppVersionStamp(): String

expect fun platformInvalidateGeoJson(eventId: String)

expect suspend fun platformFetchToFile(
    eventId: String,
    extension: String,
    destAbsolutePath: String,
): Boolean
// -------------------------------------------------------------

private val unavailable = mutableSetOf<String>()
private val lock = Mutex()

object MapDownloadGate {
    private val mutex = Mutex()
    private val allowed = mutableSetOf<String>()

    suspend fun allow(tag: String) {
        mutex.withLock { allowed += tag }
    }

    suspend fun disallow(tag: String) {
        mutex.withLock { allowed -= tag }
    }

    fun isAllowed(tag: String) = tag in allowed
}

expect suspend fun platformTryCopyInitialTagToCache(
    eventId: String,
    extension: String,
    destAbsolutePath: String,
): Boolean

/** Clear the “unavailable” session cache + in-memory geojson cache. */
fun clearUnavailableGeoJsonCache(eventId: String) {
    unavailable.remove(eventId)
    platformInvalidateGeoJson(eventId)
}

private fun metaPath(
    root: String,
    name: String,
) = "$root/$name.metadata"

suspend fun readGeoJson(eventId: String): String? {
    Log.d("MapStore", "readGeoJson: Reading GeoJSON for $eventId")
    val p = getMapFileAbsolutePath(eventId, MapFileExtension.GEOJSON)
    if (p == null) {
        Log.w("MapStore", "readGeoJson: No file path for $eventId")
        return null
    }
    Log.d("MapStore", "readGeoJson: Reading from $p")
    return platformReadText(p)
}

/**
 * Map file extension types supported by the app.
 */
enum class MapFileExtension(
    val value: String,
) {
    GEOJSON("geojson"),
    MBTILES("mbtiles"),
    ;

    override fun toString(): String = value
}

/** Single, shared implementation for both Android & iOS. */
// commonMain (MapStore)
suspend fun getMapFileAbsolutePath(
    eventId: String,
    extension: MapFileExtension,
): String? =
    lock.withLock {
        Log.d("MapStore", "getMapFileAbsolutePath: eventId=$eventId, extension=$extension")

        if (extension == MapFileExtension.GEOJSON && unavailable.contains(eventId)) {
            Log.w("MapStore", "getMapFileAbsolutePath: GeoJSON marked unavailable for $eventId")
            return null
        }

        val root = platformCacheRoot().also { platformEnsureDir(it) }
        val fileName = "$eventId.$extension"
        val dataPath = "$root/$fileName"
        val meta = metaPath(root, fileName)
        val stamp = platformAppVersionStamp()

        Log.d("MapStore", "getMapFileAbsolutePath: Checking cache at $dataPath")

        // cache hit
        if (platformFileExists(dataPath) &&
            platformFileExists(meta) &&
            runCatching { platformReadText(meta) }.getOrNull() == stamp
        ) {
            Log.i("MapStore", "getMapFileAbsolutePath: Cache HIT for $eventId.$extension -> $dataPath")
            return dataPath
        }
        Log.d("MapStore", "getMapFileAbsolutePath: Cache MISS for $eventId.$extension (exists=${platformFileExists(dataPath)})")

        // downloads disallowed → try copying from currently-visible bundle/split (no mount)
        if (!MapDownloadGate.isAllowed(eventId)) {
            Log.d("MapStore", "getMapFileAbsolutePath: Download not allowed, trying bundle/ODR copy for $eventId.$extension")
            if (platformTryCopyInitialTagToCache(eventId, extension.value, dataPath)) {
                platformWriteText(meta, stamp)
                if (extension == MapFileExtension.GEOJSON) platformInvalidateGeoJson(eventId)
                Log.i("MapStore", "getMapFileAbsolutePath: Copied from bundle/ODR -> $dataPath")
                return dataPath
            }
            Log.w("MapStore", "getMapFileAbsolutePath: Failed to copy from bundle/ODR for $eventId.$extension")
            return null
        }

        // explicit download
        Log.d("MapStore", "getMapFileAbsolutePath: Attempting explicit download for $eventId.$extension")
        val ok = platformFetchToFile(eventId, extension.value, dataPath)
        if (!ok) {
            if (extension == MapFileExtension.GEOJSON) unavailable.add(eventId)
            Log.w("MapStore", "getMapFileAbsolutePath: Download FAILED for $eventId.$extension")
            return null
        }
        platformWriteText(meta, stamp)
        if (extension == MapFileExtension.GEOJSON) platformInvalidateGeoJson(eventId)
        Log.i("MapStore", "getMapFileAbsolutePath: Download SUCCESS -> $dataPath")
        return dataPath
    }

expect fun cacheStringToFile(
    fileName: String,
    content: String,
): String?
