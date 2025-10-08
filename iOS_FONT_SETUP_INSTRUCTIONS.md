# iOS Font Setup Instructions

## Current Status
✅ Fonts downloaded and placed in correct location
✅ Android using Google Fonts successfully
❌ iOS fonts not bundled in app (manual Xcode step required)

## Problem
The font TTF files exist in `iosApp/worldwidewaves/Fonts/` but are not being bundled into the iOS app because they haven't been added to the Xcode project with target membership.

## Solution: Add Fonts to Xcode Project (One-Time Manual Step)

### Step 1: Open Project in Xcode
```bash
cd iosApp
open worldwidewaves.xcodeproj
```

### Step 2: Add Fonts to Project
1. In Xcode's Project Navigator (left sidebar), locate the `worldwidewaves` folder
2. Right-click on `worldwidewaves` folder → `Add Files to "worldwidewaves"...`
3. Navigate to `iosApp/worldwidewaves/Fonts/`
4. Select the `Fonts` folder
5. **IMPORTANT**: In the dialog that appears:
   - ✅ **Check** "Copy items if needed" (ensure fonts are in bundle)
   - ✅ **Check** "Create folder references" (NOT "Create groups")
   - ✅ **Check** "worldwidewaves" target under "Add to targets"
6. Click "Add"

### Step 3: Verify Target Membership
1. Click on any .ttf file in the `Fonts` folder in Xcode
2. Open the File Inspector (right sidebar, first tab)
3. Under "Target Membership", ensure `worldwidewaves` is ✅ checked

### Step 4: Clean and Rebuild
```bash
# In Xcode, press Cmd+Shift+K (Clean Build Folder)
# Then press Cmd+R (Build and Run)
```

OR from terminal:
```bash
cd iosApp
xcodebuild clean -project worldwidewaves.xcodeproj -scheme worldwidewaves
xcodebuild -project worldwidewaves.xcodeproj -scheme worldwidewaves -destination 'platform=iOS Simulator,name=iPhone 16'
```

## Expected Result
After adding fonts, the app should:
- ✅ Load Montserrat fonts from bundle
- ✅ Display text matching Android typography exactly
- ✅ No more "file doesn't exist" errors in console

## Files Already Configured

### ✅ `iosApp/worldwidewaves/Fonts/` (9 files)
- Montserrat-Regular.ttf
- Montserrat-Medium.ttf
- Montserrat-Bold.ttf
- MontserratAlternates-Regular.ttf
- MontserratAlternates-Medium.ttf
- MontserratAlternates-Bold.ttf
- NotoSans-Regular.ttf
- NotoSans-Medium.ttf
- NotoSans-Bold.ttf

### ✅ `iosApp/worldwidewaves/Info.plist`
UIAppFonts key properly configured with all 9 fonts

### ✅ `shared/src/iosMain/kotlin/.../Typography.ios.kt`
Font loading code ready to load from bundle

## Troubleshooting

### If fonts still don't load:
1. Check Build Phases → Copy Bundle Resources contains the .ttf files
2. Run the app and check the app bundle:
   ```bash
   # Find the app bundle
   find ~/Library/Developer/Xcode/DerivedData -name "worldwidewaves.app" -type d

   # Check if fonts are inside
   ls -la [path-to-app]/Fonts/
   ```

### If you see "Font not found in bundle" error:
- The fonts weren't properly added to Xcode project
- Repeat Step 2 above, ensuring target membership is checked

## Alternative: Xcodeproj Ruby Gem Method
If you have the `xcodeproj` gem installed:
```bash
gem install xcodeproj
ruby add_fonts_to_xcode.rb
```

## Next Steps After Manual Addition
Once fonts are added to Xcode:
1. iOS and Android will use identical Google Fonts ✅
2. Typography will match perfectly across platforms ✅
3. Future font updates only need file replacement, not re-adding ✅
