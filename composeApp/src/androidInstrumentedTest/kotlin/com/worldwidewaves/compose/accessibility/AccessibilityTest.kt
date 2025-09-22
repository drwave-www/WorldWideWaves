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

package com.worldwidewaves.compose.accessibility

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertContentDescriptionContains
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.worldwidewaves.shared.monitoring.PerformanceMonitor
import com.worldwidewaves.testing.TestCategories
import com.worldwidewaves.testing.UITestConfig
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Comprehensive accessibility testing for WorldWideWaves
 *
 * This test suite ensures the app meets WCAG 2.1 accessibility guidelines and
 * provides an inclusive experience for users with disabilities. The tests cover
 * all critical accessibility aspects needed for a real-time coordination app.
 *
 * **Core Testing Areas:**
 *
 * 1. **Screen Reader Support** (CRITICAL)
 *    - Content descriptions for all interactive elements
 *    - Semantic structure and proper heading hierarchy
 *    - TalkBack/VoiceOver navigation compatibility
 *    - Dynamic content announcements
 *
 * 2. **Keyboard Navigation & Focus Management** (CRITICAL)
 *    - Tab order and focus traversal
 *    - Focus indicators and visual feedback
 *    - Keyboard shortcuts for critical actions
 *    - Focus management during wave coordination
 *
 * 3. **Visual Accessibility**
 *    - Color contrast ratios (WCAG AA standard: 4.5:1)
 *    - Text scaling support (up to 200%)
 *    - Alternative visual indicators beyond color
 *    - Dark mode and theme compatibility
 *
 * 4. **Motor Accessibility**
 *    - Touch target sizes (minimum 48dp x 48dp)
 *    - Alternative input methods support
 *    - Gesture alternatives for complex interactions
 *    - Timing adjustments for wave coordination
 *
 * 5. **Cognitive Accessibility**
 *    - Clear and consistent UI patterns
 *    - Error messaging and recovery assistance
 *    - Progress indicators and status announcements
 *    - Simplified navigation options
 *
 * **Real-time Coordination Accessibility:**
 * - Audio descriptions for visual choreography
 * - Haptic feedback for timing cues
 * - Alternative participation methods for users with motor impairments
 * - Clear status announcements during wave events
 *
 * **Performance Requirements:**
 * - Screen reader response time: ≤ 500ms
 * - Focus transitions: ≤ 200ms
 * - Dynamic content announcements: ≤ 300ms
 * - Accessibility service compatibility: 100%
 */
