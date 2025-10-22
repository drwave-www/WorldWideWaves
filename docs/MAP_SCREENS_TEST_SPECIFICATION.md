# Map Screens Test Specification

> **Purpose**: Comprehensive test specification for the three map screen types in WorldWideWaves
> **Target**: Android and iOS platforms with shared test logic
> **Date**: October 2025

---

## Executive Summary

WorldWideWaves implements three distinct map screen types, each with different camera behavior, gesture handling, and user interaction patterns. This document provides a complete feature description and test specification for all three screens.

---

## 1. Map Screen Types Overview

### 1.1 Event Detail Screen Map
**Purpose**: Secondary read-only map showing event context
**Location**: `EventDetailScreen.kt` (lines 67, 81-85)
**Configuration**:
```kotlin
EventMapConfig(
    initialCameraPosition = BOUNDS,
    autoTargetUserOnFirstLocation = false,
    gesturesEnabled = false
)
```

### 1.2 Wave Participation Screen Map
**Purpose**: Primary participation screen with intelligent auto-tracking
**Location**: `WaveScreen.kt` (lines 61, 74-80)
**Configuration**:
```kotlin
EventMapConfig(
    initialCameraPosition = BOUNDS,
    autoTargetUserOnFirstLocation = false,
    gesturesEnabled = false
)
```

### 1.3 Full Map Screen
**Purpose**: Dedicated interactive exploration screen
**Location**: `FullMapScreen.kt` (lines 56-59)
**Configuration**:
```kotlin
EventMapConfig(
    initialCameraPosition = WINDOW,
    autoTargetUserOnFirstLocation = true,
    gesturesEnabled = true
)
```

---

## 2. Feature Descriptions by Screen Type

### 2.1 Event Detail Screen Map

#### 2.1.1 Camera Behavior
- **Initial Position**: BOUNDS mode - entire event area visible with zero padding
- **Camera Movement**: Static - camera never moves after initial positioning
- **Fitting Strategy**: Tight fit to event bounds (zero padding)
- **Min Zoom**: Calculated to show entire event area exactly
- **Max Zoom**: Event configuration `event.map.maxZoom`

#### 2.1.2 Gesture Handling
- **All Gestures**: Disabled (`gesturesEnabled = false`)
- **Pan**: Not allowed
- **Zoom**: Not allowed
- **Rotate**: Not allowed
- **Tap**: Not allowed

#### 2.1.3 User Position Display
- **User Marker**: Visible on map (updates with GPS)
- **Auto-Target**: Never triggers
- **Position Updates**: Visual marker updates only (no camera movement)

#### 2.1.4 Wave Visualization
- **Wave Polygons**: Rendered on map (via `MapPolygonDisplay` component)
- **Polygon Updates**: Passive - polygons update but don't trigger camera movement
- **Wave Progression**: Visual only (no auto-tracking)

#### 2.1.5 Boundary Enforcement
- **Mode**: BOUNDS mode (zero padding)
- **Constraint Type**: Min zoom prevents zoom-out beyond event area
- **Preventive**: Constraints applied AFTER animation (reactive)
- **Viewport**: Entire event area always visible

#### 2.1.6 Buttons & Controls
- **ButtonWave**: Present (navigates to Wave screen when enabled)
- **MapActions**: Not present
- **Target Wave**: Not available
- **Target User**: Not available

---

### 2.2 Wave Participation Screen Map

#### 2.2.1 Camera Behavior
- **Initial Position**: BOUNDS mode - entire event area visible with zero padding
- **Camera Movement**: Automatic - tracks user + wave when user enters area
- **Auto-Tracking Trigger**: User enters event area (`isInArea = true`)
- **Tracking Frequency**: Throttled to every 5% wave progression (≈20 updates total)
- **Tracking Method**: `targetUserAndWave()` - shows both user and wave front with padding
- **Fitting Strategy**: Dynamic bounds containing user + wave positions, padded 20% horizontal, 10% vertical
- **Max Bounds Size**: Limited to 50% of event area in each dimension (keeps focus tight)

#### 2.2.2 Gesture Handling
- **All Gestures**: Disabled (`gesturesEnabled = false`)
- **Pan**: Not allowed
- **Zoom**: Not allowed
- **Rotate**: Not allowed
- **Tap**: Not allowed
- **Reason**: Auto-tracking camera eliminates need for manual control

#### 2.2.3 User Position Display
- **User Marker**: Visible on map (updates with GPS)
- **Auto-Target**: Triggers when user enters area
- **Position Updates**: Both visual marker AND camera movement (auto-tracking)

#### 2.2.4 Wave Visualization
- **Wave Polygons**: Rendered on map (via `MapZoomAndLocationUpdate` component)
- **Polygon Updates**: Active - polygons trigger camera movement (auto-tracking)
- **Wave Progression**: Actively tracked (camera follows wave front)
- **Throttling**: Camera updates every **1 second (real time)** to prevent animation spam
- **Throttle Implementation**: Uses `kotlinx.coroutines.delay(WWWGlobals.Timing.MAP_CAMERA_UPDATE_INTERVAL_MS)`
- **Independent of Simulation**: Throttling uses wall clock time, not simulated/progression time

#### 2.2.5 Boundary Enforcement
- **Mode**: BOUNDS mode (zero padding for constraint calculation)
- **Constraint Type**: Min zoom prevents zoom-out beyond event area
- **Auto-Tracking Limits**: `targetUserAndWave()` respects constraint bounds
- **Padding Constraints**: Padded bounds limited to stay within event area
- **Viewport**: Entire event area always visible initially, then auto-tracking takes over

#### 2.2.6 Buttons & Controls
- **ButtonWave**: Present but typically disabled (user already participating)
- **MapActions**: Not present
- **Target Wave**: Not available (auto-tracking handles this)
- **Target User**: Not available (auto-tracking handles this)

---

### 2.3 Full Map Screen

#### 2.3.1 Camera Behavior
- **Initial Position**: WINDOW mode - fits **constraining dimension** to screen
- **Camera Movement**: Manual - user controls camera via gestures and buttons
- **Auto-Target First Location**: YES - targets user on first GPS fix (only if no user interaction yet)
  - **WINDOW Mode Override**: `targetUser()` keeps current zoom (null parameter), doesn't zoom to 16.0
- **User Interaction Detection**: `markUserInteracted()` prevents auto-targeting after user gesture/button
- **Fitting Strategy**: Intelligent aspect ratio matching (constraining dimension only)
  - Event WIDER than screen (eventAspect > screenAspect) → fit by **HEIGHT** (smallest dimension)
  - Event TALLER than screen (eventAspect < screenAspect) → fit by **WIDTH** (smallest dimension)
- **Zoom Calculation**: Uses MapLibre's `getCameraForLatLngBounds` with aspect-matched bounds
  ```kotlin
  // Create bounds matching constraining dimension
  if (eventAspect > screenAspect) {
      constrainedWidth = eventHeight * screenAspect
      bounds = BoundingBox(fullHeight, constrainedWidth)
  } else {
      constrainedHeight = eventWidth / screenAspect
      bounds = BoundingBox(constrainedHeight, fullWidth)
  }
  minZoom = mapLibre.getCameraForLatLngBounds(bounds).zoom
  ```
- **Key Principle**: Show **smallest dimension fully**, prevent outside pixels

#### 2.3.2 Gesture Handling
- **All Gestures**: Enabled (`gesturesEnabled = true`)
- **Pan**: Allowed - viewport can move within event area
- **Zoom**: Allowed - between min zoom (event fits) and max zoom (`event.map.maxZoom`)
- **Rotate**: Allowed (if MapLibre supports)
- **Tap**: Allowed (but no click handler implemented)

#### 2.3.3 Viewport Boundary Constraints (CRITICAL)

**FUNDAMENTAL RULE**: **NO SINGLE PIXEL OUTSIDE EVENT AREA ALLOWED**

##### 2.3.3.1 Preventive Enforcement Strategy
- **Constraints Applied**: BEFORE animation (preventive, not reactive)
- **MapLibre Native**: Uses `setLatLngBoundsForCameraTarget()` + `setMinZoomPreference()`
- **Gesture Prevention**: Invalid gestures rejected at platform level (not animated back)
- **Smooth Enforcement**: User never sees viewport exceeding bounds then snapping back
- **Invalid Viewport Detection**: Viewport >10° rejected as uninitialized, zero padding used

##### 2.3.3.2 Constraint Mechanisms

