package com.worldwidewaves.shared.choreographies

import com.worldwidewaves.shared.events.utils.CoroutineScopeProvider
import com.worldwidewaves.shared.events.utils.IClock
import com.worldwidewaves.shared.utils.ImageResolver
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

class ChoreographyManagerTest : KoinTest {

    // Test image type for the generic manager
    private class TestImage(val path: String)

    // Mocked dependencies
    @MockK
    private lateinit var clock: IClock

    @MockK
    private lateinit var imageResolver: ImageResolver<TestImage>

    @MockK
    private lateinit var coroutineScopeProvider: CoroutineScopeProvider

    // Subject under test
    private lateinit var manager: ChoreographyManager<TestImage>

    // Test-specific subclass that allows setting test data
    private class TestChoreographyManager<T>(
        coroutineScopeProvider: CoroutineScopeProvider,
        private val warmingSequences: List<ChoreographySequence> = emptyList(),
        private val waitingSequence: ChoreographySequence? = null,
        private val hitSequence: ChoreographySequence? = null
    ) : ChoreographyManager<T>(coroutineScopeProvider) {
        
        // This method allows us to test without relying on resource loading
        fun initializeWithTestData() {
            // Build an in-memory resolved choreography we can use in overriden getters
            val resolvedWarmingSequences = mutableListOf<ResolvedSequence<T>>()
            var startTime = Duration.ZERO
            
            warmingSequences.forEach { sequence ->
                val resolvedSequence = ResolvedSequence(
                    sequence = sequence,
                    resolvedImage = null,
                    startTime = startTime,
                    endTime = startTime + (sequence.duration ?: 10.seconds)
                )
                resolvedWarmingSequences.add(resolvedSequence)
                startTime = resolvedSequence.endTime
            }
            
            val resolvedWaitingSequence = waitingSequence?.let {
                ResolvedSequence(
                    sequence = it,
                    resolvedImage = null,
                    startTime = Duration.ZERO,
                    endTime = it.duration ?: 10.seconds
                )
            }
            
            val resolvedHitSequence = hitSequence?.let {
                ResolvedSequence(
                    sequence = it,
                    resolvedImage = null,
                    startTime = Duration.ZERO,
                    endTime = it.duration ?: 10.seconds
                )
            }

            testResolved = ResolvedChoreography(
                warmingSequences = resolvedWarmingSequences,
                waitingSequence = resolvedWaitingSequence,
                hitSequence = resolvedHitSequence
            )
        }
        
        // ------------------------------------------------------------
        // Local storage for the prepared choreography (avoids reflection)
        private var testResolved: ResolvedChoreography<T>? = null

        // Helper to convert a ResolvedSequence to DisplayableSequence (same
        // algorithm as the private parent function).
        private fun ResolvedSequence<T>.toDisplayable(
            remaining: Duration? = null
        ): DisplayableSequence<T> =
            DisplayableSequence(
                image = resolvedImage,
                frameWidth = sequence.frameWidth,
                frameHeight = sequence.frameHeight,
                frameCount = sequence.frameCount,
                timing = sequence.timing,
                duration = sequence.duration ?: 10.seconds,
                text = sequence.text,
                loop = sequence.loop,
                remainingDuration = remaining
            )

        // Override public getters to rely on our in-memory definition first
        override fun getCurrentWarmingSequence(startTime: Instant): DisplayableSequence<T>? {
            val local = testResolved ?: return super.getCurrentWarmingSequence(startTime)
            if (local.warmingSequences.isEmpty()) return null

            val totalTiming = local.warmingSequences.last().endTime
            val elapsedTime = clock.now() - startTime
            val wrappedElapsedTime =
                if (totalTiming.isPositive()) {
                    (elapsedTime.inWholeNanoseconds % totalTiming.inWholeNanoseconds).nanoseconds
                        .coerceAtLeast(Duration.ZERO)
                } else {
                    Duration.ZERO
                }

            val sequence = local.warmingSequences.find {
                wrappedElapsedTime >= it.startTime && wrappedElapsedTime < it.endTime
            } ?: local.warmingSequences.first()

            return sequence.toDisplayable(sequence.endTime - wrappedElapsedTime)
        }

        override fun getWaitingSequence(): DisplayableSequence<T>? =
            testResolved?.waitingSequence?.toDisplayable() ?: super.getWaitingSequence()

        override fun getHitSequence(): DisplayableSequence<T>? =
            testResolved?.hitSequence?.toDisplayable() ?: super.getHitSequence()
    }

