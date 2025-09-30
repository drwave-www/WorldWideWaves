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

private const val TAG = "MapStore.ios"

// ---- helpers ----
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

// ---- platform shims ----
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

// no-op in common; iOS in-memory provider is Kotlin-side; just expose a hook if you use one.
actual fun platformInvalidateGeoJson(eventId: String) { /* nothing to do here */ }

/**
 * Resolve the absolute path of an ODR resource for an event, being defensive
 * about how Xcode may lay out files inside the asset pack.
 *
 * Tries, in order:
 *  1) "Maps/<eventId>/<eventId>.<ext>"
 *  2) bundle root "<eventId>.<ext>"
 *  3) scan all *.<ext> and pick the one that matches name or expected folder
 */
private fun resolveODRResourcePath(
    eventId: String,
    extension: String,
): String? {
    // 1) Expected subdirectory layout
    val subdir = "Maps/$eventId"
    NSBundle.mainBundle.pathForResource(eventId, extension, subdir)?.let { path ->
        com.worldwidewaves.shared.utils.Log
            .v(TAG, "[$eventId] found in '$subdir': $path")
        return path
    }

    // 2) Flattened at bundle root
    NSBundle.mainBundle.URLForResource(eventId, extension)?.path?.let { path ->
        com.worldwidewaves.shared.utils.Log
            .v(TAG, "[$eventId] found at bundle root: $path")
        return path
    }

    // 3) Last-resort scan of all *.<ext>
    val any = NSBundle.mainBundle.URLsForResourcesWithExtension(extension, null)
    val urls: List<NSURL> =
        when (any) {
            is List<*> -> any.mapNotNull { it as? NSURL } // Kotlin/Native bridged collections
            else -> emptyList() // (very rare fallback; bridged in practice)
        }

    val chosen =
        urls
            .firstOrNull { url ->
                val last = url.lastPathComponent ?: ""
                last == "$eventId.$extension" || (url.path ?: "").contains("/Maps/$eventId/")
            }?.path

    if (chosen == null) {
        com.worldwidewaves.shared.utils.Log.d(
            TAG,
            "[$eventId] no path; bundle has ${urls.size} *.$extension files, none matched '$eventId'",
        )
    } else {
        com.worldwidewaves.shared.utils.Log
            .v(TAG, "[$eventId] found via scan: $chosen")
    }
    return chosen
}

/**
 * Mount the ODR tag for [eventId], resolve the resource path robustly, and copy it
 * to [destAbsolutePath]. Returns true on success.
 */
actual suspend fun platformFetchToFile(
    eventId: String,
    extension: String,
    destAbsolutePath: String,
): Boolean {
    val request =
        platform.Foundation.NSBundleResourceRequest(setOf(eventId)).apply {
            loadingPriority = 1.0
        }

    try {
        // Mount ODR tag
        val mounted =
            suspendCancellableCoroutine { cont ->
                request.beginAccessingResourcesWithCompletionHandler { error ->
                    if (error != null) {
                        com.worldwidewaves.shared.utils.Log.e(
                            TAG,
                            "ODR mount failed for '$eventId': code=${error.code}, " +
                                "domain=${error.domain}, desc=${error.localizedDescription}",
                        )
                    }
                    cont.resume(error == null)
                }
                cont.invokeOnCancellation { request.endAccessingResources() }
            }
        if (!mounted) return false

        // Resolve source inside mounted pack using the robust finder
        val src = resolveODRResourcePath(eventId, extension) ?: return false

        // Overwrite if already present, then copy
        val fm = NSFileManager.defaultManager
        if (fm.fileExistsAtPath(destAbsolutePath)) {
            fm.removeItemAtPath(destAbsolutePath, null)
        }
        return fm.copyItemAtPath(src, destAbsolutePath, null)
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
