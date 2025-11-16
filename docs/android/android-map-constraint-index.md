# Android Map Constraint Implementation - Complete Documentation Index

## Overview

This documentation provides a comprehensive analysis of the Android map constraint implementation in WorldWideWaves, designed to help understand and replicate these patterns on iOS.

**Key Insight**: Android uses a **preventive enforcement model** that locks min zoom immediately and clamps gestures in real-time, preventing invalid camera states before they occur.

---

## Documentation Files (4 Documents, 1,594 lines)

### 1. android-map-constraint-analysis.md (452 lines)

**Purpose**: Executive overview and architectural deep-dive

**Contents**:

- Architecture overview (3 layers: Enforcer, Adapter, Native)
- Mode-specific constraint logic (BOUNDS vs WINDOW)
- Min zoom calculation theory and implementation
- Gesture overflow prevention with detailed code comments
- Constraint state transitions and dynamic recalculation
- Critical implementation details (padding, locking, suppression)
- Test coverage overview
- iOS implementation takeaways

**Start here if you want to understand**:

- How the system works end-to-end
- Why certain design choices were made
- What differs between BOUNDS and WINDOW modes

---

### 2. android-constraint-code-patterns.md (611 lines)

**Purpose**: Working code snippets showing exact implementation

**Contains**:

1. Min Zoom Calculation Pattern (WINDOW Mode) - 64 lines
2. Preventive Gesture Clamping Pattern - 75 lines
3. Viewport Padding Calculation Pattern - 100 lines
4. Mode-Specific Constraint Application - 105 lines
5. Min Zoom Locking Pattern - 55 lines
6. Bounds Similarity Check Pattern - 60 lines
7. Animation Suppression Pattern - 35 lines

**Start here if you want to**:

- Copy working code patterns
- See exact implementation details
- Understand specific algorithms

**Key Methods Shown**:

- `AndroidMapLibreAdapter.setBoundsForCameraTarget()` lines 400-464
- `AndroidMapLibreAdapter.setupPreventiveGestureConstraints()` lines 516-591
- `MapBoundsEnforcer.calculateVisibleRegionPadding()` lines 383-435
- `AbstractEventMap.moveToWindowBounds()` lines 186-228

---

### 3. android-patterns-quick-reference.md (290 lines)

**Purpose**: One-page reference for developers

**Contains**:

- Architecture at a glance (visual diagram)
- Min zoom calculation decision tree (flowchart)
- Padding calculation formulas (BOUNDS vs WINDOW)
- Gesture clamping algorithm (step-by-step)
- State transition checklist (BOUNDS and WINDOW setup)
- Critical implementation rules (5 key rules with examples)
- Debugging checklist
- Files reference table
- One-page summary

**Start here if you want to**:

- Quickly understand a specific concept
- Debug issues using the checklist
- Find relevant files by feature
- Understand state transitions

---

### 4. android-source-file-reference.md (241 lines)

**Purpose**: Detailed file paths and code locations

**Contains**:

- Complete absolute file paths for all source files
- Shared layer files with line ranges
- Android-specific implementation files with line ranges
- Test file locations with test descriptions
- Key code locations indexed by feature
- Class hierarchy diagram
- Data classes used
- Integration points between layers
- Quick navigation guide
- Configuration constants

**Start here if you want to**:

- Find exact file locations
- Navigate between files
- Understand class relationships
- Locate specific code sections

---

## How to Use This Documentation

### For Understanding the Overall Design

1. Read **android-map-constraint-analysis.md** sections:
   - Architecture Overview
   - Mode-Specific Constraint Logic
   - Key Takeaways for iOS Implementation

2. Reference **android-patterns-quick-reference.md**:
   - Architecture at a Glance diagram
   - One-page summary

### For Implementation

1. Start with **android-patterns-quick-reference.md**:
   - Min Zoom Calculation Decision Tree
   - Critical Implementation Rules

2. Use **android-constraint-code-patterns.md** to:
   - Copy exact code patterns
   - Understand algorithms
   - See logging patterns

3. Reference **android-source-file-reference.md** to:
   - Find source files
   - Locate specific code sections
   - Understand integrations

### For Debugging

1. Use **android-patterns-quick-reference.md**:
   - Debugging Checklist
   - Look for specific log messages

2. Reference **android-source-file-reference.md**:
   - Find test files
   - Review test patterns

### For iOS Implementation

1. Read **android-map-constraint-analysis.md** section:
   - Key Takeaways for iOS Implementation

2. Understand patterns from:
   - Min zoom locking (must calculate once, set immediately)
   - Aspect ratio fitting (critical for no-overflow guarantee)
   - Padding logic differs by mode (BOUNDS = 0, WINDOW = viewport/2)
   - Prevent infinite loops (0.1% tolerance, lock min zoom)
   - Gesture clamping is optional but valuable

---

## Key Concepts Summary

### 1. Min Zoom Calculation

- **WINDOW Mode**: Aspect ratio fitting - fit by constraining dimension
  - Event wider than screen: fit by HEIGHT
  - Event taller than screen: fit by WIDTH
