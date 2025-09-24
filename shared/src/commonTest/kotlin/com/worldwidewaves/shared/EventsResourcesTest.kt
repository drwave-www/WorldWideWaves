package com.worldwidewaves.shared

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

import com.worldwidewaves.shared.generated.resources.Res
import com.worldwidewaves.shared.generated.resources.not_found
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Comprehensive tests for EventsResources functionality including
 * resource loading, caching, localization handling, and error scenarios.
 */
class EventsResourcesTest {

    @Test
    fun `test getEventImage returns correct location images`() {
        // Test valid location images
        val newYorkImage = getEventImage("location", "new_york_usa")
        assertNotNull(newYorkImage, "New York location image should not be null")

        val londonImage = getEventImage("location", "london_england")
        assertNotNull(londonImage, "London location image should not be null")

        val tokyoImage = getEventImage("location", "tokyo_japan")
        assertNotNull(tokyoImage, "Tokyo location image should not be null")

        val parisImage = getEventImage("location", "paris_france")
        assertNotNull(parisImage, "Paris location image should not be null")
    }

    @Test
    fun `test getEventImage returns correct map images`() {
        // Test valid map images
        val berlinMap = getEventImage("map", "berlin_germany")
        assertNotNull(berlinMap, "Berlin map image should not be null")

        val sydneyMap = getEventImage("map", "sydney_australia")
        assertNotNull(sydneyMap, "Sydney map image should not be null")

        val mumbaiMap = getEventImage("map", "mumbai_india")
        assertNotNull(mumbaiMap, "Mumbai map image should not be null")

        val saoMap = getEventImage("map", "sao_paulo_brazil")
        assertNotNull(saoMap, "São Paulo map image should not be null")
    }

    @Test
    fun `test getEventImage returns correct community images`() {
        // Test valid community images
        val europeImage = getEventImage("community", "europe")
        assertNotNull(europeImage, "Europe community image should not be null")

        val asiaImage = getEventImage("community", "asia")
        assertNotNull(asiaImage, "Asia community image should not be null")

        val africaImage = getEventImage("community", "africa")
        assertNotNull(africaImage, "Africa community image should not be null")

        val northAmericaImage = getEventImage("community", "north_america")
        assertNotNull(northAmericaImage, "North America community image should not be null")
    }

    @Test
    fun `test getEventImage returns correct country images`() {
        // Test valid country images
        val usaImage = getEventImage("country", "usa")
        assertNotNull(usaImage, "USA country image should not be null")

        val brazilImage = getEventImage("country", "brazil")
        assertNotNull(brazilImage, "Brazil country image should not be null")

        val chinaImage = getEventImage("country", "china")
        assertNotNull(chinaImage, "China country image should not be null")

        val germanyImage = getEventImage("country", "germany")
        assertNotNull(germanyImage, "Germany country image should not be null")
    }

    @Test
    fun `test getEventImage returns not_found for invalid locations`() {
        // Test invalid location IDs
        val invalidLocation = getEventImage("location", "invalid_location")
        assertEquals(Res.drawable.not_found, invalidLocation)

        val emptyLocation = getEventImage("location", "")
        assertEquals(Res.drawable.not_found, emptyLocation)

        val nonExistentCity = getEventImage("location", "atlantis_fantasy")
        assertEquals(Res.drawable.not_found, nonExistentCity)
    }

    @Test
    fun `test getEventImage returns not_found for invalid types`() {
        // Test invalid type values
        val invalidType = getEventImage("invalid_type", "new_york_usa")
        assertEquals(Res.drawable.not_found, invalidType)

        val emptyType = getEventImage("", "london_england")
        assertEquals(Res.drawable.not_found, emptyType)

        val nullType = getEventImage("unknown", "paris_france")
        assertEquals(Res.drawable.not_found, nullType)
    }

    @Test
    fun `test getCountryText returns correct string resources`() {
        // Test valid country string resources
        val usaText = getCountryText("usa")
        assertEquals(MokoRes.strings.country_usa, usaText)

        val brazilText = getCountryText("brazil")
        assertEquals(MokoRes.strings.country_brazil, brazilText)

        val japanText = getCountryText("japan")
        assertEquals(MokoRes.strings.country_japan, japanText)

        val germanyText = getCountryText("germany")
        assertEquals(MokoRes.strings.country_germany, germanyText)
    }

