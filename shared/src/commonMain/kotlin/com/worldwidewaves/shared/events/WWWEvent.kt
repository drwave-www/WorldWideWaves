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
import com.worldwidewaves.shared.getMBTilesAbsoluteFilePath
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
    return try {
        val instant =
            Instant.parse(this.date + "T00:00:00Z") // Assuming date is in "yyyy-MM-dd" format
        val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        "${dateTime.dayOfMonth.toString().padStart(2, '0')}/${
            dateTime.monthNumber.toString().padStart(2, '0')
        }"
    } catch (e: Exception) {
        "00/00"
    }
}

// ---------------------------

@OptIn(ExperimentalResourceApi::class)
suspend fun WWWEvent.getMapStyleUri(): String? {
    val mbtilesFilePath = getMBTilesAbsoluteFilePath(this.id) ?: return null
    val styleFilename = "style-${this.id}.json"

    //if (cachedFileExists(styleFilename)) { // TODO: better manage cache
    //    return cachedFileUri(styleFilename)
    //}

    val styleJsonBytes: ByteArray = Res.readBytes("files/maps/mapstyle.json")
    var newFileStr = styleJsonBytes.decodeToString()
    newFileStr = newFileStr.replace(
        "___FILE_URI___",
        "mbtiles:///$mbtilesFilePath"
    )
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
