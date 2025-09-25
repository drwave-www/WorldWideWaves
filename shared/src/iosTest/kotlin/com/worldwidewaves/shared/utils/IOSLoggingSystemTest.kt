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
import kotlin.test.assertTrue
import kotlin.test.assertNotNull

/**
 * iOS-specific tests for the logging system.
 * Tests NSLogAntilog and iOS Napier initialization.
 */
class IOSLoggingSystemTest {

    @Test
    fun testIOSLogConfigExists() {
        // Test that iOS LogConfig is properly configured
        assertNotNull(LogConfig.ENABLE_VERBOSE_LOGGING)
        assertNotNull(LogConfig.ENABLE_DEBUG_LOGGING)
        assertNotNull(LogConfig.ENABLE_PERFORMANCE_LOGGING)

        // iOS should have logging enabled for development
        assertTrue(LogConfig.ENABLE_DEBUG_LOGGING, "iOS should have debug logging enabled")
    }

    @Test
    fun testIOSInitNapierDoesNotCrash() {
        // Test that iOS initNapier() can be called without crashing
        try {
            initNapier()
            assertTrue(true, "iOS initNapier() completed successfully")
        } catch (e: Exception) {
            assertTrue(false, "iOS initNapier() should not throw: ${e.message}")
        }
    }

    @Test
    fun testIOSLogWrapperBasicCalls() {
        // Initialize logging first
        initNapier()

        // Test all Log wrapper methods don't crash on iOS
        try {
            Log.v("iOS_TEST", "iOS verbose test message")
            Log.d("iOS_TEST", "iOS debug test message")
            Log.i("iOS_TEST", "iOS info test message")
            Log.w("iOS_TEST", "iOS warning test message")
            Log.e("iOS_TEST", "iOS error test message")
            Log.performance("iOS_TEST", "iOS performance test message")

            assertTrue(true, "All iOS Log methods executed without crashing")
        } catch (e: Exception) {
            assertTrue(false, "iOS Log methods should not throw: ${e.message}")
        }
    }

    @Test
    fun testIOSLogWrapperWithThrowable() {
        // Initialize logging first
        initNapier()

        val testException = RuntimeException("iOS test exception")

        try {
            Log.e("iOS_TEST", "iOS error with throwable", testException)
            Log.w("iOS_TEST", "iOS warning with throwable", testException)

            assertTrue(true, "iOS Log methods with throwable executed successfully")
        } catch (e: Exception) {
            assertTrue(false, "iOS Log methods with throwable should not throw: ${e.message}")
        }
    }

    @Test
    fun testIOSNSLogAntilogDirect() {
        // Test NSLogAntilog directly to isolate issues
        try {
            val antilog = NSLogAntilog()

            // Test that NSLogAntilog doesn't crash when used directly
            antilog.performLog(
                io.github.aakira.napier.LogLevel.INFO,
                "iOS_DIRECT_TEST",
                null,
                "Direct NSLogAntilog test message"
            )

            assertTrue(true, "NSLogAntilog direct test completed")
        } catch (e: Exception) {
            assertTrue(false, "NSLogAntilog direct test should not crash: ${e.message}")
        }
    }
}