# WorldWideWaves - Claude Code Instructions

> **Quick Links**: [iOS Requirements](#-ios-requirements-critical) | [Build Commands](#build-and-testing-commands) | [Testing](#testing-requirements) | [Debugging](#common-issue-prevention)

## Project Overview

WorldWideWaves is an **ephemeral mobile app** designed to orchestrate human waves through cities and countries. The app creates synchronized, location-based human wave events that transcend physical and cultural boundaries, fostering unity, community, and shared human experience through real-time coordination.

**Key Concept**: Events are ephemeral - they exist briefly, creating shared moments without permanent data storage.

### Technology Stack

#### Core Framework
- **Platform**: Kotlin Multiplatform Mobile (KMM) 1.9+
- **UI**: Compose Multiplatform (shared between Android and iOS)
- **Architecture**: Clean Architecture + MVVM + Reactive Programming

#### UI Layer
- **Android**: Jetpack Compose (100% shared with iOS)
- **iOS**: Compose Multiplatform via ComposeUIViewController
- **Navigation**: Custom navigation with deep linking support
- **State**: StateFlow + Compose state management

#### Platform Services
- **Maps**: MapLibre (open-source, self-hosted tiles)
- **Location**: Native GPS with custom PositionManager abstraction
- **Sound**: MIDI-based sound choreography (shared code)
- **Resources**: MokoResources for multiplatform assets

#### Backend & Data
- **Backend**: Firebase (Firestore, Storage)
- **Local Storage**: Multiplatform settings
- **Networking**: Ktor client
- **Serialization**: Kotlinx Serialization

#### Testing
- **Unit Tests**: 902+ tests with Kotlin Test + Turbine
- **Android Instrumented**: Compose UI testing
- **iOS Testing**: In progress
- **Coverage**: High coverage on domain/data layers

#### Development Tools
- **Build**: Gradle 8.5+ with version catalogs
- **CI/CD**: GitHub Actions (Android + iOS)
- **Code Quality**: Detekt, SwiftLint
- **Logging**: Napier (multiplatform) + custom Log wrapper

---

## 🍎 iOS Requirements [CRITICAL - READ FIRST]

> **Status**: ✅ STABLE (October 2025) | **Bundle**: com.worldwidewaves | **Tests**: Pending

### Current Working Architecture

WorldWideWaves iOS uses **Compose Multiplatform** with native UIKit integration:

```
┌─────────────────────────────────────────────────┐
│         iOS App (Swift/UIKit)                    │
│                                                   │
│  ┌──────────────────────────────────────────┐   │
│  │  AppDelegate.swift                        │   │
│  │  - URL routing                            │   │
│  │  - App lifecycle                          │   │
│  └──────────────────────────────────────────┘   │
│                                                   │
│  ┌──────────────────────────────────────────┐   │
│  │  SceneDelegate.swift                      │   │
│  │  - Scene/window management                │   │
│  │  - Platform initialization (Koin, Moko)   │   │
│  │  - SKIKO configuration                    │   │
│  └──────────────────────────────────────────┘   │
│                                                   │
│  ┌──────────────────────────────────────────┐   │
│  │  IOSPlatformEnabler.swift                 │   │
│  │  - Swift-Kotlin bridge                    │   │
│  │  - Native services (haptics, etc.)        │   │
│  └──────────────────────────────────────────┘   │
└─────────────────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────┐
│     Shared Kotlin Code (Compose UI + Logic)      │
│                                                   │
│  ┌──────────────────────────────────────────┐   │
│  │  RootController.kt                        │   │
│  │  - makeMainViewController()               │   │
│  │  - makeEventViewController(id)            │   │
│  │  - makeWaveViewController(id)             │   │
│  │  - makeFullMapViewController(id)          │   │
│  └──────────────────────────────────────────┘   │
│                                                   │
│  ┌──────────────────────────────────────────┐   │
│  │  WWWMainActivity, WWWEventActivity        │   │
│  │  - Activity wrappers (Android pattern)    │   │
│  │  - Compose UI content                     │   │
│  └──────────────────────────────────────────┘   │
│                                                   │
│  ┌──────────────────────────────────────────┐   │
│  │  Shared Compose UI Components             │   │
│  │  - EventsScreen, WaveScreen, etc.         │   │
│  │  - 100% shared between platforms          │   │
│  └──────────────────────────────────────────┘   │
└─────────────────────────────────────────────────┘
```

### 🚨 iOS Deadlock Prevention Rules [MANDATORY]

iOS Kotlin/Native has **strict threading requirements**. Violating these rules causes **immediate deadlocks** on app launch.

#### ❌ NEVER DO THIS:

```kotlin
// ❌ WRONG: Object inside @Composable function
@Composable
fun MyScreen() {
    val deps = object : KoinComponent {
        val clock by inject()  // DEADLOCKS iOS!
    }
}

// ❌ WRONG: Coroutine launch in init{}
class MyClass {
    init {
        CoroutineScope.launch {  // DEADLOCKS iOS!
            doWork()
        }
    }
}

// ❌ WRONG: DI access in init{}
class MyClass {
    init {
        val dep = get<SomeDependency>()  // DEADLOCKS iOS!
    }
}
```

#### ✅ ALWAYS DO THIS:

```kotlin
// ✅ CORRECT: Use IOSSafeDI singleton (file-level)
object IOSSafeDI : KoinComponent {  // File-level is OK
    val platform: WWWPlatform by inject()
    val clock: IClock by inject()
}

@Composable
fun MyScreen() {
    val clock = getIOSSafeClock()  // Safe wrapper function
}

// ✅ CORRECT: Parameter injection
@Composable
fun MyScreen(viewModel: MyViewModel) {
    // Use viewModel
}

@Composable
fun ParentScreen() {
    val viewModel = LocalKoin.current.get<MyViewModel>()
    MyScreen(viewModel)
}

// ✅ CORRECT: Suspend initialization
class MyClass {
    private lateinit var data: Data

    suspend fun initialize() {
        data = loadData()
    }
}

@Composable
fun UseClass() {
    val instance = remember { MyClass() }
    LaunchedEffect(Unit) {
        instance.initialize()
    }
}
```

#### ✅ CORRECT: Kotlin-Swift Exception Handling

```kotlin
@Throws(Throwable::class)
fun makeMainViewController(): UIViewController { ... }
```

```swift
do {
    let vc = try RootControllerKt.makeMainViewController()
} catch let e as NSError {
    NSLog("❌ Error: \(e.localizedDescription)")
}
```

### 🧪 Automated Verification

**Before every commit to shared code, run**:
```bash
./scripts/verify-ios-safety.sh
```

**Manual verification**:
```bash
# 1. Find Composable-scoped KoinComponent (SHOULD RETURN ZERO)
rg -B10 "object.*KoinComponent" shared/src/commonMain --type kotlin \
  | rg "@Composable" -A10 | rg "object.*KoinComponent"

# 2. Find init{} blocks with coroutine launches (SHOULD RETURN ZERO)
rg -n -A 5 "init\s*\{" shared/src/commonMain --type kotlin \
  | rg "launch\{|async\{|scope\."

# 3. Find init{} blocks with DI access (SHOULD RETURN ZERO)
rg -n -A 3 "init\s*\{" shared/src/commonMain --type kotlin \
  | rg "get\(\)|inject\(\)" | rg -v "// iOS FIX"

# 4. Verify IOSSafeDI singleton exists (SHOULD RETURN ONE)
rg "object IOSSafeDI : KoinComponent" shared/src/commonMain --type kotlin

# 5. Find file-level KoinComponent objects (LEGITIMATE - for review)
rg "^object.*: KoinComponent" shared/src/commonMain --type kotlin
```

**Expected Results**:
- Commands 1-3: **ZERO results** (no violations)
- Command 4: **ONE result** (IOSSafeDI.kt)
- Command 5: **Review results** (file-level singletons are OK, verify intentional)

**Note**: Class-level `by inject()` is SAFE and expected:
```kotlin
class MyClass : KoinComponent {
    private val dependency by inject()  // ✅ SAFE - class property
}
```

**📋 TRACKING**: See [docs/iOS_VIOLATION_TRACKER.md](docs/iOS_VIOLATION_TRACKER.md) for comprehensive history
**✅ STATUS**: All 11 critical violations fixed (October 2025)
**🎯 MAINTENANCE**: Run verification commands regularly to prevent regressions

### 🔧 iOS Build & Run

#### From Xcode (Recommended):
```bash
cd iosApp
open worldwidewaves.xcodeproj
# Select worldwidewaves scheme, iPhone 15 Pro simulator
# Press Cmd+R to build and run
```

#### From Command Line:
```bash
# Build framework
./gradlew :shared:embedAndSignAppleFrameworkForXcode

# Build app
cd iosApp
xcodebuild -project worldwidewaves.xcodeproj \
  -scheme worldwidewaves \
  -destination 'platform=iOS Simulator,name=iPhone 15 Pro' \
  build
```

### 🐛 iOS Debugging

#### View Logs:
```bash
xcrun simctl spawn booted log stream \
  --predicate 'process == "WorldWideWaves"' \
  --level debug
```

#### Common Issues:

| Symptom | Cause | Solution |
|---------|-------|----------|
| App freezes on launch | DI violation | Run `verify-ios-safety.sh` |
| Koin initialization fails | Missing IOSModule | Check SceneDelegate `installPlatform()` |
| Compose UI crash | SKIKO not set | Verify `setenv("SKIKO_RENDER_API", "METAL", 1)` |
| Resources not loading | MokoRes bundle issue | Check `doInitPlatform()` logs |

### 📚 Detailed iOS Documentation

For complete iOS setup, debugging, and troubleshooting:
- **[CLAUDE_iOS.md](./CLAUDE_iOS.md)** - Complete iOS development guide
- **[docs/iOS_SUCCESS_STATE.md](./docs/iOS_SUCCESS_STATE.md)** - Current working state verification
- **[docs/iOS_VIOLATION_TRACKER.md](./docs/iOS_VIOLATION_TRACKER.md)** - Historical violations and fixes
- **[docs/iOS_DEBUGGING_GUIDE.md](./docs/iOS_DEBUGGING_GUIDE.md)** - Advanced debugging

---

## Recent Major Updates

### Position System Refactor (September 2025)
A comprehensive position system refactor has been completed to improve performance, maintainability, and reliability:

- **Unified Observer Architecture**: Replaced 3 separate observation streams with single efficient stream
- **PositionManager Integration**: Centralized position management with source priority and debouncing
- **Map Integration**: Enhanced AbstractEventMap integration with unified position handling
- **Performance Optimizations**: Conservative architectural improvements maintaining test compatibility
- **Status**: ✅ Completed (902/902 unit tests passing, instrumented tests in progress)

#### Key Principles

1. **Single Source of Truth**: PositionManager is the only source of user position
2. **No Direct Position Setting**: Components observe position, don't set it
3. **Priority Handling**: Simulation positions override GPS (for testing)
4. **Reactive Updates**: All position changes flow through StateFlow streams

#### Usage Pattern

```kotlin
// ✅ CORRECT: Observe positions from PositionManager
class MyComponent(
    private val positionManager: PositionManager
) {
    fun observePosition() {
        positionManager.positionFlow.collect { position ->
            handlePosition(position)
        }
    }
}

// ❌ WRONG: Don't create separate position sources
class MyComponent {
    private val gpsProvider = GPSProvider()  // WRONG!
}
```

---

## Mandatory Development Requirements

### Platform Compatibility
- **Cross-platform development**: All development must be compatible with **both macOS and Linux** environments
- **Script compatibility**: Bash scripts must use `#!/usr/bin/env bash` and avoid macOS-specific commands
- **Path handling**: Use POSIX-compliant path operations (avoid hardcoded paths)
- **Tool availability**: Verify tool availability and provide installation instructions for both platforms
- **Testing**: Test scripts and builds on both macOS and Linux before committing

### Build System Restrictions
- **NEVER modify gradle.build.kt files**: Always ask before making changes to build configuration files
- **Dependency changes**: Coordinate any dependency updates or additions
- **Build script modifications**: Require explicit approval before altering build logic

### Testing Requirements
- **NEVER disable tests without permission**: Always ask user permission before disabling, skipping, or renaming any test files
- **NEVER disable tests to make them pass**: Tests must be logical and business-oriented, not mirror current code implementation
- **Test failure philosophy**: If tests fail, either there's a business logic issue in the code OR the business requirements changed and tests need adaptation
- **Test-first thinking**: Tests validate business requirements, not implementation details
- **Test modifications**: Changing test logic requires explanation and user approval - explain what business requirement changed
- **Test deletion**: Absolutely forbidden without explicit user consent

### Security Patterns
- **NO credential exposure**: Never log, store, or transmit API keys, tokens, or secrets
- **Input validation**: All user inputs must be validated and sanitized
- **Error handling**: Use proper exception handling without exposing sensitive information
- **Secure communication**: All network requests must use HTTPS
- **Data protection**: Personal location data must be handled with appropriate privacy measures

### Architecture Patterns
- **Dependency Injection**: Use Koin for dependency management
- **Reactive Programming**: Leverage Kotlin Coroutines and Flow for async operations
- **State Management**: Use StateFlow for reactive state management
- **Clean Architecture**: Maintain clear separation between data, domain, and presentation layers
- **Testing**: Write comprehensive unit tests and maintain existing test coverage

### Code Modification Best Practices

#### Import Management (CRITICAL)
- **ALWAYS check existing imports BEFORE modifying code**
- When adding new function calls, classes, or language features:
  1. First check if the required import exists in the file
  2. Add missing imports immediately in the same change
  3. Verify compilation before committing
- This prevents compilation errors and reduces iteration cycles

**Common imports to check:**
- Coroutines: `kotlinx.coroutines.runBlocking`, `kotlinx.coroutines.withContext`, etc.
- Compose: `androidx.compose.runtime.key`, `androidx.compose.runtime.LaunchedEffect`, etc.
- Platform: `platform.UIKit.*`, `platform.Foundation.*` for iOS
- Logging: `com.worldwidewaves.shared.utils.Log`

### Position System Guidelines
- **PositionManager**: Use centralized position management for all location-related operations
- **Source Priority**: SIMULATION > GPS (simulation for testing, GPS for real device location)
- **No Map Click Positioning**: User position comes from GPS only, not map interactions
- **Reactive Updates**: Use unified position streams rather than direct position setting

---

## Performance Considerations for KMM

### Memory Management
- Use appropriate coroutine scopes and cancel jobs properly
- Avoid memory leaks in long-running operations
- Properly dispose of reactive streams and observers

### Battery Optimization
- Minimize GPS usage frequency through debouncing and deduplication
- Use appropriate location providers based on accuracy requirements
- Implement proper background/foreground state handling

### Cross-Platform Compatibility
- Test implementations on both Android and iOS
- Use expect/actual declarations for platform-specific code
- Maintain consistent behavior across platforms

---

## Error Handling Patterns

```kotlin
// ✅ CORRECT: Proper error handling
@Throws(Throwable::class)
fun performOperation() {
    try {
        val result = doWork()
        Log.v("Component", "Operation successful")
    } catch (e: Exception) {
        Log.e("Component", "Operation failed", throwable = e)
        // Handle error appropriately without exposing sensitive data
    }
}

// ❌ WRONG: Generic exception exposure
catch (e: Exception) {
    throw e // Don't re-throw without handling
}
```

---

## Input Validation Requirements

- Validate all geographic coordinates (latitude: -90 to 90, longitude: -180 to 180)
- Sanitize all user-provided text inputs
- Validate time/duration inputs for reasonable ranges
- Check file paths and prevent directory traversal attacks
- Validate network URLs and prevent SSRF attacks

---

## Critical Asset Protection

### Location Data
- Never log precise user coordinates in production
- Use appropriate precision levels for different use cases
- Implement proper data retention policies
- Respect user privacy preferences

### API Keys and Secrets
- Store in secure configuration (not in code)
- Use BuildConfig or equivalent for environment-specific values
- Never commit secrets to version control
- Implement proper key rotation procedures

---

## Common Issue Prevention

### Position/Location Issues
- Always check for null positions before use
- Implement proper fallback mechanisms for missing GPS
- Use PositionManager for all position operations
- Test position flows with both real and simulated data

### Coroutine Management
- Always use appropriate CoroutineScope
- Cancel jobs when components are destroyed
- Use proper exception handling in coroutines
- Avoid blocking operations on main thread

### Testing Best Practices
- Mock external dependencies properly
- Use TestCoroutineScheduler for testing time-dependent code
- Maintain test isolation and avoid test interdependencies
- Run both unit and instrumented tests before committing

---

## Code Style Guidelines

### Kotlin Style
- Follow official Kotlin coding conventions
- Use meaningful variable and function names
- Prefer immutable data structures where possible
- Use extension functions appropriately

### Documentation
- Document complex algorithms and business logic
- Use KDoc for public APIs
- Include examples in documentation where helpful
- Maintain up-to-date README files

---

## Build and Testing Commands

### Essential Commands
```bash
# Run unit tests
./gradlew :shared:testDebugUnitTest

# Run Android instrumented tests
ANDROID_SERIAL=emulator-5556 ./gradlew :composeApp:connectedDebugAndroidTest

# Build debug version
./gradlew assembleDebug

# Run lint checks
./gradlew lint

# Verify iOS safety (before committing shared code)
./scripts/verify-ios-safety.sh
```

### Testing Requirements
- All changes must pass existing test suite (902+ unit tests)
- New functionality requires corresponding tests
- Instrumented tests must pass before committing
- Performance regressions must be addressed
- iOS safety verification must pass for shared code changes

---

## Learning Protocol

When working on WorldWideWaves:

1. **Understand the Context**: Read existing code and documentation thoroughly
2. **Follow Patterns**: Use established architectural patterns and conventions
3. **Test Early**: Write tests alongside implementation
4. **Document Changes**: Update documentation for significant changes
5. **Performance First**: Consider performance implications of all changes
6. **Security Always**: Never compromise on security requirements

---

## Project Structure

```
WorldWideWaves/
├── shared/                     # KMM shared code
│   ├── src/commonMain/         # Common business logic
│   │   ├── kotlin/
│   │   │   ├── domain/         # Business logic
│   │   │   ├── data/           # Data layer
│   │   │   ├── ui/             # Shared UI (Compose)
│   │   │   ├── di/             # Dependency injection
│   │   │   ├── position/       # Position management
│   │   │   └── map/            # Map abstraction
│   ├── src/androidMain/        # Android-specific implementations
│   ├── src/iosMain/            # iOS-specific implementations
│   │   └── kotlin/
│   │       └── ui/
│   │           └── RootController.kt  # iOS ViewControllers
│   └── src/commonTest/         # Shared tests
├── composeApp/                 # Main application module
│   ├── src/androidMain/        # Android app implementation
│   └── src/androidInstrumentedTest/ # Android instrumented tests
├── iosApp/                     # iOS app
│   ├── worldwidewaves.xcodeproj
│   ├── worldwidewaves/
│   │   ├── AppDelegate.swift
│   │   ├── SceneDelegate.swift
│   │   └── IOSPlatformEnabler.swift
├── maps/                       # Map data modules
├── scripts/                    # Build and verification scripts
│   └── verify-ios-safety.sh   # iOS deadlock verification
└── docs/                       # Additional documentation
```

---

## Related Documentation

### iOS Development
- [CLAUDE_iOS.md](./CLAUDE_iOS.md) - Complete iOS development guide
- [docs/iOS_VIOLATION_TRACKER.md](docs/iOS_VIOLATION_TRACKER.md) - Deadlock violation status
- [docs/iOS_SUCCESS_STATE.md](docs/iOS_SUCCESS_STATE.md) - iOS success criteria
- [docs/iOS_DEBUGGING_GUIDE.md](docs/iOS_DEBUGGING_GUIDE.md) - Advanced debugging
- [iOS_MAP_IMPLEMENTATION_STATUS.md](iOS_MAP_IMPLEMENTATION_STATUS.md) - Map feature status
- [REMAINING_THREATS_AFTER_iOS_FIXES.md](REMAINING_THREATS_AFTER_iOS_FIXES.md) - Post-fix analysis

### Testing
- [docs/TEST_GAP_ANALYSIS.md](docs/TEST_GAP_ANALYSIS.md) - Coverage analysis
- [docs/COMPREHENSIVE_TEST_TODO.md](docs/COMPREHENSIVE_TEST_TODO.md) - Testing roadmap
- [docs/TESTING_STRATEGY.md](docs/TESTING_STRATEGY.md) - Testing approach
- [docs/UI_TESTING_GUIDE.md](docs/UI_TESTING_GUIDE.md) - UI testing guide

### Architecture
- [docs/architecture.md](docs/architecture.md) - System architecture
- [docs/ci-cd.md](docs/ci-cd.md) - CI/CD pipeline
- [docs/development.md](docs/development.md) - Development workflows
- [MAP_ARCHITECTURE_ANALYSIS.md](MAP_ARCHITECTURE_ANALYSIS.md) - Map subsystem

---

## Support and Escalation

For complex issues or architectural decisions:
1. Consult existing documentation and code patterns
2. Review similar implementations in the codebase
3. Consider performance and security implications
4. Test thoroughly with both unit and instrumented tests
5. Document decisions and rationale

---

**Last Updated**: October 1, 2025
**Version**: 2.1 (iOS Documentation Corrections + Verification)
**Maintainer**: WorldWideWaves Development Team
