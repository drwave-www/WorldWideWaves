package com.worldwidewaves.shared.data

/*
 * Copyright 2025 DrWave
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * https://www.apache.org/licenses/LICENSE-2.0
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
