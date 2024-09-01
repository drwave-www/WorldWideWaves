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

import com.worldwidewaves.shared.events.utils.DataValidator
import com.worldwidewaves.shared.getEventImage
import io.github.aakira.napier.Napier
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
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
) : DataValidator {

    enum class Status { DONE, SOON, RUNNING }

    @Serializable
    data class WaveDefinition(
        val linear: WWWEventWaveLinear? = null,
        val deep: WWWEventWaveDeep? = null,
        val linearSplit: WWWEventWaveLinearSplit? = null
    ) : DataValidator {
        override fun isValid(): Pair<Boolean, String?> =
            when {
                linear == null && deep == null && linearSplit == null ->
                    Pair(false, "event should contain one and only one wave definition")
                listOfNotNull(linear, deep, linearSplit).size != 1 ->
                    Pair(false, "only one of linear, deep, or linearSplit should be non-null")
                else -> (linear ?: deep ?: linearSplit)!!.isValid()
            }
    }

    // ---------------------------

    @Transient private var _map: WWWEventMap? = null
    val map: WWWEventMap get() = _map ?: WWWEventMap(this).apply { _map = this }

    @Transient private var _area: WWWEventArea? = null
    val area: WWWEventArea get() = _area ?: WWWEventArea(this).apply { _area = this }

    @Transient private var _wave: WWWEventWave? = null
    val wave: WWWEventWave
        get() = _wave ?: (wavedef.linear ?: wavedef.deep ?: wavedef.linearSplit
        ?: throw IllegalStateException("$id: No valid wave definition found")).apply {
            setEvent(this@WWWEvent)
            _wave = this
        }

    // ---------------------------

    fun getStatus(): Status {
        return when {
            isDone() -> Status.DONE
            isSoon() -> Status.SOON
            isRunning() -> Status.RUNNING
            else -> throw IllegalStateException("$id : Event status is undefined")
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

    fun getTZ(): TimeZone = TimeZone.of(this.timeZone)

    /**
     * Converts the start date and time of the event to a simple local date format.
     *
     * This function parses the event's start date and time, converts it to the local time zone,
     * and formats it as a string in the "dd/MM" format. If the conversion fails, it returns "00/00".
     *
     * @return A string representing the start date in the "dd/MM" format, or "00/00" if the conversion fails.
     */
    fun getLiteralStartDateSimple(): String = getStartDateTime().let {
        "${it.dayOfMonth.toString().padStart(2, '0')}/${
            it.monthNumber.toString().padStart(2, '0')
        }"
    }

    /**
     * Converts the start date and time of the event to a local `LocalDateTime`.
     *
     * This function parses the event's date and start hour, converts it to an `Instant` using the event's time zone,
     * and then converts it to a `LocalDateTime` in the same time zone.
     *
     * @return A `LocalDateTime` representing the start date and time of the event in the local time zone.
     */
    fun getStartDateTime(): LocalDateTime = runCatching {
        LocalDateTime.parse("${date}T${startHour}")
            .toInstant(getTZ())
            .toLocalDateTime(getTZ())
    }.getOrElse {
        Napier.e("$id: Error parsing start date and time: $it")
        LocalDateTime(0, 1, 1, 0, 0)
    }

    /**
     * This function checks the event for various validation criteria.
     */
    override fun isValid() : Pair<Boolean, String?> = when {
        id.isEmpty() ->
            Pair(false, "ID is empty")
        !id.matches(Regex("^[a-z_]+$")) ->
            Pair(false, "ID must be lowercase with only simple letters or underscores")
        type.isEmpty() ->
            Pair(false, "Type is empty")
        type !in listOf("city", "country", "world") ->
            Pair(false, "Type must be either 'city', 'country', or 'world'")
        location.isEmpty() ->
            Pair(false, "Location is empty")
        type == "city" && country.isNullOrEmpty() ->
            Pair(false, "Country must be specified for type 'city'")
        !date.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) || runCatching { LocalDate.parse(date) }.isFailure ->
            Pair(false, "Date format is invalid or date is not valid")
        !startHour.matches(Regex("\\d{2}:\\d{2}")) || runCatching { LocalTime.parse(startHour) }.isFailure ->
            Pair(false, "Start hour format is invalid or time is not valid")
        description.isEmpty() ->
            Pair(false, "Description is empty")
        instagramAccount.isEmpty() ->
            Pair(false, "Instagram account is empty")
        !instagramAccount.matches(Regex("^[A-Za-z0-9_.]+$")) ->
            Pair(false, "Instagram account is invalid")
        instagramHashtag.isEmpty() ->
            Pair(false, "Instagram hashtag is empty")
        !instagramHashtag.matches(Regex("^#[A-Za-z0-9_]+$")) ->
            Pair(false, "Instagram hashtag is invalid")
        mapOsmadminid.toString().toIntOrNull() == null ->
            Pair(false, "Map Osmadminid must be an integer")
        mapMaxzoom.toString().toDoubleOrNull() == null || mapMaxzoom <= 0 || mapMaxzoom >= 20 ->
            Pair(false, "Map Maxzoom must be a positive double less than 20")
        mapLanguage.isEmpty() ->
            Pair(false, "Map language is empty")
        !mapLanguage.matches(Regex("^[a-z]{2,3}$")) ->
            Pair(false, "Map language must be a valid ISO-639 code")
        mapOsmarea.isEmpty() ->
            Pair(false, "Map Osmarea is empty")
        !mapOsmarea.matches(Regex("^[a-zA-Z0-9/-]+$")) ->
            Pair(false, "Map Osmarea must be a valid string composed of one or several strings separated by '/'")
        timeZone.isEmpty() ->
            Pair(false, "Time zone is empty")
        runCatching { TimeZone.of(timeZone) }.isFailure ->
            Pair(false, "Time zone is invalid")
        !wavedef.isValid().first -> Pair(false, "Wave definition is invalid")
        else -> wavedef.isValid()
    }

}


