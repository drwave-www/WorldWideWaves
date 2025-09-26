package com.worldwidewaves.shared.viewmodels

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

import com.worldwidewaves.shared.map.MapFeatureState
import kotlinx.coroutines.flow.StateFlow

/**
 * Shared interface for map download management across platforms.
 * Provides common map download operations that work on both Android and iOS.
 * Platform implementations: AndroidMapViewModel (Play Core) and iOSMapViewModel (future).
 */
interface IMapDownloadManager {
    val featureState: StateFlow<MapFeatureState>

    /**
     * Check if a map is available for the given event ID.
     */
    suspend fun checkIfMapIsAvailable(mapId: String, autoDownload: Boolean = false)

    /**
     * Start downloading a map for the given event ID.
     */
    suspend fun downloadMap(mapId: String, onMapDownloaded: (() -> Unit)? = null)

    /**
     * Cancel any ongoing download.
     */
    suspend fun cancelDownload()

    /**
     * Get human-readable error message for error code.
     */
    fun getErrorMessage(errorCode: Int): String
}

/**
 * Shared utilities for map download management.
 * Contains platform-agnostic logic that can be reused across Android/iOS.
 */
object MapDownloadUtils {

    /**
     * Shared retry management with exponential backoff.
     */
    class RetryManager {
        private var retryCount = 0

        companion object {
            const val MAX_RETRIES = 3
            const val BASE_RETRY_DELAY_MS = 1000L
        }

        fun canRetry(): Boolean = retryCount < MAX_RETRIES
        fun getNextRetryDelay(): Long = BASE_RETRY_DELAY_MS * (1 shl retryCount)
        fun incrementRetryCount(): Int = ++retryCount
        fun resetRetryCount() { retryCount = 0 }
        fun getCurrentRetryCount(): Int = retryCount
    }

    /**
     * Calculate download progress percentage.
     */
    fun calculateProgressPercent(totalBytes: Long, downloadedBytes: Long): Int {
        return if (totalBytes > 0) {
            (downloadedBytes * 100L / totalBytes).toInt()
        } else {
            0
        }
    }

    /**
     * Check if state represents an active download.
     */
    fun isActiveDownload(state: MapFeatureState): Boolean {
        return state is MapFeatureState.Downloading ||
               state is MapFeatureState.Pending ||
               state is MapFeatureState.Installing
    }
}