**Min Zoom Constraint**:
- **Purpose**: Prevents zooming out beyond showing smallest event dimension
- **Calculation**: MapLibre's `getCameraForLatLngBounds()` with constraining-dimension bounds
  - Wide event on tall screen: bounds with full height, width = height × screenAspect
  - Tall event on wide screen: bounds with full width, height = width / screenAspect
- **Safety Margin**: **REMOVED** - constraining dimension already prevents outside pixels
- **Enforcement**: Set BEFORE animation via `setMinZoomPreference()`, blocks zoom-out gestures
- **Dimension Validation**: Validates screen and event dimensions >0 to prevent division by zero

**Camera Center Bounds (Dynamic)**:
- **Purpose**: Prevents panning where viewport edges would exceed event area
- **Calculation**: Event bounds shrunk by **CURRENT viewport half-size** (recalculates with zoom)
  ```
  currentViewport = getVisibleRegion()
  viewportHalfHeight = currentViewport.height / 2
  viewportHalfWidth = currentViewport.width / 2

  constraintBounds = {
      sw: eventBounds.sw + (viewportHalfHeight, viewportHalfWidth),
      ne: eventBounds.ne - (viewportHalfHeight, viewportHalfWidth)
  }
  ```
- **Dynamic Behavior**:
  - Zoom IN: viewport smaller → constraint bounds expand → MORE pan area
  - Zoom OUT: viewport larger → constraint bounds shrink → LESS pan area
  - At min zoom: viewport = constraining dimension → minimal pan area (edges reachable)
- **Enforcement**:
  - Android: `setLatLngBoundsForCameraTarget()` + preventive gesture constraints (clamps camera)
  - iOS: `minimumZoomLevel` only (shouldChangeFrom validates zoom only, not viewport)
- **iOS Limitation**: shouldChangeFrom can only REJECT, not CLAMP → removed viewport validation for smooth gestures

##### 2.3.3.3 Edge & Corner Behavior

**Sticking to Edges**:
- **Allowed**: Viewport can stick to event edges (camera center at constraint boundary)
- **Visual**: One edge of viewport aligns with event boundary
- **Constraint**: Opposite edge stays within event area (no overflow)

**Sticking to Corners**:
- **Allowed**: Viewport can stick to event corners (camera center at constraint corner)
- **Visual**: Two edges of viewport align with event boundaries
- **Constraint**: Opposite edges stay within event area (no overflow)

**Zoom While Stuck**:
- **Zoom In**: Always allowed (viewport shrinks, more margin from edges)
- **Zoom Out**: Allowed until min zoom reached (viewport expands but never exceeds event bounds)
- **Edge Adjustment**: As viewport expands during zoom-out, camera center automatically adjusts inward to prevent overflow
- **Corner Adjustment**: Similar behavior - camera moves toward event center as needed

##### 2.3.3.4 Dimension Recalculation
- **Trigger**: Dimension change >10% (e.g., device rotation, window resize)
- **Action**: Recalculate WINDOW bounds with new aspect ratio
- **Mechanism**: `windowBoundsNeedRecalculation` flag + camera idle listener
- **Effect**: Map automatically re-fits to new screen dimensions

#### 2.3.4 User Position Display
- **User Marker**: Visible on map (updates with GPS)
- **Auto-Target**: Triggers on first GPS fix only (if no user interaction yet)
- **Position Updates**: Visual marker updates only (no automatic camera movement after first fix)

#### 2.3.5 Wave Visualization
- **Wave Polygons**: Rendered on map (passive display)
- **Polygon Updates**: Visual only - polygons update but don't trigger camera movement
- **Wave Progression**: Visible but not auto-tracked (user controls camera)

#### 2.3.6 Buttons & Controls

**ButtonWave** (top center, 40dp padding):
- **Position**: `Modifier.align(Alignment.TopCenter).padding(top = 40.dp)`
- **Visibility**: Visible when event is running and user in area
- **Action**: Navigates to Wave Participation Screen
- **States**: Active (running + in area) / Inactive (otherwise)

**MapActions** (bottom right, 16dp padding):
- **Position**: `Modifier.align(Alignment.BottomEnd).padding(16.dp)`
- **Buttons**: Two buttons in horizontal row with spacing

  **Target Wave Button**:
  - **Icon**: `target_wave_active` (running + wave started) / `target_wave_inactive` (otherwise)
  - **Enabled**: When `eventStatus == RUNNING` AND `now() > waveStartDateTime`
  - **Action**:
    ```kotlin
    eventMap.markUserInteracted() // Prevent auto-targeting
    eventMap.targetWave() // Camera to wave longitude at user latitude
    ```
  - **Camera Zoom**: `MapDisplay.TARGET_WAVE_ZOOM`
  - **Accessibility**: `event_target_wave_on` / `event_target_wave_off`

  **Target User Button**:
  - **Icon**: `target_me_active` (in area) / `target_me_inactive` (otherwise)
  - **Enabled**: When `isInArea == true`
  - **Action**:
    ```kotlin
    eventMap.markUserInteracted() // Prevent auto-targeting
    eventMap.targetUser() // Camera to user position
    ```
  - **Camera Zoom**: `MapDisplay.TARGET_USER_ZOOM`
  - **Accessibility**: `event_target_me_on` / `event_target_me_off`

#### 2.3.7 One Full Dimension Visibility
- **Requirement**: User can zoom out to see fully the **SMALLEST dimension** (width OR height)
- **Implementation**: Min zoom calculated from constraining dimension only
- **Example**: For wide event (Paris 2.84:1) on tall screen (0.62:1), user can zoom to see full **HEIGHT** (smallest)
- **Prevents**: Outside pixels from being visible (larger dimension partially visible)
- **Verification**: Min zoom = getCameraForLatLngBounds(constrainingDimensionBounds).zoom

---

## 3. Critical Implementation Details (October 2025)

### 3.0 Recent Fixes and Learnings

#### 3.0.1 MapBoundsEnforcer Invalid Viewport Detection
**Issue**: Early in initialization, `getVisibleRegion()` returns invalid data (90°×180° - bigger than Earth!)
**Fix**: Validate viewport half-size >10° and return zero padding until map initializes
**Location**: `MapBoundsEnforcer.kt:406-413`
**Impact**: Prevents microscopic constraint bounds (0.0017°×0.0049°) that block all gestures

#### 3.0.2 Min Zoom Calculation from Constraining Dimension
**Issue**: Using full event bounds for min zoom showed BOTH dimensions, causing outside pixels
**Fix**: Create aspect-ratio-matched bounds for smallest dimension only
**Formula**:
```kotlin
if (eventAspect > screenAspect) {
    // Wide event → constrained by HEIGHT (smallest)
    constrainedWidth = eventHeight * screenAspect
} else {
    // Tall event → constrained by WIDTH (smallest)
    constrainedHeight = eventWidth / screenAspect
}
```
**Impact**: Min zoom now allows seeing smallest dimension fully without outside pixels

#### 3.0.3 Dynamic Constraint Bounds
**Issue**: Using viewport at min zoom for constraints created FIXED bounds that blocked zoom
**Fix**: Calculate constraint bounds from CURRENT viewport (changes with zoom)
**Impact**: Pan area expands when zoomed in, shrinks when zoomed out (correct behavior)

#### 3.0.4 Wave Throttling Change
**Issue**: 5% progression throttling gave ~20 updates per wave (tied to wave speed)
**Fix**: Changed to 1 second real time using `kotlinx.coroutines.delay(1000)`
**Location**: `MapZoomAndLocationUpdate.kt:58-65`
**Impact**: Consistent update frequency independent of simulation speed

#### 3.0.5 iOS Gesture Validation Removed
**Issue**: iOS `shouldChangeFrom` can only REJECT, not CLAMP like Android
**Finding**: Validating viewport boundaries caused 663 rejections, making gestures unusable
**Fix**: Removed viewport validation, kept only zoom validation
**Trade-off**: iOS allows slight viewport overshoot for smooth gestures (Android clamps perfectly)
**Location**: `MapLibreViewWrapper.swift:1296-1324`

#### 3.0.8 Gesture Detection for Button Animations
**Issue**: Preventive gesture constraints were clamping button animations (targetUser/targetWave)
**Finding**: `REASON_API_ANIMATION` was treated as user gesture, causing camera to jump back
**Fix**: Exclude `REASON_API_ANIMATION` from gesture detection (lines 530-534 in AndroidMapLibreAdapter.kt)
**Location**: `AndroidMapLibreAdapter.kt:527-535`
**Impact**: Button animations (Target User, Target Wave) now work smoothly without jumping

