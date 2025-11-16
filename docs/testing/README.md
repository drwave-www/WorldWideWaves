# WorldWideWaves Testing Documentation

> **Test Suite Status**: 100% pass rate
> **Coverage**: Production-ready with comprehensive test coverage

---

## Quick Start

### Run All Tests

```bash
# Run all unit tests
./gradlew :shared:testDebugUnitTest

# Run specific test suite
./gradlew :shared:testDebugUnitTest --tests "*.MapBoundsEnforcerUnitTest"

# Run Android instrumented tests (requires emulator)
ANDROID_SERIAL=emulator-5556 ./gradlew :composeApp:connectedDebugAndroidTest

# Run iOS safety verification
./scripts/verify-ios-safety.sh
```

### Expected Results

```
BUILD SUCCESSFUL
All tests completed, all passed, 0 failed, 0 skipped
```

---

## Test Organization

### Test Structure

```
shared/src/
├── commonTest/          # Platform-independent tests (NO MockK, NO JVM APIs)
│                       # Comprehensive tests covering shared business logic
├── androidUnitTest/     # Android-specific tests (CAN use MockK)
│                       # Android-specific implementation tests
└── iosTest/             # iOS-specific tests (NO MockK, Kotlin/Native compatible)
                        # iOS-specific implementation tests

composeApp/src/
└── androidInstrumentedTest/  # Android UI tests (requires emulator)
                              # Integration tests for MapLibre and UI workflows
```

### Test Categories

| Category | Coverage |
| ---------- | ---------- |

| **Domain Logic** | Core business logic, wave coordination, event scheduling |
| **Data Layer** | Repository, persistence, state management |
| **ViewModels** | UI state management, download coordination |
| **Map System** | MapLibre integration, bounds enforcement, gestures |
| **Position** | Position management, GPS, simulation |
| **Accessibility** | Screen reader, touch targets, contrast |
| **iOS Safety** | Deadlock prevention, thread safety |

---

## Core Documentation

### 1. Test Strategy

**[testing-strategy.md](../testing-strategy.md)** - Core testing philosophy and principles

**Key Principles**:

- Test real business logic, not mocks
- Avoid testing framework code or trivial logic
- Focus on critical user workflows
- Maintain 100% pass rate always

**What to Test**:

- ✅ Core domain logic (wave detection, scheduling, accuracy)
- ✅ Real integration points (Firebase, MapLibre, device coordination)
- ✅ Critical user workflows (wave participation, event discovery)

**What NOT to Test**:

- ❌ Mock implementations or interfaces
- ❌ Framework configuration (DI, logging, persistence)
- ❌ Trivial code (getters, setters, data classes)

### 2. Test Patterns

**[test-patterns.md](./test-patterns.md)** - Proven patterns for KMM testing

**Critical Patterns**:

#### Testing Infinite Flows

```kotlin
// ✅ CORRECT
observer.startObservation()
testScheduler.runCurrent()  // Process current emissions only
assertEquals(expected, observer.currentValue)
observer.stopObservation()  // Cancel infinite flow FIRST
testScheduler.advanceUntilIdle()  // Now safe
```

#### Testing ViewModels with Async State

```kotlin
// ✅ CORRECT
viewModel.loadEvents()
waitForEvents(viewModel, expectedSize, timeoutMs = 3000)
waitForState(viewModel.isLoading, false)
```

#### Test Isolation with Koin

```kotlin
// ✅ CORRECT
@AfterTest
fun tearDown() {
    runBlocking {
        testScopeProvider.cancelAllCoroutines()
        delay(500)  // Wait for cleanup propagation
    }
    stopKoin()
}
```

### 3. UI Testing Guide

**[ui-testing-guide.md](../ui-testing-guide.md)** - Android instrumented and iOS UI tests

**Critical Path Tests**:

- Wave participation workflow (countdown, choreography, sound coordination)
- Event discovery and filtering
- Core navigation and permissions
- Accessibility (TalkBack, VoiceOver, Dynamic Type)

