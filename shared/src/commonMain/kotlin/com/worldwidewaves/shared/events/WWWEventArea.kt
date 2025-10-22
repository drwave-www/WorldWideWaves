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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

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

    // ---------------------------

    /**
     * Clears all cached data to force fresh calculations.
     * Useful when debugging or when polygon data has been updated.
     */
    fun clearCache() {
        cachedAreaPolygons = null
        cachedBoundingBox = null
        cachedCenter = null
        cachedPositionWithinResult = null
        _polygonsLoaded.value = false
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

        cachedBoundingBox = bbox
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
        polygonsCacheMutex.withLock {
            // Double-check pattern: another coroutine might have populated the cache
            cachedAreaPolygons?.let {
                return it
            }

            // Build polygons in a temporary mutable list
            val tempPolygons: MutableArea = mutableListOf()

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
                }
            } catch (e: kotlinx.serialization.SerializationException) {
                Log.w("WWWEventArea", "GeoJSON parsing error for event ${event.id}: ${e.message}")
                // Polygon loading errors are handled gracefully - empty polygon list is acceptable
            } catch (e: Exception) {
                Log.w("WWWEventArea", "Error loading polygons for event ${event.id}: ${e.message}")
                // Polygon loading errors are handled gracefully - empty polygon list is acceptable
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
            // Cache empty list to prevent redundant file I/O attempts
            // polygonsLoaded stays false so area detection is skipped
            cachedAreaPolygons = emptyList()
            if (WWWGlobals.LogConfig.ENABLE_POSITION_TRACKING_LOGGING) {
                Log.i("WWWEventArea", "cachePolygonsIfLoaded: ${event.id} cached empty polygon list (file not found or parsing failed)")
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
