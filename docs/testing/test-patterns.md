# Testing Patterns

> **Purpose**: Proven test patterns for WorldWideWaves KMM codebase

## Test Organization

```
shared/src/
├── commonTest/          # Platform-independent tests (NO MockK, NO JVM APIs)
├── androidUnitTest/     # Android-specific tests (CAN use MockK)
└── iosTest/             # iOS-specific tests (NO MockK, Kotlin/Native compatible)
```

## Testing Infinite Flows

### ❌ WRONG: Causes Deadlock

```kotlin
@Test
fun testObserver() {
    observer.startObservation()
    testScheduler.advanceUntilIdle()  // DEADLOCK - infinite flow never completes
    // Test assertions
}
```

### ✅ CORRECT: Proper Flow Cancellation

```kotlin
@Test
fun testObserver() {
    observer.startObservation()
    testScheduler.runCurrent()  // Process current emissions only

    // Test assertions
    assertEquals(expected, observer.currentValue)

    observer.stopObservation()  // Cancel infinite flow FIRST
    testScheduler.advanceUntilIdle()  // Now safe to wait
}
```

## Testing ViewModels

### Async State Loading

```kotlin
@Test
fun testEventLoading() = runTest {
    // Start loading
    viewModel.loadEvents()

    // Wait for state change with timeout
    waitForEvents(viewModel, expectedSize = 5, timeoutMs = 3000)
    waitForState(viewModel.isLoading, expectedValue = false)

    // Assert final state
    assertEquals(5, viewModel.events.value.size)
    assertFalse(viewModel.isLoading.value)
}

// Helper function
suspend fun waitForEvents(
    viewModel: EventViewModel,
    expectedSize: Int,
    timeoutMs: Long = 3000
) = withTimeout(timeoutMs) {
    viewModel.events.first { it.size == expectedSize }
}
```

## Test Isolation with Koin

### ❌ WRONG: Test Interference

```kotlin
@AfterTest
fun tearDown() {
    stopKoin()  // Immediate stop causes flaky tests
}
```

### ✅ CORRECT: Proper Cleanup

```kotlin
@AfterTest
fun tearDown() {
    runBlocking {
        testScopeProvider.cancelAllCoroutines()
        delay(500)  // Wait for cleanup propagation
    }
    stopKoin()
}
```

## Testing Coroutines

### Time-Dependent Code

```kotlin
@Test
fun testScheduler() = runTest {
    val scheduler = ObservationScheduler(testClock)

    // Schedule event 10 seconds in future
    scheduler.scheduleEvent(eventTime = testClock.now() + 10.seconds)

    // Fast-forward virtual time
    testScheduler.advanceTimeBy(10.seconds)
    testScheduler.runCurrent()

    // Assert callback triggered
    assertTrue(scheduler.eventTriggered)
}
```

### Turbine for Flow Testing

```kotlin
@Test
fun testPositionFlow() = runTest {
    positionManager.positionFlow.test {
        // Emit test position
        positionManager.updatePosition(testPosition1)
        assertEquals(testPosition1, awaitItem())

        // Emit another
        positionManager.updatePosition(testPosition2)
        assertEquals(testPosition2, awaitItem())

        cancelAndIgnoreRemainingEvents()
    }
}
```

## Testing Map Components

### Android MapLibre

```kotlin
@Test
fun testMapBounds() {
    val map = AndroidEventMap(context, mapView)

    map.setBoundsForCameraTarget(eventBounds)

    val capturedBounds = mapView.getCameraForLatLngBounds(
        LatLngBounds.Builder()
            .include(eventBounds.southwest)
            .include(eventBounds.northeast)
            .build()
    )

    assertEquals(eventBounds, capturedBounds)
}
```

### iOS MapLibre (via wrapper)

```kotlin
@Test
fun testIosMapBounds() {
    val adapter = IosMapLibreAdapter(wrapperId = "test-map")

    adapter.setBoundsForCameraTarget(eventBounds)

    // Verify command queued in registry
    val command = MapWrapperRegistry.getPendingCommand(wrapperId = "test-map")
    assertTrue(command is SetCameraBoundsCommand)
    assertEquals(eventBounds, command.bounds)
}
```