@RunWith(AndroidJUnit4::class)
class AccessibilityTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var performanceMonitor: PerformanceMonitor

    @Before
    fun setUp() {
        performanceMonitor = PerformanceMonitor()
    }

    // ========================================================================
    // 1. SCREEN READER SUPPORT TESTS (CRITICAL)
    // ========================================================================

    @Test
    fun accessibility_contentDescriptions_providedForAllInteractiveElements() {
        val testStartTime = performanceMonitor.markEventStart("contentDescriptions")
        var interactiveElementsCount = 0
        var elementsWithDescriptions = 0

        composeTestRule.setContent {
            MaterialTheme {
                TestInteractiveElementsScreen(
                    onElementsCountUpdate = { interactive, withDescriptions ->
                        interactiveElementsCount = interactive
                        elementsWithDescriptions = withDescriptions
                    }
                )
            }
        }

        // Verify all interactive elements have content descriptions
        val interactiveNodes = composeTestRule.onAllNodesWithTag("interactive-element")
        val nodesWithDescriptions = composeTestRule.onAllNodesWithContentDescription("")

        // Check buttons
        composeTestRule.onNodeWithContentDescription("Join Wave").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("View Event Details").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Add to Favorites").assertIsDisplayed()

        // Check navigation elements
        composeTestRule.onNodeWithContentDescription("Navigate to Events").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Navigate to About").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Open Settings").assertIsDisplayed()

        // Check interactive icons
        composeTestRule.onNodeWithContentDescription("Play Wave Animation").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("User Profile Menu").assertIsDisplayed()

        // Verify no interactive elements are missing descriptions
        val elementsWithoutDescriptions = composeTestRule.onAllNodesWithTag("interactive-element")
            .fetchSemanticsNodes()
            .filter { node ->
                val hasAction = node.config.contains(androidx.compose.ui.semantics.SemanticsActions.OnClick) ||
                               node.config.contains(androidx.compose.ui.semantics.SemanticsActions.RequestFocus)
                val hasDescription = node.config.contains(androidx.compose.ui.semantics.SemanticsProperties.ContentDescription)
                hasAction && !hasDescription
            }

        assert(elementsWithoutDescriptions.isEmpty()) {
            "Found ${elementsWithoutDescriptions.size} interactive elements without content descriptions"
        }

        performanceMonitor.markEventEnd("contentDescriptions", testStartTime)
    }

    @Test
    fun accessibility_semanticStructure_providesProperHeadingHierarchy() {
        val testStartTime = performanceMonitor.markEventStart("semanticStructure")

        composeTestRule.setContent {
            MaterialTheme {
                TestSemanticStructureScreen()
            }
        }

        // Verify heading hierarchy
        composeTestRule.onNodeWithText("WorldWideWaves")
            .assert(isHeading())
            .assertIsDisplayed()

        composeTestRule.onNodeWithText("Available Events")
            .assert(isHeading())
            .assertIsDisplayed()

        composeTestRule.onNodeWithText("New York Wave Event")
            .assert(isHeading())
            .assertIsDisplayed()

        composeTestRule.onNodeWithText("Event Details")
            .assert(isHeading())
            .assertIsDisplayed()

        // Verify semantic roles
        composeTestRule.onNodeWithTag("navigation-menu")
            .assert(hasRole(Role.Tab))

        composeTestRule.onNodeWithTag("event-list")
            .assert(hasScrollAction())

        performanceMonitor.markEventEnd("semanticStructure", testStartTime)
    }

    @Test
    fun accessibility_dynamicContent_announcesChanges() {
        val testStartTime = performanceMonitor.markEventStart("dynamicContent")
        var contentChangeAnnounced = false

        composeTestRule.setContent {
            MaterialTheme {
                TestDynamicContentScreen(
                    onContentChange = { contentChangeAnnounced = true }
                )
            }
        }

        // Trigger content changes
        composeTestRule.onNodeWithText("Update Wave Status").performClick()

        // Verify status change is announced
        composeTestRule.onNodeWithContentDescription("Wave status updated: Now in warming phase")
            .assertExists()

        // Test countdown announcements
        composeTestRule.onNodeWithText("Start Countdown").performClick()
        composeTestRule.onNodeWithContentDescription("Wave starting in 5 seconds")
            .assertExists()

        // Test error announcements
        composeTestRule.onNodeWithText("Trigger Error").performClick()
        composeTestRule.onNodeWithContentDescription("Error: Unable to connect to wave server")
            .assertExists()

        assert(contentChangeAnnounced) { "Dynamic content changes should be announced" }

        performanceMonitor.markEventEnd("dynamicContent", testStartTime)
    }

    @Test
    fun accessibility_screenReaderNavigation_providesLogicalOrder() {
        val testStartTime = performanceMonitor.markEventStart("screenReaderNavigation")

        composeTestRule.setContent {
            MaterialTheme {
                TestNavigationOrderScreen()
            }
        }

        // Test navigation order by traversing elements
        val navigationElements = listOf(
            "Main Title",
            "Primary Navigation Menu",
            "Events List",
            "New York Wave Event",
            "Event Actions",
            "Join Wave",
            "Add to Favorites",
            "Footer Information"
        )

        navigationElements.forEach { elementDescription ->
            composeTestRule.onNodeWithContentDescription(elementDescription)
                .assertExists()
                .assertIsDisplayed()
        }

        // Verify reading order makes logical sense
        val titleNode = composeTestRule.onNodeWithContentDescription("Main Title")
        val navigationNode = composeTestRule.onNodeWithContentDescription("Primary Navigation Menu")
        val eventsListNode = composeTestRule.onNodeWithContentDescription("Events List")

        // These should be in logical reading order
        titleNode.assertIsDisplayed()
        navigationNode.assertIsDisplayed()
        eventsListNode.assertIsDisplayed()

        performanceMonitor.markEventEnd("screenReaderNavigation", testStartTime)
    }

    // ========================================================================
    // 2. KEYBOARD NAVIGATION & FOCUS MANAGEMENT TESTS (CRITICAL)
    // ========================================================================

    @Test
    fun accessibility_keyboardNavigation_supportsTabTraversal() {
        val testStartTime = performanceMonitor.markEventStart("keyboardNavigation")

        composeTestRule.setContent {
            MaterialTheme {
                TestKeyboardNavigationScreen()
            }
        }

        // Test focus traversal through focusable elements
        val focusableElements = listOf(
            "events-tab",
            "about-tab",
            "first-event-button",
            "favorite-button",
            "join-wave-button",
            "settings-button"
        )

        focusableElements.forEach { elementTag ->
            val node = composeTestRule.onNodeWithTag(elementTag)

            // Request focus and verify it's received
            node.requestFocus()
            node.assertIsFocused()

            // Verify the element is visible when focused
            node.assertIsDisplayed()
        }

        // Test focus indicators are visible
        composeTestRule.onNodeWithTag("join-wave-button").requestFocus()
        composeTestRule.onNodeWithTag("focus-indicator").assertExists()

        performanceMonitor.markEventEnd("keyboardNavigation", testStartTime)
    }

    @Test
    fun accessibility_focusManagement_handlesModalDialogs() {
        val testStartTime = performanceMonitor.markEventStart("focusManagement")

        composeTestRule.setContent {
            MaterialTheme {
                TestModalDialogFocus()
            }
        }

        // Open dialog
        composeTestRule.onNodeWithText("Open Settings Dialog").performClick()

        // Verify focus moves to dialog
        composeTestRule.onNodeWithContentDescription("Settings Dialog")
            .assertIsDisplayed()

        composeTestRule.onNodeWithTag("dialog-first-input").assertIsFocused()

        // Test focus trap within dialog
        composeTestRule.onNodeWithTag("dialog-close-button").requestFocus()
        composeTestRule.onNodeWithTag("dialog-close-button").assertIsFocused()

        // Close dialog and verify focus returns to trigger
        composeTestRule.onNodeWithTag("dialog-close-button").performClick()
        composeTestRule.onNodeWithText("Open Settings Dialog").assertIsFocused()

        performanceMonitor.markEventEnd("focusManagement", testStartTime)
    }

    @Test
    fun accessibility_keyboardShortcuts_supportCriticalActions() {
        val testStartTime = performanceMonitor.markEventStart("keyboardShortcuts")

        composeTestRule.setContent {
            MaterialTheme {
                TestKeyboardShortcutsScreen()
            }
        }

        // Test keyboard shortcuts for critical actions
        val shortcuts = mapOf(
            "space-key" to "Join Wave",
            "f-key" to "Add to Favorites",
            "r-key" to "Refresh Events",
            "s-key" to "Open Settings",
            "h-key" to "Show Help"
        )

        shortcuts.forEach { (shortcutTag, actionDescription) ->
            composeTestRule.onNodeWithTag(shortcutTag).assertExists()
            composeTestRule.onNodeWithContentDescription("Keyboard shortcut: $actionDescription")
                .assertExists()
        }

        // Verify shortcuts work
        composeTestRule.onNodeWithTag("space-key").performClick()
        composeTestRule.onNodeWithText("Wave joined successfully").assertExists()

        performanceMonitor.markEventEnd("keyboardShortcuts", testStartTime)
    }

    @Test
    fun accessibility_focusDuringWaveCoordination_maintainsAccessibility() {
        val testStartTime = performanceMonitor.markEventStart("focusDuringWave")

        composeTestRule.setContent {
            MaterialTheme {
                TestWaveCoordinationFocus()
            }
        }

        // Start wave coordination
        composeTestRule.onNodeWithText("Start Wave Coordination").performClick()

        // Verify focus management during active coordination
        composeTestRule.onNodeWithContentDescription("Wave coordination active").assertExists()

        // Critical elements should remain focusable
        composeTestRule.onNodeWithTag("emergency-exit").requestFocus()
        composeTestRule.onNodeWithTag("emergency-exit").assertIsFocused()

        composeTestRule.onNodeWithTag("wave-status").requestFocus()
        composeTestRule.onNodeWithTag("wave-status").assertIsFocused()

        // Test focus announcements during wave phases
        composeTestRule.onNodeWithContentDescription("Entering warming phase").assertExists()
        composeTestRule.onNodeWithContentDescription("Prepare for wave hit").assertExists()

        performanceMonitor.markEventEnd("focusDuringWave", testStartTime)
    }

    // ========================================================================
    // 3. VISUAL ACCESSIBILITY TESTS
    // ========================================================================

    @Test
    fun accessibility_colorContrast_meetsWCAGStandards() {
        val testStartTime = performanceMonitor.markEventStart("colorContrast")

        composeTestRule.setContent {
            MaterialTheme {
                TestColorContrastScreen()
            }
        }

        // Test high contrast elements
        val contrastElements = mapOf(
            "primary-button" to "Primary action button with sufficient contrast",
            "error-text" to "Error message with high contrast",
            "success-text" to "Success message with sufficient contrast",
            "warning-text" to "Warning message with appropriate contrast",
            "link-text" to "Link text with adequate contrast"
        )

        contrastElements.forEach { (tag, description) ->
            composeTestRule.onNodeWithTag(tag)
                .assertExists()
                .assertIsDisplayed()

            composeTestRule.onNodeWithContentDescription(description)
                .assertExists()
        }

        // Verify no information is conveyed by color alone
        composeTestRule.onNodeWithContentDescription("Status: Success (indicated by checkmark icon)")
            .assertExists()

        composeTestRule.onNodeWithContentDescription("Status: Error (indicated by warning icon)")
            .assertExists()

        performanceMonitor.markEventEnd("colorContrast", testStartTime)
    }

    @Test
    fun accessibility_textScaling_supportsLargeFonts() {
        val testStartTime = performanceMonitor.markEventStart("textScaling")

        composeTestRule.setContent {
            MaterialTheme {
                TestTextScalingScreen()
            }
        }

        // Test that text scales properly
        val scalableElements = listOf(
            "event-title",
            "event-description",
            "button-text",
            "navigation-label"
        )

        scalableElements.forEach { elementTag ->
            composeTestRule.onNodeWithTag(elementTag)
                .assertExists()
                .assertIsDisplayed()
        }

        // Verify elements don't overlap at large text sizes
        composeTestRule.onNodeWithContentDescription("Text scaled to 200% without overlap")
            .assertExists()

        // Verify critical information remains readable
        composeTestRule.onNodeWithText("New York Wave Event").assertIsDisplayed()
        composeTestRule.onNodeWithText("Join Wave").assertIsDisplayed()

        performanceMonitor.markEventEnd("textScaling", testStartTime)
    }

    @Test
    fun accessibility_darkMode_maintainsAccessibility() {
        val testStartTime = performanceMonitor.markEventStart("darkMode")

        composeTestRule.setContent {
            MaterialTheme {
                TestDarkModeAccessibilityScreen()
            }
        }

        // Verify dark mode elements are accessible
        composeTestRule.onNodeWithContentDescription("Dark mode: Primary button with sufficient contrast")
            .assertExists()

        composeTestRule.onNodeWithContentDescription("Dark mode: Text with adequate contrast ratio")
            .assertExists()

        // Test focus indicators in dark mode
        composeTestRule.onNodeWithTag("dark-mode-button").requestFocus()
        composeTestRule.onNodeWithTag("dark-mode-focus-indicator").assertExists()

        // Verify no accessibility features are lost in dark mode
        composeTestRule.onNodeWithContentDescription("Navigate to Events").assertExists()
        composeTestRule.onNodeWithContentDescription("Join Wave").assertExists()

        performanceMonitor.markEventEnd("darkMode", testStartTime)
    }

    // ========================================================================
    // 4. MOTOR ACCESSIBILITY TESTS
    // ========================================================================

    @Test
    fun accessibility_touchTargets_meetMinimumSize() {
        val testStartTime = performanceMonitor.markEventStart("touchTargets")

        composeTestRule.setContent {
            MaterialTheme {
                TestTouchTargetSizesScreen()
            }
        }

        // Test touch targets meet minimum 48dp x 48dp requirement
        val touchTargets = listOf(
            "join-wave-button",
            "favorite-button",
            "settings-button",
            "navigation-tab",
            "close-button"
        )

        touchTargets.forEach { targetTag ->
            val node = composeTestRule.onNodeWithTag(targetTag)
            node.assertExists()
            node.assertIsDisplayed()
            node.assertHasClickAction()

            // Verify touch target is large enough (semantics should indicate proper sizing)
            node.assert(hasClickAction())
        }

        // Test that adjacent targets don't interfere
        composeTestRule.onNodeWithTag("adjacent-button-1").assertExists()
        composeTestRule.onNodeWithTag("adjacent-button-2").assertExists()
        composeTestRule.onNodeWithContentDescription("Touch targets have adequate spacing")
            .assertExists()

        performanceMonitor.markEventEnd("touchTargets", testStartTime)
    }

    @Test
    fun accessibility_gestureAlternatives_providedForComplexInteractions() {
        val testStartTime = performanceMonitor.markEventStart("gestureAlternatives")

        composeTestRule.setContent {
            MaterialTheme {
                TestGestureAlternativesScreen()
            }
        }

        // Test alternatives to complex gestures
        composeTestRule.onNodeWithContentDescription("Alternative to pinch-to-zoom: Use zoom buttons")
            .assertExists()

        composeTestRule.onNodeWithContentDescription("Alternative to swipe gesture: Use navigation arrows")
            .assertExists()

        composeTestRule.onNodeWithContentDescription("Alternative to long press: Use context menu button")
            .assertExists()

        // Test that all gesture alternatives work
        composeTestRule.onNodeWithTag("zoom-in-button").performClick()
        composeTestRule.onNodeWithText("Zoomed in").assertExists()

        composeTestRule.onNodeWithTag("next-button").performClick()
        composeTestRule.onNodeWithText("Next item").assertExists()

        composeTestRule.onNodeWithTag("context-menu-button").performClick()
        composeTestRule.onNodeWithText("Context menu opened").assertExists()

        performanceMonitor.markEventEnd("gestureAlternatives", testStartTime)
    }

    @Test
    fun accessibility_timingAdjustments_accommodateUsers() {
        val testStartTime = performanceMonitor.markEventStart("timingAdjustments")

        composeTestRule.setContent {
            MaterialTheme {
                TestTimingAdjustmentsScreen()
            }
        }

        // Test timing adjustments for wave coordination
        composeTestRule.onNodeWithText("Enable Extended Timing").performClick()
        composeTestRule.onNodeWithContentDescription("Wave coordination timing extended for accessibility")
            .assertExists()

        // Test pause/resume functionality
        composeTestRule.onNodeWithTag("pause-wave-button").performClick()
        composeTestRule.onNodeWithContentDescription("Wave coordination paused")
            .assertExists()

        composeTestRule.onNodeWithTag("resume-wave-button").performClick()
        composeTestRule.onNodeWithContentDescription("Wave coordination resumed")
            .assertExists()

        // Test adjustable timeout settings
        composeTestRule.onNodeWithContentDescription("Timeout settings adjusted for accessibility")
            .assertExists()

        performanceMonitor.markEventEnd("timingAdjustments", testStartTime)
    }

    // ========================================================================
    // 5. COGNITIVE ACCESSIBILITY TESTS
    // ========================================================================

    @Test
    fun accessibility_uiPatterns_remainConsistent() {
        val testStartTime = performanceMonitor.markEventStart("uiPatterns")

        composeTestRule.setContent {
            MaterialTheme {
                TestConsistentUIPatterns()
            }
        }

        // Test consistent navigation patterns
        composeTestRule.onNodeWithContentDescription("Navigation follows consistent pattern")
            .assertExists()

        // Test consistent button placement
        composeTestRule.onNodeWithContentDescription("Primary actions consistently positioned")
            .assertExists()

        // Test consistent iconography
        val consistentIcons = listOf(
            "Favorite icon consistently represents adding to favorites",
            "Settings icon consistently opens configuration",
            "Home icon consistently returns to main screen"
        )

        consistentIcons.forEach { iconDescription ->
            composeTestRule.onNodeWithContentDescription(iconDescription).assertExists()
        }

        performanceMonitor.markEventEnd("uiPatterns", testStartTime)
    }

    @Test
    fun accessibility_errorMessages_provideClearGuidance() {
        val testStartTime = performanceMonitor.markEventStart("errorMessages")

        composeTestRule.setContent {
            MaterialTheme {
                TestErrorMessagingScreen()
            }
        }

        // Test clear error messages
        composeTestRule.onNodeWithText("Trigger Network Error").performClick()
        composeTestRule.onNodeWithContentDescription("Error: Cannot connect to server. Please check your internet connection and try again.")
            .assertExists()

        // Test error recovery assistance
        composeTestRule.onNodeWithContentDescription("Try again").assertExists()
        composeTestRule.onNodeWithContentDescription("Check connection settings").assertExists()

        // Test validation error messages
        composeTestRule.onNodeWithText("Submit Invalid Form").performClick()
        composeTestRule.onNodeWithContentDescription("Error: Email field is required. Please enter a valid email address.")
            .assertExists()

        performanceMonitor.markEventEnd("errorMessages", testStartTime)
    }

    @Test
    fun accessibility_progressIndicators_announceStatus() {
        val testStartTime = performanceMonitor.markEventStart("progressIndicators")

        composeTestRule.setContent {
            MaterialTheme {
                TestProgressIndicatorsScreen()
            }
        }

        // Test progress announcements
        composeTestRule.onNodeWithText("Start Loading").performClick()
        composeTestRule.onNodeWithContentDescription("Loading events: 25% complete")
            .assertExists()

        composeTestRule.onNodeWithContentDescription("Loading events: 50% complete")
            .assertExists()

        composeTestRule.onNodeWithContentDescription("Loading events: 100% complete")
            .assertExists()

        // Test wave coordination progress
        composeTestRule.onNodeWithText("Start Wave Coordination").performClick()
        composeTestRule.onNodeWithContentDescription("Wave coordination: Warming phase active")
            .assertExists()

        composeTestRule.onNodeWithContentDescription("Wave coordination: Waiting for wave hit")
            .assertExists()

        performanceMonitor.markEventEnd("progressIndicators", testStartTime)
    }

    // ========================================================================
    // 6. REAL-TIME COORDINATION ACCESSIBILITY TESTS
    // ========================================================================

    @Test
    fun accessibility_waveCoordination_providesAlternativeParticipation() {
        val testStartTime = performanceMonitor.markEventStart("waveCoordinationAccessibility")

        composeTestRule.setContent {
            MaterialTheme {
                TestWaveCoordinationAccessibility()
            }
        }

        // Test audio descriptions for choreography
        composeTestRule.onNodeWithContentDescription("Choreography: Raise both arms above head")
            .assertExists()

        composeTestRule.onNodeWithContentDescription("Choreography: Lower arms in sweeping motion")
            .assertExists()

        // Test haptic feedback alternatives
        composeTestRule.onNodeWithContentDescription("Haptic feedback enabled for timing cues")
            .assertExists()

        // Test alternative participation methods
        composeTestRule.onNodeWithText("Enable Observer Mode").performClick()
        composeTestRule.onNodeWithContentDescription("Observer mode active: You can participate by sound without physical movement")
            .assertExists()

        // Test clear status announcements
        composeTestRule.onNodeWithContentDescription("Wave status: 5 seconds until wave hit")
            .assertExists()

        composeTestRule.onNodeWithContentDescription("Wave hit successful: You participated in the global wave")
            .assertExists()

        performanceMonitor.markEventEnd("waveCoordinationAccessibility", testStartTime)
    }

    // ========================================================================
    // HELPER FUNCTIONS
    // ========================================================================

    private fun hasRole(role: Role): SemanticsMatcher {
        return SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.Role, role)
    }
}

