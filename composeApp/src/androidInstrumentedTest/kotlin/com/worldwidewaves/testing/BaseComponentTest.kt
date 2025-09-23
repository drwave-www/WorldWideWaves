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

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.worldwidewaves.shared.events.IWWWEvent.Status
import com.worldwidewaves.shared.events.utils.IClock
import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import kotlin.time.ExperimentalTime

/**
 * Base class for component-level tests.
 *
 * Provides common utilities for testing individual UI components
 * with consistent theming and mock setup.
 */
abstract class BaseComponentTest : BaseInstrumentedTest() {

    protected lateinit var mockClock: IClock

    @OptIn(ExperimentalTime::class)
    @Before
    override fun setUp() {
        super.setUp()

        // Setup common component test mocks
        mockClock = mockk<IClock>(relaxed = true) {
            every { now() } returns kotlin.time.Instant.fromEpochSeconds(1640995200) // 2022-01-01T00:00:00Z
        }
    }

    /**
     * Wraps content in MaterialTheme for consistent component testing.
     */
    protected fun setThemedContent(content: @Composable () -> Unit) {
        composeTestRule.setContent {
            MaterialTheme {
                content()
            }
        }
    }

    /**
     * Helper functions for common component interactions
     */
    protected fun findByText(text: String): SemanticsNodeInteraction =
        composeTestRule.onNodeWithText(text)

    protected fun findByTag(tag: String): SemanticsNodeInteraction =
        composeTestRule.onNodeWithTag(tag)

    protected fun findByContentDescription(description: String): SemanticsNodeInteraction =
        composeTestRule.onNodeWithContentDescription(description)

    /**
     * Common status values for component testing
     */
    companion object {
        val TEST_STATUS_SOON = Status.SOON
        val TEST_STATUS_RUNNING = Status.RUNNING
        val TEST_STATUS_DONE = Status.DONE
    }
}