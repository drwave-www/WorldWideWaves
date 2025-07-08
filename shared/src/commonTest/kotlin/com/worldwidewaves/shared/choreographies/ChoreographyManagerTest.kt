package com.worldwidewaves.shared.choreographies

import com.worldwidewaves.shared.WWWGlobals
import com.worldwidewaves.shared.events.utils.CoroutineScopeProvider
import com.worldwidewaves.shared.events.utils.IClock
import com.worldwidewaves.shared.utils.ImageResolver
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
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
import kotlin.time.Duration
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

    // Test choreography definition
    private val testDefinition = """
    {
        "warming_sequences": [
            {
                "frames": "sprites/warming1.png",
                "frame_width": 100,
                "frame_height": 100,
                "frame_count": 3,
                "timing": 500000000,
                "text": "Get Ready",
                "loop": true,
                "duration": 1500000000
            },
            {
                "frames": "sprites/warming2.png",
                "frame_width": 100,
                "frame_height": 100,
                "frame_count": 2,
                "timing": 1000000000,
                "text": "Almost There",
                "loop": true,
                "duration": 2000000000
            }
        ],
        "waiting_sequence": {
            "frames": "sprites/waiting.png",
            "frame_width": 200,
            "frame_height": 200,
            "frame_count": 4,
            "timing": 750000000,
            "text": "Wait For It",
            "loop": true,
            "duration": 3000000000
        },
        "hit_sequence": {
            "frames": "sprites/hit.png",
            "frame_width": 300,
            "frame_height": 300,
            "frame_count": 5,
            "timing": 200000000,
            "text": "Now!",
            "loop": false,
            "duration": 1000000000
        }
    }
    """.trimIndent()

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
    fun `test loadDefinition loads and parses choreography definition`() = runTest {
        // Create a spy on the manager to access private method
        val managerSpy = spyk(manager)

        // Mock the resource loading
        val loadDefinitionMethod = managerSpy::class.members.first { it.name == "loadDefinition" }
        coEvery { managerSpy.invoke(loadDefinitionMethod, WWWGlobals.FS_CHOREOGRAPHIES_CONF) } returns 
            Json { ignoreUnknownKeys = true; isLenient = true }.decodeFromString(testDefinition)

        // Call the method that uses loadDefinition
        val prepareMethod = managerSpy::class.members.first { it.name == "prepareChoreography" }
        managerSpy.invoke(prepareMethod, WWWGlobals.FS_CHOREOGRAPHIES_CONF)

        // Verify the definition was loaded
        coVerify { managerSpy.invoke(loadDefinitionMethod, WWWGlobals.FS_CHOREOGRAPHIES_CONF) }
    }

    @Test
    fun `test getCurrentWarmingSequence returns correct sequence based on elapsed time`() = runTest {
        // Create a manager with test definition directly injected
        val testManager = createManagerWithTestDefinition()

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
        // Create a manager with test definition directly injected
        val testManager = createManagerWithTestDefinition()

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
        // Create a manager with test definition directly injected
        val testManager = createManagerWithTestDefinition()

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
        // Create a manager with test definition directly injected
        val testManager = createManagerWithTestDefinition()
        
        val waitingSequence = testManager.getWaitingSequence()
        
        assertNotNull(waitingSequence)
        assertEquals("Wait For It", waitingSequence.text)
        assertEquals(200, waitingSequence.frameWidth)
        assertEquals(200, waitingSequence.frameHeight)
        assertEquals(4, waitingSequence.frameCount)
        assertEquals(0.75.seconds, waitingSequence.timing)
        assertTrue(waitingSequence.loop)
    }

    @Test
    fun `test getHitSequence returns configured hit sequence`() = runTest {
        // Create a manager with test definition directly injected
        val testManager = createManagerWithTestDefinition()
        
        val hitSequence = testManager.getHitSequence()
        
        assertNotNull(hitSequence)
        assertEquals("Now!", hitSequence.text)
        assertEquals(300, hitSequence.frameWidth)
        assertEquals(300, hitSequence.frameHeight)
        assertEquals(5, hitSequence.frameCount)
        assertEquals(0.2.seconds, hitSequence.timing)
        assertEquals(false, hitSequence.loop)
    }

    @Test
    fun `test getCurrentWarmingSequence with no warming sequences returns null`() = runTest {
        // Create a manager with empty definition
        val emptyDefinition = """
        {
            "warming_sequences": [],
            "waiting_sequence": null,
            "hit_sequence": null
        }
        """.trimIndent()
        
        val testManager = createManagerWithDefinition(emptyDefinition)
        
        val startTime = Instant.fromEpochMilliseconds(1000)
        every { clock.now() } returns Instant.fromEpochMilliseconds(2000)
        
        val sequence = testManager.getCurrentWarmingSequence(startTime)
        assertNull(sequence)
    }

    @Test
    fun `test getCurrentWarmingSequence with zero total duration returns first sequence`() = runTest {
        // Create a manager with zero-duration sequences
        val zeroDurationDefinition = """
        {
            "warming_sequences": [
                {
                    "frames": "sprites/warming1.png",
                    "frame_width": 100,
                    "frame_height": 100,
                    "frame_count": 1,
                    "timing": 0,
                    "text": "Zero Duration 1",
                    "duration": 0
                },
                {
                    "frames": "sprites/warming2.png",
                    "frame_width": 100,
                    "frame_height": 100,
                    "frame_count": 1,
                    "timing": 0,
                    "text": "Zero Duration 2",
                    "duration": 0
                }
            ]
        }
        """.trimIndent()
        
        val testManager = createManagerWithDefinition(zeroDurationDefinition)
        
        val startTime = Instant.fromEpochMilliseconds(1000)
        every { clock.now() } returns Instant.fromEpochMilliseconds(2000)
        
        val sequence = testManager.getCurrentWarmingSequence(startTime)
        assertNotNull(sequence)
        assertEquals("Zero Duration 1", sequence.text)
    }

    @Test
    fun `test error handling when loading invalid definition`() = runTest {
        // Mock resource reading to return invalid JSON
        val bytesSlot = slot<String>()
        val mockResourceReader = mockk<Any>()
        
        coEvery { mockResourceReader.readBytes(capture(bytesSlot)) } answers {
            "invalid json".encodeToByteArray()
        }
        
        // Create a manager that will fail to load the definition
        val testManager = ChoreographyManager<TestImage>(coroutineScopeProvider)
        
        // Should not throw exception, but return null for sequences
        val startTime = Instant.fromEpochMilliseconds(1000)
        every { clock.now() } returns Instant.fromEpochMilliseconds(2000)
        
        val warmingSequence = testManager.getCurrentWarmingSequence(startTime)
        val waitingSequence = testManager.getWaitingSequence()
        val hitSequence = testManager.getHitSequence()
        
        assertNull(warmingSequence)
        assertNull(waitingSequence)
        assertNull(hitSequence)
    }

    @Test
    fun `test with very large time differences`() = runTest {
        // Create a manager with test definition directly injected
        val testManager = createManagerWithTestDefinition()

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
        // Create a manager with test definition directly injected
        val testManager = createManagerWithTestDefinition()

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
        // Create a manager with test definition directly injected
        val testManager = createManagerWithTestDefinition()

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
        // Create a manager with test definition directly injected
        val testManager = createManagerWithTestDefinition()

        val startTime = Instant.fromEpochMilliseconds(1000)
        
        // 1.5 seconds + 1 nanosecond (just into second sequence)
        val timeNanos = 1500.milliseconds.inWholeNanoseconds + 1
        every { clock.now() } returns startTime + timeNanos.nanoseconds
        
        val sequence = testManager.getCurrentWarmingSequence(startTime)
        assertNotNull(sequence)
        assertEquals("Almost There", sequence.text)
    }

    // Helper method to create a manager with the test definition
    private fun createManagerWithTestDefinition(): ChoreographyManager<TestImage> {
        return createManagerWithDefinition(testDefinition)
    }
    
    // Helper method to create a manager with a custom definition
    private fun createManagerWithDefinition(definitionJson: String): ChoreographyManager<TestImage> {
        // Create a new manager
        val testManager = ChoreographyManager<TestImage>(coroutineScopeProvider)
        
        // Use reflection to set the private fields
        val definitionField = testManager::class.java.getDeclaredField("definition")
        definitionField.isAccessible = true
        val definition = Json { ignoreUnknownKeys = true; isLenient = true }
            .decodeFromString<ChoreographyDefinition>(definitionJson)
        definitionField.set(testManager, definition)
        
        // Prepare the resolved sequences
        val prepareMethod = testManager::class.java.getDeclaredMethod("prepareChoreography", String::class.java)
        prepareMethod.isAccessible = true
        prepareMethod.invoke(testManager, WWWGlobals.FS_CHOREOGRAPHIES_CONF)
        
        return testManager
    }
}