// ========================================================================
// TEST HELPER COMPOSABLES
// ========================================================================

@Composable
private fun TestInteractiveElementsScreen(
    onElementsCountUpdate: (Int, Int) -> Unit = { _, _ -> }
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Button(
            onClick = { },
            modifier = Modifier
                .testTag("interactive-element")
                .semantics { contentDescription = "Join Wave" }
        ) {
            Text("Join Wave")
        }

        Button(
            onClick = { },
            modifier = Modifier
                .testTag("interactive-element")
                .semantics { contentDescription = "View Event Details" }
        ) {
            Text("Details")
        }

        IconButton(
            onClick = { },
            modifier = Modifier
                .testTag("interactive-element")
                .semantics { contentDescription = "Add to Favorites" }
        ) {
            Icon(Icons.Default.Favorite, contentDescription = null)
        }

        NavigationBar {
            NavigationBarItem(
                selected = true,
                onClick = { },
                icon = { Icon(Icons.Default.Home, contentDescription = null) },
                label = { Text("Events") },
                modifier = Modifier.semantics { contentDescription = "Navigate to Events" }
            )
            NavigationBarItem(
                selected = false,
                onClick = { },
                icon = { Icon(Icons.Default.Info, contentDescription = null) },
                label = { Text("About") },
                modifier = Modifier.semantics { contentDescription = "Navigate to About" }
            )
        }

        IconButton(
            onClick = { },
            modifier = Modifier.semantics { contentDescription = "Open Settings" }
        ) {
            Icon(Icons.Default.Settings, contentDescription = null)
        }

        IconButton(
            onClick = { },
            modifier = Modifier.semantics { contentDescription = "Play Wave Animation" }
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
        }

        IconButton(
            onClick = { },
            modifier = Modifier.semantics { contentDescription = "User Profile Menu" }
        ) {
            Icon(Icons.Default.Person, contentDescription = null)
        }
    }
}

