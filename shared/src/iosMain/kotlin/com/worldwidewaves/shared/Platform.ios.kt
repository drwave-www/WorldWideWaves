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
package com.worldwidewaves.shared

import com.worldwidewaves.shared.di.IOSModule
import com.worldwidewaves.shared.di.sharedModule
import com.worldwidewaves.shared.events.utils.GeoJsonDataProvider
import com.worldwidewaves.shared.utils.Log
import com.worldwidewaves.shared.utils.initNapier
import com.worldwidewaves.shared.utils.setupDebugSimulation
import dev.icerock.moko.resources.StringResource
import dev.icerock.moko.resources.desc.desc
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.koin.core.logger.PrintLogger
import platform.Foundation.NSBundle
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.stringWithContentsOfFile
import platform.Foundation.writeToFile

/**
 * Initialise Koin for iOS.
 *
 * Swift code calls this via `HelperKt.doInitKoin()`.
 * We load every common module *and* the iOS-specific module only once.
 */
@Throws(Throwable::class)
fun doInitPlatform() {
    // Prevent multiple initialisations when called repeatedly from Swift previews/tests.
    if (koinApp != null) return

    // Initialize Napier logging for iOS
    Log.v(TAG, "HELPER: doInitKoin() starting with enhanced coroutine exception handling")

    // Initialize MokoRes bundle BEFORE anything else
    Log.v(TAG, "HELPER: About to initialize MokoRes bundle")
    try {
        val bundleInitialized = BundleInitializer.initializeBundle()
        Log.i(TAG, "HELPER: MokoRes bundle initialization result: $bundleInitialized")
    } catch (e: IllegalStateException) {
        Log.e(TAG, "ERROR: MokoRes bundle state error: ${e.message}")
    } catch (e: Exception) {
        Log.e(TAG, "ERROR: MokoRes bundle initialization failed: ${e.message}")
    }

    // Re-enable initNapier with bulletproof OSLogAntilog
    Log.v(TAG, "HELPER: About to call initNapier()")
    try {
        initNapier()
        Log.i(TAG, "HELPER: initNapier() completed successfully")
    } catch (e: IllegalStateException) {
        Log.e(TAG, "ERROR: Napier state error: ${e.message}")
    } catch (e: Exception) {
        Log.e(TAG, "ERROR: initNapier() failed: ${e.message}")
    }

    try {
        Log.v(TAG, "HELPER: About to create startKoin block...")
        koinApp =
            startKoin {
                // Add iOS logging - use INFO level to reduce excessive debug output
                logger(PrintLogger(if (BuildKonfig.DEBUG) Level.INFO else Level.ERROR))
                Log.v(TAG, "HELPER: Logger added")
                modules(sharedModule + IOSModule)
                Log.v(TAG, "HELPER: Modules added")
            }
        Log.i(TAG, "HELPER: startKoin completed successfully")

        // Setup debug simulation after Koin initialization (same as Android MainApplication)
        if (BuildKonfig.DEBUG) {
            try {
                setupDebugSimulation()
            } catch (e: Exception) {
                Log.w(TAG, "DEBUG: Failed to setup debug simulation: ${e.message}")
            }
        }
    } catch (e: IllegalStateException) {
        Log.e(TAG, "ERROR: Koin state error: ${e.message}")
        Log.e(TAG, "ERROR: Exception type: ${e::class.simpleName}")
    } catch (e: Exception) {
        Log.e(TAG, "ERROR: startKoin failed: ${e.message}")
        Log.e(TAG, "ERROR: Exception type: ${e::class.simpleName}")
    }
}

// Private holder to remember if Koin has already been started.
// `KoinApplication` is available on every KMP target so we can
// safely keep the reference here.
private var koinApp: KoinApplication? = null

/**
 * Platform descriptor for iOS.
 * Simply instantiate the common `WWWPlatform` with the device name/version.
 */

@OptIn(ExperimentalForeignApi::class)
actual suspend fun readGeoJson(eventId: String): String? {
    // Production-grade iOS implementation: Read GeoJSON from ODR bundle resources
    return try {
        Log.v("readGeoJson", "Loading GeoJSON for event $eventId")

        val filePath = getMapFileAbsolutePath(eventId, "geojson")
        if (filePath != null) {
            Log.i("readGeoJson", "Reading GeoJSON from: $filePath")

            // Use iOS Foundation API to read file contents safely
            val content =
                NSString
                    .stringWithContentsOfFile(
                        path = filePath,
                        encoding = NSUTF8StringEncoding,
                        error = null,
                    )?.toString()

            if (content != null) {
                val preview = content.take(100).replace("\n", " ")
                Log.i("readGeoJson", "Successfully loaded GeoJSON (${content.length} chars): $preview...")
                content
            } else {
                Log.w("readGeoJson", "Failed to read file content from $filePath")
                null
            }
        } else {
            Log.d("readGeoJson", "GeoJSON file not found for event $eventId")
            null
        }
    } catch (e: Exception) {
        Log.e("readGeoJson", "Error reading GeoJSON for event $eventId: ${e.message}", e)
        null
    }
}

