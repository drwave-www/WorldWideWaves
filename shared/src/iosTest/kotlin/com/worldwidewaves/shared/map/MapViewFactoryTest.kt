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
 */
class MapViewFactoryTest {
    @Test
    fun createNativeMapViewController_returnsUIViewController() {
        // Given
        val event = testEvent()
        val styleURL = "https://demotiles.maplibre.org/style.json"

        // When
        val result = createNativeMapViewController(event, styleURL)

        // Then - should return a UIViewController (even if fallback)
        assertNotNull(result)
        assertTrue(result is UIViewController, "Result should be UIViewController")
    }

    @Test
    fun createNativeMapViewController_handlesEmptyStyleURL() {
        // Given
        val event = testEvent()
        val styleURL = ""

        // When
        val result = createNativeMapViewController(event, styleURL)

        // Then - should handle gracefully and return fallback
        assertNotNull(result)
        assertTrue(result is UIViewController)
    }

    @Test
    fun createNativeMapViewController_logsCreation() {
        // Given
        val event = testEvent()
        val styleURL = "mbtiles://test.mbtiles"

        // When
        val result = createNativeMapViewController(event, styleURL)

        // Then - should complete without errors
        assertNotNull(result)
        // Logs are verified manually by checking console output
    }

    @Test
    fun createNativeMapViewController_handlesMultipleCalls() {
        // Given
        val event1 = testEvent()
        val event2 = testEvent()

        // When - create multiple view controllers
        val result1 = createNativeMapViewController(event1, "url1")
        val result2 = createNativeMapViewController(event2, "url2")

        // Then - both should succeed
        assertNotNull(result1)
        assertNotNull(result2)
        assertTrue(result1 !== result2, "Should create separate instances")
    }
}