#### 3.0.9 MapBoundsEnforcer Camera Idle Recalculation in WINDOW Mode
**Issue**: After button animations (zoom 16), camera idle triggered constraint recalculation
**Finding**: Recalculation used tiny viewport (0.0055° at zoom 16), creating microscopic constraints
**Symptom**: After targetUser, couldn't pan south/north (locked by tiny bounds 0.0017°×0.0049°)
**Fix**: Disable MapBoundsEnforcer camera idle listener in WINDOW mode (line 82)
**Rationale**: WINDOW mode constraints are INITIAL bounds (from first viewport), not dynamic
**Location**: `MapBoundsEnforcer.kt:79-107`
**Impact**: Constraints stay constant after button animations, full pan area available

#### 3.0.6 Screen Dimension Units
**Finding**: Android uses physical pixels, iOS uses points (density-independent)
**Status**: Both correct - aspect ratios are unitless, MapLibre handles density internally
**Validation Added**: Check dimensions >0 before aspect ratio calculation to prevent division by zero

#### 3.0.7 targetUser() Zoom Override in WINDOW Mode
**Issue**: `targetUser()` was zooming to 16.0, overriding min zoom (14.23)
**Fix**: In WINDOW mode, `targetUser()` passes null zoom to keep current zoom
**Location**: `AbstractEventMap.kt:315-320`
**Impact**: Full map stays at min zoom showing smallest dimension fully

### 3.1 Android Implementation

#### 3.1.1 Activities
- **Event Detail**: `EventActivity.kt` - creates `EventDetailScreen`
- **Wave**: `WaveActivity.kt` - creates `WaveScreen`
- **Full Map**: `EventFullMapActivity.kt` - creates `FullMapScreen`

#### 3.1.2 Map Adapter
- **Type**: `AndroidMapLibreAdapter` (wraps MapLibre Android SDK)
- **User Position**: `LocationComponent` (native automatic updates)
- **Constraint Enforcement**: `setLatLngBoundsForCameraTarget()` + `setMinZoomPreference()`

#### 3.1.3 Testing Hooks
- **MapView**: Accessible via activity for testing
- **Camera Position**: Query via `mapLibreAdapter.getCameraPosition()`
- **Visible Region**: Query via `mapLibreAdapter.getVisibleRegion()`
- **Zoom Level**: Query via `mapLibreAdapter.getZoomLevel()`
- **Gestures**: Simulate via Espresso touch events

### 3.2 iOS Implementation

#### 3.2.1 View Controllers
- **Event Detail**: `makeEventViewController(id)` in `RootController.kt` (lines 216-247)
- **Wave**: `makeWaveViewController(id)` in `RootController.kt` (lines 274-305)
- **Full Map**: `makeFullMapViewController(id)` in `RootController.kt` (lines 332-358)

#### 3.2.2 Map Adapter
- **Type**: `IosMapLibreAdapter` (wraps MapLibre iOS SDK)
- **User Position**: Custom `MLNPointAnnotation` (manual updates via PositionManager)
- **Constraint Enforcement**: `MLNMapViewDelegate.shouldChangeFrom` (gesture clamping)
- **Registry Keys**: Unique per screen to prevent wrapper conflicts
  ```kotlin
  "${event.id}-event"    // Event detail
  "${event.id}-wave"     // Wave screen
  "${event.id}-fullmap"  // Full map
  ```

#### 3.2.3 Testing Hooks
- **MLNMapView**: Accessible via view controller for testing
- **Camera**: Query via `mapView.centerCoordinate`, `mapView.zoomLevel`
- **Visible Region**: Query via `mapView.visibleCoordinateBounds`
- **Gestures**: Simulate via XCTest touch events

---

## 4. Shared Test Logic Architecture

### 4.1 Test Organization Strategy

To avoid duplicating test logic across Android and iOS, we'll use a **Platform-Agnostic Test Specification Pattern**:

```
┌─────────────────────────────────────────────────┐
│   Platform-Independent Test Specifications      │
│   (commonTest or shared documentation)          │
│                                                  │
│   - Feature behaviors (what to test)            │
│   - Expected outcomes (assertions)               │
│   - Test data (event configurations)             │
└─────────────────────────────────────────────────┘
                      │
         ┌────────────┴────────────┐
         │                         │
         ▼                         ▼
┌──────────────────┐      ┌──────────────────┐
│ Android Tests    │      │ iOS Tests        │
│ (androidTest)    │      │ (iosTest)        │
│                  │      │                  │
│ - MapTestRobot   │      │ - MapTestRobot   │
│ - Espresso       │      │ - XCTest         │
│ - ActivityRule   │      │ - XCUITest       │
└──────────────────┘      └──────────────────┘
```

### 4.2 Test Specification Format

Each test will be defined in a platform-independent format:

```kotlin
// Common test specification (pseudocode)
data class MapTestSpec(
    val name: String,
    val screenType: ScreenType,
    val preconditions: List<Precondition>,
    val actions: List<Action>,
    val assertions: List<Assertion>
)

enum class ScreenType {
    EVENT_DETAIL,
    WAVE_PARTICIPATION,
    FULL_MAP
}

sealed class Precondition {
    data class EventState(val status: Status) : Precondition()
    data class UserPosition(val lat: Double, val lng: Double) : Precondition()
    data class WaveProgression(val percentage: Int) : Precondition()
}

sealed class Action {
    object WaitForMapLoad : Action()
    data class Pan(val deltaX: Float, val deltaY: Float) : Action()
    data class Zoom(val delta: Float) : Action()
    data class TapButton(val button: Button) : Action()
    data class WaitForDuration(val ms: Long) : Action()
}

sealed class Assertion {
    data class CameraPosition(val expectedLat: Double, val expectedLng: Double, val tolerance: Double = 0.0001) : Assertion()
    data class ZoomLevel(val expectedZoom: Double, val tolerance: Double = 0.1) : Assertion()
    data class ViewportWithinBounds(val eventBounds: BoundingBox) : Assertion()
    data class GestureEnabled(val enabled: Boolean) : Assertion()
    data class ButtonVisible(val button: Button, val visible: Boolean) : Assertion()
    data class ButtonEnabled(val button: Button, val enabled: Boolean) : Assertion()
}
```

### 4.3 Platform Test Adapters

Each platform implements a `MapTestRobot` that translates generic actions/assertions to platform-specific operations:

**Android**:
```kotlin
class AndroidMapTestRobot(activity: Activity) {
    fun performAction(action: Action) { /* Espresso/UIAutomator */ }
    fun verifyAssertion(assertion: Assertion) { /* Espresso matchers */ }
}
```

**iOS**:
```kotlin
class IosMapTestRobot(viewController: UIViewController) {
    fun performAction(action: Action) { /* XCTest/XCUITest */ }
    fun verifyAssertion(assertion: Assertion) { /* XCTest assertions */ }
}
```

---

## 5. Comprehensive Test Suite

### 5.1 Event Detail Screen Tests

#### 5.1.1 Initial Camera Position
**Test**: `test_eventDetail_initialCameraShowsEntireEventArea`
- **Preconditions**:
  - Event exists with known bounds
  - Map initialized
- **Actions**:
  - Wait for map load
- **Assertions**:
  - Camera position = event center
  - Zoom level = calculated min zoom (entire event visible)
  - Viewport contains all event corners
  - Viewport padding ≈ 0 (tight fit)

#### 5.1.2 Gesture Blocking
**Test**: `test_eventDetail_allGesturesBlocked`
- **Preconditions**:
  - Map loaded
- **Actions**:
  - Attempt pan gesture (swipe)
  - Attempt pinch zoom in
  - Attempt pinch zoom out
  - Attempt double-tap zoom
  - Attempt two-finger tap zoom out
- **Assertions**:
  - Camera position unchanged
  - Zoom level unchanged
  - Viewport unchanged

#### 5.1.3 User Position Display Without Camera Movement
**Test**: `test_eventDetail_userMarkerUpdatesWithoutCameraMovement`
- **Preconditions**:
  - Map loaded
  - Initial camera position recorded
- **Actions**:
  - Simulate GPS update (position A)
  - Wait for marker update
  - Simulate GPS update (position B)
  - Wait for marker update
- **Assertions**:
  - User marker visible at position A (after first update)
  - Camera position unchanged
  - User marker visible at position B (after second update)
  - Camera position unchanged

#### 5.1.4 Wave Polygon Display Without Camera Movement
**Test**: `test_eventDetail_wavePolygonsDisplayWithoutCameraMovement`
- **Preconditions**:
  - Map loaded
  - Event status = RUNNING
  - Initial camera position recorded