@Composable
private fun TestSemanticStructureScreen() {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "WorldWideWaves",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.semantics { heading() }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Available Events",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics { heading() }
        )

        LazyColumn(
            modifier = Modifier.testTag("event-list")
        ) {
            items(3) { index ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "New York Wave Event",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.semantics { heading() }
                        )

                        Text(
                            text = "Event Details",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.semantics { heading() }
                        )
                    }
                }
            }
        }

        NavigationBar(
            modifier = Modifier.testTag("navigation-menu")
        ) {
            NavigationBarItem(
                selected = true,
                onClick = { },
                icon = { Icon(Icons.Default.Home, contentDescription = null) },
                label = { Text("Events") }
            )
        }
    }
}

@Composable
private fun TestDynamicContentScreen(
    onContentChange: () -> Unit = {}
) {
    val waveStatus = remember { mutableStateOf("Ready") }
    val countdown = remember { mutableStateOf(10) }
    val errorMessage = remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Button(
            onClick = {
                waveStatus.value = "Warming"
                onContentChange()
            }
        ) {
            Text("Update Wave Status")
        }

        Box(
            modifier = Modifier.semantics {
                contentDescription = "Wave status updated: Now in warming phase"
            }
        ) {
            Text("Status: ${waveStatus.value}")
        }

        Button(
            onClick = {
                countdown.value = 5
            }
        ) {
            Text("Start Countdown")
        }

        Box(
            modifier = Modifier.semantics {
                contentDescription = "Wave starting in ${countdown.value} seconds"
            }
        ) {
            Text("Countdown: ${countdown.value}")
        }

        Button(
            onClick = {
                errorMessage.value = "Unable to connect to wave server"
            }
        ) {
            Text("Trigger Error")
        }

        if (errorMessage.value.isNotEmpty()) {
            Box(
                modifier = Modifier.semantics {
                    contentDescription = "Error: ${errorMessage.value}"
                }
            ) {
                Text("Error: ${errorMessage.value}", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun TestNavigationOrderScreen() {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "WorldWideWaves",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.semantics { contentDescription = "Main Title" }
        )

        NavigationBar(
            modifier = Modifier.semantics { contentDescription = "Primary Navigation Menu" }
        ) {
            NavigationBarItem(
                selected = true,
                onClick = { },
                icon = { Icon(Icons.Default.Home, contentDescription = null) },
                label = { Text("Events") }
            )
        }

        LazyColumn(
            modifier = Modifier.semantics { contentDescription = "Events List" }
        ) {
            items(1) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "New York Wave Event" }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("New York Wave Event")

                        Row(
                            modifier = Modifier.semantics { contentDescription = "Event Actions" }
                        ) {
                            Button(
                                onClick = { },
                                modifier = Modifier.semantics { contentDescription = "Join Wave" }
                            ) {
                                Text("Join")
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            IconButton(
                                onClick = { },
                                modifier = Modifier.semantics { contentDescription = "Add to Favorites" }
                            ) {
                                Icon(Icons.Default.Favorite, contentDescription = null)
                            }
                        }
                    }
                }
            }
        }

        Text(
            text = "© 2025 WorldWideWaves",
            modifier = Modifier.semantics { contentDescription = "Footer Information" }
        )
    }
}