- **BOUNDS Mode**: Fit entire event, no padding
- **Critical**: Set via `setMinZoomPreference()` immediately, lock after
- **Test**: `AspectRatioFittingTest.kt` validates all combinations

### 2. Viewport Padding

- **BOUNDS Mode**: Zero padding (0, 0) - show entire event
- **WINDOW Mode**: Viewport half-size - prevent edge overflow
- **Limit**: Use 49% max to prevent bounds inversion
- **Recalculation**: Only if > 10% change, check similarity

### 3. Gesture Clamping (WINDOW Mode Only)

- **Preventive**: Intercept gestures before invalid state occurs
- **Validation**: Check viewport inside bounds in real-time
- **Clamping**: Calculate valid camera center range and clamp
- **Distinction**: Only apply to user gestures, not programmatic

### 4. State Management

- **Locking**: Min zoom locked after first calculation
- **Suppression**: Disable constraint updates during animations
- **Tolerance**: 0.1% for bounds similarity check
- **Skip Logic**: Remember to skip after suppression

### 5. Animation Coordination

- **Suppression Flag**: `suppressCorrections` in AbstractEventMap
- **Callback Pattern**: Disable before, enable after
- **Listener Skip**: Set `skipNextRecalculation` flag
- **Goal**: Prevent corrections from fighting animations

---

## Architecture at a Glance

```
┌─────────────────────────────────────────┐
│      AbstractEventMap (Shared)          │
│  ├─ moveToMapBounds() [BOUNDS mode]    │
│  └─ moveToWindowBounds() [WINDOW mode] │
└──────────────┬──────────────────────────┘
               │
               ▼
┌─────────────────────────────────────────┐
│   MapBoundsEnforcer (Shared)            │
│  ├─ applyConstraints()                 │
│  ├─ calculateConstraintBounds()        │
│  └─ calculateVisibleRegionPadding()    │
└──────────────┬──────────────────────────┘
               │
               ▼
┌─────────────────────────────────────────┐
│  AndroidMapLibreAdapter (Platform)      │
│  ├─ setBoundsForCameraTarget()         │
│  │  ├─ Calculate min zoom (aspect ratio)
│  │  ├─ Lock min zoom                  │
│  │  └─ setupPreventiveGestureConstraints()
│  │
│  ├─ setMinZoomPreference()             │
│  ├─ setMaxZoomPreference()             │
│  └─ Gesture clamping listeners         │
└──────────────┬──────────────────────────┘
               │
               ▼
┌─────────────────────────────────────────┐
│    MapLibre Native (Android)            │
│  ├─ setMinZoomPreference()             │
│  ├─ setLatLngBoundsForCameraTarget()    │
│  └─ Camera listeners                   │
└─────────────────────────────────────────┘
```

---

## Quick Reference Table

| Concept | BOUNDS Mode | WINDOW Mode |
| --------- | ------------ | ------------ |

| **Padding** | 0 (show all) | viewport/2 (prevent overflow) |
| **Min Zoom** | Fit entire event | Fit constraining dimension |
| **Gestures** | Disabled | Enabled + clamped |
| **Animation** | moveToMapBounds() | No animation, user controlled |
| **Constraint** | Camera center unconstrained | Camera center constrained |
| **Use Case** | Event detail screen | Full map screen |
| **Viewport** | Always shows event | Inside event bounds |

---

## Critical Rules (Copy-Paste These)

```kotlin
// Rule 1: Min Zoom Locking
minZoomLocked = false
if (!minZoomLocked && originalEventBounds != null) {
    calculatedMinZoom = calculateMinZoom()
    mapLibreMap.setMinZoomPreference(calculatedMinZoom)
    minZoomLocked = true  // LOCK IT
}

// Rule 2: Bounds Similarity (0.1% tolerance)
if (lastAppliedBounds != null &&
    abs(new.sw.lat - last.sw.lat) + abs(new.ne.lat - last.ne.lat) < 0.001 &&
    abs(new.sw.lng - last.sw.lng) + abs(new.ne.lng - last.ne.lng) < 0.001) {
    return  // Skip update
}

// Rule 3: Padding Clamping (49%, not 50%)
val maxLatPadding = eventLatSpan * 0.49
val maxLngPadding = eventLngSpan * 0.49
val effectiveLatPadding = min(requestedPadding, maxLatPadding)
val effectiveLngPadding = min(requestedPadding, maxLngPadding)

// Rule 4: Animation Suppression
suppressCorrections = true
try {
    mapLibreAdapter.animateCamera(...)
} finally {
    suppressCorrections = false
}

// Rule 5: Gesture Distinction
map.addOnCameraMoveStartedListener { reason ->
    isGestureInProgress = (reason == REASON_API_GESTURE)
}
```

---

## Testing Patterns

### Test Files and What They Validate

| Test File | Purpose | Coverage |
| ----------- | --------- | ---------- |

| `AspectRatioFittingTest.kt` | Min zoom calculation | Wide, tall, extreme cases |
| `BoundsWindowModeTest.kt` | WINDOW padding calc | Viewport-based constraints |
| `AbstractEventMapTest.kt` | Integration flow | Mode transitions, animations |
| `RegressionPreventionTest.kt` | Edge cases | Infinite loops, bounds inversion |
| `MapBoundsEnforcerTest.kt` | Enforcer logic | Padding, similarity checks |

