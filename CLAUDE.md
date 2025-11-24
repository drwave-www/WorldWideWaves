# WorldWideWaves - Claude Code Instructions

> **Quick Links**: [🚨 Production Status](#-production-status-critical) | [🚨 Commit Policy](#-commit-policy-critical) | [iOS Safety](#ios-requirements-critical) | [Debugging](#debugging-guidelines-critical) | [Testing](#testing-requirements) | [Build Commands](#build-commands) | [Code Quality](#code-quality-standards)

---

## 🚨 Production Status [CRITICAL]

> **⚠️ VERSION 1.0 RELEASED - APPLICATION IN PRODUCTION**

WorldWideWaves **v1.0 is live in production** with real users on both iOS and Android. Any change can impact thousands of users.

### Mandatory Change Management Process

**BEFORE making ANY change** (code, tests, documentation), complete these 4 steps using agents:

#### 1. Architecture Validation [Task/Plan]

Validate: Architecture soundness, pattern fit, alternatives, long-term implications

```
Analyze architecture for [CHANGE]:
- Does it fit existing patterns? (EventCache, MapCache, etc.)
- Better alternatives?
- Thread safety requirements?
```

#### 2. Impact Analysis [Task/Explore - very thorough]

Analyze: Android (lifecycle, Play Core), iOS (threading, memory, Kotlin/Native), shared code (expect/actual), side effects (state, observers), breaking changes

```
Analyze impact of [CHANGE]:
- Android: Build system, lifecycle, memory
- iOS: Threading, UIKit integration, memory pinning
- Shared: StateFlow observers, serialization
- Side effects: Which systems affected?
```

#### 3. Test Strategy Review [Task/Plan]

Validate: Coverage appropriate for production, platform-specific tests (androidUnitTest + iosTest), edge cases, performance

```
Review test strategy for [CHANGE]:
- Unit tests needed?
- Android/iOS-specific test requirements?
- Integration tests?
```

#### 4. Documentation Impact [Task/Explore]

Identify: CLAUDE.md sections, docs/ files, KDoc, README, architecture diagrams

```
Identify docs to update for [CHANGE]:
- CLAUDE.md sections affected?
- Architecture docs to create/update?
- Code comments and KDoc needed?
```

### Production Change Rules

| ✅ ALWAYS | ❌ NEVER |
| ----------- | ---------- |

| Use agents for non-trivial changes | Make experimental changes |
| Validate both platform builds (iOS + Android) | Skip impact analysis ("quick fixes") |
| Run full test suite (100% pass) | Skip tests (failures = production incidents) |
| Update docs immediately | Create documentation debt |

### Example: Production Change Flow

**Request**: "Add user profile caching"

1. **Architecture** (Plan): Where should cache live? Follow EventCache pattern? Thread safety?
2. **Impact** (Explore): Android SharedPreferences vs iOS UserDefaults? Affects FavoritesManager?
3. **Tests** (Plan): Cache hit/miss tests, platform-specific persistence tests, integration tests
4. **Docs** (Explore): Update Architecture Patterns section, add caching-strategy.md, KDoc

**Then implement.**

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

## iOS Safety Quick Reference [CRITICAL]

> **iOS threading violations cause immediate deadlocks**. Always consult [CLAUDE_iOS.md](CLAUDE_iOS.md) for complete details.

| ❌ NEVER | ✅ ALWAYS | Why |
| ---------- | ---------- | ----- |

| `object : KoinComponent` in @Composable | Use `IOSSafeDI.get<T>()` | Composables trigger iOS main thread deadlock |
| `by inject()` during composition | Use constructor injection or `LocalKoin.current.get<T>()` | Property delegation freezes iOS |
| `runBlocking` before ComposeUIViewController | Use suspend functions with `LaunchedEffect` | Blocks iOS main thread |
| `init{}` with DI access | Use constructor parameters or `lazy { }` | Init blocks freeze on iOS |
| `Dispatchers.Main` in properties | Use `lazy { CoroutineScope(Dispatchers.Main) }` | Property initialization crashes iOS |

**Verification**: Run `./scripts/dev/verification/verify-ios-safety.sh` before every commit touching shared code.

**See**: [CLAUDE_iOS.md](CLAUDE_iOS.md) for comprehensive iOS safety guide with all 11 violation fixes.

**Example**:

```kotlin
// ❌ DEADLOCKS iOS!
@Composable fun Screen() {
    val deps = object : KoinComponent { val clock by inject() }
}

// ✅ SAFE
@Composable fun Screen() {
    val clock = getIOSSafeClock()  // IOSSafeDI wrapper
}
```

---

## iOS Requirements [CRITICAL]

> **Status**: ✅ STABLE | **Tests**: All passing | **Violations**: None (all fixed)

For detailed iOS safety patterns, threading rules, and platform-specific guidance, see [CLAUDE_iOS.md](CLAUDE_iOS.md) and [docs/patterns/ios-safety-patterns.md](docs/patterns/ios-safety-patterns.md)

---

## Accessibility Requirements [MANDATORY]

> **Status**: ✅ WCAG 2.1 Level AA Compliant | **Tests**: All passing

**All UI components must have**:

- contentDescription (localized via MokoRes.strings)
- semantics (`role`, `contentDescription`, `stateDescription`)
- 48dp/44pt minimum touch targets
- `.sp` units for text (system font scaling)
- 4.5:1 color contrast ratio
- `semantics { heading = true }` for headings

**Example**:

```kotlin
Button(onClick = { action() },
    modifier = Modifier.size(48.dp).semantics {
        role = Role.Button
        contentDescription = "Join wave event"
    })
```

**Verify**: `./scripts/dev/verification/test_accessibility.sh` before each PR

**See**: [docs/accessibility-guide.md](docs/accessibility-guide.md)

---

## Internationalization (i18n) Requirements [MANDATORY]

> **Status**: ✅ 32 Languages | **Coverage**: 100% | **Runtime Switching**: ✅ Enabled

### Language Support (32 languages)

**Americas**: en, es, pt, fr | **Europe**: de, fr, it, nl, pl, ro, ru, tr, uk | **Middle East**: ar, fa, he, ur | **Africa**: am, ha, ig, sw, xh, yo, zu | **Asia**: bn, hi, id, ja, ko, ms, pa, th, vi, zh, fil

### Critical Rules

- ✅ **Use MokoRes**: `stringResource(MokoRes.strings.key_name)`
- ✅ **No hardcoded strings**: All user-facing text in strings.xml
- ✅ **Parameter formatting**: Use `%1$s`, `%2$d`
- ✅ **Locale-aware dates/times**: `DateTimeFormats.dayMonth()`, `DateTimeFormats.timeShort()`
- ✅ **RTL support**: Arabic, Hebrew, Farsi, Urdu handled automatically

### Runtime Language Switching

Users change language **without restart** via system settings. LocalizationManager observes locale changes → StateFlow → Compose recomposition.

### Translation Workflow

**Before commit with new strings**:

```bash
./gradlew :shared:lintDebug  # Validates all 32 languages
```

**Add strings**: (1) Edit `base/strings.xml` → (2) Run `python3 scripts/translate/update_translations.py` → (3) Verify lint → (4) Commit

**See**: `shared/src/commonMain/moko-resources/` for translations | `shared/src/*/localization/` for runtime handling

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

**Critical**: Android Play Core uses **deferred uninstall** - files persist until app update. Use `forcedUnavailable` flag (Android only) to respect user intent.

**Rules**:

1. **Download**: Call `clearForcedUnavailableIfNeeded()` BEFORE availability checks
2. **UI**: Check `isForcedUnavailable()` before setting available or loading map
3. **State**: Don't trust ViewModel alone - may be stale after uninstall

**iOS**: ODR deletes files immediately, no flag needed.

**See**: [Map Download System](docs/architecture/map-download-system.md) | [Map Cache Management](docs/architecture/map-cache-management.md)

---

## Testing Requirements

### ⚠️ CRITICAL: Cross-Platform Compilation Check [MANDATORY]

**BEFORE EVERY COMMIT**, verify both platforms:

```bash
# Kotlin (shared/ changes):
./gradlew clean :shared:testDebugUnitTest \
  :shared:compileDebugKotlinAndroid \
  :shared:compileKotlinIosSimulatorArm64

# Swift (iosApp/ changes):
xcodebuild -project iosApp/worldwidewaves.xcodeproj \
  -scheme worldwidewaves -sdk iphonesimulator build

# Expected: ALL tasks successful, 100% pass rate
```

**Why**: Android tests only compile Android code, missing iOS errors (imports, JVM-only APIs, type mismatches).

### Test Organization & Requirements

```
shared/src/
├── commonTest/       # Platform-independent (NO MockK, NO JVM APIs)
├── androidUnitTest/  # Android-specific (CAN use MockK)
└── iosTest/          # iOS-specific (NO MockK, Kotlin/Native only)
```

**Critical Rules**:

- All changes must pass existing suite (100%)
- New functionality requires tests (no test debt)
- Run ALL tests, not just relevant ones
- `./scripts/dev/verification/verify-ios-safety.sh` for shared code changes
- ✅ **COMMIT IMMEDIATELY after tests pass**

**See**: [docs/testing/test-patterns.md](docs/testing/test-patterns.md) for comprehensive patterns (infinite flows, test isolation, etc.)

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

**Key Requirements**: Blank lines around headings/lists/code blocks, single newline at EOF, space after `#`

**Validation**:

```bash
npx markdownlint-cli2 "**/*.md" "!node_modules/**" "!build/**" "!.gradle/**"
npx markdownlint-cli2 --fix "**/*.md" ...  # Auto-fix
```

**Enforcement**: Pre-push hook blocks commits with markdown errors. Use `--fix` for most issues.

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

**See**: [Simulation Mode Guide](docs/features/simulation-mode.md) | [Event Observation System](docs/architecture/event-observation-system.md) | [Wave Hit Detection System](docs/architecture/wave-hit-detection-system.md)

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

**Status**: ✅ Production-Ready | **Tests**: All passing

Time-based and immediate alerts for **favorited events only**.

**Architecture**: Expect/actual pattern with platform-specific implementations (WorkManager on Android, UNUserNotificationCenter on iOS)

**Notification Types**: 6 scheduled (1h, 30m, 10m, 5m, 1m before) + 1 immediate (wave hit) = 7 per favorited event

**Eligibility**: Favorited AND (no simulation OR speed == 1) AND not started yet

**Development**:

- Favorited: `notificationScheduler.scheduleAllNotifications(event)`
- Unfavorited: `notificationScheduler.cancelAllNotifications(eventId)`
- App launch: `notificationScheduler.syncNotifications(favorites, events)`
- Wave hit: `notificationManager.deliverNow(eventId, WaveHit, content)`

**See**: [docs/features/notification-system.md](docs/features/notification-system.md)

### Crashlytics Integration

**Status**: Hybrid Strategy | Android: ✅ Full | iOS: ⏭️ Native Only

**Why Hybrid**: Swift/Kotlin/Native interop limitations prevent iOS shared code crash reporting.

**What Gets Reported**:

- ✅ iOS native crashes (Swift/ObjC) → Firebase
- ✅ Android Kotlin crashes → Firebase
- ⏭️ iOS Kotlin crashes → Local logs only (rare - most logic is platform-specific)

**Architecture**: iOS uses Firebase SDK directly for native crashes. Android uses `CrashlyticsLogger.android.kt` for shared code. iOS shared code logs locally via `CrashlyticsLogger.ios.kt` (no-op).

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

iOS builds auto-clean Kotlin/Native temp files (>2 days old) from `/var/folders/` after `embedAndSignAppleFrameworkForXcode`.

**Opt-out**: `-PskipTempCleanup=true` | **Manual**: `./gradlew cleanupIOSTempFiles`

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
| --------- | ------- | ---------- |

| iOS app freezes on launch | DI violation | Run `./scripts/dev/verification/verify-ios-safety.sh` |
| Tests failing | Business logic issue or requirements changed | Analyze failure, ask before changing tests |
| Compilation warnings | Code quality issue | Fix ALL warnings, not just in modified files |
| Map gestures not working (iOS) | Wrong property names | Use `isZoomEnabled/isScrollEnabled` |
| Null pointer crash | Force unwrap `!!` | Use `?.` with `?:` or `requireNotNull()` |
| Camera animation stops mid-flight (Android) | Native MapLibre constraint bounds | Fixed: Constraints temporarily removed during programmatic animations |
| Map crash: "file is not a database" | Git LFS pointer file | Run `git lfs checkout` or `./scripts/dev/verification/verify-lfs-files.sh` |

### Git LFS Issues

**Symptom**: MapLibre crashes with "file is not a database" or map files are suspiciously small (< 1KB).

**Cause**: Map `.mbtiles` files are Git LFS pointers instead of actual SQLite databases. This happens when:

- Git LFS is not properly installed (`git lfs install`)
- Files were cloned before LFS setup
- LFS files were not checked out after clone

**Solution**:

```bash
# Check LFS status
git lfs status

# Files showing "-" (dash) are not downloaded
# Files showing "*" (asterisk) are properly downloaded

# Download all LFS files
git lfs checkout

# Or download specific file
git lfs checkout "maps/paris_france/src/main/assets/paris_france.mbtiles"

# Verify fix
./scripts/dev/verification/verify-lfs-files.sh
```

**Prevention**: Pre-commit hook automatically checks for LFS pointer files before commits.

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
