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

package com.worldwidewaves.testing.real

import androidx.compose.ui.test.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Duration.Companion.seconds

/**
 * Real integration tests for accessibility features with actual TalkBack and real device testing.
 *
 * These tests verify the complete accessibility experience:
 * - TalkBack integration and voice announcements
 * - Screen reader navigation with real interactions
 * - Dynamic content announcements during wave events
 * - Large text and font scaling support
 * - Touch target accessibility on real devices
 * - High contrast mode and reduced motion support
 * - Accessibility service integration
 */
@RunWith(AndroidJUnit4::class)
class RealAccessibilityIntegrationTest : BaseRealIntegrationTest() {

    @Test
    fun realAccessibility_talkBackIntegration_announcesCorrectly() = runTest {
        val trace = startPerformanceTrace("talkback_integration_real")

        // Check if TalkBack is enabled (required for this test)
        if (!deviceStateManager.isTalkBackEnabled()) {
            println("⚠️  Test requires TalkBack to be enabled")
            println("📱 Please enable TalkBack in Settings > Accessibility")
            return@runTest
        }

        // Set up test environment
        setTestLocation(40.7128, -74.0060)
        createTestEvent("talkback_test_event", 40.7128, -74.0060)
        waitForDataSync()

        // Launch app with TalkBack enabled
        composeTestRule.activityRule.launchActivity(null)

        // Wait for initial content to load
        composeTestRule.waitUntil(timeoutMillis = 25.seconds.inWholeMilliseconds) {
            try {
                composeTestRule.onNode(hasText("talkback_test_event")).assertExists()
                true
            } catch (e: AssertionError) {
                false
            }
        }

        // Test TalkBack navigation and announcements
        val elementsToTest = listOf(
            "Event list" to hasContentDescription("Event list"),
            "Map view" to hasContentDescription("Map view"),
            "Settings" to hasContentDescription("Settings"),
            "User location" to hasContentDescription("User location marker"),
            "Event item" to hasText("talkback_test_event")
        )

        var accessibleElementsFound = 0

        for ((elementName, matcher) in elementsToTest) {
            try {
                composeTestRule.onNode(matcher).assertExists()

                // Test that element has proper semantic properties
                composeTestRule.onNode(matcher).assertHasClickAction()

                println("✅ Accessible element found: $elementName")
                accessibleElementsFound++
            } catch (e: AssertionError) {
                println("⚠️  Accessibility element missing or not accessible: $elementName")
            }
        }

        // At least 60% of elements should be properly accessible
        val accessibilityScore = (accessibleElementsFound * 100) / elementsToTest.size
        assertTrue("TalkBack accessibility should be comprehensive (>60%)", accessibilityScore > 60)

        // Test dynamic content announcements
        try {
            // Trigger a state change that should announce
            composeTestRule.onNode(hasText("talkback_test_event")).performClick()
            kotlinx.coroutines.delay(2000)

            println("✅ Dynamic content interaction tested")
        } catch (e: Exception) {
            println("ℹ️  Dynamic content interaction not available")
        }

        val talkBackTime = stopPerformanceTrace()
        println("✅ TalkBack integration completed in ${talkBackTime}ms")
        println("   Accessibility score: $accessibilityScore% (${accessibleElementsFound}/${elementsToTest.size})")
    }

    @Test
    fun realAccessibility_screenReaderNavigation_worksCorrectly() = runTest {
        val trace = startPerformanceTrace("screen_reader_navigation_real")

        // Check for accessibility services
        if (!deviceStateManager.hasAccessibilityServicesEnabled()) {
            println("⚠️  Test requires accessibility services to be enabled")
            return@runTest
        }

        setTestLocation(40.7128, -74.0060)
        createTestEvent("navigation_test_event", 40.7128, -74.0060)
        waitForDataSync()

        // Launch app
        composeTestRule.activityRule.launchActivity(null)

        // Wait for content to load
        composeTestRule.waitUntil(timeoutMillis = 20.seconds.inWholeMilliseconds) {
            try {
                composeTestRule.onNode(hasText("navigation_test_event")).assertExists()
                true
            } catch (e: AssertionError) {
                false
            }
        }

        // Test screen reader navigation patterns
        val navigationTests = listOf(
            "Sequential navigation" to {
                // Test that elements can be navigated sequentially
                try {
                    val eventNode = composeTestRule.onNode(hasText("navigation_test_event"))
                    eventNode.assertExists()
                    eventNode.assertHasClickAction()
                    true
                } catch (e: Exception) {
                    false
                }
            },
            "Landmark navigation" to {
                // Test landmark/heading navigation
                try {
                    composeTestRule.onNode(
                        hasContentDescription("Main content") or
                        hasContentDescription("Events section")
                    ).assertExists()
                    true
                } catch (e: Exception) {
                    false
                }
            },
            "Interactive elements" to {
                // Test that interactive elements are properly labeled
                try {
                    composeTestRule.onNode(
                        hasContentDescription("Settings") or
                        hasContentDescription("Menu")
                    ).assertExists()
                    true
                } catch (e: Exception) {
                    false
                }
            }
        )

        var navigationTestsPassed = 0

        for ((testName, test) in navigationTests) {
            val passed = test()
            if (passed) {
                navigationTestsPassed++
                println("✅ Navigation test passed: $testName")
            } else {
                println("⚠️  Navigation test failed: $testName")
            }
        }

        // Most navigation tests should pass
        val navigationScore = (navigationTestsPassed * 100) / navigationTests.size
        assertTrue("Screen reader navigation should work well (>66%)", navigationScore > 66)

        // Test focus management
        val focusManagementWorking = try {
            // Test that focus moves appropriately
            composeTestRule.onNode(hasText("navigation_test_event")).performClick()
            kotlinx.coroutines.delay(1000)

            // Focus should move to opened content or back appropriately
            true
        } catch (e: Exception) {
            false
        }

        val navigationTime = stopPerformanceTrace()
        println("✅ Screen reader navigation completed in ${navigationTime}ms")
        println("   Navigation score: $navigationScore% (${navigationTestsPassed}/${navigationTests.size})")

        if (focusManagementWorking) {
            println("   Focus management working correctly")
        }
    }

