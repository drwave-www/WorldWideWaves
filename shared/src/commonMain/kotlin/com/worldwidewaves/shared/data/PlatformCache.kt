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

/**
 * Checks if a cached file exists in the platform-specific cache directory.
 *
 * @param fileName The name of the file to check (without path)
 * @return true if the file exists in the cache, false otherwise
 */
expect suspend fun cachedFileExists(fileName: String): Boolean

/**
 * Returns the absolute path to a cached file if it exists.
 *
 * @param fileName The name of the file to locate (without path)
 * @return The absolute path to the file, or null if it doesn't exist
 */
expect suspend fun cachedFilePath(fileName: String): String?

/**
 * Caches a file from a remote source to the local cache directory.
 * Platform-specific implementation handles network fetching and storage.
 *
 * @param fileName The name of the file to cache
 * @throws Exception if the file cannot be fetched or stored
 */
expect suspend fun cacheDeepFile(fileName: String)

/**
 * Returns the platform-specific cache directory path.
 *
 * Platform behavior:
 * - Android: Returns Context.cacheDir.absolutePath
 * - iOS: Returns NSCachesDirectory path
 *
 * @return Absolute path to the cache directory
 */
expect fun getCacheDir(): String

/**
 * Checks if a cached file's metadata indicates it is stale and needs refresh.
 * Staleness is determined by comparing stored app version with current version.
 *
 * @param fileName The name of the file to check (without path)
 * @return true if the file is stale or metadata is missing, false if fresh
 */
expect suspend fun isCachedFileStale(fileName: String): Boolean

/**
 * Updates the cache metadata for a file to mark it as fresh.
 * Writes current app version stamp to the metadata file.
 *
 * @param fileName The name of the file whose metadata should be updated
 */
expect suspend fun updateCacheMetadata(fileName: String)

/**
 * Cache all sprite and glyph files from resources.
 *
 * Reads the sprite/glyph file listing (775 files) and caches them in parallel.
 * This is the core operation that SpriteCache wraps for background execution.
 *
 * @return Cache directory path where files were stored
 * @throws Exception if caching fails
 */
expect suspend fun cacheSpriteAndGlyphs(): String

/**
 * Clear all cached sprite and glyph files.
 *
 * Deletes the style folder from cache directory.
 */
expect suspend fun clearSpriteCache()

/**
 * Count the number of cached sprite/glyph files.
 *
 * @return Number of files in the style cache directory
 */
expect suspend fun countCachedSpriteFiles(): Int

/**
 * Get available disk space in bytes.
 *
 * @return Available space in cache directory (in bytes)
 */
expect suspend fun getAvailableSpace(): Long

/**
 * Create a platform-specific SpriteCachePreferences instance.
 *
 * Factory function for creating the actual platform implementation of SpriteCachePreferences.
 * This is needed because expect classes cannot be instantiated directly in Koin.
 *
 * @return Platform-specific SpriteCachePreferences instance
 */
expect fun createSpriteCachePreferences(): SpriteCachePreferences

/**
 * Get current time in milliseconds.
 *
 * @return Current time in milliseconds since epoch
 */
expect fun currentTimeMillis(): Long
