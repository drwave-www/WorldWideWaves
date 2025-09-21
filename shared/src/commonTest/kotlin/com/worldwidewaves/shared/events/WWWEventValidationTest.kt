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

import com.worldwidewaves.shared.events.IWWWEvent.Status
import com.worldwidewaves.shared.events.utils.IClock
import com.worldwidewaves.shared.testing.MockClock
import com.worldwidewaves.shared.testing.TestHelpers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalTime::class)
class WWWEventValidationTest : KoinTest {
    private lateinit var mockClock: MockClock
    private val baseTime = TestHelpers.TestTimes.BASE_TIME

    @BeforeTest
    fun setUp() {
        mockClock = MockClock(baseTime)

        startKoin {
            modules(
                module {
                    single<IClock> { mockClock }
                }
            )
        }
    }

    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    // ===== ID Validation Tests =====

    @Test
    fun `valid event should have no validation errors`() {
        // GIVEN
        val event = TestHelpers.createTestEvent()

        // WHEN
        val errors = event.validationErrors()

        // THEN
        assertNull(errors)
    }

    @Test
    fun `empty ID should be invalid`() {
        // GIVEN
        val event = TestHelpers.createTestEvent(id = "")

        // WHEN
        val errors = event.validationErrors()

        // THEN
        assertNotNull(errors)
        assertTrue(errors.any { it.contains("ID is empty") })
    }

    @Test
    fun `ID with uppercase letters should be invalid`() {
        // GIVEN
        val event = TestHelpers.createTestEvent(id = "InvalidEvent")

        // WHEN
        val errors = event.validationErrors()

        // THEN
        assertNotNull(errors)
        assertTrue(errors.any { it.contains("ID must be lowercase with only simple letters or underscores") })
    }

    @Test
    fun `ID with numbers should be invalid`() {
        // GIVEN
        val event = TestHelpers.createTestEvent(id = "event123")

        // WHEN
        val errors = event.validationErrors()

        // THEN
        assertNotNull(errors)
        assertTrue(errors.any { it.contains("ID must be lowercase with only simple letters or underscores") })
    }

    @Test
    fun `ID with special characters should be invalid`() {
        // GIVEN
        val event = TestHelpers.createTestEvent(id = "event-special")

        // WHEN
        val errors = event.validationErrors()

        // THEN
        assertNotNull(errors)
        assertTrue(errors.any { it.contains("ID must be lowercase with only simple letters or underscores") })
    }

    @Test
    fun `ID with underscores should be valid`() {
        // GIVEN
        val event = TestHelpers.createTestEvent(id = "valid_event_name")

        // WHEN
        val errors = event.validationErrors()

        // THEN
        assertNull(errors)
    }

    // ===== Type Validation Tests =====

    @Test
    fun `empty type should be invalid`() {
        // GIVEN
        val event = TestHelpers.createTestEvent(type = "")

        // WHEN
        val errors = event.validationErrors()

        // THEN
        assertNotNull(errors)
        assertTrue(errors.any { it.contains("Type is empty") })
    }

    @Test
    fun `invalid type should be rejected`() {
        // GIVEN
        val event = TestHelpers.createTestEvent(type = "invalid")

        // WHEN
        val errors = event.validationErrors()

        // THEN
        assertNotNull(errors)
        assertTrue(errors.any { it.contains("Type must be either 'city', 'country', or 'world'") })
    }

    @Test
    fun `city type should be valid`() {
        // GIVEN
        val event = TestHelpers.createTestEvent(type = "city", country = "france")

        // WHEN
        val errors = event.validationErrors()

        // THEN
        assertNull(errors)
    }

    @Test
    fun `country type should be valid`() {
        // GIVEN
        val event = TestHelpers.createTestEvent(type = "country", country = "france")

        // WHEN
        val errors = event.validationErrors()

        // THEN
        assertNull(errors)
    }

    @Test
    fun `world type should be valid`() {
        // GIVEN
        val event = TestHelpers.createTestEvent(type = "world", country = null)

        // WHEN
        val errors = event.validationErrors()

        // THEN
        assertNull(errors)
    }

