package com.worldwidewaves.shared.events

/*
 * Copyright 2025 DrWave
 *
 * WorldWideWaves is an ephemeral mobile app designed to orchestrate human waves through cities and
 * countries. The project aims to transcend physical and cultural
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

import com.worldwidewaves.shared.MokoRes
import com.worldwidewaves.shared.WWWPlatform
import com.worldwidewaves.shared.choreographies.ChoreographyManager
import com.worldwidewaves.shared.choreographies.ChoreographyManager.DisplayableSequence
import com.worldwidewaves.shared.events.utils.Area
import com.worldwidewaves.shared.events.utils.BoundingBox
import com.worldwidewaves.shared.events.utils.DataValidator
import com.worldwidewaves.shared.events.utils.IClock
import com.worldwidewaves.shared.events.utils.Position
import com.worldwidewaves.shared.localizeString
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.jetbrains.compose.resources.DrawableResource
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.component.inject
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.time.toDuration

@OptIn(ExperimentalTime::class)
@Serializable
/**
 * Base abstraction for the "wave" part of a World-Wide-Waves event.
 *
 * A wave is the dynamic entity that travels across the event area and drives most
 * of the real-time behaviour exposed to the UI:
 * • Time helpers – duration, start-/end-time, progression.
 * • Spatial helpers – closest longitude, user-hit prediction, polygons split.
 * • Choreography hooks – warming / waiting / hit sequences via
 *   [ChoreographyManager].
 *
 * Concrete subclasses (e.g. linear, deep, split) implement the geography-specific
 * maths while this class provides common utilities and caching helpers.
 */
abstract class WWWEventWave :
    KoinComponent,
    DataValidator {
    enum class Direction { WEST, EAST }

    @OptIn(ExperimentalTime::class)
    /**
     * Immutable snapshot of the wave geometry at a given moment.
     *
     *  • *traversedPolygons* : part of the area already crossed by the wave
     *  • *remainingPolygons* : part still to come
     *
     *  UI layers use this to render previously-hit polygons with a different
     *  style while keeping the rest untouched.
     */
    data class WavePolygons(
        val timestamp: Instant,
        val traversedPolygons: Area, // Maps of cutId to list of polygons
        val remainingPolygons: Area,
    )

    // ---------------------------

    abstract val speed: Double // m/s
    abstract val direction: Direction // E/W
    abstract val approxDuration: Int // Min

    fun getApproxDuration(): Duration = approxDuration.toDuration(DurationUnit.MINUTES)

    // ---------------------------

    protected val clock: IClock by inject()
    private val choreographyManager: ChoreographyManager<DrawableResource> by inject()

    // ---------------------------

    @Transient private var _event: IWWWEvent? = null

    @Transient protected var positionRequester: (() -> Position?)? = null

    @Transient private val _positionUpdates = MutableSharedFlow<Position?>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    // ---------------------------

    abstract suspend fun getWavePolygons(): WavePolygons?

    abstract suspend fun getWaveDuration(): Duration

    abstract suspend fun hasUserBeenHitInCurrentPosition(): Boolean

    abstract suspend fun userHitDateTime(): Instant?

    abstract suspend fun closestWaveLongitude(latitude: Double): Double

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

    /**
     * Reactive flow of position updates. Emits whenever user position changes.
     */
    @Transient
    val positionUpdates: SharedFlow<Position?> = _positionUpdates.asSharedFlow()

    fun setPositionRequester(positionRequester: () -> Position?) =
        apply {
            this.positionRequester = positionRequester
        }

    /**
     * Notifies that the position has changed. Should be called by location providers
     * when position updates occur to trigger reactive position-dependent calculations.
     */
    fun notifyPositionChanged(position: Position?) {
        _positionUpdates.tryEmit(position)
    }

    fun getUserPosition(): Position? {
        var platform: WWWPlatform? = null
        try {
            platform = get()
        } catch (_: Exception) {
            Napier.w("${WWWEventWave::class.simpleName}: Platform not found, simulation disabled")
        }
        return if (platform?.isOnSimulation() == true) {
            platform.getSimulation()!!.getUserPosition()
        } else {
            positionRequester?.invoke()
        }
    }

    suspend fun timeBeforeUserHit(): Duration? {
        if (hasUserBeenHitInCurrentPosition()) return null
        val hitTime = userHitDateTime() ?: return null

        // Calculate the duration between now and the hit time
        return hitTime - clock.now()
    }

    suspend fun userClosestWaveLongitude(): Double? {
        val userPosition = getUserPosition() ?: return null
        return closestWaveLongitude(userPosition.lat)
    }

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
    suspend fun getProgression(): Double =
        when {
            event.isDone() -> 100.0
            !event.isRunning() -> 0.0
            else -> {
                val elapsedTime = clock.now().epochSeconds - event.getWaveStartDateTime().epochSeconds
                val totalTime = getWaveDuration().inWholeSeconds
                (elapsedTime.toDouble() / totalTime * 100).coerceIn(0.0, 100.0)
            }
        }

    fun getLiteralFromProgression(progression: Double): String =
        if (progression.isNaN()) "N/A" else "${(progression * 10).roundToInt() / 10.0}%"

    fun getLiteralSpeed(): String = "$speed ${localizeString(MokoRes.strings.speed_unit_mps)}"

    // ---------------------------

    override fun validationErrors(): List<String>? =
        mutableListOf<String>()
            .apply {
                when {
                    speed <= 0 || speed >= 20 ->
                        add("Speed must be greater than 0 and less than 20")

                    else -> { /* No validation errors */ }
                }
            }.takeIf { it.isNotEmpty() }
            ?.map { "${WWWEventWave::class.simpleName}: $it" }

    // ---------------------------

    fun waitingChoregraphySequence(): DisplayableSequence<DrawableResource>? = choreographyManager.getWaitingSequenceImmediate()

    // TIMING-CRITICAL: Hit sequence for precise wave synchronization
    fun hitChoregraphySequence(): DisplayableSequence<DrawableResource>? = choreographyManager.getHitSequenceImmediate()
}
