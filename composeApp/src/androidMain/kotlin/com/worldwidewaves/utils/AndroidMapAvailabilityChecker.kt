package com.worldwidewaves.utils

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

import android.content.Context
import android.util.Log
import com.google.android.play.core.splitinstall.SplitInstallManager
import com.google.android.play.core.splitinstall.SplitInstallManagerFactory
import com.google.android.play.core.splitinstall.SplitInstallStateUpdatedListener
import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus
import com.worldwidewaves.shared.data.clearEventCache
import com.worldwidewaves.shared.data.clearUnavailableGeoJsonCache
import com.worldwidewaves.shared.domain.usecases.MapAvailabilityChecker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume

/**
 * A utility class for checking and monitoring the availability of map feature modules.
 * Uses a reactive approach with StateFlow to notify observers of changes.
 * Automatically listens for module installation events.
 */
class AndroidMapAvailabilityChecker(
    val context: Context,
) : MapAvailabilityChecker {
    /** Log tag used throughout this helper for easy filtering. */
    private companion object Companion {
        private const val TAG = "WWW.Utils.MapAvail"
        private const val PREFS_NAME = "map_availability"
        private const val PREFS_KEY_FORCED_UNAVAILABLE = "forced_unavailable"
    }

    private val splitInstallManager: SplitInstallManager = SplitInstallManagerFactory.create(context)

    // A map of mapId to its availability state
    private val _mapStates = MutableStateFlow<Map<String, Boolean>>(emptyMap())

    // Public state flow that can be observed
    override val mapStates: StateFlow<Map<String, Boolean>> = _mapStates

    // Cache of queried map IDs to avoid unnecessary checks
    private val queriedMaps = ConcurrentHashMap.newKeySet<String>()

    /**
     * Modules for which we *pretend* unavailability even though Play-Core might
     * keep them physically present until the next app update.  Populated when
     * the user requests an uninstall and cleared automatically when Play
     * notifies a fresh install of the same split.
     *
     * Persisted to SharedPreferences to survive app restarts.
     */
    private val forcedUnavailable: MutableSet<String> by lazy {
        java.util.Collections.synchronizedSet(loadForcedUnavailable())
    }

    // Listener for module installation events
    private val installStateListener: SplitInstallStateUpdatedListener

    init {
        // Create and register the installation state listener
        installStateListener =
            SplitInstallStateUpdatedListener { state ->
                when (state.status()) {
                    SplitInstallSessionStatus.INSTALLED -> {
                        Log.d(TAG, "Module installation completed")
                        Log.d(
                            TAG,
                            "session=${state.sessionId()} INSTALLED modules=${state.moduleNames()}",
                        )
                        // If this module had previously been "forced" unavailable
                        // (deferred uninstall), drop the override so it becomes
                        // visible again without requiring an app restart.
                        var forcedUnavailableModified = false
                        state.moduleNames().forEach { id ->
                            val wasInForcedUnavailable = forcedUnavailable.remove(id)
                            if (wasInForcedUnavailable) {
                                forcedUnavailableModified = true
                                Log.i(TAG, "Removed $id from forcedUnavailable (re-downloaded)")
                            }
                            // Clear the session cache for newly installed maps
                            clearUnavailableGeoJsonCache(id)
                        }

                        // Persist the updated forcedUnavailable set
                        if (forcedUnavailableModified) {
                            saveForcedUnavailable()
                        }

                        refreshAvailability()
                    }
                    SplitInstallSessionStatus.FAILED -> {
                        Log.w(TAG, "Module installation failed: ${state.errorCode()}")
                        Log.w(
                            TAG,
                            "session=${state.sessionId()} FAILED code=${state.errorCode()}",
                        )
                        refreshAvailability()
                    }
                    SplitInstallSessionStatus.CANCELED -> {
                        Log.i(TAG, "Module installation canceled")
                        Log.i(TAG, "session=${state.sessionId()} CANCELED")
                        refreshAvailability()
                    }
                    else -> {
                        // Other states aren't relevant for availability changes
                    }
                }
            }

        // Register the listener with the split install manager
        splitInstallManager.registerListener(installStateListener)

        // Initialize with current state
        refreshAvailability()
    }

    /**
     * Clean up resources when this checker is no longer needed.
     * Should be called in onDestroy() if using in an Activity or Service.
     */
    fun destroy() {
        splitInstallManager.unregisterListener(installStateListener)
    }

    /**
     * Loads the forced unavailable set from SharedPreferences.
     * Handles corruption gracefully by returning an empty set on error.
     *
     * @return The set of map IDs that should be treated as unavailable
     */
    private fun loadForcedUnavailable(): MutableSet<String> =
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val stored = prefs.getStringSet(PREFS_KEY_FORCED_UNAVAILABLE, null)

            if (stored == null) {
                Log.d(TAG, "No persisted forcedUnavailable data (first run or old version)")
                mutableSetOf()
            } else {
                Log.i(TAG, "Loaded ${stored.size} forcedUnavailable entries from persistence: $stored")
                stored.toMutableSet()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load forcedUnavailable from persistence, starting fresh", e)
            // Clear corrupted data
            try {
                context
                    .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .remove(PREFS_KEY_FORCED_UNAVAILABLE)
                    .apply()
            } catch (clearError: Exception) {
                Log.e(TAG, "Failed to clear corrupted persistence data", clearError)
            }
            mutableSetOf()
        }

    /**
     * Saves the forced unavailable set to SharedPreferences.
     * Uses apply() for async write to avoid blocking the main thread.
     */
    private fun saveForcedUnavailable() {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            // Create immutable copy for thread-safety
            val snapshot = synchronized(forcedUnavailable) { forcedUnavailable.toSet() }
            prefs
                .edit()
                .putStringSet(PREFS_KEY_FORCED_UNAVAILABLE, snapshot)
                .apply()
            Log.d(TAG, "Saved ${snapshot.size} forcedUnavailable entries to persistence")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save forcedUnavailable to persistence", e)
        }
    }

    /**
     * Refreshes the availability state of all tracked maps.
     * Call this when returning to a list to ensure fresh data.
     */
    override fun refreshAvailability() {
        val installedModules = splitInstallManager.installedModules
        Log.d(TAG, "Refreshing availability. Installed modules: $installedModules")
        Log.d(TAG, "queried=$queriedMaps forcedUnavailable=$forcedUnavailable")

        // Edge case validation: Clear stale forcedUnavailable entries
        // NOTE: Disabled to fix bug where uninstalled maps immediately reappear in downloaded list.
        // The previous logic would clear forcedUnavailable entries if files still existed, but this
        // is wrong because Play Core uses deferred uninstall (files remain until next app update).
        // User intent to uninstall should be preserved even if files temporarily remain.
        // TODO: Re-enable with better logic that only clears on actual fresh install events
        /*
        val staleEntries =
            forcedUnavailable.filter { mapId ->
                // If module is installed AND files are accessible, it shouldn't be in forcedUnavailable
                mapId in installedModules && areMapFilesAccessible(mapId)
            }

        if (staleEntries.isNotEmpty()) {
            Log.i(TAG, "Clearing stale forcedUnavailable entries (manual installs?): $staleEntries")
            forcedUnavailable.removeAll(staleEntries.toSet())
            saveForcedUnavailable()
        }
         */

        // Build updated state map
        val updatedStates = HashMap<String, Boolean>()

        // First add all queried maps – honour any forced-uninstall overrides
        for (mapId in queriedMaps) {
            val moduleInstalled = installedModules.contains(mapId)
            val forcedUnavail = forcedUnavailable.contains(mapId)

            // Module must be installed by PlayCore AND have valid map files
            val isAvailable =
                if (forcedUnavail) {
                    false
                } else if (moduleInstalled) {
                    // Verify that actual map files exist and are accessible
                    areMapFilesAccessible(mapId)
                } else {
                    false
                }

            updatedStates[mapId] = isAvailable

            if (moduleInstalled && !isAvailable && !forcedUnavail) {
                Log.w(TAG, "Module $mapId installed but files not accessible")
            }
        }

        // Update the state flow with the new map
        _mapStates.value = updatedStates
        Log.d(TAG, "Updated availability: $updatedStates")
    }

    /**
     * Track multiple map IDs at once.
     */
    override fun trackMaps(mapIds: Collection<String>) {
        queriedMaps.addAll(mapIds)
        // Don't refresh here - caller should call refreshAvailability() if needed
        Log.d(TAG, "trackMaps added=${mapIds.joinToString()} totalTracked=${queriedMaps.size}")
    }

    override fun isMapDownloaded(eventId: String): Boolean {
        val downloaded = mapStates.value[eventId] == true
        Log.d(TAG, "isMapDownloaded id=$eventId -> $downloaded")
        return downloaded
    }

    /**
     * Checks if the map files (.mbtiles and .geojson) are actually accessible.
     * First checks cache (fast path), then checks if files exist in bundled split assets.
     * This ensures bundled maps from Android Studio builds are recognized as available.
     *
     * @param eventId The event/map ID to check
     * @return true if both .mbtiles and .geojson files are accessible (cache or assets)
     */
    private fun areMapFilesAccessible(eventId: String): Boolean {
        val cacheDir = context.cacheDir
        val mbtilesFile = java.io.File(cacheDir, "$eventId.mbtiles")
        val geojsonFile = java.io.File(cacheDir, "$eventId.geojson")

        val mbtilesInCache = mbtilesFile.exists() && mbtilesFile.canRead() && mbtilesFile.length() > 0
        val geojsonInCache = geojsonFile.exists() && geojsonFile.canRead() && geojsonFile.length() > 0

        // Fast path: both files already in cache
        if (mbtilesInCache && geojsonInCache) {
            return true
        }

        // Check if files are accessible from bundled split assets
        // This handles the case where dynamic feature modules are bundled from Android Studio
        // but files haven't been copied to cache yet
        val mbtilesInAssets = if (!mbtilesInCache) isAssetAccessible(eventId, "mbtiles") else true
        val geojsonInAssets = if (!geojsonInCache) isAssetAccessible(eventId, "geojson") else true

        val result = (mbtilesInCache || mbtilesInAssets) && (geojsonInCache || geojsonInAssets)

        if (!result) {
            Log.d(
                TAG,
                "areMapFilesAccessible id=$eventId " +
                    "mbtiles=(cache=$mbtilesInCache assets=$mbtilesInAssets) " +
                    "geojson=(cache=$geojsonInCache assets=$geojsonInAssets)",
            )
        }

        return result
    }

    /**
     * Checks if a specific asset file is accessible in the split context.
     * Used to verify bundled dynamic feature module assets.
     *
     * @param eventId The event/map ID
     * @param extension The file extension (without dot)
     * @return true if the asset can be opened, false otherwise
     */
    private fun isAssetAccessible(
        eventId: String,
        extension: String,
    ): Boolean {
        val assetName = "$eventId.$extension"
        return try {
            // Try to create split context for this event
            val splitContext =
                if (android.os.Build.VERSION.SDK_INT >= 26) {
                    runCatching {
                        context.createContextForSplit(eventId)
                    }.getOrNull() ?: context
                } else {
                    context
                }

            // Try to open the asset (just check existence, don't read)
            splitContext.assets.open(assetName).use { true }
        } catch (
            @Suppress("SwallowedException") e: Exception,
        ) {
            // Asset doesn't exist - expected for undownloaded maps
            false
        }
    }

    override fun getDownloadedMaps(): List<String> {
        val downloaded =
            mapStates.value
                .filterValues { it }
                .keys
                .toList()
        Log.d(TAG, "getDownloadedMaps -> $downloaded")
        return downloaded
    }

    fun canUninstallMap(eventId: String): Boolean =
        try {
            val splitInstallManager = SplitInstallManagerFactory.create(context)
            splitInstallManager.installedModules.contains(eventId)
        } catch (ise: IllegalStateException) {
            Log.e(TAG, "SplitInstallManager in invalid state: ${ise.message}")
            Log.e(TAG, "canUninstallMap id=$eventId exception=${ise.message}")
            false // If there's an error, assume it can't be uninstalled
        } catch (uoe: UnsupportedOperationException) {
            Log.e(TAG, "Unsupported operation on SplitInstallManager: ${uoe.message}")
            Log.e(TAG, "canUninstallMap id=$eventId exception=${uoe.message}")
            false // If there's an error, assume it can't be uninstalled
        }

    /**
     * Clears the forced unavailable flag for a specific map.
     *
     * CRITICAL FOR RE-DOWNLOAD AFTER UNINSTALL:
     * Called from MapDownloadCoordinator.downloadMap() BEFORE any availability checks.
     * This ensures the download flow sees accurate map availability state.
     *
     * CONTEXT:
     * Play Core uses deferred uninstall - map files remain after uninstall until next app update.
     * To respect user's uninstall intent, we set forcedUnavailable flag which:
     * 1. Persisted to SharedPreferences (survives app restart)
     * 2. Loaded on app init
     * 3. Overrides file-based availability checks
     *
     * When user re-downloads:
     * 1. This method removes from forcedUnavailable set
     * 2. Persists removal to SharedPreferences
     * 3. Calls refreshAvailability() to update reactive mapStates flow
     * 4. Now isMapInstalled() and other checks return accurate state
     *
     * @param eventId The event/map ID to clear
     * @return true if the map was in forcedUnavailable and was removed, false otherwise
     */
    fun clearForcedUnavailable(eventId: String): Boolean {
        val wasPresent = forcedUnavailable.remove(eventId)
        if (wasPresent) {
            Log.i(TAG, "Cleared forcedUnavailable for $eventId (re-download)")
            saveForcedUnavailable()
            // Update reactive state immediately to prevent race conditions
            // Ensures mapStates flow reflects the cleared state without waiting for listener
            refreshAvailability()
        }
        return wasPresent
    }

    /**
     * Checks if a map is in the forcedUnavailable set.
     *
     * Used by UI components to verify user's uninstall intent before allowing map loading.
     * Prevents stale ViewModel state from loading uninstalled maps.
     *
     * @param eventId The event/map ID to check
     * @return true if user explicitly uninstalled this map, false otherwise
     */
    fun isForcedUnavailable(eventId: String): Boolean = forcedUnavailable.contains(eventId)

    /**
     * Request uninstall of the dynamic-feature module corresponding to [eventId].
     * Implements the MapAvailabilityChecker interface method.
     *
     * Returns `true` when the uninstall request was successfully scheduled,
     * `false` otherwise.  On success the internal [_mapStates] flow is updated
     * immediately and any cached artefacts are cleared so UI can reflect the
     * change without polling.
     */
    override suspend fun requestMapUninstall(eventId: String): Boolean = uninstallMap(eventId)

    /**
     * Uninstall the dynamic-feature module corresponding to [eventId].
     *
     * Returns `true` when the uninstall request was successfully scheduled,
     * `false` otherwise.  On success the internal [_mapStates] flow is updated
     * immediately and any cached artefacts are cleared so UI can reflect the
     * change without polling.
     */
    private suspend fun uninstallMap(eventId: String): Boolean =
        suspendCancellableCoroutine { cont ->
            try {
                Log.i(TAG, "uninstallMap requested for $eventId")
                val splitInstallManager = SplitInstallManagerFactory.create(context)

                splitInstallManager
                    .deferredUninstall(listOf(eventId))
                    .addOnSuccessListener {
                        // Mark this module as "virtually" unavailable for the
                        // remainder of the session – Play Core keeps it around
                        // until next update but UI must reflect immediate removal.
                        forcedUnavailable.add(eventId)

                        // Persist the updated forcedUnavailable set
                        saveForcedUnavailable()

                        // Update reactive state immediately
                        _mapStates.value =
                            _mapStates.value.toMutableMap().apply {
                                this[eventId] = false
                            }

                        // Best-effort cache cleanup – do not fail uninstall on errors
                        try {
                            clearEventCache(eventId)
                        } catch (
                            @Suppress("SwallowedException") _: IllegalStateException,
                        ) {
                            // Non-critical: cache cleanup failure doesn't block uninstall
                        }

                        Log.i(TAG, "Uninstall scheduled for map/event: $eventId")
                        Log.i(TAG, "uninstallMap success (scheduled) id=$eventId")
                        if (cont.isActive) cont.resume(true)
                    }.addOnFailureListener { e ->
                        Log.e(
                            TAG,
                            "Deferred uninstall failed for $eventId: ${e.message}",
                        )
                        Log.e(TAG, "uninstallMap failure id=$eventId err=${e.message}")
                        if (cont.isActive) cont.resume(false)
                    }
            } catch (ise: IllegalStateException) {
                Log.e(TAG, "SplitInstallManager in invalid state for $eventId: ${ise.message}")
                Log.e(TAG, "uninstallMap exception id=$eventId err=${ise.message}")
                if (cont.isActive) cont.resume(false)
            } catch (iae: IllegalArgumentException) {
                Log.e(TAG, "Invalid module name for uninstall $eventId: ${iae.message}")
                Log.e(TAG, "uninstallMap exception id=$eventId err=${iae.message}")
                if (cont.isActive) cont.resume(false)
            } catch (uoe: UnsupportedOperationException) {
                Log.e(TAG, "Unsupported operation during uninstall for $eventId: ${uoe.message}")
                Log.e(TAG, "uninstallMap exception id=$eventId err=${uoe.message}")
                if (cont.isActive) cont.resume(false)
            }
        }
}
