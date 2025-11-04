package com.worldwidewaves.shared.map

/*
 * Copyright 2025 DrWave
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * https://www.apache.org/licenses/LICENSE-2.0
 */

import com.worldwidewaves.shared.data.isMapFileInCache
import com.worldwidewaves.shared.domain.usecases.MapAvailabilityChecker
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
import platform.Foundation.NSCocoaErrorDomain
import platform.Foundation.NSError
import platform.Foundation.NSURLErrorDomain

/**
 * Tiny, production-ready iOS map manager using On-Demand Resources (ODR).
 *
 * Responsibilities:
 * - Check if a tagged resource is currently available in the bundle.
 * - Trigger an ODR request for a given tag (mapId) and emit progress to 100.
 * - Cancel an ODR request gracefully.
 * - Synchronize UI state with IosMapAvailabilityChecker after download completion.
 *
 * State Synchronization:
 * - When a map download completes successfully, this manager notifies the
 *   IosMapAvailabilityChecker to refresh its state, ensuring the UI (EventsListScreen)
 *   immediately reflects the downloaded status without requiring an app restart.
 *
 * Notes:
 * - We *simulate* progress ticks until the ODR completion arrives, then jump to 100.
 * - We treat completion as success only if at least one expected file exists in the bundle.
 * - We endAccessingResources() on cancel. On success/failure we also end access so callers
 *   should mount again when they actually read (see PlatformIOS functions).
 */
