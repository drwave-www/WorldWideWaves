# Development Workflow

Local development guide for WorldWideWaves, covering the run/test/debug cycle, common pitfalls, and troubleshooting.

## Development Loop

### Android Development

**Quick Start:**
```bash
# Open in Android Studio
open -a "Android Studio" /path/to/WorldWideWaves

# Or command-line build
./gradlew :composeApp:assembleDebug

# Install on device
./gradlew :composeApp:installDebug
```

**Hot Reload:**
- Compose UI supports hot reload in Android Studio
- Code changes reflect instantly without full rebuild
- Press Ctrl+Shift+F9 (Cmd+Shift+F9 on Mac) to apply changes

**Debug Mode:**
```bash
# Run with debugger attached
./gradlew :composeApp:installDebug
adb shell am start -D -n com.worldwidewaves/.activities.MainActivity

# Attach debugger in Android Studio
Run > Attach Debugger to Android Process
```

### iOS Development

**Quick Start:**
```bash
# Build iOS framework
./gradlew :shared:embedAndSignAppleFrameworkForXcode

# Open Xcode project
open iosApp/worldwidewaves.xcodeproj

# Build in Xcode: Cmd+B
# Run on Simulator: Cmd+R
```

**Framework Refresh:**
When changing shared Kotlin code:
```bash
# Clean and rebuild framework
./gradlew :shared:clean
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64

# Or use Xcode build script (automatic)
# Build Phases > Run Script already configured
```

**Debug Mode:**
- Set breakpoints in Swift code (Xcode)
- Use `print()` statements for Kotlin code debugging
- View logs: Xcode > View > Debug Area > Show Debug Area

## Testing

### Unit Tests

**Run all unit tests:**
```bash
# Shared module tests (902+ tests)
./gradlew :shared:testDebugUnitTest

# Android app tests
./gradlew :composeApp:testDebugUnitTest

# Generate HTML report
# Open: shared/build/reports/tests/testDebugUnitTest/index.html
```

**Run specific test:**
```bash
./gradlew :shared:testDebugUnitTest --tests "com.worldwidewaves.shared.domain.observation.*"
```

**Skip common tests (if needed):**
```bash
./gradlew :shared:testDebugUnitTest -PdisableCommonTest
```

### UI Tests

**Prerequisites:**
1. Launch Android emulator or connect device
2. Verify device with `adb devices`
3. Ensure location permissions granted

**Run all UI tests:**
```bash
# Specify device serial if multiple devices connected
ANDROID_SERIAL=emulator-5556 ./gradlew :composeApp:connectedDebugAndroidTest
```

**Run specific test category:**
```bash
# Critical path tests
./gradlew :composeApp:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.annotation=com.worldwidewaves.test.CriticalPath

# Accessibility tests
./gradlew :composeApp:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.annotation=com.worldwidewaves.test.Accessibility
```

**Test with screenshots:**
```bash
# Run screenshot tests
./gradlew :composeApp:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.worldwidewaves.compose.screenshots.ScreenshotTests

# Pull screenshots from device
adb pull /sdcard/Pictures/Screenshots ./screenshots/
```

### Integration Tests

**Real device integration tests:**
```bash
# Enable mock locations in developer options
adb shell settings put secure mock_location 1

# Grant location permissions
adb shell pm grant com.worldwidewaves android.permission.ACCESS_FINE_LOCATION
adb shell pm grant com.worldwidewaves android.permission.ACCESS_COARSE_LOCATION

# Run integration tests
./gradlew runRealIntegrationTests
```

### Performance Tests

**Run performance test suite:**
```bash
# Fast unit tests (100ms budget)
./gradlew testFast

# Full performance tests
./gradlew :composeApp:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.annotation=com.worldwidewaves.test.Performance
```

### Sound Choreography Tests

**Mathematical simulation:**
```bash
./gradlew crowdSoundSimulation
```

**Real audio playback:**
```bash
# Ensure device audio is enabled
ANDROID_SERIAL=emulator-5556 ./gradlew :composeApp:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.worldwidewaves.compose.choreographies.RealAudioCrowdSimulationTest
```

**Interactive audio demo:**
```bash
# Install app
./gradlew :composeApp:installDebug

# Launch audio test activity
adb shell am start -n com.worldwidewaves/.debug.AudioTestActivity
```

### Test Quality