**Test Categories**:

- `@Category(TestCategories.CRITICAL)` - Must pass before release
- `@Category(TestCategories.FEATURE)` - Feature coverage
- `@Category(TestCategories.ACCESSIBILITY)` - Screen reader support
- `@Category(TestCategories.PERFORMANCE)` - Animation smoothness

---

## Test Specifications

### Comprehensive Test Specifications

**[comprehensive-test-specifications.md](../comprehensive-test-specifications.md)** - Exhaustive test specs for map bounds enforcement

**Covers**:

1. Intelligent aspect ratio fitting (height-fit vs width-fit)
2. Min zoom formula parity (iOS/Android)
3. Min zoom locking mechanism
4. Preventive gesture interception
5. Viewport padding logic
6. BOUNDS vs WINDOW mode differences

**Comprehensive test coverage**: Unit and integration tests

### Map Screens Test Specification

**[map-screens-test-specification.md](../map-screens-test-specification.md)** - Test specs for 3 map screen types

**Three Map Screen Types**:

1. **Event Detail Screen Map**
   - Secondary read-only map
   - `BOUNDS` camera position
   - Gestures disabled

2. **Wave Participation Screen Map**
   - Primary participation screen
   - Intelligent auto-tracking
   - `BOUNDS` camera position with gesture interception

3. **Full Map Screen**
   - Dedicated interactive exploration
   - `WINDOW` camera position
   - Full gesture support with dynamic constraints

**Feature Coverage**:

- Camera behavior
- Gesture handling
- User position tracking
- Wave progression rendering

---

## Test Reports

### Test Coverage Report

**[test-coverage-final-report.md](../archive/testing-reports/test-coverage-final-report.md)** - Final coverage analysis (Phases 1-2)

**Summary**:

- Comprehensive test suite
- 100% pass rate
- Critical production bugs identified and fixed during test development

**Phase 1: CRITICAL Tests** ✅

- Wave progression observer
- Observation scheduler
- Wave hit accuracy
- Event participation flow (instrumented tests)

**Phase 2: Data Integrity** ✅

- Event state manager integration
- Favorite events store

### Test Gap Analysis

**[test-gap-analysis.md](../test-gap-analysis.md)** - Missing critical tests and quality issues

**Current Status**: 100% pass rate

**Critical Gaps Identified**:

- High-priority areas requiring tests
- Categories of potentially weak tests
- Recommendations for additional test coverage

**Focus Areas**:

- WaveProgressionTracker (core business logic)
- EventStateManager (state management)
- Accessibility testing (WCAG 2.1 compliance)

### Map Testing Implementation Summary

**[map-testing-implementation-summary.md](../map-testing-implementation-summary.md)** - Map testing framework overview

**Summary**:

- Comprehensive map testing coverage
- Headless MapView testing approach
- Integration tests prioritized over UI tests

**Test Categories**:

- BOUNDS Mode
- WINDOW Mode
- Bounds validation
- Safe bounds calculation
- Nearest valid point clamping
- Padding detection

**Integration Tests** (requires emulator):

- MapLibre visible region
- Screen-specific behavior

---

## Platform-Specific Testing

### Android Testing

#### Unit Tests

```bash
# Run all Android unit tests
./gradlew :shared:testDebugUnitTest

# Run specific Android-only test
./gradlew :shared:testDebugUnitTest --tests "*.WWWEventsTest"

# Generate test report
./gradlew :shared:testDebugUnitTest
# View: shared/build/reports/tests/testDebugUnitTest/index.html
```

#### Instrumented Tests (Requires Emulator)

```bash
# Start emulator
emulator -avd Pixel_4_API_34 -no-snapshot-load &

# Get emulator serial
adb devices

# Run instrumented tests
ANDROID_SERIAL=emulator-5556 ./gradlew :composeApp:connectedDebugAndroidTest

# Run specific test class
ANDROID_SERIAL=emulator-5556 ./gradlew :composeApp:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.worldwidewaves.map.MapLibreVisibleRegionTest
```

