# Correlation Context for Distributed Tracing

## Overview

WorldWideWaves includes built-in support for distributed tracing using correlation IDs. This feature allows you to track operations across multiple components, coroutines, and layers of the application by automatically attaching a unique identifier to all log messages within a specific context.

## Architecture

The correlation context system is built on top of Kotlin coroutines and uses the `CoroutineContext` mechanism for propagation:

- **CorrelationContext**: Manages correlation IDs via coroutine context elements
- **Log utility**: Automatically includes correlation IDs in all log messages
- **Cross-platform**: Works seamlessly on both Android and iOS (KMM compatible)
- **Thread-safe**: Uses immutable context elements, no synchronization needed

## Basic Usage

### Automatic ID Generation

```kotlin
suspend fun loadEvent(eventId: String) = withCorrelation {
    Log.i("EventLoader", "Starting event load")
    // Logs: [CID-12345] Starting event load

    val event = repository.loadEvent(eventId)
    Log.i("EventLoader", "Event loaded: ${event.name}")
    // Logs: [CID-12345] Event loaded: Wave Event

    return event
}
```

### Custom Correlation IDs

Use meaningful IDs based on your domain (request IDs, event IDs, user IDs):

```kotlin
suspend fun processEvent(eventId: String) = withCorrelation("EVENT-$eventId") {
    Log.i("EventProcessor", "Processing event")
    // Logs: [EVENT-abc123] Processing event

    val waves = loadWaves(eventId)
    Log.i("EventProcessor", "Loaded ${waves.size} waves")
    // Logs: [EVENT-abc123] Loaded 3 waves
}
```

### Nested Operations

Correlation IDs automatically propagate to child coroutines:

```kotlin
suspend fun orchestrateEvent(eventId: String) = withCorrelation("EVENT-$eventId") {
    Log.i("Orchestrator", "Starting orchestration")
    // Logs: [EVENT-abc123] Starting orchestration

    // Child coroutine inherits parent's correlation ID
    launch {
        Log.i("Worker", "Processing wave 1")
        // Logs: [EVENT-abc123] Processing wave 1
    }

    launch {
        Log.i("Worker", "Processing wave 2")
        // Logs: [EVENT-abc123] Processing wave 2
    }
}
```

## Use Cases

### 1. API Request Tracking

Track a single API request through multiple layers:

```kotlin
suspend fun handleApiRequest(requestId: String, eventId: String) = withCorrelation(requestId) {
    Log.i("API", "Received request for event $eventId")

    val event = eventRepository.loadEvent(eventId)  // Inherits correlation
    val participants = participantRepository.loadParticipants(eventId)  // Inherits correlation

    Log.i("API", "Request completed successfully")
}
```

All logs from this request (across repositories, data sources, etc.) will include `[requestId]`.

### 2. Event Processing Pipeline

Track event processing from creation to wave execution:

```kotlin
suspend fun processNewEvent(event: WWWEvent) = withCorrelation("EVENT-${event.id}") {
    Log.i("Pipeline", "Event created: ${event.name}")

    validateEvent(event)  // Logs will include [EVENT-xxx]
    scheduleWaves(event)  // Logs will include [EVENT-xxx]
    notifyParticipants(event)  // Logs will include [EVENT-xxx]

    Log.i("Pipeline", "Event processing complete")
}
```

### 3. Background Job Tracking

Track long-running background jobs:

```kotlin
suspend fun syncEventsJob() = withCorrelation("SYNC-JOB-${Clock.System.now()}") {
    Log.i("SyncJob", "Starting sync job")

    val events = fetchRemoteEvents()
    events.forEach { event ->
        syncEvent(event)  // Each sync operation logged with job ID
    }

    Log.i("SyncJob", "Sync job completed")
}
```

### 4. User Session Tracking

Track all operations within a user session:

```kotlin
suspend fun startUserSession(userId: String) = withCorrelation("SESSION-$userId") {
    Log.i("Session", "User logged in")

    loadUserPreferences()
    loadFavoriteEvents()
    subscribeToNotifications()

    Log.i("Session", "Session initialized")
}
```

## Advanced Patterns

### Nested Correlation Contexts

When nesting correlation contexts, the inner context overrides the outer:

```kotlin
suspend fun outerOperation() = withCorrelation("OUTER-123") {
    Log.i("Outer", "Starting outer operation")
    // Logs: [OUTER-123] Starting outer operation

    withCorrelation("INNER-456") {
        Log.i("Inner", "Starting inner operation")
        // Logs: [INNER-456] Starting inner operation (overrides OUTER-123)
    }

    Log.i("Outer", "Continuing outer operation")
    // Logs: [OUTER-123] Continuing outer operation (reverts to OUTER-123)
}
```

### Correlation with Async Operations

Correlation IDs propagate through `async`/`await`:

```kotlin
suspend fun parallelProcessing() = withCorrelation("PARALLEL-789") {
    val results = listOf(
        async { processTask1() },  // Inherits PARALLEL-789
        async { processTask2() },  // Inherits PARALLEL-789
        async { processTask3() }   // Inherits PARALLEL-789
    ).awaitAll()

    // All logs from processTask1/2/3 will include [PARALLEL-789]
}
```

### Retrieving Current Correlation ID

Get the current correlation ID for non-logging purposes:

```kotlin
suspend fun getCurrentContext() {
    val correlationId = CorrelationContext.getCurrentId()
    if (correlationId != null) {
        // Include in error reports, analytics, etc.
        sendToAnalytics("operation_failed", mapOf("correlation_id" to correlationId))
    }
}
```

## Best Practices

### 1. Use Meaningful IDs

Choose correlation IDs that help identify the operation:

```kotlin
// ✅ GOOD: Meaningful, domain-specific ID
withCorrelation("EVENT-${event.id}")

// ✅ GOOD: Request ID from external system
withCorrelation(requestHeaders["X-Request-ID"])

// ❌ BAD: Generic or meaningless ID
withCorrelation("operation")
```

### 2. Correlation ID Naming Conventions

Use consistent prefixes for different operation types:

- `EVENT-xxx`: Event-related operations
- `WAVE-xxx`: Wave execution tracking
- `API-xxx`: API request handling
- `SYNC-xxx`: Background sync jobs
- `SESSION-xxx`: User session tracking
- `CID-xxxxx`: Auto-generated IDs (default)

### 3. Keep Correlation Scopes Focused

Don't make correlation scopes too broad:

```kotlin
// ✅ GOOD: Focused scope for specific operation
suspend fun loadEvent(id: String) = withCorrelation("LOAD-$id") {
    repository.loadEvent(id)
}

// ❌ BAD: Entire ViewModel lifecycle (too broad)
class EventViewModel : ViewModel() {
    init {
        runBlocking {
            withCorrelation("VIEWMODEL") {  // Don't do this
                // All ViewModel operations forever
            }
        }
    }
}
```

### 4. Avoid Blocking in Correlation Context

Never use `runBlocking` inside `withCorrelation` unnecessarily:

```kotlin
// ✅ GOOD: Pure suspend functions
suspend fun loadData() = withCorrelation("LOAD") {
    repository.loadData()
}

// ❌ BAD: Blocking inside correlation context
suspend fun loadDataBlocking() = withCorrelation("LOAD") {
    runBlocking {  // Unnecessary blocking
        repository.loadData()
    }
}
```

### 5. Log Entry and Exit Points

Always log at the beginning and end of correlated operations:

```kotlin
suspend fun processEvent(event: WWWEvent) = withCorrelation("EVENT-${event.id}") {
    Log.i("Processor", "Starting event processing")

    try {
        // Processing logic
        Log.i("Processor", "Event processing complete")
    } catch (e: Exception) {
        Log.e("Processor", "Event processing failed", e)
        throw e
    }
}
```

## Performance Considerations

### Overhead

- **Minimal overhead**: Correlation context uses immutable coroutine context elements
- **No synchronization**: Thread-safe without locks (uses coroutine context)
- **Lazy evaluation**: Correlation ID only accessed when logging occurs
- **Memory efficient**: Single String per context, automatically cleaned up

### When to Use

Correlation context is lightweight enough to use liberally:

- ✅ API request handling
- ✅ Background job processing
- ✅ Event processing pipelines
- ✅ User interaction flows
- ✅ Repository operations
- ✅ Network requests

### When Not to Use

Avoid in extremely high-frequency code paths:

- ❌ Inside tight loops (>1000 iterations/sec)
- ❌ Per-frame rendering operations
- ❌ Audio/video processing callbacks
- ❌ GPS coordinate updates (already deduplicated)

## Troubleshooting

### Correlation ID Not Appearing in Logs

**Symptom**: Logs don't include correlation ID prefix

**Causes**:

