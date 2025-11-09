@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

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

import com.worldwidewaves.shared.generated.resources.Res
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.jetbrains.compose.resources.ExperimentalResourceApi
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSData
import platform.Foundation.NSDate
import platform.Foundation.NSFileManager
import platform.Foundation.NSString
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.stringByAppendingPathComponent
import platform.Foundation.stringByDeletingLastPathComponent
import platform.Foundation.stringWithContentsOfFile
import platform.Foundation.timeIntervalSince1970
import platform.Foundation.writeToFile

private fun cacheRoot(): String {
    val urls =
        NSFileManager.defaultManager.URLsForDirectory(
            directory = NSCachesDirectory,
            inDomains = NSUserDomainMask,
        )
    val url = (urls.lastOrNull() as? NSURL)
    return (url?.path ?: NSTemporaryDirectory()).trimEnd('/')
}

@OptIn(ExperimentalForeignApi::class)
private fun joinPath(
    dir: String,
    name: String,
): String = NSString.create(string = dir).stringByAppendingPathComponent(name)

/** ---------- expect/actuals ---------- */

actual fun getCacheDir(): String = cacheRoot()

actual suspend fun cachedFileExists(fileName: String): Boolean {
    val full = joinPath(cacheRoot(), fileName)
    return NSFileManager.defaultManager.fileExistsAtPath(full)
}

actual suspend fun cachedFilePath(fileName: String): String? {
    val full = joinPath(cacheRoot(), fileName)
    return if (NSFileManager.defaultManager.fileExistsAtPath(full)) full else null
}

@OptIn(ExperimentalResourceApi::class)
actual suspend fun cacheDeepFile(fileName: String) {
    try {
        val bytes =
            com.worldwidewaves.shared.generated.resources.Res
                .readBytes(fileName)

        val root = cacheRoot()
        val fullPath = NSString.create(string = root).stringByAppendingPathComponent(fileName)
        val parent = NSString.create(string = fullPath).stringByDeletingLastPathComponent

        NSFileManager.defaultManager.createDirectoryAtPath(
            path = parent,
            withIntermediateDirectories = true,
            attributes = null,
            error = null,
        )

        bytes.usePinned { pinned ->
            NSData
                .create(bytes = pinned.addressOf(0), length = bytes.size.toULong())
                .writeToFile(fullPath, atomically = true)
        }
    } catch (_: Throwable) {
        // swallow; callers handle cache misses
    }
}

/**
 * iOS "staleness": compare our metadata version stamp to the current app version.
 * If no data file or metadata exists, it's stale.
 *
 * This uses version stamps (e.g., "1.0+7") instead of bundle modification time
 * to prevent cached maps from being invalidated on every app update.
 */
actual suspend fun isCachedFileStale(fileName: String): Boolean =
    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
        val root = cacheRoot()
        val dataPath = joinPath(root, fileName)
        if (!NSFileManager.defaultManager.fileExistsAtPath(dataPath)) return@withContext true

        val metaPath = joinPath(root, "$fileName.metadata")
        val metaText = NSString.stringWithContentsOfFile(metaPath, NSUTF8StringEncoding, null)

        // If no metadata exists, cache is stale
        if (metaText == null || metaText.isEmpty()) return@withContext true

        // Get current app version stamp (from MapStore.ios.kt)
        val currentStamp =
            com.worldwidewaves.shared.data
                .platformAppVersionStamp()

        // Compare version stamps - cache is stale if they don't match
        metaText != currentStamp
    }

actual suspend fun updateCacheMetadata(fileName: String) {
    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
        val metaPath = joinPath(cacheRoot(), "$fileName.metadata")
        val parent = NSString.create(string = metaPath).stringByDeletingLastPathComponent
        NSFileManager.defaultManager.createDirectoryAtPath(
            path = parent,
            withIntermediateDirectories = true,
            attributes = null,
            error = null,
        )
        // Write version stamp instead of timestamp for consistency with MapStore
        val versionStamp =
            com.worldwidewaves.shared.data
                .platformAppVersionStamp()
        NSString
            .create(string = versionStamp)
            .writeToFile(metaPath, true, NSUTF8StringEncoding, null)
    }
}

/**
 * Delete all cached artefacts (data + metadata files) that belong to a given map/event.
 * iOS implementation matching Android behavior.
 *
 * @return true if at least one file was deleted, false otherwise
 */
fun clearEventCache(eventId: String): Boolean {
    val root = getAppSupportMapsDirectory()
    val fileManager = NSFileManager.defaultManager
    var deletedAny = false

    // List of files to delete (matching Android implementation)
    val targets =
        listOf(
            "$eventId.mbtiles",
            "$eventId.mbtiles.metadata",
            "$eventId.geojson",
            "$eventId.geojson.metadata",
            "style-$eventId.json",
            "style-$eventId.json.metadata",
        )

    targets.forEach { fileName ->
        val fullPath = "$root/$fileName"
        if (fileManager.fileExistsAtPath(fullPath)) {
            val success = fileManager.removeItemAtPath(fullPath, error = null)
            if (success) {
                deletedAny = true
                com.worldwidewaves.shared.utils.Log.v(
                    "PlatformCache",
                    "Deleted $fileName for $eventId",
                )
            } else {
                com.worldwidewaves.shared.utils.Log.w(
                    "PlatformCache",
                    "Failed to delete $fileName for $eventId",
                )
            }
        }
    }

    return deletedAny
}