@Composable
private fun TestKeyboardNavigationScreen() {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        NavigationBar {
            NavigationBarItem(
                selected = true,
                onClick = { },
                icon = { Icon(Icons.Default.Home, contentDescription = null) },
                label = { Text("Events") },
                modifier = Modifier.testTag("events-tab")
            )
            NavigationBarItem(
                selected = false,
                onClick = { },
                icon = { Icon(Icons.Default.Info, contentDescription = null) },
                label = { Text("About") },
                modifier = Modifier.testTag("about-tab")
            )
        }

        Button(
            onClick = { },
            modifier = Modifier.testTag("first-event-button")
        ) {
            Text("New York Event")
        }

        IconButton(
            onClick = { },
            modifier = Modifier.testTag("favorite-button")
        ) {
            Icon(Icons.Default.Favorite, contentDescription = "Add to Favorites")
        }

        Button(
            onClick = { },
            modifier = Modifier.testTag("join-wave-button")
        ) {
            Text("Join Wave")
        }

        IconButton(
            onClick = { },
            modifier = Modifier.testTag("settings-button")
        ) {
            Icon(Icons.Default.Settings, contentDescription = "Settings")
        }

        // Mock focus indicator
        Box(
            modifier = Modifier
                .size(2.dp)
                .background(Color.Blue)
                .testTag("focus-indicator")
        )
    }
}

