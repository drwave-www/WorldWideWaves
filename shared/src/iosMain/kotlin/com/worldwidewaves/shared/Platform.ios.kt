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
    // iOS ODR: Use correct Bundle API pattern - no subdirectories, just name + extension
    return try {
        val fileName = "$eventId.$extension"
        Log.v("getMapFileAbsolutePath", "[$eventId] Checking ODR availability for: $fileName")

        // Get MapAvailabilityChecker to verify ODR status AND ensure request is retained
        val koin =
            org.koin.mp.KoinPlatform
                .getKoin()
        val mapChecker = koin.get<com.worldwidewaves.shared.domain.usecases.MapAvailabilityChecker>()

        if (!mapChecker.isMapDownloaded(eventId)) {
            Log.w("getMapFileAbsolutePath", "[$eventId] ODR resources not available - map not downloaded")
            return null
        }

        Log.v("getMapFileAbsolutePath", "[$eventId] ODR confirmed available, resolving path: $fileName")

        // CORRECT PATTERN: Use Bundle.main with name + extension only (no subdirectory)
        // The NSBundleResourceRequest must be retained for this to work
        val bundle = NSBundle.mainBundle
        val resourcePath = bundle.pathForResource(eventId, extension)

        if (resourcePath != null && NSFileManager.defaultManager.fileExistsAtPath(resourcePath)) {
            Log.i("getMapFileAbsolutePath", "[$eventId] Found ODR file: $resourcePath")
            return resourcePath
        } else {
            // Debug: Log what Bundle actually sees
            Log.w("getMapFileAbsolutePath", "[$eventId] Bundle.pathForResource returned: $resourcePath")

            // Try URL-based approach as fallback
            val resourceURL = bundle.URLForResource(eventId, extension)
            if (resourceURL != null) {
                val urlPath = resourceURL.path
                Log.i("getMapFileAbsolutePath", "[$eventId] Found ODR file via URL: $urlPath")
                return urlPath
            }

            Log.e("getMapFileAbsolutePath", "[$eventId] ODR file not found - request may not be retained")
            return null
        }
    } catch (e: Exception) {
        Log.e("getMapFileAbsolutePath", "[$eventId] Error resolving ODR file $eventId.$extension: ${e.message}", e)
        null
    }
}

actual fun cachedFileExists(fileName: String): Boolean {
    // Handle different file types: style files are cached, map files are in ODR bundle
    if (fileName.startsWith("style-")) {
        // Style files are in cache directory
        val cacheDir = getCacheDir()
        val filePath = "$cacheDir/$fileName"
        return NSFileManager.defaultManager.fileExistsAtPath(filePath)
    } else {
        // Map files are in ODR bundle - MUST respect ODR lifecycle
        val eventId = fileName.substringBeforeLast('.')

        try {
            // Check ODR availability via MapAvailabilityChecker
            val koin =
                org.koin.mp.KoinPlatform
                    .getKoin()
            val mapChecker = koin.get<com.worldwidewaves.shared.domain.usecases.MapAvailabilityChecker>()

            if (!mapChecker.isMapDownloaded(eventId)) {
                Log.v("cachedFileExists", "ODR resources not available for: $fileName")
                return false
            }

            // ONLY after ODR availability confirmed, check Bundle APIs
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
        } catch (e: Exception) {
            Log.e("cachedFileExists", "Error checking ODR availability for: $fileName", throwable = e)
            return false
        }
    }
}

actual fun cachedFilePath(fileName: String): String? {
    val fileNameWithoutExt = fileName.substringBeforeLast('.')
    val extension = fileName.substringAfterLast('.')
    val bundle = NSBundle.mainBundle

    Log.v("cachedFilePath", "Looking for file: $fileName")

    // Handle different file types: style files are generated and cached, map files are in ODR bundle
    if (fileName.startsWith("style-")) {
        // Style files (e.g., "style-paris_france.json") are generated and cached
        val cacheDir = getCacheDir()
        val filePath = "$cacheDir/$fileName"
        return if (NSFileManager.defaultManager.fileExistsAtPath(filePath)) {
            // Convert filesystem path to file:// URL for MapLibre compatibility
            "file://$filePath"
        } else {
            Log.w("cachedFilePath", "Style file not found in cache: $filePath")
            null
        }
    } else {
        // Map files (e.g., "paris_france.mbtiles", "paris_france.geojson") are in ODR bundle
        // MUST respect ODR lifecycle - only accessible after beginAccessingResources() succeeds
        val eventId = fileNameWithoutExt // e.g., "paris_france"

        try {
            // Check ODR availability via MapAvailabilityChecker
            val koin =
                org.koin.mp.KoinPlatform
                    .getKoin()
            val mapChecker = koin.get<com.worldwidewaves.shared.domain.usecases.MapAvailabilityChecker>()

            if (!mapChecker.isMapDownloaded(eventId)) {
                Log.w("cachedFilePath", "ODR resources not available for: $fileName")
                return null
            }

            Log.v("cachedFilePath", "ODR confirmed available for: $fileName")

            // ONLY after ODR availability confirmed, resolve via Bundle APIs
            val odrUrls: List<platform.Foundation.NSURL?> =
                listOf(
                    bundle.URLForResource(eventId, withExtension = extension, subdirectory = "Resources/Maps/$eventId"),
                    bundle.URLForResource(eventId, withExtension = extension, subdirectory = "Maps/$eventId"),
                    bundle.URLForResource(eventId, withExtension = extension, subdirectory = "Resources/Maps"),
                    bundle.URLForResource(eventId, withExtension = extension, subdirectory = "Maps"),
                    bundle.URLForResource(eventId, withExtension = extension, subdirectory = null),
                )

            for (url in odrUrls) {
                if (url != null) {
                    val absoluteString = url.absoluteString
                    if (absoluteString != null) {
                        Log.v("cachedFilePath", "Found ODR file after availability check: $absoluteString")
                        return absoluteString
                    }
                }
            }

            Log.e("cachedFilePath", "ODR shows available but file not found - ODR lifecycle issue: $fileName")
            return null
        } catch (e: Exception) {
            Log.e("cachedFilePath", "Error checking ODR availability for: $fileName", throwable = e)
            return null
        }
    }
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
    // iOS ODR: No caching needed - files are directly accessible after beginAccessingResources
    Log.v("cacheDeepFile", "[$fileName] iOS ODR: No caching needed, files directly accessible via Bundle APIs")
}

// ---------------------------------------------------------------------------
//  Cache-maintenance helpers – no-op on iOS (resources are bundled & immutable)
// ---------------------------------------------------------------------------

actual fun clearEventCache(eventId: String) {
    // iOS ODR: Only clear in-memory cache - ODR files managed by system
    try {
        // Invalidate GeoJSON cache in memory
        val koin =
            org.koin.mp.KoinPlatform
                .getKoin()
        val geoJsonProvider = koin.get<GeoJsonDataProvider>()
        geoJsonProvider.invalidateCache(eventId)

        Log.i("clearEventCache", "[$eventId] Cleared in-memory cache (ODR files managed by iOS)")
    } catch (e: IllegalStateException) {
        Log.w("clearEventCache", "[$eventId] State error clearing memory cache: ${e.message}")
    } catch (e: Exception) {
        Log.w("clearEventCache", "[$eventId] Error clearing memory cache: ${e.message}")
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
