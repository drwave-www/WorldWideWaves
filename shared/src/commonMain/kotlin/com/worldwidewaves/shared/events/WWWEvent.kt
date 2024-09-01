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
    val wavedef: WaveDefinition,
    val description: String,
    val instagramAccount: String,
    val instagramHashtag: String,
    var favorite: Boolean = false,
    val mapOsmadminid: Int,
    val mapMaxzoom: Double,
    val mapLanguage: String,
    val mapOsmarea: String,
    val timeZone: String
) {
    enum class Status { DONE, SOON, RUNNING }

    @Serializable
    data class WaveDefinition(
        val linear: WWWEventWaveLinear? = null,
        val deep: WWWEventWaveDeep? = null,
        val linearSplit: WWWEventWaveLinearSplit? = null
    )

    // ---------------------------

    @Transient
    var map = WWWEventMap(this)

    @Transient
    var area = WWWEventArea(this)

    @Transient
    var wave = (wavedef.linear ?: wavedef.deep ?: wavedef.linearSplit
        ?: throw IllegalStateException("$id: No valid wave definition found")).apply {
            setEvent(this@WWWEvent)
        }

    // ---------------------------

    fun getStatus(): Status {
        return when {
            isDone() -> Status.DONE
            isSoon() -> Status.SOON
            isRunning() -> Status.RUNNING
            else -> throw IllegalStateException("Event status is undefined")
        }
    }

    fun isDone(): Boolean {
        return this.id == "paris_france" // TODO: test
    }

    fun isSoon(): Boolean {
        return this.id == "unitedstates" // TODO: test…
    }

    fun isRunning(): Boolean {
        return this.id == "riodejaneiro_brazil" // TODO: test…
    }

    // ---------------------------

    private fun getEventImageByType(type: String, id: String?): Any? = id?.let { getEventImage(type, it) }

    fun getLocationImage(): Any? = getEventImageByType("location", this.id)
    fun getCommunityImage(): Any? = getEventImageByType("community", this.community)
    fun getCountryImage(): Any? = getEventImageByType("country", this.country)

    // ---------------------------

    fun getTimeZone(): TimeZone {
        return TimeZone.of(this.timeZone)
    }

    /**
     * Converts the start date and time of the event to a simple local date format.
     *
     * This function parses the event's start date and time, converts it to the local time zone,
     * and formats it as a string in the "dd/MM" format. If the conversion fails, it returns "00/00".
     *
     * @return A string representing the start date in the "dd/MM" format, or "00/00" if the conversion fails.
     */
    fun getStartDateSimpleAsLocal(): String {
        return runCatching {
            LocalDateTime.parse("${this.date}T${this.startHour}:00")
                .toInstant(getTimeZone())
                .toLocalDateTime(getTimeZone())
                .let {
                    "${it.dayOfMonth.toString().padStart(2, '0')}/${
                        it.monthNumber.toString().padStart(2, '0')
                    }"
                }
        }.getOrDefault("00/00")
    }

    /**
     * Converts the start date and time of the event to a local `LocalDateTime`.
     *
     * This function parses the event's date and start hour, converts it to an `Instant` using the event's time zone,
     * and then converts it to a `LocalDateTime` in the same time zone.
     *
     * @return A `LocalDateTime` representing the start date and time of the event in the local time zone.
     */
    fun getStartDateTime(): LocalDateTime {
        return LocalDateTime.parse("${date}T${startHour}")
            .toInstant(getTimeZone())
            .toLocalDateTime(getTimeZone())
    }

}


