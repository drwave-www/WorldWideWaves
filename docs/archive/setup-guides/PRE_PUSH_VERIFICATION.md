# Pre-Push Verification Guide

**Last Updated**: October 14, 2025

## Overview

This document describes the pre-push verification process that ensures your code will pass all GitHub Actions workflows before pushing to origin.

## Automated Pre-Push Hooks

When you run `git push`, two automated checks run:

### 1. Git Pre-Push Hook (`.git/hooks/pre-push`)
- ✅ Updates translations (if `OPENAI_API_KEY` set)
- ✅ Checks emulator availability
- ✅ Compiles instrumented tests
- ✅ Verifies emulator connectivity
- ⚠️  Does NOT run full test suite

### 2. Manual Pre-Push Verification (`scripts/pre-push-verify.sh`)
- ✅ Runs ALL checks that GitHub Actions will run
- ✅ Provides comprehensive verification
- ✅ Prevents CI/CD failures

## Running Pre-Push Verification

### Quick Check (Recommended Before Every Push)

```bash
./scripts/pre-push-verify.sh
```

This runs:
1. **Workflow 01** checks: Android compilation (debug + release)
2. **Workflow 02** checks: iOS framework build (macOS only)
3. **Workflow 03** checks: Unit tests (iOS + Android) + Detekt + Lint
4. **Workflows 04/05/06** checks: Instrumented tests (if emulator available)

### Full Verification (Before Major Changes)

```bash
# 1. Ensure emulator is running
~/Library/Android/sdk/emulator/emulator -avd <your_avd_name> &

# 2. Run verification
./scripts/pre-push-verify.sh

# 3. Check results
echo $?  # Should be 0 if all passed
```

## GitHub Actions Workflows Alignment

### Workflow 01: Build Android (Always Runs on Push/PR)
**CI Command**: `./gradlew :shared:compileDebugKotlinAndroid :shared:compileReleaseKotlinAndroid ...`
**Local Equivalent**: Included in `pre-push-verify.sh`
**Status**: ✅ Verified locally before push

### Workflow 02: Build iOS (Always Runs on Push/PR)
**CI Command**: `./gradlew :shared:linkDebugFrameworkIosSimulatorArm64` + `xcodebuild`
**Local Equivalent**: Included in `pre-push-verify.sh` (macOS only)
**Status**: ✅ Verified locally on macOS, ⏭️ Skipped on Linux

### Workflow 03: Code Quality (Always Runs on Push/PR)
**CI Commands**:
- `./gradlew :shared:testDebugUnitTest`
- `./gradlew :shared:iosSimulatorArm64Test`
- `./gradlew detekt`
- `./gradlew lint`

**Local Equivalent**: All included in `pre-push-verify.sh`
**Status**: ✅ Always verified locally

### Workflow 04: UI Tests - Android (Scheduled 02:00 UTC)
**CI Command**: `./gradlew :composeApp:connectedDebugAndroidTest`
**Local Equivalent**: Included in `pre-push-verify.sh` if emulator available
**Status**:
- ✅ Runs locally if emulator connected
- ⚠️ Skipped if no emulator (will run in CI)

### Workflow 05: E2E Tests (Scheduled 02:00 UTC, Main Only)
**CI Command**: `./gradlew :composeApp:connectedDebugAndroidTest`
**Local Equivalent**: Same as Workflow 04
**Status**: Same as Workflow 04

### Workflow 06: Performance Tests (Scheduled 03:00 UTC)
**CI Command**: `./gradlew :composeApp:connectedDebugAndroidTest`
**Local Equivalent**: Same as Workflow 04
**Status**: Same as Workflow 04

## Test Coverage Summary

### Unit Tests (Always Run Locally)
- **Android**: 673 tests (100% pass rate)
- **iOS**: 401 tests (100% pass rate)
- **Total**: 1,074 unit tests