/**
 * Cache all sprite and glyph files from resources.
 *
 * This function is extracted from WWWEventMap and made reusable for SpriteCache.
 * It reads 775 files from the style listing and caches them in parallel.
 */
@OptIn(ExperimentalResourceApi::class)
actual suspend fun cacheSpriteAndGlyphs(): String =
    kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
        try {
            val files =
                Res
                    .readBytes(com.worldwidewaves.shared.WWWGlobals.FileSystem.STYLE_LISTING)
                    .decodeToString()
                    .lines()
                    .filter { it.isNotBlank() }

            // Use parallel processing for file caching
            coroutineScope {
                files
                    .map { file ->
                        async(kotlinx.coroutines.Dispatchers.Default) {
                            cacheDeepFile("${com.worldwidewaves.shared.WWWGlobals.FileSystem.STYLE_FOLDER}/$file")
                        }
                    }.forEach { it.await() }
            }

            getCacheDir()
        } catch (e: Exception) {
            com.worldwidewaves.shared.utils.Log
                .e("cacheSpriteAndGlyphs", "Error caching sprite and glyphs", throwable = e)
            throw e
        }
    }

/**
 * Clear all cached sprite and glyph files.
 */
actual suspend fun clearSpriteCache() {
    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
        try {
            val cacheDir = cacheRoot()
            val styleDir = "$cacheDir/files/style"
            val fileManager = NSFileManager.defaultManager

            if (fileManager.fileExistsAtPath(styleDir)) {
                val success = fileManager.removeItemAtPath(styleDir, error = null)
                if (success) {
                    com.worldwidewaves.shared.utils.Log
                        .i("clearSpriteCache", "Cleared sprite cache: $styleDir")
                } else {
                    com.worldwidewaves.shared.utils.Log
                        .w("clearSpriteCache", "Failed to delete sprite cache directory")
                }
            }
        } catch (e: Exception) {
            com.worldwidewaves.shared.utils.Log
                .e("clearSpriteCache", "Error clearing sprite cache", throwable = e)
            throw e
        }
    }
}

/**
 * Count the number of cached sprite/glyph files.
 */
actual suspend fun countCachedSpriteFiles(): Int {
    return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
        try {
            val cacheDir = cacheRoot()
            val styleDir = "$cacheDir/files/style"
            val fileManager = NSFileManager.defaultManager

            if (!fileManager.fileExistsAtPath(styleDir)) return@withContext 0

            // Get all items recursively
            val url = platform.Foundation.NSURL.fileURLWithPath(styleDir)
            val resourceKeys = listOf(platform.Foundation.NSURLIsRegularFileKey)
            val enumerator =
                fileManager.enumeratorAtURL(
                    url,
                    includingPropertiesForKeys = resourceKeys,
                    options = 0u,
                    errorHandler = null,
                )

            var count = 0
            while (true) {
                val fileUrl = enumerator?.nextObject() as? platform.Foundation.NSURL ?: break

                // Check if it's a regular file (not directory)
                val values = fileUrl.resourceValuesForKeys(resourceKeys, error = null)
                val isRegularFile = (values?.get(platform.Foundation.NSURLIsRegularFileKey) as? platform.Foundation.NSNumber)?.boolValue

                if (isRegularFile == true) {
                    count++
                }
            }

            count
        } catch (e: Exception) {
            com.worldwidewaves.shared.utils.Log
                .e("countCachedSpriteFiles", "Error counting cached sprite files", throwable = e)
            0
        }
    }
}

/**
 * Get available disk space in bytes.
 */
actual suspend fun getAvailableSpace(): Long =
    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
        try {
            val fileManager = NSFileManager.defaultManager
            val cachePath = cacheRoot()
            val attributes = fileManager.attributesOfFileSystemForPath(cachePath, error = null)

            val availableSpace = attributes?.get(platform.Foundation.NSFileSystemFreeSize) as? platform.Foundation.NSNumber
            availableSpace?.longValue ?: 0L
        } catch (e: Exception) {
            com.worldwidewaves.shared.utils.Log
                .e("getAvailableSpace", "Error getting available space", throwable = e)
            0L
        }
    }

/**
 * Create a platform-specific SpriteCachePreferences instance.
 */
actual fun createSpriteCachePreferences(): SpriteCachePreferences = SpriteCachePreferences()

/**
 * Get current time in milliseconds.
 */
actual fun currentTimeMillis(): Long = (platform.Foundation.NSDate().timeIntervalSince1970() * 1000).toLong()
