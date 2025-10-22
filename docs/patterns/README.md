# WorldWideWaves Patterns Documentation

> **Purpose**: Central index for all architectural and implementation patterns in WorldWideWaves
> **Status**: Production-Ready (October 2025)
> **Audience**: All developers working on WorldWideWaves codebase

---

## Overview

This directory contains proven implementation patterns extracted from the WorldWideWaves codebase. These patterns ensure:

- **Cross-platform compatibility**: Android + iOS (Kotlin Multiplatform)
- **Production stability**: Patterns prevent crashes, deadlocks, and memory leaks
- **Code quality**: Consistent, maintainable, testable implementations
- **Developer productivity**: Clear guidance reduces iteration cycles

**What You'll Find**:
- **Critical Patterns** (MUST READ): iOS safety, null safety - violations cause crashes
- **Architecture Patterns**: Dependency injection, reactive programming - core system patterns
- **When-to-Use Guidance**: Decision trees for pattern selection
- **Production Examples**: Real code from WorldWideWaves codebase

---

## Critical Patterns (MANDATORY)

These patterns prevent **production crashes and deadlocks**. All developers MUST read and follow.

### 1. iOS Safety Patterns

**File**: [ios-safety-patterns.md](./ios-safety-patterns.md)

**What It Covers**:
- Preventing iOS Kotlin/Native deadlocks (main thread freezes)
- Safe dependency injection on iOS
- Coroutine initialization patterns
- Kotlin-Swift exception handling

**When to Read**:
- ✅ **BEFORE** modifying any `shared/src/commonMain` code
- ✅ **BEFORE** creating new `@Composable` functions
- ✅ **BEFORE** adding new DI injections
- ✅ **BEFORE** every commit to shared code

**Key Rules**:
- ❌ NEVER create `object : KoinComponent` inside `@Composable`
- ❌ NEVER launch coroutines in `init{}` blocks
- ✅ ALWAYS use `IOSSafeDI` singleton for DI access
- ✅ ALWAYS use `@Throws(Throwable::class)` for iOS-exposed functions

**Verification**:
```bash
# Run before every commit
./scripts/verify-ios-safety.sh
```

---

### 2. Null Safety Patterns

**File**: [null-safety-patterns.md](./null-safety-patterns.md)

**What It Covers**:
- Eliminating force unwrap (`!!`) operators
- Safe null handling with elvis operator
- Early return patterns
- Validation patterns with `requireNotNull()`

**When to Read**:
- ✅ **BEFORE** accessing nullable properties
- ✅ **DURING** code review (check for `!!` operators)
- ✅ **WHEN** fixing NullPointerException crashes

**Key Rules**:
- ❌ NEVER use `!!` in production code
- ✅ ALWAYS use `?.` with `?:` fallback
- ✅ ALWAYS use `requireNotNull()` with descriptive error message
- ✅ ALWAYS validate inputs with early returns

**Common Violations**:
```kotlin
// ❌ UNSAFE - crashes on null
val position = simulation!!.getUserPosition()

// ✅ SAFE - provides fallback
val position = simulation?.getUserPosition() ?: Position.UNKNOWN
```

---

## Architecture Patterns (Core System)

These patterns define the core architecture of WorldWideWaves.

### 3. Dependency Injection Patterns

**File**: [di-patterns.md](./di-patterns.md)

**What It Covers**:
- Koin module organization (4 layers)
- Scope decisions (single vs factory)
- Platform-specific DI (Android vs iOS)
- Test module overrides
- iOS-safe DI patterns

**When to Read**:
- ✅ **BEFORE** creating new classes that need dependencies
- ✅ **WHEN** adding new modules or features
- ✅ **WHEN** writing tests that need mocked dependencies

**Module Load Order** (CRITICAL):
```kotlin
val sharedModule = listOf(
    commonModule,    // 1. Events, Sound
    helpersModule,   // 2. Domain logic, Position
    datastoreModule, // 3. Persistence
    uiModule         // 4. Repositories, Use Cases
)
```

