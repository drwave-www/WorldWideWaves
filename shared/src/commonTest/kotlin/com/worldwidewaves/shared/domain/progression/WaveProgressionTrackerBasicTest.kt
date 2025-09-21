@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.worldwidewaves.shared.domain.progression

import com.worldwidewaves.shared.events.utils.IClock
import io.mockk.every
import io.mockk.mockk
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class WaveProgressionTrackerBasicTest {

    @Test
    fun `can create DefaultWaveProgressionTracker`() {
        val mockClock = mockk<IClock>()
        every { mockClock.now() } returns Instant.fromEpochSeconds(1000)

        val tracker = DefaultWaveProgressionTracker(mockClock)

        assertNotNull(tracker)
        assertEquals(0, tracker.getProgressionHistory().size)
    }

    @Test
    fun `clearProgressionHistory works`() {
        val mockClock = mockk<IClock>()
        every { mockClock.now() } returns Instant.fromEpochSeconds(1000)

        val tracker = DefaultWaveProgressionTracker(mockClock)
        tracker.clearProgressionHistory()

        assertEquals(0, tracker.getProgressionHistory().size)
    }
}