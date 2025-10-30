# WorldWideWaves - Claude Code Instructions

> **Quick Links**: [iOS Safety](#ios-requirements-critical) | [Testing](#testing-requirements) | [Build Commands](#build-commands) | [Code Quality](#code-quality-standards)

---

## Project Overview

WorldWideWaves is an **ephemeral mobile app** for orchestrating synchronized, location-based human wave events. Events exist briefly, creating shared moments without permanent data storage.

### Technology Stack

- **Platform**: Kotlin Multiplatform Mobile (KMM) 1.9+
- **UI**: Compose Multiplatform (100% shared Android/iOS)
- **Maps**: MapLibre (open-source, self-hosted)
- **Backend**: Firebase (Firestore, Storage)
- **DI**: Koin
- **Testing**: 902+ unit tests (Kotlin Test + Turbine)
- **CI/CD**: GitHub Actions (Android + iOS)

### Architecture

- **Pattern**: Clean Architecture + MVVM + Reactive Programming
- **State Management**: StateFlow + Compose
- **Position**: PositionManager as single source of truth (SIMULATION > GPS)
- **Navigation**: Custom deep linking

---

## iOS Requirements [CRITICAL]

> **Status**: ✅ STABLE | **Tests**: 902/902 passing | **Violations**: 0/11

### 🚨 iOS Deadlock Prevention [MANDATORY]

iOS Kotlin/Native has **strict threading requirements**. Violations cause **immediate deadlocks** on app launch.

#### The 6 Absolute Rules

**❌ NEVER**:
1. `object : KoinComponent` inside @Composable scopes
2. `by inject()` during Compose composition
3. `runBlocking` before ComposeUIViewController
4. Coroutine launches in `init{}` blocks
5. DI access (`get<T>()`) in `init{}` blocks
6. `Dispatchers.Main` in property initialization

**✅ ALWAYS**:
1. Use IOSSafeDI singleton for Composable DI access
2. Use parameter injection or LocalKoin.current.get<T>()
3. Use suspend functions with LaunchedEffect
4. Use constructor injection, not init{} DI
5. Use lazy initialization: `by lazy { CoroutineScope(Dispatchers.Main) }`

#### Quick Example

```kotlin
// ❌ DEADLOCKS iOS!
@Composable
fun Screen() {
    val deps = object : KoinComponent {
        val clock by inject()  // DEADLOCK
    }
}

// ✅ SAFE
@Composable
fun Screen() {
    val clock = getIOSSafeClock()  // IOSSafeDI wrapper
}
```

#### Verification

**Before EVERY commit** touching shared code:
```bash
./scripts/dev/verification/verify-ios-safety.sh
```

**Expected**: Zero violations in all checks.

**See**: [CLAUDE_iOS.md](./CLAUDE_iOS.md) for comprehensive iOS guide
**See**: [docs/patterns/ios-safety-patterns.md](docs/patterns/ios-safety-patterns.md) for all patterns

---

## Accessibility Requirements [MANDATORY]

> **Status**: ✅ WCAG 2.1 Level AA Compliant | **Tests**: 27+ passing

### All UI Components Must

- ✅ **contentDescription**: Localized via MokoRes.strings
- ✅ **semantics**: `role`, `contentDescription`, `stateDescription`
- ✅ **Touch targets**: 48dp (Android) / 44pt (iOS) minimum
- ✅ **Text scaling**: Use `.sp` units (respects system font size)
- ✅ **Screen readers**: TalkBack (Android) and VoiceOver (iOS)
- ✅ **Color contrast**: 4.5:1 minimum ratio (WCAG AA)
- ✅ **Heading hierarchy**: `semantics { heading = true }`

### Example

```kotlin
Button(
    onClick = { action() },
    modifier = Modifier
        .size(48.dp)
        .semantics {
            role = Role.Button
            contentDescription = "Join wave event"
        }
)
```

### Testing

```bash
./scripts/dev/verification/test_accessibility.sh  # Before each PR
```

**See**: [docs/accessibility-guide.md](docs/accessibility-guide.md) for complete patterns

---

## Mandatory Development Requirements

### Platform Compatibility

- **Cross-platform**: macOS + Linux required
- **Scripts**: Use `#!/usr/bin/env bash`, avoid macOS-only commands
- **Testing**: Verify on both platforms before commit

### Build System

- ❌ **NEVER modify `gradle.build.kt` without approval**
- ❌ **NEVER disable tests without permission**
- ✅ **ALWAYS run tests before commit**

### Testing Philosophy

- **Tests validate business requirements**, not implementation details
- **Test failures** mean: (1) business logic issue OR (2) requirements changed
- **Test modifications require approval** with explanation
- **Test deletion absolutely forbidden** without explicit consent

### Security

- ❌ Never log, store, or transmit API keys/tokens/secrets
- ✅ Validate all user inputs (coordinates: lat±90, lng±180)
- ✅ Use proper exception handling without exposing sensitive data
- ✅ All network requests must use HTTPS
- ✅ Handle location data with appropriate privacy measures

---

## Testing Requirements

### Test Organization

```
shared/src/
├── commonTest/          # Platform-independent (NO MockK, NO JVM APIs)
├── androidUnitTest/     # Android-specific (CAN use MockK)
└── iosTest/             # iOS-specific (NO MockK, Kotlin/Native only)
```

### Critical: Run Before Every Commit

```bash
./gradlew clean :shared:testDebugUnitTest :composeApp:assembleDebug
# Expected: 902+ tests, 100% pass rate, ~22s execution
```

### Requirements

- **All changes must pass existing test suite** (902+ tests)
- **New functionality requires tests** - no test debt
- **Run ALL tests**, not just relevant ones
- **Performance**: Monitor test execution time
- **iOS safety**: `./scripts/dev/verification/verify-ios-safety.sh` for shared code changes

### Key Test Patterns

```kotlin
// Infinite flows - Don't use advanceUntilIdle()!
observer.startObservation()
testScheduler.runCurrent()  // Process current only
// ... assertions ...
observer.stopObservation()  // Cancel first
testScheduler.advanceUntilIdle()  // Now safe

// Test isolation - Prevent flaky tests
@AfterTest
fun tearDown() {
    runBlocking {
        testScopeProvider.cancelAllCoroutines()
        delay(500)  // Cleanup propagation
    }
    stopKoin()
}
```

**See**: [docs/testing/test-patterns.md](docs/testing/test-patterns.md) for comprehensive patterns

---

## Code Quality Standards

### 🚨 Zero-Warnings Policy [MANDATORY]

**BEFORE EVERY COMMIT**: ALL platforms MUST compile with ZERO warnings.

```bash
# Pre-commit verification checklist
./gradlew :shared:compileKotlinIosSimulatorArm64        # iOS Kotlin
./gradlew :shared:compileDebugKotlinAndroid            # Android Kotlin
./gradlew :shared:testDebugUnitTest                     # All tests
cd iosApp && xcodebuild -project worldwidewaves.xcodeproj -scheme worldwidewaves build  # iOS Swift
swiftlint lint --quiet                                  # SwiftLint (0 warnings)
./gradlew detekt                                        # Detekt (0 warnings)
```

**NO EXCEPTIONS** - Fix ALL warnings, even in files you didn't modify.

### Null Safety

**Rule**: NEVER use `!!` in production code.

```kotlin
// ❌ UNSAFE
val position = simulation!!.getUserPosition()

// ✅ SAFE
val position = simulation?.getUserPosition() ?: Position.UNKNOWN

// ✅ SAFE with context
val wave = requireNotNull(linear ?: deep ?: linearSplit) {
    "Wave definition must exist after validation"
}
```

**See**: [docs/patterns/null-safety-patterns.md](docs/patterns/null-safety-patterns.md)

### Thread Safety

Any mutable shared state MUST have explicit synchronization:

```kotlin
object SharedState {
    private val mutex = Mutex()
    private val data = mutableSetOf<String>()

    suspend fun add(item: String) {
        mutex.withLock { data += item }
    }
}
```

### Detekt Suppressions (When Justified)

```kotlin
@Suppress("ReturnCount")  // Guard clauses OK
@Suppress("TooGenericExceptionCaught")  // When catching IndexOutOfBoundsException
@Suppress("MatchingDeclarationName")  // expect/actual files (*.android.kt)
```

**Always add comment explaining WHY.**

### Code Style

- **Kotlin conventions**: Follow official Kotlin coding conventions
- **Import organization**: Run `./gradlew :shared:ktlintFormat`
- **Class order**: companion → properties → init → public → private → nested
- **File size**: Target <300 lines, warning >500, must split >600

**See**: [docs/code-style/class-organization.md](docs/code-style/class-organization.md)

---

## Architecture Patterns

### Dependency Injection

- **Framework**: Koin for all DI
- **iOS Safety**: Use IOSSafeDI singleton for Composable access
- **Modules**: CommonModule, AndroidModule, IOSModule

### Position System

**Critical Rule**: PositionManager is the **single source of truth** for user position.

- ✅ Components **observe** position via `positionManager.positionFlow`
- ❌ Components **never set** position directly
- **Priority**: SIMULATION > GPS (testing > real device)
- **Pattern**: Reactive updates via StateFlow
- **Testing**: Use simulation mode for time acceleration and position control

```kotlin
// ✅ CORRECT
positionManager.positionFlow.collect { position ->
    handlePosition(position)
}

// ❌ WRONG
val gpsProvider = GPSProvider()  // Don't create separate sources!
```

**Simulation Mode**: For testing event participation without waiting for real events, use simulation mode with time acceleration and position control. See [Simulation Mode Guide](docs/features/simulation-mode.md) for complete documentation.

### Error Handling

```kotlin
// ✅ CORRECT
@Throws(Throwable::class)
fun performOperation() {
    try {
        val result = doWork()
        Log.v(TAG, "Success")
    } catch (e: SpecificException) {
        Log.e(TAG, "Failed", throwable = e)
        // Handle without exposing sensitive data
    }
}
```

---

## Build Commands

### Essential Commands

```bash
# Run unit tests
./gradlew :shared:testDebugUnitTest

# Build debug version
./gradlew assembleDebug

# Run lint checks
./gradlew detekt

# Verify iOS safety (shared code changes)
./scripts/dev/verification/verify-ios-safety.sh

# Accessibility tests
./scripts/dev/verification/test_accessibility.sh
```

### iOS Build

```bash
# Build Kotlin framework
./gradlew :shared:embedAndSignAppleFrameworkForXcode

# From Xcode (recommended)
cd iosApp
open worldwidewaves.xcodeproj
# Cmd+R to build and run

# From command line
cd iosApp
xcodebuild -project worldwidewaves.xcodeproj \
  -scheme worldwidewaves \
  -destination 'platform=iOS Simulator,name=iPhone 15 Pro' \
  build
```

### Clean Build

```bash
./gradlew clean
rm -rf ~/Library/Developer/Xcode/DerivedData/worldwidewaves-*
./gradlew :shared:embedAndSignAppleFrameworkForXcode
```

---

## Production-Ready Definition

A file/module is production-ready when:

- ✅ All unit tests passing (100%)
- ✅ Zero compilation warnings (iOS + Android)
- ✅ Zero detekt/SwiftLint warnings (or justified suppressions)
- ✅ File header present and correct
- ✅ No unsafe `!!` operators (Kotlin) or force unwraps (Swift)
- ✅ Thread safety explicit (Mutex, synchronized)
- ✅ Accessibility semantics on interactive elements
- ✅ No hardcoded user-facing strings
- ✅ Proper error handling (specific exceptions)
- ✅ Memory leak prevention (cleanup methods)
- ✅ iOS safety compliance (no deadlock patterns)

---

## Development Workflow

### Critical Rules

- **Commit** your work, always
- **Use agents** for complex multi-step tasks
- **Check all implications** when changing code (callers, documentation, signatures)
- **Clean temporary files** after development
- **Git push has costs** - GitHub Actions not free, only push when asked/required
- **Search for similar patterns** when fixing bugs (find other instances)
- **Never bypass git hooks** (pre-commit, pre-push)
- **Use short names** in code (imports for readability, not long qualified names)
- **Fix warnings immediately** - prevent accumulation

### Import Management [CRITICAL]

**ALWAYS check existing imports BEFORE modifying code.**

When adding function calls/classes:
1. Check if required import exists
2. Add missing imports in same change
3. Verify compilation before commit

**Common imports**:
- Coroutines: `kotlinx.coroutines.runBlocking`, `withContext`, etc.
- Compose: `androidx.compose.runtime.key`, `LaunchedEffect`, etc.
- Platform: `platform.UIKit.*`, `platform.Foundation.*` (iOS)
- Logging: `com.worldwidewaves.shared.utils.Log`

### Git Workflow

#### Committing Changes

**ONLY commit when user explicitly requests it.**

```bash
# 1. Check status and changes
git status && git diff

# 2. Run ALL tests
./gradlew clean :shared:testDebugUnitTest

# 3. Stage relevant files
git add <files>

# 4. Commit with descriptive message
git commit -m "$(cat <<'EOF'
feat: add wave scheduling feature

- Implement ObservationScheduler for wave timing
- Add timezone-aware scheduling
- Add 100% test coverage

🤖 Generated with [Claude Code](https://claude.com/claude-code)

Co-Authored-By: Claude <noreply@anthropic.com>
EOF
)"

# 5. Verify tests still pass
./gradlew :shared:testDebugUnitTest
```

**DO NOT push to origin** unless user explicitly requests it.

#### Creating Pull Requests

```bash
# 1. Understand full branch context
git status
git diff main...HEAD
git log main..HEAD

# 2. Draft PR summary (analyze ALL commits, not just latest)

# 3. Create PR
gh pr create --title "Title" --body "$(cat <<'EOF'
## Summary
- Bullet points of changes

## Test plan
- [ ] Checklist of testing steps

🤖 Generated with [Claude Code](https://claude.com/claude-code)
EOF
)"
```

---

## Common Issues

| Symptom | Cause | Solution |
|---------|-------|----------|
| iOS app freezes on launch | DI violation | Run `./scripts/dev/verification/verify-ios-safety.sh` |
| Tests failing | Business logic issue or requirements changed | Analyze failure, ask before changing tests |
| Compilation warnings | Code quality issue | Fix ALL warnings, not just in modified files |
| Map gestures not working (iOS) | Wrong property names | Use `isZoomEnabled/isScrollEnabled` |
| Null pointer crash | Force unwrap `!!` | Use `?.` with `?:` or `requireNotNull()` |

---

## Project Structure

```
WorldWideWaves/
├── shared/                     # KMM shared code
│   ├── src/commonMain/         # Business logic
│   │   ├── domain/             # EventObserver, WaveProgressionTracker
│   │   ├── data/               # Repositories, data sources
│   │   ├── ui/                 # Compose UI (100% shared)
│   │   ├── position/           # PositionManager
│   │   └── map/                # Map abstraction
│   ├── src/androidMain/        # Android implementations
│   ├── src/iosMain/            # iOS implementations (RootController)
│   └── src/commonTest/         # Platform-independent tests
├── composeApp/                 # Android app
├── iosApp/                     # iOS app (Swift/UIKit)
│   ├── AppDelegate.swift       # App lifecycle
│   ├── SceneDelegate.swift     # Platform initialization
│   └── IOSPlatformEnabler.swift  # Swift-Kotlin bridge
├── maps/                       # 40+ city offline map modules
├── scripts/                    # Build/verification scripts
└── docs/                       # Documentation
```

---

## Related Documentation

### iOS Development
- **[CLAUDE_iOS.md](./CLAUDE_iOS.md)** - Complete iOS development guide
- **[docs/ios/](docs/ios/)** - iOS-specific documentation hub
- **[docs/patterns/ios-safety-patterns.md](docs/patterns/ios-safety-patterns.md)** - All iOS safety patterns

### Android Development
- **[docs/android/android-development-guide.md](docs/android/android-development-guide.md)** - Complete Android development guide
- **[docs/android/](docs/android/)** - Android-specific documentation hub

### Testing
- **[docs/testing/test-patterns.md](docs/testing/test-patterns.md)** - Comprehensive test patterns
- **[docs/testing-strategy.md](docs/testing-strategy.md)** - Testing approach

### Patterns & Architecture
- **[docs/patterns/null-safety-patterns.md](docs/patterns/null-safety-patterns.md)** - Null handling patterns
- **[docs/code-style/class-organization.md](docs/code-style/class-organization.md)** - Class structure standards
- **[docs/architecture.md](docs/architecture.md)** - System architecture
- **[docs/accessibility-guide.md](docs/accessibility-guide.md)** - Complete accessibility patterns

### CI/CD & Operations
- **[docs/ci-cd.md](docs/ci-cd.md)** - CI/CD pipeline
- **[docs/development.md](docs/development.md)** - Development workflows

---

**Last Updated**: October 27, 2025
**Version**: 3.0 (Optimized for AI context efficiency)
**Maintainer**: WorldWideWaves Development Team
