package com.worldwidewaves.shared.data

/* * Copyright 2025 DrWave
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
 * limitations under the License. */

import android.content.Context
import android.os.Build
import com.worldwidewaves.shared.events.data.GeoJsonDataProvider
import com.worldwidewaves.shared.generated.resources.Res
import com.worldwidewaves.shared.utils.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.koin.java.KoinJavaComponent.inject
import java.io.File

/**
 * Cache for app update time to avoid repeated PackageManager binder calls.
 * Only needs to be fetched once per app session since it won't change until app restart.
 */
private var cachedAppUpdateTime: Long? = null

/**
 * Checks if a cached file exists in the application's cache directory.
 *
 * This function determines whether a file with the specified name exists in the cache directory.
 * It also considers whether the application is running in development mode, in which case it always
 * returns `false` to simulate the absence of cached files.
 */
actual suspend fun cachedFileExists(fileName: String): Boolean {
    val context: Context by inject(Context::class.java)
    val isDevelopmentMode = Build.HARDWARE == "ranchu" || Build.HARDWARE == "goldfish"

    return if (isDevelopmentMode) {
        // Allow caching for generated style files to prevent performance issues
        if (fileName.startsWith("style-") && fileName.endsWith(".json")) {
            val fileExists = File(context.cacheDir, fileName).exists()
            Log.i(::cachedFileExists.name, "Development mode (allowing style cache): $fileName -> $fileExists")
            fileExists
        } else {
            Log.i(::cachedFileExists.name, "Development mode (not cached): $fileName")
            false
        }
    } else {
        File(context.cacheDir, fileName).exists()
    }
}

/**
 * Retrieves the absolute path of a cached file if it exists.
 *
 * This function checks if a file with the given name exists in the cache directory of the Android context.
 * If the file exists, it returns the absolute path of the file as a string. If the file does not exist,
 * it returns `null`.
 *
 */
actual suspend fun cachedFilePath(fileName: String): String? {
    val context: Context by inject(Context::class.java)
    return File(context.cacheDir, fileName).takeIf { it.exists() }?.toURI()?.path
}

/**
 * Caches a file from the application's resources to the device's cache directory.
 *
 * This function reads the bytes of a file from the application's resources and writes them to a file
 * in the device's cache directory. If the cache directory does not exist, it is created. If an error
 * occurs during the process, it is logged.
 *
 */
@OptIn(ExperimentalResourceApi::class)
actual suspend fun cacheDeepFile(fileName: String) {
    try {
        val context: Context by inject(Context::class.java)
        val fileBytes = Res.readBytes(fileName)
        val cacheFile = File(context.cacheDir, fileName)

        cacheFile.parentFile?.mkdirs()
        cacheFile.outputStream().use { it.write(fileBytes) }
    } catch (e: java.io.FileNotFoundException) {
        Log.w(::cacheDeepFile.name, "Cannot cache deep file: $fileName (resource not found)", e)
    } catch (e: SecurityException) {
        Log.e(::cacheDeepFile.name, "Security error caching file: $fileName", e)
    } catch (e: java.io.IOException) {
        Log.e(::cacheDeepFile.name, "IO error caching file: $fileName", e)
    } catch (e: Exception) {
        Log.e(::cacheDeepFile.name, "Unexpected error caching file: $fileName", e)
    }
}

/**
 * Retrieves the absolute path to the cache directory on the Android platform.
 *
 * This function uses the Android context to access the cache directory and returns its absolute path.
 * The cache directory is a location where the application can store temporary files.
 *
 */
actual fun getCacheDir(): String {
    val context: Context by inject(Context::class.java)
    return context.cacheDir.absolutePath
}

/**
 * Determine whether an already-cached file is stale with regard to the
 * application's lastUpdateTime (which also changes when dynamic-feature
 * splits are updated through the Play Store).
 */
actual suspend fun isCachedFileStale(fileName: String): Boolean =
    withContext(Dispatchers.IO) {
        val context: Context by inject(Context::class.java)
        val cacheDir = context.cacheDir

        val dataFile = File(cacheDir, fileName)
        if (!dataFile.exists()) return@withContext true

        val metadataFile = File(cacheDir, "$fileName.metadata")
        val cachedTime =
            try {
                metadataFile.takeIf { it.exists() }?.readText()?.toLong() ?: 0L
            } catch (_: Exception) {
                0L
            }

        // Use cached app update time to avoid repeated PackageManager binder calls
        val appUpdateTime =
            cachedAppUpdateTime ?: try {
                val updateTime = context.packageManager.getPackageInfo(context.packageName, 0).lastUpdateTime
                cachedAppUpdateTime = updateTime
                updateTime
            } catch (_: Exception) {
                System.currentTimeMillis()
            }

        appUpdateTime > cachedTime
    }

