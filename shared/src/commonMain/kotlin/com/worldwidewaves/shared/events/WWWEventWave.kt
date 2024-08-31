package com.worldwidewaves.shared.events

import com.worldwidewaves.shared.WWWGlobals.Companion.WAVE_OBSERVE_DELAY
import com.worldwidewaves.shared.events.utils.Polygon
import com.worldwidewaves.shared.events.utils.Position
import com.worldwidewaves.shared.events.utils.isPointInPolygon
import com.worldwidewaves.shared.events.utils.splitPolygonByLongitude
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.offsetAt
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

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

@Serializable
data class Warming(
    val type: String,
    val longitude: Double? = null,
)

data class WaveNumbers(
    val waveTimezone: String,
    val waveSpeed: String,
    val waveStartTime: String,
    val waveEndTime: String,
    val waveTotalTime: String,
    val waveProgression: String
)

// ---------------------------

abstract class WWWEventWave {

    private var _event: WWWEvent? = null

    abstract val speed: Double
    abstract val direction: String
    abstract val warming: Warming

    // ---------------------------

    private var observationStarted = false
    private var lastObservedStatus: WWWEvent.Status? = null
    private var lastObservedProgression: Double? = null

    protected var positionRequester: (() -> Position?)? = null
    private var cachedLiteralStartTime: String? = null

    // ---------------------------

    private val waveStatusChangedListeners = mutableListOf<(WWWEvent.Status) -> Unit>()
    private val waveProgressionChangedListeners = mutableListOf<(Double) -> Unit>()
    private val waveWarmingEndedListeners = mutableListOf<() -> Unit>()
    private val waveUserIsGoingToBeHitListeners = mutableListOf<() -> Unit>()
    private val waveUserHasBeenHitListeners = mutableListOf<() -> Unit>()

    // ---------------------------

    abstract suspend fun getObservationInterval(): Long
    abstract suspend fun getEndTime(): LocalDateTime
    abstract suspend fun getTotalTime(): Duration
    abstract suspend fun getProgression(): Double
    abstract suspend fun isWarmingEnded(): Boolean
    abstract suspend fun hasUserBeenHit(): Boolean

    // ---------------------------

    protected val event: WWWEvent
        get() = this._event ?: run {
            Napier.e(tag = "WWWEventWave", message = "Event not set")
            throw IllegalStateException("Event not set")
        }

    fun setEvent(event: WWWEvent) = apply { this._event = event }

    fun setPositionRequester(positionRequester: () -> Position?) = apply {
        this.positionRequester = positionRequester
    }

    // ---------------------------

    /**
     * Starts observing the wave event if not already started.
     *
     * Launches a coroutine to initialize the last observed status and progression,
     * then calls `observeWave` to begin periodic checks.
     */
    private fun startObservation() {
        if (!observationStarted) {
            CoroutineScope(Dispatchers.IO).launch {
                lastObservedStatus = event.getStatus()
                lastObservedProgression = getProgression()

                val eventTimeZone: TimeZone = event.getTimeZone() // TODO: test
                val now: Instant = Clock.System.now().toLocalDateTime(eventTimeZone).toInstant(eventTimeZone)
                val eventStartTime: Instant = event.getStartDateTime().toInstant(eventTimeZone)
                val durationUntilEvent: Duration = eventStartTime - now

                if (!event.isDone() && !(event.isSoon() && durationUntilEvent > WAVE_OBSERVE_DELAY.hours)) {
                    observeWave()
                }

                observationStarted = true
            }
        }
    }

    /**
     * Observes the wave event for changes in status and progression.
     *
     * Launches a coroutine to periodically check the wave's status and progression.
     * If changes are detected, the corresponding change handlers are invoked.
     */
    private fun observeWave() {
        CoroutineScope(Dispatchers.IO).launch {
            delay(getObservationInterval())
            getProgression().takeIf { it != lastObservedProgression }?.also {
                lastObservedProgression = it
                onWaveProgressionChanged(it)
            }
            event.getStatus().takeIf { it != lastObservedStatus }?.also {
                lastObservedStatus = it
                onWaveStatusChanged(it)
            }
        }
    }

    // ---------------------------

    fun addOnWaveStatusChangedListener(listener: (WWWEvent.Status) -> Unit) = apply {
        waveStatusChangedListeners.add(listener)
    }.also { startObservation() }

    fun addOnWaveProgressionChangedListener(listener: (Double) -> Unit) = apply {
        waveProgressionChangedListeners.add(listener)
    }.also { startObservation() }

    fun addOnWaveWarmingEndedListener(listener: () -> Unit) = apply {
        waveWarmingEndedListeners.add(listener)
    }.also { startObservation() }

    fun addOnWaveUserIsGoingToBeHitListener(listener: () -> Unit) = apply {
        waveUserIsGoingToBeHitListeners.add(listener)
    }.also { startObservation() }

    fun addOnWaveUserUserHasBeenHitListener(listener: () -> Unit) = apply {
        waveUserHasBeenHitListeners.add(listener)
    }.also { startObservation() }

