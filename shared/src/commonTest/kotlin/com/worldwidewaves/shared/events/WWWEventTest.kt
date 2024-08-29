package com.worldwidewaves.shared.events

import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

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

    private val testEvent = WWWEvent(
        id = "paris_france",
        type = "city",
        location = "Paris",
        country = "france",
        community = "europe",
        date = "2024-03-15",
        startHour = "18:00",
        speed = 5,
        description = "Experience the wave in Paris",
        instagramAccount = "worldwidewaves.paris",
        instagramHashtag = "#waveparis",
        mapOsmadminid = 71525,
        mapMinzoom = 10.0,
        mapMaxzoom = 14.0,
        mapLanguage = "fr",
        mapOsmarea = "europe/france/ile-de-france",
        mapWarmingZoneLongitude = 2.3417,
        timeZone = "Europe/Paris"
    )

    @Test
    fun testIsDone() { // TODO: correct this test
        assertEquals(true, testEvent.isDone())
    }

    @Test
    fun testIsSoon() { // TODO: correct this test
        assertEquals(false, testEvent.isSoon())
    }

    @Test
    fun testIsRunning() { // TODO: correct this test
        assertEquals(false, testEvent.isRunning())
    }

    @Test
    fun testGetLocationImage() {
        assertNotNull(testEvent.getLocationImage())
    }

    @Test
    fun testGetCommunityImage() {
        assertNotNull(testEvent.getCommunityImage())
    }

    @Test
    fun testGetCountryImage() {
        assertNotNull(testEvent.getCountryImage())
    }

    @Test
    fun testGetTimeZone() {
        assertEquals("Europe/Paris", testEvent.getTimeZone().id)
    }

    @Test
    fun testGetStartDateSimpleAsLocal() {
        assertEquals("15/03", testEvent.getStartDateSimpleAsLocal())
    }

    @Test
    fun testGetStartDateTimeAsLocal() {
        val expectedDateTime = LocalDateTime.parse("2024-03-15T18:00")
        assertEquals(expectedDateTime, testEvent.getStartDateTimeAsLocal())
    }
}

// ---------------------------

fun createRandomWWWEvent(id: String): WWWEvent {
    val types = listOf("city", "country", "world")
    val locations = listOf("Paris", "Rio de Janeiro", "New York", "Tokyo", "Sydney")
    val countries = listOf("france", "brazil", "usa", "japan", "australia")
    val communities = listOf("europe", "south-america", "usa", "asia", "oceania")
    val descriptions = listOf(
        "Experience the wave in Paris, where the charm of the Eiffel Tower meets the elegance of the Champs-Élysées",
        "Join the wave in Rio de Janeiro, where the rhythm of Copacabana meets the majesty of Christ the Redeemer",
        "Feel the wave across the United States, from the bustling streets of New York to the sunny beaches of California",
        "Be part of the global wave, uniting people from every corner of the world in a celebration of unity and diversity"
    )
    val instagramAccounts = listOf(
        "worldwidewaves.paris",
        "worldwidewaves.rio",
        "worldwidewaves.usa",
        "worldwidewaves.tokyo",
        "worldwidewaves.sydney"
    )
    val instagramHashtags = listOf("#waveparis", "#waverio", "#waveusa", "#wavetokyo", "#wavesydney")
    val mapCenters = listOf(
        "48.8619,2.3417",
        "-22.9068,-43.1729",
        "37.0902,-95.7129",
        "35.6895,139.6917",
        "-33.8688,151.2093"
    )
    val mapOsmadminids = listOf(71525, 3448439, 148838, 1118370, 2158177)
    val mapLanguages = listOf("fr", "pt", "en", "ja", "en")
    val mapOsmareas = listOf(
        "europe/france/ile-de-france",
        "south-america/brazil/sudeste",
        "north-america/us",
        "asia/japan/kanto",
        "oceania/australia/nsw"
    )
    val timeZones = listOf("Europe/Paris", "America/Sao_Paulo", "America/New_York", "Asia/Tokyo", "Australia/Sydney")

    return WWWEvent( // TODO: improve this random data generation
        id = id,
        type = types.random(),
        location = locations.random(),
        country = countries.random(),
        community = communities.random(),
        date = "2024-03-15",
        startHour = "18:00",
        speed = 5,
        description = descriptions.random(),
        instagramAccount = instagramAccounts.random(),
        instagramHashtag = instagramHashtags.random(),
        mapOsmadminid = mapOsmadminids.random(),
        mapMinzoom = 10.0,
        mapMaxzoom = 14.0,
        mapLanguage = mapLanguages.random(),
        mapOsmarea = mapOsmareas.random(),
        mapWarmingZoneLongitude = mapCenters.random().split(",")[1].toDouble(),
        timeZone = timeZones.random()
    )
}