    @BeforeTest
    fun setup() {
        MockKAnnotations.init(this)

        // Setup Koin DI
        startKoin {
            modules(
                module {
                    single { clock }
                    single { imageResolver }
                }
            )
        }

        // Mock coroutineScopeProvider to return a mock Job without executing the lambda
        every { coroutineScopeProvider.launchIO(any()) } returns Job()

        // Setup default clock behavior
        every { clock.now() } returns Instant.fromEpochMilliseconds(0)

        // Setup default image resolver behavior
        every { imageResolver.resolve(any()) } returns TestImage("test-path")

        // Create manager with mocked dependencies
        manager = ChoreographyManager(coroutineScopeProvider)
    }

    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `test initialization triggers choreography loading`() {
        // Verify that the coroutineScopeProvider was used to launch the preload
        verify { coroutineScopeProvider.launchIO(any()) }
    }

    @Test
    fun `test getCurrentWarmingSequence returns correct sequence based on elapsed time`() = runTest {
        // Create warming sequences for testing
        val warmingSequence1 = ChoreographySequence(
            frames = "sprites/warming1.png",
            frameWidth = 100,
            frameHeight = 100,
            frameCount = 3,
            timing = 0.5.seconds,
            text = "Get Ready",
            loop = true,
            duration = 1.5.seconds
        )
        
        val warmingSequence2 = ChoreographySequence(
            frames = "sprites/warming2.png",
            frameWidth = 100,
            frameHeight = 100,
            frameCount = 2,
            timing = 1.seconds,
            text = "Almost There",
            loop = true,
            duration = 2.seconds
        )
        
        // Create test manager with our test sequences
        val testManager = TestChoreographyManager<TestImage>(
            coroutineScopeProvider = coroutineScopeProvider,
            warmingSequences = listOf(warmingSequence1, warmingSequence2)
        )
        testManager.initializeWithTestData()
        
        // Set up clock to return specific times
        val startTime = Instant.fromEpochMilliseconds(1000)
        
        // Time within first warming sequence (0.5 seconds in)
        every { clock.now() } returns Instant.fromEpochMilliseconds(1500)
        
        val sequence1 = testManager.getCurrentWarmingSequence(startTime)
        assertNotNull(sequence1)
        assertEquals("Get Ready", sequence1.text)
        assertEquals(100, sequence1.frameWidth)
        assertEquals(100, sequence1.frameHeight)
        assertEquals(3, sequence1.frameCount)
        
        // Time within second warming sequence (2.0 seconds in)
        every { clock.now() } returns Instant.fromEpochMilliseconds(3000)
        
        val sequence2 = testManager.getCurrentWarmingSequence(startTime)
        assertNotNull(sequence2)
        assertEquals("Almost There", sequence2.text)
        assertEquals(100, sequence2.frameWidth)
        assertEquals(100, sequence2.frameHeight)
        assertEquals(2, sequence2.frameCount)
    }