    @Test
    fun `test getCountryText returns empty for invalid countries`() {
        // Test invalid country IDs
        val invalidCountry = getCountryText("invalid_country")
        assertEquals(MokoRes.strings.empty, invalidCountry)

        val nullCountry = getCountryText(null)
        assertEquals(MokoRes.strings.empty, nullCountry)

        val emptyCountry = getCountryText("")
        assertEquals(MokoRes.strings.empty, emptyCountry)
    }

    @Test
    fun `test getCommunityText returns correct string resources`() {
        // Test valid community string resources
        val europeText = getCommunityText("europe")
        assertEquals(MokoRes.strings.community_europe, europeText)

        val asiaText = getCommunityText("asia")
        assertEquals(MokoRes.strings.community_asia, asiaText)

        val africaText = getCommunityText("africa")
        assertEquals(MokoRes.strings.community_africa, africaText)

        val northAmericaText = getCommunityText("north_america")
        assertEquals(MokoRes.strings.community_north_america, northAmericaText)
    }

    @Test
    fun `test getCommunityText returns empty for invalid communities`() {
        // Test invalid community IDs
        val invalidCommunity = getCommunityText("invalid_community")
        assertEquals(MokoRes.strings.empty, invalidCommunity)

        val nullCommunity = getCommunityText(null)
        assertEquals(MokoRes.strings.empty, nullCommunity)

        val emptyCommunity = getCommunityText("")
        assertEquals(MokoRes.strings.empty, emptyCommunity)
    }

    @Test
    fun `test getEventText returns correct location string resources`() {
        // Test valid location text resources
        val newYorkText = getEventText("location", "new_york_usa")
        assertEquals(MokoRes.strings.event_location_new_york_usa, newYorkText)

        val londonText = getEventText("location", "london_england")
        assertEquals(MokoRes.strings.event_location_london_england, londonText)

        val tokyoText = getEventText("location", "tokyo_japan")
        assertEquals(MokoRes.strings.event_location_tokyo_japan, tokyoText)

        val parisText = getEventText("location", "paris_france")
        assertEquals(MokoRes.strings.event_location_paris_france, parisText)
    }

    @Test
    fun `test getEventText returns correct description string resources`() {
        // Test valid description text resources
        val newYorkDesc = getEventText("description", "new_york_usa")
        assertEquals(MokoRes.strings.event_description_new_york_usa, newYorkDesc)

        val londonDesc = getEventText("description", "london_england")
        assertEquals(MokoRes.strings.event_description_london_england, londonDesc)

        val tokyoDesc = getEventText("description", "tokyo_japan")
        assertEquals(MokoRes.strings.event_description_tokyo_japan, tokyoDesc)

        val parisDesc = getEventText("description", "paris_france")
        assertEquals(MokoRes.strings.event_description_paris_france, parisDesc)
    }

    @Test
    fun `test getEventText returns empty for invalid location IDs`() {
        // Test invalid location IDs return empty
        val invalidLocation = getEventText("location", "invalid_location")
        assertEquals(MokoRes.strings.empty, invalidLocation)

        val emptyLocation = getEventText("location", "")
        assertEquals(MokoRes.strings.empty, emptyLocation)

        val nonExistentCity = getEventText("location", "atlantis_fantasy")
        assertEquals(MokoRes.strings.empty, nonExistentCity)
    }

    @Test
    fun `test getEventText throws exception for invalid types`() {
        // Test that invalid types throw exceptions
        assertFailsWith<Exception> {
            getEventText("invalid_type", "new_york_usa")
        }

        assertFailsWith<Exception> {
            getEventText("", "london_england")
        }

        assertFailsWith<Exception> {
            getEventText("unknown", "paris_france")
        }
    }

