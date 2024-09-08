package com.worldwidewaves.shared.events

import com.worldwidewaves.shared.WWWGlobals.Companion.WAVE_OBSERVE_DELAY
import com.worldwidewaves.shared.events.utils.CoroutineScopeProvider
import com.worldwidewaves.shared.events.utils.DataValidator
import com.worldwidewaves.shared.events.utils.IClock
import com.worldwidewaves.shared.events.utils.Log
import com.worldwidewaves.shared.events.utils.Polygon
import com.worldwidewaves.shared.events.utils.Position
import com.worldwidewaves.shared.events.utils.isPointInPolygon
import com.worldwidewaves.shared.getLocalDatetime
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.offsetAt
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

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
abstract class WWWEventWave : KoinComponent, DataValidator {

    data class WaveNumbers(
        val waveTimezone: String,
        val waveSpeed: String,
        val waveStartTime: String,
        val waveEndTime: String,
        val waveTotalTime: String,
        val waveProgression: String
    )

    // ---------------------------

    abstract val speed: Double
    abstract val direction: String
    abstract val warming: WWWEventWaveWarming

    // ---------------------------

    private val clock: IClock by inject()
    private val coroutineScopeProvider: CoroutineScopeProvider by inject()

    // ---------------------------

    @Transient private var _event: IWWWEvent? = null

    @Transient private var observationStarted = false
    @Transient private var lastObservedStatus: IWWWEvent.Status? = null
    @Transient private var lastObservedProgression: Double? = null

    @Transient protected var positionRequester: (() -> Position?)? = null
    @Transient private var cachedLiteralStartTime: String? = null
    @Transient private var cachedEndTime: LocalDateTime? = null

    // ---------------------------

    @Transient private val waveStatusChangedListeners = mutableListOf<(IWWWEvent.Status) -> Unit>()
    @Transient private val waveProgressionChangedListeners = mutableListOf<(Double) -> Unit>()
    @Transient private val waveWarmingEndedListeners = mutableListOf<() -> Unit>()
    @Transient private val waveUserIsGoingToBeHitListeners = mutableListOf<() -> Unit>()
    @Transient private val waveUserHasBeenHitListeners = mutableListOf<() -> Unit>()

    // ---------------------------

    abstract suspend fun getWarmingPolygons(): List<Polygon>
    abstract suspend fun getWaveDuration(): Duration
    abstract suspend fun getWarmingDuration(): Duration
    abstract suspend fun hasUserBeenHit(): Boolean

    // ---------------------------

    protected val event: IWWWEvent
        get() = this._event ?: run {
            Log.e(::event.name, "Event not set")
            throw IllegalStateException("Event not set")
        }

    fun setEvent(event: IWWWEvent) = apply { this._event = event }

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
            observationStarted = true