**Detect anti-patterns:**
```bash
./scripts/detect-test-antipatterns.sh
```

**Run quality checks:**
```bash
./gradlew testQuality
```

## Code Quality

### Linting

**Run ktlint:**
```bash
# Check all modules
./gradlew ktlintCheck

# Auto-format code
./gradlew ktlintFormat
```

**Run detekt:**
```bash
# Static analysis
./gradlew detekt

# View report
open build/reports/detekt/detekt.html
```

### License Compliance

**Generate license report:**
```bash
./gradlew generateLicenseReport

# View report
open build/reports/licenses/licenses-gradle.json
```

## Git Workflow

### Git Hooks Setup

**One-time setup:**
```bash
./dev/setup-git-hooks.sh
```

**Features enabled:**
- Automatic Android emulator launch for integration testing
- Translation updates (requires `OPENAI_API_KEY` in `local.properties`)
- Critical integration tests before push
- Automatic backup of existing custom hooks

**Skip integration tests:**
```bash
SKIP_INTEGRATION_TESTS=1 git push
```

### Commit Convention

Follow Conventional Commits:

```
<type>(<scope>): <subject>

<body>

<footer>
```

**Types:**
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `refactor`: Code refactoring
- `test`: Adding or updating tests
- `chore`: Build process or tooling changes
- `perf`: Performance improvements

**Examples:**
```
feat(choreography): add crowd sound simulation

Implements mathematical simulation of 50 people playing MIDI notes
simultaneously with timing variations to verify song recognizability.

Closes #123
```

```
fix(position): prevent duplicate position emissions

Apply debounce (500ms) and distinctUntilChanged() to position stream
to reduce StateFlow emissions by 80%.

Fixes #456
```

### Branch Strategy

**Main Branches:**
- `main` - Production-ready code
- `develop` - Integration branch (if used)

**Feature Branches:**
```bash
# Create feature branch
git checkout -b feat/add-new-city-map

# Work on feature
git add .
git commit -m "feat(maps): add Tokyo map module"

# Push and create PR
git push origin feat/add-new-city-map
```

**Release Branches:**
```bash
git checkout -b release/v0.23
# Update version in build.gradle.kts
git commit -m "chore: bump version to v0.23"
git push origin release/v0.23
```

## Debugging

### Android Debugging

**Logcat filtering:**
```bash
# View all app logs
adb logcat -s WWW

# View specific component
adb logcat | grep "PositionManager"

# Save to file
adb logcat > logcat.txt
```

**Verbose logging:**
Enable in `composeApp/build.gradle.kts`:
```kotlin
buildConfigField("boolean", "ENABLE_VERBOSE_LOGGING", "true")
```

**Performance profiling:**
- Android Studio > View > Tool Windows > Profiler
- CPU, Memory, Network, Energy profiling available
- Record traces for detailed analysis

### iOS Debugging

**Console logs:**
- Xcode > View > Debug Area > Show Debug Area
- Filter by "WWW" or component name

**Breakpoints:**
- Set breakpoints in Swift code
- Use conditional breakpoints for specific scenarios
- Symbolic breakpoints for Objective-C exceptions

**Instruments:**
```bash
# Launch Instruments for profiling
open -a Instruments
```

### Common Issues

**Issue: Gradle build fails with "Out of Memory"**

Solution:
```bash
# Increase heap size in gradle.properties
org.gradle.jvmargs=-Xmx8g
```

**Issue: Android emulator won't start**

Solutions:
```bash
# Check available AVDs
emulator -list-avds

# Launch specific AVD
emulator -avd Pixel_3a_API_30 &

# Check for KVM acceleration (Linux)
sudo kvm-ok
```

**Issue: iOS framework not found**

Solution:
```bash
# Clean and rebuild
./gradlew :shared:clean
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64

# Verify framework exists
ls -la shared/build/xcode-frameworks/Debug/iphonesimulator*/Shared.framework
```

**Issue: MapLibre not loading maps**

Solutions:
1. Verify map module included in `settings.gradle.kts`
2. Check Dynamic Feature configuration
3. Ensure map `.mbtiles` file exists in assets
4. Check logcat for MapLibre errors

**Issue: Position not updating**

Solutions:
1. Grant location permissions: Settings > Apps > WorldWideWaves > Permissions
2. Enable mock locations in developer options
3. Check GPS signal (try outdoors or use emulator location)
4. Verify `PositionManager` logs in logcat

