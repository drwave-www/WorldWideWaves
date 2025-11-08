# Scripts

Development automation and tooling for WorldWideWaves.

## Quick Start

### Verify Development Environment

Before using any scripts, verify your development environment:

```bash
./scripts/verify-setup.sh
```

This checks:

- Platform detection (macOS, Linux, Windows)
- Required tools (Java 17+, Node.js 16+, Git, ripgrep)
- Platform-specific tools (Xcode on macOS, KVM on Linux)
- Android SDK configuration
- Project configuration files
- Build system functionality

## Directory Structure

```
scripts/
├── verify-setup.sh              # Environment verification (REQUIRED)
├── verify-ios-safety.sh         # iOS thread safety validation (CRITICAL)
├── test_accessibility.sh        # WCAG 2.1 compliance testing (REQUIRED)
├── pre-push-verify.sh          # Pre-push validation (matches CI/CD)
├── detect-test-antipatterns.sh  # Test quality validation
├── run-test-quality-validation.sh # Comprehensive test validation
├── validate_test_setup.sh       # E2E test setup verification
├── test-performance-monitor.sh  # Test performance analysis
├── test-odr-bundle-generation.sh # iOS ODR validation
├── generate_test_report.py      # Test report generation
├── add-headers.sh               # Copyright header management
├── clean_xcode.sh              # Xcode cache cleanup
├── generate_firebase_config.sh  # Firebase config generation
├── generate_ios_firebase_config.sh # iOS Firebase config
│
├── dashboards/                  # Testing dashboards & monitoring
│   ├── test-analytics-reporter.sh    # Test analytics & trending
│   ├── test-execution-dashboard.sh   # Real-time test monitoring
│   ├── test-stability-tracker.sh     # Flakiness detection
│   └── test-suite-health-check.sh    # Test suite health metrics
│
├── testing/                     # Specialized test utilities
│   └── run_sound_choreography.sh     # Audio testing utility
│
├── firebase/                    # Firebase Test Lab integration
│   ├── run_all_firebase_tests.sh         # All-platform E2E test orchestrator
│   ├── run_android_firebase_tests.sh     # Android E2E tests
│   ├── run_ios_firebase_tests.sh         # iOS E2E tests
│   ├── collect_firebase_screenshots.sh   # Screenshot collection
│   └── download_firebase_logs.sh         # Log collection
│
├── maps/                        # Map generation pipeline
├── images/                      # Image processing & icon generation
│   ├── enhance_ios_icon.py              # iOS icon enhancement
│   └── create_android_icon.py           # Android icon generation
├── licenses/                    # License compliance
├── polygons/                    # Geographic boundary processing
├── style/                       # Map style generation
└── translate/                   # Localization tools
```

## Core Production Scripts

### 🔍 verify-setup.sh - **Environment Verification**

Comprehensive development environment verification script.

**Purpose:**

- Verify all required development tools
- Check platform-specific requirements
- Validate Android SDK configuration
- Test Gradle build system

**Usage:**

```bash
./scripts/verify-setup.sh
```

**Checks Performed:**

- ✅ Platform detection (macOS, Linux, Windows Git Bash)
- ✅ Java JDK 17+, Node.js 16+, Git, ripgrep
- ✅ Android SDK (ANDROID_HOME/ANDROID_SDK_ROOT)
- ✅ Platform tools (Xcode on macOS, KVM on Linux)
- ✅ Project configuration files
- ✅ Gradle build system
- ⚠️  Optional tools (Docker, gcloud, CocoaPods, SwiftLint)

**Exit Codes:**

- `0` - All required checks passed
- `1` - One or more critical checks failed

**Platform Compatibility:**

- macOS (tested on macOS 15.6+)
- Linux (Ubuntu, Debian, Fedora)
- Windows (Git Bash/MSYS2/Cygwin)

---

### 🛡️ verify-ios-safety.sh - **iOS Thread Safety Validation**

Validates iOS Kotlin/Native thread safety to prevent deadlocks.

**Purpose:**

- Detect iOS-specific threading violations
- Prevent app deadlocks on launch
- Enforce iOS safety patterns

**Usage:**

```bash
./scripts/verify-ios-safety.sh
```

**Checks:**

- ❌ No `object : KoinComponent` in @Composable scopes
- ❌ No `by inject()` during Compose composition
- ❌ No `runBlocking` before ComposeUIViewController
- ❌ No coroutine launches in `init{}` blocks
- ❌ No DI access in `init{}` blocks
- ❌ No `Dispatchers.Main` in property initialization