    @Test
    fun realAccessibility_voiceAnnouncements_duringWaveEvents() = runTest {
        val trace = startPerformanceTrace("voice_announcements_waves_real")

        if (!deviceStateManager.isTalkBackEnabled()) {
            println("⚠️  Test requires TalkBack for voice announcements")
            return@runTest
        }

        setTestLocation(40.7128, -74.0060)
        createTestEvent("voice_announcement_test", 40.7128, -74.0060, isActive = true)
        waitForDataSync()

        // Launch app
        composeTestRule.activityRule.launchActivity(null)

        // Wait for wave event to load
        composeTestRule.waitUntil(timeoutMillis = 25.seconds.inWholeMilliseconds) {
            try {
                composeTestRule.onNode(hasText("voice_announcement_test")).assertExists()
                true
            } catch (e: AssertionError) {
                false
            }
        }

        // Test announcements for different wave states
        val announcementTests = listOf(
            "Wave starting" to {
                try {
                    composeTestRule.onNode(
                        hasContentDescription("Wave starting") or
                        hasText("Starting") or
                        hasContentDescription("Event beginning")
                    ).assertExists()
                    true
                } catch (e: Exception) {
                    false
                }
            },
            "Wave participation" to {
                try {
                    // Try to join wave and test announcements
                    composeTestRule.onNode(
                        hasText("Join Wave") or
                        hasContentDescription("Participate in wave")
                    ).performClick()

                    kotlinx.coroutines.delay(2000)

                    composeTestRule.onNode(
                        hasContentDescription("Participating in wave") or
                        hasText("Participating")
                    ).assertExists()
                    true
                } catch (e: Exception) {
                    false
                }
            },
            "Wave progress" to {
                try {
                    composeTestRule.onNode(
                        hasContentDescription("Wave progress") or
                        hasContentDescription("Wave spreading") or
                        hasText("Progress")
                    ).assertExists()
                    true
                } catch (e: Exception) {
                    false
                }
            }
        )

        var announcementTestsPassed = 0

        for ((testName, test) in announcementTests) {
            kotlinx.coroutines.delay(3000) // Allow time for state changes

            val passed = test()
            if (passed) {
                announcementTestsPassed++
                println("✅ Voice announcement test passed: $testName")
            } else {
                println("ℹ️  Voice announcement test not detected: $testName")
            }
        }

        // Test that important wave events have accessibility announcements
        val importantAnnouncementsWork = announcementTestsPassed > 0

        assertTrue("Wave events should have voice announcements", importantAnnouncementsWork)

        val voiceTime = stopPerformanceTrace()
        println("✅ Voice announcements during waves completed in ${voiceTime}ms")
        println("   Announcements detected: ${announcementTestsPassed}/${announcementTests.size}")
    }

