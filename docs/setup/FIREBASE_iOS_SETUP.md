# Firebase iOS Setup - Crashlytics Integration

This guide explains how to set up Firebase Crashlytics for the iOS app, mirroring the Android implementation.

## Overview

Firebase Crashlytics provides real-time crash reporting for iOS, helping track and fix stability issues. This setup matches the Android Crashlytics configuration already in place.

## Prerequisites

- Xcode 15.0 or later
- iOS 16.0+ deployment target
- Firebase project with iOS app registered
- Bundle ID: `com.worldwidewaves`

## Step 1: Generate GoogleService-Info.plist

The project includes a script to automatically generate `GoogleService-Info.plist` from secure sources:

```bash
# Generate Firebase configuration for iOS
./scripts/generate_ios_firebase_config.sh
```

### Configuration Sources

The script reads Firebase configuration from:

1. **Environment variables** (highest priority):
   ```bash
   export FIREBASE_PROJECT_ID="your-project-id"
   export FIREBASE_PROJECT_NUMBER="your-project-number"
   export FIREBASE_IOS_APP_ID="your-ios-app-id"
   export FIREBASE_API_KEY="your-api-key"
   ```

2. **local.properties** (development):
   ```properties
   FIREBASE_PROJECT_ID=your-project-id
   FIREBASE_PROJECT_NUMBER=your-project-number
   FIREBASE_IOS_APP_ID=your-ios-app-id
   FIREBASE_API_KEY=your-api-key
   ```

   **Note**: For iOS, use `FIREBASE_IOS_APP_ID` (falls back to `FIREBASE_MOBILE_SDK_APP_ID` if not set)

### Security Note

- **Never commit** `GoogleService-Info.plist` to version control
- The file is already in `.gitignore`
- Regenerate for each environment (dev/staging/production)

## Step 2: Add Firebase SDK via Swift Package Manager

1. **Open Xcode project**:
   ```bash
   cd iosApp
   open worldwidewaves.xcodeproj
   ```

2. **Add Firebase package**:
   - Go to **File → Add Package Dependencies...**
   - Enter URL: `https://github.com/firebase/firebase-ios-sdk`
   - Version: **11.0.0** or later (recommended: use "Up to Next Major Version")
   - Click **Add Package**

3. **Select required products**:
   - ✅ **FirebaseAnalytics** - Analytics and core functionality
   - ✅ **FirebaseCrashlytics** - Crash reporting
   - Click **Add Package**

4. **Verify installation**:
   - Check that `Package Dependencies` in project navigator shows `firebase-ios-sdk`
   - Verify `FirebaseCore` and `FirebaseCrashlytics` appear in target's **Frameworks, Libraries, and Embedded Content**

## Step 3: Configure Xcode Build Settings

### Add Upload Symbols Script

Crashlytics requires dSYM upload for crash symbolication:

1. **Add Run Script Phase**:
   - Select **worldwidewaves** target
   - Go to **Build Phases** tab
   - Click **+** → **New Run Script Phase**
   - Name: `Upload dSYMs to Crashlytics`
   - Move it **after** "Compile Sources" phase

2. **Add script**:
   ```bash
   # Upload dSYMs to Firebase Crashlytics
   "${BUILD_DIR%/Build/*}/SourcePackages/checkouts/firebase-ios-sdk/Crashlytics/run"
   ```

3. **Configure inputs**:
   - Add input file:
     ```
     ${DWARF_DSYM_FOLDER_PATH}/${DWARF_DSYM_FILE_NAME}/Contents/Resources/DWARF/${TARGET_NAME}
     ```

   - Add input file:
     ```
     $(SRCROOT)/worldwidewaves/GoogleService-Info.plist
     ```

4. **Configure outputs**:
   - Add output file:
     ```
     $(DERIVED_FILE_DIR)/Crashlytics-uploaded-${CONFIGURATION}-${PLATFORM_NAME}
     ```

### Debug Symbols Configuration

Ensure debug symbols are generated for release builds:

1. Go to **Build Settings**
2. Search for **Debug Information Format**
3. Set **Release** to: **DWARF with dSYM File**
4. Search for **Strip Debug Symbols During Copy**
5. Set **Release** to: **Yes**

## Step 4: Verify Implementation

### Code Changes

The following files have been updated:

1. **AppDelegate.swift**:
   - Added `import FirebaseCore`
   - Added `import FirebaseCrashlytics`
   - Calls `FirebaseApp.configure()` in `didFinishLaunchingWithOptions`