**When to Run:**

- **MANDATORY** before every commit touching shared code
- Before creating pull requests
- When adding new Compose UI components
- When modifying DI code

**See:** [CLAUDE_iOS.md](../CLAUDE_iOS.md) for comprehensive iOS safety patterns

---

### ♿ test_accessibility.sh - **Accessibility Compliance Testing**

Runs WCAG 2.1 Level AA compliance tests.

**Purpose:**

- Validate contentDescription on interactive elements
- Check touch target sizes (48dp Android / 44pt iOS)
- Verify screen reader compatibility
- Test color contrast ratios

**Usage:**

```bash
./scripts/test_accessibility.sh
```

**Requirements:**

- All UI components must have semantic labels
- Minimum touch target sizes enforced
- Text must scale with system font size
- Color contrast must meet WCAG AA standards

**When to Run:**

- Before each pull request
- When adding new UI components
- When modifying existing UI

**See:** [docs/accessibility-guide.md](../docs/accessibility-guide.md)

---

### 🚀 pre-push-verify.sh - **Pre-Push Validation**

Runs all tests matching GitHub Actions workflows locally.

**Purpose:**

- Prevent CI failures by validating locally
- Reduce GitHub Actions costs
- Ensure code quality before push

**Usage:**

```bash
./scripts/pre-push-verify.sh
```

**Tests Run:**

- Android build (assembleDebug)
- iOS Kotlin compilation
- Unit test suite (902+ tests)
- Code quality checks (detekt, SwiftLint)
- Test anti-pattern detection

**When to Run:**

- Before `git push` to origin
- Before creating pull requests
- After making significant changes

---

## Testing Scripts

### detect-test-antipatterns.sh

Detects testing anti-patterns and violations.

**Checks:**

- Thread.sleep() usage (flaky timing)
- Missing @Test annotations
- Long test methods (>50 lines)
- Mock misuse
- Test isolation issues

**Usage:**

```bash
./scripts/detect-test-antipatterns.sh
```

**Used By:** GitHub Actions (03-code-quality.yml)

---

### run-test-quality-validation.sh

Comprehensive test quality validation combining multiple checks.

**Stages:**

1. Unit test execution
2. Anti-pattern detection
3. Integration test validation
4. Performance budget checks
5. Coverage analysis

**Usage:**

```bash
./scripts/run-test-quality-validation.sh
```

**Used By:** GitHub Actions (test-quality.yml)

---

### validate_test_setup.sh

Validates E2E test configuration and infrastructure.

**Checks:**

- testTag implementation in UI components
- Build configuration (ENABLE_SIMULATION_MODE)
- Android/iOS test files
- Firebase Test Lab scripts
- Test documentation

**Usage:**

```bash
./scripts/validate_test_setup.sh
```

---

### test-performance-monitor.sh

Monitors test execution performance.

**Features:**

- Identifies slow tests
- Validates performance budgets (100ms per unit test)
- Generates performance reports

**Usage:**

```bash
./scripts/test-performance-monitor.sh
```

---

### test-odr-bundle-generation.sh

Tests iOS On-Demand Resources (ODR) bundle generation.

**Purpose:**

- Validates Info.plist configuration
- Ensures idempotent execution
- Tests ODR asset tags

**Usage:**

```bash
./scripts/test-odr-bundle-generation.sh
```

---

## Build & Maintenance Scripts

### add-headers.sh

Adds Apache 2.0 copyright headers to source files.

**Supported Files:**

- Kotlin (.kt)
- Swift (.swift)
- Shell scripts (.sh)
- Python (.py)

**Usage:**

```bash
./scripts/add-headers.sh
```

---

### clean_xcode.sh

Cleans Xcode caches and derived data.

**Purpose:**

- Resolve build issues
- Prevent GUID conflicts
- Clear Swift Package Manager state

**Usage:**

```bash
./scripts/clean_xcode.sh
```

**When to Use:**

- iOS builds failing unexpectedly
- After Xcode updates
- When seeing "DerivedData" errors

---

### generate_firebase_config.sh

Generates Firebase configuration for both platforms.

**Usage:**

```bash
./scripts/generate_firebase_config.sh
```

**Requires:**

- `local.properties` with Firebase credentials
- Environment variables or config files

---

### generate_ios_firebase_config.sh

Generates GoogleService-Info.plist for iOS.

