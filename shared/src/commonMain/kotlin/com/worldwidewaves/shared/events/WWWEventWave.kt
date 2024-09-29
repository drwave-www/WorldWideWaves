package com.worldwidewaves.shared.events

import androidx.annotation.VisibleForTesting
import com.worldwidewaves.shared.WWWGlobals.Companion.WAVE_OBSERVE_DELAY
import com.worldwidewaves.shared.WWWGlobals.Companion.WAVE_WARMING_DURATION
import com.worldwidewaves.shared.WWWPlatform
import com.worldwidewaves.shared.events.utils.Area
import com.worldwidewaves.shared.events.utils.BoundingBox
import com.worldwidewaves.shared.events.utils.CoroutineScopeProvider
import com.worldwidewaves.shared.events.utils.DataValidator
import com.worldwidewaves.shared.events.utils.IClock
import com.worldwidewaves.shared.events.utils.Log
import com.worldwidewaves.shared.events.utils.Position
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.datetime.Instant
import kotlinx.datetime.offsetAt
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.component.inject
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/*
 * Copyright 2024 DrWave
 *
 * WorldWideWaves is an ephemeral mobile app designed to orchestrate human waves through cities and
 * countries, culminating in a global wave. The project aims to transcend physical and cultural
 * boundaries, fostering unity, community, and shared human experience by leveraging real-time
 * coordination and location-based services.
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

    enum class Direction { WEST, EAST }
    enum class WaveMode { ADD, RECOMPOSE } // Either add new polygons to the wave or recompose it

    data class WaveNumbers(
        val waveTimezone: String,
        val waveSpeed: String,
        val waveStartTime: String,
        val waveEndTime: String,
        val waveTotalTime: String,
        val waveProgression: String
    )

    data class WaveObservation(
        val progression: Double,
        val status: IWWWEvent.Status
    )

    data class WavePolygons(
        val timestamp: Instant,
        val traversedPolygons: Area, // Maps of cutId to list of polygons
        val remainingPolygons: Area,
        val addedTraversedPolygons: Area? = null
    )

    // ---------------------------

    abstract val speed: Double // m/s
    abstract val direction: Direction // E/W
    abstract val warming: WWWEventWaveWarming

    // ---------------------------

    protected val clock: IClock by inject()
    private val coroutineScopeProvider: CoroutineScopeProvider by inject()

    // ---------------------------

    @Transient private var _event: IWWWEvent? = null
    @Transient private var _bbox: BoundingBox? = null

    @Transient private var observationJob: Job? = null
    @Transient private var lastObservedStatus: IWWWEvent.Status? = null
    @Transient private var lastObservedProgression: Double? = null

    @Transient protected var positionRequester: (() -> Position?)? = null
    @Transient private var cachedLiteralStartTime: String? = null
    @Transient private var cachedLiteralEndTime: String? = null

    // ---------------------------

    @Transient private val waveStatusChangedListeners = mutableListOf<(IWWWEvent.Status) -> Unit>()
    @Transient private val waveProgressionChangedListeners = mutableListOf<(Double) -> Unit>()
    @Transient private val waveWarmingEndedListeners = mutableListOf<() -> Unit>()
    @Transient private val waveUserIsGoingToBeHitListeners = mutableListOf<() -> Unit>()
    @Transient private val waveUserHasBeenHitListeners = mutableListOf<() -> Unit>()

    // ---------------------------

    abstract suspend fun getWavePolygons(lastWaveState: WavePolygons? = null, mode: WaveMode = WaveMode.ADD): WavePolygons?
    abstract suspend fun getWaveDuration(): Duration
    abstract suspend fun hasUserBeenHitInCurrentPosition(): Boolean
    abstract suspend fun timeBeforeHit(): Duration?

    // ---------------------------

    protected val event: IWWWEvent
        get() = requireNotNull(this._event) { "Event not set" }

    protected suspend fun bbox(): BoundingBox =
        _bbox ?: event.area.bbox().also { _bbox = it }

    @Suppress("UNCHECKED_CAST")
    fun <T : WWWEventWave> setRelatedEvent(event: IWWWEvent): T {
        this._event = event
        warming.setRelatedEvent(event)
        return this as T
    }

    fun setPositionRequester(positionRequester: () -> Position?) = apply {
        this.positionRequester = positionRequester
    }

    @VisibleForTesting
    fun getUserPosition(): Position? {
        var platform : WWWPlatform? = null
        try { platform = get() } catch (e: Exception) {
            Napier.w("${WWWEventWave::class.simpleName}: Platform not found, simulation disabled")
        }
        return if (platform?.isUnderSimulation() == true) {
            platform.getSimulation()!!.getUserPosition()
        } else {
            positionRequester?.invoke()
        }
    }

    // ---------------------------

    fun getWarmingDuration(): Duration = WAVE_WARMING_DURATION

    // ---------------------------

    /**
     * Starts observing the wave event if not already started.
     *
     * Launches a coroutine to initialize the last observed status and progression,
     * then calls `observeWave` to begin periodic checks.
     */
    private fun startObservation() {
        if (observationJob == null) {
            observationJob = coroutineScopeProvider.launchIO {
                lastObservedStatus = event.getStatus()

                try {
                    lastObservedProgression = getProgression()
                } catch (e: Throwable) {
                    Log.e(
                        tag = WWWEventWave::class.simpleName!!,
                        message = "Error initializing last observed progression: $e"
                    )
                }

                if (event.isRunning() || (event.isSoon() && isNearTheEvent())) {
                    observeWave().launchIn(coroutineScopeProvider.scopeDefault())
                }
            }
        }
    }

    /**
     * Determines if the current time is near the event start time.
     *
     * This function calculates the duration between the current time and the event start time.
     * It then checks if this duration is greater than the predefined observation delay.
     *
     */
    fun isNearTheEvent(): Boolean { // FIXME: quite duplicate with event.isSoon() !? Should be in event ?
        val now: Instant = clock.now()
        val eventStartTime: Instant = event.getStartDateTime()
        val durationUntilEvent: Duration = eventStartTime - now
        return durationUntilEvent <= WAVE_OBSERVE_DELAY
    }

    /**
     * Observes the wave event for changes in status and progression.
     *
     * Launches a coroutine to periodically check the wave's status and progression.
     * If changes are detected, the corresponding change handlers are invoked.
     */
    private fun observeWave() = flow {
        while (!event.isDone()) {
            emit(WaveObservation(getProgression(), event.getStatus()))
            delay(getObservationInterval())
        }
    }.flowOn(Dispatchers.IO)
        .catch { e -> Log.e("observeWave", "Error observing wave changes: $e") }
        .onEach { (progression, status) ->
            if (progression != lastObservedProgression) {
                lastObservedProgression = progression
                onWaveProgressionChanged(progression)
            }
            if (status != lastObservedStatus) {
                lastObservedStatus = status
                onWaveStatusChanged(status)
            }
        }

    /**
     * Calculates the observation interval for the wave event.
     *
     * This function determines the appropriate interval for observing the wave event based on the
     * current time, the event start time, and the time before the user is hit by the wave.
     *
     */
    suspend fun getObservationInterval(): Long {
        val now = clock.now()
        val eventStartTime = event.getStartDateTime()
        val timeBeforeEvent = eventStartTime - now
        val timeBeforeHit = timeBeforeHit() ?: 1.days

        val interval =  when {
            timeBeforeEvent > 1.hours + 5.minutes -> 1.hours
            timeBeforeEvent > 5.minutes + 30.seconds -> 5.minutes
            timeBeforeEvent > 35.seconds -> 1.seconds
            event.isRunning() -> 500L
            timeBeforeHit < 5.seconds -> 100L
            else -> 1.days
        }

        return if (interval is Duration) interval.inWholeMilliseconds else interval as Long
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
     * Retrieves all wave-related numbers for the event.
     *
     * This function gathers various wave-related metrics such as speed, start time, end time,
     * total time, and progression. It constructs a `WaveNumbers` object containing these metrics.
     *
     */
    suspend fun getAllNumbers(): WaveNumbers {
        suspend fun safeCall(block: suspend () -> String): String =
            try { block() } catch (e: Throwable) { "error" }

        return WaveNumbers(
            waveTimezone = safeCall { getLiteralTimezone() },
            waveSpeed = safeCall { getLiteralSpeed() },
            waveStartTime = safeCall { getLiteralStartTime() },
            waveEndTime = safeCall { getLiteralEndTime() },
            waveTotalTime = safeCall { getLiteralTotalTime() },
            waveProgression = safeCall { getLiteralProgression() }
        )
    }

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
    suspend fun getEndTime(): Instant =
        event.getStartDateTime().plus(getWaveDuration() + getWarmingDuration())

     fun isWarmingEnded(): Boolean {
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
    suspend fun getProgression(): Double = when {
        event.isDone() -> 100.0
        !event.isRunning() -> 0.0
        else -> {
            val elapsedTime = clock.now().epochSeconds - event.getStartDateTime().epochSeconds
            val totalTime = getWaveDuration().inWholeSeconds
            (elapsedTime.toDouble() / totalTime * 100).coerceAtMost(100.0)
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
    @VisibleForTesting
    suspend fun getLiteralEndTime(): String =
        cachedLiteralEndTime ?: getEndTime().let { instant ->
            IClock.instantToLiteral(instant, event.getTZ())
        }.also { cachedLiteralEndTime = it }

    /**
     * Retrieves the total time of the wave event in a human-readable format.
     *
     * This function calculates the total time of the wave event and returns it as a string
     * in the format of "X min", where X is the total time in whole minutes.
     *
     */
    suspend fun getLiteralTotalTime(): String = "${getWaveDuration().inWholeMinutes} min"

    /**
     * Retrieves the literal progression of the event as a percentage string.
     */
    suspend fun getLiteralProgression(): String = "${getProgression().roundToInt()}%"

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
    fun getLiteralStartTime(): String =
        cachedLiteralStartTime ?: event.getStartDateTime().let { instant ->
            IClock.instantToLiteral(instant, event.getTZ())
        }.also { cachedLiteralStartTime = it }

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

    override fun validationErrors(): List<String>? = mutableListOf<String>().apply {
        when {
            speed <= 0 || speed >= 20 ->
                add("Speed must be greater than 0 and less than 20")

            else -> warming.validationErrors()?.let { addAll(it) }
        }
    }.takeIf { it.isNotEmpty() }?.map { "${WWWEventWave::class.simpleName}: $it" }

}