# Firebase iOS Auto-Generation Setup

This guide shows how to configure Xcode to automatically generate `GoogleService-Info.plist` during build, just like Android does with Gradle.

## Why Auto-Generation?

**Current Behavior**:

- **Android**: ✅ Automatic - `google-services.json` generated during every build
- **iOS**: ❌ Manual - Requires running `./scripts/generate_firebase_config.sh ios`

**After Setup**:

- **iOS**: ✅ Automatic - `GoogleService-Info.plist` generated during every build

## Setup Instructions

### 1. Open Xcode Project

```bash
cd iosApp
open worldwidewaves.xcodeproj
```

### 2. Add Run Script Build Phase

1. **Select the target**:
   - Click on `worldwidewaves` project in the left sidebar
   - Select the `worldwidewaves` target under "TARGETS"

2. **Go to Build Phases**:
   - Click the "Build Phases" tab at the top

3. **Add new Run Script Phase**:
   - Click the **+** button at the top left
   - Select **"New Run Script Phase"**

4. **Configure the script**:
   - Rename it to: `Generate Firebase Config`
   - **Drag it to position**: Place it **BEFORE** "Compile Sources" (very important!)
   - Add the following script:

```bash
#!/bin/bash
# Auto-generate GoogleService-Info.plist from local.properties or environment variables

set -e

SCRIPT_PATH="${PROJECT_DIR}/../scripts/generate_ios_firebase_config.sh"

# Check if script exists
if [ ! -f "$SCRIPT_PATH" ]; then
    echo "error: Firebase config generation script not found at $SCRIPT_PATH"
    exit 1
fi

# Run the generation script
echo "🔥 Auto-generating Firebase configuration..."
if "$SCRIPT_PATH"; then
    echo "✅ Firebase configuration generated successfully"
else
    echo "error: Firebase configuration generation failed"
    exit 1
fi
```

5. **Configure Input Files** (optional but recommended):
   - Click "Input Files" section
   - Add: `$(SRCROOT)/../local.properties`
   - This tells Xcode when to re-run the script

6. **Configure Output Files** (optional but recommended):
   - Click "Output Files" section
   - Add: `$(SRCROOT)/worldwidewaves/GoogleService-Info.plist`
   - This helps Xcode cache the result

### 3. Verify Setup

Build the project:

```bash
# From command line
cd iosApp
xcodebuild -project worldwidewaves.xcodeproj \
  -scheme worldwidewaves \
  -destination 'platform=iOS Simulator,name=iPhone 15 Pro' \
  build

# Or press Cmd+B in Xcode
```

Check the build log for:

```
🔥 Auto-generating Firebase configuration...
✅ Firebase configuration generated successfully
```

## How It Works

### Build Sequence

```
Xcode Build Started
    ↓
[NEW] Run Script: Generate Firebase Config
    ↓ Calls: ../scripts/generate_ios_firebase_config.sh
    ↓ Reads: local.properties or environment variables
    ↓ Generates: GoogleService-Info.plist
    ↓
Compile Sources (uses GoogleService-Info.plist)
    ↓
Link Binary
    ↓
Build Complete
```

### Cache Optimization

With Input/Output files configured, Xcode will:

- ✅ **Skip generation** if `local.properties` hasn't changed
- ✅ **Re-run automatically** if `local.properties` is modified
- ✅ **Faster incremental builds**

## Platform Parity Achieved

| Platform | Auto-Generation | Trigger | Build System |
| ---------- | ---------------- | --------- | -------------- |

| **Android** | ✅ Automatic | Every Gradle build | Gradle task |
| **iOS** | ✅ Automatic | Every Xcode build | Run Script Phase |

Both platforms now have identical behavior!

## Troubleshooting

### Script Not Found Error

```
error: Firebase config generation script not found
```

**Solution**: Ensure the path is correct relative to Xcode project:

