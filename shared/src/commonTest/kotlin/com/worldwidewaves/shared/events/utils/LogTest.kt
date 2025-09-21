package com.worldwidewaves.shared.events.utils

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

import io.github.aakira.napier.Antilog
import io.github.aakira.napier.LogLevel
import io.github.aakira.napier.Napier
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for Log wrapper functionality.
 * These tests ensure that the Log object correctly delegates to Napier
 * and provides consistent logging behavior across all log levels.
 */
class LogTest {

    private class TestAntilog : Antilog() {
        val logs = mutableListOf<LogEntry>()

        data class LogEntry(
            val level: String,
            val tag: String,
            val message: String,
            val throwable: Throwable?
        )

        override fun performLog(priority: LogLevel, tag: String?, throwable: Throwable?, message: String?) {
            logs.add(LogEntry(
                level = priority.name,
                tag = tag ?: "",
                message = message ?: "",
                throwable = throwable
            ))
        }
    }

    private fun withTestLogger(test: (TestAntilog) -> Unit) {
        val testLogger = TestAntilog()
        Napier.base(testLogger)
        try {
            test(testLogger)
        } finally {
            // Reset to default debug logger for other tests
            Napier.base(object : Antilog() {
                override fun performLog(priority: LogLevel, tag: String?, throwable: Throwable?, message: String?) {
                    println("[$priority] $tag: $message")
                }
            })
        }
    }

    @Test
    fun `should log verbose messages correctly`() {
        withTestLogger { testLogger ->
            // GIVEN: A verbose log message
            val tag = "TestTag"
            val message = "Test verbose message"

            // WHEN: Logging a verbose message
            Log.v(tag, message)

            // THEN: Should delegate to Napier with correct parameters
            assertTrue(testLogger.logs.isNotEmpty(), "Should have logged at least one message")
            val lastLog = testLogger.logs.last()
            assertEquals("VERBOSE", lastLog.level, "Should log at VERBOSE level")
            assertEquals(tag, lastLog.tag, "Should use correct tag")
            assertEquals(message, lastLog.message, "Should use correct message")
            assertEquals(null, lastLog.throwable, "Should have no throwable")
        }
    }

    @Test
    fun `should log debug messages correctly`() {
        withTestLogger { testLogger ->
            // GIVEN: A debug log message
            val tag = "DebugTag"
            val message = "Debug information"

            // WHEN: Logging a debug message
            Log.d(tag, message)

            // THEN: Should delegate to Napier with correct parameters
            assertTrue(testLogger.logs.isNotEmpty(), "Should have logged at least one message")
            val lastLog = testLogger.logs.last()
            assertEquals("DEBUG", lastLog.level, "Should log at DEBUG level")
            assertEquals(tag, lastLog.tag, "Should use correct tag")
            assertEquals(message, lastLog.message, "Should use correct message")
            assertEquals(null, lastLog.throwable, "Should have no throwable")
        }
    }

    @Test
    fun `should log info messages correctly`() {
        withTestLogger { testLogger ->
            // GIVEN: An info log message
            val tag = "InfoTag"
            val message = "Information message"

            // WHEN: Logging an info message
            Log.i(tag, message)

            // THEN: Should delegate to Napier with correct parameters
            assertTrue(testLogger.logs.isNotEmpty(), "Should have logged at least one message")
            val lastLog = testLogger.logs.last()
            assertEquals("INFO", lastLog.level, "Should log at INFO level")
            assertEquals(tag, lastLog.tag, "Should use correct tag")
            assertEquals(message, lastLog.message, "Should use correct message")
            assertEquals(null, lastLog.throwable, "Should have no throwable")
        }
    }

    @Test
    fun `should log warning messages correctly`() {
        withTestLogger { testLogger ->
            // GIVEN: A warning log message
            val tag = "WarnTag"
            val message = "Warning message"

            // WHEN: Logging a warning message
            Log.w(tag, message)

            // THEN: Should delegate to Napier with correct parameters
            assertTrue(testLogger.logs.isNotEmpty(), "Should have logged at least one message")
            val lastLog = testLogger.logs.last()
            assertEquals("WARNING", lastLog.level, "Should log at WARNING level")
            assertEquals(tag, lastLog.tag, "Should use correct tag")
            assertEquals(message, lastLog.message, "Should use correct message")
            assertEquals(null, lastLog.throwable, "Should have no throwable")
        }
    }

