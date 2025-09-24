package com.worldwidewaves.shared.viewmodels

/*
 * Copyright 2025 DrWave
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

import com.worldwidewaves.shared.WWWPlatform
import com.worldwidewaves.shared.domain.repository.EventsRepository
import com.worldwidewaves.shared.domain.usecases.CheckEventFavoritesUseCase
import com.worldwidewaves.shared.domain.usecases.FilterEventsUseCase
import com.worldwidewaves.shared.domain.usecases.GetSortedEventsUseCase
import com.worldwidewaves.shared.events.IWWWEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * iOS-specific tests for EventsViewModel to verify iOS integration works correctly.
 */
class EventsViewModelIOSTest {

    @Test
    fun `EventsViewModel initializes with empty state`() = runTest {
        val viewModel = createTestEventsViewModel()

        val events = viewModel.events.first()
        val isLoading = viewModel.isLoading.first()
        val hasError = viewModel.hasLoadingError.first()
        val hasFavorites = viewModel.hasFavorites.first()

        assertEquals(emptyList(), events)
        assertFalse(isLoading)
        assertFalse(hasError)
        assertFalse(hasFavorites)
    }

    @Test
    fun `filterEvents with onlyFavorites works correctly`() = runTest {
        val viewModel = createTestEventsViewModel()

        // Should not throw exception
        viewModel.filterEvents(onlyFavorites = true, onlyDownloaded = false)
    }

    @Test
    fun `filterEvents with onlyDownloaded works correctly`() = runTest {
        val viewModel = createTestEventsViewModel()

        // Should not throw exception
        viewModel.filterEvents(onlyFavorites = false, onlyDownloaded = true)
    }

    @Test
    fun `filterEvents with both flags works correctly`() = runTest {
        val viewModel = createTestEventsViewModel()

        // Should not throw exception
        viewModel.filterEvents(onlyFavorites = true, onlyDownloaded = true)
    }

    /**
     * Create a test EventsViewModel with mock dependencies.
     */
    private fun createTestEventsViewModel(): EventsViewModel {
        return EventsViewModel(
            eventsRepository = MockEventsRepository(),
            getSortedEventsUseCase = MockGetSortedEventsUseCase(),
            filterEventsUseCase = MockFilterEventsUseCase(),
            checkEventFavoritesUseCase = MockCheckEventFavoritesUseCase(),
            platform = MockWWWPlatform()
        )
    }

    /**
     * Mock implementations for testing
     */
    private class MockEventsRepository : EventsRepository {
        override fun loadEvents(onError: (Exception) -> Unit) {}
        override fun isLoading(): Flow<Boolean> = flowOf(false)
        override fun getLastError(): Flow<Exception?> = flowOf(null)
    }

    private class MockGetSortedEventsUseCase : GetSortedEventsUseCase {
        override fun invoke(): Flow<List<IWWWEvent>> = flowOf(emptyList())
    }

    private class MockFilterEventsUseCase : FilterEventsUseCase {
        override fun invoke(events: List<IWWWEvent>, criteria: com.worldwidewaves.shared.domain.usecases.EventFilterCriteria): List<IWWWEvent> = emptyList()
    }

    private class MockCheckEventFavoritesUseCase : CheckEventFavoritesUseCase {
        override fun hasFavoriteEvents(events: List<IWWWEvent>): Boolean = false
    }

    private class MockWWWPlatform : WWWPlatform {
        override fun getName(): String = "iOS Test"
        override fun getSimulation(): com.worldwidewaves.shared.WWWSimulation? = null
    }
}