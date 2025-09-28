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
import kotlinx.datetime.Clock
import platform.Foundation.NSBundle
import platform.Foundation.NSBundleResourceRequest
import kotlin.coroutines.resume
import kotlin.random.Random

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
class IOSPlatformMapManager : PlatformMapManager {
    companion object {
        private const val MAP_BUNDLE_EXTENSION = "geojson"
        private const val MAX_CONCURRENT_DOWNLOADS = 3 // iOS ODR best practice
        private const val PROGRESS_UPDATE_INTERVAL_MS = 50L // Smoother progress updates
        private const val MIN_DOWNLOAD_DURATION_MS = 1000L // Minimum realistic download time
        private const val MAX_DOWNLOAD_DURATION_MS = 30000L // Maximum timeout
    }

    // Thread-safe resource management
    private val activeRequests = mutableMapOf<String, NSBundleResourceRequest>()
    private val requestMutex = Mutex()

    // Concurrent download limiting
    private val downloadSemaphore = Semaphore(MAX_CONCURRENT_DOWNLOADS)

    // Cache invalidation support
    private val availabilityCache = mutableMapOf<String, Pair<Boolean, Long>>()
    private val cacheMutex = Mutex()
    private val cacheValidityMs = 30000L // 30 seconds cache validity

    /**
     * Check if an ODR map resource is available.
     */
    @OptIn(ExperimentalForeignApi::class)
    override fun isMapAvailable(mapId: String): Boolean {
        return try {
            val currentTime =
                kotlinx.datetime.Clock.System
                    .now()
                    .toEpochMilliseconds()

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
            val currentTime =
                kotlinx.datetime.Clock.System
                    .now()
                    .toEpochMilliseconds()
            availabilityCache[mapId] = Pair(false, currentTime - cacheValidityMs + 5000L) // 5 second retry window
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
                onProgress(100)
                onSuccess()
                WWWLogger.i("IOSPlatformMapManager", "ODR map already available: $mapId")
                return@withPermit
            }

            requestMutex.withLock {
                // Check if download is already in progress
                if (activeRequests.containsKey(mapId)) {
                    WWWLogger.w("IOSPlatformMapManager", "Download already in progress for $mapId")
                    onError(-3, "Download already in progress")
                    return@withPermit
                }

                // Create NSBundleResourceRequest for the map tag
                val resourceTags = setOf(mapId)
                val request = NSBundleResourceRequest(resourceTags)
                activeRequests[mapId] = request

                // Setup realistic progress tracking
                val downloadStartTime =
                    kotlinx.datetime.Clock.System
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
                    onProgress(100)
                    // Invalidate cache to reflect new availability
                    invalidateCache(mapId)
                    onSuccess()
                } else {
                    onError(-1, "ODR resource download failed")
                }
            }
        } catch (e: Exception) {
            requestMutex.withLock {
                activeRequests.remove(mapId)
            }
            onError(-2, e.message ?: "Unknown error during ODR download")
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
                mapId.contains("city") || mapId.contains("_") -> MIN_DOWNLOAD_DURATION_MS + 2000L // 3 seconds for cities
                mapId.contains("country") -> MIN_DOWNLOAD_DURATION_MS + 5000L // 6 seconds for countries
                mapId.contains("world") -> MIN_DOWNLOAD_DURATION_MS + 10000L // 11 seconds for world
                else -> MIN_DOWNLOAD_DURATION_MS + 3000L // 4 seconds default
            }

        // Add random variation (±30%) to simulate real network conditions
        val variation = Random.nextDouble(0.7, 1.3)
        return (baseDuration * variation).toLong().coerceAtMost(MAX_DOWNLOAD_DURATION_MS)
    }

    /**
     * Simulates realistic progress with exponential curve matching real download patterns.
     * Fast initial progress, slower middle phase, quick completion.
     */
    private suspend fun simulateRealisticProgress(
        mapId: String,
        estimatedDuration: Long,
        startTime: Long,
        onProgress: (Int) -> Unit,
    ) {
        try {
            var lastProgress = 0
            onProgress(0)

            while (lastProgress < 95) {
                // Check if download was cancelled
                val isActive = requestMutex.withLock { activeRequests.containsKey(mapId) }
                if (!isActive) break

                delay(PROGRESS_UPDATE_INTERVAL_MS)

                val elapsed =
                    kotlinx.datetime.Clock.System
                        .now()
                        .toEpochMilliseconds() - startTime
                val progressRatio = (elapsed.toDouble() / estimatedDuration).coerceAtMost(0.95)

                // Exponential progress curve: fast start, slower middle, quick end
                val exponentialProgress =
                    when {
                        progressRatio < 0.1 -> progressRatio * 3.0 // Fast initial 30%
                        progressRatio < 0.7 -> 0.3 + (progressRatio - 0.1) * 0.5 // Slower 50%
                        else -> 0.8 + (progressRatio - 0.7) * 0.5 // Final 15%
                    }

                val currentProgress = (exponentialProgress * 100).toInt().coerceIn(lastProgress + 1, 95)

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

    /**
     * Invalidates all cached availability data.
     * Useful after app restart or significant state changes.
     */
    suspend fun invalidateAllCache() {
        cacheMutex.withLock {
            val cacheSize = availabilityCache.size
            availabilityCache.clear()
            WWWLogger.d("IOSPlatformMapManager", "Invalidated all cache entries ($cacheSize items)")
        }
    }

    /**
     * Gets current cache statistics for monitoring and debugging.
     */
    suspend fun getCacheStats(): Map<String, Any> =
        cacheMutex.withLock {
            mapOf(
                "cached_maps" to availabilityCache.size,
                "active_downloads" to activeRequests.size,
                "available_download_slots" to downloadSemaphore.availablePermits,
                "cache_validity_ms" to cacheValidityMs,
            )
        }

    /**
     * Enhanced cleanup method with comprehensive resource management.
     * Should be called when the manager is no longer needed.
     */
    @OptIn(ExperimentalForeignApi::class)
    suspend fun cleanup() {
        WWWLogger.d("IOSPlatformMapManager", "Starting comprehensive cleanup...")

        val stats = getCacheStats()
        WWWLogger.d("IOSPlatformMapManager", "Pre-cleanup stats: $stats")

        // Cancel all active downloads with proper synchronization
        requestMutex.withLock {
            activeRequests.keys.toList().forEach { mapId ->
                try {
                    activeRequests[mapId]?.endAccessingResources()
                    WWWLogger.v("IOSPlatformMapManager", "Cleaned up ODR request for $mapId")
                } catch (e: Exception) {
                    WWWLogger.e("IOSPlatformMapManager", "Error cleaning up ODR request for $mapId", e)
                }
            }
            activeRequests.clear()
        }

        // Clear availability cache
        invalidateAllCache()

        WWWLogger.i("IOSPlatformMapManager", "Cleanup completed successfully")
    }

    /**
     * Health check method for monitoring ODR system state.
     */
    suspend fun healthCheck(): Map<String, Any> =
        try {
            val stats = getCacheStats()
            val bundle = NSBundle.mainBundle

            mapOf(
                "status" to "healthy",
                "odr_bundle_available" to (bundle != null),
                "stats" to stats,
                "timestamp" to
                    kotlinx.datetime.Clock.System
                        .now()
                        .toEpochMilliseconds(),
            )
        } catch (e: Exception) {
            WWWLogger.e("IOSPlatformMapManager", "Health check failed", e)
            mapOf(
                "status" to "error",
                "error" to (e.message ?: "Unknown error"),
                "timestamp" to
                    kotlinx.datetime.Clock.System
                        .now()
                        .toEpochMilliseconds(),
            )
        }
}
