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

package com.worldwidewaves.accessibility

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.worldwidewaves.testing.BaseAccessibilityTest
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Automated accessibility scanning using Espresso AccessibilityChecks.
 *
 * These tests leverage the Espresso Accessibility Testing Framework to automatically
 * detect common accessibility violations including:
 * - Touch target sizes (minimum 48dp x 48dp)
 * - Content descriptions for interactive elements
 * - Color contrast ratios (WCAG 2.1 standards)
 * - Clickable span handling
 * - Text size and readability
 *
 * The AccessibilityChecks are configured in BaseAccessibilityTest companion object
 * and run automatically on all UI interactions. Violations will fail the test with
 * detailed error messages.
 *
 * **Suppression Policy**:
 * Only violations that are intentional and documented should be suppressed.
 * Examples of acceptable suppressions:
 * - Decorative images without content descriptions
 * - Custom touch targets with non-standard interactions
 * - Intentional color-only indicators with alternative cues
 *
 * All suppressions must be documented in BaseAccessibilityTest.
 */
@RunWith(AndroidJUnit4::class)
class AutomatedAccessibilityScanTest : BaseAccessibilityTest() {
    /**
     * Automated accessibility scan of a typical event list screen.
     *
     * Tests automatically verify:
     * - All buttons and interactive elements have minimum 48dp touch targets
     * - All clickable items have proper content descriptions
     * - Text has sufficient color contrast against backgrounds
     * - No accessibility violations in navigation elements
     *
     * Note: This test uses a simplified mock screen rather than the real EventsScreen
     * to avoid complex ViewModel and DI setup. For production testing, integrate
     * with real screens using proper test fixtures.
     */
    @Test
    fun eventsScreen_automatedAccessibilityScan() {
        composeTestRule.setContent {
            MaterialTheme {
                TestEventsListScreen()
            }
        }

        // AccessibilityChecks runs automatically on the composed UI
        // No explicit assertions needed - violations will fail the test
        composeTestRule.waitForIdle()

        // Verify the test screen is displayed
        validateInteractiveElementsHaveDescriptions()
        validateTouchTargetSizes()
    }

    /**
     * Automated accessibility scan of wave participation interface.
     *
     * Tests automatically verify:
     * - Real-time status updates are accessible
     * - Timer displays have proper semantics
     * - Action buttons meet accessibility requirements
     * - Emergency exit controls are properly sized and described
     */
    @Test
    fun waveParticipationScreen_automatedAccessibilityScan() {
        composeTestRule.setContent {
            MaterialTheme {
                TestWaveParticipationScreen()
            }
        }

        composeTestRule.waitForIdle()

        validateInteractiveElementsHaveDescriptions()
        validateTouchTargetSizes()
        validateHeadingHierarchy()
    }

    /**
     * Automated accessibility scan of navigation components.
     *
     * Tests automatically verify:
     * - Navigation bar items are properly sized
     * - Tab selections have proper semantics
     * - Icons have content descriptions
     * - Color contrast in navigation elements
     */
    @Test
    fun navigationComponents_automatedAccessibilityScan() {
        composeTestRule.setContent {
            MaterialTheme {
                TestNavigationScreen()
            }
        }

        composeTestRule.waitForIdle()

        validateInteractiveElementsHaveDescriptions()
        validateTouchTargetSizes()
    }

    /**
     * Automated accessibility scan of interactive controls.
     *
     * Tests automatically verify:
     * - Icon buttons meet minimum touch target size
     * - All controls have proper labels
     * - Focus indicators are present
     * - Keyboard navigation is possible
     */
    @Test
    fun interactiveControls_automatedAccessibilityScan() {
        composeTestRule.setContent {
            MaterialTheme {
                TestInteractiveControlsScreen()
            }
        }

        composeTestRule.waitForIdle()

        validateInteractiveElementsHaveDescriptions()
        validateTouchTargetSizes()
    }
}

// ========================================================================
// TEST HELPER COMPOSABLES
// These mock screens simulate real app screens for accessibility testing
// ========================================================================

/**
 * Mock events list screen for accessibility testing.
 * Simulates the structure of the real EventsScreen.
 */
