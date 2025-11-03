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

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import platform.Foundation.NSBundleResourceRequest
import platform.Foundation.NSOperationQueue
import kotlin.coroutines.resume

/**
 * iOS ODR availability checker.
 * No downloads occur unless requestMapDownload(...) is called,
 * or assets are present as Initial Install Tags.
 */
class IosMapAvailabilityChecker : MapAvailabilityChecker {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val mutex = Mutex() // Protect tracked set from concurrent downloads
    private val tracked = mutableSetOf<String>()
    private val _mapStates = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    override val mapStates: StateFlow<Map<String, Boolean>> = _mapStates

    // Prevent GC and allow pinning for explicitly requested maps
    private val pinnedRequests = mutableMapOf<String, NSBundleResourceRequest>() // long-lived, explicit downloads

    private val initialTags: Set<String> by lazy {
        val obj =
            platform.Foundation.NSBundle.mainBundle
                .objectForInfoDictionaryKey("NSOnDemandResourcesInitialInstallTags")
        val tags = (obj as? List<*>)?.mapNotNull { it as? String }?.toSet() ?: emptySet()
        com.worldwidewaves.shared.utils.Log.d(
            "IosMapAvailabilityChecker",
            "Initial ODR tags from Info.plist: ${tags.joinToString(", ")}",
        )
        tags
    }

    private fun inPersistentCache(eventId: String): Boolean {
        val root =
            kotlinx.coroutines.runBlocking {
                com.worldwidewaves.shared.data
                    .platformCacheRoot()
            }
        val fm = platform.Foundation.NSFileManager.defaultManager
        return fm.fileExistsAtPath("$root/$eventId.geojson") ||
            fm.fileExistsAtPath("$root/$eventId.mbtiles")
    }

    private fun onMain(block: () -> Unit) {
        NSOperationQueue.mainQueue.addOperationWithBlock(block)
    }

    override fun trackMaps(mapIds: Collection<String>) {
        if (mapIds.isEmpty()) return

        // Thread-safe synchronous update using runBlocking + mutex
        // StateFlow updates are thread-safe on Kotlin/Native
        val updated = _mapStates.value.toMutableMap()

        runBlocking {
            mutex.withLock {
                tracked += mapIds
                for (id in mapIds) updated[id] = isMapDownloaded(id)
            }
        }

        _mapStates.value = updated
        com.worldwidewaves.shared.utils.Log.d(
            "IosMapAvailabilityChecker",
            "trackMaps: Updated mapStates for ${mapIds.size} map(s), total tracked: ${tracked.size}",
        )

        // Auto-mount ONLY initial tags, on main
        val toAuto =
            mapIds.filter { id ->
                id in initialTags && !inPersistentCache(id) && !pinnedRequests.containsKey(id)
            }
        if (toAuto.isNotEmpty()) {
            com.worldwidewaves.shared.utils.Log.d(
                "IosMapAvailabilityChecker",
                "Auto-mounting ${toAuto.size} map(s) from initial tags: ${toAuto.joinToString(", ")}",
            )
        }
        toAuto.forEach { id -> onMain { requestMapDownload(id) } }
    }

    override fun refreshAvailability() {
        val trackedCopy =
            runBlocking {
                mutex.withLock { tracked.toSet() }
            }
        if (trackedCopy.isEmpty()) return

        val updated = mutableMapOf<String, Boolean>()
        for (id in trackedCopy) updated[id] = isMapDownloaded(id)

        val oldStates = _mapStates.value
        _mapStates.value = updated

        // Log state changes for debugging
        val changedMaps = updated.filter { (id, newState) -> oldStates[id] != newState }
        if (changedMaps.isNotEmpty()) {
            com.worldwidewaves.shared.utils.Log.i(
                "IosMapAvailabilityChecker",
                "refreshAvailability: ${changedMaps.size} map(s) state changed: ${changedMaps.keys.joinToString()}",
            )
        }
    }

    override fun requestMapDownload(eventId: String) {
        com.worldwidewaves.shared.utils.Log.d(
            "IosMapAvailabilityChecker",
            "requestMapDownload called for: $eventId",
        )
        onMain {
            if (pinnedRequests.containsKey(eventId)) {
                com.worldwidewaves.shared.utils.Log.d(
                    "IosMapAvailabilityChecker",
                    "requestMapDownload: $eventId already in pinnedRequests, skipping",
                )
                return@onMain
            }
            com.worldwidewaves.shared.utils.Log.d(
                "IosMapAvailabilityChecker",
                "Creating NSBundleResourceRequest for: $eventId",
            )
            val req = NSBundleResourceRequest(setOf(eventId)).apply { loadingPriority = 1.0 }
            pinnedRequests[eventId] = req
            com.worldwidewaves.shared.utils.Log.d(
                "IosMapAvailabilityChecker",
                "Calling beginAccessingResources for: $eventId",
            )
            req.beginAccessingResourcesWithCompletionHandler { error ->
                onMain {
                    val ok = (error == null)
                    if (ok) {
                        scope.launch {
                            com.worldwidewaves.shared.data.MapDownloadGate
                                .allow(eventId)
                        }
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

    /**
     * Request uninstall/release of a downloaded map.
     * Implements the MapAvailabilityChecker interface method.
     * Releases ODR resources and updates state immediately.
     */
    override suspend fun requestMapUninstall(eventId: String): Boolean =
        kotlinx.coroutines.suspendCancellableCoroutine { cont ->
            var wasRemoved = false

            scope.launch {
                // Background: Clear cache and disallow downloads FIRST
                com.worldwidewaves.shared.data.MapDownloadGate
                    .disallow(eventId)

                // Clear cache files to ensure isMapAvailable returns false
                // Matches Android behavior in AndroidMapAvailabilityChecker
                try {
                    com.worldwidewaves.shared.data
                        .clearEventCache(eventId)
                    com.worldwidewaves.shared.data
                        .clearUnavailableGeoJsonCache(eventId)
                    com.worldwidewaves.shared.utils.Log.i(
                        "IosMapAvailabilityChecker",
                        "Cache cleared for $eventId",
                    )
                } catch (e: Exception) {
                    com.worldwidewaves.shared.utils.Log.e(
                        "IosMapAvailabilityChecker",
                        "Failed to clear cache for $eventId",
                        throwable = e,
                    )
                }

                // Main thread: ODR release + state update (after cache cleared)
                onMain {
                    val request = pinnedRequests.remove(eventId)
                    wasRemoved = request != null
                    request?.let {
                        try {
                            it.endAccessingResources()
                        } catch (_: Throwable) {
                        }
                    }

                    // Update state after cache is cleared (fixes race condition)
                    val updated = _mapStates.value.toMutableMap()
                    updated[eventId] = false
                    _mapStates.value = updated

                    com.worldwidewaves.shared.utils.Log.i(
                        "IosMapAvailabilityChecker",
                        "requestMapUninstall: Released map $eventId, state updated to false",
                    )

                    cont.resume(wasRemoved)
                }
            }
            cont.invokeOnCancellation { }
        }

    /**
     * Call this when you want to allow purge.
     * @deprecated Use requestMapUninstall() instead for proper state management
     */
    @Deprecated(
        "Use requestMapUninstall() instead",
        ReplaceWith("requestMapUninstall(eventId)"),
    )
    fun releaseDownloadedMap(eventId: String) {
        onMain {
            scope.launch {
                com.worldwidewaves.shared.data.MapDownloadGate
                    .disallow(eventId)
            }
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
