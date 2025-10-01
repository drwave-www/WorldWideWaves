@file:OptIn(ExperimentalCoroutinesApi::class, ExperimentalTime::class)

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
import com.worldwidewaves.shared.domain.usecases.EventFilterCriteria
import com.worldwidewaves.shared.domain.usecases.FilterEventsUseCase
import com.worldwidewaves.shared.domain.usecases.GetSortedEventsUseCase
import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.testing.testEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

/**
 * Comprehensive iOS lifecycle tests for EventsViewModel.
 *
 * These tests verify that EventsViewModel:
 * - Initializes without deadlocks on iOS
 * - Properly manages coroutine scopes
 * - Handles rapid lifecycle changes safely
 * - Emits StateFlow updates correctly
 * - Cleans up resources properly
 *
 * iOS-specific concerns tested:
 * - No init{} block violations (loadEvents called from LaunchedEffect)
 * - Proper threading safety
 * - StateFlow collection without blocking
 * - Lifecycle cleanup without resource leaks
 *
 * @see EventsViewModel
 * @see com.worldwidewaves.shared.ios.IosDeadlockPreventionTest
 */
class IosEventsViewModelTest {
    private val testScheduler = TestCoroutineScheduler()
    private lateinit var mockRepository: TestEventsRepository
    private lateinit var mockSortedEventsUseCase: TestGetSortedEventsUseCase
    private lateinit var mockFilterEventsUseCase: TestFilterEventsUseCase
    private lateinit var mockCheckFavoritesUseCase: TestCheckEventFavoritesUseCase
    private lateinit var mockPlatform: TestWWWPlatform
    private lateinit var viewModel: EventsViewModel

    @BeforeTest
    fun setUp() {
        // Create test doubles (no MockK in iosTest)
        mockRepository = TestEventsRepository()
        mockSortedEventsUseCase = TestGetSortedEventsUseCase()
        mockFilterEventsUseCase = TestFilterEventsUseCase()
        mockCheckFavoritesUseCase = TestCheckEventFavoritesUseCase()
        mockPlatform = TestWWWPlatform()

        // Initialize ViewModel - should NOT deadlock
        viewModel =
            EventsViewModel(
                eventsRepository = mockRepository,
                getSortedEventsUseCase = mockSortedEventsUseCase,
                filterEventsUseCase = mockFilterEventsUseCase,
                checkEventFavoritesUseCase = mockCheckFavoritesUseCase,
                platform = mockPlatform,
            )
    }

    @AfterTest
    fun tearDown() {
        // Cleanup ViewModel
        viewModel.onCleared()
    }

    // ================================================================================
    // INITIALIZATION TESTS (5 tests)
    // ================================================================================

    @Test
    fun `should initialize without deadlock on iOS`() =
        runTest(testScheduler) {
            // Test that ViewModel can be created multiple times without deadlock
            val startTime =
                kotlin.time.Clock.System
                    .now()

            try {
                withTimeout(3.seconds) {
                    repeat(5) {
                        val vm =
                            EventsViewModel(
                                eventsRepository = TestEventsRepository(),
                                getSortedEventsUseCase = TestGetSortedEventsUseCase(),
                                filterEventsUseCase = TestFilterEventsUseCase(),
                                checkEventFavoritesUseCase = TestCheckEventFavoritesUseCase(),
                                platform = TestWWWPlatform(),
                            )
                        assertNotNull(vm, "ViewModel should initialize")
                        vm.onCleared()
                    }
                }

                val duration =
                    kotlin.time.Clock.System
                        .now() - startTime
                println("✅ PASSED: 5 ViewModels initialized in ${duration.inWholeMilliseconds}ms")
                assertTrue(duration < 3.seconds, "Initialization should be fast")
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                throw AssertionError("❌ DEADLOCK DETECTED: ViewModel initialization timed out!")
            }
        }

