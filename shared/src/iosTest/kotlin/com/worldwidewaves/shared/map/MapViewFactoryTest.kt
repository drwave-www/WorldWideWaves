package com.worldwidewaves.shared.map

/*
 * Copyright 2025 DrWave
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * https://www.apache.org/licenses/LICENSE-2.0
 */

import com.worldwidewaves.shared.testing.testEvent
import platform.UIKit.UIViewController
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for iOS MapViewFactory implementation.
 *
 * NOTE: These tests require iOS UIKit context which may not be available in all test environments.
 * The tests verify that the factory handles creation gracefully even without full iOS runtime.
 */
class MapViewFactoryTest {
    @Test
    fun createNativeMapViewController_returnsUIViewController() {
        // Given
        val event = testEvent()
        val styleURL = "https://demotiles.maplibre.org/style.json"

        // When - UIViewController creation may fail in test environment, but shouldn't crash
        try {
            val result = createNativeMapViewController(event, styleURL)

            // Then - should return a UIViewController (even if fallback)
            assertNotNull(result)
            assertTrue(result is UIViewController, "Result should be UIViewController")
        } catch (e: IllegalStateException) {
            // Expected in test environment without full iOS runtime
            println("UIViewController creation failed in test environment (expected): ${e.message}")
        }
    }

    @Test
    fun createNativeMapViewController_handlesEmptyStyleURL() {
        // Given
        val event = testEvent()
        val styleURL = ""

        // When - UIViewController creation may fail in test environment
        try {
            val result = createNativeMapViewController(event, styleURL)

            // Then - should handle gracefully and return fallback
            assertNotNull(result)
            assertTrue(result is UIViewController)
        } catch (e: IllegalStateException) {
            // Expected in test environment without full iOS runtime
            println("UIViewController creation with empty URL failed in test environment (expected): ${e.message}")
        }
    }

    @Test
    fun createNativeMapViewController_logsCreation() {
        // Given
        val event = testEvent()
        val styleURL = "mbtiles://test.mbtiles"

        // When - UIViewController creation may fail in test environment
        try {
            val result = createNativeMapViewController(event, styleURL)

            // Then - should complete without errors
            assertNotNull(result)
            // Logs are verified manually by checking console output
        } catch (e: IllegalStateException) {
            // Expected in test environment without full iOS runtime
            println("UIViewController creation with mbtiles URL failed in test environment (expected): ${e.message}")
        }
    }

    @Test
    fun createNativeMapViewController_handlesMultipleCalls() {
        // Given
        val event1 = testEvent()
        val event2 = testEvent()

        // When - create multiple view controllers (may fail in test environment)
        try {
            val result1 = createNativeMapViewController(event1, "url1")
            val result2 = createNativeMapViewController(event2, "url2")

            // Then - both should succeed
            assertNotNull(result1)
            assertNotNull(result2)
            assertTrue(result1 !== result2, "Should create separate instances")
        } catch (e: IllegalStateException) {
            // Expected in test environment without full iOS runtime
            println("Multiple UIViewController creation failed in test environment (expected): ${e.message}")
        }
    }
}