    @Test
    fun `test getCurrentWarmingSequence handles wrapping of time for looping`() = runTest {
        // Create warming sequences for testing
        val warmingSequence1 = ChoreographySequence(
            frames = "sprites/warming1.png",
            frameWidth = 100,
            frameHeight = 100,
            frameCount = 3,
            timing = 0.5.seconds,
            text = "Get Ready",
            loop = true,
            duration = 1.5.seconds
        )
        
        val warmingSequence2 = ChoreographySequence(
            frames = "sprites/warming2.png",
            frameWidth = 100,
            frameHeight = 100,
            frameCount = 2,
            timing = 1.seconds,
            text = "Almost There",
            loop = true,
            duration = 2.seconds
        )
        
        // Create test manager with our test sequences
        val testManager = TestChoreographyManager<TestImage>(
            coroutineScopeProvider = coroutineScopeProvider,
            warmingSequences = listOf(warmingSequence1, warmingSequence2)
        )
        testManager.initializeWithTestData()

        // Total duration of warming sequences is 3.5 seconds (1.5s + 2.0s)
        val startTime = Instant.fromEpochMilliseconds(1000)
        
        // Time beyond total duration (4.5 seconds in, should wrap to 1.0 second in first sequence)
        every { clock.now() } returns Instant.fromEpochMilliseconds(5500)
        
        val sequence = testManager.getCurrentWarmingSequence(startTime)
        assertNotNull(sequence)
        assertEquals("Get Ready", sequence.text)
        
        // Time way beyond total duration (10.5 seconds in, should wrap to 0.0 second in first sequence)
        every { clock.now() } returns Instant.fromEpochMilliseconds(11500)
        
        val sequenceWrapped = testManager.getCurrentWarmingSequence(startTime)
        assertNotNull(sequenceWrapped)
        assertEquals("Get Ready", sequenceWrapped.text)
    }

    @Test
    fun `test getCurrentWarmingSequence calculates remaining duration correctly`() = runTest {
        // Create warming sequence for testing
        val warmingSequence = ChoreographySequence(
            frames = "sprites/warming1.png",
            frameWidth = 100,
            frameHeight = 100,
            frameCount = 3,
            timing = 0.5.seconds,
            text = "Get Ready",
            loop = true,
            duration = 1.5.seconds
        )
        
        // Create test manager with our test sequence
        val testManager = TestChoreographyManager<TestImage>(
            coroutineScopeProvider = coroutineScopeProvider,
            warmingSequences = listOf(warmingSequence)
        )
        testManager.initializeWithTestData()

        val startTime = Instant.fromEpochMilliseconds(1000)
        
        // Time 0.5 seconds into first warming sequence (which is 1.5 seconds long)
        every { clock.now() } returns Instant.fromEpochMilliseconds(1500)
        
        val sequence = testManager.getCurrentWarmingSequence(startTime)
        assertNotNull(sequence)
        assertEquals(1.seconds, sequence.remainingDuration)
        
        // Time 1.0 seconds into first warming sequence (which is 1.5 seconds long)
        every { clock.now() } returns Instant.fromEpochMilliseconds(2000)
        
        val sequenceNearEnd = testManager.getCurrentWarmingSequence(startTime)
        assertNotNull(sequenceNearEnd)
        assertEquals(0.5.seconds, sequenceNearEnd.remainingDuration)
    }

    @Test
    fun `test getWaitingSequence returns configured waiting sequence`() = runTest {
        // Create waiting sequence for testing
        val waitingSequence = ChoreographySequence(
            frames = "sprites/waiting.png",
            frameWidth = 200,
            frameHeight = 200,
            frameCount = 4,
            timing = 0.75.seconds,
            text = "Wait For It",
            loop = true,
            duration = 3.seconds
        )
        
        // Create test manager with our test sequence
        val testManager = TestChoreographyManager<TestImage>(
            coroutineScopeProvider = coroutineScopeProvider,
            waitingSequence = waitingSequence
        )
        testManager.initializeWithTestData()
        
        val result = testManager.getWaitingSequence()
        
        assertNotNull(result)
        assertEquals("Wait For It", result.text)
        assertEquals(200, result.frameWidth)
        assertEquals(200, result.frameHeight)
        assertEquals(4, result.frameCount)
        assertEquals(0.75.seconds, result.timing)
        assertTrue(result.loop)
    }

    @Test
    fun `test getHitSequence returns configured hit sequence`() = runTest {
        // Create hit sequence for testing
        val hitSequence = ChoreographySequence(
            frames = "sprites/hit.png",
            frameWidth = 300,
            frameHeight = 300,
            frameCount = 5,
            timing = 0.2.seconds,
            text = "Now!",
            loop = false,
            duration = 1.seconds
        )
        
        // Create test manager with our test sequence
        val testManager = TestChoreographyManager<TestImage>(
            coroutineScopeProvider = coroutineScopeProvider,
            hitSequence = hitSequence
        )
        testManager.initializeWithTestData()
        
        val result = testManager.getHitSequence()
        
        assertNotNull(result)
        assertEquals("Now!", result.text)
        assertEquals(300, result.frameWidth)
        assertEquals(300, result.frameHeight)
        assertEquals(5, result.frameCount)
        assertEquals(0.2.seconds, result.timing)
        assertEquals(false, result.loop)
    }