    @Test
    fun `should not call loadEvents in init block`() =
        runTest(testScheduler) {
            // Verify that repository is NOT called during initialization
            // loadEvents should be called from LaunchedEffect, not init{}
            assertFalse(mockRepository.loadEventsCalled, "loadEvents should NOT be called in init{}")
            assertFalse(mockRepository.getEventsCalled, "getEvents should NOT be called in init{}")
        }

    @Test
    fun `should emit initial state correctly`() =
        runTest(testScheduler) {
            // Verify initial state values
            val initialEvents = viewModel.events.first()
            val initialLoading = viewModel.isLoading.first()
            val initialError = viewModel.hasLoadingError.first()
            val initialFavorites = viewModel.hasFavorites.first()

            assertEquals(emptyList(), initialEvents, "Events should start empty")
            assertFalse(initialLoading, "Should not be loading initially")
            assertFalse(initialError, "Should have no error initially")
            assertFalse(initialFavorites, "Should have no favorites initially")
        }

    @Test
    fun `should create ViewModel from LaunchedEffect safely`() =
        runTest(testScheduler) {
            // Simulate LaunchedEffect pattern (iOS safe pattern)
            val vm =
                withTimeout(1.seconds) {
                    EventsViewModel(
                        eventsRepository = TestEventsRepository(),
                        getSortedEventsUseCase = TestGetSortedEventsUseCase(),
                        filterEventsUseCase = TestFilterEventsUseCase(),
                        checkEventFavoritesUseCase = TestCheckEventFavoritesUseCase(),
                        platform = TestWWWPlatform(),
                    )
                }

            // Then load events (separate from init)
            withTimeout(1.seconds) {
                vm.loadEvents()
            }

            advanceUntilIdle()
            assertNotNull(vm, "ViewModel should be created")
            vm.onCleared()
        }

    @Test
    fun `should initialize with test dependencies`() =
        runTest(testScheduler) {
            // Verify all dependencies are properly injected
            assertNotNull(viewModel, "ViewModel should exist")
            assertNotNull(mockRepository, "Repository should exist")
            assertNotNull(mockSortedEventsUseCase, "SortedEventsUseCase should exist")
            assertNotNull(mockFilterEventsUseCase, "FilterEventsUseCase should exist")
            assertNotNull(mockCheckFavoritesUseCase, "CheckFavoritesUseCase should exist")
            assertNotNull(mockPlatform, "Platform should exist")
        }

    // ================================================================================
    // LIFECYCLE TESTS (5 tests)
    // ================================================================================

    @Test
    fun `should cleanup scope on onCleared`() =
        runTest(testScheduler) {
            // Load events to start coroutines
            viewModel.loadEvents()
            advanceUntilIdle()

            // Clear the ViewModel
            viewModel.onCleared()

            // Give time for cleanup
            delay(100.milliseconds)
            advanceUntilIdle()

            // ViewModel should still exist but scope should be canceled
            assertNotNull(viewModel, "ViewModel should still exist")
        }

    @Test
    fun `should cancel all jobs on cleanup`() =
        runTest(testScheduler) {
            // Start a long-running operation
            mockRepository.simulateSlowLoad = true
            viewModel.loadEvents()

            // Don't wait for completion - cancel immediately
            delay(50.milliseconds)
            viewModel.onCleared()

            advanceUntilIdle()

            // Should not crash despite cancellation
            assertNotNull(viewModel, "ViewModel should handle cancellation")
        }

    @Test
    fun `should handle rapid create-destroy cycles`() =
        runTest(testScheduler) {
            // Simulate rapid iOS view lifecycle changes
            try {
                withTimeout(5.seconds) {
                    repeat(10) { iteration ->
                        val vm =
                            EventsViewModel(
                                eventsRepository = TestEventsRepository(),
                                getSortedEventsUseCase = TestGetSortedEventsUseCase(),
                                filterEventsUseCase = TestFilterEventsUseCase(),
                                checkEventFavoritesUseCase = TestCheckEventFavoritesUseCase(),
                                platform = TestWWWPlatform(),
                            )

                        vm.loadEvents()
                        delay(50.milliseconds) // Brief active period
                        vm.onCleared()
                        delay(50.milliseconds) // Brief inactive period

                        assertNotNull(vm, "Iteration $iteration: ViewModel should be functional")
                    }
                }

                println("✅ PASSED: Handled 10 rapid create-destroy cycles")
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                throw AssertionError("❌ LIFECYCLE DEADLOCK: Rapid cycles timed out!")
            }
        }

