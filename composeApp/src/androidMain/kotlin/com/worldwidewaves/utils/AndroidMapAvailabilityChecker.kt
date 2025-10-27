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
     */
    private val forcedUnavailable =
        java.util.Collections.synchronizedSet(mutableSetOf<String>())

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
                        state.moduleNames().forEach { id ->
                            forcedUnavailable.remove(id)
                            // Clear the session cache for newly installed maps
                            clearUnavailableGeoJsonCache(id)
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
     * Refreshes the availability state of all tracked maps.
     * Call this when returning to a list to ensure fresh data.
     */
    override fun refreshAvailability() {
        val installedModules = splitInstallManager.installedModules
        Log.d(TAG, "Refreshing availability. Installed modules: $installedModules")
        Log.d(TAG, "queried=$queriedMaps forcedUnavailable=$forcedUnavailable")

        // Build updated state map
        val updatedStates = HashMap<String, Boolean>()

        // First add all queried maps – honour any forced-uninstall overrides
        for (mapId in queriedMaps) {
            val installed = installedModules.contains(mapId)
            updatedStates[mapId] =
                if (forcedUnavailable.contains(mapId)) false else installed
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
     * Uninstall the dynamic-feature module corresponding to [eventId].
     *
     * Returns `true` when the uninstall request was successfully scheduled,
     * `false` otherwise.  On success the internal [_mapStates] flow is updated
     * immediately and any cached artefacts are cleared so UI can reflect the
     * change without polling.
     */
    suspend fun uninstallMap(eventId: String): Boolean =
        suspendCancellableCoroutine { cont ->
            try {
                Log.i(TAG, "uninstallMap requested for $eventId")
                val splitInstallManager = SplitInstallManagerFactory.create(context)

                splitInstallManager
                    .deferredUninstall(listOf(eventId))
                    .addOnSuccessListener {
                        // Mark this module as “virtually” unavailable for the
                        // remainder of the session – Play Core keeps it around
                        // until next update but UI must reflect immediate removal.
                        forcedUnavailable.add(eventId)

                        // Update reactive state immediately
                        _mapStates.value =
                            _mapStates.value.toMutableMap().apply {
                                this[eventId] = false
                            }

                        // Best-effort cache cleanup – do not fail uninstall on errors
                        try {
                            clearEventCache(eventId)
                        } catch (_: IllegalStateException) {
                            // ignore cache cleanup errors
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
