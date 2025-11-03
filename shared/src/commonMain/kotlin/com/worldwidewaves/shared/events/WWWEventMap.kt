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

import com.worldwidewaves.shared.WWWGlobals.FileSystem
import com.worldwidewaves.shared.data.MapFileExtension
import com.worldwidewaves.shared.data.cacheDeepFile
import com.worldwidewaves.shared.data.cacheStringToFile
import com.worldwidewaves.shared.data.cachedFileExists
import com.worldwidewaves.shared.data.cachedFilePath
import com.worldwidewaves.shared.data.getCacheDir
import com.worldwidewaves.shared.data.getMapFileAbsolutePath
import com.worldwidewaves.shared.data.isCachedFileStale
import com.worldwidewaves.shared.data.platformFileExists
import com.worldwidewaves.shared.data.updateCacheMetadata
import com.worldwidewaves.shared.events.data.MapDataProvider
import com.worldwidewaves.shared.events.utils.DataValidator
import com.worldwidewaves.shared.events.utils.Position
import com.worldwidewaves.shared.generated.resources.Res
import com.worldwidewaves.shared.utils.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

// ---------------------------

/**
 * Encapsulates per-event map configuration and tooling.
 *
 * Core responsibilities:
 * • Build a self-contained MapLibre **style JSON** on-the-fly, wiring the cached
 *   MBTiles, GeoJSON, sprites & glyphs locations via `getStyleUri()`.
 * • Lazy-cache heavy assets (`cacheSpriteAndGlyphs`) and keep them fresh using
 *   simple TTL checks (`isCachedFileStale`, `updateCacheMetadata`).
 * • Provide convenience helpers such as [isPositionWithin] to quickly validate
 *   whether a GPS coordinate falls inside the event bounding-box (fast bbox
 *   test – no polygon walk).
 * • Offer lightweight validation of the declarative JSON section that feeds the
 *   constructor (max-zoom bounds, language / zone format, …).
 *
 * The class is platform-agnostic; platform specific adapters (Android/iOS) only
 * have to consume the generated style URI and map boundaries.
 */