    @Test
    fun `should log error messages correctly`() {
        withTestLogger { testLogger ->
            // GIVEN: An error log message
            val tag = "ErrorTag"
            val message = "Error message"

            // WHEN: Logging an error message
            Log.e(tag, message)

            // THEN: Should delegate to Napier with correct parameters
            assertTrue(testLogger.logs.isNotEmpty(), "Should have logged at least one message")
            val lastLog = testLogger.logs.last()
            assertEquals("ERROR", lastLog.level, "Should log at ERROR level")
            assertEquals(tag, lastLog.tag, "Should use correct tag")
            assertEquals(message, lastLog.message, "Should use correct message")
            assertEquals(null, lastLog.throwable, "Should have no throwable")
        }
    }

    @Test
    fun `should log wtf messages correctly`() {
        withTestLogger { testLogger ->
            // GIVEN: A wtf (what a terrible failure) log message
            val tag = "WtfTag"
            val message = "What a terrible failure message"

            // WHEN: Logging a wtf message
            Log.wtf(tag, message)

            // THEN: Should delegate to Napier with correct parameters
            assertTrue(testLogger.logs.isNotEmpty(), "Should have logged at least one message")
            val lastLog = testLogger.logs.last()
            assertEquals("ASSERT", lastLog.level, "Should log at ASSERT level")
            assertEquals(tag, lastLog.tag, "Should use correct tag")
            assertEquals(message, lastLog.message, "Should use correct message")
            assertEquals(null, lastLog.throwable, "Should have no throwable")
        }
    }

    @Test
    fun `should log messages with throwables correctly`() {
        withTestLogger { testLogger ->
            // GIVEN: A log message with a throwable
            val tag = "ThrowableTag"
            val message = "Error with exception"
            val exception = RuntimeException("Test exception")

            // WHEN: Logging an error with throwable
            Log.e(tag, message, exception)

            // THEN: Should delegate to Napier with correct parameters including throwable
            assertTrue(testLogger.logs.isNotEmpty(), "Should have logged at least one message")
            val lastLog = testLogger.logs.last()
            assertEquals("ERROR", lastLog.level, "Should log at ERROR level")
            assertEquals(tag, lastLog.tag, "Should use correct tag")
            assertEquals(message, lastLog.message, "Should use correct message")
            assertNotNull(lastLog.throwable, "Should include throwable")
            assertEquals(exception, lastLog.throwable, "Should include the same throwable")
        }
    }

    @Test
    fun `should handle all log levels with throwables`() {
        withTestLogger { testLogger ->
            // GIVEN: Exception to log with different levels
            val exception = IllegalArgumentException("Test exception")
            val message = "Message with exception"

            // WHEN: Logging at all levels with throwables
            Log.v("VTag", message, exception)
            Log.d("DTag", message, exception)
            Log.i("ITag", message, exception)
            Log.w("WTag", message, exception)
            Log.e("ETag", message, exception)
            Log.wtf("WtfTag", message, exception)

            // THEN: All logs should include the throwable
            assertEquals(6, testLogger.logs.size, "Should have logged 6 messages")
            testLogger.logs.forEach { log ->
                assertEquals(exception, log.throwable, "All logs should include the same throwable")
                assertEquals(message, log.message, "All logs should have the same message")
            }

            // Verify correct log levels
            assertEquals("VERBOSE", testLogger.logs[0].level)
            assertEquals("DEBUG", testLogger.logs[1].level)
            assertEquals("INFO", testLogger.logs[2].level)
            assertEquals("WARNING", testLogger.logs[3].level)
            assertEquals("ERROR", testLogger.logs[4].level)
            assertEquals("ASSERT", testLogger.logs[5].level)
        }
    }

    @Test
    fun `should handle empty and special characters in messages`() {
        withTestLogger { testLogger ->
            // GIVEN: Various special message content
            val testCases = listOf(
                "" to "empty string",
                "Unicode: 🌊🌍✨" to "unicode characters",
                "Line\nBreaks\nHere" to "line breaks",
                "Tabs\tAnd\tSpaces" to "tabs and spaces",
                "Special chars: !@#$%^&*()" to "special characters",
                "Very long message ".repeat(100) to "very long message"
            )

            // WHEN: Logging various message types
            testCases.forEachIndexed { index, (message, description) ->
                Log.i("Test$index", message)
            }

            // THEN: All messages should be logged correctly
            assertEquals(testCases.size, testLogger.logs.size, "Should log all test cases")
            testCases.forEachIndexed { index, (expectedMessage, _) ->
                assertEquals(expectedMessage, testLogger.logs[index].message, "Message $index should be preserved")
                assertEquals("Test$index", testLogger.logs[index].tag, "Tag $index should be correct")
            }
        }
    }

