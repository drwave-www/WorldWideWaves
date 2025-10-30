# iOS Safety Patterns

> **Purpose**: Reference guide for safe iOS Kotlin/Native patterns to prevent deadlocks and crashes

## Critical Deadlock Patterns

### ❌ NEVER: Object inside @Composable

```kotlin
// DEADLOCKS on iOS!
@Composable
fun MyScreen() {
    val deps = object : KoinComponent {
        val clock by inject()  // Main thread freeze
    }
}
```

**Why**: Creating objects during Compose composition freezes main thread while Koin initializes on worker thread.

### ✅ ALWAYS: IOSSafeDI Singleton

```kotlin
// File-level singleton (shared/src/commonMain/.../ui/utils/IOSSafeDI.kt)
object IOSSafeDI : KoinComponent {
    val platform: WWWPlatform by inject()
    val clock: IClock by inject()
}

// Safe accessor functions
fun getIOSSafeClock(): IClock = IOSSafeDI.platform.clock

// Usage
@Composable
fun MyScreen() {
    val clock = getIOSSafeClock()  // Safe
}
```

### ❌ NEVER: Coroutine Launch in init{}

```kotlin
// DEADLOCKS on iOS!
class EventViewModel {
    init {
        CoroutineScope.launch {
            loadEvents()
        }
    }
}
```

### ✅ ALWAYS: Suspend Initialization

```kotlin
class EventViewModel {
    suspend fun initialize() {
        loadEvents()
    }
}

@Composable
fun EventScreen() {
    val viewModel = remember { EventViewModel() }
    LaunchedEffect(Unit) {
        viewModel.initialize()
    }
}
```

### ❌ NEVER: DI Access in init{}

```kotlin
// DEADLOCKS on iOS!
class EventRepository {
    init {
        val db = get<Database>()
    }
}
```

### ✅ ALWAYS: Constructor Injection

```kotlin
class EventRepository(
    private val db: Database  // Injected via Koin
)
```

### ❌ NEVER: Dispatchers.Main in Properties

```kotlin
// DEADLOCKS on iOS during construction!
class MyViewModel {
    private val scope = CoroutineScope(Dispatchers.Main)
}
```

### ✅ ALWAYS: Lazy Initialization

```kotlin
class MyViewModel {
    private val scope by lazy {
        CoroutineScope(Dispatchers.Main)
    }
}
```

## Property Initialization vs init{} Blocks

### Critical Distinction

```kotlin
// ❌ RISKY on iOS - accessed during construction
private val scope = CoroutineScope(Dispatchers.Main)

// ✅ SAFE - lazy evaluation after object fully constructed
private val scope by lazy { CoroutineScope(Dispatchers.Main) }

// ✅ SAFE - late initialization in suspend function
private lateinit var scope: CoroutineScope
suspend fun initialize() {
    scope = CoroutineScope(Dispatchers.Main)
}
```

## Thread Safety Patterns

### Mutable Shared State

```kotlin
// ❌ UNSAFE - race conditions
object MapDownloadGate {
    private val allowed = mutableSetOf<String>()
    fun allow(tag: String) { allowed += tag }
}

// ✅ SAFE - explicit synchronization
object MapDownloadGate {
    private val mutex = Mutex()
    private val allowed = mutableSetOf<String>()

    suspend fun allow(tag: String) {
        mutex.withLock { allowed += tag }
    }
}
```

## Cinterop Memory Safety Patterns

> **Priority**: CRITICAL | **Applies to**: iOS platform code using Foundation/CoreLocation/POSIX APIs

### Pattern: Memory Pinning for C Interop

**Problem**: Kotlin GC can move objects; C expects stable memory addresses.

**Solution**: Use `usePinned { }` to pin ByteArray before passing to C APIs.

```kotlin
// ✅ CORRECT
bytes.usePinned { pinned ->
    NSData.create(bytes = pinned.addressOf(0), length = bytes.size.toULong())
        .writeToFile(fullPath, atomically = true)
}

// ❌ WRONG: No pinning
val nsData = NSData.create(bytes = bytes[0], length = bytes.size.toULong())  // CRASH!
```