@Serializable
class WWWEventMap(
    val maxZoom: Double,
    val language: String,
    val zone: String,
) : KoinComponent,
    DataValidator {
    companion object {
        private const val MAX_ZOOM_LIMIT = 20.0
    }

    private var _event: IWWWEvent? = null
    private var event: IWWWEvent
        get() = requireNotNull(_event) { "Event not set" }
        set(value) {
            _event = value
        }

    // ---------------------------

    private val mapDataProvider: MapDataProvider by inject()

    // Cache the resolved style URI to avoid repeated file I/O
    private var _cachedStyleUri: String? = null

    // ---------------------------

    fun setRelatedEvent(event: WWWEvent) {
        this.event = event
    }

    // ---------------------------

    private suspend fun getMbtilesFilePath(): String? = getMapFileAbsolutePath(event.id, MapFileExtension.MBTILES)

    // ---------------------------

    /**
     * Retrieves the URI for the map style.
     *
     * This function generates a map style JSON file based on event data and caches it for reuse.
     * It retrieves MBTiles, GeoJSON, sprites, and glyphs, fills a template with the data,
     * and returns the URI of the cached style JSON.
     *
     * **Performance**: Result is cached both in-memory and on disk to avoid redundant file I/O.
     */
    @Suppress("ReturnCount") // Early returns for guard clauses improve readability
    suspend fun getStyleUri(): String? {
        Log.d("WWWEventMap", "getStyleUri() called for event: ${event.id}")

        val styleFilename = "style-${event.id}.json"

        // Check in-memory cache first, but validate both style JSON and mbtiles file exist
        _cachedStyleUri?.let { cached ->
            // Verify cached style JSON file still exists on disk
            if (cachedFileExists(styleFilename)) {
                // CRITICAL: Also verify mbtiles file exists before returning cached style
                // Style JSON references mbtiles file - if mbtiles is missing (still copying
                // asynchronously), MapLibre will show gray screen
                val mbtilesPath = getMbtilesFilePath()
                if (mbtilesPath != null) {
                    val mbtilesExists = platformFileExists(mbtilesPath)
                    if (mbtilesExists) {
                        Log.d("WWWEventMap", "getStyleUri: Using validated cached style (mbtiles ready): $cached")
                        return cached
                    } else {
                        Log.w("WWWEventMap", "getStyleUri: Mbtiles file missing (async copy in progress), invalidating style cache")
                        _cachedStyleUri = null
                    }
                } else {
                    Log.w("WWWEventMap", "getStyleUri: Cannot get mbtiles path, invalidating style cache")
                    _cachedStyleUri = null
                }
            } else {
                Log.w("WWWEventMap", "getStyleUri: Style JSON missing, clearing in-memory cache")
                _cachedStyleUri = null
            }
        }

        // Check disk cache validity: file must exist AND not be stale
        val fileExists = cachedFileExists(styleFilename)
        val isStale = if (fileExists) isCachedFileStale(styleFilename) else true
        val isCacheValid = fileExists && !isStale

        Log.d("WWWEventMap", "getStyleUri: Cache check - exists=$fileExists, stale=$isStale, valid=$isCacheValid")

        if (isCacheValid) {
            val cachedPath = cachedFilePath(styleFilename)
            Log.i("WWWEventMap", "getStyleUri: Using cached style file: $cachedPath")
            // Store in memory cache for subsequent calls
            _cachedStyleUri = cachedPath
            return cachedPath
        }
        Log.d("WWWEventMap", "getStyleUri: Cache invalid or missing, generating new style file")

        // Parallelize file path resolution and sprite/glyph caching
        val result =
            coroutineScope {
                val mbtilesDeferred = async { getMbtilesFilePath() }
                val geojsonDeferred = async { event.area.getGeoJsonFilePath() }
                val spritesDeferred = async { cacheSpriteAndGlyphs() }

                Triple(
                    mbtilesDeferred.await(),
                    geojsonDeferred.await(),
                    spritesDeferred.await(),
                )
            }
        val mbtilesFilePath = result.first
        val geojsonFilePath = result.second
        val spriteAndGlyphsPath = result.third

        if (mbtilesFilePath == null) {
            Log.e(
                "WWWEventMap",
                "getStyleUri: MBTiles file path is null for event ${event.id} - map files may not be downloaded or cache may be corrupted",
            )
            Log.e("WWWEventMap", "getStyleUri: Failed to generate style URI - returning null")
            return null
        }
        Log.i("WWWEventMap", "getStyleUri: MBTiles path = $mbtilesFilePath")

        if (geojsonFilePath == null) {
            Log.e("WWWEventMap", "getStyleUri: GeoJSON file path is null for event ${event.id} - map data may not be available")
            Log.e("WWWEventMap", "getStyleUri: Failed to generate style URI - returning null")
            return null
        }
        Log.i("WWWEventMap", "getStyleUri: GeoJSON path = $geojsonFilePath")

        Log.d("WWWEventMap", "getStyleUri: Sprite and glyphs cached at: $spriteAndGlyphsPath")

        Log.d("WWWEventMap", "getStyleUri: Loading style template...")
        val templateData = mapDataProvider.geoMapStyleData()
        Log.i("WWWEventMap", "getStyleUri: Template loaded, length=${templateData.length}")

        // Format URIs - mbtiles uses /// format, file uses // format
        val mbtilesUri = "mbtiles:///$mbtilesFilePath"
        val geojsonUri = "file://" + geojsonFilePath.removePrefix("/")
        val glyphsUri = "file://" + spriteAndGlyphsPath.removePrefix("/") + "/files/style/glyphs"
        val spriteUri = "file://" + spriteAndGlyphsPath.removePrefix("/") + "/files/style/sprites"

        val newFileStr =
            templateData
                .replace("__MBTILES_URI__", mbtilesUri)
                .replace("__GEOJSON_URI__", geojsonUri)
                .replace("__GLYPHS_URI__", glyphsUri)
                .replace("__SPRITE_URI__", spriteUri)

        Log.i("WWWEventMap", "getStyleUri: After replacements, length=${newFileStr.length}")

        Log.v("WWWEventMap", "getStyleUri: Style template replacements:")
        Log.v("WWWEventMap", "  __MBTILES_URI__ -> $mbtilesUri")
        Log.v("WWWEventMap", "  __GEOJSON_URI__ -> $geojsonUri")
        Log.v("WWWEventMap", "  __GLYPHS_URI__ -> $glyphsUri")
        Log.v("WWWEventMap", "  __SPRITE_URI__ -> $spriteUri")

        val cachedPath = cacheStringToFile(styleFilename, newFileStr)
        if (cachedPath != null) {
            updateCacheMetadata(styleFilename)
            Log.d("WWWEventMap", "getStyleUri: Style file cached at: $cachedPath")
            Log.i("WWWEventMap", "getStyleUri: Returning style path: $cachedPath")
            // Store in memory cache for subsequent calls
            _cachedStyleUri = cachedPath
            return cachedPath
        } else {
            Log.e("WWWEventMap", "getStyleUri: Failed to cache style file")
            return null
        }
    }

    /**
     * Clears the in-memory style URI cache.
     *
     * This method should be called when the style file needs to be regenerated
     * (e.g., when underlying map data changes or cache is invalidated).
     */
    fun clearStyleUriCache() {
        Log.d("WWWEventMap", "clearStyleUriCache: Clearing in-memory style URI cache for event ${event.id}")
        _cachedStyleUri = null
    }

    // ---------------------------

    /**
     * Caches sprite and glyphs resources required for the map style.
     *
     * This function reads a file listing the required resources, caches them in parallel,
     * and returns the path to the cache directory.
     *
     * **Performance**: Uses parallel processing to cache multiple files concurrently.
     * **Cancellation**: Runs in NonCancellable context to ensure caching completes even if
     * the parent scope is cancelled (e.g., during composition disposal or navigation).
     */
    @OptIn(ExperimentalResourceApi::class)
    suspend fun cacheSpriteAndGlyphs(): String =
        withContext(NonCancellable) {
            try {
                val files =
                    Res
                        .readBytes(FileSystem.STYLE_LISTING)
                        .decodeToString()
                        .lines()
                        .filter { it.isNotBlank() }

                // Use parallel processing for file caching
                coroutineScope {
                    files
                        .map { file ->
                            async(Dispatchers.Default) {
                                cacheDeepFile("${FileSystem.STYLE_FOLDER}/$file")
                            }
                        }.forEach { it.await() }
                }

                getCacheDir()
            } catch (e: Exception) {
                Log.e(::cacheSpriteAndGlyphs.name, "Error caching sprite and glyphs", e)
                throw e
            }
        }

    /**
     * Checks if a given position is within the event map's bounding box.
     *
     */
    suspend fun isPositionWithin(position: Position): Boolean =
        with(event.area.bbox()) {
            position.lat in sw.lat..ne.lat && position.lng in sw.lng..ne.lng
        }

    // ---------------------------

    override fun validationErrors(): List<String>? =
        mutableListOf<String>()
            .apply {
                when {
                    maxZoom.toString().toDoubleOrNull() == null || maxZoom <= 0 || maxZoom >= MAX_ZOOM_LIMIT ->
                        this.add("Map Maxzoom must be a positive double less than $MAX_ZOOM_LIMIT")

                    language.isEmpty() ->
                        this.add("Map language is empty")

                    !language.matches(Regex("^[a-z]{2,3}$")) ->
                        this.add("Map language must be a valid ISO-639 code")

                    zone.isEmpty() ->
                        this.add("Map Osmarea is empty")

                    !zone.matches(Regex("^[a-zA-Z0-9/-]+$")) ->
                        this.add("Map Osmarea must be a valid string composed of one or several strings separated by '/'")

                    else -> { /* No validation errors */ }
                }
            }.takeIf { it.isNotEmpty() }
            ?.map { "${WWWEventMap::class.simpleName}: $it" }
}
