# iOS Camera Command Execution Flow

**Date**: October 8, 2025
**Related**: [iOS Camera Command Queue Fix](./iOS_CAMERA_COMMAND_QUEUE_FIX.md)

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│ Kotlin (AbstractEventMap / MapBoundsEnforcer)               │
│                                                              │
│  setBoundsForCameraTarget(bounds)  →  SetConstraintBounds   │
│  setMinZoomPreference(minZoom)     →  SetMinZoom            │
│  setMaxZoomPreference(maxZoom)     →  SetMaxZoom            │
│  animateCameraToBounds(bounds)     →  AnimateToBounds       │
└──────────────────────┬───────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│ MapWrapperRegistry (Kotlin/Native Singleton)                │
│                                                              │
│  ┌─────────────────────────────────────────────────────┐   │
│  │ Configuration Queue (FIFO)                           │   │
│  │  eventId → [SetConstraintBounds, SetMinZoom, ...]    │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                              │
│  ┌─────────────────────────────────────────────────────┐   │
│  │ Animation Slot (Latest Wins)                         │   │
│  │  eventId → AnimateToBounds (overwrites previous)     │   │
│  └─────────────────────────────────────────────────────┘   │
└──────────────────────┬───────────────────────────────────────┘
                       │
                       ▼ (Immediate callback dispatch)
┌─────────────────────────────────────────────────────────────┐
│ IOSMapBridge.executePendingCameraCommand() (Swift)          │
│                                                              │
│  WHILE hasPendingCameraCommand() AND count < 10:            │
│    1. getPendingCameraCommand() → Returns config OR anim    │
│    2. executeCommand()                                       │
│    3. clearPendingCameraCommand()                           │
│    4. IF animation → BREAK (stop batching)                  │
│    5. IF config → CONTINUE (batch next)                     │
│                                                              │
│  Result: All config commands execute, then 1 animation      │
└──────────────────────┬───────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│ MapLibreViewWrapper (Swift)                                 │
│                                                              │
│  setBoundsForCameraTarget() → mapView.cameraBoundsOptions   │
│  setMinZoom()               → mapView.minimumZoomLevel      │
│  setMaxZoom()               → mapView.maximumZoomLevel      │
│  animateCameraToBounds()    → mapView.setCamera(animated)   │
└─────────────────────────────────────────────────────────────┘
```

---

## Command Flow Examples

### Example 1: Initial Map Setup

**Kotlin Code**:
```kotlin
// MapBoundsEnforcer.applyConstraints()
mapLibreAdapter.setBoundsForCameraTarget(paddedBounds)  // T=0ms
mapLibreAdapter.setMinZoomPreference(minZoom)           // T=1ms
// MapWrapperRegistry triggers immediate callback        // T=2ms
```

**Registry State** (T=2ms):
```kotlin
pendingConfigCommands["event1"] = [
    SetConstraintBounds(bounds),
    SetMinZoom(10.0)
]
pendingAnimationCommands["event1"] = null
```

**Swift Execution** (T=3ms, one callback):
```swift
// Iteration 1:
getPendingCameraCommand() → SetConstraintBounds
executeCommand() → wrapper.setBoundsForCameraTarget()
clearPendingCameraCommand() → Remove from queue
isConfigCommand = true → CONTINUE

// Iteration 2:
getPendingCameraCommand() → SetMinZoom
executeCommand() → wrapper.setMinZoom()
clearPendingCameraCommand() → Remove from queue
isConfigCommand = true → CONTINUE

// Iteration 3:
hasPendingCameraCommand() → false → EXIT LOOP

// Result: Both commands executed in ~1ms
```

---

### Example 2: Animation with Post-Configuration

**Kotlin Code**:
```kotlin
// AbstractEventMap.centerCameraOnEventBounds()
animateCameraToBounds(bounds, callback: {
    constraintManager.applyConstraints()       // → SetConstraintBounds
    setMinZoomPreference(currentZoom)         // → SetMinZoom
    setMaxZoomPreference(maxZoom)             // → SetMaxZoom
})
```

**Registry State** (during animation):
```kotlin
pendingConfigCommands["event1"] = []
pendingAnimationCommands["event1"] = AnimateToBounds(bounds, callback)
```

**Swift Execution #1** (animation trigger):
```swift
// Iteration 1:
getPendingCameraCommand() → AnimateToBounds
executeCommand() → wrapper.animateCameraToBounds() [async!]
clearPendingCameraCommand() → Clear animation slot
isConfigCommand = false → BREAK