class IosPlatformMapManager(
    private val scope: CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default),
    private val callbackDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val mapAvailabilityChecker: MapAvailabilityChecker? = null,
) : PlatformMapManager {
    private val requests = mutableMapOf<String, NSBundleResourceRequest>()
    private val progressJobs = mutableMapOf<String, Job>()
    private val mutex = Mutex()

    /**
     * Returns true if map files are available (either in cache or bundle).
     *
     * Maps are stored in cache after ODR download/copy:
     * Library/Application Support/Maps/{eventId}.geojson
     * Library/Application Support/Maps/{eventId}.mbtiles
     *
     * This matches how MapStore.readGeoJson() actually accesses the files.
     */
    @OptIn(ExperimentalForeignApi::class)
    @Suppress("ReturnCount") // Early returns for guard clauses improve readability
    override fun isMapAvailable(mapId: String): Boolean {
        Log.d(TAG, "Checking map availability for: $mapId")

        // Check cache first (where MapStore stores files after ODR copy)
        val hasGeo = isMapFileInCache(mapId, "geojson")
        val hasMb = isMapFileInCache(mapId, "mbtiles")

        if (hasGeo || hasMb) {
            Log.i(TAG, "Map available in cache: $mapId, hasGeo=$hasGeo, hasMb=$hasMb")
            return true
        }

        // Fallback: Try bundle (for ODR resources not yet copied)
        val bundleGeo = resolveFromStandardPaths(NSBundle.mainBundle, mapId, "geojson")
        val bundleMb = resolveFromStandardPaths(NSBundle.mainBundle, mapId, "mbtiles")

        if (bundleGeo != null || bundleMb != null) {
            Log.i(TAG, "Map available via bundle: $mapId, geo=$bundleGeo, mb=$bundleMb")
            return true
        }

        Log.i(TAG, "Map NOT available: $mapId (not in cache or bundle)")
        return false
    }

    /**
     * Try standard subdirectory paths (same logic as MapStore.resolveFromStandardPaths)
     */
    @OptIn(ExperimentalForeignApi::class)
    @Suppress("ReturnCount") // Early returns for guard clauses improve readability
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
            try {
                req.beginAccessingResourcesWithCompletionHandler { nsError ->
                    scope.launch(callbackDispatcher) {
                        // Always finish at 100 for UX determinism (even on failure)
                        onProgress(100)

                        if (nsError != null) {
                            val (errorCode, errorMsg) = categorizeError(nsError)
                            Log.e(
                                TAG,
                                "ODR request failed for $mapId: " +
                                    "nsCode=${nsError.code}, nsDomain=${nsError.domain}, " +
                                    "categorized=$errorCode, message=$errorMsg",
                            )
                        }

                        val isAvailable = isMapAvailable(mapId)
                        val ok = nsError == null && isAvailable
                        Log.i(TAG, "ODR completion: mapId=$mapId, error=$nsError, isAvailable=$isAvailable, ok=$ok")

                        if (ok) {
                            Log.i(TAG, "✅ Map download SUCCESS for: $mapId")
                            // Allow future downloads/caching for this event
                            scope.launch {
                                com.worldwidewaves.shared.data.MapDownloadGate
                                    .allow(mapId)
                                Log.d(TAG, "MapDownloadGate.allow called for: $mapId")

                                // Eagerly copy files to cache to ensure availability check succeeds
                                // This prevents race condition where refreshAvailability() is called
                                // before files are persisted to cache
                                Log.d(TAG, "Eagerly copying map files to cache for: $mapId")
                                val geojsonPath =
                                    com.worldwidewaves.shared.data.getMapFileAbsolutePath(
                                        mapId,
                                        com.worldwidewaves.shared.data.MapFileExtension.GEOJSON,
                                    )
                                val mbtilesPath =
                                    com.worldwidewaves.shared.data.getMapFileAbsolutePath(
                                        mapId,
                                        com.worldwidewaves.shared.data.MapFileExtension.MBTILES,
                                    )
                                Log.i(TAG, "Files cached: geojson=${geojsonPath != null}, mbtiles=${mbtilesPath != null}")

                                // Verify at least one file was successfully cached before declaring success
                                // Both files are attempted, but at minimum one must succeed for map to be usable
                                if (geojsonPath == null && mbtilesPath == null) {
                                    Log.e(TAG, "❌ Map file caching FAILED for: $mapId (both geojson and mbtiles failed)")
                                    com.worldwidewaves.shared.data.MapDownloadGate
                                        .disallow(mapId)
                                    onError(-2, "Failed to cache map files to Application Support directory")
                                    return@launch
                                }

                                // Synchronize UI state with availability checker
                                // Now that files are in cache, availability check will succeed
                                mapAvailabilityChecker?.trackMaps(listOf(mapId))
                                mapAvailabilityChecker?.refreshAvailability()
                                Log.d(TAG, "IosMapAvailabilityChecker state synced for: $mapId")

                                onSuccess()
                            }
                        } else {
                            val (errorCode, errorMsg) = categorizeError(nsError)
                            Log.e(TAG, "❌ Map download FAILED for: $mapId (code=$errorCode, message=$errorMsg)")
                            onError(errorCode, errorMsg)
                        }

                        // Cleanup: stop ticker + release access (we mount again when reading)
                        Log.d(TAG, "Cleaning up ODR request for: $mapId")
                        cancelProgressTicker(mapId)
                        scope.launch { endRequest(mapId) }
                    }
                }
            } catch (e: Exception) {
                // Handle synchronous exceptions thrown by beginAccessingResourcesWithCompletionHandler
                // This prevents app crashes when ODR throws NSException (e.g., invalid tag, duplicate request)
                Log.e(
                    TAG,
                    "❌ Exception during ODR request for $mapId: ${e.message}",
                    throwable = e,
                )
                onProgress(100)
                onError(ERROR_INVALID_REQUEST, "ODR request failed: ${e.message}")
                cancelProgressTicker(mapId)
                scope.launch { endRequest(mapId) }
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
     * Categorize NSError into user-friendly error codes.
     * Maps iOS-specific errors to consistent codes that IosMapViewModel can localize.
     */
    @OptIn(ExperimentalForeignApi::class)
    private fun categorizeError(nsError: NSError?): Pair<Int, String> {
        if (nsError == null) {
            return ERROR_UNKNOWN to "Unknown error (null NSError)"
        }

        val code = nsError.code.toInt()
        val domain = nsError.domain
        val description = nsError.localizedDescription ?: "No description"

        Log.d(TAG, "Categorizing NSError: domain=$domain, code=$code, description=$description")

        return when {
            // Insufficient storage (NSBundleOnDemandResourceOutOfSpaceError = 4992)
            domain == NSCocoaErrorDomain && code == 4992 -> {
                Log.w(TAG, "Storage error detected: $description")
                ERROR_INSUFFICIENT_STORAGE to description
            }

            // Network errors (NSURLErrorDomain)
            domain == NSURLErrorDomain -> {
                Log.w(TAG, "Network error detected: code=$code, $description")
                ERROR_NETWORK to description
            }

            // Service connection failed (4097)
            domain == NSCocoaErrorDomain && code == 4097 -> {
                Log.w(TAG, "Service connection error: $description")
                ERROR_SERVICE_DIED to description
            }

            // Tag not found / resource not in bundle (3584)
            domain == NSCocoaErrorDomain && code == 3584 -> {
                Log.w(TAG, "Resource not found: $description")
                ERROR_MODULE_UNAVAILABLE to description
            }

            // Other Cocoa errors
            domain == NSCocoaErrorDomain -> {
                Log.w(TAG, "Cocoa error: code=$code, $description")
                ERROR_UNKNOWN to description
            }

            // Unknown error domain
            else -> {
                Log.w(TAG, "Unknown error domain: $domain, code=$code, $description")
                ERROR_UNKNOWN to description
            }
        }
    }

    companion object {
        private const val TAG = "IosPlatformMapManager"
        private const val PROGRESS_TICK_DELAY_MS = 1000L
        private const val PROGRESS_INCREMENT = 10
        private const val PROGRESS_MAX_BEFORE_COMPLETION = 90

        // Error codes matching IosMapViewModel mapping
        private const val ERROR_INSUFFICIENT_STORAGE = -100
        private const val ERROR_NETWORK = -101
        private const val ERROR_SERVICE_DIED = -102
        private const val ERROR_MODULE_UNAVAILABLE = -103
        private const val ERROR_INVALID_REQUEST = -104
        private const val ERROR_UNKNOWN = -1
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
