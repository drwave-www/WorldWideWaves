package com.worldwidewaves.shared

/*
 * Copyright 2025 DrWave
 *
 * WorldWideWaves is an ephemeral mobile app designed to orchestrate human waves through cities and
 * countries, culminating in a global wave. The project aims to transcend physical and cultural
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
import android.os.Build
import android.util.Log
import com.google.android.play.core.splitcompat.SplitCompat
import com.google.android.play.core.splitinstall.SplitInstallManagerFactory
import com.worldwidewaves.shared.generated.resources.Res
import dev.icerock.moko.resources.StringResource
import dev.icerock.moko.resources.desc.desc
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.koin.java.KoinJavaComponent.inject
import org.koin.mp.KoinPlatform
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.IOException

actual suspend fun readGeoJson(eventId: String): String? {
    val filePath = getMapFileAbsolutePath(eventId, "geojson")

    return if (filePath != null) {
        withContext(Dispatchers.IO) {
            Log.i(::readGeoJson.name, "Loading geojson data for event $eventId from $filePath")
            File(filePath).readText()
        }
    } else {
        Log.e(::readGeoJson.name, "Map file not available for event $eventId")
        null
    }
}

// ---------------------------

/**
 * Retrieves the absolute path of a map file for a given event.
 *
 * This function attempts to get the absolute path of a map file (e.g., MBTiles, GeoJSON) associated with the event.
 * It first checks if the file is already cached in the device's cache directory. If the file is not cached or the
 * cached file size does not match the expected size, it reads the file from the resources and caches it.
 *
 */
actual suspend fun getMapFileAbsolutePath(eventId: String, extension: String): String? {
    val context: Context by inject(Context::class.java)
    val cachedFile = File(context.cacheDir, "$eventId.$extension")
    val metadataFile = File(context.cacheDir, "$eventId.$extension.metadata")
    val splitInstallManager = SplitInstallManagerFactory.create(context)

    if (!splitInstallManager.installedModules.contains(eventId)) {
        Log.e(::getMapFileAbsolutePath.name, "Feature module $eventId is not installed")
        return null
    }

    // Ensure split compat is installed
    SplitCompat.install(context)

    return try {
        Log.i(::getMapFileAbsolutePath.name, "Trying to get $eventId.$extension from feature module")

        val assetPath = "$eventId.$extension"

        try {
            // Get asset information - this might not provide accurate size for compressed assets
            val assetFileDescriptor = try {
                context.assets.openFd(assetPath)
            } catch (_: IOException) {
                // Some compressed assets can't be accessed via openFd, fall back to open
                null
            }

            val needsUpdate = when {
                !cachedFile.exists() -> true
                !metadataFile.exists() -> true
                else -> {
                    val lastCacheTime = try {
                        metadataFile.readText().toLong()
                    } catch (_: Exception) {
                        0L
                    }

                    // Check if the app was installed/updated after we cached the file
                    val appInstallTime = try {
                        context.packageManager.getPackageInfo(context.packageName, 0).lastUpdateTime
                    } catch (_: Exception) {
                        System.currentTimeMillis()
                    }

                    // If the app was updated after we cached the file, we should update the cache
                    appInstallTime > lastCacheTime
                }
            }

            // Close the descriptor if we opened one
            assetFileDescriptor?.close()

            if (needsUpdate) {
                Log.i(::getMapFileAbsolutePath.name, "Caching $eventId.$extension")

                withContext(Dispatchers.IO) {
                    try {
                        // Use a buffered approach for better memory efficiency
                        context.assets.open(assetPath).use { input ->
                            BufferedInputStream(input, 8192).use { bufferedInput ->
                                cachedFile.outputStream().use { fileOutput ->
                                    BufferedOutputStream(fileOutput, 8192).use { bufferedOutput ->
                                        val buffer = ByteArray(8192)
                                        var bytesRead: Int

                                        while (bufferedInput.read(buffer).also { bytesRead = it } != -1) {
                                            bufferedOutput.write(buffer, 0, bytesRead)
                                        }

                                        bufferedOutput.flush()
                                    }
                                }
                            }
                        }

                        // Update metadata after successful copy
                        metadataFile.writeText(System.currentTimeMillis().toString())
                    } catch (e: Exception) {
                        Log.e(::getMapFileAbsolutePath.name, "Error caching file: ${e.message}")
                        // Delete partially written file if there was an error
                        if (cachedFile.exists()) {
                            cachedFile.delete()
                        }
                        throw e
                    }
                }
            }

            cachedFile.absolutePath
        } catch (e: IOException) {
            Log.e(::getMapFileAbsolutePath.name, "Resource not found in module: ${e.message}")
            return null
        }
    } catch (e: Exception) {
        Log.e(::getMapFileAbsolutePath.name, "Error loading map from feature module: ${e.message}")
        e.printStackTrace()
        null
    }
}

