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
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import platform.Foundation.NSBundle
import platform.Foundation.NSBundleResourceRequest
import kotlin.coroutines.resume
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * iOS implementation of PlatformMapManager using On-Demand Resources (ODR).
 *
 * This implementation provides equivalent functionality to Android's Dynamic Feature Modules:
 * - Uses ODR (On-Demand Resources) to download map resources on-demand
 * - Manages resource lifecycle and availability
 * - Provides progress updates during resource downloads
 * - Supports cancellation of ongoing downloads
 *
 * ODR Setup Required:
 * - Map resources must be tagged in Xcode with corresponding mapIds
 * - Resources should be marked as "On Demand" in project settings
 * - Bundle tags in Info.plist must match map IDs
 */
@OptIn(ExperimentalTime::class)
class IOSPlatformMapManager : PlatformMapManager {
    companion object {
        private const val MAP_BUNDLE_EXTENSION = "geojson"
        private const val MAX_CONCURRENT_DOWNLOADS = 3 // iOS ODR best practice
        private const val PROGRESS_UPDATE_INTERVAL_MS = 50L // Smoother progress updates
        private const val MIN_DOWNLOAD_DURATION_MS = 1000L // Minimum realistic download time
        private const val MAX_DOWNLOAD_DURATION_MS = 30000L // Maximum timeout

        // Cache configuration
        private const val CACHE_VALIDITY_MS = 30000L // 30 seconds cache validity
        private const val CACHE_RETRY_WINDOW_MS = 5000L // 5 second retry window

        // Duration estimates for different map types (in milliseconds)
        private const val CITY_MAP_ADDITIONAL_DURATION_MS = 2000L
        private const val COUNTRY_MAP_ADDITIONAL_DURATION_MS = 5000L
        private const val WORLD_MAP_ADDITIONAL_DURATION_MS = 10000L
        private const val DEFAULT_MAP_ADDITIONAL_DURATION_MS = 3000L

        // Random variation bounds for realistic simulation
        private const val MIN_VARIATION_FACTOR = 0.7
        private const val MAX_VARIATION_FACTOR = 1.3

        // Progress curve thresholds
        private const val FAST_INITIAL_PROGRESS_THRESHOLD = 0.1
        private const val SLOW_MIDDLE_PROGRESS_THRESHOLD = 0.7
        private const val INITIAL_PROGRESS_MULTIPLIER = 3.0
        private const val MIDDLE_PROGRESS_BASE = 0.3
        private const val MIDDLE_PROGRESS_FACTOR = 0.5
        private const val FINAL_PROGRESS_BASE = 0.8
        private const val FINAL_PROGRESS_FACTOR = 0.5
        private const val MAX_PROGRESS_WITHOUT_COMPLETION = 95

        // Error codes
        private const val ERROR_CODE_DOWNLOAD_FAILED = -1
        private const val ERROR_CODE_UNKNOWN_ERROR = -2
        private const val ERROR_CODE_DOWNLOAD_IN_PROGRESS = -3

        // Progress constants
        private const val COMPLETE_PROGRESS_PERCENT = 100
    }

    // Thread-safe resource management
    private val activeRequests = mutableMapOf<String, NSBundleResourceRequest>()
    private val requestMutex = Mutex()

    // Concurrent download limiting
    private val downloadSemaphore = Semaphore(MAX_CONCURRENT_DOWNLOADS)

    // Cache invalidation support
    private val availabilityCache = mutableMapOf<String, Pair<Boolean, Long>>()
    private val cacheMutex = Mutex()
    private val cacheValidityMs = CACHE_VALIDITY_MS

    /**
     * Check if an ODR map resource is available.
     */
    @OptIn(ExperimentalForeignApi::class, ExperimentalTime::class)
    override fun isMapAvailable(mapId: String): Boolean {
        return try {
            val currentTime = Clock.System.now().toEpochMilliseconds()

            // Check cache first (with invalidation support)
            availabilityCache[mapId]?.let { (cachedResult, cacheTime) ->
                if (currentTime - cacheTime < cacheValidityMs) {
                    WWWLogger.v("IOSPlatformMapManager", "Using cached availability for $mapId: $cachedResult")
                    return cachedResult
                } else {
                    WWWLogger.d("IOSPlatformMapManager", "Cache expired for $mapId, refreshing...")
                }
            }

            // Check if ODR resource with this tag is available
            val bundle = NSBundle.mainBundle
            val paths = bundle.pathsForResourcesOfType(MAP_BUNDLE_EXTENSION, inDirectory = mapId)
            val isAvailable = paths.isNotEmpty()

            // Cache the result with timestamp
            availabilityCache[mapId] = Pair(isAvailable, currentTime)

            WWWLogger.d("IOSPlatformMapManager", "ODR map availability check: $mapId -> $isAvailable (cached)")
            isAvailable
        } catch (e: Exception) {
            WWWLogger.e("IOSPlatformMapManager", "Error checking ODR map availability: $mapId", e)

            // Cache failure result for shorter period to allow retries
            val currentTime = Clock.System.now().toEpochMilliseconds()
            availabilityCache[mapId] = Pair(false, currentTime - cacheValidityMs + CACHE_RETRY_WINDOW_MS)
            false
        }
    }