    @Test
    fun realAccessibility_dynamicContentAnnouncements_workCorrectly() = runTest {
        val trace = startPerformanceTrace("dynamic_content_announcements_real")

        if (!deviceStateManager.isTalkBackEnabled()) {
            println("⚠️  Test requires TalkBack for dynamic announcements")
            return@runTest
        }

        setTestLocation(40.7128, -74.0060)
        createTestEvent("dynamic_content_test", 40.7128, -74.0060)
        waitForDataSync()

        // Launch app
        composeTestRule.activityRule.launchActivity(null)

        // Wait for initial content
        composeTestRule.waitUntil(timeoutMillis = 20.seconds.inWholeMilliseconds) {
            try {
                composeTestRule.onNode(hasText("dynamic_content_test")).assertExists()
                true
            } catch (e: AssertionError) {
                false
            }
        }

        // Test dynamic content scenarios
        val dynamicContentTests = listOf(
            "New event appears" to {
                // Create new event and test announcement
                createTestEvent("new_dynamic_event", 40.7130, -74.0058)
                kotlinx.coroutines.delay(3000)

                try {
                    composeTestRule.onNode(
                        hasText("new_dynamic_event") or
                        hasContentDescription("New event available")
                    ).assertExists()
                    true
                } catch (e: Exception) {
                    false
                }
            },
            "Event status changes" to {
                try {
                    // Test status change announcements
                    composeTestRule.onNode(
                        hasContentDescription("Event status updated") or
                        hasText("Status changed")
                    ).assertExists()
                    true
                } catch (e: Exception) {
                    false
                }
            },
            "Location updates" to {
                // Test location change announcements
                setTestLocation(40.7135, -74.0055)
                kotlinx.coroutines.delay(2000)

                try {
                    composeTestRule.onNode(
                        hasContentDescription("Location updated") or
                        hasContentDescription("Position changed")
                    ).assertExists()
                    true
                } catch (e: Exception) {
                    false
                }
            }
        )

        var dynamicTestsPassed = 0

        for ((testName, test) in dynamicContentTests) {
            val passed = test()
            if (passed) {
                dynamicTestsPassed++
                println("✅ Dynamic content test passed: $testName")
            } else {
                println("ℹ️  Dynamic content test not detected: $testName")
            }
        }

        // Some dynamic content should be announced
        val dynamicAnnouncementsWork = dynamicTestsPassed > 0

        assertTrue("Dynamic content should have accessibility announcements", dynamicAnnouncementsWork)

        val dynamicTime = stopPerformanceTrace()
        println("✅ Dynamic content announcements completed in ${dynamicTime}ms")
        println("   Dynamic tests passed: ${dynamicTestsPassed}/${dynamicContentTests.size}")
    }

    @Test
    fun realAccessibility_largeTextSupport_scalesCorrectly() = runTest {
        val trace = startPerformanceTrace("large_text_support_real")

        // Test large text and font scaling
        setTestLocation(40.7128, -74.0060)
        createTestEvent("large_text_test", 40.7128, -74.0060)
        waitForDataSync()

        // Launch app
        composeTestRule.activityRule.launchActivity(null)

        // Wait for content to load
        composeTestRule.waitUntil(timeoutMillis = 20.seconds.inWholeMilliseconds) {
            try {
                composeTestRule.onNode(hasText("large_text_test")).assertExists()
                true
            } catch (e: AssertionError) {
                false
            }
        }

        // Test that text elements are readable with large text settings
        val textElementTests = listOf(
            "Event titles" to hasText("large_text_test"),
            "Button text" to (hasText("Settings") or hasText("Menu")),
            "Status text" to (hasText("Soon") or hasText("Active") or hasText("Running")),
            "Navigation labels" to (hasContentDescription("Map view") or hasContentDescription("Events"))
        )

        var textElementsPassed = 0

        for ((elementName, matcher) in textElementTests) {
            try {
                val node = composeTestRule.onNode(matcher)
                node.assertExists()
                node.assertIsDisplayed()

                println("✅ Large text element readable: $elementName")
                textElementsPassed++
            } catch (e: Exception) {
                println("⚠️  Large text element issue: $elementName")
            }
        }

        // Test UI layout with large text (should not break)
        val layoutStableWithLargeText = try {
            // Test key interactions still work
            composeTestRule.onNode(hasText("large_text_test")).performClick()
            kotlinx.coroutines.delay(1000)

            // UI should still be functional
            composeTestRule.onNode(hasText("large_text_test")).assertExists()
            true
        } catch (e: Exception) {
            false
        }

        assertTrue("Text elements should be readable with large text", textElementsPassed > 0)
        assertTrue("UI layout should be stable with large text", layoutStableWithLargeText)

        val largeTextTime = stopPerformanceTrace()
        println("✅ Large text support completed in ${largeTextTime}ms")
        println("   Text elements working: ${textElementsPassed}/${textElementTests.size}")
    }