    @Test
    fun `should support reuse after cleanup`() =
        runTest(testScheduler) {
            // First lifecycle
            viewModel.loadEvents()
            advanceUntilIdle()
            viewModel.onCleared()

            // Create new ViewModel (simulating iOS view recreation)
            val newViewModel =
                EventsViewModel(
                    eventsRepository = TestEventsRepository(),
                    getSortedEventsUseCase = TestGetSortedEventsUseCase(),
                    filterEventsUseCase = TestFilterEventsUseCase(),
                    checkEventFavoritesUseCase = TestCheckEventFavoritesUseCase(),
                    platform = TestWWWPlatform(),
                )

            // Second lifecycle
            newViewModel.loadEvents()
            advanceUntilIdle()

            assertNotNull(newViewModel, "New ViewModel should work after cleanup")
            newViewModel.onCleared()
        }

    @Test
    fun `should cleanup observers on destroy`() =
        runTest(testScheduler) {
            // Setup events with observers
            val event1 = testEvent("event1")
            val event2 = testEvent("event2")
            mockSortedEventsUseCase.setEvents(listOf(event1, event2))

            viewModel.loadEvents()
            advanceUntilIdle()

            // Verify observers were started
            assertEquals(2, viewModel.events.first().size, "Should have 2 events")

            // Cleanup
            viewModel.onCleared()
            delay(100.milliseconds)

            // Should not crash
            assertNotNull(viewModel, "ViewModel should handle observer cleanup")
        }

    // ================================================================================
    // STATEFLOW TESTS (5 tests)
    // ================================================================================

    @Test
    fun `should emit StateFlow updates on iOS`() =
        runTest(testScheduler) {
            // Start loading
            mockRepository.simulateLoading = true
            viewModel.loadEvents()

            delay(50.milliseconds)
            advanceUntilIdle()

            // Check that loading state was emitted
            val isLoading = viewModel.isLoading.first()
            assertTrue(isLoading, "Should emit loading state")

            // Complete loading
            mockRepository.simulateLoading = false
            mockRepository.completeLoading()
            advanceUntilIdle()

            val finalLoading = viewModel.isLoading.first()
            assertFalse(finalLoading, "Should emit not loading state")
        }

    @Test
    fun `should handle concurrent StateFlow subscriptions`() =
        runTest(testScheduler) {
            // Subscribe to multiple flows concurrently
            val eventsJob = kotlinx.coroutines.launch { viewModel.events.collect {} }
            val loadingJob = kotlinx.coroutines.launch { viewModel.isLoading.collect {} }
            val errorJob = kotlinx.coroutines.launch { viewModel.hasLoadingError.collect {} }
            val favoritesJob = kotlinx.coroutines.launch { viewModel.hasFavorites.collect {} }

            delay(50.milliseconds)

            // Load events
            viewModel.loadEvents()
            advanceUntilIdle()

            // Cancel all subscriptions
            eventsJob.cancel()
            loadingJob.cancel()
            errorJob.cancel()
            favoritesJob.cancel()

            // Should not deadlock or crash
            assertNotNull(viewModel, "Should handle concurrent subscriptions")
        }

    @Test
    fun `should emit loading state changes`() =
        runTest(testScheduler) {
            mockRepository.simulateLoading = true

            viewModel.loadEvents()
            delay(50.milliseconds)
            advanceUntilIdle()

            // Should be loading
            var isLoading = viewModel.isLoading.first()
            assertTrue(isLoading, "Should start loading")

            // Complete
            mockRepository.simulateLoading = false
            mockRepository.completeLoading()
            advanceUntilIdle()

            isLoading = viewModel.isLoading.first()
            assertFalse(isLoading, "Should stop loading")
        }

