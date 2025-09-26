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
import kotlinx.coroutines.delay
import platform.Foundation.NSBundle

/**
 * iOS implementation of PlatformMapManager using asset bundles.
 *
 * This implementation manages map availability through iOS app bundles rather than
 * dynamic downloads, which is more suitable for iOS App Store guidelines.
 */
class IOSPlatformMapManager : PlatformMapManager {
    companion object {
        private const val MAP_BUNDLE_PREFIX = "map_"
        private const val MAP_BUNDLE_EXTENSION = "geojson"
    }

    /**
     * Check if a map is available in the iOS app bundle.
     */
    override fun isMapAvailable(mapId: String): Boolean {
        val resourcePath = "${MAP_BUNDLE_PREFIX}$mapId"
        val bundle = NSBundle.mainBundle

        val isAvailable = bundle.pathForResource(resourcePath, MAP_BUNDLE_EXTENSION) != null

        WWWLogger.d("IOSPlatformMapManager", "Map availability check: $mapId -> $isAvailable")
        return isAvailable
    }

    /**
     * "Download" map on iOS (actually just verify bundle availability).
     *
     * Since iOS uses app bundles, there's no actual download - we simulate
     * the download process for UI consistency across platforms.
     */
    override suspend fun downloadMap(
        mapId: String,
        onProgress: (Int) -> Unit,
        onSuccess: () -> Unit,
        onError: (Int, String?) -> Unit,
    ) {
        WWWLogger.i("IOSPlatformMapManager", "Starting map 'download' (bundle verification): $mapId")

        try {
            // Simulate download progress for UI consistency
            for (progress in 0..100 step 20) {
                onProgress(progress)
                delay(100) // Simulate download time
            }

            // Check if map is actually available in bundle
            if (isMapAvailable(mapId)) {
                onSuccess()
                WWWLogger.i("IOSPlatformMapManager", "Map bundle verified successfully: $mapId")
            } else {
                onError(-1, "Map bundle not found in iOS app bundle")
                WWWLogger.e("IOSPlatformMapManager", "Map bundle not found: $mapId")
            }
        } catch (e: Exception) {
            onError(-2, e.message ?: "Unknown error during map bundle verification")
            WWWLogger.e("IOSPlatformMapManager", "Error during map bundle verification: $mapId", e)
        }
    }

    /**
     * Cancel download on iOS (no-op since we use app bundles).
     */
    override fun cancelDownload(mapId: String) {
        WWWLogger.d("IOSPlatformMapManager", "Cancel download requested for: $mapId (no-op on iOS)")
        // No-op on iOS since maps are bundled with the app
    }
}