**Scope Decision Tree**:
```
Need shared state across app?
├─ YES → Use `single { }`
│  └─ Examples: PositionManager, EventRepository
│
└─ NO → Need new instance per usage?
   ├─ YES → Use `factory { }`
   │  └─ Examples: ViewModels, Use Cases
   │
   └─ UNSURE → Default to `factory { }` (safer)
```

---

### 4. Reactive Programming Patterns

**File**: [reactive-patterns.md](./reactive-patterns.md)

**What It Covers**:
- StateFlow vs SharedFlow vs Flow (when to use each)
- Flow operators (map, filter, combine, debounce)
- ViewModel state management
- Compose integration patterns
- Backpressure strategies
- Error handling in flows
- Testing reactive code

**When to Read**:
- ✅ **BEFORE** creating new ViewModels
- ✅ **WHEN** implementing reactive UI components
- ✅ **WHEN** integrating with position/event streams
- ✅ **WHEN** writing tests for reactive code

**Flow Type Decision Tree**:
```
Need UI state (latest value always available)?
├─ YES → Use StateFlow<T>
│  └─ Example: viewModel.eventsState
│
└─ NO → Need event stream (no replay)?
   ├─ YES → Use SharedFlow<T>
   │  └─ Example: analytics events
   │
   └─ NO → Need cold stream (starts on collect)?
      └─ YES → Use Flow<T>
         └─ Example: database queries
```

**Common Patterns**:
```kotlin
// ViewModel state
private val _events = MutableStateFlow<List<WWWEvent>>(emptyList())
val events: StateFlow<List<WWWEvent>> = _events.asStateFlow()

// Compose collection
@Composable
fun EventsScreen(viewModel: EventsViewModel) {
    val events by viewModel.events.collectAsStateWithLifecycle()
}

// Testing flows
@Test
fun testEventFlow() = runTest {
    viewModel.events.test {
        assertEquals(emptyList(), awaitItem())
        viewModel.loadEvents()
        assertEquals(3, awaitItem().size)
    }
}
```

---

## Pattern Index

Comprehensive reference table for all patterns:

| Pattern Name | File | Category | When to Use | Critical? |
|-------------|------|----------|-------------|-----------|
| **IOSSafeDI Singleton** | [ios-safety-patterns.md](./ios-safety-patterns.md) | iOS Safety | DI access in shared code, @Composable functions | ✅ YES |
| **Suspend Initialization** | [ios-safety-patterns.md](./ios-safety-patterns.md) | iOS Safety | Class initialization with async operations | ✅ YES |
| **Kotlin-Swift Exception Handling** | [ios-safety-patterns.md](./ios-safety-patterns.md) | iOS Safety | Functions exposed to iOS Swift layer | ✅ YES |
| **Safe Null Handling (Elvis)** | [null-safety-patterns.md](./null-safety-patterns.md) | Null Safety | Accessing nullable properties | ✅ YES |
| **requireNotNull Validation** | [null-safety-patterns.md](./null-safety-patterns.md) | Null Safety | Critical paths where null is invalid | ✅ YES |
| **Early Return Guards** | [null-safety-patterns.md](./null-safety-patterns.md) | Null Safety | Function entry validation | ⚠️ Recommended |
| **Module Organization** | [di-patterns.md](./di-patterns.md) | Dependency Injection | Structuring Koin modules | ⚠️ Recommended |
| **Scope Decisions** | [di-patterns.md](./di-patterns.md) | Dependency Injection | Choosing single vs factory | ⚠️ Recommended |
| **Platform-Specific DI** | [di-patterns.md](./di-patterns.md) | Dependency Injection | Platform implementations (GPS, haptics) | ⚠️ Recommended |
| **StateFlow State Management** | [reactive-patterns.md](./reactive-patterns.md) | Reactive | ViewModel state with latest value | ⚠️ Recommended |
| **Flow Operators** | [reactive-patterns.md](./reactive-patterns.md) | Reactive | Transforming data streams | ⚠️ Recommended |
| **collectAsStateWithLifecycle** | [reactive-patterns.md](./reactive-patterns.md) | Reactive | Compose UI integration | ⚠️ Recommended |
| **Backpressure Handling** | [reactive-patterns.md](./reactive-patterns.md) | Reactive | High-frequency data sources (GPS) | ⚠️ Recommended |
| **Flow Error Handling** | [reactive-patterns.md](./reactive-patterns.md) | Reactive | Graceful error recovery | ⚠️ Recommended |
| **Test Flow Patterns** | [reactive-patterns.md](./reactive-patterns.md) | Testing | Testing reactive code | ⚠️ Recommended |