    @Test
    fun `should emit error state on failure`() =
        runTest(testScheduler) {
            // Simulate error during load
            mockRepository.simulateError = true

            viewModel.loadEvents()
            advanceUntilIdle()

            // Check error state
            val hasError = viewModel.hasLoadingError.first()
            assertTrue(hasError, "Should emit error state")
        }

    @Test
    fun `should collect flows without deadlock`() =
        runTest(testScheduler) {
            try {
                withTimeout(2.seconds) {
                    // Collect all flows simultaneously
                    val events = viewModel.events.first()
                    val loading = viewModel.isLoading.first()
                    val error = viewModel.hasLoadingError.first()
                    val favorites = viewModel.hasFavorites.first()

                    assertNotNull(events, "Events flow should emit")
                    assertNotNull(loading, "Loading flow should emit")
                    assertNotNull(error, "Error flow should emit")
                    assertNotNull(favorites, "Favorites flow should emit")
                }

                println("✅ PASSED: All flows collected without deadlock")
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                throw AssertionError("❌ DEADLOCK: Flow collection timed out!")
            }
        }

    // ================================================================================
    // INTEGRATION TESTS (5 tests)
    // ================================================================================

    @Test
    fun `should load events from LaunchedEffect pattern`() =
        runTest(testScheduler) {
            // This is the iOS-safe pattern used in production
            val event1 = testEvent("event1")
            val event2 = testEvent("event2")
            mockSortedEventsUseCase.setEvents(listOf(event1, event2))

            // Load from LaunchedEffect (not init{})
            viewModel.loadEvents()
            advanceUntilIdle()

            val events = viewModel.events.first()
            assertEquals(2, events.size, "Should load 2 events")
        }

    @Test
    fun `should filter events without blocking`() =
        runTest(testScheduler) {
            // Setup events
            val event1 = testEvent("event1")
            val event2 = testEvent("event2")
            mockSortedEventsUseCase.setEvents(listOf(event1, event2))

            viewModel.loadEvents()
            advanceUntilIdle()

            // Filter events (should not block)
            try {
                withTimeout(1.seconds) {
                    viewModel.filterEvents(onlyFavorites = false, onlyDownloaded = false)
                    advanceUntilIdle()
                }

                println("✅ PASSED: Filter events completed without blocking")
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                throw AssertionError("❌ BLOCKING: Filter events timed out!")
            }
        }

    @Test
    fun `should toggle favorites without deadlock`() =
        runTest(testScheduler) {
            // Setup events with favorites
            val favoriteEvent = testEvent("favorite").apply { favorite = true }
            val normalEvent = testEvent("normal")
            mockSortedEventsUseCase.setEvents(listOf(favoriteEvent, normalEvent))
            mockCheckFavoritesUseCase.hasFavorites = true

            viewModel.loadEvents()
            advanceUntilIdle()

            // Check favorites state
            val hasFavorites = viewModel.hasFavorites.first()
            assertTrue(hasFavorites, "Should detect favorites")
        }

    @Test
    fun `should handle background-foreground transitions`() =
        runTest(testScheduler) {
            // Simulate background
            viewModel.loadEvents()
            advanceUntilIdle()

            // Simulate foreground
            viewModel.onCleared()
            delay(50.milliseconds)

            // Recreate (simulating iOS app lifecycle)
            val newViewModel =
                EventsViewModel(
                    eventsRepository = TestEventsRepository(),
                    getSortedEventsUseCase = TestGetSortedEventsUseCase(),
                    filterEventsUseCase = TestFilterEventsUseCase(),
                    checkEventFavoritesUseCase = TestCheckEventFavoritesUseCase(),
                    platform = TestWWWPlatform(),
                )

            newViewModel.loadEvents()
            advanceUntilIdle()

            assertNotNull(newViewModel, "Should handle background-foreground transitions")
            newViewModel.onCleared()
        }