- **Actions**:
  - Trigger wave start
  - Wait for wave progression 25%
  - Wait for wave progression 50%
  - Wait for wave progression 75%
- **Assertions**:
  - Wave polygons visible (at each progression)
  - Camera position unchanged
  - Zoom level unchanged

#### 5.1.5 ButtonWave Visibility
**Test**: `test_eventDetail_buttonWaveVisibility`
- **Preconditions**:
  - Map loaded
- **Actions**:
  - Set event status = SCHEDULED, user not in area
  - Set event status = RUNNING, user not in area
  - Set event status = RUNNING, user in area
  - Set event status = FINISHED, user in area
- **Assertions**:
  - Button visible but disabled (SCHEDULED, not in area)
  - Button visible but disabled (RUNNING, not in area)
  - Button visible and enabled (RUNNING, in area)
  - Button visible but disabled (FINISHED, in area)

#### 5.1.6 MapActions Not Present
**Test**: `test_eventDetail_mapActionsNotPresent`
- **Preconditions**:
  - Map loaded
- **Actions**:
  - None
- **Assertions**:
  - Target Wave button not present
  - Target User button not present

---

### 5.2 Wave Participation Screen Tests

#### 5.2.1 Initial Camera Position
**Test**: `test_wave_initialCameraShowsEntireEventArea`
- **Preconditions**:
  - Event exists with known bounds
  - Map initialized
  - User NOT in area
- **Actions**:
  - Wait for map load
- **Assertions**:
  - Camera position = event center
  - Zoom level = calculated min zoom (entire event visible)
  - Viewport contains all event corners

#### 5.2.2 Auto-Tracking When User Enters Area
**Test**: `test_wave_autoTrackingTriggersWhenUserEntersArea`
- **Preconditions**:
  - Map loaded (entire event visible)
  - Event status = RUNNING
  - Wave started
  - User NOT in area
  - Initial camera position recorded
- **Actions**:
  - Simulate user entering event area (GPS update)
  - Wait for camera animation
- **Assertions**:
  - Camera position changed (moved toward user + wave)
  - Camera position approximately midpoint between user and wave
  - Viewport contains both user marker and wave front
  - Viewport has ~20% horizontal padding, ~10% vertical padding

#### 5.2.3 Auto-Tracking Throttling (1 Second Real Time)
**Test**: `test_wave_autoTrackingThrottledToOneSecond`
- **Preconditions**:
  - Map loaded
  - Event status = RUNNING
  - User in area
  - Wave started
- **Actions**:
  - Record initial camera position and timestamp
  - Wait 500ms (record camera)
  - Wait 500ms more (total 1000ms, record camera)
  - Wait 500ms more (total 1500ms, record camera)
  - Wait 500ms more (total 2000ms, record camera)
  - Wait 500ms more (total 2500ms, record camera)
- **Assertions**:
  - Camera unchanged at 500ms (throttled)
  - Camera moved at 1000ms (1 second elapsed) ✅
  - Camera unchanged at 1500ms (throttled)
  - Camera moved at 2000ms (2 seconds elapsed) ✅
  - Camera unchanged at 2500ms (throttled)
  - Update interval = 1 second real time (independent of simulation speed)
  - Uses `WWWGlobals.Timing.MAP_CAMERA_UPDATE_INTERVAL_MS` constant

#### 5.2.4 Auto-Tracking Bounds Constraints
**Test**: `test_wave_autoTrackingRespectsBoundsConstraints`
- **Preconditions**:
  - Map loaded
  - Event status = RUNNING
  - User in area (near event edge)
  - Wave started
- **Actions**:
  - Wait for wave progression 50% (user and wave far apart)
  - Record camera bounds
- **Assertions**:
  - Viewport contains user marker
  - Viewport contains wave front
  - Viewport bounded to max 50% of event area in each dimension
  - Viewport completely within event bounds (no overflow)

#### 5.2.5 Gesture Blocking
**Test**: `test_wave_allGesturesBlocked`
- **Preconditions**:
  - Map loaded
  - Auto-tracking active
- **Actions**:
  - Attempt pan gesture
  - Attempt zoom gesture
  - Wait for next auto-tracking update
- **Assertions**:
  - Manual gestures blocked
  - Auto-tracking continues (camera moves to next position)

#### 5.2.6 ButtonWave Typically Disabled
**Test**: `test_wave_buttonWaveDisabledDuringParticipation`
- **Preconditions**:
  - Map loaded
  - User in area
  - Event status = RUNNING
- **Actions**:
  - None
- **Assertions**:
  - Button visible but disabled (user already on wave screen)

#### 5.2.7 MapActions Not Present
**Test**: `test_wave_mapActionsNotPresent`
- **Preconditions**:
  - Map loaded
- **Actions**:
  - None
- **Assertions**:
  - Target Wave button not present
  - Target User button not present

---

### 5.3 Full Map Screen Tests

#### 5.3.1 Initial Camera Position (WINDOW Mode)
**Test**: `test_fullMap_initialCameraFitsEventWithAspectRatio`
- **Preconditions**:
  - Event exists with known bounds
  - Screen dimensions known
  - Map initialized
- **Actions**:
  - Wait for map load
- **Assertions**:
  - Camera position = event center
  - Zoom level = min(zoomForWidth, zoomForHeight)
  - Viewport fits event area (one dimension fills screen)
  - Viewport completely within event bounds

**Test**: `test_fullMap_aspectRatioFitting_wideEvent`
- **Preconditions**:
  - Event aspect ratio > screen aspect ratio (wide event)
- **Actions**:
  - Wait for map load
- **Assertions**:
  - Zoom calculated to fit event HEIGHT (width overflows screen but not event)
  - Event height fills screen vertically
  - Entire event width visible (no horizontal overflow beyond event bounds)

**Test**: `test_fullMap_aspectRatioFitting_tallEvent`
- **Preconditions**:
  - Event aspect ratio < screen aspect ratio (tall event)
- **Actions**:
  - Wait for map load
- **Assertions**:
  - Zoom calculated to fit event WIDTH (height overflows screen but not event)
  - Event width fills screen horizontally
  - Entire event height visible (no vertical overflow beyond event bounds)

#### 5.3.2 Auto-Target First Location
**Test**: `test_fullMap_autoTargetsUserOnFirstGpsFix`
- **Preconditions**:
  - Map loaded (event centered)
  - No user interaction yet
  - User position unknown
- **Actions**:
  - Simulate first GPS fix (user position)
  - Wait for camera animation
- **Assertions**:
  - Camera moved to user position
  - Zoom level = TARGET_USER_ZOOM
  - Auto-target does NOT trigger on subsequent GPS updates

**Test**: `test_fullMap_autoTargetBlockedByUserGesture`
- **Preconditions**:
  - Map loaded
  - User position unknown
- **Actions**:
  - Perform pan gesture (user interaction)
  - Simulate first GPS fix
  - Wait for potential camera movement
- **Assertions**:
  - Camera position unchanged (auto-target blocked by user interaction)
  - User marker visible at GPS position

**Test**: `test_fullMap_autoTargetBlockedByButton`
- **Preconditions**:
  - Map loaded
  - Event status = RUNNING
  - Wave started
- **Actions**:
  - Tap "Target Wave" button (user interaction)
  - Simulate first GPS fix
  - Wait for potential camera movement
- **Assertions**:
  - Camera at wave position (from button tap)
  - Auto-target does NOT trigger for GPS fix

#### 5.3.3 Gesture Enablement
**Test**: `test_fullMap_panGestureAllowed`
- **Preconditions**:
  - Map loaded
  - Initial camera position recorded
- **Actions**:
  - Perform pan gesture (swipe right)
  - Wait for camera movement
- **Assertions**:
  - Camera position changed (moved left on map)
  - Change magnitude proportional to swipe distance

**Test**: `test_fullMap_zoomInGestureAllowed`
- **Preconditions**:
  - Map loaded
  - Initial zoom level recorded
- **Actions**:
  - Perform pinch-to-zoom-in gesture
  - Wait for zoom animation
- **Assertions**:
  - Zoom level increased
  - Zoom level ≤ event.map.maxZoom

**Test**: `test_fullMap_zoomOutGestureAllowed`
- **Preconditions**:
  - Map loaded
  - Zoom in first (to create room to zoom out)
  - Zoom level recorded
- **Actions**:
  - Perform pinch-to-zoom-out gesture
  - Wait for zoom animation