    /**
     * Download map using ODR (On-Demand Resources).
     *
     * This initiates an ODR resource request and provides progress updates.
     */
    @OptIn(ExperimentalForeignApi::class)
    override suspend fun downloadMap(
        mapId: String,
        onProgress: (Int) -> Unit,
        onSuccess: () -> Unit,
        onError: (Int, String?) -> Unit,
    ) = downloadSemaphore.withPermit {
        WWWLogger.i(
            "IOSPlatformMapManager",
            "Starting ODR map download: $mapId (${downloadSemaphore.availablePermits + 1}/${MAX_CONCURRENT_DOWNLOADS} slots)",
        )

        try {
            // Check if already available (invalidate cache first)
            invalidateCache(mapId)
            if (isMapAvailable(mapId)) {
                onProgress(COMPLETE_PROGRESS_PERCENT)
                onSuccess()
                WWWLogger.i("IOSPlatformMapManager", "ODR map already available: $mapId")
                return@withPermit
            }

            requestMutex.withLock {
                // Check if download is already in progress
                if (activeRequests.containsKey(mapId)) {
                    WWWLogger.w("IOSPlatformMapManager", "Download already in progress for $mapId")
                    onError(ERROR_CODE_DOWNLOAD_IN_PROGRESS, "Download already in progress")
                    return@withPermit
                }

                // Create NSBundleResourceRequest for the map tag
                val resourceTags = setOf(mapId)
                val request = NSBundleResourceRequest(resourceTags)
                activeRequests[mapId] = request

                // Setup realistic progress tracking
                val downloadStartTime =
                    Clock.System
                        .now()
                        .toEpochMilliseconds()
                val estimatedDuration = estimateDownloadDuration(mapId)

                WWWLogger.d("IOSPlatformMapManager", "Estimated download duration for $mapId: ${estimatedDuration}ms")

                // Begin accessing the resource
                val downloadSuccessful =
                    suspendCancellableCoroutine { continuation ->
                        request.beginAccessingResourcesWithCompletionHandler { error ->
                            requestMutex.tryLock().let { locked ->
                                if (locked) {
                                    activeRequests.remove(mapId)
                                    requestMutex.unlock()
                                }
                            }

                            if (error != null) {
                                WWWLogger.e("IOSPlatformMapManager", "ODR download failed for $mapId: ${error.localizedDescription}")
                                continuation.resume(false)
                            } else {
                                WWWLogger.i("IOSPlatformMapManager", "ODR download succeeded for $mapId")
                                continuation.resume(true)
                            }
                        }

                        // Setup cancellation with proper cleanup
                        continuation.invokeOnCancellation {
                            requestMutex.tryLock().let { locked ->
                                if (locked) {
                                    activeRequests.remove(mapId)
                                    request.endAccessingResources()
                                    requestMutex.unlock()
                                }
                            }
                            WWWLogger.d("IOSPlatformMapManager", "ODR download cancelled for $mapId")
                        }
                    }

                // Realistic progress simulation with exponential curve
                simulateRealisticProgress(mapId, estimatedDuration, downloadStartTime, onProgress)

                if (downloadSuccessful) {
                    onProgress(COMPLETE_PROGRESS_PERCENT)
                    // Invalidate cache to reflect new availability
                    invalidateCache(mapId)
                    onSuccess()
                } else {
                    onError(ERROR_CODE_DOWNLOAD_FAILED, "ODR resource download failed")
                }
            }
        } catch (e: Exception) {
            requestMutex.withLock {
                activeRequests.remove(mapId)
            }
            onError(ERROR_CODE_UNKNOWN_ERROR, e.message ?: "Unknown error during ODR download")
            WWWLogger.e("IOSPlatformMapManager", "Error during ODR download: $mapId", e)
        }
    }

