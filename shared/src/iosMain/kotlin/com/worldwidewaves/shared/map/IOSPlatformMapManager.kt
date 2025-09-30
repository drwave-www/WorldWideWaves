package com.worldwidewaves.shared.map

/*
 * Copyright 2025 DrWave
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * https://www.apache.org/licenses/LICENSE-2.0
 */

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

    /** Returns true if a typical file for this tag is visible in the bundle right now. */
    @OptIn(ExperimentalForeignApi::class)
    override fun isMapAvailable(mapId: String): Boolean {
        // Consider either .geojson or .mbtiles as proof of availability.
        val sub = "Maps/$mapId"
        val hasGeo = NSBundle.mainBundle.pathForResource(mapId, "geojson", sub) != null
        val hasMb = NSBundle.mainBundle.pathForResource(mapId, "mbtiles", sub) != null
        return hasGeo || hasMb
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
        scope.launch {
            val req =
                mutex.withLock {
                    requests.getOrPut(mapId) {
                        NSBundleResourceRequest(setOf(mapId)).also { it.loadingPriority = 1.0 }
                    }
                }

            // start a progress ticker up to 90 while waiting
            startProgressTicker(mapId, onProgress)

            req.beginAccessingResourcesWithCompletionHandler { nsError ->
                scope.launch(callbackDispatcher) {
                    // Always finish at 100 for UX determinism (even on failure)
                    onProgress(100)

                    val ok = nsError == null && isMapAvailable(mapId)
                    if (ok) {
                        onSuccess()
                    } else {
                        onError(nsError?.code?.toInt() ?: -1, nsError?.localizedDescription ?: "ODR/bundle error")
                    }

                    // Cleanup: stop ticker + release access (we mount again when reading)
                    cancelProgressTicker(mapId)
                    scope.launch { endRequest(mapId) }
                }
            }
        }
    }

    /** Cancel an ongoing download (no-op if none). */
    override fun cancelDownload(mapId: String) {
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
        if (progressJobs[mapId]?.isActive == true) return

        progressJobs[mapId] =
            scope.launch(callbackDispatcher) {
                var p = 0
                onProgress(0)
                while (isActive && p < 90) {
                    delay(1000) // visible but conservative tick
                    p += 10
                    if (p > 90) p = 90
                    onProgress(p)
                }
            }
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
