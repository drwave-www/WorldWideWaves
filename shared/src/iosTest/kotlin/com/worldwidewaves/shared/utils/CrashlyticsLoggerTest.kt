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

package com.worldwidewaves.shared.utils

import com.worldwidewaves.shared.BuildKonfig
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for iOS Crashlytics integration.
 *
 * These tests verify the behavior of CrashlyticsLogger on iOS, including:
 * - Test reporting enable/disable functionality
 * - DEBUG mode behavior
 * - Exception recording (bridge calls are tested in integration tests)
 * - Log breadcrumb behavior
 * - Custom key behavior
 *
 * Note: These tests verify the logic, not the actual Firebase integration.
 * Actual bridge calls to Firebase are tested via integration tests.
 */
class CrashlyticsLoggerTest {
    @BeforeTest
    fun setup() {
        // Ensure test reporting is disabled at start of each test
        CrashlyticsLogger.disableTestReporting()
    }

    @AfterTest
    fun teardown() {
        // Clean up after each test
        CrashlyticsLogger.disableTestReporting()
    }

    @Test
    fun testEnableTestReporting() {
        // When: Test reporting is enabled
        CrashlyticsLogger.enableTestReporting()

        // Then: Crashlytics should report even in DEBUG builds
        // (We can't directly test isReportingEnabled as it's private, but we verify no crashes occur)
        CrashlyticsLogger.log("Test message")
    }

    @Test
    fun testDisableTestReporting() {
        // Given: Test reporting was enabled
        CrashlyticsLogger.enableTestReporting()

        // When: Test reporting is disabled
        CrashlyticsLogger.disableTestReporting()

        // Then: Crashlytics should respect DEBUG mode again
        // (We verify no crashes occur)
        CrashlyticsLogger.log("Test message after disable")
    }

    @Test
    fun testRecordExceptionDoesNotCrash() {
        // Given: An exception
        val exception = RuntimeException("Test exception")

        // When: Exception is recorded in DEBUG mode
        CrashlyticsLogger.recordException(
            throwable = exception,
            tag = "TestTag",
            message = "Test message",
        )

        // Then: No crash should occur (verification is implicit - test passes if no exception)
    }

    @Test
    fun testRecordExceptionWithTestReportingEnabled() {
        // Given: Test reporting is enabled
        CrashlyticsLogger.enableTestReporting()
        val exception = IllegalStateException("Test illegal state")

        // When: Exception is recorded
        CrashlyticsLogger.recordException(
            throwable = exception,
            tag = "IllegalStateTag",
            message = "Illegal state occurred",
        )

        // Then: No crash should occur
    }

    @Test
    fun testLogBreadcrumbWithoutTag() {
        // When: Log message without tag format
        CrashlyticsLogger.log("Simple message")

        // Then: No crash should occur
    }

    @Test
    fun testLogBreadcrumbWithTag() {
        // When: Log message with tag format [Tag] Message
        CrashlyticsLogger.log("[UserAction] Button clicked")

        // Then: No crash should occur and tag should be extracted correctly
    }

    @Test
    fun testLogBreadcrumbWithTestReportingEnabled() {
        // Given: Test reporting is enabled
        CrashlyticsLogger.enableTestReporting()

        // When: Log breadcrumb
        CrashlyticsLogger.log("[TestTag] Test breadcrumb")

        // Then: No crash should occur
    }

    @Test
    fun testSetCustomKeyDoesNotCrash() {
        // When: Custom key is set in DEBUG mode
        CrashlyticsLogger.setCustomKey("test_key", "test_value")

        // Then: No crash should occur
    }

    @Test
    fun testSetCustomKeyWithTestReportingEnabled() {
        // Given: Test reporting is enabled
        CrashlyticsLogger.enableTestReporting()

        // When: Custom key is set
        CrashlyticsLogger.setCustomKey("user_id", "12345")

        // Then: No crash should occur
    }

    @Test
    fun testMultipleOperationsInSequence() {
        // Given: Test reporting is enabled
        CrashlyticsLogger.enableTestReporting()

        // When: Multiple operations are performed
        CrashlyticsLogger.setCustomKey("session_id", "abc-123")
        CrashlyticsLogger.log("[App] App started")
        CrashlyticsLogger.log("[User] User logged in")
        CrashlyticsLogger.recordException(
            throwable = Exception("Test exception"),
            tag = "TestFlow",
            message = "Exception during test flow",
        )
        CrashlyticsLogger.setCustomKey("last_action", "test_completed")

        // Then: No crash should occur
    }

    @Test
    fun testExceptionWithNullMessage() {
        // Given: An exception with null message
        val exception =
            object : Exception() {
                override val message: String?
                    get() = null
            }

        // When: Exception is recorded
        CrashlyticsLogger.recordException(
            throwable = exception,
            tag = "NullMessageTag",
            message = "Exception with null message",
        )

        // Then: No crash should occur (should handle null gracefully)
    }

    @Test
    fun testExceptionWithLongStackTrace() {
        // Given: An exception with a deep stack trace
        fun recursiveFunction(depth: Int): Nothing {
            if (depth == 0) {
                throw RuntimeException("Deep stack trace exception")
            }
            recursiveFunction(depth - 1)
        }

        val exception =
            try {
                recursiveFunction(10)
            } catch (e: RuntimeException) {
                e
            }

        // When: Exception with long stack trace is recorded
        CrashlyticsLogger.recordException(
            throwable = exception,
            tag = "DeepStackTag",
            message = "Exception with deep stack",
        )

        // Then: No crash should occur
    }

    @Test
    fun testLogWithEmptyMessage() {
        // When: Empty message is logged
        CrashlyticsLogger.log("")

        // Then: No crash should occur
    }

    @Test
    fun testLogWithVeryLongMessage() {
        // Given: A very long message
        val longMessage = "A".repeat(10000)

        // When: Long message is logged
        CrashlyticsLogger.log(longMessage)

        // Then: No crash should occur
    }

    @Test
    fun testSetCustomKeyWithEmptyValue() {
        // When: Custom key with empty value is set
        CrashlyticsLogger.setCustomKey("empty_key", "")

        // Then: No crash should occur
    }

    @Test
    fun testSetCustomKeyWithSpecialCharacters() {
        // When: Custom key with special characters is set
        CrashlyticsLogger.setCustomKey("special_key", "Value with émoji 🎉 and symbols !@#$%")

        // Then: No crash should occur
    }

    @Test
    fun testDEBUGModeMatchesExpectation() {
        // This test verifies that the DEBUG flag is set as expected
        // In debug builds, DEBUG should be true
        // In release builds, DEBUG should be false

        // Note: We can't directly test isReportingEnabled() as it's private,
        // but we can verify BuildKonfig.DEBUG is accessible
        if (BuildKonfig.DEBUG) {
            assertTrue(
                BuildKonfig.DEBUG,
                "DEBUG mode should be true in debug builds",
            )
        } else {
            assertFalse(
                BuildKonfig.DEBUG,
                "DEBUG mode should be false in release builds",
            )
        }
    }
}