- **Assertions**:
  - Zoom level decreased
  - Zoom level ≥ calculated min zoom

#### 5.3.4 Viewport Boundary Enforcement - No Pixels Outside Event

**Test**: `test_fullMap_neverShowsPixelsOutsideEventArea_centerPosition`
- **Preconditions**:
  - Map loaded (event centered)
- **Actions**:
  - Record visible region bounds
- **Assertions**:
  - Viewport SW ≥ event bounds SW
  - Viewport NE ≤ event bounds NE
  - All viewport corners within event bounds

**Test**: `test_fullMap_neverShowsPixelsOutsideEventArea_afterPan`
- **Preconditions**:
  - Map loaded
- **Actions**:
  - Pan to north edge (multiple times, aggressive)
  - Record visible region
  - Pan to south edge (multiple times, aggressive)
  - Record visible region
  - Pan to east edge (multiple times, aggressive)
  - Record visible region
  - Pan to west edge (multiple times, aggressive)
  - Record visible region
- **Assertions**:
  - After each pan: viewport completely within event bounds
  - No overflow in any direction

**Test**: `test_fullMap_neverShowsPixelsOutsideEventArea_afterZoom`
- **Preconditions**:
  - Map loaded
- **Actions**:
  - Zoom out to min zoom (aggressive, multiple attempts)
  - Record visible region
- **Assertions**:
  - Viewport completely within event bounds
  - Zoom level = min zoom (cannot zoom out further)
  - One full dimension visible (width OR height fills screen)

**Test**: `test_fullMap_preventiveEnforcement_gesturesBlockedBeforeViolation`
- **Preconditions**:
  - Map loaded
  - Pan to west edge (viewport at boundary)
  - Record camera position
- **Actions**:
  - Attempt pan further west (would violate boundary)
  - Wait briefly
- **Assertions**:
  - Camera position unchanged (gesture rejected, not animated back)
  - Viewport still completely within event bounds
  - No visible "snap back" animation

#### 5.3.5 Edge & Corner Sticking

**Test**: `test_fullMap_canStickToNorthEdge`
- **Preconditions**:
  - Map loaded
  - Zoom level at min zoom
- **Actions**:
  - Pan to north edge
- **Assertions**:
  - Viewport north edge ≈ event north edge (within 1% tolerance)
  - Viewport south edge > event south edge (within bounds)
  - Viewport completely within event bounds

**Test**: `test_fullMap_canStickToSouthEdge`
- **Preconditions**:
  - Map loaded
  - Zoom level at min zoom
- **Actions**:
  - Pan to south edge
- **Assertions**:
  - Viewport south edge ≈ event south edge (within 1% tolerance)
  - Viewport north edge < event north edge (within bounds)
  - Viewport completely within event bounds

**Test**: `test_fullMap_canStickToEastEdge`
- **Preconditions**:
  - Map loaded
  - Zoom level at min zoom
- **Actions**:
  - Pan to east edge
- **Assertions**:
  - Viewport east edge ≈ event east edge (within 1% tolerance)
  - Viewport west edge > event west edge (within bounds)
  - Viewport completely within event bounds

**Test**: `test_fullMap_canStickToWestEdge`
- **Preconditions**:
  - Map loaded
  - Zoom level at min zoom
- **Actions**:
  - Pan to west edge
- **Assertions**:
  - Viewport west edge ≈ event west edge (within 1% tolerance)
  - Viewport east edge < event east edge (within bounds)
  - Viewport completely within event bounds

**Test**: `test_fullMap_canStickToNorthWestCorner`
- **Preconditions**:
  - Map loaded
  - Zoom level at min zoom
- **Actions**:
  - Pan to north edge
  - Pan to west edge
- **Assertions**:
  - Viewport NW corner ≈ event NW corner (within 1% tolerance)
  - Viewport SE corner within event bounds
  - Viewport completely within event bounds

**Test**: `test_fullMap_canStickToNorthEastCorner`
- **Preconditions**:
  - Map loaded
  - Zoom level at min zoom
- **Actions**:
  - Pan to north edge
  - Pan to east edge
- **Assertions**:
  - Viewport NE corner ≈ event NE corner (within 1% tolerance)
  - Viewport SW corner within event bounds
  - Viewport completely within event bounds

**Test**: `test_fullMap_canStickToSouthWestCorner`
- **Preconditions**:
  - Map loaded
  - Zoom level at min zoom
- **Actions**:
  - Pan to south edge
  - Pan to west edge
- **Assertions**:
  - Viewport SW corner ≈ event SW corner (within 1% tolerance)
  - Viewport NE corner within event bounds
  - Viewport completely within event bounds

**Test**: `test_fullMap_canStickToSouthEastCorner`
- **Preconditions**:
  - Map loaded
  - Zoom level at min zoom
- **Actions**:
  - Pan to south edge
  - Pan to east edge
- **Assertions**:
  - Viewport SE corner ≈ event SE corner (within 1% tolerance)
  - Viewport NW corner within event bounds
  - Viewport completely within event bounds

#### 5.3.6 Zoom While Stuck to Edge/Corner

**Test**: `test_fullMap_canZoomInWhileStuckToEdge`
- **Preconditions**:
  - Map loaded
  - Pan to north edge (viewport stuck)
  - Current zoom level recorded
- **Actions**:
  - Perform zoom-in gesture
  - Wait for animation
- **Assertions**:
  - Zoom level increased
  - Viewport still at north edge (or slightly inward)
  - Viewport completely within event bounds

**Test**: `test_fullMap_canZoomOutWhileStuckToEdge`
- **Preconditions**:
  - Map loaded
  - Zoom in to above min zoom
  - Pan to north edge (viewport stuck)
  - Current zoom level recorded
- **Actions**:
  - Perform zoom-out gesture
  - Wait for animation
- **Assertions**:
  - Zoom level decreased (if above min zoom)
  - Camera center adjusted inward to prevent overflow
  - Viewport still touches north edge (or moved slightly inward)
  - Viewport completely within event bounds

**Test**: `test_fullMap_canZoomInWhileStuckToCorner`
- **Preconditions**:
  - Map loaded
  - Pan to NW corner (viewport stuck)
  - Current zoom level recorded
- **Actions**:
  - Perform zoom-in gesture
  - Wait for animation
- **Assertions**:
  - Zoom level increased
  - Viewport still at NW corner (or slightly inward)
  - Viewport completely within event bounds

**Test**: `test_fullMap_canZoomOutWhileStuckToCorner`
- **Preconditions**:
  - Map loaded
  - Zoom in to above min zoom
  - Pan to NW corner (viewport stuck)
  - Current zoom level recorded
- **Actions**:
  - Perform zoom-out gesture
  - Wait for animation
- **Assertions**:
  - Zoom level decreased (if above min zoom)
  - Camera center adjusted inward (both lat and lng) to prevent overflow
  - Viewport still touches NW corner (or moved slightly inward)
  - Viewport completely within event bounds

#### 5.3.7 One Full Dimension Visibility

**Test**: `test_fullMap_canZoomToShowSmallestDimension_wideEvent`
- **Preconditions**:
  - Event is wide (eventAspect > screenAspect) - e.g., Paris
  - Map loaded
- **Actions**:
  - Zoom out to min zoom (attempt to zoom further should be blocked)
  - Center camera on event center
  - Query visible region
- **Assertions**:
  - Entire event **HEIGHT** visible (smallest dimension) ✅
  - Event **width** partially visible (NO outside pixels at edges)
  - Viewport height ≈ event height (tolerance ±5%)
  - Viewport completely within event bounds
  - Can pan horizontally to see different parts of width
  - Cannot pan vertically much (height fills viewport)

**Test**: `test_fullMap_canZoomToShowSmallestDimension_tallEvent`
- **Preconditions**:
  - Event is tall (eventAspect < screenAspect)
  - Map loaded
- **Actions**:
  - Zoom out to min zoom
  - Center camera on event center
  - Query visible region
- **Assertions**:
  - Entire event **WIDTH** visible (smallest dimension) ✅
  - Event **height** partially visible (NO outside pixels at edges)
  - Viewport width ≈ event width (tolerance ±5%)
  - Viewport completely within event bounds
  - Can pan vertically to see different parts of height
  - Cannot pan horizontally much (width fills viewport)

#### 5.3.8 Dimension Change Recalculation

**Test**: `test_fullMap_recalculatesBoundsOnDimensionChange`
- **Preconditions**:
  - Map loaded on portrait screen
  - Initial bounds recorded
- **Actions**:
  - Rotate device to landscape (dimension change >10%)
  - Wait for recalculation