// Result: Animation starts (async), callback not invoked yet
```

**Registry State** (after animation completes, callback fires):
```kotlin
pendingConfigCommands["event1"] = [
    SetConstraintBounds(bounds),
    SetMinZoom(12.0),
    SetMaxZoom(16.0)
]
pendingAnimationCommands["event1"] = null
```

**Swift Execution #2** (callback trigger):
```swift
// Iteration 1:
getPendingCameraCommand() → SetConstraintBounds
executeCommand() → wrapper.setBoundsForCameraTarget()
clearPendingCameraCommand() → Remove from queue
isConfigCommand = true → CONTINUE

// Iteration 2:
getPendingCameraCommand() → SetMinZoom
executeCommand() → wrapper.setMinZoom()
clearPendingCameraCommand() → Remove from queue
isConfigCommand = true → CONTINUE

// Iteration 3:
getPendingCameraCommand() → SetMaxZoom
executeCommand() → wrapper.setMaxZoom()
clearPendingCameraCommand() → Remove from queue
isConfigCommand = true → CONTINUE

// Iteration 4:
hasPendingCameraCommand() → false → EXIT LOOP

// Result: All 3 constraints applied after animation!
```

---

### Example 3: Rapid Animation Updates

**Kotlin Code**:
```kotlin
// User rapidly pans/zooms
animateCameraToBounds(bounds1)  // T=0ms
animateCameraToBounds(bounds2)  // T=50ms
animateCameraToBounds(bounds3)  // T=100ms
```

**Registry State** (T=100ms):
```kotlin
pendingConfigCommands["event1"] = []
pendingAnimationCommands["event1"] = AnimateToBounds(bounds3)
// bounds1 and bounds2 were OVERWRITTEN (expected behavior)
```

**Swift Execution** (T=101ms):
```swift
// Iteration 1:
getPendingCameraCommand() → AnimateToBounds(bounds3)
executeCommand() → wrapper.animateCameraToBounds(bounds3)
clearPendingCameraCommand() → Clear animation slot
isConfigCommand = false → BREAK

// Result: Only latest animation executes (bounds3)
```

---

## Priority Execution Logic

### getPendingCameraCommand() Priority

```kotlin
fun getPendingCameraCommand(eventId: String): CameraCommand? {
    // PRIORITY 1: Configuration commands (FIFO)
    val configQueue = pendingConfigCommands[eventId]
    if (configQueue?.isNotEmpty() == true) {
        return configQueue.first()  // Oldest config command
    }

    // PRIORITY 2: Animation command (latest)
    return pendingAnimationCommands[eventId]
}
```

**Why This Priority?**
1. **Configuration before Animation**: Map constraints must be set BEFORE camera moves
2. **FIFO for Config**: Order matters (e.g., bounds → minZoom → maxZoom)
3. **Latest for Animation**: Only most recent camera target matters

---

## Batching vs. One-at-a-Time

### Configuration Commands (Batched)
```swift
// Swift executes ALL config commands in one callback:
while hasPendingCameraCommand() && executedCount < 10 {
    let command = getPendingCameraCommand()
    if command is SetConstraintBounds || SetMinZoom || SetMaxZoom {
        execute(command)
        clear(command)
        executedCount += 1
        // CONTINUE LOOP → Batch next config command
    }
}
```

**Batching Benefits**:
- Reduces Kotlin-Swift callback overhead
- Ensures all constraints apply together (atomic)
- Fast (synchronous operations, no animation)

### Animation Commands (One-at-a-Time)
```swift
// Swift executes ONE animation per callback:
while hasPendingCameraCommand() && executedCount < 10 {
    let command = getPendingCameraCommand()
    if command is AnimateToPosition || AnimateToBounds || MoveToBounds {
        execute(command)  // Async operation!
        clear(command)
        executedCount += 1
        // BREAK → Stop batching, wait for animation to complete
        break
    }
}
```

**One-at-a-Time Rationale**:
- Animations are **async** (completion callback)
- Batching would queue multiple animations (wrong UX)
- Latest animation should cancel previous (expected behavior)

---

## Safety Mechanisms

### 1. Infinite Loop Prevention
```swift
let maxBatchSize = 10  // Safety limit

