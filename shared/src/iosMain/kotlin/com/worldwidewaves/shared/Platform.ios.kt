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
import com.worldwidewaves.shared.utils.Log
import com.worldwidewaves.shared.utils.initNapier
import dev.icerock.moko.resources.StringResource
import dev.icerock.moko.resources.desc.desc
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.koin.core.logger.PrintLogger
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
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
                // Add iOS logging equivalent to Android's androidLogger()
                logger(PrintLogger(Level.DEBUG))
                Log.v(TAG, "HELPER: Logger added")
                modules(sharedModule + IOSModule)
                Log.v(TAG, "HELPER: Modules added")
            }
        Log.i(TAG, "HELPER: startKoin completed successfully")

        // Setup debug simulation after Koin initialization (same as Android MainApplication)
        try {
            com.worldwidewaves.shared.utils
                .setupDebugSimulation()
        } catch (e: Exception) {
            Log.w(TAG, "DEBUG: Failed to setup debug simulation: ${e.message}")
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

actual suspend fun readGeoJson(eventId: String): String? {
    // iOS implementation: Read GeoJSON from app bundle resources
    // For iOS, GeoJSON files are typically bundled within the app
    return try {
        // This would read from iOS Bundle.main.path(forResource:)
        // For now, return null to indicate resource not found
        // Implementation depends on iOS resource management strategy
        null
    } catch (e: Exception) {
        Log.w("readGeoJson", "Error reading GeoJSON for event $eventId: ${e.message}")
        null
    }
}

@Throws(Throwable::class)
actual suspend fun getMapFileAbsolutePath(
    eventId: String,
    extension: String,
): String? {
    // iOS implementation: Get absolute path to map file
    // iOS apps store resources in the app bundle or Documents directory
    return try {
        val cacheDir = getCacheDir()
        val fileName = "$eventId.$extension"
        val filePath = "$cacheDir/$fileName"

        // Check if file exists in cache directory
        if (NSFileManager.defaultManager.fileExistsAtPath(filePath)) {
            filePath
        } else {
            // File not found in cache, could check app bundle resources here
            null
        }
    } catch (e: Exception) {
        Log.w("getMapFileAbsolutePath", "Error accessing map file for event $eventId.$extension: ${e.message}")
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
        val geoJsonProvider = koin.get<com.worldwidewaves.shared.events.utils.GeoJsonDataProvider>()
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