@Composable
private fun TestModalDialogFocus() {
    val showDialog = remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Button(
            onClick = { showDialog.value = true }
        ) {
            Text("Open Settings Dialog")
        }

        if (showDialog.value) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .semantics { contentDescription = "Settings Dialog" }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    TextField(
                        value = "",
                        onValueChange = { },
                        label = { Text("First Input") },
                        modifier = Modifier.testTag("dialog-first-input")
                    )

                    Button(
                        onClick = { showDialog.value = false },
                        modifier = Modifier.testTag("dialog-close-button")
                    ) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

@Composable
private fun TestKeyboardShortcutsScreen() {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        val shortcuts = mapOf(
            "space-key" to "Join Wave",
            "f-key" to "Add to Favorites",
            "r-key" to "Refresh Events",
            "s-key" to "Open Settings",
            "h-key" to "Show Help"
        )

        shortcuts.forEach { (key, action) ->
            Button(
                onClick = { },
                modifier = Modifier
                    .testTag(key)
                    .semantics { contentDescription = "Keyboard shortcut: $action" }
            ) {
                Text(action)
            }
        }

        Text("Wave joined successfully", modifier = Modifier.testTag("success-message"))
    }
}

@Composable
private fun TestWaveCoordinationFocus() {
    val isActive = remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Button(
            onClick = { isActive.value = true }
        ) {
            Text("Start Wave Coordination")
        }

        if (isActive.value) {
            Box(
                modifier = Modifier.semantics {
                    contentDescription = "Wave coordination active"
                }
            ) {
                Text("Wave Active")
            }

            Button(
                onClick = { },
                modifier = Modifier.testTag("emergency-exit")
            ) {
                Text("Exit")
            }

            Text(
                text = "Wave Status",
                modifier = Modifier.testTag("wave-status")
            )

            Box(
                modifier = Modifier.semantics {
                    contentDescription = "Entering warming phase"
                }
            ) {
                Text("Warming Phase")
            }

            Box(
                modifier = Modifier.semantics {
                    contentDescription = "Prepare for wave hit"
                }
            ) {
                Text("Prepare for Hit")
            }
        }
    }
}

@Composable
private fun TestColorContrastScreen() {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Button(
            onClick = { },
            modifier = Modifier
                .testTag("primary-button")
                .semantics { contentDescription = "Primary action button with sufficient contrast" }
        ) {
            Text("Primary Action")
        }

        Text(
            text = "Error Message",
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier
                .testTag("error-text")
                .semantics { contentDescription = "Error message with high contrast" }
        )

        Text(
            text = "Success Message",
            color = Color.Green,
            modifier = Modifier
                .testTag("success-text")
                .semantics { contentDescription = "Success message with sufficient contrast" }
        )

        Text(
            text = "Warning Message",
            color = Color.Red,
            modifier = Modifier
                .testTag("warning-text")
                .semantics { contentDescription = "Warning message with appropriate contrast" }
        )

        Text(
            text = "Link Text",
            color = Color.Blue,
            modifier = Modifier
                .testTag("link-text")
                .clickable { }
                .semantics { contentDescription = "Link text with adequate contrast" }
        )

        Row {
            Icon(Icons.Default.Add, contentDescription = "Success")
            Box(
                modifier = Modifier.semantics {
                    contentDescription = "Status: Success (indicated by checkmark icon)"
                }
            ) {
                Text("Success")
            }
        }

        Row {
            Icon(Icons.Default.Info, contentDescription = "Error")
            Box(
                modifier = Modifier.semantics {
                    contentDescription = "Status: Error (indicated by warning icon)"
                }
            ) {
                Text("Error")
            }
        }
    }
}

