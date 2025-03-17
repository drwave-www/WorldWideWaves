package com.worldwidewaves.shared.events

import androidx.annotation.VisibleForTesting
import com.worldwidewaves.shared.WWWPlatform
import com.worldwidewaves.shared.events.utils.Area
import com.worldwidewaves.shared.events.utils.BoundingBox
import com.worldwidewaves.shared.events.utils.DataValidator
import com.worldwidewaves.shared.events.utils.IClock
import com.worldwidewaves.shared.events.utils.Position
import io.github.aakira.napier.Napier
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.component.inject
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.DurationUnit
import kotlin.time.toDuration

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

    data class WaveNumbersLiterals(
        val waveTimezone: String = "",
        val waveSpeed: String = "..",
        val waveStartTime: String = "..",
        val waveEndTime: String = "..",
        val waveTotalTime: String = "..",
        val waveProgression: String = ".."
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
    abstract val approxDuration: Int // Min

    fun getApproxDuration(): Duration = approxDuration.toDuration(DurationUnit.MINUTES)

    // ---------------------------

    protected val clock: IClock by inject()

    // ---------------------------

    @Transient private var _event: IWWWEvent? = null
    @Transient protected var positionRequester: (() -> Position?)? = null

    // ---------------------------

    abstract suspend fun getWavePolygons(lastWaveState: WavePolygons? = null, mode: WaveMode = WaveMode.ADD): WavePolygons?
    abstract suspend fun getWaveDuration(): Duration
    abstract suspend fun hasUserBeenHitInCurrentPosition(): Boolean
    abstract suspend fun timeBeforeHit(): Duration?
    abstract suspend fun userClosestWaveLongitude(): Double?
    abstract suspend fun userPositionToWaveRatio(): Double?

    // ---------------------------

    protected val event: IWWWEvent
        get() = requireNotNull(this._event) { "Event not set" }

    protected suspend fun bbox(): BoundingBox = event.area.bbox()

    @Suppress("UNCHECKED_CAST")
    fun <T : WWWEventWave> setRelatedEvent(event: IWWWEvent): T {
        this._event = event
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

    suspend fun userIsGoingToBeHit(): Boolean = runCatching {
        timeBeforeHit()?.let { duration ->
            duration <= 1.minutes
        } ?: false
    }.getOrDefault(false)

    // ---------------------------

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
        !event.isRunning() || !event.isWarmingEnded() -> 0.0
        else -> {
            Napier.v("${WWWEventWave::class.simpleName}: current time is ${IClock.instantToLiteral(clock.now(), event.getTZ())}")
            val elapsedTime = clock.now().epochSeconds - event.getWaveStartDateTime().epochSeconds
            val totalTime = getWaveDuration().inWholeSeconds
            (elapsedTime.toDouble() / totalTime * 100).coerceAtMost(100.0)
        }
    }

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

    // ---------------------------

    override fun validationErrors(): List<String>? = mutableListOf<String>().apply {
        when {
            speed <= 0 || speed >= 20 ->
                add("Speed must be greater than 0 and less than 20")

            else -> { /* No validation errors */ }
        }
    }.takeIf { it.isNotEmpty() }?.map { "${WWWEventWave::class.simpleName}: $it" }

}