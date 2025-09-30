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

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import platform.Foundation.NSBundle
import platform.Foundation.NSBundleResourceRequest

/**
 * Minimal iOS shim: ODR is mounted ad-hoc by platform code; this class only
 * fulfils the MapAvailabilityChecker interface so other modules compile.
 */
class IOSMapAvailabilityChecker : MapAvailabilityChecker {
    private val tracked = mutableSetOf<String>()

    private val _mapStates = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    override val mapStates: StateFlow<Map<String, Boolean>> = _mapStates
    private val probeRequests = mutableSetOf<NSBundleResourceRequest>()

    override fun trackMaps(mapIds: Collection<String>) {
        if (mapIds.isEmpty()) return
        tracked += mapIds
        // best-effort state (true only if file is already in bundle as an initial tag)
        val updated = _mapStates.value.toMutableMap()
        for (id in mapIds) updated[id] = isMapDownloaded(id)
        _mapStates.value = updated
    }

    override fun refreshAvailability() {
        if (tracked.isEmpty()) return
        val updated = mutableMapOf<String, Boolean>()
        for (id in tracked) updated[id] = isMapDownloaded(id)
        _mapStates.value = updated
    }

    override fun requestMapDownload(eventId: String) {
        // no-op — downloads/mounting are performed ad-hoc by platform code (NSBundleResourceRequest)
    }

    // Keep strong refs until callbacks fire

    @OptIn(ExperimentalForeignApi::class)
    override fun isMapDownloaded(eventId: String): Boolean {
        val b = NSBundle.mainBundle
        val subs: Array<String?> = arrayOf("worldwidewaves/Maps/$eventId", "Maps/$eventId", null)
        val visible = subs.any { sub ->
            b.pathForResource(eventId, "geojson", sub) != null ||
                    b.pathForResource(eventId, "mbtiles", sub) != null
        }
        if (visible) {
            if (_mapStates.value[eventId] != true) {
                val m = _mapStates.value.toMutableMap()
                m[eventId] = true
                _mapStates.value = m
            }
            return true
        }

        // Not visible → probe without downloading
        conditionallyProbe(eventId)
        return false
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun conditionallyProbe(tag: String) {
        val req = NSBundleResourceRequest(setOf(tag))
        probeRequests.add(req) // strong ref for lifetime of the async probe

        req.conditionallyBeginAccessingResourcesWithCompletionHandler { available ->
            // End access immediately; this was only a probe
            if (available) try { req.endAccessingResources() } catch (_: Throwable) {}

            val b = NSBundle.mainBundle
            val subs: Array<String?> = arrayOf("worldwidewaves/Maps/$tag", "Maps/$tag", null)
            val visible = subs.any { sub ->
                b.pathForResource(tag, "geojson", sub) != null ||
                        b.pathForResource(tag, "mbtiles", sub) != null
            }

            val m = _mapStates.value.toMutableMap()
            m[tag] = available && visible
            _mapStates.value = m

            probeRequests.remove(req)
        }
    }

    override fun getDownloadedMaps(): List<String> = tracked.filter { isMapDownloaded(it) }
}