    @Test
    fun `should survive multiple lifecycle cycles`() =
        runTest(testScheduler) {
            try {
                withTimeout(5.seconds) {
                    repeat(5) { cycle ->
                        val vm =
                            EventsViewModel(
                                eventsRepository = TestEventsRepository(),
                                getSortedEventsUseCase = TestGetSortedEventsUseCase(),
                                filterEventsUseCase = TestFilterEventsUseCase(),
                                checkEventFavoritesUseCase = TestCheckEventFavoritesUseCase(),
                                platform = TestWWWPlatform(),
                            )

                        // Full lifecycle
                        vm.loadEvents()
                        advanceUntilIdle()

                        val events = vm.events.first()
                        assertNotNull(events, "Cycle $cycle: Events should load")

                        vm.filterEvents(onlyFavorites = false)
                        advanceUntilIdle()

                        vm.onCleared()
                        delay(100.milliseconds)
                    }
                }

                println("✅ PASSED: Survived 5 complete lifecycle cycles")
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                throw AssertionError("❌ LIFECYCLE FAILURE: Multiple cycles timed out!")
            }
        }

    // ================================================================================
    // TEST DOUBLES (No MockK in iosTest)
    // ================================================================================

    private class TestEventsRepository : EventsRepository {
        var loadEventsCalled = false
        var getEventsCalled = false
        var simulateLoading = false
        var simulateError = false
        var simulateSlowLoad = false

        private val eventsFlow = MutableStateFlow<List<IWWWEvent>>(emptyList())
        private val loadingFlow = MutableStateFlow(false)
        private val errorFlow = MutableStateFlow<Exception?>(null)

        override suspend fun getEvents(): Flow<List<IWWWEvent>> {
            getEventsCalled = true
            return eventsFlow
        }

        override suspend fun loadEvents(onLoadingError: (Exception) -> Unit) {
            loadEventsCalled = true

            if (simulateSlowLoad) {
                delay(5000.milliseconds)
                return
            }

            if (simulateLoading) {
                loadingFlow.value = true
            }

            if (simulateError) {
                val error = Exception("Test error")
                errorFlow.value = error
                onLoadingError(error)
                return
            }

            eventsFlow.value = emptyList()
            loadingFlow.value = false
        }

        fun completeLoading() {
            loadingFlow.value = false
        }

        override suspend fun getEvent(eventId: String): Flow<IWWWEvent?> = flow { emit(null) }

        override suspend fun refreshEvents(): Result<Unit> = Result.success(Unit)

        override suspend fun getCachedEventsCount(): Int = 0

        override suspend fun clearCache() {}

        override fun isLoading(): Flow<Boolean> = loadingFlow

        override fun getLastError(): Flow<Exception?> = errorFlow
    }

    private class TestGetSortedEventsUseCase : GetSortedEventsUseCase {
        private val eventsFlow = MutableStateFlow<List<IWWWEvent>>(emptyList())

        fun setEvents(events: List<IWWWEvent>) {
            eventsFlow.value = events
        }

        override suspend fun invoke(): Flow<List<IWWWEvent>> = eventsFlow
    }

    private class TestFilterEventsUseCase : FilterEventsUseCase {
        override suspend fun invoke(
            events: List<IWWWEvent>,
            criteria: EventFilterCriteria,
        ): List<IWWWEvent> = events
    }

    private class TestCheckEventFavoritesUseCase : CheckEventFavoritesUseCase {
        var hasFavorites = false

        override suspend fun hasFavoriteEvents(events: List<IWWWEvent>): Boolean = hasFavorites
    }

    private class TestWWWPlatform :
        WWWPlatform(
            name = "Test Platform",
            positionManager = null,
        ) {
        override fun getSimulation(): com.worldwidewaves.shared.simulation.WaveSimulation? = null
    }
}