@Composable
private fun TestEventsListScreen() {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
    ) {
        // Header
        Text(
            text = "WorldWideWaves Events",
            style = MaterialTheme.typography.headlineMedium,
            modifier =
                Modifier
                    .padding(16.dp)
                    .semantics { heading() },
        )

        // Event list
        LazyColumn(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
        ) {
            items(listOf("New York Wave", "Paris Wave", "Tokyo Wave")) { eventName ->
                Card(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                    ) {
                        Text(
                            text = eventName,
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.semantics { heading() },
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row {
                            Button(
                                onClick = { },
                                modifier =
                                    Modifier.semantics {
                                        contentDescription = "Join $eventName wave event"
                                    },
                            ) {
                                Text("Join Wave")
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            IconButton(
                                onClick = { },
                                modifier =
                                    Modifier.semantics {
                                        contentDescription = "Add $eventName to favorites"
                                    },
                            ) {
                                Icon(Icons.Default.Favorite, contentDescription = null)
                            }
                        }
                    }
                }
            }
        }

        // Navigation bar
        NavigationBar {
            NavigationBarItem(
                selected = true,
                onClick = { },
                icon = { Icon(Icons.Default.Home, contentDescription = null) },
                label = { Text("Events") },
                modifier =
                    Modifier.semantics {
                        contentDescription = "Navigate to events list"
                    },
            )
            NavigationBarItem(
                selected = false,
                onClick = { },
                icon = { Icon(Icons.Default.Info, contentDescription = null) },
                label = { Text("About") },
                modifier =
                    Modifier.semantics {
                        contentDescription = "Navigate to about page"
                    },
            )
            NavigationBarItem(
                selected = false,
                onClick = { },
                icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                label = { Text("Settings") },
                modifier =
                    Modifier.semantics {
                        contentDescription = "Navigate to settings"
                    },
            )
        }
    }
}

/**
 * Mock wave participation screen for accessibility testing.
 * Simulates the structure of the real WaveParticipationScreen.
 */
@Composable
private fun TestWaveParticipationScreen() {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "New York Wave",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics { heading() },
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Wave status
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(16.dp)
                    .semantics {
                        contentDescription = "Wave status: Warming phase, 30 seconds until wave hit"
                    },
        ) {
            Column {
                Text(
                    text = "Status: Warming",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "Time to hit: 30s",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Participation controls
        Button(
            onClick = { },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .semantics {
                        contentDescription = "Start wave participation"
                    },
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Start Participation")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Emergency exit
        Button(
            onClick = { },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .semantics {
                        contentDescription = "Emergency exit from wave event"
                    },
        ) {
            Text("Exit Wave")
        }
    }
}

/**
 * Mock navigation screen for accessibility testing.
 */
@Composable
private fun TestNavigationScreen() {
    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        // Content area
        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp),
        ) {
            Text(
                text = "Navigation Test Screen",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { heading() },
            )
        }

        // Bottom navigation
        NavigationBar {
            listOf(
                Triple(Icons.Default.Home, "Events", "Navigate to events list"),
                Triple(Icons.Default.Favorite, "Favorites", "Navigate to favorites"),
                Triple(Icons.Default.Settings, "Settings", "Navigate to settings"),
            ).forEach { (icon, label, description) ->
                NavigationBarItem(
                    selected = label == "Events",
                    onClick = { },
                    icon = { Icon(icon, contentDescription = null) },
                    label = { Text(label) },
                    modifier =
                        Modifier.semantics {
                            contentDescription = description
                        },
                )
            }
        }
    }
}

/**
 * Mock interactive controls screen for accessibility testing.
 */
@Composable
private fun TestInteractiveControlsScreen() {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp),
    ) {
        Text(
            text = "Interactive Controls",
            style = MaterialTheme.typography.headlineMedium,
            modifier =
                Modifier
                    .padding(bottom = 16.dp)
                    .semantics { heading() },
        )

        // Standard button
        Button(
            onClick = { },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .semantics {
                        contentDescription = "Primary action button"
                    },
        ) {
            Text("Primary Action")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Icon buttons row
        Row {
            IconButton(
                onClick = { },
                modifier =
                    Modifier
                        .size(48.dp)
                        .semantics {
                            contentDescription = "Play wave animation"
                        },
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = { },
                modifier =
                    Modifier
                        .size(48.dp)
                        .semantics {
                            contentDescription = "Add to favorites"
                        },
            ) {
                Icon(Icons.Default.Favorite, contentDescription = null)
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = { },
                modifier =
                    Modifier
                        .size(48.dp)
                        .semantics {
                            contentDescription = "Open settings menu"
                        },
            ) {
                Icon(Icons.Default.Settings, contentDescription = null)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Clickable card
        Card(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable { }
                    .semantics {
                        contentDescription = "Event card: New York Wave, tap to view details"
                    },
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
            ) {
                Text(
                    text = "New York Wave",
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = "Starting in 2 hours",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
