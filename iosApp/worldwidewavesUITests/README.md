# iOS UI Tests - Setup Instructions

This directory contains XCUITest files for Firebase Test Lab E2E testing on iOS.

## Files Created

1. **CompleteWaveParticipationUITest.swift** - Main E2E test with 21 steps
2. **XCUITestExtensions.swift** - Helper extensions for XCUIElement and XCTestCase
3. **ScreenshotHelper.swift** - Screenshot capture utilities with device info
4. **Info.plist** - UI test target configuration

## Adding UI Test Target to Xcode

### Option 1: Manual Setup (Recommended)

1. **Open Xcode project**:
   ```bash
   cd iosApp
   open worldwidewaves.xcodeproj
   ```

2. **Create UI Test Target**:
   - Click on the project in Project Navigator
   - Click the '+' button at the bottom of the targets list
   - Select "UI Testing Bundle"
   - Name it: `worldwidewavesUITests`
   - Language: Swift
   - Click Finish

3. **Add Files to Target**:
   - Delete the default test file Xcode created
   - Drag the following files from `worldwidewavesUITests/` folder into the UI test target:
     - `CompleteWaveParticipationUITest.swift`
     - `XCUITestExtensions.swift`
     - `ScreenshotHelper.swift`
   - Replace `Info.plist` with the one in this directory

4. **Configure Target Settings**:
   - Select `worldwidewavesUITests` target
   - General tab:
     - Bundle Identifier: `com.worldwidewaves.worldwidewavesUITests`
     - Deployment Target: iOS 15.0 or higher
   - Build Settings tab:
     - Search for "Info.plist File"
     - Set to: `worldwidewavesUITests/Info.plist`

5. **Add Test Target to Scheme**:
   - Product > Scheme > Edit Scheme
   - Select "Test" action
   - Click '+' to add test
   - Select `worldwidewavesUITests`
   - Click Close

### Option 2: Using xcodebuild (Command Line)

```bash
# Build for testing
xcodebuild build-for-testing \
  -project worldwidewaves.xcodeproj \
  -scheme worldwidewaves \
  -destination 'platform=iOS Simulator,name=iPhone 15 Pro' \
  -derivedDataPath ./build

# Run tests
xcodebuild test \
  -project worldwidewaves.xcodeproj \
  -scheme worldwidewaves \
  -destination 'platform=iOS Simulator,name=iPhone 15 Pro' \
  -only-testing:worldwidewavesUITests/CompleteWaveParticipationUITest
```

## Running Tests

### From Xcode

1. Select `worldwidewaves` scheme
2. Select simulator: iPhone 15 Pro or iPhone 16
3. Press `Cmd+U` to run all tests
4. Or right-click on test method > Run "testCompleteWaveParticipationJourney()"

### From Command Line

```bash
# Run specific test
xcodebuild test \
  -project worldwidewaves.xcodeproj \
  -scheme worldwidewaves \
  -destination 'platform=iOS Simulator,name=iPhone 15 Pro' \
  -only-testing:worldwidewavesUITests/CompleteWaveParticipationUITest/testCompleteWaveParticipationJourney
```

## Screenshots

Screenshots are automatically captured at each step and attached to test results:

- **Location**: Xcode > Test Navigator > Test Result > Attachments
- **Naming**: `{step}_{description}_{device}_iOS{version}.png`
- **Example**: `01_app_launch_simulation_enabled_iPhone_15_Pro_iOS18_0.png`

## Firebase Test Lab Integration

The tests are designed to work with Firebase Test Lab. See `TODO_FIREBASE_UI.md` Phase 5 for integration scripts.

## Test Requirements

- Debug build with simulation mode enabled (from Phase 1)
- Paris France event in test data
- paris_france map downloaded
- Event in "running" state

## testTag Accessibility

The tests use `testTag()` modifiers from Phase 1:

- Filters: `FilterButton_All`, `FilterButton_Favorites`, `FilterButton_Downloaded`
- Events: `EventsList`, `Event_{eventId}`, `EventFavoriteButton_{eventId}`
- Wave: `JoinWaveButton`
- About: `AboutTab_Info`, `AboutTab_FAQ`, `FaqList`, `FaqItem_{index}`

XCUITest accesses these via `.otherElements["{testTag}"]`.

## Troubleshooting

### Issue: Tests don't compile
**Solution**: Ensure UI test target is created and files are added to the target

### Issue: Can't find elements
**Solution**: Verify testTags are set in Compose UI (Phase 1 changes)

### Issue: Screenshots not appearing
**Solution**: Check Test Result attachments in Xcode Test Navigator

### Issue: App doesn't launch
**Solution**: Ensure `--uitesting` and `--simulation-enabled` launch arguments are handled in app

### Issue: "On-Demand Resources is not supported for ui testing bundle targets"
**Solution**: This is a standard Xcode warning and can be safely ignored.

**Explanation:**
- WorldWideWaves uses On-Demand Resources (ODR) for map downloads
- ODR works in the main app but is not available in UI test bundles
- This is a known Xcode limitation, not an error
- Tests can still run, they just can't trigger new ODR downloads
- Workaround: Ensure test maps (paris_france) are tagged as initial install tags

**To suppress warning (optional):**
1. Select worldwidewavesUITests target
2. Build Settings → Search "On-Demand Resources"
3. Set `ENABLE_ON_DEMAND_RESOURCES` to `NO` for UI test target only
4. Main app keeps ODR enabled

## Next Steps

After setup, proceed to:
- Phase 4: Run on simulators and fix failures
- Phase 5: Firebase Test Lab integration
- Phase 6: Screenshot collection and reporting