    private fun onWaveStatusChanged(status: WWWEvent.Status) = waveStatusChangedListeners.forEach { it(status) }
    private fun onWaveProgressionChanged(progression: Double) = waveProgressionChangedListeners.forEach { it(progression) }

    protected fun onWaveWarmingEnded() = waveWarmingEndedListeners.forEach { it() }
    protected fun onWaveUserIsGoingToBeHit() = waveUserIsGoingToBeHitListeners.forEach { it() }
    protected fun onWaveUserUserHasBeenHit() = waveUserHasBeenHitListeners.forEach { it() }

    // ---------------------------

    /**
     * Retrieves the literal end time of the wave event in "HH:mm" format.
     *
     * This function checks if the end time has been previously cached. If it has, it returns the cached value.
     * Otherwise, it calculates the end time by calling `getEndTime()`, formats it to "HH:mm" format,
     * caches the result, and then returns it.
     *
     */
    suspend fun getLiteralEndTime(): String {
        val endDateTime = getEndTime()
        val hour = endDateTime.hour.toString().padStart(2, '0')
        val minute = endDateTime.minute.toString().padStart(2, '0')
        return "$hour:$minute"
    }

    /**
     * Retrieves the total time of the wave event in a human-readable format.
     *
     * This function calculates the total time of the wave event and returns it as a string
     * in the format of "X min", where X is the total time in whole minutes.
     *
     */
    suspend fun getLiteralTotalTime(): String {
        return "${getTotalTime().inWholeMinutes} min"
    }

    /**
     * Retrieves the literal progression of the event as a percentage string.
     */
    suspend fun getLiteralProgression(): String {
        return "${getProgression()}%"
    }

    //---------------------------

    /**
     * Retrieves all wave-related numbers for the event.
     *
     * This function gathers various wave-related metrics such as speed, start time, end time,
     * total time, and progression. It constructs a `WaveNumbers` object containing these metrics.
     *
     */
    suspend fun getAllNumbers(): WaveNumbers {
        return WaveNumbers(
            waveTimezone = getLiteralTimezone(),
            waveSpeed = getLiteralSpeed(),
            waveStartTime = getLiteralStartTime(),
            waveEndTime = getLiteralEndTime(),
            waveTotalTime = getLiteralTotalTime(),
            waveProgression = getLiteralProgression()
        )
    }

    // ---------------------------

    /**
     * Retrieves the literal speed of the event in meters per second.
     *
     * This function returns the speed of the event as a string formatted with "m/s".
     *
     */
    fun getLiteralSpeed(): String = "$speed m/s"

    /**
     * Retrieves the literal start time of the event in "HH:mm" format.
     *
     * This function first checks if the start time has been cached. If it has, it returns the cached value.
     * If not, it calculates the start time by converting the event's start date and time to a local `LocalDateTime`,
     * formats the hour and minute to ensure they are two digits each, and then caches and returns the formatted time.
     *
     */
    fun getLiteralStartTime(): String {
        return cachedLiteralStartTime ?: event.getStartDateTime().let { localDateTime ->
            val hour = localDateTime.hour.toString().padStart(2, '0')
            val minute = localDateTime.minute.toString().padStart(2, '0')
            "$hour:$minute"
        }.also { cachedLiteralStartTime = it }
    }

    /**
     * Retrieves the event's time zone offset in the form "UTC+x".
     *
     * This function calculates the current offset of the event's time zone from UTC
     * and returns it as a string in the format "UTC+x".
     *
     */
    fun getLiteralTimezone(): String {
        val offset = TimeZone.of(event.timeZone).offsetAt(Clock.System.now())
        val hoursOffset = offset.totalSeconds / 3600
        return when {
            hoursOffset == 0 -> "UTC"
            hoursOffset > 0 -> "UTC+$hoursOffset"
            else -> "UTC$hoursOffset"
        }
    }

    // ---------------------------

    private var cachedWarmingPolygons: List<Polygon>? = null

    /**
     * Checks if a given position is within any of the warming polygons.
     *
     * This function retrieves the warming polygons and checks if the specified position
     * is within any of these polygons using the `isPointInPolygon` function.
     *

     */
    suspend fun isPositionWithinWarming(position: Position): Boolean {
        return getWarmingPolygons().any { isPointInPolygon(position, it) }
    }

    /**
     * Retrieves the warming polygons for the event area.
     *
     * This function returns a list of polygons representing the warming zones for the event area.
     * If the warming polygons are already cached, it returns the cached value. Otherwise, it splits
     * the event area polygon by the warming zone longitude and caches the resulting right-side polygons.
     *
     */
    suspend fun getWarmingPolygons(): List<Polygon> {
        if (cachedWarmingPolygons == null) {
            cachedWarmingPolygons = when (warming.type) {
                "longitude-cut" -> splitPolygonByLongitude(
                    event.area.getPolygon(),
                    warming.longitude ?: run {
                        Napier.e(tag = "WWWEventWave", message = "Longitude not set in configuration")
                        return emptyList()
                    }
                ).right
                else -> emptyList()
            }
        }
        return cachedWarmingPolygons!!
    }

}