# State Management Patterns

> **Last Updated**: October 27, 2025
> **Status**: Production patterns from 902+ tests
> **Scope**: UI State, Domain State, ViewModels

## Table of Contents

1. [Overview](#overview)
2. [UI State Pattern](#1-ui-state-pattern)
3. [Domain State Pattern](#2-domain-state-pattern)
4. [State Updates](#3-state-updates)
5. [State Validation](#4-state-validation)
6. [Testing State](#5-testing-state)
7. [Common Pitfalls](#6-common-pitfalls)

---

## Overview

WorldWideWaves uses **reactive state management** with Kotlin StateFlow for both UI and domain logic. All state follows immutable, copy-based update patterns with smart throttling and validation.

### Core Principles

1. **Single Source of Truth**: Each state variable has one authoritative source
2. **Immutability**: State objects are immutable data classes
3. **Reactive Updates**: StateFlow propagates changes to observers
4. **Smart Throttling**: Reduce unnecessary emissions with thresholds
5. **Thread Safety**: All state updates are atomic and thread-safe
6. **Validation**: State transitions are validated for consistency

### Architecture Layers

```
┌─────────────────────────────────────────────────┐
│         UI Layer (Compose Screens)               │
│  - Collects StateFlow in LaunchedEffect         │
│  - Renders immutable state snapshots             │
└─────────────────┬───────────────────────────────┘
                  │ observes
┌─────────────────▼───────────────────────────────┐
│         ViewModel Layer                          │
│  - MutableStateFlow (private)                   │
│  - StateFlow (public, read-only)                │
│  - Delegates logic to use cases                 │
└─────────────────┬───────────────────────────────┘
                  │ updates
┌─────────────────▼───────────────────────────────┐
│         Domain State Layer                       │
│  - EventProgressionState                        │
│  - Sealed classes for status                    │
│  - Smart throttling + validation                │
└─────────────────────────────────────────────────┘
```

---

## 1. UI State Pattern

### 1.1 Data Class Structure

**Pattern**: Immutable data classes for UI state containers

```kotlin
// ViewModel: EventsViewModel.kt:62-73
class EventsViewModel(...) : BaseViewModel() {
    // ✅ CORRECT: Private mutable, public immutable
    private val _hasFavorites = MutableStateFlow(false)
    val hasFavorites: StateFlow<Boolean> = _hasFavorites.asStateFlow()

    private val _events = MutableStateFlow<List<IWWWEvent>>(emptyList())
    val events: StateFlow<List<IWWWEvent>> = _events.asStateFlow()

    private val _loadingError = MutableStateFlow(false)
    val hasLoadingError: StateFlow<Boolean> = _loadingError.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
}
```

**Key Points**:
- Private `MutableStateFlow` with `_` prefix (e.g., `_isLoading`)
- Public `StateFlow` without prefix (e.g., `isLoading`)
- Use `.asStateFlow()` to expose read-only view
- Default values for all state properties

### 1.2 Complex UI State

**Pattern**: Group related state in data class

```kotlin
// Map download state: EventMapDownloadManager.kt:41-46
data class DownloadState(
    val isAvailable: Boolean = false,
    val isDownloading: Boolean = false,
    val progress: Int = 0,
    val error: String? = null,
)

// Usage
private val _downloadStates = mutableMapOf<String, MutableStateFlow<DownloadState>>()

fun getDownloadState(mapId: String): StateFlow<DownloadState> =
    _downloadStates
        .getOrPut(mapId) {
            MutableStateFlow(DownloadState())
        }.asStateFlow()
```

**Benefits**:
- Single state object for related properties
- Default values prevent null checks
- Easier to update atomically via `.copy()`

### 1.3 Loading/Error/Success States

**Pattern**: Sealed classes for mutually exclusive states

```kotlin
// Map feature states: MapStateHolder.kt:29-59
sealed class MapFeatureState {
    object NotChecked : MapFeatureState()
    object Available : MapFeatureState()
    object NotAvailable : MapFeatureState()
    object Pending : MapFeatureState()

    data class Downloading(
        val progress: Int,
    ) : MapFeatureState()

    object Installing : MapFeatureState()
    object Installed : MapFeatureState()

    data class Failed(
        val errorCode: Int,
        val errorMessage: String? = null,
    ) : MapFeatureState()

    object Canceling : MapFeatureState()
    object Unknown : MapFeatureState()

    data class Retrying(
        val attempt: Int,
        val maxAttempts: Int,
    ) : MapFeatureState()
}
```

**When to Use Sealed Classes**:
- Mutually exclusive states (can't be loading AND failed)
- State machines with clear transitions
- States with different data requirements

**When to Use Data Classes**:
- Multiple boolean flags that can coexist
- Simple property bags without logic
- Performance-critical paths (fewer allocations)

### 1.4 Default Values

**Pattern**: Always provide sensible defaults

```kotlin
// EventState.kt:35-47 - Domain state with all defaults
data class EventState(
    val progression: Double,
    val status: Status,
    val isUserWarmingInProgress: Boolean,
    val isStartWarmingInProgress: Boolean,
    val userIsGoingToBeHit: Boolean,
    val userHasBeenHit: Boolean,
    val userPositionRatio: Double,
    val timeBeforeHit: Duration,
    val hitDateTime: Instant,
    val userIsInArea: Boolean,
    val timestamp: Instant,
)

// ViewModel initialization with defaults
private val _events = MutableStateFlow<List<IWWWEvent>>(emptyList())  // Empty list, not null
private val _isLoading = MutableStateFlow(false)  // Start with false
```

**Guidelines**:
- Collections: Use `emptyList()`, `emptyMap()`, not `null`
- Booleans: Use `false` unless "true" is the initial state
- Numbers: Use `0`, `0.0`, or meaningful minimums
- Durations: Use `Duration.ZERO` or `Duration.INFINITE`
- Instants: Use `Instant.DISTANT_FUTURE` for unset timestamps

---

## 2. Domain State Pattern

### 2.1 Sealed Classes for Status

**Pattern**: Enum for simple status, sealed class for complex status with data

```kotlin
// Simple enum status: IWWWEvent.kt:83
enum class Status {
    UNDEFINED,
    DONE,
    NEXT,
    SOON,
    RUNNING
}

// Complex sealed class (if needed):
sealed class WaveStatus {
    object NotStarted : WaveStatus()
    data class InProgress(val elapsedSeconds: Int) : WaveStatus()
    data class Completed(val timestamp: Instant) : WaveStatus()
}
```

**Decision Tree**:
```
Does status need associated data?
├─ NO  → Use enum
└─ YES → Use sealed class
    ├─ Simple data → data class variants
    └─ No data    → object variants
```

### 2.2 State Machines

**Pattern**: Explicit state transition logic with validation

```kotlin
// EventProgressionState.kt:80-108 - State machine for event progression
class EventProgressionState {
    private val _eventStatus = MutableStateFlow(Status.UNDEFINED)
    val eventStatus: StateFlow<Status> = _eventStatus.asStateFlow()

    private val _progression = MutableStateFlow(0.0)
    val progression: StateFlow<Double> = _progression.asStateFlow()

    private val _isUserWarmingInProgress = MutableStateFlow(false)
    val isUserWarmingInProgress: StateFlow<Boolean> = _isUserWarmingInProgress.asStateFlow()

    // ... other state flows
}
```

**Transition Validation**:
```kotlin
// EventStateHolderBasicTest.kt:99-100
@Test
fun `validateState detects status-progression inconsistencies`() {
    // DONE status should have 100% progression
    val input = createTestInput(progression = 50.0, status = Status.DONE)
    val issues = eventStateHolder.validateState(input, state)

    assertTrue(issues.any { it.field == "status" })
}
```

### 2.3 Smart Throttling

**Pattern**: Only emit state updates when changes are significant

```kotlin
// EventProgressionState.kt:162-170
private fun updateProgressionIfSignificant(newProgression: Double) {
    if (abs(newProgression - lastEmittedProgression) >= PROGRESSION_THRESHOLD ||
        lastEmittedProgression < 0.0 ||
        newProgression >= 100.0
    ) { // Always emit first update or completion
        _progression.updateIfChanged(newProgression)
        lastEmittedProgression = newProgression
    }
}

// Thresholds: EventProgressionState.kt:70-75
companion object {
    private const val PROGRESSION_THRESHOLD = 0.1 // Only update if change > 0.1%
    private const val POSITION_RATIO_THRESHOLD = 0.01 // Only update if change > 1%
    private const val TIME_THRESHOLD_MS = 1000L // Normal: update if change > 1 second
    private const val CRITICAL_TIME_THRESHOLD_MS = 50L // Critical: update if change > 50ms
}
```

**Performance Characteristics**:
- Progression updates: ~20% of raw updates emitted (80% reduction)
- Position updates: ~50% of raw updates emitted
- Time updates: Adaptive based on proximity to hit

**When to Use Throttling**:
- High-frequency updates (GPS, timers, animations)
- Continuous values (progression percentages, distances)
- Performance-critical paths (main thread updates)

**When NOT to Use Throttling**:
- Boolean state changes (always significant)
- User interactions (button clicks)
- Error states (must propagate immediately)

### 2.4 Adaptive Precision

**Pattern**: Adjust throttling based on context

```kotlin
// EventProgressionState.kt:192-212 - Adaptive timing for wave hits
private fun updateTimeBeforeHitIfSignificant(newTime: Duration) {
    // Handle infinite durations
    if (newTime == INFINITE || lastEmittedTimeBeforeHit == INFINITE) {
        if (newTime != lastEmittedTimeBeforeHit) {
            _timeBeforeHit.updateIfChanged(newTime)
            lastEmittedTimeBeforeHit = newTime
        }
        return
    }

    val timeDifference = abs((newTime - lastEmittedTimeBeforeHit).inWholeMilliseconds)

    // Critical timing phase: Need sub-50ms accuracy for wave synchronization
    val isCriticalPhase = newTime.inWholeSeconds <= CRITICAL_PHASE_SECONDS && newTime > Duration.ZERO
    val threshold = if (isCriticalPhase) CRITICAL_TIME_THRESHOLD_MS else TIME_THRESHOLD_MS

    if (timeDifference >= threshold) {
        _timeBeforeHit.updateIfChanged(newTime)
        lastEmittedTimeBeforeHit = newTime
    }
}
```

**Use Cases**:
- Wave timing: 50ms precision during hit phase, 1000ms normally
- Map zoom: Fine-grained during user interaction, coarse during auto-zoom
- Distance calculations: Precise when near event, approximate when far

---

## 3. State Updates

### 3.1 Immutable Copy Pattern

**Pattern**: Never mutate state, always create new instances

```kotlin
// ✅ CORRECT: Copy-based update - EventMapDownloadManager.kt:65
updateState(mapId) { it.copy(isAvailable = isAvailable, error = null) }

// ✅ CORRECT: Multi-property update
updateState(mapId) {
    it.copy(
        isAvailable = true,
        isDownloading = false,
        progress = 100,
        error = null,
    )
}

// ❌ WRONG: Direct mutation
state.isAvailable = true  // DON'T DO THIS
```

### 3.2 updateIfChanged Extension

**Pattern**: Prevent unnecessary emissions

```kotlin
// FlowExtensions.kt:32-36
fun <T> MutableStateFlow<T>.updateIfChanged(newValue: T) {
    if (value != newValue) {
        value = newValue
    }
}

// Usage: EventProgressionState.kt:126-133
fun updateFromEventState(state: EventState) {
    updateProgressionIfSignificant(state.progression)
    _eventStatus.updateIfChanged(state.status)  // Only emits if changed
    _isUserWarmingInProgress.updateIfChanged(state.isUserWarmingInProgress)
    _userIsGoingToBeHit.updateIfChanged(state.userIsGoingToBeHit)
    // ... more updates
}
```

**Why This Matters**:
- Prevents downstream recomposition/recollection
- Reduces UI churn
- Improves performance in high-frequency updates

### 3.3 Atomic State Updates

**Pattern**: Update multiple related properties atomically

```kotlin
// ✅ CORRECT: Atomic update via single copy
private fun updateDownloadState(mapId: String, update: (DownloadState) -> DownloadState) {
    val stateFlow = _downloadStates.getOrPut(mapId) { MutableStateFlow(DownloadState()) }
    stateFlow.value = update(stateFlow.value)
}

// Usage: EventMapDownloadManager.kt:87-93
onSuccess = {
    updateState(mapId) {
        it.copy(
            isAvailable = true,
            isDownloading = false,
            progress = 100,
            error = null,
        )
    }
}

// ❌ WRONG: Non-atomic updates (race condition risk)
_isAvailable.value = true
_isDownloading.value = false  // Observers might see intermediate state!
_progress.value = 100
```

### 3.4 Direct Assignment Pattern

**Pattern**: Simple direct assignment for non-related state

```kotlin
// EventsViewModel.kt:93-121 - Simple state updates
try {
    eventsRepository.loadEvents { exception ->
        Log.e("EventsViewModel", "Error loading events", throwable = exception)
        _loadingError.value = true  // Direct assignment OK for simple flag
    }

    eventsRepository
        .isLoading()
        .onEach { isLoading -> _isLoading.value = isLoading }  // Direct in flow
        .launchIn(scope)
} catch (e: Exception) {
    _loadingError.value = true
}
```

**When to Use Direct Assignment**:
- Single independent state variable
- Boolean flags without related data
- States updated in single coroutine context

**When to Use Copy Pattern**:
- Multiple related properties
- Complex data structures
- States shared across threads

### 3.5 Thread-Safe Updates with Mutex

**Pattern**: Protect mutable shared state with Mutex

```kotlin
// MapStore.kt:58-71 - Thread-safe download gate
object MapDownloadGate {
    private val mutex = Mutex()
    private val allowed = mutableSetOf<String>()

    suspend fun allow(tag: String) {
        mutex.withLock { allowed += tag }
    }

    suspend fun disallow(tag: String) {
        mutex.withLock { allowed -= tag }
    }

    fun isAllowed(tag: String) = tag in allowed  // Read-only, no lock needed
}
```

**When to Use Mutex**:
- Mutable collections accessed from multiple coroutines
- Critical sections with multiple operations
- State that must be updated atomically

**When NOT to Use Mutex**:
- StateFlow updates (already thread-safe)
- Immutable data structures
- Single-threaded contexts

---

## 4. State Validation

### 4.1 Pre-condition Checks

**Pattern**: Validate inputs before state transitions

```kotlin
// PositionManager.kt:82-95 - Source priority validation
fun updatePosition(
    source: PositionSource,
    newPosition: Position?,
) {
    val newState = PositionState(newPosition, if (newPosition == null) null else source)

    // Pre-condition: Check source priority
    val currentState = _currentState.value
    if (!shouldAcceptUpdate(currentState, newState)) {
        Log.i("PositionManager", "[DEBUG] Rejected position update from $source")
        return  // Early exit if pre-condition fails
    }

    // Pre-condition: Check for duplicates
    if (isPositionDuplicate(currentState.position, newPosition)) {
        Log.i("PositionManager", "[DEBUG] Skipped duplicate position update")
        return
    }

    // Proceed with update
    pendingUpdate = newState
    // ...
}
```

**Validation Checklist**:
- ✅ Source priority (GPS vs Simulation)
- ✅ Value ranges (0-100%, lat/lng bounds)
- ✅ Duplicate detection (epsilon comparison)
- ✅ State consistency (DONE status = 100% progression)

### 4.2 Post-condition Validation

**Pattern**: Verify state after transitions

```kotlin
// EventStateHolderBasicTest.kt:42-70 - Validation after state calculation
@Test
fun `validateState returns no issues for valid progression`() {
    val input = EventStateInput(
        progression = 50.0,
        status = Status.RUNNING,
        userPosition = Position(40.7128, -74.0060),
        currentTime = Instant.fromEpochSeconds(1000),
    )

    val state = EventState(
        progression = 50.0,
        status = Status.RUNNING,
        // ... other properties
    )

    val issues = eventStateHolder.validateState(input, state)

    assertTrue(issues.isEmpty(), "Should have no validation issues for valid state")
}
```

### 4.3 Invariant Enforcement

**Pattern**: Define and enforce state invariants

```kotlin
// Example invariants for EventState:
// 1. progression ∈ [0.0, 100.0]
// 2. status = DONE ⟹ progression = 100.0
// 3. status = UNDEFINED ⟹ progression = 0.0
// 4. userHasBeenHit ⟹ userIsGoingToBeHit was true earlier
// 5. timeBeforeHit > 0 ⟹ hitDateTime > currentTime

// EventStateHolderBasicTest.kt:73-96 - Invariant tests
@Test
fun `validateState detects progression out of bounds`() {
    val validProgression = listOf(0.0, 50.0, 100.0)
    val invalidProgression = listOf(-1.0, 101.0, Double.NaN, Double.POSITIVE_INFINITY)

    validProgression.forEach { progression ->
        val input = createTestInput(progression = progression)
        val state = createTestState(progression = progression)
        val issues = eventStateHolder.validateState(input, state)

        val progressionIssues = issues.filter { it.field == "progression" }
        assertTrue(progressionIssues.isEmpty(), "Progression $progression should be valid")
    }

    invalidProgression.forEach { progression ->
        val input = createTestInput(progression = progression)
        val state = createTestState(progression = progression)
        val issues = eventStateHolder.validateState(input, state)

        val progressionIssues = issues.filter { it.field == "progression" }
        assertTrue(progressionIssues.isNotEmpty(), "Progression $progression should be invalid")
    }
}
```

### 4.4 Validation Issue Reporting

**Pattern**: Return structured validation results

```kotlin
// EventState.kt:63-72 - Validation issue structure
data class StateValidationIssue(
    val field: String,
    val issue: String,
    val severity: Severity = Severity.WARNING,
) {
    enum class Severity {
        WARNING,
        ERROR,
    }
}

// Usage in tests
val issues = eventStateHolder.validateState(input, state)
val criticalIssues = issues.filter { it.severity == Severity.ERROR }
if (criticalIssues.isNotEmpty()) {
    fail("Critical validation failures: ${criticalIssues.joinToString()}")
}
```

---

## 5. Testing State

### 5.1 Initial State Verification

**Pattern**: Test default state on initialization

```kotlin
// EventsViewModelTest.kt:84-100
@BeforeTest
fun setUp() {
    sharedTestClock = TestClock(currentTime = Instant.fromEpochMilliseconds(0))
    testScopeProvider = DefaultCoroutineScopeProvider()

    startKoin {
        modules(
            module {
                single<IClock> { sharedTestClock }
                single<CoroutineScopeProvider> { testScopeProvider }
            },
        )
    }
}

@Test
fun `initial state is correct`() = runTest {
    val viewModel = createViewModel()

    assertEquals(emptyList(), viewModel.events.value)
    assertFalse(viewModel.isLoading.value)
    assertFalse(viewModel.hasLoadingError.value)
    assertFalse(viewModel.hasFavorites.value)
}
```

### 5.2 State Transition Testing

**Pattern**: Test state changes through actions

```kotlin
@Test
fun `loadEvents updates state correctly`() = runTest {
    val viewModel = createViewModel()
    val mockEvents = listOf(createMockEvent("event1"))

    // Initial state
    assertEquals(emptyList(), viewModel.events.value)

    // Trigger state change
    viewModel.loadEvents()
    advanceUntilIdle()

    // Verify state transition
    assertEquals(mockEvents.size, viewModel.events.value.size)
    assertFalse(viewModel.isLoading.value)
}
```

### 5.3 Error State Testing

**Pattern**: Verify error propagation and recovery

```kotlin
@Test
fun `error during loadEvents sets error state`() = runTest {
    val repository = createFailingRepository()
    val viewModel = EventsViewModel(repository, ...)

    viewModel.loadEvents()
    advanceUntilIdle()

    assertTrue(viewModel.hasLoadingError.value)
    assertEquals(emptyList(), viewModel.events.value)
}

@Test
fun `retry after error clears error state`() = runTest {
    val viewModel = createViewModel()

    // Trigger error
    viewModel.loadEvents()
    assertTrue(viewModel.hasLoadingError.value)

    // Retry
    viewModel.loadEvents()
    advanceUntilIdle()

    assertFalse(viewModel.hasLoadingError.value)
}
```

### 5.4 State History Testing

**Pattern**: Verify state progression over time

```kotlin
@Test
fun `state progresses correctly over wave lifecycle`() = runTest {
    val stateHistory = mutableListOf<EventState>()

    // Collect state history
    launch {
        eventState.eventStatus.collect { status ->
            stateHistory.add(getCurrentState())
        }
    }

    // Trigger lifecycle
    startWave()
    advanceTimeBy(10_000) // 10 seconds

    // Verify state progression
    assertEquals(Status.UNDEFINED, stateHistory[0].status)
    assertEquals(Status.RUNNING, stateHistory[1].status)
    assertEquals(Status.DONE, stateHistory.last().status)
}
```

### 5.5 Throttling Testing

**Pattern**: Verify throttling reduces emissions

```kotlin
@Test
fun `progression throttling reduces emissions`() = runTest {
    val emissionCount = atomic(0)

    launch {
        eventState.progression.collect {
            emissionCount.incrementAndGet()
        }
    }

    // Send 100 updates with small increments
    repeat(100) { i ->
        eventState.updateProgressionIfSignificant(i * 0.05) // 0.05% increments
    }

    // Verify throttling (threshold is 0.1%)
    // Expected: ~50 emissions (every 2 updates)
    assertTrue(emissionCount.value < 60, "Expected ~50 emissions, got ${emissionCount.value}")
}
```

---

## 6. Common Pitfalls

### 6.1 Mutable State in Data Classes

**Problem**: Exposing mutable state breaks immutability

```kotlin
// ❌ WRONG: Mutable property in data class
data class EventState(
    var progression: Double,  // DON'T USE var!
    var status: Status,
)

// Consumers can mutate!
val state = viewModel.getState()
state.progression = 99.0  // Breaks single source of truth

// ✅ CORRECT: Immutable properties
data class EventState(
    val progression: Double,  // Use val
    val status: Status,
)

// Consumers must use copy
val newState = state.copy(progression = 99.0)
```

### 6.2 Forgetting to Copy on Update

**Problem**: Direct mutation instead of copy

```kotlin
// ❌ WRONG: Direct mutation
fun updateProgress(newProgress: Int) {
    val current = _downloadState.value
    current.progress = newProgress  // Won't emit new value!
    // StateFlow compares by reference, not value
}

// ✅ CORRECT: Copy-based update
fun updateProgress(newProgress: Int) {
    _downloadState.value = _downloadState.value.copy(progress = newProgress)
}

// ✅ ALSO CORRECT: Update function
fun updateProgress(newProgress: Int) {
    _downloadState.value = _downloadState.value.copy(progress = newProgress)
}
```

### 6.3 Race Conditions in State Updates

**Problem**: Non-atomic updates to related state

```kotlin
// ❌ WRONG: Multiple assignments create intermediate states
suspend fun completeDownload() {
    _isDownloading.value = false  // State 1: not downloading, not available
    delay(100)  // Simulates async work
    _isAvailable.value = true     // State 2: not downloading, available
    // Observers might see inconsistent intermediate state!
}

// ✅ CORRECT: Atomic update via data class
data class DownloadState(
    val isDownloading: Boolean,
    val isAvailable: Boolean,
)

suspend fun completeDownload() {
    _state.value = _state.value.copy(
        isDownloading = false,
        isAvailable = true,
    )
    // Observers see consistent state transition
}
```

### 6.4 Not Using updateIfChanged

**Problem**: Excessive emissions trigger unnecessary work

```kotlin
// ❌ WRONG: Always emits, even if value unchanged
fun updateStatus(newStatus: Status) {
    _status.value = newStatus  // Emits even if newStatus == current status
}

// Result: Downstream collectors run unnecessarily
launch {
    viewModel.status.collect { status ->
        expensiveUIUpdate(status)  // Runs even when status didn't change!
    }
}

// ✅ CORRECT: Use updateIfChanged
fun updateStatus(newStatus: Status) {
    _status.updateIfChanged(newStatus)  // Only emits if changed
}
```

### 6.5 Complex State That Should Be Split

**Problem**: God object with too many unrelated properties

```kotlin
// ❌ WRONG: Monolithic state with unrelated concerns
data class AppState(
    val events: List<Event>,
    val isLoading: Boolean,
    val error: String?,
    val downloadProgress: Int,
    val mapZoom: Float,
    val userPosition: Position?,
    val soundEnabled: Boolean,
    val themeMode: String,
    // ... 20 more properties
)

// Every state change triggers all collectors!

// ✅ CORRECT: Split by responsibility
class EventsViewModel {
    private val _events = MutableStateFlow<List<Event>>(emptyList())
    private val _isLoading = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)
}

class MapViewModel {
    private val _zoom = MutableStateFlow(12.0f)
    private val _userPosition = MutableStateFlow<Position?>(null)
}

class SettingsViewModel {
    private val _soundEnabled = MutableStateFlow(true)
    private val _themeMode = MutableStateFlow("system")
}
```

### 6.6 Missing Default Values

**Problem**: Nullable state when default makes sense

```kotlin
// ❌ PROBLEMATIC: Nullable requires null checks everywhere
private val _events = MutableStateFlow<List<Event>?>(null)

fun render() {
    val events = viewModel.events.value ?: emptyList()  // Repetitive
    // ...
}

// ✅ CORRECT: Non-null with sensible default
private val _events = MutableStateFlow<List<Event>>(emptyList())

fun render() {
    val events = viewModel.events.value  // No null check needed
    // ...
}
```

### 6.7 iOS Deadlock Patterns

**Problem**: Object-scoped KoinComponent in @Composable

```kotlin
// ❌ WRONG: Object inside @Composable causes iOS deadlock
@Composable
fun MyScreen() {
    val deps = object : KoinComponent {
        val clock by inject()  // DEADLOCKS iOS!
    }
}

// ✅ CORRECT: Use file-level singleton or parameter injection
object IOSSafeDI : KoinComponent {  // File-level is safe
    val clock: IClock by inject()
}

@Composable
fun MyScreen() {
    val clock = IOSSafeDI.clock  // Safe access
}

// ✅ ALSO CORRECT: Parameter injection
@Composable
fun MyScreen(clock: IClock) {
    // Use clock
}
```

**See Also**: [CLAUDE.md iOS Requirements](../../CLAUDE.md#-ios-requirements-critical)

---

## Quick Reference

### State Update Decision Tree

```
Do you need to update state?
├─ Single independent property
│  └─ _property.value = newValue
│
├─ Multiple related properties
│  └─ _state.value = _state.value.copy(prop1 = val1, prop2 = val2)
│
├─ High-frequency updates
│  └─ Use smart throttling with updateIfChanged
│
├─ Shared mutable state
│  └─ Protect with Mutex.withLock {}
│
└─ Complex state transitions
   └─ Use sealed classes + state machine
```

### Testing Checklist

- [ ] Initial state verification
- [ ] State transitions on actions
- [ ] Error state propagation
- [ ] State history validation
- [ ] Throttling effectiveness
- [ ] Thread safety (if concurrent)
- [ ] Cleanup/disposal

### Performance Guidelines

| Pattern | Emission Reduction | Use Case |
|---------|-------------------|----------|
| `updateIfChanged` | ~30-50% | Boolean/enum changes |
| Progression throttling | ~80% | Continuous values (0-100%) |
| Position throttling | ~50% | GPS/location updates |
| Adaptive timing | ~70-90% | Context-dependent precision |

---

## Related Documentation

- **[CLAUDE.md](../../CLAUDE.md)** - Project overview and iOS safety requirements
- **[Architecture Guide](../architecture.md)** - System architecture patterns
- **[Testing Strategy](../testing-strategy.md)** - Comprehensive testing approach
- **[ViewModel Patterns](./viewmodel-patterns.md)** - ViewModel-specific patterns

---

**Last Updated**: October 27, 2025
**Maintainer**: WorldWideWaves Development Team
