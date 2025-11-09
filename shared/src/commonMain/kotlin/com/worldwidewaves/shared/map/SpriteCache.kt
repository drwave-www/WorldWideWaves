/*
 * Copyright (c) 2025 WorldWideWaves
 * All rights reserved.
 */

package com.worldwidewaves.shared.map

import com.worldwidewaves.shared.data.SpriteCachePreferences
import com.worldwidewaves.shared.data.cacheSpriteAndGlyphs
import com.worldwidewaves.shared.data.clearSpriteCache
import com.worldwidewaves.shared.data.countCachedSpriteFiles
import com.worldwidewaves.shared.data.currentTimeMillis
import com.worldwidewaves.shared.data.getAvailableSpace
import com.worldwidewaves.shared.utils.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Manages sprite and glyph file caching for MapLibre maps.
 *
 * Sprites and glyphs are shared resources across all maps (775 files, ~6.4MB total).
 * This class ensures they are cached once during app initialization rather than
 * per-map-load, significantly improving map loading performance.
 *
 * **Performance Impact**:
 * - Without pre-cache: 10-20 second delay on first map load
 * - With pre-cache: <1 second on all map loads (after background cache completes)
 *
 * **Thread Safety**: All operations are protected by mutex to prevent concurrent caching.
 *
 * **iOS Safety**: Background caching is started via MainScope().launch{} from Koin,
 * not from init{}, to avoid iOS Kotlin/Native deadlocks.
 */