2. **GoogleService-Info.plist**:
   - Generated automatically by script
   - Must be present in `iosApp/worldwidewaves/` directory
   - Added to Xcode project (ensure it's in the target)

### Build and Test

1. **Generate Firebase config**:
   ```bash
   ./scripts/generate_ios_firebase_config.sh
   ```

2. **Add GoogleService-Info.plist to Xcode**:
   - Right-click on `worldwidewaves` folder in Xcode
   - Select **Add Files to "worldwidewaves"...**
   - Choose `GoogleService-Info.plist`
   - ✅ Ensure "Copy items if needed" is **unchecked** (it's already in the right place)
   - ✅ Ensure "worldwidewaves" target is **checked**
   - Click **Add**

3. **Build the project**:
   ```bash
   cd iosApp
   xcodebuild -project worldwidewaves.xcodeproj \
     -scheme worldwidewaves \
     -destination 'platform=iOS Simulator,name=iPhone 15 Pro' \
     build
   ```

4. **Run the app**:
   - Launch via Xcode (Cmd+R)
   - Check console for: `[AppDelegate] ✅ Firebase configured successfully`

5. **Test crash reporting**:
   ```swift
   // Add this test code temporarily to verify Crashlytics
   import FirebaseCrashlytics

   // Force a test crash (remove after testing!)
   // Crashlytics.crashlytics().setCustomValue("Test crash", forKey: "scenario")
   // fatalError("Test crash for Crashlytics verification")
   ```

## Step 5: Verify in Firebase Console

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project
3. Navigate to **Crashlytics**
4. After first crash, you should see:
   - Crash-free users percentage
   - Crash reports with stack traces
   - Device and OS version information

### First Crash Reporting

**Important**: Crashlytics requires the app to be restarted after a crash for the report to be sent.

1. Trigger a test crash (using code above)
2. App will crash and close
3. Relaunch the app
4. Wait 1-2 minutes for report to appear in Firebase Console

## CI/CD Integration

### GitHub Actions

Add to your iOS workflow:

```yaml
- name: Generate Firebase Config for iOS
  env:
    FIREBASE_PROJECT_ID: ${{ secrets.FIREBASE_PROJECT_ID }}
    FIREBASE_PROJECT_NUMBER: ${{ secrets.FIREBASE_PROJECT_NUMBER }}
    FIREBASE_IOS_APP_ID: ${{ secrets.FIREBASE_IOS_APP_ID }}
    FIREBASE_API_KEY: ${{ secrets.FIREBASE_API_KEY }}
  run: ./scripts/generate_ios_firebase_config.sh

- name: Build iOS with Crashlytics
  run: |
    cd iosApp
    xcodebuild -project worldwidewaves.xcodeproj \
      -scheme worldwidewaves \
      -configuration Release \
      -destination 'generic/platform=iOS' \
      clean build
```

### Required GitHub Secrets

Set these in your repository secrets (Settings → Secrets and variables → Actions):

- `FIREBASE_PROJECT_ID` - Your Firebase project ID
- `FIREBASE_PROJECT_NUMBER` - Your Firebase project number
- `FIREBASE_IOS_APP_ID` - iOS app ID from Firebase (format: `1:123456:ios:abc123def456`)
- `FIREBASE_API_KEY` - API key for iOS app
- `FIREBASE_CLIENT_ID` (optional) - For OAuth features
- `FIREBASE_REVERSED_CLIENT_ID` (optional) - For OAuth features

## Comparison with Android

| Feature | Android | iOS |
|---------|---------|-----|
| **Config File** | `google-services.json` | `GoogleService-Info.plist` |
| **Generation Script** | `generateFirebaseConfig` Gradle task | `generate_ios_firebase_config.sh` |
| **SDK Management** | Gradle dependencies | Swift Package Manager |
| **Initialization** | Auto-initialized by plugin | Manual: `FirebaseApp.configure()` |
| **Symbol Upload** | Automatic (Crashlytics Gradle plugin) | Manual (Run Script Phase) |
| **NDK Support** | Yes (`firebase-crashlytics-ndk`) | N/A (native Swift/Obj-C) |

## Troubleshooting

### GoogleService-Info.plist not found

```bash
# Ensure script has execute permissions
chmod +x scripts/generate_ios_firebase_config.sh

# Run the script
./scripts/generate_ios_firebase_config.sh

# Verify file was created
ls -la iosApp/worldwidewaves/GoogleService-Info.plist
```

### Firebase not initializing

Check console logs for errors:
```bash
xcrun simctl spawn booted log stream \
  --predicate 'process == "worldwidewaves"' \
  --level debug | grep -i firebase
```

### Crashes not appearing in console

1. Verify `GoogleService-Info.plist` is added to Xcode target
2. Check Firebase Console → Project Settings → Your apps → iOS app is registered
3. Ensure Bundle ID matches: `com.worldwidewaves`
4. Wait 1-2 minutes after relaunch (Crashlytics uploads are async)
5. Check that Upload dSYMs script is configured correctly

### Build errors with Firebase imports

```
error: no such module 'FirebaseCore'
```

**Solution**: Ensure Firebase package is properly added via SPM:
1. File → Packages → Resolve Package Versions
2. Clean build folder (Cmd+Shift+K)
3. Rebuild project (Cmd+B)

## Best Practices

1. **Never commit** `GoogleService-Info.plist` to version control
2. **Use different Firebase projects** for dev/staging/production
3. **Enable debug logging** during development:
   ```swift
   // In AppDelegate.swift (development only)
   #if DEBUG
   FirebaseConfiguration.shared.setLoggerLevel(.debug)
   #endif
   ```
4. **Test crash reporting** before releasing to production
5. **Monitor crash-free rate** in Firebase Console regularly
6. **Symbolicate crashes** by ensuring dSYMs are uploaded

## Additional Resources

- [Firebase iOS SDK Documentation](https://firebase.google.com/docs/ios/setup)
- [Crashlytics for iOS](https://firebase.google.com/docs/crashlytics/get-started?platform=ios)
- [Upload dSYMs](https://firebase.google.com/docs/crashlytics/get-deobfuscated-reports?platform=ios)
- [Android implementation](./FIREBASE_SETUP.md)

## Support

If you encounter issues:
1. Check Xcode console logs
2. Verify Firebase Console project settings
3. Review [Firebase iOS troubleshooting guide](https://firebase.google.com/docs/ios/troubleshooting)
4. Open an issue in the repository with logs and error messages