    @Test
    fun `should handle empty and special characters in tags`() {
        withTestLogger { testLogger ->
            // GIVEN: Various tag formats
            val testCases = listOf(
                "SimpleTag" to "simple tag",
                "Tag.With.Dots" to "dotted tag",
                "Tag_With_Underscores" to "underscored tag",
                "Tag-With-Dashes" to "dashed tag",
                "TagWith123Numbers" to "alphanumeric tag",
                "CamelCaseTag" to "camel case tag"
            )

            // WHEN: Logging with various tag formats
            testCases.forEach { (tag, _) ->
                Log.i(tag, "Test message")
            }

            // THEN: All tags should be preserved correctly
            assertEquals(testCases.size, testLogger.logs.size, "Should log all test cases")
            testCases.forEachIndexed { index, (expectedTag, _) ->
                assertEquals(expectedTag, testLogger.logs[index].tag, "Tag $index should be preserved")
                assertEquals("Test message", testLogger.logs[index].message, "Message should be consistent")
            }
        }
    }

    @Test
    fun `should maintain consistent behavior across multiple calls`() {
        withTestLogger { testLogger ->
            // GIVEN: Multiple log calls
            val numberOfCalls = 10

            // WHEN: Making multiple log calls at different levels
            repeat(numberOfCalls) { i ->
                when (i % 6) {
                    0 -> Log.v("Tag$i", "Message $i")
                    1 -> Log.d("Tag$i", "Message $i")
                    2 -> Log.i("Tag$i", "Message $i")
                    3 -> Log.w("Tag$i", "Message $i")
                    4 -> Log.e("Tag$i", "Message $i")
                    5 -> Log.wtf("Tag$i", "Message $i")
                }
            }

            // THEN: All calls should be logged consistently
            assertEquals(numberOfCalls, testLogger.logs.size, "Should log all calls")

            repeat(numberOfCalls) { i ->
                val log = testLogger.logs[i]
                assertEquals("Tag$i", log.tag, "Tag should match index")
                assertEquals("Message $i", log.message, "Message should match index")
                assertEquals(null, log.throwable, "No throwable should be present")

                val expectedLevel = when (i % 6) {
                    0 -> "VERBOSE"
                    1 -> "DEBUG"
                    2 -> "INFO"
                    3 -> "WARNING"
                    4 -> "ERROR"
                    5 -> "ASSERT"
                    else -> error("Unexpected index")
                }
                assertEquals(expectedLevel, log.level, "Level should match expected for index $i")
            }
        }
    }

    @Test
    fun `should handle concurrent logging correctly`() {
        withTestLogger { testLogger ->
            // GIVEN: Multiple threads logging simultaneously
            val threadCount = 5
            val logsPerThread = 20
            val expectedTotalLogs = threadCount * logsPerThread

            // WHEN: Multiple threads log concurrently
            val threads = (0 until threadCount).map { threadId ->
                Thread {
                    repeat(logsPerThread) { logId ->
                        Log.i("Thread$threadId", "Log $logId from thread $threadId")
                    }
                }
            }

            threads.forEach { it.start() }
            threads.forEach { it.join() }

            // THEN: All logs should be captured
            assertEquals(expectedTotalLogs, testLogger.logs.size, "Should capture all logs from all threads")

            // Verify that each thread's logs are present
            (0 until threadCount).forEach { threadId ->
                val threadLogs = testLogger.logs.filter { it.tag == "Thread$threadId" }
                assertEquals(logsPerThread, threadLogs.size, "Thread $threadId should have $logsPerThread logs")
            }
        }
    }

    @Test
    fun `should handle null throwable parameter correctly`() {
        withTestLogger { testLogger ->
            // GIVEN: Explicit null throwable parameter

            // WHEN: Logging with explicit null throwable
            Log.e("NullThrowableTag", "Message with explicit null throwable", null)

            // THEN: Should handle null throwable gracefully
            assertTrue(testLogger.logs.isNotEmpty(), "Should have logged the message")
            val lastLog = testLogger.logs.last()
            assertEquals("ERROR", lastLog.level, "Should log at ERROR level")
            assertEquals("NullThrowableTag", lastLog.tag, "Should use correct tag")
            assertEquals("Message with explicit null throwable", lastLog.message, "Should use correct message")
            assertEquals(null, lastLog.throwable, "Should handle null throwable")
        }
    }
}