**Usage:**

```bash
./scripts/generate_ios_firebase_config.sh
```

**Generates:**

- `iosApp/GoogleService-Info.plist`

---

## Testing Dashboards

Located in `scripts/dashboards/`

### test-analytics-reporter.sh

Generates comprehensive test analytics.

**Features:**

- Historical test tracking
- Performance trending
- Quality metrics evolution
- Recommendations

**Usage:**

```bash
./scripts/dashboards/test-analytics-reporter.sh
```

---

### test-execution-dashboard.sh

Real-time test execution monitoring dashboard.

**Features:**

- Live test metrics
- Quality indicators
- Performance analytics

**Usage:**

```bash
./scripts/dashboards/test-execution-dashboard.sh
```

---

### test-stability-tracker.sh

Tracks test stability by running tests multiple times.

**Features:**

- Detects flaky tests
- Analyzes performance consistency
- Generates stability reports

**Usage:**

```bash
./scripts/dashboards/test-stability-tracker.sh
```

---

### test-suite-health-check.sh

Comprehensive test suite health dashboard.

**Features:**

- Test pyramid analysis
- Quality metrics
- Performance analysis

**Usage:**

```bash
./scripts/dashboards/test-suite-health-check.sh
```

---

## Firebase Test Lab Integration

Located in `scripts/firebase/`

### run_all_firebase_tests.sh

Orchestrates both Android and iOS E2E tests on Firebase Test Lab.

**Features:**

- Runs Android and iOS tests
- Reports results
- Provides next steps

**Usage:**

```bash
./scripts/firebase/run_all_firebase_tests.sh
```

**Requires:**

- `gcloud` CLI authenticated
- Firebase project configured

---

### run_android_firebase_tests.sh

Builds and runs Android E2E tests on Firebase Test Lab.

**Test Matrix:**

- Pixel 3 (1080x2160, API 30)
- Pixel 5 (1080x2340, API 31)
- Samsung Galaxy S10 (1440x3040, API 29)
- Samsung Galaxy S21 (1080x2400, API 31)
- Samsung Galaxy A51 (1080x2400, API 30)

**Usage:**

```bash
./scripts/firebase/run_android_firebase_tests.sh
```

---

### run_ios_firebase_tests.sh

Builds and runs iOS UI tests on Firebase Test Lab.

**Test Matrix:**

- iPhone 15 Pro (iOS 17)
- iPhone 14 (iOS 16)
- iPhone 13 (iOS 15)
- iPhone 8 (iOS 14)
- iPad Pro 12.9 (iOS 16)

**Usage:**

```bash
./scripts/firebase/run_ios_firebase_tests.sh
```

---

### collect_firebase_screenshots.sh

Downloads Firebase Test Lab screenshots from GCS bucket.

**Usage:**

```bash
./scripts/firebase/collect_firebase_screenshots.sh
```

**Output:**

- `firebase-results/android/screenshots/`
- `firebase-results/ios/screenshots/`

---

### download_firebase_logs.sh

Downloads Firebase Test Lab logs from GCS bucket.

**Usage:**

```bash
./scripts/firebase/download_firebase_logs.sh
```

**Output:**

- `firebase-results/android/logs/`
- `firebase-results/ios/logs/`

---

## Specialized Test Utilities

Located in `scripts/testing/`

### run_sound_choreography.sh

Launches real audio crowd simulation for testing sound choreography.

**Features:**

- Auto-detects Android SDK and emulators
- Enables audio support
- Launches activity with sound enabled

**Usage:**

```bash
./scripts/testing/run_sound_choreography.sh              # Default (prompt)
./scripts/testing/run_sound_choreography.sh --play       # Run automated test
./scripts/testing/run_sound_choreography.sh --open       # Only open emulator
```

---

## Map Generation

Located in `scripts/maps/`

**📚 [Full Documentation](./maps/README.md)** - Comprehensive guide to the 5-stage pipeline

**Key Scripts:**

- `10-download_osm.sh` - Download OpenStreetMap data
- `20-generate_mbtiles.sh` - Generate offline map tiles
- `30-retrieve-geojson.sh` - Extract city boundaries
- `35-generate-default-map-images.sh` - Generate map previews
- `40-generate-modules.sh` - Create Android Dynamic Feature Modules

**Requirements:** Docker, Docker Compose, 8GB+ RAM

---

## Image Processing

Located in `scripts/images/`

### enhance_ios_icon.py

