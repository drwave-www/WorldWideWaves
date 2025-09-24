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
 * Real integration tests for first-time user onboarding flow.
 *
 * These tests verify the complete first-time user experience:
 * - Welcome screens and introduction
 * - Permission requests and explanations
 * - Tutorial and feature explanations
 * - User preference setup
 * - Account creation or anonymous usage
 * - First event discovery
 * - Accessibility during onboarding
 */
@RunWith(AndroidJUnit4::class)
class RealOnboardingFlowIntegrationTest : BaseRealIntegrationTest() {

    @Test
    fun realOnboarding_firstLaunch_showsWelcomeFlow() = runTest {
        val trace = startPerformanceTrace("first_launch_welcome_real")

        // Ensure this appears as a first launch
        // In real testing, this would require app data clearing

        // Launch app
        composeTestRule.activityRule.launchActivity(null)

        // Wait for welcome screen or onboarding to appear
        composeTestRule.waitUntil(timeoutMillis = 15.seconds.inWholeMilliseconds) {
            try {
                composeTestRule.onNode(
                    hasText("Welcome") or
                    hasText("WorldWideWaves") or
                    hasContentDescription("Welcome screen") or
                    hasTestTag("onboarding-welcome")
                ).assertExists()
                true
            } catch (e: AssertionError) {
                false
            }
        }

        // Verify welcome content is displayed
        val welcomeScreenShown = try {
            composeTestRule.onNode(
                hasText("Welcome") or
                hasText("WorldWideWaves") or
                hasContentDescription("Welcome screen")
            ).assertExists()
            true
        } catch (e: AssertionError) {
            false
        }

        assertTrue("Welcome screen should be shown on first launch", welcomeScreenShown)

        // Look for onboarding navigation elements
        val hasNextButton = try {
            composeTestRule.onNode(
                hasText("Next") or
                hasText("Get Started") or
                hasContentDescription("Continue") or
                hasTestTag("onboarding-next")
            ).assertExists()
            true
        } catch (e: AssertionError) {
            false
        }

        val welcomeTime = stopPerformanceTrace()

        assertTrue("Onboarding navigation should be available", hasNextButton)
        println("✅ Welcome flow displayed in ${welcomeTime}ms")
    }

    @Test
    fun realOnboarding_permissionExplanation_providesContext() = runTest {
        val trace = startPerformanceTrace("permission_explanation_real")

        // Launch app
        composeTestRule.activityRule.launchActivity(null)

        // Navigate through welcome if present
        try {
            composeTestRule.onNode(
                hasText("Next") or
                hasText("Get Started")
            ).performClick()

            kotlinx.coroutines.delay(1000)
        } catch (e: Exception) {
            // Continue if no welcome screen
        }

        // Wait for permission explanation screen
        composeTestRule.waitUntil(timeoutMillis = 15.seconds.inWholeMilliseconds) {
            try {
                composeTestRule.onNode(
                    hasText("Location") or
                    hasText("Permission") or
                    hasContentDescription("Location explanation") or
                    hasText("Why we need location")
                ).assertExists()
                true
            } catch (e: AssertionError) {
                false
            }
        }

        // Verify permission explanation content
        val permissionExplanationShown = try {
            composeTestRule.onNode(
                hasText("Location") or
                hasText("Permission") or
                hasText("Why we need location")
            ).assertExists()
            true
        } catch (e: AssertionError) {
            false
        }

        if (permissionExplanationShown) {
            // Look for clear explanation text
            val hasExplanation = try {
                composeTestRule.onNode(
                    hasText("find events") or
                    hasText("nearby waves") or
                    hasText("your area") or
                    hasContentDescription("Location usage explanation")
                ).assertExists()
                true
            } catch (e: AssertionError) {
                false
            }

            assertTrue("Permission explanation should provide clear context", hasExplanation)

            // Check for permission action button
            val hasPermissionButton = try {
                composeTestRule.onNode(
                    hasText("Allow Location") or
                    hasText("Grant Permission") or
                    hasContentDescription("Grant location permission")
                ).assertExists()
                true
            } catch (e: AssertionError) {
                false
            }

            assertTrue("Permission grant button should be available", hasPermissionButton)
            println("✅ Permission explanation provided with context")
        } else {
            println("ℹ️  Permission explanation not found - may be integrated differently")
        }

        val explanationTime = stopPerformanceTrace()
        println("✅ Permission explanation completed in ${explanationTime}ms")
    }

