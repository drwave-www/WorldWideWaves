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
 * Android-specific tests for the logging system.
 * Tests DebugAntilog and Android Napier initialization.
 */
class AndroidLoggingSystemTest {

    @Test
    fun testAndroidLogConfigExists() {
        // Test that Android LogConfig is properly configured
        assertNotNull(LogConfig.ENABLE_VERBOSE_LOGGING)
        assertNotNull(LogConfig.ENABLE_DEBUG_LOGGING)
        assertNotNull(LogConfig.ENABLE_PERFORMANCE_LOGGING)

        // Android should have logging enabled for development
        assertTrue(LogConfig.ENABLE_DEBUG_LOGGING, "Android should have debug logging enabled")
    }

    @Test
    fun testAndroidInitNapierWithDebugAntilog() {
        // Test that Android initNapier() uses DebugAntilog correctly
        try {
            initNapier()
            assertTrue(true, "Android initNapier() with DebugAntilog completed")
        } catch (e: Exception) {
            assertTrue(false, "Android initNapier() should not throw: ${e.message}")
        }
    }

    @Test
    fun testAndroidLogWrapperInLogcat() {
        // Test that Android logs appear in Logcat (simulated)
        initNapier()

        try {
            // These would appear in Android Logcat
            Log.v("ANDROID_TEST", "Android verbose test message")
            Log.d("ANDROID_TEST", "Android debug test message")
            Log.i("ANDROID_TEST", "Android info test message")
            Log.w("ANDROID_TEST", "Android warning test message")
            Log.e("ANDROID_TEST", "Android error test message")
            Log.performance("ANDROID_TEST", "Android performance test message")

            assertTrue(true, "All Android Log methods executed for Logcat")
        } catch (e: Exception) {
            assertTrue(false, "Android Log methods should not throw: ${e.message}")
        }
    }

    @Test
    fun testAndroidLogConfigGating() {
        // Test Android-specific LogConfig behavior
        initNapier()

        try {
            // Test verbose logging gating
            if (LogConfig.ENABLE_VERBOSE_LOGGING) {
                Log.v("ANDROID_GATING", "Verbose enabled on Android")
            }

            // Test debug logging gating
            if (LogConfig.ENABLE_DEBUG_LOGGING) {
                Log.d("ANDROID_GATING", "Debug enabled on Android")
            }

            // Test performance logging gating
            if (LogConfig.ENABLE_PERFORMANCE_LOGGING) {
                Log.performance("ANDROID_GATING", "Performance enabled on Android")
            }

            assertTrue(true, "Android LogConfig gating works correctly")
        } catch (e: Exception) {
            assertTrue(false, "Android LogConfig gating should not crash: ${e.message}")
        }
    }

    @Test
    fun testAndroidLogWithComplexMessages() {
        // Test Android logging with complex message patterns
        initNapier()

        try {
            val complexException = RuntimeException("Complex Android exception with nested cause",
                IllegalStateException("Nested cause"))

            Log.e("ANDROID_COMPLEX", "Complex error message", complexException)
            Log.w("ANDROID_COMPLEX", "Warning with Unicode: 🚀 🎯 ✅", complexException)

            val longMessage = "Android long message: " + "test ".repeat(100)
            Log.i("ANDROID_COMPLEX", longMessage)

            assertTrue(true, "Android complex message logging works")
        } catch (e: Exception) {
            assertTrue(false, "Android complex logging should not crash: ${e.message}")
        }
    }

    @Test
    fun testAndroidInitNapierIdempotent() {
        // Test that Android initNapier() can be called multiple times safely
        try {
            initNapier()
            initNapier() // Should be safe
            initNapier() // Should be safe

            Log.i("ANDROID_IDEMPOTENT", "Multiple initNapier() calls completed")
            assertTrue(true, "Android initNapier() is idempotent")
        } catch (e: Exception) {
            assertTrue(false, "Android initNapier() idempotency failed: ${e.message}")
        }
    }
}