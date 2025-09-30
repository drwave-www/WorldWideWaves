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
import platform.Foundation.NSBundle

/**
 * Minimal iOS shim: ODR is mounted ad-hoc by platform code; this class only
 * fulfils the MapAvailabilityChecker interface so other modules compile.
 */
class IOSMapAvailabilityChecker : MapAvailabilityChecker {
    private val tracked = mutableSetOf<String>()

    private val _mapStates = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    override val mapStates: StateFlow<Map<String, Boolean>> = _mapStates

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

    override fun isMapDownloaded(eventId: String): Boolean {
        // True only if resource is physically present in the app bundle
        val sub = "Maps/$eventId"
        val hasGeo = NSBundle.mainBundle.pathForResource(eventId, "geojson", sub) != null
        val hasMbtiles = NSBundle.mainBundle.pathForResource(eventId, "mbtiles", sub) != null
        return hasGeo || hasMbtiles
    }

    override fun getDownloadedMaps(): List<String> = tracked.filter { isMapDownloaded(it) }
}
