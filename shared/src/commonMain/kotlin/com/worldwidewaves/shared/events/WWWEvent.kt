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

import com.worldwidewaves.shared.events.IWWWEvent.Status
import com.worldwidewaves.shared.events.utils.DataValidator
import com.worldwidewaves.shared.events.utils.IClock
import com.worldwidewaves.shared.events.utils.Log
import com.worldwidewaves.shared.getEventImage
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.time.Duration.Companion.days

// ---------------------------

@Serializable
data class WWWEvent(

    override val id: String,
    override val type: String,
    override val location: String,
    override val country: String? = null,
    override val community: String? = null,

    override val timeZone: String,
    override val date: String,
    override val startHour: String,

    override val description: String,
    override  val instagramAccount: String,
    override val instagramHashtag: String,

    override val wavedef: WWWWaveDefinition,
    override val area: WWWEventArea,
    override val map: WWWEventMap,

    override var favorite: Boolean = false

) : IWWWEvent, DataValidator, KoinComponent {

    @Serializable
    data class WWWWaveDefinition(
        val linear: WWWEventWaveLinear? = null,
        val deep: WWWEventWaveDeep? = null,
        val linearSplit: WWWEventWaveLinearSplit? = null
    ) : DataValidator {
        override fun validationErrors(): List<String>? = mutableListOf<String>().apply {
            when {
                linear == null && deep == null && linearSplit == null ->
                    this.add("event should contain one and only one wave definition")

                listOfNotNull(linear, deep, linearSplit).size != 1 ->
                    this.add("only one of linear, deep, or linearSplit should be non-null")

                else -> (linear ?: deep ?: linearSplit)!!.validationErrors()?.let { addAll(it) }
            }
        }.takeIf { it.isNotEmpty() }?.map { "${WWWWaveDefinition::class.simpleName}: $it" }
    }

    // ---------------------------

    private val clock: IClock by inject()

    // ---------------------------

    @Transient private var _wave: WWWEventWave? = null
    override val wave: WWWEventWave
        get() = _wave ?: (wavedef.linear ?: wavedef.deep ?: wavedef.linearSplit
        ?: throw IllegalStateException("$id: No valid wave definition found")).apply {
            setRelatedEvent<WWWEventWave>(this@WWWEvent)
            _wave = this
        }

    init {
        map.setRelatedEvent(this)
        area.setRelatedEvent(this)
    }

    // ---------------------------

    override suspend fun getStatus(): Status {
        return when {
            isDone() -> Status.DONE
            isSoon() -> Status.SOON
            isRunning() -> Status.RUNNING
            else -> Status.NEXT
        }
    }

    override suspend fun isDone(): Boolean {
        val endDateTime = this.wave.getEndTime()
        return endDateTime < clock.now()
    }

    override fun isSoon(): Boolean {
        val eventDateTime = getStartDateTime()
        val now = clock.now()
        return eventDateTime > now && eventDateTime <= now.plus(30.days)
    }

    override suspend fun isRunning(): Boolean {
        val startDateTime = getStartDateTime()
        val endDateTime = this.wave.getEndTime()
        val now = clock.now()
        return startDateTime <= now && endDateTime > now
    }

    // ---------------------------

    private fun getEventImageByType(type: String, id: String?): Any? = id?.let { getEventImage(type, it) }

    override fun getLocationImage(): Any? = getEventImageByType("location", this.id)
    override fun getCommunityImage(): Any? = getEventImageByType("community", this.community)
    override fun getCountryImage(): Any? = getEventImageByType("country", this.country)

    // ---------------------------

    override fun getTZ(): TimeZone = TimeZone.of(this.timeZone)

    /**
     * Converts the start date and time of the event to a simple local date format.
     *
     * This function parses the event's start date and time, converts it to the local time zone,
     * and formats it as a string in the "dd/MM" format. If the conversion fails, it returns "00/00".
     *
     */
    override fun getLiteralStartDateSimple(): String = try {
        getStartDateTime().let {
            "${it.toLocalDateTime(getTZ()).dayOfMonth.toString().padStart(2, '0')}/${
                it.toLocalDateTime(getTZ()).monthNumber.toString().padStart(2, '0')
            }"
        }
    } catch (e: Exception) {
        "error"
    }

    /**
     * Converts the start date and time of the event to a local `LocalDateTime`.
     *
     * This function parses the event's date and start hour, converts it to an `Instant` using the event's time zone,
     * and then converts it to a `LocalDateTime` in the same time zone.
     *
     */
    override fun getStartDateTime(): Instant = runCatching {
        val localDateTime = LocalDateTime.parse("${date}T${startHour}:00")
        localDateTime.toInstant(getTZ())
    }.getOrElse {
        Log.e(::getStartDateTime.name, "$id: Error parsing start date and time: $it")
        throw IllegalStateException("$id: Error parsing start date and time")
    }

    /**
     * This function checks the event for various validation criteria.
     */
    override fun validationErrors() : List<String>? = mutableListOf<String>()
        .apply {
            when {
                id.isEmpty() ->
                    this.add("ID is empty")

                !id.matches(Regex("^[a-z_]+$")) ->
                    this.add("ID must be lowercase with only simple letters or underscores")

                type.isEmpty() ->
                    this.add("Type is empty")

                type !in listOf("city", "country", "world") ->
                    this.add("Type must be either 'city', 'country', or 'world'")

                location.isEmpty() ->
                    this.add("Location is empty")

                type == "city" && country.isNullOrEmpty() ->
                    this.add("Country must be specified for type 'city'")

                timeZone.isEmpty() ->
                    this.add("Time zone is empty")

                !date.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) || runCatching { LocalDate.parse(date) }.isFailure ->
                    this.add("Date format is invalid or date is not valid")

                !startHour.matches(Regex("\\d{2}:\\d{2}")) || runCatching { LocalTime.parse(startHour) }.isFailure ->
                    this.add("Start hour format is invalid or time is not valid")

                description.isEmpty() ->
                    this.add("Description is empty")

                instagramAccount.isEmpty() ->
                    this.add("Instagram account is empty")

                !instagramAccount.matches(Regex("^[A-Za-z0-9_.]+$")) ->
                    this.add("Instagram account is invalid")

                instagramHashtag.isEmpty() ->
                    this.add("Instagram hashtag is empty")

                !instagramHashtag.matches(Regex("^#[A-Za-z0-9_]+$")) ->
                    this.add("Instagram hashtag is invalid")

                runCatching { TimeZone.of(timeZone) }.isFailure ->
                    this.add("Time zone is invalid")

                else -> wavedef.validationErrors()?.let { addAll(it) }
                    .also { area.validationErrors()?.let { addAll(it) } }
                    .also { map.validationErrors()?.let { addAll(it) } }
            }
        }.takeIf { it.isNotEmpty() }?.map { "${WWWEvent::class.simpleName}: $it" }

}


