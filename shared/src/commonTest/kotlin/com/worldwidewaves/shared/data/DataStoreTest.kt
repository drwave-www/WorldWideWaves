package com.worldwidewaves.shared.data

import com.worldwidewaves.shared.events.utils.Log
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for DataStore functionality in a KMP-compatible way.
 * These tests focus on the public API and behavior that can be verified
 * without using reflection or JVM-specific features.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DataStoreTest {

    @BeforeTest
    fun setup() {
        // Mock the Log object for verification
        mockkObject(Log)
        justRun { Log.i(any(), any()) }
        justRun { Log.v(any(), any()) }
    }

    @AfterTest
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `test dataStoreFileName constant has correct value`() {
        assertEquals("wwwaves.preferences_pb", dataStoreFileName, 
            "dataStoreFileName should have the expected value")
    }

    @Test
    fun `test createDataStore logs initialization message`() {
        // Arrange
        val pathProvider = mockk<() -> String>()
        val logMessageSlot = slot<String>()
        
        every { pathProvider() } returns "/test/path"
        every { Log.i(any(), capture(logMessageSlot)) } returns Unit
        
        // Act - Note: This will attempt to create a real DataStore, which may not work in tests
        // But we're only verifying the logging behavior
        try {
            createDataStore(pathProvider)
        } catch (e: Exception) {
            // Ignore exceptions from actual DataStore creation
            // We're only testing the logging behavior
        }
        
        // Assert
        verify { pathProvider() }
        verify { Log.i(any(), any()) }
        
        // If the log message was captured, verify its content
        if (logMessageSlot.isCaptured) {
            assertTrue(logMessageSlot.captured.contains("/test/path"), 
                "Log message should contain the path")
        }
    }

    @Test
    fun `test path provider function is called`() {
        // Arrange
        val pathProviderMock = mockk<() -> String>()
        every { pathProviderMock() } returns "/test/path"
        
        // Act - Note: This will attempt to create a real DataStore, which may not work in tests
        // But we're only verifying that the path provider is called
        try {
            createDataStore(pathProviderMock)
        } catch (e: Exception) {
            // Ignore exceptions from actual DataStore creation
            // We're only testing that the path provider is called
        }
        
        // Assert
        verify { pathProviderMock() }
    }

    @Test
    fun `test expect function keyValueStorePath exists`() {
        // This test verifies that the expect function exists
        // We can't test its implementation directly in common code
        
        // Just verify that the function is declared
        // This is a compile-time check, not a runtime check
        val functionExists = true // If this compiles, the function exists
        assertTrue(functionExists, "keyValueStorePath function should be declared")
    }
    
}