- **Assertions**:
  - Bounds recalculated with new aspect ratio
  - Camera position adjusted to new WINDOW bounds
  - Viewport completely within event bounds
  - One dimension fills screen (aspect ratio matching)

**Test**: `test_fullMap_noRecalculationOnMinorDimensionChange`
- **Preconditions**:
  - Map loaded
  - Initial bounds recorded
- **Actions**:
  - Simulate minor dimension change (5% - below 10% threshold)
  - Wait briefly
- **Assertions**:
  - Bounds NOT recalculated (below threshold)
  - Camera position unchanged
  - `windowBoundsNeedRecalculation` flag cleared

#### 5.3.9 Target Wave Button

**Test**: `test_fullMap_targetWaveButton_visibility`
- **Preconditions**:
  - Map loaded
- **Actions**:
  - Set event status = SCHEDULED (wave not started)
  - Set event status = RUNNING, current time < wave start time
  - Set event status = RUNNING, current time > wave start time
  - Set event status = FINISHED
- **Assertions**:
  - Button shows inactive icon (SCHEDULED)
  - Button shows inactive icon (RUNNING but wave not started)
  - Button shows active icon (RUNNING and wave started)
  - Button shows inactive icon (FINISHED)

**Test**: `test_fullMap_targetWaveButton_functionality`
- **Preconditions**:
  - Map loaded
  - Event status = RUNNING
  - Wave started
  - User position known
  - Wave longitude known
- **Actions**:
  - Record camera position
  - Tap "Target Wave" button
  - Wait for camera animation
- **Assertions**:
  - Camera moved to wave position (user latitude, wave longitude)
  - Zoom level = TARGET_WAVE_ZOOM
  - `markUserInteracted()` called (auto-target disabled)

**Test**: `test_fullMap_targetWaveButton_disabledWhenWaveNotStarted`
- **Preconditions**:
  - Map loaded
  - Event status = RUNNING
  - Current time < wave start time
- **Actions**:
  - Tap "Target Wave" button
  - Wait briefly
- **Assertions**:
  - Camera position unchanged (button disabled)

#### 5.3.10 Target User Button

**Test**: `test_fullMap_targetUserButton_visibility`
- **Preconditions**:
  - Map loaded
- **Actions**:
  - Set user not in area
  - Set user in area
  - Set user not in area again
- **Assertions**:
  - Button shows inactive icon (not in area)
  - Button shows active icon (in area)
  - Button shows inactive icon (not in area again)

**Test**: `test_fullMap_targetUserButton_functionality`
- **Preconditions**:
  - Map loaded
  - User in area
  - User position known
- **Actions**:
  - Record camera position
  - Tap "Target User" button
  - Wait for camera animation
- **Assertions**:
  - Camera moved to user position
  - Zoom level = TARGET_USER_ZOOM
  - `markUserInteracted()` called (auto-target disabled)

**Test**: `test_fullMap_targetUserButton_disabledWhenNotInArea`
- **Preconditions**:
  - Map loaded
  - User not in area
- **Actions**:
  - Tap "Target User" button
  - Wait briefly
- **Assertions**:
  - Camera position unchanged (button disabled)

#### 5.3.11 ButtonWave Visibility

**Test**: `test_fullMap_buttonWave_visibilityAndNavigation`
- **Preconditions**:
  - Map loaded
- **Actions**:
  - Set event status = RUNNING, user in area
  - Tap ButtonWave
- **Assertions**:
  - Button visible and enabled
  - Navigation to Wave Participation Screen triggered

---

### 5.4 Cross-Cutting Tests (All Screen Types)

#### 5.4.1 Map Loading
**Test**: `test_{screenType}_mapLoadsSuccessfully`
- **Preconditions**:
  - Event exists
  - Map tiles available
- **Actions**:
  - Initialize screen
  - Wait for map load callback
- **Assertions**:
  - Map loaded (style applied)
  - Attribution visible (margins = 0)
  - Location component enabled
  - Max zoom = event.map.maxZoom

#### 5.4.2 User Position Marker
**Test**: `test_{screenType}_userPositionMarkerVisible`
- **Preconditions**:
  - Map loaded
  - GPS position available
- **Actions**:
  - Simulate GPS update
  - Wait for marker update
- **Assertions**:
  - User marker visible on map
  - Marker position = GPS position

#### 5.4.3 Wave Polygons Rendering
**Test**: `test_{screenType}_wavePolygonsRender`
- **Preconditions**:
  - Map loaded
  - Event status = RUNNING
- **Actions**:
  - Trigger wave start
  - Wait for wave progression 50%
- **Assertions**:
  - Wave polygons visible
  - Polygon count > 0
  - Polygons rendered with correct style

---

### 5.5 MapLibre Visible Region Tests (All Screen Types)

These tests verify that the actual visible region returned by MapLibre's native APIs matches the expected viewport bounds. This is critical for validating that our constraints are properly enforced at the MapLibre level.

#### 5.5.1 Visible Region Accuracy - Initial State

**Test**: `test_{screenType}_visibleRegionMatchesInitialCameraPosition`
- **Preconditions**:
  - Map loaded
  - Initial camera position set
  - Map tiles rendered
- **Actions**:
  - Wait for map idle
  - Query `mapLibreAdapter.getVisibleRegion()`
  - Query `mapLibreAdapter.getCameraPosition()`
  - Query `mapLibreAdapter.getZoomLevel()`
- **Assertions**:
  - Visible region contains camera center position
  - Visible region bounds calculated correctly for zoom level
  - Region bounds are valid (NE > SW, positive dimensions)
  - **Event Detail/Wave**: Visible region contains entire event bounds (BOUNDS mode)
  - **Full Map**: Visible region within event bounds (WINDOW mode)

**Platform-Specific Verification**:
- **Android**: `mapView.projection.visibleRegion.latLngBounds`
- **iOS**: `mapView.visibleCoordinateBounds` (converted from MLNCoordinateBounds)

#### 5.5.2 Visible Region After Pan

**Test**: `test_fullMap_visibleRegionUpdatesAfterPan`
- **Preconditions**:
  - Map loaded (Full Map screen only - gestures enabled)
  - Initial visible region recorded
- **Actions**:
  - Perform pan gesture (north)
  - Wait for map idle
  - Query `mapLibreAdapter.getVisibleRegion()` → regionAfterPan
  - Perform pan gesture (east)
  - Wait for map idle
  - Query `mapLibreAdapter.getVisibleRegion()` → regionAfterPanEast
- **Assertions**:
  - regionAfterPan different from initial region
  - regionAfterPan.northLatitude > initialRegion.northLatitude
  - regionAfterPan completely within event bounds
  - regionAfterPanEast different from regionAfterPan
  - regionAfterPanEast.eastLongitude > regionAfterPan.eastLongitude
  - regionAfterPanEast completely within event bounds

#### 5.5.3 Visible Region After Zoom

**Test**: `test_fullMap_visibleRegionUpdatesAfterZoom`
- **Preconditions**:
  - Map loaded (Full Map screen only)
  - Initial visible region recorded
  - Initial region dimensions calculated (height, width)
- **Actions**:
  - Perform zoom-in gesture
  - Wait for map idle
  - Query `mapLibreAdapter.getVisibleRegion()` → regionAfterZoomIn
  - Calculate regionAfterZoomIn dimensions
  - Perform zoom-out gesture (back to initial zoom)
  - Wait for map idle
  - Query `mapLibreAdapter.getVisibleRegion()` → regionAfterZoomOut
  - Calculate regionAfterZoomOut dimensions
- **Assertions**:
  - regionAfterZoomIn dimensions < initial dimensions (smaller viewport)
  - regionAfterZoomIn completely within event bounds
  - regionAfterZoomOut dimensions ≈ initial dimensions (tolerance ±5%)
  - regionAfterZoomOut completely within event bounds

#### 5.5.4 Visible Region Matches Calculated Viewport

**Test**: `test_{screenType}_visibleRegionMatchesCalculatedViewport`
- **Preconditions**:
  - Map loaded
  - Camera position known
  - Zoom level known
  - Screen dimensions known
- **Actions**:
  - Calculate expected viewport from camera + zoom + screen dimensions:
    ```kotlin
    val metersPerPixel = 156543.03392 * cos(cameraLat) / (2 ^ zoom)
    val viewportHeight = screenHeight * metersPerPixel / 111320.0 // degrees
    val viewportWidth = screenWidth * metersPerPixel / (111320.0 * cos(cameraLat))
    ```
  - Query actual visible region from MapLibre
  - Compare calculated vs actual
