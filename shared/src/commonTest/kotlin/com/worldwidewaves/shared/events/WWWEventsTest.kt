package com.worldwidewaves.shared.events

/*
 * Copyright 2024 DrWave
 *
 * WorldWideWaves is an ephemeral mobile app designed to orchestrate human waves through cities and countries,
 * culminating in a global wave. The project aims to transcend physical and cultural boundaries, fostering unity,
 * community, and shared human experience by leveraging real-time coordination and location-based services.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.worldwidewaves.shared.data.InitFavoriteEvent
import com.worldwidewaves.shared.events.utils.CoroutineScopeProvider
import com.worldwidewaves.shared.events.utils.DefaultCoroutineScopeProvider
import com.worldwidewaves.shared.events.utils.EventsConfigurationProvider
import com.worldwidewaves.shared.events.utils.EventsDecoder
import io.github.aakira.napier.Antilog
import io.github.aakira.napier.LogLevel
import io.github.aakira.napier.Napier
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.inject
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class WWWEventsTest : KoinTest {

    private val dispatcher = StandardTestDispatcher() // Create dispatcher instance
    private val exception = Exception("Test exception")

    private val mockedKoinDeclaration: KoinAppDeclaration = {
        modules(
            module {
                single { spyk(WWWEvents()) } // <-- tested class
                single<InitFavoriteEvent> { mockk() }
                single<EventsConfigurationProvider> {
                    spyk(object : EventsConfigurationProvider {
                        override suspend fun geoEventsConfiguration(): String = "[]" // Empty events
                    })
                }
                single<EventsDecoder> { spyk(object : EventsDecoder{
                    override fun decodeFromJson(jsonString: String): List<IWWWEvent> = emptyList()
                }) }
                single<CoroutineScopeProvider> { DefaultCoroutineScopeProvider(dispatcher, dispatcher) }
            }
        )
    }

    // ----------------------------

    init {
        Napier.base(object : Antilog() {
            override fun performLog(priority: LogLevel, tag: String?, throwable: Throwable?, message: String?) {
                println(message)
            }
        })
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        startKoin { mockedKoinDeclaration() }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @AfterTest
    fun tearDown() {
        stopKoin()
        Dispatchers.resetMain()
    }

    // ----------------------------

    @Test
    fun `WWWEvents should be initialized with empty events`() {
        // GIVEN
        val events: WWWEvents by inject()

        // THEN
        assertTrue(events.flow().value.isEmpty())
        assertTrue(events.list().isEmpty())
        assertTrue(events.getEventById("1") == null)
    }

    // ----------------------------

    enum class RegistrationMethod { ONCALL, BEFORE, AFTER }
    enum class CallbackType { ONLOADED, ONERROR, ONTERMINATION }
    enum class ErrorType { READCONF, DECODE, VALIDATION }

    private fun createCallbackTests(
        registrationMethod: RegistrationMethod,
        callbackType: CallbackType,
        shouldThrowException: Boolean,
        shouldBeCalled: Boolean = true,
        errorType: ErrorType = ErrorType.VALIDATION
    ) = runTest { // use co-routines test

        // GIVEN
        var callbackCalled = false // capture() doesn't work here
        val events: WWWEvents by inject()

        if (shouldThrowException) {
            when (errorType) {
                ErrorType.VALIDATION -> {
                    val eventsConfigurationProvider : EventsConfigurationProvider by inject()
                    coEvery { eventsConfigurationProvider.geoEventsConfiguration() } throws exception
                }
                ErrorType.DECODE -> {
                    val eventsDecoder: EventsDecoder by inject()
                    coEvery { eventsDecoder.decodeFromJson(any()) } throws exception
                }
                ErrorType.READCONF -> {
                    val eventsConfigurationProvider : EventsConfigurationProvider by inject()
                    coEvery { eventsConfigurationProvider.geoEventsConfiguration() } throws exception
                }
            }
        }

        // WHEN
        when (registrationMethod) {
            RegistrationMethod.BEFORE -> {
                when (callbackType) {
                    CallbackType.ONLOADED -> events.addOnEventsLoadedListener { callbackCalled = true }
                    CallbackType.ONERROR -> events.addOnEventsErrorListener { callbackCalled = true }
                    CallbackType.ONTERMINATION -> events.addOnTerminationListener { callbackCalled = true }
                }
                events.loadEvents()
                testScheduler.advanceUntilIdle() // Wait for termination
            }
            RegistrationMethod.ONCALL -> {
                when (callbackType) {
                    CallbackType.ONLOADED -> events.loadEvents(onLoaded = { callbackCalled = true })
                    CallbackType.ONERROR -> events.loadEvents(onLoadingError = { callbackCalled = true })
                    CallbackType.ONTERMINATION -> events.loadEvents(onTermination = { callbackCalled = true })
                }
                testScheduler.advanceUntilIdle() // Wait for termination
            }
            RegistrationMethod.AFTER -> {
                events.loadEvents()
                testScheduler.advanceUntilIdle() // Wait for termination
                when (callbackType) {
                    CallbackType.ONLOADED -> events.addOnEventsLoadedListener { callbackCalled = true }
                    CallbackType.ONERROR -> events.addOnEventsErrorListener { callbackCalled = true }
                    CallbackType.ONTERMINATION -> events.addOnTerminationListener { callbackCalled = true }
                }
            }
        }

        // THEN
        assertEquals(shouldBeCalled, callbackCalled)
    }

    // ----------------------------

    @Test
    fun `On LOADED WWWEvents should callback OnLoaded through loadEvents`() = createCallbackTests(
        RegistrationMethod.ONCALL, CallbackType.ONLOADED, shouldThrowException = false
    )

    @Test
    fun `On LOADED WWWEvents should not callback OnError through loadEvents`() = createCallbackTests(
        RegistrationMethod.ONCALL, CallbackType.ONERROR, shouldThrowException = false, shouldBeCalled = false
    )

    @Test
    fun `On LOADED WWWEvents should callback OnLoaded when listener registered before loading`() = createCallbackTests(
        RegistrationMethod.BEFORE, CallbackType.ONLOADED, shouldThrowException = false
    )

    @Test
    fun `On LOADED WWWEvents should not callback OnError when listener registered before loading`() = createCallbackTests(
        RegistrationMethod.BEFORE, CallbackType.ONERROR, shouldThrowException = false, shouldBeCalled = false
    )

    @Test
    fun `On LOADED WWWEvents should callback OnLoaded when listener registered after loading`() = createCallbackTests(
        RegistrationMethod.AFTER, CallbackType.ONLOADED, shouldThrowException = false
    )

    @Test
    fun `On LOADED WWWEvents should not callback OnError when listener registered after loading`() = createCallbackTests(
        RegistrationMethod.AFTER, CallbackType.ONERROR, shouldThrowException = false, shouldBeCalled = false
    )

    // ----------------------------

    @Test
    fun `On ERROR WWWEvents should callback OnError through loadEvents`() = createCallbackTests(
        RegistrationMethod.ONCALL, CallbackType.ONERROR, shouldThrowException = true
    )

    @Test
    fun `On ERROR WWWEvents should not callback OnLoaded through loadEvents`() = createCallbackTests(
        RegistrationMethod.ONCALL, CallbackType.ONLOADED, shouldThrowException = true, shouldBeCalled = false
    )

    @Test
    fun `On ERROR WWWEvents should callback OnError when listener registered before loading`() = createCallbackTests(
        RegistrationMethod.BEFORE, CallbackType.ONERROR, shouldThrowException = true
    )

    @Test
    fun `On ERROR WWWEvents should not callback OnLoaded when listener registered before loading`() = createCallbackTests(
        RegistrationMethod.BEFORE, CallbackType.ONLOADED, shouldThrowException = true, shouldBeCalled = false
    )

    @Test
    fun `On ERROR WWWEvents should callback OnError when listener registered after loading`() = createCallbackTests(
        RegistrationMethod.AFTER, CallbackType.ONERROR, shouldThrowException = true
    )

    @Test
    fun `On ERROR WWWEvents should not callback OnLoaded when listener registered after loading`() = createCallbackTests(
        RegistrationMethod.AFTER, CallbackType.ONLOADED, shouldThrowException = true, shouldBeCalled = false
    )

    // ----------------------------

    @Test
    fun `On TERMINATE-LOADED WWWEvents should callback OnTermination through loadEvents`() = createCallbackTests(
        RegistrationMethod.ONCALL, CallbackType.ONTERMINATION, shouldThrowException = false
    )

    @Test
    fun `On TERMINATE-ERROR WWWEvents should callback OnTermination through loadEvents`() = createCallbackTests(
        RegistrationMethod.ONCALL, CallbackType.ONTERMINATION, shouldThrowException = true
    )

    @Test
    fun `On TERMINATE-LOADED WWWEvents should callback OnTermination when listener registered before loading`() = createCallbackTests(
        RegistrationMethod.BEFORE, CallbackType.ONTERMINATION, shouldThrowException = false
    )

    @Test
    fun `On TERMINATE-LOADED WWWEvents should callback OnTermination when listener registered after loading`() = createCallbackTests(
        RegistrationMethod.AFTER, CallbackType.ONTERMINATION, shouldThrowException = false
    )

    @Test
    fun `On TERMINATE-ERROR WWWEvents should callback OnTermination when listener registered before loading`() = createCallbackTests(
        RegistrationMethod.BEFORE, CallbackType.ONTERMINATION, shouldThrowException = true
    )

    @Test
    fun `On TERMINATE-ERROR WWWEvents should callback OnTermination when listener registered after loading`() = createCallbackTests(
        RegistrationMethod.AFTER, CallbackType.ONTERMINATION, shouldThrowException = true
    )

    // ----------------------------

    @Test
    fun `On READCONF ERROR WWWEvents should callback OnError`() = createCallbackTests(
        RegistrationMethod.ONCALL, CallbackType.ONERROR, shouldThrowException = true, errorType = ErrorType.READCONF
    )

    @Test
    fun `On DECODE ERROR WWWEvents should callback OnError`() = createCallbackTests(
        RegistrationMethod.ONCALL, CallbackType.ONERROR, shouldThrowException = true, errorType = ErrorType.DECODE
    )

    @Test
    fun `On VALIDATION ERROR WWWEvents should callback OnError`() = createCallbackTests(
        RegistrationMethod.ONCALL, CallbackType.ONERROR, shouldThrowException = true, errorType = ErrorType.VALIDATION
    )

    // ----------------------------

    @Test
    fun `WWWEvents should be initialized with events`() = runTest {
        // GIVEN
        val events: WWWEvents by inject()
        every { events.confValidationErrors(any()) } returns mapOf( // List two unaccessed events
            Pair(mockk<IWWWEvent>( relaxed = true ), emptyList()),
            Pair(mockk<IWWWEvent>( relaxed = true ), emptyList())
        )
        val initFavoriteEvent: InitFavoriteEvent by inject() // Fake init event favorite status
        coEvery { initFavoriteEvent.call(any()) } returns Unit

        // WHEN
        events.loadEvents()
        testScheduler.advanceUntilIdle() // Wait for termination

        // THEN
        assertTrue(events.isLoaded())
        assertEquals(2, events.flow().value.size)
        assertEquals(2, events.list().size)
    }

    @Test
    fun `WWWEvents in ERROR should return the associated Exception`() = runTest {
        // GIVEN
        val events: WWWEvents by inject()
        val eventsConfigurationProvider : EventsConfigurationProvider by inject()
        coEvery { eventsConfigurationProvider.geoEventsConfiguration() } throws exception

        // WHEN
        events.loadEvents()
        testScheduler.advanceUntilIdle() // Wait for termination

        // THEN
        assertNotNull(events.getLoadingError())
    }

    @Test
    fun `getEventById should be return events by id`() = runTest {
        // GIVEN
        val events: WWWEvents by inject()
        val fakeEventId = "fake_event_id"
        val eventWithId = mockk<IWWWEvent>( relaxed = true ) {
            every { id } returns fakeEventId
        }
        every { events.confValidationErrors(any()) } returns mapOf( // List two unaccessed events
            Pair(eventWithId, null),
            Pair(mockk<IWWWEvent>( relaxed = true ), emptyList())
        )
        val initFavoriteEvent: InitFavoriteEvent by inject() // Fake init event favorite status
        coEvery { initFavoriteEvent.call(any()) } returns Unit

        // WHEN
        events.loadEvents()
        testScheduler.advanceUntilIdle() // Wait for termination

        val eventById = events.getEventById(fakeEventId)

        // THEN
        assertEquals(2, events.flow().value.size)
        assertEquals(2, events.list().size)
        assertEquals(eventWithId, eventById)
    }

    @Test
    fun `WWWEvents should filter events with validation errors`() = runTest {
        // GIVEN
        val events: WWWEvents by inject()
        val fakeEventId = "fake_event_id"
        val eventWithId = mockk<IWWWEvent>( relaxed = true ) {
            every { id } returns fakeEventId
        }
        val eventFiltered = mockk<IWWWEvent>( relaxed = true )
        val validationError1 = "Validation error 1"
        val validationError2 = "Validation error 2"
        every { events.confValidationErrors(any()) } returns mapOf( // List two unaccessed events
            Pair(eventWithId, emptyList()),
            Pair(eventFiltered, listOf(validationError1, validationError2)
            )
        )
        val initFavoriteEvent: InitFavoriteEvent by inject() // Fake init event favorite status
        coEvery { initFavoriteEvent.call(any()) } returns Unit

        // WHEN
        events.loadEvents()
        testScheduler.advanceUntilIdle() // Wait for termination

        val eventById = events.getEventById(fakeEventId)

        val validationErrors = events.getValidationErrors().firstOrNull()

        // THEN
        assertEquals(1, events.flow().value.size) // Only one event kept
        assertEquals(1, events.list().size)
        assertEquals(eventWithId, eventById)
        assertNotNull(validationErrors)
        assertEquals(eventFiltered, validationErrors.first)
        assertEquals(2, validationErrors.second.size)
        assertTrue(validationErrors.second.contains(validationError1))
        assertTrue(validationErrors.second.contains(validationError2))
    }


}