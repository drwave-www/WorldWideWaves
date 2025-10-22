package com.worldwidewaves.shared.data

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

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

/**
 * Shared iOS file system utilities to avoid code duplication.
 */

/**
 * Get the Maps directory in Application Support.
 * Returns: Library/Application Support/Maps/
 *
 * This is where map files (geojson, mbtiles) are cached after ODR download.
 */
@OptIn(ExperimentalForeignApi::class)
internal fun getAppSupportMapsDirectory(): String {
    val fm = NSFileManager.defaultManager
    val baseUrl =
        fm.URLForDirectory(
            directory = NSApplicationSupportDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = true,
            error = null,
        )
    val path = baseUrl?.path ?: error("Cannot access Application Support directory")
    return "$path/Maps"
}

/**
 * Check if a map file exists in the cache.
 *
 * @param mapId Event/city ID (e.g., "paris_france")
 * @param extension File extension ("geojson" or "mbtiles")
 * @return true if file exists in cache
 */
@OptIn(ExperimentalForeignApi::class)
internal fun isMapFileInCache(
    mapId: String,
    extension: String,
): Boolean {
    val cacheDir = getAppSupportMapsDirectory()
    val filePath = "$cacheDir/$mapId.$extension"
    return NSFileManager.defaultManager.fileExistsAtPath(filePath)
}
