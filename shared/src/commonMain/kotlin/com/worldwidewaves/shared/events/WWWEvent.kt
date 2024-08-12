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

import com.worldwidewaves.shared.cacheStringToFile
import com.worldwidewaves.shared.cachedFilePath
import com.worldwidewaves.shared.generated.resources.Res
import com.worldwidewaves.shared.getEventImage
import com.worldwidewaves.shared.getMapFileAbsolutePath
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.ExperimentalResourceApi

// ---------------------------

@Serializable
data class WWWEvent(
    val id: String,
    val type: String,
    val location: String,
    val country: String? = null,
    val community: String? = null,
    val date: String,
    val startHour: String,
    val speed: Int,
    val description: String,
    val instagramAccount: String,
    val instagramUrl: String,
    val instagramHashtag: String,
    var favorite: Boolean = false,
    val mapBbox: String,
    val mapCenter: String,
    val mapOsmadminid: Int,
    val mapMinzoom: Int,
    val mapMaxzoom: Int,
    val mapDefaultzoom: Double? = null,
    val mapLanguage: String,
    val mapOsmarea: String
)

// ---------------------------

fun WWWEvent.getMapCenter(): Pair<Double, Double> {
    val (lat, lng) = this.mapCenter.split(",").map { it.toDouble() }
    return Pair(lat, lng)
}

fun WWWEvent.getMapBbox(): List<Double> {
    // swLng, swLat, neLng, neLat
    return this.mapBbox.split(",").map { it.toDouble() }
}

// ---------------------------

fun WWWEvent.isDone(): Boolean {
    return this.id == "paris_france" // TODO: test
}

fun WWWEvent.isSoon(): Boolean {
    return this.id == "unitedstates" // TODO: test…
}

fun WWWEvent.isRunning(): Boolean {
    return this.id == "riodejaneiro_brazil" // TODO: test…
}

// ---------------------------

fun WWWEvent.getLocationImage(): Any? = getEventImage("location", this.id)
fun WWWEvent.getCommunityImage(): Any? = this.community?.let { getEventImage("community", it) }
fun WWWEvent.getCountryImage(): Any? = this.country?.let { getEventImage("country", it) }

fun WWWEvent.getFormattedSimpleDate(): String {
    return runCatching {
        Instant.parse("${this.date}T00:00:00Z")
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .let { dateTime ->
                "${dateTime.dayOfMonth.toString().padStart(2, '0')}/${dateTime.monthNumber.toString().padStart(2, '0')}"
            }
    }.getOrDefault("00/00")
}

// ---------------------------

@OptIn(ExperimentalResourceApi::class)
suspend fun WWWEvent.getMapStyleUri(): String? {
    val mbtilesFilePath = getMapFileAbsolutePath(this.id, "mbtiles") ?: return null
    val geojsonFilePath = getMapFileAbsolutePath(this.id, "geojson") ?: return null
    val styleFilename = "style-${this.id}.json"

    //if (cachedFileExists(styleFilename)) { // TODO: better manage cache
    //    return cachedFileUri(styleFilename)
    //}

    // TODO : generate the start area polygon from the geojson file, see below for code

    val newFileStr = Res.readBytes("files/maps/mapstyle.json")
        .decodeToString()
        .replace("___FILE_URI___", "mbtiles:///$mbtilesFilePath")
        .replace("___GEOJSON_URI___", "file:///$geojsonFilePath")

    cacheStringToFile(styleFilename, newFileStr)

    return cachedFilePath(styleFilename)
}

// ---------------------------

fun WWWEvent.getLiteralSpeed(): String {
    return "12 m/s"
}

fun WWWEvent.getLiteralStartTime(): String {
    return "14:00 BRT"
}

fun WWWEvent.getLiteralEndTime(): String {
    return "15:23 BRT"
}

fun WWWEvent.getLiteralTotalTime(): String {
    return "83 min"
}

fun WWWEvent.getLiteralProgression(): String {
    return "49.23%"
}


//import kotlinx.serialization.json.Json
//import kotlinx.serialization.json.jsonObject
//import kotlinx.serialization.json.jsonPrimitive
//import kotlinx.serialization.json.jsonArray
//import kotlinx.serialization.json.double
//import kotlinx.serialization.json.decodeFromJsonElement
//import kotlinx.serialization.Serializable
//import java.io.File

//@Serializable
//data class GeoJsonPolygon(val type: String, val coordinates: List<List<List<Double>>>)
//
//fun parseGeoJson(filePath: String): GeoJsonPolygon {
//    val jsonString = File(filePath).readText()
//    val jsonElement = Json.parseToJsonElement(jsonString)
//    return Json.decodeFromJsonElement(jsonElement.jsonObject["geometry"]!!)
//}
//
//fun splitPolygonByLongitude(polygon: GeoJsonPolygon, longitude: Double): List<List<List<Double>>> {
//    val coordinates = polygon.coordinates[0]
//    val leftSide = mutableListOf<List<Double>>()
//    val rightSide = mutableListOf<List<Double>>()
//    var isLeft = true
//
//    for (i in coordinates.indices) {
//        val point = coordinates[i]
//        if (point[0] < longitude) {
//            if (!isLeft) {
//                isLeft = true
//                leftSide.add(listOf(longitude, point[1]))
//                rightSide.add(listOf(longitude, point[1]))
//            }
//            leftSide.add(point)
//        } else {
//            if (isLeft) {
//                isLeft = false
//                leftSide.add(listOf(longitude, point[1]))
//                rightSide.add(listOf(longitude, point[1]))
//            }
//            rightSide.add(point)
//        }
//    }
//
//    return listOf(leftSide, rightSide)
//}
//
//fun main() {
//    val geoJsonFilePath = "path/to/your/geojson/file.geojson"
//    val specifiedLongitude = 2.3522 // Example longitude for Paris
//
//    val polygon = parseGeoJson(geoJsonFilePath)
//    val (leftPolygon, rightPolygon) = splitPolygonByLongitude(polygon, specifiedLongitude)
//
//    println("Left Polygon: $leftPolygon")
//    println("Right Polygon: $rightPolygon")
//}