Enhances iOS app icon with iOS-style effects.

**Features:**

- Squircle shape transformation
- Rounded corners
- Lighting effects
- Contrast enhancement

**Usage:**

```bash
python3 scripts/images/enhance_ios_icon.py input.png output.png
```

**Requires:** Python 3, Pillow

---

### create_android_icon.py

Generates Android adaptive launcher icons.

**Features:**

- Generates all densities (mdpi through xxxhdpi)
- Foreground layer
- Background layer
- Monochrome variant
- Legacy icons

**Usage:**

```bash
python3 scripts/images/create_android_icon.py input.png output_dir/
```

**Requires:** Python 3, Pillow

---

## Other Modules

### 📜 [Licenses](./licenses/)

License compliance and dependency management.

**Purpose:**

- Generate license reports
- Check dependency compliance
- Maintain legal documentation

---

### 🌍 [Polygons](./polygons/)

Geographic boundary processing and validation.

**Purpose:**

- Process city administrative boundaries
- Validate geographic data
- Generate boundary overlays

---

### 🎨 [Style](./style/)

Map style generation and customization.

**Purpose:**

- Generate custom MapLibre styles
- Theme map appearances
- Optimize styles for mobile rendering

---

### 🌐 [Translate](./translate/)

Localization and translation management.

**Purpose:**

- Manage app translations
- Automate localization workflows
- Validate translation completeness

---

## Common Workflows

### Before First Commit

```bash
# 1. Verify environment
./scripts/verify-setup.sh

# 2. Run all tests
./gradlew clean :shared:testDebugUnitTest

# 3. Check iOS safety (if touching shared code)
./scripts/verify-ios-safety.sh

# 4. Check accessibility (if touching UI)
./scripts/test_accessibility.sh

# 5. Pre-push verification
./scripts/pre-push-verify.sh
```

### Before Creating PR

```bash
# 1. Run full test suite
./scripts/pre-push-verify.sh

# 2. Check for test anti-patterns
./scripts/detect-test-antipatterns.sh

# 3. Verify iOS safety
./scripts/verify-ios-safety.sh

# 4. Check accessibility
./scripts/test_accessibility.sh
```

### Firebase E2E Testing

```bash
# 1. Validate test setup
./scripts/validate_test_setup.sh

# 2. Run all Firebase tests
./scripts/firebase/run_all_firebase_tests.sh

# 3. Collect screenshots
./scripts/firebase/collect_firebase_screenshots.sh

# 4. Generate report
python3 scripts/generate_test_report.py
```

### iOS Build Issues

```bash
# 1. Clean Xcode caches
./scripts/clean_xcode.sh

# 2. Rebuild Kotlin framework
./gradlew clean :shared:embedAndSignAppleFrameworkForXcode

# 3. Verify iOS safety
./scripts/verify-ios-safety.sh
```

---

## Troubleshooting

### Script Not Executable

```bash
chmod +x scripts/script-name.sh
```

### Missing Dependencies

```bash
# Verify environment
./scripts/verify-setup.sh

# Check specific module requirements
scripts/maps/check_deps.sh
```

### Docker Not Available

```bash
# Check Docker installation
docker --version
docker-compose --version

# Start Docker daemon (macOS)
open -a Docker
```

---

## Contributing

### Adding New Scripts

1. Create script in appropriate subdirectory:
   - `scripts/` - Core production tools
   - `scripts/dashboards/` - Monitoring/analytics
   - `scripts/testing/` - Specialized test utilities
   - `scripts/firebase/` - Firebase integration
   - `scripts/images/` - Image processing

2. Use `#!/usr/bin/env bash` shebang for portability

3. Include comprehensive header comments:

   ```bash
   #!/usr/bin/env bash

   # Script Name - Brief Description
   # Comprehensive description of what this script does

   set -e
   ```

4. Add error handling and logging

5. Make executable: `chmod +x script-name.sh`

6. Update this README.md

7. Test on both macOS and Linux (if applicable)

---

## Best Practices

- ✅ Use `set -e` for error handling
- ✅ Include usage documentation in script headers
- ✅ Validate inputs before processing
- ✅ Provide progress feedback for long operations
- ✅ Clean up temporary files on exit
- ✅ Use portable commands (avoid macOS-only tools)
- ✅ Test scripts on both macOS and Linux

---

**Last Updated:** October 27, 2025
**Version:** 4.0 (Reorganized structure)
**Maintainer:** WorldWideWaves Development Team