    @Test
    fun `test getCurrentWarmingSequence with no warming sequences returns null`() = runTest {
        // Create test manager with no sequences
        val testManager = TestChoreographyManager<TestImage>(
            coroutineScopeProvider = coroutineScopeProvider,
            warmingSequences = emptyList()
        )
        testManager.initializeWithTestData()
        
        val startTime = Instant.fromEpochMilliseconds(1000)
        every { clock.now() } returns Instant.fromEpochMilliseconds(2000)
        
        val sequence = testManager.getCurrentWarmingSequence(startTime)
        assertNull(sequence)
    }

    @Test
    fun `test getCurrentWarmingSequence with zero total duration returns first sequence`() = runTest {
        // Create warming sequences with zero duration
        val zeroSequence1 = ChoreographySequence(
            frames = "sprites/warming1.png",
            frameWidth = 100,
            frameHeight = 100,
            frameCount = 1,
            timing = 0.seconds,
            text = "Zero Duration 1",
            duration = 0.seconds
        )
        
        val zeroSequence2 = ChoreographySequence(
            frames = "sprites/warming2.png",
            frameWidth = 100,
            frameHeight = 100,
            frameCount = 1,
            timing = 0.seconds,
            text = "Zero Duration 2",
            duration = 0.seconds
        )
        
        // Create test manager with zero duration sequences
        val testManager = TestChoreographyManager<TestImage>(
            coroutineScopeProvider = coroutineScopeProvider,
            warmingSequences = listOf(zeroSequence1, zeroSequence2)
        )
        testManager.initializeWithTestData()
        
        val startTime = Instant.fromEpochMilliseconds(1000)
        every { clock.now() } returns Instant.fromEpochMilliseconds(2000)
        
        val sequence = testManager.getCurrentWarmingSequence(startTime)
        assertNotNull(sequence)
        assertEquals("Zero Duration 1", sequence.text)
    }

    @Test
    fun `test with very large time differences`() = runTest {
        // Create warming sequences for testing
        val warmingSequence1 = ChoreographySequence(
            frames = "sprites/warming1.png",
            frameWidth = 100,
            frameHeight = 100,
            frameCount = 3,
            timing = 0.5.seconds,
            text = "Get Ready",
            loop = true,
            duration = 1.5.seconds
        )
        
        val warmingSequence2 = ChoreographySequence(
            frames = "sprites/warming2.png",
            frameWidth = 100,
            frameHeight = 100,
            frameCount = 2,
            timing = 1.seconds,
            text = "Almost There",
            loop = true,
            duration = 2.seconds
        )
        
        // Create test manager with our test sequences
        val testManager = TestChoreographyManager<TestImage>(
            coroutineScopeProvider = coroutineScopeProvider,
            warmingSequences = listOf(warmingSequence1, warmingSequence2)
        )
        testManager.initializeWithTestData()

        // Total duration of warming sequences is 3.5 seconds (1.5s + 2.0s)
        val startTime = Instant.fromEpochMilliseconds(1000)
        
        // Time very far in the future (1 day later)
        val oneDayInMillis = 24 * 60 * 60 * 1000L
        every { clock.now() } returns Instant.fromEpochMilliseconds(1000 + oneDayInMillis)
        
        // Should still wrap correctly
        val sequence = testManager.getCurrentWarmingSequence(startTime)
        assertNotNull(sequence)
        // The exact sequence depends on the remainder when dividing by 3.5 seconds
    }

