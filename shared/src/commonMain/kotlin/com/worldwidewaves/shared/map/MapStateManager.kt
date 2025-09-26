package com.worldwidewaves.shared.map

/*
 * Copyright 2025 DrWave
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

import com.worldwidewaves.shared.utils.WWWLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Cross-platform map state management.
 *
 * This shared component manages map availability, download states, and installation
 * progress in a platform-agnostic way. Platform-specific implementations handle
 * the actual download mechanisms.
 */

/**
 * Represents possible states during map feature installation.
 * Shared across Android (Google Play Feature Delivery) and iOS (Asset bundles).
 */
sealed class MapFeatureState {
    object NotChecked : MapFeatureState()
    object Available : MapFeatureState()
    object NotAvailable : MapFeatureState()
    object Pending : MapFeatureState()

    data class Downloading(val progress: Int) : MapFeatureState()
    object Installing : MapFeatureState()
    object Installed : MapFeatureState()

    data class Failed(
        val errorCode: Int,
        val errorMessage: String? = null,
    ) : MapFeatureState()

    object Canceling : MapFeatureState()
    object Unknown : MapFeatureState()

    data class Retrying(
        val attempt: Int,
        val maxAttempts: Int,
    ) : MapFeatureState()
}

/**
 * Shared map state manager that coordinates map availability across platforms.
 */
class MapStateManager(
    private val platformMapManager: PlatformMapManager
) {
    private val _featureState = MutableStateFlow<MapFeatureState>(MapFeatureState.NotChecked)
    val featureState: StateFlow<MapFeatureState> = _featureState

    private val _mapStates = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val mapStates: StateFlow<Map<String, Boolean>> = _mapStates

    private var currentMapId: String? = null

    /**
     * Check if a specific map is available on the current platform.
     */
    suspend fun checkMapAvailability(mapId: String, autoDownload: Boolean = false) {
        WWWLogger.d("MapStateManager", "Checking availability for map: $mapId")
        currentMapId = mapId

        val isAvailable = platformMapManager.isMapAvailable(mapId)

        if (isAvailable) {
            _featureState.value = MapFeatureState.Available
            updateMapState(mapId, true)
        } else {
            _featureState.value = MapFeatureState.NotAvailable
            updateMapState(mapId, false)

            if (autoDownload) {
                downloadMap(mapId)
            }
        }
    }

    /**
     * Download/install a map using platform-specific implementation.
     */
    suspend fun downloadMap(mapId: String) {
        WWWLogger.i("MapStateManager", "Starting download for map: $mapId")
        currentMapId = mapId
        _featureState.value = MapFeatureState.Pending

        try {
            platformMapManager.downloadMap(
                mapId = mapId,
                onProgress = { progress ->
                    _featureState.value = MapFeatureState.Downloading(progress)
                },
                onSuccess = {
                    _featureState.value = MapFeatureState.Installed
                    updateMapState(mapId, true)
                    WWWLogger.i("MapStateManager", "Map download completed: $mapId")
                },
                onError = { errorCode, errorMessage ->
                    _featureState.value = MapFeatureState.Failed(errorCode, errorMessage)
                    WWWLogger.e("MapStateManager", "Map download failed: $mapId, error: $errorMessage")
                }
            )
        } catch (e: Exception) {
            _featureState.value = MapFeatureState.Failed(-1, e.message)
            WWWLogger.e("MapStateManager", "Exception during map download: $mapId", e)
        }
    }

    /**
     * Cancel ongoing map download.
     */
    fun cancelDownload() {
        currentMapId?.let { mapId ->
            WWWLogger.i("MapStateManager", "Canceling download for: $mapId")
            _featureState.value = MapFeatureState.Canceling
            platformMapManager.cancelDownload(mapId)
        }
    }

    /**
     * Update the availability state for a specific map.
     */
    private fun updateMapState(mapId: String, isAvailable: Boolean) {
        val currentStates = _mapStates.value.toMutableMap()
        currentStates[mapId] = isAvailable
        _mapStates.value = currentStates
    }

    /**
     * Refresh availability for all previously queried maps.
     */
    fun refreshAvailability() {
        val currentStates = _mapStates.value
        currentStates.keys.forEach { mapId ->
            val isAvailable = platformMapManager.isMapAvailable(mapId)
            updateMapState(mapId, isAvailable)
        }
    }
}

/**
 * Platform-specific map management interface.
 * Implemented differently on Android (Google Play Feature Delivery) and iOS (Asset bundles).
 */
interface PlatformMapManager {
    fun isMapAvailable(mapId: String): Boolean

    suspend fun downloadMap(
        mapId: String,
        onProgress: (Int) -> Unit,
        onSuccess: () -> Unit,
        onError: (Int, String?) -> Unit
    )

    fun cancelDownload(mapId: String)
}