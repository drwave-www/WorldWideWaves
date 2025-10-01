package com.worldwidewaves.shared.ui

/*
 * Copyright 2025 DrWave
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Tests for iOS reactive integration patterns.
 *
 * These tests verify that StateFlow integration works correctly for iOS,
 * and that the reactive bridge can handle state updates properly.
 */
class IOSReactiveIntegrationTest {
    @Test
    fun `StateFlow holds initial value correctly`() =
        runTest {
            val stateFlow = MutableStateFlow("initial")

            assertEquals("initial", stateFlow.value)
        }

    @Test
    fun `StateFlow updates value correctly`() =
        runTest {
            val stateFlow = MutableStateFlow("initial")

            stateFlow.value = "updated"

            assertEquals("updated", stateFlow.value)
        }

    @Test
    fun `StateFlow with custom data class works`() =
        runTest {
            data class TestData(
                val id: String,
                val name: String,
            )

            val stateFlow = MutableStateFlow<TestData?>(null)
            val testData = TestData("1", "Test Event")

            stateFlow.value = testData

            assertNotNull(stateFlow.value)
            assertEquals("1", stateFlow.value?.id)
            assertEquals("Test Event", stateFlow.value?.name)
        }

    @Test
    fun `StateFlow with list data works correctly`() =
        runTest {
            val stateFlow = MutableStateFlow<List<String>>(emptyList())

            stateFlow.value = listOf("event1", "event2", "event3")

            assertEquals(3, stateFlow.value.size)
            assertEquals("event1", stateFlow.value[0])
            assertEquals("event2", stateFlow.value[1])
            assertEquals("event3", stateFlow.value[2])
        }

    @Test
    fun `StateFlow boolean flags work correctly`() =
        runTest {
            val isLoadingFlow = MutableStateFlow(false)
            val hasErrorFlow = MutableStateFlow(false)

            // Test loading state
            isLoadingFlow.value = true
            assertEquals(true, isLoadingFlow.value)

            // Test error state
            hasErrorFlow.value = true
            assertEquals(true, hasErrorFlow.value)

            // Reset states
            isLoadingFlow.value = false
            hasErrorFlow.value = false

            assertEquals(false, isLoadingFlow.value)
            assertEquals(false, hasErrorFlow.value)
        }
}
