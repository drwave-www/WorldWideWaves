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

import com.worldwidewaves.shared.getEventImage
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

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
    val mapMinzoom: Double,
    val mapMaxzoom: Double,
    val mapDefaultzoom: Double? = null,
    val mapLanguage: String,
    val mapOsmarea: String,
    val mapWarmingZoneLongitude: Double,
    val timeZone: String
) {
    @Transient var map = WWWEventMap(this)
    @Transient var area = WWWEventArea(this)
    @Transient var wave = WWWEventWave(this)
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

private fun getEventImageByType(type: String, id: String?): Any? = id?.let { getEventImage(type, it) }

fun WWWEvent.getLocationImage(): Any? = getEventImageByType("location", this.id)
fun WWWEvent.getCommunityImage(): Any? = getEventImageByType("community", this.community)
fun WWWEvent.getCountryImage(): Any? = getEventImageByType("country", this.country)

// ---------------------------

fun WWWEvent.getTimeZone(): TimeZone = TimeZone.of(this.timeZone)

fun WWWEvent.getStartDateSimpleAsLocal(): String = runCatching {
    LocalDateTime.parse("${this.date}T${this.startHour}:00")
        .toInstant(getTimeZone())
        .toLocalDateTime(getTimeZone())
        .let { "${it.dayOfMonth.toString().padStart(2, '0')}/${it.monthNumber.toString().padStart(2, '0')}" }
}.getOrDefault("00/00")

fun WWWEvent.getStartDateTimeAsLocal(): LocalDateTime =
    LocalDateTime.parse("${date}T${startHour}")
        .toInstant(getTimeZone())
        .toLocalDateTime(getTimeZone())
