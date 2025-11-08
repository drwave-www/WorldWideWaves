# Class Organization Standards

> **Purpose**: Standard structure for Kotlin classes in WorldWideWaves

## Standard Class Structure

Classes should be organized top to bottom in this order:

```kotlin
class MyClass {
    // 1. COMPANION OBJECT (always first)
    companion object {
        private const val TAG = "MyClass"
        fun create(): MyClass = MyClass()
    }

    // 2. PROPERTIES
    // Public properties first
    val publicProperty: String

    // Private properties second
    private val privateProperty: Int

    // 3. INIT BLOCKS
    init {
        // Initialization logic
    }

    // 4. PUBLIC API METHODS
    fun publicMethod() {
        // Implementation
    }

    // 5. INTERNAL/PROTECTED METHODS
    internal fun internalMethod() {
        // Implementation
    }

    // 6. PRIVATE HELPER METHODS
    private fun helperMethod() {
        // Implementation
    }

    // 7. NESTED CLASSES/OBJECTS
    data class NestedData(val value: String)
}
```

## Section Comments for Large Files

For files >200 lines, use section comments:

```kotlin
// ============================================================
// PUBLIC API
// ============================================================

fun publicMethod1() { }
fun publicMethod2() { }

// ============================================================
// PRIVATE HELPERS
// ============================================================

private fun helper1() { }
private fun helper2() { }

// ============================================================
// DATA CLASSES
// ============================================================

data class Result(val value: String)
```

## Method Grouping Principles

- **Group related methods together** - don't organize alphabetically
- **Public API methods near the top** - most important first
- **Lifecycle methods in logical order** - onCreate → onStart → onResume → onPause → onStop → onDestroy
- **Group by feature/responsibility** - all map-related methods together, all network-related together
- **Use section comments** for files >200 lines to improve navigation

## File Size Guidelines

- **Target**: <300 lines per file
- **Warning**: >500 lines (consider splitting)
- **Maximum**: <600 lines (must split if exceeded)

### When to Split Files

Split when:

- File exceeds 600 lines
- Class has multiple unrelated responsibilities
- Testing becomes difficult due to complexity
- New developers struggle to understand the file

### How to Split

Use delegation or facade patterns:

```kotlin
// Before: 800-line EventProcessor
class EventProcessor {
    fun validateEvent() { }
    fun scheduleEvent() { }
    fun notifyParticipants() { }
    fun trackMetrics() { }
}

// After: Split into focused classes
class EventProcessor(
    private val validator: EventValidator,
    private val scheduler: EventScheduler,
    private val notifier: ParticipantNotifier,
    private val metrics: MetricsTracker
) {
    fun processEvent(event: WWWEvent) {
        validator.validate(event)
        scheduler.schedule(event)
        notifier.notify(event)
        metrics.track(event)
    }
}
```

## Import Organization

Run `./gradlew :shared:ktlintFormat` to organize imports automatically.

### Import Grouping

1. **Standard library** (kotlin.*, kotlinx.*)
2. **KMM/Compose** (androidx.*, org.jetbrains.compose.*)
3. **Project** (com.worldwidewaves.*)
4. **Platform** (platform.UIKit.*, platform.Foundation.*)

### Remove Unused Imports

```bash
./gradlew :shared:ktlintFormat
```

## Property Declaration

### Prefer val over var

```kotlin
// ✅ GOOD - immutable
val events: StateFlow<List<WWWEvent>>

// ❌ AVOID - mutable (unless necessary)
var currentEvent: WWWEvent?
```

### Lazy Initialization

```kotlin
// For expensive computations
private val expensiveResource by lazy {
    computeExpensiveResource()
}

// For iOS safety (Dispatchers.Main)
private val scope by lazy {
    CoroutineScope(Dispatchers.Main)
}
```

## Naming Conventions

### Classes

- **PascalCase**: `EventProcessor`, `WWWEvent`
- **Descriptive names**: Avoid abbreviations unless standard (WWW, GPS, UI)

### Functions

- **camelCase**: `loadEvents()`, `updatePosition()`
- **Verb phrases**: Functions do something (load, update, calculate, validate)

### Properties

- **camelCase**: `currentEvent`, `isLoading`
- **Boolean prefix**: `isLoading`, `hasPermission`, `canEdit`

### Constants

- **SCREAMING_SNAKE_CASE**: `MAX_RETRIES`, `DEFAULT_TIMEOUT`
- **In companion object** or **top-level object**:

```kotlin
companion object {
    private const val TAG = "EventProcessor"
    private const val MAX_RETRIES = 3
}
```

## Documentation

### Public API KDoc

```kotlin
/**
 * Loads events for the specified location and time range.
 *
 * @param location The geographic location to search
 * @param timeRange The time range to filter events
 * @return Flow of events matching the criteria
 * @throws NetworkException if network request fails
 */
fun loadEvents(
    location: LatLng,
    timeRange: TimeRange
): Flow<List<WWWEvent>>
```

### Complex Logic Comments

```kotlin
// Calculate min zoom to fit event bounds in viewport
// iOS uses 512px tiles (not 256px standard)
val minZoom = log2((screenHeight * 360.0) / (boundsHeight * 512.0))
```

## Examples

### ViewModel Structure

```kotlin
class EventViewModel(
    private val repository: EventRepository,
    private val clock: IClock
) : ViewModel() {

    companion object {
        private const val TAG = "EventViewModel"
    }

    // State
    private val _events = MutableStateFlow<List<WWWEvent>>(emptyList())
    val events: StateFlow<List<WWWEvent>> = _events.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Initialization
    init {
        loadEvents()
    }

    // Public API
    fun loadEvents() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = repository.fetchEvents()
                _events.value = result
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load events", throwable = e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refreshEvents() {
        loadEvents()
    }

    // Private helpers
    private fun filterPastEvents(events: List<WWWEvent>): List<WWWEvent> {
        val now = clock.now()
        return events.filter { it.endTime > now }
    }
}
```

### Repository Structure

```kotlin
class EventRepository(
    private val remoteDataSource: FirestoreDataSource,
    private val localDataSource: LocalEventsStore
) {

    companion object {
        private const val TAG = "EventRepository"
        private const val CACHE_DURATION = 5.minutes
    }

    private val mutex = Mutex()
    private var cachedEvents: List<WWWEvent>? = null
    private var cacheTime: Instant? = null

    // Public API
    suspend fun fetchEvents(): List<WWWEvent> = mutex.withLock {
        if (isCacheValid()) {
            Log.v(TAG, "Returning cached events")
            return cachedEvents!!
        }

        return fetchAndCache()
    }

    suspend fun refreshEvents(): List<WWWEvent> = mutex.withLock {
        invalidateCache()
        return fetchAndCache()
    }

    // Private helpers
    private fun isCacheValid(): Boolean {
        val cache = cachedEvents
        val time = cacheTime
        return cache != null && time != null &&
               (Clock.System.now() - time) < CACHE_DURATION
    }

    private suspend fun fetchAndCache(): List<WWWEvent> {
        val events = remoteDataSource.getEvents()
        cachedEvents = events
        cacheTime = Clock.System.now()
        localDataSource.saveEvents(events)
        return events
    }

    private fun invalidateCache() {
        cachedEvents = null
        cacheTime = null
    }
}
```

## Anti-Patterns to Avoid

### ❌ God Classes

```kotlin
// AVOID: 1000+ line class doing everything
class EventManager {
    fun validateEvent() { }
    fun saveEvent() { }
    fun loadEvent() { }
    fun scheduleNotification() { }
    fun sendNetworkRequest() { }
    fun updateUI() { }
    fun calculateDistance() { }
    // ... 50 more methods
}
```

### ❌ Scattered Responsibilities

```kotlin
// AVOID: Mixed concerns in one class
class EventScreen {
    fun renderUI() { }
    fun fetchFromNetwork() { }  // Should be in repository
    fun saveToDatabase() { }    // Should be in repository
    fun calculateDistance() { }  // Should be in domain
}
```

### ❌ Long Parameter Lists

```kotlin
// AVOID: Too many parameters
fun createEvent(
    title: String,
    description: String,
    startTime: Instant,
    endTime: Instant,
    location: LatLng,
    radius: Double,
    waveCount: Int
): WWWEvent

// PREFER: Use data class
data class EventCreateParams(
    val title: String,
    val description: String,
    val startTime: Instant,
    val endTime: Instant,
    val location: LatLng,
    val radius: Double,
    val waveCount: Int
)

fun createEvent(params: EventCreateParams): WWWEvent
```

## Reference

- **Kotlin Style Guide**: https://kotlinlang.org/docs/coding-conventions.html
- **ktlint**: `./gradlew :shared:ktlintFormat` for auto-formatting
- **File size limits**: Enforced by detekt (warning >500 lines)
