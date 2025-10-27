# Frequently Asked Questions (FAQ)

Quick answers to common questions about WorldWideWaves development, setup, and troubleshooting.

---

## Table of Contents

- [General Questions](#general-questions)
- [Development Setup Questions](#development-setup-questions)
- [iOS Development Questions](#ios-development-questions)
- [Android Development Questions](#android-development-questions)
- [Testing Questions](#testing-questions)
- [Contributing Questions](#contributing-questions)
- [Build & CI/CD Questions](#build--cicd-questions)
- [Map & Location Questions](#map--location-questions)
- [Troubleshooting](#troubleshooting)

---

## General Questions

### What is WorldWideWaves?

WorldWideWaves is a Kotlin Multiplatform mobile app that orchestrates synchronized human waves through cities worldwide, fostering unity and shared experience through real-time coordination. See [README.md](../README.md) for full overview.

### Which cities are supported?

WorldWideWaves includes offline maps for **40+ major global cities** including Paris, New York, Tokyo, Rio de Janeiro, and more. Each city has its own Dynamic Feature map module. See `maps/` directory for complete list.

### Can I use the app offline?

**Yes**, all map data is stored offline using MapLibre vector tiles. Events are loaded at app startup, so you can navigate and prepare for waves without internet connection (GPS required for position tracking).

### Is this open source?

Yes, WorldWideWaves is licensed under **Apache License 2.0**. See [LICENSE](../LICENSE) for details.

### What platforms are supported?

- **Android**: API 26+ (Android 8.0+), target SDK 36
- **iOS**: iOS 14+ via Compose Multiplatform
- **Code sharing**: ~70% code reuse between platforms

### What tech stack does WorldWideWaves use?

- **Core**: Kotlin 2.2.0, Kotlin Multiplatform, Compose Multiplatform 1.8.2
- **Maps**: MapLibre (Android 11.13.0, iOS 6.8.0)
- **DI**: Koin 4.1.0
- **Backend**: Firebase Analytics & Crashlytics
- See [README.md - Tech Stack](../README.md#tech-stack) for complete details.

---

## Development Setup Questions

### How do I set up my development environment?

**Minimum requirements**: JDK 17, Android Studio 2024.1+, 8GB RAM, 20GB disk space. For iOS: macOS with Xcode 15.0+. See [Environment Setup Guide](environment-setup.md) for platform-specific installation instructions.

### Which IDE should I use?

- **Android Studio** (recommended for Android and shared code)
- **Xcode** (required for iOS development on macOS)
- **Fleet** or **IntelliJ IDEA** (alternative for shared Kotlin code)

### How do I run tests?

```bash
# Unit tests (902+ tests, ~22 seconds)
./gradlew :shared:testDebugUnitTest

# Android UI tests (requires emulator)
./gradlew :composeApp:connectedDebugAndroidTest

# All quality checks
./gradlew :shared:testDebugUnitTest ktlintCheck detekt
```
See [Development Workflow - Testing](development.md#testing) for detailed test commands.

### How do I build for iOS?

**From Xcode (recommended)**:
```bash
cd iosApp && open worldwidewaves.xcodeproj
# Select scheme, press Cmd+R
```

**From command line**:
```bash
./gradlew :shared:embedAndSignAppleFrameworkForXcode
cd iosApp && xcodebuild -project worldwidewaves.xcodeproj -scheme worldwidewaves build
```
See [README.md - Run iOS](../README.md#5-run-ios-macos-only) and [CLAUDE_iOS.md](../CLAUDE_iOS.md) for complete iOS setup.

### What are the system requirements?

**Minimum**: 4-core CPU, 8GB RAM, 20GB disk. **Recommended**: 8+ cores (M1/M2 or equivalent), 16-32GB RAM, 50GB SSD, 1920x1080 display. See [Environment Setup - Hardware Requirements](environment-setup.md#hardware-requirements).

### Do I need Firebase credentials?

Yes, Firebase is required for analytics and crashlytics. Create `local.properties` with Firebase credentials, then run `./gradlew generateFirebaseConfig`. See [Firebase Setup Guide](setup/firebase-setup.md).

### How do I install git hooks?

```bash
./dev/setup-git-hooks.sh
```
This enables automatic emulator launch, translation updates (optional), and pre-push integration tests. Skip tests with `SKIP_INTEGRATION_TESTS=1 git push`.

---

## iOS Development Questions

### Why does my iOS app crash on launch?

**Most common cause**: Deadlock violations (coroutines in init blocks or objects inside @Composable functions). Run verification script:
```bash
./scripts/verify-ios-safety.sh
```
See [CLAUDE_iOS.md - iOS Deadlock Prevention Rules](../CLAUDE_iOS.md#-ios-deadlock-prevention-rules-mandatory) for patterns to avoid.

### How do I fix iOS threading issues?

Use `IOSSafeDI` singleton for dependency injection instead of inline objects. Avoid `runBlocking`, coroutine launches, or DI access in `init{}` blocks. Use parameter injection or lazy initialization. See [CLAUDE_iOS.md](../CLAUDE_iOS.md) for complete rules and examples.

### Where are iOS logs?

**In Xcode**: View > Debug Area > Show Debug Area, filter by "WWW"

**From command line**:
```bash
xcrun simctl spawn booted log stream --predicate 'process == "WorldWideWaves"' --level debug
```
See [iOS Debugging Guide](ios/ios-debugging-guide.md#-monitor-complete-initialization-flow) for log monitoring patterns.

### Why isn't my ViewModel working on iOS?

Likely using Android-only dependencies (androidx.lifecycle) in commonMain or accessing Dispatchers.Main during property initialization. Exclude Android dependencies from iOS configurations and use lazy initialization. See [iOS Debugging Guide - Phase 2](ios/ios-debugging-guide.md#phase-2-apply-targeted-exclusions).

### How do I verify iOS safety before committing?

```bash
./scripts/verify-ios-safety.sh
```
Expected: **ZERO violations** (no @Composable-scoped KoinComponent, no init{} coroutines, no init{} DI access). See [CLAUDE_iOS.md - Automated Verification](../CLAUDE_iOS.md#-automated-verification).

### Which iOS simulator should I use?

**iPhone 15 Pro (iOS 18+, arm64)** is recommended for testing Compose Multiplatform and MapLibre features with optimal performance on Apple Silicon Macs.

---

## Android Development Questions

### How do I run on an emulator?

```bash
# List available AVDs
emulator -list-avds

# Launch specific AVD
emulator -avd Pixel_3a_API_30 &

# Install and run app
./gradlew :composeApp:installDebug
```
See [Development Workflow - Android Development](development.md#android-development) for hot reload and debug instructions.

### Why is Gradle sync failing?

Try refreshing dependencies and cleaning:
```bash
./gradlew --refresh-dependencies
./gradlew clean build
```
If still failing, check JDK version (`java -version` should show 17), verify `local.properties` SDK path, and ensure Firebase config exists.

### How do I debug map tile loading?

Check logcat for MapLibre errors:
```bash
adb logcat | grep -E "MapLibre|Tile|MBTiles"
```
Verify `.mbtiles` file exists in map module assets and Dynamic Feature module is included in `settings.gradle.kts`. See [Development Workflow - MapLibre not loading maps](development.md#common-issues).

### What are product flavors for?

WorldWideWaves uses **Dynamic Feature modules** for maps (not product flavors). Each city is a separate on-demand module, reducing initial APK size. Map modules are in `maps/` directory.

### How do I run instrumented tests?

```bash
# Ensure emulator is running and detected
adb devices

# Run all instrumented tests
ANDROID_SERIAL=emulator-5556 ./gradlew :composeApp:connectedDebugAndroidTest

# Run specific test category (e.g., accessibility)
./gradlew :composeApp:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.annotation=com.worldwidewaves.test.Accessibility
```
See [Development Workflow - UI Tests](development.md#ui-tests) for screenshot and integration test commands.

### How do I view Android logs?

```bash
# View all app logs
adb logcat -s WWW

# View specific component
adb logcat | grep "PositionManager"

# Save to file
adb logcat > logcat.txt
```
See [Development Workflow - Android Debugging](development.md#android-debugging).

---

## Testing Questions

### How many tests exist in WorldWideWaves?

**902+ unit tests** with 100% pass rate, executing in ~22 seconds. Comprehensive coverage across domain logic, ViewModels, data layer, and iOS safety. See [CLAUDE.md - Testing Requirements](../CLAUDE.md#testing-requirements).

### How long do tests take to run?

Unit tests: **~22 seconds** on modern hardware. Android instrumented tests: **~5-10 minutes** (depends on emulator speed). Performance budget: unit tests must complete in <30s.

### Why did my test fail?

Common causes:
- **Flaky timing**: Use `runTest` with virtual time instead of delays
- **Infinite flows not cancelled**: Call `stopObservation()` before `advanceUntilIdle()`
- **Koin cleanup**: Ensure `stopKoin()` in `@AfterTest` to prevent interference
- **iOS deadlocks**: Run `./scripts/verify-ios-safety.sh` to detect violations

See [Testing Strategy](testing-strategy.md) for testing patterns.

### How do I write a new test?

Follow **"Test Real Code, Not Mocks"** philosophy:
```kotlin
@Test
fun methodName_scenario_expectedResult() {
    // Arrange: Setup real objects
    val repository = RealRepository(...)

    // Act: Execute business logic
    val result = repository.doWork()

    // Assert: Verify behavior
    assertEquals(expected, result)
}
```
Use real implementations, mock only external dependencies (network, sensors). See [Testing Strategy - Testing Patterns](testing-strategy.md#testing-patterns-by-component-type).

### What's the testing philosophy?

**"Test Real Code, Not Mocks"**: Focus on business logic, use real implementations, avoid testing framework internals or trivial code. Tests validate business requirements, not implementation details. Never disable tests to make them pass. See [Testing Strategy - Core Philosophy](testing-strategy.md#core-philosophy).

### How do I run tests for a specific module?

```bash
# Shared module only
./gradlew :shared:testDebugUnitTest

# Specific package
./gradlew :shared:testDebugUnitTest --tests "com.worldwidewaves.shared.domain.observation.*"

# Android app tests
./gradlew :composeApp:testDebugUnitTest
```

---

## Contributing Questions

### How do I contribute to WorldWideWaves?

1. Read [Contributing Guide](contributing.md) and [Environment Setup](environment-setup.md)
2. Check existing [GitHub issues](https://github.com/mglcel/WorldWideWaves/issues)
3. Fork repository and create feature branch (`feat/your-feature-name`)
4. Follow commit convention: `<type>(<scope>): <subject>`
5. Add tests for new functionality and ensure all CI checks pass
6. Submit pull request with clear description

See [Contributing Guide](contributing.md) for complete process.

### What's the code review process?

PRs require:
1. **Automated checks passing**: Quality gates (lint, tests, build), all workflows green
2. **At least 1 approval** from maintainers
3. **All comments addressed**
4. **"Squash and merge"** for clean history

See [Contributing - Review Process](contributing.md#review-process).

### How do I report bugs?

Open a GitHub issue with:
- Clear bug description and reproduction steps
- Expected vs actual behavior
- Environment (device, OS, app version)
- Screenshots/logs (use `adb logcat` for Android)

See [Contributing - Bug Reports](contributing.md#bug-reports) for template.

### What's the commit message format?

Use **Conventional Commits**:
```
<type>(<scope>): <subject>

<body>

<footer>
```

Examples:
- `feat(maps): add Tokyo offline map module`
- `fix(position): prevent duplicate emissions`
- `docs: update environment setup guide`
- `test: add choreography integration tests`

See [Contributing - Commit Convention](contributing.md#commit-convention).

### What branch naming convention should I use?

```
<type>/<short-description>

Examples:
feat/add-tokyo-map
fix/position-update-bug
refactor/choreography-engine
docs/update-setup-guide
test/add-accessibility-tests
```
See [Contributing - Branch Naming](contributing.md#branch-naming).

### How do I add a new city map?

Create a new Dynamic Feature module in `maps/` directory with MapLibre `.mbtiles` file and appropriate styling. Include module in `settings.gradle.kts`. See map generation scripts in `scripts/maps/`.

---

## Build & CI/CD Questions

### Why is my build failing?

Common causes:
- **Out of memory**: Increase heap in `gradle.properties`: `org.gradle.jvmargs=-Xmx8g`
- **Missing Firebase config**: Run `./gradlew generateFirebaseConfig`
- **Wrong JDK version**: Use JDK 17 (`java -version`)
- **Outdated dependencies**: Run `./gradlew --refresh-dependencies`

See [Development Workflow - Troubleshooting Guide](development.md#troubleshooting-guide).

### How do I clean build artifacts?

```bash
# Clean all build outputs
./gradlew clean

# Clean and rebuild
./gradlew clean build

# Clean iOS framework specifically
./gradlew :shared:clean
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64
```

### What are the quality gates in CI/CD?

Multi-stage pipeline:
1. **Build verification**: Android and iOS compilation
2. **Code quality**: ktlint, detekt, security scanning
3. **Tests**: 902+ unit tests, instrumented UI tests
4. **E2E**: Firebase Test Lab multi-device testing
5. **Performance**: Nightly regression detection

See [CI/CD Documentation](ci-cd.md) for complete pipeline details.

### How do I run linting locally?

```bash
# Check code style
./gradlew ktlintCheck

# Auto-format code
./gradlew ktlintFormat

# Static analysis
./gradlew detekt
```
All linting must pass before commit. See [Development Workflow - Code Quality](development.md#code-quality).

### Can I skip tests when pushing?

**Not recommended**, but git hooks can be skipped:
```bash
SKIP_INTEGRATION_TESTS=1 git push
```
However, CI pipeline will still enforce all tests. **Never commit code with failing tests**.

### How long does the CI pipeline take?

- **Build verification**: ~5-10 minutes
- **Quality gates**: ~3-5 minutes
- **UI tests**: ~10-15 minutes
- **Full E2E pipeline**: ~20-30 minutes

Optimized with parallel execution and caching.

---

## Map & Location Questions

### How do offline maps work?

WorldWideWaves uses **MapLibre** with pre-generated **MBTiles** (vector tiles) stored in Dynamic Feature modules. Each city module contains all map data, eliminating network dependency for rendering.

### Why isn't my position updating?

**Solutions**:
1. Grant location permissions: Settings > Apps > WorldWideWaves > Permissions
2. Enable GPS (test outdoors or use emulator location)
3. Check `PositionManager` logs: `adb logcat | grep "PositionManager"`
4. For simulation mode, verify simulation is active

See [Development Workflow - Position not updating](development.md#common-issues).

### How do I test with simulated locations?

**Android**:
```bash
# Enable mock locations
adb shell settings put secure mock_location 1

# Grant permissions
adb shell pm grant com.worldwidewaves android.permission.ACCESS_FINE_LOCATION
```

**iOS**:
```bash
# Set custom location (San Francisco)
xcrun simctl location "iPhone 15" set 37.7749,-122.4194
```

### What's the difference between GPS and simulation mode?

**GPS mode**: Uses real device GPS (FusedLocationProvider on Android, CoreLocation on iOS). **Simulation mode**: Uses programmatically set positions for testing wave choreography. PositionManager prioritizes simulation when active (for testing), otherwise uses GPS.

### How accurate is position tracking?

Uses high-accuracy location providers (~5-10m accuracy outdoors). Position updates are debounced (500ms) and deduplicated to optimize battery and reduce state emissions.

---

## Troubleshooting

### Android Studio won't launch

Increase VM heap size in `studio.vmoptions`:
```
-Xmx4g
```
Location: `/Applications/Android Studio.app/Contents/bin/studio.vmoptions` (macOS)

### Xcode command-line tools not found

```bash
sudo xcode-select --switch /Applications/Xcode.app/Contents/Developer
sudo xcode-select --install
```

### KVM permission denied (Linux)

```bash
sudo usermod -aG kvm $USER
# Logout and login
```

### Emulator won't start (Linux)

Install required 32-bit libraries:
```bash
sudo apt install libc6:i386 libncurses5:i386 libstdc++6:i386 lib32z1 libbz2-1.0:i386
```

### iOS framework not found

```bash
./gradlew :shared:clean
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64

# Verify framework exists
ls -la shared/build/xcode-frameworks/Debug/iphonesimulator*/Shared.framework
```

### MapLibre map not rendering

**Android**: Check `.mbtiles` file exists in map module assets, verify module in `settings.gradle.kts`, check logcat for MapLibre errors.

**iOS**: Verify `shouldChangeFrom` delegate is properly configured, check map bounds are valid (northeast > southwest).

### Firebase not connecting

1. Verify `google-services.json` exists in `composeApp/`
2. Check Firebase credentials in `local.properties`
3. Run `./gradlew generateFirebaseConfig`
4. Check network connectivity and Firebase project status

### Tests failing with "No mocks found"

WorldWideWaves uses **real implementations** per testing strategy. Don't create mocks for business logic. Use real objects and mock only external dependencies (network, sensors). See [Testing Strategy](testing-strategy.md).

### App crashes with NullPointerException

Likely unsafe `!!` operator in production code. Search codebase for `!!` and replace with safe calls (`?.`) or `requireNotNull()` with descriptive error message. See [CLAUDE.md - Force Unwrap Elimination](../CLAUDE.md#2-force-unwrap--elimination-is-critical).

### Gradle build "Out of Memory"

Edit `gradle.properties`:
```properties
org.gradle.jvmargs=-Xmx8g -XX:MaxDirectMemorySize=8g
```

### Git hooks not working

Re-run setup:
```bash
./dev/setup-git-hooks.sh
```
Verify hooks installed: `ls -la .git/hooks/`

---

## Need More Help?

- **Documentation Index**: [docs/README.md](README.md)
- **Complete Development Guide**: [CLAUDE.md](../CLAUDE.md)
- **iOS Development**: [CLAUDE_iOS.md](../CLAUDE_iOS.md)
- **GitHub Issues**: [Issues Page](https://github.com/mglcel/WorldWideWaves/issues)
- **GitHub Discussions**: [Discussions](https://github.com/mglcel/WorldWideWaves/discussions)

---

**Last Updated**: October 27, 2025
**Maintainer**: WorldWideWaves Development Team
