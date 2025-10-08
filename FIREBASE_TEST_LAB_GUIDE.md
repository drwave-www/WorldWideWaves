# Firebase Test Lab - Complete Guide

> **Comprehensive Firebase Test Lab E2E UI Testing for WorldWideWaves**

This guide covers the complete Firebase Test Lab integration for running automated E2E UI tests on both Android and iOS platforms.

---

## 📋 Table of Contents

- [Overview](#overview)
- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [Running Tests](#running-tests)
- [Viewing Results](#viewing-results)
- [Cost Management](#cost-management)
- [Troubleshooting](#troubleshooting)

---

## Overview

### What is Firebase Test Lab?

Firebase Test Lab provides cloud-based infrastructure to test Android and iOS apps on real devices. Our implementation includes:

- **21-step E2E test** covering complete user journey
- **6 device configurations** (3 Android + 3 iOS)
- **Automatic screenshot capture** at each step
- **HTML report generation** with side-by-side comparison
- **CI/CD ready** scripts

### Test Coverage

The E2E test validates:
1. App launch in debug/simulation mode
2. Event browsing and filtering
3. Favorites management
4. Map download verification
5. Event details viewing
6. Map interaction
7. Wave participation and choreography
8. About section navigation
9. FAQ interaction
10. Simulation mode verification

---

## Prerequisites

### 1. Firebase Project Setup

```bash
# Install Firebase CLI
npm install -g firebase-tools

# Login to Firebase
firebase login

# Create or select project
firebase projects:list
```

### 2. Google Cloud SDK

```bash
# Install gcloud CLI
brew install google-cloud-sdk

# Authenticate
gcloud auth login

# Set project
gcloud config set project worldwidewaves-test
```

### 3. Environment Variables

Add to your `.bashrc` or `.zshrc`:

```bash
export FIREBASE_PROJECT_ID="worldwidewaves-test"
export FIREBASE_RESULTS_BUCKET="worldwidewaves-test-results"
```

### 4. Firebase Test Lab API

Enable Firebase Test Lab API in your project:
1. Go to [Firebase Console](https://console.firebase.google.com)
2. Select your project
3. Navigate to Test Lab
4. Enable the API if prompted

---

## Quick Start

### Step 1: Run All Tests

```bash
# Run both Android and iOS tests
./scripts/run_all_firebase_tests.sh
```

This will:
- Build Android APKs
- Build iOS test bundle
- Run tests on 6 devices
- Upload results to Cloud Storage
- Display result URLs

### Step 2: Collect Screenshots

```bash
# Download screenshots from Cloud Storage
./scripts/collect_firebase_screenshots.sh
```

Screenshots will be saved to:
- `test_results/firebase/android/`
- `test_results/firebase/ios/`

### Step 3: Generate Report

```bash
# Generate HTML report with side-by-side screenshots
python3 scripts/generate_test_report.py
```

Open the report:
```bash
open test_results/firebase_test_report.html
```

---

## Running Tests

### All Platforms (Recommended)

```bash
./scripts/run_all_firebase_tests.sh
```

**Output:**
- Android test results
- iOS test results
- Summary with pass/fail status
- Next steps instructions

### Android Only

```bash
./scripts/run_android_firebase_tests.sh
```

**Devices:**
- Pixel 6 (API 33) - redfin
- Pixel 5 (API 31) - redfin
- Samsung Galaxy S22 (API 33) - b0q

**Execution time:** ~15 minutes

### iOS Only

```bash
./scripts/run_ios_firebase_tests.sh
```

**Devices:**
- iPhone 14 Pro (iOS 16.6)
- iPhone 13 (iOS 16.6)
- iPad Pro 12.9" (iOS 16.6)

**Execution time:** ~20 minutes

---

## Viewing Results

### Firebase Console

1. Go to [Firebase Console](https://console.firebase.google.com)
2. Select your project
3. Navigate to Test Lab > Test History
4. Click on the latest test run

**Available:**
- Test execution videos
- Screenshots
- Performance metrics
- Logs
- Test reports

### Cloud Storage

Results are stored in:
```
gs://worldwidewaves-test-results/
├── android/
│   └── YYYYMMDD_HHMMSS/
│       ├── device1/
│       ├── device2/
│       └── device3/
└── ios/
    └── YYYYMMDD_HHMMSS/
        ├── device1/
        ├── device2/
        └── device3/
```

### HTML Report

The generated HTML report includes:
- Side-by-side Android vs iOS screenshots
- Step-by-step journey visualization
- Device information
- Clickable fullscreen images
- Summary statistics

---

## Cost Management

### Pricing

Firebase Test Lab pricing (as of 2025):

| Platform | Cost per Device-Hour | Typical Test Duration | Cost per Device |
|----------|---------------------|----------------------|-----------------|
| Android  | ~$1                 | ~15 minutes          | ~$0.25          |
| iOS      | ~$5                 | ~20 minutes          | ~$1.67          |

**Per Test Run:**
- Android (3 devices): ~$0.75
- iOS (3 devices): ~$5.00
- **Total**: ~$5.75

**Monthly (30 daily runs):** ~$172.50

### Free Quota

**Spark Plan:**
- 10 tests/day on physical devices (free)
- 5 tests/day on virtual devices (free)

**Optimization Tips:**
1. Run tests only on PRs to main branch
2. Use local simulators for development
3. Limit device matrix for frequent runs
4. Use virtual devices when possible

### Custom Device Matrix

Edit scripts to use fewer devices:

```bash
# In run_android_firebase_tests.sh
--device model=redfin,version=33,locale=en_US,orientation=portrait \
# Remove other devices

# In run_ios_firebase_tests.sh
--device model=iphone14pro,version=16.6,locale=en_US,orientation=portrait \
# Remove other devices
```

---

## Troubleshooting

### Issue: gcloud CLI not found

**Solution:**
```bash
brew install google-cloud-sdk
gcloud auth login
gcloud config set project worldwidewaves-test
```

### Issue: Firebase Test Lab API not enabled

**Solution:**
1. Go to [Google Cloud Console](https://console.cloud.google.com)
2. Enable "Firebase Test Lab API"
3. Wait 2-3 minutes for propagation

### Issue: APK build fails

**Solution:**
```bash
# Clean and rebuild
./gradlew clean
./gradlew :composeApp:assembleDebug
./gradlew :composeApp:assembleDebugAndroidTest
```

### Issue: iOS build fails

**Solution:**
```bash
# Ensure Xcode is installed
xcode-select --install

# Add UI test target in Xcode (see iosApp/worldwidewavesUITests/README.md)
cd iosApp
open worldwidewaves.xcodeproj
```

### Issue: No screenshots found

**Solution:**
```bash
# Verify screenshot paths in test code
# Android: composeApp/src/androidInstrumentedTest/.../BaseE2ETest.kt
# iOS: iosApp/worldwidewavesUITests/ScreenshotHelper.swift

# Check if tests passed
gcloud firebase test ios list
gcloud firebase test android list
```

### Issue: Permission denied on scripts

**Solution:**
```bash
chmod +x scripts/run_*.sh
chmod +x scripts/collect_*.sh
chmod +x scripts/generate_*.py
```

### Issue: Python script fails

**Solution:**
```bash
# Install Python 3
brew install python3

# Verify
python3 --version

# Run script
python3 scripts/generate_test_report.py
```

---

## CI/CD Integration

### GitHub Actions

Create `.github/workflows/firebase-test-lab.yml`:

```yaml
name: Firebase Test Lab E2E

on:
  pull_request:
    branches: [main]
  workflow_dispatch:

jobs:
  firebase-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Setup gcloud
        uses: google-github-actions/setup-gcloud@v1
        with:
          service_account_key: ${{ secrets.FIREBASE_SERVICE_ACCOUNT }}
          project_id: worldwidewaves-test

      - name: Run Firebase Tests
        run: ./scripts/run_all_firebase_tests.sh

      - name: Collect Screenshots
        if: always()
        run: ./scripts/collect_firebase_screenshots.sh

      - name: Generate Report
        if: always()
        run: python3 scripts/generate_test_report.py

      - name: Upload Report
        if: always()
        uses: actions/upload-artifact@v3
        with:
          name: firebase-test-report
          path: test_results/firebase_test_report.html
```

### Service Account Setup

1. Create service account in Google Cloud Console
2. Grant roles:
   - Firebase Test Lab Admin
   - Storage Object Admin
3. Download JSON key
4. Add to GitHub Secrets as `FIREBASE_SERVICE_ACCOUNT`

---

## Advanced Usage

### Custom Test Filters

Run specific test class:

```bash
# Android
gcloud firebase test android run \
  --test-targets "class com.worldwidewaves.e2e.CompleteWaveParticipationE2ETest"

# iOS
gcloud firebase test ios run \
  --test-targets "WorldWideWavesUITests/CompleteWaveParticipationUITest/testCompleteWaveParticipationJourney"
```

### Network Conditions

Test with different network profiles:

```bash
# Add to device configuration
--network-profile LTE

# Available profiles: LTE, HSPA, EDGE, 3G
```

### Test Sharding

Split tests across multiple devices:

```bash
# Add to test configuration
--num-uniform-shards 3
```

### Video Recording

Enable video recording:

```bash
# Add to test configuration
--record-video
```

---

## Best Practices

1. **Run tests in debug build** with simulation enabled
2. **Use consistent test data** (Paris France event)
3. **Monitor test duration** (timeout is 20 minutes)
4. **Review screenshots** after each run
5. **Archive test results** for regression analysis
6. **Update device matrix** quarterly for latest devices
7. **Monitor Firebase quota** usage
8. **Use CI/CD** for automated runs on PRs

---

## Related Documentation

- [TODO_FIREBASE_UI.md](TODO_FIREBASE_UI.md) - Implementation plan
- [iosApp/worldwidewavesUITests/README.md](iosApp/worldwidewavesUITests/README.md) - iOS setup
- [Firebase Test Lab Docs](https://firebase.google.com/docs/test-lab)
- [gcloud CLI Reference](https://cloud.google.com/sdk/gcloud/reference/firebase/test)

---

## Support

For issues or questions:
1. Check [Troubleshooting](#troubleshooting) section
2. Review Firebase Test Lab console logs
3. Check GitHub Actions logs (if using CI/CD)
4. Open issue in repository

---

**Last Updated:** October 2025
**Version:** 1.0
**Status:** Production Ready ✅
