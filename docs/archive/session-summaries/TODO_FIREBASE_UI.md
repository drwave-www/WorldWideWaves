# WorldWideWaves - Firebase Test Lab UI Testing Implementation

> **PROMPT FOR NEXT CLAUDE SESSION**
>
> Use this document as your implementation guide. Follow the phases sequentially, use agents for parallel work, verify compilation and tests after each step, commit frequently.

---

## 🚀 Quick Start Prompt

```
Implement comprehensive Firebase Test Lab E2E UI tests for WorldWideWaves Android and iOS.

Follow TODO_FIREBASE_UI.md:
- Phase 1: Preparation (8-10h) - Add testTags, debug config, Firebase setup
- Phase 2: Android Test (16-20h) - Implement 21-step E2E test with helpers
- Phase 3: iOS Test (20-24h) - XCUITest implementation
- Phase 4-6: Simulator testing, Firebase integration, reporting

Test Scenario: Complete wave participation journey (21 steps)
- App launch → Events browse → Favorites → Map download
- Event details → Wave participation → Choreography
- About navigation → FAQ interaction

Use agents for parallel implementation.
Create branch: feature/firebase-ui-tests
Verify after each phase.
Estimated: 60-80 hours total.

All details, code references, and step-by-step instructions below.
```

---

## 📋 Implementation Overview

**Purpose**: Comprehensive end-to-end UI testing for Android and iOS on Firebase Test Lab
**Scope**: Complete user journey from app launch to wave participation
**Target**: Automated testing on real devices with screenshots
**Estimated Total Effort**: 60-80 hours

---

## 📋 Test Scenario: Complete Wave Participation Journey

### User Story
As a user, I want to discover events, mark favorites, download maps, join a wave, participate, and explore the app, all working correctly on both Android and iOS.

### Test Flow Overview
1. **App Launch** (Debug mode with simulation)
2. **Event Discovery** (Browse and scroll)
3. **Favorites Management** (Filter, add, verify)
4. **Map Download** (Check downloaded maps status)
5. **Event Details** (View running event, verify in-area)
6. **Map Interaction** (View map, interact)
7. **Wave Participation** (Join wave, wait for choreography)
8. **About Section** (Navigate tabs, expand FAQ)

---

## 🎯 Detailed Test Steps with Validations

### Step 1: App Launch in Debug Mode
**Action**:
- Launch app with debug configuration
- Enable simulation mode via debug settings

**Validations**:
- ✅ App launches without crash
- ✅ Splash screen appears
- ✅ Main screen loads within 3 seconds
- ✅ Simulation indicator visible (debug mode)
- ✅ Bottom navigation bar present with tabs: Events, About

**Screenshot**: `01_app_launch_simulation_enabled.png`

**Code References**:
- `composeApp/src/androidMain/kotlin/com/worldwidewaves/activities/MainActivity.kt:45` - MainActivity
- `shared/src/commonMain/kotlin/com/worldwidewaves/shared/ui/activities/MainScreen.kt:67` - Main screen content
- `shared/src/commonMain/kotlin/com/worldwidewaves/shared/WWWPlatform.kt:38` - Simulation state

---

### Step 2: Browse Events List
**Action**:
- View events list on Events tab
- Scroll through entire list (top to bottom)
- Verify at least 3 events visible

**Validations**:
- ✅ Events list loads successfully
- ✅ At least 3 events displayed
- ✅ Each event card shows:
  - Event title/name
  - City/location
  - Date and time
  - Favorite icon (unfilled initially)
  - Download status indicator
- ✅ List is scrollable
- ✅ No loading errors displayed

**Screenshot**: `02_events_list_initial_state.png`

**Code References**:
- `shared/src/commonMain/kotlin/com/worldwidewaves/shared/ui/screens/EventsScreen.kt:111` - EventsScreen composable
- `shared/src/commonMain/kotlin/com/worldwidewaves/shared/ui/screens/EventsScreen.kt:340` - Event() composable (event card rendering)
- `shared/src/commonMain/kotlin/com/worldwidewaves/shared/viewmodels/EventsViewModel.kt:88` - Events ViewModel

---

### Step 3: Filter Favorites (Empty State)
**Action**:
- Click on "Favorites" filter button/tab
- Observe empty state

**Validations**:
- ✅ Filter button responds to click
- ✅ List updates to show only favorites
- ✅ Empty state message displayed: "No favorite events" or similar
- ✅ Empty state icon/illustration shown
- ✅ UI remains responsive
- ✅ ViewModel state: `onlyFavorites = true`, `events.size = 0`

**Screenshot**: `03_favorites_empty_state.png`

**Code References**:
- `shared/src/commonMain/kotlin/com/worldwidewaves/shared/ui/screens/EventsScreen.kt:145` - Filter UI
- `shared/src/commonMain/kotlin/com/worldwidewaves/shared/viewmodels/EventsViewModel.kt:156` - Filter logic
- `shared/src/commonMain/kotlin/com/worldwidewaves/shared/domain/usecases/FilterEventsUseCase.kt:35` - Filter use case

---

### Step 4: Return to All Events
**Action**:
- Click "All Events" filter to return to full list
- Verify events reappear

**Validations**:
- ✅ Filter button responds
- ✅ Full events list displayed again
- ✅ Same events as Step 2 visible
- ✅ No duplicates
- ✅ Scroll position resets or maintains (verify behavior)
- ✅ ViewModel state: `onlyFavorites = false`, `events.size > 0`

**Screenshot**: `04_return_to_all_events.png`

**Code References**:
- Same as Step 3

---

### Step 5: Add Event to Favorites
**Action**:
- Locate second event in list
- Click favorite icon on second event
- Observe visual feedback

**Validations**:
- ✅ Favorite icon changes appearance (unfilled → filled)
- ✅ Visual feedback (animation, color change)
- ✅ Event remains in list (position unchanged)
- ✅ Favorite persists (check ViewModel state)
- ✅ No UI freeze or delay
- ✅ Other events unchanged

**Screenshot**:
- `05a_before_favorite_click.png`
- `05b_after_favorite_click.png`

**Code References**:
- `shared/src/commonMain/kotlin/com/worldwidewaves/shared/ui/screens/EventsScreen.kt:492` - EventOverlayFavorite() composable (favorite button)
- `shared/src/commonMain/kotlin/com/worldwidewaves/shared/viewmodels/EventsViewModel.kt:188` - Toggle favorite
- `shared/src/commonMain/kotlin/com/worldwidewaves/shared/data/FavoriteEventsStore.kt:45` - Persistence

---

### Step 6: Verify Event in Favorites
**Action**:
- Click "Favorites" filter again
- Verify second event appears

