package com.worldwidewaves.shared.events

/*
 * Copyright 2025 DrWave
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
import com.worldwidewaves.shared.WWWGlobals.Companion.WAVE_SOON_DELAY
import com.worldwidewaves.shared.WWWGlobals.Companion.WAVE_WARN_BEFORE_HIT
import com.worldwidewaves.shared.events.IWWWEvent.EventObservation
import com.worldwidewaves.shared.events.IWWWEvent.Status
import com.worldwidewaves.shared.events.WWWEventWave.WaveNumbersLiterals
import com.worldwidewaves.shared.events.utils.CoroutineScopeProvider
import com.worldwidewaves.shared.events.utils.DataValidator
import com.worldwidewaves.shared.events.utils.IClock
import com.worldwidewaves.shared.events.utils.Log
import com.worldwidewaves.shared.getEventImage
import com.worldwidewaves.shared.utils.updateIfChanged
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.offsetAt
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.INFINITE
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.time.Instant.Companion.DISTANT_FUTURE

// ---------------------------

@OptIn(ExperimentalTime::class)
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

    @Transient private val _eventStatus = MutableStateFlow(Status.UNDEFINED)
    @Transient override val eventStatus: StateFlow<Status> = _eventStatus.asStateFlow()

    @Transient private val _progression = MutableStateFlow(0.0)
    @Transient override val progression: StateFlow<Double> = _progression.asStateFlow()

    @Transient private val _isWarmingInProgress = MutableStateFlow(false)
    @Transient override val isWarmingInProgress: StateFlow<Boolean> = _isWarmingInProgress.asStateFlow()

    @Transient private val _userIsGoingToBeHit = MutableStateFlow(false)
    @Transient override val userIsGoingToBeHit: StateFlow<Boolean> = _userIsGoingToBeHit.asStateFlow()

    @Transient private val _userHasBeenHit = MutableStateFlow(false)
    @Transient override val userHasBeenHit: StateFlow<Boolean> = _userHasBeenHit.asStateFlow()

    @Transient private val _userPositionRatio = MutableStateFlow(0.0)
    @Transient override val userPositionRatio: StateFlow<Double> = _userPositionRatio.asStateFlow()

    @Transient private val _timeBeforeHit = MutableStateFlow(INFINITE)
    @Transient override val timeBeforeHit: StateFlow<Duration> = _timeBeforeHit.asStateFlow()

    @Transient private val _hitDateTime = MutableStateFlow(DISTANT_FUTURE)
    @Transient override val hitDateTime: StateFlow<Instant> = _hitDateTime.asStateFlow()

    @Transient private val _userIsInArea = MutableStateFlow(false)
    @Transient override val userIsInArea: StateFlow<Boolean> = _userIsInArea.asStateFlow()

    // --

    @Transient private var observationJob: Job? = null

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
        return eventDateTime > now && eventDateTime <= now.plus(WAVE_SOON_DELAY)
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
    override fun getStartDateTime(): Instant =
        try {
            val localDateTime = LocalDateTime.parse("${date}T${startHour}:00")
            localDateTime.toInstant(getTZ())
        } catch (e: Exception) {
            Log.e(::getStartDateTime.name, "$id: Error parsing start date and time: $e")
            throw IllegalStateException("$id: Error parsing start date and time")
        }

    override suspend fun getTotalTime(): Duration {
        var waveDuration = wave.getWaveDuration()

        if (waveDuration == 0.seconds) // If GeoJson has not been yet loaded we do not have the polygons
            waveDuration = wave.getApproxDuration()

        return getWarmingDuration() + WAVE_WARN_BEFORE_HIT + waveDuration
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
    override suspend fun getEndDateTime(): Instant = getStartDateTime().plus(getTotalTime())

    // ------------------------------------------------------------------------

    /**
     * Retrieves the event's time zone offset in the form "UTC+x".
     *
     * This function calculates the current offset of the event's time zone from UTC
     * and returns it as a string in the format "UTC+x".
     *
     */
    override fun getLiteralTimezone(): String {
        val hoursOffset = getTZ().offsetAt(clock.now()).totalSeconds / 3600
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
            "${it.toLocalDateTime(getTZ()).day.toString().padStart(2, '0')}/${
                it.toLocalDateTime(getTZ()).month.number .toString().padStart(2, '0')
            }"
        }
    } catch (_: Exception) {
        "00/00"
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
    override suspend fun getLiteralTotalTime(): String = "${getTotalTime().inWholeMinutes} min"

    // -----------------------------------------------------------------------

    override fun getLiteralCountry(): String {
        return country
            ?.lowercase()
            ?.replace("_", " ") // Replace underscores with spaces
            ?.split(" ") // Split into words
            ?.joinToString(" ") { word -> // Join words with spaces
                word.replaceFirstChar { char -> char.uppercase() } // Capitalize each word
            }
            ?.replace("England", "United Kingdom") // FIXME: Ugly hack for London
            ?: ""
    }

    override fun getLiteralCommunity(): String {
        return community
            ?.lowercase()
            ?.replace("_", " ") // Replace underscores with spaces
            ?.split(" ") // Split into words
            ?.joinToString(" ") { word -> // Join words with spaces
                word.replaceFirstChar { char -> char.uppercase() } // Capitalize each word
            }
            ?: ""
    }

    // -----------------------------------------------------------------------

    /**
     * Starting date/time of the wave
     */
    override fun getWaveStartDateTime() : Instant = getStartDateTime() + getWarmingDuration() + WAVE_WARN_BEFORE_HIT

    /**
     * Duration of the warming phase
     */
    override fun getWarmingDuration(): Duration = warming.getWarmingDuration()

    /**
     * Determines if the current time is near the event start time.
     *
     * This function calculates the duration between the current time and the event start time.
     * It then checks if this duration is greater than the predefined observation delay.
     *
     * Is different than isSoon on the delay, isSoon is on date while isNearTime is on hours
     *
     */
    override fun isNearTime(): Boolean {
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
            try { block() } catch (_: Throwable) { "error" }

        return WaveNumbersLiterals(
            waveTimezone = safeCall { getLiteralTimezone() },
            waveSpeed = safeCall { wave.getLiteralSpeed() },
            waveStartTime = safeCall { getLiteralStartTime() },
            waveEndTime = safeCall { getLiteralEndTime() },
            waveTotalTime = safeCall { getLiteralTotalTime() }
        )
    }

    // ---------------------------

    /**
     * Starts observing the wave event if not already started.
     *
     * Launches a coroutine to initialize the last observed status and progression,
     * then calls `observeWave` to begin periodic checks.
     */
    override fun startObservation() {
        if (observationJob == null) {
            coroutineScopeProvider.launchDefault {
                Log.v("startObservation", "Starting observation for event $id")

                // Initialize state with current values
                _eventStatus.value = getStatus()

                try {
                    _progression.value = wave.getProgression()
                } catch (e: Throwable) {
                    Log.e(
                        tag = WWWEventWave::class.simpleName!!,
                        message = "Error initializing progression: $e"
                    )
                }

                // Start observation if event is running or about to start
                // Accepted as the application will not be let running for days (isSoon includes a delay)
                if (isRunning() || (isSoon() && isNearTime())) {
                    observationJob = createObservationFlow()
                        .flowOn(Dispatchers.IO)
                        .catch { e ->
                            Log.e("observeWave", "Error in observation flow: $e")
                        }
                        .onEach { (progressionValue, status) ->
                            updateStates(progressionValue, status)
                        }
                        .launchIn(coroutineScopeProvider.scopeDefault())
                }
            }
        }
    }

    override fun stopObservation() {
        coroutineScopeProvider.launchDefault {
            Log.v("stopObservation", "Stopping observation for event $id")
            try {
                observationJob?.cancelAndJoin()
            } catch (_: CancellationException) {
                // Expected exception during cancellation
            } catch (e: Exception) {
                Log.e("stopObservation", "Error stopping observation: $e")
            } finally {
                observationJob = null
            }
        }
    }

    /**
     * Creates a flow that periodically emits wave observations.
     */
    private fun createObservationFlow() = callbackFlow {
        try {
            while (!isDone()) {
                // Get current state and emit it
                val progression = wave.getProgression()
                val status = getStatus()
                val eventObservation = EventObservation(progression, status)
                send(eventObservation)

                // Wait for the next observation interval
                val observationDelay = getObservationInterval()
                clock.delay(observationDelay)
            }

            // Final emission when event is done
            send(EventObservation(100.0, Status.DONE))

        } catch (e: Exception) {
            Log.e("observationFlow", "Error in observation flow: $e")
        }

        // Clean up when flow is cancelled
        awaitClose()
    }

    /**
     * Updates all state flows based on the current event state.
     */
    private suspend fun updateStates(progression: Double, status: Status) {
        // Update the main state flows
        _progression.updateIfChanged(progression)
        _eventStatus.updateIfChanged(status)

        // Check for warming started
        var warmingInProgress = false
        if (warming.isUserWarmingStarted()) {
            warmingInProgress = true
        }

        // Check if user is about to be hit
        var userIsGoingToBeHit = false
        val timeBeforeHit = wave.timeBeforeUserHit() ?: INFINITE
        if (timeBeforeHit > ZERO && timeBeforeHit <= WAVE_WARN_BEFORE_HIT) {
            warmingInProgress = false
            userIsGoingToBeHit = true
        }

        // Check if user has been hit
        if (wave.hasUserBeenHitInCurrentPosition()) {
            warmingInProgress = false
            userIsGoingToBeHit = false
            _userHasBeenHit.updateIfChanged(true)
        }

        // Update additional state flows
        _userPositionRatio.updateIfChanged(wave.userPositionToWaveRatio() ?: 0.0)
        _timeBeforeHit.updateIfChanged(wave.timeBeforeUserHit() ?: INFINITE)
        _hitDateTime.updateIfChanged(wave.userHitDateTime() ?: DISTANT_FUTURE)

        // User in area
        val userPosition = wave.getUserPosition()
        if (userPosition != null) {
            _userIsInArea.updateIfChanged(area.isPositionWithin(userPosition))
        } else {
            _userIsInArea.updateIfChanged(false)
        }

        _isWarmingInProgress.updateIfChanged(warmingInProgress)
        _userIsGoingToBeHit.updateIfChanged(userIsGoingToBeHit)
    }

    /**
     * Calculates the observation interval for the wave event.
     *
     * This function determines the appropriate interval for observing the wave event based on the
     * current time, the event start time, and the time before the user is hit by the wave.
     *
     */
    private suspend fun getObservationInterval(): Duration {
        val now = clock.now()
        val eventStartTime = getStartDateTime()
        val timeBeforeEvent = eventStartTime - now
        val timeBeforeHit = wave.timeBeforeUserHit() ?: 1.days

        return when {
            timeBeforeEvent > 1.hours + 5.minutes -> 1.hours
            timeBeforeEvent > 5.minutes + 30.seconds -> 5.minutes
            timeBeforeEvent > 35.seconds -> 1.seconds
            isRunning() -> 500.milliseconds
            timeBeforeHit < 2.seconds -> 50.milliseconds // For sound accuracy
            else -> 1.minutes // Default case, more reasonable than 1 day
        }
    }

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