    @Test
    fun `city type without country should be invalid`() {
        // GIVEN
        val event = TestHelpers.createTestEvent(type = "city", country = null)

        // WHEN
        val errors = event.validationErrors()

        // THEN
        assertNotNull(errors)
        assertTrue(errors.any { it.contains("Country must be specified for type 'city'") })
    }

    @Test
    fun `city type with empty country should be invalid`() {
        // GIVEN
        val event = TestHelpers.createTestEvent(type = "city", country = "")

        // WHEN
        val errors = event.validationErrors()

        // THEN
        assertNotNull(errors)
        assertTrue(errors.any { it.contains("Country must be specified for type 'city'") })
    }

    // ===== TimeZone Validation Tests =====

    @Test
    fun `empty timezone should be invalid`() {
        // GIVEN
        val event = TestHelpers.createTestEvent(timeZone = "")

        // WHEN
        val errors = event.validationErrors()

        // THEN
        assertNotNull(errors)
        assertTrue(errors.any { it.contains("Time zone is empty") })
    }

    @Test
    fun `invalid timezone should be rejected`() {
        // GIVEN
        val event = TestHelpers.createTestEvent(timeZone = "Invalid/Timezone")

        // WHEN
        val errors = event.validationErrors()

        // THEN
        assertNotNull(errors)
        assertTrue(errors.any { it.contains("Time zone is invalid") })
    }

    @Test
    fun `valid timezone formats should be accepted`() {
        val validTimezones = listOf(
            "UTC",
            "Europe/Paris",
            "America/New_York",
            "Asia/Tokyo",
            "Australia/Sydney"
        )

        validTimezones.forEach { timezone ->
            // GIVEN
            val event = TestHelpers.createTestEvent(timeZone = timezone)

            // WHEN
            val errors = event.validationErrors()

            // THEN
            assertNull(errors, "Timezone $timezone should be valid")
        }
    }

    // ===== Date Validation Tests =====

    @Test
    fun `invalid date format should be rejected`() {
        val invalidDates = listOf(
            "2022/06/15",
            "15-06-2022",
            "2022-6-15",
            "22-06-15",
            "invalid-date",
            ""
        )

        invalidDates.forEach { date ->
            // GIVEN
            val event = TestHelpers.createTestEvent(date = date)

            // WHEN
            val errors = event.validationErrors()

            // THEN
            assertNotNull(errors, "Date $date should be invalid")
            assertTrue(errors.any { it.contains("Date format is invalid or date is not valid") })
        }
    }

    @Test
    fun `invalid date values should be rejected`() {
        val invalidDates = listOf(
            "2022-13-01", // Invalid month
            "2022-02-30", // Invalid day for February
            "2022-04-31", // Invalid day for April
            "2022-00-01", // Invalid month (0)
            "2022-01-00"  // Invalid day (0)
        )

        invalidDates.forEach { date ->
            // GIVEN
            val event = TestHelpers.createTestEvent(date = date)

            // WHEN
            val errors = event.validationErrors()

            // THEN
            assertNotNull(errors, "Date $date should be invalid")
            assertTrue(errors.any { it.contains("Date format is invalid or date is not valid") })
        }
    }

    @Test
    fun `valid dates should be accepted`() {
        val validDates = listOf(
            "2022-01-01",
            "2022-12-31",
            "2024-02-29", // Leap year
            "2022-06-15"
        )

        validDates.forEach { date ->
            // GIVEN
            val event = TestHelpers.createTestEvent(date = date)

            // WHEN
            val errors = event.validationErrors()

            // THEN
            assertNull(errors, "Date $date should be valid")
        }
    }

    // ===== Start Hour Validation Tests =====

    @Test
    fun `invalid startHour format should be rejected`() {
        val invalidHours = listOf(
            "18",
            "6:00",
            "18:0",
            "25:00",
            "18:60",
            "invalid",
            ""
        )

        invalidHours.forEach { hour ->
            // GIVEN
            val event = TestHelpers.createTestEvent(startHour = hour)

            // WHEN
            val errors = event.validationErrors()

            // THEN
            assertNotNull(errors, "Start hour $hour should be invalid")
            assertTrue(errors.any { it.contains("Start hour format is invalid or time is not valid") })
        }
    }