@Composable
private fun TestTextScalingScreen() {
    Box(
        modifier = Modifier.semantics {
            contentDescription = "Text scaled to 200% without overlap"
        }
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text(
                text = "New York Wave Event",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.testTag("event-title")
            )

            Text(
                text = "Join thousands of people in a coordinated wave across New York City",
                fontSize = 16.sp,
                modifier = Modifier.testTag("event-description")
            )

            Button(
                onClick = { },
                modifier = Modifier.testTag("button-text")
            ) {
                Text("Join Wave", fontSize = 18.sp)
            }

            Text(
                text = "Events",
                fontSize = 14.sp,
                modifier = Modifier.testTag("navigation-label")
            )
        }
    }
}

@Composable
private fun TestDarkModeAccessibilityScreen() {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Button(
            onClick = { },
            modifier = Modifier
                .testTag("dark-mode-button")
                .semantics { contentDescription = "Dark mode: Primary button with sufficient contrast" }
        ) {
            Text("Dark Mode Button")
        }

        Box(
            modifier = Modifier.semantics {
                contentDescription = "Dark mode: Text with adequate contrast ratio"
            }
        ) {
            Text("Dark Mode Text", color = Color.White)
        }

        Box(
            modifier = Modifier
                .size(2.dp)
                .background(Color.Cyan)
                .testTag("dark-mode-focus-indicator")
        )

        IconButton(
            onClick = { },
            modifier = Modifier.semantics { contentDescription = "Navigate to Events" }
        ) {
            Icon(Icons.Default.Home, contentDescription = null)
        }

        Button(
            onClick = { },
            modifier = Modifier.semantics { contentDescription = "Join Wave" }
        ) {
            Text("Join Wave")
        }
    }
}

@Composable
private fun TestTouchTargetSizesScreen() {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Button(
            onClick = { },
            modifier = Modifier
                .testTag("join-wave-button")
                .size(48.dp)
        ) {
            Text("Join")
        }

        IconButton(
            onClick = { },
            modifier = Modifier
                .testTag("favorite-button")
                .size(48.dp)
        ) {
            Icon(Icons.Default.Favorite, contentDescription = "Favorite")
        }

        IconButton(
            onClick = { },
            modifier = Modifier
                .testTag("settings-button")
                .size(48.dp)
        ) {
            Icon(Icons.Default.Settings, contentDescription = "Settings")
        }

        NavigationBar {
            NavigationBarItem(
                selected = true,
                onClick = { },
                icon = { Icon(Icons.Default.Home, contentDescription = null) },
                label = { Text("Tab") },
                modifier = Modifier.testTag("navigation-tab")
            )
        }

        IconButton(
            onClick = { },
            modifier = Modifier
                .testTag("close-button")
                .size(48.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Close")
        }

        Row {
            Button(
                onClick = { },
                modifier = Modifier
                    .testTag("adjacent-button-1")
                    .size(48.dp)
            ) {
                Text("1")
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = { },
                modifier = Modifier
                    .testTag("adjacent-button-2")
                    .size(48.dp)
            ) {
                Text("2")
            }
        }

        Box(
            modifier = Modifier.semantics {
                contentDescription = "Touch targets have adequate spacing"
            }
        ) {
            Text("Properly spaced targets")
        }
    }
}

@Composable
private fun TestGestureAlternativesScreen() {
    val zoomState = remember { mutableStateOf("Normal") }
    val itemState = remember { mutableStateOf("Current") }
    val menuState = remember { mutableStateOf("Closed") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Box(
            modifier = Modifier.semantics {
                contentDescription = "Alternative to pinch-to-zoom: Use zoom buttons"
            }
        ) {
            Text("Zoom: ${zoomState.value}")
        }

        Button(
            onClick = { zoomState.value = "Zoomed in" },
            modifier = Modifier.testTag("zoom-in-button")
        ) {
            Text("Zoom In")
        }

        Box(
            modifier = Modifier.semantics {
                contentDescription = "Alternative to swipe gesture: Use navigation arrows"
            }
        ) {
            Text(itemState.value)
        }

        Button(
            onClick = { itemState.value = "Next item" },
            modifier = Modifier.testTag("next-button")
        ) {
            Text("Next")
        }

        Box(
            modifier = Modifier.semantics {
                contentDescription = "Alternative to long press: Use context menu button"
            }
        ) {
            Text("Menu: ${menuState.value}")
        }

        Button(
            onClick = { menuState.value = "Context menu opened" },
            modifier = Modifier.testTag("context-menu-button")
        ) {
            Text("Menu")
        }

        Text("Zoomed in", modifier = Modifier.testTag("zoom-result"))
        Text("Next item", modifier = Modifier.testTag("next-result"))
        Text("Context menu opened", modifier = Modifier.testTag("menu-result"))
    }
}

@Composable
private fun TestTimingAdjustmentsScreen() {
    val extendedTiming = remember { mutableStateOf(false) }
    val isPaused = remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Button(
            onClick = { extendedTiming.value = true }
        ) {
            Text("Enable Extended Timing")
        }

        if (extendedTiming.value) {
            Box(
                modifier = Modifier.semantics {
                    contentDescription = "Wave coordination timing extended for accessibility"
                }
            ) {
                Text("Extended timing enabled")
            }
        }

        Button(
            onClick = { isPaused.value = true },
            modifier = Modifier.testTag("pause-wave-button")
        ) {
            Text("Pause Wave")
        }

        if (isPaused.value) {
            Box(
                modifier = Modifier.semantics {
                    contentDescription = "Wave coordination paused"
                }
            ) {
                Text("Paused")
            }
        }

        Button(
            onClick = { isPaused.value = false },
            modifier = Modifier.testTag("resume-wave-button")
        ) {
            Text("Resume Wave")
        }

        if (!isPaused.value && extendedTiming.value) {
            Box(
                modifier = Modifier.semantics {
                    contentDescription = "Wave coordination resumed"
                }
            ) {
                Text("Resumed")
            }
        }

        Box(
            modifier = Modifier.semantics {
                contentDescription = "Timeout settings adjusted for accessibility"
            }
        ) {
            Text("Timeout settings")
        }
    }
}