**Validations**:
- ✅ Exactly 1 event shown
- ✅ Event is the second event from original list
- ✅ Event details correct (title, date, location)
- ✅ Favorite icon filled
- ✅ Download status visible
- ✅ Event is clickable

**Screenshot**: `06_favorites_with_one_event.png`

**Code References**:
- `shared/src/commonMain/kotlin/com/worldwidewaves/shared/domain/usecases/FilterEventsUseCase.kt:38` - Favorite filtering
- `shared/src/commonMain/kotlin/com/worldwidewaves/shared/domain/usecases/CheckEventFavoritesUseCase.kt:28` - Check favorites

---

### Step 7: Check Downloaded Maps Tab
**Action**:
- Click "Downloaded" filter/tab
- Verify "paris_france" map visible and marked "running"

**Validations**:
- ✅ Downloaded filter responds
- ✅ List shows events with downloaded maps
- ✅ "paris_france" event visible
- ✅ Status indicator shows "running" (event in progress)
- ✅ Event card shows map icon or download badge
- ✅ Event details correct for Paris event

**Screenshot**: `07_downloaded_maps_paris_running.png`

**Code References**:
- `shared/src/commonMain/kotlin/com/worldwidewaves/shared/viewmodels/EventsViewModel.kt:166` - Downloaded filter
- `shared/src/commonMain/kotlin/com/worldwidewaves/shared/domain/usecases/FilterEventsUseCase.kt:52` - Downloaded filtering
- `shared/src/commonMain/kotlin/com/worldwidewaves/shared/data/MapStore.kt:67` - Map download state

---

### Step 8: Open Event Details
**Action**:
- Click on the Paris France event (second event)
- Event detail screen opens

**Validations**:
- ✅ Screen transitions smoothly
- ✅ Event detail screen displays:
  - Event title and location
  - Date and time
  - Wave status: "Running"
  - User status: "In Area" (simulation)
  - Wave progression bar (0-100%)
  - Map preview
  - Join Wave button (active/enabled)
- ✅ Progression updates in real-time (animation)
- ✅ No loading errors

**Screenshot**: `08_event_detail_running_in_area.png`

**Code References**:
- `shared/src/commonMain/kotlin/com/worldwidewaves/shared/ui/activities/EventDetailScreen.kt:78` - Event detail screen
- `shared/src/commonMain/kotlin/com/worldwidewaves/shared/events/WWWEventObserver.kt:245` - In-area detection
- `shared/src/commonMain/kotlin/com/worldwidewaves/shared/domain/state/DefaultEventStateManager.kt:89` - Event state

---

### Step 9: Verify Map Loaded
**Action**:
- Observe map component on event detail screen
- Verify map tiles render correctly

**Validations**:
- ✅ Map renders without errors
- ✅ Map shows Paris city area
- ✅ User position marker visible (blue dot)
- ✅ Wave area polygon visible (colored overlay)
- ✅ Map is interactive (can pan/zoom)
- ✅ Camera centered on relevant area
- ✅ Map tiles load (no blank areas)

**Screenshot**: `09_event_map_loaded_paris.png`

**Code References**:
- `shared/src/commonMain/kotlin/com/worldwidewaves/shared/map/AbstractEventMap.kt:156` - Map setup
- `shared/src/androidMain/kotlin/com/worldwidewaves/compose/map/AndroidEventMap.kt:67` - Android map
- `shared/src/iosMain/kotlin/com/worldwidewaves/shared/map/IosEventMap.kt:45` - iOS map

---

### Step 10: Interact with Map
**Action**:
- Click/tap on map area
- Expand map to full screen (if button exists)