**Legend**:
- ✅ **Critical**: Violations cause crashes/deadlocks - MUST follow
- ⚠️ **Recommended**: Core patterns - SHOULD follow for maintainability

---

## Quick Reference: Decision Trees

### "Should I use this pattern?"

#### 1. I'm modifying shared code (commonMain)

```
Working in shared/src/commonMain?
└─ YES → Read iOS Safety Patterns FIRST
   ├─ Adding DI injection?
   │  └─ Use IOSSafeDI singleton pattern
   │
   ├─ Creating @Composable function?
   │  └─ NEVER create object : KoinComponent inside
   │
   ├─ Launching coroutines?
   │  └─ NEVER in init{}, use suspend initialize()
   │
   └─ Exposing to iOS?
      └─ Add @Throws(Throwable::class)
```

#### 2. I'm accessing nullable properties

```
Is property nullable (Type?)?
└─ YES → Read Null Safety Patterns
   ├─ Is null a valid case?
   │  ├─ YES → Use `?.` with `?:` fallback
   │  └─ NO → Use requireNotNull() with error message
   │
   ├─ Can I validate early?
   │  └─ YES → Use early return guard pattern
   │
   └─ NEVER use `!!` operator
```

#### 3. I'm creating a new class with dependencies

```
Class needs dependencies?
└─ YES → Read DI Patterns
   ├─ Need shared state across app?
   │  └─ Use single { } scope
   │
   ├─ Need new instance per usage?
   │  └─ Use factory { } scope
   │
   ├─ Platform-specific implementation?
   │  └─ Use expect/actual + platform modules
   │
   └─ Testing?
      └─ Override modules in test setup
```

#### 4. I'm implementing reactive UI

```
Need reactive state/events?
└─ YES → Read Reactive Patterns
   ├─ Need latest value always available?
   │  └─ Use StateFlow<T> in ViewModel
   │
   ├─ Need event stream (no replay)?
   │  └─ Use SharedFlow<T>
   │
   ├─ Need cold stream (starts on collect)?
   │  └─ Use Flow<T>
   │
   ├─ High-frequency updates (GPS)?
   │  └─ Use debounce/conflate for backpressure
   │
   └─ Collecting in Compose?
      └─ Use collectAsStateWithLifecycle()
```

---

## Pattern Selection Guide

### By Problem Domain

| Problem | Pattern to Use | File |
|---------|---------------|------|
| iOS app freezes on launch | IOSSafeDI Singleton | [ios-safety-patterns.md](./ios-safety-patterns.md) |
| NullPointerException in production | Safe Null Handling | [null-safety-patterns.md](./null-safety-patterns.md) |
| Need to inject dependencies | Module Organization | [di-patterns.md](./di-patterns.md) |
| UI not updating with data changes | StateFlow State Management | [reactive-patterns.md](./reactive-patterns.md) |
| GPS updates too frequent | Backpressure Handling | [reactive-patterns.md](./reactive-patterns.md) |
| Testing ViewModel state | Test Flow Patterns | [reactive-patterns.md](./reactive-patterns.md) |
| Platform-specific feature (haptics) | Platform-Specific DI | [di-patterns.md](./di-patterns.md) |
| Function exposed to iOS Swift | Kotlin-Swift Exception Handling | [ios-safety-patterns.md](./ios-safety-patterns.md) |

### By Development Phase

| Phase | Patterns to Review |
|-------|-------------------|
| **Starting new feature** | DI Patterns, Reactive Patterns |
| **Modifying shared code** | iOS Safety Patterns, Null Safety Patterns |
| **Code review** | All Critical Patterns (iOS Safety, Null Safety) |
| **Pre-commit** | iOS Safety Patterns (run verification script) |
| **Writing tests** | Test sections in DI/Reactive Patterns |
| **Fixing crashes** | Null Safety Patterns, iOS Safety Patterns |
| **Performance issues** | Backpressure Handling (Reactive Patterns) |

