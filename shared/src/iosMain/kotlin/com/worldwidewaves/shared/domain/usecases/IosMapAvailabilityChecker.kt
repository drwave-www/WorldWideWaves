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

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import platform.Foundation.NSBundleResourceRequest
import platform.Foundation.NSOperationQueue

/**
 * iOS ODR availability checker.
 * No downloads occur unless requestMapDownload(...) is called,
 * or assets are present as Initial Install Tags.
 */
class IosMapAvailabilityChecker : MapAvailabilityChecker {
    private val tracked = mutableSetOf<String>()
    private val _mapStates = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    override val mapStates: StateFlow<Map<String, Boolean>> = _mapStates

    // Prevent GC and allow pinning for explicitly requested maps
    private val pinnedRequests = mutableMapOf<String, NSBundleResourceRequest>() // long-lived, explicit downloads

    private val initialTags: Set<String> by lazy {
        val obj =
            platform.Foundation.NSBundle.mainBundle
                .objectForInfoDictionaryKey("NSOnDemandResourcesInitialInstallTags")
        (obj as? List<*>)?.mapNotNull { it as? String }?.toSet() ?: emptySet()
    }

    private fun inPersistentCache(eventId: String): Boolean {
        val root =
            com.worldwidewaves.shared.data
                .platformCacheRoot()
        val fm = platform.Foundation.NSFileManager.defaultManager
        return fm.fileExistsAtPath("$root/$eventId.geojson") ||
            fm.fileExistsAtPath("$root/$eventId.mbtiles")
    }

    private fun onMain(block: () -> Unit) {
        NSOperationQueue.mainQueue.addOperationWithBlock(block)
    }

    override fun trackMaps(mapIds: Collection<String>) {
        if (mapIds.isEmpty()) return
        tracked += mapIds

        val updated = _mapStates.value.toMutableMap()
        for (id in mapIds) updated[id] = isMapDownloaded(id)
        _mapStates.value = updated

        // Auto-mount ONLY initial tags, on main
        val toAuto =
            mapIds.filter { id ->
                id in initialTags && !inPersistentCache(id) && !pinnedRequests.containsKey(id)
            }
        toAuto.forEach { id -> onMain { requestMapDownload(id) } }
    }

    override fun refreshAvailability() {
        if (tracked.isEmpty()) return
        val updated = mutableMapOf<String, Boolean>()
        for (id in tracked) updated[id] = isMapDownloaded(id) // non-downloading checks
        _mapStates.value = updated
    }

    override fun requestMapDownload(eventId: String) {
        onMain {
            if (pinnedRequests.containsKey(eventId)) return@onMain
            val req = NSBundleResourceRequest(setOf(eventId)).apply { loadingPriority = 1.0 }
            pinnedRequests[eventId] = req
            req.beginAccessingResourcesWithCompletionHandler { error ->
                onMain {
                    val ok = (error == null)
                    if (ok) {
                        com.worldwidewaves.shared.data.MapDownloadGate
                            .allow(eventId)
                    }
                    val m = _mapStates.value.toMutableMap()
                    m[eventId] = ok || inPersistentCache(eventId)
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
        }
    }

    // Call this when you want to allow purge:
    fun releaseDownloadedMap(eventId: String) {
        onMain {
            com.worldwidewaves.shared.data.MapDownloadGate
                .disallow(eventId)
            pinnedRequests.remove(eventId)?.let {
                try {
                    it.endAccessingResources()
                } catch (_: Throwable) {
                }
            }
        }
    }

    // ---------- Non-downloading checks ----------

    override fun isMapDownloaded(eventId: String): Boolean =
        when {
            inPersistentCache(eventId) -> true
            pinnedRequests.containsKey(eventId) -> true
            isInitialTagAvailable(eventId) -> true
            else -> false
        }

    private fun isInitialTagAvailable(eventId: String): Boolean =
        eventId in initialTags &&
            com.worldwidewaves.shared.data.ODRPaths
                .bundleHas(eventId)

    override fun getDownloadedMaps(): List<String> = tracked.filter { isMapDownloaded(it) }
}