    @Test
    fun `valid startHour formats should be accepted`() {
        val validHours = listOf(
            "00:00",
            "06:30",
            "12:00",
            "18:00",
            "23:59"
        )

        validHours.forEach { hour ->
            // GIVEN
            val event = TestHelpers.createTestEvent(startHour = hour)

            // WHEN
            val errors = event.validationErrors()

            // THEN
            assertNull(errors, "Start hour $hour should be valid")
        }
    }

    // ===== Instagram Account Validation Tests =====

    @Test
    fun `empty instagram account should be invalid`() {
        // GIVEN
        val event = TestHelpers.createTestEvent(instagramAccount = "")

        // WHEN
        val errors = event.validationErrors()

        // THEN
        assertNotNull(errors)
        assertTrue(errors.any { it.contains("Instagram account is empty") })
    }

    @Test
    fun `invalid instagram account format should be rejected`() {
        val invalidAccounts = listOf(
            "account with spaces",
            "account-with-dash",
            "account@symbol",
            "account+plus"
        )

        invalidAccounts.forEach { account ->
            // GIVEN
            val event = TestHelpers.createTestEvent(instagramAccount = account)

            // WHEN
            val errors = event.validationErrors()

            // THEN
            assertNotNull(errors, "Instagram account $account should be invalid")
            assertTrue(errors.any { it.contains("Instagram account is invalid") })
        }
    }

    @Test
    fun `valid instagram account formats should be accepted`() {
        val validAccounts = listOf(
            "worldwidewaves",
            "world_wide_waves",
            "worldwidewaves123",
            "WWW.waves",
            "a"
        )

        validAccounts.forEach { account ->
            // GIVEN
            val event = TestHelpers.createTestEvent(instagramAccount = account)

            // WHEN
            val errors = event.validationErrors()

            // THEN
            assertNull(errors, "Instagram account $account should be valid")
        }
    }

    // ===== Instagram Hashtag Validation Tests =====

    @Test
    fun `empty instagram hashtag should be invalid`() {
        // GIVEN
        val event = TestHelpers.createTestEvent(instagramHashtag = "")

        // WHEN
        val errors = event.validationErrors()

        // THEN
        assertNotNull(errors)
        assertTrue(errors.any { it.contains("Instagram hashtag is empty") })
    }

    @Test
    fun `hashtag without # should be invalid`() {
        // GIVEN
        val event = TestHelpers.createTestEvent(instagramHashtag = "WorldWideWaves")

        // WHEN
        val errors = event.validationErrors()

        // THEN
        assertNotNull(errors)
        assertTrue(errors.any { it.contains("Instagram hashtag is invalid") })
    }

    @Test
    fun `hashtag with special characters should be invalid`() {
        val invalidHashtags = listOf(
            "#World Wide Waves",
            "#World-Wide-Waves",
            "#World@Waves",
            "#World+Waves"
        )

        invalidHashtags.forEach { hashtag ->
            // GIVEN
            val event = TestHelpers.createTestEvent(instagramHashtag = hashtag)

            // WHEN
            val errors = event.validationErrors()

            // THEN
            assertNotNull(errors, "Hashtag $hashtag should be invalid")
            assertTrue(errors.any { it.contains("Instagram hashtag is invalid") })
        }
    }

    @Test
    fun `valid hashtag formats should be accepted`() {
        val validHashtags = listOf(
            "#WorldWideWaves",
            "#World_Wide_Waves",
            "#WWW123",
            "#w",
            "#123"
        )

        validHashtags.forEach { hashtag ->
            // GIVEN
            val event = TestHelpers.createTestEvent(instagramHashtag = hashtag)

            // WHEN
            val errors = event.validationErrors()

            // THEN
            assertNull(errors, "Hashtag $hashtag should be valid")
        }
    }

    // ===== Status Calculation Tests =====

    @Test
    fun `event in future should have NEXT status`() = runTest {
        // GIVEN: Event starts in 2 days (beyond WAVE_SOON_DELAY)
        mockClock.setTime(baseTime)
        val event = TestHelpers.createTestEvent(
            date = "2022-02-01", // 31 days after baseTime (2022-01-01)
            startHour = "12:00"
        )

        // WHEN
        val status = event.getStatus()

        // THEN
        assertEquals(Status.NEXT, status)
    }

