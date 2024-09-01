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
import com.worldwidewaves.shared.events.utils.MapDataProvider
import com.worldwidewaves.shared.events.utils.Position
import com.worldwidewaves.shared.events.utils.convertPolygonsToGeoJson
import com.worldwidewaves.shared.generated.resources.Res
import com.worldwidewaves.shared.getCacheDir
import com.worldwidewaves.shared.getMapFileAbsolutePath
import io.github.aakira.napier.Napier
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

// ---------------------------

class WWWEventMap(
    private val event: WWWEvent
) : KoinComponent {

    private val mapDataProvider: MapDataProvider by inject()

    // ---------------------------

    private suspend fun getMbtilesFilePath(): String? {
        return getMapFileAbsolutePath(event.id, "mbtiles")
    }

    // ---------------------------

    /**
     * Retrieves the URI for the map style.
     *
     * This function generates a map style JSON file based on event data and caches it for reuse.
     * It retrieves MBTiles, GeoJSON, sprites, and glyphs, fills a template with the data,
     * and returns the URI of the cached style JSON.
     *
     * @return The URI of the cached style JSON file, or null if an error occurs.
     */
    suspend fun getStyleUri(): String? {
        val mbtilesFilePath = getMbtilesFilePath() ?: return null

        val styleFilename = "style-${event.id}.json"
        if (cachedFileExists(styleFilename)) { // TODO: BUGFIX: for testing, better manage cache
            return cachedFilePath(styleFilename)
        }

        val geojsonFilePath = event.area.getGeoJsonFilePath() ?: return null

        val warmingGeoJsonFilename = "warming-${event.id}.geojson"
        val warmingPolygons = event.wave.getWarmingPolygons()
        val warmingGeoJson = convertPolygonsToGeoJson(warmingPolygons)

        cacheStringToFile(warmingGeoJsonFilename, warmingGeoJson)
        val warmingGeoJsonFilePath = cachedFilePath(warmingGeoJsonFilename)

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
     * @return The path to the cache directory containing the sprite and glyphs resources.
     * @throws Exception if an error occurs during caching.
     */
    @OptIn(ExperimentalResourceApi::class)
    suspend fun cacheSpriteAndGlyphs(): String { // TODO: use statics
        return try {
            val listingFilePath = FS_STYLE_LISTING
            val listingContent = Res.readBytes(listingFilePath).decodeToString()
            val fileNames = listingContent.lines().filter { it.isNotBlank() }

            fileNames.forEach { fileName ->
                cacheDeepFile("$FS_STYLE_FOLDER/$fileName")
            }

            getCacheDir()
        } catch (e: Exception) {
            Napier.e("Error caching sprite and glyphs", e)
            throw e
        }
    }

    /**
     * Checks if a given position is within the event area's bounding box.
     *
     * @param position The position to check.
     * @return True if the position is within the bounding box, false otherwise.
     */
    suspend fun isPositionWithin(position: Position): Boolean {
        return with(event.area.getBoundingBox()) {
            position.lat in sw.lat..ne.lat && position.lng in sw.lng..ne.lng
        }
    }

}