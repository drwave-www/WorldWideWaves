package com.worldwidewaves.shared.ui.utils

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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ClickableTextUtilsTest {
    @Test
    fun findClickableLinks_withHttpsUrl_returnsCorrectLink() {
        val text = "Visit https://example.com for more info"
        val links = findClickableLinks(text)

        assertEquals(1, links.size)
        assertEquals("https://example.com", links[0].url)
        assertEquals(6..24, links[0].range)
    }

    @Test
    fun findClickableLinks_withHttpUrl_returnsCorrectLink() {
        val text = "Visit http://example.com for more info"
        val links = findClickableLinks(text)

        assertEquals(1, links.size)
        assertEquals("http://example.com", links[0].url)
        assertEquals(6..23, links[0].range)
    }

    @Test
    fun findClickableLinks_withUrlEndingInPeriod_excludesTrailingPeriod() {
        val text = "Check https://example.com."
        val links = findClickableLinks(text)

        assertEquals(1, links.size)
        assertEquals("https://example.com", links[0].url)
        assertEquals(6..24, links[0].range)
    }

    @Test
    fun findClickableLinks_withUrlEndingInComma_excludesTrailingComma() {
        val text = "See https://example.com, and visit"
        val links = findClickableLinks(text)

        assertEquals(1, links.size)
        assertEquals("https://example.com", links[0].url)
        assertEquals(4..22, links[0].range)
    }

    @Test
    fun findClickableLinks_withUrlEndingInExclamation_excludesTrailingExclamation() {
        val text = "Visit https://example.com!"
        val links = findClickableLinks(text)

        assertEquals(1, links.size)
        assertEquals("https://example.com", links[0].url)
        assertEquals(6..24, links[0].range)
    }

    @Test
    fun findClickableLinks_withUrlInParentheses_excludesClosingParen() {
        val text = "(see https://example.com)"
        val links = findClickableLinks(text)

        assertEquals(1, links.size)
        assertEquals("https://example.com", links[0].url)
        assertEquals(5..23, links[0].range)
    }

    @Test
    fun findClickableLinks_withUrlWithPath_returnsFullUrl() {
        val text = "Visit https://example.com/path/to/page"
        val links = findClickableLinks(text)

        assertEquals(1, links.size)
        assertEquals("https://example.com/path/to/page", links[0].url)
        assertEquals(6..37, links[0].range)
    }

    @Test
    fun findClickableLinks_withUrlWithQueryParams_returnsFullUrl() {
        val text = "Visit https://example.com/search?q=test&page=1"
        val links = findClickableLinks(text)

        assertEquals(1, links.size)
        assertEquals("https://example.com/search?q=test&page=1", links[0].url)
        assertEquals(6..45, links[0].range)
    }

    @Test
    fun findClickableLinks_withEmailAddress_returnsMailtoLink() {
        val text = "Email us at contact@example.com for help"
        val links = findClickableLinks(text)

        assertEquals(1, links.size)
        assertEquals("mailto:contact@example.com", links[0].url)
        assertEquals(12..30, links[0].range)
    }

    @Test
    fun findClickableLinks_withEmailInParentheses_returnsCorrectEmail() {
        val text = "Contact us (support@example.com) today"
        val links = findClickableLinks(text)

        assertEquals(1, links.size)
        assertEquals("mailto:support@example.com", links[0].url)
        assertEquals(12..30, links[0].range)
    }

    @Test
    fun findClickableLinks_withEmailWithDots_returnsCorrectEmail() {
        val text = "Reach out to first.last@company.co.uk"
        val links = findClickableLinks(text)

        assertEquals(1, links.size)
        assertEquals("mailto:first.last@company.co.uk", links[0].url)
        assertEquals(13..36, links[0].range)
    }

    @Test
    fun findClickableLinks_withEmailWithPlus_returnsCorrectEmail() {
        val text = "Send to user+tag@example.com"
        val links = findClickableLinks(text)

        assertEquals(1, links.size)
        assertEquals("mailto:user+tag@example.com", links[0].url)
        assertEquals(8..27, links[0].range)
    }

    @Test
    fun findClickableLinks_withMultipleUrls_returnsAllLinks() {
        val text = "Visit https://first.com and https://second.com"
        val links = findClickableLinks(text)

        assertEquals(2, links.size)
        assertEquals("https://first.com", links[0].url)
        assertEquals(6..22, links[0].range)
        assertEquals("https://second.com", links[1].url)
        assertEquals(28..45, links[1].range)
    }

    @Test
    fun findClickableLinks_withUrlAndEmail_returnsBothLinks() {
        val text = "Visit https://example.com or email contact@example.com"
        val links = findClickableLinks(text)

        assertEquals(2, links.size)
        assertEquals("https://example.com", links[0].url)
        assertEquals(6..24, links[0].range)
        assertEquals("mailto:contact@example.com", links[1].url)
        assertEquals(35..53, links[1].range)
    }

    @Test
    fun findClickableLinks_withNoLinks_returnsEmptyList() {
        val text = "This text has no links at all"
        val links = findClickableLinks(text)

        assertTrue(links.isEmpty())
    }

    @Test
    fun findClickableLinks_withEmptyString_returnsEmptyList() {
        val text = ""
        val links = findClickableLinks(text)

        assertTrue(links.isEmpty())
    }

    @Test
    fun findClickableLinks_withRealFaqAnswer5_returnsEmailLink() {
        val text =
            "Yes, volunteers are always welcome. The app's code is open-source on GitHub, and you can reach the creators at contact@worldwidewaves.net (replies not guaranteed). The best way to help is to spread the word: share photos and videos from events using the suggested hashtags, contact local influencers to amplify the message, talk about it with your community, and most importantly, join the wave!"
        val links = findClickableLinks(text)

        assertEquals(1, links.size)
        assertEquals("mailto:contact@worldwidewaves.net", links[0].url)
        // Verify the email is found and extracted correctly
        assertEquals("contact@worldwidewaves.net", text.substring(links[0].range))
    }

    @Test
    fun findClickableLinks_withRealFaqAnswer6_returnsUrlWithoutPeriod() {
        val text = "You can find a list of proposed actions, activities, and places at https://luma.com/worldwidewaves."
        val links = findClickableLinks(text)

        assertEquals(1, links.size)
        assertEquals("https://luma.com/worldwidewaves", links[0].url)
        // Verify the URL is found and extracted correctly without trailing period
        assertEquals("https://luma.com/worldwidewaves", text.substring(links[0].range))
    }

    @Test
    fun findClickableLinks_maintainsCorrectOrder_whenLinksAppearInText() {
        val text = "First https://first.com then email@test.com and finally https://last.com"
        val links = findClickableLinks(text)

        assertEquals(3, links.size)
        assertEquals("https://first.com", links[0].url)
        assertTrue(links[0].range.first < links[1].range.first)
        assertEquals("mailto:email@test.com", links[1].url)
        assertTrue(links[1].range.first < links[2].range.first)
        assertEquals("https://last.com", links[2].url)
    }

    @Test
    fun findClickableLinks_withUrlWithFragment_returnsFullUrl() {
        val text = "Jump to https://example.com/page#section"
        val links = findClickableLinks(text)

        assertEquals(1, links.size)
        assertEquals("https://example.com/page#section", links[0].url)
    }

    @Test
    fun findClickableLinks_withUrlWithPort_returnsFullUrl() {
        val text = "Connect to https://localhost:8080/api"
        val links = findClickableLinks(text)

        assertEquals(1, links.size)
        assertEquals("https://localhost:8080/api", links[0].url)
    }
}
