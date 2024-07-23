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
package com.worldwidewaves.shared.events

import com.worldwidewaves.shared.WWWPlatform
import com.worldwidewaves.shared.getImage
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable

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
    var favorite: Boolean = false
)

suspend fun WWWEvent.initFavoriteStatus() {
    this.favorite = WWWPlatform.favoriteEventsStore.isFavorite(eventId = this.id)
}

suspend fun WWWEvent.setFavorite(favorite: Boolean): WWWEvent {
    this.favorite = favorite
    WWWPlatform.favoriteEventsStore.setFavoriteStatus(this.id, favorite)
    return this
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

fun WWWEvent.isFavorite(): Boolean {
    return this.favorite || this.id == "paris_france" // TODO: test…
}

// ---------------------------

fun WWWEvent.getLocationImage(): Any? = getImage("location", this.id)
fun WWWEvent.getCommunityImage(): Any? = this.community?.let { getImage("community", it) }
fun WWWEvent.getCountryImage(): Any? = this.country?.let { getImage("country", it) }

fun WWWEvent.getFormattedSimpleDate(): String {
    return try {
        val instant = Instant.parse(this.date + "T00:00:00Z") // Assuming date is in "yyyy-MM-dd" format
        val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        "${dateTime.dayOfMonth.toString().padStart(2, '0')}/${dateTime.monthNumber.toString().padStart(2, '0')}"
    } catch (e: Exception) {
        "00/00"
    }
}