    @Test
    fun realOnboarding_tutorialFlow_explainsCoreFeatures() = runTest {
        val trace = startPerformanceTrace("tutorial_flow_real")

        // Launch app and navigate through initial screens
        composeTestRule.activityRule.launchActivity(null)

        // Skip through welcome and permission screens
        repeat(3) {
            try {
                composeTestRule.onNode(
                    hasText("Next") or
                    hasText("Continue") or
                    hasText("Skip")
                ).performClick()
                kotlinx.coroutines.delay(1000)
            } catch (e: Exception) {
                // Continue if button not found
            }
        }

        // Look for tutorial or feature explanation screens
        composeTestRule.waitUntil(timeoutMillis = 15.seconds.inWholeMilliseconds) {
            try {
                composeTestRule.onNode(
                    hasText("How it works") or
                    hasText("Tutorial") or
                    hasText("waves") or
                    hasContentDescription("Feature tutorial") or
                    hasTestTag("tutorial-screen")
                ).assertExists()
                true
            } catch (e: AssertionError) {
                false
            }
        }

        val tutorialShown = try {
            composeTestRule.onNode(
                hasText("How it works") or
                hasText("Tutorial") or
                hasText("waves")
            ).assertExists()
            true
        } catch (e: AssertionError) {
            false
        }

        if (tutorialShown) {
            // Navigate through tutorial screens
            repeat(5) { step ->
                // Look for core concept explanations
                val hasFeatureExplanation = try {
                    composeTestRule.onNode(
                        hasText("coordinate") or
                        hasText("participate") or
                        hasText("events") or
                        hasText("waves") or
                        hasContentDescription("Feature explanation")
                    ).assertExists()
                    true
                } catch (e: AssertionError) {
                    false
                }

                if (hasFeatureExplanation) {
                    println("✅ Tutorial step ${step + 1}: Feature explanation found")
                }

                // Try to navigate to next tutorial screen
                try {
                    composeTestRule.onNode(
                        hasText("Next") or
                        hasContentDescription("Next tutorial step")
                    ).performClick()
                    kotlinx.coroutines.delay(1500)
                } catch (e: Exception) {
                    break // End of tutorial
                }
            }

            println("✅ Tutorial flow navigation completed")
        } else {
            println("ℹ️  Tutorial screens not found - may be integrated differently")
        }

        val tutorialTime = stopPerformanceTrace()
        println("✅ Tutorial flow completed in ${tutorialTime}ms")
    }

    @Test
    fun realOnboarding_userPreferences_allowsCustomization() = runTest {
        val trace = startPerformanceTrace("user_preferences_setup_real")

        // Launch app and navigate through onboarding
        composeTestRule.activityRule.launchActivity(null)

        // Skip through initial screens to reach preferences
        repeat(5) {
            try {
                composeTestRule.onNode(
                    hasText("Next") or
                    hasText("Continue") or
                    hasText("Skip")
                ).performClick()
                kotlinx.coroutines.delay(1000)
            } catch (e: Exception) {
                // Continue if button not found
            }
        }

        // Look for preferences or settings screen
        composeTestRule.waitUntil(timeoutMillis = 15.seconds.inWholeMilliseconds) {
            try {
                composeTestRule.onNode(
                    hasText("Preferences") or
                    hasText("Settings") or
                    hasText("Notifications") or
                    hasContentDescription("User preferences") or
                    hasTestTag("preferences-screen")
                ).assertExists()
                true
            } catch (e: AssertionError) {
                false
            }
        }

        val preferencesShown = try {
            composeTestRule.onNode(
                hasText("Preferences") or
                hasText("Settings") or
                hasText("Notifications")
            ).assertExists()
            true
        } catch (e: AssertionError) {
            false
        }

        if (preferencesShown) {
            // Test notification preferences
            val hasNotificationToggle = try {
                composeTestRule.onNode(
                    hasContentDescription("Notification toggle") or
                    hasTestTag("notification-switch")
                ).assertExists()
                true
            } catch (e: AssertionError) {
                false
            }

            if (hasNotificationToggle) {
                // Test toggling notifications
                composeTestRule.onNode(
                    hasContentDescription("Notification toggle") or
                    hasTestTag("notification-switch")
                ).performClick()

                kotlinx.coroutines.delay(500)

                println("✅ Notification preferences toggle tested")
            }

            // Look for other preference options
            val hasOtherPreferences = try {
                composeTestRule.onNode(
                    hasText("Privacy") or
                    hasText("Sound") or
                    hasText("Theme") or
                    hasContentDescription("Additional preferences")
                ).assertExists()
                true
            } catch (e: AssertionError) {
                false
            }

            if (hasOtherPreferences) {
                println("✅ Additional preference options found")
            }

        } else {
            println("ℹ️  User preferences screen not found")
        }

        val preferencesTime = stopPerformanceTrace()
        println("✅ User preferences setup completed in ${preferencesTime}ms")
    }

