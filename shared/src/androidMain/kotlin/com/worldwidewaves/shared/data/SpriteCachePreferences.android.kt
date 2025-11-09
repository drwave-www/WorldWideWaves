/*
 * Copyright (c) 2025 WorldWideWaves
 * All rights reserved.
 */

package com.worldwidewaves.shared.data

import android.content.Context
import android.content.SharedPreferences
import org.koin.java.KoinJavaComponent.inject

/**
 * Android implementation of sprite cache preferences using SharedPreferences.
 *
 * Stores cache completion state and version in `sprite_cache_prefs` preferences file.
 *
 * **Version Format**: "VERSION_NAME+VERSION_CODE" (e.g., "1.0.0+42")
 */
actual class SpriteCachePreferences {
    private val context: Context by inject(Context::class.java)

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Check if sprite cache is marked as complete AND version is valid.
     *
     * @return true if cache complete and app version matches cached version
     */
    actual suspend fun isCacheComplete(): Boolean = prefs.getBoolean(KEY_CACHE_COMPLETE, false) && isCacheVersionValid()

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
        prefs
            .edit()
            .putBoolean(KEY_CACHE_COMPLETE, true)
            .putString(KEY_CACHE_VERSION, getCurrentVersion())
            .putLong(KEY_COMPLETION_TIMESTAMP, System.currentTimeMillis())
            .apply()
    }

    /**
     * Get the cached app version string.
     *
     * @return Cached version (e.g., "1.0.0+42") or null if not set
     */
    actual fun getCachedVersion(): String? = prefs.getString(KEY_CACHE_VERSION, null)

    /**
     * Clear all cache state.
     *
     * Called when app version changes or cache integrity fails.
     */
    actual fun clearCache() {
        prefs
            .edit()
            .putBoolean(KEY_CACHE_COMPLETE, false)
            .remove(KEY_CACHE_VERSION)
            .remove(KEY_COMPLETION_TIMESTAMP)
            .apply()
    }

    /**
     * Get current app version as "VERSION_NAME+VERSION_CODE".
     *
     * @return Version string (e.g., "1.0.0+42")
     */
    private fun getCurrentVersion(): String =
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            "${packageInfo.versionName}+${packageInfo.versionCode}"
        } catch (e: Exception) {
            // Fallback to unknown version if PackageManager fails
            "unknown+0"
        }

    companion object {
        private const val PREFS_NAME = "sprite_cache_prefs"
        private const val KEY_CACHE_COMPLETE = "cache_complete"
        private const val KEY_CACHE_VERSION = "cache_version"
        private const val KEY_COMPLETION_TIMESTAMP = "completion_timestamp"
    }
}
