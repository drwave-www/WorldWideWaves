package com.worldwidewaves.shared.events

/*
 * Copyright 2024 DrWave
 *
 * WorldWideWaves is an ephemeral mobile app designed to orchestrate human waves through cities and countries,
 * culminating in a global wave. The project aims to transcend physical and cultural boundaries, fostering unity,
 * community, and shared human experience by leveraging real-time coordination and location-based services.
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

import com.worldwidewaves.shared.WWWGlobals.Companion.FS_STYLE_FOLDER
import com.worldwidewaves.shared.WWWGlobals.Companion.FS_STYLE_LISTING
import com.worldwidewaves.shared.cacheDeepFile
import com.worldwidewaves.shared.cacheStringToFile
import com.worldwidewaves.shared.cachedFileExists
import com.worldwidewaves.shared.cachedFilePath
import com.worldwidewaves.shared.events.utils.DataValidator
import com.worldwidewaves.shared.events.utils.Log
import com.worldwidewaves.shared.events.utils.MapDataProvider
import com.worldwidewaves.shared.events.utils.Position
import com.worldwidewaves.shared.events.utils.convertPolygonsToGeoJson
import com.worldwidewaves.shared.generated.resources.Res
import com.worldwidewaves.shared.getCacheDir
import com.worldwidewaves.shared.getMapFileAbsolutePath
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

// ---------------------------

@Serializable
class WWWEventMap(

    val maxZoom: Double,
    val language: String,
    val zone: String

) : KoinComponent, DataValidator {

    private var _event: IWWWEvent? = null
    private var event: IWWWEvent
        get() = _event ?: throw IllegalStateException("Event not set")
        set(value) {
            _event = value
        }

    // ---------------------------

    private val mapDataProvider: MapDataProvider by inject()

    // ---------------------------

    fun setRelatedEvent(event: WWWEvent) {
        this.event = event
    }

    // ---------------------------

    private suspend fun getMbtilesFilePath(): String? =
        getMapFileAbsolutePath(event.id, "mbtiles")

    // ---------------------------

    /**
     * Retrieves the URI for the map style.
     *
     * This function generates a map style JSON file based on event data and caches it for reuse.
     * It retrieves MBTiles, GeoJSON, sprites, and glyphs, fills a template with the data,
     * and returns the URI of the cached style JSON.
     *
     */
    suspend fun getStyleUri(): String? {
        val mbtilesFilePath = getMbtilesFilePath() ?: return null
        val styleFilename = "style-${event.id}.json"
        if (cachedFileExists(styleFilename))
            return cachedFilePath(styleFilename)

        val geojsonFilePath = event.area.getGeoJsonFilePath() ?: return null
        val warmingGeoJsonFilePath = cacheStringToFile(
            "warming-${event.id}.geojson",
            convertPolygonsToGeoJson(event.wave.getWarmingPolygons())
        ).let { cachedFilePath(it) }

        val spriteAndGlyphsPath = cacheSpriteAndGlyphs()
        val newFileStr = mapDataProvider.geoMapStyleData()
            .replace("__MBTILES_URI__", "mbtiles:///$mbtilesFilePath")
            .replace("__GEOJSON_URI__", "file:///$geojsonFilePath")
            .replace("__GEOJSON_WARMING_URI__", "file:///$warmingGeoJsonFilePath")
            .replace("__GLYPHS_URI__", "file:///$spriteAndGlyphsPath/files/style/glyphs")
            .replace("__SPRITE_URI__", "file:///$spriteAndGlyphsPath/files/style/sprites")

        cacheStringToFile(styleFilename, newFileStr)
        return cachedFilePath(styleFilename)
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
    suspend fun cacheSpriteAndGlyphs(): String {
        return try {
            Res.readBytes(FS_STYLE_LISTING)
                .decodeToString()
                .lines()
                .filter { it.isNotBlank() }
                .forEach { cacheDeepFile("$FS_STYLE_FOLDER/$it") }
            getCacheDir()
        } catch (e: Exception) {
            Log.e(::cacheSpriteAndGlyphs.name,"Error caching sprite and glyphs", e)
            throw e
        }
    }

    /**
     * Checks if a given position is within the event area's bounding box.
     *
     */
    suspend fun isPositionWithin(position: Position): Boolean =
        with(event.area.getBoundingBox()) {
            position.lat in sw.lat..ne.lat && position.lng in sw.lng..ne.lng
        }

    // ---------------------------

    override fun validationErrors(): List<String>? = mutableListOf<String>()
        .apply {
            when {
                maxZoom.toString().toDoubleOrNull() == null || maxZoom <= 0 || maxZoom >= 20 ->
                    this.add("Map Maxzoom must be a positive double less than 20")

                language.isEmpty() ->
                    this.add("Map language is empty")

                !language.matches(Regex("^[a-z]{2,3}$")) ->
                    this.add("Map language must be a valid ISO-639 code")

                zone.isEmpty() ->
                    this.add("Map Osmarea is empty")

                !zone.matches(Regex("^[a-zA-Z0-9/-]+$")) ->
                    this.add("Map Osmarea must be a valid string composed of one or several strings separated by '/'")

                else -> { }
            }
        }.takeIf { it.isNotEmpty() }?.map { "wave: $it" }

}