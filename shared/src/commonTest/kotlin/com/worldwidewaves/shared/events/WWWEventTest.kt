package com.worldwidewaves.shared.events

import com.worldwidewaves.shared.events.WWWEvent.WWWWaveDefinition
import kotlinx.datetime.IllegalTimeZoneException
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

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
        val event =
            buildEmptyEvent(timeZone = "Pacific/Auckland", date = "2024-03-15", startHour = "18:00")

        // WHEN
        val result = event.getStartDateTime()

        // THEN
        assertEquals(LocalDateTime(2024, 3, 15, 18, 0), result)
    }

    @Test
    fun testGetStartDateSimpleAsLocal_InvalidDate() {
        // GIVEN
        val event = buildEmptyEvent(
            timeZone = "Pacific/Auckland",
            date = "invalid-date",
            startHour = "18:00"
        )

        // WHEN & THEN
        assertFailsWith<IllegalStateException> {
            event.getStartDateTime()
        }
    }

    @Test
    fun testGetLiteralStartDateSimple_ValidDate() {
        // GIVEN
        val event =
            buildEmptyEvent(timeZone = "Pacific/Auckland", date = "2024-03-15", startHour = "18:00")

        // WHEN
        val result = event.getLiteralStartDateSimple()

        // THEN
        assertEquals("15/03", result)
    }

    @Test
    fun testGetLiteralStartDateSimple_InvalidDate() {
        // GIVEN
        val event = buildEmptyEvent(
            timeZone = "Pacific/Auckland",
            date = "invalid-date",
            startHour = "18:00"
        )

        // WHEN
        val result = event.getLiteralStartDateSimple()

        // THEN
        assertEquals("error", result)
    }

    @Test
    fun testGetTZ() {
        // GIVEN
        val event = buildEmptyEvent(timeZone = "Pacific/Auckland")

        // WHEN
        val result = event.getTZ()

        // THEN
        assertEquals(TimeZone.of("Pacific/Auckland"), result)
    }

    @Test
    fun testGetTZ_InvalidTimeZone() {
        // GIVEN
        val event = buildEmptyEvent(timeZone = "Invalid/TimeZone")

        // WHEN & THEN
        assertFailsWith<IllegalTimeZoneException> {
            event.getTZ()
        }
    }

    @Test
    fun testGetLiteralStartDateSimple_InvalidTimeZone() {
        // GIVEN
        val event = buildEmptyEvent(timeZone = "Invalid/TimeZone", date = "2024-03-15", startHour = "18:00")

        // WHEN
        val result = event.getLiteralStartDateSimple()

        // THEN
        assertEquals("error", result)
    }

    @Test
    fun testValidationErrors_EmptyID() {
        // GIVEN
        val event = buildEmptyEvent(id = "")

        // WHEN
        val errors = event.validationErrors()

        // THEN
        assertTrue(errors!!.contains("event: ID is empty"))
    }

    @Test
    fun testValidationErrors_InvalidID() {
        // GIVEN
        val event = buildEmptyEvent(id = "InvalidID")

        // WHEN
        val errors = event.validationErrors()

        // THEN
        assertTrue(errors!!.contains("event: ID must be lowercase with only simple letters or underscores"))
    }

    @Test
    fun testValidationErrors_EmptyType() {
        // GIVEN
        val event = buildEmptyEvent(type = "")

        // WHEN
        val errors = event.validationErrors()

        // THEN
        assertTrue(errors!!.contains("event: Type is empty"))
    }

    @Test
    fun testValidationErrors_InvalidType() {
        // GIVEN
        val event = buildEmptyEvent(type = "invalid")

        // WHEN
        val errors = event.validationErrors()

        // THEN
        assertTrue(errors!!.contains("event: Type must be either 'city', 'country', or 'world'"))
    }

    @Test
    fun testValidationErrors_EmptyLocation() {
        // GIVEN
        val event = buildEmptyEvent(location = "")

        // WHEN
        val errors = event.validationErrors()

        // THEN
        assertTrue(errors!!.contains("event: Location is empty"))
    }

    @Test
    fun testValidationErrors_EmptyCountryForCityType() {
        // GIVEN
        val event = buildEmptyEvent(type = "city", country = null)

        // WHEN
        val errors = event.validationErrors()

        // THEN
        assertTrue(errors!!.contains("event: Country must be specified for type 'city'"))
    }

    @Test
    fun testValidationErrors_EmptyTimeZone() {
        // GIVEN
        val event = buildEmptyEvent(timeZone = "")

        // WHEN
        val errors = event.validationErrors()

        // THEN
        assertTrue(errors!!.contains("event: Time zone is empty"))
    }

    @Test
    fun testValidationErrors_InvalidDateFormat() {
        // GIVEN
        val event = buildEmptyEvent(date = "invalid-date")

        // WHEN
        val errors = event.validationErrors()

        // THEN
        assertTrue(errors!!.contains("event: Date format is invalid or date is not valid"))
    }

    @Test
    fun testValidationErrors_InvalidStartHourFormat() {
        // GIVEN
        val event = buildEmptyEvent(startHour = "invalid-time")

        // WHEN
        val errors = event.validationErrors()

        // THEN
        assertTrue(errors!!.contains("event: Start hour format is invalid or time is not valid"))
    }

    @Test
    fun testValidationErrors_EmptyDescription() {
        // GIVEN
        val event = buildEmptyEvent(description = "")

        // WHEN
        val errors = event.validationErrors()

        // THEN
        assertTrue(errors!!.contains("event: Description is empty"))
    }

    @Test
    fun testValidationErrors_EmptyInstagramAccount() {
        // GIVEN
        val event = buildEmptyEvent(instagramAccount = "")

        // WHEN
        val errors = event.validationErrors()

        // THEN
        assertTrue(errors!!.contains("event: Instagram account is empty"))
    }

    @Test
    fun testValidationErrors_InvalidInstagramAccount() {
        // GIVEN
        val event = buildEmptyEvent(instagramAccount = "Invalid@Account")

        // WHEN
        val errors = event.validationErrors()

        // THEN
        assertTrue(errors!!.contains("event: Instagram account is invalid"))
    }

    @Test
    fun testValidationErrors_EmptyInstagramHashtag() {
        // GIVEN
        val event = buildEmptyEvent(instagramHashtag = "")

        // WHEN
        val errors = event.validationErrors()

        // THEN
        assertTrue(errors!!.contains("event: Instagram hashtag is empty"))
    }

    @Test
    fun testValidationErrors_InvalidInstagramHashtag() {
        // GIVEN
        val event = buildEmptyEvent(instagramHashtag = "InvalidHashtag")

        // WHEN
        val errors = event.validationErrors()

        // THEN
        assertTrue(errors!!.contains("event: Instagram hashtag is invalid"))
    }

    @Test
    fun testValidationErrors_InvalidTimeZone() {
        // GIVEN
        val event = buildEmptyEvent(timeZone = "Invalid/TimeZone")

        // WHEN
        val errors = event.validationErrors()

        // THEN
        assertTrue(errors!!.contains("event: Time zone is invalid"))
    }

}

// ============================

fun buildEmptyEvent(
    id: String = "test",
    type: String = "city",
    location: String = "somewhere",
    country: String? = "xx",
    community: String? = null,
    timeZone: String = "Europe/London",
    date: String = "2024-03-15",
    startHour: String = "18:00",
    description: String = "some event",
    instagramAccount: String = "user",
    instagramHashtag: String = "#hashtag",
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
        country = country,
        community = community,
        timeZone = timeZone,
        date = date,
        startHour = startHour,
        description = description,
        instagramAccount = instagramAccount,
        instagramHashtag = instagramHashtag,
        wavedef = wavedef,
        area = WWWEventArea(osmAdminid),
        map = WWWEventMap(maxzoom, language, zone)
    )
}