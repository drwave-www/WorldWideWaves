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
    val p = getMapFileAbsolutePath(eventId, "geojson")
    if (p == null) {
        Log.w("MapStore", "readGeoJson: No file path for $eventId")
        return null
    }
    Log.d("MapStore", "readGeoJson: Reading from $p")
    return platformReadText(p)
}

/** Single, shared implementation for both Android & iOS. */
suspend fun getMapFileAbsolutePath(
    eventId: String,
    extension: String,
): String? =
    lock.withLock {
        Log.d("MapStore", "getMapFileAbsolutePath: Getting $eventId.$extension")

        if (extension == "geojson" && unavailable.contains(eventId)) {
            Log.d("MapStore", "getMapFileAbsolutePath: $eventId marked as unavailable")
            return null
        }

        val root = platformCacheRoot().also { platformEnsureDir(it) }
        val fileName = "$eventId.$extension"
        val dataPath = "$root/$fileName"
        val meta = metaPath(root, fileName)
        val stamp = platformAppVersionStamp()

        // Fast path: valid cached copy with matching app stamp
        if (platformFileExists(dataPath) && platformFileExists(meta)) {
            val cachedStamp = runCatching { platformReadText(meta) }.getOrNull()
            if (cachedStamp == stamp) {
                Log.i("MapStore", "getMapFileAbsolutePath: Using cached $fileName")
                return dataPath
            }
            // stale -> drop and refetch
            Log.d("MapStore", "getMapFileAbsolutePath: Stale cache for $fileName, refetching")
            runCatching { platformDeleteFile(dataPath) }
            runCatching { platformDeleteFile(meta) }
        }

        // Fetch (mount ODR / open split / copy stream -> persistent cache)
        Log.d("MapStore", "getMapFileAbsolutePath: Fetching $fileName from platform")
        val ok = platformFetchToFile(eventId, extension, dataPath)
        if (!ok) {
            Log.w("MapStore", "getMapFileAbsolutePath: Failed to fetch $fileName")
            if (extension == "geojson") unavailable.add(eventId)
            return null
        }

        // Mark with current app stamp and invalidate in-memory caches where needed
        runCatching { platformWriteText(meta, stamp) }
        if (extension == "geojson") platformInvalidateGeoJson(eventId)

        Log.i("MapStore", "getMapFileAbsolutePath: Successfully fetched $fileName")
        return dataPath
    }

expect fun cacheStringToFile(
    fileName: String,
    content: String,
): String
