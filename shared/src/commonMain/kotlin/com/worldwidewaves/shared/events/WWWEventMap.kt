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
import com.worldwidewaves.shared.data.updateCacheMetadata
import com.worldwidewaves.shared.events.data.MapDataProvider
import com.worldwidewaves.shared.events.utils.DataValidator
import com.worldwidewaves.shared.events.utils.Position
import com.worldwidewaves.shared.generated.resources.Res
import com.worldwidewaves.shared.utils.Log
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

    // Cache for style URI to avoid redundant file I/O
    @Transient
    private var cachedStyleUri: String? = null

    // ---------------------------

    fun setRelatedEvent(event: WWWEvent) {
        this.event = event
        // Clear cache when event changes
        cachedStyleUri = null
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
     * **Performance**: Result is cached in memory to avoid redundant file I/O on repeated calls.
     */
    suspend fun getStyleUri(): String? {
        // Return cached URI if available AND file still exists
        cachedStyleUri?.let { cached ->
            val styleFilename = "style-${event.id}.json"
            if (cachedFileExists(styleFilename) && !isCachedFileStale(styleFilename)) {
                Log.d("WWWEventMap", "getStyleUri: Using cached style URI for event ${event.id}")
                return cached
            } else {
                // Cache invalid - file was deleted or became stale
                Log.d("WWWEventMap", "getStyleUri: Cached URI invalid, regenerating for event ${event.id}")
                cachedStyleUri = null
            }
        }

        Log.d("WWWEventMap", "getStyleUri() called for event: ${event.id}")

        val mbtilesFilePath = getMbtilesFilePath()
        if (mbtilesFilePath == null) {
            Log.w("WWWEventMap", "getStyleUri: MBTiles file path is null for event ${event.id}")
            return null
        }
        Log.i("WWWEventMap", "getStyleUri: MBTiles path = $mbtilesFilePath")

        val styleFilename = "style-${event.id}.json"
        val isCacheValid = cachedFileExists(styleFilename) && !isCachedFileStale(styleFilename)
        if (isCacheValid) {
            val cachedPath = cachedFilePath(styleFilename)
            cachedStyleUri = cachedPath // Cache in memory
            Log.i("WWWEventMap", "getStyleUri: Using cached style file: $cachedPath")
            return cachedPath
        }
        Log.d("WWWEventMap", "getStyleUri: Cache invalid or missing, generating new style file")

        val geojsonFilePath = event.area.getGeoJsonFilePath()
        if (geojsonFilePath == null) {
            Log.w("WWWEventMap", "getStyleUri: GeoJSON file path is null for event ${event.id}")
            return null
        }
        Log.i("WWWEventMap", "getStyleUri: GeoJSON path = $geojsonFilePath")

        val spriteAndGlyphsPath = cacheSpriteAndGlyphs()
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
        updateCacheMetadata(styleFilename)
        Log.d("WWWEventMap", "getStyleUri: Style file cached at: $cachedPath")

        // Cache the URI in memory for subsequent calls
        cachedStyleUri = cachedPath

        Log.i("WWWEventMap", "getStyleUri: Returning style path: $cachedPath")
        return cachedPath
    }

    // ---------------------------

    /**
     * Caches sprite and glyphs resources required for the map style.
     *
     * This function reads a file listing the required resources, caches them individually,
     * and returns the path to the cache directory.
     *
     */
    @OptIn(ExperimentalResourceApi::class)
    suspend fun cacheSpriteAndGlyphs(): String =
        try {
            Res
                .readBytes(FileSystem.STYLE_LISTING)
                .decodeToString()
                .lines()
                .filter { it.isNotBlank() }
                .forEach { cacheDeepFile("${FileSystem.STYLE_FOLDER}/$it") }
            getCacheDir()
        } catch (e: Exception) {
            Log.e(::cacheSpriteAndGlyphs.name, "Error caching sprite and glyphs", e)
            throw e
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