@OptIn(ExperimentalForeignApi::class)
@Throws(Throwable::class)
actual suspend fun getMapFileAbsolutePath(
    eventId: String,
    extension: String,
): String? {
    // iOS ODR: Try direct access first, fall back to cache if needed
    return try {
        val fileName = "$eventId.$extension"
        Log.v("getMapFileAbsolutePath", "[$eventId] Looking for ODR file: $fileName")

        // Priority 1: Check cache directory first (for extracted files)
        val cacheDir = getCacheDir()
        val cachePath = "$cacheDir/$fileName"
        if (NSFileManager.defaultManager.fileExistsAtPath(cachePath)) {
            Log.i("getMapFileAbsolutePath", "[$eventId] Found cached file: $cachePath")
            return cachePath
        }

        // Priority 2: Try direct ODR access from bundle
        val bundle = NSBundle.mainBundle
        val odrPaths =
            listOf(
                bundle.pathForResource(eventId, extension, "Resources/Maps/$eventId"),
                bundle.pathForResource(eventId, extension, "Maps/$eventId"),
                bundle.pathForResource(eventId, extension, "Resources/Maps"),
                bundle.pathForResource(eventId, extension, "Maps"),
                bundle.pathForResource(eventId, extension),
            )

        for (path in odrPaths) {
            if (path != null && NSFileManager.defaultManager.fileExistsAtPath(path)) {
                Log.i("getMapFileAbsolutePath", "[$eventId] Found ODR file directly: $path")

                // Try to read the file to verify it's accessible
                val content = NSString.stringWithContentsOfFile(path, NSUTF8StringEncoding, null)
                if (content != null) {
                    Log.i("getMapFileAbsolutePath", "[$eventId] ODR file is directly readable")
                    return path
                } else {
                    Log.w("getMapFileAbsolutePath", "[$eventId] ODR file found but not readable: $path")
                    // Fall through to cache the file
                    break
                }
            }
        }

        Log.w("getMapFileAbsolutePath", "[$eventId] ODR file not found or not accessible: $fileName")
        Log.i("getMapFileAbsolutePath", "[$eventId] File should be available after ODR download completes")
        null
    } catch (e: Exception) {
        Log.e("getMapFileAbsolutePath", "[$eventId] Error accessing ODR file $eventId.$extension: ${e.message}", e)
        null
    }
}

actual fun cachedFileExists(fileName: String): Boolean {
    // iOS ODR: Check cache first, then ODR direct access
    val cacheDir = getCacheDir()
    val cachePath = "$cacheDir/$fileName"
    if (NSFileManager.defaultManager.fileExistsAtPath(cachePath)) {
        return true
    }

    // Check ODR direct access
    val eventId = fileName.substringBeforeLast('.')
    val extension = fileName.substringAfterLast('.')
    val bundle = NSBundle.mainBundle

    val odrPaths =
        listOf(
            bundle.pathForResource(eventId, extension, "Resources/Maps/$eventId"),
            bundle.pathForResource(eventId, extension, "Maps/$eventId"),
            bundle.pathForResource(eventId, extension, "Resources/Maps"),
            bundle.pathForResource(eventId, extension, "Maps"),
            bundle.pathForResource(eventId, extension),
        )

    return odrPaths.any { path ->
        path != null && NSFileManager.defaultManager.fileExistsAtPath(path)
    }
}

