# Local Simulator Testing Guide

> **Running E2E tests on local Android emulators and iOS simulators**

This guide explains how to run the Firebase Test Lab E2E tests locally on simulators/emulators for development and debugging.

---

## ✅ Current Setup Status

### Ready to Run:
- ✅ testTag() modifiers added to all UI components
- ✅ Debug build configured with simulation mode enabled
- ✅ Android E2E test files created and compiled
- ✅ iOS XCUITest files created
- ✅ Debug APK builds successfully

### Requires Setup:
- ⏳ Android emulator creation
- ⏳ iOS UI test target in Xcode

---

## 🤖 Android Local Testing

### Prerequisites

1. **Create Android Emulator** (if not exists):
```bash
# List available system images
sdkmanager --list | grep system-images

# Download a system image (example: Pixel 6, API 33)
sdkmanager "system-images;android-33;google_apis;arm64-v8a"

# Create AVD
avdmanager create avd \
  -n Pixel_6_API_33 \
  -k "system-images;android-33;google_apis;arm64-v8a" \
  -d pixel_6
```

2. **Start Emulator**:
```bash
# Start emulator in background
emulator -avd Pixel_6_API_33 -no-snapshot-load &

# Wait for boot
adb wait-for-device
```

### Run Android E2E Test

```bash
# Option 1: Run all instrumented tests
./gradlew :composeApp:connectedDebugAndroidTest

# Option 2: Run specific E2E test
./gradlew :composeApp:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.worldwidewaves.e2e.CompleteWaveParticipationE2ETest

# Option 3: Using adb
./gradlew :composeApp:assembleDebug
./gradlew :composeApp:assembleDebugAndroidTest
adb install -r composeApp/build/outputs/apk/debug/composeApp-debug.apk
adb install -r composeApp/build/outputs/apk/androidTest/debug/composeApp-debug-androidTest.apk
adb shell am instrument -w com.worldwidewaves.test/androidx.test.runner.AndroidJUnitRunner
```

### View Screenshots

Screenshots are saved to the emulator:
```bash
# Pull screenshots from emulator
adb pull /sdcard/Android/data/com.worldwidewaves/files/e2e_screenshots ./local_test_results/

# View screenshots
open ./local_test_results/
```

### Troubleshooting Android

**Issue: No emulator found**
```bash
# List running emulators
adb devices

# If empty, start one:
emulator -avd Pixel_6_API_33
```

**Issue: Test fails to install**
```bash
# Uninstall previous versions
adb uninstall com.worldwidewaves
adb uninstall com.worldwidewaves.test

# Reinstall
./gradlew :composeApp:installDebug
./gradlew :composeApp:installDebugAndroidTest
```

**Issue: Screenshots not found**
```bash
# Check if directory exists
adb shell ls /sdcard/Android/data/com.worldwidewaves/files/

# Create if missing
adb shell mkdir -p /sdcard/Android/data/com.worldwidewaves/files/e2e_screenshots
```

---

## 🍎 iOS Local Testing

### Prerequisites

1. **Add UI Test Target in Xcode**:

See [iosApp/worldwidewavesUITests/README.md](iosApp/worldwidewavesUITests/README.md) for detailed instructions.

Quick steps:
```bash
# 1. Open Xcode
cd iosApp
open worldwidewaves.xcodeproj

# 2. Add UI Test Target:
#    - Click project
#    - Click '+' on targets
#    - Select "UI Testing Bundle"
#    - Name: worldwidewavesUITests

# 3. Add test files to target:
#    - CompleteWaveParticipationUITest.swift
#    - XCUITestExtensions.swift
#    - ScreenshotHelper.swift
```

2. **Select iOS Simulator**:
```bash
# List available simulators
xcrun simctl list devices available | grep iPhone

# Boot a simulator
xcrun simctl boot "iPhone 15 Pro"

# Or open Simulator.app
open -a Simulator
```

### Run iOS E2E Test

**Option 1: From Xcode**
1. Open `iosApp/worldwidewaves.xcodeproj`
2. Select `worldwidewaves` scheme
3. Select simulator (iPhone 15 Pro)
4. Press `Cmd+U` to run tests
5. Or: Product > Test

**Option 2: Command Line**
```bash
# Run all UI tests
xcodebuild test \
  -project iosApp/worldwidewaves.xcodeproj \
  -scheme worldwidewaves \
  -destination 'platform=iOS Simulator,name=iPhone 15 Pro' \
  -only-testing:worldwidewavesUITests

# Run specific test
xcodebuild test \
  -project iosApp/worldwidewaves.xcodeproj \
  -scheme worldwidewaves \
  -destination 'platform=iOS Simulator,name=iPhone 15 Pro' \
  -only-testing:worldwidewavesUITests/CompleteWaveParticipationUITest/testCompleteWaveParticipationJourney
```

