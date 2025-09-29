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

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import org.jetbrains.compose.resources.ExperimentalResourceApi
import platform.Foundation.NSBundle
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSData
import platform.Foundation.NSDate
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileModificationDate
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
 * iOS "staleness": compare our metadata timestamp to the bundle's modification time.
 * If no data file or metadata exists, it's stale.
 */
actual fun isCachedFileStale(fileName: String): Boolean {
    val root = cacheRoot()
    val dataPath = joinPath(root, fileName)
    if (!NSFileManager.defaultManager.fileExistsAtPath(dataPath)) return true

    val metaPath = joinPath(root, "$fileName.metadata")
    val metaText = NSString.stringWithContentsOfFile(metaPath, NSUTF8StringEncoding, null)
    val cachedTime = metaText?.toLongOrNull() ?: 0L

    // Use bundle path modification time as a proxy for "app updated"
    val bundlePath = NSBundle.mainBundle.bundlePath
    val attrs = NSFileManager.defaultManager.attributesOfItemAtPath(bundlePath, null)
    val modDate = attrs?.get(NSFileModificationDate) as? NSDate
    val appUpdateMs = ((modDate?.timeIntervalSince1970 ?: 0.0) * 1000.0).toLong()

    return appUpdateMs > cachedTime
}

actual fun updateCacheMetadata(fileName: String) {
    val metaPath = joinPath(cacheRoot(), "$fileName.metadata")
    val parent = NSString.create(string = metaPath).stringByDeletingLastPathComponent
    NSFileManager.defaultManager.createDirectoryAtPath(
        path = parent,
        withIntermediateDirectories = true,
        attributes = null,
        error = null,
    )
    NSString
        .create(string = "${NSDate().timeIntervalSince1970.toLong() * 1000}")
        .writeToFile(metaPath, true, NSUTF8StringEncoding, null)
}
