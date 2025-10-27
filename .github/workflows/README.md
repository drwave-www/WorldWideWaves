# WorldWideWaves CI/CD Workflows

This directory contains the GitHub Actions workflows for the WorldWideWaves project. These workflows ensure code quality, comprehensive testing, and reliable builds across multiple platforms.

## 🚀 Workflow Overview

### Core Workflows

| Workflow | Purpose | Triggers | Duration |
|----------|---------|----------|-----------|
| **01-build-android.yml** | Android compilation | Push/PR | ~5-8 min |
| **02-build-ios.yml** | iOS compilation | Push/PR | ~8-12 min |
| **03-code-quality.yml** | Code quality and linting | Push/PR | ~3-5 min |
| **04-ui-tests-android.yml** | UI/Integration tests | Push/PR/Daily | ~25-45 min |
| **05-e2e-tests.yml** | End-to-end testing | Push/PR | ~15-25 min |
| **06-performance-tests.yml** | Performance benchmarks | Push/PR | ~10-20 min |
| **07-docs-check.yml** | Documentation link validation | PR (markdown changes) | ~2-5 min |
| **99-pipeline-status.yml** | Overall status aggregation | Push/PR | ~1-2 min |

## 📝 Documentation Link Check (07-docs-check.yml)

Automated validation of documentation quality and link integrity:

### Features
- **Link Validation**: Checks all internal and external links in markdown files
- **Markdown Linting**: Enforces consistent markdown formatting standards
- **Smart Caching**: Caches link check results to speed up subsequent runs
- **PR Comments**: Automatically comments on PRs with broken link details
- **Selective Triggers**: Only runs when markdown files are modified

### Link Check Capabilities
- ✅ **Internal Links**: Validates relative paths to files and headings
- ✅ **External URLs**: Verifies HTTP/HTTPS links are accessible
- ✅ **Intelligent Exclusions**: Skips localhost, build directories, and known false positives
- ✅ **Retry Logic**: Retries failed links up to 3 times to avoid transient failures
- ✅ **Timeout Control**: 30-second timeout per link to prevent hanging

### Markdown Linting
- Consistent heading styles (ATX format)
- Proper list formatting and indentation
- Code block language specification
- Trailing whitespace detection
- File ending with newline

### Configuration Files
- `.lycheeignore`: Patterns for links/paths to exclude from checking
- `.markdownlint-cli2.jsonc`: Markdown linting rules and preferences

### Excluded Patterns
- `node_modules`, `build`, `SourcePackages` directories
- Firebase/Google service URLs (authentication required)
- App Store/Play Store links (region-specific)
- Example URLs (example.com, localhost)
- Email addresses (via --exclude-mail flag)

## 📱 Android Instrumented Tests (NEW)

The most significant addition to support the new UI testing implementation:

### Features
- **Multi-API Testing**: Tests on Android API 29 & 33 for broader compatibility
- **Emulator Caching**: AVD snapshots cached for faster subsequent runs
- **Screenshot Artifacts**: Automatic collection of visual regression test screenshots
- **Performance Analysis**: Test execution timing and performance metrics
- **Comprehensive Coverage**: All new test categories (Edge Cases, Accessibility, Screenshots)

### Test Categories Covered
- 🔄 **Edge Case Testing** (`EdgeCaseTest.kt`)
  - Device rotation and configuration changes
  - Memory constraints and resource cleanup
  - Network connectivity edge cases
  - Battery optimization scenarios
  - Multi-window and split screen support

- ♿ **Accessibility Testing** (`AccessibilityTest.kt`)
  - Screen reader compatibility (TalkBack/VoiceOver)
  - Keyboard navigation and focus management
  - WCAG 2.1 compliance (color contrast, text scaling)
  - Motor accessibility (touch targets, gesture alternatives)
  - Cognitive accessibility (clear UI patterns, error guidance)

- 📸 **Screenshot Testing** (`ScreenshotTestUtils.kt`)
  - Visual regression detection
  - Baseline screenshot management
  - Animation frame capture for choreography testing
  - Cross-device screenshot comparison

### Artifacts Generated
- **Test Reports**: JUnit XML results with detailed pass/fail information
- **Screenshots**: PNG files organized by test category and device
- **Coverage Reports**: Code coverage analysis with Jacoco
- **Performance Reports**: Test execution timing and bottleneck analysis

## 🔍 Quality Gates Enhancement

Enhanced to include UI test compilation checks:

### New Checks Added
- **UI Test Compilation**: Ensures all instrumented tests compile successfully
- **Test Class Verification**: Confirms required test files exist
- **Coverage Integration**: Jacoco test coverage reporting
- **Quality Summary**: Aggregated pass/fail status across all quality dimensions

## 📊 Status Check Integration

Updated to provide comprehensive CI/CD status:

### Features
- **Workflow Orchestration**: Waits for and aggregates all workflow results
- **Intelligent Timeouts**: Monitors running workflows with configurable timeouts
- **Rich Summaries**: Detailed status reports with test coverage breakdown
- **PR Comments**: Automatic test result comments on pull requests

## ⚡ Performance Optimizations

### Caching Strategy
- **Gradle Dependencies**: Cached across workflow runs
- **Android AVD**: Emulator snapshots cached for faster startup
- **Build Artifacts**: Intermediate build outputs cached

### Parallel Execution
- **Independent Workflows**: Quality gates, builds, and tests run in parallel
- **Matrix Strategy**: Multiple Android API levels tested simultaneously
- **Selective Triggers**: UI tests can be skipped for documentation-only changes

### Resource Management
- **Concurrency Control**: Prevents multiple runs of same workflow
- **Timeout Configuration**: Prevents hanging workflows from blocking CI
- **Artifact Retention**: Configurable cleanup of old test artifacts

## 🔧 Configuration

### Environment Variables
```yaml
# Android Emulator Configuration
API_LEVEL: [29, 33]  # Test on Android 10 and 13
RAM_SIZE: 4096M      # Sufficient memory for UI tests
HEAP_SIZE: 1024M     # JVM heap for test execution
DISK_SIZE: 8192M     # Storage for emulator and test data

# Test Execution
UI_TIMEOUT_MS: 5000          # Maximum UI interaction timeout
SCREENSHOT_RETENTION: 14     # Days to keep screenshot artifacts
TEST_REPORT_RETENTION: 30    # Days to keep test reports
```

### Triggers
- **Push to main**: All workflows execute
- **Pull Requests**: All workflows execute
- **Daily Schedule**: UI tests run at 2 AM UTC to catch flaky tests
- **Manual Dispatch**: Status check can be triggered manually

## 📈 Monitoring and Reporting

### Test Results
- **JUnit Reports**: Structured XML test results
- **HTML Reports**: Human-readable test summaries
- **Coverage Reports**: Line and branch coverage analysis
- **Performance Metrics**: Test execution timing data

### Artifacts Management
- **Screenshot Gallery**: Visual test results organized by category
- **Failure Analysis**: Detailed logs for failed tests
- **Trend Analysis**: Historical test performance data
- **Regression Detection**: Automatic comparison with baseline screenshots

## 🚨 Failure Handling

### Retry Logic
- **Emulator Startup**: Automatic retry for emulator boot failures
- **Network Issues**: Retry for transient connectivity problems
- **Test Flakiness**: Matrix strategy allows partial failures

### Notifications
- **PR Comments**: Automatic test result summaries
- **Failure Reports**: Detailed artifact upload for investigation
- **Status Badges**: Repository README status indicators

## 📚 Best Practices

### For Developers
1. **Local Testing**: Run `./gradlew connectedDebugAndroidTest` before pushing
2. **Screenshot Baselines**: Update baselines when UI intentionally changes
3. **Test Naming**: Follow naming convention for test categorization
4. **Performance**: Be mindful of test execution time in CI

### For Maintainers
1. **Workflow Updates**: Test changes in feature branches first
2. **Artifact Cleanup**: Monitor storage usage and adjust retention
3. **Performance Monitoring**: Watch for CI duration increases
4. **Emulator Updates**: Keep Android API levels current

## 🔄 Migration Notes

### From Previous CI Setup
- **New Dependencies**: Android emulator actions added
- **Increased Duration**: UI tests add 20-30 minutes to CI time
- **Storage Requirements**: Screenshot artifacts require additional storage
- **API Level Testing**: Now testing multiple Android versions

### Compatibility
- **Existing Tests**: All previous unit tests continue to run
- **Build Process**: No changes to existing build workflows
- **Deployment**: No impact on release processes
- **Branch Protection**: May need to update required status checks

## 📖 References

- [Android Emulator Runner Action](https://github.com/ReactiveCircus/android-emulator-runner)
- [Compose UI Testing](https://developer.android.com/jetpack/compose/testing)
- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [Gradle Build Caching](https://docs.gradle.org/current/userguide/build_cache.html)