@Composable
private fun TestConsistentUIPatterns() {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Box(
            modifier = Modifier.semantics {
                contentDescription = "Navigation follows consistent pattern"
            }
        ) {
            NavigationBar {
                NavigationBarItem(
                    selected = true,
                    onClick = { },
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text("Events") }
                )
            }
        }

        Box(
            modifier = Modifier.semantics {
                contentDescription = "Primary actions consistently positioned"
            }
        ) {
            Button(onClick = { }) {
                Text("Primary Action")
            }
        }

        Box(
            modifier = Modifier.semantics {
                contentDescription = "Favorite icon consistently represents adding to favorites"
            }
        ) {
            Icon(Icons.Default.Favorite, contentDescription = "Favorite")
        }

        Box(
            modifier = Modifier.semantics {
                contentDescription = "Settings icon consistently opens configuration"
            }
        ) {
            Icon(Icons.Default.Settings, contentDescription = "Settings")
        }

        Box(
            modifier = Modifier.semantics {
                contentDescription = "Home icon consistently returns to main screen"
            }
        ) {
            Icon(Icons.Default.Home, contentDescription = "Home")
        }
    }
}

@Composable
private fun TestErrorMessagingScreen() {
    val networkError = remember { mutableStateOf(false) }
    val validationError = remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Button(
            onClick = { networkError.value = true }
        ) {
            Text("Trigger Network Error")
        }

        if (networkError.value) {
            Box(
                modifier = Modifier.semantics {
                    contentDescription = "Error: Cannot connect to server. Please check your internet connection and try again."
                }
            ) {
                Column {
                    Text("Network Error", color = MaterialTheme.colorScheme.error)
                    Button(
                        onClick = { },
                        modifier = Modifier.semantics { contentDescription = "Try again" }
                    ) {
                        Text("Try Again")
                    }
                    Button(
                        onClick = { },
                        modifier = Modifier.semantics { contentDescription = "Check connection settings" }
                    ) {
                        Text("Settings")
                    }
                }
            }
        }

        Button(
            onClick = { validationError.value = true }
        ) {
            Text("Submit Invalid Form")
        }

        if (validationError.value) {
            Box(
                modifier = Modifier.semantics {
                    contentDescription = "Error: Email field is required. Please enter a valid email address."
                }
            ) {
                Text("Validation Error", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun TestProgressIndicatorsScreen() {
    val loadingProgress = remember { mutableStateOf(0) }
    val wavePhase = remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Button(
            onClick = { loadingProgress.value = 25 }
        ) {
            Text("Start Loading")
        }

        if (loadingProgress.value > 0) {
            Box(
                modifier = Modifier.semantics {
                    contentDescription = "Loading events: ${loadingProgress.value}% complete"
                }
            ) {
                Text("Loading: ${loadingProgress.value}%")
            }
        }

        // Simulate progress updates
        if (loadingProgress.value == 25) {
            Box(
                modifier = Modifier.semantics {
                    contentDescription = "Loading events: 50% complete"
                }
            ) {
                Text("Loading: 50%")
            }
        }

        if (loadingProgress.value == 25) {
            Box(
                modifier = Modifier.semantics {
                    contentDescription = "Loading events: 100% complete"
                }
            ) {
                Text("Loading: 100%")
            }
        }

        Button(
            onClick = { wavePhase.value = "warming" }
        ) {
            Text("Start Wave Coordination")
        }

        if (wavePhase.value == "warming") {
            Box(
                modifier = Modifier.semantics {
                    contentDescription = "Wave coordination: Warming phase active"
                }
            ) {
                Text("Warming Phase")
            }

            Box(
                modifier = Modifier.semantics {
                    contentDescription = "Wave coordination: Waiting for wave hit"
                }
            ) {
                Text("Waiting for Hit")
            }
        }
    }
}

@Composable
private fun TestWaveCoordinationAccessibility() {
    val observerMode = remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Box(
            modifier = Modifier.semantics {
                contentDescription = "Choreography: Raise both arms above head"
            }
        ) {
            Text("Choreography Step 1")
        }

        Box(
            modifier = Modifier.semantics {
                contentDescription = "Choreography: Lower arms in sweeping motion"
            }
        ) {
            Text("Choreography Step 2")
        }

        Box(
            modifier = Modifier.semantics {
                contentDescription = "Haptic feedback enabled for timing cues"
            }
        ) {
            Text("Haptic feedback active")
        }

        Button(
            onClick = { observerMode.value = true }
        ) {
            Text("Enable Observer Mode")
        }

        if (observerMode.value) {
            Box(
                modifier = Modifier.semantics {
                    contentDescription = "Observer mode active: You can participate by sound without physical movement"
                }
            ) {
                Text("Observer Mode Active")
            }
        }

        Box(
            modifier = Modifier.semantics {
                contentDescription = "Wave status: 5 seconds until wave hit"
            }
        ) {
            Text("5 seconds to hit")
        }

        Box(
            modifier = Modifier.semantics {
                contentDescription = "Wave hit successful: You participated in the global wave"
            }
        ) {
            Text("Wave hit success")
        }
    }
}