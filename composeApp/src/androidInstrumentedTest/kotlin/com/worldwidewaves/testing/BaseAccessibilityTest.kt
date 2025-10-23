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

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.isHeading
import androidx.test.espresso.accessibility.AccessibilityChecks
import org.junit.Assert.assertTrue

/**
 * Base class for accessibility tests.
 *
 * Provides common utilities and patterns for WCAG 2.1 compliance testing.
 * Focuses on inclusive design and assistive technology support.
 */
abstract class BaseAccessibilityTest : BaseComponentTest() {
    /**
     * Validates that all interactive elements have proper content descriptions.
     * Critical for screen reader accessibility.
     */
    protected fun validateInteractiveElementsHaveDescriptions() {
        val allNodes =
            composeTestRule
                .onAllNodes(hasClickAction().or(hasScrollAction()))
                .fetchSemanticsNodes()

        val elementsWithoutDescriptions =
            allNodes.filter { node ->
                val hasAction =
                    node.config.contains(androidx.compose.ui.semantics.SemanticsActions.OnClick) ||
                        node.config.contains(androidx.compose.ui.semantics.SemanticsActions.RequestFocus)
                val hasDescription = node.config.contains(androidx.compose.ui.semantics.SemanticsProperties.ContentDescription)
                hasAction && !hasDescription
            }

        assertTrue(
            "Found ${elementsWithoutDescriptions.size} interactive elements without content descriptions",
            elementsWithoutDescriptions.isEmpty(),
        )
    }

    /**
     * Validates minimum touch target sizes for accessibility.
     * WCAG 2.1 requires 44dp minimum touch targets.
     */
    protected fun validateTouchTargetSizes(minimumSizeDp: Float = 44f) {
        val allClickableNodes =
            composeTestRule
                .onAllNodes(hasClickAction())
                .fetchSemanticsNodes()

        allClickableNodes.forEach { node ->
            val bounds = node.boundsInRoot
            val widthDp = bounds.width
            val heightDp = bounds.height

            assertTrue(
                "Touch target too small: ${widthDp}dp x ${heightDp}dp (minimum: ${minimumSizeDp}dp)",
                widthDp >= minimumSizeDp && heightDp >= minimumSizeDp,
            )
        }
    }

    /**
     * Validates proper heading hierarchy for screen readers.
     * Ensures logical content structure.
     */
    protected fun validateHeadingHierarchy() {
        val headingNodes =
            composeTestRule
                .onAllNodes(isHeading())
                .fetchSemanticsNodes()

        assertTrue(
            "No heading elements found - content structure may be unclear for screen readers",
            headingNodes.isNotEmpty(),
        )
    }

    /**
     * Custom semantic matchers for accessibility testing
     */
    protected fun hasInteractiveAction(): SemanticsMatcher =
        SemanticsMatcher("has interactive action") { node ->
            node.config.contains(androidx.compose.ui.semantics.SemanticsActions.OnClick) ||
                node.config.contains(androidx.compose.ui.semantics.SemanticsActions.RequestFocus) ||
                node.config.contains(androidx.compose.ui.semantics.SemanticsActions.OnLongClick)
        }

    protected fun hasContentDescription(): SemanticsMatcher =
        SemanticsMatcher("has content description") { node ->
            node.config.contains(androidx.compose.ui.semantics.SemanticsProperties.ContentDescription)
        }

    /**
     * Common accessibility test timeouts - longer for assistive technology compatibility
     */
    companion object {
        const val ACCESSIBILITY_TIMEOUT = 5000L
        const val SCREEN_READER_TIMEOUT = 3000L
        const val FOCUS_TIMEOUT = 2000L

        init {
            // Enable Espresso accessibility checks for automated scanning
            // This runs on all Espresso interactions automatically
            // Note: Compose UI tests don't trigger Espresso view matchers,
            // so these checks primarily apply when mixing Compose with traditional Views
            AccessibilityChecks
                .enable()
                .setRunChecksFromRootView(true)
        }
    }
}
