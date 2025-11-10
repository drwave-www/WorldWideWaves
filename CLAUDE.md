# WorldWideWaves - Claude Code Instructions

> **Quick Links**: [🚨 Production Status](#-production-status-critical) | [🚨 Commit Policy](#-commit-policy-critical) | [iOS Safety](#ios-requirements-critical) | [Debugging](#debugging-guidelines-critical) | [Testing](#testing-requirements) | [Build Commands](#build-commands) | [Code Quality](#code-quality-standards)

---

## 🚨 Production Status [CRITICAL]

> **⚠️ VERSION 1.0 RELEASED - APPLICATION IN PRODUCTION**

WorldWideWaves **v1.0 is live in production** with real users on both iOS and Android. Any change can impact thousands of users.

### Mandatory Change Management Process

**BEFORE making ANY change** (code, tests, documentation), you MUST:

#### 1. Architecture Validation [USE AGENTS]

```bash
# Use Task tool with subagent_type=Plan
```

**Questions to answer**:

- Is this change architecturally sound?
- Does it fit existing patterns?
- Are there better alternatives?
- What are the long-term implications?

#### 2. Impact Analysis [USE AGENTS]

```bash
# Use Task tool with subagent_type=Explore (thoroughness: very thorough)
```

**Analyze**:

- **Android implications**: Build system, lifecycle, permissions, Play Core
- **iOS implications**: Threading, memory, UIKit integration, Kotlin/Native
- **Shared code impact**: Expect/actual implementations, KMM patterns
- **Side effects**: State management, observers, notification system
- **Breaking changes**: API contracts, data models, serialization

#### 3. Test Strategy Review [USE AGENTS]

**Before writing tests**, validate:

- Test coverage appropriate for production code
- Both platform-specific tests (androidUnitTest + iosTest)
- Edge cases for Android and iOS differences
- Integration test requirements
- Performance implications

#### 4. Documentation Impact [USE AGENTS]

**Identify documentation updates**:

- CLAUDE.md sections affected
- docs/ files requiring updates
- Code comments and KDoc
- README changes
- Architecture diagrams

### Production Change Rules

- ✅ **Use agents for ALL non-trivial changes** - architecture, impact analysis, test review
- ✅ **Double-check platform implications** - what works on Android may deadlock iOS
- ✅ **Validate with both platform builds** - Android AND iOS compilation required
- ✅ **Run full test suite** - all tests must pass (100%)
- ✅ **Update documentation immediately** - don't create doc debt
- ❌ **NO experimental changes** - production code requires proven patterns
- ❌ **NO "quick fixes"** - every change needs impact analysis
- ❌ **NO skipping tests** - test failures are production incidents

### Example: Correct Production Change Flow

**User Request**: "Add user profile caching"

**Step 1 - Architecture** (Task/Plan agent):

```
Analyze the architecture for adding user profile caching:
1. Where should cache live? (Repository? ViewModel? Separate service?)
2. What existing patterns can we follow? (EventCache? MapCache?)
3. Cache invalidation strategy?
4. Thread safety requirements?
```

**Step 2 - Impact Analysis** (Task/Explore agent, very thorough):

```
Analyze the impact of adding user profile caching:
1. Android: SharedPreferences? Room? DataStore? Memory implications?
2. iOS: UserDefaults? CoreData? FileManager? Memory pinning required?
3. Shared: Kotlinx.serialization changes? StateFlow observers?
4. Side effects: Does this affect FavoritesManager? NotificationScheduler?
```

**Step 3 - Test Review** (Task/Plan agent):

```
Review test strategy for user profile caching:
1. Unit tests: Cache hit/miss, invalidation, serialization
2. Android-specific: SharedPreferences mocking, lifecycle
3. iOS-specific: UserDefaults behavior, memory safety
4. Integration: End-to-end user profile load/save flow
```

**Step 4 - Documentation** (Task/Explore agent):

```
Identify documentation updates for user profile caching:
1. CLAUDE.md: Add to "Architecture Patterns" section
2. Architecture docs: Create or update relevant caching documentation
3. KDoc: CacheManager, UserRepository
4. README.md: Update features list if user-facing
```

**Then and only then**: Implement the change.

---

## Project Overview

WorldWideWaves is an **ephemeral mobile app** for orchestrating synchronized, location-based human wave events. Events exist briefly, creating shared moments without permanent data storage.

**Current Status**: ✅ v1.0 in production (iOS + Android)

### Technology Stack

- **Platform**: Kotlin Multiplatform Mobile (KMM) 1.9+
- **UI**: Compose Multiplatform (100% shared Android/iOS)
- **Maps**: MapLibre (open-source, self-hosted)
- **Backend**: Firebase (Firestore, Storage)
- **DI**: Koin
- **Testing**: Comprehensive unit test suite (Kotlin Test + Turbine)
- **CI/CD**: GitHub Actions (Android + iOS)

### Architecture

- **Pattern**: Clean Architecture + MVVM + Reactive Programming
- **State Management**: StateFlow + Compose
- **Position**: PositionManager as single source of truth (SIMULATION > GPS)
- **Navigation**: Custom deep linking

---

## iOS Requirements [CRITICAL]

> **Status**: ✅ STABLE | **Tests**: All passing | **Violations**: None (all fixed)

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

> **Status**: ✅ WCAG 2.1 Level AA Compliant | **Tests**: All passing

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

## Internationalization (i18n) Requirements [MANDATORY]

> **Status**: ✅ 32 Languages | **Coverage**: 100% | **Runtime Switching**: ✅ Enabled

### Language Support

WorldWideWaves supports **32 languages** with complete translation coverage:

- **Americas**: en, es, pt, fr (Canada)
- **Europe**: de, fr, it, nl, pl, ro, ru, tr, uk
- **Middle East**: ar, fa, he, ur
- **Africa**: am, ha, ig, sw, xh, yo, zu
- **Asia**: bn, hi, id, ja, ko, ms, pa, th, vi, zh, fil

### All Localized Content Must

- ✅ **Use MokoRes**: All strings via `stringResource(MokoRes.strings.key_name)`
- ✅ **No hardcoded strings**: All user-facing text must be in strings.xml
- ✅ **Parameter formatting**: Use `%1$s`, `%2$d` for string interpolation
- ✅ **Locale-aware formatting**: Dates and times respect device locale/preferences
- ✅ **RTL support**: Arabic, Hebrew, Farsi, Urdu properly handled

### Date/Time Formatting

**Android & iOS**: Both platforms now respect device locale and timezone

```kotlin
// Use platform-aware formatting
DateTimeFormats.dayMonth(instant, timeZone)  // "24 Dec" (en) → "24. Dez" (de)
DateTimeFormats.timeShort(instant, timeZone) // "2:30 PM" (12h) → "14:30" (24h)
```

### Runtime Language Switching

Users can change language **without app restart**:

**Android**: Settings → System → Languages → Add language
**iOS**: Settings → General → Language & Region → [App] → Language

**Implementation**:

- LocalizationManager observes system locale changes
- Emits via StateFlow to trigger Compose recomposition
- UI updates automatically with new localized strings

### Translation Validation

**Before every commit with new strings**:

```bash
./gradlew :shared:lintDebug  # Validates all 32 languages have all strings
```

**Add new strings**:

1. Add to `shared/src/commonMain/moko-resources/base/strings.xml`
2. Run `python3 scripts/translate/update_translations.py`
3. Verify lint passes
4. Commit base + all translated files

### Testing Requirements

**i18n tests must cover**:

- String resource accessibility (LocalizationTest.kt)
- Date/time formatting (DateTimeFormatsTest.kt)
- Platform-specific locale behavior (platform-specific tests)
- Runtime locale change handling (LocalizationManagerTest.kt)

### Common Issues

| Issue | Cause | Solution |
|-------|-------|----------|
| iOS dates always in English | Hardcoded format | Fixed in DateTimeFormats.ios.kt (Oct 2025) |
| Missing translation warnings | Lint disabled | Re-enabled Oct 2025 |
| Language change requires restart | No detection | LocalizationManager added Oct 2025 |
| RTL layout issues | Missing semantics | Add `layoutDirection` to Compose |

**See**: `shared/src/commonMain/moko-resources/` for all translations
**See**: `shared/src/*/localization/` for runtime locale handling

---

## Mandatory Development Requirements

### 🚨 Commit Policy [CRITICAL]

**ALWAYS commit immediately after completing and testing each feature/fix.**

- ✅ **Commit automatically** after tests pass - don't wait to be asked
- ✅ **Commit after each logical unit of work** (feature, fix, refactor)
- ✅ **Run all tests before committing** - ensure nothing is broken
- ❌ **NEVER leave uncommitted work** - commit frequently
- ❌ **DO NOT push to origin** unless explicitly requested (GitHub Actions costs)

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

### Map Download System

**Critical Understanding**: Android Play Core uses **deferred uninstall** - map files remain after uninstall until app update.

**forcedUnavailable Flag** (Android only):

- ✅ Set on uninstall to respect user intent despite file persistence
- ✅ Persisted to SharedPreferences to survive app restarts
- ✅ **MUST be cleared BEFORE availability checks in download flow**
- ✅ Cleared in `MapDownloadCoordinator.downloadMap()` before `isMapInstalled()` check
- ✅ **MUST be checked before UI operations** (defense-in-depth against stale ViewModel state)
- ❌ iOS doesn't need this - ODR deletes files immediately

**Critical Rules**:

1. **Download flow**: Call `clearForcedUnavailableIfNeeded()` BEFORE any availability checks
2. **UI operations**: Check `isForcedUnavailable()` before setting map available or loading map
3. **ViewModel state**: Don't trust ViewModel.featureState alone - it may be stale after uninstall

**Defense-in-Depth**:

AndroidEventMap checks `isForcedUnavailable()` at two points:

- Before setting `isMapAvailable = true` (prevents incorrect UI state)
- Before loading map (prevents uninstalled maps from rendering)

This handles AndroidMapViewModel state persistence across navigation.

**See**:

- [Map Download System Architecture](docs/architecture/map-download-system.md) - Complete system documentation
- [Map Cache Management](docs/architecture/map-cache-management.md) - Cache lifecycle and invalidation

---

## Testing Requirements

### ⚠️ CRITICAL: Cross-Platform Compilation Check [MANDATORY]

**BEFORE EVERY COMMIT touching shared/ code**, verify compilation on **BOTH platforms**:

```bash
# MINIMUM required check before commit (Kotlin only):
./gradlew clean :shared:testDebugUnitTest \
  :shared:compileDebugKotlinAndroid \
  :shared:compileKotlinIosSimulatorArm64

# Expected: ALL tasks successful, 100% test pass rate
```

**BEFORE EVERY COMMIT touching iosApp/ Swift code**, also verify Swift compilation:

```bash
# iOS Swift compilation check:
xcodebuild -project iosApp/worldwidewaves.xcodeproj \
  -scheme worldwidewaves \
  -sdk iphonesimulator \
  build

# Expected: ** BUILD SUCCEEDED **
```

**Why**: Android unit tests (`:shared:testDebugUnitTest`) only compile Android code and miss iOS-specific compilation errors like:

- Missing imports in iOS source sets
- JVM-only APIs used in commonMain (String.format, etc.)
- Platform-specific type mismatches

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
# Expected: All tests passing with 100% pass rate
```

### Requirements

- **All changes must pass existing test suite** (comprehensive suite)
- **New functionality requires tests** - no test debt
- **Run ALL tests**, not just relevant ones
- **Performance**: Monitor test execution time
- **iOS safety**: `./scripts/dev/verification/verify-ios-safety.sh` for shared code changes
- ✅ **COMMIT IMMEDIATELY after tests pass** - don't wait to be asked

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

### Import Management & Qualified Names [MANDATORY]

**Rule**: NEVER use fully qualified class names in code. ALWAYS use imports.

**Why**: Qualified names create visual clutter, reduce readability, and make code harder to maintain.

```kotlin
// ❌ NEVER - Qualified names in code
fun initialize() {
    val platform = com.worldwidewaves.shared.WWWPlatform.instance
    val logger = com.worldwidewaves.shared.utils.Log
    return com.worldwidewaves.shared.domain.Position(0.0, 0.0)
}

// ✅ ALWAYS - Clean imports
import com.worldwidewaves.shared.WWWPlatform
import com.worldwidewaves.shared.utils.Log
import com.worldwidewaves.shared.domain.Position

fun initialize() {
    val platform = WWWPlatform.instance
    val logger = Log
    return Position(0.0, 0.0)
}
```

**Disambiguation with Type Aliases**:

When name conflicts occur, use type aliases instead of qualified names:

```kotlin
// ❌ NEVER - Mixing qualified and unqualified names
import com.worldwidewaves.shared.domain.Position

fun convert(location: android.location.Location) {
    return Position(
        location.latitude,
        location.longitude
    )
}

// ✅ ALWAYS - Type aliases for clarity
import com.worldwidewaves.shared.domain.Position
import android.location.Location as AndroidLocation

fun convert(location: AndroidLocation) {
    return Position(
        location.latitude,
        location.longitude
    )
}
```

**Common Aliases**:

- `import android.location.Location as AndroidLocation`
- `import platform.CoreLocation.CLLocation as IOSLocation`
- `import kotlinx.datetime.Instant as KotlinInstant`
- `import java.time.Instant as JavaInstant`

**Enforcement**:

- Use `./gradlew detekt` to catch qualified names
- Code reviews must reject qualified names
- No exceptions - refactor if needed

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

### Markdown Documentation Standards [MANDATORY]

**Rule**: All markdown files MUST pass markdownlint-cli2 validation before commit.

**Why**: Consistent documentation formatting ensures readability, prevents CI failures, and maintains professional quality across all project documentation.

**Configuration**: `.markdownlint-cli2.jsonc` (project root)

**Key Requirements**:

- Headings surrounded by blank lines (MD022)
- Lists surrounded by blank lines (MD032)
- Code blocks surrounded by blank lines (MD031)
- Files end with single newline (MD047)
- Space after `#` in headings (MD018)

**Pre-Commit Validation**:

```bash
# Check markdown formatting
npx markdownlint-cli2 "**/*.md" "!node_modules/**" "!build/**" "!SourcePackages/**" "!.gradle/**" "!iosApp/build/**" "!shared/build/**" "!composeApp/build/**" "!maps/**/node_modules/**"

# Auto-fix formatting issues
npx markdownlint-cli2 --fix "**/*.md" [same exclusions as above]
```

**Pre-Push Enforcement**:

The pre-push git hook automatically runs markdown linting. Push will be blocked if errors are detected.

**Common Fixes**:

```markdown
<!-- ❌ WRONG - No blank lines around heading -->
Some text here.
## Heading
More text.

<!-- ✅ CORRECT - Blank lines around heading -->
Some text here.

## Heading

More text.
```

```markdown
<!-- ❌ WRONG - No blank lines around list -->
Text before list.
- Item 1
- Item 2
Text after list.

<!-- ✅ CORRECT - Blank lines around list -->
Text before list.

- Item 1
- Item 2

Text after list.
```

**Enforcement**:

- Pre-push hook blocks commits with markdown errors
- GitHub Actions workflow fails on markdown violations
- Use auto-fix for most issues: `npx markdownlint-cli2 --fix "**/*.md" ...`

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

### Notifications System

**Status**: ✅ Production-Ready | **Tests**: All passing | **Phase**: 7 Complete

The notification system delivers time-based and immediate alerts for wave events to favorited events only.

**Key Files**:

- **Shared Core** (expect/actual pattern):
  - `shared/src/commonMain/kotlin/com/worldwidewaves/shared/notifications/NotificationTrigger.kt` - 3 trigger types (EventStarting, EventFinished, WaveHit)
  - `shared/src/commonMain/kotlin/com/worldwidewaves/shared/notifications/NotificationManager.kt` - Interface for scheduling/delivery
  - `shared/src/commonMain/kotlin/com/worldwidewaves/shared/notifications/NotificationScheduler.kt` - Eligibility logic (favorited + simulation mode compatible)
  - `shared/src/commonMain/kotlin/com/worldwidewaves/shared/notifications/NotificationContent.kt` - Localization keys + deep links
  - `shared/src/commonMain/kotlin/com/worldwidewaves/shared/notifications/NotificationContentProvider.kt` - Content generation

- **Android**:
  - `shared/src/androidMain/kotlin/com/worldwidewaves/shared/notifications/AndroidNotificationManager.kt` - WorkManager for scheduled, NotificationCompat for immediate (lines 38-150)
  - `shared/src/androidMain/kotlin/com/worldwidewaves/shared/notifications/NotificationWorker.kt` - CoroutineWorker for delivery
  - `shared/src/androidMain/kotlin/com/worldwidewaves/shared/notifications/NotificationChannelManager.kt` - Channel setup (HIGH importance)

- **iOS**:
  - `shared/src/iosMain/kotlin/com/worldwidewaves/shared/notifications/IOSNotificationManager.kt` - UNUserNotificationCenter (class-based, lazy init for iOS safety)
  - `iosApp/worldwidewaves/NotificationPermissionBridge.swift` - Permission request bridge

**Notification Types**: 6 scheduled (1h, 30m, 10m, 5m, 1m before) + 1 immediate (wave hit) = 7 total per favorited event

**Eligibility**: Event is favorited AND (no simulation OR speed == 1) AND event hasn't started

**Limits**: iOS 64 pending max (typical <60 with favorites-only), Android ~500 (typical <60)

**Development**:

1. When event favorited: Call `notificationScheduler.scheduleAllNotifications(event)`
2. When event unfavorited: Call `notificationScheduler.cancelAllNotifications(eventId)`
3. On app launch: Call `notificationScheduler.syncNotifications(favorites, events)`
4. Wave hit detection: Call `notificationManager.deliverNow(eventId, WaveHit, content)`

**Testing**: `./gradlew :shared:testDebugUnitTest` includes comprehensive notification test coverage (commonTest + androidUnitTest + iosTest)

**See**: [docs/features/notification-system.md](docs/features/notification-system.md) for comprehensive system documentation

### Crashlytics Integration

**Status**: Hybrid Strategy | Android: ✅ Full | iOS: ⏭️ Native Only

Firebase Crashlytics uses a **hybrid strategy** due to Swift/Kotlin/Native interop limitations.

**iOS Implementation**: Disabled for Kotlin shared code

- Firebase iOS SDK contains Swift dependencies that conflict with Kotlin/Native linker
- iOS app uses Firebase Crashlytics **directly** for native crashes (Swift/ObjC) ✅
- Kotlin exceptions on iOS are **logged locally only** (not sent to Firebase) ⏭️
- Rare occurrence - most app logic is platform-specific

**Android Implementation**: Fully functional ✅

- CrashlyticsLogger.android.kt reports all Kotlin exceptions to Firebase
- Complete crash reporting for shared code

**Architecture**:

```
iOS App (Swift) → Firebase Crashlytics SDK (native crashes) ✅
Kotlin Android → CrashlyticsLogger.android.kt → Firebase ✅
Kotlin iOS → CrashlyticsLogger.ios.kt (no-op, logs only) ⏭️
```

**Why iOS Bridge Disabled**:

Firebase iOS SDK requires Swift compatibility libraries that Kotlin/Native cannot provide during framework linking. Attempted solutions (static library, weak linking) fail with: `ld: library 'swiftCompatibility50' not found`.

**What Gets Reported**:

- ✅ **iOS native crashes** (Swift/ObjC) → Firebase Crashlytics
- ✅ **Android Kotlin crashes** → Firebase Crashlytics
- ⏭️ **iOS Kotlin crashes** → Local logs only (debuggable via Xcode console)

**Key Files**:

- `shared/src/iosMain/kotlin/com/worldwidewaves/shared/utils/CrashlyticsLogger.ios.kt` - No-op implementation
- `shared/src/androidMain/kotlin/com/worldwidewaves/shared/utils/CrashlyticsLogger.android.kt` - Full integration

**Future Improvement Options**:

- Swift package exposing C API (no Swift in headers)
- XCFramework approach
- Wait for Kotlin/Native Swift interop improvements

**See**: `CrashlyticsLogger.ios.kt` file header for technical details

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

### Automatic Temp File Cleanup

iOS builds automatically clean up temporary files older than 2 days from `/var/folders/` to prevent disk space accumulation.

**What gets cleaned:**

- Kotlin/Native compiler artifacts (`kotlin-daemon.*.log`)
- Kotlin compiler temp directories (`org.jetbrains.kotlin/*`)

**Cleanup behavior:**

- Runs automatically after `embedAndSignAppleFrameworkForXcode`
- Skips files newer than 2 days (safety threshold)
- Skips in CI environments (GitHub Actions)
- Logs cleanup summary (files deleted, space freed)

**Opt-out:**

```bash
./gradlew :shared:embedAndSignAppleFrameworkForXcode -PskipTempCleanup=true
```

**Manual cleanup:**

```bash
./gradlew cleanupIOSTempFiles
```

---

## Production-Ready Definition

A file/module is production-ready when:

- ✅ All unit tests passing (100%)
- ✅ Zero compilation warnings (iOS + Android)
- ✅ Zero detekt/SwiftLint warnings (or justified suppressions)
- ✅ File header present and correct
- ✅ No unsafe `!!` operators (Kotlin) or force unwraps (Swift)
- ✅ No qualified class names in code (use imports + aliases)
- ✅ Thread safety explicit (Mutex, synchronized)
- ✅ Accessibility semantics on interactive elements
- ✅ No hardcoded user-facing strings
- ✅ Proper error handling (specific exceptions)
- ✅ Memory leak prevention (cleanup methods)
- ✅ iOS safety compliance (no deadlock patterns)

---

## Debugging Guidelines [CRITICAL]

### Debugging State Synchronization Issues

When encountering state synchronization bugs (e.g., "UI shows X but validation fails"):

#### Step 1: Map Data Flow FIRST (5 minutes)

**BEFORE adding any logging or code:**

1. **Identify the observer**: What component is checking the state?
   - Use Grep to find where the validation/check happens
   - Example: `SimulationButton` checks `mapFeatureState`

2. **Trace state source**: Where does that state come from?
   - Example: `mapFeatureState` comes from `MapViewModel.featureState`

3. **Identify the actor**: What triggers the state change?
   - Example: Download button calls `EventMapDownloadManager.downloadMap()`

4. **Check the connection**: Does the actor update the observer's state source?
   - Example: Does `EventMapDownloadManager` notify `MapViewModel`? **NO!**
   - **Issue found in 5 minutes.**

#### Step 2: Use Explore Agent for Complex Flows

For multi-component issues, use Task tool with Explore agent:

```
Find all code paths related to [FEATURE]:
1. Where does [COMPONENT A] get its state?
2. Where does [COMPONENT B] trigger state changes?
3. How do these two communicate?
Thoroughness: very thorough
```

#### Step 3: Check for Dual Implementations

**Common pattern in this codebase:**

- `EventMapDownloadManager` (UI-focused, per-map state)
- `MapViewModel/MapDownloadCoordinator` (business logic, global state)

When debugging, check if multiple systems exist:

```bash
grep -r "class.*Manager\|interface.*Manager" shared/src/ | grep -i "download\|state"
```

#### What to Do

✅ **DO**:

- Spend 5-10 minutes mapping data flow FIRST
- Use Explore agent for complex multi-component issues
- Look for architectural issues (dual systems, missing connections)
- Verify hypothesis with logs AFTER understanding flow

❌ **DON'T**:

- Add logging before understanding architecture
- Assume race conditions without evidence
- Fix symptoms without understanding root cause
- Make multiple attempts without changing approach

### Common Debugging Anti-Patterns

#### 1. Debugging in the Dark

❌ **Bad**: Add logging → test → add more logging → test → eventually stumble on answer

✅ **Good**: Understand architecture → form hypothesis → add targeted verification → fix

#### 2. Trusting First Hypothesis

❌ **Bad**: "It's probably a race condition" → spend hours on that assumption

✅ **Good**: "Could be race condition OR dual systems OR missing callback" → verify systematically

#### 3. Manual Exploration for Complex Issues

❌ **Bad**: Use grep → read files → grep more → read more files → get lost

✅ **Good**: Use Explore agent with clear question: "How does X connect to Y?"

#### 4. Ignoring Log Evidence

❌ **Bad**: See `SystemA: success` but not `SystemB: updated` → keep assuming they're connected

✅ **Good**: Notice missing logs → immediately question if systems are connected

### Example: Correct Debugging Flow

**Issue**: "Download works, simulation fails with 'map required' dialog"

**Correct approach (5 minutes)**:

1. Grep: `SimulationButton.*mapFeatureState` → sees it reads `MapViewModel`
2. Grep: `downloadMap.*EventMapDownloadManager` → download button uses `EventMapDownloadManager`
3. Question: "Does EventMapDownloadManager update MapViewModel?"
4. Grep: `EventMapDownloadManager` → no calls to `MapViewModel`
5. **Root cause found**: Two separate systems, no connection
6. Fix: Add callback to connect them

---

## Development Workflow

### Critical Rules

- ✅ **COMMIT IMMEDIATELY** after completing and testing each feature/fix - always, automatically
- **Use agents** for complex multi-step tasks
- **Check all implications** when changing code (callers, documentation, signatures)
- **Clean temporary files** after development
- ❌ **Git push has costs** - GitHub Actions not free, only push when explicitly requested
- **Search for similar patterns** when fixing bugs (find other instances)
- **Never bypass git hooks** (pre-commit, pre-push)
- ❌ **NEVER use qualified class names** in code (e.g., `com.foo.Bar`) - ALWAYS use imports + aliases for disambiguation
- **Fix warnings immediately** - prevent accumulation

### Import Management [CRITICAL]

**ALWAYS check existing imports BEFORE modifying code.**

**NEVER use qualified class names in code - ALWAYS add proper imports.**

When adding function calls/classes:

1. Check if required import exists
2. Add missing imports in same change (NEVER use `com.foo.Bar` directly in code)
3. Use type aliases (`as`) if name conflicts occur
4. Verify compilation before commit

**Common imports**:

- Coroutines: `kotlinx.coroutines.runBlocking`, `withContext`, etc.
- Compose: `androidx.compose.runtime.key`, `LaunchedEffect`, etc.
- Platform: `platform.UIKit.*`, `platform.Foundation.*` (iOS)
- Logging: `com.worldwidewaves.shared.utils.Log`

**See**: [Code Quality Standards → Import Management](#import-management--qualified-names-mandatory) for detailed examples and type alias patterns

### Git Workflow

#### Committing Changes

**COMMIT AUTOMATICALLY after completing and testing each feature/fix.**

Workflow:

1. Complete the feature/fix
2. Run all tests and ensure they pass
3. Stage and commit the changes
4. Continue with next task

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

**DO NOT push to origin** unless explicitly requested (GitHub Actions costs).

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
| Camera animation stops mid-flight (Android) | Native MapLibre constraint bounds | Fixed: Constraints temporarily removed during programmatic animations |

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
- **[Cinterop Memory Safety Patterns](docs/ios/cinterop-memory-safety-patterns.md)** - Memory pinning & struct access
- **[Swift-Kotlin Bridging Guide](docs/ios/swift-kotlin-bridging-guide.md)** - Type conversions & protocols
- **[Platform API Usage Guide](docs/ios/platform-api-usage-guide.md)** - UIKit/Foundation/CoreLocation

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

**Version**: 4.0 (Production release v1.0 - Added mandatory change management process with agent requirements)
**App Version**: 1.0 (In Production)
**Maintainer**: WorldWideWaves Development Team
