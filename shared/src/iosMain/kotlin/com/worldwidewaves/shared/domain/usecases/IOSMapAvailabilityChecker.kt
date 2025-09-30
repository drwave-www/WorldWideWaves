package com.worldwidewaves.shared.domain.usecases

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
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import platform.Foundation.NSBundle
import platform.Foundation.NSBundleResourceRequest
import platform.Foundation.NSProgress

/**
 * iOS-native implementation of IMapAvailabilityChecker using On-Demand Resources (ODR).
 *
 * This implementation provides equivalent functionality to Android's Dynamic Feature Modules:
 * - Uses ODR (On-Demand Resources) to download map resources on-demand
 * - Tracks resource availability and download status
 * - Maintains proper StateFlow for reactive UI updates
 * - Provides thread-safe tracking operations
 * - Implements proper logging for debugging
 * - Supports resource cleanup and management
 *
 * ODR Setup Required:
 * - Map resources must be tagged in Xcode with corresponding eventIds
 * - Resources should be marked as "On Demand" in project settings
 * - Bundle tags in Info.plist must match event IDs
 *
 * This is a production-grade implementation suitable for iOS deployment.
 */
class IOSMapAvailabilityChecker : MapAvailabilityChecker {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _mapStates = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    override val mapStates: StateFlow<Map<String, Boolean>> = _mapStates.asStateFlow()

    private val trackedMaps = mutableSetOf<String>()

    // CRITICAL FIX: Keep strong references to NSBundleResourceRequest to prevent deallocation
    private val requests = mutableMapOf<String, NSBundleResourceRequest>()
    private val activeProgress = mutableMapOf<String, NSProgress>()
    private val mutex = Mutex()

    init {
        Log.i("IOSMapAvailabilityChecker", "🚀 IOSMapAvailabilityChecker INSTANTIATED! This should appear in logs if DI is working.")
    }