## Testing Position System

### PositionManager Priority

```kotlin
@Test
fun testSimulationOverridesGPS() = runTest {
    val manager = PositionManager()

    // Set GPS position
    manager.updateGPSPosition(gpsPosition)
    assertEquals(gpsPosition, manager.positionFlow.value)

    // Simulation overrides GPS
    manager.startSimulation(simulatedPosition)
    assertEquals(simulatedPosition, manager.positionFlow.value)

    // Stopping simulation reverts to GPS
    manager.stopSimulation()
    assertEquals(gpsPosition, manager.positionFlow.value)
}
```

## Testing Accessibility

### Semantics Verification

```kotlin
@Test
fun testButtonAccessibility() {
    composeTestRule.setContent {
        EventButton(onClick = {}, text = "Join Wave")
    }

    composeTestRule.onNodeWithText("Join Wave")
        .assertExists()
        .assertHasClickAction()
        .assert(hasRole(Role.Button))
}

@Test
fun testContentDescription() {
    composeTestRule.setContent {
        EventIcon(
            icon = Icons.Default.Event,
            contentDescription = "Event icon"
        )
    }

    composeTestRule.onNode(hasContentDescription("Event icon"))
        .assertExists()
}
```

## iOS Safety Testing

### Automated Violation Detection

```kotlin
@Test
fun testNoDeadlockPatterns() {
    // Search for Composable-scoped KoinComponent
    val violations = findComposableKoinComponents()
    assertEquals(0, violations.size, "Found KoinComponent inside @Composable: $violations")
}

@Test
fun testNoInitCoroutineLaunches() {
    val violations = findInitCoroutines()
    assertEquals(0, violations.size, "Found coroutine launches in init{}: $violations")
}

private fun findComposableKoinComponents(): List<String> {
    // Implementation using code scanning
    return emptyList()  // Should remain empty
}
```

## Common Test Patterns

### Verify Exception Messages

```kotlin
@Test
fun testRequiresNonNull() {
    val exception = assertThrows<IllegalArgumentException> {
        createWave(linear = null, deep = null, linearSplit = null)
    }

    assertEquals(
        "Wave definition must exist after validation",
        exception.message
    )
}
```

### Test Data Builders

```kotlin
fun testEvent(
    id: String = "test-event",
    title: String = "Test Event",
    startTime: Instant = Clock.System.now(),
    location: LatLng = LatLng(48.8566, 2.3522)
): WWWEvent = WWWEvent(
    id = id,
    title = title,
    startTime = startTime,
    location = location,
    // ... other fields with sensible defaults
)

@Test
fun testEventDisplay() {
    val event = testEvent(title = "Paris Wave")
    assertEquals("Paris Wave", event.title)
}
```

### Parameterized Tests

```kotlin
@Test
fun testBoundsValidation() {
    val invalidBounds = listOf(
        Bounds(sw = LatLng(10, 10), ne = LatLng(5, 5)),   // ne < sw
        Bounds(sw = LatLng(-91, 0), ne = LatLng(10, 10)), // invalid lat
        Bounds(sw = LatLng(0, -181), ne = LatLng(10, 10)) // invalid lng
    )

    invalidBounds.forEach { bounds ->
        assertThrows<IllegalArgumentException> {
            validateBounds(bounds)
        }
    }
}
```

## Test Coverage Requirements

- **Unit Tests**: 902+ tests, 100% pass rate required
- **Critical Paths**: Wave detection, scheduling, position management
- **Platform-Specific**: iOS deadlock prevention, Android lifecycle
- **Performance**: Test execution < 30 seconds for full suite

## Reference

- **Test Organization**: shared/src/{commonTest,androidUnitTest,iosTest}
- **Coverage Report**: `./gradlew :shared:testDebugUnitTest` generates HTML report
- **CI Pipeline**: .github/workflows/03-quality-security-gates.yml
- **iOS Cinterop Testing**: [Cinterop Testing Patterns](./cinterop-testing-patterns.md) - Memory safety, protocols, threading