            coroutineScopeProvider.scopeIO.launch {
                lastObservedStatus = event.getStatus()

                try {
                    lastObservedProgression = getProgression()
                } catch (e: Throwable) {
                    Log.e(
                        tag = "WWWEventWave",
                        message = "Error initializing last observed progression: $e"
                    )
                }

                if (event.isRunning() || (event.isSoon() && isNearTheEvent())) {
                    observeWave()
                }
            }
        }
    }

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
     * Determines if the current time is near the event start time.
     *
     * This function calculates the duration between the current time and the event start time.
     * It then checks if this duration is greater than the predefined observation delay.
     *
     */
    fun isNearTheEvent(): Boolean {
        val eventTimeZone: TimeZone = event.getTZ()
        val now: Instant = clock.now().toLocalDateTime(eventTimeZone).toInstant(eventTimeZone)
        val eventStartTime: Instant = event.getStartDateTime().toInstant(eventTimeZone)
        val durationUntilEvent: Duration = eventStartTime - now
        return durationUntilEvent <= WAVE_OBSERVE_DELAY.hours
    }

    /**
     * Observes the wave event for changes in status and progression.
     *
     * Launches a coroutine to periodically check the wave's status and progression.
     * If changes are detected, the corresponding change handlers are invoked.
     */
    private fun observeWave() {
        coroutineScopeProvider.scopeIO.launch {
            while (!event.isDone()) {
                try {
                    getProgression().takeIf { it != lastObservedProgression }?.also {
                        lastObservedProgression = it
                        onWaveProgressionChanged(it)
                    }
                    event.getStatus().takeIf { it != lastObservedStatus }?.also {
                        lastObservedStatus = it
                        onWaveStatusChanged(it)
                    }

                    delay(getObservationInterval())

                } catch (e: Throwable) {
                    Log.e(::observeWave.name, "Error observing wave changes: $e")
                }
            }
        }
    }

    fun getObservationInterval(): Long {
        val now = clock.now()
        val eventStartTime = event.getStartDateTime().toInstant(event.getTZ())
        val durationUntilEvent = eventStartTime - now

        return when {
            durationUntilEvent > 1.hours + 5.minutes -> 1.hours.inWholeMilliseconds
            durationUntilEvent > 5.minutes + 30.seconds -> 5.minutes.inWholeMilliseconds
            durationUntilEvent > 35.seconds -> 1.seconds.inWholeMilliseconds
            event.isRunning() -> 500L else -> 500L
        }
    }

    // ---------------------------

    fun addOnWaveStatusChangedListener(listener: (IWWWEvent.Status) -> Unit) = apply {
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

    private fun onWaveStatusChanged(status: IWWWEvent.Status) = waveStatusChangedListeners.forEach { it(status) }
    private fun onWaveProgressionChanged(progression: Double) = waveProgressionChangedListeners.forEach { it(progression) }

    protected fun onWaveWarmingEnded() = waveWarmingEndedListeners.forEach { it() }
    protected fun onWaveUserIsGoingToBeHit() = waveUserIsGoingToBeHitListeners.forEach { it() }
    protected fun onWaveUserUserHasBeenHit() = waveUserHasBeenHitListeners.forEach { it() }

    // ---------------------------

    /**
     * Calculates the end time of the event based on its start time, bounding box, and speed.
     *
     * This function determines the end time of the event by performing the following steps:
     * 1. Retrieves the start date and time of the event in the local time zone.
     * 2. Obtains the bounding box of the event area.
     * 3. Calculates the average latitude of the bounding box.
     * 4. Computes the distance across the bounding box at the average latitude.
     * 5. Calculates the duration of the event based on the distance and the event's speed.
     * 6. Adds the duration to the start time to get the end time.
     *
     */
     suspend fun getEndTime(): LocalDateTime {
        return cachedEndTime ?: run {
            val startDateTime = event.getStartDateTime()
            val duration = getWaveDuration() + getWarmingDuration()
            startDateTime.toInstant(event.getTZ()).plus(duration).toLocalDateTime(event.getTZ())
        }.also { cachedEndTime = it }
     }

     suspend fun isWarmingEnded(): Boolean {
        TODO("Not yet implemented")
     }

    /**
     * Calculates the literal progression of the event as a percentage.
     *
     * This function determines the progression of the event based on its current state and elapsed time.
     * If the event is done, it returns "100%". If the event is not running, it returns "0%".
     * Otherwise, it calculates the elapsed time since the event started and expresses it as a percentage
     * of the total event duration.
     *
     */
     suspend fun getProgression(): Double {
        return when {
            event.isDone() -> 100.0
            !event.isRunning() -> 0.0
            else -> {
                val elapsedTime = getLocalDatetime().toInstant(event.getTZ()).epochSeconds -
                        event.getStartDateTime().toInstant(event.getTZ()).epochSeconds
                val totalTime = getWaveDuration().inWholeSeconds
                (elapsedTime.toDouble() / totalTime * 100).coerceAtMost(100.0)
            }
        }
    }

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
        return "${getWaveDuration().inWholeMinutes} min"
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
            waveTimezone = try { getLiteralTimezone() } catch (e: Throwable) { "error" },
            waveSpeed = try { getLiteralSpeed() } catch (e: Throwable) { "error" },
            waveStartTime = try { getLiteralStartTime() } catch (e: Throwable) { "error" },
            waveEndTime = try { getLiteralEndTime() } catch (e: Throwable) { "error" },
            waveTotalTime = try { getLiteralTotalTime() } catch (e: Throwable) { "error" },
            waveProgression = try { getLiteralProgression() } catch (e: Throwable) { "error" }
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
        val offset = event.getTZ().offsetAt(clock.now())
        val hoursOffset = offset.totalSeconds / 3600
        return when {
            hoursOffset == 0 -> "UTC"
            hoursOffset > 0 -> "UTC+$hoursOffset"
            else -> "UTC$hoursOffset"
        }
    }

    // ---------------------------

    override fun validationErrors(): List<String>? = mutableListOf<String>()
        .apply {
            when {
                speed <= 0 || speed >= 20 ->
                    add("Speed must be greater than 0 and less than 20")

                direction != "west" && direction != "east" ->
                    add("Direction must be either 'west' or 'east'")

                else -> warming.validationErrors()?.let { addAll(it) }
            }
        }.takeIf { it.isNotEmpty() }?.map { "wave: $it" }

}