while hasPendingCameraCommand() && executedCount < maxBatchSize {
    // ... execute commands ...
}

// If limit hit, log warning (indicates queue overflow bug)
```

### 2. Execution Failure Handling
```swift
let success = executeCommand(command)
if success {
    clearPendingCameraCommand()
    // Continue or break based on command type
} else {
    // BREAK → Retry later
    break
}
```

### 3. Main Thread Enforcement
```kotlin
// MapWrapperRegistry.requestImmediateCameraExecution()
platform.darwin.dispatch_async(platform.darwin.dispatch_get_main_queue()) {
    callback.invoke()  // Ensures Swift execution on main thread
}
```

---

## Command Lifecycle

### Configuration Command Lifecycle
1. **Storage**: `setPendingCameraCommand()` → Add to queue
2. **Trigger**: `requestImmediateCameraExecution()` → Dispatch callback
3. **Retrieval**: `getPendingCameraCommand()` → Return first in queue
4. **Execution**: `executeCommand()` → Synchronous MapLibre setter
5. **Removal**: `clearPendingCameraCommand()` → Remove from queue (FIFO)
6. **Repeat**: Loop continues until queue empty

### Animation Command Lifecycle
1. **Storage**: `setPendingCameraCommand()` → Overwrite animation slot
2. **Trigger**: `requestImmediateCameraExecution()` → Dispatch callback
3. **Retrieval**: `getPendingCameraCommand()` → Return animation (if no config)
4. **Execution**: `executeCommand()` → Async MapLibre animation
5. **Removal**: `clearPendingCameraCommand()` → Clear animation slot
6. **Break**: Loop stops (one animation at a time)

---

## Performance Characteristics

### Time Complexity
- **Storage**: O(1) for animation, O(1) amortized for config queue
- **Retrieval**: O(1) (check config queue first, then animation slot)
- **Removal**: O(N) for config queue (shift elements), O(1) for animation
- **Batch Execution**: O(N) where N = config queue size (max 10)

### Space Complexity
- **Per Event**: O(C + 1) where C = config commands queued, 1 = animation slot
- **Typical**: O(3 + 1) = O(4) commands per event (bounds + minZoom + maxZoom + 1 animation)

### Callback Overhead
- **Before Fix**: 1 callback per command (N callbacks for N commands)
- **After Fix**: 1 callback for N config commands + 1 callback for animation
- **Reduction**: From N+1 to 2 callbacks (typical case: 4 → 2, 50% reduction)

---

## Edge Cases

### Edge Case 1: Empty Queue
```kotlin
hasPendingCameraCommand("event1") → false
getPendingCameraCommand("event1") → null
```
**Handling**: Swift silently returns (no error)

### Edge Case 2: Config Queue Overflow (>10)
```swift
executedCount = 10 → Exit loop
// Remaining commands stay in queue for next callback
```
**Handling**: Log warning, retry on next callback trigger

### Edge Case 3: Command Execution Failure
```swift
let success = executeCommand(command)
if !success {
    // Command stays in queue (not cleared)
    break  // Retry later
}
```
**Handling**: Preserve failed command, retry on next callback

### Edge Case 4: Animation Interrupted by New Animation
```kotlin
animateCameraToBounds(bounds1)  // Stored in slot
animateCameraToBounds(bounds2)  // OVERWRITES bounds1
```
**Handling**: bounds1 never executes (expected behavior, latest wins)

---

## Testing Strategy

### Unit Tests (Kotlin)
```kotlin
@Test
fun testConfigCommandsQueue() {
    setPendingCameraCommand("event1", SetConstraintBounds(bounds))
    setPendingCameraCommand("event1", SetMinZoom(10.0))
    setPendingCameraCommand("event1", SetMaxZoom(16.0))

    // All 3 commands in queue
    assertEquals(SetConstraintBounds, getPendingCameraCommand("event1"))
    clearPendingCameraCommand("event1")

    assertEquals(SetMinZoom, getPendingCameraCommand("event1"))
    clearPendingCameraCommand("event1")

    assertEquals(SetMaxZoom, getPendingCameraCommand("event1"))
    clearPendingCameraCommand("event1")

    assertNull(getPendingCameraCommand("event1"))
}

