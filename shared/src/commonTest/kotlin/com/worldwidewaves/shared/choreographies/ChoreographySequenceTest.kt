package com.worldwidewaves.shared.choreographies

import com.worldwidewaves.shared.utils.ImageResolver
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class ChoreographySequenceTest {

    // Test fixture for image resolver
    private class TestImage(val path: String)
    
    // Mock resolver that returns TestImage objects
    private fun createMockResolver(): ImageResolver<TestImage> {
        return mockk<ImageResolver<TestImage>>().also { resolver ->
            every { resolver.resolve(any()) } answers { 
                TestImage(firstArg())
            }
        }
    }

    @Test
    fun `test basic initialization with valid parameters`() {
        val sequence = ChoreographySequence(
            frames = "sprites/wave_animation.png",
            frameWidth = 450,
            frameHeight = 900,
            frameCount = 4,
            timing = 0.5.seconds,
            text = "Wave!",
            loop = true,
            duration = 10.seconds
        )
        
        assertEquals("sprites/wave_animation.png", sequence.frames)
        assertEquals(450, sequence.frameWidth)
        assertEquals(900, sequence.frameHeight)
        assertEquals(4, sequence.frameCount)
        assertEquals(0.5.seconds, sequence.timing)
        assertEquals("Wave!", sequence.text)
        assertTrue(sequence.loop)
        assertEquals(10.seconds, sequence.duration)
    }
    
    @Test
    fun `test initialization with minimum required parameters`() {
        val sequence = ChoreographySequence(
            frames = "sprites/minimal.png",
            frameWidth = 100,
            frameHeight = 200,
            timing = 1.seconds
        )
        
        assertEquals("sprites/minimal.png", sequence.frames)
        assertEquals(100, sequence.frameWidth)
        assertEquals(200, sequence.frameHeight)
        assertEquals(1, sequence.frameCount) // Default value
        assertEquals(1.seconds, sequence.timing)
        assertEquals("", sequence.text) // Default empty text
        assertTrue(sequence.loop) // Default to true
        assertEquals(10.seconds, sequence.duration) // Default duration
    }
    
    @Test
    fun `test validation fails with invalid frame count`() {
        assertFailsWith<IllegalArgumentException> {
            ChoreographySequence(
                frames = "sprites/invalid.png",
                frameWidth = 100,
                frameHeight = 100,
                frameCount = 0, // Invalid: must be > 0
                timing = 1.seconds
            )
        }
        
        assertFailsWith<IllegalArgumentException> {
            ChoreographySequence(
                frames = "sprites/invalid.png",
                frameWidth = 100,
                frameHeight = 100,
                frameCount = -1, // Invalid: must be > 0
                timing = 1.seconds
            )
        }
    }
    
    @Test
    fun `test validation fails with invalid frame dimensions`() {
        assertFailsWith<IllegalArgumentException> {
            ChoreographySequence(
                frames = "sprites/invalid.png",
                frameWidth = 0, // Invalid: must be > 0
                frameHeight = 100,
                timing = 1.seconds
            )
        }
        
        assertFailsWith<IllegalArgumentException> {
            ChoreographySequence(
                frames = "sprites/invalid.png",
                frameWidth = 100,
                frameHeight = 0, // Invalid: must be > 0
                timing = 1.seconds
            )
        }
        
        assertFailsWith<IllegalArgumentException> {
            ChoreographySequence(
                frames = "sprites/invalid.png",
                frameWidth = -10, // Invalid: must be > 0
                frameHeight = -20, // Invalid: must be > 0
                timing = 1.seconds
            )
        }
    }
    
    @Test
    fun `test resolveImageResources with mock resolver`() {
        val mockResolver = createMockResolver()
        
        val sequence = ChoreographySequence(
            frames = "sprites/test.png",
            frameWidth = 100,
            frameHeight = 100,
            frameCount = 3,
            timing = 1.seconds
        )
        
        val resolvedImages = sequence.resolveImageResources(mockResolver)
        
        assertEquals(3, resolvedImages.size, "Should resolve 3 frames")
        
        // Verify all frames are resolved with the same path
        resolvedImages.forEach { image ->
            assertEquals("sprites/test.png", image.path)
        }
        
        // Verify the resolver was called the expected number of times
        verify(exactly = 3) { mockResolver.resolve("sprites/test.png") }
    }
    
    @Test
    fun `test resolveImageResources with single frame`() {
        val mockResolver = createMockResolver()
        
        val sequence = ChoreographySequence(
            frames = "sprites/single.png",
            frameWidth = 100,
            frameHeight = 100,
            frameCount = 1,
            timing = 1.seconds
        )
        
        val resolvedImages = sequence.resolveImageResources(mockResolver)
        
        assertEquals(1, resolvedImages.size, "Should resolve a single frame")
        assertEquals("sprites/single.png", resolvedImages[0].path)
        
        // Verify the resolver was called exactly once
        verify(exactly = 1) { mockResolver.resolve("sprites/single.png") }
    }
    
    @Test
    fun `test resolveImageResources with resolver returning null`() {
        val nullResolver = mockk<ImageResolver<TestImage>>()
        every { nullResolver.resolve(any()) } returns null
        
        val sequence = ChoreographySequence(
            frames = "sprites/missing.png",
            frameWidth = 100,
            frameHeight = 100,
            frameCount = 3,
            timing = 1.seconds
        )
        
        val resolvedImages = sequence.resolveImageResources(nullResolver)
        
        assertTrue(resolvedImages.isEmpty(), "Should return empty list when resolver returns null")
        
        // Verify the resolver was called
        verify(exactly = 3) { nullResolver.resolve("sprites/missing.png") }
    }
    
    @Test
    fun `test serialization and deserialization`() {
        val original = ChoreographySequence(
            frames = "sprites/serialize_test.png",
            frameWidth = 200,
            frameHeight = 300,
            frameCount = 5,
            timing = 0.75.seconds,
            text = "Serialization Test",
            loop = false,
            duration = 5.seconds
        )
        
        val json = Json { 
            ignoreUnknownKeys = true 
            isLenient = true
        }
        
        // Serialize to JSON string
        val jsonString = json.encodeToString(original)
        
        // Deserialize back to object
        val deserialized = json.decodeFromString<ChoreographySequence>(jsonString)
        
        // Verify all properties are preserved
        assertEquals(original.frames, deserialized.frames)
        assertEquals(original.frameWidth, deserialized.frameWidth)
        assertEquals(original.frameHeight, deserialized.frameHeight)
        assertEquals(original.frameCount, deserialized.frameCount)
        assertEquals(original.timing, deserialized.timing)
        assertEquals(original.text, deserialized.text)
        assertEquals(original.loop, deserialized.loop)
        assertEquals(original.duration, deserialized.duration)
    }
    
    @Test
    fun `test choreography definition serialization and deserialization`() {
        val warmingSequence = ChoreographySequence(
            frames = "sprites/warming.png",
            frameWidth = 100,
            frameHeight = 100,
            frameCount = 3,
            timing = 0.5.seconds,
            text = "Get ready!",
            duration = 3.seconds
        )
        
        val waitingSequence = ChoreographySequence(
            frames = "sprites/waiting.png",
            frameWidth = 100,
            frameHeight = 100,
            frameCount = 2,
            timing = 1.seconds,
            text = "Wait...",
            duration = 2.seconds
        )
        
        val hitSequence = ChoreographySequence(
            frames = "sprites/hit.png",
            frameWidth = 100,
            frameHeight = 100,
            frameCount = 4,
            timing = 0.25.seconds,
            text = "Now!",
            loop = false,
            duration = 1.seconds
        )
        
        val definition = ChoreographyDefinition(
            warmingSequences = listOf(warmingSequence),
            waitingSequence = waitingSequence,
            hitSequence = hitSequence
        )
        
        val json = Json { 
            ignoreUnknownKeys = true 
            isLenient = true
        }
        
        // Serialize to JSON string
        val jsonString = json.encodeToString(definition)
        
        // Deserialize back to object
        val deserialized = json.decodeFromString<ChoreographyDefinition>(jsonString)
        
        // Verify all properties are preserved
        assertEquals(1, deserialized.warmingSequences.size)
        assertEquals(warmingSequence.frames, deserialized.warmingSequences[0].frames)
        assertEquals(warmingSequence.text, deserialized.warmingSequences[0].text)
        
        assertNotNull(deserialized.waitingSequence)
        assertEquals(waitingSequence.frames, deserialized.waitingSequence?.frames)
        assertEquals(waitingSequence.text, deserialized.waitingSequence?.text)
        
        assertNotNull(deserialized.hitSequence)
        assertEquals(hitSequence.frames, deserialized.hitSequence?.frames)
        assertEquals(hitSequence.text, deserialized.hitSequence?.text)
        assertFalse(deserialized.hitSequence?.loop ?: true)
    }
    
    @Test
    fun `test different timing configurations`() {
        // Very short timing
        val fastSequence = ChoreographySequence(
            frames = "sprites/fast.png",
            frameWidth = 100,
            frameHeight = 100,
            frameCount = 10,
            timing = 100.milliseconds,
            duration = 1.seconds
        )
        assertEquals(100.milliseconds, fastSequence.timing)
        
        // Very long timing
        val slowSequence = ChoreographySequence(
            frames = "sprites/slow.png",
            frameWidth = 100,
            frameHeight = 100,
            frameCount = 2,
            timing = 10.seconds,
            duration = 20.seconds
        )
        assertEquals(10.seconds, slowSequence.timing)
        
        // Zero timing (edge case)
        val zeroTimingSequence = ChoreographySequence(
            frames = "sprites/zero.png",
            frameWidth = 100,
            frameHeight = 100,
            frameCount = 1,
            timing = 0.milliseconds,
            duration = 1.seconds
        )
        assertEquals(0.milliseconds, zeroTimingSequence.timing)
    }
    
    @Test
    fun `test loop behavior configuration`() {
        // Looping sequence (default)
        val loopingSequence = ChoreographySequence(
            frames = "sprites/looping.png",
            frameWidth = 100,
            frameHeight = 100,
            timing = 1.seconds
        )
        assertTrue(loopingSequence.loop)
        
        // Non-looping sequence
        val nonLoopingSequence = ChoreographySequence(
            frames = "sprites/non_looping.png",
            frameWidth = 100,
            frameHeight = 100,
            timing = 1.seconds,
            loop = false
        )
        assertFalse(nonLoopingSequence.loop)
    }
    
    @Test
    fun `test duration configurations`() {
        // Default duration
        val defaultDurationSequence = ChoreographySequence(
            frames = "sprites/default_duration.png",
            frameWidth = 100,
            frameHeight = 100,
            timing = 1.seconds
        )
        assertEquals(10.seconds, defaultDurationSequence.duration)
        
        // Custom duration
        val customDurationSequence = ChoreographySequence(
            frames = "sprites/custom_duration.png",
            frameWidth = 100,
            frameHeight = 100,
            timing = 1.seconds,
            duration = 5.seconds
        )
        assertEquals(5.seconds, customDurationSequence.duration)
        
        // Null duration
        val nullDurationSequence = ChoreographySequence(
            frames = "sprites/null_duration.png",
            frameWidth = 100,
            frameHeight = 100,
            timing = 1.seconds,
            duration = null
        )
        assertEquals(null, nullDurationSequence.duration)
    }
    
    @Test
    fun `test deserialization from JSON string`() {
        val jsonString = """
        {
            "frames": "sprites/from_json.png",
            "frame_width": 300,
            "frame_height": 400,
            "frame_count": 6,
            "timing": "PT0.5S",
            "text": "Loaded from JSON",
            "loop": false,
            "duration": "PT3S"
        }
        """.trimIndent()
        
        val json = Json { 
            ignoreUnknownKeys = true 
            isLenient = true
        }
        
        val sequence = json.decodeFromString<ChoreographySequence>(jsonString)
        
        assertEquals("sprites/from_json.png", sequence.frames)
        assertEquals(300, sequence.frameWidth)
        assertEquals(400, sequence.frameHeight)
        assertEquals(6, sequence.frameCount)
        assertEquals(0.5.seconds, sequence.timing) // 500,000,000 nanoseconds = 0.5 seconds
        assertEquals("Loaded from JSON", sequence.text)
        assertFalse(sequence.loop)
        assertEquals(3.seconds, sequence.duration) // 3,000,000,000 nanoseconds = 3 seconds
    }
    
    @Test
    fun `test deserialization with missing optional fields`() {
        val jsonString = """
        {
            "frames": "sprites/minimal_json.png",
            "frame_width": 200,
            "frame_height": 200,
            "timing": "PT1S"
        }
        """.trimIndent()
        
        val json = Json { 
            ignoreUnknownKeys = true 
            isLenient = true
        }
        
        val sequence = json.decodeFromString<ChoreographySequence>(jsonString)
        
        assertEquals("sprites/minimal_json.png", sequence.frames)
        assertEquals(200, sequence.frameWidth)
        assertEquals(200, sequence.frameHeight)
        assertEquals(1, sequence.frameCount) // Default value
        assertEquals(1.seconds, sequence.timing)
        assertEquals("", sequence.text) // Default value
        assertTrue(sequence.loop) // Default value
        assertEquals(10.seconds, sequence.duration) // Default value
    }
    
    @Test
    fun `test choreography definition with empty sequences`() {
        val emptyDefinition = ChoreographyDefinition()
        
        assertTrue(emptyDefinition.warmingSequences.isEmpty())
        assertEquals(null, emptyDefinition.waitingSequence)
        assertEquals(null, emptyDefinition.hitSequence)
    }
    
    @Test
    fun `test resolveImageResources with large frame count`() {
        val mockResolver = createMockResolver()
        
        val sequence = ChoreographySequence(
            frames = "sprites/many_frames.png",
            frameWidth = 50,
            frameHeight = 50,
            frameCount = 100, // Large number of frames
            timing = 100.milliseconds
        )
        
        val resolvedImages = sequence.resolveImageResources(mockResolver)
        
        assertEquals(100, resolvedImages.size, "Should resolve all 100 frames")
        
        // Verify the resolver was called the expected number of times
        verify(exactly = 100) { mockResolver.resolve("sprites/many_frames.png") }
    }
}
