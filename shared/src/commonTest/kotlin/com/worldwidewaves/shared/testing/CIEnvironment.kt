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

/**
 * Utility object for detecting and adapting to CI environments.
 *
 * CI environments often have different performance characteristics and requirements
 * compared to local development environments. This utility helps tests adapt their
 * behavior accordingly.
 */
object CIEnvironment {

    /**
     * Detects if the current environment is a CI system.
     *
     * @return true if running in a CI environment, false otherwise
     */
    val isCI: Boolean by lazy {
        System.getenv("CI") == "true" ||
        System.getenv("GITHUB_ACTIONS") == "true" ||
        System.getenv("CONTINUOUS_INTEGRATION") == "true" ||
        System.getenv("BUILD_NUMBER") != null ||  // Jenkins
        System.getenv("TRAVIS") == "true" ||      // Travis CI
        System.getenv("CIRCLECI") == "true"       // CircleCI
    }

    /**
     * Gets the name of the detected CI system, if any.
     *
     * @return the name of the CI system or "unknown" if not detected
     */
    val ciSystemName: String by lazy {
        when {
            System.getenv("GITHUB_ACTIONS") == "true" -> "GitHub Actions"
            System.getenv("TRAVIS") == "true" -> "Travis CI"
            System.getenv("CIRCLECI") == "true" -> "CircleCI"
            System.getenv("BUILD_NUMBER") != null -> "Jenkins"
            System.getenv("CI") == "true" -> "Generic CI"
            else -> "local"
        }
    }

    /**
     * Performance configuration for CI environments.
     *
     * CI environments typically have:
     * - More variable performance due to shared resources
     * - Higher latency for I/O operations
     * - Less predictable timing
     */
    object Performance {
        /**
         * Maximum allowed timing ratio between consecutive test operations.
         * Higher in CI due to variable performance.
         */
        val maxTimingRatio: Double = if (isCI) 10.0 else 5.0

        /**
         * Maximum execution time for performance tests (milliseconds).
         * Higher in CI to account for slower environments.
         */
        val maxExecutionTimeMs: Long = if (isCI) 500L else 100L

        /**
         * Maximum reasonable time for operations that should be near-instantaneous (milliseconds).
         * Higher in CI to account for environment overhead.
         */
        val maxReasonableTimeMs: Double = if (isCI) 50.0 else 10.0

        /**
         * Timeout multiplier for CI environments.
         * Tests should multiply their timeouts by this factor in CI.
         */
        val timeoutMultiplier: Double = if (isCI) 3.0 else 1.0
    }

    /**
     * Resource configuration for CI environments.
     */
    object Resources {
        /**
         * Whether to enable memory-intensive tests.
         * May be disabled in resource-constrained CI environments.
         */
        val enableMemoryIntensiveTests: Boolean = !isCI ||
            System.getenv("CI_ENABLE_MEMORY_TESTS") == "true"

        /**
         * Whether to enable long-running tests.
         * May be disabled in CI to keep build times reasonable.
         */
        val enableLongRunningTests: Boolean = !isCI ||
            System.getenv("CI_ENABLE_LONG_TESTS") == "true"
    }

    /**
     * Logging configuration for CI environments.
     */
    object Logging {
        /**
         * Whether to enable verbose logging in tests.
         * Usually enabled in CI for better debugging.
         */
        val enableVerboseLogging: Boolean = isCI

        /**
         * Whether to enable performance metrics logging.
         * Useful for tracking CI performance over time.
         */
        val enablePerformanceMetrics: Boolean = isCI
    }
}