- **Assertions**:
  - Actual region center ≈ calculated center (tolerance ±0.0001°)
  - Actual region dimensions ≈ calculated dimensions (tolerance ±5%)
  - Actual region completely within event bounds

#### 5.5.5 Visible Region Consistency Across Platform APIs

**Test**: `test_{screenType}_visibleRegionConsistentWithCameraAndZoom`
- **Preconditions**:
  - Map loaded
- **Actions**:
  - Query `mapLibreAdapter.getVisibleRegion()` → visibleRegion
  - Query `mapLibreAdapter.getCameraPosition()` → cameraPosition
  - Query `mapLibreAdapter.currentZoom.value` → zoomLevel
  - Calculate region center from visibleRegion
- **Assertions**:
  - visibleRegion center ≈ cameraPosition (tolerance ±0.0001°)
  - visibleRegion dimensions inversely proportional to zoom level
  - Higher zoom → smaller visible region
  - Lower zoom → larger visible region

#### 5.5.6 Visible Region During Animation

**Test**: `test_fullMap_visibleRegionUpdatesSmoothllyDuringAnimation`
- **Preconditions**:
  - Map loaded
  - Event status = RUNNING
  - Wave started
  - User in area
- **Actions**:
  - Start animation (tap "Target Wave" button)
  - Sample visible region 5 times during animation (200ms intervals)
  - Wait for animation complete
  - Query final visible region
- **Assertions**:
  - Each sample shows progression toward target
  - All sampled regions completely within event bounds
  - Final region centered on wave position (tolerance ±0.001°)
  - Final region zoom = TARGET_WAVE_ZOOM (tolerance ±0.1)

#### 5.5.7 Visible Region at Boundary Constraints

**Test**: `test_fullMap_visibleRegionStaysWithinBoundsAtEdges`
- **Preconditions**:
  - Map loaded (Full Map screen)
  - Zoom level at min zoom
- **Actions**:
  - Pan to north edge (until blocked)
  - Wait for map idle
  - Query visible region → northEdgeRegion
  - Pan to south edge
  - Wait for map idle
  - Query visible region → southEdgeRegion
  - Pan to east edge
  - Wait for map idle
  - Query visible region → eastEdgeRegion
  - Pan to west edge
  - Wait for map idle
  - Query visible region → westEdgeRegion
- **Assertions**:
  - northEdgeRegion.northLatitude ≤ event.bounds.northLatitude + 0.0001° (tolerance)
  - northEdgeRegion completely within event bounds
  - southEdgeRegion.southLatitude ≥ event.bounds.southLatitude - 0.0001°
  - southEdgeRegion completely within event bounds
  - eastEdgeRegion.eastLongitude ≤ event.bounds.eastLongitude + 0.0001°
  - eastEdgeRegion completely within event bounds
  - westEdgeRegion.westLongitude ≥ event.bounds.westLongitude - 0.0001°
  - westEdgeRegion completely within event bounds

#### 5.5.8 Visible Region on Device Rotation

**Test**: `test_fullMap_visibleRegionRecalculatesOnRotation`
- **Preconditions**:
  - Map loaded (portrait orientation)
  - Initial visible region recorded
  - Screen dimensions recorded
- **Actions**:
  - Rotate device to landscape
  - Wait for dimension change recalculation
  - Wait for map idle
  - Query visible region → landscapeRegion
  - Calculate aspect ratios (portrait vs landscape)
- **Assertions**:
  - landscapeRegion dimensions different from portrait region
  - landscapeRegion aspect ratio ≈ landscape screen aspect ratio
  - landscapeRegion completely within event bounds
  - Camera still centered on event (tolerance ±0.01°)

#### 5.5.9 Visible Region Validation - Invalid Cases

**Test**: `test_{screenType}_visibleRegionNeverInvalid`
- **Preconditions**:
  - Map loaded
- **Actions**:
  - Perform 10 random valid gestures (pan/zoom combinations)
  - After each gesture:
    - Wait for map idle
    - Query visible region
    - Validate region
- **Assertions**:
  - All regions have NE > SW (no inverted bounds)
  - All regions have positive dimensions (width > 0, height > 0)
  - All regions completely within event bounds
  - No region has absurd dimensions (>10° in any direction)

#### 5.5.10 Visible Region Performance

**Test**: `test_{screenType}_visibleRegionQueryPerformance`
- **Preconditions**:
  - Map loaded
- **Actions**:
  - Record start time
  - Query `mapLibreAdapter.getVisibleRegion()` 100 times
  - Record end time
  - Calculate average query time
- **Assertions**:
  - Average query time < 5ms (fast enough for real-time updates)
  - No memory leaks (repeat test 10 times, memory stable)
  - All queries return valid regions

**Platform-Specific Notes**:
- **Android**: Direct native call, should be <1ms
- **iOS**: Goes through MapWrapperRegistry, may be slightly slower (2-3ms acceptable)

---

## 6. Test Data & Fixtures

### 6.1 Event Configurations

#### 6.1.1 Standard Event (Square)
```kotlin
val STANDARD_EVENT = createTestEvent(
    id = "test-standard",
    bounds = BoundingBox(
        sw = Position(48.8566, 2.3522),  // Paris center
        ne = Position(48.8666, 2.3622)   // ~1km x 1km
    ),
    maxZoom = 18.0
)
```

#### 6.1.2 Wide Event (Landscape)
```kotlin
val WIDE_EVENT = createTestEvent(
    id = "test-wide",
    bounds = BoundingBox(
        sw = Position(48.8566, 2.3422),
        ne = Position(48.8616, 2.3722)   // 2:1 aspect ratio (wide)
    ),
    maxZoom = 18.0
)
```

#### 6.1.3 Tall Event (Portrait)
```kotlin
val TALL_EVENT = createTestEvent(
    id = "test-tall",
    bounds = BoundingBox(
        sw = Position(48.8466, 2.3522),
        ne = Position(48.8666, 2.3572)   // 1:2 aspect ratio (tall)
    ),
    maxZoom = 18.0
)
```

### 6.2 User Positions

```kotlin
val USER_INSIDE_EVENT = Position(48.8616, 2.3572)  // Center of standard event
val USER_AT_NORTH_EDGE = Position(48.8665, 2.3572)
val USER_AT_SOUTH_EDGE = Position(48.8567, 2.3572)
val USER_AT_EAST_EDGE = Position(48.8616, 2.3621)
val USER_AT_WEST_EDGE = Position(48.8616, 2.3523)
val USER_OUTSIDE_EVENT = Position(48.8700, 2.3700)
```

### 6.3 Screen Dimensions

```kotlin
val PORTRAIT_PHONE = Dimensions(width = 1080, height = 1920)  // 9:16
val LANDSCAPE_PHONE = Dimensions(width = 1920, height = 1080) // 16:9
val PORTRAIT_TABLET = Dimensions(width = 1536, height = 2048) // 3:4
val SQUARE_SCREEN = Dimensions(width = 1080, height = 1080)   // 1:1
```

---

## 7. Implementation Recommendations

### 7.1 Test Execution Strategy

We'll use a **three-tier testing approach** to maximize coverage while minimizing flakiness and execution time:

#### 7.1.1 Unit Tests (Pure Logic - No MapLibre)
Test core logic in complete isolation without any MapLibre dependencies:

**Target**: MapBoundsEnforcer, constraint calculations, viewport math
**Location**: `shared/src/commonTest/` (platform-independent)
**Dependencies**: None (pure Kotlin)
**Execution**: Fast (<1 second), deterministic, no flakiness

**Examples**:
- Constraint bounds calculation (padding, shrinking)
- Min zoom calculation from bounds + viewport
- Viewport center clamping logic
- Bounds validation (inverted, invalid)

#### 7.1.2 Instrumented Integration Tests (MapLibre Without UI)
**RECOMMENDED APPROACH**: Test MapLibre components **without full UI simulators**.

This is the sweet spot for map testing:
- ✅ **Real MapLibre instances** - test actual map behavior
- ✅ **No UI rendering** - headless map view (faster, more reliable)
- ✅ **Direct API access** - programmatic camera control, no gesture simulation
- ✅ **Real visible region queries** - validate actual MapLibre calculations
- ✅ **Platform-specific** - Android and iOS separate implementations

