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

package com.worldwidewaves.testing

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

/**
 * Helper functions for E2E Firebase Test Lab UI tests.
 * Provides reusable test actions and assertions.
 */
object E2ETestHelpers {
    // ============================================================
    // FILTER INTERACTIONS
    // ============================================================

    fun clickAllEventsFilter(composeTestRule: ComposeContentTestRule) {
        composeTestRule.onNodeWithTag("FilterButton_All").performClick()
        composeTestRule.waitForIdle()
    }

    fun clickFavoritesFilter(composeTestRule: ComposeContentTestRule) {
        composeTestRule.onNodeWithTag("FilterButton_Favorites").performClick()
        composeTestRule.waitForIdle()
    }

    fun clickDownloadedFilter(composeTestRule: ComposeContentTestRule) {
        composeTestRule.onNodeWithTag("FilterButton_Downloaded").performClick()
        composeTestRule.waitForIdle()
    }

    // ============================================================
    // EVENT INTERACTIONS
    // ============================================================

    fun clickEvent(
        composeTestRule: ComposeContentTestRule,
        eventId: String,
    ) {
        composeTestRule.onNodeWithTag("Event_$eventId").performClick()
        composeTestRule.waitForIdle()
    }

    fun clickFavoriteButton(
        composeTestRule: ComposeContentTestRule,
        eventId: String,
    ) {
        composeTestRule.onNodeWithTag("EventFavoriteButton_$eventId").performClick()
        composeTestRule.waitForIdle()
    }

    fun scrollEventsList(composeTestRule: ComposeContentTestRule) {
        composeTestRule
            .onNodeWithTag("EventsList")
            .performTouchInput { swipeUp() }
        composeTestRule.waitForIdle()
    }

    // ============================================================
    // WAVE INTERACTIONS
    // ============================================================

    fun clickJoinWaveButton(composeTestRule: ComposeContentTestRule) {
        composeTestRule.onNodeWithTag("JoinWaveButton").performClick()
        composeTestRule.waitForIdle()
    }

    // ============================================================
    // ABOUT SCREEN INTERACTIONS
    // ============================================================

    fun clickAboutInfoTab(composeTestRule: ComposeContentTestRule) {
        composeTestRule.onNodeWithTag("AboutTab_Info").performClick()
        composeTestRule.waitForIdle()
    }

    fun clickAboutFaqTab(composeTestRule: ComposeContentTestRule) {
        composeTestRule.onNodeWithTag("AboutTab_FAQ").performClick()
        composeTestRule.waitForIdle()
    }

    fun clickFaqItem(
        composeTestRule: ComposeContentTestRule,
        index: Int,
    ) {
        composeTestRule.onNodeWithTag("FaqItem_$index").performClick()
        composeTestRule.waitForIdle()
    }

    fun scrollFaqList(composeTestRule: ComposeContentTestRule) {
        composeTestRule
            .onNodeWithTag("FaqList")
            .performTouchInput { swipeUp() }
        composeTestRule.waitForIdle()
    }

    // ============================================================
    // ASSERTIONS
    // ============================================================

    fun assertEventListDisplayed(composeTestRule: ComposeContentTestRule) {
        composeTestRule.onNodeWithTag("EventsList").assertIsDisplayed()
    }

    fun assertEventDisplayed(
        composeTestRule: ComposeContentTestRule,
        eventId: String,
    ) {
        composeTestRule.onNodeWithTag("Event_$eventId").assertIsDisplayed()
    }

    fun assertFilterSelected(
        composeTestRule: ComposeContentTestRule,
        filterTag: String,
    ) {
        composeTestRule.onNodeWithTag(filterTag).assertIsDisplayed()
    }

    fun assertEmptyStateDisplayed(
        composeTestRule: ComposeContentTestRule,
        message: String,
    ) {
        composeTestRule.onNodeWithText(message).assertIsDisplayed()
    }

    fun assertJoinWaveButtonDisplayed(composeTestRule: ComposeContentTestRule) {
        composeTestRule.onNodeWithTag("JoinWaveButton").assertIsDisplayed()
    }

    fun assertAboutTabDisplayed(
        composeTestRule: ComposeContentTestRule,
        tabTag: String,
    ) {
        composeTestRule.onNodeWithTag(tabTag).assertIsDisplayed()
    }

    fun assertFaqItemDisplayed(
        composeTestRule: ComposeContentTestRule,
        index: Int,
    ) {
        composeTestRule.onNodeWithTag("FaqItem_$index").assertIsDisplayed()
    }

    // ============================================================
    // WAIT UTILITIES
    // ============================================================

    /**
     * Waits for a node with the given tag to appear.
     *
     * @param composeTestRule The Compose test rule
     * @param tag The testTag to wait for
     * @param timeoutMs Timeout in milliseconds
     * @return true if node appeared, false if timeout
     */
    fun waitForNodeWithTag(
        composeTestRule: ComposeContentTestRule,
        tag: String,
        timeoutMs: Long = 5000,
    ): Boolean {
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            try {
                composeTestRule.onNodeWithTag(tag).assertIsDisplayed()
                return true
            } catch (e: AssertionError) {
                runBlocking { delay(100) }
            }
        }
        return false
    }

    /**
     * Waits for a node with the given text to appear.
     *
     * @param composeTestRule The Compose test rule
     * @param text The text to wait for
     * @param timeoutMs Timeout in milliseconds
     * @return true if node appeared, false if timeout
     */
    fun waitForNodeWithText(
        composeTestRule: ComposeContentTestRule,
        text: String,
        timeoutMs: Long = 5000,
    ): Boolean {
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            try {
                composeTestRule.onNodeWithText(text).assertIsDisplayed()
                return true
            } catch (e: AssertionError) {
                runBlocking { delay(100) }
            }
        }
        return false
    }
}
