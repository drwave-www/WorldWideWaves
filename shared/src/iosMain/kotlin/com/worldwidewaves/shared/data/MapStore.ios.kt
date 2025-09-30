@file:OptIn(ExperimentalForeignApi::class)

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
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSBundle
import platform.Foundation.NSFileManager
import platform.Foundation.NSLog
import platform.Foundation.NSString
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.stringByDeletingLastPathComponent
import platform.Foundation.stringWithContentsOfFile
import platform.Foundation.writeToFile
import kotlin.coroutines.resume

// ---------- ODR lookup helpers (shared) ----------
object ODRPaths {
    /** True if Bundle currently exposes either <id>.geojson or <id>.mbtiles without mounting. */
    fun bundleHas(eventId: String): Boolean = resolve(eventId, "geojson") != null || resolve(eventId, "mbtiles") != null

    /** Resolve absolute path for an ODR resource, being defensive about layout. */
    fun resolve(
        eventId: String,
        extension: String,
    ): String? {
        val b = NSBundle.mainBundle
        val subs = arrayOf("Maps/$eventId", "worldwidewaves/Maps/$eventId", null)
        for (sub in subs) {
            b.pathForResource(eventId, extension, sub)?.let { return it }
            if (sub == null) b.URLForResource(eventId, extension)?.path?.let { return it }
        }
        val any = b.URLsForResourcesWithExtension(extension, null)
        val urls = any?.mapNotNull { it as? NSURL } ?: emptyList()
        return urls
            .firstOrNull { url ->
                val p = url.path ?: ""
                p.endsWith("/$eventId.$extension") ||
                    p.contains("/Maps/$eventId/")
            }?.path
    }
}

// ---------- helpers ----------
@OptIn(ExperimentalForeignApi::class)
private fun appSupportMapsDir(): String {
    val fm = NSFileManager.defaultManager
    val baseUrl =
        fm.URLForDirectory(
            directory = NSApplicationSupportDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = true,
            error = null,
        ) ?: NSURL.fileURLWithPath(NSTemporaryDirectory())

    val mapsUrl = requireNotNull(baseUrl.URLByAppendingPathComponent("Maps"))
    fm.createDirectoryAtURL(mapsUrl, withIntermediateDirectories = true, attributes = null, error = null)
    return mapsUrl.path ?: (NSTemporaryDirectory() + "/Maps")
}

actual fun platformTryCopyInitialTagToCache(
    eventId: String,
    extension: String,
    destAbsolutePath: String,
): Boolean {
    val src = ODRPaths.resolve(eventId, extension) ?: return false // no mount, just visible?
    val fm = NSFileManager.defaultManager
    if (fm.fileExistsAtPath(destAbsolutePath)) fm.removeItemAtPath(destAbsolutePath, null)
    return fm.copyItemAtPath(src, destAbsolutePath, null)
}

// ---------- platform shims ----------
actual fun platformCacheRoot(): String = appSupportMapsDir()

actual fun platformFileExists(path: String): Boolean = NSFileManager.defaultManager.fileExistsAtPath(path)

actual fun platformReadText(path: String): String = (NSString.stringWithContentsOfFile(path, NSUTF8StringEncoding, null) ?: "")

@OptIn(BetaInteropApi::class)
actual fun platformWriteText(
    path: String,
    content: String,
) {
    NSString
        .create(string = content)
        .writeToFile(path, atomically = true, encoding = NSUTF8StringEncoding, error = null)
}

actual fun platformDeleteFile(path: String) {
    NSFileManager.defaultManager.removeItemAtPath(path, null)
}

actual fun platformEnsureDir(path: String) {
    NSFileManager.defaultManager.createDirectoryAtPath(path, true, null, null)
}

actual fun platformAppVersionStamp(): String {
    val short = NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String ?: "0"
    val build = NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleVersion") as? String ?: "0"
    return "$short+$build"
}

actual fun platformInvalidateGeoJson(eventId: String) { /* no-op */ }

/**
 * Mount the ODR tag for [eventId], resolve the resource path, and copy it to [destAbsolutePath].
 * Returns true on success.
 */
actual suspend fun platformFetchToFile(
    eventId: String,
    extension: String,
    destAbsolutePath: String,
): Boolean {
    NSLog("platformFetchToFile: Fetching %@.%@ to %@", eventId, extension, destAbsolutePath)

    val request =
        platform.Foundation.NSBundleResourceRequest(setOf(eventId)).apply {
            loadingPriority = 1.0
        }

    try {
        val mounted =
            suspendCancellableCoroutine { cont ->
                request.beginAccessingResourcesWithCompletionHandler { error ->
                    if (error != null) {
                        NSLog(
                            "platformFetchToFile: ODR mount failed for '%@': code=%ld domain=%@ desc=%@",
                            eventId,
                            error.code,
                            error.domain,
                            error.localizedDescription,
                        )
                    } else {
                        NSLog("platformFetchToFile: ODR mount succeeded for %@", eventId)
                    }
                    cont.resume(error == null)
                }
                cont.invokeOnCancellation { request.endAccessingResources() }
            }
        if (!mounted) {
            NSLog("platformFetchToFile: Mount failed for %@, aborting", eventId)
            return false
        }

        val src = ODRPaths.resolve(eventId, extension)
        if (src == null) {
            NSLog("platformFetchToFile: Could not resolve %@.%@ in bundle", eventId, extension)
            return false
        }

        NSLog("platformFetchToFile: Resolved source path: %@", src)

        val fm = NSFileManager.defaultManager
        if (fm.fileExistsAtPath(destAbsolutePath)) {
            fm.removeItemAtPath(destAbsolutePath, null)
        }

        val success = fm.copyItemAtPath(src, destAbsolutePath, null)
        if (success) {
            NSLog("platformFetchToFile: Successfully copied %@.%@ to cache", eventId, extension)
        } else {
            NSLog("platformFetchToFile: Failed to copy %@.%@ to %@", eventId, extension, destAbsolutePath)
        }
        return success
    } finally {
        request.endAccessingResources()
    }
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual fun cacheStringToFile(
    fileName: String,
    content: String,
): String {
    val root = platformCacheRoot()
    val path = "$root/$fileName"
    val nsPath = NSString.create(string = path)
    val parent = nsPath.stringByDeletingLastPathComponent
    NSFileManager.defaultManager.createDirectoryAtPath(parent, true, null, null)
    NSLog("cacheStringToFile: Caching data to %@", fileName)
    NSString
        .create(string = content)
        .writeToFile(path, atomically = true, encoding = NSUTF8StringEncoding, error = null)
    return fileName
}
