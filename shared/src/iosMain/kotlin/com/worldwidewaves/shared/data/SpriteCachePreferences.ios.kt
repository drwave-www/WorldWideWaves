/*
 * Copyright (c) 2025 WorldWideWaves
 * All rights reserved.
 */

package com.worldwidewaves.shared.data

import platform.Foundation.NSUserDefaults

/**
 * iOS implementation of sprite cache preferences using NSUserDefaults.
 *
 * Stores cache completion state and version in standard user defaults.
 *
 * **Version Format**: "SHORT_VERSION+BUILD" (e.g., "1.0.0+42")
 */
actual class SpriteCachePreferences {
    private val defaults = NSUserDefaults.standardUserDefaults

    /**
     * Check if sprite cache is marked as complete AND version is valid.
     *
     * @return true if cache complete and app version matches cached version
     */
    actual suspend fun isCacheComplete(): Boolean = defaults.boolForKey(KEY_CACHE_COMPLETE) && isCacheVersionValid()

    /**
     * Check if cached version matches current app version.
     *
     * @return true if versions match (cache still valid)
     */
    actual suspend fun isCacheVersionValid(): Boolean {
        val cachedVersion = getCachedVersion()
        val currentVersion = getCurrentVersion()
        return cachedVersion == currentVersion
    }

    /**
     * Mark sprite cache as complete and save current app version.
     *
     * Should be called after successful cache completion and integrity verification.
     */
    actual suspend fun markCacheComplete() {
        defaults.setBool(true, forKey = KEY_CACHE_COMPLETE)
        defaults.setObject(getCurrentVersion(), forKey = KEY_CACHE_VERSION)
        defaults.setDouble(
            currentTimeMillis().toDouble(),
            forKey = KEY_COMPLETION_TIMESTAMP,
        )
        defaults.synchronize()
    }

    /**
     * Get the cached app version string.
     *
     * @return Cached version (e.g., "1.0.0+42") or null if not set
     */
    actual fun getCachedVersion(): String? = defaults.stringForKey(KEY_CACHE_VERSION)

    /**
     * Clear all cache state.
     *
     * Called when app version changes or cache integrity fails.
     */
    actual fun clearCache() {
        defaults.setBool(false, forKey = KEY_CACHE_COMPLETE)
        defaults.removeObjectForKey(KEY_CACHE_VERSION)
        defaults.removeObjectForKey(KEY_COMPLETION_TIMESTAMP)
        defaults.synchronize()
    }

    /**
     * Get current app version as "SHORT_VERSION+BUILD".
     *
     * Uses platformAppVersionStamp() which reads from NSBundle.
     *
     * @return Version string (e.g., "1.0.0+42")
     */
    private suspend fun getCurrentVersion(): String = platformAppVersionStamp()

    companion object {
        private const val KEY_CACHE_COMPLETE = "sprite_cache_complete"
        private const val KEY_CACHE_VERSION = "sprite_cache_version"
        private const val KEY_COMPLETION_TIMESTAMP = "sprite_cache_completion_timestamp"
    }
}