/**
 * Force-update (or create) the metadata timestamp associated with a cached file.
 */
actual suspend fun updateCacheMetadata(fileName: String) {
    withContext(Dispatchers.IO) {
        val context: Context by inject(Context::class.java)
        val metadataFile = File(context.cacheDir, "$fileName.metadata")
        try {
            metadataFile.writeText(System.currentTimeMillis().toString())
        } catch (e: Exception) {
            Log.e(::updateCacheMetadata.name, "Could not write metadata for $fileName", e)
        }
    }
}

/**
 * Delete all cached artefacts (data + metadata files) that belong to a given map/event.
 */
fun clearEventCache(eventId: String) {
    val context: Context by inject(Context::class.java)
    val cacheDir = context.cacheDir

    invalidateGeoJsonCache(eventId)

    val targets =
        listOf(
            "$eventId.mbtiles",
            "$eventId.mbtiles.metadata",
            "$eventId.geojson",
            "$eventId.geojson.metadata",
            "style-$eventId.json",
            "style-$eventId.json.metadata",
        )

    targets.forEach { name ->
        deleteCachedFile(cacheDir, name)
    }
}

/**
 * Delay in milliseconds before actually deleting renamed .mbtiles files.
 * Allows MapLibre background threads to gracefully release file handles.
 */
private const val MBTILES_DELETE_DELAY_MS = 500L

/**
 * Invalidates the GeoJSON cache for the specified event.
 */
private fun invalidateGeoJsonCache(eventId: String) {
    try {
        val geoJsonProvider: GeoJsonDataProvider by inject(GeoJsonDataProvider::class.java)
        geoJsonProvider.invalidateCache(eventId)
    } catch (e: Exception) {
        Log.w(::clearEventCache.name, "Failed to invalidate GeoJSON cache for $eventId: ${e.message}")
    }
}

/**
 * Attempts to delete a cached file from the given cache directory.
 * For .mbtiles files, uses a safe two-step deletion to prevent MapLibre crashes:
 * 1. Rename the file to .deleted (breaks the file path reference)
 * 2. Schedule actual deletion after a delay (allows MapLibre to release handles)
 */
private fun deleteCachedFile(
    cacheDir: File,
    fileName: String,
) {
    try {
        val file = File(cacheDir, fileName)
        if (!file.exists()) {
            return
        }

        // CRITICAL SAFETY: .mbtiles files may have open file handles from MapLibre background threads
        // Deleting them immediately causes: "mapbox::sqlite::Exception: unable to open database file"
        // Solution: Rename first (breaks path reference), then delete after MapLibre releases handles
        if (fileName.endsWith(".mbtiles")) {
            deleteMbtilesFileSafely(file, fileName)
        } else {
            // Non-.mbtiles files (geojson, style.json) can be safely deleted immediately
            deleteNonMbtilesFile(file, fileName)
        }
    } catch (e: Exception) {
        Log.e(::clearEventCache.name, "Error while deleting $fileName", e)
    }
}

/**
 * Safely deletes an .mbtiles file using rename-then-delete strategy.
 * Prevents MapLibre crashes by breaking file path reference before actual deletion.
 */
private fun deleteMbtilesFileSafely(
    file: File,
    fileName: String,
) {
    val renamedFile = File(file.parentFile, "$fileName.deleted")

    // Step 1: Rename immediately (breaks MapLibre's path reference, forces it to handle gracefully)
    if (file.renameTo(renamedFile)) {
        Log.i(::clearEventCache.name, "Renamed $fileName to .deleted (safe from MapLibre threads)")

        // Step 2: Schedule actual deletion after MapLibre has time to release handles
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            try {
                if (renamedFile.exists() && renamedFile.delete()) {
                    Log.i(::clearEventCache.name, "Deleted renamed file $fileName.deleted")
                }
            } catch (e: Exception) {
                Log.w(::clearEventCache.name, "Failed to delete renamed $fileName.deleted", e)
            }
        }, MBTILES_DELETE_DELAY_MS)
    } else {
        Log.w(::clearEventCache.name, "Failed to rename $fileName, falling back to direct delete")
        // Fallback to direct delete (risky but better than leaving stale files)
        if (file.delete()) {
            Log.i(::clearEventCache.name, "Deleted cached file $fileName (direct)")
        }
    }
}

/**
 * Deletes non-.mbtiles files (geojson, style.json) immediately.
 * These files don't have threading concerns like .mbtiles database files.
 */
private fun deleteNonMbtilesFile(
    file: File,
    fileName: String,
) {
    if (file.delete()) {
        Log.i(::clearEventCache.name, "Deleted cached file $fileName")
    } else {
        Log.e(::clearEventCache.name, "Failed to delete cached file $fileName")
    }
}