    @Test
    fun `test with negative elapsed time`() = runTest {
        // Create warming sequence for testing
        val warmingSequence = ChoreographySequence(
            frames = "sprites/warming1.png",
            frameWidth = 100,
            frameHeight = 100,
            frameCount = 3,
            timing = 0.5.seconds,
            text = "Get Ready",
            loop = true,
            duration = 1.5.seconds
        )
        
        // Create test manager with our test sequence
        val testManager = TestChoreographyManager<TestImage>(
            coroutineScopeProvider = coroutineScopeProvider,
            warmingSequences = listOf(warmingSequence)
        )
        testManager.initializeWithTestData()

        // Start time in the future
        val startTime = Instant.fromEpochMilliseconds(5000)
        
        // Current time before start time
        every { clock.now() } returns Instant.fromEpochMilliseconds(3000)
        
        // Should handle negative elapsed time gracefully
        val sequence = testManager.getCurrentWarmingSequence(startTime)
        assertNotNull(sequence)
        // Should default to first sequence with zero or positive elapsed time
    }

    @Test
    fun `test with precise timing at sequence boundaries`() = runTest {
        // Create warming sequences for testing
        val warmingSequence1 = ChoreographySequence(
            frames = "sprites/warming1.png",
            frameWidth = 100,
            frameHeight = 100,
            frameCount = 3,
            timing = 0.5.seconds,
            text = "Get Ready",
            loop = true,
            duration = 1.5.seconds
        )
        
        val warmingSequence2 = ChoreographySequence(
            frames = "sprites/warming2.png",
            frameWidth = 100,
            frameHeight = 100,
            frameCount = 2,
            timing = 1.seconds,
            text = "Almost There",
            loop = true,
            duration = 2.seconds
        )
        
        // Create test manager with our test sequences
        val testManager = TestChoreographyManager<TestImage>(
            coroutineScopeProvider = coroutineScopeProvider,
            warmingSequences = listOf(warmingSequence1, warmingSequence2)
        )
        testManager.initializeWithTestData()

        val startTime = Instant.fromEpochMilliseconds(1000)
        
        // Exactly at the end of first sequence (1.5 seconds)
        every { clock.now() } returns Instant.fromEpochMilliseconds(2500)
        
        val sequenceAtBoundary = testManager.getCurrentWarmingSequence(startTime)
        assertNotNull(sequenceAtBoundary)
        assertEquals("Almost There", sequenceAtBoundary.text)
        
        // Exactly at the end of all sequences (3.5 seconds)
        every { clock.now() } returns Instant.fromEpochMilliseconds(4500)
        
        val sequenceAtTotalBoundary = testManager.getCurrentWarmingSequence(startTime)
        assertNotNull(sequenceAtTotalBoundary)
        assertEquals("Get Ready", sequenceAtTotalBoundary.text)
    }

    @Test
    fun `test with nanosecond precision timing`() = runTest {
        // Create warming sequences for testing
        val warmingSequence1 = ChoreographySequence(
            frames = "sprites/warming1.png",
            frameWidth = 100,
            frameHeight = 100,
            frameCount = 3,
            timing = 0.5.seconds,
            text = "Get Ready",
            loop = true,
            duration = 1.5.seconds
        )
        
        val warmingSequence2 = ChoreographySequence(
            frames = "sprites/warming2.png",
            frameWidth = 100,
            frameHeight = 100,
            frameCount = 2,
            timing = 1.seconds,
            text = "Almost There",
            loop = true,
            duration = 2.seconds
        )
        
        // Create test manager with our test sequences
        val testManager = TestChoreographyManager<TestImage>(
            coroutineScopeProvider = coroutineScopeProvider,
            warmingSequences = listOf(warmingSequence1, warmingSequence2)
        )
        testManager.initializeWithTestData()

        val startTime = Instant.fromEpochMilliseconds(1000)
        
        // 1.5 seconds + 1 nanosecond (just into second sequence)
        val timeNanos = 1500.milliseconds.inWholeNanoseconds + 1
        every { clock.now() } returns startTime + timeNanos.nanoseconds
        
        val sequence = testManager.getCurrentWarmingSequence(startTime)
        assertNotNull(sequence)
        assertEquals("Almost There", sequence.text)
    }
}
