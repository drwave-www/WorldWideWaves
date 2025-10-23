# WorldWideWaves Accessibility Guide

> **WCAG 2.1 Level AA Compliance** | Last Updated: October 2025

## Overview

WorldWideWaves implements comprehensive accessibility features for Android and iOS, ensuring all users can participate in wave coordination events regardless of visual, auditory, motor, or cognitive abilities.

---

## Table of Contents

1. [Accessibility Features](#accessibility-features)
2. [Implementation Checklist](#implementation-checklist)
3. [Code Patterns](#code-patterns)
4. [Testing Requirements](#testing-requirements)
5. [Platform-Specific Guidelines](#platform-specific-guidelines)
6. [Troubleshooting](#troubleshooting)

---

## Accessibility Features

### ✅ Implemented Features

#### Android
- **Semantics**: All interactive elements have proper roles (Button, Tab, Checkbox)
- **Content Descriptions**: All images and icons have meaningful descriptions
- **Touch Targets**: Minimum 48dp on all interactive elements
- **Color Contrast**: 4.5:1 minimum ratio (WCAG AA)
- **Text Scaling**: Proper .sp units, respects system font size
- **Live Regions**: Progress indicators announce changes
- **Heading Hierarchy**: Screen titles marked as headings
- **State Descriptions**: Toggle and selection states clearly announced

#### iOS
- **VoiceOver Integration**: Full wave coordination via audio announcements
- **Dynamic Type**: 12 text size levels (0.8x - 3.0x scaling)
- **Haptic Feedback**: Tactile cues for wave events
- **Map Accessibility**: VoiceOver can navigate map features
- **Toast Announcements**: All toasts broadcast to VoiceOver
- **Semantic Bridging**: Compose semantics automatically translate to iOS

---

## Implementation Checklist

### For All New UI Components

- [ ] **Clickable elements** have `Modifier.semantics { role = Role.Button }`
- [ ] **Images/Icons** have `contentDescription = "meaningful text"` or `null` for decorative
- [ ] **Touch targets** are minimum 48dp/44pt
- [ ] **Text colors** meet 4.5:1 contrast ratio
- [ ] **Text sizes** use `.sp` units (not `.dp`)
- [ ] **Toggles** have `toggleableState` and state descriptions
- [ ] **Tabs** have `selected` state and `Role.Tab`
- [ ] **Dynamic content** has `liveRegion = LiveRegionMode.Polite`
- [ ] **Screen titles** have `semantics { heading = true }`
- [ ] **Focus indicators** are visible for keyboard navigation

---

## Code Patterns

### 1. Basic Button Semantics

```kotlin
Button(
    onClick = { /* action */ },
    modifier = Modifier
        .semantics {
            role = Role.Button
            contentDescription = "Clear action description"
        }
) {
    Text("Button Text")
}
```

### 2. Image with Content Description

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

### 3. Touch Target Compliance

```kotlin
// Wrap small visual elements in 48dp container
Box(
    modifier = Modifier
        .size(48.dp)  // Minimum touch target
        .clickable { action() }
        .semantics { role = Role.Button },
    contentAlignment = Alignment.Center
) {
    Icon(
        modifier = Modifier.size(24.dp),  // Visual size smaller
        imageVector = icon,
        contentDescription = description
    )
}
```

### 4. Tab with Selection State

```kotlin
Tab(
    selected = isSelected,
    onClick = { onTabSelected() },
    modifier = Modifier.semantics {
        role = Role.Tab
        selected = isSelected
        stateDescription = if (isSelected) "Selected" else "Not selected"
    }
) {
    Text("Tab Label")
}
```

### 5. Checkbox/Toggle State

```kotlin
IconButton(
    onClick = { onToggle() },
    modifier = Modifier.semantics {
        role = Role.Checkbox
        toggleableState = if (isChecked) ToggleableState.On else ToggleableState.Off
        stateDescription = if (isChecked) "Checked" else "Unchecked"
    }
) {
    Icon(
        imageVector = if (isChecked) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
        contentDescription = "Checkbox label"
    )
}
```

### 6. Live Region for Progress

```kotlin
Box(
    modifier = Modifier.semantics {
        liveRegion = LiveRegionMode.Polite
        contentDescription = "Loading: $progressMessage"
    }
) {
    CircularProgressIndicator()
    Text(progressMessage)
}

// Announce milestones
LaunchedEffect(progress) {
    if (progress % 25 == 0) {
        // Screen reader announces automatically with liveRegion
    }
}
```

### 7. Heading Hierarchy

```kotlin
Text(
    text = screenTitle,
    style = MaterialTheme.typography.headlineLarge,
    modifier = Modifier.semantics { heading = true }
)
```

### 8. Color Contrast

```kotlin
// ✅ GOOD: 4.5:1 contrast
Text(
    text = "Readable text",
    color = Color(0xFF3D8F58),  // Dark green on white background
)

// ❌ BAD: 2.8:1 contrast
Text(
    text = "Hard to read",
    color = Color(0xFF5DB075),  // Light green on white - FAILS WCAG
)
```

### 9. iOS VoiceOver Announcements

```kotlin
// In shared code
val platformEnabler = getIosSafePlatformEnabler()

LaunchedEffect(waveState) {
    when (waveState) {
        WarmingStart -> {
            platformEnabler.announceForAccessibility("Wave warming phase starting")
            platformEnabler.triggerHapticWarning()
        }
        HitDetected -> {
            platformEnabler.announceForAccessibility("Wave hit successful!")
            platformEnabler.triggerHapticSuccess()
        }
    }
}
```

### 10. iOS Dynamic Type

```kotlin
// Typography automatically scales on iOS
@Composable
fun MyScreen() {
    val typography = AppTypography()  // Respects iOS Dynamic Type

    Text(
        text = "Scales with system settings",
        style = typography.bodyLarge  // Will scale 0.8x - 3.0x on iOS
    )
}
```

---

## Testing Requirements

### Automated Tests

```bash
# Run all accessibility tests
./gradlew :composeApp:connectedDebugAndroidTest --tests "*AccessibilityTest*"

# Run unit tests
./gradlew :shared:testDebugUnitTest
```

### Manual Testing

#### Android - TalkBack

1. Enable: **Settings → Accessibility → TalkBack**
2. Test scenarios:
   - Navigate entire app with swipe gestures
   - Verify all interactive elements are announced
   - Check state changes are announced
   - Confirm progress updates are announced
   - Test wave participation with audio-only cues

3. Validation:
   - [ ] All buttons, tabs, toggles announced correctly
   - [ ] Selection states are clear ("Selected" / "Not selected")
   - [ ] Progress indicators announce milestones
   - [ ] Navigation is logical and efficient
   - [ ] No silent UI elements

#### iOS - VoiceOver

1. Enable: **Settings → Accessibility → VoiceOver**
2. Test scenarios:
   - Navigate entire app with swipe gestures
   - Test map accessibility (swipe through map elements)
   - Participate in wave with VoiceOver + haptics
   - Test at maximum Dynamic Type size
   - Verify wave timing announcements

3. Validation:
   - [ ] All Compose semantics translate correctly
   - [ ] Map elements are accessible and labeled
   - [ ] Wave timing announced ("5", "4", "3", "2", "1", "Wave hit!")
   - [ ] Haptics provide tactile feedback
   - [ ] Text scales properly at all 12 sizes
   - [ ] No UI breaks at 300% text size

#### iOS - Dynamic Type

1. **Settings → Accessibility → Display & Text Size → Larger Text**
2. Test all 12 text sizes:
   - Standard: Extra Small → XXX Large
   - Accessibility: Medium → XXX Large (300%)
3. Verify no text truncation or layout breaks

### Automated Scanning

```kotlin
// Add to instrumented tests
class MyAccessibilityTest : BaseAccessibilityTest() {

    companion object {
        init {
            AccessibilityChecks.enable()
                .setRunChecksFromRootView(true)
        }
    }

    @Test
    fun screenName_accessibilityCompliance() {
        composeTestRule.setContent {
            MyScreen()
        }

        // Espresso will automatically check:
        // - Touch targets (48dp minimum)
        // - Content descriptions
        // - Color contrast
        // - Clickable span handling
    }
}
```

---

## Platform-Specific Guidelines

### Android

#### Required Modifiers

Every interactive element needs:
```kotlin
Modifier.semantics {
    role = Role.Button  // or Tab, Checkbox
    contentDescription = "Action description"
    // Optional:
    stateDescription = "Current state"
    selected = isSelected  // for tabs
    toggleableState = ToggleableState.On  // for checkboxes
}
```

#### Live Regions

For dynamic content:
```kotlin
Modifier.semantics {
    liveRegion = LiveRegionMode.Polite  // or Assertive for urgent
    contentDescription = dynamicMessage
}
```

#### Focus Management

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

### iOS

#### VoiceOver Announcements

```swift
// In Swift (IOSPlatformEnabler.swift)
@objc public func announceForAccessibility(_ message: String) {
    UIAccessibility.post(notification: .announcement, argument: message)
}
```

```kotlin
// In Kotlin (call from shared code)
platformEnabler.announceForAccessibility("Critical update")
```

#### Haptic Feedback

```swift
// Success
notificationFeedback.notificationOccurred(.success)

// Warning
notificationFeedback.notificationOccurred(.warning)

// Impact
impactFeedback.impactOccurred()
```

```kotlin
// From Kotlin
platformEnabler.triggerHapticSuccess()
platformEnabler.triggerHapticWarning()
platformEnabler.triggerHapticImpact()
```

#### Map Accessibility

```swift
// Create accessibility elements for map features
let userElement = UIAccessibilityElement(accessibilityContainer: mapView)
userElement.accessibilityLabel = "Your current position"
userElement.accessibilityTraits = .updatesFrequently
userElement.accessibilityFrame = calculateFrameForCoordinate(position)

mapView.accessibilityElements = [userElement, areaElement, ...]
```

#### Dynamic Type

```kotlin
// iOS implementation (shared/src/iosMain/.../DynamicTypeScale.kt)
@Composable
actual fun rememberDynamicTypeScale(): Float {
    val category = UIApplication.sharedApplication.preferredContentSizeCategory
    return when(category) {
        UIContentSizeCategoryExtraSmall -> 0.8f
        // ... map all 12 categories
        UIContentSizeCategoryAccessibilityExtraExtraExtraLarge -> 3.0f
        else -> 1.0f
    }
}
```

---

## Troubleshooting

### Android Issues

#### "TalkBack not announcing element"
- ✅ Check element has `contentDescription` or semantic role
- ✅ Verify element is `clickable` or `focusable`
- ✅ Check parent isn't blocking accessibility

#### "State changes not announced"
- ✅ Add `stateDescription` to semantics
- ✅ Use `toggleableState` for checkboxes
- ✅ Mark tabs with `selected = true/false`

#### "Touch target too small"
- ✅ Wrap element in 48dp Box
- ✅ Use `Modifier.size(48.dp)` on clickable parent
- ✅ Visual size can be smaller (e.g., 24dp icon inside 48dp box)

### iOS Issues

#### "VoiceOver not reading element"
- ✅ Verify Compose semantics block is present
- ✅ Check semantic bridging (contentDescription → accessibilityLabel)
- ✅ Test on real device (simulator sometimes differs)

#### "Text not scaling with Dynamic Type"
- ✅ Ensure Typography uses `AppTypography()` function
- ✅ Verify `rememberDynamicTypeScale()` is called
- ✅ Check fontSize multiplied by scale factor

#### "Map not accessible"
- ✅ Verify `mapView.isAccessibilityElement = false`
- ✅ Check `updateMapAccessibility()` is called on state changes
- ✅ Ensure accessibility elements have proper frames

#### "Haptics not working"
- ✅ Test on real device (haptics don't work on simulator)
- ✅ Check haptic generators are prepared in init
- ✅ Verify device haptics are enabled in system settings

---

## WCAG 2.1 Compliance Matrix

| Criterion | Level | Android | iOS | Notes |
|-----------|-------|---------|-----|-------|
| **1.1.1 Non-text Content** | A | ✅ | ✅ | All images have alt text |
| **1.3.1 Info and Relationships** | A | ✅ | ✅ | Headings, roles, states |
| **1.4.3 Contrast (Minimum)** | AA | ✅ | ✅ | 4.5:1 ratio achieved |
| **1.4.4 Resize Text** | AA | ✅ | ✅ | Up to 200% (iOS: 300%) |
| **2.1.1 Keyboard** | A | ✅ | ✅ | D-pad/VoiceOver navigation |
| **2.4.6 Headings and Labels** | AA | ✅ | ✅ | Screen titles marked |
| **2.4.7 Focus Visible** | AA | ⏳ | ✅ | Android: pending |
| **2.5.5 Target Size** | AAA | ✅ | ✅ | 48dp/44pt minimum |
| **4.1.2 Name, Role, Value** | A | ✅ | ✅ | Complete semantics |
| **4.1.3 Status Messages** | AA | ✅ | ✅ | Live regions implemented |

**Legend**: ✅ Implemented | ⏳ Pending | ❌ Not implemented

---

## Resources

### Internal Documentation
- [iOS Map Accessibility](./iOS_MAP_ACCESSIBILITY.md)
- [iOS Semantic Bridging](./IOS_SEMANTIC_BRIDGING.md) - Compose → iOS accessibility mapping
- [Testing Strategy](./TESTING_STRATEGY.md)
- [UI Testing Guide](./UI_TESTING_GUIDE.md)

### External Standards
- [WCAG 2.1 Guidelines](https://www.w3.org/WAI/WCAG21/quickref/)
- [Android Accessibility](https://developer.android.com/guide/topics/ui/accessibility)
- [iOS Accessibility](https://developer.apple.com/accessibility/)
- [Compose Accessibility](https://developer.android.com/jetpack/compose/accessibility)

### Testing Tools
- **Android**: TalkBack, Accessibility Scanner, Espresso AccessibilityChecks
- **iOS**: VoiceOver, Accessibility Inspector
- **Web**: WebAIM Contrast Checker

---

*Last Updated: October 2025*
*Maintained by: WorldWideWaves Development Team*
