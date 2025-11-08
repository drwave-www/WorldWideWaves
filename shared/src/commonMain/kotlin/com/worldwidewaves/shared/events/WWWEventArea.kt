package com.worldwidewaves.shared.events

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

import com.worldwidewaves.shared.WWWGlobals
import com.worldwidewaves.shared.data.MapFileExtension
import com.worldwidewaves.shared.data.getMapFileAbsolutePath
import com.worldwidewaves.shared.events.data.GeoJsonDataProvider
import com.worldwidewaves.shared.events.geometry.EventAreaGeometry
import com.worldwidewaves.shared.events.geometry.EventAreaPositionTesting
import com.worldwidewaves.shared.events.io.GeoJsonAreaParser
import com.worldwidewaves.shared.events.utils.Area
import com.worldwidewaves.shared.events.utils.BoundingBox
import com.worldwidewaves.shared.events.utils.CoroutineScopeProvider
import com.worldwidewaves.shared.events.utils.DataValidator
import com.worldwidewaves.shared.events.utils.MutableArea
import com.worldwidewaves.shared.events.utils.Position
import com.worldwidewaves.shared.utils.Log
import kotlinx.atomicfu.AtomicBoolean
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Geospatial definition of an event: polygons and bounding-box.
 *
 * Loads polygons from the event-specific cached GeoJSON (or an optional `bbox`
 * string override), then:
 *  • Caches the parsed polygons, computed bounding-box and center position.
 *  • Provides fast `isPositionWithin()` tests (bbox pre-check + polygon test).
 *  • Offers helpers such as `generateRandomPositionInArea()` for simulation.
 *  • Exposes lazy `bbox()` / `getCenter()` accessors used by map & wave logic.
 *
 * All heavy I/O / parsing work is executed inside the provided
 * [CoroutineScopeProvider] so callers remain on the main thread.
 *
 * This class now delegates to focused modules:
 * - [GeoJsonAreaParser] for GeoJSON parsing
 * - [EventAreaGeometry] for geometry calculations
 * - [EventAreaPositionTesting] for position testing
 */
