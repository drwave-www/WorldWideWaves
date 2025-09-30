package com.worldwidewaves.shared

/*
 * Copyright 2025 DrWave
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * https://www.apache.org/licenses/LICENSE-2.0
 */

import com.worldwidewaves.shared.events.utils.GeoJsonDataProvider
import com.worldwidewaves.shared.utils.Log
import com.worldwidewaves.shared.utils.initNapier
import com.worldwidewaves.shared.utils.setupDebugSimulation
import dev.icerock.moko.resources.StringResource
import dev.icerock.moko.resources.desc.desc
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.koin.core.logger.PrintLogger
import org.koin.mp.KoinPlatform
import platform.Foundation.NSBundle
import platform.Foundation.NSBundleResourceRequest
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
import kotlin.coroutines.resume

private const val TAG = "Helper"

/**
 * Initialise Koin (unchanged pattern; keep as used in your app).
 */
@Throws(Throwable::class)
fun doInitPlatform() {
    if (koinApp != null) return
    Log.v(TAG, "HELPER: doInitKoin()")

    try {
        initNapier()
    } catch (_: Throwable) {
    }

    try {
        koinApp =
            startKoin {
                logger(PrintLogger(if (BuildKonfig.DEBUG) Level.INFO else Level.ERROR))
                modules(com.worldwidewaves.shared.di.sharedModule + com.worldwidewaves.shared.di.IOSModule)
            }
        if (BuildKonfig.DEBUG) {
            try {
                setupDebugSimulation()
            } catch (_: Throwable) {
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "startKoin failed: ${e.message}")
    }
}

private var koinApp: KoinApplication? = null

// -------------------------------------------------------------------------------------------------
// Direct, self-contained ODR helpers used by the app when it needs to *read* a file now.
// They mount the tag, read, then release access. Safe + simple.
// -------------------------------------------------------------------------------------------------

@OptIn(ExperimentalForeignApi::class)
actual suspend fun readGeoJson(eventId: String): String? {
    val path = getMapFileAbsolutePath(eventId, "geojson") ?: return null
    return NSString.stringWithContentsOfFile(path, NSUTF8StringEncoding, null)?.toString()
}

@OptIn(ExperimentalForeignApi::class)
actual suspend fun getMapFileAbsolutePath(
    eventId: String,
    extension: String,
): String? {
    val tag = eventId
    val req = NSBundleResourceRequest(setOf(tag)).apply { loadingPriority = 1.0 }

    return try {
        val mounted =
            suspendCancellableCoroutine { cont ->
                req.beginAccessingResourcesWithCompletionHandler { error ->
                    if (error != null) {
                        Log.e(
                            "IOSResources",
                            "ODR mount failed for '$tag': code=${error.code}, domain=${error.domain}, desc=${error.localizedDescription}",
                        )
                    }
                    cont.resume(error == null)
                }
                cont.invokeOnCancellation { req.endAccessingResources() }
            }
        if (!mounted) return null

        // 1) Expected location: Maps/<eventId>/<eventId>.<ext>
        val inDir = "Maps/$eventId"
        NSBundle.mainBundle.pathForResource(eventId, extension, inDir)?.let { path ->
            Log.v("IOSResources", "[$eventId] found in '$inDir': $path")
            return path
        }

        // 2) Sometimes Xcode flattens paths; try direct resource
        NSBundle.mainBundle.URLForResource(eventId, extension)?.path?.let { path ->
            Log.v("IOSResources", "[$eventId] found at bundle root: $path")
            return path
        }

        // 3) Last-resort: scan all matches of the extension and pick the one that looks right
        val matches = NSBundle.mainBundle.URLsForResourcesWithExtension(extension, null)
        val chosen =
            matches
                ?.mapNotNull { it as? NSURL }
                ?.firstOrNull { url ->
                    val last = url.lastPathComponent ?: ""
                    last == "$eventId.$extension" || (url.path ?: "").contains("/Maps/$eventId/")
                }?.path

        if (chosen == null) {
            val count = matches?.size ?: 0
            Log.d("IOSResources", "[$eventId] no path; bundle has $count *.$extension files, none matched '$eventId'")
        } else {
            Log.v("IOSResources", "[$eventId] found via scan: $chosen")
        }
        chosen
    } finally {
        req.endAccessingResources()
    }
}

actual suspend fun cachedFileExists(fileName: String): Boolean =
    if (fileName.startsWith("style-")) {
        val path = "${getCacheDir()}/$fileName"
        NSFileManager.defaultManager.fileExistsAtPath(path)
    } else {
        val eventId = fileName.substringBeforeLast('.')
        val ext = fileName.substringAfterLast('.')
        val sub = "Maps/$eventId"
        NSBundle.mainBundle.pathForResource(eventId, ext, sub) != null
    }

actual suspend fun cachedFilePath(fileName: String): String? =
    if (fileName.startsWith("style-")) {
        val path = "${getCacheDir()}/$fileName"
        if (NSFileManager.defaultManager.fileExistsAtPath(path)) "file://$path" else null
    } else {
        val eventId = fileName.substringBeforeLast('.')
        val ext = fileName.substringAfterLast('.')
        val sub = "Maps/$eventId"
        NSBundle.mainBundle.pathForResource(eventId, ext, sub)?.let { "file://$it" }
    }

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
@Throws(Throwable::class)
actual fun cacheStringToFile(
    fileName: String,
    content: String,
): String {
    val filePath = "${getCacheDir()}/$fileName"
    NSString.create(string = content).writeToFile(filePath, true, NSUTF8StringEncoding, null)
    return fileName
}

actual fun getCacheDir(): String = NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, true).first() as String

@Throws(Throwable::class)
actual suspend fun cacheDeepFile(fileName: String) {
    // No-op on iOS; ODR packs are read straight from bundle after mounting.
}

actual fun clearEventCache(eventId: String) {
    try {
        val koin = KoinPlatform.getKoin()
        val geo = koin.get<GeoJsonDataProvider>()
        geo.invalidateCache(eventId)
    } catch (_: Throwable) {
        // ignore
    }
}

actual fun isCachedFileStale(fileName: String): Boolean = false

actual fun updateCacheMetadata(fileName: String) {} // no-op

actual fun localizeString(resource: StringResource): String = resource.desc().localized()
