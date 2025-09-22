package com.worldwidewaves.shared.testing

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
import kotlin.test.assertTrue
import kotlin.test.assertNotNull

/**
 * Tests for CI environment detection and configuration.
 */
class CIEnvironmentTest {

    @Test
    fun `should provide valid CI detection`() {
        // WHEN: Checking CI environment status
        val isCI = CIEnvironment.isCI
        val systemName = CIEnvironment.ciSystemName

        // THEN: Should provide consistent boolean result
        assertTrue(isCI is Boolean, "CI detection should return a boolean")
        assertNotNull(systemName, "CI system name should not be null")
        assertTrue(systemName.isNotEmpty(), "CI system name should not be empty")
    }

    @Test
    fun `should provide consistent performance configuration`() {
        // WHEN: Getting performance configuration
        val maxRatio = CIEnvironment.Performance.maxTimingRatio
        val maxExecTime = CIEnvironment.Performance.maxExecutionTimeMs
        val maxReasonableTime = CIEnvironment.Performance.maxReasonableTimeMs
        val timeoutMultiplier = CIEnvironment.Performance.timeoutMultiplier

        // THEN: Should provide valid positive values
        assertTrue(maxRatio > 0, "Max timing ratio should be positive")
        assertTrue(maxExecTime > 0, "Max execution time should be positive")
        assertTrue(maxReasonableTime > 0, "Max reasonable time should be positive")
        assertTrue(timeoutMultiplier > 0, "Timeout multiplier should be positive")

        // CI values should be more lenient than local values
        if (CIEnvironment.isCI) {
            assertTrue(maxRatio >= 5.0, "CI max ratio should be at least 5.0")
            assertTrue(maxExecTime >= 100L, "CI max execution time should be at least 100ms")
            assertTrue(maxReasonableTime >= 10.0, "CI max reasonable time should be at least 10ms")
            assertTrue(timeoutMultiplier >= 1.0, "CI timeout multiplier should be at least 1.0")
        }
    }

    @Test
    fun `should provide valid resource configuration`() {
        // WHEN: Getting resource configuration
        val enableMemoryTests = CIEnvironment.Resources.enableMemoryIntensiveTests
        val enableLongTests = CIEnvironment.Resources.enableLongRunningTests

        // THEN: Should provide valid boolean values
        assertTrue(enableMemoryTests is Boolean, "Memory tests flag should be boolean")
        assertTrue(enableLongTests is Boolean, "Long tests flag should be boolean")
    }

    @Test
    fun `should provide valid logging configuration`() {
        // WHEN: Getting logging configuration
        val verboseLogging = CIEnvironment.Logging.enableVerboseLogging
        val performanceMetrics = CIEnvironment.Logging.enablePerformanceMetrics

        // THEN: Should provide valid boolean values
        assertTrue(verboseLogging is Boolean, "Verbose logging flag should be boolean")
        assertTrue(performanceMetrics is Boolean, "Performance metrics flag should be boolean")

        // In CI, verbose logging should typically be enabled
        if (CIEnvironment.isCI) {
            assertTrue(verboseLogging, "Verbose logging should be enabled in CI")
            assertTrue(performanceMetrics, "Performance metrics should be enabled in CI")
        }
    }

    @Test
    fun `should detect known CI systems`() {
        // WHEN: Getting CI system name
        val systemName = CIEnvironment.ciSystemName

        // THEN: Should be one of the known systems or local
        val knownSystems = setOf(
            "GitHub Actions",
            "Travis CI",
            "CircleCI",
            "Jenkins",
            "Generic CI",
            "local"
        )
        assertTrue(
            systemName in knownSystems,
            "CI system name '$systemName' should be one of the known systems: $knownSystems"
        )
    }

    @Test
    fun `should have consistent CI detection across properties`() {
        // WHEN: Checking CI status and system name
        val isCI = CIEnvironment.isCI
        val systemName = CIEnvironment.ciSystemName

        // THEN: CI status should be consistent with system name
        if (isCI) {
            assertTrue(
                systemName != "local",
                "If isCI is true, system name should not be 'local'"
            )
        } else {
            assertTrue(
                systemName == "local",
                "If isCI is false, system name should be 'local'"
            )
        }
    }

    @Test
    fun `should have reasonable timeout multipliers`() {
        // WHEN: Getting timeout multiplier
        val multiplier = CIEnvironment.Performance.timeoutMultiplier

        // THEN: Should be within reasonable bounds
        assertTrue(
            multiplier >= 1.0 && multiplier <= 10.0,
            "Timeout multiplier should be between 1.0 and 10.0, got: $multiplier"
        )
    }
}