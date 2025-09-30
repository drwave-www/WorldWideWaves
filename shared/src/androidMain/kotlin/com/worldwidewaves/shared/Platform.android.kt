package com.worldwidewaves.shared

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

import android.content.Context
import dev.icerock.moko.resources.StringResource
import dev.icerock.moko.resources.desc.desc
import org.koin.mp.KoinPlatform

// // Session cache to remember unavailable geojson within the same app session
// private val unavailableGeoJsonCache = ConcurrentHashMap.newKeySet<String>()
//
// /**
// * Clear the unavailable cache for a specific event when a map is downloaded
// */
// fun clearUnavailableGeoJsonCache(eventId: String) {
//    unavailableGeoJsonCache.remove(eventId)
//
//    // Also invalidate GeoJSON cache in memory to force fresh load
//    try {
//        val geoJsonProvider: GeoJsonDataProvider by inject(
//            GeoJsonDataProvider::class.java,
//        )
//        geoJsonProvider.invalidateCache(eventId)
//    } catch (e: Exception) {
//        Log.w("clearUnavailableGeoJsonCache", "Failed to invalidate GeoJSON cache for $eventId: ${e.message}")
//    }
//
//    Log.d("clearUnavailableGeoJsonCache", "Cleared cache for event $eventId")
// }
//
// actual suspend fun readGeoJson(eventId: String): String? {
//    // Quick session cache check to avoid repeated calls for known unavailable maps
//    if (unavailableGeoJsonCache.contains(eventId)) {
//        return null
//    }
//    val filePath = getMapFileAbsolutePath(eventId, "geojson")
//
//    return if (filePath != null) {
//        withContext(Dispatchers.IO) {
//            Log.i(::readGeoJson.name, "Loading geojson data for event $eventId from $filePath")
//            File(filePath).readText()
//        }
//    } else {
//        Log.d(::readGeoJson.name, "GeoJSON file not available for event $eventId")
//        // Cache this unavailable result to avoid repeated attempts in the same session
//        unavailableGeoJsonCache.add(eventId)
//        null
//    }
// }
//
// // ---------------------------
//
// /**
// * Retrieves the absolute path of a map file for a given event.
// *
// * This function attempts to get the absolute path of a map file (e.g., MBTiles, GeoJSON) associated with the event.
// * It first checks if the file is already cached in the device's cache directory. If the file is not cached or the
// * cached file size does not match the expected size, it reads the file from the resources and caches it.
// *
// */
// actual suspend fun getMapFileAbsolutePath(
//    eventId: String,
//    extension: String,
// ): String? {
//    val context: Context by inject(Context::class.java)
//    val mapChecker: MapAvailabilityChecker by inject(MapAvailabilityChecker::class.java)
//    val cachedFile = File(context.cacheDir, "$eventId.$extension")
//    val metadataFile = File(context.cacheDir, "$eventId.$extension.metadata")
//
//    // First check if we have a valid cached file that doesn't need updating
//    val needsUpdate =
//        when {
//            !cachedFile.exists() -> {
//                Log.d(::getMapFileAbsolutePath.name, "Cache file doesn't exist for $eventId.$extension")
//                true
//            }
//            !metadataFile.exists() -> {
//                Log.d(::getMapFileAbsolutePath.name, "Metadata file doesn't exist for $eventId.$extension")
//                true
//            }
//            else -> {
//                val lastCacheTime =
//                    try {
//                        metadataFile.readText().toLong()
//                    } catch (_: Exception) {
//                        0L
//                    }
//
//                // Check if the app was installed/updated after we cached the file
//                val appInstallTime =
//                    try {
//                        context.packageManager.getPackageInfo(context.packageName, 0).lastUpdateTime
//                    } catch (_: Exception) {
//                        System.currentTimeMillis()
//                    }
//
//                val isStale = appInstallTime > lastCacheTime
//                if (isStale) {
//                    Log.d(
//                        ::getMapFileAbsolutePath.name,
//                        "Cache is stale for $eventId.$extension (app: $appInstallTime, cache: $lastCacheTime)",
//                    )
//                } else {
//                    Log.d(
//                        ::getMapFileAbsolutePath.name,
//                        "Cache is up-to-date for $eventId.$extension",
//                    )
//                }
//                isStale
//            }
//        }
//
//    // If we have a valid cached file, return its path immediately
//    if (!needsUpdate) {
//        Log.i(::getMapFileAbsolutePath.name, "Using cached file for $eventId.$extension")
//        return cachedFile.absolutePath
//    }
//
//    // Check if the map is actually downloaded before attempting expensive operations
//    if (!mapChecker.isMapDownloaded(eventId)) {
//        Log.d(::getMapFileAbsolutePath.name, "Map feature not downloaded for $eventId.$extension, skipping file access attempts")
//        return null
//    }
//
//    // If we need to update the cache, try to open the asset from feature module
//    Log.i(::getMapFileAbsolutePath.name, "Fetching $eventId.$extension from feature module")
//
//    val assetPath = "$eventId.$extension"
//
//    /* ------------------------------------------------------------------
//     * Play Feature Delivery race mitigation:
//     * Immediately after SplitInstall reports INSTALLED the asset might
//     * not yet be visible to the running Activity/process. Retry a few
//     * times with a fresh split-aware context before giving up.
//     * ---------------------------------------------------------------- */
//
//    // Implement actual retry logic as mentioned in the comment above
//    var lastException: Exception? = null
//    val maxRetries = 3
//    val retryDelayMs = 100L
//
//    for (attempt in 1..maxRetries) {
//        try {
//            val ctx =
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                    try {
//                        context.createContextForSplit(eventId)
//                    } catch (_: Exception) {
//                        context
//                    }
//                } else {
//                    context
//                }
//
//            // Ensure split-compat hooks into this context
//            SplitCompat.install(ctx)
//
//            // Try to access the asset immediately
//            return cacheAssetFromContext(ctx, assetPath, cachedFile, metadataFile, eventId, extension)
//        } catch (e: java.io.FileNotFoundException) {
//            lastException = e
//            if (attempt < maxRetries) {
//                Log.d(::getMapFileAbsolutePath.name, "Asset not accessible on attempt $attempt for $eventId.$extension, retrying...")
//                kotlinx.coroutines.delay(retryDelayMs)
//            }
//        }
//    }
//
//    // If we get here, all retries failed - handle the final exception intelligently
//    if (lastException is java.io.FileNotFoundException) {
//        Log.d(::getMapFileAbsolutePath.name, "Map feature not available: $eventId.$extension (feature module not downloaded)")
//    } else {
//        Log.e(::getMapFileAbsolutePath.name, "Error loading map from feature module: ${lastException?.message}", lastException)
//    }
//    return null
// }
//
// /**
// * Helper function to cache asset from a given context.
// * Separated for the retry logic above.
// */
// private suspend fun cacheAssetFromContext(
//    ctx: Context,
//    assetPath: String,
//    cachedFile: File,
//    metadataFile: File,
//    @Suppress("UNUSED_PARAMETER") eventId: String,
//    @Suppress("UNUSED_PARAMETER") extension: String,
// ): String {
//    withContext(Dispatchers.IO) {
//        // Use a buffered approach for better memory efficiency
//        ctx.assets.open(assetPath).use { input ->
//            BufferedInputStream(input, WWWGlobals.ByteProcessing.BUFFER_SIZE).use { bufferedInput ->
//                cachedFile.outputStream().use { fileOutput ->
//                    BufferedOutputStream(fileOutput, WWWGlobals.ByteProcessing.BUFFER_SIZE).use { bufferedOutput ->
//                        val buffer = ByteArray(WWWGlobals.ByteProcessing.BUFFER_SIZE)
//                        var bytesRead: Int
//
//                        while (bufferedInput.read(buffer).also { bytesRead = it } != -1) {
//                            bufferedOutput.write(buffer, 0, bytesRead)
//                        }
//
//                        bufferedOutput.flush()
//                    }
//                }
//            }
//        }
//
//        // Update metadata after successful copy
//        metadataFile.writeText(System.currentTimeMillis().toString())
//    }
//
//    return cachedFile.absolutePath
// }
//

// -----------------------------------------------------------

actual fun localizeString(resource: StringResource): String {
    val context = KoinPlatform.getKoin().get<Context>()
    return resource.desc().toString(context)
}
