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

import com.worldwidewaves.shared.WWWGlobals.Companion.FS_MAPS_STYLE
import com.worldwidewaves.shared.WWWGlobals.Companion.FS_STYLE_FOLDER
import com.worldwidewaves.shared.WWWGlobals.Companion.FS_STYLE_LISTING
import com.worldwidewaves.shared.cacheDeepFile
import com.worldwidewaves.shared.cacheStringToFile
import com.worldwidewaves.shared.cachedFileExists
import com.worldwidewaves.shared.cachedFilePath
import com.worldwidewaves.shared.events.utils.convertPolygonsToGeoJson
import com.worldwidewaves.shared.generated.resources.Res
import com.worldwidewaves.shared.getCacheDir
import com.worldwidewaves.shared.getMapFileAbsolutePath
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.ExperimentalResourceApi

// ---------------------------

interface MapDataProvider {
    suspend fun geoMapStyleData(): String
}

class DefaultMapDataProvider : MapDataProvider {
    @OptIn(ExperimentalResourceApi::class)
    override suspend fun geoMapStyleData(): String {
        return withContext(Dispatchers.IO) {
            Napier.i("Loading map style data from $FS_MAPS_STYLE")
            Res.readBytes(FS_MAPS_STYLE).decodeToString()
        }
    }
}

// ---------------------------

class WWWEventMap(
    private val event: WWWEvent,
    private val mapDataProvider: MapDataProvider = DefaultMapDataProvider()
) {

    private suspend fun getMbtilesFilePath(): String? {
        return getMapFileAbsolutePath(event.id, "mbtiles")
    }

    // ---------------------------

    suspend fun getStyleUri(): String? {
        val mbtilesFilePath = getMbtilesFilePath() ?: return null

        val styleFilename = "style-${event.id}.json"
        if (cachedFileExists(styleFilename)) { // TODO: BUGFIX: for testing, better manage cache
            return cachedFilePath(styleFilename)
        }

        val geojsonFilePath = event.area.getGeoJsonFilePath() ?: return null

        val warmingGeoJsonFilename = "warming-${event.id}.geojson"
        val warmingPolygons = event.area.getWarmingPolygons()
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

}