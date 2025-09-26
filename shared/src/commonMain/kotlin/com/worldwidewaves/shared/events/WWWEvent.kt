package com.worldwidewaves.shared.events

/*
 * Copyright 2025 DrWave
 *
 * WorldWideWaves is an ephemeral mobile app designed to orchestrate human waves through cities and
 * countries. The project aims to transcend physical and cultural
 * boundaries, fostering unity, community, and shared human experience by leveraging real-time
 * coordination and location-based services.
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

import androidx.annotation.VisibleForTesting
import com.worldwidewaves.shared.WWWGlobals.WaveTiming
import com.worldwidewaves.shared.events.IWWWEvent.Status
import com.worldwidewaves.shared.events.IWWWEvent.WaveNumbersLiterals
import com.worldwidewaves.shared.events.utils.DataValidator
import com.worldwidewaves.shared.events.utils.IClock
import com.worldwidewaves.shared.getCommunityText
import com.worldwidewaves.shared.getCountryText
import com.worldwidewaves.shared.getEventImage
import com.worldwidewaves.shared.getEventText
import com.worldwidewaves.shared.utils.Log
import dev.icerock.moko.resources.StringResource
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.offsetAt
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

// ---------------------------

@OptIn(ExperimentalTime::class)
@Serializable
data class WWWEvent(
    override val id: String,
    override val type: String,
    override val country: String? = null,
    override val community: String? = null,
    override val timeZone: String,
    override val date: String,
    override val startHour: String,
    override val instagramAccount: String,
    override val instagramHashtag: String,
    override val wavedef: WWWWaveDefinition,
    override val area: WWWEventArea,
    override val map: WWWEventMap,
    override var favorite: Boolean = false,
) : IWWWEvent,
    DataValidator,
    KoinComponent {
    @Serializable
    data class WWWWaveDefinition(
        val linear: WWWEventWaveLinear? = null,
        val deep: WWWEventWaveDeep? = null,
        val linearSplit: WWWEventWaveLinearSplit? = null,
    ) : DataValidator {
        override fun validationErrors(): List<String>? =
            mutableListOf<String>()
                .apply {
                    when {
                        linear == null && deep == null && linearSplit == null ->
                            this.add("event should contain one and only one wave definition")

                        listOfNotNull(linear, deep, linearSplit).size != 1 ->
                            this.add("only one of linear, deep, or linearSplit should be non-null")

                        else -> (linear ?: deep ?: linearSplit)!!.validationErrors()?.let { addAll(it) }
                    }
                }.takeIf { it.isNotEmpty() }
                ?.map { "${WWWWaveDefinition::class.simpleName}: $it" }
    }

    // ---------------------------

    private val clock: IClock by inject()

    // ---------------------------

    @Transient private var _wave: WWWEventWave? = null
    override val wave: WWWEventWave
        get() =
            _wave ?: requireNotNull(wavedef.linear ?: wavedef.deep ?: wavedef.linearSplit) {
                "$id: No valid wave definition found"
            }.apply {
                setRelatedEvent<WWWEventWave>(this@WWWEvent)
                _wave = this
            }

    init {
        map.setRelatedEvent(this)
        area.setRelatedEvent(this)
    }

    @Transient override val warming = WWWEventWaveWarming(this)

    // ---------------------------

    @Transient var cachedObserver: WWWEventObserver? = null

    override fun getEventObserver(): WWWEventObserver = cachedObserver ?: WWWEventObserver(this).also { cachedObserver = it }

    // ---------------------------

    override suspend fun getStatus(): Status =
        when {
            isDone() -> Status.DONE
            isSoon() -> Status.SOON
            isRunning() -> Status.RUNNING
            else -> Status.NEXT
        }

    override suspend fun isDone(): Boolean {
        val endDateTime = getEndDateTime()
        return endDateTime < clock.now()
    }

    override fun isSoon(): Boolean {
        val eventDateTime = getStartDateTime()
        val now = clock.now()
        return eventDateTime > now && eventDateTime <= now.plus(WaveTiming.SOON_DELAY)
    }

    override suspend fun isRunning(): Boolean {
        val startDateTime = getStartDateTime()
        val endDateTime = getEndDateTime()
        val now = clock.now()
        return startDateTime <= now && endDateTime > now
    }

    // ---------------------------

    private fun getEventImageByType(
        type: String,
        id: String?,
    ): Any? = id?.let { getEventImage(type, it) }

    override fun getLocationImage(): Any? = getEventImageByType("location", this.id)

    override fun getCommunityImage(): Any? = getEventImageByType("community", this.community)

    override fun getCountryImage(): Any? = getEventImageByType("country", this.country)

    override fun getMapImage(): Any? = getEventImageByType("map", this.id)

    // ---------------------------

    override fun getLocation(): StringResource = getEventText("location", this.id)

    override fun getDescription(): StringResource = getEventText("description", this.id)

    override fun getLiteralCountry(): StringResource = getCountryText(this.country)

    override fun getLiteralCommunity(): StringResource = getCommunityText(this.community)

    // ---------------------------

    override fun getTZ(): TimeZone = TimeZone.of(this.timeZone)

    override fun getStartDateTime(): Instant =
        try {
            val localDateTime = LocalDateTime.parse("${date}T$startHour:00")
            localDateTime.toInstant(getTZ())
        } catch (e: Exception) {
            Log.e(::getStartDateTime.name, "$id: Error parsing start date and time: $e")
            throw IllegalStateException("$id: Error parsing start date and time")
        }

    override suspend fun getTotalTime(): Duration {
        var waveDuration = wave.getWaveDuration()

        if (waveDuration == 0.seconds) {
            // If GeoJson has not been yet loaded we do not have the polygons
            waveDuration = wave.getApproxDuration()
        }

        return getWarmingDuration() + WaveTiming.WARN_BEFORE_HIT + waveDuration
    }

    override suspend fun getEndDateTime(): Instant = getStartDateTime().plus(getTotalTime())

    // ------------------------------------------------------------------------

    override fun getLiteralTimezone(): String {
        val hoursOffset = getTZ().offsetAt(clock.now()).totalSeconds / 3600
        return when {
            hoursOffset == 0 -> "UTC"
            hoursOffset > 0 -> "UTC+$hoursOffset"
            else -> "UTC$hoursOffset"
        }
    }

    override fun getLiteralStartDateSimple(): String =
        try {
            getStartDateTime().let {
                "${it.toLocalDateTime(getTZ()).day.toString().padStart(2, '0')}/${
                    it.toLocalDateTime(getTZ()).month.number .toString().padStart(2, '0')
                }"
            }
        } catch (_: Exception) {
            "00/00"
        }

    override fun getLiteralStartTime(): String = IClock.instantToLiteral(getStartDateTime(), getTZ())

    @VisibleForTesting
    override suspend fun getLiteralEndTime(): String = IClock.instantToLiteral(getEndDateTime(), getTZ())

    // -----------------------------------------------------------------------

    override suspend fun getLiteralTotalTime(): String = "${getTotalTime().inWholeMinutes} min"

    // -----------------------------------------------------------------------

    override fun getWaveStartDateTime(): Instant = getStartDateTime() + getWarmingDuration() + WaveTiming.WARN_BEFORE_HIT

    override fun getWarmingDuration(): Duration = warming.getWarmingDuration()

    override fun isNearTime(): Boolean {
        val now = clock.now()
        val eventStartTime: Instant = getStartDateTime()
        val durationUntilEvent = eventStartTime - now
        return durationUntilEvent <= WaveTiming.OBSERVE_DELAY
    }

    // -----------------------------------------------------------------------

    /**
     * Retrieves all wave-related numbers for the event.
     *
     * This function gathers various wave-related metrics such as speed, start time, end time,
     * total time, and progression. It constructs a `WaveNumbers` object containing these metrics.
     *
     */
    override suspend fun getAllNumbers(): WaveNumbersLiterals {
        suspend fun safeCall(block: suspend () -> String): String =
            try {
                block()
            } catch (_: Throwable) {
                "error"
            }

        return WaveNumbersLiterals(
            waveTimezone = safeCall { getLiteralTimezone() },
            waveSpeed = safeCall { wave.getLiteralSpeed() },
            waveStartTime = safeCall { getLiteralStartTime() },
            waveEndTime = safeCall { getLiteralEndTime() },
            waveTotalTime = safeCall { getLiteralTotalTime() },
        )
    }

    // ---------------------------

    /**
     * This function checks the event for various validation criteria.
     */
    override fun validationErrors(): List<String>? =
        mutableListOf<String>()
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

                    type == "city" && country.isNullOrEmpty() ->
                        this.add("Country must be specified for type 'city'")

                    timeZone.isEmpty() ->
                        this.add("Time zone is empty")

                    !date.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) || runCatching { LocalDate.parse(date) }.isFailure ->
                        this.add("Date format is invalid or date is not valid")

                    !startHour.matches(Regex("\\d{2}:\\d{2}")) || runCatching { LocalTime.parse(startHour) }.isFailure ->
                        this.add("Start hour format is invalid or time is not valid")

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

                    else ->
                        wavedef
                            .validationErrors()
                            ?.let { addAll(it) }
                            .also { area.validationErrors()?.let { addAll(it) } }
                            .also { map.validationErrors()?.let { addAll(it) } }
                }
            }.takeIf { it.isNotEmpty() }
            ?.map { "${WWWEvent::class.simpleName}: $it" }
}