---

## Common Scenarios

### Scenario 1: Creating a New ViewModel

**Steps**:
1. Read [di-patterns.md](./di-patterns.md) → Scope Decisions
2. Read [reactive-patterns.md](./reactive-patterns.md) → ViewModel State Management
3. Read [ios-safety-patterns.md](./ios-safety-patterns.md) → Suspend Initialization

**Pattern**:
```kotlin
class EventsViewModel(
    private val repository: EventRepository  // Constructor injection
) : ViewModel() {

    // StateFlow for UI state
    private val _events = MutableStateFlow<List<WWWEvent>>(emptyList())
    val events: StateFlow<List<WWWEvent>> = _events.asStateFlow()

    // Suspend initialization (iOS-safe)
    suspend fun initialize() {
        repository.observeEvents()
            .collect { _events.value = it }
    }
}

// DI registration
val uiModule = module {
    factory { EventsViewModel(get()) }  // Factory scope
}
```

---

### Scenario 2: Accessing Position in Composable

**Steps**:
1. Read [ios-safety-patterns.md](./ios-safety-patterns.md) → IOSSafeDI Singleton
2. Read [reactive-patterns.md](./reactive-patterns.md) → Compose Integration
3. Read [null-safety-patterns.md](./null-safety-patterns.md) → Safe Null Handling

**Pattern**:
```kotlin
@Composable
fun MapScreen() {
    // iOS-safe DI access
    val positionManager = getIOSSafePositionManager()

    // Lifecycle-aware collection
    val position by positionManager.positionFlow
        .collectAsStateWithLifecycle(initialValue = null)

    // Safe null handling
    val currentPosition = position ?: Position.UNKNOWN

    Map(centerPosition = currentPosition)
}
```

---

### Scenario 3: Platform-Specific Implementation

**Steps**:
1. Read [di-patterns.md](./di-patterns.md) → Platform-Specific DI
2. Read [ios-safety-patterns.md](./ios-safety-patterns.md) → Kotlin-Swift Exception Handling

**Pattern**:
```kotlin
// 1. Define interface (commonMain)
interface HapticProvider {
    fun triggerImpact()
}

// 2. Android implementation (androidMain)
class AndroidHapticProvider : HapticProvider {
    override fun triggerImpact() {
        // Android vibration API
    }
}

// 3. iOS implementation (iosMain)
class IosHapticProvider : HapticProvider {
    override fun triggerImpact() {
        // iOS haptics API
    }
}

// 4. Register in platform modules
val androidModule = module {
    single<HapticProvider> { AndroidHapticProvider() }
}

val iosModule = module {
    single<HapticProvider> { IosHapticProvider() }
}
```

---

## Verification Checklist

Before committing code, verify pattern compliance:

### Pre-Commit Checklist

- [ ] **iOS Safety** (if modifying shared code):
  - [ ] Run `./scripts/verify-ios-safety.sh` (MUST pass)
  - [ ] No `object : KoinComponent` inside `@Composable`
  - [ ] No coroutine launch in `init{}`
  - [ ] All iOS-exposed functions have `@Throws(Throwable::class)`

- [ ] **Null Safety**:
  - [ ] Search for `!!` operators (SHOULD BE ZERO)
  - [ ] All nullable access uses `?.` with `?:`
  - [ ] Critical paths use `requireNotNull()` with messages

- [ ] **DI Patterns**:
  - [ ] Dependencies injected via constructor
  - [ ] Scope matches lifecycle (single vs factory)
  - [ ] Module load order correct

- [ ] **Reactive Patterns**:
  - [ ] ViewModel state uses StateFlow
  - [ ] Compose uses `collectAsStateWithLifecycle()`
  - [ ] High-frequency sources use backpressure handling

### Automated Verification

```bash
# Run all checks
./scripts/verify-patterns.sh

# Individual checks
./scripts/verify-ios-safety.sh           # iOS safety violations
./gradlew detekt                         # Null safety, code quality
./gradlew :shared:testDebugUnitTest      # Pattern implementation tests
```