@Serializable
data class WWWEventArea(
    val osmAdminids: List<Int>,
    val bbox: String? = null,
) : KoinComponent,
    DataValidator {
    companion object {
        // Circuit breaker: Track failed polygon loading attempts to prevent retry storms
        // Maps event ID to timestamp (milliseconds) of last failed load attempt
        private val failedLoadAttempts = mutableMapOf<String, Long>()
        private val failedLoadMutex = Mutex()

        // Retry backoff period: Don't retry for 60 seconds after failure
        private const val CIRCUIT_BREAKER_COOLDOWN_MS = 60_000L

        /**
         * Checks if we should skip loading attempt due to recent failure (circuit breaker open)
         */
        @OptIn(ExperimentalTime::class)
        private suspend fun shouldSkipLoadDueToRecentFailure(eventId: String): Boolean {
            failedLoadMutex.withLock {
                val lastFailureTime = failedLoadAttempts[eventId] ?: return false
                val timeSinceFailure = Clock.System.now().toEpochMilliseconds() - lastFailureTime

                if (timeSinceFailure < CIRCUIT_BREAKER_COOLDOWN_MS) {
                    // Circuit breaker still open - skip retry
                    return true
                } else {
                    // Cooldown expired - allow retry and remove from map
                    failedLoadAttempts.remove(eventId)
                    return false
                }
            }
        }

        /**
         * Records a failed load attempt to activate circuit breaker
         */
        @OptIn(ExperimentalTime::class)
        private suspend fun recordFailedLoadAttempt(eventId: String) {
            failedLoadMutex.withLock {
                failedLoadAttempts[eventId] = Clock.System.now().toEpochMilliseconds()
            }
        }

        /**
         * Clears failed load attempt record (called after successful load or manual cache clear)
         */
        private suspend fun clearFailedLoadAttempt(eventId: String) {
            failedLoadMutex.withLock {
                failedLoadAttempts.remove(eventId)
            }
        }
    }

    private var _event: IWWWEvent? = null
    private var event: IWWWEvent
        get() = _event ?: error("Event not set")
        set(value) {
            _event = value
        }

    // ---------------------------

    val bboxIsOverride: Boolean by lazy { EventAreaGeometry.parseBboxString(bbox) != null }

    // ---------------------------

    private val geoJsonDataProvider: GeoJsonDataProvider by inject()
    private val coroutineScopeProvider: CoroutineScopeProvider by inject()

    @Transient private var cachedAreaPolygons: Area? = null

    @Transient private val polygonsCacheMutex = Mutex()

    // Add mutex for cache protection
    @Transient private var cachedBoundingBox: BoundingBox? = null

    @Transient private var cachedCenter: Position? = null

    @Transient private var cachedPositionWithinResult: Pair<Position, Boolean>? = null

    // Polygon loading state notification
    @Transient private val _polygonsLoaded = MutableStateFlow(false)
    val polygonsLoaded: StateFlow<Boolean> = _polygonsLoaded.asStateFlow()

    // Log-once flags to prevent excessive logging for undownloaded maps
    @Transient private val loggedMissingPolygons: AtomicBoolean = atomic(false)

    @Transient private val loggedInvalidBbox: AtomicBoolean = atomic(false)

    // ---------------------------

    /**
     * Clears transient cached data while preserving immutable polygon data.
     *
     * Polygons are derived from GeoJSON files and remain valid across observer
     * lifecycle changes (simulation start/stop, screen navigation). Clearing
     * them forces expensive re-parsing on every observer restart.
     *
     * Only position-dependent and derived data needs clearing:
     * - Bounding box (can be recalculated from polygons if needed)
     * - Center point (can be recalculated from polygons if needed)
     * - Position containment result (depends on user position, must invalidate)
     *
     * Polygon data is immutable for a given event and should not be cleared
     * unless the event's GeoJSON file actually changes (which doesn't happen
     * during normal app usage).
     */
    fun clearCache() {
        // DO NOT clear cachedAreaPolygons - immutable event data
        // DO NOT clear _polygonsLoaded - accurate state indicator

        // Clear derived/transient data
        cachedBoundingBox = null // Recomputable from polygons
        cachedCenter = null // Recomputable from polygons
        cachedPositionWithinResult = null // Position-specific cache must be cleared

        Log.v("WWWEventArea", "clearCache() called for ${event?.id} - preserved polygon data, cleared derived caches")
    }

    /**
     * Clears polygon cache to force reload from newly downloaded GeoJSON file.
     *
     * This is called specifically when a map is downloaded mid-session.
     * Unlike clearCache(), this MUST clear polygon data because the GeoJSON
     * file has changed from non-existent to available on disk.
     *
     * THREAD SAFETY: Uses mutex to prevent race conditions with concurrent
     * polygon loading operations. Ensures cache clear is atomic.
     *
     * Use case: User navigates to event → polygons try to load → file missing → empty cache.
     * Then user downloads map → file now exists → must clear empty cache to force reload.
     */
    internal suspend fun clearPolygonCacheForDownload() {
        polygonsCacheMutex.withLock {
            cachedAreaPolygons = null
            _polygonsLoaded.value = false
            cachedBoundingBox = null
            cachedCenter = null
            cachedPositionWithinResult = null

            // Clear wave duration cache - it depends on bbox from polygons
            event?.wave?.clearDurationCache()

            // Reset log flags - allow logging if issues persist after download
            loggedMissingPolygons.value = false
            loggedInvalidBbox.value = false

            // Clear circuit breaker - allow immediate retry since file is now available
            clearFailedLoadAttempt(event.id)

            Log.i(
                "WWWEventArea",
                "clearPolygonCacheForDownload() called for ${event?.id} - cleared all caches including wave duration to reload from downloaded file",
            )
        }
    }

    // ---------------------------

    fun setRelatedEvent(event: WWWEvent) {
        this.event = event
        // Clear cache to ensure fresh calculations for this event
        clearCache()
    }

    // ---------------------------

    /**
     * Retrieves the file path of the GeoJSON file for the event.
     *
     * This function attempts to get the absolute path of the GeoJSON file associated with the event.
     * It uses the event's ID to locate the file within the cache directory.
     */
    internal suspend fun getGeoJsonFilePath(): String? = getMapFileAbsolutePath(event.id, MapFileExtension.GEOJSON)

    // ---------------------------

    /**
     * Checks if a given position is within the event area.
     *
     * Delegates to [EventAreaPositionTesting] for position checking.
     */
    suspend fun isPositionWithin(position: Position): Boolean {
        val boundingBox = bbox()
        val polygons = getPolygons()

        // Defensive check: log once if polygons not loaded (expected for undownloaded maps)
        if (polygons.isEmpty()) {
            if (!loggedMissingPolygons.getAndSet(true)) {
                Log.v(
                    "WWWEventArea",
                    "isPositionWithin: ${event?.id} - empty polygons, GeoJSON not loaded yet. Position: $position",
                )
            }
            return false
        }

        val (result, newCache) =
            EventAreaPositionTesting.isPositionWithin(
                position,
                boundingBox,
                polygons,
                cachedPositionWithinResult,
            )

        // Update cache if changed
        if (newCache != null) {
            cachedPositionWithinResult = newCache
        }

        return result
    }

    /**
     * Checks if a given position is within the event area using pre-fetched polygons.
     *
     * Performance optimization: Use this overload when polygons are already available
     * to avoid redundant getPolygons() calls.
     *
     * @param position The position to check
     * @param polygons Pre-fetched polygon data
     * @return true if position is within the area, false otherwise
     */
    suspend fun isPositionWithin(
        position: Position,
        polygons: Area,
    ): Boolean {
        val boundingBox = bbox()

        val (result, newCache) =
            EventAreaPositionTesting.isPositionWithin(
                position,
                boundingBox,
                polygons,
                cachedPositionWithinResult,
            )

        // Update cache if changed
        if (newCache != null) {
            cachedPositionWithinResult = newCache
        }

        return result
    }

    // ---------------------------

    /**
     * Generates a random position within the event area.
     * Delegates to [EventAreaPositionTesting] for random position generation.
     */
    suspend fun generateRandomPositionInArea(): Position {
        val boundingBox = bbox()
        val center = getCenter()
        return EventAreaPositionTesting.generateRandomPositionInArea(event, boundingBox, center)
    }

    // ---------------------------

    /**
     * Computes the bounding box for the event area.
     * Delegates to [EventAreaGeometry] for bounding box calculation.
     *
     * Note: Invalid bboxes (0,0,0,0) are not cached to prevent cache poisoning
     * when GeoJSON files are not yet available during initial map setup.
     */
    suspend fun bbox(): BoundingBox {
        // Return cached bounding box if available
        cachedBoundingBox?.let { return it }

        val bbox =
            EventAreaGeometry.computeBoundingBox(
                event,
                bbox,
                geoJsonDataProvider,
                cachedAreaPolygons,
            )

        // Only cache valid bboxes to prevent cache poisoning
        // Invalid bbox (0,0,0,0) indicates GeoJSON data not yet available
        val isValidBbox =
            bbox.sw.lat != 0.0 ||
                bbox.sw.lng != 0.0 ||
                bbox.ne.lat != 0.0 ||
                bbox.ne.lng != 0.0

        if (isValidBbox) {
            cachedBoundingBox = bbox
        } else {
            // Log once per event (expected for undownloaded maps)
            if (!loggedInvalidBbox.getAndSet(true)) {
                Log.v(
                    "WWWEventArea",
                    "bbox: ${event.id} - invalid (0,0,0,0), GeoJSON not loaded yet",
                )
            }
        }

        return bbox
    }

    /**
     * Calculates the center position of the event area.
     * Delegates to [EventAreaGeometry] for center calculation.
     */
    suspend fun getCenter(): Position =
        cachedCenter ?: bbox().let { bbox ->
            EventAreaGeometry.computeCenter(bbox).also { cachedCenter = it }
        }

    // ---------------------------

    /**
     * Retrieves the polygons representing the event area.
     * Delegates to [GeoJsonAreaParser] for polygon loading.
     */
    suspend fun getPolygons(): Area {
        // Fast path: if cache is already populated, return immediately
        cachedAreaPolygons?.let {
            return it
        }

        // Slow path: populate cache with mutex protection
        if (WWWGlobals.LogConfig.ENABLE_POSITION_TRACKING_LOGGING) {
            Log.v("WWWEventArea", "getPolygons: ${event.id} cache empty, loading...")
        }
        return loadAndCachePolygons()
    }

    /**
     * Loads polygons from GeoJSON and caches them with mutex protection
     */
    private suspend fun loadAndCachePolygons(): Area {
        // Circuit breaker: Skip loading if we recently failed (prevents retry storms)
        if (shouldSkipLoadDueToRecentFailure(event.id)) {
            if (WWWGlobals.LogConfig.ENABLE_POSITION_TRACKING_LOGGING) {
                Log.v(
                    "WWWEventArea",
                    "loadAndCachePolygons: ${event.id} skipping load due to recent failure (circuit breaker active)",
                )
            }
            return emptyList()
        }

        polygonsCacheMutex.withLock {
            // Double-check pattern: another coroutine might have populated the cache
            cachedAreaPolygons?.let {
                return it
            }

            // Build polygons in a temporary mutable list
            val tempPolygons: MutableArea = mutableListOf()
            var loadFailed = false

            try {
                coroutineScopeProvider.withDefaultContext {
                    GeoJsonAreaParser.loadPolygonsFromGeoJson(
                        event,
                        geoJsonDataProvider,
                        bbox,
                        tempPolygons,
                    )
                }
                // Only log on successful load or failure, not empty result
                if (tempPolygons.isNotEmpty()) {
                    Log.i("WWWEventArea", "loadAndCachePolygons: ${event.id} loaded ${tempPolygons.size} polygons from GeoJSON")
                    // Clear circuit breaker on successful load
                    clearFailedLoadAttempt(event.id)
                } else {
                    // Empty result indicates file not available or corrupt - activate circuit breaker
                    loadFailed = true
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                Log.w("WWWEventArea", "Polygon loading cancelled for event ${event.id} (observer lifecycle)")
                // Don't cache cancellation as failure - allow retry on next call
                throw e
            } catch (e: kotlinx.serialization.SerializationException) {
                Log.w("WWWEventArea", "GeoJSON parsing error for event ${event.id}: ${e.message}")
                // Polygon loading errors are handled gracefully - empty polygon list is acceptable
                loadFailed = true
            } catch (e: Exception) {
                Log.w("WWWEventArea", "Error loading polygons for event ${event.id}: ${e.message}")
                // Polygon loading errors are handled gracefully - empty polygon list is acceptable
                loadFailed = true
            }

            // Activate circuit breaker if load failed
            if (loadFailed) {
                recordFailedLoadAttempt(event.id)
            }

            cachePolygonsIfLoaded(tempPolygons)
        }

        return cachedAreaPolygons ?: emptyList()
    }

    /**
     * Caches polygons and notifies if polygons were successfully loaded
     */
    private fun cachePolygonsIfLoaded(tempPolygons: MutableArea) {
        val hasPolygons = tempPolygons.isNotEmpty()

        if (hasPolygons) {
            // Atomically assign the complete immutable list
            cachedAreaPolygons = tempPolygons.toList()

            // Clear position check cache since polygon data changed
            cachedPositionWithinResult = null
            cachedBoundingBox = null

            // Notify that polygon data is now available
            _polygonsLoaded.value = true
        } else {
            // DON'T cache empty list - allow retry when file becomes available
            // This prevents permanent rejection if file I/O races with first position check
            // polygonsLoaded stays false so area detection is skipped
            if (WWWGlobals.LogConfig.ENABLE_POSITION_TRACKING_LOGGING) {
                Log.i("WWWEventArea", "cachePolygonsIfLoaded: ${event.id} polygons not yet available, will retry on next call")
            }
        }
    }

    // ---------------------------

    override fun validationErrors(): List<String>? =
        mutableListOf<String>()
            .apply {
                when {
                    else -> { /* No validation errors */ }
                }
            }.takeIf { it.isNotEmpty() }
            ?.map { "${WWWEventArea::class.simpleName}: $it" }
}
