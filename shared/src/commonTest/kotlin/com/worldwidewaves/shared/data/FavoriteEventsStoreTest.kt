package com.worldwidewaves.shared.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.preferencesOf
import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.events.utils.Log
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class FavoriteEventsStoreTest {

    // Test objects
    private lateinit var mockDataStore: DataStore<Preferences>
    private lateinit var preferencesFlow: MutableStateFlow<Preferences>
    private lateinit var favoriteEventsStore: FavoriteEventsStore

    @BeforeTest
    fun setup() {
        // Mock Log object
        mockkObject(Log)
        every { Log.e(any(), any(), any()) } returns Unit
        
        // Setup mock DataStore
        preferencesFlow = MutableStateFlow(emptyPreferences())
        mockDataStore = mockk<DataStore<Preferences>>().apply {
            every { data } returns preferencesFlow
            coEvery { this@apply.edit(any()) } coAnswers {
                val transform = firstArg<suspend (Preferences) -> Preferences>()
                val newPreferences = runBlocking { transform(preferencesFlow.value) }
                preferencesFlow.value = newPreferences
                newPreferences
            }
        }
        
        // Create the store with default dispatcher
        favoriteEventsStore = FavoriteEventsStore(mockDataStore)
    }

    @AfterTest
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `test setFavoriteStatus sets value in preferences`() = runTest {
        // Arrange
        val eventId = "test-event-1"
        val expectedKey = booleanPreferencesKey("favorite_$eventId")
        
        // Act
        favoriteEventsStore.setFavoriteStatus(eventId, true)
        
        // Assert
        assertTrue(preferencesFlow.value[expectedKey] == true, 
            "Preference should be set to true")
            
        // Act again with false
        favoriteEventsStore.setFavoriteStatus(eventId, false)
        
        // Assert
        assertTrue(preferencesFlow.value[expectedKey] == false, 
            "Preference should be set to false")
    }

    @Test
    fun `test isFavorite returns correct value`() = runTest {
        // Arrange
        val eventId = "test-event-2"
        val key = booleanPreferencesKey("favorite_$eventId")
        preferencesFlow.value = preferencesOf(key to true)
        
        // Act
        val result = favoriteEventsStore.isFavorite(eventId)
        
        // Assert
        assertTrue(result, "Should return true for favorited event")
        
        // Arrange - set to false
        preferencesFlow.value = preferencesOf(key to false)
        
        // Act
        val resultFalse = favoriteEventsStore.isFavorite(eventId)
        
        // Assert
        assertFalse(resultFalse, "Should return false for non-favorited event")
    }
    
    @Test
    fun `test isFavorite returns false for non-existent preferences`() = runTest {
        // Arrange - empty preferences
        preferencesFlow.value = emptyPreferences()
        
        // Act
        val result = favoriteEventsStore.isFavorite("non-existent-event")
        
        // Assert
        assertFalse(result, "Should return false for non-existent preference")
    }
    
    @Test
    fun `test isFavorite handles DataStore errors`() = runTest {
        // Arrange - create a DataStore that throws an exception
        val errorDataStore = mockk<DataStore<Preferences>>()
        val exception = RuntimeException("DataStore error")
        
        every { errorDataStore.data } returns flow { throw exception }
        
        val errorStore = FavoriteEventsStore(errorDataStore)
        
        // Act & Assert - should not throw and return false
        val result = errorStore.isFavorite("any-event")
        assertFalse(result, "Should return false when DataStore throws an exception")
        
        // Verify error was logged
        verify { Log.e(any(), any(), any()) }
    }
    
    @Test
    fun `test with different event IDs`() = runTest {
        // Arrange
        val eventIds = listOf("event1", "event2", "event3")
        
        // Act - set all to favorites
        eventIds.forEach { eventId ->
            favoriteEventsStore.setFavoriteStatus(eventId, true)
        }
        
        // Assert - all should be favorites
        eventIds.forEach { eventId ->
            assertTrue(favoriteEventsStore.isFavorite(eventId), 
                "Event $eventId should be favorite")
        }
        
        // Act - set one to non-favorite
        favoriteEventsStore.setFavoriteStatus("event2", false)
        
        // Assert - verify specific states
        assertTrue(favoriteEventsStore.isFavorite("event1"), "Event1 should be favorite")
        assertFalse(favoriteEventsStore.isFavorite("event2"), "Event2 should not be favorite")
        assertTrue(favoriteEventsStore.isFavorite("event3"), "Event3 should be favorite")
    }
    
    @Test
    fun `test concurrent operations`() = runTest {
        // Arrange
        val eventId = "concurrent-event"
        
        // Act - launch multiple coroutines to update simultaneously
        launch {
            favoriteEventsStore.setFavoriteStatus(eventId, true)
        }
        launch {
            favoriteEventsStore.setFavoriteStatus(eventId, false)
        }
        
        // Complete all coroutines
        advanceUntilIdle()
        
        // Assert - one of the values should have won (last writer wins)
        // We can't predict which one, but it should be either true or false
        val key = booleanPreferencesKey("favorite_$eventId")
        assertTrue(preferencesFlow.value.contains(key), 
            "Preference should exist after concurrent operations")
    }
    
    @Test
    fun `test InitFavoriteEvent functionality`() = runTest {
        // Arrange
        val eventId = "test-event-init"
        val key = booleanPreferencesKey("favorite_$eventId")
        preferencesFlow.value = preferencesOf(key to true)
        
        val mockEvent = mockk<IWWWEvent>()
        every { mockEvent.id } returns eventId
        every { mockEvent.favorite = any() } returns Unit
        
        val initFavoriteEvent = InitFavoriteEvent(favoriteEventsStore)
        
        // Act
        initFavoriteEvent.call(mockEvent)
        
        // Assert
        verify { mockEvent.favorite = true }
        
        // Arrange - change to non-favorite
        preferencesFlow.value = preferencesOf(key to false)
        
        // Act
        initFavoriteEvent.call(mockEvent)
        
        // Assert
        verify { mockEvent.favorite = false }
    }
    
    @Test
    fun `test SetEventFavorite functionality`() = runTest {
        // Arrange
        val eventId = "test-event-set"
        val mockEvent = mockk<IWWWEvent>()
        every { mockEvent.id } returns eventId
        every { mockEvent.favorite = any() } returns Unit
        
        val setEventFavorite = SetEventFavorite(favoriteEventsStore)
        
        // Act
        setEventFavorite.call(mockEvent, true)
        
        // Assert
        verify { mockEvent.favorite = true }
        val key = booleanPreferencesKey("favorite_$eventId")
        assertTrue(preferencesFlow.value[key] == true, 
            "Preference should be set to true")
        
        // Act - set to false
        setEventFavorite.call(mockEvent, false)
        
        // Assert
        verify { mockEvent.favorite = false }
        assertTrue(preferencesFlow.value[key] == false, 
            "Preference should be set to false")
    }
    
    @Test
    fun `test edge cases with empty event IDs`() = runTest {
        // Arrange
        val emptyEventId = ""
        
        // Act
        favoriteEventsStore.setFavoriteStatus(emptyEventId, true)
        val result = favoriteEventsStore.isFavorite(emptyEventId)
        
        // Assert
        assertTrue(result, "Should handle empty event ID")
        
        // Verify correct key was used
        val key = booleanPreferencesKey("favorite_")
        assertTrue(preferencesFlow.value.contains(key), 
            "Should use correct key for empty event ID")
    }
    
    @Test
    fun `test favoriteKey function creates correct keys`() = runTest {
        // Test by setting values and checking the keys in preferences
        
        // Arrange & Act
        favoriteEventsStore.setFavoriteStatus("event1", true)
        favoriteEventsStore.setFavoriteStatus("event2", true)
        
        // Assert
        val key1 = booleanPreferencesKey("favorite_event1")
        val key2 = booleanPreferencesKey("favorite_event2")
        
        assertTrue(preferencesFlow.value.contains(key1), "Should contain key for event1")
        assertTrue(preferencesFlow.value.contains(key2), "Should contain key for event2")
    }
}
