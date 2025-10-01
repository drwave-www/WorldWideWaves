package com.worldwidewaves.shared.map

/*
 * Copyright 2025 DrWave
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * https://www.apache.org/licenses/LICENSE-2.0
 */

import com.worldwidewaves.shared.utils.Log
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import platform.Foundation.NSBundle
import platform.Foundation.NSBundleResourceRequest
import platform.Foundation.NSURL

/**
 * Tiny, production-ready iOS map manager using On-Demand Resources (ODR).
 *
 * Responsibilities:
 * - Check if a tagged resource is currently available in the bundle.
 * - Trigger an ODR request for a given tag (mapId) and emit progress to 100.
 * - Cancel an ODR request gracefully.
 *
 * Notes:
 * - We *simulate* progress ticks until the ODR completion arrives, then jump to 100.
 * - We treat completion as success only if at least one expected file exists in the bundle.
 * - We endAccessingResources() on cancel. On success/failure we also end access so callers
 *   should mount again when they actually read (see PlatformIOS functions).
 */
class IOSPlatformMapManager(
    private val scope: CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default),
    private val callbackDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : PlatformMapManager {
    private val requests = mutableMapOf<String, NSBundleResourceRequest>()
    private val progressJobs = mutableMapOf<String, Job>()
    private val mutex = Mutex()

    /**
     * Returns true if a typical file for this tag is visible in the bundle right now.
     * Uses the SAME approach as MapStore.ODRPaths.resolve() which works reliably.
     */
    @OptIn(ExperimentalForeignApi::class)
    override fun isMapAvailable(mapId: String): Boolean {
        Log.d(TAG, "Checking map availability for: $mapId")

        // Try BOTH approaches like MapStore does:
        // 1. Standard paths (pathForResource + URLForResource)
        // 2. Extension search (URLsForResourcesWithExtension)

        val bundle = NSBundle.mainBundle

        // Approach 1: Try standard subdirectories (same as MapStore)
        val standardGeo = resolveFromStandardPaths(bundle, mapId, "geojson")
        val standardMb = resolveFromStandardPaths(bundle, mapId, "mbtiles")

        if (standardGeo != null || standardMb != null) {
            Log.i(TAG, "Map available via standard paths: $mapId, geo=$standardGeo, mb=$standardMb")
            return true
        }

        // Approach 2: Extension search (fallback)
        val searchGeo = findResourceByExtensionSearch(mapId, "geojson")
        val searchMb = findResourceByExtensionSearch(mapId, "mbtiles")

        val available = searchGeo != null || searchMb != null
        Log.i(TAG, "Map availability: mapId=$mapId, available=$available (search: geo=$searchGeo, mb=$searchMb)")
        return available
    }

    /**
     * Try standard subdirectory paths (same logic as MapStore.resolveFromStandardPaths)
     */
    @OptIn(ExperimentalForeignApi::class)
    private fun resolveFromStandardPaths(
        bundle: NSBundle,
        eventId: String,
        extension: String,
    ): String? {
        // Try different subdirectory patterns
        val subs = listOf("Maps/$eventId", "worldwidewaves/Maps/$eventId", null)

        for (sub in subs) {
            // Try pathForResource with subdirectory
            val path = bundle.pathForResource(eventId, extension, sub)
            if (path != null) {
                Log.d(TAG, "Found via pathForResource: $eventId.$extension in $sub")
                return path
            }

            // Try URLForResource (without subdirectory)
            if (sub == null) {
                val url = bundle.URLForResource(eventId, extension)
                if (url?.path != null) {
                    Log.d(TAG, "Found via URLForResource: $eventId.$extension")
                    return url.path
                }
            }
        }

        return null
    }

    /**
     * Start (or reuse) an ODR request for [mapId]. Emits 0..100. Calls onSuccess/onError at the end.
     * If the tag doesn't exist in any pack, the completion handler returns error and we report failure.
     */
    @OptIn(ExperimentalForeignApi::class)
    override suspend fun downloadMap(
        mapId: String,
        onProgress: (Int) -> Unit,
        onSuccess: () -> Unit,
        onError: (code: Int, message: String?) -> Unit,
    ) {
        Log.i(TAG, "📥 Starting ODR download for mapId: $mapId")
        scope.launch {
            val req =
                mutex.withLock {
                    requests.getOrPut(mapId) {
                        Log.d(TAG, "Creating new NSBundleResourceRequest for tag: $mapId")
                        NSBundleResourceRequest(setOf(mapId)).also { it.loadingPriority = 1.0 }
                    }
                }

            // start a progress ticker up to 90 while waiting
            Log.d(TAG, "Starting progress ticker for: $mapId")
            startProgressTicker(mapId, onProgress)

            Log.d(TAG, "Calling beginAccessingResources for: $mapId")
            req.beginAccessingResourcesWithCompletionHandler { nsError ->
                scope.launch(callbackDispatcher) {
                    // Always finish at 100 for UX determinism (even on failure)
                    onProgress(100)

                    if (nsError != null) {
                        Log.e(TAG, "ODR request failed for $mapId: code=${nsError.code}, message=${nsError.localizedDescription}")
                    }

                    val isAvailable = isMapAvailable(mapId)
                    val ok = nsError == null && isAvailable
                    Log.i(TAG, "ODR completion: mapId=$mapId, error=$nsError, isAvailable=$isAvailable, ok=$ok")

                    if (ok) {
                        Log.i(TAG, "✅ Map download SUCCESS for: $mapId")
                        onSuccess()
                    } else {
                        val errorCode = nsError?.code?.toInt() ?: -1
                        val errorMsg = nsError?.localizedDescription ?: "ODR/bundle error"
                        Log.e(TAG, "❌ Map download FAILED for: $mapId (code=$errorCode, message=$errorMsg)")
                        onError(errorCode, errorMsg)
                    }

                    // Cleanup: stop ticker + release access (we mount again when reading)
                    Log.d(TAG, "Cleaning up ODR request for: $mapId")
                    cancelProgressTicker(mapId)
                    scope.launch { endRequest(mapId) }
                }
            }
        }
    }

    /** Cancel an ongoing download (no-op if none). */
    override fun cancelDownload(mapId: String) {
        Log.i(TAG, "Cancelling download for: $mapId")
        scope.launch {
            cancelProgressTicker(mapId)
            endRequest(mapId)
        }
    }

    // ---- Helpers ------------------------------------------------------------------------------

    private fun startProgressTicker(
        mapId: String,
        onProgress: (Int) -> Unit,
    ) {
        // If a ticker is already running, do nothing.
        if (progressJobs[mapId]?.isActive == true) {
            Log.d(TAG, "Progress ticker already running for: $mapId")
            return
        }

        Log.d(TAG, "Starting progress ticker for: $mapId")
        progressJobs[mapId] =
            scope.launch(callbackDispatcher) {
                var p = 0
                onProgress(0)
                while (isActive && p < PROGRESS_MAX_BEFORE_COMPLETION) {
                    delay(PROGRESS_TICK_DELAY_MS)
                    p += PROGRESS_INCREMENT
                    if (p > PROGRESS_MAX_BEFORE_COMPLETION) p = PROGRESS_MAX_BEFORE_COMPLETION
                    Log.v(TAG, "Progress tick for $mapId: $p%")
                    onProgress(p)
                }
            }
    }

    /**
     * Find resource using global extension search (same as ODRPaths.resolve()).
     * This approach works reliably where pathForResource() fails.
     */
    @OptIn(ExperimentalForeignApi::class)
    private fun findResourceByExtensionSearch(
        eventId: String,
        extension: String,
    ): String? {
        val bundle = NSBundle.mainBundle
        val any = bundle.URLsForResourcesWithExtension(extension, null)
        val urls = any?.mapNotNull { it as? NSURL } ?: emptyList()

        val foundUrl =
            urls.firstOrNull { url ->
                val p = url.path ?: ""
                p.endsWith("/$eventId.$extension") ||
                    p.contains("/Maps/$eventId/")
            }

        if (foundUrl != null) {
            Log.d(TAG, "Found $extension file for $eventId: ${foundUrl.path}")
        } else {
            Log.d(TAG, "No $extension file found for $eventId (searched ${urls.size} total $extension files)")
        }

        return foundUrl?.path
    }

    companion object {
        private const val TAG = "IOSPlatformMapManager"
        private const val PROGRESS_TICK_DELAY_MS = 1000L
        private const val PROGRESS_INCREMENT = 10
        private const val PROGRESS_MAX_BEFORE_COMPLETION = 90
    }

    private suspend fun endRequest(mapId: String) {
        mutex.withLock {
            requests.remove(mapId)?.endAccessingResources()
        }
    }

    private fun cancelProgressTicker(mapId: String) {
        progressJobs.remove(mapId)?.cancel()
    }
}
