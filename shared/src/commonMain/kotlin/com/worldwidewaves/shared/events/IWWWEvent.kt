package com.worldwidewaves.shared.events

/*
 * Copyright 2024 DrWave
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
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone

// ---------------------------

interface IWWWEvent : DataValidator {

    val id: String
    val type: String
    val location: String
    val country: String?
    val community: String?

    val timeZone: String
    val date: String
    val startHour: String

    val description: String
    val instagramAccount: String
    val instagramHashtag: String

    val wavedef: WWWWaveDefinition
    val area: WWWEventArea
    val wave: WWWEventWave
    val map: WWWEventMap

    var favorite: Boolean

    // ---------------------------

    enum class Status { DONE, NEXT, SOON, RUNNING }

    // ---------------------------

    suspend fun getStatus(): Status
    suspend fun isDone(): Boolean
    fun isSoon(): Boolean
    suspend fun isRunning(): Boolean

    // ---------------------------

    fun getLocationImage(): Any?
    fun getCommunityImage(): Any?
    fun getCountryImage(): Any?

    // ---------------------------

    fun getTZ(): TimeZone
    fun getLiteralStartDateSimple(): String
    fun getStartDateTime(): Instant

}