actual fun cachedFilePath(fileName: String): String? {
    // iOS ODR: Return cache path if exists, otherwise ODR path
    val cacheDir = getCacheDir()
    val cachePath = "$cacheDir/$fileName"
    if (NSFileManager.defaultManager.fileExistsAtPath(cachePath)) {
        return NSURL.fileURLWithPath(cachePath).absoluteString
    }

    // Check ODR direct access
    val eventId = fileName.substringBeforeLast('.')
    val extension = fileName.substringAfterLast('.')
    val bundle = NSBundle.mainBundle

    val odrPaths =
        listOf(
            bundle.pathForResource(eventId, extension, "Resources/Maps/$eventId"),
            bundle.pathForResource(eventId, extension, "Maps/$eventId"),
            bundle.pathForResource(eventId, extension, "Resources/Maps"),
            bundle.pathForResource(eventId, extension, "Maps"),
            bundle.pathForResource(eventId, extension),
        )

    for (path in odrPaths) {
        if (path != null && NSFileManager.defaultManager.fileExistsAtPath(path)) {
            return NSURL.fileURLWithPath(path).absoluteString
        }
    }

    return null
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
@Throws(Throwable::class)
actual fun cacheStringToFile(
    fileName: String,
    content: String,
): String {
    val cacheDir = getCacheDir()
    val filePath = "$cacheDir/$fileName"
    val nsString = NSString.create(string = content)
    nsString.writeToFile(filePath, true, NSUTF8StringEncoding, null)
    return fileName
}

actual fun getCacheDir(): String =
    NSSearchPathForDirectoriesInDomains(
        NSCachesDirectory,
        NSUserDomainMask,
        true,
    ).first() as String

@Throws(Throwable::class)
actual suspend fun cacheDeepFile(fileName: String) {
    // iOS ODR: Cache file if direct access fails
    try {
        val eventId = fileName.substringBeforeLast('.')
        val extension = fileName.substringAfterLast('.')

        Log.v("cacheDeepFile", "[$eventId] Attempting to cache ODR file: $fileName")

        val cacheDir = getCacheDir()
        val cachedFilePath = "$cacheDir/$fileName"

        // Check if already cached
        if (NSFileManager.defaultManager.fileExistsAtPath(cachedFilePath)) {
            Log.d("cacheDeepFile", "[$eventId] File already cached: $fileName")
            return
        }

        // Try to find ODR file and copy to cache if not directly readable
        val bundle = NSBundle.mainBundle
        val odrPaths =
            listOf(
                bundle.pathForResource(eventId, extension, "Resources/Maps/$eventId"),
                bundle.pathForResource(eventId, extension, "Maps/$eventId"),
                bundle.pathForResource(eventId, extension, "Resources/Maps"),
                bundle.pathForResource(eventId, extension, "Maps"),
                bundle.pathForResource(eventId, extension),
            )

        for (path in odrPaths) {
            if (path != null && NSFileManager.defaultManager.fileExistsAtPath(path)) {
                // Copy ODR file to cache for reliable access
                val copySuccess = NSFileManager.defaultManager.copyItemAtPath(path, cachedFilePath, null)
                if (copySuccess) {
                    Log.i("cacheDeepFile", "[$eventId] Successfully cached ODR file: $fileName -> $cachedFilePath")
                    return
                } else {
                    Log.w("cacheDeepFile", "[$eventId] Failed to copy ODR file to cache: $fileName")
                }
            }
        }

        Log.w("cacheDeepFile", "[$eventId] ODR file not found for caching: $fileName")
    } catch (e: Exception) {
        Log.w("cacheDeepFile", "[$fileName] Error caching ODR file: ${e.message}", e)
    }
}

// ---------------------------------------------------------------------------
//  Cache-maintenance helpers – no-op on iOS (resources are bundled & immutable)
// ---------------------------------------------------------------------------

actual fun clearEventCache(eventId: String) {
    // iOS ODR: Clear cached files and in-memory cache
    try {
        val cacheDir = getCacheDir()
        val geoJsonFile = "$cacheDir/$eventId.geojson"
        val mbtileFile = "$cacheDir/$eventId.mbtiles"

        // Remove cached files if they exist
        var filesRemoved = 0
        if (NSFileManager.defaultManager.fileExistsAtPath(geoJsonFile)) {
            NSFileManager.defaultManager.removeItemAtPath(geoJsonFile, null)
            Log.i("clearEventCache", "[$eventId] Removed cached GeoJSON: $geoJsonFile")
            filesRemoved++
        }
        if (NSFileManager.defaultManager.fileExistsAtPath(mbtileFile)) {
            NSFileManager.defaultManager.removeItemAtPath(mbtileFile, null)
            Log.i("clearEventCache", "[$eventId] Removed cached MBTiles: $mbtileFile")
            filesRemoved++
        }

        // Invalidate GeoJSON cache in memory
        val koin =
            org.koin.mp.KoinPlatform
                .getKoin()
        val geoJsonProvider = koin.get<GeoJsonDataProvider>()
        geoJsonProvider.invalidateCache(eventId)

        Log.i("clearEventCache", "[$eventId] Cleared cache: $filesRemoved files removed, memory cache invalidated")
    } catch (e: IllegalStateException) {
        Log.w("clearEventCache", "[$eventId] State error clearing cache: ${e.message}")
    } catch (e: Exception) {
        Log.w("clearEventCache", "[$eventId] Error clearing cache: ${e.message}")
    }
}

actual fun isCachedFileStale(fileName: String): Boolean = false

actual fun updateCacheMetadata(fileName: String) {
    // no-op on iOS
}

// ---------------------------

actual fun localizeString(resource: StringResource): String {
    // Convert StringResource to localized string using iOS framework
    return resource.desc().localized()
}