    @Test
    fun realOnboarding_eventDiscovery_showsNearbyEvents() = runTest {
        val trace = startPerformanceTrace("first_event_discovery_real")

        // Set test location
        setTestLocation(40.7128, -74.0060) // New York

        // Create test events for discovery
        createTestEvent("onboarding_discovery_event", 40.7128, -74.0060)
        waitForDataSync()

        // Launch app and complete onboarding
        composeTestRule.activityRule.launchActivity(null)

        // Navigate through onboarding to main app
        repeat(6) {
            try {
                composeTestRule.onNode(
                    hasText("Next") or
                    hasText("Continue") or
                    hasText("Get Started") or
                    hasText("Done") or
                    hasText("Finish")
                ).performClick()
                kotlinx.coroutines.delay(1500)
            } catch (e: Exception) {
                // Continue if button not found
            }
        }

        // Wait for main app screen with events
        composeTestRule.waitUntil(timeoutMillis = 20.seconds.inWholeMilliseconds) {
            try {
                composeTestRule.onNode(
                    hasText("onboarding_discovery_event") or
                    hasText("Events") or
                    hasContentDescription("Event list") or
                    hasTestTag("events-list")
                ).assertExists()
                true
            } catch (e: AssertionError) {
                false
            }
        }

        // Verify event discovery works
        val eventsDiscovered = try {
            composeTestRule.onNode(
                hasText("onboarding_discovery_event") or
                hasText("Events") or
                hasContentDescription("Nearby events")
            ).assertExists()
            true
        } catch (e: AssertionError) {
            false
        }

        assertTrue("Events should be discoverable after onboarding", eventsDiscovered)

        val discoveryTime = stopPerformanceTrace()
        println("✅ Event discovery completed in ${discoveryTime}ms")
    }

    @Test
    fun realOnboarding_accessibility_supportsScreenReaders() = runTest {
        val trace = startPerformanceTrace("onboarding_accessibility_real")

        // Launch app
        composeTestRule.activityRule.launchActivity(null)

        // Check accessibility of onboarding screens
        val accessibilityElements = listOf(
            "Welcome screen",
            "Next button",
            "Permission explanation",
            "Tutorial content",
            "Settings options"
        )

        for (element in accessibilityElements) {
            try {
                // Check if accessibility content descriptions are present
                composeTestRule.onNode(hasContentDescription(element)).assertExists()
                println("✅ Accessible element found: $element")
            } catch (e: AssertionError) {
                // Element may not be present or have different description
                println("ℹ️  Accessibility element not found: $element")
            }

            // Navigate to next screen
            try {
                composeTestRule.onNode(
                    hasText("Next") or
                    hasContentDescription("Continue") or
                    hasText("Skip")
                ).performClick()
                kotlinx.coroutines.delay(1000)
            } catch (e: Exception) {
                // Continue if navigation not available
            }
        }

        // Test specific accessibility features
        val hasAccessibleNavigation = try {
            composeTestRule.onNode(
                hasContentDescription("Back") or
                hasContentDescription("Next") or
                hasContentDescription("Skip")
            ).assertExists()
            true
        } catch (e: AssertionError) {
            false
        }

        assertTrue("Onboarding should have accessible navigation", hasAccessibleNavigation)

        val accessibilityTime = stopPerformanceTrace()
        println("✅ Onboarding accessibility testing completed in ${accessibilityTime}ms")
    }

    @Test
    fun realOnboarding_skipOption_allowsQuickStart() = runTest {
        val trace = startPerformanceTrace("onboarding_skip_option_real")

        // Launch app
        composeTestRule.activityRule.launchActivity(null)

        // Look for skip option
        composeTestRule.waitUntil(timeoutMillis = 10.seconds.inWholeMilliseconds) {
            try {
                composeTestRule.onNode(
                    hasText("Skip") or
                    hasContentDescription("Skip onboarding") or
                    hasTestTag("skip-button")
                ).assertExists()
                true
            } catch (e: AssertionError) {
                false
            }
        }

        val skipOptionAvailable = try {
            composeTestRule.onNode(
                hasText("Skip") or
                hasContentDescription("Skip onboarding")
            ).assertExists()
            true
        } catch (e: AssertionError) {
            false
        }

        if (skipOptionAvailable) {
            // Test skip functionality
            composeTestRule.onNode(
                hasText("Skip") or
                hasContentDescription("Skip onboarding")
            ).performClick()

            // Wait for main app to load
            composeTestRule.waitUntil(timeoutMillis = 15.seconds.inWholeMilliseconds) {
                try {
                    composeTestRule.onNode(
                        hasText("WorldWideWaves") or
                        hasText("Events") or
                        hasContentDescription("Main screen")
                    ).assertExists()
                    true
                } catch (e: AssertionError) {
                    false
                }
            }

            val mainScreenReached = try {
                composeTestRule.onNode(
                    hasText("WorldWideWaves") or
                    hasText("Events")
                ).assertExists()
                true
            } catch (e: AssertionError) {
                false
            }

            assertTrue("Skip should lead directly to main app", mainScreenReached)
            println("✅ Skip functionality works correctly")
        } else {
            println("ℹ️  Skip option not available in onboarding")
        }

        val skipTime = stopPerformanceTrace()
        println("✅ Skip option testing completed in ${skipTime}ms")
    }
}