#### Test Categories

```bash
# Run critical tests only
ANDROID_SERIAL=emulator-5556 ./gradlew :composeApp:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.annotation=com.worldwidewaves.TestCategories.CRITICAL

# Run accessibility tests only
ANDROID_SERIAL=emulator-5556 ./gradlew :composeApp:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.annotation=com.worldwidewaves.TestCategories.ACCESSIBILITY
```

### iOS Testing

#### iOS Safety Verification (MANDATORY before commits)

```bash
# Run automated iOS safety checks
./scripts/verify-ios-safety.sh

# Expected: ZERO violations
```

**Checks for**:

- Composable-scoped KoinComponent (deadlock risk)
- `init{}` blocks with coroutine launches
- `init{}` blocks with DI access
- IOSSafeDI singleton existence
- File-level KoinComponent objects

#### iOS Unit Tests

```bash
# Compile iOS Kotlin code
./gradlew :shared:compileKotlinIosSimulatorArm64

# Run iOS-specific tests
./gradlew :shared:iosSimulatorArm64Test
```

#### iOS UI Tests (Xcode)

```bash
# Open Xcode project
cd iosApp
open worldwidewaves.xcodeproj

# Run UI tests in Xcode
# Product → Test (⌘U)
# Or select specific test: worldwidewavesUITests

# View test results
# View → Navigators → Test Navigator (⌘6)
```

#### iOS Simulator Logs

```bash
# View app logs in simulator
xcrun simctl spawn booted log stream \
  --predicate 'process == "WorldWideWaves"' \
  --level debug
```

### Accessibility Testing

**[accessibility-guide.md](../accessibility-guide.md)** - WCAG 2.1 Level AA compliance

**Test Script**:

```bash
# Run accessibility test suite
./scripts/test_accessibility.sh
```

**Manual Testing**:

- **Android**: Enable TalkBack, navigate entire app
- **iOS**: Enable VoiceOver, test with Dynamic Type at max size

**Required Validations**:

- [ ] All accessibility tests pass
- [ ] TalkBack navigation works without manual mode
- [ ] VoiceOver announces all critical events
- [ ] Touch targets verified ≥ 48dp/44pt
- [ ] Color contrast verified ≥ 4.5:1
- [ ] Text scales properly (Android: 200%, iOS: 300%)

---

## Common Commands Reference

### Essential Test Commands

```bash
# Run all tests (MANDATORY before every commit)
./gradlew clean :shared:testDebugUnitTest :composeApp:assembleDebug

# Run tests with detailed output
./gradlew :shared:testDebugUnitTest --info

# Run tests for specific package
./gradlew :shared:testDebugUnitTest --tests "com.worldwidewaves.shared.map.*"

# Run single test class
./gradlew :shared:testDebugUnitTest --tests "*.MapBoundsEnforcerUnitTest"

# Run single test method
./gradlew :shared:testDebugUnitTest --tests "*.MapBoundsEnforcerUnitTest.testBoundsMode_zeroPadding"

# Continue on failure (see all test results)
./gradlew :shared:testDebugUnitTest --continue
```

### Pre-Commit Verification Checklist

```bash
# 1. Unit tests (must pass 100%)
./gradlew :shared:testDebugUnitTest

# 2. iOS Kotlin compilation (zero warnings)
./gradlew :shared:compileKotlinIosSimulatorArm64

# 3. Android Kotlin compilation (zero warnings)
./gradlew :shared:compileDebugKotlinAndroid

# 4. Detekt (zero warnings)
./gradlew detekt

# 5. SwiftLint (zero errors, warnings acceptable if justified)
swiftlint lint iosApp/worldwidewaves --quiet

# 6. iOS safety verification
./scripts/verify-ios-safety.sh
```

**All checks must pass with ZERO errors before commit.**

### Test Debugging

