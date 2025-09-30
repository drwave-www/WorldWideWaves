package com.worldwidewaves.shared.domain.usecases

/* * Copyright 2025 DrWave
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
 * limitations under the License. */

import com.worldwidewaves.shared.utils.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import platform.Foundation.NSBundleResourceRequest
import platform.darwin.DISPATCH_TIME_NOW
import platform.darwin.NSEC_PER_MSEC
import platform.darwin.dispatch_semaphore_create
import platform.darwin.dispatch_semaphore_signal
import platform.darwin.dispatch_semaphore_wait
import platform.darwin.dispatch_time

/**
 * iOS ODR availability checker.
 * No downloads occur unless requestMapDownload(...) is called,
 * or assets are present as Initial Install Tags.
 */
class IOSMapAvailabilityChecker : MapAvailabilityChecker {
    private val tag = "IOSMapAvailabilityChecker"
    private val tracked = mutableSetOf<String>()
    private val _mapStates = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    override val mapStates: StateFlow<Map<String, Boolean>> = _mapStates

    // Prevent GC and allow pinning for explicitly requested maps
    private val probeRequests = mutableSetOf<NSBundleResourceRequest>() // short-lived, conditional probes
    private val pinnedRequests = mutableMapOf<String, NSBundleResourceRequest>() // long-lived, explicit downloads

    override fun trackMaps(mapIds: Collection<String>) {
        if (mapIds.isEmpty()) return
        tracked += mapIds
        val updated = _mapStates.value.toMutableMap()
        for (id in mapIds) updated[id] = isMapDownloaded(id) // non-downloading checks
        _mapStates.value = updated
    }

    override fun refreshAvailability() {
        if (tracked.isEmpty()) return
        val updated = mutableMapOf<String, Boolean>()
        for (id in tracked) updated[id] = isMapDownloaded(id) // non-downloading checks
        _mapStates.value = updated
    }

    /** Explicit download+cache. Pins the pack until you call requestMapDownload again with force=false or implement a release. */
    override fun requestMapDownload(eventId: String) {
        if (pinnedRequests.containsKey(eventId)) return
        val req = NSBundleResourceRequest(setOf(eventId)).apply { loadingPriority = 1.0 }
        pinnedRequests[eventId] = req
        req.beginAccessingResourcesWithCompletionHandler { error ->
            val ok = (error == null)
            val m = _mapStates.value.toMutableMap()
            m[eventId] = ok
            _mapStates.value = m
            if (!ok) {
                try {
                    req.endAccessingResources()
                } catch (_: Throwable) {
                }
                pinnedRequests.remove(eventId)
            }
        }
    }

    // Call this when you want to allow purge:
    fun releaseDownloadedMap(eventId: String) {
        pinnedRequests.remove(eventId)?.let { r ->
            try {
                r.endAccessingResources()
            } catch (_: Throwable) {
            }
        }
    }

    // ---------- Non-downloading checks ----------

    override fun isMapDownloaded(eventId: String): Boolean {
        // 1) Fast path: persistent cache already has a copy → downloaded.
        //    Mirror your MapStore cache rule.
        val cacheRoot =
            com.worldwidewaves.shared.data
                .platformCacheRoot()
        val fm = platform.Foundation.NSFileManager.defaultManager
        val inCache =
            fm.fileExistsAtPath("$cacheRoot/$eventId.geojson") ||
                fm.fileExistsAtPath("$cacheRoot/$eventId.mbtiles")

        if (inCache) {
            val m = _mapStates.value.toMutableMap()
            if (m[eventId] != true) {
                m[eventId] = true
                _mapStates.value = m
            }
            Log.d(tag, "$eventId map download status : true")
            return true
        }

        // 2) If the pack is currently mounted or an initial tag, Bundle may already expose it.
        if (com.worldwidewaves.shared.data.ODRPaths
                .bundleHas(eventId)
        ) {
            val m = _mapStates.value.toMutableMap()
            if (m[eventId] != true) {
                m[eventId] = true
                _mapStates.value = m
            }
            Log.d(tag, "$eventId map download status : true")
            return true
        }

        // 3) ODR “available without download?” probe. Do not mount or download.
        return if (!platform.Foundation.NSThread.isMainThread) {
            val ok = conditionallyIsAvailableSync(eventId, timeoutMs = 100)
            val m = _mapStates.value.toMutableMap()
            m[eventId] = ok
            _mapStates.value = m
            Log.d(tag, "$eventId map download status : $ok")
            ok
        } else {
            conditionallyProbe(eventId)
            Log.d(tag, "$eventId map download status : false")
            false
        }
    }

    // Synchronous, non-downloading availability check. Do NOT call on main thread.
    private fun conditionallyIsAvailableSync(
        tag: String,
        timeoutMs: Long,
    ): Boolean {
        val req = NSBundleResourceRequest(setOf(tag))
        probeRequests.add(req)
        var available = false
        val sem = dispatch_semaphore_create(0)
        req.conditionallyBeginAccessingResourcesWithCompletionHandler { ok ->
            available = ok
            if (ok) {
                try {
                    req.endAccessingResources()
                } catch (_: Throwable) {
                }
            }
            dispatch_semaphore_signal(sem)
        }
        val t =
            if (timeoutMs <= 0) {
                DISPATCH_TIME_NOW
            } else {
                dispatch_time(DISPATCH_TIME_NOW, timeoutMs * NSEC_PER_MSEC.toLong())
            }
        dispatch_semaphore_wait(sem, t)
        probeRequests.remove(req)
        return available
    }

    private fun conditionallyProbe(tag: String) {
        val req = NSBundleResourceRequest(setOf(tag))
        probeRequests.add(req)
        req.conditionallyBeginAccessingResourcesWithCompletionHandler { available ->
            if (available) {
                try {
                    req.endAccessingResources()
                } catch (_: Throwable) {
                }
            }
            val m = _mapStates.value.toMutableMap()
            m[tag] = available
            _mapStates.value = m
            probeRequests.remove(req)
        }
    }

    override fun getDownloadedMaps(): List<String> = tracked.filter { isMapDownloaded(it) }
}
