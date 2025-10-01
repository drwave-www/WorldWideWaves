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

import com.worldwidewaves.shared.utils.Log
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSBundle
import platform.Foundation.NSFileManager
import platform.Foundation.NSLog
import platform.Foundation.NSOperationQueue
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

private fun onMain(block: () -> Unit) {
    NSOperationQueue.mainQueue.addOperationWithBlock(block)
}

object ODRPaths {
    /** True if Bundle currently exposes either <id>.geojson or <id>.mbtiles without mounting. */
    fun bundleHas(eventId: String): Boolean = resolve(eventId, "geojson") != null || resolve(eventId, "mbtiles") != null

    /** Resolve absolute path for an ODR resource, being defensive about layout. */
    fun resolve(
        eventId: String,
        extension: String,
    ): String? {
        Log.d("ODRPaths", "resolve: eventId=$eventId, extension=$extension")
        val b = NSBundle.mainBundle

        // Try standard subdirectories first
        resolveFromStandardPaths(b, eventId, extension)?.let {
            Log.i("ODRPaths", "resolve: Found via standard paths -> $it")
            return it
        }

        // Fallback: search all resources with the extension
        val result = resolveFromExtensionSearch(b, eventId, extension)
        if (result != null) {
            Log.i("ODRPaths", "resolve: Found via extension search -> $result")
        } else {
            Log.w("ODRPaths", "resolve: NOT FOUND for $eventId.$extension")
        }
        return result
    }

    private fun resolveFromStandardPaths(
        bundle: NSBundle,
        eventId: String,
        extension: String,
    ): String? {
        Log.d("ODRPaths", "resolveFromStandardPaths: $eventId.$extension")
        val subs = arrayOf("Maps/$eventId", "worldwidewaves/Maps/$eventId", null)
        return subs.firstNotNullOfOrNull { sub ->
            val path = bundle.pathForResource(eventId, extension, sub)
            if (path != null) {
                Log.d("ODRPaths", "  Found via pathForResource with sub=$sub -> $path")
                return@firstNotNullOfOrNull path
            }
            if (sub == null) {
                val url = bundle.URLForResource(eventId, extension)
                if (url?.path != null) {
                    Log.d("ODRPaths", "  Found via URLForResource -> ${url.path}")
                    return@firstNotNullOfOrNull url.path
                }
            }
            null
        }
    }

    private fun resolveFromExtensionSearch(
        bundle: NSBundle,
        eventId: String,
        extension: String,
    ): String? {
        Log.d("ODRPaths", "resolveFromExtensionSearch: $eventId.$extension")
        val any = bundle.URLsForResourcesWithExtension(extension, null)
        val urls = any?.mapNotNull { it as? NSURL } ?: emptyList()
        Log.d("ODRPaths", "  Found ${urls.size} resources with extension .$extension")

        val result =
            urls
                .firstOrNull { url ->
                    val p = url.path ?: ""
                    val matches = p.endsWith("/$eventId.$extension") || p.contains("/Maps/$eventId/")
                    if (matches) {
                        Log.d("ODRPaths", "  Matched: $p")
                    }
                    matches
                }?.path

        if (result == null && urls.isNotEmpty()) {
            Log.d("ODRPaths", "  Available .$extension files (first 5):")
            urls.take(5).forEach { url ->
                Log.d("ODRPaths", "    ${url.path}")
            }
        }
        return result
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
    val request = platform.Foundation.NSBundleResourceRequest(setOf(eventId)).apply { loadingPriority = 1.0 }
    try {
        return mountAndCopyResource(request, eventId, extension, destAbsolutePath)
    } finally {
        onMain { request.endAccessingResources() }
    }
}

private suspend fun mountAndCopyResource(
    request: platform.Foundation.NSBundleResourceRequest,
    eventId: String,
    extension: String,
    destAbsolutePath: String,
): Boolean {
    val mounted = mountODRResource(request)
    if (!mounted) return false

    return copyResourceToDestination(eventId, extension, destAbsolutePath)
}

private suspend fun mountODRResource(request: platform.Foundation.NSBundleResourceRequest): Boolean =
    suspendCancellableCoroutine { cont ->
        onMain {
            request.beginAccessingResourcesWithCompletionHandler { error ->
                cont.resume(error == null)
            }
        }
        cont.invokeOnCancellation { onMain { request.endAccessingResources() } }
    }

private fun copyResourceToDestination(
    eventId: String,
    extension: String,
    destAbsolutePath: String,
): Boolean {
    val src = ODRPaths.resolve(eventId, extension) ?: return false
    val fm = NSFileManager.defaultManager
    if (fm.fileExistsAtPath(destAbsolutePath)) fm.removeItemAtPath(destAbsolutePath, null)
    return fm.copyItemAtPath(src, destAbsolutePath, null)
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