```bash
# Run tests with stack traces
./gradlew :shared:testDebugUnitTest --stacktrace

# Run tests with full debug output
./gradlew :shared:testDebugUnitTest --debug

# Clean and rebuild
./gradlew clean :shared:testDebugUnitTest

# View test reports
open shared/build/reports/tests/testDebugUnitTest/index.html
```

---

## Test Quality Guidelines

### Test Requirements (from CLAUDE.md)

**CRITICAL RULES**:

- ✅ **ALWAYS run tests after commits** - Ensure no regressions
- ✅ **NEVER disable tests without permission** - Fix issues, don't hide them
- ✅ **NEVER disable tests to make them pass** - Tests validate business logic
- ✅ **Run ALL tests before commit** - Not just relevant tests
- ✅ **Self-verification requirement** - Test fixes yourself when possible

### Test-First Philosophy

**Tests must be logical and business-oriented**, not mirror current implementation:

- If tests fail → either business logic issue in code OR business requirements changed
- Tests validate business requirements, not implementation details
- Test modifications require explanation and user approval
- Document what business requirement changed

### Test Organization Standards

**Test Class Structure**:

```kotlin
class MyFeatureTest {
    // 1. COMPANION OBJECT (test data, constants)
    companion object {
        private const val TIMEOUT_MS = 3000
    }

    // 2. TEST FIXTURES (SUT, dependencies, test scheduler)
    private lateinit var featureUnderTest: MyFeature
    private lateinit var testScheduler: TestCoroutineScheduler

    // 3. SETUP
    @BeforeTest
    fun setup() {
        // Initialize test dependencies
    }

    // 4. TEARDOWN
    @AfterTest
    fun tearDown() {
        // Clean up resources
    }

    // 5. TEST METHODS (grouped by feature/scenario)
    @Test
    fun testFeatureScenario1() { }

    @Test
    fun testFeatureScenario2() { }
}
```

### Platform-Specific Test Requirements

**CommonTest** (shared/src/commonTest):

- ❌ NO MockK (not supported on iOS)
- ❌ NO JVM-only APIs
- ✅ Use Kotlin Test annotations
- ✅ Platform-independent business logic only

**AndroidUnitTest** (shared/src/androidUnitTest):

- ✅ CAN use MockK
- ✅ CAN use Android-specific APIs
- ✅ Test Android implementations only

**iOSTest** (shared/src/iosTest):

- ❌ NO MockK (Kotlin/Native limitation)
- ✅ Use expect/actual for platform-specific behavior
- ✅ Test iOS implementations only

---

## CI/CD Integration

### GitHub Actions Workflows

**Test Execution**:

- All tests run on every PR
- Both Android and iOS platforms verified
- Zero-tolerance for test failures

**Workflow Files**:

- `.github/workflows/android-tests.yml` - Android unit + instrumented tests
- `.github/workflows/ios-tests.yml` - iOS compilation + unit tests
- `.github/workflows/accessibility-tests.yml` - Accessibility compliance

### Local Pre-Push Hook

**`.git-hooks/pre-push`** (automatically runs before push):

```bash
#!/bin/bash
echo "Running tests before push..."
./gradlew :shared:testDebugUnitTest || exit 1
./scripts/verify-ios-safety.sh || exit 1
echo "✅ All checks passed. Proceeding with push."
```

---

## Test Metrics

### Current Test Statistics

| Metric | Value |
| -------- | ------- |

| **Pass Rate** | 100% |
| **Coverage** | Production-ready |
| **Platform Distribution** | Common, Android, iOS |

### Test Coverage by Layer

| Layer | Coverage |
| ------- | ---------- |

| Domain | Core business logic, wave coordination, event scheduling |
| Data | Repository, persistence, state management |
| ViewModels | UI state management, download coordination |
| Map System | MapLibre integration, bounds enforcement, gestures |
| Position | Position management, GPS, simulation |
| Accessibility | Screen reader, touch targets, contrast - Complete |
| iOS Safety | Deadlock prevention, thread safety - All violations fixed |

---

## Troubleshooting

### Common Test Issues

#### Tests Hang Indefinitely

