package com.worldwidewaves.shared.utils

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class HelpersTest {
    @Test
    fun `test updateIfChanged updates value when different`() =
        runTest {
            // Arrange
            val flow = MutableStateFlow("initial")

            // Act
            flow.updateIfChanged("updated")

            // Assert
            assertEquals("updated", flow.value, "Flow value should be updated when new value is different")
        }

    @Test
    fun `test updateIfChanged does not update value when same`() =
        runTest {
            // Arrange
            val flow = MutableStateFlow("same")

            // Act
            flow.updateIfChanged("same")

            // Assert
            assertEquals("same", flow.value, "Flow value should remain unchanged when new value is the same")
        }

    @Test
    fun `test updateIfChanged with various primitive types`() =
        runTest {
            // Integer test
            val intFlow = MutableStateFlow(42)
            intFlow.updateIfChanged(42)
            assertEquals(42, intFlow.value, "Integer value should remain unchanged")
            intFlow.updateIfChanged(100)
            assertEquals(100, intFlow.value, "Integer value should be updated")

            // Boolean test
            val boolFlow = MutableStateFlow(true)
            boolFlow.updateIfChanged(true)
            assertEquals(true, boolFlow.value, "Boolean value should remain unchanged")
            boolFlow.updateIfChanged(false)
            assertEquals(false, boolFlow.value, "Boolean value should be updated")

            // Double test
            val doubleFlow = MutableStateFlow(3.14)
            doubleFlow.updateIfChanged(3.14)
            assertEquals(3.14, doubleFlow.value, "Double value should remain unchanged")
            doubleFlow.updateIfChanged(2.71)
            assertEquals(2.71, doubleFlow.value, "Double value should be updated")
        }

    @Test
    fun `test updateIfChanged with null values`() =
        runTest {
            // Nullable flow with initial null value
            val nullableFlow = MutableStateFlow<String?>(null)

            // Update with null (same value)
            nullableFlow.updateIfChanged(null)
            assertEquals(null, nullableFlow.value, "Null value should remain unchanged")

            // Update with non-null value
            nullableFlow.updateIfChanged("not null")
            assertEquals("not null", nullableFlow.value, "Value should be updated from null to non-null")

            // Update back to null (different value)
            nullableFlow.updateIfChanged(null)
            assertEquals(null, nullableFlow.value, "Value should be updated from non-null to null")
        }

    @Test
    fun `test updateIfChanged with collections`() =
        runTest {
            // List test
            val listFlow = MutableStateFlow(listOf(1, 2, 3))

            // Same list content but different instance
            listFlow.updateIfChanged(listOf(1, 2, 3))
            assertEquals(listOf(1, 2, 3), listFlow.value, "List should be updated even with same content due to reference equality")

            // Different list content
            listFlow.updateIfChanged(listOf(4, 5, 6))
            assertEquals(listOf(4, 5, 6), listFlow.value, "List should be updated with different content")

            // Empty list
            listFlow.updateIfChanged(emptyList())
            assertEquals(emptyList<Int>(), listFlow.value, "List should be updated to empty list")

            // Map test
            val mapFlow = MutableStateFlow(mapOf("key1" to 1, "key2" to 2))
            mapFlow.updateIfChanged(mapOf("key1" to 1, "key2" to 2))
            assertEquals(mapOf("key1" to 1, "key2" to 2), mapFlow.value, "Map should be updated due to reference equality")
        }

    @Test
    fun `test updateIfChanged with custom data class`() =
        runTest {
            // Define a simple data class for testing
            data class TestData(
                val id: Int,
                val name: String,
            )

            val dataFlow = MutableStateFlow(TestData(1, "test"))

            // Same content, different instance
            dataFlow.updateIfChanged(TestData(1, "test"))
            assertEquals(
                TestData(1, "test"),
                dataFlow.value,
                "Data class should be updated even with same content due to reference equality",
            )

            // Different content
            dataFlow.updateIfChanged(TestData(2, "updated"))
            assertEquals(TestData(2, "updated"), dataFlow.value, "Data class should be updated with different content")
        }

    @Test
    fun `test updateIfChanged with custom class using equals override`() =
        runTest {
            // Define a class with custom equals implementation
            class CustomEqualsClass(
                val value: String,
            ) {
                override fun equals(other: Any?): Boolean {
                    if (this === other) return true
                    if (other !is CustomEqualsClass) return false
                    return value == other.value
                }

                override fun hashCode(): Int = value.hashCode()
            }

            val customFlow = MutableStateFlow(CustomEqualsClass("test"))

            // Same content, different instance, but equals() returns true
            customFlow.updateIfChanged(CustomEqualsClass("test"))
            assertTrue(customFlow.value.value == "test", "Custom class should not be updated when equals() returns true")

            // Different content
            customFlow.updateIfChanged(CustomEqualsClass("updated"))
            assertTrue(customFlow.value.value == "updated", "Custom class should be updated when equals() returns false")
        }

    @Test
    fun `test subscribers are only notified when value actually changes`() =
        runTest {
            // Create a test scope with a dispatcher
            val testScope = TestScope(UnconfinedTestDispatcher())

            // Create the flow
            val flow = MutableStateFlow("initial")

            // Collect values
            val collectedValues = mutableListOf<String>()
            val job =
                testScope.launch {
                    flow.collect { value ->
                        collectedValues.add(value)
                    }
                }

            // Initial value should be collected
            assertEquals(1, collectedValues.size, "Initial value should be collected")
            assertEquals("initial", collectedValues[0], "Initial value should be 'initial'")

            // Update with same value - should not trigger collection
            flow.updateIfChanged("initial")
            assertEquals(1, collectedValues.size, "No new collection should happen when value doesn't change")

            // Update with different value - should trigger collection
            flow.updateIfChanged("updated")
            assertEquals(2, collectedValues.size, "New collection should happen when value changes")
            assertEquals("updated", collectedValues[1], "New value should be 'updated'")

            // Update with same value again - should not trigger collection
            flow.updateIfChanged("updated")
            assertEquals(2, collectedValues.size, "No new collection should happen when value doesn't change")

            // Clean up
            job.cancel()
        }

    @Test
    fun `test performance by counting actual updates`() =
        runTest {
            // Create a wrapper to count actual updates
            class UpdateCounter<T>(
                initialValue: T,
            ) {
                val flow = MutableStateFlow(initialValue)
                var updateCount = 0
                    private set

                fun update(newValue: T) {
                    val oldValue = flow.value
                    flow.updateIfChanged(newValue)
                    if (oldValue != newValue) {
                        updateCount++
                    }
                }
            }

            // Test with a series of updates
            val counter = UpdateCounter("start")

            // First update should increment counter
            counter.update("change1")
            assertEquals(1, counter.updateCount, "Counter should increment on first change")
            assertEquals("change1", counter.flow.value, "Value should be updated")

            // Same value should not increment counter
            counter.update("change1")
            assertEquals(1, counter.updateCount, "Counter should not increment when value doesn't change")

            // Different value should increment counter
            counter.update("change2")
            assertEquals(2, counter.updateCount, "Counter should increment on second change")
            assertEquals("change2", counter.flow.value, "Value should be updated again")

            // Multiple updates with only some changes
            val values = listOf("change2", "change3", "change3", "change4", "change4", "change4", "change5")
            values.forEach { counter.update(it) }

            // Should only have 3 more updates (for change3, change4, change5)
            assertEquals(5, counter.updateCount, "Counter should only increment for actual changes")
            assertEquals("change5", counter.flow.value, "Final value should be correct")
        }

    @Test
    fun `test with large number of identical updates`() =
        runTest {
            val flow = MutableStateFlow(0)
            var emissionCount = 0

            // Collect and count emissions
            val job =
                launch {
                    flow.collect {
                        emissionCount++
                    }
                }

            runCurrent()

            // Initial emission
            assertEquals(1, emissionCount, "Should have initial emission")

            // Perform 1000 identical updates
            repeat(1000) {
                flow.updateIfChanged(0)
            }

            // Should still only have the initial emission
            assertEquals(1, emissionCount, "Should still only have initial emission after identical updates")

            // One real update
            flow.updateIfChanged(1)
            runCurrent()

            assertEquals(2, emissionCount, "Should have one additional emission after value change")

            job.cancel()
        }
}
