package com.worldwidewaves.utils

/*
 * Copyright 2024 DrWave
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
import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.ConcurrentHashMap

/**
 * A utility class for checking and monitoring the availability of map feature modules.
 * Uses a reactive approach with StateFlow to notify observers of changes.
 * Automatically listens for module installation events.
 */
class MapAvailabilityChecker(val context: Context) {

    private val splitInstallManager: SplitInstallManager = SplitInstallManagerFactory.create(context)

    // A map of mapId to its availability state
    private val _mapStates = MutableStateFlow<Map<String, Boolean>>(emptyMap())

    // Public state flow that can be observed
    val mapStates: StateFlow<Map<String, Boolean>> = _mapStates

    // Cache of queried map IDs to avoid unnecessary checks
    private val queriedMaps = ConcurrentHashMap.newKeySet<String>()

    // Listener for module installation events
    private val installStateListener: SplitInstallStateUpdatedListener

    init {
        // Create and register the installation state listener
        installStateListener = SplitInstallStateUpdatedListener { state ->
            when (state.status()) {
                SplitInstallSessionStatus.INSTALLED -> {
                    Log.d(::MapAvailabilityChecker.name, "Module installation completed")
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

        // First add all queried maps
        for (mapId in queriedMaps) {
            updatedStates[mapId] = installedModules.contains(mapId)
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

    fun uninstallMap(eventId: String) {
        try {
            // Call the API to uninstall the feature module
            val splitInstallManager = SplitInstallManagerFactory.create(context)

            // Directly call deferredUninstall with the module name
            splitInstallManager.deferredUninstall(listOf(eventId))

            Log.i("MapAvailabilityChecker", "Uninstalled map for event: $eventId")
        } catch (e: Exception) {
            Log.e("MapAvailabilityChecker", "Error uninstalling map for event $eventId: ${e.message}")
        }
    }

}