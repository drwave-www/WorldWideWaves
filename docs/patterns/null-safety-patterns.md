# Null Safety Patterns

> **Purpose**: Production-ready null handling patterns for WorldWideWaves

## Force Unwrap Prohibition

**Rule**: NEVER use `!!` in production code.

### ❌ UNSAFE Patterns

```kotlin
// Crashes on null
val position = platform.getSimulation()!!.getUserPosition()

// No context on failure
val waveDefinition = linear ?: deep ?: linearSplit!!
```

### ✅ SAFE Patterns

#### Pattern 1: Safe Calls with Elvis

```kotlin
// Provide fallback value
val position = platform.getSimulation()?.getUserPosition() ?: Position.UNKNOWN

// Chain safely
val eventTitle = event?.details?.title ?: "Unknown Event"
```

#### Pattern 2: requireNotNull with Context

```kotlin
// Fail fast with descriptive error
val waveDefinition = requireNotNull(linear ?: deep ?: linearSplit) {
    "Wave definition must exist after validation"
}

// Fail fast in critical paths
val database = requireNotNull(dbInstance) {
    "Database must be initialized before access"
}
```

#### Pattern 3: Early Returns

```kotlin
fun processEvent(event: WWWEvent?) {
    val safeEvent = event ?: run {
        Log.w(TAG, "Event is null, skipping processing")
        return
    }

    // Continue with non-null event
    processValidEvent(safeEvent)
}
```

#### Pattern 4: Let for Scoped Null Checks

```kotlin
// Execute block only if non-null
simulation?.let { sim ->
    val position = sim.getUserPosition()
    updateMapPosition(position)
}

// Chain multiple nullable operations
event?.location?.let { location ->
    mapController.centerOn(location)
}
```

## Swift Force Unwrap Guidelines

### ❌ Avoid Force Unwraps

```swift
// Crashes if nil
let camera = mapView.camera!
let bounds = constraintBounds!.ne
```

### ✅ Safe Alternatives

```swift
// Guard with early return
guard let camera = mapView.camera else {
    NSLog("❌ Camera is nil")
    return
}

// Optional chaining
if let ne = constraintBounds?.ne {
    validateBounds(ne)
}

// Nil coalescing
let zoom = currentZoom ?? defaultZoom
```

## Common Null-Prone Areas

| Location | Risk | Pattern |
|----------|------|---------|
| Event loading | Firestore returns null | `requireNotNull` with context |
| GPS position | No location permission | Elvis with fallback position |
| DI resolution | Missing module | Constructor injection |
| Map initialization | Async style loading | Queue pending operations |
| User settings | First launch | Provide defaults |

## Testing Null Handling

```kotlin
@Test
fun `handles null event gracefully`() {
    val result = repository.loadEvent(id = "nonexistent")

    // Assert null handling, not crash
    assertNull(result)
    // OR with sealed class
    assertTrue(result is Result.NotFound)
}

@Test
fun `requires non-null wave definition`() {
    val exception = assertThrows<IllegalArgumentException> {
        createWave(linear = null, deep = null, linearSplit = null)
    }

    assertEquals(
        "Wave definition must exist after validation",
        exception.message
    )
}
```

## Migration from !! to Safe Patterns

When refactoring code with `!!` operators:

1. **Understand the guarantee**: Why was the developer confident it's non-null?
2. **Add validation**: Make the guarantee explicit with `requireNotNull`
3. **Provide fallback**: If nullable is acceptable, use `?:` with sensible default
4. **Document reasoning**: Add comment explaining why non-null is expected

```kotlin
// Before: Implicit assumption
val wave = event.waves[0]!!

// After: Explicit validation
val wave = requireNotNull(event.waves.firstOrNull()) {
    "Event must have at least one wave (validated in WWWEvent constructor)"
}
```

## Reference

- **Detekt Rule**: `detekt.yml` - `ForbiddenMethodCall` for `!!` operator
- **Pre-commit Hook**: Scans for `!!` in changed files