    @Test
    fun `event within soon delay should have SOON status`() = runTest {
        // GIVEN: Event starts in 15 days (within WAVE_SOON_DELAY of 30 days)
        mockClock.setTime(baseTime)
        val event = TestHelpers.createTestEvent(
            date = "2022-01-16", // 15 days after baseTime
            startHour = "12:00"
        )

        // WHEN
        val status = event.getStatus()

        // THEN
        assertEquals(Status.SOON, status)
    }

    @Test
    fun `running event should have RUNNING status`() = runTest {
        // GIVEN: Event that started 1 hour ago and is still running
        mockClock.setTime(baseTime + 1.hours)
        val event = TestHelpers.createRunningEvent(
            startedAgo = 1.hours,
            totalDuration = 4.hours
        )

        // WHEN
        val status = event.getStatus()

        // THEN
        assertEquals(Status.RUNNING, status)
    }

    @Test
    fun `completed event should have DONE status`() = runTest {
        // GIVEN: Event that ended in the past
        mockClock.setTime(baseTime + 10.hours)
        val event = TestHelpers.createCompletedEvent(
            endedAgo = 5.hours,
            totalDuration = 1.hours
        )

        // WHEN
        val status = event.getStatus()

        // THEN
        assertEquals(Status.DONE, status)
    }

    // ===== DateTime Parsing Tests =====

    @Test
    fun `getStartDateTime should handle timezone offset correctly`() {
        // GIVEN: Event in different timezone
        val event = TestHelpers.createTestEvent(
            timeZone = "America/New_York",
            date = "2022-06-15",
            startHour = "18:00"
        )

        // WHEN
        val startDateTime = event.getStartDateTime()

        // THEN: Should return correct UTC instant
        assertNotNull(startDateTime)
    }

    @Test
    fun `isNearTime should respect WAVE_OBSERVE_DELAY`() {
        // GIVEN: Event exactly at WAVE_OBSERVE_DELAY distance
        mockClock.setTime(baseTime)
        val event = TestHelpers.createTestEvent(
            date = "2022-01-01",
            startHour = "02:00" // 2 hours after baseTime (equals WAVE_OBSERVE_DELAY)
        )

        // WHEN
        val isNear = event.isNearTime()

        // THEN
        assertTrue(isNear)
    }

    @Test
    fun `isNearTime should return false for events beyond WAVE_OBSERVE_DELAY`() {
        // GIVEN: Event beyond WAVE_OBSERVE_DELAY
        mockClock.setTime(baseTime)
        val event = TestHelpers.createTestEvent(
            date = "2022-01-01",
            startHour = "03:00" // 3 hours after baseTime (beyond WAVE_OBSERVE_DELAY)
        )

        // WHEN
        val isNear = event.isNearTime()

        // THEN
        assertTrue(!isNear)
    }

    // ===== Wave Definition Tests =====

    @Test
    fun `wave definition validation should propagate errors`() {
        // GIVEN: Event with area validation errors
        val mockArea = TestHelpers.createMockArea()
        io.mockk.every { mockArea.validationErrors() } returns listOf("Area is invalid", "Polygon is malformed")

        val event = TestHelpers.createTestEvent(area = mockArea)

        // WHEN
        val errors = event.validationErrors()

        // THEN: Should include area validation errors
        assertNotNull(errors)
        assertTrue(errors.any { it.contains("Area is invalid") })
        assertTrue(errors.any { it.contains("Polygon is malformed") })
    }

    @Test
    fun `map validation errors should be included in event validation`() {
        // GIVEN: Map with validation errors
        val mockMap = TestHelpers.createMockMap()
        io.mockk.every { mockMap.validationErrors() } returns listOf("Map style is invalid")

        val event = TestHelpers.createTestEvent(map = mockMap)

        // WHEN
        val errors = event.validationErrors()

        // THEN: Should include map validation errors
        assertNotNull(errors)
        assertTrue(errors.any { it.contains("Map style is invalid") })
    }
}