```kotlin
// ❌ PROBLEM: advanceUntilIdle() on infinite flow
testScheduler.advanceUntilIdle()

// ✅ SOLUTION: Use runCurrent() and cancel flow
testScheduler.runCurrent()
flow.cancel()
testScheduler.advanceUntilIdle()
```

#### Test Isolation Failures

```kotlin
// ❌ PROBLEM: Koin not stopped between tests
@AfterTest
fun tearDown() {
    // Missing stopKoin()
}

// ✅ SOLUTION: Proper cleanup
@AfterTest
fun tearDown() {
    runBlocking {
        testScopeProvider.cancelAllCoroutines()
        delay(500)
    }
    stopKoin()
}
```

#### Flaky ViewModel Tests

```kotlin
// ❌ PROBLEM: Not waiting for async state
assertEquals(expected, viewModel.events.value)

// ✅ SOLUTION: Use helper functions
waitForEvents(viewModel, expectedSize, timeoutMs = 3000)
waitForState(viewModel.isLoading, false)
```

### Instrumented Test Issues

#### Emulator Not Found

```bash
# List available emulators
emulator -list-avds

# Start specific emulator
emulator -avd Pixel_4_API_34 -no-snapshot-load &

# Verify connection
adb devices
```

#### Test APK Installation Fails

```bash
# Clear app data
adb shell pm clear com.worldwidewaves

# Uninstall test APK
adb uninstall com.worldwidewaves.test

# Clean and rebuild
./gradlew clean :composeApp:assembleDebugAndroidTest
```

---

## Future Test Improvements

### Planned Enhancements

**Phase 5: Performance Testing** (Q1 2026)

- [ ] Animation frame rate monitoring
- [ ] Memory leak detection
- [ ] Battery consumption tests
- [ ] Network latency simulation

**Phase 6: Chaos Engineering** (Q2 2026)

- [ ] Simulate GPS dropout during wave
- [ ] Network failures during event download
- [ ] Low battery scenarios
- [ ] Airplane mode transitions

**Phase 7: iOS UI Tests** (Q2 2026)

- [ ] Port Android instrumented tests to iOS
- [ ] XCTest suite for critical workflows
- [ ] Accessibility Inspector integration

### Known Test Gaps

**From test-gap-analysis.md**:

1. WaveProgressionTracker (needs additional coverage)
2. EventStateManager edge cases
3. Complex user position scenarios
4. Error recovery workflows

---

## Additional Resources

### Documentation

- [Architecture Overview](../architecture.md) - System architecture and design
- [iOS Development Guide](../CLAUDE_iOS.md) - Complete iOS setup and debugging
- [CI/CD Pipeline](../ci-cd.md) - Continuous integration workflows
- [Development Workflows](../development.md) - Development best practices

### External Resources

- [Kotlin Test Documentation](https://kotlinlang.org/api/latest/kotlin.test/)
- [Compose Testing](https://developer.android.com/jetpack/compose/testing)
- [Turbine Documentation](https://github.com/cashapp/turbine) - Flow testing library
- [XCTest Guide](https://developer.apple.com/documentation/xctest) - iOS testing

---

## Support and Questions

### Getting Help

1. **Review existing documentation** - Check this README and linked docs
2. **Search test files** - Look for similar test patterns in codebase
3. **Run verification scripts** - Use automated checks to identify issues
4. **Consult CLAUDE.md** - Review project-specific testing requirements

### Contributing Tests

**Before adding new tests**:

1. Review [testing-strategy.md](../testing-strategy.md) - Understand what to test
2. Review [test-patterns.md](./test-patterns.md) - Use proven patterns
3. Run existing tests - Ensure 100% pass rate
4. Follow test organization standards - Consistent structure

**After adding tests**:

1. Verify 100% pass rate locally
2. Update relevant documentation if needed
3. Run pre-commit verification checklist
4. Include test count in commit message

---

**Version**: 1.0
**Maintainer**: WorldWideWaves Development Team
