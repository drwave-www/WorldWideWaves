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
expect suspend fun platformCacheRoot(): String

expect suspend fun platformFileExists(path: String): Boolean

expect suspend fun platformReadText(path: String): String

expect suspend fun platformWriteText(
    path: String,
    content: String,
)

expect suspend fun platformDeleteFile(path: String)

expect suspend fun platformEnsureDir(path: String)

expect suspend fun platformAppVersionStamp(): String

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

/**
 * Reads the GeoJSON data for a specific event from the local cache.
 *
 * This function:
 * - Locates the cached GeoJSON file for the event
 * - Returns null if the file doesn't exist or is marked unavailable
 * - Logs the read operation for debugging
 *
 * @param eventId The unique identifier of the event
 * @return The GeoJSON string content, or null if unavailable
 */
suspend fun readGeoJson(eventId: String): String? {
    Log.d("MapStore", "readGeoJson: Reading GeoJSON for $eventId")
    val p = getMapFileAbsolutePath(eventId, MapFileExtension.GEOJSON)
    if (p == null) {
        Log.d("MapStore", "readGeoJson: No file path for $eventId (map not downloaded)")
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

/**
 * Retrieves the absolute path to a cached map file, fetching it if necessary.
 *
 * This is the single, shared implementation for both Android & iOS that handles:
 * 1. Cache hit: Returns path if file exists with matching version metadata
 * 2. Bundle/ODR copy: Attempts to copy from app bundle/On-Demand Resources if downloads disabled
 * 3. Explicit download: Fetches from remote server if download is allowed
 *
 * Download control:
 * - Respects MapDownloadGate to prevent unwanted network usage
 * - Marks GeoJSON files as unavailable if download fails (prevents retry loops)
 *
 * Thread safety: Uses mutex lock to prevent concurrent cache operations
 *
 * @param eventId The unique identifier of the event
 * @param extension The file extension type (GEOJSON or MBTILES)
 * @return Absolute path to the cached file, or null if unavailable
 *
 * @see MapDownloadGate for download permission control
 * @see clearUnavailableGeoJsonCache to reset unavailability status
 */
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

        // Check if data file exists first
        val dataFileExists = platformFileExists(dataPath)

        if (dataFileExists) {
            // Data file exists - check metadata
            val metaExists = platformFileExists(meta)
            val storedStamp = if (metaExists) runCatching { platformReadText(meta) }.getOrNull() else null

            if (storedStamp == stamp) {
                // Perfect cache hit - version matches
                Log.i("MapStore", "getMapFileAbsolutePath: Cache HIT for $eventId.$extension -> $dataPath")
                return dataPath
            } else if (storedStamp != null && storedStamp.isNotEmpty()) {
                // Cache exists with old version stamp - migrate to current version
                Log.i("MapStore", "getMapFileAbsolutePath: Migrating cache from version $storedStamp to $stamp for $eventId.$extension")
                platformWriteText(meta, stamp)
                if (extension == MapFileExtension.GEOJSON) platformInvalidateGeoJson(eventId)
                Log.i("MapStore", "getMapFileAbsolutePath: Cache migrated successfully -> $dataPath")
                return dataPath
            } else if (!metaExists) {
                // Data file exists but no metadata - create metadata
                Log.i("MapStore", "getMapFileAbsolutePath: Creating missing metadata for existing cache $eventId.$extension")
                platformWriteText(meta, stamp)
                if (extension == MapFileExtension.GEOJSON) platformInvalidateGeoJson(eventId)
                Log.i("MapStore", "getMapFileAbsolutePath: Metadata created -> $dataPath")
                return dataPath
            }
        }
        Log.d("MapStore", "getMapFileAbsolutePath: Cache MISS for $eventId.$extension (exists=$dataFileExists)")

        // downloads disallowed → try copying from currently-visible bundle/split (no mount)
        if (!MapDownloadGate.isAllowed(eventId)) {
            Log.d("MapStore", "getMapFileAbsolutePath: Download not allowed, trying bundle/ODR copy for $eventId.$extension")
            if (platformTryCopyInitialTagToCache(eventId, extension.value, dataPath)) {
                platformWriteText(meta, stamp)
                if (extension == MapFileExtension.GEOJSON) platformInvalidateGeoJson(eventId)
                Log.i("MapStore", "getMapFileAbsolutePath: Copied from bundle/ODR -> $dataPath")
                return dataPath
            }
            Log.d(
                "MapStore",
                "getMapFileAbsolutePath: Map not available in bundle/ODR for $eventId.$extension (expected for non-downloaded maps)",
            )
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

expect suspend fun cacheStringToFile(
    fileName: String,
    content: String,
): String?
