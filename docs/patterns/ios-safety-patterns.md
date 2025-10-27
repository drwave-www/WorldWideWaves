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

## Reference

- **Tracking**: docs/ios/ios-violation-tracker.md (historical violations)
- **Verification**: ./scripts/verify-ios-safety.sh
- **Status**: All 11 critical violations fixed (October 2025)