**Validations**:
- ✅ Map responds to touch
- ✅ Pan gesture works (drag map)
- ✅ Zoom gestures work (pinch or double-tap)
- ✅ User marker remains visible
- ✅ Wave polygon remains visible
- ✅ Camera constraints active (can't pan outside event area)
- ✅ Full map button visible and functional

**Screenshot**:
- `10a_map_interaction_pan.png`
- `10b_map_fullscreen.png`

**Code References**:
- `shared/src/commonMain/kotlin/com/worldwidewaves/shared/map/MapBoundsEnforcer.kt:78` - Camera constraints
- `shared/src/commonMain/kotlin/com/worldwidewaves/shared/ui/activities/FullMapScreen.kt:56` - Full map screen

---

### Step 11: Join Wave (Click Wave Button)
**Action**:
- Return to event detail (if in full map)
- Click "Join Wave" / "Wave" button
- Navigate to wave participation screen

**Validations**:
- ✅ Button is active/enabled (event is running, user in area)
- ✅ Button responds to click
- ✅ Screen transition to wave participation
- ✅ Wave screen loads within 2 seconds
- ✅ No navigation errors

**Screenshot**: `11_click_join_wave_button.png`

**Code References**:
- `shared/src/commonMain/kotlin/com/worldwidewaves/shared/ui/components/shared/ButtonWave.kt:45` - Wave button
- `shared/src/commonMain/kotlin/com/worldwidewaves/shared/ui/activities/WaveParticipationScreen.kt:89` - Wave screen

---

### Step 12: Wave Participation Screen
**Action**:
- Observe wave participation screen
- Verify all components display correctly

**Validations**:
- ✅ Screen title: Event name
- ✅ Wave status indicator visible
- ✅ Countdown timer/wave progression displayed
- ✅ User in-area indicator shows "YES" or green
- ✅ Wave progression bar (0-100%)
- ✅ Sound/choreography controls visible
- ✅ Map mini-view showing user position
- ✅ Real-time updates (progression changes)

**Screenshot**: `12_wave_participation_screen.png`

**Code References**:
- `shared/src/commonMain/kotlin/com/worldwidewaves/shared/ui/activities/WaveParticipationScreen.kt:134` - Wave UI
- `shared/src/commonMain/kotlin/com/worldwidewaves/shared/utils/WaveProgressionObserver.kt:89` - Progression tracking

---

### Step 13: Wait for Choreography
**Action**:
- Wait for wave choreography to trigger
- Observe sound/visual feedback
- Verify choreography plays

**Validations**:
- ✅ Choreography triggers at correct wave timing
- ✅ Visual animations display (warming → hit → done states)
- ✅ Sound plays (if device not muted)
- ✅ Haptic feedback (vibration) on wave hit
- ✅ Progression bar updates smoothly
- ✅ State transitions: Observer → Warming → Waiting → Hit → Done
- ✅ Choreography completes without crashes
- ✅ User receives "Wave Hit" confirmation

**Screenshots**:
- `13a_wave_warming_state.png`
- `13b_wave_waiting_state.png`
- `13c_wave_hit_moment.png`
- `13d_wave_done_state.png`

**Code References**:
- `shared/src/commonMain/kotlin/com/worldwidewaves/shared/ui/components/wave/choreographies/WaveChoreographies.kt:67` - Choreography UI
- `shared/src/commonMain/kotlin/com/worldwidewaves/shared/choreographies/SoundChoreographyManager.kt:88` - Choreography manager
- `shared/src/commonMain/kotlin/com/worldwidewaves/shared/sound/SoundChoreographyCoordinator.kt:111` - Sound coordination
- `shared/src/commonMain/kotlin/com/worldwidewaves/shared/events/WWWEventObserver.kt:389` - Hit detection

---

### Step 14: Navigate to About Tab
**Action**:
- Click "About" tab in bottom navigation
- First tab loads (About Info)

**Validations**:
- ✅ Navigation to About tab successful
- ✅ About screen loads within 1 second
- ✅ First tab (Info) is selected by default
- ✅ App name and version displayed
- ✅ Description/intro text visible
- ✅ Logo or branding visible
- ✅ Social media links present

**Screenshot**: `14_about_tab_info.png`

**Code References**:
- `shared/src/commonMain/kotlin/com/worldwidewaves/shared/ui/screens/AboutScreen.kt:45` - About screen
- `shared/src/commonMain/kotlin/com/worldwidewaves/shared/ui/screens/about/AboutInfoScreen.kt:60` - Info tab

---

### Step 15: Scroll About Info Tab
**Action**:
- Scroll down the Info tab to bottom
- Verify all content visible

**Validations**:
- ✅ Scroll works smoothly
- ✅ All content sections visible:
  - App description
  - How it works
  - Community guidelines
  - Privacy information
  - Attribution/credits
- ✅ Links are clickable
- ✅ No text truncation
- ✅ Images load correctly

**Screenshot**:
- `15a_about_info_top.png`
- `15b_about_info_bottom.png`

**Code References**:
- `shared/src/commonMain/kotlin/com/worldwidewaves/shared/ui/screens/about/AboutInfoScreen.kt:100` - Info content

---

### Step 16: Navigate to FAQ Tab
**Action**:
- Click second tab (FAQ)
- FAQ list loads

**Validations**:
- ✅ Tab switches successfully
- ✅ FAQ list displays
- ✅ At least 5 FAQ items visible
- ✅ Each FAQ item shows:
  - Question text
  - Expand/collapse icon
  - Clean formatting
- ✅ All items collapsed initially

**Screenshot**: `16_about_faq_collapsed.png`

**Code References**:
- `shared/src/commonMain/kotlin/com/worldwidewaves/shared/ui/screens/about/AboutFaqScreen.kt:90` - FAQ screen

---

### Step 17: Scroll FAQ Tab
**Action**:
- Scroll through entire FAQ list
- Verify all questions visible

**Validations**:
- ✅ Scroll works smoothly
- ✅ All FAQ items visible
- ✅ No items cut off
- ✅ List is complete (last item fully visible)

**Screenshot**: `17_faq_list_scrolled.png`

**Code References**:
- Same as Step 16

---

### Step 18: Expand FAQ Item
**Action**:
- Click first FAQ item to expand
- Read answer content
- Click again to collapse

**Validations**:
- ✅ FAQ item expands with animation
- ✅ Answer text displays completely
- ✅ Expand icon rotates/changes (▼ → ▲)
- ✅ Other items remain collapsed
- ✅ Content is readable and formatted
- ✅ Item collapses when clicked again
- ✅ Animation smooth

**Screenshots**:
- `18a_faq_item_expanded.png`
- `18b_faq_item_collapsed.png`

**Code References**:
- `shared/src/commonMain/kotlin/com/worldwidewaves/shared/ui/screens/about/AboutFaqScreen.kt:120` - FAQ expansion logic

---

### Additional Validation Steps

#### Step 19: Verify Simulation Mode Throughout
**Action**:
- Check simulation indicator present on all screens
- Verify simulated time/position working

**Validations**:
- ✅ Simulation button/indicator visible on relevant screens
- ✅ Time advances in simulation
- ✅ User position can be simulated
- ✅ Wave progression uses simulated time

**Screenshot**: `19_simulation_mode_active.png`

**Code References**:
- `shared/src/commonMain/kotlin/com/worldwidewaves/shared/ui/components/shared/SimulationButton.kt:45` - Simulation UI
- `shared/src/commonMain/kotlin/com/worldwidewaves/shared/WWWSimulation.kt:67` - Simulation logic

---

#### Step 20: Back Navigation Flow
**Action**:
- From About screen, press back button
- Should return to Events tab
- Press back again
- Should exit app or show exit confirmation

**Validations**:
- ✅ Back navigation works correctly
- ✅ Navigation stack maintained
- ✅ No unexpected screen transitions
- ✅ State preserved during navigation

**Screenshot**: `20_back_navigation.png`

**Code References**:
- Platform-specific back handling

---

#### Step 21: Configuration Changes (Removed - Portrait Only)
**Note**: App is locked to portrait orientation on both Android and iOS.
Rotation testing is not applicable.

**Configuration**:
- Android: `android:screenOrientation="portrait"` in AndroidManifest.xml
- iOS: `UISupportedInterfaceOrientations` limited to portrait in Info.plist
- iPad: Portrait and portrait upside-down only

---

#### Step 22: Memory and Performance
**Action**:
- Monitor memory usage throughout test
- Verify no memory leaks

**Validations**:
- ✅ Memory usage stays below 200MB
- ✅ No memory leaks after navigation cycles
- ✅ UI remains responsive (60 FPS)
- ✅ No ANR (Application Not Responding)
- ✅ Battery drain acceptable

**Metrics to Capture**:
- Peak memory usage
- Average FPS
- Battery consumption
- Network data usage

**Code References**:
- `shared/src/commonMain/kotlin/com/worldwidewaves/shared/testing/PerformanceMonitor.kt:47` - Performance monitoring

---

## 🏗️ Implementation Plan

### Phase 1: Preparation (8-10 hours)

#### 1.1 Setup Test Infrastructure
**Tasks**:
- [ ] Create `iosApp/iosAppUITests/` directory for iOS tests
- [ ] Add XCUITest target in Xcode project
- [ ] Configure Firebase Test Lab project
- [ ] Setup test devices matrix
- [ ] Configure screenshot capture utilities

**Deliverables**:
- XCUITest target configured
- Firebase project linked
- Test device matrix defined

**Effort**: 3-4 hours

---

#### 1.2 Add Accessibility Identifiers
**Tasks**:
- [ ] Add `testTag()` to all critical UI components in Compose
- [ ] Document accessibility IDs for both platforms
- [ ] Verify IDs work on both Android and iOS

**Critical Components**:
```kotlin
// EventsScreen.kt - Event() composable (event card is inline)
Modifier.testTag("Event_$eventId")
Modifier.testTag("EventFavoriteButton_$eventId")
Modifier.testTag("EventsList")
Modifier.testTag("FilterButton_All")
Modifier.testTag("FilterButton_Favorites")
Modifier.testTag("FilterButton_Downloaded")

// ButtonWave.kt
Modifier.testTag("JoinWaveButton")

// AboutScreen.kt
Modifier.testTag("AboutTab_Info")
Modifier.testTag("AboutTab_FAQ")
```

**Files to Modify**:
- `shared/src/commonMain/kotlin/com/worldwidewaves/shared/ui/screens/EventsScreen.kt` (Event() composable - event card is inline)
- `shared/src/commonMain/kotlin/com/worldwidewaves/shared/ui/components/shared/ButtonWave.kt`
- `shared/src/commonMain/kotlin/com/worldwidewaves/shared/ui/screens/AboutScreen.kt`
- `shared/src/commonMain/kotlin/com/worldwidewaves/shared/ui/screens/about/AboutFaqScreen.kt`
- `shared/src/commonMain/kotlin/com/worldwidewaves/shared/ui/screens/about/AboutInfoScreen.kt`

**Effort**: 3-4 hours

---

#### 1.3 Setup Debug Configuration
**Tasks**:
- [ ] Create debug build variant with simulation enabled by default
- [ ] Add debug menu for enabling simulation mode
- [ ] Configure test data (ensure Paris France event exists)
- [ ] Setup mock Firebase backend (or use test project)

**Files to Modify**:
- `composeApp/build.gradle.kts` - Debug build config
- `shared/src/commonMain/kotlin/com/worldwidewaves/shared/di/CommonModule.kt` - Test data injection

**Effort**: 2 hours

---

### Phase 2: Android Instrumented Test (16-20 hours)

#### 2.1 Create Base Test Class
**File**: `composeApp/src/androidInstrumentedTest/kotlin/com/worldwidewaves/e2e/BaseE2ETest.kt`

**Content**:
```kotlin
@RunWith(AndroidJUnit4::class)
abstract class BaseE2ETest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val screenshotRule = ScreenshotTestRule()

    @Before
    fun setup() {
        // Enable simulation mode
        // Clear app data
        // Setup test preconditions
    }

    @After
    fun teardown() {
        // Capture final screenshot
        // Clear test data
    }

    protected fun captureScreenshot(name: String) {
        screenshotRule.capture(name)
    }

    protected fun waitForEventsList() {
        composeTestRule.waitUntil(5000) {
            composeTestRule.onNodeWithTag("EventsList").isDisplayed()
        }
    }
}
```

**Effort**: 2-3 hours

---

#### 2.2 Implement Test Steps (Android)
**File**: `composeApp/src/androidInstrumentedTest/kotlin/com/worldwidewaves/e2e/CompleteWaveParticipationE2ETest.kt`

**Structure**:
```kotlin
@Test
fun testCompleteWaveParticipationJourney() {
    // Step 1: App Launch
    captureScreenshot("01_app_launch_simulation_enabled")
    verifyMainScreenLoaded()

    // Step 2: Browse Events
    verifyEventsListLoaded()
    scrollEventsList()
    captureScreenshot("02_events_list_initial_state")

    // Step 3: Filter Favorites (Empty)
    clickFavoritesFilter()
    verifyEmptyFavorites()
    captureScreenshot("03_favorites_empty_state")

    // Step 4: Return to All Events
    clickAllEventsFilter()
    verifyEventsListLoaded()
    captureScreenshot("04_return_to_all_events")

    // Step 5: Add to Favorites
    captureScreenshot("05a_before_favorite_click")
    clickFavoriteOnSecondEvent()
    verifyFavoriteIconFilled()
    captureScreenshot("05b_after_favorite_click")

    // Step 6: Verify in Favorites
    clickFavoritesFilter()
    verifyOneEventInFavorites()
    captureScreenshot("06_favorites_with_one_event")

    // Step 7: Check Downloaded
    clickDownloadedFilter()
    verifyParisEventVisible()
    verifyEventStatusRunning()
    captureScreenshot("07_downloaded_maps_paris_running")

    // Step 8: Open Event Details
    clickOnParisEvent()
    verifyEventDetailScreen()
    verifyUserInArea()
    verifyWaveProgression()
    captureScreenshot("08_event_detail_running_in_area")

    // Step 9: Verify Map
    verifyMapLoaded()
    verifyUserMarker()
    verifyWavePolygon()
    captureScreenshot("09_event_map_loaded_paris")

    // Step 10: Interact with Map
    panMap()
    captureScreenshot("10a_map_interaction_pan")
    openFullMap()
    captureScreenshot("10b_map_fullscreen")
    navigateBack()

    // Step 11: Join Wave
    clickJoinWaveButton()
    verifyWaveScreen()
    captureScreenshot("11_wave_participation_screen")

    // Step 12: Wave Participation
    verifyWaveComponentsVisible()
    captureScreenshot("12_wave_participation_active")

    // Step 13: Wait for Choreography
    waitForChoreography()
    captureScreenshot("13a_wave_warming_state")
    waitForWaveHit()
    captureScreenshot("13c_wave_hit_moment")
    verifyWaveHitConfirmation()
    captureScreenshot("13d_wave_done_state")

    // Step 14: Navigate to About
    navigateToAboutTab()
    verifyAboutInfoTab()
    captureScreenshot("14_about_tab_info")

    // Step 15: Scroll About Info
    scrollAboutInfo()
    captureScreenshot("15a_about_info_top")
    scrollToBottom()
    captureScreenshot("15b_about_info_bottom")

    // Step 16: FAQ Tab
    clickFaqTab()
    verifyFaqList()
    captureScreenshot("16_about_faq_collapsed")

    // Step 17: Scroll FAQ
    scrollFaqList()
    captureScreenshot("17_faq_list_scrolled")

    // Step 18: Expand FAQ
    captureScreenshot("18a_before_faq_expand")
    expandFirstFaq()
    verifyFaqExpanded()
    captureScreenshot("18b_faq_item_expanded")
    collapseFirstFaq()
    verifyFaqCollapsed()

    // Step 19: Verify Simulation Active
    verifySimulationIndicator()
    captureScreenshot("19_simulation_mode_active")

    // Step 20: Back Navigation
    pressBack()
    verifyOnEventsTab()
    captureScreenshot("20_back_navigation")

    // Step 21: Configuration Change (Android)
    rotateToLandscape()
    verifyStatePreserved()
    captureScreenshot("21a_landscape_mode")
    rotateToPortrait()
    captureScreenshot("21b_portrait_mode")
}
```

**Helper Methods** (50+ methods needed):
```kotlin
private fun verifyMainScreenLoaded() {
    composeTestRule.onNodeWithTag("MainScreen").assertIsDisplayed()
    composeTestRule.onNodeWithTag("EventsTab").assertIsDisplayed()
}

private fun clickFavoritesFilter() {
    composeTestRule.onNodeWithTag("FilterButton_Favorites").performClick()
}

private fun verifyEmptyFavorites() {
    composeTestRule.onNodeWithText("No favorite events").assertIsDisplayed()
}

// ... 47+ more helper methods
```

**Effort**: 12-14 hours

---

#### 2.3 Android Screenshot Utilities
**File**: `composeApp/src/androidInstrumentedTest/kotlin/com/worldwidewaves/testing/ScreenshotTestRule.kt`

**Implementation**:
```kotlin
class ScreenshotTestRule : TestWatcher() {
    private val screenshotDir = File(
        InstrumentationRegistry.getInstrumentation()
            .targetContext.externalCacheDir,
        "screenshots"
    )

    override fun starting(description: Description) {
        screenshotDir.mkdirs()
    }

    fun capture(name: String) {
        val deviceModel = Build.MODEL.replace(" ", "_")
        val deviceVersion = "API${Build.VERSION.SDK_INT}"
        val filename = "${name}_${deviceModel}_${deviceVersion}.png"

        InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
            ?.let { bitmap ->
                File(screenshotDir, filename).outputStream().use {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                }
            }
    }
}
```

**Effort**: 2 hours

---

### Phase 3: iOS UI Test (20-24 hours)

#### 3.1 Create XCUITest Files
**File**: `iosApp/iosAppUITests/CompleteWaveParticipationUITest.swift`

**Structure**:
```swift
import XCTest

class CompleteWaveParticipationUITest: XCTestCase {
    var app: XCUIApplication!

    override func setUpWithError() throws {
        continueAfterFailure = false
        app = XCUIApplication()
        app.launchArguments = ["--uitesting", "--simulation-enabled"]
        app.launch()
    }

    override func tearDownWithError() throws {
        // Cleanup
    }

    func testCompleteWaveParticipationJourney() throws {
        // Step 1: App Launch
        XCTAssertTrue(app.wait(for: .runningForeground, timeout: 5))
        takeScreenshot(name: "01_app_launch_simulation_enabled")

        // Step 2: Browse Events
        let eventsList = app.otherElements["EventsList"]
        XCTAssertTrue(eventsList.waitForExistence(timeout: 5))
        takeScreenshot(name: "02_events_list_initial_state")

        // Step 3: Filter Favorites
        app.buttons["FilterButton_Favorites"].tap()
        XCTAssertTrue(app.staticTexts["No favorite events"].exists)
        takeScreenshot(name: "03_favorites_empty_state")

        // ... Continue with all 21 steps
    }

    func takeScreenshot(name: String) {
        let screenshot = app.screenshot()
        let attachment = XCTAttachment(screenshot: screenshot)
        attachment.name = name
        attachment.lifetime = .keepAlways
        add(attachment)
    }
}
```

**Challenges**:
- Compose UI accessibility from XCUITest (relies on testTag semantics)
- Swift-Kotlin bridging for test data
- Timing issues with async operations

**Effort**: 16-18 hours

---

#### 3.2 iOS Screenshot Configuration
**File**: `iosApp/iosAppUITests/ScreenshotHelper.swift`

**Implementation**:
```swift
class ScreenshotHelper {
    static func capture(app: XCUIApplication, name: String, testCase: XCTestCase) {
        let screenshot = app.screenshot()
        let deviceName = UIDevice.current.name.replacingOccurrences(of: " ", with: "_")
        let osVersion = UIDevice.current.systemVersion.replacingOccurrences(of: ".", with: "_")
        let filename = "\(name)_\(deviceName)_iOS\(osVersion)"

        let attachment = XCTAttachment(screenshot: screenshot)
        attachment.name = filename
        attachment.lifetime = .keepAlways
        testCase.add(attachment)
    }
}
```

**Effort**: 2 hours

---

#### 3.3 iOS Test Helpers
**File**: `iosApp/iosAppUITests/UITestExtensions.swift`

**Content**:
```swift
extension XCUIElement {
    func waitForExistenceAndTap(timeout: TimeInterval = 5) -> Bool {
        guard self.waitForExistence(timeout: timeout) else {
            return false
        }
        self.tap()
        return true
    }

    func scrollToElement(_ element: XCUIElement) {
        while !element.isHittable {
            self.swipeUp()
        }
    }
}

extension XCTestCase {
    func verifyElementExists(_ element: XCUIElement, timeout: TimeInterval = 5) {
        XCTAssertTrue(
            element.waitForExistence(timeout: timeout),
            "Element does not exist: \(element.debugDescription)"
        )
    }
}
```

**Effort**: 2-3 hours

---

### Phase 4: Run on Simulators (4-6 hours)

#### 4.1 Android Emulator Testing
**Tasks**:
- [ ] Setup Android emulator (Pixel 6, API 33)
- [ ] Run test: `./gradlew :composeApp:connectedDebugAndroidTest`
- [ ] Collect screenshots from emulator
- [ ] Verify all steps pass
- [ ] Fix any failures

**Command**:
```bash
# Start emulator
emulator -avd Pixel_6_API_33 -no-snapshot-load

# Run tests
ANDROID_SERIAL=emulator-5556 ./gradlew :composeApp:connectedDebugAndroidTest

# Collect screenshots
adb pull /sdcard/Android/data/com.worldwidewaves/cache/screenshots/ ./test_results/android/
```

**Effort**: 2-3 hours

---

#### 4.2 iOS Simulator Testing
**Tasks**:
- [ ] Setup iOS simulator (iPhone 15 Pro, iOS 17)
- [ ] Run test from Xcode: `Cmd+U` or `xcodebuild test`
- [ ] Collect screenshots from test results
- [ ] Verify all steps pass
- [ ] Fix any failures

**Command**:
```bash
# Boot simulator
xcrun simctl boot "iPhone 15 Pro"

# Run tests
xcodebuild test \
  -project iosApp/worldwidewaves.xcodeproj \
  -scheme worldwidewaves \
  -destination 'platform=iOS Simulator,name=iPhone 15 Pro' \
  -resultBundlePath ./test_results/ios/

# Extract screenshots
xcparse screenshots ./test_results/ios/ ./test_results/ios/screenshots/
```

**Effort**: 2-3 hours

---

### Phase 5: Firebase Test Lab Integration (8-12 hours)

#### 5.1 Configure Firebase Test Lab
**Tasks**:
- [ ] Create Firebase project (or use existing)
- [ ] Enable Test Lab API
- [ ] Setup billing (required for Test Lab)
- [ ] Configure service account credentials
- [ ] Define device matrix

**Device Matrix**:
```yaml
# Android devices
- model: Pixel6, version: 33, locale: en_US, orientation: portrait
- model: Pixel5, version: 31, locale: en_US, orientation: portrait
- model: SamsungGalaxyS22, version: 33, locale: en_US, orientation: portrait

# iOS devices
- model: iphone14pro, version: 16.6, locale: en_US, orientation: portrait
- model: iphone13, version: 16.6, locale: en_US, orientation: portrait
- model: ipadpro12, version: 16.6, locale: en_US, orientation: portrait
```

**Effort**: 2-3 hours

---

#### 5.2 Android Firebase Test Lab
**File**: `scripts/run_android_firebase_tests.sh`

**Content**:
```bash
#!/usr/bin/env bash
set -e

# Build APKs
./gradlew :composeApp:assembleDebug
./gradlew :composeApp:assembleDebugAndroidTest

# Upload and run on Firebase Test Lab
gcloud firebase test android run \
  --type instrumentation \
  --app composeApp/build/outputs/apk/debug/composeApp-debug.apk \
  --test composeApp/build/outputs/apk/androidTest/debug/composeApp-debug-androidTest.apk \
  --device model=Pixel6,version=33,locale=en_US,orientation=portrait \
  --device model=Pixel5,version=31,locale=en_US,orientation=portrait \
  --device model=SamsungGalaxyS22,version=33,locale=en_US,orientation=portrait \
  --timeout 20m \
  --results-bucket=worldwidewaves-test-results \
  --results-dir=android/$(date +%Y%m%d_%H%M%S) \
  --environment-variables coverage=true,clearPackageData=true \
  --directories-to-pull /sdcard/Android/data/com.worldwidewaves/cache/screenshots

echo "✅ Android tests running on Firebase Test Lab"
echo "View results: https://console.firebase.google.com/project/YOUR_PROJECT/testlab/histories"
```

**Effort**: 2-3 hours

---

#### 5.3 iOS Firebase Test Lab
**File**: `scripts/run_ios_firebase_tests.sh`

**Content**:
```bash
#!/usr/bin/env bash
set -e

# Build iOS app and test bundle
cd iosApp
xcodebuild build-for-testing \
  -project worldwidewaves.xcodeproj \
  -scheme worldwidewaves \
  -sdk iphoneos \
  -derivedDataPath ./build

# Create test bundle zip
cd build/Build/Products
zip -r worldwidewaves_tests.zip Debug-iphoneos/worldwidewaves.app Debug-iphoneos/worldwidewavesUITests-Runner.app

# Upload and run on Firebase Test Lab
gcloud firebase test ios run \
  --test ./worldwidewaves_tests.zip \
  --device model=iphone14pro,version=16.6,locale=en_US,orientation=portrait \
  --device model=iphone13,version=16.6,locale=en_US,orientation=portrait \
  --timeout 20m \
  --results-bucket=worldwidewaves-test-results \
  --results-dir=ios/$(date +%Y%m%d_%H%M%S)

echo "✅ iOS tests running on Firebase Test Lab"
echo "View results: https://console.firebase.google.com/project/YOUR_PROJECT/testlab/histories"
```

**Effort**: 2-3 hours

---

#### 5.4 Combined Runner Script
**File**: `scripts/run_all_firebase_tests.sh`

**Content**:
```bash
#!/usr/bin/env bash
set -e

echo "🚀 Running Complete Firebase Test Lab Suite"
echo "==========================================="

# Authenticate with Firebase
gcloud auth login
gcloud config set project worldwidewaves-test

# Run Android tests
echo ""
echo "📱 Running Android Tests..."
./scripts/run_android_firebase_tests.sh

# Run iOS tests
echo ""
echo "🍎 Running iOS Tests..."
./scripts/run_ios_firebase_tests.sh

echo ""
echo "✅ All tests submitted to Firebase Test Lab"
echo "Monitor progress: https://console.firebase.google.com/project/worldwidewaves-test/testlab/histories"
echo ""
echo "Screenshots will be available in test results under:"
echo "  - Android: /sdcard/Android/data/com.worldwidewaves/cache/screenshots/"
echo "  - iOS: Attachments in XCTest results"
```

**Effort**: 1 hour

---

### Phase 6: Screenshot Analysis & Reporting (4-6 hours)

#### 6.1 Screenshot Collection Script
**File**: `scripts/collect_firebase_screenshots.sh`

**Content**:
```bash
#!/usr/bin/env bash

# Download latest test results from Firebase
LATEST_ANDROID=$(gsutil ls gs://worldwidewaves-test-results/android/ | tail -1)
LATEST_IOS=$(gsutil ls gs://worldwidewaves-test-results/ios/ | tail -1)

mkdir -p test_results/firebase/{android,ios}

# Download Android screenshots
gsutil -m cp -r "${LATEST_ANDROID}*/screenshots/" test_results/firebase/android/

# Download iOS screenshots
gsutil -m cp -r "${LATEST_IOS}*/xcresult/" test_results/firebase/ios/

echo "✅ Screenshots downloaded to test_results/firebase/"
```

**Effort**: 1 hour

---

#### 6.2 Test Report Generator
**File**: `scripts/generate_test_report.py`

**Purpose**: Generate HTML report with side-by-side screenshots

**Features**:
- Parse Firebase Test Lab results
- Extract all screenshots
- Create comparison table (Android vs iOS)
- Highlight failures
- Generate shareable HTML report

**Effort**: 3-4 hours

---

## 📦 Deliverables Summary

### Code Files
1. **Android**:
   - `BaseE2ETest.kt` - Base test class
   - `CompleteWaveParticipationE2ETest.kt` - Main test (800-1000 lines)
   - `ScreenshotTestRule.kt` - Screenshot utility
   - `E2ETestHelpers.kt` - 50+ helper methods

2. **iOS**:
   - `CompleteWaveParticipationUITest.swift` - Main test (800-1000 lines)
   - `ScreenshotHelper.swift` - Screenshot utility
   - `UITestExtensions.swift` - Helper extensions

3. **Shared Code Modifications**:
   - Add `testTag()` to 30+ UI components
   - Add debug mode configuration
   - Ensure simulation mode toggleable

4. **Scripts**:
   - `run_android_firebase_tests.sh`
   - `run_ios_firebase_tests.sh`
   - `run_all_firebase_tests.sh`
   - `collect_firebase_screenshots.sh`
   - `generate_test_report.py`

### Documentation
1. **Firebase Test Lab Setup Guide**
2. **Test Execution Instructions**
3. **Screenshot Analysis Guidelines**
4. **Troubleshooting Guide**

---

## ⏱️ Effort Breakdown

| Phase | Task | Android | iOS | Total |
|-------|------|---------|-----|-------|
| **Phase 1** | Preparation | 4h | 4h | **8h** |
| | Add testTags | 2h | 2h | 4h |
| | Debug config | 1h | 1h | 2h |
| | Firebase setup | 1h | 1h | 2h |
| **Phase 2** | Android Test | 16h | - | **16h** |
| | Base class | 2h | - | 2h |
| | Main test | 12h | - | 12h |
| | Screenshots | 2h | - | 2h |
| **Phase 3** | iOS Test | - | 20h | **20h** |
| | XCUITest setup | - | 4h | 4h |
| | Main test | - | 14h | 14h |
| | Screenshots | - | 2h | 2h |
| **Phase 4** | Simulator Tests | 2h | 3h | **5h** |
| **Phase 5** | Firebase Integration | 3h | 3h | **6h** |
| | Scripts | 2h | 2h | 4h |
| | Execution | 1h | 1h | 2h |
| **Phase 6** | Reporting | 3h | 2h | **5h** |
| **TOTAL** | | **28h** | **32h** | **60h** |

---

## 🎯 Success Criteria

### Functional
- [ ] Test runs end-to-end without intervention
- [ ] All 21 steps execute successfully on both platforms
- [ ] Screenshots captured for every step on every device
- [ ] Test completes in <20 minutes per device

### Quality
- [ ] Test covers 100% of critical user journey
- [ ] Assertions validate business logic, not just UI presence
- [ ] Screenshots are clear and properly labeled
- [ ] Test is maintainable (helper methods, clear structure)

### Automation
- [ ] One command runs both Android and iOS tests
- [ ] Results automatically uploaded to Firebase Test Lab
- [ ] Screenshots automatically collected
- [ ] HTML report automatically generated
- [ ] CI/CD integration ready

---

## 🚀 Quick Start Commands (After Implementation)

```bash
# Run on local simulators
./gradlew :composeApp:connectedDebugAndroidTest  # Android
xcodebuild test -scheme worldwidewaves -destination 'platform=iOS Simulator,name=iPhone 15 Pro'  # iOS

# Run on Firebase Test Lab
./scripts/run_all_firebase_tests.sh

# Collect results
./scripts/collect_firebase_screenshots.sh

# Generate report
python3 scripts/generate_test_report.py
```

---

## 📊 Expected Test Metrics

**Test Duration**:
- Android: 8-12 minutes per device
- iOS: 10-15 minutes per device
- Total (3 Android + 2 iOS devices): ~60 minutes

**Screenshot Count**:
- 21 steps × 5 devices = **105 screenshots** minimum
- With landscape/portrait: **130+ screenshots**

**Test Data**:
- Network requests: ~50
- GPS updates: ~100
- UI interactions: ~80
- State transitions: ~40

---

## 🛠️ Technologies Used

### Android
- **UI Testing**: Jetpack Compose Test, Espresso
- **Runner**: AndroidJUnit4
- **Screenshot**: UIAutomation.takeScreenshot()
- **Assertions**: JUnit + Compose semantics

### iOS
- **UI Testing**: XCUITest
- **Language**: Swift
- **Screenshot**: XCUIScreen.screenshot()
- **Assertions**: XCTest framework

### Firebase Test Lab
- **Platform**: Google Cloud Platform
- **CLI**: gcloud firebase test
- **Storage**: Cloud Storage for screenshots
- **Reporting**: Built-in Test Lab reports + custom HTML

### CI/CD Integration
- **GitHub Actions** workflow for automated runs
- **Trigger**: On PR to main, manual dispatch
- **Notifications**: Slack/email on failure
- **Artifacts**: Screenshots uploaded to PR comments

---

## 🔍 Advanced Features (Optional)

### Performance Monitoring
- Capture FPS during wave choreography
- Measure memory usage throughout test
- Track network bandwidth consumption
- Log battery drain

### Accessibility Testing
- Run with TalkBack (Android) / VoiceOver (iOS)
- Verify all elements have content descriptions
- Test with font scaling (150%, 200%)
- Verify color contrast (accessibility scanner)

### Internationalization
- Run tests in multiple locales (en, fr, es, pt, zh)
- Verify translations display correctly
- Check RTL layouts (Arabic, Hebrew)

### Network Conditions
- Test with slow 3G simulation
- Test with network interruption
- Test offline mode behavior
- Test airplane mode scenarios

---

## 📚 Dependencies to Add

### Android
```kotlin
// build.gradle.kts (composeApp)
androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.5.4")
androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
androidTestImplementation("androidx.test.ext:junit:1.1.5")
androidTestImplementation("androidx.test.uiautomator:uiautomator:2.2.0")
```

### iOS
```ruby
# Podfile
target 'iosAppUITests' do
  inherit! :search_paths
  # XCUITest is built-in, no pods needed
end
```

### Firebase
```bash
# Install gcloud CLI
brew install google-cloud-sdk

# Install Firebase tools
npm install -g firebase-tools

# Authenticate
gcloud auth login
firebase login
```

---

## ⚠️ Important Notes

### Simulation Mode Requirements
- Ensure simulation can be enabled via launch arguments
- Mock GPS position to Paris coordinates (48.8566, 2.3522)
- Mock time progression for wave timing
- Use test Firebase project (not production)

### Test Data Requirements
- Paris France event must exist in test database
- Event must be in "running" state during test
- Map tiles for Paris must be available
- At least 3 events total in test data

### Timing Considerations
- Wave choreography takes 30-60 seconds
- Add explicit waits (`waitUntil`, `waitForExistence`)
- Use timeouts generously (Firebase Test Lab can be slow)
- Consider test flakiness (retry logic)

### Platform Differences
- Android: Use `testTag()` for element identification
- iOS: XCUITest sees Compose elements as generic accessibility elements
- May need platform-specific accessibility labels
- Choreography timing may differ (platform audio latency)

---

## 🎓 Lessons from Existing Tests

### From AndroidInstrumentedTest Suite
**Good Patterns**:
- `BaseInstrumentedTest.kt` - Excellent base class structure
- `UITestAssertions.kt` - Reusable assertion helpers
- `UITestFactory.kt` - Test data creation utilities

**Use These**:
- `waitForEventToLoad(eventId: String, timeout: Long = 5000)`
- `verifyWaveStateTransition(from: State, to: State)`
- `assertWaveProgressionWithinTolerance(expected: Double, tolerance: Double = 5.0)`

### From Existing Unit Tests
**Patterns to Replicate**:
- Wait helpers with timeout (`waitForEvents`, `waitForState`)
- State transition verification
- Performance monitoring
- Error scenario handling

---

## 🔗 Integration with CI/CD

### GitHub Actions Workflow
**File**: `.github/workflows/firebase-ui-tests.yml`

```yaml
name: Firebase UI Tests

on:
  pull_request:
    branches: [main]
  workflow_dispatch:

jobs:
  firebase-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Setup Java
        uses: actions/setup-java@v3
        with:
          java-version: '17'

      - name: Build Android APKs
        run: |
          ./gradlew :composeApp:assembleDebug
          ./gradlew :composeApp:assembleDebugAndroidTest

      - name: Setup gcloud
        uses: google-github-actions/setup-gcloud@v1
        with:
          service_account_key: ${{ secrets.FIREBASE_SERVICE_ACCOUNT }}

      - name: Run Android Firebase Tests
        run: ./scripts/run_android_firebase_tests.sh

      - name: Build iOS
        run: ./scripts/build_ios_for_testing.sh

      - name: Run iOS Firebase Tests
        run: ./scripts/run_ios_firebase_tests.sh

      - name: Collect Screenshots
        run: ./scripts/collect_firebase_screenshots.sh

      - name: Generate Report
        run: python3 scripts/generate_test_report.py

      - name: Upload Results
        uses: actions/upload-artifact@v3
        with:
          name: firebase-test-results
          path: test_results/
```

---

## 📝 Next Steps Checklist

### Preparation Phase
- [ ] Review this TODO with team
- [ ] Get Firebase Test Lab quota/budget approval
- [ ] Setup Firebase project
- [ ] Define test data requirements
- [ ] Create test user accounts if needed

### Development Phase
- [ ] Implement Phase 1: Preparation (8-10h)
- [ ] Implement Phase 2: Android Test (16-20h)
- [ ] Implement Phase 3: iOS Test (20-24h)
- [ ] Implement Phase 4: Simulator Testing (4-6h)
- [ ] Implement Phase 5: Firebase Integration (8-12h)
- [ ] Implement Phase 6: Reporting (4-6h)

### Execution Phase
- [ ] Run on simulators first
- [ ] Fix all failures locally
- [ ] Submit to Firebase Test Lab
- [ ] Analyze results
- [ ] Fix any device-specific issues
- [ ] Achieve 100% pass rate

---

## 💰 Cost Estimate

### Firebase Test Lab Pricing
- **Android**: ~$1 per device-hour
- **iOS**: ~$5 per device-hour

**Per Test Run**:
- Android (3 devices × 15 min): ~$0.75
- iOS (2 devices × 20 min): ~$3.30
- **Total per run**: ~$4.05

**Monthly** (assuming daily runs):
- 30 runs × $4.05 = **$121.50/month**

**Note**: First 10 tests/day free on Spark plan physical devices

---

## 🎯 Acceptance Criteria

### Test Must:
1. Run completely automated (no manual intervention)
2. Complete in <20 minutes per device
3. Capture screenshot for every step
4. Pass on all targeted devices (3 Android + 2 iOS)
5. Generate actionable failure reports
6. Be maintainable (clear code, good structure)

### Screenshots Must:
1. Be high resolution (device native)
2. Show entire screen (no cropping)
3. Be properly labeled (step number, device, OS version)
4. Highlight key UI elements (arrows/annotations optional)
5. Be organized by device and step

### Report Must:
1. Show pass/fail status per device
2. Display all screenshots in order
3. Highlight differences between Android/iOS
4. Include performance metrics
5. Be shareable (HTML file or URL)

---

## 🔧 Maintenance Considerations

### Test Updates Required When:
- UI layout changes (update element locators)
- New features added (extend test flow)
- Event data structure changes (update test data)
- Navigation flow changes (update test steps)

### Regular Reviews:
- **Monthly**: Verify test still passes on latest OS versions
- **Per Release**: Run full suite before production deploy
- **On Failures**: Investigate within 24 hours

---

## 📖 Reference Documentation

### Firebase Test Lab
- https://firebase.google.com/docs/test-lab/android/get-started
- https://firebase.google.com/docs/test-lab/ios/get-started

### Testing Frameworks
- Jetpack Compose Testing: https://developer.android.com/jetpack/compose/testing
- XCUITest Guide: https://developer.apple.com/documentation/xctest/user_interface_tests

### WorldWideWaves Specific
- `docs/UI_TESTING_GUIDE.md` - Existing UI testing patterns
- `CLAUDE.md` - Project conventions
- `composeApp/src/androidInstrumentedTest/` - Existing test examples

---

## 🚨 Known Challenges & Solutions

### Challenge 1: Compose UI in XCUITest
**Problem**: XCUITest sees Compose elements as generic accessibility elements
**Solution**: Use clear, unique `testTag()` values and semantic properties
**Workaround**: Add explicit accessibility labels for iOS

### Challenge 2: Timing Issues
**Problem**: Wave choreography timing is time-sensitive
**Solution**: Use simulation mode with controllable time
**Fallback**: Add retry logic and longer timeouts

### Challenge 3: GPS Simulation
**Problem**: Need to simulate user in Paris for wave participation
**Solution**: Use PositionManager simulation mode
**Verification**: Check `isUserInArea` state in tests

### Challenge 4: Screenshot Consistency
**Problem**: Dynamic content (time, progression) changes between captures
**Solution**: Use simulation with fixed time, mock dynamic data
**Alternative**: Capture multiple screenshots and select best

### Challenge 5: Network Mocking
**Problem**: Tests need consistent Firebase data
**Solution**: Use dedicated test Firebase project with static data
**Backup**: Mock Firebase with local responses

---

## 📈 Success Metrics

### Test Quality
- **Pass Rate**: >95% on all devices
- **Flakiness**: <2% (max 2 flaky tests per 100 runs)
- **Execution Time**: <15 minutes per device
- **Coverage**: 100% of critical user paths

### Business Impact
- Catch UI regressions before production
- Verify wave participation works end-to-end
- Validate cross-platform consistency
- Reduce manual testing time by 80%

---

## 🎬 Next Actions

**When Ready to Implement**:
1. Review and approve this TODO
2. Allocate 60-80 hours for implementation
3. Setup Firebase Test Lab project
4. Start with Phase 1 (Preparation)
5. Use agents for parallel development (Android + iOS)
6. Test locally on simulators first
7. Submit to Firebase Test Lab
8. Iterate until 100% pass rate

**Estimated Timeline**:
- **Full-time**: 1.5-2 weeks
- **Part-time (50%)**: 3-4 weeks
- **Part-time (25%)**: 6-8 weeks

---

*Created: October 3, 2025*
*Status: Planning - Ready for Implementation*
*Effort: 60-80 hours*
*Priority: HIGH (post-unit test completion)*