### Key Test Insights

- Wide event (Paris 2.84:1) on tall screen → fits by HEIGHT
- Tall event (0.25:1) on wide screen → fits by WIDTH
- Bounds similarity with 0.1% tolerance prevents loops
- Extreme cases (100:1, 1:100) tested for overflow

---

## File Organization

```
WorldWideWaves/
├── ANDROID_MAP_CONSTRAINT_INDEX.md (This file - Navigation)
├── ANDROID_MAP_CONSTRAINT_ANALYSIS.md (Deep dive)
├── ANDROID_CONSTRAINT_CODE_PATTERNS.md (Code snippets)
├── ANDROID_PATTERNS_QUICK_REFERENCE.md (One-page reference)
├── ANDROID_SOURCE_FILE_REFERENCE.md (File locations)
│
├── shared/src/commonMain/kotlin/com/worldwidewaves/shared/map/
│   ├── AbstractEventMap.kt (Platform-independent map logic)
│   ├── MapBoundsEnforcer.kt (Constraint enforcement)
│   └── MapLibreAdapter.kt (Platform interface)
│
├── composeApp/src/androidMain/kotlin/com/worldwidewaves/map/
│   ├── AndroidMapLibreAdapter.kt (Android implementation)
│   └── AndroidEventMap.kt (Android UI integration)
│
└── shared/src/androidUnitTest/kotlin/com/worldwidewaves/shared/map/
    ├── AspectRatioFittingTest.kt (Min zoom tests)
    ├── BoundsWindowModeTest.kt (WINDOW mode tests)
    └── RegressionPreventionTest.kt (Edge case tests)
```

---

## Next Steps

1. **Understand the pattern** (30 min)
   - Read ANDROID_MAP_CONSTRAINT_ANALYSIS.md
   - Review ANDROID_PATTERNS_QUICK_REFERENCE.md

2. **Study the code** (1-2 hours)
   - Read ANDROID_CONSTRAINT_CODE_PATTERNS.md
   - Navigate files using ANDROID_SOURCE_FILE_REFERENCE.md
   - Read source files in this order:
     1. `MapBoundsEnforcer.kt` lines 383-435 (padding logic)
     2. `AbstractEventMap.kt` lines 124-228 (mode setup)
     3. `AndroidMapLibreAdapter.kt` lines 367-514 (min zoom)
     4. `AndroidMapLibreAdapter.kt` lines 516-591 (gesture clamping)

3. **Plan iOS implementation**
   - Map each Android concept to iOS equivalent
   - Note where APIs differ (e.g., `shouldChangeFrom` vs gesture listeners)
   - Create iOS adapter following same structure

4. **Implement incrementally**
   - Start with min zoom calculation (most critical)
   - Add bounds constraints (viewport prevention)
   - Add gesture clamping (nice to have, validate workflow)

---

## Document Relationships

```
INDEX (You are here)
  │
  ├─→ ANALYSIS.md (Understand architecture)
  │    └─→ CODE_PATTERNS.md (See exact code)
  │         └─→ SOURCE_FILE_REFERENCE.md (Find in codebase)
  │
  └─→ QUICK_REFERENCE.md (Quick lookup)
       ├─→ Decision trees
       ├─→ Checklists
       └─→ Code snippets
```

---

## Questions This Documentation Answers

- **Why**: Why use preventive enforcement instead of reactive?
  → Prevents invalid states before they occur, prevents fighting with user gestures

- **What**: What's the difference between BOUNDS and WINDOW mode?
  → BOUNDS: zero padding, show all event; WINDOW: viewport padding, prevent overflow

- **How**: How is min zoom calculated?
  → Compare aspect ratios, fit by constraining dimension, use MapLibre camera API

- **When**: When is min zoom set?
  → Immediately in `setBoundsForCameraTarget()`, locked after first calculation

- **Where**: Where does gesture clamping happen?
  → In `setupPreventiveGestureConstraints()`, during `onCameraMove` callback

- **How Much**: How much padding is safe?
  → Use 49% of event size max to prevent bounds inversion

---

## Version Information

- **Last Updated**: October 23, 2025
- **Android Implementation**: Complete and working
- **Test Coverage**: 902+ unit tests, comprehensive edge cases
- **iOS Status**: Ready for analysis and replication

---

## Related Documentation in Project

- `CLAUDE.md` - Project-wide development instructions
- `CLAUDE_iOS.md` - iOS-specific guidelines
- `docs/ios/ios-violation-tracker.md` - iOS deadlock prevention
- `docs/architecture.md` - Overall system architecture

---

## Contact & Support

For questions about implementation:

1. Review the relevant documentation section above
2. Check code snippets in ANDROID_CONSTRAINT_CODE_PATTERNS.md
3. Reference test cases in test files
4. Consult ANDROID_SOURCE_FILE_REFERENCE.md for file locations

---

**Happy reading! Start with ANDROID_MAP_CONSTRAINT_ANALYSIS.md for the full story.**
