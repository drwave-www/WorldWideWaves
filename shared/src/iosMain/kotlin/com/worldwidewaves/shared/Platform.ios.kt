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
    // Production-grade iOS implementation: Check cache and ODR bundle resources
    return try {
        val fileName = "$eventId.$extension"
        Log.v("getMapFileAbsolutePath", "Looking for $fileName")

        // Priority 1: Check cache directory (downloaded maps)
        val cacheDir = getCacheDir()
        val cachePath = "$cacheDir/$fileName"
        if (NSFileManager.defaultManager.fileExistsAtPath(cachePath)) {
            Log.d("getMapFileAbsolutePath", "Found $fileName in cache: $cachePath")
            return cachePath
        }

        // Priority 2: Check main bundle resources (ODR maps)
        val bundle = NSBundle.mainBundle
        val resourcePath = bundle.pathForResource(eventId, extension)
        if (resourcePath != null && NSFileManager.defaultManager.fileExistsAtPath(resourcePath)) {
            Log.d("getMapFileAbsolutePath", "Found $fileName in bundle: $resourcePath")
            return resourcePath
        }

        // Priority 3: Check Maps subdirectory in bundle (alternative ODR structure)
        val mapsResourcePath = bundle.pathForResource(eventId, extension, "Maps")
        if (mapsResourcePath != null && NSFileManager.defaultManager.fileExistsAtPath(mapsResourcePath)) {
            Log.d("getMapFileAbsolutePath", "Found $fileName in Maps subdirectory: $mapsResourcePath")
            return mapsResourcePath
        }

        // Priority 4: Check Resources/Maps subdirectory (alternative ODR structure)
        val resourcesMapsPath = bundle.pathForResource(eventId, extension, "Resources/Maps")
        if (resourcesMapsPath != null && NSFileManager.defaultManager.fileExistsAtPath(resourcesMapsPath)) {
            Log.d("getMapFileAbsolutePath", "Found $fileName in Resources/Maps: $resourcesMapsPath")
            return resourcesMapsPath
        }

        // Priority 5: Check city subdirectory structure: Maps/eventId/eventId.extension
        val citySubdirPath = bundle.pathForResource(eventId, extension, "Maps/$eventId")
        if (citySubdirPath != null && NSFileManager.defaultManager.fileExistsAtPath(citySubdirPath)) {
            Log.d("getMapFileAbsolutePath", "Found $fileName in Maps/$eventId: $citySubdirPath")
            return citySubdirPath
        }

        // Priority 6: Check Resources/Maps city subdirectory: Resources/Maps/eventId/eventId.extension
        val resourcesCitySubdirPath = bundle.pathForResource(eventId, extension, "Resources/Maps/$eventId")
        if (resourcesCitySubdirPath != null && NSFileManager.defaultManager.fileExistsAtPath(resourcesCitySubdirPath)) {
            Log.d("getMapFileAbsolutePath", "Found $fileName in Resources/Maps/$eventId: $resourcesCitySubdirPath")
            return resourcesCitySubdirPath
        }

        Log.d("getMapFileAbsolutePath", "Map file $fileName not found in any location")
        null
    } catch (e: Exception) {
        Log.e("getMapFileAbsolutePath", "Error accessing map file $eventId.$extension: ${e.message}", e)
        null
    }
}

actual fun cachedFileExists(fileName: String): Boolean {
    val cacheDir = getCacheDir()
    val filePath = "$cacheDir/$fileName"
    return NSFileManager.defaultManager.fileExistsAtPath(filePath)
}

actual fun cachedFilePath(fileName: String): String? {
    val cacheDir = getCacheDir()
    val filePath = "$cacheDir/$fileName"
    return NSURL.fileURLWithPath(filePath).absoluteString
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
    // iOS implementation: Cache a file from deep/nested resources
    // For iOS, this typically involves copying from app bundle to cache directory
    try {
        // This would involve reading from iOS Bundle resources and writing to cache
        // Implementation depends on iOS resource bundling strategy
        // For now, this is a no-op as files are typically pre-bundled in iOS
    } catch (e: Exception) {
        Log.w("cacheDeepFile", "Error caching file $fileName: ${e.message}")
        // File caching is not critical for iOS operation
    }
}

// ---------------------------------------------------------------------------
//  Cache-maintenance helpers – no-op on iOS (resources are bundled & immutable)
// ---------------------------------------------------------------------------

actual fun clearEventCache(eventId: String) {
    // Also invalidate GeoJSON cache in memory
    try {
        // Safe iOS approach - use direct Koin access without KoinComponent
        val koin =
            org.koin.mp.KoinPlatform
                .getKoin()
        val geoJsonProvider = koin.get<GeoJsonDataProvider>()
        geoJsonProvider.invalidateCache(eventId)
    } catch (e: IllegalStateException) {
        Log.w("clearEventCache", "State error clearing cache for event $eventId: ${e.message}")
        // Cache invalidation is not critical for iOS operation
    } catch (e: Exception) {
        Log.w("clearEventCache", "Unexpected error clearing cache for event $eventId: ${e.message}")
        // Cache invalidation is not critical for iOS operation
    }
    // Note: Other map assets are shipped inside the app bundle and don't need clearing
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