**Android Implementation** (`androidInstrumentedTest/`):
```kotlin
@RunWith(AndroidJUnit4::class)
class MapLibreIntegrationTest {
    private lateinit var mapView: MapView
    private lateinit var mapLibreAdapter: AndroidMapLibreAdapter

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // Create MapView WITHOUT adding to activity (headless)
        mapView = MapView(context)
        mapView.measure(
            View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY)
        )
        mapView.layout(0, 0, 1080, 1920)

        mapLibreAdapter = AndroidMapLibreAdapter(event.id)

        // Load map style and wait for ready
        val latch = CountDownLatch(1)
        mapView.getMapAsync { map ->
            mapLibreAdapter.setMap(map)
            mapLibreAdapter.setStyle(stylePath) { latch.countDown() }
        }
        latch.await(5, TimeUnit.SECONDS)
    }

    @Test
    fun testVisibleRegionMatchesCameraPosition() {
        // Set camera programmatically (no gestures needed)
        mapLibreAdapter.animateCamera(
            Position(48.8566, 2.3522),
            zoom = 15.0
        )

        // Wait for animation
        Thread.sleep(500)

        // Query real visible region from MapLibre
        val visibleRegion = mapLibreAdapter.getVisibleRegion()
        val cameraPosition = mapLibreAdapter.getCameraPosition()

        // Validate using real MapLibre calculations
        assertNotNull(visibleRegion)
        assertNotNull(cameraPosition)

        // Camera should be in center of visible region
        val regionCenter = visibleRegion.center
        assertEquals(cameraPosition.latitude, regionCenter.latitude, 0.0001)
        assertEquals(cameraPosition.longitude, regionCenter.longitude, 0.0001)
    }

    @Test
    fun testBoundaryConstraintsEnforced() {
        val eventBounds = event.area.bbox()

        // Apply constraints programmatically
        mapLibreAdapter.setBoundsForCameraTarget(
            constraintBounds = eventBounds,
            applyZoomSafetyMargin = true,
            originalEventBounds = eventBounds
        )

        // Try to move camera outside bounds
        mapLibreAdapter.animateCamera(
            Position(
                eventBounds.northLatitude + 1.0, // Way outside
                eventBounds.eastLongitude + 1.0
            ),
            zoom = 10.0
        )

        Thread.sleep(500)

        // Query actual position from MapLibre
        val actualPosition = mapLibreAdapter.getCameraPosition()
        val visibleRegion = mapLibreAdapter.getVisibleRegion()

        // MapLibre should have clamped the camera
        assertTrue(eventBounds.contains(actualPosition))
        assertTrue(visibleRegion.isCompletelyWithin(eventBounds))
    }
}
```

**iOS Implementation** (`iosTest/`):
```kotlin
@Test
fun testMapLibreVisibleRegionIos() = runBlocking {
    // Create headless MLNMapView
    val mapView = MLNMapView(frame = CGRectMake(0.0, 0.0, 375.0, 667.0))

    val adapter = IosMapLibreAdapter(event.id)
    adapter.setMap(mapView)

    // Load style and wait
    val styleLoaded = CompletableDeferred<Unit>()
    adapter.setStyle(stylePath) { styleLoaded.complete(Unit) }
    styleLoaded.await()

    // Set camera programmatically
    adapter.animateCamera(Position(48.8566, 2.3522), zoom = 15.0)
    delay(500)

    // Query visible region through MapWrapperRegistry
    val visibleRegion = MapWrapperRegistry.getVisibleRegion(event.id)
    assertNotNull(visibleRegion)

    // Validate bounds
    assertTrue(visibleRegion.isValid())
}
```

**Key Benefits**:
- **No Emulator/Simulator UI** - tests run headless (faster)
- **No Gesture Simulation** - programmatic camera control (more reliable)
- **Real MapLibre** - actual map calculations, not mocks
- **Direct Validation** - query visible region, camera, zoom directly
- **Parallel Execution** - multiple tests can run simultaneously

#### 7.1.3 Full E2E UI Tests (Complete User Flows)
Test complete user interactions with UI rendering:

**When to Use**:
- Button tap interactions (Target Wave, Target User)
- Gesture validation (pan/zoom blocked on Event Detail/Wave screens)
- Screen navigation flows
- Visual regression testing

**When NOT to Use**:
- MapLibre API validation (use integration tests instead)
- Boundary constraint logic (use unit tests instead)
- Performance testing (use integration tests instead)

**Platform**:
- **Android**: Espresso UI tests on emulator
- **iOS**: XCUITest on simulator

**Examples**:
- User taps "Target Wave" button → camera moves to wave
- User tries to pan on Event Detail screen → gesture blocked
- User navigates from Full Map → Wave screen → map state preserved

#### 7.1.4 Test Distribution Recommendation

```
Total Tests: 56+
├── Unit Tests (30%) - ~17 tests
│   ├─ MapBoundsEnforcer logic
│   ├─ Viewport calculations
│   └─ Constraint math
│
├── Instrumented Integration (60%) - ~34 tests  ⭐ PRIMARY FOCUS
│   ├─ All visible region tests (10 tests)
│   ├─ Boundary enforcement (8 tests)
│   ├─ Camera positioning (8 tests)
│   ├─ Zoom constraints (5 tests)
│   └─ Auto-tracking logic (3 tests)
│
└── E2E UI Tests (10%) - ~5 tests
    ├─ Button interactions (2 tests)
    ├─ Gesture blocking (2 tests)
    └─ Screen navigation (1 test)
```

**Rationale**:
- **Integration tests** provide best ROI: real MapLibre behavior without UI flakiness
- **Unit tests** validate core logic cheaply
- **E2E tests** only for user interaction flows that can't be tested otherwise

### 7.2 Test Isolation

- **Mock Dependencies**: PositionManager, EventObserver, MapViewModel
- **Test Coroutines**: Use TestCoroutineScheduler for time-based tests
- **Deterministic Events**: Use fixed event configurations (no random data)

### 7.3 Flakiness Prevention

- **Explicit Waits**: Wait for map load, camera animations, style load
- **Tolerance Bounds**: Use tolerance for floating-point comparisons (±0.0001° for coords, ±0.1 for zoom)
- **Retry Logic**: Retry assertion checks with timeout for asynchronous updates
- **Idempotent Tests**: Each test starts with clean state (no side effects)

### 7.4 Platform-Specific Considerations

**Android**:
- Use Espresso idling resources for map animations
- Test on emulator with software rendering (avoid GPU differences)
- Use ActivityScenarioRule for activity lifecycle management

**iOS**:
- Use XCTWaiter for animation completion
- Test on simulator (avoid device hardware variations)
- Use XCUIApplication for full UI testing

---

## 8. Success Criteria

### 8.1 Coverage Goals
- **Event Detail Screen**: 100% feature coverage (6 tests minimum)
- **Wave Participation Screen**: 100% feature coverage (7 tests minimum)
- **Full Map Screen**: 100% feature coverage (30+ tests minimum)
- **Cross-Cutting**: All screens (3 tests minimum)
- **MapLibre Visible Region**: All screens (10 tests minimum)
- **Total Test Count**: 56+ tests across both platforms

### 8.2 Quality Metrics
- **Pass Rate**: 100% on both Android and iOS
- **Flakiness**: <1% (no more than 1 failure per 100 runs)
- **Execution Time**: <10 minutes per platform (includes visible region validation)
- **Code Coverage**: >90% of map-related code
- **MapLibre Integration**: All visible region queries return valid, consistent results

### 8.3 Acceptance Criteria
- ✅ All boundary constraint tests pass (no pixels outside event area)
- ✅ All gesture blocking tests pass (gestures work/blocked as expected)
- ✅ All button tests pass (visibility, enablement, functionality)
- ✅ All auto-tracking tests pass (camera movements correct)
- ✅ All aspect ratio tests pass (WINDOW mode fits correctly)
- ✅ All visible region tests pass (MapLibre returns accurate viewport bounds)
- ✅ Visible region consistency verified across camera, zoom, and bounds APIs
- ✅ Visible region validation during animations, gestures, and dimension changes
- ✅ Platform parity verified (Android and iOS behave identically)

---

## 9. Future Enhancements

### 9.1 Performance Testing
- Measure camera animation smoothness (FPS)
- Measure gesture response time
- Measure map tile loading performance

### 9.2 Accessibility Testing
- Screen reader navigation (TalkBack/VoiceOver)
- Button accessibility labels
- Map region announcements

### 9.3 Visual Regression Testing
- Screenshot comparison for map rendering
- Visual diff for polygon styles
- Camera position visualization

---

**Document Version**: 1.0
**Last Updated**: October 2025
**Authors**: WorldWideWaves Development Team
**Review Status**: Ready for Review
