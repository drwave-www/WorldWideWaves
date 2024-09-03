package com.worldwidewaves.shared.events

import com.worldwidewaves.shared.events.WWWEvent.WWWWaveDefinition
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

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

class WWWEventTest {

    @Test
    fun testGetStartDateSimpleAsLocal() {
        // GIVEN
        val event = buildEmptyEvent(timeZone = "Pacific/Auckland", date = "2024-03-15", startHour = "18:00")

        // WHEN
        val result = event.getStartDateTime()

        // THEN
        assertEquals(LocalDateTime(2024, 3, 15, 18, 0), result)
    }

    @Test
    fun testGetStartDateSimpleAsLocal_InvalidDate() {
        // GIVEN
        val event = buildEmptyEvent(timeZone = "Pacific/Auckland", date = "invalid-date", startHour = "18:00")

        // WHEN
        val result = event.getStartDateTime()

        // THEN
        assertEquals(LocalDateTime(0, 1, 1, 0, 0), result)
    }

}

// ============================

fun buildEmptyEvent(
    id: String = "",
    type: String = "",
    location: String = "",
    timeZone: String = "",
    date: String = "",
    startHour: String = "",
    description: String = "",
    instagramAccount: String = "",
    instagramHashtag: String = "",
    wavedef: WWWWaveDefinition = WWWWaveDefinition(),
    osmAdminid: Int = 0,
    maxzoom: Double = 0.0,
    language: String = "",
    zone: String = ""
): WWWEvent {
    return WWWEvent(
        id = id,
        type = type,
        location = location,
        timeZone = timeZone,
        date = date,
        startHour = startHour,
        description = description,
        instagramAccount = instagramAccount,
        instagramHashtag = instagramHashtag,
        wavedef = wavedef,
        area = WWWEventArea(osmAdminid, warming = WWWEventArea.Warming(type = "")),
        map = WWWEventMap(maxzoom, language, zone)
    )
}