// ---------------------------

/**
 * Checks if a cached file exists in the application's cache directory.
 *
 * This function determines whether a file with the specified name exists in the cache directory.
 * It also considers whether the application is running in development mode, in which case it always
 * returns `false` to simulate the absence of cached files.
 *
 */
actual fun cachedFileExists(fileName: String): Boolean {
    val context: Context by inject(Context::class.java)
    val isDevelopmentMode = Build.HARDWARE == "ranchu" || Build.HARDWARE == "goldfish"

    return if (isDevelopmentMode) {
        Log.i(::cachedFileExists.name, "Development mode (not cached): $fileName")
        false
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
actual fun cachedFilePath(fileName: String): String? {
    val context: Context by inject(Context::class.java)
    return File(context.cacheDir, fileName).takeIf { it.exists() }?.toURI()?.path
}

/**
 * Caches a string content to a file in the application's cache directory.
 *
 * This function writes the provided string content to a file with the specified name
 * in the cache directory of the application. It logs the file name to which the data
 * is being cached.
 *
 */
actual fun cacheStringToFile(fileName: String, content: String) : String {
    val context: Context by inject(Context::class.java)
    Log.v(::cacheStringToFile.name, "Caching data to $fileName")
    File(context.cacheDir, fileName).writeText(content)
    return fileName
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
    } catch (e: Exception) {
        Log.e(::cacheDeepFile.name, "Error caching file: $fileName", e)
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

// ---------------------------------------------------------------------------
//  Cache-maintenance helpers (Android actuals)
// ---------------------------------------------------------------------------

/**
 * Delete all cached artefacts (data + metadata files) that belong to a given map/event.
 */
actual fun clearEventCache(eventId: String) {
    val context: Context by inject(Context::class.java)
    val cacheDir = context.cacheDir

    val targets = listOf(
        "$eventId.mbtiles",
        "$eventId.mbtiles.metadata",
        "$eventId.geojson",
        "$eventId.geojson.metadata",
        "style-$eventId.json",
        "style-$eventId.json.metadata"
    )

    for (name in targets) {
        try {
            val f = File(cacheDir, name)
            if (f.exists()) {
                if (f.delete()) {
                    Log.i(::clearEventCache.name, "Deleted cached file $name")
                } else {
                    Log.e(::clearEventCache.name, "Failed to delete cached file $name")
                }
            }
        } catch (e: Exception) {
            Log.e(::clearEventCache.name, "Error while deleting $name", e)
        }
    }
}

/**
 * Determine whether an already-cached file is stale with regard to the
 * application’s lastUpdateTime (which also changes when dynamic-feature
 * splits are updated through the Play Store).
 */
actual fun isCachedFileStale(fileName: String): Boolean {
    val context: Context by inject(Context::class.java)
    val cacheDir = context.cacheDir

    val dataFile = File(cacheDir, fileName)
    if (!dataFile.exists()) return true

    val metadataFile = File(cacheDir, "$fileName.metadata")
    val cachedTime = try {
        metadataFile.takeIf { it.exists() }?.readText()?.toLong() ?: 0L
    } catch (_: Exception) {
        0L
    }

    val appUpdateTime = try {
        context.packageManager.getPackageInfo(context.packageName, 0).lastUpdateTime
    } catch (_: Exception) {
        System.currentTimeMillis()
    }

    return appUpdateTime > cachedTime
}

/**
 * Force-update (or create) the metadata timestamp associated with a cached file.
 */
actual fun updateCacheMetadata(fileName: String) {
    val context: Context by inject(Context::class.java)
    val metadataFile = File(context.cacheDir, "$fileName.metadata")
    try {
        metadataFile.writeText(System.currentTimeMillis().toString())
    } catch (e: Exception) {
        Log.e(::updateCacheMetadata.name, "Could not write metadata for $fileName", e)
    }
}

// -----------------------------------------------------------

actual fun localizeString(resource: StringResource): String {
    val context = KoinPlatform.getKoin().get<Context>()
    return resource.desc().toString(context)
}