    @OptIn(ExperimentalForeignApi::class)
    override fun refreshAvailability() {
        Log.d("IOSMapAvailabilityChecker", "refreshAvailability() called - checking ODR resource availability")

        scope.launch {
            mutex.withLock {
                val updatedStates = mutableMapOf<String, Boolean>()

                for (mapId in trackedMaps) {
                    val isAvailable = checkResourceAvailability(mapId)
                    updatedStates[mapId] = isAvailable
                    Log.v("IOSMapAvailabilityChecker", "ODR availability check: $mapId -> $isAvailable")
                }

                _mapStates.value = updatedStates
                Log.d("IOSMapAvailabilityChecker", "Updated ODR states: ${updatedStates.size} resources checked")
            }
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    override fun isMapDownloaded(eventId: String): Boolean {
        val isAvailable = checkResourceAvailability(eventId)
        Log.v("IOSMapAvailabilityChecker", "isMapDownloaded($eventId) -> $isAvailable (ODR check)")
        return isAvailable
    }

    override fun getDownloadedMaps(): List<String> {
        // Return intersection of tracked maps and those marked as available in state
        val downloadedMaps =
            trackedMaps.filter { mapId ->
                _mapStates.value[mapId] == true
            }
        Log.v("IOSMapAvailabilityChecker", "getDownloadedMaps() -> ${downloadedMaps.size} available ODR resources")
        return downloadedMaps
    }

    /**
     * Explicitly request download of a specific map resource.
     * This triggers ODR download for maps that aren't initial install tags.
     */
    override fun requestMapDownload(eventId: String) {
        if (!trackedMaps.contains(eventId)) {
            Log.w("IOSMapAvailabilityChecker", "Cannot request download for untracked map: $eventId")
            return
        }

        scope.launch {
            mutex.withLock {
                if (!activeProgress.containsKey(eventId) && _mapStates.value[eventId] != true) {
                    Log.i("IOSMapAvailabilityChecker", "Explicitly requesting ODR download for: $eventId")
                    beginAccessingResources(eventId)
                } else {
                    Log.d("IOSMapAvailabilityChecker", "Map $eventId already requesting or available")
                }
            }
        }
    }

    override fun trackMaps(mapIds: Collection<String>) {
        Log.d("IOSMapAvailabilityChecker", "trackMaps() called with ${mapIds.size} ODR map IDs: ${mapIds.joinToString()}")

        // Synchronously update tracked maps and state to ensure tests can verify immediately
        val oldSize = trackedMaps.size
        trackedMaps.addAll(mapIds)
        val newSize = trackedMaps.size

        if (newSize > oldSize) {
            Log.i("IOSMapAvailabilityChecker", "Added ${newSize - oldSize} new ODR maps to tracking. Total tracked: $newSize")

            // All ODR resources (including initial install tags) start as unavailable
            // They become available only after beginAccessingResources completes successfully
            val updatedStates = _mapStates.value.toMutableMap()
            mapIds.forEach { mapId ->
                // All new maps start as unavailable - ODR request is needed first
                Log.d("IOSMapAvailabilityChecker", "Adding $mapId to tracking")
                updatedStates[mapId] = false // Start as unavailable until ODR completes
            }
            _mapStates.value = updatedStates

            // Only automatically request initial install tags (configured via ODR in Xcode project)
            // Regular ODR resources should be requested explicitly via requestMapDownload()
            scope.launch {
                mutex.withLock {
                    // TODO: Read initial install tags from ODR configuration instead of hardcoding
                    // For now, get from iOS bundle Info.plist ON_DEMAND_RESOURCES_INITIAL_INSTALL_TAGS
                    val initialInstallTags = getInitialInstallTags()
                    val newInitialInstallMaps =
                        mapIds.filter { mapId ->
                            // Only auto-request initial install tags
                            mapId in initialInstallTags &&
                                !activeProgress.containsKey(mapId) &&
                                _mapStates.value[mapId] != true
                        }
                    if (newInitialInstallMaps.isNotEmpty()) {
                        Log.d("IOSMapAvailabilityChecker", "Auto-requesting initial install tags: ${newInitialInstallMaps.joinToString()}")
                        newInitialInstallMaps.forEach { mapId ->
                            beginAccessingResources(mapId)
                        }
                    }

                    // Log regular ODR maps that are tracked but not auto-requested
                    val regularODRMaps = mapIds.filter { it !in initialInstallTags }
                    if (regularODRMaps.isNotEmpty()) {
                        Log.d(
                            "IOSMapAvailabilityChecker",
                            "Tracked ${regularODRMaps.size} regular ODR maps (not auto-requested): ${regularODRMaps.joinToString()}",
                        )
                    }
                }
            }
        }
    }

    /**
     * Debug what Bundle actually sees after ODR success
     */
    @OptIn(ExperimentalForeignApi::class)
    private fun debugBundleContents(mapId: String) {
        try {
            val bundle = NSBundle.mainBundle

            // Check what geojson files Bundle can see
            val geojsonUrls = bundle.URLsForResourcesWithExtension("geojson", null)
            Log.d("IOSMapAvailabilityChecker", "[$mapId] Bundle sees ${geojsonUrls?.count()} .geojson files total")

            val mbtileUrls = bundle.URLsForResourcesWithExtension("mbtiles", null)
            Log.d("IOSMapAvailabilityChecker", "[$mapId] Bundle sees ${mbtileUrls?.count()} .mbtiles files total")

            // Try specific lookups
            val geojsonPath = bundle.pathForResource(mapId, "geojson")
            val geojsonURL = bundle.URLForResource(mapId, "geojson")

            Log.d("IOSMapAvailabilityChecker", "[$mapId] pathForResource(geojson): $geojsonPath")
            Log.d("IOSMapAvailabilityChecker", "[$mapId] URLForResource(geojson): $geojsonURL")

            val mbtilesPath = bundle.pathForResource(mapId, "mbtiles")
            val mbtilesURL = bundle.URLForResource(mapId, "mbtiles")

            Log.d("IOSMapAvailabilityChecker", "[$mapId] pathForResource(mbtiles): $mbtilesPath")
            Log.d("IOSMapAvailabilityChecker", "[$mapId] URLForResource(mbtiles): $mbtilesURL")
        } catch (e: Exception) {
            Log.w("IOSMapAvailabilityChecker", "[$mapId] Debug bundle contents failed: ${e.message}")
        }
    }

    /**
     * Get initial install tags from iOS bundle configuration instead of hardcoding.
     * Reads from Info.plist ON_DEMAND_RESOURCES_INITIAL_INSTALL_TAGS.
     */
    @OptIn(ExperimentalForeignApi::class)
    private fun getInitialInstallTags(): List<String> =
        try {
            val bundle = platform.Foundation.NSBundle.mainBundle
            val initialInstallTags = bundle.objectForInfoDictionaryKey("ON_DEMAND_RESOURCES_INITIAL_INSTALL_TAGS") as? String

            if (initialInstallTags != null) {
                val tags = initialInstallTags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                Log.d("IOSMapAvailabilityChecker", "Read initial install tags from bundle: $tags")
                tags
            } else {
                Log.w(
                    "IOSMapAvailabilityChecker",
                    "No ON_DEMAND_RESOURCES_INITIAL_INSTALL_TAGS found in Info.plist, defaulting to empty list",
                )
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("IOSMapAvailabilityChecker", "Error reading initial install tags from bundle: ${e.message}")
            // Fallback: no auto-download
            emptyList()
        }

    @OptIn(ExperimentalForeignApi::class)
    private fun checkResourceAvailability(mapId: String): Boolean =
        try {
            Log.v("IOSMapAvailabilityChecker", "Checking ODR availability for $mapId")

            // Check current state - only return true if ODR has actually succeeded
            val currentState = _mapStates.value[mapId] ?: false
            val isRequesting = activeProgress.containsKey(mapId)

            Log.v("IOSMapAvailabilityChecker", "ODR status for $mapId: requesting=$isRequesting, state=$currentState")

            // Resource is available ONLY if ODR completed successfully (state=true)
            // This applies to ALL ODR resources, including initial install tags
            currentState
        } catch (e: Exception) {
            Log.e("IOSMapAvailabilityChecker", "Error checking ODR availability for $mapId", throwable = e)
            false
        }

    @OptIn(ExperimentalForeignApi::class)
    private fun beginAccessingResources(mapId: String) {
        try {
            Log.d("IOSMapAvailabilityChecker", "Beginning ODR resource access for $mapId")

            // Create NSBundleResourceRequest for the map tag
            // Use setOf for Kotlin/Native compatibility
            val resourceTags = setOf(mapId)
            val request = NSBundleResourceRequest(resourceTags)
            request.loadingPriority = 1.0

            Log.d("IOSMapAvailabilityChecker", "[$mapId] Created NSBundleResourceRequest with tags: $resourceTags")
            Log.d("IOSMapAvailabilityChecker", "[$mapId] Request object: $request")
            Log.d("IOSMapAvailabilityChecker", "[$mapId] Request progress: ${request.progress}")

            // CRITICAL FIX: Cancel any previous request for this tag before creating new one
            requests.remove(mapId)?.let { oldRequest ->
                Log.d("IOSMapAvailabilityChecker", "[$mapId] Ending previous ODR request before starting new one")
                oldRequest.endAccessingResources()
            }

            // CRITICAL FIX: Store strong reference to prevent deallocation
            requests[mapId] = request
            activeProgress[mapId] = request.progress

            // Begin accessing the resource asynchronously
            Log.d("IOSMapAvailabilityChecker", "[$mapId] Calling beginAccessingResourcesWithCompletionHandler...")
            request.beginAccessingResourcesWithCompletionHandler { error ->
                Log.d("IOSMapAvailabilityChecker", "[$mapId] ⚡ ODR COMPLETION CALLBACK INVOKED ⚡")
                Log.d("IOSMapAvailabilityChecker", "[$mapId] Callback error parameter: $error")

                scope.launch {
                    Log.d("IOSMapAvailabilityChecker", "[$mapId] Inside coroutine scope in callback")
                    mutex.withLock {
                        Log.d("IOSMapAvailabilityChecker", "[$mapId] Acquired mutex lock in callback")
                        activeProgress.remove(mapId)

                        if (error != null) {
                            Log.e("IOSMapAvailabilityChecker", "[$mapId] ODR resource access FAILED: ${error.localizedDescription}")
                            Log.e("IOSMapAvailabilityChecker", "[$mapId] Error code: ${error.code}")
                            Log.e("IOSMapAvailabilityChecker", "[$mapId] Error domain: ${error.domain}")
                            Log.e("IOSMapAvailabilityChecker", "[$mapId] Error userInfo: ${error.userInfo}")

                            // Update state to reflect failure
                            val currentStates = _mapStates.value.toMutableMap()
                            currentStates[mapId] = false
                            _mapStates.value = currentStates
                            Log.d("IOSMapAvailabilityChecker", "[$mapId] Updated state to FAILED")
                        } else {
                            Log.i("IOSMapAvailabilityChecker", "[$mapId] ODR resource access SUCCEEDED ✅")

                            // Update state to reflect success
                            val currentStates = _mapStates.value.toMutableMap()
                            currentStates[mapId] = true
                            _mapStates.value = currentStates
                            Log.d("IOSMapAvailabilityChecker", "[$mapId] Updated state to SUCCESS")

                            // Debug: Log what Bundle actually sees after ODR success
                            debugBundleContents(mapId)

                            // Invalidate GeoJSON cache after a delay to prevent immediate reload loops
                            // The delay ensures ODR state is fully stable before allowing retry
                            scope.launch {
                                Log.d("IOSMapAvailabilityChecker", "[$mapId] Starting delayed cache invalidation...")
                                kotlinx.coroutines.delay(100) // Small delay to prevent tight loops
                                try {
                                    val koin =
                                        org.koin.mp.KoinPlatform
                                            .getKoin()
                                    val geoJsonProvider = koin.get<com.worldwidewaves.shared.events.utils.GeoJsonDataProvider>()
                                    geoJsonProvider.invalidateCache(mapId)
                                    Log.i("IOSMapAvailabilityChecker", "[$mapId] Invalidated GeoJSON cache after ODR success (delayed)")
                                } catch (e: Exception) {
                                    Log.w("IOSMapAvailabilityChecker", "[$mapId] Failed to invalidate GeoJSON cache: ${e.message}")
                                }
                            }
                        }
                    }
                }
            }
            Log.d(
                "IOSMapAvailabilityChecker",
                "[$mapId] beginAccessingResourcesWithCompletionHandler call completed, waiting for callback...",
            )

            Log.d("IOSMapAvailabilityChecker", "[$mapId] Stored strong reference. Total requests: ${requests.size}")
        } catch (e: Exception) {
            Log.e("IOSMapAvailabilityChecker", "Exception starting ODR resource access for $mapId", throwable = e)
        }
    }

    /**
     * Release ODR resources for a specific map.
     */
    @OptIn(ExperimentalForeignApi::class)
    fun release(mapId: String) =
        scope.launch {
            mutex.withLock {
                Log.d("IOSMapAvailabilityChecker", "Releasing ODR resources for $mapId")

                // CRITICAL FIX: End accessing on the same request instance we created
                requests.remove(mapId)?.let { request ->
                    try {
                        request.endAccessingResources()
                        Log.v("IOSMapAvailabilityChecker", "Ended ODR resource access for $mapId")
                    } catch (e: Exception) {
                        Log.w("IOSMapAvailabilityChecker", "Error ending ODR resource access for $mapId", throwable = e)
                    }
                }

                activeProgress.remove(mapId)
                trackedMaps.remove(mapId)

                val currentStates = _mapStates.value.toMutableMap()
                currentStates.remove(mapId)
                _mapStates.value = currentStates
            }
        }

    /**
     * Cleanup method to end resource access for all tracked maps.
     * Should be called when the checker is no longer needed.
     */
    @OptIn(ExperimentalForeignApi::class)
    fun cleanup() =
        scope.launch {
            mutex.withLock {
                Log.d("IOSMapAvailabilityChecker", "Cleaning up ODR resources for ${requests.size} active requests")

                // CRITICAL FIX: End accessing on the same request instances we created
                requests.values.forEach { request ->
                    try {
                        request.endAccessingResources()
                        Log.v("IOSMapAvailabilityChecker", "Ended ODR resource access")
                    } catch (e: Exception) {
                        Log.w("IOSMapAvailabilityChecker", "Error ending ODR resource access", throwable = e)
                    }
                }

                requests.clear()
                activeProgress.clear()
                trackedMaps.clear()
                _mapStates.value = emptyMap()
            }
        }
}
