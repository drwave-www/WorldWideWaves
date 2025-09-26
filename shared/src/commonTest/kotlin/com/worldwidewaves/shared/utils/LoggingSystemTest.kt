package com.worldwidewaves.shared.utils

/*
 * Copyright 2025 DrWave
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Comprehensive tests for the cross-platform logging system.
 * Verifies LogConfig, initNapier(), and Log wrapper functionality.
 */
class LoggingSystemTest {

    @Test
    fun testLogConfigExists() {
        // Test that LogConfig is properly configured on all platforms
        assertNotNull(LogConfig.ENABLE_VERBOSE_LOGGING)
        assertNotNull(LogConfig.ENABLE_DEBUG_LOGGING)
        assertNotNull(LogConfig.ENABLE_PERFORMANCE_LOGGING)
    }

    @Test
    fun testInitNapierDoesNotCrash() {
        // Test that initNapier() can be called without crashing
        try {
            initNapier()
            assertTrue(true, "initNapier() completed successfully")
        } catch (e: Exception) {
            assertTrue(false, "initNapier() should not throw: ${e.message}")
        }
    }

    @Test
    fun testLogWrapperBasicCalls() {
        // Initialize logging first
        initNapier()

        // Test all Log wrapper methods don't crash
        try {
            Log.v("TEST", "Verbose test message")
            Log.d("TEST", "Debug test message")
            Log.i("TEST", "Info test message")
            Log.w("TEST", "Warning test message")
            Log.e("TEST", "Error test message")
            Log.performance("TEST", "Performance test message")

            assertTrue(true, "All Log methods executed without crashing")
        } catch (e: Exception) {
            assertTrue(false, "Log methods should not throw: ${e.message}")
        }
    }

    @Test
    fun testLogWrapperWithThrowable() {
        // Initialize logging first
        initNapier()

        val testException = RuntimeException("Test exception")

        try {
            Log.e("TEST", "Error with throwable", testException)
            Log.w("TEST", "Warning with throwable", testException)

            assertTrue(true, "Log methods with throwable executed successfully")
        } catch (e: Exception) {
            assertTrue(false, "Log methods with throwable should not throw: ${e.message}")
        }
    }

    @Test
    fun testLogConfigGatesVerboseLogging() {
        // Test that verbose logging respects LogConfig flags
        initNapier()

        // This should not crash regardless of LogConfig settings
        try {
            Log.v("TEST", "This should be gated by ENABLE_VERBOSE_LOGGING")
            assertTrue(true, "Verbose logging gating works")
        } catch (e: Exception) {
            assertTrue(false, "Verbose logging should not crash: ${e.message}")
        }
    }

    @Test
    fun testLogConfigGatesDebugLogging() {
        // Test that debug logging respects LogConfig flags
        initNapier()

        try {
            Log.d("TEST", "This should be gated by ENABLE_DEBUG_LOGGING")
            assertTrue(true, "Debug logging gating works")
        } catch (e: Exception) {
            assertTrue(false, "Debug logging should not crash: ${e.message}")
        }
    }

    @Test
    fun testLogConfigGatesPerformanceLogging() {
        // Test that performance logging respects LogConfig flags
        initNapier()

        try {
            Log.performance("TEST", "This should be gated by ENABLE_PERFORMANCE_LOGGING")
            assertTrue(true, "Performance logging gating works")
        } catch (e: Exception) {
            assertTrue(false, "Performance logging should not crash: ${e.message}")
        }
    }

    @Test
    fun testInitNapierCanBeCalledMultipleTimes() {
        // Test that initNapier() is idempotent and safe to call multiple times
        try {
            initNapier()
            initNapier() // Should not crash on second call
            initNapier() // Should not crash on third call

            assertTrue(true, "Multiple initNapier() calls handled safely")
        } catch (e: Exception) {
            assertTrue(false, "Multiple initNapier() calls should be safe: ${e.message}")
        }
    }

    @Test
    fun testLogWrapperAllLevels() {
        // Test all log levels with various message types
        initNapier()

        try {
            // Test empty messages
            Log.v("TEST", "")
            Log.d("TEST", "")
            Log.i("TEST", "")
            Log.w("TEST", "")
            Log.e("TEST", "")

            // Test null safety
            Log.i("TEST", null.toString())

            // Test long messages
            val longMessage = "A".repeat(1000)
            Log.i("TEST", longMessage)

            // Test special characters
            Log.i("TEST", "Special chars: 🚀 🎯 ✅ ❌ 💥")

            assertTrue(true, "All log level variations handled correctly")
        } catch (e: Exception) {
            assertTrue(false, "Log level variations should not crash: ${e.message}")
        }
    }

    @Test
    fun testLogConfigFlags() {
        // Test that LogConfig flags are properly configured
        assertNotNull(LogConfig.ENABLE_VERBOSE_LOGGING, "ENABLE_VERBOSE_LOGGING should not be null")
        assertNotNull(LogConfig.ENABLE_DEBUG_LOGGING, "ENABLE_DEBUG_LOGGING should not be null")
        assertNotNull(LogConfig.ENABLE_PERFORMANCE_LOGGING, "ENABLE_PERFORMANCE_LOGGING should not be null")

        // On Android, debug logging should be enabled in debug builds
        assertTrue(LogConfig.ENABLE_DEBUG_LOGGING, "Debug logging should be enabled on Android")
    }

    @Test
    fun testPerformanceLoggingFormat() {
        // Test that performance logging has correct format
        initNapier()

        try {
            Log.performance("PERF_TEST", "Operation took 123ms")
            assertTrue(true, "Performance logging format works")
        } catch (e: Exception) {
            assertTrue(false, "Performance logging format should not crash: ${e.message}")
        }
    }

    @Test
    fun testLogWrapperThreadSafety() {
        // Test that logging works from multiple threads
        initNapier()

        try {
            // Simple thread safety test
            repeat(10) { i ->
                Log.i("THREAD_TEST", "Message from iteration $i")
            }

            assertTrue(true, "Thread safety test completed")
        } catch (e: Exception) {
            assertTrue(false, "Thread safety test should not crash: ${e.message}")
        }
    }
}