1. Not using `withCorrelation` wrapper
2. Logging from non-suspend function outside correlation scope
3. Using plain `Napier.i()` instead of `Log.i()`

**Solution**:

```kotlin
// ❌ WRONG: Using Napier directly
suspend fun myFunction() = withCorrelation("TEST") {
    Napier.i("MyTag", "Message")  // No correlation ID
}

// ✅ CORRECT: Using Log wrapper
suspend fun myFunction() = withCorrelation("TEST") {
    Log.i("MyTag", "Message")  // Includes [TEST] prefix
}
```

### Correlation ID Lost in Child Coroutines

**Symptom**: Child coroutines don't inherit correlation ID

**Cause**: Using `GlobalScope` or explicit `Dispatchers.Default` context

**Solution**:

```kotlin
// ❌ WRONG: GlobalScope doesn't inherit context
withCorrelation("PARENT") {
    GlobalScope.launch {
        Log.i("Child", "Message")  // No correlation ID
    }
}

// ✅ CORRECT: Regular launch inherits context
withCorrelation("PARENT") {
    launch {
        Log.i("Child", "Message")  // Includes [PARENT]
    }
}
```

### Performance Impact on High-Frequency Logging

**Symptom**: Noticeable performance degradation with correlation IDs

**Cause**: Using correlation context in very high-frequency operations

**Solution**:

1. Use `WWWGlobals.LogConfig.ENABLE_VERBOSE_LOGGING` to disable verbose logs in production
2. Move correlation context to higher-level operation (not per-iteration)
3. Use performance logging flag for high-frequency debug logs

## Testing

### Unit Testing with Correlation Context

```kotlin
@Test
fun testWithCorrelation() = runTest {
    withCorrelation("TEST-123") {
        val id = CorrelationContext.getCurrentId()
        assertEquals("TEST-123", id)

        // Test your logic that uses correlation
        myFunction()
    }
}
```

### Verifying Correlation Propagation

```kotlin
@Test
fun testCorrelationPropagation() = runTest {
    withCorrelation("PARENT") {
        launch {
            assertEquals("PARENT", CorrelationContext.getCurrentId())
        }
    }
}
```

## Implementation Details

### How It Works

1. **Context Element**: `CorrelationIdElement` extends `AbstractCoroutineContextElement`
2. **Propagation**: Kotlin coroutines automatically propagate context to children
3. **Retrieval**: `getCurrentId()` accesses `coroutineContext[CorrelationIdElement]`
4. **Logging**: `Log` wrapper uses `runBlocking` to access suspend context

### iOS Compatibility

The correlation context is fully compatible with iOS/Native:

- No `ThreadLocal` usage (not available on iOS)
- Uses coroutine context (platform-agnostic)
- Works with Kotlin/Native memory model
- No iOS-specific deadlock risks

### Thread Safety

Correlation context is inherently thread-safe:

- Coroutine context elements are immutable
- No shared mutable state
- No synchronization primitives needed
- Safe for concurrent coroutines

## Migration Guide

### From Manual Request ID Passing

**Before**:

```kotlin
fun loadEvent(eventId: String, requestId: String) {
    Log.i("Loader", "[$requestId] Loading event")
    repository.loadEvent(eventId, requestId)
}
```

**After**:

```kotlin
suspend fun loadEvent(eventId: String) = withCorrelation("EVENT-$eventId") {
    Log.i("Loader", "Loading event")  // Correlation ID added automatically
    repository.loadEvent(eventId)
}
```

### From Global Tracing Variables

**Before**:

```kotlin
object TracingContext {
    var currentRequestId: String? = null  // Thread-unsafe!
}

fun processRequest(id: String) {
    TracingContext.currentRequestId = id
    Log.i("API", "[${TracingContext.currentRequestId}] Processing")
}
```

**After**:

```kotlin
suspend fun processRequest(id: String) = withCorrelation(id) {
    Log.i("API", "Processing")  // Thread-safe, scoped correlation
}
```

## See Also

- [Log Utility Documentation](../shared/src/commonMain/kotlin/com/worldwidewaves/shared/utils/Log.kt)
- [CorrelationContext Source](../shared/src/commonMain/kotlin/com/worldwidewaves/shared/utils/CorrelationContext.kt)
- [Kotlin Coroutines Context Documentation](https://kotlinlang.org/docs/coroutine-context-and-dispatchers.html)

---

**Version**: 1.0
**Maintainer**: WorldWideWaves Development Team