- Xcode project: `iosApp/worldwidewaves.xcodeproj`
- Script location: `scripts/generate_ios_firebase_config.sh`
- Relative path: `../scripts/generate_ios_firebase_config.sh`

### Generation Failed Error

```
error: Firebase configuration generation failed
```

**Solution**: Check that `local.properties` has required fields:

```bash
# Verify configuration
cat local.properties | grep FIREBASE
```

Required fields:

- `FIREBASE_PROJECT_ID`
- `FIREBASE_PROJECT_NUMBER`
- `FIREBASE_IOS_APP_ID` (or `FIREBASE_MOBILE_SDK_APP_ID`)
- `FIREBASE_API_KEY` (or `FIREBASE_IOS_API_KEY`)

### Build Still Requires Manual Generation

**Symptom**: Build fails with "GoogleService-Info.plist not found"

**Solution**:

1. Verify Run Script Phase is **before** "Compile Sources"
2. Run once manually to bootstrap: `./scripts/generate_ios_firebase_config.sh`
3. Clean build folder: Product → Clean Build Folder (Cmd+Shift+K)
4. Rebuild

## CI/CD Integration

The Run Script Phase works automatically in CI/CD as long as:

1. **Environment variables are set** (recommended for CI):

   ```yaml
   - name: Build iOS
     env:
       FIREBASE_PROJECT_ID: ${{ secrets.FIREBASE_PROJECT_ID }}
       FIREBASE_PROJECT_NUMBER: ${{ secrets.FIREBASE_PROJECT_NUMBER }}
       FIREBASE_IOS_APP_ID: ${{ secrets.FIREBASE_IOS_APP_ID }}
       FIREBASE_API_KEY: ${{ secrets.FIREBASE_API_KEY }}
     run: |
       cd iosApp
       xcodebuild -project worldwidewaves.xcodeproj \
         -scheme worldwidewaves \
         build
   ```

2. **OR local.properties is generated first**:

   ```yaml
   - name: Generate local.properties
     run: |
       cat > local.properties << EOF
       FIREBASE_PROJECT_ID=${{ secrets.FIREBASE_PROJECT_ID }}
       ...
       EOF

   - name: Build iOS
     run: |
       cd iosApp
       xcodebuild ...
   ```

## Manual Generation (Still Available)

You can still generate manually if needed:

```bash
# iOS only
./scripts/generate_ios_firebase_config.sh

# Both platforms
./scripts/generate_firebase_config.sh all
```

Manual generation is useful for:

- Validating configuration before building
- Generating configs without building
- Debugging configuration issues

## Comparison with Android

### Android (Gradle-based)

**Location**: `composeApp/build.gradle.kts`

```kotlin
tasks.register("generateFirebaseConfig") { ... }

tasks.named("preBuild") {
    dependsOn("generateFirebaseConfig")  // Auto-runs before build
}
```

### iOS (Xcode-based)

**Location**: Xcode → Target → Build Phases → Run Script

```bash
"${PROJECT_DIR}/../scripts/generate_ios_firebase_config.sh"
# Auto-runs before compile phase
```

Both achieve the same result using their respective build system's mechanisms.

## Benefits of Auto-Generation

- ✅ **No manual steps** - Developers don't need to remember to run scripts
- ✅ **Always up-to-date** - Config regenerated if `local.properties` changes
- ✅ **CI/CD friendly** - Works automatically in pipelines
- ✅ **Platform parity** - Identical developer experience across Android/iOS
- ✅ **Error prevention** - Catches missing configuration before linking
- ✅ **Security** - Config files never committed, always generated fresh

## Next Steps

After completing this setup:

1. ✅ Delete manually generated `GoogleService-Info.plist` (it will auto-regenerate)
2. ✅ Build the project to verify auto-generation works
3. ✅ Commit the Xcode project changes (`.pbxproj` file)
4. ✅ Update team documentation to remove manual generation steps

Your iOS build process now matches Android's automated Firebase configuration! 🎉
