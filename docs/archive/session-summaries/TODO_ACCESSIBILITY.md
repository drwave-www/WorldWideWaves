# WorldWideWaves - Comprehensive Accessibility Implementation TODO

> **PROMPT FOR NEXT CLAUDE SESSION**
>
> Implement complete accessibility support for Android and iOS to achieve WCAG 2.1 Level AA compliance and platform-specific accessibility standards.

---

## 🚀 Quick Start Prompt

```
Implement comprehensive accessibility for WorldWideWaves Android and iOS apps.

⏱️ TOTAL EFFORT: 120-155 hours (3-4 weeks full-time)

Time Breakdown:
- Phase 1: Critical Android fixes → 40-50 hours (1 week)
- Phase 2: iOS accessibility implementation → 50-60 hours (2 weeks)
- Phase 3: Testing & verification → 20-30 hours (1 week)
- Phase 4: Documentation & maintenance → 10-15 hours (integrated)

With Agents (Parallel): Could reduce to 100-120 hours
Part-time (50%): 6-8 weeks
Part-time (25%): 12-16 weeks (3-4 months)

Current Status:
- ✅ Excellent accessibility TEST infrastructure (2,735+ lines, 34 tests)
- ❌ Minimal PRODUCTION implementation (20 contentDescriptions, 0 semantics blocks)
- Gap: Tests use mock UI, actual app needs accessibility features

Critical Issues to Fix:
1. 0 semantic blocks in production (Android/iOS)
2. Missing iOS Dynamic Type support (16h)
3. No iOS VoiceOver announcements for wave timing (6h)
4. Touch target violations - 4 components below minimum (6h)
5. Color contrast failures - primary green 2.8:1 (needs 4.5:1) (3h)
6. iOS map has zero accessibility (12h)

**Agent Orchestration Strategy**:

Phase 1 (Android Critical - Parallel with 3 agents):
- Agent 1: Add semantics to EventsScreen, ButtonWave, SimulationButton (12h)
- Agent 2: Fix touch targets + color contrast (8-9h)
- Agent 3: Complete contentDescription coverage + live regions (14h)
Total wall time: ~14 hours (vs 40-50 sequential)

Phase 2 (iOS - Parallel with 3 agents):
- Agent 1: Implement Dynamic Type scaling (16h)
- Agent 2: VoiceOver announcements + haptic feedback (12h)
- Agent 3: Map accessibility implementation (12h)
Total wall time: ~16 hours (vs 50-60 sequential)

Phase 3 (Testing - Sequential, manual required):
- Agent 1: Connect production UI to tests (12h)
- Manual: TalkBack testing (4h)
- Manual: VoiceOver testing (4h)
- Agent 2: Automated scanning integration (4h)
Total wall time: ~20 hours (manual testing cannot be parallelized)

**With optimal agent usage: 50-60 hours wall time vs 120-155 hours sequential**
(60-65% time savings through parallelization)

**Execution Plan**:
1. Deploy 3 agents simultaneously per phase
2. Each agent works on independent file sets
3. Verify compilation after each agent completes
4. Run tests before moving to next phase
5. Manual testing only after automated fixes complete

Stay on current branch: optimization/phase-3-large-files
Verify with TalkBack/VoiceOver after each phase.
Commit after each phase completion.

All details, priorities, code examples, and implementation guides below.
```

---

## 📊 Current Accessibility Status

### Android App: 4.5/10 (NEEDS SIGNIFICANT IMPROVEMENT)
- ❌ 0 semantic blocks implemented
- ⚠️ 20 contentDescription attributes (partial coverage)
- ❌ 0 role assignments
- ⚠️ Touch targets: Some below 48dp
- ✅ Text scaling: Proper .sp units (100%)
- ⚠️ Color contrast: Needs verification
- ❌ Focus management: Not implemented
- ❌ Live regions: Not implemented

### iOS App: 3/10 (CRITICAL GAPS)
- ⚠️ VoiceOver: Relies on Compose auto-bridging (unverified)
- ❌ Dynamic Type: Zero implementation
- ❌ Haptic feedback: Missing
- ❌ Map accessibility: Zero configuration
- ❌ Testing: 0 iOS accessibility tests
- ⚠️ Toast announcements: Visual only

