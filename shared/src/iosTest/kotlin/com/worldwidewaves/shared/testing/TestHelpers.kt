package com.worldwidewaves.shared.testing

/*
 * Copyright 2025 DrWave
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * https://www.apache.org/licenses/LICENSE-2.0
 */

import com.worldwidewaves.shared.events.IWWWEvent
import com.worldwidewaves.shared.events.WWWEvent
import com.worldwidewaves.shared.events.WWWEventArea
import com.worldwidewaves.shared.events.WWWEventMap
import com.worldwidewaves.shared.events.WWWEventObserver
import com.worldwidewaves.shared.events.WWWEventWave
import com.worldwidewaves.shared.events.WWWEventWaveLinear
import com.worldwidewaves.shared.events.WWWEventWaveWarming
import dev.icerock.moko.resources.StringResource
import kotlinx.datetime.TimeZone
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Simple test implementation of IWWWEvent for iOS tests.
 *
 * This implementation provides minimal valid data to satisfy the IWWWEvent interface
 * without requiring complex dependencies or Koin initialization.
 */
@OptIn(ExperimentalTime::class)
private class TestEvent(
    override val id: String = "test_event",
) : IWWWEvent {
    override val type: String = "city"
    override val country: String = "usa"
    override val community: String = "new_york"
    override val timeZone: String = "America/New_York"
    override val date: String = "2025-10-15"
    override val startHour: String = "18:00"
    override val instagramAccount: String = "worldwidewaves"
    override val instagramHashtag: String = "#wwwaves"

    override val wavedef: WWWEvent.WWWWaveDefinition =
        WWWEvent.WWWWaveDefinition(
            linear =
                WWWEventWaveLinear(
                    speed = 50.0,
                    direction = WWWEventWave.Direction.WEST,
                    approxDuration = 3600,
                ),
            deep = null,
            linearSplit = null,
        )

    override val area: WWWEventArea =
        WWWEventArea(
            osmAdminids = listOf(1, 2, 3),
            bbox = "-74.01,40.71,-74.00,40.72",
        )

    override val map: WWWEventMap =
        WWWEventMap(
            maxZoom = 16.0,
            language = "en",
            zone = "north-america/us/new-york",
        )

    // Lazy initialization to avoid KoinComponent issues
    private val _warming by lazy {
        WWWEventWaveWarming(this)
    }
    override val warming: WWWEventWaveWarming get() = _warming

    private val _wave by lazy {
        wavedef.linear ?: error("No linear wave defined")
    }
    override val wave: WWWEventWave get() = _wave

    override var favorite: Boolean = false

    // Create a simple observer without Koin dependencies
    private val _observer by lazy {
        WWWEventObserver(this)
    }
    override val observer: WWWEventObserver get() = _observer

    override fun getEventObserver(): WWWEventObserver = _observer

    // Status methods
    override suspend fun getStatus(): IWWWEvent.Status = IWWWEvent.Status.NEXT

    override suspend fun isDone(): Boolean = false

    override fun isSoon(): Boolean = false

    override suspend fun isRunning(): Boolean = false

    override fun isNearTime(): Boolean = false

    // Image methods
    override fun getLocationImage(): Any? = null

    override fun getCommunityImage(): Any? = null

    override fun getCountryImage(): Any? = null

    override fun getMapImage(): Any? = null

    // Localized string methods - return simple StringResource mock
    private val mockStringResource = StringResource("mock_resource")

    override fun getLocation(): StringResource = mockStringResource

    override fun getDescription(): StringResource = mockStringResource

    override fun getLiteralCountry(): StringResource = mockStringResource

    override fun getLiteralCommunity(): StringResource = mockStringResource

    // Time methods
    override fun getTZ(): TimeZone = TimeZone.of(timeZone)

    override fun getStartDateTime(): Instant = Instant.parse("2025-10-15T18:00:00Z")

    override suspend fun getTotalTime(): Duration = 1.hours

    override suspend fun getEndDateTime(): Instant = getStartDateTime().plus(getTotalTime())

    override fun getLiteralTimezone(): String = "UTC-5"

    override fun getLiteralStartDateSimple(): String = "15/10"

    override fun getLiteralStartTime(): String = "18:00"

    override suspend fun getLiteralEndTime(): String = "19:00"

    override suspend fun getLiteralTotalTime(): String = "60 min"

    override fun getWaveStartDateTime(): Instant = getStartDateTime()

    override fun getWarmingDuration(): Duration = Duration.ZERO

    override suspend fun getAllNumbers(): IWWWEvent.WaveNumbersLiterals =
        IWWWEvent.WaveNumbersLiterals(
            waveTimezone = "UTC-5",
            waveSpeed = "50 km/h",
            waveStartTime = "18:00",
            waveEndTime = "19:00",
            waveTotalTime = "60 min",
        )

    override fun validationErrors(): List<String>? = null
}

/**
 * Get a test event for iOS tests.
 *
 * Returns a minimal valid IWWWEvent implementation suitable for testing
 * without requiring Koin or complex dependencies.
 */
fun testEvent(id: String = "test_event"): IWWWEvent = TestEvent(id)