    /**
     * Cancel active ODR download for the specified map.
     */
    @OptIn(ExperimentalForeignApi::class)
    override fun cancelDownload(mapId: String) {
        WWWLogger.d("IOSPlatformMapManager", "Cancel ODR download requested for: $mapId")

        try {
            requestMutex.tryLock().let { locked ->
                if (locked) {
                    activeRequests[mapId]?.let { request ->
                        try {
                            request.endAccessingResources()
                            activeRequests.remove(mapId)
                            WWWLogger.i("IOSPlatformMapManager", "ODR download cancelled successfully: $mapId")
                        } catch (e: Exception) {
                            WWWLogger.e("IOSPlatformMapManager", "Error cancelling ODR download: $mapId", e)
                        }
                    } ?: run {
                        WWWLogger.d("IOSPlatformMapManager", "No active ODR download to cancel for: $mapId")
                    }
                    requestMutex.unlock()
                }
            }
        } catch (e: Exception) {
            WWWLogger.e("IOSPlatformMapManager", "Error in cancelDownload synchronization: $mapId", e)
        }
    }

    /**
     * Estimates realistic download duration based on map ID characteristics.
     * Factors in typical ODR resource sizes and network conditions.
     */
    private fun estimateDownloadDuration(mapId: String): Long {
        // Estimate based on map complexity (city vs country vs world)
        val baseDuration =
            when {
                mapId.contains("city") || mapId.contains("_") -> MIN_DOWNLOAD_DURATION_MS + CITY_MAP_ADDITIONAL_DURATION_MS
                mapId.contains("country") -> MIN_DOWNLOAD_DURATION_MS + COUNTRY_MAP_ADDITIONAL_DURATION_MS
                mapId.contains("world") -> MIN_DOWNLOAD_DURATION_MS + WORLD_MAP_ADDITIONAL_DURATION_MS
                else -> MIN_DOWNLOAD_DURATION_MS + DEFAULT_MAP_ADDITIONAL_DURATION_MS
            }

        // Add random variation (±30%) to simulate real network conditions
        val variation = Random.nextDouble(MIN_VARIATION_FACTOR, MAX_VARIATION_FACTOR)
        return (baseDuration * variation).toLong().coerceAtMost(MAX_DOWNLOAD_DURATION_MS)
    }

    /**
     * Simulates realistic progress with exponential curve matching real download patterns.
     * Fast initial progress, slower middle phase, quick completion.
     */
    @OptIn(ExperimentalTime::class)
    private suspend fun simulateRealisticProgress(
        mapId: String,
        estimatedDuration: Long,
        startTime: Long,
        onProgress: (Int) -> Unit,
    ) {
        try {
            var lastProgress = 0
            onProgress(0)

            while (lastProgress < MAX_PROGRESS_WITHOUT_COMPLETION) {
                // Check if download was cancelled
                val isActive = requestMutex.withLock { activeRequests.containsKey(mapId) }
                if (!isActive) break

                delay(PROGRESS_UPDATE_INTERVAL_MS)

                val elapsed = Clock.System.now().toEpochMilliseconds() - startTime
                val progressRatio = (elapsed.toDouble() / estimatedDuration).coerceAtMost(MAX_PROGRESS_WITHOUT_COMPLETION / 100.0)

                // Exponential progress curve: fast start, slower middle, quick end
                val exponentialProgress =
                    when {
                        progressRatio < FAST_INITIAL_PROGRESS_THRESHOLD -> progressRatio * INITIAL_PROGRESS_MULTIPLIER // Fast initial 30%
                        progressRatio < SLOW_MIDDLE_PROGRESS_THRESHOLD ->
                            MIDDLE_PROGRESS_BASE +
                                (progressRatio - FAST_INITIAL_PROGRESS_THRESHOLD) * MIDDLE_PROGRESS_FACTOR // Slower 50%
                        else -> FINAL_PROGRESS_BASE + (progressRatio - SLOW_MIDDLE_PROGRESS_THRESHOLD) * FINAL_PROGRESS_FACTOR // Final 15%
                    }

                val currentProgress =
                    (exponentialProgress * COMPLETE_PROGRESS_PERCENT).toInt().coerceIn(
                        lastProgress + 1,
                        MAX_PROGRESS_WITHOUT_COMPLETION,
                    )

                if (currentProgress > lastProgress) {
                    onProgress(currentProgress)
                    lastProgress = currentProgress
                }
            }
        } catch (e: Exception) {
            WWWLogger.e("IOSPlatformMapManager", "Error in progress simulation for $mapId", e)
        }
    }

    /**
     * Invalidates cached availability for specific map ID.
     * Forces fresh ODR availability check on next access.
     */
    private suspend fun invalidateCache(mapId: String) {
        cacheMutex.withLock {
            availabilityCache.remove(mapId)
            WWWLogger.v("IOSPlatformMapManager", "Cache invalidated for $mapId")
        }
    }
}