### Test Coverage: 9/10 (EXCELLENT BUT NOT USED)
- ✅ 2,735+ lines of accessibility test code
- ✅ 34 comprehensive tests
- ✅ Covers all WCAG 2.1 categories
- ❌ Tests mock UI instead of production components

---

## 🔴 CRITICAL ISSUES (Must Fix Before Launch)

### Issue 1: Missing Semantic Properties (Android)
**Impact**: Screen readers cannot properly announce UI elements
**Affected**: All interactive components
**WCAG**: 4.1.2 (Name, Role, Value)

**Files Needing Immediate Attention**:
1. `shared/src/commonMain/kotlin/com/worldwidewaves/shared/ui/screens/EventsScreen.kt` - 0 semantics blocks
2. `shared/src/commonMain/kotlin/com/worldwidewaves/shared/ui/components/shared/ButtonWave.kt` - 0 semantics blocks
3. `shared/src/commonMain/kotlin/com/worldwidewaves/shared/ui/components/shared/SimulationButton.kt` - 0 semantics blocks

**Effort**: 8-12 hours

---

### Issue 2: Touch Target Violations (Both Platforms)
**Impact**: Users with motor impairments cannot reliably tap elements
**WCAG**: 2.5.5 (Target Size - Minimum 48dp/44pt)

**Violations**:
| Component | Size | Location | Fix |
|-----------|------|----------|-----|
| SimulationButton icon | 24dp | SimulationButton.kt:112 | Wrap in 48dp Box |
| Back button icon | 20dp | BaseEventBackgroundScreen.kt:170 | Increase to 48dp |
| Favorite image | 36dp | EventsScreen.kt:541 | Increase to 48dp |
| Map downloaded icon | 36dp | EventsScreen.kt:480 | Increase to 48dp |

**Effort**: 4-6 hours

---

### Issue 3: Color Contrast Failures (Both Platforms)
**Impact**: Users with low vision cannot read text
**WCAG**: 1.4.3 (Contrast Minimum - 4.5:1 for text)