@Test
fun testAnimationCommandsOverwrite() {
    setPendingCameraCommand("event1", AnimateToPosition(pos1))
    setPendingCameraCommand("event1", AnimateToPosition(pos2))

    // Only latest animation
    assertEquals(pos2, getPendingCameraCommand("event1").position)
}
```

### Integration Tests (Swift)
```swift
func testBatchExecution() {
    // Store 3 config commands
    registry.setPendingCameraCommand("event1", SetConstraintBounds(...))
    registry.setPendingCameraCommand("event1", SetMinZoom(10.0))
    registry.setPendingCameraCommand("event1", SetMaxZoom(16.0))

    // Execute once
    IOSMapBridge.executePendingCameraCommand(eventId: "event1")

    // All 3 should be executed
    XCTAssertFalse(registry.hasPendingCameraCommand(eventId: "event1"))
}
```

### Manual Tests
1. Open event detail screen → Verify constraints apply
2. Rapid zoom in/out → Verify no constraint violations
3. Check logs → Verify all SetConstraintBounds execute (not 0)
4. Monitor maxBatchSize → Verify no overflow warnings

---

## Monitoring & Debugging

### Log Patterns

**Successful Config Batch**:
```
📸 Storing camera command for event: abc123 → SetConstraintBounds
Config command queued, queue size=1
🚀 Immediate render callback triggered for: abc123
📸 Executing camera command for event: abc123, type: SetConstraintBounds
✅ Camera command executed and cleared for event: abc123
📸 Executing camera command for event: abc123, type: SetMinZoom
✅ Camera command executed and cleared for event: abc123
Batch execution complete: 2 command(s) executed
```

**Animation Execution**:
```
📸 Storing camera command for event: abc123 → AnimateToBounds(padding=50)
Animation command stored (overwrites previous animation)
📸 Executing camera command for event: abc123, type: AnimateToBounds
Animation command executed, stopping batch execution
✅ Camera command executed and cleared for event: abc123
Batch execution complete: 1 command(s) executed
```

**Queue Overflow Warning** (should never happen):
```
⚠️ Camera command batch execution hit limit (10 commands)
Remaining commands will execute on next callback
```

### Debug Inspection

**Check Queue State**:
```kotlin
// In MapWrapperRegistry
fun debugPrintQueueState(eventId: String) {
    val configQueue = pendingConfigCommands[eventId]
    val animationCmd = pendingAnimationCommands[eventId]
    Log.d(TAG, "Config queue size: ${configQueue?.size ?: 0}")
    Log.d(TAG, "Animation slot: ${animationCmd?.let { it::class.simpleName } ?: "empty"}")
}
```

**Check Execution Count**:
```swift
// In IOSMapBridge
var totalExecutionCount = 0
var configExecutionCount = 0
var animationExecutionCount = 0

// Increment in executeCommand() based on command type
// Log periodically for monitoring
```

---

## Related Documentation

- [iOS Camera Command Queue Fix](./iOS_CAMERA_COMMAND_QUEUE_FIX.md) - Full implementation details
- [iOS Map Implementation Status](../iOS_MAP_IMPLEMENTATION_STATUS.md) - Overall map feature status
- [Map Architecture Analysis](../MAP_ARCHITECTURE_ANALYSIS.md) - System architecture
- [iOS Success State](./iOS_SUCCESS_STATE.md) - Verification criteria

---

**Last Updated**: October 8, 2025
**Maintainer**: WorldWideWaves Team
