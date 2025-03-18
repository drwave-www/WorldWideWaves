package com.worldwidewaves.shared.events

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

import androidx.annotation.VisibleForTesting
import com.worldwidewaves.shared.WWWGlobals.Companion.WAVE_OBSERVE_DELAY
import com.worldwidewaves.shared.WWWGlobals.Companion.WAVE_WARMING_DURATION
import com.worldwidewaves.shared.WWWGlobals.Companion.WAVE_WARN_BEFORE_HIT
import com.worldwidewaves.shared.events.IWWWEvent.EventObservation
import com.worldwidewaves.shared.events.IWWWEvent.Status
import com.worldwidewaves.shared.events.WWWEventWave.WaveNumbersLiterals
import com.worldwidewaves.shared.events.utils.CoroutineScopeProvider
import com.worldwidewaves.shared.events.utils.DataValidator
import com.worldwidewaves.shared.events.utils.IClock
import com.worldwidewaves.shared.events.utils.Log
import com.worldwidewaves.shared.getEventImage
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
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.offsetAt
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.INFINITE
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

// ---------------------------

@Serializable
data class WWWEvent(

    override val id: String,
    override val type: String,
    override val location: String,
    override val country: String? = null,
    override val community: String? = null,

    override val timeZone: String,
    override val date: String,
    override val startHour: String,

    override val description: String,
    override  val instagramAccount: String,
    override val instagramHashtag: String,

    override val wavedef: WWWWaveDefinition,
    override val area: WWWEventArea,
    override val map: WWWEventMap,

    override var favorite: Boolean = false

) : IWWWEvent, DataValidator, KoinComponent {

    @Serializable
    data class WWWWaveDefinition(
        val linear: WWWEventWaveLinear? = null,
        val deep: WWWEventWaveDeep? = null,
        val linearSplit: WWWEventWaveLinearSplit? = null
    ) : DataValidator {
        override fun validationErrors(): List<String>? = mutableListOf<String>().apply {
            when {
                linear == null && deep == null && linearSplit == null ->
                    this.add("event should contain one and only one wave definition")

                listOfNotNull(linear, deep, linearSplit).size != 1 ->
                    this.add("only one of linear, deep, or linearSplit should be non-null")

                else -> (linear ?: deep ?: linearSplit)!!.validationErrors()?.let { addAll(it) }
            }
        }.takeIf { it.isNotEmpty() }?.map { "${WWWWaveDefinition::class.simpleName}: $it" }
    }

    // ---------------------------

    private val clock: IClock by inject()

    // ---------------------------

    private val coroutineScopeProvider: CoroutineScopeProvider by inject()

    @Transient private val statusChangedListeners = mutableMapOf<Int, (Status) -> Unit>()
    @Transient private val waveProgressionChangedListeners = mutableMapOf<Int, (Double) -> Unit>()
    @Transient private val warmingEndedListeners = mutableMapOf<Int, () -> Unit>()
    @Transient private val userIsGoingToBeHitListeners = mutableMapOf<Int, () -> Unit>()
    @Transient private val userHasBeenHitListeners = mutableMapOf<Int, () -> Unit>()

    // --

    @Transient private val warmingEndedNotifiedListeners = mutableListOf<Int>()
    @Transient private val userIsGoingToBeHitNotifiedListeners = mutableListOf<Int>()
    @Transient private val userHasBeenHitNotifiedListeners = mutableListOf<Int>()

    @Transient private val statusChangedMutex = Mutex()
    @Transient private val waveProgressionChangedMutex = Mutex()
    @Transient private val warmingEndedMutex = Mutex()
    @Transient private val userIsGoingToBeHitMutex = Mutex()
    @Transient private val userHasBeenHitMutex = Mutex()

    // --

    @Transient private var isObserving: Boolean = false
    @Transient private var observationJob: Job? = null
    @Transient private var lastObservedStatus: Status? = null
    @Transient private var lastObservedProgression: Double? = null

    // ---------------------------

    @Transient private var _wave: WWWEventWave? = null
    override val wave: WWWEventWave
        get() = _wave ?: requireNotNull(wavedef.linear ?: wavedef.deep ?: wavedef.linearSplit) {
            "$id: No valid wave definition found"
        }.apply {
            setRelatedEvent<WWWEventWave>(this@WWWEvent)
            _wave = this
        }

    init {
        map.setRelatedEvent(this)
        area.setRelatedEvent(this)
    }

    @Transient override val warming = WWWEventWaveWarming(this)

    // ---------------------------

    override suspend fun getStatus(): Status {
        return when {
            isDone() -> Status.DONE
            isSoon() -> Status.SOON
            isRunning() -> Status.RUNNING
            else -> Status.NEXT
        }
    }

    override suspend fun isDone(): Boolean {
        val endDateTime = getEndDateTime()
        return endDateTime < clock.now()
    }

    override fun isSoon(): Boolean {
        val eventDateTime = getStartDateTime()
        val now = clock.now()
        return eventDateTime > now && eventDateTime <= now.plus(30.days)
    }

    override suspend fun isRunning(): Boolean {
        val startDateTime = getStartDateTime()
        val endDateTime = getEndDateTime()
        val now = clock.now()
        return startDateTime <= now && endDateTime > now
    }

    // ---------------------------

    private fun getEventImageByType(type: String, id: String?): Any? = id?.let { getEventImage(type, it) }

    override fun getLocationImage(): Any? = getEventImageByType("location", this.id)
    override fun getCommunityImage(): Any? = getEventImageByType("community", this.community)
    override fun getCountryImage(): Any? = getEventImageByType("country", this.country)

    // ---------------------------

    override fun getTZ(): TimeZone = TimeZone.of(this.timeZone)

    /**
     * Converts the start date and time of the event to a local `LocalDateTime`.
     *
     * This function parses the event's date and start hour, converts it to an `Instant` using the event's time zone,
     * and then converts it to a `LocalDateTime` in the same time zone.
     *
     */
    override fun getStartDateTime(): Instant = runCatching {
        val localDateTime = LocalDateTime.parse("${date}T${startHour}:00")
        localDateTime.toInstant(getTZ())
    }.getOrElse {
        Log.e(::getStartDateTime.name, "$id: Error parsing start date and time: $it")
        throw IllegalStateException("$id: Error parsing start date and time")
    }

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
    override suspend fun getEndDateTime(): Instant {
        var waveDuration = wave.getWaveDuration()

        if (waveDuration == 0.seconds) // If GeoJson has not been yet loaded we do not have the polygons
            waveDuration = wave.getApproxDuration()

        return getStartDateTime().plus(waveDuration + getWarmingDuration())
    }


    // ------------------------------------------------------------------------

    /**
     * Retrieves the event's time zone offset in the form "UTC+x".
     *
     * This function calculates the current offset of the event's time zone from UTC
     * and returns it as a string in the format "UTC+x".
     *
     */
    override fun getLiteralTimezone(): String {
        val offset = getTZ().offsetAt(clock.now())
        val hoursOffset = offset.totalSeconds / 3600
        return when {
            hoursOffset == 0 -> "UTC"
            hoursOffset > 0 -> "UTC+$hoursOffset"
            else -> "UTC$hoursOffset"
        }
    }

    /**
     * Converts the start date and time of the event to a simple local date format.
     *
     * This function parses the event's start date and time, converts it to the local time zone,
     * and formats it as a string in the "dd/MM" format. If the conversion fails, it returns "00/00".
     *
     */
    override fun getLiteralStartDateSimple(): String = try {
        getStartDateTime().let {
            "${it.toLocalDateTime(getTZ()).dayOfMonth.toString().padStart(2, '0')}/${
                it.toLocalDateTime(getTZ()).monthNumber.toString().padStart(2, '0')
            }"
        }
    } catch (e: Exception) {
        "error"
    }

    /**
     * Retrieves the literal start time of the event in "HH:mm" format.
     *
     * This function first checks if the start time has been cached. If it has, it returns the cached value.
     * If not, it calculates the start time by converting the event's start date and time to a local `LocalDateTime`,
     * formats the hour and minute to ensure they are two digits each, and then caches and returns the formatted time.
     *
     */
    override fun getLiteralStartTime(): String = IClock.instantToLiteral(getStartDateTime(), getTZ())

    /**
     * Retrieves the literal end time of the wave event in "HH:mm" format.
     *
     * This function checks if the end time has been previously cached. If it has, it returns the cached value.
     * Otherwise, it calculates the end time by calling `getEndTime()`, formats it to "HH:mm" format,
     * caches the result, and then returns it.
     *
     */
    @VisibleForTesting
    override suspend fun getLiteralEndTime(): String = IClock.instantToLiteral(getEndDateTime(), getTZ())

    // -----------------------------------------------------------------------

    /**
     * Retrieves the total time of the wave event in a human-readable format.
     *
     * This function calculates the total time of the wave event and returns it as a string
     * in the format of "X min", where X is the total time in whole minutes.
     *
     */
    override suspend fun getLiteralTotalTime(): String = "${wave.getWaveDuration().inWholeMinutes} min" // FIXME bad time used

    // -----------------------------------------------------------------------

    /**
     * Starting date/time of the wave
     */
    override fun getWaveStartDateTime() : Instant = getStartDateTime() + getWarmingDuration()

    /**
     * Duration of the warming phase
     */
    override fun getWarmingDuration(): Duration = WAVE_WARMING_DURATION

    /**
     * Warming is ended
     */
    override fun isWarmingEnded(): Boolean = clock.now() > getStartDateTime().plus(getWarmingDuration())

    /**
     * Determines if the current time is near the event start time.
     *
     * This function calculates the duration between the current time and the event start time.
     * It then checks if this duration is greater than the predefined observation delay.
     *
     * Is different than isSoon on the delay, isSoon is on date while isNearTime is on hours
     *
     */
    override fun isNearTime(): Boolean { // FIXME: quite duplicate with event.isSoon() !?
        val now = clock.now()
        val eventStartTime: Instant = getStartDateTime()
        val durationUntilEvent = eventStartTime - now
        return durationUntilEvent <= WAVE_OBSERVE_DELAY
    }

    // -----------------------------------------------------------------------

    /**
     * Retrieves all wave-related numbers for the event.
     *
     * This function gathers various wave-related metrics such as speed, start time, end time,
     * total time, and progression. It constructs a `WaveNumbers` object containing these metrics.
     *
     */
    override suspend fun getAllNumbers(): WaveNumbersLiterals {
        suspend fun safeCall(block: suspend () -> String): String =
            try { block() } catch (e: Throwable) { "error" }

        return WaveNumbersLiterals(
            waveTimezone = safeCall { getLiteralTimezone() },
            waveSpeed = safeCall { wave.getLiteralSpeed() },
            waveStartTime = safeCall { getLiteralStartTime() },
            waveEndTime = safeCall { getLiteralEndTime() },
            waveTotalTime = safeCall { getLiteralTotalTime() },
            waveProgression = safeCall { wave.getLiteralProgression() }
        )
    }

    // ---------------------------

    /**
     * Starts observing the wave event if not already started.
     *
     * Launches a coroutine to initialize the last observed status and progression,
     * then calls `observeWave` to begin periodic checks.
     */
    private fun startObservation() {
        if (observationJob == null) {
            coroutineScopeProvider.launchDefault {
                lastObservedStatus = getStatus()

                try {
                    lastObservedProgression = wave.getProgression()
                } catch (e: Throwable) {
                    Log.e(
                        tag = WWWEventWave::class.simpleName!!,
                        message = "Error initializing last observed progression: $e"
                    )
                }

                if (isRunning() || (isSoon() && isNearTime())) {
                    observationJob = observe().launchIn(coroutineScopeProvider.scopeDefault())
                }
            }
        }
    }

    override fun stopObservation() {
        observationJob?.cancel()
        observationJob = null
        isObserving = false
    }

    override fun stopListeners(vararg listenerKeys: Int) = stopListeners(listenerKeys.toList())
    override fun stopListeners(listenerKeys: List<Int>) {
        listenerKeys.forEach { key ->
            listOf(
                statusChangedListeners,
                waveProgressionChangedListeners,
                warmingEndedListeners,
                userIsGoingToBeHitListeners,
                userHasBeenHitListeners
            ).firstOrNull { it.remove(key) != null }
        }
    }

    /**
     * Observes the wave event for changes in status and progression.
     *
     * Launches a coroutine to periodically check the wave's status and progression.
     * If changes are detected, the corresponding change handlers are invoked.
     */
    private fun observe() = flow {
        isObserving = true
        while (isObserving) {
            emit(EventObservation(wave.getProgression(), getStatus()))
            if (isDone()) break
            delay(getObservationInterval())
        }
    }.flowOn(Dispatchers.IO)
        .catch { e -> Log.e("observeWave", "Error observing wave changes: $e") }
        .onEach { (progression, status) ->

            // No more listeners, stop observation
            if (statusChangedListeners.isEmpty() &&
                waveProgressionChangedListeners.isEmpty() &&
                warmingEndedListeners.isEmpty() &&
                userIsGoingToBeHitListeners.isEmpty() &&
                userHasBeenHitListeners.isEmpty()) {
                stopObservation()
                return@onEach
            }

            // Wave progression
            waveProgressionChangedMutex.withLock {
                if (waveProgressionChangedListeners.isNotEmpty() && progression != lastObservedProgression) {
                    lastObservedProgression = progression
                    onWaveProgressionChanged(progression)
                }
            }

            // Wave/Event status
            statusChangedMutex.withLock {
                if (statusChangedListeners.isNotEmpty() && status != lastObservedStatus) {
                    lastObservedStatus = status
                    onEventStatusChanged(status)
                }
            }

            // Warming ended
            warmingEndedMutex.withLock {
                if (warmingEndedListeners.isNotEmpty() &&
                    isWarmingEnded() && warmingEndedNotifiedListeners.size != warmingEndedListeners.size) {
                    onWarmingEnded()
                }
            }

            // User is going to be hit
            val timeBeforeHit = wave.timeBeforeHit() ?: INFINITE
            userIsGoingToBeHitMutex.withLock {
                if (userIsGoingToBeHitListeners.isNotEmpty() &&
                    (timeBeforeHit > ZERO && timeBeforeHit <= WAVE_WARN_BEFORE_HIT) &&
                    userIsGoingToBeHitNotifiedListeners.size != userIsGoingToBeHitListeners.size) {
                    onUserIsGoingToBeHit()
                }
            }

            // User has been hit
            userHasBeenHitMutex.withLock {
                if (userHasBeenHitListeners.isNotEmpty() &&
                    wave.hasUserBeenHitInCurrentPosition() &&
                    userHasBeenHitNotifiedListeners.size != userHasBeenHitListeners.size) {
                    onUserHasBeenHit()
                }
            }

        }

    /**
     * Calculates the observation interval for the wave event.
     *
     * This function determines the appropriate interval for observing the wave event based on the
     * current time, the event start time, and the time before the user is hit by the wave.
     *
     */
    private suspend fun getObservationInterval(): Long {
        val now = clock.now()
        val eventStartTime = getStartDateTime()
        val timeBeforeEvent = eventStartTime - now
        val timeBeforeHit = wave.timeBeforeHit() ?: 1.days

        val interval =  when {
            timeBeforeEvent > 1.hours + 5.minutes -> 1.hours
            timeBeforeEvent > 5.minutes + 30.seconds -> 5.minutes
            timeBeforeEvent > 35.seconds -> 1.seconds
            isRunning() -> 500L
            timeBeforeHit < 5.seconds -> 100L
            else -> 1.days
        }

        return if (interval is Duration) interval.inWholeMilliseconds else interval as Long
    }

    // ---------------------------

    private fun generateKey(prefix: Int): Int {
        val randomPart = Random.nextInt(1000000)
        return "$prefix$randomPart".toInt()
    }

    private fun <T> addListener(
        listener: T,
        listenersMap: MutableMap<Int, T>,
        mutex: Mutex,
        keyPrefix: Int
    ): Int {
        val key = generateKey(keyPrefix)
        runBlocking {
            mutex.withLock {
                if (!listenersMap.containsValue(listener)) {
                    listenersMap[key] = listener
                }
            }
            startObservation()
        }
        return key
    }

    override fun addOnStatusChangedListener(listener: (Status) -> Unit): Int =
        addListener(listener, statusChangedListeners, statusChangedMutex, 100)

    override fun addOnWaveProgressionChangedListener(listener: (Double) -> Unit): Int =
        addListener(listener, waveProgressionChangedListeners, waveProgressionChangedMutex, 200)

    override fun addOnWarmingEndedListener(listener: () -> Unit): Int =
        addListener(listener, warmingEndedListeners, warmingEndedMutex, 300)

    override fun addOnUserIsGoingToBeHitListener(listener: () -> Unit): Int =
        addListener(listener, userIsGoingToBeHitListeners, userIsGoingToBeHitMutex, 400)

    override fun addOnUserHasBeenHitListener(listener: () -> Unit): Int =
        addListener(listener, userHasBeenHitListeners, userHasBeenHitMutex, 500)

    // --------

    private fun <T> notifyListeners(
        listeners: Map<Int, T>,
        action: (T) -> Unit,
        notificationTracker: MutableList<Int>? = null
    ) {
        listeners.forEach { (key, listener) ->
            if (notificationTracker == null || key !in notificationTracker) {
                try {
                    action(listener)
                    notificationTracker?.add(key)
                } catch (e: Exception) {
                    Napier.e("Error invoking event listener: $e")
                }
            }
        }
    }

    private fun onEventStatusChanged(status: Status) =
        notifyListeners(statusChangedListeners, { it(status) })

    private fun onWaveProgressionChanged(progression: Double) =
        notifyListeners(waveProgressionChangedListeners, { it(progression) })

    private fun onWarmingEnded() =
        notifyListeners(warmingEndedListeners, { it() }, warmingEndedNotifiedListeners)

    private fun onUserIsGoingToBeHit() =
        notifyListeners(userIsGoingToBeHitListeners, { it() }, userIsGoingToBeHitNotifiedListeners)

    private fun onUserHasBeenHit() =
        notifyListeners(userHasBeenHitListeners, { it() }, userHasBeenHitNotifiedListeners)

    // ---------------------------

    /**
     * This function checks the event for various validation criteria.
     */
    override fun validationErrors() : List<String>? = mutableListOf<String>().apply {
        when {
            id.isEmpty() ->
                this.add("ID is empty")

            !id.matches(Regex("^[a-z_]+$")) ->
                this.add("ID must be lowercase with only simple letters or underscores")

            type.isEmpty() ->
                this.add("Type is empty")

            type !in listOf("city", "country", "world") ->
                this.add("Type must be either 'city', 'country', or 'world'")

            location.isEmpty() ->
                this.add("Location is empty")

            type == "city" && country.isNullOrEmpty() ->
                this.add("Country must be specified for type 'city'")

            timeZone.isEmpty() ->
                this.add("Time zone is empty")

            !date.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) || runCatching { LocalDate.parse(date) }.isFailure ->
                this.add("Date format is invalid or date is not valid")

            !startHour.matches(Regex("\\d{2}:\\d{2}")) || runCatching { LocalTime.parse(startHour) }.isFailure ->
                this.add("Start hour format is invalid or time is not valid")

            description.isEmpty() ->
                this.add("Description is empty")

            instagramAccount.isEmpty() ->
                this.add("Instagram account is empty")

            !instagramAccount.matches(Regex("^[A-Za-z0-9_.]+$")) ->
                this.add("Instagram account is invalid")

            instagramHashtag.isEmpty() ->
                this.add("Instagram hashtag is empty")

            !instagramHashtag.matches(Regex("^#[A-Za-z0-9_]+$")) ->
                this.add("Instagram hashtag is invalid")

            runCatching { TimeZone.of(timeZone) }.isFailure ->
                this.add("Time zone is invalid")

            else -> wavedef.validationErrors()?.let { addAll(it) }
                .also { area.validationErrors()?.let { addAll(it) } }
                .also { map.validationErrors()?.let { addAll(it) } }
        }
    }.takeIf { it.isNotEmpty() }?.map { "${WWWEvent::class.simpleName}: $it" }

}