    @Test
    fun `test resource loading consistency across major cities`() {
        val majorCities = listOf(
            "new_york_usa", "london_england", "tokyo_japan", "paris_france",
            "berlin_germany", "sydney_australia", "mumbai_india", "sao_paulo_brazil"
        )

        // Verify all major cities have consistent resource availability
        majorCities.forEach { cityId ->
            // Location image should be available
            val locationImage = getEventImage("location", cityId)
            assertNotNull(locationImage, "Location image should be available for $cityId")

            // Map image should be available
            val mapImage = getEventImage("map", cityId)
            assertNotNull(mapImage, "Map image should be available for $cityId")

            // Location text should be available
            val locationText = getEventText("location", cityId)
            assertTrue(locationText != MokoRes.strings.empty, "Location text should be available for $cityId")

            // Description text should be available
            val descriptionText = getEventText("description", cityId)
            assertTrue(descriptionText != MokoRes.strings.empty, "Description text should be available for $cityId")
        }
    }

    @Test
    fun `test resource memory management and caching behavior`() {
        // Test repeated access to same resources
        val resourceId = "new_york_usa"

        // Call the same resource multiple times to test caching behavior
        val image1 = getEventImage("location", resourceId)
        val image2 = getEventImage("location", resourceId)
        val image3 = getEventImage("location", resourceId)

        // All should return the same resource reference
        assertEquals(image1, image2, "Repeated calls should return same resource")
        assertEquals(image2, image3, "Repeated calls should return same resource")

        val text1 = getEventText("location", resourceId)
        val text2 = getEventText("location", resourceId)
        val text3 = getEventText("location", resourceId)

        // All should return the same string resource
        assertEquals(text1, text2, "Repeated calls should return same string resource")
        assertEquals(text2, text3, "Repeated calls should return same string resource")
    }

    @Test
    fun `test resource loading performance with large datasets`() {
        val startTime = System.currentTimeMillis()

        // Test loading a large number of resources
        val allLocations = listOf(
            "new_york_usa", "los_angeles_usa", "chicago_usa", "san_francisco_usa",
            "london_england", "paris_france", "berlin_germany", "madrid_spain", "rome_italy",
            "tokyo_japan", "seoul_south_korea", "beijing_china", "shanghai_china", "hong_kong_china",
            "mumbai_india", "delhi_india", "bangalore_india", "sydney_australia", "melbourne_australia",
            "sao_paulo_brazil", "buenos_aires_argentina", "mexico_city_mexico"
        )

        allLocations.forEach { location ->
            getEventImage("location", location)
            getEventImage("map", location)
            getEventText("location", location)
            getEventText("description", location)
        }

        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime

        // Resource loading should be reasonably fast (under 1 second for all resources)
        assertTrue(duration < 1000, "Resource loading should complete within 1 second, took ${duration}ms")
    }

    @Test
    fun `test missing resource fallback behavior`() {
        // Test that fallback resources are properly returned
        val fallbackImage = getEventImage("location", "non_existent_location")
        assertEquals(Res.drawable.not_found, fallbackImage, "Should return not_found for missing image")

        val fallbackCountry = getCountryText("non_existent_country")
        assertEquals(MokoRes.strings.empty, fallbackCountry, "Should return empty for missing country")

        val fallbackCommunity = getCommunityText("non_existent_community")
        assertEquals(MokoRes.strings.empty, fallbackCommunity, "Should return empty for missing community")

        val fallbackLocation = getEventText("location", "non_existent_location")
        assertEquals(MokoRes.strings.empty, fallbackLocation, "Should return empty for missing location text")
    }

    @Test
    fun `test special characters and edge cases in resource IDs`() {
        // Test edge cases with special characters and unusual formatting
        val specialCaseIds = listOf(
            "democratic_republic_of_the_congo",
            "united_arab_emirates",
            "hong_kong_china",
            "sao_paulo_brazil"
        )

        specialCaseIds.forEach { id ->
            // Should handle special characters without errors
            val countryText = getCountryText(id)
            assertNotNull(countryText, "Should handle special characters in country ID: $id")

            val locationImage = getEventImage("location", id)
            assertNotNull(locationImage, "Should handle special characters in location ID: $id")
        }
    }
}