---

## Related Documentation

### Core Documentation
- [CLAUDE.md](../../CLAUDE.md) - Main project instructions
- [CLAUDE_iOS.md](../../CLAUDE_iOS.md) - Complete iOS development guide

### iOS-Specific
- [docs/ios/ios-violation-tracker.md](../ios/ios-violation-tracker.md) - Deadlock violation history
- [docs/ios/ios-success-state.md](../ios/ios-success-state.md) - iOS success criteria
- [docs/ios/ios-debugging-guide.md](../ios/ios-debugging-guide.md) - Advanced debugging

### Testing
- [docs/comprehensive-test-specifications.md](../comprehensive-test-specifications.md) - Testing patterns
- [docs/testing-strategy.md](../testing-strategy.md) - Testing approach

### Architecture
- [docs/architecture.md](../architecture.md) - System architecture
- [docs/architecture/map-architecture-analysis.md](../architecture/map-architecture-analysis.md) - Map subsystem

---

## Pattern Evolution

### How Patterns Are Created

1. **Identify Problem**: Crash, deadlock, or maintenance issue
2. **Implement Solution**: Fix with clear pattern
3. **Validate in Production**: Verify pattern works
4. **Document Pattern**: Add to this directory
5. **Add to Index**: Update this README
6. **Add Verification**: Add automated check if possible

### Recent Pattern Additions

- **October 2025**: iOS Safety Patterns (11 violations fixed)
- **October 2025**: Null Safety Patterns (8 force unwraps eliminated)
- **October 2025**: DI Patterns (Koin module organization)
- **October 2025**: Reactive Patterns (StateFlow best practices)

### Future Pattern Candidates

- **Thread Safety Patterns**: Mutex usage, synchronized blocks
- **Testing Patterns**: Comprehensive test organization
- **Performance Patterns**: Memory optimization, battery efficiency
- **Accessibility Patterns**: WCAG compliance (see [docs/accessibility-guide.md](../accessibility-guide.md))

---

## Contributing Patterns

### When to Document a New Pattern

Add a pattern when:
- ✅ Pattern solves a **recurring problem** (>2 occurrences)
- ✅ Pattern prevents **production crashes/deadlocks**
- ✅ Pattern improves **code maintainability** significantly
- ✅ Pattern is **production-validated** (tested in real usage)

### How to Document a Pattern

1. **Create file**: `docs/patterns/{category}-patterns.md`
2. **Structure**:
   - Overview: What problem does it solve?
   - Critical Rules: ❌ NEVER / ✅ ALWAYS
   - Code Examples: Real WorldWideWaves code
   - When to Use: Clear decision criteria
   - Verification: How to validate pattern compliance
3. **Update this README**: Add to Pattern Index table
4. **Add verification**: Script if automatable
5. **Update CLAUDE.md**: Reference pattern if critical

### Pattern Documentation Template

```markdown
# {Category} Patterns

> **Purpose**: [What problem domain this covers]
> **Status**: [Draft / Production-Ready]
> **Version**: [1.0]

## Critical Rules

### ❌ NEVER: [Anti-pattern name]

[Bad code example]

**Why**: [Explanation of problem]

### ✅ ALWAYS: [Correct pattern name]

[Good code example]

**Why**: [Explanation of benefits]

## When to Use

- Scenario 1
- Scenario 2

## Verification

[Automated check commands]
```

---

## Support

### Questions About Patterns?

1. **Search this index**: Use decision trees and scenario guides
2. **Check pattern file**: Read full pattern documentation
3. **Review examples**: See production code in WorldWideWaves
4. **Run verification**: Use automated checks
5. **Ask in PR review**: Tag pattern violations in code review

### Pattern Violations in PR?

1. **Identify violation**: Which pattern is violated?
2. **Reference pattern file**: Link to specific section
3. **Explain impact**: Why does it matter? (crash risk, etc.)
4. **Suggest fix**: Provide correct pattern example
5. **Verify fix**: Run automated checks

---

**Last Updated**: October 27, 2025
**Version**: 1.0
**Maintainer**: WorldWideWaves Development Team
