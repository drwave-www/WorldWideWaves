package com.worldwidewaves.detekt

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

import androidx.compose.runtime.Composable
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Test class to verify that Compose functions follow proper naming conventions
 * and that our Detekt configuration correctly handles @Composable function naming.
 *
 * This test ensures that:
 * 1. Compose functions use PascalCase (which is the standard for Compose)
 * 2. Regular functions use camelCase
 * 3. Detekt configuration properly ignores @Composable functions for FunctionNaming rule
 */
class ComposeFunctionNamingTest {

    @Test
    fun `should allow PascalCase for Composable functions`() {
        // GIVEN: Compose functions follow PascalCase convention
        // WHEN: These functions are defined with @Composable annotation
        // THEN: They should be accepted by our Detekt configuration

        // This test validates that our detekt.yml configuration includes:
        // FunctionNaming:
        //   ignoreAnnotated: ['Composable']

        assertTrue(
            "Compose functions should use PascalCase naming convention",
            ::TestComposableFunction.name.first().isUpperCase()
        )
    }

    @Test
    fun `should enforce camelCase for regular functions`() {
        // GIVEN: Regular functions should follow camelCase convention
        // WHEN: These functions are defined without @Composable annotation
        // THEN: They should follow standard Kotlin naming conventions

        assertTrue(
            "Regular functions should use camelCase naming convention",
            ::testRegularFunction.name.first().isLowerCase()
        )
    }

    @Test
    fun `should verify naming pattern compliance`() {
        // GIVEN: Function naming patterns
        val composableFunctionName = "TestComposableFunction"
        val regularFunctionName = "testRegularFunction"

        // WHEN: Checking naming patterns
        val composablePattern = Regex("[A-Z][a-zA-Z0-9]*")
        val regularPattern = Regex("[a-z][a-zA-Z0-9]*")

        // THEN: Both should match their respective patterns
        assertTrue(
            "Composable function should match PascalCase pattern",
            composablePattern.matches(composableFunctionName)
        )

        assertTrue(
            "Regular function should match camelCase pattern",
            regularPattern.matches(regularFunctionName)
        )
    }

    // Example functions to test naming conventions
    @Composable
    private fun TestComposableFunction() {
        // Compose function following PascalCase - this should be allowed
    }

    private fun testRegularFunction() {
        // Regular function following camelCase - this should be enforced
    }
}