package com.worldwidewaves.utils

/*
 * Copyright 2025 DrWave
 *
 * WorldWideWaves is an ephemeral mobile app designed to orchestrate human waves through cities and
 * countries, culminating in a global wave. The project aims to transcend physical and cultural
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
import com.google.android.play.core.splitinstall.SplitInstallException
import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus
import com.google.android.play.core.splitinstall.model.SplitInstallErrorCode
import com.worldwidewaves.shared.clearEventCache
import com.worldwidewaves.shared.data.HiddenMapsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.runBlocking
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * A utility class for checking and monitoring the availability of map feature modules.
 * Uses a reactive approach with StateFlow to notify observers of changes.
 * Automatically listens for module installation events.
 */
class MapAvailabilityChecker(val context: Context) : KoinComponent {

    private val splitInstallManager: SplitInstallManager = SplitInstallManagerFactory.create(context)

    // Persisted store for “hidden” maps (deferred-uninstalled in this session)
    private val hiddenMapsStore: HiddenMapsStore by inject()

    // A map of mapId to its availability state
    private val _mapStates = MutableStateFlow<Map<String, Boolean>>(emptyMap())

    // Public state flow that can be observed
    val mapStates: StateFlow<Map<String, Boolean>> = _mapStates

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
        // Load previously hidden IDs from the DataStore so they persist across restarts
        runBlocking {
            try {
                forcedUnavailable.addAll(hiddenMapsStore.getAll())
            } catch (e: Exception) {
                Log.e(::MapAvailabilityChecker.name, "Failed to load hidden maps set", e)
            }
        }

        // Create and register the installation state listener
        installStateListener = SplitInstallStateUpdatedListener { state ->
            when (state.status()) {
                SplitInstallSessionStatus.INSTALLED -> {
                    Log.d(::MapAvailabilityChecker.name, "Module installation completed")
                    // If this module had previously been “forced” unavailable
                    // (deferred uninstall), drop the override so it becomes
                    // visible again without requiring an app restart.
                    state.moduleNames()?.forEach { id ->
                        forcedUnavailable.remove(id)
                        // Also persist removal from the hidden set
                        runBlocking {
                            try { hiddenMapsStore.remove(id) } catch (_: Exception) { }
                        }
                    }
                    refreshAvailability()
                }
                SplitInstallSessionStatus.FAILED -> {
                    Log.d(::MapAvailabilityChecker.name, "Module installation failed: ${state.errorCode()}")
                    refreshAvailability()
                }
                SplitInstallSessionStatus.CANCELED -> {
                    Log.d(::MapAvailabilityChecker.name, "Module installation canceled")
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
    fun refreshAvailability() {
        val installedModules = splitInstallManager.installedModules
        Log.d(::MapAvailabilityChecker.name, "Refreshing availability. Installed modules: $installedModules")

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
    }

    /**
     * Track multiple map IDs at once.
     */
    fun trackMaps(mapIds: Collection<String>) {
        queriedMaps.addAll(mapIds)
        // Don't refresh here - caller should call refreshAvailability() if needed
    }

    fun isMapDownloaded(eventId: String): Boolean {
        return mapStates.value[eventId] == true
    }

    fun canUninstallMap(eventId: String): Boolean {
        try {
            val splitInstallManager = SplitInstallManagerFactory.create(context)
            return splitInstallManager.installedModules.contains(eventId) // Not installed, so can't uninstall
        } catch (e: Exception) {
            Log.e("MapAvailabilityChecker", "Error checking if map can be uninstalled: ${e.message}")
            return false // If there's an error, assume it can't be uninstalled
        }
    }

    /**
     * Uninstall the dynamic-feature module corresponding to [eventId].
     *
     * Returns `true` when the uninstall request was successfully scheduled,
     * `false` otherwise.  On success the internal [_mapStates] flow is updated
     * immediately and any cached artefacts are cleared so UI can reflect the
     * change without polling.
     */
    suspend fun uninstallMap(eventId: String): Boolean = suspendCancellableCoroutine { cont ->
        try {
            val splitInstallManager = SplitInstallManagerFactory.create(context)

            splitInstallManager
                .deferredUninstall(listOf(eventId))
                .addOnSuccessListener {
                    // Mark this module as “virtually” unavailable for the
                    // remainder of the session – Play Core keeps it around
                    // until next update but UI must reflect immediate removal.
                    forcedUnavailable.add(eventId)
                    // Persist in DataStore so state survives process death
                    runBlocking {
                        try { hiddenMapsStore.add(eventId) } catch (_: Exception) { }
                    }

                    // Update reactive state immediately
                    _mapStates.value = _mapStates.value.toMutableMap().apply {
                        this[eventId] = false
                    }

                    // Best-effort cache cleanup – do not fail uninstall on errors
                    try { clearEventCache(eventId) } catch (_: Exception) { }

                    Log.i("MapAvailabilityChecker", "Uninstall scheduled for map/event: $eventId")
                    if (cont.isActive) cont.resume(true)
                }
                .addOnFailureListener { e ->
                    // Treat some Play-Core errors as soft success so UI can proceed
                    val softSuccess = if (e is SplitInstallException) {
                        when (e.errorCode) {
                            SplitInstallErrorCode.API_NOT_AVAILABLE,
                            SplitInstallErrorCode.MODULE_UNAVAILABLE,
                            SplitInstallErrorCode.SERVICE_DIED -> true
                            else -> false
                        }
                    } else false

                    if (softSuccess) {
                        Log.w(
                            "MapAvailabilityChecker",
                            "Deferred uninstall failed for $eventId with soft-success code ${if (e is SplitInstallException) e.errorCode else "N/A"} – proceeding as success"
                        )
                        // Apply forced override so subsequent refreshes don’t
                        // resurrect the availability flag.
                        forcedUnavailable.add(eventId)
                        runBlocking {
                            try { hiddenMapsStore.add(eventId) } catch (_: Exception) { }
                        }

                        // Update reactive state immediately
                        _mapStates.value = _mapStates.value.toMutableMap().apply {
                            this[eventId] = false
                        }
                        try { clearEventCache(eventId) } catch (_: Exception) { }
                        if (cont.isActive) cont.resume(true)
                    } else {
                        Log.e(
                            "MapAvailabilityChecker",
                            "Deferred uninstall failed for $eventId: ${e.message}"
                        )
                        if (cont.isActive) cont.resume(false)
                    }
                }
        } catch (e: Exception) {
            Log.e("MapAvailabilityChecker", "Error initiating uninstall for $eventId: ${e.message}")
            if (cont.isActive) cont.resume(false)
        }
    }

}