### View Screenshots (iOS)

Screenshots are attached to test results:

**From Xcode:**
1. Open Test Navigator (Cmd+6)
2. Click on test result
3. Click "Attachments" tab
4. Screenshots are listed with timestamps

**From Command Line:**
```bash
# Find test results bundle
find ~/Library/Developer/Xcode/DerivedData -name "*.xcresult" -type d | head -1

# Extract screenshots using xcparse (if installed)
xcparse screenshots <path-to-xcresult> ./ios_screenshots/

# Or install xcparse:
brew install chargepoint/xcparse/xcparse
```

### Troubleshooting iOS

**Issue: UI test target not found**
- Solution: Add UI test target in Xcode (see Prerequisites)

**Issue: Test fails with "Element not found"**
- Solution: Ensure simulation mode is enabled in app
- Check that testTags are accessible (`.otherElements["TestTag"]`)

**Issue: Simulator doesn't boot**
```bash
# Reset simulator
xcrun simctl erase "iPhone 15 Pro"

# Boot again
xcrun simctl boot "iPhone 15 Pro"
```

---

## 🧪 Quick Validation

### Validate Setup
```bash
# Run validation script
./scripts/validate_test_setup.sh
```

This checks:
- ✅ testTag implementation
- ✅ Build configuration
- ✅ Test files exist
- ✅ Scripts are executable
- ✅ Documentation is present

### Build Test
```bash
# Android
./gradlew :composeApp:assembleDebug
./gradlew :composeApp:compileDebugAndroidTestKotlin

# iOS
cd iosApp
xcodebuild build \
  -project worldwidewaves.xcodeproj \
  -scheme worldwidewaves \
  -destination 'platform=iOS Simulator,name=iPhone 15 Pro'
```

---

## 📊 Comparison: Local vs Firebase Test Lab

| Feature | Local Simulator | Firebase Test Lab |
|---------|----------------|-------------------|
| **Cost** | Free | ~$5.75/run |
| **Devices** | 1 at a time | 6 devices parallel |
| **Speed** | Faster (no upload) | Slower (build upload) |
| **Real devices** | No | Yes |
| **Screenshots** | Manual pull | Auto-collected |
| **Reports** | Manual | HTML auto-generated |
| **CI/CD** | Requires runner | Cloud-based |
| **Network conditions** | Limited | Multiple profiles |
| **Best for** | Development | Pre-release validation |

---

## 🎯 Recommended Workflow

### During Development:
1. **Make code changes**
2. **Run local simulator test** (fast iteration)
3. **Fix issues immediately**
4. **Repeat until working**

### Before PR/Release:
1. **Run Firebase Test Lab** (comprehensive)
2. **Collect screenshots**
3. **Generate HTML report**
4. **Review across all devices**
5. **Attach report to PR**

### CI/CD Pipeline:
- **PR commits**: Local simulator (GitHub Actions runner)
- **Main branch**: Firebase Test Lab (full device matrix)
- **Releases**: Firebase Test Lab + report archiving

---

## 📝 Current Status

### ✅ Ready:
- Android test infrastructure complete
- iOS test files created
- Build configuration ready
- Scripts executable

### ⏳ Next Steps:
1. **Create Android emulator** (5 minutes)
2. **Add iOS UI test target** (10 minutes)
3. **Run first local test** (verify setup)
4. **Run Firebase Test Lab** (production validation)

---

## 🚀 Quick Start Commands

```bash
# 1. Validate setup
./scripts/validate_test_setup.sh

# 2. Build APK
./gradlew :composeApp:assembleDebug

# 3. Create emulator (if needed)
avdmanager create avd -n TestDevice -k "system-images;android-33;google_apis;arm64-v8a" -d pixel_6

# 4. Run Android test
emulator -avd TestDevice &
./gradlew :composeApp:connectedDebugAndroidTest

# 5. Pull screenshots
adb pull /sdcard/Android/data/com.worldwidewaves/files/e2e_screenshots ./screenshots/
```

---

## 📚 Related Documentation

- [FIREBASE_TEST_LAB_GUIDE.md](FIREBASE_TEST_LAB_GUIDE.md) - Firebase Test Lab guide
- [iosApp/worldwidewavesUITests/README.md](iosApp/worldwidewavesUITests/README.md) - iOS setup
- [TODO_FIREBASE_UI.md](TODO_FIREBASE_UI.md) - Implementation plan

---

**Status:** Infrastructure ready, emulators/simulators setup required
**Last Updated:** October 2025
