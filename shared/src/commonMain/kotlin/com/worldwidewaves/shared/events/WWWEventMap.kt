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
import com.worldwidewaves.shared.cacheStringToFile
import com.worldwidewaves.shared.cachedFileExists
import com.worldwidewaves.shared.cachedFilePath
import com.worldwidewaves.shared.events.utils.convertPolygonsToGeoJson
import com.worldwidewaves.shared.generated.resources.Res
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

    fun getCenter(): Pair<Double, Double> {
        val coordinates = event.mapCenter.split(",").mapNotNull { it.toDoubleOrNull() }
        require(coordinates.size == 2) { "Invalid mapCenter format" }
        return Pair(coordinates[0], coordinates[1])
    }

    fun getBbox(): List<Double> {
        val coordinates = event.mapBbox.split(",").mapNotNull { it.toDoubleOrNull() }
        require(coordinates.size == 4) { "Invalid mapBbox format" }
        return coordinates
    }

    // ---------------------------q

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

        val newFileStr = mapDataProvider.geoMapStyleData()
            .replace("___FILE_URI___", "mbtiles:///$mbtilesFilePath")
            .replace("___GEOJSON_URI___", "file:///$geojsonFilePath")
            .replace("___GEOJSON_WARMING_URI___", "file:///$warmingGeoJsonFilePath")

        cacheStringToFile(styleFilename, newFileStr)
        return cachedFilePath(styleFilename)
    }

}