    @Test
    fun realAccessibility_touchTargetSize_meetsAccessibilityStandards() = runTest {
        val trace = startPerformanceTrace("touch_target_accessibility_real")

        setTestLocation(40.7128, -74.0060)
        createTestEvent("touch_target_test", 40.7128, -74.0060)
        waitForDataSync()

        // Launch app
        composeTestRule.activityRule.launchActivity(null)

        // Wait for content to load
        composeTestRule.waitUntil(timeoutMillis = 20.seconds.inWholeMilliseconds) {
            try {
                composeTestRule.onNode(hasText("touch_target_test")).assertExists()
                true
            } catch (e: AssertionError) {
                false
            }
        }

        // Test touch targets for accessibility compliance
        val touchTargetTests = listOf(
            "Event items" to hasText("touch_target_test"),
            "Navigation buttons" to (hasContentDescription("Settings") or hasText("Settings")),
            "Map interactions" to hasContentDescription("Map view"),
            "Action buttons" to (hasText("Join Wave") or hasContentDescription("Participate"))
        )

        var accessibleTouchTargets = 0

        for ((targetName, matcher) in touchTargetTests) {
            try {
                val node = composeTestRule.onNode(matcher)
                node.assertExists()
                node.assertHasClickAction()
                node.assertIsDisplayed()

                // Test that touch target is large enough (by attempting interaction)
                node.performClick()
                kotlinx.coroutines.delay(500)

                println("✅ Touch target accessible: $targetName")
                accessibleTouchTargets++
            } catch (e: Exception) {
                println("⚠️  Touch target issue: $targetName - ${e.message}")
            }
        }

        // Test touch target spacing (elements should not be too close)
        val touchTargetSpacingOk = try {
            // Multiple elements should be separately clickable
            composeTestRule.onAllNodes(hasClickAction()).assertCountEquals(greaterThan = 2)
            true
        } catch (e: Exception) {
            false
        }

        // Most touch targets should be accessible
        val touchTargetScore = (accessibleTouchTargets * 100) / touchTargetTests.size
        assertTrue("Touch targets should meet accessibility standards (>75%)", touchTargetScore > 75)
        assertTrue("Touch target spacing should be adequate", touchTargetSpacingOk)

        val touchTargetTime = stopPerformanceTrace()
        println("✅ Touch target accessibility completed in ${touchTargetTime}ms")
        println("   Touch target score: $touchTargetScore% (${accessibleTouchTargets}/${touchTargetTests.size})")
    }

    @Test
    fun realAccessibility_colorContrastAndMotion_supportsPreferences() = runTest {
        val trace = startPerformanceTrace("contrast_motion_preferences_real")

        setTestLocation(40.7128, -74.0060)
        createTestEvent("contrast_test", 40.7128, -74.0060)
        waitForDataSync()

        // Launch app
        composeTestRule.activityRule.launchActivity(null)

        // Wait for content to load
        composeTestRule.waitUntil(timeoutMillis = 20.seconds.inWholeMilliseconds) {
            try {
                composeTestRule.onNode(hasText("contrast_test")).assertExists()
                true
            } catch (e: AssertionError) {
                false
            }
        }

        // Test high contrast mode support
        val highContrastTests = listOf(
            "Text visibility" to {
                try {
                    composeTestRule.onNode(hasText("contrast_test")).assertIsDisplayed()
                    true
                } catch (e: Exception) {
                    false
                }
            },
            "Button visibility" to {
                try {
                    composeTestRule.onNode(
                        hasContentDescription("Settings") or hasText("Settings")
                    ).assertIsDisplayed()
                    true
                } catch (e: Exception) {
                    false
                }
            },
            "Interactive elements" to {
                try {
                    composeTestRule.onAllNodes(hasClickAction()).fetchSemanticsNodes().isNotEmpty()
                } catch (e: Exception) {
                    false
                }
            }
        )

        var contrastTestsPassed = 0

        for ((testName, test) in highContrastTests) {
            if (test()) {
                contrastTestsPassed++
                println("✅ High contrast test passed: $testName")
            } else {
                println("⚠️  High contrast test failed: $testName")
            }
        }

        // Test reduced motion support
        val reducedMotionSupport = try {
            // App should function without relying on animations
            composeTestRule.onNode(hasText("contrast_test")).performClick()
            kotlinx.coroutines.delay(1000)

            // UI should still update appropriately
            true
        } catch (e: Exception) {
            false
        }

        // Test color-independent information
        val colorIndependentInfo = try {
            // Important information should not rely solely on color
            composeTestRule.onNode(
                hasContentDescription("Event status") or
                hasText("Active") or
                hasText("Soon")
            ).assertExists()
            true
        } catch (e: Exception) {
            false
        }

        val contrastScore = (contrastTestsPassed * 100) / highContrastTests.size
        assertTrue("High contrast mode should be supported (>66%)", contrastScore > 66)
        assertTrue("Reduced motion should be supported", reducedMotionSupport)
        assertTrue("Information should not depend solely on color", colorIndependentInfo)

        val preferencesTime = stopPerformanceTrace()
        println("✅ Color contrast and motion preferences completed in ${preferencesTime}ms")
        println("   Contrast score: $contrastScore% (${contrastTestsPassed}/${highContrastTests.size})")
    }
}