class SpriteCache(
    private val preferences: SpriteCachePreferences,
    private val scope: CoroutineScope,
) {
    /**
     * Cache state representation.
     */
    sealed class CacheState {
        /**
         * Cache has not been started yet.
         */
        data object NotStarted : CacheState()

        /**
         * Cache operation is in progress.
         * @property progress Current progress (0.0 to 1.0), null if unknown
         */
        data class Loading(
            val progress: Float? = null,
        ) : CacheState()

        /**
         * Cache completed successfully.
         * @property fileCount Number of files cached
         * @property durationMs Time taken to cache in milliseconds
         */
        data class Complete(
            val fileCount: Int,
            val durationMs: Long,
        ) : CacheState()

        /**
         * Cache failed with an error.
         * @property error The exception that caused the failure
         * @property message Human-readable error message
         */
        data class Failed(
            val error: Exception,
            val message: String,
        ) : CacheState()
    }

    private val _cacheState = MutableStateFlow<CacheState>(CacheState.NotStarted)

    /**
     * Observable cache state flow.
     *
     * Emits state changes as cache progresses through lifecycle:
     * NotStarted → Loading → Complete/Failed
     *
     * UI components can collect this flow to show progress indicators.
     */
    val cacheStateFlow: StateFlow<CacheState> = _cacheState.asStateFlow()

    private val cacheMutex = Mutex()
    private var cacheJob: Job? = null

    /**
     * Check if cache is complete and valid.
     *
     * @return true if cache completed successfully and integrity verified
     */
    suspend fun isComplete(): Boolean =
        when (val state = _cacheState.value) {
            is CacheState.Complete -> verifyCacheIntegrity()
            else -> false
        }

    /**
     * Start background sprite/glyph caching.
     *
     * This is a non-blocking operation that starts caching in the background.
     * Call from app initialization (Koin module) to begin pre-caching.
     *
     * **Thread Safety**: Multiple calls are safe - only one cache operation runs at a time.
     *
     * **Behavior**:
     * - If cache already complete: No-op
     * - If cache in progress: No-op (existing job continues)
     * - If cache failed or not started: Starts new cache job
     */
    fun startBackgroundCache() {
        scope.launch {
            cacheMutex.withLock {
                // Skip if already complete
                if (isComplete()) {
                    Log.i(TAG, "Sprite cache already complete, skipping")
                    return@launch
                }

                // Skip if already running
                if (cacheJob?.isActive == true) {
                    Log.i(TAG, "Sprite cache already in progress, skipping")
                    return@launch
                }

                // Check version - invalidate cache if app updated
                if (!preferences.isCacheVersionValid()) {
                    Log.i(TAG, "App version changed, clearing sprite cache")
                    clearSpriteCache()
                    preferences.clearCache()
                }

                // Check disk space
                val availableSpace = getAvailableSpace()
                val requiredSpace = REQUIRED_DISK_SPACE_BYTES
                if (availableSpace < requiredSpace) {
                    val message = "Insufficient disk space: ${availableSpace}B available, ${requiredSpace}B required"
                    Log.w(TAG, message)
                    _cacheState.value =
                        CacheState.Failed(
                            Exception("Insufficient disk space"),
                            message,
                        )
                    return@launch
                }

                // Start cache job
                cacheJob =
                    scope.launch(Dispatchers.Default) {
                        try {
                            Log.i(TAG, "Starting sprite cache (${EXPECTED_FILE_COUNT} files, ~${EXPECTED_SIZE_MB}MB)")
                            _cacheState.value = CacheState.Loading(progress = null)

                            val startTime = currentTimeMillis()
                            cacheSpriteAndGlyphs()
                            val duration = currentTimeMillis() - startTime

                            // Verify integrity
                            if (verifyCacheIntegrity()) {
                                preferences.markCacheComplete()
                                _cacheState.value =
                                    CacheState.Complete(
                                        fileCount = EXPECTED_FILE_COUNT,
                                        durationMs = duration,
                                    )
                                Log.i(TAG, "Sprite cache complete! ($EXPECTED_FILE_COUNT files, ${duration}ms)")
                            } else {
                                val message = "Cache integrity verification failed"
                                Log.e(TAG, message)
                                _cacheState.value =
                                    CacheState.Failed(
                                        Exception("Cache integrity check failed"),
                                        message,
                                    )
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Sprite cache failed", throwable = e)
                            _cacheState.value =
                                CacheState.Failed(
                                    e,
                                    e.message ?: "Unknown error",
                                )
                        }
                    }
            }
        }
    }

    /**
     * Suspend until cache completes (either successfully or with failure).
     *
     * This is a reactive listener pattern - no arbitrary timeouts.
     *
     * @return Final cache state (Complete or Failed)
     */
    suspend fun awaitCompletion(): CacheState =
        cacheStateFlow
            .filter { it is CacheState.Complete || it is CacheState.Failed }
            .first()

    /**
     * Verify cache integrity by checking file count.
     *
     * @return true if at least 95% of expected files exist
     */
    private suspend fun verifyCacheIntegrity(): Boolean =
        try {
            val actualCount = countCachedSpriteFiles()
            val threshold = (EXPECTED_FILE_COUNT * INTEGRITY_THRESHOLD).toInt()
            val isValid = actualCount >= threshold

            if (!isValid) {
                Log.w(TAG, "Cache integrity check failed: $actualCount/$EXPECTED_FILE_COUNT files (threshold: $threshold)")
            }

            isValid
        } catch (e: Exception) {
            Log.e(TAG, "Failed to verify cache integrity", throwable = e)
            false
        }

    companion object {
        private const val TAG = "SpriteCache"

        /**
         * Expected number of sprite/glyph files (from style_file_listing.txt).
         * Breakdown:
         * - 257 Roboto Regular glyph files (~2.0MB)
         * - 257 Roboto Medium glyph files (~2.1MB)
         * - 257 Roboto Condensed Italic glyph files (~2.0MB)
         * - 4 sprite files (2 JSON + 2 PNG, ~156KB)
         * Total: 775 files, ~6.4MB
         */
        private const val EXPECTED_FILE_COUNT = 775

        /**
         * Expected total size in MB (approximate).
         */
        private const val EXPECTED_SIZE_MB = 6.4

        /**
         * Minimum required disk space (10MB buffer for safety).
         */
        private const val REQUIRED_DISK_SPACE_BYTES = 10L * 1024 * 1024

        /**
         * Cache integrity threshold (95% of files must exist).
         */
        private const val INTEGRITY_THRESHOLD = 0.95f
    }
}
