package com.worldwidewaves.utils

/*
 * Copyright 2025 DrWave
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE.org/licenses/LICENSE-2.0
 */

import android.content.Context
import com.worldwidewaves.shared.WWWPlatform
import com.worldwidewaves.shared.WWWSimulation
import com.worldwidewaves.shared.events.utils.Position
import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Tests for SimulationLocationEngine GPS_MARKER handling.
 *
 * Ensures GPS_MARKER (999.0, 999.0) sentinel value is never passed to
 * MapLibre APIs, which would cause crashes due to invalid coordinates.
 */
@OptIn(ExperimentalTime::class)
class SimulationLocationEngineTest {
    @Test
    fun `getSimulatedLocation returns null when GPS_MARKER is used`() {
        // GIVEN: Platform with GPS_MARKER simulation (time acceleration, use real GPS)
        val now = Instant.fromEpochMilliseconds(1000)
        val simulation =
            WWWSimulation(
                startDateTime = now,
                userPosition = WWWSimulation.GPS_MARKER,
                initialSpeed = 1,
            )

        val mockPlatform = mockk<WWWPlatform>(relaxed = true)
        every { mockPlatform.isOnSimulation() } returns true
        every { mockPlatform.getSimulation() } returns simulation

        val mockContext = mockk<Context>(relaxed = true)
        val engine = SimulationLocationEngine(mockContext)

        // Access private method via reflection for testing
        val getSimulatedLocationMethod =
            SimulationLocationEngine::class.java.getDeclaredMethod("getSimulatedLocation")
        getSimulatedLocationMethod.isAccessible = true

        // WHEN: Getting simulated location with GPS_MARKER
        val result = getSimulatedLocationMethod.invoke(engine) as? android.location.Location

        // THEN: Should return null to fall back to real device GPS
        assertNull(
            result,
            "GPS_MARKER should return null to prevent MapLibre crash with invalid coordinates",
        )
    }

    @Test
    fun `getSimulatedLocation returns valid location for normal position`() {
        // GIVEN: Platform with valid simulation position (Paris)
        val now = Instant.fromEpochMilliseconds(1000)
        val parisPosition = Position(48.8566, 2.3522)
        val simulation =
            WWWSimulation(
                startDateTime = now,
                userPosition = parisPosition,
                initialSpeed = 1,
            )

        val mockPlatform = mockk<WWWPlatform>(relaxed = true)
        every { mockPlatform.isOnSimulation() } returns true
        every { mockPlatform.getSimulation() } returns simulation

        val mockContext = mockk<Context>(relaxed = true)
        val engine = SimulationLocationEngine(mockContext)

        // Access private method via reflection for testing
        val getSimulatedLocationMethod =
            SimulationLocationEngine::class.java.getDeclaredMethod("getSimulatedLocation")
        getSimulatedLocationMethod.isAccessible = true

        // WHEN: Getting simulated location with valid position
        val result = getSimulatedLocationMethod.invoke(engine) as? android.location.Location

        // THEN: Should return valid location with correct coordinates
        assertNotNull(result, "Valid position should return non-null Location")
        assertEquals(48.8566, result.latitude, 0.0001, "Latitude should match Paris")
        assertEquals(2.3522, result.longitude, 0.0001, "Longitude should match Paris")
    }
}