**Violations**:
| Color Pair | Ratio | Required | Location |
|------------|-------|----------|----------|
| Primary Green (#5DB075) on White | 2.8:1 | 4.5:1 | Button text |
| Gray (#BDBDBD) on Light Gray | 1.9:1 | 4.5:1 | Quaternary text |

**Fix**: Darken primary green to #3D8F58 (4.5:1 ratio)

**Effort**: 2-3 hours

---

### Issue 4: iOS Dynamic Type Not Supported
**Impact**: iOS users with vision impairments cannot scale text
**Platform**: iOS only (Android handles automatically)

**Current**: Fixed .sp sizes, no Dynamic Type scaling
**Needed**: UIFontMetrics integration for Compose text

**Effort**: 12-16 hours

---

### Issue 5: iOS VoiceOver Announcements Missing
**Impact**: Critical timing announcements don't reach VoiceOver users
**Affected**: Real-time wave coordination (core app feature)

**Missing**:
- Wave timing announcements ("Wave starting in 5 seconds")
- Wave hit confirmation
- State transitions (warming → waiting → hit)

**Fix Needed in IosPlatformEnabler.swift**:
```swift
func announceForAccessibility(_ message: String) {
    UIAccessibility.post(notification: .announcement, argument: message)
}
```

**Effort**: 4-6 hours

---

### Issue 6: iOS Map Accessibility Zero Implementation
**Impact**: VoiceOver users cannot understand map content
**Affected**: EventMapView.swift

**Missing**:
- Event area boundary labels
- User position marker label
- Wave progression circles
- Alternative map navigation

**Effort**: 8-12 hours

---

## 🟠 HIGH PRIORITY (Fix in Sprint 1-2)

### Issue 7: Incomplete contentDescription Coverage
**Current**: 20 instances
**Needed**: 50+ (all images, icons, interactive elements)

**Missing**:
- All icons without descriptions
- Decorative images not marked
- Hardcoded English descriptions (not localized)

**Effort**: 6-8 hours

---

### Issue 8: No State Descriptions
**Impact**: Users don't know selection/toggle states

**Affected Components**:
- Filter buttons (All/Favorites/Downloaded)
- Favorite toggle icon
- Simulation button states

**Fix Example**:
```kotlin
Modifier.semantics {
    role = Role.Tab
    selected = isSelected
    stateDescription = if (isSelected) "Selected" else "Not selected"
}
```

**Effort**: 4-6 hours

---

### Issue 9: No Live Regions for Dynamic Content
**Impact**: Loading/progress changes not announced

**Affected**:
- LoadingIndicator
- DownloadProgressIndicator
- Wave progression updates

**Effort**: 4-6 hours

---

### Issue 10: No Role Assignments
**Current**: 0 role assignments
**Needed**: 25+ interactive elements

**Fix**: Add `role = Role.Button/Tab/Checkbox` to all clickable elements

**Effort**: 4-6 hours

---

## 🟡 MEDIUM PRIORITY (Sprint 3)

### Issue 11: No Focus Indicators
**WCAG**: 2.4.7 (Focus Visible)

**Fix**: Add focus indicators for keyboard/D-pad navigation

**Effort**: 6-8 hours

---

### Issue 12: No Heading Hierarchy
**WCAG**: 1.3.1 (Info and Relationships)

**Fix**: Add `semantics { heading = true }` to screen titles and section headers

**Effort**: 2-3 hours

---

### Issue 13: No Reduced Motion Support
**WCAG**: 2.3.3 (Animation from Interactions)

**Fix**: Check system reduced motion setting, disable blinking animations

**Effort**: 3-4 hours

---

### Issue 14: iOS Gesture Conflicts
**Platform**: iOS only

**Fix**: Configure VoiceOver gesture passthrough for map

**Effort**: 4-6 hours

---

## 📋 Implementation Plan

### Phase 1: Critical Android Fixes (40-50 hours, 1 week)

#### 1.1 Add Semantic Properties to All Interactive Elements (12 hours)

**File**: `shared/src/commonMain/kotlin/com/worldwidewaves/shared/ui/screens/EventsScreen.kt`

**Tasks**:
- [ ] Add semantics to FavoritesSelector (3 filter buttons)
  ```kotlin
  Modifier
      .clickable { onSelectFilter(filter) }
      .semantics {
          role = Role.Tab
          selected = isSelected
          stateDescription = if (isSelected) "Selected" else "Not selected"
          contentDescription = filterLabel
      }
  ```

- [ ] Add semantics to Event card (clickable)
  ```kotlin
  Modifier
      .clickable { onEventClick(event.id) }
      .semantics {
          role = Role.Button
          contentDescription = "${event.city} event on ${event.date}"
      }
  ```

- [ ] Add semantics to Favorite toggle
  ```kotlin
  Modifier
      .clickable { onFavoriteToggle() }
      .semantics {
          role = Role.Checkbox
          toggleableState = if (isFavorite) ToggleableState.On else ToggleableState.Off
          stateDescription = if (isFavorite) "Favorited" else "Not favorited"
      }
  ```

**File**: `shared/src/commonMain/kotlin/com/worldwidewaves/shared/ui/components/shared/ButtonWave.kt`

**Tasks**:
- [ ] Add semantics to wave button
  ```kotlin
  Modifier.semantics {
      role = Role.Button
      contentDescription = buttonText
      stateDescription = if (isEnabled) "Enabled" else "Disabled - ${reason}"
  }
  ```

**File**: `shared/src/commonMain/kotlin/com/worldwidewaves/shared/ui/components/shared/SimulationButton.kt`

**Tasks**:
- [ ] Add semantics for simulation states
  ```kotlin
  Modifier.semantics {
      role = Role.Button
      contentDescription = when(state) {
          "idle" -> "Start simulation"
          "loading" -> "Simulation loading"
          "active" -> "Stop simulation"
      }
      stateDescription = state
  }
  ```

**Verification**:
- Run: `./gradlew :composeApp:connectedDebugAndroidTest --tests "*AccessibilityTest*"`
- Test with TalkBack enabled

---

#### 1.2 Fix Touch Target Violations (6 hours)

**Tasks**:
- [ ] SimulationButton: Ensure 48dp minimum
- [ ] Back button: Increase from 20dp to 48dp
- [ ] Favorite icon: Increase from 36dp to 48dp
- [ ] Downloaded icon: Increase from 36dp to 48dp

**Pattern**:
```kotlin
Box(
    modifier = Modifier
        .size(48.dp)  // Minimum touch target
        .clickable { /* action */ }
        .semantics { role = Role.Button }
) {
    Icon(
        modifier = Modifier.size(24.dp),  // Visual size can be smaller
        imageVector = icon
    )
}
```

**Verification**:
- Use Android Accessibility Scanner app
- Verify 48dp minimum in tests

---

#### 1.3 Fix Color Contrast (3 hours)

**File**: `shared/src/commonMain/kotlin/com/worldwidewaves/shared/ui/theme/Colors.kt`

**Tasks**:
- [ ] Change primary green: #5DB075 → #3D8F58 (4.5:1 ratio)
- [ ] Review quaternary colors: Ensure 4.5:1 minimum
- [ ] Test all color combinations with contrast checker

**Verification**:
- Use WebAIM Contrast Checker
- Test with high contrast mode enabled

---

#### 1.4 Complete contentDescription Coverage (8 hours)

**Tasks**:
- [ ] Audit all Image(), Icon() components
- [ ] Add contentDescription to all missing (30+ locations)
- [ ] Localize hardcoded descriptions
- [ ] Mark decorative images: `contentDescription = null`

**Pattern**:
```kotlin
// Meaningful image
Icon(
    imageVector = Icons.Default.Favorite,
    contentDescription = stringResource(MokoRes.strings.add_to_favorites)  // Localized
)

// Decorative image
Image(
    painter = painterResource(Res.drawable.background),
    contentDescription = null  // Explicitly decorative
)
```

---

#### 1.5 Implement Live Regions (6 hours)

**File**: `shared/src/commonMain/kotlin/com/worldwidewaves/shared/ui/components/Indicators.kt`

**Tasks**:
- [ ] Add live region to LoadingIndicator
  ```kotlin
  Modifier.semantics {
      liveRegion = LiveRegionMode.Polite
      contentDescription = message
  }
  ```

- [ ] Add progress announcements to DownloadProgressIndicator
  ```kotlin
  LaunchedEffect(progress) {
      if (progress % 25 == 0) {  // Announce at 0%, 25%, 50%, 75%, 100%
          // Screen reader will announce automatically with liveRegion
      }
  }
  ```

- [ ] Add live region to wave progression updates

**Verification**:
- Test with TalkBack, verify announcements at milestones

---

#### 1.6 Add Heading Hierarchy (3 hours)

**Tasks**:
- [ ] Mark screen titles as headings
  ```kotlin
  Text(
      text = screenTitle,
      modifier = Modifier.semantics { heading = true }
  )
  ```

- [ ] Add to: EventsScreen, WaveParticipationScreen, AboutScreen, EventDetailScreen
- [ ] Verify heading navigation with TalkBack

---

#### 1.7 Implement Focus Management (8 hours)

**Tasks**:
- [ ] Add focus indicators to interactive elements
  ```kotlin
  var isFocused by remember { mutableStateOf(false) }

  Modifier
      .focusable()
      .onFocusChanged { isFocused = it.isFocused }
      .border(
          width = if (isFocused) 2.dp else 0.dp,
          color = MaterialTheme.colorScheme.primary
      )
  ```

- [ ] Test keyboard/D-pad navigation
- [ ] Verify focus order logical

---

### Phase 2: iOS Accessibility Implementation (50-60 hours, 2 weeks)

#### 2.1 Implement Dynamic Type Support (16 hours)

**File**: Create `shared/src/iosMain/kotlin/com/worldwidewaves/shared/ui/theme/DynamicTypeScale.kt`

**Tasks**:
- [ ] Create Dynamic Type scaling system
  ```kotlin
  @Composable
  actual fun rememberDynamicTypeScale(): Float {
      val category = UIApplication.sharedApplication.preferredContentSizeCategory
      return when(category) {
          UIContentSizeCategoryExtraSmall -> 0.8f
          UIContentSizeCategorySmall -> 0.9f
          UIContentSizeCategoryMedium -> 1.0f
          UIContentSizeCategoryLarge -> 1.1f
          UIContentSizeCategoryExtraLarge -> 1.2f
          UIContentSizeCategoryExtraExtraLarge -> 1.3f
          UIContentSizeCategoryExtraExtraExtraLarge -> 1.4f
          UIContentSizeCategoryAccessibilityMedium -> 1.6f
          UIContentSizeCategoryAccessibilityLarge -> 1.9f
          UIContentSizeCategoryAccessibilityExtraLarge -> 2.2f
          UIContentSizeCategoryAccessibilityExtraExtraLarge -> 2.6f
          UIContentSizeCategoryAccessibilityExtraExtraExtraLarge -> 3.0f
          else -> 1.0f
      }
  }
  ```

- [ ] Apply scaling to all text: `fontSize = (baseFontSize.sp * dynamicTypeScale)`
- [ ] Test at all 12 iOS text size settings
- [ ] Ensure no text truncation at 300% scale

**Verification**:
- Settings → Accessibility → Display & Text Size → Larger Text
- Test at maximum size

---

#### 2.2 Add VoiceOver Announcements (6 hours)

**File**: `iosApp/worldwidewaves/IosPlatformEnabler.swift`

**Tasks**:
- [ ] Add announcement method:
  ```swift
  @objc func announceForAccessibility(_ message: String) {
      UIAccessibility.post(notification: .announcement, argument: message)
  }
  ```

- [ ] Expose to Kotlin:
  ```kotlin
  // In shared/src/iosMain/../IosPlatformEnabler.kt
  fun announceForAccessibility(message: String)
  ```

- [ ] Use for wave timing:
  ```kotlin
  when (waveState) {
      WarmingStart -> platform.announceForAccessibility("Wave warming phase starting")
      HitDetected -> platform.announceForAccessibility("Wave hit successful!")
  }
  ```

**Verification**:
- Test with VoiceOver enabled
- Verify all critical announcements work

---

#### 2.3 Implement iOS Map Accessibility (12 hours)

**File**: `iosApp/worldwidewaves/MapLibre/EventMapView.swift`

**Tasks**:
- [ ] Configure map container accessibility:
  ```swift
  mapView.isAccessibilityElement = false  // Container of elements
  mapView.accessibilityNavigationStyle = .combined
  ```

- [ ] Add accessible overlay views for map features:
  ```swift
  // User position marker
  let userMarkerAccessibilityElement = UIAccessibilityElement(accessibilityContainer: mapView)
  userMarkerAccessibilityElement.accessibilityLabel = "Your current position"
  userMarkerAccessibilityElement.accessibilityTraits = .updatesFrequently
  userMarkerAccessibilityElement.accessibilityFrame = /* marker frame */

  // Event area
  let areaAccessibilityElement = UIAccessibilityElement(accessibilityContainer: mapView)
  areaAccessibilityElement.accessibilityLabel = "Event wave area boundary"
  areaAccessibilityElement.accessibilityHint = "Event covers \(cityName)"

  mapView.accessibilityElements = [userMarkerAccessibilityElement, areaAccessibilityElement]
  ```

- [ ] Add alternative map description:
  ```swift
  let mapDescriptionElement = UIAccessibilityElement(accessibilityContainer: mapView)
  mapDescriptionElement.accessibilityLabel = "Map showing \(cityName) event area. You are currently \(distanceFromCenter)m from center."
  ```

**Verification**:
- Test with VoiceOver
- Verify map elements are announced

---

#### 2.4 Implement Haptic Feedback (6 hours)

**File**: `iosApp/worldwidewaves/IosPlatformEnabler.swift`

**Tasks**:
- [ ] Add haptic feedback methods:
  ```swift
  private let notificationFeedback = UINotificationFeedbackGenerator()
  private let impactFeedback = UIImpactFeedbackGenerator(style: .medium)

  @objc func triggerHapticSuccess() {
      notificationFeedback.notificationOccurred(.success)
  }

  @objc func triggerHapticWarning() {
      notificationFeedback.notificationOccurred(.warning)
  }

  @objc func triggerHapticImpact() {
      impactFeedback.impactOccurred()
  }
  ```

- [ ] Use for wave events:
  - Wave starting: warning haptic
  - Wave hit: success haptic
  - Entering warming zone: impact haptic

**Verification**:
- Test on physical device (haptics don't work on simulator)
- Test with VoiceOver + haptics together

---

#### 2.5 Verify Compose→iOS Semantic Bridging (10 hours)

**Create**: `shared/src/iosTest/kotlin/com/worldwidewaves/shared/accessibility/IosAccessibilityBridgingTest.kt`

**Tasks**:
- [ ] Test contentDescription → accessibilityLabel
- [ ] Test heading() → accessibilityTraits .header
- [ ] Test role → accessibilityTraits mapping
- [ ] Test state descriptions
- [ ] Document what works and what doesn't

**Example Test**:
```kotlin
@Test
fun `Compose contentDescription should map to iOS accessibilityLabel`() {
    val vc = makeMainViewController()
    // Access UIView hierarchy and verify accessibilityLabel is set
    // This requires platform-specific UIKit inspection
}
```

---

#### 2.6 Fix Toast Announcements (2 hours)

**File**: `iosApp/worldwidewaves/IosPlatformEnabler.swift`

**Tasks**:
- [ ] Update toast() to announce:
  ```swift
  func toast(message: String) {
      Self.showToast(message: message, in: hostView)
      UIAccessibility.post(notification: .announcement, argument: message)  // ADD THIS
  }
  ```

---

### Phase 3: Testing & Verification (20-30 hours, 1 week)

#### 3.1 Connect Production Code to Accessibility Tests (12 hours)

**Current Problem**: Tests use mock composables, not actual app screens

**Tasks**:
- [ ] Modify AccessibilityTest.kt to test real screens:
  ```kotlin
  @Test
  fun eventsScreen_accessibility_contentDescriptions() {
      composeTestRule.setContent {
          EventsScreen(viewModel = testViewModel)  // REAL screen
      }

      // Run existing test logic
      validateInteractiveElementsHaveDescriptions()
  }
  ```

- [ ] Update all 27 accessibility tests to use real UI components
- [ ] Fix any accessibility issues found in production code
- [ ] Achieve 100% pass rate

**Verification**:
- All 27 accessibility tests pass with production UI
- No mocked components in tests

---

#### 3.2 Manual TalkBack Testing (Android) (4 hours)

**Test Scenarios**:
1. Navigate entire app with TalkBack only
2. Browse events, add to favorites
3. Join wave and participate
4. Navigate About section
5. Complete full user journey

**Validation Checklist**:
- [ ] All interactive elements announced
- [ ] States clearly described
- [ ] Progress updates announced
- [ ] Navigation logical and efficient
- [ ] No silent UI elements
- [ ] Gestures work with TalkBack

---

#### 3.3 Manual VoiceOver Testing (iOS) (4 hours)

**Test Scenarios**:
1. Navigate entire app with VoiceOver only
2. Test map accessibility
3. Test wave participation with audio cues
4. Test with Dynamic Type at maximum

**Validation Checklist**:
- [ ] All Compose semantics translate correctly
- [ ] Map elements are accessible
- [ ] Wave timing announced
- [ ] Text scales properly
- [ ] Haptics work with VoiceOver

---

#### 3.4 Automated Accessibility Scanning (4 hours)

**Tools to Integrate**:
1. **Google Accessibility Scanner** (manual)
2. **Espresso AccessibilityChecks** (automated)

**Tasks**:
- [ ] Add to BaseAccessibilityTest.kt:
  ```kotlin
  companion object {
      init {
          AccessibilityChecks.enable()
              .setRunChecksFromRootView(true)
              .setSuppressingResultMatcher(
                  allOf(
                      matchesCheckNames(containsString("SpeakableTextPresentCheck")),
                      matchesViews(withId(R.id.decorative_image))
                  )
              )
      }
  }
  ```

- [ ] Run against all screens
- [ ] Fix all scanner violations

---

#### 3.5 Create Accessibility Test Report (3 hours)

**Tasks**:
- [ ] Run full accessibility test suite
- [ ] Generate HTML report with results
- [ ] Document all remaining issues
- [ ] Create accessibility compliance certificate

---

### Phase 4: Documentation & Maintenance (10-15 hours)

#### 4.1 Create Accessibility Guidelines (6 hours)

**File**: `docs/ACCESSIBILITY_GUIDE.md`

**Content**:
- Accessibility requirements checklist
- Code examples for common patterns
- Testing procedures
- Platform-specific considerations

---

#### 4.2 Add Accessibility to PR Checklist (2 hours)

**File**: `.github/pull_request_template.md`

**Checklist**:
- [ ] All interactive elements have contentDescription
- [ ] All custom components have semantic roles
- [ ] Touch targets meet 48dp minimum
- [ ] Color contrast verified (4.5:1)
- [ ] Tested with TalkBack (Android) and VoiceOver (iOS)
- [ ] Dynamic content has live regions

---

#### 4.3 Create Accessibility Testing Scripts (3 hours)

**File**: `scripts/test_accessibility.sh`

```bash
#!/usr/bin/env bash

echo "🔍 Running Accessibility Test Suite"

# Android accessibility tests
./gradlew :composeApp:connectedDebugAndroidTest \
  --tests "*AccessibilityTest*" \
  --tests "*RealAccessibilityIntegrationTest*"

# iOS accessibility tests (when created)
./gradlew :shared:iosSimulatorArm64Test \
  --tests "*IosAccessibilityTest*"

echo "✅ Accessibility tests complete"
```

---

#### 4.4 Update CLAUDE.md (2 hours)

**Add Section**: Accessibility Requirements

```markdown
### Accessibility Requirements

**All UI components must**:
- Have contentDescription for images/icons (localized)
- Use semantics {} blocks for interactive elements
- Meet 48dp/44pt minimum touch targets
- Support text scaling (use .sp units)
- Announce state changes (live regions)
- Work with screen readers (TalkBack/VoiceOver)

**Testing**:
- Run accessibility tests before each PR
- Manual testing with TalkBack (Android) and VoiceOver (iOS)
- Verify color contrast with WCAG AA standards (4.5:1)
```

---

## 📊 Effort Summary

| Phase | Tasks | Hours | Timeline |
|-------|-------|-------|----------|
| **Phase 1: Android Critical** | 7 tasks | 40-50h | Week 1 |
| **Phase 2: iOS Implementation** | 6 tasks | 50-60h | Weeks 2-3 |
| **Phase 3: Testing** | 5 tasks | 20-30h | Week 4 |
| **Phase 4: Documentation** | 4 tasks | 10-15h | Week 4 |
| **TOTAL** | **22 tasks** | **120-155h** | **4 weeks** |

---

## ✅ Success Criteria

### Android
- [ ] All 27 accessibility tests pass with production UI
- [ ] TalkBack navigation complete without manual mode
- [ ] All interactive elements have semantics
- [ ] Touch targets ≥ 48dp
- [ ] Color contrast ≥ 4.5:1
- [ ] Google Accessibility Scanner: 0 violations

### iOS
- [ ] VoiceOver announces all critical events
- [ ] Dynamic Type works at all 12 sizes
- [ ] Map accessible to VoiceOver users
- [ ] Haptic feedback works
- [ ] Compose semantics verified to bridge correctly
- [ ] Wave coordination works with VoiceOver

### Overall
- [ ] WCAG 2.1 Level AA compliance: >90%
- [ ] Both platforms support screen readers fully
- [ ] Real-time coordination accessible
- [ ] Documentation complete
- [ ] CI/CD accessibility gates in place

---

## 🎯 Priority Ranking

### Sprint 1 (Week 1)
1. ✅ Add semantics to all interactive elements (12h) - CRITICAL
2. ✅ Fix touch target violations (6h) - CRITICAL
3. ✅ Fix color contrast (3h) - CRITICAL
4. ✅ Complete contentDescription coverage (8h) - HIGH
5. ✅ Implement live regions (6h) - HIGH

### Sprint 2 (Week 2)
6. ✅ iOS Dynamic Type (16h) - CRITICAL
7. ✅ iOS VoiceOver announcements (6h) - CRITICAL
8. ✅ iOS map accessibility (12h) - CRITICAL
9. ✅ Add heading hierarchy (3h) - HIGH

### Sprint 3 (Week 3)
10. ✅ iOS haptic feedback (6h) - MEDIUM
11. ✅ Verify Compose→iOS bridging (10h) - HIGH
12. ✅ Implement focus management (8h) - MEDIUM
13. ✅ Fix toast announcements (2h) - MEDIUM

### Sprint 4 (Week 4)
14. ✅ Connect tests to production UI (12h) - CRITICAL
15. ✅ Manual TalkBack testing (4h) - CRITICAL
16. ✅ Manual VoiceOver testing (4h) - CRITICAL
17. ✅ Automated scanning (4h) - HIGH
18. ✅ Documentation (10h) - MEDIUM

---

## 📚 Key References

### Existing Test Infrastructure
- `composeApp/src/androidInstrumentedTest/kotlin/com/worldwidewaves/testing/BaseAccessibilityTest.kt` - Base utilities
- `composeApp/src/androidInstrumentedTest/kotlin/com/worldwidewaves/accessibility/AccessibilityTest.kt` - 27 tests
- `composeApp/src/realIntegrationTest/kotlin/com/worldwidewaves/testing/real/RealAccessibilityIntegrationTest.kt` - TalkBack tests

### Standards & Guidelines
- **WCAG 2.1**: https://www.w3.org/WAI/WCAG21/quickref/
- **Android Accessibility**: https://developer.android.com/guide/topics/ui/accessibility
- **iOS Accessibility**: https://developer.apple.com/accessibility/
- **Compose Accessibility**: https://developer.android.com/jetpack/compose/accessibility

### Internal Documentation
- `docs/UI_TESTING_GUIDE.md` - Existing testing patterns
- `CLAUDE.md` - Project conventions (to be updated)

---

## ⚠️ Critical Notes

### Testing Strategy
- **DO NOT** create new test infrastructure (already excellent)
- **DO** implement accessibility in production code to match tests
- **DO** convert mock-based tests to test real UI components
- **VERIFY** Compose semantics work on iOS (critical assumption)

### Real-Time Coordination Accessibility
This is **THE MOST CRITICAL** accessibility requirement:
- Wave timing is core app feature
- Users with disabilities must receive timing cues
- Announcements must be immediate (not delayed)
- Haptics required as fallback for hearing-impaired users
- Test extensively with real users

### Platform Differences
- **Android**: More mature Compose accessibility support
- **iOS**: Requires platform-specific code for full compliance
- **Shared Code**: Benefits both platforms when implemented correctly

---

## 🔧 Quick Commands

```bash
# Run Android accessibility tests
./gradlew :composeApp:connectedDebugAndroidTest --tests "*AccessibilityTest*"

# Run real device TalkBack tests (requires device)
./gradlew :composeApp:connectedDebugAndroidTest --tests "*RealAccessibilityIntegrationTest*"

# Run iOS accessibility tests (to be created)
./gradlew :shared:iosSimulatorArm64Test --tests "*IosAccessibilityTest*"

# Manual testing
# Android: Settings → Accessibility → TalkBack
# iOS: Settings → Accessibility → VoiceOver

# Contrast checker
open https://webaim.org/resources/contrastchecker/
```

---

## 💡 Implementation Tips

### Use Existing Patterns
The test infrastructure shows EXACTLY what's needed:
- Copy semantic patterns from test composables
- Apply to production UI components
- Run tests to verify compliance

### Start Small
1. Fix EventsScreen first (main screen)
2. Verify tests pass
3. Apply same pattern to other screens
4. Iterate until 100% coverage

### Test Continuously
- Run accessibility tests after each change
- Use TalkBack/VoiceOver during development
- Don't wait until the end

### Document Decisions
- Record why certain accessibility choices made
- Document iOS-specific workarounds
- Update CLAUDE.md with accessibility patterns

---

*Created: October 3, 2025*
*Status: Ready for Implementation*
*Total Effort: 120-155 hours (4 weeks)*
*Priority: CRITICAL (required for inclusive app launch)*