**Issue: Tests failing with "No mocks found"**

Solution: Use real implementations instead of mocks per testing strategy.

**Issue: Firebase not connecting**

Solutions:
1. Verify `google-services.json` exists in `composeApp/`
2. Check Firebase project configuration
3. Ensure API keys in `local.properties`
4. Run `./gradlew generateFirebaseConfig`

## Performance Tips

### Build Performance

**Enable Gradle daemon:**
```properties
# gradle.properties
org.gradle.daemon=true
org.gradle.parallel=true
org.gradle.caching=true
```

**Increase workers:**
```properties
org.gradle.workers.max=4
```

**Use configuration cache:**
```bash
./gradlew build --configuration-cache
```

### IDE Performance

**Exclude build directories:**
- Android Studio > Settings > Editor > File Types
- Add to "Ignore files and folders": `*.iml;build;.gradle;node_modules;`

**Increase IDE memory:**
- Help > Edit Custom VM Options
- Add: `-Xmx4g -XX:ReservedCodeCacheSize=512m`

### Runtime Performance

**Enable R8 (release builds):**
Already enabled in `composeApp/build.gradle.kts`:
```kotlin
buildTypes {
    release {
        isMinifyEnabled = true
        isShrinkResources = true
    }
}
```

**Monitor memory leaks:**
- Use LeakCanary (add dependency for debug builds)
- Profile with Android Studio Memory Profiler

## Troubleshooting Guide

### Build Issues

| Symptom | Likely Cause | Solution |
|---------|--------------|----------|
| Gradle sync fails | Dependency resolution | `./gradlew --refresh-dependencies` |
| Compile errors | Outdated cache | `./gradlew clean build` |
| iOS build fails | Framework not generated | Rebuild framework with Gradle |
| Map module not found | Missing in settings.gradle.kts | Add `:maps:city_name` to includes |

### Runtime Issues

| Symptom | Likely Cause | Solution |
|---------|--------------|----------|
| App crashes on launch | Firebase config missing | Run `generateFirebaseConfig` |
| Map not rendering | Tile source error | Check .mbtiles file exists |
| Position stuck | Location permissions | Grant permissions in settings |
| Audio not playing | AudioManager not initialized | Check Koin DI setup |

### Test Issues

| Symptom | Likely Cause | Solution |
|---------|--------------|----------|
| Tests timeout | Emulator slow | Use faster AVD (API 28-30) |
| Flaky tests | Race conditions | Add proper synchronization |
| Screenshots differ | Font/rendering variance | Use consistent emulator config |
| Performance tests fail | Background processes | Close other apps |

## Development Best Practices

### Code Style

**Kotlin conventions:**
- 4-space indentation
- 120 character line limit
- No wildcard imports
- Explicit return types for public APIs

**Compose best practices:**
- Use `remember` for expensive computations
- Hoist state to appropriate level
- Use `LaunchedEffect` for side effects
- Avoid side effects in composition

### Dependency Injection

**Koin usage:**
```kotlin
// Define module
val myModule = module {
    single { MyRepository(get()) }
    viewModel { MyViewModel(get()) }
}

// Use in Composable
@Composable
fun MyScreen() {
    val viewModel: MyViewModel = koinViewModel()
}
```

### Coroutines

**Use appropriate dispatcher:**
```kotlin
// UI updates
withContext(Dispatchers.Main) { updateUI() }

// CPU-bound work
withContext(Dispatchers.Default) { calculateDistance() }

// No Dispatchers.IO (use suspend functions)
```

**Structured concurrency:**
```kotlin
viewModelScope.launch {
    try {
        val result = performOperation()
        _state.value = Success(result)
    } catch (e: Exception) {
        _state.value = Error(e)
    }
}
```

### Testing Philosophy

**Test real code, not mocks:**
- Focus on business logic and integration points
- Use real implementations where possible
- Mock only external dependencies (network, device sensors)
- Avoid testing framework internals

**Test pyramid:**
```
       /\
      /E2E\       Few, critical user journeys
     /------\
    /  UI   \     Important interactions
   /----------\
  /   Unit     \  Many, fast business logic tests
 /--------------\
```

## Further Reading

- [Architecture](architecture.md)
- [CI/CD Pipeline](ci-cd.md)
- [Environment Setup](environment-setup.md)
- [Contributing Guidelines](contributing.md)