**Used in**: `PlatformCache.ios.kt`, `Typography.ios.kt`

### Pattern: useContents for Struct Access

**Problem**: iOS structs (CLLocationCoordinate2D, CGRect) are value types in managed memory.

**Solution**: Use `useContents { }` to safely access struct fields.

```kotlin
// ✅ CORRECT
location.coordinate.useContents {
    val position = Position(
        lat = latitude,
        lng = longitude
    )
}

// ❌ WRONG: Direct access
val lat = location.coordinate.latitude  // UNDEFINED BEHAVIOR!
```

**Used in**: `IosLocationProvider.kt`

### Pattern: addressOf Pointer Safety

**Problem**: Pointers are only valid within pinned scope.

**Solution**: Complete all C operations inside `usePinned { }` block.

```kotlin
// ✅ CORRECT: Pointer used within scope
bytes.usePinned { pinned ->
    val pointer = pinned.addressOf(0)
    fread(pointer, 1u, size.toULong(), file)  // Safe
}

// ❌ WRONG: Escaping pointer
val unsafePointer = bytes.usePinned { it.addressOf(0) }
// Pointer is now INVALID!
```

**Used in**: `Typography.ios.kt` (font loading)

### Verification

```bash
./scripts/dev/verification/verify-ios-safety.sh  # Checks 8-11 validate cinterop
```

**Related Documentation**:
- [Cinterop Memory Safety Patterns](../ios/cinterop-memory-safety-patterns.md) - Complete guide
- [Platform API Usage Guide](../ios/platform-api-usage-guide.md) - Threading requirements

## Kotlin-Swift Exception Handling

```kotlin
// Kotlin side - declare throws
@Throws(Throwable::class)
fun makeMainViewController(): UIViewController {
    // Implementation
}

// Swift side - proper error handling
do {
    let vc = try RootControllerKt.makeMainViewController()
    self.window?.rootViewController = vc
} catch let e as NSError {
    NSLog("❌ Error: \(e.localizedDescription)")
}
```

## Verification Commands

```bash
# Run before EVERY commit touching shared code
./scripts/verify-ios-safety.sh

# Manual checks
# 1. Find Composable-scoped KoinComponent (should be ZERO)
rg -B10 "object.*KoinComponent" shared/src/commonMain --type kotlin \
  | rg "@Composable" -A10 | rg "object.*KoinComponent"

# 2. Find init{} coroutine launches (should be ZERO)
rg -n -A 5 "init\s*\{" shared/src/commonMain --type kotlin \
  | rg "launch\{|async\{|scope\."

# 3. Find init{} DI access (should be ZERO)
rg -n -A 3 "init\s*\{" shared/src/commonMain --type kotlin \
  | rg "get\(\)|inject\(\)" | rg -v "// iOS FIX"
```

## Common Violations and Fixes

| Violation | File Pattern | Fix |
|-----------|--------------|-----|
| Object in Composable | `@Composable` with nested `object : KoinComponent` | Use IOSSafeDI singleton |
| Init DI access | `init { get<T>() }` | Constructor injection |
| Init coroutine | `init { scope.launch }` | Suspend initialize() |
| Property dispatcher | `val scope = CoroutineScope(Main)` | Lazy initialization |
| Unpinned ByteArray | `NSData.create(bytes = bytes[0], ...)` | Use `bytes.usePinned { }` |
| Direct struct access | `location.coordinate.latitude` | Use `coordinate.useContents { }` |
| Escaping pointer | `val ptr = usePinned { addressOf(0) }` | Keep all C ops inside pinned block |

## Reference

- **Tracking**: docs/ios/ios-violation-tracker.md (historical violations)
- **Verification**: ./scripts/dev/verification/verify-ios-safety.sh
- **Cinterop Patterns**: docs/ios/cinterop-memory-safety-patterns.md (complete guide)
- **Platform APIs**: docs/ios/platform-api-usage-guide.md (threading requirements)
- **Status**: All 11 critical violations fixed (October 2025)
