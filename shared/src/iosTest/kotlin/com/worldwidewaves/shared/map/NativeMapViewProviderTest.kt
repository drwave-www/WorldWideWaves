package com.worldwidewaves.shared.map

/*
 * Copyright 2025 DrWave
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * https://www.apache.org/licenses/LICENSE-2.0
 */

import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.testing.testEvent
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import platform.UIKit.UIViewController
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for NativeMapViewProvider pattern and registration.
 */
class NativeMapViewProviderTest {
    @BeforeTest
    fun setup() {
        stopKoin()
    }

    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun iosNativeMapViewProvider_returnsUIViewController() {
        // Given
        val provider = IosNativeMapViewProvider()
        val event = testEvent()

        // When
        val result = provider.createMapView(event, "https://test.com/style.json")

        // Then
        assertNotNull(result)
        assertTrue(result is UIViewController)
    }

    @Test
    fun registerNativeMapViewProvider_registersInKoin() {
        // Given
        startKoin {
            modules(module {})
        }
        val mockProvider = IosNativeMapViewProvider()

        // When
        registerNativeMapViewProvider(mockProvider)

        // Then
        val koin =
            org.koin.mp.KoinPlatform
                .getKoin()
        val registered = koin.getOrNull<NativeMapViewProvider>()

        assertNotNull(registered)
        assertEquals(mockProvider, registered)
    }

    @Test
    fun mapViewFactory_usesRegisteredProvider() {
        // Given
        startKoin {
            modules(module {})
        }
        val testProvider = TestNativeMapViewProvider()
        registerNativeMapViewProvider(testProvider)

        val event = testEvent()

        // When
        val result = createNativeMapViewController(event, "test://style")

        // Then
        assertTrue(testProvider.createMapViewCalled)
        assertEquals(event.id, testProvider.lastEventId)
    }

    // Test implementation
    private class TestNativeMapViewProvider : NativeMapViewProvider {
        var createMapViewCalled = false
        var lastEventId: String? = null
        var lastRegistryKey: String? = null

        override fun createMapView(
            event: IWWWEvent,
            styleURL: String,
            enableGestures: Boolean,
            registryKey: String?,
        ): Any {
            createMapViewCalled = true
            lastEventId = event.id
            lastRegistryKey = registryKey
            return UIViewController()
        }
    }
}
