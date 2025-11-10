/*
 * Copyright (c) 2025 WorldWideWaves
 * All rights reserved.
 */

package com.worldwidewaves.shared.data

/**
 * Platform-specific storage for sprite cache state.
 *
 * This interface abstracts persistent storage (SharedPreferences on Android,
 * NSUserDefaults on iOS) for tracking sprite cache completion and versioning.
 *
 * **Purpose**: Ensure sprite cache persists across app restarts and invalidates
 * when app version changes.
 */
@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect class SpriteCachePreferences {
    /**
     * Check if sprite cache is marked as complete AND version is valid.
     *
     * @return true if cache complete and app version matches cached version
     */
    suspend fun isCacheComplete(): Boolean

    /**
     * Check if cached version matches current app version.
     *
     * @return true if versions match (cache still valid)
     */
    suspend fun isCacheVersionValid(): Boolean

    /**
     * Mark sprite cache as complete and save current app version.
     *
     * Should be called after successful cache completion and integrity verification.
     */
    suspend fun markCacheComplete()

    /**
     * Get the cached app version string.
     *
     * @return Cached version (e.g., "1.0.0+42") or null if not set
     */
    fun getCachedVersion(): String?

    /**
     * Clear all cache state.
     *
     * Called when app version changes or cache integrity fails.
     */
    fun clearCache()
}