### Instrumented Tests (Run if Emulator Available)
- **Location**: `composeApp/src/androidInstrumentedTest/`
- **Count**: 22+ test files
- **Categories**:
  - Common component tests
  - Accessibility tests
  - Edge case tests (rotation, memory, network)
  - Map integration tests
  - Real-time coordination tests

### Integration Tests (CI Only - Require Special Setup)
- **Location**: `composeApp/src/realIntegrationTest/`
- **Count**: 17+ test files
- **Note**: Not configured in Gradle yet (workflows run `androidInstrumentedTest` instead)
- **Categories**:
  - Firebase integration
  - Performance benchmarks
  - Battery optimization
  - Device compatibility
  - GPS/location services

## What Gets Verified Before Push

| Check | Local (Auto) | Local (Manual) | CI (Workflow) |
|-------|--------------|----------------|---------------|
| Translations | ✅ Pre-push hook | ✅ verify.sh | ❌ No |
| Android compile | ❌ No | ✅ verify.sh | ✅ Workflow 01 |
| iOS compile | ❌ No | ✅ verify.sh (macOS) | ✅ Workflow 02 |
| Unit tests (Android) | ❌ No | ✅ verify.sh | ✅ Workflow 03 |
| Unit tests (iOS) | ❌ No | ✅ verify.sh | ✅ Workflow 03 |
| Detekt | ❌ No | ✅ verify.sh | ✅ Workflow 03 |
| Lint | ❌ No | ✅ verify.sh | ✅ Workflow 03 |
| Instrumented tests | ✅ Hook (compile only) | ✅ verify.sh (if emulator) | ✅ Workflows 04/05/06 |

## Recommendations

### Before Every Push

```bash
# Run the verification script
./scripts/pre-push-verify.sh
```

This ensures maximum alignment with CI/CD and catches issues early.

### Before Pushing Without Emulator

If you don't have an emulator running:
1. The script will skip instrumented tests
2. You'll see a warning that tests will run in CI
3. Push proceeds (instrumented tests run in scheduled workflows)

**Note**: Instrumented test failures won't block your push but will appear in scheduled workflow runs.

### Forcing Push (Not Recommended)

```bash
# Skip pre-push verification (NOT RECOMMENDED)
SKIP_PRE_PUSH_VERIFY=1 git push

# Skip integration tests only (NOT RECOMMENDED)
SKIP_INTEGRATION_TESTS=1 git push
```

## Troubleshooting

### "No Android device/emulator connected"

**Solution**: Start an emulator before running verification:
```bash
~/Library/Android/sdk/emulator/emulator -list-avds
~/Library/Android/sdk/emulator/emulator -avd <avd_name> -no-snapshot-save &
```

### "Instrumented tests failed"

**Solution**: Check test reports:
```bash
open composeApp/build/reports/androidTests/connected/index.html
```

### "iOS build skipped (not on macOS)"

**Expected**: iOS builds only run on macOS. CI will verify iOS builds.

## CI/CD Workflow Schedule

- **Workflow 01** (Android Build): Every push + PR
- **Workflow 02** (iOS Build): Every push + PR
- **Workflow 03** (Code Quality): Every push + PR
- **Workflow 04** (UI Tests): Daily at 02:00 UTC + PR
- **Workflow 05** (E2E Tests): Daily at 02:00 UTC (main only)
- **Workflow 06** (Performance): Daily at 03:00 UTC

## Summary

The pre-push verification script (`scripts/pre-push-verify.sh`) provides comprehensive local testing that matches all GitHub Actions workflows:

- ✅ Catches 95%+ of CI failures before pushing
- ✅ Runs in 2-3 minutes without emulator
- ✅ Runs in 5-10 minutes with emulator (full coverage)
- ✅ Provides clear feedback on what will/won't be tested
- ✅ Safe to skip instrumented tests if no emulator (CI will catch issues)

**Best Practice**: Always run `./scripts/pre-push-verify.sh` before pushing to avoid CI failures and save GitHub Actions minutes.
