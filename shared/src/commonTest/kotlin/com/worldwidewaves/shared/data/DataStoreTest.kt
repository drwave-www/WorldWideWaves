package com.worldwidewaves.shared.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.worldwidewaves.shared.events.utils.Log
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import okio.Path
import okio.Path.Companion.toPath
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty
import kotlin.reflect.full.companionObject
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.isAccessible
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DataStoreTest {

    // Mock objects for testing
    private lateinit var mockDataStore: DataStore<Preferences>
    private lateinit var mockPreferenceDataStoreFactory: PreferenceDataStoreFactory
    private lateinit var mockPath: Path

    @BeforeTest
    fun setup() {
        // Mock the DataStore and factory
        mockDataStore = mockk(relaxed = true)
        mockPreferenceDataStoreFactory = mockk()
        mockPath = mockk()

        // Mock the Path.toPath extension function
        mockkStatic("okio.Path\$Companion.toPath")
        every { any<String>().toPath() } returns mockPath

        // Mock the PreferenceDataStoreFactory.createWithPath
        mockkStatic(PreferenceDataStoreFactory::class)
        every { 
            PreferenceDataStoreFactory.createWithPath(any()) 
        } returns mockDataStore

        // Mock the Log object
        mockkObject(Log)
        justRun { Log.i(any(), any()) }
        justRun { Log.v(any(), any()) }

        // Reset the dataStore field before each test
        resetDataStoreField()
    }

    @AfterTest
    fun tearDown() {
        unmockkAll()
    }

    // Helper method to reset the dataStore field using reflection
    private fun resetDataStoreField() {
        val dataStoreClass = Class.forName("com.worldwidewaves.shared.data.DataStoreKt")
        val dataStoreField = dataStoreClass.getDeclaredField("dataStore")
        dataStoreField.isAccessible = true
        
        // Use Kotlin's property mechanism to reset the lateinit field
        val dataStoreProperty = dataStoreClass.kotlin.declaredMemberProperties
            .first { it.name == "dataStore" } as KMutableProperty<*>
        dataStoreProperty.isAccessible = true
        
        // Clear the isInitialized flag
        val isInitializedField = dataStoreField.javaClass.getDeclaredField("isInitialized")
        isInitializedField.isAccessible = true
        isInitializedField.setBoolean(dataStoreField, false)
    }

    @Test
    fun `test createDataStore creates new instance when not initialized`() {
        // Arrange
        val pathProvider = { "/test/path" }
        
        // Act
        val result = createDataStore(pathProvider)
        
        // Assert
        assertNotNull(result, "DataStore should be created")
        verify { PreferenceDataStoreFactory.createWithPath(any()) }
        verify { Log.i(any(), any()) }
    }

    @Test
    fun `test createDataStore returns existing instance when already initialized`() {
        // Arrange
        val pathProvider = { "/test/path" }
        
        // Act
        val firstResult = createDataStore(pathProvider)
        val secondResult = createDataStore(pathProvider)
        
        // Assert
        assertSame(firstResult, secondResult, "Should return the same instance")
        verify(exactly = 1) { PreferenceDataStoreFactory.createWithPath(any()) }
        verify(exactly = 1) { Log.i(any(), any()) }
        verify(exactly = 1) { Log.v(any(), any()) }
    }

    @Test
    fun `test path provider function is called`() {
        // Arrange
        val pathProviderMock = mockk<() -> String>()
        every { pathProviderMock() } returns "/test/path"
        
        // Act
        createDataStore(pathProviderMock)
        
        // Assert
        verify { pathProviderMock() }
    }

    @Test
    fun `test path is converted to Path object`() {
        // Arrange
        val testPath = "/test/path"
        val pathProvider = { testPath }
        
        // Act
        createDataStore(pathProvider)
        
        // Assert
        verify { testPath.toPath() }
    }

    @Test
    fun `test thread safety with concurrent access`() = runTest {
        // Arrange
        val pathProvider = { "/test/path" }
        val testScope = TestScope(UnconfinedTestDispatcher())
        val creationCount = 5
        
        // Act - Launch multiple coroutines to call createDataStore concurrently
        val results = mutableListOf<DataStore<Preferences>>()
        val jobs = List(creationCount) {
            testScope.launch {
                results.add(createDataStore(pathProvider))
            }
        }
        
        // Wait for all jobs to complete
        jobs.forEach { it.join() }
        
        // Assert
        verify(exactly = 1) { PreferenceDataStoreFactory.createWithPath(any()) }
        results.forEach { assertSame(results[0], it, "All instances should be the same") }
    }

    @Test
    fun `test createDataStore logs initialization message`() {
        // Arrange
        val pathProvider = { "/test/path" }
        val logMessageSlot = slot<String>()
        
        every { Log.i(any(), capture(logMessageSlot)) } returns Unit
        
        // Act
        createDataStore(pathProvider)
        
        // Assert
        assertTrue(logMessageSlot.captured.contains("/test/path"), 
            "Log message should contain the path")
    }

    @Test
    fun `test createDataStore logs already initialized message`() {
        // Arrange
        val pathProvider = { "/test/path" }
        val logMessageSlot = slot<String>()
        
        // First call to initialize
        createDataStore(pathProvider)
        
        every { Log.v(any(), capture(logMessageSlot)) } returns Unit
        
        // Act - Second call when already initialized
        createDataStore(pathProvider)
        
        // Assert
        assertTrue(logMessageSlot.captured.contains("already initialized"), 
            "Log message should indicate already initialized")
        assertTrue(logMessageSlot.captured.contains("/test/path"), 
            "Log message should contain the path")
    }

    @Test
    fun `test createDataStore with different path providers`() {
        // Arrange
        val firstPathProvider = { "/first/path" }
        val secondPathProvider = { "/second/path" }
        
        // Act
        val firstResult = createDataStore(firstPathProvider)
        
        // Reset to simulate a fresh environment
        resetDataStoreField()
        
        val secondResult = createDataStore(secondPathProvider)
        
        // Assert - Different instances due to reset between calls
        verify(exactly = 2) { PreferenceDataStoreFactory.createWithPath(any()) }
        
        // Verify both paths were used
        verify { "/first/path".toPath() }
        verify { "/second/path".toPath() }
    }

    @Test
    fun `test singleton behavior across multiple calls`() {
        // Arrange
        val pathProviders = List(5) { index -> { "/path/$index" } }
        
        // Act - Call with first path provider to initialize
        val firstResult = createDataStore(pathProviders[0])
        
        // Call with different path providers
        val results = pathProviders.drop(1).map { createDataStore(it) }
        
        // Assert
        results.forEach { 
            assertSame(firstResult, it, "All calls should return the first initialized instance") 
        }
        
        // Only the first path should be used to create the DataStore
        verify(exactly = 1) { PreferenceDataStoreFactory.createWithPath(any()) }
        verify { "/path/0".toPath() }
    }

    @Test
    fun `test multiple initialization attempts with same path`() {
        // Arrange
        val pathProvider = { "/test/path" }
        val initCount = 10
        
        // Act - Call multiple times with same path
        val results = List(initCount) { createDataStore(pathProvider) }
        
        // Assert
        results.forEach { 
            assertSame(results[0], it, "All calls should return the same instance") 
        }
        
        // Factory should be called only once
        verify(exactly = 1) { PreferenceDataStoreFactory.createWithPath(any()) }
        
        // First call logs as info, subsequent calls log as verbose
        verify(exactly = 1) { Log.i(any(), any()) }
        verify(exactly = initCount - 1) { Log.v(any(), any()) }
    }

    @Test
    fun `test synchronization lock is used`() {
        // This test verifies that the synchronized block is used
        // We need to mock the synchronized function and verify it's called
        
        // Arrange
        val pathProvider = { "/test/path" }
        val mockLock = mockk<SynchronizedObject>()
        val mockSyncResult = mockk<DataStore<Preferences>>()
        
        // Replace the lock field with our mock
        val dataStoreClass = Class.forName("com.worldwidewaves.shared.data.DataStoreKt")
        val lockField = dataStoreClass.getDeclaredField("lock")
        lockField.isAccessible = true
        val originalLock = lockField.get(null)
        lockField.set(null, mockLock)
        
        // Mock the synchronized function
        mockkStatic("kotlinx.atomicfu.locks.SynchronizedKt")
        every { 
            synchronized(mockLock, any<() -> DataStore<Preferences>>()) 
        } returns mockSyncResult
        
        try {
            // Act
            val result = createDataStore(pathProvider)
            
            // Assert
            assertSame(mockSyncResult, result, "Should return result from synchronized block")
            verify { synchronized(mockLock, any<() -> DataStore<Preferences>>()) }
        } finally {
            // Restore the original lock
            lockField.set(null, originalLock)
        }
    }

    @Test
    fun `test dataStoreFileName constant has correct value`() {
        assertEquals("wwwaves.preferences_pb", dataStoreFileName, 
            "dataStoreFileName should have the expected value")
    }

    @Test
    fun `test error handling for invalid path`() {
        // Arrange
        val invalidPathProvider = { "" }
        val exception = RuntimeException("Invalid path")
        
        every { "".toPath() } throws exception
        
        // Act & Assert
        try {
            createDataStore(invalidPathProvider)
            throw AssertionError("Should have thrown an exception")
        } catch (e: RuntimeException) {
            assertEquals("Invalid path", e.message)
        }
    }

    @Test
    fun `test error handling for DataStore creation failure`() {
        // Arrange
        val pathProvider = { "/test/path" }
        val exception = RuntimeException("DataStore creation failed")
        
        every { 
            PreferenceDataStoreFactory.createWithPath(any()) 
        } throws exception
        
        // Act & Assert
        try {
            createDataStore(pathProvider)
            throw AssertionError("Should have thrown an exception")
        } catch (e: RuntimeException) {
            assertEquals("DataStore creation failed", e.message)
        }
    }
}
