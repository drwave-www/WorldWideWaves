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
import dev.icerock.moko.resources.StringResource
import dev.icerock.moko.resources.desc.desc
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import org.koin.core.context.loadKoinModules
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.writeToFile

fun initKoinIOS() {
    // Initialise Koin only once (see Helper.doInitKoin).
    doInitKoin()
    loadKoinModules(IOSModule)
}

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
        null
    }
}

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

actual suspend fun cacheDeepFile(fileName: String) {
    // iOS implementation: Cache a file from deep/nested resources
    // For iOS, this typically involves copying from app bundle to cache directory
    try {
        // This would involve reading from iOS Bundle resources and writing to cache
        // Implementation depends on iOS resource bundling strategy
        // For now, this is a no-op as files are typically pre-bundled in iOS
    } catch (e: Exception) {
        // Silent failure - file caching is not critical for iOS operation
    }
}

// ---------------------------------------------------------------------------
//  Cache-maintenance helpers – no-op on iOS (resources are bundled & immutable)
// ---------------------------------------------------------------------------

actual fun clearEventCache(eventId: String) {
    // no-op on iOS – all map assets are shipped inside the app bundle
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
