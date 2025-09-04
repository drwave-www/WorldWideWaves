package com.worldwidewaves.shared.events

/*
 * Copyright 2025 DrWave
 *
 * WorldWideWaves is an ephemeral mobile app designed to orchestrate human waves through cities and
 * countries, culminating in a global wave. The project aims to transcend physical and cultural
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

import com.worldwidewaves.shared.events.WWWEvent.WWWWaveDefinition
import com.worldwidewaves.shared.events.utils.DataValidator
import dev.icerock.moko.resources.StringResource
import kotlinx.datetime.TimeZone
import kotlinx.serialization.Transient
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

// ---------------------------

/**
 * Defines the shared contract for a WorldWideWaves event.
 *
 * An implementation encapsulates:
 * • Stable identifiers (id, country/community, type)  
 * • Scheduling information (time-zone, date, start hour, total / end time helpers)  
 * • Geospatial components (area polygons, bounding-box, map style)  
 * • Wave definition & warming phase details (see [WWWEventWave] subclasses)  
 * • Social metadata (Instagram account / hashtag)  
 * • Runtime state helpers (favorite flag, [Status] computation, observer accessor)
 *
 * The interface is consumed by view-models, observers, and UI composables across
 * platforms; therefore only platform-agnostic types are exposed.  Platform-specific
 * implementations (e.g. Android/iOS data classes) must satisfy this contract to
 * integrate seamlessly with the shared logic.
 */
@OptIn(ExperimentalTime::class)
interface IWWWEvent : DataValidator {

    data class WaveNumbersLiterals(
        val waveTimezone: String = "",
        val waveSpeed: String = "..",
        val waveStartTime: String = "..",
        val waveEndTime: String = "..",
        val waveTotalTime: String = "..",
    )

    val id: String
    val type: String
    val country: String?
    val community: String?

    val timeZone: String
    val date: String
    val startHour: String

    val instagramAccount: String
    val instagramHashtag: String

    val wavedef: WWWWaveDefinition
    val area: WWWEventArea
    val warming: WWWEventWaveWarming
    val wave: WWWEventWave
    val map: WWWEventMap

    var favorite: Boolean

    // ---------------------------

    enum class Status { UNDEFINED, DONE, NEXT, SOON, RUNNING }

    // ---------------------------

    suspend fun getStatus(): Status
    suspend fun isDone(): Boolean
    fun isSoon(): Boolean
    suspend fun isRunning(): Boolean

    // - Images -------------------

    fun getLocationImage(): Any?
    fun getCommunityImage(): Any?
    fun getCountryImage(): Any?
    fun getMapImage(): Any?

    // - Localized ---------------

    fun getLocation(): StringResource
    fun getDescription(): StringResource
    fun getLiteralCountry(): StringResource
    fun getLiteralCommunity(): StringResource

    // ---------------------------

    fun getTZ(): TimeZone
    fun getStartDateTime(): Instant
    suspend fun getTotalTime(): Duration
    suspend fun getEndDateTime(): Instant

    fun getLiteralTimezone(): String
    fun getLiteralStartDateSimple(): String
    fun getLiteralStartTime(): String
    suspend fun getLiteralEndTime(): String
    suspend fun getLiteralTotalTime(): String

    fun getWaveStartDateTime() : Instant
    fun getWarmingDuration(): Duration
    fun isNearTime(): Boolean

    // ---------------------------

    suspend fun getAllNumbers(): WaveNumbersLiterals

    // ---------------------------

    @Transient val observer: WWWEventObserver
        get() = getEventObserver()
    fun getEventObserver(): WWWEventObserver

}


